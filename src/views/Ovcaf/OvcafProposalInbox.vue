<template>
  <div class="end-page end-page--fluid ovcaf-proposals-page">
    <header class="end-page-header">
      <h2>Proposals</h2>
      <p class="end-subtitle">Financial validation queue</p>
    </header>

    <div class="end-card ovcaf-proposals-card">
      <!-- Filters. Search and every select are applied on the server. -->
      <div class="end-toolbar ovcaf-toolbar">
        <input
          v-model="filters.search"
          type="text"
          class="end-input ovcaf-search"
          placeholder="Search by ID, title, or project leader..."
          @input="onSearchInput"
        />
        <select v-model="filters.status" class="end-select" @change="onFilterChange">
          <option value="All">All Statuses</option>
          <option v-for="status in options.statuses" :key="status" :value="status">
            {{ proposalStatusLabel(status) }}
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
        <select v-model="filters.sortSelection" class="end-select ovcaf-sort" @change="onSortChange">
          <option value="receivedAt-desc">Newest received</option>
          <option value="receivedAt-asc">Oldest received</option>
          <option value="documentId-asc">Proposal ID</option>
          <option value="projectTitle-asc">Research title</option>
          <option value="projectLeader-asc">Project leader</option>
          <option value="college-asc">College</option>
        </select>
        <button type="button" class="end-btn is-link clear-btn" @click="clearFilters">
          Clear All
        </button>
      </div>

      <!-- Loading / error -->
      <p v-if="loading" class="ovcaf-state">Loading proposals...</p>
      <div v-else-if="error" class="ovcaf-error" role="alert">
        <span>{{ error }}</span>
        <button type="button" class="end-btn is-view retry-btn" @click="fetchProposals">Retry</button>
      </div>

      <template v-else>
        <div class="end-table-scroll" role="region" aria-label="OVCAF proposals" tabindex="0">
          <table class="end-table ovcaf-table">
            <thead>
              <tr>
                <th>Proposal ID</th>
                <th>Research Title</th>
                <th>Project Leader</th>
                <th>College</th>
                <th>Date Received</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody v-if="rows.length === 0">
              <tr>
                <td colspan="7" class="ovcaf-empty">No proposals match your current search or filters.</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="p in rows" :key="p.id">
                <td class="cell-id" :title="p.documentId">{{ p.documentId || '—' }}</td>
                <td class="cell-title">
                  <span class="title-text" :title="p.title">{{ p.title }}</span>
                </td>
                <td class="cell-leader">{{ p.projectLeader }}</td>
                <td class="cell-college" :title="p.college">{{ p.college }}</td>
                <td class="cell-date">{{ formatDate(p.dateReceived) }}</td>
                <td>
                  <span class="end-badge" :class="statusTone(p.status)">
                    {{ proposalStatusLabel(p.status) }}
                  </span>
                </td>
                <td class="text-right">
                  <div class="row-actions">
                    <router-link :to="`/ovcaf/proposals/${p.id}`" class="end-btn is-view row-btn">
                      View Details
                    </router-link>
                    <router-link v-if="isPending(p)" :to="`/ovcaf/validate/${p.id}`"
                      class="end-btn is-view row-btn validate-btn">
                      Validate
                    </router-link>
                  </div>
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
          item-label="proposal"
          item-label-plural="proposals"
          @update:page="goToPage"
          @update:page-size="setPageSize"
        />
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import '@/assets/endorsement.css'
import PaginationBar from '@/components/PaginationBar.vue'
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'
import { normalizeStatusKey } from '@/utils/statusLabel.js'

const loading = ref(true)
const error = ref('')
const rows = ref([])
const totalItems = ref(0)
const totalPages = ref(1)
const page = ref(1)
const options = reactive({ statuses: [], colleges: [], programs: [] })

const DEFAULT_SORT = 'receivedAt-desc'

const filters = reactive({
  search: '',
  status: 'All',
  college: 'All',
  program: 'All',
  sortSelection: DEFAULT_SORT,
  page: 1,
  limit: 10,
})

let searchTimer = null

const sortFields = computed(() => {
  const [sortBy, sortDir] = String(filters.sortSelection || DEFAULT_SORT).split('-')
  return { sortBy: sortBy || 'receivedAt', sortDir: sortDir === 'asc' ? 'asc' : 'desc' }
})

const rangeStart = computed(() => (totalItems.value ? (page.value - 1) * filters.limit + 1 : 0))
const rangeEnd = computed(() => Math.min(page.value * filters.limit, totalItems.value))

/**
 * Every filter, the ordering and the pagination are resolved by the API. The page
 * therefore always shows the same result regardless of how much of the list was
 * previously loaded.
 */
