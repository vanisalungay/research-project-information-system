<template>
  <div class="end-page end-page--fluid rec-assigned-page">
    <header class="end-page-header">
      <h2>Assigned Proposals for Review</h2>
      <p class="end-subtitle">Review and evaluate proposals assigned by OVCRIGE</p>
    </header>

    <div class="end-card rec-assigned-card">
      <!-- FILTER BAR -->
      <div class="end-toolbar rec-toolbar">
        <input
          v-model="search"
          type="text"
          class="end-input rec-search"
          placeholder="Search by title, proponent, program or document ID..."
        />
        <select v-model="statusFilter" class="end-select rec-status">
          <option value="ALL">All Status</option>
          <option value="UNDER_REVIEW">{{ proposalStatusLabel('UNDER_REVIEW') }}</option>
          <option value="REC_APPROVED">{{ proposalStatusLabel('REC_APPROVED') }}</option>
          <option value="REC_REJECTED">{{ proposalStatusLabel('REC_REJECTED') }}</option>
          <option value="REC_REVISION">{{ proposalStatusLabel('REC_REVISION') }}</option>
        </select>
      </div>

      <!-- Loading / Error -->
      <p v-if="loading" class="rec-state">Loading proposals...</p>
      <p v-else-if="error" class="rec-state rec-error">{{ error }}</p>

      <!-- TABLE -->
      <template v-else>
        <div
          class="end-table-scroll"
          role="region"
          aria-label="Assigned proposals"
          tabindex="0"
        >
          <table class="end-table rec-assigned-table">
            <thead>
              <tr>
                <th>Proposal ID</th>
                <th>Title</th>
                <th>Proponent</th>
                <th>Date Submitted</th>
                <th>Status</th>
                <th class="text-right">Actions</th>
              </tr>
            </thead>
            <tbody v-if="filteredProposals.length === 0">
              <tr>
                <td colspan="6" class="rec-empty">No proposals found.</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="proposal in pagedProposals" :key="proposal.id">
                <td
                  class="cell-id"
                  :title="proposal.documentId || proposal.proposalCode || ''"
                >
                  {{ proposal.documentId || proposal.proposalCode || '—' }}
                </td>
                <td class="cell-title">
                  <span
                    class="title-text"
                    :title="proposal.projectTitle || proposal.title"
                  >
                    {{ proposal.projectTitle || proposal.title }}
                  </span>
                  <p class="subtext">{{ proposal.programTitle || 'N/A' }}</p>
                </td>
                <td class="cell-proponent">
                  {{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}
                </td>
                <td class="cell-date">{{ submittedDate(proposal) }}</td>
                <td>
                  <span class="end-badge" :class="statusTone(proposal.status)">
                    {{ proposalStatusLabel(proposal.status) }}
                  </span>
                </td>
                <td class="text-right">
                  <button
                    v-if="proposal.status === 'UNDER_REVIEW'"
                    class="end-btn is-view row-btn"
                    @click="goToReview(proposal.id)"
                  >
                    REVIEW
                  </button>
                  <button
                    v-else
                    class="end-btn is-view row-btn"
                    @click="goToView(proposal.id)"
                  >
                    VIEW
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- PAGINATION -->
        <PaginationBar
          v-if="filteredProposals.length"
          :page="page"
          :page-size="pageSize"
          :total="filteredProposals.length"
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
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel, proposalStatusMatches } from '@/utils/proposalStatus.js'
import '@/assets/endorsement.css'

const router = useRouter()
const proposals = ref([])
const loading = ref(true)
const error = ref(null)
const search = ref('')
const statusFilter = ref('ALL')

/** One page of the filtered worklist. */
const pageSize = ref(10)
const page = ref(1)

const loadProposals = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/proposals?statusIn=UNDER_REVIEW&statusIn=REC_APPROVED&statusIn=REC_REJECTED&statusIn=REC_REVISION')
    proposals.value = Array.isArray(res.data) ? res.data : []
    page.value = 1
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals.'
    proposals.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadProposals)

const filteredProposals = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return proposals.value.filter((p) => {
    if (statusFilter.value !== 'ALL' && !proposalStatusMatches(p.status, statusFilter.value)) {
      return false
    }
    if (!keyword) return true
    return (
      (p.projectTitle || p.title || '').toLowerCase().includes(keyword) ||
      (p.projectLeader || p.proponent?.name || '').toLowerCase().includes(keyword) ||
      (p.programTitle || '').toLowerCase().includes(keyword) ||
      String(p.documentId || p.proposalCode || '').toLowerCase().includes(keyword)
    )
  })
})

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredProposals.value.length / pageSize.value)),
)

const pagedProposals = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredProposals.value.slice(start, start + pageSize.value)
})

const rangeStart = computed(() =>
  filteredProposals.value.length ? (page.value - 1) * pageSize.value + 1 : 0,
)
const rangeEnd = computed(() =>
  Math.min(page.value * pageSize.value, filteredProposals.value.length),
)

