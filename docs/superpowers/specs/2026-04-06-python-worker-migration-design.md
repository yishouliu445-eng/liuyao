# Python Worker Migration Design

**Date:** 2026-04-06

## Goal

将当前由 Java 主服务直接执行的资料导入加工链，迁移为“Java 负责资料登记与状态展示，Python worker 负责文本加工与向量入库”的保守架构。

## Current State

当前链路位于 `liuyao-app`：

- `BookService` 负责登记 `book` 与 `doc_process_task`
- `TaskService` 负责执行 `BOOK_PARSE` 任务
- `KnowledgeImportService` 负责 TXT/PDF 文本读取、简单切片与 `knowledge_reference` 入库
- `KnowledgeController` / `KnowledgeSearchService` 直接查询 `knowledge_reference`

这条链路已经证明了任务登记与展示是可用的，但切片能力、标签能力、embedding 能力以及可诊断性都不足，且文本加工逻辑耦合在 Java Web 服务中。

## Target Architecture

保守迁移后的职责边界：

- Java 主服务继续负责：
  - `POST /api/books/import-requests`
  - `GET /api/books`
  - `GET /api/tasks/doc-process`
  - chunk / 检索结果展示
  - 重试入口与状态展示
- Python worker 负责：
  - 轮询并抢占 `BOOK_PARSE` 任务
  - 读取 `book.file_path`
  - TXT 清洗、粗切、细切、标签识别、chunk 类型判定
  - embedding
  - 写入 `book_chunk`
  - 更新 `doc_process_task` 与 `book` 状态

核心原则是不引入第二个业务后端。Python worker 直接连接同一 PostgreSQL，只承担离线加工责任。

## Migration Strategy

采用分阶段替换，而不是一次性切断 Java：

### Phase 1

- 新增 `book_chunk` 及其配套迁移
- 新建 `liuyao-worker/` Python 项目骨架
- Python worker 先接管 `BOOK_PARSE` 的轮询、抢占、TXT 处理、embedding 占位和回写
- Java 侧新增/改造查询能力，允许从 `book_chunk` 查看结果

### Phase 2

- Java 的 `TaskService.executeDocProcessTask` 从“实际处理”改为“手动投递/重置为待处理”
- `KnowledgeImportService` 退化为兼容层或删除
- `KnowledgeSearchService` 改为以 `book_chunk` 为主查询源

### Phase 3

- 扩展 PDF、OCR、更多古籍专用 chunker
- 接入真实 embedding provider
- 引入向量检索接口

## Data Contract

### Existing Tables Reused

- `book`
- `doc_process_task`

### New Table

新增 `book_chunk`，用于承载 Python worker 的结构化结果。字段建议：

- `id BIGSERIAL PRIMARY KEY`
- `book_id BIGINT NOT NULL`
- `task_id BIGINT NOT NULL`
- `chapter_title VARCHAR(255)`
- `chunk_index INT NOT NULL`
- `content TEXT NOT NULL`
- `content_type VARCHAR(50) NOT NULL`
- `focus_topic VARCHAR(100)`
- `topic_tags_json TEXT NOT NULL`
- `metadata_json TEXT NOT NULL`
- `char_count INT NOT NULL`
- `sentence_count INT NOT NULL`
- `embedding vector(1024)` 或初期 `TEXT/NULL` 占位，按 provider 能力确定
- `embedding_model VARCHAR(100)`
- `embedding_provider VARCHAR(100)`
- `embedding_dim INT`
- `embedding_version VARCHAR(50)`
- `created_at`
- `updated_at`

同时新增索引：

- `idx_book_chunk_book_id_chunk_index`
- `idx_book_chunk_focus_topic`
- 若启用 pgvector，则增加 embedding 索引

### Task Locking

为支持多 worker 抢占，`doc_process_task` 需要补充最小锁定字段：

- `processor_type VARCHAR(50)`，例如 `JAVA` / `PYTHON_WORKER`
- `locked_by VARCHAR(100)`，记录 worker 实例
- `locked_at TIMESTAMP`
- `started_at TIMESTAMP`
- `finished_at TIMESTAMP`

