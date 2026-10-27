/**
 * notifications.test.js
 * ---------------------------------------------------------------------------
 * Covers the two things the redesigned shell needs from the notification data:
 * the unread rules themselves (pure functions) and the wiring of the shell - the
 * header bell, the sidebar badge and the layout must all read the count from the
 * shared store, never from a hardcoded number, and the navigation must keep working.
 *
 * The Vue files are checked as text because the project has no DOM test harness;
 * the checks are the ones a reviewer would make by eye (same markup, same bindings).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'

import { isUnreadNotification, unreadBadgeLabel, unreadCountOf } from './notifications.js'

const read = (relativePath) => readFileSync(new URL(relativePath, import.meta.url), 'utf8')

const HEADER = read('../components/HeaderLogo.vue')
const SIDEBAR = read('../components/NavMenu.vue')
const LAYOUT = read('../layouts/DefaultLayout.vue')
const TOKENS = read('../assets/css/base.css')
const INDEX_HTML = read('../../index.html')

/** The favicon the browser tab uses - the header shows this exact file. */
const FAVICON = readFileSync(new URL('../../public/favicon.ico', import.meta.url))

/* ===== Unread rules ===== */

test('a notification is unread until its read flag is set', () => {
  assert.equal(isUnreadNotification({ isRead: false }), true)
  assert.equal(isUnreadNotification({ isRead: true }), false)
  // Some screens read the flag as `read` - both shapes must agree.
  assert.equal(isUnreadNotification({ read: false }), true)
  assert.equal(isUnreadNotification({ read: true }), false)
  // `isRead` wins when both are present.
  assert.equal(isUnreadNotification({ isRead: true, read: false }), false)
})

test('malformed entries are never counted as unread', () => {
  assert.equal(isUnreadNotification(null), false)
  assert.equal(isUnreadNotification(undefined), false)
  assert.equal(isUnreadNotification('notification'), false)
  assert.equal(isUnreadNotification(7), false)
})

test('the unread count only counts unread notifications', () => {
  assert.equal(unreadCountOf([{ isRead: false }, { isRead: true }, { isRead: false }]), 2)
  assert.equal(unreadCountOf([{ isRead: true }]), 0)
  assert.equal(unreadCountOf([]), 0)
  // A missing or unexpected payload shows no badge instead of throwing.
  assert.equal(unreadCountOf(undefined), 0)
  assert.equal(unreadCountOf(null), 0)
  assert.equal(unreadCountOf({ unread: 3 }), 0)
})

test('the badge label is empty at zero and capped above 99', () => {
  assert.equal(unreadBadgeLabel(0), '')
  assert.equal(unreadBadgeLabel(-4), '')
  assert.equal(unreadBadgeLabel(1), '1')
  assert.equal(unreadBadgeLabel(9), '9')
  assert.equal(unreadBadgeLabel(99), '99')
  assert.equal(unreadBadgeLabel(100), '99+')
  assert.equal(unreadBadgeLabel(1234), '99+')
  assert.equal(unreadBadgeLabel('3'), '3')
  assert.equal(unreadBadgeLabel('nope'), '')
  assert.equal(unreadBadgeLabel(undefined), '')
})

/* ===== Shell tokens ===== */

test('the shell colours come from the shared tokens', () => {
  const expected = {
    '--rpis-header-bg': '#262342',
    '--rpis-sidebar-bg': '#1c1a3c',
    '--rpis-accent': '#ffd000',
    '--rpis-on-dark-soft': '#c8c8e8',
    '--rpis-on-dark-muted': '#9999c4',
    '--rpis-help-bg': '#2d2a5b',
    '--rpis-help-border': '#403b75',
    '--rpis-badge': '#315bff',
    '--rpis-content-bg': '#f5f5fc',
    '--rpis-divider': '#383553',
    '--rpis-separator': '#464260',
    '--rpis-sidebar-width': '261px',
    '--rpis-header-height': '67px',
  }

  for (const [token, value] of Object.entries(expected)) {
    assert.match(TOKENS, new RegExp(`${token}: ${value};`), `${token} should be ${value}`)
  }
})

test('the header and the sidebar use the tokens instead of their own colours', () => {
  assert.match(HEADER, /background: var\(--rpis-header-bg\)/)
  assert.match(HEADER, /border-bottom: var\(--rpis-header-border-height\) solid var\(--rpis-header-border\)/)
  assert.match(SIDEBAR, /background: var\(--rpis-sidebar-bg\)/)
  assert.match(SIDEBAR, /background: var\(--rpis-accent\)/)
})

