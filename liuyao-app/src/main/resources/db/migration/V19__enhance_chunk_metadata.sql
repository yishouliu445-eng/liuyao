-- V19: Enhance book_chunk with enriched metadata fields
ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS knowledge_type VARCHAR(50);
ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS has_timing_prediction BOOLEAN DEFAULT FALSE;

-- Add index for knowledge_type filtering
CREATE INDEX IF NOT EXISTS idx_book_chunk_knowledge_type ON book_chunk (knowledge_type);
