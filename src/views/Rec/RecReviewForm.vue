<template>
  <div class="review-wrapper">
    <!-- BREADCRUMB -->
    <p class="breadcrumb">
      Submitted Proposals / {{ proposal.title || 'Proposal' }} - 2024
      <span class="active">Review</span>
    </p>

    <!-- LOADING / ERROR STATES -->
    <p v-if="loadingProposal" class="page-state">Loading proposal details…</p>
    <p v-else-if="loadError" class="page-state page-state-error">{{ loadError }}</p>

    <template v-else>
      <!-- TITLE + DOWNLOAD BUTTON -->
      <div class="top-row">
        <h2 class="review-title">Proposal Info</h2>
        <button class="btn-view" @click="goToDetailed">VIEW</button>
      </div>

      <!-- PROPOSAL INFO -->
      <div class="info-grid">
        <div>
          <label>Title</label>
          <p>{{ proposal.title || '-' }}</p>
        </div>

        <div>
          <label>Proponent</label>
          <p>{{ proposal.proponent || '-' }}</p>
        </div>

        <div>
          <label>Department / Office</label>
          <p>{{ proposal.department || '-' }}</p>
        </div>

        <div>
          <label>Date Submitted</label>
          <p>{{ proposal.dateSubmitted || '-' }}</p>
        </div>

        <div>
          <label>Status</label>
          <p>{{ proposalStatusLabel(proposal.status) || '-' }}</p>
        </div>

        <div>
          <label>Research Category</label>
          <p>{{ proposal.researchCategory || '-' }}</p>
        </div>

        <div>
          <label>Funding Source</label>
          <p>{{ proposal.fundingSource || '-' }}</p>
        </div>

        <div>
          <label>Requested Budget</label>
          <p>
            {{ proposal.requestedBudget != null ? '₱' + proposal.requestedBudget.toLocaleString() : '-' }}
          </p>
        </div>
      </div>

      <!-- SCORING GUIDE -->
      <h3 class="section-title">Scoring Guide</h3>

      <div class="table-scroll">
        <table class="simple-table">
          <thead>
            <tr>
              <th>Rating</th>
              <th>Meaning</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>5</td>
              <td>Excellent</td>
            </tr>
            <tr>
              <td>4</td>
              <td>Very Good</td>
            </tr>
            <tr>
              <td>3</td>
              <td>Good</td>
            </tr>
            <tr>
              <td>2</td>
              <td>Fair</td>
            </tr>
            <tr>
              <td>1</td>
              <td>Poor</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- PART I -->
      <h3 class="section-title">I. Criteria for Evaluating Proposals</h3>

      <div class="table-scroll">
        <table class="criteria-table">
          <thead>
            <tr>
              <th>Criterion</th>
              <th>Definition</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>Relevance or Significance</td>
              <td>
                Aligned with the priorities of the OVCRIGE, the approach ensures strategic relevance to
                the university's objectives while being mindful of the political, cultural, and
                traditional context, as well as gender and development considerations.....
              </td>
            </tr>
            <tr>
              <td>Technical / Scientific Merit</td>
              <td>
                Sound scientific basis to generate new knowledge or apply existing knowledge in an
                innovative manner
              </td>
            </tr>
            <tr>
              <td>Budget Appropriateness</td>
              <td>The proposed budget is commensurate to the proposed work plan and deliverables. …</td>
            </tr>
            <tr>
              <td>Competence of Proponent</td>
              <td>
                Proponent’s expertise is relevant to the proposal and with proven competence to
                implement, manage and complete R&D programs/projects within the approved duration and
                budget. …
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- PART II -->
      <div class="evaluation-section">
        <h3 class="section-title">II. Evaluation Criteria</h3>
        <p class="evaluation-hint">
          Select one score from 1 (Poor) to 5 (Excellent) for each indicator. Scoring is optional.
        </p>

        <div v-for="group in SCORE_GROUPS" :key="group.key" class="score-group">
          <div class="score-group-head">
            <h4>{{ group.title }} ({{ group.weight }})</h4>
          </div>

          <div v-for="row in group.rows" :key="row.field" class="score-item">
            <p :id="`criterion-${row.field}`" class="score-label">{{ row.label }}</p>

            <div class="score-picker" role="radiogroup" :aria-labelledby="`criterion-${row.field}`">
              <label v-for="n in SCORE_VALUES" :key="n" class="score-option">
                <input
                  v-model="scores[row.field]"
                  class="score-radio"
                  type="radio"
                  :name="`score-${row.field}`"
                  :value="n"
                  :aria-label="`${n} of 5`"
                  :disabled="isForwarding"
                />
                <span class="score-circle">{{ n }}</span>
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- COMMENTS -->
      <label class="comment-label" for="overall-comments">Overall Comments</label>
      <textarea
        id="overall-comments"
        v-model="overallComments"
        class="comment-box"
        rows="5"
      ></textarea>

      <!-- DECISION SUBMISSION -->
      <h3 class="decision-title">Decision Submission</h3>

      <div class="decision-buttons">
        <button
          class="btn-approve"
          :disabled="!proposal.id || isForwarding || proposal.status === 'REC_APPROVED'"
          @click="openApproveModal"
        >
          Approve Proposal
        </button>
        <button class="btn-return" :disabled="!proposal.id || isForwarding" @click="goToReturn">
          Return for Revision
        </button>
        <button class="btn-reject" :disabled="!proposal.id || isForwarding" @click="goToReject">
          Reject Proposal
        </button>
      </div>
    </template>

    <!-- FORWARDING CONFIRMATION MODAL -->
    <Teleport to="body">
      <div
        v-if="showApproveModal"
        class="confirm-scrim"
        @click.self="closeApproveModal"
        @keydown.esc="closeApproveModal"
      >
        <div
          ref="approveSheet"
          class="confirm-sheet"
          role="dialog"
          aria-modal="true"
          aria-labelledby="approve-title"
          aria-describedby="approve-message"
          tabindex="-1"
        >
          <h3 id="approve-title" class="confirm-heading">Forward Proposal</h3>
          <p id="approve-message" class="confirm-message">
            Are you sure you want to forward this proposal?
          </p>

          <div class="confirm-actions">
            <button
              type="button"
              class="btn-cancel"
              :disabled="isForwarding"
              @click="closeApproveModal"
            >
              Cancel
            </button>
            <button
              type="button"
              class="btn-approve"
              :disabled="isForwarding"
              :aria-busy="isForwarding"
              @click="confirmApproval"
            >
              <span v-if="isForwarding" class="confirm-spinner" aria-hidden="true"></span>
              Approve
            </button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- SUCCESS MESSAGE -->
    <Teleport to="body">
      <div v-if="showSuccessMessage" class="confirm-scrim">
        <div
          class="confirm-sheet"
          role="dialog"
          aria-modal="true"
          aria-labelledby="success-title"
        >
          <h3 id="success-title" class="confirm-heading">Success</h3>
          <p class="confirm-message">The proposal has been forwarded successfully.</p>

          <div class="confirm-actions">
            <button type="button" class="btn-primary" @click="closeSuccessMessage">Okay</button>
          </div>
        </div>
      </div>
    </Teleport>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'
