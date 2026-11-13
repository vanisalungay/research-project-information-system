/**
 * deanRoleManagement.test.js
 * ---------------------------------------------------------------------------
 * The Dean role has a dedicated page (Dean Role Management) and the per-row Manage Dean
 * Role action on User Accounts, both reusing the existing Dean screen and endpoint.
 *
 * Pinned here: User Accounts offers Manage Dean Role beside Reset Password on a Dean's
 * row, the Dean account is still listed there, the dedicated page is an administrator page
 * in the sidebar, it is role-restricted on both the route and the backend, and it reuses
 * the existing Dean screen and endpoint instead of reimplementing them.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const exists = (path) => existsSync(fileURLToPath(new URL(path, import.meta.url)))

const NAV = read('../components/NavMenu.vue')
const ROUTER = read('../router/index.js')
const USERS = read('../views/Admin/UserAccounts.vue')
const PAGE = read('../views/Admin/DeanRoleManagement.vue')
const MODAL = read('../views/Admin/DeanAccountsModal.vue')
const CREATE_MODAL = read('../views/Admin/CreateUserModal.vue')
const REPLACE_MODAL = read('../views/Admin/ReplaceDeanModal.vue')
const SECURITY = read('../../backend/src/main/java/com/rpis/backend/config/SecurityConfig.java')

test('the Manage Dean Role action is on User Accounts, beside its other actions', () => {
  assert.ok(USERS.includes('Manage Dean Role'), 'the Dean action is offered on the row')
  assert.ok(USERS.includes('@click="openDeanModal(user)"'), 'and opens the Dean screen')
  assert.ok(USERS.includes('ReplaceDeanModal'), 'the screen it opens is the replacement screen')
  assert.ok(USERS.includes('dean-manage-btn'), 'with its own button style')
  // Reset Password is untouched and still offered for every row, the Dean's included.
  assert.ok(USERS.includes('Reset Password'), 'Reset Password stays')
  assert.ok(USERS.includes('openResetModal'), 'and still works')
})

test('the Dean account is still listed as an official account in User Accounts', () => {
  assert.ok(USERS.includes("'DEAN': 'Dean'"), 'the Dean role is still labelled in the table')
  assert.ok(USERS.includes('filteredUsers'), 'the Dean is still part of the ordinary list')
  assert.ok(
    !/role\s*!==\s*UserRole\.DEAN/.test(USERS),
    'and is not filtered out of User Management',
  )
})

test('the administrator sidebar carries the new item, next to User Accounts', () => {
  const item = NAV.match(/text: 'Dean Role Management',[\s\S]*?section: 'main',/)
  assert.ok(item, 'the Dean Role Management navigation item must exist')
  assert.ok(item[0].includes("path: '/dean-role-management'"), 'it points at its own route')
  assert.ok(item[0].includes('roles: [UserRole.RPS_ADMIN]'), 'it is an administrator item')
  assert.ok(!item[0].includes('RPS_STAFF'), 'and is not offered to ORPS Staff')

  const usersItem = NAV.indexOf("text: 'User Accounts'")
  const deanItem = NAV.indexOf("text: 'Dean Role Management'")
  assert.ok(usersItem > -1 && deanItem > -1, 'both items are declared')
  assert.ok(deanItem > usersItem, 'the Dean item follows User Accounts')
  assert.ok(deanItem - usersItem < 600, 'and sits right next to it')

  assert.ok(/deanRole:\s*`<svg/.test(NAV), 'the item uses a role-assignment icon')
})

test('the page is routed and its route is restricted to the administrator', () => {
  assert.ok(
    ROUTER.includes("path: '/dean-role-management'"),
    'the dedicated route must be registered',
  )
  assert.ok(
    ROUTER.includes("component: DeanRoleManagement"),
    'and must load the dedicated page',
  )
  const rule = ROUTER.match(
    /match: \(path\) => path\.startsWith\('\/dean-role-management'\),\s*\n\s*roles: \[([^\]]+)\]/,
  )
  assert.ok(rule, 'the /dean-role-management route must be role-restricted')
  assert.equal(rule[1].trim(), 'UserRole.RPS_ADMIN', 'and to the administrator only')
})

test('the backend keeps the Dean operations to the administrator', () => {
  assert.match(
    SECURITY,
    /"\/api\/users\/deans", "\/api\/users\/deans\/\*\*"\)\s*\.hasRole\(UserRoles\.RPS_ADMIN\)/,
    'the Dean endpoints must stay administrator-only',
  )
})

test('the page lists Dean appointments (current and former) and reuses existing pieces', () => {
  assert.ok(exists('../views/Admin/DeanRoleManagement.vue'), 'the page exists')
  assert.ok(PAGE.includes('Dean Role Management'), 'it carries the page title')
  assert.ok(
    PAGE.includes(
      'Manage Dean accounts, departmental assignments, and their designated academic units.',
    ),
    'and the required description',
  )

  // The header offers account creation through the existing form; the replacement action
  // lives on every ACTIVE row.
  assert.ok(PAGE.includes('+ Create Dean Account'), 'the header offers Create Dean Account')
  assert.ok(!PAGE.includes('Refresh'), 'the Refresh button is removed')
  assert.ok(PAGE.includes('Manage Dean Role'), 'an ACTIVE row offers Manage Dean Role')
  assert.ok(PAGE.includes('openManage'), 'which opens the replacement screen')
  assert.ok(PAGE.includes('ReplaceDeanModal'), 'the replacement screen is reused')

  // The history and the replacement come from the Dean management API, never the
  // account-creation endpoint.
  assert.ok(PAGE.includes("/api/users/deans/assignments"), 'the history is read from the API')
  assert.ok(
    !PAGE.includes('/api/users/admin/create'),
    'the page does not call the account-creation endpoint itself',
  )

  // The account is created by the existing form, through the existing endpoint.
  assert.ok(
    PAGE.includes('CreateUserModal from') && PAGE.includes('<CreateUserModal'),
    'the standard Create Account form is reused',
  )
  assert.ok(PAGE.includes('initial-role="DEAN"'), 'opened on the Dean role')
  assert.ok(PAGE.includes('lock-role'), 'and locked to it')
  assert.ok(PAGE.includes('onDeanCreated'), 'the list reloads after a creation')
  assert.ok(
    CREATE_MODAL.includes("api.post('/api/users/admin/create'"),
    'the reused form uses the one account-creation endpoint',
  )
  assert.ok(CREATE_MODAL.includes('temporaryPassword'), 'which returns the credentials once')
  assert.ok(CREATE_MODAL.includes('cred-password'), 'and displays the temporary password')
  assert.ok(CREATE_MODAL.includes('RPIS_COLLEGES'), 'with the real college list')

  // The table shows the current and former appointments with the required columns.
  for (const column of ['Dean Name', 'Email', 'Assigned College / Department', 'Designation',
    'Status', 'Date Assigned', 'Date Revoked', 'Actions']) {
    assert.ok(PAGE.includes(`<th>${column}</th>`), `the table shows ${column}`)
  }
  assert.ok(PAGE.includes('View Dean Details'), 'the details action stays on every row')
  assert.ok(
    PAGE.includes("{ value: 'ACTIVE'") && PAGE.includes("{ value: 'REVOKED'") &&
      PAGE.includes("{ value: 'ALL'"),
    'the Active / Revoked / All filters are offered',
  )
  assert.ok(PAGE.includes('setFilter'), 'and switch the list')
  assert.ok(PAGE.includes('No Dean accounts yet'), 'an empty state is provided')
  assert.ok(PAGE.includes('Loading Dean assignments'), 'a loading state is provided')
  assert.ok(PAGE.includes('could not be loaded'), 'an error state is provided')
  assert.ok(PAGE.includes('success-banner'), 'and success feedback')
})

test('the Dean Role Management form offers the five colleges under User Role Authority', () => {
  assert.match(PAGE, /college-authority/, 'the page opens the form in college-authority mode')
  assert.match(CREATE_MODAL, /v-if="collegeAuthority"/, 'the form has a college-authority control')
  assert.match(CREATE_MODAL, /id="collegeAuthority"/, 'a dedicated college control exists')
  assert.match(
    CREATE_MODAL,
    /<option value="" disabled>Select College<\/option>/,
    'with a Select College placeholder',
  )
  assert.match(CREATE_MODAL, /v-for="college in colleges"/, 'the options are the colleges')
  assert.match(CREATE_MODAL, /RPIS_COLLEGES/, 'reusing the one college list')
  // The college is written to the field the backend DTO stores, never to the role.
  assert.match(
    CREATE_MODAL,
    /id="collegeAuthority"[\s\S]*?v-model="user\.assignedCollege"/,
    'the selected college is bound to assignedCollege',
  )
  // The standard role dropdown (the User Accounts form) keeps the seven system roles.
  assert.match(CREATE_MODAL, /<option value="RPS_STAFF">/, 'the standard role list is intact')
})

test('the replacement screen limits candidates to the faculty of the managed unit', () => {
  assert.match(
    REPLACE_MODAL,
    /No eligible replacement Dean found/,
    'a clear empty state is offered when nobody is eligible',
  )
  assert.match(
    REPLACE_MODAL,
    /\/api\/users\/deans\/candidates/,
    'candidates come from the Dean management API',
  )
  assert.match(
    REPLACE_MODAL,
    /college: college\.value/,
    'the request is scoped to the unit being managed',
  )
  assert.match(
    REPLACE_MODAL,
    /candidate\.name }} — \{\{ candidate\.email/,
    'each candidate shows its full name and institutional email',
  )
})
