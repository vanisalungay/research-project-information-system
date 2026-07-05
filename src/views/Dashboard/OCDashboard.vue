<template>
  <div class="dashboard-content animate-fade">
    <!-- HEADER -->
    <div class="dashboard-header">
      <h2>CHANCELLOR DASHBOARD</h2>
      <p>Office of the Chancellor — Final Approval, Budget Determination & Special Orders (SO)</p>
    </div>

    <!-- METRICS CARDS -->
    <div class="summary-cards">
      <div class="card bg-yellow">
        <p class="card-title">Pending Final Approval</p>
        <h2>{{ stats.pending }}</h2>
      </div>
      <div class="card bg-green">
        <p class="card-title">Approved / Released</p>
        <h2>{{ stats.approved }}</h2>
      </div>
      <div class="card bg-blue">
        <p class="card-title">Sent to OVCAF (Budget)</p>
        <h2>{{ stats.sentToOvcaf }}</h2>
      </div>
    </div>

    <div v-if="isLoading" class="loading-state">
      <div class="loading-spinner"></div>
      <p>Loading proposals...</p>
    </div>

    <div v-else class="sections-container">
      <!-- SECTION 1: Awaiting Final Executive Sign-off -->
      <div class="section">
        <h3>Proposals for Final Approval</h3>

        <div v-if="forFinalApproval.length > 0" class="table-wrapper">
          <div class="table-header">
            <div class="th">Proposal Title</div>
            <div class="th">Proponent</div>
            <div class="th">Date Endorsed</div>
            <div class="th">Status</div>
            <div class="th">Action</div>
          </div>
          <div class="table-row" v-for="p in forFinalApproval" :key="p.id">
            <div class="td title-cell">{{ p.title }}</div>
            <div class="td">{{ p.adviser }}</div>
            <div class="td">{{ p.date }}</div>
            <div class="td"><span class="status-badge pending">{{ p.status }}</span></div>
            <div class="td">
              <button class="action-btn" @click="reviewProposal(p.id)">Review & Approve</button>
            </div>
          </div>
        </div>

        <div v-else class="empty-banner">
          <p>No proposals currently awaiting final executive sign-off.</p>
        </div>
      </div>

      <!-- SECTION 2: Approved / Sent to OVCAF -->
      <div class="section">
        <h3>Approved & Budget-Forwarded Proposals</h3>

        <div v-if="approvedProposals.length > 0" class="table-wrapper">
          <div class="table-header">
            <div class="th">Proposal Title</div>
            <div class="th">Proponent</div>
            <div class="th">Date</div>
            <div class="th">Status</div>
            <div class="th">Action</div>
          </div>
          <div class="table-row" v-for="p in approvedProposals" :key="p.id">
            <div class="td title-cell">{{ p.title }}</div>
            <div class="td">{{ p.adviser }}</div>
            <div class="td">{{ p.date }}</div>
            <div class="td">
              <span class="status-badge" :class="p.badgeClass">{{ p.status }}</span>
            </div>
            <div class="td">
              <button class="view-btn" @click="reviewProposal(p.id)">View</button>
            </div>
          </div>
        </div>

        <div v-else class="empty-banner">
          <p>Approved proposals and those forwarded to Finance/OVCAF for budget processing will appear here.</p>
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

const stats = ref({ pending: 0, approved: 0, sentToOvcaf: 0 })
const forFinalApproval = ref([])
const approvedProposals = ref([])
const isLoading = ref(true)

