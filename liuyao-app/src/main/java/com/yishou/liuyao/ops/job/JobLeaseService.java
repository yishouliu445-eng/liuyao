package com.yishou.liuyao.ops.job;

import com.yishou.liuyao.ops.job.domain.JobLease;
import com.yishou.liuyao.ops.job.repository.JobLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@Service
public class JobLeaseService {

    private static final Logger log = LoggerFactory.getLogger(JobLeaseService.class);

    private final JobLeaseRepository jobLeaseRepository;
    private final Supplier<LocalDateTime> currentTimeSupplier;
    private final String ownerId;
    private final Duration leaseDuration;
    private final AtomicBoolean persistenceEnabled = new AtomicBoolean(true);

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
        if (!persistenceEnabled.get()) {
            return true;
        }

        try {
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
        } catch (RuntimeException exception) {
            if (!isMissingLeaseTable(exception)) {
                throw exception;
            }
            persistenceEnabled.set(false);
            log.warn("任务租约持久化不可用，已降级为单机放行: {}", exception.getMessage());
            return true;
        }
    }

    private boolean isMissingLeaseTable(RuntimeException exception) {
        Throwable current = exception;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && (message.contains("relation \"job_lease\" does not exist")
                    || message.contains("relation job_lease does not exist")
                    || message.contains("Table \"JOB_LEASE\" not found"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
