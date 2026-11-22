<template>
  <div class="notification-page">
    <!-- HEADER -->
    <header class="page-header">
      <div class="header-left">
        <h1>Notifications</h1>
        <span class="badge" v-if="unreadCount > 0">{{ unreadCount }} unread</span>
      </div>
      <div class="header-actions">
        <button
          class="btn-mark-all btn-primary"
          @click="markAllAsRead"
          :disabled="unreadCount === 0 || loading"
        >
          Mark All as Read
        </button>
      </div>
    </header>

    <!-- FILTERS -->
    <div class="filter-bar">
      <input 
        type="text" 
        v-model="searchQuery" 
        placeholder="Search notifications..." 
        class="search-input"
      />
      <select v-model="filterStatus" class="filter-select">
        <option value="all">All Notifications</option>
        <option value="unread">Unread</option>
        <option value="read">Read</option>
      </select>
      <!-- Filtering by kind keeps the proposal updates (the busiest kind, especially for
           RPS) easy to pick out of the list. Nothing is hidden by default. -->
      <select v-model="filterKind" class="filter-select">
        <option value="all">All kinds</option>
        <option value="proposal">Proposal updates</option>
        <option value="call">Calls for Proposals</option>
        <option value="endorsement">Endorsements</option>
        <option value="other">Other</option>
      </select>
    </div>

    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading notifications...</p>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="error" class="error-state">
      <p class="error-text">{{ error }}</p>
      <button class="btn-retry btn-secondary" @click="fetchNotifications">Retry</button>
    </div>

    <!-- NOTIFICATIONS LIST -->
    <template v-else>
      <div v-if="filteredNotifications.length > 0">
        <!-- Proposal updates sit under their own heading, but every group is fully
             expanded: no notification content is hidden behind a collapse control. -->
        <section v-for="group in notificationGroups" :key="group.key" class="notification-group">
          <div v-if="group.title" class="group-header">
            <span class="group-title">{{ group.title }}</span>
            <span class="group-count">{{ group.items.length }}</span>
          </div>

          <div class="notifications-list">
            <div
              v-for="notification in group.items"
              :key="notification.id"
              class="notification-card"
              :class="{
                unread: !notification.isRead,
                read: notification.isRead
              }"
              @click="handleNotificationClick(notification)"
            >
          <div class="notification-icon">
            <span v-if="!notification.isRead" class="unread-dot"></span>
            <svg v-else class="read-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          </div>

          <div class="notification-content">
            <div class="notification-header">
              <h3 class="notification-title">{{ notification.title }}</h3>
              <RelativeTime class="notification-time" :value="notification.createdAt" />
            </div>

            <!-- Call for Proposal notification: the call it is about, with its CURRENT
                 status (Open = green, Closed = red, Scheduled = blue). The status label is
                 always shown next to the colour, so the status never depends on colour. -->
            <div
              v-if="callStatus(notification) || notification.proposalAnnouncementName"
              class="notification-call"
            >
              <span
                v-if="callStatus(notification)"
                class="call-status"
                :class="statusTone(notification.proposalAnnouncementStatus)"
              >
                <span class="call-status-dot" aria-hidden="true"></span
                >{{ callStatus(notification).label }}
              </span>
              <span v-if="notification.proposalAnnouncementName" class="call-name">
                {{ notification.proposalAnnouncementName }}
              </span>
            </div>

            <p class="notification-message">{{ notification.message }}</p>
            <div class="notification-footer">
              <div class="notification-meta">
                <span class="notification-type" :class="getTypeClass(notification.type)">
                  {{ formatType(notification.type) }}
                </span>
                <!-- The formatted Proposal ID the backend resolves from the linked proposal's
                     stored documentId (MSUN-ORPS-DA-028-2026-REV00). It is omitted when the
                     record has no proposal or no identifier, never replaced by the database id. -->
                <span
                  v-if="notificationKind(notification, userStore.user?.role) === 'proposal' && notification.proposalDocumentId"
                  class="notification-proposal"
                >
                  {{ notification.proposalDocumentId }}
                </span>
              </div>
              <button
                v-if="!notification.isRead"
                class="btn-mark-read btn-view"
                @click.stop="markAsRead(notification.id)"
              >
                Mark as Read
              </button>
            </div>
          </div>

          <div v-if="canOpen(notification)" class="notification-arrow">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
            </div>
          </div>
        </section>

        <PaginationBar
          :page="page"
          :page-size="pageSize"
          :total="filteredNotifications.length"
          item-label="notification"
          item-label-plural="notifications"
          @update:page="setPage"
          @update:page-size="setPageSize"
        />
      </div>

      <!-- EMPTY STATE -->
      <div v-else class="empty-state">
        <div class="empty-icon">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path>
            <path d="M13.73 21a2 2 0 0 1-3.46 0"></path>
          </svg>
        </div>
        <h3>No notifications found</h3>
        <p v-if="searchQuery || filterStatus !== 'all'">
          Try adjusting your search or filter criteria
        </p>
        <p v-else>
          You're all caught up! New notifications will appear here when there are updates on your proposals.
        </p>
      </div>
    </template>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { useUserDataStore } from '@/stores/userData'
