/**
 * reviewScores.test.js
 * ---------------------------------------------------------------------------
 * Guards the arithmetic used on the evaluation page of the OC proposal-details
 * document. The 15 stored criteria are averaged per weighted group; a group with
 * no scores is skipped and the remaining weights are renormalized, so a partly
 * filled review still prints a meaningful score instead of a misleading zero.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { REVIEW_GROUPS, groupAverage, reviewWeightedScore } from './reviewScores.js'

const allScores = (value) => {
  const review = {}
  for (const group of REVIEW_GROUPS) {
    for (const field of group.fields) review[field] = value
  }
  return review
}

test('the groups carry the official weights and every score field', () => {
  assert.deepEqual(
    REVIEW_GROUPS.map((group) => group.weight),
    [20, 30, 30, 20],
  )
  assert.equal(
    REVIEW_GROUPS.reduce((count, group) => count + group.fields.length, 0),
    15,
    'the form has 15 scored criteria',
  )
})

test('a uniform review averages to that score', () => {
  const review = allScores(4)
  for (const group of REVIEW_GROUPS) {
    assert.equal(groupAverage(review, group.fields), 4)
  }
  assert.equal(reviewWeightedScore(review), 4)
})

test('group averages ignore blank and out-of-range values', () => {
  const soundness = REVIEW_GROUPS[0].fields
  const review = {
    [soundness[0]]: 5,
    [soundness[1]]: null,
    [soundness[2]]: 3,
    [soundness[3]]: 9,
  }
  assert.equal(groupAverage(review, soundness), 4)
  assert.equal(groupAverage({}, soundness), null)
  assert.equal(reviewWeightedScore({}), null)
})

test('the overall score is weighted, not a flat mean of the groups', () => {
  // Soundness (20%) = 2, Suitability (30%) = 4, Significance (30%) = 4, Competence (20%) = 2
  const review = {}
  REVIEW_GROUPS.forEach((group, index) => {
    const value = index === 0 || index === 3 ? 2 : 4
    for (const field of group.fields) review[field] = value
  })
  // 2*.2 + 4*.3 + 4*.3 + 2*.2 = 0.4 + 1.2 + 1.2 + 0.4 = 3.2
  assert.equal(reviewWeightedScore(review), 3.2)
})

test('a partly filled review renormalizes the weights it does carry', () => {
  const review = {}
  for (const field of REVIEW_GROUPS[0].fields) review[field] = 5 // Soundness only
  assert.equal(reviewWeightedScore(review), 5)
})
