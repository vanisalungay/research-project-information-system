<template>
  <div class="page">

    <div class="page-header">
      <h2>Budget Processing</h2>
      <p>Process and track approved research project budgets.</p>
    </div>

    <div class="toolbar">
      <input
        v-model="search"
        class="search"
        type="text"
        placeholder="Search proposal..."
      >
    </div>

    <div class="filters">
      <button
        v-for="filter in filters"
        :key="filter"
        :class="{active: activeFilter === filter}"
        @click="activeFilter = filter"
      >
        {{ filter }}
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading budget records...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="fetchProposals">Retry</button>
    </div>

    <div v-else class="table-container">
      <table class="table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Proposal Title</th>
            <th>Project Leader</th>
            <th>College</th>
            <th>Status</th>
            <th>Action</th>
          </tr>
        </thead>

        <tbody>
          <tr
            v-for="proposal in filteredProposals"
            :key="proposal.id"
            class="clickable-row"
            @click="openProposal(proposal)"
          >
            <td>#{{ proposal.id }}</td>
            <td>{{ proposal.projectTitle || 'Untitled' }}</td>
            <td>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</td>
            <td>{{ proposal.college || 'N/A' }}</td>
            <td>
              <span
                class="status"
                :class="getStatusClass(proposal.status)"
              >
                {{ formatStatus(proposal.status) }}
              </span>
            </td>
            <td>
              <button class="view-btn" @click.stop="openProposal(proposal)">View</button>
            </td>
          </tr>

          <tr v-if="filteredProposals.length === 0">
            <td colspan="6" class="empty">
              No budget records found.
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
  name: "RpsBudgetProcessing",

  data() {
    return {
      search: "",
      proposals: [],
      activeFilter: "All",
      loading: false,
      error: null,
      filters: [
        "All",
        "Approved",
        "Ready for Release",
        "Released",
        "Returned"
      ],
    }
  },

  computed: {
    filteredProposals() {
      let result = this.proposals

      const keyword = this.search.toLowerCase().trim()
      if (keyword) {
        result = result.filter(proposal =>
          (proposal.projectTitle || '').toLowerCase().includes(keyword) ||
          (proposal.projectLeader || proposal.proponent?.name || '').toLowerCase().includes(keyword) ||
          String(proposal.id).includes(keyword)
        )
      }

      if (this.activeFilter !== "All") {
        const statusMap = {
          "Approved": "APPROVED",
          "Ready for Release": "READY_FOR_RELEASE",
          "Released": "RELEASED",
          "Returned": "RETURNED",
        }
        const backendStatus = statusMap[this.activeFilter]
        if (backendStatus) {
          result = result.filter(p => p.status === backendStatus)
        }
      }

      return result
    }
  },

  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals?statusIn=APPROVED&statusIn=READY_FOR_RELEASE&statusIn=RELEASED&statusIn=RETURNED')
        this.proposals = Array.isArray(res.data) ? res.data : []
      } catch (err) {
        console.error('Failed to fetch budget proposals:', err)
        this.error = 'Failed to load budget records.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },

    formatStatus(status) {
      const statusMap = {
        'APPROVED': 'Approved',
        'READY_FOR_RELEASE': 'Ready for Release',
        'RELEASED': 'Released',
        'RETURNED': 'Returned',
      }
      return statusMap[status] || status
    },

    getStatusClass(status) {
      const classMap = {
        'APPROVED': 'approved',
        'READY_FOR_RELEASE': 'ready',
        'RELEASED': 'released',
        'RETURNED': 'returned',
      }
      return classMap[status] || ''
    },

    openProposal(proposal) {
      this.$router.push({ name: 'ProposalDetails', params: { id: proposal.id } })
    }
  },

  mounted() {
    this.fetchProposals()
  }
}
</script>

<style scoped>
.page {
  padding: 25px;
  font-family: Segoe UI, sans-serif;
  background: #f5f7fb;
  min-height: 100vh;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.page-header p {
  margin-top: 5px;
  font-size: 14px;
  color: #64748b;
}

.toolbar {
  margin-bottom: 20px;
}

.search {
  width: 100%;
  padding: 12px 15px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
}

.search:focus {
  border-color: #2563eb;
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

.table-container {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,.08);
}

.table {
  width: 100%;
  border-collapse: collapse;
}

.table thead {
  background: #1e40af;
  color: #fff;
}

.table th,
.table td {
  padding: 15px;
  text-align: left;
  font-size: 14px;
}

.table td {
  border-bottom: 1px solid #e5e7eb;
  color: #374151;
}

.table tbody tr:hover {
  background: #f8fafc;
}

.status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  display: inline-block;
  text-align: center;
  min-width: 140px;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.ready {
  background: #fef3c7;
  color: #92400e;
}

.released {
  background: #dbeafe;
  color: #1d4ed8;
}

.returned {
  background: #fee2e2;
  color: #b91c1c;
}

.view-btn {
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 8px 18px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
}

.view-btn:hover {
  background: #1d4ed8;
}

.empty {
  padding: 25px;
  text-align: center;
  color: #94a3b8;
}

.filters {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.filters button {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: #e2e8f0;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
}

.filters button.active {
  background: #1e40af;
  color: #fff;
}

.clickable-row {
  cursor: pointer;
  transition: .2s;
}

.clickable-row:hover {
  background: #eff6ff !important;
}
</style>