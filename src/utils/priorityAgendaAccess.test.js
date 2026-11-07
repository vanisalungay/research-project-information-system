/**
 * priorityAgendaAccess.test.js
 * ---------------------------------------------------------------------------
 * Guards the transfer of the Priority Agenda page from ORPS Staff to ORPS Admin.
 *
 * The page itself was not rewritten: it is the same component, the same endpoints and the
 * same records - only the role that may reach it changed. All three layers that enforce that
 * are pinned here (the sidebar item, the route guard and the backend rule), because hiding a
 * menu item alone would still let an ORPS Staff account open the page by typing its URL.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const exists = (path) => existsSync(fileURLToPath(new URL(path, import.meta.url)))

const NAV = read('../components/NavMenu.vue')
const ROUTER = read('../router/index.js')
const SECURITY = read('../../backend/src/main/java/com/rpis/backend/config/SecurityConfig.java')
const PAGE = read('../views/Admin/PriorityAgendas.vue')

test('the sidebar item belongs to the administrator and to nobody else', () => {
  const item = NAV.match(/text: 'Priority Agendas',[\s\S]*?section: 'main',/)
  assert.ok(item, 'the Priority Agendas navigation item must exist')
  assert.ok(item[0].includes('roles: [UserRole.RPS_ADMIN]'), 'it must be an administrator item')
  assert.ok(!item[0].includes('RPS_STAFF'), 'and must not list ORPS Staff')
})

test('the route guard restricts the page to the administrator', () => {
  const rule = ROUTER.match(
    /match: \(path\) => path\.startsWith\('\/priority-agendas'\),\s*\n\s*roles: \[([^\]]+)\]/,
  )
  assert.ok(rule, 'the /priority-agendas route must be role-restricted')
  assert.equal(rule[1].trim(), 'UserRole.RPS_ADMIN', 'and to the administrator only')
  // The page is still the one existing component - no second Priority Agenda page.
  assert.ok(ROUTER.includes('component: PriorityAgendas'), 'the route still uses the page')
  assert.ok(exists('../views/Admin/PriorityAgendas.vue'), 'the component still exists')
})

test('the backend allows the administrator to manage the agenda and keeps the reads open', () => {
  assert.match(
    SECURITY,
    /\.requestMatchers\("\/api\/priority-agendas\/\*\*"\)\s*\n\s*\.hasRole\(UserRoles\.RPS_ADMIN\)/,
    'the management endpoints must be administrator-only',
  )
  assert.ok(
    SECURITY.includes('\"/api/priority-agendas/active\", \"/api/priority-agendas/lookup\"'),
    'the two reads the Proponent and Call for Proposal forms use must stay available',
  )
  assert.ok(
    !/priority-agendas[\s\S]{0,90}hasAnyRole\(UserRoles\.RPS_ADMIN, UserRoles\.RPS_STAFF\)/.test(SECURITY),
    'ORPS Staff must not keep access to the agenda endpoints',
  )
})

test('the page itself is untouched: same component, same endpoints, same records', () => {
  assert.ok(PAGE.includes("api.get('/api/priority-agendas/sets')"), 'it still reads the saved sets')
  assert.ok(PAGE.includes('/api/priority-agendas/sets/'), 'and still writes them')
  assert.ok(PAGE.includes('/api/priority-agendas/items/'), 'including item edits')
  assert.ok(PAGE.includes('loadSets'), 'the existing load is untouched')
})