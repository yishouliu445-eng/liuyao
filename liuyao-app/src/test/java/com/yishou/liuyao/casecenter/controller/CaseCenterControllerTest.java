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
                .andExpect(status().isNotFound())
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
    void shouldReplayCaseThroughHttpEndpoint() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作推进会不会反复");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 15, 10, 0));
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").path("items").get(0).path("caseId").asLong();

        mockMvc.perform(get("/api/cases/{caseId}/replay", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId))
                .andExpect(jsonPath("$.data.baselineRuleCodes").isArray())
                .andExpect(jsonPath("$.data.replayRuleCodes").isArray())
                .andExpect(jsonPath("$.data.baselineEffectiveRuleCodes").isArray())
                .andExpect(jsonPath("$.data.replayEffectiveRuleCodes").isArray())
                .andExpect(jsonPath("$.data.baselineSuppressedRuleCodes").isArray())
                .andExpect(jsonPath("$.data.replaySuppressedRuleCodes").isArray())
                .andExpect(jsonPath("$.data.baselineTags").isArray())
                .andExpect(jsonPath("$.data.replayTags").isArray())
                .andExpect(jsonPath("$.data.baselineEffectiveScore").isNumber())
                .andExpect(jsonPath("$.data.replayEffectiveScore").isNumber())
                .andExpect(jsonPath("$.data.effectiveScoreDelta").isNumber())
                .andExpect(jsonPath("$.data.baselineRuleVersion").isNotEmpty())
                .andExpect(jsonPath("$.data.replayRuleVersion").isNotEmpty())
                .andExpect(jsonPath("$.data.replayUseGodConfigVersion").isNotEmpty())
                .andExpect(jsonPath("$.data.ruleBundleVersion").value("v1"))
                .andExpect(jsonPath("$.data.ruleDefinitionsVersion").value("v1"))
                .andExpect(jsonPath("$.data.useGodRulesVersion").value("v1"))
                .andExpect(jsonPath("$.data.resultLevelChanged").isBoolean())
                .andExpect(jsonPath("$.data.baselineAnalysisContext").exists())
                .andExpect(jsonPath("$.data.baselineStructuredResult").exists())
                .andExpect(jsonPath("$.data.baselineRuleHits").isArray())
                .andExpect(jsonPath("$.data.baselineSummary").isNotEmpty())
                .andExpect(jsonPath("$.data.replaySummary").isNotEmpty())
                .andExpect(jsonPath("$.data.summaryChanged").isBoolean())
                .andExpect(jsonPath("$.data.analysisChanged").isBoolean())
                .andExpect(jsonPath("$.data.recommendPersistReplay").isBoolean())
                .andExpect(jsonPath("$.data.persistenceAssessment").isNotEmpty())
                .andExpect(jsonPath("$.data.replayRuleHits").isArray())
                .andExpect(jsonPath("$.data.replayAnalysis").value(org.hamcrest.Matchers.containsString("合作")))
                .andExpect(jsonPath("$.data.replayAnalysisContext.useGod").value("应爻"));
    }

    @Test
    void shouldExposeReplayPersistenceAssessmentEndpoint() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作复盘值不值得留历史");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 17, 10, 0));
        request.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        request.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/cases/replay-assessments")
                        .param("questionCategory", "合作")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].caseId").exists())
                .andExpect(jsonPath("$.data.items[0].questionCategory").value("合作"))
                .andExpect(jsonPath("$.data.items[0].recommendPersistReplay").isBoolean())
                .andExpect(jsonPath("$.data.items[0].persistenceAssessment").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].ruleBundleVersion").value("v1"))
                .andExpect(jsonPath("$.data.items[0].replayRuleVersion").isNotEmpty());
    }

    @Test
    void shouldPersistAndListReplayRunsThroughHttpEndpoints() throws Exception {
        DivinationAnalyzeRequest request = new DivinationAnalyzeRequest();
        request.setQuestionText("这次合作 replay run 需要保留吗");
        request.setQuestionCategory("合作");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime(LocalDateTime.of(2026, 4, 19, 10, 0));
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
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").path("items").get(0).path("caseId").asLong();

        mockMvc.perform(post("/api/cases/{caseId}/replay-runs", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.caseId").value(caseId))
                .andExpect(jsonPath("$.data.replayRunId").exists())
                .andExpect(jsonPath("$.data.ruleBundleVersion").value("v1"))
                .andExpect(jsonPath("$.data.payloadJson").isNotEmpty());

        mockMvc.perform(get("/api/cases/{caseId}/replay-runs", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].caseId").value(caseId))
                .andExpect(jsonPath("$.data[0].replayRuleVersion").isNotEmpty());
    }

    @Test
    void shouldSearchReplayRunsThroughHttpEndpoint() throws Exception {
        DivinationAnalyzeRequest coopRequest = new DivinationAnalyzeRequest();
        coopRequest.setQuestionText("这次合作 replay run 需要全局筛选吗");
        coopRequest.setQuestionCategory("合作");
        coopRequest.setDivinationMethod("手工起卦");
        coopRequest.setDivinationTime(LocalDateTime.of(2026, 4, 22, 10, 0));
        coopRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        coopRequest.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coopRequest)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/cases/search")
                        .param("questionCategory", "合作")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").path("items").get(0).path("caseId").asLong();

        mockMvc.perform(post("/api/cases/{caseId}/replay-runs", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cases/replay-runs/search")
                        .param("questionCategory", "合作")
                        .param("recommendPersistReplay", "false")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[0].caseId").exists())
                .andExpect(jsonPath("$.data.items[0].questionCategory").value("合作"))
                .andExpect(jsonPath("$.data.items[0].recommendPersistReplay").value(false))
                .andExpect(jsonPath("$.data.items[0].replayRunId").exists());
    }

    @Test
    void shouldExposeReplayRunStatsThroughHttpEndpoint() throws Exception {
        DivinationAnalyzeRequest coopRequest = new DivinationAnalyzeRequest();
        coopRequest.setQuestionText("这次合作 replay run 统计接口可见吗");
        coopRequest.setQuestionCategory("合作");
        coopRequest.setDivinationMethod("手工起卦");
        coopRequest.setDivinationTime(LocalDateTime.of(2026, 4, 25, 10, 0));
        coopRequest.setRawLines(List.of("老阳", "少阴", "少阳", "少阴", "老阴", "少阳"));
        coopRequest.setMovingLines(List.of(1, 5));

        mockMvc.perform(post("/api/divinations/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(coopRequest)))
                .andExpect(status().isOk());

        String listResponse = mockMvc.perform(get("/api/cases/search")
                        .param("questionCategory", "合作")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long caseId = objectMapper.readTree(listResponse).path("data").path("items").get(0).path("caseId").asLong();

        mockMvc.perform(post("/api/cases/{caseId}/replay-runs", caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/cases/replay-runs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRuns").isNumber())
                .andExpect(jsonPath("$.data.recommendPersistRuns").isNumber())
                .andExpect(jsonPath("$.data.observeOnlyRuns").isNumber())
                .andExpect(jsonPath("$.data.categoryStats").isArray())
                .andExpect(jsonPath("$.data.categoryStats[0].questionCategory").isNotEmpty())
                .andExpect(jsonPath("$.data.categoryStats[0].runCount").isNumber());
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
