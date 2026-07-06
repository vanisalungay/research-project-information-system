<template>
  <nav class="sidebar-nav">
    <!-- Main Navigation Items -->
    <ul class="nav-list">
      <li
        v-for="item in mainNavItems"
        :key="item.path"
        :class="{ active: isSelected(item.path) }"
      >
        <router-link :to="item.path" class="nav-link">
          <span class="nav-icon" v-html="getIcon(item.iconKey)"></span>
          <span class="nav-text">{{ item.text }}</span>
        </router-link>
      </li>
    </ul>

    <!-- Bottom Section: Profile Settings -->
    <div class="nav-bottom">
      <div class="nav-divider">
        <span class="divider-label">Account</span>
      </div>
      <ul class="nav-list">
        <li
          v-for="item in bottomNavItems"
          :key="item.path"
          :class="{ active: isSelected(item.path) }"
        >
          <router-link :to="item.path" class="nav-link">
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

const route = useRoute()
const userStore = useUserDataStore()

// SVG icon definitions
const icons = {
  dashboard: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><rect x="14" y="14" width="7" height="7" rx="1"/></svg>`,
  inbox: `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="22 12 16 12 14 15 10 15 8 12 2 12"/><path d="M5.45 5.11L2 12v6a2 2 0 002 2h16a2 2 0 002-2v-6l-3.45-6.89A2 2 0 0016.76 4H7.24a2 2 0 00-1.79 1.11z"/></svg>`,
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
}

const getIcon = (key) => icons[key] || icons.dashboard

const allNavItems = [
  // Shared: Dashboard
  {
    iconKey: 'dashboard',
    text: 'Dashboard',
    path: '/home',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF, UserRole.PROPONENT, UserRole.OVCAF, UserRole.OVCRIGE, UserRole.REC, UserRole.OC],
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
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.RPS_ADMIN],
    section: 'main',
  },

  // RPS STAFF
  {
    iconKey: 'inbox',
    text: 'Inbox',
    path: '/inbox',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'proposals',
    text: 'Submitted Proposals',
    path: '/rps-subproposal',
    roles: [UserRole.RPS_STAFF],
    section: 'main',
  },
  {
    iconKey: 'endorse',
    text: 'Endorsed Proposals',
    path: '/rpsendorsed-prop',
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

  // PROPONENT
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },
  {
    iconKey: 'revisions',
    text: 'Revisions',
    path: '/revisions',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },
  {
    iconKey: 'proposals',
    text: 'Proposals',
    path: '/proposals',
    roles: [UserRole.PROPONENT],
    section: 'main',
  },
  {
    iconKey: 'evalCriteria',
    text: 'Evaluation Criteria',
    path: '/evalCriteria',
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
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OVCRIGE],
    section: 'main',
  },

  // OVCAF
  {
    iconKey: 'review',
    text: 'Proposals for Review',
    path: '/ovcaf-dash',
    roles: [UserRole.OVCAF],
    section: 'main',
  },
  {
    iconKey: 'notification',
    text: 'Notifications',
    path: '/notifications',
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

  // Shared: Profile Settings (bottom section)
  {
    iconKey: 'settings',
    text: 'Profile Settings',
    path: '/manage-profile',
    roles: [UserRole.RPS_ADMIN, UserRole.RPS_STAFF, UserRole.PROPONENT, UserRole.OVCAF, UserRole.OVCRIGE, UserRole.REC, UserRole.OC],
    section: 'bottom',
  },
]

const filteredItems = computed(() => {
  if (!userStore.user) return []
  return allNavItems.filter(item => item.roles.includes(userStore.user.role))
})

const mainNavItems = computed(() => filteredItems.value.filter(item => item.section === 'main'))
const bottomNavItems = computed(() => filteredItems.value.filter(item => item.section === 'bottom'))

const isSelected = (path) => route.path === path
</script>

<style scoped>
.sidebar-nav {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 20px 12px;
}

.nav-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-bottom {
  margin-top: auto;
}

/* Divider */
.nav-divider {
  display: flex;
  align-items: center;
  padding: 16px 12px 8px;
}

.divider-label {
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 1.5px;
  color: rgba(255, 216, 3, 0.4);
}

/* Nav Items */
li {
  background: #262342;
  border-radius: 10px;
  transition: all 0.15s ease;
  position: relative;
}

li:hover {
  background: #332f5a;
}

li.active {
  background: #ffffff;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

li.active:hover {
  background: #ffffff;
}

/* Nav Link */
.nav-link {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
  color: #ffd803;
  letter-spacing: 0.2px;
  transition: color 0.15s ease;
}

li.active .nav-link {
  color: #262342;
}

/* Icon */
.nav-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-icon :deep(svg) {
  width: 20px;
  height: 20px;
}

/* Text */
.nav-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Active indicator bar */
li.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: #ffd803;
  border-radius: 0 3px 3px 0;
}
</style>