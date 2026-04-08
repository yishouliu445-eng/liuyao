# API 文档

本文档描述 `liuyao-app` 当前已经实现并可联调的接口。

## 统一响应格式

所有接口统一返回：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {}
}
```

失败时格式保持一致：

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "案例不存在"
}
```

## 1. 起卦分析

### 请求

- Method: `POST`
- Path: `/api/divinations/analyze`
- Content-Type: `application/json`

请求体：

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

字段说明：

- `questionText`: 必填，问题文本
- `questionCategory`: 选填，问题分类，进入主流程前会做轻量规范化。当前首批标准问类为 `求职 / 工作 / 收入 / 压力 / 人际 / 成长 / 考试 / 感情 / 健康 / 出行 / 合作`
- 第二批已接入标准问类：`财运 / 婚姻 / 复合 / 升职 / 调岗 / 房产 / 搬家 / 官司 / 寻物`
- `divinationMethod`: 选填，起卦方式说明
- `divinationTime`: 必填，起卦时间
- `rawLines`: 选填，六爻原始输入，推荐传 6 项
- `movingLines`: 选填，动爻序号列表

常见别名示例：

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

### 成功响应

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
    "data": {
      "chartSnapshot": {
        "question": "这次出行会不会顺利",
        "questionCategory": "出行",
        "divinationMethod": "MANUAL_PLACEHOLDER",
        "divinationTime": "2026-04-06T10:00:00",
        "mainHexagram": "山火贲",
        "changedHexagram": "风山渐",
        "mainHexagramCode": "001101",
        "changedHexagramCode": "011001",
        "mainUpperTrigram": "艮",
        "mainLowerTrigram": "离",
        "changedUpperTrigram": "巽",
        "changedLowerTrigram": "艮",
        "palace": "艮",
        "palaceWuXing": "土",
        "shi": 1,
        "ying": 4,
        "useGod": "父母",
        "riChen": "庚戌",
        "yueJian": "辰",
        "snapshotVersion": "v1",
        "calendarVersion": "v1",
        "kongWang": ["寅", "卯"],
        "lines": [
          {
            "index": 1,
            "yinYang": "阳",
            "moving": true,
            "changeTo": "阴",
            "liuQin": "官鬼",
            "liuShen": "玄武",
            "branch": "卯",
            "wuXing": "木",
            "changeBranch": "辰",
            "changeWuXing": "土",
            "changeLiuQin": "兄弟",
            "shi": true,
            "ying": false
          }
        ]
    },
    "ruleHits": [
      {
        "ruleCode": "USE_GOD_SELECTION",
        "ruleName": "用神选择",
        "hitReason": "根据问题意图选择当前优先观察对象。",
        "impactLevel": "HIGH",
        "evidence": {
          "intent": "TRAVEL",
          "useGod": "父母"
        }
      }
    ],
    "analysis": "卦象概览：问出行，本卦山火贲，变卦风山渐。 用神判断：本次以父母为用神。当前共命中7条规则。冲突裁剪后有效评分1，整体中性，存在反复。本卦山火贲，围绕用神父母共命中7条规则，当前评分1，整体中性，存在反复，当前标签：暂无明显标签。当前未见明显规则冲突。当前以原始评分为准。 关系判断：出行层面当前标签为暂无明显标签；当前主导规则为USE_GOD_SELECTION、R010、R011、SHI_YING_EXISTS、SHI_YING_RELATION、R013、MOVING_LINE_EXISTS。 动爻影响：世爻或关键爻有发动信号，事情不会完全静止。 结论建议：当前有效评分1，整体中性，存在反复；出行层面先看行程是否受阻。",
    "analysisContext": {
      "contextVersion": "v1",
      "question": "这次出行会不会顺利",
      "questionCategory": "出行",
      "useGod": "父母",
      "mainHexagram": "山火贲",
      "changedHexagram": "风山渐",
      "ruleCount": 4,
      "ruleCodes": ["USE_GOD_SELECTION", "MOVING_LINE_EXISTS", "SHI_YING_EXISTS", "SHI_YING_RELATION"],
      "knowledgeSnippets": ["[《增删卜易》·用神总论] 用神宜旺相，不宜休囚。"]
    }
  }
}
```

### 关键返回字段

`chartSnapshot`:

- `mainHexagram / changedHexagram`: 本卦、变卦名称
- `mainHexagramCode / changedHexagramCode`: 六位卦码
- `mainUpperTrigram / mainLowerTrigram`: 本卦拆出来的上卦、下卦
- `changedUpperTrigram / changedLowerTrigram`: 变卦拆出来的上卦、下卦
- `palace / palaceWuXing`: 卦宫与宫五行
- `shi / ying`: 世应位置
- `useGod`: 当前自动选择的用神
- `riChen / yueJian / kongWang`: 历法字段
- `snapshotVersion / calendarVersion`: 当前盘面快照版本和历法版本
- `lines`: 六爻结构明细

`chartSnapshot.lines[*]`:

- `index`: 爻位序号，固定 `1-6`
- `yinYang`: 当前爻阴阳
- `moving`: 是否动爻
- `liuQin / liuShen`: 当前爻六亲与六神
- `branch / wuXing`: 当前爻纳甲地支与五行
- `changeTo / changeBranch / changeWuXing / changeLiuQin`: 动爻变后字段，静爻通常为空
- `shi / ying`: 当前爻是否为世位或应位

`ruleHits`:

- `ruleCode`: 规则编码
- `ruleName`: 规则名称
- `hitReason`: 命中原因
- `impactLevel`: 影响等级
- `evidence`: 结构化证据

当前 `evidence` 会尽量统一包含这些字段：

- `useGod`: 当前规则所围绕的用神
- `mainHexagram / changedHexagram`: 规则命中时对应的卦名
- `palace / palaceWuXing`: 当前卦宫信息
- `mainUpperTrigram / mainLowerTrigram`: 本卦上下卦
- `changedUpperTrigram / changedLowerTrigram`: 变卦上下卦
- `targetCount`: 命中目标数量
- `targetSummary`: 面向展示的目标爻摘要列表
- `targets`: 面向规则细查的目标爻明细列表

常见 `targetSummary / targets` 单项结构：

- `lineIndex`: 爻位序号
- `liuQin`: 六亲
- `branch`: 地支
- `wuXing`: 五行
- `moving`: 是否动爻
- `changeTo / changeBranch / changeWuXing / changeLiuQin`: 动变后的结构字段，静爻时通常为空

`analysis` 当前已升级为工程第一版可读解读，会尽量反映：

- 卦象概览
- 用神判断
- 关系判断
- 动爻影响
- 风险提示
- 结论建议
- 下一步建议
- 首条可参考资料

`analysisContext` 当前是面向前端和后续分析模块的结构化输入快照：

- `contextVersion`: 当前上下文版本号
- `question / questionCategory`: 当前问题文本和分类
- `useGod`: 当前自动选出的用神
- `mainHexagram / changedHexagram`: 盘面主卦信息
- `ruleCount / ruleCodes`: 当前命中的规则数量和规则编码
- `knowledgeSnippets`: 当前知识召回结果，格式会尽量带上资料来源和章节名
- `chartSnapshot`: 当次分析使用的盘面快照副本，便于前端和后续分析模块直接复用

`structuredResult` 当前包含：

- `score`: 规则累加后的总分
- `resultLevel`: 当前工程评级，`GOOD / NEUTRAL / BAD`
- `effectiveScore`: 冲突裁剪后保留下来的有效评分
- `effectiveResultLevel`: 基于有效评分重新计算的工程评级
- `tags`: 汇总标签
- `effectiveRuleCodes`: 当前裁剪后仍保留为有效信号的规则编码
- `suppressedRuleCodes`: 当前被冲突裁剪压制掉的规则编码
- `summary`: 结构化汇总摘要
- `categorySummaries`: 按规则类别分组后的命中结果
- `conflictSummaries`: 同类正负规则冲突的汇总结果

`structuredResult.categorySummaries[*]`：

- `category`: 规则类别，如 `YONGSHEN_STATE / SHI_YING / MOVING_CHANGE`
- `hitCount`: 当前类别命中的规则数量
- `score`: 当前类别贡献的分数
- `effectiveHitCount`: 冲突裁剪后当前类别保留下来的有效规则数量
- `effectiveScore`: 冲突裁剪后当前类别保留下来的有效分数
- `stageOrder`: 当前类别在规则引擎中的执行阶段顺序

`structuredResult.conflictSummaries[*]`：

- `category`: 发生冲突的规则类别
- `positiveCount / negativeCount`: 当前类别中正向、负向规则数量
- `positiveScore / negativeScore`: 当前类别中正向、负向规则总分
- `netScore`: 冲突抵消后的净分值
- `decision`: 当前最小冲突决策，`POSITIVE_DOMINANT / NEGATIVE_DOMINANT / MIXED`
- `positiveRules / negativeRules`: 分别参与冲突的正向、负向规则编码
- `effectiveRules`: 当前决策下保留为主导信号的规则编码
- `suppressedRules`: 当前决策下被压制的规则编码

## 2. 案例列表

### 请求

- Method: `GET`
- Path: `/api/cases`

### 成功响应

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": [
    {
      "caseId": 12,
      "questionText": "这次合作签约能不能顺利推进",
      "questionCategory": "合作",
      "divinationTime": "2026-04-10T10:00:00",
      "status": "ANALYZED",
      "mainHexagram": "山火贲",
      "changedHexagram": "风山渐",
      "palace": "艮",
      "useGod": "应爻"
    }
  ]
}
```

