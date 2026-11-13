/**
 * callForProposal.test.js
 * ---------------------------------------------------------------------------
 * Covers the rules of the Call for Proposal page (step 1 of the Create Proposal
 * workflow): the exact page copy, the deadline / window formatting of the real
 * saved dates, and the guard that only a genuinely open call (ACTIVE and today
 * inside its window - the same rule the backend applies) can be selected.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  AVAILABLE_CALLS_ENDPOINT,
  CALL_DEADLINE_LABEL,
  CALL_FOR_PROPOSAL_INSTRUCTION,
  CALL_FOR_PROPOSAL_TITLE,
  CALL_OPENING_LABEL,
  CALL_SELECTION_BUTTON_LABEL,
  CALL_STATUS_SCHEDULED,
  CALL_WINDOW_LABEL,
  NO_AVAILABLE_CALLS_MESSAGE,
  applicationsOpenOnLabel,
  callDeadlineLabel,
  callOpeningLabel,
  callWindowLabel,
  formatCallDate,
  isCallListable,
  isCallOpen,
  isCallScheduled,
  selectableCall,
  sortCallsForDisplay,
} from './callForProposal.js'

const call = (overrides = {}) => ({
  id: 1,
  name: 'Call for Proposals 2026',
  status: 'ACTIVE',
  startDate: '2026-01-01',
  endDate: '2026-12-31',
  ...overrides,
})

test('the page uses the existing calls endpoint and shows the required copy', () => {
  assert.equal(AVAILABLE_CALLS_ENDPOINT, '/api/proposal-announcements/available')
  assert.equal(CALL_FOR_PROPOSAL_TITLE, 'Call for Proposal')
  assert.equal(
    CALL_FOR_PROPOSAL_INSTRUCTION,
    'Select an open call for proposal that you want to respond to. Review its details and deadline before creating your proposal.',
  )
  assert.equal(CALL_SELECTION_BUTTON_LABEL, 'Apply to This Call')
  assert.equal(
    NO_AVAILABLE_CALLS_MESSAGE,
    'There are currently no open or upcoming calls for proposals. Please check again later.',
  )
  assert.equal(CALL_DEADLINE_LABEL, 'Deadline')
  assert.equal(CALL_WINDOW_LABEL, 'Submission period')
  assert.equal(CALL_OPENING_LABEL, 'Opening date')
  assert.equal(CALL_STATUS_SCHEDULED, 'Scheduled')
})

test('the saved dates are shown in the same format as the rest of RPIS', () => {
  assert.equal(formatCallDate('2026-03-04'), 'March 4, 2026')
  assert.equal(formatCallDate('2026-12-31'), 'December 31, 2026')
  assert.equal(formatCallDate(''), '—')
  assert.equal(formatCallDate(null), '—')
  assert.equal(formatCallDate('not-a-date'), '—')
})

test('the deadline and the submission window come from the saved dates', () => {
  const open = call({ startDate: '2026-02-02', endDate: '2026-03-04' })
  assert.equal(callDeadlineLabel(open), 'March 4, 2026')
  assert.equal(callWindowLabel(open), 'February 2, 2026 – March 4, 2026')
})

test('a call is open only while it is ACTIVE and inside its window', () => {
  const today = '2026-06-15'
  assert.equal(isCallOpen(call(), today), true)
  assert.equal(isCallOpen(call({ startDate: today, endDate: today }), today), true)
  assert.equal(isCallOpen(call({ startDate: today }), today), true)
  assert.equal(isCallOpen(call({ endDate: today }), today), true)
})

test('drafts, scheduled, closed and expired calls are never open', () => {
  const today = '2026-06-15'
  assert.equal(isCallOpen(call({ status: 'DRAFT' }), today), false)
  assert.equal(isCallOpen(call({ status: 'CLOSED' }), today), false)
  assert.equal(isCallOpen(call({ startDate: '2026-07-01', endDate: '2026-08-01' }), today), false)
  assert.equal(isCallOpen(call({ startDate: '2026-01-01', endDate: '2026-06-14' }), today), false)
})

test('a call without an identifier can never be selected', () => {
  assert.equal(isCallOpen(null, '2026-06-15'), false)
  assert.equal(isCallOpen(undefined, '2026-06-15'), false)
  assert.equal(isCallOpen(call({ id: null }), '2026-06-15'), false)
  assert.equal(selectableCall(call({ id: null }), '2026-06-15'), null)
})

test('only an open call is handed to the workflow', () => {
  const today = '2026-06-15'
  const open = call({ id: 7 })
  assert.equal(selectableCall(open, today), open)
  assert.equal(selectableCall(call({ status: 'CLOSED' }), today), null)
  assert.equal(selectableCall(call({ endDate: '2026-06-14' }), today), null)
})

test('a scheduled call is listed but can never be selected', () => {
  const today = '2026-06-15'
  const upcoming = call({
    id: 3,
    name: 'Upcoming Call',
    startDate: '2026-07-01',
    endDate: '2026-07-31',
  })

  assert.equal(isCallScheduled(upcoming, today), true)
  assert.equal(isCallListable(upcoming, today), true)
  assert.equal(isCallOpen(upcoming, today), false)
  // The button is disabled in the UI and this guard is the last line of defence.
  assert.equal(selectableCall(upcoming, today), null)
})

test('the day a call opens it stops being scheduled and becomes selectable', () => {
  const upcoming = call({ id: 3, startDate: '2026-07-01', endDate: '2026-07-31' })

  assert.equal(isCallScheduled(upcoming, '2026-06-30'), true)
  assert.equal(isCallOpen(upcoming, '2026-06-30'), false)
  assert.equal(isCallScheduled(upcoming, '2026-07-01'), false)
  assert.equal(isCallOpen(upcoming, '2026-07-01'), true)
  assert.equal(selectableCall(upcoming, '2026-07-01'), upcoming)
})

test('a call stops being open once its deadline has passed', () => {
  const callEndingToday = call({ id: 4, startDate: '2026-06-01', endDate: '2026-06-15' })

  assert.equal(isCallOpen(callEndingToday, '2026-06-15'), true)
  assert.equal(isCallOpen(callEndingToday, '2026-06-16'), false)
  assert.equal(isCallListable(callEndingToday, '2026-06-16'), false)
})

test('drafts, closed and expired calls are never listed', () => {
  const today = '2026-06-15'
  assert.equal(isCallListable(call({ status: 'DRAFT' }), today), false)
  assert.equal(isCallListable(call({ status: 'CLOSED' }), today), false)
  assert.equal(
    isCallListable(call({ startDate: '2026-01-01', endDate: '2026-06-14' }), today),
    false,
  )
  assert.equal(isCallListable(call({ id: null }), today), false)
  assert.equal(isCallListable(null, today), false)
})

test('the opening date and the notice use the real saved opening date', () => {
  const upcoming = call({ id: 3, startDate: '2026-11-02', endDate: '2026-11-30' })

  assert.equal(callOpeningLabel(upcoming), 'November 2, 2026')
  assert.equal(applicationsOpenOnLabel(upcoming), 'Applications will open on November 2, 2026.')
  // A missing or unusable date is reported, never rendered as "Invalid Date".
  assert.equal(applicationsOpenOnLabel({}), 'Applications will open on —.')
})

test('open calls come first and upcoming calls follow by opening date', () => {
  const today = '2026-06-15'
  const calls = [
    call({ id: 5, name: 'Upcoming later', startDate: '2026-09-01', endDate: '2026-09-30' }),
    call({ id: 1, name: 'Open A', startDate: '2026-06-01', endDate: '2026-06-30' }),
    call({ id: 6, name: 'Upcoming sooner', startDate: '2026-07-01', endDate: '2026-07-31' }),
    call({ id: 2, name: 'Open B', startDate: '2026-06-10', endDate: '2026-06-20' }),
  ]

  assert.deepEqual(
    sortCallsForDisplay(calls, today).map((c) => c.name),
    ['Open A', 'Open B', 'Upcoming sooner', 'Upcoming later'],
  )
})

test('the display order drops everything that is not listable and keeps the input intact', () => {
  const today = '2026-06-15'
  const calls = [
    call({ id: 1, name: 'Open' }),
    call({ id: 2, name: 'Draft', status: 'DRAFT' }),
    call({ id: 3, name: 'Expired', startDate: '2026-01-01', endDate: '2026-06-14' }),
    call({ id: 4, name: 'Upcoming', startDate: '2026-08-01', endDate: '2026-08-31' }),
  ]

  // The page filters with isCallListable and then sorts; nothing else is dropped.
  const listable = calls.filter((c) => isCallListable(c, today))
  assert.deepEqual(
    sortCallsForDisplay(listable, today).map((c) => c.name),
    ['Open', 'Upcoming'],
  )
  assert.equal(calls.length, 4, 'the caller list must not be mutated')
  assert.deepEqual(sortCallsForDisplay(null, today), [])
})
