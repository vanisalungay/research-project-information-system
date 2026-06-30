<template>
  <div class="content-wrapper">
    <section class="welcome-section">
      <div class="welcome-left">
        <h1>
          Welcome,
          {{ user.first_name && user.last_name
              ? `${user.first_name} ${user.last_name}`
              : 'Proponent' }}!
        </h1>
        <p>Track your research proposals and manage submissions</p>

        <div class="stats-row">
          <div class="stat-box">
            <span class="stat-label">Active Proposals</span>
            <span class="stat-value">
              {{ statistics.activeProposals }}
            </span>
          </div>

          <div class="stat-box pending">
            <span class="stat-label">Pending Revisions</span>
            <span class="stat-value">
              {{ statistics.pendingRevisions }}
            </span>
          </div>

          <div class="stat-box approved">
            <span class="stat-label">Approved</span>
            <span class="stat-value">
              {{ statistics.approvedProposals }}
            </span>
          </div>
        </div>
      </div>
    </section>

    <!-- SEARCH -->
    <div class="search-container">
      <input
        v-model="search"
        type="text"
        placeholder="Search your proposals by title, ID, or status..."
      />
    </div>

    <!-- REVISION DEADLINE -->
    <section
      v-if="revisionDeadline"
      class="deadline-card"
    >
      <div class="deadline-left">
        <div class="deadline-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#000" stroke-width="2" />
            <path d="M12 6v6l4 2" stroke="#000" stroke-width="2" />
          </svg>
        </div>

        <div>
          <div class="deadline-title">
            Revision Deadlines
            <span class="pill">
              {{ revisionDeadline.pendingCount }} Pending
            </span>
          </div>

          <div class="proposal-name">
            {{ revisionDeadline.title }}
          </div>

          <div class="proposal-id">
            {{ revisionDeadline.proposalId }}
          </div>
        </div>
      </div>

      <div class="deadline-right">
        <div class="deadline-info">
          <span>Deadline</span>
          <strong>{{ revisionDeadline.daysLeft }} days left</strong>
        </div>

        <button
          class="submit-btn"
          @click="goToRevision"
        >
          Submit Revision
        </button>
      </div>
    </section>

    <!-- EMPTY STATE -->
    <section
      v-else
      class="deadline-card"
    >
      <div class="deadline-left">
        <div class="deadline-icon">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="10" stroke="#000" stroke-width="2" />
            <path d="M12 6v6l4 2" stroke="#000" stroke-width="2" />
          </svg>
        </div>

        <div>
          <div class="deadline-title">
            Revision Deadlines
          </div>

          <div class="proposal-name">
            No pending revisions.
          </div>

          <div class="proposal-id">
            You're all caught up!
          </div>
        </div>
      </div>
    </section>

    <!-- RECENTLY SUBMITTED -->
    <section class="grid">
      <div class="panel">
        <div class="panel-header">
          <div>
            Recently Submitted Proposals
            <span class="count-badge">
              {{ recentProposals.length }}
            </span>
          </div>

          <a
            @click="goToProposals"
            style="cursor: pointer"
          >
            View All
          </a>
        </div>

        <!-- No Proposals -->
        <div
          v-if="recentProposals.length === 0"
          class="panel-item"
        >
          <div>
            <div class="item-title">No submitted proposals yet.</div>
            <div class="item-meta">
              Your submitted proposals will appear here.
            </div>
          </div>
        </div>

        <!-- Proposal List -->
        <div
          v-for="proposal in recentProposals"
          :key="proposal.id"
          class="panel-item"
        >
          <div>
            <div class="item-title">
              {{ proposal.title }}
            </div>

            <div class="item-meta">
              {{ proposal.proposalId }} • {{ proposal.dateSubmitted }}
            </div>
          </div>

          <span
            class="status"
            :class="proposal.statusClass"
          >
            {{ proposal.status }}
          </span>
        </div>
      </div>
    </section>

    <!-- REVIEWER FEEDBACK -->
    <section class="grid">
      <div class="panel">
        <div class="panel-header">
          <div>
            Reviewer Feedback
            <span class="count-badge yellow">
              {{ feedbacks.length }}
            </span>
          </div>

          <a
            @click="goToFeedback"
            style="cursor: pointer"
          >
            View All
          </a>
        </div>

        <!-- Empty State -->
        <div
          v-if="feedbacks.length === 0"
          class="feedback-item"
        >
          <div>
            <div>No reviewer feedback yet.</div>
            <small>Feedback from reviewers will appear here.</small>
          </div>
        </div>

        <!-- Feedback List -->
        <div
          v-for="feedback in feedbacks"
          :key="feedback.id"
          class="feedback-item"
        >
          <span
            class="dot"
            :class="getFeedbackColor(feedback.type)"
          ></span>

          <div>
            <div>{{ feedback.message }}</div>
            <small>{{ feedback.time }}</small>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// Logged-in user
const user = ref({
  first_name: '',
  last_name: ''
})

// Dashboard statistics
const statistics = ref({
  activeProposals: 0,
  pendingRevisions: 0,
  approvedProposals: 0
})

// Search
const search = ref('')

// Revision deadline
const revisionDeadline = ref(null)

// Recently submitted proposals
const recentProposals = ref([])

// Reviewer feedback
const feedbacks = ref([])

// Loading
const loading = ref(false)

// Fetch dashboard data
async function fetchDashboard() {
  loading.value = true

  try {
    // API will be connected here later
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboard()
})

function goToRevision() {
  router.push('/revisions')
}

function goToProposals() {
  router.push('/proposals')
}

function goToFeedback() {
  router.push('/proponent-feedback')
}

function getFeedbackColor(type) {
  switch (type) {
    case 'revision':
      return 'yellow'

    case 'approved':
      return 'green'

    default:
      return 'yellow'
  }
}

function getStatusClass(status) {
  switch (status) {
    case 'Under Review':
      return 'under-review'

    case 'Approved':
      return 'approved'

    case 'Revision Required':
      return 'revision'

    default:
      return ''
  }
}
</script>

<style scoped>
:root {
  --bg-color: #f8f8f8;
  --card-bg: #ffffff;
  --text-color: #1f1a3d;
  --muted-text: #6b7280;
  --primary: #2452ff;
  --border: #ddd;
}

/* DARK MODE OVERRIDE */
@media (prefers-color-scheme: dark) {
  :root {
    --bg-color: #0f172a;
    --card-bg: #1e293b;
    --text-color: #f1f5f9;
    --muted-text: #94a3b8;
    --primary: #3b82f6;
    --border: #334155;
  }
}

/* BASE */
.content-wrapper {
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;

  padding: 2rem;
  background: #f3f4f6;
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
}

.welcome-section,
.search-container,
.deadline-card,
.grid {
  width: 100%;
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
  background: rgba(255, 193, 7, 0.25);
  color: white;
}

.stat-box.approved {
  background: rgba(40, 167, 69, 0.25);
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
