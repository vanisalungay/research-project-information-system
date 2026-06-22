<template>
  <div class="page">
    <div class="back" @click="$router.go(-1)">← Back to Dashboard</div>

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
      <button class="view-btn">View Details</button>
    </div>

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

    <!-- EXECUTIVE SUMMARY -->
    <div class="card">
      <h3>Executive Summary</h3>
      <p class="summary-text" style="text-align: justify">
        This research proposal aims to develop sustainable aquaculture systems in coastal
        communities, addressing both environmental conservation and livelihood improvement. The
        study will implement and evaluate integrated multi-trophic aquaculture (IMTA) systems in
        three pilot sites.
      </p>

      <div class="summary-stats">
        <div>
          <span>Expected Duration</span>
          <p>12 months</p>
        </div>
        <div>
          <span>Beneficiaries</span>
          <p>300 coastal farmers</p>
        </div>
      </div>
    </div>

    <!-- CHANCELLOR NOTES -->
    <div class="card">
      <h3>Chancellor’s Notes</h3>
      <textarea placeholder="Add your notes and comments..."></textarea>
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
          This proposal will be approved for implementation. You can upload the Special Order
          separately.
        </p>
      </div>

      <div class="modal-actions">
        <button class="btn-cancel" @click="showConfirm = false">Cancel</button>
        <button class="btn-confirm" @click="confirmApproval">Confirm Approval</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'FinalApproval',
  data() {
    return {
      showConfirm: false,
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
    confirmApproval() {
      this.showConfirm = false

      const proposalId = this.$route.params.id

      this.$router.push({
        path: `/oc/final-approval/${proposalId}/approved`,
      })
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

/* VIEW BUTTON */
.view-btn {
  background: #f1f3f8;
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

/* STATUS */
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

/* MODAL */
.modal {
  width: 460px;
  background: #ffffff;
  border-radius: 14px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
  padding: 20px;
}

/* HEADER */
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
