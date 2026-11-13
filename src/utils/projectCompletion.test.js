/**
 * projectCompletion.test.js
 * ---------------------------------------------------------------------------
 * Guards the completed-project state as the frontend sees it.
 *
 * The completion transition itself is made and persisted by the backend
 * (QuarterlyProgressReportService): once Q1, Q2, Q3 and the Terminal Report are all
 * SUBMITTED the project's stored status becomes COMPLETED. The frontend must then:
 *
 *   1. display COMPLETED as "Completed" everywhere - never as "Implementation
 *      Approved" and never the other way round;
 *   2. keep the completed project's reports visible;
 *   3. never offer a new reporting stage: creating a report still requires an
 *      approved-for-implementation project (or the same approval chain), so a
 *      COMPLETED project cannot start a fifth stage.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  REPORTABLE_STATUSES,
  isQuarterlyReportEligible,
  isReportableProject,
} from './quarterlyReport.js'
import {
  isCompletedStatus,
  proposalStatusLabel,
  proposalStatusMatches,
} from './proposalStatus.js'
import { reviewStatusLabel } from './reviewStatus.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const REPORTS_PAGE = read('../views/Proponent/ProponentQuarterlyReports.vue')
const LAUNCHER = read('../composables/useQuarterlyReportLauncher.js')

const issued = (status) => ({
  status,
  soNumber: 'SO-2026-001',
  soUploadedAt: '2026-07-28T23:20:00',
})

test('1-4. a project is completed only in the completed status, never while reporting', () => {
  // The stored completion status is the only one that reads Completed; the statuses a
  // project still being implemented carries never do.
  assert.equal(proposalStatusLabel('COMPLETED'), 'Completed')
  for (const status of ['APPROVED', 'RELEASED', 'IMPLEMENTATION']) {
    assert.notEqual(proposalStatusLabel(status), 'Completed', status)
    assert.equal(isCompletedStatus(status), false, status)
  }
  assert.equal(isCompletedStatus('COMPLETED'), true)
})

test('5. every role-facing label helper shows a completed project as Completed', () => {
  // Listing pages use proposalStatusLabel; review/detail pages use reviewStatusLabel.
  // Both delegate to the shared catalogue, so the wording cannot differ by role.
  assert.equal(proposalStatusLabel('COMPLETED'), 'Completed')
  assert.equal(reviewStatusLabel('COMPLETED'), 'Completed')
  assert.equal(proposalStatusLabel(' completed '), 'Completed')
  assert.equal(reviewStatusLabel('completed'), 'Completed')

  // And a completed project is never treated as merely approved for implementation.
  assert.equal(proposalStatusMatches('COMPLETED', 'APPROVED'), false)
  assert.equal(proposalStatusMatches('APPROVED', 'COMPLETED'), false)
  assert.notEqual(proposalStatusLabel('COMPLETED'), proposalStatusLabel('APPROVED'))
})

test('a completed project keeps its reports visible', () => {
  assert.equal(isReportableProject(issued('COMPLETED')), true)
  // Same approval-chain evidence is still required; COMPLETED alone is not enough.
  assert.equal(isReportableProject({ status: 'COMPLETED', soNumber: 'SO-2026-001' }), false)
  assert.equal(isReportableProject({ status: 'COMPLETED', soNumber: '', soUploadedAt: 'x' }), false)
  assert.equal(isReportableProject(null), false)
})

test('a completed project can never start another reporting stage', () => {
  // Creation keeps the strict implementation statuses; COMPLETED is read-only.
  assert.equal(isQuarterlyReportEligible(issued('COMPLETED')), false)
  for (const status of ['APPROVED', 'RELEASED', 'IMPLEMENTATION']) {
    assert.equal(isQuarterlyReportEligible(issued(status)), true, status)
  }
  assert.ok(REPORTABLE_STATUSES.includes('COMPLETED'))
  assert.ok(!REPORTABLE_STATUSES.some((status) => !status))
})

test('the reports page separates viewing from creating', () => {
  assert.ok(
    REPORTS_PAGE.includes('isReportableProject(project.value)'),
    'the page decides whether reports are visible through isReportableProject',
  )
  assert.ok(
    REPORTS_PAGE.includes('isQuarterlyReportEligible(project.value)'),
    'starting a new stage still uses the strict eligibility rule',
  )
  assert.ok(
    REPORTS_PAGE.includes(':disabled="!canCreateReport || allStagesComplete"'),
    'the create action is disabled for a completed project',
  )
  assert.ok(
    REPORTS_PAGE.includes('if (reportingAvailable.value)'),
    'a completed project still resolves its reporting stage',
  )
})

test('the create-report launcher keeps the strict creation rule', () => {
  assert.ok(
    LAUNCHER.includes('isQuarterlyReportEligible'),
    'monitoring and project details offer the action only for a reportable-in-progress project',
  )
  assert.ok(
    !LAUNCHER.includes('isReportableProject'),
    'the launcher never treats a completed project as one that can start a new stage',
  )
})
