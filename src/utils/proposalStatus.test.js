/**
 * proposalStatus.test.js
 * Verifies which statuses expose the "Make a Copy" action on the proponent pages.
 * Only REJECTED proposals may be copied: approval and returned-for-revision
 * statuses must never show the action.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { DUPLICABLE_STATUSES, isDuplicable } from './proposalStatus.js'

test('REJECTED is the only copyable status', () => {
  assert.deepEqual(DUPLICABLE_STATUSES, ['REJECTED'])
})

test('a rejected proposal shows the Make a Copy action', () => {
  assert.equal(isDuplicable('REJECTED'), true)
  // Status matching is case-insensitive.
  assert.equal(isDuplicable('rejected'), true)
})

test('an approved proposal does not show the Make a Copy action', () => {
  assert.equal(isDuplicable('APPROVED'), false)
})

test('returned-for-revision statuses do not show the Make a Copy action', () => {
  assert.equal(isDuplicable('RPS_RETURNED'), false)
  assert.equal(isDuplicable('REVISION'), false)
  assert.equal(isDuplicable('REC_REVISION'), false)
  assert.equal(isDuplicable('RETURNED_TO_RPS'), false)
})

test('draft and submitted proposals do not show the Make a Copy action', () => {
  assert.equal(isDuplicable('DRAFT'), false)
  assert.equal(isDuplicable('SUBMITTED'), false)
})

test('missing or non-string statuses never show the Make a Copy action', () => {
  assert.equal(isDuplicable(null), false)
  assert.equal(isDuplicable(undefined), false)
  assert.equal(isDuplicable(''), false)
  assert.equal(isDuplicable(42), false)
})
