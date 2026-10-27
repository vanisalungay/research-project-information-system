<template>
  <div class="end-page">
    <div class="end-page-header">
      <h2>Request for Endorsement</h2>
      <p class="end-subtitle">
        Ask your College Dean / Department Chair to endorse your project before you fill
        in the full proposal. Only the preliminary information below is needed at this
        stage.
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading the open Calls for Proposal…</p>
    </div>

    <div v-else-if="loadError" class="end-card is-error">
      <p>{{ loadError }}</p>
      <div class="end-actions">
        <button class="end-btn" @click="loadCalls">Retry</button>
      </div>
    </div>

    <template v-else>
      <div v-if="successMessage" class="end-banner is-success">
        <span>{{ successMessage }}</span>
        <router-link class="end-banner-link" to="/endorsements">View my requests</router-link>
      </div>

      <div v-if="calls.length === 0" class="end-card is-empty">
        <p>
          There are currently no open Calls for Proposal. An endorsement request can only
          be raised for a call that is open for submissions.
        </p>
        <div class="end-actions">
          <router-link class="end-btn" to="/proposals">Back to My Projects</router-link>
        </div>
      </div>

      <form v-else class="end-card" @submit.prevent="submit">
        <div v-if="errorMessage" class="end-banner is-error">
          <span>{{ errorMessage }}</span>
        </div>

        <div class="end-field">
          <label for="proposalTitle">Proposal / Program Title <span class="end-req">*</span></label>
          <input
            id="proposalTitle"
            v-model="form.proposalTitle"
            class="end-input"
            type="text"
            maxlength="500"
            placeholder="e.g. Seaweed-Based Biostimulant for Mangrove Rehabilitation"
            :disabled="submitting"
          />
          <span v-if="showValidation && errors.proposalTitle" class="end-field-error">
            {{ errors.proposalTitle }}
          </span>
        </div>

        <div class="end-field">
          <label for="proposalAnnouncementId">Call for Proposal <span class="end-req">*</span></label>
          <select
            id="proposalAnnouncementId"
            v-model="form.proposalAnnouncementId"
            class="end-select"
            :disabled="submitting"
          >
            <option value="">Select the call being applied to</option>
            <option v-for="call in calls" :key="call.id" :value="call.id">
              {{ call.name }} (deadline {{ call.endDate }})
            </option>
          </select>
          <span v-if="showValidation && errors.proposalAnnouncementId" class="end-field-error">
            {{ errors.proposalAnnouncementId }}
          </span>
        </div>

        <div v-if="selectedCall" class="end-note" :class="{ 'is-warning': !templateAvailable }">
          <strong>Funding program:</strong> {{ selectedProgramLabel }}
          <span v-if="!templateAvailable">
            — no official endorsement letter is configured for this program yet, so it
            cannot be endorsed at this time.
          </span>
        </div>

        <div class="end-field">
          <label for="college">Implementing College / Department <span class="end-req">*</span></label>
          <select id="college" v-model="form.college" class="end-select" :disabled="submitting">
            <option value="">Select College / Department</option>
            <option v-for="college in colleges" :key="college" :value="college">
              {{ college }}
            </option>
          </select>
          <span v-if="showValidation && errors.college" class="end-field-error">
            {{ errors.college }}
          </span>
        </div>

        <div class="end-field">
          <label for="department">Department / Unit (optional)</label>
          <input
            id="department"
            v-model="form.department"
            class="end-input"
            type="text"
            placeholder="e.g. Department of Marine Biology"
            :disabled="submitting"
          />
        </div>

        <div class="end-field">
          <label for="researchSummary">Brief Research Description (optional)</label>
          <textarea
            id="researchSummary"
            v-model="form.researchSummary"
            class="end-textarea"
            rows="4"
            placeholder="A short summary of what the project will do."
            :disabled="submitting"
          ></textarea>
        </div>

        <div class="end-field">
          <label for="notes">Notes for the Dean (optional)</label>
          <textarea
            id="notes"
            v-model="form.notes"
            class="end-textarea"
            rows="3"
            placeholder="Anything the Dean should know before reviewing the request."
            :disabled="submitting"
          ></textarea>
        </div>

        <div class="end-actions">
          <router-link class="end-btn" to="/endorsements">Cancel</router-link>
          <button class="end-btn is-primary" type="submit" :disabled="submitting">
            {{ submitting ? 'Submitting…' : 'Submit Request for Endorsement' }}
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import api from '@/utils/api'
import '@/assets/endorsement.css'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'
import {
  buildEndorsementRequestPayload,
  createEndorsementRequest,
  fundingProgramLabel,
  validateEndorsementRequest,
} from '@/utils/endorsement.js'

const colleges = RPIS_COLLEGES
const calls = ref([])
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const showValidation = ref(false)
const errorMessage = ref('')
const successMessage = ref('')

const form = reactive({
  proposalTitle: '',
  proposalAnnouncementId: '',
  college: '',
  department: '',
  researchSummary: '',
  notes: '',
})

const errors = computed(() => validateEndorsementRequest(form))

const selectedCall = computed(
  () => calls.value.find((call) => String(call.id) === String(form.proposalAnnouncementId)) || null,
)

const selectedProgramLabel = computed(() =>
  fundingProgramLabel({
    fundingProgram: selectedCall.value?.fundingProgram,
    fundingProgramLabel: selectedCall.value?.fundingProgramLabel,
  }),
)

/**
 * Whether an official endorsement letter exists for the selected call's funding
 * program. Only the Internally Funded Project (IFP) letter is implemented, so a call
 * for another program is reported as not endorseable here instead of being silently
 * rendered with the IFP wording. The backend refuses it as well.
 */
const templateAvailable = computed(() => {
  const program = String(selectedCall.value?.fundingProgram ?? 'IFP').toUpperCase()
  return program === 'IFP' || program === ''
})

async function loadCalls() {
  loading.value = true
  loadError.value = ''
  try {
    // The same endpoint the Create Proposal workflow uses: only calls that are
    // published and inside their submission window are returned.
    const response = await api.get('/api/proposal-announcements/available')
    calls.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    loadError.value =
      error?.response?.data?.error ||
      'The open Calls for Proposal could not be loaded. Please try again.'
  } finally {
    loading.value = false
  }
}

async function submit() {
  showValidation.value = true
  errorMessage.value = ''
  successMessage.value = ''

  if (Object.keys(errors.value).length > 0) {
    return
  }

  submitting.value = true
  try {
    const created = await createEndorsementRequest(buildEndorsementRequestPayload(form))
    successMessage.value = `Request ${created.referenceNumber} was submitted and is now pending your Dean's endorsement.`
    form.proposalTitle = ''
    form.proposalAnnouncementId = ''
    form.college = ''
    form.department = ''
    form.researchSummary = ''
    form.notes = ''
    showValidation.value = false
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.error ||
      'The request could not be submitted. Please check the details and try again.'
  } finally {
    submitting.value = false
  }
}

onMounted(loadCalls)
</script>
