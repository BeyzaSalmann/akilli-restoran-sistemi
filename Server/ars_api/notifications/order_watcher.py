"""Dashboard (JDBC) ve mobil (API) siparişlerini izleyip garsona push gönderir."""

from __future__ import annotations

import asyncio
import logging
from typing import Any

from ..database import db_session
from .push_service import notify_waiters

logger = logging.getLogger(__name__)

POLL_SECONDS = 2.0

ORDER_STATUS_TR = {
    "Hazırlanıyor": "Hazırlanıyor",
    "Servis Edildi": "Servis Edildi",
    "Tamamlandı": "Tamamlandı",
    "İptal Edildi": "İptal Edildi",
    None: "Hazırlanıyor",
}


def _active_orders_snapshot() -> dict[int, tuple[int | None, float, str | None]]:
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT siparis_id, masa_no, toplam_tutar, durum
            FROM Siparis
            WHERE durum IS NULL
               OR (durum != 'Tamamlandı' AND durum != 'İptal Edildi')
            ORDER BY siparis_id
            """
        ).fetchall()
    return {
        int(row["siparis_id"]): (
            row["masa_no"],
            float(row["toplam_tutar"] or 0),
            row["durum"],
        )
        for row in rows
    }


def _order_item_names(order_id: int) -> str:
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT COALESCE(u.urun_adi, 'Product #' || sd.urun_id) AS name, sd.adet
            FROM SiparisDetay sd
            LEFT JOIN Urun u ON sd.urun_id = u.urun_id
            WHERE sd.siparis_id = ?
            """,
            (order_id,),
        ).fetchall()
    parts: list[str] = []
    for row in rows:
        name = row["name"]
        qty = int(row["adet"] or 1)
        parts.append(f"{name} x{qty}" if qty > 1 else name)
    return ", ".join(parts) if parts else "Ürün bilgisi yok"


def _notify_new_order(order_id: int, masa_no: int | None, _total: float) -> None:
    masa_label = f"Masa {masa_no}" if masa_no is not None else f"Sipariş #{order_id}"
    items_text = _order_item_names(order_id)
    notify_waiters(
        "Yeni sipariş",
        f"{masa_label} · {items_text}",
        {
            "type": "new_order",
            "orderId": order_id,
            "tableNumber": masa_no,
        },
    )


def _notify_status_change(order_id: int, masa_no: int | None, durum: str | None) -> None:
    masa_label = f"Masa {masa_no}" if masa_no is not None else f"Sipariş #{order_id}"
    status_label = ORDER_STATUS_TR.get(durum, durum or "Güncellendi")
    notify_waiters(
        masa_label,
        f"Durum: {status_label}",
        {
            "type": "order_status",
            "orderId": order_id,
            "status": durum,
        },
    )


def _diff_and_notify(
    previous: dict[int, tuple[int | None, float, str | None]],
    current: dict[int, tuple[int | None, float, str | None]],
) -> None:
    for order_id, (masa_no, total, durum) in current.items():
        if order_id not in previous:
            _notify_new_order(order_id, masa_no, total)
            continue
        prev_masa, _prev_total, prev_durum = previous[order_id]
        if prev_durum != durum:
            _notify_status_change(order_id, masa_no or prev_masa, durum)


async def order_watcher_loop() -> None:
    previous: dict[int, tuple[int | None, float, str | None]] = {}
    logger.info("Order watcher started (poll=%ss)", POLL_SECONDS)
    while True:
        try:
            await asyncio.sleep(POLL_SECONDS)
            current = _active_orders_snapshot()
            if previous:
                _diff_and_notify(previous, current)
            previous = current
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            logger.warning("Order watcher error: %s", exc)


def start_order_watcher() -> asyncio.Task[Any]:
    return asyncio.create_task(order_watcher_loop())
