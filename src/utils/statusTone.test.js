/**
 * statusTone.test.js
 * ---------------------------------------------------------------------------
 * Verifies the one status -> colour tone mapping every role and module uses.
 *
 * The point of the helper is that the same saved status can never be green on one page and
 * amber on another, so the mapping table below is the contract: every real RPIS status is
 * asserted here, plus the normalisation and the fallbacks.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { STATUS_TONES, normalizeStatus, statusTone } from './statusTone.js'
import { reviewStatusTone } from './reviewStatus.js'
import { endorsementStatusTone } from './endorsement.js'
import { announcementStatusTone } from './announcementStatus.js'

test('a finished or successful status is green', () => {
  for (const status of [
    'FOR REVIEW',
    'SUBMITTED',
    'APPROVED',
    'REC_APPROVED',
    'OVC_APPROVED',
    'APPROVED_ENDORSED',
    'ENDORSED',
    'COMPLETED',
    'RELEASED',
    'SUCCESS',
    'OPEN',
  ]) {
    assert.equal(statusTone(status), 'is-approved', status)
  }
})

test('every return or revision status is amber', () => {
  for (const status of [
    'RETURNED',
    'RETURNED_BY_REC',
    'RETURNED_BY_RPS',
    'RETURNED_BY_OVCAF',
    'RETURNED_TO_PROPONENT',
    'RETURNED_TO_RPS',
    'RPS_RETURNED',
    'RETURNED_FOR_REVISION',
    'PENDING_REVISION',
    'REVISION',
    'REC_REVISION',
    'REVOKED',
    'WARNING',
  ]) {
    assert.equal(statusTone(status), 'is-returned', status)
  }
})

test('every rejection status is red', () => {
  for (const status of [
    'REJECTED',
    'REJECTED_BY_REC',
    'REC_REJECTED',
    'FAILED',
    'ERROR',
    'CLOSED',
  ]) {
    assert.equal(statusTone(status), 'is-rejected', status)
  }
})

test('a status still in the workflow is light blue', () => {
  for (const status of [
    'UNDER_REVIEW',
    'RPS_REVIEW',
    'REC_EVALUATION',
    'OVCRIGE_COORDINATION',
    'CHANCELLOR_REVIEW',
    'FINANCE_AUTHORIZATION',
    'FOR_OVCAF_APPROVAL',
    'FOR_OC_APPROVAL',
    'READY_FOR_RELEASE',
    'PENDING',
    'PENDING_VALIDATION',
    'UNDER_VALIDATION',
    'IMPLEMENTATION',
    'ONGOING',
    'WAITING',
    'SCHEDULED',
    'INFO',
  ]) {
    assert.equal(statusTone(status), 'is-pending', status)
  }
})

test('a draft is grey and a neutral or unknown status falls back to grey', () => {
  assert.equal(statusTone('DRAFT'), 'is-draft')
  assert.equal(statusTone('INACTIVE'), 'is-neutral')
  assert.equal(statusTone('SOMETHING_NEW'), 'is-neutral')
  assert.equal(statusTone(''), 'is-neutral')
  assert.equal(statusTone(null), 'is-neutral')
  assert.equal(statusTone(undefined), 'is-neutral')
})

test('a plain active/inactive flag reads green/grey', () => {
  assert.equal(statusTone('ACTIVE'), 'is-approved')
  assert.equal(statusTone('active'), 'is-approved')
  assert.equal(statusTone('INACTIVE'), 'is-neutral')
  assert.equal(statusTone('inactive'), 'is-neutral')
})

test('the status is normalised before it is matched', () => {
  assert.equal(normalizeStatus(' submitted '), 'SUBMITTED')
  assert.equal(normalizeStatus(null), '')
  assert.equal(statusTone(' submitted '), 'is-approved')
  assert.equal(statusTone('Pending Validation'), 'is-pending')
  assert.equal(statusTone('Under Review'), 'is-pending')
})

test('a known status never changes colour when it is written differently', () => {
  assert.equal(statusTone('returned'), statusTone('RETURNED'))
  assert.equal(statusTone('Rejected'), statusTone('REJECTED'))
  assert.equal(statusTone('approved'), statusTone('APPROVED'))
})

test('every value the helper returns is one of the shared tones, and never empty', () => {
  for (const status of ['', null, undefined, 'APPROVED', 'RETURNED', 'REJECTED', 'DRAFT', 'X']) {
    const tone = statusTone(status)
    assert.notEqual(tone, '', String(status))
    assert.ok(STATUS_TONES.includes(tone), `${status} -> ${tone}`)
  }
})

test('the existing public helpers delegate to the shared mapping', () => {
  // The review pages keep no tone class for in-progress statuses; their badge base is the
  // pending treatment. SUBMITTED is now green like every other finished status.
  assert.equal(reviewStatusTone('UNDER_REVIEW'), '')
  assert.equal(reviewStatusTone('FOR REVIEW'), 'is-approved')
  assert.equal(reviewStatusTone('SUBMITTED'), 'is-approved')
  assert.equal(reviewStatusTone('RETURNED'), 'is-returned')
  assert.equal(reviewStatusTone('REJECTED'), 'is-rejected')

  // The endorsement workflow keeps exactly its three business states.
  assert.equal(endorsementStatusTone('PENDING'), 'is-pending')
  assert.equal(endorsementStatusTone('APPROVED'), 'is-approved')
  assert.equal(endorsementStatusTone('REJECTED'), 'is-rejected')

  // Announcements keep their own four states.
  assert.equal(announcementStatusTone('DRAFT'), 'draft')
  assert.equal(announcementStatusTone('SCHEDULED'), 'scheduled')
  assert.equal(announcementStatusTone('OPEN'), 'open')
  assert.equal(announcementStatusTone('CLOSED'), 'closed')
})
