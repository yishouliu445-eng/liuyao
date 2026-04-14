# Platform Runtime Refactor Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前六爻 `v2.0` 系统从“功能可用但平台能力分散”的状态，推进为统一分析执行契约、统一证据对象、统一验证降级和统一运行时治理的模块化平台基线。

**Architecture:** 采用“模块化单体先平台化、再保留后续拆分边界”的策略，在 `liuyao-app` 内新增分析执行平台、证据平台、评估平台和运行时平台的清晰边界，并通过兼容适配层逐步收口 `legacy analyze` 与 `Session` 主链路。第一阶段不物理拆服务，优先统一契约、审计、验证、证据和运行时能力。

**Tech Stack:** Java 17, Spring Boot 3.3, Maven, JPA, Flyway, PostgreSQL, pgvector, JUnit 5, 现有 `liuyao-worker`, 现有 Prompt Regression / Replay / Session API 测试体系

---

## Chunk 1: 统一分析执行契约

### Task 1: 为统一执行信封写失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/runtime/AnalysisExecutionServiceTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/session/controller/SessionApiIntegrationTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/PromptRegressionTest.java`

- [ ] **Step 1: 写“首次分析返回统一 execution envelope”的失败测试**

```java
@Test
void shouldReturnExecutionEnvelopeForInitialAnalysis() {
    AnalysisExecutionEnvelope envelope = service.execute(initialRequest());
    assertNotNull(envelope.getExecutionId());
    assertNotNull(envelope.getDeterministic());
    assertNotNull(envelope.getPresentation());
}
```

- [ ] **Step 2: 写“legacy analyze 也经由统一执行契约”的失败测试**

```java
@Test
void shouldBuildLegacyResponseFromExecutionEnvelope() {
    DivinationAnalyzeResponse response = divinationService.analyze(request);
    assertNotNull(response.getAnalysisContext());
    assertNotNull(response.getStructuredResult());
}
```

- [ ] **Step 3: 写“Prompt regression 可读取统一 execution 版本信息”的失败测试**

```java
assertEquals("v1", envelope.getVersions().getPromptVersion());
assertNotNull(envelope.getVersions().getModelVersion());
```

- [ ] **Step 4: 运行测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=AnalysisExecutionServiceTest,SessionApiIntegrationTest,PromptRegressionTest test
```

Expected:

- 缺少 `analysis.runtime` 契约类
- 现有 `SessionService` 和 `DivinationService` 尚未统一收口

### Task 2: 新增分析执行契约对象

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/contract/AnalysisExecutionRequest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/contract/AnalysisExecutionEnvelope.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/contract/DeterministicFactSnapshot.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/contract/ExecutionDegradation.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/contract/ExecutionVersions.java`

- [ ] **Step 1: 定义执行模式、请求元数据、运行时选项**

```java
public enum AnalysisMode {
    INITIAL, FOLLOW_UP, LEGACY_COMPAT, REPLAY
}
```

- [ ] **Step 2: 定义统一执行结果信封**
- [ ] **Step 3: 定义 deterministic / retrieval / llm / presentation / degradation / versions / metrics 子对象**
- [ ] **Step 4: 保持 DTO 仅承载契约，不混入业务逻辑**

### Task 3: 实现 `AnalysisExecutionService` 第一版编排骨架

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/runtime/AnalysisExecutionService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/runtime/DeterministicFactSnapshotBuilder.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/DivinationService.java`

- [ ] **Step 1: 先抽出“确定性事实构建”逻辑**

```java
DeterministicFactSnapshot snapshot = factSnapshotBuilder.build(
    chart, ruleHits, structuredResult, analysisContext
);
```

- [ ] **Step 2: 在 `SessionService#createSession` 中改为调用 `AnalysisExecutionService`**
- [ ] **Step 3: 在 `SessionService#addMessage` 中改为调用统一执行服务**
- [ ] **Step 4: 在 `DivinationService#analyze` 中通过适配器走 `LEGACY_COMPAT`**
- [ ] **Step 5: 先让 envelope 可用，再保留旧响应 DTO 映射**

### Task 4: 运行统一执行契约相关测试

**Files:**
- Test only

