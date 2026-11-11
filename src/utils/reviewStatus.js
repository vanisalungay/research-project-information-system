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
import { statusTone } from './statusTone.js'
import { isForReviewStatus } from './proposalStatus.js'
import { statusLabel } from './statusLabel.js'

/** Tones the design layer knows: approved (green), rejected (red), returned (amber), draft (grey). */
export const REVIEW_STATUS_TONES = ['is-approved', 'is-rejected', 'is-returned', 'is-draft']

/**
 * The tone class for a saved status, from the one shared mapping (utils/statusTone.js) so a
 * status looks the same here as on every dashboard and workflow page.
 *
 * A status that is still being processed (UNDER_REVIEW, REC_EVALUATION, CHANCELLOR_REVIEW, ...)
 * has no tone here: it keeps the review badge's own pending treatment rather than a tone class.
 */
export function reviewStatusTone(status) {
  const value = String(status ?? '').toUpperCase()
  if (!value) return 'is-draft'
  const tone = statusTone(value)
  return tone === 'is-pending' || tone === 'is-neutral' ? '' : tone
}

/**
 * The badge text: the saved status through the one shared formatter
 * (utils/statusLabel.js), so it is Title Case with no underscores. Both
 * submission spellings (FOR REVIEW and the legacy SUBMITTED) read "For Review".
 */
export function reviewStatusLabel(status) {
  const value = String(status ?? '').trim()
  if (!value) return 'Draft'
  if (isForReviewStatus(value)) return 'For Review'
  return statusLabel(value)
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