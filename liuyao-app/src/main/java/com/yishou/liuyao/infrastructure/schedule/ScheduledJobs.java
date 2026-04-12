package com.yishou.liuyao.infrastructure.schedule;

import com.yishou.liuyao.calendar.service.VerificationEventService;
import com.yishou.liuyao.session.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobs.class);

    private final SessionService sessionService;
    private final VerificationEventService verificationEventService;

    public ScheduledJobs(SessionService sessionService, VerificationEventService verificationEventService) {
        this.sessionService = sessionService;
        this.verificationEventService = verificationEventService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void closeInactiveSessions() {
        int closedCount = sessionService.closeInactiveSessions();
        log.info("定时关闭超时 Session 完成: closedCount={}", closedCount);
    }

    @Scheduled(cron = "0 0 9 * * *")
    public void sendVerificationReminders() {
        int reminderCount = verificationEventService.sendDueReminders();
        log.info("定时发送应验提醒完成: reminderCount={}", reminderCount);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void expireOverdueVerificationEvents() {
        int expiredCount = verificationEventService.markExpiredEvents();
        log.info("定时标记过期应验事件完成: expiredCount={}", expiredCount);
    }
}
