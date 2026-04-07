# Python Worker Migration Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move document-processing work for book imports out of the Java web app into a Python worker while keeping Java responsible for task creation, status display, and result queries.

**Architecture:** Java remains the metadata and API owner. A new Python worker polls shared PostgreSQL for `BOOK_PARSE` tasks, claims them atomically, processes TXT files into structured chunks with tags and embeddings, writes `book_chunk`, and updates task/book status. Java then reads `book_chunk` for downstream display.

**Tech Stack:** Spring Boot, PostgreSQL, Flyway, Python 3, `psycopg`, optional `pgvector`, mock embedding provider for the first slice.

---

## Files And Responsibilities

- Create: `docs/superpowers/specs/2026-04-06-python-worker-migration-design.md`
- Create: `docs/superpowers/plans/2026-04-06-python-worker-migration.md`
- Create: `liuyao-app/src/main/resources/db/migration/V5__add_book_chunk_and_task_lock_fields.sql`
- Create: `liuyao-worker/README.md`
- Create: `liuyao-worker/requirements.txt`
- Create: `liuyao-worker/app/config/settings.py`
- Create: `liuyao-worker/app/db/connection.py`
- Create: `liuyao-worker/app/db/repositories.py`
- Create: `liuyao-worker/app/schemas/task_models.py`
- Create: `liuyao-worker/app/schemas/chunk_models.py`
- Create: `liuyao-worker/app/parser/txt_parser.py`
- Create: `liuyao-worker/app/cleaner/text_cleaner.py`
- Create: `liuyao-worker/app/chunker/base_chunker.py`
- Create: `liuyao-worker/app/chunker/classic_text_chunker.py`
- Create: `liuyao-worker/app/chunker/zengshan_buyi_chunker.py`
- Create: `liuyao-worker/app/embedding/base_embedder.py`
- Create: `liuyao-worker/app/embedding/mock_embedder.py`
- Create: `liuyao-worker/app/pipeline/book_pipeline.py`
- Create: `liuyao-worker/app/task_runner/worker.py`
- Create: `liuyao-worker/app/main.py`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/task/service/TaskService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/task/controller/TaskController.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/controller/KnowledgeController.java`
- Create or Modify: Java DTO/repository/service files needed to read from `book_chunk`
- Test: focused Java tests around task state and knowledge query compatibility

## Chunk 1: Database Contract

### Task 1: Add migration for worker-owned processing state and chunk storage

- [x] **Step 1: Write the migration**

Add a Flyway migration that:

- creates `book_chunk`
- adds worker lock fields to `doc_process_task`
- adds timestamps useful for diagnostics
- keeps existing `book` and `doc_process_task` ids intact

- [x] **Step 2: Review schema compatibility**

Check that:

- existing rows remain valid
- new columns are nullable or have safe defaults
- the migration does not break current H2/PostgreSQL test startup

- [x] **Step 3: Run focused verification**

Run: `mvn -q -DskipTests compile`

Expected: compile succeeds and Flyway validates migrations.

## Chunk 2: Python Worker Skeleton

### Task 2: Scaffold a minimal worker that can poll and claim tasks

- [x] **Step 1: Create the Python project skeleton**

Add `liuyao-worker/` with:

- config
- db
- schemas
- parser
- cleaner
- chunker
- embedding
- pipeline
- task_runner

- [x] **Step 2: Implement DB access and task claim flow**

Support:

- load config from env
- open PostgreSQL connection
- fetch one `PENDING` `BOOK_PARSE` task
- atomically claim it as `PROCESSING`

- [x] **Step 3: Implement a no-op processing loop**

For the first executable version:

- load the referenced book
- read TXT content
- produce at least one `ChunkRecord`
- write rows into `book_chunk`
- mark task/book as `COMPLETED`

- [x] **Step 4: Add mock embedding**

Return a deterministic placeholder vector or skip vector storage when provider is `mock`.

- [x] **Step 5: Verify locally**

Run: `python3 -m py_compile $(find liuyao-worker -name '*.py')`

Expected: no syntax errors.

## Chunk 3: TXT Processing Pipeline

### Task 3: Implement the first rule-based chunking path

- [x] **Step 1: Add text cleaning**

Normalize:

- line endings
- repeated whitespace
- obvious directory noise
- stray page markers

- [x] **Step 2: Add a classic chunker**

Implement:

- coarse split by headings and blank lines
- refine overlong blocks
- identify `topic_tags`, `focus_topic`, and `content_type`

- [x] **Step 3: Add a `zengshan_buyi` specialization**

Handle:

- chapter headings
- `野鹤曰`
- `断曰`
- `如占`
- similar classical triggers

- [x] **Step 4: Verify with the provided TXT**

Run the worker against `增删卜易--野鹤老人.txt` after encoding normalization.

Expected: chunk count is meaningfully higher than the current 18-reference Java path and each row has tags/metadata.

## Chunk 4: Java Read Path Alignment

### Task 4: Make Java read worker results without processing them

- [x] **Step 1: Add Java-side read models for `book_chunk`**

Create:

- entity or JDBC projection
- DTOs for chunk responses
- repository/service methods

- [x] **Step 2: Wire knowledge query endpoints to `book_chunk`**

Keep API shape simple and compatible where reasonable.

- [x] **Step 3: Remove or neutralize Java in-process parsing**

Change `TaskService.executeDocProcessTask` so it no longer calls `KnowledgeImportService.importBook`.

Safer first behavior:

- reset task to `PENDING`, clear error, and return accepted/requeued semantics
- or fail fast with a clear message that processing is owned by Python worker

- [x] **Step 4: Run focused tests**

Run: `mvn -q -Dtest=BookImportControllerTest,KnowledgeImportExecutionTest test`

Expected: update tests or replace them with worker-compatible equivalents so the Java side reflects the new ownership boundary.

## Chunk 5: Verification And Cleanup

### Task 5: Verify the end-to-end migration slice

- [x] **Step 1: Start the Java app**

Run: `mvn spring-boot:run`

- [x] **Step 2: Create an import request**

Use the existing `/api/books/import-requests` endpoint.

- [x] **Step 3: Run the Python worker**

Point it at the shared PostgreSQL and the test TXT.

- [x] **Step 4: Verify final state**

Confirm:

- `book.parse_status=COMPLETED`
- `doc_process_task.status=COMPLETED`
- `book_chunk` rows exist
- Java query endpoints can read the results

- [x] **Step 5: Document residual risks**

Capture:

- provider selection
- pgvector setup assumptions
- fallback/rollback path from Java processing

## Notes

- Keep diffs minimal because the repository is already dirty.
- Do not refactor unrelated modules.
- Prefer mock embedding in the first executable slice; real providers can come later without changing the pipeline contract.
- Avoid deleting `knowledge_reference` in the same slice; first stop writing to it, then remove it in a later cleanup.
