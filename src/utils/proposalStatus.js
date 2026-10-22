/**
 * proposalStatus.js
 * Shared proposal-status helpers for the proponent interface.
 */

/**
 * Statuses whose proposal content may be reused to create a new proposal.
 * Mirrors ProposalService.DUPLICABLE_STATUSES on the backend, which is the
 * authoritative check — this list only controls which rows show the
 * "Make a Copy" action.
 *
 * Only REJECTED proposals are copyable. Approval statuses (APPROVED,
 * IMPLEMENTATION, RELEASED, COMPLETED) and returned-for-revision statuses
 * (RPS_RETURNED, REVISION, REC_REVISION, RETURNED_TO_RPS) are intentionally
 * excluded: rejection is the only workflow outcome that means "rework this and
 * submit it again", so it is the only case that must be kept distinct.
 */
export const DUPLICABLE_STATUSES = ['REJECTED']

/** True when a proposal in the given status can be copied. */
export function isDuplicable(status) {
  return typeof status === 'string' && DUPLICABLE_STATUSES.includes(status.toUpperCase())
}
