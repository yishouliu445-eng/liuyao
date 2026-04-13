CREATE OR REPLACE FUNCTION book_chunk_normalize_search_text(input_text TEXT)
RETURNS TEXT AS $$
DECLARE
  normalized TEXT;
BEGIN
  normalized := regexp_replace(coalesce(input_text, ''), '([一-龥])', ' \1 ', 'g');
  normalized := regexp_replace(normalized, '[[:space:][:punct:]，。；！？：、“”‘’（）《》〈〉【】〔〕—…]+', ' ', 'g');
  normalized := regexp_replace(normalized, '\s+', ' ', 'g');
  RETURN btrim(normalized);
END
$$ LANGUAGE plpgsql IMMUTABLE;

CREATE OR REPLACE FUNCTION book_chunk_tsvector_trigger()
RETURNS trigger AS $$
BEGIN
  NEW.content_tsv := to_tsvector('simple', book_chunk_normalize_search_text(NEW.content));
  RETURN NEW;
END
$$ LANGUAGE plpgsql;

UPDATE book_chunk
SET content_tsv = to_tsvector('simple', book_chunk_normalize_search_text(content));