import { useUserDataStore } from '@/stores/userData'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

const { dialogState, showAlert } = useDialog()

const router = useRouter()
const route = useRoute()
const userStore = useUserDataStore()

/**
 * The 15 evaluation criteria, in the order the review form presents them. `field` is the exact
 * camelCase name of the matching column on `ProposalReview`, so the same definition drives both
 * the rendering and the request body.
 */
const SCORE_GROUPS = [
  {
    key: 'A',
    title: 'A. Soundness of Proposal',
    weight: '20%',
    rows: [
      {
        field: 'soundnessSectorNeedScore',
        label: 'R&D addresses relevant sectoral need (applicable to pressing concern)',
      },
      {
        field: 'soundnessEffectiveSolutionScore',
        label: 'Solution provided is most effective compared to other proposed solutions',
      },
      {
        field: 'soundnessReasonableBudgetScore',
        label: 'Proposed budget is reasonable (not expensive vis-a-vis output)',
      },
      {
        field: 'soundnessDoableWorkplanScore',
        label: 'Work plan is doable within the given timeframe',
      },
    ],
  },
  {
    key: 'B',
    title: 'B. Suitability of Output',
    weight: '30%',
    rows: [
      {
        field: 'suitabilityCostEffectiveScore',
        label:
          'R&D output is cost-effective (cost is competitive in relation to new or existing products or processes)',
      },
      {
        field: 'suitabilityPartnersIdentifiedScore',
        label:
          'Has identified partners to adopt the technology (with letter of support from the head of the company)',
      },
      {
        field: 'suitabilityCommercializationScore',
        label:
          'Output can be commercialized (through an existing manufacturer, spin-off, or start-up company)',
      },
      {
        field: 'suitabilityTimelyUtilizationScore',
        label:
          'R&D utilization is timely (output should not be overtaken by other solutions)',
      },
    ],
  },
  {
    key: 'C',
    title: 'C. Significance of Outcome',
    weight: '30%',
    rows: [
      {
        field: 'significanceEconomicImpactScore',
        label:
          'Economic impact: increase in productivity, increase in income, new jobs generated, high return of investment (ROI)',
      },
      {
        field: 'significanceSocialImpactScore',
        label:
          'Social: working partnerships established, training opportunities provided, policies adopted, increased access to basic services (i.e., food, health, education); political, cultural, gender sensitivity and inclusivity',
      },
      {
        field: 'significanceEnvironmentalImpactScore',
        label:
          'Environment: enhanced environmental health standards, no adverse effect to the environment',
      },
      {
        field: 'significanceSustainabilityScore',
        label:
          'Sustainability: sustainability mechanisms established in terms of institutional, financial and human resources capability (submission of a new proposal to sustain a completed or ongoing proposal does not constitute sustainability of the project)',
      },
    ],
  },
  {
    key: 'D',
    title: 'D. Competence of Proponent',
    weight: '20%',
    rows: [
      {
        field: 'competenceAlignmentScore',
        label: "Proponent's expertise aligned with the proposal",
      },
      {
        field: 'competenceCollaborationScore',
        label: 'Collaboration with relevant agencies and/or industry partners',
      },
      {
        field: 'competenceDeliverablesUnderstandingScore',
        label:
          "Thorough understanding of the proposal's deliverables 5 MSUN OVCRIGE has good experience with the proponent",
      },
    ],
  },
]

