/**
 * recApprovalValidation.test.js
 * ---------------------------------------------------------------------------
 * REC may not approve without a complete 1-5 evaluation and a real overall comment.
 * The scoring system itself is unchanged; this only pins down the approval gate.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  EVALUATION_SCORE_MAX,
  EVALUATION_SCORE_MIN,
  evaluationApprovalError,
  isEvaluationApprovalReady,
  missingEvaluationScores,
  normalizeEvaluationScore,
} from './recEvaluation.js'

const FIELDS = [
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

const completeScores = () => Object.fromEntries(FIELDS.map((field) => [field, 4]))
const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

test('the allowed range is the one the form presents (1 to 5)', () => {
  assert.equal(EVALUATION_SCORE_MIN, 1)
  assert.equal(EVALUATION_SCORE_MAX, 5)
  for (const value of [1, 2, 3, 4, 5]) assert.equal(normalizeEvaluationScore(value), value)
  for (const value of [0, 6, -1, 2.5, 'abc', '', null, undefined]) {
    assert.equal(normalizeEvaluationScore(value), null)
  }
  assert.equal(normalizeEvaluationScore('3'), 3, 'a numeric string from a form is accepted')
})

test('missing score fields are identified', () => {
  const scores = completeScores()
  assert.deepEqual(missingEvaluationScores(scores, FIELDS), [])
  delete scores.competenceAlignmentScore
  scores.soundnessSectorNeedScore = null
  scores.suitabilityCostEffectiveScore = 9
  assert.deepEqual(
    missingEvaluationScores(scores, FIELDS),
    ['soundnessSectorNeedScore', 'suitabilityCostEffectiveScore', 'competenceAlignmentScore'],
  )
})

test('approval requires every score', () => {
  const scores = completeScores()
  delete scores.significanceSustainabilityScore
  const message = evaluationApprovalError(scores, FIELDS, 'A complete evaluation.')
  assert.match(message, /all 15 evaluation criteria/i)
  assert.match(message, /1 score is still missing/i)
  assert.equal(isEvaluationApprovalReady(scores, FIELDS, 'A complete evaluation.'), false)
})

test('a blank or whitespace-only comment blocks approval', () => {
  for (const comment of ['', '   ', '\n\t', null, undefined]) {
    assert.match(evaluationApprovalError(completeScores(), FIELDS, comment), /overall comment is required/i)
    assert.equal(isEvaluationApprovalReady(completeScores(), FIELDS, comment), false)
  }
})

test('a complete evaluation with a real comment is the only approval state', () => {
  assert.equal(evaluationApprovalError(completeScores(), FIELDS, 'Meets the criteria.'), '')
  assert.equal(isEvaluationApprovalReady(completeScores(), FIELDS, 'Meets the criteria.'), true)
})

test('the form uses the shared approval rule and requires the evaluation', () => {
  const form = read('../views/Rec/RecReviewForm.vue')
  assert.ok(form.includes("from '@/utils/recEvaluation.js'"))
  assert.ok(form.includes('evaluationApprovalError(scores, SCORE_FIELDS, overallComments.value)'))
  assert.ok(form.includes('isEvaluationApprovalReady(scores, SCORE_FIELDS, overallComments.value)'))
  assert.ok(form.includes('!approvalReady'), 'Approve is disabled until the evaluation is complete')
  assert.ok(form.includes('id="approval-requirements"'), 'the missing requirements are shown')
  assert.ok(!form.includes('Scoring is optional'), 'scoring is no longer presented as optional')
  assert.ok(form.includes('normalizeEvaluationScore(scores[field])'))
  assert.ok(form.includes("title: 'Evaluation Incomplete'"), 'the attempt gets a clear message')
})

test('REC approval asks for confirmation and cannot be submitted twice', () => {
  const form = read('../views/Rec/RecReviewForm.vue')
  assert.ok(form.includes('Confirm REC Approval'))
  assert.ok(form.includes('will be approved after confirmation'))
  assert.ok(form.includes('if (isForwarding.value) return'), 'duplicate submissions are blocked')
  assert.ok(form.includes('if (!approvalReady.value)'), 'the rule is re-checked on submit')
})