- [ ] **Step 1: 运行统一执行和 Session 测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=AnalysisExecutionServiceTest,SessionApiIntegrationTest test
```

Expected:

- execution envelope 测试通过
- Session 行为不回归

- [ ] **Step 2: 运行 Prompt regression**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=PromptRegressionTest -Dspring.profiles.active=prompt-test test
```

Expected:

- 结构化分析仍通过黄金数据集

### Task 5: 提交本 Chunk

**Files:**
- Stage only `analysis.contract`, `analysis.runtime` 第一版、`SessionService`、`DivinationService`、相关测试

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: unify analysis execution contract"
```

---

## Chunk 2: 证据对象化与引用治理

### Task 6: 为 evidence 对象与引用映射写失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/evidence/service/EvidenceRetrievalServiceTest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/evidence/service/CitationValidationServiceTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/session/controller/SessionApiIntegrationTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/PromptRegressionTest.java`

- [ ] **Step 1: 写“检索结果返回 EvidenceHit 而不是纯字符串”的失败测试**
- [ ] **Step 2: 写“classicReferences 必须能映射到本次 evidence hits”的失败测试**
- [ ] **Step 3: 写“追问检索必须真正带入 followUpQuestion”的失败测试**
- [ ] **Step 4: 运行测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=EvidenceRetrievalServiceTest,CitationValidationServiceTest,SessionApiIntegrationTest,PromptRegressionTest test
```

Expected:

- 当前 `KnowledgeSearchService` 只返回 `List<String>`
- 当前追问路径未真正消费 follow-up 语义

### Task 7: 新增 evidence 平台 DTO 与检索服务

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/dto/EvidenceQuery.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/dto/EvidenceHit.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/dto/EvidenceCitation.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/dto/EvidenceSelectionResult.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/service/EvidenceRetrievalService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evidence/service/CitationValidationService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`

- [ ] **Step 1: 把 `KnowledgeSearchService` 的检索能力下沉为 `EvidenceRetrievalService` 可复用方法**
- [ ] **Step 2: 给 `EvidenceHit` 补足 `chunkId/bookId/sourceTitle/chapterTitle/knowledgeType/content/score/rank`**
- [ ] **Step 3: 为旧 Prompt 组装保留 `toPromptSnippets()` 适配方法**
- [ ] **Step 4: 让 `SessionService` 和 execution runtime 消费 `EvidenceSelectionResult`**

### Task 8: 扩展 `AnalysisOutputDTO` 与 Prompt 约束支持结构化引用

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/AnalysisOutputDTO.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/prompts/v1/system/orchestrated_analyst.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/resources/golden-dataset/schema/analysis_output_schema.json`

- [ ] **Step 1: 为 `ClassicReference` 增加可选 `chunkId` / `bookId` / `citationId`**
- [ ] **Step 2: Prompt 中要求引用只允许来自当前 evidence hits**
- [ ] **Step 3: JSON schema 允许结构化来源字段**

### Task 9: 运行 evidence 与 regression 测试

**Files:**
- Test only

- [ ] **Step 1: 运行 evidence 单测与 Session 集成测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=EvidenceRetrievalServiceTest,CitationValidationServiceTest,SessionApiIntegrationTest test
```

Expected:

- Session 响应中的引用可映射到真实 evidence hits

- [ ] **Step 2: 运行 Prompt regression**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=PromptRegressionTest -Dspring.profiles.active=prompt-test test
```

Expected:

- Schema 与核心断言仍通过

### Task 10: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: introduce evidence objects and citation validation"
```

---

## Chunk 3: 语义验证、降级分型与执行审计

### Task 11: 为 validation pipeline 与 degradation 写失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/validation/AnalysisValidationPipelineTest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/validation/SafetyPolicyValidatorTest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/ops/audit/ExecutionAuditServiceTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/LlmClientTest.java`