const SCORE_FIELDS = SCORE_GROUPS.flatMap((group) => group.rows.map((row) => row.field))
const SCORE_VALUES = [1, 2, 3, 4, 5]

// ==== Evaluation state ====
const scores = reactive(Object.fromEntries(SCORE_FIELDS.map((field) => [field, null])))
const overallComments = ref('')

// ==== Modal / request state ====
const showApproveModal = ref(false)
const showSuccessMessage = ref(false)
const isForwarding = ref(false)
const approveSheet = ref(null)
let redirectTimer = null

// ==== Proposal state ====
const proposal = ref({
  id: null,
  title: '',
  proponent: '',
  department: '',
  dateSubmitted: '',
  status: '',
  researchCategory: '',
  fundingSource: '',
  requestedBudget: null,
})
const loadingProposal = ref(false)
const loadError = ref('')

/**
 * Clears everything the evaluator entered. Called from the route watcher so switching between
 * proposals inside the reused component never carries a score (or a modal) across.
 */
const resetEvaluation = () => {
  for (const field of SCORE_FIELDS) scores[field] = null
  overallComments.value = ''
  showApproveModal.value = false
  showSuccessMessage.value = false
  isForwarding.value = false
}

/**
 * The id of the proposal under review: the params form the REVIEW button uses, or the query
 * form, so a refresh, a direct link and the browser's Back/Forward all resolve it the same
 * way instead of depending on how the page happened to be opened.
 */
const routeProposalId = () => route.params.id ?? route.query.id ?? null

/**
 * Reads the proposal. A route without an id and a proposal that is not there are both said
 * out loud, instead of leaving the page as an empty shell with no explanation.
 */
