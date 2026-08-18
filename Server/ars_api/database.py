import sqlite3
from contextlib import contextmanager
from typing import Generator

from .config import DB_PATH

# API status ↔ desktop DB (Turkish) values
TABLE_STATUS_TO_DB = {
    "empty": "Boş",
    "occupied": "Dolu",
    "bill_requested": "Hesap İstendi",
}
TABLE_STATUS_FROM_DB = {v: k for k, v in TABLE_STATUS_TO_DB.items()}

ORDER_STATUS_TO_DB = {
    "preparing": "Hazırlanıyor",
    "served": "Servis Edildi",
    "completed": "Tamamlandı",
    "cancelled": "İptal Edildi",
}
ORDER_STATUS_FROM_DB = {v: k for k, v in ORDER_STATUS_TO_DB.items()}


def get_connection() -> sqlite3.Connection:
    conn = sqlite3.connect(DB_PATH, check_same_thread=False)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA foreign_keys=ON;")
    return conn


@contextmanager
def db_session() -> Generator[sqlite3.Connection, None, None]:
    conn = get_connection()
    try:
        yield conn
        conn.commit()
    except Exception:
        conn.rollback()
        raise
    finally:
        conn.close()


def ensure_schema(conn: sqlite3.Connection) -> None:
    conn.executescript(
        """
        CREATE TABLE IF NOT EXISTS Musteri (
            musteriID INTEGER PRIMARY KEY AUTOINCREMENT,
            duyguDurumu TEXT NOT NULL,
            siparisZamani DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS Menu (
            menuID INTEGER PRIMARY KEY AUTOINCREMENT,
            duyguKategorisi TEXT NOT NULL,
            urunAdi TEXT NOT NULL,
            fiyat REAL NOT NULL
        );
        CREATE TABLE IF NOT EXISTS Urun (
            urun_id INTEGER PRIMARY KEY AUTOINCREMENT,
            urun_adi TEXT NOT NULL,
            kategori TEXT NOT NULL,
            fiyat REAL NOT NULL
        );
        CREATE TABLE IF NOT EXISTS Masa (
            masa_id INTEGER PRIMARY KEY,
            masa_no INTEGER UNIQUE NOT NULL,
            durum TEXT DEFAULT 'Boş',
            olusturma_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS Siparis (
            siparis_id INTEGER PRIMARY KEY AUTOINCREMENT,
            siparis_tarihi DATETIME DEFAULT CURRENT_TIMESTAMP,
            toplam_tutar REAL NOT NULL,
            masa_no INTEGER,
            durum TEXT DEFAULT 'Hazırlanıyor'
        );
        CREATE TABLE IF NOT EXISTS SiparisDetay (
            detay_id INTEGER PRIMARY KEY AUTOINCREMENT,
            siparis_id INTEGER NOT NULL,
            urun_id INTEGER NOT NULL,
            adet INTEGER NOT NULL,
            birim_fiyat REAL NOT NULL,
            toplam_fiyat REAL NOT NULL,
            FOREIGN KEY (siparis_id) REFERENCES Siparis(siparis_id),
            FOREIGN KEY (urun_id) REFERENCES Urun(urun_id)
        );
        CREATE TABLE IF NOT EXISTS DeviceToken (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            push_token TEXT NOT NULL UNIQUE,
            platform TEXT,
            created_at DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS DuyguOlcum (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            masa_no INTEGER NOT NULL,
            duygu_durumu TEXT NOT NULL,
            guven REAL,
            olcum_zamani DATETIME DEFAULT CURRENT_TIMESTAMP
        );
        CREATE TABLE IF NOT EXISTS DuyguUyari (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            masa_no INTEGER NOT NULL,
            negatif_yuzde REAL NOT NULL,
            sure_dakika INTEGER NOT NULL,
            olusturma_zamani DATETIME DEFAULT CURRENT_TIMESTAMP,
            okundu INTEGER DEFAULT 0
        );
        """
    )

    _seed_emotion_menu(conn)

    row = conn.execute("SELECT COUNT(*) FROM Masa").fetchone()
    if row[0] == 0:
        for n in range(1, 11):
            conn.execute(
                "INSERT OR IGNORE INTO Masa(masa_no, durum) VALUES (?, 'Boş')",
                (n,),
            )


def _seed_emotion_menu(conn: sqlite3.Connection) -> None:
    row = conn.execute("SELECT COUNT(*) FROM Menu").fetchone()
    if row[0] > 0:
        return
    samples = [
        ("mutlu", "Cheesecake", 9.99),
        ("mutlu", "Baklava", 8.50),
        ("notr", "Hamburger", 12.95),
        ("notr", "French Fries", 7.00),
        ("uzgun", "Mac & Cheese", 7.00),
        ("uzgun", "Hot Dog", 9.00),
    ]
    conn.executemany(
        "INSERT INTO Menu(duyguKategorisi, urunAdi, fiyat) VALUES (?, ?, ?)",
        samples,
    )


def table_status_from_db(value: str | None) -> str:
    if not value:
        return "empty"
    return TABLE_STATUS_FROM_DB.get(value, "occupied")


def table_status_to_db(value: str) -> str:
    if value not in TABLE_STATUS_TO_DB:
        raise ValueError(f"Invalid table status: {value}")
    return TABLE_STATUS_TO_DB[value]


def order_status_from_db(value: str | None) -> str:
    if not value:
        return "preparing"
    return ORDER_STATUS_FROM_DB.get(value, "preparing")


def order_status_to_db(value: str) -> str:
    if value not in ORDER_STATUS_TO_DB:
        raise ValueError(f"Invalid order status: {value}")
    return ORDER_STATUS_TO_DB[value]


def resolve_table(conn: sqlite3.Connection, table_id: int) -> sqlite3.Row | None:
    row = conn.execute(
        "SELECT masa_id, masa_no, durum FROM Masa WHERE masa_id = ?",
        (table_id,),
    ).fetchone()
    if row:
        return row
    return conn.execute(
        "SELECT masa_id, masa_no, durum FROM Masa WHERE masa_no = ?",
        (table_id,),
    ).fetchone()
