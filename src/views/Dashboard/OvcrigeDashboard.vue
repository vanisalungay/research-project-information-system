<template>
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== HEADER ===================== -->
    <section class="dash-banner ovc-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.office"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">OVCRIGE Dashboard</h2>
        <p class="dash-banner-sub">
          Office of the Vice Chancellor for Research, Innovation and Global Engagement
        </p>
      </div>
    </section>

    <!-- ===================== STATISTICS ===================== -->
    <section class="dash-stats ovc-stats">
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
    <div class="dash-row dash-row--main ovc-main">
      <!-- Latest Endorsed Proposals -->
      <section class="dash-section ovc-panel ovc-proposals">
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

        <div
          class="end-table-scroll ovc-table-scroll"
          role="region"
          aria-label="Latest endorsed proposals"
          tabindex="0"
        >
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

              <tr v-else-if="latestEndorsed.length === 0">
                <td colspan="6" class="ovc-empty">
                  No proposals found.
                </td>
              </tr>

              <tr v-for="proposal in latestEndorsed" :key="proposal.id">
                <td class="cell-id">
                  {{ proposal.documentId || proposal.proposalCode || '—' }}
                </td>
                <td class="cell-title">
                  <span class="ovc-title-text" :title="proposal.projectTitle">{{ proposal.projectTitle }}</span>
                </td>
                <td class="cell-proponent">
                  {{ proposal.projectLeader || proposal.proponent?.name || '-' }}
                </td>
                <td class="cell-date">
                  {{ proposalSubmittedAt(proposal)?.substring(0, 10) || '—' }}
                </td>
                <td class="cell-status">
                  <span class="end-badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) }}</span>
                </td>
                <td class="text-right">
                  <button class="end-btn is-view row-btn" @click="goToReview(proposal.id)">REVIEW</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- Notifications -->
      <section class="dash-section ovc-panel ovc-notifications-panel">
        <NotificationPreview
          compact
          title="Notifications"
          :limit="5"
          view-all-to="/notifications"
          empty-text="No notifications."
        />
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { latestProposalSnippets } from '@/utils/dashboardProposals'
import {
  OVCRIGE_DASHBOARD_FETCH_STATUSES,
  isOvcrigePipelineProposal,
  proposalSubmittedAt,
} from '@/utils/ovcrigeProposals'
import NotificationPreview from '@/components/NotificationPreview.vue'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

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
 * The status pill tone comes from the one shared mapping (utils/statusTone.js); the status
 * text shown stays exactly what the record carries.
 */

const router = useRouter()

// Dashboard data (will come from backend)
const stats = ref({
  endorsed: 0,
  underReview: 0,
  recApproved: 0,
  revision: 0
})

const proposals = ref([])

/**
 * The preview shows the five latest proposals that are in OVCRIGE's pipeline, newest
 * submission first. The full array remains the source of the statistic cards; this only
 * removes proposals that have not been endorsed to OVCRIGE (and finished projects) from
 * the table, and shortens it to five.
 */
const latestEndorsed = computed(() =>
  latestProposalSnippets(
    proposals.value.filter(isOvcrigePipelineProposal),
    { dateField: 'submittedAt', fallbackDateFields: ['createdAt'] },
  ),
)

const loading = ref(true)

