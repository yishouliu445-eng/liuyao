package com.yishou.liuyao.ops.job;

import com.yishou.liuyao.ops.job.domain.JobLease;
import com.yishou.liuyao.ops.job.repository.JobLeaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class JobLeaseService {

    private final JobLeaseRepository jobLeaseRepository;
    private final Supplier<LocalDateTime> currentTimeSupplier;
    private final String ownerId;
    private final Duration leaseDuration;

    @Autowired
    public JobLeaseService(JobLeaseRepository jobLeaseRepository,
                           @Value("${liuyao.ops.job.lease-seconds:300}") long leaseSeconds) {
        this(jobLeaseRepository, LocalDateTime::now, "node-" + UUID.randomUUID(), Duration.ofSeconds(leaseSeconds));
    }

    JobLeaseService(JobLeaseRepository jobLeaseRepository,
                    Supplier<LocalDateTime> currentTimeSupplier,
                    String ownerId,
                    Duration leaseDuration) {
        this.jobLeaseRepository = jobLeaseRepository;
        this.currentTimeSupplier = currentTimeSupplier;
        this.ownerId = ownerId;
        this.leaseDuration = leaseDuration;
    }

    @Transactional
    public boolean tryAcquire(String jobName) {
        LocalDateTime now = currentTimeSupplier.get();
        JobLease lease = jobLeaseRepository.findByJobName(jobName).orElse(null);
        if (lease == null) {
            lease = new JobLease();
            lease.setJobName(jobName);
            lease.setOwnerId(ownerId);
            lease.setLastAcquiredAt(now);
            lease.setLeaseUntil(now.plus(leaseDuration));
            jobLeaseRepository.save(lease);
            return true;
        }

        boolean isOwnedByCurrentNode = ownerId.equals(lease.getOwnerId());
        boolean isExpired = !lease.getLeaseUntil().isAfter(now);
        if (!isOwnedByCurrentNode && !isExpired) {
            return false;
        }

        lease.setOwnerId(ownerId);
        lease.setLastAcquiredAt(now);
        lease.setLeaseUntil(now.plus(leaseDuration));
        jobLeaseRepository.save(lease);
        return true;
    }
}
