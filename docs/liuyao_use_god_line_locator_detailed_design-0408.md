# 六爻系统：用神定位模块详细设计文档（可直接交给 Codex / Claude）

版本：v1.0  
定位：面向当前已有排盘、问题意图识别、规则引擎骨架的六爻系统  
用途：用于直接生成代码、拆任务、补测试、做重构  
目标：把“只知道用神类型”升级为“能精确定位本次分析到底看哪一爻作为主用神，并给出候选、证据、回退策略”

---

# 1. 文档目标

本文档专门描述六爻系统中的“用神定位模块（Use God Line Locator）”。

当前很多系统只能做到：

- 根据问题类型知道要看“官鬼 / 妻财 / 父母 / 子孙 / 兄弟 / 世爻”
- 但不知道卦中有多个同类六亲时，到底哪一爻才是当前分析真正的主用神

这会导致三个后果：

1. 规则引擎命中的证据不稳定
2. 分析结果过于抽象，只能停留在“官鬼为用神”
3. 复杂规则（动变、月破、日破、空亡、合冲、入墓等）没有稳定锚点

所以，本模块的目标是：

- 先选出用神类型
- 再从六爻中选出“主用神爻”
- 同时保留候选爻、选择理由、证据、回退策略
- 为规则引擎提供稳定输入

一句话：

用神选择决定“看哪一类”。  
用神定位决定“具体看哪一爻”。

---

# 2. 模块在整体系统中的位置

系统分层如下：

1. 排盘层（Chart / Paipan）
2. 问题意图层（Question Intent）
3. 用神选择层（Use God Type Selection）
4. 用神定位层（Use God Line Locator） ← 本文档重点
5. 规则引擎层（Rule Engine）
6. 推理汇总层（Analysis）
7. 表达层（LLM / Rendering）

主流程：

```text
用户问题
 -> QuestionIntentResolver
 -> UseGodSelector（选出类型）
 -> UseGodLineLocator（选出具体爻）
 -> RuleEngine（围绕主用神执行规则）
 -> AnalysisService
 -> 输出结构化结果
```

---

# 3. 模块职责边界

## 3.1 本模块负责什么

本模块负责：

- 根据已知的 useGodType，从排盘结果中寻找候选爻
- 对候选爻打分排序
- 选出主用神爻
- 输出候选列表、选中理由、打分明细、fallback 信息
- 为后续规则引擎提供 `useGodLineIndex`

## 3.2 本模块不负责什么

本模块不负责：

- 决定问题属于哪种场景
- 决定用神类型本身（这是 UseGodSelector 的职责）
- 直接断吉凶
- 生成自然语言文案
- 执行完整规则引擎

## 3.3 输入前提

进入本模块前，系统已经有：

- ChartSnapshot
- QuestionIntent
- UseGodSelection（类型级）
- lines 的六亲 / 六神 / 世应 / 动爻 / 纳甲 / 月建 / 日辰 / 空亡等基础字段

---

# 4. 核心设计结论

结论先写清楚，便于交给代码模型：

1. 用神定位必须独立成模块，不要散落在 RuleEngine 或 AnalysisService 中
2. 输出必须是结构化结果，不是只返回一个整数
3. 支持多个候选爻
4. 支持场景优先级
5. 支持动爻优先
6. 支持世应关系加权
7. 支持距离加权
8. 支持空亡 / 月破 / 日破 / 旺衰作为评分因素
9. 支持“找不到理想用神爻”时的回退策略
10. 所有选择都必须保留 evidence

---

# 5. 输入模型设计

## 5.1 ChartSnapshot（排盘快照）

建议本模块依赖的最小字段如下：

```json
{
  "mainHexagram": "雷水解",
  "changedHexagram": "风水涣",
  "shiIndex": 3,
  "yingIndex": 6,
  "riChen": {
    "gan": "丙",
    "zhi": "戌",
    "element": "火"
  },
  "yueJian": {
    "zhi": "寅",
    "element": "木"
  },
  "kongWang": ["子", "丑"],
  "lines": [
    {
      "index": 1,
      "relative": "兄弟",
      "sixGod": "青龙",
      "branch": "子",
      "branchElement": "水",
      "isMoving": false,
      "isShi": false,
      "isYing": false,
      "hidden": false
    }
  ]
}
```

