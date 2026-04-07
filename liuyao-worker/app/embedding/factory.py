from app.embedding.base_embedder import BaseEmbedder
from app.embedding.dashscope_embedder import DashScopeEmbedder
from app.embedding.http_embedder import HttpEmbedder
from app.embedding.mock_embedder import MockEmbedder


def create_embedder(provider: str,
                    model: str,
                    dim: int | None,
                    base_url: str | None,
                    api_key: str | None,
                    timeout_seconds: int) -> BaseEmbedder:
    provider = provider.strip().lower()
    if provider == "mock":
        if dim is None:
            raise RuntimeError("Mock embedding provider requires dim")
        return MockEmbedder(dim)
    if provider == "dashscope":
        if not api_key:
            raise RuntimeError("DashScope embedding provider requires api_key")
        return DashScopeEmbedder(
            api_key=api_key,
            model=model,
            base_url=base_url,
            timeout_seconds=timeout_seconds,
        )
    if provider == "http":
        if not base_url:
            raise RuntimeError("HTTP embedding provider requires base_url")
        return HttpEmbedder(base_url=base_url, api_key=api_key, model=model, timeout_seconds=timeout_seconds)
    raise RuntimeError(f"Unsupported embedding provider: {provider}")
