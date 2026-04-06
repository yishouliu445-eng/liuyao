ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS main_hexagram VARCHAR(100);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS changed_hexagram VARCHAR(100);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS main_hexagram_code VARCHAR(20);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS changed_hexagram_code VARCHAR(20);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS palace VARCHAR(50);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS palace_wu_xing VARCHAR(20);

ALTER TABLE case_chart_snapshot
    ADD COLUMN IF NOT EXISTS use_god VARCHAR(50);
