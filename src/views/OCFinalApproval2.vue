<template>
  <div class="page">
    <!-- BACK -->
    <div class="back" @click="$router.go(-1)">← Back to Dashboard</div>

    <!-- PROPOSAL HEADER CARD -->
    <div class="card proposal-card">
      <div class="proposal-left">
        <h1>Community Development Program 2024</h1>

        <div class="proposal-meta">
          <div>
            <span>Proponent</span>
            <p>Dr. Maria Santos</p>
          </div>
          <div>
            <span>Program</span>
            <p>DAGAT</p>
          </div>
          <div>
            <span>Budget</span>
            <p>PHP 750,000</p>
          </div>
          <div>
            <span>Duration</span>
            <p>12 Months</p>
          </div>
        </div>
      </div>
      <button class="view-btn" @click="goToDetailed">View Details</button>
    </div>

    <!-- STATUS FLOW -->
    <div class="status-row">
      <div class="card status-card">
        <div class="status-dot"></div>
        <div>
          <h4>RII DARES</h4>
          <p>Endorsed</p>
          <small>Dec 11, 2024</small>
        </div>
      </div>

      <div class="card status-card">
        <div class="status-dot"></div>
        <div>
          <h4>REC Evaluation</h4>
          <p>Approved</p>
          <small>Score: 89/100 (89.0%)</small>
        </div>
      </div>

      <div class="card status-card">
        <div class="status-dot"></div>
        <div>
          <h4>OVCRIGE</h4>
          <p>Forwarded to OC</p>
          <small>Dec 13, 2024</small>
        </div>
      </div>
    </div>

    <!-- REC EVALUATION SUMMARY -->
    <div class="card">
      <h3>REC Evaluation Summary</h3>

      <div v-for="item in evaluation" :key="item.label" class="eval-row">
        <div class="eval-header">
          <span>{{ item.label }}</span>
          <span>{{ item.score }}</span>
        </div>

        <div class="bar">
          <div class="bar-fill" :style="{ width: item.percent + '%' }"></div>
        </div>
      </div>

      <div class="overall">
        <div class="eval-header">
          <strong>Overall Evaluation Score</strong>
          <strong>89/100 (89.0%)</strong>
        </div>
        <div class="bar overall-bar">
          <div class="bar-fill"></div>
        </div>
      </div>
    </div>

    <!-- REC RECOMMENDATION -->
    <div class="card">
      <h3>REC Recommendation</h3>
      <div class="recommendation">
        <strong>Recommended for Approval</strong>
        <p>
          The Research Evaluation Committee unanimously recommends this proposal for approval. The
          research methodology is sound, the budget is justified, and the expected outcomes align
          with the university’s research priorities.
        </p>
      </div>
    </div>

    <!-- FINAL APPROVAL -->
    <div class="card">
      <div class="recommendation">
        <strong>Proposal Approved</strong>
        <p>
          This proposal has been approved for implementation. Please upload the Special Order
          document to proceed.
        </p>
      </div>
    </div>

    <div class="footer">
      <button class="approve-btn" @click="showUploadModal = true">Upload Special Order</button>
    </div>

    <!-- UPLOAD SPECIAL ORDER MODAL -->
    <div v-if="showUploadModal" class="modal-overlay">
      <div class="modal-card">
        <div class="modal-header">
          <div class="modal-icon">🏅</div>
          <h3>Upload Special Order</h3>
          <p>Upload the official Special Order document for this research proposal</p>
        </div>

        <!-- PROPOSAL INFO -->
        <div class="modal-info">
          <div>
            <span>Proposal Title</span>
            <p>Sustainable Aquaculture Systems in Coastal Communities</p>
          </div>
          <div>
            <span>Proponent</span>
            <p>Dr. Maria Santos</p>
          </div>
          <div>
            <span>Program</span>
            <p>DAGAT</p>
          </div>
          <div>
            <span>Budget</span>
            <p>PHP 750,000</p>
          </div>
        </div>

        <!-- FILE UPLOAD -->
        <div class="upload-box">
          <input type="file" accept="application/pdf" hidden ref="fileInput" />
          <div class="upload-area" @click="$refs.fileInput.click()">
            <p>Click to upload or drag and drop</p>
            <small>PDF files only (Max 5MB)</small>
          </div>
        </div>

        <!-- ACTION BUTTONS -->
        <div class="modal-actions">
          <button class="cancel-btn" @click="showUploadModal = false">Cancel</button>
          <button class="issue-btn" @click="issueSpecialOrder">Issue Special Order</button>
        </div>
      </div>
    </div>

    <!-- APPROVAL SUCCESS MODAL -->
    <div v-if="showSuccessModal" class="modal-overlay">
      <div class="success-card">
        <button class="close-btn" @click="showSuccessModal = false">×</button>
        <div class="success-icon">✔</div>
        <h3>Proposal Approved!</h3>
        <p class="success-text">The proposal has been successfully approved.</p>
        <button class="success-btn" @click="goToDashboard">View Approved Proposals</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'OCFinalApproval2',
  data() {
    return {
      showUploadModal: false,
      showSuccessModal: false,
      evaluation: [
        { label: 'Relevance and Significance', score: '18/20', percent: 90 },
        { label: 'Research Methodology', score: '22/25', percent: 88 },
        { label: 'Feasibility and Timeline', score: '13/15', percent: 87 },
        { label: 'Budget Justification', score: '14/15', percent: 93 },
        { label: 'Expected Outcomes', score: '13/15', percent: 87 },
        { label: 'Researcher Qualifications', score: '9/10', percent: 90 },
      ],
    }
  },
  methods: {
    goToDetailed() {
      this.$router.push('/oc-detailed')
    },
    issueSpecialOrder() {
      this.showUploadModal = false
      this.showSuccessModal = true
    },
    goToDashboard() {
      this.$router.push('/home')
    },
  },
}
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

