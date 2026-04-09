# Liuyao Transformation Week 1 Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在一周内完成六爻系统第一阶段改造闭环，让“用神类型选择”升级为“主用神爻定位 + 规则上下文稳定事实 + 第一批配置规则 + 可维护分析服务”。

**Architecture:** 保留现有 `UseGodSelector -> UseGodRule -> RuleEngineService -> AnalysisService` 主链路，不重写规则引擎，不引入新框架。核心策略是先让 Java 层稳定产出“主用神爻事实”，再让 JSON 规则基于这些事实扩展判断，最后对分析服务做小步拆分，确保每一步都可以独立验证和回归。

**Tech Stack:** Java 17, Spring Boot 3.3, Maven, JUnit 5, 现有 `liuyao-app` rule/usegod/analysis 模块

---

## Chunk 1: 用神定位结果建模

### Task 1: 先补定位结果与 fallback 的失败测试

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodFallbackTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodRuleTest.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodLineLocatorTest.java`

- [ ] **Step 1: 写 `应爻/世爻` 可被定位的失败测试**

```java
@Test
void shouldResolveYingLineWhenUseGodTypeIsYING() {
    // 构造含 ying=6 的 chartSnapshot
    // 断言定位结果 selectedLineIndex == 6
}
```

- [ ] **Step 2: 写同类多候选六亲的评分/排序失败测试**

```java
@Test
void shouldPreferMovingOfficialGhostWhenMultipleCandidatesExist() {
    // 两个官鬼候选，其中一条发动且更接近世
    // 断言选中发动候选，并返回 candidates/scoreDetails
}
```

- [ ] **Step 3: 写“类型命中但无匹配爻” fallback 失败测试**

```java
@Test
void shouldFallbackToShiLineWhenSelectedTypeHasNoMatchingLine() {
    // 断言 fallbackApplied = true
}
```

- [ ] **Step 4: 运行专项测试确认先失败**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=UseGodFallbackTest,UseGodRuleTest,UseGodLineLocatorTest test
```

Expected:

- 失败点集中在“无统一定位结果模型”和“无应爻/世爻统一定位能力”

### Task 2: 扩展 UseGodSelection 结果模型

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodSelection.java`

- [ ] **Step 1: 保留现有字段**
- [ ] **Step 2: 增加定位结果字段**
- [ ] **Step 3: 保持旧调用点最小改动**

建议新增字段：

```java
private final Integer selectedLineIndex;
private final List<Integer> candidateLineIndexes;
private final String selectionStrategy;
private final String selectionReason;
private final Boolean fallbackApplied;
private final String fallbackStrategy;
private final List<Map<String, Object>> scoreDetails;
private final Map<String, Object> evidence;
```

- [ ] **Step 4: 调整构造器与 getter**

### Task 3: 让 UseGodSelector 返回完整选择结果

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodSelector.java`

- [ ] **Step 1: 先保持 intent/type 选择逻辑不变**
- [ ] **Step 2: 在 selector 边界调用 locator**
- [ ] **Step 3: unknown intent 仍保留人工判断兜底**

- [ ] **Step 4: 运行专项测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=UseGodFallbackTest,UseGodRuleTest,UseGodLineLocatorTest test
```

Expected:

- 新测试通过
- 老的 unknown fallback 行为不回归

### Task 4: 提交本 Chunk

**Files:**
- Stage only usegod result model + tests

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: add use-god line selection result model"
```

---

## Chunk 2: 实现 UseGodLineLocator 与共享定位链路

