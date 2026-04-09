import logging
import time

from app.config.settings import Settings
from app.db.connection import create_connection
from app.db.repositories import BookRepository, ChunkRepository, TaskRepository, RuleCandidateRepository
from app.pipeline.book_pipeline import BookPipeline
from app.pipeline.rule_extract_pipeline import RuleExtractPipeline

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
            if task:
                return self._process_book_parse_task(task, connection, task_repository, book_repository, chunk_repository)
            
            task = task_repository.claim_next_rule_extract_task(self.settings.worker_id)
            if task:
                return self._process_rule_extract_task(task, connection, task_repository, book_repository)
                
            return False

    def _process_book_parse_task(self, task, connection, task_repository, book_repository, chunk_repository) -> bool:
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

    def _process_rule_extract_task(self, task, connection, task_repository, book_repository) -> bool:
        chunk_repository = ChunkRepository(connection)
        candidate_repository = RuleCandidateRepository(connection)
        try:
            book = book_repository.get_book(task.book_id)
            chunks = candidate_repository.get_chunks_for_rule_extraction(task.book_id)
            LOGGER.info("Found %s chunks for rule extraction for book %s", len(chunks), task.book_id)
            
            pipeline = RuleExtractPipeline(
                llm_api_key=self.settings.llm_api_key,
                llm_model=self.settings.llm_model,
                llm_base_url=self.settings.llm_base_url,
            )
            
            records = pipeline.process(task, book, chunks)
            created_count = candidate_repository.save_candidates(records)
            task_repository.mark_task_completed(task.task_id)
            LOGGER.info("Processed rule extraction task %s for book %s with %s rules", task.task_id, task.book_id, created_count)
            return True
        except Exception as exc:
            task_repository.mark_task_failed(task.task_id, str(exc))
            LOGGER.exception("Failed rule extract task %s", task.task_id)
            return True

    def run_forever(self) -> None:
        while True:
            claimed = self.run_once()
            if not claimed:
                time.sleep(self.settings.poll_interval_seconds)
