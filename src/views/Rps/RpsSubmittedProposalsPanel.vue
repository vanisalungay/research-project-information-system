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

      <div v-if="category !== 'rejected'" class="select-wrap">
        <svg class="field-icon filter-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
        </svg>
        <select v-model="selectedStatus" class="status-select">
          <option value="">All Status</option>
          <option value="FOR REVIEW">{{ proposalStatusLabel('FOR REVIEW') }}</option>
          <option value="REVISION">{{ proposalStatusLabel('REVISION') }}</option>
          <option value="ENDORSED">{{ proposalStatusLabel('ENDORSED') }}</option>
          <option value="REJECTED">{{ proposalStatusLabel('REJECTED') }}</option>
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
            <tr v-for="proposal in visibleProposals" :key="proposal.id">
              <td class="document-id">{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
              <td>
                <div class="title-cell">{{ proposal.projectTitle || proposal.title }}</div>
                <div class="subtext">{{ proposal.programTitle || 'N/A' }}</div>
              </td>
              <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
              <td class="date-cell">
                {{ proposal.submittedAt?.substring(0, 10) || 'N/A' }}
                <div
                  v-if="category === 'rejected' && proposal.returnedAt"
                  class="subtext"
                >
                  Returned {{ String(proposal.returnedAt).substring(0, 10) }}
                </div>
              </td>
              <td>
                <span :class="['status', statusTone(proposal.status)]">
                  {{ proposalStatusLabel(proposal.status) }}
                </span>
                <div v-if="category === 'rejected'" class="subtext">Rejected by OVCRIGE</div>
              </td>
              <td>
                <button class="review-btn btn-view" @click="viewProposal(proposal)">Review</button>
              </td>
            </tr>
            <tr v-if="filteredProposals.length === 0">
              <td colspan="6" class="empty">
                {{ search || selectedStatus
                  ? 'No proposals match your search.'
                  : category === 'rejected'
                    ? 'No rejected proposals found.'
                    : 'No submitted proposals found.' }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="category === 'rejected'" class="table-footer pager">
        <span>
          Showing {{ rangeStart }}–{{ rangeEnd }} of {{ filteredProposals.length }}
          rejected proposal{{ filteredProposals.length === 1 ? '' : 's' }}
        </span>
        <div v-if="totalPages > 1" class="pager-controls">
          <button type="button" :disabled="page === 1" @click="page--">&lsaquo;</button>
          <button
            v-for="pg in totalPages"
            :key="pg"
            type="button"
            class="pager-num"
            :class="{ active: pg === page }"
            @click="page = pg"
          >
            {{ pg }}
          </button>
          <button type="button" :disabled="page === totalPages" @click="page++">&rsaquo;</button>
        </div>
      </div>
      <div v-else class="table-footer">
        Showing {{ filteredProposals.length }} proposal{{ filteredProposals.length === 1 ? '' : 's' }}
      </div>
    </div>
  </div>
</template>

<script>
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import {
  isForReviewStatus,
  proposalStatusLabel,
  proposalStatusMatches,
} from '@/utils/proposalStatus.js'

/**
 * A proposal OVCRIGE rejected and returned to ORPS. It shares RETURNED_TO_RPS with
 * ordinary revision returns, so return_kind is what separates the two: only a
 * REJECTION return belongs in the Rejected Proposals category.
 */
const isOvcrigeRejection = (proposal) =>
  String(proposal?.status || '').toUpperCase() === 'RETURNED_TO_RPS' &&
  String(proposal?.returnKind || '').toUpperCase() === 'REJECTION'

export default {
  name: 'RpsSubmittedProposalsPanel',
  props: {
    /**
     * 'all' (default) lists the complete submitted set. 'rejected' is the Rejected
     * Proposals category: the same table, restricted to proposals OVCRIGE rejected and
     * returned to ORPS (status RETURNED_TO_RPS, returnedByOffice OVCRIGE).
     */
    category: { type: String, default: 'all' },
  },
  data() {
    return {
      search: '',
      selectedStatus: '',
      proposals: [],
      loading: false,
      error: null,
      page: 1,
      pageSize: 10,
    }
  },
  computed: {
    isRejectedCategory() {
      return this.category === 'rejected'
    },
    filteredProposals() {
      const keyword = this.search.toLowerCase().trim()
      const rejectedOnly = this.isRejectedCategory
      return this.proposals.filter((p) => {
        if (rejectedOnly && !isOvcrigeRejection(p)) return false
        const matchesStatus = proposalStatusMatches(p.status, this.selectedStatus)
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
    totalPages() {
      return Math.max(1, Math.ceil(this.filteredProposals.length / this.pageSize))
    },
    paginatedProposals() {
      const start = (this.page - 1) * this.pageSize
      return this.filteredProposals.slice(start, start + this.pageSize)
    },
    visibleProposals() {
      // The complete Submitted list keeps its existing one-page behaviour; the new
      // Rejected category pages its own, usually shorter, list.
      return this.isRejectedCategory ? this.paginatedProposals : this.filteredProposals
    },
    rangeStart() {
      return this.filteredProposals.length ? (this.page - 1) * this.pageSize + 1 : 0
    },
    rangeEnd() {
      return Math.min(this.page * this.pageSize, this.filteredProposals.length)
    },
  },
  watch: {
    search() {
      this.page = 1
    },
    selectedStatus() {
      this.page = 1
    },
    totalPages(count) {
      if (this.page > count) this.page = count
    },
  },
  methods: {
    statusTone,
    proposalStatusLabel,
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const rejected = this.isRejectedCategory
        // Rejected Proposals: returned to ORPS by OVCRIGE, newest rejection first.
        // The Submitted list keeps its original statuses and submission ordering.
        const res = await api.get('/api/proposals', {
          params: rejected
            ? { statusIn: ['RETURNED_TO_RPS'], sort: 'returnedAt' }
            : {
                statusIn: ['FOR REVIEW', 'SUBMITTED', 'REVISION', 'RETURNED_TO_RPS'],
                // Server-side ordering by the real submission event (not created_at or
                // updated_at). The server also breaks equal timestamps by proposal id and
                // puts records with no submission timestamp last.
                sort: 'submittedAt',
              },
          paramsSerializer: { indexes: null },
        })
        this.proposals = Array.isArray(res.data) ? res.data : []
        this.page = 1
      } catch (err) {
        console.error(err)
        this.error = 'Failed to load proposals.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },
    viewProposal(proposal) {
      if (isForReviewStatus(proposal.status)) {
        // Newly submitted (FOR REVIEW, or legacy SUBMITTED) proposals go to the review
        // page with endorse/return/reject actions.
        this.$router.push({ name: 'ProposalReview', params: { id: proposal.id } })
      } else if (proposal.status === 'RETURNED_TO_RPS') {
        this.$router.push({ name: 'RpsForwardRevision', params: { id: proposal.id } })
      } else if (proposal.status === 'REVISION' || proposal.status === 'RPS_RETURNED') {
        // A proposal that is back with the proponent is read, not actioned, by RPS: it
        // opens the same details page as the notification and every other listing, so
        // the header and history are the canonical ones.
        this.$router.push({ name: 'ProposalDetails', params: { id: proposal.id } })
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
.review-btn {
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

.review-btn:focus-visible {
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

/* Rejected category pager — same compact scale as the table. */
.pager {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.pager-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pager-controls button {
  min-width: 24px;
  height: 24px;
  padding: 0 6px;
  border: 1px solid #e0e4ef;
  border-radius: 6px;
  background: #ffffff;
  color: #25245b;
  font-family: inherit;
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
}

.pager-controls button:hover:not(:disabled) {
  background: #f4f6fb;
}

.pager-controls button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.pager-controls .pager-num.active {
  background: #25245b;
  border-color: #25245b;
  color: #ffd400;
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
