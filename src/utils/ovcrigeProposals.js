/**
 * ovcrigeProposals.js
 * ---------------------------------------------------------------------------
 * The one place that defines which proposal statuses belong to the OVCRIGE module.
 *
 * A proposal reaches OVCRIGE when RPS endorses it (`ENDORSED`) and stays in the office's
 * pipeline while it is evaluated by REC (`UNDER_REVIEW`), recommended for approval
 * (`REC_APPROVED` / `OVC_APPROVED`), or forwarded on to OVCAF or the Office of the
 * Chancellor (`FOR_OVCAF_APPROVAL` / `FOR_OC_APPROVAL`). Proposals that have not been
 * endorsed yet (`FOR REVIEW` / `SUBMITTED`) and projects that have moved past the
 * workflow (`APPROVED`) are deliberately not part of it.
 *
 * The dashboard preview, the "Endorsed Proposals (from RPS)" list and the "Proposals for
 * OC Endorsement" list read these constants so they cannot drift apart.
 *
 * Run the rules with: npm test
 */
import { normalizeProposalStatus } from './proposalStatus.js'

/** Proposals that are, or have been, in OVCRIGE's hands - the dashboard preview set. */
export const OVCRIGE_PIPELINE_STATUSES = Object.freeze([
  'ENDORSED',
  'UNDER_REVIEW',
  'REC_APPROVED',
  'OVC_APPROVED',
  'FOR_OVCAF_APPROVAL',
  'FOR_OC_APPROVAL',
])

/**
 * What the dashboard requests in one call: the pipeline plus the two returned statuses
 * its "For Revision" statistic counts. The table still shows only the pipeline set.
 */
export const OVCRIGE_DASHBOARD_FETCH_STATUSES = Object.freeze([
  ...OVCRIGE_PIPELINE_STATUSES,
  'REVISION',
  'REC_REVISION',
])

/** REC-recommended proposals the office must forward to the Office of the Chancellor. */
export const OVCRIGE_OC_ENDORSEMENT_STATUSES = Object.freeze([
  'REC_APPROVED',
  'OVC_APPROVED',
  'FOR_OC_APPROVAL',
])

/** True when a proposal's stored status belongs to the OVCRIGE pipeline. */
export function isOvcrigePipelineProposal(proposal) {
  const status = normalizeProposalStatus(proposal?.status)
  return Boolean(status) && OVCRIGE_PIPELINE_STATUSES.includes(status)
}

/**
 * The proposal's submission date. `createdAt` is only a fallback for a record that
 * predates the submission timestamp; the last edit (`updatedAt`) is never used.
 */
export function proposalSubmittedAt(proposal) {
  return proposal?.submittedAt || proposal?.createdAt || null
}
