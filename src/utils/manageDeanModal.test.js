/**
 * manageDeanModal.test.js
 * ---------------------------------------------------------------------------
 * The Manage Dean modal on Dean Role Management. It shows the selected Dean's real
 * appointment/account values and offers exactly one management action - Replace Dean -
 * without any in-place editing control. Revocation is automatic as part of the replacement,
 * so there is no standalone Revoke action.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const MODAL = read('../views/Admin/ManageDeanModal.vue')

test('the modal has the reference header and fixed footer', () => {
  assert.ok(MODAL.includes('Manage Dean'), 'the reference title')
  assert.ok(
    MODAL.includes('Manage the account and assignment of this college dean.'),
    'the reference subtitle',
  )
  assert.ok(MODAL.includes('class="md-header-icon"'), 'the graduation-cap icon container')
  assert.ok(MODAL.includes('aria-label="Close manage dean"'), 'the close icon is labelled')
  assert.match(MODAL, /<footer class="md-footer">/)
  assert.match(MODAL, /\.md-modal \{[\s\S]*?max-height: 90vh/)
  assert.match(MODAL, /\.md-body \{[\s\S]*?overflow-y: auto/)
})

test('the profile uses the actual name, initials and appointment status', () => {
  assert.ok(MODAL.includes('initialsFromName(deanName.value)'), 'initials come from the name')
  assert.ok(MODAL.includes('{{ deanName || \'—\' }}'), 'the actual name is shown')
  assert.ok(MODAL.includes('{{ statusLabel }}'), 'the status label is dynamic')
  assert.ok(MODAL.includes('statusClass'), 'the status tone is dynamic')
  assert.ok(MODAL.includes('md-status-dot'), 'the reference status dot is present')
})

test('the information card reads the real values', () => {
  assert.ok(MODAL.includes('{{ email || \'—\' }}'), 'email comes from the selection')
  assert.ok(MODAL.includes('collegeDisplayParts(raw)'), 'the college name/code split is shared')
  assert.ok(MODAL.includes('{{ collegeName }}'), 'the full college name is shown')
  assert.ok(MODAL.includes('{{ collegeCode }}'), 'the code badge is conditional')
  assert.ok(MODAL.includes('{{ designation || \'—\' }}'), 'designation is shown')
  assert.ok(MODAL.includes('{{ roleLabel }}'), 'role is shown')
  assert.ok(MODAL.includes('{{ dateAssigned }}'), 'date assigned is shown')
  assert.ok(MODAL.includes('/api/users/${props.assignment.userId}'), 'the current account is read')
  assert.ok(MODAL.includes('assignment.dateAssigned'), 'the assignment date is the source')
})

test('replacement is the only management action; there is no standalone revoke', () => {
  assert.ok(MODAL.includes('@click="$emit(\'replace\')"'), 'Replace Dean is offered')
  assert.ok(!MODAL.includes("$emit('revoke')"), 'no standalone revoke is emitted')
  assert.ok(!MODAL.includes('md-revoke'), 'no revoke button is rendered')
  assert.ok(!MODAL.includes('>Revoke'), 'no Revoke label is rendered')
  assert.ok(!MODAL.includes('canRevoke'), 'no revoke eligibility remains')
  assert.ok(MODAL.includes('The one management action'), 'the replacement is the one action')
})

test('the modal is read-only, accessible and responsive', () => {
  assert.ok(MODAL.includes('role="dialog"'))
  assert.ok(MODAL.includes('aria-modal="true"'))
  assert.ok(MODAL.includes('@click.self="$emit(\'close\')"'))
  assert.ok(MODAL.includes('onKeydown'), 'Escape and Tab are handled')
  assert.ok(MODAL.includes('previouslyFocused?.focus?.()'), 'focus returns to the trigger')
  assert.ok(MODAL.includes("document.body.style.overflow = 'hidden'"), 'background scroll is locked')
  assert.ok(!MODAL.includes('md-edit'), 'no editing control is introduced')
  assert.ok(!MODAL.includes('<input'), 'the details are read-only')
  assert.match(MODAL, /@media \(max-width: 480px\)[\s\S]*?\.md-footer \{[\s\S]*?grid-template-columns: minmax\(0, 1fr\)/)
})
