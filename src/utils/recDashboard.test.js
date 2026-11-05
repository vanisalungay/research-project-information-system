/**
 * recDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the REC dashboard's structure (src/views/Dashboard/RecDashboard.vue).
 *
 * The page was restyled to match the Proponent and RPS dashboards. A redesign is exactly
 * the moment a statistic can become a hardcoded number, a status can be reworded, a button
 * can lose its handler or a route can be mistyped, and nothing type-checks any of it. The
 * file is read as text here - the same technique proponentDashboard.test.js and
 * navigation.test.js use - and each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const DASHBOARD_FILE = fileURLToPath(
  new URL('../views/Dashboard/RecDashboard.vue', import.meta.url),
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

/** Every named route declared by the router. */
function routeNames() {
  return new Set([...ROUTER.matchAll(/name:\s*'([^']+)'/g)].map((match) => match[1]))
}

test('the dashboard fills the width the shell gives it', () => {
  // The page used to be wrapped in its own .dashboard-layout (height: 100vh) inside the
  // shell's content area, which padded and scrolled the page twice. The fluid modifier is
  // what makes it behave like the Proponent and RPS dashboards.
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
  assert.equal(values.length, 3, 'the dashboard must render its three statistic values')

  for (const value of values) {
    assert.ok(value.startsWith('{{'), `a statistic is hardcoded instead of counted: "${value}"`)
  }

  // The three counts are still the ones the page computed before the restyle.
  for (const [label, binding] of [
    ['Submitted Proposals', 'totalAssigned'],
    ['Completed Reviews', 'completedReviews'],
    ['Pending Evaluation', 'pendingEvaluation'],
  ]) {
    assert.ok(DASHBOARD.includes(label), `the "${label}" card label must be kept`)
    assert.ok(DASHBOARD.includes(`{{ ${binding} }}`), `the "${label}" card must use ${binding}`)
  }
})

test('the dashboard reads the same endpoints it always did', () => {
  assert.match(DASHBOARD, /api\.get\('\/api\/proposals'\)/, 'proposals come from the same endpoint')
  assert.match(
    DASHBOARD,
    /api\.get\(`\/api\/notifications\?userId=\$\{user\.id\}`\)/,
    'notifications come from the same endpoint',
  )
})

test('the dashboard keeps every route it links to', () => {
  const paths = routePaths()
  const names = routeNames()

  // Path targets.
  for (const path of ['/rec-notif', '/assigned-proposals']) {
    assert.ok(DASHBOARD.includes(`router.push('${path}')`), `the dashboard must still push ${path}`)
    assert.ok(paths.has(path), `${path} is not a route in router/index.js`)
  }

  // Named targets.
  for (const name of ['RecReviewForm', 'RecMeetingDetails']) {
    assert.ok(DASHBOARD.includes(`name: '${name}'`), `the dashboard must still open ${name}`)
    assert.ok(names.has(name), `${name} is not a named route in router/index.js`)
  }
})

test('the proposal table keeps its columns, cells and raw status text', () => {
  for (const header of ['Title', 'Proponent', 'Date Submitted', 'Status', 'Actions']) {
    assert.ok(
      DASHBOARD.includes(`<th>${header}</th>`) || DASHBOARD.includes(`>${header}</th>`),
      `the "${header}" column must be kept`,
    )
  }

  // The cells are still the same bindings the page already used.
  assert.match(DASHBOARD, /\{\{ proposal\.projectTitle \}\}/, 'the title cell must be kept')
  assert.match(DASHBOARD, /\{\{ proposal\.proponent\?\.name \}\}/, 'the proponent cell must be kept')
  assert.match(
    DASHBOARD,
    /\{\{ proposal\.createdAt\?\.substring\(0, 10\) \}\}/,
    'the submission date cell must be kept',
  )

  // The status shown is the raw record value, not a rewritten label.
  assert.match(
    DASHBOARD,
    /class="end-badge"[^>]*>\s*\{\{ proposal\.status \}\}/,
    'the status pill must show the record value unchanged',
  )

  // The Review/View actions keep their condition and their handlers.
  assert.match(
    DASHBOARD,
    /v-if="proposal\.status === 'UNDER_REVIEW'"/,
    'the Review condition must be kept',
  )
  assert.match(DASHBOARD, /@click="goToReview\(proposal\.id\)"/, 'the Review action must be kept')
  assert.match(DASHBOARD, /@click="goToMeeting\(proposal\.id\)"/, 'the View action must be kept')

  // The empty state is the real one, not placeholder text.
  assert.ok(DASHBOARD.includes('No submitted proposals.'), 'the table needs its empty state')
})

test('the notifications keep their message, date and order', () => {
  assert.match(
    DASHBOARD,
    /v-for="notification in notifications\.slice\(0,5\)"/,
    'the notifications must keep their order and their limit',
  )
  assert.match(
    DASHBOARD,
    /\{\{ notification\.message \}\}/,
    'the notification message must be shown unchanged',
  )
  assert.match(
    DASHBOARD,
    /\{\{ formatDate\(notification\.createdAt\) \}\}/,
    'the notification timestamp must be shown unchanged',
  )
  assert.ok(DASHBOARD.includes('No notifications.'), 'the notifications need their empty state')
  assert.match(DASHBOARD, /@click="goToNotifications"/, 'the notifications action must be kept')
})

test('the status pill tones are chosen from the record status', () => {
  assert.match(DASHBOARD, /statusTone\(proposal\.status\)/, 'the pill must use the status tone')
  assert.match(
    DASHBOARD,
    /if \(status === 'UNDER_REVIEW'\) return 'is-pending'/,
    'under review is amber',
  )
  assert.match(DASHBOARD, /if \(status === 'REC_REJECTED'\) return 'is-rejected'/, 'a rejection is red')
  assert.match(DASHBOARD, /return 'is-approved'/, 'the remaining statuses are green')
})
