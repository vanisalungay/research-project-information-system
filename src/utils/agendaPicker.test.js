/**
 * agendaPicker.test.js
 * ---------------------------------------------------------------------------
 * Verifies the rules behind the searchable "existing agenda" dropdown of the Add
 * Agenda form (src/utils/agendaPicker.js).
 *
 * The risk this covers is that the dropdown stops reflecting the database: that it
 * invents options, loses an agenda an administrator already saved, offers a record
 * that is already part of the framework being edited, or reuses a stable key that is
 * already taken. The rules are pure functions, so they are verified without a browser
 * and without the API.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  agendaCatalogue,
  agendaPrefill,
  createAgendaLabel,
  existingAgendaByName,
  frameworkHeading,
  frameworkKeys,
  normalizeAgendaText,
  searchAgendas,
} from './agendaPicker.js'

/** Two saved frameworks, exactly as GET /api/priority-agendas/sets returns them. */
const SETS = [
  {
    id: 1,
    frameworkName: 'MSUN RIIDE',
    coverageStartYear: 2025,
    coverageEndYear: 2028,
    active: false,
    agendas: [
      { id: 11, agendaKey: 'dagat', name: 'DAGAT Program', subLabel: 'Commodity', description: 'Fisheries and marine resources' },
      { id: 12, agendaKey: 'punla', name: 'PUNLA Program', subLabel: 'Priority Topic', description: 'Agriculture and food security' },
      { id: 13, agendaKey: 'tanglaw', name: 'TANGLAW Program', subLabel: 'Sector', description: 'Transforming academia through nurturing growth' },
    ],
  },
  {
    id: 2,
    frameworkName: 'MSUN RIDE',
    coverageStartYear: 2029,
    coverageEndYear: 2032,
    active: true,
    agendas: [
      { id: 21, agendaKey: 'dagat', name: 'DAGAT Program', subLabel: 'Commodity', description: 'Fisheries and marine resources' },
      { id: 22, agendaKey: 'kalikasan', name: 'KALIKASAN Program', subLabel: 'Priority Topic', description: 'Environment and climate resilience' },
    ],
  },
]

test('the framework heading is composed from the stored name and coverage period', () => {
  assert.equal(frameworkHeading(SETS[0]), 'MSUN RIIDE 2025\u20132028')
  assert.equal(frameworkHeading({ frameworkName: 'MSUN RIDE' }), 'MSUN RIDE')
  assert.equal(frameworkHeading(null), '')
})

test('the options are the saved records, never a hard-coded list', () => {
  const catalogue = agendaCatalogue(SETS)

  assert.deepEqual(
    catalogue.map((record) => record.name),
    ['DAGAT Program', 'PUNLA Program', 'TANGLAW Program', 'KALIKASAN Program'],
  )

  // An agenda saved in two frameworks is one option, and both are remembered.
  const dagat = catalogue.find((record) => record.name === 'DAGAT Program')
  assert.deepEqual(dagat.setIds, [1, 2])
  assert.equal(dagat.reuseCount, 2)
  assert.equal(dagat.frameworkHeading, 'MSUN RIIDE 2025\u20132028')

  // Nothing is invented for a framework with no agenda, or for no frameworks at all.
  assert.deepEqual(agendaCatalogue([{ id: 9, frameworkName: 'Empty', agendas: [] }]), [])
  assert.deepEqual(agendaCatalogue([]), [])
  assert.deepEqual(agendaCatalogue(null), [])
})

test('a keyword returns the matching records, best match first', () => {
  const catalogue = agendaCatalogue(SETS)

  assert.deepEqual(searchAgendas(catalogue, 'dagat').map((r) => r.name), ['DAGAT Program'])
  assert.deepEqual(searchAgendas(catalogue, 'kalikasan').map((r) => r.name), ['KALIKASAN Program'])

  // A word inside the name matches every agenda that has it.
  assert.deepEqual(
    searchAgendas(catalogue, 'program').map((r) => r.name),
    ['DAGAT Program', 'KALIKASAN Program', 'PUNLA Program', 'TANGLAW Program'],
  )

  // The prompt and the description are searched too, not only the name.
  assert.deepEqual(searchAgendas(catalogue, 'commodity').map((r) => r.name), ['DAGAT Program'])
  assert.deepEqual(searchAgendas(catalogue, 'food security').map((r) => r.name), ['PUNLA Program'])

  // Case and stray spaces do not matter.
  assert.deepEqual(searchAgendas(catalogue, '  DaGaT ').map((r) => r.name), ['DAGAT Program'])

  // A keyword nobody matches yields nothing, which is what opens the create action.
  assert.deepEqual(searchAgendas(catalogue, 'blue economy'), [])
})

