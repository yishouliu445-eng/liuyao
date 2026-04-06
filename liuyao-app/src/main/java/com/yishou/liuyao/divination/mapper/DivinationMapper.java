package com.yishou.liuyao.divination.mapper;

import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import org.springframework.stereotype.Component;

@Component
public class DivinationMapper {

    public DivinationInput toInput(DivinationAnalyzeRequest request) {
        DivinationInput input = new DivinationInput();
        input.setQuestion(request.getQuestionText());
        input.setQuestionCategory(request.getQuestionCategory());
        input.setRawLines(request.getRawLines());
        input.setMovingLines(request.getMovingLines());
        input.setDivinationTime(request.getDivinationTime());
        return input;
    }
}
