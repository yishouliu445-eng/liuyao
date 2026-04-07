# 六爻系统 Python Worker 设计文档

> 版本：v1.0  
> 适用阶段：MVP / 第一阶段可落地实现  
> 目标：承接 Java 主服务创建的文档处理任务，完成 TXT 文本清洗、切片、标签识别、embedding 与入库

---

# 1. 文档目标

这份文档用于定义 **Python 辅助服务 / Worker** 的职责、模块边界、处理流程和落地方式。

在当前系统分工里：

- **Java 主服务** 已负责：
  - 接收上传
  - 元数据管理
  - 任务调度
  - 结果展示
- **Python Worker** 负责：
  - 读取原始文本
  - 文本清洗
  - 章节 / 段落识别
  - 古籍专用切片
  - 标签识别
  - chunk 类型判定
  - embedding
  - 结果写回 PostgreSQL

所以 Python Worker 的定位不是“第二个后端”，而是：

**一个面向知识入库的文本加工流水线。**

---

# 2. 设计原则

## 2.1 单一职责
Python Worker 只做数据加工，不负责：
- 用户权限
- HTTP 主业务 API
- 卦例管理
- 排盘引擎
- 规则引擎主逻辑

## 2.2 可替换
后续如果你想把：
- embedding 模型
- chunk 规则
- 标签策略
- OCR 流程

替换掉，不应该影响 Java 主服务。

## 2.3 可回溯
每个 chunk 都必须能追踪到：
- 来源书籍
- 原始块
- 切分层级
- 主题标签
- 生成版本

## 2.4 先规则，后智能
第一阶段切片逻辑以规则为主，不依赖复杂模型：
- 标题识别
- 长段二次切分
- 主题簇判断
- 卦例结构切分

## 2.5 可诊断
处理结果必须支持排查：
- 为什么只切出 18 条
- 哪些块过长
- 哪些块多主题未细分
- 哪些块类型判断错误

---

# 3. Python Worker 在整体架构中的位置

```text
[前端]
   ↓
[Java 主服务]
   ├── 上传书籍
   ├── 保存 book
   ├── 创建 doc_process_task
   └── 查询结果
          ↓
[Python Worker]
   ├── 拉取待处理任务
   ├── 读取 TXT
   ├── 清洗
   ├── 粗切
   ├── 细切
   ├── 标签识别
   ├── embedding
   └── 写回 PostgreSQL
          ↓
[PostgreSQL + pgvector]
```

---

# 4. 职责边界

# 4.1 Java 主服务负责

- `POST /api/books/upload`
- 保存 `book`
- 保存 `doc_process_task`
- 展示书籍状态
- 展示 chunk 列表
- 触发重试（可选）

# 4.2 Python Worker 负责

- 查询待处理任务
- 读取 `book.file_path`
- 解析 TXT 内容
- 执行切片 pipeline
- 调用 embedding
- 写入 `book_chunk`
- 更新任务状态

---

# 5. 推荐项目结构

```text
liuyao-worker/
  ├── app/
  │   ├── config/
  │   │   ├── settings.py
  │   │   └── logging_config.py
  │   ├── db/
  │   │   ├── connection.py
  │   │   ├── models.py
  │   │   └── repositories.py
  │   ├── task_runner/
  │   │   ├── worker.py
  │   │   ├── task_fetcher.py
  │   │   └── task_updater.py
  │   ├── parser/
  │   │   └── txt_parser.py
  │   ├── cleaner/
  │   │   └── text_cleaner.py
  │   ├── chunker/
  │   │   ├── base_chunker.py
  │   │   ├── classic_text_chunker.py
  │   │   ├── zengshan_buyi_chunker.py
  │   │   ├── title_detector.py
  │   │   ├── sentence_splitter.py
  │   │   ├── topic_classifier.py
  │   │   └── chunk_types.py
  │   ├── embedding/
  │   │   ├── base_embedder.py
  │   │   ├── ali_embedder.py
  │   │   └── mock_embedder.py
  │   ├── pipeline/
  │   │   ├── book_pipeline.py
  │   │   └── chunk_builders.py
  │   ├── schemas/
  │   │   ├── chunk_models.py
  │   │   └── task_models.py
  │   └── main.py
  ├── requirements.txt
  └── README.md
```

---

# 6. 核心模块说明

# 6.1 config

## 作用
集中管理配置，不把常量写死在逻辑代码里。

## 建议内容
- 数据库连接
- embedding 模型配置
- worker 轮询频率
- chunk 阈值参数
- 日志级别

