<template>
  <div class="end-page end-page--fluid">
    <div class="end-page-header">
      <h2>Endorsement Request</h2>
      <p class="end-subtitle">
        {{ request ? request.referenceNumber : '' }} —
        {{ request ? endorsementStatusDescription(request.status) : 'Loading…' }}
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading the request…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <router-link class="end-btn" to="/endorsements">Back to my requests</router-link>
      </div>
    </div>

    <template v-else-if="request">
      <div v-if="actionMessage" class="end-banner is-info">
        <span>{{ actionMessage }}</span>
      </div>
      <div v-if="actionError" class="end-banner is-error">
        <span>{{ actionError }}</span>
      </div>

      <div class="end-card">
        <div class="end-toolbar">
          <span class="end-badge" :class="endorsementStatusTone(request.status)">
            {{ endorsementStatusLabel(request.status) }}
          </span>
          <span class="end-muted">
            Submitted {{ formatEndorsementTimestamp(request.createdAt) }}
          </span>
        </div>

        <div class="end-grid">
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
            <span class="end-label">Assigned Dean</span>
            <span class="end-value">{{ request.assignedDeanName || 'Not assigned yet' }}</span>
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

      <!-- DECISION -->
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
          <div class="end-field">
            <span class="end-label">Decided at</span>
            <span class="end-value">{{ formatEndorsementTimestamp(request.decidedAt) || '—' }}</span>
          </div>
          <div v-if="request.letterDocumentCode" class="end-field">
            <span class="end-label">Document code</span>
            <span class="end-value">{{ request.letterDocumentCode }}</span>
          </div>
        </div>

        <div v-if="isRejected(request.status)" class="end-note is-warning">
          <strong>Reason for rejection:</strong> {{ request.rejectionReason }}
        </div>
      </div>

      <!-- ISSUED LETTER -->
      <div v-if="request.hasLetter" class="end-card">
        <h3 class="end-section-title">Official endorsement letter</h3>
        <p class="end-value">
          {{ request.letterFileName || 'Endorsement letter' }}
          <span v-if="request.letterVersion" class="end-muted">
            · version {{ request.letterVersion }}
          </span>
        </p>
        <div class="end-actions">
          <button class="end-btn" :disabled="busy" @click="previewLetter">View</button>
          <button class="end-btn is-primary" :disabled="busy" @click="downloadLetter">
            Download
          </button>
          <router-link class="end-btn" to="/endorsements?tab=certifications">
            Open Issued Certifications
          </router-link>
        </div>
      </div>

      <!-- CONTINUE -->
      <div v-if="isApproved(request.status)" class="end-card">
        <h3 class="end-section-title">Continue to proposal submission</h3>
        <p class="end-value">
          Your endorsement is approved. Continue to the existing proposal form: your
          title, call and college are carried over and the proposal will be linked to
          this endorsement. The full proposal still has to be completed and submitted.
        </p>
        <div class="end-actions">
          <button class="end-btn is-primary" @click="continueToProposal">
            Continue to Proposal Submission
          </button>
        </div>
      </div>

      <!-- TIMELINE -->
      <div class="end-card">
        <h3 class="end-section-title">History</h3>
        <ul v-if="events.length" class="end-timeline">
          <li v-for="(event, index) in events" :key="index">
            <span class="end-timeline-title">{{ eventLabel(event.eventType) }}</span>
            {{ event.detail }}
            <div class="end-muted">
              {{ formatEndorsementTimestamp(event.createdAt) }}
              <template v-if="event.actorName"> · {{ event.actorName }}</template>
            </div>
          </li>
        </ul>
        <p v-else class="end-muted">No history recorded yet.</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@/assets/endorsement.css'
import {
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
  saveDocumentBlob,
  suggestedLetterFileName,
} from '@/utils/endorsement.js'

const route = useRoute()
const router = useRouter()

const request = ref(null)
const events = ref([])
const loading = ref(true)
const error = ref('')
const busy = ref(false)
const actionMessage = ref('')
const actionError = ref('')

/** Labels of the audit events, so the history reads as sentences. */
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

async function withLetter(action, message) {
  busy.value = true
  actionMessage.value = ''
  actionError.value = ''
  try {
    await action()
    if (message) actionMessage.value = message
  } catch (err) {
    actionError.value =
      err?.response?.data?.error || 'The endorsement letter could not be retrieved.'
  } finally {
    busy.value = false
  }
}

/** The letter is served by the same endpoint for the preview and the download. */
function previewLetter() {
  return withLetter(async () => {
    const blob = await fetchEndorsementDocument(request.value.id)
    openDocumentBlob(blob, request.value.letterFileName)
  })
}

function downloadLetter() {
  return withLetter(async () => {
    const blob = await fetchEndorsementDocument(request.value.id, { download: true })
    saveDocumentBlob(blob, suggestedLetterFileName(request.value.referenceNumber))
  })
}

/** Hands over to the existing proposal form, carrying the endorsement reference. */
function continueToProposal() {
  router.push({ path: '/proposals', query: { endorsement: request.value.id } })
}

onMounted(load)
</script>
