# 六爻AI断卦系统 v2.0 基础设施与质量保障设计文档

> **文档版本**：1.0
> **最后更新**：2026-04-12
> **前置文档**：[产品需求文档](./六爻AI断卦系统产品需求文档.md) · [技术设计文档](./六爻AI断卦系统设计文档.md)
> **覆盖范围**：RAG知识管线升级、测试策略、DevOps部署、成本模型、Prompt版本管理与评估

---

## 一、RAG 知识管线升级（liuyao-worker）

### 1.1 现状分析

当前 Worker 管线流程：

```
古籍TXT/PDF → parse_txt/parse_pdf → clean_text → ClassicTextChunker → DashScopeEmbedder → book_chunk 表
```

**已有能力**：
- 按段落粗分块 + 按标点/触发词精分块（`ClassicTextChunker`）
- 7个固定主题标签（用神/世应/六亲/月破/日破/空亡/动爻）的关键词匹配
- `content_type` 三分类（rule/example/mixed）
- DashScope `text-embedding-v4` 1024维向量
- pgvector 向量存储 + 语义检索

**存在的问题**：

| 问题 | 影响 |
|------|------|
| 主题标签仅靠关键词匹配，覆盖率低 | RAG 召回率不足，大量有价值的chunk因未命中关键词被丢弃 |
| 古籍卦例没有结构化拆分 | "某人占X事，得X卦，断曰…结果…" 这类完整卦例被切碎，检索时拼不回来 |
| 缺少全文索引 | 无法做精确关键词匹配，只能靠语义近似 |
| 缺少结构化元数据 | 无法按"适用卦象/六亲关系/应期类型"精确过滤 |
| chunk 质量无评估 | 不知道哪些chunk有用、哪些是噪声 |

### 1.2 改造方案

#### 1.2.1 新增卦例级分块器 `CaseExampleChunker`

**目标**：识别古籍中的完整卦例，保持其完整性不被切碎。

**卦例识别模式**（以《增删卜易》为代表）：

```
起始标记：
  - "占…得…卦" / "某人占…" / "如占…" / "余测…"

结束标记：
  - 下一个卦例的起始标记
  - 下一个章节标题
  - 空行分隔的新段落（非续写）

内部结构：
  - 背景：谁问什么事
  - 卦象：得什么卦
  - 断语：断曰 / 野鹤曰
  - 结果：后来果然… / 果应…
```

**实现**：

```python
# liuyao-worker/app/chunker/case_example_chunker.py

import re
from app.chunker.base_chunker import BaseChunker
from app.schemas.chunk_models import ChunkDraft

# 卦例起始的正则模式
CASE_START_PATTERNS = [
    r"占.{1,10}得.{2,8}卦",
    r"某人占",
    r"如占",
    r"余测",
    r"余占",
    r"有人占",
    r"一人占",
    r"又占",
]
CASE_START_REGEX = re.compile("|".join(CASE_START_PATTERNS))

# 卦例结果的标志词
RESULT_MARKERS = ["后来", "果然", "果应", "应验", "后果", "其后"]


class CaseExampleChunker(BaseChunker):
    """
    专门识别古籍中的完整卦例，保持卦例结构完整。
    应在 ClassicTextChunker 之前执行，提取卦例后剩余文本再走通用分块。
    """

    def chunk(self, text: str, metadata: dict) -> list[ChunkDraft]:
        cases: list[ChunkDraft] = []
        remaining_parts: list[str] = []

        blocks = self._split_to_paragraphs(text)
        i = 0
        while i < len(blocks):
            block = blocks[i]
            if CASE_START_REGEX.search(block):
                # 发现卦例起始 → 向后收集直到下一个卦例起始或章节标题
                case_content = block
                j = i + 1
                while j < len(blocks):
                    next_block = blocks[j]
                    if CASE_START_REGEX.search(next_block):
                        break  # 下一个卦例开始了
                    if self._is_chapter_heading(next_block):
                        break
                    case_content += "\n" + next_block
                    j += 1

                has_result = any(m in case_content for m in RESULT_MARKERS)
                hexagram_name = self._extract_hexagram_name(case_content)

                cases.append(ChunkDraft(
                    content=case_content.strip(),
                    chapter_title=metadata.get("chapter_title"),
                    split_level=1,
                    content_type="case_example",
                    topic_tags=self._detect_topics(case_content),
                    focus_topic=self._detect_topics(case_content)[0]
                        if self._detect_topics(case_content) else None,
                    metadata={
                        "is_complete_case": has_result,
                        "hexagram_name": hexagram_name,
                        "char_count": len(case_content),
                    },
                ))
                i = j
            else:
                remaining_parts.append(block)
                i += 1

        return cases  # remaining_parts 交给 ClassicTextChunker 处理

    def get_remaining_text(self) -> str:
        """返回未被识别为卦例的剩余文本，供后续分块器处理"""
        # 实现略
        ...
```

