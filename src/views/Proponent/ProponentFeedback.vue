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
        <h3>{{ stats.total_reviews }}</h3>
        <p>Total Reviews</p>
      </div>

      <div class="stat-card green">
        <h3>{{ stats.approved_reviews }}</h3>
        <p>Approved</p>
      </div>

      <div class="stat-card yellow">
        <h3>{{ stats.revision_reviews }}</h3>
        <p>Revisions Requested</p>
      </div>

      <div class="stat-card blue">
        <h3>{{ stats.reviewed_proposals }}</h3>
        <p>Proposals Reviewed</p>
      </div>
    </div>

    <!-- Proposal Cards -->
    <div v-if="feedbacks.length">
      <div
        class="proposal-card"
        v-for="proposal in feedbacks"
        :key="proposal.id"
      >
        <div class="proposal-header">
          <div>
            <h3>{{ proposal.project_title }}</h3>

            <span class="proposal-id">
              ID: {{ proposal.proposal_id }}
            </span>

            <span
              class="badge"
              :class="proposal.proposal_status_class"
            >
              {{ proposal.proposal_status }}
            </span>
          </div>

          <button class="btn-view">
            View Proposal
          </button>
        </div>

        <div
          class="reviewer-section"
          v-for="review in proposal.reviews"
          :key="review.id"
        >
          <div class="reviewer-header">
            <div>
              <strong>{{ review.reviewer_name }}</strong>
              <p class="role">
                {{ review.reviewer_role }}
              </p>
            </div>

            <div
              class="review-status"
              :class="review.status_class"
            >
              {{ review.review_status }}
            </div>
          </div>

          <div class="feedback-box">
            <strong>Feedback Summary</strong>

            <p>
              {{ review.feedback_summary }}
            </p>
          </div>

          <button class="btn-download">
            Download Full Evaluation Report
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div
      v-else
      class="proposal-card empty-card"
    >
      <div class="proposal-header">
        <div>
          <h3>No Reviewer Feedback</h3>
          <span class="proposal-id">
            There are currently no reviewed proposals.
          </span>
        </div>
      </div>

      <div class="reviewer-section empty-state">
        <h4>No reviewer feedback available</h4>

        <p>
          You don't have any reviewed proposals yet. Once reviewers evaluate your submitted proposals,
          their feedback and evaluation reports will appear here.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const loading = ref(false)

const stats = ref({
  total_reviews: 0,
  approved_reviews: 0,
  revision_reviews: 0,
  reviewed_proposals: 0,
})

const feedbacks = ref([])

async function fetchFeedbacks() {
  loading.value = true

  try {
    // API call goes here later
    // Example:
    // const response = await axios.get(...)
    //
    // stats.value = response.data.stats
    // feedbacks.value = response.data.feedbacks

  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchFeedbacks()
})
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

.empty-state {
  text-align: center;
  padding: 40px 20px;
}

.empty-state h4 {
  margin: 0 0 10px;
  color: #374151;
  font-size: 18px;
}

.empty-state p {
  margin: 0;
  color: #6b7280;
  line-height: 1.6;
  max-width: 500px;
  margin-inline: auto;
}
</style>