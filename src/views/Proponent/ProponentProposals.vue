<template>
  <div class="proposals-content">
    <div class="top-bar">
      <h2>myProjects</h2>
      <div class="top-bar-actions">
        <button class="btn-revisions" @click="openRevisions">Revisions</button>
        <button class="btn-new" @click="openNewProposal">
          + Create New Proposal
        </button>
      </div>
    </div>

    <!-- MODALS -->
    <!-- Step 1: the Call for Proposal the proponent responds to -->
    <ProponentCallForProposal v-if="showCallModal" @select="handleCallSelected"
      @close="showCallModal = false" />

    <ProponentSubmitProp ref="step1Ref" v-model="showModal" :selected-call="selectedCall" @save="handleSave"
      @next="goToStep2" @goToStep="handleGoToStep" @update:modelValue="refreshOnClose" />

    <ProponentSubmitProp2 ref="step2Ref" v-if="showModal2" :open="showModal2"
      :saved-signature="proposalData.certificationSignature"
      :endorsement="endorsementRecord"
      @update:signature="proposalData.certificationSignature = $event"
      @close="showModal2 = false; refreshOnClose()" @openPrevious="goBackToStep1" @openCriteria="openCriteriaModal"
      @submitProposal="handleStep2Submit" @goToStep="handleGoToStep" />

    <ProponentSubmitProp3 ref="step3Ref" v-model="showCriteria" :proposalData="proposalData"
      :endorsement-request-id="endorsementRequestId" @back="goBackToStep2"
      @savedraft="handleSaveDraft" @goToStep="handleGoToStep" @update:modelValue="refreshOnClose" />

    <p class="subtitle">
      Manage and track all your proposal submissions
    </p>

    <!-- FILTERS -->
    <div class="filters">
      <input type="text" v-model="search" placeholder="Search by title, ID, college, leader, or date..." />
      <select v-model="statusFilter">
        <option value="">All Status</option>
        <option value="DRAFT">Draft</option>
        <option value="SUBMITTED">Submitted</option>
        <option value="UNDER_REVIEW">Under Review</option>
        <option value="APPROVED">Approved</option>
        <option value="REJECTED">Rejected</option>
      </select>
    </div>

    <!-- LOADING -->
    <p v-if="loading">Loading proposals...</p>

    <!-- ERROR -->
    <p v-if="error" class="error-text">{{ error }}</p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && proposals.length === 0">No proposals found.</p>

    <!-- TABLE -->
    <table v-if="proposals.length > 0" class="proposals-table">
      <thead>
        <tr>
          <th>Proposal ID</th>
          <th>Title</th>
          <th>Status</th>
          <th>Submitted</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
          <td>{{ proposal.projectTitle || proposal.title }}</td>
          <td>
            <span class="status" :class="(proposal.status || '').toLowerCase()">
              {{ proposal.status }}
            </span>
          </td>
          <td>{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>
          <td>
            <button class="btn-view" @click="viewProposal(proposal.id)">View</button>
            <button v-if="proposal.status === 'DRAFT'" class="btn-delete"
              @click="deleteProposal(proposal.id)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>

  <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
    @cancel="dialogState.onCancel" @close="dialogState.show = false" />
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
// The Dean endorsement the proposal is being continued from, when the Proponent
// arrived through "Continue to Proposal Submission".
import { endorsementProposalPrefill, fetchEndorsementRequest, isApproved } from '@/utils/endorsement.js'
import { currentUserId } from '@/utils/session'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

import ProponentCallForProposal from './ProponentCallForProposal.vue'
import ProponentSubmitProp from './ProponentSubmitProp.vue'
import ProponentSubmitProp2 from './ProponentSubmitProp2.vue'
import ProponentSubmitProp3 from './ProponentSubmitProp3.vue'

const router = useRouter()
const route = useRoute()

const proposals = ref([])
const loading = ref(false)
const error = ref(null)
const search = ref('')
const statusFilter = ref('')

const showModal = ref(false)
const showModal2 = ref(false)
const showCriteria = ref(false)
const proposalData = ref({})
/**
 * The approved endorsement the proposal is being continued from, or null. It is
 * passed to the Review & Submit step, which links the saved proposal to it.
 */
const endorsementRequestId = ref(null)
/**
 * The approved endorsement record itself, so the Certification & Endorsement step can
 * fill its "Endorsed By" section from it and lock it.
 */
const endorsementRecord = ref(null)

// Step 1 of the workflow: the Call for Proposal the proponent is responding to. The
// real call (with its database id) is kept here for the whole workflow, so every step
// keeps working with the same call and the submitted proposal is attached to it.
const showCallModal = ref(false)
const selectedCall = ref(null)

const step1Ref = ref(null)
const step2Ref = ref(null)
const step3Ref = ref(null)

/**
 * The selected call in the shape the workflow already passes between steps, so it
 * travels with the proposal data and reaches the submission payload.
 */
const callSelectionFields = (call) =>
  call ? { proposal_announcement_id: call.id, proposal_announcement_name: call.name } : {}

const resetAllForms = () => {
  step1Ref.value?.resetForm()
  step2Ref.value?.resetForm()
  step3Ref.value?.resetForm()
  proposalData.value = {}
  selectedCall.value = null
}

const openRevisions = () => {
  router.push('/revisions')
}

/**
 * The Create Proposal workflow always starts with the Call for Proposal step: the
 * proponent picks the open call they are responding to before the proposal details.
 * The step itself loads the open calls and shows the empty state when there is
 * nothing to respond to.
 */
const openNewProposal = () => {
  resetAllForms()
  showCallModal.value = true
}

/** The proponent selected a call: keep it and continue with the proposal details. */
const handleCallSelected = (call) => {
  selectedCall.value = call
  proposalData.value = { ...proposalData.value, ...callSelectionFields(call) }
  showCallModal.value = false
  showModal.value = true
}

/** Back to the Call for Proposal step, so a different call can be selected. */
const openCallSelection = () => {
  showModal.value = false
  showModal2.value = false
  showCriteria.value = false
  showCallModal.value = true
}

const fetchProposals = async () => {
  loading.value = true
  error.value = null
  proposals.value = []
  const requestUserId = currentUserId()
  try {
    const res = await api.get('/api/proposals')
    // Discard a response if the session changed while it was in flight.
    if (currentUserId() !== requestUserId) return
    proposals.value = Array.isArray(res.data) ? res.data : res.data.data || []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals'
    proposals.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchProposals()
  // Check if we're editing a draft (from the Edit Draft button on detail page)
  const editId = route.query.edit
  if (editId) {
    await openDraftForEdit(editId)
    return
  }
  // Arriving from an approved endorsement ("Continue to Proposal Submission"): the
  // title, the Call for Proposal and the college are prepopulated from the
  // endorsement record, and the resulting proposal is linked to it once it is saved.
  const endorsementId = route.query.endorsement
  if (endorsementId) {
    await openEndorsedProposal(endorsementId)
  }
})

/**
 * Opens the Create Proposal workflow for an approved endorsement. The endorsement is
 * read from the backend (never from the query string), so the Proponent can only
 * continue from their own approved endorsement, and the full proposal form still has
 * to be completed and validated before it can be submitted.
 */
const openEndorsedProposal = async (id) => {
  try {
    const endorsement = await fetchEndorsementRequest(id)
    if (!isApproved(endorsement?.status)) {
      await showAlert('Only an approved endorsement can be continued to a proposal.', {
        type: 'warning',
        title: 'Not Endorsed',
      })
      router.replace({ path: '/proposals' })
      return
    }

    resetAllForms()
    await nextTick()
    endorsementRequestId.value = endorsement.id
    endorsementRecord.value = endorsement
    if (endorsement.proposalAnnouncementId) {
      selectedCall.value = {
        id: endorsement.proposalAnnouncementId,
        name: endorsement.proposalAnnouncementName || '',
      }
    }
    // Program Title and Project Title are carried over as the two independent values they
    // are, in the shape the Create Proposal form actually reads (`endorsementProposalPrefill`
    // returns the proposal DTO keys loadProposal maps). The Call for Proposal travels in
    // `selectedCall` above, which is what attaches the saved proposal to it.
    step1Ref.value?.loadProposal(endorsementProposalPrefill(endorsement))
    showModal.value = true
    router.replace({ path: '/proposals' })
  } catch (err) {
    console.error(err)
    await showAlert('The endorsement could not be loaded.', { type: 'error', title: 'Load Failed' })
    router.replace({ path: '/proposals' })
  }
}

const openDraftForEdit = async (id) => {
  try {
    const res = await api.get(`/api/proposals/${id}`)
    const proposalData = res.data
    if (proposalData.status !== 'DRAFT') {
      await showAlert('Only draft proposals can be edited.', { type: 'warning', title: 'Cannot Edit' })
      // Clean up the URL
      router.replace({ path: '/proposals' })
      return
    }
    // Open the modal and load the proposal data
    resetAllForms()
    await nextTick()
    // The draft already belongs to a Call for Proposal: keep that call as the
    // selection, so the workflow shows it and the submission stays under the same
    // call instead of being re-attached to whatever call is currently active.
    if (proposalData.proposalAnnouncementId) {
      selectedCall.value = { id: proposalData.proposalAnnouncementId, name: '' }
    }
    step1Ref.value?.loadProposal(proposalData)
    showModal.value = true
    // Clean up the URL so refreshing doesn't re-open the modal
    router.replace({ path: '/proposals' })
  } catch (err) {
    console.error(err)
    await showAlert('Failed to load proposal for editing.', { type: 'error', title: 'Load Failed' })
    router.replace({ path: '/proposals' })
  }
}

const filteredProposals = computed(() => {
  return proposals.value.filter(p => {
    if (statusFilter.value && p.status !== statusFilter.value) return false
    if (search.value.trim()) {
      const kw = search.value.toLowerCase()
      const title = String(p.projectTitle || p.title || '').toLowerCase()
      const proposalId = String(p.documentId || p.proposalCode || p.id || '').toLowerCase()
      const college = String(p.college || '').toLowerCase()
      const leader = String(p.projectLeader || p.teamLeader || '').toLowerCase()
      const submitted = String(p.createdAt || p.dateSubmitted || '').toLowerCase()
      return (
        title.includes(kw) ||
        proposalId.includes(kw) ||
        college.includes(kw) ||
        leader.includes(kw) ||
        submitted.includes(kw)
      )
    }
    return true
  }).sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

const viewProposal = (id) => {
  router.push({ name: 'ProponentDetiailedProp', query: { id } })
}

// ===== Make a Copy =====
// The action lives on the proposal details page (opened with View), not in this
// table: a copy is made from the proposal the proponent is looking at, whatever
// its status, and the backend creates a brand-new DRAFT that this page then shows.

const deleteProposal = async (id) => {
  const confirmed = await showConfirm('Are you sure you want to delete this draft proposal?', {
    title: 'Delete Draft',
    type: 'danger',
    confirmText: 'Delete'
  })
  if (!confirmed) return
  try {
    await api.delete(`/api/proposals/${id}`)
    proposals.value = proposals.value.filter(p => p.id !== id)
    await showAlert('Draft deleted successfully.', { type: 'success', title: 'Draft Deleted' })
  } catch (err) {
    console.error(err)
    await showAlert('Failed to delete proposal. Please try again.', { type: 'error', title: 'Delete Failed' })
  }
}

// ===== MODAL FLOW WITH DATA PASSING =====
// Step numbering of the 4-step Create Proposal workflow:
//   1 Call for Proposal   2 Proposal Details   3 Certification   4 Review & Submit

const handleSave = (formData) => {
  // Save form data from Step 2 (Proposal Details)
  proposalData.value = { ...formData, ...callSelectionFields(selectedCall.value) }
  showModal.value = false
}

/**
 * Jumps to another step. Steps 2-4 are only reachable once a call has been selected,
 * so a request for a later step without a selection opens the Call for Proposal step
 * instead - the proponent can never reach the proposal form without a valid call.
 */
const handleGoToStep = (step) => {
  if (step <= 1 || !selectedCall.value) {
    openCallSelection()
  } else if (step === 2) {
    showCallModal.value = false
    showModal2.value = false
    showCriteria.value = false
    showModal.value = true
  } else if (step === 3) {
    showCallModal.value = false
    showModal.value = false
    showCriteria.value = false
    showModal2.value = true
  } else if (step === 4) {
    showCallModal.value = false
    showModal.value = false
    showModal2.value = false
    showCriteria.value = true
  }
}

const goToStep2 = (formData) => {
  // Save data from Step 2 (Proposal Details) and open Step 3 (Certification). The
  // selected call travels with the proposal data.
  if (formData) {
    proposalData.value = {
      ...proposalData.value,
      ...formData,
      ...callSelectionFields(selectedCall.value)
    }
  }
  showModal.value = false
  showModal2.value = true
}

const goBackToStep1 = () => {
  showModal2.value = false
  showModal.value = true
}

const handleStep2Submit = (certificationData) => {
  // Store certification data alongside proposal data. The signature chosen in the
  // step travels with it, so going back to that step keeps the same selection
  // instead of starting over from the account's saved signature.
  if (certificationData) {
    proposalData.value = {
      ...proposalData.value,
      certification: certificationData.certification,
      signatureFiles: certificationData.signatureFiles,
      certificationSignature:
        certificationData.certificationSignature ?? proposalData.value.certificationSignature ?? null
    }
  }
  // The selected call is kept until the proposal is submitted.
  proposalData.value = { ...proposalData.value, ...callSelectionFields(selectedCall.value) }
  showModal2.value = false
  showCriteria.value = true
}

const goBackToStep2 = () => {
  showCriteria.value = false
  showModal2.value = true
}

const openCriteriaModal = () => {
  showModal2.value = false
  showCriteria.value = true
}

const handleSaveDraft = () => {
  showCriteria.value = false
  resetAllForms()
  fetchProposals() // Refresh the list
}

// Watch for any modal to close and refresh the table
const refreshOnClose = () => {
  fetchProposals()
}
</script>

<style scoped>
.proposals-content {
  padding: 24px;
  background: #ffffff;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.top-bar-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-revisions {
  background: #4b3f72;
  border: none;
  color: #ffffff;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 16px;
}

.btn-new {
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

.btn-new:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filters input,
.filters select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.proposals-table td:first-child {
  font-family: monospace;
  font-weight: 600;
  color: #4b3f72;
}

.proposals-table th {
  text-align: left;
  padding: 12px;
  background: #4b3f72;
  color: #ffffff;
}

.proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.pending {
  background: #fef3c7;
  color: #92400e;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.draft {
  background: #e5e7eb;
  color: #374151;
}

.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.btn-view {
  background: #60a5fa;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
}

.btn-delete {
  background: #ef4444;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.btn-delete:hover {
  background: #dc2626;
}

.error-text {
  color: #dc2626;
}
</style>