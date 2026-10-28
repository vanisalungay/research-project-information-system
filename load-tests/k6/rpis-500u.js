/**
 * RPIS 500-concurrent-user load test (k6).
 * ---------------------------------------------------------------------------
 * Simulates 500 simultaneous signed-in users against the RPIS backend and
 * measures the five flows the requirement names: login, dashboard loading,
 * proposal listing, notifications and proposal submission.
 *
 * Design notes
 *  - The 500 VUs are split into two scenarios that run at the same time:
 *      browse  (475 VUs) - login once, then the read-only screens a user
 *                         actually opens (dashboard, proposal list, proposal
 *                         detail, notifications, call for proposals);
 *      submit   (25 VUs) - login once, then create proposals.
 *    Peak concurrency is therefore 500, which is what is being measured.
 *  - Every request carries an explicit `name` tag, so the summary reports
 *    p50/p95/p99 per flow instead of one blended number.
 *  - Every VU is a distinct synthetic account (`loadtest.proponent.NNNN`), so
 *    the run exercises real per-user authorization and data isolation rather
 *    than 500 copies of one session.
 *  - Submissions are capped (MAX_SUBMISSIONS) so a long run cannot grow the
 *    test database without bound. The cap is reported in the summary.
 *
 * Usage (see load-tests/README.md):
 *   k6 run -e BASE_URL=http://localhost:8082 load-tests/k6/rpis-500u.js
 */
import http from 'k6/http'
import { check, sleep, group } from 'k6'
import { Counter, Trend } from 'k6/metrics'

// ---------------------------------------------------------------- configuration
const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/+$/, '')
const PASSWORD = __ENV.RPIS_LOADTEST_PASSWORD || 'LoadTest#2026'
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@msunaawan.edu.ph'
const EMAIL_PREFIX = __ENV.EMAIL_PREFIX || 'loadtest.proponent.'

const PROPONENT_COUNT = Number(__ENV.PROPONENT_COUNT || 500)
const BROWSE_VUS = Number(__ENV.BROWSE_VUS || 475)
const SUBMIT_VUS = Number(__ENV.SUBMIT_VUS || 25)
/**
 * Submissions each submit VU may make. The cap exists so a long run cannot grow the
 * test database without bound; the total is SUBMIT_VUS x this value and is reported
 * in the summary. It is a per-VU cap on purpose: k6 gives each VU its own counters,
 * so a per-VU limit is exact and needs no shared state between VUs.
 */
const MAX_SUBMISSIONS_PER_VU = Number(__ENV.MAX_SUBMISSIONS_PER_VU || 20)

const RAMP = __ENV.RAMP || '30s'
const HOLD = __ENV.HOLD || '4m'
const DOWN = __ENV.DOWN || '30s'
const THINK_TIME = Number(__ENV.THINK_TIME || 1.5)

// Thresholds are the acceptance criteria for the run. A breach fails the run.
const THRESHOLDS = {
  // At 500 users, more than 1% failed requests is a defect, not noise.
  http_req_failed: ['rate<0.01'],
  // p95 across every request.
  http_req_duration: ['p(95)<1500', 'p(99)<4000'],
  // Per-flow budgets for the endpoints the requirement names.
  'http_req_duration{name:login}': ['p(95)<1200', 'p(99)<3000'],
  'http_req_duration{name:dashboard-proposals}': ['p(95)<900', 'p(99)<2500'],
  'http_req_duration{name:proposal-list-filtered}': ['p(95)<900', 'p(99)<2500'],
  'http_req_duration{name:proposal-detail}': ['p(95)<900', 'p(99)<2500'],
  'http_req_duration{name:notifications}': ['p(95)<900', 'p(99)<2500'],
  'http_req_duration{name:call-for-proposals}': ['p(95)<900', 'p(99)<2500'],
  'http_req_duration{name:proposal-submit}': ['p(95)<2000', 'p(99)<5000'],
  // Custom checks must pass.
  checks: ['rate>0.99'],
}

// ------------------------------------------------------------------- scenarios
const stagesFor = (peak) => [
  { duration: RAMP, target: Math.max(1, Math.round(peak / 2)) },
  { duration: RAMP, target: peak },
  { duration: HOLD, target: peak },
  { duration: DOWN, target: 0 },
]

export const options = {
  discardResponseBodies: false,
  // k6 only computes the percentiles it is asked for. Without this the summary
  // reports p99 as 0, which would silently hide the tail.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    browse: {
      executor: 'ramping-vus',
      exec: 'browseFlow',
      startVUs: 0,
      stages: stagesFor(BROWSE_VUS),
      gracefulRampDown: '20s',
    },
    submit: {
      executor: 'ramping-vus',
      exec: 'submitFlow',
      startVUs: 0,
      stages: stagesFor(SUBMIT_VUS),
      gracefulRampDown: '20s',
    },
  },
  thresholds: THRESHOLDS,
}

