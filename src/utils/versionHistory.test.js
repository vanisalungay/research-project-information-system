/**
 * versionHistory.test.js
 * ---------------------------------------------------------------------------
 * Submission & Revision History ordering: newest actual event first, with a
 * deterministic secondary order and no mutation of the fetched records.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { sortVersionsNewestFirst, versionEventTimestamp } from './versionHistory.js'

const v = (versionNumber, submissionDate, revisionDate = null) => ({
  id: versionNumber,
  versionNumber,
  submissionDate,
  revisionDate,
})

test('a revision uses its revision event, not the original submission date', () => {
  const original = v(1, '2025-01-01T08:00:00')
  const revised = v(2, '2025-01-01T08:00:00', '2025-03-01T09:00:00')
  assert.equal(
    versionEventTimestamp(revised),
    new Date('2025-03-01T09:00:00').getTime(),
  )
  assert.deepEqual(sortVersionsNewestFirst([original, revised]).map((x) => x.versionNumber), [2, 1])
})

test('the newest submission or revision is first and older ones follow', () => {
  const rows = [
    v(1, '2025-01-01T08:00:00'),
    v(3, '2025-01-01T08:00:00', '2025-06-01T08:00:00'),
    v(2, '2025-01-01T08:00:00', '2025-03-01T08:00:00'),
  ]
  assert.deepEqual(sortVersionsNewestFirst(rows).map((x) => x.versionNumber), [3, 2, 1])
})

test('a row-creation timestamp never outranks a newer submission event', () => {
  const rows = [
    { id: 1, versionNumber: 1, submissionDate: '2025-01-01T08:00:00', createdAt: '2025-01-01T08:00:00' },
    { id: 2, versionNumber: 2, submissionDate: '2025-05-01T08:00:00', createdAt: '2025-01-02T08:00:00' },
  ]
  assert.deepEqual(sortVersionsNewestFirst(rows).map((x) => x.versionNumber), [2, 1])
})

test('createdAt is only the fallback for a legacy row with no event date', () => {
  const rows = [
    { id: 1, versionNumber: 1, createdAt: '2025-01-01T00:00:00' },
    { id: 2, versionNumber: 2, createdAt: '2025-02-01T00:00:00' },
  ]
  assert.deepEqual(sortVersionsNewestFirst(rows).map((x) => x.versionNumber), [2, 1])
})

test('identical timestamps use version number, then id, descending', () => {
  const rows = [
    { id: 9, versionNumber: 2, submissionDate: '2025-01-01T00:00:00' },
    { id: 4, versionNumber: 2, submissionDate: '2025-01-01T00:00:00' },
    { id: 1, versionNumber: 1, submissionDate: '2025-01-01T00:00:00' },
  ]
  assert.deepEqual(sortVersionsNewestFirst(rows).map((x) => x.id), [9, 4, 1])
})

test('rows without any usable timestamp sort last', () => {
  const rows = [
    { id: 1, versionNumber: 1 },
    { id: 3, versionNumber: 3, submissionDate: 'not-a-date' },
    { id: 2, versionNumber: 2, submissionDate: '2025-01-01T00:00:00' },
  ]
  assert.deepEqual(sortVersionsNewestFirst(rows).map((x) => x.id), [2, 3, 1])
})

test('sorting returns a new array and preserves every record', () => {
  const rows = [v(1, '2025-01-01T00:00:00'), v(2, '2025-02-01T00:00:00')]
  const sorted = sortVersionsNewestFirst(rows)
  assert.notEqual(sorted, rows)
  assert.deepEqual(rows.map((x) => x.versionNumber), [1, 2])
  assert.equal(sorted.length, 2)
  assert.deepEqual(sortVersionsNewestFirst(null), [])
})
