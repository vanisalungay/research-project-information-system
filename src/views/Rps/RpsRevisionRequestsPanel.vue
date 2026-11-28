<template>
  <div class="panel">
    <div class="tabs">
      <button :class="['tab', { active: activeTab === 'awaiting' }]" @click="activeTab = 'awaiting'">
        Awaiting RPS Review ({{ awaiting.length }})
      </button>
      <button :class="['tab', { active: activeTab === 'forwarded' }]" @click="activeTab = 'forwarded'">
        Forwarded to Proponent ({{ forwarded.length }})
      </button>
    </div>

    <p v-if="loading" class="muted">Loading...</p>
    <p v-else-if="error" class="error">{{ error }}</p>

    <template v-else-if="activeTab === 'awaiting'">
      <p v-if="awaiting.length === 0" class="muted">No revision requests found.</p>
      <div v-for="p in awaitingPageItems" :key="p.id" class="card">
        <div class="card-top">
          <h3>{{ p.projectTitle || 'Untitled' }}</h3>
          <span class="badge" :class="statusTone(p.status)">{{ proposalStatusLabel(p.status) }}</span>
        </div>
        <div class="meta">
          <div><label>Returned by</label><span>{{ p.returnedByOffice || '—' }}</span></div>
          <div><label>User</label><span>{{ p.returnedByName || '—' }}</span></div>
          <div><label>Date returned</label><span>{{ formatDate(p.returnedAt) }}</span></div>
        </div>
        <div class="remarks">
          <label>Revision remarks</label>
          <p>{{ p.returnRemarks || p.remarks || 'No remarks provided.' }}</p>
        </div>

        <div v-if="(flaggedSections[p.id] || []).length > 0" class="section-flags">
          <label>Section-specific remarks</label>
          <div v-for="section in flaggedSections[p.id]" :key="section.key" class="section-flag">
            <span class="section-flag-head">
              {{ section.key }} · {{ section.label }}
              <template v-if="section.reviewerRole"> — {{ section.reviewerRole }}</template>
            </span>
            <p v-for="(remark, index) in section.remarks" :key="remark.id ?? index" class="section-flag-text">
              {{ remark.remark }}
            </p>
          </div>
        </div>

        <div class="actions">
          <router-link :to="`/rps-forward-revision/${p.id}`" class="btn btn-primary">Review &amp; Forward</router-link>
        </div>
      </div>
      <PaginationBar
        v-if="awaiting.length"
        :page="awaitingPage"
        :page-size="awaitingPageSize"
        :total="awaiting.length"
        item-label="revision request"
        item-label-plural="revision requests"
        @update:page="setAwaitingPage"
        @update:page-size="setAwaitingPageSize"
      />
    </template>

    <template v-else>
      <p v-if="forwarded.length === 0" class="muted">No revision requests found.</p>
      <div v-for="p in forwardedPageItems" :key="p.id" class="card">
        <div class="card-top">
          <h3>{{ p.projectTitle || 'Untitled' }}</h3>
          <span class="badge" :class="statusTone(p.status)">{{ proposalStatusLabel(p.status) }}</span>
        </div>
        <div class="meta">
          <div><label>Returned by</label><span>{{ p.returnedByOffice || '—' }}</span></div>
          <div><label>Deadline</label><span>{{ formatDate(p.revisionDeadline) }}</span></div>
          <div><label>Forwarded on</label><span>{{ formatDate(p.revisionForwardedAt) }}</span></div>
          <div>
            <label>Deadline status</label>
            <span :class="{ overdue: isOverdue(p) }">{{ isOverdue(p) ? 'Overdue' : 'On time' }}</span>
          </div>
        </div>
        <div class="remarks">
          <label>Revision remarks</label>
          <p>{{ p.returnRemarks || p.remarks || 'No remarks provided.' }}</p>
          <p v-if="p.revisionNotes" class="rps-note"><label>RPS note</label>{{ p.revisionNotes }}</p>
        </div>
        <p class="hint">The proponent has not yet resubmitted this revision. Once submitted, it will appear under Submitted Proposals.</p>
      </div>
      <PaginationBar
        v-if="forwarded.length"
        :page="forwardedPage"
        :page-size="forwardedPageSize"
        :total="forwarded.length"
        item-label="revision request"
        item-label-plural="revision requests"
        @update:page="setForwardedPage"
        @update:page-size="setForwardedPageSize"
      />
    </template>
  </div>