test('a record already in the framework being edited is flagged, not silently offered', () => {
  const catalogue = agendaCatalogue(SETS)

  assert.equal(searchAgendas(catalogue, 'dagat', { targetSetId: 3 })[0].alreadyInFramework, false)
  assert.equal(searchAgendas(catalogue, 'dagat', { targetSetId: 1 })[0].alreadyInFramework, true)

  // Of two equally good matches, a reusable one is listed before one already there.
  const matches = searchAgendas(catalogue, 'program', { targetSetId: 1 })
  assert.equal(matches[0].alreadyInFramework, false)
})

test('a blank keyword lists the whole catalogue, and the list can be capped', () => {
  const catalogue = agendaCatalogue(SETS)

  assert.equal(searchAgendas(catalogue, '').length, 4)
  assert.equal(searchAgendas(catalogue, '   ').length, 4)
  assert.equal(searchAgendas(catalogue, '', { limit: 2 }).length, 2)
})

test('selecting a record fills the form fields and keeps the stable key', () => {
  const catalogue = agendaCatalogue(SETS)
  const punla = catalogue.find((record) => record.name === 'PUNLA Program')

  assert.deepEqual(agendaPrefill(punla, { existingKeys: frameworkKeys(SETS, 2) }), {
    name: 'PUNLA Program',
    subLabel: 'Priority Topic',
    description: 'Agriculture and food security',
    agendaKey: 'punla',
    keyTaken: false,
  })
})

test('a stable key already used in this framework is left blank instead of clashing', () => {
  const catalogue = agendaCatalogue(SETS)
  const dagat = catalogue.find((record) => record.name === 'DAGAT Program')

  // "dagat" is already used inside framework 2, so reusing the agenda there must not
  // reuse the key - the backend refuses a duplicate key in the same set.
  const prefill = agendaPrefill(dagat, { existingKeys: frameworkKeys(SETS, 2) })

  assert.equal(prefill.agendaKey, '')
  assert.equal(prefill.keyTaken, true)
  assert.equal(prefill.name, 'DAGAT Program')
})

test('the framework keys are exactly the ones already stored in it', () => {
  assert.deepEqual(frameworkKeys(SETS, 1), ['dagat', 'punla', 'tanglaw'])
  assert.deepEqual(frameworkKeys(SETS, 2), ['dagat', 'kalikasan'])
  assert.deepEqual(frameworkKeys(SETS, 99), [])
})

test('an existing name is recognised, and a new one is not', () => {
  const catalogue = agendaCatalogue(SETS)

  assert.equal(existingAgendaByName(catalogue, 'dagat program').name, 'DAGAT Program')
  assert.equal(existingAgendaByName(catalogue, 'Blue Economy Program'), null)
  assert.equal(existingAgendaByName(catalogue, ''), null)
})

test('the create action names what will be created', () => {
  assert.equal(createAgendaLabel('Blue Economy'), 'Create new agenda \u201cBlue Economy\u201d')
  assert.equal(createAgendaLabel(''), 'Create a new agenda')
})

test('searching and prefilling never mutate the saved records', () => {
  const catalogue = agendaCatalogue(SETS)
  const before = JSON.stringify(catalogue)

  searchAgendas(catalogue, 'dagat', { targetSetId: 1 })
  agendaPrefill(catalogue[0], { existingKeys: ['dagat'] })

  assert.equal(JSON.stringify(catalogue), before, 'the picker only reads what it is given')
})

test('text is matched without case or accent differences', () => {
  assert.equal(normalizeAgendaText('  Kalikasan   Program '), 'kalikasan program')
})
