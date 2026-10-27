<template>
  <div class="cooperating-agencies">
    <!-- The checklist is the shared backend directory, never a hardcoded array: it
         holds the agencies an administrator has configured for the university, and
         it is read-only here (a proponent can select, never create). -->
    <p v-if="loading" class="ca-status">Loading cooperating agencies…</p>

    <div v-else-if="loadError" class="ca-error-block">
      <p class="ca-status ca-status-error" role="alert">{{ loadError }}</p>
      <button type="button" class="ca-retry" @click="loadAgencies()">Retry</button>
    </div>

    <p v-else-if="agencies.length === 0" class="ca-status">
      No cooperating agencies are available yet. Please contact the administrator to request an
      addition.
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
      agencies above.
    </p>

    <!-- The directory is administrator-managed: this form can only select from it,
         so an agency that is missing has to be requested instead of being created. -->
    <p v-if="!loading && !loadError && agencies.length > 0" class="ca-admin-note">
      Need an agency that is not listed? Please contact the administrator to request its addition.
    </p>
  </div>
</template>

<script setup>
/**
 * CooperatingAgenciesChecklist.vue
 * ---------------------------------------------------------------------------
 * Section 02 of the Create Proposal form: a checklist of the cooperating agency
 * directory an administrator maintains for the university.
 *
 * The checklist is loaded from the backend (GET /api/cooperating-agencies) and is
 * read-only here: a proponent selects the agencies that cooperate on the proposal
 * and can never create one. An agency that is missing is added by an administrator
 * - POST /api/cooperating-agencies is restricted to the RPS roles - so the global
 * directory cannot be changed from the proposal form.
 * Editing a saved proposal restores its selection from
 * GET /api/proposals/{id}/cooperating-agencies.
 */
import { computed, onMounted, ref, watch } from 'vue'

import api from '@/utils/api'
import { normalizeSelectedIds, sortAgenciesByName } from '@/utils/cooperatingAgencies'

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

/* Shown below the checklist: the directory is administrator-managed, so a missing
   agency is requested rather than created here. */
.ca-admin-note {
  margin: 0;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}
</style>
