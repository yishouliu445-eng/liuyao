package com.yishou.liuyao.infrastructure.schedule;

import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.ops.job.JobLeaseService;
import com.yishou.liuyao.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledJobsTest {

    @Mock
    private SessionService sessionService;
    @Mock
    private VerificationEventService verificationEventService;
    @Mock
    private JobLeaseService jobLeaseService;

    @Test
    void shouldDelegateScheduledTasksToServicesAfterLeaseAcquired() {
        when(jobLeaseService.tryAcquire(ScheduledJobs.CLOSE_INACTIVE_SESSIONS_JOB)).thenReturn(true);
        when(jobLeaseService.tryAcquire(ScheduledJobs.SEND_VERIFICATION_REMINDERS_JOB)).thenReturn(true);
        when(jobLeaseService.tryAcquire(ScheduledJobs.EXPIRE_VERIFICATION_EVENTS_JOB)).thenReturn(true);

        ScheduledJobs jobs = new ScheduledJobs(sessionService, verificationEventService, jobLeaseService);

        jobs.closeInactiveSessions();
        jobs.sendVerificationReminders();
        jobs.expireOverdueVerificationEvents();

        verify(jobLeaseService).tryAcquire(ScheduledJobs.CLOSE_INACTIVE_SESSIONS_JOB);
        verify(jobLeaseService).tryAcquire(ScheduledJobs.SEND_VERIFICATION_REMINDERS_JOB);
        verify(jobLeaseService).tryAcquire(ScheduledJobs.EXPIRE_VERIFICATION_EVENTS_JOB);
        verify(sessionService).closeInactiveSessions();
        verify(verificationEventService).sendDueReminders();
        verify(verificationEventService).markExpiredEvents();
    }

    @Test
    void shouldSkipScheduledTasksWhenLeaseIsNotAcquired() {
        when(jobLeaseService.tryAcquire(ScheduledJobs.CLOSE_INACTIVE_SESSIONS_JOB)).thenReturn(false);
        when(jobLeaseService.tryAcquire(ScheduledJobs.SEND_VERIFICATION_REMINDERS_JOB)).thenReturn(false);
        when(jobLeaseService.tryAcquire(ScheduledJobs.EXPIRE_VERIFICATION_EVENTS_JOB)).thenReturn(false);

        ScheduledJobs jobs = new ScheduledJobs(sessionService, verificationEventService, jobLeaseService);

        jobs.closeInactiveSessions();
        jobs.sendVerificationReminders();
        jobs.expireOverdueVerificationEvents();

        verify(jobLeaseService).tryAcquire(ScheduledJobs.CLOSE_INACTIVE_SESSIONS_JOB);
        verify(jobLeaseService).tryAcquire(ScheduledJobs.SEND_VERIFICATION_REMINDERS_JOB);
        verify(jobLeaseService).tryAcquire(ScheduledJobs.EXPIRE_VERIFICATION_EVENTS_JOB);
        verifyNoInteractions(sessionService);
        verifyNoInteractions(verificationEventService);
    }
}
