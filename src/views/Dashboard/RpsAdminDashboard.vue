<template>
  <div class="dashboard-container animate-fade">
    <!-- Dashboard Header -->
    <div class="dashboard-header">
      <div class="header-text">
        <h2>RPS Admin Overview</h2>
        <p>System administration & account management panel.</p>
      </div>
      <button class="refresh-btn" @click="loadDashboardData" :disabled="isLoading">
        <svg class="refresh-icon" :class="{ spinning: isLoading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>{{ isLoading ? 'Refreshing...' : 'Refresh Data' }}</span>
      </button>
    </div>

    <!-- User Stats Row -->
    <h3 class="section-title">User Accounts</h3>
    <div class="stats-row">
      <div class="stat-card blue-accent">
        <div class="stat-meta">
          <span class="stat-label">Total Users</span>
          <h3 class="stat-value">{{ totalUsers }}</h3>
        </div>
        <div class="stat-icon-wrapper blue">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/>
          </svg>
        </div>
      </div>

      <div class="stat-card yellow-accent">
        <div class="stat-meta">
          <span class="stat-label">Pending Approvals</span>
          <h3 class="stat-value">{{ pendingUsers }}</h3>
        </div>
        <div class="stat-icon-wrapper yellow">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
      </div>

      <div class="stat-card green-accent">
        <div class="stat-meta">
          <span class="stat-label">Active Users</span>
          <h3 class="stat-value">{{ approvedUsers }}</h3>
        </div>
        <div class="stat-icon-wrapper green">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
          </svg>
        </div>
      </div>

      <div class="stat-card red-accent">
        <div class="stat-meta">
          <span class="stat-label">Rejected Users</span>
          <h3 class="stat-value">{{ rejectedUsers }}</h3>
        </div>
        <div class="stat-icon-wrapper red">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- Pending Approvals: one compact card - header with the live pending count and the
         link to the full list, then the account rows with their two decisions. -->
    <section class="pending-panel" aria-labelledby="pending-approvals-title">
      <header class="pending-header">
        <div class="pending-heading">
          <h3 id="pending-approvals-title" class="pending-title">Pending Approvals</h3>
          <span
            class="pending-count"
            :aria-label="`${pendingUsers} pending ${pendingUsers === 1 ? 'account' : 'accounts'}`"
          >{{ pendingUsers }}</span>
        </div>

        <router-link to="/user-accounts" class="view-all-btn">
          <span>View All</span>
          <svg
            class="view-all-icon"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            aria-hidden="true"
          >
            <line x1="5" y1="12" x2="19" y2="12" />
            <polyline points="12 5 19 12 12 19" />
          </svg>
        </router-link>
      </header>

      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>Loading user accounts...</p>
      </div>

      <div v-else-if="pendingUsersList.length > 0" class="pending-table-scroll">
        <table class="pending-table">
          <colgroup>
            <col class="col-applicant" />
            <col class="col-email" />
            <col class="col-role" />
            <col class="col-registered" />
            <col class="col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col">Applicant</th>
              <th scope="col">Email Address</th>
              <th scope="col">Role</th>
              <th scope="col">Registered</th>
              <th scope="col" class="actions-heading">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in pendingUsersList" :key="user.id">
              <td>
                <div class="applicant">
                  <span class="avatar" aria-hidden="true">{{ initialsOf(user.name) }}</span>
                  <span class="applicant-name">{{ user.name || 'Unknown' }}</span>
                </div>
              </td>
              <td class="email-cell">{{ user.email || 'N/A' }}</td>
              <td>
                <span class="role-pill">{{ formatRole(user.role) }}</span>
              </td>
              <td class="date-cell">{{ formatDate(user.dateRegistered) }}</td>
              <td class="actions-cell">
                <div class="action-btns">
                  <button
                    type="button"
                    class="approve-btn"
                    :disabled="actionLoading"
                    :aria-busy="isActing(user.id, 'approve')"
                    @click="approveUser(user)"
                  >
                    <span
                      v-if="isActing(user.id, 'approve')"
                      class="btn-spinner"
                      aria-hidden="true"
                    ></span>
                    <svg
                      v-else
                      class="btn-icon"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <polyline points="20 6 9 17 4 12" />
                    </svg>
                    <span>Approve</span>
                  </button>

                  <button
                    type="button"
                    class="reject-btn"
                    :disabled="actionLoading"
                    :aria-busy="isActing(user.id, 'reject')"
                    @click="rejectUser(user)"
                  >
                    <span
                      v-if="isActing(user.id, 'reject')"
                      class="btn-spinner"
                      aria-hidden="true"
                    ></span>
                    <svg
                      v-else
                      class="btn-icon"
                      viewBox="0 0 24 24"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="2.4"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                      aria-hidden="true"
                    >
                      <line x1="18" y1="6" x2="6" y2="18" />
                      <line x1="6" y1="6" x2="18" y2="18" />
                    </svg>
                    <span>Reject</span>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="empty-state">
        <h3>No Pending Approvals</h3>
        <p>All user accounts have been reviewed.</p>
      </div>
    </section>

    <NotificationPreview
      title="Notifications"
      :limit="5"
      view-all-to="/notifications"
      empty-text="No notifications."
    />

    <!-- Success Toast -->
    <div v-if="showToast" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import NotificationPreview from '@/components/NotificationPreview.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showConfirm } = useDialog()

