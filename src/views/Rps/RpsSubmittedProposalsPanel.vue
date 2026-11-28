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
        </select>
        <svg class="field-icon chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>

      <!-- Rejected Proposals: filter by the real workflow record, not the label. -->
      <div
        v-else
        class="rejection-status-filter"
        role="group"
        aria-label="Filter rejected proposals"
      >
        <button
          v-for="option in rejectionStatusOptions"
          :key="option.value"
          type="button"
          class="filter-chip"
          :class="{ active: selectedRejectionStatus === option.value }"
          :aria-pressed="selectedRejectionStatus === option.value"
          @click="selectedRejectionStatus = option.value"
        >
          {{ option.label }}
        </button>
      </div>

      <!-- In the Rejected category ORPS can narrow the queue by the office that rejected. -->
      <div v-if="category === 'rejected'" class="select-wrap">
        <svg class="field-icon filter-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"></polygon>
        </svg>
        <select v-model="selectedOffice" class="status-select" aria-label="Filter by rejecting office">
          <option value="">All Rejecting Offices</option>
          <option v-for="office in rejectionOffices" :key="office" :value="office">{{ office }}</option>
        </select>
        <svg class="field-icon chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polyline points="6 9 12 15 18 9"></polyline>
        </svg>
      </div>

      <button
        v-if="hasActiveFilters"
        type="button"
        class="clear-filters-btn"
        @click="clearFilters"
      >
        Clear filters
      </button>
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
              <th v-if="category === 'rejected'">Date Processed</th>
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
                <div v-if="category === 'rejected' && rejectionRemarks(proposal)" class="subtext rejection-remarks">
                  <strong>Reason:</strong> {{ rejectionRemarks(proposal) }}
                </div>
              </td>
              <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
              <td class="date-cell">
                {{ proposal.submittedAt?.substring(0, 10) || 'N/A' }}
              </td>
              <td v-if="category === 'rejected'" class="date-cell processed-cell">
                {{ processedDate(proposal) }}
                <div class="subtext">
                  {{ isPendingRejection(proposal) ? 'Returned to ORPS' : 'Forwarded to proponent' }}
                </div>
              </td>
              <td>
                <span :class="['status', statusTone(proposal.status)]">
                  {{ proposalStatusLabel(proposal.status) }}
                </span>
                <div v-if="category === 'rejected'" class="subtext">
                  <template v-if="isPendingRejection(proposal)">
                    Rejected by {{ rejectionOffice(proposal) }} · awaiting ORPS
                  </template>
                  <template v-else>
                    Rejected by {{ rejectionOffice(proposal) }} · forwarded to proponent
                  </template>
                </div>
              </td>
              <td>
                <button class="review-btn btn-view" @click="viewProposal(proposal)">
                  {{ isPendingRejection(proposal) ? 'Review' : 'View' }}
                </button>
              </td>
            </tr>
            <tr v-if="filteredProposals.length === 0">
              <td :colspan="category === 'rejected' ? 7 : 6" class="empty">{{ emptyMessage }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <PaginationBar
        v-if="filteredProposals.length"
        :page="page"
        :page-size="pageSize"
        :total="displayTotal"
        item-label="proposal"
        item-label-plural="proposals"
        @update:page="setPage"
        @update:page-size="setPageSize"
      />
    </div>
  </div>
</template>

<script>
import api from '@/utils/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { clampPage } from '@/composables/usePagination.js'
import { statusTone } from '@/utils/statusTone.js'
import {
  isForReviewStatus,
  proposalStatusLabel,
} from '@/utils/proposalStatus.js'

import {
  isPendingRejection,
  matchesRejectionStatus,
  rejectionOfficeOf,
  rejectionOfficesOf,
  rejectionRemarksOf,
} from '@/utils/rejectionQueue.js'

/**
 * The three status filters of the Rejected Proposals category. They map to the stored
 * workflow records, never to the badge text:
 *  - ALL: every rejection in the category (pending hand-off + finalized).
 *  - RETURNED: status RETURNED_TO_RPS with return_kind REJECTION (awaiting ORPS).
 *  - REJECTED: status REJECTED (the rejection was finalized / decided by ORPS).
 * Ordinary RETURNED_TO_RPS revision returns are excluded from this category entirely.
 */
const REJECTION_STATUS_OPTIONS = [
  { value: 'ALL', label: 'All' },
  { value: 'RETURNED', label: proposalStatusLabel('RETURNED_TO_RPS') },
  { value: 'REJECTED', label: proposalStatusLabel('REJECTED') },
]

export default {
  name: 'RpsSubmittedProposalsPanel',
  components: { PaginationBar },
  props: {
    /**
     * 'all' (default) lists the complete submitted set. 'rejected' is the Rejected
     * Proposals category: every reviewing-office rejection (RETURNED_TO_RPS with
     * return_kind REJECTION) plus the rejections ORPS has already finalized (REJECTED).
     */
    category: { type: String, default: 'all' },
  },
  data() {
    return {
      search: '',
      selectedStatus: '',
      selectedOffice: '',
      selectedRejectionStatus: 'ALL',
      rejectionStatusOptions: REJECTION_STATUS_OPTIONS,
      proposals: [],
      loading: false,
      error: null,
      hasLoaded: false,
      page: 1,
      pageSize: 10,
      serverTotalItems: 0,
      serverTotalPages: 1,
      searchTimer: null,
      suppressFetch: false,
    }
  },
  computed: {
    isRejectedCategory() {
      return this.category === 'rejected'
    },
    /** The distinct rejecting offices in the loaded rejected rows. */
    rejectionOffices() {
      return rejectionOfficesOf(this.proposals)
    },
    hasActiveFilters() {
      if (this.isRejectedCategory) {
        return Boolean(
          this.search.trim()
          || this.selectedOffice
          || this.selectedRejectionStatus !== 'ALL',
        )
      }
      return Boolean(this.search.trim() || this.selectedStatus)
    },
    emptyMessage() {
      if (this.hasActiveFilters) return 'No proposals match the selected search and filters.'
      return this.isRejectedCategory
        ? 'No rejected proposals found.'
        : 'No submitted proposals found.'
    },
    filteredProposals() {
      // The Rejected queue is still filtered locally (it is a small, dedicated queue);
      // the All Submitted list is already filtered and sliced by the server.
      if (!this.isRejectedCategory) return this.proposals
      const keyword = this.search.toLowerCase().trim()
      return this.proposals.filter((p) => {
        if (!matchesRejectionStatus(p, this.selectedRejectionStatus)) return false
        if (this.selectedOffice && this.rejectionOffice(p) !== this.selectedOffice) return false
        if (!keyword) return true
        const title = String(p.projectTitle || p.title || '').toLowerCase()
        const program = String(p.programTitle || '').toLowerCase()
        const documentId = String(p.documentId || p.proposalCode || '').toLowerCase()
        const leader = String(p.projectLeader || p.proponent?.name || '').toLowerCase()
        const office = String(p.returnedByOffice || '').toLowerCase()
        const reason = String(p.returnRemarks || p.remarks || '').toLowerCase()
        return (
          title.includes(keyword) ||
          program.includes(keyword) ||
          documentId.includes(keyword) ||
          leader.includes(keyword) ||
          office.includes(keyword) ||
          reason.includes(keyword)
        )
      })
    },
    displayTotal() {
      return this.isRejectedCategory ? this.filteredProposals.length : this.serverTotalItems
    },
    totalPages() {
      if (!this.isRejectedCategory) return Math.max(1, this.serverTotalPages)
      return Math.max(1, Math.ceil(this.filteredProposals.length / this.pageSize))
    },
    paginatedProposals() {
      // The All Submitted API already returned only the requested page.
      if (!this.isRejectedCategory) return this.filteredProposals
      const start = (this.page - 1) * this.pageSize
      return this.filteredProposals.slice(start, start + this.pageSize)
    },
    visibleProposals() {
      // Every category is paginated. The total reported by PaginationBar is the real
      // filtered total, not the number of rows on the current page.
      return this.paginatedProposals
    },
  },
  watch: {
    search() {
      if (this.suppressFetch) return
      this.page = 1
      // The All Submitted list is searched by the API; debounce typing. The rejected
      // queue filters its small local set immediately.
      if (!this.isRejectedCategory) this.scheduleFetch()
    },
    selectedStatus() {
      if (this.suppressFetch) return
      this.page = 1
      if (!this.isRejectedCategory) this.fetchProposals()
    },
    selectedOffice() {
      this.page = 1
    },
    selectedRejectionStatus() {
      this.page = 1
    },
    totalPages(count) {
      if (this.page > count) this.page = count
    },
  },
  methods: {
    statusTone,
    proposalStatusLabel,
    scheduleFetch() {
      if (this.searchTimer) clearTimeout(this.searchTimer)
      this.searchTimer = setTimeout(() => this.fetchProposals(), 250)
    },
    setPage(value) {
      const next = clampPage(value, this.totalPages)
      if (next === this.page) return
      this.page = next
      if (!this.isRejectedCategory) this.fetchProposals()
    },
    setPageSize(value) {
      this.pageSize = Math.max(1, Number(value) || 10)
      this.page = 1
      if (!this.isRejectedCategory) this.fetchProposals()
    },
    /** Restores the full category without changing the stored catalogue. */
    clearFilters() {
      this.suppressFetch = true
      this.search = ''
      this.selectedStatus = ''
      this.selectedOffice = ''
      this.selectedRejectionStatus = 'ALL'
      this.page = 1
      this.suppressFetch = false
      if (!this.isRejectedCategory) this.fetchProposals()
    },
    isPendingRejection(proposal) {
      return isPendingRejection(proposal)
    },
    /** The office that made the rejection, as recorded on the proposal. */
    rejectionOffice(proposal) {
      return rejectionOfficeOf(proposal)
    },
    /** The reason the rejecting office recorded, capped for the table row. */
    rejectionRemarks(proposal) {
      return rejectionRemarksOf(proposal)
    },
    /**
     * Date Processed, from the actual workflow event:
     *  - pending hand-off -> when the rejecting office routed it to ORPS;
     *  - finalized -> when ORPS forwarded the rejection to the proponent.
     */
    processedDate(proposal) {
      const value = isPendingRejection(proposal)
        ? proposal.rejectionRoutedAt || proposal.routedAt
        : proposal.rejectionProcessedAt || proposal.processedAt
      if (!value) return '—'
      const date = new Date(value)
      if (Number.isNaN(date.getTime())) return '—'
      return date.toLocaleString('en-US', {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
        hour: 'numeric',
        minute: '2-digit',
      })
    },
    async fetchProposals() {
      if (this.searchTimer) {
        clearTimeout(this.searchTimer)
        this.searchTimer = null
      }
      this.loading = true
      this.error = null
      try {
        const rejected = this.isRejectedCategory
        // Rejected Proposals: read from the dedicated ORPS queue endpoint. It contains
        // every reviewing-office rejection (pending hand-off + finalized) together with
        // the actual routed-at and processed-at workflow timestamps.
        // All Submitted: page/filter/search are resolved by the API so a large dataset
        // is not downloaded only to be sliced in the browser.
        const res = rejected
          ? await api.get('/api/proposals/rejections')
          : await api.get('/api/proposals/page', {
              params: {
                statusIn: ['FOR REVIEW', 'SUBMITTED', 'REVISION', 'RETURNED_TO_RPS'],
                status: this.selectedStatus || undefined,
                sort: 'submittedAt',
                search: this.search.trim() || undefined,
                page: this.page,
                size: this.pageSize,
              },
              paramsSerializer: { indexes: null },
            })
        if (rejected) {
          this.proposals = Array.isArray(res.data) ? res.data : []
          this.serverTotalItems = this.proposals.length
          this.serverTotalPages = Math.max(
            1,
            Math.ceil(this.proposals.length / this.pageSize),
          )
          this.page = 1
        } else {
          this.proposals = Array.isArray(res.data?.items) ? res.data.items : []
          this.serverTotalItems = Number(res.data?.totalItems) || 0
          this.serverTotalPages = Number(res.data?.totalPages) || 1
          this.page = Number(res.data?.page) || 1
          this.pageSize = Number(res.data?.size) || this.pageSize
        }
      } catch (err) {
        console.error(err)
        this.error = 'Failed to load proposals.'
        this.proposals = []
        this.serverTotalItems = 0
        this.serverTotalPages = 1
      } finally {
        this.loading = false
        this.hasLoaded = true
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
  beforeUnmount() {
    if (this.searchTimer) clearTimeout(this.searchTimer)
  },
  /**
   * The parent keeps each category alive, so returning to this tab after a proposal was
   * rejected or forwarded elsewhere reloads the queue instead of showing cached rows.
   */
  activated() {
    if (this.hasLoaded) this.fetchProposals()
  },
}
</script>

<style scoped>
.panel {
  width: 100%;
  font-family: var(--ui-font-ui);
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

/* Rejected Proposals status filter: real, visible chips rather than hidden tabs. */
.rejection-status-filter {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}

.filter-chip {
  padding: 6px 12px;
  border: 1px solid #dfe2ef;
  border-radius: 999px;
  background: #fff;
  color: #3b4160;
  font: inherit;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.filter-chip:hover {
  border-color: #1f1a3d;
  color: #1f1a3d;
}

.filter-chip:focus-visible {
  outline: 2px solid #1f1a3d;
  outline-offset: 2px;
}

.filter-chip.active {
  background: #1f1a3d;
  border-color: #1f1a3d;
  color: #ffdf00;
}

.clear-filters-btn {
  padding: 6px 10px;
  border: none;
  background: transparent;
  color: #6b5a3a;
  font: inherit;
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
  text-decoration: underline;
  cursor: pointer;
}

.clear-filters-btn:hover {
  color: #1f1a3d;
}

.select-wrap {
  position: relative;
  display: flex;
  align-items: center;
  flex: 0 0 auto;
}

.field-icon {
  position: absolute;
  color: var(--ui-text-muted);
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
  font-size: var(--ui-font-caption);
  font-family: inherit;
  color: #20234f;
  background: #ffffff;
  outline: none;
}

.search-input::placeholder {
  color: var(--ui-text-muted);
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
  font-size: var(--ui-font-caption);
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
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
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
  color: var(--ui-text-muted);
  font-size: var(--ui-font-table-header);
  font-weight: var(--ui-weight-semibold);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
  text-align: left;
  padding: 9px 12px;
  border-bottom: 1px solid #e3e6ef;
  white-space: nowrap;
}

.proposals-table td {
  padding: 9px 12px;
  border-bottom: 1px solid #eef0f5;
  font-size: var(--ui-font-table-cell-compact);
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
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-medium);
  color: #25245b;
  word-break: break-word;
}

.title-cell {
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-medium);
  color: #20234f;
  line-height: var(--ui-line-snug);
}

.subtext {
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
  margin-top: 2px;
}

.rejection-remarks {
  max-width: 420px;
  white-space: normal;
  line-height: var(--ui-line-snug);
  color: #6b5a3a;
}

.rejection-remarks strong {
  color: #4b3d24;
}

.date-cell {
  color: var(--ui-text-muted);
  white-space: nowrap;
}

/* The Date Processed column carries a date, time and workflow caption. */
.processed-cell {
  min-width: 150px;
  vertical-align: top;
}

/* STATUS BADGES
   Geometry only; the `.is-*` colours come from assets/status.css. */
.status {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  white-space: nowrap;
}

/* ACTION BUTTON */
.review-btn {
  border: none;
  height: 29px;
  padding: 0 12px;
  border-radius: 7px;
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
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
  color: var(--ui-text-muted);
  padding: 24px;
  font-size: var(--ui-font-caption);
}

.table-footer {
  padding: 10px 12px;
  font-size: var(--ui-font-caption);
  color: var(--ui-text-muted);
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
  font-size: var(--ui-control-font-sm);
  font-weight: var(--ui-weight-semibold);
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
