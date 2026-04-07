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

        String listResponse = mockMvc.perform(get("/api/cases/search")
                        .param("questionCategory", "合作")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].caseId").exists())
                .andExpect(jsonPath("$.data.items[0].useGod").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").path("items").get(0).path("caseId").asLong();

        mockMvc.perform(get("/api/cases/{caseId}", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId))
                .andExpect(jsonPath("$.data.questionCategory").value("合作"))
                .andExpect(jsonPath("$.data.chartSnapshot.useGod").value("应爻"))
                .andExpect(jsonPath("$.data.chartSnapshot.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.chartSnapshot.changedHexagram").value("风山渐"))
                .andExpect(jsonPath("$.data.chartSnapshot.palace").value("艮"))
                .andExpect(jsonPath("$.data.chartSnapshot.snapshotVersion").value("v1"))
                .andExpect(jsonPath("$.data.chartSnapshot.calendarVersion").value("v1"))
                .andExpect(jsonPath("$.data.chartSnapshot.mainUpperTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.chartSnapshot.mainLowerTrigram").isNotEmpty())
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].branch").value("卯"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[0].changeBranch").value("辰"))
                .andExpect(jsonPath("$.data.chartSnapshot.lines[4].changeLiuQin").value("父母"))
                .andExpect(jsonPath("$.data.ruleHits").isArray())
                .andExpect(jsonPath("$.data.ruleHits[0].evidence.useGod").isNotEmpty())
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("问合作")))
                .andExpect(jsonPath("$.data.analysis").value(org.hamcrest.Matchers.containsString("以应爻为用神")))
                .andExpect(jsonPath("$.data.analysisContext.contextVersion").value("v1"))
                .andExpect(jsonPath("$.data.analysisContext.useGod").value("应爻"))
                .andExpect(jsonPath("$.data.analysisContext.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.chartSnapshot.mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.analysisContext.ruleCodes").isArray());
    }

    @Test
    void shouldReturnUnifiedFailureWhenCaseNotFound() throws Exception {
        mockMvc.perform(get("/api/cases/{caseId}", 999999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("案例不存在"));
    }

    @Test
    void shouldSupportPagedSearchByCategory() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次出行会不会顺利");
        request.setQuestionCategory("出行");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 11, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/search")
                        .param("questionCategory", "出行")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].questionCategory").value("出行"))
                .andExpect(jsonPath("$.data.items[0].mainHexagram").value("山火贲"))
                .andExpect(jsonPath("$.data.items[0].changedHexagram").value("风山渐"))
                .andExpect(jsonPath("$.data.items[0].palace").value("艮"))
                .andExpect(jsonPath("$.data.items[0].useGod").value("父母"));
    }

    @Test
    void shouldClampInvalidPagingParametersOnSearch() throws Exception {
        mockMvc.perform(get("/api/cases/search")
                        .param("page", "0")
                        .param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(50))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void shouldAllowSearchWithoutCategory() throws Exception {
        mockMvc.perform(get("/api/cases/search")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(5))
                .andExpect(jsonPath("$.data.items").isArray());
    }
}