const users = ref([])
const isLoading = ref(true)
const actionLoading = ref(false)
// Which row and which decision is in flight, so only that button shows a spinner while
// both decisions stay disabled everywhere.
const actingId = ref(null)
const actingAction = ref('')
const showToast = ref(false)
const toastMessage = ref('')
const toastType = ref('success')

// User stats
const totalUsers = computed(() => users.value.length)
const pendingUsers = computed(() => users.value.filter(u => u.status === 'PENDING').length)
const approvedUsers = computed(() => users.value.filter(u => u.status === 'APPROVED').length)
const rejectedUsers = computed(() => users.value.filter(u => u.status === 'REJECTED').length)
const pendingUsersList = computed(() => users.value.filter(u => u.status === 'PENDING').slice(0, 5))

const formatRole = (role) => {
  const map = { PROPONENT: 'Proponent', DEAN: 'Dean', RPS_ADMIN: 'RPS Admin', RPS_STAFF: 'RPS Staff', OVCRIGE: 'OVCRIGE', OVCAF: 'OVCAF', REC: 'REC', OC: 'Chancellor' }
  return map[role] || role || '—'
}

/**
 * The avatar initials, taken from the account's real name: first letter of the first and
 * the last word ("Juan Dela Cruz" -> "JC"). A single-word name uses its first two letters.
 */
const initialsOf = (name) => {
  const parts = String(name || '')
    .trim()
    .split(/\s+/)
    .filter(Boolean)
  if (parts.length === 0) return '?'
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase()
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
}

/** True while this row's decision is the one in flight. */
const isActing = (id, action) => actingId.value === id && actingAction.value === action

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

const showNotification = (message, type = 'success') => {
  toastMessage.value = message
  toastType.value = type
  showToast.value = true
  setTimeout(() => { showToast.value = false }, 3000)
}

const approveUser = async (user) => {
  if (actionLoading.value) return
  const confirmed = await showConfirm(
    `Approve the account of ${user.name}? The account will be able to sign in with its assigned role.`,
    { title: 'Approve Account', type: 'warning', confirmText: 'Approve' },
  )
  if (!confirmed) return
  actionLoading.value = true
  actingId.value = user.id
  actingAction.value = 'approve'
  try {
    await api.put(`/api/users/${user.id}/approve`)
    user.status = 'APPROVED'
    showNotification(`${user.name} has been approved.`)
  } catch (err) {
    console.error('Failed to approve user:', err)
    showNotification('Failed to approve user.', 'error')
  } finally {
    actionLoading.value = false
    actingId.value = null
    actingAction.value = ''
  }
}

const rejectUser = async (user) => {
  if (actionLoading.value) return
  const confirmed = await showConfirm(
    `Reject the account of ${user.name}? The account will not be able to sign in.`,
    { title: 'Reject Account', type: 'danger', confirmText: 'Reject Account' },
  )
  if (!confirmed) return
  actionLoading.value = true
  actingId.value = user.id
  actingAction.value = 'reject'
  try {
    await api.put(`/api/users/${user.id}/reject`)
    user.status = 'REJECTED'
    showNotification(`${user.name} has been rejected.`, 'error')
  } catch (err) {
    console.error('Failed to reject user:', err)
    showNotification('Failed to reject user.', 'error')
  } finally {
    actionLoading.value = false
    actingId.value = null
    actingAction.value = ''
  }
}

