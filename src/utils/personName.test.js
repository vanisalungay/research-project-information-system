/**
 * personName.test.js
 * ---------------------------------------------------------------------------
 * Pins down the one name format the whole application uses. Every case here is
 * also mirrored by PersonNameFormatterTest on the backend.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  FULL_NAME_EXAMPLE,
  PERSON_NAME_EXAMPLE,
  PERSON_NAME_FORMAT,
  firstLastNameError,
  formatPersonName,
  formatPersonNameLines,
} from './personName.js'

test('the required instruction and example are exactly the agreed wording', () => {
  assert.equal(
    PERSON_NAME_FORMAT,
    'Last Name, First Name, Middle Initial (optional), Suffix (optional)',
  )
  assert.equal(PERSON_NAME_EXAMPLE, 'Dela Cruz, Juan, M., Jr.')
})

test('an all-uppercase name becomes title case', () => {
  assert.equal(formatPersonName('JUAN DELA CRUZ'), 'Juan Dela Cruz')
  assert.equal(formatPersonName('XARZHA MAURINE BAGARES'), 'Xarzha Maurine Bagares')
})

test('an all-lowercase name becomes title case', () => {
  assert.equal(formatPersonName('juan dela cruz'), 'Juan Dela Cruz')
})

test('a mixed-case name becomes title case', () => {
  assert.equal(formatPersonName('jUaN dElA cRuZ'), 'Juan Dela Cruz')
})

test('the last-name-first format keeps its commas, periods and suffix', () => {
  assert.equal(formatPersonName('JUAN, DELA CRUZ, M.'), 'Juan, Dela Cruz, M.')
  assert.equal(formatPersonName('DELA CRUZ, JUAN, M., JR.'), 'Dela Cruz, Juan, M., Jr.')
  assert.equal(formatPersonName('dela cruz,juan,m.,jr.'), 'Dela Cruz, Juan, M., Jr.')
})

test('middle initial and suffix are optional', () => {
  assert.equal(formatPersonName('DELA CRUZ, JUAN'), 'Dela Cruz, Juan')
  assert.equal(formatPersonName('DELA CRUZ, JUAN, JR.'), 'Dela Cruz, Juan, Jr.')
})

test('multi-word and particle surnames are kept whole', () => {
  assert.equal(formatPersonName('DE LA CRUZ, JUAN'), 'De La Cruz, Juan')
  assert.equal(formatPersonName('DELA CRUZ, MARIA CLARA'), 'Dela Cruz, Maria Clara')
  assert.equal(formatPersonName('VAN DER BERG, ANA'), 'Van Der Berg, Ana')
})

test('Roman-numeral suffixes stay upper case', () => {
  assert.equal(formatPersonName('DELA CRUZ, JUAN, III'), 'Dela Cruz, Juan, III')
  assert.equal(formatPersonName('santos, jose, iv'), 'Santos, Jose, IV')
})

test('hyphens and apostrophes are preserved and cased correctly', () => {
  assert.equal(formatPersonName('ANA-MARIA REYES'), 'Ana-Maria Reyes')
  assert.equal(formatPersonName("JOSE O'BRIEN"), "Jose O'Brien")
  assert.equal(formatPersonName('dela cruz, jean-luc'), 'Dela Cruz, Jean-Luc')
})

test('extra spaces are collapsed and the value is trimmed', () => {
  assert.equal(formatPersonName('  dela   cruz ,   juan  '), 'Dela Cruz, Juan')
})

test('blank values stay blank and null is passed through', () => {
  assert.equal(formatPersonName(''), '')
  assert.equal(formatPersonName('   '), '')
  assert.equal(formatPersonName(null), null)
  assert.equal(formatPersonName(undefined), undefined)
})

test('a multi-line team is formatted line by line', () => {
  assert.equal(
    formatPersonNameLines('BAGARES, XARZHA MAURINE\nlungay, vanisa\n\nDELA CRUZ, JUAN, M., JR.'),
    'Bagares, Xarzha Maurine\nLungay, Vanisa\n\nDela Cruz, Juan, M., Jr.',
  )
  assert.equal(formatPersonNameLines(''), '')
})

test('the account name example is First Name first, then Last Name', () => {
  assert.equal(FULL_NAME_EXAMPLE, 'Juan Dela Cruz')
})

test('account names accept a plain First Name Last Name entry', () => {
  assert.equal(firstLastNameError('Juan Dela Cruz'), '')
  assert.equal(firstLastNameError('  JUAN   DELA CRUZ  '), '')
  assert.equal(firstLastNameError('Maria Clara Dela Cruz'), '')
  assert.equal(firstLastNameError('Juan Dela Cruz Jr.'), '')
})

test('account names reject an empty name, a one-word name and the comma order', () => {
  assert.match(firstLastNameError(''), /required/i)
  assert.match(firstLastNameError('   '), /required/i)
  assert.match(firstLastNameError('Juan'), /first name and the last name/i)
  assert.match(firstLastNameError('Dela Cruz, Juan'), /without commas/i)
})

test('formatting is idempotent', () => {
  const once = formatPersonName('DELA CRUZ, JUAN, M., JR.')
  assert.equal(formatPersonName(once), once)
})
