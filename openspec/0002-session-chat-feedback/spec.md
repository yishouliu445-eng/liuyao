# Liuyao v2.0 Session-Based Chat & Feedback Loop Spec

## Status

Draft

## Summary

将六爻系统从"单次排盘工具"升级为"多轮对话 + 应验反馈闭环平台"。核心改造包括：编排式LLM分析引擎替代简单润色、Session多轮对话、应验日历与反馈收集。同时补齐基础设施：容器化部署、测试策略、Prompt版本管理、成本控制。

## Context

0001 已完成 Spring Boot 骨架、排盘引擎、规则引擎、RAG知识检索、LLM润色层和H5前端。当前系统是一个完整的单次交互管线，但存在以下瓶颈：

- LLM 仅做文字润色（`LlmExpressionClient`），不做深度分析
- 无会话概念，用户无法追问
- 无数据反馈闭环，无法度量AI准确率
- 无容器化部署、无CI、无Prompt版本管理

详细需求与设计见：
- `docs/六爻AI断卦系统产品需求文档.md`
- `docs/六爻AI断卦系统设计文档.md`
- `docs/六爻AI断卦系统基础设施与质量保障设计文档.md`

## Goals

1. 用户可以围绕同一卦象进行多轮追问对话，系统保持上下文记忆
2. LLM分析从"润色"升级为"编排式深度解析"，输出结构化JSON
3. 建立应验日历和反馈收集机制，形成数据飞轮
4. 全服务容器化，建立CI流水线
5. Prompt模板外置、可版本化、可回归测试

## Non-Goals

1. 不实现真正的Multi-Agent框架（LangGraph等），用编排式Prompt模拟
2. 不实现TTS语音（P2阶段再做）
3. 不实现跨设备日历同步（先做应用内日历）
4. 不实现SSE流式输出（后续增强）
5. 不做MCP服务化（架构预留但不实现）

## Architecture Decisions

### AD-001 编排式Prompt替代Multi-Agent

单次LLM调用中通过System Prompt分区模拟三个视角（易理推演/古籍佐证/整合输出），而非调用三个独立Agent。省去多次API调用的延迟和成本。保留升级为真Multi-Agent的架构空间。

### AD-002 Session作为新的顶层编排入口

新增 `SessionService` 替代 `DivinationService` 作为核心编排入口。`DivinationService` 保留向后兼容，内部代理到 `SessionService`。

### AD-003 强制JSON Schema输出

LLM调用必须设置 `response_format: {"type": "json_object"}`，输出通过Schema校验后再使用。解析失败则降级为 `AnalysisSectionComposer` 的机械文本。

### AD-004 Token预算制

ContextWindowBuilder 为上下文的6个区域（System/排盘/规则/对话历史/知识/输出预留）各分配Token预算，总计不超过9000 tokens，兼容8K context模型。

### AD-005 Prompt模板外置

Prompt从Java硬编码抽出为 `src/main/resources/prompts/v1/*.md` 模板文件，支持版本切换和A/B测试。

### AD-006 保留降级路径

`AnalysisSectionComposer` 保留为降级方案。LLM不可用时，系统仍能基于规则引擎输出结构化分析文本。

## New Module Boundaries

### `session` (NEW)

职责：
- Session生命周期管理（创建/追问/关闭/超时清理）
- 消息持久化
- 上下文构建与Token裁剪

核心类：
- `SessionController`
- `SessionService`
- `SessionPersistenceService`
- `ContextWindowBuilder`

### `analysis` (REFACTOR)

变更：
- 新增 `OrchestratedAnalysisService` 替代 `AnalysisService`
- 新增 `PromptTemplateEngine`
- 重构 `LlmExpressionClient` → `LlmClient`
- `AnalysisSectionComposer` 保留为降级

### `calendar` (NEW)

职责：
- 应验事件管理
- 反馈收集
- 日历/时间线视图API
- 定时提醒与过期处理

核心类：
- `CalendarController`
- `VerificationEventService`

## Database Changes

新增4张表：`chat_session`, `chat_message`, `verification_event`, `verification_feedback`
变更2张表：`divination_case` 加 `session_id`, `case_analysis_result` 加 `structured_payload_json`
新增全文索引：`book_chunk` 加 `content_tsv` tsvector列

详见设计文档 §二。

## Delivery Phases

### Phase 0: Infrastructure (1周)

容器化、CI、Mock LLM、分支策略、Prompt模板目录

### Phase 1: Chat Core (4周)

Session数据模型 → LLM重构 → 多轮追问 → 前端对话UI

### Phase 2: Feedback Loop (3周)

应验事件 → 日历视图 → 反馈表单 → 定时推送

### Phase 3: Polish & RAG Upgrade (2周)

新中式设计 → 动效 → RAG管线升级（卦例分块+元数据提取+全文索引）

## Risks

1. LLM输出不遵守JSON Schema → 强制`response_format` + 校验 + 重试 + 降级
2. 多轮对话Token溢出 → 严格Token预算 + 滑动窗口 + 摘要压缩
3. 用户反馈填写率低 → 轻量化表单（选择题为主）+ 推送提醒
4. LLM API成本超预期 → 追问用小模型 + 用户限流 + 成本监控告警

## Acceptance

- 用户可以起卦后围绕同一卦象追问至少10轮，AI不丢失上下文
- AI输出通过JSON Schema校验，无极端宿命论断语
- 应验事件自动创建，反馈可提交
- `docker-compose up` 一键启动全部服务
- Prompt变更后可运行回归测试验证质量
