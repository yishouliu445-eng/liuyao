package com.yishou.liuyao.calendar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.analysis.dto.AnalysisOutputDTO;
import com.yishou.liuyao.calendar.domain.VerificationEvent;
import com.yishou.liuyao.calendar.domain.VerificationFeedback;
import com.yishou.liuyao.calendar.dto.VerificationEventDTO;
import com.yishou.liuyao.calendar.dto.VerificationEventPageDTO;
import com.yishou.liuyao.calendar.dto.VerificationFeedbackSubmitRequest;
import com.yishou.liuyao.calendar.repository.VerificationEventRepository;
import com.yishou.liuyao.calendar.repository.VerificationFeedbackRepository;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.infrastructure.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class VerificationEventService {

    private final VerificationEventRepository eventRepository;
    private final VerificationFeedbackRepository feedbackRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Value("${liuyao.verification.expire-after-days:14}")
    private int expireAfterDays = 14;

    @Autowired
    public VerificationEventService(VerificationEventRepository eventRepository,
                                    VerificationFeedbackRepository feedbackRepository,
                                    ObjectMapper objectMapper) {
        this(eventRepository, feedbackRepository, objectMapper, Clock.systemDefaultZone());
    }

    VerificationEventService(VerificationEventRepository eventRepository,
                             VerificationFeedbackRepository feedbackRepository,
                             ObjectMapper objectMapper,
                             Clock clock) {
        this.eventRepository = eventRepository;
        this.feedbackRepository = feedbackRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public VerificationEventDTO createEventFromAnalysis(UUID sessionId,
                                                        Long userId,
                                                        String questionCategory,
                                                        AnalysisOutputDTO analysisOutput) {
        if (analysisOutput == null || analysisOutput.getAnalysis() == null) {
            return null;
        }
        String predictedTimeline = analysisOutput.getAnalysis().getPredictedTimeline();
        String predictionSummary = analysisOutput.getAnalysis().getConclusion();
        return createEventFromPrediction(sessionId, userId, questionCategory, predictedTimeline, predictionSummary);
    }

    @Transactional
    public VerificationEventDTO createEventFromPrediction(UUID sessionId,
                                                          Long userId,
                                                          String questionCategory,
                                                          String predictedTimeline,
                                                          String predictionSummary) {
        if (sessionId == null || predictedTimeline == null || predictedTimeline.isBlank()) {
            return null;
        }

        PredictionParseResult parsed = parsePredictedTimeline(predictedTimeline);
        if (parsed == null) {
            return null;
        }

        Optional<VerificationEvent> existing = eventRepository.findBySessionIdAndPredictedDate(sessionId, parsed.predictedDate());
        VerificationEvent event = existing.orElseGet(() -> VerificationEvent.create(
                sessionId,
                userId,
                parsed.predictedDate(),
                parsed.predictedPrecision(),
                resolveSummary(predictionSummary, parsed.predictionSummary()),
                questionCategory
        ));

        if (existing.isPresent()) {
            event.setPredictedPrecision(parsed.predictedPrecision());
            event.setPredictionSummary(resolveSummary(predictionSummary, parsed.predictionSummary()));
            event.setQuestionCategory(questionCategory);
            if (!"VERIFIED".equals(event.getStatus())) {
                event.setStatus("PENDING");
            }
        }

        return toDto(eventRepository.save(event));
    }

    @Transactional(readOnly = true)
    public VerificationEventPageDTO listMonthlyEvents(Long userId, int year, int month, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = YearMonth.of(year, month).atEndOfMonth();
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<VerificationEvent> events = userId == null
                ? eventRepository.findByPredictedDateBetweenOrderByPredictedDateAscCreatedAtAsc(startDate, endDate, pageable)
                : eventRepository.findByUserIdAndPredictedDateBetweenOrderByPredictedDateAscCreatedAtAsc(userId, startDate, endDate, pageable);
        return toPageDto(events, safePage, safeSize);
    }

    @Transactional(readOnly = true)
    public VerificationEventPageDTO listTimeline(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageable = PageRequest.of(safePage - 1, safeSize);
        Page<VerificationEvent> events = userId == null
                ? eventRepository.findAllByOrderByPredictedDateDescCreatedAtDesc(pageable)
                : eventRepository.findByUserIdOrderByPredictedDateDescCreatedAtDesc(userId, pageable);
        return toPageDto(events, safePage, safeSize);
    }

    @Transactional
    public VerificationEventDTO submitFeedback(UUID eventId, VerificationFeedbackSubmitRequest request) {
        VerificationEvent event = eventRepository.findById(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VERIFICATION_EVENT_NOT_FOUND, "应验事件不存在"));
        if (feedbackRepository.existsByEvent_Id(eventId)) {
            throw new BusinessException(ErrorCode.FEEDBACK_ALREADY_SUBMITTED, "该应验事件已提交反馈");
        }

        String tagsJson = JsonUtils.toJson(objectMapper, request.getTags() == null ? List.of() : request.getTags());
        VerificationFeedback feedback = VerificationFeedback.create(
                event,
                request.getAccuracy(),
                request.getActualOutcome(),
                tagsJson
        );
        feedbackRepository.save(feedback);
        event.attachFeedback(feedback);
        event.markStatus("VERIFIED");
        return toDto(eventRepository.save(event));
    }

    @Transactional
    public int sendDueReminders() {
        return sendDueReminders(LocalDate.now(clock));
    }

    @Transactional
    public int sendDueReminders(LocalDate today) {
        List<VerificationEvent> dueEvents = eventRepository
                .findByStatusAndPredictedDateLessThanEqualOrderByPredictedDateAscCreatedAtAsc("PENDING", today)
                .stream()
                .filter(event -> event.getReminderSentAt() == null)
                .toList();
        dueEvents.forEach(event -> {
            event.markReminderSent();
            eventRepository.save(event);
        });
        return dueEvents.size();
    }

    @Transactional
    public int markExpiredEvents() {
        return markExpiredEvents(LocalDate.now(clock));
    }

    @Transactional
    public int markExpiredEvents(LocalDate today) {
        LocalDate expireBefore = today.minusDays(expireAfterDays);
        List<VerificationEvent> expiredEvents = eventRepository
                .findByStatusAndPredictedDateLessThanEqualOrderByPredictedDateAscCreatedAtAsc("PENDING", expireBefore);
        expiredEvents.forEach(event -> {
            event.markStatus("EXPIRED");
            eventRepository.save(event);
        });
        return expiredEvents.size();
    }

    PredictionParseResult parsePredictedTimeline(String predictedTimeline) {
        if (predictedTimeline == null || predictedTimeline.isBlank()) {
            return null;
        }
        String normalized = predictedTimeline.strip().replace(" ", "");
        LocalDate today = LocalDate.now(clock);

        java.util.regex.Matcher explicitDate = java.util.regex.Pattern
                .compile("(\\d{4})[-年/.](\\d{1,2})[-月/.](\\d{1,2})日?")
                .matcher(normalized);
        if (explicitDate.find()) {
            return new PredictionParseResult(
                    LocalDate.of(
                            Integer.parseInt(explicitDate.group(1)),
                            Integer.parseInt(explicitDate.group(2)),
                            Integer.parseInt(explicitDate.group(3))
                    ),
                    "DAY",
                    predictedTimeline
            );
        }

        if (normalized.contains("明天")) {
            return new PredictionParseResult(today.plusDays(1), "DAY", predictedTimeline);
        }
        if (normalized.contains("后天")) {
            return new PredictionParseResult(today.plusDays(2), "DAY", predictedTimeline);
        }
        if (normalized.contains("今天") || normalized.contains("今日")) {
            return new PredictionParseResult(today, "DAY", predictedTimeline);
        }

        if (normalized.contains("下周")) {
            LocalDate nextWeek = today.plusWeeks(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new PredictionParseResult(nextWeek, "WEEK", predictedTimeline);
        }
        if (normalized.contains("本周") || normalized.contains("这周")) {
            LocalDate currentWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new PredictionParseResult(currentWeek, "WEEK", predictedTimeline);
        }

        java.util.regex.Matcher rangeMonths = java.util.regex.Pattern
                .compile("([一二两三四五六七八九十\\d]+)(?:至|到|\\-)([一二两三四五六七八九十\\d]+)个?月内")
                .matcher(normalized);
        if (rangeMonths.find()) {
            int maxMonths = parseChineseNumber(rangeMonths.group(2));
            return new PredictionParseResult(today.plusMonths(maxMonths), "RANGE", predictedTimeline);
        }

        java.util.regex.Matcher singleMonthRange = java.util.regex.Pattern
                .compile("([一二两三四五六七八九十\\d]+)个?月内")
                .matcher(normalized);
        if (singleMonthRange.find()) {
            int months = parseChineseNumber(singleMonthRange.group(1));
            return new PredictionParseResult(today.plusMonths(months), "RANGE", predictedTimeline);
        }

        int monthOffset = (normalized.contains("下个月") || normalized.contains("下月")) ? 1 : 0;
        YearMonth targetMonth = YearMonth.from(today.plusMonths(monthOffset));
        if (normalized.contains("上旬")) {
            return new PredictionParseResult(targetMonth.atDay(5), "EARLY_MONTH", predictedTimeline);
        }
        if (normalized.contains("中旬")) {
            return new PredictionParseResult(targetMonth.atDay(15), "MID_MONTH", predictedTimeline);
        }
        if (normalized.contains("下旬")) {
            return new PredictionParseResult(targetMonth.atDay(25), "LATE_MONTH", predictedTimeline);
        }
        if (normalized.contains("月底") || normalized.contains("月末")) {
            return new PredictionParseResult(targetMonth.atEndOfMonth(), "LATE_MONTH", predictedTimeline);
        }
        if (normalized.contains("本月") || normalized.contains("这个月")
                || normalized.contains("下个月") || normalized.contains("下月")) {
            return new PredictionParseResult(targetMonth.atDay(15), "MONTH", predictedTimeline);
        }

        return null;
    }

    private VerificationEventPageDTO toPageDto(Page<VerificationEvent> pageData, int page, int size) {
        VerificationEventPageDTO dto = new VerificationEventPageDTO();
        dto.setPage(page);
        dto.setSize(size);
        dto.setTotal(pageData.getTotalElements());
        dto.setItems(pageData.getContent().stream().map(this::toDto).toList());
        return dto;
    }

    private VerificationEventDTO toDto(VerificationEvent event) {
        VerificationEventDTO dto = new VerificationEventDTO();
        dto.setEventId(event.getId());
        dto.setSessionId(event.getSessionId());
        dto.setUserId(event.getUserId());
        dto.setPredictedDate(event.getPredictedDate());
        dto.setPredictedPrecision(event.getPredictedPrecision());
        dto.setPredictionSummary(event.getPredictionSummary());
        dto.setQuestionCategory(event.getQuestionCategory());
        dto.setStatus(event.getStatus());
        dto.setReminderSentAt(event.getReminderSentAt());
        dto.setCreatedAt(event.getCreatedAt());
        dto.setFeedbackSubmitted(event.getFeedback() != null);
        dto.setFeedbackAccuracy(event.getFeedback() == null ? null : event.getFeedback().getAccuracy());
        return dto;
    }

    private String resolveSummary(String predictionSummary, String fallbackSummary) {
        if (predictionSummary != null && !predictionSummary.isBlank()) {
            return predictionSummary;
        }
        return fallbackSummary;
    }

    private int parseChineseNumber(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.chars().allMatch(Character::isDigit)) {
            return Integer.parseInt(normalized);
        }
        return switch (normalized) {
            case "一" -> 1;
            case "二", "两" -> 2;
            case "三" -> 3;
            case "四" -> 4;
            case "五" -> 5;
            case "六" -> 6;
            case "七" -> 7;
            case "八" -> 8;
            case "九" -> 9;
            case "十" -> 10;
            default -> 1;
        };
    }

    record PredictionParseResult(LocalDate predictedDate, String predictedPrecision, String predictionSummary) {
    }
}
