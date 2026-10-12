<template>
  <div class="master-page">
    <div class="page-header">
      <div>
        <h1>Quarterly Progress Reports</h1>
        <p class="subtitle">Submitted quarterly reports of approved, SO-issued projects</p>
      </div>
    </div>

    <p v-if="actionError" class="action-error">{{ actionError }}</p>

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
      <div v-else class="report-list">
        <div v-for="r in selectedReports" :key="r.id" class="report-card">
          <div class="report-info">
            <span class="report-type">{{ reportTypeLabel(r) }}</span>
            <span v-if="periodLabel(r)" class="report-period">{{ periodLabel(r) }}</span>
            <span class="status-badge" :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'">{{ r.status }}</span>
            <span class="report-meta">Submitted: {{ formatDate(r.submittedAt) || '—' }}</span>
          </div>
          <div class="report-actions">
            <template v-if="isTerminal(r)">
              <button v-if="r.terminalReportFileName" class="btn-view" @click="previewDoc(r, 'terminal')">View Terminal Report</button>
              <button v-if="r.terminalReportFileName" class="btn-download" @click="downloadDoc(r, 'terminal')">Download</button>
              <span v-else class="report-meta">No terminal report file attached.</span>
            </template>
            <template v-else>
              <button class="btn-view" @click="view(r)">View Report</button>
              <!-- The Project Workplan is structured form data shown in the report
                   view; a legacy uploaded workplan file (if any) stays downloadable. -->
              <span v-if="hasWorkplan(r)" class="report-meta">Project Workplan saved</span>
              <button v-if="r.workplanFileName" class="btn-download" @click="downloadDoc(r, 'workplan')">Download workplan file</button>
              <span v-if="!hasWorkplan(r) && !r.workplanFileName" class="report-meta">No Project Workplan saved.</span>
            </template>
          </div>
        </div>
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
          <p v-else class="report-meta">No Project Workplan has been saved for this reporting stage.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'
import ProjectWorkplanForm from '@/components/ProjectWorkplanForm.vue'
import { downloadReportDocument, previewReportDocument } from '@/utils/fileDownload'

const projects = ref([])
const loading = ref(true)
const error = ref('')
const actionError = ref('')
const selectedProject = ref(null)
const selectedReports = ref([])
const reportsLoading = ref(false)
const viewing = ref(null)

// Saved Project Workplan of the report open in the read-only modal. A deep copy
// is passed so the read-only component can normalise its display (twelve month
// cells, etc.) without touching the loaded report data.
const viewingWorkplan = computed(() => {
  const r = viewing.value
  if (!r || !r.workplan) return null
  return JSON.parse(JSON.stringify(r.workplan))
})

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

function view(r) {
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

// The Project Workplan is structured form data (workplan_json), not a file. A
// legacy uploaded workplan file may still exist and stays downloadable.
function hasWorkplan(r) {
  const objectives = r?.workplan?.objectives
  return Array.isArray(objectives) && objectives.some((o) => (o?.objective || '').trim() !== '')
}

function reportTypeLabel(r) {
  return isTerminal(r) ? 'Terminal Report' : 'Quarterly Progress Report'
}

function periodLabel(r) {
  if (isTerminal(r)) return ''
  return `${r.period || ''} ${r.year || ''}`.trim()
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

async function previewDoc(r, docType) {
  try {
    actionError.value = ''
    await previewReportDocument(r.id, docType)
  } catch (e) {
    actionError.value = 'Failed to open the document.'
  }
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
.action-error {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #dc2626;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
}
.project-table {
  width: 100%;
  border-collapse: collapse;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}
.project-table th,
.project-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
  font-size: 14px;
}
.project-table th {
  background: #f1f5f9;
  color: #475569;
  font-weight: 700;
}
.id-cell {
  color: #64748b;
}
.title-cell {
  font-weight: 600;
  color: #0f172a;
}
.report-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.report-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px 20px;
}
.report-info {
  display: flex;
  align-items: center;
  gap: 14px;
}
.report-type {
  font-size: 12px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: #4338ca;
  background: #eef2ff;
  padding: 4px 10px;
  border-radius: 6px;
}
.report-period {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}
.report-meta {
  color: #64748b;
  font-size: 13px;
}
.report-actions {
  display: flex;
  align-items: center;
  gap: 10px;
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
.btn-download {
  background: #ffd400;
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
</style>
