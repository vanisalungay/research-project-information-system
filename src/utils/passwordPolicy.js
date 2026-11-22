/**
 * passwordPolicy.js
 * ---------------------------------------------------------------------------
 * The one description of the RPIS password policy, shared by every screen that
 * creates or changes a password:
 *
 *   - first-login / temporary-password change  (views/Auth/ChangePassword.vue)
 *   - Manage Profile -> Security & Access      (views/Shared/ProfileSettings.vue)
 *   - administrator password reset             (views/Admin/UserAccounts.vue)
 *   - account registration                     (views/Auth/CreateAccount.vue,
 *                                               views/Auth/ProponentRegister.vue)
 *
 * The backend enforces the same rules in PasswordPolicy.java; this module only
 * drives the live checklist and the disabled state of the submit button. It
 * never sends, stores or logs the password.
 *
 * The lower-case rule predates the explicit requirement list: the existing
 * backend policy already required it, and removing it would weaken a control.
 * The example password satisfies every rule.
 */

/** The shortest accepted new password. */
export const PASSWORD_MIN_LENGTH = 8

/** A password that demonstrates every requirement. */
export const PASSWORD_EXAMPLE = 'RpisSystem@2026'

/** The requirements, in the order the interface presents them. */
export const PASSWORD_REQUIREMENTS = Object.freeze([
  Object.freeze({
    key: 'length',
    label: 'At least 8 characters',
    test: (value) => value.length >= PASSWORD_MIN_LENGTH,
  }),
  Object.freeze({
    key: 'uppercase',
    label: 'At least 1 uppercase letter (A–Z)',
    test: (value) => /[A-Z]/.test(value),
  }),
  Object.freeze({
    key: 'lowercase',
    label: 'At least 1 lowercase letter (a–z)',
    test: (value) => /[a-z]/.test(value),
  }),
  Object.freeze({
    key: 'number',
    label: 'At least 1 number (0–9)',
    test: (value) => /[0-9]/.test(value),
  }),
  Object.freeze({
    key: 'symbol',
    label: 'At least 1 special character or symbol (e.g., @, #, $, !, *)',
    test: (value) => /[^A-Za-z0-9]/.test(value),
  }),
])

/** The instruction shown above the field checklist. */
export const PASSWORD_REQUIREMENT_INTRO =
  'Your new password must meet all of the following requirements:'

/** The per-field instructions. */
export const PASSWORD_FIELD_HINTS = Object.freeze({
  current: 'Enter your existing password.',
  new: 'Create a new password that meets all the requirements listed above.',
  confirm: 'Re-enter your new password to verify that both entries match.',
})

/**
 * Every requirement paired with whether `value` satisfies it. The result is a new
 * array, so a caller can render it directly and it changes with the value.
 *
 * @param {string} value the password being typed
 * @returns {{ key: string, label: string, met: boolean }[]}
 */
export function passwordChecks(value) {
  const password = typeof value === 'string' ? value : ''
  return PASSWORD_REQUIREMENTS.map((requirement) => ({
    key: requirement.key,
    label: requirement.label,
    met: requirement.test(password),
  }))
}

/** True only when every requirement is met. */
export function passwordMeetsPolicy(value) {
  return passwordChecks(value).every((check) => check.met)
}

/** The requirements still missing, in display order. */
export function unmetPasswordRequirements(value) {
  return passwordChecks(value).filter((check) => !check.met)
}

/**
 * The first unmet requirement as an instruction, or '' when the password is
 * acceptable. The message names the rule only - never the value.
 */
export function passwordPolicyMessage(value) {
  const missing = unmetPasswordRequirements(value)
  if (missing.length === 0) return ''
  return `${missing[0].label.replace(/^At least /, 'Add at least ')}.`
}

/** True when the new password was typed the same way twice and is non-empty. */
export function passwordConfirmationMatches(password, confirmation) {
  return (
    typeof password === 'string' &&
    password !== '' &&
    password === confirmation
  )
}

/** Everything the form needs before its submit button may be enabled. */
export function passwordFormReady(password, confirmation) {
  return passwordMeetsPolicy(password) && passwordConfirmationMatches(password, confirmation)
}
