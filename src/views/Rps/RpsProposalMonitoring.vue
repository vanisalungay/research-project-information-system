<template>
  <div class="page">
    <!-- Page heading -->
    <header class="monitoring-header">
      <h1 class="monitoring-title">Proposal Monitoring Dashboard</h1>
      <p class="monitoring-subtitle">
        Track the end-to-end progress of submitted research proposals across all offices.
      </p>
    </header>

    <!-- Search & filters -->
    <section class="filter-panel" aria-label="Proposal filters">
      <div class="field field-search">
        <label for="search-input">Search Proposals</label>
        <div class="search-control">
          <svg class="search-icon" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
            <circle cx="11" cy="11" r="7" />
            <path d="m20 20-3.4-3.4" />
          </svg>
          <input
            id="search-input"
            v-model="searchQuery"
            type="text"
            class="control"
            placeholder="Search by title, leader, or proposal ID..."
            autocomplete="off"
          />
        </div>
      </div>

      <div class="field">
        <label for="office-filter">Current Office</label>
        <select
          id="office-filter"
          v-model="selectedOffice"
          class="control select-control"
        >
          <option value="">All offices</option>
          <option value="Proponent">Proponent</option>
          <option value="RPS">RPS</option>
          <option value="OVCRIGE">OVCRIGE</option>
          <option value="REC">REC</option>
          <option value="OC">OC</option>
          <option value="OVCAF">OVCAF</option>
          <option value="Implementation">Implementation</option>
          <option value="Ongoing Implementation">Ongoing Implementation</option>
          <option value="Completed">Completed</option>
        </select>
      </div>

      <div class="field">
        <label for="status-filter">Current Status</label>
        <select
          id="status-filter"
          v-model="selectedStatus"
          class="control select-control"
        >
          <option value="">All statuses</option>
          <option value="FOR REVIEW">{{ proposalStatusLabel('FOR REVIEW') }}</option>
          <option value="ENDORSED">{{ proposalStatusLabel('ENDORSED') }}</option>
          <option value="UNDER_REVIEW">{{ proposalStatusLabel('UNDER_REVIEW') }}</option>
          <option value="APPROVED">{{ proposalStatusLabel('APPROVED') }}</option>
          <option value="COMPLETED">{{ proposalStatusLabel('COMPLETED') }}</option>
          <option value="RPS_RETURNED">{{ proposalStatusLabel('RPS_RETURNED') }}</option>
          <option value="REJECTED">{{ proposalStatusLabel('REJECTED') }}</option>
          <option value="READY_FOR_RELEASE">{{ proposalStatusLabel('READY_FOR_RELEASE') }}</option>
          <option value="RELEASED">{{ proposalStatusLabel('RELEASED') }}</option>
        </select>
      </div>

      <div class="field field-actions">
        <button type="button" class="clear-filters" @click="clearFilters">
          Clear filters
        </button>
      </div>
    </section>

    <!-- Loading skeleton -->
    <div
      v-if="loading"
      class="proposal-grid"
      aria-busy="true"
      aria-live="polite"
    >
      <span class="sr-only">Loading proposals...</span>
      <div
        v-for="n in 6"
        :key="n"
        class="proposal-card skeleton-card"
        aria-hidden="true"
      >
        <div class="skeleton-row">
          <span class="skeleton skeleton-id"></span>
          <span class="skeleton skeleton-badge"></span>
        </div>
        <span class="skeleton skeleton-title"></span>
        <span class="skeleton skeleton-title skeleton-title-short"></span>
        <div class="skeleton-row skeleton-proponent">
          <span class="skeleton skeleton-avatar"></span>
          <span class="skeleton skeleton-name"></span>
        </div>
        <div class="skeleton-row skeleton-office">
          <span class="skeleton skeleton-office-label"></span>
          <span class="skeleton skeleton-percent"></span>
        </div>
        <span class="skeleton skeleton-bar"></span>
        <div class="skeleton-row skeleton-footer">
          <span class="skeleton skeleton-date"></span>
          <span class="skeleton skeleton-button"></span>
        </div>
      </div>
    </div>

    <!-- API error -->
    <div v-else-if="error" class="state-card">
      <p class="error-text">{{ error }}</p>
      <button type="button" class="retry-btn" @click="fetchProposals">Retry</button>
    </div>

    <template v-else>
      <!-- Empty state -->
      <div v-if="filteredProposals.length === 0" class="empty-state">
        <span class="empty-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" focusable="false">
            <path d="M4 7a2 2 0 0 1 2-2h4l2 2h6a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2Z" />
            <path d="M9.5 13.5h5" />
          </svg>
        </span>
        <h2>No Proposals Found</h2>
        <p>There are no proposals matching your search or selected filters.</p>
      </div>

      <!-- Proposal cards -->
      <div v-else class="proposal-grid">
        <article
          v-for="prop in filteredProposals"
          :key="prop.id ?? prop.documentId"
          class="proposal-card"
        >
          <div class="card-top">
            <span class="proposal-id" :title="displayProposalId(prop)">
              {{ displayProposalId(prop) }}
            </span>
            <span
              :class="['status-badge', statusTone(prop.status)]"
              :title="formatStatus(prop.status)"
            >
              <span class="status-dot" aria-hidden="true"></span>
              <span class="status-text">{{ formatStatus(prop.status) }}</span>
            </span>
          </div>

          <h2 class="proposal-title" :title="displayTitle(prop)">
            {{ displayTitle(prop) }}
          </h2>

          <div class="proponent-row">
            <span class="avatar" aria-hidden="true">{{ initials(prop) }}</span>
            <span class="proponent-name" :title="proponentName(prop)">
              {{ proponentName(prop) }}
            </span>
            <span
              v-if="collegeAbbrev(prop)"
              class="college-pill"
              :title="collegeLabel(prop)"
            >
              {{ collegeAbbrev(prop) }}
            </span>
          </div>

          <div class="progress-block">
            <div class="progress-meta">
              <span class="office-text" :title="'At: ' + getCurrentOffice(prop)">
                At: {{ getCurrentOffice(prop) }}
              </span>
              <span class="progress-value">{{ progressPercent(prop) }}%</span>
            </div>
            <div
              class="progress-track"
              role="progressbar"
              :aria-valuenow="progressPercent(prop)"
              aria-valuemin="0"
              aria-valuemax="100"
              :aria-label="'Workflow progress for ' + displayProposalId(prop)"
            >
              <span
                class="progress-fill"
                :style="{
                  width: progressPercent(prop) + '%',
                  backgroundColor: progressColor(prop)
                }"
              ></span>
            </div>
          </div>

          <footer class="card-footer">
            <div class="submitted">
              <span class="submitted-label">Submitted</span>
              <span class="submitted-date">{{ formatDate(prop.createdAt) }}</span>
            </div>
            <button
              type="button"
              class="view-details"
              @click="viewMonitoring(prop.id)"
            >
              <span>View Details</span>
              <svg class="view-arrow" viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M5 12h14" />
                <path d="m13 6 6 6-6 6" />
              </svg>
            </button>
          </footer>
        </article>
      </div>
    </template>
  </div>
