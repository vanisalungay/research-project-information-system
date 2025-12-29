<template>
  <div class="proposal-page">
    <div class="top-grid">
      <!-- Proposal Information -->
      <div class="card">
        <h2 class="title">Community Water and Filtration Enhancement Program 2024</h2>
        <p class="subtitle">Proposal ID #1</p>

        <div class="info-grid">
          <div>
            <span class="label">Category</span>
            <span class="value">Kalikasan Program</span>
          </div>
          <div>
            <span class="label">Total Budget</span>
            <span class="value">₱150,000</span>
          </div>
          <div>
            <span class="label">Duration</span>
            <span class="value">12 months</span>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-edit" @click="editProposal">Edit Proposal</button>

          <button class="btn btn-save" @click="viewDetails">See Detailed Proposal</button>

          <button class="btn btn-download" @click="downloadPdf">Download PDF</button>
        </div>
      </div>

      <!-- Assigned Reviewer -->
      <div class="card reviewer-card">
        <span class="label">Assigned Reviewer</span>

        <div class="reviewer">
          <div class="avatar">SR</div>
          <div>
            <strong>Dr. Sarah Johnson</strong>
            <p>Senior Evaluator</p>
          </div>
        </div>

        <button class="btn btn-contact full">Contact Reviewer</button>
      </div>
    </div>

    <!-- WORKFLOW + ATTACHMENTS -->
    <div class="mid-grid">
      <!-- WORKFLOW TIMELINE -->
      <div class="card">
        <h3 class="section-title">Workflow Timeline</h3>

        <div class="workflow">
          <div class="wf-item success">
            <div class="wf-header">
              <span>Submission</span>
              <span class="wf-date">2024-12-10</span>
            </div>
            <p class="wf-meta">By You</p>
            <p>"Proposal successfully submitted"</p>
          </div>

          <div class="wf-arrow">↓</div>

          <div class="wf-item success">
            <div class="wf-header">
              <span>RII Review</span>
              <span class="wf-date">2024-12-11</span>
            </div>
            <p class="wf-meta">By Dr. Maria Santos (RII)</p>
            <p>"Endorsed by RII – Project aligns with institutional research priorities"</p>
          </div>

          <div class="wf-arrow">↓</div>

          <div class="wf-item success">
            <div class="wf-header">
              <span>Sent to OVCGRE</span>
              <span class="wf-date">2024-12-11</span>
            </div>
            <p class="wf-meta">By RII Administrator</p>
            <p>"Forwarded to OVCGRE for final approval"</p>
          </div>

          <div class="wf-arrow">↓</div>

          <div class="wf-item current">
            <div class="wf-header">
              <span>OVCGRE Review</span>
            </div>
            <p>"Currently under OVCGRE review"</p>
            <span class="status-pill">Current Stage</span>
          </div>

          <div class="wf-arrow">↓</div>

          <div class="wf-item pending">
            <div class="wf-header">
              <span>Final Approval</span>
            </div>
          </div>
        </div>
      </div>

      <!-- ATTACHMENTS -->
      <div class="card">
        <h3 class="section-title">Attachments</h3>

        <ul class="files">
          <li>📄 proposal.pdf</li>
          <li>📊 budget_breakdown.xlsx</li>
          <li>📄 timeline.pdf</li>
          <li>🖼 water-enhancement.jpg</li>
        </ul>
      </div>
    </div>

    <!-- ACTIVITY LOG -->
    <div class="card">
      <h3 class="section-title">Activity Log</h3>

      <div class="activity-log">
        <div class="log-item blue">
          <div class="log-header">
            <strong>Proposal submitted</strong>
            <span>2024-12-10 08:30 AM</span>
          </div>
          <p>Complete proposal package submitted successfully</p>
        </div>

        <div class="log-item sky">
          <div class="log-header">
            <strong>Received by RII</strong>
            <span>2024-12-10 09:45 AM</span>
          </div>
          <p>Proposal routed to RII for evaluation</p>
        </div>

        <div class="log-item purple">
          <div class="log-header">
            <strong>RII Review Completed</strong>
            <span>2024-12-11 01:15 PM</span>
          </div>
          <p>Review completed by Dr. Maria Santos</p>
        </div>

        <div class="log-item green">
          <div class="log-header">
            <strong>Endorsed by RII</strong>
            <span>2024-12-11 02:10 PM</span>
          </div>
          <p>
            Project aligns with institutional research priorities. Budget is reasonable and
            methodology is sound.
          </p>
        </div>

        <div class="log-item sky">
          <div class="log-header">
            <strong>Transferred to OVCGRE</strong>
            <span>2024-12-11 03:20 PM</span>
          </div>
          <p>Your proposal has been forwarded to OVCGRE for final review and approval</p>
        </div>

        <div class="log-item lavender">
          <div class="log-header">
            <strong>Currently Under OVCGRE Review</strong>
            <span>2024-12-11 03:21 PM</span>
          </div>
          <p>Pending OVCGRE administrative action</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const editProposal = () => {
  router.push('/submit-revision')
}

