import secrets
from typing import Annotated

from fastapi import APIRouter, Depends, HTTPException, Security
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer

from ..config import WAITER_NAME, WAITER_PIN
from ..models.schemas import LoginRequest, LoginResponse
from ..token_store import add_token, has_token

router = APIRouter(prefix="/auth", tags=["auth"])

bearer_scheme = HTTPBearer()


def verify_token(
    credentials: Annotated[HTTPAuthorizationCredentials, Security(bearer_scheme)],
) -> str:
    token = credentials.credentials
    if not has_token(token):
        raise HTTPException(status_code=401, detail="Invalid token")
    return token


@router.post("/login", response_model=LoginResponse)
def login(body: LoginRequest) -> LoginResponse:
    if body.pin != WAITER_PIN:
        raise HTTPException(status_code=401, detail="Invalid PIN")
    token = secrets.token_urlsafe(32)
    add_token(token)
    return LoginResponse(token=token, waiterName=WAITER_NAME)