说明：

- 当前默认返回最近 20 条案例
- 排序方式为 `divinationTime desc, id desc`

## 2.1 规则定义列表

### 请求

- Method: `GET`
- Path: `/api/rules/definitions`

### 成功响应

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "version": "v1",
    "total": 23,
    "rules": [
      {
        "ruleId": "R019",
        "ruleCode": "R019",
        "name": "用神旺且世旺",
        "category": "COMPOSITE",
        "priority": 60,
        "enabled": true,
        "version": "v1",
        "conditionJson": "{\"allOf\":[...]}",
        "effectJson": "{\"score\":3,\"tags\":[\"双强\"]}",
        "description": "用神与世爻都旺，整体偏吉"
      }
    ]
  }
}
```

说明：

- 当前接口返回的是已经同步到数据库的规则定义
- 适合前端调试、规则回放、规则台账核对

## 3. 案例详情

### 请求

- Method: `GET`
- Path: `/api/cases/{caseId}`

### 成功响应

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "caseId": 12,
    "questionText": "这次合作签约能不能顺利推进",
    "questionCategory": "合作",
    "divinationTime": "2026-04-10T10:00:00",
    "status": "ANALYZED",
    "chartSnapshot": {},
    "ruleHits": [],
    "analysis": "当前为骨架版分析结果...",
    "analysisContext": {
      "contextVersion": "v1",
      "question": "这次合作签约能不能顺利推进",
      "questionCategory": "合作",
      "useGod": "应爻",
      "mainHexagram": "山火贲",
      "changedHexagram": "风山渐",
      "ruleCount": 4,
      "ruleCodes": ["USE_GOD_SELECTION", "MOVING_LINE_EXISTS", "SHI_YING_EXISTS", "SHI_YING_RELATION"],
      "knowledgeSnippets": []
    }
  }
}
```

