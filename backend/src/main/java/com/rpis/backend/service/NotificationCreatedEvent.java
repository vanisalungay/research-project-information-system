package com.rpis.backend.service;

/**
 * Raised after a notification row is saved. A transactional listener forwards it to the
 * recipient's open SSE streams only once the surrounding workflow transaction commits,
 * so an action that fails (and rolls back) never reaches a browser.
 */
public record NotificationCreatedEvent(Long notificationId, Long userId) {
}
