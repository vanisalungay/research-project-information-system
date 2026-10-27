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
      <button class="retry-btn" @click="loadSets">Retry</button>
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
            <span class="status-badge" :class="set.active ? 'status-active' : 'status-inactive'">
              {{ set.active ? 'Active' : 'Inactive' }}
            </span>
          </div>
          <div class="framework-actions">
            <button v-if="!set.active" class="activate-btn" @click="activateSet(set)">Activate</button>
            <button class="edit-btn" @click="openEditFramework(set)">Edit</button>
            <button class="edit-btn" @click="openCreateItem(set)">+ Add Agenda</button>
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
                <span class="status-badge" :class="item.active ? 'status-active' : 'status-inactive'">
                  {{ item.active ? 'Active' : 'Inactive' }}
                </span>
              </td>
              <td class="actions-cell">
                <button class="edit-btn" @click="openEditItem(set, item)">Edit</button>
                <button class="edit-btn" @click="toggleItem(set, item)">
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
    <div v-if="showFrameworkModal" class="modal-overlay" @click.self="closeModals">
      <div class="modal-box">
        <header class="modal-header">
          <div class="header-text">
            <h3 class="header-title">
              {{ editingFrameworkId ? 'Edit Framework' : 'Create Framework' }}
            </h3>
            <p class="header-subtitle">
              The framework name and coverage period are what the New Proposal form displays
              beside "Priority Agenda".
            </p>
          </div>
          <button type="button" class="header-close" @click="closeModals" :disabled="saving" aria-label="Close">
            ×
          </button>
        </header>

        <div class="modal-body">
          <div class="form-group">
            <label>Framework / program name</label>
            <input v-model="frameworkForm.frameworkName" type="text" class="form-input"
              placeholder="e.g., MSUN RIIDE" maxlength="150" />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Coverage start year</label>
              <input v-model.number="frameworkForm.coverageStartYear" type="number" class="form-input"
                min="1900" max="3000" placeholder="2025" />
            </div>
            <div class="form-group">
              <label>Coverage end year</label>
              <input v-model.number="frameworkForm.coverageEndYear" type="number" class="form-input"
                min="1900" max="3000" placeholder="2028" />
            </div>
          </div>
          <label class="checkbox-row">
            <input type="checkbox" v-model="frameworkForm.active" />
            <span>Activate this framework for new submissions (deactivates the current one)</span>
          </label>

          <p v-if="formError" class="error-text form-error">{{ formError }}</p>
        </div>

        <footer class="modal-footer">
          <div class="modal-actions">
            <button class="cancel-btn" @click="closeModals" :disabled="saving">Cancel</button>
            <button class="confirm-btn" @click="saveFramework" :disabled="saving">
              {{ saving ? 'Saving...' : (editingFrameworkId ? 'Save Changes' : 'Create Framework') }}
            </button>
          </div>
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
            <button class="cancel-btn" @click="closeModals" :disabled="saving">Cancel</button>
            <button class="confirm-btn" @click="saveItem" :disabled="saving">
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
import { ref, computed, onMounted } from 'vue'

import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

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
}

function openCreateFramework() {
  editingFrameworkId.value = null
  frameworkForm.value = emptyFrameworkForm()
  formError.value = null
  showFrameworkModal.value = true
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
    formError.value = 'Framework name is required.'
    return
  }
  if (start === null || start === undefined || start === '' || end === null || end === undefined || end === '') {
    formError.value = 'Coverage start year and end year are required.'
    return
  }
  if (Number(end) < Number(start)) {
    formError.value = 'Coverage end year must not be earlier than the start year.'
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
  font-size: 22px;
  color: #1f2937;
}

.subtitle {
  margin: 0;
  max-width: 720px;
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.create-btn {
  background: #facc15;
  border: none;
  padding: 9px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.banner {
  margin: 16px 0;
  padding: 10px 14px;
  border-radius: 8px;
  font-size: 13px;
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
  background: #4b3f72;
  color: #ffffff;
  border: none;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.empty {
  color: #6b7280;
  padding: 12px 0;
  font-size: 13px;
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
  font-size: 16px;
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
  font-size: 11px;
  font-weight: 700;
}

.status-active {
  background: #dcfce7;
  color: #166534;
}

.status-inactive {
  background: #e5e7eb;
  color: #374151;
}

.items-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.items-table th {
  text-align: left;
  padding: 8px 10px;
  background: #f8fafc;
  color: #475569;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
  border-bottom: 1px solid #e5e7eb;
}

.items-table td {
  padding: 10px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

.item-name {
  font-weight: 600;
  color: #1f2937;
}

.item-description {
  display: block;
  margin-top: 3px;
  font-weight: 400;
  font-size: 11.5px;
  color: #6b7280;
  max-width: 420px;
}

.item-key {
  font-family: monospace;
  color: #4b3f72;
}

.actions-cell {
  white-space: nowrap;
}

.activate-btn {
  background: #16a34a;
  color: #ffffff;
  border: none;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
  font-size: 12px;
}

.edit-btn {
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #3730a3;
  padding: 5px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
  font-size: 12px;
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
  font-size: 16px;
}

.header-subtitle {
  margin: 0;
  font-size: 12px;
  color: #d9d5ea;
  line-height: 1.45;
}

.header-close {
  background: transparent;
  border: none;
  color: #ffffff;
  font-size: 20px;
  line-height: 1;
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
  font-size: 12px;
  font-weight: 600;
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
  font-size: 13px;
  box-sizing: border-box;
}

.form-error {
  margin: 4px 0 0;
  font-size: 12.5px;
}

.field-hint {
  margin: 5px 0 0;
  font-size: 11px;
  color: #6b7280;
  line-height: 1.45;
}

.readonly-value {
  margin: 0;
  font-family: monospace;
  color: #4b3f72;
  font-size: 13px;
}

.checkbox-row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-size: 12.5px;
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
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  padding: 9px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.confirm-btn {
  background: #4b3f72;
  color: #ffffff;
  border: none;
  padding: 9px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}
</style>


