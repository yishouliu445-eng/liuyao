package com.yishou.liuyao.ops.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionEnvelope;
import com.yishou.liuyao.analysis.validation.AnalysisValidationIssue;
import com.yishou.liuyao.analysis.validation.AnalysisValidationResult;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.ops.audit.domain.AnalysisRun;
import com.yishou.liuyao.ops.audit.domain.AnalysisRunCitation;
import com.yishou.liuyao.ops.audit.domain.AnalysisRunIssue;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunCitationRepository;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunIssueRepository;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ExecutionAuditService {

    private static final Logger log = LoggerFactory.getLogger(ExecutionAuditService.class);

    private final AnalysisRunRepository analysisRunRepository;
    private final AnalysisRunIssueRepository analysisRunIssueRepository;
    private final AnalysisRunCitationRepository analysisRunCitationRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public ExecutionAuditService(AnalysisRunRepository analysisRunRepository,
                                 AnalysisRunIssueRepository analysisRunIssueRepository,
                                 AnalysisRunCitationRepository analysisRunCitationRepository,
                                 ObjectMapper objectMapper) {
        this.analysisRunRepository = analysisRunRepository;
        this.analysisRunIssueRepository = analysisRunIssueRepository;
        this.analysisRunCitationRepository = analysisRunCitationRepository;
        this.objectMapper = objectMapper;
    }

    public ExecutionAuditService(AnalysisRunRepository analysisRunRepository,
                                 AnalysisRunIssueRepository analysisRunIssueRepository,
                                 AnalysisRunCitationRepository analysisRunCitationRepository) {
        this(analysisRunRepository, analysisRunIssueRepository, analysisRunCitationRepository, new ObjectMapper());
    }

    public void record(AnalysisExecutionEnvelope envelope, Object validationResult) {
        record(envelope, toValidationResult(validationResult));
    }

    @Transactional
    public void record(AnalysisExecutionEnvelope envelope, AnalysisValidationResult validationResult) {
        try {
            if (envelope == null || envelope.getExecutionId() == null
                    || analysisRunRepository.existsByExecutionId(envelope.getExecutionId())) {
                return;
            }

            AnalysisRun run = new AnalysisRun();
            run.setExecutionId(envelope.getExecutionId());
            run.setExecutionMode(envelope.getMode() == null ? "UNKNOWN" : envelope.getMode().name());
            run.setPromptVersion(envelope.getVersions() == null ? null : envelope.getVersions().getPromptVersion());
            run.setModelVersion(envelope.getVersions() == null ? null : envelope.getVersions().getModelVersion());
            run.setQuestionCategory(envelope.getChartSnapshot() == null ? null : envelope.getChartSnapshot().getQuestionCategory());
            run.setQuestionText(envelope.getChartSnapshot() == null ? null : envelope.getChartSnapshot().getQuestion());
            if (envelope.getChartSnapshot() != null) {
                run.setUseGod(envelope.getChartSnapshot().getUseGod());
                run.setMainHexagram(envelope.getChartSnapshot().getMainHexagram());
                run.setChangedHexagram(envelope.getChartSnapshot().getChangedHexagram());
            }
            if (envelope.getDegradation() != null) {
                run.setDegradationLevel(envelope.getDegradation().getLevel());
                run.setDegradationReasons(joinValues(envelope.getDegradation().getReasons()));
            }
            run.setValidationIssueCount(validationResult == null || validationResult.getIssues() == null
                    ? 0
                    : validationResult.getIssues().size());
            run.setCitationCount(resolveCitationCount(envelope));
            run.setAnalysisConclusion(readConclusion(envelope.getAnalysisOutput()));
            run.setLegacyAnalysisText(envelope.getLegacyAnalysisText());
            run = analysisRunRepository.save(run);

            if (validationResult != null && validationResult.getIssues() != null) {
                for (AnalysisValidationIssue issue : validationResult.getIssues()) {
                    if (issue == null) {
                        continue;
                    }
                    AnalysisRunIssue row = new AnalysisRunIssue();
                    row.setAnalysisRun(run);
                    row.setIssueCode(issue.getCode());
                    row.setIssueMessage(issue.getMessage());
                    row.setSeverity(issue.getSeverity());
                    analysisRunIssueRepository.save(row);
                }
            }

            if (hasCitations(envelope.getAnalysisOutput())) {
                for (AnalysisOutputDTO.ClassicReference reference : envelope.getAnalysisOutput().getAnalysis().getClassicReferences()) {
                    AnalysisRunCitation row = new AnalysisRunCitation();
                    row.setAnalysisRun(run);
                    row.setCitationId(reference.getCitationId());
                    row.setChunkId(reference.getChunkId());
                    row.setBookId(reference.getBookId());
                    row.setReferenceSource(reference.getSource());
                    row.setReferenceQuote(reference.getQuote());
                    row.setReferenceRelevance(reference.getRelevance());
                    EvidenceHit matchedEvidence = findMatchingEvidence(reference, envelope.getEvidenceHits());
                    if (matchedEvidence != null) {
                        row.setMatchedSourceTitle(matchedEvidence.getSourceTitle());
                        row.setMatchedChapterTitle(matchedEvidence.getChapterTitle());
                    }
                    analysisRunCitationRepository.save(row);
                }
            }
        } catch (Exception exception) {
            log.warn("执行审计落库失败（不影响主流程）: {}", exception.getMessage());
        }
    }

    private AnalysisValidationResult toValidationResult(Object validationResult) {
        if (validationResult instanceof AnalysisValidationResult typed) {
            return typed;
        }
        AnalysisValidationResult result = new AnalysisValidationResult();
        if (validationResult instanceof Map<?, ?> map) {
            Object issues = map.get("issues");
            if (issues instanceof List<?> issueList) {
                List<AnalysisValidationIssue> converted = new ArrayList<>();
                for (Object issue : issueList) {
                    AnalysisValidationIssue convertedIssue = toValidationIssue(issue);
                    if (convertedIssue != null) {
                        converted.add(convertedIssue);
                    }
                }
                result.setIssues(converted);
            }
        }
        return result;
    }

    private AnalysisValidationIssue toValidationIssue(Object issue) {
        if (issue == null) {
            return null;
        }
        if (issue instanceof AnalysisValidationIssue typed) {
            return typed;
        }
        AnalysisValidationIssue converted = new AnalysisValidationIssue();
        if (issue instanceof Map<?, ?> map) {
            Object code = map.get("code");
            Object message = map.get("message");
            Object severity = map.get("severity");
            converted.setCode(code == null ? null : String.valueOf(code));
            converted.setMessage(message == null ? null : String.valueOf(message));
            converted.setSeverity(severity == null ? null : String.valueOf(severity));
            return converted;
        }
        return null;
    }

    private int resolveCitationCount(AnalysisExecutionEnvelope envelope) {
        if (!hasCitations(envelope.getAnalysisOutput())) {
            return envelope.getEvidenceHits() == null ? 0 : envelope.getEvidenceHits().size();
        }
        return envelope.getAnalysisOutput().getAnalysis().getClassicReferences().size();
    }

    private String readConclusion(AnalysisOutputDTO output) {
        if (output == null || output.getAnalysis() == null) {
            return null;
        }
        return output.getAnalysis().getConclusion();
    }

    private EvidenceHit findMatchingEvidence(AnalysisOutputDTO.ClassicReference reference, List<EvidenceHit> evidenceHits) {
        if (reference == null || evidenceHits == null) {
            return null;
        }
        for (EvidenceHit evidenceHit : evidenceHits) {
            if (reference.getCitationId() != null && Objects.equals(reference.getCitationId(), evidenceHit.getCitationId())) {
                return evidenceHit;
            }
            if (reference.getChunkId() != null && Objects.equals(reference.getChunkId(), evidenceHit.getChunkId())) {
                return evidenceHit;
            }
        }
        return null;
    }

    private boolean hasCitations(AnalysisOutputDTO output) {
        return output != null
                && output.getAnalysis() != null
                && output.getAnalysis().getClassicReferences() != null
                && !output.getAnalysis().getClassicReferences().isEmpty();
    }

    private String joinValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        List<String> cleaned = values.stream().filter(Objects::nonNull).toList();
        if (cleaned.isEmpty()) {
            return null;
        }
        return String.join(" | ", cleaned);
    }
}
