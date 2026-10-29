/**
 * rpsAdminDashboard.test.js
 * ---------------------------------------------------------------------------
 * Guards the RPS administrator dashboard (src/views/Dashboard/RpsAdminDashboard.vue).
 *
 * Three sections were removed from this page because the administrator manages accounts:
 * the proposal overview, the recent submissions table and the quick actions panel. The
 * screens they pointed at were NOT removed - User Accounts, the proposal pages and the
 * notifications page are all still routed and still reachable - so the risk this test
 * covers is the opposite one: that a removed section creeps back in, that the removal takes
 * a page or a route with it, or that another role's dashboard is touched.
 *
 * The files are read as text - the technique navigation.test.js and proponentDashboard.test.js
 * already use - so this needs no browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const exists = (path) => existsSync(fileURLToPath(new URL(path, import.meta.url)))

const DASHBOARD = read('../views/Dashboard/RpsAdminDashboard.vue')
const HUB = read('../views/Dashboard/Index.vue')
const ROUTER = read('../router/index.js')

test('the three removed sections are gone from the administrator dashboard', () => {
  // The labels each section rendered. None of them may come back.
  for (const removed of [
    'Proposal Overview',
    'Total Proposals',
    'Pending Review',
    'Endorsed',
    'Completed',
    'RECENT SUBMISSIONS',
    'Quick Actions',
    'Manage User Accounts',
    'View All Proposals',
  ]) {
    assert.ok(
      !DASHBOARD.includes(removed),
      `the administrator dashboard must not render "${removed}"`,
    )
  }

  // The classes only those sections used went with them, so no orphaned styling is left.
  for (const removedClass of [
    'quick-actions-panel',
    'quick-action-row',
    'hub-btn',
    'table-review-btn',
    'status-badge',
    'purple-accent',
  ]) {
    assert.ok(
      !DASHBOARD.includes(removedClass),
      `"${removedClass}" must be gone with the section that used it`,
    )
  }
})

test('the code the removed sections used is gone with them', () => {
  // Those two sections were the only readers of the proposal list, so the request and
  // everything that existed to render it are dead code on this page. The endpoint itself is
  // untouched - it is simply not called from here any more.
  assert.ok(
    !DASHBOARD.includes("api.get('/api/proposals')"),
    'the dashboard must not request the proposal list',
  )

  for (const dead of [
    'totalProposals',
    'pendingProposals',
    'endorsedProposals',
    'completedProposals',
    'recentProposals',
    'formatStatus',
    'getStatusClass',
  ]) {
    assert.ok(!DASHBOARD.includes(dead), `${dead} must not be left behind`)
  }
})

test('what the administrator dashboard still does is intact', () => {
  // The account queue: the four user statistics, the pending approvals table with its
  // approve/reject actions and its link to the full User Accounts page, the refresh action,
  // and the toast those actions report through.
  for (const kept of [
    'Total Users',
    'Pending Approvals',
    'Approved Users',
    'Rejected Users',
    'PENDING APPROVALS',
    'Refresh Data',
    'view-all-btn',
    "'/user-accounts'",
    'approveUser',
    'rejectUser',
    "api.get('/api/users')",
    'toast',
  ]) {
    assert.ok(DASHBOARD.includes(kept), `${kept} must still be part of the dashboard`)
  }
})

test('the pages the removed sections pointed at are all still routed', () => {
  // Removing a dashboard section must never take a page with it: these are the destinations
  // the removed buttons used, plus the dashboard route itself.
  for (const route of ['/user-accounts', 'rps-subproposal', 'notifications', 'rpsadmin-dash']) {
    assert.ok(ROUTER.includes(route), `the ${route} route must still exist`)
  }

  for (const view of [
    '../views/Admin/UserAccounts.vue',
    '../views/Rps/RpsSubmittedProposals.vue',
    '../views/Notifications/NotificationPage.vue',
  ]) {
    assert.ok(exists(view), `${view} must still exist`)
  }
})

test('every other role keeps its own dashboard', () => {
  // The administrator's dashboard was the only file changed, so each role must still render
  // its own component.
  for (const [role, component] of [
    ['OVCRIGE', 'OvcrigeDashboard'],
    ['PROPONENT', 'ProponentDashboard'],
    ['REC', 'RecDashboard'],
    ['RPS_STAFF', 'RpsStaffDashboard'],
    ['RPS_ADMIN', 'RpsAdminDashboard'],
    ['OVCAF', 'OvcafDashboard'],
    ['OC', 'OCDashboard'],
    ['DEAN', 'DeanDashboard'],
  ]) {
    assert.ok(
      HUB.includes(`<${component} v-if="loggedInUser.role === UserRole.${role}">`),
      `${role} must still render its own dashboard`,
    )
    assert.ok(
      exists(`../views/Dashboard/${component}.vue`),
      `${component}.vue must still exist`,
    )
  }
})
