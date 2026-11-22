/**
 * passwordPolicy.test.js
 * ---------------------------------------------------------------------------
 * The shared frontend policy. The backend mirrors these rules in
 * PasswordPolicy.java; these tests pin the live checklist and the rule that
 * enables a form's submit button.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import {
  PASSWORD_EXAMPLE,
  PASSWORD_REQUIREMENTS,
  PASSWORD_REQUIREMENT_INTRO,
  passwordChecks,
  passwordConfirmationMatches,
  passwordFormReady,
  passwordMeetsPolicy,
  passwordPolicyMessage,
  unmetPasswordRequirements,
} from './passwordPolicy.js'

test('the interface lists the required length, uppercase, number and symbol rules', () => {
  const labels = PASSWORD_REQUIREMENTS.map((requirement) => requirement.label)
  assert.ok(labels.includes('At least 8 characters'))
  assert.ok(labels.includes('At least 1 uppercase letter (A–Z)'))
  assert.ok(labels.includes('At least 1 number (0–9)'))
  assert.ok(labels.some((label) => label.includes('special character or symbol')))
  assert.match(PASSWORD_REQUIREMENT_INTRO, /Your new password must meet all of the following requirements:/)
})

test('the example password satisfies every requirement', () => {
  assert.equal(PASSWORD_EXAMPLE, 'RpisSystem@2026')
  assert.equal(passwordMeetsPolicy(PASSWORD_EXAMPLE), true)
  assert.deepEqual(unmetPasswordRequirements(PASSWORD_EXAMPLE), [])
})

test('every requirement reports its own state and updates with the value', () => {
  const empty = passwordChecks('')
  assert.equal(empty.length, PASSWORD_REQUIREMENTS.length)
  assert.ok(empty.every((check) => check.met === false))

  const partial = passwordChecks('Rpis2026')
  assert.equal(partial.find((check) => check.key === 'length').met, true)
  assert.equal(partial.find((check) => check.key === 'uppercase').met, true)
  assert.equal(partial.find((check) => check.key === 'lowercase').met, true)
  assert.equal(partial.find((check) => check.key === 'number').met, true)
  assert.equal(partial.find((check) => check.key === 'symbol').met, false)

  const complete = passwordChecks(PASSWORD_EXAMPLE)
  assert.ok(complete.every((check) => check.met === true))
})

test('a password shorter than eight characters is rejected', () => {
  assert.equal(passwordMeetsPolicy('Rpi1!'), false)
})

test('a password without an uppercase letter is rejected', () => {
  assert.equal(passwordMeetsPolicy('rpis@2026'), false)
  assert.match(passwordPolicyMessage('rpis@2026'), /uppercase letter/)
})

test('a password without a number is rejected', () => {
  assert.equal(passwordMeetsPolicy('RpisSystem@'), false)
  assert.match(passwordPolicyMessage('RpisSystem@'), /number/)
})

test('a password without a symbol is rejected', () => {
  assert.equal(passwordMeetsPolicy('RpisSystem2026'), false)
  assert.match(passwordPolicyMessage('RpisSystem2026'), /special character or symbol/)
})

test('the policy message names the first unmet requirement only', () => {
  assert.equal(passwordPolicyMessage(PASSWORD_EXAMPLE), '')
  assert.match(passwordPolicyMessage('abc'), /^Add at least 8 characters\.$/)
})

test('confirmation must be non-empty and identical', () => {
  assert.equal(passwordConfirmationMatches(PASSWORD_EXAMPLE, PASSWORD_EXAMPLE), true)
  assert.equal(passwordConfirmationMatches(PASSWORD_EXAMPLE, ''), false)
  assert.equal(passwordConfirmationMatches(PASSWORD_EXAMPLE, 'RpisSystem@2027'), false)
  assert.equal(passwordConfirmationMatches('', ''), false)
})

test('a form is ready only with a valid password and a matching confirmation', () => {
  assert.equal(passwordFormReady(PASSWORD_EXAMPLE, PASSWORD_EXAMPLE), true)
  assert.equal(passwordFormReady(PASSWORD_EXAMPLE, 'RpisSystem@2027'), false)
  assert.equal(passwordFormReady('weak', 'weak'), false)
})