const loadDashboardData = async () => {
  try {
    isLoading.value = true
    // This dashboard shows the account queue only. The two sections that read
    // /api/proposals were removed, so the request is not made any more; the endpoint and
    // every other page that uses it are unchanged.
    const usersRes = await api.get('/api/users')
    users.value = (Array.isArray(usersRes.data) ? usersRes.data : []).map(u => ({
      ...u,
      status: (u.status || 'APPROVED').toUpperCase()
    }))
  } catch (error) {
    console.error('Failed to load dashboard data:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadDashboardData)
</script>

<style scoped>
.dashboard-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* Header */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 18px;
}

.header-text h2 {
  font-size: var(--ui-font-page-title);
  font-weight: var(--ui-weight-bold);
  color: #0f172a;
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: var(--ui-font-body);
  color: var(--ui-text-muted);
  margin: 0;
}

.refresh-btn {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: var(--ui-control-font-md);
  font-weight: var(--ui-weight-semibold);
  color: #475569;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #f8fafc;
}

.refresh-icon { width: 14px; height: 14px; }
.spinning { animation: spin 1s linear infinite; }

/* Section Title */
.section-title {
  font-size: var(--ui-font-section-title);
  font-weight: var(--ui-weight-semibold);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
  color: var(--ui-text-muted);
  margin: 0;
}

/* Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s ease;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.04);
}

.blue-accent { border-left: 4px solid #3b82f6; }
.green-accent { border-left: 4px solid #10b981; }
.yellow-accent { border-left: 4px solid #f59e0b; }
.red-accent { border-left: 4px solid #ef4444; }

.stat-meta { text-align: left; }

.stat-label {
  font-size: var(--ui-font-label);
  font-weight: var(--ui-weight-semibold);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
  color: var(--ui-text-muted);
  display: block;
  margin-bottom: 4px;
}

.stat-value {
  font-size: var(--ui-font-stat);
  font-weight: var(--ui-weight-bold);
  color: #0f172a;
  margin: 0;
  line-height: var(--ui-line-display);
}

.stat-icon-wrapper {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-svg { width: 22px; height: 22px; }

.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.green { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.yellow { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

/* ===== Pending Approvals =====
   One compact card: header (title, live count, View All) -> table -> account rows.
   Flat surface, thin blue-grey border, no heavy shadow. */
.pending-panel {
  background: #ffffff;
  border: 1px solid #dce5f0;
  border-radius: 14px;
  overflow: hidden;
}

/* Header */
.pending-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid #e8edf4;
}

.pending-heading {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.pending-title {
  margin: 0;
  font-size: var(--ui-font-body);
  font-weight: var(--ui-weight-semibold);
  color: #17243a;
  line-height: var(--ui-line-display);
  white-space: nowrap;
}

/* The live number of pending accounts, never a fixed value. */
.pending-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  padding: 2px 7px;
  border-radius: 11px;
  background: #fff2c7;
  color: #7a5200;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-tight);
}

/* A text link, not a button: the existing User Accounts route. */
.view-all-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 0;
  border: none;
  background: transparent;
  color: #1d4ed8;
  font-family: inherit;
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.15s ease;
}

.view-all-btn:hover {
  color: #1e40af;
  text-decoration: underline;
}

.view-all-btn:focus-visible {
  outline: 2px solid #1d4ed8;
  outline-offset: 2px;
  border-radius: 4px;
}

.view-all-icon {
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
}

/* Table: the card clips; on a narrow screen this inner box scrolls sideways. */
.pending-table-scroll {
  overflow-x: auto;
}

.pending-table {
  width: 100%;
  min-width: 900px;
  border-collapse: collapse;
  table-layout: fixed;
}

.col-applicant { width: 24%; }
.col-email { width: 27%; }
.col-role { width: 12%; }
.col-registered { width: 16%; }
.col-actions { width: 21%; }

.pending-table th {
  padding: 11px 16px;
  background: #fbfcfe;
  border-bottom: 1px solid #eff3f9;
  color: var(--ui-text-soft);
  font-size: var(--ui-font-table-header);
  font-weight: var(--ui-weight-semibold);
  letter-spacing: var(--ui-tracking-caps);
  text-transform: uppercase;
  text-align: left;
  white-space: nowrap;
}

.pending-table td {
  padding: 12px 16px;
  color: #1e293b;
  font-size: var(--ui-font-table-cell-compact);
  text-align: left;
  vertical-align: middle;
}

/* A hairline only between rows, never under the last one. */
.pending-table tbody tr:not(:last-child) td {
  border-bottom: 1px solid #f1f5f9;
}

.actions-heading,
.actions-cell {
  text-align: right;
}

/* Scoped selectors need the table element too: `.pending-table th/td` is more specific
   than a bare `.actions-heading`, and would otherwise force these cells back to left. */
.pending-table .actions-heading {
  padding-right: 18px;
  text-align: right;
}

.pending-table .actions-cell {
  padding-right: 18px;
  padding-left: 8px;
  text-align: right;
}

