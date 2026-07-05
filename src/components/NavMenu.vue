<template>
  <nav>
    <ul>
      <li
        v-for="item in navItems"
        :key="item.path"
        :class="{ active: isSelected(item.path) }"
      >
        <router-link :to="item.path">
          <img
            class="nav-icon"
            :src="isSelected(item.path) ? item.activeIcon : item.icon"
            alt=""
          />
          {{ item.text }}
        </router-link>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { UserRole, useUserDataStore } from '@/stores/userData'

import dashboardIcon from '@/assets/images/dashboard.png'
import dashboardActiveIcon from '@/assets/images/dashboard_active.png'
import notificationIcon from '@/assets/images/notification.png'
import notificationActiveIcon from '@/assets/images/notification_active.png'
import submittedIcon from '@/assets/images/Add_ring.png'
import submittedActiveIcon from '@/assets/images/Add_ring_active.png'
import EndorseIcon from '@/assets/images/endorse icon.png'
import EndorseActiveIcon from '@/assets/images/endorse_active.png'
import SettingsIcon from '@/assets/images/Settings.png'
import SettingsActiveIcon from '@/assets/images/Settings_active.png'

const route = useRoute()
const userStore = useUserDataStore()

const allNavItems = [
  // ===========================
  // RPS ADMIN
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/rpsadmin-dash',
    roles: [UserRole.RPS_ADMIN],
  },

  {
  icon: submittedIcon,
  activeIcon: submittedActiveIcon,
  text: 'User Accounts',
  path: '/user-accounts',
  roles: [UserRole.RPS_ADMIN],
},

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/rpsadmin-notif',
    roles: [UserRole.RPS_ADMIN],
  },

  // ===========================
  // RPS STAFF
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/rpsstaff-dash',
    roles: [UserRole.RPS_STAFF],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Inbox',
    path: '/inbox',
    roles: [UserRole.RPS_STAFF],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/rpsstaff-notification',
    roles: [UserRole.RPS_STAFF],
  },
  // {
  //   icon: submittedIcon,
  //   activeIcon: submittedActiveIcon,
  //   text: 'Submitted Proposals',
  //   path: '/rps-subproposal',
  //   roles: [UserRole.RPS_STAFF],
  // },
  {
    icon: EndorseIcon,
    activeIcon: EndorseActiveIcon,
    text: 'Endorsed Proposals',
    path: '/rpsendorsed-prop',
    roles: [UserRole.RPS_STAFF],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Budget Processing',
    path: '/budget-processing',
    roles: [UserRole.RPS_STAFF],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Proposal Monitoring',
    path: '/rps-proposal-monitoring',
    roles: [UserRole.RPS_STAFF],
  },

  // ===========================
  // PROPONENT
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/proponent-dashboard',
    roles: [UserRole.PROPONENT],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/proponent-notification',
    roles: [UserRole.PROPONENT],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Revisions',
    path: '/revisions',
    roles: [UserRole.PROPONENT],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Proposals',
    path: '/proposals',
    roles: [UserRole.PROPONENT],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Evaluation Criteria',
    path: '/evalCriteria',
    roles: [UserRole.PROPONENT],
  },

  // ===========================
  // OVCRIGE
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/home',
    roles: [UserRole.OVCRIGE],
  },
  // {
  //   icon: submittedIcon,
  //   activeIcon: submittedActiveIcon,
  //   text: 'Submitted Proposals',
  //   path: '/submit-proposals',
  //   roles: [UserRole.OVCRIGE],
  // },
  {
    icon: EndorseIcon,
    activeIcon: EndorseActiveIcon,
    text: 'Endorsed Proposals',
    path: '/endorsed-proposals',
    roles: [UserRole.OVCRIGE],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OVCRIGE],
  },

  // ===========================
  // OVCAF
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/ovcaf/dash',
    roles: [UserRole.OVCAF],
  },

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Proposal Inbox',
    path: '/ovcaf/inbox',
    roles: [UserRole.OVCAF],
  },

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Financial Reports',
    path: '/ovcaf/reports',
    roles: [UserRole.OVCAF],
  },




  // ===========================
  // REC
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/rec-dash',
    roles: [UserRole.REC],
  },
  {
    icon: submittedIcon,
    activeIcon: submittedActiveIcon,
    text: 'Assigned Proposals',
    path: '/assigned-proposals',
    roles: [UserRole.REC],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/rec-notif',
    roles: [UserRole.REC],
  },

  // ===========================
  // OC
  // ===========================
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/oc-dashboard',
    roles: [UserRole.OC],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/oc-notification',
    roles: [UserRole.OC],
  },

  // ===========================
  // PROFILE SETTINGS (Shared)
  // ===========================
  {
    icon: SettingsIcon,
    activeIcon: SettingsActiveIcon,
    text: 'Profile Settings',
    path: '/manage-profile',
    roles: [
      UserRole.RPS_ADMIN,
      UserRole.RPS_STAFF,
      UserRole.PROPONENT,
      UserRole.OVCAF,
      UserRole.OVCRIGE,
      UserRole.REC,
      UserRole.OC,
    ],
  },
]

const navItems = computed(() => {
  if (!userStore.user) return []

  console.log('Current Role:', userStore.user.role)

  return allNavItems.filter(item =>
    item.roles.includes(userStore.user.role)
  )
})

const isSelected = (path) => route.path === path
</script>

<style scoped>
ul {
  padding: 100px 0 0 20px;
  list-style: none;
  margin: 0;
}

li {
  margin-bottom: 8px;
  height: 50px;
  border-radius: 12px;
  width: 215px;
  padding: 0 5px;
  background: #262342;
}

li a {
  display: flex;
  align-items: center;
  height: 100%;
  text-decoration: none;
  font-weight: bold;
  color: #ffd803;
}

li.active {
  background: white;
  width: 100%;
  border-top-right-radius: 0;
  border-bottom-right-radius: 0;
}

li.active a {
  color: #262342;
}

.nav-icon {
  width: 30px;
  height: 30px;
  margin-right: 8px;
}
</style>