</template>

<script setup>
import { ref, onActivated, onMounted, watch } from 'vue'
import api from '@/utils/api'
import PaginationBar from '@/components/PaginationBar.vue'
import { usePagination } from '@/composables/usePagination.js'
import { activeRemarksBySection, PROPOSAL_SECTIONS } from '@/utils/proposalSections'
import { statusTone } from '@/utils/statusTone.js'
import { proposalStatusLabel } from '@/utils/proposalStatus.js'

const awaiting = ref([])
const forwarded = ref([])
const loading = ref(false)
const error = ref('')
const hasLoaded = ref(false)
const activeTab = ref('awaiting')
// Section remarks returned by the reviewing office, keyed by proposal id.
const flaggedSections = ref({})

// Each tab pages its own list. The totals are the real filtered totals, so the pager stays
// accurate when a revision is forwarded or a rejection is excluded.
const {
  page: awaitingPage,
  pageSize: awaitingPageSize,
  pageItems: awaitingPageItems,
  setPage: setAwaitingPage,
  setPageSize: setAwaitingPageSize,
  reset: resetAwaitingPage,
} = usePagination(awaiting)
const {
  page: forwardedPage,
  pageSize: forwardedPageSize,
  pageItems: forwardedPageItems,
  setPage: setForwardedPage,
  setPageSize: setForwardedPageSize,
  reset: resetForwardedPage,
} = usePagination(forwarded)

// Switching tabs is a criteria change: the next list starts on its first page.
watch(activeTab, (tab) => {
  if (tab === 'awaiting') resetAwaitingPage()
  else resetForwardedPage()
})

/**
 * OVCRIGE rejections share RETURNED_TO_RPS with ordinary revision returns. They are
 * not revision requests: the Submitted Proposals page lists them under Rejected
 * Proposals and ORPS completes them through the rejection action. An OVCRIGE
 * return-for-revision keeps return_kind=REVISION and stays here.
 */
const isOvcrigeRejection = (proposal) =>
  String(proposal?.returnKind || '').toUpperCase() === 'REJECTION'