</template>

<script>
import api from '@/utils/api'
import { statusTone, statusColor } from '@/utils/statusTone.js'
import {
  proposalStatusLabel,
  proposalStatusMatches,
  normalizeProposalStatus,
} from '@/utils/proposalStatus.js'

const PROPONENT_FALLBACK = 'Proponent unavailable'

export default {
  name: 'ProposalMonitoringList',

  data() {
    return {
      searchQuery: '',
      selectedOffice: '',
      selectedStatus: '',
      proposals: [],
      loading: false,
      error: null,
    }
  },

  computed: {
    filteredProposals() {
      // Drafts belong to the Proponent's workspace and never enter monitoring:
      // excluding them here also keeps them out of search, filters and the grid.
      let result = this.proposals.filter(
        (p) => normalizeProposalStatus(p.status) !== 'DRAFT',
      )

      const term = this.searchQuery.toLowerCase().trim()
      if (term) {
        result = result.filter(p =>
          String(p.projectTitle || '').toLowerCase().includes(term) ||
          String(p.projectLeader || p.proponent?.name || '').toLowerCase().includes(term) ||
          String(p.id).includes(term) ||
          String(p.documentId || p.proposalCode || '').toLowerCase().includes(term)
        )
      }

      if (this.selectedOffice) {
        result = result.filter(p => this.getCurrentOffice(p) === this.selectedOffice)
      }

      if (this.selectedStatus) {
        result = result.filter(p => proposalStatusMatches(p.status, this.selectedStatus))
      }

      result.sort((a, b) => {
        return new Date(b.createdAt || 0) - new Date(a.createdAt || 0)
      })

      return result
    }
  },

  methods: {
    async fetchProposals() {
      this.loading = true
      this.error = null
      try {
        const res = await api.get('/api/proposals')
        const rows = Array.isArray(res.data) ? res.data : []
        const validRows = rows.filter((p) => p && typeof p === 'object' && !Array.isArray(p))
        if (validRows.length !== rows.length) {
          console.warn(`Skipped ${rows.length - validRows.length} malformed proposal record(s).`)
        }
        this.proposals = validRows
      } catch (err) {
        console.error('Failed to fetch proposals:', err)
        this.error = 'Failed to load proposals. Ensure the backend server is running.'
        this.proposals = []
      } finally {
        this.loading = false
      }
    },

    getCurrentOffice(proposal) {
      const status = normalizeProposalStatus(proposal?.status)
      const officeMap = {
        'DRAFT': 'Proponent',
        'FOR REVIEW': 'RPS',
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
        'APPROVED': 'Ongoing Implementation',
        'READY_FOR_RELEASE': 'OVCAF',
        'RELEASED': 'Implementation',
        'IMPLEMENTATION': 'Implementation',
        'COMPLETED': 'Completed',
        'REJECTED': 'RPS',
        'RETURNED': 'OVCAF',
      }
      return officeMap[status] || 'RPS'
    },

    formatStatus(status) {
      return proposalStatusLabel(status)
    },

    getProgressPercentage(proposal) {
      const status = normalizeProposalStatus(proposal?.status)
      const progressMap = {
        'DRAFT': 5,
        'FOR REVIEW': 15,
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

    progressPercent(proposal) {
      const value = Number(this.getProgressPercentage(proposal))
      if (!Number.isFinite(value)) return 0
      return Math.max(0, Math.min(100, Math.round(value)))
    },

    // The progress colour comes from the centralized status palette, so the bar
    // reflects the current workflow stage without a second colour system.
    progressColor(proposal) {
      return statusColor(proposal?.status)
    },

    displayProposalId(proposal) {
      const stored = proposal?.documentId ?? proposal?.proposalCode
      const text = stored == null ? '' : String(stored).trim()
      if (text) return text
      const id = proposal?.id
      return id == null || String(id).trim() === '' ? '—' : `#${id}`
    },

    displayTitle(proposal) {
      const title = proposal?.projectTitle
      const text = typeof title === 'string' ? title.trim() : ''
      return text || 'Untitled Proposal'
    },

    rawProponentName(proposal) {
      const leader = proposal?.projectLeader
      const leaderText = typeof leader === 'string' ? leader.trim() : ''
      if (leaderText) return leaderText

      const owner = proposal?.proponent?.name
      return typeof owner === 'string' ? owner.trim() : ''
    },

    proponentName(proposal) {
      return this.rawProponentName(proposal) || PROPONENT_FALLBACK
    },

    initials(proposal) {
      const name = this.rawProponentName(proposal)
      if (!name) return '—'

      const words = name.split(/\s+/).filter((word) => /[A-Za-z0-9]/.test(word))
      if (words.length === 0) return '—'
      if (words.length === 1) return words[0].slice(0, 2).toUpperCase()
      return (words[0].charAt(0) + words[1].charAt(0)).toUpperCase()
    },

    collegeLabel(proposal) {
      const college = proposal?.college
      return typeof college === 'string' ? college.trim() : ''
    },

    collegeAbbrev(proposal) {
      const college = this.collegeLabel(proposal)
      if (!college) return ''

      // Many records already store the acronym in parentheses, e.g.
      // "College of Business and Information Technology (CBIT)".
      const parenthesized = college.match(/\(([^)]+)\)\s*$/)
      if (parenthesized) return parenthesized[1].trim()

      const words = college.split(/\s+/).filter(Boolean)
      if (words.length === 1) return college

      const meaningful = words.filter(
        (word) => !/^(of|and|the|for|in|&)$/i.test(word),
      )
      const acronym = meaningful.map((word) => word.charAt(0)).join('').toUpperCase()
      return acronym || college
    },

    statusTone,
    proposalStatusLabel,
    proposalStatusMatches,

    formatDate(dateString) {
      if (!dateString) return 'N/A'
      const date = new Date(dateString)
      if (Number.isNaN(date.getTime())) return 'N/A'
      return date.toLocaleDateString('en-US', {
        year: 'numeric', month: 'short', day: 'numeric'
      })
    },

    viewMonitoring(id) {
      if (id == null || String(id).trim() === '') return
      this.$router.push({ name: 'ProposalDetails', params: { id: id } })
    },

    clearFilters() {
      this.searchQuery = ''
      this.selectedOffice = ''
      this.selectedStatus = ''
    },
  },

  mounted() {
    this.fetchProposals()
  }
}
</script>

<style scoped>
.page,
.page *,
.page *::before,
.page *::after {
  box-sizing: border-box;
}

.page {
  width: 100%;
  max-width: 100%;
  padding: 2px 0 16px;
  color: #1f2a44;
  font-family: 'Inter', 'Segoe UI', sans-serif;
}

/* ===== Heading ===== */
/* These class names are page-specific: a few other views ship unscoped
   `.page-header` / `.page-title` rules, so generic names would inherit their
   dark heading block and flex layout here. */
.monitoring-header {
  display: block;
  margin: 0 0 16px;
}

.monitoring-title {
  display: block;
  width: auto;
  margin: 0;
  padding: 0;
  background: transparent;
  color: #101c3f;
  font-size: 23px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.01em;
}

.monitoring-subtitle {
  margin: 5px 0 0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

/* ===== Filter panel ===== */
.filter-panel {
  display: grid;
  grid-template-columns:
    minmax(0, 2.4fr)
    minmax(150px, 0.85fr)
    minmax(160px, 0.95fr)
    auto;
  align-items: end;
  gap: 12px;
  margin-bottom: 16px;
  padding: 14px 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.03),
    0 8px 24px rgba(15, 23, 42, 0.025);
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.field label {
  color: #6b7892;
  font-size: 10px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: 0.055em;
  text-transform: uppercase;
}

.control {
  width: 100%;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dbe1ee;
  border-radius: 8px;
  outline: none;
  background: #ffffff;
  color: #26324c;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.2;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.control::placeholder {
  color: #98a3b8;
}

.control:focus {
  border-color: #8da6f8;
  box-shadow: 0 0 0 3px rgba(49, 88, 217, 0.1);
}

.search-control {
  position: relative;
}

.search-icon {
  position: absolute;
  top: 50%;
  left: 12px;
  width: 14px;
  height: 14px;
  transform: translateY(-50%);
  fill: none;
  stroke: #8a97ad;
  stroke-width: 2;
  stroke-linecap: round;
  stroke-linejoin: round;
  pointer-events: none;
}

.search-control .control {
  padding-left: 34px;
}

.select-control {
  appearance: none;
  -webkit-appearance: none;
  padding-right: 32px;
  cursor: pointer;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%236b7892' stroke-width='2.4' stroke-linecap='round' stroke-linejoin='round'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 11px center;
}

.field-actions {
  align-self: end;
  flex-direction: row;
  align-items: flex-end;
}

.clear-filters {
  height: 40px;
  padding: 0 14px;
  border: 1px solid #dfe4ef;
  border-radius: 8px;
  background: #ffffff;
  color: #1c2a4a;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease;
}

.clear-filters:hover {
  background: #f7f8fc;
  border-color: #cdd6e7;
}

.clear-filters:focus-visible {
  outline: 2px solid #8da6f8;
  outline-offset: 2px;
}

/* ===== Card grid ===== */
.proposal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: stretch;
}

.proposal-card {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 242px;
  padding: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.035),
    0 6px 18px rgba(15, 23, 42, 0.02);
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease;
}

