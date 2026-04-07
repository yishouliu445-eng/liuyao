from abc import ABC, abstractmethod

from app.schemas.chunk_models import ChunkDraft


class BaseChunker(ABC):
    @abstractmethod
    def chunk(self, text: str, metadata: dict) -> list[ChunkDraft]:
        raise NotImplementedError
