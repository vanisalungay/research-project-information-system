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
              <option value="RII">RII</option>
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
              <option value="Submitted">Submitted</option>
              <option value="Under Review">Under Review</option>
              <option value="Approved">Approved</option>
              <option value="Returned for Revision">Returned for Revision</option>
              <option value="Rejected">Rejected</option>
              <option value="Ready for Release">Ready for Release</option>
              <option value="Funds Released">Funds Released</option>
              <option value="For Implementation">For Implementation</option>
              <option value="Ongoing">Ongoing</option>
              <option value="Completed">Completed</option>
            </select>
          </div>

          <!-- Clear Filters -->
          <div class="filter-item btn-clear-wrapper">
            <button class="btn btn-secondary btn-clear" @click="clearFilters">Clear Filters</button>
          </div>
        </div>
      </div>

      <!-- Table Card -->
      <div class="table-card">
        <div class="table-responsive">
          <table class="proposal-table">
            <thead>
              <tr>
                <th style="width: 110px;">Proposal ID</th>
                <th>Project Title</th>
                <th>Project Leader</th>
                <th>College</th>
                <th>Department</th>
                <th style="width: 120px;">Submitted</th>
                <th>Current Office</th>
                <th>Current Status</th>
                <th style="width: 90px; text-align: center;">Progress</th>
                <th style="width: 140px; text-align: center;">Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="prop in paginatedProposals" :key="prop.id">
                <td class="id-cell">#{{ prop.code }}</td>
                <td class="title-cell">
                  <strong class="prop-title">{{ prop.title }}</strong>
                  <div class="prop-program">{{ prop.program }}</div>
                </td>
                <td>{{ prop.leader }}</td>
                <td>{{ prop.college }}</td>
                <td>{{ prop.department }}</td>
                <td>{{ prop.submittedDate }}</td>
                <td class="office-cell">
                  <span class="office-tag">{{ getCurrentOffice(prop) }}</span>
                </td>
                <td>
                  <span :class="['status-badge', getBadgeClass(getCurrentOverallStatus(prop))]">
                    {{ getCurrentOverallStatus(prop) }}
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
                    View Monitoring
                  </button>
                </td>
              </tr>
              <tr v-if="filteredProposals.length === 0">
                <td colspan="10" class="empty-state">
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
const STORAGE_KEY = 'proposals_workflow_db';
const defaultProposals = [];

export default {
  name: 'ProposalMonitoringList',

  data() {
    return {
      searchQuery: '',
      selectedOffice: '',
      selectedStatus: '',
      currentPage: 1,
      perPage: 5,
      proposals: [],
    }
  },

  computed: {
    filteredProposals() {
      let result = [...this.proposals];

      const term = this.searchQuery.toLowerCase().trim();
      if (term) {
        result = result.filter(p => 
          p.title.toLowerCase().includes(term) ||
          p.leader.toLowerCase().includes(term) ||
          p.code.toLowerCase().includes(term)
        );
      }

      if (this.selectedOffice) {
        result = result.filter(p => this.getCurrentOffice(p) === this.selectedOffice);
      }

      if (this.selectedStatus) {
        result = result.filter(p => this.getCurrentOverallStatus(p) === this.selectedStatus);
      }

      result.sort((a, b) => {
        return new Date(b.submittedDate) - new Date(a.submittedDate);
      });

      return result;
    },

    totalPages() {
      const count = this.filteredProposals.length;
      return count > 0 ? Math.ceil(count / this.perPage) : 1;
    },

    paginationStartIndex() {
      return (this.currentPage - 1) * this.perPage;
    },

    paginationEndIndex() {
      const end = this.currentPage * this.perPage;
      return Math.min(end, this.filteredProposals.length);
    },

    paginatedProposals() {
      return this.filteredProposals.slice(this.paginationStartIndex, this.paginationEndIndex);
    }
  },

  methods: {
    getShortOfficeName(key) {
      const map = {
        proponent: 'Proponent',
        rii: 'RII',
        ovcrige: 'OVCRIGE',
        rec: 'REC',
        oc: 'OC',
        ovcaf: 'OVCAF',
        implementation: 'Implementation'
      };
      return map[key] || key.toUpperCase();
    },

    getCurrentOffice(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status === 'Rejected' || stage.status === 'Returned for Revision') {
          return this.getShortOfficeName(key);
        }
      }
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status !== 'Approved' && stage.status !== 'Funds Released' && stage.status !== 'Completed') {
          return this.getShortOfficeName(key);
        }
      }
      
      return 'Implementation';
    },

    getCurrentOverallStatus(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      
      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status === 'Rejected' || stage.status === 'Returned for Revision') {
          return stage.status;
        }
      }

      for (const key of order) {
        const stage = proposal.stages[key];
        if (stage.status !== 'Approved' && stage.status !== 'Funds Released' && stage.status !== 'Completed') {
          if (stage.status === 'Waiting') {
            const prevIndex = order.indexOf(key) - 1;
            if (prevIndex >= 0) {
              const prevStage = proposal.stages[order[prevIndex]];
              if (prevStage.status === 'Approved' || prevStage.status === 'Funds Released') {
                return 'Under Review';
              }
            }
            return 'Waiting';
          }
          return stage.status;
        }
      }
      
      const lastStage = proposal.stages.implementation;
      return lastStage.status;
    },

    getProgressPercentage(proposal) {
      const order = ['proponent', 'rii', 'ovcrige', 'rec', 'oc', 'ovcaf', 'implementation'];
      let completedCount = 0;
      
      for (const key of order) {
        const stage = proposal.stages[key];
        const status = stage.status;
        if (status === 'Approved' || status === 'Funds Released' || status === 'Completed') {
          completedCount += 1.0;
        } else if (status === 'Under Review' || status === 'Ongoing') {
          completedCount += 0.5;
        } else if (status === 'Ready for Release' || status === 'For Implementation') {
          completedCount += 0.8;
        } else if (status === 'Returned for Revision') {
          completedCount += 0.2;
        } else if (status === 'Rejected') {
          completedCount += 0.1;
        }
      }
      return Math.min(100, Math.round((completedCount / order.length) * 100));
    },

    getBadgeClass(status) {
      switch (status) {
        case 'Approved':
        case 'Completed':
        case 'Funds Released':
          return 'badge-green';
        case 'Returned for Revision':
        case 'Ready for Release':
          return 'badge-yellow';
        case 'Rejected':
          return 'badge-red';
        case 'Under Review':
        case 'For Implementation':
        case 'Ongoing':
        case 'Submitted':
          return 'badge-blue';
        case 'Waiting':
        default:
          return 'badge-gray';
      }
    },

    viewMonitoring(id) {
      this.$router.push({ name: 'ProposalMonitoringDetails', params: { id: id } });
    },

    clearFilters() {
      this.searchQuery = '';
      this.selectedOffice = '';
      this.selectedStatus = '';
      this.currentPage = 1;
    },

    loadProposals() {
      localStorage.removeItem(STORAGE_KEY);
      this.proposals = [];
    }
  },

  mounted() {
    this.loadProposals();
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
  width: 120%;
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
