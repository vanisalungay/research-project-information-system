/**
 * Compatibility facade for the old proposal-only resolver.
 *
 * `notificationDestination.js` is the single resolver; this module keeps the role
 * constants, the path constants and the two helper names the dashboards and tests use.
 */
import {
  proposalDestinationForRole,
  asNotificationId,
} from './notificationDestination.js'

export const REC_ROLE = 'REC'
export const OVCRIGE_ROLE = 'OVCRIGE'
export const REC_PROPOSAL_DETAILS_PATH = '/rec-prop'
export const OVCRIGE_PROPOSAL_DETAILS_ROUTE = 'OvcReviewProposal'
export const RPS_ROLES = ['RPS_ADMIN', 'RPS_STAFF']
export const RPS_PROPOSAL_DETAILS_ROUTE = 'ProposalDetails'
export const OC_ROLE = 'OC'
export const OC_PROPOSAL_DETAILS_ROUTE = 'OCFinalApproval'
export const OVCAF_ROLE = 'OVCAF'
export const OVCAF_PROPOSAL_DETAILS_PATH = '/ovcaf/proposals'
export const PROPONENT_ROLE = 'PROPONENT'
export const PROPONENT_PROPOSAL_DETAILS_ROUTE = 'ProponentPropDetails'

export function proposalIdFromNotification(notification) {
  return notification ? asNotificationId(notification.proposalId) : null
}

/**
 * Where clicking a proposal notification takes `role`, or null when the role has no
 * proposal page (the Dean) or the notification carries no usable id.
 */
export function proposalNotificationDestination(notification, role) {
  return proposalDestinationForRole(notification?.proposalId, role)
}
