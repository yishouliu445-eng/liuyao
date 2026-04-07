import unittest
from unittest.mock import patch

from app.pipeline.book_pipeline import BookPipeline
from app.schemas.chunk_models import ChunkDraft
from app.schemas.task_models import BookRecord, TaskRecord


class _FakeEmbedder:
    def __init__(self):
        self.calls = []

    def embed_many(self, texts: list[str]) -> list[list[float]]:
        self.calls.append(list(texts))
        return [[0.1] * 4 for _ in texts]


class BookPipelineTest(unittest.TestCase):
    def test_reads_pdf_source_with_pdf_parser(self):
        fake_embedder = _FakeEmbedder()
        drafts = [ChunkDraft(content="用神见于篇首", topic_tags=["用神"], focus_topic="用神")]

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_pdf", return_value="pdf raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.ZengshanBuyiChunker.chunk", return_value=drafts):
            pipeline = BookPipeline(
                embedding_dim=None,
                embedding_batch_size=8,
                embedding_provider="mock",
                embedding_model="mock-8d-v1",
                embedding_base_url=None,
                embedding_api_key=None,
                embedding_timeout_seconds=30,
                vector_store_dim=4,
            )
            records = pipeline.process(
                TaskRecord(task_id=1, book_id=1, status="PENDING", payload_json="{}"),
                BookRecord(book_id=1, title="六爻预测学", author="黎光", source_type="PDF", file_path="/tmp/x.pdf", file_size=1),
            )

        self.assertEqual(1, len(records))
        self.assertEqual([["用神见于篇首"]], fake_embedder.calls)

    def test_batches_embedding_requests(self):
        fake_embedder = _FakeEmbedder()
        drafts = [
            ChunkDraft(content="用神一", topic_tags=["用神"], focus_topic="用神"),
            ChunkDraft(content="世应二", topic_tags=["世应"], focus_topic="世应"),
            ChunkDraft(content="六亲三", topic_tags=["六亲"], focus_topic="六亲"),
        ]

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_txt", return_value="raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.ZengshanBuyiChunker.chunk", return_value=drafts):
            pipeline = BookPipeline(
                embedding_dim=None,
                embedding_batch_size=2,
                embedding_provider="dashscope",
                embedding_model="text-embedding-v4",
                embedding_base_url=None,
                embedding_api_key="sk-test",
                embedding_timeout_seconds=30,
                vector_store_dim=4,
            )
            records = pipeline.process(
                TaskRecord(task_id=1, book_id=1, status="PENDING", payload_json="{}"),
                BookRecord(book_id=1, title="增删卜易", author="野鹤老人", source_type="TXT", file_path="/tmp/x.txt", file_size=1),
            )

        self.assertEqual([["用神一", "世应二"], ["六亲三"]], fake_embedder.calls)
        self.assertEqual(3, len(records))
        self.assertEqual("[0.1,0.1,0.1,0.1]", records[0].embedding_vector_literal)

    def test_skips_vector_literal_when_embedding_dim_does_not_match_store_dim(self):
        fake_embedder = _FakeEmbedder()
        drafts = [ChunkDraft(content="用神一", topic_tags=["用神"], focus_topic="用神")]

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_txt", return_value="raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.ZengshanBuyiChunker.chunk", return_value=drafts):
            pipeline = BookPipeline(
                embedding_dim=None,
                embedding_batch_size=8,
                embedding_provider="dashscope",
                embedding_model="text-embedding-v4",
                embedding_base_url=None,
                embedding_api_key="sk-test",
                embedding_timeout_seconds=30,
                vector_store_dim=1024,
            )
            records = pipeline.process(
                TaskRecord(task_id=1, book_id=1, status="PENDING", payload_json="{}"),
                BookRecord(book_id=1, title="增删卜易", author="野鹤老人", source_type="TXT", file_path="/tmp/x.txt", file_size=1),
            )

        self.assertIsNone(records[0].embedding_vector_literal)


if __name__ == "__main__":
    unittest.main()
