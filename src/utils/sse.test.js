/**
 * sse.test.js
 * The notification stream is read with fetch, so the frame parser has to survive
 * arbitrary chunk boundaries, CRLF and the comments/heartbeats the server sends.
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { createSseParser, parseSseFrame } from './sse.js'

test('a complete frame is parsed into its event name and JSON payload', () => {
  assert.deepEqual(parseSseFrame('event: notification\ndata: {"id":7}'), {
    event: 'notification',
    id: null,
    data: '{"id":7}',
  })
})

test('comments and empty frames are ignored', () => {
  assert.equal(parseSseFrame(''), null)
  assert.equal(parseSseFrame(': keep-alive'), null)
})

test('multiple data lines are joined with a newline', () => {
  const frame = parseSseFrame('event: message\ndata: one\ndata: two')
  assert.equal(frame.data, 'one\ntwo')
})

test('the event name defaults to "message"', () => {
  assert.equal(parseSseFrame('data: hello').event, 'message')
})

test('a frame split across chunks is delivered once', () => {
  const received = []
  const parser = createSseParser((event, payload) => received.push([event, payload]))

  parser.push('event: notifi')
  parser.push('cation\ndata: {"id":')
  parser.push('7}\n\n')
  parser.push('event: ping\ndata: {"ping":true}\n\n')

  assert.deepEqual(received, [
    ['notification', { id: 7 }],
    ['ping', { ping: true }],
  ])
})

test('CRLF and lone CR line endings are accepted', () => {
  const received = []
  const parser = createSseParser((event, payload) => received.push([event, payload]))

  parser.push('event: notification\r\ndata: {"id":1}\r\n\r\n')
  parser.push('event: ping\rdata: {"ping":true}\r\r')

  assert.deepEqual(received, [
    ['notification', { id: 1 }],
    ['ping', { ping: true }],
  ])
})

test('several frames in one chunk are all delivered', () => {
  const received = []
  const parser = createSseParser((event, payload) => received.push([event, payload]))

  parser.push(
    'event: notification\ndata: {"id":1}\n\n' +
      'event: notification\ndata: {"id":2}\n\n' +
      'event: notifications-changed\ndata: {"reason":"read"}\n\n',
  )

  assert.deepEqual(received, [
    ['notification', { id: 1 }],
    ['notification', { id: 2 }],
    ['notifications-changed', { reason: 'read' }],
  ])
})

test('a non-JSON payload is still delivered as text', () => {
  const received = []
  const parser = createSseParser((event, payload) => received.push([event, payload]))

  parser.push('event: message\ndata: plain text\n\n')

  assert.deepEqual(received, [['message', 'plain text']])
})

test('reset drops an incomplete buffer', () => {
  const received = []
  const parser = createSseParser((event, payload) => received.push([event, payload]))

  parser.push('event: notification\ndata: {"id":1}')
  parser.reset()
  parser.push('\n\n')

  assert.deepEqual(received, [])
})
