package com.yishou.liuyao.infrastructure.ratelimit;

import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

@Component
public class RateLimiter {

    private final Supplier<LocalDate> currentDateSupplier;
    private final Map<LocalDate, Map<String, AtomicInteger>> dailyCounters = new ConcurrentHashMap<>();

    @Value("${liuyao.rate-limit.anonymous-per-day:5}")
    private int anonymousPerDay = 5;

    @Value("${liuyao.rate-limit.authenticated-per-day:20}")
    private int authenticatedPerDay = 20;

    public RateLimiter() {
        this(LocalDate::now);
    }

    RateLimiter(Supplier<LocalDate> currentDateSupplier) {
        this.currentDateSupplier = currentDateSupplier;
    }

    public void acquire(Long userId) {
        LocalDate today = currentDateSupplier.get();
        pruneExpiredCounters(today);

        String principal = resolvePrincipal(userId);
        int limit = resolveLimit(userId);
        AtomicInteger counter = dailyCounters
                .computeIfAbsent(today, ignored -> new ConcurrentHashMap<>())
                .computeIfAbsent(principal, ignored -> new AtomicInteger(0));

        int nextCount = counter.incrementAndGet();
        if (nextCount <= limit) {
            return;
        }

        counter.decrementAndGet();
        throw new BusinessException(
                ErrorCode.RATE_LIMIT_EXCEEDED,
                "今日请求次数已达上限，匿名用户最多 " + anonymousPerDay
                        + " 次/天，登录用户最多 " + authenticatedPerDay + " 次/天"
        );
    }

    private void pruneExpiredCounters(LocalDate today) {
        dailyCounters.keySet().removeIf(date -> date.isBefore(today));
    }

    private String resolvePrincipal(Long userId) {
        return userId == null ? "anonymous" : "user:" + userId;
    }

    private int resolveLimit(Long userId) {
        return userId == null ? anonymousPerDay : authenticatedPerDay;
    }
}
