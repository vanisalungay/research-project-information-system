<template>
  <!--
    The RPS Staff overview dashboard.

    Every panel below reads the records the RPS pages already use: the proposal list
    (GET /api/proposals) and the Proposal Announcement list (GET /api/proposal-announcements).
    The screen answers "what needs my attention?" and links to the full pages, which keep
    their own filters, tables and actions.

    Only the presentation lives here. The markup reuses the shared design layer
    (assets/dashboard.css and assets/endorsement.css) - the same cards, pills, tables and
    spacing the Proponent and Dean dashboards already use - so no request, no binding and no
    action changed: the panels are the same ones, in the same order, with the same data.
  -->
  <div class="dash-page">
    <!-- ===================== HEADER / WELCOME ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.users"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">RPS Overview</h2>
        <p class="dash-banner-sub">Administrative and publication services monitoring panel.</p>
      </div>
      <button class="refresh-btn" @click="refreshDashboard" :disabled="isRefreshing">
        <svg
          class="refresh-icon"
          :class="{ spinning: isRefreshing }"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          stroke-linecap="round"
          stroke-linejoin="round"
        >
          <path d="M21.5 2v6h-6M21.34 15.57a10 10 0 11-.57-8.38l5.67-5.67"></path>
        </svg>
        <span>{{ isRefreshing ? 'Refreshing...' : 'Refresh Data' }}</span>
      </button>
    </section>

    <!-- ===== ROW 1: PROPOSAL ANNOUNCEMENTS (2/3) + ENDORSED PROPOSALS (1/3) ===== -->
    <div class="dash-row dash-row--main">
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.megaphone"></span>
          <div class="section-heading">
            <h3 class="dash-section-title">Proposal announcements</h3>
            <p class="section-sub">Current proposal calls, submission periods and announcement details.</p>
          </div>
          <div class="dash-section-actions">
            <button class="section-link" @click="$router.push('/proposal-announcements')">
              <span>View all announcements</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

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
      </section>

      <!-- Endorsed proposals: the same list and the same count as before, in the right column -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.endorse"></span>
          <h3 class="dash-section-title">Endorsed Proposals</h3>
          <div class="dash-section-actions">
            <button class="section-link" @click="$router.push('/rpsendorsed-prop')">
              <span>View all</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

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
    </div>

    <!-- ===== ROW 2: AWAITING RPS ACTION (2/3) + REVISION REQUESTS (1/3) ===== -->
    <div class="dash-row dash-row--main">
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.proposals"></span>
          <div class="section-heading">
            <h3 class="dash-section-title">Awaiting RPS Action</h3>
            <p class="section-sub">Newest submissions to review</p>
          </div>
          <div class="dash-section-actions">
            <button class="section-link" @click="$router.push('/rps-subproposal')">
              <span>View all submissions</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

        <div v-if="isLoading" class="panel-state">Loading submissions...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="awaitingAction.length === 0" class="panel-state">No submissions awaiting action.</div>
        <div v-else class="end-table-scroll">
          <table class="end-table panel-table">
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
                  <button class="row-btn" @click="$router.push(`/proposal/${prop.id}`)">
                    <span>Review</span>
                    <span class="row-btn-arrow" aria-hidden="true">&rarr;</span>
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Revision requests: the same rows, limit and deadline label as before -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.revisions"></span>
          <h3 class="dash-section-title">Revision Requests</h3>
          <div class="dash-section-actions">
            <button class="section-link" @click="$router.push('/rps-revision-requests')">
              <span>View all</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

        <p class="panel-summary">
          <strong>{{ revisionRequests.length }}</strong>
          revision request{{ revisionRequests.length === 1 ? '' : 's' }}
        </p>

        <div v-if="isLoading" class="panel-state">Loading revision requests...</div>
        <div v-else-if="error" class="panel-state error-text">{{ error }}</div>
        <div v-else-if="revisionRequests.length === 0" class="panel-state">No revision requests found.</div>
        <ul v-else class="item-list">
          <li
            v-for="prop in revisionRequests.slice(0, 5)"
            :key="prop.id"
            class="item"
            :class="{ urgent: isOverdue(prop) }"
          >
            <div class="item-main">
              <div class="item-title">{{ prop.projectTitle || 'Untitled' }}</div>
              <div class="item-meta">
                {{ prop.projectLeader || prop.proponent?.name || 'Unknown' }} &bull;
                {{ prop.documentId || prop.proposalCode || '—' }}
              </div>
              <div class="item-meta item-due">
                <span class="item-icon" aria-hidden="true" v-html="icons.calendar"></span>
                <span>{{ revDateLabel(prop) }}</span>
              </div>
            </div>
            <span class="badge" :class="statusMeta(prop.status).cls">{{ statusMeta(prop.status).label }}</span>
          </li>
        </ul>
      </section>
    </div>

    <!-- ===== ROW 3: BUDGET PROCESSING (FULL WIDTH) ===== -->
    <section class="dash-section">
      <header class="dash-section-head">
        <span class="dash-section-icon" v-html="icons.budget"></span>
        <div class="section-heading">
          <h3 class="dash-section-title">Budget Processing</h3>
          <p class="section-sub">Approved research project budgets</p>
        </div>
        <div class="dash-section-actions">
          <button class="section-link" @click="$router.push('/budget-processing')">
            <span>Open budget processing</span>
            <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
          </button>
        </div>
      </header>

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
      <div v-else class="end-table-scroll">
        <table class="end-table panel-table">
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
                <button
                  class="row-btn"
                  @click="$router.push({ name: 'ProposalDetails', params: { id: prop.id } })"
                >
                  <span>View</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import api from '@/utils/api'
