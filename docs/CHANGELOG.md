# CHANGELOG

## v2.0.0

### Added

- 新增 Session 会话层：支持创建会话、追问、关闭、历史列表和详情查询
- 新增编排式分析链路：`LlmClient`、`PromptTemplateEngine`、`ContextWindowBuilder`、`OrchestratedAnalysisService`
- 新增应验日历模块：应验事件、反馈、月视图、时间线、提醒和过期处理
- 新增 H5 多轮对话页、历史会话页、应验日历页
- 新增 PostgreSQL Testcontainers 集成测试基线
- 新增 Dockerfile、Nginx 配置、统一 `docker-compose.yml`、GitHub Actions CI 工作流
- 新增 prompt regression 黄金数据集与 `prompt-test` profile 回归入口

### Changed

- 旧接口 `POST /api/divinations/analyze` 代理到新的 Session 创建流程，同时保持返回字段兼容
- Mock LLM 在 `test` profile 下改为默认主实现，避免测试误走真实 LLM
- Context Window 增加 Token 预算裁剪，最近 5 轮完整保留，超出部分压缩为摘要
- Verification 事件从结构化分析结果里的 `predictedTimeline` 自动派生
- Compose 默认 PostgreSQL 外部端口调整为 `5433`，降低本机 `5432` 冲突概率
- README 更新为 v2.0 架构说明、启动方式和 prompt regression 命令

### Fixed

- 修复 Session API 与 Case Center 之间的基线分析落库不一致问题
- 修复 PostgreSQL 迁移链路在无 `pgvector` 扩展环境下的兼容问题
- 修复 `BusinessException` 到 HTTP 状态码的映射，已关闭会话追问返回 `409`
- 修复 Python worker 的缩进错误，恢复容器内正常启动
- 修复 prompt regression 在 `prompt-test` profile 下无法复用 Mock LLM 的问题

### Deprecated

- `AnalysisService` 标记为旧版单轮分析入口，后续由 `OrchestratedAnalysisService` 取代
- `LlmExpressionClient` 标记为旧版表达层客户端，后续由新 `LlmClient` 体系取代

## v1.0.0

### Added

- 六爻起卦分析最小闭环
- 规则引擎与案例留痕
- 基础 API、测试和项目骨架
