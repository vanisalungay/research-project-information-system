<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>Priority Agendas</h2>
        <p class="subtitle">
          Manage the framework (program name and coverage period) and the agenda list the
          Proponent "Submit New Proposal" form offers. Only one framework is active for new
          submissions at a time.
        </p>
      </div>
      <button class="create-btn" @click="openCreateFramework">+ Create Framework</button>
    </div>

    <!-- Which framework new proposals currently select from -->
    <div v-if="activeSet" class="banner banner-open">
      <strong>Active framework:</strong>&nbsp;{{ activeSet.frameworkName }}
      ({{ activeSet.coverageStartYear }}–{{ activeSet.coverageEndYear }}) ·
      {{ activeItemCount(activeSet) }} agenda{{ activeItemCount(activeSet) === 1 ? '' : 's' }} offered
    </div>
    <div v-else-if="!loading && !error" class="banner banner-closed">
      <strong>No framework is active.</strong>&nbsp;The New Proposal form shows its empty state
      until a framework is activated.
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading priority agendas...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn btn-secondary" @click="loadSets">Retry</button>
    </div>

    <!-- Framework list -->
    <template v-else>
      <p v-if="!sets.length" class="empty">
        No framework is configured yet. Create one to start offering priority agendas to
        proponents.
      </p>

      <div v-for="set in sets" :key="set.id" class="framework-card" :class="{ 'is-active': set.active }">
        <div class="framework-head">
          <div class="framework-title">
            <h3>{{ set.frameworkName }} {{ set.coverageStartYear }}–{{ set.coverageEndYear }}</h3>
            <span class="status-badge" :class="statusTone(set.active ? 'ACTIVE' : 'INACTIVE')">
              {{ priorityAgendaStatusLabel(set.active) }}
            </span>
          </div>
          <div class="framework-actions">
            <button v-if="!set.active" class="activate-btn btn-approve" @click="activateSet(set)">Activate</button>
            <button class="edit-btn btn-edit" @click="openEditFramework(set)">Edit</button>
            <button class="edit-btn btn-edit" @click="openCreateItem(set)">+ Add Agenda</button>
            <!-- Program-level action: deactivates the whole framework, never its agendas. -->
            <button
              v-if="set.active"
              class="deactivate-btn btn-danger"
              @click="deactivateSet(set)"
            >
              Deactivate Program
            </button>
          </div>
        </div>

        <table class="items-table">
          <thead>
            <tr>
              <th>Agenda</th>
              <th>Key</th>
              <th>Prompt</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in set.agendas" :key="item.id">
              <td class="item-name">
                {{ item.name }}
                <span v-if="item.description" class="item-description">{{ item.description }}</span>
              </td>
              <td class="item-key">{{ item.agendaKey }}</td>
              <td>{{ item.subLabel || '—' }}</td>
              <td>
                <span class="status-badge" :class="statusTone(item.active ? 'ACTIVE' : 'INACTIVE')">
                  {{ priorityAgendaStatusLabel(item.active) }}
                </span>
              </td>
              <td class="actions-cell">
                <button class="edit-btn btn-edit" @click="openEditItem(set, item)">Edit</button>
                <button class="edit-btn btn-edit" @click="toggleItem(set, item)">
                  {{ item.active ? 'Deactivate' : 'Activate' }}
                </button>
              </td>
            </tr>
            <tr v-if="!set.agendas || !set.agendas.length">
              <td colspan="5" class="empty">
                No agenda yet. Use "Add Agenda" to add the first one.
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- ================= Create / Edit framework ================= -->
    <div
      v-if="showFrameworkModal"
      class="modal-overlay"
      @click.self="closeModals"
      @keydown.esc="closeModals"
    >
      <div
        class="fw-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="framework-modal-title"
      >
        <header class="fw-header">
          <span class="fw-header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
              stroke-linecap="round" stroke-linejoin="round">
              <path d="m12 2 9 5-9 5-9-5 9-5z" />
              <path d="m3 12 9 5 9-5" />
              <path d="m3 17 9 5 9-5" />
            </svg>
          </span>
          <div class="fw-heading">
            <h3 id="framework-modal-title">
              {{ editingFrameworkId ? 'Edit framework' : 'Create framework' }}
            </h3>
            <p>Set the name and years that appear next to "Priority Agenda" on new proposals.</p>
          </div>
          <button
            type="button"
            class="fw-close"
            :disabled="saving"
            aria-label="Close create framework"
            @click="closeModals"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="fw-body">
          <div class="fw-field">
            <label for="frameworkName">Framework / program name</label>
            <input
              id="frameworkName"
              ref="frameworkNameInput"
              v-model="frameworkForm.frameworkName"
              type="text"
              class="fw-input"
              placeholder="e.g., MSUN RIIE"
              maxlength="150"
              autocomplete="off"
            />
          </div>

          <div class="fw-row">
            <div class="fw-field">
              <label for="coverageStartYear">Start year</label>
              <input
                id="coverageStartYear"
                v-model.number="frameworkForm.coverageStartYear"
                type="number"
                class="fw-input"
                min="1900"
                max="3000"
                placeholder="2026"
              />
            </div>
            <div class="fw-field">
              <label for="coverageEndYear">End year</label>
              <input
                id="coverageEndYear"
                v-model.number="frameworkForm.coverageEndYear"
                type="number"
                class="fw-input"
                min="1900"
                max="3000"
                placeholder="2029"
              />
            </div>
          </div>
          <p class="fw-hint">Enter a four-digit start and end year.</p>
          <p v-if="yearRangeError" class="fw-hint fw-hint-error" role="alert">
            {{ yearRangeError }}
          </p>

          <!-- What the Proponent sees on the New Proposal form, from the live form state. -->
          <section class="fw-preview" aria-label="Preview on the new proposal form">
            <span class="fw-preview-label">Shown on the new proposal form</span>
            <div class="fw-preview-row">
              <span class="fw-preview-agenda">Priority Agenda</span>
              <span class="fw-preview-badge">{{ frameworkPreview }}</span>
            </div>
          </section>

          <!-- The existing active-framework flag, presented as a switch. -->
          <section class="fw-toggle-card">
            <div class="fw-toggle-text">
              <span class="fw-toggle-title">Make this the active framework</span>
              <span class="fw-toggle-description">
                New submissions will use it, and the current active framework will be deactivated.
              </span>
            </div>
            <label class="fw-switch">
              <input
                v-model="frameworkForm.active"
                type="checkbox"
                aria-label="Make this the active framework"
              />
              <span class="fw-switch-track" aria-hidden="true">
                <span class="fw-switch-thumb"></span>
              </span>
            </label>
          </section>

          <p v-if="formError" class="fw-error" role="alert">{{ formError }}</p>
        </div>

        <footer class="fw-footer">
          <button type="button" class="fw-cancel" :disabled="saving" @click="closeModals">
            Cancel
          </button>
          <button
            type="button"
            class="fw-create"
            :disabled="saving || !canSaveFramework"
            @click="saveFramework"
          >
            <span>
              {{ saving ? 'Saving...' : (editingFrameworkId ? 'Save changes' : 'Create framework') }}
            </span>
            <svg v-if="!saving" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="4" y1="12" x2="19" y2="12" />
              <polyline points="13 6 19 12 13 18" />
            </svg>
          </button>
        </footer>
      </div>
    </div>

    <!-- ================= Create / Edit agenda item ================= -->
    <div v-if="showItemModal" class="modal-overlay" @click.self="closeModals">
      <div class="modal-box">
        <header class="modal-header">
          <div class="header-text">
            <h3 class="header-title">
              {{ editingItemId ? 'Edit Agenda' : 'Add Agenda' }}
            </h3>
            <p class="header-subtitle">
              {{ itemForm.setLabel }}
            </p>
          </div>
          <button type="button" class="header-close" @click="closeModals" :disabled="saving" aria-label="Close">
            ×
          </button>
        </header>

        <div class="modal-body">
          <!-- Reuse an existing agenda instead of retyping it. The options are the agenda
               records this page loaded from the API, so anything saved earlier is offered
               here. Shown only while creating: an existing agenda is edited in place, and
               its stable key can never change. -->
          <div v-if="!editingItemId" class="form-group">
            <label>Reuse an existing agenda</label>
            <AgendaPicker :agendas="agendaOptions" :target-set-id="itemForm.setId"
              @select="applyAgendaRecord" @create="useTypedAgendaName" />
            <p v-if="pickerHint" class="field-hint">{{ pickerHint }}</p>
          </div>
          <div class="form-group">
            <label>Agenda name</label>
            <input v-model="itemForm.name" type="text" class="form-input"
              placeholder="e.g., DAGAT Program" maxlength="150" />
          </div>
          <div class="form-group">
            <label>Prompt beside the value input</label>
            <input v-model="itemForm.subLabel" type="text" class="form-input"
              placeholder="e.g., Commodity, Priority Topic or Sector" maxlength="80" />
          </div>
          <div class="form-group">
            <label>Description (optional)</label>
            <textarea v-model="itemForm.description" class="form-input" rows="2"
              placeholder="The expansion of the program name, shown to administrators"></textarea>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Stable key</label>
              <input v-if="!editingItemId" v-model="itemForm.agendaKey" type="text" class="form-input"
                placeholder="Leave blank to derive from the name" maxlength="50" />
              <p v-else class="readonly-value">{{ itemForm.agendaKey }}</p>
              <p class="field-hint">
                Lowercase letters, digits and hyphens. It is stored with every proposal that
                selects this agenda, so it can never be changed after the agenda is created.
              </p>
            </div>
            <div class="form-group">
              <label>Display order</label>
              <input v-model.number="itemForm.displayOrder" type="number" class="form-input" min="0" />
            </div>
          </div>
          <label class="checkbox-row">
            <input type="checkbox" v-model="itemForm.active" />
            <span>Available for new submissions (deactivating keeps it on existing proposals)</span>
          </label>

          <p v-if="formError" class="error-text form-error">{{ formError }}</p>
        </div>


        <footer class="modal-footer">
          <div class="modal-actions">
            <button class="cancel-btn btn-secondary" @click="closeModals" :disabled="saving">Cancel</button>
            <button class="confirm-btn btn-primary" @click="saveItem" :disabled="saving">
              {{ saving ? 'Saving...' : (editingItemId ? 'Save Changes' : 'Add Agenda') }}
            </button>
          </div>
        </footer>
      </div>
    </div>

    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted } from 'vue'

