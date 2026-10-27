/**
 * priorityAgendas.test.js
 * ---------------------------------------------------------------------------
 * Verifies the rules behind section 05 (Priority Agenda) now that the framework
 * heading and the agenda list are master data: the heading is composed from the
 * stored framework name and coverage period (never a hard-coded year range), the
 * form state is built from the configured agendas, a proposal's own historical
 * selections survive a framework change, and the submission payload keeps the shape
 * the backend expects.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  agendaLabelIndex,
  agendaPayload,
  agendaPrograms,
  buildAgendaState,
  frameworkLabel,
  mergeAgendaState,
  resolveAgendaLabel,
  selectedAgendas,
} from './priorityAgendas.js'

const ACTIVE = {
  agendaSetId: 1,
  frameworkName: 'MSUN RIIDE',
  coverageStartYear: 2025,
  coverageEndYear: 2028,
  agendas: [
    { id: 1, agendaKey: 'dagat', name: 'DAGAT Program', subLabel: 'Commodity', displayOrder: 1 },
    { id: 2, agendaKey: 'punla', name: 'PUNLA Program', subLabel: 'Priority Topic', displayOrder: 2 },
  ],
}

test('the heading is composed from the stored framework and coverage period', () => {
  assert.equal(frameworkLabel(ACTIVE), '(based on MSUN RIIDE 2025\u20132028)')

  // A framework an administrator configures later needs no code change.
  assert.equal(
    frameworkLabel({ frameworkName: 'MSUN RIDE', coverageStartYear: 2029, coverageEndYear: 2032 }),
    '(based on MSUN RIDE 2029\u20132032)',
  )
})

test('the heading is empty when nothing is configured', () => {
  assert.equal(frameworkLabel(null), '')
  assert.equal(frameworkLabel({}), '')
  assert.equal(frameworkLabel({ frameworkName: '   ' }), '')
  // A framework without a coverage period still names itself.
  assert.equal(frameworkLabel({ frameworkName: 'MSUN RIDE' }), '(based on MSUN RIDE)')
})

test('the programs follow the configured order and skip blank keys', () => {
  assert.deepEqual(agendaPrograms(ACTIVE.agendas), [
    { key: 'dagat', label: 'DAGAT Program', subLabel: 'Commodity', description: '' },
    { key: 'punla', label: 'PUNLA Program', subLabel: 'Priority Topic', description: '' },
  ])
  assert.deepEqual(agendaPrograms(null), [])
  assert.deepEqual(agendaPrograms([{ name: 'No key' }, { agendaKey: 'dagat', name: 'D' }]), [
    { key: 'dagat', label: 'D', subLabel: '', description: '' },
  ])
})

test('a new submission starts with every configured agenda unchecked', () => {
  assert.deepEqual(buildAgendaState(ACTIVE.agendas), {
    dagat: { selected: false, value: '' },
    punla: { selected: false, value: '' },
  })
  assert.deepEqual(buildAgendaState([]), {})
})

test('an existing proposal keeps its selections for the active framework', () => {
  const state = mergeAgendaState(ACTIVE.agendas, [
    { agendaKey: 'dagat', selected: true, value: 'Coastal monitoring' },
  ])

  assert.deepEqual(state, {
    dagat: { selected: true, value: 'Coastal monitoring' },
    punla: { selected: false, value: '' },
  })
})

test('a selection of a deactivated framework is preserved, never dropped', () => {
  // The proposal was submitted under the old framework; the new one has different
  // agendas. The historical row stays in the payload (and is shown read-only).
  const state = mergeAgendaState(ACTIVE.agendas, [
    { agendaKey: 'dagat', selected: true, value: 'Kept' },
    { agendaKey: 'blue-economy', selected: true, value: 'Legacy selection' },
  ])

  assert.deepEqual(state['blue-economy'], { selected: true, value: 'Legacy selection' })
  assert.equal(Object.keys(state).length, 3)
})

test('the saved rows may also arrive as an object (older shape)', () => {
  const state = mergeAgendaState(ACTIVE.agendas, {
    dagat: { selected: true, value: 'X' },
  })
  assert.equal(state.dagat.selected, true)
  assert.equal(state.dagat.value, 'X')
})

test('the submission payload keeps the shape the backend expects', () => {
  const state = {
    dagat: { selected: true, value: 'Coastal' },
    punla: { selected: false, value: '' },
  }

  assert.deepEqual(agendaPayload(state), {
    dagat: { selected: true, value: 'Coastal' },
    punla: { selected: false, value: '' },
  })
  assert.deepEqual(agendaPayload(null), {})
})

test('the selected agendas are listed in order for the printout', () => {
  assert.deepEqual(
    selectedAgendas({
      dagat: { selected: true, value: 'A' },
      punla: { selected: false, value: 'B' },
      kalikasan: { selected: true, value: 'C' },
    }),
    [
      { key: 'dagat', value: 'A' },
      { key: 'kalikasan', value: 'C' },
    ],
  )
})

test('a historical agenda label falls back to the stable key', () => {
  const index = agendaLabelIndex([
    { agendaKey: 'dagat', name: 'DAGAT Program', subLabel: 'Commodity' },
  ])

  assert.equal(resolveAgendaLabel(index, 'dagat'), 'DAGAT Program (Commodity)')
  // Unknown key (or a failed lookup) degrades to the key, never to a wrong name.
  assert.equal(resolveAgendaLabel(index, 'blue-economy'), 'blue-economy')
  assert.equal(resolveAgendaLabel({}, 'dagat'), 'dagat')
  assert.equal(resolveAgendaLabel(index, undefined), 'Priority Agenda')
})
