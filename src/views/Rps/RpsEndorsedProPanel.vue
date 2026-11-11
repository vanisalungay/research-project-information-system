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
    </div>

    <!-- Loading -->
    <div v-if="loading" class="state-box">Loading endorsed proposals...</div>

    <!-- Error -->
    <div v-else-if="error" class="state-box error-text">
      <span>{{ error }}</span>
      <button class="retry-btn btn-secondary" @click="fetchProposals">Retry</button>
    </div>

    <!-- Table -->
    <div v-else class="table-card">
      <div class="table-scroll">
        <table class="proposals-table">
          <thead>
            <tr>
              <th class="col-id">Document ID</th>
              <th>Proposal Title</th>
              <th>Project Leader</th>
              <th>College</th>
              <th>Status</th>
              <th class="col-actions">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="proposal in filteredProposals" :key="proposal.id">
              <td class="document-id">{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
              <td>
                <div class="title-cell">{{ proposal.projectTitle || 'Untitled' }}</div>
              </td>
              <td>{{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}</td>
              <td>{{ proposal.college || 'N/A' }}</td>
              <td>
                <span class="status" :class="statusTone(proposal.status)">
                  {{ formatStatus(proposal.status) }}
                </span>
              </td>
              <td>
                <button class="view-btn btn-view" @click="viewProposal(proposal)">View</button>
              </td>
            </tr>
            <tr v-if="filteredProposals.length === 0">
              <td colspan="6" class="empty">
                {{ search ? 'No proposals match your search.' : 'No endorsed proposals found.' }}
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
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

export default {
  name: 'RpsEndorsedProPanel',
  data() {
    return {
      search: '',
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
        String(proposal.documentId || proposal.proposalCode || proposal.id).toLowerCase().includes(keyword)
      )
    },
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
      return proposalStatusLabel(status)
    },

    statusTone,
  },
  mounted() {
    this.fetchProposals()
  }
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

/* STATES */
.state-box {
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 0 0 10px 10px;
  padding: 22px;
  font-size: 12px;
  color: #78809a;
  display: flex;
  align-items: center;
  gap: 12px;
}

.error-text {
  color: #dc2626;
}

/* Layout only; the action colours are global (assets/actions.css). */
.retry-btn {
  padding: 6px 16px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
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

/* STATUS BADGES
   Geometry only; the `.is-*` colours come from assets/status.css. */
.status {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 9px;
  font-weight: 600;
  white-space: nowrap;
}

/* ACTION BUTTON */
.view-btn {
  height: 29px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: 11px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s ease;
}

.view-btn:focus-visible {
  outline: 2px solid var(--btn-focus-outline);
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
</style>
