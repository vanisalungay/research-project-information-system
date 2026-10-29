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
      <button class="retry-btn" @click="loadUsers">Retry</button>
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
              <span class="role-badge" :class="getRoleClass(user.role)">
                {{ formatRole(user.role) }}
              </span>
            </td>
            <td>
              <span class="status-badge" :class="getStatusClass(user.status)">
                {{ user.status || 'APPROVED' }}
              </span>
            </td>
            <td class="actions-cell">
              <button class="reset-btn" @click="openResetModal(user)">
                Reset Password
              </button>
              <!-- A Dean account keeps its Dean role management: the designation and the
                   college it endorses for, or revoking the role. The role itself is
                   granted when the account is created, through the standard Create
                   Account flow above. -->
              <button
                v-if="isDean(user)"
                class="dean-manage-btn"
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

    <!-- Dean role management for one existing Dean account, opened from that account's
         row. Dean accounts are created through the standard Create Account flow above,
         so this screen only edits the designation / college or revokes the role. -->
    <DeanAccountsModal
      v-if="showDeanModal"
      :dean-id="selectedDeanId"
      @close="closeDeanModal"
      @changed="loadUsers"
    />

    <!-- Reset Password Modal -->
    <div v-if="showResetModal" class="modal-overlay" @click.self="closeResetModal">
      <div class="modal-box">
        <h3>Reset Password</h3>

        <div class="user-info">
          <p><strong>User:</strong> {{ selectedUser?.name }}</p>
          <p><strong>Email:</strong> {{ selectedUser?.email }}</p>
          <p><strong>Role:</strong> {{ formatRole(selectedUser?.role) }}</p>
        </div>

        <div class="form-group">
          <label>New Password</label>
          <input
            v-model="newPassword"
            type="password"
            placeholder="Enter new temporary password"
            class="form-input"
          />
        </div>

        <div class="form-group">
          <label>Confirm Password</label>
          <input
            v-model="confirmPassword"
            type="password"
            placeholder="Confirm new password"
            class="form-input"
          />
        </div>

        <p v-if="resetError" class="error-text">{{ resetError }}</p>
        <p v-if="resetSuccess" class="success-text">{{ resetSuccess }}</p>

        <div class="modal-actions">
          <button class="cancel-btn" @click="closeResetModal">Cancel</button>
          <button
            class="confirm-btn"
            @click="resetPassword"
            :disabled="resetLoading"
          >
            {{ resetLoading ? 'Resetting...' : 'Reset Password' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import { UserRole } from '@/stores/userData'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'
import DeanAccountsModal from '@/views/Admin/DeanAccountsModal.vue'

const users = ref([])
const showModal = ref(false)
// The Dean role management screen is opened for one account at a time, from that
// account's row, so it needs the id of the Dean it was opened for.
const showDeanModal = ref(false)
const selectedDeanId = ref(null)
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

const filteredUsers = computed(() => {
  const keyword = search.value.toLowerCase().trim()
  if (!keyword) return users.value
  return users.value.filter(u =>
    (u.name || '').toLowerCase().includes(keyword) ||
    (u.email || '').toLowerCase().includes(keyword) ||
    (u.role || '').toLowerCase().includes(keyword)
  )
})

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

const getStatusClass = (status) => {
  if (status === 'APPROVED') return 'status-approved'
  if (status === 'PENDING') return 'status-pending'
  if (status === 'REJECTED') return 'status-rejected'
  return ''
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

/** True when the account holds the Dean role (the canonical `DEAN` value). */
const isDean = (user) => String(user?.role || '').toUpperCase() === UserRole.DEAN

/** Opens the Dean role management screen for the account of the clicked row. */
const openDeanModal = (user) => {
  selectedDeanId.value = user.id
  showDeanModal.value = true
}

const closeDeanModal = () => {
  showDeanModal.value = false
  selectedDeanId.value = null
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

  if (newPassword.value.length < 6) {
    resetError.value = 'Password must be at least 6 characters.'
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
    console.error('Failed to reset password:', err)
    resetError.value = 'Failed to reset password. Please try again.'
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
  background: #2563eb;
  color: white;
  border: none;
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

.status-approved { background: #dcfce7; color: #166534; }
.status-pending { background: #fef3c7; color: #92400e; }
.status-rejected { background: #fee2e2; color: #991b1b; }

.actions-cell {
  white-space: nowrap;
}

.reset-btn {
  padding: 6px 14px;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.reset-btn:hover {
  background: #d97706;
}

/* The Dean row action: a quiet secondary button, the same size as Reset Password. */
.dean-manage-btn {
  margin-left: 8px;
  padding: 6px 14px;
  background: #ffffff;
  color: #2452ff;
  border: 1px solid #2452ff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.dean-manage-btn:hover {
  background: #eff6ff;
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

.cancel-btn {
  padding: 10px 18px;
  background: #e2e8f0;
  color: #334155;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.cancel-btn:hover {
  background: #cbd5e1;
}

.confirm-btn {
  padding: 10px 18px;
  background: #f59e0b;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

.confirm-btn:hover {
  background: #d97706;
}

.confirm-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
</style>