/* CARD */
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

/* VIEW BUTTON */
.view-btn {
  background: gray;
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

/* STATUS ROW */
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

.overall-bar .bar-fill {
  width: 89%;
}

/* RECOMMENDATION */
.recommendation {
  background: #ecfdf3;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  color: #065f46;
}

/* SUMMARY */
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

/* FOOTER */
.footer {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.approve-btn {
  width: 60%;
  padding: 14px;
  background: #2f2e41;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}

/* MODAL OVERLAY */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

/* MODAL CARD */
.modal-card {
  background: #fff;
  width: 520px;
  border-radius: 16px;
  padding: 24px;
}

/* HEADER */
.modal-header {
  text-align: center;
  margin-bottom: 20px;
}

.modal-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.modal-header h3 {
  margin-bottom: 6px;
}

.modal-header p {
  font-size: 13px;
  color: #6b7280;
}

/* INFO GRID */
.modal-info {
  background: #f7f7ff;
  border-radius: 12px;
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.modal-info span {
  font-size: 11px;
  color: #6b7280;
}

.modal-info p {
  font-weight: 600;
  font-size: 13px;
}

/* UPLOAD BOX */
.upload-box {
  margin-bottom: 20px;
}

.upload-area {
  border: 2px dashed #c7c7ff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  color: #6b7280;
}

/* ACTION BUTTONS */
.modal-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.cancel-btn {
  flex: 1;
  background: #f3f4f6;
  border: none;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
}

.issue-btn {
  flex: 1;
  background: #a855f7;
  color: white;
  border: none;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
}

/* SUCCESS MODAL */
.success-card {
  background: #fff;
  width: 420px;
  border-radius: 16px;
  padding: 28px;
  text-align: center;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 16px;
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}

.success-icon {
  width: 48px;
  height: 48px;
  background: #22c55e;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  font-size: 22px;
}

.success-text {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.success-btn {
  width: 100%;
  background: #facc15;
  border: none;
  border-radius: 10px;
  padding: 12px;
  font-weight: 600;
  cursor: pointer;
}
</style>
