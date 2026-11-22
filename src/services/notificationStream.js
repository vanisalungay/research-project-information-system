import { createSseParser } from '../utils/sse.js'

const DEFAULT_WATCHDOG_MS = 60_000

/** The bearer token the Axios client would send, or null when signed out. */
export function currentAuthToken() {
  try {
    const stored = localStorage.getItem('user_data')
    if (!stored) return null
    const user = JSON.parse(atob(stored))
    return user?.token || null
  } catch {
    return null
  }
}

/**
 * A fetch-based SSE connection.
 *
 * `EventSource` cannot send the Bearer header the API requires, so the response body is
 * read as a stream and fed to the shared frame parser. The caller owns reconnection:
 * this object only reports that the stream opened, delivered an event, or failed.
 */
export function createNotificationStream({
  // The caller passes the same API base URL the Axios client uses. A relative default
  // keeps this module importable by the Node tests without pulling in Axios.
  baseUrl = '',
  getToken = currentAuthToken,
  onEvent = () => {},
  onOpen = () => {},
  onError = () => {},
  watchdogMs = DEFAULT_WATCHDOG_MS,
  fetchImpl = typeof fetch === 'function' ? fetch.bind(globalThis) : null,
} = {}) {
  let controller = null
  let watchdog = null
  let running = false

  const clearWatchdog = () => {
    if (watchdog !== null) {
      clearTimeout(watchdog)
      watchdog = null
    }
  }

  const armWatchdog = () => {
    clearWatchdog()
    if (!watchdogMs || watchdogMs <= 0) return
    watchdog = setTimeout(() => {
      if (!running) return
      stop()
      onError(Object.assign(new Error('Notification stream timed out.'), { watchdog: true }))
    }, watchdogMs)
  }

  const stop = () => {
    running = false
    clearWatchdog()
    if (controller) {
      controller.abort()
      controller = null
    }
  }

  const connect = async () => {
    if (!fetchImpl) {
      onError(new Error('Streaming is not supported by this browser.'))
      return
    }
    const token = getToken()
    if (!token) {
      stop()
      onError(Object.assign(new Error('Not signed in.'), { status: 401 }))
      return
    }

    running = true
    controller = new AbortController()
    const parser = createSseParser((event, payload) => {
      armWatchdog()
      onEvent(event, payload)
    })

    try {
      const response = await fetchImpl(`${baseUrl}/api/notifications/stream`, {
        method: 'GET',
        headers: {
          Authorization: `Bearer ${token}`,
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache',
        },
        cache: 'no-store',
        signal: controller.signal,
      })

      if (!response.ok || !response.body) {
        const error = new Error(`Notification stream failed (${response.status}).`)
        error.status = response.status
        stop()
        onError(error)
        return
      }

      onOpen()
      armWatchdog()

      const reader = response.body.getReader()
      const decoder = new TextDecoder()

      while (running) {
        const { value, done } = await reader.read()
        if (done) break
        parser.push(decoder.decode(value, { stream: true }))
      }

      if (running) {
        // The server closed the connection; the store decides whether to retry.
        stop()
        onError(new Error('Notification stream closed.'))
      }
    } catch (error) {
      const wasAborted = error?.name === 'AbortError' || !running
      stop()
      if (!wasAborted) onError(error)
    }
  }

  return {
    start() {
      if (running) return
      connect()
    },
    stop,
    isRunning: () => running,
  }
}
