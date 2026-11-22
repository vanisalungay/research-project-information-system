<template>
  <div class="submitted-page">
    <!-- PAGE TITLE -->
    <div class="page-header">
      <h2>Endorsed Proposals (from RPS)</h2>
      <p>Proposals endorsed by RPS for OVCRIGE review and REC coordination</p>
    </div>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <div class="search-wrapper">
        <input
          v-model="search"
          type="text"
          class="search"
          @input="page = 1"
          placeholder="Search by ID, title, program, or project leader..."
        />
        <button
          v-if="search"
          type="button"
          class="clear-search"
          aria-label="Clear search"
          @click="search = ''; page = 1"
        >
          &times;
        </button>
      </div>

      <select v-model="statusFilter" class="filter-select" @change="page = 1">
        <option value="ALL">All Status</option>
        <option v-for="status in statusOptions" :key="status" :value="status">
          {{ proposalStatusLabel(status) }}
        </option>
      </select>

      <select v-model="programFilter" class="filter-select" @change="page = 1">
        <option value="ALL">All Programs</option>
        <option v-for="program in programOptions" :key="program" :value="program">
          {{ program }}
        </option>
      </select>

      <button
        type="button"
        class="reset-filters"
        :disabled="!hasActiveFilters"
        @click="resetFilters"
      >
        Reset
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="table-card">
      <p style="text-align:center;padding:40px;">Loading proposals...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="table-card">
      <p style="text-align:center;padding:40px;color:#dc2626;">{{ error }}</p>
    </div>

    <!-- TABLE -->
    <div v-else class="table-card">
      <table>
        <thead>
          <tr>
            <th>Proposal ID</th>
            <th>Title</th>
            <th>Proponent</th>
            <th>Date Submitted</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="proposal in paginatedProposals" :key="proposal.id">
            <td>{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
            <td>
              <strong>{{ proposal.projectTitle }}</strong>
              <br><small>{{ proposal.programTitle }}</small>
            </td>
            <td>{{ proposal.projectLeader }}</td>
            <td>{{ proposalSubmittedAt(proposal)?.substring(0,10) || '-' }}</td>
            <td>
              <span class="status status-badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) }}</span>
            </td>
            <td>
              <button class="review-btn btn-view" @click="goToReview(proposal.id)">REVIEW</button>
            </td>
          </tr>
          <tr v-if="filteredProposals.length === 0">
            <td colspan="6" class="empty-cell">No proposals match your search and filters.</td>
          </tr>
        </tbody>
      </table>

      <!-- PAGINATION -->
      <PaginationBar
        v-if="filteredProposals.length"
        :page="page"
        :page-size="pageSize"
        :total="filteredProposals.length"
        item-label="proposal"
        item-label-plural="proposals"
        @update:page="page = $event"
        @update:page-size="setPageSize"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel, proposalStatusMatches } from '@/utils/proposalStatus.js'
import {
  OVCRIGE_PIPELINE_STATUSES,
  proposalSubmittedAt,
} from '@/utils/ovcrigeProposals'

const router = useRouter()
const proposals = ref([])
const search = ref('')
const statusFilter = ref('ALL')
const programFilter = ref('ALL')
const page = ref(1)
const loading = ref(false)
const error = ref(null)

/** How many proposals one page holds. Matches the other office list (OVCAF inbox). */
const pageSize = ref(10)

const statusOptions = OVCRIGE_PIPELINE_STATUSES

const loadProposals = async () => {
  loading.value = true
  error.value = null
  try {
    // The same pipeline set and the same server-side submission ordering as the OVCRIGE
    // dashboard preview, so "View All" shows exactly what the preview counts.
    const res = await api.get('/api/proposals', {
      params: {
        statusIn: [...OVCRIGE_PIPELINE_STATUSES],
        sort: 'submittedAt',
      },
      paramsSerializer: { indexes: null },
    })
    proposals.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals. Ensure the backend server is running.'
    proposals.value = []
  } finally {
    loading.value = false
  }
}
onMounted(loadProposals)

