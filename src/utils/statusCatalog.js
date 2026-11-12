/**
 * statusCatalog.js
 * ---------------------------------------------------------------------------
 * The single canonical display-name configuration for every status category in
 * RPIS.
 *
 * Stored values never change: each map below is keyed by the normalized form of
 * the value the backend/API returns (see `normalizeStatusKey`), so filters,
 * comparisons, request payloads and database rows keep using the raw code. Only
 * the text rendered on screen comes from this file.
 *
 * The maps are deliberately category-specific. The same token can mean different
 * things in different modules - `APPROVED` on a proposal, an account and a
 * decision; `ACTIVE` on a Dean assignment, a remark and an announcement - and a
 * global map cannot represent that without showing one category's wording in
 * another. Decision values, event types and notification categories are *not*
 * record statuses and must not be looked up as proposal statuses.
 *
 * `statusLabel` in statusLabel.js remains the generic Title Case fallback for a
 * code that has no category map entry. Do not use it for a known domain status.
 *
 * Run the rules with: npm test
 */

/** Words kept lowercase inside a generated label, except when first. */
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

/** Institutional acronyms that stay uppercase inside a generated label. */
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
 * The comparison key of a stored value: trimmed, whitespace/hyphens collapsed to
 * underscores and uppercased, so `FOR REVIEW`, `FOR_REVIEW` and `for-review` all
 * resolve to `FOR_REVIEW`. Returns '' for a missing or unusable value.
 */
export function normalizeStatusKey(value) {
  if (value === null || value === undefined) return ''
  if (typeof value === 'object' || typeof value === 'function') return ''
  const text = String(value).trim()
  if (!text) return ''
  return text.replace(/[\s-]+/g, '_').replace(/_+/g, '_').toUpperCase()
}

/**
 * Look up a display label in one category map. Returns `fallback` ('' by
 * default) when the value is empty or the map has no entry.
 */
export function labelFrom(labels, value, fallback = '') {
  const key = normalizeStatusKey(value)
  if (!key) return ''
  return labels[key] ?? fallback
}

/**
 * Canonical proposal status labels. `SUBMITTED` is the legacy spelling of
 * `FOR REVIEW`; both are the same workflow state and both read "For RPS Review".
 *
 * `ENDORSED` is the RPS → OVCRIGE forwarding stage ("RPS endorsed it for OVCRIGE
 * coordination"), NOT the Dean's endorsement. The Dean endorses an endorsement
 * request before the proposal is submitted; that stage is tracked by
 * ENDORSEMENT_STATUS_LABELS (PENDING/APPROVED/REJECTED), not by a proposal status.
 * `OVC_APPROVED` is OVCRIGE's own approval, a later stage.
 *
 * `RETURNED_TO_RPS` and `RPS_RETURNED` are NOT aliases: they are opposite return
 * directions and must keep different labels.
 *   - RETURNED_TO_RPS  a reviewing office returned the proposal to RPS (RPS acts next)
 *   - RPS_RETURNED     RPS returned / forwarded the proposal to the Proponent for revision
 * Both stay in the returned orange family.
 *
 * APPROVED (approved for implementation) and COMPLETED (finished reporting) are
 * separate states and must never share a label.
 */
export const PROPOSAL_STATUS_LABELS = Object.freeze({
  DRAFT: 'Draft',
  FOR_REVIEW: 'For RPS Review',
  SUBMITTED: 'For RPS Review',
  ENDORSED: 'Endorsed to OVCRIGE',
  UNDER_REVIEW: 'Under Review by REC',
  REC_APPROVED: 'Approved by REC',
  OVC_APPROVED: 'Approved by OVCRIGE',
  FOR_OC_APPROVAL: 'For OC Approval',
  FOR_OVCAF_APPROVAL: 'For OVCAF Approval',
  APPROVED: 'Implementation Approved',
  READY_FOR_RELEASE: 'Ready for Fund Release',
  RELEASED: 'Released',
  RETURNED_TO_RPS: 'Returned to RPS',
  RPS_RETURNED: 'Returned to Proponent',
  RETURNED: 'Returned for Revision',
  REJECTED: 'Rejected',
  COMPLETED: 'Completed',
})

/**
 * Timeline-only / declared-but-unused proposal codes retained for historical
 * compatibility. They are not written by the current backend, so a current
 * status badge must not present them as an ordinary stored status. Kept in a
 * separate map precisely so they cannot be confused with the canonical set.
 */
