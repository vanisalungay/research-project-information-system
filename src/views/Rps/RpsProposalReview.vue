<template>
  <div class="proposal-page">
    <header class="proposal-header">
      <button @click="goBack" class="back-btn">← Back</button>
      <div class="header-title">
        <h2>Proposal Review</h2>
        <span>RPS / RII DARES Evaluation</span>
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
        <div class="section-block">
          <h3>Project Profile</h3>
          <div class="grid-2">
            <div><span>Program Title</span><p>{{ proposal.programTitle || 'N/A' }}</p></div>
            <div><span>Project Title</span><p>{{ proposal.projectTitle || 'N/A' }}</p></div>
            <div><span>Project Leader</span><p>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p></div>
            <div><span>College / Department</span><p>{{ proposal.college || 'N/A' }}</p></div>
            <div><span>Duration</span><p>{{ proposal.duration || 'N/A' }} months</p></div>
            <div><span>Start Date</span><p>{{ proposal.startDate || 'N/A' }}</p></div>
            <div><span>End Date</span><p>{{ proposal.endDate || 'N/A' }}</p></div>
            <div><span>Research Type</span><p>{{ proposal.researchType || 'N/A' }}</p></div>
          </div>
        </div>

        <div class="section-block">
          <h3>Cooperating Agencies</h3>
          <p>{{ proposal.cooperatingAgencies || 'Not provided' }}</p>
        </div>

        <div class="section-block">
          <h3>Sites of Implementation</h3>
          <table v-if="proposal.sites && proposal.sites.length" class="proposal-table">
            <thead>
              <tr><th>Country</th><th>Region</th><th>Province</th><th>Municipality</th><th>Barangay</th></tr>
            </thead>
            <tbody>
              <tr v-for="(site, i) in proposal.sites" :key="i">
                <td>{{ site.country }}</td><td>{{ site.region }}</td><td>{{ site.province }}</td>
                <td>{{ site.municipality }}</td><td>{{ site.barangay }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else>No implementation sites provided.</p>
        </div>

        <div class="section-block">
          <h3>Executive Summary</h3>
          <p>{{ proposal.executiveSummary || 'Not provided' }}</p>
        </div>

        <div class="section-block">
          <h3>Methodology</h3>
          <p>{{ proposal.methodology || 'Not provided' }}</p>
        </div>

        <div class="section-block">
          <h3>Budget</h3>
          <table v-if="proposal.budget && proposal.budget.length" class="proposal-table">
            <thead>
              <tr><th>Agency</th><th>PS</th><th>MOOE</th><th>EO</th><th>Total</th></tr>
            </thead>
            <tbody>
              <tr v-for="(b, i) in proposal.budget" :key="i">
                <td>{{ b.agency }}</td><td>{{ b.ps }}</td><td>{{ b.mooe }}</td><td>{{ b.eo }}</td><td>{{ b.total }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else>No budget data available.</p>
        </div>

        <div class="section-block">
          <h3>Attachments</h3>
          <ul class="files">
            <li v-if="proposal.reviewFileName">📄 {{ proposal.reviewFileName }}</li>
            <li v-if="proposal.roadmapFileName">📄 {{ proposal.roadmapFileName }}</li>
            <li v-if="proposal.gadFileName">📄 {{ proposal.gadFileName }}</li>
            <li v-if="!proposal.reviewFileName && !proposal.roadmapFileName && !proposal.gadFileName">No attachments.</li>
          </ul>
        </div>
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
            <h4>Reviewer Notes</h4>
            <textarea v-model="remarks" rows="5" placeholder="Enter your review remarks or comments..."></textarea>
          </div>

          <div class="review-actions">
            <button class="approve-btn" @click="endorseProposal" :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : '✓ Endorse to OVCRIGE' }}
            </button>
            <button class="return-btn" @click="returnForRevision" :disabled="actionLoading">
              ⟳ Return for Revision
            </button>
            <button class="reject-btn" @click="rejectProposal" :disabled="actionLoading">
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
    <ConfirmDialog
      v-if="dialog.show"
      :type="dialog.type"
      :variant="dialog.variant"
      :title="dialog.title"
      :message="dialog.message"
      :confirmText="dialog.confirmText"
      :cancelText="dialog.cancelText"
      @confirm="dialog.onConfirm"
      @cancel="dialog.onCancel"
      @close="dialog.show = false"
    />
  </div>
</template>

<script>
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

