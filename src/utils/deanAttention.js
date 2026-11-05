/**
 * deanAttention.js
 * ---------------------------------------------------------------------------
 * How many items are waiting for a Dean on their dashboard.
 *
 * The Dean's dashboard is the one page that holds the endorsement requests of the Dean's
 * own college and the Dean's notifications, so the sidebar badge next to Dashboard counts
 * both. The two sources must not be counted twice: deciding an endorsement request also
 * raises a notification of type ENDORSEMENT for the same event (EndorsementRequestService),
 * so those notifications are excluded from the notification half of the count.
 *
 * Both inputs are real records - the requests come from GET /api/endorsements (which the
 * backend already scopes to the signed-in Dean's college) and the notifications from the
 * shared notification store. Nothing is hardcoded.
 */

import { isUnreadNotification } from './notifications.js'
import { isEndorsementNotification } from './notificationTarget.js'

/** A safe non-negative whole number, so a failed request never invents a badge. */
function asCount(value) {
  const n = Number(value)
  return Number.isFinite(n) && n > 0 ? Math.trunc(n) : 0
}

/** True when a notification is unread and is not about an endorsement request. */
function isOtherUnreadNotification(notification) {
  return isUnreadNotification(notification) && !isEndorsementNotification(notification)
}

/**
 * The number of items waiting on the Dean's dashboard: the pending endorsement requests
 * plus the unread notifications that are not the endorsement notification of one of those
 * requests. A missing or invalid input counts as zero.
 */
export function deanAttentionCount(pendingRequests, notifications) {
  const requests = asCount(pendingRequests)
  const unread = (Array.isArray(notifications) ? notifications : []).filter(
    isOtherUnreadNotification,
  ).length
  return requests + unread
}
