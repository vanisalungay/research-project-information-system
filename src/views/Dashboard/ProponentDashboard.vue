<template>
  <div class="content-wrapper">
    <section class="welcome-section">
      <div class="welcome-left">
        <h1>
          Welcome,
          {{ user.first_name && user.last_name
              ? `${user.first_name} ${user.last_name}`
              : 'Proponent' }}!
        </h1>
        <p>Track your research proposals and manage submissions</p>

        <div class="stats-row">
          <div class="stat-box">
            <span class="stat-label">Active Proposals</span>
            <span class="stat-value">
              {{ statistics.activeProposals }}
            </span>
          </div>

          <div class="stat-box pending">
            <span class="stat-label">Pending Revisions</span>
            <span class="stat-value">
              {{ statistics.pendingRevisions }}
            </span>
          </div>

          <div class="stat-box approved">
            <span class="stat-label">Approved</span>
            <span class="stat-value">
              {{ statistics.approvedProposals }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- CREATE NEW PROPOSAL -->
    <div class="dashboard-actions">
      <button class="btn-new" @click="goToProposals">
        CREATE NEW PROPOSAL
      </button>
    </div>

    <!-- REVISION DEADLINE -->
    <section
      v-if="revisionDeadline"
      class="deadline-card"
    >
      <div class="deadline-left">
        <div class="deadline-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#000" stroke-width="2" />
            <path d="M12 6v6l4 2" stroke="#000" stroke-width="2" />
          </svg>
        </div>

        <div>
          <div class="deadline-title">
            Revision Deadlines
            <span class="pill">
              {{ revisionDeadline.pendingCount }} Pending
            </span>
          </div>

          <div class="proposal-name">
            {{ revisionDeadline.title }}
          </div>

          <div class="proposal-id">
            {{ revisionDeadline.proposalId }}
          </div>
        </div>
      </div>

      <div class="deadline-right">
        <strong class="deadline-days">
          {{ revisionDeadline.daysLeft }} days left
        </strong>

        <button
          class="submit-btn"
          @click="goToRevision"
        >
          Submit Revision
        </button>
      </div>
    </section>

    <!-- EMPTY STATE -->
    <section
      v-else
      class="deadline-card"
    >
      <div class="deadline-left">
        <div class="deadline-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#000" stroke-width="2" />
            <path d="M12 6v6l4 2" stroke="#000" stroke-width="2" />
          </svg>
        </div>

        <div>
          <div class="deadline-title">
            Revision Deadlines
          </div>

          <div class="proposal-name">
            No pending revisions.
          </div>

          <div class="proposal-id">
            You're all caught up!
          </div>
        </div>
      </div>
    </section>

    <!-- RECENTLY SUBMITTED -->
    <section class="grid proposal-panel">
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            Recent Proposals
          </div>

          <a
            @click="goToProposals"
            style="cursor: pointer"
          >
            View All
          </a>
        </div>

        <!-- No Proposals -->
        <div
          v-if="recentProposals.length === 0"
          class="panel-item"
        >
          <div>
            <div class="item-title">No submitted proposals yet.</div>
            <div class="item-meta">
              Your submitted proposals will appear here.
            </div>
          </div>
        </div>

        <!-- Proposal List -->
        <div
          v-for="proposal in recentProposals"
          :key="proposal.id"
          class="panel-item"
        >
          <div>
            <div class="item-title">
              {{ proposal.title }}
            </div>

            <div class="item-meta">
              {{ proposal.proposalId }} • {{ proposal.dateSubmitted }}
            </div>
          </div>

          <span
            class="status"
            :class="proposal.statusClass"
          >
            {{ proposal.status }}
          </span>
        </div>
      </div>
    </section>

    <!-- PROPOSAL MONITORING -->
    <section class="grid proposal-panel">
      <div class="panel">
        <div class="panel-header">
          <div class="panel-title">
            Proposal Monitoring
          </div>

          <a
            @click="goToMonitoring"
            style="cursor: pointer"
          >
            View All
          </a>
        </div>

        <!-- Empty State -->
        <div
          v-if="monitoringProposals.length === 0"
          class="panel-item"
        >
          <div>
            <div class="item-title">No proposals to monitor yet.</div>
            <div class="item-meta">Your proposal progress will appear here.</div>
          </div>
        </div>

        <!-- Proposal List -->
        <div
          v-for="proposal in monitoringProposals"
          :key="proposal.id"
          class="panel-item"
        >
          <div>
            <div class="item-title">{{ proposal.title }}</div>
            <div class="item-meta">{{ proposal.proposalId }} • {{ proposal.dateSubmitted }}</div>
          </div>

          <div class="monitoring-status">
            <span
              class="status"
              :class="proposal.statusClass"
            >
              {{ proposal.status }}
            </span>
            <span class="monitoring-progress">
              {{ proposal.progress }}% Complete
            </span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()

// Logged-in user
const user = ref({
  first_name: '',
  last_name: ''
})

// Dashboard statistics
const statistics = ref({
  activeProposals: 0,
  pendingRevisions: 0,
  approvedProposals: 0
})

// Revision deadline
const revisionDeadline = ref(null)

// Recently submitted proposals
const recentProposals = ref([])

// Proposal monitoring
const monitoringProposals = ref([])

// Statuses that mean a proposal has been returned to the proponent and needs revision.
const REVISION_STATUSES = ['RPS_RETURNED', 'REC_REVISION', 'REVISION']

// Statuses excluded from the Recent Proposals list (finalized + drafts).
const RECENT_EXCLUDED_STATUSES = ['REJECTED', 'REC_REJECTED', 'COMPLETED', 'APPROVED', 'DRAFT']

// Statuses excluded from Proposal Monitoring (drafts + rejected).
const MONITORING_EXCLUDED_STATUSES = ['DRAFT', 'REJECTED', 'REC_REJECTED']

// Mirror the Proposal Monitoring page's progress calculation.
function getProgressPercentage(p) {
  const status = p.status
  const terminalStatuses = ['REJECTED', 'REC_REJECTED', 'RPS_RETURNED', 'REC_REVISION', 'RETURNED']
  if (terminalStatuses.includes(status)) return 0
  if (status === 'RELEASED' || status === 'COMPLETED') return 100

  const stages = ['DRAFT', 'SUBMITTED', 'ENDORSED', 'UNDER_REVIEW', 'REC_APPROVED', 'FOR_OC_APPROVAL']
  if (!p || p.needsBudget !== false) stages.push('FOR_OVCAF_APPROVAL')
  stages.push('APPROVED')

  const index = stages.findIndex(s => s === status)
  if (index === -1) return 0
  return Math.round(((index + 1) / stages.length) * 100)
}

// Map a raw proposal into the shape used by the dashboard panels.
function mapProposalItem(p) {
  return {
    id: p.id,
    title: p.projectTitle || 'Untitled Research Project',
    proposalId: p.documentId || p.proposalCode || '—',
    dateSubmitted: p.createdAt ? new Date(p.createdAt).toLocaleDateString() : 'N/A',
    status: p.status,
    statusClass: (p.status || '').toLowerCase(),
    progress: getProgressPercentage(p)
  }
}

// Loading
const loading = ref(false)

// Compute whole days remaining until the revision deadline.
function computeDaysLeft(deadlineValue) {
  if (!deadlineValue) return 0
  const deadline = new Date(deadlineValue)
  if (isNaN(deadline.getTime())) return 0
  return Math.max(0, Math.floor((deadline.getTime() - Date.now()) / (1000 * 60 * 60 * 24)))
}

// Fetch dashboard data
async function fetchDashboard() {
  loading.value = true

  try {
    // Decode user session from localStorage
    const stored = localStorage.getItem('user_data')
    if (stored) {
      const currentUser = JSON.parse(atob(stored))
      const names = (currentUser.name || '').split(' ')
      user.value = {
        first_name: names[0] || 'User',
        last_name: names.slice(1).join(' ') || ''
      }

      // Fetch dynamic proposals list from API
      const response = await api.get('/api/proposals')
      const data = response.data || []

      // Filter to only display proposals owned by the current logged-in proponent
      const myProposals = data.filter(p => 
        (p.proponent && p.proponent.id === currentUser.id) ||
        (p.projectLeader && p.projectLeader.toLowerCase().includes((currentUser.name || '').toLowerCase()))
      )

      // Map stats
      statistics.value.activeProposals = myProposals.filter(p => 
        p.status === 'SUBMITTED' || p.status === 'ENDORSED' || p.status === 'UNDER_REVIEW' || p.status === 'REC_APPROVED'
      ).length

      statistics.value.pendingRevisions = myProposals.filter(p => 
        REVISION_STATUSES.includes(p.status)
      ).length

      statistics.value.approvedProposals = myProposals.filter(p => 
        p.status === 'APPROVED' || p.status === 'RELEASED'
      ).length

      // Sort owned proposals newest-first (same ordering as My Projects).
      const sortedProposals = myProposals
        .slice()
        .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))

      // Map recent proposals (newest first), excluding finalized/draft statuses.
      recentProposals.value = sortedProposals
        .filter(p => !RECENT_EXCLUDED_STATUSES.includes(p.status))
        .slice(0, 5)
        .map(mapProposalItem)

      // Proposal monitoring: exclude draft/rejected, then show first five (newest first).
      monitoringProposals.value = sortedProposals
        .filter(p => !MONITORING_EXCLUDED_STATUSES.includes(p.status))
        .slice(0, 5)
        .map(mapProposalItem)

      // Extract pending revisions that still have an active revision deadline for
      // the deadline banner. Proposals whose deadline has already passed, or whose
      // displayed day count has reached zero, are excluded so the banner always
      // highlights the next upcoming deadline.
      const nowTs = Date.now()
      const revisionItems = myProposals.filter(p => {
        if (!REVISION_STATUSES.includes(p.status)) return false
        const deadlineTime = p.revisionDeadline ? new Date(p.revisionDeadline).getTime() : NaN
        if (isNaN(deadlineTime) || deadlineTime <= nowTs) return false
        return computeDaysLeft(p.revisionDeadline) > 0
      })
      if (revisionItems.length > 0) {
        // Sort pending revisions by deadline (ascending) so the nearest/closest
        // upcoming deadline is displayed first. Missing/invalid deadlines sort last.
        const sortedRevisions = revisionItems
          .slice()
          .sort((a, b) => {
            const aTime = new Date(a.revisionDeadline).getTime()
            const bTime = new Date(b.revisionDeadline).getTime()
            const aValid = !isNaN(aTime)
            const bValid = !isNaN(bTime)
            if (!aValid && !bValid) return 0
            if (!aValid) return 1
            if (!bValid) return -1
            return aTime - bTime
          })

        const nearest = sortedRevisions[0]
        revisionDeadline.value = {
          pendingCount: revisionItems.length,
          title: nearest.projectTitle || 'Project Revision Required',
          proposalId: nearest.documentId || nearest.proposalCode || '—',
          daysLeft: computeDaysLeft(nearest.revisionDeadline)
        }
      } else {
        revisionDeadline.value = null
      }
    }
  } catch (error) {
    console.error("Failed to load proponent dashboard metrics:", error)
  } finally {
    loading.value = false
  }
}

