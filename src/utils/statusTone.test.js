/**
 * statusTone.test.js
 * ---------------------------------------------------------------------------
 * Verifies the one status -> colour tone mapping every role and module uses.
 *
 * The approved palette is the contract: every real RPIS status is asserted here, plus the
 * normalisation, the aliases and the safe fallback. A value that matches nothing must read
 * gray, never green, so an unknown status can never look approved or completed.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { STATUS_TONES, normalizeStatus, statusTone } from './statusTone.js'
import { reviewStatusTone } from './reviewStatus.js'
import { endorsementStatusTone } from './endorsement.js'
import { announcementStatusTone } from './announcementStatus.js'

test('a submission or schedule reads blue', () => {
  for (const status of ['FOR REVIEW', 'FOR_REVIEW', 'for review', 'SUBMITTED', 'SCHEDULED']) {
    assert.equal(statusTone(status), 'is-blue', status)
  }
})

test('each workflow stage keeps its approved colour', () => {
  // ENDORSED is the RPS -> OVCRIGE forwarding stage: it shares the OVCRIGE office cyan.
  assert.equal(statusTone('ENDORSED'), 'is-cyan', 'RPS endorsed to OVCRIGE')
  assert.equal(statusTone('UNDER_REVIEW'), 'is-purple', 'REC under review')
  assert.equal(statusTone('REC_APPROVED'), 'is-indigo', 'REC approval')
  assert.equal(statusTone('OVC_APPROVED'), 'is-cyan', 'OVCRIGE approval')
  assert.equal(statusTone('APPROVED'), 'is-approved', 'Implementation Approved')
  assert.equal(statusTone('COMPLETED'), 'is-completed', 'Completed')
  assert.equal(statusTone('RELEASED'), 'is-released', 'funds released')
})

test('Implementation Approved and Completed use distinguishable greens', () => {
  assert.equal(statusTone('APPROVED'), 'is-approved')
  assert.equal(statusTone('COMPLETED'), 'is-completed')
  assert.notEqual(statusTone('APPROVED'), statusTone('COMPLETED'))
})

test('every returned or revision status is orange', () => {
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
  ]) {
    assert.equal(statusTone(status), 'is-orange', status)
  }
})

test('Ready for Fund Release is orange, never green', () => {
  assert.equal(statusTone('READY_FOR_RELEASE'), 'is-orange')
  assert.notEqual(statusTone('READY_FOR_RELEASE'), statusTone('APPROVED'))
})

test('every rejection or failure is red', () => {
  for (const status of [
    'REJECTED',
    'REJECTED_BY_REC',
    'REC_REJECTED',
    'FAILED',
    'ERROR',
  ]) {
    assert.equal(statusTone(status), 'is-rejected', status)
  }
})

test('a pending item is amber, an under-review item purple', () => {
  for (const status of ['PENDING', 'PENDING_VALIDATION', 'PENDING_APPROVAL', 'WAITING']) {
    assert.equal(statusTone(status), 'is-amber', status)
  }
  for (const status of ['UNDER_REVIEW', 'REC_EVALUATION', 'CHANCELLOR_REVIEW']) {
    assert.equal(statusTone(status), 'is-purple', status)
  }
})

test('a status still moving through the pipeline reads blue', () => {
  for (const status of ['RPS_REVIEW', 'UNDER_VALIDATION', 'ONGOING', 'INFO']) {
    assert.equal(statusTone(status), 'is-blue', status)
  }
  // The OVCRIGE stage keeps the cyan of the office that owns it.
  assert.equal(statusTone('OVCRIGE_COORDINATION'), 'is-cyan')
})

test('a call for proposal keeps its approved colours', () => {
  assert.equal(statusTone('OPEN'), 'is-approved')
  assert.equal(statusTone('CLOSED'), 'is-gray')
  assert.equal(statusTone('SCHEDULED'), 'is-blue')
  assert.equal(statusTone('DRAFT'), 'is-gray')
})

test('a draft is gray and an unknown status falls back to gray, never green', () => {
  assert.equal(statusTone('DRAFT'), 'is-gray')
  assert.equal(statusTone('INACTIVE'), 'is-gray')
  assert.equal(statusTone('REVOKED'), 'is-gray')
  assert.equal(statusTone('SUPERSEDED'), 'is-gray')
  assert.equal(statusTone('SOMETHING_NEW'), 'is-gray')
  assert.equal(statusTone(''), 'is-gray')
  assert.equal(statusTone(null), 'is-gray')
  assert.equal(statusTone(undefined), 'is-gray')
})

test('a plain active/inactive flag reads green/gray', () => {
  assert.equal(statusTone('ACTIVE'), 'is-approved')
  assert.equal(statusTone('active'), 'is-approved')
  assert.equal(statusTone('INACTIVE'), 'is-gray')
  assert.equal(statusTone('inactive'), 'is-gray')
})

test('the status is normalised before it is matched', () => {
  assert.equal(normalizeStatus(' submitted '), 'SUBMITTED')
  assert.equal(normalizeStatus(null), '')
  assert.equal(statusTone(' submitted '), 'is-blue')
  assert.equal(statusTone('Pending Validation'), 'is-amber')
  assert.equal(statusTone('Under Review'), 'is-purple')
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
  // The review pages now follow the approved palette for every status, including the
  // in-progress stages that used to keep the badge's own fallback.
  assert.equal(reviewStatusTone('UNDER_REVIEW'), 'is-purple')
  assert.equal(reviewStatusTone('FOR REVIEW'), 'is-blue')
  assert.equal(reviewStatusTone('SUBMITTED'), 'is-blue')
  assert.equal(reviewStatusTone('RETURNED'), 'is-orange')
  assert.equal(reviewStatusTone('REJECTED'), 'is-rejected')

  // The endorsement workflow keeps exactly its three business states.
  assert.equal(endorsementStatusTone('PENDING'), 'is-amber')
  assert.equal(endorsementStatusTone('APPROVED'), 'is-approved')
  assert.equal(endorsementStatusTone('REJECTED'), 'is-rejected')

  // Announcements keep their own four states, painted from the same palette.
  assert.equal(announcementStatusTone('DRAFT'), 'is-gray')
  assert.equal(announcementStatusTone('SCHEDULED'), 'is-blue')
  assert.equal(announcementStatusTone('OPEN'), 'is-approved')
  assert.equal(announcementStatusTone('CLOSED'), 'is-gray')
})
