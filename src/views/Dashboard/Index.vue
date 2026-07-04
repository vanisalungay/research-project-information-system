<template>
  <div class="dashboard-hub">
    <!-- Active Role-Specific Dashboard Renderer with Dynamic Lazy Loading -->
    <OvcrigeDashboard v-if="loggedInUser.role === UserRole.OVCRIGE"></OvcrigeDashboard>
    <ProponentDashboard v-if="loggedInUser.role === UserRole.PROPONENT"></ProponentDashboard>
    <RecDashboard v-if="loggedInUser.role === UserRole.REC"></RecDashboard>
    <RpsStaffDashboard v-if="loggedInUser.role === UserRole.RPS_STAFF"></RpsStaffDashboard>
    <RpsAdminDashboard v-if="loggedInUser.role === UserRole.RPS_ADMIN"></RpsAdminDashboard>
    <OvcafDashboard v-if="loggedInUser.role === UserRole.OVCAF"></OvcafDashboard>
    <OCDashboard v-if="loggedInUser.role === UserRole.OC"></OCDashboard>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useUserDataStore, UserRole } from '@/stores/userData'

// Centralized dynamic lazy-loading definitions to isolate CSS scopes
const OvcrigeDashboard = defineAsyncComponent(() => import('./OvcrigeDashboard.vue'))
const ProponentDashboard = defineAsyncComponent(() => import('./ProponentDashboard.vue'))
const RecDashboard = defineAsyncComponent(() => import('./RecDashboard.vue'))
const RpsStaffDashboard = defineAsyncComponent(() => import('./RpsStaffDashboard.vue'))
const RpsAdminDashboard = defineAsyncComponent(() => import('./RpsAdminDashboard.vue'))
const OvcafDashboard = defineAsyncComponent(() => import('./OvcafDashboard.vue'))
const OCDashboard = defineAsyncComponent(() => import('./OCDashboard.vue'))

const useUser = useUserDataStore()

const loggedInUser = computed(() => {
  return useUser.user || { role: '' }
})
</script>

<style scoped>
.dashboard-hub {
  width: 100%;
  min-height: calc(100vh - 169px);
  animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
