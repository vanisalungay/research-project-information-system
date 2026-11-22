/**
 * passwordFields.test.js
 * ---------------------------------------------------------------------------
 * Guards the one password field of RPIS: every password input is the shared
 * PasswordInput, which starts hidden and has an accessible eye toggle inside the
 * field. The value is bound normally, so validation and submission are unchanged.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

/** The template only - style selectors such as input[type="password"] are not fields. */
const templateOf = (source) => source
  .replace(/<style[\s\S]*?<\/style>/g, '')
  .replace(/<script[\s\S]*?<\/script>/g, '')

const COMPONENT = read('../components/PasswordInput.vue')

test('the shared field is hidden by default and toggles only the input type', () => {
  assert.match(COMPONENT, /const visible = ref\(false\)/)
  assert.match(COMPONENT, /:type="visible \? 'text' : 'password'"/)
  assert.match(COMPONENT, /const model = defineModel\(\{ type: String, default: '' \}\)/)
  assert.match(COMPONENT, /@input="model = \$event\.target\.value"/)
})

test('the toggle has an accessible name and its own state', () => {
  assert.match(COMPONENT, /:aria-label="visible \? 'Hide password' : 'Show password'"/)
  assert.match(COMPONENT, /:aria-pressed="visible"/)
  assert.match(COMPONENT, /<button\s+type="button"/)
  assert.match(COMPONENT, /:focus-visible/)
})

test('the eye and eye-off icons are inline SVGs, not a new dependency', () => {
  assert.match(COMPONENT, /v-if="visible"[\s\S]*?<path d="m3 3 18 18"/, 'the revealed state shows eye-off')
  assert.match(COMPONENT, /v-else[\s\S]*?<circle cx="12" cy="12" r="3"/, 'the hidden state shows the eye')
  assert.ok(!COMPONENT.includes('from \'lucide'), 'no icon dependency is introduced')
})

test('a mouse toggle returns focus so typing can continue', () => {
  assert.match(COMPONENT, /if \(event\?\.detail > 0\) inputRef\.value\?\.focus\(\)/)
})

test('every password field uses the shared component, each independently', () => {
  const files = {
    '../components/HeaderLogo.vue': 1,
    '../views/Admin/UserAccounts.vue': 2,
    '../views/Auth/ChangePassword.vue': 3,
    '../views/Auth/CreateAccount.vue': 2,
    '../views/Auth/Login.vue': 1,
    '../views/Auth/ProponentLogin.vue': 1,
    '../views/Auth/ProponentRegister.vue': 2,
    '../views/Shared/ProfileSettings.vue': 3,
  }

  let total = 0
  for (const [path, expected] of Object.entries(files)) {
    const source = read(path)
    const template = templateOf(source)
    const count = (template.match(/<PasswordInput/g) || []).length
    total += count
    assert.equal(count, expected, `${path} must render ${expected} shared password field(s)`)
    assert.ok(source.includes("from '@/components/PasswordInput.vue'"),
      `${path} must import the shared password field`)
    assert.ok(!template.includes('type="password"'),
      `${path} must not keep an ad-hoc password input`)
    assert.ok(!/reveal\.|cp-toggle/.test(template),
      `${path} must not keep its own visibility toggle`)
  }
  assert.equal(total, 15, '15 password inputs were identified across the application')
})

test('the first-time password page uses the shared eye instead of Show/Hide text buttons', () => {
  const source = read('../views/Auth/ChangePassword.vue')
  assert.ok(!source.includes('cp-toggle'), 'the old text toggle is gone')
  assert.equal((templateOf(source).match(/<PasswordInput/g) || []).length, 3)
})
