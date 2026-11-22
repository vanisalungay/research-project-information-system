/**
 * dashboardProposals.test.js
 * ---------------------------------------------------------------------------
 * Pins down the dashboard preview rule: at most five distinct proposals, newest
 * first, without changing the full list the dashboard uses for its counts.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  DASHBOARD_PROPOSAL_LIMIT,
  latestProposalSnippets,
  proposalRecency,
} from './dashboardProposals.js'

const proposal = (id, updatedAt, extra = {}) => ({
  id,
  projectTitle: `Proposal ${id}`,
  updatedAt,
  createdAt: updatedAt,
  ...extra,
})

test('the preview never shows more than five proposals', () => {
  const list = Array.from({ length: 12 }, (unused, index) =>
    proposal(index + 1, `2026-11-${String(index + 1).padStart(2, '0')}T10:00:00`),
  )
  const preview = latestProposalSnippets(list)
  assert.equal(preview.length, DASHBOARD_PROPOSAL_LIMIT)
  assert.equal(DASHBOARD_PROPOSAL_LIMIT, 5)
})

test('the newest proposal is first', () => {
  const preview = latestProposalSnippets([
    proposal(1, '2026-11-01T10:00:00'),
    proposal(2, '2026-11-05T10:00:00'),
    proposal(3, '2026-11-03T10:00:00'),
  ])
  assert.deepEqual(preview.map((p) => p.id), [2, 3, 1])
})

test('the requested date field wins, with the known fields as fallbacks', () => {
  const rows = [
    { id: 1, createdAt: '2026-11-01T10:00:00', updatedAt: '2026-11-09T10:00:00' },
    { id: 2, createdAt: '2026-11-05T10:00:00', updatedAt: '2026-11-02T10:00:00' },
  ]
  // Newest submission first.
  assert.deepEqual(
    latestProposalSnippets(rows, { dateField: 'createdAt' }).map((p) => p.id),
    [2, 1],
  )
  // Newest status change first.
  assert.deepEqual(
    latestProposalSnippets(rows, { dateField: 'updatedAt' }).map((p) => p.id),
    [1, 2],
  )
})

test('an explicit fallback list stops a submission sort falling through to an edit time', () => {
  const rows = [
    { id: 1, submittedAt: '2026-11-10T10:00:00', updatedAt: '2026-11-01T10:00:00' },
    { id: 2, createdAt: '2026-11-05T10:00:00', updatedAt: '2026-11-20T10:00:00' },
  ]
  assert.deepEqual(
    latestProposalSnippets(rows, {
      dateField: 'submittedAt',
      fallbackDateFields: ['createdAt'],
    }).map((p) => p.id),
    [1, 2],
    'the row without a submission date uses its creation date, never its edit date',
  )
})

test('an invalid or missing date falls back to the id, and ties break by id', () => {
  const rows = [
    { id: 3, updatedAt: 'not-a-date' },
    { id: 7 },
    { id: 5, updatedAt: null },
    { id: 6, updatedAt: '2026-11-05T10:00:00' },
  ]
  assert.deepEqual(latestProposalSnippets(rows).map((p) => p.id), [6, 7, 5, 3])
  assert.equal(proposalRecency({ id: 1 }), 0)
})

test('duplicate proposals appear only once', () => {
  const row = proposal(4, '2026-11-04T10:00:00')
  const preview = latestProposalSnippets([row, { ...row }, proposal(5, '2026-11-03T10:00:00')])
  assert.deepEqual(preview.map((p) => p.id), [4, 5])
})

test('the original list and its order are not changed', () => {
  const list = [
    proposal(1, '2026-11-01T10:00:00'),
    proposal(2, '2026-11-02T10:00:00'),
    proposal(3, '2026-11-03T10:00:00'),
  ]
  const before = list.map((p) => p.id)
  const preview = latestProposalSnippets(list)
  assert.deepEqual(list.map((p) => p.id), before)
  assert.notEqual(preview, list)
})

test('a caller can request a smaller preview and an empty list is safe', () => {
  const list = [
    proposal(1, '2026-11-01T10:00:00'),
    proposal(2, '2026-11-02T10:00:00'),
    proposal(3, '2026-11-03T10:00:00'),
  ]
  assert.deepEqual(latestProposalSnippets(list, { limit: 2 }).map((p) => p.id), [3, 2])
  assert.deepEqual(latestProposalSnippets([]), [])
  assert.deepEqual(latestProposalSnippets(null), [])
  assert.deepEqual(latestProposalSnippets(list, { limit: 0 }), [])
})