import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import AgendaPicker from '@/components/AgendaPicker.vue'
import { useDialog } from '@/composables/useDialog'
import { agendaCatalogue, agendaPrefill, frameworkKeys } from '@/utils/agendaPicker'
import { statusTone } from '@/utils/statusTone.js'
import { priorityAgendaStatusLabel } from '@/utils/statusCatalog.js'

/**
 * Administrator management of the Priority Agenda master data (section 05 of the
 * Create Proposal form).
 *
 * The Proponent form reads its framework heading and its agenda checkboxes from
 * `GET /api/priority-agendas/active`, so everything configured here is reflected
 * without a source change or redeployment. Activating a framework deactivates the
 * previous one; nothing is ever deleted, so proposals submitted under an older
 * framework keep their historical selection.
 *
 * Every operation is re-authorized by the backend (SecurityConfig restricts the
 * management endpoints to the RPS roles), so this page is a convenience, never the
 * guard.
 */

const { dialogState, showAlert, showConfirm } = useDialog()

const sets = ref([])
const loading = ref(false)
const error = ref(null)

const showFrameworkModal = ref(false)
const editingFrameworkId = ref(null)
const frameworkForm = ref(emptyFrameworkForm())
/** The name input, focused when the framework modal opens. */
const frameworkNameInput = ref(null)

