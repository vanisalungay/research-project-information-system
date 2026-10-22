<template>
  <div class="qpr-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← Back</button>
      <div>
        <h1>Quarterly Progress Reports</h1>
        <p class="subtitle">{{ project?.projectTitle || 'Project' }}</p>
      </div>
      <button class="btn-primary" :disabled="!eligible || allStagesComplete" @click="createNew">
        {{ nextStageAction }}
      </button>
    </div>

    <p v-if="actionError" class="action-error">{{ actionError }}</p>

    <div v-if="loading" class="state">Loading reports...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="!eligible" class="state notice">
      Quarterly progress reports are only available for projects that are approved for implementation
      and have an issued Special Order (SO).
    </div>
    <template v-else>
      <!-- Reporting stage tracker: stages 1-3 (QPR + Project Workplan) then the
           Terminal Report stage. -->
      <div class="stages-card">
        <div class="stages-header">
          <span class="stages-title">Reporting Progress</span>
          <span v-if="allStagesComplete" class="stages-done">All required reports submitted</span>
          <span v-else class="stages-next">
            Next required stage: <strong>{{ nextStageLabel }}</strong>
          </span>
        </div>
        <div class="stage-chips">
          <span v-for="s in stageSummary" :key="s.key" class="stage-chip" :class="s.state">
            {{ s.label }}
          </span>
        </div>
      </div>

      <div v-if="reports.length === 0" class="state">
        No reports submitted yet. Click the button above to start the first reporting stage.
      </div>
      <table v-else class="report-table">
        <thead>
          <tr>
            <th scope="col" class="cell-report">Report</th>
            <th scope="col" class="cell-date">Date Submitted</th>
            <th scope="col" class="cell-status">Status</th>
            <th scope="col" class="cell-actions">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in reports" :key="r.id">
            <td class="cell-report">
              <div class="report-info">
                <span class="report-title">{{ reportTypeLabel(r) }}</span>
                <span v-if="periodLabel(r)" class="report-period">{{ periodLabel(r) }}</span>
              </div>
            </td>
            <td class="cell-date">
              <span class="report-date">{{ formatDate(r.submittedAt) || '—' }}</span>
            </td>
            <td class="cell-status">
              <div class="report-status">
                <span class="status-badge" :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'">{{ r.status }}</span>
              </div>
            </td>
            <td class="cell-actions">
              <div class="report-actions">
                <template v-if="isTerminal(r)">
                  <button
                    v-if="r.terminalReportFileName"
                    class="btn-view"
                    @click="openTerminalReport(r)"
                  >
                    View
                  </button>
                  <span v-else class="report-meta">No terminal report file</span>
                </template>
                <template v-else>
                  <button class="btn-view" @click="view(r)">View</button>
                  <!-- A legacy uploaded workplan file (from before the interactive
                       workplan form) stays downloadable. -->
                  <button v-if="r.workplanFileName" class="btn-download" @click="downloadDoc(r, 'workplan')">Download workplan file</button>
                </template>
                <button v-if="r.status === 'DRAFT'" class="btn-edit" @click="edit(r)">Edit</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </template>

    <!-- Read-only view modal: the report content scrolls inside the modal while
         its header and the page navigation footer stay in place. -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal modal--viewer">
        <div class="modal-header">
          <h2>Quarterly Progress Report</h2>
          <button class="close-btn" @click="viewing = null">✕</button>
        </div>
        <div class="modal-body">
          <QuarterlyReportViewer :report="viewing" @close="viewing = null" />
        </div>
      </div>
    </div>

    <!-- Terminal Report preview modal (shared component): shows the actual
         uploaded file through the existing authorized document endpoint, with a
         Download action. Opening it never downloads, changes or deletes the
         report. -->
    <TerminalReportPreviewModal
      v-if="terminalModal"
      :report-id="terminalModal.reportId"
      :file-name="terminalModal.fileName"
      :period-label="terminalModal.periodLabel"
      @close="closeTerminalModal"
    />

  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import { resetScrollPosition } from '@/utils/scrollReset'
