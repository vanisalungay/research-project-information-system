<template>
  <div class="dashboard-content">
    <!-- Summary Cards -->
    <div class="summary-cards">
      <div class="card">
        <p class="card-title">Pending Final Approval</p>
        <h2>{{ stats.pending_final_approval }}</h2>
      </div>

      <div class="card">
        <p class="card-title">Approved This Month</p>
        <h2>{{ stats.approved_this_month }}</h2>
      </div>

      <div class="card">
        <p class="card-title">Special Orders Issued</p>
        <h2>{{ stats.special_orders_issued }}</h2>
      </div>
    </div>

    <!-- For Final Approval -->
    <div class="section">
      <h3>Proposals for Final Approval</h3>

      <div
        v-if="forFinalApproval.length"
      >
        <div
          class="proposal-card"
          v-for="proposal in forFinalApproval"
          :key="proposal.id"
        >
          <div class="proposal-header">
            <h4>{{ proposal.project_title }}</h4>

            <span class="status endorsed">
              {{ proposal.proposal_status }}
            </span>
          </div>

          <p class="tag">
            {{ proposal.research_category }}
          </p>

          <p class="meta">
            {{ proposal.project_leader }} • {{ proposal.submitted_at }}
          </p>

          <button
            class="action-btn"
            @click="reviewProposal(proposal.id)"
          >
            Review & Approve
          </button>
        </div>
      </div>

      <div
        v-else
        class="proposal-card"
      >
        No proposals awaiting final approval.
      </div>
    </div>

    <!-- Approved Proposals -->
    <div class="section">
      <h3>Approved Proposals</h3>

      <div
        v-if="approvedProposals.length"
      >
        <div
          class="proposal-card"
          v-for="proposal in approvedProposals"
          :key="proposal.id"
        >
          <div class="proposal-header">
            <h4>{{ proposal.project_title }}</h4>

            <span class="status approved">
              {{ proposal.proposal_status }}
            </span>
          </div>

          <p class="tag">
            {{ proposal.research_category }}
          </p>

          <p class="meta">
            {{ proposal.project_leader }} • {{ proposal.approved_at }}
          </p>
        </div>
      </div>

      <div
        v-else
        class="proposal-card"
      >
        No approved proposals available.
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)

const stats = ref({
  pending_final_approval: 0,
  approved_this_month: 0,
  special_orders_issued: 0,
})

const forFinalApproval = ref([])

const approvedProposals = ref([])

async function fetchDashboard() {
  loading.value = true

  try {
    // Backend API goes here later

    // Example:
    // const response = await axios.get('/api/oc/dashboard')
    //
    // stats.value = response.data.stats
    // forFinalApproval.value = response.data.for_final_approval
    // approvedProposals.value = response.data.approved_proposals

  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

function reviewProposal(id) {
  router.push(`/oc/final-approval/${id}`)
}

onMounted(() => {
  fetchDashboard()
})
</script>

<style scoped>
.dashboard-content {
  padding: 24px;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.card {
  background: #ffffff;
  padding: 16px;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 14px;
  color: #666;
}

.card h2 {
  margin-top: 8px;
  font-size: 28px;
  font-weight: bold;
}

.section {
  margin-bottom: 32px;
}

.section h3 {
  margin-bottom: 16px;
}

.proposal-card {
  background: #ffffff;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag {
  color: #0aa;
  font-size: 13px;
  margin: 4px 0;
}

.meta {
  font-size: 13px;
  color: #777;
}

.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status.endorsed {
  background: #efe3ff;
  color: #6a1b9a;
}

.status.approved {
  background: #e3f9ec;
  color: #2e7d32;
}

.action-btn {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(to right, #009688, #1976d2);
  color: #fff;
  cursor: pointer;
}
</style>