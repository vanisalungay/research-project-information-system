/**
 * deactivateFramework.test.js
 * ---------------------------------------------------------------------------
 * The program-level Deactivate Program action on Priority Agendas. It is separate
 * from the agenda-level Edit / Activate / Deactivate actions: it deactivates the
 * whole framework through a dedicated endpoint, after a confirmation that names the
 * program, and never touches the agendas inside it.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Admin/PriorityAgendas.vue')

test('an active program offers a program-level Deactivate Program action', () => {
  assert.ok(PAGE.includes('Deactivate Program'), 'the action is labelled')
  assert.match(PAGE, /v-if="set\.active"[\s\S]*?class="deactivate-btn btn-danger"/,
    'it is offered for an active program, with the destructive style')
  assert.match(PAGE, /class="deactivate-btn btn-danger"[\s\S]*?Deactivate Program/)
  assert.match(PAGE, /v-if="!set\.active"[\s\S]*?>Activate<\/button>/,
    'an inactive program keeps the existing Activate action')
  // The badge is still driven by the real program status.
  assert.ok(PAGE.includes('priorityAgendaStatusLabel(set.active)'))
})

test('deactivation asks for confirmation and names the program', () => {
  assert.ok(PAGE.includes("title: 'Deactivate Framework / Program'"), 'the required title')
  assert.ok(PAGE.includes("confirmText: 'Deactivate Program'"), 'the required action label')
  assert.ok(PAGE.includes("type: 'danger'"), 'a destructive confirmation')
  assert.ok(PAGE.includes('Are you sure you want to deactivate this framework/program?'),
    'the required question')
  assert.ok(PAGE.includes('You are about to deactivate ${label}.'), 'the real program is named')
  assert.ok(PAGE.includes('Individual agenda statuses will not be'),
    'the agenda statuses are explicitly spared')
  assert.ok(!PAGE.includes('You are about to deactivate MSUN RIIDE'),
    'no program name is hardcoded')
})

test('deactivation persists through the program-level endpoint and reloads', () => {
  assert.ok(PAGE.includes('api.put(`/api/priority-agendas/sets/${set.id}/deactivate`)'),
    'the dedicated program endpoint is used')
  assert.ok(PAGE.includes('await loadSets()'), 'the list is refreshed after the change')
  assert.ok(PAGE.includes("title: 'Framework Deactivated'"), 'success feedback is shown')
  assert.ok(PAGE.includes("title: 'Deactivation Failed'"), 'and failures are reported')
})

test('agenda-level status stays separate from program-level status', () => {
  assert.ok(PAGE.includes('api.put(`/api/priority-agendas/items/${item.id}`'),
    'agenda updates keep their own endpoint')
  assert.ok(PAGE.includes("item.active ? 'Deactivate' : 'Activate'"),
    'agenda activation is unchanged')
  assert.ok(PAGE.includes('itemForm.active'), 'the agenda form flag is unchanged')
  assert.ok(PAGE.includes('set.active'), 'the program badge and actions use the set flag')
})
