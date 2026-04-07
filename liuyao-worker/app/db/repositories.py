import json
from typing import Iterable

from psycopg import Connection

from app.schemas.chunk_models import ChunkRecord
from app.schemas.task_models import BookRecord, TaskRecord


class TaskRepository:
    def __init__(self, connection: Connection):
        self.connection = connection

    def claim_next_book_parse_task(self, worker_id: str) -> TaskRecord | None:
        sql = """
            WITH picked AS (
                SELECT id
                FROM doc_process_task
                WHERE task_type = 'BOOK_PARSE' AND status = 'PENDING'
                ORDER BY id
                FOR UPDATE SKIP LOCKED
                LIMIT 1
            )
            UPDATE doc_process_task task
            SET status = 'PROCESSING',
                processor_type = 'PYTHON_WORKER',
                locked_by = %s,
                locked_at = CURRENT_TIMESTAMP,
                started_at = CURRENT_TIMESTAMP,
                finished_at = NULL,
                error_message = NULL
            FROM picked
            WHERE task.id = picked.id
            RETURNING task.id, task.ref_id, task.status, COALESCE(task.payload_json, '')
        """
        with self.connection.transaction():
            with self.connection.cursor() as cursor:
                cursor.execute(sql, (worker_id,))
                row = cursor.fetchone()
                if row is None:
                    return None
                return TaskRecord(
                    task_id=row[0],
                    book_id=row[1],
                    status=row[2],
                    payload_json=row[3],
                )

    def mark_task_completed(self, task_id: int) -> None:
        with self.connection.transaction():
            with self.connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE doc_process_task
                    SET status = 'COMPLETED',
                        error_message = NULL,
                        finished_at = CURRENT_TIMESTAMP
                    WHERE id = %s
                    """,
                    (task_id,),
                )

    def mark_task_failed(self, task_id: int, error_message: str) -> None:
        with self.connection.transaction():
            with self.connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE doc_process_task
                    SET status = 'FAILED',
                        error_message = %s,
                        finished_at = CURRENT_TIMESTAMP
                    WHERE id = %s
                    """,
                    (error_message, task_id),
                )


class BookRepository:
    def __init__(self, connection: Connection):
        self.connection = connection

    def get_book(self, book_id: int) -> BookRecord:
        with self.connection.cursor() as cursor:
            cursor.execute(
                """
                SELECT id, title, author, source_type, file_path, file_size
                FROM book
                WHERE id = %s
                """,
                (book_id,),
            )
            row = cursor.fetchone()
        if row is None:
            raise RuntimeError(f"Book not found: {book_id}")
        return BookRecord(
            book_id=row[0],
            title=row[1],
            author=row[2],
            source_type=row[3],
            file_path=row[4],
            file_size=row[5],
        )

    def update_parse_status(self, book_id: int, parse_status: str) -> None:
        with self.connection.transaction():
            with self.connection.cursor() as cursor:
                cursor.execute(
                    """
                    UPDATE book
                    SET parse_status = %s,
                        updated_at = CURRENT_TIMESTAMP
                    WHERE id = %s
                    """,
                    (parse_status, book_id),
                )


class ChunkRepository:
    def __init__(self, connection: Connection):
        self.connection = connection
        self._vector_storage_ready: bool | None = None

    def replace_chunks_for_book(self, book_id: int, chunks: Iterable[ChunkRecord]) -> int:
        rows = list(chunks)
        vector_storage_ready = self._ensure_vector_storage()
        with self.connection.transaction():
            with self.connection.cursor() as cursor:
                cursor.execute("DELETE FROM book_chunk WHERE book_id = %s", (book_id,))
                for row in rows:
                    if vector_storage_ready:
                        cursor.execute(
                            """
                        INSERT INTO book_chunk (
                            book_id,
                            task_id,
                            chapter_title,
                                chunk_index,
                                content,
                                content_type,
                                focus_topic,
                                topic_tags_json,
                                metadata_json,
                                char_count,
                                sentence_count,
                                embedding_json,
                                embedding_vector,
                                embedding_model,
                                embedding_provider,
                                embedding_dim,
                                embedding_version
                            ) VALUES (
                                %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s::vector, %s, %s, %s, %s
                            )
                            """,
                            (
                                row.book_id,
                                row.task_id,
                                row.chapter_title,
                                row.chunk_index,
                                row.content,
                                row.content_type,
                                row.focus_topic,
                                row.topic_tags_json,
                                row.metadata_json,
                                row.char_count,
                                row.sentence_count,
                                row.embedding_json,
                                row.embedding_vector_literal,
                                row.embedding_model,
                                row.embedding_provider,
                                row.embedding_dim,
                                row.embedding_version,
                            ),
                        )
                    else:
                        cursor.execute(
                            """
                            INSERT INTO book_chunk (
                                book_id,
                                task_id,
                                chapter_title,
                                chunk_index,
                                content,
                                content_type,
                                focus_topic,
                                topic_tags_json,
                                metadata_json,
                                char_count,
                            sentence_count,
                            embedding_json,
                            embedding_vector,
                            embedding_model,
                            embedding_provider,
                            embedding_dim,
                            embedding_version
                        ) VALUES (
                            %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, CAST(%s AS vector), %s, %s, %s, %s
                        )
                        """,
                        (
                            row.book_id,
                                row.task_id,
                                row.chapter_title,
                                row.chunk_index,
                                row.content,
                                row.content_type,
                                row.focus_topic,
                                row.topic_tags_json,
                            row.metadata_json,
                            row.char_count,
                            row.sentence_count,
                            row.embedding_json,
                            row.embedding_vector_literal,
                            row.embedding_model,
                            row.embedding_provider,
                            row.embedding_dim,
                            row.embedding_version,
                            ),
                        )
        return len(rows)

    def _ensure_vector_storage(self) -> bool:
        if self._vector_storage_ready is not None:
            return self._vector_storage_ready
        try:
            with self.connection.transaction():
                with self.connection.cursor() as cursor:
                    cursor.execute("CREATE EXTENSION IF NOT EXISTS vector")
                    cursor.execute("ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS embedding_vector vector")
                    cursor.execute(
                        """
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_name = 'book_chunk' AND column_name = 'embedding_vector'
                        """
                    )
                    self._vector_storage_ready = cursor.fetchone() is not None
        except Exception:
            self._vector_storage_ready = False
        return self._vector_storage_ready


def dump_json(data: dict | list) -> str:
    return json.dumps(data, ensure_ascii=False)
