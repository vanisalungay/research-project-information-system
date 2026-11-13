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
import { STATUS_TONES, statusTone } from './statusTone.js'
import { proposalStatusLabel } from './proposalStatus.js'

/** Tones the design layer knows: the full approved palette (see statusCatalog.js). */
export const REVIEW_STATUS_TONES = STATUS_TONES

/**
 * The tone class for a saved status, from the one shared mapping (utils/statusTone.js) so a
 * status looks the same here as on every dashboard and workflow page.
 *
 * Every status has a colour now - an in-progress status reads purple/blue instead of the
 * badge's own fallback - so the header and the status card follow the approved palette.
 */
export function reviewStatusTone(status) {
  const value = String(status ?? '').trim()
  if (!value) return 'is-draft'
  return statusTone(value)
}

/**
 * The badge text: the saved status through the proposal-domain formatter
 * (utils/proposalStatus.js), so it uses the canonical proposal labels everywhere. Both
 * submission spellings (FOR REVIEW and the legacy SUBMITTED) read "For RPS Review",
 * APPROVED reads "Implementation Approved" and COMPLETED reads "Completed".
 */
export function reviewStatusLabel(status) {
  const value = String(status ?? '').trim()
  if (!value) return 'Draft'
  return proposalStatusLabel(value)
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