package com.yishou.liuyao.rule.usegod;

import org.springframework.stereotype.Component;

@Component
public class QuestionDirectionResolutionService {

    private final QuestionCategoryNormalizer questionCategoryNormalizer;
    private final QuestionIntentResolver questionIntentResolver;

    public QuestionDirectionResolutionService(QuestionCategoryNormalizer questionCategoryNormalizer,
                                              QuestionIntentResolver questionIntentResolver) {
        this.questionCategoryNormalizer = questionCategoryNormalizer;
        this.questionIntentResolver = questionIntentResolver;
    }

    public QuestionDirectionResolution resolve(String questionText,
                                               String userSelectedDirection,
                                               String confirmedDirection) {
        String detectedDirection = questionIntentResolver.detectDirectionFromText(questionText);
        String normalizedSelectedDirection = normalize(userSelectedDirection);
        String normalizedConfirmedDirection = normalize(confirmedDirection);

        if (normalizedSelectedDirection.isBlank()) {
            return build(questionText, detectedDirection, normalizedSelectedDirection, detectedDirection,
                    false, detectedDirection, "detected");
        }
        if (detectedDirection.isBlank() || detectedDirection.equals(normalizedSelectedDirection)) {
            return build(questionText, detectedDirection, normalizedSelectedDirection, normalizedSelectedDirection,
                    false, detectedDirection, "user_selected");
        }
        if (!normalizedConfirmedDirection.isBlank()) {
            return build(questionText, detectedDirection, normalizedSelectedDirection, normalizedConfirmedDirection,
                    false, detectedDirection, "confirmed");
        }
        return build(questionText, detectedDirection, normalizedSelectedDirection, normalizedSelectedDirection,
                true, detectedDirection, "conflict");
    }

    private QuestionDirectionResolution build(String questionText,
                                              String detectedDirection,
                                              String userSelectedDirection,
                                              String finalDirection,
                                              boolean requiresConfirmation,
                                              String suggestedDirection,
                                              String source) {
        return new QuestionDirectionResolution(
                questionText == null ? "" : questionText,
                detectedDirection == null ? "" : detectedDirection,
                userSelectedDirection == null ? "" : userSelectedDirection,
                finalDirection == null ? "" : finalDirection,
                requiresConfirmation,
                suggestedDirection == null ? "" : suggestedDirection,
                source,
                detectedDirection == null || detectedDirection.isBlank() ? 0.0 : 1.0
        );
    }

    private String normalize(String direction) {
        String normalized = questionCategoryNormalizer.normalize(direction);
        return normalized == null ? "" : normalized;
    }
}
