<template>
  <div class="proposal-detail-page rv-page">
    <!-- HEADER: the shared review header - Back, title, id, status and DOWNLOAD. The
         handlers stay this page's own; only the chrome is shared. -->
    <ProposalReviewHeader
      :title="proposal.projectTitle"
      :document-id="proposal.documentId || proposal.proposalCode"
      :status="proposal.status"
      @back="goBack"
      :download-loading="downloadLoading"
      @download="downloadProposal"
    />
    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-secondary" @click="goBack">Go Back</button>
    </div>

    <!-- Content -->
    <template v-else-if="proposal.id">
      <div class="rv-body">
        <!-- LEFT COLUMN: Main Content -->
        <div class="rv-main">
          <!-- Every reviewable field, rendered from the shared field registry
               (src/utils/proposalFields.js) so each field has its own remark control and
               the correct entity/DTO key. Sections 01-27 are covered exhaustively. -->
          <ProposalFieldSection
            v-for="section in PROPOSAL_FIELD_SECTIONS"
            :key="section.key"
            :section="section"
            :proposal="proposal"
            :proposal-id="$route.params.id"
            :remark-index="remarkIndex"
            :editable="true"
            @download="downloadFile"
            @remark-saved="onRemarkSaved"
            @remark-removed="onRemarkRemoved"
          />

          <!-- Section 19: the completed GAD Checklist 2 (read-only). Every reviewer sees the
               17 responses, their scores, the comments and the total GAD score. -->
          <div v-if="proposal.gadChecklist" class="rv-gad-card">
            <GadChecklist :model-value="proposal.gadChecklist" readonly />
          </div>
        </div>

        <!-- RIGHT COLUMN: Sidebar -->
        <aside class="rv-aside">
          <!-- Proposal Status, Quick Information and Review Information: the shared summary
               cards. The values are the ones this page already loaded. -->
          <ProposalReviewSidebar
            :status="proposal.status"
            :created-at="proposal.createdAt"
            :updated-at="proposal.updatedAt"
            :quick="quickRows"
            :review="reviewRows"
          />

          <!-- Status Timeline -->
          <div class="sidebar-card">
            <h3>Status Timeline</h3>
            <ul class="timeline">
              <li>
                <span class="dot completed"></span>
                <div>
                  <strong>Proposal Submitted</strong>
                  <small>{{ formatDate(proposal.createdAt) }}</small>
                </div>
              </li>
              <li v-if="proposal.status !== 'SUBMITTED'" :class="{ active: proposal.status === 'ENDORSED' }">
                <span class="dot"
                  :class="{ completed: ['ENDORSED', 'UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>RPS Endorsed</strong>
                  <small>{{ proposal.status === 'ENDORSED' || ['UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL',
                    'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) ? 'Completed' : 'Pending' }}</small>
                </div>
              </li>
              <li
                v-if="['UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'UNDER_REVIEW' }">
                <span class="dot"
                  :class="{ completed: ['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>Forwarded to REC</strong>
                  <small>{{ ['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL',
                    'APPROVED'].includes(proposal.status) ? 'Completed' : 'In Progress' }}</small>
                </div>
              </li>
              <li v-if="['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'REC_APPROVED' }">
                <span class="dot"
                  :class="{ completed: ['FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>REC Approved</strong>
                  <small>{{ ['FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) ?
                    'Completed' : 'Current' }}</small>
                </div>
              </li>
              <li v-if="['FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'FOR_OC_APPROVAL' }">
                <span class="dot" :class="{ completed: proposal.status === 'APPROVED' }"></span>
                <div>
                  <strong>Forwarded to Chancellor</strong>
                  <small>{{ proposal.status === 'APPROVED' ? 'Completed' : 'In Progress' }}</small>
                </div>
              </li>
              <li v-if="proposal.status === 'APPROVED'" class="active">
                <span class="dot completed"></span>
                <div>
                  <strong>Final Approval</strong>
                  <small>Completed</small>
                </div>
              </li>
            </ul>
          </div>

          <!-- OVCRIGE Actions -->
          <div class="sidebar-card action-card">
            <h3>OVCRIGE Actions</h3>
            <button v-if="proposal.status === 'ENDORSED'" class="btn-action btn-primary" @click="forwardToRec"
              :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Forward to REC for Evaluation' }}
            </button>
            <button v-if="proposal.status === 'REC_APPROVED'" class="btn-action btn-primary" @click="forwardToOc"
              :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Forward to Chancellor (OC) for Final Approval' }}
            </button>
            <button class="btn-action btn-warning" @click="returnForRevision" :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Return for Revision' }}
            </button>
            <button class="btn-action btn-danger" @click="rejectProposal" :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Reject Proposal' }}
            </button>
          </div>

          <!-- Attachments -->
          <div class="sidebar-card">
            <h3>Attachments</h3>
            <ul class="files-list">
              <li v-if="proposal.reviewFileName" class="file-item">
                <span>📄 Review of Literature</span>
                <button class="download-btn btn-download" @click="downloadFile(proposal.reviewFileName)">DOWNLOAD</button>
              </li>
              <li v-if="proposal.roadmapFileName" class="file-item">
                <span>📄 Technology Roadmap</span>
                <button class="download-btn btn-download"
                  @click="downloadFile(proposal.roadmapFilePath || proposal.roadmapFileName)">DOWNLOAD</button>
              </li>
              <li v-if="proposal.gadFileName" class="file-item">
                <span>📄 GAD Score</span>
                <button class="download-btn btn-download" @click="downloadFile(proposal.gadFileName)">DOWNLOAD</button>
              </li>
              <li v-if="proposal.beneficiariesFileName || proposal.beneficiariesFilePath" class="file-item">
                <span>📄 Line-Item Budget</span>
                <button class="download-btn btn-download"
                  @click="downloadFile(proposal.beneficiariesFilePath || proposal.beneficiariesFileName)">DOWNLOAD</button>
              </li>
              <li v-if="proposal.submittedByFileName || proposal.submittedByFilePath" class="file-item">
                <span>📄 Submitted By Signature</span>
                <button class="download-btn btn-download"
                  @click="downloadFile(proposal.submittedByFilePath || proposal.submittedByFileName)">DOWNLOAD</button>
              </li>
              <li v-if="proposal.endorsementFileName || proposal.endorsementFilePath" class="file-item">
                <span>📄 Endorsement Document</span>
                <button class="download-btn btn-download"
                  @click="downloadFile(proposal.endorsementFilePath || proposal.endorsementFileName)">DOWNLOAD</button>
              </li>
              <li
                v-if="!proposal.reviewFileName && !proposal.roadmapFileName && !proposal.gadFileName && !(proposal.beneficiariesFileName || proposal.beneficiariesFilePath) && !(proposal.submittedByFileName || proposal.submittedByFilePath) && !(proposal.endorsementFileName || proposal.endorsementFilePath)"
                class="empty-files">
                No attachments uploaded.
              </li>
            </ul>
          </div>
        </aside>
      </div>
    </template>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>{{ successMessage }}</p>
        <button class="btn-primary" @click="showSuccess = false; $router.push('/submit-proposals')">OK</button>
      </div>
    </div>

    <!-- ERROR MODAL -->
    <div v-if="showError" class="modal-overlay">
      <div class="modal-box">
        <p class="error-text">{{ errorMessage }}</p>
        <button class="btn-primary" @click="showError = false">OK</button>
      </div>
    </div>

    <!-- CONFIRM DIALOG -->
    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'
import { downloadUploadedFile } from '@/utils/fileDownload'
import { downloadProposalRecord } from '@/utils/proposalDetailsDocument'
import SdgChecklist from '@/components/SdgChecklist.vue'
import GadChecklist from '@/components/GadChecklist.vue'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import { PROPOSAL_FIELD_SECTIONS, buildFieldRemarkIndex } from '@/utils/proposalFields'
import ProposalReviewHeader from '@/components/ProposalReviewHeader.vue'
import ProposalReviewSidebar from '@/components/ProposalReviewSidebar.vue'

const { dialogState, showAlert, showConfirm } = useDialog()

const router = useRouter()
const route = useRoute()

const proposal = ref({})
const loading = ref(true)
const error = ref(null)
const actionLoading = ref(false)
const downloadLoading = ref(false)
const showSuccess = ref(false)
const showError = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const hasPriorityAgendas = computed(() => {
  if (!proposal.value.priorityAgendas) return false
  return Object.values(proposal.value.priorityAgendas).some(agenda => agenda.selected)
})

const loadProposal = async () => {
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
    await loadFieldRemarks()
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

// ===== Field-level remarks (active revision round) =====
const remarksResponse = ref(null)
const remarksError = ref('')
const remarkIndex = computed(() => buildFieldRemarkIndex(remarksResponse.value))

const loadFieldRemarks = async () => {
  try {
    const res = await api.get(`/api/proposals/${route.params.id}/field-remarks`)
    remarksResponse.value = res.data || null
    remarksError.value = ''
  } catch (err) {
    console.error(err)
    remarksResponse.value = null
    remarksError.value = 'Unable to load the existing remarks.'
  }
}

const onRemarkSaved = (item) => {
  const current = { ...(remarksResponse.value || { activeRound: 1, rounds: [] }) }
  const rounds = Array.isArray(current.rounds) ? [...current.rounds] : []
  let activeIndex = rounds.findIndex((round) => round.active)
  if (activeIndex < 0) {
    rounds.unshift({ round: current.activeRound || 1, active: true, remarks: [] })
    activeIndex = 0
    current.activeRound = rounds[0].round
  }
  const remarks = [...(rounds[activeIndex].remarks || [])]
  const existing = remarks.findIndex((row) => row.id === item.id || row.fieldKey === item.fieldKey)
  if (existing >= 0) remarks[existing] = item
  else remarks.push(item)
  rounds[activeIndex] = { ...rounds[activeIndex], remarks }
  remarksResponse.value = { ...current, rounds }
}

const onRemarkRemoved = (remarkId) => {
  if (!remarksResponse.value) return
  const rounds = (remarksResponse.value.rounds || []).map((round) => ({
    ...round,
    remarks: (round.remarks || []).filter((row) => row.id !== remarkId),
  }))
  remarksResponse.value = { ...remarksResponse.value, rounds }
}

onMounted(loadProposal)

// Vue Router reuses this component when only the id changes, so the route id is watched:
// opening another proposal, or Back/Forward between two, reloads the right record.
watch(() => route.params.id, () => loadProposal())

const forwardToRec = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-rec`)
    successMessage.value = 'Proposal has been forwarded to REC for evaluation.'
    showSuccess.value = true
    proposal.value.status = 'UNDER_REVIEW'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const forwardToOc = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-oc`)
    successMessage.value = 'Proposal has been forwarded to the Chancellor (OC) for final approval.'
    showSuccess.value = true
    proposal.value.status = 'FOR_OC_APPROVAL'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const returnForRevision = () => {
  const proposalId = route.params.id
  router.push({ name: 'OvcReturn4Revision', params: { id: proposalId } })
}

const rejectProposal = async () => {
  const confirmed = await showConfirm('Are you sure you want to reject this proposal?', {
    title: 'Reject Proposal',
    type: 'danger',
    confirmText: 'Reject'
  })
  if (!confirmed) return
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/reject`)
    successMessage.value = 'Proposal has been rejected.'
    showSuccess.value = true
    proposal.value.status = 'REJECTED'
  } catch (err) {
    errorMessage.value = 'Failed to reject proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const downloadFile = (fileName) => {
  downloadUploadedFile(fileName)
}

/**
 * The DOWNLOAD action of the header: the complete proposal document, built and printed by
 * the same shared helper every other role's page uses (proposalDetailsDocument.js).
 *
 * This page used to only fire the four attachment downloads here, so on a proposal without
 * those attachments the button did nothing at all - no document, no error, no feedback. The
 * attachments themselves are still downloadable, one by one, from the cards below.
 */
const downloadProposal = async () => {
  if (!proposal.value?.id) {
    errorMessage.value = 'The proposal is still loading. Please try again in a moment.'
    showError.value = true
    return
  }

  downloadLoading.value = true
  try {
    // The shared role-neutral document, re-read by id so the printed proposal is
    // current, followed by the official Full-Blown R&D Proposal form.
    await downloadProposalRecord({ proposalId: proposal.value.id, proposal: proposal.value })
  } catch (err) {
    console.error(err)
    errorMessage.value = 'Could not prepare the proposal document. Please try again.'
    showError.value = true
  } finally {
    downloadLoading.value = false
  }
}

/**
 * The summary rows the shared sidebar shows. Everything here is a value this page already
 * loaded - the component drops a row that has nothing to show, so no card is ever empty.
 */
const quickRows = computed(() => [
  { label: 'Project Leader', value: proposal.value.projectLeader || 'N/A' },
  { label: 'College', value: proposal.value.college || 'N/A' },
  { label: 'Duration', value: proposal.value.duration ? proposal.value.duration + ' months' : 'N/A' },
  { label: 'Research Type', value: proposal.value.researchType || 'N/A' },
])

const reviewRows = computed(() => [
  { label: 'Reviewed By', value: proposal.value.reviewedBy || '' },
  { label: 'Position', value: formatReviewerPosition(proposal.value.reviewedByPosition) || '' },
])

const goBack = () => {
  router.back()
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatReviewerPosition = (position) => {
  if (!position) return 'N/A'
  const positionMap = {
    'RPS_STAFF': 'RPS Staff',
    'RPS_DIRECTOR': 'RPS Director'
  }
  return positionMap[position] || position
}
</script>

<style scoped>
/* The shell supplies the content padding and the lavender content background; a page-level
   background and a 100vh minimum only fought with it. */
.proposal-detail-page {
  width: 100%;
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

/* The header and sidebar render the status through the shared .rv-status classes
   (assets/status.css); the old page-local .status-badge palette is gone. */
.header-actions {
  display: flex;
  gap: 10px;
}

/* Layout only; the primary colour comes from assets/actions.css. */
.btn-primary {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* LOADING & ERROR */
.loading-state,
.error-state {
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
  to {
    transform: rotate(360deg);
  }
}

.error-state p {
  color: #dc2626;
  font-size: 14px;
}

/* CONTENT GRID */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
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
  font-weight: 700;
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
  gap: 16px;
  /* Keep each field at its natural height so a remark on one field can never
     stretch its row partner and misalign the neighbouring column. */
  align-items: start;
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
  margin: 0;
}

/* TEXT CONTENT */
.text-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-text {
  color: #94a3b8;
  font-style: italic;
  font-size: 14px;
}

/* TABLES */
.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8fafc;
  color: #1e293b;
  font-size: 12px;
  font-weight: 700;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
  text-transform: uppercase;
}

.data-table td {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  font-size: 14px;
  color: #334155;
}

/* SUBSECTIONS */
.subsection {
  margin-bottom: 20px;
}

.subsection:last-child {
  margin-bottom: 0;
}

.subsection h3 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin: 0 0 8px 0;
}

.objectives>div {
  margin-bottom: 12px;
}

.objectives strong {
  display: block;
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

/* TYPE BADGE */
.type-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.type-badge.basic {
  background: #dbeafe;
  color: #1e40af;
}

.type-badge.applied {
  background: #dcfce7;
  color: #166534;
}

/* AGENDA LIST */
.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.agenda-item {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 14px;
}

.agenda-item strong {
  color: #4f46e5;
}

/* FILE ATTACHMENT */
.file-attachment {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.file-icon {
  font-size: 24px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin: 0;
}

.file-type {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

/* Layout only; the action colours are global (assets/actions.css). */
.btn-download {
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

/* SIDEBAR */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 16px;
}

.sidebar-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 12px 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.status-meta p {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0;
}

/* QUICK INFO */
.quick-info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
  font-weight: 500;
  color: #1e293b;
}

/* REVIEW INFO */
.review-info-card {
  background: #f0fdf4;
  border-color: #86efac;
}

.review-info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-info-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
}

.review-info-value {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.reviewer-position {
  color: #4f46e5;
}

/* TIMELINE */
.timeline {
  list-style: none;
  padding: 0;
  margin: 0;
}

.timeline li {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

.timeline li.active::before {
  background: #4f46e5;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e2e8f0;
  flex-shrink: 0;
  margin-top: 4px;
}

.dot.completed {
  background: var(--status-approved-solid);
}

.timeline li strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.timeline li small {
  font-size: 12px;
  color: #64748b;
}

/* ACTION CARD */
.action-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-action {
  width: 100%;
  padding: 10px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* The colours for .btn-primary / .btn-warning / .btn-danger come from assets/actions.css. */

/* FILES LIST */
.files-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.file-item:last-child {
  border-bottom: none;
}

.download-btn {
  cursor: pointer;
  font-size: 16px;
}

.empty-files {
  color: #94a3b8;
  font-style: italic;
  font-size: 13px;
  padding: 8px 0;
}

/* MODALS */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  background: white;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  width: 320px;
}

.modal-box p {
  margin-bottom: 16px;
  font-size: 14px;
  color: #334155;
}

.error-text {
  color: #dc2626;
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
  }
}
</style>