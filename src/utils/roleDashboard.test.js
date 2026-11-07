/**
 * roleDashboard.test.js
 * ---------------------------------------------------------------------------
 * The dashboard each role lands on. The change-password page sends a user there after a
 * successful change, so it has to agree with the login redirect for every role.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import { ROLE_DASHBOARDS, dashboardForRole } from './roleDashboard.js'

// The role vocabulary, read from the store source so this test needs no app alias.
const STORE = readFileSync(fileURLToPath(new URL('../stores/userData.js', import.meta.url)), 'utf8')
const ROLE_KEYS = [
  ...STORE.match(/export const UserRole = \{([\s\S]*?)\n\}/)[1].matchAll(/([A-Z_]+):\s*'[A-Z_]+'/g),
].map((match) => match[1])

test('every role in the application has a dashboard', () => {
  assert.ok(ROLE_KEYS.length >= 8, 'the role vocabulary must be readable')
  for (const role of ROLE_KEYS) {
    assert.ok(dashboardForRole(role).startsWith('/'), role + ' must resolve to a route')
  }
})

test('the mappings match the routes the application actually uses', () => {
  assert.equal(dashboardForRole('RPS_ADMIN'), '/rpsadmin-dash')
  assert.equal(dashboardForRole('RPS_STAFF'), '/rpsstaff-dash')
  assert.equal(dashboardForRole('REC'), '/rec-dash')
  assert.equal(dashboardForRole('OC'), '/oc-dashboard')
  assert.equal(dashboardForRole('DEAN'), '/dean-dashboard')
  assert.equal(dashboardForRole('PROPONENT'), '/proponent-dashboard')
})

test('an unknown role falls back to the shared home', () => {
  assert.equal(dashboardForRole('SOMETHING_ELSE'), '/home')
  assert.equal(dashboardForRole(undefined), '/home')
  assert.ok(Object.keys(ROLE_DASHBOARDS).length > 0)
})