const loadDashboard = async () => {
  try {
    // Fetch proposals relevant to the OVCRIGE workflow. Ordering is done by the server
    // on the real submission event (submitted_at), so a record's last edit or the order
    // the rows happen to be stored in can never decide what appears first. The returned
    // statuses are included only because the "For Revision" card counts them; the table
    // itself is filtered to the pipeline below.
    const proposalResponse = await api.get('/api/proposals', {
      params: {
        statusIn: [...OVCRIGE_DASHBOARD_FETCH_STATUSES],
        sort: 'submittedAt'
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

  } catch (error) {
    console.log(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})

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
  width: 24px;
  height: 24px;
  stroke-width: 1.8;
}

/* ===== Banner =====
   A compact content-area heading: the shared branded gradient and gold hairline stay,
   with a tighter vertical rhythm so it reads as a section heading, not a hero block. */
.dash-banner.ovc-banner {
  padding: 18px 22px;
  gap: 16px;
  border-radius: 14px;
}

.ovc-banner .dash-avatar {
  width: 46px;
  height: 46px;
  border-radius: 12px;
}

.ovc-banner .dash-banner-title {
  font-size: 1.375rem;
  line-height: 1.2;
  margin-bottom: 3px;
}

.ovc-banner .dash-banner-sub {
  font-size: 0.8375rem;
  line-height: 1.45;
  max-width: 72ch;
}

/* ===== Statistic cards =====
   Four in one row on wide screens, two-up then one-up as space tightens. The count is the
   dominant element; the icon sits in a small tinted container beside the label. */
.dash-stats.ovc-stats {
  gap: 14px;
}

.dash-stat {
  min-height: 104px;
  padding: 16px;
  gap: 10px;
  justify-content: space-between;
  border-radius: 12px;
}

.dash-stat:hover {
  border-color: #d5dcf2;
}

.dash-stat .dash-stat-icon {
  width: 32px;
  height: 32px;
  border-radius: 9px;
}

.dash-stat .dash-stat-icon :deep(svg) {
  width: 17px;
  height: 17px;
}

.dash-stat .dash-stat-label {
  font-size: 0.75rem;
  line-height: 1.35;
}

.dash-stat .dash-stat-value {
  font-size: 2rem;
  line-height: 1.05;
  letter-spacing: -0.01em;
}

@media (min-width: 1024px) {
  .ovc-stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (max-width: 1023px) {
  .ovc-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .ovc-stats {
    grid-template-columns: minmax(0, 1fr);
  }
}

/* ===== Panels =====
   The proposal panel gets the larger share because the table needs horizontal room; the
   notification panel gets the remaining third. Both reuse the shared .dash-section shell
   (background, border, shadow and dark theme) and only tighten its spacing. */
.dash-row--main.ovc-main {
  align-items: stretch;
}

.dash-section.ovc-panel {
  padding: 18px;
  gap: 14px;
}

.ovc-proposals,
.ovc-notifications-panel {
  min-width: 0;
}

/* Keep the table's own scrollbar from forcing the panel (and the page) wider. */
.ovc-table-scroll {
  min-width: 0;
  max-width: 100%;
}

@media (max-width: 1599px) {
  .ovc-main {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (min-width: 1600px) {
  .ovc-main {
    grid-template-columns: minmax(0, 2fr) minmax(0, 1fr);
  }
}

/* ===== Section links (the "View All" controls) ===== */
.section-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 8px;
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

/* ===== Endorsed proposals table =====
   Fixed column widths keep the title readable and stop the status/action columns from
   being squeezed. When the panel is narrower than the 880px six-column minimum, the table
   scrolls inside .end-table-scroll; the page itself never scrolls sideways. */
.end-page--fluid .ovc-table {
  width: 100%;
  min-width: 800px;
  table-layout: fixed;
}

.ovc-table {
  font-size: 0.8125rem;
}

.ovc-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  padding: 10px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  white-space: nowrap;
}

.ovc-table th:nth-child(1) {
  width: 17%;
}

.ovc-table th:nth-child(2) {
  width: 24%;
}

.ovc-table th:nth-child(3) {
  width: 14%;
}

.ovc-table th:nth-child(4) {
  width: 13%;
}

.ovc-table th:nth-child(5) {
  width: 21%;
}

.ovc-table th:nth-child(6) {
  width: 11%;
}

.ovc-table td {
  padding: 9px 10px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
}

.ovc-table tbody tr:hover td {
  background: #fafbff;
}

.ovc-table th.text-right,
.ovc-table td.text-right {
  text-align: right;
}

.cell-id {
  font-size: 0.6875rem;
  line-height: 1.35;
  color: #5b6484;
}

/* Long formatted ids break at their hyphens instead of widening the column. */
.ovc-table td:nth-child(1) {
  white-space: normal;
  overflow-wrap: anywhere;
}

/* The submitted date is one token and never wraps. */
.ovc-table td:nth-child(4) {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

/* At most two lines per title; the full text stays in the DOM and in the title tooltip. */
.ovc-title-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  line-height: 1.35;
}

.cell-proponent {
  font-size: 0.78rem;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.ovc-table .end-badge {
  padding: 2px 7px;
  font-size: 0.656rem;
}

.ovc-empty {
  padding: 24px 12px;
  text-align: center;
  color: #78809a;
}

/* A table row action is smaller than a page action and never wraps. */
.row-btn {
  padding: 6px 10px;
  border-radius: 8px;
  font-size: 0.75rem;
  white-space: nowrap;
}

/* ===== Small screens ===== */
@media (max-width: 720px) {
  .dash-banner.ovc-banner {
    padding: 16px 18px;
    gap: 14px;
  }

  .ovc-banner .dash-avatar {
    width: 42px;
    height: 42px;
  }

  .ovc-banner .dash-banner-title {
    font-size: 1.2rem;
  }
}

@media (max-width: 560px) {
  .section-link {
    margin-left: 0;
  }
}
</style>
