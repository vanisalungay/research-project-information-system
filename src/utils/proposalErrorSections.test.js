/**
 * proposalErrorSections.test.js
 * ---------------------------------------------------------------------------
 * The grouping that lets the Create Proposal validation banner name every
 * incomplete section and navigate to it.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  SECTION_NAVIGATION,
  groupErrorsBySection,
  sectionNavigation,
} from './proposalErrorSections.js'
import { PROPOSAL_SECTIONS } from './proposalSections.js'
import { PROPOSAL_SUBSTEPS } from './proposalSubsteps.js'

const SECTION_REFS = {
  project_title: 'section_project_profile',
  project_leader: 'section_project_profile',
  address: 'section_project_profile',
  cooperating_agencies: 'section_cooperating_agencies',
  sites: 'section_sites',
  research_type: 'section_research_type',
  methodology: 'section_methodology',
  line_item_budget_file: 'section_budget',
}

test('every proposal section has a number, title and page', () => {
  const refs = PROPOSAL_SUBSTEPS.flatMap((substep) => substep.sections)
  assert.equal(refs.length, PROPOSAL_SECTIONS.length)
  refs.forEach((ref, index) => {
    const navigation = sectionNavigation(ref)
    assert.equal(navigation.number, PROPOSAL_SECTIONS[index].key)
    assert.equal(navigation.title, PROPOSAL_SECTIONS[index].label)
    assert.ok(navigation.substep >= 1)
  })
  assert.equal(Object.keys(SECTION_NAVIGATION).length, PROPOSAL_SECTIONS.length)
})

test('errors are grouped by section in validation order', () => {
  const groups = groupErrorsBySection(
    [
      { field: 'project_title', message: 'Project Title is required.' },
      { field: 'address', message: 'Please select a region.' },
      { field: 'methodology', message: 'Methodology is required.' },
      { field: 'project_leader', message: 'Project Leader is required.' },
    ],
    SECTION_REFS,
  )

  assert.equal(groups.length, 2)
  assert.equal(groups[0].number, '01')
  assert.equal(groups[0].title, 'Project Profile')
  assert.deepEqual(
    groups[0].errors.map((entry) => entry.field),
    ['project_title', 'address', 'project_leader'],
  )
  assert.equal(groups[1].title, 'Methodology')
  assert.equal(groups[1].substep, 4)
})

test('each section keeps its own page and can be navigated independently', () => {
  const groups = groupErrorsBySection(
    [
      { field: 'sites', message: 'Site 1: select a region.' },
      { field: 'methodology', message: 'Methodology is required.' },
      { field: 'line_item_budget_file', message: 'Budget is required.' },
    ],
    SECTION_REFS,
  )
  assert.deepEqual(
    groups.map((group) => [group.number, group.substep]),
    [
      ['03', 2],
      ['12', 4],
      ['25', 6],
    ],
  )
})

test('a field without a section is kept in a visible fallback group', () => {
  const groups = groupErrorsBySection(
    [{ field: 'unknown_field', message: 'Something is required.' }],
    SECTION_REFS,
  )
  assert.equal(groups.length, 1)
  assert.equal(groups[0].title, 'Other required information')
  assert.equal(groups[0].errors[0].field, 'unknown_field')
})

test('an empty error list produces no groups', () => {
  assert.deepEqual(groupErrorsBySection([], SECTION_REFS), [])
  assert.deepEqual(groupErrorsBySection(null, SECTION_REFS), [])
})
