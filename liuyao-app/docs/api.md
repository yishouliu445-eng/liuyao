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
- `questionCategory`: 选填，问题分类，当前支持如 `收入 / 感情 / 健康 / 出行 / 合作`
- `divinationMethod`: 选填，起卦方式说明
- `divinationTime`: 必填，起卦时间
- `rawLines`: 选填，六爻原始输入，推荐传 6 项
- `movingLines`: 选填，动爻序号列表

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
      "mainHexagram": "火水未济",
      "changedHexagram": "雷水解",
      "mainHexagramCode": "101010",
      "changedHexagramCode": "100010",
      "palace": "离",
      "palaceWuXing": "火",
      "shi": 3,
      "ying": 6,
      "useGod": "父母",
      "riChen": "甲子",
      "yueJian": "辰",
      "kongWang": ["戌", "亥"],
      "lines": []
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
    "analysis": "当前为骨架版分析结果..."
  }
}
```

### 关键返回字段

`chartSnapshot`:

- `mainHexagram / changedHexagram`: 本卦、变卦名称
- `mainHexagramCode / changedHexagramCode`: 六位卦码
- `palace / palaceWuXing`: 卦宫与宫五行
- `shi / ying`: 世应位置
- `useGod`: 当前自动选择的用神
- `riChen / yueJian / kongWang`: 历法字段
- `lines`: 六爻结构明细

`ruleHits`:

- `ruleCode`: 规则编码
- `ruleName`: 规则名称
- `hitReason`: 命中原因
- `impactLevel`: 影响等级
- `evidence`: 结构化证据

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
      "mainHexagram": "火水未济",
      "changedHexagram": "雷水解",
      "palace": "离",
      "useGod": "应爻"
    }
  ]
}
```

说明：

- 当前默认返回最近 20 条案例
- 排序方式为 `divinationTime desc, id desc`

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
    "analysis": "当前为骨架版分析结果..."
  }
}
```

说明：

- `chartSnapshot` 来自已归档的 `chart_json`
- `ruleHits` 来自已归档的规则命中记录
- `analysis` 来自已归档的分析结果

### 未找到案例

```json
{
  "success": false,
  "code": "NOT_FOUND",
  "message": "案例不存在"
}
```

## 4. 健康检查

### CaseCenter

- Method: `GET`
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
