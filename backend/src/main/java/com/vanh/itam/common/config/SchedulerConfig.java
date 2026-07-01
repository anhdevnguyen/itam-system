package com.vanh.itam.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Kích hoạt @Scheduled (AuditService auto-expire) và @Async (EmailNotificationService).
 */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulerConfig {
}
