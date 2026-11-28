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

    <!-- Status filter and search share one toolbar: both narrow the same loaded list,
         so the page counter below always describes what the two selected. The order and
         the styling are the ones Dean Role Management already uses. -->
    <div class="toolbar">
      <div class="filters" role="group" aria-label="Filter accounts by status">
        <button
          type="button"
          class="filter-pill"
          :class="{ active: statusFilter === '' }"
          :aria-pressed="statusFilter === ''"
          @click="statusFilter = ''"
        >
          <span>All Accounts</span>
          <span class="filter-count">{{ users.length }}</span>
        </button>
        <button
          v-for="option in statusOptions"
          :key="option.value"
          type="button"
          class="filter-pill"
          :class="{ active: statusFilter === option.value }"
          :aria-pressed="statusFilter === option.value"
          @click="statusFilter = option.value"
        >
          <span>{{ option.label }}</span>
          <span class="filter-count">{{ option.count }}</span>
        </button>
      </div>

      <input
        v-model="search"
        type="search"
        class="search-input"
        placeholder="Search by name, email, or role..."
        aria-label="Search accounts by name, email, or role"
      />
    </div>

    <!-- Result of the last pending-account decision, kept on the page after the modal
         closes so the administrator sees what happened to the request. -->
    <div
      v-if="actionNotice.message"
      class="notice"
      :class="`notice--${actionNotice.type}`"
      role="status"
    >
      {{ actionNotice.message }}
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
          <tr v-for="user in visibleUsers" :key="user.id">
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
              <!-- A pending registration is reviewed here: the Actions button opens the
                   request so the administrator can read it and Approve or Reject it. -->
              <button
                v-if="isPending(user)"
                class="review-btn btn-primary"
                @click="openPendingModal(user)"
              >
                Actions
              </button>
              <template v-else>
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
              </template>
            </td>
          </tr>
          <tr v-if="filteredUsers.length === 0">
            <td colspan="5" class="empty">{{ emptyMessage }}</td>
          </tr>
        </tbody>
      </table>
      <PaginationBar
        v-if="filteredUsers.length"
        :page="page"
        :page-size="pageSize"
        :total="filteredUsers.length"
        item-label="account"
        item-label-plural="accounts"
        @update:page="setPage"
        @update:page-size="setPageSize"
      />
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
    <div
      v-if="showResetModal"
      class="adm-overlay"
      @click.self="closeResetModal"
      @keydown.esc="closeResetModal"
    >
      <div
        ref="resetModal"
        class="adm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reset-password-title"
        tabindex="-1"
        @keydown="onResetKeydown"
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

        <form class="adm-form" @submit.prevent="resetPassword">
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
              type="submit"
              class="adm-btn adm-btn--primary"
              :disabled="resetLoading || !resetPasswordValid"
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
        </form>
      </div>
    </div>

    <!-- Review a pending account. The administrator reads the request here and decides:
         Approve lets the account sign in with its assigned role, Reject closes it. -->
    <div
      v-if="showPendingModal"
      class="adm-overlay"
      @click.self="closePendingModal"
      @keydown.esc="closePendingModal"
    >
      <div
        ref="pendingModal"
        class="adm-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-account-title"
        tabindex="-1"
        @keydown="onPendingKeydown"
      >
        <header class="adm-header">
          <span class="adm-header-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.9"
              stroke-linecap="round" stroke-linejoin="round">
              <path d="M9 12l2 2 4-4" />
              <circle cx="12" cy="12" r="9" />
            </svg>
          </span>
          <div class="adm-heading">
            <h2 id="review-account-title">Review account request</h2>
            <p>Approve to let this account sign in with its assigned role, or reject it.</p>
          </div>
          <button
            type="button"
            class="adm-close"
            aria-label="Close review account modal"
            :disabled="pendingAction !== ''"
            @click="closePendingModal"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <div class="adm-body">
          <p v-if="pendingError" class="adm-error" role="alert">{{ pendingError }}</p>

          <section class="adm-panel" aria-labelledby="review-account-details-title">
            <h3 id="review-account-details-title" class="adm-panel-title">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M20 21a8 8 0 0 0-16 0" />
                <circle cx="12" cy="7" r="4" />
              </svg>
              <span>Requested account</span>
            </h3>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Full name</span>
              <span class="adm-credential-value">{{ pendingUser?.name || '—' }}</span>
            </div>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Email</span>
              <span class="adm-credential-value">{{ pendingUser?.email || '—' }}</span>
            </div>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Role</span>
              <span class="adm-credential-value">{{ pendingUser ? roleLabels(pendingUser) : '—' }}</span>
            </div>
            <div class="adm-credential-row">
              <span class="adm-credential-label">Requested on</span>
              <span class="adm-credential-value">{{ formatDate(pendingUser?.dateRegistered) }}</span>
            </div>
          </section>

          <p class="adm-hint">
            Approving grants the account the role shown above. Rejecting closes the
            registration, and the account cannot sign in.
          </p>
        </div>

        <footer class="adm-footer review-footer">
          <button
            type="button"
            class="adm-btn adm-btn--secondary"
            :disabled="pendingAction !== ''"
            @click="closePendingModal"
          >
            Cancel
          </button>
          <button
            type="button"
            class="adm-btn btn-reject"
            :disabled="pendingAction !== ''"
            :aria-busy="pendingAction === 'reject'"
            @click="rejectPending"
          >
            <span v-if="pendingAction === 'reject'" class="adm-spinner" aria-hidden="true"></span>
            <span>{{ pendingAction === 'reject' ? 'Rejecting…' : 'Reject' }}</span>
          </button>
          <button
            type="button"
            class="adm-btn btn-approve"
            :disabled="pendingAction !== ''"
            :aria-busy="pendingAction === 'approve'"
            @click="approvePending"
          >
            <span v-if="pendingAction === 'approve'" class="adm-spinner" aria-hidden="true"></span>
            <span>{{ pendingAction === 'approve' ? 'Approving…' : 'Approve' }}</span>
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, nextTick, onBeforeUnmount, onMounted, watch } from 'vue'
import '@/assets/adminModal.css'
import api from '@/utils/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { usePagination } from '@/composables/usePagination.js'
import { UserRole } from '@/stores/userData'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'
import PasswordInput from '@/components/PasswordInput.vue'
import PasswordRequirements from '@/components/PasswordRequirements.vue'
import PasswordFieldHint from '@/components/PasswordFieldHint.vue'
import ManageDeanFlow from '@/views/Admin/ManageDeanFlow.vue'
import { statusTone } from '@/utils/statusTone.js'
import { accountStatusLabel, deanAssignmentStatusLabel } from '@/utils/statusCatalog.js'
import {
  accountStatusFilterOptions,
  matchesAccountStatus,
} from '@/utils/accountStatusFilter.js'
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
// '' is the "All Accounts" option; any other value is one stored account status.
const statusFilter = ref('')

