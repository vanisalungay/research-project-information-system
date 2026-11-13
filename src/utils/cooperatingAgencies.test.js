/**
 * cooperatingAgencies.test.js
 * ---------------------------------------------------------------------------
 * Covers the client-side rules of the "Cooperating Agencies" checklist
 * (section 02 of the Create Proposal form): the checklist is ordered by the
 * lower-cased agency name, the selection is normalized, and a selection becomes
 * the legacy text the existing screens read.
 *
 * The proponent interface is read-only over the directory, so the module must not
 * expose any way to create an agency - the first test locks that in.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import * as cooperatingAgencies from './cooperatingAgencies.js'
import {
  agencyNameKey,
  describeAgencySelection,
  joinAgencyNames,
  normalizeAgencyName,
  normalizeSelectedIds,
  sortAgenciesByName,
} from './cooperatingAgencies.js'

const DIRECTORY = [
  { id: 1, name: 'Department of Science and Technology (DOST)' },
  { id: 2, name: 'Department of the Interior and Local Government (DILG)' },
  { id: 3, name: 'Department of Tourism (DOT)' },
  { id: 4, name: 'Local Government Unit (LGU)' },
]

test('the proponent module cannot create an agency', () => {
  // Creating an agency is an administrator action (POST /api/cooperating-agencies
  // is restricted to the RPS roles), so the proponent-side helpers that validated
  // and merged a new agency name are gone and must not come back.
  for (const removed of [
    'validateAgencyName',
    'mergeAgency',
    'findAgencyByName',
    'AGENCY_NAME_MAX_LENGTH',
  ]) {
    assert.equal(cooperatingAgencies[removed], undefined, `${removed} must not be exported`)
  }

  // What is left is read-only: ordering, selection and the legacy text.
  assert.deepEqual(Object.keys(cooperatingAgencies).sort(), [
    'agencyNameKey',
    'describeAgencySelection',
    'joinAgencyNames',
    'normalizeAgencyName',
    'normalizeSelectedIds',
    'sortAgenciesByName',
  ])
})

test('an agency name is trimmed and its inner whitespace collapsed', () => {
  assert.equal(normalizeAgencyName('  Bureau   of Fisheries  '), 'Bureau of Fisheries')
  assert.equal(normalizeAgencyName('\tPhilippine\tSpace\nAgency '), 'Philippine Space Agency')
  assert.equal(normalizeAgencyName(null), '')
  assert.equal(normalizeAgencyName(undefined), '')
  assert.equal(agencyNameKey('  Department of Health (DOH) '), 'department of health (doh)')
})

test('the checklist is ordered by the lower-cased name, then by id', () => {
  const sorted = sortAgenciesByName([
    { id: 3, name: 'Department of Tourism (DOT)' },
    { id: 1, name: 'Department of Science and Technology (DOST)' },
    { id: 2, name: 'Department of the Interior and Local Government (DILG)' },
    { id: 5, name: 'alpha agency' },
  ])
  assert.deepEqual(
    sorted.map((agency) => agency.id),
    [5, 1, 2, 3],
  )

  // Same name (different case) keeps a stable order by id.
  const twins = sortAgenciesByName([
    { id: 9, name: 'Zeta Agency' },
    { id: 4, name: 'zeta agency' },
  ])
  assert.deepEqual(
    twins.map((agency) => agency.id),
    [4, 9],
  )

  const original = [
    { id: 1, name: 'B' },
    { id: 2, name: 'A' },
  ]
  sortAgenciesByName(original)
  assert.deepEqual(
    original.map((agency) => agency.id),
    [1, 2],
  )
})

test('the selected ids are numbers, unique and never empty entries', () => {
  assert.deepEqual(normalizeSelectedIds(['3', 1, 3, null, undefined, 'x', 2]), [3, 1, 2])
  assert.deepEqual(normalizeSelectedIds(undefined), [])
  assert.deepEqual(normalizeSelectedIds([]), [])
})

test('the selection becomes the legacy cooperating-agencies text', () => {
  // Directory order, not the order the checkboxes were clicked in.
  assert.equal(
    describeAgencySelection(DIRECTORY, [4, 1]),
    'Department of Science and Technology (DOST), Local Government Unit (LGU)',
  )
  assert.equal(
    describeAgencySelection(DIRECTORY, ['2']),
    'Department of the Interior and Local Government (DILG)',
  )
  // Ids that are not in the directory contribute nothing.
  assert.equal(describeAgencySelection(DIRECTORY, [99]), '')
  assert.equal(describeAgencySelection(DIRECTORY, []), '')
  assert.equal(describeAgencySelection(null, [1]), '')
  assert.equal(joinAgencyNames(['DOST', '', null, 'DENR']), 'DOST, DENR')
})
