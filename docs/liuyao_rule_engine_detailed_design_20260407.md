# 六爻系统工程设计文档（Rule Engine 优先版）

版本：v1.0  
用途：可直接交给 Codex / Claude 用于生成代码、拆任务、补测试、做重构  
适用对象：当前已有 Java 主系统、准备把六爻项目从“排盘工具”升级为“规则推理系统”的开发者

---

# 1. 文档目标

本文档用于明确六爻系统当前阶段的开发优先级、模块边界、数据结构、规则引擎设计、排盘冻结策略、任务拆分方式，以及可直接交给代码生成模型执行的实现要求。

这不是概念介绍文档，而是面向落地开发的工程文档。

---

# 2. 项目当前判断

根据现有项目状态，可以认为：

1. 你已经有了排盘主流程和系统骨架。
2. 当前最缺的不是“再做一个更花的排盘”，而是“规则引擎”和“可解释推理”。
3. 如果继续把主要精力放在排盘功能扩展上，项目会停留在“高级排盘工具”层。
4. 如果现在切到规则层，你的项目会进入“六爻推理系统”的轨道。

因此，本阶段总原则如下：

- 保留现有排盘实现，不推倒重来
- 不整库搬运开源项目
- 排盘只补正确性与测试，不再无限扩功能
- 主开发重心切换到规则一期
- 所有分析结论必须来自结构化规则，不允许写死断语

---

# 3. 本阶段总目标

本阶段只做三件核心事：

1. 把排盘层冻结为“够用且稳定”的基础设施
2. 建立规则引擎一期
3. 打通“排盘 -> 用神 -> 规则命中 -> 推理结果 -> 可解释输出”的闭环

不做的内容：

- 不追求一次性做完《增删卜易》全量规则
- 不追求一次性做大师级断卦
- 不优先做复杂前端
- 不优先做 LLM 文风润色
- 不优先做资料大规模自动抽取后的高阶推理

---

# 4. 系统分层设计

建议把整个系统明确拆为五层：

## 4.1 排盘层（Paipan Engine）

职责：

- 接收起卦输入
- 计算本卦、变卦
- 计算世应、六神、纳甲、六亲
- 计算月建、日辰、空亡
- 输出标准化结构

产出：

- 结构化排盘 JSON
- 不负责吉凶判断
- 不负责自然语言解释

## 4.2 数据层（Data Layer）

职责：

- 保存排盘快照
- 保存案例
- 保存书籍切片
- 保存规则定义
- 保存分析结果和命中规则

## 4.3 规则层（Rule Engine）

职责：

- 读取结构化规则
- 基于排盘结果判断规则是否命中
- 输出规则命中列表、分数、标签、证据

这是本阶段的重点。

## 4.4 推理层（Reasoning Layer）

职责：

- 组织规则执行顺序
- 做冲突处理
- 汇总最终评分
- 生成结构化分析结论

## 4.5 表达层（LLM / Rendering Layer）

职责：

- 将结构化分析结果转成自然语言
- 负责“怎么说”
- 不负责“判断本身”

当前阶段可先不重点开发。

---

# 5. 当前阶段的优先级排序

优先级必须明确，避免模型乱扩展：

1. 规则引擎
2. 用神选择模块
3. 排盘正确性测试与冻结
4. 分析结果可解释输出
5. 案例管理与规则回归
6. LLM 表达层
7. 更多起卦方式和界面体验

原则：

- 规则优先于界面
- 正确性优先于功能数量
- 可解释优先于文风华丽
- 系统闭环优先于功能堆砌

---

# 6. 排盘层策略：保留、自测、冻结

## 6.1 结论

排盘层不要推倒重做，也不要把未来 2~4 周主要时间用来深挖排盘功能。

理由：

- 你已经有自己的系统骨架和接口结构
- 排盘不是主要壁垒
- 开源项目可以作为对照源，而不是主干替代物
- 真正决定项目上限的是规则和推理，而不是排盘界面和输入方式

