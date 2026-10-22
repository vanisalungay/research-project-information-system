/**
 * quarterlyReport.test.js
 * Verifies the quarterly-reporting eligibility rule the proponent interface uses
 * to offer the Create Quarterly Report action: approved for implementation
 * (APPROVED / RELEASED) AND an issued Special Order (SO). Approval alone, or an
 * SO on a project that is not approved for implementation, must not qualify -
 * this mirrors the backend's authoritative check.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  IMPLEMENTATION_STATUSES,
  hasIssuedSpecialOrder,
  isApprovedForImplementation,
  isQuarterlyReportEligible,
} from './quarterlyReport.js'

test('the implementation statuses match the backend rule', () => {
  assert.deepEqual(IMPLEMENTATION_STATUSES, ['APPROVED', 'RELEASED'])
})

test('an approved project with an issued SO is eligible', () => {
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: 'SO-2026-001' }), true)
  assert.equal(isQuarterlyReportEligible({ status: 'RELEASED', soNumber: 'SO-2026-014' }), true)
})

test('approval without an issued SO is not eligible', () => {
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: '' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'RELEASED', soNumber: '   ' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: null }), false)
})

test('an issued SO on a project that is not approved for implementation is not eligible', () => {
  const statuses = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'REC_APPROVED', 'REJECTED', 'COMPLETED']
  for (const status of statuses) {
    assert.equal(isQuarterlyReportEligible({ status, soNumber: 'SO-2026-001' }), false, status)
  }
})

test('a missing proposal is never eligible', () => {
  assert.equal(isQuarterlyReportEligible(null), false)
  assert.equal(isQuarterlyReportEligible(undefined), false)
  assert.equal(isQuarterlyReportEligible({}), false)
})

test('the individual checks report what they measure', () => {
  assert.equal(hasIssuedSpecialOrder({ soNumber: 'SO-2026-001' }), true)
  assert.equal(hasIssuedSpecialOrder({ soNumber: '  ' }), false)
  assert.equal(hasIssuedSpecialOrder({ soNumber: 42 }), false)
  assert.equal(hasIssuedSpecialOrder(null), false)

  assert.equal(isApprovedForImplementation({ status: 'APPROVED' }), true)
  assert.equal(isApprovedForImplementation({ status: 'RELEASED' }), true)
  assert.equal(isApprovedForImplementation({ status: 'approved' }), false)
  assert.equal(isApprovedForImplementation({}), false)
})
