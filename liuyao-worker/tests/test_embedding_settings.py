import os
import unittest
from unittest.mock import patch

from app.config.settings import load_settings
from app.embedding.factory import create_embedder
from app.embedding.http_embedder import HttpEmbedder
from app.embedding.mock_embedder import MockEmbedder


class EmbeddingSettingsTest(unittest.TestCase):
    def test_load_settings_defaults_to_dashscope(self) -> None:
        with patch.dict(
            os.environ,
            {
                "DASHSCOPE_API_KEY": "sk-test",
            },
            clear=True,
        ):
            settings = load_settings()

        self.assertEqual(settings.embedding_provider, "dashscope")
        self.assertEqual(settings.embedding_model, "text-embedding-v4")
        self.assertEqual(settings.embedding_api_key, "sk-test")
        self.assertEqual(settings.embedding_dim, None)

    def test_load_settings_normalizes_mock_provider(self) -> None:
        with patch.dict(
            os.environ,
            {
                "LIUYAO_EMBEDDING_PROVIDER": " Mock ",
                "LIUYAO_EMBEDDING_MODEL": " mock-8d-v1 ",
            },
            clear=True,
        ):
            settings = load_settings()

        self.assertEqual(settings.embedding_provider, "mock")
        self.assertEqual(settings.embedding_model, "mock-8d-v1")
        self.assertEqual(settings.embedding_dim, 8)

    def test_load_settings_leaves_embedding_dim_empty_for_real_provider(self) -> None:
        with patch.dict(
            os.environ,
            {
                "LIUYAO_EMBEDDING_PROVIDER": "dashscope",
                "LIUYAO_EMBEDDING_MODEL": "text-embedding-v4",
                "DASHSCOPE_API_KEY": "sk-test",
            },
            clear=False,
        ):
            settings = load_settings()

        self.assertEqual(settings.embedding_dim, None)

    def test_load_settings_requires_http_provider_base_url(self) -> None:
        with patch.dict(os.environ, {"LIUYAO_EMBEDDING_PROVIDER": "http"}, clear=False):
            with self.assertRaisesRegex(RuntimeError, "LIUYAO_EMBEDDING_BASE_URL is required"):
                load_settings()

    def test_create_embedder_returns_http_embedder_without_api_key(self) -> None:
        embedder = create_embedder(
            provider="http",
            model="text-embedding-3-small",
            dim=8,
            base_url="https://example.com/v1",
            api_key=None,
            timeout_seconds=10,
        )

        self.assertIsInstance(embedder, HttpEmbedder)

    def test_create_embedder_returns_mock_embedder(self) -> None:
        embedder = create_embedder(
            provider="mock",
            model="mock-8d-v1",
            dim=8,
            base_url=None,
            api_key=None,
            timeout_seconds=10,
        )

        self.assertIsInstance(embedder, MockEmbedder)


if __name__ == "__main__":
    unittest.main()