## 6.2 排盘层的阶段目标

排盘只需要做到“够用 + 稳定 + 可测”。

达到以下标准即可冻结：

1. 手工六爻输入稳定可用
2. 本卦/变卦计算稳定
3. 世应定位稳定
4. 六亲、六神、纳甲计算稳定
5. 月建、日辰、空亡稳定输出
6. 输出结构固定
7. 有回归测试样本

## 6.3 排盘冻结前必须补完的 10 个校验点

### 校验点 1：64 卦编码映射完整性

要求：

- 校验 64 卦编码表是否完整
- 输入任意合法六爻组合，都能映射到本卦
- 不允许出现未知卦编码

### 校验点 2：动爻生成变卦正确性

要求：

- 单动爻、双动爻、多动爻都能正确翻转
- 变卦结果稳定
- 对同一输入重复执行结果一致

### 校验点 3：世应定位正确性

要求：

- 每个卦的世应位置可稳定计算
- 有标准样例校验
- 输出中明确标记 isShi / isYing

### 校验点 4：六亲计算正确性

要求：

- 以宫位五行为基准计算六亲
- 对每爻都能得出六亲
- 不允许空值或非法值

### 校验点 5：六神计算正确性

要求：

- 按日干顺序稳定排六神
- 每条爻输出六神字段
- 有样例回归

### 校验点 6：纳甲映射正确性

要求：

- 每卦每爻纳甲结果固定
- 干支输出必须结构化
- 不允许纯文本拼接后难以复用

### 校验点 7：月建输出正确性

要求：

- 月建结构化输出
- 至少包含月支、月五行、月旺衰参考信息

### 校验点 8：日辰输出正确性

要求：

- 日辰结构化输出
- 至少包含日干、日支、五行

### 校验点 9：空亡计算正确性

要求：

- 空亡必须来自真实历法字段
- 输出可用于规则判断
- 不允许只展示、不参与计算

### 校验点 10：标准 JSON 稳定性

要求：

- 同一输入永远得到相同结构
- 字段命名统一
- 后续规则层只依赖此结构，不直接依赖 UI 字段

## 6.4 排盘冻结后的原则

排盘冻结后：

- 不再大规模改接口
- 不轻易修改字段含义
- 新增字段必须向后兼容
- 排盘模块只接受 bugfix 和必要映射补完
- 不允许为了方便某条规则临时乱加特殊字段

---

# 7. 是否参考开源项目

## 7.1 总策略

参考，不整库搬。

## 7.2 应该借鉴的内容

可以参考开源项目的：

- 多种起卦方式
- 历法计算边界
- 映射表
- 某些排盘显示细节
- 特殊输入处理

## 7.3 不应该直接照搬的内容

不建议直接搬的内容：

- 主体工程结构
- 分析逻辑
- 领域模型命名
- 结果返回格式
- 杂糅式脚本代码

## 7.4 推荐的使用方式

开源项目作为：

- 结果对照器
- 样例来源
- 局部逻辑参考
- 回归校验参考

不要作为：

- 你系统的主干代码来源
- 你的架构设计依据
- 你的推理规则来源

---

# 8. 规则在整个项目中的位置

把《增删卜易》拆成机器规则，属于以下步骤：

- 规则抽取（Rule Extraction）
- 规则结构化（Rule Structuring）
- 规则执行（Rule Execution）
- 推理汇总（Reasoning Aggregation）

它不是“后期锦上添花”，而是系统核心。

工程上可以理解为：

- 排盘层负责“把卦排出来”
- 规则层负责“判断哪些现象成立”
- 推理层负责“综合现象得出结论”
- LLM 层负责“把结论说成人话”

因此规则不是以后做，而是现在就要开始。

---

# 9. 本阶段的最小可用闭环

本阶段必须先做出下面这个闭环：

1. 用户提交问题和起卦输入
2. 系统完成排盘
3. 系统根据问题识别意图
4. 系统确定用神
5. 规则引擎执行
6. 输出命中规则列表
7. 输出结构化分析结果
8. 可选：再由 LLM 生成自然语言解释

