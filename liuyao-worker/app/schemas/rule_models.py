from dataclasses import dataclass, field

@dataclass
class RuleCandidateDraft:
    rule_title: str
    category: str | None = None
    condition_desc: str | None = None
    effect_direction: str | None = None
    evidence_text: str | None = None
    confidence: float = 0.0

@dataclass
class RuleCandidateRecord(RuleCandidateDraft):
    book_chunk_id: int = 0
    task_id: int = 0
    source_book: str | None = None
    status: str = "PENDING"
