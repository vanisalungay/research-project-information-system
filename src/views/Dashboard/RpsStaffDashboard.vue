<template>
  <div class="dashboard-container animate-fade">
    
    <!-- Dashboard Header -->
    <div class="dashboard-header">
      <div class="header-text">
        <h2>RPS Overview</h2>
        <p>Administrative and publication services research management panel.</p>
      </div>
      <button class="refresh-btn" @click="loadDashboardData" :disabled="isLoading">
        <svg class="refresh-icon" :class="{ spinning: isLoading }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <span>{{ isLoading ? 'Refreshing...' : 'Refresh Data' }}</span>
      </button>
    </div>

    <!-- Quick Stats Row Grid -->
    <div class="stats-row">
      
      <div class="stat-card blue-accent">
        <div class="stat-meta">
          <span class="stat-label">Total Proposals</span>
          <h3 class="stat-value">{{ totalProposals }}</h3>
        </div>
        <div class="stat-icon-wrapper blue">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
      </div>

      <div class="stat-card green-accent">
        <div class="stat-meta">
          <span class="stat-label">Endorsed Proposals</span>
          <h3 class="stat-value">{{ endorsedProposals }}</h3>
        </div>
        <div class="stat-icon-wrapper green">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      </div>

      <div class="stat-card yellow-accent">
        <div class="stat-meta">
          <span class="stat-label">Pending RPS Review</span>
          <h3 class="stat-value">{{ pendingReview }}</h3>
        </div>
        <div class="stat-icon-wrapper yellow">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
      </div>

      <div class="stat-card red-accent">
        <div class="stat-meta">
          <span class="stat-label">Failed / Returned</span>
          <h3 class="stat-value">{{ failedProposals }}</h3>
        </div>
        <div class="stat-icon-wrapper red">
          <svg class="stat-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 11-18 0 9 9 0 0118 0z" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>

    </div>

    <!-- Main Content Panel Split -->
    <div class="dashboard-body">
      
      <!-- Left Panel: Recent Database Activities -->
      <div class="main-panel">
        <div class="panel-header">
          <h3>RECENT SUBMISSIONS</h3>
          <span class="panel-badge">LIVE TRACKING</span>
        </div>

        <div v-if="isLoading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>Compiling research metrics...</p>
        </div>

        <div v-else-if="activities.length > 0" class="activity-list">
          <div
            v-for="item in activities"
            :key="item.id"
            :class="['activity-card', item.borderClass]"
          >
            <div class="activity-details">
              <div class="activity-text">
                <h4>{{ item.title }}</h4>
                <p>{{ item.description }}</p>
              </div>
              <span :class="['status-badge', item.badgeClass]">
                {{ item.status }}
              </span>
            </div>
            <div class="activity-footer">
              <button class="review-btn" @click="$router.push(`/proposal/${item.id}`)">
                Manage Details
              </button>
            </div>
          </div>

          <div class="action-row">
            <h3>Proposals Awaiting RPS Action</h3>
            <button class="view-all-btn" @click="$router.push('/rps-subproposal')">
              View All Submissions &rarr;
            </button>
          </div>

          <!-- Dynamic Awaiting Proposals List -->
          <div class="table-container">
            <table class="proposal-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Project Leader</th>
                  <th>Status</th>
                  <th class="text-right">Actions</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="prop in pendingProposalsList" :key="prop.id">
                  <td class="font-bold">{{ prop.projectTitle || "Untitled Project" }}</td>
                  <td>{{ prop.projectLeader || "Unknown" }}</td>
                  <td>
                    <span class="table-status-pill pending">Pending</span>
                  </td>
                  <td class="text-right">
                    <button class="table-review-btn" @click="$router.push(`/proposal/${prop.id}`)">
                      Review
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div v-else class="empty-state">
          <div class="empty-icon-wrapper">
            <svg class="empty-svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h3>No Active Submissions Yet</h3>
          <p>Research proposals, reviews, and publication metrics will appear here dynamically once faculty submitters compile drafts.</p>
        </div>
      </div>

      <!-- Right Panel: Side Action Blocks -->
      <div class="side-panel">
        
        <!-- Live Action Streams -->
        <div class="side-card">
          <h3 class="side-card-title">Live Notification Logs</h3>
          <p class="side-card-subtitle">Real-time alerts processed from active users.</p>

          <ul class="activity-timeline">
            <li v-for="(log, idx) in activityLogs" :key="idx">
              <span class="timeline-dot"></span>
              <div class="timeline-text">
                <span class="log-desc">{{ log.text }}</span>
                <span class="log-time">{{ log.time }}</span>
              </div>
            </li>
          </ul>

          <button class="side-action-btn border-top" @click="$router.push('/rpsstaff-notification')">
            View All Logs
          </button>
        </div>

        <!-- Quick Administration Hub -->
        <div class="side-card bg-highlight">
          <h3 class="side-card-title dark-text">Quick Actions</h3>
          <p class="side-card-subtitle dark-text">Speed dial administrative utilities.</p>
          
          <div class="quick-action-column">
            <button class="hub-btn primary" @click="$router.push('/rps-subproposal')">
              Review Submitted Proposals
            </button>
            <button class="hub-btn secondary" @click="$router.push('/rpsfunded')">
              Manage Funding Releases
            </button>
          </div>
        </div>

      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/utils/api'

