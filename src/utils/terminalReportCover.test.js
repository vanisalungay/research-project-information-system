/**
 * terminalReportCover.test.js
 * ---------------------------------------------------------------------------
 * Guards the Terminal Report cover page the proponent completes.
 *
 * The cover asks only for the Project Team (printed as the authors). The other cover values -
 * program/project/study title, collaborators, implementing and funding agencies, and the
 * month/year of submission - are taken from the project and submission records RPIS already
 * holds, so they are neither requested nor displayed again on the cover. The full data model
 * keeps every value, so nothing is lost when a report is saved. The files are read as text,
 * like the other guards in src/utils.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const FORM = read('../components/TerminalReportForm.vue')
const MODEL = read('./terminalReport.js')

const COVER_KEYS = [
  'programTitle',
  'projectTitle',
  'studyTitle',
  'projectTeam',
  'collaborators',
  'implementingAgencies',
  'fundingAgencies',
  'submissionMonthYear',
]

test('the cover only asks the proponent for the project team', () => {
  assert.ok(FORM.includes('TERMINAL_COVER_INPUT_FIELDS'), 'the form uses the cover input list')
  assert.ok(!FORM.includes('TERMINAL_COVER_FIELDS'), 'and no longer renders the full field list')
  assert.ok(FORM.includes('v-for="field in coverInputFields"'), 'the cover is driven by that list')
  assert.ok(FORM.includes('tr-textarea'), 'the project team is a multiline field')
  assert.ok(FORM.includes('v-model="model[field.key]"'), 'and it stays editable')
})

test('the removed cover fields are no longer requested or labelled', () => {
  for (const label of ['Study Title (if applicable)', 'Collaborators', 'Implementing Agencies',
    'Funding Agencies', 'Month and Year of Submission']) {
    assert.ok(!FORM.includes(label), `the cover must not show "${label}"`)
  }
})

test('the cover keeps the letterhead, the title, the authors and the report date', () => {
  for (const kept of ['tr-cover-title', 'tr-cover-team', 'tr-cover-report', 'tr-cover-month']) {
    assert.ok(FORM.includes(kept), `the cover keeps ${kept}`)
  }
})

test('the data model keeps every cover value, so older reports still render', () => {
  for (const key of COVER_KEYS) {
    assert.ok(MODEL.includes(`key: '${key}'`), `${key} stays in the data model`)
  }
  assert.ok(MODEL.includes('TERMINAL_COVER_INPUT_FIELDS'), 'the cover input list is declared')
})
