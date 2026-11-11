/**
 * ovcrigeDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the OVCRIGE dashboard's structure (src/views/Dashboard/OvcrigeDashboard.vue).
 *
 * The page was restyled to match the Proponent, RPS and REC dashboards. A redesign is
 * exactly the moment a statistic can become a hardcoded number, a status can be reworded,
 * a button can lose its handler or a route can be mistyped, and nothing type-checks any of
 * it. The file is read as text here - the same technique proponentDashboard.test.js and
 * navigation.test.js use - and each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/OvcrigeDashboard.vue', import.meta.url),
)
const ROUTER_FILE = fileURLToPath(new URL('../router/index.js', import.meta.url))

const read = (file) => readFileSync(file, 'utf8')
const DASHBOARD = read(DASHBOARD_FILE)
const ROUTER = read(ROUTER_FILE)

/** Every route path declared by the router, normalized to a leading slash. */
function routePaths() {
  return new Set(
    [...ROUTER.matchAll(/path:\s*'([^']+)'/g)].map((match) =>
      match[1].startsWith('/') ? match[1] : `/${match[1]}`,
    ),
  )
}

test('the dashboard fills the width the shell gives it', () => {
  // The page used to be wrapped in its own .dashboard-layout (height: 100vh) inside the
  // shell's content area, which padded and scrolled the page twice. The fluid modifier is
  // what makes it behave like the Proponent, RPS and REC dashboards.
  assert.match(
    DASHBOARD,
    /class="end-page end-page--fluid dash-page"/,
    'the dashboard root must keep the full-width modifier',
  )
})

test('the dashboard reuses the shared design layer', () => {
  assert.match(
    DASHBOARD,
    /import '@\/assets\/dashboard\.css'/,
    'the dashboard must import the shared dashboard stylesheet',
  )
  assert.match(
    DASHBOARD,
    /import '@\/assets\/endorsement\.css'/,
    'the dashboard must import the shared endorsement stylesheet',
  )

  // The bespoke, page-only layout wrapper may not come back.
  assert.ok(
    !DASHBOARD.includes('dashboard-layout'),
    'the dashboard must not reintroduce its own layout wrapper',
  )
})