import { announcementStatus } from '@/utils/announcementStatus'

// The shared design layer: the same cards, pills, tables and spacing the Proponent and Dean
// dashboards already use. Importing it changes how this page looks and nothing else - every
// request, computed value and action below is untouched.
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment. The map
 * is presentation only - no panel, request or binding depends on it.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  users: svg(
    '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>' +
      '<path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  ),
  megaphone: svg(
    '<path d="M3 11v2a1 1 0 0 0 1 1h2l4 4V6L6 10H4a1 1 0 0 0-1 1z"/>' +
      '<path d="M15 8.5a4 4 0 0 1 0 7"/><path d="M18 6a8 8 0 0 1 0 12"/>',
  ),
  proposals: svg(
    '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/>' +
      '<path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>',
  ),
  endorse: svg(
    '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>',
  ),
  revisions: svg('<path d="M3 2v6h6"/><path d="M3.51 15a9 9 0 1 0 2.13-9.36L3 8"/>'),
  budget: svg('<path d="M12 3v18"/><path d="M16 7H10a3 3 0 0 0 0 6h4a3 3 0 0 1 0 6H7"/>'),
  calendar: svg(
    '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M8 3v4M16 3v4M3 11h18"/>',
  ),
}

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
  const status = announcementStatus(announcement)
  if (status === 'Open') return 'Open now · accepting submissions'
  if (status === 'Scheduled') return `Opens ${formatAnnouncementDate(announcement.startDate)}`
  return status
}

/** Badge shows the standard announcement status: Draft, Scheduled, Open or Closed. */
function announcementBadge(announcement) {
  return announcementStatus(announcement)
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
/* ============================================================================
   RPS dashboard presentation.

   The page reuses the shared design layer: assets/dashboard.css provides the
   welcome banner, the section cards, the grid and the pastel accents, and
   assets/endorsement.css provides the buttons, the status badges and the
   tables. Only what is specific to this screen is declared here, so nothing can
   leak into another page.
   ============================================================================ */

/* ===== Header: the refresh action inside the banner ===== */
.refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
  padding: 10px 18px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.14);
  color: #ffffff;
  font-family: inherit;
  font-size: 0.875rem;
  font-weight: 600;
  transition: background-color 0.15s ease, border-color 0.15s ease;
}

