/**
 * deanAttention.js (store)
 * ---------------------------------------------------------------------------
 * The Dean's pending endorsement-request count, shared by the sidebar badge and the Dean
 * dashboard.
 *
 * It reads the same endpoint the endorsement pages read (GET /api/endorsements), which the
 * backend already scopes to the signed-in Dean's college, so a Dean can never count another
 * college's requests. Nothing is hardcoded and no new endpoint is introduced.
 */
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { ENDORSEMENT_STATUS, fetchEndorsementRequests } from '@/utils/endorsement.js'
import { UserRole } from '@/stores/userData'

export const useDeanAttentionStore = defineStore('deanAttention', () => {
  /** How many endorsement requests are still waiting for this Dean's decision. */
  const pendingCount = ref(0)
  const loading = ref(false)

  const hasPending = computed(() => pendingCount.value > 0)

  /**
   * Re-reads the pending requests of the signed-in Dean. Only a Dean has endorsement
   * requests to decide, so any other role (and a signed-out shell) resolves to zero
   * without a request. A failure keeps the last known count instead of showing a wrong 0.
   */
  async function refresh(role = null) {
    if (role !== UserRole.DEAN) {
      pendingCount.value = 0
      return pendingCount.value
    }
    loading.value = true
    try {
      const requests = await fetchEndorsementRequests({ status: ENDORSEMENT_STATUS.PENDING })
      pendingCount.value = requests.length
    } catch (error) {
      console.error('Failed to load the Dean pending request count:', error?.message)
    } finally {
      loading.value = false
    }
    return pendingCount.value
  }

  /** Called on logout so the next user never sees the previous count. */
  function reset() {
    pendingCount.value = 0
  }

  /**
   * The Dean dashboard already loads the Dean's requests, so it publishes the count it
   * computed from that same response instead of triggering a second request.
   */
  function setPendingCount(value) {
    const n = Number(value)
    pendingCount.value = Number.isFinite(n) && n > 0 ? Math.trunc(n) : 0
  }

  return { pendingCount, loading, hasPending, refresh, reset, setPendingCount }
})
