/**
 * terminalReportCompletion.test.js
 * ---------------------------------------------------------------------------
 * Guards the completion indicator of the Proponent's Terminal Report section navigation.
 *
 * A step counts as complete only when every part the format requires is present - a step is
 * never complete merely because it was opened or saved, and a value that only the project
 * record supplied (the pre-filled cover title, the pre-filled summary rows) never completes a
 * step on its own. The Cover Page was the case that was wrong: it showed a green check as soon
 * as RPIS printed the automatically populated title.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import {
  emptyTerminalReport,
  terminalReportFormState,
  terminalSectionFilled,
} from './terminalReport.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const VIEW = read('../views/Proponent/ProponentQuarterlyReportForm.vue')

const TODAY = new Date('2026-10-26T00:00:00Z')
const proposal = {
  programTitle: 'Fisheries Program',
  projectTitle: 'Fish Landing Survey',
  projectLeader: 'Juan Dela Cruz',
  college: 'College of Marine Sciences',
  cooperatingAgencies: 'LGU Naawan',
  proponent: { email: 'juan@msunaawan.edu.ph' },
}

const newReport = () => terminalReportFormState(null, proposal, { today: TODAY })

test('a new report with only the pre-filled cover values is not complete', () => {
  const report = newReport()
  assert.ok(report.projectTitle, 'the project title is pre-filled from the project record')
  assert.ok(!report.projectTeam, 'the project team starts empty')
  assert.equal(terminalSectionFilled(report, 'cover'), false)
})

test('the cover completes only when every required value is present', () => {
  const report = newReport()

  // Only some required values: the project team is filled, the title is missing.
  report.projectTitle = ''
  report.projectTeam = 'Juan Dela Cruz'
  assert.equal(terminalSectionFilled(report, 'cover'), false, 'a partial cover stays incomplete')

  // Every required value present.
  report.projectTitle = 'Fish Landing Survey'
  assert.equal(terminalSectionFilled(report, 'cover'), true)
})

test('clearing a required cover value takes the check away again', () => {
  const report = newReport()
  report.projectTeam = 'Juan Dela Cruz'
  assert.equal(terminalSectionFilled(report, 'cover'), true)

  report.projectTeam = '   '
  assert.equal(
    terminalSectionFilled(report, 'cover'),
    false,
    'a whitespace-only value does not count'
  )
})

test('reopening a draft keeps its completion status', () => {
  const incomplete = { ...newReport(), projectTitle: 'Fish Landing Survey', projectTeam: '' }
  const reopenedIncomplete = terminalReportFormState(incomplete, proposal, { today: TODAY })
  assert.equal(terminalSectionFilled(reopenedIncomplete, 'cover'), false, 'an incomplete draft stays incomplete')

  const complete = {
    ...newReport(),
    projectTitle: 'Fish Landing Survey',
    projectTeam: 'Juan Dela Cruz',
  }
  const reopenedComplete = terminalReportFormState(complete, proposal, { today: TODAY })
  assert.equal(terminalSectionFilled(reopenedComplete, 'cover'), true, 'a complete draft stays complete')
})

test('saving a draft never marks a step complete on its own', () => {
  const report = emptyTerminalReport()
  report.acknowledgment = 'Thanks to everyone.'
  assert.equal(terminalSectionFilled(report, 'cover'), false)
  assert.equal(terminalSectionFilled(report, 'preliminary'), false)
  assert.equal(terminalSectionFilled(report, 'resultsAndDiscussion'), false)
})

test("the summary needs the proponent's own content, not the pre-filled rows", () => {
  const report = newReport()
  assert.equal(terminalSectionFilled(report, 'summary'), false)
  report.summaryTotalBudget = 'PHP 25,000.00'
  assert.equal(terminalSectionFilled(report, 'summary'), true)
})

test('a submission that completes the project is shown immediately from the response', () => {
  assert.ok(
    VIEW.includes("report.value?.proposalStatus === 'COMPLETED'"),
    'the form reads the proposal status returned by the submission response'
  )
  assert.ok(
    VIEW.includes('This project is now marked'),
    'the proponent is told the project completed without reopening it'
  )
})

test('the section being edited is never shown as complete', () => {
  assert.ok(
    VIEW.includes("if (index === terminalSectionIndex.value) return 'current'"),
    'the current step keeps its own indicator'
  )
  assert.ok(VIEW.includes("? 'done' : 'upcoming'"), 'only a non-current step can be marked done')
  assert.ok(
    VIEW.includes('terminalSectionFilled(terminalReport'),
    'completion is derived from the report, not a stored flag'
  )
})
