-- V18: Add full-text search support to book_chunk
ALTER TABLE book_chunk ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- Create GIN index for full-text search
CREATE INDEX IF NOT EXISTS idx_book_chunk_content_tsv ON book_chunk USING GIN (content_tsv);

-- Add trigger function to update content_tsv
-- Note: Using 'simple' as default configuration to avoid dependency on zhparser in all environments.
-- For production, zhparser is recommended for better Chinese tokenization.
CREATE OR REPLACE FUNCTION book_chunk_tsvector_trigger() RETURNS trigger AS $$
BEGIN
  new.content_tsv := to_tsvector('simple', coalesce(new.content, ''));
  RETURN new;
END
$$ LANGUAGE plpgsql;

-- Create trigger
DROP TRIGGER IF EXISTS trg_book_chunk_tsvector ON book_chunk;
CREATE TRIGGER trg_book_chunk_tsvector
BEFORE INSERT OR UPDATE OF content ON book_chunk
FOR EACH ROW EXECUTE FUNCTION book_chunk_tsvector_trigger();

-- Backfill existing data
UPDATE book_chunk SET content_tsv = to_tsvector('simple', coalesce(content, '')) WHERE content_tsv IS NULL;
