/**
 * ovcrigeProposals.test.js
 * ---------------------------------------------------------------------------
 * Pins down which proposals belong to the OVCRIGE module, so the dashboard preview and
 * the two OVCRIGE lists cannot drift apart.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  OVCRIGE_DASHBOARD_FETCH_STATUSES,
  OVCRIGE_OC_ENDORSEMENT_STATUSES,
  OVCRIGE_PIPELINE_STATUSES,
  isOvcrigePipelineProposal,
  proposalSubmittedAt,
} from './ovcrigeProposals.js'

test('the pipeline holds every status from RPS endorsement through OC forwarding', () => {
  assert.deepEqual(OVCRIGE_PIPELINE_STATUSES, [
    'ENDORSED',
    'UNDER_REVIEW',
    'REC_APPROVED',
    'OVC_APPROVED',
    'FOR_OVCAF_APPROVAL',
    'FOR_OC_APPROVAL',
  ])
})

test('not-yet-endorsed and finished proposals are not in the pipeline', () => {
  for (const status of ['DRAFT', 'FOR REVIEW', 'SUBMITTED', 'APPROVED', 'COMPLETED']) {
    assert.equal(isOvcrigePipelineProposal({ status }), false, status)
  }
})

test('the pipeline check normalizes the stored status', () => {
  assert.equal(isOvcrigePipelineProposal({ status: ' endorsed ' }), true)
  assert.equal(isOvcrigePipelineProposal({ status: 'rec_approved' }), true)
  assert.equal(isOvcrigePipelineProposal({ status: null }), false)
  assert.equal(isOvcrigePipelineProposal(null), false)
})

test('the dashboard request is the pipeline plus the two returned statuses it counts', () => {
  for (const status of OVCRIGE_PIPELINE_STATUSES) {
    assert.ok(OVCRIGE_DASHBOARD_FETCH_STATUSES.includes(status), status)
  }
  assert.ok(OVCRIGE_DASHBOARD_FETCH_STATUSES.includes('REVISION'))
  assert.ok(OVCRIGE_DASHBOARD_FETCH_STATUSES.includes('REC_REVISION'))
})

test('the OC endorsement list is the REC-recommended subset', () => {
  assert.deepEqual(OVCRIGE_OC_ENDORSEMENT_STATUSES, [
    'REC_APPROVED',
    'OVC_APPROVED',
    'FOR_OC_APPROVAL',
  ])
  for (const status of OVCRIGE_OC_ENDORSEMENT_STATUSES) {
    assert.ok(OVCRIGE_PIPELINE_STATUSES.includes(status), status)
  }
})

test('the displayed date is the submission event, never the last edit', () => {
  assert.equal(
    proposalSubmittedAt({
      submittedAt: '2026-11-10T10:00:00',
      createdAt: '2026-11-01T10:00:00',
      updatedAt: '2026-11-20T10:00:00',
    }),
    '2026-11-10T10:00:00',
  )
  assert.equal(proposalSubmittedAt({ createdAt: '2026-11-01T10:00:00', updatedAt: '2026-11-20T10:00:00' }), '2026-11-01T10:00:00')
  assert.equal(proposalSubmittedAt({}), null)
  assert.equal(proposalSubmittedAt(null), null)
})
