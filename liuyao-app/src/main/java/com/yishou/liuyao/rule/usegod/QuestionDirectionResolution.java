package com.yishou.liuyao.rule.usegod;

public record QuestionDirectionResolution(
        String questionText,
        String detectedDirection,
        String userSelectedDirection,
        String finalDirection,
        boolean requiresConfirmation,
        String suggestedDirection,
        String source,
        double confidence
) {
}
