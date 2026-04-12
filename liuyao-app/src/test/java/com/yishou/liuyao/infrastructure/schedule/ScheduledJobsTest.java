package com.yishou.liuyao.infrastructure.schedule;

import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.session.service.SessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ScheduledJobsTest {

    @Mock
    private SessionService sessionService;
    @Mock
    private VerificationEventService verificationEventService;

    @Test
    void shouldDelegateScheduledTasksToServices() {
        ScheduledJobs jobs = new ScheduledJobs(sessionService, verificationEventService);

        jobs.closeInactiveSessions();
        jobs.sendVerificationReminders();
        jobs.expireOverdueVerificationEvents();

        verify(sessionService).closeInactiveSessions();
        verify(verificationEventService).sendDueReminders();
        verify(verificationEventService).markExpiredEvents();
    }
}
