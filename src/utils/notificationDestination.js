/**
 * notificationDestination.js
 * ---------------------------------------------------------------------------
 * The single answer to "where should clicking this notification take this user?".
 *
 * The destination is resolved from the saved notification record (its `type` and the
 * reference id) and the signed-in role. It is never inferred from the title or message
 * text. The only role-dependent exception is the legacy `ENDORSEMENT` row: before the
 * dedicated `PROPOSAL_ENDORSED` type existed, OVCRIGE and OVCAF received that type for
 * a proposal that was forwarded to them, so those rows are opened as proposals for
 * those two roles while the Dean and the Proponent still see an endorsement request.
 *
 * A null result means there is no page this user may open for the record. Callers must
 * show a message instead of navigating to the Dashboard.
 */
import { NOTIFICATION_TYPES } from './notificationTypes.js'

export const ROLES = Object.freeze({
  PROPONENT: 'PROPONENT',
  RPS_ADMIN: 'RPS_ADMIN',
  RPS_STAFF: 'RPS_STAFF',
  DEAN: 'DEAN',
  OVCRIGE: 'OVCRIGE',
  OVCAF: 'OVCAF',
  REC: 'REC',
  OC: 'OC',
})

/** A usable positive database id, or null for anything else (missing, text, 0, -1). */
export function asNotificationId(value) {
  if (value === null || value === undefined || value === '') return null
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

/**
 * The proposal a notification may open, resolved by the backend.
 *
 * `linkedProposalId` is the backend's answer: the proposal for a proposal-workflow row,
 * the proposal behind an endorsement request for an `ENDORSEMENT` row, or null when the
 * linked record no longer exists. When the field is present at all its value wins - a
 * deliberate null must not fall back to the polymorphic `proposalId`, which may be an
 * endorsement-request id or a dangling reference. Payloads from an older backend do not
 * carry the field, so `proposalId` remains the fallback.
 */
export function notificationProposalId(notification) {
  if (!notification || typeof notification !== 'object') return null
  const hasResolvedLink = Object.prototype.hasOwnProperty.call(notification, 'linkedProposalId')
  return asNotificationId(hasResolvedLink ? notification.linkedProposalId : notification.proposalId)
}

/** The proposal page each role owns. Null for a role with no proposal page (Dean). */
export function proposalDestinationForRole(proposalId, role) {
  const id = asNotificationId(proposalId)
  if (!id) return null

  switch (role) {
    case ROLES.PROPONENT:
      return { name: 'ProponentPropDetails', params: { id } }
    case ROLES.RPS_ADMIN:
    case ROLES.RPS_STAFF:
      return { name: 'ProposalDetails', params: { id } }
    case ROLES.REC:
      return { path: '/rec-prop', query: { id } }
    case ROLES.OVCRIGE:
      return { name: 'OvcReviewProposal', params: { id } }
    case ROLES.OVCAF:
      return { path: `/ovcaf/proposals/${id}` }
    case ROLES.OC:
      return { name: 'OCFinalApproval', params: { id } }
    default:
      return null
  }
}

/** The report page each role owns for a quarterly-report or completion notification. */
function reportDestinationForRole(proposalId, role) {
  const id = asNotificationId(proposalId)
  if (!id) return null

  if (role === ROLES.PROPONENT) {
    return { name: 'ProponentQuarterlyReports', params: { proposalId: id } }
  }
  if (role === ROLES.RPS_ADMIN || role === ROLES.RPS_STAFF) {
    return { name: 'RpsQuarterlyReports', query: { proposalId: id } }
  }
  if (role === ROLES.OVCRIGE) {
    return { name: 'OvcQuarterlyReports', query: { proposalId: id } }
  }
  return null
}

/** The Dean endorsement screens. */
function endorsementDestination(notification, role) {
  const requestId = asNotificationId(notification?.proposalId)

  if (role === ROLES.DEAN) {
    return requestId
      ? { name: 'DeanEndorsementReview', params: { id: requestId } }
      : { name: 'DeanEndorsementRequests' }
  }
  if (role === ROLES.PROPONENT) {
    return requestId
      ? { name: 'ProponentEndorsementDetails', params: { id: requestId } }
      : { name: 'ProponentEndorsements' }
  }
  // Historical proposal-forwarding rows addressed to OVCRIGE/OVCAF. Their `proposalId` is
  // polymorphic, so the backend's resolved proposal link is used instead.
  if (role === ROLES.OVCRIGE || role === ROLES.OVCAF) {
    return proposalDestinationForRole(notificationProposalId(notification), role)
  }
  return null
}

/**
 * Where clicking `notification` takes `role`, or null when it has no destination this
 * role may open.
 */
export function resolveNotificationDestination(notification, role) {
  if (!notification || typeof notification !== 'object') return null
  const type = String(notification.type || '').toUpperCase()

  if (type === NOTIFICATION_TYPES.PROPOSAL_ANNOUNCEMENT) {
    const announcementId = asNotificationId(notification.proposalAnnouncementId)
    return role === ROLES.PROPONENT && announcementId
      ? { name: 'ProponentAnnouncementDetails', params: { id: announcementId } }
      : null
  }

  if (type === NOTIFICATION_TYPES.ENDORSEMENT) {
    return endorsementDestination(notification, role)
  }

  if (type === NOTIFICATION_TYPES.QUARTERLY_REPORT ||
      type === NOTIFICATION_TYPES.PROJECT_COMPLETION) {
    return reportDestinationForRole(notificationProposalId(notification), role)
  }

  if (type === NOTIFICATION_TYPES.DEAN_ASSIGNMENT) {
    // There is no per-assignment page; the account's own profile shows the role and
    // scope that changed. This is a real destination, not a Dashboard fallback.
    return { path: '/manage-profile' }
  }

  if (type === NOTIFICATION_TYPES.SPECIAL_ORDER_DRAFT) {
    // OVCAF cannot open a proposal whose OC approval is not finalized: the detail
    // service requires soUploadedAt, which a draft never sets. Show the honest
    // "nothing to open yet" message instead of routing to a page that returns 404.
    return null
  }

  // An OVCRIGE rejection is returned to ORPS, whose office completes the decision.
  // ORPS opens the return screen (where the rejection is forwarded to the proponent),
  // not the read-only proposal details.
  if (
    (role === ROLES.RPS_ADMIN || role === ROLES.RPS_STAFF) &&
    type === NOTIFICATION_TYPES.REJECTION
  ) {
    const proposalId = notificationProposalId(notification)
    if (proposalId) return { name: 'RpsForwardRevision', params: { id: proposalId } }
  }

  // A Proponent's revision notification opens the revision form directly. Budget
  // returns use FUNDING, not REVISION, so those still open the proposal details where
  // the financial feedback is shown.
  if (
    role === ROLES.PROPONENT &&
    (type === NOTIFICATION_TYPES.REVISION ||
      type === NOTIFICATION_TYPES.REVISION_DEADLINE_REMINDER)
  ) {
    const proposalId = notificationProposalId(notification)
    if (proposalId) return { path: `/submit-revision/${proposalId}` }
  }

  // Proposal-workflow types, and any older/untyped row that still carries a proposal
  // reference, open the proposal page the role owns.
  const proposalDestination = proposalDestinationForRole(notificationProposalId(notification), role)
  if (proposalDestination) return proposalDestination

  // A notification about an announcement that is not of the announcement type (for
  // example an old row) still has a valid Proponent destination.
  if (role === ROLES.PROPONENT) {
    const announcementId = asNotificationId(notification.proposalAnnouncementId)
    if (announcementId) {
      return { name: 'ProponentAnnouncementDetails', params: { id: announcementId } }
    }
  }
  return null
}

/** Compatibility wrappers for callers of the previous two resolver modules. */
export function notificationDestination(notification, role) {
  return resolveNotificationDestination(notification, role)
}

export function proposalNotificationDestination(notification, role) {
  if (!notification) return null
  return proposalDestinationForRole(notificationProposalId(notification), role)
}
