<template>
  <nav class="sidebar-nav" :class="{ 'is-collapsed': collapsed }">
    <!-- Section label -->
    <p class="menu-label">Menu</p>

    <!-- Main Navigation Items -->
    <ul class="nav-list">
      <li v-for="item in mainNavItems" :key="item.path" :class="{ active: isSelected(item.path) }">
        <router-link :to="item.path" class="nav-link" :title="collapsed ? item.text : null">
          <span class="nav-icon" v-html="getIcon(item.iconKey)"></span>
          <span class="nav-text">{{ item.text }}</span>
          <!-- The unread count is real data (same source as the notifications page). -->
          <span v-if="badgeFor(item)" class="nav-badge">{{ badgeFor(item) }}</span>
        </router-link>
      </li>
    </ul>

    <!-- Bottom Section: help card, divider, Profile Settings -->
    <div class="nav-bottom">
      <!-- The help card describes the proponent workflow, so only proponents see it. -->
      <div v-if="showHelpCard" class="help-card">
        <p class="help-title">Need a hand?</p>
        <p class="help-text">Check your proposal status or reach the research office.</p>
      </div>

      <div class="nav-divider" aria-hidden="true"></div>

      <ul class="nav-list">
        <li v-for="item in bottomNavItems" :key="item.path" class="profile-item"
          :class="{ active: isSelected(item.path) }">
          <router-link :to="item.path" class="nav-link profile-link" :title="collapsed ? item.text : null">
            <span class="nav-icon" v-html="getIcon(item.iconKey)"></span>
            <span class="nav-text">{{ item.text }}</span>
          </router-link>
        </li>
      </ul>
    </div>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'
import { useNotificationStore } from '@/stores/notifications'
import { unreadBadgeLabel } from '@/utils/notifications'

const props = defineProps({
  /** True while the shell shows the narrow icon rail instead of the full sidebar. */
  collapsed: { type: Boolean, default: false },
})

const route = useRoute()
const userStore = useUserDataStore()
const notificationStore = useNotificationStore()

// SVG icon definitions
const icons = {
  dashboard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>`,
  notification: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.73 21a2 2 0 01-3.46 0"/></svg>`,
  proposals: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>`,
  endorse: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 11-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>`,
  budget: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="12" y1="1" x2="12" y2="23"/><path d="M17 5H9.5a3.5 3.5 0 000 7h5a3.5 3.5 0 010 7H6"/></svg>`,
  monitoring: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"/><line x1="12" y1="20" x2="12" y2="4"/><line x1="6" y1="20" x2="6" y2="14"/></svg>`,
  users: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>`,
  revisions: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>`,
  evalCriteria: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>`,
  settings: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/></svg>`,
  review: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>`,
  calendar: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>`,
}

const getIcon = (key) => icons[key] || icons.dashboard

