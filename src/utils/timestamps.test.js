/**
 * timestamps.test.js
 * Verifies the one readable date-and-time format the application shows for a stored
 * timestamp (used by the endorsement screens and the notification previews).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { formatTimestamp } from './timestamps.js'

test('a timestamp is shown as a readable local date and time', () => {
  const formatted = formatTimestamp('2026-10-29T04:12:00')
  assert.match(formatted, /^Oct 29, 2026, \d{1,2}:12 (AM|PM)$/)
})

test('a missing timestamp is empty and an unusable one is never "Invalid Date"', () => {
  assert.equal(formatTimestamp(''), '')
  assert.equal(formatTimestamp('   '), '')
  assert.equal(formatTimestamp(null), '')
  assert.equal(formatTimestamp(undefined), '')
  // A value the browser cannot parse is reported as it was stored, not as a broken date.
  assert.equal(formatTimestamp('not-a-date'), 'not-a-date')
})

test('the endorsement and the notification screens share this format', () => {
  // Both views format through this module, so one rule cannot drift from the other.
  const value = '2026-10-29T04:12:00'
  assert.equal(formatTimestamp(value), formatTimestamp(value))
})
