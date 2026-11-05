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

test('a finished or successful status reads green', () => {
  for (const status of ['APPROVED', 'REC_APPROVED', 'ENDORSED', 'COMPLETED']) {
    assert.equal(reviewStatusTone(status), 'is-approved', status)
  }
})

test('a rejection reads red and a return reads amber', () => {
  for (const status of ['REJECTED', 'REC_REJECTED']) {
    assert.equal(reviewStatusTone(status), 'is-rejected', status)
  }
  for (const status of ['RETURNED', 'RPS_RETURNED', 'REVISION']) {
    assert.equal(reviewStatusTone(status), 'is-returned', status)
  }
})

test('a status that is still being processed keeps the default badge', () => {
  for (const status of [
    'UNDER_REVIEW',
    'REC_EVALUATION',
    'OVCRIGE_COORDINATION',
    'CHANCELLOR_REVIEW',
    'IMPLEMENTATION',
  ]) {
    assert.equal(reviewStatusTone(status), '', status)
  }
})

test('a missing status is a draft', () => {
  assert.equal(reviewStatusTone(''), 'is-draft')
  assert.equal(reviewStatusTone(null), 'is-draft')
  assert.equal(reviewStatusTone(undefined), 'is-draft')
  assert.equal(reviewStatusLabel(''), 'DRAFT')
})

test('the label spells the saved status the way the workflow does', () => {
  assert.equal(reviewStatusLabel('UNDER_REVIEW'), 'UNDER REVIEW')
  assert.equal(reviewStatusLabel('REC_APPROVED'), 'REC APPROVED')
  assert.equal(reviewStatusLabel('DRAFT'), 'DRAFT')
})

test('every tone the helper returns is one the design layer styles', () => {
  for (const status of ['APPROVED', 'REJECTED', 'RETURNED', 'DRAFT', 'UNDER_REVIEW']) {
    const tone = reviewStatusTone(status)
    assert.ok(tone === '' || REVIEW_STATUS_TONES.includes(tone), status + ' -> ' + tone)
  }
})

test('a date is shown short, and an unusable one is left out', () => {
  assert.equal(reviewDateLabel('2026-07-29T00:00:00'), 'Jul 29, 2026')
  assert.equal(reviewDateLabel(''), '')
  assert.equal(reviewDateLabel(null), '')
  assert.equal(reviewDateLabel('not a date'), '')
})