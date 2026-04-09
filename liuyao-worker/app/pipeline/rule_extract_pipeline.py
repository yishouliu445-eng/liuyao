import logging

from app.schemas.chunk_models import ChunkRecord
from app.schemas.rule_models import RuleCandidateRecord
from app.schemas.task_models import BookRecord, TaskRecord
from app.pipeline.rule_extractor import RuleExtractor

LOGGER = logging.getLogger(__name__)

class RuleExtractPipeline:
    def __init__(self,
                 llm_api_key: str | None,
                 llm_model: str,
                 llm_base_url: str | None):
        self.extractor = RuleExtractor(
            api_key=llm_api_key,
            model=llm_model,
            base_url=llm_base_url
        )
    
    def process(self, task: TaskRecord, book: BookRecord, chunks: list[ChunkRecord]) -> list[RuleCandidateRecord]:
        records: list[RuleCandidateRecord] = []
        for chunk in chunks:
            # We assume chunk ID is stored as an ad hoc property `chunk.chunk_id` in the repository
            chunk_id = getattr(chunk, 'chunk_id', 0)
            if not chunk_id:
                LOGGER.warning(f"Chunk missing ID for book {book.book_id}, skipping extraction.")
                continue
            
            drafts = self.extractor.extract_rules(chunk.content)
            for draft in drafts:
                # Discard low-confidence extractions automatically
                if draft.confidence < 0.5:
                    continue
                    
                records.append(
                    RuleCandidateRecord(
                        rule_title=draft.rule_title,
                        category=draft.category,
                        condition_desc=draft.condition_desc,
                        effect_direction=draft.effect_direction,
                        evidence_text=draft.evidence_text,
                        confidence=draft.confidence,
                        book_chunk_id=chunk_id,
                        task_id=task.task_id,
                        source_book=book.title,
                        status="PENDING"
                    )
                )
        return records