import QuarterlyReportViewer from '@/components/QuarterlyReportViewer.vue'
import TerminalReportPreviewModal from '@/components/TerminalReportPreviewModal.vue'
import { downloadReportDocument } from '@/utils/fileDownload'
import { isQuarterlyReportEligible } from '@/utils/quarterlyReport'

const route = useRoute()
const router = useRouter()
const proposalId = computed(() => route.params.proposalId)

const project = ref(null)
const reports = ref([])
const nextStage = ref(null)
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const viewing = ref(null)
const terminalModal = ref(null)

// Quarterly reporting is only available to approved, SO-issued projects - the
// same rule the backend enforces (utils/quarterlyReport.js).
const eligible = computed(() => isQuarterlyReportEligible(project.value))

const allStagesComplete = computed(() => !!nextStage.value?.allStagesComplete)

const nextStageLabel = computed(() => {
  if (!nextStage.value || nextStage.value.allStagesComplete) return ''
  return nextStage.value.reportType === 'TERMINAL_REPORT'
    ? 'Terminal Report'
    : `Quarterly Report ${nextStage.value.period} + Project Workplan`
})

const nextStageAction = computed(() => {
  if (allStagesComplete.value) return 'All Reports Submitted'
  const label = nextStage.value?.reportType === 'TERMINAL_REPORT'
    ? 'Terminal Report'
    : `Quarterly Report ${nextStage.value?.period || ''}`
  return nextStage.value?.draftReportId ? `Continue ${label}` : `+ Start ${label}`
})

// Stages 1-3 (QPR + Project Workplan) then the Terminal Report stage.
const stageSummary = computed(() => {
  const defs = [
    { key: 'Q1', label: 'Q1 · QPR + Workplan', period: 'Q1', type: 'QUARTERLY_PROGRESS' },
    { key: 'Q2', label: 'Q2 · QPR + Workplan', period: 'Q2', type: 'QUARTERLY_PROGRESS' },
    { key: 'Q3', label: 'Q3 · QPR + Workplan', period: 'Q3', type: 'QUARTERLY_PROGRESS' },
    { key: 'TERMINAL', label: 'Terminal Report', period: 'TERMINAL', type: 'TERMINAL_REPORT' },
  ]
  return defs.map((def) => {
    const match = reports.value.find(
      (r) => (r.reportType || 'QUARTERLY_PROGRESS') === def.type &&
        (r.period || '').toUpperCase() === def.period,
    )
    let state = 'pending'
    if (match && match.status === 'SUBMITTED') state = 'done'
    else if (match && match.status === 'DRAFT') state = 'draft'
    else if (!allStagesComplete.value && nextStage.value?.period === def.period) state = 'current'
    return { key: def.key, label: def.label, state }
  })
})

function isTerminal(r) {
  return r?.reportType === 'TERMINAL_REPORT'
}

function reportTypeLabel(r) {
  return isTerminal(r) ? 'Terminal Report' : 'Quarterly Progress Report'
}
// The Terminal Report closes the project's reporting cycle, so it is labelled
// with the quarter of the last quarterly reporting stage recorded for this
// project (its own stored reporting year is used for the year). No quarter or
// year is hardcoded.
const lastQuarterlyReport = computed(() => {
  const order = ['Q1', 'Q2', 'Q3', 'Q4']
  let best = null
  let bestIndex = -1
  for (const r of reports.value) {
    if ((r.reportType || 'QUARTERLY_PROGRESS') !== 'QUARTERLY_PROGRESS') continue
    const index = order.indexOf((r.period || '').toUpperCase())
    if (index > bestIndex) {
      bestIndex = index
      best = r
    }
  }
  return best
})

function periodLabel(r) {
  if (isTerminal(r)) {
    const quarter = (lastQuarterlyReport.value?.period || '').toUpperCase()
    const year = r?.year || lastQuarterlyReport.value?.year || new Date().getFullYear()
    return quarter ? `${quarter} ${year}` : `${year}`
  }
  return `${r.period || ''} ${r.year || ''}`.trim()
}