## 5.2 QuestionIntent

```json
{
  "questionType": "CAREER",
  "scene": "JOB_INTERVIEW",
  "targetFocus": "OUTCOME",
  "confidence": 0.91
}
```

## 5.3 UseGodSelection（类型级结果）

```json
{
  "useGodType": "OFFICIAL_GHOST",
  "displayName": "官鬼",
  "selectionReason": "问工作、职位、录用结果，取官鬼",
  "fallbacks": ["PARENT", "SELF_LINE"]
}
```

---

# 6. 输出模型设计

本模块输出必须比“一个 lineIndex”丰富得多。

## 6.1 UseGodLineSelectionResult

```json
{
  "useGodType": "OFFICIAL_GHOST",
  "selectedLineIndex": 4,
  "candidateLineIndexes": [4, 6],
  "selectionStrategy": "SCORING",
  "selectionReason": "同类候选中，四爻官鬼发动，且更接近世爻，优先选为主用神",
  "fallbackApplied": false,
  "fallbackStrategy": null,
  "scoreDetails": [
    {
      "lineIndex": 4,
      "totalScore": 9,
      "reasons": [
        {"factor": "MATCH_RELATIVE", "score": 5, "message": "六亲匹配官鬼"},
        {"factor": "IS_MOVING", "score": 2, "message": "发动优先"},
        {"factor": "NEAR_SHI", "score": 1, "message": "更接近世爻"},
        {"factor": "NOT_EMPTY", "score": 1, "message": "不空亡"}
      ]
    },
    {
      "lineIndex": 6,
      "totalScore": 6,
      "reasons": [
        {"factor": "MATCH_RELATIVE", "score": 5, "message": "六亲匹配官鬼"},
        {"factor": "IS_YING", "score": 1, "message": "为应爻"}
      ]
    }
  ],
  "evidence": {
    "scene": "JOB_INTERVIEW",
    "preferredRelative": "官鬼",
    "shiIndex": 3
  }
}
```

## 6.2 输出字段要求

必须包含：

- useGodType
- selectedLineIndex
- candidateLineIndexes
- selectionStrategy
- selectionReason
- fallbackApplied
- fallbackStrategy
- scoreDetails
- evidence

建议额外包含：

- selectedLineSnapshot
- alternativeCandidates
- ignoredCandidates
- version

---

# 7. 领域枚举设计

建议明确以下枚举。

## 7.1 UseGodType

```text
OFFICIAL_GHOST
WIFE_WEALTH
PARENT
CHILDREN
SIBLING
SELF_LINE
TARGET_LINE
```

## 7.2 SelectionStrategy

```text
SINGLE_MATCH
SCORING
SCENE_PRIORITY
FALLBACK
SELF_ONLY
```

## 7.3 ScoringFactor

```text
MATCH_RELATIVE
IS_MOVING
IS_SHI
IS_YING
NEAR_SHI
NEAR_YING
NOT_EMPTY
IS_EMPTY
MONTH_SUPPORT
DAY_SUPPORT
MONTH_BREAK
DAY_BREAK
STRONG_STATE
WEAK_STATE
SCENE_MATCH
HIDDEN_LINE
DISTANT_LINE
FALLBACK_MATCH
```

## 7.4 FallbackStrategyType

```text
USE_FIRST_MATCH
USE_NEAREST_TO_SHI
USE_MOVING_ONE
USE_SHI_LINE
USE_PARENT_AS_BACKUP
USE_NONE
```

---

# 8. 定位逻辑总流程

## 8.1 总体流程