const loadProposal = async () => {
  const proposalId = routeProposalId()
  loadError.value = ''
  if (!proposalId) {
    loadError.value = 'No proposal was selected. Open one from the proposal list.'
    loadingProposal.value = false
    return
  }

  loadingProposal.value = true
  try {
    const res = await api.get(`/api/proposals/${proposalId}`)
    const data = res.data || null
    if (!data || data.id === null || data.id === undefined) {
      loadError.value = 'This proposal could not be found.'
      return
    }
    proposal.value = {
      id: data.id,
      title: data.projectTitle || data.title || '',
      proponent: data.projectLeader || data.proponent?.name || '',
      department: data.college || data.proponent?.departmentOffice || '',
      dateSubmitted:
        data.submittedByDate || (data.createdAt ? String(data.createdAt).slice(0, 10) : ''),
      status: data.status || '',
      researchCategory: data.researchType || '',
      fundingSource: '',
      requestedBudget: null,
    }
  } catch (err) {
    console.error(err)
    loadError.value = 'Failed to load proposal details.'
  } finally {
    loadingProposal.value = false
  }
}

// The route drives the load, so another proposal, Back/Forward, a refresh and a direct link
// all reload - nothing is kept in temporary component state.
watch(
  routeProposalId,
  () => {
    resetEvaluation()
    loadProposal()
  },
  { immediate: true },
)

// Move focus into the confirmation sheet so Escape and keyboard users reach it immediately.
watch(showApproveModal, async (open) => {
  if (!open) return
  await nextTick()
  approveSheet.value?.focus()
})

const openApproveModal = () => {
  if (isForwarding.value || !proposal.value.id || proposal.value.status === 'REC_APPROVED') return
  showApproveModal.value = true
}

const closeApproveModal = () => {
  if (isForwarding.value) return
  showApproveModal.value = false
}

/**
 * Reads the signed-in reviewer's id from the store, rehydrating once from localStorage if the
 * store is empty (a direct mount or a cleared session).
 */
const resolveReviewerId = () => {
  if (userStore.user?.id == null) {
    try {
      userStore.hydrate()
    } catch (err) {
      console.error(err)
    }
  }
  return userStore.user?.id ?? null
}

/** Keeps every submitted score an integer in 1-5 (or null when the criterion is left blank). */
const normalizeScore = (value) => {
  const number = Number(value)
  return Number.isInteger(number) && number >= 1 && number <= 5 ? number : null
}

/** The review body: the 15 scores, the overall comments, and the REC decision. */
const buildReviewPayload = () => {
  const payload = {}
  for (const field of SCORE_FIELDS) payload[field] = normalizeScore(scores[field])
  return {
    ...payload,
    overallComments: overallComments.value.trim() || null,
    decision: 'APPROVED',
  }
}

const confirmApproval = async () => {
  // Set synchronously: a second click (or a double-tap) returns before a second request starts.
  if (isForwarding.value) return

  const proposalId = proposal.value.id
  if (proposalId == null) {
    await showAlert('No proposal was selected. Open one from the proposal list.', {
      type: 'error',
      title: 'Cannot Forward',
    })
    return
  }

  const reviewerId = resolveReviewerId()
  if (reviewerId == null) {
    await showAlert('Your reviewer account could not be identified. Please sign in again.', {
      type: 'error',
      title: 'Cannot Forward',
    })
    return
  }

  isForwarding.value = true
  try {
    await api.post(`/api/proposals/${proposalId}/reviews`, buildReviewPayload(), {
      params: { reviewerId },
    })

    showApproveModal.value = false
    proposal.value.status = 'REC_APPROVED'
    showSuccessMessage.value = true

    redirectTimer = setTimeout(() => {
      router.push('/rec-dash')
    }, 1500)
  } catch (err) {
    console.error(err)
    const body = err?.response?.data
    const serverMessage =
      typeof body === 'string' ? body : typeof body?.message === 'string' ? body.message : ''
    await showAlert(
      serverMessage.trim() || 'Failed to forward the proposal. Please try again.',
      { type: 'error', title: 'Forwarding Failed' },
    )
  } finally {
    isForwarding.value = false
  }
}

const goToReturn = () => {
  router.push({ name: 'RecReturn4RevisionWithId', params: { id: proposal.value.id } })
}

