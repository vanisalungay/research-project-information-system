/**
 * projectApprovalDisplay.test.js
 * ---------------------------------------------------------------------------
 * Guards the two presentation rules final approval introduces:
 *
 *   - a proposal stored as APPROVED is shown as a completed project ("Completed"),
 *     never as a pending "Approved" proposal;
 *   - its row action reads "Open" instead of "View", while every earlier status keeps
 *     "View".
 *
 * The pages are read as text - the technique ovcafProposalDisplay.test.js and the dashboard
 * guard tests already use - because they import Vite `?inline` assets Node cannot resolve.
 * The wording rule itself is unit-tested in proposalStatus.test.js; this file checks that
 * every listing page actually goes through the shared helper.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PROPONENT_PROPOSALS = read('../views/Proponent/ProponentProposals.vue')
const PROPONENT_MONITORING = read('../views/Proponent/ProponentProposalMonitoring.vue')
const RPS_MONITORING = read('../views/Rps/RpsProposalMonitoring.vue')
const OVCAF_INBOX = read('../views/Ovcaf/OvcafProposalInbox.vue')
const RPS_BUDGET = read('../views/Rps/RpsBudgetProcessing.vue')
const RPS_STAFF = read('../views/Dashboard/RpsStaffDashboard.vue')
const RPS_ENDORSED = read('../views/Rps/RpsEndorsedProPanel.vue')
const OC_DASHBOARD = read('../views/Dashboard/OCDashboard.vue')
const OVCAF_DASHBOARD = read('../views/Dashboard/OvcafDashboard.vue')

const LISTING_PAGES = [
  ['ProponentProposals.vue', PROPONENT_PROPOSALS],
  ['OCDashboard.vue', OC_DASHBOARD],
  ['OvcafDashboard.vue', OVCAF_DASHBOARD],
  ['RpsStaffDashboard.vue', RPS_STAFF],
  ['RpsBudgetProcessing.vue', RPS_BUDGET],
  ['RpsEndorsedProPanel.vue', RPS_ENDORSED],
]

test('the proposal status filter offers Completed instead of Approved', () => {
  for (const [name, source] of [
    ['ProponentProposals.vue', PROPONENT_PROPOSALS],
    ['ProponentProposalMonitoring.vue', PROPONENT_MONITORING],
    ['RpsProposalMonitoring.vue', RPS_MONITORING],
    ['OvcafProposalInbox.vue', OVCAF_INBOX],
  ]) {
    assert.ok(
      source.includes('<option value="APPROVED">Completed</option>'),
      name + ' must label the APPROVED filter Completed',
    )
    assert.ok(!/>\s*Approved\s*<\/option>/.test(source), name + ' must not offer an Approved option')
  }
})

test('the monitoring filter has one Completed option, backed by the alias', () => {
  const completedOptions = PROPONENT_MONITORING.match(/>\s*Completed\s*<\/option>/g) || []
  assert.equal(completedOptions.length, 1, 'Completed must be offered exactly once')
  assert.ok(!PROPONENT_MONITORING.includes('value="COMPLETED"'), 'the duplicate COMPLETED option is gone')
  assert.ok(
    PROPONENT_MONITORING.includes("const APPROVED_STAGE = { key: 'APPROVED', label: 'Completed' }"),
    'the final timeline stage is labelled Completed',
  )
})

test('the budget filters read Completed and still select the stored APPROVED rows', () => {
  assert.match(
    RPS_BUDGET,
    /filters:\s*\[[^\]]*"Completed"/,
    'the Budget Processing filter button must read Completed',
  )
  assert.ok(
    RPS_BUDGET.includes('"Completed": "APPROVED"'),
    'the Completed button must still map to the stored APPROVED status',
  )
  assert.ok(!RPS_BUDGET.includes('"Approved": "APPROVED"'), 'no Approved status-map key remains')

  assert.match(
    RPS_STAFF,
    /budgetFilters\s*=\s*\[[^\]]*'Completed'/,
    'the staff dashboard budget filter must read Completed',
  )
  assert.ok(
    RPS_STAFF.includes("'Completed': 'APPROVED'"),
    'the Completed chip must still map to the stored APPROVED status',
  )
  assert.ok(!RPS_STAFF.includes("'Approved': 'APPROVED'"), 'no Approved status-map key remains')
})

test('every listing page renders its row action through the shared helper', () => {
  for (const [name, source] of LISTING_PAGES) {
    assert.ok(source.includes('proposalActionLabel('), name + ' must use proposalActionLabel')
    assert.ok(!/>\s*View\s*</.test(source), name + ' must not keep a hardcoded View label')
  }
})

test('the surrounding actions were not renamed', () => {
  for (const source of [PROPONENT_MONITORING, RPS_MONITORING, OVCAF_INBOX]) {
    assert.ok(source.includes('View Details'), 'View Details is outside this change')
  }
  assert.ok(OC_DASHBOARD.includes('Review &amp; Approve'), 'the final-approval action is unchanged')
  assert.match(OVCAF_DASHBOARD, />\s*Review\s*</, 'the OVCAF review action is unchanged')
})