const showItemModal = ref(false)
const editingItemId = ref(null)
const itemForm = ref(emptyItemForm(null))

const saving = ref(false)
const formError = ref(null)

/** A new framework defaults to the current year and a three-year coverage period. */
function emptyFrameworkForm() {
  const year = new Date().getFullYear()
  return {
    frameworkName: '',
    coverageStartYear: year,
    coverageEndYear: year + 3,
    active: false,
  }
}

function emptyItemForm(set) {
  return {
    setId: set?.id ?? null,
    setLabel: set
      ? `Framework: ${set.frameworkName} ${set.coverageStartYear}\u2013${set.coverageEndYear}`
      : '',
    agendaKey: '',
    name: '',
    subLabel: '',
    description: '',
    displayOrder: set?.agendas?.length ?? 0,
    active: true,
  }
}

const activeSet = computed(() => sets.value.find((set) => set.active) || null)

/** A coverage year is one the form accepts: a four-digit year within its min/max range. */
function isValidYear(value) {
  const year = Number(value)
  return Number.isInteger(year) && year >= 1900 && year <= 3000
}

/** Shown under the year inputs when an entered range is impossible. */
const yearRangeError = computed(() => {
  const start = frameworkForm.value.coverageStartYear
  const end = frameworkForm.value.coverageEndYear
  if (start === '' || start === null || start === undefined) return ''
  if (end === '' || end === null || end === undefined) return ''
  if (!isValidYear(start) || !isValidYear(end)) return ''
  return Number(end) < Number(start)
    ? 'End year must not be earlier than the start year.'
    : ''
})

