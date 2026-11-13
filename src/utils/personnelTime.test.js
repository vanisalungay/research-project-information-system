/**
 * personnelTime.test.js
 * ---------------------------------------------------------------------------
 * Section 24 (Personnel Requirement) - the "% Time Devoted" column: a number only, shown
 * with the % symbol, kept between 0 and 100, stored as a plain number and read back
 * correctly for a value an older record saved as "50%".
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  TIME_DEVOTED_HINT,
  TIME_DEVOTED_LABEL,
  TIME_DEVOTED_MAX,
  TIME_DEVOTED_MIN,
  TIME_DEVOTED_RANGE_ERROR,
  finalizeTimeDevoted,
  formatTimeDevoted,
  isValidTimeDevoted,
  parseTimeDevoted,
  sanitizeTimeDevotedInput,
  timeDevotedPayload,
  validateTimeDevotedRows
} from './personnelTime.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const readBackend = (path) =>
  readFileSync(fileURLToPath(new URL('../../backend/' + path, import.meta.url)), 'utf8')

const FORM = read('../views/Proponent/ProponentSubmitProp.vue')
const REVISION = read('../views/Proponent/ProponentSubmitRev.vue')
const STEP4 = read('../views/Proponent/ProponentSubmitProp3.vue')
const DETAIL = read('../views/Proponent/ProponentDetiailedProp.vue')
const REGISTRY = read('./proposalFields.js')
const FIELD_SECTION = read('../components/ProposalFieldSection.vue')
const HISTORY = read('../components/SubmissionHistory.vue')
const DOCUMENT = read('./documentExport.js')
const OFFICIAL = read('./officialProposalForm.js')

// ============================ the rules ============================

test('the accepted range is 0 - 100 and the column keeps its official heading', () => {
  assert.equal(TIME_DEVOTED_MIN, 0)
  assert.equal(TIME_DEVOTED_MAX, 100)
  assert.equal(TIME_DEVOTED_LABEL, '% Time Devoted')
  assert.match(TIME_DEVOTED_RANGE_ERROR, /number between 0 and 100/)
  assert.match(TIME_DEVOTED_HINT, /% symbol is added automatically/)
})

test('a typed number is kept as a number (25, 50, 75, 100)', () => {
  for (const value of ['25', '50', '75', '100', '0']) {
    const { value: kept, clamped } = sanitizeTimeDevotedInput(value)
    assert.equal(kept, value, `${value} is kept as it is`)
    assert.equal(clamped, false)
  }
})

test('letters and other invalid characters never survive the field', () => {
  assert.equal(sanitizeTimeDevotedInput('abc').value, '')
  assert.equal(sanitizeTimeDevotedInput('2a5').value, '25')
  assert.equal(sanitizeTimeDevotedInput('fifty').value, '')
  assert.equal(sanitizeTimeDevotedInput('50%').value, '50', 'a typed % symbol is dropped')
  assert.equal(sanitizeTimeDevotedInput(' 5 0 ').value, '50', 'spacing is dropped')
})

test('a negative value cannot be entered', () => {
  assert.equal(sanitizeTimeDevotedInput('-5').value, '5', 'the minus sign is dropped')
  assert.equal(sanitizeTimeDevotedInput('-100').value, '100')
  assert.equal(isValidTimeDevoted('-5'), false, 'and it stays invalid')
})

test('a legacy negative or over-100 value is clamped exactly as the backend clamps it', () => {
  // Only a record saved before the field was numeric can hold these values. The
  // backend caps them to 0 / 100 on save, and so does every frontend layer here - a
  // reopened "-25" must not silently become +25.
  assert.equal(parseTimeDevoted('-25'), '0')
  assert.equal(parseTimeDevoted('-0.5'), '0')
  assert.equal(parseTimeDevoted('-25%'), '0')
  assert.equal(formatTimeDevoted('-25'), '0%')
  assert.equal(formatTimeDevoted('150'), '100%')
  assert.equal(timeDevotedPayload('-25'), '0')
  assert.equal(timeDevotedPayload('150'), '100')
})

test('a value above 100 is capped at 100 and reported as clamped', () => {
  const { value, clamped } = sanitizeTimeDevotedInput('150')
  assert.equal(value, '100')
  assert.equal(clamped, true)
  assert.equal(sanitizeTimeDevotedInput('999').value, '100')
})

test('decimals are supported', () => {
  assert.equal(sanitizeTimeDevotedInput('12.5').value, '12.5')
  assert.equal(sanitizeTimeDevotedInput('.5').value, '0.5')
  assert.equal(sanitizeTimeDevotedInput('50.25').value, '50.25')
  assert.equal(isValidTimeDevoted('12.5'), true)
  assert.equal(isValidTimeDevoted('100.01'), false)
  // A value still being typed ("50.") is the number 50 once it is finished.
  assert.equal(finalizeTimeDevoted('50.'), '50')
  assert.equal(isValidTimeDevoted('50.'), true)
})

test('only a number between 0 and 100 is valid', () => {
  for (const value of ['25', '50', '100', '0', '12.5']) {
    assert.equal(isValidTimeDevoted(value), true, `${value} is valid`)
  }
  for (const value of ['', '   ', 'abc', 'fifty', '150', '-5', '12,5', '50%%']) {
    assert.equal(isValidTimeDevoted(value), false, `${value} is not valid`)
  }
})

test('the display always carries the % symbol', () => {
  assert.equal(formatTimeDevoted('25'), '25%')
  assert.equal(formatTimeDevoted('50'), '50%')
  assert.equal(formatTimeDevoted('100'), '100%')
  assert.equal(formatTimeDevoted('12.5'), '12.5%')
  assert.equal(formatTimeDevoted('50%'), '50%', 'already saved with the symbol: not doubled')
  assert.equal(formatTimeDevoted('50 %'), '50%')
  assert.equal(formatTimeDevoted(''), '')
  assert.equal(formatTimeDevoted(null), '')
})

test('a legacy value saved as prose is shown unchanged instead of being lost', () => {
  assert.equal(formatTimeDevoted('fifty percent'), 'fifty percent')
  assert.equal(parseTimeDevoted('fifty percent'), 'fifty percent')
  assert.equal(timeDevotedPayload('fifty percent'), 'fifty percent')
})

test('a saved percentage is read back as a number', () => {
  assert.equal(parseTimeDevoted('50%'), '50')
  assert.equal(parseTimeDevoted('50 %'), '50')
  assert.equal(parseTimeDevoted(' 75 '), '75')
  assert.equal(parseTimeDevoted(''), '')
  assert.equal(parseTimeDevoted(null), '')
  assert.equal(parseTimeDevoted(50), '50')
})

test('what is sent to the API is the plain number, never the symbol', () => {
  assert.equal(timeDevotedPayload('25'), '25')
  assert.equal(timeDevotedPayload('25%'), '25')
  assert.equal(timeDevotedPayload('12.50'), '12.5')
  assert.equal(timeDevotedPayload(''), '')
})

test('the row validation names the row that is not a number', () => {
  const rows = [
    { position: 'Leader', effort: '50', responsibilities: 'x' },
    { position: 'Assistant', effort: 'abc', responsibilities: 'y' },
    { position: 'Encoder', effort: '150', responsibilities: 'z' }
  ]
  const result = validateTimeDevotedRows(rows)
  assert.equal(result.valid, false)
  assert.equal(result.rowErrors[0], undefined, 'the valid row reports nothing')
  assert.match(result.rowErrors[1], /number between 0 and 100/)
  assert.match(result.rowErrors[2], /number between 0 and 100/)
  assert.match(result.message, /#2, #3/)
})

test('an empty cell is left to the section rule that already requires every field', () => {
  const result = validateTimeDevotedRows([{ position: 'Leader', effort: '', responsibilities: 'x' }])
  assert.equal(result.valid, true)
})

// ======================= how the field is wired =======================

test('both proponent forms use a number field with the % symbol beside it', () => {
  for (const [name, source] of [['create form', FORM], ['revision form', REVISION]]) {
    assert.ok(source.includes("import") && source.includes('@/utils/personnelTime'), `${name} uses the rules`)
    assert.ok(
      source.includes('type="number" min="0" max="100" step="0.01"'),
      `${name} accepts a number only`
    )
    assert.ok(source.includes('class="percent-symbol"'), `${name} shows the % symbol itself`)
    assert.ok(source.includes('onTimeDevotedInput(row, $event)'), `${name} sanitizes every keystroke`)
    assert.ok(source.includes('TIME_DEVOTED_HINT'), `${name} explains the rule`)
    assert.ok(
      !source.includes('placeholder="e.g. 50%"'),
      `${name} no longer asks the proponent to type the symbol`
    )
  }
  assert.ok(FORM.includes('timeDevotedErrorFor(index)'), 'the create form reports the row')
  assert.ok(REVISION.includes('timeDevotedErrorFor(index)'), 'so does the revision form')
})

test('the value is validated before saving or submitting', () => {
  assert.ok(FORM.includes('validateTimeDevotedRows(proposal.personnel_requirements)'), 'form validation')
  assert.ok(FORM.includes('errors.personnel_requirements ='), 'and its own message')
  assert.ok(STEP4.includes('validateTimeDevotedRows(d.personnel_requirements)'), 'step 4 re-checks it')
  assert.ok(REVISION.includes('validateTimeDevotedRows(proposal.value.personnel_requirements)'), 'revision too')
})

test('the payload carries the plain number, not the symbol', () => {
  for (const [name, source] of [['create form', FORM], ['step 4', STEP4], ['revision form', REVISION]]) {
    assert.ok(source.includes('timeDevotedPayload('), `${name} sends the number`)
  }
  assert.ok(!/\btime: p\.effort \|\| ''/.test(REVISION), 'the raw text is no longer sent')
  assert.ok(!FORM.includes('timeDevoted: r.effort,'), 'so is the print payload')
})

test('a reopened draft keeps editing the value it saved', () => {
  assert.ok(FORM.includes('effort: parseTimeDevoted(p.timeDevoted)'), 'the create form reopens it')
  assert.ok(REVISION.includes('effort: parseTimeDevoted(p.timeDevoted)'), 'the revision form reopens it')
})

test('every page that shows the plan shows the percentage', () => {
  assert.ok(DETAIL.includes('formatTimeDevoted(row.timeDevoted)'), 'the proponent detail view')
  assert.ok(HISTORY.includes('formatTimeDevoted'), 'the submission history')
  assert.ok(REGISTRY.includes('format: formatTimeDevoted'), 'the shared review registry')
  assert.ok(FIELD_SECTION.includes('column.format'), 'the shared renderer applies it')
  assert.ok(DOCUMENT.includes('formatTimeDevoted(r.timeDevoted)'), 'the proposal document')
  assert.ok(OFFICIAL.includes('formatTimeDevoted(r.timeDevoted)'), 'the official form')
})

test('the backend normalizes and validates the same range', () => {
  const service = readBackend('src/main/java/com/rpis/backend/service/ProposalService.java')
  assert.ok(service.includes('normalizeTimeDevoted('), 'every value is normalized on save')
  assert.ok(service.includes('TIME_DEVOTED_MIN'), 'the range is enforced')
  assert.ok(service.includes('TIME_DEVOTED_MAX'))
  assert.ok(service.includes('TIME_DEVOTED_MESSAGE'), 'with one message for both layers')
  assert.ok(service.includes('replace("%", "")'), 'a legacy % symbol is stripped, never doubled')
})
