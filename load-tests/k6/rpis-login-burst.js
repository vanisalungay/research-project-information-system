/**
 * RPIS login burst - 500 simultaneous logins (k6).
 * ---------------------------------------------------------------------------
 * The mixed 500-user test logs each virtual user in once, spread over the ramp.
 * This script isolates the authentication path: 500 VUs each perform exactly one
 * login at the same instant, which is the worst case for the login endpoint and
 * the only way to see whether the session/token path itself can take 500 users
 * arriving together.
 *
 * Usage:
 *   k6 run -e BASE_URL=http://localhost:8082 load-tests/k6/rpis-login-burst.js
 */
import http from 'k6/http'
import { check } from 'k6'
import { Counter } from 'k6/metrics'

const BASE_URL = (__ENV.BASE_URL || 'http://localhost:8082').replace(/\/+$/, '')
const PASSWORD = __ENV.RPIS_LOADTEST_PASSWORD || 'LoadTest#2026'
const EMAIL_DOMAIN = __ENV.EMAIL_DOMAIN || '@msunaawan.edu.ph'
const EMAIL_PREFIX = __ENV.EMAIL_PREFIX || 'loadtest.proponent.'
const USERS = Number(__ENV.USERS || 500)

const logins = new Counter('rpis_logins')

export const options = {
  // k6 only computes the percentiles it is asked for.
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
  // Every VU logs in exactly once, all of them starting together.
  scenarios: {
    login_burst: {
      executor: 'shared-iterations',
      vus: USERS,
      iterations: USERS,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{name:login}': ['p(95)<1500', 'p(99)<3000'],
    checks: ['rate>0.99'],
  },
}

export default function () {
  const index = ((__VU - 1) % USERS) + 1
  const email = EMAIL_PREFIX + String(index).padStart(4, '0') + EMAIL_DOMAIN

  const response = http.post(
    `${BASE_URL}/api/users/login`,
    JSON.stringify({ email, password: PASSWORD, role: 'PROPONENT' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } },
  )

  const ok = check(response, {
    'login: status is 200': (r) => r.status === 200,
    'login: a token was issued': (r) => {
      try {
        return typeof r.json('token') === 'string' && r.json('token').length > 0
      } catch (e) {
        return false
      }
    },
  })

  if (ok) {
    logins.add(1)
  }
}

export function handleSummary(data) {
  const metrics = data.metrics || {}
  const pick = (name, field) =>
    metrics[name] && metrics[name].values ? metrics[name].values[field] : null

  const lines = []
  lines.push('')
  lines.push(`RPIS login burst - ${USERS} simultaneous logins`)
  lines.push('================================================')
  lines.push(`target      : ${BASE_URL}`)
  lines.push(`VUs         : ${USERS} (one login each, all at once)`)
  lines.push(`logins ok   : ${pick('rpis_logins', 'count')} of ${USERS}`)
  lines.push(`error rate  : ${((pick('http_req_failed', 'rate') || 0) * 100).toFixed(3)} %`)
  const metric = 'http_req_duration{name:login}'
  // k6 calls the median `med`; there is no `p(50)`.
  const p = (field) => Math.round(pick(metric, field) || 0)
  lines.push(
    `latency (ms): p50 ${p('med')}  p95 ${p('p(95)')}  p99 ${p('p(99)')}  max ${p('max')}`,
  )
  lines.push('')

  const text = lines.join('\n')
  return {
    stdout: text,
    'load-tests/results/k6-login-burst-summary.json': JSON.stringify(data, null, 2),
    'load-tests/results/k6-login-burst-summary.txt': text,
  }
}