#### 1.2.2 LLM 辅助元数据提取

**目标**：对每个 chunk 用 LLM 提取结构化元数据标签，提升 RAG 检索的精确度。

**新增 Pipeline 阶段**：`MetadataEnrichmentStage`

```python
# liuyao-worker/app/pipeline/metadata_enrichment.py

METADATA_EXTRACTION_PROMPT = """
你是一位六爻知识库的索引专家。请分析下面这段古籍文本，提取结构化元数据。

<text>
{chunk_content}
</text>

请以JSON格式输出：
{{
  "applicable_hexagrams": ["卦名1", "卦名2"],    // 这段文本适用于哪些卦象，没有就空数组
  "liu_qin_focus": ["妻财", "官鬼"],              // 涉及哪些六亲，没有就空数组
  "topic_tags": ["用神", "空亡"],                  // 涉及哪些主题，从以下选：用神/世应/六亲/月破/日破/空亡/动爻/应期/化解/旺衰
  "scenario_types": ["求财", "婚姻"],              // 适用什么占卜场景，没有就空数组
  "has_timing_prediction": true,                  // 是否包含应期（时间预测）相关内容
  "knowledge_type": "rule"                        // rule=断卦规则 / case=卦例 / theory=理论 / commentary=注释
}}

只输出JSON，不要解释。
"""


class MetadataEnrichmentStage:
    def __init__(self, llm_client):
        self.llm_client = llm_client

    def enrich(self, chunks: list[ChunkDraft]) -> list[ChunkDraft]:
        """批量为chunk提取结构化元数据并合并到原有metadata中"""
        for chunk in chunks:
            try:
                extracted = self.llm_client.chat(
                    METADATA_EXTRACTION_PROMPT.format(chunk_content=chunk.content),
                    temperature=0.1,
                    max_tokens=300,
                    force_json=True,
                )
                if extracted:
                    chunk.metadata.update(extracted)
                    # 合并LLM提取的topic_tags到原有标签
                    llm_topics = extracted.get("topic_tags", [])
                    merged = list(set(chunk.topic_tags + llm_topics))
                    chunk.topic_tags = merged
                    if not chunk.focus_topic and merged:
                        chunk.focus_topic = merged[0]
            except Exception:
                pass  # LLM提取失败不中断主流程
        return chunks
```

**成本估算**：假设 500 个 chunk，每个 ~200 tokens input + ~100 tokens output：
- qwen-turbo: 500 × 300 tokens × ¥0.001/1K ≈ ¥0.15（可接受的一次性成本）

#### 1.2.3 PostgreSQL 全文索引

**目标**：支持混合检索中的关键词精确匹配。

```sql
-- V18__add_fulltext_search_to_book_chunk.sql

-- 新增 tsvector 列（使用 simple 配置，因为中文需要分词）
ALTER TABLE book_chunk
    ADD COLUMN IF NOT EXISTS content_tsv tsvector;

-- 生成 tsvector（使用 simple 配置 + 手动分词）
-- 由于 PostgreSQL 原生不支持中文分词，使用 pg_jieba 或退而用 simple + bigram
UPDATE book_chunk
SET content_tsv = to_tsvector('simple', content);

-- GIN 索引
CREATE INDEX IF NOT EXISTS idx_book_chunk_content_tsv
    ON book_chunk USING GIN(content_tsv);

-- 触发器：新插入时自动维护 tsvector
CREATE OR REPLACE FUNCTION book_chunk_tsv_trigger() RETURNS trigger AS $$
BEGIN
    NEW.content_tsv := to_tsvector('simple', NEW.content);
    RETURN NEW;
END
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_book_chunk_tsv
    BEFORE INSERT OR UPDATE OF content ON book_chunk
    FOR EACH ROW
    EXECUTE FUNCTION book_chunk_tsv_trigger();
```

**Java 侧查询**（`BookChunkVectorSearchRepository` 新增方法）：

```java
/**
 * 关键词检索：使用 PostgreSQL tsquery 做全文匹配。
 * 中文场景下由于分词限制，退化为 LIKE 模糊匹配 + topic 标签过滤。
 */
@Query(value = """
    SELECT id, book_id, content, chapter_title, focus_topic,
           topic_tags_json, metadata_json,
           ts_rank(content_tsv, plainto_tsquery('simple', :query)) AS rank
    FROM book_chunk
    WHERE content_tsv @@ plainto_tsquery('simple', :query)
    ORDER BY rank DESC
    LIMIT :limit
    """, nativeQuery = true)
List<BookChunkVectorSearchRow> keywordSearch(
    @Param("query") String query,
    @Param("limit") int limit);
```

#### 1.2.4 book_chunk 表结构变更