const allNavItems = [
  // Shared: Dashboard
  {
    iconKey: 'dashboard',
    text: 'Dashboard',
    path: '/home',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF, UserRole.PROPONENT, UserRole.DEAN, UserRole.OVCAF, UserRole.OVCRIGE, UserRole.REC, UserRole.OC],
    section: 'main',
  },

  {
    iconKey: 'proposals',
    text: 'Submitted Proposals',
    path: '/rps-subproposal',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },

  // RPS ADMIN
  {
    iconKey: 'users',
    text: 'User Accounts',
    path: '/user-accounts',
    roles: [UserRole.RPS_ADMIN],
    section: 'main',
  },
  {
    iconKey: 'calendar',
    text: 'Proposal Announcements',
    path: '/proposal-announcements',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'evalCriteria',
    text: 'Priority Agendas',
    path: '/priority-agendas',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.RPS_ADMIN],
    section: 'main',
  },

  // RPS STAFF
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },

  {
    iconKey: 'budget',
    text: 'Budget Processing',
    path: '/budget-processing',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'monitoring',
    text: 'Proposal Monitoring',
    path: '/rps-proposal-monitoring',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'monitoring',
    text: 'Quarterly Reports',
    path: '/rps-quarterly-reports',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF],
    section: 'main',
  },

  // Dean / Department Chair: the whole Dean workflow in three items - the dashboard
  // (the shared /home item above, which renders the Dean dashboard for this role), the
  // endorsement requests of their own college, and notifications. There is deliberately
  // no Endorsement Letter Settings entry: the ORPS Director is no longer configured, so
  // nothing has to be set up before a request can be reviewed or decided.
  {
    iconKey: 'endorse',
    text: 'Endorsement Requests',
    path: '/dean-requests',
    roles: [UserRole.DEAN],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.DEAN],
    section: 'main',
  },

  // PROPONENT
  {
    iconKey: 'proposals',
    text: 'My Projects',
    path: '/proposals',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },
  {
    // One item for the whole endorsement workflow. The consolidated My Endorsements page
    // holds the request form, the tracking list and the issued letters as sections, so
    // there is deliberately no second item for any of them.
    iconKey: 'endorse',
    text: 'My Endorsements',
    path: '/endorsements',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },

  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },
  {
    iconKey: 'monitoring',
    text: 'Proposal Monitoring',
    path: '/proponent-proposal-monitoring',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },

  // OVCRIGE
  {
    iconKey: 'endorse',
    text: 'Endorsed Proposals',
    path: '/endorsed-proposals',
    roles: [UserRole.OVCRIGE],
    section: 'main',
  },
  {
    iconKey: 'monitoring',
    text: 'Quarterly Reports',
    path: '/ovc-quarterly-reports',
    roles: [UserRole.OVCRIGE],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OVCRIGE],
    section: 'main',
  },

  // OVCAF
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OVCAF],
    section: 'main',
  },
  {
    iconKey: 'review',
    text: 'Inbox',
    path: '/ovcaf/inbox',
    roles: [UserRole.OVCAF],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Financial Reports',
    path: '/ovcaf/reports',
    roles: [UserRole.OVCAF],
    section: 'main',
  },

  // REC
  {
    iconKey: 'proposals',
    text: 'Assigned Proposals',
    path: '/assigned-proposals',
    roles: [UserRole.REC],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.REC],
    section: 'main',
  },

  // OC
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OC],
    section: 'main',
  },

  // Shared: Profile Settings (bottom section). Every role keeps it, the Dean included:
  // Manage Profile -> Electronic Signature is where a Dean saves the signature the
  // official endorsement letter is signed with.
  {
    iconKey: 'settings',
    text: 'Profile Settings',
    path: '/manage-profile',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF, UserRole.PROPONENT, UserRole.DEAN, UserRole.OVCAF, UserRole.OVCRIGE, UserRole.REC, UserRole.OC],
    section: 'bottom',
  },
]

const filteredItems = computed(() => {
  if (!userStore.user) return []
  return allNavItems.filter(item => item.roles.includes(userStore.user.role))
})

const mainNavItems = computed(() => filteredItems.value.filter(item => item.section === 'main'))
const bottomNavItems = computed(() => filteredItems.value.filter(item => item.section === 'bottom'))

/**
 * Roles that see the help card. Its text is about the proponent workflow ("check your
 * proposal status"), so it is limited to proponents - the same role identifier the
 * navigation items and the store use. Everyone else simply does not render it, so no
 * empty card or gap is left behind; the divider and Profile Settings stay in place.
 */
const HELP_CARD_ROLES = [UserRole.PROPONENT]

const showHelpCard = computed(
  () => !props.collapsed && HELP_CARD_ROLES.includes(userStore.user?.role),
)

/**
 * True when the item is the page the user is on. A nested route counts as its parent,
 * so /dean-requests/7 keeps "Endorsement Requests" highlighted (and /proposals/7 keeps
 * "Proposals"): the item the user actually navigated from stays marked while they are
 * inside it.
 */
const isSelected = (path) => route.path === path || route.path.startsWith(`${path}/`)

/**
 * The unread count badge. It is attached to the Notifications link only and comes from
 * the shared notification store, so it matches the header bell and the notifications
 * page - nothing is hardcoded and nothing is shown when the count is zero.
 */
const isNotificationsItem = (item) => item.text === 'Notifications'

const badgeFor = (item) =>
  isNotificationsItem(item) ? unreadBadgeLabel(notificationStore.unreadCount) : ''
</script>

<style scoped>
/* ===== Sidebar container =====
   The shell paints the panel background; the nav fills it and keeps the main links at
   the top while the help card and Profile Settings stay at the bottom. */
.sidebar-nav {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 22px 0 16px;
  background: var(--rpis-sidebar-bg);
}

