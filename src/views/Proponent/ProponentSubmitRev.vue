<template>
  <div class="revision-page">
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
          <h1>Submit Revision</h1>
          <p class="subtitle">Proposal #{{ proposalId }}</p>
        </div>
      </div>
    </header>

    <!-- ERROR MESSAGE -->
    <div v-if="error" class="error-banner">
      <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- SUCCESS MESSAGE -->
    <div v-if="successMessage" class="success-banner">
      <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
      <span>{{ successMessage }}</span>
    </div>

    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- REVISION FORM -->
    <template v-else-if="proposalId">
      <div class="content-grid">
        <!-- LEFT COLUMN: Main Form -->
        <div class="main-content">
          <!-- Project Profile Section -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">01</span>
              <h2>Project Profile</h2>
            </div>
            <div class="card-body">
              <div class="info-grid">
                <div class="info-item">
                  <label>Program Title *</label>
                  <input v-model="formData.programTitle" type="text" required />
                </div>
                <div class="info-item">
                  <label>Project Title *</label>
                  <input v-model="formData.projectTitle" type="text" required />
                </div>
                <div class="info-item">
                  <label>Project Leader *</label>
                  <input v-model="formData.projectLeader" type="text" required />
                </div>
                <div class="info-item">
                  <label>Duration (months) *</label>
                  <input v-model="formData.duration" type="text" required />
                </div>
                <div class="info-item">
                  <label>Start Date *</label>
                  <input v-model="formData.startDate" type="date" required />
                </div>
                <div class="info-item">
                  <label>End Date *</label>
                  <input v-model="formData.endDate" type="date" required />
                </div>
                <div class="info-item full-width">
                  <label>Implementing College / Department *</label>
                  <input v-model="formData.college" type="text" required />
                </div>
                <div class="info-item full-width">
                  <label>Address / Contact</label>
                  <input v-model="formData.address" type="text" />
                </div>
              </div>
            </div>
          </section>

          <!-- Research Details Section -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">02</span>
              <h2>Research Details</h2>
            </div>
            <div class="card-body">
              <div class="form-group">
                <label>Cooperating Agencies</label>
                <textarea v-model="formData.cooperatingAgencies" rows="3"></textarea>
              </div>
              <div class="form-group">
                <label>Type of Research</label>
                <select v-model="formData.researchType">
                  <option value="">Select type</option>
                  <option value="BASIC">Basic Research</option>
                  <option value="APPLIED">Applied Research</option>
                  <option value="DEVELOPMENT">Development Research</option>
                </select>
              </div>
              <div class="form-group">
                <label>Innovation Goals</label>
                <textarea v-model="formData.innovationGoals" rows="4"></textarea>
              </div>
              <div class="form-group">
                <label>Sector Relevance</label>
                <textarea v-model="formData.sectorRelevance" rows="4"></textarea>
              </div>
              <div class="form-group">
                <label>Sustainable Development Goals</label>
                <textarea v-model="formData.sdg" rows="3"></textarea>
              </div>
            </div>
          </section>

          <!-- Executive Summary & Introduction -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">03</span>
              <h2>Executive Summary & Introduction</h2>
            </div>
            <div class="card-body">
              <div class="form-group">
                <label>Executive Summary</label>
                <textarea v-model="formData.executiveSummary" rows="5"></textarea>
              </div>
              <div class="form-group">
                <label>Rationale / Significance</label>
                <textarea v-model="formData.rationale" rows="4"></textarea>
              </div>
              <div class="form-group">
                <label>Scientific Basis / Theoretical Framework</label>
                <textarea v-model="formData.framework" rows="4"></textarea>
              </div>
              <div class="form-group">
                <label>General Objective</label>
                <textarea v-model="formData.objectivesGeneral" rows="3"></textarea>
              </div>
              <div class="form-group">
                <label>Specific Objectives</label>
                <textarea v-model="formData.objectivesSpecific" rows="4"></textarea>
              </div>
            </div>
          </section>

          <!-- Methodology Section -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">04</span>
              <h2>Methodology</h2>
            </div>
            <div class="card-body">
              <div class="form-group">
                <label>Methodology</label>
                <textarea v-model="formData.methodology" rows="6"></textarea>
              </div>
            </div>
          </section>
        </div>

        <!-- RIGHT COLUMN: Sidebar -->
        <aside class="sidebar">
          <!-- Info Card -->
          <div class="sidebar-card">
            <h3>Revision Information</h3>
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">Proposal ID</span>
                <span class="info-value">#{{ proposalId }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">Status</span>
                <span class="info-value status-badge revision">REVISION</span>
              </div>
            </div>
          </div>

          <!-- Instructions Card -->
          <div class="sidebar-card">
            <h3>Instructions</h3>
            <ul class="instructions-list">
              <li>Review the feedback from reviewers</li>
              <li>Update the required sections based on the feedback</li>
              <li>Make sure all changes are addressed</li>
              <li>Submit the revised proposal for re-evaluation</li>
            </ul>
          </div>

          <!-- Action Buttons -->
          <div class="sidebar-card action-card">
            <h3>Actions</h3>
            <button class="btn-action btn-cancel" @click="goBack">
              ← Cancel
            </button>
            <button class="btn-action btn-draft" @click="saveDraft">
              💾 Save Draft
            </button>
            <button 
              class="btn-action btn-submit" 
              @click="submitRevision"
              :disabled="loading"
            >
              {{ loading ? 'Submitting...' : '✓ Submit Revision' }}
            </button>
          </div>
        </aside>
      </div>
    </template>

    <!-- NO PROPOSAL ID -->
    <div v-else class="no-proposal-message">
      <p>No proposal selected for revision.</p>
      <button class="btn-primary" @click="$router.push('/revisions')">
        Go to Revisions
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'

