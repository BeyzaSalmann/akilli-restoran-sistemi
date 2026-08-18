import os
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_DB = (
    PROJECT_ROOT
    / "DesktopApp"
    / "AkilliRestoranSistemi son hal"
    / "AkilliRestoranSistemi"
    / "restoran.db"
)

DB_PATH = os.getenv("ARS_DB_PATH", str(DEFAULT_DB))
HOST = os.getenv("ARS_API_HOST", "0.0.0.0")
PORT = int(os.getenv("ARS_API_PORT", "8000"))

SIPARIS_TAHMIN_DIR = PROJECT_ROOT / "SiparisTahmin"
MENU_CSV = SIPARIS_TAHMIN_DIR / "menu_items.csv"
ORDERS_CSV = SIPARIS_TAHMIN_DIR / "order_details.csv"

# MVP waiter auth
WAITER_PIN = os.getenv("ARS_WAITER_PIN", "1234")
WAITER_NAME = os.getenv("ARS_WAITER_NAME", "Garson")

TOKEN_STORE_PATH = Path(
    os.getenv(
        "ARS_TOKEN_FILE",
        str(Path(__file__).resolve().parent / ".auth_tokens.json"),
    )
)

# Duygu uyarı eşikleri (Phase 5)
EMOTION_ALERT_WINDOW_MIN = int(os.getenv("ARS_EMOTION_ALERT_WINDOW_MIN", "3"))
EMOTION_NEGATIVE_THRESHOLD = float(os.getenv("ARS_EMOTION_NEGATIVE_THRESHOLD", "0.6"))
EMOTION_ALERT_COOLDOWN_MIN = int(os.getenv("ARS_EMOTION_ALERT_COOLDOWN_MIN", "5"))
EMOTION_MIN_SAMPLES = int(os.getenv("ARS_EMOTION_MIN_SAMPLES", "2"))
EMOTION_MIN_DURATION_MIN = int(os.getenv("ARS_EMOTION_MIN_DURATION_MIN", "2"))
