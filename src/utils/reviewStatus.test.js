/**
 * reviewStatus.test.js
 * ---------------------------------------------------------------------------
 * Verifies the status wording and colour tone the proposal review pages show.
 *
 * The status itself is never invented: it is the saved proposal status, translated into the
 * label the header renders and the tone class the design layer styles. Keeping that mapping
 * in one place is what makes every role show the same badge for the same workflow state.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  REVIEW_STATUS_TONES,
  reviewDateLabel,
  reviewStatusLabel,
  reviewStatusTone,
} from './reviewStatus.js'

test('each approval stage keeps its approved colour', () => {
  assert.equal(reviewStatusTone('APPROVED'), 'is-approved', 'green')
  assert.equal(reviewStatusTone('REC_APPROVED'), 'is-indigo', 'REC')
  assert.equal(reviewStatusTone('OVC_APPROVED'), 'is-cyan', 'OVCRIGE')
  assert.equal(reviewStatusTone('ENDORSED'), 'is-cyan', 'RPS → OVCRIGE')
  assert.equal(reviewStatusTone('COMPLETED'), 'is-completed', 'dark green')
})

test('a rejection reads red and a return reads orange', () => {
  for (const status of ['REJECTED', 'REC_REJECTED']) {
    assert.equal(reviewStatusTone(status), 'is-rejected', status)
  }
  for (const status of ['RETURNED', 'RPS_RETURNED', 'REVISION']) {
    assert.equal(reviewStatusTone(status), 'is-orange', status)
  }
})

test('a status that is still being processed uses its review stage colour', () => {
  assert.equal(reviewStatusTone('UNDER_REVIEW'), 'is-purple')
  assert.equal(reviewStatusTone('REC_EVALUATION'), 'is-purple')
  assert.equal(reviewStatusTone('OVCRIGE_COORDINATION'), 'is-cyan')
  assert.equal(reviewStatusTone('CHANCELLOR_REVIEW'), 'is-purple')
  assert.equal(reviewStatusTone('IMPLEMENTATION'), 'is-approved')
})

test('a missing status is a draft', () => {
  assert.equal(reviewStatusTone(''), 'is-draft')
  assert.equal(reviewStatusTone(null), 'is-draft')
  assert.equal(reviewStatusTone(undefined), 'is-draft')
  assert.equal(reviewStatusLabel(''), 'Draft')
})

test('the label spells the saved status canonically', () => {
  assert.equal(reviewStatusLabel('UNDER_REVIEW'), 'Under Review by REC')
  assert.equal(reviewStatusLabel('REC_APPROVED'), 'Approved by REC')
  assert.equal(reviewStatusLabel('DRAFT'), 'Draft')
})

test('both submission spellings read "For RPS Review"', () => {
  assert.equal(reviewStatusLabel('FOR REVIEW'), 'For RPS Review')
  assert.equal(reviewStatusLabel('for review'), 'For RPS Review')
  assert.equal(reviewStatusLabel('SUBMITTED'), 'For RPS Review')
  assert.equal(reviewStatusTone('FOR REVIEW'), 'is-blue')
  assert.equal(reviewStatusTone('SUBMITTED'), 'is-blue')
})

test('APPROVED and COMPLETED keep their own labels on review pages too', () => {
  assert.equal(reviewStatusLabel('APPROVED'), 'Implementation Approved')
  assert.equal(reviewStatusLabel('approved'), 'Implementation Approved')
  assert.equal(reviewStatusLabel('COMPLETED'), 'Completed')
  assert.equal(reviewStatusLabel('FOR_OC_APPROVAL'), 'For OC Approval')
  assert.equal(reviewStatusLabel('FOR_OVCAF_APPROVAL'), 'For OVCAF Approval')
  assert.equal(reviewStatusTone('APPROVED'), 'is-approved')
})

test('every tone the helper returns is one the design layer styles', () => {
  for (const status of ['APPROVED', 'REJECTED', 'RETURNED', 'DRAFT', 'UNDER_REVIEW']) {
    const tone = reviewStatusTone(status)
    assert.ok(REVIEW_STATUS_TONES.includes(tone), status + ' -> ' + tone)
  }
})

test('a date is shown short, and an unusable one is left out', () => {
  assert.equal(reviewDateLabel('2026-07-29T00:00:00'), 'Jul 29, 2026')
  assert.equal(reviewDateLabel(''), '')
  assert.equal(reviewDateLabel(null), '')
  assert.equal(reviewDateLabel('not a date'), '')
})