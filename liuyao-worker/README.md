# liuyao-worker

Python worker for book document processing.

## Current Scope

This first slice does the following:

- polls `doc_process_task` for `BOOK_PARSE` tasks
- claims a task atomically
- reads the referenced TXT file
- normalizes text
- chunks the text with a rule-based pipeline
- generates embeddings
- writes results into `book_chunk`
- keeps `embedding_json` for compatibility and writes `embedding_vector` when PostgreSQL `pgvector` is available
- updates task and book status
- applies lightweight OCR cleanup for common Chinese recognition errors
- backfills topic tags from OCR-friendly aliases and chapter headings when possible

## Environment Variables

- `LIUYAO_DB_DSN`: PostgreSQL DSN, for example `postgresql://dify:password123@localhost:5432/liuyao`
- `LIUYAO_WORKER_ID`: worker instance identifier
- `LIUYAO_POLL_INTERVAL_SECONDS`: poll interval in daemon mode, defaults to `5`
- `LIUYAO_EMBEDDING_PROVIDER`: defaults to `dashscope`
- `LIUYAO_EMBEDDING_MODEL`: defaults to `text-embedding-v4`
- `LIUYAO_EMBEDDING_DIM`: optional; only needed for `mock`, defaults to `8`
- `LIUYAO_EMBEDDING_BASE_URL`: required when provider is `http`
- `LIUYAO_EMBEDDING_API_KEY`: optional for local compatible services, required by providers that enforce auth
- `LIUYAO_EMBEDDING_TIMEOUT_SECONDS`: defaults to `30`
- `LIUYAO_EMBEDDING_BATCH_SIZE`: defaults to `16`; used for batch embedding requests
- `LIUYAO_VECTOR_STORE_DIM`: defaults to `1024`; only matching vectors are written into `embedding_vector`
- `DASHSCOPE_API_KEY`: used automatically when provider is `dashscope` and `LIUYAO_EMBEDDING_API_KEY` is not set
- `LIUYAO_PDF_OCR_MAX_PAGES`: defaults to `80`; limits OCR pages for scanned PDFs to keep processing time bounded
- `TESSDATA_PREFIX`: optional; points to a custom `tessdata` directory when OCR needs extra languages such as `chi_sim`

## Embedding Providers

- `mock`: deterministic local fallback for tests or offline verification
- `dashscope`: Alibaba Cloud Bailian text embeddings via the OpenAI-compatible SDK
- `http`: generic OpenAI-compatible `POST /embeddings` provider

Example:

```bash
export LIUYAO_EMBEDDING_PROVIDER=http
export LIUYAO_EMBEDDING_BASE_URL=http://localhost:8000/v1
export LIUYAO_EMBEDDING_MODEL=text-embedding-3-small
python3 -m app.main --once
```

DashScope example:

```bash
export LIUYAO_EMBEDDING_PROVIDER=dashscope
export LIUYAO_EMBEDDING_MODEL=text-embedding-v4
export DASHSCOPE_API_KEY=sk-xxx
python3 -m app.main --once
```

## Usage

Run once:

```bash
cd liuyao-worker
python3 -m app.main --once
```

Run continuously:

```bash
cd liuyao-worker
python3 -m app.main
```

## Residual Risks

- TXT is currently the most stable path and produces the best topic tags.
- Extractable PDF is supported directly; scanned PDF falls back to OCR.
- Scanned PDF OCR quality depends on local Tesseract language packs. For Chinese books, `chi_sim` should be installed or exposed through `TESSDATA_PREFIX`.
- Scanned PDF tagging is now more tolerant of OCR variants such as `六艾/六攻 -> 六爻`, `月玻 -> 月破`, `动艾 -> 动爻`, and can also inherit topics from chapter headings, but there will still be `untagged` chunks when OCR quality is poor.
- Large scanned PDFs can be slow. The worker now limits OCR to the first `80` pages by default; increase `LIUYAO_PDF_OCR_MAX_PAGES` only when needed.
- Java no longer performs in-process parsing. If the Python worker is unavailable, tasks remain `PENDING` until requeued or picked up by the worker.
