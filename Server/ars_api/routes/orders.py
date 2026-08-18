from fastapi import APIRouter, Depends, HTTPException, Query

from ..database import (
    db_session,
    order_status_from_db,
    order_status_to_db,
    resolve_table,
)
from ..models.schemas import (
    CreateOrderRequest,
    CreateOrderResponse,
    OrderItemSummary,
    OrderStatusUpdate,
    OrderSummary,
)
from .auth import verify_token

router = APIRouter(prefix="/orders", tags=["orders"], dependencies=[Depends(verify_token)])


def _order_items(conn, siparis_id: int) -> list[OrderItemSummary]:
    rows = conn.execute(
        """
        SELECT sd.urun_id,
               COALESCE(u.urun_adi, 'Product #' || sd.urun_id || ' (legacy)') AS urun_adi,
               sd.adet, sd.birim_fiyat, sd.toplam_fiyat
        FROM SiparisDetay sd
        LEFT JOIN Urun u ON sd.urun_id = u.urun_id
        WHERE sd.siparis_id = ?
        """,
        (siparis_id,),
    ).fetchall()
    return [
        OrderItemSummary(
            productId=r["urun_id"],
            productName=r["urun_adi"],
            quantity=r["adet"],
            unitPrice=r["birim_fiyat"],
            lineTotal=r["toplam_fiyat"],
        )
        for r in rows
    ]


def _build_order_summary(conn, row) -> OrderSummary:
    return OrderSummary(
        id=row["siparis_id"],
        tableNumber=row["masa_no"],
        status=order_status_from_db(row["durum"]),
        totalAmount=row["toplam_tutar"],
        items=_order_items(conn, row["siparis_id"]),
    )


@router.get("/active", response_model=list[OrderSummary])
def list_active_orders(
    tableId: int | None = Query(default=None),
) -> list[OrderSummary]:
    with db_session() as conn:
        if tableId is not None:
            table = resolve_table(conn, tableId)
            if not table:
                raise HTTPException(status_code=404, detail="Table not found")
            masa_no = table["masa_no"]
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
        else:
            rows = conn.execute(
                """
                SELECT siparis_id, toplam_tutar, masa_no, durum
                FROM Siparis
                WHERE (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))
                ORDER BY siparis_tarihi DESC
                """
            ).fetchall()
        return [_build_order_summary(conn, r) for r in rows]


@router.post("", response_model=CreateOrderResponse, status_code=201)
def create_order(body: CreateOrderRequest) -> CreateOrderResponse:
    with db_session() as conn:
        table = resolve_table(conn, body.tableId)
        if not table:
            # tableId may be masa_no directly
            table = conn.execute(
                "SELECT masa_id, masa_no, durum FROM Masa WHERE masa_no = ?",
                (body.tableId,),
            ).fetchone()
        if not table:
            raise HTTPException(status_code=404, detail="Table not found")

        masa_no = table["masa_no"]
        total = 0.0
        line_data: list[tuple[int, int, float, float]] = []

        for item in body.items:
            product = conn.execute(
                "SELECT urun_id, fiyat FROM Urun WHERE urun_id = ?",
                (item.productId,),
            ).fetchone()
            if not product:
                raise HTTPException(
                    status_code=400,
                    detail=f"Product not found: {item.productId}",
                )
            line_total = product["fiyat"] * item.quantity
            total += line_total
            line_data.append(
                (product["urun_id"], item.quantity, product["fiyat"], line_total)
            )

        cur = conn.execute(
            "INSERT INTO Siparis(toplam_tutar, masa_no, durum) VALUES (?, ?, 'Hazırlanıyor')",
            (total, masa_no),
        )
        siparis_id = cur.lastrowid

        for urun_id, qty, unit_price, line_total in line_data:
            conn.execute(
                """
                INSERT INTO SiparisDetay(siparis_id, urun_id, adet, birim_fiyat, toplam_fiyat)
                VALUES (?, ?, ?, ?, ?)
                """,
                (siparis_id, urun_id, qty, unit_price, line_total),
            )

        conn.execute(
            "UPDATE Masa SET durum = 'Dolu' WHERE masa_id = ?",
            (table["masa_id"],),
        )

        # Push: order_watcher (dashboard + mobil, ortak DB)

        return CreateOrderResponse(
            orderId=siparis_id,
            tableNumber=masa_no,
            totalAmount=total,
            status="preparing",
        )


@router.patch("/{order_id}/status", response_model=OrderSummary)
def update_order_status(order_id: int, body: OrderStatusUpdate) -> OrderSummary:
    try:
        db_status = order_status_to_db(body.status)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc

    with db_session() as conn:
        row = conn.execute(
            "SELECT siparis_id, toplam_tutar, masa_no, durum FROM Siparis WHERE siparis_id = ?",
            (order_id,),
        ).fetchone()
        if not row:
            raise HTTPException(status_code=404, detail="Order not found")

        conn.execute(
            "UPDATE Siparis SET durum = ? WHERE siparis_id = ?",
            (db_status, order_id),
        )

        if body.status == "cancelled":
            if row["masa_no"] is not None:
                active = conn.execute(
                    """
                    SELECT COUNT(*) FROM Siparis
                    WHERE masa_no = ?
                      AND siparis_id != ?
                      AND (durum IS NULL OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi'))
                    """,
                    (row["masa_no"], order_id),
                ).fetchone()[0]
                if active == 0:
                    conn.execute(
                        "UPDATE Masa SET durum = 'Boş' WHERE masa_no = ?",
                        (row["masa_no"],),
                    )

        updated = conn.execute(
            "SELECT siparis_id, toplam_tutar, masa_no, durum FROM Siparis WHERE siparis_id = ?",
            (order_id,),
        ).fetchone()

        # Push: order_watcher (dashboard + mobil, ortak DB)

        return _build_order_summary(conn, updated)
