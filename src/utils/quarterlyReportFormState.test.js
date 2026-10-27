/**
 * quarterlyReportFormState.test.js
 * Verifies the destination side of "Create a Copy": the values carried from the previous
 * report reach the form's real reactive state (never replaced by the form's defaults), and
 * the payload the existing save/submit endpoints receive is the same data the form shows.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { copyReportValues } from './quarterlyReportCopy.js'
import {
  OUTPUT_TYPES,
  WORKPLAN_MONTHS,
  emptyObjective,
  reportFormState,
  reportPayload,
  seedOutputs,
  workplanFormState,
  workplanPayload,
} from './quarterlyReportFormState.js'

/** A submitted Q1 2026 report with every eligible field populated. */
const sourceReport = {
  id: 11,
  proposalId: 42,
  period: 'Q1',
  year: 2026,
  reportType: 'QUARTERLY_PROGRESS',
  status: 'SUBMITTED',
  submittedAt: '2026-04-05T09:30:00',
  submittedBy: 7,
  createdBy: 7,
  projectTitle: 'Fish Landing Survey',
  projectLeader: 'Juan Dela Cruz',
  leaderGender: 'Male',
  baseStation: 'MSU Naawan',
  sitesOfImplementation: 'Naawan, Initao',
  projectDuration: '12',
  projectStartDate: '2026-01-01',
  projectEndDate: '2026-12-31',
  catchUpPlan: 'Catch up on month 2 sampling.',
  problemsConcerns: 'Weather delayed the field work.',
  suggestedSolutions: 'Move sampling to the dry season.',
  objectives: [
    {
      objective: 'Identify the fishes at market sites',
      target: '100%',
      actual: '80%',
      percentagePeriod: '80%',
      percentageCumulative: '40%',
    },
  ],
  outputs: [
    {
      outputType: 'Publications',
      expectedOutput: 'One journal article',
      actualOutput: 'Draft only',
    },
    { outputType: 'Patents/IP', expectedOutput: '', actualOutput: '' },
  ],
  workplan: {
    asOf: '2026-03-31',
    projectTitle: 'Fish Landing Survey',
    projectLeader: 'Juan Dela Cruz',
    projectDuration: '12',
    objectives: [
      { objective: 'Identify the fishes at market sites', months: ['Process PRs'], total: '100%' },
    ],
    remarks: 'On track.',
  },
  workplanFileName: 'workplan.pdf',
}

/** The blank next-report state the existing form builds for the new report. */
const nextReportTarget = {
  period: 'Q2',
  reportType: 'QUARTERLY_PROGRESS',
  projectTitle: 'Fish Landing Survey',
  projectLeader: 'Juan Dela Cruz',
  projectDuration: '12',
  projectStartDate: '2026-01-01',
  projectEndDate: '2026-12-31',
  objectives: [],
  outputs: [],
}

const TEXT_FIELDS = [
  'projectTitle',
  'projectLeader',
  'leaderGender',
  'baseStation',
  'sitesOfImplementation',
  'projectDuration',
  'projectStartDate',
  'projectEndDate',
  'catchUpPlan',
  'problemsConcerns',
  'suggestedSolutions',
]

test('a report without data still produces a complete, valid form state', () => {
  const form = reportFormState(null)

  assert.equal(form.period, 'Q1')
  assert.equal(form.year, new Date().getFullYear())
  for (const field of TEXT_FIELDS) assert.equal(form[field], '', field)
  assert.deepEqual(form.objectives, [emptyObjective()])
  assert.deepEqual(form.outputs, seedOutputs())
  assert.equal(form.outputs.length, OUTPUT_TYPES.length)
})

test('a saved report is applied to the form state as it is stored', () => {
  const form = reportFormState(sourceReport)

  assert.equal(form.period, 'Q1')
  assert.equal(form.year, 2026)
  for (const field of TEXT_FIELDS) {
    assert.equal(form[field], sourceReport[field], field)
  }
  assert.equal(form.objectives[0].actual, '80%')
  assert.equal(form.objectives[0].percentagePeriod, '80%')
  assert.equal(form.objectives[0].percentageCumulative, '40%')
  assert.equal(form.outputs[0].expectedOutput, 'One journal article')
  assert.equal(form.outputs[0].actualOutput, 'Draft only')
  assert.equal(form.outputs.length, OUTPUT_TYPES.length)
})

test('every copied field reaches the form state of the new report', () => {
  const form = reportFormState(copyReportValues(sourceReport, nextReportTarget))

  // The reporting stage stays the one the existing workflow prepared.
  assert.equal(form.period, 'Q2')
  assert.equal(form.year, new Date().getFullYear())
  // Every populated content field of the previous report is in the form.
  for (const field of TEXT_FIELDS) {
    assert.equal(form[field], sourceReport[field], field)
  }
  assert.deepEqual(form.objectives, sourceReport.objectives)
  assert.equal(form.outputs[0].actualOutput, 'Draft only')
  assert.equal(form.outputs[5].outputType, 'Policy', 'the 6Ps rows stay in form order')
})

