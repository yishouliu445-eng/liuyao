# Liuyao Spring Boot Skeleton Spec

## Status

Draft

## Summary

基于现有设计文档，生成一个可持续演进的 Java Spring Boot 主服务骨架，统一包名为 `com.yishou.liuyao`。第一阶段优先完成完整骨架、核心数据契约、数据库映射边界与规则引擎接入点；在骨架验证通过后，再补最小业务闭环。

## Context

当前仓库包含以下输入材料：

- `liuyao_springboot_project_design.md`
- `liuyao_core_code_design.md`
- `liuyao_database_sql_design.md`
- `liuyao_engineering_principles.md`
- `liuyao_usegod_pack/`

当前仓库尚未存在可直接运行的 Spring Boot 工程，因此本次工作首先是“依据文档生成工程骨架”，而不是在既有工程上增量修改。

## Goals

1. 生成一个可启动、结构完整、与设计文档一致的 Spring Boot 工程。
2. 将核心领域对象、模块边界、分层方式固定下来，减少后续返工。
3. 将 `ChartSnapshot` 设为排盘、规则、分析、留痕的统一数据契约。
4. 将 `liuyao_usegod_pack` 融入正式工程，统一到 `com.yishou.liuyao` 包路径。
5. 为第二阶段最小闭环预留清晰入口：请求、排盘、规则、持久化、响应。

## Non-Goals

1. 不在第一阶段实现完整排盘算法、历法换算或真实六爻推演逻辑。
2. 不接入真实 LLM、Embedding、RAG 检索能力。
3. 不完成书籍解析、向量检索、异步 worker 的完整业务实现。
4. 不引入 DSL、脚本化规则或复杂插件系统。
5. 不做多服务拆分，先保持单体主服务。

## Project Layout

工程名称暂定为 `liuyao-app`，推荐结构如下：

```text
liuyao-app/
  ├── pom.xml
  ├── src/main/java/com/yishou/liuyao
  │   ├── LiuyaoApplication.java
  │   ├── common/
  │   ├── infrastructure/
  │   ├── auth/
  │   ├── book/
  │   ├── knowledge/
  │   ├── divination/
  │   ├── rule/
  │   ├── analysis/
  │   ├── casecenter/
  │   └── task/
  ├── src/main/resources/
  │   ├── application.yml
  │   ├── db/migration/
  │   └── rules/use_god_rules.json
  └── src/test/java/com/yishou/liuyao
```

## Architecture Decisions

### AD-001 单体 Spring Boot 优先

第一阶段使用单体工程承载所有主服务能力，降低初始化成本和跨模块协作复杂度。

### AD-002 统一分层

每个业务模块统一使用以下分层：

- `controller`
- `service`
- `domain`
- `repository`
- `dto`
- `mapper`

### AD-003 `ChartSnapshot` 为唯一真相

规则引擎、AI 编排、卦例留痕、回放能力均依赖 `ChartSnapshot`，避免各模块建立私有盘面结构。

### AD-004 规则引擎先代码化

第一阶段规则仍以 Java 类为主，配置只用于轻量映射。`usegod` 规则包沿用“Java 规则 + JSON 配置”的方式。

### AD-005 系统必须可降级

即使分析模块、知识模块尚未接入真实能力，系统也应能返回基础排盘结构、规则命中与留痕结果。

## Module Boundaries

### `common`

职责：

- 统一响应结构
- 基础异常体系
- 错误码定义
- 通用分页对象
- 基础实体父类

计划包含：

- `ApiResponse`
- `BusinessException`
- `ErrorCode`
- `PageResult`
- `BaseEntity`

### `infrastructure`

职责：

- Spring 配置
- Jackson 配置
- 数据库与审计配置
- 时间与 JSON 等工具类

### `divination`

职责：

- 接收起卦输入
- 生成 `ChartSnapshot`
- 承载排盘领域对象

第一阶段只实现占位型 `ChartBuilderService`，保证结构可运行、可测试、可替换。

核心对象：

- `DivinationInput`
- `ChartSnapshot`
- `LineInfo`
- `Hexagram`

### `rule`

职责：

- 定义规则接口
- 组织规则执行
- 输出结构化 `RuleHit`

