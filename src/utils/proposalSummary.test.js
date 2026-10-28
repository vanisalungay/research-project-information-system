/**
 * proposalSummary.test.js
 * Verifies the proposal buckets the Proponent dashboard summarises by.
 *
 * The point of these tests is that the summary can never disagree with the records: every
 * status the application knows belongs to a bucket, a proposal is counted exactly once,
 * and an unknown status is never guessed into the wrong card.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { PROPOSAL_STATUSES } from './proposalStatus.js'
import {
  PROPOSAL_SUMMARY_BUCKETS,
  isEveryStatusBucketed,
  normalizeProposalStatus,
  proposalBucketOf,
  proposalSummaryCounts,
} from './proposalSummary.js'

test('every status the application stores belongs to a summary bucket', () => {
  // The shared vocabulary (utils/proposalStatus.js) is the source of truth: a status added
  // there without a bucket would only show up in Total and silently vanish from the cards.
  assert.equal(
    isEveryStatusBucketed(PROPOSAL_STATUSES),
    true,
    `unmapped statuses: ${PROPOSAL_STATUSES.filter((s) => !proposalBucketOf(s)).join(', ')}`,
  )
})

test('the statuses the workflow pages and the backend use are mapped', () => {
  // Stage keys of the Proposal Monitoring timeline and statuses the backend services write.
  const pipeline = [
    'DRAFT',
    'SUBMITTED',
    'ENDORSED',
    'UNDER_REVIEW',
    'REC_APPROVED',
    'FOR_OC_APPROVAL',
    'FOR_OVCAF_APPROVAL',
    'READY_FOR_RELEASE',
    'APPROVED',
    'RELEASED',
    'COMPLETED',
    'RPS_RETURNED',
    'REC_REVISION',
    'RETURNED',
    'RETURNED_TO_RPS',
    'REC_REJECTED',
    'REJECTED',
  ]
  assert.equal(isEveryStatusBucketed(pipeline), true)

  assert.equal(proposalBucketOf('DRAFT'), 'draft')
  assert.equal(proposalBucketOf('SUBMITTED'), 'submitted')
  assert.equal(proposalBucketOf('ENDORSED'), 'underReview')
  assert.equal(proposalBucketOf('FOR_OVCAF_APPROVAL'), 'underReview')
  assert.equal(proposalBucketOf('APPROVED'), 'approved')
  assert.equal(proposalBucketOf('COMPLETED'), 'approved')
  assert.equal(proposalBucketOf('RPS_RETURNED'), 'returned')
  assert.equal(proposalBucketOf('REJECTED'), 'returned')
})

test('a status is matched case-insensitively and an unknown one is never guessed', () => {
  assert.equal(normalizeProposalStatus(' approved '), 'APPROVED')
  assert.equal(proposalBucketOf('approved'), 'approved')
  assert.equal(proposalBucketOf(''), '')
  assert.equal(proposalBucketOf(null), '')
  assert.equal(proposalBucketOf(undefined), '')
  assert.equal(proposalBucketOf(42), '')
  assert.equal(proposalBucketOf('SOMETHING_NEW'), '')
})

test('the counts add up and every proposal is counted once', () => {
  const proposals = [
    { status: 'DRAFT' },
    { status: 'DRAFT' },
    { status: 'SUBMITTED' },
    { status: 'ENDORSED' },
    { status: 'UNDER_REVIEW' },
    { status: 'APPROVED' },
    { status: 'RELEASED' },
    { status: 'RPS_RETURNED' },
    { status: 'REJECTED' },
    { status: 'SOMETHING_NEW' },
  ]
  const counts = proposalSummaryCounts(proposals)

  assert.deepEqual(counts, {
    total: 10,
    draft: 2,
    submitted: 1,
    underReview: 2,
    approved: 2,
    returned: 2,
  })
  // 9 of the 10 belong to a bucket; the unknown one is counted only in Total.
  const bucketed =
    counts.draft + counts.submitted + counts.underReview + counts.approved + counts.returned
  assert.equal(bucketed, 9)
})

test('an empty or odd response reports zeros instead of failing', () => {
  const empty = { total: 0, draft: 0, submitted: 0, underReview: 0, approved: 0, returned: 0 }
  assert.deepEqual(proposalSummaryCounts([]), empty)
  assert.deepEqual(proposalSummaryCounts(null), empty)
  assert.deepEqual(proposalSummaryCounts(undefined), empty)
  assert.deepEqual(proposalSummaryCounts({}), empty)
  // An entry without a status is still a record, so it counts in Total only.
  assert.equal(proposalSummaryCounts([{}, null]).total, 2)
})

test('the cards are declared in display order with their accent and icon', () => {
  assert.deepEqual(
    PROPOSAL_SUMMARY_BUCKETS.map((bucket) => bucket.id),
    ['total', 'draft', 'submitted', 'underReview', 'approved', 'returned'],
  )
  // One restrained accent per meaning, and every card carries an icon.
  assert.deepEqual(
    PROPOSAL_SUMMARY_BUCKETS.map((bucket) => bucket.accent),
    ['indigo', 'blue', 'violet', 'amber', 'green', 'red'],
  )
  for (const bucket of PROPOSAL_SUMMARY_BUCKETS) {
    assert.ok(bucket.label.length > 0, `${bucket.id} must have a label`)
    assert.ok(bucket.icon.length > 0, `${bucket.id} must have an icon`)
  }
})
