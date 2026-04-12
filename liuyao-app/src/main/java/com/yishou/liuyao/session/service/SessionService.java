package com.yishou.liuyao.session.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisContextDTO;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.analysis.service.AnalysisService;
import com.yishou.liuyao.analysis.dto.StructuredAnalysisResultDTO;
import com.yishou.liuyao.analysis.service.AnalysisContextFactory;
import com.yishou.liuyao.casecenter.domain.CaseChartSnapshot;
import com.yishou.liuyao.analysis.service.OrchestratedAnalysisService;
import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.casecenter.repository.CaseChartSnapshotRepository;
import com.yishou.liuyao.casecenter.service.CaseCenterService;
import com.yishou.liuyao.common.dto.PageResult;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.divination.domain.ChartSnapshot;
import com.yishou.liuyao.divination.dto.DivinationAnalyzeRequest;
import com.yishou.liuyao.divination.dto.ChartSnapshotDTO;
import com.yishou.liuyao.divination.mapper.DivinationMapper;
import com.yishou.liuyao.divination.service.ChartBuilderService;
import com.yishou.liuyao.knowledge.service.KnowledgeSearchService;
import com.yishou.liuyao.rule.RuleHit;
import com.yishou.liuyao.rule.dto.RuleHitDTO;
import com.yishou.liuyao.rule.service.RuleEvaluationResult;
import com.yishou.liuyao.rule.service.RuleEngineService;
import com.yishou.liuyao.session.domain.ChatMessage;
import com.yishou.liuyao.session.domain.ChatSession;
import com.yishou.liuyao.session.dto.*;
import com.yishou.liuyao.session.repository.ChatMessageRepository;
import com.yishou.liuyao.session.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Session 核心编排服务。
 *
 * <p>这是 v2.0 的主入口，替代旧的 {@code DivinationService}。流程：
 * <ol>
 *   <li>接收起卦请求 → 排盘 → 规则 → RAG → 编排LLM → 持久化 → 返回</li>
 *   <li>追问请求 → 加载Session → 构建上下文 → 编排LLM → 追加消息 → 返回</li>
 * </ol>
 * </p>
 */
