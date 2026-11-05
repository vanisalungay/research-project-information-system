<template>
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== HEADER ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.office"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">OVCRIGE Dashboard</h2>
        <p class="dash-banner-sub">
          Office of the Vice Chancellor for Research, Innovation and Global Engagement
        </p>
      </div>
    </section>
    <!-- SIDEBAR -->
    <!-- <aside class="sidebar">
      <h3 class="sidebar-title">Menu</h3>

      <ul class="menu">
        <li class="active">Dashboard</li>
        <li>Submitted Proposals</li>
        <li>Endorsed Proposal</li>
        <li>Notifications</li>
      </ul>
    </aside> -->

    <!-- ===================== STATISTICS ===================== -->
    <section class="dash-stats">
      <div class="dash-stat dash-stat--blue">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.endorsed"></span>
          <span class="dash-stat-label">Endorsed from RPS</span>
        </div>
        <div class="dash-stat-value">{{ stats.endorsed }}</div>
      </div>

      <div class="dash-stat dash-stat--amber">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.clock"></span>
          <span class="dash-stat-label">Under REC Review</span>
        </div>
        <div class="dash-stat-value">{{ stats.underReview }}</div>
      </div>

      <div class="dash-stat dash-stat--green">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.check"></span>
          <span class="dash-stat-label">REC Approved</span>
        </div>
        <div class="dash-stat-value">{{ stats.recApproved }}</div>
      </div>

      <div class="dash-stat dash-stat--red">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.revision"></span>
          <span class="dash-stat-label">For Revision</span>
        </div>
        <div class="dash-stat-value">{{ stats.revision }}</div>
      </div>
    </section>

    <!-- ===================== PROPOSALS + NOTIFICATIONS ===================== -->
    <div class="dash-row dash-row--main">
      <!-- Latest Endorsed Proposals -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.proposals"></span>
          <h3 class="dash-section-title">Latest Endorsed Proposals</h3>
          <div class="dash-section-actions">
            <button class="section-link" @click="goToEndorsed">
              <span>View All</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

        <div class="end-table-scroll">
          <table class="end-table ovc-table">
            <thead>
              <tr>
                <th>Proposal ID</th>
                <th>Title</th>
                <th>Proponent</th>
                <th>Submitted</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>

            <tbody>
              <tr v-if="loading">
                <td colspan="6" class="ovc-empty">
                  Loading...
                </td>
              </tr>

              <tr v-else-if="proposals.length === 0">
                <td colspan="6" class="ovc-empty">
                  No proposals found.
                </td>
              </tr>

              <tr v-for="proposal in proposals.slice(0, 5)" :key="proposal.id">
                <td class="cell-id">
                  {{ proposal.documentId || proposal.proposalCode || '—' }}
                </td>
                <td class="cell-title">{{ proposal.projectTitle }}</td>
                <td>
                  {{ proposal.projectLeader || proposal.proponent?.name || '-' }}
                </td>
                <td class="cell-id">
                  {{ proposal.createdAt?.substring(0, 10) }}
                </td>
                <td>
                  <span class="end-badge" :class="statusTone(proposal.status)">
                    {{ proposal.status }}
                  </span>
                </td>
                <td class="text-right">
                  <button class="end-btn is-primary row-btn" @click="goToReview(proposal.id)">
                    REVIEW PROPOSAL
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Notifications -->
      <aside class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.bell"></span>
          <h3 class="dash-section-title">Notifications</h3>
        </header>

        <ul class="dash-notes">
          <li v-if="loading" class="dash-empty">
            Loading...
          </li>

          <li v-else-if="notifications.length === 0" class="dash-empty">
            No notifications.
          </li>

          <li
            v-for="notification in notifications.slice(0, 5)"
            :key="notification.id"
            class="dash-note dash-note--clickable"
            @click="openNotification(notification)"
          >
            <span class="dash-note-icon" v-html="icons.bell"></span>
            <div class="dash-note-body">
              <p class="dash-note-text">{{ notification.message }}</p>
              <div class="dash-note-foot">
                <span class="dash-note-date">
                  {{ notification.createdAt?.substring(0, 16) }}
                </span>
              </div>
            </div>
          </li>
        </ul>

        <button class="section-link notif-view-all" @click="goToNotifications">
          <span>View All</span>
          <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { UserRole } from '@/stores/userData'
import { proposalNotificationDestination } from '@/utils/proposalNotificationTarget'

// The shared design layer of the role dashboards: the welcome banner, the statistic cards,
// the section cards, the grid and the pastel accents (dashboard.css) plus the buttons, the
// status pills and the table (endorsement.css). Importing them changes how this page looks
// and nothing else - every request, binding and action below is unchanged.
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment. The map
 * is presentation only - no statistic, proposal or notification depends on it.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  office: svg(
    '<path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/>' +
      '<path d="M9 21v-5h6v5"/><path d="M9 10h.01M15 10h.01M9 13h.01M15 13h.01"/>',
  ),
  endorsed: svg(
    '<path d="M12 3l7 3v5c0 4.4-3 7.9-7 9-4-1.1-7-4.6-7-9V6z"/>' +
      '<path d="m9 12 2 2 4-4.5"/>',
  ),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  check: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  revision: svg(
    '<path d="M3 12a9 9 0 0 1 15.5-6.2L21 8"/><path d="M21 3v5h-5"/>' +
      '<path d="M21 12a9 9 0 0 1-15.5 6.2L3 16"/><path d="M3 21v-5h5"/>',
  ),
  proposals: svg(
    '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/>' +
      '<path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>',
  ),
  bell: svg(
    '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>' +
      '<path d="M13.7 21a2 2 0 0 1-3.4 0"/>',
  ),
}

