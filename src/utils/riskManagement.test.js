/**
 * riskManagement.test.js
 * ---------------------------------------------------------------------------
 * Section 21 (Risk Management Plan) as a table: the helpers the Create Proposal form, the
 * Revision form, the Review & Submit step, the review pages and the documents all share.
 *
 * These are executed assertions on the real module - not source-text checks - because the
 * rules they encode are what stops an incomplete or whitespace-only risk entry from being
 * submitted. Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  RISK_COLUMNS,
  RISK_LEVELS,
  blankRiskEntry,
  ensureRiskRows,
  isEmptyRiskEntry,
  normalizeRiskLevel,
  riskEntriesFromSaved,
  riskEntriesPayload,
  riskPlanText,
  riskRowsFrom,
  validateRiskEntries,
  validateRiskPlan
} from './riskManagement.js'

const complete = (overrides = {}) => ({
  identifiedRisks: 'Delayed delivery of research equipment',
  likelihood: 'High',
  impact: 'Medium',
  mitigationMeasure: 'Identify alternative suppliers and coordinate procurement in advance.',
  ...overrides
})

test('the table offers exactly the High / Medium / Low levels for both dropdowns', () => {
  assert.deepEqual(RISK_LEVELS, ['High', 'Medium', 'Low'])
  assert.deepEqual(
    RISK_COLUMNS.map((column) => column.label),
    ['Identified Risks', 'Likelihood', 'Impact', 'Mitigation Measure']
  )
})

test('a new row starts with nothing selected in the dropdowns', () => {
  const row = blankRiskEntry()
  assert.equal(row.likelihood, '')
  assert.equal(row.impact, '')
  assert.equal(row.identifiedRisks, '')
  assert.equal(row.mitigationMeasure, '')
  assert.ok(isEmptyRiskEntry(row))
})

test('a new proposal always has at least one row to type in', () => {
  assert.equal(ensureRiskRows([]).length, 1)
  assert.equal(ensureRiskRows(null).length, 1)
  const saved = [complete()]
  assert.equal(ensureRiskRows(saved), saved, 'the saved rows are used as they are')
})

test('only High, Medium and Low are recognized as a level', () => {
  assert.equal(normalizeRiskLevel('High'), 'High')
  assert.equal(normalizeRiskLevel('medium'), 'Medium')
  assert.equal(normalizeRiskLevel('  LOW  '), 'Low')
  assert.equal(normalizeRiskLevel('Very high'), '', 'free text is not a level')
  assert.equal(normalizeRiskLevel(''), '')
  assert.equal(normalizeRiskLevel(null), '')
})

test('scenario 1: one complete risk entry is valid', () => {
  const result = validateRiskEntries([complete()])
  assert.equal(result.valid, true)
  assert.equal(result.hasEntries, true)
  assert.equal(result.message, '')
})

test('scenario 2: several risk entries are valid and stay in the order they were entered', () => {
  const rows = [complete(), complete({ identifiedRisks: 'Low participation of fisherfolk', likelihood: 'Low' })]
  const result = validateRiskEntries(rows)
  assert.equal(result.valid, true)
  const payload = riskEntriesPayload(rows)
  assert.equal(payload.length, 2)
  assert.equal(payload[0].identifiedRisks, 'Delayed delivery of research equipment')
  assert.equal(payload[1].identifiedRisks, 'Low participation of fisherfolk')
  assert.equal(payload[1].likelihood, 'Low')
})

test('scenarios 5-8: a missing identified risk, likelihood, impact or mitigation blocks submission', () => {
  const cases = [
    ['identifiedRisks', { identifiedRisks: '' }],
    ['likelihood', { likelihood: '' }],
    ['impact', { impact: '' }],
    ['mitigationMeasure', { mitigationMeasure: '' }]
  ]
  for (const [field, overrides] of cases) {
    const result = validateRiskEntries([complete(overrides)])
    assert.equal(result.valid, false, `${field} must be required`)
    assert.ok(result.rowErrors[0][field], `row 1 must report its ${field}`)
  }
})

test('scenario 9: whitespace-only values are treated as missing', () => {
  const result = validateRiskEntries([complete({ identifiedRisks: '   ', mitigationMeasure: '\t\n ' })])
  assert.equal(result.valid, false)
  assert.ok(result.rowErrors[0].identifiedRisks)
  assert.ok(result.rowErrors[0].mitigationMeasure)
  assert.equal(riskEntriesPayload([complete({ identifiedRisks: '   ' })])[0].identifiedRisks, '')
})

test('the incomplete row is identified by its own number', () => {
  const result = validateRiskEntries([
    complete(),
    complete({ identifiedRisks: 'Low participation of fisherfolk' }),
    complete({ mitigationMeasure: '' })
  ])
  assert.equal(result.valid, false)
  assert.equal(Object.keys(result.rowErrors[0] || {}).length, 0, 'the complete row reports nothing')
  assert.equal(Object.keys(result.rowErrors[1] || {}).length, 0, 'the second complete row reports nothing')
  assert.ok(result.rowErrors[2].mitigationMeasure, 'the third row reports the missing measure')
  assert.match(result.message, /#3/)
})

test('a plan with no filled-in row yet is reported as missing', () => {
  const result = validateRiskEntries([blankRiskEntry()])
  assert.equal(result.valid, false)
  assert.equal(result.hasEntries, false)
  assert.deepEqual(result.emptyRows, [0], 'the seeded row is empty, not wrong')
  assert.match(result.message, /Add at least one risk entry/)
})

test('an added but untouched row next to a complete one is not an error', () => {
  const result = validateRiskEntries([complete(), blankRiskEntry()])
  assert.equal(result.valid, true, 'an empty extra row carries nothing to complete')
})

test('a level that is not High / Medium / Low is rejected like a missing one', () => {
  const result = validateRiskEntries([complete({ impact: 'Catastrophic' })])
  assert.equal(result.valid, false)
  assert.ok(result.rowErrors[0].impact)
})

test('scenario 15: a proposal that only has the old single-text plan stays valid', () => {
  const result = validateRiskPlan([blankRiskEntry()], {
    legacyText: 'Risks, assumptions and mitigation strategies in prose.'
  })
  assert.equal(result.valid, true)
  assert.equal(result.legacyOnly, true)
})

test('the old single-text plan never excuses an incomplete new row', () => {
  const result = validateRiskPlan([complete({ likelihood: '' })], { legacyText: 'Old prose plan.' })
  assert.equal(result.valid, false)
  assert.ok(result.rowErrors[0].likelihood)
})

test('scenarios 10-11: what a save returns is what a reopened proposal edits', () => {
  // The API returns the entity array (camelCase keys); a blank entry is dropped so a
  // reopened proposal never shows an empty or duplicated row.
  const saved = [
    { identifiedRisks: 'Delayed delivery', likelihood: 'High', impact: 'Medium', mitigationMeasure: 'Order early.' },
    { identifiedRisks: '   ', likelihood: '', impact: '', mitigationMeasure: '' },
    { identifiedRisks: 'Low participation', likelihood: 'low', impact: 'HIGH', mitigationMeasure: 'Assemblies.' }
  ]
  const rows = riskEntriesFromSaved(saved)
  assert.equal(rows.length, 2, 'the blank row is not shown again')
  assert.equal(rows[1].likelihood, 'Low', 'the level comes back canonical')
  assert.equal(rows[1].impact, 'High')
  assert.equal(rows.length, new Set(rows.map((row) => row.identifiedRisks)).size, 'no duplicated rows')
})

test('scenario 15-16: an old proposal has no structured rows and keeps its text', () => {
  assert.deepEqual(riskEntriesFromSaved(undefined), [])
  assert.deepEqual(riskEntriesFromSaved(null), [])
  assert.deepEqual(riskEntriesFromSaved('[]'), [])
  assert.deepEqual(riskRowsFrom({ risks: 'Old prose plan.' }), [])
  assert.equal(riskRowsFrom({ riskManagement: [complete()] }).length, 1)
})

test('the payload trims the text and never sends the seeded empty row', () => {
  const payload = riskEntriesPayload([
    complete({ identifiedRisks: '  Delayed delivery  ', mitigationMeasure: ' Order early. ' }),
    blankRiskEntry()
  ])
  assert.deepEqual(payload, [
    {
      identifiedRisks: 'Delayed delivery',
      likelihood: 'High',
      impact: 'Medium',
      mitigationMeasure: 'Order early.'
    }
  ])
  assert.deepEqual(riskEntriesPayload(null), [])
})

test('the readable single-text rendering matches what the backend stores', () => {
  const text = riskPlanText([
    complete(),
    complete({
      identifiedRisks: 'Low participation',
      likelihood: 'Low',
      impact: 'High',
      mitigationMeasure: 'Assemblies.'
    })
  ])
  assert.match(text, /^1\. Delayed delivery of research equipment \(Likelihood: High, Impact: Medium\)/)
  assert.match(text, /\n {3}Mitigation: Identify alternative suppliers and coordinate procurement in advance\./)
  assert.match(text, /\n2\. Low participation \(Likelihood: Low, Impact: High\)/)
})