/** The live preview: the real form values, or a placeholder before they are typed. */
const frameworkPreview = computed(() => {
  const name = (frameworkForm.value.frameworkName || '').trim() || 'Your framework name'
  const start = frameworkForm.value.coverageStartYear
  const end = frameworkForm.value.coverageEndYear
  // An impossible range is never previewed as if it were valid; the red hint below the
  // year fields explains why the years are missing from the badge.
  const years = isValidYear(start) && isValidYear(end) && Number(end) >= Number(start)
    ? ` (${Number(start)}–${Number(end)})`
    : ''
  return name + years
})

/** The footer button is enabled only for a valid name and a valid year range. */
const canSaveFramework = computed(() => {
  const name = (frameworkForm.value.frameworkName || '').trim()
  const start = frameworkForm.value.coverageStartYear
  const end = frameworkForm.value.coverageEndYear
  return Boolean(name) && isValidYear(start) && isValidYear(end) && Number(end) >= Number(start)
})

/** How many agendas of a framework are actually offered to new proposals. */
function activeItemCount(set) {
  return (set?.agendas || []).filter((item) => item.active).length
}

function extractErrorMessage(err, fallback) {
  const data = err?.response?.data
  if (data && typeof data === 'object' && data.error) return data.error
  if (typeof data === 'string' && data.trim()) return data
  return fallback
}

// ===== LOAD =====

async function loadSets() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/priority-agendas/sets')
    sets.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = extractErrorMessage(err, 'Failed to load the priority agendas.')
    sets.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadSets)

// ===== MODAL STATE =====

function closeModals() {
  showFrameworkModal.value = false
  showItemModal.value = false
  formError.value = null
  pickerHint.value = ''
}

function openCreateFramework() {
  editingFrameworkId.value = null
  frameworkForm.value = emptyFrameworkForm()
  formError.value = null
  showFrameworkModal.value = true
  nextTick(() => frameworkNameInput.value?.focus())
}

function openEditFramework(set) {
  editingFrameworkId.value = set.id
  frameworkForm.value = {
    frameworkName: set.frameworkName,
    coverageStartYear: set.coverageStartYear,
    coverageEndYear: set.coverageEndYear,
    active: Boolean(set.active),
  }
  formError.value = null
  showFrameworkModal.value = true
  nextTick(() => frameworkNameInput.value?.focus())
}

function openCreateItem(set) {
  editingItemId.value = null
  itemForm.value = emptyItemForm(set)
  formError.value = null
  showItemModal.value = true
}

function openEditItem(set, item) {
  editingItemId.value = item.id
  itemForm.value = {
    setId: set.id,
    setLabel: `Framework: ${set.frameworkName} ${set.coverageStartYear}\u2013${set.coverageEndYear}`,
    agendaKey: item.agendaKey,
    name: item.name,
    subLabel: item.subLabel || '',
    description: item.description || '',
    displayOrder: item.displayOrder ?? 0,
    active: Boolean(item.active),
  }
  formError.value = null
  showItemModal.value = true
}
// ===== SAVE =====