const viewDetails = () => {
  router.push('/detailed-proposal')
}

const downloadPdf = () => {
  const link = document.createElement('a')
  link.href = '/files/proposal.pdf'
  link.download = 'proposal.pdf'
  link.click()
}
</script>

<style scoped>
.proposal-page {
  padding: 24px;
  background: #f2f2f2;
}

/* GRID */
.top-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
}

.mid-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

/* CARD */
.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

/* TEXT */
.title {
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  color: #888;
  margin-bottom: 16px;
}

.label {
  font-size: 12px;
  color: #888;
}

.value {
  display: block;
  font-weight: 600;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

/* INFO */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

/* BUTTONS */
.actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  transition: all 0.2s ease;
}

.btn-edit {
  color: #1976d2;
  border: 1px solid #90caf9;
}

.btn-edit:hover {
  background: #e3f2fd;
}

.btn-save {
  background: #fdd835;
  border: 1px solid #fbc02d;
  color: #000;
}

.btn-save:hover {
  background: #fbc02d;
}

.btn-download {
  color: #555;
  border: 1px solid #ccc;
}

.btn-download:hover {
  background: #f5f5f5;
}

/* CONTACT REVIEWER BUTTON */
.btn-contact {
  background: #1e88e5;
  color: #fff;
  border: 1px solid #1e88e5;
  font-weight: 500;
}

.btn-contact:hover {
  background: #1565c0;
  border-color: #1565c0;
}

/* REVIEWER */
.reviewer {
  display: flex;
  gap: 12px;
  margin: 12px 0 20px;
  align-items: center;
}

.avatar {
  width: 40px;
  height: 40px;
  background: #673ab7;
  color: #fff;
  border-radius: 50%;
  display: grid;
  place-items: center;
  font-weight: bold;
}

/* WORKFLOW */
.workflow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wf-item {
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
}

.wf-header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}

.wf-date {
  font-size: 12px;
  color: #666;
}

.wf-meta {
  font-size: 12px;
  color: #666;
}

.wf-arrow {
  text-align: center;
  color: #bbb;
  font-size: 18px;
}

.success {
  background: #e9fff1;
  border: 1px solid #9be7b2;
}

.current {
  background: #fff4cc;
  border: 1px solid #ffca28;
}

.pending {
  background: #f5f5f5;
  border: 1px solid #ddd;
}

.status-pill {
  margin-top: 8px;
  display: inline-block;
  background: #ffca28;
  padding: 4px 10px;
  font-size: 11px;
  border-radius: 12px;
  font-weight: 600;
}

/* FILES */
.files {
  list-style: none;
  padding: 0;
  margin: 0;
}

.files li {
  padding: 8px 0;
  font-size: 14px;
}

/* ACTIVITY LOG */
.activity-log {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-item {
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.log-header span {
  font-size: 11px;
  color: #555;
}

.log-item {
  background: #e8f5e9;
  border: 1px solid #a5d6a7;
}
</style>
