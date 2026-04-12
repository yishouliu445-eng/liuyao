# Liuyao v2.0 Session-Based Chat & Feedback Loop Task Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将系统从单次排盘工具升级为多轮对话+应验反馈平台。按"基础设施→后端核心→前端跟进→应验闭环→体验打磨"的顺序推进。

**Architecture:** 新增 Session 会话层 + 编排式LLM分析引擎 + 应验日历模块。保留现有排盘引擎和规则引擎不变，旧API向后兼容。

**Tech Stack:** Java 21, Spring Boot, Maven, PostgreSQL + pgvector, Flyway, React + TypeScript + Vite, Python 3.12, Docker, GitHub Actions

---

## Chunk 1: Infrastructure Setup

### Task 1: 建立分支与环境基础

**Files:**
- Create: `docker-compose.yml` (根目录)
- Create: `.env.example`
- Create: `.gitignore` (追加 `.env`)

- [x] 创建 git 分支 `feature/v2-session`
- [x] 创建根目录 `docker-compose.yml`（pgvector镜像、app、worker、h5）
- [x] 创建 `.env.example`（DB_PASSWORD, LLM_API_KEY, DASHSCOPE_API_KEY 等）
- [x] 更新 `.gitignore` 排除 `.env`
- [x] 验证 `docker-compose up postgres` 可正常启动

### Task 2: 建立 Mock LLM 与测试环境

**Files:**
- Create: `liuyao-app/src/main/resources/application-test.yml`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/MockLlmClient.java`

- [x] 创建 `application-test.yml`（LLM走Mock、测试数据库配置）
- [x] 创建 `MockLlmClient`（`@Profile("test")`，返回固定结构化JSON）
- [x] 验证 `mvn test` 使用 Mock 而非真实 LLM
- [x] 现有测试全部通过

### Task 3: 创建 Prompt 模板目录与初版模板

**Files:**
- Create: `liuyao-app/src/main/resources/prompts/v1/system/orchestrated_analyst.md`
- Create: `liuyao-app/src/main/resources/prompts/v1/context/chart_context.md`
- Create: `liuyao-app/src/main/resources/prompts/v1/context/rule_context.md`
- Create: `liuyao-app/src/main/resources/prompts/v1/context/knowledge_context.md`
- Create: `liuyao-app/src/main/resources/prompts/v1/user/initial_analysis.md`
- Create: `liuyao-app/src/main/resources/prompts/v1/user/follow_up.md`
- Create: `liuyao-app/src/main/resources/prompts/manifest.yml`

- [x] 根据设计文档 §3.3 编写 System Prompt 初版
- [x] 编写排盘数据/规则命中/知识片段的上下文注入模板
- [x] 编写首次分析和追问的 User Prompt 模板
- [x] 创建 manifest.yml 版本清单
- [x] 验证模板文件可被 Spring 资源加载器读取

---

## Chunk 2: Database & Data Model

### Task 4: Session 与消息表

**Files:**
- Create: `liuyao-app/src/main/resources/db/migration/V14__create_chat_session_and_message.sql`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/domain/ChatSession.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/domain/ChatMessage.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/repository/ChatSessionRepository.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/repository/ChatMessageRepository.java`

- [x] 编写 V14 migration（chat_session + chat_message 建表 + 索引）
- [x] 创建 JPA Entity: `ChatSession`, `ChatMessage`
- [x] 创建 Repository 接口
- [x] 本地执行 Flyway migration 验证无报错
- [x] 编写 Repository 层简单查询测试

### Task 5: 应验事件与反馈表