const route = useRoute()

// Get proposal ID from route params
const proposalId = computed(() => route.params.id as string | undefined)

// Form data for revision submission
const formData = ref({
  programTitle: '',
  projectTitle: '',
  projectLeader: '',
  duration: '',
  startDate: '',
  endDate: '',
  college: '',
  address: '',
  cooperatingAgencies: '',
  researchType: '',
  innovationGoals: '',
  sectorRelevance: '',
  sdg: '',
  executiveSummary: '',
  rationale: '',
  framework: '',
  objectivesGeneral: '',
  objectivesSpecific: '',
  methodology: '',
  reviewFileName: '',
  roadmapFileName: '',
  gadFileName: '',
  beneficiariesFileName: ''
})

const loading = ref(false)
const error = ref('')
const successMessage = ref('')

// Fetch existing proposal data
const fetchProposal = async () => {
  if (!proposalId.value) return
  
  loading.value = true
  error.value = ''
  
  try {
    const res = await api.get(`/api/proposals/${proposalId.value}`)
    const proposal = res.data
    
    // Populate form with existing data
    formData.value = {
      programTitle: proposal.programTitle || '',
      projectTitle: proposal.projectTitle || '',
      projectLeader: proposal.projectLeader || '',
      duration: proposal.duration || '',
      startDate: proposal.startDate || '',
      endDate: proposal.endDate || '',
      college: proposal.college || '',
      address: proposal.address || '',
      cooperatingAgencies: proposal.cooperatingAgencies || '',
      researchType: proposal.researchType || '',
      innovationGoals: proposal.innovationGoals || '',
      sectorRelevance: proposal.sectorRelevance || '',
      sdg: proposal.sdg || '',
      executiveSummary: proposal.executiveSummary || '',
      rationale: proposal.rationale || '',
      framework: proposal.framework || '',
      objectivesGeneral: proposal.objectivesGeneral || '',
      objectivesSpecific: proposal.objectivesSpecific || '',
      methodology: proposal.methodology || '',
      reviewFileName: proposal.reviewFileName || '',
      roadmapFileName: proposal.roadmapFileName || '',
      gadFileName: proposal.gadFileName || '',
      beneficiariesFileName: proposal.beneficiariesFileName || ''
    }
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

const submitRevision = async () => {
  if (!proposalId.value) {
    error.value = 'No proposal ID provided.'
    return
  }
  
  loading.value = true
  error.value = ''
  successMessage.value = ''
  
  try {
    // Update proposal with revised data and set status to SUBMITTED
    await api.put(`/api/proposals/${proposalId.value}`, {
      ...formData.value,
      status: 'SUBMITTED'
    })
    
    successMessage.value = 'Revision submitted successfully!'
    
    // Redirect to revisions page after 2 seconds
    setTimeout(() => {
      window.location.href = '/revisions'
    }, 2000)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to submit revision. Please try again.'
  } finally {
    loading.value = false
  }
}

const saveDraft = () => {
  alert('Draft saved successfully!')
}

const goBack = () => {
  window.history.back()
}

onMounted(() => {
  fetchProposal()
})
</script>

<style scoped>
.revision-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
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
}

.back-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* ERROR & SUCCESS BANNERS */
.error-banner, .success-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.success-banner {
  background: #dcfce7;
  border: 1px solid #86efac;
  color: #166534;
}

.error-icon, .success-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* LOADING STATE */
.loading-state {
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
  gap: 6px;
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

.info-item input,
.info-item select {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  transition: all 0.15s;
}

.info-item input:focus,
.info-item select:focus,
.info-item textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

/* FORM GROUP */
.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  font-family: inherit;
  transition: all 0.15s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
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

/* INFO LIST */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.revision {
  background: #e0e7ff;
  color: #3730a3;
}

/* INSTRUCTIONS */
.instructions-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.instructions-list li {
  padding: 8px 0;
  padding-left: 20px;
  position: relative;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.instructions-list li:before {
  content: "•";
  position: absolute;
  left: 0;
  color: #4f46e5;
  font-weight: bold;
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

.btn-action.btn-cancel {
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-action.btn-cancel:hover {
  background: #f1f5f9;
}

.btn-action.btn-draft {
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-action.btn-draft:hover {
  background: #f1f5f9;
}

.btn-action.btn-submit {
  background: #4f46e5;
  color: #fff;
}

.btn-action.btn-submit:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-action.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* NO PROPOSAL MESSAGE */
.no-proposal-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.no-proposal-message p {
  font-size: 16px;
  color: #64748b;
}

.btn-primary {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:hover {
  background: #4338ca;
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

  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
}
</style>