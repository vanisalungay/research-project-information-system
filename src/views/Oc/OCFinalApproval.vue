<template>
  <div class="page">
    <div class="back-link">
      <router-link to="/oc-dashboard" class="back-btn">
        <span class="back-arrow">&#8592;</span> Back to Dashboard
      </router-link>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="card center">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card center">
      <p class="error-text">{{ error }}</p>
      <button class="btn-dark" @click="$router.push('/oc-dashboard')" style="margin-top:12px;">Back to Dashboard</button>
    </div>

    <!-- Proposal Content -->
    <div v-else class="layout">
      <!-- LEFT COLUMN -->
      <div class="left-col">
        <div class="card proposal-card">
          <div class="proposal-header">
            <h1>{{ proposal.projectTitle || 'Untitled' }}</h1>
            <span class="status-badge endorsed">{{ proposal.status || 'N/A' }}</span>
          </div>
          <div class="proposal-meta">
            <div class="meta-item">
              <span class="meta-label">Proponent</span>
              <p class="meta-value">{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">Program</span>
              <p class="meta-value">{{ proposal.programTitle || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">Duration</span>
              <p class="meta-value">{{ proposal.duration || 'N/A' }}</p>
            </div>
            <div class="meta-item">
              <span class="meta-label">College</span>
              <p class="meta-value">{{ proposal.college || 'N/A' }}</p>
            </div>
          </div>
        </div>

        <div class="card">
          <h3>Executive Summary</h3>
          <p class="summary-text">{{ proposal.executiveSummary || 'No executive summary provided.' }}</p>
        </div>

        <div class="card">
          <h3>Chancellor's Notes</h3>
          <textarea v-model="chancellorNotes" placeholder="Add your notes and comments..."></textarea>
        </div>
      </div>

      <!-- RIGHT COLUMN -->
      <div class="right-col">
        <!-- Status Timeline -->
        <div class="card">
          <h3>Approval Trail</h3>
          <div class="timeline">
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">RPS / RII DARES</p>
                <p class="tl-desc">Endorsed</p>
                <small>{{ proposal.createdAt?.substring(0,10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">REC Evaluation</p>
                <p class="tl-desc">Completed</p>
                <small>{{ proposal.updatedAt?.substring(0,10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item done">
              <div class="dot"></div>
              <div>
                <p class="tl-title">OVCRIGE</p>
                <p class="tl-desc">Forwarded to OC</p>
                <small>{{ proposal.updatedAt?.substring(0,10) || 'N/A' }}</small>
              </div>
            </div>
            <div class="timeline-item current">
              <div class="dot pulse"></div>
              <div>
                <p class="tl-title">Office of the Chancellor</p>
                <p class="tl-desc">Pending Final Decision</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Budget Determination -->
        <div class="card">
          <h3>Budget Requirement</h3>
          <p class="budget-desc">Determine whether this project requires budget funding.</p>
          <div class="budget-options">
            <label class="radio-label" :class="{ selected: needsBudget === true }">
              <input type="radio" v-model="needsBudget" :value="true" />
              <span><strong>With Budget</strong><br/>Forward to Finance Office / OVCAF for budget endorsement and release</span>
            </label>
            <label class="radio-label" :class="{ selected: needsBudget === false }">
              <input type="radio" v-model="needsBudget" :value="false" />
              <span><strong>Without Budget</strong><br/>Proponent proceeds directly with project implementation</span>
            </label>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button
            class="btn-approve"
            :class="{ disabled: needsBudget === null }"
            :disabled="actionLoading || needsBudget === null"
            @click="showConfirm = true"
          >
            {{ actionLoading ? 'Processing...' : 'Approve for Implementation' }}
          </button>
          <button
            class="btn-return"
            :disabled="actionLoading"
            @click="returnToOvcrige"
          >
            {{ actionLoading ? 'Processing...' : 'Return to OVCRIGE' }}
          </button>
        </div>
        <p v-if="needsBudget === null" class="hint">Select a budget option above to enable approval.</p>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirm" class="modal-overlay" @click.self="showConfirm = false">
      <div class="modal">
        <div class="modal-header">
          <span>Confirm Approval</span>
          <span class="close" @click="showConfirm = false">&times;</span>
        </div>
        <div class="modal-body">
          <div class="check-icon">&#10003;</div>
          <h4>Approve This Proposal?</h4>
          <p v-if="needsBudget">This proposal will be forwarded to the Finance Office / OVCAF for budget endorsement and release.</p>
          <p v-else>This proposal will be approved for immediate implementation without budget.</p>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showConfirm = false">Cancel</button>
          <button class="btn-confirm" @click="confirmApproval">Confirm Approval</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay" @click.self="handleSuccessClose">
      <div class="modal success-modal">
        <div class="modal-body">
          <div class="check-icon success">&#10003;</div>
          <p>{{ successMessage }}</p>
        </div>
        <div class="modal-actions center">
          <button class="btn-confirm" @click="handleSuccessClose">OK</button>
        </div>
      </div>
    </div>
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
      needsBudget: null,
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
          this.loading = false
          return
        }
        const res = await api.get(`/api/proposals/${proposalId}`)
        this.proposal = res.data || {}
      } catch (err) {
        console.error('Failed to load proposal:', err)
        this.error = 'Failed to load proposal details. Check that the backend is running.'
      } finally {
        this.loading = false
      }
    },
    async confirmApproval() {
      this.showConfirm = false
      this.actionLoading = true
      try {
        const proposalId = this.$route.params.id
        if (this.needsBudget) {
          await api.put(`/api/proposals/${proposalId}/forward-to-ovcaf`)
          this.successMessage = 'Proposal approved! Forwarded to OVCAF for budget endorsement and release.'
        } else {
          await api.put(`/api/proposals/${proposalId}/final-approve`)
          this.successMessage = 'Proposal approved for immediate implementation (no budget required).'
        }
        this.showSuccess = true
      } catch (err) {
        console.error('Approval failed:', err)
        this.successMessage = 'An error occurred. Please ensure the backend server is running and try again.'
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
        console.error('Return failed:', err)
        this.successMessage = 'An error occurred. Please ensure the backend server is running and try again.'
        this.showSuccess = true
      } finally {
        this.actionLoading = false
      }
    },
    handleSuccessClose() {
      this.showSuccess = false
      this.$router.push('/oc-dashboard')
    },
  },
}
</script>

<style scoped>
* {
  box-sizing: border-box;
}

.page {
  padding: 24px;
  background: #f1f5f9;
  min-height: 100vh;
  font-family: 'Segoe UI', Arial, sans-serif;
}

.back-link {
  margin-bottom: 20px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 9px 18px;
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  text-decoration: none;
  transition: all 0.2s;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.back-btn:hover {
  background: #f8fafc;
  border-color: #cbd5e1;
  color: #1e293b;
  box-shadow: 0 2px 6px rgba(0,0,0,0.08);
}

.back-arrow {
  font-size: 16px;
  line-height: 1;
}

.center {
  text-align: center;
  padding: 60px 40px !important;
}

/* Layout */
.layout {
  display: grid;
  grid-template-columns: 1fr 420px;
  gap: 24px;
  align-items: start;
}

@media (max-width: 960px) {
  .layout {
    grid-template-columns: 1fr;
  }
}

/* Cards */
.card {
  background: #fff;
  border-radius: 14px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}

.card h3 {
  font-size: 15px;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 16px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #f1f5f9;
}

/* Proposal Header */
.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 20px;
}

.proposal-header h1 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
  line-height: 1.3;
}

.status-badge.endorsed {
  font-size: 11px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 20px;
  background: #fef3c7;
  color: #92400e;
  white-space: nowrap;
  flex-shrink: 0;
}

/* Meta Grid */
.proposal-meta {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr;
  gap: 16px;
}

@media (max-width: 600px) {
  .proposal-meta {
    grid-template-columns: 1fr 1fr;
  }
}

.meta-label {
  font-size: 11px;
  color: #94a3b8;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  font-weight: 600;
}

.meta-value {
  font-weight: 600;
  color: #334155;
  margin: 4px 0 0 0;
  font-size: 14px;
}

/* Summary */
.summary-text {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  text-align: justify;
  margin: 0;
}

/* Textarea */
textarea {
  width: 100%;
  min-height: 130px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  padding: 14px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s;
}

textarea:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 3px rgba(99, 102, 241, 0.1);
}

/* Timeline */
.timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.timeline-item {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  position: relative;
}

.timeline-item:not(:last-child)::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 32px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
  margin-top: 4px;
  flex-shrink: 0;
  z-index: 1;
}

.timeline-item.done .dot {
  background: #22c55e;
}

.timeline-item.current .dot {
  background: #f59e0b;
}

.dot.pulse {
  box-shadow: 0 0 0 4px rgba(245, 158, 11, 0.25);
}

.tl-title {
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  margin: 0;
}

.tl-desc {
  font-size: 12px;
  color: #64748b;
  margin: 2px 0;
}

.timeline-item small {
  font-size: 11px;
  color: #94a3b8;
}

/* Budget Options */
.budget-desc {
  font-size: 13px;
  color: #64748b;
  margin: 0 0 14px 0;
}

.budget-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.radio-label {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-label:hover {
  border-color: #93c5fd;
}

.radio-label.selected {
  border-color: #3b82f6;
  background: #eff6ff;
}

.radio-label input[type="radio"] {
  margin-top: 2px;
  flex-shrink: 0;
  accent-color: #3b82f6;
}

.radio-label span {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.radio-label span strong {
  color: #1e293b;
  font-size: 14px;
}

/* Action Buttons */
.action-buttons {
  display: flex;
  gap: 12px;
}

.btn-approve {
  flex: 1;
  padding: 14px 24px;
  background: #1e293b;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-approve:hover:not(:disabled) {
  background: #0f172a;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.btn-approve.disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-approve:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-return {
  padding: 14px 28px;
  background: white;
  color: #dc2626;
  border: 2px solid #fca5a5;
  border-radius: 12px;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-return:hover:not(:disabled) {
  background: #fef2f2;
  border-color: #dc2626;
}

.btn-return:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.hint {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 10px;
}

/* Error */
.error-text {
  color: #dc2626;
  font-size: 14px;
}

.btn-dark {
  padding: 12px 32px;
  background: #1e293b;
  color: white;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

/* Modal */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.15s;
}

.modal {
  width: 440px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
  padding: 24px;
  animation: slideUp 0.2s ease;
}

.success-modal {
  width: 400px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.close {
  cursor: pointer;
  font-size: 24px;
  color: #94a3b8;
  line-height: 1;
  transition: color 0.2s;
}

.close:hover {
  color: #475569;
}

.modal-body {
  text-align: center;
  margin: 24px 0;
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
  margin: 0 auto 16px auto;
}

.check-icon.success {
  background: #dcfce7;
  color: #16a34a;
}

.modal-body h4 {
  margin: 0 0 8px 0;
  font-size: 16px;
  color: #1e293b;
}

.modal-body p {
  font-size: 13px;
  color: #64748b;
  margin: 0;
  line-height: 1.6;
}

.modal-actions {
  display: flex;
  gap: 12px;
}

.modal-actions.center {
  justify-content: center;
}

.btn-cancel {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  background: white;
  color: #475569;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-cancel:hover {
  background: #f8fafc;
}

.btn-confirm {
  flex: 1;
  padding: 11px;
  border-radius: 10px;
  border: none;
  background: #16a34a;
  color: white;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-confirm:hover {
  background: #15803d;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>