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
        No quarterly progress reports submitted for this project yet.
      </div>
      <div v-else class="report-list">
        <div v-for="r in selectedReports" :key="r.id" class="report-card">
          <div class="report-info">
            <span class="report-period">{{ r.period }} {{ r.year }}</span>
            <span class="status-badge" :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'">{{ r.status }}</span>
            <span class="report-meta">Submitted: {{ formatDate(r.submittedAt) || '—' }}</span>
          </div>
          <div class="report-actions">
            <button class="btn-view" @click="view(r)">View</button>
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
            <th>#</th>
            <th>Project Title</th>
            <th>Project Leader</th>
            <th>SO Number</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="p in projects" :key="p.id">
            <td class="id-cell">#{{ p.id }}</td>
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
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'

const projects = ref([])
const loading = ref(true)
const error = ref('')
const selectedProject = ref(null)
const selectedReports = ref([])
const reportsLoading = ref(false)
const viewing = ref(null)

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
