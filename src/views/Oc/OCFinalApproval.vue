<template>
  <div class="page">
    <div class="back" @click="$router.go(-1)">← Back to Dashboard</div>

    <!-- Loading -->
    <div v-if="loading" class="card" style="text-align:center;padding:40px;">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card" style="text-align:center;padding:40px;">
      <p class="error-text">{{ error }}</p>
      <button class="approve" @click="$router.push('/oc-dashboard')" style="margin-top:12px;">Back to Dashboard</button>
    </div>

    <!-- Proposal Content -->
    <template v-else-if="proposal.id">
      <div class="card proposal-card">
        <div class="proposal-left">
          <h1>{{ proposal.projectTitle }}</h1>
          <div class="proposal-meta">
            <div>
              <span>Proponent</span>
              <p>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p>
            </div>
            <div>
              <span>Program</span>
              <p>{{ proposal.programTitle || 'N/A' }}</p>
            </div>
            <div>
              <span>Duration</span>
              <p>{{ proposal.duration || 'N/A' }}</p>
            </div>
            <div>
              <span>College</span>
              <p>{{ proposal.college || 'N/A' }}</p>
            </div>
          </div>
        </div>
        <button class="view-btn" @click="$router.push('/oc-detailed')">View Details</button>
      </div>

      <div class="status-row">
        <div class="card status-card">
          <div class="status-dot"></div>
          <div>
            <h4>RPS / RII DARES</h4>
            <p>Endorsed</p>
            <small>{{ proposal.createdAt?.substring(0,10) || 'N/A' }}</small>
          </div>
        </div>
        <div class="card status-card">
          <div class="status-dot"></div>
          <div>
            <h4>REC Evaluation</h4>
            <p>Completed</p>
            <small>{{ proposal.updatedAt?.substring(0,10) || 'N/A' }}</small>
          </div>
        </div>
        <div class="card status-card">
          <div class="status-dot"></div>
          <div>
            <h4>OVCRIGE</h4>
            <p>Forwarded to OC</p>
            <small>{{ proposal.updatedAt?.substring(0,10) || 'N/A' }}</small>
          </div>
        </div>
      </div>

      <div class="card">
        <h3>Executive Summary</h3>
        <p class="summary-text" style="text-align: justify">
          {{ proposal.executiveSummary || 'No executive summary provided.' }}
        </p>
      </div>

      <div class="card">
        <h3>Chancellor's Notes</h3>
        <textarea v-model="chancellorNotes" placeholder="Add your notes and comments..."></textarea>
      </div>

      <div class="footer">
        <button class="approve" @click="showConfirm = true" :disabled="actionLoading">
          {{ actionLoading ? 'Processing...' : 'Approve for Implementation' }}
        </button>
        <button class="return-btn" @click="returnToOvcrige" :disabled="actionLoading" style="margin-left:12px;">
          {{ actionLoading ? 'Processing...' : 'Return to OVCRIGE' }}
        </button>
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
            <p>This proposal will be approved for implementation. Funds will be notified for release.</p>
          </div>
          <div class="modal-actions">
            <button class="btn-cancel" @click="showConfirm = false">Cancel</button>
            <button class="btn-confirm" @click="confirmApproval">Confirm Approval</button>
          </div>
        </div>
      </div>

      <!-- SUCCESS MODAL -->
      <div v-if="showSuccess" class="modal-overlay">
        <div class="modal">
          <div class="modal-body">
            <p>{{ successMessage }}</p>
          </div>
          <div class="modal-actions" style="justify-content:center;">
            <button class="btn-confirm" @click="showSuccess = false; $router.push('/oc-dashboard')">OK</button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import api from '@/utils/api'

export default {
  name: 'FinalApproval',
  data() {
    return {
      proposal: {},
      loading: true,
      error: null,
      showConfirm: false,
      showSuccess: false,
      successMessage: '',
      actionLoading: false,
      chancellorNotes: '',
    }
  },
  async mounted() {
    await this.loadProposal()
  },
  methods: {
    async loadProposal() {
      this.loading = true
      this.error = null
      try {
        const proposalId = this.$route.params.id
        if (!proposalId) {
          this.error = 'No proposal ID provided.'
          return
        }
        const res = await api.get(`/api/proposals/${proposalId}`)
        this.proposal = res.data
      } catch (err) {
        console.error(err)
        this.error = 'Failed to load proposal details.'
      } finally {
        this.loading = false
      }
    },
    async confirmApproval() {
      this.showConfirm = false
      this.actionLoading = true
      try {
        const proposalId = this.$route.params.id
        await api.put(`/api/proposals/${proposalId}/final-approve`)
        this.successMessage = 'Proposal has been approved for implementation!'
        this.showSuccess = true
        this.proposal.status = 'APPROVED'
      } catch (err) {
        this.successMessage = 'Failed to approve proposal. Please try again.'
        this.showSuccess = true
      } finally {
        this.actionLoading = false
      }
    },
    async returnToOvcrige() {
      this.actionLoading = true
      try {
        const proposalId = this.$route.params.id
        await api.put(`/api/proposals/${proposalId}/return-revision`)
        this.successMessage = 'Proposal has been returned to OVCRIGE.'
        this.showSuccess = true
      } catch (err) {
        this.successMessage = 'Failed to return proposal. Please try again.'
        this.showSuccess = true
      } finally {
        this.actionLoading = false
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding: 24px;
  background: #f4f6f9;
  min-height: 100vh;
}
.back {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 12px;
  cursor: pointer;
}
.card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
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
.summary-text {
  font-size: 14px;
  color: #374151;
}
textarea {
  width: 100%;
  min-height: 120px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 12px;
  font-size: 14px;
}
.footer {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
.approve {
  padding: 14px 40px;
  background: #2f2e41;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}
.return-btn {
  padding: 14px 40px;
  background: #f39c12;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}
.error-text {
  color: #dc2626;
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