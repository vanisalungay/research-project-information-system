/**
 * userAccountApprovalUi.test.js
 * ---------------------------------------------------------------------------
 * Guards the three pieces of the RPS Administration account-review work:
 *
 *   1. The dashboard's Pending Approvals table renders Approve and Reject as one
 *      equally-sized control (the shorter label used to make Reject smaller).
 *   2. The pending-account dialog in User Accounts is wired to the existing
 *      PUT /api/users/{id}/approve and /reject endpoints, blocks a second click
 *      while a decision is in flight, and reloads the list from the server after
 *      the decision so the table shows the persisted status.
 *   3. The User Accounts page filters by the stored account status, in the same
 *      pass as its search and its pagination.
 *
 * The SFCs are read as text - the technique the other admin tests use - so this
 * needs no browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const ACCOUNTS = read('../views/Admin/UserAccounts.vue')
const DASHBOARD = read('../views/Dashboard/RpsAdminDashboard.vue')

test('the dashboard gives Approve and Reject the same compact box', () => {
  // One rule for both buttons, so neither label can render a different size.
  const shared = /\.approve-btn,\s*\.reject-btn\s*\{([\s\S]*?)\n\}/.exec(DASHBOARD)
  assert.ok(shared, 'Approve and Reject must share one sizing rule')
  assert.match(shared[1], /width:\s*75px/, 'the exact same width on both')
  assert.match(shared[1], /height:\s*28px/, 'the exact same height on both')
  assert.match(shared[1], /padding:\s*0 6px/, 'equal padding')
  assert.match(shared[1], /margin:\s*0/, 'aligned despite the legacy global reject-button margin')
  assert.match(shared[1], /border-radius:\s*9px/, 'equal radius')
  assert.match(shared[1], /font-size:\s*var\(--ui-control-font-sm\)/, 'equal type')
  assert.match(shared[1], /font-weight:\s*var\(--ui-weight-semibold\)/, 'equal weight')
  assert.match(shared[1], /justify-content:\s*center/, 'the label is centred in the box')
  assert.match(shared[1], /box-sizing:\s*border-box/)

  // The reference pair: filled green Approve, outlined red Reject.
  assert.match(DASHBOARD, /\.approve-btn\s*\{[^}]*background:\s*#047857/)
  assert.match(DASHBOARD, /\.reject-btn\s*\{[^}]*background:\s*#ffffff[^}]*border:\s*1px solid #fecaca[^}]*color:\s*#dc2626/)
  assert.match(DASHBOARD, /\.action-btns\s*\{[^}]*gap:\s*9px/)
  assert.match(DASHBOARD, /\.action-btns\s*\{[^}]*justify-content:\s*flex-end/)

  // The pair keeps its real handlers and is disabled while one decision is in flight.
  assert.match(DASHBOARD, /<button[\s\S]{0,500}class="approve-btn"[\s\S]{0,500}@click="approveUser\(user\)"/)
  assert.match(DASHBOARD, /<button[\s\S]{0,500}class="reject-btn"[\s\S]{0,500}@click="rejectUser\(user\)"/)
  assert.match(DASHBOARD, /:disabled="actionLoading"/, 'a decision in flight disables both')
  assert.match(DASHBOARD, /isActing\(user\.id, 'approve'\)/, 'the acting row shows the spinner')
  assert.match(DASHBOARD, /isActing\(user\.id, 'reject'\)/)
})

test('the dashboard pending card follows the compact reference layout', () => {
  // One card with a header: title, live count badge, View All text link.
  assert.match(DASHBOARD, /<section class="pending-panel" aria-labelledby="pending-approvals-title">/)
  assert.match(DASHBOARD, /id="pending-approvals-title" class="pending-title">Pending Approvals/)
  assert.match(DASHBOARD, /class="pending-count"[\s\S]{0,200}\{\{ pendingUsers \}\}/)
  assert.match(DASHBOARD, /<router-link to="\/user-accounts" class="view-all-btn">/)
  assert.match(DASHBOARD, /class="view-all-icon"/)

  // The five reference columns, in order.
  const applicant = DASHBOARD.indexOf('>Applicant</th>')
  const email = DASHBOARD.indexOf('>Email Address</th>')
  const role = DASHBOARD.indexOf('>Role</th>')
  const registered = DASHBOARD.indexOf('>Registered</th>')
  const actions = DASHBOARD.indexOf('>Actions</th>')
  assert.ok(applicant > -1, 'Applicant column')
  assert.ok(applicant < email && email < role && role < registered && registered < actions,
    'the columns keep the reference order')

  // Real values: initials from the name, email, role, registration date.
  assert.match(DASHBOARD, /class="avatar" aria-hidden="true">\{\{ initialsOf\(user\.name\) \}\}/)
  assert.match(DASHBOARD, /const initialsOf = \(name\) =>/)
  assert.match(DASHBOARD, /\{\{ user\.email \|\| 'N\/A' \}\}/)
  assert.match(DASHBOARD, /class="role-pill">\{\{ formatRole\(user\.role\) \}\}/)
  assert.match(DASHBOARD, /\{\{ formatDate\(user\.dateRegistered\) \}\}/)

  // The compact, flat card treatment from the reference.
  assert.match(DASHBOARD, /\.pending-panel\s*\{[^}]*border:\s*1px solid #dce5f0[^}]*border-radius:\s*14px/)
  assert.match(DASHBOARD, /\.pending-header\s*\{[^}]*padding:\s*14px 18px[^}]*border-bottom:\s*1px solid #e8edf4/)
  assert.match(DASHBOARD, /\.avatar\s*\{[^}]*width:\s*27px[^}]*height:\s*27px/)
  assert.match(DASHBOARD, /\.pending-table\s*\{[^}]*min-width:\s*900px/)
  assert.match(DASHBOARD, /\.pending-table-scroll\s*\{[^}]*overflow-x:\s*auto/)
})

test('the dashboard actions still call the real approval endpoints', () => {
  assert.match(DASHBOARD, /api\.put\(`\/api\/users\/\$\{user\.id\}\/approve`\)/)
  assert.match(DASHBOARD, /api\.put\(`\/api\/users\/\$\{user\.id\}\/reject`\)/)
  assert.match(DASHBOARD, /user\.status = 'APPROVED'/)
  assert.match(DASHBOARD, /user\.status = 'REJECTED'/)
})

test('the pending-account dialog is bound to the existing approve and reject endpoints', () => {
  // The buttons inside the modal actually invoke the handlers...
  assert.match(ACCOUNTS, /@click="approvePending"/)
  assert.match(ACCOUNTS, /@click="rejectPending"/)

  // ...and the handlers call the same endpoints the dashboard uses.
  assert.match(ACCOUNTS, /api\.put\(`\/api\/users\/\$\{pendingUser\.value\.id\}\/approve`\)/)
  assert.match(ACCOUNTS, /api\.put\(`\/api\/users\/\$\{pendingUser\.value\.id\}\/reject`\)/)

  // A second click while the request is in flight is ignored, not queued.
  assert.match(ACCOUNTS, /if \(!pendingUser\.value \|\| pendingAction\.value\) return/)

  // The in-flight state is a non-empty action name, or ''. It must be converted to a
  // boolean: Vue renders a bound boolean attribute for the empty string as PRESENT
  // (`includeBooleanAttr('') === true`), so the old bare binding disabled every button
  // while the dialog was idle - which is why Approve and Reject did nothing.
  const pendingDisabled = [...ACCOUNTS.matchAll(/:disabled="([^"]*pendingAction[^"]*)"/g)].map(
    (match) => match[1],
  )
  assert.ok(pendingDisabled.length >= 3, 'the in-flight state must gate all decisions')
  assert.ok(
    pendingDisabled.every((binding) => binding === "pendingAction !== ''"),
    `every binding must compare against '', found: ${pendingDisabled.join(' / ')}`,
  )
  assert.ok(
    !/:disabled="pendingAction"/.test(ACCOUNTS),
    'the bare string binding would disable the buttons while the dialog is idle',
  )
})

test('the dialog reloads the stored state and reports the outcome', () => {
  // Both decisions close the dialog and re-read the list, so the row's new status is the
  // one the backend persisted - never a frontend-only edit.
  assert.match(
    ACCOUNTS,
    /closePendingModal\(true\)[\s\S]{0,80}await loadUsers\(\)/,
    'the list is reloaded from the server after the decision',
  )
  assert.ok(
    (ACCOUNTS.match(/await loadUsers\(\)/g) || []).length >= 2,
    'approval and rejection both reload',
  )
  assert.match(ACCOUNTS, /actionNotice\.value = \{ type: 'success'/, 'approval reports success')
  assert.match(ACCOUNTS, /actionNotice\.value = \{ type: 'error'/, 'rejection reports the rejection')
  assert.match(ACCOUNTS, /pendingError\.value = err\?\.response\?\.data\?\.error/,
    'a refused decision shows the server reason')

  // The reloaded list is the only source of the rows.
  assert.match(ACCOUNTS, /res = await api\.get\('\/api\/users'\)/)
})

test('the page filters by the stored account status in the same pass as the search', () => {
  // The option list is built from the loaded rows and the shared status labels.
  assert.match(ACCOUNTS, /accountStatusFilterOptions\(users\.value\)/)
  assert.match(ACCOUNTS, /matchesAccountStatus\(u, statusFilter\.value\)/)
  assert.match(ACCOUNTS, /watch\(\[search, statusFilter\], resetPage\)/,
    'a new status starts the result set at its first page')

  // One computed list feeds the table AND the pagination, so a filter cannot show a row
  // the pager did not count.
  assert.match(ACCOUNTS, /usePagination\(filteredUsers\)/)
  assert.match(ACCOUNTS, /v-for="user in visibleUsers"/)
  assert.match(ACCOUNTS, /:total="filteredUsers\.length"/)
})

test('the filter is a labelled group of real status pills', () => {
  assert.match(ACCOUNTS, /role="group" aria-label="Filter accounts by status"/)
  assert.match(ACCOUNTS, /All Accounts/)
  assert.match(ACCOUNTS, /v-for="option in statusOptions"/)
  assert.match(ACCOUNTS, /:class="\{ active: statusFilter === '' \}"/, 'All Accounts is the empty selection')
  assert.match(ACCOUNTS, /:aria-pressed="statusFilter === option\.value"/)
  assert.match(ACCOUNTS, /class="filter-count"/, 'each pill shows its real count')
})

test('the empty table explains an empty filter result, not a missing list', () => {
  assert.match(ACCOUNTS, /\{\{ emptyMessage \}\}/)
  assert.match(ACCOUNTS, /No accounts match the selected search and status filter\./)
})
