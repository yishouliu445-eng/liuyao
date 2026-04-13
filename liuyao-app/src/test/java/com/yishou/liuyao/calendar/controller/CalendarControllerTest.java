package com.yishou.liuyao.calendar.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.calendar.dto.VerificationFeedbackSubmitRequest;
import com.yishou.liuyao.session.dto.SessionCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "spring.flyway.enabled=true")
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldAutoCreateCalendarEventAndSupportFeedbackSubmission() throws Exception {
        String createResponse = mockMvc.perform(post("/api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.analysis.analysis.predictedTimeline").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID sessionId = readSessionId(createResponse);
        LocalDate current = LocalDate.now();

        String monthlyResponse = mockMvc.perform(get("/api/calendar/events")
                        .param("userId", "2001")
                        .param("year", String.valueOf(current.plusMonths(2).getYear()))
                        .param("month", String.valueOf(current.plusMonths(2).getMonthValue()))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].eventId").value(notNullValue()))
                .andExpect(jsonPath("$.data.items[0].sessionId").value(sessionId.toString()))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID eventId = readEventId(monthlyResponse);

        mockMvc.perform(post("/api/calendar/events/{eventId}/feedback", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new VerificationFeedbackSubmitRequest(
                                "ACCURATE",
                                "两个月内确实有推进",
                                List.of("推进", "应验")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.status").value("VERIFIED"))
                .andExpect(jsonPath("$.data.feedbackSubmitted").value(true))
                .andExpect(jsonPath("$.data.feedbackAccuracy").value("ACCURATE"));

        mockMvc.perform(get("/api/calendar/timeline")
                        .param("userId", "2001")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.items[0].eventId").value(eventId.toString()))
                .andExpect(jsonPath("$.data.items[0].feedbackSubmitted").value(true));
    }

    private SessionCreateRequest buildCreateRequest() {
        SessionCreateRequest request = new SessionCreateRequest();
        request.setUserId(2001L);
        request.setQuestionText("这次合作什么时候能看到推进");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime("2026-04-12T10:00:00");
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));
        return request;
    }

    private UUID readSessionId(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return UUID.fromString(root.path("data").path("sessionId").asText());
    }

    private UUID readEventId(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        return UUID.fromString(root.path("data").path("items").get(0).path("eventId").asText());
    }
}
