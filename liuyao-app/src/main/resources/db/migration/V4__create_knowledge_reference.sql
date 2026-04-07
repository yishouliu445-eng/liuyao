CREATE TABLE IF NOT EXISTS knowledge_reference (
  id BIGSERIAL PRIMARY KEY,
  book_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  title VARCHAR(255),
  topic_tag VARCHAR(100),
  source_type VARCHAR(50) NOT NULL,
  source_page INT,
  segment_index INT NOT NULL,
  content TEXT NOT NULL,
  keyword_summary VARCHAR(500),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
