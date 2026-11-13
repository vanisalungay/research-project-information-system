/**
 * deanDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the Dean dashboard's structure (src/views/Dashboard/DeanDashboard.vue) after its
 * restyle to match the other role dashboards.
 *
 * The page is a Vue component, so nothing type-checks the things a redesign can break: a
 * statistic turned into a hardcoded number, a table column renamed, a row action that
 * loses its route, or the full-width modifier the Dean pages depend on (see
 * deanPages.test.js). The file is read as text here - the technique the other dashboard
 * tests use - and each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/DeanDashboard.vue', import.meta.url),
)
const ROUTER_FILE = fileURLToPath(new URL('../router/index.js', import.meta.url))

const read = (file) => readFileSync(file, 'utf8')
const DASHBOARD = read(DASHBOARD_FILE)
const ROUTER = read(ROUTER_FILE)

test('the dashboard keeps the Dean full-width contract', () => {
  // deanPages.test.js requires this exact class on the Dean pages; the inner .dash-page
  // grid is what spaces the banner, the cards and the sections.
  assert.match(
    DASHBOARD,
    /class="end-page end-page--fluid end-page--dean"/,
    'the Dean dashboard root must keep the full-width modifier',
  )
  assert.match(DASHBOARD, /class="dash-page"/, 'the dashboard needs its grid wrapper')
})

test('the dashboard reuses the shared design layer', () => {
  assert.match(DASHBOARD, /import '@\/assets\/dashboard\.css'/, 'the shared dashboard stylesheet must be imported')
  assert.match(DASHBOARD, /import '@\/assets\/endorsement\.css'/, 'the shared endorsement stylesheet must be imported')
  assert.ok(DASHBOARD.includes('dash-banner'), 'the welcome banner must be used')
  assert.ok(DASHBOARD.includes('dash-stats'), 'the statistic cards must be used')
})

test('the three statistics are bound to real data, never typed in', () => {
  const values = [...DASHBOARD.matchAll(/dash-stat-value">\s*([^\n<]+)/g)].map((m) => m[1].trim())
  assert.equal(values.length, 3, 'the dashboard must render its three statistics')
  for (const value of values) {
    assert.ok(value.startsWith('{{'), `a statistic is hardcoded instead of counted: "${value}"`)
  }

  for (const [label, binding] of [
    ['Pending requests', '{{ pending.length }}'],
    ['Approved', '{{ approved.length }}'],
    ['Rejected', '{{ rejected.length }}'],
  ]) {
    assert.ok(DASHBOARD.includes(label), `the "${label}" card label must be kept`)
    assert.ok(DASHBOARD.includes(binding), `the "${label}" card must use ${binding}`)
  }
})

test('the welcome text and the data source are unchanged', () => {
  assert.match(DASHBOARD, /Welcome, \{\{ displayName \}\}!/, 'the greeting must be kept')
  assert.ok(
    DASHBOARD.includes('Review and decide the endorsement requests of your college.'),
    'the welcome description must be kept',
  )
  assert.match(DASHBOARD, /fetchEndorsementRequests\(\)/, 'the endorsement endpoint must be kept')
  assert.match(DASHBOARD, /ENDORSEMENT_STATUS\.(PENDING|APPROVED|REJECTED)/, 'the status filters must be kept')
})

test('the tables keep their columns and cell bindings', () => {
  for (const header of ['Reference', 'Proponent', 'Project title', 'College', 'Submitted', 'Decided', 'Status']) {
    assert.ok(DASHBOARD.includes(`<th>${header}</th>`) || DASHBOARD.includes(`>${header}</th>`), `the "${header}" column must be kept`)
  }
  for (const binding of [
    '{{ request.referenceNumber }}',
    '{{ request.proponentName }}',
    '{{ request.projectTitle }}',
    '{{ request.programTitle }}',
    '{{ request.college }}',
    '{{ formatEndorsementTimestamp(request.createdAt) }}',
    '{{ formatEndorsementTimestamp(request.decidedAt) }}',
    '{{ endorsementStatusLabel(request.status) }}',
  ]) {
    assert.ok(DASHBOARD.includes(binding), `the cell ${binding} must be kept`)
  }
  assert.match(DASHBOARD, /endorsementStatusTone\(request\.status\)/, 'the status pill tone must be kept')
  assert.ok(DASHBOARD.includes('end-table-scroll'), 'the tables must scroll inside their card')
})

test('the empty states and the row actions are unchanged', () => {
  assert.ok(DASHBOARD.includes('Nothing is waiting for your endorsement right now.'), 'the pending empty state must be kept')
  assert.ok(DASHBOARD.includes('No decisions recorded yet.'), 'the decided empty state must be kept')

  // "See all requests" and the row Review/View actions point at the Dean request routes.
  assert.match(DASHBOARD, /to="\/dean-requests"/, 'the See all requests link must be kept')
  assert.match(DASHBOARD, /to="`\/dean-requests\/\$\{request\.id\}`"/, 'the row actions must open the request')
  assert.ok(ROUTER.includes("path: 'dean-requests'"), 'the /dean-requests route must exist')
  assert.ok(ROUTER.includes("path: 'dean-requests/:id'"), 'the /dean-requests/:id route must exist')
})