如果没有第 4~7 步，系统仍然只是排盘工具。

---

# 10. 领域模型设计

以下模型建议优先明确并稳定下来。

## 10.1 DivinationRequest

用于表示一次起卦请求。

```json
{
  "question": "我这次面试能过吗",
  "questionType": "CAREER",
  "inputMode": "MANUAL_LINES",
  "lines": [1, 0, 1, 1, 0, 0],
  "movingLines": [2, 5],
  "timestamp": "2026-04-07T17:00:00"
}
```

建议字段：

- question
- questionType
- inputMode
- lines
- movingLines
- timestamp
- userId（如有）
- source（手动、接口、导入等）

## 10.2 ChartSnapshot

用于表示排盘后的标准结构。

```json
{
  "mainHexagram": "雷水解",
  "mainHexagramCode": "001101",
  "changedHexagram": "风水涣",
  "changedHexagramCode": "110101",
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
  "shiIndex": 3,
  "yingIndex": 6,
  "lines": [
    {
      "index": 1,
      "yinYang": "YANG",
      "isMoving": false,
      "naJiaGan": "壬",
      "naJiaZhi": "子",
      "branchElement": "水",
      "relative": "兄弟",
      "sixGod": "青龙",
      "isShi": false,
      "isYing": false
    }
  ]
}
```

## 10.3 QuestionIntent

用于表示问题意图。

```json
{
  "questionType": "CAREER",
  "scene": "JOB_INTERVIEW",
  "targetFocus": "OUTCOME",
  "confidence": 0.92
}
```

## 10.4 UseGodSelection

用于表示用神选择结果。

```json
{
  "questionType": "CAREER",
  "useGodType": "OFFICIAL_GHOST",
  "displayName": "官鬼",
  "selectionReason": "问工作、职位、录用结果，优先取官鬼为用神",
  "fallbacks": ["PARENT", "SELF_LINE"]
}
```

## 10.5 RuleDefinition

用于表示一条规则定义。

```json
{
  "id": "R001",
  "name": "用神被冲",
  "category": "YONGSHEN_STATE",
  "priority": 100,
  "enabled": true,
  "condition": {
    "target": "YONGSHEN",
    "operator": "HAS_RELATION",
    "value": "CHONG"
  },
  "effect": {
    "score": -2,
    "tags": ["不稳定"],
    "conclusionHints": ["用神受冲，事情波动较大"]
  },
  "description": "用神被冲，主不稳"
}
```

## 10.6 RuleHit

用于表示某条规则命中结果。

```json
{
  "ruleId": "R001",
  "ruleName": "用神被冲",
  "hit": true,
  "scoreDelta": -2,
  "tags": ["不稳定"],
  "evidence": {
    "targetLineIndex": 4,
    "targetRelative": "官鬼",
    "matchedRelation": "CHONG"
  },
  "message": "官鬼为用神，当前被冲，主不稳"
}
```

## 10.7 AnalysisResult

用于表示一次结构化分析结果。

```json
{
  "requestId": "xxx",
  "questionType": "CAREER",
  "useGodType": "OFFICIAL_GHOST",
  "score": -1,
  "resultLevel": "NEUTRAL_TO_BAD",
  "tags": ["波动", "压力"],
  "ruleHits": [],
  "analysisContext": {
    "shiState": "weak",
    "yongshenState": "chong",
    "movingCount": 2
  },
  "summary": "总体偏不稳，结果存在反复",
  "generatedAt": "2026-04-07T17:10:00"
}
```

---

# 11. 用神模块设计

用神模块是规则引擎的入口，不应被省略。

## 11.1 为什么优先做用神

因为六爻的绝大多数后续判断，都依赖“你到底在看哪一爻、哪一类六亲”。

如果没有用神，后续规则会失去主目标。

## 11.2 用神模块的职责