test('the form keeps a row property it does not know yet', () => {
  const form = reportFormState({
    objectives: [{ objective: 'Objective one', target: '100%', extraColumn: 'kept' }],
  })

  assert.equal(form.objectives[0].extraColumn, 'kept')
  assert.equal(form.objectives[0].actual, '', 'missing columns are still defaulted')
})

test('valid falsy values are not replaced by the form defaults', () => {
  const form = reportFormState({ projectDuration: 0, projectEndDate: false, catchUpPlan: '0' })

  assert.equal(form.projectDuration, '0')
  assert.equal(form.projectEndDate, 'false')
  assert.equal(form.catchUpPlan, '0')
})

test('the saved payload carries exactly the values the form shows', () => {
  const form = reportFormState(copyReportValues(sourceReport, nextReportTarget))
  const payload = reportPayload(form)

  assert.equal(payload.period, 'Q2')
  for (const field of TEXT_FIELDS) {
    assert.equal(payload[field], sourceReport[field], field)
  }
  assert.deepEqual(payload.objectives, form.objectives)
  assert.deepEqual(payload.outputs, form.outputs)
  assert.equal(payload.objectives[0].actual, '80%')
  assert.equal(payload.outputs[0].actualOutput, 'Draft only')
  assert.equal(payload.projectStartDate, '2026-01-01')
})

test('an empty date is saved as null, exactly as the existing form does', () => {
  const payload = reportPayload(reportFormState({ projectStartDate: '', projectEndDate: '' }))

  assert.equal(payload.projectStartDate, null)
  assert.equal(payload.projectEndDate, null)
})

test('the workplan form state keeps the whole copied plan', () => {
  const copied = copyReportValues(sourceReport, nextReportTarget)
  const workplan = workplanFormState(copied.workplan, { projectTitle: 'Proposal title' })

  assert.equal(workplan.asOf, '2026-03-31')
  assert.equal(workplan.remarks, 'On track.')
  assert.equal(workplan.projectTitle, 'Fish Landing Survey')
  assert.equal(workplan.projectLeader, 'Juan Dela Cruz')
  assert.equal(workplan.projectDuration, '12')
  assert.equal(workplan.objectives[0].objective, 'Identify the fishes at market sites')
  assert.equal(workplan.objectives[0].total, '100%')
  assert.equal(workplan.objectives[0].months[0], 'Process PRs')
  assert.equal(workplan.objectives[0].months.length, WORKPLAN_MONTHS)
  assert.equal(workplan.objectives.length, 3, 'the editable workplan keeps its minimum rows')
})

test('the workplan payload carries every month cell and value of the form', () => {
  const workplan = workplanFormState(sourceReport.workplan, {})
  const payload = workplanPayload(workplan)

  assert.equal(payload.asOf, '2026-03-31')
  assert.equal(payload.remarks, 'On track.')
  assert.equal(payload.objectives.length, 3)
  assert.equal(payload.objectives[0].objective, 'Identify the fishes at market sites')
  assert.equal(payload.objectives[0].total, '100%')
  assert.equal(payload.objectives[0].months.length, WORKPLAN_MONTHS)
  assert.equal(payload.objectives[0].months[0], 'Process PRs')
  assert.equal(payload.objectives[1].objective, '', 'the padded rows stay empty')
})

test('a workplan with no saved data is seeded from the proposal', () => {
  const workplan = workplanFormState(null, {
    projectTitle: 'Proposal title',
    projectLeader: 'Ana Proponent',
    duration: '18',
  })

  assert.equal(workplan.asOf, '')
  assert.equal(workplan.remarks, '')
  assert.equal(workplan.projectTitle, 'Proposal title')
  assert.equal(workplan.projectLeader, 'Ana Proponent')
  assert.equal(workplan.projectDuration, '18')
  assert.equal(workplan.objectives.length, 3)
  assert.equal(workplan.objectives[0].months.length, WORKPLAN_MONTHS)
})

test('a saved workplan value always wins over the proposal value', () => {
  const workplan = workplanFormState(
    { projectTitle: 'Saved workplan title', projectLeader: 'Saved leader', projectDuration: '24' },
    { projectTitle: 'Proposal title', projectLeader: 'Proposal leader', duration: '12' },
  )

  assert.equal(workplan.projectTitle, 'Saved workplan title')
  assert.equal(workplan.projectLeader, 'Saved leader')
  assert.equal(workplan.projectDuration, '24')
})
