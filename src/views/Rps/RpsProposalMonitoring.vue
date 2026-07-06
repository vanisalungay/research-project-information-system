<template>
  <div class="page">
    <!-- PAGE 1: PROPOSAL MONITORING LIST -->
    <div class="list-page">
      <!-- Header -->
      <div class="dashboard-header">
        <div>
          <h2>Proposal Monitoring Dashboard</h2>
          <p class="subtitle">Track the end-to-end progress of submitted research proposals across all offices</p>
        </div>
      </div>

      <!-- Controls & Filters Panel -->
      <div class="filters-card">
        <div class="filters-grid">
          <!-- Search -->
          <div class="filter-item search-box">
            <label for="search-input">Search Proposals</label>
            <input 
              id="search-input"
              v-model="searchQuery" 
              type="text" 
              placeholder="Search by title, leader, or proposal ID..."
              class="form-control"
              @input="currentPage = 1"
            />
          </div>

          <!-- Filter by Current Office -->
          <div class="filter-item">
            <label for="office-filter">Filter by Current Office</label>
            <select id="office-filter" v-model="selectedOffice" class="form-control select-control" @change="currentPage = 1">
              <option value="">All Offices</option>
              <option value="Proponent">Proponent</option>
              <option value="RPS">RPS</option>
              <option value="OVCRIGE">OVCRIGE</option>
              <option value="REC">REC</option>
              <option value="OC">OC</option>
              <option value="OVCAF">OVCAF</option>
              <option value="Implementation">Implementation</option>
            </select>
          </div>

          <!-- Filter by Status -->
          <div class="filter-item">
            <label for="status-filter">Filter by Current Status</label>
            <select id="status-filter" v-model="selectedStatus" class="form-control select-control" @change="currentPage = 1">
              <option value="">All Statuses</option>
              <option value="SUBMITTED">Submitted</option>
              <option value="ENDORSED">Endorsed</option>
              <option value="UNDER_REVIEW">Under Review</option>
              <option value="APPROVED">Approved</option>
              <option value="RPS_RETURNED">Returned by RPS</option>
              <option value="REJECTED">Rejected</option>
              <option value="READY_FOR_RELEASE">Ready for Release</option>
              <option value="RELEASED">Funds Released</option>
            </select>
          </div>

          <!-- Clear Filters -->
          <div class="filter-item btn-clear-wrapper">
            <button class="btn btn-secondary btn-clear" @click="clearFilters">Clear Filters</button>
          </div>
        </div>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading-state">
        <p>Loading proposals...</p>
      </div>

      <!-- Error -->
      <div v-else-if="error" class="loading-state">
        <p class="error-text">{{ error }}</p>
        <button class="btn btn-primary" @click="fetchProposals">Retry</button>
      </div>

      <!-- Table Card -->
      <div v-else class="table-card">
        <div class="table-responsive">
          <table class="proposal-table">
            <thead>
              <tr>
                <th style="width: 80px;">ID</th>
                <th>Project Title</th>
                <th>Project Leader</th>
                <th>College</th>
                <th style="width: 110px;">Submitted</th>
                <th>Current Office</th>
                <th>Current Status</th>
                <th style="width: 90px; text-align: center;">Progress</th>
                <th style="width: 140px; text-align: center;">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prop in paginatedProposals" :key="prop.id">
                <td class="id-cell">#{{ prop.id }}</td>
                <td class="title-cell">
                  <strong class="prop-title">{{ prop.projectTitle || 'Untitled' }}</strong>
                  <div class="prop-program">{{ prop.programTitle || '' }}</div>
                </td>
                <td>{{ prop.projectLeader || prop.proponent?.name || 'N/A' }}</td>
                <td>{{ prop.college || 'N/A' }}</td>
                <td>{{ formatDate(prop.createdAt) }}</td>
                <td class="office-cell">
                  <span class="office-tag">{{ getCurrentOffice(prop) }}</span>
                </td>
                <td>
                  <span :class="['status-badge', getBadgeClass(prop.status)]">
                    {{ formatStatus(prop.status) }}
                  </span>
                </td>
                <td class="progress-cell">
                  <div class="progress-bar-wrapper">
                    <span class="progress-val">{{ getProgressPercentage(prop) }}%</span>
                    <div class="progress-track">
                      <div class="progress-fill" :style="{ width: getProgressPercentage(prop) + '%' }"></div>
                    </div>
                  </div>
                </td>
                <td class="action-cell">
                  <button class="btn btn-primary btn-view" @click="viewMonitoring(prop.id)">
                    View Details
                  </button>
                </td>
              </tr>
              <tr v-if="filteredProposals.length === 0">
                <td colspan="9" class="empty-state">
                  <div class="empty-icon">📂</div>
                  <h3>No Proposals Found</h3>
                  <p>Try refining your search terms or filter selections.</p>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Pagination Controls -->
        <div v-if="filteredProposals.length > 0" class="pagination-panel">
          <div class="pagination-info">
            Showing <strong>{{ paginationStartIndex + 1 }}</strong> to <strong>{{ paginationEndIndex }}</strong> of <strong>{{ filteredProposals.length }}</strong> proposals
          </div>
          <div class="pagination-buttons">
            <button 
              class="btn btn-pagination" 
              :disabled="currentPage === 1" 
              @click="currentPage--"
            >
              Previous
            </button>
            <span class="page-indicator">Page {{ currentPage }} of {{ totalPages }}</span>
            <button 
              class="btn btn-pagination" 
              :disabled="currentPage === totalPages" 
              @click="currentPage++"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/utils/api'

