import json
from pathlib import Path

from .config import TOKEN_STORE_PATH

_tokens: set[str] = set()
_loaded = False


def _ensure_loaded() -> None:
    global _loaded
    if _loaded:
        return
    _loaded = True
    if not TOKEN_STORE_PATH.exists():
        return
    try:
        data = json.loads(TOKEN_STORE_PATH.read_text(encoding="utf-8"))
        if isinstance(data, list):
            _tokens.update(str(item) for item in data)
    except (json.JSONDecodeError, OSError):
        _tokens.clear()


def _persist() -> None:
    TOKEN_STORE_PATH.parent.mkdir(parents=True, exist_ok=True)
    TOKEN_STORE_PATH.write_text(
        json.dumps(sorted(_tokens), indent=2),
        encoding="utf-8",
    )


def all_tokens() -> set[str]:
    _ensure_loaded()
    return set(_tokens)


def add_token(token: str) -> None:
    _ensure_loaded()
    _tokens.add(token)
    _persist()


def has_token(token: str) -> bool:
    _ensure_loaded()
    return token in _tokens


def remove_token(token: str) -> None:
    _ensure_loaded()
    if token in _tokens:
        _tokens.remove(token)
        _persist()
