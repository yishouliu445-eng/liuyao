import json
from dataclasses import asdict

from app.chunker.zengshan_buyi_chunker import ZengshanBuyiChunker
from app.cleaner.text_cleaner import clean_text
from app.db.repositories import dump_json
from app.embedding.factory import create_embedder
from app.parser.txt_parser import parse_pdf, parse_txt
from app.schemas.chunk_models import ChunkRecord
from app.schemas.task_models import BookRecord, TaskRecord


class BookPipeline:
    def __init__(self,
                 embedding_dim: int | None,
                 embedding_batch_size: int,
                 embedding_provider: str,
                 embedding_model: str,
                 embedding_base_url: str | None,
                 embedding_api_key: str | None,
                 embedding_timeout_seconds: int,
                 vector_store_dim: int):
        self.chunker = ZengshanBuyiChunker()
        self.embedder = create_embedder(
            provider=embedding_provider,
            model=embedding_model,
            dim=embedding_dim,
            base_url=embedding_base_url,
            api_key=embedding_api_key,
            timeout_seconds=embedding_timeout_seconds,
        )
        self.embedding_batch_size = max(1, embedding_batch_size)
        self.embedding_provider = embedding_provider
        self.embedding_model = embedding_model
        self.vector_store_dim = vector_store_dim

    def process(self, task: TaskRecord, book: BookRecord) -> list[ChunkRecord]:
        text = self._read_source_text(book)
        cleaned = clean_text(text)
        drafts = self.chunker.chunk(cleaned, {
            "book_title": book.title,
            "allow_untagged": (book.source_type or "").upper() == "PDF",
        })
        records: list[ChunkRecord] = []
        for batch_start in range(0, len(drafts), self.embedding_batch_size):
            batch = drafts[batch_start:batch_start + self.embedding_batch_size]
            embeddings = self.embedder.embed_many([draft.content for draft in batch])
            if len(embeddings) != len(batch):
                raise RuntimeError(f"Embedding batch size mismatch: expected {len(batch)}, got {len(embeddings)}")
            for offset, (draft, embedding) in enumerate(zip(batch, embeddings), start=1):
                chunk_index = batch_start + offset
                records.append(
                    ChunkRecord(
                        book_id=book.book_id,
                        task_id=task.task_id,
                        chunk_index=chunk_index,
                        content=draft.content,
                        chapter_title=draft.chapter_title,
                        content_type=draft.content_type,
                        focus_topic=draft.focus_topic,
                        topic_tags_json=dump_json(draft.topic_tags),
                        metadata_json=json.dumps(asdict(draft)["metadata"], ensure_ascii=False),
                        char_count=len(draft.content),
                        sentence_count=_count_sentences(draft.content),
                        embedding_json=dump_json(embedding),
                        embedding_vector_literal=_to_vector_literal(embedding, self.vector_store_dim),
                        embedding_model=self.embedding_model,
                        embedding_provider=self.embedding_provider,
                        embedding_dim=len(embedding),
                        embedding_version="v1",
                    )
                )
        if not records:
            raise RuntimeError("No chunk records produced for book")
        return records

    def _read_source_text(self, book: BookRecord) -> str:
        source_type = (book.source_type or "TXT").upper()
        if source_type == "PDF":
            return parse_pdf(book.file_path)
        return parse_txt(book.file_path)


def _count_sentences(content: str) -> int:
    count = 0
    for token in ("。", "；", "！", "？", "："):
        count += content.count(token)
    return max(count, 1)


def _to_vector_literal(embedding: list[float], vector_store_dim: int) -> str | None:
    if len(embedding) != vector_store_dim:
        return None
    return "[" + ",".join(str(value) for value in embedding) + "]"
