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
          <span class="stat-label">Approved Users</span>
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

    <!-- Pending Approvals Table -->
    <div class="main-panel">
      <div class="panel-header">
        <h3>PENDING APPROVALS</h3>
        <button class="view-all-btn" @click="$router.push('/user-accounts')">View All &rarr;</button>
      </div>

      <div v-if="isLoading" class="loading-state">
        <div class="loading-spinner"></div>
        <p>Loading user accounts...</p>
      </div>

      <div v-else-if="pendingUsersList.length > 0" class="table-container">
        <table class="proposal-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
              <th>Registered</th>
              <th class="text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in pendingUsersList" :key="user.id">
              <td class="font-bold">{{ user.name || 'Unknown' }}</td>
              <td>{{ user.email || 'N/A' }}</td>
              <td>
                <span class="role-badge" :class="getRoleClass(user.role)">{{ formatRole(user.role) }}</span>
              </td>
              <td>{{ formatDate(user.dateRegistered) }}</td>
              <td class="text-right">
                <div class="action-btns">
                  <button class="approve-btn" @click="approveUser(user)" :disabled="actionLoading">✓ Approve</button>
                  <button class="reject-btn" @click="rejectUser(user)" :disabled="actionLoading">✕ Reject</button>
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
    </div>

    <!-- Success Toast -->
    <div v-if="showToast" class="toast" :class="toastType">
      {{ toastMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'

const users = ref([])
const isLoading = ref(true)
const actionLoading = ref(false)
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
  return map[role] || role
}

const getRoleClass = (role) => {
  const map = { PROPONENT: 'role-proponent', DEAN: 'role-dean', RPS_ADMIN: 'role-admin', RPS_STAFF: 'role-staff', OVCRIGE: 'role-ovcrige', OVCAF: 'role-ovcaf', REC: 'role-rec', OC: 'role-oc' }
  return map[role] || ''
}

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
  actionLoading.value = true
  try {
    await api.put(`/api/users/${user.id}/approve`)
    user.status = 'APPROVED'
    showNotification(`${user.name} has been approved.`)
  } catch (err) {
    console.error('Failed to approve user:', err)
    showNotification('Failed to approve user.', 'error')
  } finally {
    actionLoading.value = false
  }
}

const rejectUser = async (user) => {
  actionLoading.value = true
  try {
    await api.put(`/api/users/${user.id}/reject`)
    user.status = 'REJECTED'
    showNotification(`${user.name} has been rejected.`, 'error')
  } catch (err) {
    console.error('Failed to reject user:', err)
    showNotification('Failed to reject user.', 'error')
  } finally {
    actionLoading.value = false
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
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.refresh-btn {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
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
  font-size: 0.8125rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: #64748b;
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
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
  display: block;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.2;
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

/* Main Panel */
.main-panel {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 24px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}

.panel-header h3 {
  font-size: 0.875rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #1e293b;
  margin: 0;
}

.view-all-btn {
  background: transparent;
  border: none;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #2452ff;
  cursor: pointer;
  padding: 4px;
}

.view-all-btn:hover { text-decoration: underline; }

/* Loading */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 20px;
  gap: 12px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(36, 82, 255, 0.2);
  border-top-color: #2452ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.loading-state p {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

/* Table */
.table-container {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  overflow: hidden;
}

.proposal-table {
  width: 100%;
  border-collapse: collapse;
}

.proposal-table th,
.proposal-table td {
  padding: 12px 16px;
  font-size: 0.8125rem;
}

.proposal-table th {
  background: #f8fafc;
  color: #64748b;
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.proposal-table td {
  border-bottom: 1px solid #f1f5f9;
  color: #1f2937;
  text-align: left;
}

.proposal-table tr:last-child td { border-bottom: none; }
.font-bold { font-weight: 600; }
.text-right { text-align: right !important; }

/* Role Badges */
.role-badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
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

/* Action Buttons */
.action-btns {
  display: flex;
  gap: 6px;
  justify-content: flex-end;
}

.approve-btn {
  background: #10b981;
  color: white;
  border: none;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.approve-btn:hover:not(:disabled) { background: #059669; }
.approve-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.reject-btn {
  background: #ef4444;
  color: white;
  border: none;
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.reject-btn:hover:not(:disabled) { background: #dc2626; }
.reject-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* Empty State */
.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.empty-state h3 {
  font-size: 1rem;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 4px 0;
}

.empty-state p {
  font-size: 0.8125rem;
  color: #64748b;
  margin: 0;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 12px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
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