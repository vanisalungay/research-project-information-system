/**
 * callForProposal.js
 * ---------------------------------------------------------------------------
 * Presentation rules of the "Call for Proposal" page - step 1 of the Create
 * Proposal workflow (ProponentCallForProposal.vue).
 *
 * The page lists the Calls for Proposal a proponent can see: the ones that are open
 * for submissions right now and the published ones that open later (scheduled). Both
 * come from the backend (GET /api/proposal-announcements/available), which returns
 * only stored status ACTIVE calls that have not passed their deadline - using the
 * status/date rule the whole system already uses: ACTIVE with today inside
 * [startDate, endDate] is Open, ACTIVE with a start date still ahead is Scheduled.
 *
 * Nothing is invented here - the page only formats the real saved values and applies
 * the same rule again before a call is selected, so an expired or not-yet-open call
 * can never be picked. A scheduled call is shown with a disabled button, and the
 * backend re-validates the call on submission (ProposalService.requireOpenAnnouncement).
 *
 * Run the rules with: npm test
 */
import { announcementStatus, localTodayISO } from './announcementStatus.js'

/** Endpoint that returns the open and scheduled Calls for Proposal. */
export const AVAILABLE_CALLS_ENDPOINT = '/api/proposal-announcements/available'

/** Page heading of step 1. */
export const CALL_FOR_PROPOSAL_TITLE = 'Call for Proposal'

/** Instruction shown under the heading. */
export const CALL_FOR_PROPOSAL_INSTRUCTION =
  'Select an open call for proposal that you want to respond to. Review its details and deadline before creating your proposal.'

/** Action of every call card. */
export const CALL_SELECTION_BUTTON_LABEL = 'Apply to This Call'

/** Shown when no call is open or upcoming. */
export const NO_AVAILABLE_CALLS_MESSAGE =
  'There are currently no open or upcoming calls for proposals. Please check again later.'

/** Label of the deadline shown on a card. */
export const CALL_DEADLINE_LABEL = 'Deadline'

/** Label of the submission period shown on a card. */
export const CALL_WINDOW_LABEL = 'Submission period'

/** Label of the opening date shown on a scheduled card. */
export const CALL_OPENING_LABEL = 'Opening date'

/** The status a scheduled call shows in its badge. */
export const CALL_STATUS_SCHEDULED = 'Scheduled'

/**
 * An ISO date (YYYY-MM-DD, the format the API returns) as a readable date
 * ("March 4, 2026"). Unusable values are reported as an em dash instead of
 * "Invalid Date", so a card never shows a broken deadline.
 */
export function formatCallDate(iso) {
  if (!iso) return '—'
  const parsed = new Date(`${iso}T00:00:00`)
  if (Number.isNaN(parsed.getTime())) return '—'
  return parsed.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
}

/** The saved submission window of a call, e.g. "February 2, 2026 – March 4, 2026". */
export function callWindowLabel(call) {
  const start = formatCallDate(call?.startDate)
  const end = formatCallDate(call?.endDate)
  return `${start} – ${end}`
}

/** The saved deadline of a call, ready for display. */
export function callDeadlineLabel(call) {
  return formatCallDate(call?.endDate)
}

/** The saved opening date of a scheduled call, ready for display. */
export function callOpeningLabel(call) {
  return formatCallDate(call?.startDate)
}

/**
 * Why a scheduled call cannot be applied to yet, with its real opening date:
 * "Applications will open on November 2, 2026."
 */
export function applicationsOpenOnLabel(call) {
  return `Applications will open on ${formatCallDate(call?.startDate)}.`
}

/**
 * True when a call is genuinely open for submissions - the same rule the backend
 * applies before it returns the list and before it accepts a proposal for a call.
 * Used as the last guard before a card can be selected.
 */
export function isCallOpen(call, today = localTodayISO()) {
  if (!call || call.id === null || call.id === undefined) return false
  return announcementStatus(call, today) === 'Open'
}

/**
 * True when a published call is still ahead of its submission window: it is shown on
 * the page, but it cannot be applied to yet.
 */
export function isCallScheduled(call, today = localTodayISO()) {
  if (!call || call.id === null || call.id === undefined) return false
  return announcementStatus(call, today) === CALL_STATUS_SCHEDULED
}

/**
 * True when a call belongs on the page at all: open now or scheduled to open later.
 * Drafts, closed, expired and not-yet-published calls are never listed.
 */
export function isCallListable(call, today = localTodayISO()) {
  return isCallOpen(call, today) || isCallScheduled(call, today)
}

/**
 * The call that may be selected, or null when it is not open (expired, closed,
 * scheduled or a draft). Keeps the selection step from ever using a stale call.
 */
export function selectableCall(call, today = localTodayISO()) {
  return isCallOpen(call, today) ? call : null
}

/**
 * The page order: the open calls first (newest submission window first, the order the
 * backend returns them in), then the scheduled calls by their opening date, earliest
 * first. Returns a new array, so the caller's list is never mutated.
 */
export function sortCallsForDisplay(calls, today = localTodayISO()) {
  const list = Array.isArray(calls) ? [...calls] : []
  const openingDate = (call) => String(call?.startDate || '')
  const open = list.filter((call) => isCallOpen(call, today))
  const scheduled = list
    .filter((call) => isCallScheduled(call, today))
    .sort((a, b) => openingDate(a).localeCompare(openingDate(b)) || (a.id ?? 0) - (b.id ?? 0))
  return [...open, ...scheduled]
}
