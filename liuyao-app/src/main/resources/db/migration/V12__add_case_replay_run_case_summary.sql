ALTER TABLE case_replay_run
  ADD COLUMN IF NOT EXISTS question_text TEXT;

ALTER TABLE case_replay_run
  ADD COLUMN IF NOT EXISTS question_category VARCHAR(100);
