import { onMounted, onUnmounted, readonly, ref } from 'vue'

import { RELATIVE_TIME_TICK_MS } from '@/utils/relativeTime'

/**
 * One shared clock for every relative timestamp on the page.
 *
 * Each component subscribes to the same ref and the module runs a single interval while
 * at least one component is mounted. A long notification list therefore has one timer,
 * not one per card, and the timer is cleared as soon as the last card unmounts.
 */
const now = ref(Date.now())
let subscribers = 0
let timer = null

function startTicker() {
  subscribers += 1
  // Refresh immediately: after the last label unmounted the timer stopped, so `now`
  // may be old. A newly opened panel must not show a timestamp that is 10 minutes
  // stale until the first interval fires.
  now.value = Date.now()
  if (timer !== null || typeof window === 'undefined') return
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, RELATIVE_TIME_TICK_MS)
}

function stopTicker() {
  subscribers = Math.max(0, subscribers - 1)
  if (subscribers === 0 && timer !== null) {
    window.clearInterval(timer)
    timer = null
  }
}

/** A shared, ticking "now" for relative labels. */
export function useNow() {
  onMounted(startTicker)
  onUnmounted(stopTicker)
  return readonly(now)
}
