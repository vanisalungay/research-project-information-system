/**
 * proposalStatus.js
 * Shared proposal-status helpers for the proponent interface.
 *
 * A Proponent submission is stored as `FOR REVIEW` (uppercase, with a space). Proposals
 * saved before that wording change carry the legacy `SUBMITTED` value, which means the
 * exact same workflow state: every helper here treats the two as equivalent, so old and
 * new rows display, filter and count the same way.
 */
import { statusLabel } from './statusLabel.js'

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
 * spelling matches both, so selecting "For Review" also returns legacy `SUBMITTED` rows;
 * every other comparison is an exact normalized match (never a substring match).
 */
export function proposalStatusMatches(status, filter) {
  const expected = normalizeProposalStatus(filter)
  if (!expected) return true
  if (isForReviewStatus(expected)) return isForReviewStatus(status)
  return normalizeProposalStatus(status) === expected
}

/**
 * The badge text for a stored proposal status: both submission spellings read
 * "For Review"; every other status is formatted by the one shared formatter
 * (utils/statusLabel.js), so it is Title Case with no underscores everywhere.
 */
export function proposalStatusLabel(status) {
  const value = String(status ?? '').trim()
  if (!value) return ''
  return isForReviewStatus(value) ? 'For Review' : statusLabel(value)
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
