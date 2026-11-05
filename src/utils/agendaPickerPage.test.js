/**
 * agendaPickerPage.test.js
 * ---------------------------------------------------------------------------
 * Guards how the searchable agenda picker is wired into the Add Agenda form
 * (src/views/Admin/PriorityAgendas.vue).
 *
 * The picker must never become a static dropdown: its options are the agenda records
 * the page loads from the API, and the create / edit / activate / toggle workflows
 * around it must survive. The files are read as text - the technique
 * navigation.test.js and the dashboard guard tests already use - so this needs no
 * browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PAGE = read('../views/Admin/PriorityAgendas.vue')
const PICKER = read('../components/AgendaPicker.vue')

test('the Add Agenda modal offers the picker, fed from the records the API returned', () => {
  assert.ok(PAGE.includes("import AgendaPicker from '@/components/AgendaPicker.vue'"))
  assert.ok(PAGE.includes(':agendas="agendaOptions"'), 'the options come from the loaded sets')
  assert.ok(PAGE.includes('agendaCatalogue(sets.value)'), 'built from the API data, not a literal')

  // The options must be derived, never a hard-coded list. (The page's own input
  // placeholder "e.g., DAGAT Program" is only a hint, not an option.)
  assert.ok(!/agendaOptions\s*=\s*\[/.test(PAGE), 'the options are never a literal list')
  for (const file of [['page', PAGE], ['picker', PICKER]]) {
    assert.ok(
      !/name:\s*'(DAGAT|PUNLA|KALIKASAN|TANGLAW)/.test(file[1]),
      'the ' + file[0] + ' must not contain hard-coded agenda records',
    )
  }
})

test('the picker is offered only while creating an agenda', () => {
  assert.ok(
    PAGE.includes('v-if="!editingItemId"'),
    'an existing agenda is edited in place, and its stable key can never change',
  )
})

test('selecting an existing agenda only prefills the form, through the shared rules', () => {
  assert.ok(PAGE.includes('agendaPrefill(record'), 'the prefill rule is shared, not reimplemented')
  assert.ok(PAGE.includes('frameworkKeys(sets.value, itemForm.value.setId)'))
  assert.ok(PICKER.includes('searchAgendas'), 'the search rule is shared, not reimplemented')
  assert.ok(PICKER.includes('createAgendaLabel'), 'the create action names what it creates')
})

test('the existing create, edit, activate and toggle workflows are untouched', () => {
  for (const kept of [
    "api.get('/api/priority-agendas/sets')",
    "api.post('/api/priority-agendas/sets', payload)",
    '/api/priority-agendas/sets/',
    '/api/priority-agendas/items/',
    'Agenda name is required.',
    'Agenda Added',
    'Agenda Updated',
  ]) {
    assert.ok(PAGE.includes(kept), kept + ' must still be part of the page')
  }
})

test('the picker itself never calls the API', () => {
  assert.ok(!/api\.(get|post|put|delete)/.test(PICKER), 'the picker only searches and emits')
})
