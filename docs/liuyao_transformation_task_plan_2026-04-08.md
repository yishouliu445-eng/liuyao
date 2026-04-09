# 	六爻系统改造任务表（2026-04-08）

基于文档：

- `docs/liuyao_gap_tasks-2026-04-08.md`
- `docs/liuyao_use_god_line_locator_detailed_design-0408.md`

基于现状代码核对：

- `liuyao-app` 已有排盘、用神类型选择、规则引擎、分析服务、casecenter 留痕
- `liuyao-worker` 已有书籍切片、embedding、入库能力

---

## 一、结论

### 当前整体进度（截至 2026-04-09）

- `P0` 可视为基本完成，主用神定位、规则上下文、规则 matcher、首批配置规则、分析服务小步拆分、关键回归测试都已落地
- `P1` 不只进入启动阶段，而是已经完成了三项核心落地
  - `T1-1` 只读 replay 已完成
  - `T1-2` replay 持久化历史已完成，并已超出“是否值得持久化”的评估阶段
  - `T1-3` 规则版本治理已完成最小闭环，并已把 metadata 接入运行时
- 当前最主要的未完成部分已不在 replay / version governance，而是在 `P2` 的“知识转规则”链路

### 当前阶段判断

- 这份计划已经不再处于“做主链路”的早期阶段
- 更准确地说，当前状态是：
  - `P0 基本收口`
  - `P1 第一阶段已完成`
  - 下一阶段重点应转向 `T2-1 / T2-2`

当前系统不是从零开始，而是已经完成了骨架期。

现在最应该优先补齐的，不是继续铺更多外围能力，而是先把以下主链路闭环：

1. 用神类型 -> 主用神爻定位
2. 主用神爻 -> 规则上下文事实
3. 规则上下文事实 -> 配置化主轴规则
4. 规则结果 -> 稳定分析输出
5. 稳定分析输出 -> 案例回放验证

一句话说，优先级应当是：

`主用神锚点精确化 > 规则密度补齐 > 分析服务拆分 > 案例回归 > 知识转规则`

---

## 二、现状判断

### 2.1 已有能力

- 排盘与快照模型已具备
- 问题意图识别与用神类型选择已具备
- Java 规则 + JSON 规则混合引擎已具备
- 分析文本组装已具备
- 案例留痕已具备
- 文档切片与知识入库已具备

### 2.2 当前最大缺口

当前系统最大缺口不是“没有规则引擎”，而是：

- 已能选出“看哪一类用神”
- 但还不能稳定选出“具体看哪一爻”

这会直接导致：

- `RuleEvaluationContext` 的核心事实不稳定
- 同类多爻时实际判断锚点漂移
- `应爻/世爻` 类型可以被选中，但共享定位链路并未真正支持
- 规则证据无法形成统一的“为何选这一爻”的结构化结果

---

## 三、改造原则

本次改造建议遵循以下原则：

- 优先做最小闭环，不做大拆大建
- 保留现有“Java 产事实，JSON 吃事实”的规则结构
- 尽量复用现有 `casecenter` 与 `worker`，不另起一套系统
- `AnalysisService` 先小步拆分，不做大规模重构
- replay 先做只读对比，再决定是否落库

---

## 四、P0 任务：本周必须完成

## T0-1 用神定位结果模型升级

### 目标

把当前“只有类型级结果”的 `UseGodSelection`，升级为“类型 + 主用神爻 + 候选 + 评分 + fallback + evidence”的结果模型。

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodSelection.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/ChartSnapshot.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodSelector.java`

### 主要改造点

- 保留现有 `intent/useGod/priority/scenario/note/configVersion`
- 新增或扩展以下字段
  - `selectedLineIndex`
  - `candidateLineIndexes`
  - `selectionStrategy`
  - `selectionReason`
  - `fallbackApplied`
  - `fallbackStrategy`
  - `scoreDetails`
  - `evidence`

### 验收标准

- 主流程能拿到稳定的 `useGodLineIndex`
- 不再依赖字符串匹配后 `findFirst()`
- `应爻/世爻` 类型在模型层可被统一表达

### 依赖

- 无，作为后续所有任务前置

---

## T0-2 实现真正的 UseGodLineLocator

### 目标

把当前偏工具类的 `UseGodLineLocator` 升级为真正的“定位模块”。

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodLineLocator.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodSelector.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodRule.java`