.refresh-btn:hover:not(:disabled) {
  background: rgba(255, 255, 255, 0.24);
  border-color: rgba(255, 255, 255, 0.5);
}

.refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.refresh-icon {
  width: 15px;
  height: 15px;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* The banner avatar carries an icon injected with v-html, so it is reached through :deep(). */
.dash-avatar :deep(svg) {
  width: 26px;
  height: 26px;
  stroke-width: 1.8;
}

/* ===== Section headings ===== */
.section-heading {
  flex: 1 1 auto;
  min-width: 0;
}

.section-sub {
  margin: 2px 0 0;
  font-size: 0.8125rem;
  line-height: 1.4;
  color: #78809a;
}

.section-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 6px;
  border-radius: 8px;
  background: transparent;
  color: #315bff;
  font-family: inherit;
  font-size: 0.8125rem;
  font-weight: 600;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.section-link:hover {
  background: #eef1ff;
  color: #1d40cc;
}

.section-link-arrow {
  font-size: 0.875rem;
  line-height: 1;
}

/* The count a list panel already shows above its rows. */
.panel-summary {
  margin: 0;
  font-size: 0.8125rem;
  color: #78809a;
}

.panel-summary strong {
  font-size: 1.125rem;
  color: #1c1a3c;
  font-variant-numeric: tabular-nums;
}

/* Loading, error and empty messages. */
.panel-state {
  padding: 10px 2px;
  font-size: 0.875rem;
  color: #78809a;
}

.error-text {
  color: #b91c1c;
}


/* ===== Proposal announcements =====
   Two cards per row while the panel has the room, one below 720px. The tone of a
   card still follows the status the card already shows: Open, Scheduled or Closed. */
.announcement-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
}

.announcement-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 16px;
  border: 1px solid #e8eaf6;
  border-radius: 12px;
  background: #fdfdff;
}

.announcement-card.is-open {
  background: #f6fef9;
  border-color: #cdf0d9;
}

.announcement-card.is-upcoming {
  background: #f7f9ff;
  border-color: #dfe6fb;
}

.announcement-card.is-expired {
  background: #fafafc;
  border-color: #e8e8f2;
}

.announcement-card.is-neutral {
  background: #fbfbfd;
  border-color: #e8eaf6;
}

.announcement-status-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.status-indicator {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.status-dot {
  flex-shrink: 0;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #9ca3af;
}

.status-label {
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #64748b;
}

.status-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  background: #eef0f6;
  color: #4b5570;
}

.is-open .status-dot {
  background: #10b981;
}

.is-open .status-label {
  color: #047857;
}

.is-open .status-badge {
  background: #d1fae5;
  color: #065f46;
}

.is-upcoming .status-dot {
  background: #7c3aed;
}

.is-upcoming .status-label {
  color: #6d28d9;
}

.is-upcoming .status-badge {
  background: #ede9fe;
  color: #6d28d9;
}

.is-expired .status-dot {
  background: #8b8ba7;
}

.is-expired .status-label {
  color: #6b7591;
}

.is-expired .status-badge {
  background: #eceef6;
  color: #5b6484;
}

.announcement-title {
  margin: 12px 0 10px;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.35;
  color: #1c1a3c;
  overflow-wrap: anywhere;
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
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #78809a;
}

.meta-value {
  display: block;
  margin-top: 2px;
  font-size: 0.8125rem;
  font-weight: 600;
  line-height: 1.35;
  color: #25245b;
}

.announcement-progress {
  margin-top: 14px;
}

.progress-track {
  width: 100%;
  height: 5px;
  border-radius: 999px;
  background: #e6e9f6;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #315bff 0%, #7c3aed 100%);
  transition: width 0.18s ease;
}

.is-open .progress-track {
  background: #d6f2e1;
}

