ALTER TABLE case_analysis_result
ADD COLUMN IF NOT EXISTS analysis_context_json TEXT;
