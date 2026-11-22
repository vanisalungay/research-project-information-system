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

/** Roles for which ENDORSEMENT means a Dean endorsement request. */
const ENDORSEMENT_REQUEST_ROLES = ['DEAN', 'PROPONENT']

/** A type that is an administrative account event, not a proposal update. */
const ADMINISTRATIVE_TYPES = ['DEAN_ASSIGNMENT']

/**
 * The kind of a notification, from its saved `type` field.
 *
 * `ENDORSEMENT` is role-dependent for backward compatibility: rows written before the
 * dedicated `PROPOSAL_ENDORSED` type carry it even when they were a proposal forwarded
 * to OVCRIGE/OVCAF. For those roles the row belongs with the proposal updates; a Dean
 * or a Proponent really is looking at an endorsement request. Without a role the
 * historical meaning (endorsement) is kept so older callers do not change behaviour.
 */
export function notificationKind(notification, role = null) {
  const type = String(notification?.type || '').toUpperCase()
  if (type === 'ENDORSEMENT') {
    if (!role) return 'endorsement'
    return ENDORSEMENT_REQUEST_ROLES.includes(role) ? 'endorsement' : PROPOSAL_KIND
  }
  if (type === 'PROPOSAL_ANNOUNCEMENT') return 'call'
  if (ADMINISTRATIVE_TYPES.includes(type)) return 'other'
  if (type) return PROPOSAL_KIND
  return 'other'
}

/**
 * Splits a notification list into the proposal updates and everything else, preserving the
 * order it was given (the API returns newest first). A missing or invalid list yields two
 * empty lists rather than failing.
 */
export function groupNotifications(notifications, role = null) {
  const list = Array.isArray(notifications) ? notifications : []
  const proposalUpdates = []
  const others = []
  for (const notification of list) {
    if (notificationKind(notification, role) === PROPOSAL_KIND) {
      proposalUpdates.push(notification)
    } else {
      others.push(notification)
    }
  }
  return { proposalUpdates, others }
}
