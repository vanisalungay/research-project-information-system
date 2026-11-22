<template>
  <div class="page">
    <!-- Page header: the title on the left, the single Create Account action aligned
         on the right. Both are vertically centred, and the row stacks on a small
         screen instead of squeezing the two apart. -->
    <div class="header">
      <h2>User Accounts</h2>

      <div class="header-actions">
        <button class="create-btn" @click="showModal = true">
          + Create Account
        </button>
      </div>
    </div>

    <!-- Search -->
    <div class="toolbar">
      <input
        v-model="search"
        type="text"
        class="search-input"
        placeholder="Search by name, email, or role..."
      />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading users...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn btn-secondary" @click="loadUsers">Retry</button>
    </div>

    <!-- Table. The wrapper scrolls sideways on a narrow screen instead of letting the
         table overflow the page container. -->
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Email</th>
            <th>Role</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id">
            <td>{{ user.name }}</td>
            <td>{{ user.email }}</td>
            <td>
              <div class="role-list">
                <span
                  v-for="role in rolesOf(user)"
                  :key="role"
                  class="role-badge"
                  :class="getRoleClass(role)"
                >
                  {{ formatRole(role) }}
                </span>
                <!-- A role the account no longer holds is still part of its record: a
                     revoked Dean assignment is shown as Dean (Revoked), never replaced by
                     a role the account never had. -->
                <span
                  v-for="role in revokedRolesOf(user)"
                  :key="`revoked-${role}`"
                  class="role-badge role-revoked"
                >
                  {{ formatRole(role) }} (Revoked)
                </span>
              </div>
            </td>
            <td>
              <!-- For a Dean, the status is the appointment's, from the same row Dean Role
                   Management reads: Active or Revoked, never a different word for the same
                   state. Other accounts keep the account status. -->
              <span
                v-if="user.deanStatus"
                class="status-badge"
                :class="user.deanStatus === 'ACTIVE' ? 'dean-status-active' : 'dean-status-revoked'"
              >
                {{ deanAssignmentStatusLabel(user.deanStatus) }}
              </span>
              <span v-else class="status-badge" :class="statusTone(user.status)">
                {{ accountStatusLabel(user.status) || 'Active' }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="reset-btn btn-secondary" @click="openResetModal(user)">
                Reset Password
              </button>
              <!-- A Dean account keeps its Dean role management: the designation and the
                   college it endorses for, or revoking the role. The role itself is
                   granted when the account is created, through the standard Create
                   Account flow above. -->
              <button
                v-if="isDean(user) && user.deanStatus !== 'REVOKED'"
                class="dean-manage-btn btn-edit"
                @click="openDeanModal(user)"
              >
                Manage Dean Role
              </button>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="5" class="empty">No users found.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Create User Modal -->
    <CreateUserModal
      v-if="showModal"
      @close="showModal = false"
      @created="loadUsers"
    />

    <!-- Manage Dean for one existing Dean account, opened from that account's row. This is
         the same shared flow Dean Role Management uses: the same details dialog and the
         same Replace Dean workflow, so the two modules cannot behave differently. -->
    <ManageDeanFlow
      v-if="showDeanModal && selectedAssignment"
      :assignment="selectedAssignment"
      @close="closeDeanModal"
      @changed="onDeanChanged"
    />

    <!-- Reset Password Modal: the same compact dialog language as Create user account. -->
    <div v-if="showResetModal" class="adm-overlay" @click.self="closeResetModal">
      <div
        class="adm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reset-password-title"
      >
        <header class="adm-header">
          <span class="adm-header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
              stroke-linecap="round" stroke-linejoin="round">
              <circle cx="8" cy="15" r="4" />
              <path d="m11 12 8-8" />
              <path d="m18 5 2 2" />
              <path d="m15 8 2 2" />
            </svg>
          </span>
          <div class="adm-heading">
            <h2 id="reset-password-title">Reset password</h2>
            <p>Generate a temporary password for this user to regain access.</p>
          </div>
          <button
            type="button"
            class="adm-close"
            aria-label="Close reset password modal"
            :disabled="resetLoading"
            @click="closeResetModal"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="adm-body">
          <p v-if="resetError" class="adm-error" role="alert">{{ resetError }}</p>
          <p v-if="resetSuccess" class="adm-success" role="status">{{ resetSuccess }}</p>

          <section class="adm-panel" aria-labelledby="reset-account-title">
            <h3 id="reset-account-title" class="adm-panel-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20 21a8 8 0 0 0-16 0" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>Selected account</span>
            </h3>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Full name</span>
              <span class="adm-credential-value">{{ selectedUser?.name || '—' }}</span>
            </div>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Email</span>
              <span class="adm-credential-value">{{ selectedUser?.email || '—' }}</span>
            </div>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Role</span>
              <span class="adm-credential-value">{{ roleLabels(selectedUser) }}</span>
            </div>
          </section>

          <div class="adm-field">
            <label for="resetNewPassword">New temporary password</label>
            <PasswordInput
              id="resetNewPassword"
              v-model="newPassword"
              placeholder="Enter a temporary password"
              class="adm-input"
              autocomplete="new-password"
              aria-describedby="resetNewPassword-hint reset-password-requirements"
            />
            <PasswordFieldHint id="resetNewPassword-hint" kind="new" />
          </div>

          <div class="adm-field">
            <label for="resetConfirmPassword">Confirm password</label>
            <PasswordInput
              id="resetConfirmPassword"
              v-model="confirmPassword"
              placeholder="Confirm the temporary password"
              class="adm-input"
              autocomplete="new-password"
              aria-describedby="resetConfirmPassword-hint"
            />
            <PasswordFieldHint id="resetConfirmPassword-hint" kind="confirm" />
          </div>

          <PasswordRequirements
            id="reset-password-requirements"
            :password="newPassword"
            :confirm="confirmPassword"
          />

          <p class="adm-hint">
            The password changes immediately. Share it securely with the user; they can
            change it later from Account Settings.
          </p>
        </div>

        <footer class="adm-footer">
          <button
            type="button"
            class="adm-btn adm-btn--secondary"
            :disabled="resetLoading"
            @click="closeResetModal"
          >
            Cancel
          </button>
          <button
            type="button"
            class="adm-btn adm-btn--primary"
            :disabled="resetLoading || !resetPasswordValid"
            @click="resetPassword"
          >
            <span v-if="resetLoading" class="adm-spinner" aria-hidden="true"></span>
            <span>{{ resetLoading ? 'Resetting…' : 'Reset password' }}</span>
            <svg v-if="!resetLoading" viewBox="0 0 24 24" fill="none" stroke="currentColor"
              stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <circle cx="8" cy="15" r="4" />
              <path d="m11 12 8-8" />
              <path d="m18 5 2 2" />
            </svg>
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import '@/assets/adminModal.css'
import api from '@/utils/api'
import { UserRole } from '@/stores/userData'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'
import PasswordInput from '@/components/PasswordInput.vue'
import PasswordRequirements from '@/components/PasswordRequirements.vue'
import PasswordFieldHint from '@/components/PasswordFieldHint.vue'
import ManageDeanFlow from '@/views/Admin/ManageDeanFlow.vue'
import { statusTone } from '@/utils/statusTone.js'
import { accountStatusLabel, deanAssignmentStatusLabel } from '@/utils/statusCatalog.js'
import { passwordFormReady, passwordPolicyMessage } from '@/utils/passwordPolicy'

const users = ref([])
const showModal = ref(false)
// The Manage Dean Role screen is opened for one account at a time, from that account's
// row, so it carries the appointment it was opened for.
const showDeanModal = ref(false)
const selectedAssignment = ref(null)
const loading = ref(false)
const error = ref(null)
const search = ref('')

// Reset password state
const showResetModal = ref(false)
const selectedUser = ref(null)
const newPassword = ref('')
const confirmPassword = ref('')
const resetError = ref('')
const resetSuccess = ref('')
const resetLoading = ref(false)

// The reset button waits for a password that satisfies every requirement and a
// matching confirmation; the server re-validates the same policy.
const resetPasswordValid = computed(() => passwordFormReady(newPassword.value, confirmPassword.value))

const filteredUsers = computed(() => {
  const keyword = search.value.toLowerCase().trim()
  if (!keyword) return users.value
  return users.value.filter(u =>
    (u.name || '').toLowerCase().includes(keyword) ||
    (u.email || '').toLowerCase().includes(keyword) ||
    rolesOf(u).join(' ').toLowerCase().includes(keyword) ||
    revokedRolesOf(u).join(' ').toLowerCase().includes(keyword)
  )
})

/**
 * Every role the account holds. The API returns the primary role first and the full
 * assignment set; older payloads carry only `role`, which is treated as a one-role list.
 */
const rolesOf = (user) => {
  if (Array.isArray(user?.roles)) return user.roles.filter(Boolean)
  // Older payloads carry only the primary role. The primary column is historical, so it
  // is used only when the API sent no roles list at all.
  return user?.role ? [user.role] : []
}

const revokedRolesOf = (user) => (
  Array.isArray(user?.revokedRoles) ? user.revokedRoles.filter(Boolean) : []
)

const roleLabels = (user) => [
  ...rolesOf(user).map(formatRole),
  ...revokedRolesOf(user).map((role) => `${formatRole(role)} (Revoked)`),
].join(', ') || 'No active role'

const formatRole = (role) => {
  const roleMap = {
    'PROPONENT': 'Proponent',
    'DEAN': 'Dean',
    'RPS_ADMIN': 'RPS Admin',
    'RPS_STAFF': 'RPS Staff',
    'OVCRIGE': 'OVCRIGE',
    'OVCAF': 'OVCAF',
    'REC': 'REC',
    'OC': 'Chancellor',
  }
  return roleMap[role] || role
}

const getRoleClass = (role) => {
  const classMap = {
    'PROPONENT': 'role-proponent',
    'DEAN': 'role-dean',
    'RPS_ADMIN': 'role-admin',
    'RPS_STAFF': 'role-staff',
    'OVCRIGE': 'role-ovcrige',
    'OVCAF': 'role-ovcaf',
    'REC': 'role-rec',
    'OC': 'role-oc',
  }
  return classMap[role] || ''
}

const loadUsers = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/users')
    users.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error('Failed to load users:', err)
    error.value = 'Failed to load user accounts.'
    users.value = []
  } finally {
    loading.value = false
  }
}

