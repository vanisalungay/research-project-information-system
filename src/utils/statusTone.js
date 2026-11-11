/**
 * statusTone.js
 * ---------------------------------------------------------------------------
 * The one place a workflow status is translated into a colour tone.
 *
 * Every role and module renders statuses through this helper, so the same saved status can
 * never be green on one page and amber on another. The tone names are the ones the shared
 * stylesheet (assets/status.css) styles; the actual colours live in the design tokens in
 * assets/css/base.css.
 *
 * The rules match on substrings, so a status the backend adds later (for example a new
 * RETURNED_BY_* value) is covered without touching this file. No status value is renamed or
 * invented here: a value that matches nothing falls back to the neutral tone.
 *
 * Verifiable without a browser - see statusTone.test.js.
 */

/** Every tone the shared stylesheet styles. */
export const STATUS_TONES = [
  'is-approved',
  'is-returned',
  'is-rejected',
  'is-pending',
  'is-draft',
  'is-neutral',
]

/** The saved status as the uppercase text the rules compare against ('' when there is none). */
export function normalizeStatus(status) {
  return String(status ?? '')
    .trim()
    .toUpperCase()
}

/**
 * The phrases that mean a status is finished successfully. `FOR REVIEW` is included on
 * purpose: it is the stored status of a completed Proponent submission (the same workflow
 * state as the legacy `SUBMITTED`), so the submission milestone keeps its existing colour.
 */
const APPROVED_MARKERS = [
  'APPROVED',
  'ENDORSED',
  'COMPLETED',
  'FOR REVIEW',
  'SUBMITTED',
  'RELEASED',
  'SUCCESS',
  'OPEN',
]

/** The phrases that mean a status is still moving through the workflow. */
const PENDING_MARKERS = [
  'PENDING',
  'REVIEW',
  'APPROVAL',
  'EVALUATION',
  'COORDINATION',
  'AUTHORIZATION',
  'VALIDATION',
  'RELEASE',
  'IMPLEMENTATION',
  'ONGOING',
  'WAITING',
  'PROGRESS',
  'SCHEDULED',
  'INFO',
]

/** True when the uppercase status contains any of the given phrases. */
function includesAny(value, markers) {
  return markers.some((marker) => value.includes(marker))
}

/**
 * The tone class of a saved status. Never returns '' - an empty, inactive or unknown value
 * is neutral, so a badge always has a defined look.
 *
 * The order matters: REJECTED is red even though it also contains the word "REJECT" inside
 * a longer workflow value, and RETURNED_FOR_REVISION is amber even though it is "pending"
 * in the sense of still being worked on.
 */
export function statusTone(status) {
  const value = normalizeStatus(status)
  if (!value) return 'is-neutral'

  // A plain active/inactive flag (agenda sets, Dean assignments, announcements).
  if (value === 'ACTIVE') return 'is-approved'
  if (value === 'INACTIVE') return 'is-neutral'

  // A rejection, failure or closed item is red.
  if (value.includes('REJECT') || includesAny(value, ['FAILED', 'ERROR', 'CLOSED'])) {
    return 'is-rejected'
  }

  // Every return / revision / revocation is amber.
  if (value.includes('RETURN') || includesAny(value, ['REVISION', 'REVOKED', 'WARNING'])) {
    return 'is-returned'
  }

  // A draft is not yet in the workflow: grey.
  if (value.includes('DRAFT')) return 'is-draft'

  // Finished successfully: green. "APPROVED" (not "APPROV") so FOR_OC_APPROVAL stays pending.
  if (includesAny(value, APPROVED_MARKERS)) return 'is-approved'

  // Still moving through a review / approval / implementation stage: light blue.
  if (includesAny(value, PENDING_MARKERS)) return 'is-pending'

  // INACTIVE and anything unrecognised: neutral.
  return 'is-neutral'
}
