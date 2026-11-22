/**
 * notifications.js
 * ---------------------------------------------------------------------------
 * The single source of truth for the signed-in account's notifications: the list, the
 * unread badge, the mark-read actions, and the live SSE connection.
 *
 * Every surface (header bell, sidebar badge, notifications page, dashboard previews)
 * reads this store, so marking one item read updates all of them immediately instead of
 * waiting for a route change. The server remains authoritative: a failed write reverts
 * the optimistic change and re-reads the persisted list.
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import api, { API_BASE_URL } from '@/utils/api'
import { recentNotifications, unreadCountOf } from '@/utils/notifications'
import { createNotificationStream } from '@/services/notificationStream'

/** How many notifications a dashboard preview shows. */
const RECENT_LIMIT = 5

/** How often the list is reconciled while the tab is visible. */
const RECONCILE_MS = 90_000

/** Coalesces a burst of live events into one list request. */
const EVENT_REFRESH_DELAY_MS = 250

const RETRY_BASE_MS = 1_000
const RETRY_MAX_MS = 30_000

export const useNotificationStore = defineStore('notifications', () => {
  const userId = ref(null)
  const notifications = ref([])
  const loading = ref(false)
  const error = ref(null)
  /** 'off' | 'connecting' | 'live' | 'retrying' | 'unauthorized' */
  const streamState = ref('off')

  let inFlight = null
  let inFlightUserId = null
  let connection = null
  let retryTimer = null
  let retryDelay = RETRY_BASE_MS
  let refreshTimer = null
  let reconcileTimer = null
  let stopped = false
  let windowListenersAttached = false

  const unreadCount = computed(() => unreadCountOf(notifications.value))
  const hasUnread = computed(() => unreadCount.value > 0)
  const recent = computed(() => recentNotifications(notifications.value, RECENT_LIMIT))

  /**
   * Re-reads the current account's notifications. Concurrent callers (a live event and
   * a focus refresh, for example) share one request. A failure keeps the last known
   * list instead of showing a misleading empty list.
   */
  async function refresh(currentUserId = null) {
    const id = currentUserId ?? userId.value
    if (id == null) {
      notifications.value = []
      return 0
    }
    userId.value = id

    if (inFlight && inFlightUserId === id) return inFlight

    inFlightUserId = id
    loading.value = true
    inFlight = (async () => {
      try {
        const res = await api.get('/api/notifications')
        notifications.value = Array.isArray(res.data) ? res.data : []
        error.value = null
      } catch (requestError) {
        error.value = 'Failed to load notifications'
        console.error(
          'Failed to load notifications:',
          requestError?.response?.status,
          requestError?.response?.data || requestError?.message,
        )
      } finally {
        loading.value = false
        inFlight = null
        inFlightUserId = null
      }
      return unreadCount.value
    })()

    return inFlight
  }

  function applyReadState(notificationId) {
    const notification = notifications.value.find(
      (item) => String(item.id) === String(notificationId),
    )
    if (notification) notification.isRead = true
  }

  function applyAllRead() {
    for (const notification of notifications.value) {
      notification.isRead = true
    }
  }

  /** Marks one notification read locally first, then persists the change. */
  async function markAsRead(notificationId) {
    const notification = notifications.value.find(
      (item) => String(item.id) === String(notificationId),
    )
    if (!notification || notification.isRead) return
    notification.isRead = true
    try {
      await api.put(`/api/notifications/${notificationId}/read`)
    } catch (requestError) {
      notification.isRead = false
      console.error('Failed to mark notification as read:', requestError?.message)
      await refresh()
    }
  }

  /** Marks every visible notification read locally first, then persists the change. */
  async function markAllAsRead() {
    const unread = notifications.value.filter((item) => !item.isRead)
    if (unread.length === 0) return
    applyAllRead()
    try {
      await api.put('/api/notifications/read-all')
    } catch (requestError) {
      console.error('Failed to mark all notifications as read:', requestError?.message)
      for (const notification of unread) notification.isRead = false
      await refresh()
    }
  }

  function clearRefreshTimer() {
    if (refreshTimer !== null) {
      clearTimeout(refreshTimer)
      refreshTimer = null
    }
  }

  function scheduleRefresh(delay = EVENT_REFRESH_DELAY_MS) {
    if (refreshTimer !== null) return
    refreshTimer = setTimeout(() => {
      refreshTimer = null
      refresh(userId.value)
    }, delay)
  }

  function handleStreamEvent(event, payload) {
    if (event === 'notification') {
      // The row is persisted and the id is only a hint. A list refresh guarantees the
      // computed fields (formatted proposal id, announcement details) are correct.
      scheduleRefresh()
      return
    }
    if (event === 'notifications-changed') {
      // Another tab changed a read state; re-read so every badge agrees.
      scheduleRefresh(0)
      return
    }
    if (event === 'connected') {
      // The open handler already refreshed; nothing else to do.
      return
    }
    if (payload?.id && event === 'message') {
      scheduleRefresh()
    }
  }

  function clearRetryTimer() {
    if (retryTimer !== null) {
      clearTimeout(retryTimer)
      retryTimer = null
    }
  }

  function scheduleReconnect(delay = null) {
    if (stopped || userId.value == null) return
    clearRetryTimer()
    const wait = delay ?? Math.min(RETRY_MAX_MS, retryDelay)
    retryDelay = Math.min(RETRY_MAX_MS, Math.max(RETRY_BASE_MS, retryDelay * 2))
    const jittered = Math.round(wait * (0.8 + Math.random() * 0.4))
    streamState.value = 'retrying'
    retryTimer = setTimeout(() => {
      retryTimer = null
      openStream()
    }, jittered)
  }

  function openStream() {
    if (stopped || userId.value == null) return
    clearRetryTimer()
    streamState.value = 'connecting'
    connection = createNotificationStream({
      baseUrl: API_BASE_URL,
      onEvent: handleStreamEvent,
      onOpen: () => {
        retryDelay = RETRY_BASE_MS
        streamState.value = 'live'
        // Recover anything that happened while the stream was down.
        refresh(userId.value)
      },
      onError: (streamError) => {
        connection = null
        if (stopped) return
        if (streamError?.status === 401 || streamError?.status === 403) {
          streamState.value = 'unauthorized'
          return
        }
        scheduleReconnect()
      },
    })
    connection.start()
  }

  function handleVisibilityChange() {
    if (typeof document === 'undefined') return
    if (document.visibilityState !== 'visible') return
    refresh(userId.value)
    if (!connection || !connection.isRunning()) scheduleReconnect(0)
  }

  function handleFocusOrOnline() {
    refresh(userId.value)
    if (!connection || !connection.isRunning()) scheduleReconnect(0)
  }

  function attachWindowListeners() {
    if (windowListenersAttached || typeof window === 'undefined') return
    windowListenersAttached = true
    document.addEventListener('visibilitychange', handleVisibilityChange)
    window.addEventListener('focus', handleFocusOrOnline)
    window.addEventListener('online', handleFocusOrOnline)
  }

  function detachWindowListeners() {
    if (!windowListenersAttached || typeof window === 'undefined') return
    windowListenersAttached = false
    document.removeEventListener('visibilitychange', handleVisibilityChange)
    window.removeEventListener('focus', handleFocusOrOnline)
    window.removeEventListener('online', handleFocusOrOnline)
  }

  function startReconcile() {
    if (reconcileTimer !== null || typeof window === 'undefined') return
    reconcileTimer = window.setInterval(() => {
      if (typeof document === 'undefined' || document.visibilityState === 'visible') {
        refresh(userId.value)
      }
    }, RECONCILE_MS)
  }

  function stopReconcile() {
    if (reconcileTimer !== null) {
      window.clearInterval(reconcileTimer)
      reconcileTimer = null
    }
  }

  /** Opens the live stream for `currentUserId`. Safe to call on every login/route. */
  function connect(currentUserId) {
    if (currentUserId == null) {
      disconnect()
      return
    }
    if (userId.value === currentUserId && connection?.isRunning()) {
      return
    }
    disconnect()
    stopped = false
    userId.value = currentUserId
    attachWindowListeners()
    startReconcile()
    openStream()
  }

  /** Stops the stream and its timers but keeps the list that is already on screen. */
  function disconnect() {
    stopped = true
    clearRetryTimer()
    clearRefreshTimer()
    stopReconcile()
    detachWindowListeners()
    if (connection) {
      connection.stop()
      connection = null
    }
    streamState.value = 'off'
  }

  /** Called on logout: stop everything and forget the previous account's list. */
  function reset() {
    disconnect()
    userId.value = null
    notifications.value = []
    loading.value = false
    error.value = null
  }

  return {
    userId,
    notifications,
    loading,
    error,
    streamState,
    unreadCount,
    hasUnread,
    recent,
    refresh,
    markAsRead,
    markAllAsRead,
    applyReadState,
    applyAllRead,
    connect,
    disconnect,
    reset,
  }
})