async function saveFramework() {
  const name = (frameworkForm.value.frameworkName || '').trim()
  const start = frameworkForm.value.coverageStartYear
  const end = frameworkForm.value.coverageEndYear
  if (!name) {
    formError.value = 'Framework / program name is required.'
    return
  }
  if (!isValidYear(start)) {
    formError.value = 'Enter a four-digit start year.'
    return
  }
  if (!isValidYear(end)) {
    formError.value = 'Enter a four-digit end year.'
    return
  }
  if (Number(end) < Number(start)) {
    formError.value = 'End year must not be earlier than the start year.'
    return
  }

  saving.value = true
  formError.value = null
  try {
    const payload = {
      frameworkName: name,
      coverageStartYear: Number(start),
      coverageEndYear: Number(end),
      active: Boolean(frameworkForm.value.active),
    }
    if (editingFrameworkId.value) {
      await api.put(`/api/priority-agendas/sets/${editingFrameworkId.value}`, payload)
    } else {
      await api.post('/api/priority-agendas/sets', payload)
    }
    closeModals()
    await loadSets()
    await showAlert('Priority agenda framework saved.', {
      type: 'success',
      title: editingFrameworkId.value ? 'Framework Updated' : 'Framework Created',
    })
  } catch (err) {
    console.error(err)
    formError.value = extractErrorMessage(err, 'Failed to save the framework. Please try again.')
  } finally {
    saving.value = false
  }
}

async function saveItem() {
  const name = (itemForm.value.name || '').trim()
  if (!name) {
    formError.value = 'Agenda name is required.'
    return
  }

  saving.value = true
  formError.value = null
  try {
    const payload = {
      name,
      subLabel: (itemForm.value.subLabel || '').trim(),
      description: (itemForm.value.description || '').trim(),
      displayOrder: Number(itemForm.value.displayOrder ?? 0),
      active: Boolean(itemForm.value.active),
    }
    if (editingItemId.value) {
      await api.put(`/api/priority-agendas/items/${editingItemId.value}`, payload)
    } else {
      await api.post(`/api/priority-agendas/sets/${itemForm.value.setId}/items`, {
        ...payload,
        // A blank key lets the backend derive a unique one from the name.
        agendaKey: (itemForm.value.agendaKey || '').trim(),
      })
    }
    closeModals()
    await loadSets()
    await showAlert('Priority agenda saved.', {
      type: 'success',
      title: editingItemId.value ? 'Agenda Updated' : 'Agenda Added',
    })
  } catch (err) {
    console.error(err)
    formError.value = extractErrorMessage(err, 'Failed to save the agenda. Please try again.')
  } finally {
    saving.value = false
  }
}

// ===== ACTIVATE / DEACTIVATE =====

async function activateSet(set) {
  const confirmed = await showConfirm(
    `Activate ${set.frameworkName} ${set.coverageStartYear}\u2013${set.coverageEndYear}? ` +
      'The currently active framework will be deactivated. Proposals already submitted keep ' +
      'their historical agenda selection.',
    { title: 'Activate Framework', type: 'warning', confirmText: 'Activate' }
  )
  if (!confirmed) return

  try {
    await api.put(`/api/priority-agendas/sets/${set.id}/activate`)
    await loadSets()
    await showAlert('Framework activated. New proposals now use this framework.', {
      type: 'success',
      title: 'Framework Activated',
    })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to activate the framework.'), {
      type: 'error',
      title: 'Activation Failed',
    })
  }
}

/**
 * Deactivates the whole framework / program (not one agenda). The confirmation names the
 * program; the backend keeps the set, its years and every agenda, and only clears the
 * program-level active flag.
 */
async function deactivateSet(set) {
  const label = `${set.frameworkName} ${set.coverageStartYear}–${set.coverageEndYear}`
  const confirmed = await showConfirm(
    'Are you sure you want to deactivate this framework/program? ' +
      `You are about to deactivate ${label}. This will deactivate the program itself and ` +
      'affect its availability for new proposals. Individual agenda statuses will not be ' +
      'changed unless required by the existing system logic.',
    {
      title: 'Deactivate Framework / Program',
      type: 'danger',
      confirmText: 'Deactivate Program',
    }
  )
  if (!confirmed) return

  try {
    await api.put(`/api/priority-agendas/sets/${set.id}/deactivate`)
    await loadSets()
    await showAlert(
      `${label} is now inactive. New proposals will show their empty state until a framework is activated.`,
      { type: 'success', title: 'Framework Deactivated' }
    )
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to deactivate the framework.'), {
      type: 'error',
      title: 'Deactivation Failed',
    })
  }
}

