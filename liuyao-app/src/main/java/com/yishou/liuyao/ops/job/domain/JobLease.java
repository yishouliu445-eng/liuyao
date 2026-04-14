package com.yishou.liuyao.ops.job.domain;

import com.yishou.liuyao.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "job_lease")
public class JobLease extends BaseEntity {

    @Column(name = "job_name", nullable = false)
    private String jobName;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "lease_until", nullable = false)
    private LocalDateTime leaseUntil;

    @Column(name = "last_acquired_at", nullable = false)
    private LocalDateTime lastAcquiredAt;

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDateTime getLeaseUntil() {
        return leaseUntil;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public LocalDateTime getLastAcquiredAt() {
        return lastAcquiredAt;
    }

    public void setLastAcquiredAt(LocalDateTime lastAcquiredAt) {
        this.lastAcquiredAt = lastAcquiredAt;
    }
}