### Task 5: 重构 UseGodLineLocator 成真正定位模块

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodLineLocator.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodRule.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodLineLocatorTest.java`

- [ ] **Step 1: 提取按 `UseGodType` 找候选的方法**

```java
List<LineInfo> findCandidates(ChartSnapshot chart, UseGodType useGodType)
```

- [ ] **Step 2: 实现 `SELF_LINE` / `YING` / 六亲类型统一定位**
- [ ] **Step 3: 实现一期评分模型**

评分建议：

```text
MATCH_RELATIVE +5
IS_MOVING +2
IS_SHI +2
IS_YING +1
NEAR_SHI +1
NOT_EMPTY +1
IS_EMPTY -2
MONTH_BREAK -2
DAY_BREAK -2
HIDDEN_LINE -1
```

- [ ] **Step 4: 实现 `SINGLE_MATCH / SCORING / FALLBACK / SELF_ONLY`**
- [ ] **Step 5: 输出统一 evidence 与 scoreDetails**

### Task 6: 让 UseGodRule 写入完整定位证据

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/UseGodRule.java`

- [ ] **Step 1: 继续写 `chart.useGod` 以兼容旧链路**
- [ ] **Step 2: 同时写入 `selectedLineIndex` 等结构化 evidence**
- [ ] **Step 3: 避免在 `ext` 中散落太多重复字段**

### Task 7: 运行定位与规则回归

**Files:**
- Test only

- [ ] **Step 1: 运行 `UseGodLineLocatorTest,UseGodRuleTest`**
- [ ] **Step 2: 运行首批 use-god batch rule 测试**

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=UseGodLineLocatorTest,UseGodRuleTest,UseGodStrengthRuleTest,UseGodDayBreakRuleTest,UseGodMonthBreakRuleTest,MovingLineAffectUseGodRuleTest test
```

Expected:

- 旧规则可继续命中
- 不再依赖字符串 `findFirst()`

### Task 8: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: implement use-god line locator with scoring and fallback"
```

---

## Chunk 3: 围绕主用神爻重建 RuleEvaluationContext

### Task 9: 先写 context derivation 失败测试

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleEvaluationContextTest.java`

- [ ] **Step 1: 写主用神事实 derivation 测试**
- [ ] **Step 2: 写 moving/shi/ying 相关布尔字段测试**
- [ ] **Step 3: 写 best-score 和 line-count 测试**

### Task 10: 扩展 RuleEvaluationContext

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEvaluationContext.java`

- [ ] **Step 1: 改为消费 `UseGodSelection` 或其稳定结果**
- [ ] **Step 2: 基于主选爻推导统一上下文字段**

推荐新增字段：

```java
private Boolean useGodMoving;
private Integer useGodLineCount;
private Boolean useGodEmpty;
private Boolean useGodMonthBreak;
private Boolean useGodDayBreak;
private Integer useGodBestScore;
private Boolean hasMovingShengUseGod;
private Boolean hasMovingKeUseGod;
private Boolean hasChangedShengUseGod;
private Boolean hasChangedKeUseGod;
private String useGodSelfTransform;
private Boolean shiYingExists;
private Integer useGodDistanceToShi;
```

- [ ] **Step 3: 保留旧字段兼容现有规则**

### Task 11: 让 RuleEngineService 透传新增事实

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEngineService.java`

- [ ] **Step 1: 把新 context 字段加到 configured rule evidence**
- [ ] **Step 2: 确保日志与旧行为兼容**

### Task 12: 运行规则上下文与场景回归

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=RuleEvaluationContextTest,RuleEngineScenarioRegressionTest,RuleEngineRealChartRegressionTest test
```

Expected:

- 场景回归不退化
- 新 context 字段 derivation 正确

### Task 13: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "refactor: rebuild rule evaluation context around selected use-god line"
```

---

## Chunk 4: 精修 RuleMatcher 并补第一批配置规则

### Task 14: 先补 matcher operator/target 测试

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleMatcherTest.java`

- [ ] **Step 1: 为新 target 写读取测试**
- [ ] **Step 2: 为新 operator 写单测**