export default {
  name: 'ProposalMonitoringList',

  data() {
    return {
      searchQuery: '',
      selectedOffice: '',
      selectedStatus: '',
      currentPage: 1,
      perPage: 10,
      proposals: [],
      loading: false,
      error: null,
    }
  },

  computed: {
    filteredProposals() {
      let result = [...this.proposals]

      const term = this.searchQuery.toLowerCase().trim()
      if (term) {
        result = result.filter(p => 
          (p.projectTitle || '').toLowerCase().includes(term) ||
          (p.projectLeader || p.proponent?.name || '').toLowerCase().includes(term) ||
          String(p.id).includes(term)
        )
      }

      if (this.selectedOffice) {
        result = result.filter(p => this.getCurrentOffice(p) === this.selectedOffice)
      }

      if (this.selectedStatus) {
        result = result.filter(p => p.status === this.selectedStatus)
      }

      result.sort((a, b) => {
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      })

      return result
    },

    totalPages() {
      const count = this.filteredProposals.length
      return count > 0 ? Math.ceil(count / this.perPage) : 1
    },

    paginationStartIndex() {
      return (this.currentPage - 1) * this.perPage
    },

    paginationEndIndex() {
      const end = this.currentPage * this.perPage
      return Math.min(end, this.filteredProposals.length)
    },

    paginatedProposals() {
      return this.filteredProposals.slice(this.paginationStartIndex, this.paginationEndIndex)
    }
  },

  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals')
        this.proposals = Array.isArray(res.data) ? res.data : []
      } catch (err) {
        console.error('Failed to fetch proposals:', err)
        this.error = 'Failed to load proposals. Ensure the backend server is running.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },

    getCurrentOffice(proposal) {
      const status = proposal.status
      const officeMap = {
        'DRAFT': 'Proponent',
        'SUBMITTED': 'RPS',
        'PENDING': 'RPS',
        'RPS_RETURNED': 'Proponent',
        'REVISION': 'Proponent',
        'ENDORSED': 'OVCRIGE',
        'OVCRIGE_COORDINATION': 'OVCRIGE',
        'UNDER_REVIEW': 'REC',
        'REC_EVALUATION': 'REC',
        'REC_APPROVED': 'OVCRIGE',
        'REC_REVISION': 'Proponent',
        'REC_REJECTED': 'Proponent',
        'FOR_OVCAF_APPROVAL': 'OVCAF',
        'FOR_OC_APPROVAL': 'OC',
        'APPROVED': 'OVCAF',
        'READY_FOR_RELEASE': 'OVCAF',
        'RELEASED': 'Implementation',
        'IMPLEMENTATION': 'Implementation',
        'COMPLETED': 'Implementation',
        'REJECTED': 'RPS',
        'RETURNED': 'OVCAF',
      }
      return officeMap[status] || 'RPS'
    },

    formatStatus(status) {
      const statusMap = {
        'DRAFT': 'Draft',
        'SUBMITTED': 'Submitted',
        'PENDING': 'Pending',
        'RPS_RETURNED': 'Returned by RPS',
        'REVISION': 'Revision',
        'ENDORSED': 'Endorsed',
        'OVCRIGE_COORDINATION': 'OVCRIGE Coordination',
        'UNDER_REVIEW': 'Under REC Review',
        'REC_APPROVED': 'REC Approved',
        'REC_REVISION': 'REC Revision',
        'REC_REJECTED': 'REC Rejected',
        'FOR_OVCAF_APPROVAL': 'For OVCAF',
        'FOR_OC_APPROVAL': 'For Chancellor',
        'APPROVED': 'Approved',
        'READY_FOR_RELEASE': 'Ready for Release',
        'RELEASED': 'Funds Released',
        'IMPLEMENTATION': 'Implementation',
        'COMPLETED': 'Completed',
        'REJECTED': 'Rejected',
        'RETURNED': 'Returned',
      }
      return statusMap[status] || status
    },

    getProgressPercentage(proposal) {
      const status = proposal.status
      const progressMap = {
        'DRAFT': 5,
        'SUBMITTED': 15,
        'PENDING': 15,
        'RPS_RETURNED': 10,
        'REVISION': 10,
        'ENDORSED': 30,
        'OVCRIGE_COORDINATION': 35,
        'UNDER_REVIEW': 45,
        'REC_EVALUATION': 45,
        'REC_APPROVED': 55,
        'REC_REVISION': 20,
        'REC_REJECTED': 15,
        'FOR_OVCAF_APPROVAL': 65,
        'FOR_OC_APPROVAL': 75,
        'APPROVED': 80,
        'READY_FOR_RELEASE': 85,
        'RELEASED': 90,
        'IMPLEMENTATION': 95,
        'COMPLETED': 100,
        'REJECTED': 10,
        'RETURNED': 70,
      }
      return progressMap[status] || 0
    },

    getBadgeClass(status) {
      if (['APPROVED', 'COMPLETED', 'RELEASED', 'IMPLEMENTATION'].includes(status)) return 'badge-green'
      if (['ENDORSED', 'REC_APPROVED', 'READY_FOR_RELEASE'].includes(status)) return 'badge-blue'
      if (['RPS_RETURNED', 'REVISION', 'REC_REVISION', 'RETURNED'].includes(status)) return 'badge-yellow'
      if (['REJECTED', 'REC_REJECTED'].includes(status)) return 'badge-red'
      if (['SUBMITTED', 'PENDING', 'UNDER_REVIEW', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL'].includes(status)) return 'badge-blue'
      return 'badge-gray'
    },

    formatDate(dateString) {
      if (!dateString) return 'N/A'
      return new Date(dateString).toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric'
      })
    },

    viewMonitoring(id) {
      this.$router.push({ name: 'ProposalDetails', params: { id: id } })
    },

    clearFilters() {
      this.searchQuery = ''
      this.selectedOffice = ''
      this.selectedStatus = ''
      this.currentPage = 1
    },
  },

  mounted() {
    this.fetchProposals()
  }
}
</script>

