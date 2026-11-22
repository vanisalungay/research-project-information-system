/**
 * Compatibility facade for the old endorsement/announcement resolver.
 *
 * The actual decision now lives in `notificationDestination.js`, which resolves every
 * notification type for every role. This module keeps the names and route constants the
 * older components and tests import.
 */
import {
  resolveNotificationDestination,
  asNotificationId,
} from './notificationDestination.js'

export const ANNOUNCEMENT_DETAILS_ROUTE = 'ProponentAnnouncementDetails'
export const ENDORSEMENT_REVIEW_ROUTE = 'DeanEndorsementReview'
export const ENDORSEMENT_LIST_ROUTE = 'DeanEndorsementRequests'
export const PROPONENT_ENDORSEMENT_DETAILS_ROUTE = 'ProponentEndorsementDetails'
export const PROPONENT_ENDORSEMENTS_ROUTE = 'ProponentEndorsements'
export const ENDORSEMENT_NOTIFICATION_TYPE = 'ENDORSEMENT'
export const PROPONENT_ROLE = 'PROPONENT'
export const DEAN_ROLE = 'DEAN'

export function isEndorsementNotification(notification) {
  return Boolean(notification) && notification.type === ENDORSEMENT_NOTIFICATION_TYPE
}

export function endorsementRequestIdFromNotification(notification) {
  return isEndorsementNotification(notification) ? asNotificationId(notification.proposalId) : null
}

export function announcementIdFromNotification(notification) {
  if (!notification || isEndorsementNotification(notification)) return null
  return asNotificationId(notification.proposalAnnouncementId)
}

/** Where clicking a notification takes `role`, or null when it has no destination. */
export function notificationDestination(notification, role) {
  return resolveNotificationDestination(notification, role)
}