这些字段用于实现原子抢占、诊断与超时恢复。

## Task Lifecycle

状态流转保持简单：

- `PENDING`
- `PROCESSING`
- `COMPLETED`
- `FAILED`

Python worker 的标准流程：

1. 拉取 `PENDING` + `task_type=BOOK_PARSE`
2. 原子更新为 `PROCESSING`
3. 更新 `book.parse_status=PROCESSING`
4. 执行 pipeline
5. 成功时覆盖当前 `book_id` 的历史 chunk 并写入新 chunk
6. 更新任务与书目为 `COMPLETED`
7. 失败时写 `error_message` 并更新为 `FAILED`

幂等规则：

- 针对单个 `book_id`，每次成功处理前先删除旧 `book_chunk`
- 不覆盖其他 `book_id` 的数据
- 失败不删除历史成功数据，除非当前策略明确要求全量替换

## Python Worker Project Structure

新增顶层目录 `liuyao-worker/`：

- `app/config`：环境变量、日志、阈值配置
- `app/db`：连接与 SQL 仓储
- `app/task_runner`：轮询、抢占、状态更新
- `app/parser`：TXT 读取
- `app/cleaner`：预清洗
- `app/chunker`：通用古籍切片器 + 《增删卜易》专用切片器
- `app/embedding`：provider 抽象与 mock/provider 实现
- `app/pipeline`：整条处理编排
- `app/schemas`：`TaskDTO`、`BookDTO`、`ChunkDraft`、`ChunkRecord`
- `main.py`：启动入口

## Chunking Strategy

Python worker 不再沿用 Java 当前的“空行分段 + 主题词过滤”。

新的 TXT 处理规则：

1. 预清洗：换行、空白、页码噪声、目录残片、标点标准化
2. 一级粗切：标题、双换行、明显分隔符、语义起点
3. 二级细切：超长块、多主题块、规则/案例混合块
4. 分类打标：
   - `topic_tags`
   - `focus_topic`
   - `content_type`：`rule` / `concept` / `example` / `mixed`
5. embedding
6. 入库

《增删卜易》需要专用 chunker，因为它的章节名、引文结构、`野鹤曰` / `断曰` / `如占` 等触发词都很稳定，适合定制规则。

## Java Changes

Java 侧坚持最小改动：

- `BookService` 保持导入请求登记逻辑
- `TaskService` 逐步停止实际文档处理逻辑
- `KnowledgeSearchService` 改为查询 `book_chunk`
- `KnowledgeController` 暴露新的 chunk 查询接口或兼容映射
- 数据迁移期间保留 `knowledge_reference`，但不再作为未来主存储

初期可保留原 `/api/tasks/doc-process/{taskId}/execute` 接口，但其语义改为：

- 手动重置任务为 `PENDING`
- 或触发一次“允许 Python worker 重新拾取”的动作

不再在 Java 进程内执行切片和入库。

## Risks

### Dirty Working Tree

当前仓库已有大量未提交改动，迁移工作必须尽量局部化，避免触碰无关 Java 模块。

### Dual Storage Risk

若 `knowledge_reference` 与 `book_chunk` 长时间并存，容易造成结果源不一致。需要尽快把查询口统一到 `book_chunk`。

### Worker Ownership

如果没有任务锁字段和抢占 SQL，多 worker 场景会重复处理同一任务。

### Embedding Provider Variability

模型维度、限流、超时都可能变化，embedding 层必须抽象，不能把 provider 写死在 pipeline 中。

## Recommended First Slice

第一批实施应聚焦于“固定契约，不求一次到位”：

1. 新增 `book_chunk` 与 `doc_process_task` 锁定字段迁移
2. 创建 `liuyao-worker/` 最小可运行骨架
3. 用 mock embedding 跑通 TXT -> chunk -> DB 写入闭环
4. Java 增加对 `book_chunk` 的只读查询
5. 保留老链路作为短期回退，但默认路径切向 Python worker

这能在最小风险下完成职责迁移，并为后续真实 embedding / pgvector 检索留出稳定接口。
