-- V14: 创建对话会话表和消息表

CREATE TABLE IF NOT EXISTS chat_session (
    id              UUID PRIMARY KEY,
    user_id         BIGINT,
    case_id         BIGINT REFERENCES divination_case(id),
    chart_snapshot_id BIGINT REFERENCES case_chart_snapshot(id),
    original_question TEXT NOT NULL,
    question_category VARCHAR(100),
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    message_count   INT          NOT NULL DEFAULT 0,
    total_tokens    INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_active_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    closed_at       TIMESTAMP,
    metadata_json   TEXT
);

CREATE INDEX IF NOT EXISTS idx_chat_session_user_status
    ON chat_session(user_id, status);

CREATE INDEX IF NOT EXISTS idx_chat_session_last_active
    ON chat_session(last_active_at)
    WHERE status = 'ACTIVE';

CREATE TABLE IF NOT EXISTS chat_message (
    id              UUID PRIMARY KEY,
    session_id      UUID         NOT NULL REFERENCES chat_session(id),
    role            VARCHAR(20)  NOT NULL,
    content         TEXT         NOT NULL,
    structured_json TEXT,
    token_count     INT,
    model_used      VARCHAR(100),
    processing_ms   INT,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_chat_message_session
    ON chat_message(session_id, created_at);