export default {
  name: 'RpsProposalReview',
  components: { ConfirmDialog },
  data() {
    return {
      proposal: {},
      loading: true,
      error: null,
      remarks: '',
      showReturnOptions: false,
      actionLoading: false,
      showSuccess: false,
      showError: false,
      successMessage: '',
      errorMessage: '',
      dialog: {
        show: false,
        type: 'info',
        variant: 'alert',
        title: '',
        message: '',
        confirmText: 'OK',
        cancelText: 'Cancel',
        onConfirm: () => {},
        onCancel: () => {},
      },
    }
  },
  async mounted() {
    await this.fetchProposal()
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
    async endorseProposal() {
      const confirmed = await this._showConfirm('Endorse this proposal to OVCRIGE?', {
        title: 'Endorse Proposal',
        type: 'info',
        confirmText: 'Endorse'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
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
      if (!this.remarks.trim()) {
        await this._showAlert('Please provide remarks before returning for revision.', { type: 'warning', title: 'Remarks Required' })
        return
      }
      const confirmed = await this._showConfirm('Return this proposal for revision?', {
        title: 'Return for Revision',
        type: 'warning',
        confirmText: 'Return'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
        await api.put(`/api/proposals/${this.$route.params.id}/return-revision`, null, {
          params: { remarks: this.remarks.trim() }
        })
        this.successMessage = 'Proposal returned to proponent for revision.'
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
      const confirmed = await this._showConfirm('Are you sure you want to reject this proposal?', {
        title: 'Reject Proposal',
        type: 'danger',
        confirmText: 'Reject'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
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
      await this._showAlert('Download feature coming soon.', { type: 'info', title: 'Coming Soon' })
    },
  },
}
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }
.proposal-page { width: 100%; min-height: 100vh; padding: 16px; background: #f5f6fa; }
.proposal-header { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; margin-bottom: 16px; }
.back-btn { padding: 8px 16px; border: none; border-radius: 8px; background: #2563eb; color: #fff; cursor: pointer; font-size: 14px; }
.header-title { text-align: center; }
.header-title h2 { font-size: 20px; color: #1f2937; }
.header-title span { font-size: 13px; color: #6b7280; }
.proposal-layout { display: grid; grid-template-columns: 1fr 320px; gap: 16px; align-items: start; }
.proposal-content { background: #fff; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; }
.proposal-sidebar { background: #fff; border: 1px solid #dbe3ef; border-radius: 12px; padding: 20px; box-shadow: 0 8px 24px rgba(0,0,0,.06); }
.section-block { margin-bottom: 24px; }
.section-block h3 { font-size: 16px; color: #1f2937; border-bottom: 2px solid #e5e7eb; padding-bottom: 8px; margin-bottom: 12px; }
.grid-2 { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.grid-2 span { font-size: 12px; color: #888; display: block; }
.grid-2 p { font-weight: 600; margin-top: 4px; }
.proposal-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.proposal-table th, .proposal-table td { border: 1px solid #e5e7eb; padding: 8px; font-size: 13px; text-align: left; }
.proposal-table th { background: #f8fafc; }
.files { list-style: none; }
.files li { padding: 8px 0; border-bottom: 1px solid #eee; }
.review-card { display: flex; flex-direction: column; gap: 14px; }
.review-card h2 { font-size: 18px; color: #1f2937; }
.review-section h4 { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
.status-badge { display: inline-block; padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: 600; }
.status-badge.submitted { background: #f3e8ff; color: #6b21a8; }
.status-badge.endorsed { background: #d9f5e5; color: #1e7f4f; }
.status-badge.revision { background: #e0e7ff; color: #3730a3; }
.status-badge.rps_returned { background: #fef3c7; color: #92400e; }
textarea { width: 100%; padding: 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; resize: vertical; }
.review-actions { display: flex; flex-direction: column; gap: 10px; margin-top: 12px; }
.approve-btn, .return-btn, .reject-btn { padding: 12px; border: none; border-radius: 8px; font-size: 14px; font-weight: 600; cursor: pointer; }
.approve-btn { background: #10b981; color: white; }
.approve-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.return-btn { background: #f59e0b; color: white; }
.return-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.reject-btn { background: #dc2626; color: white; }
.reject-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.return-options { padding: 12px; background: #fef3c7; border-radius: 8px; font-size: 13px; }
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-box { background: white; padding: 24px; border-radius: 12px; text-align: center; max-width: 400px; }
.modal-box p { margin-bottom: 16px; font-size: 15px; }
.error-text { color: #dc2626; }
.btn-primary { padding: 10px 24px; background: #2563eb; color: white; border: none; border-radius: 8px; cursor: pointer; font-weight: 600; }
.download-btn { padding: 10px 18px; background: #6b7280; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; }
</style>