<template>
  <div class="page-wrapper">
    <!-- HEADER TITLE -->
    <div class="header-banner">
      <h1 v-if="proposal.status === 'FOR_OVCAF_APPROVAL'">PROPOSAL REVIEW & ENDORSEMENT</h1>
      <h1 v-else>FUND REQUEST VALIDATION</h1>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="content-area" style="text-align:center;padding:60px;">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="content-area" style="text-align:center;padding:60px;">
      <p class="error-text">{{ error }}</p>
    </div>

    <!-- Proposal Data -->
    <template v-else-if="proposal.id">
      <!-- ACTION BUTTONS -->
      <div class="action-buttons">
        <!-- Approval Chain: Endorse to OC -->
        <button v-if="proposal.status === 'FOR_OVCAF_APPROVAL'" class="btn-yellow btn-primary" @click="endorseToOc" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Endorse to Chancellor (OC) for Final Approval' }}
        </button>

        <!-- Budget Processing: Approve & Endorse to RII -->
        <button v-if="canProcessBudget" class="btn-yellow btn-approve" @click="approveEndorse" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Approve & Endorse to RII' }}
        </button>

        <button class="btn-yellow btn-return" @click="returnProponent" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Return to Proponent' }}
        </button>
        <button class="btn-yellow btn-reject" @click="showRejectModal = true" :disabled="actionLoading">
          Reject Request
        </button>
      </div>

      <!-- SCROLLABLE CONTENT AREA -->
      <div class="content-area">
        <div class="proposal-body">
          <h4>FIELD-LEVEL REVIEW</h4>
          <p class="field-note">
            Leave a remark beside any field that needs correction; the proponent sees it beside that
            exact field on the revision page.
          </p>
          <p v-if="remarksError" class="error-text">{{ remarksError }}</p>
          <ProposalFieldSection
            v-for="section in PROPOSAL_FIELD_SECTIONS"
            :key="section.key"
            :section="section"
            :proposal="proposal"
            :proposal-id="proposal.id"
            :remark-index="remarkIndex"
            :editable="true"
            @remark-saved="onRemarkSaved"
            @remark-removed="onRemarkRemoved"
          />
        </div>
        <div class="proposal-body">
          <h4>1. PROJECT PROFILE</h4>
          <div class="two-column">
            <div>
              <p><strong>Program Title:</strong> {{ proposal.programTitle || 'N/A' }}</p>
              <p><strong>Project Title:</strong> {{ proposal.projectTitle || 'N/A' }}</p>
              <p><strong>Project Leader:</strong> {{ proposal.projectLeader || 'N/A' }}</p>
              <p><strong>Project Duration:</strong> {{ proposal.duration || 'N/A' }}</p>
              <p><strong>Start Date:</strong> {{ proposal.startDate || 'N/A' }}</p>
              <p><strong>End Date:</strong> {{ proposal.endDate || 'N/A' }}</p>
            </div>
            <div>
              <p><strong>College/Department:</strong> {{ proposal.college || 'N/A' }}</p>
              <p><strong>Status:</strong> <span class="status-badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) }}</span></p>
              <p><strong>Date Submitted:</strong> {{ proposal.createdAt?.substring(0,10) || 'N/A' }}</p>
            </div>
          </div>

          <h4>EXECUTIVE SUMMARY</h4>
          <p>{{ proposal.executiveSummary || 'No executive summary provided.' }}</p>

          <h4>LINE-ITEM BUDGET</h4>
          <div v-if="budgetFileName" class="budget-doc-row">
            <span class="budget-doc-name">📄 {{ budgetFileName }}</span>
            <button class="download-btn btn-view" @click="viewBudgetDocument">VIEW</button>
            <button class="download-btn btn-download" @click="saveBudgetDocument">DOWNLOAD</button>
          </div>
          <!-- A stored reference that yielded no printable name is a retrieval problem,
               not a missing document. -->
          <p v-else-if="hasBudgetReference" class="error-text">
            The Line-Item Budget document is attached, but its reference could not be resolved.
          </p>
          <p v-else class="budget-doc-empty">No Line-Item Budget uploaded.</p>
          <p v-if="budgetDocError" class="error-text">{{ budgetDocError }}</p>

          <button class="download-btn btn-download" @click="downloadProject">DOWNLOAD COMPLETE PROPOSAL</button>
        </div>
      </div>
    </template>

    <!-- No proposal found -->
    <div v-else class="content-area" style="text-align:center;padding:60px;">
      <p>No proposal ID provided. Please select a proposal from the dashboard.</p>
    </div>

    <!-- REJECT MODAL -->
    <div class="overlay" v-if="showRejectModal">
      <div class="confirm-box">
        <p class="confirm-text">Please provide a reason for rejection:</p>
        <textarea v-model="rejectReason" class="reason-input" placeholder="Enter rejection reason..."></textarea>
        <div class="confirm-actions">
          <button class="btn-confirm btn-reject" @click="confirmReject" :disabled="!rejectReason.trim()">CONFIRM</button>
          <button class="btn-cancel" @click="showRejectModal = false">CANCEL</button>
        </div>
      </div>
    </div>

    <!-- APPROVE CONFIRMATION MODAL -->
    <div class="overlay" v-if="showApproveConfirm">
      <div class="confirm-box">
        <p class="confirm-text" v-if="approvalType === 'oc'">Are you sure you want to endorse this proposal to the Chancellor's Office for final approval?</p>
        <p class="confirm-text" v-else>Are you sure you want to approve this fund request and endorse it to RII for release?</p>
        <div class="confirm-actions">
          <button class="btn-confirm btn-approve" @click="confirmApprove">CONFIRM</button>
          <button class="btn-cancel" @click="showApproveConfirm = false">CANCEL</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MESSAGE MODAL -->
    <div class="overlay" v-if="showSuccess">
      <div class="success-box">{{ successMessage }}</div>
    </div>

    <!-- Line-Item Budget preview: opens over this page, never navigates away. -->
    <LineItemBudgetPreviewModal v-if="showBudgetPreview" :file-path="budgetDocumentPath(proposal)"
      :file-name="budgetFileName" @close="showBudgetPreview = false" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'