/* Applicant: initial avatar + real name */
.applicant {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  flex: 0 0 27px;
  border-radius: 50%;
  background: #eff6ff;
  border: 1px solid #d7e7ff;
  color: #1d4ed8;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-none);
}

.applicant-name {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #1e293b;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-medium);
}

/* `.pending-table td` is more specific than a bare cell class, so the table is repeated
   here to keep the muted metadata colour. */
.pending-table .email-cell,
.pending-table .date-cell {
  overflow: hidden;
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Role: one compact blue pill, whatever the role. */
.role-pill {
  display: inline-flex;
  align-items: center;
  padding: 4px 9px;
  border: 1px solid #d7e7ff;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-display);
  white-space: nowrap;
}

/* Actions: equal Approve / Reject, side by side, right-aligned */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 9px;
  flex-wrap: nowrap;
}

/* One box for both: the same width, height, radius, font and spacing, so neither label
   can make one button larger. */
.approve-btn,
.reject-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  box-sizing: border-box;
  width: 75px;
  height: 28px;
  /* RecRejectProposal.vue ships an unscoped reject-button margin-top rule, which would
     otherwise drop Reject below Approve. The pair is aligned here explicitly. */
  margin: 0;
  padding: 0 6px;
  border-radius: 9px;
  font-family: inherit;
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
  line-height: var(--ui-line-none);
  white-space: nowrap;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

/* Filled green: the positive decision. */
.approve-btn {
  background: #047857;
  border: 1px solid #059669;
  color: #ffffff;
}

.approve-btn:hover:not(:disabled) {
  background: #047857;
  border-color: #047857;
}

/* Outlined red: the negative decision, visually lighter than Approve. */
.reject-btn {
  background: #ffffff;
  border: 1px solid #fecaca;
  color: #dc2626;
}

.reject-btn:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #fca5a5;
}

.approve-btn:focus-visible,
.reject-btn:focus-visible {
  outline: 2px solid #1d4ed8;
  outline-offset: 2px;
}

.approve-btn:disabled,
.reject-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-icon {
  width: 12px;
  height: 12px;
  flex: 0 0 auto;
}

/* The in-flight indicator replaces the icon, so the box never changes size. */
.btn-spinner {
  width: 11px;
  height: 11px;
  flex: 0 0 auto;
  border: 1.5px solid rgba(255, 255, 255, 0.45);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

.reject-btn .btn-spinner {
  border-color: rgba(220, 38, 38, 0.25);
  border-top-color: #dc2626;
}

/* Loading and empty states stay compact inside the card. */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 18px;
}

.loading-spinner {
  width: 22px;
  height: 22px;
  border: 2.5px solid rgba(36, 82, 255, 0.2);
  border-top-color: #2452ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-state p {
  margin: 0;
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
}

.empty-state {
  padding: 28px 18px;
  text-align: center;
}

.empty-state h3 {
  margin: 0 0 4px;
  color: #17243a;
  font-size: var(--ui-font-card-title);
  font-weight: var(--ui-weight-semibold);
}

.empty-state p {
  margin: 0;
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
}

/* The 12px header labels need a little less gutter before the table outgrows a
   tablet-width content area; the desktop gutters stay as designed. */
@media (max-width: 1200px) {
  .pending-table {
    min-width: 820px;
  }

  .pending-table th,
  .pending-table td {
    padding-left: 10px;
    padding-right: 10px;
  }

  /* Keep the whole institutional address visible on a tablet: two lines beat a
     truncated domain. */
  .pending-table .email-cell {
    white-space: normal;
    overflow-wrap: anywhere;
  }
}

@media (max-width: 640px) {
  .pending-header {
    padding: 12px 14px;
  }

  .pending-table th,
  .pending-table td {
    padding-left: 12px;
    padding-right: 12px;
  }

  .actions-heading,
  .actions-cell {
    padding-right: 14px;
  }
}

/* Toast */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: var(--ui-font-body);
  font-weight: var(--ui-weight-semibold);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.toast.success { background: #10b981; color: white; }
.toast.error { background: #ef4444; color: white; }

/* Animations */
.animate-fade { animation: fadeIn 0.3s ease; }

@keyframes spin { to { transform: rotate(360deg); } }
@keyframes fadeIn { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: translateY(0); } }
@keyframes slideIn { from { opacity: 0; transform: translateX(20px); } to { opacity: 1; transform: translateX(0); } }

@media (max-width: 900px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 540px) {
  .stats-row { grid-template-columns: 1fr; }
  .dashboard-header { flex-direction: column; align-items: flex-start; gap: 14px; }
}
</style>