```text
Step1: 接收 chart + questionIntent + useGodSelection
Step2: 根据 useGodType 筛出所有候选爻
Step3: 若只有一个候选 -> 直接选中
Step4: 若多个候选 -> 应用评分模型
Step5: 若无候选 -> 应用 fallback 策略
Step6: 输出主用神爻、候选列表、评分细节、证据
```

## 8.2 伪代码

```python
def locate_use_god_line(chart, intent, use_god_selection):
    candidates = find_candidates(chart, use_god_selection.useGodType)

    if len(candidates) == 1:
        return single_match_result(candidates[0])

    if len(candidates) > 1:
        scored = score_candidates(candidates, chart, intent, use_god_selection)
        best = sort_by_score(scored)[0]
        return scoring_result(best, scored)

    return apply_fallback(chart, intent, use_god_selection)
```

---

# 9. 候选爻筛选规则

## 9.1 基础筛选

先根据用神类型找候选：

- OFFICIAL_GHOST -> relative == 官鬼
- WIFE_WEALTH -> relative == 妻财
- PARENT -> relative == 父母
- CHILDREN -> relative == 子孙
- SIBLING -> relative == 兄弟
- SELF_LINE -> isShi == true

## 9.2 特殊说明

1. 若 useGodType = SELF_LINE，则直接取世爻，不必打分
2. 若某类型全卦没有对应六亲，则进入 fallback
3. 若同类六亲有多个，则必须打分排序，不能随机取第一个

---

# 10. 候选打分模型（一期）

注意：这是工程版评分，不是最终传统体系全量实现。

## 10.1 基础分

### 因子 1：六亲匹配
- 匹配目标六亲：+5

### 因子 2：发动优先
- isMoving = true：+2

### 因子 3：世爻加权
- isShi = true：+2（仅在某些场景允许）
- 距离世爻最近：+1

### 因子 4：应爻加权
- isYing = true：+1（对关系类/对方类问题更重要）

### 因子 5：空亡影响
- 在空亡中：-2
- 不空亡：+1

### 因子 6：月日支持
- 受月建生扶：+1
- 受日辰生扶：+1
- 月破：-2
- 日破：-2

### 因子 7：旺衰状态
- 强：+1
- 弱：-1

### 因子 8：隐藏爻
- hidden = true：-1（一期先降权，不先主选）

### 因子 9：场景匹配
- 场景要求“看对方”且此爻为应：+1
- 场景要求“看结果变化”且此爻发动：+1

## 10.2 距离定义

建议使用爻位距离：

```text
distance = abs(lineIndex - shiIndex)
```

可加规则：

- distance == 0 -> +2
- distance == 1 -> +1
- distance >= 3 -> 0 或 -1

## 10.3 总分公式

```text
totalScore = sum(all factors)
```

一期不必做复杂归一化，直接累加即可。

---

# 11. 场景优先级规则

不同问题场景，对候选爻的优先级不一样。

## 11.1 求职 / 录用 / 职位类

用神类型通常为：官鬼

优先级建议：

1. 发动的官鬼
2. 更接近世爻的官鬼
3. 不空亡、不破的官鬼
4. 应位官鬼（如更代表外部岗位/单位）
5. 其他官鬼

## 11.2 财运 / 收益 / 回款类

用神类型通常为：妻财

优先级建议：

1. 发动的妻财
2. 受生的妻财
3. 更接近世爻的妻财
4. 不空亡、不破的妻财
5. 被兄弟强克的妻财要降权

## 11.3 考试 / 文书 / 证件 / 合同类

用神类型通常为：父母

优先级建议：

1. 发动的父母
2. 不破不空的父母
3. 更接近世爻的父母
4. 能生世的父母
5. 若多个父母，优先结构更稳定者

## 11.4 婚恋 / 关系类

男问女：
- 通常取妻财

女问男：
- 通常取官鬼

优先级建议：

1. 应位候选可加权
2. 发动候选可加权
3. 合世者可加权
4. 冲世者需保留但不一定优先
5. 空亡者降权

## 11.5 健康 / 疾病类

