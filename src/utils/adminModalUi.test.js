/**
 * adminModalUi.test.js
 * ---------------------------------------------------------------------------
 * Guards the shared administrator modal language: Create user account and Reset password
 * must stay one component family.
 *
 * The two dialogs live in different SFCs (CreateUserModal.vue and UserAccounts.vue), so
 * nothing type-checks that they keep the same header, fields, panel, footer and buttons.
 * Both files are read as text and checked against the shared stylesheet they import.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CREATE = read('../views/Admin/CreateUserModal.vue')
const ACCOUNTS = read('../views/Admin/UserAccounts.vue')
const SHARED = read('../assets/adminModal.css')

test('both dialogs import and use the one shared stylesheet', () => {
  assert.ok(CREATE.includes("import '@/assets/adminModal.css'"))
  assert.ok(ACCOUNTS.includes("import '@/assets/adminModal.css'"))
  for (const cls of [
    'adm-modal', 'adm-header', 'adm-header-icon', 'adm-heading', 'adm-close',
    'adm-body', 'adm-field', 'adm-footer', 'adm-btn--secondary', 'adm-btn--primary',
  ]) {
    assert.ok(SHARED.includes(`.${cls}`), `the shared stylesheet must define .${cls}`)
  }
})

test('the Create user account dialog matches the reference structure', () => {
  const standard = CREATE.slice(CREATE.indexOf('STANDARD ACCOUNT CREATION'))
  assert.ok(standard.includes('class="adm-modal"'))
  assert.ok(standard.includes('ref="standardModal"'))
  assert.ok(standard.includes('tabindex="-1"'))
  assert.ok(standard.includes('aria-labelledby="create-account-title"'))
  assert.ok(CREATE.includes('class="adm-overlay"'), 'the shared overlay is used')
  assert.ok(standard.includes('>Create user account</h2>'))
  assert.ok(standard.includes('Add an official account and choose what this person can access.'))

  // Fields in reference order.
  const name = standard.indexOf('for="name">Full name</label>')
  const hint = standard.indexOf('First name and last name only. Example: Juan Dela Cruz')
  const role = standard.indexOf('for="role">User role</label>')
  const panel = standard.indexOf('id="createdForYouTitle"')
  const email = standard.indexOf('for="email">Email address</label>')
  const password = standard.indexOf('for="tempPassword">Temporary password</label>')
  assert.ok(name > -1 && hint > name, 'the full name and its helper text')
  assert.ok(role > hint, 'User role follows the full name field')
  assert.ok(panel > role, 'Created for you follows the role')
  assert.ok(email > panel, 'the email preview sits inside the panel')
  assert.ok(password > email, 'the password preview follows the email')
  assert.ok(standard.includes('Built from the name. If the address is taken, a number is added before the domain.'))
  assert.ok(standard.includes('Generated securely when you create the account'))
  assert.ok(standard.includes('You will see it once, right after the account is created.'))
  assert.ok(standard.includes('id="email"'))
  assert.ok(standard.includes(':value="generatedEmail"'), 'the preview is generated, not hardcoded')
  assert.match(
    standard,
    /<input\s+id="tempPassword"[\s\S]*?readonly/,
    'the password status is a labelable read-only control',
  )

  // Footer: Cancel and Create account, with an arrow on the primary action.
  assert.ok(standard.includes('class="adm-footer"'))
  assert.match(standard, /Cancel\s*<\/button>/)
  assert.ok(standard.includes("isLoading ? 'Creating…' : 'Create account'"))
  assert.match(standard, /class="adm-btn adm-btn--primary"[\s\S]*?<line x1="4" y1="12" x2="19" y2="12"/)
})

test('the Reset password dialog uses the same family, with real account data', () => {
  const reset = ACCOUNTS.slice(ACCOUNTS.indexOf('Reset Password Modal'))
  assert.ok(reset.includes('class="adm-overlay"'))
  assert.ok(reset.includes('class="adm-modal"'))
  assert.ok(reset.includes('aria-labelledby="reset-password-title"'))
  assert.ok(reset.includes('>Reset password</h2>'))
  assert.ok(reset.includes('Generate a temporary password for this user to regain access.'))
  assert.ok(reset.includes('class="adm-header-icon"'))
  assert.ok(reset.includes('Selected account'))
  assert.ok(reset.includes('selectedUser?.name'))
  assert.ok(reset.includes('selectedUser?.email'))
  assert.ok(reset.includes('roleLabels(selectedUser)'))
  assert.ok(reset.includes('class="adm-footer"'))
  assert.ok(reset.includes("resetLoading ? 'Resetting…' : 'Reset password'"))
  assert.ok(reset.includes('class="adm-input"'), 'password fields use the shared input style')
  assert.ok(reset.includes('<form class="adm-form" @submit.prevent="resetPassword">'))
  assert.ok(reset.includes('type="submit"'), 'Enter submits the reset form')
  assert.ok(reset.includes('@keydown.esc="closeResetModal"'))
  assert.ok(reset.includes('onResetKeydown'), 'Tab stays in the reset dialog')
  assert.ok(reset.includes('if (resetLoading.value) return'),
    'a reset in flight cannot be dismissed or submitted twice')
})

test('the legacy element selectors no longer override the shared dialog classes', () => {
  // Vue's scoped input/select/button selectors outrank the shared .adm-* classes, which
  // made Create and Reset look different. The generic rules must stay removed.
  assert.ok(!/^input,\s*$/m.test(CREATE), 'no generic input/select rule')
  assert.ok(!/^button \{$/m.test(CREATE), 'no generic button rule')
  assert.ok(!/^\.overlay \{/m.test(CREATE), 'the shared overlay replaced the local one')
  assert.match(SHARED, /\.adm-header \{[\s\S]*?padding: 12px 12px 11px/)
})

test('the shared dialog keeps the compact, scrollable, accessible contract', () => {
  assert.match(SHARED, /\.adm-modal \{[\s\S]*?width: 404px/)
  assert.match(SHARED, /\.adm-modal \{[\s\S]*?max-height: 90vh/)
  assert.match(SHARED, /\.adm-body \{[\s\S]*?overflow-y: auto/)
  assert.match(SHARED, /\.adm-footer \{[\s\S]*?grid-template-columns:/)
  assert.match(SHARED, /\.adm-close:focus-visible/)
  assert.match(SHARED, /\.adm-btn:focus-visible/)
  assert.match(CREATE, /aria-label="Close create user account modal"/)
  assert.ok(ACCOUNTS.includes('aria-label="Close reset password modal"'))
})
