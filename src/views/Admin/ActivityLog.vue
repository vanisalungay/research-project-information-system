<template>
  <div class="page">
    <!-- Page header: title and description on the left, the entry count on the right. -->
    <div class="header">
      <div>
        <h2>Activity Log</h2>
        <p class="subtitle">
          Monitor and review the history of activities performed in the system by your
          administrator account.
        </p>
      </div>
      <div class="header-actions">
        <span class="count-chip">
          {{ totalElements }} {{ totalElements === 1 ? 'activity' : 'activities' }}
        </span>
      </div>
    </div>

    <!-- Filters -->
    <div class="toolbar">
      <input
        v-model="search"
        type="text"
        class="search-input"
        placeholder="Search by activity or module..."
        @keyup.enter="applyFilters"
      />

      <select v-model="moduleFilter" class="filter-select" @change="applyFilters">
        <option value="">All modules</option>
        <option v-for="name in modules" :key="name" :value="name">{{ name }}</option>
      </select>

      <select v-model="statusFilter" class="filter-select" @change="applyFilters">
        <option value="">All statuses</option>
        <option value="SUCCESS">Success</option>
        <option value="FAILED">Failed</option>
      </select>

      <label class="date-field">
        <span>From</span>
        <input v-model="fromDate" type="date" class="date-input" @change="applyFilters" />
      </label>

      <label class="date-field">
        <span>To</span>
        <input v-model="toDate" type="date" class="date-input" @change="applyFilters" />
      </label>

      <button type="button" class="clear-btn" @click="clearFilters">Clear</button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="loading-state">
      <p>Loading activity...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="loading-state">
      <p class="error-text">{{ error }}</p>
      <button type="button" class="retry-btn" @click="load">Retry</button>
    </div>

    <!-- Empty state -->
    <div v-else-if="!entries.length" class="empty-state">
      <div class="empty-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
          stroke-linecap="round" stroke-linejoin="round">
          <path d="M3 12a9 9 0 1 0 3-6.7" />
          <polyline points="3 4 3 10 9 10" />
          <polyline points="12 8 12 12 15 14" />
        </svg>
      </div>
      <h3>No activity yet</h3>
      <p v-if="hasFilters">
        No activity matches the current search and filters. Try clearing them.
      </p>
      <p v-else>
        Actions you perform with this administrator account - creating an account, updating a
        call for proposal, changing your password - are recorded here as they happen.
      </p>
    </div>

    <!-- Table -->
    <div v-else class="table-scroll">
      <table>
        <thead>
          <tr>
            <th>Date &amp; Time</th>
            <th>Activity</th>
            <th>Module</th>
            <th>Details</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>
          <tr v-for="entry in entries" :key="entry.id">
            <td class="nowrap">{{ formatDateTime(entry.createdAt) }}</td>
            <td class="activity-cell">{{ entry.description }}</td>
            <td class="nowrap">{{ entry.module }}</td>
            <td class="details-cell">{{ entry.details }}</td>
            <td>
              <span class="status-badge" :class="statusTone(entry.status)">
                {{ formatStatus(entry.status) }}
              </span>
            </td>
            <td class="actions-cell">
              <button type="button" class="view-btn btn-view" @click="openDetails(entry)">
                View Details
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Pagination -->
    <div v-if="!loading && !error && entries.length" class="pagination">
      <span class="range-label">{{ rangeLabel }}</span>
      <div class="pager">
        <button type="button" class="page-btn" :disabled="page === 0" @click="goToPage(page - 1)">
          Previous
        </button>
        <span class="page-label">Page {{ page + 1 }} of {{ Math.max(totalPages, 1) }}</span>
        <button
          type="button"
          class="page-btn"
          :disabled="page + 1 >= totalPages"
          @click="goToPage(page + 1)"
        >
          Next
        </button>
      </div>
    </div>

    <!-- View Details -->
    <div v-if="showDetails" class="modal-backdrop" @click.self="closeDetails">
      <div class="modal">
        <div class="modal-header">
          <h3>Activity Details</h3>
          <button type="button" class="close-btn" @click="closeDetails">&times;</button>
        </div>

        <div class="modal-body">
          <p v-if="detailsError" class="error-text">{{ detailsError }}</p>

          <dl v-else-if="selected" class="detail-list">
            <dt>Date &amp; Time</dt>
            <dd>{{ formatDateTime(selected.createdAt) }}</dd>

            <dt>Activity</dt>
            <dd>{{ selected.description }}</dd>

            <dt>Module</dt>
            <dd>{{ selected.module }}</dd>

            <dt>Status</dt>
            <dd>
              <span class="status-badge" :class="statusTone(selected.status)">
                {{ formatStatus(selected.status) }}
              </span>
            </dd>

            <dt>Performed by</dt>
            <dd>{{ selected.userName || '—' }}</dd>

            <dt>Action</dt>
            <dd>{{ selected.action }}</dd>

            <dt>Details</dt>
            <dd class="detail-text">{{ selected.details || 'No further information was recorded.' }}</dd>
          </dl>
        </div>

        <div class="modal-footer">
          <button type="button" class="clear-btn" @click="closeDetails">Close</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
