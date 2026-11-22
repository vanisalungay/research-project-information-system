<template>
  <div class="endorsed-page">
    <h2>Proposals for OC Endorsement</h2>
    <p class="subtitle">Proposals approved by REC that OVCRIGE must forward to the Office of the Chancellor (OC) for final approval per the process flow</p>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <div class="search-wrapper">
        <input
          class="search"
          v-model="search"
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
        <option value="ALL">All Statuses</option>
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

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>Proposal ID</th>
            <th>Project Title</th>
            <th>Program</th>
            <th>Project Leader</th>
            <th>Status</th>
            <th>Date Updated</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="proposal in paginatedProposals" :key="proposal.id">
            <td>{{ proposal.documentId || proposal.proposalCode || '—' }}</td>
            <td>{{ proposal.projectTitle }}</td>
            <td>{{ proposal.programTitle }}</td>
            <td>{{ proposal.projectLeader }}</td>
            <td>
              <span class="status status-badge" :class="statusTone(proposal.status)">{{ proposalStatusLabel(proposal.status) }}</span>
            </td>
            <td>{{ proposal.updatedAt?.substring(0,10) }}</td>
            <td>
              <button class="view btn-view" @click="goToReview(proposal.id)">REVIEW</button>
            </td>
          </tr>
          <tr v-if="loading">
            <td colspan="7" class="empty-cell">Loading proposals...</td>
          </tr>
          <tr v-else-if="filteredProposals.length === 0">
            <td colspan="7" class="empty-cell">No proposals match your search and filters.</td>
          </tr>
        </tbody>
      </table>

      <!-- PAGINATION -->
      <div v-if="totalPages > 1" class="pagination-container">
        <div class="pagination-info">
          Showing <strong>{{ rangeStart }}</strong>–<strong>{{ rangeEnd }}</strong> of
          <strong>{{ filteredProposals.length }}</strong> proposals
        </div>
        <div class="pagination-controls">
          <button class="pagination-btn" :disabled="page === 1" @click="page--">&lsaquo;</button>
          <button
            v-for="pg in totalPages"
            :key="pg"
            class="pagination-btn page-num"
            :class="{ active: pg === page }"
            @click="page = pg"
          >
            {{ pg }}
          </button>
          <button class="pagination-btn" :disabled="page === totalPages" @click="page++">&rsaquo;</button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel, proposalStatusMatches } from '@/utils/proposalStatus.js'
import { OVCRIGE_OC_ENDORSEMENT_STATUSES } from '@/utils/ovcrigeProposals'

const router = useRouter()
const proposals = ref([])
const search = ref('')
const statusFilter = ref('ALL')
const programFilter = ref('ALL')
const page = ref(1)
const loading = ref(true)

/** How many proposals one page holds. Matches the other office list (OVCAF inbox). */
const PAGE_SIZE = 8

const statusOptions = OVCRIGE_OC_ENDORSEMENT_STATUSES

const loadEndorsedProposals = async () => {
  loading.value = true
  try {
    // Show proposals where REC approved and OVCRIGE needs to forward to OC. Ordering is
    // explicit so pagination is stable and matches the "Date Updated" column shown.
    const response = await api.get('/api/proposals', {
      params: {
        statusIn: [...OVCRIGE_OC_ENDORSEMENT_STATUSES],
        sort: 'updatedAt',
      },
      paramsSerializer: { indexes: null },
    })
    proposals.value = Array.isArray(response.data) ? response.data : []
  } catch (error) {
    console.error(error)
    proposals.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadEndorsedProposals)

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

const totalPages = computed(() => Math.max(1, Math.ceil(filteredProposals.value.length / PAGE_SIZE)))

const paginatedProposals = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return filteredProposals.value.slice(start, start + PAGE_SIZE)
})

const rangeStart = computed(() =>
  filteredProposals.value.length ? (page.value - 1) * PAGE_SIZE + 1 : 0,
)
const rangeEnd = computed(() => Math.min(page.value * PAGE_SIZE, filteredProposals.value.length))

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

const goToReview = (id) => {
  router.push({ name: 'OvcReviewProposal', params: { id } })
}
</script>

<style scoped>
.endorsed-page {
  padding: 25px;
}
.subtitle {
  color: #777;
  margin-bottom: 15px;
}
/* FILTER BAR */
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 15px;
}
.search-wrapper {
  position: relative;
  flex: 1 1 260px;
  min-width: 220px;
}
.search {
  width: 100%;
  padding: 10px 34px 10px 12px;
  border-radius: 8px;
  border: 1px solid #ddd;
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
  border: 1px solid #ddd;
  background: #fff;
  min-width: 170px;
}
/* Layout only; .btn-cancel supplies the neutral action tone. */
.reset-filters {
  padding: 10px 16px;
  border: 1px solid #ddd;
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
  background: #3c3b6e;
  color: white;
  padding: 10px;
  text-align: left;
}
td {
  padding: 10px;
  border-bottom: 1px solid #eee;
}
.empty-cell {
  text-align: center;
  color: #64748b;
  padding: 24px 12px;
}
/* Geometry only; the status tone comes from assets/status.css. */
.status {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
.view {
  padding: 6px 14px;
  border-radius: 20px;
  cursor: pointer;
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
  background: #3c3b6e;
  border-color: #3c3b6e;
  color: #fff;
  font-weight: 600;
}
</style>