- [ ] **Step 1: 写“JSON 合法但结论方向与 structuredResult 冲突”的失败测试**
- [ ] **Step 2: 写“引用字段与 evidence hits 不匹配”的失败测试**
- [ ] **Step 3: 写“健康/官司问题必须命中安全策略”的失败测试**
- [ ] **Step 4: 写“降级原因必须结构化进入 envelope”的失败测试**
- [ ] **Step 5: 运行测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=AnalysisValidationPipelineTest,SafetyPolicyValidatorTest,ExecutionAuditServiceTest,LlmClientTest test
```

Expected:

- 当前只有 JSON 结构回退，没有完整语义分型和审计台账

### Task 12: 新增 validation pipeline 与 degradation resolver

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/validation/AnalysisValidationPipeline.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/validation/SchemaValidationStage.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/validation/SemanticAlignmentStage.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/validation/CitationValidationStage.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/validation/SafetyPolicyStage.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/runtime/DegradationResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/runtime/AnalysisExecutionService.java`

- [ ] **Step 1: 先在 runtime 中注入 pipeline**
- [ ] **Step 2: 为每类失败定义 issue code**
- [ ] **Step 3: 让 envelope 产出 `degradation.level` 与 `degradation.reasons`**
- [ ] **Step 4: 对安全拒答返回稳定结构**

### Task 13: 新增执行审计持久化

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/db/migration/V21__create_analysis_run_tables.sql`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/domain/AnalysisRun.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/domain/AnalysisRunIssue.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/domain/AnalysisRunCitation.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/repository/AnalysisRunRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/repository/AnalysisRunIssueRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/repository/AnalysisRunCitationRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/audit/service/ExecutionAuditService.java`

- [ ] **Step 1: 新建 `analysis_run` / `analysis_run_issue` / `analysis_run_citation`**
- [ ] **Step 2: 落 execution id、版本信息、degradation、metrics**
- [ ] **Step 3: 审计落库失败不得中断主流程**

### Task 14: 运行 validation、audit 与主流程测试

**Files:**
- Test only

- [ ] **Step 1: 运行 validation / llm / session 测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=AnalysisValidationPipelineTest,SafetyPolicyValidatorTest,LlmClientTest,SessionApiIntegrationTest test
```

Expected:

- 非法语义输出被分型并降级

- [ ] **Step 2: 运行 Flyway 与审计相关集成测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=ExecutionAuditServiceTest test
```

Expected:

- 审计表可落地，执行摘要可查询

### Task 15: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: add analysis validation and execution audit"
```

---

## Chunk 4: 运行时平台与 legacy 收口

### Task 16: 为持久化限流与租约调度写失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/ops/ratelimit/PersistentRateLimiterTest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/ops/job/JobLeaseServiceTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/infrastructure/schedule/ScheduledJobsTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/session/controller/SessionApiIntegrationTest.java`

- [ ] **Step 1: 写“多实例限流共享计数”的失败测试**
- [ ] **Step 2: 写“定时任务必须先获取租约”的失败测试**
- [ ] **Step 3: 写“legacy analyze 不再依赖旧 AnalysisService 主逻辑”的失败测试**
- [ ] **Step 4: 运行测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=PersistentRateLimiterTest,JobLeaseServiceTest,ScheduledJobsTest,SessionApiIntegrationTest test
```

Expected:

- 当前限流仍为内存实现
- 当前定时任务无租约保护
- legacy analyze 仍保留旧链路依赖

### Task 17: 新增持久化限流和租约调度实现

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/db/migration/V22__create_ops_platform_tables.sql`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/ratelimit/domain/RateLimitBucket.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/ratelimit/repository/RateLimitBucketRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/ratelimit/PersistentRateLimiter.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/job/domain/JobLease.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/job/repository/JobLeaseRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/ops/job/JobLeaseService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/schedule/ScheduledJobs.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionService.java`

- [ ] **Step 1: 用 `PersistentRateLimiter` 替换内存 `RateLimiter` 调用点**
- [ ] **Step 2: 在 `ScheduledJobs` 每个入口前先尝试获取租约**
- [ ] **Step 3: 对失败获取租约的场景只记录 debug/info，不报错**

### Task 18: legacy 收口与旧分析链降级为兼容组件

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/DivinationService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/presentation/PresentationCompatibilityAdapter.java`

- [ ] **Step 1: 将 `AnalysisService` 明确降级为兼容 fallback 组件**
- [ ] **Step 2: 所有 legacy analyze 文本由 `PresentationCompatibilityAdapter` 从 execution envelope 生成**
- [ ] **Step 3: 移除 `DivinationService` 对旧主链路的核心依赖**

