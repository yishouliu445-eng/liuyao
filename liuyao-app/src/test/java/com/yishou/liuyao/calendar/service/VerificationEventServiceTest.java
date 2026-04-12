package com.yishou.liuyao.calendar.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yishou.liuyao.calendar.domain.VerificationEvent;
import com.yishou.liuyao.calendar.domain.VerificationFeedback;
import com.yishou.liuyao.calendar.dto.VerificationEventDTO;
import com.yishou.liuyao.calendar.dto.VerificationEventPageDTO;
import com.yishou.liuyao.calendar.dto.VerificationFeedbackSubmitRequest;
import com.yishou.liuyao.calendar.repository.VerificationEventRepository;
import com.yishou.liuyao.calendar.repository.VerificationFeedbackRepository;
import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationEventServiceTest {

    @Mock
    private VerificationEventRepository eventRepository;
    @Mock
    private VerificationFeedbackRepository feedbackRepository;

    private VerificationEventService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-04-12T10:00:00Z"), ZoneId.of("Asia/Shanghai"));
        service = new VerificationEventService(
                eventRepository,
                feedbackRepository,
                new ObjectMapper().findAndRegisterModules(),
                fixedClock
        );
    }

    @Test
    void shouldParsePredictedTimelineAndPersistVerificationEvent() {
        UUID sessionId = UUID.randomUUID();
        when(eventRepository.findBySessionIdAndPredictedDate(eq(sessionId), eq(LocalDate.of(2026, 5, 15))))
                .thenReturn(Optional.empty());
        when(eventRepository.save(any(VerificationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationEventDTO event = service.createEventFromPrediction(
                sessionId,
                1001L,
                "合作",
                "下个月中旬",
                "合作会在下个月中旬出现明确推进信号"
        );

        assertNotNull(event);
        assertEquals(sessionId, event.getSessionId());
        assertEquals(LocalDate.of(2026, 5, 15), event.getPredictedDate());
        assertEquals("MID_MONTH", event.getPredictedPrecision());
        assertEquals("PENDING", event.getStatus());
        assertEquals("合作", event.getQuestionCategory());
        verify(eventRepository).save(any(VerificationEvent.class));
    }

    @Test
    void shouldSupportTimelineRangeParsing() {
        VerificationEventService.PredictionParseResult result = service.parsePredictedTimeline("一至两个月内有进展");

        assertEquals(LocalDate.of(2026, 6, 12), result.predictedDate());
        assertEquals("RANGE", result.predictedPrecision());
        assertTrue(result.predictionSummary().contains("一至两个月内有进展"));
    }

    @Test
    void shouldSubmitFeedbackAndMarkEventVerified() {
        UUID eventId = UUID.randomUUID();
        VerificationEvent event = VerificationEvent.create(
                UUID.randomUUID(),
                1001L,
                LocalDate.of(2026, 5, 15),
                "MID_MONTH",
                "下个月中旬有结果",
                "合作"
        );
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(feedbackRepository.existsByEvent_Id(eventId)).thenReturn(false);
        when(feedbackRepository.save(any(VerificationFeedback.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(VerificationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VerificationEventDTO updated = service.submitFeedback(eventId, new VerificationFeedbackSubmitRequest(
                "ACCURATE",
                "合作在 5 月中旬顺利推进",
                List.of("合作", "签约")
        ));

        assertEquals("VERIFIED", updated.getStatus());
        assertTrue(updated.getFeedbackSubmitted());
        assertEquals("ACCURATE", updated.getFeedbackAccuracy());
        verify(feedbackRepository).save(any(VerificationFeedback.class));
        verify(eventRepository).save(event);
    }

    @Test
    void shouldRejectDuplicateFeedbackSubmission() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(VerificationEvent.create(
                UUID.randomUUID(),
                1001L,
                LocalDate.of(2026, 5, 15),
                "MID_MONTH",
                "下个月中旬有结果",
                "合作"
        )));
        when(feedbackRepository.existsByEvent_Id(eventId)).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.submitFeedback(eventId, new VerificationFeedbackSubmitRequest("ACCURATE", "结果应验", List.of("应验"))));

        assertEquals(ErrorCode.FEEDBACK_ALREADY_SUBMITTED, exception.getErrorCode());
    }

    @Test
    void shouldListMonthlyEventsAndTimeline() {
        VerificationEvent mayEvent = VerificationEvent.create(
                UUID.randomUUID(),
                1001L,
                LocalDate.of(2026, 5, 15),
                "MID_MONTH",
                "5 月中旬有推进",
                "合作"
        );
        VerificationEvent juneEvent = VerificationEvent.create(
                UUID.randomUUID(),
                1001L,
                LocalDate.of(2026, 6, 12),
                "RANGE",
                "1-2 个月内有进展",
                "合作"
        );
        when(eventRepository.findByUserIdAndPredictedDateBetweenOrderByPredictedDateAscCreatedAtAsc(
                eq(1001L), eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 31)), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(mayEvent), PageRequest.of(0, 10), 1));
        when(eventRepository.findByUserIdOrderByPredictedDateDescCreatedAtDesc(eq(1001L), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(juneEvent, mayEvent), PageRequest.of(0, 10), 2));

        VerificationEventPageDTO monthly = service.listMonthlyEvents(1001L, 2026, 5, 1, 10);
        VerificationEventPageDTO timeline = service.listTimeline(1001L, 1, 10);

        assertEquals(1, monthly.getItems().size());
        assertEquals(LocalDate.of(2026, 5, 15), monthly.getItems().get(0).getPredictedDate());
        assertEquals(2, timeline.getItems().size());
        assertFalse(timeline.getItems().isEmpty());
    }

    @Test
    void shouldMarkExpiredPendingEvents() {
        VerificationEvent expired = VerificationEvent.create(
                UUID.randomUUID(),
                1001L,
                LocalDate.of(2026, 3, 20),
                "MONTH",
                "3 月内会有结果",
                "合作"
        );
        when(eventRepository.findByStatusAndPredictedDateLessThanEqualOrderByPredictedDateAscCreatedAtAsc(
                "PENDING", LocalDate.of(2026, 3, 29)))
                .thenReturn(List.of(expired));
        when(eventRepository.save(any(VerificationEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = service.markExpiredEvents(LocalDate.of(2026, 4, 12));

        assertEquals(1, updated);
        assertEquals("EXPIRED", expired.getStatus());
        verify(eventRepository, times(1)).save(expired);
    }
}
