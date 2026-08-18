from fastapi import APIRouter, Depends, HTTPException, Query

from ..database import db_session
from ..models.schemas import RecommendationItem, RecommendationsResponse
from ..recommendations.service import get_engine
from .auth import verify_token

router = APIRouter(
    prefix="/recommendations",
    tags=["recommendations"],
    dependencies=[Depends(verify_token)],
)


@router.get("", response_model=RecommendationsResponse)
def get_recommendations(
    productIds: str = Query(..., description="Comma-separated urun_id list, e.g. 101,117"),
) -> RecommendationsResponse:
    engine = get_engine()
    if engine is None:
        raise HTTPException(status_code=503, detail="Recommendation engine not ready")

    try:
        ids = [int(x.strip()) for x in productIds.split(",") if x.strip()]
    except ValueError as exc:
        raise HTTPException(status_code=400, detail="Invalid productIds") from exc

    if not ids:
        return RecommendationsResponse(items=[])

    items: list[RecommendationItem] = []
    seen: set[int] = set()

    with db_session() as conn:
        cart_names: list[str] = []
        for pid in ids:
            row = conn.execute(
                "SELECT urun_adi FROM Urun WHERE urun_id = ?", (pid,)
            ).fetchone()
            if row:
                cart_names.append(row["urun_adi"])

        if not cart_names:
            return RecommendationsResponse(items=[])

        raw = engine.recommend(cart_names, limit=10)

        for name, confidence in raw:
            row = conn.execute(
                "SELECT urun_id, urun_adi, fiyat FROM Urun WHERE urun_adi = ?",
                (name,),
            ).fetchone()
            if not row or row["urun_id"] in ids or row["urun_id"] in seen:
                continue
            seen.add(row["urun_id"])
            items.append(
                RecommendationItem(
                    productId=row["urun_id"],
                    name=row["urun_adi"],
                    price=row["fiyat"],
                    confidence=round(confidence, 3),
                )
            )
            if len(items) >= 5:
                break

    return RecommendationsResponse(items=items)