import { downloadOfficialProposal } from '@/utils/officialProposalForm'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import LineItemBudgetPreviewModal from '@/components/LineItemBudgetPreviewModal.vue'
import { PROPOSAL_FIELD_SECTIONS, buildFieldRemarkIndex } from '@/utils/proposalFields'
import { budgetDocumentName, budgetDocumentPath } from '@/utils/budgetDocument'
import { downloadUploadedFile } from '@/utils/fileDownload'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

const route = useRoute()

const proposal = ref({})
const loading = ref(true)
const error = ref(null)
const actionLoading = ref(false)
const approvalType = ref('budget')

// Section 25 is an uploaded document, not a recomputable amount table. This page
// already renders the same saved record (GET /api/proposals/{id}) as the proposal
// viewer, so the Project Leader and the budget document cannot disagree with it.
const budgetFileName = computed(() => budgetDocumentName(proposal.value))
const hasBudgetReference = computed(() => Boolean(budgetDocumentPath(proposal.value)))
const budgetDocError = ref('')
// View opens the preview modal over this page; the evaluation state stays as it is.
const showBudgetPreview = ref(false)

const viewBudgetDocument = () => {
  if (!budgetDocumentPath(proposal.value)) {
    budgetDocError.value = 'The Line-Item Budget document could not be retrieved.'
    return
  }
  budgetDocError.value = ''
  showBudgetPreview.value = true
}

const saveBudgetDocument = async () => {
  const path = budgetDocumentPath(proposal.value)
  if (!path) {
    budgetDocError.value = 'The Line-Item Budget document could not be retrieved.'
    return
  }
  budgetDocError.value = ''
  try {
    await downloadUploadedFile(path, budgetFileName.value)
  } catch (err) {
    console.error(err)
    budgetDocError.value = 'The Line-Item Budget document could not be downloaded. Please try again.'
  }
}

// Field-level remarks of the active revision round.
const remarksResponse = ref(null)
const remarksError = ref('')
const remarkIndex = computed(() => buildFieldRemarkIndex(remarksResponse.value))

