/**
 * proposalStatus.js
 * Shared proposal-status helpers for the proponent interface.
 *
 * A Proponent submission is stored as `FOR REVIEW` (uppercase, with a space). Proposals
 * saved before that wording change carry the legacy `SUBMITTED` value, which means the
 * exact same workflow state: every helper here treats the two as equivalent, so old and
 * new rows display, filter and count the same way.
 *
 * `APPROVED` means approved for implementation; it is distinct from `COMPLETED`
 * (finished reporting). The stored value is left untouched - backend filters and status
 * history keep reading it - while the badge reads "Implementation Approved" and the
 * row action "Open". `FOR_OC_APPROVAL` / `FOR_OVCAF_APPROVAL` are still pending and keep
 * their own wording.
 */
import { statusLabel } from './statusLabel.js'
import {
  PROPOSAL_STATUS_LABELS,
  PROPOSAL_TIMELINE_STATUS_LABELS,
} from './statusCatalog.js'

/** The stored status of a proposal the Proponent has submitted and ORPS must review. */
export const FOR_REVIEW = 'FOR REVIEW'

/** Legacy spelling of the same state, kept for proposals saved before the rename. */
export const LEGACY_SUBMITTED = 'SUBMITTED'

/**
 * The statuses the proposal workflow reports. Kept in one place so the proponent
 * interface never has to hardcode a single status when it reasons about a
 * proposal (see the status filter of My Projects and the details page badge).
 */
export const PROPOSAL_STATUSES = [
  'DRAFT',
  FOR_REVIEW,
  LEGACY_SUBMITTED,
  'UNDER_REVIEW',
  'RPS_REVIEW',
  'RPS_RETURNED',
  'OVCRIGE_COORDINATION',
  'REC_EVALUATION',
  'CHANCELLOR_REVIEW',
  'FINANCE_AUTHORIZATION',
  'APPROVED',
  'REJECTED',
  'REVISION',
  'RETURNED_TO_RPS',
  'RELEASED',
  'IMPLEMENTATION',
  'COMPLETED',
]

/** The stored status of a proposal whose final approval step has completed. */
export const APPROVED = 'APPROVED'

/**
 * The statuses at or past final approval: the proposal is now a finished project. They are
 * the statuses whose row action reads "Open" instead of "View".
 */
export const PROJECT_STATUSES = [
  APPROVED,
  'READY_FOR_RELEASE',
  'RELEASED',
  'IMPLEMENTATION',
  'COMPLETED',
]

/**
 * True only for the stored `COMPLETED` status (required reporting finished).
 * `APPROVED` (approved for implementation) is deliberately not included.
 */
export function isCompletedStatus(status) {
  return normalizeProposalStatus(status) === 'COMPLETED'
}

/** True when the proposal has passed final approval and is displayed as a project. */
export function isApprovedProject(status) {
  return PROJECT_STATUSES.includes(normalizeProposalStatus(status))
}

/** The row action for a proposal/project: "Open" once fully approved, else "View". */
export function proposalActionLabel(status) {
  return isApprovedProject(status) ? 'Open' : 'View'
}

/**
 * The statuses whose proposal content may be reused through "Make a Copy".
 *
 * Every status is copyable. The copy is a brand-new DRAFT with its own proposal
 * code / document id that inherits none of the original's workflow state (status,
 * review comments, decision history, endorsement/approval records, SO number,
 * signatures, reports or submission timestamps), and the proponent must review
 * and submit it through the normal workflow. So reusing the content of a rejected,
 * pending, under-review, approved, returned-for-revision or completed proposal is
 * always allowed - the action never depends on where the proposal stands.
 */
export const COPYABLE_STATUSES = [...PROPOSAL_STATUSES]

/** The stored status of a proposal, normalized for comparison ('' when unusable). */
export function normalizeProposalStatus(status) {
  return String(status ?? '')
    .trim()
    .toUpperCase()
}

/** True for both spellings of the submitted/for-review state. */
export function isForReviewStatus(status) {
  const value = normalizeProposalStatus(status)
  return value === FOR_REVIEW || value === LEGACY_SUBMITTED
}

/**
 * The one canonical spelling of a stored status: a legacy `SUBMITTED` becomes
 * `FOR REVIEW`, every other value is only normalized. Used where a status is compared
 * against a workflow stage key (e.g. the Proposal Monitoring timeline).
 */
export function canonicalProposalStatus(status) {
  return isForReviewStatus(status) ? FOR_REVIEW : normalizeProposalStatus(status)
}

/**
 * True when a stored status matches a filter value. A filter for either submission
 * spelling matches both, so selecting "For RPS Review" also returns legacy `SUBMITTED`
 * rows. Every other comparison is an exact normalized match (never a substring match);
 * in particular `APPROVED` (approved for implementation) and `COMPLETED` (finished
 * reporting) are different statuses and each filter matches only its own value.
 */
export function proposalStatusMatches(status, filter) {
  const expected = normalizeProposalStatus(filter)
  if (!expected) return true
  if (isForReviewStatus(expected)) return isForReviewStatus(status)
  return normalizeProposalStatus(status) === expected
}

/**
 * The badge text for a stored proposal status. Both submission spellings read
 * "For RPS Review"; `APPROVED` reads "Implementation Approved" and `COMPLETED`
 * reads "Completed". Timeline-only codes retained for historical records fall back to
 * their timeline label, and anything else uses the generic Title Case formatter.
 */
export function proposalStatusLabel(status) {
  const value = String(status ?? '').trim()
  if (!value) return ''
  const key = normalizeProposalStatus(value).replace(/[\s-]+/g, '_')
  const canonicalKey = key === 'SUBMITTED' ? 'FOR_REVIEW' : key
  return (
    PROPOSAL_STATUS_LABELS[canonicalKey] ||
    PROPOSAL_TIMELINE_STATUS_LABELS[canonicalKey] ||
    statusLabel(value)
  )
}

/**
 * True when a proposal in the given status exposes the "Make a Copy" action.
 *
 * Always true for a real proposal: every status is copyable, and a proposal whose
 * status is missing or is one this module does not know yet must not lose the
 * action either. Only a value that cannot be a status at all (a number, an
 * object) is refused, so the details page keeps rendering the button.
 */
export function isDuplicable(status) {
  return status === null || status === undefined || typeof status === 'string'
}
