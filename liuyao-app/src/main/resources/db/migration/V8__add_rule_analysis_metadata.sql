ALTER TABLE case_rule_hit
    ADD COLUMN IF NOT EXISTS category VARCHAR(100);

ALTER TABLE case_rule_hit
    ADD COLUMN IF NOT EXISTS score_delta INTEGER;

ALTER TABLE case_rule_hit
    ADD COLUMN IF NOT EXISTS tags_json TEXT;

ALTER TABLE case_analysis_result
    ADD COLUMN IF NOT EXISTS score INTEGER;

ALTER TABLE case_analysis_result
    ADD COLUMN IF NOT EXISTS result_level VARCHAR(100);

ALTER TABLE case_analysis_result
    ADD COLUMN IF NOT EXISTS structured_result_json TEXT;
