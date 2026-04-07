# Python Worker 迁移阶段进展

**日期：** 2026-04-06

## 当前结论

当前资料导入链路已经从“Java 直接处理文本”切到“Java 负责登记与展示，Python worker 负责切片、embedding、入库”的主路径。

针对《增删卜易--野鹤老人.txt》的真实数据闭环已经跑通，百炼文本向量模型 `text-embedding-v4` 已接入，Java 语义检索接口也已可用。

## 已完成工作

### 1. Java / Python 职责迁移

- Java 主服务保留导入请求登记、任务展示、结果查询与重试入口。
- Python worker 已接管 `BOOK_PARSE` 任务的轮询、抢占、处理与回写。
- `doc_process_task` 已补充锁定与执行诊断字段：
  - `locked_by`
  - `locked_at`
  - `started_at`
  - `finished_at`
- Java API 已暴露这些诊断字段，方便判断任务是否被 worker 抢占、是否开始、是否结束。

### 2. 文本切片与 metadata 细化

- Python chunker 已支持《增删卜易》的规则化切片。
- 章节标题识别已增强。
- chunk metadata 已细化并在 Java API 中统一转为 camelCase。
- 当前 metadata 中已明确包含：
  - `splitReason`
  - `splitStrategy`
  - `splitTrigger`
  - `sourceBlockIndex`
  - `sourcePieceIndex`
  - `coarseLength`
  - `refinedLength`
  - `topicHitCount`

### 3. embedding 能力

- Python worker 已支持三类 embedding provider：
  - `mock`
  - `http`
  - `dashscope`
- 已接入阿里云百炼 OpenAI 兼容接口。
- `text-embedding-v4` 真实调用成功，当前真实返回维度为 `1024`。
- worker 已支持批量 embedding。
- DashScope 的批量请求已按官方限制自动拆批，当前单批上限按 `10` 处理。
- 系统已保留 `embedding_json` 兼容字段，同时写入 `embedding_vector`。

### 4. pgvector 与语义检索

- PostgreSQL 已安装 `pgvector` 扩展。
- `book_chunk.embedding_vector` 已写入真实向量。
- Java 已新增语义检索接口：
  - `GET /api/knowledge/chunks/semantic`
- 返回体已包含：
  - `similarityScore`
  - `topicTags`
  - 细化后的 `metadata`
- Java 语义检索 SQL 已修复 PostgreSQL 参数绑定问题。
- 当前语义检索会按 query embedding 的真实维度过滤 `embedding_dim`，避免不同维度向量混查。

### 5. 性能项

- 已修复“无维度 `vector` 列无法直接建立 HNSW 索引”的问题。
- 方案调整为：
  - 保留 `embedding_vector vector`
  - 使用 expression + partial index
- 已新增 1024 维向量索引迁移，索引表达式为：
  - `embedding_vector::vector(1024)`
- 这样既能服务当前百炼 `1024` 维模型，也不把表结构锁死到单一维度。

## 已验证结果

### 真实数据

- `bookId=3`
- `taskId=3`
- `book_chunk` 已写入 `376` 条记录
- `376` 条记录全部带有非空 `embedding_vector`
- 当前 provider:
  - `dashscope`
- 当前 model:
  - `text-embedding-v4`
- 当前 `embedding_dim`:
  - `1024`

### 已通过验证

- Python worker 能成功处理 TXT 并更新任务/书籍状态
- Java 能读取 chunk、topic tags、metadata
- Java 语义检索接口能返回真实召回结果
- 已执行通过的关键测试：
  - `KnowledgeImportExecutionTest`
  - `KnowledgeSemanticSearchControllerTest`

## 当前未完成工作

### 1. 多维度索引体系

目前只为 `1024` 维建立了表达式索引，适配当前百炼模型。

如果后续接入新的 embedding 模型维度，还需要：

- 按新维度增量新增 expression index
- 明确每种模型与维度的索引策略
- 避免让查询无意退化为全表扫描

### 2. 检索质量增强

当前语义检索已可用，但还停留在“单纯向量召回”阶段，尚未完成：

- 混合召回（关键词 + 向量）
- rerank
- 命中解释
- 召回去重与上下文拼装

### 3. 资料处理能力扩展

当前主闭环以 TXT 为主，以下能力还未完成：

- PDF 正式支持
- OCR
- 更丰富的古籍专用 chunker
- 更系统的噪声页 / 目录页处理

### 4. 运行与运维能力

当前 worker 已能跑通，但工程化能力还不完整：

- 常驻部署脚本
- 失败重试策略细化
- 死锁/超时任务回收
- 指标监控与告警
- 任务吞吐与耗时统计

## 建议继续优化的工作

### 优先级高

- 为后续可能接入的其他维度模型预留索引迁移模板
- 为语义检索补充真实 PostgreSQL 集成测试，而不仅是 H2 + mock
- 在查询层加入 `EXPLAIN ANALYZE` 基准验证，确认 1024 维查询已实际命中 HNSW 索引

### 优先级中

- 增加 query embedding 缓存，减少重复问题的远程调用成本
- 引入混合召回与 rerank
- 为 `book_chunk` 增加更明确的质量评估指标

### 优先级低

- 补全 README 与运维文档
- 清理 `knowledge_reference` 兼容层
- 统一 worker / Java 的 embedding 配置命名

## 交接建议

下一阶段如果继续推进，建议按这个顺序处理：

1. 先做真实 PostgreSQL 下的检索性能验证，确认索引命中。
2. 再决定是否引入混合召回与 rerank。
3. 然后补 PDF/OCR 与更强的资料清洗能力。

当前主链路已经具备继续向“知识检索 / 问答编排”上层能力扩展的基础，不需要再回头重做底层导入闭环。