const proposals = ref([])
const activities = ref([])
const totalProposals = ref(0)
const endorsedProposals = ref(0)
const pendingReview = ref(0)
const failedProposals = ref(0)
const isLoading = ref(true)

// Static activity fallback log data if no database activity exists
const activityLogs = ref([
  { text: 'Proposal "Community Development Program" submitted by leader.', time: '2 hours ago' },
  { text: 'Proposal "Healthcare Facility Upgrade" forwarded to review list.', time: '1 day ago' },
  { text: 'System seed initialized with temporary test accounts.', time: '3 days ago' }
])

// Compute list of dynamic pending proposals needing action
const pendingProposalsList = computed(() => {
  return proposals.value
    .filter(p => p.status === 'SUBMITTED' || p.status === 'PENDING')
    .slice(0, 3)
})

const loadDashboardData = async () => {
  try {
    isLoading.value = true
    const response = await api.get('/api/proposals')
    const data = response.data || []
    proposals.value = data

    // Calculate database counts dynamically
    totalProposals.value = data.length
    endorsedProposals.value = data.filter(p => p.status === 'ENDORSED' || p.status === 'APPROVED' || p.status === 'REC_APPROVED').length
    pendingReview.value = data.filter(p => p.status === 'SUBMITTED' || p.status === 'PENDING').length
    failedProposals.value = data.filter(p => p.status === 'REJECTED' || p.status === 'REVISION' || p.status === 'REC_REVISION' || p.status === 'RETURNED').length

    // Filter dynamic submissions list for top activity logs
    if (data.length > 0) {
      // Sort to show latest first
      const sorted = [...data].sort((a, b) => b.id - a.id)
      activities.value = sorted.slice(0, 4).map(p => {
        let statusText = 'Pending'
        let borderClass = 'review-border'
        let badgeClass = 'review-badge'
        
        if (p.status === 'APPROVED') {
          statusText = 'Approved'
          borderClass = 'pass-border'
          badgeClass = 'passed-badge'
        } else if (p.status === 'REJECTED' || p.status === 'RETURNED') {
          statusText = 'Returned'
          borderClass = 'fail-border'
          badgeClass = 'failed-badge'
        } else if (p.status === 'ENDORSED') {
          statusText = 'Endorsed'
          borderClass = 'pass-border'
          badgeClass = 'passed-badge'
        }

        return {
          id: p.id,
          title: p.projectTitle || "Untitled Research",
          description: "Leader: " + (p.projectLeader || "Anonymous"),
          status: statusText,
          borderClass,
          badgeClass
        }
      })
    }
  } catch (error) {
    console.error("Failed to compile dashboard metrics:", error)
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
  gap: 24px;
}

/* Header layout block */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--color-border, #e2e8f0);
  padding-bottom: 18px;
}

.header-text {
  text-align: left;
}

.header-text h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin: 0 0 4px 0;
}

