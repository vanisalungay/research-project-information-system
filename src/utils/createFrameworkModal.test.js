/**
 * createFrameworkModal.test.js
 * ---------------------------------------------------------------------------
 * The redesigned Create Framework dialog on Priority Agendas. It is a compact
 * centred dialog with an icon header, a full-width name field, a two-column year
 * row, a live "New Proposal" preview, the active-framework switch and a
 * two-button footer. The creation API, year validation and active flag are the
 * existing ones.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Admin/PriorityAgendas.vue')

test('the framework dialog has the reference header and compact shell', () => {
  assert.ok(PAGE.includes('Create framework'), 'the reference title')
  assert.ok(
    PAGE.includes('Set the name and years that appear next to "Priority Agenda" on new proposals.'),
    'the reference subtitle',
  )
  assert.ok(PAGE.includes('class="fw-header-icon"'), 'the icon tile')
  assert.ok(PAGE.includes('aria-labelledby="framework-modal-title"'), 'the dialog is labelled')
  assert.ok(PAGE.includes('aria-label="Close create framework"'), 'the close is labelled')
  assert.match(PAGE, /\.fw-modal \{[\s\S]*?width: 352px/, 'compact width')
  assert.match(PAGE, /\.fw-modal \{[\s\S]*?max-height: 90vh/, 'bounded height')
  assert.match(PAGE, /\.fw-body \{[\s\S]*?overflow-y: auto/, 'the body scrolls')
  assert.ok(PAGE.includes('@keydown.esc="closeModals"'), 'Escape closes it')
})

test('the fields and their guidance match the reference', () => {
  assert.ok(PAGE.includes('Framework / program name'), 'the name label')
  assert.ok(PAGE.includes('placeholder="e.g., MSUN RIIE"'), 'the name placeholder')
  assert.ok(PAGE.includes('Start year') && PAGE.includes('End year'), 'the two year labels')
  assert.ok(PAGE.includes('Enter a four-digit start and end year.'), 'the year helper')
  assert.ok(PAGE.includes('v-model="frameworkForm.frameworkName"'))
  assert.ok(PAGE.includes('v-model.number="frameworkForm.coverageStartYear"'))
  assert.ok(PAGE.includes('v-model.number="frameworkForm.coverageEndYear"'))
  assert.match(PAGE, /class="fw-row"/, 'the years are side by side')
})

test('the preview is live and never hardcoded', () => {
  assert.ok(PAGE.includes('{{ frameworkPreview }}'), 'the badge is bound')
  assert.ok(PAGE.includes('const frameworkPreview = computed'), 'and computed')
  assert.ok(PAGE.includes('Your framework name'), 'a placeholder when the name is blank')
  assert.ok(PAGE.includes("(frameworkForm.value.frameworkName || '').trim()"),
    'the name comes from the form')
  assert.ok(PAGE.includes('Priority Agenda'), 'the preview row label')
  assert.ok(!PAGE.includes('MSUN RIIE (2026'), 'no sample preview is hardcoded')
  assert.ok(PAGE.includes('Number(end) >= Number(start)'),
    'an impossible range is never previewed as valid')
})

test('the active framework switch keeps the existing flag', () => {
  assert.ok(PAGE.includes('Make this the active framework'), 'the card title')
  assert.ok(
    PAGE.includes('New submissions will use it, and the current active framework will be deactivated.'),
    'the card description',
  )
  assert.ok(PAGE.includes('v-model="frameworkForm.active"'), 'the existing flag is bound')
  assert.ok(PAGE.includes('class="fw-switch"'), 'presented as a switch')
  assert.ok(PAGE.includes('type="checkbox"'), 'on a real checkbox')
  assert.ok(PAGE.includes('active: Boolean(frameworkForm.value.active)'), 'and still sent')
})

test('validation, the creation API and the footer are preserved', () => {
  assert.ok(PAGE.includes('const canSaveFramework = computed'), 'the button state is computed')
  assert.ok(PAGE.includes(':disabled="saving || !canSaveFramework"'), 'and disables the action')
  assert.ok(PAGE.includes('const yearRangeError = computed'), 'the range is checked')
  assert.ok(PAGE.includes('Enter a four-digit start year.'))
  assert.ok(PAGE.includes('Enter a four-digit end year.'))
  assert.ok(PAGE.includes('End year must not be earlier than the start year.'))
  assert.ok(PAGE.includes('class="fw-cancel"'), 'Cancel is offered')
  assert.ok(PAGE.includes('class="fw-create"'), 'Create is offered')
  assert.ok(PAGE.includes("api.post('/api/priority-agendas/sets', payload)"),
    'the creation endpoint is unchanged')
  assert.ok(PAGE.includes('/api/priority-agendas/sets/${editingFrameworkId.value}'),
    'the edit endpoint is unchanged')
})
