<template>
  <div class="panel">
    <!-- Toolbar -->
    <div class="toolbar">
      <div class="search-wrap">
        <svg class="field-icon search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <circle cx="11" cy="11" r="8"></circle>
          <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
        </svg>
        <input
          v-model="search"
          type="text"
          class="search-input"
          placeholder="Search by title, proponent or document ID"
        />
      </div>

      <div class="select-wrap">
        <svg class="field-icon filter-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
        </svg>
        <select v-model="selectedStatus" class="status-select">
          <option value="">All Status</option>
          <option value="SUBMITTED">Pending</option>
          <option value="REVISION">Revision</option>
          <option value="ENDORSED">Endorsed</option>
          <option value="REJECTED">Rejected</option>
        </select>
        <svg class="field-icon chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-box">Loading proposals...</div>

    <!-- Error -->
    <div v-else-if="error" class="state-box error-text">{{ error }}</div>

    <!-- Table -->
    <div v-else class="table-card">
      <div class="table-scroll">
        <table class="proposals-table">
          <thead>
            <tr>
              <th class="col-id">Document ID</th>
              <th>Proposal Title</th>
              <th>Proponent</th>
              <th>Date Submitted</th>
              <th>Status</th>
              <th class="col-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposal in filteredProposals" :key="proposal.id">
              <td class="document-id">{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
              <td>
                <div class="title-cell">{{ proposal.projectTitle || proposal.title }}</div>
                <div class="subtext">{{ proposal.programTitle || 'N/A' }}</div>
              </td>
              <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
              <td class="date-cell">{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>
              <td>
                <span :class="['status', (proposal.status || '').toLowerCase()]">
                  {{ proposal.status }}
                </span>
              </td>
              <td>
                <button class="review-btn" @click="viewProposal(proposal)">Review</button>
              </td>
            </tr>
            <tr v-if="filteredProposals.length === 0">
              <td colspan="6" class="empty">
                {{ search || selectedStatus ? 'No proposals match your search.' : 'No submitted proposals found.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="table-footer">
        Showing {{ filteredProposals.length }} proposal{{ filteredProposals.length === 1 ? '' : 's' }}
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/utils/api'

export default {
  name: 'RpsSubmittedProposalsPanel',
  data() {
    return {
      search: '',
      selectedStatus: '',
      proposals: [],
      loading: false,
      error: null,
    }
  },
  computed: {
    filteredProposals() {
      const keyword = this.search.toLowerCase().trim()
      return this.proposals.filter((p) => {
        const matchesStatus = this.selectedStatus === '' || p.status === this.selectedStatus
        if (!matchesStatus) return false
        if (!keyword) return true
        const title = String(p.projectTitle || p.title || '').toLowerCase()
        const program = String(p.programTitle || '').toLowerCase()
        const documentId = String(p.documentId || p.proposalCode || '').toLowerCase()
        const leader = String(p.projectLeader || p.proponent?.name || '').toLowerCase()
        return (
          title.includes(keyword) ||
          program.includes(keyword) ||
          documentId.includes(keyword) ||
          leader.includes(keyword)
        )
      })
    },
  },
  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals?statusIn=SUBMITTED&statusIn=REVISION&statusIn=RETURNED_TO_RPS')
        this.proposals = Array.isArray(res.data) ? res.data : []
      } catch (err) {
        console.error(err)
        this.error = 'Failed to load proposals.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },
    viewProposal(proposal) {
      if (proposal.status === 'SUBMITTED') {
        // SUBMITTED proposals go to the review page with endorse/return/reject actions
        this.$router.push({ name: 'ProposalReview', params: { id: proposal.id } })
      } else if (proposal.status === 'RETURNED_TO_RPS') {
        this.$router.push({ name: 'RpsForwardRevision', params: { id: proposal.id } })
      } else if (proposal.status === 'REVISION' || proposal.status === 'RPS_RETURNED') {
        // Returned proposals go to the review viewer
        this.$router.push({ name: 'ReviewResultViewer', params: { id: proposal.id } })
      } else {
        // All other statuses go to the details viewer
        this.$router.push({ name: 'ProposalDetails', params: { id: proposal.id } })
      }
    },
  },
  mounted() {
    this.fetchProposals()
  },
}
</script>

<style scoped>
.panel {
  width: 100%;
  font-family: 'Inter', sans-serif;
}

/* TOOLBAR */
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  padding: 11px 12px;
}

.search-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 1 1 260px;
  min-width: 180px;
}

.select-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.field-icon {
  position: absolute;
  color: #78809a;
  pointer-events: none;
}

.search-icon {
  left: 10px;
  width: 14px;
  height: 14px;
}

.filter-icon {
  left: 10px;
  width: 13px;
  height: 13px;
}

.chevron {
  right: 9px;
  width: 12px;
  height: 12px;
}

.search-input {
  width: 100%;
  height: 34px;
  padding: 0 10px 0 32px;
  border: 1px solid #e0e4ef;
  border-radius: 7px;
  font-size: 12px;
  font-family: inherit;
  color: #20234f;
  background: #ffffff;
  outline: none;
}

.search-input::placeholder {
  color: #78809a;
}

.search-input:focus {
  border-color: #25245b;
  box-shadow: 0 0 0 3px rgba(37, 36, 91, 0.1);
}

.status-select {
  height: 34px;
  min-width: 120px;
  padding: 0 26px 0 30px;
  border: 1px solid #e0e4ef;
  border-radius: 7px;
  font-size: 12px;
  font-family: inherit;
  color: #20234f;
  background: #ffffff;
  outline: none;
  cursor: pointer;
  appearance: none;
  -webkit-appearance: none;
}

.status-select:focus {
  border-color: #25245b;
  box-shadow: 0 0 0 3px rgba(37, 36, 91, 0.1);
}

/* STATES */
.state-box {
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 0 0 10px 10px;
  padding: 22px;
  font-size: 12px;
  color: #78809a;
}

.error-text {
  color: #dc2626;
}

/* TABLE */
.table-card {
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 0 0 10px 10px;
  overflow: hidden;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

.proposals-table {
  width: 100%;
  min-width: 640px;
  border-collapse: collapse;
}

.proposals-table th {
  background: #ffffff;
  color: #78809a;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  text-align: left;
  padding: 9px 12px;
  border-bottom: 1px solid #e3e6ef;
  white-space: nowrap;
}

.proposals-table td {
  padding: 9px 12px;
  border-bottom: 1px solid #eef0f5;
  font-size: 11px;
  color: #20234f;
  vertical-align: top;
}

.proposals-table tbody tr:hover {
  background: #f8fafc;
}

.proposals-table tbody tr:last-child td {
  border-bottom: none;
}

.col-id {
  width: 150px;
}

.col-actions {
  width: 96px;
}

.document-id {
  font-size: 10px;
  font-weight: 500;
  color: #25245b;
  word-break: break-word;
}

.title-cell {
  font-size: 11px;
  font-weight: 500;
  color: #20234f;
  line-height: 1.35;
}

.subtext {
  font-size: 10px;
  color: #78809a;
  margin-top: 2px;
}

.date-cell {
  color: #78809a;
  white-space: nowrap;
}

/* STATUS BADGES */
.status {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 600;
  white-space: nowrap;
}

.submitted {
  background: #ede9fe;
  color: #5b21b6;
}

.revision,
.returned_to_rps {
  background: #fef3c7;
  color: #92400e;
}

.endorsed {
  background: #dcfce7;
  color: #166534;
}

.rejected {
  background: #fee2e2;
  color: #b91c1c;
}

/* ACTION BUTTON */
.review-btn {
  background: #ffd400;
  color: #25245b;
  border: none;
  height: 29px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s ease;
}

.review-btn:hover {
  background: #f0c800;
}

.review-btn:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.empty {
  text-align: center;
  color: #78809a;
  padding: 24px;
  font-size: 12px;
}

.table-footer {
  padding: 10px 12px;
  font-size: 10px;
  color: #78809a;
  border-top: 1px solid #eef0f5;
}

@media (max-width: 640px) {
  .search-wrap {
    flex: 1 1 100%;
  }

  .select-wrap,
  .status-select {
    width: 100%;
  }
}
</style>
