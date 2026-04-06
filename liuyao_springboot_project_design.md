# 六爻辅助研判系统 - Spring Boot 项目骨架设计文档

> 版本：v1.0  
> 目标：给出一套适合第一阶段直接开工的 Java 主服务骨架设计  
> 技术栈：Java 17 + Spring Boot + PostgreSQL + pgvector

---

# 1. 设计目标

Spring Boot 主服务承担的是**系统主业务逻辑**，核心职责包括：

- 卦例管理
- 排盘引擎
- 规则引擎
- 书籍元数据管理
- 知识检索编排
- AI 分析编排
- 异步任务状态管理

这份文档重点不是“怎么把项目搭起来”，而是：
- 模块怎么分
- 每层放什么
- 关键类该怎么命名
- 第一阶段哪些东西必须先有

---

# 2. 推荐项目结构

```text
liuyao-app/
  ├── src/main/java/com/example/liuyao
  │   ├── LiuyaoApplication.java
  │   ├── common/
  │   ├── auth/
  │   ├── book/
  │   ├── knowledge/
  │   ├── divination/
  │   ├── rule/
  │   ├── analysis/
  │   ├── casecenter/
  │   ├── task/
  │   └── infrastructure/
  ├── src/main/resources/
  │   ├── application.yml
  │   └── db/
  └── pom.xml
```

---

# 3. 分层规范

每个业务模块建议统一按下面的层次组织：

```text
module/
  ├── controller/
  ├── service/
  ├── domain/
  ├── repository/
  ├── dto/
  └── mapper/
```

## 各层职责

### controller
- 接 HTTP 请求
- 参数校验
- 返回统一响应

### service
- 编排业务逻辑
- 事务控制
- 调用其他模块服务

### domain
- 核心领域对象
- 值对象
- 领域规则

### repository
- DB 持久化访问
- JPA / MyBatis 映射

### dto
- 入参 / 出参对象
- 不直接暴露数据库实体

### mapper
- DTO 与领域对象 / 实体之间转换

---

# 4. 核心模块设计

# 4.1 common 模块

## 作用
放通用基础设施，避免各业务模块重复造轮子。

## 建议类
- `ApiResponse<T>`
- `BusinessException`
- `ErrorCode`
- `BaseEntity`
- `JsonUtils`
- `TimeUtils`
- `PageResult<T>`

---

# 4.2 book 模块

## 作用
处理书籍上传后的主业务入口，不做具体解析，只管理：
- 书籍元数据
- 解析任务创建
- chunk 查询

## 推荐目录
```text
book/
  ├── controller/
  │   └── BookController.java
  ├── service/
  │   ├── BookService.java
  │   └── BookQueryService.java
  ├── domain/
  │   ├── Book.java
  │   └── BookChunk.java
  ├── repository/
  │   ├── BookRepository.java
  │   └── BookChunkRepository.java
  ├── dto/
  │   ├── BookUploadResponse.java
  │   ├── BookListItemDTO.java
  │   └── BookChunkDTO.java
  └── mapper/
      └── BookMapper.java
```

## 关键服务
- `BookService#uploadBook(...)`
- `BookService#createParseTask(...)`
- `BookQueryService#listBooks(...)`
- `BookQueryService#listBookChunks(...)`

---

# 4.3 divination 模块

## 作用
排盘引擎，是整个系统的“确定性底座”。

## 推荐目录
```text
divination/
  ├── controller/
  │   └── DivinationController.java
  ├── service/
  │   ├── DivinationService.java
  │   └── ChartBuilderService.java
  ├── domain/
  │   ├── ChartSnapshot.java
  │   ├── LineInfo.java
  │   ├── Hexagram.java
  │   └── DivinationInput.java
  ├── dto/
  │   ├── DivinationAnalyzeRequest.java
  │   └── DivinationAnalyzeResponse.java
  └── mapper/
      └── DivinationMapper.java
```

