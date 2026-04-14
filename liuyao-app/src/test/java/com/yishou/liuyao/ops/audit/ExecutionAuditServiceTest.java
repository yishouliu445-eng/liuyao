package com.yishou.liuyao.ops.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionDegradation;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionEnvelope;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionMode;
import com.yishou.liuyao.analysis.runtime.AnalysisExecutionVersions;
import com.yishou.liuyao.analysis.validation.AnalysisValidationIssue;
import com.yishou.liuyao.analysis.validation.AnalysisValidationResult;
import com.yishou.liuyao.evidence.dto.EvidenceHit;
import com.yishou.liuyao.ops.audit.domain.AnalysisRun;
import com.yishou.liuyao.ops.audit.domain.AnalysisRunCitation;
import com.yishou.liuyao.ops.audit.domain.AnalysisRunIssue;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunCitationRepository;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunIssueRepository;
import com.yishou.liuyao.ops.audit.repository.AnalysisRunRepository;
import com.yishou.liuyao.ops.audit.service.ExecutionAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionAuditServiceTest {

    @Mock
    private AnalysisRunRepository analysisRunRepository;
    @Mock
    private AnalysisRunIssueRepository analysisRunIssueRepository;
    @Mock
    private AnalysisRunCitationRepository analysisRunCitationRepository;

    private ExecutionAuditService executionAuditService;

    @BeforeEach
    void setUp() {
        executionAuditService = new ExecutionAuditService(
                analysisRunRepository,
                analysisRunIssueRepository,
                analysisRunCitationRepository,
                new ObjectMapper()
        );
    }

    @Test
    void shouldPersistExecutionRunIssuesAndCitations() {
        UUID executionId = UUID.randomUUID();
        AnalysisExecutionEnvelope envelope = sampleEnvelope(executionId);
        AnalysisValidationResult validationResult = new AnalysisValidationResult();
        validationResult.setIssues(List.of(
                new AnalysisValidationIssue("RESULT_DIRECTION_CONFLICT", "结论方向与结构化结果冲突", "WARN")
        ));

        when(analysisRunRepository.existsByExecutionId(executionId)).thenReturn(false);
        when(analysisRunRepository.save(any(AnalysisRun.class))).thenAnswer(invocation -> {
            AnalysisRun run = invocation.getArgument(0);
            run.setId(101L);
            return run;
        });
        when(analysisRunIssueRepository.save(any(AnalysisRunIssue.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(analysisRunCitationRepository.save(any(AnalysisRunCitation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        executionAuditService.record(envelope, validationResult);

        ArgumentCaptor<AnalysisRun> runCaptor = ArgumentCaptor.forClass(AnalysisRun.class);
        verify(analysisRunRepository).save(runCaptor.capture());
        AnalysisRun savedRun = runCaptor.getValue();
        assertEquals(executionId, savedRun.getExecutionId());
        assertEquals("WARN", savedRun.getDegradationLevel());
        assertEquals("RESULT_DIRECTION_CONFLICT", savedRun.getDegradationReasons());
        assertEquals(1, savedRun.getValidationIssueCount());
        assertEquals(1, savedRun.getCitationCount());
        assertEquals("走势仍可推进。", savedRun.getAnalysisConclusion());
        assertNull(savedRun.getLegacyAnalysisText());

        ArgumentCaptor<AnalysisRunIssue> issueCaptor = ArgumentCaptor.forClass(AnalysisRunIssue.class);
        verify(analysisRunIssueRepository).save(issueCaptor.capture());
        assertEquals(101L, issueCaptor.getValue().getAnalysisRunId());
        assertEquals("RESULT_DIRECTION_CONFLICT", issueCaptor.getValue().getIssueCode());
        assertEquals("WARN", issueCaptor.getValue().getSeverity());

        ArgumentCaptor<AnalysisRunCitation> citationCaptor = ArgumentCaptor.forClass(AnalysisRunCitation.class);
        verify(analysisRunCitationRepository).save(citationCaptor.capture());
        assertEquals(101L, citationCaptor.getValue().getAnalysisRunId());
        assertEquals("chunk:21", citationCaptor.getValue().getCitationId());
        assertEquals("增删卜易", citationCaptor.getValue().getMatchedSourceTitle());
        assertEquals("用神章", citationCaptor.getValue().getMatchedChapterTitle());
    }

    @Test
    void shouldSkipDuplicateExecutionId() {
        UUID executionId = UUID.randomUUID();

        when(analysisRunRepository.existsByExecutionId(executionId)).thenReturn(true);

        executionAuditService.record(sampleEnvelope(executionId), new AnalysisValidationResult());

        verify(analysisRunRepository, times(0)).save(any(AnalysisRun.class));
        verify(analysisRunIssueRepository, times(0)).save(any(AnalysisRunIssue.class));
        verify(analysisRunCitationRepository, times(0)).save(any(AnalysisRunCitation.class));
    }

    private AnalysisExecutionEnvelope sampleEnvelope(UUID executionId) {
        AnalysisExecutionEnvelope envelope = new AnalysisExecutionEnvelope();
        envelope.setExecutionId(executionId);
        envelope.setMode(AnalysisExecutionMode.INITIAL);
        envelope.setVersions(sampleVersions());
        envelope.setDegradation(sampleDegradation());
        envelope.setAnalysisOutput(sampleOutput());
        envelope.setEvidenceHits(List.of(sampleEvidenceHit()));
        return envelope;
    }

    private AnalysisExecutionVersions sampleVersions() {
        AnalysisExecutionVersions versions = new AnalysisExecutionVersions();
        versions.setPromptVersion("v1");
        versions.setModelVersion("mock");
        return versions;
    }

    private AnalysisExecutionDegradation sampleDegradation() {
        AnalysisExecutionDegradation degradation = new AnalysisExecutionDegradation();
        degradation.setLevel("WARN");
        degradation.setReasons(List.of("RESULT_DIRECTION_CONFLICT"));
        return degradation;
    }

    private AnalysisOutputDTO sampleOutput() {
        AnalysisOutputDTO.ClassicReference reference = new AnalysisOutputDTO.ClassicReference();
        reference.setSource("《增删卜易·用神章》");
        reference.setQuote("用神旺相则事可成");
        reference.setRelevance("用于说明走势向好");
        reference.setCitationId("chunk:21");
        reference.setChunkId(21L);
        reference.setBookId(1L);

        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion("走势仍可推进。");
        analysis.setActionPlan(List.of("继续推进"));
        analysis.setEmotionalTone("ENCOURAGING");
        analysis.setClassicReferences(List.of(reference));

        AnalysisOutputDTO.AnalysisMetadata metadata = new AnalysisOutputDTO.AnalysisMetadata();
        metadata.setConfidence(0.8);
        metadata.setModelUsed("mock");
        metadata.setRagSourceCount(1);
        metadata.setProcessingTimeMs(12);

        AnalysisOutputDTO output = new AnalysisOutputDTO();
        output.setAnalysis(analysis);
        output.setMetadata(metadata);
        output.setSmartPrompts(List.of("下一步怎么做"));
        return output;
    }

    private EvidenceHit sampleEvidenceHit() {
        EvidenceHit hit = new EvidenceHit();
        hit.setCitationId("chunk:21");
        hit.setChunkId(21L);
        hit.setBookId(1L);
        hit.setSourceTitle("增删卜易");
        hit.setChapterTitle("用神章");
        hit.setContent("用神旺相则事可成");
        return hit;
    }
}
