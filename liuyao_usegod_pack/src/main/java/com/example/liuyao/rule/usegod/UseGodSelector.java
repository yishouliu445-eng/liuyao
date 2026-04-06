package com.example.liuyao.rule.usegod;

import com.example.liuyao.divination.domain.ChartSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UseGodSelector {

    private final QuestionIntentResolver intentResolver;
    private final UseGodRuleConfigLoader configLoader;

    public UseGodSelector(QuestionIntentResolver intentResolver,
                          UseGodRuleConfigLoader configLoader) {
        this.intentResolver = intentResolver;
        this.configLoader = configLoader;
    }

    public UseGodSelection select(ChartSnapshot chart) {
        QuestionIntent intent = intentResolver.resolve(chart.getQuestion(), chart.getQuestionCategory());
        UseGodRuleItem item = configLoader.getRule(intent);

        if (item == null) {
            return new UseGodSelection(
                    QuestionIntent.UNKNOWN,
                    null,
                    999,
                    "未知场景",
                    "未命中用神规则，请人工判断。",
                    configLoader.getVersion()
            );
        }

        return new UseGodSelection(
                intent,
                UseGodType.valueOf(item.getUseGod()),
                item.getPriority(),
                item.getScenario(),
                item.getNote(),
                configLoader.getVersion()
        );
    }
}
