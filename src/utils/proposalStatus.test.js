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

import {
  COPYABLE_STATUSES,
  FOR_REVIEW,
  LEGACY_SUBMITTED,
  PROPOSAL_STATUSES,
  canonicalProposalStatus,
  isDuplicable,
  isForReviewStatus,
  normalizeProposalStatus,
  proposalStatusLabel,
  proposalStatusMatches,
} from './proposalStatus.js'

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

test('both submission spellings are the same workflow state', () => {
  assert.equal(FOR_REVIEW, 'FOR REVIEW')
  assert.equal(LEGACY_SUBMITTED, 'SUBMITTED')
  assert.ok(PROPOSAL_STATUSES.includes(FOR_REVIEW))
  assert.ok(PROPOSAL_STATUSES.includes(LEGACY_SUBMITTED))

  for (const status of ['FOR REVIEW', 'for review', ' For Review ', 'SUBMITTED', 'submitted']) {
    assert.equal(isForReviewStatus(status), true, status)
    assert.equal(canonicalProposalStatus(status), FOR_REVIEW, status)
  }
  for (const status of ['DRAFT', 'RPS_RETURNED', 'SUBMITTED_FOR_REVIEW', '', null, undefined]) {
    assert.equal(isForReviewStatus(status), false, String(status))
  }
  assert.equal(canonicalProposalStatus('rps_returned'), 'RPS_RETURNED')
  assert.equal(canonicalProposalStatus(null), '')
})

test('a filter for either spelling matches both, and other filters stay exact', () => {
  assert.equal(proposalStatusMatches('SUBMITTED', 'FOR REVIEW'), true)
  assert.equal(proposalStatusMatches('FOR REVIEW', 'SUBMITTED'), true)
  assert.equal(proposalStatusMatches('for review', 'for review'), true)
  assert.equal(proposalStatusMatches('FOR REVIEW', ''), true, 'an empty filter matches every row')
  assert.equal(proposalStatusMatches('DRAFT', 'FOR REVIEW'), false)
  assert.equal(proposalStatusMatches('SUBMITTED_FOR_REVIEW', 'SUBMITTED'), false, 'never a substring match')
  assert.equal(proposalStatusMatches('draft', 'DRAFT'), true)
})

test('both submission spellings read "For Review"; other statuses are unchanged', () => {
  assert.equal(proposalStatusLabel('FOR REVIEW'), 'For Review')
  assert.equal(proposalStatusLabel('for review'), 'For Review')
  assert.equal(proposalStatusLabel('SUBMITTED'), 'For Review')
  assert.equal(proposalStatusLabel('DRAFT'), 'Draft')
  assert.equal(proposalStatusLabel('RPS_RETURNED'), 'RPS Returned')
  assert.equal(proposalStatusLabel(''), '')
  assert.equal(normalizeProposalStatus(' approved '), 'APPROVED')
})
