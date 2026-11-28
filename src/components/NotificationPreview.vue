<template>
  <section class="notification-preview" :class="{ 'is-compact': compact }">
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
        <span>View all</span>
        <span v-if="compact" class="preview-view-all-arrow" aria-hidden="true">&rarr;</span>
      </button>
    </header>

    <p v-if="items.length === 0" class="preview-empty">{{ emptyText }}</p>

    <ul v-else class="preview-list">
      <li
        v-for="notification in items"
        :key="notification.id"
        class="preview-item"
        :class="{ 'is-unread': !notification.isRead }"
        :role="compact ? 'button' : null"
        :tabindex="compact ? 0 : null"
        @click="openNotification(notification)"
        @keydown="handleItemKeydown($event, notification)"
      >
        <span v-if="!notification.isRead" class="preview-dot" aria-hidden="true"></span>
        <span v-if="compact && !notification.isRead" class="preview-sr-only">Unread</span>
        <div class="preview-content">
          <p class="preview-title">{{ notification.title }}</p>
          <p class="preview-message" :title="compact ? notification.message : null">{{ notification.message }}</p>
          <p
            v-if="compact && notification.proposalDocumentId"
            class="preview-doc-id"
          >
            {{ notification.proposalDocumentId }}
          </p>
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
  /** Compact dashboard variant: tighter cards, scrollable list, keyboard support. */
  compact: { type: Boolean, default: false },
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

/**
 * Enter/Space on a compact card opens it exactly like a click. The classic preview keeps
 * its existing mouse-only behaviour, so no other dashboard's interaction changes.
 */
function handleItemKeydown(event, notification) {
  if (!props.compact) return
  if (event.key !== 'Enter' && event.key !== ' ') return
  event.preventDefault()
  openNotification(notification)
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
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
  color: #0f172a;
}

.preview-badge {
  background: #dc2626;
  color: #fff;
  border-radius: 999px;
  padding: 2px 8px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
}

.preview-view-all {
  background: none;
  border: none;
  color: #1d4ed8;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
  padding: 4px;
}

.preview-empty {
  margin: 0;
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
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
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
  color: #0f172a;
}

.preview-message {
  margin: 2px 0 4px;
  font-size: var(--ui-font-caption);
  color: #475569;
  line-height: var(--ui-line-snug);
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.preview-time {
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
}

/* ===========================================================================
   Compact variant (opt-in; OVCRIGE dashboard only).
   Every rule below is scoped to .is-compact, so the Dean, REC, OC, OVCAF,
   Proponent and RPS dashboards keep the presentation they have today.
   =========================================================================== */

.notification-preview.is-compact {
  /* The caller supplies the panel shell (.dash-section); the component is only content. */
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
}

.notification-preview.is-compact .preview-header {
  flex-wrap: wrap;
  margin-bottom: 12px;
  gap: 6px 10px;
}

.notification-preview.is-compact .preview-heading {
  gap: 8px;
  min-width: 0;
}

.notification-preview.is-compact .preview-heading h2 {
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-tight);
  color: #1c1a3c;
}

.notification-preview.is-compact .preview-badge {
  background: #e9efff;
  color: #1d40cc;
  border: 1px solid #c7d2fe;
  border-radius: 999px;
  padding: 1px 8px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-body);
  white-space: nowrap;
}

.notification-preview.is-compact .preview-view-all {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  padding: 4px 8px;
  border-radius: 8px;
  color: #315bff;
  font-size: var(--ui-font-small);
  font-weight: var(--ui-weight-semibold);
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.notification-preview.is-compact .preview-view-all:hover {
  background: #eef1ff;
  color: #1d40cc;
}

.notification-preview.is-compact .preview-view-all:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.notification-preview.is-compact .preview-view-all-arrow {
  font-size: var(--ui-font-body);
  line-height: var(--ui-line-none);
}

.notification-preview.is-compact .preview-list {
  gap: 8px;
  max-height: 320px;
  overflow-y: auto;
  overscroll-behavior: contain;
}

.notification-preview.is-compact .preview-list::-webkit-scrollbar {
  width: 6px;
}

.notification-preview.is-compact .preview-list::-webkit-scrollbar-thumb {
  background: #d7dcef;
  border-radius: 999px;
}

.notification-preview.is-compact .preview-list::-webkit-scrollbar-track {
  background: transparent;
}

.notification-preview.is-compact .preview-item {
  padding: 10px 12px;
  gap: 10px;
  border: 1px solid #edf0f8;
  border-radius: 10px;
  background: #fbfcff;
}

.notification-preview.is-compact .preview-item:hover {
  background: #f4f7ff;
  border-color: #e2e8f5;
}

.notification-preview.is-compact .preview-item.is-unread {
  background: #eef4ff;
  border-color: #dbe4ff;
}

.notification-preview.is-compact .preview-item:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: -2px;
}

.notification-preview.is-compact .preview-dot {
  width: 7px;
  height: 7px;
  margin-top: 6px;
  background: #315bff;
}

.notification-preview.is-compact .preview-content {
  flex: 1;
}

.notification-preview.is-compact .preview-title {
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-snug);
}

.notification-preview.is-compact .preview-item.is-unread .preview-title {
  font-weight: var(--ui-weight-bold);
}

.notification-preview.is-compact .preview-message {
  margin: 2px 0 3px;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: #5b6484;
}

.notification-preview.is-compact .preview-doc-id {
  margin: 0 0 3px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  letter-spacing: var(--ui-tracking-label);
  color: var(--ui-text-muted);
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.notification-preview.is-compact .preview-time {
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
}

.notification-preview.is-compact .preview-empty {
  padding: 10px 0;
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
}

/* When the dashboard stacks its panels, the notification panel gets the full content
   width. Two readable card columns use that space instead of stretching each message into
   one very long line. The side-by-side desktop layout keeps the single-column list, and a
   narrow phone keeps one full-width card. */
@media (min-width: 760px) and (max-width: 1599px) {
  .notification-preview.is-compact .preview-list {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(min(360px, 100%), 1fr));
    align-content: start;
  }
}

/* The unread state is announced, not conveyed by colour alone. */
.preview-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (prefers-color-scheme: dark) {
  .notification-preview.is-compact .preview-heading h2 {
    color: #f1f2fb !important;
  }

  .notification-preview.is-compact .preview-badge {
    background: rgba(49, 91, 255, 0.2) !important;
    color: #b9c6ff !important;
    border-color: rgba(148, 166, 255, 0.35) !important;
  }

  .notification-preview.is-compact .preview-item {
    background: rgba(255, 255, 255, 0.04) !important;
    border-color: rgba(255, 255, 255, 0.08) !important;
  }

  .notification-preview.is-compact .preview-item:hover {
    background: rgba(255, 255, 255, 0.08) !important;
  }

  .notification-preview.is-compact .preview-item.is-unread {
    background: rgba(49, 91, 255, 0.16) !important;
    border-color: rgba(120, 145, 255, 0.35) !important;
  }

  .notification-preview.is-compact .preview-title {
    color: #e8eaf6 !important;
  }

  .notification-preview.is-compact .preview-message {
    color: #b8bfd6 !important;
  }

  .notification-preview.is-compact .preview-doc-id,
  .notification-preview.is-compact .preview-time {
    color: #94a3b8 !important;
  }

  .notification-preview.is-compact .preview-empty {
    color: #94a3b8 !important;
  }
}
</style>
