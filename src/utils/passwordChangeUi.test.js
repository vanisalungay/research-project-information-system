/**
 * passwordChangeUi.test.js
 * ---------------------------------------------------------------------------
 * Guards the wiring of the password-change experience across every interface:
 * the shared checklist and field hints are present, the submit buttons wait for
 * the same policy, and the backend remains the authority.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CHANGE = read('../views/Auth/ChangePassword.vue')
const PROFILE = read('../views/Shared/ProfileSettings.vue')
const RESET = read('../views/Admin/UserAccounts.vue')
const CREATE = read('../views/Auth/CreateAccount.vue')
const REGISTER = read('../views/Auth/ProponentRegister.vue')
const REQUIREMENTS = read('../components/PasswordRequirements.vue')
const HINT = read('../components/PasswordFieldHint.vue')
const POLICY_JS = read('passwordPolicy.js')
const POLICY_JAVA = read('../../backend/src/main/java/com/rpis/backend/util/PasswordPolicy.java')
const USER_SERVICE = read('../../backend/src/main/java/com/rpis/backend/service/UserService.java')
const CONTROLLER = read('../../backend/src/main/java/com/rpis/backend/controller/UserController.java')

test('the checklist shows the requested instruction, rules and example', () => {
  assert.ok(REQUIREMENTS.includes('Password Requirements'))
  assert.ok(REQUIREMENTS.includes('PASSWORD_REQUIREMENT_INTRO'))
  assert.ok(REQUIREMENTS.includes('PASSWORD_EXAMPLE'))
  assert.ok(REQUIREMENTS.includes('Password Requirements'))
  assert.ok(REQUIREMENTS.includes('is-met'))
  assert.ok(REQUIREMENTS.includes('Passwords match.'))
  assert.ok(REQUIREMENTS.includes('Passwords do not match.'))
  assert.ok(REQUIREMENTS.includes('aria-live="polite"'), 'updates are announced')
  assert.ok(POLICY_JS.includes("PASSWORD_EXAMPLE = 'RpisSystem@2026'"))
})

test('the field hints use the requested wording', () => {
  assert.ok(HINT.includes('PASSWORD_FIELD_HINTS'))
  assert.ok(POLICY_JS.includes("current: 'Enter your existing password.'"))
  assert.ok(POLICY_JS.includes(
    "new: 'Create a new password that meets all the requirements listed above.'",
  ))
  assert.ok(POLICY_JS.includes(
    "confirm: 'Re-enter your new password to verify that both entries match.'",
  ))
})

test('the first-login password change shows the rules and waits for them', () => {
  assert.ok(CHANGE.includes("import PasswordRequirements from '@/components/PasswordRequirements.vue'"))
  assert.ok(CHANGE.includes("import PasswordFieldHint from '@/components/PasswordFieldHint.vue'"))
  assert.ok(CHANGE.includes('<PasswordRequirements'))
  assert.ok(CHANGE.includes('kind="current"'))
  assert.ok(CHANGE.includes('kind="new"'))
  assert.ok(CHANGE.includes('kind="confirm"'))
  assert.ok(CHANGE.includes('passwordFormReady('))
  assert.ok(CHANGE.includes(':disabled="submitting || !isComplete"'))
  assert.ok(CHANGE.includes("api.put('/api/users/change-password'"))
})

test('Manage Profile sends password changes through the current-password endpoint', () => {
  assert.ok(PROFILE.includes("import PasswordRequirements from '@/components/PasswordRequirements.vue'"))
  assert.ok(PROFILE.includes('<PasswordRequirements'))
  assert.ok(PROFILE.includes('kind="current"'))
  assert.ok(PROFILE.includes('kind="new"'))
  assert.ok(PROFILE.includes('kind="confirm"'))
  assert.ok(PROFILE.includes(':disabled="securitySubmitDisabled || securitySaving"'))
  assert.ok(PROFILE.includes("api.put('/api/users/change-password'"))
  assert.ok(PROFILE.includes('currentPassword: securityForm.oldPassword'))
  assert.ok(PROFILE.includes('confirmPassword: securityForm.confirmPassword'))
  assert.ok(!PROFILE.includes('payload.password'), 'a password is never sent through the profile update')
})

test('the administrator reset modal enforces the same policy', () => {
  assert.ok(RESET.includes("import PasswordRequirements from '@/components/PasswordRequirements.vue'"))
  assert.ok(RESET.includes('<PasswordRequirements'))
  assert.ok(RESET.includes('resetPasswordValid'))
  assert.ok(RESET.includes(':disabled="resetLoading || !resetPasswordValid"'))
  assert.ok(RESET.includes('passwordPolicyMessage('))
  assert.ok(!RESET.includes('at least 6 characters'), 'the old six-character rule is gone')
})

test('both registration forms show the rules and disable an incomplete submit', () => {
  for (const source of [CREATE, REGISTER]) {
    assert.ok(source.includes("import PasswordRequirements from '@/components/PasswordRequirements.vue'"))
    assert.ok(source.includes("import PasswordFieldHint from '@/components/PasswordFieldHint.vue'"))
    assert.ok(source.includes('<PasswordRequirements'))
    assert.ok(source.includes('passwordFormReady('))
    assert.ok(source.includes(':disabled="!passwordReady"'))
  }
})

test('the backend enforces every rule independently of the browser', () => {
  assert.ok(POLICY_JAVA.includes('MINIMUM_LENGTH = 8'))
  assert.ok(POLICY_JAVA.includes('at least one uppercase letter'))
  assert.ok(POLICY_JAVA.includes('at least one lowercase letter'))
  assert.ok(POLICY_JAVA.includes('at least one number'))
  assert.ok(POLICY_JAVA.includes('at least one special character or symbol'))
  assert.ok(POLICY_JAVA.includes('requireValid('))
  assert.ok(USER_SERVICE.includes('PasswordPolicy.requireValid(user.getPassword())'),
    'registration is validated')
  assert.ok(USER_SERVICE.includes('PasswordPolicy.requireValid(updatedUser.getPassword())'),
    'the administrator reset is validated')
  assert.ok(USER_SERVICE.includes('PasswordPolicy.requireValid(newPassword)'),
    'the change-password path is validated')
  assert.ok(CONTROLLER.includes('@PutMapping("/change-password")'))
  assert.ok(CONTROLLER.includes('currentUser.requireUserId()'))
})

test('no password value is logged or returned', () => {
  assert.ok(!POLICY_JAVA.includes('System.out'))
  assert.ok(!POLICY_JAVA.toLowerCase().includes('log.'))
  assert.ok(!POLICY_JS.includes('console.'))
  assert.ok(!REQUIREMENTS.includes('console.'))
  assert.ok(!HINT.includes('console.'))
  // An axios error's config carries the submitted request body, so a page that
  // handles a password request must never log the error object itself.
  assert.ok(!CREATE.includes('console.log(error)'))
  assert.ok(!PROFILE.includes("console.error('Could not update the account credentials:', error)"))
  assert.ok(!RESET.includes("console.error('Failed to reset password:', err)"))
})
