<template>
  <div class="cooperating-agencies">
    <!-- The checklist is the shared backend directory, never a hardcoded array:
         an agency added by any proponent shows up here for everyone. -->
    <p v-if="loading" class="ca-status">Loading cooperating agencies…</p>

    <div v-else-if="loadError" class="ca-error-block">
      <p class="ca-status ca-status-error" role="alert">{{ loadError }}</p>
      <button type="button" class="ca-retry" @click="loadAgencies()">Retry</button>
    </div>

    <p v-else-if="agencies.length === 0" class="ca-status">
      No cooperating agencies are available yet. Add the first one below.
    </p>

    <div v-else class="ca-checklist">
      <label v-for="agency in agencies" :key="agency.id" class="ca-checkbox">
        <input type="checkbox" :value="agency.id" v-model="selectedIds" />
        <span>{{ agency.name }}</span>
      </label>
    </div>

    <!-- A proposal saved before the checklist existed carries free text only. It is
         never dropped: it is shown so the proponent can transfer it to the list. -->
    <p v-if="legacySelection && selectedIds.length === 0" class="ca-legacy-note">
      Agencies saved in the previous format: &ldquo;{{ legacySelection }}&rdquo;. Please select the
      agencies below.
    </p>

    <div class="ca-add">
      <label class="ca-add-label" for="ca-new-agency">Add Another Agency</label>
      <div class="ca-add-row">
        <input
          id="ca-new-agency"
          v-model="newAgencyName"
          type="text"
          class="ca-add-input"
          placeholder="Enter cooperating agency name"
          :disabled="adding"
          @keyup.enter.prevent="addAgency"
        />
        <button
          type="button"
          class="ca-add-btn"
          :disabled="adding || newAgencyName.trim() === ''"
          @click="addAgency"
        >
          {{ adding ? 'Adding…' : 'Add Agency' }}
        </button>
      </div>
      <p v-if="addError" class="ca-status ca-status-error" role="alert">{{ addError }}</p>
      <p v-else-if="addSuccess" class="ca-status ca-status-success" role="status">
        {{ addSuccess }}
      </p>
      <p v-else class="ca-hint">
        An agency typed here is only saved when you click Add Agency. A saved agency is offered to
        every proponent.
      </p>
    </div>
  </div>
</template>

<script setup>
/**
 * CooperatingAgenciesChecklist.vue
 * ---------------------------------------------------------------------------
 * Section 02 of the Create Proposal form: a checklist of the shared cooperating
 * agency directory plus the "Add Another Agency" action.
 *
 * The list is loaded from the backend (GET /api/cooperating-agencies) so an agency
 * added by one proponent is offered to every other proponent. A new agency is only
 * stored when the proponent explicitly clicks Add Agency, is validated (including a
 * case-insensitive duplicate check) before and after the request, and is selected
 * for the current proposal right away - the proponent can still uncheck it.
 * Editing a saved proposal restores its selection from
 * GET /api/proposals/{id}/cooperating-agencies.
 */
import { computed, onMounted, ref, watch } from 'vue'

import api from '@/utils/api'
import {
  findAgencyByName,
  mergeAgency,
  normalizeAgencyName,
  normalizeSelectedIds,
  sortAgenciesByName,
  validateAgencyName,
} from '@/utils/cooperatingAgencies'

const props = defineProps({
  /** Ids of the agencies selected for this proposal (v-model:selected). */
  selected: { type: Array, default: () => [] },
  /** Free text saved before the checklist existed; shown while nothing is selected. */
  legacySelection: { type: String, default: '' },
  /** Saved proposal being edited: its selection is restored from the backend. */
  proposalId: { type: [Number, String], default: null },
})
const emit = defineEmits(['update:selected', 'update:names'])

const agencies = ref([])
const loading = ref(false)
const loadError = ref('')
const adding = ref(false)
const addError = ref('')
const addSuccess = ref('')
const newAgencyName = ref('')
const restoredProposalId = ref(null)

const selectedIds = computed({
  get: () => normalizeSelectedIds(props.selected),
  set: (ids) => emit('update:selected', normalizeSelectedIds(ids)),
})

// The names travel with the ids so the form can keep the legacy text column and
// its printout in step with the checklist without loading the directory again.
const selectedNames = computed(() => {
  const selected = new Set(selectedIds.value)
  return agencies.value
    .filter((agency) => selected.has(Number(agency.id)))
    .map((agency) => agency.name)
})
watch(selectedNames, (names) => emit('update:names', [...names]), { immediate: true })

/** Loads the shared directory (and the saved selection when editing). */
const loadAgencies = async ({ silent = false } = {}) => {
  if (!silent) {
    loading.value = true
    loadError.value = ''
  }
  try {
    const res = await api.get('/api/cooperating-agencies')
    agencies.value = sortAgenciesByName(Array.isArray(res.data) ? res.data : [])
    await restoreSavedSelection()
  } catch (err) {
    console.error(
      'Failed to load cooperating agencies:',
      err?.response?.status,
      err?.response?.data || err?.message,
    )
    if (!silent) {
      agencies.value = []
      loadError.value =
        'Unable to load the cooperating agencies. Please check your connection and try again.'
    }
  } finally {
    if (!silent) loading.value = false
  }
}

