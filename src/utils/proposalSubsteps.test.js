/**
 * proposalSubsteps.test.js
 * ---------------------------------------------------------------------------
 * Covers the pages of step 2 (Proposal Details) and the rules behind the
 * Next / Previous navigation.
 *
 * The important guarantees are checked against the real form: the pages must be a
 * consecutive slice of the sections the form already renders (same order, no gap, no
 * overlap), and every field the form validates must belong to exactly one page - a
 * field without a page would never be shown and its error could not be reported.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import {
  PROPOSAL_SUBSTEPS,
  PROPOSAL_SUBSTEP_COUNT,
  clampSubstep,
  errorsOfSubstep,
  fieldsOfSubstep,
  sectionsOfSubstep,
  substepOfField,
  substepTitle,
  uncoveredFields,
} from './proposalSubsteps.js'

const FORM_FILE = new URL('../views/Proponent/ProponentSubmitProp.vue', import.meta.url)
const formSource = readFileSync(FORM_FILE, 'utf8')
const templateSource = formSource.slice(0, formSource.indexOf('<script setup>'))

/** The section refs the form renders, in template order. */
const renderedSections = () =>
  [...templateSource.matchAll(/ref="(section_[a-z_]+)"/g)].map((match) => match[1])

/** The form's own field -> section map (the one its scrolling and errors use). */
const sectionRefsOfForm = () => {
  const start = formSource.indexOf('const sectionRefs = {')
  const block = formSource.slice(start, formSource.indexOf('\n}', start))
  return Object.fromEntries(
    [...block.matchAll(/^\s*([a-z_]+):\s*'([^']+)'/gm)].map((match) => [match[1], match[2]]),
  )
}

const flatSections = () => PROPOSAL_SUBSTEPS.flatMap((substep) => substep.sections)

test('proposal details is split into consecutive pages of the existing sections', () => {
  assert.ok(PROPOSAL_SUBSTEP_COUNT > 1, 'step 2 must have more than one page')
  assert.equal(PROPOSAL_SUBSTEP_COUNT, PROPOSAL_SUBSTEPS.length)

  // Every page shows at least one section, and each section is on exactly one page.
  for (const substep of PROPOSAL_SUBSTEPS) {
    assert.ok(substep.id && substep.title, 'every page needs an id and a title')
    assert.ok(substep.sections.length > 0, `${substep.id} must contain a section`)
  }
  const flattened = flatSections()
  assert.equal(new Set(flattened).size, flattened.length, 'no section may appear twice')

  // The pages are the form's own sections, in the order the form renders them.
  assert.deepEqual(flatSections(), renderedSections())
})

test('a page owns the validation fields of its sections', () => {
  const sectionRefs = sectionRefsOfForm()

  // The project profile page owns the ten fields of section 01.
  assert.deepEqual(fieldsOfSubstep(sectionRefs, 1).sort(), [
    'address',
    'contact',
    'department',
    'duration',
    'end_date',
    'program_title',
    'project_leader',
    'project_leader_sex',
    'project_title',
    'start_date',
  ])

  // The second page owns the agencies, sites, research type and priority agenda.
  assert.deepEqual(fieldsOfSubstep(sectionRefs, 2).sort(), [
    'cooperating_agencies',
    'priority_agendas',
    'research_type',
    'sites',
  ])

  // The last page owns the references, personnel and budget fields.
  assert.deepEqual(fieldsOfSubstep(sectionRefs, PROPOSAL_SUBSTEP_COUNT).sort(), [
    'line_item_budget_file',
    'literature_cited',
    'other_projects_number',
    'personnel_requirements',
  ])
})

