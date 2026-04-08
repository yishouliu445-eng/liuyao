# Question Category Taxonomy Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 收敛六爻问类体系，新增问类规范化入口，并把首批标准问类接入用神识别、知识召回和分析模板。

**Architecture:** 保持现有 `QuestionIntent` 与规则配置基本不动，在其前面增加一个轻量的问类规范化器。先统一外部 `questionCategory` 的中文标准名，再让 mapper、intent resolver、knowledge、analysis 共用这套标准问类。

**Tech Stack:** Java 17, Spring Boot, JUnit 5, 现有 rule/usegod 与 analysis 模块

---

## Chunk 1: 问类体系文档与规范化组件

### Task 1: 新增问类体系文档

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/openspec/0001-springboot-skeleton/question-category-taxonomy.md`

- [ ] **Step 1: 写文档**
- [ ] **Step 2: 列出一级大类和首批标准问类**
- [ ] **Step 3: 列出别名映射与当前覆盖范围**

### Task 2: 新增问类规范化器

**Files:**
- Create: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionCategoryNormalizer.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionCategoryNormalizerTest.java`

- [ ] **Step 1: 先写失败测试**
- [ ] **Step 2: 实现最小别名映射**
- [ ] **Step 3: 运行测试**

## Chunk 2: 把规范化接入主链路

### Task 3: 在输入映射阶段规范化问类

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/divination/mapper/DivinationMapper.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/divination/service/DivinationServiceIntegrationTest.java`

- [ ] **Step 1: 注入规范化器**
- [ ] **Step 2: 将请求问类转换为标准问类**
- [ ] **Step 3: 补集成断言**

### Task 4: 在用神识别阶段复用标准问类

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolver.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/rule/usegod/QuestionIntentResolverTest.java`

- [ ] **Step 1: 先用标准问类做映射**
- [ ] **Step 2: 保留文本兜底逻辑**
- [ ] **Step 3: 补求职/人际/考试等样例**

## Chunk 3: 让知识召回和分析模板吃到新问类

### Task 5: 扩展知识召回的问类分支

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchService.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/knowledge/service/KnowledgeSearchServiceTest.java`

- [ ] **Step 1: 按标准问类调整召回偏好**
- [ ] **Step 2: 补求职/考试/人际的召回测试**
- [ ] **Step 3: 运行测试**

### Task 6: 扩展分析模板的问类提示

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/main/java/com/yishou/liuyao/analysis/service/AnalysisService.java`
- Test: `/Users/liuyishou/wordspace/liuyao/liuyao-app/src/test/java/com/yishou/liuyao/analysis/service/AnalysisServiceTest.java`

- [ ] **Step 1: 按标准问类拆结论提示**
- [ ] **Step 2: 保持旧类别兼容**
- [ ] **Step 3: 补分析文本断言**

## Chunk 4: 文档与回归

### Task 7: 更新接口文档与说明

**Files:**
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/docs/api.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/liuyao-app/README.md`
- Modify: `/Users/liuyishou/wordspace/liuyao/openspec/0001-springboot-skeleton/product-todo.md`

- [ ] **Step 1: 记录首批标准问类**
- [ ] **Step 2: 记录问类规范化行为**
- [ ] **Step 3: 更新后续待办**

### Task 8: 运行专项和全量回归

**Files:**
- Test only

- [ ] **Step 1: 运行 `QuestionCategoryNormalizerTest,QuestionIntentResolverTest,KnowledgeSearchServiceTest,AnalysisServiceTest`**
- [ ] **Step 2: 运行 `DivinationControllerTest,DivinationServiceIntegrationTest`**
- [ ] **Step 3: 运行 `mvn test -q`**
