/**
 * proponentDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the Proponent dashboard's structure (src/views/Dashboard/ProponentDashboard.vue).
 *
 * The page is a Vue component, so nothing type-checks the things that made it unusable
 * before: a width cap that left half the screen empty, links to routes that do not exist,
 * or a statistic typed in by hand instead of counted from the records. The file is read as
 * text here - the same technique navigation.test.js uses for the navigation and the router
 * - and each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/ProponentDashboard.vue', import.meta.url),
)
const ROUTER_FILE = fileURLToPath(new URL('../router/index.js', import.meta.url))
const STYLES_FILE = fileURLToPath(new URL('../assets/dashboard.css', import.meta.url))

const read = (file) => readFileSync(file, 'utf8')
const DASHBOARD = read(DASHBOARD_FILE)
const ROUTER = read(ROUTER_FILE)
const STYLES = read(STYLES_FILE)

/** Every route path declared by the router, normalized to a leading slash. */
function routePaths() {
  return new Set(
    [...ROUTER.matchAll(/path:\s*'([^']+)'/g)].map((match) =>
      match[1].startsWith('/') ? match[1] : `/${match[1]}`,
    ),
  )
}

/** Every destination the dashboard links to, with the dynamic part removed. */
function dashboardLinks() {
  return [...DASHBOARD.matchAll(/:?to="`?([^"`]+)`?"/g)].map(
    (match) => match[1].split('${')[0].split('?')[0].replace(/\/+$/, '') || '/',
  )
}

test('the dashboard fills the width the shell gives it', () => {
  // The page used to be capped by .end-page (max-width: 1040px), which left a large empty
  // area to the right on any wide screen. The fluid modifier is what prevents that.
  assert.match(
    DASHBOARD,
    /class="end-page end-page--fluid dash-page"/,
    'the dashboard root must keep the full-width modifier',
  )
  assert.match(
    STYLES,
    /\.dash-page\s*\{[^}]*width:\s*100%/,
    'the dashboard page grid must fill its container',
  )
  // No page-level width cap may be reintroduced in the dashboard stylesheet.
  assert.ok(
    !/\.dash-page\s*\{[^}]*max-width/.test(STYLES),
    'the dashboard page must not cap its own width',
  )
})

test('every action on the dashboard points at a real route', () => {
  const routes = routePaths()
  const links = dashboardLinks()
  assert.ok(links.length >= 6, 'the dashboard must link to its workflow pages')

  for (const link of links) {
    const exists = [...routes].some((route) => {
      const base = route.replace(/\/:.*$/, '')
      return link === base || link.startsWith(`${base}/`)
    })
    assert.ok(exists, `the dashboard links to ${link}, which is not a route in router/index.js`)
  }

  // The four quick actions of the agreed navigation are all present.
  for (const required of [
    '/proposals',
    '/proponent-proposal-monitoring',
    '/endorsements',
    '/notifications',
  ]) {
    assert.ok(links.includes(required), `the quick actions must include ${required}`)
  }
})

test('every statistic is bound to real data, never typed into the template', () => {
  const values = [...DASHBOARD.matchAll(/dash-stat-value"[^>]*>\s*([^\n<]+)/g)].map((m) => m[1].trim())
  assert.ok(values.length > 0, 'the dashboard must render statistic values')

  for (const value of values) {
    assert.ok(value.startsWith('{{'), `a statistic is hardcoded instead of counted: "${value}"`)
  }

  // The cards themselves come from the shared bucket definition.
  assert.match(DASHBOARD, /PROPOSAL_SUMMARY_BUCKETS/, 'the proposal cards must use the shared buckets')
  assert.match(DASHBOARD, /proposalSummaryCounts\(/, 'the proposal counts must be computed from the records')
  assert.match(DASHBOARD, /endorsementSummaryCounts\(/, 'the endorsement counts must be computed from the records')
})

test('the dashboard reads the Proponent-scoped endpoints and reuses the shared store', () => {
  // Three list requests plus the notification list the shell already reads.
  assert.match(DASHBOARD, /api\.get\('\/api\/proposals'\)/, 'proposals come from the scoped endpoint')
  assert.match(DASHBOARD, /api\.get\(AVAILABLE_CALLS_ENDPOINT\)/, 'the calls come from the scoped endpoint')
  assert.match(DASHBOARD, /fetchEndorsementRequests\(\)/, 'the endorsements come from the scoped endpoint')
  assert.match(DASHBOARD, /useNotificationStore\(\)/, 'notifications come from the shared store')

  // No second notification request and no extra Certifications request: the issued-letter
  // count is derived from the endorsement records, which already carry it.
  assert.ok(
    !DASHBOARD.includes("'/api/notifications'"),
    'the dashboard must not fetch the notifications the shell already loaded',
  )
  assert.ok(
    !DASHBOARD.includes('fetchCertifications'),
    'the dashboard must not request the certifications list again',
  )
})

test('a loading, an error and an empty state are handled for every section', () => {
  for (const state of [
    'callsLoading',
    'callsError',
    'proposalsLoading',
    'proposalsError',
    'endorsementsLoading',
    'endorsementsError',
  ]) {
    assert.ok(DASHBOARD.includes(state), `the dashboard must handle ${state}`)
  }

  // The empty states are the real ones, not placeholder text.
  assert.match(DASHBOARD, /NO_AVAILABLE_CALLS_MESSAGE/, 'the call section needs its empty state')
  assert.match(DASHBOARD, /dash-empty/, 'the sections need their empty states')
})

test('the layout is driven by responsive rules, not by fixed widths', () => {
  assert.match(STYLES, /@media \(min-width: 1200px\)/, 'the paired rows need their breakpoint')
  assert.match(STYLES, /@media \(max-width: 560px\)/, 'the mobile rules need their breakpoint')

  // The page and the sections may not be given a fixed pixel width.
  assert.ok(
    !/\.dash-(page|section)\s*\{[^}]*width:\s*\d+px/.test(STYLES),
    'the page and the sections must be sized by the grid, not by fixed pixels',
  )
})

