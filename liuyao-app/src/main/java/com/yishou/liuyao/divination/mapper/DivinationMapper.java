package com.yishou.liuyao.divination.mapper;

import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.rule.usegod.QuestionCategoryNormalizer;
import org.springframework.stereotype.Component;

@Component
public class DivinationMapper {

    private final QuestionCategoryNormalizer questionCategoryNormalizer;

    public DivinationMapper(QuestionCategoryNormalizer questionCategoryNormalizer) {
        this.questionCategoryNormalizer = questionCategoryNormalizer;
    }

    public DivinationInput toInput(DivinationAnalyzeRequest request) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(request.getQuestionText());
        // 请求进入领域层前先做问类规范化，避免同义分类把后续规则和模板打散。
        String finalDirection = request.getFinalDirection() == null || request.getFinalDirection().isBlank()
                ? request.getQuestionCategory()
                : request.getFinalDirection();
        input.setQuestionCategory(questionCategoryNormalizer.normalize(finalDirection));
        input.setRawLines(request.getRawLines());
        input.setMovingLines(request.getMovingLines());
        input.setDivinationTime(request.getDivinationTime());
        return input;
    }
}
