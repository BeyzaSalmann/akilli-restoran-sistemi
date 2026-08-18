import logging
from typing import Any

import httpx

from ..database import db_session

logger = logging.getLogger(__name__)

EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send"


def _load_tokens() -> list[str]:
    with db_session() as conn:
        rows = conn.execute("SELECT push_token FROM DeviceToken").fetchall()
    return [row["push_token"] for row in rows]


def register_push_token(token: str, platform: str | None = None) -> None:
    with db_session() as conn:
        conn.execute(
            """
            INSERT INTO DeviceToken(push_token, platform) VALUES (?, ?)
            ON CONFLICT(push_token) DO UPDATE SET platform = excluded.platform
            """,
            (token, platform),
        )


def unregister_push_token(token: str) -> None:
    with db_session() as conn:
        conn.execute("DELETE FROM DeviceToken WHERE push_token = ?", (token,))


def notify_waiters(title: str, body: str, data: dict[str, Any] | None = None) -> int:
    tokens = _load_tokens()
    if not tokens:
        return 0

    payload = [
        {
            "to": token,
            "title": title,
            "body": body,
            "sound": "default",
            "data": data or {},
        }
        for token in tokens
    ]

    try:
        response = httpx.post(
            EXPO_PUSH_URL,
            json=payload,
            headers={"Accept": "application/json", "Content-Type": "application/json"},
            timeout=10.0,
        )
        response.raise_for_status()
        logger.info("Push sent to %d device(s): %s", len(tokens), title)
    except httpx.HTTPError as exc:
        logger.warning("Push delivery failed: %s", exc)
    return len(tokens)
