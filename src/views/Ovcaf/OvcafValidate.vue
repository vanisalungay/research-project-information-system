<template>
  <div class="page-wrapper">
    <!-- HEADER TITLE -->
    <div class="header-banner">
      <h1>FUND REQUEST VALIDATION</h1>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="content-area" style="text-align:center;padding:60px;">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="content-area" style="text-align:center;padding:60px;">
      <p class="error-text">{{ error }}</p>
    </div>

    <!-- Proposal Data -->
    <template v-else-if="proposal.id">
      <!-- ACTION BUTTONS -->
      <div class="action-buttons">
        <button class="btn-yellow" @click="approveEndorse" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Approve & Endorse to RII' }}
        </button>
        <button class="btn-yellow" @click="returnProponent" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Return to Proponent' }}
        </button>
        <button class="btn-yellow reject" @click="showRejectModal = true" :disabled="actionLoading">
          Reject Request
        </button>
      </div>

      <!-- SCROLLABLE CONTENT AREA -->
      <div class="content-area">
        <div class="proposal-body">
          <h4>1. PROJECT PROFILE</h4>
          <div class="two-column">
            <div>
              <p><strong>Program Title:</strong> {{ proposal.programTitle || 'N/A' }}</p>
              <p><strong>Project Title:</strong> {{ proposal.projectTitle || 'N/A' }}</p>
              <p><strong>Project Leader:</strong> {{ proposal.projectLeader || 'N/A' }}</p>
              <p><strong>Project Duration:</strong> {{ proposal.duration || 'N/A' }}</p>
              <p><strong>Start Date:</strong> {{ proposal.startDate || 'N/A' }}</p>
              <p><strong>End Date:</strong> {{ proposal.endDate || 'N/A' }}</p>
            </div>
            <div>
              <p><strong>College/Department:</strong> {{ proposal.college || 'N/A' }}</p>
              <p><strong>Status:</strong> <span class="status-badge">{{ proposal.status }}</span></p>
              <p><strong>Date Submitted:</strong> {{ proposal.createdAt?.substring(0,10) || 'N/A' }}</p>
            </div>
          </div>

          <h4>EXECUTIVE SUMMARY</h4>
          <p>{{ proposal.executiveSummary || 'No executive summary provided.' }}</p>

          <h4>BUDGET</h4>
          <table class="impl-table" v-if="proposal.budget && proposal.budget.length > 0">
            <thead>
              <tr>
                <th>Agency</th>
                <th>PS</th>
                <th>MOOE</th>
                <th>EO</th>
                <th>Total</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, idx) in proposal.budget" :key="idx">
                <td>{{ item.agency || 'N/A' }}</td>
                <td>{{ item.ps || '0' }}</td>
                <td>{{ item.mooe || '0' }}</td>
                <td>{{ item.eo || '0' }}</td>
                <td>{{ item.total || '0' }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else>No budget data available.</p>

          <button class="download-btn" @click="downloadProject">DOWNLOAD PROJECT</button>
        </div>
      </div>
    </template>

    <!-- No proposal found -->
    <div v-else class="content-area" style="text-align:center;padding:60px;">
      <p>No proposal ID provided. Please select a proposal from the dashboard.</p>
    </div>

    <!-- REJECT MODAL -->
    <div class="overlay" v-if="showRejectModal">
      <div class="confirm-box">
        <p class="confirm-text">Please provide a reason for rejection:</p>
        <textarea v-model="rejectReason" class="reason-input" placeholder="Enter rejection reason..."></textarea>
        <div class="confirm-actions">
          <button class="btn-confirm" @click="confirmReject" :disabled="!rejectReason.trim()">CONFIRM</button>
          <button class="btn-cancel" @click="showRejectModal = false">CANCEL</button>
        </div>
      </div>
    </div>

    <!-- APPROVE CONFIRMATION MODAL -->
    <div class="overlay" v-if="showApproveConfirm">
      <div class="confirm-box">
        <p class="confirm-text">Are you sure you want to approve this fund request and endorse it to RII for release?</p>
        <div class="confirm-actions">
          <button class="btn-confirm" @click="approveConfirmed">CONFIRM</button>
          <button class="btn-cancel" @click="showApproveConfirm = false">CANCEL</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MESSAGE MODAL -->
    <div class="overlay" v-if="showSuccess">
      <div class="success-box">{{ successMessage }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'

const route = useRoute()

const proposal = ref({})
const loading = ref(true)
const error = ref(null)
const actionLoading = ref(false)

const showRejectModal = ref(false)
const rejectReason = ref('')
const showApproveConfirm = ref(false)
const showSuccess = ref(false)
const successMessage = ref('')

const loadProposal = async () => {
  loading.value = true
  error.value = null
  try {
    const proposalId = route.params.id
    if (!proposalId) {
      loading.value = false
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

const approveEndorse = () => {
  showApproveConfirm.value = true
}

const approveConfirmed = async () => {
  showApproveConfirm.value = false
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/status?status=READY_FOR_RELEASE`)
    successMessage.value = 'Fund request approved and endorsed to RII.'
    showSuccess.value = true
    proposal.value.status = 'READY_FOR_RELEASE'
  } catch (err) {
    successMessage.value = 'Failed to approve fund request. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const returnProponent = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/return-revision`)
    successMessage.value = 'Returned to proponent successfully.'
    showSuccess.value = true
  } catch (err) {
    successMessage.value = 'Failed to return. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) return
  showRejectModal.value = false
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/reject`)
    successMessage.value = 'Request rejected.'
    showSuccess.value = true
  } catch (err) {
    successMessage.value = 'Failed to reject. Please try again.'
    showSuccess.value = true
  } finally {
    actionLoading.value = false
    rejectReason.value = ''
    setTimeout(() => { showSuccess.value = false }, 3000)
  }
}

const downloadProject = () => {
  const link = document.createElement('a')
  link.href = `/files/project-proposal.pdf`
  link.download = 'project-proposal.pdf'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}
</script>

<style scoped>
.page-wrapper {
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column;
  height: 100vh;
  width: 135%;
}
.header-banner {
  background: #1c2145;
  padding: 20px 30px;
  color: white;
  font-weight: bold;
}
.header-banner h1 {
  margin: 0;
  font-size: 28px;
  letter-spacing: 1px;
}
.action-buttons {
  width: 100%;
  padding: 20px 30px;
  background: white;
  border-bottom: 2px solid #e6e6e6;
  display: flex;
  justify-content: center;
  gap: 20px;
  box-sizing: border-box;
}
.content-area {
  padding: 20px 40px;
  overflow-y: auto;
}
.btn-yellow {
  background: #ffd400;
  border: none;
  padding: 15px 25px;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
  font-size: 15px;
}
.btn-yellow:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.reject {
  background: #ffd400;
}
.proposal-body h4 {
  margin-top: 35px;
  margin-bottom: 10px;
  font-weight: bold;
  font-size: 18px;
}
.proposal-body p {
  line-height: 1.6;
  font-size: 15px;
}
.two-column {
  display: flex;
  justify-content: space-between;
  gap: 40px;
}
.two-column > div {
  width: 48%;
}
.impl-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 15px;
  font-size: 15px;
}
.impl-table th, .impl-table td {
  border: 1px solid black;
  padding: 8px;
}
.download-btn {
  margin-left: auto;
  display: block;
  background: #ffd800;
  border: none;
  padding: 12px 20px;
  border-radius: 10px;
  cursor: pointer;
  margin-top: 20px;
}
.overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
}
.confirm-box {
  background: #3c376b;
  width: 600px;
  padding: 35px 30px;
  border-radius: 40px;
  color: #ffd700;
  text-align: center;
}
.confirm-text {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 35px;
}
.confirm-actions {
  display: flex;
  justify-content: center;
  gap: 60px;
}
.btn-confirm {
  background: #ffd700;
  color: black;
  border: none;
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}
.btn-cancel {
  background: #ff0000;
  color: white;
  border: none;
  padding: 12px 35px;
  border-radius: 15px;
  font-weight: bold;
  cursor: pointer;
}
.success-box {
  background: #3c376b;
  color: #ffd700;
  padding: 15px 40px;
  border-radius: 40px;
  font-weight: bold;
  font-size: 18px;
}
.reason-input {
  width: 80%;
  padding: 10px;
  border-radius: 10px;
  border: none;
  margin-bottom: 20px;
  font-size: 14px;
  color: #000;
}
.error-text { color: #dc2626; }
.status-badge {
  background: #ffd700;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  color: #000;
}
</style>