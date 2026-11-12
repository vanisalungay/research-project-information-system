/**
 * proponentQuarterlyReportEntry.test.js
 * ---------------------------------------------------------------------------
 * Guards for the placement, destination and eligibility of the "Create
 * Quarterly Report" action.
 *
 * The action belongs to exactly two places in the proponent interface:
 *   - Project Details, beside Download;
 *   - Proposal Monitoring, beside View Details.
 * It must not appear on the My Projects dashboard. Both places open the existing
 * Quarterly Progress Reports overview through one shared launcher, which reads
 * the one eligibility rule (utils/quarterlyReport.js): approved for
 * implementation AND an issued Special Order. The overview page resolves the
 * next required stage itself; only its Start Quarterly Report button opens the
 * report form. The backend validates the same rule again.
 *
 * The pages are validated at the source level, the technique the other guard
 * tests use, because they import Vite `?inline` assets Node cannot resolve.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const DETAILS = read('../views/Proponent/ProponentDetiailedProp.vue')
const PROJECTS = read('../views/Proponent/ProponentProposals.vue')
const MONITORING = read('../views/Proponent/ProponentProposalMonitoring.vue')
const REPORTS = read('../views/Proponent/ProponentQuarterlyReports.vue')
const FORM = read('../views/Proponent/ProponentQuarterlyReportForm.vue')
const LAUNCHER = read('../composables/useQuarterlyReportLauncher.js')
const ROUTER = read('../router/index.js')

const indexOf = (source, needle) => {
  const index = source.indexOf(needle)
  assert.notEqual(index, -1, `expected to find: ${needle}`)
  return index
}

test('the My Projects dashboard no longer offers the quarterly report action', () => {
  assert.ok(!PROJECTS.includes('Create Quarterly Report'), 'no action on the dashboard')
  assert.ok(!PROJECTS.includes('useQuarterlyReportLauncher'), 'it does not open the workflow')
  assert.ok(!PROJECTS.includes('next-stage'), 'it does not resolve a report stage')
  assert.ok(!PROJECTS.includes('isQuarterlyReportEligible'), 'it has no QPR eligibility logic')
})

test('Project Details places Create Quarterly Report beside Download', () => {
  const header = DETAILS.slice(
    indexOf(DETAILS, '<div class="header-actions">'),
    indexOf(DETAILS, '</header>'),
  )
  const download = indexOf(header, '@click="printProposal"')
  const create = indexOf(header, 'Create Quarterly Report')
  assert.ok(create > download, 'the action follows Download in the header actions')
  assert.ok(
    DETAILS.includes('v-if="canCreateQuarterlyReport(proposal)"'),
    'shown only for an eligible project',
  )
  assert.ok(
    DETAILS.includes('openQuarterlyReport(proposal)'),
    'it opens the workflow for the project being viewed',
  )
})

test('Proposal Monitoring places Create Quarterly Report beside View Details', () => {
  const actionsStart = indexOf(MONITORING, '<div class="card-actions">')
  const actionsEnd = MONITORING.indexOf('</div>', indexOf(MONITORING, 'Submit Revision', actionsStart))
  const actions = MONITORING.slice(actionsStart, actionsEnd)
  const view = indexOf(actions, 'View Details')
  const create = indexOf(actions, 'Create Quarterly Report')
  assert.ok(create > view, 'the action follows View Details in the same card actions')
  assert.ok(
    MONITORING.includes('v-if="isEligibleForReports(proposal)"'),
    'shown only for an eligible row',
  )
  assert.ok(
    MONITORING.includes('openQuarterlyReport(proposal)'),
    'it opens the workflow for that row project',
  )
})

test('both locations open the overview through the one shared launcher', () => {
  for (const [name, source] of Object.entries({ DETAILS, MONITORING })) {
    assert.ok(source.includes('useQuarterlyReportLauncher'), `${name} uses the shared launcher`)
  }
  assert.ok(LAUNCHER.includes('isQuarterlyReportEligible'), 'one eligibility rule')
  assert.ok(
    LAUNCHER.includes("name: 'ProponentQuarterlyReports'"),
    'the existing overview route is the one destination',
  )
  assert.ok(
    LAUNCHER.includes('params: { proposalId: proposal.id }'),
    'the overview receives the correct proposal id',
  )
  assert.ok(
    !LAUNCHER.includes('/new'),
    'the launcher no longer opens the report form directly',
  )
})

test('the overview page owns the next-stage resolution and the report form action', () => {
  assert.ok(
    ROUTER.includes("path: 'quarterly-reports/:proposalId'") &&
      ROUTER.includes("name: 'ProponentQuarterlyReports'") &&
      ROUTER.includes('component: ProponentQuarterlyReports'),
    'the existing overview route is reused',
  )
  assert.ok(
    ROUTER.includes("path: 'quarterly-reports/:proposalId/new'") &&
      ROUTER.includes("name: 'ProponentQuarterlyReportNew'"),
    'the existing report form route is preserved',
  )
  assert.ok(
    REPORTS.includes('/api/quarterly-reports/next-stage/'),
    'the next required stage comes from the existing endpoint',
  )
  assert.ok(REPORTS.includes('loadNextStage'), 'the overview resolves the stage itself')
  assert.ok(REPORTS.includes('nextStageAction'), 'the Start button is labelled from that stage')
  assert.ok(REPORTS.includes('function createNew'), 'Start opens the existing report form')
  assert.ok(REPORTS.includes('ProponentQuarterlyReportNew'), 'the existing form route is reused')
})

test('the shared rule is not re-implemented inline anywhere', () => {
  for (const [name, source] of Object.entries({ DETAILS, PROJECTS, MONITORING, REPORTS, FORM })) {
    assert.ok(!source.includes('soNumber.trim()'), `${name} does not check the SO itself`)
    assert.ok(
      !/status === 'APPROVED' \|\|/.test(source),
      `${name} does not hard-code the implementation statuses itself`,
    )
  }
  assert.ok(
    MONITORING.includes('const isEligibleForReports = canCreateQuarterlyReport'),
    'monitoring uses the launcher\'s eligibility check',
  )
  assert.ok(
    FORM.includes('isQuarterlyReportEligible(project.value)'),
    'the report form delegates to the shared rule',
  )
})

test('opening the action never creates a report itself', () => {
  assert.ok(!LAUNCHER.includes('api.post('), 'the form writes the report, not the launcher')
  assert.ok(
    !LAUNCHER.includes('/api/quarterly-reports/'),
    'the launcher only navigates; the overview page fetches the reporting state',
  )
  assert.ok(
    REPORTS.includes(':disabled="!canCreateReport || allStagesComplete"'),
    'a completed project cannot start another stage from the overview',
  )
})
