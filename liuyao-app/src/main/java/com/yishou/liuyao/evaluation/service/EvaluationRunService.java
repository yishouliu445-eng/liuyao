package com.yishou.liuyao.evaluation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.casecenter.dto.CaseReplayDTO;
import com.yishou.liuyao.evaluation.domain.EvaluationRun;
import com.yishou.liuyao.evaluation.dto.EvaluationScenario;
import com.yishou.liuyao.evaluation.dto.EvaluationScoreCard;
import com.yishou.liuyao.evaluation.repository.EvaluationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class EvaluationRunService {

    private static final Logger log = LoggerFactory.getLogger(EvaluationRunService.class);

    private final EvaluationDatasetRegistry evaluationDatasetRegistry;
    private final ObjectProvider<EvaluationRunRepository> evaluationRunRepositoryProvider;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean persistenceEnabled = new AtomicBoolean(true);

    public EvaluationRunService(EvaluationDatasetRegistry evaluationDatasetRegistry,
                                ObjectProvider<EvaluationRunRepository> evaluationRunRepositoryProvider,
                                ObjectMapper objectMapper) {
        this.evaluationDatasetRegistry = evaluationDatasetRegistry;
        this.evaluationRunRepositoryProvider = evaluationRunRepositoryProvider;
        this.objectMapper = objectMapper;
    }

    public EvaluationScoreCard evaluatePromptRegression(String scenarioId,
                                                        String questionCategory,
                                                        AnalysisOutputDTO output) {
        int hitCount = output != null && output.getMetadata() != null ? output.getMetadata().getRagSourceCount() : 0;
        int citationCount = output != null
                && output.getAnalysis() != null
                && output.getAnalysis().getClassicReferences() != null
                ? output.getAnalysis().getClassicReferences().size()
                : 0;
        int matchedCitationCount = output != null
                && output.getAnalysis() != null
                && output.getAnalysis().getClassicReferences() != null
                ? (int) output.getAnalysis().getClassicReferences().stream()
                .filter(reference -> reference.getCitationId() != null || reference.getChunkId() != null || reference.getBookId() != null)
                .count()
                : 0;
        EvaluationScoreCard scoreCard = buildScoreCard(
                scenarioId,
                "PROMPT_REGRESSION",
                hitCount,
                citationCount,
                matchedCitationCount,
                output != null
                        && output.getAnalysis() != null
                        && output.getAnalysis().getConclusion() != null
                        && !output.getAnalysis().getConclusion().isBlank()
        );
        recordRun("PROMPT_REGRESSION", buildPromptScenario(scenarioId, questionCategory), scoreCard);
        return scoreCard;
    }

    public EvaluationScenario fromReplay(CaseReplayDTO replay) {
        EvaluationScenario scenario = new EvaluationScenario();
        scenario.setScenarioId("case:" + replay.getCaseId());
        scenario.setScenarioType("CASE_REPLAY");
        scenario.setQuestionCategory(replay.getReplayAnalysisContext() == null ? null : replay.getReplayAnalysisContext().getQuestionCategory());
        scenario.setQuestionText(replay.getReplayAnalysisContext() == null ? null : replay.getReplayAnalysisContext().getQuestion());
        scenario.setRuleVersion(replay.getReplayRuleVersion());
        scenario.getMetadata().put("baselineRuleVersion", replay.getBaselineRuleVersion());
        scenario.getMetadata().put("ruleBundleVersion", replay.getRuleBundleVersion());
        scenario.getMetadata().put("resultLevelChanged", replay.getResultLevelChanged());
        return scenario;
    }

    public EvaluationScoreCard evaluateRetrievalSelection(String scenarioId,
                                                          int hitCount,
                                                          int citationCount,
                                                          int matchedCitationCount) {
        return buildScoreCard(
                scenarioId,
                "RAG_RECALL",
                hitCount,
                citationCount,
                matchedCitationCount,
                hitCount > 0 && matchedCitationCount > 0
        );
    }

    public EvaluationRun recordRun(String datasetType,
                                   EvaluationScenario scenario,
                                   EvaluationScoreCard scoreCard) {
        if (!evaluationDatasetRegistry.supports(datasetType) || scenario == null || scoreCard == null) {
            return null;
        }
        if (!persistenceEnabled.get()) {
            return null;
        }
        EvaluationRunRepository repository = evaluationRunRepositoryProvider.getIfAvailable();
        if (repository == null) {
            return null;
        }

        EvaluationRun entity = new EvaluationRun();
        entity.setDatasetType(datasetType);
        entity.setScenarioType(scenario.getScenarioType());
        entity.setScenarioId(scenario.getScenarioId());
        entity.setQuestionCategory(scenario.getQuestionCategory());
        entity.setPassed(Boolean.TRUE.equals(scoreCard.getPassed()));
        entity.setSummary(scoreCard.getSummary());
        entity.setScoreCardJson(writeJson(scoreCard));
        entity.setSelectedCitationRate(scoreCard.getSelectedCitationRate());
        entity.setCitationMismatchRate(scoreCard.getCitationMismatchRate());
        try {
            return repository.save(entity);
        } catch (RuntimeException exception) {
            if (isMissingRelation(exception)) {
                persistenceEnabled.set(false);
            }
            log.warn("评估结果落库失败，已跳过持久化: {}", exception.getMessage());
            return null;
        }
    }

    private EvaluationScenario buildPromptScenario(String scenarioId, String questionCategory) {
        EvaluationScenario scenario = new EvaluationScenario();
        scenario.setScenarioId(scenarioId);
        scenario.setScenarioType("PROMPT_REGRESSION");
        scenario.setQuestionCategory(questionCategory);
        return scenario;
    }

    private EvaluationScoreCard buildScoreCard(String scenarioId,
                                               String datasetType,
                                               int hitCount,
                                               int citationCount,
                                               int matchedCitationCount,
                                               boolean passed) {
        EvaluationScoreCard scoreCard = new EvaluationScoreCard();
        scoreCard.setScenarioId(scenarioId);
        scoreCard.setDatasetType(datasetType);
        scoreCard.setPassed(passed);
        scoreCard.setHitCount(hitCount);
        scoreCard.setCitationCount(citationCount);
        scoreCard.setMatchedCitationCount(matchedCitationCount);
        scoreCard.setSelectedCitationRate(citationCount == 0 ? 1.0D : (double) matchedCitationCount / citationCount);
        scoreCard.setCitationMismatchRate(citationCount == 0 ? 0.0D : (double) (citationCount - matchedCitationCount) / citationCount);
        scoreCard.setSummary("dataset=" + datasetType
                + ", hits=" + hitCount
                + ", citations=" + matchedCitationCount + "/" + citationCount);
        return scoreCard;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{}";
        }
    }

    private boolean isMissingRelation(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("relation \"evaluation_run\" does not exist")
                    || message.contains("relation evaluation_run does not exist")
                    || message.contains("Table \"EVALUATION_RUN\" not found"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
