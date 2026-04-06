# 六爻用神选择规则包（可直接复制到项目）

## 这份代码做了什么

这不是“自动断卦”，而是把“先看什么”这件事工程化。

它做了 3 件事：

1. 先把问题识别成更细的 `QuestionIntent`
2. 再通过 `use_god_rules.json` 做用神映射
3. 输出一条结构化 `RuleHit`

---

## 为什么不是“问工作一定取官鬼”

因为“工作”不是一个足够细的问题类型。

例如：
- 问能不能入职：看官鬼
- 问工资多少：看妻财
- 问同事关系：看兄弟
- 问成长空间：看子孙

所以代码里先做的是 **意图识别**，再做 **用神映射**。

---

## 你要放到哪里

把这些文件复制进你的 Spring Boot 项目：

- `src/main/java/com/example/liuyao/rule/...`
- `src/main/resources/rules/use_god_rules.json`

你自己的 `ChartSnapshot` 最少需要这些字段：

```java
private String question;
private String questionCategory;
```

如果你当前项目字段名不一致，改一下 `UseGodSelector` 即可。

---

## 依赖要求

需要项目里已经有：
- Spring Boot
- Jackson（Spring Boot Web 默认一般就有）

---

## 怎么接入现有 RuleEngineService

如果你的 `RuleEngineService` 是按 Spring 注入 `List<Rule>`，那 `UseGodRule` 加上 `@Component` 后会自动参与执行。

例如：

```java
@Service
public class RuleEngineService {

    private final List<Rule> rules;

    public RuleEngineService(List<Rule> rules) {
        this.rules = rules;
    }

    public List<RuleHit> evaluate(ChartSnapshot chart) {
        return rules.stream()
                .map(rule -> rule.evaluate(chart))
                .filter(RuleHit::getHit)
                .toList();
    }
}
```

---

## 这条规则当前的定位

当前它的定位是：

- 帮你确定“当前问题优先看什么”
- 不是直接判断吉凶
- 不负责长篇解释

AI 或 RAG 可以根据 `RuleHit.evidence.useGod` 再去解释。

---

## 下一步建议

你可以继续接两条简单规则：

1. `MovingLineExistsRule`：有无动爻
2. `ShiYingExistsRule`：世应是否正常定位

这样就能跑出更像样的 `RuleHit` 列表。
