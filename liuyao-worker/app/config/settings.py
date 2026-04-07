import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    db_dsn: str
    worker_id: str = "python-worker-1"
    poll_interval_seconds: int = 5
    embedding_provider: str = "dashscope"
    embedding_model: str = "text-embedding-v4"
    embedding_dim: int | None = None
    embedding_base_url: str | None = None
    embedding_api_key: str | None = None
    embedding_timeout_seconds: int = 30
    embedding_batch_size: int = 16
    vector_store_dim: int = 1024


def load_settings() -> Settings:
    dsn = os.getenv("LIUYAO_DB_DSN", "postgresql://dify:password123@localhost:5432/liuyao")
    embedding_provider = os.getenv("LIUYAO_EMBEDDING_PROVIDER", "dashscope").strip().lower()
    embedding_model = os.getenv("LIUYAO_EMBEDDING_MODEL", "text-embedding-v4").strip()
    embedding_dim = _read_optional_int_env("LIUYAO_EMBEDDING_DIM")
    embedding_base_url = _read_optional_env("LIUYAO_EMBEDDING_BASE_URL")
    embedding_api_key = _read_optional_env("LIUYAO_EMBEDDING_API_KEY")
    if embedding_provider == "mock" and embedding_dim is None:
        embedding_dim = 8
    if embedding_provider == "dashscope" and not embedding_api_key:
        embedding_api_key = _read_optional_env("DASHSCOPE_API_KEY")
    if embedding_provider == "dashscope" and not embedding_api_key:
        raise RuntimeError("DASHSCOPE_API_KEY or LIUYAO_EMBEDDING_API_KEY is required when LIUYAO_EMBEDDING_PROVIDER=dashscope")
    if embedding_provider == "http" and not embedding_base_url:
        raise RuntimeError("LIUYAO_EMBEDDING_BASE_URL is required when LIUYAO_EMBEDDING_PROVIDER=http")
    return Settings(
        db_dsn=dsn,
        worker_id=os.getenv("LIUYAO_WORKER_ID", "python-worker-1"),
        poll_interval_seconds=int(os.getenv("LIUYAO_POLL_INTERVAL_SECONDS", "5")),
        embedding_provider=embedding_provider,
        embedding_model=embedding_model,
        embedding_dim=embedding_dim,
        embedding_base_url=embedding_base_url,
        embedding_api_key=embedding_api_key,
        embedding_timeout_seconds=int(os.getenv("LIUYAO_EMBEDDING_TIMEOUT_SECONDS", "30")),
        embedding_batch_size=int(os.getenv("LIUYAO_EMBEDDING_BATCH_SIZE", "16")),
        vector_store_dim=int(os.getenv("LIUYAO_VECTOR_STORE_DIM", "1024")),
    )


def _read_optional_env(name: str) -> str | None:
    value = os.getenv(name)
    if value is None:
        return None
    stripped = value.strip()
    return stripped or None


def _read_optional_int_env(name: str) -> int | None:
    value = _read_optional_env(name)
    if value is None:
        return None
    return int(value)