<style scoped>
.page {
  padding: 30px;
  font-family: 'Poppins', 'Inter', sans-serif;
  background-color: #f8fafc;
  min-height: 100vh;
  color: #1e293b;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.dashboard-header {
  margin-bottom: 24px;
}

.dashboard-header h2 {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.subtitle {
  color: #64748b;
  margin: 4px 0 0 0;
  font-size: 14px;
}

.filters-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.02);
}

.filters-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex: 1;
  min-width: 200px;
}

.filter-item.search-box {
  flex: 2;
  min-width: 300px;
}

.filter-item label {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: #64748b;
  letter-spacing: 0.5px;
}

.form-control {
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 10px 14px;
  font-size: 13px;
  color: #334155;
  background-color: #f8fafc;
  transition: all 0.15s;
  height: 42px;
  box-sizing: border-box;
}

.form-control:focus {
  border-color: #4f46e5;
  outline: none;
  background-color: white;
}

.select-control {
  cursor: pointer;
}

.btn-clear-wrapper {
  flex: 0 0 auto;
  min-width: unset;
}

.btn {
  padding: 10px 20px;
  font-size: 13px;
  font-weight: 600;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: all 0.15s;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.btn-primary {
  background-color: #4f46e5;
  color: white;
}

.btn-primary:hover {
  background-color: #4338ca;
}

.btn-secondary {
  background-color: #e2e8f0;
  color: #334155;
  border: 1px solid #cbd5e1;
}

.btn-secondary:hover {
  background-color: #cbd5e1;
}

.btn-clear {
  width: 100%;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: #64748b;
}

.error-text {
  color: #dc2626;
}

.table-card {
  background: white;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.02);
  overflow: hidden;
  margin-bottom: 24px;
}