/* Section label */
.menu-label {
  margin: 0 0 18px;
  padding: 0 24px;
  font-size: 12.5px;
  font-weight: 500;
  letter-spacing: 0.2px;
  color: var(--rpis-on-dark-muted);
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
}

.nav-bottom {
  margin-top: auto;
  display: flex;
  flex-direction: column;
}

/* ===== Nav items ===== */
li {
  position: relative;
  background: transparent;
  transition: background 0.18s ease;
}

li:hover {
  background: rgba(255, 255, 255, 0.05);
}

li.active {
  background: rgba(255, 255, 255, 0.08);
}

/* The active route is marked with a thin yellow edge instead of the yellow fill that
   belongs to the Profile Settings button. */
li.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 3px;
  border-radius: 0 3px 3px 0;
  background: var(--rpis-accent);
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 15px;
  min-height: 51px;
  padding: 0 20px 0 26px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  line-height: 20px;
  color: var(--rpis-icon);
  transition: color 0.18s ease;
}

.nav-link:hover {
  color: var(--rpis-on-dark);
}

li.active .nav-link {
  color: var(--rpis-on-dark);
  font-weight: 600;
}

.nav-link:focus-visible {
  outline: 2px solid var(--rpis-accent);
  outline-offset: -2px;
}

/* Icons */
.nav-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
}

.nav-icon :deep(svg) {
  width: 18px;
  height: 18px;
  stroke-width: 1.6;
}

/* Text */
.nav-text {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* ===== Unread count badge (real count, capped at 99+) ===== */
.nav-badge {
  flex-shrink: 0;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  margin-left: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  background: var(--rpis-badge);
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
}


/* ===== Help card ===== */
.help-card {
  margin: 0 16px 0 13px;
  padding: 16px;
  border-radius: 17px;
  background: var(--rpis-help-bg);
  border: 1px solid var(--rpis-help-border);
}

.help-title {
  margin: 0 0 4px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--rpis-on-dark);
}

.help-text {
  margin: 0;
  font-size: 12.5px;
  font-weight: 400;
  line-height: 17.5px;
  color: var(--rpis-on-dark-soft);
}

/* ===== Divider ===== */
.nav-divider {
  height: 1px;
  margin: 13px 12px;
  background: var(--rpis-divider);
}

/* ===== Profile Settings (the one yellow control of the sidebar) ===== */
.profile-item {
  margin: 0 14px 0 11px;
  border-radius: 14px;
  background: var(--rpis-accent);
  transition: background 0.18s ease;
}

.profile-item:hover,
.profile-item.active {
  background: var(--rpis-accent-hover);
}

/* The yellow button is its own highlight, so it never gets the active edge. */
.profile-item.active::before {
  content: none;
}

.profile-link,
.profile-item.active .profile-link {
  justify-content: flex-start;
  min-height: 46px;
  padding: 0 16px;
  gap: 12px;
  font-size: 14px;
  font-weight: 700;
  color: #1c1a3c;
}

.profile-link:hover {
  color: #1c1a3c;
}

.profile-link:focus-visible {
  outline: 2px solid #1c1a3c;
  outline-offset: -3px;
}

.profile-item .nav-icon,
.profile-item .nav-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

.profile-item .nav-icon :deep(svg) {
  stroke-width: 1.7;
}

/* ===== Collapsed rail =====
   Only the icons remain; the label, the row text and the help card step aside and the
   title attribute keeps every link identifiable. */
.sidebar-nav.is-collapsed .menu-label,
.sidebar-nav.is-collapsed .nav-text,
.sidebar-nav.is-collapsed .help-card {
  display: none;
}

.sidebar-nav.is-collapsed .nav-link {
  justify-content: center;
  gap: 0;
  padding: 0 10px;
}

.sidebar-nav.is-collapsed .nav-badge {
  position: absolute;
  top: 8px;
  right: 10px;
  min-width: 18px;
  height: 18px;
  margin: 0;
  padding: 0 4px;
  font-size: 10px;
}

.sidebar-nav.is-collapsed .nav-divider {
  margin: 12px 14px;
}

.sidebar-nav.is-collapsed .profile-item {
  margin: 0 14px;
}

.sidebar-nav.is-collapsed .profile-link {
  justify-content: center;
  padding: 0;
}
</style>