test('every field the form validates belongs to exactly one page', () => {
  const sectionRefs = sectionRefsOfForm()

  // A field without a page would never be shown - and its error could not be shown.
  assert.deepEqual(uncoveredFields(sectionRefs), [])
  assert.ok(Object.keys(sectionRefs).length > 30, 'the form still validates its full field set')

  // Each field maps to one page, and that page really contains its section.
  for (const [field, section] of Object.entries(sectionRefs)) {
    const page = substepOfField(sectionRefs, field)
    assert.ok(page >= 1 && page <= PROPOSAL_SUBSTEP_COUNT, `${field} must have a page`)
    assert.ok(sectionsOfSubstep(page).includes(section), `${field} must be on its section's page`)
  }
  // A field that is not part of the form has no page.
  assert.equal(substepOfField(sectionRefs, 'not_a_field'), null)
})

test('only the errors of the page on screen are reported', () => {
  const sectionRefs = sectionRefsOfForm()
  const errors = [
    { field: 'project_title', message: 'Project Title is required.' },
    { field: 'methodology', message: 'Methodology is required.' },
    { field: 'line_item_budget_file', message: 'Line-Item Budget file is required.' },
  ]

  assert.deepEqual(
    errorsOfSubstep(errors, sectionRefs, 1).map((entry) => entry.field),
    ['project_title'],
  )
  assert.deepEqual(
    errorsOfSubstep(errors, sectionRefs, 4).map((entry) => entry.field),
    ['methodology'],
  )
  assert.deepEqual(
    errorsOfSubstep(errors, sectionRefs, PROPOSAL_SUBSTEP_COUNT).map((entry) => entry.field),
    ['line_item_budget_file'],
  )
  // A page without errors (and an unknown page) reports nothing.
  assert.deepEqual(errorsOfSubstep(errors, sectionRefs, 2), [])
  assert.deepEqual(errorsOfSubstep(errors, sectionRefs, 99), [])
  assert.deepEqual(errorsOfSubstep(null, sectionRefs, 1), [])
})

test('a page number is clamped to the pages that exist', () => {
  assert.equal(clampSubstep(1), 1)
  assert.equal(clampSubstep(PROPOSAL_SUBSTEP_COUNT), PROPOSAL_SUBSTEP_COUNT)
  assert.equal(clampSubstep(0), 1)
  assert.equal(clampSubstep(-3), 1)
  assert.equal(clampSubstep(PROPOSAL_SUBSTEP_COUNT + 5), PROPOSAL_SUBSTEP_COUNT)
  assert.equal(clampSubstep(undefined), 1)
  assert.equal(clampSubstep('2'), 2)
  assert.equal(clampSubstep(2.7), 2)
})

test('the form keeps the main four-step workflow and its panels', () => {
  // The substeps only exist inside step 2: the header badge, the four-step bar and
  // the hand-over to step 3 are still the ones the workflow already had.
  assert.ok(templateSource.includes('<span class="header-badge">Step 2 of 4</span>'))
  assert.equal((templateSource.match(/class="progress-step/g) || []).length, 4)
  assert.ok(formSource.includes("emit('next', { ...proposal })"))
  assert.ok(formSource.includes('@click="goToNextSubstepOrContinue"'))
  assert.ok(formSource.includes("{{ isLastSubstep ? 'Continue to Certification & Endorsement' : 'Next' }}"))

  // The pages are shown/hidden, not re-created: every section of the form is still
  // rendered exactly once, inside one panel.
  assert.equal(
    (templateSource.match(/class="substep-panel"/g) || []).length,
    PROPOSAL_SUBSTEP_COUNT,
  )
  for (let page = 1; page <= PROPOSAL_SUBSTEP_COUNT; page += 1) {
    assert.ok(
      templateSource.includes(`v-show="currentSubstep === ${page}" class="substep-panel"`),
      `page ${page} must have its panel`,
    )
  }
})

test('every page number resolves to a page', () => {
  assert.equal(substepTitle(1), 'Project Profile')
  assert.equal(substepTitle(PROPOSAL_SUBSTEP_COUNT), 'References, Team & Budget')
  assert.equal(substepTitle(0), '')
  assert.equal(substepTitle(PROPOSAL_SUBSTEP_COUNT + 1), '')
  assert.deepEqual(sectionsOfSubstep(1), ['section_project_profile'])
  assert.deepEqual(sectionsOfSubstep(0), [])
})
