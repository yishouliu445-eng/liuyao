ALTER TABLE doc_process_task
  ADD COLUMN IF NOT EXISTS processor_type VARCHAR(50);

ALTER TABLE doc_process_task
  ADD COLUMN IF NOT EXISTS locked_by VARCHAR(100);

ALTER TABLE doc_process_task
  ADD COLUMN IF NOT EXISTS locked_at TIMESTAMP;

ALTER TABLE doc_process_task
  ADD COLUMN IF NOT EXISTS started_at TIMESTAMP;

ALTER TABLE doc_process_task
  ADD COLUMN IF NOT EXISTS finished_at TIMESTAMP;

CREATE TABLE IF NOT EXISTS book_chunk (
  id BIGSERIAL PRIMARY KEY,
  book_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  chapter_title VARCHAR(255),
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  content_type VARCHAR(50) NOT NULL,
  focus_topic VARCHAR(100),
  topic_tags_json TEXT NOT NULL,
  metadata_json TEXT NOT NULL,
  char_count INT NOT NULL DEFAULT 0,
  sentence_count INT NOT NULL DEFAULT 0,
  embedding_json TEXT,
  embedding_model VARCHAR(100),
  embedding_provider VARCHAR(100),
  embedding_dim INT,
  embedding_version VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_book_chunk_book_id_chunk_index
  ON book_chunk (book_id, chunk_index);

CREATE INDEX IF NOT EXISTS idx_book_chunk_focus_topic
  ON book_chunk (focus_topic);

CREATE INDEX IF NOT EXISTS idx_doc_process_task_status_type
  ON doc_process_task (status, task_type);
