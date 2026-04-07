import logging
import time

from app.config.settings import Settings
from app.db.connection import create_connection
from app.db.repositories import BookRepository, ChunkRepository, TaskRepository
from app.pipeline.book_pipeline import BookPipeline

LOGGER = logging.getLogger(__name__)


class Worker:
    def __init__(self, settings: Settings):
        self.settings = settings

    def run_once(self) -> bool:
        with create_connection(self.settings.db_dsn) as connection:
            task_repository = TaskRepository(connection)
            book_repository = BookRepository(connection)
            chunk_repository = ChunkRepository(connection)
            task = task_repository.claim_next_book_parse_task(self.settings.worker_id)
            if task is None:
                return False

            book_repository.update_parse_status(task.book_id, "PROCESSING")
            try:
                book = book_repository.get_book(task.book_id)
                pipeline = BookPipeline(
                    embedding_dim=self.settings.embedding_dim,
                    embedding_batch_size=self.settings.embedding_batch_size,
                    embedding_provider=self.settings.embedding_provider,
                    embedding_model=self.settings.embedding_model,
                    embedding_base_url=self.settings.embedding_base_url,
                    embedding_api_key=self.settings.embedding_api_key,
                    embedding_timeout_seconds=self.settings.embedding_timeout_seconds,
                    vector_store_dim=self.settings.vector_store_dim,
                )
                chunks = pipeline.process(task, book)
                created_count = chunk_repository.replace_chunks_for_book(task.book_id, chunks)
                task_repository.mark_task_completed(task.task_id)
                book_repository.update_parse_status(task.book_id, "COMPLETED")
                LOGGER.info("Processed task %s for book %s with %s chunks", task.task_id, task.book_id, created_count)
                return True
            except Exception as exc:
                task_repository.mark_task_failed(task.task_id, str(exc))
                book_repository.update_parse_status(task.book_id, "FAILED")
                LOGGER.exception("Failed task %s", task.task_id)
                return True

    def run_forever(self) -> None:
        while True:
            claimed = self.run_once()
            if not claimed:
                time.sleep(self.settings.poll_interval_seconds)
