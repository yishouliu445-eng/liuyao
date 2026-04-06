# 六爻系统核心代码设计（Java 落地版）

> 目标：提供可以直接开始编码的核心代码结构  
> 范围：排盘引擎 + 规则引擎 + 主流程编排

---

# 一、核心领域对象

## 1. ChartSnapshot

```java
public class ChartSnapshot {

    private String question;
    private String questionCategory;
    private String divinationMethod;
    private LocalDateTime divinationTime;

    private String mainHexagram;
    private String changedHexagram;

    private Integer shi;
    private Integer ying;

    private String riChen;
    private String yueJian;
    private List<String> kongWang;

    private List<LineInfo> lines;

    private Map<String, Object> ext; // 扩展字段

}
```

---

## 2. LineInfo

```java
public class LineInfo {

    private Integer index;
    private String yinYang;
    private Boolean isMoving;
    private String changeTo;

    private String liuQin;
    private String liuShen;
    private String branch;

    private Boolean isShi;
    private Boolean isYing;

}
```

---

# 二、排盘引擎骨架

## 1. 输入对象

```java
public class DivinationInput {

    private List<String> rawLines;
    private List<Integer> movingLines;
    private LocalDateTime divinationTime;

}
```

---

## 2. ChartBuilderService

```java
@Service
public class ChartBuilderService {

    public ChartSnapshot buildChart(DivinationInput input) {

        ChartSnapshot chart = new ChartSnapshot();

        // TODO: 卦象计算
        // TODO: 六亲计算
        // TODO: 六神计算
        // TODO: 世应定位
        // TODO: 空亡计算

        return chart;
    }
}
```

---

# 三、规则引擎

## 1. Rule 接口

```java
public interface Rule {

    RuleHit evaluate(ChartSnapshot chart);

}
```

---

## 2. RuleHit

```java
public class RuleHit {

    private String ruleCode;
    private String ruleName;
    private Boolean hit;

    private String hitReason;
    private String impactLevel;

    private Map<String, Object> evidence;

}
```

---

## 3. 示例规则：用神规则

```java
@Component
public class JobUseGodRule implements Rule {

    @Override
    public RuleHit evaluate(ChartSnapshot chart) {

        RuleHit hit = new RuleHit();
        hit.setRuleCode("JOB_USE_GOD");
        hit.setRuleName("问工作取官鬼");

        if ("求职".equals(chart.getQuestionCategory())) {
            hit.setHit(true);
            hit.setImpactLevel("HIGH");
            hit.setHitReason("求职类问题，用神取官鬼");
        } else {
            hit.setHit(false);
        }

        return hit;
    }
}
```

---

## 4. RuleEngineService

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

# 四、AI 分析模块

## 1. AnalysisService

```java
@Service
public class AnalysisService {

    public String analyze(String question,
                          ChartSnapshot chart,
                          List<RuleHit> ruleHits) {

        // TODO: 拼接 prompt
        // TODO: 调用 LLM

        return "分析结果";
    }
}
```

---

# 五、主流程编排

## 1. DivinationService

```java
@Service
public class DivinationService {

    private final ChartBuilderService chartBuilder;
    private final RuleEngineService ruleEngine;
    private final AnalysisService analysisService;

    public DivinationService(ChartBuilderService chartBuilder,
                             RuleEngineService ruleEngine,
                             AnalysisService analysisService) {
        this.chartBuilder = chartBuilder;
        this.ruleEngine = ruleEngine;
        this.analysisService = analysisService;
    }

    public DivinationAnalyzeResponse analyze(DivinationAnalyzeRequest request) {

        DivinationInput input = new DivinationInput();
        input.setRawLines(request.getRawLines());
        input.setMovingLines(request.getMovingLines());
        input.setDivinationTime(request.getDivinationTime());

        ChartSnapshot chart = chartBuilder.buildChart(input);

        List<RuleHit> hits = ruleEngine.evaluate(chart);

        String analysis = analysisService.analyze(
                request.getQuestionText(),
                chart,
                hits
        );

        return new DivinationAnalyzeResponse(chart, hits, analysis);
    }
}
```

---

# 六、总结

这套代码结构核心思想：

- ChartSnapshot 是核心数据
- RuleEngine 只负责判断
- AI 只负责解释
- 主流程清晰分层

