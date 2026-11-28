<template>
  <div class="end-page end-page--fluid ovcaf-quarterly-page">
    <header class="end-page-header">
      <h2>Quarterly Reports</h2>
      <p class="end-subtitle">
        Submitted reports of implementation-approved projects whose budget OVCAF processed
      </p>
    </header>

    <div class="end-card ovcaf-quarterly-card">
      <!-- Filters. All values are applied by the server to the actual records. -->
      <div class="end-toolbar ovcaf-toolbar">
        <input
          v-model="filters.search"
          type="text"
          class="end-input ovcaf-search"
          placeholder="Search by proposal ID, project, proponent or college..."
          @input="onSearchInput"
        />
        <select v-model="filters.period" class="end-select" @change="onFilterChange">
          <option value="All">All Periods</option>
          <option v-for="period in options.periods" :key="period" :value="period">
            {{ periodLabelFor(period) }}
          </option>
        </select>
        <select v-model="filters.college" class="end-select" @change="onFilterChange">
          <option value="All">All Colleges</option>
          <option v-for="college in options.colleges" :key="college" :value="college">
            {{ college }}
          </option>
        </select>
        <select v-model="filters.program" class="end-select" @change="onFilterChange">
          <option value="All">All Programs</option>
          <option v-for="program in options.programs" :key="program" :value="program">
            {{ program }}
          </option>
        </select>
        <select v-model="filters.projectId" class="end-select ovcaf-project-select" @change="onFilterChange">
          <option value="All">All Projects</option>
          <option v-for="project in options.projects" :key="project.id" :value="String(project.id)">
            {{ project.documentId || '—' }} — {{ project.projectTitle }}
          </option>
        </select>
        <select v-model="filters.sortSelection" class="end-select ovcaf-sort" @change="onSortChange">
          <option value="submittedAt-desc">Newest submitted</option>
          <option value="submittedAt-asc">Oldest submitted</option>
          <option value="projectTitle-asc">Project title</option>
          <option value="college-asc">College</option>
          <option value="period-asc">Reporting period</option>
        </select>
        <button type="button" class="end-btn is-link clear-btn" @click="clearFilters">Clear All</button>
      </div>

      <p v-if="loading" class="ovcaf-state">Loading quarterly reports...</p>
      <div v-else-if="error" class="ovcaf-error" role="alert">
        <span>{{ error }}</span>
        <button type="button" class="end-btn is-view retry-btn" @click="fetchReports">Retry</button>
      </div>

      <template v-else>
        <div class="end-table-scroll" role="region" aria-label="Quarterly reports" tabindex="0">
          <table class="end-table ovcaf-report-table">
            <thead>
              <tr>
                <th>Proposal ID</th>
                <th>Project Title</th>
                <th>Proponent</th>
                <th>College</th>
                <th>Period</th>
                <th>Date Submitted</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody v-if="rows.length === 0">
              <tr>
                <td colspan="8" class="ovcaf-empty">
                  No submitted quarterly reports match the current filters.
                </td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="row in rows" :key="row.id">
                <td class="cell-id" :title="row.documentId">{{ row.documentId || '—' }}</td>
                <td class="cell-title">
                  <span class="title-text" :title="row.projectTitle">{{ row.projectTitle || 'Untitled' }}</span>
                  <p class="report-type">{{ reportName(row.report) }}</p>
                </td>
                <td class="cell-leader">{{ row.projectLeader || '—' }}</td>
                <td class="cell-college" :title="row.college">{{ row.college || '—' }}</td>
                <td class="cell-period">{{ reportPeriodLabel(row.report) || 'Terminal' }}</td>
                <td class="cell-date">{{ formatReportDate(row.submittedAt) || '—' }}</td>
                <td>
                  <span class="end-badge" :class="statusTone(row.status)">
                    {{ reportStatusLabel(row.status) }}
                  </span>
                </td>
                <td class="text-right">
                  <button type="button" class="end-btn is-view row-btn" @click="viewReport(row)">
                    View
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <PaginationBar
          v-if="totalItems > 0"
          :page="page"
          :page-size="filters.limit"
          :total="totalItems"
          item-label="report"
          item-label-plural="reports"
          @update:page="goToPage"
          @update:page-size="setPageSize"
        />
      </template>
    </div>

    <!-- Read-only quarterly progress report: the same three-page viewer the
         proponent and the other offices use. -->
    <div v-if="viewing" class="modal-overlay" @click.self="viewing = null">
      <div class="modal modal--viewer">
        <div class="modal-header">
          <h2>{{ reportName(viewing) }}</h2>
          <button class="close-btn" @click="viewing = null" aria-label="Close">&times;</button>
        </div>
        <div class="modal-body">
          <QuarterlyReportViewer :report="viewing" @close="viewing = null" />
        </div>
      </div>
    </div>

    <!-- Structured Terminal Report, read as a document. Read only. -->
    <div v-if="terminalDoc" class="modal-overlay" @click.self="terminalDoc = null">
      <div class="modal modal--terminal-doc">
        <div class="modal-header">
          <h2>Terminal Report</h2>
          <button class="close-btn" @click="terminalDoc = null" aria-label="Close">&times;</button>
        </div>
        <div class="modal-body">
          <TerminalReportForm :model-value="terminalDoc.terminalReport" readonly />
        </div>
      </div>
    </div>

    <!-- Legacy uploaded terminal-report file, read only. -->
    <TerminalReportPreviewModal
      v-if="terminalFile"
      :report-id="terminalFile.reportId"
      :file-name="terminalFile.fileName"
      :period-label="terminalFile.periodLabel"
      documents-base-path="/api/ovcaf/quarterly-reports"
      @close="terminalFile = null"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import '@/assets/endorsement.css'
