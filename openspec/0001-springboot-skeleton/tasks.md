# Liuyao Spring Boot Skeleton Task Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 先生成完整 Spring Boot 主服务骨架，再在骨架稳定后补最小闭环。

**Architecture:** 使用单体 Spring Boot 工程承载主服务能力，按业务模块和统一分层组织代码。以 `ChartSnapshot` 为核心契约，先接入 `usegod` 规则包，再补最小主链路。

**Tech Stack:** Java 17, Spring Boot, Maven, PostgreSQL, Jackson, JPA/MyBatis 待定, Flyway 或 SQL 初始化脚本

---

## Chunk 1: Scope Lock

### Task 1: 固定工程生成约束

**Files:**
- Create: `openspec/0001-springboot-skeleton/spec.md`
- Create: `openspec/0001-springboot-skeleton/tasks.md`

- [ ] 确认根包名固定为 `com.yishou.liuyao`
- [ ] 确认实施顺序固定为“完整骨架 -> 最小闭环”
- [ ] 确认第一阶段非目标：不做真实排盘、不接 LLM、不做完整知识检索
- [ ] 确认 `ChartSnapshot`、`RuleHit`、数据库留痕为核心契约

### Task 2: 明确工程初始化决策

**Files:**
- Create: `liuyao-app/pom.xml`
- Create: `liuyao-app/src/main/resources/application.yml`

- [ ] 决定构建工具为 Maven
- [ ] 决定 Java 版本为 17
- [ ] 决定 Spring Boot 核心依赖集合
- [ ] 决定数据库接入方式与迁移脚本目录
- [ ] 决定测试依赖与最小验证命令

## Chunk 2: Bootstrap

### Task 3: 创建工程骨架

**Files:**
- Create: `liuyao-app/pom.xml`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/LiuyaoApplication.java`
- Create: `liuyao-app/src/main/resources/application.yml`

- [ ] 创建 Maven `pom.xml`
- [ ] 创建 Spring Boot 启动类
- [ ] 创建基础 `application.yml`
- [ ] 添加 `src/test` 目录占位
- [ ] 运行启动类级别最小构建验证

### Task 4: 创建模块目录

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/auth/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/task/`

- [ ] 为每个模块创建标准分层目录
- [ ] 保持命名与设计文档一致
- [ ] 不引入无关模块

## Chunk 3: Common And Infrastructure

### Task 5: 落通用层

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/dto/ApiResponse.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/dto/PageResult.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/exception/BusinessException.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/exception/ErrorCode.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/common/domain/BaseEntity.java`

- [ ] 创建统一响应对象
- [ ] 创建分页对象
- [ ] 创建异常与错误码
- [ ] 创建基础实体父类
- [ ] 为全局异常处理预留入口

### Task 6: 落基础配置

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/config/JacksonConfig.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/config/DatabaseConfig.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/util/JsonUtils.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/infrastructure/util/TimeUtils.java`

- [ ] 创建 Jackson 配置
- [ ] 创建数据库配置占位
- [ ] 创建 JSON 工具类
- [ ] 创建时间工具类

## Chunk 4: Core Domain Skeleton

### Task 7: 落 `divination` 领域对象

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/DivinationInput.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/ChartSnapshot.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/LineInfo.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/Hexagram.java`

- [ ] 创建 `DivinationInput`
- [ ] 创建 `ChartSnapshot`
- [ ] 创建 `LineInfo`
- [ ] 创建 `Hexagram`
- [ ] 为版本字段与扩展字段留口

### Task 8: 落 `divination` 服务与接口骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/controller/DivinationController.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/service/DivinationService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ChartBuilderService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/dto/DivinationAnalyzeRequest.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/dto/DivinationAnalyzeResponse.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/divination/mapper/DivinationMapper.java`

- [ ] 创建请求响应 DTO
- [ ] 创建控制器
- [ ] 创建主编排服务
- [ ] 创建占位排盘服务
- [ ] 创建 DTO 映射器

