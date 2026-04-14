-- V21: create analysis execution audit tables

CREATE TABLE IF NOT EXISTS analysis_run (
    id BIGSERIAL PRIMARY KEY,
    execution_id UUID NOT NULL UNIQUE,
    execution_mode VARCHAR(30) NOT NULL,
    question_text TEXT,
    question_category VARCHAR(100),
    use_god VARCHAR(100),
    main_hexagram VARCHAR(100),
    changed_hexagram VARCHAR(100),
    prompt_version VARCHAR(50),
    model_version VARCHAR(100),
    degradation_level VARCHAR(30),
    confidence DOUBLE PRECISION,
    payload_json TEXT,
    rag_source_count INT,
    degradation_reasons TEXT,
    validation_issue_count INT NOT NULL DEFAULT 0,
    citation_count INT NOT NULL DEFAULT 0,
    analysis_conclusion TEXT,
    legacy_analysis_text TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_run_execution_id
    ON analysis_run (execution_id);

CREATE INDEX IF NOT EXISTS idx_analysis_run_execution_mode_id
    ON analysis_run (execution_mode, id DESC);

CREATE TABLE IF NOT EXISTS analysis_run_issue (
    id BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL REFERENCES analysis_run(id) ON DELETE CASCADE,
    issue_code VARCHAR(100) NOT NULL,
    issue_message TEXT,
    severity VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_run_issue_run_id
    ON analysis_run_issue (analysis_run_id, id ASC);

CREATE TABLE IF NOT EXISTS analysis_run_citation (
    id BIGSERIAL PRIMARY KEY,
    analysis_run_id BIGINT NOT NULL REFERENCES analysis_run(id) ON DELETE CASCADE,
    citation_id VARCHAR(100),
    chunk_id BIGINT,
    book_id BIGINT,
    source_title TEXT,
    chapter_title TEXT,
    reference_source TEXT,
    reference_quote TEXT,
    reference_relevance TEXT,
    matched_source_title TEXT,
    matched_chapter_title TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_analysis_run_citation_run_id
    ON analysis_run_citation (analysis_run_id, id ASC);

CREATE INDEX IF NOT EXISTS idx_analysis_run_citation_citation_id
    ON analysis_run_citation (citation_id);