test('every statistic is bound to real data, never typed into the template', () => {
  const values = [...DASHBOARD.matchAll(/dash-stat-value">\s*([^\n<]+)/g)].map((m) => m[1].trim())
  assert.equal(values.length, 4, 'the dashboard must render its four statistic values')

  for (const value of values) {
    assert.ok(value.startsWith('{{'), `a statistic is hardcoded instead of counted: "${value}"`)
  }

  // The four counts are still the ones the page computed before the restyle.
  for (const [label, binding] of [
    ['Endorsed from RPS', 'stats.endorsed'],
    ['Under REC Review', 'stats.underReview'],
    ['REC Approved', 'stats.recApproved'],
    ['For Revision', 'stats.revision'],
  ]) {
    assert.ok(DASHBOARD.includes(label), `the "${label}" card label must be kept`)
    assert.ok(DASHBOARD.includes(`{{ ${binding} }}`), `the "${label}" card must use ${binding}`)
  }
})

test('the dashboard reads the same endpoints it always did', () => {
  assert.match(DASHBOARD, /api\.get\('\/api\/proposals'/, 'proposals come from the same endpoint')
  assert.match(
    DASHBOARD,
    /api\.get\(\s*'\/api\/notifications\?userId=4'\s*\)/,
    'notifications come from the same endpoint',
  )

  // The statistics are still filtered from the records the endpoint returns.
  for (const filter of [
    "p.status === 'ENDORSED'",
    "p.status === 'UNDER_REVIEW'",
    "p.status === 'REC_APPROVED'",
    "p.status === 'REVISION'",
  ]) {
    assert.ok(DASHBOARD.includes(filter), `the statistic filter ${filter} must be kept`)
  }
})

test('the dashboard keeps every route it links to', () => {
  const paths = routePaths()

  // Path targets, with the dynamic part removed for the comparison.
  for (const [target, route] of [
    ["router.push('/notifications')", '/notifications'],
    ["router.push('/submit-proposals')", '/submit-proposals'],
    ['router.push(`/review-prop/${id}`)', '/review-prop/:id'],
  ]) {
    assert.ok(DASHBOARD.includes(target), `the dashboard must still use ${target}`)
    assert.ok(paths.has(route), `${route} is not a route in router/index.js`)
  }
})

test('the proposal table keeps its columns, cells and status text', () => {
  for (const header of ['Proposal ID', 'Title', 'Proponent', 'Submitted', 'Status', 'Actions']) {
    assert.ok(
      DASHBOARD.includes(`<th>${header}</th>`) || DASHBOARD.includes(`>${header}</th>`),
      `the "${header}" column must be kept`,
    )
  }

  // The cells are still the same bindings the page already used.
  assert.match(
    DASHBOARD,
    /\{\{ proposal\.documentId \|\| proposal\.proposalCode \|\| '—' \}\}/,
    'the proposal id cell must be kept',
  )
  assert.match(DASHBOARD, /\{\{ proposal\.projectTitle \}\}/, 'the title cell must be kept')
  assert.match(
    DASHBOARD,
    /\{\{ proposal\.projectLeader \|\| proposal\.proponent\?\.name \|\| '-' \}\}/,
    'the proponent cell must be kept',
  )
  assert.match(
    DASHBOARD,
    /\{\{ proposal\.createdAt\?\.substring\(0, 10\) \}\}/,
    'the submission date cell must be kept',
  )

  // The status shown goes through the shared proposal label, so a newly submitted
  // proposal reads "For Review" exactly as it does on the ORPS and Proponent pages.
  assert.match(
    DASHBOARD,
    /class="end-badge"[^>]*>\s*\{\{ proposalStatusLabel\(proposal\.status\) \}\}/,
    'the status pill must use the shared proposal status label',
  )

  // The action keeps its handler and the list keeps its limit.
  assert.match(DASHBOARD, /@click="goToReview\(proposal\.id\)"/, 'the Review action must be kept')
  assert.match(
    DASHBOARD,
    /v-for="proposal in proposals\.slice\(0, 5\)"/,
    'the proposals must keep their order and their limit',
  )

  // The loading and empty states are the real ones, not placeholder text.
  assert.ok(DASHBOARD.includes('Loading...'), 'the table needs its loading state')
  assert.ok(DASHBOARD.includes('No proposals found.'), 'the table needs its empty state')
})

test('the notifications keep their message, date and order', () => {
  assert.match(
    DASHBOARD,
    /v-for="notification in notifications\.slice\(0, 5\)"/,
    'the notifications must keep their order and their limit',
  )
  assert.match(
    DASHBOARD,
    /\{\{ notification\.message \}\}/,
    'the notification message must be shown unchanged',
  )
  assert.match(
    DASHBOARD,
    /\{\{ notification\.createdAt\?\.substring\(0, 16\) \}\}/,
    'the notification timestamp must be shown unchanged',
  )
  assert.ok(DASHBOARD.includes('No notifications.'), 'the notifications need their empty state')
  assert.match(DASHBOARD, /@click="goToNotifications"/, 'the notifications action must be kept')
})

test('the status pill tones are chosen from the record status', () => {
  assert.match(DASHBOARD, /statusTone\(proposal\.status\)/, 'the pill must use the status tone')
  assert.match(
    DASHBOARD,
    /import \{ statusTone \} from '@\/utils\/statusTone\.js'/,
    'the tone must come from the shared status mapping',
  )

  // The page no longer carries its own switch: the shared mapping covers every stage.
  assert.doesNotMatch(DASHBOARD, /switch \(status\)/, 'the page must not keep its own status switch')
  assert.doesNotMatch(DASHBOARD, /return 'is-info'/, 'the retired page-local tones must be gone')
})

test('the banner keeps its title and office subtitle', () => {
  assert.match(DASHBOARD, /class="dash-banner-title">OVCRIGE Dashboard</, 'the banner title must be kept')
  assert.match(
    DASHBOARD,
    /Office of the Vice Chancellor for Research, Innovation and Global Engagement/,
    'the banner subtitle must be kept',
  )
})
