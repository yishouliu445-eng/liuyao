CREATE TABLE evaluation_run (
    id BIGSERIAL PRIMARY KEY,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    dataset_type VARCHAR(64) NOT NULL,
    scenario_type VARCHAR(64) NOT NULL,
    scenario_id VARCHAR(128) NOT NULL,
    question_category VARCHAR(64),
    passed BOOLEAN NOT NULL,
    summary TEXT,
    score_card_json TEXT,
    selected_citation_rate DOUBLE PRECISION,
    citation_mismatch_rate DOUBLE PRECISION
);

CREATE INDEX idx_evaluation_run_dataset_type
    ON evaluation_run (dataset_type);

CREATE INDEX idx_evaluation_run_scenario_type
    ON evaluation_run (scenario_type);
