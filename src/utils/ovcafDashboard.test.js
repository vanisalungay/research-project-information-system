/**
 * ovcafDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the OVCAF dashboard's structure (src/views/Dashboard/OvcafDashboard.vue) after its
 * restyle to match the other role dashboards.
 *
 * The page is a Vue component, so nothing type-checks the things a redesign can break: a
 * statistic turned into a hardcoded number, a status filter dropped from the fetch, a table
 * column renamed, or a row action that loses its route. The file is read as text here - the
 * technique the other dashboard tests use - and each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/OvcafDashboard.vue', import.meta.url),
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
  assert.ok(!DASHBOARD.includes('dashboard-wrapper'), 'the dashboard must not reintroduce its own wrapper')
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
    ['PROPOSALS FOR REVIEW', '{{ stats.forReview }}'],
    ['ENDORSED TO OC', '{{ stats.endorsedToOc }}'],
    ['APPROVED / RELEASED', '{{ stats.completed }}'],
  ]) {
    assert.ok(DASHBOARD.includes(label), `the "${label}" card label must be kept`)
    assert.ok(DASHBOARD.includes(binding), `the "${label}" card must use ${binding}`)
  }
})

test('the header wording and the endpoint are unchanged', () => {
  assert.match(DASHBOARD, /class="dash-banner-title">OVCAF DASHBOARD</, 'the header title must be kept')
  assert.ok(
    DASHBOARD.includes('Office of the Vice Chancellor for Admin and Finance'),
    'the header subtitle must be kept',
  )
  assert.match(DASHBOARD, /api\.get\('\/api\/proposals'\)/, 'proposals come from the same endpoint')
})

test('the status filters behind every count are unchanged', () => {
  for (const status of [
    'FOR_OVCAF_APPROVAL',
    'REC_APPROVED',
    'FOR_OC_APPROVAL',
    'OVC_APPROVED',
    'APPROVED',
    'READY_FOR_RELEASE',
    'RELEASED',
  ]) {
    assert.ok(DASHBOARD.includes(`'${status}'`), `the ${status} filter must be kept`)
  }
})

test('the tables keep their columns, cells and actions', () => {
  // The name column is labelled Project Leader: the value is the person named on the
  // proposal form, not the generic account that owns the record.
  for (const header of ['Proposal Title', 'Project Leader', 'Status', 'Action']) {
    assert.ok(DASHBOARD.includes(`>${header}</th>`), `the "${header}" column must be kept`)
  }
  assert.ok(DASHBOARD.includes('p.projectLeader'), 'the column is filled from the form value')
  for (const binding of ['{{ proposal.title }}', '{{ proposal.proponent }}', '{{ proposal.status }}']) {
    assert.ok(DASHBOARD.includes(binding), `the cell ${binding} must be kept`)
  }
  assert.match(DASHBOARD, /statusTone\(proposal\.status\)/, 'the status pill tone must be used')
  assert.match(
    DASHBOARD,
    /@click="\$router\.push\(`\/ovcaf\/validate\/\$\{proposal\.id\}`\)"/,
    'the Review/View action must open the validation page',
  )
  assert.ok(ROUTER.includes("path: 'ovcaf/validate/:id?'"), 'the /ovcaf/validate/:id route must exist')
})

test('the empty states are the real ones, not placeholder text', () => {
  assert.ok(DASHBOARD.includes('No proposals awaiting OVCAF review.'), 'the review empty state must be kept')
  assert.ok(DASHBOARD.includes('No approved proposals requesting funding.'), 'the funds empty state must be kept')
})
