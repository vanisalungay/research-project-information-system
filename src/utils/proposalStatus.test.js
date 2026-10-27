/**
 * proposalStatus.test.js
 * Verifies which statuses expose the "Make a Copy" action.
 *
 * The action is available for a proposal in any status - the copy is a brand-new
 * draft that inherits no workflow state, so the original's status never decides
 * whether its content may be reused.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { COPYABLE_STATUSES, PROPOSAL_STATUSES, isDuplicable } from './proposalStatus.js'

test('every status the workflow reports is copyable', () => {
  assert.ok(PROPOSAL_STATUSES.length > 0, 'the status vocabulary must not be empty')
  assert.deepEqual(COPYABLE_STATUSES, PROPOSAL_STATUSES)
  for (const status of PROPOSAL_STATUSES) {
    assert.equal(isDuplicable(status), true, `${status} must show the Make a Copy action`)
    assert.equal(
      isDuplicable(status.toLowerCase()),
      true,
      `${status} must show the action whatever the casing`,
    )
  }
})

test('rejected, approved, pending and other statuses all show the action', () => {
  const statuses = [
    'REJECTED',
    'APPROVED',
    'SUBMITTED',
    'UNDER_REVIEW',
    'DRAFT',
    'RPS_RETURNED',
    'REVISION',
    'RETURNED_TO_RPS',
    'IMPLEMENTATION',
    'RELEASED',
    'COMPLETED',
  ]
  for (const status of statuses) {
    assert.equal(isDuplicable(status), true, `${status} must show the Make a Copy action`)
  }
})

test('a status this module does not know yet does not hide the action', () => {
  assert.equal(isDuplicable('PENDING_APPROVAL'), true)
  assert.equal(isDuplicable('  rejected  '), true)
})

test('a missing status does not hide the action', () => {
  assert.equal(isDuplicable(null), true)
  assert.equal(isDuplicable(undefined), true)
  assert.equal(isDuplicable(''), true)
})

test('a value that cannot be a status is refused', () => {
  assert.equal(isDuplicable(42), false)
  assert.equal(isDuplicable({}), false)
})
