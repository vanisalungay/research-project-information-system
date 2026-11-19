package com.rpis.backend.service;

/**
 * Raised after a notification's read state changes, so every open session of the same
 * account can update its badge and list without a reload.
 *
 * @param reason {@code "read"} for one notification, {@code "read-all"} for the bulk action
 */
public record NotificationChangedEvent(Long userId, String reason) {
}