### 主要改造点

- 支持按 `UseGodType` 找候选，而不是只按 `liuQin` 字符串找
- 支持 `SELF_LINE`、`YING`、六亲类目标统一进入同一定位链路
- 支持如下基础策略
  - `SINGLE_MATCH`
  - `SCORING`
  - `FALLBACK`
  - `SELF_ONLY`
- 支持如下一期评分因子
  - 六亲匹配
  - 是否发动
  - 是否世爻/应爻
  - 距世距离
  - 是否空亡
  - 是否月破/日破
  - 旺衰加权
  - 隐藏爻降权
  - 场景加权

### 验收标准

- 同类多爻时不再随机取第一条
- `应爻/世爻` 类目标能被定位
- 输出候选、主选、评分、fallback、evidence

### 依赖

- 依赖 `T0-1`

---

## T0-3 RuleEvaluationContext 围绕主用神爻重建

### 目标

把规则上下文从“同类六亲第一个匹配项”改成“主选用神爻事实”。

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEvaluationContext.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEngineService.java`

### 主要改造点

- `useGodLineIndex` 基于定位结果，而非 `findFirst()`
- 所有用神相关事实统一从主用神爻推导
- 新增推荐上下文字段
  - `USE_GOD_MOVING`
  - `USE_GOD_LINE_COUNT`
  - `USE_GOD_EMPTY`
  - `USE_GOD_MONTH_BREAK`
  - `USE_GOD_DAY_BREAK`
  - `USE_GOD_BEST_SCORE`
  - `HAS_MOVING_SHENG_USE_GOD`
  - `HAS_MOVING_KE_USE_GOD`
  - `HAS_CHANGED_SHENG_USE_GOD`
  - `HAS_CHANGED_KE_USE_GOD`
  - `USE_GOD_SELF_TRANSFORM`
  - `SHI_YING_EXISTS`
- 视规则需要补充
  - `YING_MOVING`
  - `YING_EMPTY`
  - `USE_GOD_DISTANCE_TO_SHI`

### 验收标准

- 新上下文字段可被规则层稳定读取
- 用神相关规则不再各自重复找目标爻

### 依赖

- 依赖 `T0-1`
- 依赖 `T0-2`

---

## T0-4 精修 RuleMatcher，不重写架构

### 目标

保持现有“Java 规则产事实，JSON 规则消费事实”的结构，只补 matcher 能力，不引入表达式引擎。

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleMatcher.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/rule/definition/RuleCondition.java`

### 主要改造点

- 扩展 `readValue()` 支持新 target
- 如有需要，仅补少量 operator
  - `GREATER_THAN_OR_EQUALS`
  - `LESS_THAN_OR_EQUALS`
  - `CONTAINS`

### 验收标准

- 新增规则不需要写回 Java if/else
- 现有 `allOf/anyOf/noneOf` 行为不被破坏

### 依赖

- 依赖 `T0-3`

---

## T0-5 第一批主轴规则补齐

### 目标

先补第一批最有价值的 15 到 20 条规则，不建议一口气上 30 条。

### 主要文件

- `liuyao-app/src/main/resources/rules/rule_definitions.json`

### 第一批优先规则

#### 用神相关

- 用神生世
- 用神克世
- 用神合世
- 用神空亡
- 用神月破
- 用神日破
- 用神动化进
- 用神动化退
- 动爻生用神
- 动爻克用神

#### 世爻相关

- 世爻旺
- 世爻弱
- 世爻受生
- 世爻受克

#### 世应/结构相关

- 世应存在
- 世应相克
- 应爻生世
- 应爻克世
- 多动爻扰动
- 主用神距世较近

### 验收标准

