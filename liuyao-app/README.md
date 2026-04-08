# liuyao-app

当前工程是基于 `com.yishou.liuyao` 的 Spring Boot 六爻骨架项目。

## 模块说明

- `divination`: 起卦输入、排盘构建、分析主流程
- `rule`: 用神与结构规则
- `casecenter`: 卦例留痕、案例列表、案例详情
- `analysis`: 当前为工程第一版分段式结构化解读服务
- `common / infrastructure`: 通用响应、异常、JSON、配置等基础设施

## 启动说明

本地开发默认使用 Spring Boot 配置。

- JDK: `17+`
- 构建: `mvn clean test`
- 启动: `mvn spring-boot:run`

如果你直接本地运行 PostgreSQL，可以先设置这些环境变量：

```bash
export DB_URL=jdbc:postgresql://localhost:5432/liuyao
export DB_USERNAME=dify
export DB_PASSWORD=password123
export SERVER_PORT=8080
```

如果你希望直接用 Docker 启 PostgreSQL，可以在项目根目录执行：

```bash
docker compose up -d
```

对应文件：

- [docker-compose.yml](/Users/liuyishou/wordspace/liuyao/liuyao-app/docker-compose.yml)

## 数据库说明

- 生产目标数据库为 PostgreSQL
- 当前测试使用 H2 内存库
- 数据库结构通过 Flyway 管理
- 当前迁移版本：
  - `V1__init_schema.sql`
  - `V2__add_case_snapshot_index_fields.sql`
  - `V3__add_analysis_context_to_case_analysis_result.sql`

推荐先在 PostgreSQL 中创建数据库：

```sql
CREATE DATABASE liuyao;
```

如果使用仓库自带 `docker-compose.yml`，数据库会自动创建为 `liuyao`。

## 测试命令

- 全量测试：`mvn test -q`
- 指定接口测试：`mvn -q -Dtest=DivinationControllerTest,CaseCenterControllerTest test`
- 指定规则测试：`mvn -q -Dtest=QuestionIntentResolverTest,RuleEngineScenarioRegressionTest test`

## 当前已打通

- 起卦分析最小闭环
- 真实历法字段：`riChen / yueJian / kongWang`
- 本卦/变卦、世应、六神、纳甲、六亲第一版
- 用神规则与部分结构规则
- 卦例留痕与最近案例查询
- 单页前端联调台：`GET /`

## 分析接口示例

`POST /api/divinations/analyze`

请求示例：

```json
{
  "questionText": "这次出行会不会顺利",
  "questionCategory": "出行",
  "divinationMethod": "手工起卦",
  "divinationTime": "2026-04-06T10:00:00",
  "rawLines": ["老阳", "少阴", "少阳", "少阴", "老阴", "少阳"],
  "movingLines": [1, 5]
}
```

返回体中的关键字段：

- `chartSnapshot.mainHexagram`
- `chartSnapshot.changedHexagram`
- `chartSnapshot.mainHexagramCode`
- `chartSnapshot.changedHexagramCode`
- `chartSnapshot.mainUpperTrigram`
- `chartSnapshot.mainLowerTrigram`
- `chartSnapshot.palace`
- `chartSnapshot.useGod`
- `chartSnapshot.lines`
- `ruleHits`
- `analysis`
- `analysisContext`
- `structuredResult`

