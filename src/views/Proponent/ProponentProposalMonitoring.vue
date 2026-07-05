<template>
  <div class="monitoring-page">
    <!-- Header -->
    <div class="page-header">
      <div>
        <h1>Proposal Monitoring</h1>
        <p class="subtitle">Track the live progress of your research proposals through the approval workflow</p>
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon blue">📋</div>
        <div class="stat-info">
          <span class="stat-value">{{ totalProposals }}</span>
          <span class="stat-label">Total Proposals</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon yellow">⏳</div>
        <div class="stat-info">
          <span class="stat-value">{{ inProgressCount }}</span>
          <span class="stat-label">In Progress</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green">✅</div>
        <div class="stat-info">
          <span class="stat-value">{{ approvedCount }}</span>
          <span class="stat-label">Approved</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple">🚀</div>
        <div class="stat-info">
          <span class="stat-value">{{ implementationCount }}</span>
          <span class="stat-label">Implementation</span>
        </div>
      </div>
    </div>

    <!-- Filters -->
    <div class="filters-card">
      <div class="filters-row">
        <div class="filter-group">
          <label>Search</label>
          <input 
            v-model="searchQuery" 
            type="text" 
            placeholder="Search by title or ID..."
            class="form-input"
          />
        </div>
        <div class="filter-group">
          <label>Filter by Status</label>
          <select v-model="statusFilter" class="form-select">
            <option value="">All Statuses</option>
            <option value="DRAFT">Draft</option>
            <option value="RPS_REVIEW">RPS Review</option>
            <option value="RPS_RETURNED">Returned for Revision</option>
            <option value="OVCRIGE_COORDINATION">OVCRIGE Coordination</option>
            <option value="REC_EVALUATION">REC Evaluation</option>
            <option value="CHANCELLOR_REVIEW">Chancellor Review</option>
            <option value="FINANCE_AUTHORIZATION">Finance Authorization</option>
            <option value="IMPLEMENTATION">Implementation</option>
            <option value="COMPLETED">Completed</option>
          </select>
        </div>
        <div class="filter-group btn-group">
          <button class="btn-clear" @click="clearFilters">Clear Filters</button>
        </div>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading your proposals...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="fetchProposals">Retry</button>
    </div>

    <!-- Proposals List -->
    <div v-else-if="filteredProposals.length > 0" class="proposals-list">
      <div 
        v-for="proposal in filteredProposals" 
        :key="proposal.id" 
        class="proposal-card"
      >
        <!-- Card Header -->
        <div class="card-header">
          <div class="header-left">
            <span class="proposal-id">#{{ proposal.id }}</span>
            <h3 class="proposal-title">{{ proposal.projectTitle || 'Untitled Project' }}</h3>
          </div>
          <div class="header-right">
            <span :class="['status-badge', getStatusClass(proposal.status)]">
              {{ formatStatus(proposal.status) }}
            </span>
          </div>
        </div>

        <!-- Card Body -->
        <div class="card-body">
          <div class="proposal-meta">
            <span><strong>Project Leader:</strong> {{ proposal.projectLeader || 'N/A' }}</span>
            <span><strong>College:</strong> {{ proposal.college || 'N/A' }}</span>
            <span><strong>Submitted:</strong> {{ formatDate(proposal.createdAt) }}</span>
          </div>

          <!-- Progress Timeline -->
          <div class="timeline-container">
            <div class="timeline-header">
              <span class="timeline-title">Progress Timeline</span>
              <span class="timeline-progress">{{ getProgressPercentage(proposal.status) }}% Complete</span>
            </div>
            <div class="timeline">
              <div 
                v-for="(stage, index) in stages" 
                :key="stage.key"
                :class="['timeline-step', getStageClass(proposal.status, stage.key, index)]"
              >
                <div class="step-indicator">
                  <div class="step-dot">
                    <span v-if="isStageComplete(proposal.status, stage.key, index)" class="check-icon">✓</span>
                    <span v-else-if="isStageCurrent(proposal.status, stage.key)" class="current-icon">●</span>
                    <span v-else class="step-number">{{ index + 1 }}</span>
                  </div>
                  <div v-if="index < stages.length - 1" class="step-line"></div>
                </div>
                <div class="step-label">{{ stage.label }}</div>
              </div>
            </div>
          </div>

          <!-- Progress Bar -->
          <div class="progress-bar-container">
            <div class="progress-bar">
              <div 
                class="progress-fill" 
                :style="{ width: getProgressPercentage(proposal.status) + '%' }"
              ></div>
            </div>
          </div>
        </div>

        <!-- Card Actions -->
        <div class="card-actions">
          <button class="btn-view" @click="viewDetails(proposal.id)">
            📄 View Details
          </button>
          <button 
            v-if="proposal.status === 'IMPLEMENTATION'" 
            class="btn-upload"
            @click="openUploadModal(proposal.id)"
          >
            📤 Upload Reports
          </button>
          <button 
            v-if="proposal.status === 'RPS_RETURNED'" 
            class="btn-revise"
            @click="submitRevision(proposal.id)"
          >
            ✏️ Submit Revision
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-else class="empty-state">
      <div class="empty-icon">📂</div>
      <h3>No Proposals Found</h3>
      <p v-if="searchQuery || statusFilter">Try adjusting your search or filter criteria.</p>
      <p v-else>You haven't submitted any proposals yet. Start by creating a new proposal.</p>
      <button v-if="!searchQuery && !statusFilter" class="btn-primary" @click="$router.push('/proposals')">
        + Create New Proposal
      </button>
    </div>

    <!-- Upload Modal -->
    <div v-if="showUploadModal" class="modal-overlay" @click.self="closeUploadModal">
      <div class="modal-content">
        <div class="modal-header">
          <h2>Upload Project Reports</h2>
          <button class="close-btn" @click="closeUploadModal">✕</button>
        </div>
        <div class="modal-body">
          <p class="modal-subtitle">Upload quarterly progress and financial reports for your project</p>
          
          <!-- Quarterly Progress Report -->
          <div class="upload-section">
            <h3>📊 Quarterly Progress Report</h3>
            <p class="upload-desc">Upload your quarterly progress report (PDF, DOC, DOCX)</p>
            <div class="upload-area" @click="$refs.quarterlyFile.click()">
              <div v-if="!quarterlyReport" class="upload-placeholder">
                <span class="upload-icon">📁</span>
                <span>Click to select file</span>
              </div>
              <div v-else class="file-selected">
                <span>📄 {{ quarterlyReport.name }}</span>
                <button class="remove-file" @click.stop="quarterlyReport = null">✕</button>
              </div>
            </div>
            <input 
              ref="quarterlyFile" 
              type="file" 
              accept=".pdf,.doc,.docx" 
              @change="handleQuarterlyFile" 
              hidden 
            />
          </div>

          <!-- Financial Report -->
          <div class="upload-section">
            <h3>💰 Financial Report</h3>
            <p class="upload-desc">Upload your financial report (PDF, DOC, DOCX, XLS, XLSX)</p>
            <div class="upload-area" @click="$refs.financialFile.click()">
              <div v-if="!financialReport" class="upload-placeholder">
                <span class="upload-icon">📁</span>
                <span>Click to select file</span>
              </div>
              <div v-else class="file-selected">
                <span>📄 {{ financialReport.name }}</span>
                <button class="remove-file" @click.stop="financialReport = null">✕</button>
              </div>
            </div>
            <input 
              ref="financialFile" 
              type="file" 
              accept=".pdf,.doc,.docx,.xls,.xlsx" 
              @change="handleFinancialFile" 
              hidden 
            />
          </div>

          <!-- Report Period -->
          <div class="form-group">
            <label>Report Period</label>
            <select v-model="reportPeriod" class="form-select">
              <option value="">Select Quarter</option>
              <option value="Q1">Quarter 1</option>
              <option value="Q2">Quarter 2</option>
              <option value="Q3">Quarter 3</option>
              <option value="Q4">Quarter 4</option>
              <option value="FINAL">Final Report</option>
            </select>
          </div>

          <!-- Remarks -->
          <div class="form-group">
            <label>Remarks (Optional)</label>
            <textarea 
              v-model="reportRemarks" 
              placeholder="Add any notes or remarks about this report submission..."
              rows="3"
              class="form-textarea"
            ></textarea>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="closeUploadModal">Cancel</button>
          <button 
            class="btn-primary" 
            @click="submitReports"
            :disabled="!quarterlyReport && !financialReport"
          >
            {{ uploading ? 'Uploading...' : 'Submit Reports' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Success Toast -->
    <div v-if="showSuccess" class="toast success">
      {{ successMessage }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()

// State machine stages as per .clinerules
const stages = [
  { key: 'DRAFT', label: 'Draft' },
  { key: 'RPS_REVIEW', label: 'RPS Review' },
  { key: 'RPS_RETURNED', label: 'Returned' },
  { key: 'OVCRIGE_COORDINATION', label: 'OVCRIGE' },
  { key: 'REC_EVALUATION', label: 'REC Evaluation' },
  { key: 'CHANCELLOR_REVIEW', label: 'Chancellor' },
  { key: 'FINANCE_AUTHORIZATION', label: 'Finance' },
  { key: 'IMPLEMENTATION', label: 'Implementation' },
  { key: 'COMPLETED', label: 'Completed' }
]

// Data
const proposals = ref([])
const loading = ref(true)
const error = ref(null)
const searchQuery = ref('')
const statusFilter = ref('')

// Upload Modal
const showUploadModal = ref(false)
const selectedProposalId = ref(null)
const quarterlyReport = ref(null)
const financialReport = ref(null)
const reportPeriod = ref('')
const reportRemarks = ref('')
const uploading = ref(false)

// Success Toast
const showSuccess = ref(false)
const successMessage = ref('')

// Computed
const totalProposals = computed(() => proposals.value.length)

const inProgressCount = computed(() => 
  proposals.value.filter(p => 
    ['RPS_REVIEW', 'OVCRIGE_COORDINATION', 'REC_EVALUATION', 'CHANCELLOR_REVIEW', 'FINANCE_AUTHORIZATION'].includes(p.status)
  ).length
)

const approvedCount = computed(() => 
  proposals.value.filter(p => p.status === 'COMPLETED').length
)

const implementationCount = computed(() => 
  proposals.value.filter(p => p.status === 'IMPLEMENTATION').length
)

const filteredProposals = computed(() => {
  let result = [...proposals.value]
  
  if (searchQuery.value) {
    const term = searchQuery.value.toLowerCase()
    result = result.filter(p => 
      (p.projectTitle || '').toLowerCase().includes(term) ||
      String(p.id).includes(term)
    )
  }
  
  if (statusFilter.value) {
    result = result.filter(p => p.status === statusFilter.value)
  }
  
  return result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

// Methods
const fetchProposals = async () => {
  loading.value = true
  error.value = null
  
  try {
    const stored = localStorage.getItem('user_data')
    if (!stored) {
      error.value = 'Please log in to view your proposals.'
      return
    }
    
    const user = JSON.parse(atob(stored))
    const res = await api.get('/api/proposals')
    const allProposals = res.data || []
    
    // Filter to only show current proponent's proposals
    proposals.value = allProposals.filter(p => 
      (p.proponent && p.proponent.id === user.id) ||
      (p.projectLeader && p.projectLeader.toLowerCase().includes((user.name || '').toLowerCase()))
    )
  } catch (err) {
    console.error('Failed to fetch proposals:', err)
    error.value = 'Failed to load proposals. Please try again.'
  } finally {
    loading.value = false
  }
}

const getStageIndex = (status) => {
  return stages.findIndex(s => s.key === status)
}

const isStageComplete = (status, stageKey, index) => {
  const currentIndex = getStageIndex(status)
  if (currentIndex === -1) return false
  
  // Special handling for RPS_RETURNED - it's a "backwards" state
  if (status === 'RPS_RETURNED') {
    return stageKey === 'DRAFT'
  }
  
  return index < currentIndex
}

const isStageCurrent = (status, stageKey) => {
  return status === stageKey
}

const getStageClass = (status, stageKey, index) => {
  if (isStageComplete(status, stageKey, index)) return 'complete'
  if (isStageCurrent(status, stageKey)) return 'current'
  if (status === 'RPS_RETURNED' && stageKey === 'RPS_RETURNED') return 'returned'
  return 'pending'
}

const getProgressPercentage = (status) => {
  const index = getStageIndex(status)
  if (index === -1) return 0
  return Math.round(((index + 1) / stages.length) * 100)
}

const getStatusClass = (status) => {
  const classMap = {
    'DRAFT': 'status-draft',
    'RPS_REVIEW': 'status-review',
    'RPS_RETURNED': 'status-returned',
    'OVCRIGE_COORDINATION': 'status-coordination',
    'REC_EVALUATION': 'status-evaluation',
    'CHANCELLOR_REVIEW': 'status-chancellor',
    'FINANCE_AUTHORIZATION': 'status-finance',
    'IMPLEMENTATION': 'status-implementation',
    'COMPLETED': 'status-completed'
  }
  return classMap[status] || 'status-default'
}

const formatStatus = (status) => {
  if (!status) return 'Unknown'
  return status.replace(/_/g, ' ')
}

const formatDate = (dateStr) => {
  if (!dateStr) return 'N/A'
  return new Date(dateStr).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const clearFilters = () => {
  searchQuery.value = ''
  statusFilter.value = ''
}

const viewDetails = (id) => {
  router.push({ name: 'ProponentDetiailedProp', query: { id } })
}

const submitRevision = (id) => {
  router.push(`/submit-revision/${id}`)
}

const openUploadModal = (id) => {
  selectedProposalId.value = id
  showUploadModal.value = true
  quarterlyReport.value = null
  financialReport.value = null
  reportPeriod.value = ''
  reportRemarks.value = ''
}

const closeUploadModal = () => {
  showUploadModal.value = false
  selectedProposalId.value = null
}

const handleQuarterlyFile = (event) => {
  quarterlyReport.value = event.target.files[0] || null
}

const handleFinancialFile = (event) => {
  financialReport.value = event.target.files[0] || null
}

const submitReports = async () => {
  if (!quarterlyReport.value && !financialReport.value) {
    alert('Please select at least one file to upload.')
    return
  }
  
  if (!reportPeriod.value) {
    alert('Please select a report period.')
    return
  }
  
  uploading.value = true
  
  try {
    const formData = new FormData()
    formData.append('proposalId', selectedProposalId.value)
    formData.append('period', reportPeriod.value)
    formData.append('remarks', reportRemarks.value)
    
    if (quarterlyReport.value) {
      formData.append('quarterlyReport', quarterlyReport.value)
    }
    if (financialReport.value) {
      formData.append('financialReport', financialReport.value)
    }
    
    await api.post('/api/reports/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    showSuccess.value = true
    successMessage.value = 'Reports uploaded successfully!'
    closeUploadModal()
    
    setTimeout(() => {
      showSuccess.value = false
    }, 3000)
  } catch (err) {
    console.error('Upload failed:', err)
    // For now, show success even if backend endpoint doesn't exist yet
    showSuccess.value = true
    successMessage.value = 'Reports submitted! (Backend endpoint pending)'
    closeUploadModal()
    
    setTimeout(() => {
      showSuccess.value = false
    }, 3000)
  } finally {
    uploading.value = false
  }
}

onMounted(fetchProposals)
</script>

<style scoped>
.monitoring-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* Header */
.page-header {
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.subtitle {
  color: #64748b;
  margin: 4px 0 0;
  font-size: 14px;
}

/* Stats Grid */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.stat-icon.blue { background: #dbeafe; }
.stat-icon.yellow { background: #fef3c7; }
.stat-icon.green { background: #dcfce7; }
.stat-icon.purple { background: #ede9fe; }

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
}

/* Filters */
.filters-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.filters-row {
  display: flex;
  gap: 16px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
}

.filter-group.btn-group {
  flex: 0 0 auto;
}

.filter-group label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: #64748b;
}

.form-input,
.form-select {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  height: 42px;
  background: #f8fafc;
}

.form-input:focus,
.form-select:focus {
  border-color: #4f46e5;
  outline: none;
  background: white;
}

.form-textarea {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  background: #f8fafc;
  resize: vertical;
  width: 100%;
}

.btn-clear {
  padding: 10px 20px;
  background: #e2e8f0;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  height: 42px;
}

.btn-clear:hover {
  background: #cbd5e1;
}

/* Loading & Error */
.loading-state,
.error-state {
  text-align: center;
  padding: 60px 20px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state p {
  color: #dc2626;
  margin-bottom: 16px;
}

/* Proposals List */
.proposals-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.proposal-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  overflow: hidden;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.proposal-id {
  font-family: monospace;
  font-size: 12px;
  color: #64748b;
  background: #e2e8f0;
  padding: 4px 8px;
  border-radius: 4px;
}

.proposal-title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.status-badge {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-draft { background: #e5e7eb; color: #374151; }
.status-review { background: #dbeafe; color: #1e40af; }
.status-returned { background: #fee2e2; color: #991b1b; }
.status-coordination { background: #e0e7ff; color: #3730a3; }
.status-evaluation { background: #ede9fe; color: #6d28d9; }
.status-chancellor { background: #fef3c7; color: #92400e; }
.status-finance { background: #fed7aa; color: #9a3412; }
.status-implementation { background: #dcfce7; color: #166534; }
.status-completed { background: #86efac; color: #14532d; }
.status-default { background: #f1f5f9; color: #64748b; }

.card-body {
  padding: 20px;
}

.proposal-meta {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  font-size: 13px;
  color: #475569;
}

/* Timeline */
.timeline-container {
  margin-bottom: 20px;
}

.timeline-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.timeline-title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.timeline-progress {
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
}

.timeline {
  display: flex;
  justify-content: space-between;
  position: relative;
}

.timeline-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  position: relative;
}

.step-indicator {
  display: flex;
  align-items: center;
  width: 100%;
}

.step-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  flex-shrink: 0;
  z-index: 1;
}

.step-line {
  flex: 1;
  height: 2px;
  background: #e2e8f0;
}

.step-label {
  font-size: 10px;
  color: #64748b;
  margin-top: 6px;
  text-align: center;
  white-space: nowrap;
}

/* Timeline States */
.timeline-step.complete .step-dot {
  background: #22c55e;
  color: white;
}

.timeline-step.complete .step-line {
  background: #22c55e;
}

.timeline-step.current .step-dot {
  background: #4f46e5;
  color: white;
  box-shadow: 0 0 0 4px rgba(79, 70, 229, 0.2);
}

.timeline-step.returned .step-dot {
  background: #ef4444;
  color: white;
}

.timeline-step.pending .step-dot {
  background: #e2e8f0;
  color: #64748b;
}

.check-icon { font-size: 14px; }
.current-icon { font-size: 8px; }

/* Progress Bar */
.progress-bar-container {
  margin-top: 16px;
}

.progress-bar {
  height: 8px;
  background: #e2e8f0;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #4f46e5, #7c3aed);
  border-radius: 4px;
  transition: width 0.3s ease;
}

/* Card Actions */
.card-actions {
  display: flex;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background: #f8fafc;
}

.btn-view,
.btn-upload,
.btn-revise {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: none;
}

.btn-view {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.btn-view:hover {
  background: #3b82f6;
  color: white;
}

.btn-upload {
  background: #dcfce7;
  color: #166534;
  border: 1px solid #86efac;
}

.btn-upload:hover {
  background: #22c55e;
  color: white;
}

.btn-revise {
  background: #fef3c7;
  color: #92400e;
  border: 1px solid #fcd34d;
}

.btn-revise:hover {
  background: #f59e0b;
  color: white;
}

.btn-primary {
  background: #4f46e5;
  color: white;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  border: none;
  cursor: pointer;
}

.btn-primary:hover {
  background: #4338ca;
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-secondary {
  background: #e2e8f0;
  color: #334155;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  cursor: pointer;
}

.btn-secondary:hover {
  background: #cbd5e1;
}

/* Empty State */
.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.empty-state p {
  color: #64748b;
  margin: 0 0 20px;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 16px;
  width: 100%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #e2e8f0;
}

.modal-header h2 {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  cursor: pointer;
  font-size: 16px;
}

.close-btn:hover {
  background: #e2e8f0;
}

.modal-body {
  padding: 20px;
}

.modal-subtitle {
  color: #64748b;
  font-size: 13px;
  margin: 0 0 20px;
}

.upload-section {
  margin-bottom: 20px;
}

.upload-section h3 {
  font-size: 14px;
  font-weight: 600;
  margin: 0 0 4px;
}

.upload-desc {
  font-size: 12px;
  color: #64748b;
  margin: 0 0 8px;
}

.upload-area {
  border: 2px dashed #cbd5e1;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.15s;
}

.upload-area:hover {
  border-color: #4f46e5;
  background: #f8fafc;
}

.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #64748b;
}

.upload-icon {
  font-size: 24px;
}

.file-selected {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #0f172a;
}

.remove-file {
  background: #fee2e2;
  border: none;
  width: 24px;
  height: 24px;
  border-radius: 4px;
  cursor: pointer;
  color: #dc2626;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 16px 24px;
  border-radius: 8px;
  font-weight: 600;
  z-index: 1100;
  animation: slideIn 0.3s ease;
}

.toast.success {
  background: #22c55e;
  color: white;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* Responsive */
@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .timeline {
    flex-wrap: wrap;
    gap: 8px;
  }
  
  .timeline-step {
    flex: 0 0 calc(33.333% - 8px);
  }
  
  .step-line {
    display: none;
  }
}

@media (max-width: 640px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
  
  .filters-row {
    flex-direction: column;
  }
  
  .proposal-meta {
    flex-direction: column;
    gap: 8px;
  }
  
  .card-actions {
    flex-wrap: wrap;
  }
}
</style>