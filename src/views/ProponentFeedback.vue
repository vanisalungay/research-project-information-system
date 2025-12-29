<template>
  <div class="content-wrapper">
    <!-- Page Title -->
    <h2 class="page-title">All Reviewer Feedback</h2>
    <p class="page-subtitle">
      Comprehensive view of all feedback received from reviewers across all your proposals
    </p>

    <!-- Stats Cards -->
    <div class="stats-grid">
      <div class="stat-card dark">
        <h3>5</h3>
        <p>Total Reviews</p>
      </div>

      <div class="stat-card green">
        <h3>2</h3>
        <p>Approved</p>
      </div>

      <div class="stat-card yellow">
        <h3>1</h3>
        <p>Revisions Requested</p>
      </div>

      <div class="stat-card blue">
        <h3>5</h3>
        <p>Proposals Reviewed</p>
      </div>
    </div>

    <!-- Proposal Card -->
    <div class="proposal-card">
      <div class="proposal-header">
        <div>
          <h3>Sustainable Aquaculture Practices in Mindanao Coastal Areas</h3>
          <span class="proposal-id">ID: P-2024-001</span>
          <span class="badge under-review">UNDER REVIEW</span>
        </div>
        <button class="btn-view">View Proposal</button>
      </div>

      <div class="reviewer-section">
        <div class="reviewer-header">
          <div>
            <strong>Dr. Juan Reyes</strong>
            <p class="role">Technical Reviewer</p>
          </div>
          <div class="review-status approved">Approved with minor revisions</div>
        </div>

        <div class="feedback-box">
          <strong>Feedback Summary</strong>
          <p>
            Strong research design with clear objectives. Methodology is sound and feasible within
            the proposed timeline.
          </p>
        </div>

        <button class="btn-download">Download Full Evaluation Report</button>
      </div>
    </div>

    <!-- Second Proposal -->
    <div class="proposal-card">
      <div class="proposal-header">
        <div>
          <h3>AI-Driven Crop Disease Detection System</h3>
          <span class="proposal-id">ID: P-2024-002</span>
          <span class="badge approved">APPROVED</span>
        </div>
        <button class="btn-view">View Proposal</button>
      </div>

      <div class="reviewer-section">
        <div class="reviewer-header">
          <div>
            <strong>Dr. Elena Torres</strong>
            <p class="role">Technical Reviewer</p>
          </div>
          <div class="review-status recommended">Highly Recommended for Approval</div>
        </div>

        <div class="feedback-box">
          <strong>Feedback Summary</strong>
          <p>
            Excellent proposal with innovative approach. Strong technical foundation and clear
            implementation plan.
          </p>
        </div>

        <button class="btn-download">Download Full Evaluation Report</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ProponentFeedback',
}
</script>

<style scoped>
.content-wrapper {
  padding: 24px;
  background: #f5f6fa;
  min-height: 100vh;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 4px;
}

.page-subtitle {
  color: #6b7280;
  margin-bottom: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  border-radius: 10px;
  padding: 18px;
  color: #fff;
}

.stat-card h3 {
  font-size: 28px;
  margin: 0;
}

.stat-card p {
  margin: 4px 0 0;
  font-size: 14px;
}

.stat-card.dark {
  background: #1f2937;
}

.stat-card.green {
  background: #16a34a;
}

.stat-card.yellow {
  background: #facc15;
}

.stat-card.blue {
  background: #a5b4fc;
}

.proposal-card {
  background: #fff;
  border-radius: 12px;
  margin-bottom: 20px;
  overflow: hidden;
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #111827;
  color: #fff;
}

.proposal-header h3 {
  margin: 0 0 6px;
  font-size: 16px;
}

.proposal-id {
  font-size: 12px;
  color: #9ca3af;
  margin-right: 8px;
}

.badge {
  font-size: 11px;
  padding: 4px 8px;
  border-radius: 999px;
  font-weight: 600;
}

.badge.under-review {
  background: #374151;
}

.badge.approved {
  background: #22c55e;
  color: #064e3b;
}

.btn-view {
  background: #1f2937;
  color: #fff;
  border: 1px solid #374151;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.btn-download {
  margin-top: 12px;
  background: #111827;
  color: #fff;
  border: none;
  padding: 10px 14px;
  border-radius: 8px;
  cursor: pointer;
}

.reviewer-section {
  padding: 16px;
}

.reviewer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.role {
  font-size: 12px;
  color: #6b7280;
  margin: 2px 0 0;
}

.review-status {
  font-size: 12px;
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 600;
}

.review-status.approved {
  background: #dcfce7;
  color: #166534;
}

.review-status.recommended {
  background: #bbf7d0;
  color: #14532d;
}

.feedback-box {
  margin-top: 12px;
  background: #f9fafb;
  padding: 12px;
  border-radius: 8px;
}

.feedback-box p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #374151;
}
</style>