.table-responsive {
  width: 100%;
  overflow-x: auto;
}

.proposal-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
  font-size: 13.5px;
}

.proposal-table th {
  background-color: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 14px 18px;
  border-bottom: 2px solid #e2e8f0;
  font-size: 11px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.proposal-table td {
  padding: 16px 18px;
  border-bottom: 1px solid #f1f5f9;
  color: #334155;
  vertical-align: middle;
}

.proposal-table tbody tr:hover {
  background-color: #f8fafc;
}

.id-cell {
  font-family: monospace;
  font-weight: 600;
  color: #64748b;
}

.title-cell {
  max-width: 320px;
}

.prop-title {
  color: #0f172a;
  display: block;
  line-height: 1.4;
}

.prop-program {
  font-size: 11px;
  color: #64748b;
  margin-top: 3px;
}

.office-cell .office-tag {
  background-color: #f1f5f9;
  color: #334155;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 11px;
}

.progress-bar-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-val {
  font-size: 11px;
  font-weight: 600;
  color: #4f46e5;
  text-align: right;
}

.progress-track {
  height: 6px;
  background-color: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
  width: 100%;
}

.progress-fill {
  height: 100%;
  background-color: #4f46e5;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.btn-view {
  background-color: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  height: unset;
  border-radius: 6px;
}

.btn-view:hover {
  background-color: #3b82f6;
  color: white;
  border-color: #3b82f6;
}

.empty-state {
  text-align: center;
  padding: 60px 20px !important;
}

.empty-icon {
  font-size: 40px;
  margin-bottom: 12px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 6px 0;
  color: #0f172a;
}

.empty-state p {
  color: #64748b;
  margin: 0;
  font-size: 13px;
}

.pagination-panel {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-top: 1px solid #e2e8f0;
  background-color: #f8fafc;
  flex-wrap: wrap;
  gap: 12px;
}

.pagination-info {
  font-size: 12.5px;
  color: #64748b;
}

.pagination-buttons {
  display: flex;
  align-items: center;
  gap: 12px;
}

.btn-pagination {
  padding: 6px 14px;
  font-size: 12px;
  height: 32px;
  border: 1px solid #cbd5e1;
  background: white;
  color: #334155;
}

.btn-pagination:hover:not(:disabled) {
  background-color: #f1f5f9;
}

.btn-pagination:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-indicator {
  font-size: 12.5px;
  font-weight: 500;
  color: #334155;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 11.5px;
  font-weight: 600;
  display: inline-block;
}

.badge-gray {
  background-color: #f1f5f9;
  color: #64748b;
}

.badge-blue {
  background-color: #dbeafe;
  color: #1e40af;
}

.badge-green {
  background-color: #dcfce7;
  color: #166534;
}

.badge-yellow {
  background-color: #fef3c7;
  color: #92400e;
}

.badge-red {
  background-color: #fee2e2;
  color: #991b1b;
}
</style>