/**
 * notifications.js
 * ---------------------------------------------------------------------------
 * The unread notification count the shell shows (the dot on the header bell and the
 * count badge next to the sidebar's Notifications item).
 *
 * The number always comes from the same data the notification pages read
 * (GET /api/notifications?userId=...) - nothing is hardcoded and no separate endpoint
 * is introduced. The count is shared through this store so the header and the sidebar
 * never fetch or keep two copies of it.
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import api from '@/utils/api'
import { unreadCountOf } from '@/utils/notifications'

export const useNotificationStore = defineStore('notifications', () => {
  /** The signed-in user whose notifications are counted. */
  const userId = ref(null)
  const unreadCount = ref(0)
  const loading = ref(false)

  /** True while there is something new to show on the bell / badge. */
  const hasUnread = computed(() => unreadCount.value > 0)

  /**
   * Re-reads the current user's notifications and updates the count. A failure keeps
   * the last known count instead of showing a wrong "0 unread".
   */
  async function refresh(currentUserId = null) {
    const id = currentUserId ?? userId.value
    if (id == null) {
      userId.value = null
      unreadCount.value = 0
      return unreadCount.value
    }
    userId.value = id
    loading.value = true
    try {
      const res = await api.get('/api/notifications', { params: { userId: id } })
      unreadCount.value = unreadCountOf(res.data)
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

  /** Called on logout so the next user never sees the previous count. */
  function reset() {
    userId.value = null
    unreadCount.value = 0
  }

  return { userId, unreadCount, hasUnread, loading, refresh, reset }
})