watch([search, statusFilter], () => {
  page.value = 1
})
watch(totalPages, (count) => {
  if (page.value > count) page.value = count
})

/** The actual submission event, not the draft row's creation date. */
const submittedDate = (proposal) => {
  const value = proposal?.submittedAt || proposal?.createdAt
  return value ? String(value).substring(0, 10) : 'N/A'
}

const goToPage = (next) => {
  if (next < 1 || next > totalPages.value) return
  page.value = next
}

const setPageSize = (value) => {
  pageSize.value = Math.max(1, Number(value) || 10)
  page.value = 1
}

const goToReview = (id) => {
  router.push({ name: 'RecReviewForm', params: { id } })
}

const goToView = (id) => {
  router.push({ name: 'RecMeetingDetails', params: { id } })
}
</script>

<style>
.assigned-wrapper {
  padding: 20px;
}
.page-title {
  margin: 0;
  font-size: var(--ui-font-page-title);
  font-weight: var(--ui-weight-bold);
}
.page-subtitle {
  margin: 4px 0 18px;
  opacity: 0.7;
}
.filter-bar {
  background: white;
  padding: 15px;
  border-radius: 10px;
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
}
.search-bar {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.status-select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.proposal-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}
.proposal-table thead {
  background: #2f2a66;
  color: white;
}
.proposal-table th,
.proposal-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
}
.subtext {
  font-size: var(--ui-font-caption);
  opacity: 0.7;
}
.status-pill {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
}
.review-btn,
.view-btn {
  padding: 7px 14px;
  border-radius: 6px;
  cursor: pointer;
}
</style>

<style scoped>
/* ============================================================================
   Assigned Proposals presentation.

   The shared .end-* layer (assets/endorsement.css) supplies the page, table,
   badges, buttons and pager. These rules only tune the REC worklist: fixed
   column proportions, a readable title/ID, a compact status badge and the
   right-aligned action. The legacy global style block above is kept because
   its class names are shared with other statically imported pages.
   ============================================================================ */

.rec-assigned-page {
  width: 100%;
}

.rec-assigned-card {
  padding: 18px;
}

.rec-toolbar {
  margin-bottom: 14px;
}

.rec-search {
  flex: 1 1 240px;
  min-width: 0;
}

.rec-status {
  width: 210px;
  flex: 0 0 auto;
}

/* Higher specificity than the shared .end-page--fluid .end-table minimum. */
.end-page--fluid .rec-assigned-table {
  min-width: 900px;
  table-layout: fixed;
  font-size: var(--ui-font-small);
}

.rec-assigned-table th {
  background: #f6f7fd;
  color: #1c1a3c;
  padding: 10px;
  font-size: var(--ui-font-table-header);
  font-weight: var(--ui-weight-semibold);
  letter-spacing: var(--ui-tracking-caps);
  text-transform: uppercase;
  white-space: nowrap;
}

.rec-assigned-table th:nth-child(1) {
  width: 17%;
}

.rec-assigned-table th:nth-child(2) {
  width: 23%;
}

.rec-assigned-table th:nth-child(3) {
  width: 14%;
}

.rec-assigned-table th:nth-child(4) {
  width: 14%;
}

.rec-assigned-table th:nth-child(5) {
  width: 21%;
}

.rec-assigned-table th:nth-child(6) {
  width: 11%;
}

.rec-assigned-table td {
  padding: 9px 10px;
  vertical-align: middle;
  border-bottom: 1px solid #f1f5f9;
}

.rec-assigned-table tbody tr:hover td {
  background: #fafbff;
}

.rec-assigned-table th.text-right,
.rec-assigned-table td.text-right {
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

.cell-title .subtext {
  margin: 2px 0 0;
  font-size: var(--ui-font-caption);
  opacity: 1;
  color: var(--ui-text-muted);
  white-space: normal;
  overflow-wrap: anywhere;
}

.cell-proponent {
  font-size: var(--ui-font-small);
  color: #1f2937;
  overflow-wrap: anywhere;
}

.cell-date {
  font-size: var(--ui-font-caption);
  color: #5b6484;
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.rec-assigned-table .end-badge {
  padding: 2px 7px;
  font-size: var(--ui-font-caption);
}

.row-btn {
  padding: 6px 10px;
  font-size: var(--ui-control-font-sm);
  white-space: nowrap;
}

.rec-state {
  margin: 0;
  padding: 22px;
  font-size: var(--ui-font-small);
  color: var(--ui-text-muted);
}

.rec-error {
  color: #dc2626;
}

.rec-empty {
  padding: 24px 12px;
  text-align: center;
  color: var(--ui-text-muted);
}

@media (max-width: 640px) {
  .rec-assigned-card {
    padding: 14px;
  }

  .rec-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .rec-search,
  .rec-status {
    width: 100%;
    flex: 1 1 auto;
  }
}
</style>
