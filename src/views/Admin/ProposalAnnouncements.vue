<template>
  <div class="page">
    <div class="header">
      <div>
        <h2>Proposal Announcements</h2>
        <p class="subtitle">Manage the submission windows during which proponents can submit new proposals.</p>
      </div>
      <button class="create-btn" @click="openCreateModal">+ Create Announcement</button>
    </div>

    <!-- Current submission window banner -->
    <div v-if="activeAnnouncement" class="banner banner-open">
      <strong>Submissions are OPEN:</strong>&nbsp;{{ activeAnnouncement.name }}
      ({{ formatDate(activeAnnouncement.startDate) }} – {{ formatDate(activeAnnouncement.endDate) }})
    </div>
    <div v-else-if="!loading && !error" class="banner banner-closed">
      <strong>Submissions are CLOSED.</strong>&nbsp;There is no active proposal announcement at this time.
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading proposal announcements...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="loadAnnouncements">Retry</button>
    </div>

    <!-- Table -->
    <table v-else>
      <thead>
        <tr>
          <th>Announcement Name</th>
          <th>Start Date</th>
          <th>End Date</th>
          <th>Status</th>
          <th>Window</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="announcement in announcements" :key="announcement.id">
          <td class="announcement-name">{{ announcement.name }}</td>
          <td>{{ formatDate(announcement.startDate) }}</td>
          <td>{{ formatDate(announcement.endDate) }}</td>
          <td>
            <span class="status-badge" :class="statusClass(announcement)">{{ announcementStatus(announcement) }}</span>
          </td>
          <td>
            <span class="window-hint" :class="windowClass(announcement)">{{ windowLabel(announcement) }}</span>
          </td>
          <td class="actions-cell">
            <button v-if="announcement.status !== 'ACTIVE'" class="activate-btn" @click="activateAnnouncementAction(announcement)">
              Activate
            </button>
            <button class="edit-btn" @click="openEditModal(announcement)">Edit</button>
            <button v-if="canDelete(announcement)" class="delete-btn" @click="deleteAnnouncementAction(announcement)">
              Delete
            </button>
          </td>
        </tr>
        <tr v-if="announcements.length === 0">
          <td colspan="6" class="empty">No proposal announcements yet. Create one to open proposal submissions.</td>
        </tr>
      </tbody>
    </table>

    <!-- Create / Edit Modal -->
    <div v-if="showModal" class="modal-overlay" @click.self="closeModal">
      <div class="modal-box create-announcement-modal">
        <!-- Header: dark purple announcement banner -->
        <header class="modal-header">
          <span class="header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M4 10v3a1 1 0 0 0 1 1h2l4 4V5L7 9H5a1 1 0 0 0-1 1z" stroke-linecap="round"
                stroke-linejoin="round" />
              <path d="M14 9a4 4 0 0 1 0 6" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M17 6.5a7.5 7.5 0 0 1 0 11" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
          </span>
          <div class="header-text">
            <h3 class="header-title">
              {{ isEditing ? 'Edit Proposal Announcement' : 'Create Proposal Announcement' }}
            </h3>
            <p class="header-subtitle">Set the submission window and choose the priority agendas for this call.</p>
          </div>
          <button type="button" class="header-close" @click="closeModal" :disabled="saving" aria-label="Close">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
              <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
            </svg>
          </button>
        </header>

        <div class="modal-body">
          <!-- 1. Announcement details. The name is part of the published announcement, so
               it is shown read-only when an existing call is updated. -->
          <section class="form-section">
            <div class="section-head">
              <h4 class="section-title"><span class="section-badge">1</span>Announcement details</h4>
              <p v-if="isEditing" class="section-note">
                The announcement name cannot be changed after the call is created.
              </p>
            </div>
            <div class="form-group">
              <label>Announcement name</label>
              <input v-if="!isEditing" v-model="form.name" type="text" class="form-input"
                placeholder="e.g., FY 2026 - 1st Call for Proposals" />
              <p v-else class="readonly-value">{{ form.name || '—' }}</p>
            </div>
          </section>

          <!-- 2. Submission window -->
          <section class="form-section">
            <div class="section-head">
              <h4 class="section-title"><span class="section-badge">2</span>Submission window</h4>
            </div>
            <div class="form-row">
              <div class="form-group">
                <label>Start date</label>
                <input v-model="form.startDate" type="date" class="form-input" />
              </div>
              <div class="form-group">
                <label>End date</label>
                <input v-model="form.endDate" type="date" class="form-input" :min="form.startDate || undefined" />
              </div>
              <div class="form-group">
                <label>Status</label>
                <select v-model="form.status" class="form-input">
                  <option value="DRAFT">Draft</option>
                  <option value="ACTIVE">Open</option>
                  <option value="CLOSED">Closed</option>
                </select>
              </div>
            </div>
            <p v-if="form.status === 'ACTIVE'" class="helper-banner">
              <span class="banner-icon" aria-hidden="true">
                <!-- viewBox cropped to the glyph (no built-in left margin) plus an
                     intrinsic size, so the icon never depends on a CSS selector -->
                <svg width="11" height="11" viewBox="2 2 20 20" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="9" />
                  <path d="M12 11v5M12 8h.01" stroke-linecap="round" />
                </svg>
              </span>
              <span class="banner-text">Saving with Open status notifies all proponents that submissions are open.</span>
            </p>
          </section>

          <!-- 3. Priority agenda: chosen when the call is created. Updating an existing
               call shows the saved selection read-only. -->
          <section class="form-section">
            <div class="section-head">
              <h4 class="section-title"><span class="section-badge">3</span>Priority agenda</h4>
              <p v-if="isEditing" class="section-note">
                The priority agenda cannot be changed after the call is created.
              </p>
            </div>
            <template v-if="isEditing">
              <ul v-if="selectedPrograms.length" class="readonly-list">
                <li v-for="program in selectedPrograms" :key="program.key" class="readonly-item">
                  <span class="program-badge" aria-hidden="true">{{ program.key.charAt(0).toUpperCase() }}</span>
                  <span class="option-text">
                    <span class="option-label">{{ program.label }}</span>
                    <span class="option-description">{{ program.description }}</span>
                  </span>
                </li>
              </ul>
              <p v-else class="readonly-value">No priority program is selected for this call.</p>
            </template>
            <template v-else>
              <div class="selection-row">
                <p class="selection-counter" aria-live="polite">
                  {{ selectedProgramCount }} of {{ PROGRAM_KEYS.length }} programs selected
                </p>
                <button type="button" class="check-all-btn" @click="toggleAllPrograms">
                  {{ allProgramsSelected ? 'Uncheck All' : 'Check All' }}
                </button>
              </div>
              <div class="option-list">
                <label v-for="program in RIIDE_PROGRAMS" :key="program.key" class="option-item"
                  :class="{ selected: form.priorityAgendas.includes(program.key) }">
                  <input type="checkbox" :value="program.key" v-model="form.priorityAgendas" />
                  <span class="program-badge" aria-hidden="true">{{ program.key.charAt(0).toUpperCase() }}</span>
                  <span class="option-text">
                    <span class="option-label">{{ program.label }}</span>
                    <span class="option-description">{{ program.description }}</span>
                  </span>
                </label>
              </div>
            </template>
          </section>

          <!-- 4. Announcement reminders: chosen when the call is created. Updating an
               existing call shows the saved reminders read-only. -->
          <section class="form-section">
            <div class="section-head">
              <h4 class="section-title"><span class="section-badge">4</span>Announcement reminders</h4>
              <p v-if="isEditing" class="section-note">
                The announcement reminders cannot be changed after the call is created.
              </p>
              <p v-else class="section-note">Select the reminders to include in the announcement.</p>
            </div>
            <template v-if="isEditing">
              <ul v-if="selectedReminders.length" class="readonly-list">
                <li v-for="reminder in selectedReminders" :key="reminder.key" class="readonly-item">
                  <span class="option-text">
                    <span class="reminder-text">{{ reminder.text }}</span>
                  </span>
                </li>
              </ul>
              <p v-else class="readonly-value">No reminder is included in this call.</p>
            </template>
            <div v-else class="option-list">
              <label v-for="reminder in ANNOUNCEMENT_REMINDERS" :key="reminder.key"
                class="option-item reminder-item"
                :class="{ selected: form.reminderIds.includes(reminder.key) }">
                <input type="checkbox" :value="reminder.key" v-model="form.reminderIds" />
                <span class="option-text">
                  <span class="reminder-text">{{ reminder.text }}</span>
                </span>
              </label>
            </div>
          </section>

          <!-- 5. Additional announcement: written when the call is created. Updating an
               existing call shows the saved text read-only. -->
          <section class="form-section">
            <div class="section-head">
              <h4 class="section-title"><span class="section-badge">5</span>Additional announcement (Optional)</h4>
              <p v-if="isEditing" class="section-note">
                The additional announcement cannot be changed after the call is created.
              </p>
            </div>
            <div class="form-group">
              <textarea v-if="!isEditing" v-model="form.additionalAnnouncement" class="form-input form-textarea" rows="4"
                placeholder="Add special instructions, funding details, submission links, contact information, or other information for this call..."></textarea>
              <p v-else class="readonly-value readonly-value--multiline">
                {{ form.additionalAnnouncement || 'No additional announcement was added to this call.' }}
              </p>
            </div>
          </section>

          <p v-if="formError" class="error-text form-error">{{ formError }}</p>
        </div>

        <!-- Footer: selection/status summary left, actions right -->
        <footer class="modal-footer">
          <p class="footer-summary">
            {{ selectedProgramCount }} of {{ PROGRAM_KEYS.length }} programs selected · Status: {{ formStatusLabel }}
          </p>
          <div class="modal-actions">
            <button class="cancel-btn" @click="closeModal" :disabled="saving">Cancel</button>
            <button class="confirm-btn create-announcement-button" @click="saveAnnouncement" :disabled="saving">
              <svg v-if="!isEditing && !saving" class="btn-icon" viewBox="0 0 24 24" fill="none"
                stroke="currentColor" stroke-width="2.5" aria-hidden="true">
                <path d="M12 5v14M5 12h14" stroke-linecap="round" />
              </svg>
              <span class="confirm-btn-label">{{ saving ? 'Saving...' : (isEditing ? 'Save Changes' : 'Create Announcement') }}</span>
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
import { announcementStatus } from '@/utils/announcementStatus'
import {
  announcementCreatePayload,
  announcementFormError,
  announcementUpdatePayload,
} from '@/utils/announcementForm'
import {
  RIIDE_PROGRAMS,
  ANNOUNCEMENT_REMINDERS,
  PROGRAM_KEYS,
  defaultReminderKeys,
  normalizeProgramKeys,
  normalizeReminderKeys,
  isAllSelected,
  toggleAllSelection,
} from '@/utils/announcementOptions'

