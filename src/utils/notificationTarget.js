/**
 * notificationTarget.js
 * Where a notification should take the user.
 *
 * Proposal notifications carry the proposal they belong to (`proposalId`); Call
 * for Proposals notifications carry the announcement they were raised for
 * (`proposalAnnouncementId`). Those ids come from the saved notification record,
 * so the exact announcement a notification refers to is opened - nothing is
 * inferred from the notification text, title or ordering.
 */

/** Name of the Proponent route that shows a Call for Proposals announcement. */
export const ANNOUNCEMENT_DETAILS_ROUTE = 'ProponentAnnouncementDetails'

/** A usable positive database id, or null for anything else (missing, text, 0, -1). */
function asId(value) {
  if (value === null || value === undefined || value === '') return null
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

/**
 * The Call for Proposals announcement a notification points at, or null when the
 * notification is not about an announcement (or carries no usable reference).
 * Such a notification has no destination and only changes its read state.
 */
export function announcementIdFromNotification(notification) {
  return notification ? asId(notification.proposalAnnouncementId) : null
}
