import json
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


class _FakeEnricher:
    def __init__(self):
        self.calls = []

    def enrich(self, draft: ChunkDraft) -> ChunkDraft:
        self.calls.append(draft.content)
        draft.focus_topic = "婚姻"
        draft.topic_tags = list(dict.fromkeys(draft.topic_tags + ["应期", "空亡"]))
        draft.knowledge_type = "CASE"
        draft.has_timing_prediction = True
        draft.metadata["applicable_hexagrams"] = ["天火同人"]
        draft.metadata["scenario_types"] = ["婚姻"]
        draft.metadata["liu_qin_focus"] = ["妻财"]
        draft.metadata["focus_topic"] = "婚姻"
        draft.metadata["topic_tags"] = list(draft.topic_tags)
        draft.metadata["knowledge_type"] = "CASE"
        draft.metadata["has_timing_prediction"] = True
        return draft


class BookPipelineTest(unittest.TestCase):
    def test_reads_pdf_source_with_pdf_parser(self):
        fake_embedder = _FakeEmbedder()
        drafts = [ChunkDraft(content="用神见于篇首", topic_tags=["用神"], focus_topic="用神")]

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_pdf", return_value="pdf raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.CaseExampleChunker.chunk", return_value=drafts):
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
                patch("app.pipeline.book_pipeline.CaseExampleChunker.chunk", return_value=drafts):
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
                patch("app.pipeline.book_pipeline.CaseExampleChunker.chunk", return_value=drafts):
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

    def test_preserves_enriched_chunk_metadata(self):
        fake_embedder = _FakeEmbedder()
        draft = ChunkDraft(
            content="如占求财，断曰财可得。",
            topic_tags=["用神"],
            focus_topic="用神",
            knowledge_type="CASE",
            has_timing_prediction=True,
        )

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_txt", return_value="raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.CaseExampleChunker.chunk", return_value=[draft]):
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
                BookRecord(book_id=1, title="增删卜易", author="野鹤老人", source_type="TXT", file_path="/tmp/x.txt", file_size=1),
            )

        self.assertEqual("CASE", records[0].knowledge_type)
        self.assertTrue(records[0].has_timing_prediction)

    def test_applies_metadata_enricher_before_serializing_records(self):
        fake_embedder = _FakeEmbedder()
        fake_enricher = _FakeEnricher()
        draft = ChunkDraft(content="如占婚姻，断曰可成。", topic_tags=["用神"], focus_topic="用神")

        with patch("app.pipeline.book_pipeline.create_embedder", return_value=fake_embedder), \
                patch("app.pipeline.book_pipeline.parse_txt", return_value="raw"), \
                patch("app.pipeline.book_pipeline.clean_text", return_value="cleaned"), \
                patch("app.pipeline.book_pipeline.CaseExampleChunker.chunk", return_value=[draft]), \
                patch("app.pipeline.book_pipeline.MetadataEnricher", return_value=fake_enricher):
            pipeline = BookPipeline(
                embedding_dim=None,
                embedding_batch_size=8,
                embedding_provider="mock",
                embedding_model="mock-8d-v1",
                embedding_base_url=None,
                embedding_api_key="sk-test",
                embedding_timeout_seconds=30,
                vector_store_dim=4,
                llm_api_key="sk-llm",
                llm_model="mock-llm",
                llm_base_url=None,
            )
            records = pipeline.process(
                TaskRecord(task_id=1, book_id=1, status="PENDING", payload_json="{}"),
                BookRecord(book_id=1, title="增删卜易", author="野鹤老人", source_type="TXT", file_path="/tmp/x.txt", file_size=1),
            )

        self.assertEqual(["如占婚姻，断曰可成。"], fake_enricher.calls)
        self.assertEqual("婚姻", records[0].focus_topic)
        self.assertEqual("CASE", records[0].knowledge_type)
        self.assertTrue(records[0].has_timing_prediction)
        self.assertEqual({"用神", "应期", "空亡"}, set(json.loads(records[0].topic_tags_json)))
        metadata = json.loads(records[0].metadata_json)
        self.assertEqual(["天火同人"], metadata["applicable_hexagrams"])
        self.assertEqual(["婚姻"], metadata["scenario_types"])
        self.assertEqual(["妻财"], metadata["liu_qin_focus"])
        self.assertEqual("婚姻", metadata["focus_topic"])
        self.assertEqual("CASE", metadata["knowledge_type"])
        self.assertTrue(metadata["has_timing_prediction"])


if __name__ == "__main__":
    unittest.main()