说明：

- `chartSnapshot` 来自已归档的 `chart_json`
- `ruleHits` 来自已归档的规则命中记录
- `analysis` 来自已归档的分析结果
- `analysisContext` 来自归档时保存的结构化分析输入，可用于回放、调试和后续分析升级
- 案例详情中的 `chartSnapshot` 会完整保留接口分析时的上下卦、宫位、历法和六爻明细
- 案例详情中的 `ruleHits.evidence` 与分析接口保持同一结构，便于前端复用展示逻辑

### 未找到案例

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "案例不存在"
}
```

## 4. 案例筛选分页

### 请求

- Method: `GET`
- Path: `/api/cases/search`

查询参数：

- `questionCategory`: 选填，按问题分类筛选，如 `出行 / 合作 / 收入`
- `page`: 选填，页码，从 `1` 开始，默认 `1`
- `size`: 选填，每页条数，默认 `10`，当前最大 `50`

### 成功响应

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "page": 1,
    "size": 5,
    "total": 2,
    "items": [
      {
        "caseId": 8,
        "questionText": "这次出行会不会顺利",
        "questionCategory": "出行",
        "divinationTime": "2026-04-11T10:00:00",
        "status": "ANALYZED",
        "mainHexagram": "山火贲",
        "changedHexagram": "风山渐",
        "palace": "艮",
        "useGod": "父母"
      }
    ]
  }
}
```

