/**
 * deanAttention.test.js
 * ---------------------------------------------------------------------------
 * Guards the Dean dashboard badge count (src/utils/deanAttention.js).
 *
 * The badge counts the real pending endorsement requests plus the unread notifications, and
 * must not count one event twice just because an endorsement request also raised an
 * ENDORSEMENT notification. The rules are pure functions, so they are verified without a
 * browser or a server.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { deanAttentionCount } from './deanAttention.js'

const unread = (extra = {}) => ({ id: 1, isRead: false, ...extra })
const read = (extra = {}) => ({ id: 2, isRead: true, ...extra })

test('the count is the pending requests plus the unread notifications', () => {
  assert.equal(deanAttentionCount(2, [unread(), unread({ id: 3 }), read()]), 4)
})

test('an endorsement notification never counts twice', () => {
  // The request is pending and it also raised an ENDORSEMENT notification for the same
  // event, so the event is one item, not two.
  assert.equal(deanAttentionCount(1, [unread({ type: 'ENDORSEMENT', proposalId: 9 })]), 1)

  // A notification about something else is a separate event, so it is added.
  assert.equal(deanAttentionCount(1, [unread({ type: 'PROPOSAL_UPDATE' })]), 2)
})

test('a read notification is not waiting for attention', () => {
  assert.equal(deanAttentionCount(0, [read(), read({ id: 4 })]), 0)
  assert.equal(deanAttentionCount(0, [unread({ isRead: true })]), 0)
  // The API serialises the flag as isRead; the older `read` shape is understood too.
  assert.equal(deanAttentionCount(0, [{ id: 5, read: false }]), 1)
})

test('a missing or invalid input counts as zero, never a wrong badge', () => {
  assert.equal(deanAttentionCount(null, null), 0)
  assert.equal(deanAttentionCount(undefined, undefined), 0)
  assert.equal(deanAttentionCount(-3, 'nope'), 0)
  assert.equal(deanAttentionCount('2', []), 2)
  assert.equal(deanAttentionCount(0, []), 0)
})
