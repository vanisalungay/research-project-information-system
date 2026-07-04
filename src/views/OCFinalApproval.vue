<template>
  <div class="page">
    <div v-if="loading">
      Loading proposal...
    </div>

    <div v-else-if="error">
      Failed to load proposal.
    </div>

    <div v-else-if="isEmpty">
      No proposal found.
    </div>

    <div v-else>
      <div class="back" @click="$router.go(-1)">← Back to Dashboard</div>

      <div class="card proposal-card">
        <div class="proposal-left">
          <h1>{{ proposal.project_title }}</h1>

          <div class="proposal-meta">
            <div>
              <span>Proponent</span>
              <p>{{ proposal.project_leader }}</p>
            </div>

            <div>
              <span>Program</span>
              <p>{{ proposal.program_title }}</p>
            </div>

            <div>
              <span>Budget</span>
              <p>{{ proposal.total_budget }}</p>
            </div>

            <div>
              <span>Duration</span>
              <p>{{ proposal.duration }}</p>
            </div>
          </div>
        </div>
        <button class="view-btn" @click="goToDetailed">View Details</button>
      </div>

      <div class="status-row">
        <div
          class="card status-card"
          v-for="status in workflowStatus"
          :key="status.id"
        >
          <div class="status-dot"></div>

          <div>
            <h4>{{ status.stage }}</h4>
            <p>{{ status.status }}</p>
            <small>{{ status.description }}</small>
          </div>
        </div>
      </div>

      <!-- REC EVALUATION SUMMARY -->
      <div class="card">
        <h3>REC Evaluation Summary</h3>

        <div class="eval-row" v-for="row in evaluation" :key="row.label">
          <div class="eval-header">
            <span>{{ row.label }}</span>
            <span>{{ row.score }}</span>
          </div>

          <div class="bar">
            <div class="bar-fill" :style="{ width: row.percent + '%' }"></div>
          </div>
        </div>

        <div class="overall">
          <div class="eval-header">
            <strong>Overall Evaluation Score</strong>
            <strong>{{ proposal.overall_score }}</strong>
          </div>
          <div class="bar overall-bar">
            <div
              class="bar-fill"
              :style="{ width: proposal.overall_percent + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- REC RECOMMENDATION -->
      <div class="card">
        <h3>REC Recommendation</h3>
        <div class="recommendation">
          <strong>{{ proposal.recommendation_title }}</strong>
          <p>{{ proposal.recommendation }}</p>
        </div>
      </div>

      <!-- EXECUTIVE SUMMARY -->
      <div class="card">
        <h3>Executive Summary</h3>
        <p class="summary-text" style="text-align: justify">
          {{ proposal.executive_summary }}
        </p>

        <div class="summary-stats">
          <div>
            <span>Expected Duration</span>
            <p>{{ proposal.duration }}</p>
          </div>
          <div>
            <span>Beneficiaries</span>
            <p>{{ proposal.target_beneficiaries }}</p>
          </div>
        </div>
      </div>

      <!-- CHANCELLOR NOTES -->
      <div class="card">
        <h3>Chancellor’s Notes</h3>
        <textarea
          v-model="chancellorNotes"
          placeholder="Add your notes and comments..."
        ></textarea>
      </div>

      <!-- APPROVE -->
      <div class="footer">
        <button class="approve" @click="showConfirm = true">Approve for Implementation</button>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirm" class="modal-overlay">
      <div class="modal">
        <div class="modal-header">
          <span>Approve for Implementation</span>
          <span class="close" @click="showConfirm = false">×</span>
        </div>

        <div class="modal-body">
          <div class="check-icon">✓</div>

          <h4>Approve This Proposal?</h4>

          <p>
            This is proposal will be approved for implementation. You can upload the Special Order
            separately.
          </p>
        </div>

        <div class="modal-actions">
          <button class="btn-cancel" @click="showConfirm = false">Cancel</button>
          <button class="btn-confirm" @click="confirmApproval">Confirm Approval</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const proposalId = route.params.id