## 示例
- `DB_URL`
- `EMBEDDING_PROVIDER`
- `EMBEDDING_MODEL`
- `COARSE_BLOCK_MAX_LEN`
- `FORCE_REFINE_LEN`
- `POLL_INTERVAL_SECONDS`

---

# 6.2 db

## 作用
处理数据库连接和持久化。

## 推荐内容
- PostgreSQL 连接
- 查询待处理任务
- 查询书籍信息
- 插入 `book_chunk`
- 更新 `doc_process_task`

## 建议方法
- `fetch_pending_tasks(task_type, limit)`
- `get_book_by_id(book_id)`
- `insert_book_chunks(chunks)`
- `mark_task_processing(task_id)`
- `mark_task_success(task_id)`
- `mark_task_failed(task_id, error_message)`

---

# 6.3 task_runner

## 作用
控制 Worker 的任务生命周期。

## 推荐职责
- 定时轮询待处理任务
- 抢占任务
- 调用 pipeline
- 写回成功 / 失败状态
- 记录异常日志

## 推荐流程
```text
获取任务
→ 标记 processing
→ 执行 pipeline
→ 成功则 success
→ 失败则 failed + error_message
```

---

# 6.4 parser

## 作用
负责把原始文件读成纯文本。

## 第一阶段支持
- TXT

## 后续可扩展
- PDF
- EPUB
- OCR 图片文本

## 当前建议
先把 TXT 做稳，再扩展其他格式。

---

# 6.5 cleaner

## 作用
对原始文本做预清洗。

## 典型处理
- 统一换行
- 清理空白
- 去掉页码噪声
- 标准化标点
- 删除明显目录残片
- 标题候选识别前的规范化

---

# 6.6 chunker

这是 Python Worker 最核心的模块。

## 作用
把清洗后的文本切成高质量 chunk。

## 建议拆分

### `base_chunker.py`
定义统一接口：
- `chunk(text, metadata) -> list[ChunkDraft]`

### `classic_text_chunker.py`
古籍通用切片器：
- 适配术数类、命理类、断法类文本

### `zengshan_buyi_chunker.py`
《增删卜易》专用切片器：
- 做更细的专用规则
- 适合你当前这本书

### `title_detector.py`
负责识别：
- 标题行
- 小节名
- 章名

### `sentence_splitter.py`
负责：
- 规则断句
- 古文触发词补切

### `topic_classifier.py`
负责：
- 主题簇识别
- 命中标签打标
- focus topic 选择

### `chunk_types.py`
定义 chunk 类型：
- `rule`
- `concept`
- `example`
- `mixed`

---

# 6.7 embedding

## 作用
负责把 chunk 文本转为向量。

## 设计建议
通过统一抽象隐藏供应商差异：

### `base_embedder.py`
```python
class BaseEmbedder:
    def embed(self, text: str) -> list[float]:
        raise NotImplementedError
```

### 实现类
- `AliEmbedder`
- `MockEmbedder`

## 为什么要这样做
因为你后续可能换：
- 阿里 embedding
- 其他国内模型
- 本地模型

业务 pipeline 不应该跟着改。

---

# 6.8 pipeline

## 作用
编排整条图书入库流水线。

## 推荐主流程
```text
读取文本
→ 清洗
→ 粗切
→ 细切
→ 标签识别
→ 类型判定
→ embedding
→ 入库
```

## 主编排函数建议
- `process_book_task(task_id, book_id)`

---

# 6.9 schemas

## 作用
定义 Python 内部的数据对象，避免到处传 dict。

## 推荐对象
- `TaskDTO`
- `BookDTO`
- `ChunkDraft`
- `ChunkRecord`

---

# 7. 数据对象设计

# 7.1 ChunkDraft

表示“尚未入库”的中间切片对象。

```python
from dataclasses import dataclass, field
from typing import Optional

@dataclass
class ChunkDraft:
    content: str
    chapter_title: Optional[str] = None
    source_block_id: Optional[str] = None
    parent_chunk_id: Optional[str] = None
    split_level: int = 1
    chunk_type: str = "mixed"
    topic_tags: list[str] = field(default_factory=list)
    focus_topic: Optional[str] = None
    char_count: int = 0
    sentence_count: int = 0
    metadata: dict = field(default_factory=dict)
```

---

# 7.2 ChunkRecord

表示最终准备写入数据库的对象。

```python
from dataclasses import dataclass
from typing import Optional

@dataclass
class ChunkRecord:
    book_id: int
    chapter_title: Optional[str]
    chunk_index: int
    content: str
    content_type: str
    tags_json: dict
    metadata_json: dict
    embedding: Optional[list[float]]
    embedding_model: Optional[str]
    embedding_provider: Optional[str]
    embedding_dim: Optional[int]
    embedding_version: Optional[str]
```

