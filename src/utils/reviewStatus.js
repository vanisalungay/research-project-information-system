/**
 * reviewStatus.js
 * ---------------------------------------------------------------------------
 * The status wording and colour tone the proposal review / detailed-proposal pages show.
 *
 * The status itself is never invented here: it is the saved proposal status, only
 * translated into the label the header and the status card render and the tone class the
 * design layer styles. Keeping it in one place is what makes every role's page show the
 * same badge for the same workflow state.
 *
 * Verifiable without a browser - see reviewStatus.test.js.
 */

/** Tones the design layer knows: approved (green), rejected (red), returned (amber), draft (grey). */
export const REVIEW_STATUS_TONES = ['is-approved', 'is-rejected', 'is-returned', 'is-draft']

/**
 * The tone class for a saved status. A status that is still being processed (UNDER_REVIEW,
 * REC_EVALUATION, CHANCELLOR_REVIEW, ...) has no tone: it keeps the default lavender badge.
 */
export function reviewStatusTone(status) {
  const value = String(status ?? '').toUpperCase()
  if (!value) return 'is-draft'
  if (value.includes('APPROVED') || value.includes('ENDORSED') || value.includes('COMPLETED')) {
    return 'is-approved'
  }
  if (value.includes('REJECT')) return 'is-rejected'
  if (value.includes('RETURN') || value.includes('REVISION')) return 'is-returned'
  if (value.includes('DRAFT')) return 'is-draft'
  return ''
}

/** The badge text: the saved status, spelled the way the workflow spells it. */
export function reviewStatusLabel(status) {
  const value = String(status ?? '').trim()
  return value ? value.replace(/_/g, ' ') : 'DRAFT'
}

/**
 * A saved timestamp as the short label the summary cards show ("Jul 29, 2026"). An empty or
 * unusable value yields '' so the row is simply not rendered.
 */
export function reviewDateLabel(value) {
  if (value === null || value === undefined || value === '') return ''
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}