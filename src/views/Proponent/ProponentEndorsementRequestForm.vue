<template>
  <!-- The "Request Endorsement" section of the consolidated My Endorsements page. It is a
       section, not a page of its own: the page provides the heading, the tabs and the
       confirmation message, while this component owns the form and its validation. -->
  <div>
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

        <!-- Program Title and Project Title are two independent fields, laid out exactly
             like the Create Proposal form: the Program Title is the dropdown on the left
             (the programme the project belongs to - optional, an existing title may be
             reused or a new one created) and the Project Title is the required text field
             on the right (the title of the project itself). Neither field ever fills the
             other. -->
        <div class="end-row">
          <div class="end-field end-field--program-title">
            <label>Program Title</label>
            <ProgramTitleSelect
              v-model="form.programTitleChoice"
              :titles="existingProgramTitles"
              :loading="programTitlesLoading"
              @change="onProgramTitleChoiceChange"
              @create="startProgramTitleCreation"
            />
            <div v-if="form.programTitleChoice === CREATE_NEW_PROGRAM_TITLE"
              class="end-program-title-new-row">
              <input
                ref="newProgramTitleInput"
                v-model="form.programTitle"
                class="end-input end-program-title-new-input"
                type="text"
                :maxlength="MAX_PROGRAM_TITLE_LENGTH"
                placeholder="e.g. Community Development Program"
                :disabled="submitting"
              />
              <button type="button" class="end-program-title-new-cancel" :disabled="submitting"
                @click="cancelProgramTitleCreation">
                Cancel
              </button>
            </div>
            <span v-if="showValidation && errors.programTitle" class="end-field-error">
              {{ errors.programTitle }}
            </span>
            <span v-else-if="programTitlesError" class="end-field-hint is-error">
              {{ programTitlesError }}
            </span>
            <span v-else-if="programTitlesEmpty" class="end-field-hint">
              No previously submitted program titles yet — choose "Create New Program Title" to add
              one.
            </span>
          </div>

          <div class="end-field">
            <label for="projectTitle">Project Title <span class="end-req">*</span></label>
            <input
              id="projectTitle"
              v-model="form.projectTitle"
              class="end-input"
              type="text"
              :maxlength="MAX_PROJECT_TITLE_LENGTH"
              placeholder="e.g. Seaweed-Based Biostimulant for Mangrove Rehabilitation"
              :disabled="submitting"
            />
            <span v-if="showValidation && errors.projectTitle" class="end-field-error">
              {{ errors.projectTitle }}
            </span>
          </div>
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
          <!-- Cancelling leaves the section: the page switches back to the request list. -->
          <button class="end-btn" type="button" :disabled="submitting" @click="$emit('cancel')">
            Cancel
          </button>
          <button class="end-btn is-primary" type="submit" :disabled="submitting">
            {{ submitting ? 'Submitting…' : 'Submit Request for Endorsement' }}
          </button>
        </div>
      </form>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import api from '@/utils/api'
import '@/assets/endorsement.css'
import ProgramTitleSelect from '@/components/ProgramTitleSelect.vue'
import { RPIS_COLLEGES } from '@/utils/endorsementColleges.js'
import {
  MAX_PROGRAM_TITLE_LENGTH,
  CREATE_NEW_PROGRAM_TITLE,
  programTitlesFromProposals,
} from '@/utils/programTitleOptions.js'
import {
  MAX_PROJECT_TITLE_LENGTH,
  buildEndorsementRequestPayload,
  createEndorsementRequest,
  fundingProgramLabel,
  validateEndorsementRequest,
} from '@/utils/endorsement.js'

/**
 * `submitted` hands the created request to the page, which shows the confirmation and
 * switches to the request list; `cancel` asks the page to go back to that list. The form
 * never navigates on its own: it is one section of the page, so the page decides where
 * the Proponent lands next.
 */
const emit = defineEmits(['submitted', 'cancel'])

const colleges = RPIS_COLLEGES
const calls = ref([])
const loading = ref(true)
const loadError = ref('')
const submitting = ref(false)
const showValidation = ref(false)
const errorMessage = ref('')

const form = reactive({
  // The control's own value: '' (nothing chosen), the "create a new program title"
  // sentinel, or one of the proponent's existing titles. It is never submitted.
  programTitleChoice: '',
  // What is submitted as the Program Title: '' or a real title. The sentinel can never
  // reach this value, so a control value can never be stored as a title.
  programTitle: '',
  // The required title of the project itself - a field of its own, never filled from the
  // program title.
  projectTitle: '',
  proposalAnnouncementId: '',
  college: '',
  department: '',
  researchSummary: '',
  notes: '',
})

