package com.rpis.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables the background revision-deadline reminder. The scheduler bean itself is
 * conditional on {@code rpis.notifications.deadline-reminders.enabled}, so enabling
 * scheduling here does not start any job in the test profile.
 */
@Configuration
@EnableScheduling
public class NotificationSchedulingConfig {
}
