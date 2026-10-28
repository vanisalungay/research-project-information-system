/**
 * notifications.js
 * ---------------------------------------------------------------------------
 * The notification state the shell shows (the dot on the header bell and the count badge
 * next to the sidebar's Notifications item) plus the list those numbers are counted from.
 *
 * Everything comes from the same data the notification pages read
 * (GET /api/notifications?userId=...) - nothing is hardcoded and no separate endpoint is
 * introduced. The count and the list are shared through this store, so the header, the
 * sidebar and any page that previews notifications never fetch or keep two copies of the
 * same records.
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import api from '@/utils/api'
import { recentNotifications, unreadCountOf } from '@/utils/notifications'

/** How many notifications a preview (the dashboard) shows. */
const RECENT_LIMIT = 5

export const useNotificationStore = defineStore('notifications', () => {
  /** The signed-in user whose notifications are counted. */
  const userId = ref(null)
  const unreadCount = ref(0)
  const loading = ref(false)
  /** The list the count was computed from, newest first as the API returns it. */
  const notifications = ref([])

  /** True while there is something new to show on the bell / badge. */
  const hasUnread = computed(() => unreadCount.value > 0)

  /** The most recent notifications, for a short preview on a dashboard. */
  const recent = computed(() => recentNotifications(notifications.value, RECENT_LIMIT))

  /**
   * Re-reads the current user's notifications and updates the count and the list. A
   * failure keeps the last known values instead of showing a wrong "0 unread" or an
   * empty list.
   */
  async function refresh(currentUserId = null) {
    const id = currentUserId ?? userId.value
    if (id == null) {
      userId.value = null
      unreadCount.value = 0
      notifications.value = []
      return unreadCount.value
    }
    userId.value = id
    loading.value = true
    try {
      const res = await api.get('/api/notifications', { params: { userId: id } })
      notifications.value = Array.isArray(res.data) ? res.data : []
      unreadCount.value = unreadCountOf(notifications.value)
    } catch (error) {
      console.error(
        'Failed to load the notification count:',
        error?.response?.status,
        error?.response?.data || error?.message,
      )
    } finally {
      loading.value = false
    }
    return unreadCount.value
  }

  /** Called on logout so the next user never sees the previous count or list. */
  function reset() {
    userId.value = null
    unreadCount.value = 0
    notifications.value = []
  }

  return { userId, unreadCount, hasUnread, loading, notifications, recent, refresh, reset }
})
