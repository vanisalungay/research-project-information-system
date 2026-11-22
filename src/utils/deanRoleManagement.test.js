/**
 * deanRoleManagement.test.js
 * ---------------------------------------------------------------------------
 * The Dean role has a dedicated page (Dean Role Management) and the per-row Manage Dean
 * Role action on User Accounts, both reusing the existing Dean screen and endpoint.
 *
 * Pinned here: the Dean Role Management table has exactly one management action per active
 * appointment - Manage Dean - which opens the modal with the Dean's details and Replace Dean.
 * Revocation is automatic on replacement; the old separate Replace Dean / View Dean Details /
 * Revoke buttons are gone.
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
const CREATE_MODAL = read('../views/Admin/CreateUserModal.vue')
const MANAGE_MODAL = read('../views/Admin/ManageDeanModal.vue')
const FLOW = read('../views/Admin/ManageDeanFlow.vue')
const SECURITY = read('../../backend/src/main/java/com/rpis/backend/config/SecurityConfig.java')

test('the Manage Dean Role action is on User Accounts, beside its other actions', () => {
  assert.ok(USERS.includes('Manage Dean Role'), 'the Dean action is offered on the row')
  assert.ok(USERS.includes('@click="openDeanModal(user)"'), 'and opens the Dean screen')
  assert.ok(USERS.includes('ManageDeanFlow'), 'the screen it opens is the shared Manage Dean flow')
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

  // The header offers account creation through the existing form.
  assert.ok(PAGE.includes('Create Dean account'), 'the header offers Create Dean account')
  assert.ok(!PAGE.includes('Refresh'), 'the Refresh button is removed')

  // One management action per ACTIVE appointment.
  assert.ok(PAGE.includes('class="manage-btn"'), 'an ACTIVE row offers the Manage button')
  assert.ok(PAGE.includes('@click="openManage(row)"'), 'which opens the modal')
  assert.ok(PAGE.includes('ManageDeanFlow'), 'the modal is the shared Manage Dean flow')
  assert.ok(!PAGE.includes('ReplaceDeanModal'), 'the old replacement screen is not in the table')
  assert.ok(!PAGE.includes('View Dean Details'), 'the old details action is removed')
  assert.ok(!PAGE.includes('>Replace Dean<') && !PAGE.includes('>Revoke<'),
    'the old per-row actions are not in the table')

  // Revocation is automatic on replacement: the page has no standalone Revoke action.
  assert.ok(!PAGE.includes('@revoke='), 'the modal has no revoke action')
  assert.ok(!PAGE.includes('ConfirmDialog'), 'no revoke confirmation dialog remains')
  assert.ok(!PAGE.includes('api.delete('), 'the page never calls the standalone revoke endpoint')

  // The history and the replacement come from the Dean management API, never the
  // account-creation endpoint.
  assert.ok(PAGE.includes("/api/users/deans/assignments"), 'the history is read from the API')

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

  // The compact table shows the current and former appointments with the seven
  // reference columns, in order.
  const headers = ['DEAN', 'COLLEGE / DEPARTMENT', 'DESIGNATION', 'STATUS', 'ASSIGNED',
    'REVOKED', 'ACTION']
  let lastHeader = -1
  for (const column of headers) {
    const columnAt = PAGE.indexOf(`>${column}</th>`)
    assert.ok(columnAt > -1, `the table shows ${column}`)
    assert.ok(columnAt > lastHeader, `${column} keeps the reference column order`)
    lastHeader = columnAt
  }
  assert.ok(
    PAGE.includes("{ value: 'ACTIVE'") && PAGE.includes("{ value: 'REVOKED'") &&
      PAGE.includes("{ value: 'ALL'"),
    'the Active / Revoked / All filters are offered',
  )
  assert.ok(PAGE.includes('setFilter'), 'and switch the list')
  assert.ok(PAGE.includes('No matching Dean accounts found.'),
    'a search empty state is provided')
  assert.ok(PAGE.includes('No revoked Dean accounts.'), 'and a revoked empty state')
  assert.ok(PAGE.includes('Loading Dean assignments'), 'a loading state is provided')
  assert.ok(PAGE.includes('could not be loaded'), 'an error state is provided')
  assert.ok(PAGE.includes('success-banner'), 'and success feedback')
})

test('the Dean Role Management form scrolls its own fields and offers the five colleges', () => {
  assert.match(PAGE, /college-authority/, 'the page opens the form in college-authority mode')
  assert.match(CREATE_MODAL, /const deanMode = computed\(\(\) => props\.collegeAuthority\)/,
    'the Dean presentation is selected by the college-authority mode')
  assert.match(CREATE_MODAL, /id="deanCollege"/, 'a dedicated Assigned college control exists')
  assert.match(
    CREATE_MODAL,
    /<option value="" disabled>Select the college this Dean endorses for<\/option>/,
    'with the required placeholder',
  )
  assert.match(CREATE_MODAL, /v-for="college in colleges"/, 'the options are the colleges')
  assert.match(CREATE_MODAL, /RPIS_COLLEGES/, 'reusing the one college list')
  // The college is written to the field the backend DTO stores, never to the role.
  assert.match(
    CREATE_MODAL,
    /id="deanCollege"[\s\S]*?v-model="user\.assignedCollege"/,
    'the selected college is bound to assignedCollege',
  )
  // The role is fixed to the Dean account this page creates.
  assert.match(CREATE_MODAL, /id="deanRole"[\s\S]*?:value="deanRoleLabel"[\s\S]*?readonly/,
    'the Dean role is displayed as a fixed, read-only value')
  // The standard role dropdown (the User Accounts form) keeps the seven system roles.
  assert.match(CREATE_MODAL, /<option value="RPS_STAFF">/, 'the standard role list is intact')
})

test('Manage Dean is the one management action, and it opens the modal', () => {
  // The modal is opened for the clicked appointment and shows that Dean's real data.
  assert.match(PAGE, /openManage\(assignment\)/)
  assert.match(PAGE, /managedAssignment\.value = assignment/)
  assert.match(PAGE, /<ManageDeanFlow[\s\S]*?:assignment="managedAssignment"/)
  assert.match(PAGE, /@changed="onDeanReplaced"/)
  assert.ok(!PAGE.includes('@revoke='), 'replacement is the only management action')

  // Replace Dean hands off, through the shared flow, to the Create Dean Account form
  // locked to the appointment's college and identified by the appointment being replaced.
  assert.match(FLOW, /<ManageDeanModal[\s\S]*?:assignment="assignment"/)
  assert.match(FLOW, /:initial-college="assignment\.assignedCollege \|\| ''"/)
  assert.match(FLOW, /lock-college/)
  assert.match(FLOW, /:replacement-assignment-id="assignment\.id"/)

  assert.ok(!PAGE.includes('revokeTarget'), 'no standalone revoke state remains')
  assert.match(PAGE, /row\.statusKey === 'ACTIVE'/, 'only an active appointment is managed')
})

test('the redesigned page has filter counts, search and a dynamic footer', () => {
  // Filter pills carry a count badge next to each label, derived from the records.
  assert.ok(PAGE.includes('class="filter-pill"'), 'the pills are offered')
  assert.ok(PAGE.includes('class="filter-count"'), 'with a count badge')
  assert.ok(PAGE.includes('{{ filterCounts[option.value] }}'), 'and the real count')
  assert.ok(PAGE.includes('const filterCounts = computed'), 'computed from the API records')
  assert.ok(PAGE.includes('aria-pressed'), 'the selected filter is exposed to assistive tech')

  // Search covers name, email and college, case-insensitively, on the loaded records.
  assert.ok(PAGE.includes('Search name, email or college...'), 'the reference placeholder')
  assert.ok(PAGE.includes('v-model="search"'), 'the input filters as the user types')
  assert.ok(PAGE.includes("String(assignment.name || '').toLowerCase().includes(term)"))
  assert.ok(PAGE.includes("String(assignment.email || '').toLowerCase().includes(term)"))
  assert.ok(PAGE.includes("String(assignment.assignedCollege || '').toLowerCase().includes(term)"))

  // The footer count is derived, never hardcoded.
  assert.ok(PAGE.includes('Showing {{ rows.length }} of {{ filteredTotal }} dean records'))
  assert.ok(!/Showing \d+ of \d+ dean records/.test(PAGE), 'no sample count is hardcoded')

  // The Dean identity, college code, status and dates all come from real data.
  assert.ok(PAGE.includes('initialsFromName(assignment.name)'), 'initials come from the name')
  assert.ok(PAGE.includes('collegeDisplayParts(assignment.assignedCollege)'),
    'the college name/code split is shared')
  assert.ok(PAGE.includes('row.college.abbreviation'), 'the code badge is conditional')
  assert.ok(PAGE.includes('deanAssignmentStatusLabel(row.status)'), 'the status is dynamic')
  assert.ok(PAGE.includes('formatDate(assignment.dateAssigned)'),
    'assigned is the appointment date, not the account creation date')
  assert.ok(PAGE.includes('formatDate(assignment.dateRevoked)'), 'revoked is the real date')
  assert.ok(PAGE.includes("month: 'short'"), 'one consistent date format')

  // One truthful API read supplies every view; the page is unpaginated.
  assert.ok(PAGE.includes("params: { status: 'ALL' }"), 'the records are read once')
  assert.ok(PAGE.includes('class="table-card"'), 'the table lives in one rounded card')
  assert.ok(PAGE.includes('class="table-footer"'), 'the footer is part of the card')
  assert.ok(PAGE.includes('class="search-box"'), 'the search field is compact')
})

test('the table columns follow the selected filter', () => {
  // Active: the REVOKED column is dropped, the ACTION column stays.
  assert.match(PAGE, /v-if="filter !== 'ACTIVE'"[\s\S]*?REVOKED<\/th>/)
  assert.match(PAGE, /v-if="filter !== 'ACTIVE'"[\s\S]*?\{\{ row\.revokedLabel \}\}/,
    'and no revoked date is rendered for an active row')

  // Revoked: the ACTION column is dropped, the REVOKED date stays.
  assert.match(PAGE, /v-if="filter !== 'REVOKED'"[\s\S]*?ACTION<\/th>/)
  assert.match(PAGE, /v-if="filter !== 'REVOKED'"[\s\S]*?class="action-cell"/,
    'and no action cell is rendered for a revoked row')

  // All keeps both columns, and the state rows span whatever is rendered.
  assert.match(PAGE, /filter\.value === 'ALL' \? 7 : 6/)
  assert.ok(PAGE.includes(':colspan="columnCount"'), 'the state rows span the visible columns')
  assert.ok(!PAGE.includes('colspan="7"'), 'no hardcoded span remains')
  assert.match(PAGE, /row\.statusKey === 'ACTIVE'/,
    'the Manage button is still gated on an active appointment')
})

test('the Manage Dean modal contains the details and the single management action', () => {
  assert.ok(MANAGE_MODAL.includes('Manage Dean'), 'the reference title')
  assert.ok(
    MANAGE_MODAL.includes('Manage the account and assignment of this college dean.'),
    'the reference subtitle',
  )
  assert.ok(MANAGE_MODAL.includes('class="md-header-icon"'), 'the graduation-cap icon container')
  assert.ok(MANAGE_MODAL.includes('aria-label="Close manage dean"'), 'the close icon is labelled')

  // The real values, not sample data.
  assert.ok(MANAGE_MODAL.includes('initialsFromName(deanName.value)'), 'initials come from the name')
  assert.ok(MANAGE_MODAL.includes('{{ deanName || \'—\' }}'), 'the actual name is shown')
  assert.ok(MANAGE_MODAL.includes('{{ email || \'—\' }}'), 'email comes from the selection')
  assert.ok(MANAGE_MODAL.includes('collegeDisplayParts(raw)'), 'the college name/code split is shared')
  assert.ok(MANAGE_MODAL.includes('{{ designation || \'—\' }}'), 'designation is shown')
  assert.ok(MANAGE_MODAL.includes('{{ roleLabel }}'), 'role is shown')
  assert.ok(MANAGE_MODAL.includes('{{ dateAssigned }}'), 'date assigned is shown')
  assert.ok(MANAGE_MODAL.includes('/api/users/${props.assignment.userId}'), 'the account is read')

  // Replacement is the single management action; revocation is part of it.
  assert.ok(MANAGE_MODAL.includes('Replace Dean'), 'Replace Dean is offered')
  assert.ok(MANAGE_MODAL.includes('@click="$emit(\'replace\')"'), 'and emits the replacement intent')
  assert.ok(!MANAGE_MODAL.includes("$emit('revoke')"), 'no standalone revoke is offered')
  assert.ok(!MANAGE_MODAL.includes('md-revoke'), 'no revoke button is rendered')
  assert.match(MANAGE_MODAL, /class="md-action md-replace btn-primary"/)

  // The dialog contract: centred, rounded, scrollable body, accessible.
  assert.match(MANAGE_MODAL, /\.md-modal \{[\s\S]*?max-height: 90vh/)
  assert.match(MANAGE_MODAL, /\.md-body \{[\s\S]*?overflow-y: auto/)
  assert.ok(MANAGE_MODAL.includes('role="dialog"'))
  assert.ok(MANAGE_MODAL.includes('aria-modal="true"'))
  assert.ok(MANAGE_MODAL.includes('onKeydown'), 'Escape and Tab are handled')
  assert.ok(MANAGE_MODAL.includes('document.body.style.overflow = \'hidden\''),
    'background scroll is locked')
})

test('the replacement form is locked to the appointment college', () => {
  assert.match(CREATE_MODAL, /lockCollege: \{ type: Boolean, default: false \}/)
  assert.match(CREATE_MODAL, /replacementAssignmentId: \{ type: \[Number, String\], default: null \}/)
  assert.match(CREATE_MODAL, /const isReplacement = computed/)
  assert.match(CREATE_MODAL, /id="deanCollege"[\s\S]*?readonly/)
  assert.ok(CREATE_MODAL.includes('Fixed to the college of the Dean being replaced'),
    'the lock is explained')
  assert.match(CREATE_MODAL, /api\.post\('\/api\/users\/deans\/replacement'/)
  assert.match(CREATE_MODAL, /assignmentId: Number\(props\.replacementAssignmentId\)/)
  assert.match(CREATE_MODAL, /emit\('created', \{[\s\S]*?replacement: isReplacement\.value/,
    'the page knows the creation completed a replacement')
})

test('both administration screens use the one shared Manage Dean flow', () => {
  // The same components and the same replacement workflow: no screen keeps its own
  // duplicate Manage Dean implementation.
  assert.ok(USERS.includes("import ManageDeanFlow from '@/views/Admin/ManageDeanFlow.vue'"))
  assert.ok(PAGE.includes("import ManageDeanFlow from '@/views/Admin/ManageDeanFlow.vue'"))
  assert.ok(FLOW.includes("import ManageDeanModal from '@/views/Admin/ManageDeanModal.vue'"))
  assert.ok(FLOW.includes("import CreateUserModal from '@/views/Admin/CreateUserModal.vue'"))
  assert.match(FLOW, /lock-role/)
  assert.match(FLOW, /lock-college/)
  assert.match(FLOW, /college-authority/)
  assert.ok(!exists('../views/Admin/DeanAccountsModal.vue'),
    'the unused legacy Dean modal is removed rather than kept as a third implementation')
  assert.ok(!exists('../views/Admin/ReplaceDeanModal.vue'),
    'the User Accounts-only candidate picker is folded into the shared flow')
})