/**
 * Restores the selection of a saved proposal. The ids live in their own endpoint
 * (the proposal response keeps the legacy text), so this runs once per proposal, as
 * soon as the directory is available.
 */
const restoreSavedSelection = async () => {
  const id = props.proposalId
  if (!id || restoredProposalId.value === id) return
  try {
    const res = await api.get(`/api/proposals/${id}/cooperating-agencies`)
    const saved = normalizeSelectedIds(
      (Array.isArray(res.data) ? res.data : []).map((agency) => agency.id),
    )
    restoredProposalId.value = id
    if (saved.length) selectedIds.value = saved
  } catch (err) {
    // Never block the form: the proponent can still select the agencies by hand.
    console.error(
      'Failed to restore the saved cooperating agencies:',
      err?.response?.status,
      err?.response?.data || err?.message,
    )
  }
}

/** Saves a new agency through the backend, then adds and selects it here. */
const addAgency = async () => {
  if (adding.value) return
  const name = normalizeAgencyName(newAgencyName.value)
  addSuccess.value = ''
  const invalid = validateAgencyName(name, agencies.value)
  if (invalid) {
    addError.value = invalid
    return
  }

  adding.value = true
  addError.value = ''
  try {
    const res = await api.post('/api/cooperating-agencies', { name })
    const created = res.data
    agencies.value = mergeAgency(agencies.value, created)
    // Selected for this proposal right away; the proponent may still uncheck it.
    selectedIds.value = [...selectedIds.value, Number(created.id)]
    newAgencyName.value = ''
    addSuccess.value = `“${created.name}” was saved to the cooperating agencies list and selected for this proposal.`
  } catch (err) {
    await reportAddFailure(err, name)
  } finally {
    adding.value = false
  }
}

/** Turns a failed add into the message the proponent sees. */
const reportAddFailure = async (err, name) => {
  const status = err?.response?.status
  const message = err?.response?.data?.error

  if (status === 409) {
    // Already in the directory - possibly added by someone else after this form was
    // opened, so the list is refreshed and the existing agency is selected instead.
    addError.value = message || `${name} is already in the cooperating agencies list.`
    try {
      await loadAgencies({ silent: true })
      const existing = findAgencyByName(agencies.value, name)
      if (existing) {
        selectedIds.value = [...selectedIds.value, Number(existing.id)]
        addError.value = `${existing.name} is already in the cooperating agencies list. It has been selected for this proposal.`
      }
    } catch (reloadError) {
      console.error('Failed to refresh the cooperating agencies:', reloadError?.message)
    }
    return
  }

  if (status === 400) {
    addError.value = message || 'Please check the agency name and try again.'
    return
  }
  addError.value = 'Unable to save the agency right now. Please try again.'
}

onMounted(loadAgencies)
watch(
  () => props.proposalId,
  (id) => {
    // A different draft was opened in the same form: restore its selection too.
    restoredProposalId.value = null
    if (id) loadAgencies({ silent: true })
  },
)

defineExpose({ loadAgencies })
</script>

<style scoped>
/* Kept in step with the SDG checklist and the rest of section 02: same grid, same
   typography and the same indigo accent, so the section looks unchanged. */
.cooperating-agencies {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.ca-status {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

.ca-status-error {
  color: #b91c1c;
}

.ca-status-success {
  color: #15803d;
}

.ca-hint {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

.ca-error-block {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.ca-retry {
  padding: 0;
  border: none;
  background: none;
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
  text-decoration: underline;
  cursor: pointer;
}

.ca-checklist {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 6px 16px;
}

.ca-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin: 0;
  padding: 6px 8px;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.4;
  color: #475569;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.15s;
}

.ca-checkbox:hover {
  background: #f8fafc;
}

.ca-checkbox input {
  width: 16px;
  height: 16px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  accent-color: #4f46e5;
  cursor: pointer;
  flex-shrink: 0;
  vertical-align: middle;
}

.ca-legacy-note {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

.ca-add {
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.ca-add-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.ca-add-row {
  display: flex;
  align-items: stretch;
  gap: 10px;
  flex-wrap: wrap;
}

.ca-add-input {
  flex: 1 1 240px;
  min-width: 0;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  font-family: inherit;
  color: #1e293b;
  background: #fff;
  box-sizing: border-box;
}

.ca-add-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.ca-add-input::placeholder {
  color: #94a3b8;
}

.ca-add-btn {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: #fff;
  background: #4f46e5;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background 0.15s,
    opacity 0.15s;
}

.ca-add-btn:hover:not(:disabled) {
  background: #4338ca;
}

.ca-add-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* The button takes the full width on a phone so the input stays usable. */
@media (max-width: 560px) {
  .ca-add-btn {
    width: 100%;
  }
}
</style>