const { dialogState, showAlert, showConfirm } = useDialog()

const announcements = ref([])
const loading = ref(false)
const error = ref(null)

const showModal = ref(false)
const isEditing = ref(false)
const editingId = ref(null)
const saving = ref(false)
const formError = ref(null)
const form = ref(emptyForm())

function emptyForm() {
  return {
    name: '',
    startDate: '',
    endDate: '',
    status: 'ACTIVE',
    // A new announcement starts with no agenda selected and with every standard
    // reminder selected; both can be changed before saving.
    priorityAgendas: [],
    reminderIds: defaultReminderKeys(),
    additionalAnnouncement: '',
  }
}

// The Check All button label always follows the actual checkbox selection.
const allProgramsSelected = computed(() => isAllSelected(form.value.priorityAgendas, PROGRAM_KEYS))

// Selection counter shown under the section heading, derived from the same state.
const selectedProgramCount = computed(() => normalizeProgramKeys(form.value.priorityAgendas).length)

// Saved selections of the call being edited, resolved to their display text so the
// read-only sections show exactly what is stored (no editing control is rendered).
const selectedPrograms = computed(() =>
  RIIDE_PROGRAMS.filter((program) => form.value.priorityAgendas.includes(program.key))
)
const selectedReminders = computed(() =>
  ANNOUNCEMENT_REMINDERS.filter((reminder) => form.value.reminderIds.includes(reminder.key))
)

