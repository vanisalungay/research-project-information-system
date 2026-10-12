<template>
  <div class="dashboard-container">
    <!-- DASHBOARD HEADER -->
    <div class="dashboard-header">
      <div class="header-text">
        <h2>RPS Overview</h2>
        <p>Administrative and publication services monitoring panel.</p>
      </div>
      <button class="refresh-btn" @click="refreshDashboard" :disabled="isRefreshing">
        <svg class="refresh-icon" :class="{ spinning: isRefreshing }" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67"></path>
        </svg>
        <span>{{ isRefreshing ? 'Refreshing...' : 'Refresh Data' }}</span>
      </button>
    </div>

    <!-- PROPOSAL ANNOUNCEMENTS -->
    <section class="announcements-section">
      <div class="announcements-panel">
        <div class="section-header">
          <div class="section-heading">
            <h3>Proposal announcements</h3>
            <p class="section-sub">Current proposal calls, submission periods and announcement details.</p>
          </div>
          <button class="section-link" @click="$router.push('/proposal-announcements')">
            <span>View all announcements</span>
            <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
          </button>
        </div>

        <div v-if="announcementsLoading" class="panel-state">Loading proposal announcements...</div>
        <div v-else-if="announcementsError" class="panel-state error-text">{{ announcementsError }}</div>
        <div v-else-if="announcements.length === 0" class="panel-state">No proposal announcements available at the moment.</div>
        <div v-else class="announcement-grid">
          <article
            v-for="ann in announcementCards"
            :key="ann.id"
            class="announcement-card"
            :class="`is-${ann.variant}`"
          >
            <div class="announcement-status-row">
              <span class="status-indicator">
                <span class="status-dot"></span>
                <span class="status-label">{{ ann.statusLabel }}</span>
              </span>
              <span class="status-badge">{{ ann.badge }}</span>
            </div>

            <h4 class="announcement-title">{{ ann.title }}</h4>

            <div class="announcement-meta" :class="{ 'meta-two': !ann.hasTimeLeft }">
              <div class="meta-group">
                <span class="meta-label">{{ ann.opensLabel }}</span>
                <span class="meta-value">{{ ann.opens }}</span>
              </div>
              <div class="meta-group">
                <span class="meta-label">{{ ann.deadlineLabel }}</span>
                <span class="meta-value">{{ ann.deadline }}</span>
              </div>
              <div v-if="ann.hasTimeLeft" class="meta-group">
                <span class="meta-label">Time left</span>
                <span class="meta-value">{{ ann.timeLeft }}</span>
              </div>
            </div>

            <div v-if="ann.progress !== null" class="announcement-progress">
              <div class="progress-track">
                <div class="progress-fill" :style="{ width: ann.progress + '%' }"></div>
              </div>
              <div class="progress-dates">
                <span>{{ ann.startShort }}</span>
                <span>{{ ann.endShort }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>
    </section>

    <!-- PANELS GRID -->
    <div class="panels-grid">
      <!-- PANEL A: AWAITING RPS ACTION -->
      <section class="panel">
        <div class="panel-header">
          <div class="panel-heading">
            <h3>Awaiting RPS Action</h3>
            <p class="panel-sub">Newest submissions to review</p>
          </div>
          <button class="panel-link" @click="$router.push('/rps-subproposal')">View all submissions &rarr;</button>
        </div>

        <div v-if="isLoading" class="panel-state">Loading submissions...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="awaitingAction.length === 0" class="panel-state">No submissions awaiting action.</div>
        <div v-else class="panel-table-wrap">
          <table class="panel-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Project Leader</th>
                <th>Status</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prop in awaitingAction" :key="prop.id">
                <td class="cell-title">{{ prop.projectTitle || 'Untitled Project' }}</td>
                <td>{{ prop.projectLeader || 'Unknown' }}</td>
                <td>
                  <span class="badge" :class="statusMeta(prop.status).cls">{{ statusMeta(prop.status).label }}</span>
                </td>
                <td class="text-right">
                  <button class="row-btn" @click="$router.push(`/proposal/${prop.id}`)">Review</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- PANEL B: REVISION REQUESTS -->
      <section class="panel">
        <div class="panel-header">
          <div class="panel-heading">
            <h3>Revision Requests</h3>
          </div>
          <button class="panel-link" @click="$router.push('/rps-revision-requests')">View all &rarr;</button>
        </div>

        <p class="panel-summary">
          <strong>{{ revisionRequests.length }}</strong>
          revision request{{ revisionRequests.length === 1 ? '' : 's' }}
        </p>

        <div v-if="isLoading" class="panel-state">Loading revision requests...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="revisionRequests.length === 0" class="panel-state">No revision requests found.</div>
        <ul v-else class="item-list">
          <li v-for="prop in revisionRequests.slice(0, 5)" :key="prop.id" class="item" :class="{ urgent: isOverdue(prop) }">
            <div class="item-main">
              <div class="item-title">{{ prop.projectTitle || 'Untitled' }}</div>
              <div class="item-meta">
                {{ prop.projectLeader || prop.proponent?.name || 'Unknown' }} &bull;
                {{ prop.documentId || prop.proposalCode || '—' }}
              </div>
              <div class="item-meta">{{ revDateLabel(prop) }}</div>
            </div>
            <span class="badge" :class="statusMeta(prop.status).cls">{{ statusMeta(prop.status).label }}</span>
          </li>
        </ul>
      </section>

      <!-- PANEL C: ENDORSED PROPOSALS -->
      <section class="panel">
        <div class="panel-header">
          <div class="panel-heading">
            <h3>Endorsed Proposals</h3>
          </div>
          <button class="panel-link" @click="$router.push('/rpsendorsed-prop')">View all &rarr;</button>
        </div>

        <p class="panel-summary">
          <strong>{{ endorsedProposals.length }}</strong>
          endorsed proposal{{ endorsedProposals.length === 1 ? '' : 's' }}
        </p>

        <div v-if="isLoading" class="panel-state">Loading endorsed proposals...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="endorsedProposals.length === 0" class="panel-state">No endorsed proposals found.</div>
        <ul v-else class="item-list">
          <li v-for="prop in endorsedProposals.slice(0, 5)" :key="prop.id" class="item">
            <div class="item-main">
              <div class="item-title">{{ prop.projectTitle || 'Untitled' }}</div>
              <div class="item-meta">
                {{ prop.projectLeader || prop.proponent?.name || 'N/A' }} &bull;
                {{ prop.documentId || prop.proposalCode || '—' }}
              </div>
            </div>
            <span class="badge" :class="statusMeta(prop.status).cls">{{ statusMeta(prop.status).label }}</span>
          </li>
        </ul>
      </section>

      <!-- PANEL D: BUDGET PROCESSING -->
      <section class="panel">
        <div class="panel-header">
          <div class="panel-heading">
            <h3>Budget Processing</h3>
            <p class="panel-sub">Approved research project budgets</p>
          </div>
          <button class="panel-link" @click="$router.push('/budget-processing')">Open budget processing &rarr;</button>
        </div>

        <div class="budget-filters">
          <button
            v-for="filter in budgetFilters"
            :key="filter"
            class="budget-filter"
            :class="{ active: budgetFilter === filter }"
            @click="budgetFilter = filter"
          >
            {{ filter }}
          </button>
        </div>

        <div v-if="isLoading" class="panel-state">Loading budget records...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="filteredBudget.length === 0" class="panel-state">No budget records available.</div>
        <div v-else class="panel-table-wrap">
          <table class="panel-table">
            <thead>
              <tr>
                <th>Document ID</th>
                <th>Proposal Title</th>
                <th>Project Leader</th>
                <th>Status</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prop in filteredBudget.slice(0, 5)" :key="prop.id">
                <td class="cell-id">{{ prop.documentId || prop.proposalCode || '—' }}</td>
                <td class="cell-title">{{ prop.projectTitle || 'Untitled' }}</td>
                <td>{{ prop.projectLeader || prop.proponent?.name || 'N/A' }}</td>
                <td>
                  <span class="badge" :class="statusMeta(prop.status).cls">{{ statusMeta(prop.status).label }}</span>
                </td>
                <td class="text-right">
                  <button class="row-btn" @click="$router.push({ name: 'ProposalDetails', params: { id: prop.id } })">View</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/utils/api'

// Single data source reused from the existing dashboard. The backend returns all
// proposals, so every panel below is derived client-side using the exact same
// status sets used by the corresponding existing RPS pages.
const proposals = ref([])
const isLoading = ref(true)
const error = ref(null)

// Preserve the existing dashboard's record limit for the awaiting-action table.
const AWAITING_LIMIT = 3

// Status sets mirror the existing pages' filters (Revision / Endorsed / Budget).
const REVISION_STATUSES = ['RETURNED_TO_RPS', 'RPS_RETURNED']
const ENDORSED_STATUSES = ['ENDORSED', 'UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED']
const BUDGET_STATUSES = ['APPROVED', 'READY_FOR_RELEASE', 'RELEASED', 'RETURNED']
const PENDING_STATUSES = ['SUBMITTED', 'PENDING']

const byStatus = (set) => proposals.value.filter(p => set.includes(p.status))

const revisionRequests = computed(() => byStatus(REVISION_STATUSES))
const endorsedProposals = computed(() => byStatus(ENDORSED_STATUSES))
const budgetProposals = computed(() => byStatus(BUDGET_STATUSES))
const pendingReviewList = computed(() => byStatus(PENDING_STATUSES))
const awaitingAction = computed(() => pendingReviewList.value.slice(0, AWAITING_LIMIT))

// Budget category filters mirror the existing Budget Processing page.
const budgetFilters = ['All', 'Approved', 'Ready for Release', 'Released', 'Returned']
const budgetStatusMap = {
  'Approved': 'APPROVED',
  'Ready for Release': 'READY_FOR_RELEASE',
  'Released': 'RELEASED',
  'Returned': 'RETURNED',
}
const budgetFilter = ref('All')
const filteredBudget = computed(() => {
  if (budgetFilter.value === 'All') return budgetProposals.value
  const status = budgetStatusMap[budgetFilter.value]
  return budgetProposals.value.filter(p => p.status === status)
})

// Status label + badge mapping reusing the existing status meanings.
const STATUS_META = {
  SUBMITTED: { label: 'Pending', cls: 'pending' },
  PENDING: { label: 'Pending', cls: 'pending' },
  RETURNED_TO_RPS: { label: 'Returned to RPS', cls: 'returned' },
  RPS_RETURNED: { label: 'Returned to Proponent', cls: 'returned' },
  REVISION: { label: 'Revision', cls: 'revision' },
  REC_REVISION: { label: 'Revision', cls: 'revision' },
  ENDORSED: { label: 'Endorsed', cls: 'endorsed' },
  UNDER_REVIEW: { label: 'Under Review', cls: 'pending' },
  REC_APPROVED: { label: 'REC Approved', cls: 'approved' },
  FOR_OVCAF_APPROVAL: { label: 'For OVCAF Review', cls: 'pending' },
  FOR_OC_APPROVAL: { label: 'For Chancellor Approval', cls: 'pending' },
  APPROVED: { label: 'Approved', cls: 'approved' },
  READY_FOR_RELEASE: { label: 'Ready for Release', cls: 'ready' },
  RELEASED: { label: 'Released', cls: 'released' },
  RETURNED: { label: 'Returned', cls: 'returned' },
}
const statusMeta = (status) => STATUS_META[status] || { label: status || 'Unknown', cls: 'pending' }

// Existing deadline logic: overdue when the revision deadline has passed.
function isOverdue(prop) {
  if (!prop.revisionDeadline) return false
  const d = new Date(prop.revisionDeadline)
  return !isNaN(d.getTime()) && d.getTime() < Date.now()
}

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleDateString()
}