- 接收 QuestionIntent
- 根据问题类别确定用神类型
- 结合卦中对应六亲定位用神爻
- 若有多个候选，按优先级和规则选择主用神
- 若没有明显用神，给出回退策略

## 11.3 用神选择的一期范围

只做最常见问题：

- 工作 / 求职
- 财运 / 求财
- 婚姻 / 感情
- 学业 / 考试
- 健康 / 疾病
- 出行
- 官司
- 房屋 / 文书 / 合同

## 11.4 建议的用神映射表（第一版）

- 问工作、职位、录用：官鬼
- 问财运、收益、钱：妻财
- 问考试、证书、学习成果：父母
- 问婚恋对象（男问女）：妻财
- 问婚恋对象（女问男）：官鬼
- 问子女、快乐、解除忧患：子孙
- 问自己状态：世爻
- 问文书、合同、学历、证件：父母
- 问竞争、同辈、合作分利：兄弟

## 11.5 输出要求

用神模块输出至少包含：

- useGodType
- targetLineIndex
- candidateLineIndexes
- selectionReason
- fallbackStrategy

---

# 12. 规则引擎设计

## 12.1 原则

所有规则必须：

- 结构化
- 可配置
- 可开关
- 可排序
- 可测试
- 可解释

禁止：

- 在业务层随意写死“如果官鬼被冲就返回失败”
- 在控制器里拼分析文案
- 在规则之外直接下结论

## 12.2 规则分类建议

规则可以分为以下几类：

1. 用神状态类
2. 世爻状态类
3. 世应用类
4. 动爻变化类
5. 空亡类
6. 月日旺衰类
7. 多条件综合类
8. 场景专用类

## 12.3 规则字段建议

每条规则建议包含：

- id
- name
- category
- priority
- enabled
- version
- condition
- effect
- description
- examples
- createdBy
- updatedAt

## 12.4 执行顺序建议

建议按以下顺序执行：

1. 用神状态
2. 世爻状态
3. 世应关系
4. 动爻与变爻
5. 空亡
6. 旺衰
7. 综合规则
8. 场景加权规则

---

# 13. 规则条件表达建议

为了避免早期 DSL 太重，一期可用简化条件表达模型。

## 13.1 基础条件类型

建议支持以下 operator：

- EQUALS
- NOT_EQUALS
- IN
- NOT_IN
- GREATER_THAN
- LESS_THAN
- HAS_RELATION
- IS_EMPTY
- NOT_EMPTY
- IS_TRUE
- IS_FALSE

## 13.2 目标 target 建议

一期支持以下 target：

- YONGSHEN
- SHI
- YING
- MOVING_COUNT
- QUESTION_TYPE
- MAIN_HEXAGRAM
- CHANGED_HEXAGRAM
- KONG_WANG
- LINE_RELATIVE
- LINE_ELEMENT
- SHI_YING_RELATION

## 13.3 组合条件

支持：

- allOf
- anyOf
- noneOf

示例：

```json
{
  "allOf": [
    {"target": "YONGSHEN", "operator": "HAS_RELATION", "value": "CHONG"},
    {"target": "SHI", "operator": "EQUALS", "value": "WEAK"}
  ]
}
```

---

# 14. 规则一期：建议先做的 20 条规则

注意：这是一期，不是终局版。目标是先把系统闭环跑起来。

## 14.1 用神状态类

### R001 用神被冲
- 效果：-2
- 标签：不稳

### R002 用神被克
- 效果：-2
- 标签：受制

### R003 用神生世
- 效果：+2
- 标签：有利

### R004 用神合世
- 效果：+1
- 标签：有接近、有机会

### R005 用神空亡
- 效果：-2
- 标签：落空、虚

### R006 用神发动
- 效果：+1
- 标签：有变化

## 14.2 世爻状态类

### R007 世爻旺
- 效果：+2
- 标签：自身有力

### R008 世爻弱
- 效果：-2
- 标签：自身不足

### R009 世爻被克
- 效果：-2
- 标签：压力

### R010 世爻发动
- 效果：+1
- 标签：主动变化

