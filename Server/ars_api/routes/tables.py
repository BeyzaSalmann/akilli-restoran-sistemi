from fastapi import APIRouter, Depends, HTTPException

from ..database import (
    db_session,
    order_status_from_db,
    resolve_table,
    table_status_from_db,
    table_status_to_db,
)
from ..notifications.push_service import notify_waiters
from ..models.schemas import (
    CloseBillResponse,
    OrderItemSummary,
    OrderSummary,
    TableDetail,
    TableStatusUpdate,
    TableSummary,
)
from .auth import verify_token

router = APIRouter(prefix="/tables", tags=["tables"], dependencies=[Depends(verify_token)])


def _fetch_active_orders(conn, masa_no: int) -> list[OrderSummary]:
    rows = conn.execute(
        """
        SELECT siparis_id, toplam_tutar, masa_no, durum
        FROM Siparis
        WHERE masa_no = ?
          AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))
        ORDER BY siparis_tarihi DESC
        """,
        (masa_no,),
    ).fetchall()

    orders: list[OrderSummary] = []
    for row in rows:
        items_rows = conn.execute(
            """
            SELECT sd.urun_id,
                   COALESCE(u.urun_adi, 'Product #' || sd.urun_id || ' (legacy)') AS urun_adi,
                   sd.adet, sd.birim_fiyat, sd.toplam_fiyat
            FROM SiparisDetay sd
            LEFT JOIN Urun u ON sd.urun_id = u.urun_id
            WHERE sd.siparis_id = ?
            """,
            (row["siparis_id"],),
        ).fetchall()
        items = [
            OrderItemSummary(
                productId=r["urun_id"],
                productName=r["urun_adi"],
                quantity=r["adet"],
                unitPrice=r["birim_fiyat"],
                lineTotal=r["toplam_fiyat"],
            )
            for r in items_rows
        ]
        orders.append(
            OrderSummary(
                id=row["siparis_id"],
                tableNumber=row["masa_no"],
                status=order_status_from_db(row["durum"]),
                totalAmount=row["toplam_tutar"],
                items=items,
            )
        )
    return orders


@router.get("", response_model=list[TableSummary])
def list_tables() -> list[TableSummary]:
    with db_session() as conn:
        rows = conn.execute(
            "SELECT masa_id, masa_no, durum FROM Masa ORDER BY masa_no"
        ).fetchall()
        return [
            TableSummary(
                id=r["masa_id"],
                number=r["masa_no"],
                status=table_status_from_db(r["durum"]),
            )
            for r in rows
        ]


@router.get("/{table_id}", response_model=TableDetail)
def get_table(table_id: int) -> TableDetail:
    with db_session() as conn:
        row = resolve_table(conn, table_id)
        if not row:
            raise HTTPException(status_code=404, detail="Table not found")
        return TableDetail(
            id=row["masa_id"],
            number=row["masa_no"],
            status=table_status_from_db(row["durum"]),
            activeOrders=_fetch_active_orders(conn, row["masa_no"]),
        )


@router.patch("/{table_id}/status", response_model=TableSummary)
def update_table_status(table_id: int, body: TableStatusUpdate) -> TableSummary:
    try:
        db_status = table_status_to_db(body.status)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    with db_session() as conn:
        row = resolve_table(conn, table_id)
        if not row:
            raise HTTPException(status_code=404, detail="Table not found")
        conn.execute(
            "UPDATE Masa SET durum = ? WHERE masa_id = ?",
            (db_status, row["masa_id"]),
        )
        if body.status == "bill_requested":
            notify_waiters(
                "Hesap istendi",
                f"Masa {row['masa_no']} hesap istiyor",
                {"type": "bill_requested", "tableNumber": row["masa_no"]},
            )
        return TableSummary(
            id=row["masa_id"],
            number=row["masa_no"],
            status=body.status,
        )


@router.post("/{table_id}/close-bill", response_model=CloseBillResponse)
def close_bill(table_id: int) -> CloseBillResponse:
    with db_session() as conn:
        row = resolve_table(conn, table_id)
        if not row:
            raise HTTPException(status_code=404, detail="Table not found")
        masa_no = row["masa_no"]

        order_rows = conn.execute(
            """
            SELECT siparis_id, toplam_tutar
            FROM Siparis
            WHERE masa_no = ?
              AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))
            """,
            (masa_no,),
        ).fetchall()

        total = 0.0
        completed_ids: list[int] = []
        for o in order_rows:
            total += o["toplam_tutar"]
            completed_ids.append(o["siparis_id"])
            conn.execute(
                "UPDATE Siparis SET durum = 'Tamamlandı' WHERE siparis_id = ?",
                (o["siparis_id"],),
            )

        conn.execute(
            "UPDATE Masa SET durum = 'Boş' WHERE masa_id = ?",
            (row["masa_id"],),
        )

        return CloseBillResponse(
            tableNumber=masa_no,
            totalAmount=total,
            completedOrderIds=completed_ids,
        )