// Reset password state
const showResetModal = ref(false)
const selectedUser = ref(null)
const newPassword = ref('')
const confirmPassword = ref('')
const resetError = ref('')
const resetSuccess = ref('')
const resetLoading = ref(false)
const resetModal = ref(null)
let resetCloseTimer = null
let resetTrigger = null
let bodyOverflowBefore = ''

// The reset button waits for a password that satisfies every requirement and a
// matching confirmation; the server re-validates the same policy.
const resetPasswordValid = computed(() => passwordFormReady(newPassword.value, confirmPassword.value))

// Pending-account review. `pendingAction` is '' when idle, otherwise the decision in
// flight ('approve' / 'reject'), so the dialog cannot fire twice or be dismissed mid-request.
//
// Bind it as a boolean (`pendingAction !== ''`), never as the raw action string. A bound
// boolean attribute counts the EMPTY STRING as present (Vue's includeBooleanAttr('') is
// true, which mirrors HTML's disabled=""), so the raw binding left all three buttons
// disabled the moment the dialog opened - the reason Approve and Reject did nothing.
const showPendingModal = ref(false)
const pendingUser = ref(null)
const pendingAction = ref('')
const pendingError = ref('')
const pendingModal = ref(null)
const actionNotice = ref({ type: 'success', message: '' })
let pendingTrigger = null
let pendingBodyOverflowBefore = ''

const isPending = (user) => String(user?.status || '').toUpperCase() === 'PENDING'

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return String(value)
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

const openPendingModal = (user) => {
  pendingTrigger = document.activeElement
  pendingBodyOverflowBefore = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  pendingUser.value = user
  pendingAction.value = ''
  pendingError.value = ''
  showPendingModal.value = true
  nextTick(() => pendingModal.value?.querySelector('.btn-approve')?.focus())
}

/** Force is used by a completed decision: the request in flight is already done. */
const closePendingModal = (force = false) => {
  if (pendingAction.value && !force) return
  showPendingModal.value = false
  document.body.style.overflow = pendingBodyOverflowBefore
  pendingBodyOverflowBefore = ''
  if (pendingTrigger && typeof pendingTrigger.focus === 'function') {
    pendingTrigger.focus()
  }
  pendingTrigger = null
  pendingUser.value = null
  pendingError.value = ''
}