### R011 世爻空亡
- 效果：-2
- 标签：心虚、落空

## 14.3 世应关系类

### R012 世应相生
- 效果：+2
- 标签：互动有利

### R013 世应相克
- 效果：-2
- 标签：对立

### R014 应爻生世
- 效果：+2
- 标签：外部助力

### R015 应爻克世
- 效果：-2
- 标签：外部压制

## 14.4 动爻与变化类

### R016 多动爻（>3）
- 效果：-1
- 标签：局势混乱

### R017 用神动而生世
- 效果：+3
- 标签：主动来助

### R018 用神动而化退
- 效果：-2
- 标签：后劲不足

## 14.5 综合类

### R019 用神旺且世旺
- 效果：+3
- 标签：双强

### R020 用神弱且世弱
- 效果：-3
- 标签：双弱

---

# 15. 规则 JSON 示例

以下为建议格式。

## 15.1 单条件规则

```json
{
  "id": "R001",
  "name": "用神被冲",
  "category": "YONGSHEN_STATE",
  "priority": 100,
  "enabled": true,
  "condition": {
    "target": "YONGSHEN",
    "operator": "HAS_RELATION",
    "value": "CHONG"
  },
  "effect": {
    "score": -2,
    "tags": ["不稳"],
    "conclusionHints": ["用神受冲，事情不稳"]
  },
  "description": "用神被冲，主不稳定"
}
```

## 15.2 多条件规则

```json
{
  "id": "R019",
  "name": "用神旺且世旺",
  "category": "COMPOSITE",
  "priority": 60,
  "enabled": true,
  "condition": {
    "allOf": [
      {"target": "YONGSHEN", "operator": "EQUALS", "value": "STRONG"},
      {"target": "SHI", "operator": "EQUALS", "value": "STRONG"}
    ]
  },
  "effect": {
    "score": 3,
    "tags": ["双强"],
    "conclusionHints": ["主事目标与自身状态均较有力"]
  },
  "description": "用神与世爻都旺，整体偏吉"
}
```

---

# 16. 规则执行流程

建议按如下流程实现：

## 16.1 总流程

1. 接收 DivinationRequest
2. 执行排盘，生成 ChartSnapshot
3. 识别问题意图，生成 QuestionIntent
4. 选择用神，生成 UseGodSelection
5. 构建 RuleEvaluationContext
6. 读取启用中的规则
7. 逐条执行规则匹配
8. 记录 RuleHit
9. 汇总得分、标签、证据
10. 生成 AnalysisResult

## 16.2 RuleEvaluationContext 建议

```json
{
  "questionType": "CAREER",
  "useGodType": "OFFICIAL_GHOST",
  "useGodLineIndex": 4,
  "yongshenState": ["CHONG", "MOVING"],
  "shiState": ["WEAK"],
  "shiYingRelation": "KE",
  "movingCount": 2,
  "kongWangBranches": ["子", "丑"]
}
```

## 16.3 结果汇总建议

评分可以先采用简单累加模型：

- score > 2: 偏吉
- -2 <= score <= 2: 中性/有反复
- score < -2: 偏凶

注意：这只是一期工程评分，不代表传统六爻完整体系。

---

# 17. 可解释输出要求

每条命中规则都必须保留证据，不允许只返回分数。

至少要输出：

- 规则 ID
- 规则名称
- 命中原因
- 目标爻
- 对应字段
- 分数变化
- 标签

示例：

```json
{
  "ruleId": "R001",
  "ruleName": "用神被冲",
  "message": "官鬼为用神，位于四爻，当前受冲，事情波动较大",
  "evidence": {
    "targetLineIndex": 4,
    "relative": "官鬼",
    "relation": "CHONG"
  },
  "scoreDelta": -2
}
```

---

# 18. 数据库存储建议

## 18.1 tables 建议

### divination_record
保存原始请求与排盘结果

建议字段：

- id
- question
- question_type
- input_mode
- request_json
- chart_json
- created_at

### rule_definition
保存规则定义

