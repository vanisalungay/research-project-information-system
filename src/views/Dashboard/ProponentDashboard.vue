<template>
  <div class="content-wrapper">
    <section class="welcome-section">
      <div class="welcome-left">
        <h1>Welcome, Xarzha Bagares!</h1>
        <p>Track your research proposals and manage submissions</p>

        <div class="stats-row">
          <div class="stat-box">
            <span class="stat-label">Active Proposals</span>
            <span class="stat-value">6</span>
          </div>

          <div class="stat-box pending">
            <span class="stat-label">Pending Revisions</span>
            <span class="stat-value">2</span>
          </div>

          <div class="stat-box approved">
            <span class="stat-label">Approved</span>
            <span class="stat-value">2</span>
          </div>
        </div>
      </div>
    </section>

    <!-- SEARCH -->
    <div class="search-container">
      <input
        type="text"
        placeholder="Search your proposals by title, ID, or status..."
      />
    </div>

    <!-- REVISION DEADLINE -->
    <section class="deadline-card">
      <div class="deadline-left">
        <div class="deadline-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#000" stroke-width="2"/>
            <path d="M12 6v6l4 2" stroke="#000" stroke-width="2"/>
          </svg>
        </div>

        <div>
          <div class="deadline-title">
            Revision Deadlines
            <span class="pill">2 Pending</span>
          </div>
          <div class="proposal-name">
            Community-Based Marine Resource Management
          </div>
          <div class="proposal-id">P-2024-003</div>
        </div>
      </div>

      <div class="deadline-right">
        <div class="deadline-info">
          <span>Deadline</span>
          <strong>5 days left</strong>
        </div>
        <button class="submit-btn" @click="goToRevision">
  Submit Revision
</button>
      </div>
    </section>

    <section class="grid">
      <!-- RECENTLY SUBMITTED -->
      <div class="panel">
        <div class="panel-header">
  <div>
    Recently Submitted Proposals
    <span class="count-badge">6</span>
  </div>
  <a @click="goToProposals" style="cursor: pointer;">View All</a>
</div>


        <div class="panel-item">
          <div>
            <div class="item-title">Sustainable Aquaculture Practices</div>
            <div class="item-meta">P-2024-001 • 2024-11-20</div>
          </div>
          <span class="status under-review">Under Review</span>
        </div>

        <div class="panel-item">
          <div>
            <div class="item-title">AI-Driven Crop Disease Detection</div>
            <div class="item-meta">P-2024-002 • 2024-11-18</div>
          </div>
          <span class="status approved">Approved</span>
        </div>

        <div class="panel-item">
          <div>
            <div class="item-title">
              Community-Based Marine Resource Management
            </div>
            <div class="item-meta">P-2024-003 • 2024-11-22</div>
          </div>
          <span class="status revision">Revision Required</span>
        </div>
      </div>

      <!-- REVIEWER FEEDBACK -->
      <div class="panel">
        <div class="panel-header">
          <div>
            Reviewer Feedback
            <span class="count-badge yellow">3</span>
          </div>
          <a @click="goToFeedback">View All</a>
        </div>

        <div class="feedback-item">
          <span class="dot yellow"></span>
          <div>
            <div>Your proposal P-2024-003 requires revision</div>
            <small>3 hours ago</small>
          </div>
        </div>

        <div class="feedback-item">
          <span class="dot green"></span>
          <div>
            <div>Proposal P-2024-002 has been approved!</div>
            <small>1 day ago</small>
          </div>
        </div>

        <div class="feedback-item">
          <span class="dot yellow"></span>
          <div>
            <div>
              Revision deadline for P-2024-004 is approaching (5 days left)
            </div>
            <small>2 days ago</small>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
const router = useRouter()

function goToRevision() {
  router.push('/revisions')
}

function goToProposals() {
  router.push('/proposals') 
}

function goToFeedback() {
  router.push('/proponent-feedback')
}

</script>

<style scoped>
/* BASE */
.content-wrapper {
  padding: 2rem;
  background: #f3f4f6;
  min-height: 100vh;
  font-family: "Inter", sans-serif;
}

