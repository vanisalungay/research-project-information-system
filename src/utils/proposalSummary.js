/**
 * proposalSummary.js
 * ---------------------------------------------------------------------------
 * The buckets the Proponent dashboard summarises proposals by.
 *
 * The buckets are built only from statuses RPIS actually stores - the vocabulary in
 * proposalStatus.js plus the stage keys the Proposal Monitoring timeline and the backend
 * services use - so the dashboard can never invent a status of its own, and a proposal
 * is never reported under a different status than the page that owns it.
 *
 * A proposal counts in exactly one bucket (Total counts everything), so the buckets can
 * be added up and compared with the records the backend returned.
 */

/** Statuses of a proposal that has not been submitted yet. */
const DRAFT_STATUSES = ['DRAFT']

/** Statuses of a proposal that has been submitted and is waiting for the first review. */
const SUBMITTED_STATUSES = ['SUBMITTED']

/**
 * Statuses of a proposal that is inside the review pipeline: endorsed by RPS, being
 * evaluated, awaiting a decision, or awaiting the release of its funds. The list is the
 * union of the statuses the backend writes (ProposalService, ProposalReviewService,
 * OvcafService) and the stage keys the Proposal Monitoring timeline shows.
 */
const UNDER_REVIEW_STATUSES = [
  'ENDORSED',
  'RPS_REVIEW',
  'UNDER_REVIEW',
  'REC_APPROVED',
  'REC_EVALUATION',
  'OVCRIGE_COORDINATION',
  'CHANCELLOR_REVIEW',
  'FOR_OC_APPROVAL',
  'FOR_OVCAF_APPROVAL',
  'OVC_APPROVED',
  'APPROVED_ENDORSED',
  'READY_FOR_RELEASE',
  'FINANCE_AUTHORIZATION',
]

/** Statuses of a proposal that has been approved (and, later, funded or implemented). */
const APPROVED_STATUSES = ['APPROVED', 'RELEASED', 'IMPLEMENTATION', 'COMPLETED']

/**
 * Statuses that put the proposal back in the Proponent's hands or end it: returned for
 * revision (with or without a deadline) and rejected. They are one bucket on purpose -
 * both mean "this one needs your attention or is over", and the exact status is shown on
 * My Projects.
 */
const RETURNED_STATUSES = [
  'RPS_RETURNED',
  'RETURNED',
  'RETURNED_TO_RPS',
  'RETURNED_FOR_REVISION',
  'REVISION',
  'PENDING_REVISION',
  'REC_REVISION',
  'REC_REJECTED',
  'REJECTED',
]

/**
 * The summary cards, in display order. `id` is the key of the returned counts, `accent` is
 * the dashboard's stat-card accent (see assets/dashboard.css) and `icon` is the key of the
 * icon the card shows - both declared here so the six cards can never drift apart.
 */
export const PROPOSAL_SUMMARY_BUCKETS = [
  { id: 'total', label: 'Total proposals', accent: 'indigo', icon: 'stack' },
  { id: 'draft', label: 'Draft', accent: 'blue', icon: 'draft' },
  { id: 'submitted', label: 'Submitted', accent: 'violet', icon: 'sent' },
  { id: 'underReview', label: 'Under review', accent: 'amber', icon: 'review' },
  { id: 'approved', label: 'Approved', accent: 'green', icon: 'approved' },
  { id: 'returned', label: 'Returned / Rejected', accent: 'red', icon: 'returned' },
]

/** The statuses of each bucket, by bucket id. */
const BUCKET_STATUSES = {
  draft: DRAFT_STATUSES,
  submitted: SUBMITTED_STATUSES,
  underReview: UNDER_REVIEW_STATUSES,
  approved: APPROVED_STATUSES,
  returned: RETURNED_STATUSES,
}

/** The stored status of a proposal, normalized for comparison ('' when unusable). */
export function normalizeProposalStatus(status) {
  return String(status ?? '')
    .trim()
    .toUpperCase()
}

/**
 * The bucket a stored status belongs to, or '' when it belongs to none. Total is not a
 * bucket a single status can be in - it is the count of the whole list.
 */
export function proposalBucketOf(status) {
  const value = normalizeProposalStatus(status)
  if (!value) return ''
  const entry = Object.entries(BUCKET_STATUSES).find(([, statuses]) => statuses.includes(value))
  return entry ? entry[0] : ''
}

/**
 * The counts the dashboard cards show for the given proposals. Every proposal is counted
 * once in `total`; a proposal whose status is not one of the known statuses still counts
 * in `total` and in no other bucket, so the numbers never claim more than the records.
 */
export function proposalSummaryCounts(proposals) {
  const list = Array.isArray(proposals) ? proposals : []
  const counts = { total: list.length, draft: 0, submitted: 0, underReview: 0, approved: 0, returned: 0 }
  for (const proposal of list) {
    const bucket = proposalBucketOf(proposal?.status)
    if (bucket) counts[bucket] += 1
  }
  return counts
}

/**
 * True when every status of the shared vocabulary is mapped to a bucket. Used by the
 * tests: a status added to proposalStatus.js without a bucket here would otherwise be
 * counted only in Total and quietly disappear from the summary.
 */
export function isEveryStatusBucketed(statuses) {
  return (Array.isArray(statuses) ? statuses : []).every((status) => proposalBucketOf(status) !== '')
}
