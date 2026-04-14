package com.yishou.liuyao.ops.ratelimit;

import com.yishou.liuyao.common.exception.BusinessException;
import com.yishou.liuyao.common.exception.ErrorCode;
import com.yishou.liuyao.ops.ratelimit.domain.RateLimitBucket;
import com.yishou.liuyao.ops.ratelimit.repository.RateLimitBucketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.function.Supplier;

@Component
public class PersistentRateLimiter {

    private final RateLimitBucketRepository rateLimitBucketRepository;
    private final Supplier<LocalDate> currentDateSupplier;

    @Value("${liuyao.rate-limit.anonymous-per-day:5}")
    private int anonymousPerDay = 5;

    @Value("${liuyao.rate-limit.authenticated-per-day:20}")
    private int authenticatedPerDay = 20;

    @Autowired
    public PersistentRateLimiter(RateLimitBucketRepository rateLimitBucketRepository) {
        this(rateLimitBucketRepository, LocalDate::now);
    }

    PersistentRateLimiter(RateLimitBucketRepository rateLimitBucketRepository,
                          Supplier<LocalDate> currentDateSupplier) {
        this.rateLimitBucketRepository = rateLimitBucketRepository;
        this.currentDateSupplier = currentDateSupplier;
    }

    @Transactional
    public void acquire(Long userId) {
        LocalDate today = currentDateSupplier.get();
        rateLimitBucketRepository.deleteByBucketDateBefore(today);

        String principal = resolvePrincipal(userId);
        int limit = resolveLimit(userId);
        RateLimitBucket bucket = rateLimitBucketRepository.findByBucketDateAndPrincipal(today, principal)
                .orElseGet(() -> newBucket(today, principal, limit));

        int nextCount = (bucket.getRequestCount() == null ? 0 : bucket.getRequestCount()) + 1;
        if (nextCount > limit) {
            throw new BusinessException(
                    ErrorCode.RATE_LIMIT_EXCEEDED,
                    "今日请求次数已达上限，匿名用户最多 " + anonymousPerDay
                            + " 次/天，登录用户最多 " + authenticatedPerDay + " 次/天"
            );
        }

        bucket.setLimitValue(limit);
        bucket.setRequestCount(nextCount);
        rateLimitBucketRepository.save(bucket);
    }

    private RateLimitBucket newBucket(LocalDate today, String principal, int limit) {
        RateLimitBucket bucket = new RateLimitBucket();
        bucket.setBucketDate(today);
        bucket.setPrincipal(principal);
        bucket.setLimitValue(limit);
        bucket.setRequestCount(0);
        return bucket;
    }

    private String resolvePrincipal(Long userId) {
        return userId == null ? "anonymous" : "user:" + userId;
    }

    private int resolveLimit(Long userId) {
        return userId == null ? anonymousPerDay : authenticatedPerDay;
    }
}
