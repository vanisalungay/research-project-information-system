/**
 * proposalStatus.js
 * Shared proposal-status helpers for the proponent interface.
 */

/**
 * Statuses whose proposal content may be reused to create a new proposal.
 * Mirrors ProposalService.DUPLICABLE_STATUSES on the backend, which is the
 * authoritative check — this list only controls which rows show the
 * "Duplicate" action.
 */
export const DUPLICABLE_STATUSES = ['APPROVED', 'IMPLEMENTATION', 'RELEASED', 'COMPLETED']

/** True when a proposal in the given status can be duplicated. */
export function isDuplicable(status) {
  return typeof status === 'string' && DUPLICABLE_STATUSES.includes(status.toUpperCase())
}
