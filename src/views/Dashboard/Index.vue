<template>
  <div>
    <OvcrigeDashboard v-if="loggedInUser.role === UserRole.OVCRIGE"></OvcrigeDashboard>
    <ProponentDashboard v-if="loggedInUser.role === UserRole.PROPONENT"></ProponentDashboard>
    <RecDashboard v-if="loggedInUser.role === UserRole.REC"></RecDashboard>
    <RiiStaffDashboard v-if="loggedInUser.role === UserRole.RII_STAFF"></RiiStaffDashboard>
    <RiiAdminDashboard v-if="loggedInUser.role === UserRole.RII_ADMIN"></RiiAdminDashboard>
    <OvcafDashboard v-if="loggedInUser.role === UserRole.OVCAF"></OvcafDashboard>
    <OCDashboard v-if="loggedInUser.role === UserRole.OC"></OCDashboard>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { useUserDataStore, UserRole } from '@/stores/userData'

import OvcrigeDashboard from './OvcrigeDashboard.vue'
import ProponentDashboard from './ProponentDashboard.vue'
import RecDashboard from './RecDashboard.vue'
import RiiStaffDashboard from './RiiStaffDashboard.vue'
import RiiAdminDashboard from './RiiAdminDashboard.vue'
import OvcafDashboard from './OvcafDashboard.vue'
import OCDashboard from './OCDashboard.vue'

const useUser = useUserDataStore()

const loggedInUser = computed(() => {
  return useUser.user
})
</script>