**Files:**
- Create: `liuyao-app/src/main/resources/db/migration/V15__create_verification_tables.sql`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/domain/VerificationEvent.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/domain/VerificationFeedback.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/repository/VerificationEventRepository.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/repository/VerificationFeedbackRepository.java`

- [x] 编写 V15 migration（verification_event + verification_feedback）
- [x] 创建 JPA Entity
- [x] 创建 Repository 接口
- [x] 验证 migration 执行成功

### Task 6: 现有表变更

**Files:**
- Create: `liuyao-app/src/main/resources/db/migration/V16__link_case_to_session.sql`
- Create: `liuyao-app/src/main/resources/db/migration/V17__add_structured_payload.sql`

- [x] V16: `divination_case` 新增 `session_id` 列
- [x] V17: `case_analysis_result` 新增 `structured_payload_json` 列
- [/] 验证 migration 不影响现有数据

---

## Chunk 3: LLM Client & Analysis Engine Refactor

### Task 7: 重构 LlmClient

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/LlmClient.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/LlmResponse.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/LlmRequestOptions.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/ChatMessage.java`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/LlmClientTest.java`

- [x] 创建新 `LlmClient`（支持 messages 数组、强制 JSON、Token 统计）
- [x] 创建 `LlmResponse` record（含 parsedJson, tokenCount, latencyMs）
- [x] 编写测试：正常 JSON 解析、非法 JSON 降级、超时处理
- [ ] 旧 `LlmExpressionClient` 暂不删除，待 Task 9 完成后废弃

### Task 8: 实现 PromptTemplateEngine 与 ContextWindowBuilder

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/PromptTemplateEngine.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/ContextWindowBuilder.java`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/ContextWindowBuilderTest.java`

- [x] `PromptTemplateEngine`: 加载模板文件、变量注入、版本切换
- [x] `ContextWindowBuilder`: Token预算分配、排盘JSON精简、规则摘要精简、对话历史裁剪
- [x] 测试：5轮内完整保留、6轮起压缩摘要、总Token不超限
- [x] 测试：排盘JSON精简后关键字段保留、ext去除

### Task 9: 实现 OrchestratedAnalysisService

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/OrchestratedAnalysisService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/AnalysisOutputDTO.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/AnalysisInputDTO.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/FollowUpInputDTO.java`

- [x] 实现 `analyzeInitial()`：首次分析流程
- [x] 实现 `analyzeFollowUp()`：追问分析流程
- [x] JSON Schema 校验逻辑 + 重试1次 + 降级到 AnalysisSectionComposer
- [x] 集成 PromptTemplateEngine 和 ContextWindowBuilder
- [x] 用 MockLlmClient 验证端到端流程

---

## Chunk 4: Session Service & API

### Task 10: 实现 SessionService

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/service/SessionPersistenceService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateRequest.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/SessionCreateResponse.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/MessageRequest.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/dto/MessageResponse.java`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/session/service/SessionServiceTest.java`

- [x] `createSession()`: 排盘→规则→RAG→LLM→持久化→返回
- [x] `addMessage()`: 加载Session→构建上下文→LLM→持久化→返回
- [x] `getSession()`: 获取Session详情+消息历史
- [x] `listSessions()`: 分页查询用户Session列表
- [x] `closeSession()`: 手动关闭
- [x] 测试：创建→追问3轮→关闭；超时自动关闭；消息上限拒绝

### Task 11: 实现 SessionController

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/session/controller/SessionController.java`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/session/controller/SessionApiIntegrationTest.java`

- [x] `POST /api/sessions` → createSession
- [x] `POST /api/sessions/{id}/messages` → addMessage
- [x] `GET /api/sessions/{id}` → getSession
- [x] `GET /api/sessions` → listSessions
- [x] `DELETE /api/sessions/{id}` → closeSession
- [x] 集成测试：起卦→追问→关闭Session的完整API链路
- [x] 集成测试：已关闭Session追问返回409

### Task 12: 向后兼容旧API