/** Keep Tab inside the review dialog while it is open. */
const onPendingKeydown = (event) => {
  if (event?.key !== 'Tab' || !pendingModal.value) return
  const focusable = [...pendingModal.value.querySelectorAll(
    'button:not([disabled]), input:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  )].filter((element) => element.offsetParent !== null)
  if (focusable.length === 0) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

const approvePending = async () => {
  if (!pendingUser.value || pendingAction.value) return
  const name = pendingUser.value.name
  pendingAction.value = 'approve'
  pendingError.value = ''
  try {
    await api.put(`/api/users/${pendingUser.value.id}/approve`)
    actionNotice.value = { type: 'success', message: `${name} has been approved.` }
    closePendingModal(true)
    await loadUsers()
  } catch (err) {
    console.error('Failed to approve user:', err?.response?.status ?? 'no response')
    pendingError.value = err?.response?.data?.error || 'Failed to approve the account.'
  } finally {
    pendingAction.value = ''
  }
}

const rejectPending = async () => {
  if (!pendingUser.value || pendingAction.value) return
  const name = pendingUser.value.name
  pendingAction.value = 'reject'
  pendingError.value = ''
  try {
    await api.put(`/api/users/${pendingUser.value.id}/reject`)
    actionNotice.value = { type: 'error', message: `${name} has been rejected.` }
    closePendingModal(true)
    await loadUsers()
  } catch (err) {
    console.error('Failed to reject user:', err?.response?.status ?? 'no response')
    pendingError.value = err?.response?.data?.error || 'Failed to reject the account.'
  } finally {
    pendingAction.value = ''
  }
}

const filteredUsers = computed(() => {
  const keyword = search.value.toLowerCase().trim()
  return users.value.filter(u => {
    // The status filter and the search narrow the same list, so a row has to pass both.
    // An account whose stored status is unknown is still found by "All Accounts".
    if (!matchesAccountStatus(u, statusFilter.value)) return false
    if (!keyword) return true
    return (
      (u.name || '').toLowerCase().includes(keyword) ||
      (u.email || '').toLowerCase().includes(keyword) ||
      rolesOf(u).join(' ').toLowerCase().includes(keyword) ||
      revokedRolesOf(u).join(' ').toLowerCase().includes(keyword)
    )
  })
})

// The options (and their counts) come from the account statuses the backend actually
// stores, read from the loaded rows - never from a hardcoded list of display words.
const statusOptions = computed(() => accountStatusFilterOptions(users.value))

const emptyMessage = computed(() => {
  if (users.value.length === 0) return 'No users found.'
  if (search.value.trim() || statusFilter.value) {
    return 'No accounts match the selected search and status filter.'
  }
  return 'No users found.'
})

// Accounts page their own filtered list; the total is the real number of matching
// accounts, not the number of rows currently rendered.
const {
  page,
  pageSize,
  pageItems: visibleUsers,
  setPage,
  setPageSize,
  reset: resetPage,
} = usePagination(filteredUsers)

// A new search or a different status is a new result set: start at its first page.
watch([search, statusFilter], resetPage)

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
  if (resetCloseTimer !== null) {
    clearTimeout(resetCloseTimer)
    resetCloseTimer = null
  }
  resetTrigger = document.activeElement
  bodyOverflowBefore = document.body.style.overflow
  document.body.style.overflow = 'hidden'
  selectedUser.value = user
  newPassword.value = ''
  confirmPassword.value = ''
  resetError.value = ''
  resetSuccess.value = ''
  showResetModal.value = true
  nextTick(() => resetModal.value?.querySelector('#resetNewPassword')?.focus())
}

/** Keep Tab inside the reset dialog while it is open. */
const onResetKeydown = (event) => {
  if (event?.key !== 'Tab' || !resetModal.value) return
  const focusable = [...resetModal.value.querySelectorAll(
    'button:not([disabled]), input:not([disabled]), [href], [tabindex]:not([tabindex="-1"])',
  )].filter((element) => element.offsetParent !== null)
  if (focusable.length === 0) return
  const first = focusable[0]
  const last = focusable[focusable.length - 1]
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault()
    last.focus()
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault()
    first.focus()
  }
}

const closeResetModal = () => {
  // A reset in flight must not be dismissed: the request would still complete while the
  // administrator loses the success or failure feedback.
  if (resetLoading.value) return
  if (resetCloseTimer !== null) {
    clearTimeout(resetCloseTimer)
    resetCloseTimer = null
  }
  showResetModal.value = false
  document.body.style.overflow = bodyOverflowBefore
  if (resetTrigger && typeof resetTrigger.focus === 'function') {
    resetTrigger.focus()
  }
  resetTrigger = null
  selectedUser.value = null
  newPassword.value = ''
  confirmPassword.value = ''
  resetError.value = ''
  resetSuccess.value = ''
}

const resetPassword = async () => {
  // Enter submits the form; a second submit while the request is in flight is ignored.
  if (resetLoading.value) return
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
    resetCloseTimer = setTimeout(() => {
      resetCloseTimer = null
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

onBeforeUnmount(() => {
  if (resetCloseTimer !== null) {
    clearTimeout(resetCloseTimer)
    resetCloseTimer = null
  }
  if (showResetModal.value) {
    document.body.style.overflow = bodyOverflowBefore
  }
  if (showPendingModal.value) {
    document.body.style.overflow = pendingBodyOverflowBefore
  }
})
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
  font-size: var(--ui-font-page-title);
  color: #1f2937;
  font-weight: var(--ui-weight-bold);
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
  font-weight: var(--ui-weight-semibold);
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

/* Filter pills on the left, search on the right; both wrap on a narrow screen. */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* The same pill the Dean Role Management page uses, so the two admin lists read alike. */
.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #dfe3ee;
  border-radius: 999px;
  background: #ffffff;
  color: #3b3d55;
  font-family: inherit;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.filter-pill:hover:not(.active) {
  background: #f6f7fb;
  border-color: #cdd3e3;
}

.filter-pill.active {
  background: #262342;
  border-color: #262342;
  color: #ffffff;
}

.filter-pill:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

/* The count is derived from the loaded rows, never from a hardcoded number. */
.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #eef1f8;
  color: #4a5169;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-bold);
  line-height: var(--ui-line-none);
}

.filter-pill.active .filter-count {
  background: #ffd000;
  color: #262342;
}

.search-input {
  flex: 1 1 260px;
  min-width: 200px;
  max-width: 380px;
  padding: 10px 14px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: var(--ui-font-body);
  outline: none;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #2452ff;
}

/* On a narrow screen the pills and the search stack instead of being squeezed. */
@media (max-width: 640px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input {
    max-width: none;
  }
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: var(--ui-text-muted);
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
  font-size: var(--ui-font-table-header);
}

th {
  background: #f8fafc;
  color: #475569;
  font-weight: var(--ui-weight-semibold);
  text-transform: uppercase;
  font-size: var(--ui-font-table-header);
  letter-spacing: var(--ui-tracking-caps);
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
  color: var(--ui-text-muted);
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
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
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
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  display: inline-block;
}


.notice {
  margin-bottom: 16px;
  padding: 10px 14px;
  border-radius: 8px;
  border: 1px solid transparent;
  font-size: var(--ui-font-body);
  font-weight: var(--ui-weight-medium);
}

.notice--success {
  background: #e8f7ee;
  border-color: #b7e4c7;
  color: #16794a;
}

.notice--error {
  background: #fdf0ef;
  border-color: #f5c2c0;
  color: #b42318;
}

.actions-cell {
  white-space: nowrap;
}

/* The pending-account action: same geometry as Reset Password; colour from the
   shared action system (assets/actions.css). */
.review-btn {
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: var(--ui-control-font-md);
  font-weight: var(--ui-weight-semibold);
}

/* Three decisions do not fit the two-column footer of the other dialogs. */
.review-footer {
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1fr) minmax(0, 1fr);
}

/* Approve and Reject are the shared action buttons (assets/actions.css), so they carry
   their action colour from there; here they only have to fill their equal grid columns
   and keep the same 1px box as Cancel, whatever the label. */
.review-footer .adm-btn {
  width: 100%;
  white-space: nowrap;
}

.review-footer .btn-approve,
.review-footer .btn-reject {
  border-width: 1px;
  border-style: solid;
}

/* On a phone the three decisions get one row each for Cancel and a shared row for the
   decision pair, so "Approving…" is never squeezed into a third of the dialog. */
@media (max-width: 480px) {
  .review-footer {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }

  .review-footer .adm-btn--secondary {
    grid-column: 1 / -1;
  }
}

/* Layout only; the action colours come from assets/actions.css. */
.reset-btn {
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: var(--ui-control-font-md);
  font-weight: var(--ui-weight-semibold);
}

/* The Dean row action: the same size as Reset Password. */
.dean-manage-btn {
  margin-left: 8px;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: var(--ui-control-font-md);
  font-weight: var(--ui-weight-semibold);
}

</style>