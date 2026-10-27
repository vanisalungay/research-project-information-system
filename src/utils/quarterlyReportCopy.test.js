/**
 * quarterlyReportCopy.test.js
 * Verifies the "Create a Copy" mapping of the proponent's quarterly reporting: every
 * eligible editable field of the previous report reaches the new report form (including
 * the objectives' measured accomplishments and percentages, the 6Ps actual outputs and
 * the whole Project Workplan), the new report keeps its OWN reporting period / type /
 * status / identifier, no system-managed field travels, nested rows are deep-copied, and
 * an existing draft is never overwritten.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  EXCLUDED_REPORT_FIELDS,
  OBJECTIVE_FIELDS,
  OUTPUT_FIELDS,
  WORKPLAN_FIELDS,
  WORKPLAN_OBJECTIVE_FIELDS,
  copyReportValues,
  copySourceId,
  copyWorkplanValues,
} from './quarterlyReportCopy.js'

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
  updatedAt: '2026-04-05T09:30:00',
  copiedFromReportId: null,
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
      {
        objective: 'Identify the fishes at market sites',
        months: ['Process PRs', '', '', 'Sample the landing sites'],
        total: '100%',
      },
    ],
    remarks: 'On track.',
  },
  workplanFileName: 'workplan.pdf',
  workplanFilePath: 'uploads/reports/abc_workplan.pdf',
  terminalReportFileName: null,
  terminalReportFilePath: null,
}

/** The blank next-report state the existing form builds for a new report. */
const newReportTarget = {
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

test('the copy source id is read from the query value, or rejected', () => {
  assert.equal(copySourceId('11'), 11)
  assert.equal(copySourceId(11), 11)
  assert.equal(copySourceId('11.0'), 11)
  assert.equal(copySourceId(null), null)
  assert.equal(copySourceId(undefined), null)
  assert.equal(copySourceId(''), null)
  assert.equal(copySourceId('abc'), null)
  assert.equal(copySourceId('0'), null)
  assert.equal(copySourceId('-3'), null)
  assert.equal(copySourceId('1.5'), null)
})

test('the project identification and the narrative sections are copied', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  assert.equal(copied.projectTitle, 'Fish Landing Survey')
  assert.equal(copied.projectLeader, 'Juan Dela Cruz')
  assert.equal(copied.leaderGender, 'Male')
  assert.equal(copied.baseStation, 'MSU Naawan')
  assert.equal(copied.sitesOfImplementation, 'Naawan, Initao')
  assert.equal(copied.projectDuration, '12')
  assert.equal(copied.projectStartDate, '2026-01-01')
  assert.equal(copied.projectEndDate, '2026-12-31')
  assert.equal(copied.catchUpPlan, 'Catch up on month 2 sampling.')
  assert.equal(copied.problemsConcerns, 'Weather delayed the field work.')
  assert.equal(copied.suggestedSolutions, 'Move sampling to the dry season.')
})

test('the objectives table travels completely, including the accomplishments', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  assert.deepEqual(copied.objectives, [
    {
      objective: 'Identify the fishes at market sites',
      target: '100%',
      actual: '80%',
      percentagePeriod: '80%',
      percentageCumulative: '40%',
    },
  ])
  for (const field of OBJECTIVE_FIELDS) {
    assert.equal(copied.objectives[0][field], sourceReport.objectives[0][field], field)
  }
})

test('the 6Ps table travels completely, including the actual outputs', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  assert.deepEqual(copied.outputs, [
    {
      outputType: 'Publications',
      expectedOutput: 'One journal article',
      actualOutput: 'Draft only',
    },
    { outputType: 'Patents/IP', expectedOutput: '', actualOutput: '' },
  ])
  for (const field of OUTPUT_FIELDS) {
    assert.equal(copied.outputs[0][field], sourceReport.outputs[0][field], field)
  }
})

test('the whole Project Workplan travels, including the as-of date and remarks', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  assert.notEqual(copied.workplan, sourceReport.workplan, 'the workplan is copied, not shared')
  for (const field of WORKPLAN_FIELDS) {
    assert.equal(copied.workplan[field], sourceReport.workplan[field], field)
  }
  for (const field of WORKPLAN_OBJECTIVE_FIELDS.filter((f) => f !== 'months')) {
    assert.equal(
      copied.workplan.objectives[0][field],
      sourceReport.workplan.objectives[0][field],
      field,
    )
  }
  assert.deepEqual(copied.workplan.objectives[0].months, [
    'Process PRs',
    '',
    '',
    'Sample the landing sites',
    '',
    '',
    '',
    '',
    '',
    '',
    '',
    '',
  ])
})