// ===== PROGRAM TITLE (optional: reuse an existing title or create a new one) =====
// The choices, the sentinel and the click rule live in utils/programTitleOptions.js and
// the control that renders them is components/ProgramTitleSelect.vue - the same pair the
// Create Proposal form uses, so the two forms behave identically. The titles offered are
// the proponent's own previously used program titles, read from the same source.
const existingProgramTitles = ref([])
const programTitlesLoading = ref(false)
const programTitlesError = ref('')
const newProgramTitleInput = ref(null)
// What the field held before the inline creation started, so cancelling restores it.
const programTitleBeforeCreation = ref(null)

// True when the request succeeded but the proponent simply has no reusable titles yet.
// This is an empty state, not an error.
const programTitlesEmpty = computed(
  () =>
    !programTitlesLoading.value &&
    !programTitlesError.value &&
    existingProgramTitles.value.length === 0,
)

/**
 * Loads the distinct program titles the proponent may reuse. The same endpoint and the
 * same extraction rule the Create Proposal form uses (`/api/proposals` already returns
 * only the caller's own proposals), so a title created in one form is offered by the
 * other. Failures are surfaced inline and never block the form: the proponent can always
 * fall back to "Create New Program Title".
 */
async function fetchExistingProgramTitles() {
  programTitlesLoading.value = true
  programTitlesError.value = ''
  try {
    const response = await api.get('/api/proposals')
    existingProgramTitles.value = programTitlesFromProposals(response.data)
  } catch (error) {
    // A genuine API failure must never be reported as "no titles exist".
    console.error(
      'Failed to load existing program titles:',
      error?.response?.status,
      error?.response?.data || error?.message,
    )
    existingProgramTitles.value = []
    programTitlesError.value =
      'Unable to load existing program titles. You can still create a new one.'
  } finally {
    programTitlesLoading.value = false
  }
}

// Keeps the submitted value (form.programTitle) in step with the control. Choosing
// "Create New Program Title" clears any previously selected existing title so a stale
// value can never be submitted; the proponent then types the new title below the field.
const onProgramTitleChoiceChange = () => {
  if (form.programTitleChoice === CREATE_NEW_PROGRAM_TITLE) {
    form.programTitle = ''
  } else {
    form.programTitle = form.programTitleChoice || ''
  }
}

/**
 * Opens the inline "new program title" flow (the input under the field). It is reached by
 * choosing the option, by double clicking the control, or from the keyboard. Opening it
 * twice is harmless: the flow is already showing, so the proponent keeps what they typed
 * and only the focus moves.
 */
const startProgramTitleCreation = () => {
  if (form.programTitleChoice === CREATE_NEW_PROGRAM_TITLE) {
    newProgramTitleInput.value?.focus()
    return
  }
  programTitleBeforeCreation.value = {
    choice: form.programTitleChoice,
    title: form.programTitle,
  }
  form.programTitleChoice = CREATE_NEW_PROGRAM_TITLE
  onProgramTitleChoiceChange()
  nextTick(() => newProgramTitleInput.value?.focus())
}

/** Cancels the creation: the field goes back to what it held, nothing else changes. */
const cancelProgramTitleCreation = () => {
  form.programTitleChoice = programTitleBeforeCreation.value?.choice ?? ''
  form.programTitle = programTitleBeforeCreation.value?.title ?? ''
  programTitleBeforeCreation.value = null
}

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

  if (Object.keys(errors.value).length > 0) {
    return
  }

  submitting.value = true
  try {
    const created = await createEndorsementRequest(buildEndorsementRequestPayload(form))
    // Both titles are cleared as the two independent fields they are.
    form.programTitleChoice = ''
    form.programTitle = ''
    form.projectTitle = ''
    programTitleBeforeCreation.value = null
    form.proposalAnnouncementId = ''
    form.college = ''
    form.department = ''
    form.researchSummary = ''
    form.notes = ''
    showValidation.value = false
    // The page shows the confirmation and switches to the request list, which it reloads
    // so the new request is already in it.
    emit('submitted', created)
  } catch (error) {
    errorMessage.value =
      error?.response?.data?.error ||
      'The request could not be submitted. Please check the details and try again.'
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCalls()
  // The Program Title choices come from the same source the Create Proposal form reads.
  fetchExistingProgramTitles()
})
</script>