可根据具体系统设定使用官鬼、子孙或世爻辅看。

一期建议：

- 主用神按既定业务映射
- 世爻作为辅助参考，不在本模块直接合并断语

---

# 12. Fallback 回退策略设计

这是必须有的。

## 12.1 为什么必须有 fallback

实际卦里可能出现：

- 全卦没有该类六亲
- 候选全都极弱或空破
- 某些场景结构不典型
- 业务侧想强制得到一个主观察点

## 12.2 回退策略顺序建议

### 对于一般问题

1. 优先取同类候选中得分最高者
2. 若无同类候选，尝试 fallback type
3. 若 fallback type 也无候选，取世爻
4. 若仍失败，返回 null + 明确原因

### 对于 questionType = CAREER

建议 fallback：

1. OFFICIAL_GHOST
2. PARENT
3. SELF_LINE

### 对于 questionType = WEALTH

建议 fallback：

1. WIFE_WEALTH
2. CHILDREN
3. SELF_LINE

### 对于 questionType = EXAM

建议 fallback：

1. PARENT
2. OFFICIAL_GHOST
3. SELF_LINE

## 12.3 输出要求

只要使用 fallback，必须输出：

- fallbackApplied = true
- fallbackStrategy
- originalUseGodType
- actualSelectedType
- fallbackReason

---

# 13. 证据链设计

用神定位模块必须可解释。

## 13.1 每个候选的证据项

建议结构：

```json
{
  "lineIndex": 4,
  "matchedRelative": "官鬼",
  "isMoving": true,
  "isShi": false,
  "isYing": false,
  "distanceToShi": 1,
  "inKongWang": false,
  "monthBreak": false,
  "dayBreak": false,
  "strengthState": "STRONG"
}
```

## 13.2 被选中理由

建议输出一段结构化 summary：

```json
{
  "summary": "四爻官鬼发动，且更接近世爻，不空不破，因此优先选为主用神",
  "highlights": [
    "六亲匹配官鬼",
    "发动",
    "距离世爻近",
    "未空亡"
  ]
}
```

---

# 14. Java 领域对象建议

下面是推荐的 Java 类设计。

## 14.1 UseGodLineLocator

职责：

- 主入口服务
- 协调候选查找、评分、排序、fallback

建议方法：

```java
UseGodLineSelectionResult locate(
    ChartSnapshot chart,
    QuestionIntent intent,
    UseGodSelection useGodSelection
);
```

## 14.2 UseGodCandidateFinder

职责：

- 从 chart 中按 useGodType 找到候选爻

建议方法：

```java
List<UseGodCandidate> findCandidates(
    ChartSnapshot chart,
    UseGodType useGodType
);
```

## 14.3 UseGodCandidateScorer

职责：

- 计算候选得分
- 输出 score details

建议方法：

```java
ScoredUseGodCandidate score(
    UseGodCandidate candidate,
    ChartSnapshot chart,
    QuestionIntent intent,
    UseGodSelection selection
);
```

## 14.4 UseGodFallbackResolver

职责：

- 无候选或需要回退时选择替代策略

建议方法：

```java
UseGodLineSelectionResult resolveFallback(
    ChartSnapshot chart,
    QuestionIntent intent,
    UseGodSelection selection
);
```

## 14.5 UseGodLineSelectionResult

职责：

- 统一返回结果

建议字段：

- useGodType
- selectedLineIndex
- candidateLineIndexes
- selectionStrategy
- selectionReason
- fallbackApplied
- fallbackStrategy
- scoreDetails
- evidence

---

# 15. Java 包结构建议

```text
domain/usegod/
  UseGodSelector.java
  UseGodLineLocator.java
  UseGodCandidateFinder.java
  UseGodCandidateScorer.java
  UseGodFallbackResolver.java
  UseGodSelection.java
  UseGodLineSelectionResult.java
  UseGodCandidate.java
  ScoredUseGodCandidate.java
  SelectionReason.java
  FallbackStrategyType.java
```

---

