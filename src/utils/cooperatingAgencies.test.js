/**
 * cooperatingAgencies.test.js
 * ---------------------------------------------------------------------------
 * Covers the client-side rules of the "Cooperating Agencies" checklist
 * (section 02 of the Create Proposal form): the name of a new agency is
 * normalized before it is sent, an empty / too long / already existing name is
 * rejected (case-insensitively), a newly added agency is merged into the
 * checklist in its alphabetical place, and the selection is turned into the
 * legacy text the existing screens read.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  AGENCY_NAME_MAX_LENGTH,
  agencyNameKey,
  describeAgencySelection,
  findAgencyByName,
  joinAgencyNames,
  mergeAgency,
  normalizeAgencyName,
  normalizeSelectedIds,
  sortAgenciesByName,
  validateAgencyName,
} from './cooperatingAgencies.js'

const DIRECTORY = [
  { id: 1, name: 'Department of Science and Technology (DOST)' },
  { id: 2, name: 'Department of the Interior and Local Government (DILG)' },
  { id: 3, name: 'Department of Tourism (DOT)' },
  { id: 4, name: 'Local Government Unit (LGU)' },
]

test('an agency name is trimmed and its inner whitespace collapsed', () => {
  assert.equal(normalizeAgencyName('  Bureau   of Fisheries  '), 'Bureau of Fisheries')
  assert.equal(normalizeAgencyName('\tPhilippine\tSpace\nAgency '), 'Philippine Space Agency')
  assert.equal(normalizeAgencyName(null), '')
  assert.equal(normalizeAgencyName(undefined), '')
  assert.equal(agencyNameKey('  Department of Health (DOH) '), 'department of health (doh)')
})

test('an existing agency is found regardless of case and spacing', () => {
  assert.equal(findAgencyByName(DIRECTORY, 'department of science and technology (dost)')?.id, 1)
  assert.equal(findAgencyByName(DIRECTORY, '  DEPARTMENT   OF TOURISM (DOT) ')?.id, 3)
  assert.equal(findAgencyByName(DIRECTORY, 'Department of Energy (DOE)'), null)
  assert.equal(findAgencyByName(DIRECTORY, '   '), null)
  assert.equal(findAgencyByName(null, 'DOST'), null)
})

test('a blank name is rejected before anything is sent', () => {
  assert.match(validateAgencyName('', DIRECTORY), /enter the name/i)
  assert.match(validateAgencyName('   ', DIRECTORY), /enter the name/i)
})

test('a name that already exists is rejected case-insensitively', () => {
  assert.match(
    validateAgencyName('department of science and technology (dost)', DIRECTORY),
    /already in the cooperating agencies list/i,
  )
  assert.match(
    validateAgencyName('  LOCAL   GOVERNMENT UNIT (LGU) ', DIRECTORY),
    /already in the cooperating agencies list/i,
  )
})

test('a too long name is rejected, a new one is accepted', () => {
  assert.match(
    validateAgencyName('x'.repeat(AGENCY_NAME_MAX_LENGTH + 1), DIRECTORY),
    /at most 200 characters/i,
  )
  assert.equal(
    validateAgencyName('Bureau of Fisheries and Aquatic Resources (BFAR)', DIRECTORY),
    '',
  )
  assert.equal(validateAgencyName('  Bureau  of Fisheries ', []), '')
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

test('a newly added agency is merged into the checklist in its place', () => {
  const merged = mergeAgency(DIRECTORY, {
    id: 5,
    name: 'Bureau of Fisheries and Aquatic Resources (BFAR)',
  })
  assert.deepEqual(
    merged.map((agency) => agency.id),
    [5, 1, 2, 3, 4],
  )
  // Nothing is duplicated when the same agency comes back from the backend.
  assert.deepEqual(mergeAgency(DIRECTORY, { id: 1, name: 'DOST' }).length, DIRECTORY.length)
  // An unsaved agency (no id) never enters the checklist.
  assert.deepEqual(mergeAgency(DIRECTORY, { name: 'No id' }).length, DIRECTORY.length)
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