.header-text p {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

.refresh-btn {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #cbd5e1);
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--color-text, #475569);
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  transition: all 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: var(--color-background-soft, #f8fafc);
  color: var(--color-heading, #0f172a);
}

.refresh-icon {
  width: 14px;
  height: 14px;
}

.spinning {
  animation: spin 1s linear infinite;
}

/* Quick Stats Row */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.stat-card {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 12px;
  padding: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.2s cubic-bezier(0.16, 1, 0.3, 1);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.04);
}

/* Left borders for visual accents */
.blue-accent { border-left: 4px solid #3b82f6; }
.green-accent { border-left: 4px solid #10b981; }
.yellow-accent { border-left: 4px solid #f59e0b; }
.red-accent { border-left: 4px solid #ef4444; }

.stat-meta {
  text-align: left;
}

.stat-label {
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--color-text-soft, #64748b);
  display: block;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
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

.stat-svg {
  width: 22px;
  height: 22px;
}

.blue { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.green { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.yellow { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
.red { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

/* Dashboard layout splitting panels */
.dashboard-body {
  display: grid;
  grid-template-columns: 2fr 1.2fr;
  gap: 20px;
}

.main-panel {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 14px;
  padding: 28px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  padding-bottom: 14px;
}

.panel-header h3 {
  font-size: 0.9375rem;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--color-heading, #1e293b);
  margin: 0;
}

.panel-badge {
  font-size: 0.6875rem;
  font-weight: 700;
  background: rgba(36, 82, 255, 0.1);
  color: #2452ff;
  padding: 4px 10px;
  border-radius: 20px;
}

/* Loading indicators */
.loading-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px 20px;
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
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

/* Submission cards list */
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  text-align: left;
}

.activity-card {
  background: var(--color-background-soft, #f8fafc);
  border-radius: 12px;
  padding: 16px 20px;
  border: 1px solid var(--color-border, #cbd5e1);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* Accent borders */
.pass-border { border-left: 4px solid #10b981; }
.review-border { border-left: 4px solid #3b82f6; }
.fail-border { border-left: 4px solid #ef4444; }

.activity-details {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.activity-text h4 {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-heading, #0f172a);
  margin: 0 0 4px 0;
  line-height: 1.4;
}

.activity-text p {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

.status-badge {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 12px;
  border-radius: 20px;
}

.passed-badge { background: rgba(16, 185, 129, 0.1); color: #10b981; }
.review-badge { background: rgba(59, 130, 246, 0.1); color: #3b82f6; }
.failed-badge { background: rgba(239, 68, 68, 0.1); color: #ef4444; }

.activity-footer {
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--color-border, rgba(0, 0, 0, 0.04));
  padding-top: 10px;
}

.review-btn {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #cbd5e1);
  color: var(--color-text, #475569);
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
}

.review-btn:hover {
  background: var(--color-background-soft, #f8fafc);
  color: var(--color-heading, #0f172a);
  border-color: #ffd803;
}

.action-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 10px 0;
  border-top: 1px solid var(--color-border, #f1f5f9);
  padding-top: 24px;
}

.action-row h3 {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-heading, #1e293b);
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

.view-all-btn:hover {
  text-decoration: underline;
}

/* Action Proposals table styling */
.table-container {
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 8px;
  overflow: hidden;
  margin-top: 10px;
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
  background: var(--color-background-soft, #f8fafc);
  color: var(--color-text-soft, #64748b);
  font-weight: 600;
  text-align: left;
  border-bottom: 1px solid var(--color-border, #cbd5e1);
}

.proposal-table td {
  border-bottom: 1px solid var(--color-border, #cbd5e1);
  color: var(--color-text, #1f2937);
  text-align: left;
}

.proposal-table tr:last-child td {
  border-bottom: none;
}

.font-bold {
  font-weight: 600;
}

.table-status-pill {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.table-status-pill.pending {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.text-right {
  text-align: right !important;
}

.table-review-btn {
  background: #2452ff;
  border: 1px solid #2452ff;
  color: white !important;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
}

.table-review-btn:hover {
  background: #1d40cc;
  border-color: #1d40cc;
}

/* Empty states */
.empty-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 60px 24px;
}

.empty-icon-wrapper {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: var(--color-background-soft, #f3f4f6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-soft, #64748b);
  margin-bottom: 18px;
}

.empty-svg {
  width: 32px;
  height: 32px;
}

.empty-state h3 {
  font-size: 1.125rem;
  font-weight: 600;
  color: var(--color-heading, #1f2937);
  margin: 0 0 6px 0;
}

.empty-state p {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #6b7280);
  max-width: 360px;
  line-height: 1.5;
  margin: 0;
}

/* Side panel container column */
.side-panel {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.side-card {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: 14px;
  padding: 24px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  text-align: left;
}

.bg-highlight {
  background: #ffd803 !important;
  border-color: #ffd803 !important;
}

.side-card-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin: 0 0 4px 0;
}

.side-card-title.dark-text {
  color: #000000 !important;
}

.side-card-subtitle {
  font-size: 0.75rem;
  color: var(--color-text-soft, #64748b);
  margin: 0 0 18px 0;
}

.side-card-subtitle.dark-text {
  color: rgba(0, 0, 0, 0.7) !important;
}

/* Timeline logs list */
.activity-timeline {
  list-style: none;
  padding: 0;
  margin: 0 0 20px 0;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.activity-timeline li {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.timeline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #2452ff;
  margin-top: 5px;
  flex-shrink: 0;
}

.timeline-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.log-desc {
  font-size: 0.8125rem;
  color: var(--color-text, #1f2937);
  line-height: 1.4;
}

.log-time {
  font-size: 0.6875rem;
  color: var(--color-text-soft, #64748b);
}

.side-action-btn {
  background: transparent;
  border: none;
  width: 100%;
  text-align: center;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #2452ff;
  cursor: pointer;
  padding-top: 14px;
}

.border-top {
  border-top: 1px solid var(--color-border, #f1f5f9);
}

.side-action-btn:hover {
  text-decoration: underline;
}

/* Quick Action Buttons column */
.quick-action-column {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.hub-btn {
  padding: 12px;
  border-radius: 8px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  width: 100%;
  border: none;
}

.hub-btn.primary {
  background: #000000 !important;
  color: #ffd803 !important;
}

.hub-btn.primary:hover {
  background: #1e293b !important;
}

.hub-btn.secondary {
  background: rgba(0, 0, 0, 0.08) !important;
  color: #000000 !important;
}

.hub-btn.secondary:hover {
  background: rgba(0, 0, 0, 0.14) !important;
}

/* Fade in effects */
.animate-fade {
  animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 900px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .dashboard-body {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 540px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .dashboard-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }
}
</style>
