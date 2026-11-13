/**
 * revisionDeadline.test.js
 * Verifies the revision-deadline wording and the pending-revision eligibility rule used by
 * the Proponent dashboard: "1 day left" is singular, expired deadlines are excluded from
 * the count, deadlines later today are still counted, and non-revision statuses are ignored.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  PENDING_REVISION_STATUSES,
  revisionDeadlineTime,
  isRevisionDeadlinePassed,
  revisionDaysLeft,
  revisionDaysLeftLabel,
  isPendingRevision,
  countPendingRevisions,
  compareRevisionDeadlines,
} from './revisionDeadline.js'

const DAY = 24 * 60 * 60 * 1000
const NOW = new Date('2026-10-19T09:00:00').getTime()
const at = (offsetMs) => new Date(NOW + offsetMs).toISOString()

// 1. Singular / plural wording.
test('a single remaining day uses the singular label', () => {
  assert.equal(revisionDaysLeftLabel(1), '1 day left')
})

test('any other number of days uses the plural label', () => {
  assert.equal(revisionDaysLeftLabel(2), '2 days left')
  assert.equal(revisionDaysLeftLabel(3), '3 days left')
  assert.equal(revisionDaysLeftLabel(0), '0 days left')
  assert.equal(revisionDaysLeftLabel(14), '14 days left')
})

// 2. Remaining-days calculation (same formula the dashboard already used).
test('whole days remaining are counted from the saved deadline', () => {
  assert.equal(revisionDaysLeft(at(3 * DAY + 60_000), NOW), 3)
  assert.equal(revisionDaysLeft(at(DAY), NOW), 1)
  assert.equal(revisionDaysLeft(at(-DAY), NOW), 0)
  assert.equal(revisionDaysLeft(null, NOW), 0)
  assert.equal(revisionDaysLeft('not-a-date', NOW), 0)
  assert.equal(revisionDeadlineTime('not-a-date'), null)
})

// 3. Expired vs still-eligible deadlines.
test('only a deadline strictly in the past counts as passed', () => {
  assert.equal(isRevisionDeadlinePassed(at(-60_000), NOW), true)
  assert.equal(isRevisionDeadlinePassed(at(60_000), NOW), false)
  assert.equal(isRevisionDeadlinePassed(at(3 * DAY), NOW), false)
  assert.equal(isRevisionDeadlinePassed(at(0), NOW), false)
  assert.equal(isRevisionDeadlinePassed(null, NOW), false)
  assert.equal(isRevisionDeadlinePassed('not-a-date', NOW), false)
})

// 4. The counting example from the requirement.
test('expired revisions are excluded while future and same-day revisions are counted', () => {
  const proposals = [
    { id: 1, status: 'RPS_RETURNED', revisionDeadline: at(DAY) },
    { id: 2, status: 'REC_REVISION', revisionDeadline: at(3 * DAY) },
    { id: 3, status: 'REVISION', revisionDeadline: at(-DAY) },
    { id: 4, status: 'RPS_RETURNED', revisionDeadline: at(2 * 60 * 60 * 1000) },
  ]

  assert.equal(countPendingRevisions(proposals, NOW), 3)
  assert.deepEqual(
    proposals.filter((proposal) => isPendingRevision(proposal, NOW)).map((proposal) => proposal.id),
    [1, 2, 4]
  )
})

// 5. Status rules are unchanged - only the revision statuses can be pending.
test('statuses outside the revision workflow are never pending', () => {
  const otherStatuses = [
    'DRAFT',
    'SUBMITTED',
    'ENDORSED',
    'UNDER_REVIEW',
    'REC_APPROVED',
    'APPROVED',
    'RELEASED',
    'COMPLETED',
    'REJECTED',
    'REC_REJECTED',
  ]

  for (const status of otherStatuses) {
    assert.equal(isPendingRevision({ status, revisionDeadline: at(DAY) }, NOW), false, status)
  }
})

test('a revision status whose deadline expired is not pending', () => {
  for (const status of PENDING_REVISION_STATUSES) {
    assert.equal(isPendingRevision({ status, revisionDeadline: at(-1) }, NOW), false, status)
    assert.equal(isPendingRevision({ status, revisionDeadline: at(DAY) }, NOW), true, status)
  }
})

test('a revision status without a deadline is still pending', () => {
  assert.equal(isPendingRevision({ status: 'RPS_RETURNED', revisionDeadline: null }, NOW), true)
})

// 6. Each eligible revision is counted exactly once.
test('every eligible revision is counted once and only once', () => {
  const proposals = [
    { id: 1, status: 'RPS_RETURNED', revisionDeadline: at(DAY) },
    { id: 2, status: 'RPS_RETURNED', revisionDeadline: at(-DAY) },
    { id: 3, status: 'REVISION', revisionDeadline: at(5 * DAY) },
    { id: 4, status: 'COMPLETED', revisionDeadline: at(5 * DAY) },
  ]

  assert.equal(countPendingRevisions(proposals, NOW), 2)
})

test('missing or empty proposal data counts as zero', () => {
  assert.equal(countPendingRevisions(null, NOW), 0)
  assert.equal(countPendingRevisions([], NOW), 0)
  assert.equal(countPendingRevisions([null, undefined], NOW), 0)
})

// 7. Queue ordering for the revisions page.
const order = (deadlines) =>
  deadlines
    .slice()
    .sort((a, b) => compareRevisionDeadlines(a.deadline, b.deadline, NOW))
    .map((item) => item.id)

test('revisions that can still be revised come before expired ones', () => {
  const queue = [
    { id: 'expired', deadline: at(-DAY) },
    { id: 'active', deadline: at(2 * DAY) },
  ]

  assert.deepEqual(order(queue), ['active', 'expired'])
})

test('active revisions are ordered by the nearest deadline first', () => {
  const queue = [
    { id: 'later', deadline: at(5 * DAY) },
    { id: 'soon', deadline: at(DAY) },
    { id: 'later-today', deadline: at(60 * 60 * 1000) },
    { id: 'middle', deadline: at(3 * DAY) },
  ]

  assert.deepEqual(order(queue), ['later-today', 'soon', 'middle', 'later'])
})

test('expired revisions are ordered with the most recently expired first', () => {
  const queue = [
    { id: 'oldest', deadline: at(-10 * DAY) },
    { id: 'newest', deadline: at(-1 * 60 * 1000) },
    { id: 'middle', deadline: at(-3 * DAY) },
  ]

  assert.deepEqual(order(queue), ['newest', 'middle', 'oldest'])
})

test('a full queue is grouped: active (nearest first) then expired (latest first)', () => {
  const queue = [
    { id: 'expired-old', deadline: at(-8 * DAY) },
    { id: 'active-far', deadline: at(6 * DAY) },
    { id: 'expired-recent', deadline: at(-2 * 60 * 60 * 1000) },
    { id: 'active-near', deadline: at(2 * DAY) },
    { id: 'expired-yesterday', deadline: at(-DAY) },
  ]

  assert.deepEqual(order(queue), [
    'active-near',
    'active-far',
    'expired-recent',
    'expired-yesterday',
    'expired-old',
  ])
})

test('revisions without a usable deadline are not expired and sort after dated ones', () => {
  const queue = [
    { id: 'no-deadline', deadline: null },
    { id: 'expired', deadline: at(-DAY) },
    { id: 'active', deadline: at(DAY) },
    { id: 'bad-deadline', deadline: 'not-a-date' },
  ]

  assert.deepEqual(order(queue), ['active', 'no-deadline', 'bad-deadline', 'expired'])
})

test('equal deadlines keep their existing relative order', () => {
  const deadline = at(3 * DAY)
  const queue = [
    { id: 'first', deadline },
    { id: 'second', deadline },
    { id: 'third', deadline },
  ]

  assert.deepEqual(order(queue), ['first', 'second', 'third'])
  assert.equal(compareRevisionDeadlines(deadline, deadline, NOW), 0)
})
