package com.yishou.liuyao.rule.usegod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionDirectionResolutionServiceTest {

    private final QuestionDirectionResolutionService service = new QuestionDirectionResolutionService(
            new QuestionCategoryNormalizer(),
            new QuestionIntentResolver(new QuestionCategoryNormalizer())
    );

    @Test
    void shouldPreferDetectedDirectionWhenUserDidNotSelectOne() {
        QuestionDirectionResolution result = service.resolve("我和前任还有机会复合吗", null, null);

        assertEquals("感情", result.detectedDirection());
        assertEquals("感情", result.finalDirection());
        assertFalse(result.requiresConfirmation());
    }

    @Test
    void shouldKeepSelectedDirectionWhenDetectedDirectionIsUnknown() {
        QuestionDirectionResolution result = service.resolve("这件事后面会怎样", "合作", null);

        assertEquals("", result.detectedDirection());
        assertEquals("合作", result.finalDirection());
        assertFalse(result.requiresConfirmation());
    }

    @Test
    void shouldRequireConfirmationWhenSelectionConflictsWithDetection() {
        QuestionDirectionResolution result = service.resolve("我和前任还有机会复合吗", "出行", null);

        assertEquals("感情", result.detectedDirection());
        assertEquals("出行", result.userSelectedDirection());
        assertEquals("感情", result.suggestedDirection());
        assertTrue(result.requiresConfirmation());
    }

    @Test
    void shouldAcceptConfirmedDirectionWhenConflictWasResolved() {
        QuestionDirectionResolution result = service.resolve("我和前任还有机会复合吗", "出行", "感情");

        assertEquals("感情", result.finalDirection());
        assertFalse(result.requiresConfirmation());
        assertEquals("confirmed", result.source());
    }
}
