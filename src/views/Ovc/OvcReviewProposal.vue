<template>
  <div class="page-wrapper">
    <!-- HEADER -->
    <div class="page-header">
      <div>
        <h2>{{ proposal.projectTitle || 'Loading...' }}</h2>
        <span class="badge">{{ proposal.status }}</span>
      </div>
      <div class="actions">
        <button class="btn outline" @click="downloadAll">Download All Documents</button>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="btn primary" @click="$router.push('/submit-proposals')">Back to List</button>
    </div>

    <!-- MAIN CONTENT -->
    <div v-else class="grid">
      <!-- LEFT -->
      <div class="left">
        <section class="card">
          <h3>Proposal Information</h3>
          <div class="info-grid">
            <p><strong>Program:</strong> {{ proposal.programTitle || 'N/A' }}</p>
            <p><strong>Project:</strong> {{ proposal.projectTitle || 'N/A' }}</p>
            <p><strong>Proponent:</strong> {{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p>
            <p><strong>College:</strong> {{ proposal.college || 'N/A' }}</p>
            <p><strong>Duration:</strong> {{ proposal.duration || 'N/A' }}</p>
            <p><strong>Date Submitted:</strong> {{ proposal.createdAt?.substring(0,10) || 'N/A' }}</p>
          </div>
        </section>

        <section class="card">
          <h3>Executive Summary</h3>
          <p>{{ proposal.executiveSummary || 'No executive summary provided.' }}</p>
        </section>

        <section class="card actions-card">
          <h3>OVCRIGE Actions</h3>
          <p class="action-hint">Choose an action for this proposal:</p>
          <button v-if="proposal.status === 'ENDORSED'" class="btn primary" @click="forwardToRec" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : 'Forward to REC for Evaluation' }}
          </button>
          <button v-if="proposal.status === 'REC_APPROVED' || proposal.status === 'OVC_APPROVED'" class="btn primary" @click="forwardToOvcaf" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : 'Forward to OVCAF for Review & Endorsement' }}
          </button>
          <button class="btn warning" @click="returnForRevision" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : 'Return for Revision' }}
          </button>
          <button class="btn danger" @click="rejectProposal" :disabled="actionLoading">
            {{ actionLoading ? 'Processing...' : 'Reject Proposal' }}
          </button>
        </section>
      </div>

      <!-- RIGHT -->
      <div class="right">
        <section class="card">
          <h3>Status Timeline</h3>
          <ul class="timeline">
            <li>
              <span class="dot"></span>
              <div>
                <strong>Proposal Submitted</strong>
                <small>{{ proposal.createdAt?.substring(0,10) || 'N/A' }}</small>
              </div>
            </li>
            <li v-if="proposal.status !== 'SUBMITTED'">
              <span class="dot"></span>
              <div>
                <strong>{{ proposal.status === 'UNDER_REVIEW' ? 'Forwarded to REC' : proposal.status === 'REC_APPROVED' ? 'REC Approved' : proposal.status === 'FOR_OVCAF_APPROVAL' ? 'Forwarded to OVCAF' : proposal.status === 'FOR_OC_APPROVAL' ? 'Forwarded to OC' : 'Status Updated' }}</strong>
                <small>{{ proposal.updatedAt?.substring(0,10) || 'N/A' }}</small>
              </div>
            </li>
          </ul>
        </section>

        <section class="card">
          <h3>Attachments</h3>
          <ul class="files">
            <li v-if="proposal.reviewFileName">
              📄 Review of Literature
              <button class="download" @click="downloadFile(proposal.reviewFileName)">⬇️</button>
            </li>
            <li v-if="proposal.roadmapFileName">
              📄 Technology Roadmap
              <button class="download" @click="downloadFile(proposal.roadmapFileName)">⬇️</button>
            </li>
            <li v-if="proposal.gadFileName">
              📄 GAD Score
              <button class="download" @click="downloadFile(proposal.gadFileName)">⬇️</button>
            </li>
            <li v-if="!proposal.reviewFileName && !proposal.roadmapFileName && !proposal.gadFileName">
              No attachments uploaded.
            </li>
          </ul>
        </section>
      </div>
    </div>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>{{ successMessage }}</p>
        <button class="btn primary" @click="showSuccess = false; $router.push('/submit-proposals')">OK</button>
      </div>
    </div>

    <!-- ERROR MODAL -->
    <div v-if="showError" class="modal-overlay">
      <div class="modal-box">
        <p class="error-text">{{ errorMessage }}</p>
        <button class="btn primary" @click="showError = false">OK</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()
