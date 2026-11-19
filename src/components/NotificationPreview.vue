<template>
  <section class="notification-preview">
    <header class="preview-header">
      <div class="preview-heading">
        <h2>{{ title }}</h2>
        <span v-if="unreadCount > 0" class="preview-badge">{{ unreadBadgeLabel(unreadCount) }} unread</span>
      </div>
      <button
        v-if="showViewAll"
        type="button"
        class="preview-view-all"
        @click="router.push(viewAllTo)"
      >
        View all
      </button>
    </header>

    <p v-if="items.length === 0" class="preview-empty">{{ emptyText }}</p>

    <ul v-else class="preview-list">
      <li
        v-for="notification in items"
        :key="notification.id"
        class="preview-item"
        :class="{ 'is-unread': !notification.isRead }"
        @click="openNotification(notification)"
      >
        <span v-if="!notification.isRead" class="preview-dot" aria-hidden="true"></span>
        <div class="preview-content">
          <p class="preview-title">{{ notification.title }}</p>
          <p class="preview-message">{{ notification.message }}</p>
          <RelativeTime class="preview-time" :value="notification.createdAt" />
        </div>
      </li>
    </ul>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import ConfirmDialog from '@/components/ConfirmDialog.vue'
import RelativeTime from '@/components/RelativeTime.vue'
import { useDialog } from '@/composables/useDialog'
import { useNotificationStore } from '@/stores/notifications'
import { useUserDataStore } from '@/stores/userData'
import { unreadBadgeLabel } from '@/utils/notifications'
import { resolveNotificationDestination } from '@/utils/notificationDestination'

const props = defineProps({
  /** Heading shown above the preview. */
  title: { type: String, default: 'Notifications' },
  /** How many of the newest notifications to show. */
  limit: { type: Number, default: 5 },
  /** Where "View all" goes. */
  viewAllTo: { type: String, default: '/notifications' },
  /** Shown when the account has no notifications. */
  emptyText: { type: String, default: 'No notifications yet.' },
  /** Hide the "View all" control when the page owns the full list already. */
  showViewAll: { type: Boolean, default: true },
})

const router = useRouter()
const notificationStore = useNotificationStore()
const userStore = useUserDataStore()
const { dialogState, showAlert } = useDialog()

const items = computed(() =>
  notificationStore.notifications.slice(0, Math.max(0, props.limit)),
)
const unreadCount = computed(() => notificationStore.unreadCount)

async function openNotification(notification) {
  if (!notification.isRead) {
    notificationStore.markAsRead(notification.id)
  }
  const destination = resolveNotificationDestination(
    notification,
    userStore.user?.role,
  )
  if (destination) {
    router.push(destination)
    return
  }
  await showAlert(
    'This notification is not linked to a record you can open.',
    { type: 'info', title: 'Nothing to open' },
  )
}
</script>

<style scoped>
.notification-preview {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px 20px;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.preview-heading {
  display: flex;
  align-items: center;
  gap: 10px;
}

.preview-heading h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.preview-badge {
  background: #ef4444;
  color: #fff;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 700;
}

.preview-view-all {
  background: none;
  border: none;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  padding: 4px;
}

.preview-empty {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.preview-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-item {
  position: relative;
  display: flex;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #f8fafc;
  cursor: pointer;
  transition: background 0.15s ease;
}

.preview-item:hover {
  background: #eef2f7;
}

.preview-item.is-unread {
  background: #eff6ff;
}

.preview-dot {
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: #2563eb;
  flex-shrink: 0;
}

.preview-content {
  min-width: 0;
}

.preview-title {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.preview-message {
  margin: 2px 0 4px;
  font-size: 12px;
  color: #475569;
  line-height: 1.4;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.preview-time {
  font-size: 11px;
  color: #64748b;
}
</style>
