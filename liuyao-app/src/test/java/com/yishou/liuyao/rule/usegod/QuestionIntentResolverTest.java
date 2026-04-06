package com.yishou.liuyao.rule.usegod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionIntentResolverTest {

    private final QuestionIntentResolver resolver = new QuestionIntentResolver();

    @Test
    void shouldPreferStructuredCategoryWhenPresent() {
        QuestionIntent intent = resolver.resolve("我想知道工资会涨多少", "收入");

        assertEquals(QuestionIntent.INCOME, intent);
    }

    @Test
    void shouldFallbackToQuestionTextWhenCategoryUnknown() {
        QuestionIntent intent = resolver.resolve("这次面试能顺利入职吗", null);

        assertEquals(QuestionIntent.JOB_OPPORTUNITY, intent);
    }

    @Test
    void shouldResolveTravelIntentFromCategoryAndText() {
        assertEquals(QuestionIntent.TRAVEL, resolver.resolve("这次出行会不会顺利", "出行"));
        assertEquals(QuestionIntent.TRAVEL, resolver.resolve("这趟旅行路上顺利吗", null));
    }
}
