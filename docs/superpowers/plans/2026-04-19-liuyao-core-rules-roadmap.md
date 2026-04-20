# Liuyao Core Rules Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the next stage of the six-yao core engine by hardening the existing plate-calculation baseline, then filling the highest-value missing rule families that directly affect judgment quality.

**Architecture:** Keep the current layering unchanged: `divination` computes a stable chart snapshot, `rule` evaluates deterministic rule hits, `analysis` turns those hits into explanation text, and `knowledge` supplies citations and snippets. Do not mix “book knowledge import” with “rule execution” until the core rule semantics are stable.

**Tech Stack:** Spring Boot, Java rule engine, JSON-configured rule definitions, Python worker for book parsing/chunking, Maven/JUnit.

---

## Scope

This plan only covers the six-yao core and only the rule/logic side.

Included:
- 本卦/变卦
- 纳甲
- 六亲
- 六神
- 世应
- 动爻与变爻
- 用神选择与强弱
- 月破 / 日破 / 旬空
- 伏神 / 飞神
- 知识库导入主题扩展

Excluded from the first implementation pass:
- 八字 / 奇门 / 罗盘
- 页面布局
- 神煞全量体系
- 互卦 / 错卦 / 综卦
- 大而全的“应期系统”

## Current Baseline

Already implemented in first version:
- README confirms the current baseline includes real calendar fields plus first-version `本卦/变卦、世应、六神、纳甲、六亲` and part of the use-god/structure rules: [README.md](/Users/liuyishou/wordspace/liuyao/liuyao-app/README.md:64)
- Chart assembly entry: [ChartBuilderService.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ChartBuilderService.java:43)
- Calendar fields: [CalendarFacade.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/CalendarFacade.java:15)
- Existing resolver entry points: [NaJiaResolver.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/NaJiaResolver.java:9), [LiuQinResolver.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/LiuQinResolver.java:6), [LiuShenResolver.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/LiuShenResolver.java:8), [ShiYingResolver.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ShiYingResolver.java:10)
- Existing rule engine entry: [RuleEngineService.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/service/RuleEngineService.java:51)
- Existing use-god and moving-rule core: [UseGodLineLocator.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodLineLocator.java:20), [UseGodStrengthRule.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodStrengthRule.java:20), [MovingLineAffectUseGodRule.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/MovingLineAffectUseGodRule.java:19), [ShiYingRelationRule.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced/ShiYingRelationRule.java:20)
- Existing rule configuration bundles: [rules/v1](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1)
- Existing knowledge import topics are still narrow: [KnowledgeImportService.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeImportService.java:32), [KnowledgeSearchService.java](/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java:69)

## Priority Order

1. Stabilize the plate-calculation baseline with regression coverage.
2. Upgrade use-god / moving-change / empty-break semantics from first-version heuristics into a denser state system.
3. Add `伏神 / 飞神`, because this is the largest missing family that materially changes interpretation.
4. Expand knowledge-import topics to match the new rule families.
5. Leave `反吟 / 伏吟 / 互卦 / 错卦 / 综卦 / 神煞` for phase two after the core is stable.

## Chunk 1: Baseline Plate Correctness

### Task 1: Freeze And Verify The Plate Baseline

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ChartBuilderService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/NaJiaResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/LiuQinResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/LiuShenResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ShiYingResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/CalendarFacade.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/NaJiaResolverTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/LiuQinResolverTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/LiuShenResolverTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/ShiYingResolverTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/ChartBuilderRegressionTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/ChartBuilderLinePatternRegressionTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/ChartBuilderServiceCalendarTest.java`

- [ ] Add or expand regression samples that lock expected output for `本卦/变卦/世应/纳甲/六亲/六神/旬空`.
- [ ] Verify whether `LiuShenResolver`'s current “简化表” is acceptable for the intended lineage; if not, replace it before adding more rules.
- [ ] Verify that `ChartBuilderService` always emits enough line-level detail for later rules and remove placeholder semantics only after tests are in place.
- [ ] Run the baseline divination tests.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=NaJiaResolverTest,LiuQinResolverTest,LiuShenResolverTest,ShiYingResolverTest,ChartBuilderRegressionTest,ChartBuilderLinePatternRegressionTest,ChartBuilderServiceCalendarTest test
```

Expected:
- All plate-related tests pass.
- No silent fallback remains for cases that should be deterministically derived.

Acceptance:
- We can trust the chart snapshot as a stable foundation.
- No new rule work starts until this baseline is green.

## Chunk 2: Core Rule Density

### Task 2: Upgrade Use-God Selection And State Evaluation

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodLineLocator.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodStrengthRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced/UseGodMonthBreakRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/UseGodDayBreakRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced/UseGodEmptyRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/yongshen_rules.json`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/use_god_rules.json`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/UseGodLineLocatorTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/batch/UseGodStrengthRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/advanced/UseGodMonthBreakRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/batch/UseGodDayBreakRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/advanced/UseGodEmptyRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleEngineScenarioRegressionTest.java`

- [ ] Replace ad-hoc scoring with explicit state evidence such as `旺 / 相 / 休 / 囚 / 破 / 空 / 动 / 变`.
- [ ] Make the selected use-god line explainable and reproducible from evidence, not just from a final score.
- [ ] Ensure `月破 / 日破 / 旬空` are represented as composable state flags rather than isolated one-off deductions.
- [ ] Add scenario regression tests for job, money, relationship, travel, and health questions.
- [ ] Keep rule semantics deterministic and separate from the AI explanation layer.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=UseGodLineLocatorTest,UseGodStrengthRuleTest,UseGodMonthBreakRuleTest,UseGodDayBreakRuleTest,UseGodEmptyRuleTest,RuleEngineScenarioRegressionTest test
```

