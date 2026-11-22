/**
 * proponentNotifications.test.js
 * ---------------------------------------------------------------------------
 * The Proponent notification feed is one strict reverse-chronological list, and a
 * revision notification opens the revision form. The page is read as text (the same
 * technique the other guard tests use).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Notifications/NotificationPage.vue')
const DESTINATION = read('notificationDestination.js')

test('the Proponent feed is not split into groups that break the newest-first order', () => {
  assert.ok(PAGE.includes("role === 'PROPONENT'"), 'the Proponent case is explicit')
  assert.ok(PAGE.includes("role === 'OVCRIGE'"), 'the OVCRIGE feed is also one untitled list')
  assert.ok(PAGE.includes("key: 'all'"), 'the Proponent list is one group')
  assert.match(
    PAGE,
    /sort\(\(a, b\) => new Date\(b\.createdAt\) - new Date\(a\.createdAt\)\)/,
    'the list is sorted newest first',
  )
  assert.match(
    PAGE,
    /pageNotifications\.value/,
    'the visible groups come from the paged slice of the sorted filtered list',
  )
  // Signing out lives in the sidebar, below Profile Settings.
  assert.match(PAGE, /role === 'PROPONENT'/, 'the Proponent feed branch is kept')
})

test('the Proponent feeds are scoped to the signed-in account', () => {
  assert.ok(PAGE.includes("notificationStore.refresh(userStore.user?.id)"),
    'the list is loaded for the signed-in account')
  assert.ok(PAGE.includes('notificationStore.markAllAsRead()'), 'Mark All as Read uses the store')
})

test('a revision notification opens the proposal revision form', () => {
  assert.ok(DESTINATION.includes('NOTIFICATION_TYPES.REVISION'))
  assert.ok(DESTINATION.includes('NOTIFICATION_TYPES.REVISION_DEADLINE_REMINDER'))
  assert.ok(DESTINATION.includes('`/submit-revision/${proposalId}`'))
})
