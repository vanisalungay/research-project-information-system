import test from 'node:test'
import assert from 'node:assert/strict'

import {
  DEADLINE_OPTIONS,
  DEFAULT_PROPOSAL_KEYS,
  PREFERENCE_KEYS,
  PROPOSAL_OPTIONS,
  initialDeadlineSelection,
  initialProposalSelection,
  normalizeKeys,
  preferenceLabel,
} from './notificationPreferences.js'

test('legacy labels are normalized to stable keys', () => {
  assert.deepEqual(
    normalizeKeys([
      'New Proposal Assigned',
      'Proposal Status Updated',
      'Revision Deadline Reminders',
    ]),
    [
      PREFERENCE_KEYS.NEW_ASSIGNED,
      PREFERENCE_KEYS.STATUS_UPDATED,
      PREFERENCE_KEYS.DEADLINE_REVISION,
    ],
  )
})

test('normalization trims, drops blanks and de-duplicates without losing unknowns', () => {
  assert.deepEqual(
    normalizeKeys(['  proposal.status_updated ', '', null, 'proposal.status_updated', 'custom.key']),
    ['proposal.status_updated', 'custom.key'],
  )
})

test('an unsaved account sees the optional proposal defaults on and no deadline reminders', () => {
  const user = { notifyPreferencesConfigured: false, notifyProposals: [], notifyDeadlines: [] }

  assert.deepEqual(initialProposalSelection(user), [...DEFAULT_PROPOSAL_KEYS])
  assert.deepEqual(initialDeadlineSelection(user), [])
})

test('an explicitly saved all-off account sees no optional selections', () => {
  const user = { notifyPreferencesConfigured: true, notifyProposals: [], notifyDeadlines: [] }

  assert.deepEqual(initialProposalSelection(user), [])
  assert.deepEqual(initialDeadlineSelection(user), [])
})

test('a saved selection is shown as its stable keys', () => {
  const user = {
    notifyPreferencesConfigured: true,
    notifyProposals: ['Proposal Status Updated'],
    notifyDeadlines: ['Revision Deadline Reminders'],
  }

  assert.deepEqual(initialProposalSelection(user), [PREFERENCE_KEYS.STATUS_UPDATED])
  assert.deepEqual(initialDeadlineSelection(user), [PREFERENCE_KEYS.DEADLINE_REVISION])
})

test('each option has a stable key and a display label', () => {
  assert.equal(PROPOSAL_OPTIONS.length, 4)
  assert.equal(DEADLINE_OPTIONS.length, 2)
  for (const option of [...PROPOSAL_OPTIONS, ...DEADLINE_OPTIONS]) {
    assert.ok(option.key.includes('.'))
    assert.ok(option.label)
    assert.ok(option.description)
  }
  assert.equal(preferenceLabel(PREFERENCE_KEYS.STATUS_UPDATED), 'Proposal Status Updated')
})

test('evaluation deadline reminders are marked unavailable', () => {
  const evaluation = DEADLINE_OPTIONS.find(
    (option) => option.key === PREFERENCE_KEYS.DEADLINE_EVALUATION,
  )
  assert.equal(evaluation.available, false)
})
