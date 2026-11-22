/**
 * proposalTimeline.test.js
 * ---------------------------------------------------------------------------
 * The OVCRIGE Status Timeline must be newest-first by the real event timestamp, keep
 * every milestone and history record, and never use a formatted date string or a
 * BASELINE_SEED row to decide the order.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { buildProposalTimeline } from './proposalTimeline.js'

const row = (id, toStatus, changedAt, extra = {}) => ({
  id,
  toStatus,
  changedAt,
  source: 'APPLICATION',
  ...extra,
})

test('milestones and real transitions are ordered newest first by raw timestamp', () => {
  const proposal = {
    status: 'FOR_OC_APPROVAL',
    submittedAt: '2026-11-01T09:00:00',
    endorsedAt: '2026-11-02T10:00:00',
  }
  const history = [
    row(5, 'FOR_OC_APPROVAL', '2026-11-10T12:00:00', { actorName: 'OVCRIGE User' }),
    row(4, 'REC_APPROVED', '2026-11-09T11:00:00'),
    row(3, 'UNDER_REVIEW', '2026-11-05T09:00:00'),
    row(2, 'ENDORSED', '2026-11-02T10:00:00'),
    row(1, 'FOR_REVIEW', '2026-11-01T09:00:00'),
  ]

  const timeline = buildProposalTimeline(proposal, history)

  assert.deepEqual(
    timeline.map((entry) => entry.label),
    [
      'Forwarded to Chancellor',
      'REC Approved',
      'Forwarded to REC',
      'RPS Endorsed',
      'Proposal Submitted',
    ],
  )
  assert.equal(timeline[0].state, 'active')
  assert.equal(timeline[0].actorName, 'OVCRIGE User')
  assert.equal(timeline[1].state, 'completed')
})

test('a baseline seed row never decides the order', () => {
  const proposal = { status: 'FOR_OC_APPROVAL', submittedAt: '2026-11-01T09:00:00' }
  const history = [
    // Derived from an arbitrary updated_at by the migration: not an event.
    row(99, 'FOR_OC_APPROVAL', '2026-12-01T00:00:00', { source: 'BASELINE_SEED' }),
    row(5, 'FOR_OC_APPROVAL', '2026-11-10T12:00:00'),
  ]

  const timeline = buildProposalTimeline(proposal, history)
  assert.equal(timeline[0].timestamp, '2026-11-10T12:00:00')
  assert.equal(
    timeline.some((entry) => entry.timestamp === '2026-12-01T00:00:00'),
    false,
    'a baseline row must not appear or affect the order',
  )
})

test('the current stage stays first even without a history timestamp', () => {
  const proposal = {
    status: 'UNDER_REVIEW',
    submittedAt: '2026-11-01T09:00:00',
    endorsedAt: '2026-11-02T10:00:00',
  }
  const timeline = buildProposalTimeline(proposal, [])
  assert.equal(timeline[0].label, 'Forwarded to REC')
  assert.equal(timeline[0].state, 'active')
})

test('same-timestamp events are broken by the history id, newest first', () => {
  const proposal = { status: 'RETURNED_TO_RPS' }
  const history = [
    row(7, 'RETURNED_TO_RPS', '2026-11-10T12:00:00', { remarks: 'First reason' }),
    row(8, 'RETURNED_TO_RPS', '2026-11-10T12:00:00', { remarks: 'Second reason' }),
  ]
  const timeline = buildProposalTimeline(proposal, history)
  assert.equal(timeline[0].historyId, 8)
  assert.equal(timeline[1].historyId, 7)
})

test('a return/rejection remains a visible history entry', () => {
  const proposal = {
    status: 'RETURNED_TO_RPS',
    submittedAt: '2026-11-01T09:00:00',
    endorsedAt: '2026-11-02T10:00:00',
  }
  const history = [
    row(9, 'RETURNED_TO_RPS', '2026-11-12T08:00:00', {
      actorName: 'OVCRIGE User',
      remarks: 'Does not meet the program criteria.',
    }),
    row(2, 'ENDORSED', '2026-11-02T10:00:00'),
  ]

  const timeline = buildProposalTimeline(proposal, history)
  assert.equal(timeline[0].label, 'Returned to RPS')
  assert.equal(timeline[0].remarks, 'Does not meet the program criteria.')
  assert.equal(timeline[0].actorName, 'OVCRIGE User')
  assert.ok(timeline.some((entry) => entry.label === 'RPS Endorsed'))
  assert.ok(timeline.some((entry) => entry.label === 'Proposal Submitted'))
})

test('undated intermediate milestones still sort in workflow order', () => {
  const proposal = {
    status: 'FOR_OC_APPROVAL',
    submittedAt: '2026-11-01T09:00:00',
    endorsedAt: '2026-11-02T10:00:00',
  }
  // No history rows at all: REC review and REC approval have no recorded timestamps.
  const timeline = buildProposalTimeline(proposal, [])
  assert.deepEqual(
    timeline.map((entry) => entry.label),
    [
      'Forwarded to Chancellor',
      'REC Approved',
      'Forwarded to REC',
      'RPS Endorsed',
      'Proposal Submitted',
    ],
  )
  // The undated milestones keep their status text, not a made-up date.
  assert.equal(timeline[1].timestamp, null)
  assert.equal(timeline[1].detail, 'Completed')
})

test('the six milestone labels are preserved', () => {
  const timeline = buildProposalTimeline(
    { status: 'APPROVED', submittedAt: '2026-11-01T09:00:00', endorsedAt: '2026-11-02T10:00:00' },
    [
      row(2, 'ENDORSED', '2026-11-02T10:00:00'),
      row(3, 'UNDER_REVIEW', '2026-11-03T10:00:00'),
      row(4, 'REC_APPROVED', '2026-11-04T10:00:00'),
      row(5, 'FOR_OC_APPROVAL', '2026-11-05T10:00:00'),
      row(6, 'APPROVED', '2026-11-06T10:00:00'),
    ],
  )
  for (const label of [
    'Proposal Submitted',
    'RPS Endorsed',
    'Forwarded to REC',
    'REC Approved',
    'Forwarded to Chancellor',
    'Final Approval',
  ]) {
    assert.ok(timeline.some((entry) => entry.label === label), label)
  }
  assert.equal(timeline[0].label, 'Final Approval')
})