```sql
-- V19__enhance_book_chunk_metadata.sql

-- 新增结构化元数据列（从 metadata_json 中提取为独立列，加速过滤）
ALTER TABLE book_chunk
    ADD COLUMN IF NOT EXISTS knowledge_type VARCHAR(50),           -- rule/case/theory/commentary
    ADD COLUMN IF NOT EXISTS has_timing_prediction BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS scenario_types_json TEXT,              -- ["求财","婚姻"]
    ADD COLUMN IF NOT EXISTS applicable_hexagrams_json TEXT,        -- ["天火同人"]
    ADD COLUMN IF NOT EXISTS liu_qin_focus_json TEXT;               -- ["妻财","官鬼"]

-- 索引
CREATE INDEX IF NOT EXISTS idx_book_chunk_knowledge_type
    ON book_chunk(knowledge_type);
CREATE INDEX IF NOT EXISTS idx_book_chunk_has_timing
    ON book_chunk(has_timing_prediction) WHERE has_timing_prediction = TRUE;
```

#### 1.2.5 改造后的 Pipeline 流程

```
古籍文件
  │
  ├── 1. parse_txt / parse_pdf          （保留）
  ├── 2. clean_text                     （保留）
  ├── 3. CaseExampleChunker             （NEW — 优先提取完整卦例）
  │      ├── 卦例 chunks → content_type = "case_example"
  │      └── 剩余文本 ↓
  ├── 4. ClassicTextChunker             （保留 — 处理剩余文本）
  ├── 5. MetadataEnrichmentStage        （NEW — LLM 提取结构化元数据）
  ├── 6. DashScopeEmbedder              （保留 — 向量化）
  └── 7. 写入 book_chunk 表 + 全文索引自动维护
```

---

## 二、测试策略

### 2.1 测试分层

```
┌─────────────────────────────────────────────┐
│  E2E 测试 (Playwright / 手动)                │  ← 关键用户旅程
│  ┌─────────────────────────────────────────┐ │
│  │  集成测试 (Spring Boot Test)              │ │  ← API 契约验证
│  │  ┌─────────────────────────────────────┐ │ │
│  │  │  Prompt 回归测试 (Golden Dataset)     │ │ │  ← LLM 输出质量
│  │  │  ┌─────────────────────────────────┐ │ │ │
│  │  │  │  单元测试 (JUnit / Vitest / pytest)│ │ │ │  ← 业务逻辑
│  │  │  └─────────────────────────────────┘ │ │ │
│  │  └─────────────────────────────────────┘ │ │
│  └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

### 2.2 后端单元测试（JUnit 5）

#### 现有测试（保留并增强）

| 测试文件 | 覆盖范围 | 状态 |
|---------|---------|------|
| `UseGodStrengthRuleTest` | 用神旺衰规则 | ✅ 保留 |
| `UseGodEmptyRuleTest` | 空亡规则 | ✅ 保留 |
| `ShiYingRelationRuleTest` | 世应关系 | ✅ 保留 |
| `MovingLineExistsRuleTest` | 动爻检测 | ✅ 保留 |
| `AnalysisSectionComposerTest` | 机械文本拼接 | ✅ 保留（降级验证） |
| `AnalysisContextFactoryTest` | 分析上下文构建 | ✅ 保留 |

#### 新增测试

| 测试类 | 覆盖范围 | 关键测试用例 |
|--------|---------|-------------|
| `SessionServiceTest` | Session 生命周期 | 创建→追问→关闭；超时自动关闭；消息上限拒绝 |
| `ContextWindowBuilderTest` | Token 预算管理 | 5轮内完整保留；6轮起压缩摘要；总Token不超限 |
| `LlmClientTest` | LLM调用 | 正常JSON响应解析；非法JSON修复；超时降级；API Key无效 |
| `VerificationEventServiceTest` | 应验事件 | 应期文本解析（"下个月中旬"→日期）；反馈提交；过期处理 |
| `PromptTemplateEngineTest` | Prompt模板 | 模板加载；变量注入；类别匹配Few-Shot |
| `HybridSearchTest` | 混合检索 | 语义+关键词合并去重；拒识阈值过滤；空结果处理 |

#### LLM Mock 方案

```java
/**
 * 测试环境的 LLM Mock，返回固定的结构化 JSON。
 * 保证测试可重复、无API成本。
 */
@Profile("test")
@Component
public class MockLlmClient extends LlmClient {

    private static final String MOCK_ANALYSIS_JSON = """
        {
          "analysis": {
            "hexagramOverview": "测试卦象概览",
            "useGodAnalysis": "测试用神分析",
            "detailedReasoning": "测试推演",
            "classicReferences": [],
            "conclusion": "测试结论",
            "actionPlan": ["建议1", "建议2"],
            "predictedTimeline": null,
            "emotionalTone": "CALM"
          },
          "metadata": { "confidence": 0.8, "modelUsed": "mock", "ragSourceCount": 0, "processingTimeMs": 10 },
          "smartPrompts": ["追问1?", "追问2?", "追问3?"]
        }
        """;

