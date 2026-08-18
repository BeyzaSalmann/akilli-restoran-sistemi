from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi

from .config import DB_PATH, MENU_CSV, ORDERS_CSV
from .database import db_session, ensure_schema
from .notifications.emotion_watcher import start_emotion_watcher
from .notifications.order_watcher import start_order_watcher
from .recommendations.apriori_engine import AprioriEngine
from .recommendations.menu_sync import repair_orphan_order_details, sync_urun_from_csv
from .recommendations.service import init_engine
from .routes import auth, emotion, menu, notifications, orders, recommendations, tables

PUBLIC_PATHS = {"/api/auth/login", "/api/health"}


@asynccontextmanager
async def lifespan(_: FastAPI):
    app.openapi_schema = None
    with db_session() as conn:
        ensure_schema(conn)
        count = sync_urun_from_csv(conn, MENU_CSV)
        print(f"Menu synced — {count} products from CSV")
        repaired = repair_orphan_order_details(conn)
        if repaired:
            print(f"Repaired {repaired} legacy order line(s)")
        engine = AprioriEngine(MENU_CSV, ORDERS_CSV)
        engine.load_csv_transactions()
        engine.load_db_transactions(conn)
        engine.train()
        init_engine(engine)
        print(f"Apriori ready — {len(engine.rules)} rules, {len(engine.transactions)} transactions")
    print(f"ARS API ready — DB: {DB_PATH}")
    watcher_task = start_order_watcher()
    emotion_task = start_emotion_watcher()
    yield
    watcher_task.cancel()
    emotion_task.cancel()
    try:
        await watcher_task
    except Exception:
        pass
    try:
        await emotion_task
    except Exception:
        pass


app = FastAPI(
    title="ARS Waiter API",
    version="3.0.0",
    lifespan=lifespan,
    swagger_ui_parameters={"persistAuthorization": True},
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(auth.router, prefix="/api")
app.include_router(tables.router, prefix="/api")
app.include_router(menu.router, prefix="/api")
app.include_router(orders.router, prefix="/api")
app.include_router(recommendations.router, prefix="/api")
app.include_router(notifications.router, prefix="/api")
app.include_router(emotion.router, prefix="/api")


@app.get("/api/health")
def health():
    return {"status": "ok"}


def custom_openapi():
    if app.openapi_schema:
        return app.openapi_schema

    schema = get_openapi(
        title=app.title,
        version=app.version,
        routes=app.routes,
    )
    schema.setdefault("components", {})
    schema["components"]["securitySchemes"] = {
        "BearerAuth": {
            "type": "http",
            "scheme": "bearer",
            "description": "POST /api/auth/login ile alınan token. Sadece token değerini yapıştırın.",
        }
    }

    for path, methods in schema.get("paths", {}).items():
        if path in PUBLIC_PATHS:
            continue
        for method in methods.values():
            if isinstance(method, dict) and "operationId" in method:
                method["security"] = [{"BearerAuth": []}]

    app.openapi_schema = schema
    return schema


app.openapi = custom_openapi
