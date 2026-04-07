import json
import unittest
from unittest.mock import patch

from app.embedding.factory import create_embedder
from app.embedding.http_embedder import HttpEmbedder


class _FakeResponse:
    def __init__(self, payload: dict):
        self.payload = payload

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def read(self) -> bytes:
        return json.dumps(self.payload).encode("utf-8")


class EmbeddingHttpTest(unittest.TestCase):
    def test_factory_allows_http_provider_without_api_key(self):
        embedder = create_embedder(
            provider=" HTTP ",
            model="text-embedding-3-small",
            dim=8,
            base_url="http://localhost:8000/v1",
            api_key=None,
            timeout_seconds=10,
        )

        self.assertIsInstance(embedder, HttpEmbedder)

    def test_http_embedder_omits_authorization_header_when_api_key_missing(self):
        captured = {}

        def fake_urlopen(request, timeout):
            captured["request"] = request
            captured["timeout"] = timeout
            return _FakeResponse({"data": [{"embedding": [0.1, 0.2, 0.3]}]})

        embedder = HttpEmbedder(
            base_url="http://localhost:8000/v1",
            api_key=None,
            model="text-embedding-3-small",
            timeout_seconds=12,
        )

        with patch("urllib.request.urlopen", side_effect=fake_urlopen):
            embedding = embedder.embed("用神宜旺相")

        self.assertEqual([0.1, 0.2, 0.3], embedding)
        self.assertEqual(12, captured["timeout"])
        self.assertEqual("http://localhost:8000/v1/embeddings", captured["request"].full_url)
        self.assertIsNone(captured["request"].get_header("Authorization"))

    def test_http_embedder_returns_multiple_embeddings_in_order(self):
        def fake_urlopen(request, timeout):
            return _FakeResponse({"data": [
                {"embedding": [0.1, 0.2]},
                {"embedding": [0.3, 0.4]},
            ]})

        embedder = HttpEmbedder(
            base_url="http://localhost:8000/v1",
            api_key=None,
            model="text-embedding-3-small",
            timeout_seconds=12,
        )

        with patch("urllib.request.urlopen", side_effect=fake_urlopen):
            embeddings = embedder.embed_many(["甲", "乙"])

        self.assertEqual([[0.1, 0.2], [0.3, 0.4]], embeddings)


if __name__ == "__main__":
    unittest.main()