// Utility helper to safely parse current session user
function parsedUser() {
  try {
    return JSON.parse(atob(localStorage.getItem('user_data')))
  } catch (e) {
    return { id: null }
  }
}

onMounted(() => {
  fetchDashboard()
})

function goToRevision() {
  router.push('/revisions')
}

function goToProposals() {
  router.push('/proposals')
}

function goToMonitoring() {
  router.push('/proponent-proposal-monitoring')
}

function getStatusClass(status) {
  switch (status) {
    case 'Under Review':
      return 'under-review'

    case 'Approved':
      return 'approved'

    case 'Revision Required':
      return 'revision'

    default:
      return ''
  }
}
</script>

<style scoped>
:root {
  --bg-color: #f8f8f8;
  --card-bg: #ffffff;
  --text-color: #1f1a3d;
  --muted-text: #6b7280;
  --primary: #2452ff;
  --border: #ddd;
}

/* DARK MODE OVERRIDE */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-color: #0f172a;
    --card-bg: #1e293b;
    --text-color: #f1f5f9;
    --muted-text: #94a3b8;
    --primary: #3b82f6;
    --border: #334155;
  }
}

/* BASE */
.content-wrapper {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;

  padding: 2rem;
  background: #f3f4f6;
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
}

.welcome-section,
.deadline-card,
.grid {
  width: 100%;
}

