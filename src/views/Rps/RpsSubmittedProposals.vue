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
          <th>Proposal Title</th>
          <th>Proponent</th>
          <th>Date Submitted</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>
            <strong>{{ proposal.projectTitle || proposal.title }}</strong>
            <div class="subtext">{{ proposal.programTitle || 'N/A' }}</div>
          </td>
          <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
          <td>{{ proposal.createdAt?.substring(0,10) || 'N/A' }}</td>
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
  padding: 32px;
  font-family: Arial, sans-serif;
  width: 100%;
  max-width: 100%;
}
.subtitle {
  color: #666;
  margin-bottom: 24px;
}
.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 24px;
  width: 100%;
}
.search {
  flex: 1;
  min-width: 280px;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid #ccc;
}
.status-select, .filter-btn {
  padding: 10px 22px;
  border-radius: 20px;
  border: 1px solid #ccc;
  background: white;
  min-width: 150px;
}
.proposals-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.proposals-table th, .proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  word-wrap: break-word;
}
.subtext {
  font-size: 12px;
  color: #777;
}
.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}
.submitted { background-color: #ffe2a8; color: #8a5b00; }
.revision { background-color: #d6e4ff; color: #2a4dbf; }
.endorsed { background-color: #d9f5e5; color: #1e7f4f; }
.rejected { background-color: #ffd6d6; color: #a10000; }
.review-btn {
  background-color: #5da9ff;
  color: white;
  border: none;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
}
.empty {
  text-align: center;
  color: #777;
}
</style>