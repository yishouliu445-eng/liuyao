# 六爻辅助研判系统 - 数据库设计与 SQL 文档

> 版本：v1.0  
> 目标：提供一套可直接落地到 PostgreSQL 的第一阶段数据库设计  
> 适用范围：Java 主服务 + Python worker + PostgreSQL + pgvector

---

# 1. 设计目标

本数据库设计服务于以下核心能力：

1. 书籍导入与解析
2. 知识块存储与检索
3. 卦例保存与复盘
4. 排盘快照留痕
5. 规则命中记录
6. AI 分析结果留痕
7. 文档处理异步任务
8. 后续相似案例检索扩展

---

# 2. 设计原则

## 2.1 业务数据、半结构化数据、向量数据统一落 PostgreSQL
- 业务数据：表结构
- 半结构化数据：JSONB
- 向量数据：pgvector

## 2.2 历史结果必须保存快照
排盘结果、规则命中、分析结果都要按“当次结果”留痕，避免后续算法升级污染历史记录。

## 2.3 模型能力必须可切换
embedding 与 LLM 相关表，必须预留：
- provider
- model_name
- dim
- version

## 2.4 MVP 阶段不过度拆表
先满足开发效率与清晰度，避免一开始就追求极端规范化。

---

# 3. 扩展准备

## 3.1 启用 pgvector

```sql
CREATE EXTENSION IF NOT EXISTS vector;
```

---

# 4. 枚举值约定

## 4.1 解析状态
- PENDING
- PROCESSING
- SUCCESS
- FAILED

## 4.2 卦例状态
- DRAFT
- ANALYZED
- RESULT_PENDING
- CLOSED

## 4.3 任务状态
- PENDING
- PROCESSING
- SUCCESS
- FAILED

## 4.4 影响等级
- LOW
- MEDIUM
- HIGH

## 4.5 分块类型
- concept
- rule
- example
- glossary
- raw

---

# 5. 建表 SQL

# 5.1 用户表

```sql
CREATE TABLE IF NOT EXISTS user_account (
  id BIGSERIAL PRIMARY KEY,
  username VARCHAR(100) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  nickname VARCHAR(100),
  status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_user_account_status ON user_account(status);
```

---

# 5.2 书籍表

```sql
CREATE TABLE IF NOT EXISTS book (
  id BIGSERIAL PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255),
  source_type VARCHAR(50),
  file_path VARCHAR(500) NOT NULL,
  file_size BIGINT,
  parse_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  remark TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_book_parse_status ON book(parse_status);
CREATE INDEX IF NOT EXISTS idx_book_created_at ON book(created_at);
```

---

# 5.3 书籍分块表

> 说明：向量维度 `1024` 仅为示例，实际请按所选 embedding 模型维度调整。

```sql
CREATE TABLE IF NOT EXISTS book_chunk (
  id BIGSERIAL PRIMARY KEY,
  book_id BIGINT NOT NULL REFERENCES book(id) ON DELETE CASCADE,
  chapter_title VARCHAR(255),
  chunk_index INT NOT NULL,
  content TEXT NOT NULL,
  content_type VARCHAR(50),
  tags_json JSONB,
  metadata_json JSONB,
  embedding VECTOR(1024),
  embedding_model VARCHAR(100),
  embedding_provider VARCHAR(100),
  embedding_dim INT,
  embedding_version VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_book_chunk_book_id ON book_chunk(book_id);
CREATE INDEX IF NOT EXISTS idx_book_chunk_content_type ON book_chunk(content_type);
CREATE INDEX IF NOT EXISTS idx_book_chunk_created_at ON book_chunk(created_at);
CREATE INDEX IF NOT EXISTS idx_book_chunk_tags_json ON book_chunk USING GIN(tags_json);
CREATE INDEX IF NOT EXISTS idx_book_chunk_metadata_json ON book_chunk USING GIN(metadata_json);
```

## 可选向量索引（后续规模上来再启用）
```sql
-- 示例：使用 cosine 距离
-- CREATE INDEX idx_book_chunk_embedding_ivfflat
-- ON book_chunk USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);
```

---

