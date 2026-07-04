<template>
  <div class="dashboard-wrapper animate-fade">
    <!-- HEADER -->
    <div class="header">
      <div class="header-title">
        <div class="header-main">OVCAF DASHBOARD</div>
        <div class="header-sub">Office of the Vice Chancellor for Admin and Finance</div>
      </div>
    </div>

    <!-- STATS METRICS ROW -->
    <div class="stats-container">
      <div class="stat-card">
        <div class="stat-title">ENDORSED PROPOSALS</div>
        <div class="stat-number">{{ stats.endorsed }}</div>
      </div>

      <div class="stat-card">
        <div class="stat-title">PENDING RELEASE EVALUATIONS</div>
        <div class="stat-number">{{ stats.pending }}</div>
      </div>

      <div class="stat-card">
        <div class="stat-title">COMPLETED REVIEWS & RELEASED</div>
        <div class="stat-number">{{ stats.completed }}</div>
      </div>
    </div>

    <!-- PROPOSALS TABLE -->
    <div class="table-title">APPROVED PROPOSALS REQUESTING FUNDS</div>

    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading funding entries...</p>
    </div>

    <div v-else-if="proposals.length > 0" class="table-wrapper">
      <div class="table-header">
        <div class="th">Proposal Title</div>
        <div class="th">Proponent Name</div>
        <div class="th">Amount Requested</div>
        <div class="th">Action</div>
      </div>

      <div class="table-row" v-for="proposal in proposals" :key="proposal.id">
        <div class="td">{{ proposal.title }}</div>
        <div class="td">{{ proposal.proponent }}</div>
        <div class="td">{{ proposal.amount }}</div>
        <div class="td actions-cell">
          <div class="validate-btn" @click="$router.push('ovcaf-validate')">Validate</div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <p>No proposals currently requesting funding validation.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '@/utils/api'

const proposals = ref([])
const stats = ref({
  endorsed: 0,
  pending: 0,
  completed: 0
})
const isLoading = ref(true)

const loadDashboard = async () => {
  try {
    isLoading.value = true
    const response = await api.get('/api/proposals')
    const data = response.data || []

    // Calculate dynamic stats
    stats.value.endorsed = data.filter(
      p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE'
    ).length

    stats.value.pending = data.filter(
      p => p.status === 'READY_FOR_RELEASE'
    ).length

    stats.value.completed = data.filter(
      p => p.status === 'RELEASED'
    ).length

    // Filter proposals to show only approved or ready for release
    proposals.value = data
      .filter(p => p.status === 'APPROVED' || p.status === 'READY_FOR_RELEASE' || p.status === 'RELEASED')
      .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Project',
        proponent: p.projectLeader || 'Unknown Proponent',
        amount: p.budget ? '₱ ' + p.budget.toLocaleString() : 'N/A'
      }))

  } catch (error) {
    console.error('Failed to load OVCAF metrics:', error)
  } finally {
    isLoading.value = false
  }
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-wrapper {
  font-family: inherit;
  background: var(--color-background, #ffffff);
  padding: 20px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  box-sizing: border-box;
}

.header {
  background: #2c2b4f;
  padding: 30px;
  border-radius: 12px;
  margin-bottom: 24px;
}

.header-main {
  font-size: 32px;
  font-weight: bold;
  color: #ffc800;
  text-align: left;
}

.header-sub {
  font-size: 14px;
  color: #ffffff;
  margin-top: 5px;
  text-align: left;
}

.stats-container {
  display: flex;
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  border: 1px solid var(--color-border, #cbd5e1);
  background: var(--color-card-background, #ffffff);
  padding: 25px;
  flex: 1;
  text-align: center;
  border-radius: 10px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}

.stat-title {
  font-weight: bold;
  font-size: 12px;
  color: var(--color-text-soft, #64748b);
  letter-spacing: 0.05em;
}

.stat-number {
  font-size: 36px;
  font-weight: bold;
  margin-top: 10px;
  color: var(--color-heading, #0f172a);
}

.table-title {
  font-weight: bold;
  font-size: 1.125rem;
  text-align: left;
  margin-bottom: 14px;
  color: var(--color-heading, #1e293b);
}

.table-wrapper {
  border: 1px solid var(--color-border, #cbd5e1);
  border-radius: 8px;
  overflow: hidden;
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 2fr 2fr 1.5fr 1fr;
  align-items: center;
}

.table-header {
  background: #2c2b4f;
  color: white;
  font-weight: bold;
}

.th,
.td {
  padding: 14px 16px;
  text-align: left;
  font-size: 14px;
}

.td {
  color: var(--color-text, #1f2937);
  border-bottom: 1px solid var(--color-border, #e2e8f0);
}

.table-row:last-child .td {
  border-bottom: none;
}

.actions-cell {
  display: flex;
  justify-content: flex-start;
  padding-left: 16px;
}

.validate-btn {
  background: #2452ff;
  color: white !important;
  padding: 6px 16px;
  text-align: center;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
  font-weight: 600;
  transition: background 0.15s ease;
}

.validate-btn:hover {
  background: #1d40cc;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 40px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid rgba(36, 82, 255, 0.2);
  border-top-color: #2452ff;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.empty-state {
  padding: 40px;
  background: var(--color-background-soft, #f8fafc);
  border: 1px dashed var(--color-border, #cbd5e1);
  border-radius: 8px;
  color: var(--color-text-soft, #64748b);
  font-size: 14px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-fade {
  animation: fadeIn 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>