test('the new report keeps its own reporting period, type and status', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  assert.equal(copied.period, 'Q2', 'the previous quarter is never copied')
  assert.equal(copied.reportType, 'QUARTERLY_PROGRESS')
  assert.equal(copied.status, undefined, 'the new report is saved as a draft by the workflow')
  assert.equal(copied.id, undefined, 'the new report gets its own identifier')
  assert.equal(copied.year, undefined, 'the reporting year belongs to the new report')
})

test('no system-managed field of the source report travels', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  // Every excluded field keeps exactly the value the existing workflow prepared for the
  // new report (or stays absent) - never the source report's value.
  for (const field of EXCLUDED_REPORT_FIELDS) {
    assert.equal(copied[field], newReportTarget[field], `${field} must keep its own value`)
  }
  assert.equal(copied.period, 'Q2', 'the reporting period comes from the workflow')
  assert.equal(copied.reportType, 'QUARTERLY_PROGRESS')
  assert.equal(copied.id, undefined, 'the new report gets its own identifier')
  assert.equal(copied.status, undefined, 'the status is set when the new report is saved')
  assert.equal(copied.year, undefined, 'the reporting year belongs to the new report')
  assert.equal(copied.submittedAt, undefined)
  assert.equal(copied.submittedBy, undefined)
  assert.equal(copied.createdBy, undefined)
  assert.equal(copied.createdAt, undefined)
  assert.equal(copied.updatedAt, undefined)
  assert.equal(copied.workplanFileName, undefined)
  assert.equal(copied.workplanFilePath, undefined)
  assert.equal(copied.terminalReportFileName, undefined)
  assert.equal(copied.terminalReportFilePath, undefined)
  assert.equal(copied.copiedFromReportId, undefined)
})

test('an eligible field the copy handler never named is still carried', () => {
  const source = { ...sourceReport, projectStatusNote: 'A field added to the report later' }
  const copied = copyReportValues(source, newReportTarget)

  assert.equal(copied.projectStatusNote, 'A field added to the report later')
})

test('valid values such as 0 and false are carried, not replaced by defaults', () => {
  const source = { ...sourceReport, projectDuration: 0, projectEndDate: false, catchUpPlan: '0' }
  const copied = copyReportValues(source, newReportTarget)

  assert.equal(copied.projectDuration, 0)
  assert.equal(copied.projectEndDate, false)
  assert.equal(copied.catchUpPlan, '0')
})

test('nested rows are deep-copied: editing the copy never changes the source report', () => {
  const copied = copyReportValues(sourceReport, newReportTarget)

  copied.objectives[0].actual = 'edited'
  copied.objectives[0].target = 'edited'
  copied.outputs[0].expectedOutput = 'edited'
  copied.outputs[0].actualOutput = 'edited'
  copied.workplan.objectives[0].months[0] = 'edited'
  copied.workplan.remarks = 'edited'

  assert.equal(sourceReport.objectives[0].actual, '80%')
  assert.equal(sourceReport.objectives[0].target, '100%')
  assert.equal(sourceReport.outputs[0].expectedOutput, 'One journal article')
  assert.equal(sourceReport.outputs[0].actualOutput, 'Draft only')
  assert.equal(sourceReport.workplan.objectives[0].months[0], 'Process PRs')
  assert.equal(sourceReport.workplan.remarks, 'On track.')
})

test('an existing draft keeps its own content and only gains what it is missing', () => {
  const draft = {
    id: 21,
    period: 'Q2',
    reportType: 'QUARTERLY_PROGRESS',
    status: 'DRAFT',
    projectTitle: 'Fish Landing Survey (renamed)',
    catchUpPlan: 'Already written for Q2.',
    objectives: [
      {
        objective: 'My own Q2 objective',
        target: '50%',
        actual: '10%',
        percentagePeriod: '10%',
        percentageCumulative: '5%',
      },
    ],
    outputs: [{ outputType: 'Publications', expectedOutput: 'Two articles', actualOutput: 'One' }],
  }

  const copied = copyReportValues(sourceReport, draft, { fillEmptyOnly: true })

  // Saved draft content always wins.
  assert.equal(copied.projectTitle, 'Fish Landing Survey (renamed)')
  assert.equal(copied.catchUpPlan, 'Already written for Q2.')
  assert.deepEqual(copied.objectives, draft.objectives)
  assert.deepEqual(copied.outputs, draft.outputs)
  assert.equal(copied.id, 21, 'the existing draft is reused, never replaced')
  assert.equal(copied.status, 'DRAFT')

  // Missing values are filled in from the previous report.
  assert.equal(copied.problemsConcerns, 'Weather delayed the field work.')
  assert.equal(copied.suggestedSolutions, 'Move sampling to the dry season.')
  assert.equal(copied.baseStation, 'MSU Naawan')
  assert.equal(copied.projectEndDate, '2026-12-31')
})