# 5.4 文档处理任务表

```sql
CREATE TABLE IF NOT EXISTS doc_process_task (
  id BIGSERIAL PRIMARY KEY,
  task_type VARCHAR(50) NOT NULL,
  ref_id BIGINT NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  error_message TEXT,
  payload_json JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_doc_process_task_status ON doc_process_task(status);
CREATE INDEX IF NOT EXISTS idx_doc_process_task_type ON doc_process_task(task_type);
CREATE INDEX IF NOT EXISTS idx_doc_process_task_ref_id ON doc_process_task(ref_id);
CREATE INDEX IF NOT EXISTS idx_doc_process_task_created_at ON doc_process_task(created_at);
```

---

# 5.5 卦例表

```sql
CREATE TABLE IF NOT EXISTS divination_case (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES user_account(id) ON DELETE SET NULL,
  title VARCHAR(255),
  question_text TEXT NOT NULL,
  question_category VARCHAR(100),
  divination_method VARCHAR(100),
  divination_time TIMESTAMP NOT NULL,
  status VARCHAR(50) NOT NULL DEFAULT 'ANALYZED',
  result_feedback TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_divination_case_user_id ON divination_case(user_id);
CREATE INDEX IF NOT EXISTS idx_divination_case_category ON divination_case(question_category);
CREATE INDEX IF NOT EXISTS idx_divination_case_status ON divination_case(status);
CREATE INDEX IF NOT EXISTS idx_divination_case_divination_time ON divination_case(divination_time);
CREATE INDEX IF NOT EXISTS idx_divination_case_created_at ON divination_case(created_at);
```

---

# 5.6 排盘快照表

```sql
CREATE TABLE IF NOT EXISTS case_chart_snapshot (
  id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL REFERENCES divination_case(id) ON DELETE CASCADE,
  chart_json JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_case_chart_snapshot_case_id ON case_chart_snapshot(case_id);
CREATE INDEX IF NOT EXISTS idx_case_chart_snapshot_chart_json ON case_chart_snapshot USING GIN(chart_json);
```

---

# 5.7 规则命中表

```sql
CREATE TABLE IF NOT EXISTS case_rule_hit (
  id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL REFERENCES divination_case(id) ON DELETE CASCADE,
  rule_code VARCHAR(100) NOT NULL,
  rule_name VARCHAR(255) NOT NULL,
  hit_reason TEXT,
  impact_level VARCHAR(50),
  explanation TEXT,
  evidence_json JSONB,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_case_rule_hit_case_id ON case_rule_hit(case_id);
CREATE INDEX IF NOT EXISTS idx_case_rule_hit_rule_code ON case_rule_hit(rule_code);
CREATE INDEX IF NOT EXISTS idx_case_rule_hit_impact_level ON case_rule_hit(impact_level);
CREATE INDEX IF NOT EXISTS idx_case_rule_hit_evidence_json ON case_rule_hit USING GIN(evidence_json);
```

---

# 5.8 AI 分析记录表

```sql
CREATE TABLE IF NOT EXISTS case_analysis_record (
  id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL REFERENCES divination_case(id) ON DELETE CASCADE,
  model_provider VARCHAR(100),
  model_name VARCHAR(100),
  prompt_text TEXT,
  response_text TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_case_analysis_record_case_id ON case_analysis_record(case_id);
CREATE INDEX IF NOT EXISTS idx_case_analysis_record_model_name ON case_analysis_record(model_name);
CREATE INDEX IF NOT EXISTS idx_case_analysis_record_created_at ON case_analysis_record(created_at);
```

---

# 5.9 案例向量表

