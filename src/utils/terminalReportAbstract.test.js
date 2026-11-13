/**
 * terminalReportAbstract.test.js
 * ---------------------------------------------------------------------------
 * Guards the Abstract / Keywords part of the Terminal Report form.
 *
 * The Proponent edits three preliminary fields: the required Abstract (one page, at most 350
 * words, with a live counter), the Keywords, and the Acknowledgment. Each stays editable, is
 * saved with the report and is printed in the generated document. The files are read as text
 * / pure functions, like the other guards.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import {
  MAX_ABSTRACT_WORDS,
  TERMINAL_NARRATIVE_FIELDS,
  TERMINAL_SECTIONS,
  terminalReportFormState,
  terminalReportPayload,
  terminalReportValidation,
} from './terminalReport.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const FORM = read('../components/TerminalReportForm.vue')

const TODAY = new Date('2026-10-26T00:00:00Z')
const proposal = { programTitle: 'Fisheries Program', projectTitle: 'Fish Landing Survey' }

test('the abstract is kept: label, instruction, required and the 350-word limit', () => {
  const abstract = TERMINAL_NARRATIVE_FIELDS.find((field) => field.key === 'abstractText')
  assert.ok(abstract, 'the abstract stays in the form model')
  assert.equal(abstract.label, 'ABSTRACT')
  assert.equal(abstract.required, true, 'the abstract is required')
  assert.equal(abstract.maxWords, 350)
  assert.equal(MAX_ABSTRACT_WORDS, 350)
  assert.match(
    abstract.hint,
    /rationale, methods, results and findings, conclusion and recommendations/
  )
})

test('the keywords are kept with their instruction', () => {
  const keywords = TERMINAL_NARRATIVE_FIELDS.find((field) => field.key === 'keywords')
  assert.ok(keywords, 'the keywords stay in the form model')
  assert.equal(keywords.label, 'Keywords')
  assert.match(keywords.hint, /Separated by commas/)
})

test('the acknowledgment is editable again with its own input', () => {
  assert.ok(
    TERMINAL_SECTIONS.some((step) => step.sheets.includes('acknowledgment')),
    'a form step shows the acknowledgment sheet'
  )
  assert.ok(FORM.includes('v-model="model.acknowledgment"'), 'the acknowledgment input is back')
  assert.ok(
    FORM.includes('Key persons, institutions and other entities that contributed'),
    'the acknowledgment description is back'
  )
})

test('the acknowledgment value of a saved report is preserved', () => {
  const saved = {
    ...terminalReportFormState(null, proposal, { today: TODAY }),
    acknowledgment: 'Thanks to everyone.',
  }
  const state = terminalReportFormState(saved, proposal, { today: TODAY })
  assert.equal(state.acknowledgment, 'Thanks to everyone.', 'the value survives reopening')
  assert.equal(
    terminalReportPayload(state).acknowledgment,
    'Thanks to everyone.',
    'and the save keeps it'
  )
})

test('the abstract stays required and is limited to 350 words', () => {
  const base = terminalReportFormState(null, proposal, { today: TODAY })
  const words = (count) => Array.from({ length: count }, () => 'word').join(' ')

  const missing = terminalReportValidation({ ...base, abstractText: '' }, { forSubmission: true })
  assert.ok(missing.errors.abstractText, 'a missing abstract blocks submission')

  const over = terminalReportValidation(
    { ...base, abstractText: words(351) },
    { forSubmission: true }
  )
  assert.match(over.errors.abstractText, /350 words/, 'an over-long abstract is rejected')

  const ok = terminalReportValidation({ ...base, abstractText: words(350) }, { forSubmission: true })
  assert.ok(!ok.errors.abstractText, 'exactly 350 words is accepted')
})

test('the form keeps the abstract counter and the acknowledgment input', () => {
  assert.ok(FORM.includes('v-for="field in narrativeFields"'), 'the narrative fields drive the form')
  assert.ok(FORM.includes('tr-counter'), 'the abstract keeps its live word counter')
  assert.ok(FORM.includes('tr-area--error'), 'the narrative fields keep their validation state')
  assert.ok(FORM.includes("shows('acknowledgment')"), 'the acknowledgment sheet is part of the form')
})