const loadDashboard = async () => {
  try {
    isLoading.value = true

    // Fetch proposals that are relevant to OC
    const [pendingRes, approvedRes] = await Promise.all([
      api.get('/api/proposals?statusIn=REC_APPROVED&statusIn=FOR_OC_APPROVAL&statusIn=OVC_APPROVED'),
      api.get('/api/proposals?statusIn=FOR_OVCAF_APPROVAL&statusIn=APPROVED&statusIn=READY_FOR_RELEASE&statusIn=RELEASED'),
    ])

    const pendingData = Array.isArray(pendingRes.data) ? pendingRes.data : []
    const approvedData = Array.isArray(approvedRes.data) ? approvedRes.data : []

    stats.value.pending = pendingData.length
    stats.value.sentToOvcaf = approvedData.filter(p => p.status === 'FOR_OVCAF_APPROVAL').length
    stats.value.approved = approvedData.length

    forFinalApproval.value = pendingData.map(p => ({
      id: p.id,
      title: p.projectTitle || 'Untitled Proposal',
      adviser: p.projectLeader || p.proponent?.name || 'Unknown',
      date: p.updatedAt?.substring(0, 10) || p.createdAt?.substring(0, 10) || 'N/A',
      status: p.status,
    }))

    approvedProposals.value = approvedData.map(p => ({
      id: p.id,
      title: p.projectTitle || 'Untitled Proposal',
      adviser: p.projectLeader || p.proponent?.name || 'Unknown',
      date: p.updatedAt?.substring(0, 10) || p.createdAt?.substring(0, 10) || 'N/A',
      status: p.status,
      badgeClass: p.status === 'FOR_OVCAF_APPROVAL' ? 'budget' : 'approved',
    }))
  } catch (error) {
    console.error('Failed to load Chancellor dashboard:', error)
  } finally {
    isLoading.value = false
  }
}

const reviewProposal = (id) => {
  router.push(`/oc/final-approval/${id}`)
}

onMounted(loadDashboard)
</script>

<style scoped>
.dashboard-content {
  padding: 24px;
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
}

.dashboard-header {
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 14px;
  margin-bottom: 24px;
  text-align: left;
}

.dashboard-header h2 {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.dashboard-header p {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  text-align: left;
}

.card-title {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  margin: 0;
}

.card h2 {
  margin-top: 8px;
  font-size: 2rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0;
}

.bg-yellow { border-top: 4px solid #f59e0b; }
.bg-green { border-top: 4px solid #10b981; }
.bg-blue { border-top: 4px solid #3b82f6; }

.sections-container {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.section h3 {
  font-size: 1rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 14px 0;
  border-left: 4px solid #3b82f6;
  padding-left: 12px;
}

/* Table */
.table-wrapper {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.table-header, .table-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr 1fr 1fr 1fr;
  align-items: center;
}

.table-header {
  background: #1e293b;
  color: white;
  font-weight: 600;
  font-size: 13px;
}

.th, .td {
  padding: 13px 14px;
  text-align: left;
  font-size: 13px;
}

.td {
  color: #334155;
  border-bottom: 1px solid #f1f5f9;
}

.table-row:last-child .td {
  border-bottom: none;
}

.title-cell {
  font-weight: 600;
  color: #0f172a;
}

/* Status Badges */
.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
}

.status-badge.pending {
  background: #fef3c7;
  color: #92400e;
}

.status-badge.approved {
  background: #dcfce7;
  color: #166534;
}

.status-badge.budget {
  background: #dbeafe;
  color: #1e40af;
}

/* Buttons */
.action-btn {
  background: #3b82f6;
  color: white;
  border: none;
  padding: 7px 15px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s;
}

.action-btn:hover {
  background: #2563eb;
}

.view-btn {
  background: #f1f5f9;
  color: #475569;
  border: 1px solid #e2e8f0;
  padding: 7px 15px;
  border-radius: 7px;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.view-btn:hover {
  background: #e2e8f0;
  color: #1e293b;
}

/* Loading */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 60px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(59, 130, 246, 0.2);
  border-top-color: #3b82f6;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.empty-banner {
  padding: 32px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  color: #64748b;
  font-size: 0.875rem;
  text-align: center;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.animate-fade {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .summary-cards {
    grid-template-columns: 1fr;
  }
  .table-header, .table-row {
    grid-template-columns: 1fr 1fr;
  }
}
</style>