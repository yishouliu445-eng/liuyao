from openai import OpenAI

from app.embedding.base_embedder import BaseEmbedder


DEFAULT_DASHSCOPE_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1"
MAX_DASHSCOPE_BATCH_SIZE = 10


class DashScopeEmbedder(BaseEmbedder):
    def __init__(self,
                 api_key: str,
                 model: str,
                 base_url: str | None,
                 timeout_seconds: int):
        self.model = model
        self.client = OpenAI(
            api_key=api_key,
            base_url=(base_url or DEFAULT_DASHSCOPE_BASE_URL).rstrip("/"),
            timeout=timeout_seconds,
        )

    def embed(self, text: str) -> list[float]:
        return self.embed_many([text])[0]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        embeddings: list[list[float]] = []
        for start in range(0, len(texts), MAX_DASHSCOPE_BATCH_SIZE):
            batch = texts[start:start + MAX_DASHSCOPE_BATCH_SIZE]
            response = self.client.embeddings.create(
                model=self.model,
                input=batch,
            )
            try:
                batch_embeddings = [item.embedding for item in response.data]
            except (AttributeError, IndexError, TypeError) as exc:
                raise RuntimeError("DashScope embedding response missing data[].embedding") from exc
            if len(batch_embeddings) != len(batch):
                raise RuntimeError(
                    f"DashScope embedding response count mismatch: expected {len(batch)}, got {len(batch_embeddings)}"
                )
            embeddings.extend(batch_embeddings)
        return embeddings
