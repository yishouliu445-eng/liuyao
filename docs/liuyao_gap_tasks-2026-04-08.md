# 六爻系统缺口分析与下一阶段任务清单（可执行版）

版本：v2.0  
用途：直接丢给 Codex / Claude 拆任务与生成代码  
基于：当前已实现排盘 + 用神类型 + 规则引擎 + 分析服务

---

# 一、当前系统成熟度评估

当前系统已具备：

- 排盘引擎（完整服务拆分 + 测试）
- 用神选择（类型级）
- 规则引擎（结构化 + JSON + Java规则）
- 分析服务（完整流程 + 结构化输出）
- 文档处理 worker（知识入库 + embedding）

结论：

👉 已完成「系统骨架期」  
👉 进入「能力深化期」

---

# 二、当前最大缺口（必须优先解决）

## 1️⃣ 用神“具体爻定位”缺失（最高优先级）

当前问题：

- 只确定了“官鬼/妻财”等类型
- 没有统一输出“哪一爻是用神”

导致：

- 规则命中缺少锚点
- 解释不具体
- 后续规则难扩展

---

## 🎯 目标

输出结构：

```json
{
  "useGodType": "OFFICIAL_GHOST",
  "useGodLineIndex": 4,
  "candidates": [4, 6],
  "selectionReason": "优先取动爻官鬼",
  "fallbackStrategy": "取最近世爻的官鬼"
}
```

---

## 🛠️ Codex任务

实现：

- UseGodLineLocator
- UseGodSelection升级
- 主流程返回 useGodLineIndex

要求：

- 支持多个候选
- 支持优先级规则
- 支持 fallback

---

# 三、规则系统缺口

## 当前问题

规则数量不足（偏基础状态判断）

---

## 🎯 目标

补齐“主轴规则”

---

## 第一批必须新增规则（优先实现）

### 用神相关

- 用神生世
- 用神克世
- 用神合世
- 用神入墓
- 用神逢合
- 用神逢冲细分（冲开/冲散）

---

### 世爻相关

- 世爻旺衰判断细化
- 世爻受生/受克
- 世爻与用神距离

---

### 动爻相关

- 用神动化进
- 用神动化退
- 动爻冲世
- 动爻冲用神

---

### 结构规则

- 世应距离影响
- 多动爻扰动等级
- 主用神与辅助用神冲突

---

## 🛠️ Codex任务

- 新增 20~30 条规则 JSON
- 补 RuleMatcher 支持复合条件
- 增加 evidence 输出

---

# 四、AnalysisService 拆分（避免失控）

当前问题：

- AnalysisService 过大（600+行）
- 有吞掉规则逻辑风险

---

## 🎯 目标拆分

拆成：

- RuleEngineService（判断）
- AnalysisAssembler（拼结构）
- ExplanationBuilder（解释）

---

## 🛠️ Codex任务

- 拆分 AnalysisService
- 禁止业务逻辑写入 controller
- 保证规则只在 RuleEngine 执行

---

# 五、案例回归体系（核心资产）

## 当前问题

测试偏工程验证，不是业务验证

---

## 🎯 目标

建立案例库：

```json
{
  "question": "面试能否通过",
  "chart": {},
  "useGod": {},
  "expectedResult": "通过",
  "realResult": "通过",
  "confidence": 0.9
}
```

---

## 🛠️ Codex任务

- 建 case 表
- 写 CaseReplayService
- 支持批量回放
- 输出规则命中差异

---

# 六、知识 → 规则闭环（关键升级）

当前：

- worker 只做文本切片

---

## 🎯 目标

新增流程：

```text
书籍切片 → 规则候选 → 人工审核 → rule库
```

---

## 🛠️ Codex任务

- 实现 RuleCandidateExtractor
- 提取规则描述
- 输出候选 JSON

---

# 七、rules目录改造

当前问题：

- zip文件不可维护

---

## 🎯 目标

改为：

```text
rules/
  v1/
    yongshen_rules.json
    shi_rules.json
    composite_rules.json
```

---

## 🛠️ Codex任务

- 拆 zip
- 转 JSON
- 支持版本加载

---

# 八、优先级执行顺序

## P0（本周必须做）

1. 用神定位
2. 规则补充20条
3. AnalysisService拆分

---

## P1（下周）

4. 案例回归系统
5. 规则管理增强

---

## P2（后续）

6. 知识转规则
7. LLM表达层

---

# 九、最终目标

系统升级为：

👉 六爻推理引擎（Rule-based AI System）

而不是：

❌ 排盘工具

---

# 十、一句话总结

你现在缺的不是代码能力，而是：

👉 规则密度 + 用神精度 + 案例厚度