    @Override
    public LlmResponse chat(List<ChatMessage> messages, LlmRequestOptions options) {
        return new LlmResponse(
            true, MOCK_ANALYSIS_JSON, parseJson(MOCK_ANALYSIS_JSON),
            "mock", 100, 50, 10, null
        );
    }
}
```

**配置**：

```yaml
# application-test.yml
spring:
  profiles:
    active: test

liuyao:
  llm:
    enabled: true  # 启用但走Mock
```

### 2.3 Prompt 回归测试（黄金数据集）

这是整个测试策略中最关键的一环。

#### 黄金数据集结构

```
src/test/resources/golden-dataset/
├── cases/
│   ├── career_promotion_positive.json    ← 事业升职-正面
│   ├── career_promotion_negative.json    ← 事业升职-负面
│   ├── relationship_reconcile.json       ← 感情复合
│   ├── health_recovery.json              ← 健康恢复
│   ├── finance_investment.json           ← 财运投资
│   ├── edge_empty_rag.json              ← 边界：RAG无结果
│   ├── edge_all_negative.json           ← 边界：全部规则负面
│   ├── edge_ambiguous_question.json     ← 边界：模糊提问
│   ├── edge_followup_5rounds.json       ← 边界：5轮追问
│   └── edge_followup_10rounds.json      ← 边界：10轮追问
└── schema/
    └── analysis_output_schema.json       ← JSON Schema 定义
```

#### 单个测试Case格式

```json
{
  "caseId": "career_promotion_positive",
  "description": "事业升职-正面场景：用神旺相、世应相生",
  "input": {
    "question": "我下个月能升职吗？",
    "questionCategory": "升职",
    "chartSnapshot": {
      "mainHexagram": "天火同人",
      "changedHexagram": "天雷无妄",
      "useGod": "官鬼",
      "palace": "离宫",
      "palaceWuXing": "火",
      "shi": 2, "ying": 5,
      "riChen": "巳", "yueJian": "辰",
      "kongWang": ["午", "未"],
      "lines": [ "..." ]
    },
    "ruleHits": [
      { "ruleCode": "USE_GOD_STRENGTH", "impactLevel": "HIGH", "scoreDelta": 2, "hitReason": "用神旺相" },
      { "ruleCode": "SHI_YING_RELATION", "impactLevel": "HIGH", "scoreDelta": 2, "hitReason": "应生世" }
    ],
    "structuredResult": {
      "effectiveScore": 4, "effectiveResultLevel": "GOOD"
    },
    "knowledgeSnippets": [
      "[《增删卜易·官鬼章》] 官鬼持世旺相，升迁有望..."
    ]
  },
  "assertions": {
    "jsonSchemaValid": true,
    "conclusionDirection": "POSITIVE",
    "mustNotContain": ["必死", "绝对", "注定失败", "破产"],
    "mustContain": ["建议", "行动"],
    "actionPlanMinLength": 2,
    "classicReferencesMaxFabricated": 0,
    "smartPromptsLength": 3,
    "emotionalToneOneOf": ["CALM", "ENCOURAGING"]
  }
}
```

#### 回归测试执行器

```java
/**
 * Prompt 回归测试。
 * 对每个黄金数据集Case，调用真实LLM，断言输出满足质量规则。
 *
 * 运行方式：mvn test -Dtest=PromptRegressionTest -Dspring.profiles.active=prompt-test
 * 注意：此测试调用真实LLM，有API成本，不在CI常规流水线中运行，
 *       而是在Prompt变更时手动触发。
 */
@SpringBootTest
@ActiveProfiles("prompt-test")
@Tag("prompt-regression")
public class PromptRegressionTest {

    @Autowired OrchestratedAnalysisService analysisService;

