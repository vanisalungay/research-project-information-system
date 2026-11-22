/**
 * dashboardProposalLimits.test.js
 * ---------------------------------------------------------------------------
 * Every dashboard proposal preview is limited by the shared helper, not by CSS
 * and not by an un-sorted slice of the API response. The full authorized list is
 * still available to the dashboard for its statistics and for "View all".
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const dashboards = {
  RPS_STAFF: read('../views/Dashboard/RpsStaffDashboard.vue'),
  OVCRIGE: read('../views/Dashboard/OvcrigeDashboard.vue'),
  REC: read('../views/Dashboard/RecDashboard.vue'),
  OC: read('../views/Dashboard/OCDashboard.vue'),
  OVCAF: read('../views/Dashboard/OvcafDashboard.vue'),
}

test('every applicable dashboard uses the shared latest-five helper', () => {
  for (const [role, source] of Object.entries(dashboards)) {
    assert.ok(source.includes("from '@/utils/dashboardProposals'"),
      `${role} must import the shared dashboard proposal limit`)
    assert.ok(source.includes('latestProposalSnippets('),
      `${role} must build its previews with latestProposalSnippets`)
    assert.ok(!/\.slice\(0,\s*5\)/.test(source),
      `${role} must not use an un-sorted slice for its proposal preview`)
  }
})

test('RPS Staff previews the latest five in every proposal section', () => {
  const source = dashboards.RPS_STAFF
  for (const preview of ['endorsedPreview', 'revisionPreview', 'filteredBudgetPreview', 'awaitingAction']) {
    assert.ok(source.includes(`v-for="prop in ${preview}"`) || source.includes(`v-for="prop in ${preview}"`),
      `the ${preview} preview must be rendered from the shared helper`)
  }
  // Each preview is ordered by the event it represents, matching its full listing page.
  assert.match(source, /latestProposalSnippets\(endorsedProposals\.value, \{ dateField: 'endorsedAt' \}\)/)
  assert.match(source, /latestProposalSnippets\(revisionRequests\.value, \{ dateField: 'returnedAt' \}\)/)
  assert.match(source, /latestProposalSnippets\(pendingReviewList\.value, \{ dateField: 'submittedAt' \}\)/)
  assert.match(source, /latestProposalSnippets\(filteredBudget\.value, \{ dateField: 'updatedAt' \}\)/)
})

test('OVCRIGE shows the latest five endorsed proposals, newest submission first', () => {
  const source = dashboards.OVCRIGE
  assert.match(source, /v-for="proposal in latestEndorsed"/)
  assert.match(
    source,
    /latestProposalSnippets\(\s*proposals\.value\.filter\(isOvcrigePipelineProposal\),\s*\{ dateField: 'submittedAt', fallbackDateFields: \['createdAt'\] \},?\s*\)/,
  )
})

test('REC shows the latest five assigned proposals', () => {
  const source = dashboards.REC
  assert.match(source, /v-for="proposal in latestProposals"/)
  assert.match(source, /latestProposalSnippets\(proposals\.value, \{ dateField: 'createdAt' \}\)/)
})

test('OC previews five per section, newest submission first', () => {
  const source = dashboards.OC
  assert.match(source, /v-for="p in visibleForFinalApproval"/)
  assert.match(source, /v-for="p in visibleApprovedProposals"/)
  assert.match(source, /const SUBMITTED_DATE_OPTIONS = \{ dateField: 'submittedAt', fallbackDateFields: \[\] \}/)
  assert.match(source, /latestProposalSnippets\(forFinalApproval\.value, SUBMITTED_DATE_OPTIONS\)/)
  assert.match(source, /latestProposalSnippets\(approvedProposals\.value, SUBMITTED_DATE_OPTIONS\)/)
  assert.ok(source.includes("query: { view: 'all' }"),
    'View all must open the full-listing view')
  assert.ok(source.includes('showAll.value ? forFinalApproval.value : forFinalApprovalPreview.value'))
  assert.ok(source.includes('showAll.value ? approvedProposals.value : approvedProposalsPreview.value'))
})

test('OVCAF previews five per section and links to the full Proposals page', () => {
  const source = dashboards.OVCAF
  assert.match(source, /v-for="proposal in forReviewPreview"/)
  assert.match(source, /v-for="proposal in completedPreview"/)
  assert.match(source, /latestProposalSnippets\(forReview\.value, \{ dateField: 'updatedAt' \}\)/)
  assert.match(source, /latestProposalSnippets\(completed\.value, \{ dateField: 'updatedAt' \}\)/)
  assert.ok(source.includes('to="/ovcaf/proposals"'), 'the full OVCAF listing must remain reachable')
})

test('existing View All destinations are preserved', () => {
  assert.ok(dashboards.RPS_STAFF.includes("$router.push('/rpsendorsed-prop')"))
  assert.ok(dashboards.RPS_STAFF.includes("$router.push('/rps-revision-requests')"))
  assert.ok(dashboards.RPS_STAFF.includes("$router.push('/budget-processing')"))
  assert.ok(dashboards.RPS_STAFF.includes("$router.push('/rps-subproposal')"))
  assert.ok(dashboards.OVCRIGE.includes("router.push('/submit-proposals')"))
  assert.ok(dashboards.REC.includes("router.push('/assigned-proposals')"))
})

test('the dashboards without a proposal preview are unchanged in this respect', () => {
  const proponent = read('../views/Dashboard/ProponentDashboard.vue')
  const dean = read('../views/Dashboard/DeanDashboard.vue')
  const rpsAdmin = read('../views/Dashboard/RpsAdminDashboard.vue')

  // Proponent: proposal cards are aggregate bucket counts, not a proposal list.
  assert.ok(!proponent.includes('latestProposalSnippets'))
  assert.ok(proponent.includes('proposalCards'))
  // Dean: the queue and the recent decisions are endorsement requests (decided is already 5).
  assert.ok(!dean.includes('latestProposalSnippets'))
  assert.ok(dean.includes('.slice(0, 5)'))
  // RPS Admin: the only preview is the pending-user list (already five).
  assert.ok(!rpsAdmin.includes('latestProposalSnippets'))
  assert.ok(rpsAdmin.includes('.slice(0, 5)'))
})
