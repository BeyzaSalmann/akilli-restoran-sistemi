from fastapi import APIRouter, Depends

from ..database import db_session
from ..models.schemas import MenuItem
from .auth import verify_token

router = APIRouter(prefix="/menu", tags=["menu"], dependencies=[Depends(verify_token)])


@router.get("/items", response_model=list[MenuItem])
def list_menu_items() -> list[MenuItem]:
    with db_session() as conn:
        rows = conn.execute(
            "SELECT urun_id, urun_adi, kategori, fiyat FROM Urun ORDER BY kategori, urun_adi"
        ).fetchall()
        return [
            MenuItem(
                id=r["urun_id"],
                name=r["urun_adi"],
                category=r["kategori"],
                price=r["fiyat"],
            )
            for r in rows
        ]
