import unittest

from app.db.repositories import ChunkRepository, RuleCandidateRepository
from app.schemas.chunk_models import ChunkRecord


class _FakeTransaction:
    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False


class _RecordingCursor:
    def __init__(self, rows=None):
        self.rows = rows or []
        self.executed = []

    def __enter__(self):
        return self

    def __exit__(self, exc_type, exc, tb):
        return False

    def execute(self, sql, params=None):
        self.executed.append((sql, params))
        if params is not None:
            placeholder_count = sql.count("%s")
            if placeholder_count != len(params):
                raise AssertionError(f"placeholder mismatch: {placeholder_count} != {len(params)}")

    def fetchone(self):
        return None

    def fetchall(self):
        return list(self.rows)


class _FakeConnection:
    def __init__(self, cursor):
        self._cursor = cursor

    def transaction(self):
        return _FakeTransaction()

    def cursor(self):
        return self._cursor


def _chunk_record():
    return ChunkRecord(
        book_id=1,
        task_id=2,
        chunk_index=3,
        content="如占求财，断曰财可得。",
        chapter_title="求财章",
        content_type="case_example",
        focus_topic="求财",
        knowledge_type="CASE",
        has_timing_prediction=True,
        topic_tags_json='["求财"]',
        metadata_json='{"splitReason":"case_preservation"}',
        char_count=12,
        sentence_count=1,
        embedding_json='[0.1,0.2]',
        embedding_vector_literal="[0.1,0.2]",
        embedding_model="mock",
        embedding_provider="mock",
        embedding_dim=2,
        embedding_version="v1",
    )


class ChunkRepositoryTest(unittest.TestCase):
    def test_replace_chunks_for_book_binds_all_columns_in_vector_mode(self):
        cursor = _RecordingCursor()
        repository = ChunkRepository(_FakeConnection(cursor))
        repository._vector_storage_ready = True

        count = repository.replace_chunks_for_book(1, [_chunk_record()])

        self.assertEqual(1, count)
        insert_sql, insert_params = cursor.executed[-1]
        self.assertEqual(insert_sql.count("%s"), len(insert_params))
        self.assertIn("%s::vector", insert_sql)

    def test_replace_chunks_for_book_does_not_cast_to_vector_in_fallback_mode(self):
        cursor = _RecordingCursor()
        repository = ChunkRepository(_FakeConnection(cursor))
        repository._vector_storage_ready = False

        count = repository.replace_chunks_for_book(1, [_chunk_record()])

        self.assertEqual(1, count)
        insert_sql, insert_params = cursor.executed[-1]
        self.assertEqual(insert_sql.count("%s"), len(insert_params))
        self.assertNotIn("CAST(%s AS vector)", insert_sql)
        self.assertNotIn("::vector", insert_sql)


class RuleCandidateRepositoryTest(unittest.TestCase):
    def test_rule_extract_query_includes_case_example_chunks(self):
        cursor = _RecordingCursor(rows=[])
        repository = RuleCandidateRepository(_FakeConnection(cursor))

        chunks = repository.get_chunks_for_rule_extraction(9)

        self.assertEqual([], chunks)
        query, _ = cursor.executed[0]
        self.assertIn("'case_example'", query)


if __name__ == "__main__":
    unittest.main()