function revDateLabel(prop) {
  if (prop.status === 'RPS_RETURNED') return 'Deadline: ' + formatDate(prop.revisionDeadline)
  return 'Returned: ' + formatDate(prop.returnedAt)
}

async function loadDashboardData() {
  isLoading.value = true
  error.value = null
  try {
    const res = await api.get('/api/proposals')
    proposals.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error('Failed to load RPS dashboard metrics:', err)
    error.value = 'Failed to load dashboard data.'
    proposals.value = []
  } finally {
    isLoading.value = false
  }
}

// ===== Proposal announcements (reuses the existing Proposal Announcement feature) =====
// Only a small subset is shown on the dashboard; the full list lives on the
// existing Proposal Announcements page. Announcement order/visibility is unchanged.
const ANNOUNCEMENT_LIMIT = 4
const announcements = ref([])
const announcementsLoading = ref(true)
const announcementsError = ref(null)
const visibleAnnouncements = computed(() => announcements.value.slice(0, ANNOUNCEMENT_LIMIT))

function formatAnnouncementDate(iso) {
  if (!iso) return '—'
  const d = new Date(`${iso}T00:00:00`)
  return isNaN(d.getTime()) ? String(iso) : d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function formatAnnouncementShortDate(iso) {
  if (!iso) return ''
  const d = new Date(`${iso}T00:00:00`)
  return isNaN(d.getTime()) ? String(iso) : d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' })
}

/** Local today as YYYY-MM-DD (matches backend LocalDate serialization). */
function localTodayISO() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

function daysBetween(fromISO, toISO) {
  const from = new Date(`${fromISO}T00:00:00`)
  const to = new Date(`${toISO}T00:00:00`)
  if (isNaN(from.getTime()) || isNaN(to.getTime())) return NaN
  return Math.round((to.getTime() - from.getTime()) / 86400000)
}

function announcementIsActiveNow(announcement) {
  const today = localTodayISO()
  return announcement.status === 'ACTIVE' && announcement.startDate <= today && today <= announcement.endDate
}

/** Display variant derived from the existing status + date-range rules. */
function announcementVariant(announcement) {
  const today = localTodayISO()
  if (announcement.endDate < today) return 'expired'
  if (announcementIsActiveNow(announcement)) return 'open'
  if (announcement.status === 'ACTIVE' && announcement.startDate > today) return 'upcoming'
  return 'neutral'
}

function announcementStatusLabel(announcement) {
  const variant = announcementVariant(announcement)
  if (variant === 'open') return 'Open now · accepting submissions'
  if (variant === 'expired') return 'Expired'
  if (variant === 'upcoming') return `Opens ${formatAnnouncementDate(announcement.startDate)}`
  return announcement.status
}

/** Badge always reflects the actual state; expired is shown as EXPIRED. */
function announcementBadge(announcement) {
  return announcementVariant(announcement) === 'expired' ? 'EXPIRED' : announcement.status
}

function announcementHasTimeLeft(announcement) {
  const variant = announcementVariant(announcement)
  return variant === 'open' || variant === 'upcoming'
}

function announcementTimeLeft(announcement) {
  const d = daysBetween(localTodayISO(), announcement.endDate)
  if (isNaN(d) || d < 0) return '—'
  return `${d} day${d === 1 ? '' : 's'}`
}

/** Elapsed share of the submission window, clamped to 0–100 (null when not meaningful). */
function announcementProgress(announcement) {
  const start = new Date(`${announcement.startDate}T00:00:00`).getTime()
  const end = new Date(`${announcement.endDate}T00:00:00`).getTime()
  if (isNaN(start) || isNaN(end) || end <= start) return null
  const now = new Date(`${localTodayISO()}T00:00:00`).getTime()
  const pct = ((now - start) / (end - start)) * 100
  return Math.min(100, Math.max(0, Math.round(pct)))
}

const announcementCards = computed(() =>
  visibleAnnouncements.value.map((announcement) => {
    const variant = announcementVariant(announcement)
    return {
      id: announcement.id,
      title: announcement.name,
      variant,
      statusLabel: announcementStatusLabel(announcement),
      badge: announcementBadge(announcement),
      opensLabel: variant === 'expired' ? 'Opened' : 'Opens',
      deadlineLabel: variant === 'expired' ? 'Closed' : 'Deadline',
      opens: formatAnnouncementDate(announcement.startDate),
      deadline: formatAnnouncementDate(announcement.endDate),
      hasTimeLeft: announcementHasTimeLeft(announcement),
      timeLeft: announcementTimeLeft(announcement),
      progress: announcementProgress(announcement),
      startShort: formatAnnouncementShortDate(announcement.startDate),
      endShort: formatAnnouncementShortDate(announcement.endDate),
    }
  })
)

async function loadAnnouncements() {
  announcementsLoading.value = true
  announcementsError.value = null
  try {
    const res = await api.get('/api/proposal-announcements')
    announcements.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error('Failed to load proposal announcements:', err)
    announcementsError.value = 'Failed to load proposal announcements.'
    announcements.value = []
  } finally {
    announcementsLoading.value = false
  }
}

const isRefreshing = computed(() => isLoading.value || announcementsLoading.value)

function refreshDashboard() {
  loadDashboardData()
  loadAnnouncements()
}

onMounted(() => {
  loadDashboardData()
  loadAnnouncements()
})
</script>

<style scoped>
.dashboard-container {
  /* Fill the content area (which has 32px padding) with the light page background. */
  margin: -32px;
  padding: 22px 26px 30px;
  background: #f3f4f6;
  min-height: calc(100vh - 169px);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
  font-family: 'Inter', sans-serif;
}

/* HEADER */
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  flex-wrap: wrap;
}

