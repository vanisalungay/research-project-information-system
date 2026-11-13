<template>
  <!--
    The Chancellor (Office of the Chancellor) dashboard.

    Only the presentation changed: the three counts and the two proposal lists are the same
    records, read from the same endpoints with the same status filters and bound to the same
    fields. The page reuses the shared design layer (assets/dashboard.css and
    assets/endorsement.css) - the same banner, cards, tables and pills the other role
    dashboards use - so every dashboard belongs to one design system.
  -->
  <div class="end-page end-page--fluid dash-page">
    <!-- ===================== HEADER ===================== -->
    <section class="dash-banner">
      <span class="dash-avatar" aria-hidden="true" v-html="icons.chancellor"></span>
      <div class="dash-banner-text">
        <h2 class="dash-banner-title">CHANCELLOR DASHBOARD</h2>
        <p class="dash-banner-sub">
          Office of the Chancellor — Final Approval, Budget Determination &amp; Special Orders (SO)
        </p>
      </div>
    </section>

    <!-- ===================== STATISTICS ===================== -->
    <section class="dash-stats">
      <div class="dash-stat dash-stat--amber">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.clock"></span>
          <span class="dash-stat-label">Pending Final Approval</span>
        </div>
        <div class="dash-stat-value">{{ stats.pending }}</div>
      </div>

      <div class="dash-stat dash-stat--green">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.check"></span>
          <span class="dash-stat-label">Approved / Released</span>
        </div>
        <div class="dash-stat-value">{{ stats.approved }}</div>
      </div>

      <div class="dash-stat dash-stat--blue">
        <div class="dash-stat-head">
          <span class="dash-stat-icon" v-html="icons.send"></span>
          <span class="dash-stat-label">Sent to OVCAF (Budget)</span>
        </div>
        <div class="dash-stat-value">{{ stats.sentToOvcaf }}</div>
      </div>
    </section>

    <!-- ===================== LOADING ===================== -->
    <section v-if="isLoading" class="dash-section">
      <div class="loading-state">
        <span class="loading-spinner"></span>
        <span>Loading proposals...</span>
      </div>
    </section>

    <template v-else>
      <!-- ===================== PROPOSALS FOR FINAL APPROVAL ===================== -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.proposals"></span>
          <h3 class="dash-section-title">Proposals for Final Approval</h3>
        </header>

        <div v-if="forFinalApproval.length > 0" class="end-table-scroll">
          <table class="end-table oc-table">
            <thead>
              <tr>
                <th>Proposal Title</th>
                <th>Proponent</th>
                <th>Date Endorsed</th>
                <th>Status</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in forFinalApproval" :key="p.id">
                <td class="cell-title">{{ p.title }}</td>
                <td>{{ p.adviser }}</td>
                <td class="cell-id">{{ p.date }}</td>
                <td>
                  <span class="end-badge" :class="statusTone(p.status)">{{ proposalStatusLabel(p.status) }}</span>
                  <!-- An SO form saved without finalizing the approval. -->
                  <span v-if="isSoDraft(p)" class="end-badge is-draft draft-badge">SO Draft</span>
                </td>
                <td class="text-right">
                  <div class="row-actions">
                    <button class="end-btn is-view row-btn" @click="reviewProposal(p.id)">
                      Review &amp; Approve
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

        <p v-else class="dash-empty">No proposals currently awaiting final executive sign-off.</p>
      </section>

      <!-- ===================== APPROVED & BUDGET-FORWARDED ===================== -->
      <section class="dash-section">
        <header class="dash-section-head">
          <span class="dash-section-icon" v-html="icons.funds"></span>
          <h3 class="dash-section-title">Approved &amp; Budget-Forwarded Proposals</h3>
        </header>

        <div v-if="approvedProposals.length > 0" class="end-table-scroll">
          <table class="end-table oc-table">
            <thead>
              <tr>
                <th>Proposal Title</th>
                <th>Proponent</th>
                <th>Date</th>
                <th>Status</th>
                <th class="text-right">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="p in approvedProposals" :key="p.id">
                <td class="cell-title">{{ p.title }}</td>
                <td>{{ p.adviser }}</td>
                <td class="cell-id">{{ p.date }}</td>
                <td>
                  <span class="end-badge" :class="statusTone(p.status)">{{ proposalStatusLabel(p.status) }}</span>
                </td>
                <td class="text-right">
                  <div class="row-actions">
                    <button class="end-btn is-view row-btn" @click="reviewProposal(p.id)">{{ proposalActionLabel(p.status) }}</button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

        <p v-else class="dash-empty">
          Approved proposals and those forwarded to Finance/OVCAF for budget processing will appear here.
        </p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import { proposalActionLabel, proposalStatusLabel } from '@/utils/proposalStatus.js'