Expected:
- Existing scenarios still pass.
- Each scenario can show exactly why the use-god was chosen and why it is judged strong or weak.

Acceptance:
- The rule engine can express the main judgment backbone without relying on vague prompt text.

### Task 3: Deepen Moving-Change And Shi-Ying Interpretation

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/batch/MovingLineAffectUseGodRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced/ShiYingRelationRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/moving_rules.json`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/shi_rules.json`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/composite_rules.json`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/batch/MovingLineAffectUseGodRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/advanced/ShiYingRelationRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleEngineScenarioRegressionTest.java`

- [ ] Expand moving-line semantics from only `生 / 克 / 同五行` to include same-line transformation, retreat/advance, and direct interference with `世` or the chosen use-god.
- [ ] Keep `世应` as a structural relation, but add scenario-specific interpretation only through rule config, not hard-coded prose.
- [ ] Add at least one regression case where moving lines and shi-ying point in different directions, so the engine must resolve mixed signals.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=MovingLineAffectUseGodRuleTest,ShiYingRelationRuleTest,RuleEngineScenarioRegressionTest test
```

Expected:
- We can distinguish “有变化” from “变化对主事有利/不利/反复”.

Acceptance:
- The engine explains moving-change and shi-ying as evidence, not as generic filler text.

## Chunk 3: Missing High-Value Rule Family

### Task 4: Introduce Fu-Shen / Fei-Shen As First-Class Data

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/FuShenResolver.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/LineInfo.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/domain/ChartSnapshot.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service/ChartBuilderService.java`
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/FuShenResolverTest.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/ChartBuilderRegressionTest.java`

- [ ] Decide and document one authoritative algorithm source for `伏神 / 飞神`; do not implement against mixed lineages.
- [ ] Extend `LineInfo` so the hidden-line data is explicit and testable.
- [ ] Compute fu-shen during chart build instead of letting later rules guess from text.
- [ ] Add regression cases where the missing six-kin relationship is only visible through `伏神`.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=FuShenResolverTest,ChartBuilderRegressionTest test
```

Expected:
- A chart snapshot can expose hidden-line data without requiring the LLM to infer it.

Acceptance:
- `伏神 / 飞神` exists as chart data, not just as future prose.

### Task 5: Add Fu-Shen / Fei-Shen Rules

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced/FuShenFlyShenRule.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/yongshen_rules.json`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1/rule_definitions.json`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/advanced/FuShenFlyShenRuleTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/service/RuleEngineScenarioRegressionTest.java`

- [ ] Encode at least the first deterministic rule set: `用神不上卦时取伏神`, `飞神压伏`, `伏得生扶`, `伏被冲破`.
- [ ] Expose evidence fields that later analysis can cite directly.
- [ ] Add scenario tests where conclusions differ materially after fu-shen is considered.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=FuShenFlyShenRuleTest,RuleEngineScenarioRegressionTest test
```

Expected:
- The engine can now reason about cases where the obvious surface lines are insufficient.

Acceptance:
- `伏神 / 飞神` changes actual rule output, not just display output.

## Chunk 4: Knowledge Base Alignment

### Task 6: Expand Knowledge Import Topics To Match Core Rules

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeImportService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-worker/app/pipeline/book_pipeline.py`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/knowledge/controller/KnowledgeImportExecutionTest.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchServiceTest.java`

- [ ] Expand default import topics from `用神 / 世应 / 六亲 / 月破 / 日破 / 空亡 / 动爻` to include the next-stage core topics such as `伏神`, `飞神`, `旬空`, `入墓`, `冲开`, `化进`, `化退`.
- [ ] Keep retrieval topics aligned with what the rule engine can actually produce; do not import large topic families that the engine cannot yet use.
- [ ] Verify that TXT and PDF imports can still produce stable chunks after adding new topic tags.

Run:
```bash
cd /Users/liuyishou/wordspace/liuyao/liuyao-app
../apache-maven-3.9.6/bin/mvn -q -Dtest=KnowledgeImportExecutionTest,KnowledgeSearchServiceTest test
```

Expected:
- Imported books can be retrieved under the same topic vocabulary used by the deterministic rules.

Acceptance:
- Knowledge import stops being a separate silo and becomes a direct amplifier for the rule engine.

## Phase Two After Core Stability

Only start these after all tasks above are green and scenario regressions are trusted.

### Task 7: Add Phase-Two Symbolic Families

**Files likely touched:**
- `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/service`
- `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/advanced`
- `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/resources/rules/v1`
- `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule`

Second-wave candidates:
- [ ] `反吟 / 伏吟`
- [ ] `应期` first deterministic version
- [ ] `互卦 / 错卦 / 综卦`
- [ ] `神煞`

Gate:
- These features must not be used to hide weakness in the core use-god / moving-change / empty-break logic.

## Recommended Delivery Order

If this work is split into releases, use this sequence:

1. Release A: baseline plate correctness
2. Release B: use-god and moving-change strengthening
3. Release C: `伏神 / 飞神`
4. Release D: knowledge-base topic expansion
5. Release E: phase-two symbolic families

## Validation Checklist

- [ ] Plate snapshot tests are green.
- [ ] Rule scenario regressions are green.
- [ ] No new core rule depends on free-form model inference.
- [ ] Book-import topics match actual rule vocabulary.
- [ ] Each newly added rule has at least one positive and one negative regression case.

## Practical Product Guidance

If only one thing is done next, do this first:
- Finish `用神状态 + 动变 + 空破` before anything decorative.

If two things are done next, do this:
- Add `伏神 / 飞神` immediately after the state-system upgrade.

If you want stronger credibility with experienced six-yao users:
- Prioritize correctness of `伏神 / 飞神 / 空破 / 动变` over `神煞 / 互错综`.
