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
  APPROVED,
  COPYABLE_STATUSES,
  FOR_REVIEW,
  LEGACY_SUBMITTED,
  PROPOSAL_STATUSES,
  PROJECT_STATUSES,
  canonicalProposalStatus,
  isApprovedProject,
  isCompletedStatus,
  isDuplicable,
  isForReviewStatus,
  normalizeProposalStatus,
  proposalActionLabel,
  proposalStatusLabel,
  proposalStatusMatches,
} from './proposalStatus.js'
import { PROPOSAL_STATUS_LABELS } from './statusCatalog.js'

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

test('both submission spellings read "For RPS Review"; other labels are canonical', () => {
  assert.equal(proposalStatusLabel('FOR REVIEW'), 'For RPS Review')
  assert.equal(proposalStatusLabel('for review'), 'For RPS Review')
  assert.equal(proposalStatusLabel('SUBMITTED'), 'For RPS Review')
  assert.equal(proposalStatusLabel('DRAFT'), 'Draft')
  assert.equal(proposalStatusLabel('ENDORSED'), 'Endorsed to OVCRIGE')
  assert.equal(proposalStatusLabel('UNDER_REVIEW'), 'Under Review by REC')
  assert.equal(proposalStatusLabel('RPS_RETURNED'), 'Returned to Proponent')
  assert.equal(proposalStatusLabel('RETURNED_TO_RPS'), 'Returned to RPS')
  assert.equal(proposalStatusLabel('RETURNED'), 'Returned for Revision')
  assert.equal(proposalStatusLabel(''), '')
  assert.equal(normalizeProposalStatus(' approved '), 'APPROVED')
})

test('final approval reads "Implementation Approved", never "Completed"', () => {
  for (const status of ['APPROVED', 'approved', '  Approved  ']) {
    assert.equal(proposalStatusLabel(status), 'Implementation Approved', status)
  }
  assert.equal(proposalStatusLabel('COMPLETED'), 'Completed')
  assert.equal(proposalStatusLabel('FOR_OC_APPROVAL'), 'For OC Approval')
  assert.equal(proposalStatusLabel('FOR_OVCAF_APPROVAL'), 'For OVCAF Approval')
  assert.equal(proposalStatusLabel('REC_APPROVED'), 'Approved by REC')
  assert.equal(proposalStatusLabel('OVC_APPROVED'), 'Approved by OVCRIGE')
  assert.equal(proposalStatusLabel('READY_FOR_RELEASE'), 'Ready for Fund Release')
  assert.equal(proposalStatusLabel('RELEASED'), 'Released')

  // APPROVED and COMPLETED are separate states and must never share a label.
  assert.notEqual(proposalStatusLabel('APPROVED'), proposalStatusLabel('COMPLETED'))
})

test('OVCAF decision values are not proposal statuses', () => {
  // The decision is stored on OvcafValidation; the proposal it produces reads APPROVED
  // or RETURNED. The proposal label map must therefore not claim either decision value.
  assert.equal(PROPOSAL_STATUS_LABELS.APPROVED_ENDORSED, undefined)
  assert.equal(PROPOSAL_STATUS_LABELS.RETURNED_FOR_REVISION, undefined)
  const label = proposalStatusLabel('APPROVED_ENDORSED')
  assert.notEqual(label, proposalStatusLabel('APPROVED'))
  assert.notEqual(label, 'Approved')
})

test('APPROVED and COMPLETED are distinct in filters and completion checks', () => {
  assert.equal(proposalStatusMatches('APPROVED', 'APPROVED'), true)
  assert.equal(proposalStatusMatches('COMPLETED', 'COMPLETED'), true)
  assert.equal(proposalStatusMatches('COMPLETED', 'APPROVED'), false)
  assert.equal(proposalStatusMatches('APPROVED', 'COMPLETED'), false)
  for (const status of [
    'READY_FOR_RELEASE',
    'FOR_OC_APPROVAL',
    'FOR_OVCAF_APPROVAL',
    'REC_APPROVED',
  ]) {
    assert.equal(proposalStatusMatches(status, 'APPROVED'), false, status)
    assert.equal(proposalStatusMatches(status, 'COMPLETED'), false, status)
  }
  assert.equal(proposalStatusMatches('COMPLETED', 'READY_FOR_RELEASE'), false)

  assert.equal(isCompletedStatus('COMPLETED'), true)
  assert.equal(isCompletedStatus('APPROVED'), false)
  assert.equal(isCompletedStatus('READY_FOR_RELEASE'), false)
  assert.equal(isCompletedStatus('RELEASED'), false)
})

test('the Open row action triggers only at final approval or later', () => {
  assert.equal(APPROVED, 'APPROVED')
  for (const status of PROJECT_STATUSES) {
    assert.equal(isApprovedProject(status), true, status)
    assert.equal(proposalActionLabel(status), 'Open', status)
  }
  for (const status of [
    'DRAFT',
    'FOR REVIEW',
    'SUBMITTED',
    'ENDORSED',
    'UNDER_REVIEW',
    'REC_APPROVED',
    'FOR_OVCAF_APPROVAL',
    'FOR_OC_APPROVAL',
    'RETURNED',
    'RPS_RETURNED',
    'REJECTED',
    'REC_REJECTED',
    'REC_REVISION',
    'REVISION',
    '',
    null,
    undefined,
  ]) {
    assert.equal(isApprovedProject(status), false, String(status))
    assert.equal(proposalActionLabel(status), 'View', String(status))
  }
})
