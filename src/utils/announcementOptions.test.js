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
  RIIDE_PROGRAMS,
  ANNOUNCEMENT_REMINDERS,
  PROGRAM_KEYS,
  REMINDER_KEYS,
  defaultReminderKeys,
  normalizeProgramKeys,
  normalizeReminderKeys,
  isAllSelected,
  toggleAllSelection,
} from './announcementOptions.js'

test('the five RIIDE programs use stable identifiers and the official text', () => {
  assert.deepEqual(PROGRAM_KEYS, ['dagat', 'punla', 'kalikasan', 'negosyo', 'tanglaw'])
  assert.deepEqual(
    RIIDE_PROGRAMS.map((program) => [program.label, program.description]),
    [
      ['DAGAT Program', 'Development of Aquatic Governance and Advanced Technologies'],
      ['PUNLA Program', 'Progressive Upliftment for Nutrition, Livelihood, and Agriculture'],
      [
        'KALIKASAN Program',
        'Knowledge and Actions for Lasting Impact: Keeping Atmospheres, Seas, and Nature Sustainable',
      ],
      [
        'NEGOSYO Program',
        'Nurturing Entrepreneurship and Growth through Outstanding Strategic Yielding Opportunities',
      ],
      [
        'TANGLAW Program',
        'Transforming Academia through Nurturing Growth, Leadership, Advancement, and Wisdom',
      ],
    ]
  )
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
  assert.deepEqual(normalizeProgramKeys(undefined), [])
  assert.deepEqual(normalizeProgramKeys(null), [])
})

test('selecting a single program keeps exactly its identifier', () => {
  assert.deepEqual(normalizeProgramKeys(['dagat']), ['dagat'])
})

test('selecting multiple programs keeps every identifier', () => {
  assert.deepEqual(normalizeProgramKeys(['dagat', 'kalikasan', 'tanglaw']), [
    'dagat',
    'kalikasan',
    'tanglaw',
  ])
})

test('check all selects all five programs and uncheck all clears them', () => {
  const all = toggleAllSelection([], PROGRAM_KEYS)
  assert.deepEqual(all, PROGRAM_KEYS)

  const cleared = toggleAllSelection(all, PROGRAM_KEYS)
  assert.deepEqual(cleared, [])
})

test('the check-all label follows individual selections', () => {
  // Label is derived from isAllSelected: false -> "Check All", true -> "Uncheck All".
  assert.equal(isAllSelected([], PROGRAM_KEYS), false)
  assert.equal(isAllSelected(['dagat'], PROGRAM_KEYS), false)
  assert.equal(isAllSelected(['dagat', 'punla', 'kalikasan', 'negosyo'], PROGRAM_KEYS), false)
  assert.equal(isAllSelected(PROGRAM_KEYS, PROGRAM_KEYS), true)

  // Partially selected + "Check All" -> everything selected -> label flips to Uncheck All.
  const afterCheckAll = toggleAllSelection(['dagat', 'punla'], PROGRAM_KEYS)
  assert.equal(isAllSelected(afterCheckAll, PROGRAM_KEYS), true)
})

test('reminders can be deselected independently', () => {
  assert.deepEqual(normalizeReminderKeys(['research-plan', 'collaboration']), [
    'research-plan',
    'collaboration',
  ])
  assert.deepEqual(normalizeReminderKeys([]), [])
})

test('stored selections are normalized (case, duplicates, unknown ids, legacy strings)', () => {
  assert.deepEqual(normalizeProgramKeys(['DAGAT', 'dagat', 'not-a-program']), ['dagat'])
  assert.deepEqual(normalizeProgramKeys('dagat; punla'), ['dagat', 'punla'])
  assert.deepEqual(normalizeProgramKeys('dagat,punla'), ['dagat', 'punla'])
  assert.deepEqual(normalizeReminderKeys(['RESEARCH-PLAN', 'nope']), ['research-plan'])
  assert.deepEqual(normalizeReminderKeys('collaboration\nresearch-plan'), [
    'research-plan',
    'collaboration',
  ])
})