### Task 15: 精修 RuleMatcher

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleMatcher.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/definition/RuleCondition.java`

- [ ] **Step 1: 扩展 `readValue()`**
- [ ] **Step 2: 如需要只补 `GREATER_THAN_OR_EQUALS / LESS_THAN_OR_EQUALS / CONTAINS`**
- [ ] **Step 3: 不引入表达式求值器**

### Task 16: 补第一批 rule_definitions

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/rule_definitions.json`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleDefinitionConfigLoaderTest.java`

- [ ] **Step 1: 先补用神与世关系规则**
- [ ] **Step 2: 再补 moving/composite 规则**
- [ ] **Step 3: 补 loader 解析测试**

第一批建议规则：

```text
用神生世
用神克世
用神合世
用神空亡
用神月破
用神日破
用神动化进
用神动化退
动爻生用神
动爻克用神
世爻旺
世爻弱
世爻受生
世爻受克
世应存在
世应相克
应爻生世
应爻克世
多动爻扰动
主用神距世较近
```

### Task 17: 跑配置规则专项与真实盘回归

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=RuleMatcherTest,RuleDefinitionConfigLoaderTest,RuleEngineScenarioRegressionTest,RuleEngineRealChartRegressionTest test
```

Expected:

- 新规则可解析、可命中
- 真实盘回归不出现明显退化

### Task 18: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "feat: add first batch of configured yongshen and shi rules"
```

---

## Chunk 5: AnalysisService 小步拆分

### Task 19: 抽离知识证据选择组件

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisKnowledgeEvidenceService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/AnalysisKnowledgeEvidenceServiceTest.java`

- [ ] **Step 1: 先写知识片段筛选单测**
- [ ] **Step 2: 抽出知识片段选择与裁剪逻辑**
- [ ] **Step 3: 保持 AnalysisService facade 不变**

### Task 20: 抽离分类文案解析组件

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisCategoryTextResolver.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/AnalysisCategoryTextResolverTest.java`

- [ ] **Step 1: 抽出分类分支 switch**
- [ ] **Step 2: 抽出 risk/action/direction 文案**
- [ ] **Step 3: 让 AnalysisService 只负责 section 组装**

### Task 21: 跑分析回归

Run:

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=AnalysisKnowledgeEvidenceServiceTest,AnalysisCategoryTextResolverTest,AnalysisServiceTest test
```

Expected:

- 新组件单测通过
- 旧的 `AnalysisServiceTest` 继续通过

### Task 22: 提交本 Chunk

- [ ] **Step 1: `git add` 本 chunk 相关文件**
- [ ] **Step 2: 提交**

```bash
git commit -m "refactor: split analysis service into knowledge and category helpers"
```

---

## Chunk 6: 第一周收尾回归

### Task 23: 跑本周专项回归

**Files:**
- Test only

- [ ] **Step 1: 运行 usegod/rule/analysis 相关专项**

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=UseGodFallbackTest,UseGodRuleTest,UseGodLineLocatorTest,RuleEvaluationContextTest,RuleMatcherTest,RuleDefinitionConfigLoaderTest,AnalysisServiceTest test
```

- [ ] **Step 2: 运行场景与真实盘回归**

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn -q -Dtest=RuleEngineScenarioRegressionTest,RuleEngineRealChartRegressionTest test
```

- [ ] **Step 3: 如时间允许跑全量测试**

```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app && mvn test -q
```

Expected:

- P0 范围内测试全部通过
- 若全量测试失败，应记录失败项是否与本次改造无关

### Task 24: 更新文档与下一周入口

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/docs/liuyao_transformation_task_plan_2026-04-08.md`

- [ ] **Step 1: 勾掉本周已完成项**
- [ ] **Step 2: 把下周入口聚焦到 `T1-1 Case Replay`**

---

## Week 1 Exit Criteria

本周完成的判定标准：

- `UseGodSelection` 已能表达主用神爻结果
- `UseGodLineLocator` 已支持六亲类、世爻、应爻统一定位
- `RuleEvaluationContext` 不再使用同类六亲 `findFirst()`
- 第一批配置规则可消费新的用神事实
- `AnalysisService` 至少拆出 2 个小组件
- 关键专项与场景回归通过

---

## Out of Scope for Week 1

以下内容本周不做，避免任务扩散：

- Case replay 持久化历史
- `rules/` zip 资源全面迁移
- worker 侧 rule candidate extraction
- 候选规则审核后台
- LLM 表达层增强