说明：

- 当前使用简单分页结构，不引入额外分页对象
- 排序方式仍为 `divinationTime desc, id desc`
- 如果不传 `questionCategory`，会返回全量分页结果

## 5. 健康检查

### CaseCenter

- Method: `GET`
 
## 6. 资料导入准备

### 书目登记

- Method: `POST`
- Path: `/api/books/import-requests`

请求体：

```json
{
  "title": "增删卜易摘录",
  "author": "野鹤老人",
  "sourceType": "DOCX",
  "filePath": "/data/liuyao/books/zengshanbuyi.docx",
  "fileSize": 40960,
  "remark": "首批导入：用神、世应、六亲",
  "topicTags": ["用神", "世应", "六亲"]
}
```

成功响应片段：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "bookId": 1,
    "taskId": 1,
    "parseStatus": "PENDING",
    "taskStatus": "PENDING"
  }
}
```

说明：

- 这一步只负责登记资料文件和创建解析任务
- 真正的切片、抽取、知识入库会沿着 `task` 链继续推进

### 书目列表

- Method: `GET`
- Path: `/api/books`

返回最近 20 条已登记书目，当前主要用于确认资料是否已进入导入台账。

### 文档处理任务列表

- Method: `GET`
- Path: `/api/tasks/doc-process`

返回最近 20 条导入相关任务，当前会包含：

- `taskType`
- `refId`
- `status`
- `retryCount`
- `payloadJson`

### 执行导入任务

- Method: `POST`
- Path: `/api/tasks/doc-process/{taskId}/execute`

成功响应片段：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "taskId": 1,
    "status": "COMPLETED",
    "createdReferenceCount": 2
  }
}
```

说明：

- 当前支持 `TXT` 和可抽取文本的 `PDF`
- 如果 PDF 是扫描版图片，当前不会自动 OCR

### 首批导入主题预览

- Method: `GET`
- Path: `/api/knowledge/import-topics`

返回当前建议优先导入的六爻主题，以及 `book / knowledge / task` 三个模块的职责说明。

### 导入后的知识条目查询

- Method: `GET`
- Path: `/api/knowledge/references`

查询参数：

- `topicTag`: 选填，按主题筛选，如 `用神 / 世应 / 六亲`

成功响应片段：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": {
    "items": [
      {
        "id": 1,
        "bookId": 1,
        "taskId": 1,
        "title": "用神总论",
        "topicTag": "用神",
        "sourceType": "TXT",
        "sourcePage": null,
        "segmentIndex": 1,
        "content": "用神总论\n用神宜旺相，不宜休囚。",
        "keywordSummary": "用神"
      }
    ]
  }
}
```
- Path: `/api/cases/health`

成功返回：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "OK",
  "data": "casecenter-module-ready"
}
```

## 备注

- 当前分析文本仍是骨架版占位实现
- 当前规则是“真实盘面字段 + 启发式规则”的混合阶段
- 历史案例快照读取已兼容旧字段，如 `ext / isMoving / isShi / isYing`