const fetchProposals = async () => {
  loading.value = true
  error.value = ''
  try {
    const { sortBy, sortDir } = sortFields.value
    const response = await api.get('/api/ovcaf/proposals/page', {
      params: {
        search: filters.search.trim() || undefined,
        status: filters.status !== 'All' ? filters.status : undefined,
        college: filters.college !== 'All' ? filters.college : undefined,
        program: filters.program !== 'All' ? filters.program : undefined,
        sortBy,
        sortDir,
        page: filters.page,
        size: filters.limit,
      },
    })
    const data = response.data || {}
    rows.value = (data.items || []).map((p) => ({
      id: p.id,
      documentId: p.documentId || '',
      title: p.projectTitle || 'Untitled Project',
      // The form's Project Leader must win; `proponentName` is only the
      // server-side fallback for records that never carried the form field.
      projectLeader: p.projectLeader || p.proponentName || 'Unknown',
      college: p.college || 'Not specified',
      program: p.programTitle || '',
      // The actual OVCAF receipt event, never the draft's creation date.
      dateReceived: p.receivedAt || null,
      status: p.status,
    }))
    totalItems.value = Number(data.totalItems) || 0
    totalPages.value = Number(data.totalPages) || 1
    page.value = Number(data.page) || 1
    filters.page = page.value
    const filterOptions = data.filterOptions || {}
    options.statuses = Array.isArray(filterOptions.statuses) ? filterOptions.statuses : []
    options.colleges = Array.isArray(filterOptions.colleges) ? filterOptions.colleges : []
    options.programs = Array.isArray(filterOptions.programs) ? filterOptions.programs : []
  } catch (err) {
    console.error('Error fetching OVCAF proposals:', err)
    error.value = 'Failed to load proposals. Please try again.'
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
  searchTimer = setTimeout(fetchProposals, 250)
}

const onFilterChange = () => {
  filters.page = 1
  fetchProposals()
}

const onSortChange = () => {
  filters.page = 1
  fetchProposals()
}

const clearFilters = () => {
  filters.search = ''
  filters.status = 'All'
  filters.college = 'All'
  filters.program = 'All'
  filters.sortSelection = DEFAULT_SORT
  filters.page = 1
  fetchProposals()
}

const goToPage = (next) => {
  if (next < 1 || next > totalPages.value) return
  filters.page = next
  fetchProposals()
}

/** A new page size reloads the server window from its first page. */
const setPageSize = (value) => {
  filters.limit = Math.max(1, Number(value) || 10)
  filters.page = 1
  fetchProposals()
}

const isPending = (p) => normalizeStatusKey(p.status) === 'FOR_OVCAF_APPROVAL'

const formatDate = (value) => {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' })
}

watch(
  () => filters.sortSelection,
  () => { filters.page = 1 },
)

onMounted(fetchProposals)
onBeforeUnmount(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
/* ============================================================================
   OVCAF Proposals page. The shared .end-* layer (assets/endorsement.css) owns the
   page shell, toolbar, table, badges, buttons and pager. Only the OVCAF-specific
   column proportions and responsive wrapping are declared here.
   ============================================================================ */

.ovcaf-proposals-card {
  padding: 18px;
}

.ovcaf-toolbar {
  display: grid;
  grid-template-columns: minmax(220px, 1.8fr) minmax(140px, 1fr) minmax(160px, 1.2fr)
    minmax(140px, 1fr) minmax(160px, 1.1fr) auto;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.ovcaf-search {
  min-width: 0;
}

.ovcaf-toolbar .end-select {
  width: 100%;
  min-width: 0;
}

.clear-btn {
  white-space: nowrap;
}

.end-page--fluid .ovcaf-table {
  min-width: 1020px;
  table-layout: fixed;
  font-size: 0.8125rem;
}

.ovcaf-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  padding: 10px;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.03em;
  text-transform: uppercase;
  white-space: nowrap;
}

.ovcaf-table th:nth-child(1) { width: 14%; }
.ovcaf-table th:nth-child(2) { width: 22%; }
.ovcaf-table th:nth-child(3) { width: 11%; }
.ovcaf-table th:nth-child(4) { width: 15%; }
.ovcaf-table th:nth-child(5) { width: 12%; }
.ovcaf-table th:nth-child(6) { width: 15%; }
.ovcaf-table th:nth-child(7) { width: 11%; }

.ovcaf-table td {
  padding: 9px 10px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
}

.ovcaf-table tbody tr:hover td {
  background: #fafbff;
}

.ovcaf-table th.text-right,
.ovcaf-table td.text-right {
  text-align: right;
}

.cell-id {
  font-size: 0.6875rem;
  line-height: 1.35;
  color: #5b6484;
  overflow-wrap: anywhere;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

.title-text {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  overflow-wrap: anywhere;
  line-height: 1.35;
}

.cell-leader {
  font-size: 0.78rem;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.cell-college {
  font-size: 0.75rem;
  color: #475569;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.cell-date {
  font-size: 0.75rem;
  color: #5b6484;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.ovcaf-table .end-badge {
  padding: 2px 7px;
  font-size: 0.6875rem;
}

.row-actions {
  display: inline-flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.row-btn {
  padding: 5px 9px;
  font-size: 0.7rem;
  white-space: nowrap;
}

.ovcaf-state {
  margin: 0;
  padding: 22px;
  font-size: 0.8125rem;
  color: #78809a;
}

.ovcaf-error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  padding: 10px 14px;
  border-radius: 8px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #b91c1c;
  font-size: 0.875rem;
  font-weight: 500;
}

.retry-btn {
  flex: 0 0 auto;
}

.ovcaf-empty {
  padding: 24px 12px;
  text-align: center;
  color: #78809a;
}

@media (max-width: 1100px) {
  .ovcaf-toolbar {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .ovcaf-proposals-card {
    padding: 14px;
  }

  .ovcaf-toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