export const PROPOSAL_TIMELINE_STATUS_LABELS = Object.freeze({
  RPS_REVIEW: 'RPS Review',
  OVCRIGE_COORDINATION: 'OVCRIGE Coordination',
  REC_EVALUATION: 'REC Evaluation',
  CHANCELLOR_REVIEW: 'Chancellor Review',
  FINANCE_AUTHORIZATION: 'Finance Authorization',
  IMPLEMENTATION: 'Implementation',
  FOR_IMPLEMENTATION: 'For Implementation',
  REC_REJECTED: 'REC Rejected',
  REVISION: 'Revision',
  REC_REVISION: 'REC Revision',
  PENDING_REVISION: 'Pending Revision',
  PENDING: 'Pending',
})

/** Quarterly Progress Report and Terminal Report record statuses. */
export const REPORT_STATUS_LABELS = Object.freeze({
  DRAFT: 'Draft',
  SUBMITTED: 'Submitted',
})

/** User-account access statuses. APPROVED means the account is active (green). */
export const ACCOUNT_STATUS_LABELS = Object.freeze({
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  INACTIVE: 'Inactive',
})

/** Dean endorsement-request statuses. */
export const ENDORSEMENT_STATUS_LABELS = Object.freeze({
  PENDING: 'Pending',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
})

/** Issued certification-document statuses. */
export const CERTIFICATION_STATUS_LABELS = Object.freeze({
  ISSUED: 'Issued',
  SUPERSEDED: 'Superseded',
})

/** Dean assignment statuses. "All" is a filter, not a status. */
export const DEAN_ASSIGNMENT_STATUS_LABELS = Object.freeze({
  ACTIVE: 'Active',
  REVOKED: 'Revoked',
})

/**
 * The derived, proponent-visible announcement status. Stored `ACTIVE` is never
 * shown as "Active": it is Scheduled or Open depending on the dates.
 */
export const ANNOUNCEMENT_STATUS_LABELS = Object.freeze({
  DRAFT: 'Draft',
  SCHEDULED: 'Scheduled',
  OPEN: 'Open',
  CLOSED: 'Closed',
})

/** Priority agendas are derived from a Boolean, not a stored status enum. */
export const PRIORITY_AGENDA_STATUS_LABELS = Object.freeze({
  ACTIVE: 'Active',
  INACTIVE: 'Inactive',
})

/**
 * OVCAF evaluator decisions. These are decision values, not proposal statuses.
 * `APPROVED_ENDORSED` reads "Approved" here; the proposal it produces reads
 * "Implementation Approved".
 */
export const OVCAF_DECISION_LABELS = Object.freeze({
  APPROVED_ENDORSED: 'Budget Approved',
  RETURNED_FOR_REVISION: 'Returned for Revision',
  REJECTED: 'Rejected',
})

/** Reviewer decisions. Kept separate from the proposal status they produce. */
export const REVIEW_DECISION_LABELS = Object.freeze({
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  REVISION_REQUIRED: 'Revision Required',
  REVISION: 'For Revision',
})

/** Reviewer field-remark rounding statuses (internal). */
export const SECTION_REMARK_STATUS_LABELS = Object.freeze({
  ACTIVE: 'Active',
  SUPERSEDED: 'Superseded',
})

/** Activity-log execution results, not workflow statuses. */
export const ACTIVITY_STATUS_LABELS = Object.freeze({
  SUCCESS: 'Success',
  FAILED: 'Failed',
})

/** Account status label. */
export function accountStatusLabel(value, fallback = '') {
  return labelFrom(ACCOUNT_STATUS_LABELS, value, fallback)
}

/** Quarterly Progress Report / Terminal Report status label. */
export function reportStatusLabel(value, fallback = '') {
  return labelFrom(REPORT_STATUS_LABELS, value, fallback)
}

/** Certification-document status label. */
export function certificationStatusLabel(value, fallback = '') {
  return labelFrom(CERTIFICATION_STATUS_LABELS, value, fallback)
}

/** Dean assignment status label. */
export function deanAssignmentStatusLabel(value, fallback = '') {
  return labelFrom(DEAN_ASSIGNMENT_STATUS_LABELS, value, fallback)
}

