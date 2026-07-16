<template>
  <div v-if="modelValue" class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <div class="header-left">
          <h2>Review & Submit</h2>
          <span class="header-badge">Step 3 of 3</span>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="close">✕</button>
        </div>
      </header>

      <!-- PROGRESS BAR -->
      <div class="progress-bar">
        <div class="progress-step completed" @click="$emit('goToStep', 1)">
          <span class="step-number">✓</span>
          <span class="step-label">Proposal Details</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step completed" @click="$emit('goToStep', 2)">
          <span class="step-number">✓</span>
          <span class="step-label">Certification</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step active" @click="$emit('goToStep', 3)">
          <span class="step-number">3</span>
          <span class="step-label">Review & Submit</span>
        </div>
      </div>

      <!-- VALIDATION ERROR BANNER -->
      <div v-if="validationErrors.length > 0" class="validation-banner" ref="errorBannerRef">
        <div class="banner-icon">⚠</div>
        <div class="banner-content">
          <strong>Proposal is incomplete. Please go back and fill in the missing fields.</strong>
          <ul>
            <li v-for="(err, i) in validationErrors" :key="i">{{ err }}</li>
          </ul>
        </div>
      </div>

      <!-- BODY -->
      <div class="modal-body" ref="modalBodyRef">
        <!-- Loading State -->
        <div v-if="submitting" class="loading-state">
          <div class="spinner"></div>
          <p>{{ submitMessage }}</p>
        </div>

        <!-- Success State -->
        <div v-else-if="submitted" class="success-state">
          <div class="success-icon">✓</div>
          <h3>Proposal Submitted Successfully!</h3>
          <p>Your proposal has been submitted for review. You can track its status from your dashboard.</p>
          <button class="btn-primary" @click="close">Done</button>
        </div>

        <!-- Review Content -->
        <template v-else>
          <!-- Summary Section -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">01</span>
              <h3>Proposal Summary</h3>
            </div>
            <div class="section-card">
              <div class="summary-grid">
                <div class="summary-item">
                  <span class="summary-label">Program Title</span>
                  <span class="summary-value">{{ proposalData.program_title || 'Not provided' }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Project Title</span>
                  <span class="summary-value">{{ proposalData.project_title || 'Not provided' }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Project Leader</span>
                  <span class="summary-value">{{ proposalData.project_leader || 'Not provided' }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Duration</span>
                  <span class="summary-value">{{ proposalData.duration || 'Not provided' }} months</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Department</span>
                  <span class="summary-value">{{ proposalData.department || 'Not provided' }}</span>
                </div>
                <div class="summary-item">
                  <span class="summary-label">Research Type</span>
                  <span class="summary-value">{{ proposalData.research_type || 'Not selected' }}</span>
                </div>
              </div>
            </div>
          </section>

          <!-- Files Summary -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">02</span>
              <h3>Uploaded Documents</h3>
            </div>
            <div class="section-card">
              <div class="files-summary">
                <div class="file-row" :class="{ attached: proposalData.review_of_literature_file }">
                  <span class="file-name">Review of Literature</span>
                  <span class="file-status">{{ proposalData.review_of_literature_file ? '✓ Attached' : '— Not attached'
                    }}</span>
                </div>
                <div class="file-row" :class="{ attached: proposalData.technology_roadmap_file }">
                  <span class="file-name">Technology Roadmap</span>
                  <span class="file-status">{{ proposalData.technology_roadmap_file ? '✓ Attached' : '— Not attached'
                    }}</span>
                </div>
                <div class="file-row" :class="{ attached: proposalData.gad_score_file }">
                  <span class="file-name">GAD Score</span>
                  <span class="file-status">{{ proposalData.gad_score_file ? '✓ Attached' : '— Not attached' }}</span>
                </div>
                <div class="file-row" :class="{ attached: proposalData.line_item_budget_file }">
                  <span class="file-name">Line-Item Budget</span>
                  <span class="file-status">{{ proposalData.line_item_budget_file ? '✓ Attached' : '— Not attached'
                    }}</span>
                </div>
              </div>
            </div>
          </section>

          <!-- Confirm Checkbox -->
          <section class="form-section">
            <div class="section-card confirm-card">
              <label class="confirm-checkbox">
                <input type="checkbox" v-model="confirmed" />
                <span class="checkmark"></span>
                <span class="confirm-text">
                  I confirm that all information provided is accurate and complete. I understand that
                  submitting this proposal constitutes a formal application for research funding.
                </span>
              </label>
            </div>
          </section>

          <!-- Error message from API -->
          <div v-if="error" class="error-banner">
            <p>{{ error }}</p>
          </div>
        </template>
      </div>

      <!-- FOOTER (only show when not submitting/submitted) -->
      <footer v-if="!submitting && !submitted" class="modal-footer">
        <button class="btn-secondary" @click="goBack" :disabled="submitting">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back
        </button>
        <div class="footer-right">
          <button class="btn-outline" @click="saveAsDraftLocal" :disabled="submitting">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
              <polyline points="17 21 17 13 7 13 7 21" />
              <polyline points="7 3 7 8 15 8" />
            </svg>
            Save as Draft
          </button>
          <button class="btn-primary" @click="submitProposal" :disabled="!confirmed || submitting">
            Submit Proposal
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M5 12h14" />
              <path d="M12 5l7 7-7 7" />
            </svg>
          </button>
        </div>
      </footer>
    </div>

    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const props = defineProps({
  modelValue: Boolean,
  proposalData: {
    type: Object,
    default: () => ({})
  }
})
const emit = defineEmits(['update:modelValue', 'back', 'savedraft', 'goToStep'])

const userStore = useUserDataStore()

const modalBodyRef = ref(null)
const errorBannerRef = ref(null)
const confirmed = ref(false)
const submitting = ref(false)
const submitted = ref(false)
const submitMessage = ref('')
const error = ref(null)

const resetForm = () => {
  confirmed.value = false
  submitted.value = false
  submitting.value = false
  submitMessage.value = ''
  error.value = null
}

defineExpose({ resetForm })

const close = () => emit('update:modelValue', false)

const goBack = () => {
  emit('back')
}

// ===== FINAL COMPLETENESS GUARD =====
const isBlank = (val) => !val || (typeof val === 'string' && val.trim() === '')

const validationErrors = computed(() => {
  const errs = []
  const d = props.proposalData || {}

  // Step 1 fields
  if (isBlank(d.program_title)) errs.push('Program Title is missing.')
  if (isBlank(d.project_title)) errs.push('Project Title is missing.')
  if (isBlank(d.project_leader)) errs.push('Project Leader is missing.')
  if (isBlank(d.project_leader_sex)) errs.push('Sex is not selected.')
  if (isBlank(d.duration)) errs.push('Duration is missing.')
  if (isBlank(d.start_date)) errs.push('Start Date is missing.')
  if (isBlank(d.end_date)) errs.push('End Date is missing.')
  if (isBlank(d.department)) errs.push('Department is missing.')
  if (isBlank(d.address)) errs.push('Address / Contact is missing.')
  if (isBlank(d.cooperating_agencies)) errs.push('Cooperating Agencies is missing.')

  // Sites
  if (!d.sites || !d.sites.length) {
    errs.push('No site of implementation provided.')
  } else {
    const siteFields = ['country', 'region', 'province', 'district', 'municipality', 'barangay']
    if (d.sites.some(s => siteFields.some(f => isBlank(s[f])))) {
      errs.push('Site of Implementation has incomplete fields.')
    }
  }

  if (isBlank(d.research_type)) errs.push('Research Type is not selected.')

  // Priority Agenda
  const agendas = d.priority_agendas || {}
  const selectedAgendas = Object.entries(agendas).filter(([, v]) => v.selected)
  if (selectedAgendas.length === 0) {
    errs.push('No Priority Agenda selected.')
  } else if (selectedAgendas.some(([, v]) => isBlank(v.value))) {
    errs.push('Priority Agenda details are incomplete.')
  }

  if (isBlank(d.innovation_goals)) errs.push('Innovation Goals is missing.')
  if (isBlank(d.sector_relevance)) errs.push('Sector Relevance is missing.')
  if (isBlank(d.sustainable_development_goals)) errs.push('SDG is missing.')
  if (isBlank(d.executive_summary)) errs.push('Executive Summary is missing.')
  if (isBlank(d.rationale)) errs.push('Rationale is missing.')
  if (isBlank(d.theoretical_framework)) errs.push('Theoretical Framework is missing.')
  if (isBlank(d.general_objective)) errs.push('General Objective is missing.')
  if (isBlank(d.specific_objectives)) errs.push('Specific Objectives is missing.')
  if (!d.review_of_literature_file) errs.push('Review of Literature file is missing.')
  if (isBlank(d.methodology)) errs.push('Methodology is missing.')
  if (isBlank(d.expected_outputs)) errs.push('Expected Outputs is missing.')
  if (isBlank(d.potential_outcomes)) errs.push('Potential Outcomes is missing.')
  if (isBlank(d.economic_impact)) errs.push('Economic Impact is missing.')
  if (isBlank(d.social_ethical_impact)) errs.push('Social / Ethical Impact is missing.')
  if (isBlank(d.target_beneficiaries)) errs.push('Target Beneficiaries is missing.')
  if (isBlank(d.sustainability_plan)) errs.push('Sustainability Plan is missing.')
  if (!d.gad_score_file) errs.push('GAD Score file is missing.')
  if (isBlank(d.limitations)) errs.push('Limitations is missing.')
  if (isBlank(d.risks_assumptions)) errs.push('Risk Management Plan is missing.')

  // Logical Framework
  if (!d.logical_framework || !d.logical_framework.length) {
    errs.push('Logical Framework is missing.')
  } else if (d.logical_framework.some(r => isBlank(r.outcome_indicator) || isBlank(r.output_indicator))) {
    errs.push('Logical Framework has incomplete rows.')
  }

  if (isBlank(d.literature_cited)) errs.push('Literature Cited is missing.')

  // Personnel
  if (!d.personnel_requirements || !d.personnel_requirements.length) {
    errs.push('Personnel Requirements is missing.')
  } else if (d.personnel_requirements.some(r => isBlank(r.position) || isBlank(r.effort) || isBlank(r.responsibilities))) {
    errs.push('Personnel Requirements has incomplete rows.')
  }

  if (!d.line_item_budget_file) errs.push('Line-Item Budget file is missing.')
  if (isBlank(d.other_projects_number)) errs.push('Number of Other Projects is missing.')

  return errs
})

const saveAsDraftLocal = async () => {
  submitting.value = true
  submitMessage.value = 'Saving draft...'
  error.value = null
  try {
    await saveOrUpdateProposal('DRAFT')
    emit('savedraft')
    await showAlert('Proposal saved as draft successfully!', { type: 'success', title: 'Draft Saved' })
  } catch (err) {
    error.value = 'Failed to save draft. Please try again.'
  } finally {
    submitting.value = false
  }
}

const submitProposal = async () => {
  if (!confirmed.value) return

  // Final completeness check
  if (validationErrors.value.length > 0) {
    await showAlert(
      `Your proposal has ${validationErrors.value.length} missing field(s). Please go back and complete all sections before submitting.`,
      { type: 'warning', title: 'Incomplete Proposal' }
    )
    return
  }

  submitting.value = true
  submitMessage.value = 'Submitting proposal...'
  error.value = null

  try {
    await saveOrUpdateProposal('SUBMITTED')
    submitted.value = true
    submitMessage.value = 'Proposal submitted!'
  } catch (err) {
    error.value = err.response?.data || 'Failed to submit proposal. Please try again.'
  } finally {
    submitting.value = false
  }
}

const saveOrUpdateProposal = async (status) => {
  const data = props.proposalData
  const proponentId = userStore.user?.id

  if (!proponentId) {
    throw new Error('User not logged in')
  }

  const payload = {
    proponentId,
    programTitle: data.program_title || '',
    projectTitle: data.project_title || '',
    projectLeader: data.project_leader || '',
    duration: data.duration || '',
    startDate: data.start_date || '',
    endDate: data.end_date || '',
    college: data.department || '',
    address: data.address || '',
    cooperatingAgencies: data.cooperating_agencies || '',
    researchType: data.research_type || '',
    innovationGoals: data.innovation_goals || '',
    sectorRelevance: data.sector_relevance || '',
    sdg: data.sustainable_development_goals || '',
    executiveSummary: data.executive_summary || '',
    rationale: data.rationale || '',
    framework: data.theoretical_framework || '',
    objectivesGeneral: data.general_objective || '',
    objectivesSpecific: data.specific_objectives || '',
    methodology: data.methodology || '',
    outputs: data.expected_outputs || '',
    outcomes: data.potential_outcomes || '',
    impactEconomic: data.economic_impact || '',
    impactSocial: data.social_ethical_impact || '',
    beneficiaries: data.target_beneficiaries || '',
    sustainability: data.sustainability_plan || '',
    risks: data.risks_assumptions || '',
    otherProjectsNumber: data.other_projects_number || '',
    status,
    sites: (data.sites || []).map(s => ({
      country: s.country || '',
      region: s.region || '',
      province: s.province || '',
      district: s.district || '',
      municipality: s.municipality || '',
      barangay: s.barangay || ''
    })),
    priorityAgenda: Object.fromEntries(
      Object.entries(data.priority_agendas || {}).map(([key, val]) => [
        key,
        { selected: val.selected || false, value: val.value || '' }
      ])
    ),
    logFrame: (data.logical_framework || []).map(lf => ({
      outcome: lf.outcome_indicator || '',
      output: lf.output_indicator || ''
    })),
    personnel: (data.personnel_requirements || []).map(p => ({
      position: p.position || '',
      time: p.effort || '',
      responsibilities: p.responsibilities || ''
    })),
    otherProjects: (data.other_projects || []).map(op => ({
      title: op.project_title || '',
      agency: op.funding_agency || '',
      involvement: op.involvement || ''
    }))
  }

  // If we have a proposal_id, update; otherwise create
  if (data.proposal_id) {
    await api.put(`/api/proposals/${data.proposal_id}`, payload)
  } else {
    await api.post('/api/proposals', payload)
  }
}
</script>

<style scoped>
/* ===== BACKDROP ===== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

/* ===== CONTAINER ===== */
.modal-container {
  background: #f8fafc;
  width: 100%;
  max-width: 800px;
  max-height: 92vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ===== HEADER ===== */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-badge {
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

/* ===== PROGRESS BAR ===== */
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: #94a3b8;
  transition: all 0.2s;
  flex-shrink: 0;
}

.progress-step.active .step-number {
  background: #4f46e5;
  color: #fff;
}

.progress-step.completed .step-number {
  background: #10b981;
  color: #fff;
  font-size: 12px;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  white-space: nowrap;
}

.progress-step.active .step-label {
  color: #1e293b;
  font-weight: 600;
}

.progress-step.completed .step-label {
  color: #10b981;
  font-weight: 600;
}

.progress-line {
  width: 60px;
  height: 2px;
  background: #e2e8f0;
  margin: 0 12px;
  flex-shrink: 0;
}

.progress-line.filled {
  background: #10b981;
}

/* ===== BODY ===== */
.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

/* ===== FORM SECTIONS ===== */
.form-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.section-number {
  font-size: 13px;
  font-weight: 700;
  color: #4f46e5;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.15s;
}

/* ===== SUMMARY GRID ===== */
.summary-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.summary-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.summary-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

/* ===== FILES SUMMARY ===== */
.files-summary {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.file-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #f1f5f9;
}

.file-row:last-child {
  border-bottom: none;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #475569;
}

.file-status {
  font-size: 13px;
  color: #94a3b8;
}

.file-row.attached .file-status {
  color: #10b981;
  font-weight: 600;
}

/* ===== CONFIRM CARD ===== */
.confirm-card {
  background: #f8faff;
  border: 2px solid #e2e8f0;
}

.confirm-checkbox {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
}

.confirm-checkbox input {
  display: none;
}

.checkmark {
  width: 22px;
  height: 22px;
  min-width: 22px;
  border: 2px solid #cbd5e1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  margin-top: 1px;
}

.confirm-checkbox input:checked+.checkmark {
  background: #4f46e5;
  border-color: #4f46e5;
}

.confirm-checkbox input:checked+.checkmark::after {
  content: '✓';
  color: #fff;
  font-size: 14px;
  font-weight: 700;
}

.confirm-text {
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
}

/* ===== LOADING / SUCCESS STATES ===== */
.loading-state,
.success-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
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
  to {
    transform: rotate(360deg);
  }
}

.success-icon {
  width: 64px;
  height: 64px;
  background: #ecfdf5;
  color: #10b981;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  font-weight: 700;
}

.success-state h3 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.success-state p {
  color: #64748b;
  max-width: 400px;
  line-height: 1.6;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 10px;
  padding: 12px 16px;
  color: #dc2626;
  font-size: 14px;
}

/* ===== FOOTER ===== */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #d1d5db;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline:hover {
  border-color: #4f46e5;
  color: #4f46e5;
  background: #f8faff;
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #4f46e5;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

/* ===== VALIDATION BANNER ===== */
.validation-banner {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 4px solid #dc2626;
  margin: 0;
  flex-shrink: 0;
}

.banner-icon {
  font-size: 20px;
  color: #dc2626;
  flex-shrink: 0;
  line-height: 1;
}

.banner-content {
  flex: 1;
}

.banner-content strong {
  display: block;
  font-size: 14px;
  color: #991b1b;
  margin-bottom: 6px;
}

.banner-content ul {
  margin: 0;
  padding-left: 18px;
  list-style: disc;
}

.banner-content li {
  font-size: 12px;
  color: #b91c1c;
  margin-bottom: 2px;
  line-height: 1.4;
}

/* ===== SCROLLBAR ===== */
.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