// Footer summary label: reuses the existing announcement status helper, so the
// displayed status always matches the table's Draft / Scheduled / Open / Closed.
const formStatusLabel = computed(() =>
  announcementStatus({
    status: form.value.status,
    startDate: form.value.startDate,
    endDate: form.value.endDate,
  })
)

function toggleAllPrograms() {
  form.value.priorityAgendas = toggleAllSelection(form.value.priorityAgendas, PROGRAM_KEYS)
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

function isActiveNow(announcement) {
  const today = localTodayISO()
  return announcement.status === 'ACTIVE' && announcement.startDate <= today && today <= announcement.endDate
}

const activeAnnouncement = computed(() => announcements.value.find(isActiveNow) || null)

// An announcement can be deleted only when it is not active and no proponent has
// submitted a proposal under it.
function canDelete(announcement) {
  return announcement.status !== 'ACTIVE' && !announcement.hasSubmittedProposals
}

function statusClass(announcement) {
  return {
    Draft: 'status-draft',
    Scheduled: 'status-scheduled',
    Open: 'status-active',
    Closed: 'status-closed',
  }[announcementStatus(announcement)] || ''
}

function windowLabel(announcement) {
  const today = localTodayISO()
  if (announcement.endDate < today) return 'Expired'
  if (isActiveNow(announcement)) return 'Accepting submissions'
  if (announcement.status === 'ACTIVE' && announcement.startDate > today) return `Opens ${formatDate(announcement.startDate)}`
  return '—'
}

function windowClass(announcement) {
  const today = localTodayISO()
  if (isActiveNow(announcement)) return 'window-open'
  if (announcement.endDate < today) return 'window-expired'
  if (announcement.status === 'ACTIVE' && announcement.startDate > today) return 'window-upcoming'
  return ''
}

function extractErrorMessage(err, fallback) {
  const data = err.response?.data
  if (data && typeof data === 'object' && data.error) return data.error
  if (typeof data === 'string' && data.trim()) return data
  return fallback
}

// ===== LOAD =====

async function loadAnnouncements() {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/proposal-announcements')
    announcements.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = extractErrorMessage(err, 'Failed to load proposal announcements.')
    announcements.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadAnnouncements)

// ===== CREATE / EDIT =====

function openCreateModal() {
  isEditing.value = false
  editingId.value = null
  form.value = emptyForm()
  formError.value = null
  showModal.value = true
}

function openEditModal(announcement) {
  isEditing.value = true
  editingId.value = announcement.id
  form.value = {
    // Only the submission window and the status can be updated; the remaining fields are
    // loaded so the modal can show the saved call information read-only.
    name: announcement.name,
    startDate: announcement.startDate,
    endDate: announcement.endDate,
    status: announcement.status,
    // Restore the saved selection (never the new-announcement defaults); legacy
    // announcements without these values load as an empty selection / empty text.
    priorityAgendas: normalizeProgramKeys(announcement.priorityAgendas),
    reminderIds: normalizeReminderKeys(announcement.reminderIds),
    additionalAnnouncement: announcement.additionalAnnouncement || '',
  }
  formError.value = null
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  formError.value = null
}

async function saveAnnouncement() {
  // Client-side validation (the backend re-validates everything). Only a NEW call requires
  // the announcement name: an existing call already has one, the update never sends it, and
  // the restricted fields are never asked for again (utils/announcementForm.js).
  const invalid = announcementFormError(form.value, { isEditing: isEditing.value })
  if (invalid) {
    formError.value = invalid
    return
  }

  saving.value = true
  formError.value = null
  try {
    if (isEditing.value) {
      // Updating an existing call sends the submission window and the status only: the
      // name, priority agenda, announcement reminders and additional announcement keep the
      // values they were created with (the backend only accepts these three fields anyway).
      await api.put(
        `/api/proposal-announcements/${editingId.value}`,
        announcementUpdatePayload(form.value)
      )
    } else {
      await api.post('/api/proposal-announcements', announcementCreatePayload(form.value))
    }

    const becameActive = form.value.status === 'ACTIVE'
    closeModal()
    await loadAnnouncements()
    await showAlert(
      becameActive
        ? 'Proposal announcement saved and activated. All proponents have been notified.'
        : 'Proposal announcement saved successfully.',
      { type: 'success', title: isEditing.value ? 'Announcement Updated' : 'Announcement Created' }
    )
  } catch (err) {
    console.error(err)
    formError.value = extractErrorMessage(err, 'Failed to save the proposal announcement. Please try again.')
  } finally {
    saving.value = false
  }
}

// ===== ACTIVATE =====

async function activateAnnouncementAction(announcement) {
  const confirmed = await showConfirm(
    `Activate "${announcement.name}" (${formatDate(announcement.startDate)} – ${formatDate(announcement.endDate)})? All proponents will be notified that submissions are open.`,
    { title: 'Activate Announcement', type: 'warning', confirmText: 'Activate' }
  )
  if (!confirmed) return

  try {
    await api.put(`/api/proposal-announcements/${announcement.id}/activate`)
    await loadAnnouncements()
    await showAlert('Proposal announcement activated. All proponents have been notified.', {
      type: 'success',
      title: 'Announcement Activated',
    })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to activate the announcement.'), {
      type: 'error',
      title: 'Activation Failed',
    })
  }
}

