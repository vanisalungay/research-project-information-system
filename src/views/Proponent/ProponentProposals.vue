<template>
  <div class="proposals-content">
    <header class="page-header">
      <div class="page-heading">
        <h2 class="page-title">myProjects</h2>
        <p class="subtitle">Manage and track all your proposal submissions</p>
      </div>
      <div class="top-bar-actions">
        <button type="button" class="btn-revisions btn-secondary" @click="openRevisions">View Revisions</button>
        <button type="button" class="btn-new btn-primary" @click="openNewProposal">
          + Create New Proposal
        </button>
      </div>
    </header>

    <!-- MODALS -->
    <!-- Step 1: the Call for Proposal the proponent responds to -->
    <ProponentCallForProposal v-if="showCallModal" @select="handleCallSelected"
      @close="showCallModal = false" />

    <ProponentSubmitProp ref="step1Ref" v-model="showModal" :selected-call="selectedCall"
      @savedraft="handleStep1SaveDraft"
      @next="goToStep2" @goToStep="handleGoToStep" @update:modelValue="refreshOnClose" />

    <!-- The Certification step stays mounted for the whole workflow (it is only hidden
         while closed). Navigating back to the Proposal Details step must never discard the
         Submitted By / Endorsed By fields and the chosen signature / endorsement PDF: the
         workflow reads the step's current state whenever it saves, whichever step the
         proponent is on. -->
    <ProponentSubmitProp2 ref="step2Ref" :open="showModal2"
      :saved-signature="proposalData.certificationSignature"
      :endorsement="endorsementRecord"
      :saved-endorsement-file-name="proposalData.endorsementFileName || ''"
      :saved-endorsement-file-path="proposalData.endorsementFilePath || ''"
      :saved-certification="savedCertificationForStep2"
      :saved-submitted-signature-file-name="proposalData.submittedByFileName || ''"
      :saved-submitted-signature-file-path="proposalData.submittedByFilePath || ''"
      @update:signature="proposalData.certificationSignature = $event"
      @savedraft="handleStep2SaveDraft"
      @close="showModal2 = false; refreshOnClose()" @openPrevious="goBackToStep1" @openCriteria="openCriteriaModal"
      @submitProposal="handleStep2Submit" @goToStep="handleGoToStep" />

    <ProponentSubmitProp3 ref="step3Ref" v-model="showCriteria" :proposalData="proposalData"
      :endorsement-request-id="endorsementRequestId" @back="goBackToStep2"
      @savedraft="handleSaveDraft" @goToStep="handleGoToStep" @update:modelValue="refreshOnClose" />

    <!-- FILTERS -->
    <div class="filters-card">
      <div class="filters">
        <input type="text" v-model="search" aria-label="Search proposals"
          placeholder="Search by title, ID, college, leader, or date..." />
        <select v-model="statusFilter" aria-label="Filter by status">
          <option value="">All Status</option>
          <option value="DRAFT">{{ proposalStatusLabel('DRAFT') }}</option>
          <option value="FOR REVIEW">{{ proposalStatusLabel('FOR REVIEW') }}</option>
          <option value="UNDER_REVIEW">{{ proposalStatusLabel('UNDER_REVIEW') }}</option>
          <option value="APPROVED">{{ proposalStatusLabel('APPROVED') }}</option>
          <option value="COMPLETED">{{ proposalStatusLabel('COMPLETED') }}</option>
          <option value="REJECTED">{{ proposalStatusLabel('REJECTED') }}</option>
        </select>
      </div>
    </div>

    <!-- LOADING -->
    <p v-if="loading" class="state-message state-loading" role="status">Loading proposals...</p>

    <!-- ERROR -->
    <p v-if="error" class="state-message state-error error-text">{{ error }}</p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && proposals.length === 0" class="state-message state-empty">No proposals found.</p>

    <!-- TABLE -->
    <div v-if="proposals.length > 0" class="table-card">
      <div class="table-scroll">
        <table class="proposals-table">
          <colgroup>
            <col class="col-id" />
            <col class="col-title" />
            <col class="col-status" />
            <col class="col-submitted" />
            <col class="col-actions" />
          </colgroup>
          <thead>
            <tr>
              <th scope="col">Proposal ID</th>
              <th scope="col">Title</th>
              <th scope="col">Status</th>
              <th scope="col">Submitted</th>
              <th scope="col" class="actions-head">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposal in filteredProposals" :key="proposal.id">
              <td class="cell-id" :title="proposal.documentId || proposal.proposalCode || '—'">
                {{ proposal.documentId || proposal.proposalCode || '—' }}
              </td>
              <td class="cell-title">
                <span class="title-text" :title="proposal.projectTitle || proposal.title">
                  {{ proposal.projectTitle || proposal.title }}
                </span>
              </td>
              <td class="cell-status">
                <span class="status" :class="statusTone(proposal.status)">
                  {{ proposalStatusLabel(proposal.status) }}
                </span>
              </td>
              <td class="cell-date">{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>
              <td class="cell-actions">
                <div class="row-actions">
                  <!-- Open (an approved project) is green; View (still in the workflow) is
                       blue, so the two row actions read apart at a glance. -->
                  <button type="button" class="btn-view"
                    :class="isApprovedProject(proposal.status) ? 'action-open' : 'action-view'"
                    @click="viewProposal(proposal.id)">{{ proposalActionLabel(proposal.status) }}</button>
                  <button v-if="proposal.status === 'DRAFT'" type="button" class="btn-delete btn-reject"
                    @click="deleteProposal(proposal.id)">Delete</button>
                  <span v-else class="action-placeholder" aria-hidden="true"></span>
                </div>
              </td>
            </tr>
            <tr v-if="filteredProposals.length === 0">
              <td colspan="5" class="empty-row">No proposals match your search.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
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
import { draftEndorsementFields } from '@/utils/endorsementDocument'
import { statusTone } from '@/utils/statusTone.js'
import { isApprovedProject, proposalActionLabel, proposalStatusLabel, proposalStatusMatches } from '@/utils/proposalStatus.js'
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
    // A local copy of the fetched proposal, named apart from the workflow's
    // `proposalData` ref so the document references below are not written into a
    // discarded object.
    const loaded = res.data
    if (loaded.status !== 'DRAFT') {
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
    if (loaded.proposalAnnouncementId) {
      selectedCall.value = { id: loaded.proposalAnnouncementId, name: '' }
    }
    step1Ref.value?.loadProposal(loaded)
    // Step 1 loads only the numbered form fields. The shared snapshot that Review &
    // Submit saves is hydrated from it now, plus the endorsement document, the stored
    // certification and the stored signature - so reopening a draft and saving again
    // keeps every one of them instead of blanking the parts Step 1 does not carry.
    proposalData.value = {
      ...proposalData.value,
      ...(step1Ref.value?.getFormData?.() || {}),
      ...callSelectionFields(selectedCall.value),
      ...draftEndorsementFields(loaded),
      submittedByName: loaded.submittedByName || '',
      submittedByDesignation: loaded.submittedByDesignation || '',
      submittedByDate: loaded.submittedByDate || '',
      submittedByFileName: loaded.submittedByFileName || '',
      submittedByFilePath: loaded.submittedByFilePath || '',
      endorsedByName: loaded.endorsedByName || '',
      endorsedByDesignation: loaded.endorsedByDesignation || '',
      endorsedByDate: loaded.endorsedByDate || '',
    }
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
    if (!proposalStatusMatches(p.status, statusFilter.value)) return false
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

/**
 * The certification to restore in the Certification step. It prefers what the
 * proponent typed in this session over what the draft stored, so navigating back and
 * forth keeps the current values; a freshly loaded draft has no in-session values yet,
 * so its stored certification fills them.
 */
const savedCertificationForStep2 = computed(() => {
  const data = proposalData.value || {}
  const inSession = data.certification || {}
  return {
    submittedByName: inSession.submitted_name || data.submittedByName || '',
    submittedByDesignation: inSession.submitted_designation || data.submittedByDesignation || '',
    submittedByDate: inSession.submitted_date || data.submittedByDate || '',
    endorsedByName: inSession.endorsed_name || data.endorsedByName || '',
    endorsedByDesignation: inSession.endorsed_designation || data.endorsedByDesignation || '',
    endorsedByDate: inSession.endorsed_date || data.endorsedByDate || '',
  }
})

/** Copies the details step's current state into the workflow's shared snapshot. */
const mergeStep1Form = (snapshot) => {
  proposalData.value = {
    ...proposalData.value,
    ...(snapshot || {}),
    ...callSelectionFields(selectedCall.value),
  }
}

/** Stores the certification data emitted by the Certification step (submit or draft). */
const mergeCertification = (certificationData) => {
  if (!certificationData) return
  proposalData.value = {
    ...proposalData.value,
    certification: certificationData.certification,
    signatureFiles: certificationData.signatureFiles,
    certificationSignature:
      certificationData.certificationSignature ?? proposalData.value.certificationSignature ?? null
  }
}

/**
 * Jumps to another step. Steps 2-4 are only reachable once a call has been selected,
 * so a request for a later step without a selection opens the Call for Proposal step
 * instead - the proponent can never reach the proposal form without a valid call.
 *
 * Entering a later step hydrates the shared snapshot from the steps that are already
 * open, so a jump straight to Review & Submit can never save an empty logical
 * framework / personnel / sites / certification in place of what was typed.
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
    mergeStep1Form(step1Ref.value?.getFormData?.())
    showCallModal.value = false
    showModal.value = false
    showCriteria.value = false
    showModal2.value = true
  } else if (step === 4) {
    mergeStep1Form(step1Ref.value?.getFormData?.())
    mergeCertification(step2Ref.value?.getCertificationData?.())
    showCallModal.value = false
    showModal.value = false
    showModal2.value = false
    showCriteria.value = true
  }
}

const goToStep2 = (formData) => {
  // Save data from Step 2 (Proposal Details) and open Step 3 (Certification). The
  // selected call travels with the proposal data.
  mergeStep1Form(formData)
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
  mergeCertification(certificationData)
  mergeStep1Form(step1Ref.value?.getFormData?.())
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

/**
 * Saves a draft from the Certification step. The workflow owns the one save path
 * (Review & Submit), so the details typed here and on the previous steps - and the
 * files chosen on both - are stored with the proposal.
 */
const handleStep2SaveDraft = async (certificationData) => {
  mergeCertification(certificationData)
  await saveDraftFromAnyStep()
}

/** Saves a draft from the Proposal Details step, with everything typed so far. */
const handleStep1SaveDraft = async (formData) => {
  mergeStep1Form(formData)
  await saveDraftFromAnyStep()
}

/**
 * Runs the silent save of the Review & Submit step and reports its outcome. The
 * attachments chosen in every step are uploaded by that one save path, so a draft
 * saved from any step keeps its files.
 */
const saveDraftFromAnyStep = async () => {
  // Whatever step triggered the save, the draft stores everything the proponent has
  // entered so far. Both steps stay mounted, so read their current state directly rather
  // than relying on the last emitted snapshot (which is missing when the user goes back
  // to Proposal Details and saves from there).
  mergeStep1Form(step1Ref.value?.getFormData?.())
  mergeCertification(step2Ref.value?.getCertificationData?.())
  // The Review & Submit step reads the shared snapshot, which was just replaced - let
  // the reactive props update before it builds the payload.
  await nextTick()
  const result = await step3Ref.value?.saveDraft?.()
  if (!result?.saved) {
    await showAlert(result?.message || 'Failed to save draft. Please try again.', {
      type: 'error',
      title: 'Save Failed'
    })
    return
  }
  // Keep the id of the draft that was created, so it is updated - never duplicated.
  if (result.proposalId) {
    proposalData.value = { ...proposalData.value, proposal_id: result.proposalId }
  }
  showModal.value = false
  showModal2.value = false
  showCriteria.value = false
  await showAlert(
    result.uploadError
      ? `Your draft was saved, but one attachment could not be uploaded: ${result.uploadError}`
      : 'Draft saved successfully!',
    { type: result.uploadError ? 'warning' : 'success', title: 'Draft Saved' }
  )
  resetAllForms()
  fetchProposals() // Refresh the list
}

const handleSaveDraft = () => {
  // Save as Draft from Review & Submit: that step already wrote the draft, so the
  // workflow only closes and refreshes.
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
/* ============================================================
   Proponent — My Projects
   Layout and typography only. Status tones stay with assets/status.css and
   action colours with assets/actions.css, except the row Open/View pair below,
   which gets a page-local green/blue so the two actions are distinguishable.
   ============================================================ */

.proposals-content {
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 0;               /* the shell's .app-content owns the gutter */
  background: transparent;  /* let the lavender canvas show through */
  color: #262342;
}

/* ===== Page header ===== */
.page-header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.page-heading {
  min-width: 0;
}

.page-title {
  /* Claim the full heading box: ProfileInformation.vue ships an UNscoped
     `.page-title` rule (dark #1e1c3a background, 16px padding, width:100%)
     that otherwise leaks onto this page and paints a dark block behind the
     dark-ink heading. Reset those inherited box properties here. */
  margin: 0;
  padding: 0;
  width: auto;
  background: transparent;
  color: #262342;
  font-size: var(--fs-xl, 1.5rem);
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.015em;
}

.subtitle {
  margin: 4px 0 0;
  color: #5b6478;
  font-size: var(--fs-sm, 0.875rem);
  line-height: 1.5;
}

.top-bar-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

/* ===== Header buttons ===== */
/* View Revisions: light orange surface + soft orange border so it reads as a control.
   Hover and press change colours only; the box never changes size or position. */
.btn-revisions {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid #fed7aa;
  border-radius: 10px;
  background: #fff0db;
  color: #c2410c;
  font-size: var(--fs-sm, 0.875rem);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(194, 65, 12, 0.08);
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.btn-revisions:hover:not(:disabled),
.btn-revisions:active:not(:disabled) {
  background: #ffedd5;
  border-color: #fdba74;
  color: #9a3412;
}

.btn-revisions:focus-visible {
  outline: 2px solid #c2410c;
  outline-offset: 2px;
}

/* Create New Proposal keeps the global blue action colour (assets/actions.css);
   only its geometry and shadow are refined here. */
.btn-new {
  min-height: 42px;
  padding: 0 18px;
  border-radius: 10px;
  font-size: var(--fs-sm, 0.875rem);
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(36, 82, 255, 0.18);
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.btn-new:hover:not(:disabled) {
  box-shadow: 0 6px 16px rgba(36, 82, 255, 0.24);
}

.btn-new:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* ===== Search + status filter ===== */
.filters-card {
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e6e8f4;
  border-radius: 14px;
  box-shadow: 0 4px 14px rgba(38, 35, 66, 0.05);
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.filters input[type='text'] {
  flex: 1 1 320px;
  min-width: 220px;
  height: 42px;
  /* Cancel the leftover margin-top that ProfileInformation.vue's unscoped
     `input` rule applies, so the search field aligns with the select. */
  margin: 0;
  padding: 0 14px;
  border: 1px solid #e3ddf2;
  border-radius: 10px;
  background: #fbfbff;
  color: #262342;
  font-size: var(--fs-sm, 0.875rem);
}

.filters select {
  flex: 0 0 190px;
  height: 42px;
  padding: 0 12px;
  border: 1px solid #e3ddf2;
  border-radius: 10px;
  background: #fbfbff;
  color: #262342;
  font-size: var(--fs-sm, 0.875rem);
  font-weight: 500;
  cursor: pointer;
}

.filters input[type='text']:focus,
.filters select:focus {
  outline: none;
  border-color: #8b7cc9;
  box-shadow: 0 0 0 3px rgba(107, 92, 165, 0.16);
}

/* ===== Loading / error / empty states ===== */
.state-message {
  margin: 0;
  padding: 26px 20px;
  text-align: center;
  background: #ffffff;
  border: 1px dashed #e3ddf2;
  border-radius: 14px;
  color: #6b7591;
  font-size: var(--fs-sm, 0.875rem);
}

.state-message.state-error {
  color: #b91c1c;
  border-color: #fecaca;
  background: #fff7f7;
}

/* ===== Table card ===== */
.table-card {
  background: #ffffff;
  border: 1px solid #e6e8f4;
  border-radius: 14px;
  box-shadow: 0 8px 24px rgba(38, 35, 66, 0.07);
  overflow: hidden;
}

.table-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.proposals-table {
  width: 100%;
  min-width: 920px;
  table-layout: fixed;
  border-collapse: collapse;
  background: #ffffff;
  font-size: var(--fs-sm, 0.875rem);
}

.col-id { width: 150px; }
.col-title { width: auto; }
.col-status { width: 192px; }
.col-submitted { width: 128px; }
.col-actions { width: 200px; }

/* Purple header band with the gold brand hairline. */
.proposals-table thead th {
  padding: 14px 16px;
  background: #4b3f72;
  color: #ffffff;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  text-align: left;
  white-space: nowrap;
  border-bottom: 3px solid var(--rpis-accent, #ffd000);
}

.proposals-table thead th.actions-head {
  text-align: right;
}

/* ===== Rows ===== */
.proposals-table tbody tr {
  background: #ffffff;
}

.proposals-table tbody td {
  height: 64px;
  padding: 10px 16px;
  vertical-align: middle;
  color: #262342;
  border-bottom: 1px solid #ece9f7;
  transition: background-color 0.15s ease;
}

.proposals-table tbody tr:last-child td {
  border-bottom: none;
}

.proposals-table tbody tr:hover td {
  background: #f6f2ff;
}

/* Proposal ID */
.cell-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, 'Liberation Mono', monospace;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #4b3f72;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* Title: two lines maximum, so every row keeps the same height. */
.cell-title {
  font-weight: 500;
  color: #262342;
}

.title-text {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  min-height: 2.9em;
  max-height: 2.9em;
  line-height: 1.45;
  overflow-wrap: anywhere;
}

.cell-date {
  color: #5b6478;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.cell-status {
  white-space: nowrap;
}

/* Badge geometry only. The tone background/text stay with assets/status.css. */
.proposals-table .status {
  display: inline-flex;
  align-items: center;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  line-height: 1.35;
  white-space: nowrap;
}

/* ===== Actions =====
   A fixed two-column grid on every row: View always occupies the first column and
   Delete the second, whether or not Delete is rendered. Rows without Delete fill the
   second column with an invisible placeholder, so nothing shifts and nothing jumps. */
.cell-actions {
  text-align: right;
}

.row-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  align-items: center;
  gap: 8px;
  width: 100%;
  max-width: 176px;
  margin-left: auto;
}

.row-actions .btn-view,
.row-actions .btn-delete {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  min-width: 0;
  height: 34px;
  margin: 0;
  padding: 0 8px;
  border-radius: 8px;
  font-size: 12.5px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
  transform: none;
}

/* Hovering or pressing only changes the action colour. */
.row-actions .btn-view:hover,
.row-actions .btn-delete:hover,
.row-actions .btn-view:active,
.row-actions .btn-delete:active {
  transform: none;
}

/* Row action colours, on this page only.
   The button is one element whose label flips between "Open" (an approved project) and
   "View" (a proposal still in the workflow); both used to share the global `.btn-view`
   blue. `action-open` is green and `action-view` is blue so the two read apart without
   touching assets/actions.css or any other page's buttons. Only colours are set here -
   the geometry above is unchanged. */
.row-actions .btn-view.action-open {
  background: #DCFCE7;
  border-color: #BBF7D0;
  color: #15803D;
}

.row-actions .btn-view.action-open:hover:not(:disabled),
.row-actions .btn-view.action-open:active:not(:disabled) {
  background: #BBF7D0;
  border-color: #BBF7D0;
  color: #166534;
}

.row-actions .btn-view.action-view {
  background: #DBEAFE;
  border-color: #BFDBFE;
  color: #1D4ED8;
}

.row-actions .btn-view.action-view:hover:not(:disabled),
.row-actions .btn-view.action-view:active:not(:disabled) {
  background: #BFDBFE;
  border-color: #BFDBFE;
  color: #1E40AF;
}

/* Dark theme: actions.css pins `.btn-view` to the dark palette with !important, which
   would flatten both rows back to the same colour. Keep the pair distinguishable by
   forcing the same two tones here; the scoped selector is the more specific one. */
@media (prefers-color-scheme: dark) {
  .row-actions .btn-view.action-open {
    background: #DCFCE7 !important;
    border-color: #BBF7D0 !important;
    color: #15803D !important;
  }

  .row-actions .btn-view.action-open:hover:not(:disabled),
  .row-actions .btn-view.action-open:active:not(:disabled) {
    background: #BBF7D0 !important;
    border-color: #BBF7D0 !important;
    color: #166534 !important;
  }

  .row-actions .btn-view.action-view {
    background: #DBEAFE !important;
    border-color: #BFDBFE !important;
    color: #1D4ED8 !important;
  }

  .row-actions .btn-view.action-view:hover:not(:disabled),
  .row-actions .btn-view.action-view:active:not(:disabled) {
    background: #BFDBFE !important;
    border-color: #BFDBFE !important;
    color: #1E40AF !important;
  }
}

.action-placeholder {
  display: block;
  height: 34px;
}

/* ===== No results ===== */
.proposals-table tbody td.empty-row {
  height: auto;
  padding: 34px 16px;
  text-align: center;
  color: #5b6478;
}

.error-text {
  color: #dc2626;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .page-header {
    align-items: flex-start;
  }

  .top-bar-actions {
    width: 100%;
  }

  .filters select {
    flex: 1 1 160px;
  }
}

@media (max-width: 640px) {
  .page-title {
    font-size: 1.25rem;
  }

  .filters {
    flex-direction: column;
    align-items: stretch;
  }

  .filters input[type='text'],
  .filters select {
    flex: 1 1 auto;
    width: 100%;
    min-width: 0;
  }

  .top-bar-actions .btn-revisions,
  .top-bar-actions .btn-new {
    flex: 1 1 auto;
    justify-content: center;
  }

  .filters-card {
    padding: 12px;
  }
}

/* ===== Dark theme =====
   base.css forces table/card/span colours with !important, so these overrides use
   !important too; the scoped [data-v] attribute gives them the higher specificity. */
@media (prefers-color-scheme: dark) {
  .proposals-content {
    background: transparent !important;
    color: #f1f5f9 !important;
  }

  .page-title {
    padding: 0 !important;
    width: auto !important;
    background: transparent !important;
    color: #e7e2fb !important;
  }

  .subtitle {
    color: #cbd5e1 !important;
  }

  .btn-revisions {
    background: #2c2748 !important;
    border-color: #8b7cc9 !important;
    color: #e6e0ff !important;
    box-shadow: none;
  }

  .btn-revisions:hover:not(:disabled),
  .btn-revisions:active:not(:disabled) {
    background: #3a3360 !important;
    border-color: #a394dd !important;
    color: #ffffff !important;
  }

  .filters-card,
  .state-message,
  .table-card {
    background: #22304d !important;
    border-color: #334155 !important;
    box-shadow: none;
  }

  .state-message.state-error {
    background: #3a2329 !important;
    border-color: #6b2a2a !important;
    color: #fca5a5 !important;
  }

  .filters input[type='text'],
  .filters select {
    background: #0f172a !important;
    border-color: #334155 !important;
    color: #f1f5f9 !important;
  }

  .filters input::placeholder {
    color: #64748b !important;
  }

  .filters input[type='text']:focus,
  .filters select:focus {
    border-color: #8ab4ff !important;
    box-shadow: 0 0 0 3px rgba(138, 180, 255, 0.22) !important;
  }

  .proposals-table {
    background: #22304d !important;
  }

  .proposals-table thead th {
    background: #3d3460 !important;
    color: #f5f3ff !important;
    border-bottom-color: #ffd000 !important;
    border-bottom-width: 3px !important;
  }

  .proposals-table tbody tr {
    background: #22304d !important;
  }

  .proposals-table tbody td {
    background: #22304d !important;
    color: #e2e8f0 !important;
    border-bottom-color: #334155 !important;
  }

  .proposals-table tbody tr:hover td {
    background: #2d3f63 !important;
  }

  .cell-id {
    color: #c4b8f0 !important;
  }

  .cell-title,
  .title-text {
    color: #f1f5f9 !important;
  }

  .cell-date {
    color: #94a3b8 !important;
  }

  .proposals-table tbody td.empty-row {
    color: #94a3b8 !important;
  }

  .error-text {
    color: #fca5a5 !important;
  }
}

/* ===== Print =====
   The global print guard hides the header and buttons; only the card clipping and
   the filter chrome have to go. */
@media print {
  .filters-card {
    display: none !important;
  }

  .table-card {
    border: none;
    box-shadow: none;
    overflow: visible;
  }

  .table-scroll {
    overflow: visible;
  }

  .proposals-table {
    min-width: 0;
    table-layout: auto;
    background: #ffffff;
  }

  .title-text {
    display: block;
    min-height: 0;
    max-height: none;
    -webkit-line-clamp: unset;
  }
}
</style>