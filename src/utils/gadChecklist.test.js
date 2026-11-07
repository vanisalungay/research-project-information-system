/**
 * gadChecklist.test.js
 * ---------------------------------------------------------------------------
 * Guards the GAD Checklist 2 content and the scoring rules of Section 19.
 *
 * The module is the single source of truth for the interactive GAD assessment: the 10
 * criteria, the 17 items and their exact wording, the four scoring scales, the criterion
 * subtotals, the total (maximum 20) and the official interpretation. Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  GAD_CRITERIA,
  GAD_ITEMS,
  GAD_ITEM_COUNT,
  GAD_MAX_SCORE,
  emptyGadChecklist,
  formatGadScore,
  gadChecklistFromSaved,
  gadChecklistPayload,
  gadChecklistSummary,
  gadChecklistValidation,
  gadInterpretation,
  gadItemScore,
} from './gadChecklist.js'

const allOf = (response) => Object.fromEntries(GAD_ITEMS.map((item) => [item.key, response]))

test('the checklist has the ten criteria and the seventeen items in order', () => {
  assert.equal(GAD_CRITERIA.length, 10)
  assert.equal(GAD_ITEM_COUNT, 17)
  assert.deepEqual(GAD_ITEMS.map((item) => item.key), [
    '1.1', '1.2', '2.0', '3.1', '3.2', '4.0', '5.0', '6.1', '6.2', '6.3',
    '7.0', '8.0', '9.1', '9.2', '10.1', '10.2', '10.3',
  ])
})

test('the criteria headings and questions keep the official wording', () => {
  assert.deepEqual(GAD_CRITERIA.map((c) => `${c.number} ${c.title}`), [
    '1.0 Involvement of women and men',
    '2.0 Collection of sex-disaggregated data and gender-related information',
    '3.0 Conduct of gender analysis and identification of gender issues',
    '4.0 Gender equality goals, outcomes, and outputs',
    '5.0 Matching of strategies with gender issues',
    '6.0 Gender analysis of the likely impacts of the project',
    '7.0 Monitoring targets and indicators',
    '8.0 Sex-disaggregated database requirements',
    '9.0 Resources',
    "10.0 Relationship with the agency's GAD efforts",
  ])
  assert.equal(
    GAD_ITEMS.find((i) => i.key === '4.0').question,
    'Does the project have clearly stated gender equality goals, objectives, outcomes or outputs?'
  )
  assert.equal(
    GAD_ITEMS.find((i) => i.key === '10.1').conditional,
    "IF THE AGENCY HAS NO GAD PLAN: Will the project help in formulating the implementing agency's GAD plan?"
  )
  assert.equal(
    GAD_ITEMS.find((i) => i.key === '9.1').alt,
    'OR, will the project tap counterpart funds from LGUs/partners for its GAD efforts?'
  )
})

test('every item scores by its own scale, including 6.3 = 0.66', () => {
  assert.equal(gadItemScore({ responses: { '1.1': 'PARTLY' } }, '1.1'), 0.5)
  assert.equal(gadItemScore({ responses: { '2.0': 'YES' } }, '2.0'), 2)
  assert.equal(gadItemScore({ responses: { '6.1': 'YES' } }, '6.1'), 0.67)
  assert.equal(gadItemScore({ responses: { '6.3': 'YES' } }, '6.3'), 0.66)
  assert.equal(gadItemScore({ responses: { '10.2': 'PARTLY' } }, '10.2'), 0.33)
  assert.equal(gadItemScore(null, '1.1'), null)
  assert.equal(gadItemScore({ responses: { '1.1': 'MAYBE' } }, '1.1'), null)
})

test('criterion subtotals follow the official formulas', () => {
  const checklist = {
    responses: {
      '1.1': 'YES', '1.2': 'PARTLY',
      '6.1': 'YES', '6.2': 'YES', '6.3': 'YES',
      '10.1': 'YES', '10.2': 'YES', '10.3': 'YES',
    },
  }
  const summary = gadChecklistSummary(checklist)
  assert.equal(summary.subtotals['1.0'], 1.5)
  assert.equal(summary.subtotals['6.0'], 2)
  assert.equal(summary.subtotals['10.0'], 2, 'the subtotal is bounded by the stated maximum')
})

test('all-No scores 0 and all-Yes scores the maximum 20', () => {
  assert.equal(gadChecklistSummary({ responses: allOf('NO') }).totalScore, 0)
  assert.equal(gadChecklistSummary({ responses: allOf('YES') }).totalScore, GAD_MAX_SCORE)
})

test('the interpretation follows the official bands', () => {
  assert.equal(gadInterpretation(0).range, '0-3.9')
  assert.equal(gadInterpretation(3.99).range, '0-3.9')
  assert.equal(gadInterpretation(4).range, '4.0-7.9')
  assert.equal(gadInterpretation(7.99).range, '4.0-7.9')
  assert.equal(gadInterpretation(8).range, '8.0-14.9')
  assert.equal(gadInterpretation(14.99).range, '8.0-14.9')
  assert.equal(gadInterpretation(15).range, '15.0-20.0')
  assert.equal(gadInterpretation(20).range, '15.0-20.0')
  assert.equal(gadChecklistSummary({ responses: allOf('YES') }).interpretationIndex, 3)
  assert.equal(gadChecklistSummary(emptyGadChecklist()).interpretationIndex, 0)
})

test('an incomplete checklist is never final; validation lists the unanswered items', () => {
  const empty = emptyGadChecklist()
  const summary = gadChecklistSummary(empty)
  assert.equal(summary.completed, false)
  assert.equal(summary.unanswered.length, 17)
  assert.equal(summary.answeredCount, 0)

  const invalid = gadChecklistValidation(empty)
  assert.equal(invalid.valid, false)
  assert.match(invalid.message, /17 items are still unanswered/)
  assert.deepEqual(invalid.unanswered, GAD_ITEMS.map((item) => item.key))

  assert.equal(gadChecklistValidation({ responses: allOf('YES') }).valid, true)
})

test('the payload keeps the responses and the non-empty comments', () => {
  const payload = gadChecklistPayload({
    responses: { '1.1': 'YES', '1.2': 'NO' },
    comments: { '1.1': 'Discussed with the women.', '1.2': '   ' },
  })
  assert.deepEqual(payload.responses, { '1.1': 'YES', '1.2': 'NO' })
  assert.deepEqual(payload.comments, { '1.1': 'Discussed with the women.' })
  assert.equal(payload.completed, false)
  assert.equal(payload.interpretation, '', 'no final interpretation while incomplete')

  const full = gadChecklistPayload({ responses: allOf('YES') })
  assert.equal(full.completed, true)
  assert.equal(full.totalScore, 20)
  assert.equal(full.interpretation, 'Proposed project is gender-responsive')
})

test('a saved value with an unknown response or item never loses the rest', () => {
  const state = gadChecklistFromSaved({
    responses: { '1.1': 'PARTLY', '9.9': 'YES', '2.0': 'NOPE' },
    comments: { '1.1': 'note', '9.9': 'ignored' },
  })
  assert.deepEqual(state.responses, { '1.1': 'PARTLY' })
  assert.equal(state.comments['1.1'], 'note')
  assert.equal(state.comments['2.0'], '')
})

test('scores are formatted with two decimals everywhere', () => {
  assert.equal(formatGadScore(0.5), '0.50')
  assert.equal(formatGadScore(0), '0.00')
  assert.equal(formatGadScore(20), '20.00')
  assert.equal(formatGadScore(undefined), '0.00')
})
