/**
 * announcementUpdates.test.js
 * Verifies the deadline extension notice of a Call for Proposal: the previous deadline is
 * the deadline recorded for THAT extension event (never the new deadline and never an
 * older event's deadline), each extension keeps its own dates, and a call that was never
 * extended - or is no longer open - shows no notice.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  announcementHistory,
  extensionNotice,
  latestDeadlineExtension,
} from './announcementUpdates.js'

/** An extension event: November 12, 2026 -> November 20, 2026. */
const firstExtension = {
  id: 12,
  updateType: 'DEADLINE_EXTENDED',
  previousStatus: 'ACTIVE',
  previousStartDate: '2026-11-01',
  previousEndDate: '2026-11-12',
  newStatus: 'ACTIVE',
  newStartDate: '2026-11-01',
  newEndDate: '2026-11-20',
  effectiveDate: '2026-11-10',
}

/** A second extension of the same call: November 20, 2026 -> November 25, 2026. */
const secondExtension = {
  id: 13,
  updateType: 'DEADLINE_EXTENDED',
  previousStatus: 'ACTIVE',
  previousStartDate: '2026-11-01',
  previousEndDate: '2026-11-20',
  newStatus: 'ACTIVE',
  newStartDate: '2026-11-01',
  newEndDate: '2026-11-25',
  effectiveDate: '2026-11-18',
}

/** The publication record, for calls that were published after the history existed. */
const published = {
  id: 11,
  updateType: 'PUBLISHED',
  previousStatus: null,
  previousEndDate: null,
  newStatus: 'ACTIVE',
  newStartDate: '2026-11-01',
  newEndDate: '2026-11-12',
  effectiveDate: '2026-11-01',
}

/** The call as the API returns it while it is open under the extended deadline. */
const extendedOpenCall = {
  id: 11,
  name: 'Call for Extension and Community Development Proposals',
  status: 'ACTIVE',
  startDate: '2026-11-01',
  endDate: '2026-11-20',
  updates: [firstExtension],
}

test('the recorded history is read oldest first, and only from stored entries', () => {
  const history = announcementHistory({
    updates: [secondExtension, published, firstExtension],
  })

  assert.deepEqual(
    history.map((entry) => entry.id),
    [11, 12, 13],
  )
  assert.deepEqual(
    history.map((entry) => entry.newEndDate),
    ['2026-11-12', '2026-11-20', '2026-11-25'],
  )
})

test('a call without recorded history has an empty history (nothing is invented)', () => {
  assert.deepEqual(announcementHistory({ id: 1, status: 'ACTIVE' }), [])
  assert.deepEqual(announcementHistory({ id: 1, updates: null }), [])
  assert.deepEqual(announcementHistory(null), [])
  assert.deepEqual(announcementHistory(undefined), [])
})

test('the latest extension is the last recorded extension event', () => {
  const extension = latestDeadlineExtension({
    updates: [published, firstExtension, secondExtension],
  })

  assert.equal(extension.id, 13)
  assert.equal(extension.previousEndDate, '2026-11-20')
  assert.equal(extension.newEndDate, '2026-11-25')
})

test('a call that was never extended has no extension', () => {
  assert.equal(latestDeadlineExtension({ updates: [published] }), null)
  assert.equal(latestDeadlineExtension({ id: 1 }), null)
})

// Verification 1: the first extension (November 12 -> November 20) announces the new
// deadline and the deadline it replaced.
test('the first extension shows the previous deadline and the new deadline', () => {
  const notice = extensionNotice(extendedOpenCall, '2026-11-15')

  assert.equal(notice.message, 'This call for proposal has been extended until November 20, 2026.')
  assert.equal(notice.previousDeadlineMessage, 'Previous deadline: November 12, 2026')
  assert.equal(notice.newDeadline, '2026-11-20')
  assert.equal(notice.previousDeadline, '2026-11-12')
})

