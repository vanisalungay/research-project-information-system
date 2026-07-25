<template>
  <div class="page">
    <!-- Header -->
    <h2>Submitted Proposals</h2>
    <p class="subtitle">Review and manage all proposals forwarded to RPS</p>

    <!-- Controls -->
    <div class="controls">
      <input type="text" v-model="search" placeholder="Search proposals by title or category..." class="search" />
      <select v-model="selectedStatus" class="status-select">
        <option value="">All Status</option>
        <option value="SUBMITTED">Pending</option>
        <option value="REVISION">Revision</option>
        <option value="ENDORSED">Endorsed</option>
        <option value="REJECTED">Rejected</option>
      </select>
    </div>

    <!-- Loading -->
    <p v-if="loading" style="padding:20px;">Loading proposals...</p>
    <p v-else-if="error" style="padding:20px;color:#dc2626;">{{ error }}</p>

    <!-- Table -->
    <table v-else class="proposals-table">
      <thead>
        <tr>
          <th>Document ID</th>
          <th>Proposal Title</th>
          <th>Proponent</th>
          <th>Date Submitted</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td class="document-id">{{ proposal.documentId || proposal.proposalCode || 'N/A' }}</td>
          <td>
            <strong>{{ proposal.projectTitle || proposal.title }}</strong>
            <div class="subtext">{{ proposal.programTitle || 'N/A' }}</div>
          </td>
          <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
          <td>{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>
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
          <td colspan="5" class="empty">No proposals found.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
import api from '@/utils/api'

export default {
  name: 'SubmittedProposals',
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
      return this.proposals.filter((p) => {
        const matchesSearch = this.search === '' ||
          (p.projectTitle || p.title || '').toLowerCase().includes(this.search.toLowerCase()) ||
          (p.programTitle || '').toLowerCase().includes(this.search.toLowerCase())
        const matchesStatus = this.selectedStatus === '' || p.status === this.selectedStatus
        return matchesSearch && matchesStatus
      })
    },
  },
  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals?statusIn=SUBMITTED&statusIn=REVISION')
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
.page {
  padding: var(--space-7);
  font-family: 'Inter', sans-serif;
  width: 100%;
  max-width: 100%;
}

.subtitle {
  color: var(--color-text-soft);
  margin-bottom: var(--space-6);
  font-size: var(--fs-sm);
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
  width: 100%;
}

.search {
  flex: 1;
  min-width: 280px;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  font-size: var(--fs-sm);
  font-family: inherit;
  background: var(--color-background);
  color: var(--color-text);
  outline: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.search:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.12);
}

.status-select,
.filter-btn {
  padding: var(--space-2) var(--space-5);
  border-radius: 20px;
  border: 1px solid var(--color-border);
  background: var(--color-card-background);
  color: var(--color-text);
  font-size: var(--fs-sm);
  font-family: inherit;
  min-width: 150px;
  outline: none;
  transition: border-color 0.15s ease;
}

.status-select:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(36, 82, 255, 0.12);
}

.proposals-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.proposals-table th,
.proposals-table td {
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border);
  word-wrap: break-word;
  font-size: var(--fs-sm);
  text-align: left;
}

.proposals-table th {
  background: var(--color-background-soft);
  color: var(--color-text-soft);
  font-weight: 600;
  font-size: var(--fs-xs);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.proposals-table tr:last-child td {
  border-bottom: none;
}

.subtext {
  font-size: var(--fs-xs);
  color: var(--color-text-soft);
  margin-top: 2px;
}

.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: var(--fs-xs);
  font-weight: 600;
  display: inline-block;
}

.submitted {
  background-color: rgba(245, 158, 11, 0.12);
  color: #b45309;
}

.revision {
  background-color: rgba(59, 130, 246, 0.12);
  color: #1d4ed8;
}

.endorsed {
  background-color: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.rejected {
  background-color: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.review-btn {
  background: var(--color-primary);
  color: #ffffff;
  border: none;
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-sm);
  font-size: var(--fs-xs);
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s ease;
}

.review-btn:hover {
  background: var(--color-primary-hover);
}

.empty {
  text-align: center;
  color: var(--color-text-soft);
  padding: var(--space-7);
}

.document-id {
  font-family: 'Inter', monospace;
  font-variant-numeric: tabular-nums;
  font-weight: 600;
  color: var(--color-brand-navy);
  font-size: var(--fs-xs);
}
</style>