// ===== DELETE =====

async function deleteAnnouncementAction(announcement) {
  const confirmed = await showConfirm(
    `Are you sure you want to delete "${announcement.name}"? This action cannot be undone.`,
    { title: 'Delete Announcement', type: 'danger', confirmText: 'Delete' }
  )
  if (!confirmed) return

  try {
    await api.delete(`/api/proposal-announcements/${announcement.id}`)
    await loadAnnouncements()
    await showAlert('Proposal announcement deleted.', { type: 'success', title: 'Announcement Deleted' })
  } catch (err) {
    console.error(err)
    await showAlert(extractErrorMessage(err, 'Failed to delete the announcement.'), {
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

.announcement-name {
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

.status-scheduled {
  background: #dbeafe;
  color: #1e40af;
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

/* ===== Create / Edit Announcement modal ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(23, 20, 43, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  z-index: 999;
}

.modal-box {
  display: flex;
  flex-direction: column;
  width: 92vw;
  max-width: 1140px;
  max-height: 90vh;
  background: #f8f8fc;
  border: 1px solid #dedeea;
  border-radius: 12px;
  overflow: hidden;
  box-sizing: border-box;
  box-shadow: 0 18px 40px rgba(41, 35, 79, 0.28);
}

/* Header: dark purple announcement banner */
.modal-header {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px 11px 14px;
  background: #29234f;
  border-bottom: 3px solid #f5c400;
}

.header-icon {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: #ffcc00;
  color: #29234f;
}

.header-icon svg {
  width: 18px;
  height: 18px;
}

.header-text {
  flex: 1 1 auto;
  min-width: 0;
}

.header-title {
  margin: 0;
  font-size: 17.5px;
  font-weight: 700;
  line-height: 1.3;
  color: #ffffff;
}

.header-subtitle {
  margin: 3px 0 0 0;
  font-size: 12.5px;
  line-height: 1.4;
  color: #d9d5ea;
}

.header-close {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  padding: 0;
  border: none;
  border-radius: 7px;
  background: #3b3566;
  color: #e8e6f6;
  cursor: pointer;
}

.header-close svg {
  width: 14px;
  height: 14px;
}

.header-close:hover {
  background: #4a4380;
}

.header-close:focus-visible {
  outline: 2px solid #ffcc00;
  outline-offset: 2px;
}

.header-close:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Body (scrolls; header and footer stay put) */
.modal-body {
  flex: 1 1 auto;
  min-height: 0;
  padding: 14px;
  overflow-y: auto;
}

/* Numbered section headings */
.form-section {
  margin-top: 14px;
  text-align: left;
}

.form-section:first-of-type {
  margin-top: 0;
}

/* Numbered section headings — modal-scoped so headings on other pages keep their
   own styles. Deliberately larger than field labels (12.5px); the modal's own
   header title (.header-title) is untouched. */
.create-announcement-modal .section-head {
  /* 9px between a section heading and the content it labels. */
  margin-bottom: 9px;
}

.create-announcement-modal .section-title {
  display: flex;
  align-items: center; /* number badge stays centred on the heading text */
  gap: 9px;
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.3;
  color: #302954;
  text-align: left;
}

.create-announcement-modal .section-badge {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #302954;
  color: #ffffff;
  font-size: 12.5px;
  font-weight: 700;
  line-height: 1;
}

.section-note {
  margin: 4px 0 0 0;
  font-size: 11.5px;
  line-height: 1.45;
  color: #77758f;
  text-align: left;
}

/* Read-only information of an existing call: the restricted fields are displayed as
   plain text (no input, textarea, select or checkbox is rendered for them). */
.readonly-value {
  margin: 0;
  padding: 10px 12px;
  background: #f7f7fb;
  border: 1px solid #e0e0ea;
  border-radius: 8px;
  color: #33314f;
  font-size: 13px;
  line-height: 1.5;
  text-align: left;
  white-space: pre-line;
}

.readonly-value--multiline {
  min-height: 44px;
}

.readonly-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.readonly-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 11px;
  background: #f7f7fb;
  border: 1px solid #e0e0ea;
  border-radius: 8px;
  text-align: left;
}

/* Form controls */
.form-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.form-group label {
  margin-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.3;
  color: #302954;
}

.form-input {
  width: 100%;
  min-height: 38px;
  padding: 6px 10px;
  border: 1px solid #dcdce8;
  border-radius: 6px;
  background: #ffffff;
  color: #302954;
  font-size: 13.5px;
  line-height: 1.3;
  outline: none;
  box-sizing: border-box;
}

/* ===== Submission window: one identical box for all three controls =====
   The global rules in assets/main.css style form controls with
   "input[type=...], select, textarea" (specificity 0,1,1), which can beat a plain
   class rule and let the two date inputs and the native <select> resolve
   different padding/font metrics. These modal-scoped selectors outrank them, so
   Start date, End date and Status always share the same box and vertical
   alignment. The textarea is untouched (it keeps .form-textarea). */
.create-announcement-modal .form-row .form-group {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.create-announcement-modal .form-row .form-group label {
  display: block;
  margin: 0 0 6px 0;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.3;
}

.create-announcement-modal .form-row input.form-input,
.create-announcement-modal .form-row select.form-input {
  display: block;
  width: 100%;
  height: 38px;
  min-height: 38px;
  max-height: 38px;
  margin: 0;
  padding: 0 10px;
  border: 1px solid #dcdce8;
  border-radius: 6px;
  background: #ffffff;
  color: #302954;
  font-family: inherit;
  font-size: 13.5px;
  /* The content box is 36px (38px - 2px border). Matching the line box to it
     centres the text and the native dropdown arrow in both control types, with
     no transform/positioning offset. */
  line-height: 36px;
  box-sizing: border-box;
  vertical-align: middle;
}

.create-announcement-modal .form-row select.form-input {
  /* Keep the selected value clear of the native dropdown arrow. */
  padding-right: 30px;
}

.form-input::placeholder {
  color: #9c9ab2;
}

.form-input:focus {
  border-color: #29234f;
  box-shadow: 0 0 0 2px rgba(41, 35, 79, 0.12);
}

/* Give the Draft / Open / Closed options room instead of running together
   (the select's own padding comes from the modal-scoped rule above). */
select.form-input option {
  padding: 6px 10px;
}

/* Start date, End date and Status share one row inside the Submission window. */
.form-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  align-items: start;
  gap: 10px;
}

/* Submission-window helper banner */
.helper-banner {
  display: flex;
  /* Icon and message share the same vertical centre — no per-icon offset needed. */
  align-items: center;
  /* The single space between the icon and the message text. Nothing else adds any:
     the icon has no margin/padding and no literal whitespace sits between them. */
  gap: 6px;
  margin: 10px 0 0 0;
  /* Normal inner padding only; the icon starts at this edge (its SVG viewBox is
     cropped to the glyph, so it carries no built-in leading margin). */
  padding: 8px 10px;
  background: #fff7d8;
  border: 1px solid #f4d77b;
  border-radius: 7px;
  font-size: 11.5px;
  line-height: 1.45;
  color: #6b5316;
  text-align: left;
}

.banner-icon {
  /* Never shrinks, so the wrapped message can never overlap the icon. */
  flex: 0 0 auto;
  display: flex;
  width: 11px;
  height: 11px;
  color: #b8860b;
}

.banner-icon svg {
  /* Glyph-sized box matching the cropped viewBox: the icon renders at the same
     visual size as before, but flush with the banner's padding edge. The SVG also
     carries width/height attributes, so it keeps this size even if this selector
     ever fails to apply. */
  display: block;
  width: 11px;
  height: 11px;
}

.banner-text {
  /* Lets the message wrap naturally inside the banner instead of forcing width. */
  min-width: 0;
}

/* ===== Footer: selection/status summary left, actions right ===== */
.modal-footer {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px 12px;
  padding: 10px 14px;
  background: #ffffff;
  border-top: 1px solid #e6e6f0;
}

.footer-summary {
  margin: 0;
  min-width: 0;
  font-size: 11.5px;
  line-height: 1.45;
  color: #77758f;
  text-align: left;
}

.modal-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
  margin-left: auto;
}

/* ===== Footer actions: both buttons are exactly the same box =====
   Modal-scoped so the global button rules in assets/main.css cannot interfere,
   and specific enough that the shared size is never overridden. */
.create-announcement-modal .modal-footer .cancel-btn,
.create-announcement-modal .modal-footer .confirm-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 190px;
  min-width: 0;
  max-width: 100%;
  height: 38px;
  min-height: 38px;
  margin: 0;
  padding: 0 14px;
  border: none;
  border-radius: 7px;
  box-sizing: border-box;
  cursor: pointer;
  font-family: inherit;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1;
  white-space: nowrap;
  text-align: center;
}

/* The label is a nested <span>, and the global reset in assets/css/base.css
   ("* { font-weight: normal }") targets that span directly — a direct declaration
   beats inheritance, which is why the label rendered at weight 400. Weighting the
   span explicitly restores the bold primary label. */
.create-announcement-modal .modal-footer .confirm-btn,
.create-announcement-modal .modal-footer .confirm-btn .confirm-btn-label {
  font-weight: 700;
}

.btn-icon {
  flex: 0 0 auto;
  width: 13px;
  height: 13px;
}

.cancel-btn {
  background: #eceaf5;
  color: #302954;
}

.cancel-btn:hover {
  background: #dedbf0;
}

.confirm-btn {
  background: #ffcc00;
  color: #302954;
  font-weight: 700;
}

.confirm-btn:hover {
  background: #f5c400;
}

.confirm-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== Priority agenda toolbar: counter left, Check All right ===== */
.selection-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.selection-counter {
  margin: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #eceaf5;
  color: #302954;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.4;
  text-align: left;
}

.check-all-btn {
  flex: 0 0 auto;
  padding: 6px 11px;
  background: #ffffff;
  border: 1px solid #cfcde0;
  border-radius: 6px;
  color: #302954;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.check-all-btn:hover {
  background: #f1eff8;
}

.check-all-btn:focus-visible {
  outline: 2px solid #29234f;
  outline-offset: 2px;
}

/* ===== Program and reminder cards ===== */
.option-list {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.option-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  min-height: 50px;
  padding: 9px 11px;
  background: #ffffff;
  border: 1px solid #e0e0ea;
  border-radius: 8px;
  cursor: pointer;
  text-align: left;
  box-sizing: border-box;
  transition:
    border-color 0.15s ease,
    background 0.15s ease;
}

.option-item.reminder-item {
  align-items: center;
  min-height: 44px;
}

.option-item:hover {
  border-color: #c3c0d8;
}

.option-item.selected {
  border-color: #ffcc00;
  background: #fffdf3;
}

.option-item:focus-within {
  border-color: #29234f;
  box-shadow: 0 0 0 2px rgba(41, 35, 79, 0.12);
}

.option-item input[type='checkbox'] {
  flex: 0 0 auto;
  width: 15px;
  height: 15px;
  margin: 2px 0 0 0;
  accent-color: #29234f;
  cursor: pointer;
}

.option-item.reminder-item input[type='checkbox'] {
  margin: 0;
}

.program-badge {
  flex: 0 0 auto;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 7px;
  background: #29234f;
  color: #ffffff;
  font-size: 12px;
  font-weight: 700;
}

.option-text {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
  text-align: left;
}

.option-label {
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.35;
  color: #302954;
  overflow-wrap: anywhere;
}

.option-description {
  font-size: 11.5px;
  line-height: 1.45;
  color: #77758f;
  overflow-wrap: anywhere;
}

.reminder-text {
  font-size: 12.5px;
  line-height: 1.5;
  color: #4a4666;
  overflow-wrap: anywhere;
}

.form-error {
  margin: 10px 0 0 0;
  font-size: 11.5px;
}

/* ===== Responsive: tablet keeps two columns, phones stack everything ===== */
@media (max-width: 900px) {
  .form-row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 620px) {
  .modal-overlay {
    padding: 10px;
  }

  .modal-box {
    width: 94vw;
    max-height: 92vh;
  }

  .modal-body {
    padding: 12px;
  }

  /* Keep the title on one line on narrow screens. */
  .header-title {
    font-size: 16px;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .selection-row {
    align-items: stretch;
  }

  .check-all-btn {
    width: 100%;
    text-align: center;
  }

  .modal-footer {
    justify-content: stretch;
  }

  .modal-actions {
    width: 100%;
  }

  .create-announcement-modal .modal-footer .cancel-btn,
  .create-announcement-modal .modal-footer .confirm-btn {
    width: 100%;
  }
}

.form-textarea {
  resize: vertical;
  min-height: 90px;
  font-family: inherit;
  line-height: 1.45;
}
</style>