---

# 8. TXT 切片设计（重点）

你当前的资料是 TXT，《增删卜易》属于术数古籍文本，推荐采用：

**粗切 + 细切 + 多主题派生**

而不是：

**空行段落切 + 主题词过滤**

---

# 8.1 预清洗规则

## 必做
- `\r\n` → `\n`
- 连续空白压缩
- 连续 3 个以上空行压成 2 个
- 去掉行首行尾空格
- 去掉明显页码、扫描残留
- 标准化标点

---

# 8.2 一级粗切规则

按以下优先级：

1. 标题
2. 双换行
3. 明显分隔符
4. 特殊语义行起点

## 标题候选规则
- 单行长度 2～20
- 不含长叙述标点
- 包含“章 / 篇 / 诀 / 论 / 法 / 总”
- 前后有空行

---

# 8.3 二级细切触发条件

任一命中即细切：

- 粗块长度 > 350
- 粗块长度 > 600（强制细切）
- 命中主题簇 >= 2
- 同时命中规则簇和案例簇
- 出现多个触发词：
  - 凡
  - 若
  - 如
  - 又
  - 断曰
  - 占曰
  - 野鹤曰
  - 余曰

---

# 8.4 句群切分规则

## 一级断句符
- `。`
- `；`
- `！`
- `？`
- `：`

## 二级触发词补切
- `凡`
- `若`
- `如`
- `又`
- `断曰`
- `占曰`
- `野鹤曰`
- `余曰`
- `或曰`

---

# 8.5 主题簇

建议先做静态主题簇表：

## 用神簇
- 用神
- 取用
- 官鬼
- 妻财
- 子孙
- 父母
- 兄弟

## 世应簇
- 世
- 应
- 世爻
- 应爻
- 持世

## 旺衰簇
- 旺
- 相
- 休
- 囚
- 月建
- 日辰
- 生扶

## 空亡簇
- 空亡
- 旬空
- 填实
- 冲空

## 月破日破簇
- 月破
- 日破
- 逢破

## 动变簇
- 动爻
- 变爻
- 化进
- 化退
- 回头生
- 回头克

## 案例簇
- 占
- 占曰
- 某人
- 断曰
- 果验
- 应于
- 后果

---

# 8.6 多主题派生规则

这是你当前最需要的。

## 当前错误方式
一大段命中多个主题词，但仍只生成 1 条 chunk。

## 正确方式
允许一个粗块派生多个子块：

- `block_001_a`：focus_topic = 用神
- `block_001_b`：focus_topic = 空亡
- `block_001_c`：focus_topic = 月破

## 注意
它们可以共享：
- `source_block_id`
- 原文来源
- 章节信息

但每个子块必须有：
- 自己的 `focus_topic`
- 自己的 `chunk_type`
- 自己的切分层级

---

# 8.7 chunk 类型判定

## rule
特征：
- 凡、若、则、主、宜、忌
- 偏概括
- 规则感强

## concept
特征：
- 解释某概念
- 定义某术语
- 偏说明

## example
特征：
- 有人物 / 事件
- 有占问背景
- 有结果 / 应验

## mixed
拆不开时兜底，但应尽量减少。

---

# 8.8 长度阈值建议

```text
COARSE_BLOCK_MAX_LEN = 350
FORCE_REFINE_LEN = 600
RULE_CHUNK_MIN_LEN = 60
RULE_CHUNK_MAX_LEN = 260
EXAMPLE_CHUNK_MIN_LEN = 180
EXAMPLE_CHUNK_MAX_LEN = 600
MULTI_TOPIC_THRESHOLD = 2
```

---

# 9. Pipeline 详细流程

# 9.1 主流程

```text
task_runner
→ get book
→ parse txt
→ clean text
→ chunk text
→ build chunk drafts
→ classify topic / type
→ embed content
→ write book_chunk
→ mark task success
```

---

# 9.2 主流程伪代码

```python
def process_book_task(task):
    book = repository.get_book_by_id(task.ref_id)
    raw_text = txt_parser.read(book.file_path)
    clean_text = text_cleaner.clean(raw_text)

    chunk_drafts = zengshan_buyi_chunker.chunk(
        clean_text,
        metadata={
            "book_id": book.id,
            "book_title": book.title
        }
    )

    chunk_records = []
    for idx, draft in enumerate(chunk_drafts, start=1):
        vector = embedder.embed(draft.content)
        chunk_records.append(
            build_chunk_record(book.id, idx, draft, vector)
        )

    repository.insert_book_chunks(chunk_records)
    repository.mark_task_success(task.id)
```

---

# 10. 数据库存储映射建议

写入 `book_chunk` 时：