/**
 * Activity Log (RPS ADMIN).
 *
 * The administrator's own history, read from the backend - the page never invents a
 * row. The endpoint returns only the entries that belong to the signed-in
 * administrator and requires the RPS_ADMIN role, so the filtering the user does here
 * can only ever narrow their own records.
 */
import { computed, onMounted, ref } from 'vue'
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import { statusLabel } from '@/utils/statusLabel.js'

const entries = ref([])
const modules = ref([])
const loading = ref(true)
const error = ref('')

const search = ref('')
const moduleFilter = ref('')
const statusFilter = ref('')
const fromDate = ref('')
const toDate = ref('')

const page = ref(0)
const size = ref(10)
const totalElements = ref(0)
const totalPages = ref(0)

const showDetails = ref(false)
const selected = ref(null)
const detailsError = ref('')

const hasFilters = computed(
  () =>
    Boolean(search.value.trim()) ||
    Boolean(moduleFilter.value) ||
    Boolean(statusFilter.value) ||
    Boolean(fromDate.value) ||
    Boolean(toDate.value),
)

const rangeLabel = computed(() => {
  if (!totalElements.value) return 'No activity'
  const first = page.value * size.value + 1
  const last = Math.min((page.value + 1) * size.value, totalElements.value)
  return `Showing ${first}-${last} of ${totalElements.value}`
})

/** Loads one page with the current filters applied. */
const load = async () => {
  loading.value = true
  error.value = ''
  try {
    const params = { page: page.value, size: size.value }
    if (search.value.trim()) params.search = search.value.trim()
    if (moduleFilter.value) params.module = moduleFilter.value
    if (statusFilter.value) params.status = statusFilter.value
    if (fromDate.value) params.from = fromDate.value
    if (toDate.value) params.to = toDate.value

    const { data } = await api.get('/api/activity-logs', { params })
    entries.value = data?.content ?? []
    totalElements.value = data?.totalElements ?? 0
    totalPages.value = data?.totalPages ?? 0
  } catch (e) {
    console.error('Could not load the activity log', e)
    error.value = 'Your activity history could not be loaded. Please try again.'
    entries.value = []
    totalElements.value = 0
    totalPages.value = 0
  } finally {
    loading.value = false
  }
}

/** The modules this administrator actually has entries for. */
const loadModules = async () => {
  try {
    const { data } = await api.get('/api/activity-logs/modules')
    modules.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error('Could not load the activity modules', e)
    modules.value = []
  }
}

const applyFilters = () => {
  page.value = 0
  load()
}

const clearFilters = () => {
  search.value = ''
  moduleFilter.value = ''
  statusFilter.value = ''
  fromDate.value = ''
  toDate.value = ''
  page.value = 0
  load()
}

const goToPage = (next) => {
  if (next < 0 || next >= totalPages.value) return
  page.value = next
  load()
}

/** Opens the details panel and re-reads that entry from the backend. */
const openDetails = async (entry) => {
  selected.value = entry
  detailsError.value = ''
  showDetails.value = true
  try {
    const { data } = await api.get(`/api/activity-logs/${entry.id}`)
    selected.value = data
  } catch (e) {
    console.error('Could not load the activity details', e)
    detailsError.value = 'The full details of this activity could not be loaded.'
  }
}

