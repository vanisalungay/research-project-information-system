/**
 * rejectionQueue.test.js
 * ---------------------------------------------------------------------------
 * The ORPS rejected-queue read model: a rejection hand-off, a finalized rejection and
 * an ordinary revision return must never be confused, and the office shown to ORPS is
 * real data, never a guess.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  isFinalRejection,
  isPendingRejection,
  isRejectedProposal,
  matchesRejectionStatus,
  rejectionOfficeOf,
  rejectionOfficesOf,
  rejectionRemarksOf,
} from './rejectionQueue.js'

const handoff = {
  id: 1,
  status: 'RETURNED_TO_RPS',
  returnKind: 'REJECTION',
  returnedByOffice: 'REC',
  returnRemarks: 'The methodology is incomplete.',
}

test('a rejection hand-off is pending, a finalized rejection is still in the queue', () => {
  assert.equal(isPendingRejection(handoff), true)
  assert.equal(isRejectedProposal(handoff), true)
  assert.equal(isPendingRejection({ status: 'REJECTED', returnedByOffice: 'REC' }), false)
  assert.equal(isRejectedProposal({ status: 'REJECTED', returnedByOffice: 'REC' }), true)
  assert.equal(isFinalRejection({ status: 'REJECTED' }), true)
  assert.equal(isFinalRejection(handoff), false)
})

test('an ordinary revision return is not a rejection', () => {
  const revision = {
    status: 'RETURNED_TO_RPS',
    returnKind: 'REVISION',
    returnedByOffice: 'OVCRIGE',
  }
  assert.equal(isPendingRejection(revision), false)
  assert.equal(isRejectedProposal(revision), false)
})

test('the three status filters match the stored record, never the label', () => {
  const final = { status: 'REJECTED', returnedByOffice: 'REC' }
  const revision = { status: 'RETURNED_TO_RPS', returnKind: 'REVISION' }
  assert.equal(matchesRejectionStatus(handoff, 'ALL'), true)
  assert.equal(matchesRejectionStatus(final, 'ALL'), true)
  assert.equal(matchesRejectionStatus(revision, 'ALL'), false)
  assert.equal(matchesRejectionStatus(handoff, 'RETURNED'), true)
  assert.equal(matchesRejectionStatus(final, 'RETURNED'), false)
  assert.equal(matchesRejectionStatus(revision, 'RETURNED'), false)
  assert.equal(matchesRejectionStatus(handoff, 'REJECTED'), false)
  assert.equal(matchesRejectionStatus(final, 'REJECTED'), true)
  assert.equal(matchesRejectionStatus(handoff, ''), true, 'no filter behaves like All')
})

test('the rejecting office comes from the stored row, with ORPS as the only fallback', () => {
  assert.equal(rejectionOfficeOf(handoff), 'REC')
  assert.equal(rejectionOfficeOf({ status: 'REJECTED' }), 'ORPS')
  assert.equal(rejectionOfficeOf({ status: 'REJECTED', returnedByOffice: ' ovcaf ' }), 'OVCAF')
})

test('the reason is capped for the table without dropping the stored value', () => {
  assert.equal(rejectionRemarksOf(handoff), 'The methodology is incomplete.')
  assert.equal(rejectionRemarksOf({}), '')
  const long = 'x'.repeat(300)
  assert.equal(rejectionRemarksOf({ remarks: long }).length, 221)
})

test('the office filter is built from rejected rows only, alphabetically', () => {
  const rows = [
    handoff,
    { status: 'REJECTED', returnedByOffice: 'OVCAF' },
    { status: 'RETURNED_TO_RPS', returnKind: 'REVISION', returnedByOffice: 'OVCRIGE' },
    { status: 'REJECTED' },
  ]
  assert.deepEqual(rejectionOfficesOf(rows), ['ORPS', 'OVCAF', 'REC'])
  assert.deepEqual(rejectionOfficesOf(null), [])
})