## 关键对象
- `DivinationInput`：起卦输入
- `ChartSnapshot`：完整排盘输出
- `LineInfo`：单爻信息
- `Hexagram`：卦象对象

## 关键服务
- `DivinationService#analyze(...)`
- `ChartBuilderService#buildChart(...)`

---

# 4.4 rule 模块

## 作用
基于 `ChartSnapshot` 命中规则，输出结构化规则结果。

## 推荐目录
```text
rule/
  ├── service/
  │   ├── RuleEngineService.java
  │   ├── Rule.java
  │   └── rules/
  │       ├── JobUseGodRule.java
  │       ├── ShiYingRelationRule.java
  │       ├── EmptyRule.java
  │       └── MovingLineRule.java
  ├── domain/
  │   └── RuleHit.java
  └── dto/
      └── RuleHitDTO.java
```

## 核心接口建议

### `Rule`
```java
public interface Rule {
    RuleHit evaluate(ChartSnapshot chartSnapshot);
}
```

### `RuleEngineService`
负责：
- 加载规则列表
- 顺序执行规则
- 聚合 `RuleHit`

## 第一批建议规则
- `JobUseGodRule`
- `WealthUseGodRule`
- `ShiWeakRule`
- `YingStrongRule`
- `EmptyRule`
- `MonthBreakRule`
- `MovingLineRule`
- `LiuChongRule`

---

# 4.5 knowledge 模块

## 作用
做知识检索编排，不直接关心 embedding 模型细节。

## 推荐目录
```text
knowledge/
  ├── controller/
  │   └── KnowledgeController.java
  ├── service/
  │   ├── KnowledgeSearchService.java
  │   ├── KeywordRecallService.java
  │   └── VectorRecallService.java
  ├── domain/
  │   └── KnowledgeReference.java
  ├── dto/
  │   ├── KnowledgeSearchRequest.java
  │   └── KnowledgeSearchResponse.java
  └── mapper/
      └── KnowledgeMapper.java
```

## 推荐职责分拆
- `KeywordRecallService`：关键词召回
- `VectorRecallService`：向量召回
- `KnowledgeSearchService`：合并排序去重

---

# 4.6 analysis 模块

## 作用
AI 编排层，只负责：
- 组织输入
- 调用模型
- 存结果

## 推荐目录
```text
analysis/
  ├── service/
  │   ├── AnalysisService.java
  │   ├── PromptBuilder.java
  │   └── AnalysisRecordService.java
  ├── domain/
  │   ├── AnalysisInput.java
  │   └── AnalysisResult.java
  └── dto/
      └── AnalysisDTO.java
```

## 核心服务
- `AnalysisService#generateAnalysis(...)`
- `PromptBuilder#build(...)`
- `AnalysisRecordService#save(...)`

---

# 4.7 casecenter 模块

## 作用
沉淀卦例与复盘数据。

## 推荐目录
```text
casecenter/
  ├── controller/
  │   └── CaseController.java
  ├── service/
  │   ├── CaseService.java
  │   ├── CaseQueryService.java
  │   └── CaseFeedbackService.java
  ├── domain/
  │   ├── DivinationCase.java
  │   ├── CaseChartSnapshot.java
  │   └── CaseAnalysisRecord.java
  ├── repository/
  │   ├── DivinationCaseRepository.java
  │   ├── CaseChartSnapshotRepository.java
  │   ├── CaseRuleHitRepository.java
  │   └── CaseAnalysisRecordRepository.java
  ├── dto/
  │   ├── CreateCaseRequest.java
  │   ├── CaseDetailResponse.java
  │   └── CaseFeedbackRequest.java
  └── mapper/
      └── CaseMapper.java
```

## 核心服务
- `CaseService#createCase(...)`
- `CaseQueryService#getCaseDetail(...)`
- `CaseFeedbackService#saveFeedback(...)`

---

# 4.8 task 模块