- 规则 evidence 输出格式统一
- 配置规则可以直接建立在 `RuleEvaluationContext` 事实上
- 尽量不新增“写死在 Java 里的结论规则”

### 依赖

- 依赖 `T0-3`
- 依赖 `T0-4`

---

## T0-6 AnalysisService 小步拆分

### 目标

在不破坏现有输出和接口的情况下，让 `AnalysisService` 从超大类变成“薄 facade + 小组件协作”。

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/dto/AnalysisContextDTO.java`

### 推荐拆分顺序

#### 第一步

抽出 `AnalysisKnowledgeEvidenceService`

负责：

- 知识片段筛选
- 优先级评分
- 规则码偏好
- 引用裁剪
- 证据拼装

#### 第二步

抽出 `AnalysisCategoryTextResolver`

负责：

- 不同问题类别的文案分支
- 风险提示文案
- 下一步建议文案
- 分类观察 lead

#### 可选第三步

抽出 `AnalysisContextFactory`

用于消除 `AnalysisService` 和 `DivinationService` 之间的上下文组装重复。

### 验收标准

- 对外接口不变
- 现有 `AnalysisServiceTest` 通过
- 分析输出文本不出现明显回归

### 当前完成态

- `AnalysisKnowledgeEvidenceService` 已拆出，负责知识片段筛选、规则码偏好、引用裁剪与证据拼装
- `AnalysisCategoryTextResolver` 已拆出，负责类别 lead 与类别摘要文案
- `AnalysisOutcomeTextResolver` 已拆出，负责风险提示、下一步建议、结论方向文案
- `AnalysisSectionComposer` 已拆出，负责分析文本各 section 的组装顺序与拼接
- `AnalysisContextFactory` 已拆出，负责复用 `AnalysisContextDTO` 的核心字段组装，减少分析主链与 replay 链路重复
- `AnalysisService` 已收敛为薄 facade，保留入口与 `AnalysisContextDTO` 组装，不改变对外接口

### 实现落点

- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisKnowledgeEvidenceService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisCategoryTextResolver.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisOutcomeTextResolver.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisSectionComposer.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisContextFactory.java`

### 当前判断

- `T0-6` 已进入“可认为阶段性完成”的状态
- 现阶段不建议继续大拆 `AnalysisService`，优先保持 facade 入口稳定
- 若后续继续推进，优先级更高的是继续观察 `DivinationService` / replay 链路中剩余 DTO 转换是否值得再抽一层 mapper，而不是继续拆 `AnalysisService`

### 依赖

- 可与 `T0-4/T0-5` 并行推进

---

## T0-7 测试补齐

### 目标

为本次改造补足关键回归测试，避免“看起来能跑，实际锚点飘”。

### 主要文件

- `liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodFallbackTest.java`
- `liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodRuleTest.java`
- `liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleMatcherTest.java`
- `liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleEngineRealChartRegressionTest.java`
- `liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/AnalysisServiceTest.java`

### 必补测试

- `应爻/世爻` 类型定位
- 同类六亲多候选评分
- “类型已选中但无匹配爻” fallback
- 新增 context 字段 derivation
- 新 operator 行为
- 新 JSON 规则加载与命中
- analysis facade 回归

### 验收标准

- 至少有一组真实盘面或手工快照覆盖多候选定位
- 至少有一组场景覆盖 fallback

### 依赖

- 贯穿所有 P0 任务

---

## 五、P1 任务：下一阶段

## T1-1 先做只读 Case Replay

### 目标

在不新增 replay 表的前提下，先让系统能够拿历史 case 重跑，并输出差异结果。

### 当前完成态（2026-04-09）

- 已支持从已存 `chart_json` 反序列化后重跑当前规则与分析链路
- 已支持查看单 case 在当前规则下的 replay 结果
- 已返回规则 diff、有效规则 diff、压制规则 diff、分数变化、结果等级变化、摘要变化、分析变化
- 已返回 baseline / replay 两侧的结构化结果、分析上下文、规则命中详情
- 已返回当前规则 bundle / 规则定义 / 用神配置版本信息
- 已补充只读 replay assessment 列表，用于先验证 replay 的使用价值和字段结构