import { useNotificationStore } from '@/stores/notifications'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import PaginationBar from '@/components/PaginationBar.vue'
import RelativeTime from '@/components/RelativeTime.vue'
import { useDialog } from '@/composables/useDialog'
import { usePagination } from '@/composables/usePagination.js'
import { resolveNotificationDestination } from '@/utils/notificationDestination'
import { announcementStatusLabel, announcementStatusTone } from '@/utils/announcementStatus'
import { statusTone } from '@/utils/statusTone.js'
import { groupNotifications, notificationKind } from '@/utils/notificationGroups'

const { dialogState, showAlert } = useDialog()

const router = useRouter()

const userStore = useUserDataStore()
const notificationStore = useNotificationStore()
const { notifications, loading, error, unreadCount } = storeToRefs(notificationStore)

const searchQuery = ref('')
const filterStatus = ref('all')
/** The kind of notification to show; the list stays newest-first within a kind. */
const filterKind = ref('all')

const filteredNotifications = computed(() => {
  let result = notifications.value

  // Filter by read/unread
  if (filterStatus.value === 'unread') {
    result = result.filter(n => !n.isRead)
  } else if (filterStatus.value === 'read') {
    result = result.filter(n => n.isRead)
  }

  // Filter by kind (proposal updates, calls, endorsements, ...)
  if (filterKind.value !== 'all') {
    result = result.filter(
      n => notificationKind(n, userStore.user?.role) === filterKind.value,
    )
  }

  // Filter by search query
  if (searchQuery.value.trim()) {
    const keyword = searchQuery.value.toLowerCase()
    result = result.filter(n =>
      n.title?.toLowerCase().includes(keyword) ||
      n.message?.toLowerCase().includes(keyword),
    )
  }

  // Sort a copy: the store's array must never be reordered by a filter view.
  return [...result].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

/**
 * The visible notifications, split so the proposal updates sit in their own group. Nothing
 * is dropped: the groups always hold every notification the filters left in, newest first.
 * The groups are always rendered in full - there is no collapsed or compact state.
 *
 * Two feeds are one strict reverse-chronological list: the Proponent's, because a proposal
 * update must never be pushed below an older call or endorsement row, and OVCRIGE's,
 * because every notification that office receives is a proposal update - a "Proposal
 * Updates" heading over the only group is just clutter.
 */
const notificationGroups = computed(() => {
  const role = userStore.user?.role
  if (role === 'PROPONENT' || role === 'OVCRIGE') {
    return pageNotifications.value.length
      ? [{ key: 'all', title: '', items: pageNotifications.value }]
      : []
  }

  const { proposalUpdates, others } = groupNotifications(
    pageNotifications.value,
    userStore.user?.role,
  )
  const groups = []
  if (others.length) {
    groups.push({ key: 'others', title: '', items: others })
  }
  if (proposalUpdates.length) {
    groups.push({
      key: 'proposal',
      title: 'Proposal Updates',
      items: proposalUpdates,
    })
  }
  return groups
})

// The list pages the filtered notifications. Groups are built from the current page, so a
// "Proposal Updates" heading only covers the rows actually rendered beneath it.
const {
  page,
  pageSize,
  pageItems: pageNotifications,
  setPage,
  setPageSize,
  reset: resetPage,
} = usePagination(filteredNotifications)

// Changing a filter is a criteria change: the list starts again on its first page.
watch([searchQuery, filterStatus, filterKind], resetPage)

const fetchNotifications = () => notificationStore.refresh(userStore.user?.id)

const markAsRead = (id) => notificationStore.markAsRead(id)

const markAllAsRead = () => notificationStore.markAllAsRead()

const canOpen = (notification) =>
  Boolean(resolveNotificationDestination(notification, userStore.user?.role))

const handleNotificationClick = async (notification) => {
  // The read-state write is best-effort and never blocks navigation.
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

  // A valid notification always carries a record reference; when the record is gone,
  // or this role may not open it, say so instead of silently landing on a Dashboard.
  await showAlert(
    'This notification is not linked to a record you can open.',
    { type: 'info', title: 'Nothing to open' },
  )
}

const formatType = (type) => {
  if (!type) return 'Notification'
  return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const getTypeClass = (type) => {
  const typeMap = {
    'PROPOSAL_UPDATE': 'type-proposal',
    'PROPOSAL_ENDORSED': 'type-proposal',
    'PROPOSAL_SUBMISSION': 'type-proposal',
    'REVIEW_REQUEST': 'type-review',
    'APPROVAL': 'type-approval',
    'REVISION': 'type-revision',
    'REJECTION': 'type-rejection',
    'ENDORSEMENT': 'type-endorsement',
    'FUNDING': 'type-funding',
    'PROPOSAL_ANNOUNCEMENT': 'type-default',
    'QUARTERLY_REPORT': 'type-proposal',
    'PROJECT_COMPLETION': 'type-approval',
    'DEAN_ASSIGNMENT': 'type-default',
    'SPECIAL_ORDER_DRAFT': 'type-funding',
    'REVISION_DEADLINE_REMINDER': 'type-revision',
    'EVALUATION_DEADLINE_REMINDER': 'type-revision',
  }
  return typeMap[type] || 'type-default'
}

/**
 * The status badge of a Call for Proposals notification, or null when the notification is
 * not about a call. The status is the one the backend derived at read time from the saved
 * call (stored status plus its current dates), so an extended call that is still open
 * reads as "Open" rather than the status it had when the notification was raised. The
 * label is always rendered together with the colour, so the status never relies on colour
 * alone.
 */
const callStatus = (notification) => {
  const label = announcementStatusLabel(notification?.proposalAnnouncementStatus)
  const tone = announcementStatusTone(notification?.proposalAnnouncementStatus)
  return label && tone ? { label, tone } : null
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notification-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h1 {
  font-size: 28px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.badge {
  background: #ef4444;
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* Layout only; the action colours come from assets/actions.css. */
.btn-mark-all {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-mark-all:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-mark-all:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* FILTER BAR */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
}

.search-input {
  flex: 1;
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  transition: all 0.15s;
}

.search-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.filter-select {
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 14px;
  background: #fff;
  cursor: pointer;
  min-width: 180px;
}

.filter-select:focus {
  outline: none;
  border-color: #4f46e5;
}

/* LOADING & ERROR */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-text {
  color: #dc2626;
  font-size: 14px;
}

/* Layout only; the action colours come from assets/actions.css. */
.btn-retry {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* NOTIFICATION GROUPS */
.notification-group + .notification-group {
  margin-top: 20px;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 12px 16px;
  margin-bottom: 12px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 10px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 700;
  color: #3730a3;
  text-align: left;
}

.group-count {
  margin-left: auto;
  min-width: 26px;
  padding: 2px 8px;
  background: #4f46e5;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

/* NOTIFICATIONS LIST */
.notifications-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.notification-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  cursor: pointer;
  transition: all 0.15s;
}

.notification-card:hover {
  border-color: #4f46e5;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.1);
  transform: translateY(-2px);
}

.notification-card.unread {
  background: #f8faff;
  border-left: 4px solid #4f46e5;
}

.notification-card.read {
  opacity: 0.75;
}

.notification-card.read:hover {
  opacity: 1;
}

/* ICON */
.notification-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.unread-dot {
  width: 12px;
  height: 12px;
  background: #4f46e5;
  border-radius: 50%;
}

.read-icon {
  color: #10b981;
}

/* CONTENT */
.notification-content {
  flex: 1;
  min-width: 0;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
  gap: 12px;
}

.notification-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow-wrap: anywhere;
}

.notification-time {
  font-size: 12px;
  color: #94a3b8;
  white-space: nowrap;
}

.notification-message {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0 0 12px 0;
  white-space: pre-line;
  overflow-wrap: anywhere;
}

/* CALL FOR PROPOSAL STATUS */
.notification-call {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.call-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.4;
}

.call-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.call-name {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.notification-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.notification-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  min-width: 0;
}

.notification-proposal {
  padding: 4px 10px;
  border-radius: 6px;
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  overflow-wrap: anywhere;
}

.notification-type {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 6px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.type-proposal { background: #dbeafe; color: #1e40af; }
.type-review { background: #fef3c7; color: #92400e; }
.type-approval { background: #d1fae5; color: #065f46; }
.type-revision { background: #e0e7ff; color: #3730a3; }
.type-rejection { background: #fee2e2; color: #991b1b; }
.type-endorsement { background: #fce7f3; color: #9d174d; }
.type-funding { background: #ecfdf5; color: #065f46; }
.type-default { background: #f1f5f9; color: #475569; }

/* Layout only; the action colours come from assets/actions.css. */
.btn-mark-read {
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

/* ARROW */
.notification-arrow {
  color: #94a3b8;
  flex-shrink: 0;
  margin-top: 4px;
}

.notification-card:hover .notification-arrow {
  color: #4f46e5;
}

/* EMPTY STATE */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-icon {
  color: #cbd5e1;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 20px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.empty-state p {
  font-size: 14px;
  color: #64748b;
  max-width: 400px;
  line-height: 1.6;
  margin: 0;
}

/* RESPONSIVE */
@media (max-width: 640px) {
  .notification-page {
    padding: 16px;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-select {
    width: 100%;
  }

  .notification-header {
    flex-direction: column;
  }

  .notification-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>