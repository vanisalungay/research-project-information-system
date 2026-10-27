<template>
  <div class="master-page">
    <div class="page-header">
      <div>
        <h1>Quarterly Progress Reports</h1>
        <p class="subtitle">Submitted quarterly reports of approved, SO-issued projects</p>
      </div>
    </div>

    <div v-if="loading" class="state">Loading projects...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>

    <!-- Selected project: report history -->
    <template v-else-if="selectedProject">
      <div class="sub-header">
        <button class="back-btn" @click="selectedProject = null">← All Projects</button>
        <div>
          <h2>{{ selectedProject.projectTitle }}</h2>
          <p class="subtitle">SO Number: {{ selectedProject.soNumber || 'N/A' }}</p>
        </div>
      </div>

      <div v-if="reportsLoading" class="state">Loading reports...</div>
      <div v-else-if="selectedReports.length === 0" class="state">
        No quarterly progress reports or terminal reports submitted for this project yet.
      </div>
      <!-- Every submitted report is one row: the terminal report and each
           quarterly report are listed separately, newest first (the order the
           existing reports endpoint returns). -->
      <div v-else class="report-table-wrap">
        <table class="report-table">
          <thead>
            <tr>
              <th>Report</th>
              <th>Date Submitted</th>
              <th>Status</th>
              <th class="cell-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in selectedReports" :key="r.id">
              <td class="report-name">{{ reportName(r) }}</td>
              <td class="report-date">{{ formatDate(r.submittedAt) || '—' }}</td>
              <td>
                <span
                  class="status-badge"
                  :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'"
                >
                  {{ formatStatus(r.status) }}
                </span>
              </td>
              <td class="cell-actions">
                <button class="btn-view" @click="viewReport(r)">View</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- Master list of eligible projects -->
    <template v-else>
      <div v-if="projects.length === 0" class="state">
        No approved projects with an issued Special Order yet.
      </div>
      <table v-else class="project-table">
        <thead>
          <tr>
            <th>Proposal ID</th>
            <th>Project Title</th>
            <th>Project Leader</th>
            <th>SO Number</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in projects" :key="p.id">
            <td class="id-cell">{{ p.documentId || p.proposalCode || '—' }}</td>
            <td class="title-cell">{{ p.projectTitle || 'Untitled' }}</td>
            <td>{{ p.projectLeader || p.proponent?.name || 'N/A' }}</td>
            <td>{{ p.soNumber || 'N/A' }}</td>
            <td><span class="status-badge submitted">{{ formatStatus(p.status) }}</span></td>
            <td><button class="btn-view" @click="openProject(p)">View Reports</button></td>
          </tr>
        </tbody>
      </table>
    </template>

    <!-- Read-only quarterly progress report modal: the same three page viewer the
         proponent sees, with its own Previous Page / Next Page navigation. -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal modal--viewer">
        <div class="modal-header">
          <h2>Quarterly Progress Report</h2>
          <button class="close-btn" @click="viewing = null" aria-label="Close">✕</button>
        </div>
        <div class="modal-body">
          <QuarterlyReportViewer :report="viewing" @close="viewing = null" />
        </div>
      </div>
    </div>

    <!-- Terminal Report document viewer: a Terminal Report completed in RPIS is read as
         a document (same component the proponent side uses), read only. -->
    <div v-if="terminalDoc" class="modal-overlay" @click.self="terminalDoc = null">
      <div class="modal modal--viewer modal--terminal-doc">
        <div class="modal-header">
          <div class="modal-header-text">
            <h2>Terminal Report</h2>
            <p class="modal-header-sub">
              {{ terminalDoc.projectTitle || terminalDoc.proposal?.projectTitle || '' }}
            </p>
          </div>
          <div class="modal-header-actions">
            <button class="btn-print" @click="printTerminalDocument">Print / Save as PDF</button>
            <button class="close-btn" @click="terminalDoc = null" aria-label="Close">✕</button>
          </div>
        </div>
        <div class="modal-body">
          <TerminalReportForm
            ref="terminalDocRef"
            :model-value="terminalDoc.terminalReport"
            readonly
          >
            <template #attachment>
              <p v-if="terminalDoc.terminalReportFileName" class="tr-note">
                Attachment: {{ terminalDoc.terminalReportFileName }}
              </p>
            </template>
          </TerminalReportForm>
        </div>
      </div>
    </div>

    <!-- Terminal Report preview modal (shared component): shows the actual
         uploaded file, read only, with its own Download action. Used for legacy
         reports that only hold a file, and for the file attached to a structured
         report. -->
    <TerminalReportPreviewModal
      v-if="terminalModal"
      :report-id="terminalModal.reportId"
      :file-name="terminalModal.fileName"
      :period-label="terminalModal.periodLabel"
      @close="terminalModal = null"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import QuarterlyReportViewer from '@/components/QuarterlyReportViewer.vue'