**Files:**
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/service/DivinationService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/controller/DivinationController.java`

- [x] `POST /api/divinations/analyze` 内部代理到 SessionService.createSession()
- [x] 返回格式保持向后兼容（旧字段不丢失）
- [x] Feature Flag: `session-api-enabled` 控制是否启用新路径
- [x] 验证旧API仍可正常调用

---

## Chunk 5: Frontend Chat UI

### Task 13: 对话页面核心

**Files:**
- Create: `liuyao-h5/src/pages/SessionPage.tsx`
- Create: `liuyao-h5/src/components/chat/ChatBubble.tsx`
- Create: `liuyao-h5/src/components/chat/SmartPromptBar.tsx`
- Create: `liuyao-h5/src/components/chat/ChatInput.tsx`
- Create: `liuyao-h5/src/components/chart/CollapsibleChart.tsx`
- Create: `liuyao-h5/src/api/sessions.ts`
- Modify: `liuyao-h5/src/App.tsx` (新增路由)

- [x] 创建对话页面布局（排盘卡片+消息列表+输入框+SmartPrompt）
- [x] 实现 ChatBubble（用户/AI消息气泡，AI气泡支持结构化渲染）
- [x] 实现 SmartPromptBar（3个追问建议气泡，点击自动发送）
- [x] 实现 ChatInput（输入框+发送按钮+loading状态）
- [x] 实现 CollapsibleChart（可折叠排盘卡片）
- [x] 新增路由 `/session/:id`
- [x] 对接 Session API

### Task 14: 历史会话列表

**Files:**
- Create: `liuyao-h5/src/pages/HistoryPage.tsx`
- Create: `liuyao-h5/src/components/history/SessionCard.tsx`
- Modify: `liuyao-h5/src/App.tsx` (新增路由)

- [x] 创建历史会话列表页面
- [x] Session卡片（问题摘要+类别+消息数+最后活跃时间）
- [x] 点击进入对话页面继续追问
- [x] 新增路由 `/history`
- [x] 底部导航增加"历史"入口

### Task 15: 改造首页起卦流程

**Files:**
- Modify: `liuyao-h5/src/pages/HomePage.tsx`
- Modify: `liuyao-h5/src/components/input/QuestionForm.tsx`

- [x] 起卦成功后跳转到 `/session/:id` 而非停留在首页展示结果
- [x] QuestionForm 调用 `POST /api/sessions` 替代旧 API
- [x] 保留加载动画（LoadingInk）

---

## Chunk 6: Verification Calendar

### Task 16: 应验事件服务

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/service/VerificationEventService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/dto/*.java`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/calendar/service/VerificationEventServiceTest.java`

- [x] 应期文本解析（"下个月中旬"→日期+精度）
- [x] 从 LLM 输出的 `predictedTimeline` 自动创建事件
- [x] 反馈提交
- [x] 按月查询事件列表
- [x] 时间线分页查询
- [x] 测试：应期解析各种模式、反馈提交、过期处理

### Task 17: 日历API

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/calendar/controller/CalendarController.java`

- [x] `GET /api/calendar/events` — 按月查询
- [x] `POST /api/calendar/events/{id}/feedback` — 提交反馈
- [x] `GET /api/calendar/timeline` — 时间线

