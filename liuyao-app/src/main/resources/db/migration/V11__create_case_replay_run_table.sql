CREATE TABLE IF NOT EXISTS case_replay_run (
  id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL,
  rule_bundle_version VARCHAR(50),
  rule_definitions_version VARCHAR(50),
  use_god_rules_version VARCHAR(50),
  baseline_rule_version VARCHAR(50),
  replay_rule_version VARCHAR(50),
  baseline_use_god_config_version VARCHAR(50),
  replay_use_god_config_version VARCHAR(50),
  recommend_persist_replay BOOLEAN NOT NULL DEFAULT FALSE,
  persistence_assessment TEXT,
  score_delta INT,
  effective_score_delta INT,
  result_level_changed BOOLEAN NOT NULL DEFAULT FALSE,
  summary_changed BOOLEAN NOT NULL DEFAULT FALSE,
  analysis_changed BOOLEAN NOT NULL DEFAULT FALSE,
  payload_json TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_case_replay_run_case_id_id
  ON case_replay_run (case_id, id DESC);
