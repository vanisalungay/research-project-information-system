<template>
  <div class="end-page">
    <div class="end-page-header">
      <h2>Endorsement Request</h2>
      <p class="end-subtitle">
        {{ request ? request.referenceNumber : '' }}
        <template v-if="request"> · {{ endorsementStatusDescription(request.status) }}</template>
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading the request…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <router-link class="end-btn" to="/dean-requests">Back to the list</router-link>
      </div>
    </div>

    <template v-else-if="request">
      <div v-if="actionMessage" class="end-banner is-success">
        <span>{{ actionMessage }}</span>
      </div>
      <div v-if="actionError" class="end-banner is-error">
        <span>{{ actionError }}</span>
      </div>

      <!-- REQUEST -->
      <div class="end-card">
        <div class="end-toolbar">
          <span class="end-badge" :class="endorsementStatusTone(request.status)">
            {{ endorsementStatusLabel(request.status) }}
          </span>
          <span class="end-muted">Submitted {{ formatEndorsementTimestamp(request.createdAt) }}</span>
        </div>

        <div class="end-grid">
          <div class="end-field">
            <span class="end-label">Proponent</span>
            <span class="end-value">
              {{ request.proponentName }}
              <div class="end-muted">{{ request.proponentEmail }}</div>
            </span>
          </div>
          <div class="end-field">
            <span class="end-label">Program Title</span>
            <span class="end-value">{{ request.programTitle || '—' }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">Project Title</span>
            <span class="end-value">{{ request.projectTitle }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">Call for Proposal</span>
            <span class="end-value">{{ request.proposalAnnouncementName || '—' }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">Funding program</span>
            <span class="end-value">{{ fundingProgramLabel(request) }}</span>
          </div>
          <div class="end-field">
            <span class="end-label">College / Department</span>
            <span class="end-value">
              {{ request.college }}<template v-if="request.department"> · {{ request.department }}</template>
            </span>
          </div>
          <div class="end-field">
            <span class="end-label">Submission date</span>
            <span class="end-value">{{ formatEndorsementTimestamp(request.createdAt) }}</span>
          </div>
        </div>

        <div v-if="request.researchSummary" class="end-field">
          <span class="end-label">Research description</span>
          <span class="end-value">{{ request.researchSummary }}</span>
        </div>
        <div v-if="request.notes" class="end-field">
          <span class="end-label">Notes for the Dean</span>
          <span class="end-value">{{ request.notes }}</span>
        </div>
      </div>

      <!-- DECISION ALREADY TAKEN -->
      <div v-if="!isPending(request.status)" class="end-card">
        <h3 class="end-section-title">Decision</h3>
        <div class="end-grid">
          <div class="end-field">
            <span class="end-label">Decided by</span>
            <span class="end-value">
              {{ request.decidedByName || '—' }}
              <template v-if="request.decidedByDesignation">
                ({{ request.decidedByDesignation }})
              </template>
            </span>
          </div>
          <div class="end-field">
            <span class="end-label">Date of endorsement</span>
            <span class="end-value">{{ formatEndorsementDate(request.endorsementDate) || '—' }}</span>
          </div>
          <div v-if="request.letterDocumentCode" class="end-field">
            <span class="end-label">Document code</span>
            <span class="end-value">{{ request.letterDocumentCode }}</span>
          </div>
          <div v-if="request.letterVersion" class="end-field">
            <span class="end-label">Letter version</span>
            <span class="end-value">{{ request.letterVersion }}</span>
          </div>
        </div>

        <div v-if="isRejected(request.status)" class="end-note is-warning">
          <strong>Reason for rejection:</strong> {{ request.rejectionReason }}
        </div>

        <div v-if="request.hasLetter" class="end-actions">
          <button class="end-btn" :disabled="busy" @click="previewLetter">View issued letter</button>
          <button class="end-btn" :disabled="busy" @click="downloadLetter">Download</button>
        </div>
      </div>

      <!-- APPROVE -->
      <div v-if="isPending(request.status)" class="end-card">
        <h3 class="end-section-title">Approve and issue the endorsement letter</h3>

        <div v-if="blockedReason" class="end-note is-warning">{{ blockedReason }}</div>

        <div class="end-grid">
          <div class="end-field">
            <label for="endorserName">Full Name of Endorser <span class="end-req">*</span></label>
            <input
              id="endorserName"
              v-model="approvalForm.endorserName"
              class="end-input"
              type="text"
              :disabled="busy || Boolean(blockedReason)"
            />
            <span v-if="showApprovalErrors && approvalErrors.endorserName" class="end-field-error">
              {{ approvalErrors.endorserName }}
            </span>
          </div>
          <div class="end-field">
            <label for="endorserDesignation">Designation / Title <span class="end-req">*</span></label>
            <input
              id="endorserDesignation"
              v-model="approvalForm.endorserDesignation"
              class="end-input"
              type="text"
              placeholder="e.g. Dean"
              :disabled="busy || Boolean(blockedReason)"
            />
            <span
              v-if="showApprovalErrors && approvalErrors.endorserDesignation"
              class="end-field-error"
            >
              {{ approvalErrors.endorserDesignation }}
            </span>
          </div>
          <div class="end-field">
            <label for="endorsementDate">Date of Endorsement <span class="end-req">*</span></label>
            <input
              id="endorsementDate"
              v-model="approvalForm.endorsementDate"
              class="end-input"
              type="date"
              :disabled="busy || Boolean(blockedReason)"
            />
            <span v-if="showApprovalErrors && approvalErrors.endorsementDate" class="end-field-error">
              {{ approvalErrors.endorsementDate }}
            </span>
          </div>
        </div>

        <div class="end-note">
          <strong>Electronic signature:</strong>
          <template v-if="hasSignature">
            your saved signature is used to sign the letter.
            <router-link class="end-banner-link" to="/manage-profile">Review it</router-link>
          </template>
          <template v-else>
            you have not saved a signature yet, so the letter cannot be issued.
            <router-link class="end-banner-link" to="/manage-profile">Save one</router-link>
          </template>
          <div class="end-muted">
            The letter is signed with the electronic signature image on your account. This
            is an image-based electronic signature, not a cryptographic digital signature.
          </div>
        </div>

        <div class="end-actions">
          <button
            class="end-btn is-approve"
            :disabled="busy || Boolean(blockedReason)"
            @click="confirmApprove"
          >
            {{ busy ? 'Working…' : 'Approve and Issue Endorsement' }}
          </button>
        </div>
      </div>

      <!-- REJECT -->
      <div v-if="isPending(request.status)" class="end-card">
        <h3 class="end-section-title">Reject the request</h3>
        <div class="end-field">
          <label for="rejectionReason">Reason for rejection <span class="end-req">*</span></label>
          <textarea
            id="rejectionReason"
            v-model="rejectionForm.rejectionReason"
            class="end-textarea"
            rows="3"
            placeholder="Explain what the Proponent has to address."
            :disabled="busy"
          ></textarea>
          <span v-if="showRejectionErrors && rejectionErrors.rejectionReason" class="end-field-error">
            {{ rejectionErrors.rejectionReason }}
          </span>
        </div>
        <div class="end-actions">
          <button class="end-btn is-danger" :disabled="busy" @click="confirmReject">
            Reject Request
          </button>
        </div>
      </div>

      <!-- REISSUE -->
      <div v-if="isApproved(request.status) && request.hasLetter" class="end-card">
        <h3 class="end-section-title">Reissue the letter</h3>
        <p class="end-value">
          A correction is issued as a new version: the current letter is preserved and
          stays downloadable, and the new version replaces it in the Proponent's
          Certifications. The official record is never silently overwritten.
        </p>
        <div class="end-actions">
          <button class="end-btn" :disabled="busy" @click="confirmReissue">Reissue Letter</button>
        </div>
      </div>

      <!-- AUDIT -->
      <div class="end-card">
        <h3 class="end-section-title">Audit history</h3>
        <ul v-if="events.length" class="end-timeline">
          <li v-for="(event, index) in events" :key="index">
            <span class="end-timeline-title">{{ eventLabel(event.eventType) }}</span>
            {{ event.detail }}
            <div class="end-muted">
              {{ formatEndorsementTimestamp(event.createdAt) }}
              <template v-if="event.actorName"> · {{ event.actorName }}</template>
              <template v-if="event.actorRole"> · {{ event.actorRole }}</template>
            </div>
          </li>
        </ul>
        <p v-else class="end-muted">No history recorded yet.</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import '@/assets/endorsement.css'
import { useDialog } from '@/composables/useDialog'
import { useUserDataStore } from '@/stores/userData'
import { loadAccountSignatures } from '@/utils/proposalSignature.js'
import {
  approveBlockedReason,
  approveEndorsementRequest,
  buildApprovalPayload,
  buildRejectionPayload,
  endorsementStatusDescription,
  endorsementStatusLabel,
  endorsementStatusTone,
  fetchEndorsementDocument,
  fetchEndorsementEvents,
  fetchEndorsementRequest,
  formatEndorsementDate,
  formatEndorsementTimestamp,
  fundingProgramLabel,
  isApproved,
  isPending,
  isRejected,
  openDocumentBlob,
  rejectEndorsementRequest,
  reissueEndorsementLetter,
  saveDocumentBlob,
  suggestedLetterFileName,
  todayISO,
  validateApproval,
  validateRejection,
} from '@/utils/endorsement.js'

const route = useRoute()
const userStore = useUserDataStore()
const { showConfirm } = useDialog()

const request = ref(null)
const events = ref([])
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const actionMessage = ref('')
const actionError = ref('')
const hasSignature = ref(false)

const showApprovalErrors = ref(false)
const showRejectionErrors = ref(false)

const approvalForm = reactive({
  endorserName: userStore.user?.name || '',
  endorserDesignation: userStore.user?.designation || 'Dean',
  endorsementDate: todayISO(),
})

const rejectionForm = reactive({ rejectionReason: '' })

const approvalErrors = computed(() => validateApproval(approvalForm))
const rejectionErrors = computed(() => validateRejection(rejectionForm))

/**
 * Why the Approve action is unavailable, or '' when it is. The backend re-checks
 * every rule; this only explains the state on screen.
 */
const blockedReason = computed(() =>
  approveBlockedReason(request.value, { hasSignature: hasSignature.value }),
)

const EVENT_LABELS = {
  CREATED: 'Request created',
  ASSIGNED: 'Routed to a Dean',
  APPROVED: 'Approved',
  REJECTED: 'Rejected',
  LETTER_GENERATED: 'Endorsement letter generated',
  LETTER_REISSUED: 'Endorsement letter reissued',
  CERTIFICATION_CREATED: 'Filed in Certifications',
  PROPOSAL_LINKED: 'Linked to a proposal',
}

const eventLabel = (type) => EVENT_LABELS[type] || type

async function load() {
  loading.value = true
  error.value = ''
  try {
    request.value = await fetchEndorsementRequest(route.params.id)
    events.value = await fetchEndorsementEvents(route.params.id)
  } catch (err) {
    error.value =
      err?.response?.data?.error || 'This endorsement request could not be loaded.'
  } finally {
    loading.value = false
  }
}

/** The Dean's own saved signature, so the approval state is honest up front. */
async function loadSignatureState() {
  try {
    const signatures = await loadAccountSignatures()
    hasSignature.value = signatures.length > 0
  } catch {
    hasSignature.value = false
  }
}

async function approve() {
  showApprovalErrors.value = true
  if (Object.keys(approvalErrors.value).length > 0) return

  busy.value = true
  actionMessage.value = ''
  actionError.value = ''
  try {
    request.value = await approveEndorsementRequest(
      request.value.id,
      buildApprovalPayload(approvalForm),
    )
    events.value = await fetchEndorsementEvents(request.value.id)
    actionMessage.value =
      'The request was approved and the official endorsement letter was issued. It is now in the Proponent’s Certifications.'
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The request could not be approved.'
  } finally {
    busy.value = false
  }
}

async function reject() {
  showRejectionErrors.value = true
  if (Object.keys(rejectionErrors.value).length > 0) return

  busy.value = true
  actionMessage.value = ''
  actionError.value = ''
  try {
    request.value = await rejectEndorsementRequest(
      request.value.id,
      buildRejectionPayload(rejectionForm),
    )
    events.value = await fetchEndorsementEvents(request.value.id)
    actionMessage.value =
      'The request was rejected and the reason was recorded for the Proponent.'
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The request could not be rejected.'
  } finally {
    busy.value = false
  }
}

async function reissue() {
  busy.value = true
  actionMessage.value = ''
  actionError.value = ''
  try {
    request.value = await reissueEndorsementLetter(
      request.value.id,
      buildApprovalPayload(approvalForm),
    )
    events.value = await fetchEndorsementEvents(request.value.id)
    actionMessage.value = `The letter was reissued as version ${request.value.letterVersion}.`
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The letter could not be reissued.'
  } finally {
    busy.value = false
  }
}

/**
 * Confirmation dialogs. Approving permanently issues an official letter and
 * rejecting closes the request, so both are confirmed first.
 */
async function confirmApprove() {
  const confirmed = await showConfirm(
    'Approving issues the official MSU-Naawan endorsement letter, signed with your saved electronic signature, and files it in the Proponent’s Certifications. A correction afterwards requires an explicit reissue.',
    { title: 'Approve and issue the endorsement letter?', confirmText: 'Approve and Issue' },
  )
  if (confirmed) await approve()
}

async function confirmReject() {
  showRejectionErrors.value = true
  if (Object.keys(rejectionErrors.value).length > 0) return

  const confirmed = await showConfirm(
    'Rejecting closes this request and the Proponent sees the reason you recorded. No endorsement letter is issued.',
    { title: 'Reject this request?', confirmText: 'Reject Request', type: 'danger' },
  )
  if (confirmed) await reject()
}

async function confirmReissue() {
  const confirmed = await showConfirm(
    'A new version of the letter is issued. The current version is preserved and stays downloadable; the new one replaces it in the Proponent’s Certifications.',
    { title: 'Reissue the endorsement letter?', confirmText: 'Reissue' },
  )
  if (confirmed) await reissue()
}

/** The issued letter: the same endpoint serves the preview and the download. */
async function previewLetter() {
  busy.value = true
  actionError.value = ''
  try {
    const blob = await fetchEndorsementDocument(request.value.id)
    openDocumentBlob(blob, request.value.letterFileName)
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The letter could not be opened.'
  } finally {
    busy.value = false
  }
}

async function downloadLetter() {
  busy.value = true
  actionError.value = ''
  try {
    const blob = await fetchEndorsementDocument(request.value.id, { download: true })
    saveDocumentBlob(blob, suggestedLetterFileName(request.value.referenceNumber))
  } catch (err) {
    actionError.value = err?.response?.data?.error || 'The letter could not be downloaded.'
  } finally {
    busy.value = false
  }
}

onMounted(async () => {
  await Promise.all([load(), loadSignatureState()])
})
</script>
