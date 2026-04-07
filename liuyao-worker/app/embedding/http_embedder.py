import json
import urllib.error
import urllib.request

from app.embedding.base_embedder import BaseEmbedder


class HttpEmbedder(BaseEmbedder):
    def __init__(self, base_url: str, api_key: str | None, model: str, timeout_seconds: int):
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model = model
        self.timeout_seconds = timeout_seconds

    def embed(self, text: str) -> list[float]:
        return self.embed_many([text])[0]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        headers = {
            "Content-Type": "application/json",
        }
        if self.api_key:
            headers["Authorization"] = f"Bearer {self.api_key}"
        request = urllib.request.Request(
            url=f"{self.base_url}/embeddings",
            data=json.dumps({"model": self.model, "input": texts}).encode("utf-8"),
            headers=headers,
            method="POST",
        )
        try:
            with urllib.request.urlopen(request, timeout=self.timeout_seconds) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except urllib.error.URLError as exc:
            raise RuntimeError(f"Embedding request failed: {exc}") from exc

        try:
            data = payload["data"]
            embeddings = [item["embedding"] for item in data]
        except (KeyError, IndexError, TypeError) as exc:
            raise RuntimeError("Embedding response missing data[].embedding") from exc
        if len(embeddings) != len(texts):
            raise RuntimeError(f"Embedding response count mismatch: expected {len(texts)}, got {len(embeddings)}")
        return embeddings