成功响应片段示例：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "chartSnapshot": {
      "questionCategory": "出行",
      "mainHexagram": "山火贲",
      "changedHexagram": "风山渐",
      "mainHexagramCode": "001101",
      "changedHexagramCode": "011001",
      "palace": "艮",
      "useGod": "父母"
    },
    "ruleHits": [
      {
        "ruleCode": "USE_GOD_SELECTION",
        "ruleName": "用神选择",
        "impactLevel": "HIGH"
      }
    ],
    "analysis": "卦象概览：问收入，本卦山火贲，变卦风山渐。 用神判断：本次以妻财为用神。当前共命中11条规则。",
    "analysisContext": {
      "contextVersion": "v1",
      "useGod": "父母",
      "mainHexagram": "山火贲",
      "changedHexagram": "风山渐"
    },
    "structuredResult": {
      "score": 1,
      "resultLevel": "NEUTRAL",
      "effectiveScore": 3,
      "effectiveResultLevel": "GOOD",
      "effectiveRuleCodes": ["R003"],
      "suppressedRuleCodes": ["R005"],
      "categorySummaries": [
        {
          "category": "YONGSHEN_STATE",
          "hitCount": 1,
          "score": 0,
          "effectiveHitCount": 1,
          "effectiveScore": 2,
          "stageOrder": 1
        }
      ],
      "conflictSummaries": [
        {
          "category": "YONGSHEN_STATE",
          "positiveCount": 1,
          "negativeCount": 1,
          "positiveScore": 3,
          "negativeScore": -1,
          "netScore": 2,
          "decision": "POSITIVE_DOMINANT",
          "positiveRules": ["R003"],
          "negativeRules": ["R005"],
          "effectiveRules": ["R003"],
          "suppressedRules": ["R005"]
        }
      ]
    }
  }
}
```

## 案例查询接口示例

`GET /api/cases`

返回每条记录的关键摘要字段：

- `caseId`
- `questionText`
- `questionCategory`
- `divinationTime`
- `status`
- `mainHexagram`
- `changedHexagram`
- `palace`
- `useGod`

## 案例详情接口示例

`GET /api/cases/{caseId}`

返回体中的关键字段：

- `caseId`
- `questionText`
- `questionCategory`
- `divinationTime`
- `status`
- `chartSnapshot`
- `ruleHits`
- `analysis`
- `analysisContext`

## API 草稿

更完整的接口文档见：

- [docs/api.md](/Users/liuyishou/wordspace/liuyao/liuyao-app/docs/api.md)

当前第一版接口：

- `POST /api/divinations/analyze`
- `GET /api/rules/definitions`
- `GET /api/cases`
- `GET /api/cases/search?questionCategory=出行&page=1&size=5`
- `GET /api/cases/{caseId}`

当前 `questionCategory` 已接入一层轻量规范化，首批标准问类为：

- `求职 / 工作 / 收入 / 压力 / 人际 / 成长 / 考试 / 感情 / 健康 / 出行 / 合作`

第二批已接入标准问类为：

- `财运 / 婚姻 / 复合 / 升职 / 调岗 / 房产 / 搬家 / 官司 / 寻物`

常见同义分类会自动归一，例如：

- `工资 / 薪资 / 财运 -> 收入`
- `面试 / 录用 / offer -> 求职`
- `关系 / 同事 -> 人际`
- `婚姻 / 恋爱 / 复合 -> 感情`
- `旅行 / 差旅 -> 出行`
- `买房 / 卖房 -> 房产`
- `诉讼 / 纠纷 -> 官司`
- `失物 / 找东西 -> 寻物`

当前第二批里已经补到专门用神配置的类目为：

- `房产 -> 父母`
- `搬家 -> 父母`
- `官司 -> 官鬼`
- `寻物 -> 妻财`

当前响应风格统一为：

- `success`
- `code`
- `message`
- `data`

常见错误返回示例：

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "案例不存在"
}
```

规则 evidence 当前已经逐步统一，常见会包含：

- `useGod`
- `mainHexagram / changedHexagram`
- `mainUpperTrigram / mainLowerTrigram`
- `changedUpperTrigram / changedLowerTrigram`
- `palace / palaceWuXing`
- `targetCount`

当前分析文本已经进入“工程第二版”：

- 会输出 `卦象概览 / 用神判断 / 关系判断 / 动爻影响 / 风险提示 / 结论建议 / 下一步建议 / 可参考资料`
- 不同问类会有不同的关注点、风险点和行动建议
- `targetSummary / targets`

结构化分析结果当前还会额外返回：

- `structuredResult.score`
- `structuredResult.resultLevel`
- `structuredResult.effectiveScore`
- `structuredResult.effectiveResultLevel`
- `structuredResult.tags`
- `structuredResult.effectiveRuleCodes`
- `structuredResult.suppressedRuleCodes`
- `structuredResult.summary`
- `structuredResult.categorySummaries`
- `structuredResult.categorySummaries[*].effectiveHitCount / effectiveScore`
- `structuredResult.conflictSummaries`

知识片段当前会尽量带来源前缀，例如：

- `[《增删卜易》·用神总论] 用神宜旺相，不宜休囚。`

规则定义查询接口当前会返回：

- `version`
- `total`
- `rules[*].ruleId / ruleCode / name`
- `rules[*].conditionJson / effectJson`

## 前端联调台

当前工程已经自带一个不引前端框架的静态联调页，直接由 Spring Boot 托管：

- 入口：`GET /`
- 静态资源：`/index.html`、`/app.css`、`/app.js`

当前页面能力：

- 固定盘起卦分析
- 分析结果摘要展示
- 规则命中摘要展示
- 案例列表筛选与刷新
- 案例详情查看

## 资料导入准备接口

当前已经补了首批资料导入的准备接口：

- `POST /api/books/import-requests`
- `GET /api/books`
- `GET /api/tasks/doc-process`
- `POST /api/tasks/doc-process/{taskId}/execute`
- `GET /api/knowledge/import-topics`
- `GET /api/knowledge/references?topicTag=用神`

这几条接口现在的职责是：

- 先把原始资料登记进书目台账
- 同步创建解析任务
- 执行 `txt / 可抽文本 PDF` 的正文抽取与切片入库
- 给出首批建议导入的主题清单
- 提供最小知识查询结果，确认导入内容可被消费
