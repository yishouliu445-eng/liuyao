package com.yishou.liuyao.rule.usegod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestionCategoryNormalizerTest {

    private final QuestionCategoryNormalizer normalizer = new QuestionCategoryNormalizer();

    @Test
    void shouldNormalizeKnownAliasesIntoCanonicalCategory() {
        assertEquals("收入", normalizer.normalize("工资"));
        assertEquals("求职", normalizer.normalize("面试"));
        assertEquals("人际", normalizer.normalize("relation"));
        assertEquals("婚姻", normalizer.normalize("婚姻"));
        assertEquals("合作", normalizer.normalize("合同"));
        assertEquals("财运", normalizer.normalize("投资"));
        assertEquals("婚姻", normalizer.normalize("复婚"));
        assertEquals("升职", normalizer.normalize("晋升"));
        assertEquals("官司", normalizer.normalize("纠纷"));
        assertEquals("寻物", normalizer.normalize("失物"));
    }

    @Test
    void shouldKeepUnknownCategoryWhenAliasNotConfigured() {
        assertEquals("房产", normalizer.normalize("房产"));
    }
}