import PaginationBar from '@/components/PaginationBar.vue'
import api from '@/utils/api'
import QuarterlyReportViewer from '@/components/QuarterlyReportViewer.vue'
import TerminalReportForm from '@/components/TerminalReportForm.vue'
import TerminalReportPreviewModal from '@/components/TerminalReportPreviewModal.vue'
import { hasTerminalReportContent } from '@/utils/terminalReport'
import { statusTone } from '@/utils/statusTone.js'
import { reportStatusLabel } from '@/utils/statusCatalog.js'
import {
  isTerminalReport,
  reportName,
  reportPeriodLabel,
  formatReportDate,
} from '@/utils/reportViewing.js'

const loading = ref(true)
const error = ref('')
const rows = ref([])
const totalItems = ref(0)
const totalPages = ref(1)
const page = ref(1)
const options = reactive({ periods: [], colleges: [], programs: [], projects: [] })

const DEFAULT_SORT = 'submittedAt-desc'

const filters = reactive({
  search: '',
  period: 'All',
  college: 'All',
  program: 'All',
  projectId: 'All',
  sortSelection: DEFAULT_SORT,
  page: 1,
  limit: 10,
})

const viewing = ref(null)
const terminalDoc = ref(null)
const terminalFile = ref(null)

let searchTimer = null

const sortFields = computed(() => {
  const [sortBy, sortDir] = String(filters.sortSelection || DEFAULT_SORT).split('-')
  return { sortBy: sortBy || 'submittedAt', sortDir: sortDir === 'asc' ? 'asc' : 'desc' }
})

const rangeStart = computed(() => (totalItems.value ? (page.value - 1) * filters.limit + 1 : 0))
const rangeEnd = computed(() => Math.min(page.value * filters.limit, totalItems.value))

const fetchReports = async () => {
  loading.value = true
  error.value = ''
  try {
    const { sortBy, sortDir } = sortFields.value
    const response = await api.get('/api/ovcaf/quarterly-reports', {
      params: {
        search: filters.search.trim() || undefined,
        period: filters.period !== 'All' ? filters.period : undefined,
        college: filters.college !== 'All' ? filters.college : undefined,
        program: filters.program !== 'All' ? filters.program : undefined,
        proposalId: filters.projectId !== 'All' ? Number(filters.projectId) : undefined,
        sortBy,
        sortDir,
        page: filters.page,
        size: filters.limit,
      },
    })
    const data = response.data || {}
    rows.value = Array.isArray(data.items) ? data.items : []
    totalItems.value = Number(data.totalItems) || 0
    totalPages.value = Number(data.totalPages) || 1
    page.value = Number(data.page) || 1
    filters.page = page.value
    const filterOptions = data.filterOptions || {}
    options.periods = Array.isArray(filterOptions.periods) ? filterOptions.periods : []
    options.colleges = Array.isArray(filterOptions.colleges) ? filterOptions.colleges : []
    options.programs = Array.isArray(filterOptions.programs) ? filterOptions.programs : []
    options.projects = Array.isArray(filterOptions.projects) ? filterOptions.projects : []
  } catch (err) {
    console.error('Error fetching OVCAF quarterly reports:', err)
    error.value = err.response?.data?.error || 'Failed to load quarterly reports. Please try again.'
    rows.value = []
    totalItems.value = 0
    totalPages.value = 1
  } finally {
    loading.value = false
  }
}

const onSearchInput = () => {
  filters.page = 1
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(fetchReports, 250)
}

const onFilterChange = () => {
  filters.page = 1
  fetchReports()
}

const onSortChange = () => {
  filters.page = 1
  fetchReports()
}

const clearFilters = () => {
  filters.search = ''
  filters.period = 'All'
  filters.college = 'All'
  filters.program = 'All'
  filters.projectId = 'All'
  filters.sortSelection = DEFAULT_SORT
  filters.page = 1
  fetchReports()
}

const goToPage = (next) => {
  if (next < 1 || next > totalPages.value) return
  filters.page = next
  fetchReports()
}

/** A new page size reloads the server window from its first page. */
const setPageSize = (value) => {
  filters.limit = Math.max(1, Number(value) || 10)
  filters.page = 1
  fetchReports()
}

