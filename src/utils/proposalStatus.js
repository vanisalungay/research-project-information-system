/**
 * proposalStatus.js
 * Shared proposal-status helpers for the proponent interface.
 */

/**
 * The statuses the proposal workflow reports. Kept in one place so the proponent
 * interface never has to hardcode a single status when it reasons about a
 * proposal (see the status filter of My Projects and the details page badge).
 */
export const PROPOSAL_STATUSES = [
  'DRAFT',
  'SUBMITTED',
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
