/**
 * RPIS verification load balancer.
 * ---------------------------------------------------------------------------
 * A minimal round-robin reverse proxy used to verify that the application behaves
 * correctly behind a load balancer: that a token minted by one instance is accepted by
 * another, that a proposal or notification written through one instance is readable
 * through the other, and that a file uploaded through one instance can be downloaded
 * through the other.
 *
 * DISCLOSURE: this is a dev-grade balancer written for verification, NOT a production
 * one. It has no TLS, no connection draining beyond the in-flight request, no rate
 * limiting and no observability beyond a log line per request. A real deployment uses
 * nginx, HAProxy, an ingress controller or a cloud load balancer. What it does provide
 * is the property under test: requests are distributed across instances and an instance
 * that fails its health probe is taken out of rotation.
 *
 * Usage:
 *   node load-tests/lb/rpis-lb.mjs
 *   PORT=8080 TARGETS=http://localhost:8082,http://localhost:8083 node load-tests/lb/rpis-lb.mjs
 */
import http from 'node:http'
import { parse as parseUrl } from 'node:url'

const PORT = Number(process.env.PORT || 8080)
const TARGETS = (process.env.TARGETS || 'http://localhost:8082,http://localhost:8083')
  .split(',')
  .map((t) => t.trim().replace(/\/+$/, ''))
  .filter(Boolean)
const HEALTH_PATH = process.env.HEALTH_PATH || '/api/health/ready'
const HEALTH_INTERVAL_MS = Number(process.env.HEALTH_INTERVAL_MS || 5000)
const UPSTREAM_TIMEOUT_MS = Number(process.env.UPSTREAM_TIMEOUT_MS || 60000)

/**
 * An explicit keep-alive agent rather than the process-wide default.
 *
 * The default agent's behaviour has changed between Node releases (keep-alive became the
 * default in Node 19), and a proxy that opened a fresh upstream connection per request
 * would spend the run churning ephemeral ports and would make its own latency - not the
 * application's - the dominant term in the measurement. Stating it here makes the
 * measurement reproducible on any Node version.
 */
const agent = new http.Agent({
  keepAlive: true,
  maxSockets: Number(process.env.LB_MAX_SOCKETS || 512),
})

/** Health state per target. An unhealthy target is skipped until it recovers. */
const state = new Map(TARGETS.map((target) => [target, { healthy: false, checks: 0, failures: 0 }]))
let cursor = 0

/**
 * Per-instance counters, so a run can prove traffic was actually spread and can report
 * what each instance contributed rather than only the aggregate k6 saw.
 */
const stats = new Map(
  TARGETS.map((target) => [target, { served: 0, errors: 0, totalMs: 0, maxMs: 0 }]),
)

/** Records one request's duration against the instance that served it. */
function recordLatency(target, startedAt) {
  const entry = stats.get(target)
  const ms = Number(process.hrtime.bigint() - startedAt) / 1e6
  entry.totalMs += ms
  if (ms > entry.maxMs) entry.maxMs = ms
}

function healthyTargets() {
  return TARGETS.filter((t) => state.get(t).healthy)
}

function pickTarget() {
  const candidates = healthyTargets()
  if (candidates.length === 0) return null
  const target = candidates[cursor % candidates.length]
  cursor++
  return target
}

async function probe(target) {
  const entry = state.get(target)
  try {
    const response = await fetch(`${target}${HEALTH_PATH}`, {
      signal: AbortSignal.timeout(4000),
    })
    if (response.ok) {
      if (!entry.healthy) {
        console.log(`[lb] ${target} is UP`)
      }
      entry.healthy = true
      entry.failures = 0
    } else {
      throw new Error(`status ${response.status}`)
    }
  } catch (error) {
    entry.failures++
    // Two consecutive failures before removing a target, so one slow probe does not
    // take a healthy instance out of rotation.
    if (entry.healthy && entry.failures < 2) return
    if (entry.healthy) {
      console.log(`[lb] ${target} is DOWN (${error.message})`)
    }
    entry.healthy = false
  } finally {
    entry.checks++
  }
}

const server = http.createServer((clientRequest, clientResponse) => {
  const target = pickTarget()
  if (!target) {
    clientResponse.writeHead(503, { 'Content-Type': 'application/json' })
    clientResponse.end(JSON.stringify({ error: 'no healthy upstream' }))
    return
  }

  const startedAt = process.hrtime.bigint()
  const entry = stats.get(target)
  entry.served++

  const url = parseUrl(clientRequest.url)
  const upstream = new URL(target)
  const proxyRequest = http.request(
    {
      hostname: upstream.hostname,
      port: upstream.port,
      path: url.path,
      method: clientRequest.method,
      headers: { ...clientRequest.headers, host: upstream.host },
      timeout: UPSTREAM_TIMEOUT_MS,
      agent,
    },
    (upstreamResponse) => {
      // Expose which instance served the request, so a cross-instance test can assert it.
      clientResponse.writeHead(upstreamResponse.statusCode, {
        ...upstreamResponse.headers,
        'x-rpis-instance': target,
      })
      recordLatency(target, startedAt)
      upstreamResponse.pipe(clientResponse)
    },
  )

  proxyRequest.on('timeout', () => proxyRequest.destroy(new Error('upstream timeout')))
  proxyRequest.on('error', (error) => {
    entry.errors++
    recordLatency(target, startedAt)
    if (!clientResponse.headersSent) {
      clientResponse.writeHead(502, { 'Content-Type': 'application/json' })
    }
    clientResponse.end(JSON.stringify({ error: `upstream failed: ${error.message}` }))
  })

  clientRequest.pipe(proxyRequest)
})

// A dedicated endpoint so a test can read the distribution and health without guessing.
server.on('request', () => {})

const control = http.createServer((request, response) => {
  if (request.url === '/lb/status') {
    response.writeHead(200, { 'Content-Type': 'application/json' })
    response.end(
      JSON.stringify({
        targets: TARGETS.map((target) => {
          const entry = stats.get(target)
          return {
            target,
            healthy: state.get(target).healthy,
            served: entry.served,
            errors: entry.errors,
            meanMs: entry.served ? Number((entry.totalMs / entry.served).toFixed(2)) : 0,
            maxMs: Number(entry.maxMs.toFixed(2)),
          }
        }),
      }),
    )
    return
  }
  response.writeHead(404)
  response.end()
})

server.listen(PORT, () => {
  console.log(`[lb] listening on http://localhost:${PORT}`)
  console.log(`[lb] targets: ${TARGETS.join(', ')}`)
  for (const target of TARGETS) probe(target)
})

// The control endpoint lives on PORT + 1 so it can never be confused with application
// traffic and never reaches an instance.
control.listen(PORT + 1, () => {
  console.log(`[lb] control endpoint on http://localhost:${PORT + 1}/lb/status`)
})

setInterval(() => {
  for (const target of TARGETS) probe(target)
}, HEALTH_INTERVAL_MS)
