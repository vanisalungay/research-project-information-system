<template>
  <div class="notifications-hub">
    <!-- Active Role-Specific Notification Renderer with Dynamic Lazy Loading -->
    <OvcNotifications v-if="loggedInUser.role === UserRole.OVCRIGE"></OvcNotifications>
    <ProponentNotifications v-if="loggedInUser.role === UserRole.PROPONENT"></ProponentNotifications>
    <RecNotification v-if="loggedInUser.role === UserRole.REC"></RecNotification>
    <RpsStaffNotification v-if="loggedInUser.role === UserRole.RPS_STAFF"></RpsStaffNotification>
    <RpsAdminNotification v-if="loggedInUser.role === UserRole.RPS_ADMIN"></RpsAdminNotification>
    <OvcafNotification v-if="loggedInUser.role === UserRole.OVCAF"></OvcafNotification>
    <OCNotifications v-if="loggedInUser.role === UserRole.OC"></OCNotifications>
  </div>
</template>

<script setup>
import { computed, defineAsyncComponent } from 'vue'
import { useUserDataStore, UserRole } from '@/stores/userData'

// Centralized dynamic lazy-loading definitions to isolate CSS scopes
const OvcNotifications = defineAsyncComponent(() => import('./OvcNotifications.vue'))
const ProponentNotifications = defineAsyncComponent(() => import('./ProponentNotifications.vue'))
const RecNotification = defineAsyncComponent(() => import('./RecNotification.vue'))
const RpsStaffNotification = defineAsyncComponent(() => import('./RpsStaffNotification.vue'))
const RpsAdminNotification = defineAsyncComponent(() => import('./RpsAdminNotification.vue'))
const OvcafNotification = defineAsyncComponent(() => import('./OvcafNotification.vue'))
const OCNotifications = defineAsyncComponent(() => import('./OCNotifications.vue'))

const useUser = useUserDataStore()

const loggedInUser = computed(() => {
  return useUser.user || { role: '' }
})
</script>

<style scoped>
.notifications-hub {
  width: 100%;
  min-height: calc(100vh - 169px);
  animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>