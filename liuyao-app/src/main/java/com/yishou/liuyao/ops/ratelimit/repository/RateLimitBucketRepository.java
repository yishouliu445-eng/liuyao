package com.yishou.liuyao.ops.ratelimit.repository;

import com.yishou.liuyao.ops.ratelimit.domain.RateLimitBucket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface RateLimitBucketRepository extends JpaRepository<RateLimitBucket, Long> {

    Optional<RateLimitBucket> findByBucketDateAndPrincipal(LocalDate bucketDate, String principal);

    long deleteByBucketDateBefore(LocalDate bucketDate);
}
