<template>
  <!--
    The REC (Research Evaluation Committee) overview dashboard.

    Only the presentation changed here: the statistics, the latest assigned proposals and the
    notifications are the same records, read from the same endpoints and bound to the same
    computed values. The page reuses the shared design layer (assets/dashboard.css and
    assets/endorsement.css) - the same cards, pills, tables and spacing the Proponent and RPS
    dashboards already use - so all three screens belong to one design system.
  -->
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== HEADER ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.committee"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">REC Dashboard</h2>
        <p class="dash-banner-sub">Research Evaluation Committee</p>
      </div>
    </section>

    <!-- ===================== STATISTICS ===================== -->
    <section class="dash-stats">
      <div class="dash-stat dash-stat--indigo">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.proposals"></span>
          <span class="dash-stat-label">Submitted Proposals</span>
        </div>
        <div class="dash-stat-value">{{ totalAssigned }}</div>
      </div>

      <div class="dash-stat dash-stat--green">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.check"></span>
          <span class="dash-stat-label">Completed Reviews</span>
        </div>
        <div class="dash-stat-value">{{ completedReviews }}</div>
      </div>

      <div class="dash-stat dash-stat--amber">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.clock"></span>
          <span class="dash-stat-label">Pending Evaluation</span>
        </div>
        <div class="dash-stat-value">{{ pendingEvaluation }}</div>
      </div>
    </section>

    <!-- ===================== PROPOSALS + NOTIFICATIONS ===================== -->
    <div class="dash-row dash-row--main">
      <!-- Latest Assigned Proposals -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.proposals"></span>
          <h3 class="dash-section-title">Latest Assigned Proposals</h3>
          <div class="dash-section-actions">
            <button class="section-link" @click="goToAssigned">
              <span>View All</span>
              <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
            </button>
          </div>
        </header>

        <div class="end-table-scroll">
          <table class="end-table rec-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Proponent</th>
                <th>Date Submitted</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>

            <tbody v-if="latestProposals.length">
              <tr
                v-for="proposal in latestProposals"
                :key="proposal.id"
              >
                <td class="cell-title">{{ proposal.projectTitle }}</td>

                <td>{{ proposal.proponent?.name }}</td>

                <td class="cell-id">{{ proposal.createdAt?.substring(0, 10) }}</td>

                <td>
                  <span class="end-badge" :class="statusTone(proposal.status)">
                    {{ proposal.status }}
                  </span>
                </td>

                <td class="text-right">
                  <button
                    v-if="proposal.status === 'UNDER_REVIEW'"
                    class="end-btn is-primary row-btn"
                    @click="goToReview(proposal.id)"
                  >
                    Review
                  </button>

                  <button
                    v-else
                    class="end-btn row-btn"
                    @click="goToMeeting(proposal.id)"
                  >
                    View
                  </button>
                </td>
              </tr>
            </tbody>

            <tbody v-else>
              <tr>
                <td colspan="5" class="rec-empty">
                  No submitted proposals.
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

        <ul v-if="notifications.length" class="dash-notes">
          <li
            v-for="notification in notifications.slice(0,5)"
            :key="notification.id"
            class="dash-note dash-note--clickable"
            @click="openNotification(notification)"
          >
            <span class="dash-note-icon" v-html="icons.bell"></span>
            <div class="dash-note-body">
              <p class="dash-note-text">{{ notification.message }}</p>
              <div class="dash-note-foot">
                <span class="dash-note-date">
                  {{ formatDate(notification.createdAt) }}
                </span>
              </div>
            </div>
          </li>
        </ul>

        <p v-else class="dash-empty">No notifications.</p>

        <button class="section-link notif-view-all" @click="goToNotifications">
          <span>View All</span>
          <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
        </button>
      </aside>
    </div>
  </div>
</template>

<script setup>
import api from '@/utils/api'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
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
  committee: svg(
    '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>' +
      '<path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  ),
  proposals: svg(
    '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/>' +
      '<path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>',
  ),
  check: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  bell: svg(
    '<path d="M18 8a6 6 0 1 0-12 0c0 7-3 9-3 9h18s-3-2-3-9"/>' +
      '<path d="M13.7 21a2 2 0 0 1-3.4 0"/>',
  ),
}

/**
 * The pastel tone of a status pill. The status text shown stays exactly what the record
 * carries; only the colour is chosen here - amber while under review, green when approved,
 * red when rejected.
 */
const statusTone = (status) => {
  if (status === 'UNDER_REVIEW') return 'is-pending'
  if (status === 'REC_REJECTED') return 'is-rejected'
  return 'is-approved'
}

const router = useRouter()

const proposals = ref([])
const notifications = ref([])

const latestProposals = computed(() =>
  [...proposals.value]
    .sort((a, b) => b.id - a.id)
    .slice(0, 5)
)

const totalAssigned = computed(() =>
  proposals.value.length
)

const completedReviews = computed(() =>
  proposals.value.filter(
    p =>
      p.status === 'REC_APPROVED' ||
      p.status === 'ENDORSED'
  ).length
)

const pendingEvaluation = computed(() =>
  proposals.value.filter(
    p =>
      p.status === 'UNDER_REVIEW'
  ).length
)

const loadDashboard = async () => {
  try {
    const proposalRes = await api.get('/api/proposals')
    const allProposals = proposalRes.data || []

    // Get current user
    const stored = localStorage.getItem('user_data')
    if (stored) {
      const user = JSON.parse(atob(stored))

      // Filter proposals assigned to this REC user
      // Assuming proposals have an 'assignedTo' field or similar
      // For now, show all UNDER_REVIEW proposals
      proposals.value = allProposals.filter(p =>
        p.status === 'UNDER_REVIEW' ||
        p.status === 'REC_APPROVED' ||
        p.status === 'REC_REJECTED'
      )

      // Load notifications
      const notifRes = await api.get(`/api/notifications?userId=${user.id}`)
      notifications.value = notifRes.data || []
    }
  } catch (err) {
    console.error(err)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(loadDashboard)

const goToNotifications = () => {
  router.push('/rec-notif')
}

/**
 * Where a notification opens: the exact proposal it carries, on this role's proposal page
 * (/rec-prop?id=...). The id is the saved record reference - never the notification title -
 * and a notification with no reference has no destination.
 */
const notificationDestination = (notification) =>
  proposalNotificationDestination(notification, UserRole.REC)

/** Opens the proposal a notification is about. Navigation only - the read state is untouched. */
const openNotification = (notification) => {
  const destination = notificationDestination(notification)
  if (destination) router.push(destination)
}

const goToAssigned = () => {
  router.push('/assigned-proposals')
}

const goToReview = (id) => {
  router.push({ name: 'RecReviewForm', params: { id } })
}

const goToMeeting = (id) => {
  router.push({ name: 'RecMeetingDetails', params: { id } })
}
</script>

<style scoped>
/* ============================================================================
   REC dashboard presentation.

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

/* ===== Proposals table =====
   The shared .end-table is used as it is; only the header band, the row alignment and the
   compact row action are set here so the table lines up with the button beside it. */
.rec-table th {
  background: #f6f7fd;
  font-weight: 700;
  white-space: nowrap;
}

.rec-table td {
  vertical-align: middle;
}

.rec-table tbody tr:hover td {
  background: #fafbff;
}

.rec-table th.text-right,
.rec-table td.text-right {
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

.rec-empty {
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