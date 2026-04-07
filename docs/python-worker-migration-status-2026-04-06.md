# Python Worker 迁移进展

**日期：** 2026-04-06

## 1. 当前目标

将资料导入加工链从 Java 主服务迁移到 Python worker，当前阶段以《增删卜易》TXT 导入、切片、embedding、入库和 Java 侧查询打通为主。

## 2. 已完成工作

### 2.1 Java 主服务职责切换

- `POST /api/books/import-requests` 仍由 Java 负责资料登记。
- `BOOK_PARSE` 任务已改为由 Python worker 消费，Java 侧支持重入队与状态展示。
- 任务诊断字段已暴露到接口：
  - `lockedAt`
  - `startedAt`
  - `finishedAt`

相关实现：

- `liuyao-app/src/main/java/com/yishou/liuyao/task/service/TaskService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/task/dto/TaskSummaryDTO.java`

### 2.2 Python worker 主链路

- 已建立 `liuyao-worker/` 最小可运行项目。
- 已支持轮询 `doc_process_task`，并通过 `FOR UPDATE SKIP LOCKED` 原子抢占任务。
- 已支持 TXT 读取、文本清洗、规则切片、标签识别、embedding、`book_chunk` 入库。
- 已支持成功/失败后同步更新 `doc_process_task` 与 `book.parse_status`。

相关实现：

- `liuyao-worker/app/task_runner/worker.py`
- `liuyao-worker/app/pipeline/book_pipeline.py`
- `liuyao-worker/app/db/repositories.py`
- `liuyao-worker/app/parser/txt_parser.py`
- `liuyao-worker/app/cleaner/text_cleaner.py`

### 2.3 《增删卜易》切片能力

- 已实现古籍规则型切片器。
- 已增强章节标题识别。
- 已补充更明确的切片元数据，当前接口已返回：
  - `splitReason`
  - `splitStrategy`
  - `splitTrigger`
  - `sourceBlockIndex`
  - `sourcePieceIndex`
  - `coarseLength`
  - `refinedLength`
  - `topicHitCount`
- Java API 已将数据库中的 metadata key 统一规范成 camelCase。

相关实现：

- `liuyao-worker/app/chunker/classic_text_chunker.py`
- `liuyao-worker/app/schemas/chunk_models.py`
- `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/mapper/KnowledgeMapper.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`

### 2.4 Embedding 能力

- 已支持三类 provider：
  - `mock`
  - `http`
  - `dashscope`
- 已接入阿里云百炼 `text-embedding-v4`。
- 已支持批量 embedding。
- 已处理 DashScope 单批最大 10 条的限制。
- 已改为由真实返回结果推断 `embedding_dim`，不再要求所有 provider 依赖固定维度配置。

相关实现：

- `liuyao-worker/app/embedding/base_embedder.py`
- `liuyao-worker/app/embedding/mock_embedder.py`
- `liuyao-worker/app/embedding/http_embedder.py`
- `liuyao-worker/app/embedding/dashscope_embedder.py`
- `liuyao-worker/app/embedding/factory.py`
- `liuyao-worker/app/config/settings.py`

### 2.5 向量存储与语义检索

- `book_chunk` 已同时保留：
  - `embedding_json`
  - `embedding_vector`
- 当前 PostgreSQL 已启用 `pgvector` 扩展。
- 当前真实数据已写入 1024 维向量。
- Java 已新增语义检索接口：
  - `GET /api/knowledge/chunks/semantic`
- 接口返回已包含 `similarityScore`。
- 已修复 PostgreSQL 下语义检索参数绑定问题。
- 已补充 1024 维向量表达式索引迁移，以匹配当前无固定维度列的设计。

相关实现：

- `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/repository/BookChunkVectorSearchRepository.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeQueryEmbeddingService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/controller/KnowledgeController.java`
- `liuyao-app/src/main/java/db/migration/V6__add_book_chunk_pgvector_support.java`
- `liuyao-app/src/main/java/db/migration/V7__add_book_chunk_vector_1024_index.java`

### 2.6 测试与验证

- Python 侧已补充以下测试：
  - `tests/test_classic_text_chunker.py`
  - `tests/test_embedding_http.py`
  - `tests/test_embedding_settings.py`
  - `tests/test_dashscope_embedder.py`
- Java 侧已补充以下测试：
  - `KnowledgeImportExecutionTest`
  - `KnowledgeSemanticSearchControllerTest`
- 已完成真实链路验证：
  - DashScope embedding 成功返回 1024 维向量
  - `bookId=3` 已成功写入 `376` 条 chunk
  - `/api/knowledge/chunks/semantic` 已返回真实语义检索结果

## 3. 未完成工作

### 3.1 文档类型扩展

- 当前只稳定支持 TXT。
- PDF、OCR、混合来源文档尚未迁移到 Python worker 主链路。

### 3.2 查询编排能力

- 当前 Java 侧已具备基础语义召回，但还没有形成“关键词召回 + 向量召回 + 重排”的统一检索编排。
- 当前接口仍以 chunk 直出为主，尚未接到更高层问答或知识编排能力。

### 3.3 历史链路收口

- `knowledge_reference` 仍作为兼容存量存在。
- 老的 Java 文档处理实现尚未彻底删除，职责边界虽然已经基本转向 Python worker，但兼容代码还未完全收口。

### 3.4 运行与部署

- worker 目前可运行，但还没有补齐正式部署方式：
  - 进程守护
  - 生产环境配置模板
  - 告警与可观测性
  - 失败任务自动恢复策略

## 4. 待优化项

### 4.1 向量索引策略

- 当前索引优化针对 1024 维 DashScope 向量生效。
- 现有列设计允许不同 `embedding_dim` 共存，因此未来若切换模型维度，需要为新维度补充对应 partial expression index。
- 若后续确定长期只使用单一模型，可评估将 `embedding_vector` 固化为 `vector(1024)`，进一步简化索引和查询表达式。

### 4.2 切片质量

- 章节标题、触发词切分已明显改善，但仍属于规则驱动。
- 后续仍可继续优化：
  - 更稳定的章节边界识别
  - 规则/案例/解释混合段落的拆分
  - 更细粒度的主题标签
  - 更可靠的 `contentType` 判定

### 4.3 Embedding 与召回质量

- 当前已接入真实向量，但还缺少效果评估基线。
- 后续建议补充：
  - 查询集与期望命中集
  - 召回准确率评估
  - 不同 chunk 尺寸对召回结果的影响对比
  - 不同模型的成本与效果对比

### 4.4 可观测性

- 目前已有任务锁、开始/结束时间，但还缺少更系统的运行诊断：
  - 每批 embedding 耗时
  - 每本书处理耗时
  - 失败类型分布
  - 索引命中与检索耗时统计

## 5. 当前建议的下一阶段

1. 收口 Java 老链路，统一查询源到 `book_chunk`。
2. 为语义检索补充更完整的召回评估样例。
3. 将 worker 运行方式标准化，补齐生产启动与恢复策略。
4. 评估 PDF/OCR 进入迁移范围的优先级。
