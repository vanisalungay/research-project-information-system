/**
 * terminalSummarySheet.test.js
 * ---------------------------------------------------------------------------
 * Guards the summary sheet of the Terminal Report.
 *
 * The summary sheet is three labelled tables - Project Information Details, Major
 * Accomplishments and Financial Information Details - and the proponent edits the Details
 * column of each. Every edited value is stored on the Terminal Report itself, so the original
 * project record is never touched. The files are read as text / pure functions, like the
 * other guards.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import {
  TERMINAL_PROJECT_INFO_FIELDS,
  TERMINAL_SUMMARY_HEADINGS,
  formatPhpAmount,
  terminalReportFormState,
  terminalReportPayload,
  terminalSummaryRows,
} from './terminalReport.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const FORM = read('../components/TerminalReportForm.vue')

const TODAY = new Date('2026-10-26T00:00:00Z')

const proposal = {
  programTitle: 'Fisheries Program',
  projectTitle: 'Fish Landing Survey',
  projectLeader: 'Juan Dela Cruz',
  projectLeaderSex: 'Male',
  college: 'College of Marine Sciences',
  cooperatingAgencies: 'LGU Naawan',
  addressMunicipality: 'Naawan',
  addressProvince: 'Misamis Oriental',
  proponent: { email: 'juan@msunaawan.edu.ph' },
}

const byLabel = (report) =>
  Object.fromEntries(report.summaryRows.map((row) => [row.label, row.detail]))

test('the project information table has the ten fixed rows in document order', () => {
  assert.deepEqual(terminalSummaryRows([]).map((row) => row.label), TERMINAL_PROJECT_INFO_FIELDS)
})

test('the fixed rows are prefilled from the project record', () => {
  const state = terminalReportFormState(null, proposal, { today: TODAY })
  const by = byLabel(state)
  assert.equal(by['Program Title'], 'Fisheries Program')
  assert.equal(by['Project Title'], 'Fish Landing Survey')
  assert.equal(by['Project Leader/Gender'], 'Juan Dela Cruz / Male')
  assert.equal(by['Agency'], 'College of Marine Sciences')
  assert.equal(by['Address/Email'], 'Naawan, Misamis Oriental / juan@msunaawan.edu.ph')
  assert.equal(by['Cooperating Agency'], 'LGU Naawan')
  assert.equal(by['Base Station'], '', 'a value with no source stays empty for the proponent')
})

test('a saved detail wins over the prefill, so a reopened draft keeps the edit', () => {
  const saved = {
    summaryRows: [{ label: 'Base Station', detail: 'Naawan Marine Station' }],
    summaryTotalBudget: 'PHP 25,000.00',
  }
  const state = terminalReportFormState(saved, proposal, { today: TODAY })
  const by = byLabel(state)
  assert.equal(by['Base Station'], 'Naawan Marine Station', 'the entered value is kept')
  assert.equal(by['Project Title'], 'Fish Landing Survey', 'a row with no saved value is prefilled')
  assert.equal(state.summaryTotalBudget, 'PHP 25,000.00')
})

test('an older report that stored split rows is folded into the combined rows', () => {
  const rows = terminalSummaryRows([
    { label: 'Project Leader', detail: 'Juan Dela Cruz' },
    { label: 'Gender', detail: 'Male' },
    { label: 'Address', detail: 'Naawan' },
    { label: 'Email', detail: 'juan@msunaawan.edu.ph' },
  ])
  const by = Object.fromEntries(rows.map((row) => [row.label, row.detail]))
  assert.equal(by['Project Leader/Gender'], 'Juan Dela Cruz / Male')
  assert.equal(by['Address/Email'], 'Naawan / juan@msunaawan.edu.ph')
})

test('the edited details survive a save and reopen round-trip', () => {
  const state = terminalReportFormState(null, proposal, { today: TODAY })
  const rows = Object.fromEntries(state.summaryRows.map((row) => [row.label, row]))
  rows['Base Station'].detail = 'Naawan Marine Station'
  rows['Area Covered'].detail = 'Coastal barangays of Naawan'
  rows['Production Comparison'].detail = '12 t increase over the baseline'
  state.summaryTotalBudget = '25000'
  state.summarySourceOfFunds = 'MSU Naawan'
  state.summaryObjectives = [
    { objective: 'Assess landing volume', accomplishment: 'Surveyed 5 sites' },
  ]

  const payload = terminalReportPayload(state)
  const reopened = terminalReportFormState(payload, proposal, { today: TODAY })
  const by = byLabel(reopened)
  assert.equal(by['Base Station'], 'Naawan Marine Station')
  assert.equal(by['Area Covered'], 'Coastal barangays of Naawan')
  assert.equal(by['Production Comparison'], '12 t increase over the baseline')
  assert.equal(reopened.summaryTotalBudget, '25000')
  assert.equal(reopened.summarySourceOfFunds, 'MSU Naawan')
  assert.deepEqual(reopened.summaryObjectives, [
    { objective: 'Assess landing volume', accomplishment: 'Surveyed 5 sites' },
  ])
})

test('money is formatted as PHP with two decimals', () => {
  assert.equal(formatPhpAmount('25000'), 'PHP 25,000.00')
  assert.equal(formatPhpAmount('1234.5'), 'PHP 1,234.50')
  assert.equal(formatPhpAmount('PHP 25,000.00'), 'PHP 25,000.00', 'an already-formatted value is kept')
  assert.equal(formatPhpAmount(''), '')
  assert.equal(formatPhpAmount('to be determined'), 'to be determined')
})

test('the summary sheet keeps its three sections and edits the Details column', () => {
  assert.ok(FORM.includes('SUMMARY FOR THE TERMINAL REPORT'), 'the sheet keeps its heading')
  const order = [
    TERMINAL_SUMMARY_HEADINGS.projectInfo,
    TERMINAL_SUMMARY_HEADINGS.accomplishments,
    TERMINAL_SUMMARY_HEADINGS.financial,
  ].map((heading) => {
    assert.ok(FORM.includes(heading), `the summary shows "${heading}"`)
    return FORM.indexOf(heading)
  })
  assert.ok(order[0] < order[1] && order[1] < order[2], 'the sections appear in document order')
  assert.ok(FORM.includes('tr-summary-table'), 'each section is a table')
  assert.ok(FORM.includes('v-model="row.detail"'), 'the project information Details column is editable')
  assert.ok(FORM.includes('v-model="model.summaryTotalBudget"'), 'the total budget is editable')
  assert.ok(FORM.includes('v-model="model.summarySourceOfFunds"'), 'the source of fund is editable')
  assert.ok(!FORM.includes('financialRows'), 'no removed helper is referenced')
  assert.ok(!FORM.includes('summaryRows.value'), 'the rows are read from the model, not a removed computed')
})
