/**
 * adminUserAccounts.test.js
 * ---------------------------------------------------------------------------
 * Guards the administrator's account screens:
 *
 *   src/views/Admin/UserAccounts.vue       the list, the search and the page header
 *   src/views/Admin/CreateUserModal.vue    the standard Create Account form
 *   src/views/Admin/DeanAccountsModal.vue  the Dean role management screen
 *
 * Three things were wrong here, and each of them is checked below:
 *
 *   1. The page carried a separate "Dean Accounts" button beside "+ Create Account", so
 *      a Dean was created by a second, different workflow - and because the header had
 *      three children with `justify-content: space-between`, "+ Create Account" was
 *      pushed into the middle of the page. A Dean is now created by the standard form
 *      like every other role, and the Dean screen only manages the role of an account
 *      that already holds it.
 *   2. The Create Account form did not offer the Dean role at all: the backend refused it
 *      (UserService.createStaffAccount) and the only way to provision a Dean was that
 *      separate screen. The form now offers it, and the backend routes that one role to
 *      the existing Dean provisioning service.
 *   3. A Dean account is stored with the role `DEAN` - never `RPS_DEAN`, which the
 *      database, the bearer token and SecurityConfig all compare verbatim. The value the
 *      form sends is checked against the backend's canonical constant, because a wrong
 *      role is either refused by the backend or stored as an account that can never sign
 *      in.
 *
 * The files are read as text - the technique navigation.test.js and
 * loginRoleOptions.test.js already use - so none of this needs a browser or a build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const USER_ACCOUNTS = read('../views/Admin/UserAccounts.vue')
const CREATE_MODAL = read('../views/Admin/CreateUserModal.vue')
const DEAN_MODAL = read('../views/Admin/DeanAccountsModal.vue')
const USER_STORE = read('../stores/userData.js')
const BACKEND_ROLES = read('../../backend/src/main/java/com/rpis/backend/model/UserRoles.java')

/** The role vocabulary, as declared key -> the role string the backend stores. */
function roleVocabulary() {
  const block = USER_STORE.match(/export const UserRole = \{([\s\S]*?)\n\}/)
  assert.ok(block, 'the role vocabulary must be declared in src/stores/userData.js')

  const vocabulary = new Map()
  for (const match of block[1].matchAll(/([A-Z_]+):\s*'([A-Z_]+)'/g)) {
    vocabulary.set(match[1], match[2])
  }
  assert.ok(vocabulary.size > 0, 'the role vocabulary must not be empty')
  return vocabulary
}

/** The role options the Create Account form offers, parsed value by value. */
function roleOptions() {
  return [...CREATE_MODAL.matchAll(/<option value="([A-Z_]+)">([^<]*)<\/option>/g)].map(
    (match) => ({ value: match[1], label: match[2].trim() }),
  )
}

test('the Create Account form offers the Dean role, labelled "Dean"', () => {
  const dean = roleOptions().find((option) => option.value === 'DEAN')

  assert.ok(dean, 'the standard Create Account form must offer the Dean role')
  assert.match(dean.label, /^Dean\b/, `the role must be labelled "Dean", not "${dean.label}"`)
  assert.ok(
    !/RPS/i.test(dean.label),
    `the Dean is not an RPS sub-role, so the label must not say so: "${dean.label}"`,
  )
})

test('the role sent for a Dean is the value the backend stores', () => {
  // The canonical value, from the one place the backend declares it. It is stored
  // verbatim in users.role, embedded in the bearer token and compared by SecurityConfig,
  // so the form has to send exactly this.
  assert.match(
    BACKEND_ROLES,
    /String DEAN = "DEAN"/,
    'the backend must declare the canonical Dean role as DEAN',
  )
  assert.ok(
    !/String\s+RPS_DEAN/.test(BACKEND_ROLES),
    'no RPS_DEAN constant may be declared: existing Dean accounts are stored as DEAN',
  )
  assert.equal(
    roleVocabulary().get('DEAN'),
    'DEAN',
    'the frontend vocabulary must send the same value the backend stores',
  )
})

test('every role the Create Account form offers exists in the role vocabulary', () => {
  const known = roleVocabulary()
  const options = roleOptions()

  for (const option of options) {
    assert.ok(
      known.has(option.value),
      `the form offers the unknown role ${option.value}; the backend would refuse the account`,
    )
  }

  // Every role the backend can store is still offered, so no account type lost its way in
  // while the Dean option was added.
  for (const required of ['RPS_STAFF', 'RPS_ADMIN', 'DEAN', 'OVCAF', 'OVCRIGE', 'REC', 'OC']) {
    assert.ok(
      options.some((option) => option.value === required),
      `the form must keep offering ${required}`,
    )
  }
})

