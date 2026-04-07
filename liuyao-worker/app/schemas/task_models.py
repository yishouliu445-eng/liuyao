from dataclasses import dataclass


@dataclass(frozen=True)
class TaskRecord:
    task_id: int
    book_id: int
    status: str
    payload_json: str


@dataclass(frozen=True)
class BookRecord:
    book_id: int
    title: str
    author: str | None
    source_type: str | None
    file_path: str
    file_size: int | None