const periodLabelFor = (period) => (period === 'TERMINAL' ? 'Terminal Report' : period)

/**
 * Open a report read-only. Structured reports use the shared viewer/document
 * components; a legacy terminal row that only holds a file uses the preview modal
 * through OVCAF's own authorized documents route. No edit action exists.
 */
const viewReport = (row) => {
  const report = row.report || row
  if (!isTerminalReport(report)) {
    viewing.value = report
    return
  }
  if (hasTerminalReportContent(report.terminalReport)) {
    terminalDoc.value = { ...report, projectTitle: row.projectTitle }
    return
  }
  terminalFile.value = {
    reportId: report.id,
    fileName: report.terminalReportFileName || '',
    periodLabel: reportPeriodLabel(report) || 'Terminal Report',
  }
}

onMounted(fetchReports)
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
/* The shared .end-* layer owns the page, toolbar, table, badge and pager. */
.ovcaf-quarterly-card {
  padding: 18px;
}

.ovcaf-toolbar {
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.ovcaf-search {
  flex: 1 1 240px;
  min-width: 0;
}

.ovcaf-toolbar .end-select {
  flex: 0 0 auto;
  max-width: 220px;
}

.ovcaf-project-select,
.ovcaf-sort {
  max-width: 260px;
}

.clear-btn {
  flex: 0 0 auto;
  white-space: nowrap;
}

.end-page--fluid .ovcaf-report-table {
  min-width: 1040px;
  table-layout: fixed;
  font-size: var(--ui-font-small);
}

.ovcaf-report-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  padding: 10px;
  font-size: var(--ui-font-table-header);
  font-weight: var(--ui-weight-semibold);
  letter-spacing: var(--ui-tracking-caps);
  text-transform: uppercase;
  white-space: nowrap;
}

.ovcaf-report-table th:nth-child(1) { width: 14%; }
.ovcaf-report-table th:nth-child(2) { width: 23%; }
.ovcaf-report-table th:nth-child(3) { width: 12%; }
.ovcaf-report-table th:nth-child(4) { width: 15%; }
.ovcaf-report-table th:nth-child(5) { width: 9%; }
.ovcaf-report-table th:nth-child(6) { width: 12%; }
.ovcaf-report-table th:nth-child(7) { width: 8%; }
.ovcaf-report-table th:nth-child(8) { width: 7%; }

.ovcaf-report-table td {
  padding: 9px 10px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
}

.ovcaf-report-table tbody tr:hover td {
  background: #fafbff;
}

.ovcaf-report-table th.text-right,
.ovcaf-report-table td.text-right {
  text-align: right;
}

.cell-id {
  font-size: var(--ui-font-caption);
  line-height: var(--ui-line-snug);
  color: #5b6484;
  overflow-wrap: anywhere;
}

.cell-title {
  font-weight: var(--ui-weight-semibold);
  color: #1c1a3c;
}

.title-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  line-height: var(--ui-line-snug);
}

.report-type {
  margin: 2px 0 0;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-medium);
  color: var(--ui-text-muted);
  overflow-wrap: anywhere;
}

.cell-leader {
  font-size: var(--ui-font-small);
  color: #1f2937;
  overflow-wrap: anywhere;
}

.cell-college {
  font-size: var(--ui-font-caption);
  color: #475569;
  line-height: var(--ui-line-snug);
  overflow-wrap: anywhere;
}

.cell-period,
.cell-date {
  font-size: var(--ui-font-caption);
  color: #5b6484;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.ovcaf-report-table .end-badge {
  padding: 2px 7px;
  font-size: var(--ui-font-caption);
}

.row-btn {
  padding: 6px 10px;
  font-size: var(--ui-control-font-sm);
  white-space: nowrap;
}

.ovcaf-state {
  margin: 0;
  padding: 22px;
  font-size: var(--ui-font-small);
  color: var(--ui-text-muted);
}

.ovcaf-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: var(--ui-font-small);
  font-weight: var(--ui-weight-medium);
}

.ovcaf-empty {
  padding: 24px 12px;
  text-align: center;
  color: var(--ui-text-muted);
}

/* Read-only viewer modal, mirroring the shared report viewer shell. */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  z-index: 200;
  padding: 24px;
  overflow-y: auto;
}

.modal {
  background: #fff;
  width: 100%;
  max-width: 1000px;
  border-radius: 12px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal--terminal-doc {
  max-width: 1180px;
}

.modal--viewer {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  background: #fff;
  flex-shrink: 0;
}

.modal-header h2 {
  margin: 0;
  font-size: var(--ui-font-card-title);
}

.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  cursor: pointer;
}

.modal-body {
  padding: 16px 20px 24px;
}

.modal--viewer .modal-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0;
}

@media (max-width: 768px) {
  .ovcaf-quarterly-card {
    padding: 14px;
  }

  .ovcaf-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .ovcaf-search,
  .ovcaf-toolbar .end-select,
  .ovcaf-project-select,
  .ovcaf-sort {
    width: 100%;
    max-width: none;
  }
}
</style>
