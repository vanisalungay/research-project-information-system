<template>
  <div class="proposal-page">
    <header class="proposal-header">
      <button @click="goBack" class="back-btn">← Back</button>
      <div class="header-title">
        <h2>Proposal Review</h2>
        <span>RPS / RPS DARES Evaluation</span>
      </div>
      <div class="header-actions">
        <button class="download-btn" @click="downloadProposal">⬇ Download Proposal</button>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" style="text-align:center;padding:60px;">
      <p>Loading proposal details...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" style="text-align:center;padding:60px;">
      <p style="color:#dc2626;">{{ error }}</p>
      <button @click="goBack" style="margin-top:12px;padding:10px 20px;">Go Back</button>
    </div>

    <div v-else class="proposal-layout">
      <!-- LEFT: Proposal Content -->
      <section class="proposal-content">
        <!-- Every reviewable field of the proposal, rendered from the shared field
             registry (src/utils/proposalFields.js) so each field has its own remark control
             and the correct entity/DTO key. Sections 01-27 are covered exhaustively. -->
        <ProposalFieldSection
          v-for="section in PROPOSAL_FIELD_SECTIONS"
          :key="section.key"
          :section="section"
          :proposal="proposal"
          :proposal-id="$route.params.id"
          :remark-index="remarkIndex"
          :editable="true"
          @download="downloadFile"
          @remark-saved="onRemarkSaved"
          @remark-removed="onRemarkRemoved"
        />
      </section>

      <!-- RIGHT: Review Actions -->
      <aside class="proposal-sidebar">
        <div class="review-card">
          <h2>Reviewer Accountability</h2>
          <div class="review-section">
            <h4>Proposal Status</h4>
            <span class="status-badge" :class="(proposal.status || '').toLowerCase()">{{ proposal.status }}</span>
          </div>

          <div class="review-section">
            <h4>Reviewer Name</h4>
            <input type="text" v-model="reviewerName" placeholder="Enter your full name" class="reviewer-input" />
          </div>

          <div class="review-section">
            <h4>Reviewer Position</h4>
            <select v-model="reviewerPosition" class="position-select">
              <option value="">-- Select Position --</option>
              <option value="RPS_STAFF">RPS Staff</option>
              <option value="RPS_DIRECTOR">RPS Director</option>
            </select>
          </div>

          <div class="review-section">
            <label class="checkbox-label">
              <input type="checkbox" v-model="isCertified" />
              <span>I certify that I am the assigned reviewer.</span>
            </label>
          </div>

          <div class="review-section">
            <h4>Reviewer Notes (overall)</h4>
            <textarea v-model="remarks" rows="5" placeholder="Enter your review remarks or comments..."></textarea>
            <small style="color:#6b7280;">
              Use the field remarks in the proposal to point at a specific field.
            </small>
          </div>

          <div class="review-section">
            <h4>Field Remarks ({{ fieldRemarkCount }})</h4>
            <p v-if="fieldRemarkCount === 0" class="remarks-empty">
              No field remarks yet. Use “Add Remark” beside any field.
            </p>
            <ul v-else class="remarks-list">
              <li v-for="item in fieldRemarkList" :key="item.id">
                <button type="button" class="remarks-link" @click="scrollToField(item)">
                  {{ item.fieldLabel || item.fieldKey || item.sectionLabel }}
                </button>
                <span class="remarks-preview">{{ item.remark }}</span>
              </li>
            </ul>
            <p v-if="remarksError" class="remarks-error">{{ remarksError }}</p>
          </div>

          <div class="review-section">
            <h4>Revision Deadline</h4>
            <input type="date" v-model="revisionDeadline" class="reviewer-input" />
            <small style="color:#6b7280;">Required when returning for revision. RPS sets the proponent's resubmission deadline.</small>
          </div>

          <div class="review-actions">
            <button class="approve-btn" @click="endorseProposal" :disabled="actionLoading || !canPerformAction">
              {{ actionLoading ? 'Processing...' : '✓ Endorse to OVCRIGE' }}
            </button>
            <button class="return-btn" @click="returnForRevision" :disabled="actionLoading || !canPerformAction">
              ⟳ Return for Revision
            </button>
            <button class="reject-btn" @click="rejectProposal" :disabled="actionLoading || !canPerformAction">
              ✕ Reject Proposal
            </button>
          </div>

        </div>
      </aside>
    </div>

    <!-- Success Modal -->
    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>{{ successMessage }}</p>
        <button class="btn-primary" @click="showSuccess = false; $router.push('/rps-subproposal')">OK</button>
      </div>
    </div>

    <!-- Error Modal -->
    <div v-if="showError" class="modal-overlay">
      <div class="modal-box">
        <p class="error-text">{{ errorMessage }}</p>
        <button class="btn-primary" @click="showError = false">OK</button>
      </div>
    </div>

    <!-- Confirm Dialog -->
    <ConfirmDialog v-if="dialog.show" :type="dialog.type" :variant="dialog.variant" :title="dialog.title"
      :message="dialog.message" :confirmText="dialog.confirmText" :cancelText="dialog.cancelText"
      @confirm="dialog.onConfirm" @cancel="dialog.onCancel" @close="dialog.show = false" />
  </div>