const closeDetails = () => {
  showDetails.value = false
  selected.value = null
  detailsError.value = ''
}

const formatDateTime = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return date.toLocaleString('en-US', {
    year: 'numeric',
    month: 'short',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

const formatStatus = (status) => statusLabel(status) || 'Success'

onMounted(() => {
  loadModules()
  load()
})
</script>

<style scoped>
/* Same shell as the other administrator pages: the DefaultLayout supplies the
   sidebar and the header, so this file only styles its own content. */
.page {
  padding: 20px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.header h2 {
  margin: 0;
  font-size: 24px;
  color: #1f2937;
}

.subtitle {
  margin: 6px 0 0;
  color: #6b7280;
  font-size: 14px;
  max-width: 720px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.count-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: #eef2ff;
  color: #2452ff;
  font-size: 13px;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.search-input,
.filter-select,
.date-input {
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  color: #1f2937;
  background: #fff;
}

.search-input {
  flex: 1 1 260px;
  min-width: 220px;
}

.search-input:focus,
.filter-select:focus,
.date-input:focus {
  outline: none;
  border-color: #2452ff;
}

.date-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #6b7280;
}

.clear-btn,
.retry-btn,
.page-btn {
  border-radius: 6px;
  border: 1px solid #d1d5db;
  background: #fff;
  color: #374151;
  padding: 9px 14px;
  font-size: 14px;
  cursor: pointer;
}

/* The View action colour comes from the shared action system (assets/actions.css). */
.view-btn {
  padding: 9px 14px;
  font-size: 14px;
}

.clear-btn:hover,
.retry-btn:hover,
.page-btn:hover:not(:disabled) {
  border-color: #2452ff;
  color: #2452ff;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.table-scroll {
  overflow-x: auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead th {
  text-align: left;
  padding: 12px 14px;
  background: #f9fafb;
  color: #374151;
  font-weight: 600;
  border-bottom: 1px solid #e5e7eb;
  white-space: nowrap;
}

tbody td {
  padding: 12px 14px;
  border-bottom: 1px solid #f3f4f6;
  color: #374151;
  vertical-align: top;
}

tbody tr:hover {
  background: #f9fafb;
}

.nowrap {
  white-space: nowrap;
}

.activity-cell {
  font-weight: 600;
  color: #111827;
}

.details-cell,
.detail-text {
  max-width: 420px;
  color: #6b7280;
  overflow-wrap: anywhere;
}

.actions-cell {
  white-space: nowrap;
}

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.loading-state,
.empty-state {
  padding: 40px 20px;
  text-align: center;
  color: #6b7280;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.empty-state h3 {
  margin: 12px 0 6px;
  color: #1f2937;
}

.empty-icon {
  width: 44px;
  height: 44px;
  margin: 0 auto;
  color: #9ca3af;
}

.empty-icon svg {
  width: 100%;
  height: 100%;
}

.error-text {
  color: #b42318;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.range-label,
.page-label {
  color: #6b7280;
  font-size: 13px;
}

.pager {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(17, 24, 39, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  z-index: 50;
}

.modal {
  background: #fff;
  border-radius: 10px;
  width: min(620px, 100%);
  max-height: 85vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #1f2937;
}

.close-btn {
  border: none;
  background: none;
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  color: #6b7280;
}

.modal-body {
  padding: 18px 20px;
  overflow-y: auto;
}

.modal-footer {
  padding: 14px 20px;
  border-top: 1px solid #e5e7eb;
  text-align: right;
}

.detail-list {
  display: grid;
  grid-template-columns: 150px 1fr;
  gap: 10px 16px;
  margin: 0;
}

.detail-list dt {
  color: #6b7280;
  font-size: 13px;
}

.detail-list dd {
  margin: 0;
  color: #1f2937;
  font-size: 14px;
}

@media (max-width: 640px) {
  .header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    margin-left: 0;
  }

  .detail-list {
    grid-template-columns: 1fr;
  }
}
</style>
