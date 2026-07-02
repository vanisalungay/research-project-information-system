<template>
  <nav>
    <ul>
      <li v-for="item in navItems" :key="item.path" :class="{ active: isSelected(item.path) }">
        <router-link :to="item.path">
          <img class="nav-icon" :src="isSelected(item.path) ? item.activeIcon : item.icon" />
          {{ item.text }}
        </router-link>
      </li>
    </ul>
  </nav>
</template>

<script setup>
import { computed } from 'vue'
import { UserRole, useUserDataStore } from '@/stores/userData'
import { useRoute } from 'vue-router'

import dashboardIcon from '@/assets/images/dashboard.png'
import dashboardActiveIcon from '@/assets/images/dashboard_active.png'
import notificationIcon from '@/assets/images/notification.png'
import notificationActiveIcon from '@/assets/images/notification_active.png'
import submittedIcon from '../assets/images/Add_ring.png'
import submittedActiveIcon from '../assets/images/Add_ring_active.png'
import EndorseIcon from '../assets/images/endorse icon.png'
import EndorseActiveIcon from '../assets/images/endorse_active.png'
import SettingsIcon from '@/assets/images/Settings.png'
import SettingsActiveIcon from '@/assets/images/Settings_active.png'
import revisionIcon from '@/assets/images/revision.png'
import revisionActiveIcon from '@/assets/images/revision_active.png'
import proposalIcon from '@/assets/images/proposal.png'
import proposalActiveIcon from '@/assets/images/proposal_active.png'

const route = useRoute()
const userStore = useUserDataStore()

const allNavItems = [
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/home',
    roles: [UserRole.OVCRIGE],
  },

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

  {
    icon: submittedIcon,
    activeIcon: submittedActiveIcon,
    text: 'Submitted Proposals',
    path: '/submit-proposals',
    roles: [UserRole.OVCRIGE],
  },
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
    roles: [UserRole.RII_STAFF],
  },
  
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/rpsstaff-notification',
    roles: [UserRole.RPS_STAFF],
  },

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
    roles: [UserRole.RII_STAFF],
    text: 'Funded Proposal',
    path: '/funded-prop',
    roles: [UserRole.RPS_STAFF],
  },

  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/ovcaf-dash',
    roles: [UserRole.OVCAF],
  },
  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/rpsadmin-dash',
    roles: [UserRole.RPS_ADMIN],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/rpsadmin-notif',
    roles: [UserRole.RPS_ADMIN],
  },
  
  
  {
    icon: submittedIcon,
    activeIcon: submittedActiveIcon,
    text: 'Proposal Monitoring',
    path: '/rii-subproposal',
    roles: [
      UserRole.PROPONENT,
      UserRole.RII_STAFF,
      UserRole.RII_ADMIN,
      UserRole.OVCRIGE,
      UserRole.REC,
      UserRole.OC,
      UserRole.OVCAF
    ],
    text: 'Submitted Proposals',
    path: '/rps-subproposal',
    roles: [UserRole.RPS_STAFF],
  },

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/ovcaf-notification',
    roles: [UserRole.OVCAF],
  },

  {
    icon: EndorseIcon,
    activeIcon: EndorseActiveIcon,
    text: 'Endorsed Proposals',
    path: '/endorsed-proposals',
    roles: [UserRole.OVCRIGE],
  },

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
    text: 'Submitted Proposals',
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

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/notifications',
    roles: [UserRole.OVCRIGE],
  },

  
  {
    icon: SettingsIcon,
    activeIcon: SettingsActiveIcon,
    text: 'Profile Settings',
    path: '/manage-profile',
    roles: [
      UserRole.REC,
      UserRole.OVCRIGE,
      UserRole.OC,
      UserRole.RPS_ADMIN,
      UserRole.RPS_STAFF,
      UserRole.PROPONENT,
    ],
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
]

const navItems = computed(() => {
  if (!userStore.user) return []
  return allNavItems.filter((item) => item.roles.includes(userStore.user.role))
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
  background-color: #262342;
  color: #ffd803;
}

li img {
  margin-right: 5px;
}

li a {
  display: flex;
  align-items: center;
  justify-items: start;
  height: 100%;
  text-decoration: none;
  font-weight: bold;

  color: #ffd803;
}

li.active {
  background-color: white;
  border-top-left-radius: 12px;
  border-bottom-left-radius: 12px;
  border-top-right-radius: unset;
  border-bottom-right-radius: unset;
  width: 100%;
}

li.active a {
  color: #262342;
}

router-link {
  text-decoration: none;
  color: #333;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
}

.nav-icon {
  width: 30px;
  height: 30px;
}
</style>