```sql
CREATE TABLE IF NOT EXISTS case_embedding (
  id BIGSERIAL PRIMARY KEY,
  case_id BIGINT NOT NULL REFERENCES divination_case(id) ON DELETE CASCADE,
  summary_text TEXT NOT NULL,
  embedding VECTOR(1024),
  embedding_model VARCHAR(100),
  embedding_provider VARCHAR(100),
  embedding_dim INT,
  embedding_version VARCHAR(50),
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## 推荐索引
```sql
CREATE INDEX IF NOT EXISTS idx_case_embedding_case_id ON case_embedding(case_id);
CREATE INDEX IF NOT EXISTS idx_case_embedding_created_at ON case_embedding(created_at);
```

## 可选向量索引
```sql
-- CREATE INDEX idx_case_embedding_ivfflat
-- ON case_embedding USING ivfflat (embedding vector_cosine_ops)
-- WITH (lists = 100);
```

---

# 6. 推荐的初始化顺序

1. 启用 `pgvector`
2. 建基础表
3. 建普通索引
4. 跑通主链路
5. 数据量上来后再加向量索引

---

# 7. 典型查询示例

## 7.1 查询某个卦例详情
```sql
SELECT *
FROM divination_case
WHERE id = :case_id;
```

## 7.2 查询某个卦例对应的盘面快照
```sql
SELECT chart_json
FROM case_chart_snapshot
WHERE case_id = :case_id
ORDER BY created_at DESC
LIMIT 1;
```

## 7.3 查询某个卦例命中的规则
```sql
SELECT rule_code, rule_name, hit_reason, impact_level, explanation
FROM case_rule_hit
WHERE case_id = :case_id
ORDER BY created_at ASC;
```

## 7.4 查询某本书的所有 rule 类型 chunk
```sql
SELECT id, chapter_title, content
FROM book_chunk
WHERE book_id = :book_id
  AND content_type = 'rule'
ORDER BY chunk_index ASC;
```

## 7.5 基于余弦距离做知识块相似搜索
```sql
SELECT id, book_id, chapter_title, content,
       1 - (embedding <=> :query_vector) AS score
FROM book_chunk
WHERE embedding IS NOT NULL
ORDER BY embedding <=> :query_vector
LIMIT 10;
```

## 7.6 基于标签过滤 + 向量搜索
```sql
SELECT id, book_id, chapter_title, content,
       1 - (embedding <=> :query_vector) AS score
FROM book_chunk
WHERE embedding IS NOT NULL
  AND tags_json ? '求职'
ORDER BY embedding <=> :query_vector
LIMIT 10;
```

---

# 8. 推荐的 JSON 结构示例

## 8.1 `tags_json`
```json
["求职", "用神", "官鬼"]
```

## 8.2 `metadata_json`
```json
{
  "page": 42,
  "section": "用神篇",
  "source_file_name": "六爻大全.pdf",
  "clean_version": "v1"
}
```

## 8.3 `payload_json`
```json
{
  "bookId": 101,
  "filePath": "/data/books/liuyao.pdf",
  "parseMode": "STANDARD"
}
```

## 8.4 `evidence_json`
```json
{
  "lineIndex": 3,
  "useGod": "官鬼",
  "kongWang": ["戌", "亥"]
}
```

## 8.5 `chart_json`
```json
{
  "mainHexagram": "乾为天",
  "changedHexagram": "天风姤",
  "shi": 3,
  "ying": 6,
  "riChen": "甲子",
  "yueJian": "寅",
  "kongWang": ["戌", "亥"]
}
```

---

# 9. 后续可扩展表

如果进入第二阶段或第三阶段，可以考虑新增：

- `rule_definition`：结构化规则库
- `book_parse_log`：详细解析日志
- `knowledge_query_log`：检索日志
- `analysis_feedback`：用户对分析质量的反馈
- `case_timeline`：卦例进展记录

MVP 阶段不是必须。

---

# 10. 落地建议

## 第一阶段必须建的表
- `book`
- `book_chunk`
- `doc_process_task`
- `divination_case`
- `case_chart_snapshot`
- `case_rule_hit`
- `case_analysis_record`

## 第二阶段可补
- `case_embedding`
- `user_account`

如果前期先自己开发调试，认证体系甚至可以先简化。

---

# 11. 总结

这套 SQL 设计强调三件事：

1. **先把链路跑通**
2. **所有关键分析结果都留痕**
3. **为模型和向量能力切换预留空间**

MVP 阶段推荐组合：

- PostgreSQL
- pgvector
- JSONB 快照
- 普通索引先行
- 向量索引后置