const loadFieldRemarks = async (proposalId) => {
  try {
    const res = await api.get(`/api/proposals/${proposalId}/field-remarks`)
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

const showRejectModal = ref(false)
const rejectReason = ref('')
const showApproveConfirm = ref(false)
const showSuccess = ref(false)
const successMessage = ref('')

const canProcessBudget = computed(() => {
  return proposal.value.status === 'APPROVED' || proposal.value.status === 'READY_FOR_RELEASE'
})

const loadProposal = async () => {
  loading.value = true
  error.value = null
  showBudgetPreview.value = false
  try {
    const proposalId = route.params.id
    if (!proposalId) {
      loading.value = false
      return
    }
    const res = await api.get(`/api/proposals/${proposalId}`)
    proposal.value = res.data
    await loadFieldRemarks(proposalId)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProposal)

// Vue Router reuses this component when only the id changes, so the route id is watched:
// opening another proposal, or Back/Forward between two, reloads the right record.
watch(() => route.params.id, () => loadProposal())

const endorseToOc = () => {
  approvalType.value = 'oc'
  showApproveConfirm.value = true
}

const approveEndorse = () => {
  approvalType.value = 'budget'
  showApproveConfirm.value = true
}

const confirmApprove = async () => {
  showApproveConfirm.value = false
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    if (approvalType.value === 'oc') {
      await api.put(`/api/proposals/${proposalId}/forward-to-oc`)
      successMessage.value = 'Proposal has been endorsed to the Chancellor (OC) for final approval.'
      proposal.value.status = 'FOR_OC_APPROVAL'
    } else {
      await api.put(`/api/proposals/${proposalId}/status?status=READY_FOR_RELEASE`)
      successMessage.value = 'Fund request approved and endorsed to RII.'
      proposal.value.status = 'READY_FOR_RELEASE'
    }
    showSuccess.value = true
  } catch (err) {
    successMessage.value = 'Failed to process request. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const returnProponent = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/return-revision`)
    successMessage.value = 'Returned to proponent successfully.'
    showSuccess.value = true
  } catch (err) {
    successMessage.value = 'Failed to return. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return
  showRejectModal.value = false
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/reject`)
    successMessage.value = 'Request rejected.'
    showSuccess.value = true
  } catch (err) {
    successMessage.value = 'Failed to reject. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    rejectReason.value = ''
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const downloadProject = () => {
  downloadOfficialProposal(proposal.value)
}
</script>

<style scoped>
.page-wrapper {
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 135%;
}
.header-banner {
  background: #1c2145;
  padding: 20px 30px;
  color: white;
  font-weight: bold;
}
.header-banner h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
}
.action-buttons {
  width: 100%;
  padding: 20px 30px;
  background: white;
  border-bottom: 2px solid #e6e6e6;
  display: flex;
  justify-content: center;
  gap: 20px;
  box-sizing: border-box;
  flex-wrap: wrap;
}
.content-area {
  padding: 20px 40px;
  overflow-y: auto;
}
/* Layout only; the action colour comes from assets/actions.css. */
.btn-yellow {
  padding: 15px 25px;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
  font-size: 15px;
}
.btn-yellow:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.proposal-body h4 {
  margin-top: 35px;
  margin-bottom: 10px;
  font-weight: bold;
  font-size: 18px;
}
.proposal-body p {
  line-height: 1.6;
  font-size: 15px;
}
.two-column {
  display: flex;
  justify-content: space-between;
  gap: 40px;
}
.two-column > div {
  width: 48%;
}
.impl-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
  font-size: 15px;
}
.impl-table th, .impl-table td {
  border: 1px solid black;
  padding: 8px;
}
/* Layout only; .btn-view / .btn-download supply the colours. */
.download-btn {
  margin-left: auto;
  display: block;
  padding: 12px 20px;
  border-radius: 10px;
  cursor: pointer;
  margin-top: 20px;
}
.budget-doc-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.budget-doc-name {
  font-size: 15px;
  overflow-wrap: anywhere;
}
.budget-doc-row .download-btn {
  margin: 0;
  display: inline-block;
  padding: 8px 16px;
  font-weight: bold;
}
.budget-doc-empty {
  color: #6b7280;
  font-style: italic;
}
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}
.confirm-box {
  background: #3c376b;
  width: 600px;
  padding: 35px 30px;
  border-radius: 40px;
  color: #ffd700;
  text-align: center;
}
.confirm-text {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 35px;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 60px;
}
/* Layout only; .btn-reject / .btn-approve / .btn-cancel supply the colours. */
.btn-confirm {
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}
.btn-cancel {
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}
.success-box {
  background: #3c376b;
  color: #ffd700;
  padding: 15px 40px;
  border-radius: 40px;
  font-weight: bold;
  font-size: 18px;
}
.reason-input {
  width: 80%;
  padding: 10px;
  border-radius: 10px;
  border: none;
  margin-bottom: 20px;
  font-size: 14px;
  color: #000;
}
.error-text { color: #dc2626; }
/* Geometry only; the tone comes from assets/status.css. */
.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}
</style>