/** True when the account holds the Dean role, even as one of several roles. */
const isDean = (user) => rolesOf(user).some((role) => String(role).toUpperCase() === UserRole.DEAN)

/**
 * Opens the Manage Dean Role screen for the account of the clicked row: the sitting Dean
 * and the appointment that will be replaced. The real appointment (with the date it
 * started) is preferred; an account-level fallback is used if it cannot be read.
 */
const openDeanModal = async (user) => {
  try {
    // Replacement is addressed by the ACTIVE appointment id, not just the account, so
    // the workflow opens only once that appointment has been loaded.
    const response = await api.get('/api/users/deans/assignments', { params: { status: 'ACTIVE' } })
    const match = (response.data || []).find((a) => String(a.userId) === String(user.id))
    if (!match) {
      error.value = 'No active Dean appointment was found for this account.'
      return
    }
    selectedAssignment.value = match
    showDeanModal.value = true
  } catch (requestError) {
    console.error('Could not load the Dean appointment', requestError)
    error.value = 'The Dean appointment could not be loaded. Please try again.'
  }
}

const closeDeanModal = () => {
  showDeanModal.value = false
  selectedAssignment.value = null
}

/**
 * The shared flow completed a replacement. Reload the list behind it; the flow keeps its
 * credentials panel open so the administrator can hand them over.
 */
