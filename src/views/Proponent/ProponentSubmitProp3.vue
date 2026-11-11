<template>
  <div v-if="modelValue" class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <div class="header-left">
          <h2>Review & Submit</h2>
          <span class="header-badge">Step 4 of 4</span>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="close">✕</button>
        </div>
      </header>

      <!-- PROGRESS BAR -->
      <div class="progress-bar">
        <div class="progress-step completed" @click="$emit('goToStep', 1)">
          <span class="step-number">✓</span>
          <span class="step-label">Call for Proposal</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step completed" @click="$emit('goToStep', 2)">
          <span class="step-number">✓</span>
          <span class="step-label">Proposal Details</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step completed" @click="$emit('goToStep', 3)">
          <span class="step-number">✓</span>
          <span class="step-label">Certification</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step active">
          <span class="step-number">4</span>
          <span class="step-label">Review &amp; Submit</span>
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
                
                <div class="file-row" :class="{ attached: technologyMapAttached }">
                  <span class="file-name">Technology Roadmap</span>
                  <span class="file-status">{{ technologyMapAttached ? '✓ Attached' : '— Not attached' }}</span>
                </div>
                <!-- Section 19 is the interactive checklist, not a file. -->
                <div class="file-row" :class="{ attached: gadChecklistSummary(proposalData.gad_checklist).completed }">
                  <span class="file-name">GAD Checklist 2</span>
                  <span class="file-status">{{ gadChecklistStatus }}</span>
                </div>
                <div class="file-row" :class="{ attached: budgetAttached }">
                  <span class="file-name">Line-Item Budget</span>
                  <span class="file-status">{{ budgetAttached ? '✓ Attached' : '— Not attached' }}</span>
                </div>
                <div class="file-row" :class="{ attached: submittedSignatureAttached }">
                  <span class="file-name">Submitted By Signature</span>
                  <span class="file-status">{{ submittedSignatureAttached ? '✓ Attached' : '— Not attached' }}</span>
                  <span v-if="submittedSignaturePath" class="file-actions">
                    <button type="button" class="btn-view-file btn-view"
                      @click="previewUploadedFile(submittedSignaturePath)">View</button>
                    <button type="button" class="btn-view-file btn-download"
                      @click="downloadUploadedFile(submittedSignaturePath)">Download</button>
                  </span>
                </div>
                <div class="file-row" :class="{ attached: endorsementDocumentAttached }">
                  <span class="file-name">Endorsement Document</span>
                  <span class="file-status">{{ endorsementDocumentAttached ? '✓ Attached' : '— Not attached' }}</span>
                  <span v-if="endorsementDocumentPath" class="file-actions">
                    <button type="button" class="btn-view-file btn-view"
                      @click="previewUploadedFile(endorsementDocumentPath)">View</button>
                    <button type="button" class="btn-view-file btn-download"
                      @click="downloadUploadedFile(endorsementDocumentPath)">Download</button>
                  </span>
                </div>
              </div>
            </div>
          </section>

          <!-- Literature Cited (section 23): exactly what will be submitted, with the
               APA italics, so the proponent can verify the references before submitting. -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">03</span>
              <h3>Literature Cited</h3>
            </div>
            <div class="section-card">
              <ul v-if="literatureEntries.length" class="review-references">
                <li v-for="entry in literatureEntries" :key="entry.id" class="review-reference">
                  <span class="review-reference-text" v-html="entryHtml(entry)"></span>
                </li>
              </ul>
              <p v-else class="review-empty">No references were added.</p>
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
import {
  PHILIPPINES,
  formatAddressContact,
  isIndependentCity,
  loadCitiesMunicipalities,
} from '@/utils/philippineLocations'
import { validateContact } from '@/utils/contactDetails'
import { joinAgencyNames, normalizeSelectedIds } from '@/utils/cooperatingAgencies'
import {
  technologyMapKey,
  technologyMapUploadError,
  uploadTechnologyMap,
} from '@/utils/technologyMap'
import { budgetFileKey, budgetFileUploadError, uploadBudgetFile } from '@/utils/budgetFile'
// Section 19: the checklist is sent with the proposal; a never-started checklist leaves
// the stored one untouched, while an explicitly emptied one clears it.
import {
  gadChecklistPayloadForSave,
  gadChecklistSummary,
  gadChecklistValidation,
} from '@/utils/gadChecklist'
// "Submitted By" signature: uploaded to the proposal after it exists, exactly like the
// endorsement document, so the file stays associated with this proposal.
import {
  submittedSignatureKey,
  submittedSignatureUploadError,
  uploadSubmittedSignature,
} from '@/utils/submittedSignature'
// Section 21: the structured Risk Management Plan is validated with the same rule the form
// uses, so the review page and the form can never disagree about what is missing.
import { riskEntriesPayload, validateRiskPlan } from '@/utils/riskManagement'
// The endorsement document ("Endorsed By") is uploaded right after the proposal is saved.
import {
  endorsementDocumentKey,
  endorsementDocumentUploadError,
  uploadEndorsementDocument,
} from '@/utils/endorsementDocument'
// Section 24: "% Time Devoted" must be a number between 0 and 100.
import { timeDevotedPayload, validateTimeDevotedRows } from '@/utils/personnelTime'
// Section 23: the references shown on the review come from the same entry list the
// form uses (plain text + the structured entries of the reference builder).
import { entryHtml, parseReferenceEntries } from '@/utils/apaReferences'
// Saved attachments are downloaded / previewed through the authorized file route.
import { downloadUploadedFile, previewUploadedFile } from '@/utils/fileDownload'
// The endorsement workflow: the proposal is linked to the endorsement it was started
// from, so the two records are associated by a foreign key and never by title.
import { linkEndorsementToProposal } from '@/utils/endorsement.js'

