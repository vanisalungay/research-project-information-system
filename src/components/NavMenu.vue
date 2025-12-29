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
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
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
    path: '/riistaff-dash',
    roles: [UserRole.RII_STAFF],
  },
  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/riistaff-notification',
    roles: [UserRole.RII_STAFF],
  },

  {
    icon: EndorseIcon,
    activeIcon: EndorseActiveIcon,
    text: 'Endorsed Proposals',
    path: '/riiendorsed-prop',
    roles: [UserRole.RII_STAFF],
  },

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Funded Proposal',
    path: '/funded-prop',
    roles: [UserRole.RII_STAFF],
  },

  {
    icon: dashboardIcon,
    activeIcon: dashboardActiveIcon,
    text: 'Dashboard',
    path: '/ovcaf-dash',
    roles: [UserRole.OVCAF],
  },

  {
    icon: notificationIcon,
    activeIcon: notificationActiveIcon,
    text: 'Notifications',
    path: '/riiadmin-notif',
    roles: [UserRole.RII_ADMIN],
  },

  {
    icon: submittedIcon,
    activeIcon: submittedActiveIcon,
    text: 'Submitted Proposals',
    path: '/rii-subproposal',
    roles: [UserRole.RII_STAFF],
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
      UserRole.RII_ADMIN,
      UserRole.RII_STAFF,
      UserRole.PROPONENT,
    ],
  },

  {
    icon: revisionIcon,
    activeIcon: revisionActiveIcon,
    text: 'Revisions',
    path: '/revisions',
    roles: [UserRole.PROPONENT],
  },

  {
    icon: proposalIcon,
    activeIcon: proposalActiveIcon,
    text: 'Proposals',
    path: '/proposals',
    roles: [UserRole.PROPONENT],
  },

  {
    icon: EndorseIcon,
    activeIcon: EndorseActiveIcon,
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
  /* font-size: 20px; */
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