.header-text h2 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #20234f;
  line-height: 1.2;
}

.header-text p {
  margin: 4px 0 0;
  font-size: 12px;
  font-weight: 400;
  color: #77809a;
  line-height: 1.4;
}

.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 34px;
  padding: 0 13px;
  background: #ffffff;
  border: 1px solid #e6e8ef;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 600;
  color: #25245b;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #25245b;
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-icon {
  width: 14px;
  height: 14px;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* PROPOSAL ANNOUNCEMENTS */
.announcements-section {
  width: 100%;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.section-heading h3 {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  color: #25245b;
  line-height: 1.2;
}

.section-sub {
  margin: 3px 0 0;
  font-size: 10px;
  font-weight: 400;
  color: #78809a;
  line-height: 1.4;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: transparent;
  border: none;
  padding: 0;
  font-size: 10px;
  font-weight: 600;
  color: #25245b;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.section-link:hover {
  text-decoration: underline;
}

.section-link-arrow {
  font-size: 11px;
  line-height: 1;
}

.announcements-panel {
  width: 100%;
  box-sizing: border-box;
  background: #ffffff;
  border: 1px solid #e5e7ef;
  border-radius: 13px;
  padding: 14px;
}

.announcement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 11px;
  align-items: start;
}

.announcement-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 13px;
  border: 1px solid #e6e8ef;
  border-radius: 12px;
  background: #ffffff;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

/* OPEN / ACCEPTING SUBMISSIONS */
.announcement-card.is-open {
  background: #f0fdf4;
  border-color: #bbf7d0;
}

/* EXPIRED */
.announcement-card.is-expired {
  background: #f7f7fc;
  border-color: #e6e6f0;
}

/* UPCOMING */
.announcement-card.is-upcoming {
  background: #f5f8ff;
  border-color: #dce6fa;
}

/* NEUTRAL (draft / other) */
.announcement-card.is-neutral {
  background: #fafafb;
  border-color: #e6e8ef;
}

.announcement-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  min-width: 0;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
  background: #9ca3af;
}

