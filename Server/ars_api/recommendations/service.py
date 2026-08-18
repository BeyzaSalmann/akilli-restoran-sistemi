from .apriori_engine import AprioriEngine

_engine: AprioriEngine | None = None


def get_engine() -> AprioriEngine | None:
    return _engine


def init_engine(engine: AprioriEngine) -> None:
    global _engine
    _engine = engine