/** Activates / deactivates one agenda item without touching any proposal. */
async function toggleItem(set, item) {
  const nextActive = !item.active
  const confirmed = await showConfirm(
    `${nextActive ? 'Activate' : 'Deactivate'} "${item.name}"? ` +
      (nextActive
        ? 'New proposals will be able to select it.'
        : 'It will no longer be offered for new proposals, but existing proposals keep their selection.'),
    { title: nextActive ? 'Activate Agenda' : 'Deactivate Agenda', type: 'warning' }
  )
  if (!confirmed) return

  try {
    await api.put(`/api/priority-agendas/items/${item.id}`, {
      name: item.name,
      subLabel: item.subLabel || '',
      description: item.description || '',
      displayOrder: item.displayOrder ?? 0,
      active: nextActive,
    })
    await loadSets()
    await showAlert(`Agenda ${nextActive ? 'activated' : 'deactivated'}.`, {
      type: 'success',
      title: nextActive ? 'Agenda Activated' : 'Agenda Deactivated',
    })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to update the agenda.'), {
      type: 'error',
      title: 'Update Failed',
    })
  }
}

// ===== REUSE AN EXISTING AGENDA =====

/** The dropdown options: every agenda already saved, from every framework. */
const agendaOptions = computed(() => agendaCatalogue(sets.value))

/** Explains what the last selection filled in, or why the key was left blank. */
const pickerHint = ref('')

/** Copies a selected existing agenda into the form. Nothing is saved here. */
function applyAgendaRecord(record) {
  const prefill = agendaPrefill(record, {
    existingKeys: frameworkKeys(sets.value, itemForm.value.setId),
  })
  itemForm.value.name = prefill.name
  itemForm.value.subLabel = prefill.subLabel
  itemForm.value.description = prefill.description
  itemForm.value.agendaKey = prefill.agendaKey
  pickerHint.value = prefill.keyTaken
    ? '"' + prefill.name + '" is already an agenda of this framework, so its key is taken. A new key will be derived when you save.'
    : 'Reused "' + prefill.name + '". Adjust anything before saving.'
}

/** "Create new agenda" from the dropdown: the typed name becomes the new agenda. */
function useTypedAgendaName(name) {
  itemForm.value.name = name
  pickerHint.value = ''
}
</script>

<style scoped>
.page {
  padding: 24px;
  background: #ffffff;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.header h2 {
  margin: 0 0 6px;
  font-size: var(--ui-font-page-title);
  color: #1f2937;
  font-weight: var(--ui-weight-bold);
}

.subtitle {
  margin: 0;
  max-width: 720px;
  color: #6b7280;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-body);
}

/* Layout only; the action colours come from assets/actions.css. */
.create-btn {
  padding: 9px 16px;
  border-radius: 6px;
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
  white-space: nowrap;
}

.banner {
  margin: 16px 0;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: var(--ui-font-small);
}

