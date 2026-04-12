-- V15: 创建应验事件表和反馈表

CREATE TABLE IF NOT EXISTS verification_event (
    id                  UUID PRIMARY KEY,
    session_id          UUID         NOT NULL REFERENCES chat_session(id),
    user_id             BIGINT,
    predicted_date      DATE         NOT NULL,
    predicted_precision VARCHAR(20)  NOT NULL DEFAULT 'MONTH',
    prediction_summary  TEXT         NOT NULL,
    question_category   VARCHAR(100),
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    reminder_sent_at    TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_verification_event_user
    ON verification_event(user_id, status);

CREATE INDEX IF NOT EXISTS idx_verification_event_date
    ON verification_event(predicted_date)
    WHERE status = 'PENDING';

CREATE TABLE IF NOT EXISTS verification_feedback (
    id              UUID PRIMARY KEY,
    event_id        UUID         NOT NULL REFERENCES verification_event(id) UNIQUE,
    accuracy        VARCHAR(30)  NOT NULL,
    actual_outcome  TEXT,
    tags_json       TEXT,
    submitted_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