const onDeanChanged = async () => {
  await loadUsers()
}

const openResetModal = (user) => {
  selectedUser.value = user
  newPassword.value = ''
  confirmPassword.value = ''
  resetError.value = ''
  resetSuccess.value = ''
  showResetModal.value = true
}

const closeResetModal = () => {
  showResetModal.value = false
  selectedUser.value = null
  newPassword.value = ''
  confirmPassword.value = ''
  resetError.value = ''
  resetSuccess.value = ''
}

const resetPassword = async () => {
  resetError.value = ''
  resetSuccess.value = ''

  if (!newPassword.value.trim()) {
    resetError.value = 'Please enter a new password.'
    return
  }

  const policyMessage = passwordPolicyMessage(newPassword.value)
  if (policyMessage) {
    resetError.value = policyMessage
    return
  }

  if (newPassword.value !== confirmPassword.value) {
    resetError.value = 'Passwords do not match.'
    return
  }

  resetLoading.value = true
  try {
    await api.put(`/api/users/${selectedUser.value.id}`, {
      password: newPassword.value
    })
    resetSuccess.value = `Password for ${selectedUser.value.name} has been reset successfully.`
    setTimeout(() => {
      closeResetModal()
    }, 2000)
  } catch (err) {
    // The error object carries the reset request body; log only the HTTP status.
    console.error('Password reset request failed.', err?.response?.status ?? 'no response')
    const body = err?.response?.data
    resetError.value =
      (typeof body === 'string' && body.trim()) ||
      body?.error ||
      'Failed to reset password. Please try again.'
  } finally {
    resetLoading.value = false
  }
}

