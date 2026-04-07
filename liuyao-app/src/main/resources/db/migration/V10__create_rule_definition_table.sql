CREATE TABLE IF NOT EXISTS rule_definition (
  id BIGSERIAL PRIMARY KEY,
  rule_id VARCHAR(100) NOT NULL,
  rule_code VARCHAR(100) NOT NULL,
  name VARCHAR(255) NOT NULL,
  category VARCHAR(100),
  priority INT,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  version VARCHAR(50),
  condition_json TEXT,
  effect_json TEXT,
  description TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_rule_definition_rule_id ON rule_definition(rule_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_rule_definition_rule_code ON rule_definition(rule_code);