# 16. 数据结构详细定义

## 16.1 UseGodCandidate

```json
{
  "lineIndex": 4,
  "relative": "官鬼",
  "isMoving": true,
  "isShi": false,
  "isYing": false,
  "branch": "午",
  "element": "火",
  "hidden": false
}
```

## 16.2 ScoredUseGodCandidate

```json
{
  "lineIndex": 4,
  "totalScore": 9,
  "scoreItems": [
    {"factor": "MATCH_RELATIVE", "score": 5, "message": "六亲匹配"},
    {"factor": "IS_MOVING", "score": 2, "message": "发动优先"},
    {"factor": "NEAR_SHI", "score": 1, "message": "距离世爻近"},
    {"factor": "NOT_EMPTY", "score": 1, "message": "未空亡"}
  ],
  "snapshot": {}
}
```

## 16.3 SelectionReason

```json
{
  "summary": "四爻官鬼发动且更接近世爻，优先选为本次分析用神",
  "primaryFactors": ["MATCH_RELATIVE", "IS_MOVING", "NEAR_SHI"],
  "fallbackApplied": false
}
```

---

# 17. 和规则引擎的接口约定

用神定位模块完成后，RuleEngine 应只依赖其结果，不再自己重复选用神。

## 17.1 RuleEvaluationContext 必须新增字段

```json
{
  "useGodType": "OFFICIAL_GHOST",
  "useGodLineIndex": 4,
  "candidateLineIndexes": [4, 6],
  "fallbackApplied": false
}
```

## 17.2 RuleEngine 的职责边界

RuleEngine 可以做：

- 判断 selectedLine 是否空亡
- 判断 selectedLine 是否月破 / 日破
- 判断 selectedLine 是否发动
- 判断 selectedLine 与世爻/应爻的关系

RuleEngine 不应再做：

- 在多个官鬼里重新挑一个
- 因为不满意结果而改选别的候选

---

# 18. 测试设计

这一块非常重要，必须单独设计。

## 18.1 单元测试分类

### A. 候选查找测试

测试内容：

- 根据 useGodType 正确找到所有候选
- SELF_LINE 直接返回世爻
- 没有候选时返回空列表

### B. 单候选直接命中测试

测试内容：

- 全卦只有一个官鬼时，直接选中
- 不需要 fallback
- strategy = SINGLE_MATCH

### C. 多候选评分排序测试

测试内容：

- 多个同类候选时，发动者优先
- 同样发动时，距离世爻更近者优先
- 空亡者降权
- 月破/日破者降权

### D. fallback 测试

测试内容：

- 无目标六亲时使用 fallback
- fallback 到父母
- fallback 到世爻
- fallback 信息写入结果

### E. 场景差异测试

测试内容：

- CAREER 场景优先官鬼
- RELATION 场景应位加权
- WEALTH 场景妻财优先

## 18.2 回归测试建议

至少建立以下案例：

1. 单官鬼卦
2. 双官鬼卦：一个动、一个静
3. 双官鬼卦：一个空、一个实
4. 双妻财卦：一个近世、一个远世
5. 无官鬼卦：career fallback 到父母
6. SELF_LINE 场景
7. relation 场景：应位候选更优
8. exam 场景：父母优先

---

# 19. 数据库 / 配置化建议

一期可以先纯代码实现评分权重，但建议预留配置能力。

## 19.1 场景评分配置示例

```json
{
  "CAREER": {
    "movingBonus": 2,
    "nearShiBonus": 1,
    "yingBonus": 1,
    "kongPenalty": -2,
    "monthBreakPenalty": -2
  },
  "RELATION": {
    "movingBonus": 1,
    "nearShiBonus": 1,
    "yingBonus": 2,
    "kongPenalty": -2
  }
}
```

## 19.2 fallback 配置示例

```json
{
  "CAREER": ["OFFICIAL_GHOST", "PARENT", "SELF_LINE"],
  "WEALTH": ["WIFE_WEALTH", "CHILDREN", "SELF_LINE"],
  "EXAM": ["PARENT", "OFFICIAL_GHOST", "SELF_LINE"]
}
```