const { dialogState, showAlert, showConfirm } = useDialog()

const props = defineProps({
  modelValue: Boolean,
  proposalData: {
    type: Object,
    default: () => ({})
  },
  /**
   * The approved Dean endorsement this proposal was started from, when the
   * Proponent arrived through the endorsement workflow ("Continue to Proposal
   * Submission"). After the proposal is saved it is linked to that endorsement with
   * a stable database relationship, never by matching titles. Null for every other
   * proposal, which keeps working exactly as before.
   */
  endorsementRequestId: {
    type: [Number, String],
    default: null
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

// ===== TECHNOLOGY MAP (section 13) =====
// The image is chosen in step 1 and travels with the draft. It is uploaded here,
// once, right after the proposal has been saved (see saveOrUpdateProposal), so the
// image is attached to the exact proposal that was created or updated.
const uploadedTechnologyMapKey = ref('')

/** The chosen image, if any (a saved file name is not a file). */
const technologyMapFile = computed(() => {
  const file = props.proposalData?.technology_roadmap_file
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

const uploadTechnologyMapIfNeeded = async (proposalId) => {
  const file = technologyMapFile.value
  if (!proposalId || !file) return { ok: true, message: '' }
  const key = technologyMapKey(file)
  if (key === uploadedTechnologyMapKey.value) return { ok: true, message: '' }

  try {
    const saved = await uploadTechnologyMap(proposalId, file)
    uploadedTechnologyMapKey.value = key
    // Keep the authoritative name / path from the server, so the review payload and the
    // reopened draft describe the image that is actually attached.
    if (saved) {
      props.proposalData.roadmap_file_name = saved.roadmapFileName || props.proposalData.roadmap_file_name || ''
      props.proposalData.roadmap_file_path = saved.roadmapFilePath || props.proposalData.roadmap_file_path || ''
    }
    return { ok: true, message: '' }
  } catch (err) {
    // The proposal itself may already be saved; only the image could not be attached, so
    // the failure is reported without hiding the saved proposal.
    const message = technologyMapUploadError(err)
    await showAlert(message, { type: 'error', title: 'Technology Map Not Uploaded' })
    return { ok: false, message }
  }
}

// ===== LINE-ITEM BUDGET FILE (section 25) =====
// The filled-out budget is chosen in step 1 and travels with the draft. Like the
// technology map it is uploaded here, once, right after the proposal has been
// saved, so the file is attached to the exact proposal that was created or updated.
const uploadedBudgetFileKey = ref('')

/** The chosen file, if any (a saved file name is not a file). */
const budgetFileToUpload = computed(() => {
  const file = props.proposalData?.line_item_budget_file
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

/**
 * Section 23: the references of the proposal, read from the same entry list the form
 * builds (the structured entries when the reference builder was used, otherwise the
 * stored plain text). The review therefore shows exactly what will be submitted.
 */
const literatureEntries = computed(() =>
  parseReferenceEntries(props.proposalData?.literature_json, props.proposalData?.literature_cited),
)

/* ===== UPLOADED DOCUMENTS SUMMARY =====
   Every row reflects the real state of the proposal: a file chosen in this session, a
   file already stored on a reopened draft, or - for section 19 - the checklist itself
   (which is data, not an upload). */
const technologyMapAttached = computed(() =>
  Boolean(
    props.proposalData?.technology_roadmap_file?.name ||
      props.proposalData?.roadmap_file_path ||
      props.proposalData?.roadmap_file_name,
  ),
)
const budgetAttached = computed(() =>
  Boolean(
    props.proposalData?.line_item_budget_file ||
      props.proposalData?.budget_file_path ||
      props.proposalData?.budget_file_name,
  ),
)
const submittedSignaturePath = computed(
  () => props.proposalData?.submittedByFilePath || props.proposalData?.submittedByFileName || '',
)
const submittedSignatureAttached = computed(() =>
  Boolean(submittedSignaturePath.value || props.proposalData?.signatureFiles?.submitted),
)
const endorsementDocumentPath = computed(
  () => props.proposalData?.endorsementFilePath || props.proposalData?.endorsementFileName || '',
)
const endorsementDocumentAttached = computed(() =>
  Boolean(
    endorsementDocumentPath.value ||
      props.proposalData?.signatureFiles?.endorsed ||
      props.endorsementRequestId,
  ),
)
/** "Complete (12.50/20.00)" or "N of 17 answered" - never a missing-file message. */
const gadChecklistStatus = computed(() => {
  const summary = gadChecklistSummary(props.proposalData?.gad_checklist)
  return summary.completed
    ? `✓ Complete — ${summary.formattedTotal} / ${summary.maxScore}`
    : `${summary.answeredCount} of ${summary.itemCount} answered — not yet complete`
})

const uploadBudgetFileIfNeeded = async (proposalId) => {
  const file = budgetFileToUpload.value
  if (!proposalId || !file) return { ok: true, message: '' }
  const key = budgetFileKey(file)
  if (key === uploadedBudgetFileKey.value) return { ok: true, message: '' }

  try {
    const saved = await uploadBudgetFile(proposalId, file)
    uploadedBudgetFileKey.value = key
    if (saved) {
      props.proposalData.budget_file_name = saved.beneficiariesFileName || props.proposalData.budget_file_name || ''
      props.proposalData.budget_file_path = saved.beneficiariesFilePath || props.proposalData.budget_file_path || ''
    }
    return { ok: true, message: '' }
  } catch (err) {
    // The proposal itself may already be saved; only the file could not be attached, so
    // the failure is reported without hiding the saved proposal.
    const message = budgetFileUploadError(err)
    await showAlert(message, { type: 'error', title: 'Budget File Not Uploaded' })
    return { ok: false, message }
  }
}

/* ===== ENDORSING DOCUMENT (Certification & Endorsement step, "Endorsed By") =====
   The endorsement PDF chosen in step 3 travels with the draft and is uploaded right
   after the proposal is saved, exactly like the Technology Map and the Line-Item Budget,
   so the document is attached to this exact proposal and stays retrievable (the
   authorized file route serves it to the Proponent and the reviewers). */
const uploadedEndorsementDocumentKey = ref('')

/** The chosen document, if any (a saved file name is not a file). */
const endorsementDocumentToUpload = computed(() => {
  const file = props.proposalData?.signatureFiles?.endorsed
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

const uploadEndorsementDocumentIfNeeded = async (proposalId) => {
  const file = endorsementDocumentToUpload.value
  if (!proposalId || !file) return { ok: true, message: '' }
  const key = endorsementDocumentKey(file)
  if (key === uploadedEndorsementDocumentKey.value) return { ok: true, message: '' }

  try {
    const saved = await uploadEndorsementDocument(proposalId, file)
    uploadedEndorsementDocumentKey.value = key
    if (saved) {
      props.proposalData.endorsementFileName =
        saved.endorsementFileName || props.proposalData.endorsementFileName || ''
      props.proposalData.endorsementFilePath =
        saved.endorsementFilePath || props.proposalData.endorsementFilePath || ''
    }
    return { ok: true, message: '' }
  } catch (err) {
    // The proposal itself may already be saved; only the document could not be attached,
    // so the failure is reported (with the backend's own reason) without hiding the result.
    const message = endorsementDocumentUploadError(err)
    await showAlert(message, { type: 'error', title: 'Endorsement Document Not Uploaded' })
    return { ok: false, message }
  }
}

/* ===== SUBMITTED BY SIGNATURE (Certification & Endorsement step) =====
   The signature chosen in step 3 travels with the draft and is uploaded right after the
   proposal exists, exactly like the endorsement document. The name / path it stores on
   the proposal are what a reopened draft shows and what the authorized file route
   serves to the Proponent and the reviewers. */
const uploadedSubmittedSignatureKey = ref('')

/** The chosen signature, if any (a saved file name is not a file). */
const submittedSignatureToUpload = computed(() => {
  const file = props.proposalData?.signatureFiles?.submitted
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

const uploadSubmittedSignatureIfNeeded = async (proposalId) => {
  const file = submittedSignatureToUpload.value
  if (!proposalId || !file) return { ok: true, message: '' }
  const key = submittedSignatureKey(file)
  if (key === uploadedSubmittedSignatureKey.value) return { ok: true, message: '' }

  try {
    const saved = await uploadSubmittedSignature(proposalId, file)
    uploadedSubmittedSignatureKey.value = key
    if (saved) {
      props.proposalData.submittedByFileName =
        saved.submittedByFileName || props.proposalData.submittedByFileName || ''
      props.proposalData.submittedByFilePath =
        saved.submittedByFilePath || props.proposalData.submittedByFilePath || ''
    }
    return { ok: true, message: '' }
  } catch (err) {
    const message = submittedSignatureUploadError(err)
    await showAlert(message, { type: 'error', title: 'Signature Not Uploaded' })
    return { ok: false, message }
  }
}

const resetForm = () => {
  confirmed.value = false
  submitted.value = false
  submitting.value = false
  submitMessage.value = ''
  error.value = null
  uploadedTechnologyMapKey.value = ''
  uploadedBudgetFileKey.value = ''
  // Without this, a reopened workflow could believe the previous session's endorsement
  // PDF was already uploaded and skip a real one.
  uploadedEndorsementDocumentKey.value = ''
  uploadedSubmittedSignatureKey.value = ''
}

/**
 * The workflow's silent "Save as Draft" (called by the parent when any step saves). It
 * runs the same save path as the Review & Submit button and returns a result instead of
 * showing the success alert itself:
 * `{ saved, proposalId, uploadError }`. `saved` is false only when the proposal itself
 * could not be written - an attachment that failed to upload still leaves the draft
 * saved, with its reason in `uploadError`.
 */
async function saveDraft() {
  try {
    return await saveOrUpdateProposal('DRAFT')
  } catch (err) {
    const body = err?.response?.data
    const message =
      (body && typeof body === 'object' && body.error) ||
      (typeof body === 'string' && body.trim() !== '' ? body : '') ||
      err?.message ||
      'Failed to save draft.'
    error.value = String(message)
    return {
      saved: false,
      proposalId: props.proposalData?.proposal_id || null,
      uploadError: '',
      message: String(message)
    }
  }
}

defineExpose({ resetForm, saveDraft })

const close = () => emit('update:modelValue', false)

const goBack = () => {
  emit('back')
}

// ===== FINAL COMPLETENESS GUARD =====
const isBlank = (val) => !val || (typeof val === 'string' && val.trim() === '')

const sixPsList = [
  { key: 'publications', label: 'Publications' },
  { key: 'patents', label: 'Patents/IP' },
  { key: 'products', label: 'Products' },
  { key: 'people_services', label: 'People Services' },
  { key: 'partnerships', label: 'Partnerships' },
  { key: 'policy', label: 'Policy' },
]

const serializeSixPs = (expectedOutputs) => {
  const lines = []
  sixPsList.forEach((p) => {
    const item = expectedOutputs && expectedOutputs[p.key]
    if (item && item.selected && item.value && item.value.trim() !== '') {
      lines.push(`${p.label}: ${item.value.trim()}`)
    }
  })
  return lines.join('\n')
}

// Bundled PSGC cities / municipalities, read when the review page opens. They
// tell a complete address whose city belongs to no province and no district
// (City of Isabela, City of Cotabato) apart from an address that is simply
// missing its province.
const citiesMunicipalities = ref([])
const citiesRead = ref(false)

watch(
  () => props.modelValue,
  async (open) => {
    if (!open || citiesRead.value) return
    try {
      citiesMunicipalities.value = await loadCitiesMunicipalities()
    } catch (err) {
      // The dataset ships with the app; if it cannot be read, the province /
      // district rule is left to step 1 and to the backend instead of blocking a
      // valid address here.
      console.error('Failed to load the PSGC cities / municipalities:', err?.message)
      citiesMunicipalities.value = []
    } finally {
      citiesRead.value = true
    }
  },
  { immediate: true },
)

/**
 * The certification value to use: what the proponent typed in this session, else what a
 * reopened draft already stored. Both must be blank for the field to count as missing,
 * so a draft's stored certification is never reported as absent on final submission.
 */
const certificationValue = (typed, stored) => {
  const current = typeof typed === 'string' ? typed.trim() : ''
  if (current) return current
  const saved = typeof stored === 'string' ? stored.trim() : ''
  return saved
}

const validationErrors = computed(() => {
  const errs = []
  const d = props.proposalData || {}

  // Step 1 fields
  if (isBlank(d.project_title)) errs.push('Project Title is missing.')
  if (isBlank(d.project_leader)) errs.push('Project Leader is missing.')
  if (isBlank(d.project_leader_sex)) errs.push('Sex is not selected.')
  if (isBlank(d.duration)) errs.push('Duration is missing.')
  if (isBlank(d.start_date)) errs.push('Start Date is missing.')
  if (isBlank(d.end_date)) errs.push('End Date is missing.')
  if (isBlank(d.department)) errs.push('Department is missing.')
  // Address and Contact are two separate sections now; step 1 validates them in
  // full, this is the review page's summary check. The Address section is
  // Philippine-only, so the country is not asked for (it is filled in when the
  // proposal is submitted). The province / district is required unless the
  // selected city belongs to no province and no district.
  const address = d.address_details || {}
  const addressParts = ['region', 'municipality', 'barangay']
  const areaLevelApplies =
    !isBlank(address.province) ||
    !isBlank(address.district) ||
    (citiesMunicipalities.value.length > 0 &&
      !isIndependentCity(citiesMunicipalities.value, address.municipality))
  if (addressParts.some((part) => isBlank(address[part])) || !areaLevelApplies) {
    errs.push('Address is incomplete.')
  }
  const contactError = validateContact(d.contact_details)
  if (contactError) errs.push(`Contact: ${contactError}`)
  // Section 02: at least one agency of the shared directory must be selected. A
  // proposal saved before the checklist (free text only) keeps the previous rule.
  if (!normalizeSelectedIds(d.cooperating_agency_ids).length && isBlank(d.cooperating_agencies)) {
    errs.push('Cooperating Agencies is missing.')
  }

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
  if (!Array.isArray(d.sustainable_development_goals) || d.sustainable_development_goals.length === 0) errs.push('SDG is missing.')
  if (isBlank(d.executive_summary)) errs.push('Executive Summary is missing.')
  if (isBlank(d.rationale)) errs.push('Rationale is missing.')
  if (isBlank(d.theoretical_framework)) errs.push('Theoretical Framework is missing.')
  if (isBlank(d.general_objective)) errs.push('General Objective is missing.')
  if (isBlank(d.specific_objectives)) errs.push('Specific Objectives is missing.')
  if (isBlank(d.review_of_literature)) errs.push('Review of Literature is missing.')
  if (isBlank(d.methodology)) errs.push('Methodology is missing.')
  const outputs = d.expected_outputs || {}
  const selectedOutputs = Object.entries(outputs).filter(([, v]) => v.selected)
  if (selectedOutputs.length === 0) {
    errs.push('No Expected Output (6P) selected.')
  } else if (selectedOutputs.some(([, v]) => isBlank(v.value))) {
    errs.push('Expected Output (6P) details are incomplete.')
  }
  if (isBlank(d.potential_outcomes)) errs.push('Potential Outcomes is missing.')
  if (isBlank(d.economic_impact)) errs.push('Economic Impact is missing.')
  if (isBlank(d.social_ethical_impact)) errs.push('Social / Ethical Impact is missing.')
  if (isBlank(d.target_beneficiaries)) errs.push('Target Beneficiaries is missing.')
  if (isBlank(d.sustainability_plan)) errs.push('Sustainability Plan is missing.')
  // Section 19 is the interactive GAD Checklist 2, not a file upload: every one of its
  // 17 assessment items needs a response. A legacy GAD document is no longer demanded.
  const gadCheck = gadChecklistValidation(d.gad_checklist)
  if (!gadCheck.valid) errs.push(gadCheck.message)
  if (isBlank(d.limitations)) errs.push('Limitations is missing.')
  // Section 21: the structured Risk Management Plan. Every entry the proponent started must
  // be complete and at least one entry must exist; a proposal whose plan is only the old
  // single text (no structured entry started) stays valid, exactly like in the form.
  const riskPlan = validateRiskPlan(d.risk_management, { legacyText: d.risks_assumptions })
  if (!riskPlan.valid) errs.push(`Risk Management Plan: ${riskPlan.message}`)

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
  // "% Time Devoted" is a percentage: a value that is not a number between 0 and 100
  // (e.g. prose saved before the field was numeric) is reported with its row number.
  const personnelTime = validateTimeDevotedRows(d.personnel_requirements)
  if (!personnelTime.valid) errs.push(personnelTime.message)

  // A file chosen in this session or a file already attached to the proposal (restored
  // when a draft is reopened) both satisfy the requirement.
  if (!d.line_item_budget_file && !d.budget_file_path && !d.budget_file_name) {
    errs.push('Line-Item Budget file is missing.')
  }

  // Certification & Endorsement (step 3). These are final-submission requirements only:
  // saving a draft never runs this check, so an incomplete certification does not block
  // a draft. A signature / endorsement PDF already stored on the proposal satisfies the
  // document requirement, so reopening a draft never demands it again.
  const cert = d.certification || {}
  const signatureFiles = d.signatureFiles || {}
  if (isBlank(certificationValue(cert.submitted_name, d.submittedByName))) {
    errs.push('Submitted By: Full Name is missing.')
  }
  if (isBlank(certificationValue(cert.submitted_designation, d.submittedByDesignation))) {
    errs.push('Submitted By: Designation is missing.')
  }
  if (isBlank(certificationValue(cert.submitted_date, d.submittedByDate))) {
    errs.push('Submitted By: Date is missing.')
  }
  if (!signatureFiles.submitted && !d.submittedByFilePath && !d.submittedByFileName) {
    errs.push('Submitted By: Signature file is missing.')
  }
  if (isBlank(certificationValue(cert.endorsed_name, d.endorsedByName))) {
    errs.push('Endorsed By: Full Name is missing.')
  }
  if (isBlank(certificationValue(cert.endorsed_designation, d.endorsedByDesignation))) {
    errs.push('Endorsed By: Designation is missing.')
  }
  if (isBlank(certificationValue(cert.endorsed_date, d.endorsedByDate))) {
    errs.push('Endorsed By: Date is missing.')
  }
  if (
    !signatureFiles.endorsed &&
    !d.endorsementFilePath &&
    !d.endorsementFileName &&
    !props.endorsementRequestId
  ) {
    errs.push('Endorsed By: Endorsement Document is missing.')
  }

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
    const body = err.response?.data
    const backendMessage = body && typeof body === 'object' ? body.error : body
    error.value = backendMessage || err.message || 'Failed to save draft. Please try again.'
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
    // An upload failure is thrown with a readable message before the SUBMITTED status is
    // written; it must reach the proponent instead of a generic "failed" text.
    const body = err.response?.data
    const backendMessage = body && typeof body === 'object' ? body.error : body
    error.value = backendMessage || err.message || 'Failed to submit proposal. Please try again.'
  } finally {
    submitting.value = false
  }
}

/**
 * Builds the proposal payload from the shared workflow data.
 *
 * It is called twice by the save path: once with status DRAFT to create a draft that the
 * attachments can be uploaded to, and once with the requested status after the uploads, so
 * the version snapshot recorded on submission sees the stored file paths. Building it
 * rather than keeping one object is why the upload responses' authoritative file names
 * reach the final payload.
 *
 * A blank text field that belongs to a file or the certification is sent as `null`, which
 * the backend treats as "preserve the stored value": an empty form control never deletes a
 * file or a certification that is already saved.
 */
const buildProposalPayload = (data, proponentId, status) => {
  const agencyIds = normalizeSelectedIds(data.cooperating_agency_ids)
  const agencyText = joinAgencyNames(data.cooperating_agency_names)
  const certification = data.certification || {}
  return {
    proponentId,
    // Step 1 of the workflow: the Call for Proposal this proposal responds to. The
    // backend attaches the proposal to that exact call and re-validates that the call
    // is still open before the submission is accepted.
    proposalAnnouncementId: data.proposal_announcement_id || null,
    programTitle: data.program_title || '',
    projectTitle: data.project_title || '',
    projectLeader: data.project_leader || '',
    projectLeaderSex: data.project_leader_sex || '',
    duration: data.duration || '',
    startDate: data.start_date || '',
    endDate: data.end_date || '',
    college: data.department || '',
    // The combined "Address / Contact" column stays in sync with the two new
    // sections, so existing screens and exports keep showing both.
    address: formatAddressContact(data.address_details, data.contact_details),
    // The Address section is Philippine-only: the country is never asked for, so it
    // is filled in here (a stored country - a legacy record - is kept as it is).
    addressCountry: data.address_details?.country || PHILIPPINES.name,
    addressRegion: data.address_details?.region || '',
    addressProvince: data.address_details?.province || '',
    addressDistrict: data.address_details?.district || '',
    addressMunicipality: data.address_details?.municipality || '',
    addressBarangay: data.address_details?.barangay || '',
    contactType: data.contact_details?.type || '',
    contactValue: (data.contact_details?.value || '').trim(),
    // Section 02: the checklist selection travels as directory ids, and the legacy
    // text column is derived from the selected names (the backend derives it from
    // the ids again, so the text and the relation can never drift apart).
    //
    // The ids are `null` when nothing is selected: on a reopened draft the checklist
    // loads its stored selection asynchronously, so an empty snapshot must mean "this
    // client has not loaded the selection", never "clear it". A genuine selection is
    // always non-empty, and final submission still requires one.
    cooperatingAgencies: agencyIds.length ? agencyText || null : data.cooperating_agencies || null,
    cooperatingAgencyIds: agencyIds.length ? agencyIds : null,
    researchType: data.research_type || '',
    innovationGoals: data.innovation_goals || '',
    sectorRelevance: data.sector_relevance || '',
    sdg: data.sustainable_development_goals || [],
    executiveSummary: data.executive_summary || '',
    rationale: data.rationale || '',
    framework: data.theoretical_framework || '',
    objectivesGeneral: data.general_objective || '',
    objectivesSpecific: data.specific_objectives || '',
    methodology: data.methodology || '',
    review: data.review_of_literature || '',
    // Section 11 is text. The name of a legacy document is only preserved, never
    // required; `null` leaves whatever the proposal already stores.
    reviewFileName: data.review_file_name || null,
    // Section 13: the name of the chosen technology map (or the stored one). The bytes
    // are uploaded after the proposal exists (see uploadTechnologyMapIfNeeded), and that
    // upload stores the authoritative name / path.
    roadmapFileName: data.roadmap_file_name || data.technology_roadmap_file?.name || null,
    limitationsText: data.limitations || '',
    outputs: serializeSixPs(data.expected_outputs),
    outcomes: data.potential_outcomes || '',
    impactEconomic: data.economic_impact || '',
    impactSocial: data.social_ethical_impact || '',
    beneficiaries: data.target_beneficiaries || '',
    // Section 25: the file name of the filled-out Line-Item Budget. The bytes are
    // uploaded right after the proposal exists (see uploadBudgetFileIfNeeded), and
    // that upload stores the authoritative name / path on the proposal itself.
    beneficiariesFileName: data.budget_file_name || data.line_item_budget_file?.name || null,
    sustainability: data.sustainability_plan || '',
    risks: data.risks_assumptions || '',
    // Section 21: the structured Risk Management Plan (identified risks, likelihood,
    // impact and mitigation measure per entry) built in step 1.
    riskManagement: riskEntriesPayload(data.risk_management),
    otherProjectsNumber: data.other_projects_number || '',
    referencesText: data.literature_cited || '',
    // Section 23: the structured entries behind the list (italics + editing).
    referencesJson: data.literature_json || '',
    // Section 19: the interactive checklist. `null` means the proposal never had one
    // and nothing was entered, so a legacy score/document is preserved; once started or
    // once saved, the current state (even an emptied one) is sent.
    gadChecklist: gadChecklistPayloadForSave(data.gad_checklist, data.gad_checklist_saved),
    // The name of a legacy GAD document is preserved, never required.
    gadFileName: data.gad_file_name || null,
    // Certification & Endorsement (step 3). The value typed in this session wins, and a
    // reopened draft's stored value is sent back unchanged when the step has not been
    // touched (the Certification step stays mounted, so its state is always available).
    // Only a genuinely blank pair is sent as `null`, which the backend treats as
    // "preserve the stored value"; an empty form control never erases the certification.
    submittedByName: certificationValue(certification.submitted_name, data.submittedByName) || null,
    submittedByDesignation:
      certificationValue(certification.submitted_designation, data.submittedByDesignation) || null,
    submittedByDate: certificationValue(certification.submitted_date, data.submittedByDate) || null,
    endorsedByName: certificationValue(certification.endorsed_name, data.endorsedByName) || null,
    endorsedByDesignation:
      certificationValue(certification.endorsed_designation, data.endorsedByDesignation) || null,
    endorsedByDate: certificationValue(certification.endorsed_date, data.endorsedByDate) || null,
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
      time: timeDevotedPayload(p.effort),
      responsibilities: p.responsibilities || ''
    })),
    otherProjects: (data.other_projects || []).map(op => ({
      title: op.project_title || '',
      agency: op.funding_agency || '',
      involvement: op.involvement || ''
    }))
  }
}

/**
 * The single save path of the workflow.
 *
 * 1. A proposal that has no id yet is created as a DRAFT first - the id is kept on the
 *    shared data, so saving again updates that draft instead of creating a duplicate.
 * 2. Every chosen attachment is uploaded to that proposal (technology map, budget,
 *    endorsement PDF, submitted signature) and the endorsement is linked.
 * 3. The requested status is written last, so a version snapshot recorded on submission
 *    sees the stored file paths.
 *
 * On a DRAFT save an upload failure still leaves the saved draft, with the reason
 * returned; on a SUBMITTED save it aborts before the status is written, so the proposal
 * stays an editable draft with its files already attached.
 */
const saveOrUpdateProposal = async (status) => {
  const data = props.proposalData
  const proponentId = userStore.user?.id

  if (!proponentId) {
    throw new Error('User not logged in')
  }

  let proposalId = data.proposal_id || null
  if (!proposalId) {
    const created = await api.post(
      '/api/proposals',
      buildProposalPayload(data, proponentId, 'DRAFT'),
    )
    proposalId = created.data?.id ?? null
    if (proposalId) {
      // Remember the draft that was just created: every later save updates it, and the
      // attachments below are stored on this exact record.
      data.proposal_id = proposalId
    }
  }
  if (!proposalId) {
    throw new Error('The proposal could not be created.')
  }

  // The attachments are uploaded right after the proposal exists, so each one is stored
  // on this exact proposal and the name / path it stores is authoritative.
  const uploadResults = []
  uploadResults.push(await uploadTechnologyMapIfNeeded(proposalId))

  // The proposal was started from an approved endorsement: link the two records. The
  // backend validates the ownership, the approval and that one endorsement backs at
  // most one proposal; a failure here never invalidates the saved proposal, so it is
  // reported without blocking the submission.
  await linkEndorsementIfNeeded(proposalId)

  uploadResults.push(await uploadBudgetFileIfNeeded(proposalId))
  uploadResults.push(await uploadEndorsementDocumentIfNeeded(proposalId))
  uploadResults.push(await uploadSubmittedSignatureIfNeeded(proposalId))

  const failures = uploadResults.filter((result) => !result.ok)
  if (failures.length && status === 'SUBMITTED') {
    // Never claim a file was stored: the submission is stopped before the SUBMITTED
    // status is written, and the saved proposal remains an editable draft.
    throw new Error(failures[0].message)
  }

  const payload = buildProposalPayload(data, proponentId, status)
  await api.put(`/api/proposals/${proposalId}`, payload)
  // From now on an emptied checklist is an explicit clear, not "never started".
  if (payload.gadChecklist) data.gad_checklist_saved = true

  return {
    saved: true,
    proposalId,
    uploadError: failures.length ? failures[0].message : ''
  }
}

/**
 * Links the approved endorsement the Proponent continued from, when there is one.
 * Idempotent on the backend, so re-saving the proposal never duplicates the link.
 */
async function linkEndorsementIfNeeded(proposalId) {
  const endorsementId = props.endorsementRequestId
  if (!endorsementId || !proposalId) return
  try {
    await linkEndorsementToProposal(endorsementId, proposalId)
  } catch (err) {
    console.warn('The endorsement could not be linked to the proposal.', err)
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
  color: #ffffff;
  background: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 700;
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

/* ===== LITERATURE CITED (section 23) =====
   The references exactly as they will be submitted: one entry each, with the APA
   italics and a hanging indent. */
.review-references {
  margin: 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.review-reference {
  font-size: 13px;
  line-height: 1.7;
  color: #1e293b;
}

.review-reference-text {
  display: block;
  padding-left: 2em;
  text-indent: -2em;
  overflow-wrap: anywhere;
}

.review-empty {
  margin: 0;
  font-size: 13px;
  color: #64748b;
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
  color: var(--status-neutral-text) !important;
}

.file-row.attached .file-status {
  color: var(--status-approved-text) !important;
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

/* Layout only; the colours come from assets/actions.css. */
.btn-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary:hover {
  transform: translateY(-1px);
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
