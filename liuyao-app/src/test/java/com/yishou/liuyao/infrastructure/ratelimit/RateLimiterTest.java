package com.yishou.liuyao.infrastructure.ratelimit;

import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateLimiterTest {

    @Test
    void shouldRejectAnonymousUserAfterDailyLimit() {
        AtomicReference<LocalDate> currentDate = new AtomicReference<>(LocalDate.of(2026, 4, 13));
        RateLimiter rateLimiter = buildRateLimiter(currentDate);

        for (int index = 0; index < 5; index++) {
            assertDoesNotThrow(() -> rateLimiter.acquire(null));
        }

        BusinessException exception = assertThrows(BusinessException.class, () -> rateLimiter.acquire(null));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void shouldAllowAuthenticatedUserUpToHigherDailyLimit() {
        AtomicReference<LocalDate> currentDate = new AtomicReference<>(LocalDate.of(2026, 4, 13));
        RateLimiter rateLimiter = buildRateLimiter(currentDate);

        for (int index = 0; index < 20; index++) {
            assertDoesNotThrow(() -> rateLimiter.acquire(1001L));
        }

        BusinessException exception = assertThrows(BusinessException.class, () -> rateLimiter.acquire(1001L));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    @Test
    void shouldResetQuotaOnNextDay() {
        AtomicReference<LocalDate> currentDate = new AtomicReference<>(LocalDate.of(2026, 4, 13));
        RateLimiter rateLimiter = buildRateLimiter(currentDate);

        for (int index = 0; index < 5; index++) {
            rateLimiter.acquire(null);
        }

        currentDate.set(LocalDate.of(2026, 4, 14));
        assertDoesNotThrow(() -> rateLimiter.acquire(null));
    }

    private RateLimiter buildRateLimiter(AtomicReference<LocalDate> currentDate) {
        RateLimiter rateLimiter = new RateLimiter(currentDate::get);
        ReflectionTestUtils.setField(rateLimiter, "anonymousPerDay", 5);
        ReflectionTestUtils.setField(rateLimiter, "authenticatedPerDay", 20);
        return rateLimiter;
    }
}
