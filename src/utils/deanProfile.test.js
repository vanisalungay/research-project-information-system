/**
 * deanProfile.test.js
 * ---------------------------------------------------------------------------
 * Guards the two account-level fixes on the Dean end-user screens:
 *
 *  - Manage Profile showed no endorsement scope at all, so a Dean could not see the
 *    college / department they actually act for. The field must render for a Dean only,
 *    must be read-only (the RPS Administrator owns it), and must show the account's real
 *    value - never a hardcoded college.
 *
 *  - The sign-in form showed two eyes: the app's own PasswordInput toggle and the
 *    password reveal control Edge draws inside a password input. The browser's control
 *    must stay hidden without introducing a second toggle.
 *
 * Both files are read as text - the technique deanPages.test.js uses - and each check
 * states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PROFILE = read('../views/Shared/ProfileSettings.vue')
const PASSWORD_INPUT = read('../components/PasswordInput.vue')

test('a Dean sees the assigned college / department as read-only account information', () => {
  assert.match(
    PROFILE,
    /const isDean = computed\(\(\) => userStore\.user\?\.role === UserRole\.DEAN\)/,
    'the field must be scoped to the Dean role, not every account',
  )
  assert.match(
    PROFILE,
    /<div v-if="isDean" class="field-group">[\s\S]*?Assigned College \/ Department/,
    'the read-only field must only render for a Dean',
  )
  assert.match(
    PROFILE,
    /id="assignedCollege"[\s\S]*?readonly[\s\S]*?aria-readonly="true"/,
    'the assigned college must not be editable from the profile page',
  )
  assert.match(
    PROFILE,
    /Assigned by the RPS Administrator and not editable here\./,
    'the page must say who owns the field',
  )
})

test('the assigned college comes from the account, not from a hardcoded value', () => {
  assert.match(
    PROFILE,
    /const assignedCollege = ref\(userStore\.user\?\.assignedCollege \|\| ''\)/,
    'the signed-in session must seed the field',
  )
  assert.match(
    PROFILE,
    /const response = await api\.get\(`\/api\/users\/\$\{storedUser\.id\}`\)/,
    'the account must be reloaded from the backend',
  )
  assert.match(
    PROFILE,
    /assignedCollege\.value = user\.assignedCollege \|\| ''/,
    'the backend account must refresh the field, so a reassignment is not stale',
  )
  assert.match(
    PROFILE,
    /:value="assignedCollege \|\| 'Not assigned'"/,
    'an account without an assignment must say so instead of rendering a blank',
  )
})

test('the browser password reveal control is hidden, leaving one eye', () => {
  // Edge draws its own reveal button inside every password input. The shared component
  // already supplies the accessible eye, so the native one is what made two appear.
  assert.match(
    PASSWORD_INPUT,
    /\.password-field > input::-ms-reveal,[\s\S]*?\.password-field > input::-ms-clear \{\s*display: none;/,
    'Edge native reveal/clear controls must be hidden',
  )
  assert.equal(
    (PASSWORD_INPUT.match(/class="password-field__toggle"/g) || []).length,
    1,
    'the component must render exactly one toggle',
  )
  assert.ok(
    !PASSWORD_INPUT.includes('type="password"'),
    'the input type stays bound to the component state, not hardcoded',
  )
})
