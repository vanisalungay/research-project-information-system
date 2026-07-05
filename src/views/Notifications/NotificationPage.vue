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
          class="btn-mark-all" 
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
    </div>

    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading notifications...</p>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="error" class="error-state">
      <p class="error-text">{{ error }}</p>
      <button class="btn-retry" @click="fetchNotifications">Retry</button>
    </div>

    <!-- NOTIFICATIONS LIST -->
    <template v-else>
      <div v-if="filteredNotifications.length > 0" class="notifications-list">
        <div 
          v-for="notification in filteredNotifications" 
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
              <span class="notification-time">{{ formatDate(notification.createdAt) }}</span>
            </div>
            <p class="notification-message">{{ notification.message }}</p>
            <div class="notification-footer">
              <span class="notification-type" :class="getTypeClass(notification.type)">
                {{ formatType(notification.type) }}
              </span>
              <button 
                v-if="!notification.isRead" 
                class="btn-mark-read"
                @click.stop="markAsRead(notification.id)"
              >
                Mark as Read
              </button>
            </div>
          </div>

          <div class="notification-arrow">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6"></polyline>
            </svg>
          </div>
        </div>
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
import { ref, computed, onMounted } from 'vue'
import { useUserDataStore } from '@/stores/userData'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const userStore = useUserDataStore()

const notifications = ref([])
const loading = ref(false)
const error = ref(null)
const searchQuery = ref('')
const filterStatus = ref('all')

const unreadCount = computed(() => {
  return notifications.value.filter(n => !n.isRead).length
})

const filteredNotifications = computed(() => {
  let result = notifications.value

  // Filter by read/unread
  if (filterStatus.value === 'unread') {
    result = result.filter(n => !n.isRead)
  } else if (filterStatus.value === 'read') {
    result = result.filter(n => n.isRead)
  }

  // Filter by search query
  if (searchQuery.value.trim()) {
    const keyword = searchQuery.value.toLowerCase()
    result = result.filter(n => 
      n.title?.toLowerCase().includes(keyword) ||
      n.message?.toLowerCase().includes(keyword)
    )
  }

  // Sort by date (newest first)
  return result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const fetchNotifications = async () => {
  loading.value = true
  error.value = null
  try {
    const userId = userStore.user?.id
    if (!userId) {
      error.value = 'User not authenticated'
      return
    }
    const res = await api.get(`/api/notifications?userId=${userId}`)
    notifications.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load notifications'
  } finally {
    loading.value = false
  }
}

const markAsRead = async (id) => {
  try {
    await api.put(`/api/notifications/${id}/read`)
    const notification = notifications.value.find(n => n.id === id)
    if (notification) {
      notification.isRead = true
    }
  } catch (err) {
    console.error(err)
    await showAlert('Failed to mark notification as read', { type: 'error', title: 'Error' })
  }
}

const markAllAsRead = async () => {
  try {
    const userId = userStore.user?.id
    await api.put('/api/notifications/read-all', null, {
      params: { userId }
    })
    notifications.value.forEach(n => n.isRead = true)
  } catch (err) {
    console.error(err)
    await showAlert('Failed to mark all notifications as read', { type: 'error', title: 'Error' })
  }
}

const handleNotificationClick = (notification) => {
  // Mark as read when clicked
  if (!notification.isRead) {
    markAsRead(notification.id)
  }
  
  // Navigate to related proposal if available
  if (notification.proposalId) {
    // You can customize the navigation based on user role
    window.location.href = `/detailed-proposal?id=${notification.proposalId}`
  }
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  const date = new Date(dateString)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 1) return 'Just now'
  if (minutes < 60) return `${minutes} minute${minutes > 1 ? 's' : ''} ago`
  if (hours < 24) return `${hours} hour${hours > 1 ? 's' : ''} ago`
  if (days < 7) return `${days} day${days > 1 ? 's' : ''} ago`
  
  return date.toLocaleDateString('en-US', {
    month: 'short',
    day: 'numeric',
    year: date.getFullYear() !== now.getFullYear() ? 'numeric' : undefined
  })
}

const formatType = (type) => {
  if (!type) return 'Notification'
  return type.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase())
}

const getTypeClass = (type) => {
  const typeMap = {
    'PROPOSAL_UPDATE': 'type-proposal',
    'REVIEW_REQUEST': 'type-review',
    'APPROVAL': 'type-approval',
    'REVISION': 'type-revision',
    'REJECTION': 'type-rejection',
    'ENDORSEMENT': 'type-endorsement',
    'FUNDING': 'type-funding'
  }
  return typeMap[type] || 'type-default'
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

.btn-mark-all {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-mark-all:hover:not(:disabled) {
  background: #4338ca;
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

.btn-retry {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
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
}

.notification-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
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

.btn-mark-read {
  background: none;
  border: 1px solid #4f46e5;
  color: #4f46e5;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-mark-read:hover {
  background: #4f46e5;
  color: #fff;
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