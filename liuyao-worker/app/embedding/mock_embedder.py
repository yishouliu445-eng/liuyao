from app.embedding.base_embedder import BaseEmbedder


class MockEmbedder(BaseEmbedder):
    def __init__(self, dim: int):
        self.dim = dim

    def embed(self, text: str) -> list[float]:
        values = [0.0] * self.dim
        for index, char in enumerate(text):
            values[index % self.dim] += (ord(char) % 97) / 100.0
        total = sum(values) or 1.0
        return [round(value / total, 6) for value in values]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        return [self.embed(text) for text in texts]

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        return [self.embed(text) for text in texts]
