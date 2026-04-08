package com.yishou.liuyao.rule.usegod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionIntentResolverTest {

    private final QuestionIntentResolver resolver = new QuestionIntentResolver(new QuestionCategoryNormalizer());

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

    @Test
    void shouldResolveMoreCanonicalCategoriesAfterNormalization() {
        assertEquals(QuestionIntent.JOB_OPPORTUNITY, resolver.resolve("这次面试能过吗", "面试"));
        assertEquals(QuestionIntent.RELATION, resolver.resolve("同事关系会不会改善", "关系"));
        assertEquals(QuestionIntent.EXAM, resolver.resolve("考试能过吗", "考证"));
        assertEquals(QuestionIntent.INCOME, resolver.resolve("这次投资能赚钱吗", "投资"));
        assertEquals(QuestionIntent.EMOTION, resolver.resolve("还有没有复婚机会", "复婚"));
        assertEquals(QuestionIntent.GROWTH, resolver.resolve("这次晋升机会大吗", "晋升"));
        assertEquals(QuestionIntent.LAWSUIT, resolver.resolve("这场官司压力大不大", "纠纷"));
        assertEquals(QuestionIntent.REAL_ESTATE, resolver.resolve("买房这件事能成吗", "买房"));
        assertEquals(QuestionIntent.RELOCATION, resolver.resolve("这次搬迁会顺利吗", "迁居"));
        assertEquals(QuestionIntent.LOST_ITEM, resolver.resolve("丢的证件还能找到吗", "失物"));
    }
}