## 作用
管理文档处理任务和状态。

## 推荐目录
```text
task/
  ├── service/
  │   ├── TaskService.java
  │   └── TaskQueryService.java
  ├── domain/
  │   └── DocProcessTask.java
  ├── repository/
  │   └── DocProcessTaskRepository.java
  └── dto/
      └── TaskDTO.java
```

## 核心服务
- `TaskService#createBookParseTask(...)`
- `TaskService#markProcessing(...)`
- `TaskService#markSuccess(...)`
- `TaskService#markFailed(...)`

---

# 4.9 infrastructure 模块

## 作用
放技术适配层，而不是业务逻辑。

## 推荐子模块
```text
infrastructure/
  ├── db/
  ├── storage/
  ├── model/
  └── config/
```

## 关键能力
- PostgreSQL 持久化配置
- pgvector SQL 封装
- 文件存储封装
- 外部模型调用封装

---

# 5. 推荐的统一响应结构

```java
public class ApiResponse<T> {
    private boolean success;
    private String code;
    private String message;
    private T data;
}
```

## 成功返回示例
```json
{
  "success": true,
  "code": "OK",
  "message": "success",
  "data": {}
}
```

---

# 6. 关键 DTO 设计

# 6.1 排盘分析请求 DTO

```java
public class DivinationAnalyzeRequest {
    private String questionText;
    private String questionCategory;
    private LocalDateTime divinationTime;
    private String divinationMethod;
    private List<String> rawLines;
    private List<Integer> movingLines;
}
```

# 6.2 排盘分析响应 DTO

```java
public class DivinationAnalyzeResponse {
    private ChartSnapshot chartSnapshot;
    private List<RuleHitDTO> ruleHits;
    private AnalysisDTO analysis;
    private List<KnowledgeReferenceDTO> references;
}
```

# 6.3 知识检索请求 DTO

```java
public class KnowledgeSearchRequest {
    private String query;
    private Integer topK;
    private KnowledgeFilterDTO filters;
}
```

# 6.4 保存卦例请求 DTO

```java
public class CreateCaseRequest {
    private String questionText;
    private String questionCategory;
    private LocalDateTime divinationTime;
    private ChartSnapshot chartSnapshot;
    private List<RuleHitDTO> ruleHits;
    private AnalysisDTO analysis;
}
```

---

# 7. 关键领域对象建议

# 7.1 ChartSnapshot

```java
public class ChartSnapshot {
    private String question;
    private String questionCategory;
    private String divinationMethod;
    private LocalDateTime divinationTime;
    private String mainHexagram;
    private String changedHexagram;
    private Integer shi;
    private Integer ying;
    private String riChen;
    private String yueJian;
    private List<String> kongWang;
    private List<LineInfo> lines;
}
```

# 7.2 LineInfo

```java
public class LineInfo {
    private Integer index;
    private String yinYang;
    private Boolean moving;
    private String changeTo;
    private String liuQin;
    private String liuShen;
    private String branch;
    private Boolean shi;
    private Boolean ying;
}
```

# 7.3 RuleHit

```java
public class RuleHit {
    private String ruleCode;
    private String ruleName;
    private Boolean hit;
    private String hitReason;
    private String impactLevel;
    private String explanation;
    private Map<String, Object> evidence;
}
```

---

# 8. 主链路服务编排建议

## 8.1 排盘分析主流程

建议由 `DivinationService` 统一编排：

1. 参数转 `DivinationInput`
2. 调 `ChartBuilderService` 生成 `ChartSnapshot`
3. 调 `RuleEngineService` 计算 `RuleHit`
4. 调 `KnowledgeSearchService` 检索引用
5. 调 `AnalysisService` 生成 AI 分析
6. 组装响应

