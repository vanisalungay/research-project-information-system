import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const STORE = read('../stores/notifications.js')
const CLIENT = read('../services/notificationStream.js')

test('a reconnect refreshes the persisted list to recover missed events', () => {
  assert.match(STORE, /onOpen:\s*\(\)\s*=>\s*\{[\s\S]{0,220}refresh\(userId\.value\)/)
})

test('a read-state change in another tab triggers a fresh read', () => {
  assert.match(STORE, /notifications-changed[\s\S]{0,140}scheduleRefresh\(0\)/)
})

test('disconnect aborts the stream and clears the timers', () => {
  assert.match(STORE, /function disconnect\(\)[\s\S]{0,400}connection\.stop\(\)/)
  assert.match(STORE, /function disconnect\(\)[\s\S]{0,400}clearRetryTimer\(\)/)
  assert.match(STORE, /function disconnect\(\)[\s\S]{0,400}clearRefreshTimer\(\)/)
})

test('the client watchdog aborts a silent stream and reports an error', () => {
  assert.match(CLIENT, /watchdogMs/)
  assert.match(CLIENT, /stop\(\)/)
  assert.match(CLIENT, /watchdog:\s*true/)
})
