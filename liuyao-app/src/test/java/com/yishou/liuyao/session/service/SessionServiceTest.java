package com.yishou.liuyao.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.service.AnalysisContextFactory;
import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.analysis.service.OrchestratedAnalysisService;
import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.domain.DivinationInput;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.divination.service.ChartBuilderService;
import com.yishou.liuyao.infrastructure.ratelimit.RateLimiter;
import com.yishou.liuyao.knowledge.service.KnowledgeSearchService;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.service.RuleEngineService;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import com.yishou.liuyao.session.domain.ChatMessage;
import com.yishou.liuyao.session.domain.ChatSession;
import com.yishou.liuyao.session.dto.MessageRequest;
import com.yishou.liuyao.session.dto.MessageResponse;
import com.yishou.liuyao.session.dto.SessionCreateRequest;
import com.yishou.liuyao.session.dto.SessionCreateResponse;
import com.yishou.liuyao.session.repository.ChatMessageRepository;
import com.yishou.liuyao.session.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private DivinationMapper divinationMapper;
    @Mock
    private ChartBuilderService chartBuilderService;
    @Mock
    private RuleEngineService ruleEngineService;
    @Mock
    private KnowledgeSearchService knowledgeSearchService;
    @Mock
    private OrchestratedAnalysisService analysisService;
    @Mock
    private AnalysisService legacyAnalysisService;
    @Mock
    private AnalysisContextFactory analysisContextFactory;
    @Mock
    private VerificationEventService verificationEventService;
    @Mock
    private CaseCenterService caseCenterService;
    @Mock
    private CaseChartSnapshotRepository caseChartSnapshotRepository;
    @Mock
    private ChatSessionRepository sessionRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private RateLimiter rateLimiter;

    private SessionService sessionService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        sessionService = new SessionService(
                divinationMapper,
                chartBuilderService,
                ruleEngineService,
                knowledgeSearchService,
                analysisService,
                legacyAnalysisService,
                analysisContextFactory,
                verificationEventService,
                caseCenterService,
                caseChartSnapshotRepository,
                sessionRepository,
                messageRepository,
                rateLimiter,
                objectMapper
        );
        ReflectionTestUtils.setField(sessionService, "maxMessagesPerSession", 50);
        ReflectionTestUtils.setField(sessionService, "maxInactiveHours", 24);
    }

    @Test
    void createSessionShouldExposeChartSnapshotForFrontendChat() {
        SessionCreateRequest request = new SessionCreateRequest();
        request.setUserId(7L);
        request.setQuestionText("我这个月面试结果如何");
        request.setQuestionCategory("事业");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime("2026-04-12T10:00:00");
        request.setRawLines(List.of("少阳", "少阴", "少阳", "少阴", "少阳", "少阴"));
        request.setMovingLines(List.of(2, 5));

        DivinationInput input = new DivinationInput();
        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisContextDTO analysisContext = sampleAnalysisContext();
        AnalysisOutputDTO analysisOutput = sampleAnalysisOutput("综合来看，事情有推进空间。");

        when(divinationMapper.toInput(any(DivinationAnalyzeRequest.class))).thenReturn(input);
        when(chartBuilderService.buildChart(input)).thenReturn(chartSnapshot);
        when(ruleEngineService.evaluateResult(any(ChartSnapshot.class))).thenReturn(evaluationResult);
        when(analysisContextFactory.create(request.getQuestionText(), chartSnapshot, evaluationResult.getHits()))
                .thenReturn(analysisContext);
        when(knowledgeSearchService.suggestKnowledgeSnippets(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(List.of("《增删卜易》：用神得地则事可成"));
        when(analysisService.analyzeInitial(eq(chartSnapshot), anyList(), eq(8), eq("POSITIVE"), anyList()))
                .thenReturn(analysisOutput);
        when(caseCenterService.recordAnalysis(any(), eq(chartSnapshot), anyList(), eq(analysisContext), any(), anyString()))
                .thenReturn(new CaseCenterService.RecordedAnalysisRefs(101L, 202L));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                ReflectionTestUtils.setField(session, "id", UUID.fromString("11111111-1111-1111-1111-111111111111"));
            }
            return session;
        });
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionCreateResponse response = sessionService.createSession(request);

        assertNotNull(response.getSessionId());
        assertNotNull(response.getChartSnapshot());
        assertEquals("地火明夷", response.getChartSnapshot().getMainHexagram());
        assertSame(analysisContext, response.getAnalysisContext());
        assertSame(analysisOutput, response.getAnalysis());
        assertEquals(1, response.getMessageCount());
        assertEquals(1, response.getRuleHits().size());

        ArgumentCaptor<ChatSession> sessionCaptor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepository, times(2)).save(sessionCaptor.capture());
        List<ChatSession> savedSessions = sessionCaptor.getAllValues();
        ChatSession savedSession = savedSessions.get(savedSessions.size() - 1);
        assertEquals(101L, savedSession.getCaseId());
        assertEquals(202L, savedSession.getChartSnapshotId());
        verify(rateLimiter).acquire(7L);
    }

    @Test
    void addMessageShouldReloadChartSnapshotFromStoredCaseSnapshot() throws Exception {
        UUID sessionId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        ChatSession session = ChatSession.create(7L, 101L, 202L, "我这个月面试结果如何", "事业");
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.incrementMessage(120);

        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        CaseChartSnapshot storedSnapshot = new CaseChartSnapshot();
        storedSnapshot.setCaseId(101L);
        storedSnapshot.setChartJson(objectMapper.writeValueAsString(chartSnapshot));

        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisOutputDTO followUpOutput = sampleAnalysisOutput("追问来看，结果仍偏正面。");

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(caseChartSnapshotRepository.findByCaseId(101L)).thenReturn(Optional.of(storedSnapshot));
        when(ruleEngineService.evaluateResult(any(ChartSnapshot.class))).thenReturn(evaluationResult);
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId))
                .thenReturn(List.of(ChatMessage.userMessage(sessionId, "我这个月面试结果如何")));
        when(knowledgeSearchService.suggestKnowledgeSnippets(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(List.of());
        when(analysisService.analyzeFollowUp(any(ChartSnapshot.class), anyList(), eq(8), eq("POSITIVE"), anyList(), anyList(), eq("还需要注意什么？")))
                .thenReturn(followUpOutput);
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                ReflectionTestUtils.setField(message, "id", UUID.fromString("33333333-3333-3333-3333-333333333333"));
            }
            if (message.getCreatedAt() == null) {
                ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 12, 11, 0));
            }
            return message;
        });
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MessageResponse response = sessionService.addMessage(sessionId, new MessageRequest("还需要注意什么？"));

        assertEquals(sessionId, response.getSessionId());
        assertNotNull(response.getMessageId());
        assertSame(followUpOutput, response.getAnalysis());
        assertEquals(2, response.getSessionMessageCount());

        ArgumentCaptor<ChartSnapshot> chartCaptor = ArgumentCaptor.forClass(ChartSnapshot.class);
        verify(analysisService).analyzeFollowUp(chartCaptor.capture(), anyList(), eq(8), eq("POSITIVE"), anyList(), anyList(), eq("还需要注意什么？"));
        assertEquals("地火明夷", chartCaptor.getValue().getMainHexagram());
        assertEquals("官鬼", chartCaptor.getValue().getUseGod());
        verify(rateLimiter).acquire(7L);
    }

    @Test
    void createSessionFollowUpThreeRoundsAndCloseShouldMaintainLifecycle() throws Exception {
        SessionCreateRequest request = buildCreateRequest();
        DivinationInput input = new DivinationInput();
        ChartSnapshot chartSnapshot = sampleChartSnapshot();
        RuleEvaluationResult evaluationResult = sampleEvaluationResult();
        AnalysisContextDTO analysisContext = sampleAnalysisContext();
        AnalysisOutputDTO initialOutput = sampleAnalysisOutput("第一轮结论");
        AnalysisOutputDTO followUp1 = sampleAnalysisOutput("第二轮结论");
        AnalysisOutputDTO followUp2 = sampleAnalysisOutput("第三轮结论");
        AnalysisOutputDTO followUp3 = sampleAnalysisOutput("第四轮结论");
        CaseChartSnapshot storedSnapshot = new CaseChartSnapshot();
        storedSnapshot.setCaseId(101L);
        storedSnapshot.setChartJson(objectMapper.writeValueAsString(chartSnapshot));

        AtomicReference<ChatSession> storedSession = new AtomicReference<>();
        AtomicInteger messageIdSequence = new AtomicInteger(1);
        List<ChatMessage> persistedMessages = new ArrayList<>();

        when(divinationMapper.toInput(any(DivinationAnalyzeRequest.class))).thenReturn(input);
        when(chartBuilderService.buildChart(input)).thenReturn(chartSnapshot);
        when(ruleEngineService.evaluateResult(any(ChartSnapshot.class))).thenReturn(evaluationResult);
        when(analysisContextFactory.create(request.getQuestionText(), chartSnapshot, evaluationResult.getHits()))
                .thenReturn(analysisContext);
        when(knowledgeSearchService.suggestKnowledgeSnippets(anyString(), anyString(), anyList(), anyInt()))
                .thenReturn(List.of("《增删卜易》：用神得地则事可成"));
        when(analysisService.analyzeInitial(eq(chartSnapshot), anyList(), eq(8), eq("POSITIVE"), anyList()))
                .thenReturn(initialOutput);
        when(analysisService.analyzeFollowUp(any(ChartSnapshot.class), anyList(), eq(8), eq("POSITIVE"), anyList(), anyList(), anyString()))
                .thenReturn(followUp1, followUp2, followUp3);
        when(caseCenterService.recordAnalysis(any(), eq(chartSnapshot), anyList(), eq(analysisContext), any(), anyString()))
                .thenReturn(new CaseCenterService.RecordedAnalysisRefs(101L, 202L));
        when(caseChartSnapshotRepository.findById(202L)).thenReturn(Optional.of(storedSnapshot));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> {
            ChatSession session = invocation.getArgument(0);
            if (session.getId() == null) {
                ReflectionTestUtils.setField(session, "id", UUID.fromString("44444444-4444-4444-4444-444444444444"));
            }
            storedSession.set(session);
            return session;
        });
        when(sessionRepository.findById(UUID.fromString("44444444-4444-4444-4444-444444444444")))
                .thenAnswer(invocation -> Optional.ofNullable(storedSession.get()));
        when(messageRepository.save(any(ChatMessage.class))).thenAnswer(invocation -> {
            ChatMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                String suffix = "%012d".formatted(messageIdSequence.getAndIncrement());
                ReflectionTestUtils.setField(message, "id", UUID.fromString("00000000-0000-0000-0000-" + suffix));
            }
            if (message.getCreatedAt() == null) {
                ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 4, 12, 12, 0));
            }
            persistedMessages.add(message);
            return message;
        });
        when(messageRepository.findBySessionIdOrderByCreatedAtAsc(UUID.fromString("44444444-4444-4444-4444-444444444444")))
                .thenAnswer(invocation -> List.copyOf(persistedMessages));

        SessionCreateResponse createResponse = sessionService.createSession(request);
        MessageResponse secondRound = sessionService.addMessage(createResponse.getSessionId(), new MessageRequest("第一轮追问"));
        MessageResponse thirdRound = sessionService.addMessage(createResponse.getSessionId(), new MessageRequest("第二轮追问"));
        MessageResponse fourthRound = sessionService.addMessage(createResponse.getSessionId(), new MessageRequest("第三轮追问"));
        sessionService.closeSession(createResponse.getSessionId());

        assertEquals(1, createResponse.getMessageCount());
        assertEquals(2, secondRound.getSessionMessageCount());
        assertEquals(3, thirdRound.getSessionMessageCount());
        assertEquals(4, fourthRound.getSessionMessageCount());
        assertEquals("CLOSED", storedSession.get().getStatus());
        verify(messageRepository, times(8)).save(any(ChatMessage.class));
        verify(rateLimiter, times(4)).acquire(7L);
    }

    @Test
    void addMessageShouldAutoCloseExpiredSession() {
        UUID sessionId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        ChatSession session = ChatSession.create(7L, 101L, 202L, "原问题", "事业");
        ReflectionTestUtils.setField(session, "id", sessionId);
        ReflectionTestUtils.setField(session, "lastActiveAt", LocalDateTime.now().minusHours(25));

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(ChatSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sessionService.addMessage(sessionId, new MessageRequest("超时后还想追问")));

        assertEquals(ErrorCode.SESSION_ALREADY_CLOSED, exception.getErrorCode());
        assertEquals("CLOSED", session.getStatus());
        verify(sessionRepository).save(session);
        verifyNoInteractions(analysisService);
    }

    @Test
    void addMessageShouldRejectWhenMessageLimitExceeded() {
        UUID sessionId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        ChatSession session = ChatSession.create(7L, 101L, 202L, "原问题", "事业");
        ReflectionTestUtils.setField(session, "id", sessionId);
        session.setMessageCount(50);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sessionService.addMessage(sessionId, new MessageRequest("还能继续吗")));

        assertEquals(ErrorCode.SESSION_MESSAGE_LIMIT_EXCEEDED, exception.getErrorCode());
        verifyNoInteractions(messageRepository, analysisService);
    }

    @Test
    void addMessageShouldRejectWhenRateLimitExceeded() {
        UUID sessionId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        ChatSession session = ChatSession.create(7L, 101L, 202L, "原问题", "事业");
        ReflectionTestUtils.setField(session, "id", sessionId);

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED, "今日请求次数已达上限"))
                .when(rateLimiter).acquire(7L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> sessionService.addMessage(sessionId, new MessageRequest("还能继续吗")));

        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        verifyNoInteractions(messageRepository, analysisService);
    }

    @Test
    void closeInactiveSessionsShouldDelegateToRepository() {
        when(sessionRepository.closeInactiveSessions(any(), any())).thenReturn(3);

        int closedCount = sessionService.closeInactiveSessions(java.time.Duration.ofHours(24));

        assertEquals(3, closedCount);
        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(sessionRepository).closeInactiveSessions(cutoffCaptor.capture(), nowCaptor.capture());
        assertEquals(java.time.Duration.ofHours(24), java.time.Duration.between(cutoffCaptor.getValue(), nowCaptor.getValue()));
    }

    private SessionCreateRequest buildCreateRequest() {
        SessionCreateRequest request = new SessionCreateRequest();
        request.setUserId(7L);
        request.setQuestionText("我这个月面试结果如何");
        request.setQuestionCategory("事业");
        request.setDivinationMethod("手工起卦");
        request.setDivinationTime("2026-04-12T10:00:00");
        request.setRawLines(List.of("少阳", "少阴", "少阳", "少阴", "少阳", "少阴"));
        request.setMovingLines(List.of(2, 5));
        return request;
    }

    private ChartSnapshot sampleChartSnapshot() {
        ChartSnapshot snapshot = new ChartSnapshot();
        snapshot.setQuestion("我这个月面试结果如何");
        snapshot.setQuestionCategory("事业");
        snapshot.setDivinationMethod("手工起卦");
        snapshot.setDivinationTime(LocalDateTime.of(2026, 4, 12, 10, 0));
        snapshot.setMainHexagram("地火明夷");
        snapshot.setChangedHexagram("地天泰");
        snapshot.setMainHexagramCode("000101");
        snapshot.setChangedHexagramCode("000111");
        snapshot.setPalace("坎");
        snapshot.setPalaceWuXing("水");
        snapshot.setUseGod("官鬼");
        snapshot.setRiChen("甲子");
        snapshot.setYueJian("辰");
        return snapshot;
    }

    private RuleEvaluationResult sampleEvaluationResult() {
        RuleHit hit = new RuleHit();
        hit.setRuleCode("career-positive");
        hit.setHit(Boolean.TRUE);
        hit.setRuleName("官鬼得势");
        hit.setCategory("CAREER");

        RuleEvaluationResult result = new RuleEvaluationResult();
        result.setHits(List.of(hit));
        result.setEffectiveScore(8);
        result.setEffectiveResultLevel("POSITIVE");
        return result;
    }

    private AnalysisContextDTO sampleAnalysisContext() {
        AnalysisContextDTO context = new AnalysisContextDTO();
        context.setContextVersion("v1");
        context.setQuestion("我这个月面试结果如何");
        context.setQuestionCategory("事业");
        context.setUseGod("官鬼");
        context.setRuleCodes(List.of("career-positive"));
        context.setRuleCount(1);
        return context;
    }

    private AnalysisOutputDTO sampleAnalysisOutput(String conclusion) {
        AnalysisOutputDTO.HexagramAnalysis analysis = new AnalysisOutputDTO.HexagramAnalysis();
        analysis.setConclusion(conclusion);
        analysis.setDetailedReasoning("推演显示主线向好。");
        analysis.setActionPlan(List.of("保持节奏", "准备复盘"));

        AnalysisOutputDTO dto = new AnalysisOutputDTO();
        dto.setAnalysis(analysis);
        dto.setSmartPrompts(List.of("何时有结果", "还有什么风险"));
        return dto;
    }
}