.status-label {
  font-size: 8.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  color: #64748b;
  line-height: 1.2;
}

.status-badge {
  flex-shrink: 0;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 8px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.3px;
  background: #e5e7eb;
  color: #374151;
}

.is-open .status-dot { background: #16a34a; }
.is-open .status-label { color: #166534; }
.is-open .status-badge { background: #dcfce7; color: #166534; }

.is-expired .status-dot { background: #8b8ba7; }
.is-expired .status-label { color: #777792; }
.is-expired .status-badge { background: #e8e8f3; color: #777792; }

.is-upcoming .status-dot { background: #2563eb; }
.is-upcoming .status-label { color: #2563eb; }
.is-upcoming .status-badge { background: #dbeafe; color: #1d4ed8; }

.announcement-title {
  margin: 10px 0 8px;
  font-size: 14px;
  font-weight: 700;
  color: #25245b;
  line-height: 1.3;
}

.announcement-meta {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px 12px;
}

.announcement-meta.meta-two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.meta-group {
  min-width: 0;
}

.meta-label {
  display: block;
  font-size: 7.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: #78809a;
}

.meta-value {
  display: block;
  margin-top: 2px;
  font-size: 9.5px;
  font-weight: 600;
  color: #25245b;
  line-height: 1.3;
}

.announcement-progress {
  margin-top: 11px;
}

.progress-track {
  width: 100%;
  height: 4.5px;
  background: #d1fae5;
  border-radius: 999px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #16a34a;
  border-radius: 999px;
  transition: width 0.18s ease;
}

.is-expired .progress-track { background: #dad9e8; }
.is-expired .progress-fill { background: #8b8ba7; }
.is-upcoming .progress-track { background: #dbeafe; }
.is-upcoming .progress-fill { background: #93b4f5; }

.progress-dates {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 7.5px;
  color: #78809a;
}

/* PANELS GRID */
.panels-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.panel {
  background: #ffffff;
  border: 1px solid #e6e8ef;
  border-radius: 12px;
  padding: 16px;
  box-sizing: border-box;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.panel-heading h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #20234f;
}

.panel-sub {
  margin: 3px 0 0;
  font-size: 11px;
  color: #77809a;
}

.panel-link {
  background: none;
  border: none;
  padding: 0;
  font-size: 11px;
  font-weight: 600;
  color: #25245b;
  font-family: inherit;
  cursor: pointer;
  white-space: nowrap;
}

.panel-link:hover {
  text-decoration: underline;
}

.panel-summary {
  margin: 0 0 10px;
  font-size: 11px;
  color: #77809a;
}

.panel-summary strong {
  font-size: 14px;
  color: #20234f;
}

.panel-state {
  padding: 14px 2px;
  font-size: 12px;
  color: #77809a;
}

.error-text {
  color: #d64545;
}

/* TABLES */
.panel-table-wrap {
  width: 100%;
  overflow-x: auto;
}

.panel-table {
  width: 100%;
  border-collapse: collapse;
}

.panel-table th {
  padding: 7px 8px;
  text-align: left;
  font-size: 9px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: #77809a;
  border-bottom: 1px solid #e6e8ef;
  white-space: nowrap;
}

.panel-table td {
  padding: 8px;
  font-size: 11px;
  color: #1e234f;
  border-bottom: 1px solid #eef0f5;
  vertical-align: middle;
}

.panel-table tbody tr:last-child td {
  border-bottom: none;
}

.panel-table tbody tr:hover {
  background: #f8fafc;
}

.text-right {
  text-align: right;
}

.cell-title {
  font-weight: 500;
}

.cell-id {
  font-size: 10px;
  font-weight: 500;
  color: #25245b;
  white-space: nowrap;
}

/* BADGES */
.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 600;
  white-space: nowrap;
}

.badge.pending,
.badge.ready {
  background: #fef3c7;
  color: #92400e;
}

.badge.revision,
.badge.returned {
  background: #fde8e8;
  color: #b91c1c;
}

.badge.endorsed {
  background: #dbeafe;
  color: #1d4ed8;
}

.badge.approved {
  background: #dcfce7;
  color: #166534;
}

.badge.released {
  background: #e0e7ff;
  color: #3730a3;
}

/* ROW ACTION BUTTON */
.row-btn {
  background: #ffd400;
  color: #25245b;
  border: none;
  height: 27px;
  padding: 0 11px;
  border-radius: 6px;
  font-size: 10px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.row-btn:hover {
  background: #f0c800;
}

.row-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

/* ITEM LISTS (Revision + Endorsed) */
.item-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
  padding: 9px 11px;
  background: #ffffff;
  border: 1px solid #e6e8ef;
  border-radius: 8px;
}

.item.urgent {
  background: #fef6f6;
  border-color: #f6c9c9;
}

.item-main {
  min-width: 0;
}

.item-title {
  font-size: 11px;
  font-weight: 600;
  color: #1e234f;
  line-height: 1.35;
}

.item-meta {
  margin-top: 2px;
  font-size: 9px;
  color: #77809a;
  overflow-wrap: anywhere;
}

/* BUDGET FILTERS */
.budget-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 10px;
}

.budget-filter {
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #e6e8ef;
  background: #ffffff;
  color: #25245b;
  font-size: 10px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.budget-filter:hover {
  border-color: #25245b;
}

.budget-filter.active {
  background: #25245b;
  border-color: #25245b;
  color: #ffd400;
  font-weight: 600;
}

/* RESPONSIVE */
@media (max-width: 900px) {
  .panels-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .announcement-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .dashboard-container {
    padding: 16px 14px 24px;
  }

  .dashboard-header {
    flex-direction: column;
    align-items: stretch;
  }

  .section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