.is-open .progress-fill {
  background: linear-gradient(90deg, #10b981 0%, #34d399 100%);
}

.is-upcoming .progress-track {
  background: #e3e8fb;
}

.is-upcoming .progress-fill {
  background: #a5b4fc;
}

.is-expired .progress-track {
  background: #e6e8f2;
}

.is-expired .progress-fill {
  background: #b8bcd2;
}

.progress-dates {
  display: flex;
  justify-content: space-between;
  margin-top: 6px;
  font-size: 0.6875rem;
  color: #78809a;
}


/* ===== Revision requests and endorsed proposals =====
   One row per record, with the same title, meta and status the panel already
   rendered. An overdue revision keeps the red tint it already had. */
.item-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #e8eaf6;
  border-radius: 10px;
  background: #fdfdff;
  transition: border-color 0.15s ease, background-color 0.15s ease;
}

.item:hover {
  border-color: #d5daf0;
  background: #fafbff;
}

.item.urgent {
  background: #fff6f6;
  border-color: #f8d4d4;
}

.item-main {
  min-width: 0;
}

.item-title {
  font-size: 0.875rem;
  font-weight: 600;
  line-height: 1.4;
  color: #1c1a3c;
  overflow-wrap: anywhere;
}

.item-meta {
  margin-top: 3px;
  font-size: 0.75rem;
  color: #78809a;
  overflow-wrap: anywhere;
}

/* The date line of a revision keeps its calendar glyph beside the label. */
.item-due {
  display: flex;
  align-items: center;
  gap: 5px;
}

.item-icon {
  display: inline-flex;
  color: #78809a;
}

.item-icon :deep(svg) {
  width: 13px;
  height: 13px;
  stroke-width: 2;
}

/* ===== Status pills =====
   The label and the tone are the ones the panel already used. */
.badge {
  display: inline-block;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
  white-space: nowrap;
}

.badge.pending,
.badge.ready {
  background: #fef3c7;
  color: #92400e;
}

.badge.revision,
.badge.returned {
  background: #fee2e2;
  color: #991b1b;
}

.badge.endorsed {
  background: #dbeafe;
  color: #1d4ed8;
}

.badge.approved {
  background: #d1fae5;
  color: #065f46;
}

.badge.released {
  background: #ede9fe;
  color: #6d28d9;
}

/* ===== Row action ===== */
.row-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 14px;
  border-radius: 8px;
  background: #315bff;
  color: #ffffff;
  font-family: inherit;
  font-size: 0.75rem;
  font-weight: 700;
  transition: background-color 0.15s ease, box-shadow 0.15s ease;
}

.row-btn:hover {
  background: #1d40cc;
  box-shadow: 0 6px 16px rgba(49, 91, 255, 0.24);
}

.row-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.row-btn-arrow {
  font-size: 0.8125rem;
  line-height: 1;
}

/* ===== Budget filters ===== */
.budget-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.budget-filter {
  padding: 6px 14px;
  border: 1px solid #e3e6f2;
  border-radius: 999px;
  background: #ffffff;
  color: #4b5570;
  font-family: inherit;
  font-size: 0.75rem;
  font-weight: 600;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.budget-filter:hover {
  border-color: #c3cbf0;
  color: #25245b;
}

.budget-filter.active {
  background: #315bff;
  border-color: #315bff;
  color: #ffffff;
}

/* ===== Tables =====
   The shared .end-table is used as it is; only the two things a dashboard row
   needs are adjusted here - the header keeps the light band, and the cells line
   up with the action button beside them. */
.panel-table th {
  background: #f6f7fd;
  font-weight: 700;
}

.panel-table th.text-right,
.panel-table td.text-right {
  text-align: right;
}

.panel-table td {
  vertical-align: middle;
}

.panel-table tbody tr:hover td {
  background: #fafbff;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

.cell-id {
  font-size: 0.75rem;
  font-weight: 600;
  color: #5b6484;
  white-space: nowrap;
}

/* ===== Small screens ===== */
@media (max-width: 720px) {
  .announcement-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .refresh-btn {
    width: 100%;
    margin-left: 0;
    justify-content: center;
  }

  .item {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