    @ParameterizedTest
    @MethodSource("loadGoldenCases")
    void testGoldenCase(GoldenCase goldenCase) {
        AnalysisOutputDTO output = analysisService.analyzeInitial(goldenCase.toInput());

        // 1. JSON Schema 合规
        assertJsonSchemaValid(output);

        // 2. 结论方向一致性
        String direction = inferDirection(output.getAnalysis().getConclusion());
        assertEquals(goldenCase.getAssertions().getConclusionDirection(), direction,
            "结论方向不一致: " + goldenCase.getCaseId());

        // 3. 禁用词检查
        for (String forbidden : goldenCase.getAssertions().getMustNotContain()) {
            assertFalse(output.getAnalysis().getConclusion().contains(forbidden),
                "包含禁用词 '" + forbidden + "': " + goldenCase.getCaseId());
        }

        // 4. 必含元素检查
        for (String required : goldenCase.getAssertions().getMustContain()) {
            assertTrue(fullText(output).contains(required),
                "缺少必含词 '" + required + "': " + goldenCase.getCaseId());
        }

        // 5. 行动建议数量
        assertTrue(output.getAnalysis().getActionPlan().size()
                >= goldenCase.getAssertions().getActionPlanMinLength());

        // 6. 古籍引用不捏造（检查source是否在已知古籍列表中）
        for (var ref : output.getAnalysis().getClassicReferences()) {
            assertTrue(KNOWN_SOURCES.contains(extractBookName(ref.getSource())),
                "疑似捏造古籍引用: " + ref.getSource());
        }

        // 7. SmartPrompt 数量
        assertEquals(3, output.getSmartPrompts().size());
    }
}
```

### 2.4 集成测试（API 契约）

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SessionApiIntegrationTest {

    @Autowired TestRestTemplate restTemplate;

    @Test
    void 起卦创建Session_返回完整响应() {
        SessionCreateRequest request = buildCareerRequest();
        var response = restTemplate.postForEntity("/api/sessions", request, ApiResponse.class);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody().getData().get("sessionId"));
        assertNotNull(response.getBody().getData().get("chartSnapshot"));
        assertNotNull(response.getBody().getData().get("analysis"));
        assertNotNull(response.getBody().getData().get("smartPrompts"));
    }

    @Test
    void 追问_返回分析结果和新SmartPrompt() {
        // 1. 先创建Session
        String sessionId = createSession();

        // 2. 追问
        MessageRequest msgRequest = new MessageRequest("空亡对我有什么影响？");
        var response = restTemplate.postForEntity(
            "/api/sessions/" + sessionId + "/messages", msgRequest, ApiResponse.class);

        assertEquals(200, response.getStatusCodeValue());
        assertNotNull(response.getBody().getData().get("messageId"));
        assertNotNull(response.getBody().getData().get("smartPrompts"));
    }

    @Test
    void 已关闭Session追问_返回409() {
        String sessionId = createSession();
        restTemplate.delete("/api/sessions/" + sessionId);

        MessageRequest msgRequest = new MessageRequest("还想问一下...");
        var response = restTemplate.postForEntity(
            "/api/sessions/" + sessionId + "/messages", msgRequest, ApiResponse.class);

        assertEquals(409, response.getStatusCodeValue());
    }
}
```

### 2.5 前端测试

当前前端 0 测试。新增方案：

```
liuyao-h5/
├── vitest.config.ts                  ← 测试配置
├── src/
│   ├── __tests__/
│   │   ├── SessionPage.test.tsx      ← 对话页面核心交互
│   │   ├── SmartPrompt.test.tsx      ← 追问气泡点击
│   │   ├── CalendarView.test.tsx     ← 日历视图渲染
│   │   └── api.mock.ts              ← API Mock
```

**关键测试用例**：

| 测试 | 断言 |
|------|------|
| 起卦成功后进入对话页面 | 排盘卡片可见 + AI首次分析可见 + SmartPrompt可见 |
| 点击SmartPrompt发送追问 | 追问内容出现在对话区 + loading状态 + AI回复出现 |
| 10轮追问后排盘数据仍展示 | 排盘卡片中的本卦/变卦名称不变 |
| Session关闭后输入框禁用 | 输入框disabled + 提示"已关闭" |

### 2.6 Worker 测试增强

现有 7 个 pytest 测试保留，新增：

| 测试文件 | 覆盖 |
|---------|------|
| `test_case_example_chunker.py` | 卦例识别正确性（从增删卜易中提取5个已知卦例验证） |
| `test_metadata_enrichment.py` | LLM元数据提取格式合规（用Mock LLM） |
| `test_fulltext_search.py` | 全文索引写入和查询（需测试DB） |

---

## 三、DevOps 与部署

### 3.1 当前部署方式的问题

```bash
# 当前 start.sh 的问题：
# 1. 进程管理靠 PID 文件 → 僵尸进程难清理
# 2. 前端裸 npm run dev → 生产环境不应用开发服务器
# 3. 数据库密码硬编码 → POSTGRES_PASSWORD: postgres
# 4. 无健康检查 → 服务挂了无感知
# 5. 无日志聚合 → 散落在 app.log / worker.log / h5.log
```

### 3.2 容器化方案

#### Dockerfile — Java 后端

```dockerfile
# liuyao-app/Dockerfile
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app
COPY target/liuyao-app-*.jar app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Dockerfile — Python Worker

```dockerfile
# liuyao-worker/Dockerfile
FROM python:3.12-slim

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ app/

HEALTHCHECK --interval=60s --timeout=5s \
    CMD python -c "import psycopg; psycopg.connect('${LIUYAO_DB_DSN}')" || exit 1

CMD ["python", "-m", "app.main"]
```

#### Dockerfile — H5 前端（Nginx 静态部署）

```dockerfile
# liuyao-h5/Dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

```nginx
# liuyao-h5/nginx.conf
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    # SPA 路由 fallback
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://liuyao-app:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

### 3.3 统一 Docker Compose

```yaml
# docker-compose.yml (项目根目录)
version: "3.9"

