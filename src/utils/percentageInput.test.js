/**
 * percentageInput.test.js
 * ---------------------------------------------------------------------------
 * The percentage rules of the Quarterly Progress Report: numeric-only input, the
 * "%" display treatment, persistence without the sign, and the Project Workplan's
 * automatic AVERAGE TOTAL.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  averagePercentage,
  formatAverageTotal,
  formatPercentage,
  isValidPercentage,
  normalizePercentage,
  parsePercentage,
  percentageFormValue,
  sanitizePercentageInput,
} from './percentageInput.js'

test('typed input keeps digits and one decimal point only', () => {
  assert.equal(sanitizePercentageInput('75'), '75')
  assert.equal(sanitizePercentageInput('75.5'), '75.5')
  assert.equal(sanitizePercentageInput('75%'), '75', 'a typed sign is never stored')
  assert.equal(sanitizePercentageInput(' 75 % '), '75')
  assert.equal(sanitizePercentageInput('abc'), '')
  assert.equal(sanitizePercentageInput('7a5'), '75')
  assert.equal(sanitizePercentageInput('1.2.3'), '1.23')
  assert.equal(sanitizePercentageInput('.'), '')
  assert.equal(sanitizePercentageInput(''), '')
  assert.equal(sanitizePercentageInput(null), '')
})

test('the input never exceeds 100 and never becomes negative', () => {
  assert.equal(sanitizePercentageInput('0'), '0')
  assert.equal(sanitizePercentageInput('100'), '100')
  assert.equal(sanitizePercentageInput('100.0'), '100.0')
  assert.equal(sanitizePercentageInput('101'), '100')
  assert.equal(sanitizePercentageInput('999'), '100')
  assert.equal(sanitizePercentageInput('-10'), '10', 'the sign is not a number character')
})

test('a stored value is the number without the percentage sign', () => {
  assert.equal(normalizePercentage('75'), '75')
  assert.equal(normalizePercentage('75.5'), '75.5')
  assert.equal(normalizePercentage('75.50'), '75.5')
  assert.equal(normalizePercentage('80%'), '80', 'a legacy value is normalised, not duplicated')
  assert.equal(normalizePercentage('0'), '0')
  assert.equal(normalizePercentage('100'), '100')
  assert.equal(normalizePercentage(''), '')
  assert.equal(normalizePercentage(null), '')
})

test('a non-numeric legacy value is preserved for correction, never silently blanked', () => {
  assert.equal(normalizePercentage('not a number'), 'not a number')
  assert.equal(percentageFormValue('80%'), '80')
  assert.equal(percentageFormValue('not a number'), 'not a number')
})

test('only numbers from 0 to 100 are valid; empty is valid', () => {
  assert.equal(isValidPercentage(''), true)
  assert.equal(isValidPercentage('0'), true)
  assert.equal(isValidPercentage('100'), true)
  assert.equal(isValidPercentage('75.5'), true)
  assert.equal(isValidPercentage('100%'), true)
  assert.equal(isValidPercentage('100.01'), false)
  assert.equal(isValidPercentage('-1'), false)
  assert.equal(isValidPercentage('abc'), false)
  assert.equal(isValidPercentage('50abc'), false)
  assert.equal(isValidPercentage('NaN'), false)
})

test('parsing reads integers, decimals and a legacy sign', () => {
  assert.equal(parsePercentage('75'), 75)
  assert.equal(parsePercentage('75.5'), 75.5)
  assert.equal(parsePercentage('75%'), 75)
  assert.equal(parsePercentage(''), null)
  assert.equal(parsePercentage('101'), null)
  assert.equal(parsePercentage('abc'), null)
})

test('display adds exactly one percentage sign', () => {
  assert.equal(formatPercentage('75'), '75%')
  assert.equal(formatPercentage('75.5'), '75.5%')
  assert.equal(formatPercentage('80%'), '80%', 'a legacy value never becomes 80%%')
  assert.equal(formatPercentage(''), '')
  assert.equal(formatPercentage(null), '')
})

test('AVERAGE TOTAL is the mean of the months actually entered', () => {
  assert.equal(averagePercentage(['50', '70', '', '', '', '', '', '', '', '', '', '']), '60')
  assert.equal(averagePercentage(['80', '60', '100']), '80')
  assert.equal(averagePercentage(['40', '', '60']), '50')
  assert.equal(averagePercentage(['100', '100', '100']), '100')
  assert.equal(averagePercentage(['50', '50', '51']), '50.33')
  assert.equal(averagePercentage(['', '', '']), '', 'all months empty means no average')
  assert.equal(averagePercentage([]), '')
  assert.equal(averagePercentage(null), '')
  assert.equal(averagePercentage(['0', '']), '0', 'a real 0 is a value, unlike an empty month')
})

test('AVERAGE TOTAL is displayed with the percentage sign', () => {
  assert.equal(formatAverageTotal(['50', '70']), '60%')
  assert.equal(formatAverageTotal(['50', '50', '51']), '50.33%')
  assert.equal(formatAverageTotal(['', '']), '')
})
