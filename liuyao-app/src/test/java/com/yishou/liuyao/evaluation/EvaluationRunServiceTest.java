package com.yishou.liuyao.evaluation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayDTO;
import com.yishou.liuyao.evaluation.domain.EvaluationRun;
import com.yishou.liuyao.evaluation.dto.EvaluationScenario;
import com.yishou.liuyao.evaluation.dto.EvaluationScoreCard;
import com.yishou.liuyao.evaluation.repository.EvaluationRunRepository;
import com.yishou.liuyao.evaluation.service.EvaluationDatasetRegistry;
import com.yishou.liuyao.evaluation.service.EvaluationRunService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EvaluationRunServiceTest {

    @Test
    void shouldBuildPromptRegressionScoreCard() {
        EvaluationRunService service = new EvaluationRunService(
                new EvaluationDatasetRegistry(),
                emptyRepositoryProvider(),
                new ObjectMapper()
        );
        AnalysisOutputDTO output = new AnalysisOutputDTO();
        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("可以推进");
        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setCitationId("chunk:21");
        analysis.setClassicReferences(java.util.List.of(reference));
        output.setAnalysis(analysis);
        AnalysisOutputDTO.AnalysisMetadata metadata = new AnalysisOutputDTO.AnalysisMetadata();
        metadata.setRagSourceCount(2);
        output.setMetadata(metadata);

        EvaluationScoreCard scoreCard = service.evaluatePromptRegression("case-01", "合作", output);

        assertEquals("PROMPT_REGRESSION", scoreCard.getDatasetType());
        assertEquals(2, scoreCard.getHitCount());
        assertEquals(1, scoreCard.getCitationCount());
        assertEquals(1, scoreCard.getMatchedCitationCount());
        assertTrue(scoreCard.getPassed());
        assertNotNull(scoreCard.getSummary());
    }

    @Test
    void shouldConvertReplayToEvaluationScenario() {
        EvaluationRunService service = new EvaluationRunService(
                new EvaluationDatasetRegistry(),
                emptyRepositoryProvider(),
                new ObjectMapper()
        );
        CaseReplayDTO replay = new CaseReplayDTO();
        replay.setCaseId(101L);
        replay.setReplayRuleVersion("v1");
        replay.setBaselineRuleVersion("legacy-v0");
        replay.setRuleBundleVersion("bundle-v1");
        replay.setResultLevelChanged(Boolean.TRUE);
        AnalysisContextDTO replayContext = new AnalysisContextDTO();
        replayContext.setQuestion("这次合作推进会不会反复");
        replayContext.setQuestionCategory("合作");
        replay.setReplayAnalysisContext(replayContext);

        EvaluationScenario scenario = service.fromReplay(replay);

        assertEquals("case:101", scenario.getScenarioId());
        assertEquals("CASE_REPLAY", scenario.getScenarioType());
        assertEquals("合作", scenario.getQuestionCategory());
        assertEquals("v1", scenario.getRuleVersion());
        assertEquals("bundle-v1", scenario.getMetadata().get("ruleBundleVersion"));
    }

    @Test
    void shouldBuildRetrievalScoreCard() {
        EvaluationRunService service = new EvaluationRunService(
                new EvaluationDatasetRegistry(),
                emptyRepositoryProvider(),
                new ObjectMapper()
        );

        EvaluationScoreCard scoreCard = service.evaluateRetrievalSelection("rag-01", 4, 2, 1);

        assertEquals("RAG_RECALL", scoreCard.getDatasetType());
        assertEquals(4, scoreCard.getHitCount());
        assertEquals(0.5D, scoreCard.getSelectedCitationRate());
        assertEquals(0.5D, scoreCard.getCitationMismatchRate());
    }

    @Test
    void shouldSkipPersistenceWhenRepositorySaveFails() {
        EvaluationRunRepository repository = mock(EvaluationRunRepository.class);
        when(repository.save(any(EvaluationRun.class))).thenThrow(new RuntimeException("relation evaluation_run does not exist"));
        EvaluationRunService service = new EvaluationRunService(
                new EvaluationDatasetRegistry(),
                repositoryProvider(repository),
                new ObjectMapper()
        );
        EvaluationScenario scenario = new EvaluationScenario();
        scenario.setScenarioId("prompt-01");
        scenario.setScenarioType("PROMPT_REGRESSION");
        EvaluationScoreCard scoreCard = new EvaluationScoreCard();
        scoreCard.setScenarioId("prompt-01");
        scoreCard.setDatasetType("PROMPT_REGRESSION");
        scoreCard.setPassed(Boolean.TRUE);
        scoreCard.setSummary("ok");

        EvaluationRun persisted = service.recordRun("PROMPT_REGRESSION", scenario, scoreCard);
        EvaluationRun persistedAgain = service.recordRun("PROMPT_REGRESSION", scenario, scoreCard);

        assertNull(persisted);
        assertNull(persistedAgain);
        verify(repository, times(1)).save(any(EvaluationRun.class));
    }

    private ObjectProvider<com.yishou.liuyao.evaluation.repository.EvaluationRunRepository> emptyRepositoryProvider() {
        return repositoryProvider(null);
    }

    private ObjectProvider<EvaluationRunRepository> repositoryProvider(EvaluationRunRepository repository) {
        return new ObjectProvider<>() {
            @Override
            public EvaluationRunRepository getObject(Object... args) {
                return repository;
            }

            @Override
            public EvaluationRunRepository getIfAvailable() {
                return repository;
            }

            @Override
            public EvaluationRunRepository getIfUnique() {
                return repository;
            }

            @Override
            public EvaluationRunRepository getObject() {
                return repository;
            }
        };
    }
}