function formatDate(value) {
  if (!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

function isOverdue(p) {
  if (!p.revisionDeadline) return false
  const d = new Date(p.revisionDeadline)
  return !isNaN(d.getTime()) && d.getTime() < Date.now()
}

async function fetchSectionRemarks(proposalList) {
  const results = await Promise.all(
    proposalList.map(async (p) => {
      try {
        const res = await api.get(`/api/proposals/${p.id}/section-remarks`)
        const map = activeRemarksBySection(res.data)
        return [
          p.id,
          PROPOSAL_SECTIONS.filter((section) => (map[section.key] || []).length > 0).map((section) => ({
            key: section.key,
            label: section.label,
            reviewerRole: map[section.key][0]?.reviewerRole || '',
            remarks: map[section.key]
          }))
        ]
      } catch (err) {
        // The request list itself must still render if the remarks cannot be read.
        console.error(err)
        return [p.id, []]
      }
    })
  )
  flaggedSections.value = Object.fromEntries(results)
}

async function fetchProposals() {
  loading.value = true
  error.value = ''
  try {
    // Each tab is ordered by the event that put the proposal there: the office's return
    // for "Awaiting RPS Review", and RPS's forward to the proponent for "Forwarded".
    const [awaitingRes, forwardedRes] = await Promise.all([
      api.get('/api/proposals?statusIn=RETURNED_TO_RPS&sort=returnedAt'),
      api.get('/api/proposals?statusIn=RPS_RETURNED&sort=revisionForwardedAt'),
    ])
    awaiting.value = (Array.isArray(awaitingRes.data) ? awaitingRes.data : [])
      .filter((p) => !isOvcrigeRejection(p))
    forwarded.value = (Array.isArray(forwardedRes.data) ? forwardedRes.data : [])
      .filter((p) => !isOvcrigeRejection(p))
    await fetchSectionRemarks([...awaiting.value, ...forwarded.value])
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load revision requests.'
  } finally {
    loading.value = false
    hasLoaded.value = true
  }
}

onMounted(fetchProposals)

// The parent keeps each category alive: returning to this tab after RPS forwarded a
// revision (or a proposal was returned) reloads the real records instead of cached rows.
onActivated(() => {
  if (hasLoaded.value) fetchProposals()
})
</script>

<style scoped>
.panel {
  width: 100%;
  font-family: var(--ui-font-ui);
}

/* TABS */
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 12px;
}

.tab {
  padding: 7px 13px;
  border-radius: 8px;
  border: 1px solid #e3e6ef;
  background: #ffffff;
  color: #25245b;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-medium);
  font-family: inherit;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.tab:hover {
  border-color: #25245b;
}

.tab:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.tab.active {
  background: #25245b;
  border-color: #25245b;
  color: #ffd400;
  font-weight: var(--ui-weight-semibold);
}

.muted {
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
}

.error {
  color: #dc2626;
  font-size: var(--ui-font-caption);
}

/* CARDS */
.card {
  background: #ffffff;
  border: 1px solid #e3e6ef;
  border-radius: 10px;
  padding: 14px 16px;
  margin-bottom: 12px;
}

.card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 10px;
}

.card-top h3 {
  margin: 0;
  font-size: var(--ui-font-body);
  font-weight: var(--ui-weight-semibold);
  color: #20234f;
}

/* Geometry only; the `.is-*` tone colours come from assets/status.css. */
.badge {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  white-space: nowrap;
}

.meta {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px 14px;
  margin: 10px 0;
}

.meta label {
  display: block;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: var(--ui-text-muted);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
}

.meta span {
  font-size: var(--ui-font-caption);
  color: #20234f;
}

.meta .overdue {
  color: #dc2626;
  font-weight: var(--ui-weight-bold);
}

.remarks {
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  padding: 8px 12px;
  border-radius: 6px;
}

.remarks label {
  display: block;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: var(--ui-text-muted);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
}

.remarks p {
  margin: 4px 0 0;
  color: #475569;
  font-size: var(--ui-font-caption);
  white-space: pre-wrap;
}

.remarks .rps-note {
  margin-top: 8px;
  color: #3730a3;
}

.section-flags {
  margin-top: 10px;
  background: #eef2ff;
  border-left: 3px solid #4f46e5;
  padding: 8px 12px;
  border-radius: 6px;
}

.section-flags label {
  display: block;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: var(--ui-text-muted);
  text-transform: uppercase;
  letter-spacing: var(--ui-tracking-caps);
}

.section-flag + .section-flag {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #c7d2fe;
}

.section-flag-head {
  display: block;
  font-size: var(--ui-font-caption);
  font-weight: var(--ui-weight-semibold);
  color: #3730a3;
}

.section-flag-text {
  margin: 3px 0 0;
  font-size: var(--ui-font-caption);
  color: #475569;
  white-space: pre-wrap;
}

.actions {
  margin-top: 12px;
}

.btn {
  display: inline-block;
  padding: 8px 15px;
  border-radius: 7px;
  text-decoration: none;
  font-weight: var(--ui-weight-semibold);
  font-size: var(--ui-control-font-sm);
  transition: background-color 0.15s ease;
}

.hint {
  color: var(--ui-text-muted);
  font-size: var(--ui-font-caption);
  margin: 10px 0 0;
}
</style>
