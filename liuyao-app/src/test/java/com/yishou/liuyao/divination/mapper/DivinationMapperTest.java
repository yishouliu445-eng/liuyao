package com.yishou.liuyao.divination.mapper;

import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DivinationMapperTest {

    @Test
    void shouldNormalizeQuestionCategoryBeforeEnteringDomainLayer() {
        DivinationMapper mapper = new DivinationMapper(new QuestionCategoryNormalizer());
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionCategory("工资");

        DivinationInput input = mapper.toInput(request);

        assertEquals("收入", input.getQuestionCategory());
    }

    @Test
    void shouldPreferFinalDirectionWhenPresent() {
        DivinationMapper mapper = new DivinationMapper(new QuestionCategoryNormalizer());
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionCategory("出行");
        request.setFinalDirection("感情");

        DivinationInput input = mapper.toInput(request);

        assertEquals("感情", input.getQuestionCategory());
    }
}