/** Priority-agenda status label derived from the Boolean `active` flag. */
export function priorityAgendaStatusLabel(active, fallback = '') {
  return labelFrom(PRIORITY_AGENDA_STATUS_LABELS, active ? 'ACTIVE' : 'INACTIVE', fallback)
}

/** OVCAF decision label (never a proposal status label). */
export function ovcafDecisionLabel(value, fallback = '') {
  return labelFrom(OVCAF_DECISION_LABELS, value, fallback)
}

/** Reviewer decision label (never a proposal status label). */
export function reviewDecisionLabel(value, fallback = '') {
  return labelFrom(REVIEW_DECISION_LABELS, value, fallback)
}

/** Section-remark status label. */
export function sectionRemarkStatusLabel(value, fallback = '') {
  return labelFrom(SECTION_REMARK_STATUS_LABELS, value, fallback)
}

/** Activity execution-result label. */
export function activityStatusLabel(value, fallback = '') {
  return labelFrom(ACTIVITY_STATUS_LABELS, value, fallback)
}

// ===========================================================================
// Colour system
// ---------------------------------------------------------------------------
// One approved semantic palette for every status in RPIS. A status is resolved
// to a *variant* (the meaning of its colour), and the variant is what the
// stylesheet paints (`assets/status.css`, tokens in `assets/css/base.css`).
// The colours below are the design reference; the CSS keeps a light badge
// background with darker text in the same hue, so contrast stays readable.
//
// The same stored value resolves to the same variant on every page and for
// every role: no component owns a status colour of its own. Unknown codes are
// deliberately gray - never green - so an unrecognised value can never look
// approved.
// ===========================================================================

/** The approved semantic colour palette. */
export const STATUS_PALETTE = Object.freeze({
  gray: '#64748B', // draft, inactive, revoked, superseded, closed
  blue: '#2563EB', // submitted / for review / scheduled
  purple: '#7C3AED', // under review
  indigo: '#4F46E5', // REC approval
  cyan: '#0891B2', // OVCRIGE family: RPS endorsement to OVCRIGE and OVCRIGE approval
  teal: '#0F766E', // retained in the palette; no current status maps to it
  released: '#0D9488', // funds released (a distinct teal from endorsement)
  amber: '#D97706', // pending and awaiting approval
  orange: '#EA580C', // ready for fund release and every returned status
  approved: '#15803D', // approved / Implementation Approved
  completed: '#166534', // completed (a darker green than approved)
  rejected: '#DC2626', // rejected and failed
})

/**
 * The tone class each variant is painted with. These are the class names
 * `assets/status.css` styles; the geometry (padding, radius, font) stays with
 * the badge class of the page.
 */
export const STATUS_VARIANT_CLASSES = Object.freeze({
  gray: 'is-gray',
  blue: 'is-blue',
  purple: 'is-purple',
  indigo: 'is-indigo',
  cyan: 'is-cyan',
  teal: 'is-teal',
  released: 'is-released',
  amber: 'is-amber',
  orange: 'is-orange',
  approved: 'is-approved',
  completed: 'is-completed',
  rejected: 'is-rejected',
})

/**
 * The canonical variant of every status the application stores or displays,
 * across all categories. Category maps above own the *wording*; this map owns
 * the *colour*, and because the same token means the same thing everywhere the
 * two never disagree.
 */