/* WELCOME */
.welcome-section {
  background: linear-gradient(90deg, #2b294a 0%, #3a375f 100%);
  border-radius: 0.875rem;
  padding: 2rem;
  color: #fff;
}

.welcome-left h1 {
  margin: 0;
  font-size: 1.375rem;
  font-weight: 600;
}

.welcome-left p {
  margin: 0.375rem 0 1rem;
  font-size: 0.875rem;
  opacity: 0.85;
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  justify-content: center;
  align-items: center;
}

.stat-box {
  background: rgba(255, 255, 255, 0.12);
  padding: 0.875rem 1.25rem;
  border-radius: 0.75rem;
  flex: 1 1 200px;
  min-width: 200px;
  text-align: center;
}

.stat-label {
  font-size: 0.8125rem;
  opacity: 0.9;
}

.stat-value {
  display: block;
  margin-top: 0.375rem;
  font-size: 1.375rem;
  font-weight: 700;
}

.stat-box.pending {
  background: rgba(255, 193, 7, 0.25);
  color: white;
}

.stat-box.approved {
  background: rgba(40, 167, 69, 0.25);
}

/* CREATE NEW PROPOSAL BUTTON */
.dashboard-actions {
  display: flex;
  justify-content: flex-end;
  margin: 1.25rem 0;
}

.btn-new {
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
  color: #1f2937;
  white-space: nowrap;
}

/* DEADLINE */
.deadline-card {
  background: #fffbe6;
  border: 1px solid #ffd200;
  border-radius: 0.875rem;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.deadline-left {
  display: flex;
  gap: 0.875rem;
  align-items: center;
  flex: 1 1 260px;
}

.deadline-icon {
  width: 2.25rem;
  height: 2.25rem;
  background: #fef3c7;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.deadline-icon svg {
  width: 55%;
  height: 55%;
}

.deadline-title {
  font-weight: 700;
  font-size: 1rem;
  display: inline-flex;
  align-items: center;
}

.pill {
  background: #fef3c7;
  color: #92400e;
  padding: 0.125rem 0.625rem;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: 600;
  margin-left: 0.5rem;
  white-space: nowrap;
}

.proposal-name {
  font-size: 0.9rem;
  font-weight: 600;
  margin-top: 0.375rem;
}

.proposal-id {
  font-size: 0.75rem;
  color: #6b7280;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  margin-top: 0.125rem;
}

.deadline-right {
  display: flex;
  align-items: center;
  gap: 0.875rem;
  flex-shrink: 0;
}

.deadline-days {
  font-size: 0.9rem;
  font-weight: 600;
  color: #b45309;
  white-space: nowrap;
}

.submit-btn {
  background: #ffd200;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 0.75rem;
  font-weight: 600;
  color: #1f2937;
  cursor: pointer;
  white-space: nowrap;
}

@media (max-width: 560px) {
  .deadline-right {
    width: 100%;
  }
}

/* GRID */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.25rem;
  margin-top: 1.375rem;
}

/* PANELS */
.panel {
  background: #fff;
  border-radius: 0.875rem;
  padding: 1rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.625rem;
}

.panel-header a {
  font-size: 0.75rem;
  color: #6b7280;
  cursor: pointer;
}

.panel-title {
  font-weight: 700;
  font-size: 1rem;
}

.count-badge {
  background: #e5e7eb;
  padding: 0.125rem 0.5rem;
  border-radius: 0.75rem;
  font-size: 0.75rem;
  margin-left: 0.375rem;
}

.count-badge.yellow {
  background: #ffd200;
}

/* ITEMS */
.panel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f1f1;
}

