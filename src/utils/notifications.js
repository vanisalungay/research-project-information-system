/**
 * notifications.js
 * ---------------------------------------------------------------------------
 * Small, pure rules about notifications that the application shell needs (the dot on
 * the header bell and the unread badge in the sidebar).
 *
 * The shell reads the same notification list the notification pages read, so the
 * rules here only mirror what those pages already do (`!notification.isRead`); they
 * are kept in one place so the header and the sidebar can never disagree.
 */

/** True when a notification has not been read yet. */
export function isUnreadNotification(notification) {
  if (!notification || typeof notification !== 'object') return false
  // The API serialises the flag as `isRead`; a couple of older screens read `read`.
  const read = notification.isRead ?? notification.read
  return !read
}

/** How many of the given notifications are unread (0 for a missing/invalid list). */
export function unreadCountOf(notifications) {
  return (Array.isArray(notifications) ? notifications : []).filter(isUnreadNotification).length
}

/**
 * The count as it is shown in a small badge: everything above 99 is capped, so a long
 * number can never break the row it sits in.
 */
export function unreadBadgeLabel(count) {
  const value = Number.isFinite(Number(count)) ? Math.max(0, Math.trunc(Number(count))) : 0
  if (value === 0) return ''
  return value > 99 ? '99+' : String(value)
}
