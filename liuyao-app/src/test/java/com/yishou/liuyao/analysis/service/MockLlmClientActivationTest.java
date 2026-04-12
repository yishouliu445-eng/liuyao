package com.yishou.liuyao.analysis.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest
@ActiveProfiles("test")
class MockLlmClientActivationTest {

    @Autowired
    private LlmClient llmClient;

    @Test
    void shouldUseMockLlmClientInTestProfile() {
        assertInstanceOf(MockLlmClient.class, llmClient);
    }
}