/** Program options come from the loaded records, never from a hardcoded list. */
const programOptions = computed(() => {
  const programs = new Set()
  for (const proposal of proposals.value) {
    const name = String(proposal.programTitle || '').trim()
    if (name) programs.add(name)
  }
  return [...programs].sort((a, b) => a.localeCompare(b))
})

const hasActiveFilters = computed(
  () => Boolean(search.value.trim()) || statusFilter.value !== 'ALL' || programFilter.value !== 'ALL',
)

const filteredProposals = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return proposals.value.filter((proposal) => {
    if (statusFilter.value !== 'ALL' && !proposalStatusMatches(proposal.status, statusFilter.value)) {
      return false
    }
    if (programFilter.value !== 'ALL' && (proposal.programTitle || '') !== programFilter.value) {
      return false
    }
    if (!keyword) return true
    return (
      (proposal.documentId || proposal.proposalCode || '').toLowerCase().includes(keyword) ||
      (proposal.projectTitle || '').toLowerCase().includes(keyword) ||
      (proposal.programTitle || '').toLowerCase().includes(keyword) ||
      (proposal.projectLeader || '').toLowerCase().includes(keyword)
    )
  })
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProposals.value.length / pageSize.value)))

const paginatedProposals = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredProposals.value.slice(start, start + pageSize.value)
})

const rangeStart = computed(() =>
  filteredProposals.value.length ? (page.value - 1) * pageSize.value + 1 : 0,
)
const rangeEnd = computed(() => Math.min(page.value * pageSize.value, filteredProposals.value.length))

// Selecting a filter returns to the first page. A shrinking result set can also leave the
// current page past the end, so clamp it whenever the page count drops.
watch([statusFilter, programFilter], () => {
  page.value = 1
})
watch(totalPages, (count) => {
  if (page.value > count) page.value = count
})

const resetFilters = () => {
  search.value = ''
  statusFilter.value = 'ALL'
  programFilter.value = 'ALL'
  page.value = 1
}

/** A new page size starts a new result window on its first page. */
const setPageSize = (value) => {
  pageSize.value = Math.max(1, Number(value) || 10)
  page.value = 1
}

const goToReview = (proposalId) => {
  router.push({ name: 'OvcReviewProposal', params: { id: proposalId } })
}
</script>

<style scoped>
.submitted-page {
  padding: 25px;
}
.page-header h2 {
  margin: 0;
}
.page-header p {
  color: #666;
  margin-top: 5px;
}
/* FILTER BAR */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin: 20px 0;
}
.search-wrapper {
  position: relative;
  flex: 1 1 260px;
  min-width: 220px;
}
.filter-bar .search {
  width: 100%;
  padding: 10px 34px 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.clear-search {
  position: absolute;
  top: 50%;
  right: 8px;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  border: none;
  border-radius: 50%;
  background: #e2e8f0;
  color: #475569;
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
}
.filter-select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
  background: #fff;
  min-width: 170px;
}
/* Layout only; .btn-cancel supplies the neutral action tone. */
.reset-filters {
  padding: 10px 16px;
  border: 1px solid #ccc;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}
.reset-filters:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.table-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th {
  background: #2f2b57;
  color: white;
  padding: 12px;
  text-align: left;
}
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}
small {
  color: #777;
}
.empty-cell {
  text-align: center;
  color: #64748b;
  padding: 24px 12px;
}
.status {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
/* Layout only; .btn-view supplies the colour. */
.review-btn {
  padding: 7px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: .2s;
}
/* PAGINATION — the same control as the OVCAF inbox. */
.pagination-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding-top: 14px;
  margin-top: 8px;
  border-top: 1px solid #eee;
}
.pagination-info {
  font-size: 13px;
  color: #64748b;
}
.pagination-controls {
  display: flex;
  align-items: center;
  gap: 6px;
}
.pagination-btn {
  min-width: 32px;
  padding: 6px 10px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  color: #334155;
  cursor: pointer;
  font-size: 13px;
}
.pagination-btn:hover:not(:disabled) {
  background: #f1f5f9;
}
.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.pagination-btn.page-num.active {
  background: #2f2b57;
  border-color: #2f2b57;
  color: #fff;
  font-weight: 600;
}
</style>