/* WELCOME */
.welcome-section {
  background: linear-gradient(90deg, #2b294a 0%, #3a375f 100%);
  border-radius: 0.875rem;
  padding: 2rem;
  color: #fff;
}

.welcome-left h1 {
  margin: 0;
  font-size: 1.375rem;
  font-weight: 600;
}

.welcome-left p {
  margin: 0.375rem 0 1rem;
  font-size: 0.875rem;
  opacity: 0.85;
}

.stats-row {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  justify-content: center;
  align-items: center;
}

.stat-box {
  background: rgba(255, 255, 255, 0.12);
  padding: 0.875rem 1.25rem;
  border-radius: 0.75rem;
  flex: 1 1 200px;
  min-width: 200px;
  text-align: center;
}

.stat-label {
  font-size: 0.8125rem;
  opacity: 0.9;
}

.stat-value {
  display: block;
  margin-top: 0.375rem;
  font-size: 1.375rem;
  font-weight: 700;
}

.stat-box.pending {
  background: rgba(255,193,7,0.25);
  color: white;
}

.stat-box.approved {
  background: rgba(40,167,69,0.25);
}

/* SEARCH */
.search-container {
  margin: 1.25rem 0;
}

.search-container input {
  width: 100%;
  padding: 0.875rem 1rem;
  border-radius: 0.75rem;
  border: 1px solid #e5e7eb;
  font-size: 0.875rem;
}

/* DEADLINE */
.deadline-card {
  background: #fffbe6;
  border: 2px solid #ffd200;
  border-radius: 0.875rem;
  padding: 1rem 1.25rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 1rem;
}

.deadline-left {
  display: flex;
  gap: 1rem;
  align-items: center;
  flex: 1 1 250px;
}

.deadline-icon {
  width: 2.5rem;
  height: 2.5rem;
  background: #ffd200;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
}

.deadline-icon svg {
  width: 50%;
  height: 50%;
}

.deadline-info {
  display: flex;
  flex-direction: column;
  text-align: right;
}

.deadline-title {
  font-weight: 600;
  font-size: 0.875rem;
}

.pill {
  background: #ffd200;
  padding: 0.125rem 0.625rem;
  border-radius: 1rem;
  font-size: 0.75rem;
  margin-left: 0.375rem;
}

.proposal-name {
  font-size: 0.875rem;
  margin-top: 0.375rem;
}

.proposal-id {
  font-size: 0.75rem;
  color: #6b7280;
}

.deadline-right {
  display: flex;
  align-items: center;
  gap: 1.25rem;
  flex: 1 1 150px;
  justify-content: flex-end;
}

.deadline-info span {
  display: block;
  font-size: 0.75rem;
  color: #6b7280;
}

.submit-btn {
  background: #ffd200;
  border: none;
  padding: 0.625rem 1.125rem;
  border-radius: 1rem;
  font-weight: 600;
  cursor: pointer;
}

/* GRID */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.25rem;
  margin-top: 1.375rem;
}

/* PANELS */
.panel {
  background: #fff;
  border-radius: 0.875rem;
  padding: 1rem;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
  font-size: 0.875rem;
  margin-bottom: 0.625rem;
}

.panel-header a {
  font-size: 0.75rem;
  color: #6b7280;
  cursor: pointer;
}

.count-badge {
  background: #e5e7eb;
  padding: 0.125rem 0.5rem;
  border-radius: 0.75rem;
  font-size: 0.75rem;
  margin-left: 0.375rem;
}

.count-badge.yellow {
  background: #ffd200;
}

/* ITEMS */
.panel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f1f1;
}

.panel-item:last-child {
  border-bottom: none;
}

.item-title {
  font-size: 0.875rem;
  font-weight: 500;
}

.item-meta {
  font-size: 0.75rem;
  color: #6b7280;
}

/* STATUS */
.status {
  font-size: 0.75rem;
  padding: 0.25rem 0.625rem;
  border-radius: 1rem;
}

.status.under-review {
  background: #e5e7eb;
  color: black;
  font-weight: lighter;
}

.status.approved {
  background: #d1fae5;
  color: black;
  font-weight: lighter;
}

.status.revision {
  background: #fef3c7;
  color: black;
  font-weight: lighter;
}

/* FEEDBACK */
.feedback-item {
  display: flex;
  gap: 0.625rem;
  padding: 0.75rem 0;
  border-bottom: 1px solid #f1f1f1;
}

.feedback-item:last-child {
  border-bottom: none;
}

.dot {
  width: 0.625rem;
  height: 0.625rem;
  border-radius: 50%;
  margin-top: 0.375rem;
}

.dot.yellow {
  background: #ffd200;
}

.dot.green {
  background: #22c55e;
}
</style>