/**
 * The pastel tone of a status pill. The status text shown stays exactly what the record
 * carries; only the colour is chosen here, by the workflow stage the status belongs to.
 */
const statusTone = (status) => {
  switch (status) {
    case 'SUBMITTED':
      return 'is-pending'
    case 'ENDORSED':
    case 'FOR_OVCAF_APPROVAL':
    case 'FOR_OC_APPROVAL':
      return 'is-info'
    case 'UNDER_REVIEW':
      return 'is-progress'
    case 'REC_APPROVED':
    case 'APPROVED':
    case 'OVC_APPROVED':
      return 'is-approved'
    case 'REVISION':
    case 'REC_REVISION':
    case 'REJECTED':
    case 'REC_REJECTED':
      return 'is-rejected'
    default:
      return 'is-neutral'
  }
}

const router = useRouter()

// Dashboard data (will come from backend)
const stats = ref({
  endorsed: 0,
  underReview: 0,
  recApproved: 0,
  revision: 0
})

const proposals = ref([])
const notifications = ref([])

const loading = ref(true)

const loadDashboard = async () => {
  try {
    // Fetch proposals relevant to OVCRIGE workflow
    // Use proper params serialization for array parameters
    const proposalResponse = await api.get('/api/proposals', {
      params: {
        statusIn: ['SUBMITTED', 'ENDORSED', 'UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED']
      },
      paramsSerializer: {
        indexes: null // array format: statusIn=a&statusIn=b&statusIn=c
      }
    })

    proposals.value = proposalResponse.data

    stats.value.endorsed = proposals.value.filter(
      p => p.status === 'ENDORSED'
    ).length

    stats.value.underReview = proposals.value.filter(
      p => p.status === 'UNDER_REVIEW'
    ).length

    stats.value.recApproved = proposals.value.filter(
      p => p.status === 'REC_APPROVED' || p.status === 'OVC_APPROVED'
    ).length

    stats.value.revision = proposals.value.filter(
      p => p.status === 'REVISION' || p.status === 'REC_REVISION'
    ).length

    // Notification endpoint
    // Replace userId later using logged in user
    const notifResponse = await api.get(
      '/api/notifications?userId=4'
    )

    notifications.value = notifResponse.data

  } catch (error) {
    console.log(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})

const goToNotifications = () => {
  router.push('/notifications')
}

/**
 * Where a notification opens: the exact proposal it carries, on this role's proposal page
 * (/review-prop/:id). The id is the saved record reference - never the notification title -
 * and a notification with no reference has no destination.
 */
const notificationDestination = (notification) =>
  proposalNotificationDestination(notification, UserRole.OVCRIGE)

/** Opens the proposal a notification is about. Navigation only - the read state is untouched. */
const openNotification = (notification) => {
  const destination = notificationDestination(notification)
  if (destination) router.push(destination)
}

const goToEndorsed = () => {
  router.push('/submit-proposals')
}

const goToReview = id => {
  router.push(`/review-prop/${id}`)
}
</script>

<style scoped>
/* ============================================================================
   OVCRIGE dashboard presentation.

   The page reuses the shared design layer: assets/dashboard.css provides the welcome
   banner, the statistic cards, the section cards, the grid and the pastel accents, and
   assets/endorsement.css provides the buttons, the status pills and the table. Only what
   is specific to this screen is declared here, so nothing can leak into another page.
   ============================================================================ */

/* The banner avatar carries an icon injected with v-html, so it is reached through :deep(). */
.dash-avatar :deep(svg) {
  width: 26px;
  height: 26px;
  stroke-width: 1.8;
}

/* ===== Section links (the "View All" controls) ===== */
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

.section-link:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.section-link-arrow {
  font-size: 0.875rem;
  line-height: 1;
}

/* The notifications card keeps its action on its own row, at the start. */
.notif-view-all {
  align-self: flex-start;
}

/* ===== Status pills =====
   These tones extend the shared .end-badge (is-pending / is-approved / is-rejected) with
   the pastel tones this workflow also needs. */
.end-badge.is-info {
  background: #dbeafe;
  color: #1d4ed8;
}

.end-badge.is-progress {
  background: #ede9fe;
  color: #6d28d9;
}

.end-badge.is-neutral {
  background: #eef1f7;
  color: #475569;
}

/* ===== Endorsed proposals table =====
   The shared .end-table is used as it is; only the header band, the row alignment and the
   compact row action are set here so the table lines up with the button beside it. */
.ovc-table th {
  background: #f6f7fd;
  font-weight: 700;
  white-space: nowrap;
}

.ovc-table td {
  vertical-align: middle;
}

.ovc-table tbody tr:hover td {
  background: #fafbff;
}

.ovc-table th.text-right,
.ovc-table td.text-right {
  text-align: right;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

.cell-id {
  font-size: 0.8125rem;
  color: #5b6484;
  white-space: nowrap;
}

.ovc-empty {
  padding: 24px 12px;
  text-align: center;
  color: #78809a;
}

/* A table row action is smaller than a page action. */
.row-btn {
  padding: 7px 14px;
  font-size: 0.8125rem;
}

/* ===== Small screens ===== */
@media (max-width: 560px) {
  .section-link {
    margin-left: 0;
  }
}
</style>