import TerminalReportPreviewModal from '@/components/TerminalReportPreviewModal.vue'
import TerminalReportForm from '@/components/TerminalReportForm.vue'
import { printTerminalReport } from '@/utils/terminalReportPrint'
import { hasTerminalReportContent } from '@/utils/terminalReport'

const projects = ref([])
const loading = ref(true)
const error = ref('')
const selectedProject = ref(null)
const selectedReports = ref([])
const reportsLoading = ref(false)
const viewing = ref(null)
const terminalModal = ref(null)
// A Terminal Report completed in RPIS, opened as a document.
const terminalDoc = ref(null)
const terminalDocRef = ref(null)

async function fetchProjects() {
  loading.value = true
  error.value = ''
  try {
    const res = await api.get('/api/quarterly-reports/eligible-projects')
    projects.value = res.data || []
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load projects.'
  } finally {
    loading.value = false
  }
}

async function openProject(p) {
  selectedProject.value = p
  selectedReports.value = []
  reportsLoading.value = true
  try {
    const res = await api.get(`/api/quarterly-reports/proposal/${p.id}`)
    selectedReports.value = res.data || []
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load reports.'
  } finally {
    reportsLoading.value = false
  }
}

// One "View" action per row, dispatching on the report type the row holds:
// terminal reports open the report document (a Terminal Report completed in RPIS)
// or, for a legacy report that only holds a file, the document preview modal;
// every quarterly progress report opens the read-only three page report modal.
// Both are read only - the report id of the clicked row is the record they load.
function viewReport(r) {
  if (isTerminal(r)) {
    if (hasStructuredTerminal(r)) {
      terminalDoc.value = r
      return
    }
    terminalModal.value = {
      reportId: r.id,
      fileName: r.terminalReportFileName || '',
      periodLabel: periodLabel(r),
    }
    return
  }
  viewing.value = r
}

/**
 * True when the report holds a Terminal Report completed in RPIS (the structured
 * document). Those reports are read as a document; a legacy report that only holds an
 * uploaded file keeps the existing file preview. A submitted structured report always
 * satisfies the core content rule the backend enforces at submission.
 */
function hasStructuredTerminal(r) {
  return hasTerminalReportContent(r?.terminalReport)
}

/** Prints the open Terminal Report document, or saves it as PDF. */
function printTerminalDocument() {
  const element = terminalDocRef.value?.$el || terminalDocRef.value
  return printTerminalReport({
    title: `Terminal Report - ${terminalDoc.value?.projectTitle || ''}`,
    element,
  })
}

function formatDate(d) {
  return d ? new Date(d).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : ''
}
function formatStatus(s) {
  return s ? s.replace(/_/g, ' ') : 'Unknown'
}

// Reporting stages 1-3 are quarterly progress reports (each paired with a
// Project Workplan); the fourth stage is the Terminal Report.
function isTerminal(r) {
  return r?.reportType === 'TERMINAL_REPORT'
}

// The Project Workplan is structured form data (workplan_json), not a file, and
// is displayed inside the read-only report viewer (page 3).
function reportTypeLabel(r) {
  return isTerminal(r) ? 'Terminal Report' : 'Quarterly Progress Report'
}