建议字段：

- id
- rule_code
- name
- category
- priority
- enabled
- version
- condition_json
- effect_json
- description
- created_at
- updated_at

### analysis_result
保存分析结果

建议字段：

- id
- divination_id
- use_god_type
- score
- result_level
- summary
- result_json
- created_at

### analysis_rule_hit
保存规则命中记录

建议字段：

- id
- analysis_result_id
- rule_code
- score_delta
- hit_json
- created_at

### knowledge_slice
保存书籍切片与案例切片

建议字段：

- id
- source_name
- source_type
- topic
- content
- tags
- metadata_json
- created_at

---

# 19. 包结构建议

如果你当前是 Java 主项目，可考虑如下结构：

```text
liuyao-app/
  src/main/java/.../
    controller/
    application/
    domain/
      divination/
      chart/
      intent/
      usegod/
      rule/
      analysis/
    infrastructure/
      persistence/
      config/
      external/
    shared/
```

进一步细化：

```text
domain/divination/
  DivinationRequest.java
  DivinationRecord.java

domain/chart/
  ChartSnapshot.java
  LineInfo.java
  ChartBuilderService.java

domain/intent/
  QuestionIntent.java
  QuestionIntentResolver.java

domain/usegod/
  UseGodType.java
  UseGodSelection.java
  UseGodSelector.java

domain/rule/
  RuleDefinition.java
  RuleCondition.java
  RuleEffect.java
  RuleEngine.java
  RuleMatcher.java
  RuleRepository.java
  RuleHit.java

domain/analysis/
  AnalysisResult.java
  AnalysisService.java
  AnalysisAssembler.java
```

---

# 20. 代码生成模型的实现要求

下面这部分可以直接贴给 Codex / Claude。

## 20.1 总体要求

- 使用现有排盘结构，不要推翻主干
- 不要把规则硬编码在 controller
- 所有规则定义从 JSON 或数据库加载
- 所有规则执行结果要保留 evidence
- 先实现一期规则，不要尝试一次做完整传统体系
- 代码必须可测试
- 规则层和排盘层解耦
- RuleEngine 不直接依赖 UI DTO

## 20.2 必须完成的类

- QuestionIntentResolver
- UseGodSelector
- RuleDefinition
- RuleCondition
- RuleEffect
- RuleMatcher
- RuleEngine
- RuleHit
- RuleEvaluationContext
- AnalysisService
- AnalysisResult

## 20.3 必须完成的测试

- 64 卦映射测试
- 动爻变卦测试
- 世应定位测试
- 六亲计算测试
- 六神计算测试
- 空亡计算测试
- 用神选择测试
- 单规则命中测试
- 多规则组合测试
- 回归样例测试

## 20.4 禁止事项

- 禁止在 controller 里写分析逻辑
- 禁止返回纯文本而不保留结构化分析结果
- 禁止为了赶进度把规则写成一堆 if/else 散落在业务层
- 禁止把自然语言生成和规则判断耦合在一起

---

# 21. 开发顺序建议

按下面顺序做，不要乱跳。

## 阶段 A：排盘冻结

1. 固定 ChartSnapshot 结构
2. 完成 10 个排盘校验点
3. 建立 20~30 个标准样例回归测试
4. 冻结排盘接口

## 阶段 B：用神一期

1. 设计 QuestionIntent 枚举
2. 建立问题类型到用神类型的映射
3. 实现 UseGodSelector
4. 完成常见场景测试

## 阶段 C：规则引擎一期

1. 定义 RuleDefinition / RuleCondition / RuleEffect
2. 实现 RuleEvaluationContext
3. 实现 RuleMatcher
4. 实现 RuleEngine
5. 导入首批 20 条规则
6. 完成规则单测与组合测试

## 阶段 D：分析结果组装

1. 实现 AnalysisService
2. 汇总 score / tags / ruleHits / summary
3. 返回结构化结果
4. 保存 analysis_result 与 analysis_rule_hit