// --------------------------------------------------------------------- metrics
const submissionsMade = new Counter('rpis_submissions_made')
const submissionsSkipped = new Counter('rpis_submissions_skipped_at_cap')
const loginsMade = new Counter('rpis_logins')
const perUserProposalCount = new Trend('rpis_proposals_returned_per_user')

// --------------------------------------------------------------------- helpers

/** Per-VU state: the token is obtained once and reused, exactly like a browser. */
let cachedToken = null
let cachedUserId = null

/** The synthetic account this VU acts as. */
function accountFor(vu) {
  const index = ((vu - 1) % PROPONENT_COUNT) + 1
  return {
    email: EMAIL_PREFIX + String(index).padStart(4, '0') + EMAIL_DOMAIN,
    role: 'PROPONENT',
  }
}

function authHeaders(token) {
  return { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }
}

/**
 * Signs this VU in (once) and returns its token. The login request itself is
 * tagged `login`, so the authentication path is measured under load like every
 * other flow.
 */
function ensureToken(vu) {
  if (cachedToken) {
    return cachedToken
  }

  const account = accountFor(vu)
  const response = http.post(
    `${BASE_URL}/api/users/login`,
    JSON.stringify({ email: account.email, password: PASSWORD, role: account.role }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  )

  const ok = check(response, {
    'login: status is 200': (r) => r.status === 200,
    'login: response carries a token': (r) => {
      try {
        return typeof r.json('token') === 'string' && r.json('token').length > 0
      } catch (e) {
        return false
      }
    },
  })

  if (!ok) {
    // Do not cache a failed login; the next iteration retries it.
    return null
  }

  cachedToken = response.json('token')
  cachedUserId = response.json('id')
  loginsMade.add(1)
  return cachedToken
}

/** Logs in if needed and backs off briefly when there is no token. */
function requireToken(vu) {
  const token = ensureToken(vu)
  if (!token) {
    sleep(1)
  }
  return token
}

// ----------------------------------------------------------------------- flows

/**
 * The read-only journey a signed-in Proponent performs: open the dashboard, page
 * through their proposals, open one proposal, read the notifications and look at
 * the calls for proposals.
 */
export function browseFlow() {
  const token = requireToken(__VU)
  if (!token) return
  const headers = authHeaders(token)

  group('dashboard', () => {
    // The dashboard loads the user's own proposals; its counters, recent list and
    // monitoring panel are all derived from this one response.
    const response = http.get(`${BASE_URL}/api/proposals`, {
      headers,
      tags: { name: 'dashboard-proposals' },
    })
    check(response, { 'dashboard: status is 200': (r) => r.status === 200 })
    try {
      const body = response.json()
      if (Array.isArray(body)) {
        perUserProposalCount.add(body.length)
      }
    } catch (e) {
      // A non-JSON body is already reported by the status check above.
    }
  })
  sleep(THINK_TIME)

  group('proposal list (status filter)', () => {
    const response = http.get(`${BASE_URL}/api/proposals?statusIn=SUBMITTED&statusIn=APPROVED`, {
      headers,
      tags: { name: 'proposal-list-filtered' },
    })
    check(response, { 'proposal list: status is 200': (r) => r.status === 200 })
  })
  sleep(THINK_TIME)

  group('proposal detail', () => {
    const list = http.get(`${BASE_URL}/api/proposals`, {
      headers,
      tags: { name: 'dashboard-proposals' },
    })
    let proposalId = null
    try {
      const body = list.json()
      if (Array.isArray(body) && body.length > 0) {
        proposalId = body[0].id
      }
    } catch (e) {
      // Handled by the fallback below.
    }

    if (proposalId !== null) {
      const detail = http.get(`${BASE_URL}/api/proposals/${proposalId}`, {
        headers,
        tags: { name: 'proposal-detail' },
      })
      check(detail, { 'proposal detail: status is 200': (r) => r.status === 200 })
    } else {
      // The account has no proposals yet: still exercise the endpoint, and accept
      // 403 (not the owner) as a legitimate answer rather than an error.
      const detail = http.get(`${BASE_URL}/api/proposals/1`, {
        headers,
        tags: { name: 'proposal-detail' },
      })
      check(detail, {
        'proposal detail: 200 or 403': (r) => r.status === 200 || r.status === 403,
      })
    }
  })
  sleep(THINK_TIME)

  group('notifications', () => {
    const response = http.get(`${BASE_URL}/api/notifications?userId=${cachedUserId}`, {
      headers,
      tags: { name: 'notifications' },
    })
    check(response, { 'notifications: status is 200': (r) => r.status === 200 })
  })
  sleep(THINK_TIME)

  group('call for proposals', () => {
    const response = http.get(`${BASE_URL}/api/proposal-announcements/available`, {
      headers,
      tags: { name: 'call-for-proposals' },
    })
    check(response, { 'call for proposals: status is 200': (r) => r.status === 200 })
  })
  sleep(THINK_TIME)
}

/**
 * The write journey: create a proposal. Capped so a long run cannot grow the
 * database without bound; the cap is reported in the summary.
 */
export function submitFlow() {
  const token = requireToken(__VU)
  if (!token) return
  const headers = authHeaders(token)

  // Cap this VU's submissions so a long run cannot grow the test database without
  // bound. `__ITER` is this VU's own iteration counter, which makes the cap exact.
  if (__ITER >= MAX_SUBMISSIONS_PER_VU) {
    submissionsSkipped.add(1)
    sleep(THINK_TIME * 2)
    return
  }

  const payload = {
    projectTitle: `[SYNTHETIC] k6 load-test draft VU${__VU}-${__ITER}`,
    status: 'DRAFT',
    duration: '12',
  }

  const response = http.post(`${BASE_URL}/api/proposals`, JSON.stringify(payload), {
    headers,
    tags: { name: 'proposal-submit' },
  })

  const accepted = check(response, {
    'proposal submit: status is 201': (r) => r.status === 201,
    'proposal submit: a proposal code was allocated': (r) => {
      try {
        const code = r.json('proposalCode')
        return typeof code === 'string' && code.length > 0
      } catch (e) {
        return false
      }
    },
  })

  if (accepted) {
    submissionsMade.add(1)
  } else {
    submissionsSkipped.add(1)
  }

  sleep(THINK_TIME * 2)
}


// --------------------------------------------------------------------- summary
export function handleSummary(data) {
  const metrics = data.metrics || {}
  const pick = (name, field) =>
    metrics[name] && metrics[name].values ? metrics[name].values[field] : null

  const lines = []
  lines.push('')
  lines.push('RPIS 500-user load test - summary')
  lines.push('=================================')
  lines.push(`target        : ${BASE_URL}`)
  lines.push(
    `peak VUs      : ${BROWSE_VUS} browse + ${SUBMIT_VUS} submit = ${BROWSE_VUS + SUBMIT_VUS}`,
  )
  lines.push(`requests      : ${pick('http_reqs', 'count')}`)
  lines.push(`throughput    : ${(pick('http_reqs', 'rate') || 0).toFixed(2)} req/s`)
  lines.push(`error rate    : ${((pick('http_req_failed', 'rate') || 0) * 100).toFixed(3)} %`)
  lines.push(`checks passed : ${((pick('checks', 'rate') || 0) * 100).toFixed(3)} %`)
  lines.push(`logins        : ${pick('rpis_logins', 'count')}`)
  lines.push(
    `submissions   : ${pick('rpis_submissions_made', 'count') || 0} ` +
      `(skipped at the ${MAX_SUBMISSIONS_PER_VU}-per-VU cap: ${pick('rpis_submissions_skipped_at_cap', 'count') || 0})`,
  )
  lines.push('')
  lines.push('latency (ms)          p50      p95      p99      max')
  const flows = [
    ['all requests', 'http_req_duration'],
    ['login', 'http_req_duration{name:login}'],
    ['dashboard', 'http_req_duration{name:dashboard-proposals}'],
    ['proposal list', 'http_req_duration{name:proposal-list-filtered}'],
    ['proposal detail', 'http_req_duration{name:proposal-detail}'],
    ['notifications', 'http_req_duration{name:notifications}'],
    ['calls', 'http_req_duration{name:call-for-proposals}'],
    ['submit', 'http_req_duration{name:proposal-submit}'],
  ]
  for (const [label, metric] of flows) {
    // k6 calls the median `med`; there is no `p(50)`.
    const p50 = pick(metric, 'med')
    if (p50 === null) {
      lines.push(`${label.padEnd(20)} (no data)`)
      continue
    }
    lines.push(
      label.padEnd(20) +
        String(Math.round(p50)).padStart(6) +
        String(Math.round(pick(metric, 'p(95)') || 0)).padStart(9) +
        String(Math.round(pick(metric, 'p(99)') || 0)).padStart(9) +
        String(Math.round(pick(metric, 'max') || 0)).padStart(9),
    )
  }
  lines.push('')

  const text = lines.join('\n')
  return {
    stdout: text,
    'load-tests/results/k6-500u-summary.json': JSON.stringify(data, null, 2),
    'load-tests/results/k6-500u-summary.txt': text,
  }
}