const goToReject = () => {
  router.push({ name: 'RecRejectProposalWithId', params: { id: proposal.value.id } })
}

const goToDetailed = () => {
  const proposalId = proposal.value.id || route.params.id || route.query.id
  router.push({ path: '/rec-prop', query: { id: proposalId } })
}

const closeSuccessMessage = () => {
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
  showSuccessMessage.value = false
  router.push('/rec-dash')
}

onBeforeUnmount(() => {
  if (redirectTimer) {
    clearTimeout(redirectTimer)
    redirectTimer = null
  }
})
</script>

<style scoped>
.review-wrapper {
  padding: var(--space-5, 20px) clamp(14px, 3vw, 25px);
}

/* Breadcrumb */
.breadcrumb {
  font-size: var(--fs-sm, 14px);
  color: var(--color-text-soft, #64748b);
  margin-bottom: var(--space-3, 12px);
  overflow-wrap: anywhere;
}

.breadcrumb .active {
  font-weight: 600;
  color: var(--color-primary, #2452ff);
}

/* Title + button */
.top-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3, 12px);
  justify-content: space-between;
  align-items: center;
}

.review-title {
  font-size: var(--fs-xl, 24px);
  margin: 0;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: var(--space-4, 16px);
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #ddd);
  padding: var(--space-5, 20px);
  border-radius: var(--radius-lg, 12px);
  margin: var(--space-5, 20px) 0;
}

.info-grid > div {
  min-width: 0;
}

