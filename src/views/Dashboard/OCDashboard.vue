<template>
  <div class="dashboard-content animate-fade">
    
    <!-- HEADER -->
    <div class="dashboard-header">
      <h2>CHANCELLOR DASHBOARD</h2>
      <p>Office of the Chancellor - Final Executive Review & Special Orders</p>
    </div>

    <!-- METRICS CARDS -->
    <div class="summary-cards">
      <div class="card bg-yellow">
        <p class="card-title">Pending Final Approval</p>
        <h2>{{ stats.pending }}</h2>
      </div>

      <div class="card bg-green">
        <p class="card-title">Approved Projects</p>
        <h2>{{ stats.approved }}</h2>
      </div>

      <div class="card bg-blue">
        <p class="card-title">Special Orders Issued</p>
        <h2>{{ stats.specialOrders }}</h2>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading chancellor records...</p>
    </div>

    <div v-else class="sections-container">
      <!-- SECTION 1: Awaiting Final Executive Sign-off -->
      <div class="section">
        <h3>Proposals for Final Approval</h3>

        <div v-if="forFinalApproval.length > 0" class="cards-list">
          <div class="proposal-card" v-for="proposal in forFinalApproval" :key="proposal.id">
            <div class="proposal-header">
              <h4>{{ proposal.title }}</h4>
              <span class="status endorsed">Endorsed</span>
            </div>

            <p class="tag">{{ proposal.category }}</p>
            <p class="meta">Leader: {{ proposal.adviser }} • Submitted: {{ proposal.date }}</p>

            <button class="action-btn" @click="reviewProposal(proposal.id)">Review & Approve</button>
          </div>
        </div>

        <div v-else class="empty-banner">
          <p>No proposals currently awaiting final executive sign-off.</p>
        </div>
      </div>

      <!-- SECTION 2: Approved Projects -->
      <div class="section">
        <h3>Approved Proposals</h3>

        <div v-if="approvedProposals.length > 0" class="cards-list">
          <div class="proposal-card border-left-green" v-for="proposal in approvedProposals" :key="proposal.id">
            <div class="proposal-header">
              <h4>{{ proposal.title }}</h4>
              <span class="status approved">Approved</span>
            </div>

            <p class="tag">{{ proposal.category }}</p>
            <p class="meta">Leader: {{ proposal.adviser }} • {{ proposal.date }}</p>
          </div>
        </div>

        <div v-else class="empty-state-banner">
          <p>Approved proposals will be listed here once orders are finalized.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()

const stats = ref({
  pending: 0,
  approved: 0,
  specialOrders: 0
})

const forFinalApproval = ref([])
const approvedProposals = ref([])
const isLoading = ref(true)

const loadChancellorDashboard = async () => {
  try {
    isLoading.value = true
    const response = await api.get('/api/proposals')
    const data = response.data || []

    // Calculate executive statistics dynamically
    stats.value.pending = data.filter(p => p.status === 'REC_APPROVED' || p.status === 'FOR_OC_APPROVAL' || p.status === 'OVC_APPROVED').length
    stats.value.approved = data.filter(p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE' || p.status === 'RELEASED').length
    stats.value.specialOrders = data.filter(p => p.status === 'RELEASED').length

    // Map proposals awaiting OC final action
    forFinalApproval.value = data
      .filter(p => p.status === 'REC_APPROVED' || p.status === 'FOR_OC_APPROVAL' || p.status === 'OVC_APPROVED')
      .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Proposal',
        category: p.fundingProgram || 'Research Grant',
        adviser: p.projectLeader || 'Unknown Proponent',
        date: p.createdAt ? new Date(p.createdAt).toLocaleDateString() : 'N/A'
      }))

    // Map already approved proposals
    approvedProposals.value = data
      .filter(p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE' || p.status === 'RELEASED')
      .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Proposal',
        category: p.fundingProgram || 'Research Grant',
        adviser: p.projectLeader || 'Unknown Proponent',
        date: p.createdAt ? new Date(p.createdAt).toLocaleDateString() : 'N/A'
      }))

  } catch (error) {
    console.error("Failed to compile Chancellor statistics:", error)
  } finally {
    isLoading.value = false
  }
}

const reviewProposal = (id) => {
  router.push(`/oc/final-approval/${id}`)
}

onMounted(loadChancellorDashboard)
</script>

<style scoped>
.dashboard-content {
  padding: 24px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  border-bottom: 1px solid var(--color-border, #e2e8f0);
  padding-bottom: 14px;
  margin-bottom: 24px;
  text-align: left;
}

.dashboard-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin: 0 0 4px 0;
}

.dashboard-header p {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.card {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
  text-align: left;
}

.card-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-soft, #64748b);
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0;
}

.card h2 {
  margin-top: 8px;
  font-size: 2rem;
  font-weight: 700;
  color: var(--color-heading, #0f172a);
  margin-bottom: 0;
}

/* Background accents */
.bg-yellow { border-top: 4px solid #f59e0b; }
.bg-green { border-top: 4px solid #10b981; }
.bg-blue { border-top: 4px solid #3b82f6; }

.sections-container {
  display: flex;
  flex-direction: column;
  gap: 32px;
  text-align: left;
}

.section h3 {
  font-size: 1.125rem;
  font-weight: 700;
  color: var(--color-heading, #1e293b);
  margin: 0 0 16px 0;
  border-left: 4px solid #2452ff;
  padding-left: 12px;
}

.cards-list,
.cards-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.proposal-card {
  background: var(--color-card-background, #ffffff);
  border: 1px solid var(--color-border, #e2e8f0);
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.01);
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-left: 4px solid #f59e0b;
}

.border-left-green {
  border-left-color: #10b981 !important;
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.proposal-header h4 {
  font-size: 1rem;
  font-weight: 600;
  color: var(--color-heading, #0f172a);
  margin: 0;
}

.status {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 12px;
}

.status.endorsed {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
}

.status.approved {
  background: rgba(16, 185, 129, 0.1);
  color: #10b981;
}

.tag {
  font-size: 0.75rem;
  background: var(--color-background-soft, #f1f5f9);
  color: var(--color-text-soft, #475569);
  padding: 4px 10px;
  border-radius: 4px;
  width: fit-content;
  margin: 0;
  font-weight: 500;
}

.meta {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

.action-btn {
  align-self: flex-start;
  background: #2452ff;
  border: 1px solid #2452ff;
  color: white !important;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s ease;
  margin-top: 4px;
}

.action-btn:hover {
  background: #1d40cc;
  border-color: #1d40cc;
}

.loading-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 60px;
  gap: 12px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(36, 82, 255, 0.2);
  border-top-color: #2452ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.empty-banner,
.empty-state-banner {
  padding: 24px;
  background: var(--color-background-soft, #f8fafc);
  border: 1px dashed var(--color-border, #cbd5e1);
  border-radius: 10px;
  color: var(--color-text-soft, #64748b);
  font-size: 0.875rem;
  text-align: center;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-fade {
  animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
}
</style>
