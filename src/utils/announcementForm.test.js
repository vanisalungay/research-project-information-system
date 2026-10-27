/**
 * announcementForm.test.js
 * Verifies the validation and payload rules of the ORPS Call for Proposal modal: creating a
 * call still requires an announcement name, updating an existing call never does (the name
 * and the other restricted fields are not editable and are never sent), and the submission
 * window keeps its existing rules.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ANNOUNCEMENT_UPDATE_FIELDS,
  announcementCreatePayload,
  announcementFormError,
  announcementUpdatePayload,
} from './announcementForm.js'

/**
 * The agenda keys of the active framework. They are master data now (the Priority
 * Agenda admin page), so the caller passes them in instead of the module hard-coding
 * the five programs.
 */
const AGENDA_KEYS = ['dagat', 'punla', 'kalikasan', 'negosyo', 'tanglaw']

/** The state of the modal when an existing call is being updated. */
const updateForm = {
  name: 'FY 2026 - 1st Call for Proposals',
  startDate: '2026-10-01',
  endDate: '2026-10-31',
  status: 'ACTIVE',
  priorityAgendas: ['dagat'],
  reminderIds: ['research-plan'],
  additionalAnnouncement: 'Original body.',
}

test('a new call still requires an announcement name', () => {
  const form = { ...updateForm, name: '' }

  assert.equal(announcementFormError(form, { isEditing: false }), 'Announcement name is required.')
  assert.equal(
    announcementFormError({ ...form, name: '   ' }, { isEditing: false }),
    'Announcement name is required.',
  )
  assert.equal(
    announcementFormError({ ...form, name: undefined }, { isEditing: false }),
    'Announcement name is required.',
  )
  assert.equal(announcementFormError(updateForm, { isEditing: false }), '')
})

test('updating an existing call never requires the announcement name', () => {
  for (const name of ['', '   ', undefined, null, 0]) {
    assert.equal(
      announcementFormError({ ...updateForm, name }, { isEditing: true }),
      '',
      `an update must not ask for the name (${String(name)})`,
    )
  }
  // The default (no options) is the create rule.
  assert.equal(announcementFormError({ ...updateForm, name: '' }), 'Announcement name is required.')
})

test('the submission window rules are the same for both flows', () => {
  assert.equal(
    announcementFormError({ ...updateForm, startDate: '' }, { isEditing: true }),
    'Start date and end date are required.',
  )
  assert.equal(
    announcementFormError({ ...updateForm, endDate: undefined }, { isEditing: true }),
    'Start date and end date are required.',
  )
  assert.equal(
    announcementFormError(
      { ...updateForm, startDate: '2026-10-31', endDate: '2026-10-01' },
      { isEditing: true },
    ),
    'End date cannot be earlier than the start date.',
  )
  assert.equal(announcementFormError(updateForm, { isEditing: true }), '')
})

test('an update payload carries the submission window and the status only', () => {
  const payload = announcementUpdatePayload(updateForm)

  assert.deepEqual(payload, {
    startDate: '2026-10-01',
    endDate: '2026-10-31',
    status: 'ACTIVE',
  })
  assert.deepEqual(Object.keys(payload).sort(), [...ANNOUNCEMENT_UPDATE_FIELDS].sort())
})

test('an update payload can never replace a restricted field', () => {
  const payload = announcementUpdatePayload({
    ...updateForm,
    name: '',
    priorityAgendas: [],
    reminderIds: [],
    additionalAnnouncement: '',
    updateExplanation: 'Should never be sent.',
  })

  for (const field of [
    'name',
    'priorityAgendas',
    'reminderIds',
    'additionalAnnouncement',
    'updateExplanation',
  ]) {
    assert.equal(Object.prototype.hasOwnProperty.call(payload, field), false, field)
  }
})

test('a create payload carries the complete announcement', () => {
  const payload = announcementCreatePayload(updateForm, AGENDA_KEYS)

  assert.deepEqual(payload, {
    name: 'FY 2026 - 1st Call for Proposals',
    startDate: '2026-10-01',
    endDate: '2026-10-31',
    status: 'ACTIVE',
    priorityAgendas: ['dagat'],
    reminderIds: ['research-plan'],
    additionalAnnouncement: 'Original body.',
  })
})

test('a create payload trims the name and normalises the selections', () => {
  const payload = announcementCreatePayload(
    {
      name: '  Trimmed Call  ',
      startDate: '2026-10-01',
      endDate: '2026-10-31',
      status: 'ACTIVE',
      priorityAgendas: ['DAGAT', 'dagat', 'not-a-program'],
      reminderIds: ['COLLABORATION', 'bogus-reminder'],
      additionalAnnouncement: '  Body.  ',
    },
    AGENDA_KEYS,
  )

  assert.equal(payload.name, 'Trimmed Call')
  assert.deepEqual(payload.priorityAgendas, ['dagat'])
  assert.deepEqual(payload.reminderIds, ['collaboration'])
  assert.equal(payload.additionalAnnouncement, 'Body.')
})

test('a create payload keeps only the agendas of the active framework', () => {
  // An agenda the administrator has not configured (or has deactivated) is dropped,
  // never silently persisted on a new call.
  const payload = announcementCreatePayload(
    { ...updateForm, priorityAgendas: ['dagat', 'blue-economy'] },
    ['blue-economy'],
  )

  assert.deepEqual(payload.priorityAgendas, ['blue-economy'])

  // Without the master data loaded yet, nothing is accepted rather than everything.
  assert.deepEqual(announcementCreatePayload(updateForm).priorityAgendas, [])
})

test('a missing form state is handled without throwing', () => {
  assert.equal(
    announcementFormError(null, { isEditing: true }),
    'Start date and end date are required.',
  )
  assert.equal(
    announcementFormError(undefined, { isEditing: false }),
    'Announcement name is required.',
  )
  assert.deepEqual(announcementUpdatePayload(null), {
    startDate: undefined,
    endDate: undefined,
    status: undefined,
  })
})
