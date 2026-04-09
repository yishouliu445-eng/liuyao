package com.yishou.liuyao.rule.usegod;

import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.rule.batch.UseGodLineLocator;
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
            return new UseGodSelection(QuestionIntent.UNKNOWN, null, 999, "未知场景", "未命中用神规则，请人工判断。", useGodRuleConfigLoader.getVersion(),
                    null, null, null, null, false, null, null, null);
        }
        UseGodType useGodType = UseGodType.valueOf(item.getUseGod());
        UseGodLineLocator.SelectionResult selectionResult = UseGodLineLocator.locate(chartSnapshot, useGodType);
        return new UseGodSelection(
                intent,
                useGodType,
                item.getPriority(),
                item.getScenario(),
                item.getNote(),
                useGodRuleConfigLoader.getVersion(),
                selectionResult.selectedLineIndex(),
                selectionResult.candidateLineIndexes(),
                selectionResult.selectionStrategy(),
                selectionResult.selectionReason(),
                selectionResult.fallbackApplied(),
                selectionResult.fallbackStrategy(),
                selectionResult.scoreDetails(),
                selectionResult.evidence()
        );
    }
}
