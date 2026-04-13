package com.yishou.liuyao.analysis.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class MockLlmClientSpringTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void shouldInjectMockLlmClientAndReturnFixedResponse() {
        assertTrue(llmClient instanceof MockLlmClient);

        LlmClient.LlmResponse response = llmClient.chat(
                List.of(LlmClient.ChatMessage.system("system"), LlmClient.ChatMessage.user("question")),
                null,
                true);

        assertTrue(response.success());
        assertEquals("mock", response.model());
        assertNotNull(response.parsedJson());
        assertEquals("【测试模式】本卦天火同人，象征协作共进，用神旺相。",
                response.parsedJson().path("analysis").path("hexagramOverview").asText());
    }
}
