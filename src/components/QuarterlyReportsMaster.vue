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

    <!-- Terminal Report preview modal (shared component): shows the actual
         uploaded file, read only, with its own Download action. -->
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

const projects = ref([])
const loading = ref(true)
const error = ref('')
const selectedProject = ref(null)
const selectedReports = ref([])
const reportsLoading = ref(false)
const viewing = ref(null)
const terminalModal = ref(null)

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
// terminal reports open the document preview modal, every quarterly progress
// report opens the read-only three page report modal. Both are read only - the
// report id of the clicked row is the record they load.
function viewReport(r) {
  if (isTerminal(r)) {
    terminalModal.value = {
      reportId: r.id,
      fileName: r.terminalReportFileName || '',
      periodLabel: periodLabel(r),
    }
    return
  }
  viewing.value = r
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
</style>