.panel-item:last-child {
  border-bottom: none;
}

.item-title {
  font-size: 0.875rem;
  font-weight: 500;
}

.item-meta {
  font-size: 0.75rem;
  color: #6b7280;
}

/* STATUS */
.status {
  font-size: 0.75rem;
  padding: 0.25rem 0.625rem;
  border-radius: 1rem;
}

.status.under-review {
  background: #e5e7eb;
  color: black;
  font-weight: lighter;
}

.status.approved {
  background: #d1fae5;
  color: black;
  font-weight: lighter;
}

.status.revision {
  background: #fef3c7;
  color: black;
  font-weight: lighter;
}

/* Proposal status badges - match My Projects */
.proposal-panel .status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.proposal-panel .status.draft {
  background: #e5e7eb;
  color: #374151;
}

.proposal-panel .status.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.proposal-panel .status.approved {
  background: #dcfce7;
  color: #166534;
}

.proposal-panel .status.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.proposal-panel .status.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.proposal-panel .status.pending {
  background: #fef3c7;
  color: #92400e;
}

.monitoring-status {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.25rem;
}

.monitoring-progress {
  font-size: 0.7rem;
  color: #6b7280;
  font-weight: 500;
}

/* FEEDBACK */
.feedback-item {
  display: flex;
  gap: 0.625rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f1f1;
}

.feedback-item:last-child {
  border-bottom: none;
}

.dot {
  width: 0.625rem;
  height: 0.625rem;
  border-radius: 50%;
  margin-top: 0.375rem;
}

.dot.yellow {
  background: #ffd200;
}

.dot.green {
  background: #22c55e;
}
</style>
