/**
 * announcementOptions.test.js
 * Verifies the RIIDE priority-agenda programs, the standard announcement
 * reminders and the selection helpers behind the Create/Edit Proposal
 * Announcement form: stable identifiers (never labels or positions), the
 * all-three reminder default, and the Check All / Uncheck All behaviour.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ANNOUNCEMENT_REMINDERS,
  REMINDER_KEYS,
  defaultReminderKeys,
  normalizeProgramKeys,
  normalizeReminderKeys,
  isAllSelected,
  toggleAllSelection,
} from './announcementOptions.js'

/**
 * The agenda keys of the currently active framework. They are master data now (the
 * Priority Agenda admin page), so every helper takes them as an argument - the module
 * no longer hard-codes the five programs.
 */
const AGENDA_KEYS = ['dagat', 'punla', 'kalikasan', 'negosyo', 'tanglaw']

test('the agenda list is no longer hard-coded in this module', () => {
  // Without the master data the helpers accept nothing, instead of silently falling
  // back to a built-in list.
  assert.deepEqual(normalizeProgramKeys(['dagat', 'punla']), [])
  assert.deepEqual(normalizeProgramKeys(undefined), [])
  assert.deepEqual(normalizeProgramKeys(null), [])
  assert.equal(isAllSelected(['dagat'], []), false)
  assert.deepEqual(toggleAllSelection([], []), [])
})

test('the three standard reminders keep their exact text and stable identifiers', () => {
  assert.deepEqual(REMINDER_KEYS, ['research-plan', 'eligible-submitters', 'collaboration'])
  assert.deepEqual(
    ANNOUNCEMENT_REMINDERS.map((reminder) => reminder.text),
    [
      'Proposals must include a clear research plan, objectives, expected outcomes, and a detailed budget.',
      'All full-time faculty, researchers, and staff of MSU at Naawan are eligible to submit proposals.',
      'Collaborative and interdisciplinary projects are highly encouraged.',
    ]
  )
})

test('a new announcement selects all three reminders and no agenda', () => {
  assert.deepEqual(defaultReminderKeys(), REMINDER_KEYS)
  assert.deepEqual(normalizeProgramKeys(undefined, AGENDA_KEYS), [])
  assert.deepEqual(normalizeProgramKeys(null, AGENDA_KEYS), [])
})

test('selecting a single program keeps exactly its identifier', () => {
  assert.deepEqual(normalizeProgramKeys(['dagat'], AGENDA_KEYS), ['dagat'])
})

test('selecting multiple programs keeps every identifier', () => {
  assert.deepEqual(normalizeProgramKeys(['dagat', 'kalikasan', 'tanglaw'], AGENDA_KEYS), [
    'dagat',
    'kalikasan',
    'tanglaw',
  ])
})

test('a program outside the active framework is dropped', () => {
  // An agenda an administrator has not configured (or has deactivated) can never be
  // selected on a call, whatever the client sends.
  assert.deepEqual(normalizeProgramKeys(['dagat', 'blue-economy'], AGENDA_KEYS), ['dagat'])
  assert.deepEqual(normalizeProgramKeys(['blue-economy'], ['blue-economy']), ['blue-economy'])
})

test('check all selects every active program and uncheck all clears them', () => {
  const all = toggleAllSelection([], AGENDA_KEYS)
  assert.deepEqual(all, AGENDA_KEYS)

  const cleared = toggleAllSelection(all, AGENDA_KEYS)
  assert.deepEqual(cleared, [])
})

test('the check-all label follows individual selections', () => {
  // Label is derived from isAllSelected: false -> "Check All", true -> "Uncheck All".
  assert.equal(isAllSelected([], AGENDA_KEYS), false)
  assert.equal(isAllSelected(['dagat'], AGENDA_KEYS), false)
  assert.equal(isAllSelected(['dagat', 'punla', 'kalikasan', 'negosyo'], AGENDA_KEYS), false)
  assert.equal(isAllSelected(AGENDA_KEYS, AGENDA_KEYS), true)

  // Partially selected + "Check All" -> everything selected -> label flips to Uncheck All.
  const afterCheckAll = toggleAllSelection(['dagat', 'punla'], AGENDA_KEYS)
  assert.equal(isAllSelected(afterCheckAll, AGENDA_KEYS), true)
})

test('reminders can be deselected independently', () => {
  assert.deepEqual(normalizeReminderKeys(['research-plan', 'collaboration']), [
    'research-plan',
    'collaboration',
  ])
  assert.deepEqual(normalizeReminderKeys([]), [])
})

test('stored selections are normalized (case, duplicates, unknown ids, legacy strings)', () => {
  assert.deepEqual(normalizeProgramKeys(['DAGAT', 'dagat', 'not-a-program'], AGENDA_KEYS), ['dagat'])
  assert.deepEqual(normalizeProgramKeys('dagat; punla', AGENDA_KEYS), ['dagat', 'punla'])
  assert.deepEqual(normalizeProgramKeys('dagat,punla', AGENDA_KEYS), ['dagat', 'punla'])
  assert.deepEqual(normalizeReminderKeys(['RESEARCH-PLAN', 'nope']), ['research-plan'])
  assert.deepEqual(normalizeReminderKeys('collaboration\nresearch-plan'), [
    'research-plan',
    'collaboration',
  ])
})