### Task 19: 运行运行时与兼容路径测试

**Files:**
- Test only

- [ ] **Step 1: 运行 ops、schedule、session 测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=PersistentRateLimiterTest,JobLeaseServiceTest,ScheduledJobsTest,SessionApiIntegrationTest test
```

Expected:

- 多实例限流和租约逻辑测试通过

- [ ] **Step 2: 跑一次后端全量测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn test
```

Expected:

- 所有后端测试通过

### Task 20: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: add persistent ops platform and legacy compatibility adapter"
```

---

## Chunk 5: 统一评估平台

### Task 21: 为 evaluation 平台写失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/evaluation/EvaluationRunServiceTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/PromptRegressionTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/casecenter/service/CaseCenterServiceIntegrationTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/knowledge/controller/KnowledgeImportExecutionTest.java`

- [ ] **Step 1: 写“Prompt regression 可输出统一评估摘要”的失败测试**
- [ ] **Step 2: 写“replay 可转成 evaluation scenario”的失败测试**
- [ ] **Step 3: 写“RAG 召回测试可输出有效引用率”的失败测试**
- [ ] **Step 4: 运行测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=EvaluationRunServiceTest,PromptRegressionTest,CaseCenterServiceIntegrationTest,KnowledgeImportExecutionTest test
```

Expected:

- 当前没有统一 `evaluation` 平台和评估摘要对象

### Task 22: 新增 evaluation 平台骨架

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/db/migration/V23__create_evaluation_run_table.sql`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/domain/EvaluationRun.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/repository/EvaluationRunRepository.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/dto/EvaluationScenario.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/dto/EvaluationScoreCard.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/service/EvaluationDatasetRegistry.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/evaluation/service/EvaluationRunService.java`

- [ ] **Step 1: 统一 prompt / replay / rag 三类 scenario 入口**
- [ ] **Step 2: 统一评估报告对象**
- [ ] **Step 3: 先支持离线评估，不急于暴露对外 API**

### Task 23: 将现有 regression / replay 接入 evaluation 平台

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/PromptRegressionTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/CaseCenterService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`

- [ ] **Step 1: Prompt regression 产出评估摘要**
- [ ] **Step 2: replayCase 可导出 `EvaluationScenario`**
- [ ] **Step 3: RAG 检索评估至少记录 hit count、selected citation rate、citation mismatch rate**

### Task 24: 运行全链路质量测试

**Files:**
- Test only

- [ ] **Step 1: 运行评估相关专项测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=EvaluationRunServiceTest,PromptRegressionTest,CaseCenterServiceIntegrationTest,KnowledgeImportExecutionTest test
```

Expected:

- 统一评估平台可落库与生成报告

- [ ] **Step 2: 全量运行后端测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn test
```

Expected:

- 后端测试全绿

### Task 25: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: add unified evaluation platform"
```

---

## 收尾任务

### Task 26: 更新文档与 CI

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/README.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/docs/api.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/.github/workflows/ci.yml`
- Modify: `/Users/liuyishou/wordspace/liuyao/docs/六爻AI断卦系统设计文档.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/docs/六爻AI断卦系统基础设施与质量保障设计文档.md`

- [ ] **Step 1: 更新 README 中的架构与启动说明**
- [ ] **Step 2: 更新 API 文档中的统一 execution / degradation / citation 字段**
- [ ] **Step 3: 在 CI 中按改动范围补充 evaluation 触发逻辑**

### Task 27: 最终验证

**Files:**
- Test only

- [ ] **Step 1: 后端全量测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn test
```

- [ ] **Step 2: 前端构建**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-h5 && npm run build
```

- [ ] **Step 3: worker 测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-worker && pytest
```

- [ ] **Step 4: docker compose 配置验证**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao && docker compose config
```

Expected:

- 全仓关键验证通过

### Task 28: 最终提交

- [ ] **Step 1: `git add` 文档、代码、迁移、测试、CI 修改**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: establish platform runtime foundation for liuyao"
```

---

Plan complete and saved to `docs/superpowers/plans/2026-04-13-platform-runtime-refactor.md`. Ready to execute?