const loading = ref(false)

const error = ref(false)

const showConfirm = ref(false)

const proposal = ref({
  id: null,
  project_title: '',
  project_leader: '',
  program_title: '',
  total_budget: '',
  duration: '',
  executive_summary: '',
  target_beneficiaries: '',
  overall_score: '',
  overall_percent: 0,
  recommendation_title: '',
  recommendation: '',
})

const workflowStatus = ref([])

const evaluation = ref([])

const chancellorNotes = ref('')

const isEmpty = computed(() => {
  return !proposal.value.id
})

async function fetchProposal() {
  loading.value = true
  error.value = false

  try {
    // const response = await axios.get(`/api/oc/final-approval/${proposalId}`)

    // proposal.value = response.data.proposal
    // workflowStatus.value = response.data.workflow_status
    // evaluation.value = response.data.evaluation
    // chancellorNotes.value = response.data.chancellor_notes

  } catch (err) {
    console.error(err)
    error.value = true
  } finally {
    loading.value = false
  }
}

function goToDetailed() {
  router.push(`/oc-detailed/${proposalId}`)
}

async function confirmApproval() {
  showConfirm.value = false

  try {
    // await axios.post(`/api/oc/final-approval/${proposalId}/approve`, {
    //   chancellor_notes: chancellorNotes.value
    // })

    router.push('/oc-final-approval2')
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchProposal()
})
</script>

<style scoped>
.page {
  padding: 24px;
  background: #f4f6f9;
  min-height: 100vh;
}

/* BACK */
.back {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 12px;
  cursor: pointer;
}

/* CARDS */
.card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

/* TOP CARD */
.proposal-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.proposal-left h1 {
  font-size: 20px;
  margin-bottom: 12px;
}

.proposal-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.proposal-meta span {
  font-size: 12px;
  color: #888;
}

.proposal-meta p {
  font-weight: 600;
  margin-top: 4px;
}

.view-btn {
  background: gray;
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

.status-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.status-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.status-dot {
  width: 10px;
  height: 10px;
  background: #28a745;
  border-radius: 50%;
  margin-top: 6px;
}

.status-card h4 {
  margin: 0;
  font-size: 14px;
}

.status-card p {
  margin: 4px 0;
}

.bold {
  font-weight: 700;
}

/* EVALUATION */
.eval-row {
  margin-bottom: 14px;
}

.eval-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 14px;
}

.bar {
  width: 100%;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
}

.bar-fill {
  height: 100%;
  background: #374151;
  border-radius: 4px;
}

.overall {
  margin-top: 20px;
}

.recommendation {
  background: #ecfdf3;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  color: #065f46;
}

.summary-text {
  font-size: 14px;
  color: #374151;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  margin-top: 14px;
}

.summary-stats span {
  font-size: 12px;
  color: #6b7280;
}

/* NOTES */
textarea {
  width: 100%;
  min-height: 120px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 12px;
  font-size: 14px;
}

/* FOOTER */
.footer {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.approve {
  width: 60%;
  padding: 14px;
  background: #2f2e41;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal {
  width: 460px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
  padding: 20px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 14px;
  font-weight: 500;
}

.close {
  cursor: pointer;
  font-size: 20px;
  color: #64748b;
}

/* BODY */
.modal-body {
  text-align: center;
  margin: 26px 0;
}

.check-icon {
  width: 56px;
  height: 56px;
  background: #dcfce7;
  color: #16a34a;
  border-radius: 50%;
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: auto;
}

.modal-body h4 {
  margin-top: 14px;
  font-size: 16px;
}

.modal-body p {
  font-size: 13px;
  color: #475569;
  margin-top: 6px;
}

/* ACTIONS */
.modal-actions {
  display: flex;
  gap: 12px;
}

.btn-cancel {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: white;
  cursor: pointer;
}

.btn-confirm {
  flex: 1;
  padding: 10px;
  border-radius: 10px;
  border: none;
  background: #16a34a;
  color: white;
  cursor: pointer;
}
</style>
