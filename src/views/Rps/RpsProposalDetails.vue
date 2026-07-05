<template>
  <div class="proposal-detail-page">
    <!-- HEADER -->
    <header class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
          Back
        </button>
        <div class="header-info">
          <h1>{{ proposal.projectTitle || 'Proposal Details' }}</h1>
          <div class="header-meta">
            <span class="proposal-id">ID: {{ proposal.id }}</span>
            <span class="status-badge" :class="(proposal.status || '').toLowerCase()">
              {{ proposal.status || 'DRAFT' }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="downloadProposal">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          Download PDF
        </button>
      </div>
    </header>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="goBack">Go Back</button>
    </div>

    <!-- Content -->
    <template v-else-if="proposal.id">
      <div class="content-grid">
        <!-- LEFT COLUMN: Main Content -->
        <div class="main-content">
          <!-- Project Profile -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">01</span>
              <h2>Project Profile</h2>
            </div>
            <div class="card-body">
              <div class="info-grid">
                <div class="info-item">
                  <label>Program Title</label>
                  <p>{{ proposal.programTitle || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Project Title</label>
                  <p>{{ proposal.projectTitle || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Project Leader</label>
                  <p>{{ proposal.projectLeader || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Duration</label>
                  <p>{{ proposal.duration || 'Not provided' }} months</p>
                </div>
                <div class="info-item">
                  <label>Start Date</label>
                  <p>{{ proposal.startDate || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>End Date</label>
                  <p>{{ proposal.endDate || 'Not provided' }}</p>
                </div>
                <div class="info-item full-width">
                  <label>Implementing College / Department</label>
                  <p>{{ proposal.college || 'Not provided' }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Cooperating Agencies -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">02</span>
              <h2>Cooperating Agencies</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.cooperatingAgencies || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Sites of Implementation -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">03</span>
              <h2>Sites of Implementation</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.sites && proposal.sites.length > 0" class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Country</th>
                      <th>Region</th>
                      <th>Province</th>
                      <th>Municipality</th>
                      <th>Barangay</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(site, index) in proposal.sites" :key="index">
                      <td>{{ site.country }}</td>
                      <td>{{ site.region }}</td>
                      <td>{{ site.province }}</td>
                      <td>{{ site.municipality }}</td>
                      <td>{{ site.barangay }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No implementation sites available.</p>
            </div>
          </section>

          <!-- Research Type & Priority Agenda -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">04</span>
              <h2>Type of Research</h2>
            </div>
            <div class="card-body">
              <span class="type-badge" :class="(proposal.researchType || '').toLowerCase()">
                {{ proposal.researchType || 'Not specified' }}
              </span>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">05</span>
              <h2>Priority Agenda</h2>
            </div>
            <div class="card-body">
              <div v-if="hasPriorityAgendas" class="agenda-list">
                <div v-if="proposal.priorityAgendas?.dagat?.selected" class="agenda-item">
                  <strong>DAGAT:</strong> {{ proposal.priorityAgendas.dagat.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.punla?.selected" class="agenda-item">
                  <strong>PUNLA:</strong> {{ proposal.priorityAgendas.punla.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.kalikasan?.selected" class="agenda-item">
                  <strong>KALIKASAN:</strong> {{ proposal.priorityAgendas.kalikasan.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.negosyo?.selected" class="agenda-item">
                  <strong>NEGOSYO:</strong> {{ proposal.priorityAgendas.negosyo.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.tanglaw?.selected" class="agenda-item">
                  <strong>TANGLAW:</strong> {{ proposal.priorityAgendas.tanglaw.value }}
                </div>
              </div>
              <p v-else class="empty-text">No priority agenda selected.</p>
            </div>
          </section>

          <!-- Text Sections -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">06</span>
              <h2>Executive Summary</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.executiveSummary || 'Not provided' }}</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">07</span>
              <h2>Methodology</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.methodology || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Documents -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">08</span>
              <h2>Documents & Attachments</h2>
            </div>
            <div class="card-body">
              <div class="documents-grid">
                <div v-if="proposal.reviewFileName" class="doc-item">
                  <div class="doc-icon">📄</div>
                  <div class="doc-info">
                    <h4>Review of Literature</h4>
                    <p>{{ proposal.reviewFileName }}</p>
                  </div>
                  <button class="btn-download" @click="downloadFile(proposal.reviewFileName)">Download</button>
                </div>
                <div v-if="proposal.roadmapFileName" class="doc-item">
                  <div class="doc-icon">📄</div>
                  <div class="doc-info">
                    <h4>Technology Roadmap</h4>
                    <p>{{ proposal.roadmapFileName }}</p>
                  </div>
                  <button class="btn-download" @click="downloadFile(proposal.roadmapFileName)">Download</button>
                </div>
                <div v-if="proposal.gadFileName" class="doc-item">
                  <div class="doc-icon">📄</div>
                  <div class="doc-info">
                    <h4>GAD Score</h4>
                    <p>{{ proposal.gadFileName }}</p>
                  </div>
                  <button class="btn-download" @click="downloadFile(proposal.gadFileName)">Download</button>
                </div>
              </div>
            </div>
          </section>
        </div>

        <!-- RIGHT COLUMN: Sidebar -->
        <aside class="sidebar">
          <!-- Status Card -->
          <div class="sidebar-card">
            <h3>Proposal Status</h3>
            <div class="status-indicator" :class="(proposal.status || '').toLowerCase()">
              <span class="status-dot"></span>
              <span class="status-text">{{ proposal.status || 'DRAFT' }}</span>
            </div>
            <div class="status-meta">
              <p><strong>Created:</strong> {{ formatDate(proposal.createdAt) }}</p>
              <p><strong>Updated:</strong> {{ formatDate(proposal.updatedAt) }}</p>
            </div>
          </div>

          <!-- Quick Info -->
          <div class="sidebar-card">
            <h3>Quick Information</h3>
            <div class="quick-info-list">
              <div class="quick-info-item">
                <span class="info-label">Project Leader</span>
                <span class="info-value">{{ proposal.projectLeader || 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">College</span>
                <span class="info-value">{{ proposal.college || 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">Duration</span>
                <span class="info-value">{{ proposal.duration ? proposal.duration + ' months' : 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">Research Type</span>
                <span class="info-value">{{ proposal.researchType || 'N/A' }}</span>
              </div>
            </div>
          </div>

          <!-- Reviewer Accountability -->
          <div class="sidebar-card accountability-card">
            <h3>Reviewer Accountability</h3>
            <div class="reviewer-info">
              <div class="info-item">
                <label>Reviewer Name</label>
                <input type="text" v-model="reviewerName" class="reviewer-input" placeholder="Enter your full name" />
              </div>
              <div class="info-item">
                <label>Position</label>
                <select v-model="reviewerPosition" class="position-select">
                  <option value="">-- Select Position --</option>
                  <option value="RSP_STAFF">RSP Staff</option>
                  <option value="RSP_DIRECTOR">RSP Director</option>
                </select>
              </div>
            </div>
            <div class="certify-section">
              <label class="checkbox-label">
                <input type="checkbox" v-model="isCertified" />
                <span>I certify that I am the assigned reviewer.</span>
              </label>
            </div>
            <button class="btn-action btn-primary" @click="beginReview" :disabled="!canBeginReview">
              Begin Review
            </button>
          </div>

          <!-- Action Buttons -->
          <div class="sidebar-card action-card">
            <h3>Actions</h3>
            <button class="btn-action btn-secondary" @click="downloadProposal">
              Download Proposal
            </button>
          </div>
        </aside>
      </div>
    </template>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const route = useRoute()
const proposal = ref({})
const loading = ref(true)
const error = ref(null)

// Reviewer Accountability fields
const reviewerName = ref('')
const reviewerPosition = ref('')
const isCertified = ref(false)

const hasPriorityAgendas = computed(() => {
  if (!proposal.value.priorityAgendas) return false
  return Object.values(proposal.value.priorityAgendas).some(agenda => agenda.selected)
})

const canBeginReview = computed(() => {
  return reviewerName.value && reviewerPosition.value && isCertified.value
})

const fetchProposal = async () => {
  loading.value = true
  error.value = null
  try {
    const proposalId = route.params.id
    if (!proposalId) {
      error.value = 'No proposal ID provided.'
      return
    }
    const res = await api.get(`/api/proposals/${proposalId}`)
    proposal.value = res.data
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

const saveReviewerInfo = async () => {
  try {
    await api.put(`/api/proposals/${proposal.value.id}/reviewer-info`, null, {
      params: {
        reviewedBy: reviewerName.value,
        reviewedByPosition: reviewerPosition.value
      }
    })
  } catch (err) {
    console.error(err)
  }
}

const beginReview = async () => {
  await saveReviewerInfo()
  window.location.href = `/proposal/${proposal.value.id}/review`
}

onMounted(() => {
  fetchProposal()
})

const goBack = () => {
  window.history.back()
}

const downloadProposal = async () => {
  await showAlert('Downloading proposal PDF...', { type: 'info', title: 'Download' })
}

const downloadFile = (fileName) => {
  if (!fileName) return
  const link = document.createElement('a')
  link.href = `http://localhost:8081/uploads/${fileName}`
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}
</script>

<style scoped>
.proposal-detail-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: 4px;
}

.back-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.header-info {
  flex: 1;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.header-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.proposal-id {
  font-size: 13px;
  color: #64748b;
  font-family: monospace;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.draft { background: #e5e7eb; color: #374151; }
.status-badge.submitted { background: #f3e8ff; color: #6b21a8; }
.status-badge.under_review { background: #ede9fe; color: #6d28d9; }
.status-badge.approved { background: #dcfce7; color: #166534; }
.status-badge.rejected { background: #fee2e2; color: #991b1b; }
.status-badge.revision { background: #e0e7ff; color: #3730a3; }

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #f1f5f9;
}

.btn-primary {
  background: #4f46e5;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}

/* LOADING & ERROR */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state p {
  color: #dc2626;
  font-size: 14px;
}

/* CONTENT GRID */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* CONTENT CARD */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.card-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #4f46e5;
  color: #fff;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.card-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.card-body {
  padding: 20px;
}

/* INFO GRID */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.info-item p {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  margin: 0;
}

/* TEXT CONTENT */
.text-content {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin: 0;
  white-space: pre-wrap;
}

/* TYPE BADGE */
.type-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  background: #eef2ff;
  color: #4f46e5;
}

.type-badge.basic {
  background: #dbeafe;
  color: #1e40af;
}

.type-badge.applied {
  background: #d1fae5;
  color: #065f46;
}

/* AGENDA LIST */
.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agenda-item {
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border-left: 3px solid #4f46e5;
}

.agenda-item strong {
  color: #4f46e5;
  font-size: 13px;
}

.agenda-item span {
  font-size: 14px;
  color: #1e293b;
}

/* TABLES */
.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  background: #f8fafc;
}

.data-table th {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  color: #475569;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
}

/* DOCUMENTS */
.documents-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.doc-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 10px;
  font-size: 24px;
  border: 1px solid #e2e8f0;
}

.doc-info {
  flex: 1;
}

.doc-info h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 2px 0;
}

.doc-info p {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.btn-download {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:hover {
  background: #4338ca;
}

/* SIDEBAR */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}

.sidebar-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 16px 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

/* STATUS CARD */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-indicator.draft { background: #f1f5f9; color: #64748b; }
.status-indicator.submitted { background: #f3e8ff; color: #6b21a8; }
.status-indicator.under_review { background: #ede9fe; color: #6d28d9; }
.status-indicator.approved { background: #dcfce7; color: #166534; }
.status-indicator.rejected { background: #fee2e2; color: #991b1b; }
.status-indicator.revision { background: #e0e7ff; color: #3730a3; }

.status-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.status-meta p {
  margin: 0;
}

.status-meta strong {
  color: #475569;
}

/* QUICK INFO */
.quick-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

/* REVIEWER ACCOUNTABILITY */
.accountability-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.accountability-card .info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.accountability-card label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
}

.reviewer-input, .position-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  color: #1e293b;
}

.reviewer-input {
  font-size: 14px;
}

.reviewer-input::placeholder {
  color: #9ca3af;
}

.certify-section {
  margin-top: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

/* ACTION CARD */
.action-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-action {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action.btn-primary {
  background: #4f46e5;
  color: #fff;
}

.btn-action.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-action.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-action.btn-secondary {
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-action.btn-secondary:hover {
  background: #f1f5f9;
}

/* EMPTY TEXT */
.empty-text {
  color: #94a3b8;
  font-style: italic;
  font-size: 14px;
}

/* RESPONSIVE */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .header-meta {
    flex-direction: column;
    align-items: flex-start;
  }

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>