### 当前实现落点

- service / controller
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/CaseCenterService.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/controller/CaseCenterController.java`
- DTO
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayAssessmentDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayAssessmentListDTO.java`

### 当前接口

- `GET /api/cases/{caseId}/replay`
- `GET /api/cases/replay-assessments`

### 主要文件

- `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/CaseCenterService.java`
- `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/controller/CaseCenterController.java`

### 主要改造点

- 从已存 `chart_json` 反序列化为 `ChartSnapshot`
- 重跑
  - `RuleEngineService`
  - `KnowledgeSearchService`
  - `AnalysisService`
- 返回以下 diff
  - 新增规则
  - 消失规则
  - 分数变化
  - 结果等级变化
  - 分析摘要变化

### 验收标准

- 不必先落库
- 先能完成“查看某 case 在当前规则下会变成什么结果”

### 当前判断

- 这一步已经完成，并且比原始目标多走了一步
  - 已把 replay 结果的版本信息显式化
  - 已把 replay assessment 做成只读列表
- 后续 replay 相关工作不再需要回到“能否只读 replay”的问题，而是围绕持久化治理和使用方式继续推进

### 依赖

- 建议在 P0 完成后进行

---

## T1-2 Replay 持久化历史

### 目标

只有在只读 replay 足够稳定后，才决定是否引入 replay 结果持久化。

### 当前完成态（2026-04-09）

- 已采用“新增 replay run 表”的最小方案落地，而不是扩展 `case_analysis_result`
- 已支持把单次 replay 结果持久化为 `case_replay_run`
- 已支持按单 case 查看 replay run 历史
- 已支持按 `questionCategory`、`recommendPersistReplay` 分页筛选 replay run
- 已支持 replay run 统计视图
  - 总 run 数
  - 建议持久化数
  - 观察型数
  - 按分类聚合分布

### 当前实现落点

- migration
  - `liuyao-app/src/main/resources/db/migration/V11__create_case_replay_run_table.sql`
  - `liuyao-app/src/main/resources/db/migration/V12__add_case_replay_run_case_summary.sql`
- service / controller
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/service/CaseCenterService.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/controller/CaseCenterController.java`
- DTO / repository
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayRunDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayRunListDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayRunStatsDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/dto/CaseReplayRunCategoryStatsDTO.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/casecenter/repository/CaseReplayRunRepository.java`

### 当前接口

- `POST /api/cases/{caseId}/replay-runs`
- `GET /api/cases/{caseId}/replay-runs`
- `GET /api/cases/replay-runs/search`
- `GET /api/cases/replay-runs/stats`

### 可选方向

- 扩展 `case_analysis_result`
- 或新增 replay run 表

### 原则

- 不建议提前建很多 replay 表
- 先验证 replay 的使用价值和字段结构

### 当前判断

- 这一步已经证明 replay history 有实际使用价值，后续不需要再讨论“是否要有 replay run”
- 下一阶段重点不再是建表，而是治理
  - replay run 保留周期
  - 是否需要异步批量 replay
  - 是否需要把统计口径沉到专门查询层
  - 是否需要把 `recommendPersistReplay` 进一步升级为人工确认状态

---

## T1-3 规则版本治理

### 目标

把目前难维护的 zip 规则资源逐步替换为版本化 JSON 目录结构。

### 当前完成态（2026-04-09）

- 已完成从单文件/zip 维护心智切换到版本化目录治理
- 运行时资源已按 `rules/v1/` 加载
- `rule_definitions.json` 已升级为 manifest，用于聚合多个分类规则文件
- `use_god_rules.json` 已纳入同一版本目录治理
- 已新增 `metadata.json`，并接入运行时 metadata loader
- replay / replay run 已能返回规则 bundle、规则定义、用神配置版本
- 根目录 `rules/v1/` 已作为治理镜像存在，便于目录化维护与对照

### 当前实现落点

- runtime resources
  - `liuyao-app/src/main/resources/rules/v1/`
