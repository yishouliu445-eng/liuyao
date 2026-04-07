from dataclasses import dataclass, field


@dataclass
class ChunkDraft:
    content: str
    chapter_title: str | None = None
    split_level: int = 1
    content_type: str = "mixed"
    topic_tags: list[str] = field(default_factory=list)
    focus_topic: str | None = None
    metadata: dict = field(default_factory=dict)


@dataclass
class ChunkRecord:
    book_id: int
    task_id: int
    chunk_index: int
    content: str
    chapter_title: str | None
    content_type: str
    focus_topic: str | None
    topic_tags_json: str
    metadata_json: str
    char_count: int
    sentence_count: int
    embedding_json: str | None
    embedding_vector_literal: str | None
    embedding_model: str | None
    embedding_provider: str | None
    embedding_dim: int | None
    embedding_version: str | None