## 伪代码示意
```java
public DivinationAnalyzeResponse analyze(DivinationAnalyzeRequest request) {
    DivinationInput input = divinationMapper.toInput(request);
    ChartSnapshot chart = chartBuilderService.buildChart(input);
    List<RuleHit> ruleHits = ruleEngineService.evaluate(chart);
    List<KnowledgeReference> refs = knowledgeSearchService.searchForAnalysis(request.getQuestionText(), chart, ruleHits);
    AnalysisResult analysis = analysisService.generateAnalysis(request.getQuestionText(), chart, ruleHits, refs);
    return divinationMapper.toResponse(chart, ruleHits, analysis, refs);
}
```

---

# 9. 模型客户端接口建议

# 9.1 EmbeddingClient

```java
public interface EmbeddingClient {
    List<Double> embed(String text);
}
```

## 实现示例
- `AliEmbeddingClient`
- `MockEmbeddingClient`

# 9.2 ChatModelClient

```java
public interface ChatModelClient {
    String chat(String prompt);
}
```

## 实现示例
- `AliChatModelClient`
- `MockChatModelClient`

---

# 10. Repository 设计建议

如果你习惯 JPA，可以先用 JPA 跑通 CRUD。  
如果你更在意复杂 SQL 控制，`knowledge` 模块可以单独用 MyBatis 或 JDBC Template。

## 推荐做法
- 普通业务表：JPA
- 向量检索与自定义 SQL：JDBC Template / MyBatis

这样比较灵活。

---

# 11. application.yml 建议骨架

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/liuyao
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

app:
  storage:
    local-base-path: /data/liuyao
  model:
    embedding:
      provider: ali
      model-name: text-embedding
    chat:
      provider: ali
      model-name: qwen
```

---

# 12. 第一阶段必做类清单

## 12.1 common
- `ApiResponse`
- `BusinessException`

## 12.2 divination
- `DivinationController`
- `DivinationService`
- `ChartBuilderService`
- `ChartSnapshot`
- `LineInfo`
- `DivinationAnalyzeRequest`
- `DivinationAnalyzeResponse`

## 12.3 rule
- `Rule`
- `RuleEngineService`
- `RuleHit`
- 第一批规则类

## 12.4 casecenter
- `CaseController`
- `CaseService`
- `CaseQueryService`
- `CreateCaseRequest`
- `CaseDetailResponse`

## 12.5 book
- `BookController`
- `BookService`
- `BookQueryService`

## 12.6 task
- `TaskService`

---

# 13. 第一阶段建议开发顺序

## 第一步：基础工程
- 初始化 Spring Boot 项目
- 接 PostgreSQL
- 建基础表
- 建统一异常和响应结构

## 第二步：卦例与排盘
- 实现 `ChartSnapshot`
- 实现 `DivinationController`
- 实现排盘引擎骨架
- 保存卦例

## 第三步：规则引擎
- 实现 `Rule` 接口
- 实现 `RuleEngineService`
- 先上 5~8 条规则

## 第四步：知识库入口
- 实现上传书籍
- 创建任务记录
- 查询 chunk

## 第五步：分析编排
- 接入模型客户端接口
- 用 mock 先跑通
- 后面再切真实模型

---

# 14. 重要提醒

## 14.1 Controller 不要写业务
Controller 只做入参、出参和路由。

## 14.2 Service 不要直接拼 SQL 向量检索
向量 SQL 统一放 repository / infrastructure 层。

## 14.3 不要把模型 SDK 散落在各模块
统一从 `infrastructure/model` 出口调用。

## 14.4 先用 mock 跑通 AI 编排
这样你可以先验证接口和流程，不被模型接入卡住。

---

# 15. 总结

这套 Spring Boot 骨架的核心思想是：

- **业务模块清晰**
- **确定性引擎集中**
- **模型调用统一出口**
- **先跑单体，再逐步增强**

第一阶段的目标不是“做出完美架构”，而是：

1. 把排盘主链路跑通
2. 把规则命中跑通
3. 把卦例沉淀下来
4. 给后续知识库和 AI 分析预留干净接口
