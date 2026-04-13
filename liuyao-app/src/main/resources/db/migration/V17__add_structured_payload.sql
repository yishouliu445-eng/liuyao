-- V17: 为 case_analysis_result 增加结构化 JSON 输出字段

ALTER TABLE case_analysis_result
    ADD COLUMN IF NOT EXISTS structured_payload_json TEXT;

ALTER TABLE case_analysis_result
    ADD COLUMN IF NOT EXISTS prompt_version VARCHAR(20);
