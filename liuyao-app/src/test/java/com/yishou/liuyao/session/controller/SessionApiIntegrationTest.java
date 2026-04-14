package com.yishou.liuyao.session.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.session.dto.MessageRequest;
import com.yishou.liuyao.session.dto.SessionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class SessionApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateSessionFollowUpFetchAndCloseSessionThroughHttpApi() throws Exception {
        SessionCreateRequest createRequest = buildCreateRequest();

        String createResponse = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.executionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.messageCount").value(1))
                .andExpect(jsonPath("$.data.chartSnapshot.mainHexagram").isNotEmpty())
                .andExpect(jsonPath("$.data.analysis.analysis.conclusion").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID sessionId = readSessionId(createResponse);

        mockMvc.perform(post("/api/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MessageRequest("那接下来还需要注意什么？"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.messageId").value(notNullValue()))
                .andExpect(jsonPath("$.data.executionId").value(notNullValue()))
                .andExpect(jsonPath("$.data.sessionMessageCount").value(2))
                .andExpect(jsonPath("$.data.analysis.analysis.conclusion").isNotEmpty())
                .andExpect(jsonPath("$.data.smartPrompts").isArray());

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.messageCount").value(2))
                .andExpect(jsonPath("$.data.closedAt").isEmpty())
                .andExpect(jsonPath("$.data.messages", hasSize(4)))
                .andExpect(jsonPath("$.data.messages[0].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.messages[2].role").value("USER"))
                .andExpect(jsonPath("$.data.messages[3].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data.chartSnapshot.mainHexagram").isNotEmpty());

        mockMvc.perform(get("/api/sessions/{sessionId}/messages", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(4)))
                .andExpect(jsonPath("$.data[0].role").value("USER"))
                .andExpect(jsonPath("$.data[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[2].role").value("USER"))
                .andExpect(jsonPath("$.data[3].role").value("ASSISTANT"));

        mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CLOSED"))
                .andExpect(jsonPath("$.data.closedAt").isNotEmpty());
    }

    @Test
    void shouldRejectFollowUpOnClosedSessionWithConflict() throws Exception {
        UUID sessionId = createAndCloseSession();

        mockMvc.perform(post("/api/sessions/{sessionId}/messages", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MessageRequest("关闭后还能继续追问吗？"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("SESSION_ALREADY_CLOSED"))
                .andExpect(jsonPath("$.message").value("该会话已关闭，请开启新会话"));
    }

    @Test
    void shouldKeepContextAcrossFiveFollowUpRounds() throws Exception {
        UUID sessionId = createSession(buildCreateRequest());

        String[] followUps = {
                "第一轮我想确认推进节奏",
                "第二轮如果对方回复慢怎么办",
                "第三轮我需要先推进合同还是先推进报价",
                "第四轮如果对方临时变更需求怎么办",
                "第五轮请结合最开始的合作问题，告诉我下一步重点"
        };

        for (String followUp : followUps) {
            mockMvc.perform(post("/api/sessions/{sessionId}/messages", sessionId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MessageRequest(followUp))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.analysis.analysis.conclusion").isNotEmpty());
        }

        mockMvc.perform(get("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(6))
                .andExpect(jsonPath("$.data.messages", hasSize(12)))
                .andExpect(jsonPath("$.data.messages[11].content").value(containsString("这次合作推进会不会顺利")))
                .andExpect(jsonPath("$.data.messages[11].content").value(containsString("第五轮请结合最开始的合作问题，告诉我下一步重点")));
    }

    @Test
    void shouldReturnAtLeastTwoActionSuggestionsForNegativeConclusion() throws Exception {
        SessionCreateRequest request = buildCreateRequest();
        request.setQuestionText("如果这次合作最后失败了，我该怎么应对");

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.analysis.analysis.conclusion").value("综合来看，当前阻力偏大，需先稳住节奏再谋推进。"))
                .andExpect(jsonPath("$.data.analysis.analysis.emotionalTone").value("CAUTIOUS"))
                .andExpect(jsonPath("$.data.analysis.analysis.actionPlan.length()").value(greaterThanOrEqualTo(2)));
    }

    @Test
    void shouldReturnTooManyRequestsWhenAnonymousDailyQuotaIsExceeded() throws Exception {
        SessionCreateRequest anonymousRequest = buildCreateRequest();
        anonymousRequest.setUserId(null);

        for (int index = 0; index < 5; index++) {
            mockMvc.perform(post("/api/sessions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(anonymousRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(anonymousRequest)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"));
    }

    private SessionCreateRequest buildCreateRequest() {
        SessionCreateRequest request = new SessionCreateRequest();
        request.setUserId(1001L);
        request.setQuestionText("这次合作推进会不会顺利");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime("2026-04-12T10:00:00");
        request.setRawLines(java.util.List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(java.util.List.of(1, 5));
        return request;
    }

    private UUID createAndCloseSession() throws Exception {
        UUID sessionId = createSession(buildCreateRequest());

        mockMvc.perform(delete("/api/sessions/{sessionId}", sessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        return sessionId;
    }

    private UUID createSession(SessionCreateRequest request) throws Exception {
        String response = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return readSessionId(response);
    }

    private UUID readSessionId(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String sessionId = root.path("data").path("sessionId").asText(null);
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalStateException("sessionId not found in response");
        }
        return UUID.fromString(sessionId);
    }
}
