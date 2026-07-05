<template>
  <div class="review-wrapper">
    <!-- BREADCRUMB -->
    <p class="breadcrumb">
      Submitted Proposals / {{ proposal.title }} - 2024
      <span class="active">Review</span>
    </p>

    <!-- TITLE + DOWNLOAD BUTTON -->
    <div class="top-row">
      <h2 class="review-title">Proposal Info</h2>
      <button class="btn-download" @click="goToDetailed">View detailed Proposal</button>
    </div>

  <!-- PROPOSAL INFO -->
<div class="info-grid">
  <div>
    <label>Title</label>
    <p>{{ proposal.title || '-' }}</p>
  </div>

  <div>
    <label>Proponent</label>
    <p>{{ proposal.proponent?.name || '-' }}</p>
  </div>

  <div>
    <label>Department / Office</label>
    <p>{{ proposal.proponent?.departmentOffice || '-' }}</p>
  </div>

  <div>
    <label>Date Submitted</label>
    <p>{{ proposal.dateSubmitted || '-' }}</p>
  </div>

  <div>
    <label>Status</label>
    <p>{{ proposal.status || '-' }}</p>
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
    <p>₱{{ proposal.requestedBudget?.toLocaleString() || '0' }}</p>
  </div>
</div>

    <!-- SCORING GUIDE -->
    <h3 class="section-title">Scoring Guide</h3>

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

    <!-- PART I -->
    <h3 class="section-title">I. Criteria for Evaluating Proposals</h3>

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

    <!-- PART II -->
    <h3 class="section-title">II. Evaluation Criteria</h3>

    <table class="evaluation-table">
      <thead>
        <tr>
          <th>Criteria</th>
          <th>Indicators</th>
          <th>Raw Score (1–5)</th>
        </tr>
      </thead>

      <tbody>
        <!-- A. Soundness of Proposal -->
        <tr class="criteria-row">
          <td colspan="3"><strong>A. Soundness of Proposal (20%)</strong></td>
        </tr>

        <tr>
          <td></td>
          <td>R&D addresses relevant sectoral need (applicable to pressing concern)</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>Solution provided is most effective compared to other proposed solutions</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>Proposed budget is reasonable (not expensive vis-a-vis output)</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>Work plan is doable within the given timeframe</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <!-- B. Suitability of Output -->
        <tr class="criteria-row">
          <td colspan="3"><strong>B. Suitability of Output (30%)</strong></td>
        </tr>

        <tr>
          <td></td>
          <td>
            R&D output is cost-effective (cost is competitive in relation to new or existing
            products or processes)
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Has identified partners to adopt the technology (with letter of support from the head of
            the company)
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Output can be commercialized (through an existing manufacturer, spin-off, or start-up
            company)
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>R&D utilization is timely (output should not be overtaken by other solutions)</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <!-- C. Significance of Outcome -->
        <tr class="criteria-row">
          <td colspan="3"><strong>C. Significance of Outcome (30%)</strong></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Economic impact: increase in productivity , increase in income, new jobs generated, high
            return of investment (ROI)
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Social: working partnerships established, training opportunities provided, policies
            adopted, increased access to basic services (i.e., food, health, education); political,
            cultural, gender sensitivity and inclusivity
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Environment: enhanced environmental health standards, no adverse effect to the
            environment
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Sustainability: sustainability mechanisms established in terms of institutional,
            financial and human resources capability (submission of a new proposal to sustain a
            completed or ongoing proposal does not constitute sustainability of the project)
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <!-- D. Competence of Proponent -->
        <tr class="criteria-row">
          <td colspan="3"><strong>D. Competence of Proponent (20%)</strong></td>
        </tr>

        <tr>
          <td></td>
          <td>Proponent's expertise aligned with the proposal</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>Collaboration with relevant agencies and/or industry partners</td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>

        <tr>
          <td></td>
          <td>
            Thorough understanding of the proposal's deliverables 5 MSUN OVCRIGE has good experience
            with the proponent
          </td>
          <td><input type="number" min="1" max="5" class="rate-input" /></td>
        </tr>
      </tbody>
    </table>

    <!-- COMMENTS -->
    <label class="comment-label">Overall Comments</label>
    <textarea class="comment-box" rows="5"></textarea>

    <!-- DECISION SUBMISSION -->
    <h3 class="decision-title">Decision Submission</h3>

    <div class="decision-buttons">
      <button class="btn-approve" @click="openApproveModal">Approve Proposal</button>
      <button class="btn-revision" @click="goToReturn">Return for Revision</button>
      <button class="btn-reject" @click="goToReject">Reject Proposal</button>
    </div>

    <!-- APPROVAL CONFIRMATION MODAL -->
    <div v-if="showApproveModal" class="modal-overlay">
      <div class="modal-box">
        <h3>Approve Proposal</h3>
        <p>Are you sure you want to approve this?</p>

        <div class="modal-actions">
          <button class="btn-cancel" @click="closeApproveModal">Cancel</button>
          <button class="btn-confirm" @click="confirmApproval">Confirm</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MESSAGE -->
    <div v-if="showSuccessMessage" class="modal-overlay">
      <div class="modal-box">
        <h3>Success</h3>
        <p>The proposal has been approved successfully.</p>

        <div class="modal-actions">
          <button class="btn-confirm" @click="closeSuccessMessage">Okay</button>
        </div>
      </div>
    </div>

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
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const router = useRouter()
const route = useRoute()

