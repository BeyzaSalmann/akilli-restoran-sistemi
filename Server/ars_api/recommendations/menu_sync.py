import csv
import sqlite3
from pathlib import Path


def sync_urun_from_csv(conn: sqlite3.Connection, menu_csv_path: Path) -> int:
    """
    Sync Urun table from menu_items.csv.
    urun_id = menu_item_id so Apriori CSV item_id aligns with the live menu.
    """
    rows: list[tuple[int, str, str, float]] = []
    with open(menu_csv_path, encoding="utf-8-sig") as f:
        reader = csv.DictReader(f)
        for row in reader:
            try:
                item_id = int(float(row["menu_item_id"]))
                name = row["item_name"].strip()
                category = row.get("category", "").strip() or "General"
                price = float(row["price"])
                rows.append((item_id, name, category, price))
            except (ValueError, KeyError):
                continue

    if not rows:
        return 0

    csv_ids = {r[0] for r in rows}
    placeholders = ",".join("?" * len(csv_ids))

    conn.execute("PRAGMA foreign_keys=OFF")
    try:
        conn.execute(
            f"DELETE FROM Urun WHERE urun_id NOT IN ({placeholders})",
            tuple(csv_ids),
        )
        conn.executemany(
            """
            INSERT INTO Urun(urun_id, urun_adi, kategori, fiyat) VALUES (?, ?, ?, ?)
            ON CONFLICT(urun_id) DO UPDATE SET
                urun_adi = excluded.urun_adi,
                kategori = excluded.kategori,
                fiyat = excluded.fiyat
            """,
            rows,
        )
    finally:
        conn.execute("PRAGMA foreign_keys=ON")

    return len(rows)


def repair_orphan_order_details(conn: sqlite3.Connection) -> int:
    """
    Remove order lines that reference deleted menu ids and recalculate totals.
    Keeps historical orders consistent after CSV menu sync.
    """
    orphans = conn.execute(
        """
        SELECT sd.detay_id, sd.siparis_id
        FROM SiparisDetay sd
        LEFT JOIN Urun u ON sd.urun_id = u.urun_id
        WHERE u.urun_id IS NULL
        """
    ).fetchall()
    if not orphans:
        return 0

    affected: set[int] = set()
    for row in orphans:
        conn.execute("DELETE FROM SiparisDetay WHERE detay_id = ?", (row["detay_id"],))
        affected.add(row["siparis_id"])

    for siparis_id in affected:
        total = conn.execute(
            "SELECT COALESCE(SUM(toplam_fiyat), 0) FROM SiparisDetay WHERE siparis_id = ?",
            (siparis_id,),
        ).fetchone()[0]
        conn.execute(
            "UPDATE Siparis SET toplam_tutar = ? WHERE siparis_id = ?",
            (total, siparis_id),
        )

    return len(orphans)

