package com.yishou.liuyao.infrastructure.schedule;

import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.ops.job.JobLeaseService;
import com.yishou.liuyao.session.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);
    static final String CLOSE_INACTIVE_SESSIONS_JOB = "session.closeInactiveSessions";
    static final String SEND_VERIFICATION_REMINDERS_JOB = "verification.sendDueReminders";
    static final String EXPIRE_VERIFICATION_EVENTS_JOB = "verification.expireOverdueEvents";

    private final SessionService sessionService;
    private final VerificationEventService verificationEventService;
    private final JobLeaseService jobLeaseService;

    public ScheduledJobs(SessionService sessionService,
                         VerificationEventService verificationEventService,
                         JobLeaseService jobLeaseService) {
        this.sessionService = sessionService;
        this.verificationEventService = verificationEventService;
        this.jobLeaseService = jobLeaseService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void closeInactiveSessions() {
        if (!jobLeaseService.tryAcquire(CLOSE_INACTIVE_SESSIONS_JOB)) {
            log.info("跳过定时关闭超时 Session，本节点未获取到租约");
            return;
        }
        int closedCount = sessionService.closeInactiveSessions();
        log.info("定时关闭超时 Session 完成: closedCount={}", closedCount);
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendVerificationReminders() {
        if (!jobLeaseService.tryAcquire(SEND_VERIFICATION_REMINDERS_JOB)) {
            log.info("跳过定时发送应验提醒，本节点未获取到租约");
            return;
        }
        int reminderCount = verificationEventService.sendDueReminders();
        log.info("定时发送应验提醒完成: reminderCount={}", reminderCount);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueVerificationEvents() {
        if (!jobLeaseService.tryAcquire(EXPIRE_VERIFICATION_EVENTS_JOB)) {
            log.info("跳过定时过期应验事件，本节点未获取到租约");
            return;
        }
        int expiredCount = verificationEventService.markExpiredEvents();
        log.info("定时标记过期应验事件完成: expiredCount={}", expiredCount);
    }
}
