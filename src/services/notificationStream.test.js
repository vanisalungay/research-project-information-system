/**
 * notificationStream.test.js
 * The browser reads SSE with fetch because EventSource cannot send the Bearer header.
 * This test feeds a real ReadableStream through the client and checks that the frames
 * reach the store callbacks - the part the parser tests cannot prove on their own.
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { createNotificationStream } from './notificationStream.js'

const encoder = new TextEncoder()

function responseWith(chunks) {
  const stream = new ReadableStream({
    start(controller) {
      for (const chunk of chunks) controller.enqueue(encoder.encode(chunk))
      controller.close()
    },
  })
  return new Response(stream, {
    status: 200,
    headers: { 'content-type': 'text/event-stream' },
  })
}

function waitFor(check, timeoutMs = 2000) {
  return new Promise((resolve, reject) => {
    const started = Date.now()
    const tick = () => {
      if (check()) return resolve()
      if (Date.now() - started > timeoutMs) return reject(new Error('timed out'))
      setTimeout(tick, 10)
    }
    tick()
  })
}

test('the stream client sends the bearer token and delivers named frames', async () => {
  const requests = []
  const received = []
  let opened = false
  let closed = false

  const client = createNotificationStream({
    baseUrl: 'http://example.test',
    getToken: () => 'token-123',
    fetchImpl: async (url, options) => {
      requests.push({ url, options })
      return responseWith([
        'event: connected\ndata: {"userId":7}\n\n',
        'event: notification\ndata: {"id":42}\n\n',
      ])
    },
    onOpen: () => {
      opened = true
    },
    onEvent: (event, payload) => received.push([event, payload]),
    onError: () => {
      closed = true
    },
  })

  client.start()

  await waitFor(() => received.length === 2)
  await waitFor(() => closed)

  assert.equal(requests[0].url, 'http://example.test/api/notifications/stream')
  assert.equal(requests[0].options.headers.Authorization, 'Bearer token-123')
  assert.equal(requests[0].options.headers.Accept, 'text/event-stream')
  assert.equal(opened, true)
  assert.deepEqual(received, [
    ['connected', { userId: 7 }],
    ['notification', { id: 42 }],
  ])
})

test('a missing token is reported without opening a request', async () => {
  let requested = false
  let error = null

  const client = createNotificationStream({
    baseUrl: 'http://example.test',
    getToken: () => null,
    fetchImpl: async () => {
      requested = true
      return responseWith([])
    },
    onError: (failure) => {
      error = failure
    },
  })

  client.start()
  await waitFor(() => error !== null)

  assert.equal(requested, false)
  assert.equal(error.status, 401)
  assert.equal(client.isRunning(), false)
})

test('a non-success response is reported as an error', async () => {
  let error = null
  const client = createNotificationStream({
    baseUrl: 'http://example.test',
    getToken: () => 'token',
    fetchImpl: async () => new Response('', { status: 403 }),
    onError: (failure) => {
      error = failure
    },
  })

  client.start()
  await waitFor(() => error !== null)

  assert.equal(error.status, 403)
})