/* ===== Layout ===== */

test('the layout stacks a full-width header above the sidebar and the content', () => {
  assert.match(LAYOUT, /<HeaderLogo class="app-header" :collapsed="sidebarCollapsed" @toggle-sidebar="toggleSidebar" \/>/)
  assert.match(LAYOUT, /<aside class="app-sidebar" :class="\{ 'is-collapsed': sidebarCollapsed \}">/)
  assert.match(LAYOUT, /<NavMenu :collapsed="sidebarCollapsed" \/>/)
  assert.match(LAYOUT, /<main class="app-content">/)

  // The header row and the sidebar column keep the reference proportions and the
  // content region is the only part that scrolls.
  assert.match(LAYOUT, /\.app-sidebar \{\s*width: var\(--rpis-sidebar-width\)/)
  assert.match(LAYOUT, /\.app-sidebar\.is-collapsed \{\s*width: var\(--rpis-sidebar-collapsed-width\)/)
  assert.match(LAYOUT, /\.app-content \{[\s\S]*?overflow-y: auto;/)
  assert.doesNotMatch(LAYOUT, /169px/)
})

test('the sidebar toggle is the only collapse mechanism and keeps the route state', () => {
  assert.match(LAYOUT, /const toggleSidebar = \(\) => \{\s*sidebarCollapsed\.value = !sidebarCollapsed\.value/)
  assert.match(LAYOUT, /matchMedia\(COMPACT_LAYOUT\)/)
  // Toggling only changes the rail; it never navigates.
  assert.doesNotMatch(LAYOUT, /toggleSidebar[\s\S]{0,80}router\.push/)
})

test('the unread count is refreshed from the shared store', () => {
  assert.match(LAYOUT, /useNotificationStore/)
  assert.match(LAYOUT, /notificationStore\.refresh\(userId\)/)
  assert.match(LAYOUT, /route\.fullPath/)
})

/* ===== Header ===== */

test('the header keeps the hamburger, the brand, the bell and the account control', () => {
  assert.match(HEADER, /aria-label="Toggle sidebar"/)
  assert.match(HEADER, /@click="\$emit\('toggle-sidebar'\)"/)
  assert.match(HEADER, /<img class="brand-logo" src="\/favicon.ico" alt="" aria-hidden="true" \/>/)
  assert.match(HEADER, /<span class="brand-research">Research<\/span><span class="brand-project">Project<\/span>/)
  assert.match(HEADER, /class="brand-subtitle">Information System</)
  assert.match(HEADER, /class="header-separator"/)
})

test('the header logo is the system favicon, not a drawn mark', () => {
  // The browser tab and the header point at the same asset.
  assert.match(INDEX_HTML, /<link rel="icon" href="\/favicon\.ico">/)
  assert.match(HEADER, /src="\/favicon\.ico"/)

  // The old circular "RP" mark is gone from the markup and from the styles.
  assert.doesNotMatch(HEADER, />\s*RP\s*</)
  assert.doesNotMatch(HEADER, /\.brand-logo\s*\{[^}]*border-radius/)
  assert.doesNotMatch(HEADER, /\.brand-logo\s*\{[^}]*background/)
  assert.doesNotMatch(HEADER, /\.brand-logo\s*\{[^}]*border:/)
  assert.doesNotMatch(HEADER, /\.brand-logo\s*\{[^}]*font-size/)

  // The image keeps its proportions and the header keeps its size.
  assert.match(HEADER, /\.brand-logo \{[\s\S]*?width: 44px;[\s\S]*?height: 44px;/)
  assert.match(HEADER, /\.brand-logo \{[\s\S]*?object-fit: contain;/)

  // The shipped asset really is an icon file (it is what the tab loads).
  assert.equal(FAVICON.readUInt16LE(0), 0)
  assert.equal(FAVICON.readUInt16LE(2), 1)
  assert.ok(FAVICON.readUInt16LE(4) >= 1)
})

test('the notification control shows real data and opens the existing page', () => {
  assert.match(HEADER, /const openNotifications = \(\) => \{\s*router\.push\('\/notifications'\)/)
  assert.match(HEADER, /v-if="notificationStore\.hasUnread"/)
  assert.match(HEADER, /notificationStore\.unreadCount/)
  // No hardcoded badge: the dot is the only indicator and it is conditional.
  assert.doesNotMatch(HEADER, /class="notif-dot"[^>]*>\s*\d/)
})

test('the account control shows the signed-in user and keeps the dropdown', () => {
  assert.match(HEADER, /\{\{ userStore\.user\?\.name \?\? 'Guest' \}\}/)
  assert.match(HEADER, /\{\{ formatRole\(userStore\.user\?\.role\) \}\}/)
  assert.match(HEADER, /v-for="menuItem in menuItems"/)
  assert.match(HEADER, /case 'LOGOUT':/)
  // The example account in the reference is never hardcoded.
  assert.doesNotMatch(HEADER, /Proponent User/)
})

/* ===== Sidebar ===== */

test('the sidebar keeps the reference structure', () => {
  assert.match(SIDEBAR, /<p class="menu-label">Menu<\/p>/)
  assert.match(SIDEBAR, /v-for="item in mainNavItems"/)
  assert.match(SIDEBAR, /v-if="badgeFor\(item\)" class="nav-badge"/)
  assert.match(SIDEBAR, /v-if="showHelpCard" class="help-card"/)
  assert.match(SIDEBAR, /class="help-title">Need a hand\?</)
  assert.match(SIDEBAR, /Check your proposal status or reach the research office\./)
  assert.match(SIDEBAR, /class="nav-divider"/)
  assert.match(SIDEBAR, /class="nav-link profile-link"/)
})

test('the help card is limited to proponents', () => {
  // The condition uses the real role identifier of the signed-in user, not a label.
  assert.match(SIDEBAR, /const HELP_CARD_ROLES = \[UserRole\.PROPONENT\]/)
  assert.match(
    SIDEBAR,
    /const showHelpCard = computed\(\s*\(\) => !props\.collapsed && HELP_CARD_ROLES\.includes\(userStore\.user\?\.role\),\s*\)/,
  )

  // The card is the only thing the condition controls, so every other role keeps the
  // divider and the Profile Settings button in the same place.
  assert.equal((SIDEBAR.match(/v-if="showHelpCard"/g) || []).length, 1)
  assert.match(SIDEBAR, /<\/div>\s*<div class="nav-divider" aria-hidden="true"><\/div>/)
  assert.doesNotMatch(SIDEBAR, /v-if="!collapsed" class="help-card"/)

  // The role exists in the store with exactly this identifier.
  const USER_STORE = read('../stores/userData.js')
  assert.match(USER_STORE, /PROPONENT: 'PROPONENT'/)
  assert.match(USER_STORE, /role: UserRole\.PROPONENT/)
})

test('the sidebar badge is driven by the shared unread count', () => {
  assert.match(SIDEBAR, /unreadBadgeLabel\(notificationStore\.unreadCount\)/)
  assert.match(SIDEBAR, /item\.text === 'Notifications'/)
  assert.doesNotMatch(SIDEBAR, /class="nav-badge"[^>]*>\s*\d/)
})

test('every role keeps its navigation items and the role filter', () => {
  assert.match(SIDEBAR, /allNavItems\.filter\(item => item\.roles\.includes\(userStore\.user\.role\)\)/)

  for (const path of [
    '/home',
    '/proposals',
    '/notifications',
    '/proponent-proposal-monitoring',
    '/rps-proposal-monitoring',
    '/rps-subproposal',
    '/rps-quarterly-reports',
    '/user-accounts',
    '/proposal-announcements',
    '/budget-processing',
    '/endorsed-proposals',
    '/ovc-quarterly-reports',
    '/ovcaf/inbox',
    '/ovcaf/reports',
    '/assigned-proposals',
    '/manage-profile',
  ]) {
    assert.match(SIDEBAR, new RegExp(`path: '${path.replace(/\//g, '\\/')}'`), `${path} is missing`)
  }
})

test('the profile button is the only yellow control and the collapse keeps it usable', () => {
  assert.match(SIDEBAR, /\.profile-item \{[\s\S]*?background: var\(--rpis-accent\)/)
  // The collapsed rail drops the row text (it is grouped with the label and the card).
  assert.match(SIDEBAR, /\.sidebar-nav\.is-collapsed \.nav-text,[\s\S]*?display: none;/)
  assert.match(SIDEBAR, /\.sidebar-nav\.is-collapsed \.profile-link \{\s*justify-content: center;/)
  // Icon-only rows stay identifiable through the title attribute.
  assert.match(SIDEBAR, /:title="collapsed \? item\.text : null"/)
})