function periodLabel(r) {
  if (isTerminal(r)) return ''
  return `${r.period || ''} ${r.year || ''}`.trim()
}

/**
 * Report column value, derived from the stored record: the terminal report is
 * named by its type, and each quarterly progress report by its reporting period
 * (e.g. "Quarterly Progress Report — Q3 2026").
 */
function reportName(r) {
  const period = periodLabel(r)
  return period ? `${reportTypeLabel(r)} — ${period}` : reportTypeLabel(r)
}

onMounted(fetchProjects)
</script>

<style scoped>
.master-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}
.page-header h1 {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}
.sub-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.sub-header h2 {
  margin: 0;
  font-size: 18px;
}
.back-btn {
  background: #fff;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
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
.project-table,
.report-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.project-table th,
.project-table td,
.report-table th,
.report-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  font-size: 14px;
}
.project-table th,
.report-table th {
  background: #f1f5f9;
  color: #475569;
  font-weight: 700;
}
/* The submitted-reports table keeps its four columns readable on smaller
   screens by scrolling horizontally instead of overlapping the columns. */
.report-table-wrap {
  overflow-x: auto;
}
.report-table {
  min-width: 720px;
}
.report-name {
  font-weight: 600;
  color: #0f172a;
}
.report-date {
  color: #64748b;
  white-space: nowrap;
}
.cell-actions {
  white-space: nowrap;
}
.id-cell {
  color: #64748b;
}
.title-cell {
  font-weight: 600;
  color: #0f172a;
}
.btn-view {
  background: #eef2ff;
  color: #1c2145;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
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
/* The report viewer modal uses a column layout, so the modal header keeps its
   place while the viewer owns the scrolling and its own page navigation. */
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
  padding: 0; /* the viewer owns its own padding */
}

/* ---------- Terminal Report document viewer ----------
   A Terminal Report completed in RPIS is read as a document: the modal keeps its
   header (title, print action, close) in place while the document scrolls. */
.modal.modal--terminal-doc {
  max-width: 1180px;
}
.modal--terminal-doc .modal-header {
  gap: 12px;
}
.modal-header-text {
  min-width: 0;
}
.modal-header-sub {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}
.modal-header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
.modal--terminal-doc .modal-body {
  overflow-y: auto;
  padding: 16px 20px 24px;
  background: #f8fafc;
}
.btn-print {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #1c2145;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
}

/* The report is authored for a printed Letter page (terminalReportDocument.css), so it is
   presented at a comfortable reading size here; print output keeps the printed sizes. */
@media screen {
  .modal--terminal-doc :deep(.tr-sheet) {
    width: 100%;
    min-width: 820px;
    min-height: 0;
  }
  .modal--terminal-doc :deep(.tr-cover-title) {
    font-size: 22pt;
  }
  .modal--terminal-doc :deep(.tr-cover-team) {
    font-size: 11pt;
  }
  .modal--terminal-doc :deep(.tr-cover-report) {
    font-size: 14pt;
  }
  .modal--terminal-doc :deep(.tr-front-heading),
  .modal--terminal-doc :deep(.tr-section-heading) {
    font-size: 14pt;
  }
  .modal--terminal-doc :deep(.tr-subsection-heading) {
    font-size: 13pt;
  }
  .modal--terminal-doc :deep(.tr-body-title) {
    font-size: 14pt;
  }
  .modal--terminal-doc :deep(.tr-paragraph),
  .modal--terminal-doc :deep(.tr-reference),
  .modal--terminal-doc :deep(.tr-keywords) {
    font-size: 11.5pt;
  }
  .modal--terminal-doc :deep(.tr-table-caption),
  .modal--terminal-doc :deep(.tr-list-entry) {
    font-size: 12pt;
  }
  .modal--terminal-doc :deep(.tr-grid) {
    font-size: 10.5pt;
  }
  .modal--terminal-doc :deep(.tr-page-number) {
    font-size: 12pt;
  }
}
</style>
