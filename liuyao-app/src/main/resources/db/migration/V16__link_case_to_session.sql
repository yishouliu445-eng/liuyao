-- V16: 关联 divination_case 到 chat_session

ALTER TABLE divination_case
    ADD COLUMN IF NOT EXISTS session_id UUID REFERENCES chat_session(id);

CREATE INDEX IF NOT EXISTS idx_divination_case_session
    ON divination_case(session_id);