.proposal-card:hover {
  border-color: #d5ddef;
  box-shadow:
    0 2px 4px rgba(15, 23, 42, 0.04),
    0 10px 24px rgba(15, 23, 42, 0.04);
}

.card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-width: 0;
}

.proposal-id {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #8a96ab;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 9.5px;
  letter-spacing: 0.015em;
  white-space: nowrap;
  text-overflow: ellipsis;
}

/* Geometry only; the tone colours come from assets/status.css. */
.status-badge {
  display: inline-flex;
  align-items: center;
  flex: 0 0 auto;
  gap: 5px;
  max-width: 65%;
  min-width: 0;
  overflow: hidden;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 9.5px;
  font-weight: 700;
  line-height: 1.4;
  white-space: nowrap;
}

.status-dot {
  flex: 0 0 auto;
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
  opacity: 0.9;
}

.status-text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.proposal-title {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  min-height: 2.8em;
  margin: 11px 0 0;
  overflow: hidden;
  color: #142142;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.4;
}

.proponent-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  margin-top: 10px;
}

.avatar {
  display: inline-flex;
  flex: 0 0 26px;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: #e9eeff;
  color: #3158d9;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.proponent-name {
  flex: 1 1 auto;
  min-width: 0;
  overflow: hidden;
  color: #5d6b85;
  font-size: 10.5px;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.college-pill {
  flex: 0 0 auto;
  max-width: 36%;
  overflow: hidden;
  padding: 2px 7px;
  border-radius: 999px;
  background: #f2f5fb;
  color: #68758c;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.03em;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-transform: uppercase;
}

.progress-block {
  margin-top: 13px;
}

.progress-meta {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  color: #5f6d87;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.35;
}

.office-text {
  min-width: 0;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.progress-value {
  flex: 0 0 auto;
  color: #253452;
  font-variant-numeric: tabular-nums;
}

.progress-track {
  width: 100%;
  height: 4px;
  margin-top: 7px;
  overflow: hidden;
  border-radius: 999px;
  background: #edf0f7;
}

.progress-fill {
  display: block;
  height: 100%;
  border-radius: inherit;
  transition: width 0.3s ease;
}

.card-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 10px;
  margin-top: auto;
  padding-top: 13px;
  border-top: 1px solid #eef1f7;
}

.submitted {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.submitted-label {
  color: #97a2b5;
  font-size: 9px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.submitted-date {
  color: #1c2a4a;
  font-size: 10.5px;
  font-weight: 600;
  white-space: nowrap;
}

.view-details {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: #e8edff;
  color: #3158d9;
  font-family: inherit;
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.view-details:hover {
  background: #dde5ff;
  color: #244ad0;
}

.view-details:focus-visible {
  outline: 2px solid #7d97ec;
  outline-offset: 2px;
}

.view-arrow {
  width: 12px;
  height: 12px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2.2;
  stroke-linecap: round;
  stroke-linejoin: round;
}

/* ===== Empty / error states ===== */
.empty-state,
.state-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 320px;
  padding: 48px 24px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  text-align: center;
  box-shadow:
    0 1px 2px rgba(15, 23, 42, 0.03),
    0 8px 24px rgba(15, 23, 42, 0.02);
}

.empty-icon {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  margin-bottom: 12px;
  border-radius: 14px;
  background: #eef2ff;
  color: #5c74b8;
}

.empty-icon svg {
  width: 22px;
  height: 22px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.empty-state h2 {
  margin: 0;
  color: #17233f;
  font-size: 15px;
  font-weight: 700;
}

.empty-state p {
  max-width: 340px;
  margin: 6px 0 0;
  color: #74819a;
  font-size: 11.5px;
  line-height: 1.5;
}

.error-text {
  margin: 0 0 14px;
  color: #b42318;
  font-size: 12.5px;
  line-height: 1.5;
}

.retry-btn {
  height: 36px;
  padding: 0 16px;
  border: none;
  border-radius: 8px;
  background: #3158d9;
  color: #ffffff;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.15s ease;
}

.retry-btn:hover {
  background: #244ad0;
}

.retry-btn:focus-visible {
  outline: 2px solid #8da6f8;
  outline-offset: 2px;
}

/* ===== Loading skeleton ===== */
.skeleton-card {
  pointer-events: none;
}

.skeleton {
  display: block;
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    #eef1f7 25%,
    #f7f9fc 37%,
    #eef1f7 63%
  );
  background-size: 400% 100%;
  animation: monitoring-shimmer 1.4s ease infinite;
}

.skeleton-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.skeleton-id {
  width: 45%;
  height: 8px;
}

.skeleton-badge {
  width: 32%;
  height: 16px;
  border-radius: 999px;
}

.skeleton-title {
  width: 100%;
  height: 10px;
  margin-top: 12px;
}

.skeleton-title-short {
  width: 72%;
  margin-top: 7px;
}

.skeleton-proponent {
  justify-content: flex-start;
  margin-top: 12px;
}

.skeleton-avatar {
  flex: 0 0 26px;
  width: 26px;
  height: 26px;
  border-radius: 50%;
}

.skeleton-name {
  width: 50%;
  height: 8px;
}

.skeleton-office {
  margin-top: 14px;
}

.skeleton-office-label {
  width: 35%;
  height: 8px;
}

.skeleton-percent {
  width: 12%;
  height: 8px;
}

.skeleton-bar {
  width: 100%;
  height: 4px;
  margin-top: 8px;
  border-radius: 999px;
}

.skeleton-footer {
  margin-top: auto;
  padding-top: 13px;
  border-top: 1px solid #eef1f7;
}

.skeleton-date {
  width: 30%;
  height: 9px;
}

.skeleton-button {
  width: 30%;
  height: 28px;
  border-radius: 8px;
}

@keyframes monitoring-shimmer {
  0% {
    background-position: 100% 0;
  }
  100% {
    background-position: 0 0;
  }
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* ===== Responsive ===== */
@media (max-width: 1199px) {
  .proposal-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1100px) {
  .filter-panel {
    grid-template-columns:
      minmax(0, 1.6fr)
      minmax(140px, 1fr)
      minmax(150px, 1fr)
      auto;
  }
}

@media (max-width: 900px) {
  .filter-panel {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .field-search {
    grid-column: 1 / -1;
  }

  .field-actions {
    grid-column: 1 / -1;
  }

  .clear-filters {
    width: 100%;
  }
}

@media (max-width: 767px) {
  .monitoring-title {
    font-size: 20px;
  }

  .monitoring-subtitle {
    font-size: 11.5px;
  }

  .proposal-grid {
    grid-template-columns: minmax(0, 1fr);
    gap: 14px;
  }

  .proposal-card {
    min-height: 0;
    padding: 14px;
  }
}

@media (max-width: 520px) {
  .filter-panel {
    grid-template-columns: minmax(0, 1fr);
    gap: 10px;
    padding: 12px;
  }

  .field-search,
  .field-actions {
    grid-column: auto;
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton {
    animation: none;
  }

  .progress-fill {
    transition: none;
  }
}
</style>