</template>

<script>
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { downloadProposalDocument } from '@/utils/documentExport'
import { downloadUploadedFile } from '@/utils/fileDownload'
import SdgChecklist from '@/components/SdgChecklist.vue'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import {
  PROPOSAL_FIELD_SECTIONS,
  buildFieldRemarkIndex,
  sectionKeyForField,
} from '@/utils/proposalFields'

export default {
  name: 'RpsProposalReview',
  components: { ConfirmDialog, SdgChecklist, ProposalFieldSection },
  data() {
    return {
      proposal: {},
      loading: true,
      error: null,
      remarks: '',
      revisionDeadline: '',
      reviewerName: '',
      reviewerPosition: '',
      isCertified: false,
      showReturnOptions: false,
      actionLoading: false,
      showSuccess: false,
      showError: false,
      successMessage: '',
      errorMessage: '',
      // Field-level remarks: { fieldKey: [remark, ...] } for the active round.
      PROPOSAL_FIELD_SECTIONS,
      remarksResponse: null,
      remarksError: '',
      dialog: {
        show: false,
        type: 'info',
        variant: 'alert',
        title: '',
        message: '',
        confirmText: 'OK',
        cancelText: 'Cancel',
        onConfirm: () => { },
        onCancel: () => { },
      },
    }
  },
  computed: {
    canPerformAction() {
      return this.reviewerName.trim() && this.reviewerPosition && this.isCertified
    },
    hasPriorityAgendas() {
      if (!this.proposal.priorityAgendas) return false
      return Object.values(this.proposal.priorityAgendas).some(agenda => agenda.selected)
    },
    /** { fieldKey: [remark, ...] } for the currently open revision round. */
    remarkIndex() {
      return buildFieldRemarkIndex(this.remarksResponse)
    },
    /** Flat list of the field remarks recorded so far (sidebar summary). */
    fieldRemarkList() {
      return Object.values(this.remarkIndex).flat()
    },
    fieldRemarkCount() {
      return this.fieldRemarkList.length
    }
  },
  async mounted() {
    await this.fetchProposal()
    await this.loadFieldRemarks()
  },
  methods: {
    _showDialog(message, options = {}) {
      return new Promise((resolve) => {
        this.dialog.type = options.type || 'info'
        this.dialog.variant = options.variant || 'alert'
        this.dialog.title = options.title || 'Notice'
        this.dialog.message = message
        this.dialog.confirmText = options.confirmText || 'OK'
        this.dialog.cancelText = options.cancelText || 'Cancel'
        this.dialog.onConfirm = () => { this.dialog.show = false; resolve(true) }
        this.dialog.onCancel = () => { this.dialog.show = false; resolve(false) }
        this.dialog.show = true
      })
    },
    _showAlert(message, options = {}) {
      return this._showDialog(message, { ...options, variant: 'alert' })
    },
    _showConfirm(message, options = {}) {
      return this._showDialog(message, { ...options, variant: 'confirm' })
    },
    async fetchProposal() {
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
    goBack() {
      this.$router.push('/rps-subproposal')
    },
    /** Loads the section- and field-level remarks of this proposal. */
    async loadFieldRemarks() {
      try {
        const res = await api.get(`/api/proposals/${this.$route.params.id}/field-remarks`)
        this.remarksResponse = res.data || null
        this.remarksError = ''
      } catch (err) {
        console.error(err)
        this.remarksResponse = null
        this.remarksError = 'Unable to load the existing remarks.'
      }
    },
    /** Reflects a remark saved from a field control without a full refetch. */
    onRemarkSaved(item) {
      const current = { ...(this.remarksResponse || { proposalId: this.$route.params.id, activeRound: 1, rounds: [] }) }
      const rounds = Array.isArray(current.rounds) ? [...current.rounds] : []
      let activeIndex = rounds.findIndex((round) => round.active)
      if (activeIndex < 0) {
        rounds.unshift({ round: current.activeRound || 1, active: true, remarks: [] })
        activeIndex = 0
        current.activeRound = rounds[0].round
      }
      const remarks = [...(rounds[activeIndex].remarks || [])]
      const existing = remarks.findIndex((row) => row.id === item.id || row.fieldKey === item.fieldKey)
      if (existing >= 0) remarks[existing] = item
      else remarks.push(item)
      rounds[activeIndex] = { ...rounds[activeIndex], remarks }
      this.remarksResponse = { ...current, rounds }
    },
    /** Removes a deleted remark from the local index. */
    onRemarkRemoved(remarkId) {
      if (!this.remarksResponse) return
      const rounds = (this.remarksResponse.rounds || []).map((round) => ({
        ...round,
        remarks: (round.remarks || []).filter((row) => row.id !== remarkId),
      }))
      this.remarksResponse = { ...this.remarksResponse, rounds }
    },
    /** Scrolls to the section that owns a field remark. */
    scrollToField(item) {
      const sectionKey = item.sectionKey || sectionKeyForField(item.fieldKey)
      const target = document.getElementById(`pf-section-${sectionKey}`)
      if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' })
    },
    async saveReviewerInfo() {
      try {
        await api.put(`/api/proposals/${this.$route.params.id}/reviewer-info`, null, {
          params: {
            reviewedBy: this.reviewerName.trim(),
            reviewedByPosition: this.reviewerPosition
          }
        })
      } catch (err) {
        console.error('Failed to save reviewer info:', err)
      }
    },
    async endorseProposal() {
      if (!this.canPerformAction) {
        await this._showAlert('Please complete the reviewer accountability section before endorsing.', { type: 'warning', title: 'Reviewer Info Required' })
        return
      }
      const confirmed = await this._showConfirm('Endorse this proposal to OVCRIGE?', {
        title: 'Endorse Proposal',
        type: 'info',
        confirmText: 'Endorse'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
        // Save reviewer identity first
        await this.saveReviewerInfo()
        await api.put(`/api/proposals/${this.$route.params.id}/endorse`)
        this.successMessage = 'Proposal has been endorsed to OVCRIGE.'
        this.showSuccess = true
        this.proposal.status = 'ENDORSED'
      } catch (err) {
        console.error(err)
        this.errorMessage = 'Failed to endorse proposal. Please try again.'
        this.showError = true
      } finally {
        this.actionLoading = false
      }
    },
    async returnForRevision() {
      if (!this.canPerformAction) {
        await this._showAlert('Please complete the reviewer accountability section before returning.', { type: 'warning', title: 'Reviewer Info Required' })
        return
      }
      if (!this.remarks.trim()) {
        await this._showAlert('Please provide remarks before returning for revision.', { type: 'warning', title: 'Remarks Required' })
        return
      }
      if (!this.revisionDeadline) {
        await this._showAlert('Please set the revision deadline before returning for revision.', { type: 'warning', title: 'Deadline Required' })
        return
      }
      const confirmed = await this._showConfirm('Return this proposal for revision and forward the request to the proponent?', {
        title: 'Return for Revision',
        type: 'warning',
        confirmText: 'Return'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
        // Save reviewer identity first
        await this.saveReviewerInfo()
        let currentUserId = null
        let currentUserName = this.reviewerName.trim() || null
        try {
          const stored = localStorage.getItem('user_data')
          if (stored) {
            const parsed = JSON.parse(atob(stored))
            currentUserId = parsed && parsed.id ? parsed.id : null
            if (!currentUserName && parsed && parsed.name) currentUserName = parsed.name
          }
        } catch (e) { /* ignore */ }
        // RPS is the forwarder: set the deadline and forward to the proponent.
        await api.put(`/api/proposals/${this.$route.params.id}/forward-revision`, {
          forwardedById: currentUserId,
          forwardedByName: currentUserName,
          deadline: this.revisionDeadline,
          notes: null,
          remarks: this.remarks.trim()
        })
        this.successMessage = 'Proposal returned and forwarded to the proponent for revision.'
        this.showSuccess = true
        this.proposal.status = 'RPS_RETURNED'
      } catch (err) {
        console.error(err)
        this.errorMessage = 'Failed to return proposal. Please try again.'
        this.showError = true
      } finally {
        this.actionLoading = false
      }
    },
    async rejectProposal() {
      if (!this.canPerformAction) {
        await this._showAlert('Please complete the reviewer accountability section before rejecting.', { type: 'warning', title: 'Reviewer Info Required' })
        return
      }
      const confirmed = await this._showConfirm('Are you sure you want to reject this proposal?', {
        title: 'Reject Proposal',
        type: 'danger',
        confirmText: 'Reject'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
        // Save reviewer identity first
        await this.saveReviewerInfo()
        await api.put(`/api/proposals/${this.$route.params.id}/reject`)
        this.successMessage = 'Proposal has been rejected.'
        this.showSuccess = true
        this.proposal.status = 'REJECTED'
      } catch (err) {
        console.error(err)
        this.errorMessage = 'Failed to reject proposal. Please try again.'
        this.showError = true
      } finally {
        this.actionLoading = false
      }
    },
    async downloadProposal() {
      downloadProposalDocument(this.proposal)
    },
    downloadFile(fileName) {
      downloadUploadedFile(fileName)
    },
  },
}
</script>

<style scoped>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.proposal-page {
  width: 100%;
  min-height: 100vh;
  padding: 16px;
  background: #f5f6fa;
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  margin-bottom: 16px;
}

.back-btn {
  padding: 8px 16px;
  border: none;
  border-radius: 8px;
  background: #2563eb;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.header-title {
  text-align: center;
}

.header-title h2 {
  font-size: 20px;
  color: #1f2937;
}

.header-title span {
  font-size: 13px;
  color: #6b7280;
}

.proposal-layout {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 16px;
  align-items: start;
}

.proposal-content {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.proposal-sidebar {
  background: #fff;
  border: 1px solid #dbe3ef;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, .06);
}

.section-block {
  margin-bottom: 24px;
}

.section-block h3 {
  font-size: 16px;
  color: #1f2937;
  border-bottom: 2px solid #e5e7eb;
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.grid-2 span {
  font-size: 12px;
  color: #888;
  display: block;
}

.grid-2 p {
  font-weight: 600;
  margin-top: 4px;
}

.proposal-table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 8px;
}

.proposal-table th,
.proposal-table td {
  border: 1px solid #e5e7eb;
  padding: 8px;
  font-size: 13px;
  text-align: left;
}

.proposal-table th {
  background: #f8fafc;
}

.files {
  list-style: none;
}

.files li {
  padding: 8px 0;
  border-bottom: 1px solid #eee;
}

.review-card {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.review-card h2 {
  font-size: 18px;
  color: #1f2937;
}

.review-section h4 {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.remarks-empty {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.remarks-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 240px;
  overflow-y: auto;
}

.remarks-list li {
  border-left: 3px solid #f59e0b;
  padding-left: 8px;
}

.remarks-link {
  display: block;
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
  color: #4f46e5;
  text-align: left;
  cursor: pointer;
}

.remarks-link:hover {
  text-decoration: underline;
}

.remarks-preview {
  display: block;
  font-size: 11px;
  color: #6b7280;
  line-height: 1.4;
  margin-top: 2px;
}

.remarks-error {
  margin: 8px 0 0;
  font-size: 11px;
  color: #dc2626;
}

.status-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.status-badge.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.status-badge.endorsed {
  background: #d9f5e5;
  color: #1e7f4f;
}

.status-badge.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.status-badge.rps_returned {
  background: #fef3c7;
  color: #92400e;
}

textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
}

.reviewer-input,
.position-select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  background: #fff;
  color: #1e293b;
  margin-top: 4px;
}

.reviewer-input::placeholder {
  color: #9ca3af;
}

.position-select {
  cursor: pointer;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #475569;
  cursor: pointer;
  margin-top: 4px;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}

.review-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 12px;
}

.approve-btn,
.return-btn,
.reject-btn {
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.approve-btn {
  background: #10b981;
  color: white;
}

.approve-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.return-btn {
  background: #f59e0b;
  color: white;
}

.return-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reject-btn {
  background: #dc2626;
  color: white;
}

.reject-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.return-options {
  padding: 12px;
  background: #fef3c7;
  border-radius: 8px;
  font-size: 13px;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-box {
  background: white;
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  max-width: 400px;
}

.modal-box p {
  margin-bottom: 16px;
  font-size: 15px;
}

.error-text {
  color: #dc2626;
}

.btn-primary {
  padding: 10px 24px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

.download-btn {
  padding: 10px 18px;
  background: #6b7280;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
}

.full-width {
  grid-column: 1 / -1;
}

.text-content {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  white-space: pre-wrap;
  margin: 0;
}

.empty-text {
  color: #94a3b8;
  font-style: italic;
  font-size: 14px;
}

.subsection {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f1f5f9;
}

.subsection:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.subsection h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 6px 0;
}

.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.agenda-item {
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 6px;
  border-left: 3px solid #4f46e5;
  font-size: 14px;
}

.agenda-item strong {
  color: #4f46e5;
  font-size: 13px;
}

.file-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
}

.btn-download-sm {
  padding: 4px 12px;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.btn-download-sm:hover {
  background: #4338ca;
}

.section-block p {
  margin: 4px 0;
  color: #334155;
  font-size: 14px;
}

.section-block p strong {
  color: #0f172a;
}
</style>
