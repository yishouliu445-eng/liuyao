# 六爻系统下一阶段重点任务（P0 / P1 / P2）执行文档

版本：v1.0  
用途：直接交给 Codex / Claude 执行开发  
基于：当前已完成排盘 + 用神类型 + 规则引擎 + 初步分析服务

---

# 一、总体判断

当前系统已经完成：

- 排盘引擎（稳定）
- 用神类型选择（已完成）
- 规则引擎框架（已完成）
- 分析服务（已成型）
- 文档处理与知识入库（worker）

当前阶段不再属于“搭架构”，而是：

👉 **增强推理能力 + 构建可迭代体系**

---

# 二、阶段目标

本阶段核心目标：

1. 锁定“主用神爻”
2. 提升规则密度（规则二期）
3. 拆分分析职责（防止失控）
4. 建立案例回放系统（核心资产）
5. 打通知识 → 规则链路（长期能力）

---

# 三、P0（必须优先完成）

## 1️⃣ 用神具体爻定位（最高优先级）

### 目标

输出：

- useGodType
- useGodLineIndex
- candidateLineIndexes
- selectionReason
- fallbackStrategy

### 必做

- 实现 UseGodLineLocator
- 实现 CandidateScorer
- 实现 FallbackResolver
- 主流程返回 selectedLineIndex

---

## 2️⃣ 规则二期（围绕主用神）

### 必补规则

- 用神生世 / 克世
- 应爻生世 / 克世
- 用神合世 / 冲世
- 用神空亡 / 出空
- 用神月破 / 日破
- 用神动化进 / 动化退
- 世爻旺衰
- 多动爻扰动

### 要求

- 所有规则围绕 selectedLineIndex
- 输出 evidence
- 支持复合条件

---

## 3️⃣ AnalysisService 拆分

### 当前问题

- 逻辑过重
- 有吞规则风险

### 拆分结构

- RuleEngineService（判断）
- AnalysisAssembler（组装）
- ExplanationBuilder（解释）

### 要求

- 禁止在 analysis 中写规则判断
- 禁止 controller 写逻辑

---

# 四、P1（紧接着做）

## 4️⃣ 案例回放系统

### 目标

支持：

- 批量案例重跑
- 新旧规则对比
- 回归验证

### 数据结构

```json
{
  "question": "",
  "chart": {},
  "useGod": {},
  "expectedResult": "",
  "realResult": ""
}
```

### 功能

- CaseReplayService
- 批量执行
- 输出差异报告

---

## 5️⃣ 规则版本管理

### 目标

- 规则版本号
- 规则变更记录
- 分析结果绑定规则版本

### 必做

- rule_version 字段
- 分析结果记录 version
- 支持回放旧版本

---

# 五、P2（后续优化）

## 6️⃣ 知识 → 规则闭环

### 当前问题

- worker 只做切片

### 目标

```text
文本 → 规则候选 → 人工确认 → rule库
```

### 必做

- RuleCandidateExtractor
- 候选 JSON 输出
- 人工审核接口

---

## 7️⃣ LLM 表达层

### 目标

- 把结构化结果转为自然语言

### 注意

- LLM 不参与判断
- 只做表达

---

# 六、执行顺序（必须按顺序）

## 阶段 1（本周）

- 用神定位
- 规则二期（20条）
- Analysis 拆分

## 阶段 2（下周）

- 案例回放
- 规则版本

## 阶段 3

- 知识转规则
- LLM

---

# 七、给 Codex / Claude 的任务

## 任务 1：用神定位

实现：

- UseGodLineLocator
- 候选查找
- 评分排序
- fallback

---

## 任务 2：规则扩展

实现：

- 新增 20 条规则
- 支持复合条件
- 输出 evidence

---

## 任务 3：分析拆分

实现：

- AnalysisAssembler
- ExplanationBuilder
- 清理 AnalysisService

---

## 任务 4：案例系统

实现：

- CaseReplayService
- 回归执行
- 差异分析

---

## 任务 5：规则版本

实现：

- rule_version
- 分析绑定版本
- 回放支持

---

# 八、最终目标

系统升级为：

👉 六爻推理引擎（Rule-based AI System）

而不是：

❌ 排盘工具

---

# 九、一句话总结

你现在缺的不是功能，而是：

👉 主用神锚点 + 规则密度 + 案例回放 + 规则演化能力
