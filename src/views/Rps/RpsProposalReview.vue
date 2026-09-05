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
        <!-- 1. Project Profile -->
        <div class="section-block">
          <h3>(1) Project Profile</h3>
          <div class="grid-2">
            <div><span>Program Title</span>
              <p>{{ proposal.programTitle || 'N/A' }}</p>
            </div>
            <div><span>Project Title</span>
              <p>{{ proposal.projectTitle || 'N/A' }}</p>
            </div>
            <div><span>Project Leader</span>
              <p>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</p>
            </div>
            <div><span>Sex</span>
              <p>{{ proposal.projectLeaderSex || 'N/A' }}</p>
            </div>
            <div><span>Duration</span>
              <p>{{ proposal.duration || 'N/A' }} months</p>
            </div>
            <div><span>Start Date</span>
              <p>{{ proposal.startDate || 'N/A' }}</p>
            </div>
            <div><span>End Date</span>
              <p>{{ proposal.endDate || 'N/A' }}</p>
            </div>
            <div><span>Research Type</span>
              <p>{{ proposal.researchType || 'N/A' }}</p>
            </div>
            <div class="full-width"><span>Implementing College / Department</span>
              <p>{{ proposal.college || 'N/A' }}</p>
            </div>
            <div class="full-width"><span>Address / Contact</span>
              <p>{{ proposal.address || 'N/A' }}</p>
            </div>
          </div>
        </div>

        <!-- 2. Cooperating Agencies -->
        <div class="section-block">
          <h3>(2) Cooperating Agencies</h3>
          <p class="text-content">{{ proposal.cooperatingAgencies || 'Not provided' }}</p>
        </div>

        <!-- 3. Sites of Implementation -->
        <div class="section-block">
          <h3>(3) Sites of Implementation</h3>
          <table v-if="proposal.sites && proposal.sites.length" class="proposal-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Country</th>
                <th>Region</th>
                <th>Province</th>
                <th>District</th>
                <th>Municipality</th>
                <th>Barangay</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(site, i) in proposal.sites" :key="i">
                <td>{{ i + 1 }}</td>
                <td>{{ site.country }}</td>
                <td>{{ site.region }}</td>
                <td>{{ site.province }}</td>
                <td>{{ site.district }}</td>
                <td>{{ site.municipality }}</td>
                <td>{{ site.barangay }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-text">No implementation sites provided.</p>
        </div>

        <!-- 4. Type of Research -->
        <div class="section-block">
          <h3>(4) Type of Research</h3>
          <p class="text-content">{{ proposal.researchType || 'Not specified' }}</p>
        </div>

        <!-- 5. Priority Agenda -->
        <div class="section-block">
          <h3>(5) Priority Agenda</h3>
          <div v-if="hasPriorityAgendas" class="agenda-list">
            <div v-if="proposal.priorityAgendas?.dagat?.selected" class="agenda-item"><strong>DAGAT:</strong> {{
              proposal.priorityAgendas.dagat.value }}</div>
            <div v-if="proposal.priorityAgendas?.punla?.selected" class="agenda-item"><strong>PUNLA:</strong> {{
              proposal.priorityAgendas.punla.value }}</div>
            <div v-if="proposal.priorityAgendas?.kalikasan?.selected" class="agenda-item"><strong>KALIKASAN:</strong> {{
              proposal.priorityAgendas.kalikasan.value }}</div>
            <div v-if="proposal.priorityAgendas?.negosyo?.selected" class="agenda-item"><strong>NEGOSYO:</strong> {{
              proposal.priorityAgendas.negosyo.value }}</div>
            <div v-if="proposal.priorityAgendas?.tanglaw?.selected" class="agenda-item"><strong>TANGLAW:</strong> {{
              proposal.priorityAgendas.tanglaw.value }}</div>
          </div>
          <p v-else class="empty-text">No priority agenda selected.</p>
        </div>

        <!-- 6. Innovation Goals -->
        <div class="section-block">
          <h3>(6) Innovation Goals</h3>
          <p class="text-content">{{ proposal.innovationGoals || 'Not provided' }}</p>
        </div>

        <!-- 7. Sector Relevance -->
        <div class="section-block">
          <h3>(7) Sector Relevance</h3>
          <p class="text-content">{{ proposal.sectorRelevance || 'Not provided' }}</p>
        </div>

        <!-- 8. SDG -->
        <div class="section-block">
          <h3>(8) Sustainable Development Goals (SDG)</h3>
          <p class="text-content">{{ proposal.sdg || 'Not provided' }}</p>
        </div>

        <!-- 9. Executive Summary -->
        <div class="section-block">
          <h3>(9) Executive Summary</h3>
          <p class="text-content">{{ proposal.executiveSummary || 'Not provided' }}</p>
        </div>

        <!-- 10. Introduction -->
        <div class="section-block">
          <h3>(10) Introduction</h3>
          <div class="subsection">
            <h4>10.1 Rationale / Significance</h4>
            <p class="text-content">{{ proposal.rationale || 'Not provided' }}</p>
          </div>
          <div class="subsection">
            <h4>10.2 Scientific Basis / Theoretical Framework</h4>
            <p class="text-content">{{ proposal.framework || 'Not provided' }}</p>
          </div>
          <div class="subsection">
            <h4>10.3 Objectives</h4>
            <p><strong>General Objective:</strong> {{ proposal.objectivesGeneral || 'Not provided' }}</p>
            <p><strong>Specific Objectives:</strong> {{ proposal.objectivesSpecific || 'Not provided' }}</p>
          </div>
        </div>

        <!-- 11. Review of Literature -->
        <div class="section-block">
          <h3>(11) Review of Literature</h3>
          <div v-if="proposal.reviewFileName" class="file-row">
            <span>📄 {{ proposal.reviewFileName }}</span>
            <button class="btn-download-sm" @click="downloadFile(proposal.reviewFileName)">Download</button>
          </div>
          <p v-else class="empty-text">No file uploaded.</p>
        </div>

        <!-- 12. Methodology -->
        <div class="section-block">
          <h3>(12) Methodology</h3>
          <p class="text-content">{{ proposal.methodology || 'Not provided' }}</p>
        </div>

        <!-- 13. Technology Roadmap -->
        <div class="section-block">
          <h3>(13) Technology Roadmap</h3>
          <div v-if="proposal.roadmapFileName" class="file-row">
            <span>📄 {{ proposal.roadmapFileName }}</span>
            <button class="btn-download-sm" @click="downloadFile(proposal.roadmapFileName)">Download</button>
          </div>
          <p v-else class="empty-text">No file uploaded.</p>
        </div>

        <!-- 14. Expected Outputs -->
        <div class="section-block">
          <h3>(14) Expected Outputs (6Ps)</h3>
          <p class="text-content">{{ proposal.outputs || 'Not provided' }}</p>
        </div>

        <!-- 15. Potential Outcomes -->
        <div class="section-block">
          <h3>(15) Potential Outcomes</h3>
          <p class="text-content">{{ proposal.outcomes || 'Not provided' }}</p>
        </div>

        <!-- 16. Potential Impacts -->
        <div class="section-block">
          <h3>(16) Potential Impacts (2Is)</h3>
          <p><strong>a. Economic Impact:</strong> {{ proposal.impactEconomic || 'Not provided' }}</p>
          <p><strong>b. Social / Ethical Impact:</strong> {{ proposal.impactSocial || 'Not provided' }}</p>
        </div>

        <!-- 17. Target Beneficiaries -->
        <div class="section-block">
          <h3>(17) Target Beneficiaries</h3>
          <p class="text-content">{{ proposal.beneficiaries || 'Not provided' }}</p>
        </div>

        <!-- 18. Sustainability Plan -->
        <div class="section-block">
          <h3>(18) Sustainability Plan</h3>
          <p class="text-content">{{ proposal.sustainability || 'Not provided' }}</p>
        </div>

        <!-- 19. GAD Score -->
        <div class="section-block">
          <h3>(19) Gender and Development (GAD) Score</h3>
          <div v-if="proposal.gadFileName" class="file-row">
            <span>📄 {{ proposal.gadFileName }}</span>
            <button class="btn-download-sm" @click="downloadFile(proposal.gadFileName)">Download</button>
          </div>
          <p v-else class="empty-text">No file uploaded.</p>
        </div>

        <!-- 20. Limitations -->
        <div class="section-block">
          <h3>(20) Limitations of the Project</h3>
          <p class="text-content">{{ proposal.limitations || 'Not provided' }}</p>
        </div>

        <!-- 21. Risks & Assumptions -->
        <div class="section-block">
          <h3>(21) Risk Management Plan</h3>
          <p class="text-content">{{ proposal.risks || 'Not provided' }}</p>
        </div>

        <!-- 22. Logical Framework -->
        <div class="section-block">
          <h3>(22) Logical Framework</h3>
          <table v-if="proposal.logFrame && proposal.logFrame.length" class="proposal-table">
            <thead>
              <tr>
                <th>Outcome Indicators</th>
                <th>Output Indicators / Physical Target</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in proposal.logFrame" :key="i">
                <td>{{ row.outcome || '-' }}</td>
                <td>{{ row.output || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-text">No logical framework data.</p>
        </div>

        <!-- 23. Literature Cited -->
        <div class="section-block">
          <h3>(23) Literature Cited</h3>
          <p class="text-content">{{ proposal.literatureCited || 'Not provided' }}</p>
        </div>

        <!-- 24. Personnel Requirement -->
        <div class="section-block">
          <h3>(24) Personnel Requirement</h3>
          <table v-if="proposal.personnel && proposal.personnel.length" class="proposal-table">
            <thead>
              <tr>
                <th>Position</th>
                <th>% Time Devoted</th>
                <th>Responsibilities</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in proposal.personnel" :key="i">
                <td>{{ row.position || '-' }}</td>
                <td>{{ row.time || '-' }}</td>
                <td>{{ row.responsibilities || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-text">No personnel data.</p>
        </div>

        <!-- 25. Line-Item Budget -->
        <div class="section-block">
          <h3>(25) Line-Item Budget Requirement</h3>
          <div v-if="proposal.budgetFileName" class="file-row">
            <span>📄 {{ proposal.budgetFileName }}</span>
            <button class="btn-download-sm" @click="downloadFile(proposal.budgetFileName)">Download</button>
          </div>
          <p v-else class="empty-text">No budget file uploaded.</p>
        </div>

        <!-- 26. Other Ongoing Projects -->
        <div class="section-block">
          <h3>(26) Other Ongoing Projects</h3>
          <p v-if="proposal.otherProjectsNumber" class="text-content" style="margin-bottom:8px;">
            <strong>Number of other projects:</strong> {{ proposal.otherProjectsNumber }}
          </p>
          <table v-if="proposal.otherProjects && proposal.otherProjects.length" class="proposal-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Funding Agency</th>
                <th>Involvement</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in proposal.otherProjects" :key="i">
                <td>{{ row.title || '-' }}</td>
                <td>{{ row.agency || '-' }}</td>
                <td>{{ row.involvement || '-' }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="empty-text">No other projects.</p>
        </div>

        <!-- 27. Other Supporting Documents -->
        <div class="section-block">
          <h3>(27) Other Supporting Documents</h3>
          <p class="text-content">Please refer to the attached documents for additional necessary documents.</p>
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
            <h4>Reviewer Notes</h4>
            <textarea v-model="remarks" rows="5" placeholder="Enter your review remarks or comments..."></textarea>
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

export default {
  name: 'RpsProposalReview',
  components: { ConfirmDialog },
  data() {
    return {
      proposal: {},
      loading: true,
      error: null,
      remarks: '',
      reviewerName: '',
      reviewerPosition: '',
      isCertified: false,
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
      const confirmed = await this._showConfirm('Return this proposal for revision?', {
        title: 'Return for Revision',
        type: 'warning',
        confirmText: 'Return'
      })
      if (!confirmed) return
      this.actionLoading = true
      try {
        // Save reviewer identity first
        await this.saveReviewerInfo()
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
