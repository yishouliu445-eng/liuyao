package com.yishou.liuyao.rule.usegod;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import org.springframework.stereotype.Component;

@Component
public class UseGodSelector {

    private final QuestionIntentResolver questionIntentResolver;
    private final UseGodRuleConfigLoader useGodRuleConfigLoader;

    public UseGodSelector(QuestionIntentResolver questionIntentResolver,
                          UseGodRuleConfigLoader useGodRuleConfigLoader) {
        this.questionIntentResolver = questionIntentResolver;
        this.useGodRuleConfigLoader = useGodRuleConfigLoader;
    }

    public UseGodSelection select(ChartSnapshot chartSnapshot) {
        QuestionIntent intent = questionIntentResolver.resolve(chartSnapshot.getQuestion(), chartSnapshot.getQuestionCategory());
        UseGodRuleItem item = useGodRuleConfigLoader.getRule(intent);
        if (item == null) {
            return new UseGodSelection(QuestionIntent.UNKNOWN, null, 999, "未知场景", "未命中用神规则，请人工判断。", useGodRuleConfigLoader.getVersion());
        }
        return new UseGodSelection(
                intent,
                UseGodType.valueOf(item.getUseGod()),
                item.getPriority(),
                item.getScenario(),
                item.getNote(),
                useGodRuleConfigLoader.getVersion()
        );
    }
}
