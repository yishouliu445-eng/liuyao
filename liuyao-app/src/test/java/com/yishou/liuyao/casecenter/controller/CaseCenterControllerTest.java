package com.yishou.liuyao.casecenter.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "spring.flyway.enabled=true")
class CaseCenterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldListAndLoadCaseDetailThroughHttpEndpoints() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作签约能不能顺利推进");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 10, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].caseId").exists())
                .andExpect(jsonPath("$.data[0].useGod").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").get(0).path("caseId").asLong();

        mockMvc.perform(get("/api/cases/{caseId}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId))
                .andExpect(jsonPath("$.data.questionCategory").value("合作"))
                .andExpect(jsonPath("$.data.chartSnapshot.useGod").value("应爻"))
                .andExpect(jsonPath("$.data.ruleHits").isArray())
                .andExpect(jsonPath("$.data.analysis").isNotEmpty());
    }

    @Test
    void shouldReturnUnifiedFailureWhenCaseNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/{caseId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("案例不存在"));
    }
}