export const STATUS_COLOR_VARIANTS = Object.freeze({
  // Proposal workflow
  DRAFT: 'gray',
  FOR_REVIEW: 'blue',
  SUBMITTED: 'blue',
  // RPS forwards the proposal to OVCRIGE; cyan is the OVCRIGE office colour (the same
  // family as OVC_APPROVED). It is a forwarding stage, not the Dean's endorsement.
  ENDORSED: 'cyan',
  UNDER_REVIEW: 'purple',
  REC_APPROVED: 'indigo',
  OVC_APPROVED: 'cyan',
  FOR_OC_APPROVAL: 'amber',
  FOR_OVCAF_APPROVAL: 'amber',
  READY_FOR_RELEASE: 'orange',
  RELEASED: 'released',
  APPROVED: 'approved',
  COMPLETED: 'completed',
  RETURNED_TO_RPS: 'orange',
  RPS_RETURNED: 'orange',
  RETURNED: 'orange',
  REJECTED: 'rejected',

  // Decisions, accounts, endorsements, assignments, calls, certifications
  APPROVED_ENDORSED: 'approved',
  RETURNED_FOR_REVISION: 'orange',
  REVISION_REQUIRED: 'orange',
  REVISION: 'orange',
  PENDING: 'amber',
  ACTIVE: 'approved',
  INACTIVE: 'gray',
  REVOKED: 'gray',
  CLOSED: 'gray',
  OPEN: 'approved',
  SCHEDULED: 'blue',
  ISSUED: 'approved',
  SUPERSEDED: 'gray',
  SUCCESS: 'approved',
  FAILED: 'rejected',

  // Timeline / prose milestones (stage names and the monitoring timeline)
  RPS_REVIEW: 'blue',
  OVCRIGE_COORDINATION: 'cyan',
  REC_EVALUATION: 'purple',
  CHANCELLOR_REVIEW: 'purple',
  // Awaiting the finance office's budget authorization: a pending decision, amber.
  FINANCE_AUTHORIZATION: 'amber',
  IMPLEMENTATION: 'approved',
  // The implementation stage activated after funds release ("Project activated following
  // funds release"); the same green as IMPLEMENTATION. Without this entry the wording
  // would fall through to the gray default, which would read as inactive.
  FOR_IMPLEMENTATION: 'approved',
  FUNDS_RELEASED: 'released',
  REC_REJECTED: 'rejected',
  REC_REVISION: 'orange',
  PENDING_REVISION: 'orange',
  WAITING: 'amber',
  ONGOING: 'blue',
})

/**
 * The colour variant of a status. Exact codes win; a status this module has not
 * seen yet is classified by its wording so a future RETURNED_BY_* or
 * *_APPROVED value still lands in the right family. Anything unrecognised is
 * gray: a safe fallback that never reads as approved or completed.
 */
export function statusVariant(value) {
  const key = normalizeStatusKey(value)
  if (!key) return 'gray'
  if (STATUS_COLOR_VARIANTS[key]) return STATUS_COLOR_VARIANTS[key]

  if (key.includes('REJECT') || key.includes('FAILED') || key.includes('ERROR')) return 'rejected'
  if (key.includes('RETURN') || key.includes('REVISION') || key.includes('REVOK')) return 'orange'
  if (key.includes('DRAFT')) return 'gray'
  if (key.includes('COMPLETED')) return 'completed'
  if (key.includes('APPROVAL') || key.includes('PENDING') || key.includes('WAITING')) return 'amber'
  if (
    key.includes('APPROVED') ||
    key.includes('ENDORSED') ||
    key.includes('RELEASED') ||
    key.includes('ISSUED') ||
    key.includes('SUCCESS') ||
    key === 'ACTIVE' ||
    key === 'OPEN'
  ) {
    return key.includes('RELEASED') ? 'released' : 'approved'
  }
  if (
    key.includes('REVIEW') ||
    key.includes('EVALUATION') ||
    key.includes('SCHEDULED') ||
    key.includes('SUBMITTED') ||
    key.includes('INFO') ||
    key.includes('VALIDATION') ||
    key.includes('COORDINATION') ||
    key.includes('ONGOING')
  ) {
    return key.includes('EVALUATION') ? 'purple' : 'blue'
  }
  return 'gray'
}

/** The tone class of a status (e.g. `is-approved`), from the approved palette. */
export function statusVariantClass(value) {
  return STATUS_VARIANT_CLASSES[statusVariant(value)]
}

/** The approved hex colour of a status (e.g. `#15803D`). */
export function statusColor(value) {
  return STATUS_PALETTE[statusVariant(value)]
}

/**
 * Title-case fallback for a code with no category entry. Mirrors the generic
 * formatter's output without importing it, so this module stays dependency-free.
 */
export function genericStatusLabel(value) {
  const key = normalizeStatusKey(value)
  if (!key) return ''
  const acronyms = new Set(STATUS_ACRONYMS)
  const capitalize = (token, index) => {
    if (!token) return ''
    if (acronyms.has(token)) return token
    if (/^\d+$/.test(token)) return token
    const lower = token.toLowerCase()
    if (index > 0 && MINOR_WORDS.has(lower)) return lower
    return lower.charAt(0).toUpperCase() + lower.slice(1)
  }
  return key
    .split('_')
    .map((token, index) =>
      token
        .split('/')
        .map((segment) => capitalize(segment, index))
        .join('/'),
    )
    .join(' ')
}
