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
      <div v-else class="report-list">
        <div v-for="r in reports" :key="r.id" class="report-card">
          <div class="report-info">
            <span class="report-title">{{ reportTypeLabel(r) }}</span>
            <span v-if="periodLabel(r)" class="report-period">{{ periodLabel(r) }}</span>
            <span class="report-date">Submitted: {{ formatDate(r.submittedAt) || '—' }}</span>
          </div>
          <div class="report-status">
            <span class="status-badge" :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'">{{ r.status }}</span>
          </div>
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
        </div>
      </div>
    </template>

    <!-- Read-only view modal -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal">
        <div class="modal-header">
          <h2>Quarterly Progress Report</h2>
          <button class="close-btn" @click="viewing = null">✕</button>
        </div>
        <div class="modal-body">
          <QuarterlyReportForm :report="viewing" readonly />
          <ProjectWorkplanForm v-if="viewingWorkplan" :model-value="viewingWorkplan" readonly />
        </div>
      </div>
    </div>

    <!-- Terminal Report view modal: shows the uploaded file and offers the
         authorized download. Opening it never downloads or changes the report. -->
    <div v-if="terminalModal" class="modal-overlay" @click.self="closeTerminalModal">
      <div
        class="modal modal--terminal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="terminal-report-modal-title"
      >
        <div class="modal-header">
          <h2 id="terminal-report-modal-title">Terminal Report</h2>
          <button class="close-btn" @click="closeTerminalModal" aria-label="Close">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="terminalModal.fileName" class="terminal-file">
            <span class="terminal-file-icon" :class="`terminal-file-icon--${terminalModal.kind}`">
              {{ terminalModal.typeLabel }}
            </span>
            <div class="terminal-file-info">
              <span class="terminal-file-name" :title="terminalModal.fileName">
                {{ terminalModal.fileName }}
              </span>
              <span class="terminal-file-sub">
                {{ terminalModal.typeLabel }}
                <template v-if="terminalModal.periodLabel"> · {{ terminalModal.periodLabel }}</template>
              </span>
            </div>
          </div>
          <p v-else class="report-meta">No Terminal Report file is attached to this report.</p>
          <p v-if="terminalModal.error" class="terminal-file-error">{{ terminalModal.error }}</p>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeTerminalModal">Close</button>
          <button
            class="btn-primary"
            :disabled="!terminalModal.fileName || terminalModal.downloading"
            @click="downloadTerminalReport"
          >
            {{ terminalModal.downloading ? 'Downloading…' : 'Download' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'
import ProjectWorkplanForm from '@/components/ProjectWorkplanForm.vue'
import { downloadReportDocument } from '@/utils/fileDownload'

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

// Saved Project Workplan of the report open in the read-only modal (deep copy so
// the read-only component can normalise its display without mutating the report).
const viewingWorkplan = computed(() => {
  const r = viewing.value
  if (!r || !r.workplan) return null
  return JSON.parse(JSON.stringify(r.workplan))
})

const eligible = computed(() => {
  const p = project.value
  if (!p) return false
  const hasSo = p.soNumber && p.soNumber.trim() !== ''
  const approved = p.status === 'APPROVED' || p.status === 'RELEASED'
  return hasSo && approved
})

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

/** File-type bucket for the modal icon (pdf / doc / generic). */
function fileKind(name) {
  const ext = (name || '').toLowerCase().split('.').pop()
  if (ext === 'pdf') return 'pdf'
  if (ext === 'doc' || ext === 'docx') return 'doc'
  return 'file'
}

/** Extension shown on the modal icon, e.g. "PDF" or "DOCX". */
function fileTypeLabel(name) {
  const lower = (name || '').toLowerCase()
  const ext = lower.split('.').pop()
  if (!ext || ext === lower) return 'FILE'
  return ext.toUpperCase()
}

// Opens the read-only Terminal Report modal. Nothing is downloaded here and the
// report's status / saved data are left untouched.
function openTerminalReport(r) {
  const fileName = r.terminalReportFileName || ''
  terminalModal.value = {
    reportId: r.id,
    fileName,
    kind: fileKind(fileName),
    typeLabel: fileTypeLabel(fileName),
    periodLabel: periodLabel(r),
    error: '',
    downloading: false,
  }
}

function closeTerminalModal() {
  terminalModal.value = null
}

// Downloads through the existing authorized endpoint used by the report cards.
async function downloadTerminalReport() {
  const modal = terminalModal.value
  if (!modal || !modal.fileName) return
  modal.downloading = true
  modal.error = ''
  try {
    await downloadReportDocument(modal.reportId, 'terminal', modal.fileName)
  } catch (e) {
    modal.error = 'Failed to download the Terminal Report. Please try again.'
  } finally {
    modal.downloading = false
  }
}

function onKeydown(e) {
  if (e.key === 'Escape' && terminalModal.value) closeTerminalModal()
}

onMounted(() => {
  fetchAll()
  window.addEventListener('keydown', onKeydown)
})

onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
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
.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.report-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
}
/* Left section: report title, then the quarter/year, then the submission date. */
.report-info {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 0;
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
  font-size: 12px;
  color: #94a3b8;
}
/* Status badge gets its own column so it lines up on every card. */
.report-status {
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.report-meta {
  color: #64748b;
  font-size: 13px;
}
/* Right section: the action button(s). */
.report-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
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
.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
}
.status-badge.submitted {
  background: #dcfce7;
  color: #166534;
}
.status-badge.draft {
  background: #fef3c7;
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
/* Terminal Report modal */
.modal--terminal {
  max-width: 460px;
}
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}
.modal-footer .btn-primary {
  margin-left: 0;
}
.btn-secondary {
  background: #e2e8f0;
  color: #1c2145;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.terminal-file {
  display: flex;
  align-items: center;
  gap: 12px;
}
.terminal-file-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e2e8f0;
  color: #475569;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.2px;
}
.terminal-file-icon--pdf {
  background: #fee2e2;
  color: #b91c1c;
}
.terminal-file-icon--doc {
  background: #dbeafe;
  color: #1d4ed8;
}
.terminal-file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.terminal-file-name {
  font-size: 14px;
  font-weight: 600;
  color: #1c2145;
  overflow-wrap: anywhere;
}
.terminal-file-sub {
  font-size: 12px;
  color: #64748b;
}
.terminal-file-error {
  margin: 12px 0 0;
  font-size: 13px;
  color: #dc2626;
}

/* Report cards: stack the sections when there is not enough width. */
@media (max-width: 640px) {
  .report-card {
    grid-template-columns: minmax(0, 1fr);
    gap: 12px;
  }
  .report-status,
  .report-actions {
    justify-content: flex-start;
  }
}
</style>