onMounted(loadUsers)
</script>

<style scoped>
.page {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

/* The single page action. `margin-left: auto` keeps it on the right even when the
   header wraps. */
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

/* Layout only; the action colours come from assets/actions.css. */
.create-btn {
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

/* On a narrow screen the title and the action stack, so neither is squeezed. */
@media (max-width: 640px) {
  .header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    margin-left: 0;
  }
}

.toolbar {
  margin-bottom: 16px;
}

.search-input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #2452ff;
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
  border-radius: 8px;
  cursor: pointer;
}

/* The table keeps its own horizontal scroll so the page container never overflows. */
.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 640px;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

th, td {
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

.role-list {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

/* A role the account no longer holds: visible for the record, visually inactive. */
.role-revoked {
  background: #f1f5f9;
  color: #64748b;
  border: 1px dashed #cbd5e1;
}

.dean-status-active {
  background: #e8f7ee;
  color: #16794a;
}

.dean-status-revoked {
  background: #fdf0ef;
  color: #b42318;
}

.role-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}

.role-proponent { background: #dbeafe; color: #1e40af; }
.role-admin { background: #fce7f3; color: #9d174d; }
.role-dean { background: #cffafe; color: #155e75; }
.role-staff { background: #e0e7ff; color: #3730a3; }
.role-ovcrige { background: #d1fae5; color: #065f46; }
.role-ovcaf { background: #fef3c7; color: #92400e; }
.role-rec { background: #ede9fe; color: #6d28d9; }
.role-oc { background: #fee2e2; color: #991b1b; }

.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
}


.actions-cell {
  white-space: nowrap;
}

/* Layout only; the action colours come from assets/actions.css. */
.reset-btn {
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

/* The Dean row action: the same size as Reset Password. */
.dean-manage-btn {
  margin-left: 8px;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  background: white;
  padding: 28px;
  border-radius: 12px;
  width: 420px;
  max-width: 90vw;
}

.modal-box h3 {
  font-size: 20px;
  color: #1f2937;
  margin-bottom: 16px;
}

.user-info {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 16px;
}

.user-info p {
  margin: 4px 0;
  font-size: 14px;
  color: #475569;
}

.form-group {
  margin-bottom: 14px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.form-input:focus {
  border-color: #2452ff;
}

.success-text {
  color: #16a34a;
  font-size: 14px;
  margin-bottom: 8px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

/* Layout only; the action colours come from assets/actions.css. */
.cancel-btn {
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.confirm-btn {
  padding: 10px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>