### Task 18: 定时任务

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/schedule/ScheduledJobs.java`

- [x] 每小时：关闭超24h无交互的Session
- [x] 每天9点：发送应验提醒
- [x] 每天凌晨：标记过期事件

### Task 19: 日历前端

**Files:**
- Create: `liuyao-h5/src/pages/CalendarPage.tsx`
- Create: `liuyao-h5/src/components/calendar/MonthView.tsx`
- Create: `liuyao-h5/src/components/calendar/TimelineView.tsx`
- Create: `liuyao-h5/src/components/calendar/FeedbackForm.tsx`
- Create: `liuyao-h5/src/api/calendar.ts`

- [x] 月视图（日期上标注应验事件）
- [x] 时间线视图（占卜历史+应验状态）
- [x] 反馈表单（选择题+标签+可选文字）
- [x] 新增路由 `/calendar`

---

## Chunk 7: Quality & DevOps

### Task 20: 容器化

**Files:**
- Create: `liuyao-app/Dockerfile`
- Create: `liuyao-worker/Dockerfile`
- Create: `liuyao-h5/Dockerfile`
- Create: `liuyao-h5/nginx.conf`
- Modify: `docker-compose.yml`

- [x] Java后端 Dockerfile（多阶段构建）
- [x] Python Worker Dockerfile
- [x] H5前端 Dockerfile（build+Nginx）
- [x] Nginx 配置（SPA路由+API反代）
- [x] 统一 docker-compose.yml（PG+App+Worker+H5）
- [x] 验证 `docker-compose up` 全部服务正常启动

### Task 21: CI Pipeline

**Files:**
- Create: `.github/workflows/ci.yml`

- [x] backend-test job（Java测试+PG服务）
- [x] worker-test job（pytest）
- [x] frontend-build job（npm ci + build）
- [x] prompt-regression job（仅prompts/变更时触发）

### Task 22: Prompt回归测试框架

**Files:**
- Create: `liuyao-app/src/test/resources/golden-dataset/cases/*.json`
- Create: `liuyao-app/src/test/resources/golden-dataset/schema/analysis_output_schema.json`
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/analysis/PromptRegressionTest.java`

- [ ] 编写10个黄金数据集Case（覆盖事业/感情/健康/财运/边界场景）
- [ ] 编写JSON Schema定义
- [ ] 实现 PromptRegressionTest（参数化测试+7项断言）
- [ ] 验证在 prompt-test profile 下可运行

### Task 23: 成本控制

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/ratelimit/RateLimiter.java`
- Modify: `liuyao-app/src/main/resources/application.yml`

- [ ] 实现用户请求限流（匿名5次/天，登录20次/天）
- [ ] LlmClient 记录每次调用的 Token 用量
- [ ] 配置日成本告警阈值
- [ ] 追问支持切换为更小模型（配置化）

---

## Chunk 8: RAG Pipeline Upgrade

### Task 24: Worker 卦例分块器

**Files:**
- Create: `liuyao-worker/app/chunker/case_example_chunker.py`
- Create: `liuyao-worker/tests/test_case_example_chunker.py`

- [ ] 实现卦例起始/结束模式识别
- [ ] 保持完整卦例不被切碎
- [ ] 标记 `content_type = "case_example"`
- [ ] 用增删卜易中的已知卦例编写测试

### Task 25: LLM元数据提取

**Files:**
- Create: `liuyao-worker/app/pipeline/metadata_enrichment.py`
- Create: `liuyao-worker/tests/test_metadata_enrichment.py`

- [ ] 实现 MetadataEnrichmentStage
- [ ] LLM提取: topic_tags, knowledge_type, scenario_types, applicable_hexagrams 等
- [ ] 失败不中断主流程
- [ ] Mock LLM 测试

### Task 26: 全文索引与混合检索

**Files:**
- Create: `liuyao-app/src/main/resources/db/migration/V18__add_fulltext_search.sql`
- Create: `liuyao-app/src/main/resources/db/migration/V19__enhance_chunk_metadata.sql`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`

- [ ] V18: book_chunk 新增 content_tsv 列 + GIN索引 + 触发器
- [ ] V19: book_chunk 新增 knowledge_type, has_timing_prediction 等列
- [ ] KnowledgeSearchService 新增 `hybridSearch()` 方法
- [ ] 实现 RRF (Reciprocal Rank Fusion) 合并排序
- [ ] 实现拒识阈值过滤 (similarity < 0.65 → 丢弃)

---

## Chunk 9: Verification & Wrap-up

### Task 27: 端到端验证

- [x] 起卦→首次分析→追问5轮→AI不丢失上下文
- [x] 负面结论包含≥2条行动建议
- [x] 应验事件自动创建→反馈可提交
- [x] 旧API `/api/divinations/analyze` 仍正常工作
- [x] `docker-compose up` 一键启动全部服务
- [x] 所有现有测试 + 新增测试通过

### Task 28: 收尾

**Files:**
- Modify: `README.md`
- Create: `docs/CHANGELOG.md`

- [ ] 更新 README（新架构说明、启动方式、API文档链接）
- [ ] 记录 v1.0→v2.0 的完整变更日志
- [ ] 废弃标记：`AnalysisService`, `LlmExpressionClient`
- [ ] 合并分支到 main