// Regression: the previous deadline must never be the new deadline itself. This is the
// case that used to be wrong - a call whose only recorded entry is the extension (calls
// published before the history existed have no publication record).
test("the previous deadline is never the extension's own new deadline", () => {
  const onlyExtensionRecorded = { ...extendedOpenCall, updates: [firstExtension] }
  const notice = extensionNotice(onlyExtensionRecorded, '2026-11-15')

  assert.notEqual(notice.previousDeadline, notice.newDeadline)
  assert.equal(notice.previousDeadlineMessage, 'Previous deadline: November 12, 2026')
})

// Verification 2: a second extension (November 20 -> November 25) announces its own
// previous deadline, not the call's first deadline.
test('a second extension shows the deadline the first extension had set', () => {
  const twiceExtended = {
    ...extendedOpenCall,
    endDate: '2026-11-25',
    updates: [published, firstExtension, secondExtension],
  }
  const notice = extensionNotice(twiceExtended, '2026-11-20')

  assert.equal(notice.message, 'This call for proposal has been extended until November 25, 2026.')
  assert.equal(notice.previousDeadlineMessage, 'Previous deadline: November 20, 2026')
  assert.equal(notice.previousDeadline, '2026-11-20')
})

// Verification 3: an earlier extension keeps its own dates after a later extension.
test('an earlier extension keeps the dates of its own event', () => {
  const first = latestDeadlineExtension({ updates: [published, firstExtension] })
  const second = latestDeadlineExtension({
    updates: [published, firstExtension, secondExtension],
  })

  assert.equal(first.previousEndDate, '2026-11-12')
  assert.equal(first.newEndDate, '2026-11-20')
  assert.equal(second.previousEndDate, '2026-11-20')
  assert.equal(second.newEndDate, '2026-11-25')
})

test('the previous deadline is omitted when the event recorded none', () => {
  const withoutPrevious = {
    ...extendedOpenCall,
    updates: [{ id: 12, updateType: 'DEADLINE_EXTENDED', newEndDate: '2026-11-20' }],
  }
  const notice = extensionNotice(withoutPrevious, '2026-11-15')

  assert.equal(notice.previousDeadline, '')
  assert.equal(notice.previousDeadlineMessage, '')
  assert.equal(notice.message, 'This call for proposal has been extended until November 20, 2026.')
})

test('a call with no recorded extension shows no extension notice', () => {
  const call = {
    id: 1,
    status: 'ACTIVE',
    startDate: '2026-11-01',
    endDate: '2026-11-20',
    updates: [published],
  }

  assert.equal(extensionNotice(call, '2026-11-15'), null)
})

test('a call that is not open shows no extension notice', () => {
  // Stored status CLOSED wins over the dates.
  assert.equal(extensionNotice({ ...extendedOpenCall, status: 'CLOSED' }, '2026-11-15'), null)
  // The extended window is over as well.
  assert.equal(extensionNotice(extendedOpenCall, '2026-12-05'), null)
  // A published call whose window has not started yet.
  assert.equal(
    extensionNotice(
      { ...extendedOpenCall, startDate: '2026-12-01', endDate: '2026-12-20' },
      '2026-11-15',
    ),
    null,
  )
})

test('the notice stays visible after the superseded deadline has passed', () => {
  // November 12 is in the past, the extended deadline is not, and the call is still ACTIVE.
  const notice = extensionNotice(extendedOpenCall, '2026-11-15')

  assert.notEqual(notice, null)
  assert.match(notice.message, /November 20, 2026/)
})

test('unusable stored dates never produce a broken notice', () => {
  const call = {
    ...extendedOpenCall,
    updates: [
      {
        id: 12,
        updateType: 'DEADLINE_EXTENDED',
        previousEndDate: 'not-a-date',
        newEndDate: '2026-11-20',
      },
    ],
  }

  assert.equal(extensionNotice(call, '2026-11-15').previousDeadlineMessage, 'Previous deadline: —')
})