第一阶段包含：

- `Rule`
- `RuleHit`
- `RuleEngineService`
- `usegod` 子包
- 2~3 条简单确定性规则的预留位置

### `analysis`

职责：

- 编排分析输入
- 生成占位分析结果
- 为后续 LLM 接入保留稳定接口

### `casecenter`

职责：

- 管理卦例主记录
- 保存排盘快照
- 保存规则命中
- 保存分析结果

### `book`

职责：

- 管理书籍元数据
- 创建解析任务
- 查询书籍及分块

第一阶段只保留控制器、服务接口和实体映射骨架。

### `knowledge`

职责：

- 提供知识检索编排入口
- 预留关键词召回与向量召回服务接口

### `task`

职责：

- 管理异步任务状态
- 作为书籍解析等异步能力的承接点

### `auth`

职责：

- 预留用户与权限边界

第一阶段不实现完整认证流程，仅保留骨架。

## Domain Contracts

### `ChartSnapshot`

必须至少包含以下字段：

- `question`
- `questionCategory`
- `divinationMethod`
- `divinationTime`
- `mainHexagram`
- `changedHexagram`
- `shi`
- `ying`
- `riChen`
- `yueJian`
- `kongWang`
- `lines`
- `ext`
- `snapshotVersion`
- `calendarVersion`

说明：

- `question` 与 `questionCategory` 是 `usegod` 规则包的最小依赖。
- `snapshotVersion` 与 `calendarVersion` 用于后续算法升级下的历史兼容。

### `LineInfo`

第一阶段建议包含：

- `index`
- `yinYang`
- `moving`
- `changeTo`
- `liuQin`
- `liuShen`
- `branch`
- `shi`
- `ying`

### `RuleHit`

第一阶段统一结构：

- `ruleCode`
- `ruleName`
- `hit`
- `hitReason`
- `impactLevel`
- `evidence`

后续如需补充 `targets`、`explanation` 等字段，以兼容扩展方式追加。

## Persistence Scope

第一阶段数据库落表以文档为准，重点优先以下表：

- `divination_case`
- `case_chart_snapshot`
- `case_rule_hit`
- `case_analysis_result`
- `doc_process_task`
- `book`
- `book_chunk`
- `user_account`

说明：

- 即使部分模块业务未实现，也先保留表结构与实体边界。
- `pgvector` 的扩展准备保留在脚本中，但向量能力不纳入本阶段验收重点。

## Delivery Phases

### Phase 1: Complete Skeleton

目标：

- 完整生成 Spring Boot 工程
- 所有主模块完成目录与基础类落位
- 基础配置、统一响应、数据库脚本、核心领域对象齐备
- `usegod` 规则包完成路径迁移与接入准备

验收标准：

- 工程可启动
- 包结构与模块结构完整
- 关键类名、目录、资源位置稳定

### Phase 2: Minimal Flow

目标：

- 打通最小主链路
- `DivinationController` 接收请求
- `ChartBuilderService` 生成占位 `ChartSnapshot`
- `RuleEngineService` 执行 `UseGodRule`
- `casecenter` 完成基础留痕
- 返回结构化响应

验收标准：

- 调用一次接口即可拿到 `chart + ruleHits + analysisStub`
- 占位快照、规则命中可被保存

## Risks

1. 排盘算法尚未实现，第一阶段仅能提供结构正确但业务占位的快照。
2. `usegod` 规则依赖 `question` 和 `questionCategory`，若后续前端入参不稳定，需要在 DTO 层提前约束。
3. 文档中数据库设计较完整，但骨架阶段若全部映射过深，可能增加无效代码；因此第一阶段以边界清晰优先。
4. 当前仓库无既有工程和测试基础，本次生成需同步补上最小启动验证与规则单测基础。

## Open Questions

1. 工程目录名是否正式使用 `liuyao-app`。
2. 构建工具默认采用 Maven 是否满足后续团队习惯。
3. 第一阶段数据库迁移工具使用 Flyway 还是纯 SQL 初始化脚本。

## Acceptance

本规格通过后，下一步将输出细化任务清单，并按“先骨架、后闭环”的顺序实施。