.info-grid label {
  display: block;
  margin-bottom: var(--space-1, 4px);
  font-weight: 600;
  font-size: var(--fs-xs, 12px);
  text-transform: uppercase;
  letter-spacing: 0.03em;
  color: var(--color-text-soft, #64748b);
}

.info-grid p {
  margin: 0;
  font-size: var(--fs-sm, 14px);
  overflow-wrap: anywhere;
}

/* Section title */
.section-title {
  font-size: var(--fs-lg, 18px);
  margin: var(--space-5, 20px) 0 var(--space-3, 12px);
  font-weight: 600;
}

/* Tables */
.table-scroll {
  width: 100%;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  margin-bottom: var(--space-5, 20px);
}

.simple-table,
.criteria-table {
  width: 100%;
  border-collapse: collapse;
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #aaa);
}

.simple-table {
  min-width: 280px;
}

.criteria-table {
  min-width: 560px;
}

.simple-table th,
.simple-table td,
.criteria-table th,
.criteria-table td {
  border: 1px solid var(--color-border, #ccc);
  padding: 10px 12px;
  font-size: var(--fs-sm, 14px);
  vertical-align: top;
}

/* Evaluation */
.evaluation-section {
  margin-top: var(--space-5, 20px);
}

.evaluation-hint {
  margin: 0 0 var(--space-3, 12px);
  font-size: var(--fs-sm, 14px);
  color: var(--color-text-soft, #64748b);
}

.score-group {
  margin-bottom: var(--space-4, 16px);
  border: 1px solid var(--color-border, #dbe4ee);
  border-radius: var(--radius-lg, 12px);
  overflow: hidden;
  background: var(--color-card-background, #ffffff);
}

.score-group-head {
  padding: var(--space-3, 12px) var(--space-4, 16px);
  background: var(--color-background-mute, #f2f2f2);
  border-bottom: 1px solid var(--color-border, #dbe4ee);
}

.score-group-head h4 {
  margin: 0;
  font-size: var(--fs-sm, 14px);
  font-weight: 700;
}

.score-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-4, 16px);
  padding: var(--space-3, 12px) var(--space-4, 16px);
  border-top: 1px solid var(--color-border, #dbe4ee);
}

.score-item:first-of-type {
  border-top: 0;
}

.score-label {
  margin: 0;
  font-size: var(--fs-sm, 14px);
  line-height: 1.45;
  overflow-wrap: anywhere;
}

/* Circular score selector */
.score-picker {
  display: flex;
  align-items: center;
  gap: var(--space-2, 8px);
}

.score-option {
  position: relative;
  display: inline-flex;
  flex: 0 0 auto;
  cursor: pointer;
}

.score-radio {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  margin: 0;
  opacity: 0;
  appearance: none;
  cursor: pointer;
}

.score-circle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: 1.5px solid var(--color-border, #cbd5e1);
  border-radius: 50%;
  background: var(--color-card-background, #ffffff);
  color: var(--color-text, #1e293b);
  font-size: var(--fs-sm, 14px);
  font-weight: 600;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease;
}

.score-radio:not(:disabled):hover + .score-circle {
  border-color: var(--color-primary, #2452ff);
  color: var(--color-primary, #2452ff);
}

.score-radio:focus-visible + .score-circle {
  outline: 2px solid var(--color-primary, #2452ff);
  outline-offset: 2px;
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.18);
}

.score-radio:checked + .score-circle {
  background: var(--color-primary, #2452ff);
  border-color: var(--color-primary, #2452ff);
  /* Wins over the global dark-mode `span { color: ... !important }` rule. */
  color: #ffffff !important;
}

.score-radio:disabled + .score-circle {
  opacity: 0.55;
  cursor: not-allowed;
}

/* Comments */
.comment-label {
  display: block;
  font-weight: 600;
  margin-bottom: var(--space-1, 4px);
}

.comment-box {
  width: 100%;
  border: 1px solid var(--color-border, #aaa);
  border-radius: var(--radius-md, 8px);
  padding: 12px;
  margin-top: 5px;
  resize: vertical;
  font-family: inherit;
  font-size: var(--fs-sm, 14px);
  color: var(--color-text, #1e293b);
  background: var(--color-card-background, #ffffff);
}

/* Decision Submission */
.decision-title {
  margin: var(--space-6, 24px) 0 var(--space-3, 12px);
  font-size: var(--fs-lg, 18px);
  font-weight: 600;
}

.decision-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3, 12px);
  margin-bottom: var(--space-7, 32px);
}

.decision-buttons > button {
  white-space: nowrap;
}

/* Loading / error states */
.page-state {
  padding: var(--space-7, 32px) var(--space-4, 16px);
  text-align: center;
  color: var(--color-text-soft, #64748b);
}

.page-state-error {
  color: var(--color-danger, #dc2626);
}

/* Confirmation / success modal */
.confirm-scrim {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-4, 16px);
  background: rgba(15, 23, 42, 0.55);
  backdrop-filter: blur(3px);
}

.confirm-sheet {
  width: min(420px, 100%);
  max-height: calc(100vh - 32px);
  overflow-y: auto;
  padding: var(--space-6, 24px);
  border: 1px solid var(--color-border, #e2e8f0);
  border-radius: var(--radius-xl, 14px);
  background: var(--color-card-background, #ffffff);
  color: var(--color-text, #1e293b);
  text-align: center;
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.18);
}

.confirm-heading {
  margin: 0 0 var(--space-2, 8px);
  font-size: var(--fs-lg, 18px);
}

.confirm-message {
  margin: 0 0 var(--space-5, 20px);
  font-size: var(--fs-sm, 14px);
  color: var(--color-text-soft, #64748b);
}

.confirm-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-3, 12px);
  flex-wrap: wrap;
}

.confirm-spinner {
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255, 255, 255, 0.4);
  border-top-color: currentColor;
  border-radius: 50%;
  animation: confirm-spin 0.8s linear infinite;
}

@keyframes confirm-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1024px) {
  .info-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .score-item {
    grid-template-columns: minmax(0, 1fr);
    align-items: start;
    gap: var(--space-2, 8px);
  }

  .score-picker {
    justify-content: flex-start;
  }
}

@media (max-width: 640px) {
  .review-wrapper {
    padding: 14px;
  }

  .top-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .info-grid {
    grid-template-columns: minmax(0, 1fr);
    padding: var(--space-4, 16px);
  }

  .decision-buttons {
    flex-direction: column;
  }

  .decision-buttons > button {
    width: 100%;
  }

  .confirm-actions {
    flex-direction: column-reverse;
  }

  .confirm-actions > button {
    width: 100%;
  }
}

@media (max-width: 400px) {
  .score-picker {
    gap: 6px;
  }

  .score-circle {
    width: 30px;
    height: 30px;
  }
}
</style>