## Chunk 5: Rule Engine

### Task 9: 落规则引擎骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/Rule.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/RuleHit.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEngineService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/dto/RuleHitDTO.java`

- [ ] 创建规则接口
- [ ] 创建结构化命中对象
- [ ] 创建规则执行服务
- [ ] 创建对外 DTO

### Task 10: 接入 `usegod` 规则包

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/*.java`
- Create: `liuyao-app/src/main/resources/rules/use_god_rules.json`

- [ ] 迁移 `liuyao_usegod_pack` 的 `usegod` 代码
- [ ] 将包名调整为 `com.yishou.liuyao`
- [ ] 对齐 `ChartSnapshot` 的字段访问
- [ ] 确保配置文件能在启动时加载
- [ ] 为后续简单规则预留 `rules/` 目录

## Chunk 6: Case Persistence

### Task 11: 落 `casecenter` 骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/domain/DivinationCase.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/domain/CaseChartSnapshot.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/domain/CaseRuleHit.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/domain/CaseAnalysisResult.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/repository/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/*.java`

- [ ] 创建卦例实体
- [ ] 创建快照实体
- [ ] 创建规则命中实体
- [ ] 创建分析结果实体
- [ ] 创建 repository 接口
- [ ] 创建服务骨架

### Task 12: 添加数据库脚本

**Files:**
- Create: `liuyao-app/src/main/resources/db/migration/*`

- [ ] 增加核心建表脚本
- [ ] 增加 `pgvector` 扩展准备脚本
- [ ] 保持表名和字段名与设计文档一致
- [ ] 避免提前做过深范式化

## Chunk 7: Secondary Modules

### Task 13: 落 `analysis` 骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/controller/AnalysisController.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/*.java`

- [ ] 创建分析模块控制器
- [ ] 创建分析服务接口
- [ ] 创建占位分析返回结构

### Task 14: 落 `book` 骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/controller/BookController.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/service/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/domain/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/repository/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/dto/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/book/mapper/BookMapper.java`

- [ ] 创建书籍模块骨架
- [ ] 保留上传、查询、分块查询入口
- [ ] 暂不实现真实解析逻辑

### Task 15: 落 `knowledge` 骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/controller/KnowledgeController.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/domain/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/dto/*.java`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/knowledge/mapper/KnowledgeMapper.java`

- [ ] 创建知识模块骨架
- [ ] 预留关键词召回与向量召回服务
- [ ] 暂不实现真实检索逻辑

### Task 16: 落 `task` 与 `auth` 骨架

**Files:**
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/task/**`
- Create: `liuyao-app/src/main/java/com/yishou/liuyao/auth/**`

- [ ] 创建任务模块骨架
- [ ] 创建认证模块骨架
- [ ] 保持接口最小化

## Chunk 8: Minimal Flow

### Task 17: 打通最小业务闭环

**Files:**
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/controller/DivinationController.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/service/DivinationService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ChartBuilderService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEngineService.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/*.java`
- Modify: `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`

- [ ] 接收分析请求
- [ ] 生成占位 `ChartSnapshot`
- [ ] 执行 `usegod` 规则
- [ ] 生成占位分析结果
- [ ] 保存 case、snapshot、ruleHit、analysisResult
- [ ] 返回聚合响应

## Chunk 9: Verification

### Task 18: 建立最小验证

**Files:**
- Create: `liuyao-app/src/test/java/com/yishou/liuyao/...`

- [ ] 增加应用启动测试
- [ ] 增加 `QuestionIntentResolver` 单测
- [ ] 增加 `UseGodRule` 单测
- [ ] 增加最小闭环服务层测试
- [ ] 执行构建与测试验证

### Task 19: 收尾检查

**Files:**
- Modify: `liuyao-app/README.md`

- [ ] 写明启动方式
- [ ] 写明当前阶段已实现与未实现内容
- [ ] 确认没有偏离最小骨架目标
- [ ] 确认后续迭代入口清晰
