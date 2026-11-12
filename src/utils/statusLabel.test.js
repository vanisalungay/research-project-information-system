/**
 * statusLabel.test.js
 * ---------------------------------------------------------------------------
 * Verifies the generic fallback formatter.
 *
 * Category labels now live in statusCatalog.js and every screen that knows its
 * category must use that category's helper. This formatter is only the title-case
 * fallback for a code with no category entry (and the translation used inside the
 * category helpers). These tests pin the acronyms that must stay uppercase, the
 * minor-word rule, the fallback, and the rule that no proposal status can render
 * with an underscore or in ALL CAPS.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { normalizeStatusKey, statusLabel } from './statusLabel.js'
import { PROPOSAL_STATUSES, proposalStatusLabel } from './proposalStatus.js'

test('formatting a fallback label again leaves it unchanged', () => {
  assert.equal(statusLabel('SOMETHING_NEW'), 'Something New')
  assert.equal(statusLabel('Something New'), 'Something New')
  for (const value of ['Something New', 'RPS Endorse', 'Approved/Released', 'N/A']) {
    assert.equal(statusLabel(statusLabel(value)), statusLabel(value), value)
  }
})

test('the stored spelling is normalized before it is looked up', () => {
  assert.equal(normalizeStatusKey('  for review  '), 'FOR_REVIEW')
  assert.equal(normalizeStatusKey('for-review'), 'FOR_REVIEW')
  assert.equal(normalizeStatusKey('UNDER_REVIEW'), 'UNDER_REVIEW')
  assert.equal(normalizeStatusKey(null), '')
  assert.equal(normalizeStatusKey(undefined), '')
  assert.equal(normalizeStatusKey(''), '')
  assert.equal(normalizeStatusKey({}), '')
  assert.equal(normalizeStatusKey(() => {}), '')
})

test('institutional acronyms stay uppercase', () => {
  assert.equal(statusLabel('REC_APPROVED'), 'REC Approved')
  assert.equal(statusLabel('FOR_OVCAF_APPROVAL'), 'For OVCAF Approval')
  assert.equal(statusLabel('FOR_OC_APPROVAL'), 'For OC Approval')
  assert.equal(statusLabel('RPS_RETURNED'), 'RPS Returned')
  assert.equal(statusLabel('RETURNED_TO_RPS'), 'Returned to RPS')
  assert.equal(statusLabel('OVCRIGE_COORDINATION'), 'OVCRIGE Coordination')
  assert.equal(statusLabel('ORPS'), 'ORPS')
  assert.equal(statusLabel('GAD'), 'GAD')
})

test('the fallback is generic: it never applies a category meaning', () => {
  // SUBMITTED is a proposal state ("For RPS Review") and a report state ("Submitted").
  // The fallback must not pick either; each screen uses its category helper.
  assert.equal(statusLabel('SUBMITTED'), 'Submitted')
  assert.equal(proposalStatusLabel('SUBMITTED'), 'For RPS Review')
  assert.equal(proposalStatusLabel('FOR REVIEW'), 'For RPS Review')

  // APPROVED means an account is active, a proposal is approved for implementation
  // and a decision is approved. The fallback can only title-case the token.
  assert.equal(statusLabel('APPROVED'), 'Approved')
  assert.equal(statusLabel('COMPLETED'), 'Completed')
})

test('minor words stay lowercase inside a label', () => {
  assert.equal(statusLabel('RETURNED_TO_RPS'), 'Returned to RPS')
  assert.equal(statusLabel('READY_FOR_RELEASE'), 'Ready for Release')
  assert.equal(statusLabel('RETURNED_FOR_REVISION'), 'Returned for Revision')
})

test('a slash separates independently capitalized segments', () => {
  assert.equal(statusLabel('N/A'), 'N/A')
  assert.equal(statusLabel('APPROVED/RELEASED'), 'Approved/Released')
})

test('an unknown status falls back to Title Case without losing acronyms', () => {
  assert.equal(statusLabel('SOMETHING_NEW'), 'Something New')
  assert.equal(statusLabel('rps_endorse'), 'RPS Endorse')
  assert.equal(statusLabel('rec_endorse'), 'REC Endorse')
  assert.equal(statusLabel('returned_to_rps'), 'Returned to RPS')
  assert.equal(statusLabel('PENDING_APPROVAL'), 'Pending Approval')
})

test('missing and non-status values render nothing instead of placeholder text', () => {
  assert.equal(statusLabel(null), '')
  assert.equal(statusLabel(undefined), '')
  assert.equal(statusLabel(''), '')
  assert.equal(statusLabel('   '), '')
  assert.equal(statusLabel({}), '')
  assert.equal(statusLabel(42), '42')
})

test('every proposal status is displayed without underscores or ALL CAPS', () => {
  for (const status of PROPOSAL_STATUSES) {
    const label = proposalStatusLabel(status)
    assert.ok(label.length > 0, `${status} must have a label`)
    assert.ok(!label.includes('_'), `${status} renders an underscore: ${label}`)
    assert.notEqual(label, label.toUpperCase(), `${status} renders in ALL CAPS: ${label}`)
  }
})

test('the fallback never leaves underscores or ALL CAPS in a generated label', () => {
  for (const status of ['SOMETHING_NEW', 'RPS_ENDORSE', 'REC_ENDORSE', 'N_A']) {
    const label = statusLabel(status)
    assert.ok(!label.includes('_'), `label contains an underscore: ${label}`)
    assert.notEqual(label, label.toUpperCase(), `label is ALL CAPS: ${label}`)
  }
})
