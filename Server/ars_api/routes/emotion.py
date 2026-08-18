from fastapi import APIRouter, Depends, HTTPException

from ..database import db_session
from ..models.schemas import EmotionAlertSummary
from ..notifications.emotion_watcher import format_alert_message
from .auth import verify_token

router = APIRouter(prefix="/emotion", tags=["emotion"], dependencies=[Depends(verify_token)])


@router.get("/alerts", response_model=list[EmotionAlertSummary])
def list_unread_alerts() -> list[EmotionAlertSummary]:
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT id, masa_no, negatif_yuzde, sure_dakika, olusturma_zamani
            FROM DuyguUyari
            WHERE okundu = 0
            ORDER BY id DESC
            LIMIT 20
            """
        ).fetchall()
    return [
        EmotionAlertSummary(
            id=row["id"],
            tableNumber=row["masa_no"],
            negativePercent=row["negatif_yuzde"],
            durationMinutes=row["sure_dakika"],
            message=format_alert_message(
                row["masa_no"],
                row["negatif_yuzde"],
                row["sure_dakika"],
            ),
            createdAt=row["olusturma_zamani"],
        )
        for row in rows
    ]


@router.patch("/alerts/{alert_id}/ack")
def acknowledge_alert(alert_id: int) -> dict[str, str]:
    with db_session() as conn:
        cur = conn.execute(
            "UPDATE DuyguUyari SET okundu = 1 WHERE id = ? AND okundu = 0",
            (alert_id,),
        )
        if cur.rowcount == 0:
            raise HTTPException(status_code=404, detail="Alert not found")
    return {"status": "acknowledged"}


@router.get("/summary/{table_number}")
def table_emotion_summary(table_number: int) -> dict[str, float | int | None]:
    """Son 5 dakikadaki negatif duygu oranı (dashboard/mobil özet)."""
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT duygu_durumu
            FROM DuyguOlcum
            WHERE masa_no = ?
              AND olcum_zamani >= datetime('now', '-5 minutes')
            """,
            (table_number,),
        ).fetchall()
    if not rows:
        return {"tableNumber": table_number, "sampleCount": 0, "negativePercent": None}
    from ..notifications.emotion_watcher import is_negative_emotion

    neg = sum(1 for r in rows if is_negative_emotion(r["duygu_durumu"]))
    pct = round(100.0 * neg / len(rows), 1)
    return {
        "tableNumber": table_number,
        "sampleCount": len(rows),
        "negativePercent": pct,
    }