---

# 20. 给 Codex / Claude 的实现要求

下面这部分可以直接贴给代码模型。

## 20.1 实现目标

请在现有六爻项目基础上实现“用神定位模块”，要求：

1. 输入为 ChartSnapshot + QuestionIntent + UseGodSelection
2. 输出为结构化 UseGodLineSelectionResult
3. 支持单候选、多个候选、无候选 fallback
4. 支持评分明细
5. 支持候选证据输出
6. RuleEngine 后续只使用 selectedLineIndex，不自行重新选用神

## 20.2 必须实现的类

- UseGodLineLocator
- UseGodCandidateFinder
- UseGodCandidateScorer
- UseGodFallbackResolver
- UseGodLineSelectionResult
- UseGodCandidate
- ScoredUseGodCandidate
- SelectionReason

## 20.3 必须实现的测试

- UseGodCandidateFinderTest
- UseGodCandidateScorerTest
- UseGodFallbackResolverTest
- UseGodLineLocatorTest
- UseGodLineLocatorRegressionTest

## 20.4 明确禁止

- 禁止把定位逻辑写进 AnalysisService
- 禁止 RuleEngine 自己重新选择用神
- 禁止只返回 lineIndex 而不保留 reason/evidence
- 禁止在 controller 中出现定位逻辑

---

# 21. 交付物清单

本模块完成后，至少应交付：

1. 用神定位主服务
2. 候选查找组件
3. 候选评分组件
4. fallback 解析组件
5. 结构化返回对象
6. 至少 8 组单元测试
7. 至少 8 条回归案例
8. 与 RuleEngine 的接口接通
9. 主流程响应中暴露 `useGodLineIndex`

---

# 22. API 输出建议

分析接口返回建议包含：

```json
{
  "chart": {},
  "intent": {},
  "useGodSelection": {
    "useGodType": "OFFICIAL_GHOST",
    "displayName": "官鬼"
  },
  "useGodLineSelection": {
    "selectedLineIndex": 4,
    "candidateLineIndexes": [4, 6],
    "selectionReason": "四爻官鬼发动且更近世爻，优先选中",
    "fallbackApplied": false,
    "scoreDetails": []
  },
  "analysisResult": {}
}
```

---

# 23. 开发顺序建议

按下面顺序做，不要跳：

## 阶段 A
1. 固定返回模型
2. 实现候选查找
3. 实现单候选命中逻辑

## 阶段 B
4. 实现评分模型
5. 实现多候选排序
6. 实现证据输出

## 阶段 C
7. 实现 fallback 策略
8. 接入 RuleEvaluationContext
9. 改造 RuleEngine 使用 selectedLineIndex

## 阶段 D
10. 补回归测试
11. 在 Analysis 接口中暴露结果
12. 形成可审计日志

---

# 24. 本阶段验收标准

只有满足以下条件，才算用神定位模块完成：

1. 系统不再只知道“看官鬼”，而能明确“看第四爻官鬼”
2. 多候选时有稳定排序结果
3. 无候选时有明确 fallback
4. 每次选择都有 reason 和 evidence
5. RuleEngine 不再自己二次选用神
6. 测试可覆盖单候选 / 多候选 / fallback / 场景差异
7. 主流程返回中包含 `useGodLineIndex`

---

# 25. 最终总结

用神模块分两层：

第一层：UseGodSelector  
回答的是：这次该看哪一类六亲。

第二层：UseGodLineLocator  
回答的是：这次具体看哪一爻。

没有第二层，规则引擎就会一直漂在空中。  
补上第二层后，系统才真正有了稳定的“主观察点”。

一句话总结：

- 用神类型 = 目标类别
- 用神定位 = 目标实例
- 规则引擎 = 围绕目标实例做判断
- 分析服务 = 把结构化判断拼成结果

本模块是你当前系统最值得优先补完的一块。

---
