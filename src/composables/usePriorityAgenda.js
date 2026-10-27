import { computed, ref } from 'vue'

import api from '@/utils/api'
import {
  agendaLabelIndex,
  agendaPrograms as toPrograms,
  frameworkLabel,
} from '@/utils/priorityAgendas'

/**
 * usePriorityAgenda.js
 * ---------------------------------------------------------------------------
 * Loads the Priority Agenda master data (section 05) from the backend, so the
 * framework heading and the agenda checkboxes always reflect what an administrator
 * configured - never a hard-coded list.
 *
 * Two reads are exposed:
 *   * `load()`               the ACTIVE configuration (framework name, coverage
 *                            period and its active agenda items), used by the
 *                            Create Proposal form, the Revision form and the
 *                            Create/Edit Call for Proposal form;
 *   * `resolveLabels(keys)`  the display names of arbitrary keys, including the
 *                            keys of a deactivated framework, used by the
 *                            printouts/exports of a saved proposal.
 *
 * The three states the callers must handle are kept apart on purpose: `loading`,
 * `error` and "loaded but empty". A failed request never silently falls back to
 * hard-coded agendas.
 */

/** Reads the readable message out of an axios error (mirrors the admin views). */
function extractErrorMessage(err, fallback) {
  const data = err?.response?.data
  if (data && typeof data === 'object' && data.error) return data.error
  if (typeof data === 'string' && data.trim()) return data
  return fallback
}

export function usePriorityAgenda() {
  const agendaConfig = ref(null)
  const loading = ref(false)
  const error = ref(null)
  const loaded = ref(false)

  /** The active agenda items, in display order: `[{ key, label, subLabel }]`. */
  const agendaPrograms = computed(() => toPrograms(agendaConfig.value?.agendas))

  /** The heading beside "Priority Agenda", e.g. "(based on MSUN RIIDE 2025–2028)". */
  const agendaLabel = computed(() => frameworkLabel(agendaConfig.value))

  const hasAgendas = computed(() => agendaPrograms.value.length > 0)

  /** Loads (or reloads) the active configuration. Resolves to it, or null on error. */
  async function load() {
    loading.value = true
    error.value = null
    try {
      const response = await api.get('/api/priority-agendas/active')
      agendaConfig.value = response.data || null
      return agendaConfig.value
    } catch (err) {
      console.error(err)
      error.value = extractErrorMessage(err, 'Failed to load the priority agenda configuration.')
      agendaConfig.value = null
      return null
    } finally {
      loading.value = false
      loaded.value = true
    }
  }

  /**
   * Resolves the display names of the given stable keys (active or not) to
   * `{ agendaKey: { name, subLabel } }`. A failure resolves to an empty index, so a
   * printout degrades to the raw keys instead of breaking.
   */
  async function resolveLabels(keys) {
    const requested = (Array.isArray(keys) ? keys : []).filter(Boolean)
    if (requested.length === 0) return {}
    try {
      const response = await api.get('/api/priority-agendas/lookup', {
        params: { keys: requested.join(',') },
      })
      return agendaLabelIndex(response.data)
    } catch (err) {
      console.error(err)
      return {}
    }
  }

  return {
    agendaConfig,
    agendaPrograms,
    agendaLabel,
    hasAgendas,
    loading,
    error,
    loaded,
    load,
    resolveLabels,
  }
}
