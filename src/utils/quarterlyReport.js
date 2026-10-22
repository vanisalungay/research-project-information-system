/**
 * quarterlyReport.js
 * Shared quarterly-reporting rules for the proponent interface.
 *
 * Reporting eligibility mirrors the backend's authoritative check
 * (QuarterlyProgressReportService.isEligible): a project must be approved for
 * implementation (APPROVED or RELEASED - the statuses that mean "approved for
 * implementation") AND have an issued Special Order (SO). The frontend only uses
 * this to decide which actions to offer; every quarterly report request is still
 * validated by the backend.
 */

/**
 * Proposal statuses that mean "approved for implementation". Mirrors
 * QuarterlyProgressReportService.IMPLEMENTATION_STATUSES on the backend.
 */
export const IMPLEMENTATION_STATUSES = ['APPROVED', 'RELEASED']

/** True when the project has an issued Special Order (SO) number. */
export function hasIssuedSpecialOrder(proposal) {
  const soNumber = proposal?.soNumber
  return typeof soNumber === 'string' && soNumber.trim() !== ''
}

/** True when the project's status is "approved for implementation". */
export function isApprovedForImplementation(proposal) {
  return IMPLEMENTATION_STATUSES.includes(proposal?.status)
}

/**
 * True when the project may create quarterly progress reports: approved for
 * implementation with an issued SO. Reporting stages are only available to
 * approved, SO-issued projects, so approval alone is never enough.
 */
export function isQuarterlyReportEligible(proposal) {
  return isApprovedForImplementation(proposal) && hasIssuedSpecialOrder(proposal)
}
