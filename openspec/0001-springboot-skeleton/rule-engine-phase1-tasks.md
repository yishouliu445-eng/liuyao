# Rule Engine 一期任务清单

## 本轮目标

参考 [liuyao_rule_engine_detailed_design_20260407.md](/Users/liuyishou/wordspace/liuyao/docs/liuyao_rule_engine_detailed_design_20260407.md)，先补齐一期最小闭环里最关键的三件事：

- 规则元数据
- 规则执行顺序
- 结构化规则汇总结果

## 任务拆分

- [x] T1. 为 `RuleHit` 增加一期需要的元数据字段
  - `category`
  - `priority`
  - `scoreDelta`
  - `tags`

- [x] T2. 为规则引擎增加统一汇总结果
  - 命中规则排序
  - 总分计算
  - `resultLevel`
  - 标签汇总
  - 结构化 `summary`

- [x] T3. 在分析接口里透出结构化规则汇总
  - `analysisContext.structuredResult`
  - `response.structuredResult`

- [x] T4. 补对应回归测试
  - 规则引擎排序与汇总测试
  - HTTP 响应结构测试

## 暂未纳入本轮

- [ ] 规则 JSON 配置化
- [ ] 通用 `condition / effect` DSL
- [ ] `rule_definition` 表落库
- [ ] 冲突处理器
- [ ] 推理层独立模块化

## 第二批任务

- [x] T5. 增加规则定义模型与 JSON 加载
  - `RuleDefinition`
  - `RuleCondition`
  - `RuleEffect`
  - `rules/rule_definitions.json`

- [x] T6. 增加 `RuleEvaluationContext` 与最小 `RuleMatcher`
  - 一期先支持 `EQUALS`
  - 一期先支持 `GREATER_THAN`
  - 一期先支持 `allOf`

- [x] T7. 把 JSON 规则接入现有规则引擎
  - 先接入复合规则，不替换现有硬编码规则
  - 一期先覆盖 `R016 / R019 / R020`

- [x] T8. 补单测与组合回归
  - 规则定义加载测试
  - 条件匹配测试
  - 规则引擎接入测试

## 第三批任务

- [x] T9. 扩展一期最小条件集
  - 支持 `NOT_EQUALS`
  - 支持 `IN`
  - 支持 `IS_EMPTY / NOT_EMPTY`
  - 支持 `IS_TRUE / IS_FALSE`

- [x] T10. 补世爻状态规则的一期配置版
  - `R007 世爻旺`
  - `R008 世爻弱`
  - `R010 世爻发动`
  - `R011 世爻空亡`

- [x] T11. 扩展 `RuleEvaluationContext`
  - `useGodFound`
  - `shiMoving`
  - `shiEmpty`

- [x] T12. 调整接口与集成测试
  - 放宽固定 `ruleCount` 断言
  - 校验新增规则进入 `analysisContext.ruleCodes`

- [x] T13. 补一期剩余一批配置规则
  - `R003 用神生世`
  - `R004 用神合世`
  - `R009 世爻被克`
  - `R013 世应相克`
  - `R014 应爻生世`
  - `R015 应爻克世`
  - `R018 用神动而化退`

- [x] T14. 扩展匹配上下文字段
  - `useGodToShiRelation`
  - `useGodHeShi`
  - `useGodRetreat`

- [x] T15. 补专项回归测试
  - 用神对世爻关系规则
  - 世应关系配置规则
  - 用神化退规则

## 第四批任务

- [x] T16. 补 `HAS_RELATION` 等更贴近六爻语义的条件操作符
  - 支持 `HAS_RELATION`
  - 支持 `NOT_IN`
  - 支持 `LESS_THAN`

- [x] T17. 完成 `rule_definition` 表落库
  - 新增规则定义迁移表
  - 启动时把 JSON 规则同步到数据库
  - 提供 `GET /api/rules/definitions` 查询接口

- [x] T18. 补规则分组执行结果
  - 按 `category` 汇总命中数与分值
  - 在 `structuredResult.categorySummaries` 中透出
  - 补 HTTP 与集成测试

## 当前进度

- [x] T19. 补最小冲突处理器
  - 识别同类正负规则冲突
  - 输出 `positiveCount / negativeCount / positiveScore / negativeScore / netScore`
  - 输出 `decision`
  - 输出 `effectiveRules / suppressedRules`

- [x] T20. 推理层继续独立模块化
  - 拆出 `RuleCategorySummaryService`
  - 拆出 `RuleConflictResolver`
  - 在 `structuredResult.conflictSummaries` 中透出

- [x] T21. 增加冲突同分时的优先级裁剪
  - 同分冲突先比较正负规则优先级
  - 决策结果进入 `decision / effectiveRules / suppressedRules`

- [x] T22. 让冲突决策影响最终有效规则集和有效评分
  - 输出 `effectiveScore / effectiveResultLevel`
  - 输出 `effectiveRuleCodes / suppressedRuleCodes`

## 仍未完成

- [ ] 更细的冲突处理器
  - 更复杂的互斥规则优先级裁剪策略
  - [x] 决策后影响最终有效规则集
  - [x] 决策后影响有效评分
  - [ ] 是否反向影响最终展示排序与类别汇总

- [ ] 推理层进一步独立模块化
  - 从 `RuleReasoningService` 继续拆出更明确的推理组装器
  - 把分组汇总、冲突处理、结果评级进一步解耦
