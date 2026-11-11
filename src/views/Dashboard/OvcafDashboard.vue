<template>
  <!--
    The OVCAF (Office of the Vice Chancellor for Admin and Finance) dashboard.

    Only the presentation changed: the three counts and the two proposal lists are the same
    records, filtered from the same endpoint by the same statuses and bound to the same
    fields. The page reuses the shared design layer (assets/dashboard.css and
    assets/endorsement.css) - the same banner, cards, tables and pills the other role
    dashboards use - so every dashboard belongs to one design system.
  -->
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== HEADER ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.office"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">OVCAF DASHBOARD</h2>
        <p class="dash-banner-sub">Office of the Vice Chancellor for Admin and Finance</p>
      </div>
    </section>

    <!-- ===================== STATISTICS ===================== -->
    <section class="dash-stats">
      <div class="dash-stat dash-stat--amber">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.clock"></span>
          <span class="dash-stat-label">Proposals for Review</span>
        </div>
        <div class="dash-stat-value">{{ stats.forReview }}</div>
      </div>

      <div class="dash-stat dash-stat--blue">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.send"></span>
          <span class="dash-stat-label">Endorsed to OC</span>
        </div>
        <div class="dash-stat-value">{{ stats.endorsedToOc }}</div>
      </div>

      <div class="dash-stat dash-stat--green">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.check"></span>
          <span class="dash-stat-label">Approved / Released</span>
        </div>
        <div class="dash-stat-value">{{ stats.completed }}</div>
      </div>
    </section>

    <!-- ===================== PROPOSALS AWAITING OVCAF REVIEW ===================== -->
    <section class="dash-section">
      <header class="dash-section-head">
        <span class="dash-section-icon" v-html="icons.proposals"></span>
        <h3 class="dash-section-title">PROPOSALS AWAITING OVCAF REVIEW</h3>
      </header>

      <div v-if="isLoading" class="loading-state">
        <span class="loading-spinner"></span>
        <span>Loading proposals...</span>
      </div>

      <div v-else-if="forReview.length > 0" class="end-table-scroll">
        <table class="end-table ovcaf-table">
          <thead>
            <tr>
              <th>Proposal Title</th>
              <th>Project Leader</th>
              <th>Status</th>
              <th class="text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposal in forReview" :key="proposal.id">
              <td class="cell-title">{{ proposal.title }}</td>
              <td>{{ proposal.proponent }}</td>
              <td>
                <span class="end-badge" :class="statusTone(proposal.status)">
                  {{ proposalStatusLabel(proposal.status) }}
                </span>
              </td>
              <td class="text-right">
                <button class="end-btn is-view row-btn" @click="$router.push(`/ovcaf/validate/${proposal.id}`)">
                  Review
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-else class="dash-empty">No proposals awaiting OVCAF review.</p>
    </section>

    <!-- ===================== APPROVED PROPOSALS REQUESTING FUNDS ===================== -->
    <section class="dash-section">
      <header class="dash-section-head">
        <span class="dash-section-icon" v-html="icons.funds"></span>
        <h3 class="dash-section-title">APPROVED PROPOSALS REQUESTING FUNDS</h3>
      </header>

      <div v-if="completed.length > 0" class="end-table-scroll">
        <table class="end-table ovcaf-table">
          <thead>
            <tr>
              <th>Proposal Title</th>
              <th>Project Leader</th>
              <th>Status</th>
              <th class="text-right">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposal in completed" :key="proposal.id">
              <td class="cell-title">{{ proposal.title }}</td>
              <td>{{ proposal.proponent }}</td>
              <td>
                <span class="end-badge" :class="statusTone(proposal.status)">
                  {{ proposalStatusLabel(proposal.status) }}
                </span>
              </td>
              <td class="text-right">
                <button class="end-btn is-view row-btn" @click="$router.push(`/ovcaf/validate/${proposal.id}`)">
                  {{ proposalActionLabel(proposal.status) }}
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <p v-else class="dash-empty">No approved proposals requesting funding.</p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import { proposalActionLabel, proposalStatusLabel } from '@/utils/proposalStatus.js'

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment. The map
 * is presentation only - no count or proposal depends on it.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  office: svg(
    '<path d="M3 21h18"/><path d="M5 21V7l7-4 7 4v14"/>' +
      '<path d="M9 21v-5h6v5"/><path d="M9 10h.01M15 10h.01M9 13h.01M15 13h.01"/>',
  ),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  send: svg('<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4z"/>'),
  check: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  proposals: svg(
    '<path d="M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z"/>' +
      '<path d="M14 3v5h5"/><path d="M9 13h6M9 17h4"/>',
  ),
  funds: svg(
    '<circle cx="12" cy="12" r="9"/><path d="M12 7v10"/>' +
      '<path d="M14.5 9.5a2.5 2.5 0 0 0-2.5-2h-1a2 2 0 0 0 0 4h2a2 2 0 0 1 0 4h-1a2.5 2.5 0 0 1-2.5-2"/>',
  ),
}

