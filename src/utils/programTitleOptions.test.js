/**
 * programTitleOptions.test.js
 * ---------------------------------------------------------------------------
 * Covers the choices of the (now optional) Program Title field and the click rule
 * behind the double-click shortcut.
 *
 * The field must be usable while empty, the placeholder must read exactly as agreed,
 * the "create a new program title" action must be recognisable (it is the one flagged
 * `create`, which the control paints blue), and a double click must never fall through
 * to a normal selection.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  CREATE_NEW_PROGRAM_TITLE,
  CREATE_NEW_PROGRAM_TITLE_LABEL,
  MAX_PROGRAM_TITLE_LENGTH,
  PROGRAM_TITLE_PLACEHOLDER,
  isCreatingProgramTitle,
  programTitleClickAction,
  programTitleLabel,
  programTitleOptions,
  programTitlesFromProposals,
} from './programTitleOptions.js'

const TITLES = ['Aquaculture Program', 'Fisheries Program']

test('the placeholder is the exact agreed text and comes first', () => {
  const options = programTitleOptions({ titles: TITLES, selected: '' })
  assert.equal(options[0].value, '')
  assert.equal(options[0].label, 'Select a program title or create a new program title')
  assert.equal(PROGRAM_TITLE_PLACEHOLDER, 'Select a program title or create a new program title')
  // Nothing is selected: the field is optional and simply shows the placeholder.
  assert.equal(options[0].selected, true)
  assert.equal(programTitleLabel(options), PROGRAM_TITLE_PLACEHOLDER)
})

test('the create action sits second, keeps its label and is flagged for the blue', () => {
  const options = programTitleOptions({ titles: TITLES, selected: '' })
  assert.equal(options[1].value, CREATE_NEW_PROGRAM_TITLE)
  assert.equal(options[1].label, CREATE_NEW_PROGRAM_TITLE_LABEL)
  assert.equal(options[1].label, 'Create New Program Title')
  assert.equal(options[1].create, true)
  // Only the create action carries the flag: it is the one the control paints blue.
  assert.equal(options.filter((option) => option.create).length, 1)
})

test('the reusable titles follow, in the order they were given', () => {
  const options = programTitleOptions({ titles: TITLES, selected: 'Fisheries Program' })
  assert.deepEqual(
    options.map((option) => option.label),
    [PROGRAM_TITLE_PLACEHOLDER, CREATE_NEW_PROGRAM_TITLE_LABEL, ...TITLES],
  )
  assert.equal(options[2].selected, false)
  assert.equal(options[3].selected, true)
  assert.equal(programTitleLabel(options), 'Fisheries Program')
})

test('the field shows what is selected, and the placeholder when empty', () => {
  // Empty (optional, untouched): the placeholder.
  assert.equal(programTitleLabel(programTitleOptions({ titles: TITLES })), PROGRAM_TITLE_PLACEHOLDER)
  // A title from the list.
  assert.equal(
    programTitleLabel(programTitleOptions({ titles: TITLES, selected: TITLES[0] })),
    TITLES[0],
  )
  // Creating a new one: the create label (the value itself is typed below the field).
  assert.equal(
    programTitleLabel(programTitleOptions({ titles: TITLES, selected: CREATE_NEW_PROGRAM_TITLE })),
    CREATE_NEW_PROGRAM_TITLE_LABEL,
  )
  // A saved title that is not in the loaded list yet still shows (draft/revision).
  assert.equal(
    programTitleLabel(programTitleOptions({ titles: [], selected: 'Old Program' })),
    PROGRAM_TITLE_PLACEHOLDER,
  )
})

test('the sentinel can never collide with a real title', () => {
  const options = programTitleOptions({ titles: ['Create New Program Title'], selected: '' })
  assert.equal(options[1].value, CREATE_NEW_PROGRAM_TITLE)
  assert.equal(options[2].value, 'Create New Program Title')
  assert.notEqual(options[1].value, options[2].value)
  assert.equal(isCreatingProgramTitle(CREATE_NEW_PROGRAM_TITLE), true)
  assert.equal(isCreatingProgramTitle(''), false)
  assert.equal(isCreatingProgramTitle('Fisheries Program'), false)
})

test('a missing or odd input is handled without failing', () => {
  assert.deepEqual(programTitleOptions().slice(0, 2).map((option) => option.value), ['', CREATE_NEW_PROGRAM_TITLE])
  assert.equal(programTitleOptions({ titles: null }).length, 2)
  assert.equal(programTitleLabel(null), PROGRAM_TITLE_PLACEHOLDER)
  assert.equal(programTitleLabel(undefined), PROGRAM_TITLE_PLACEHOLDER)
})

test('one click opens or closes the list, a double click asks to create', () => {
  assert.equal(programTitleClickAction(1, false), 'open')
  assert.equal(programTitleClickAction(1, true), 'close')
  // The second click of a pair never selects a title: it opens the creation flow.
  assert.equal(programTitleClickAction(2, false), 'create')
  assert.equal(programTitleClickAction(2, true), 'create')
  assert.equal(programTitleClickAction(3, true), 'create')
})

test('the reusable titles are read from the proposals, trimmed, deduplicated and sorted', () => {
  assert.deepEqual(
    programTitlesFromProposals([
      { programTitle: 'Fisheries Program' },
      { programTitle: '  Aquaculture Program  ' },
      { programTitle: 'Fisheries Program' },
      { programTitle: '' },
      { programTitle: '   ' },
      { programTitle: null },
      {},
      null,
    ]),
    ['Aquaculture Program', 'Fisheries Program'],
  )

  // The same rule serves the Create Proposal form and the Request for Endorsement form,
  // so an odd or missing response can never throw.
  assert.deepEqual(programTitlesFromProposals([]), [])
  assert.deepEqual(programTitlesFromProposals(null), [])
  assert.deepEqual(programTitlesFromProposals(), [])
})

test('the program title width matches what a proposal can store', () => {
  // proposals.program_title is VARCHAR(255): a title created on the endorsement form has
  // to fit it, because it is carried over to the proposal unchanged.
  assert.equal(MAX_PROGRAM_TITLE_LENGTH, 255)
})