async function fetchAll() {
  loading.value = true
  error.value = ''
  try {
    const [p, r] = await Promise.all([
      api.get(`/api/proposals/${proposalId.value}`),
      api.get(`/api/quarterly-reports/proposal/${proposalId.value}`),
    ])
    project.value = p.data
    reports.value = r.data || []
    if (eligible.value) {
      try {
        const n = await api.get(`/api/quarterly-reports/next-stage/${proposalId.value}`)
        nextStage.value = n.data
      } catch (e) {
        nextStage.value = null
      }
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load reports.'
  } finally {
    loading.value = false
  }
}

function createNew() {
  const draftId = nextStage.value?.draftReportId
  if (draftId) {
    router.push(`/quarterly-reports/${proposalId.value}/edit/${draftId}`)
  } else {
    router.push(`/quarterly-reports/${proposalId.value}/new`)
  }
}
function edit(r) {
  router.push(`/quarterly-reports/${proposalId.value}/edit/${r.id}`)
}
function view(r) {
  viewing.value = r
}
function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : ''
}

async function downloadDoc(r, docType) {
  try {
    actionError.value = ''
    const fileName = docType === 'workplan' ? r.workplanFileName : r.terminalReportFileName
    await downloadReportDocument(r.id, docType, fileName)
  } catch (e) {
    actionError.value = 'Failed to download the document.'
  }
}

// Opens the read-only Terminal Report preview modal. The shared modal component
// loads the file through the existing authorized endpoint itself; nothing is
// downloaded here and the report's status / saved data are left untouched.
function openTerminalReport(r) {
  terminalModal.value = {
    reportId: r.id,
    fileName: r.terminalReportFileName || '',
    periodLabel: periodLabel(r),
  }
}

function closeTerminalModal() {
  terminalModal.value = null
}

// The app shell scrolls its <main class="content"> region (not the window), and
// that region keeps the scroll position of the previous page. Arriving here from
// Proposal Monitoring (after scrolling its proposal list) used to land half-way
// down this page, hiding its title and the reporting progress. Resetting the
// real scroll container after the DOM updates puts the top back in view, while
// normal downward scrolling still works when the page is taller than the screen.
async function scrollPageToTop() {
  await nextTick()
  resetScrollPosition()
}

onMounted(async () => {
  // Reset before and after the reports load, because the loaded page is taller
  // than the loading state.
  await scrollPageToTop()
  await fetchAll()
  await scrollPageToTop()
})
</script>

<style scoped>
.qpr-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}
.back-btn {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.stages-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 16px;
}
.stages-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}
.stages-title {
  font-weight: 700;
  color: #0f172a;
}
.stages-next {
  color: #92400e;
  font-size: 13px;
}
.stages-done {
  color: #166534;
  font-weight: 700;
  font-size: 13px;
}
.stage-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.stage-chip {
  padding: 6px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  border: 1px solid #cbd5e1;
  background: #f8fafc;
  color: #475569;
}
.stage-chip.done {
  background: #dcfce7;
  border-color: #86efac;
  color: #166534;
}
.stage-chip.draft {
  background: #fef3c7;
  border-color: #fde68a;
  color: #92400e;
}
.stage-chip.current {
  background: #eef2ff;
  border-color: #a5b4fc;
  color: #3730a3;
}
.btn-primary {
  margin-left: auto;
  background: #ffd400;
  color: #1c2145;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.state {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 32px;
  border-radius: 8px;
  text-align: center;
  color: #64748b;
}
.state.error {
  color: #dc2626;
}
.action-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}
.state.notice {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}
/* Report list rendered as a table so the Report / Status / Actions headers line
   up with every row. Each row keeps the previous card look (white, rounded, 1px
   border, 12px gaps) via border-spacing plus per-cell borders. */
