<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>Application Cycles</h2>
        <p class="subtitle">Manage the submission windows during which proponents can submit new proposals.</p>
      </div>
      <button class="create-btn" @click="openCreateModal">+ Create Cycle</button>
    </div>

    <!-- Current submission window banner -->
    <div v-if="activeCycle" class="banner banner-open">
      <strong>Submissions are OPEN:</strong>&nbsp;{{ activeCycle.name }}
      ({{ formatDate(activeCycle.startDate) }} – {{ formatDate(activeCycle.endDate) }})
    </div>
    <div v-else-if="!loading && !error" class="banner banner-closed">
      <strong>Submissions are CLOSED.</strong>&nbsp;There is no active application cycle at this time.
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading application cycles...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="loadCycles">Retry</button>
    </div>

    <!-- Table -->
    <table v-else>
      <thead>
        <tr>
          <th>Cycle Name</th>
          <th>Start Date</th>
          <th>End Date</th>
          <th>Status</th>
          <th>Window</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="cycle in cycles" :key="cycle.id">
          <td class="cycle-name">{{ cycle.name }}</td>
          <td>{{ formatDate(cycle.startDate) }}</td>
          <td>{{ formatDate(cycle.endDate) }}</td>
          <td>
            <span class="status-badge" :class="statusClass(cycle.status)">{{ cycle.status }}</span>
          </td>
          <td>
            <span class="window-hint" :class="windowClass(cycle)">{{ windowLabel(cycle) }}</span>
          </td>
          <td class="actions-cell">
            <button v-if="cycle.status !== 'ACTIVE'" class="activate-btn" @click="activateCycleAction(cycle)">
              Activate
            </button>
            <button class="edit-btn" @click="openEditModal(cycle)">Edit</button>
            <button v-if="cycle.status !== 'ACTIVE'" class="delete-btn" @click="deleteCycleAction(cycle)">
              Delete
            </button>
          </td>
        </tr>
        <tr v-if="cycles.length === 0">
          <td colspan="6" class="empty">No application cycles yet. Create one to open proposal submissions.</td>
        </tr>
      </tbody>
    </table>

    <!-- Create / Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-box">
        <h3>{{ isEditing ? 'Edit Application Cycle' : 'Create Application Cycle' }}</h3>

        <div class="form-group">
          <label>Cycle Name</label>
          <input v-model="form.name" type="text" class="form-input"
            placeholder="e.g., FY 2026 - 1st Call for Proposals" />
        </div>

        <div class="form-row">
          <div class="form-group">
            <label>Start Date</label>
            <input v-model="form.startDate" type="date" class="form-input" />
          </div>
          <div class="form-group">
            <label>End Date</label>
            <input v-model="form.endDate" type="date" class="form-input" :min="form.startDate || undefined" />
          </div>
        </div>

        <div class="form-group">
          <label>Status</label>
          <select v-model="form.status" class="form-input">
            <option value="DRAFT">Draft</option>
            <option value="ACTIVE">Active</option>
            <option value="CLOSED">Closed</option>
          </select>
          <p v-if="form.status === 'ACTIVE'" class="hint">
            Saving with Active status notifies all proponents that submissions are open.
          </p>
        </div>

        <p v-if="formError" class="error-text">{{ formError }}</p>

        <div class="modal-actions">
          <button class="cancel-btn" @click="closeModal" :disabled="saving">Cancel</button>
          <button class="confirm-btn" @click="saveCycle" :disabled="saving">
            {{ saving ? 'Saving...' : (isEditing ? 'Save Changes' : 'Create Cycle') }}
          </button>
        </div>
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

const { dialogState, showAlert, showConfirm } = useDialog()

const cycles = ref([])
const loading = ref(false)
const error = ref(null)

const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return { name: '', startDate: '', endDate: '', status: 'DRAFT' }
}

// ===== HELPERS =====

/** Local today as YYYY-MM-DD (matches backend LocalDate serialization). */
function localTodayISO() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function formatDate(iso) {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function isActiveNow(cycle) {
  const today = localTodayISO()
  return cycle.status === 'ACTIVE' && cycle.startDate <= today && today <= cycle.endDate
}

const activeCycle = computed(() => cycles.value.find(isActiveNow) || null)

function statusClass(status) {
  return {
    DRAFT: 'status-draft',
    ACTIVE: 'status-active',
    CLOSED: 'status-closed',
  }[status] || ''
}

function windowLabel(cycle) {
  const today = localTodayISO()
  if (cycle.endDate < today) return 'Expired'
  if (isActiveNow(cycle)) return 'Accepting submissions'
  if (cycle.status === 'ACTIVE' && cycle.startDate > today) return `Opens ${formatDate(cycle.startDate)}`
  return '—'
}

function windowClass(cycle) {
  const today = localTodayISO()
  if (isActiveNow(cycle)) return 'window-open'
  if (cycle.endDate < today) return 'window-expired'
  if (cycle.status === 'ACTIVE' && cycle.startDate > today) return 'window-upcoming'
  return ''
}

function extractErrorMessage(err, fallback) {
  const data = err.response?.data
  if (data && typeof data === 'object' && data.error) return data.error
  if (typeof data === 'string' && data.trim()) return data
  return fallback
}

// ===== LOAD =====

async function loadCycles() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/application-cycles')
    cycles.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = extractErrorMessage(err, 'Failed to load application cycles.')
    cycles.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCycles)

// ===== CREATE / EDIT =====

function openCreateModal() {
  isEditing.value = false
  editingId.value = null
  form.value = emptyForm()
  formError.value = null
  showModal.value = true
}

