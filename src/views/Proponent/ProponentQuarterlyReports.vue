<template>
  <div class="qpr-page">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← Back</button>
      <div>
        <h1>Quarterly Progress Reports</h1>
        <p class="subtitle">{{ project?.projectTitle || 'Project' }}</p>
      </div>
      <button class="btn-primary" :disabled="!eligible" @click="createNew">+ Create Report</button>
    </div>

    <div v-if="loading" class="state">Loading reports...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="!eligible" class="state notice">
      Quarterly progress reports are only available for projects that are approved for implementation
      and have an issued Special Order (SO).
    </div>
    <div v-else-if="reports.length === 0" class="state">
      No quarterly progress reports yet. Click "Create Report" to start your first quarter report.
    </div>
    <div v-else class="report-list">
      <div v-for="r in reports" :key="r.id" class="report-card">
        <div class="report-info">
          <span class="report-period">{{ r.period }} {{ r.year }}</span>
          <span class="status-badge" :class="r.status === 'SUBMITTED' ? 'submitted' : 'draft'">{{ r.status }}</span>
          <span class="report-meta">Submitted: {{ formatDate(r.submittedAt) || '—' }}</span>
        </div>
        <div class="report-actions">
          <button class="btn-view" @click="view(r)">View</button>
          <button v-if="r.status === 'DRAFT'" class="btn-edit" @click="edit(r)">Edit</button>
        </div>
      </div>
    </div>

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
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'

const route = useRoute()
const router = useRouter()
const proposalId = computed(() => route.params.proposalId)

const project = ref(null)
const reports = ref([])
const loading = ref(true)
const error = ref('')
const viewing = ref(null)

const eligible = computed(() => {
  const p = project.value
  if (!p) return false
  const hasSo = p.soNumber && p.soNumber.trim() !== ''
  const approved = p.status === 'APPROVED' || p.status === 'RELEASED'
  return hasSo && approved
})

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
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load reports.'
  } finally {
    loading.value = false
  }
}

function createNew() {
  router.push(`/quarterly-reports/${proposalId.value}/new`)
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

onMounted(fetchAll)
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
.btn-view,
.btn-edit {
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