.banner-open {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.banner-closed {
  background: #fef3c7;
  border: 1px solid #fde68a;
  color: #92400e;
}

.loading-state {
  padding: 24px 0;
  color: #6b7280;
}

.error-text {
  color: #dc2626;
}

.retry-btn {
  margin-top: 8px;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.empty {
  color: #6b7280;
  padding: 12px 0;
  font-size: var(--ui-font-caption);
}

.framework-card {
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}

.framework-card.is-active {
  border-color: #86efac;
  box-shadow: 0 0 0 1px #bbf7d0 inset;
}

.framework-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.framework-title {
  display: flex;
  align-items: center;
  gap: 10px;
}

.framework-title h3 {
  margin: 0;
  font-size: var(--ui-font-body-lg);
  color: #1f2937;
}

.framework-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.status-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: var(--ui-font-small);
}

.items-table th {
  text-align: left;
  padding: 8px 10px;
  background: #f8fafc;
  color: #475569;
  font-size: var(--ui-font-table-header);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
  border-bottom: 1px solid #e5e7eb;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

.item-name {
  font-weight: var(--ui-weight-semibold);
  color: #1f2937;
}

.item-description {
  display: block;
  margin-top: 3px;
  font-weight: var(--ui-weight-regular);
  font-size: var(--ui-font-caption);
  color: #6b7280;
  max-width: 420px;
}

.item-key {
  font-family: var(--ui-font-mono);
  color: #4b3f72;
}

.actions-cell {
  white-space: nowrap;
}

/* Layout only; .btn-approve / .btn-edit / .btn-secondary supply the colours. */
.activate-btn {
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
  font-size: var(--ui-control-font-sm);
}

.edit-btn {
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
  font-size: var(--ui-control-font-sm);
}

/* Layout only; .btn-danger supplies the destructive colour. */
.deactivate-btn {
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 0;
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
}

/* ===== Modals ===== */

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 1000;
}

.modal-box {
  width: 100%;
  max-width: 560px;
  background: #ffffff;
  border-radius: 12px;
  overflow: hidden;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  background: #4b3f72;
  color: #ffffff;
  padding: 16px 18px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.header-title {
  margin: 0 0 4px;
  font-size: var(--ui-font-body-lg);
}

.header-subtitle {
  margin: 0;
  font-size: var(--ui-font-caption);
  color: #d9d5ea;
  line-height: var(--ui-line-snug);
}

.header-close {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: var(--ui-icon-lg);
  line-height: var(--ui-line-none);
  cursor: pointer;
}

.modal-body {
  padding: 18px;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 14px;
  flex: 1;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  font-size: var(--ui-font-label);
  font-weight: var(--ui-weight-semibold);
  color: #374151;
}

.form-row {
  display: flex;
  gap: 14px;
}

.form-input {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-family: inherit;
  font-size: var(--ui-font-small);
  box-sizing: border-box;
}

.form-error {
  margin: 4px 0 0;
  font-size: var(--ui-font-caption);
}

.field-hint {
  margin: 5px 0 0;
  font-size: var(--ui-font-caption);
  color: #6b7280;
  line-height: var(--ui-line-snug);
}

.readonly-value {
  margin: 0;
  font-family: var(--ui-font-mono);
  color: #4b3f72;
  font-size: var(--ui-font-small);
}

.checkbox-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: var(--ui-font-small);
  color: #374151;
  cursor: pointer;
}

.modal-footer {
  padding: 14px 18px;
  border-top: 1px solid #e5e7eb;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.cancel-btn {
  padding: 9px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: var(--ui-weight-semibold);
}

.confirm-btn {
  padding: 9px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: var(--ui-weight-semibold);
}

/* ===== Create / Edit framework dialog =====================================
   A compact, centred form dialog: icon header, full-width name, a two-column
   year row, the live "New Proposal" preview, the active switch and a
   two-button footer. The Add Agenda modal above keeps its own layout. */
.fw-modal {
  display: flex;
  flex-direction: column;
  width: 352px;
  max-width: calc(100vw - 24px);
  max-height: 90vh;
  overflow: hidden;
  border: 1px solid #e5e8f1;
  border-radius: 18px;
  background: #ffffff;
  box-shadow:
    0 24px 48px -16px rgba(20, 24, 50, 0.32),
    0 8px 18px -10px rgba(20, 24, 50, 0.18);
}

.fw-header {
  display: flex;
  align-items: flex-start;
  gap: 11px;
  flex: 0 0 auto;
  padding: 16px 16px 14px;
  border-bottom: 1px solid #eceef5;
  background: #ffffff;
}

.fw-header-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 10px;
  background: #262342;
  color: #ffd000;
}

.fw-header-icon svg {
  width: 18px;
  height: 18px;
}

.fw-heading {
  flex: 1;
  min-width: 0;
}

.fw-heading h3 {
  margin: 0;
  font-size: var(--ui-font-body);
  font-weight: var(--ui-weight-bold);
  letter-spacing: var(--ui-tracking-tight);
  line-height: var(--ui-line-tight);
  color: #1c1a3c;
}

.fw-heading p {
  margin: 3px 0 0;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: var(--ui-text-muted);
}

.fw-close {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  flex: 0 0 auto;
  margin-left: 2px;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: transparent;
  color: #8a8fa3;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.fw-close svg {
  width: 16px;
  height: 16px;
}

.fw-close:hover:not(:disabled) {
  background: #f2f3f9;
  color: #262342;
}

.fw-close:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.fw-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 16px;
}

.fw-body::-webkit-scrollbar {
  width: 7px;
}

