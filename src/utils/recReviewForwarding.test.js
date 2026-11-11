/**
 * recReviewForwarding.test.js
 * ---------------------------------------------------------------------------
 * Guards the REC evaluation form's scoring and forwarding contract.
 *
 * The page was previously presentational: the 15 score fields were unbound number inputs and the
 * Approve action only changed the proposal status. These assertions lock in the replacement - a
 * circular 1-5 radio picker, a confirmation modal, and a single review submission that persists
 * the scores - so a later restyle cannot quietly drop any of them.
 *
 * The file is read as text, the technique the other guard tests use, so this needs no browser.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const FORM = read('../views/Rec/RecReviewForm.vue')

/** The exact names of the score columns on the ProposalReview entity. */
const SCORE_FIELDS = [
  'soundnessSectorNeedScore',
  'soundnessEffectiveSolutionScore',
  'soundnessReasonableBudgetScore',
  'soundnessDoableWorkplanScore',
  'suitabilityCostEffectiveScore',
  'suitabilityPartnersIdentifiedScore',
  'suitabilityCommercializationScore',
  'suitabilityTimelyUtilizationScore',
  'significanceEconomicImpactScore',
  'significanceSocialImpactScore',
  'significanceEnvironmentalImpactScore',
  'significanceSustainabilityScore',
  'competenceAlignmentScore',
  'competenceCollaborationScore',
  'competenceDeliverablesUnderstandingScore',
]

test('every raw score is a circular radio choice, never a number input', () => {
  for (const field of SCORE_FIELDS) {
    assert.ok(FORM.includes(`'${field}'`), field + ' must stay bound in the form')
  }
  assert.ok(FORM.includes('type="radio"'), 'scores are chosen with radio buttons')
  assert.ok(FORM.includes('SCORE_VALUES'), 'the 1-5 choices come from one definition')
  assert.ok(!FORM.includes('type="number"'), 'the old number input must stay gone')
  assert.ok(!FORM.includes('rate-input'), 'the old rate-input field must stay gone')
  assert.ok(FORM.includes(':value="n"'), 'each radio carries a numeric 1-5 value')
})

test('the forwarding confirmation asks first and offers Approve and Cancel', () => {
  assert.ok(
    FORM.includes('Are you sure you want to forward this proposal?'),
    'the exact confirmation message is present',
  )
  assert.match(FORM, /Cancel\s*<\/button>/, 'Cancel closes without forwarding')
  assert.match(FORM, /Approve\s*<\/button>/, 'Approve executes the forwarding action')
  assert.ok(FORM.includes('showApproveModal'), 'the modal is driven by its own state')
})

test('forwarding saves the scores through the existing review endpoint', () => {
  assert.ok(FORM.includes('/reviews'), 'the existing review endpoint is reused')
  assert.ok(FORM.includes('params: { reviewerId }'), 'the reviewer id is sent as a query param')
  assert.ok(FORM.includes("decision: 'APPROVED'"), 'the REC decision is submitted')
  assert.ok(
    !FORM.includes('status?status=REC_APPROVED'),
    'the status must be set by submitReview, not by a second request',
  )
})

test('a second Approve click cannot start a second request', () => {
  const guard = FORM.indexOf('if (isForwarding.value) return')
  const flag = FORM.indexOf('isForwarding.value = true')
  const request = FORM.indexOf('/reviews')

  assert.ok(guard >= 0, 'confirmApproval starts with an in-flight guard')
  assert.ok(flag > guard, 'the flag is set after the guard')
  assert.ok(request > flag, 'the request is only reached after the flag is set')
})