test('the Dean fields are shown, validated and sent for the Dean role only', () => {
  // Both fields exist and are bound to the form state.
  assert.match(CREATE_MODAL, /v-model="user\.designation"/, 'the designation field must exist')
  assert.match(CREATE_MODAL, /v-model="user\.assignedCollege"/, 'the assigned college must exist')

  // They sit behind the Dean-only condition, so no other role sees them.
  assert.match(
    CREATE_MODAL,
    /<template v-if="isDean">[\s\S]*v-model="user\.designation"[\s\S]*v-model="user\.assignedCollege"[\s\S]*<\/template>/,
    'the Dean fields must be rendered only for the Dean role',
  )
  assert.match(
    CREATE_MODAL,
    /const isDean = computed\(\(\) => user\.value\.role === UserRole\.DEAN\)/,
    'the Dean condition must compare against the canonical role value',
  )

  // The college list is the shared one, never a second hardcoded list.
  assert.match(
    CREATE_MODAL,
    /import \{ RPIS_COLLEGES \}/,
    'the colleges must come from the shared list the rest of the application uses',
  )
  assert.match(CREATE_MODAL, /v-for="college in colleges"/, 'the college select must use that list')

  // Both are validated before the request is sent.
  assert.match(
    CREATE_MODAL,
    /isDean\.value && !user\.value\.designation\.trim\(\)/,
    'a Dean without a designation must be refused with a message',
  )
  assert.match(
    CREATE_MODAL,
    /isDean\.value && !user\.value\.assignedCollege/,
    'a Dean without an assigned college must be refused with a message',
  )

  // And they are added to the payload for the Dean role only, so every other role's
  // request is unchanged.
  assert.match(
    CREATE_MODAL,
    /if \(isDean\.value\) \{[\s\S]*payload\.designation[\s\S]*payload\.assignedCollege[\s\S]*\}/,
    'the Dean fields must not be sent for the other roles',
  )
})

test('the standard form is the only account-creation endpoint used', () => {
  assert.match(
    CREATE_MODAL,
    /api\.post\(\s*["']\/api\/users\/admin\/create["']/,
    'the form must keep posting to the existing account-creation endpoint',
  )
})

test('the page header keeps the title left and the single action on the right', () => {
  // One action, inside the right-aligned container: the Dean Accounts button that used to
  // sit beside it is what pushed this button into the middle of the page.
  assert.match(
    USER_ACCOUNTS,
    /<div class="header">\s*<h2>User Accounts<\/h2>[\s\S]*?<div class="header-actions">[\s\S]*?\+ Create Account[\s\S]*?<\/div>\s*<\/div>/,
    'the header must be the title followed by the right-aligned actions container',
  )
  assert.ok(!/dean-btn/.test(USER_ACCOUNTS), 'the separate Dean Accounts button must be gone')
  assert.ok(
    !/>\s*Dean Accounts\s*</.test(USER_ACCOUNTS),
    'no Dean Accounts control may be rendered on this page',
  )

  const header = /\.header\s*\{([^}]*)\}/.exec(USER_ACCOUNTS)
  assert.ok(header, 'the .header rule must exist')
  assert.match(header[1], /align-items:\s*center/, 'the title and the action must be centred')
  assert.match(header[1], /justify-content:\s*space-between/)
  assert.match(
    USER_ACCOUNTS,
    /\.header-actions\s*\{[^}]*margin-left:\s*auto/,
    'the actions container is what right-aligns the button',
  )
  assert.match(
    USER_ACCOUNTS,
    /@media \(max-width: 640px\)\s*\{[\s\S]*?\.header\s*\{[^}]*flex-direction:\s*column/,
    'the header must stack on a narrow screen',
  )

  // The search field and the table stay in the same page container, and the table scrolls
  // inside it instead of overflowing the page.
  assert.match(USER_ACCOUNTS, /<div class="page">[\s\S]*class="toolbar"[\s\S]*class="table-scroll"/)
  assert.match(USER_ACCOUNTS, /\.table-scroll\s*\{[^}]*overflow-x:\s*auto/)
})

test('a Dean row keeps the Dean role management, and no other row gains it', () => {
  assert.match(
    USER_ACCOUNTS,
    /v-if="isDean\(user\)"[\s\S]*@click="openDeanModal\(user\)"/,
    'only a Dean row may open the Dean role management',
  )
  assert.match(
    USER_ACCOUNTS,
    /const isDean = \(user\) => String\(user\?\.role \|\| ''\)\.toUpperCase\(\) === UserRole\.DEAN/,
    'the row action must be limited to the canonical Dean role',
  )
  assert.match(
    USER_ACCOUNTS,
    /:dean-id="selectedDeanId"/,
    'the Dean screen must be opened for the account of the clicked row',
  )

  // Search still covers the name, the e-mail and the role, which is how a Dean is found
  // in the list.
  assert.match(USER_ACCOUNTS, /u\.name \|\| ''\)\.toLowerCase\(\)\.includes\(keyword\)/)
  assert.match(USER_ACCOUNTS, /u\.email \|\| ''\)\.toLowerCase\(\)\.includes\(keyword\)/)
  assert.match(USER_ACCOUNTS, /u\.role \|\| ''\)\.toLowerCase\(\)\.includes\(keyword\)/)
})

test('the Dean screen manages an existing account and never creates one', () => {
  // The form only renders for an account that already exists, so there is exactly one
  // place in the application where a Dean account can be created: the standard form.
  assert.match(DEAN_MODAL, /<form v-if="form\.userId"/, 'the Dean form must require an account')
  assert.ok(
    !/deanPassword/.test(DEAN_MODAL),
    'the Dean screen must not ask for a password: it never creates an account',
  )
  assert.ok(
    !/Create Dean Account/.test(DEAN_MODAL),
    'the Dean screen must not offer a create action',
  )
  assert.ok(
    !/password:/.test(DEAN_MODAL),
    'the Dean screen must never send a password for an existing account',
  )
  assert.match(DEAN_MODAL, /userId: form\.userId/, 'every save must be an update')

  // Editing the scope and revoking the role are still there, so no capability was lost
  // when the page button was removed.
  assert.match(DEAN_MODAL, /@click="edit\(dean\)"/, 'the Dean scope must stay editable')
  assert.match(DEAN_MODAL, /@click="revoke\(dean\)"/, 'the Dean role must stay revocable')
})