.fw-body::-webkit-scrollbar-thumb {
  background: #d7dbe8;
  border-radius: 999px;
}

.fw-body::-webkit-scrollbar-track {
  background: transparent;
}

.fw-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.fw-field label {
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: #3b3d55;
}

.fw-input {
  width: 100%;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #dfe3ee;
  border-radius: 8px;
  background: #ffffff;
  color: #1c1a3c;
  font-family: inherit;
  font-size: var(--ui-font-caption);
  box-sizing: border-box;
}

.fw-input::placeholder {
  color: #a0a5b6;
}

.fw-input:focus {
  outline: none;
  border-color: #315bff;
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.12);
}

.fw-row {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
}

.fw-hint {
  margin: -6px 0 0;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: #81869a;
}

.fw-hint-error {
  margin-top: -8px;
  color: #b42318;
  font-weight: var(--ui-weight-semibold);
}

/* Preview card */
.fw-preview {
  display: flex;
  flex-direction: column;
  gap: 7px;
  padding: 11px 12px;
  border: 1px solid #e2e7f3;
  border-radius: 13px;
  background: #f4f6fc;
}

.fw-preview-label {
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  letter-spacing: var(--ui-tracking-caps);
  text-transform: uppercase;
  color: #7b8298;
}

.fw-preview-row {
  display: flex;
  align-items: center;
  gap: 7px;
  flex-wrap: wrap;
  min-height: 30px;
  padding: 6px 9px;
  border: 1px dashed #d9dfec;
  border-radius: 8px;
  background: #ffffff;
}

.fw-preview-agenda {
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: #3b3d55;
  white-space: nowrap;
}

.fw-preview-badge {
  max-width: 100%;
  padding: 2px 8px;
  border-radius: 999px;
  background: #e6edfc;
  color: #31539b;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-body);
  overflow-wrap: anywhere;
}

/* Active framework switch card */
.fw-toggle-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 11px 12px;
  border: 1px solid #e6e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.fw-toggle-text {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.fw-toggle-title {
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-snug);
  color: #1c1a3c;
}

.fw-toggle-description {
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: #81869a;
}

.fw-switch {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  cursor: pointer;
}

.fw-switch input {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  border: 0;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  overflow: hidden;
  white-space: nowrap;
}

.fw-switch-track {
  position: relative;
  display: inline-block;
  width: 32px;
  height: 18px;
  border-radius: 999px;
  background: #c8ceda;
  transition: background 0.15s;
}

.fw-switch-thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: 0 1px 2px rgba(20, 24, 50, 0.22);
  transition: transform 0.15s;
}

.fw-switch input:checked + .fw-switch-track {
  background: #315bff;
}

.fw-switch input:checked + .fw-switch-track .fw-switch-thumb {
  transform: translateX(14px);
}

.fw-switch input:focus-visible + .fw-switch-track {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.fw-error {
  margin: 0;
  padding: 8px 10px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
  color: #991b1b;
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
}

.fw-footer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 8px;
  flex: 0 0 auto;
  padding: 12px 16px 16px;
  border-top: 1px solid #eceef5;
  background: #ffffff;
}

.fw-cancel,
.fw-create {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  border-radius: 8px;
  font-family: inherit;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.fw-cancel {
  border: 1px solid #dfe3ee;
  background: #ffffff;
  color: #262342;
}

.fw-cancel:hover:not(:disabled) {
  background: #f6f7fb;
  border-color: #cdd3e3;
}

.fw-create {
  border: 1px solid transparent;
  background: #2452ff;
  color: #ffffff;
}

.fw-create:hover:not(:disabled) {
  background: #1e46e0;
}

.fw-create:disabled {
  border-color: #d8e0f0;
  background: #e7ecf6;
  color: #8a8fa3;
  cursor: not-allowed;
}

.fw-cancel:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.fw-create svg {
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
}

.fw-cancel:focus-visible,
.fw-create:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

@media (max-width: 380px) {
  .fw-modal {
    width: calc(100vw - 20px);
    border-radius: 16px;
  }

  .fw-header {
    padding: 14px 14px 12px;
  }

  .fw-body {
    padding: 14px;
  }

  .fw-footer {
    padding: 10px 14px 14px;
  }
}
</style>