services:
  postgres:
    image: pgvector/pgvector:pg16
    container_name: liuyao-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: liuyao
      POSTGRES_USER: ${DB_USER:-liuyao}
      POSTGRES_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}
    ports:
      - "${DB_PORT:-5432}:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER:-liuyao}"]
      interval: 10s
      timeout: 5s
      retries: 5

  app:
    build: ./liuyao-app
    container_name: liuyao-app
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/liuyao
      DB_USERNAME: ${DB_USER:-liuyao}
      DB_PASSWORD: ${DB_PASSWORD:?DB_PASSWORD is required}
      LIUYAO_LLM_API_KEY: ${LLM_API_KEY:-}
      LIUYAO_LLM_BASE_URL: ${LLM_BASE_URL:-https://dashscope.aliyuncs.com/compatible-mode/v1}
      LIUYAO_LLM_MODEL: ${LLM_MODEL:-qwen-plus}
    ports:
      - "${APP_PORT:-8080}:8080"
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s

  worker:
    build: ./liuyao-worker
    container_name: liuyao-worker
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      LIUYAO_DB_DSN: postgresql://${DB_USER:-liuyao}:${DB_PASSWORD}@postgres:5432/liuyao
      DASHSCOPE_API_KEY: ${DASHSCOPE_API_KEY:-}
      LIUYAO_EMBEDDING_PROVIDER: ${EMBEDDING_PROVIDER:-dashscope}

  h5:
    build: ./liuyao-h5
    container_name: liuyao-h5
    restart: unless-stopped
    depends_on:
      - app
    ports:
      - "${H5_PORT:-80}:80"

volumes:
  postgres-data:
```

#### `.env.example`（敏感信息模板）

```env
# .env.example — 复制为 .env 后填入真实值
DB_USER=liuyao
DB_PASSWORD=         # 必填
LLM_API_KEY=         # 必填，DashScope API Key
DASHSCOPE_API_KEY=   # Worker 用的 Embedding Key
LLM_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
LLM_MODEL=qwen-plus
```

### 3.4 CI Pipeline（GitHub Actions）

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

jobs:
  backend-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: pgvector/pgvector:pg16
        env:
          POSTGRES_DB: liuyao_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: ["5432:5432"]
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - name: Run tests
        working-directory: liuyao-app
        env:
          DB_URL: jdbc:postgresql://localhost:5432/liuyao_test
          DB_USERNAME: test
          DB_PASSWORD: test
          LIUYAO_LLM_ENABLED: false
        run: mvn test -B

  worker-test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-python@v5
        with:
          python-version: "3.12"
      - name: Install & test
        working-directory: liuyao-worker
        env:
          LIUYAO_EMBEDDING_PROVIDER: mock
        run: |
          pip install -r requirements.txt
          pytest tests/ -v

  frontend-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - name: Install & build
        working-directory: liuyao-h5
        run: |
          npm ci
          npm run build

  # Prompt 回归测试 — 仅在 prompts/ 目录变更时触发
  prompt-regression:
    runs-on: ubuntu-latest
    if: contains(github.event.head_commit.modified, 'prompts/')
    needs: backend-test
    services:
      postgres:
        image: pgvector/pgvector:pg16
        env:
          POSTGRES_DB: liuyao_test
          POSTGRES_USER: test
          POSTGRES_PASSWORD: test
        ports: ["5432:5432"]
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 21
      - name: Prompt regression
        working-directory: liuyao-app
        env:
          DB_URL: jdbc:postgresql://localhost:5432/liuyao_test
          DB_USERNAME: test
          DB_PASSWORD: test
          LIUYAO_LLM_API_KEY: ${{ secrets.LLM_API_KEY }}
          LIUYAO_LLM_ENABLED: true
        run: mvn test -B -Dtest=PromptRegressionTest -Dgroups=prompt-regression
```

---

## 四、成本模型

### 4.1 LLM API 成本估算

以 DashScope qwen-plus 为基准（¥0.004/1K tokens）：

| 场景 | Prompt Tokens | Completion Tokens | 单次成本 |
|------|:---:|:---:|:---:|
| 首次分析 | ~4,000 | ~1,200 | ¥0.021 |
| 追问（第2轮） | ~5,000 | ~800 | ¥0.023 |
| 追问（第5轮） | ~7,000 | ~800 | ¥0.031 |
| 追问（第10轮） | ~9,000 | ~800 | ¥0.039 |
| **单会话平均（3.5轮）** | — | — | **¥0.097** |

| 日活用户 | 人均会话数 | 日成本 | 月成本 |
|:---:|:---:|:---:|:---:|
| 100 | 1.5 | ¥14.6 | ¥438 |
| 1,000 | 1.5 | ¥146 | ¥4,380 |
| 10,000 | 1.5 | ¥1,460 | ¥43,800 |

### 4.2 Embedding 成本

| 场景 | 成本 |
|------|------|
| 古籍入库（一次性，~5000 chunks） | DashScope text-embedding-v4: ~¥2 |
| LLM元数据提取（一次性，~5000 chunks） | qwen-turbo: ~¥0.75 |
| 每次 RAG 查询 Embedding | ~¥0.0001/次（忽略不计） |

### 4.3 成本控制策略

| 策略 | 说明 | 预期节省 |
|------|------|---------|
| **追问用小模型** | 首次分析用 qwen-plus，追问用 qwen-turbo（便宜60%） | ~40% |
| **Token 预算控制** | ContextWindowBuilder 严格控制上下文大小 | 防止失控 |
| **用户请求限流** | 匿名用户: 5次/天，登录用户: 20次/天 | 防滥用 |
| **缓存相似问题** | 同一Session内相同追问直接返回缓存 | ~5% |
| **降级策略** | LLM不可用时返回机械文本，零API成本 | 保底 |

### 4.4 限流实现

```java
@Component
public class RateLimiter {

    // 简单实现：基于 Redis 或内存 Map
    private final Map<String, AtomicInteger> dailyCounts = new ConcurrentHashMap<>();

    private static final int ANONYMOUS_DAILY_LIMIT = 5;
    private static final int AUTHENTICATED_DAILY_LIMIT = 20;

    public void checkLimit(Long userId, boolean authenticated) {
        String key = (userId != null ? "user:" + userId : "anon:" + getClientIp())
                     + ":" + LocalDate.now();
        int limit = authenticated ? AUTHENTICATED_DAILY_LIMIT : ANONYMOUS_DAILY_LIMIT;
        int count = dailyCounts.computeIfAbsent(key, k -> new AtomicInteger(0))
                               .incrementAndGet();
        if (count > limit) {
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED,
                "今日请求次数已达上限（" + limit + "次），请明天再来");
        }
    }
}
```

### 4.5 成本监控

```yaml
# application.yml 新增
liuyao:
  cost:
    alert-daily-threshold-yuan: 50    # 日成本超过50元告警
    alert-monthly-threshold-yuan: 1000
```

```java
// 每次LLM调用后记录
@Async
public void recordCost(String model, int promptTokens, int completionTokens) {
    double cost = calculateCost(model, promptTokens, completionTokens);
    dailyCostAccumulator.addAndGet(cost);
    if (dailyCostAccumulator.get() > costProperties.getAlertDailyThresholdYuan()) {
        alertService.sendCostAlert("日LLM成本已达 ¥" + dailyCostAccumulator.get());
    }
}
```

---

## 五、Prompt 版本管理与效果评估

### 5.1 版本管理

#### 目录结构

```
src/main/resources/prompts/
├── v1/                                    ← 版本号目录
│   ├── system/
│   │   └── orchestrated_analyst.md
│   ├── context/
│   │   ├── chart_context.md
│   │   ├── rule_context.md
│   │   └── knowledge_context.md
│   ├── user/
│   │   ├── initial_analysis.md
│   │   └── follow_up.md
│   └── few_shot/
│       ├── example_career.md
│       ├── example_relationship.md
│       └── example_health.md
├── v2/                                    ← 新版本（迭代时创建）
│   └── ...
└── manifest.yml                           ← 版本清单
```

#### 版本清单 (manifest.yml)

```yaml
# prompts/manifest.yml
current_version: v1
versions:
  v1:
    created_at: "2026-04-12"
    author: "yishou"
    description: "初始编排式Prompt，含3类Few-Shot示例"
    changelog: "首次发布"
  # v2:
  #   created_at: "2026-05-xx"
  #   author: "xx"
  #   description: "优化情感识别，增加健康类Few-Shot"
  #   changelog: "改进了负面结论的表达方式"
```

#### 运行时版本切换

```yaml
# application.yml
liuyao:
  prompt:
    version: ${LIUYAO_PROMPT_VERSION:v1}      # 当前使用的Prompt版本
    ab-test-enabled: false                     # A/B测试开关
    ab-test-versions: ["v1", "v2"]             # A/B测试的两个版本
    ab-test-ratio: 0.5                         # v2的流量比例
```

```java
@Component
public class PromptTemplateEngine {

    @Value("${liuyao.prompt.version}")
    private String currentVersion;

    @Value("${liuyao.prompt.ab-test-enabled}")
    private boolean abTestEnabled;

    /**
     * 加载指定版本的Prompt模板。
     * A/B测试模式下，根据 sessionId hash 分流。
     */
    public String loadTemplate(String templateName, UUID sessionId) {
        String version = resolveVersion(sessionId);
        String path = "prompts/" + version + "/" + templateName;
        return loadResource(path);
    }

    private String resolveVersion(UUID sessionId) {
        if (!abTestEnabled) return currentVersion;
        // 基于 sessionId 的稳定哈希分流，确保同一Session始终用同一版本
        double hash = (sessionId.hashCode() & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL;
        return hash < abTestRatio ? abTestVersions.get(1) : abTestVersions.get(0);
    }
}
```

### 5.2 效果评估体系

#### 评估维度

| 维度 | 权重 | 评估方式 | 指标 |
|------|:---:|---------|------|
| **格式合规** | 20% | 自动化 | JSON Schema校验通过率 |
| **结论准确性** | 30% | 自动化 + 人工 | 与规则引擎结论方向一致性 |
| **安全合规** | 20% | 自动化 | 禁用词命中率=0 |
| **同理心/语气** | 15% | LLM-as-Judge | 1-5分评分 |
| **实用性** | 15% | LLM-as-Judge | 1-5分评分（行动建议的可操作性） |

#### LLM-as-Judge 评分器

```python
# tools/prompt_evaluator.py — 可独立运行的评估脚本

JUDGE_PROMPT = """
你是一位AI输出质量评审员。请评估以下六爻分析输出的质量。

<user_question>{question}</user_question>
<ai_output>{ai_output}</ai_output>

请从以下维度打分（1-5分）并给出简短理由：

1. **同理心与语气** (tone_score): AI是否展现了同理心？语气是否合适？是否有绝对宿命论？
2. **实用性** (usefulness_score): 行动建议是否具体可操作？是否空洞泛化？
3. **专业性** (expertise_score): 六爻相关内容是否准确？引用是否得当？

以JSON输出：
{{
  "tone_score": 4,
  "tone_reason": "语气温和，有安抚效果，但可以更个性化",
  "usefulness_score": 3,
  "usefulness_reason": "建议较泛化，可以更具体",
  "expertise_score": 5,
  "expertise_reason": "六爻分析准确，引用恰当"
}}
"""


def evaluate_output(question: str, ai_output: str, judge_model: str = "qwen-plus") -> dict:
    """用LLM做为评审员，对AI输出打分"""
    prompt = JUDGE_PROMPT.format(question=question, ai_output=ai_output)
    # 调用LLM获取评分...
    return scores


def run_evaluation_suite(golden_dataset_path: str, prompt_version: str):
    """对黄金数据集跑完整评估，输出报告"""
    cases = load_golden_cases(golden_dataset_path)
    results = []
    for case in cases:
        output = call_analysis_api(case, prompt_version)

        # 自动化检查
        schema_valid = validate_json_schema(output)
        direction_match = check_direction_consistency(output, case)
        safety_pass = check_forbidden_words(output)

        # LLM-as-Judge 评分
        judge_scores = evaluate_output(case.question, output.conclusion)

        results.append({
            "case_id": case.case_id,
            "schema_valid": schema_valid,
            "direction_match": direction_match,
            "safety_pass": safety_pass,
            **judge_scores,
        })

    # 输出评估报告
    generate_report(results, prompt_version)
```

#### 评估报告格式

```
========================================
  Prompt 评估报告
  版本: v2 vs v1 (基线)
  日期: 2026-05-15
  测试集: 10 个黄金数据集Case
========================================

┌────────────────────┬────────┬────────┐
│ 指标               │  v1    │  v2    │
├────────────────────┼────────┼────────┤
│ JSON Schema 通过率  │ 100%   │ 100%   │
│ 结论方向一致性      │  90%   │  90%   │
│ 安全合规通过率      │ 100%   │ 100%   │
│ 同理心评分 (avg)    │  3.8   │  4.2 ↑ │
│ 实用性评分 (avg)    │  3.5   │  4.0 ↑ │
│ 专业性评分 (avg)    │  4.0   │  4.1   │
│ 综合加权分          │  3.72  │  4.02 ↑│
├────────────────────┼────────┼────────┤
│ 结论               │        │ ✅ 可上线│
└────────────────────┴────────┴────────┘

退化Case: 无
```

### 5.3 Prompt 变更流程

```
1. 开发者修改 prompts/v2/ 目录下的模板
      │
2. 本地运行 Prompt 回归测试
   mvn test -Dtest=PromptRegressionTest -Dgroups=prompt-regression
      │
3. 通过 → 提交PR，CI自动触发 prompt-regression job
      │
4. CI通过 → 运行评估脚本，生成对比报告
   python tools/prompt_evaluator.py --baseline v1 --candidate v2
      │
5. 评估报告附在PR中供Review
      │
6. 合入后，修改 manifest.yml: current_version → v2
      │
7. 或开启 A/B 测试，观察线上指标后再全量切换
```

---

## 六、交付排期（与主设计文档对齐）

这5个维度并非独立阶段，而是嵌入到主文档的 Phase 1~3 中：

| 主Phase | 本文档对应交付 |
|---------|--------------|
| **Phase 0（准备期，1周）** | Docker化 + CI Pipeline + `.env` 管理 + LLM Mock |
| **Phase 1（W1-W4）** | Prompt模板外置 + 版本管理 + ContextWindowBuilder测试 + SessionService测试 |
| **Phase 2（W5-W7）** | 应验事件测试 + 黄金数据集 + Prompt回归测试框架 |
| **Phase 3（W8-W9）** | RAG管线升级（卦例分块 + 元数据提取 + 全文索引） + 前端测试 + 成本监控 |
| **持续** | Prompt评估流程 + A/B测试 + 成本报表 |
