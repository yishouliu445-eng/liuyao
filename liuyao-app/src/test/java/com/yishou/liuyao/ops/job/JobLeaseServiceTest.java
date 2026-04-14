package com.yishou.liuyao.ops.job;

import com.yishou.liuyao.ops.job.domain.JobLease;
import com.yishou.liuyao.ops.job.repository.JobLeaseRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=true")
class JobLeaseServiceTest {

    @Autowired
    private JobLeaseRepository jobLeaseRepository;

    @BeforeEach
    void setUp() {
        jobLeaseRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        jobLeaseRepository.deleteAll();
    }

    @Test
    void shouldRequireLeaseExpirationBeforeAnotherOwnerCanAcquire() {
        AtomicReference<LocalDateTime> currentTime = new AtomicReference<>(LocalDateTime.of(2026, 4, 14, 12, 0));
        JobLeaseService firstNode = new JobLeaseService(
                jobLeaseRepository,
                currentTime::get,
                "node-a",
                Duration.ofMinutes(5)
        );
        JobLeaseService secondNode = new JobLeaseService(
                jobLeaseRepository,
                currentTime::get,
                "node-b",
                Duration.ofMinutes(5)
        );

        assertTrue(firstNode.tryAcquire("session.closeInactiveSessions"));
        assertFalse(secondNode.tryAcquire("session.closeInactiveSessions"));

        currentTime.set(currentTime.get().plusMinutes(6));
        assertTrue(secondNode.tryAcquire("session.closeInactiveSessions"));

        JobLease lease = jobLeaseRepository.findByJobName("session.closeInactiveSessions").orElseThrow();
        assertEquals("node-b", lease.getOwnerId());
    }
}