const route = useRoute()

const proposal = ref({})
const loading = ref(true)
const error = ref(null)
const actionLoading = ref(false)
const showSuccess = ref(false)
const showError = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const loadProposal = async () => {
  loading.value = true
  error.value = null
  try {
    const proposalId = route.params.id
    if (!proposalId) {
      error.value = 'No proposal ID provided.'
      return
    }
    const res = await api.get(`/api/proposals/${proposalId}`)
    proposal.value = res.data
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

onMounted(loadProposal)

const forwardToRec = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-rec`)
    successMessage.value = 'Proposal has been forwarded to REC for evaluation.'
    showSuccess.value = true
    proposal.value.status = 'UNDER_REVIEW'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const forwardToOvcaf = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-ovcaf`)
    successMessage.value = 'Proposal has been forwarded to OVCAF for review and endorsement.'
    showSuccess.value = true
    proposal.value.status = 'FOR_OVCAF_APPROVAL'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const forwardToOc = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-oc`)
    successMessage.value = 'Proposal has been forwarded to the Chancellor (OC) for final approval.'
    showSuccess.value = true
    proposal.value.status = 'FOR_OC_APPROVAL'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const returnForRevision = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/return-revision`)
    successMessage.value = 'Proposal has been returned to the proponent for revision.'
    showSuccess.value = true
    proposal.value.status = 'REVISION'
  } catch (err) {
    errorMessage.value = 'Failed to return proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const rejectProposal = async () => {
  if (!confirm('Are you sure you want to reject this proposal?')) return
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/reject`)
    successMessage.value = 'Proposal has been rejected.'
    showSuccess.value = true
    proposal.value.status = 'REJECTED'
  } catch (err) {
    errorMessage.value = 'Failed to reject proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const downloadFile = (fileName) => {
  if (!fileName) return
  const link = document.createElement('a')
  link.href = `http://localhost:8081/uploads/${fileName}`
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const downloadAll = () => {
  const files = [proposal.value.reviewFileName, proposal.value.roadmapFileName, proposal.value.gadFileName].filter(Boolean)
  files.forEach((file, index) => {
    setTimeout(() => downloadFile(file), index * 300)
  })
}
</script>

<style>
.page-wrapper {
  padding: 25px;
  background: #f4f6f8;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.badge {
  background: #ffd803;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: bold;
}
.loading-state {
  text-align: center;
  padding: 60px;
  color: #6b7280;
}
.error-text {
  color: #dc2626;
}
.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}
.card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 15px;
}
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.timeline li {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}
.dot {
  width: 10px;
  height: 10px;
  background: #ffd803;
  border-radius: 50%;
  margin-top: 6px;
}
.files li {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}
.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-weight: bold;
  margin-right: 8px;
  margin-bottom: 8px;
}
.primary {
  background: #4d7cff;
  color: white;
}
.warning {
  background: #f39c12;
  color: white;
}
.danger {
  background: #dc2626;
  color: white;
}
.outline {
  background: #4d7cff;
  color: white;
}
.action-hint {
  color: #6b7280;
  margin-bottom: 12px;
  font-size: 14px;
}
.actions-card .btn {
  display: block;
  width: 100%;
  margin-bottom: 10px;
  padding: 12px;
  font-size: 14px;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}
.modal-box {
  background: white;
  padding: 20px;
  border-radius: 10px;
  text-align: center;
  width: 300px;
}
</style>