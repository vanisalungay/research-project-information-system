/**
 * deanAccountModal.test.js
 * ---------------------------------------------------------------------------
 * The Create Dean Account modal shown from Dean Role Management. It is still the
 * standard CreateUserModal and the same /api/users/admin/create call; only the Dean
 * presentation is compact and scrollable.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const MODAL = read('../views/Admin/CreateUserModal.vue')
const PAGE = read('../views/Admin/DeanRoleManagement.vue')

test('the Dean modal has the reference header, subtitle and account icon', () => {
  assert.ok(MODAL.includes('Create user account'), 'the reference title')
  assert.ok(
    MODAL.includes('Add an official account and choose what this person can access.'),
    'the reference subtitle',
  )
  assert.ok(MODAL.includes('class="dean-header-icon"'), 'the icon container exists')
  assert.ok(MODAL.includes('aria-labelledby="dean-create-title"'), 'the dialog is labelled')
  assert.ok(MODAL.includes('aria-label="Close create user account modal"'), 'the close is labelled')
})

test('the form follows the reference field order and wording', () => {
  const start = MODAL.indexOf('DEAN ACCOUNT CREATION')
  const DEAN = MODAL.slice(start, MODAL.indexOf('STANDARD ACCOUNT CREATION'))
  const order = [
    '<label for="deanFullName">Full name (First Name Last Name)</label>',
    'First Name Last Name - for example, Juan Dela Cruz.',
    '<label for="deanRole">Role</label>',
    '<label for="deanDesignation">Designation / title</label>',
    '<label for="deanCollege">Assigned college</label>',
    'Select the college this Dean endorses for',
    'Created for you',
    'Email address',
    'Built from the name. If this person already has an account, that account is',
    'reused - the Dean role is added without a duplicate address or password.',
    'Temporary password',
    'Generated securely when you create the account',
    'You will see it once, right after the account is created.',
  ]
  let last = -1
  for (const text of order) {
    const at = DEAN.indexOf(text)
    assert.ok(at > last, `${text} appears in the reference order`)
    last = at
  }
  assert.ok(DEAN.includes('class="dean-row"'), 'Role and Designation share a row')
  assert.ok(DEAN.includes('class="created-for-you"'), 'the credentials card is distinct')
})

test('the Dean name is a plain First Name Last Name entry', () => {
  assert.ok(MODAL.includes('placeholder="Juan Dela Cruz"'), 'the reference example')
  assert.ok(
    MODAL.includes('First Name Last Name - for example, Juan Dela Cruz.'),
    'the format instruction',
  )
  // The old Last, First / middle initial / suffix guidance is gone from the Dean form.
  const start = MODAL.indexOf('DEAN ACCOUNT CREATION')
  const deanForm = MODAL.slice(start, MODAL.indexOf('STANDARD ACCOUNT CREATION'))
  assert.ok(!deanForm.includes('Middle Initial'), 'no middle initial is requested')
  assert.ok(!deanForm.includes('Last name, First name'), 'no Last, First order is requested')
  // Validation: one field, first and last name, no commas.
  assert.ok(MODAL.includes("user.value.name.includes(',')"), 'the comma order is refused')
  assert.ok(MODAL.includes('Enter both the first name and the last name'),
    'a first and a last name are required')
  assert.ok(MODAL.includes('formatPersonName(user.value.name)'),
    'capitalization normalization is preserved')
})

test('the standard Create Account form uses the same First Name Last Name entry', () => {
  assert.ok(MODAL.includes('placeholder="Juan Dela Cruz"'), 'the simple example')
  assert.ok(
    MODAL.includes('First name and last name only. Example: Juan Dela Cruz'),
    'the standard form helper text',
  )
  assert.ok(!MODAL.includes('placeholder="Dela Cruz, Juan, M., Jr."'),
    'the old comma-format placeholder is removed')
  assert.ok(!MODAL.includes('PersonNameHint'), 'the old Last, First hint is not used here')
  assert.ok(
    MODAL.includes('The Create User Account form asks for a plain First Name Last Name entry.'),
    'the validation applies to every role, not only the Dean form',
  )
  assert.ok(MODAL.includes('Enter both the first name and the last name'),
    'a first and a last name are required')
})

test('the footer is fixed and contains the two reference actions', () => {
  assert.ok(MODAL.includes('<footer class="dean-footer">'), 'the footer exists')
  assert.ok(MODAL.includes('class="dean-cancel"'), 'Cancel is on the left')
  assert.ok(MODAL.includes('class="dean-create"'), 'Create account is on the right')
  assert.ok(MODAL.includes("{{ isLoading ? 'Creating...' : 'Create account' }}"), 'the label')
  assert.match(MODAL, /\.dean-modal \{[\s\S]*?max-height: 90vh/)
  assert.match(MODAL, /\.dean-body \{[\s\S]*?overflow-y: auto/)
})

test('the generated email preview and one-time password follow the existing rules', () => {
  assert.ok(MODAL.includes('generatedEmail'), 'the email preview is derived from the name')
  assert.ok(MODAL.includes('institutionalEmail'), 'through the existing naming rule')
  assert.ok(!MODAL.includes('v-model="user.email"'), 'the email is never typed')
  assert.ok(!MODAL.includes('v-model="user.password"'), 'the password is never set by the browser')
  assert.ok(MODAL.includes('body.temporaryPassword'), 'the one-time password comes from the response')
  assert.ok(MODAL.includes('v-else class="dean-created-wrap"'), 'credentials replace the form after creation')
  assert.ok(MODAL.includes('emit(\'created\','), 'the list behind the modal is refreshed')
})

test('the modal preserves the Dean role, college requirement and creation endpoint', () => {
  assert.ok(PAGE.includes('initial-role="DEAN"') && PAGE.includes('lock-role'))
  assert.ok(MODAL.includes("api.post('/api/users/admin/create'"), 'one account-creation endpoint')
  assert.ok(MODAL.includes('payload.designation'), 'the designation is sent')
  assert.ok(MODAL.includes('payload.assignedCollege'), 'the college is sent')
  assert.ok(MODAL.includes("if (isDean.value && !props.lockCollege && !user.value.assignedCollege)"),
    'college is required unless a replacement inherits it')
  assert.ok(MODAL.includes("if (isDean.value && !user.value.designation.trim())"), 'designation is required')
  assert.ok(MODAL.includes('if (isLoading.value) return'), 'double submission is prevented')
  assert.ok(MODAL.includes(':disabled="isLoading || !canCreateDean"'), 'the footer requires valid fields')
})

test('the keyboard and focus contract is present', () => {
  assert.ok(MODAL.includes('aria-modal="true"'))
  assert.ok(MODAL.includes('@keydown.esc="requestClose"'))
  assert.ok(MODAL.includes('window.addEventListener(\'keydown\', onModalKeydown)'))
  assert.ok(MODAL.includes("event?.key !== 'Tab'"), 'Tab is kept inside the Dean modal')
  assert.ok(MODAL.includes('deanNameInput.value?.focus()'), 'the name field receives focus')
  assert.ok(MODAL.includes('previouslyFocused.focus()'), 'focus returns to the trigger')
})