## chapter_title
来自标题识别结果

## content
最终切片内容

## content_type
- `rule`
- `concept`
- `example`
- `mixed`

## tags_json
建议结构：
```json
{
  "topic_tags": ["用神", "官鬼", "求职"],
  "focus_topic": "用神"
}
```

## metadata_json
建议结构：
```json
{
  "source_block_id": "block_00012",
  "parent_chunk_id": null,
  "split_level": 2,
  "char_count": 168,
  "sentence_count": 3,
  "chunker_name": "zengshan_buyi_chunker",
  "chunker_version": "v2.0"
}
```

---

# 11. 日志与诊断设计

这是非常重要的一部分。

## 11.1 推荐记录的统计指标
每本书处理后记录：

- 原始文本长度
- 粗块数量
- 进入细切的块数量
- 最终 chunk 数量
- 平均 chunk 长度
- 过长 chunk 数量
- 多主题 chunk 数量
- rule / concept / example 分布

## 11.2 推荐输出诊断文件
例如：
- `book_101_chunk_diagnostics.json`

用于排查：
- 为什么只切了 18 条
- 哪些块没细切
- 哪些主题识别失效

---

# 12. 失败处理

## 可恢复错误
- embedding API 临时失败
- 数据库写入暂时失败
- 文件路径异常

处理：
- 标记失败
- 写 error_message
- 支持重试

## 不可恢复错误
- TXT 内容为空
- 全书解析后无有效 chunk
- 配置缺失

处理：
- 直接 failed
- 保留详细错误信息

---

# 13. 配置建议

## settings.py 建议字段

```python
DB_URL = "postgresql://..."
DB_USER = "..."
DB_PASSWORD = "..."

POLL_INTERVAL_SECONDS = 5
TASK_BATCH_SIZE = 5

COARSE_BLOCK_MAX_LEN = 350
FORCE_REFINE_LEN = 600
RULE_CHUNK_MIN_LEN = 60
RULE_CHUNK_MAX_LEN = 260
EXAMPLE_CHUNK_MIN_LEN = 180
EXAMPLE_CHUNK_MAX_LEN = 600
MULTI_TOPIC_THRESHOLD = 2

EMBEDDING_PROVIDER = "ali"
EMBEDDING_MODEL = "text-embedding"
EMBEDDING_VERSION = "v1"
```

---

# 14. 第一阶段开发顺序建议

## 第 1 步
先把任务轮询和 TXT 读取跑通

## 第 2 步
实现 `TextCleaner`

## 第 3 步
实现 `ClassicTextChunker`

## 第 4 步
实现《增删卜易》专用 chunker

## 第 5 步
实现主题簇识别与多主题派生

## 第 6 步
接 mock embedding

## 第 7 步
接真实 embedding

---

# 15. 最小可用版本（MVP）

第一阶段先做到这些就够：

- 读取 TXT
- 清洗
- 标题识别
- 粗切
- 长块细切
- 多主题派生
- 类型判定
- 写入数据库

先别急着追求：
- 完美分类
- OCR
- rerank
- 多格式支持

---

# 16. 推荐的类与函数列表

## parser
- `read_txt(path: str) -> str`

## cleaner
- `clean(text: str) -> str`

## chunker
- `detect_titles(text: str) -> list[TitleMarker]`
- `coarse_split(text: str) -> list[str]`
- `refine_block(block: str) -> list[str]`
- `classify_topics(text: str) -> list[str]`
- `detect_chunk_type(text: str) -> str`
- `chunk(text: str, metadata: dict) -> list[ChunkDraft]`

## embedding
- `embed(text: str) -> list[float]`

## repository
- `fetch_pending_tasks()`
- `get_book_by_id(book_id: int)`
- `insert_book_chunks(chunks: list[ChunkRecord])`
- `mark_task_success(task_id: int)`
- `mark_task_failed(task_id: int, msg: str)`

---

# 17. 结论

这份 Python Worker 设计的核心思想是：

- **Java 管入口与业务**
- **Python 管文本加工**
- **先规则切片，后智能增强**
- **允许一个原始大段派生多个子 chunk**
- **每条 chunk 都必须可追溯、可诊断、可入库**

对于你当前的 TXT 场景，最关键的不是“再加主题词”，而是：

**建立一套古籍专用的二级切片机制。**

---

# 18. 下一步可继续产出

基于这份设计文档，下一步最适合继续细化为：

1. `requirements.txt` 推荐依赖清单  
2. Python Worker 项目骨架代码  
3. `zengshan_buyi_chunker.py` 可运行版本  
4. 数据库 repository 示例实现  
5. embedding 抽象与阿里模型接入模板
