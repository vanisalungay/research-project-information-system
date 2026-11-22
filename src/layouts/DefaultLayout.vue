<template>
  <div class="app-layout">
    <!-- The header spans the whole viewport, including the area above the sidebar. -->
    <HeaderLogo class="app-header" :collapsed="sidebarCollapsed" @toggle-sidebar="toggleSidebar" />

    <div class="app-body">
      <!-- The sidebar starts directly below the header and fills the remaining height;
           its own content scrolls when it is taller than the viewport. -->
      <aside class="app-sidebar" :class="{ 'is-collapsed': sidebarCollapsed }">
        <NavMenu :collapsed="sidebarCollapsed" />
      </aside>

      <main class="app-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import HeaderLogo from '@/components/HeaderLogo.vue'
import NavMenu from '@/components/NavMenu.vue'
import { useUserDataStore } from '@/stores/userData'
import { useNotificationStore } from '@/stores/notifications'
import { useDeanAttentionStore } from '@/stores/deanAttention'

const route = useRoute()
const userStore = useUserDataStore()
const notificationStore = useNotificationStore()
const deanAttentionStore = useDeanAttentionStore()

/**
 * The sidebar is expanded on a desktop and a narrow icon rail on a smaller screen; the
 * hamburger in the header toggles it in both cases, so there is only ever one state.
 */
const COMPACT_LAYOUT = '(max-width: 1024px)'
const sidebarCollapsed = ref(false)

let compactQuery = null
const syncCompactLayout = (event) => {
  sidebarCollapsed.value = event.matches
}

if (typeof window !== 'undefined' && typeof window.matchMedia === 'function') {
  compactQuery = window.matchMedia(COMPACT_LAYOUT)
  sidebarCollapsed.value = compactQuery.matches
  compactQuery.addEventListener('change', syncCompactLayout)
}

onBeforeUnmount(() => {
  compactQuery?.removeEventListener('change', syncCompactLayout)
  notificationStore.disconnect()
})

const toggleSidebar = () => {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

/**
 * The unread badge and list come from one store, fed by one live SSE connection for
 * the whole authenticated shell. It starts when the signed-in account is known and
 * stops when the shell goes away; route changes no longer trigger a list request
 * because marking a notification read updates the store directly.
 */
watch(
  () => userStore.user?.id ?? null,
  (userId) => {
    if (userId == null) {
      notificationStore.reset()
    } else {
      notificationStore.connect(userId)
      notificationStore.refresh(userId)
    }
    // The Dean's dashboard also holds the endorsement requests, so its sidebar badge
    // follows the account/route. Any other role resolves to zero without a request.
    deanAttentionStore.refresh(userStore.user?.role ?? null)
  },
  { immediate: true },
)

// The Dean's combined "attention" badge still depends on the page (a newly opened
// endorsement request may not produce a notification of its own).
watch(
  () => route.fullPath,
  () => deanAttentionStore.refresh(userStore.user?.role ?? null),
)
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--rpis-content-bg);
}

.app-header {
  flex-shrink: 0;
}

.app-body {
  flex: 1;
  display: flex;
  min-height: 0;
}

.app-sidebar {
  width: var(--rpis-sidebar-width);
  flex-shrink: 0;
  background: var(--rpis-sidebar-bg);
  overflow-y: auto;
  overflow-x: hidden;
  transition: width 0.2s ease;
}

.app-sidebar.is-collapsed {
  width: var(--rpis-sidebar-collapsed-width);
}

/* The page content keeps its own padding and scrolls on its own, never under the
   header or the sidebar. */
.app-content {
  flex: 1;
  min-width: 0;
  overflow-y: auto;
  background: var(--rpis-content-bg);
  padding: 32px;
}

@media (max-width: 640px) {
  .app-content {
    padding: 20px 16px;
  }
}
</style>