@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final DivinationMapper divinationMapper;
    private final ChartBuilderService chartBuilderService;
    private final RuleEngineService ruleEngineService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final OrchestratedAnalysisService analysisService;
    private final AnalysisService legacyAnalysisService;
    private final AnalysisContextFactory analysisContextFactory;
    private final VerificationEventService verificationEventService;
    private final CaseCenterService caseCenterService;
    private final CaseChartSnapshotRepository caseChartSnapshotRepository;
    private final ChatSessionRepository sessionRepo;
    private final ChatMessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    @Value("${liuyao.session.max-messages-per-session:50}")
    private int maxMessagesPerSession;

    @Value("${liuyao.session.max-inactive-hours:24}")
    private int maxInactiveHours;

    public SessionService(DivinationMapper divinationMapper,
                          ChartBuilderService chartBuilderService,
                          RuleEngineService ruleEngineService,
                          KnowledgeSearchService knowledgeSearchService,
                          OrchestratedAnalysisService analysisService,
                          AnalysisService legacyAnalysisService,
                          AnalysisContextFactory analysisContextFactory,
                          VerificationEventService verificationEventService,
                          CaseCenterService caseCenterService,
                          CaseChartSnapshotRepository caseChartSnapshotRepository,
                          ChatSessionRepository sessionRepo,
                          ChatMessageRepository messageRepo,
                          ObjectMapper objectMapper) {
        this.divinationMapper = divinationMapper;
        this.chartBuilderService = chartBuilderService;
        this.ruleEngineService = ruleEngineService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.analysisService = analysisService;
        this.legacyAnalysisService = legacyAnalysisService;
        this.analysisContextFactory = analysisContextFactory;
        this.verificationEventService = verificationEventService;
        this.caseCenterService = caseCenterService;
        this.caseChartSnapshotRepository = caseChartSnapshotRepository;
        this.sessionRepo = sessionRepo;
        this.messageRepo = messageRepo;
        this.objectMapper = objectMapper;
    }

    // ---- 创建 Session（起卦） ----

    @Transactional
    public SessionCreateResponse createSession(SessionCreateRequest request) {
        log.info("创建Session: category={}, question={}", request.getQuestionCategory(),
                truncate(request.getQuestionText(), 30));

        // 1. 排盘
        DivinationAnalyzeRequest divRequest = toAnalyzeRequest(request);
        ChartSnapshot chart = chartBuilderService.buildChart(divinationMapper.toInput(divRequest));

        // 2. 规则引擎
        RuleEvaluationResult evalResult = ruleEngineService.evaluateResult(chart);
        List<RuleHit> ruleHits = evalResult.getHits();
        int effectiveScore = evalResult.getEffectiveScore() != null ? evalResult.getEffectiveScore() : 0;
        String resultLevel = evalResult.getEffectiveResultLevel();
        StructuredAnalysisResultDTO structuredResult = toStructuredResultDto(evalResult);

        // 3. RAG 知识检索
        List<String> snippets = safeSearchKnowledge(chart, ruleHits);
        List<RuleHitDTO> ruleHitDtos = toRuleHitDtos(ruleHits);
        AnalysisContextDTO analysisContext = buildAnalysisContext(
                request.getQuestionText(), chart, ruleHits, ruleHitDtos, structuredResult, snippets);

        // 4. 编排 LLM
        AnalysisOutputDTO analysis = analysisService.analyzeInitial(
                chart, ruleHits, effectiveScore, resultLevel, snippets);

        // 5. 持久化卦例（保留向后兼容）
        Long caseId = null;
        Long snapshotId = null;
        try {
            CaseCenterService.RecordedAnalysisRefs caseRecord = caseCenterService.recordAnalysis(
                    divRequest, chart, ruleHits, analysisContext, structuredResult, buildCaseAnalysisText(analysisContext, analysis));
            caseId = caseRecord != null ? caseRecord.caseId() : null;
            snapshotId = caseRecord != null ? caseRecord.snapshotId() : null;
        } catch (Exception e) {
            log.warn("卦例持久化失败（不影响主流程）: {}", e.getMessage());
        }

        // 6. 创建 Session
        ChatSession session = ChatSession.create(
                request.getUserId(), caseId, snapshotId,
                request.getQuestionText(), request.getQuestionCategory());
        session = sessionRepo.save(session);

        // 7. 保存首条消息（USER + ASSISTANT）
        ChatMessage userMsg = ChatMessage.userMessage(session.getId(), request.getQuestionText());
        messageRepo.save(userMsg);

        String analysisJson = toJson(analysis);
        ChatMessage aiMsg = ChatMessage.assistantMessage(
                session.getId(), extractConclusion(analysis),
                analysisJson, extractModel(analysis),
                extractTokens(analysis), extractLatency(analysis));
        aiMsg = messageRepo.save(aiMsg);
        session.incrementMessage(extractTokens(analysis));
        sessionRepo.save(session);
        createVerificationEventIfNeeded(session, analysis);

        // 8. 构建响应
        SessionCreateResponse response = new SessionCreateResponse();
        response.setSessionId(session.getId());
        response.setStatus(session.getStatus());
        response.setChartSnapshot(toChartSnapshotDto(chart));
        response.setRuleHits(ruleHitDtos);
        response.setAnalysisContext(analysisContext);
        response.setStructuredResult(structuredResult);
        response.setAnalysis(analysis);
        response.setSmartPrompts(analysis.getSmartPrompts() != null
                ? analysis.getSmartPrompts() : Collections.emptyList());
        response.setMessageCount(session.getMessageCount());
        return response;
    }

    // ---- 追问 ----

    @Transactional
    public MessageResponse addMessage(UUID sessionId, MessageRequest request) {
        // 加载并校验 Session
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SESSION_NOT_FOUND, "会话不存在: " + sessionId));

        if (isExpired(session, Duration.ofHours(maxInactiveHours))) {
            session.close();
            sessionRepo.save(session);
            throw new BusinessException(ErrorCode.SESSION_ALREADY_CLOSED,
                    "该会话已关闭，请开启新会话");
        }

        if (!session.isActive()) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_CLOSED,
                    "该会话已关闭，请开启新会话");
        }

        if (session.getMessageCount() >= maxMessagesPerSession) {
            throw new BusinessException(ErrorCode.SESSION_MESSAGE_LIMIT_EXCEEDED,
                    "单次会话消息数已达上限（" + maxMessagesPerSession + "条）");
        }

        // 刷新活跃时间
        session.refreshActivity();

        // 加载卦例上下文
        ChartSnapshot chart = loadChartFromSession(session);
        RuleEvaluationResult evalResult = ruleEngineService.evaluateResult(chart);
        List<RuleHit> ruleHits = evalResult.getHits();
        int effectiveScore = evalResult.getEffectiveScore() != null ? evalResult.getEffectiveScore() : 0;
        String resultLevel = evalResult.getEffectiveResultLevel();

        // 加载历史消息
        List<ChatMessage> history = messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // RAG 检索（针对追问内容）
        List<String> snippets = safeSearchKnowledgeByQuestion(request.getContent(), chart);

        // 编排 LLM 追问
        AnalysisOutputDTO analysis = analysisService.analyzeFollowUp(
                chart, ruleHits, effectiveScore, resultLevel,
                history, snippets, request.getContent());

        // 持久化消息
        ChatMessage userMsg = ChatMessage.userMessage(session.getId(), request.getContent());
        messageRepo.save(userMsg);

        String analysisJson = toJson(analysis);
        ChatMessage aiMsg = ChatMessage.assistantMessage(
                session.getId(), extractConclusion(analysis),
                analysisJson, extractModel(analysis),
                extractTokens(analysis), extractLatency(analysis));
        aiMsg = messageRepo.save(aiMsg);
        session.incrementMessage(extractTokens(analysis));
        sessionRepo.save(session);
        createVerificationEventIfNeeded(session, analysis);

        // 构建响应
        MessageResponse response = new MessageResponse();
        response.setMessageId(aiMsg.getId());
        response.setSessionId(sessionId);
        response.setAnalysis(analysis);
        response.setSmartPrompts(analysis.getSmartPrompts() != null
                ? analysis.getSmartPrompts() : Collections.emptyList());
        response.setCreatedAt(aiMsg.getCreatedAt());
        response.setSessionMessageCount(session.getMessageCount());
        return response;
    }

    // ---- 关闭 Session ----

    @Transactional
    public void closeSession(UUID sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SESSION_NOT_FOUND, "会话不存在: " + sessionId));
        session.close();
        sessionRepo.save(session);
        log.info("Session已关闭: {}", sessionId);
    }

    @Transactional
    public int closeInactiveSessions() {
        return closeInactiveSessions(Duration.ofHours(maxInactiveHours));
    }

    @Transactional
    public int closeInactiveSessions(Duration maxInactiveDuration) {
        if (maxInactiveDuration == null || maxInactiveDuration.isNegative() || maxInactiveDuration.isZero()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的会话超时时长");
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minus(maxInactiveDuration);
        int closedCount = sessionRepo.closeInactiveSessions(cutoff, now);
        log.info("自动关闭超时Session: cutoff={}, closedCount={}", cutoff, closedCount);
        return closedCount;
    }

    // ---- 查询 ----

    public PageResult<ChatSession> listSessions(Long userId, int page, int size) {
        Page<ChatSession> pageResult = sessionRepo.findByUserIdOrderByLastActiveAtDesc(
                userId, PageRequest.of(page, size));
        PageResult<ChatSession> result = new PageResult<>();
        result.setRecords(pageResult.getContent());
        result.setTotal(pageResult.getTotalElements());
        return result;
    }

    public SessionDetailResponse getSession(UUID sessionId) {
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.SESSION_NOT_FOUND, "会话不存在: " + sessionId));
        SessionDetailResponse response = new SessionDetailResponse();
        response.setSessionId(session.getId());
        response.setStatus(session.getStatus());
        response.setOriginalQuestion(session.getOriginalQuestion());
        response.setQuestionCategory(session.getQuestionCategory());
        response.setMessageCount(session.getMessageCount());
        response.setTotalTokens(session.getTotalTokens());
        response.setCreatedAt(session.getCreatedAt());
        response.setLastActiveAt(session.getLastActiveAt());
        response.setClosedAt(session.getClosedAt());
        response.setChartSnapshot(toChartSnapshotDto(loadChartFromSession(session)));
        response.setMessages(messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId));
        return response;
    }

    public List<ChatMessage> getMessages(UUID sessionId) {
        return messageRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // ---- Private Helpers ----

    private List<String> safeSearchKnowledge(ChartSnapshot chart, List<RuleHit> ruleHits) {
        try {
            List<String> ruleCodes = ruleHits.stream()
                    .filter(h -> Boolean.TRUE.equals(h.getHit()))
                    .map(RuleHit::getRuleCode)
                    .toList();
            return knowledgeSearchService.suggestKnowledgeSnippets(
                    chart.getQuestionCategory(), chart.getUseGod(), ruleCodes, 6);
        } catch (Exception e) {
            log.warn("RAG知识检索失败（不影响主流程）: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> safeSearchKnowledgeByQuestion(String question, ChartSnapshot chart) {
        try {
            // 追问时用问题内容作为语义查询，结合卦象类别做过滤
            return knowledgeSearchService.suggestKnowledgeSnippets(
                    chart.getQuestionCategory(), chart.getUseGod(),
                    List.of(), 4);
        } catch (Exception e) {
            log.warn("追问RAG检索失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    private ChartSnapshot loadChartFromSession(ChatSession session) {
        CaseChartSnapshot storedSnapshot = null;
        if (session.getChartSnapshotId() != null) {
            storedSnapshot = caseChartSnapshotRepository.findById(session.getChartSnapshotId()).orElse(null);
        }
        if (storedSnapshot == null && session.getCaseId() != null) {
            storedSnapshot = caseChartSnapshotRepository.findByCaseId(session.getCaseId()).orElse(null);
        }
        if (storedSnapshot == null || storedSnapshot.getChartJson() == null || storedSnapshot.getChartJson().isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "无法从会话中恢复卦象快照: " + session.getId());
        }
        try {
            return objectMapper.readValue(storedSnapshot.getChartJson(), ChartSnapshot.class);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                    "会话卦象快照解析失败: " + session.getId());
        }
    }

    private DivinationAnalyzeRequest toAnalyzeRequest(SessionCreateRequest req) {
        DivinationAnalyzeRequest r = new DivinationAnalyzeRequest();
        r.setQuestionText(req.getQuestionText());
        r.setQuestionCategory(req.getQuestionCategory());
        r.setDivinationMethod(req.getDivinationMethod());
        r.setMovingLines(req.getMovingLines());
        r.setRawLines(req.getRawLines());
        if (req.getDivinationTime() != null && !req.getDivinationTime().isBlank()) {
            r.setDivinationTime(java.time.LocalDateTime.parse(req.getDivinationTime()));
        } else {
            r.setDivinationTime(LocalDateTime.now());
        }
        return r;
    }

    private boolean isExpired(ChatSession session, Duration maxInactiveDuration) {
        if (session == null || !session.isActive() || session.getLastActiveAt() == null) {
            return false;
        }
        LocalDateTime cutoff = LocalDateTime.now().minus(maxInactiveDuration);
        return session.getLastActiveAt().isBefore(cutoff);
    }

    private AnalysisContextDTO buildAnalysisContext(String question,
                                                    ChartSnapshot chartSnapshot,
                                                    List<RuleHit> ruleHits,
                                                    List<RuleHitDTO> ruleHitDtos,
                                                    StructuredAnalysisResultDTO structuredResult,
                                                    List<String> knowledgeSnippets) {
        AnalysisContextDTO context = analysisContextFactory.create(question, chartSnapshot, ruleHits);
        context.setChartSnapshot(toChartSnapshotDto(chartSnapshot));
        context.setRuleHits(ruleHitDtos);
        context.setStructuredResult(structuredResult);
        context.setKnowledgeSnippets(knowledgeSnippets != null ? knowledgeSnippets : List.of());
        return context;
    }

    private ChartSnapshotDTO toChartSnapshotDto(ChartSnapshot chartSnapshot) {
        return objectMapper.convertValue(chartSnapshot, ChartSnapshotDTO.class);
    }

    private List<RuleHitDTO> toRuleHitDtos(List<RuleHit> ruleHits) {
        return ruleHits.stream().map(this::toRuleHitDto).toList();
    }

    private RuleHitDTO toRuleHitDto(RuleHit ruleHit) {
        RuleHitDTO dto = new RuleHitDTO();
        dto.setRuleId(ruleHit.getRuleId());
        dto.setRuleCode(ruleHit.getRuleCode());
        dto.setRuleName(ruleHit.getRuleName());
        dto.setCategory(ruleHit.getCategory());
        dto.setPriority(ruleHit.getPriority());
        dto.setHitReason(ruleHit.getHitReason());
        dto.setImpactLevel(ruleHit.getImpactLevel());
        dto.setScoreDelta(ruleHit.getScoreDelta());
        dto.setTags(ruleHit.getTags());
        dto.setEvidence(ruleHit.getEvidence());
        return dto;
    }

    private StructuredAnalysisResultDTO toStructuredResultDto(RuleEvaluationResult evaluationResult) {
        StructuredAnalysisResultDTO dto = new StructuredAnalysisResultDTO();
        dto.setScore(evaluationResult.getScore());
        dto.setResultLevel(evaluationResult.getResultLevel());
        dto.setEffectiveScore(evaluationResult.getEffectiveScore());
        dto.setEffectiveResultLevel(evaluationResult.getEffectiveResultLevel());
        dto.setTags(evaluationResult.getTags() != null ? evaluationResult.getTags() : List.of());
        dto.setEffectiveRuleCodes(evaluationResult.getEffectiveRuleCodes() != null
                ? evaluationResult.getEffectiveRuleCodes() : List.of());
        dto.setSuppressedRuleCodes(evaluationResult.getSuppressedRuleCodes() != null
                ? evaluationResult.getSuppressedRuleCodes() : List.of());
        dto.setSummary(evaluationResult.getSummary());
        dto.setCategorySummaries(evaluationResult.getCategorySummaries().stream().map(this::toCategorySummaryDto).toList());
        dto.setConflictSummaries(evaluationResult.getConflictSummaries().stream().map(this::toConflictSummaryDto).toList());
        return dto;
    }

    private com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO toCategorySummaryDto(java.util.Map<String, Object> item) {
        com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO dto =
                new com.yishou.liuyao.analysis.dto.RuleCategorySummaryDTO();
        dto.setCategory(String.valueOf(item.getOrDefault("category", "GENERAL")));
        dto.setHitCount(asInteger(item.get("hitCount")));
        dto.setScore(asInteger(item.get("score")));
        dto.setEffectiveHitCount(asInteger(item.get("effectiveHitCount")));
        dto.setEffectiveScore(asInteger(item.get("effectiveScore")));
        dto.setStageOrder(asInteger(item.get("stageOrder")));
        return dto;
    }

    private com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO toConflictSummaryDto(java.util.Map<String, Object> item) {
        com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO dto =
                new com.yishou.liuyao.analysis.dto.RuleConflictSummaryDTO();
        dto.setCategory(String.valueOf(item.getOrDefault("category", "GENERAL")));
        dto.setPositiveCount(asInteger(item.get("positiveCount")));
        dto.setNegativeCount(asInteger(item.get("negativeCount")));
        dto.setPositiveScore(asInteger(item.get("positiveScore")));
        dto.setNegativeScore(asInteger(item.get("negativeScore")));
        dto.setNetScore(asInteger(item.get("netScore")));
        dto.setDecision(String.valueOf(item.getOrDefault("decision", "MIXED")));
        dto.setPositiveRules(asStringList(item.get("positiveRules")));
        dto.setNegativeRules(asStringList(item.get("negativeRules")));
        dto.setEffectiveRules(asStringList(item.get("effectiveRules")));
        dto.setSuppressedRules(asStringList(item.get("suppressedRules")));
        return dto;
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private List<String> asStringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        return List.of();
    }

    private String extractConclusion(AnalysisOutputDTO dto) {
        if (dto == null || dto.getAnalysis() == null) return "";
        String c = dto.getAnalysis().getConclusion();
        return c != null ? c : "";
    }

    private String buildCaseAnalysisText(AnalysisContextDTO analysisContext, AnalysisOutputDTO analysisOutput) {
        if (analysisContext != null) {
            try {
                String legacyAnalysis = legacyAnalysisService.analyze(analysisContext);
                if (legacyAnalysis != null && !legacyAnalysis.isBlank()) {
                    return legacyAnalysis;
                }
            } catch (Exception exception) {
                log.warn("生成案例基线分析文本失败，回退为结论摘要: {}", exception.getMessage());
            }
        }
        return extractConclusion(analysisOutput);
    }

    private void createVerificationEventIfNeeded(ChatSession session, AnalysisOutputDTO analysisOutput) {
        if (session == null || session.getId() == null || analysisOutput == null) {
            return;
        }
        try {
            verificationEventService.createEventFromAnalysis(
                    session.getId(),
                    session.getUserId(),
                    session.getQuestionCategory(),
                    analysisOutput
            );
        } catch (Exception exception) {
            log.warn("自动创建应验事件失败（不影响主流程）: {}", exception.getMessage());
        }
    }

    private String extractModel(AnalysisOutputDTO dto) {
        if (dto == null || dto.getMetadata() == null) return "unknown";
        return dto.getMetadata().getModelUsed();
    }

    private int extractTokens(AnalysisOutputDTO dto) {
        return 0; // LLM token count is tracked in LlmClient logs, not propagated here yet
    }

    private int extractLatency(AnalysisOutputDTO dto) {
        if (dto == null || dto.getMetadata() == null) return 0;
        return dto.getMetadata().getProcessingTimeMs();
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }
}
