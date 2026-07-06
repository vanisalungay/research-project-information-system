<template>
  <div class="page">
    <!-- Header -->
    <h2>RII Endorsed Proposals</h2>
    <p class="subtitle">
      Proposals endorsed by RPS and forwarded to OVCRIGE
    </p>

    <div class="page-header">
      <div>
        <h2>Endorsed Proposals</h2>
        <p>Review and track proposals endorsed to OVCRIGE.</p>
      </div>
    </div>

    <!-- Search -->
    <div class="toolbar">
      <input
        v-model="search"
        type="text"
        class="search"
        placeholder="Search Proposal Title, Project Leader..."
      />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading endorsed proposals...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="fetchProposals">Retry</button>
    </div>

    <!-- Table -->
    <div v-else class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Proposal Title</th>
            <th>Project Leader</th>
            <th>College</th>
            <th>Status</th>
            <th width="180">Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="proposal in filteredProposals"
            :key="proposal.id"
          >
            <td>#{{ proposal.id }}</td>
            <td>{{ proposal.projectTitle || 'Untitled' }}</td>
            <td>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</td>
            <td>{{ proposal.college || 'N/A' }}</td>

            <td class="status-cell">
              <span
                class="status"
                :class="getStatusClass(proposal.status)"
              >
                {{ formatStatus(proposal.status) }}
              </span>
            </td>

            <td class="actions">
              <button
                class="view-btn"
                @click="viewProposal(proposal)"
              >
                View
              </button>
            </td>
          </tr>

          <tr v-if="filteredProposals.length === 0">
            <td colspan="6" class="empty">
              No endorsed proposals found.
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import api from '@/utils/api'

export default {
  name: "RIIEndorsedProposals",

  data() {
    return {
      search: "",
      proposals: [],
      loading: false,
      error: null,
    }
  },

  computed: {
    filteredProposals() {
      const keyword = this.search.toLowerCase().trim()
      if (!keyword) return this.proposals

      return this.proposals.filter(proposal =>
        (proposal.projectTitle || '').toLowerCase().includes(keyword) ||
        (proposal.projectLeader || proposal.proponent?.name || '').toLowerCase().includes(keyword) ||
        (String(proposal.id)).includes(keyword)
      )
    }
  },

  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals?statusIn=ENDORSED&statusIn=UNDER_REVIEW&statusIn=REC_APPROVED&statusIn=FOR_OVCAF_APPROVAL&statusIn=FOR_OC_APPROVAL&statusIn=APPROVED')
        this.proposals = Array.isArray(res.data) ? res.data : []
      } catch (err) {
        console.error('Failed to fetch endorsed proposals:', err)
        this.error = 'Failed to load endorsed proposals.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },

    viewProposal(proposal) {
      this.$router.push({ name: 'ProposalDetails', params: { id: proposal.id } })
    },

    formatStatus(status) {
      const statusMap = {
        'ENDORSED': 'Endorsed to OVCRIGE',
        'UNDER_REVIEW': 'Under REC Evaluation',
        'REC_APPROVED': 'REC Approved',
        'FOR_OVCAF_APPROVAL': 'For OVCAF Review',
        'FOR_OC_APPROVAL': 'For Chancellor Approval',
        'APPROVED': 'Approved',
      }
      return statusMap[status] || status
    },

    getStatusClass(status) {
      const classMap = {
        'ENDORSED': 'endorsed',
        'UNDER_REVIEW': 'pending',
        'REC_APPROVED': 'approved',
        'FOR_OVCAF_APPROVAL': 'pending',
        'FOR_OC_APPROVAL': 'pending',
        'APPROVED': 'approved',
        'REJECTED': 'rejected',
        'RETURNED': 'returned',
      }
      return classMap[status] || 'pending'
    }
  },

  mounted() {
    this.fetchProposals()
  }
}
</script>

<style scoped>
.page{
  padding:25px;
  font-family:Segoe UI,sans-serif;
  background:#f5f7fb;
  min-height:100vh;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.page-header{
  margin-bottom:20px;
}

.page-header h2{
  margin:0;
  color:#1e293b;
  font-size:28px;
  font-weight:700;
}

.page-header p{
  margin-top:5px;
  color:#64748b;
  font-size:14px;
}

.subtitle {
  color: #666;
  margin-bottom: 16px;
}

.toolbar{
  margin-bottom:20px;
}

.search{
  width:100%;
  padding:12px 15px;
  border:1px solid #d1d5db;
  border-radius:8px;
  font-size:14px;
  outline:none;
  transition:.2s;
  box-sizing:border-box;
}

.search:focus{
  border-color:#2563eb;
}

.loading-state {
  text-align: center;
  padding: 40px 20px;
  color: #64748b;
}

.error-text {
  color: #dc2626;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.table-container{
  background:#fff;
  border-radius:12px;
  overflow:hidden;
  box-shadow:0 4px 12px rgba(0,0,0,.08);
}

.table{
  width:100%;
  border-collapse:collapse;
}

.table thead tr{
  background:#1e40af !important;
}

.table thead th{
  background: #1e293b !important;
  color:#fff !important;
}

.table th{
  padding:15px;
  text-align:left;
  font-size:14px;
}

.table td{
  padding:15px;
  border-bottom:1px solid #e5e7eb;
  font-size:14px;
  color:#374151;
  vertical-align:middle;
}

.table tbody tr:hover{
  background:#f8fafc;
}

.view-btn{
  background:#3b82f6;
  color:#fff;
  border:none;
  padding:8px 14px;
  border-radius:6px;
  cursor:pointer;
}

.view-btn:hover{
  background:#2563eb;
}

.empty{
  text-align:center;
  padding:30px;
  color:#94a3b8;
}

.status{
  padding:6px 12px;
  border-radius:20px;
  font-size:12px;
  font-weight:600;
  display:inline-block;
}

.status-cell{
  vertical-align:middle;
}

.pending{
  background:#FEF3C7;
  color:#92400E;
}

.endorsed{
  background:#DBEAFE;
  color:#1D4ED8;
}

.approved{
  background:#DCFCE7;
  color:#166534;
}

.returned{
  background:#FDE68A;
  color:#B45309;
}

.rejected{
  background:#FEE2E2;
  color:#B91C1C;
}

.actions{
  display:flex;
  gap:8px;
  align-items:center;
}
</style>