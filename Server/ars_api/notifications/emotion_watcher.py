"""Masa bazlı duygu ölçümlerini izler; eşik aşılınca garsona uyarı gönderir."""

from __future__ import annotations

import asyncio
import logging
import time
from typing import Any

from ..config import (
    EMOTION_ALERT_COOLDOWN_MIN,
    EMOTION_ALERT_WINDOW_MIN,
    EMOTION_MIN_DURATION_MIN,
    EMOTION_MIN_SAMPLES,
    EMOTION_NEGATIVE_THRESHOLD,
)
from ..database import db_session
from .push_service import notify_waiters

logger = logging.getLogger(__name__)

POLL_SECONDS = 10.0

NEGATIVE_KEYWORDS = (
    "kizgin",
    "kızgın",
    "uzgun",
    "üzgün",
    "angry",
    "sad",
    "fear",
    "korku",
    "tiksinme",
    "disgust",
)

_last_alert_at: dict[int, float] = {}


def is_negative_emotion(duygu: str | None) -> bool:
    if not duygu:
        return False
    lowered = duygu.lower()
    return any(keyword in lowered for keyword in NEGATIVE_KEYWORDS)


def _fetch_masa_measurements(masa_no: int, window_min: int) -> list[tuple[str, str]]:
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT duygu_durumu, olcum_zamani
            FROM DuyguOlcum
            WHERE masa_no = ?
              AND olcum_zamani >= datetime('now', ?)
            ORDER BY olcum_zamani ASC
            """,
            (masa_no, f"-{window_min} minutes"),
        ).fetchall()
    return [(row["duygu_durumu"], row["olcum_zamani"]) for row in rows]


def _fetch_active_masalar() -> list[int]:
    with db_session() as conn:
        rows = conn.execute(
            """
            SELECT DISTINCT masa_no FROM DuyguOlcum
            WHERE olcum_zamani >= datetime('now', ?)
            """,
            (f"-{EMOTION_ALERT_WINDOW_MIN} minutes",),
        ).fetchall()
    return [int(row["masa_no"]) for row in rows]


def _minutes_since(timestamp: str) -> int:
    with db_session() as conn:
        row = conn.execute(
            "SELECT CAST((julianday('now') - julianday(?)) * 24 * 60 AS INTEGER)",
            (timestamp,),
        ).fetchone()
    return max(int(row[0] if row else 0), 0)


def _analyze_masa(masa_no: int) -> tuple[float, int] | None:
    measurements = _fetch_masa_measurements(masa_no, EMOTION_ALERT_WINDOW_MIN)
    if len(measurements) < EMOTION_MIN_SAMPLES:
        return None

    negative = [m for m in measurements if is_negative_emotion(m[0])]
    ratio = len(negative) / len(measurements)
    if ratio < EMOTION_NEGATIVE_THRESHOLD:
        return None

    first_negative_time = negative[0][1]
    duration_min = _minutes_since(first_negative_time)
    if duration_min < EMOTION_MIN_DURATION_MIN:
        return None

    return round(ratio * 100, 1), duration_min


def format_alert_message(masa_no: int, negative_pct: float, duration_min: int) -> str:
    return (
        f"Masa {masa_no} · %{negative_pct:.0f} olumsuz duygu · "
        f"{duration_min} dakikadır"
    )


def _create_alert(masa_no: int, negative_pct: float, duration_min: int) -> int:
    with db_session() as conn:
        cur = conn.execute(
            """
            INSERT INTO DuyguUyari(masa_no, negatif_yuzde, sure_dakika, okundu)
            VALUES (?, ?, ?, 0)
            """,
            (masa_no, negative_pct, duration_min),
        )
        return int(cur.lastrowid)


def _should_cooldown(masa_no: int) -> bool:
    last = _last_alert_at.get(masa_no)
    if last is None:
        return False
    elapsed_min = (time.time() - last) / 60.0
    return elapsed_min < EMOTION_ALERT_COOLDOWN_MIN


def _check_and_alert() -> None:
    for masa_no in _fetch_active_masalar():
        if _should_cooldown(masa_no):
            continue
        result = _analyze_masa(masa_no)
        if result is None:
            continue
        negative_pct, duration_min = result
        alert_id = _create_alert(masa_no, negative_pct, duration_min)
        _last_alert_at[masa_no] = time.time()

        title = "⚠ Duygu uyarısı"
        body = format_alert_message(masa_no, negative_pct, duration_min)
        notify_waiters(
            title,
            body,
            {
                "type": "emotion_alert",
                "alertId": alert_id,
                "tableNumber": masa_no,
                "negativePercent": negative_pct,
                "durationMinutes": duration_min,
            },
        )
        logger.info("Emotion alert masa %s: %s", masa_no, body)


async def emotion_watcher_loop() -> None:
    logger.info(
        "Emotion watcher started (window=%sm, threshold=%.0f%%, min_duration=%sm, poll=%ss)",
        EMOTION_ALERT_WINDOW_MIN,
        EMOTION_NEGATIVE_THRESHOLD * 100,
        EMOTION_MIN_DURATION_MIN,
        POLL_SECONDS,
    )
    while True:
        try:
            await asyncio.sleep(POLL_SECONDS)
            _check_and_alert()
        except asyncio.CancelledError:
            raise
        except Exception as exc:
            logger.warning("Emotion watcher error: %s", exc)


def start_emotion_watcher() -> asyncio.Task[Any]:
    return asyncio.create_task(emotion_watcher_loop())
