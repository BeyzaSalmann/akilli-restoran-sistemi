from fastapi import APIRouter, Depends

from ..models.schemas import PushTokenRegister
from ..notifications.push_service import register_push_token, unregister_push_token
from .auth import verify_token

router = APIRouter(prefix="/notifications", tags=["notifications"])


@router.post("/register", dependencies=[Depends(verify_token)])
def register_device(body: PushTokenRegister) -> dict[str, str]:
    register_push_token(body.pushToken, body.platform)
    return {"status": "registered"}


@router.delete("/register", dependencies=[Depends(verify_token)])
def unregister_device(body: PushTokenRegister) -> dict[str, str]:
    unregister_push_token(body.pushToken)
    return {"status": "unregistered"}
