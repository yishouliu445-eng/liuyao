package com.yishou.liuyao.ops.ratelimit.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "rate_limit_bucket")
public class RateLimitBucket extends BaseEntity {

    @Column(name = "bucket_date", nullable = false)
    private LocalDate bucketDate;

    @Column(name = "principal", nullable = false)
    private String principal;

    @Column(name = "request_count", nullable = false)
    private Integer requestCount;

    @Column(name = "limit_value", nullable = false)
    private Integer limitValue;

    public LocalDate getBucketDate() {
        return bucketDate;
    }

    public void setBucketDate(LocalDate bucketDate) {
        this.bucketDate = bucketDate;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public Integer getLimitValue() {
        return limitValue;
    }

    public void setLimitValue(Integer limitValue) {
        this.limitValue = limitValue;
    }
}