- governance mirror
  - `rules/v1/`
  - `rules/README.md`
- loader / metadata
  - `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleDefinitionConfigLoader.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodRuleConfigLoader.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleResourceMetadataLoader.java`
  - `liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleResourceMetadata.java`
- config
  - `liuyao-app/src/main/resources/application.yml`

### 当前资源结构

```text
rules/
  v1/
    metadata.json
    rule_definitions.json
    yongshen_rules.json
    shi_rules.json
    moving_rules.json
    composite_rules.json
    use_god_rules.json
```

### 主要目录

- `rules/`
- `liuyao-app/src/main/resources/rules/`

### 推荐结构

```text
rules/
  v1/
    yongshen_rules.json
    shi_rules.json
    moving_rules.json
    composite_rules.json
```

### 验收标准

- 至少支持按版本目录加载
- zip 不再作为主维护形式

### 当前判断

- 这一步已经达到文档里的最小闭环，并且比最初规划更完整
  - 不只是目录化
  - 还把版本元信息接入了运行时
- 后续如果继续推进，重点将是多版本并存治理，而不是继续纠结目录结构本身

---

## 六、P2 任务：后续升级

## T2-1 书籍知识 -> 规则候选提取

### 目标

把现有 worker 从“只切片存储”，升级成“能提规则候选”。

### 主要文件

- `liuyao-worker/app/pipeline/book_pipeline.py`
- `liuyao-worker/app/chunker/classic_text_chunker.py`
- `liuyao-worker/app/task_runner/worker.py`
- `liuyao-worker/app/db/repositories.py`

### 主要改造点

- 增加新的 task type
- 针对 `book_chunk` 中规则型片段做结构提取
- 输出候选规则 JSON，包含
  - 规则标题
  - 适用主题
  - 条件描述
  - 影响方向
  - 来源书籍
  - 原文证据
  - 置信度

### 验收标准

- 候选规则可人工审核
- 不直接写入正式 `rule_definition`

---

## T2-2 候选规则审核与晋升

### 目标

建立 `rule_candidate -> review -> promote -> rule_definition` 的闭环。

### 主要改造点

- 新增 candidate 存储结构
- 增加审核状态
- 复用现有 `rule_definition` 能力，不另建平行正式规则库

### 验收标准

- 规则入库前必须可审查
- 正式规则来源可追溯

---

## 七、推荐执行顺序

建议按以下顺序推进：

1. `T0-1` 用神定位结果模型升级
2. `T0-2` 实现真正的 UseGodLineLocator
3. `T0-3` RuleEvaluationContext 围绕主用神爻重建
4. `T0-4` 精修 RuleMatcher
5. `T0-5` 第一批主轴规则补齐
6. `T0-6` AnalysisService 小步拆分
7. `T0-7` 测试补齐
8. `T1-1` 只读 Case Replay
9. `T1-3` 规则版本治理
10. `T2-1` 规则候选提取
11. `T2-2` 候选规则审核晋升

---

## 八、建议提交粒度

建议不要把所有任务混成一次提交，推荐按以下粒度拆分：

1. `feat: add use-god line selection result model`
2. `feat: implement use-god line locator with scoring and fallback`
3. `refactor: rebuild rule evaluation context around selected use-god line`
4. `feat: add first batch of configured yongshen and shi rules`
5. `refactor: split analysis service into knowledge and category helpers`
6. `test: add use-god locator and regression coverage`
7. `feat: add case replay diff endpoint`
8. `refactor: version rule resource layout`
9. `feat: extract rule candidates from book chunks`

---

## 九、最终建议

如果只能抓一个重点，那就优先把下面这条链路做稳：

`UseGodSelector -> UseGodLineLocator -> RuleEvaluationContext -> rule_definitions.json`

因为只要这个链路稳定了：

- 规则可以继续堆
- 分析解释会更具体
- replay 才有意义
- 知识转规则才不会失焦

当前系统最缺的不是“再多几个功能”，而是：

- 主用神锚点
- 规则事实密度
- 可回放验证能力
