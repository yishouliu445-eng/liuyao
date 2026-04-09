CREATE TABLE IF NOT EXISTS rule_candidate (
  id BIGSERIAL PRIMARY KEY,
  book_chunk_id BIGINT NOT NULL,
  task_id BIGINT NOT NULL,
  rule_title VARCHAR(255) NOT NULL,
  category VARCHAR(100),
  condition_desc TEXT,
  effect_direction VARCHAR(50),
  source_book VARCHAR(255),
  evidence_text TEXT,
  confidence DOUBLE PRECISION,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_rule_candidate_status ON rule_candidate (status);
CREATE INDEX IF NOT EXISTS idx_rule_candidate_category ON rule_candidate (category);
CREATE INDEX IF NOT EXISTS idx_rule_candidate_book_chunk_id ON rule_candidate (book_chunk_id);