## 阶段 E：可解释输出和 LLM

1. 将结构化结果转为 prompt 输入
2. 让 LLM 只负责润色和表达
3. 保留结构化结果为真值来源

---

# 22. 交付物清单

这一阶段完成后，应至少交付：

1. 稳定的 ChartSnapshot
2. 排盘回归测试集
3. QuestionIntentResolver
4. UseGodSelector
5. RuleDefinition JSON
6. RuleEngine
7. RuleHit 结构
8. AnalysisResult 结构
9. 最小闭环接口
10. 至少 5 个真实案例回归

---

# 23. 最小 API 建议

## 23.1 排盘接口

`POST /api/divination/chart`

输入：

```json
{
  "question": "我这次面试能过吗",
  "inputMode": "MANUAL_LINES",
  "lines": [1, 0, 1, 1, 0, 0],
  "movingLines": [2, 5],
  "timestamp": "2026-04-07T17:00:00"
}
```

输出：

```json
{
  "chart": {}
}
```

## 23.2 分析接口

`POST /api/divination/analyze`

输出建议包含：

```json
{
  "chart": {},
  "intent": {},
  "useGodSelection": {},
  "analysisResult": {}
}
```

---

# 24. 给 Codex / Claude 的任务拆分模板

下面这段可以直接复制给代码模型。

## 任务 1：排盘冻结与测试

请基于现有项目完成以下工作：

1. 固定 ChartSnapshot 和 LineInfo 结构
2. 检查并补齐本卦/变卦/世应/六亲/六神/纳甲/月建/日辰/空亡字段
3. 为以下内容编写单元测试和回归测试：
   - 64 卦编码映射
   - 动爻生成变卦
   - 世应定位
   - 六亲计算
   - 六神计算
   - 空亡计算
4. 不要新增复杂新功能，只做正确性和稳定性收口

## 任务 2：实现问题意图与用神选择

请实现：

- QuestionIntent 枚举和对象
- QuestionIntentResolver
- UseGodType 枚举
- UseGodSelection 对象
- UseGodSelector 服务

要求：

- 先支持工作、财运、婚恋、考试、健康、合同、房屋、出行八类问题
- 输出结构化用神结果
- 为每一类问题写单元测试

## 任务 3：实现规则引擎一期

请实现：

- RuleDefinition
- RuleCondition
- RuleEffect
- RuleEvaluationContext
- RuleMatcher
- RuleEngine
- RuleHit

要求：

- 规则定义从 JSON 加载
- 支持单条件和 allOf 条件
- 支持 score/tags/conclusionHints
- 输出命中证据
- 不允许把规则散落在业务层 if/else 中

## 任务 4：实现分析服务

请实现：

- AnalysisService
- AnalysisResult
- AnalysisAssembler

要求：

- 输入为 DivinationRequest 或 ChartSnapshot + QuestionIntent
- 输出为结构化分析结果
- 汇总 score、tags、ruleHits、analysisContext
- 保留证据链
- 先不要做复杂文案生成

---

# 25. 本阶段验收标准

只有同时满足以下条件，才算这一阶段完成：

1. 排盘结果结构稳定
2. 排盘回归测试通过
3. 问题意图可识别
4. 用神可结构化输出
5. 至少 20 条规则可执行
6. 分析结果可返回规则命中列表
7. 分析结果可保留 evidence
8. 真实案例可复跑
9. 修改规则不会破坏排盘
10. 代码生成模型补代码后，人工能看懂并继续维护

---

# 26. 最终结论

你现在不是要继续做“另一个六爻排盘工具”。

你现在要做的是：

- 以你现有排盘为底座
- 尽快冻结排盘边界
- 立刻进入用神与规则阶段
- 构建一个可解释、可测试、可扩展的六爻推理系统

一句话总结：

排盘是输入基础设施。  
规则是核心大脑。  
推理是系统价值。  
LLM 只是表达层。

本阶段的正确方向不是“继续深挖排盘功能”，而是“让系统第一次真正会判断”。

---
