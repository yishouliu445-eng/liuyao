package com.yishou.liuyao.ops.ratelimit;

import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.ops.ratelimit.domain.RateLimitBucket;
import com.yishou.liuyao.ops.ratelimit.repository.RateLimitBucketRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class PersistentRateLimiterTest {

    @Autowired
    private RateLimitBucketRepository rateLimitBucketRepository;

    @BeforeEach
    void setUp() {
        rateLimitBucketRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        rateLimitBucketRepository.deleteAll();
    }

    @Test
    void shouldShareQuotaAcrossTwoLimiterInstances() {
        AtomicReference<LocalDate> currentDate = new AtomicReference<>(LocalDate.of(2026, 4, 14));
        PersistentRateLimiter firstLimiter = buildRateLimiter(currentDate);
        PersistentRateLimiter secondLimiter = buildRateLimiter(currentDate);

        firstLimiter.acquire(null);
        secondLimiter.acquire(null);
        firstLimiter.acquire(null);
        secondLimiter.acquire(null);
        firstLimiter.acquire(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> secondLimiter.acquire(null));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());

        RateLimitBucket bucket = rateLimitBucketRepository.findByBucketDateAndPrincipal(currentDate.get(), "anonymous")
                .orElseThrow();
        assertEquals(5, bucket.getRequestCount());
    }

    private PersistentRateLimiter buildRateLimiter(AtomicReference<LocalDate> currentDate) {
        PersistentRateLimiter rateLimiter = new PersistentRateLimiter(rateLimitBucketRepository, currentDate::get);
        ReflectionTestUtils.setField(rateLimiter, "anonymousPerDay", 5);
        ReflectionTestUtils.setField(rateLimiter, "authenticatedPerDay", 20);
        return rateLimiter;
    }
}