/**
 * The status pill tone comes from the one shared mapping (utils/statusTone.js); the status
 * text shown stays exactly what the record carries.
 */

const forReview = ref([])
const completed = ref([])
const stats = ref({
  forReview: 0,
  endorsedToOc: 0,
  completed: 0
})
const isLoading = ref(true)

/**
 * A proposal is OVCAF's only after the Office of the Chancellor recorded its final
 * approval and the issued Special Order. The status alone is not evidence (a record
 * can carry an APPROVED-looking status without any OC decision); the SO number and
 * the time it was recorded are written together by the OC final-approval
 * transaction, and the backend applies the same rule to the /api/ovcaf endpoints.
 */
const hasOcApproval = (proposal) =>
  typeof proposal?.soNumber === 'string' &&
  proposal.soNumber.trim() !== '' &&
  proposal.soUploadedAt != null

const loadDashboard = async () => {
  try {
    isLoading.value = true
    const response = await api.get('/api/proposals')
    const data = (response.data || []).filter(hasOcApproval)

    // Proposals awaiting OVCAF budget evaluation (OC has approved and forwarded them).
    stats.value.forReview = data.filter(p => p.status === 'FOR_OVCAF_APPROVAL').length

    // Proposals OVCAF has returned to OC / are awaiting the Chancellor's next step.
    stats.value.endorsedToOc = data.filter(p => p.status === 'FOR_OC_APPROVAL').length

    // Completed (approved, released)
    stats.value.completed = data.filter(
      p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE' || p.status === 'RELEASED'
    ).length

    // Only proposals with a completed OC approval + SO that still await OVCAF's
    // budget decision can be reviewed. Earlier-stage records (REC_APPROVED,
    // FOR_OC_APPROVAL) are not yet OVCAF's to act on.
    forReview.value = data
      .filter(p => p.status === 'FOR_OVCAF_APPROVAL')
      .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Project',
        proponent: p.projectLeader || 'Unknown Proponent',
        status: p.status
      }))

    // Approved/funding proposals
    completed.value = data
      .filter(p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE' || p.status === 'RELEASED')
      .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Project',
        proponent: p.projectLeader || 'Unknown Proponent',
        status: p.status
      }))

  } catch (error) {
    console.error('Failed to load OVCAF metrics:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
/* ============================================================================
   OVCAF dashboard presentation.

   The page reuses the shared design layer: assets/dashboard.css provides the welcome
   banner, the statistic cards and the section cards, and assets/endorsement.css provides
   the table, the status pills and the buttons. Only what is specific to this screen is
   declared here, so nothing can leak into another page.
   ============================================================================ */

/* The banner avatar carries an icon injected with v-html, so it is reached through :deep(). */
.dash-avatar :deep(svg) {
  width: 26px;
  height: 26px;
  stroke-width: 1.8;
}

/* ===== Status pills =====
   The tone colours are shared (assets/status.css); this page adds no palette of its own. */

/* ===== Tables =====
   The shared .end-table is used as it is; only the header band, the row alignment and the
   compact row action are set here so the table lines up with the button beside it. */
.ovcaf-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  font-weight: 700;
  white-space: nowrap;
}

.ovcaf-table td {
  vertical-align: middle;
}

.ovcaf-table tbody tr:hover td {
  background: #fafbff;
}

.ovcaf-table th.text-right,
.ovcaf-table td.text-right {
  text-align: right;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

/* A table row action is smaller than a page action. */
.row-btn {
  padding: 7px 14px;
  font-size: 0.8125rem;
}

/* ===== Loading ===== */
.loading-state {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 4px;
  color: #64748b;
  font-size: 0.875rem;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(49, 91, 255, 0.25);
  border-top-color: #315bff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
