/**
 * projectApprovalDisplay.test.js
 * ---------------------------------------------------------------------------
 * Guards the canonical approval naming on the proposal listing pages:
 *
 *   - a proposal stored as APPROVED is shown as "Implementation Approved",
 *     never as "Completed";
 *   - a proposal stored as COMPLETED is shown as "Completed" and has its own
 *     filter option, so the two states are never merged;
 *   - filters keep the raw stored value while their text comes from the shared
 *     proposal label;
 *   - every listing page still renders its row action through the shared helper.
 *
 * The pages are read as text - the technique ovcafProposalDisplay.test.js and the
 * dashboard guard tests already use - because they import Vite `?inline` assets Node
 * cannot resolve. The wording rule itself is unit-tested in proposalStatus.test.js;
 * this file checks that every listing page actually goes through the shared helpers.
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

/**
 * The status-filter select only. RpsProposalMonitoring also offers "Completed" as a
 * Current Office value, so the status-label guards must not read the office select.
 */
function statusFilterSource(source) {
  const marker = source.indexOf('id="status-filter"')
  if (marker === -1) return source
  const open = source.lastIndexOf('<select', marker)
  const close = source.indexOf('</select>', marker)
  return open === -1 || close === -1 ? source : source.slice(open, close)
}

test('the status filters take their text from the canonical label', () => {
  for (const [name, source] of [
    ['ProponentProposals.vue', PROPONENT_PROPOSALS],
    ['ProponentProposalMonitoring.vue', PROPONENT_MONITORING],
    ['RpsProposalMonitoring.vue', RPS_MONITORING],
    ['OvcafProposalInbox.vue', OVCAF_INBOX],
  ]) {
    const statusFilter = statusFilterSource(source)
    assert.ok(
      statusFilter.includes(`<option value="APPROVED">{{ proposalStatusLabel('APPROVED') }}</option>`),
      name + ' must render the APPROVED filter through proposalStatusLabel',
    )
    assert.ok(
      !statusFilter.includes('<option value="APPROVED">Completed</option>'),
      name + ' must not label the APPROVED filter Completed',
    )
    assert.ok(!/>\s*Approved\s*<\/option>/.test(statusFilter), name + ' must not hardcode an Approved option')
    assert.ok(
      !/>\s*Completed\s*<\/option>/.test(statusFilter),
      name + ' must not hardcode a Completed option',
    )
  }
})

test('COMPLETED has its own option wherever it is offered', () => {
  for (const [name, source] of [
    ['ProponentProposals.vue', PROPONENT_PROPOSALS],
    ['ProponentProposalMonitoring.vue', PROPONENT_MONITORING],
    ['RpsProposalMonitoring.vue', RPS_MONITORING],
  ]) {
    assert.ok(
      source.includes(`<option value="COMPLETED">{{ proposalStatusLabel('COMPLETED') }}</option>`),
      name + ' must offer the stored COMPLETED status separately',
    )
  }

  // The OVCAF inbox does not show completed proposals, so it offers no COMPLETED option.
  assert.ok(!OVCAF_INBOX.includes('value="COMPLETED"'), 'the OVCAF inbox must not add a COMPLETED filter')
})

test('the monitoring timeline labels its final stage through the helper', () => {
  assert.ok(
    PROPONENT_MONITORING.includes(
      "const APPROVED_STAGE = { key: 'APPROVED', label: proposalStatusLabel('APPROVED') }",
    ),
    'the final timeline stage must use the canonical APPROVED label',
  )
  assert.ok(
    PROPONENT_MONITORING.includes(
      "const OVCAF_STAGE = { key: 'FOR_OVCAF_APPROVAL', label: proposalStatusLabel('FOR_OVCAF_APPROVAL') }",
    ),
    'the OVCAF timeline stage must use the canonical label',
  )
  assert.ok(
    PROPONENT_MONITORING.includes("POST_APPROVAL_STATUSES = ['RELEASED', 'COMPLETED']"),
    'post-approval progress keeps RELEASED and COMPLETED distinct from APPROVED',
  )
})

test('the budget filters show canonical labels and select the raw stored status', () => {
  for (const [name, source] of [
    ['RpsBudgetProcessing.vue', RPS_BUDGET],
    ['RpsStaffDashboard.vue', RPS_STAFF],
  ]) {
    assert.ok(
      source.includes("[proposalStatusLabel('APPROVED')]: 'APPROVED'"),
      name + ' must key the APPROVED chip by its canonical label and keep the raw value',
    )
    assert.ok(
      source.includes("[proposalStatusLabel('RETURNED')]: 'RETURNED'"),
      name + ' must key the RETURNED chip by its canonical label and keep the raw value',
    )
    assert.ok(!source.includes("'Completed': 'APPROVED'"), name + ' must not alias Completed to APPROVED')
    assert.ok(!source.includes('"Completed": "APPROVED"'), name + ' must not alias Completed to APPROVED')
  }
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
