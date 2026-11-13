/**
 * notificationTarget.js
 * Where a notification should take the user.
 *
 * Two kinds of notification have a destination:
 *
 *   - an endorsement notification (`type = ENDORSEMENT`) is about ONE endorsement
 *     request. The request travels in `proposalId`, because the notification table has
 *     a single record reference and the type says what that reference is. The Call for
 *     Proposals the request answers travels separately in `proposalAnnouncementId` and
 *     is only displayed on the card - it is never a destination, because the Dean who
 *     receives the notification is not authorized to open a Proponent's announcement
 *     page;
 *   - a Call for Proposals notification (`type = PROPOSAL_ANNOUNCEMENT`) is about one
 *     announcement, which it carries in `proposalAnnouncementId`.
 *
 * Those ids come from the saved notification record, so the exact record a notification
 * refers to is opened - nothing is inferred from the notification text, title or
 * ordering. A notification with no usable reference has no destination and only changes
 * its read state.
 */

/** Name of the Proponent route that shows a Call for Proposals announcement. */
export const ANNOUNCEMENT_DETAILS_ROUTE = 'ProponentAnnouncementDetails'

/** Name of the Dean route that reviews one endorsement request. */
export const ENDORSEMENT_REVIEW_ROUTE = 'DeanEndorsementReview'

/** Name of the Dean route that lists the endorsement requests of their college. */
export const ENDORSEMENT_LIST_ROUTE = 'DeanEndorsementRequests'

/** Name of the Proponent route that follows one of their own endorsement requests. */
export const PROPONENT_ENDORSEMENT_DETAILS_ROUTE = 'ProponentEndorsementDetails'

/** Name of the Proponent route that lists their own endorsement requests. */
export const PROPONENT_ENDORSEMENTS_ROUTE = 'ProponentEndorsements'

/** The notification type of an endorsement notification. */
export const ENDORSEMENT_NOTIFICATION_TYPE = 'ENDORSEMENT'

/** The Faculty Proponent role value (UserRole.PROPONENT in stores/userData.js). */
export const PROPONENT_ROLE = 'PROPONENT'

/** A usable positive database id, or null for anything else (missing, text, 0, -1). */
function asId(value) {
  if (value === null || value === undefined || value === '') return null
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

/** True when the notification is about an endorsement request. */
export function isEndorsementNotification(notification) {
  return Boolean(notification) && notification.type === ENDORSEMENT_NOTIFICATION_TYPE
}

/**
 * The endorsement request a notification is about, or null when the notification is not
 * an endorsement notification (or carries no usable reference).
 */
export function endorsementRequestIdFromNotification(notification) {
  return isEndorsementNotification(notification) ? asId(notification.proposalId) : null
}

/**
 * The Call for Proposals announcement a notification points at, or null when the
 * notification is not about an announcement (or carries no usable reference).
 *
 * An endorsement notification is never an announcement notification, whatever the record
 * happens to carry: the request's own Call for Proposals is shown on the card and must
 * never be used as the destination.
 */
export function announcementIdFromNotification(notification) {
  if (!notification || isEndorsementNotification(notification)) return null
  return asId(notification.proposalAnnouncementId)
}

/**
 * Where clicking a notification takes `role`, or null when it has no destination - the
 * click then only changes the read state, exactly as before.
 *
 * An endorsement notification opens the endorsement request itself: the Dean reviews it
 * on the endorsement review screen, the Proponent follows it in their own requests. A
 * notification raised before the request was carried (no usable reference) opens that
 * role's endorsement list instead of failing on a record that cannot be resolved.
 */
export function notificationDestination(notification, role) {
  if (isEndorsementNotification(notification)) {
    const requestId = endorsementRequestIdFromNotification(notification)
    if (role === PROPONENT_ROLE) {
      return requestId
        ? { name: PROPONENT_ENDORSEMENT_DETAILS_ROUTE, params: { id: requestId } }
        : { name: PROPONENT_ENDORSEMENTS_ROUTE }
    }
    return requestId
      ? { name: ENDORSEMENT_REVIEW_ROUTE, params: { id: requestId } }
      : { name: ENDORSEMENT_LIST_ROUTE }
  }

  const announcementId = announcementIdFromNotification(notification)
  return announcementId
    ? { name: ANNOUNCEMENT_DETAILS_ROUTE, params: { id: announcementId } }
    : null
}