.report-table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0 12px;
}
.report-table thead th {
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  color: #ffffff;
  background: #4b3f72;
  text-align: center;
}
.report-table thead th.cell-report {
  padding-left: 20px;
  text-align: left;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
}
.report-table thead th.cell-actions {
  padding-right: 20px;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
}
.report-table tbody td {
  background: #fff;
  border-top: 1px solid #e2e8f0;
  border-bottom: 1px solid #e2e8f0;
  padding: 16px 16px;
  vertical-align: middle;
}
.report-table tbody td.cell-report {
  padding-left: 20px;
}
.report-table tbody td.cell-date,
.report-table tbody td.cell-status,
.report-table tbody td.cell-actions {
  white-space: nowrap;
  text-align: center;
}
.report-table tbody td.cell-actions {
  padding-right: 20px;
}
.report-table tbody td:first-child {
  border-left: 1px solid #e2e8f0;
  border-top-left-radius: 10px;
  border-bottom-left-radius: 10px;
}
.report-table tbody td:last-child {
  border-right: 1px solid #e2e8f0;
  border-top-right-radius: 10px;
  border-bottom-right-radius: 10px;
}
/* Left section: report title, then the quarter/year. */
.report-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 0;
  text-align: left;
}
.report-title {
  font-size: 15px;
  font-weight: 700;
  line-height: 1.25;
  color: #1c2145;
}
.report-period {
  font-size: 13px;
  font-weight: 600;
  color: #475569;
}
.report-date {
  font-size: 13px;
  color: #1c2145;
}
/* Status badge gets its own column so it lines up on every card. */
.report-status {
  display: flex;
  align-items: center;
  justify-content: center;
}
.report-meta {
  color: #64748b;
  font-size: 13px;
}
/* Right section: the action button(s). */
.report-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.btn-view,
.btn-edit,
.btn-download {
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}
.btn-view {
  background: #eef2ff;
  color: #1c2145;
}
.btn-edit {
  background: #ffd400;
  color: #1c2145;
}
.btn-download {
  background: #dcfce7;
  color: #166534;
}
/* Read-only status label: no pill/background/rounded styling so it cannot be
   mistaken for a clickable button. */
.status-badge {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}
.status-badge.submitted {
  color: #166534;
}
.status-badge.draft {
  color: #92400e;
}
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  z-index: 200;
  padding: 24px;
  overflow-y: auto;
}
.modal {
  background: #fff;
  width: 100%;
  max-width: 1000px;
  border-radius: 12px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  background: #fff;
}
.modal-header h2 {
  margin: 0;
  font-size: 18px;
}
.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  cursor: pointer;
}
.modal-body {
  padding: 16px 20px 24px;
}
/* The report viewer modal uses a column layout, so the modal header keeps its
   place while the viewer owns the scrolling. Scoped to the modifier, so the
   list modals are unaffected. */
.modal.modal--viewer {
  display: flex;
  flex-direction: column;
  overflow: hidden; /* the modal itself never scrolls */
}
.modal--viewer .modal-header {
  flex-shrink: 0;
}
.modal--viewer .modal-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* The viewer owns the report and footer padding, so the scrollable area and
     the footer can span the full modal width. */
  padding: 0;
}
/* Small screens: hide the column headers and stack each report as a card. */
@media (max-width: 640px) {
  .report-table {
    border-spacing: 0;
  }
  .report-table thead {
    display: none;
  }
  .report-table,
  .report-table tbody,
  .report-table tr,
  .report-table td {
    display: block;
    width: 100%;
  }
  .report-table tbody tr {
    background: #fff;
    border: 1px solid #e2e8f0;
    border-radius: 10px;
    padding: 16px 20px;
    margin-bottom: 12px;
  }
  .report-table tbody td,
  .report-table tbody td:first-child,
  .report-table tbody td:last-child {
    background: transparent;
    border: none;
    border-radius: 0;
    padding: 0;
  }
  .report-table tbody td.cell-report {
    margin-bottom: 4px;
  }
  .report-table tbody td.cell-date {
    margin-bottom: 12px;
  }
  .report-status,
  .report-actions {
    justify-content: center;
  }
  .report-actions {
    flex-wrap: wrap;
  }
}
</style>
