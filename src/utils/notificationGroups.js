/**
 * notificationGroups.js
 * ---------------------------------------------------------------------------
 * How the notification list is split for display.
 *
 * The page shows every notification it receives, but the proposal review/processing updates
 * (by far the busiest kind, especially for the reviewing offices) are gathered under their
 * own "Proposal Updates" group so the page is not one long flat list. Nothing is dropped:
 * the two lists together always hold exactly the records that were passed in, in the same
 * order.
 *
 * The kind comes from the saved `type` field, never from the message text.
 */

/** The kind of a proposal review/processing update (the group's contents). */
export const PROPOSAL_KIND = 'proposal'

/**
 * The kind of a notification, from its saved `type` field. The proposal workflow raises
 * PROPOSAL_UPDATE / REVIEW_REQUEST / APPROVAL / REVISION / REJECTION / FUNDING; an
 * endorsement raises ENDORSEMENT; a Call for Proposals raises PROPOSAL_ANNOUNCEMENT; a
 * notification with no type is an administrative one.
 */
export function notificationKind(notification) {
  const type = String(notification?.type || '').toUpperCase()
  if (type === 'ENDORSEMENT') return 'endorsement'
  if (type === 'PROPOSAL_ANNOUNCEMENT') return 'call'
  if (type) return PROPOSAL_KIND
  return 'other'
}

/**
 * Splits a notification list into the proposal updates and everything else, preserving the
 * order it was given (the API returns newest first). A missing or invalid list yields two
 * empty lists rather than failing.
 */
export function groupNotifications(notifications) {
  const list = Array.isArray(notifications) ? notifications : []
  const proposalUpdates = []
  const others = []
  for (const notification of list) {
    if (notificationKind(notification) === PROPOSAL_KIND) {
      proposalUpdates.push(notification)
    } else {
      others.push(notification)
    }
  }
  return { proposalUpdates, others }
}
