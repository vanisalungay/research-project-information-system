/**
 * quarterlyReport.test.js
 * Verifies the quarterly-reporting eligibility rule the proponent interface uses
 * to offer the Create Quarterly Report action: approved for implementation
 * (APPROVED / RELEASED / IMPLEMENTATION) AND the Office of the Chancellor's
 * recorded Special Order approval (SO number + recorded time). Approval alone, a
 * bare SO number, or an SO on a project that is not approved for implementation
 * must not qualify - this mirrors the backend's authoritative check.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  IMPLEMENTATION_STATUSES,
  hasIssuedSpecialOrder,
  hasRecordedOcApproval,
  isApprovedForImplementation,
  isQuarterlyReportEligible,
  nextQuarterlyReportPath,
} from './quarterlyReport.js'

/** An SO as the OC final-approval transaction records it: number + recorded time. */
const recordedSo = (soNumber) => ({ soNumber, soUploadedAt: '2026-07-28T23:20:42' })

test('the implementation statuses match the backend rule', () => {
  assert.deepEqual(IMPLEMENTATION_STATUSES, ['APPROVED', 'RELEASED', 'IMPLEMENTATION'])
})

test('an approved project with a recorded OC SO approval is eligible', () => {
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', ...recordedSo('SO-2026-001') }), true)
  assert.equal(isQuarterlyReportEligible({ status: 'RELEASED', ...recordedSo('SO-2026-014') }), true)
  assert.equal(isQuarterlyReportEligible({ status: 'IMPLEMENTATION', ...recordedSo('SO-2026-020') }), true,
    'IMPLEMENTATION is the implementation-phase status the rest of the UI treats as approved')
})

test('the status comparison tolerates casing and surrounding whitespace', () => {
  assert.equal(isQuarterlyReportEligible({ status: ' approved ', ...recordedSo('SO-2026-001') }), true)
  assert.equal(isQuarterlyReportEligible({ status: 'Released', ...recordedSo('SO-2026-014') }), true)
  assert.equal(isQuarterlyReportEligible({ status: 'implementation', ...recordedSo('SO-2026-020') }), true)
  assert.equal(isQuarterlyReportEligible({ status: '   ', ...recordedSo('SO-2026-001') }), false)
})

test('approval without an issued SO is not eligible', () => {
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: '' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'RELEASED', soNumber: '   ' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: null }), false)
})

test('an SO number without the recorded OC approval time is not eligible', () => {
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: 'SO-2026-001' }), false,
    'a number entered without the OC approval record is not evidence of an issued SO')
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: 'SO-2026-001', soUploadedAt: '   ' }), false)
  assert.equal(isQuarterlyReportEligible({ status: 'APPROVED', soNumber: 'SO-2026-001', soUploadedAt: null }), false)
})

test('an issued SO on a project that is not approved for implementation is not eligible', () => {
  const statuses = ['DRAFT', 'SUBMITTED', 'UNDER_REVIEW', 'REC_APPROVED', 'REJECTED', 'COMPLETED']
  for (const status of statuses) {
    assert.equal(isQuarterlyReportEligible({ status, ...recordedSo('SO-2026-001') }), false, status)
  }
})

test('a missing proposal is never eligible', () => {
  assert.equal(isQuarterlyReportEligible(null), false)
  assert.equal(isQuarterlyReportEligible(undefined), false)
  assert.equal(isQuarterlyReportEligible({}), false)
})

test('the next-stage response opens the existing report form', () => {
  assert.equal(
    nextQuarterlyReportPath(47, { reportType: 'QUARTERLY_PROGRESS', period: 'Q1' }),
    '/quarterly-reports/47/new',
    'a new stage opens the creation form',
  )
  assert.equal(
    nextQuarterlyReportPath(47, { period: 'Q2', draftReportId: 21 }),
    '/quarterly-reports/47/edit/21',
    'an unfinished draft is continued instead of duplicated',
  )
})

test('a completed project has no form to open', () => {
  assert.equal(nextQuarterlyReportPath(47, { allStagesComplete: true }), '')
  assert.equal(nextQuarterlyReportPath(47), '')
  assert.equal(nextQuarterlyReportPath(null, { period: 'Q1' }), '')
})

test('the individual checks report what they measure', () => {
  assert.equal(hasIssuedSpecialOrder({ soNumber: 'SO-2026-001' }), true)
  assert.equal(hasIssuedSpecialOrder({ soNumber: '  ' }), false)
  assert.equal(hasIssuedSpecialOrder({ soNumber: 42 }), false)
  assert.equal(hasIssuedSpecialOrder(null), false)

  assert.equal(hasRecordedOcApproval({ soNumber: 'SO-2026-001', soUploadedAt: '2026-07-28T23:20:42' }), true)
  assert.equal(hasRecordedOcApproval({ soNumber: 'SO-2026-001', soUploadedAt: [2026, 7, 28, 23, 20] }), true,
    'a timestamp serialized as an array is still a recorded time')
  assert.equal(hasRecordedOcApproval({ soNumber: 'SO-2026-001' }), false)
  assert.equal(hasRecordedOcApproval({ soUploadedAt: '2026-07-28T23:20:42' }), false)
  assert.equal(hasRecordedOcApproval(null), false)

  assert.equal(isApprovedForImplementation({ status: 'APPROVED' }), true)
  assert.equal(isApprovedForImplementation({ status: 'RELEASED' }), true)
  assert.equal(isApprovedForImplementation({ status: 'IMPLEMENTATION' }), true)
  assert.equal(isApprovedForImplementation({ status: 'approved' }), true,
    'the status is normalised before comparison, as the backend does')
  assert.equal(isApprovedForImplementation({ status: 'FOR_OVCAF_APPROVAL' }), false)
  assert.equal(isApprovedForImplementation({}), false)
})
