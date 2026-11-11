/**
 * statusLabel.js
 * ---------------------------------------------------------------------------
 * The one place that turns a stored status code into the text every role sees.
 *
 * Backend statuses are stored as codes (`UNDER_REVIEW`, `FOR OVCAF_APPROVAL`,
 * `RPS_RETURNED`, ...). They are never changed here: the formatter only maps a
 * code to its display label, so comparisons, filters and API payloads keep using
 * the raw value. Every screen must render through this module (or a thin domain
 * wrapper such as `proposalStatusLabel`) instead of formatting statuses itself.
 *
 * A status is displayed in Title Case with no underscores. Institutional
 * acronyms stay uppercase (REC, OVCAF, RPS, OC, OVC, OVCRIGE). A known value is
 * read from the explicit label map; an unknown value falls back to a safe
 * title-case translation, so a future status can never leak `SOME_STATUS` to a
 * user.
 *
 * Run the rules with: npm test
 */

/** Acronyms that stay uppercase inside a status label. */
export const STATUS_ACRONYMS = Object.freeze([
  'RPS',
  'REC',
  'OVC',
  'OVCAF',
  'OC',
  'OVCRIGE',
  'ORPS',
  'GAD',
])

/**
 * The approved display label of every status the system knows, keyed by the
 * normalized form (see {@link normalizeStatusKey}). `SUBMITTED` is the generic
 * spelling; the proposal workflow deliberately shows it as "For Review" through
 * `proposalStatusLabel` because it is the same workflow state as `FOR REVIEW`.
 */
export const KNOWN_STATUS_LABELS = Object.freeze({
  DRAFT: 'Draft',
  FOR_REVIEW: 'For Review',
  SUBMITTED: 'Submitted',
  ENDORSED: 'Endorsed',
  UNDER_REVIEW: 'Under Review',
  REC_APPROVED: 'REC Approved',
  OVC_APPROVED: 'OVC Approved',
  FOR_OVCAF_APPROVAL: 'For OVCAF Approval',
  FOR_OC_APPROVAL: 'For OC Approval',
  APPROVED: 'Approved',
  READY_FOR_RELEASE: 'Ready for Release',
  RELEASED: 'Released',
  RETURNED: 'Returned',
  RETURNED_TO_RPS: 'Returned to RPS',
  RPS_RETURNED: 'RPS Returned',
  REVISION: 'Revision',
  REC_REVISION: 'REC Revision',
  PENDING_REVISION: 'Pending Revision',
  REC_REJECTED: 'REC Rejected',
  REJECTED: 'Rejected',
  IMPLEMENTATION: 'Implementation',
  COMPLETED: 'Completed',
  RPS_REVIEW: 'RPS Review',
  OVCRIGE_COORDINATION: 'OVCRIGE Coordination',
  REC_EVALUATION: 'REC Evaluation',
  CHANCELLOR_REVIEW: 'Chancellor Review',
  FINANCE_AUTHORIZATION: 'Finance Authorization',
  RETURNED_FOR_REVISION: 'Returned for Revision',
  REVISION_REQUIRED: 'Revision Required',
  APPROVED_ENDORSED: 'Approved & Endorsed',
  PENDING: 'Pending',
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
  REVOKED: 'Revoked',
  ISSUED: 'Issued',
  SUPERSEDED: 'Superseded',
  SUCCESS: 'Success',
  FAILED: 'Failed',
  SCHEDULED: 'Scheduled',
  OPEN: 'Open',
  CLOSED: 'Closed',
})

/** Words kept lowercase inside an unknown status label, except when first. */
const MINOR_WORDS = new Set([
  'to',
  'for',
  'of',
  'and',
  'by',
  'in',
  'on',
  'with',
  'the',
  'a',
  'an',
  'from',
])

const ACRONYM_SET = new Set(STATUS_ACRONYMS)

/**
 * The comparison key of a stored status: trimmed, whitespace/hyphens collapsed
 * to underscores and uppercased, so `FOR REVIEW`, `FOR_REVIEW` and `for-review`
 * all resolve to `FOR_REVIEW`. Returns '' for a missing or unusable value.
 */
export function normalizeStatusKey(value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object' || typeof value === 'function') return ''
  const text = String(value).trim()
  if (!text) return ''
  return text.replace(/[\s-]+/g, '_').replace(/_+/g, '_').toUpperCase()
}

/** Capitalize one slash-free token of an unknown status. */
function capitalizeToken(token, index) {
  if (!token) return ''
  if (ACRONYM_SET.has(token)) return token
  if (/^\d+$/.test(token)) return token
  const lower = token.toLowerCase()
  if (index > 0 && MINOR_WORDS.has(lower)) return lower
  return lower.charAt(0).toUpperCase() + lower.slice(1)
}

/** Capitalize a token that may contain slash-separated segments (`N/A`). */
function capitalizeSegments(token, index) {
  return token
    .split('/')
    .map((segment) => capitalizeToken(segment, index))
    .join('/')
}

/**
 * The display label of a status. Known codes read from the label map; an
 * unknown code falls back to Title Case with `_` replaced by a space.
 * Empty, null and unusable values return ''.
 */
export function statusLabel(value) {
  const key = normalizeStatusKey(value)
  if (!key) return ''
  const known = KNOWN_STATUS_LABELS[key]
  if (known) return known
  return key
    .split('_')
    .map((token, index) => capitalizeSegments(token, index))
    .join(' ')
}
