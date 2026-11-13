/**
 * quarterlyReport.js
 * Shared quarterly-reporting rules for the proponent interface.
 *
 * Reporting eligibility mirrors the backend's authoritative check
 * (QuarterlyProgressReportService.isEligible): a project must be approved for
 * implementation (APPROVED, RELEASED or IMPLEMENTATION - see
 * IMPLEMENTATION_STATUSES) AND carry the Office of the Chancellor's recorded
 * Special Order (SO number + recorded time). The backend additionally requires the
 * persisted OVCAF budget approval when the Chancellor routed the proposal through
 * budget evaluation. The frontend only uses this to decide which actions to offer;
 * every quarterly report request is still validated by the backend.
 */

export {
  REPORT_STATUS_LABELS,
  reportStatusLabel,
} from './statusCatalog.js'

/**
 * Proposal statuses that mean "approved for implementation". Mirrors
 * QuarterlyProgressReportService.IMPLEMENTATION_STATUSES on the backend, including
 * the implementation-phase status the rest of the proponent interface already
 * treats as approved (proposalStatus.js / proposalSummary.js).
 */
export const IMPLEMENTATION_STATUSES = ['APPROVED', 'RELEASED', 'IMPLEMENTATION']

/**
 * Statuses whose reports remain available on the reporting pages. It adds the completed
 * status to IMPLEMENTATION_STATUSES so a project does not lose access to the reports it
 * already filed once its final required report moves it to COMPLETED. This never grants
 * the right to create a new report; that stays with isQuarterlyReportEligible() below.
 */
export const REPORTABLE_STATUSES = [...IMPLEMENTATION_STATUSES, 'COMPLETED']

/** True when the project has an issued Special Order (SO) number. */
export function hasIssuedSpecialOrder(proposal) {
  const soNumber = proposal?.soNumber
  return typeof soNumber === 'string' && soNumber.trim() !== ''
}

/**
 * True when the Office of the Chancellor's SO record was actually written: the SO
 * number and the time it was recorded are set together by the OC final-approval
 * transaction. A number typed straight into the record is not OC approval evidence.
 * The timestamp accepts both an ISO string and the array form some serializers use.
 */
export function hasRecordedOcApproval(proposal) {
  if (!hasIssuedSpecialOrder(proposal)) return false
  const recordedAt = proposal?.soUploadedAt
  if (Array.isArray(recordedAt)) return recordedAt.length > 0
  return typeof recordedAt === 'string' && recordedAt.trim() !== ''
}

/** True when the project's status is "approved for implementation". */
export function isApprovedForImplementation(proposal) {
  const status = typeof proposal?.status === 'string' ? proposal.status.trim().toUpperCase() : ''
  return IMPLEMENTATION_STATUSES.includes(status)
}

/**
 * True when the project's filed reports may still be read and its next reporting stage
 * resolved. The approval-chain evidence is the same as for reporting eligibility; a
 * COMPLETED project is accepted because it already passed that chain and keeps access
 * to the reports it submitted. Creating a new report still requires
 * {@link isQuarterlyReportEligible}.
 */
export function isReportableProject(proposal) {
  const status = typeof proposal?.status === 'string' ? proposal.status.trim().toUpperCase() : ''
  return REPORTABLE_STATUSES.includes(status) && hasRecordedOcApproval(proposal)
}

/**
 * True when the project may create quarterly progress reports: approved for
 * implementation with a recorded Office of the Chancellor SO. Approval alone is
 * never enough, and a status containing "APPROVED" is not treated as the approval
 * record. When the Chancellor required budget evaluation, the backend additionally
 * requires the persisted OVCAF approval; the proposal list does not carry that
 * record, so the backend remains authoritative for that part of the chain.
 */
export function isQuarterlyReportEligible(proposal) {
  return isApprovedForImplementation(proposal) && hasRecordedOcApproval(proposal)
}

/**
 * The route of the existing quarterly-report form for a project's next required
 * stage. `stageInfo` is the response of
 * {@code GET /api/quarterly-reports/next-stage/{proposalId}}.
 *
 * An unfinished draft is continued (its id is in the stage response) instead of
 * starting a second report, and a project whose reporting is already complete has
 * no route to open - the caller reports that instead of showing an empty form.
 *
 * @param {number|string} proposalId the project's proposal id
 * @param {{allStagesComplete?: boolean, draftReportId?: number}} [stageInfo]
 * @returns {string} the form's route, or '' when nothing may be opened
 */
export function nextQuarterlyReportPath(proposalId, stageInfo) {
  if (!proposalId || !stageInfo || stageInfo.allStagesComplete) return ''
  return stageInfo.draftReportId
    ? `/quarterly-reports/${proposalId}/edit/${stageInfo.draftReportId}`
    : `/quarterly-reports/${proposalId}/new`
}