test('an existing draft without a plan yet is filled in from the previous report', () => {
  const emptyDraft = {
    id: 22,
    period: 'Q2',
    status: 'DRAFT',
    objectives: [
      { objective: '', target: '', actual: '', percentagePeriod: '', percentageCumulative: '' },
    ],
    outputs: [],
  }

  const copied = copyReportValues(sourceReport, emptyDraft, { fillEmptyOnly: true })

  assert.equal(copied.objectives.length, 1)
  assert.equal(copied.objectives[0].objective, 'Identify the fishes at market sites')
  assert.equal(copied.objectives[0].actual, '80%')
  assert.equal(copied.outputs[0].expectedOutput, 'One journal article')
  assert.equal(copied.outputs[0].actualOutput, 'Draft only')
  assert.equal(copied.id, 22)
})

test('a draft workplan that already describes an objective is never replaced', () => {
  const draftWorkplan = {
    asOf: '2026-06-30',
    objectives: [{ objective: 'My own Q2 workplan objective', months: [], total: '' }],
    remarks: 'Q2 remarks.',
  }

  const copied = copyWorkplanValues(sourceReport.workplan, draftWorkplan, { fillEmptyOnly: true })

  assert.deepEqual(copied.objectives, draftWorkplan.objectives)
  assert.equal(copied.asOf, '2026-06-30', 'the draft keeps its own reporting date')
  assert.equal(copied.remarks, 'Q2 remarks.')
  assert.equal(copied.projectTitle, 'Fish Landing Survey', 'a missing header is filled in')
})

test('a draft workplan without a plan is filled in from the previous report', () => {
  const copied = copyWorkplanValues(sourceReport.workplan, { objectives: [] })

  assert.equal(copied.asOf, '2026-03-31')
  assert.equal(copied.remarks, 'On track.')
  assert.equal(copied.objectives.length, 1)
  assert.equal(copied.objectives[0].total, '100%')
  assert.equal(copied.objectives[0].months[0], 'Process PRs')
})

test('a missing or unusable source leaves the target untouched', () => {
  assert.deepEqual(copyReportValues(null, newReportTarget), newReportTarget)
  assert.deepEqual(copyReportValues(undefined, newReportTarget), newReportTarget)
  assert.deepEqual(copyReportValues({}, newReportTarget), newReportTarget)

  const target = { period: 'Q2', objectives: [], outputs: [] }
  assert.deepEqual(copyReportValues(null, target), target)
  assert.deepEqual(copyWorkplanValues(null, {}), {})
  assert.deepEqual(copyWorkplanValues(undefined, { asOf: 'x' }), { asOf: 'x' })
})

test('a source without a plan keeps the target rows and never breaks the copy', () => {
  const source = { ...sourceReport, objectives: [], outputs: [], workplan: { objectives: [] } }
  const copied = copyReportValues(source, newReportTarget)

  assert.deepEqual(copied.objectives, [])
  assert.deepEqual(copied.outputs, [])
  assert.equal(copied.projectTitle, 'Fish Landing Survey')
  assert.deepEqual(copyWorkplanValues(source.workplan, { objectives: [] }).objectives, [])
})

test('blank rows are not carried, but a row with only measured results is kept', () => {
  const source = {
    ...sourceReport,
    objectives: [
      { objective: '   ', target: '', actual: '', percentagePeriod: '', percentageCumulative: '' },
      { objective: 'Keep this one', target: '75%', actual: '50%' },
      { objective: '', target: '', actual: 'Reported late', percentagePeriod: '20%' },
    ],
  }
  const copied = copyReportValues(source, newReportTarget)

  assert.equal(copied.objectives.length, 2)
  assert.equal(copied.objectives[0].objective, 'Keep this one')
  assert.equal(copied.objectives[1].actual, 'Reported late')
  assert.equal(copied.objectives[1].percentagePeriod, '20%')
})

test('a workplan row with only month cells or a total is kept as well', () => {
  const source = {
    ...sourceReport,
    workplan: {
      ...sourceReport.workplan,
      objectives: [
        { objective: '', months: ['Field work'], total: '' },
        { objective: 'Objective two', months: [], total: '50%' },
        { objective: '', months: [], total: '' },
      ],
    },
  }
  const copied = copyWorkplanValues(source.workplan, {})

  assert.equal(copied.objectives.length, 2)
  assert.equal(copied.objectives[0].months[0], 'Field work')
  assert.equal(copied.objectives[1].total, '50%')
  assert.equal(copied.objectives[0].months.length, 12, 'every row carries twelve month cells')
})