import { isSoDraft } from '@/utils/soDraft.js'

const router = useRouter()

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment. The map
 * is presentation only - no count or proposal depends on it.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  chancellor: svg(
    '<path d="M3 21h18"/><path d="M5 21V9l7-4 7 4v12"/>' +
      '<path d="M9 21v-4h6v4"/><path d="M10 12h4"/>',
  ),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  check: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  send: svg('<path d="M22 2 11 13"/><path d="M22 2 15 22l-4-9-9-4z"/>'),
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

const stats = ref({ pending: 0, approved: 0, sentToOvcaf: 0 })
const forFinalApproval = ref([])
const approvedProposals = ref([])
const isLoading = ref(true)

/** The fields the draft badge and the document generator read from a list row. */
const soFields = (p) => ({
  soNumber: p.soNumber,
  soFileName: p.soFileName,
  soFilePath: p.soFilePath,
  soUploadedAt: p.soUploadedAt,
  needsBudget: p.needsBudget,
})

const loadDashboard = async () => {
  try {
    isLoading.value = true

    // Only proposals that are actually awaiting the Chancellor can be acted on.
    // REC_APPROVED and OVC_APPROVED belong to OVCRIGE's queue - listing them here
    // produced rows whose action buttons could only fail.
    const [pendingRes, approvedRes] = await Promise.all([
      api.get('/api/proposals?statusIn=FOR_OC_APPROVAL'),
      api.get('/api/proposals?statusIn=FOR_OVCAF_APPROVAL&statusIn=APPROVED&statusIn=READY_FOR_RELEASE&statusIn=RELEASED'),
    ])

    const pendingData = Array.isArray(pendingRes.data) ? pendingRes.data : []
    const approvedData = Array.isArray(approvedRes.data) ? approvedRes.data : []

    stats.value.pending = pendingData.length
    stats.value.sentToOvcaf = approvedData.filter(p => p.status === 'FOR_OVCAF_APPROVAL').length
    stats.value.approved = approvedData.length

    forFinalApproval.value = pendingData.map(p => ({
      id: p.id,
      title: p.projectTitle || 'Untitled Proposal',
      adviser: p.projectLeader || p.proponent?.name || 'Unknown',
      date: p.updatedAt?.substring(0, 10) || p.createdAt?.substring(0, 10) || 'N/A',
      status: p.status,
      ...soFields(p),
    }))

    approvedProposals.value = approvedData.map(p => ({
      id: p.id,
      title: p.projectTitle || 'Untitled Proposal',
      adviser: p.projectLeader || p.proponent?.name || 'Unknown',
      date: p.updatedAt?.substring(0, 10) || p.createdAt?.substring(0, 10) || 'N/A',
      status: p.status,
      badgeClass: p.status === 'FOR_OVCAF_APPROVAL' ? 'budget' : 'approved',
      ...soFields(p),
    }))
  } catch (error) {
    console.error('Failed to load Chancellor dashboard:', error)
  } finally {
    isLoading.value = false
  }
}

const reviewProposal = (id) => {
  router.push(`/oc/final-approval/${id}`)
}

onMounted(loadDashboard)
</script>

<style scoped>
/* ============================================================================
   Chancellor dashboard presentation.

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
.oc-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  font-weight: 700;
  white-space: nowrap;
}

.oc-table td {
  vertical-align: middle;
}

.oc-table tbody tr:hover td {
  background: #fafbff;
}

.oc-table th.text-right,
.oc-table td.text-right {
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

/* A table row action is smaller than a page action. */
.row-btn {
  padding: 7px 14px;
  font-size: 0.8125rem;
}

/* The two row actions sit side by side and wrap on a narrow screen. */
.row-actions {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}

/* A draft badge sits beside the status pill. */
.draft-badge {
  margin-left: 8px;
}

/* A failed download is reported once, above the tables. */
.dash-error {
  margin: 0 0 16px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 0.875rem;
  font-weight: 500;
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