function openEditModal(cycle) {
  isEditing.value = true
  editingId.value = cycle.id
  form.value = {
    name: cycle.name,
    startDate: cycle.startDate,
    endDate: cycle.endDate,
    status: cycle.status,
  }
  formError.value = null
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  formError.value = null
}

async function saveCycle() {
  // Client-side validation (backend re-validates everything)
  if (!form.value.name.trim()) {
    formError.value = 'Cycle name is required.'
    return
  }
  if (!form.value.startDate || !form.value.endDate) {
    formError.value = 'Start date and end date are required.'
    return
  }
  if (form.value.endDate < form.value.startDate) {
    formError.value = 'End date cannot be earlier than the start date.'
    return
  }

  saving.value = true
  formError.value = null
  try {
    const payload = {
      name: form.value.name.trim(),
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      status: form.value.status,
    }

    if (isEditing.value) {
      await api.put(`/api/application-cycles/${editingId.value}`, payload)
    } else {
      await api.post('/api/application-cycles', payload)
    }

    const becameActive = payload.status === 'ACTIVE'
    closeModal()
    await loadCycles()
    await showAlert(
      becameActive
        ? 'Application cycle saved and activated. All proponents have been notified.'
        : 'Application cycle saved successfully.',
      { type: 'success', title: isEditing.value ? 'Cycle Updated' : 'Cycle Created' }
    )
  } catch (err) {
    console.error(err)
    formError.value = extractErrorMessage(err, 'Failed to save the application cycle. Please try again.')
  } finally {
    saving.value = false
  }
}

// ===== ACTIVATE =====

async function activateCycleAction(cycle) {
  const confirmed = await showConfirm(
    `Activate "${cycle.name}" (${formatDate(cycle.startDate)} – ${formatDate(cycle.endDate)})? All proponents will be notified that submissions are open.`,
    { title: 'Activate Cycle', type: 'warning', confirmText: 'Activate' }
  )
  if (!confirmed) return

  try {
    await api.put(`/api/application-cycles/${cycle.id}/activate`)
    await loadCycles()
    await showAlert('Application cycle activated. All proponents have been notified.', {
      type: 'success',
      title: 'Cycle Activated',
    })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to activate the cycle.'), {
      type: 'error',
      title: 'Activation Failed',
    })
  }
}

// ===== DELETE =====

async function deleteCycleAction(cycle) {
  const confirmed = await showConfirm(
    `Are you sure you want to delete "${cycle.name}"? This action cannot be undone.`,
    { title: 'Delete Cycle', type: 'danger', confirmText: 'Delete' }
  )
  if (!confirmed) return

  try {
    await api.delete(`/api/application-cycles/${cycle.id}`)
    await loadCycles()
    await showAlert('Application cycle deleted.', { type: 'success', title: 'Cycle Deleted' })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to delete the cycle.'), {
      type: 'error',
      title: 'Delete Failed',
    })
  }
}
</script>

<style scoped>
.page {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h2 {
  font-size: 24px;
  color: #1f2937;
  margin: 0;
}

.subtitle {
  color: #6b7280;
  margin: 4px 0 0 0;
  font-size: 14px;
}

.create-btn {
  padding: 10px 18px;
  background: #2452ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.create-btn:hover {
  background: #1b44d6;
}

.banner {
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 14px;
}

.banner-open {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #bbf7d0;
}

.banner-closed {
  background: #fee2e2;
  color: #991b1b;
  border: 1px solid #fecaca;
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #64748b;
}

.error-text {
  color: #dc2626;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

th,
td {
  border: 1px solid #e5e7eb;
  padding: 12px 16px;
  text-align: left;
  font-size: 14px;
}

th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  text-transform: uppercase;
  font-size: 12px;
  letter-spacing: 0.5px;
}

td {
  color: #1f2937;
}

.empty {
  text-align: center;
  color: #94a3b8;
  padding: 30px;
}

.cycle-name {
  font-weight: 600;
}

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.status-draft {
  background: #e5e7eb;
  color: #374151;
}

.status-active {
  background: #dcfce7;
  color: #166534;
}

.status-closed {
  background: #fee2e2;
  color: #991b1b;
}

.window-hint {
  font-size: 12px;
  font-weight: 600;
  color: #94a3b8;
}

.window-open {
  color: #16a34a;
}

.window-upcoming {
  color: #2563eb;
}

.window-expired {
  color: #9ca3af;
}

.actions-cell {
  white-space: nowrap;
}

.activate-btn {
  padding: 6px 14px;
  background: #16a34a;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  margin-right: 6px;
}

.activate-btn:hover {
  background: #15803d;
}

.edit-btn {
  padding: 6px 14px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  margin-right: 6px;
}

.edit-btn:hover {
  background: #1d4ed8;
}

.delete-btn {
  padding: 6px 14px;
  background: #ef4444;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.delete-btn:hover {
  background: #dc2626;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  width: 480px;
  max-width: 92vw;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.modal-box h3 {
  margin: 0 0 16px 0;
  color: #1f2937;
}

.form-group {
  margin-bottom: 14px;
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.form-input {
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  width: 100%;
}

.form-input:focus {
  border-color: #2452ff;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.hint {
  font-size: 12px;
  color: #64748b;
  margin: 6px 0 0 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.cancel-btn {
  padding: 10px 18px;
  background: #f1f5f9;
  color: #334155;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.cancel-btn:hover {
  background: #e2e8f0;
}

.confirm-btn {
  padding: 10px 18px;
  background: #2452ff;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>
