import unittest
from types import SimpleNamespace
from unittest.mock import patch

from app.config.settings import load_settings
from app.embedding.dashscope_embedder import DEFAULT_DASHSCOPE_BASE_URL, DashScopeEmbedder, MAX_DASHSCOPE_BATCH_SIZE
from app.embedding.factory import create_embedder


class DashScopeEmbedderTest(unittest.TestCase):
    def test_factory_creates_dashscope_embedder(self):
        with patch("app.embedding.dashscope_embedder.OpenAI") as openai_cls:
            embedder = create_embedder(
                provider="dashscope",
                model="text-embedding-v4",
                dim=1024,
                base_url=None,
                api_key="sk-test",
                timeout_seconds=15,
            )

        self.assertIsInstance(embedder, DashScopeEmbedder)
        openai_cls.assert_called_once_with(
            api_key="sk-test",
            base_url=DEFAULT_DASHSCOPE_BASE_URL,
            timeout=15,
        )

    def test_embed_reads_first_embedding_vector(self):
        fake_client = SimpleNamespace(
            embeddings=SimpleNamespace(
                create=lambda model, input: SimpleNamespace(
                    data=[SimpleNamespace(embedding=[0.11, 0.22])]
                )
            )
        )
        with patch("app.embedding.dashscope_embedder.OpenAI", return_value=fake_client):
            embedder = DashScopeEmbedder(
                api_key="sk-test",
                model="text-embedding-v4",
                base_url=None,
                timeout_seconds=20,
            )

        self.assertEqual([0.11, 0.22], embedder.embed("衣服的质量杠杠的"))

    def test_embed_many_reads_multiple_embeddings(self):
        fake_client = SimpleNamespace(
            embeddings=SimpleNamespace(
                create=lambda model, input: SimpleNamespace(
                    data=[
                        SimpleNamespace(embedding=[0.11, 0.22]),
                        SimpleNamespace(embedding=[0.33, 0.44]),
                    ]
                )
            )
        )
        with patch("app.embedding.dashscope_embedder.OpenAI", return_value=fake_client):
            embedder = DashScopeEmbedder(
                api_key="sk-test",
                model="text-embedding-v4",
                base_url=None,
                timeout_seconds=20,
            )

        self.assertEqual([[0.11, 0.22], [0.33, 0.44]], embedder.embed_many(["甲", "乙"]))

    def test_embed_many_splits_large_batches(self):
        calls = []

        def fake_create(model, input):
            calls.append(list(input))
            return SimpleNamespace(
                data=[SimpleNamespace(embedding=[float(index)]) for index, _ in enumerate(input, start=1)]
            )

        fake_client = SimpleNamespace(embeddings=SimpleNamespace(create=fake_create))
        with patch("app.embedding.dashscope_embedder.OpenAI", return_value=fake_client):
            embedder = DashScopeEmbedder(
                api_key="sk-test",
                model="text-embedding-v4",
                base_url=None,
                timeout_seconds=20,
            )

        texts = [str(index) for index in range(MAX_DASHSCOPE_BATCH_SIZE + 2)]
        embeddings = embedder.embed_many(texts)

        self.assertEqual(2, len(calls))
        self.assertEqual(MAX_DASHSCOPE_BATCH_SIZE, len(calls[0]))
        self.assertEqual(2, len(calls[1]))
        self.assertEqual(len(texts), len(embeddings))

    def test_settings_fall_back_to_dashscope_api_key(self):
        env = {
            "LIUYAO_EMBEDDING_PROVIDER": "dashscope",
            "LIUYAO_EMBEDDING_MODEL": "text-embedding-v4",
            "DASHSCOPE_API_KEY": "sk-dashscope",
        }
        with patch.dict("os.environ", env, clear=False):
            settings = load_settings()

        self.assertEqual("dashscope", settings.embedding_provider)
        self.assertEqual("sk-dashscope", settings.embedding_api_key)


if __name__ == "__main__":
    unittest.main()
