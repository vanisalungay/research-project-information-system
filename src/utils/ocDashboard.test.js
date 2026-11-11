/**
 * ocDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the Chancellor dashboard's structure (src/views/Dashboard/OCDashboard.vue) after
 * its restyle to match the other role dashboards.
 *
 * The page is a Vue component, so nothing type-checks the things a redesign can break: a
 * statistic turned into a hardcoded number, a status filter dropped from either fetch, a
 * table column renamed, or the Review & Approve action losing its route. The file is read
 * as text here - the technique the other dashboard tests use - and each check states the
 * mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/OCDashboard.vue', import.meta.url),
)
const ROUTER_FILE = fileURLToPath(new URL('../router/index.js', import.meta.url))

const read = (file) => readFileSync(file, 'utf8')
const DASHBOARD = read(DASHBOARD_FILE)
const ROUTER = read(ROUTER_FILE)

test('the dashboard fills the width the shell gives it', () => {
  assert.match(
    DASHBOARD,
    /class="end-page end-page--fluid dash-page"/,
    'the dashboard root must keep the full-width modifier',
  )
  // The bespoke, page-only wrapper may not come back.
  assert.ok(!DASHBOARD.includes('dashboard-content'), 'the dashboard must not reintroduce its own wrapper')
})

test('the dashboard reuses the shared design layer', () => {
  assert.match(DASHBOARD, /import '@\/assets\/dashboard\.css'/, 'the shared dashboard stylesheet must be imported')
  assert.match(DASHBOARD, /import '@\/assets\/endorsement\.css'/, 'the shared endorsement stylesheet must be imported')
  assert.ok(DASHBOARD.includes('dash-banner'), 'the header banner must be used')
  assert.ok(DASHBOARD.includes('dash-stats'), 'the statistic cards must be used')
  assert.ok(DASHBOARD.includes('end-table'), 'the shared table must be used')
})

test('the three statistics are bound to real data, never typed in', () => {
  const values = [...DASHBOARD.matchAll(/dash-stat-value">\s*([^\n<]+)/g)].map((m) => m[1].trim())
  assert.equal(values.length, 3, 'the dashboard must render its three statistics')
  for (const value of values) {
    assert.ok(value.startsWith('{{'), `a statistic is hardcoded instead of counted: "${value}"`)
  }

  for (const [label, binding] of [
    ['Pending Final Approval', '{{ stats.pending }}'],
    ['Approved / Released', '{{ stats.approved }}'],
    ['Sent to OVCAF (Budget)', '{{ stats.sentToOvcaf }}'],
  ]) {
    assert.ok(DASHBOARD.includes(label), `the "${label}" card label must be kept`)
    assert.ok(DASHBOARD.includes(binding), `the "${label}" card must use ${binding}`)
  }
})

test('the title, subtitle and both endpoints are unchanged', () => {
  assert.match(DASHBOARD, /class="dash-banner-title">CHANCELLOR DASHBOARD</, 'the title must be kept')
  assert.ok(
    DASHBOARD.includes('Office of the Chancellor — Final Approval, Budget Determination &amp; Special Orders (SO)'),
    'the subtitle must be kept',
  )

  assert.ok(
    DASHBOARD.includes("api.get('/api/proposals?statusIn=FOR_OC_APPROVAL')"),
    'the pending fetch must contain only the status the Chancellor can act on',
  )
  assert.ok(
    DASHBOARD.includes(
      "api.get('/api/proposals?statusIn=FOR_OVCAF_APPROVAL&statusIn=APPROVED&statusIn=READY_FOR_RELEASE&statusIn=RELEASED')",
    ),
    'the approved fetch must keep its status filters',
  )
})

test('the tables keep their columns, cells and actions', () => {
  for (const header of ['Proposal Title', 'Proponent', 'Date Endorsed', 'Date', 'Status', 'Action']) {
    assert.ok(DASHBOARD.includes(`>${header}</th>`), `the "${header}" column must be kept`)
  }
  for (const binding of ['{{ p.title }}', '{{ p.adviser }}', '{{ p.date }}', '{{ proposalStatusLabel(p.status) }}']) {
    assert.ok(DASHBOARD.includes(binding), `the cell ${binding} must be kept`)
  }
  assert.match(DASHBOARD, /statusTone\(p\.status\)/, 'the status pill tone must be used')
  assert.match(DASHBOARD, /@click="reviewProposal\(p\.id\)"/, 'the Review & Approve / View action must be kept')
  assert.match(DASHBOARD, /router\.push\(`\/oc\/final-approval\/\$\{id\}`\)/, 'the action must open the final-approval page')
  assert.ok(ROUTER.includes("path: 'oc/final-approval/:id'"), 'the /oc/final-approval/:id route must exist')
})

test('the empty states are the real ones, not placeholder text', () => {
  assert.ok(
    DASHBOARD.includes('No proposals currently awaiting final executive sign-off.'),
    'the final-approval empty state must be kept',
  )
  assert.ok(
    DASHBOARD.includes('Approved proposals and those forwarded to Finance/OVCAF for budget processing will appear here.'),
    'the approved empty state must be kept',
  )
})

test('downloads live inside Proposal Details, not on the dashboard', () => {
  // The request is explicit: the Chancellor downloads from the proposal's own page,
  // after opening it, so no row may offer a Download action.
  assert.ok(!DASHBOARD.includes('is-download'), 'the dashboard must not render a Download button')
  assert.ok(!DASHBOARD.includes('downloadProposal'), 'the dashboard must not carry a download handler')
  assert.ok(
    !DASHBOARD.includes('proposalDetailsDocument'),
    'the dashboard must not import the document generator',
  )
  assert.match(DASHBOARD, /@click="reviewProposal\(p\.id\)"/, 'the row action opens the details page instead')
})

test('a saved SO draft is visible in the pending table', () => {
  assert.match(DASHBOARD, /import \{ isSoDraft \} from '@\/utils\/soDraft\.js'/,
    'the shared draft rule must be used')
  assert.ok(DASHBOARD.includes('v-if="isSoDraft(p)"'), 'the pending row must show the draft state')
  assert.ok(DASHBOARD.includes('end-badge is-draft'), 'the shared draft badge tone must be used')
  assert.ok(DASHBOARD.includes('>SO Draft<'), 'the badge must name the draft')
  assert.ok(DASHBOARD.includes('soFilePath: p.soFilePath'), 'the row must carry the fields the rule reads')
  assert.ok(DASHBOARD.includes('soUploadedAt: p.soUploadedAt'), 'the row must carry the SO date')
})
