/**
 * proposalNotificationTarget.js
 * ---------------------------------------------------------------------------
 * Where a proposal notification takes a REC or OVCRIGE user.
 *
 * The REC and OVCRIGE dashboards show the workflow notifications those offices receive
 * (for example "Proposal awaiting REC review and evaluation" or "REC has evaluated and
 * recommended proposal for approval"). Every one of them is raised by ProposalService
 * with the proposal it is about in `proposalId` - the notification table has a single
 * record reference and the type says what that reference is (see model/Notification.java).
 *
 * Clicking one opens that exact proposal's details page for the signed-in role:
 *
 *   - REC opens the detailed proposal view (`/rec-prop?id=...`, RecDetailedProp);
 *   - OVCRIGE opens the proposal review view (`/review-prop/:id`, OvcReviewProposal).
 *
 * Those are the pages each role already uses, so nothing new is routed. The id is the
 * saved record reference, never guessed from the notification text, title or ordering;
 * a notification without a usable reference has no destination and the click does nothing
 * (it also never changes the read state - the dashboards only navigate).
 *
 * The endorsement-request notifications (whose `proposalId` is an endorsement request, not
 * a proposal) are only ever sent to the Dean and the Proponent - see
 * EndorsementRequestService.notifyUser - so they never reach these two roles.
 */

/** The Research Evaluation Committee role (UserRole.REC in stores/userData.js). */
export const REC_ROLE = 'REC'

/** The OVCRIGE role (UserRole.OVCRIGE in stores/userData.js). */
export const OVCRIGE_ROLE = 'OVCRIGE'

/** Path of the REC route that shows one proposal's details (it reads the id from the query). */
export const REC_PROPOSAL_DETAILS_PATH = '/rec-prop'

/** Name of the OVCRIGE route that opens one proposal for review. */
export const OVCRIGE_PROPOSAL_DETAILS_ROUTE = 'OvcReviewProposal'

/** The RPS roles (RPS Admin and RPS Staff) that share the RPS proposal-details page. */
export const RPS_ROLES = ['RPS_ADMIN', 'RPS_STAFF']

/** Name of the RPS route that shows one proposal. */
export const RPS_PROPOSAL_DETAILS_ROUTE = 'ProposalDetails'

/** The Office of the Chancellor role, whose proposal page is the final-approval screen. */
export const OC_ROLE = 'OC'

/** Name of the Chancellor route that opens one proposal for final approval. */
export const OC_PROPOSAL_DETAILS_ROUTE = 'OCFinalApproval'

/** The OVCAF role, whose proposal page is the financial-validation screen. */
export const OVCAF_ROLE = 'OVCAF'

/** Path of the OVCAF route that opens one proposal (the id is a path parameter). */
export const OVCAF_PROPOSAL_DETAILS_PATH = '/ovcaf/proposals'

/** A usable positive database id, or null for anything else (missing, text, 0, -1). */
function asId(value) {
  if (value === null || value === undefined || value === '') return null
  const id = Number(value)
  return Number.isInteger(id) && id > 0 ? id : null
}

/**
 * The proposal a notification is about, or null when it carries no usable reference.
 */
export function proposalIdFromNotification(notification) {
  return notification ? asId(notification.proposalId) : null
}

/**
 * Where clicking a proposal notification takes `role`, or null when it has no destination -
 * the click then does nothing, exactly as before. Only the two dashboards that render these
 * notifications have a mapping; any other role resolves to null rather than a wrong page.
 */
export function proposalNotificationDestination(notification, role) {
  const proposalId = proposalIdFromNotification(notification)
  if (!proposalId) return null

  if (role === REC_ROLE) {
    return { path: REC_PROPOSAL_DETAILS_PATH, query: { id: proposalId } }
  }
  if (role === OVCRIGE_ROLE) {
    return { name: OVCRIGE_PROPOSAL_DETAILS_ROUTE, params: { id: proposalId } }
  }
  if (RPS_ROLES.includes(role)) {
    return { name: RPS_PROPOSAL_DETAILS_ROUTE, params: { id: proposalId } }
  }
  if (role === OC_ROLE) {
    return { name: OC_PROPOSAL_DETAILS_ROUTE, params: { id: proposalId } }
  }
  if (role === OVCAF_ROLE) {
    return { path: `${OVCAF_PROPOSAL_DETAILS_PATH}/${proposalId}` }
  }
  return null
}