// Modal states
const showApproveModal = ref(false)
const showSuccessMessage = ref(false)
const loading = ref(false)
const error = ref('')

// Proposal data
const proposal = ref({
  id: null,
  title: '',
  program: '',
  proponent: '',
  date: '',
  type: '',
  status: ''
})

// Fetch proposal data
onMounted(async () => {
  const proposalId = route.params.id
  if (proposalId) {
    try {
      loading.value = true
      const res = await api.get(`/api/proposals/${proposalId}`)
      const data = res.data
      proposal.value = {
        id: data.id,
        title: data.projectTitle || data.title || '',
        program: data.programTitle || '',
        proponent: data.projectLeader || data.proponent?.name || '',
        date: data.createdAt || '',
        type: data.researchType || '',
        status: data.status || ''
      }
    } catch (err) {
      console.error(err)
      error.value = 'Failed to load proposal details.'
    } finally {
      loading.value = false
    }
  }
})

const openApproveModal = () => {
  showApproveModal.value = true
}

const closeApproveModal = () => {
  showApproveModal.value = false
}

const confirmApproval = async () => {
  try {
    loading.value = true
    await api.put(`/api/proposals/${proposal.value.id}/status?status=REC_APPROVED`)
    showApproveModal.value = false
    showSuccessMessage.value = true
    proposal.value.status = 'REC_APPROVED'
    setTimeout(() => {
      router.push('/rec-dash')
    }, 1500)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to approve proposal.'
    await showAlert('Failed to approve proposal. Please try again.', { type: 'error', title: 'Approval Failed' })
  } finally {
    loading.value = false
  }
}

const goToReturn = () => {
  router.push({ name: 'RecReturn4Revision', params: { id: proposal.value.id } })
}

const goToReject = () => {
  router.push({ name: 'RecRejectProposal', params: { id: proposal.value.id } })
}

const goToDetailed = () => {
  const proposalId = proposal.value.id || route.params.id
  router.push({ path: '/rec-prop', query: { id: proposalId } })
}

const closeSuccessMessage = () => {
  showSuccessMessage.value = false
  router.push('/rec-dash')
}
</script>

<style scoped>
.review-wrapper {
  padding: 20px 25px;
}

/* Breadcrumb */
.breadcrumb {
  font-size: 14px;
  opacity: 0.7;
  margin-bottom: 10px;
}

.breadcrumb .active {
  font-weight: 600;
  color: #2f2a66;
}

/* Title + button */
.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.review-title {
  font-size: 24px;
  margin: 0;
}

.btn-download {
  background: #ffcc00;
  border: none;
  padding: 10px 16px;
  font-weight: 600;
  border-radius: 6px;
  cursor: pointer;
}

/* Info Grid */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 18px;
  background: white;
  border: 1px solid #ddd;
  padding: 18px;
  border-radius: 10px;
  margin: 20px 0;
}

.info-grid label {
  font-weight: 600;
  font-size: 13px;
}

.info-grid p {
  margin: 0;
}

/* Section title */
.section-title {
  font-size: 18px;
  margin: 20px 0 10px;
  font-weight: 600;
}

/* Tables */
.simple-table,
.criteria-table,
.evaluation-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border: 1px solid #aaa;
  margin-bottom: 20px;
}

.simple-table th,
.simple-table td,
.criteria-table th,
.criteria-table td,
.evaluation-table th,
.evaluation-table td {
  border: 1px solid #000;
  padding: 10px;
  font-size: 14px;
}

.criteria-row td {
  background: #f2f2f2;
  font-weight: 600;
}

/* Rating input */
.rate-input {
  width: 60px;
  padding: 6px;
  border: 1px solid #ccc;
  border-radius: 6px;
}

/* Comments */
.comment-label {
  font-weight: 600;
}

.comment-box {
  width: 100%;
  border: 1px solid #aaa;
  border-radius: 8px;
  padding: 12px;
  margin-top: 5px;
  resize: vertical;
}

/* Buttons */
.action-buttons {
  margin-top: 20px;
  display: flex;
  gap: 10px;
}

/* Decision Submission */
.decision-title {
  margin-top: 25px;
  margin-bottom: 10px;
  font-size: 18px;
  font-weight: 600;
}

.decision-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 30px;
}

/* Approve */
.btn-approve {
  background: #4caf50;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

/* Return for Revision */
.btn-revision {
  background: #ffcc00;
  color: #333;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

/* Reject */
.btn-reject {
  background: #f44336;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 6px;
  font-weight: 600;
  cursor: pointer;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-box {
  background: white;
  padding: 20px;
  border-radius: 8px;
  width: 350px;
  text-align: center;
}

.modal-actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 10px;
}

.btn-cancel {
  background: #ccc;
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
}

.btn-confirm {
  background: #4caf50;
  color: white;
  padding: 8px 16px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
}
</style>
