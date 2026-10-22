<template>
  <div class="revision-wrapper">
    <!-- HEADER -->
    <div class="revision-header">
      <div class="header-text">
        <h1>Revision Dashboard</h1>
        <p>Address the reviewer comments, then resubmit before each deadline.</p>
      </div>

      <div class="revision-count">
        <span class="count-dot"></span>
        {{ actionableRevisions.length }} proposals need revision
      </div>
    </div>

    <!-- SEARCH -->
    <div class="revision-search">
      <label
        class="search-label"
        for="revision-search"
      >
        Search Revisions
      </label>
      <input
        id="revision-search"
        v-model="searchQuery"
        type="text"
        class="search-input"
        placeholder="Search by proposal ID, title, project leader, or deadline..."
      />
    </div>

    <!-- LOADING -->
    <p v-if="loading">Loading...</p>

    <!-- ERROR -->
    <p v-if="error" class="error-text">
      {{ error }}
    </p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && revisions.length === 0">
      No revisions found.
    </p>

    <!-- NO SEARCH MATCHES -->
    <p v-else-if="!loading && filteredRevisions.length === 0">
      No revision requests match your search.
    </p>

    <!-- CARDS GRID -->
    <div class="revision-grid">
      <div
        v-for="revision in filteredRevisions"
        :key="revision.id"
        class="revision-card"
      >
        <!-- DAYS LEFT + STATUS/TITLE/ID -->
        <div class="card-header-row">
          <div
            class="days-indicator"
            :class="{ 'is-nearest': revision.isNearest }"
          >
            <span class="days-number">{{ revision.daysLeft }}</span>
            <span class="days-label">Days Left</span>
          </div>

          <div class="card-top">
            <div class="status-row">
              <span class="status-dot"></span>
              <span class="revision-tag">Revision Required</span>
            </div>

            <h3 class="proposal-title">{{ revision.title }}</h3>
            <p class="proposal-id">{{ revision.code }}</p>
          </div>
        </div>

        <!-- DEADLINE + RETURNED BY -->
        <div class="meta-row">
          <span class="deadline">
            <svg
              class="meta-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect>
              <line x1="16" y1="2" x2="16" y2="6"></line>
              <line x1="8" y1="2" x2="8" y2="6"></line>
              <line x1="3" y1="10" x2="21" y2="10"></line>
            </svg>
            <span class="deadline-label">Due</span>
            <span
              class="deadline-value"
              :class="{ overdue: revision.overdue }"
            >
              {{ revision.deadline }}
            </span>
          </span>

          <span v-if="revision.returnedBy" class="returned-by">
            <svg
              class="meta-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <polyline points="9 14 4 9 9 4"></polyline>
              <path d="M20 20v-7a4 4 0 0 0-4-4H4"></path>
            </svg>
            Returned by {{ revision.returnedBy }}
          </span>
        </div>

        <!-- REVIEWER COMMENTS -->
        <div class="comments-box">
          <div class="comments-label">Reviewer Comments</div>
          <p class="comment-text">{{ revision.comment }}</p>
          <div v-if="revision.rpsNote" class="rps-note">
            <strong>RPS Note:</strong>
            <p>{{ revision.rpsNote }}</p>
          </div>
        </div>

        <!-- ACTIONS -->
        <div class="card-actions">
          <button
            class="submit-btn"
            @click="submitRevision(revision.id)"
          >
            <svg
              class="btn-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M17 3a2.828 2.828 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5L17 3z"></path>
            </svg>
            Submit Revision
          </button>

          <button
            class="details-btn"
            @click="viewDetails(revision.id)"
          >
            <svg
              class="btn-icon"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
              <circle cx="12" cy="12" r="3"></circle>
            </svg>
            View Details
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'
import { currentUserId } from '@/utils/session'
import {
  compareRevisionDeadlines,
  isRevisionDeadlinePassed,
  revisionDaysLeft,
} from '@/utils/revisionDeadline'

const router = useRouter()

/**
 * EMPTY STATE (NO MOCK DATA)
 */
const revisions = ref<any[]>([])
const loading = ref(false)
const error = ref<string | null>(null)
const searchQuery = ref('')

/**
 * FETCH FROM BACKEND
 * Expected:
 * GET /api/revisions
 */
function formatDeadline(value) {
  if (!value) return 'No deadline set'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

// Build a lowercased, searchable representation of a revision's deadline using
// both the date format shown on the card and the underlying data format.
function deadlineSearchText(revision: any) {
  const displayed = String(revision.deadline || '').toLowerCase()
  if (!revision.deadlineRaw) return displayed
  const d = new Date(revision.deadlineRaw)
  if (isNaN(d.getTime())) return displayed
  return [displayed, d.toLocaleString(), d.toLocaleDateString(), d.toISOString()]
    .join(' ')
    .toLowerCase()
}

// Client-side search across the fields shown on each revision card: formatted
// proposal ID, proposal title, project/team leader, and the revision deadline.
// Matching is case-insensitive and supports partial matches. Clearing the input
// restores the full list of revision requests the user is authorized to see.
const filteredRevisions = computed(() => {
  const term = searchQuery.value.trim().toLowerCase()
  if (!term) return revisions.value

  return revisions.value.filter(revision => {
    const title = String(revision.title || '').toLowerCase()
    const code = String(revision.code || '').toLowerCase()
    const leader = String(revision.projectLeader || '').toLowerCase()
    return (
      title.includes(term) ||
      code.includes(term) ||
      leader.includes(term) ||
      deadlineSearchText(revision).includes(term)
    )
  })
})

// Header summary: only the revision requests the proponent can still revise and resubmit.
// The queue already holds just the RPS-forwarded revision requests (the status rule in
// fetchRevisions), so the remaining condition is the deadline: a request whose deadline
// has passed is no longer counted, even though it stays visible further down the list.
// `overdue` is set from the shared deadline rule, so the count, the ordering and the
// expired card styling all agree.
const actionableRevisions = computed(() =>
  revisions.value.filter(revision => !revision.overdue)
)

async function fetchRevisions() {
  loading.value = true
  error.value = null

  try {
    const requestUserId = currentUserId()
    revisions.value = []
    if (requestUserId) {
      // The backend scopes this response to the authenticated proponent, so no
      // client-side owner filtering is used.
      const res = await api.get('/api/proposals')
      // Discard a response if the session changed while it was in flight.
      if (currentUserId() !== requestUserId) return
      const myProposals = Array.isArray(res.data) ? res.data : []

      // Only RPS-forwarded revision requests should appear in the proponent's queue.
      revisions.value = myProposals
        .filter(p => p.status === 'RPS_RETURNED')
        .map(p => ({
        id: p.id,
        title: p.projectTitle || 'Untitled Proposal',
        code: p.documentId || p.proposalCode || '—',
        projectLeader: p.projectLeader || p.teamLeader || '',
        returnedBy: p.returnedByOffice || null,
        comment: p.returnRemarks || p.remarks || 'No specific comments provided. Please review your proposal and make necessary revisions.',
        rpsNote: p.revisionNotes || null,
        deadline: p.revisionDeadline ? formatDeadline(p.revisionDeadline) : 'No deadline set',
        deadlineRaw: p.revisionDeadline || null,
        overdue: isRevisionDeadlinePassed(p.revisionDeadline),
        status: p.status,
        daysLeft: revisionDaysLeft(p.revisionDeadline),
        deadlineTs: p.revisionDeadline ? new Date(p.revisionDeadline).getTime() : NaN
      }))

      // Display order for the proponent: revision requests that can still be revised and
      // resubmitted come first (nearest deadline first), followed by deadline-expired
      // requests (most recently expired first). The shared deadline rule decides what
      // counts as expired, and a copy is sorted so the fetched data is never modified.
      revisions.value = revisions.value
        .slice()
        .sort((a, b) => compareRevisionDeadlines(a.deadlineRaw, b.deadlineRaw))

      // Visual emphasis only: identify the revision with the nearest upcoming
      // deadline (does not change the displayed order).
      let nearestTs = Infinity
      revisions.value.forEach(r => {
        if (!r.overdue && !isNaN(r.deadlineTs) && r.deadlineTs < nearestTs) {
          nearestTs = r.deadlineTs
        }
      })
      revisions.value.forEach(r => {
        r.isNearest = !r.overdue && !isNaN(r.deadlineTs) && r.deadlineTs === nearestTs
      })
    } else {
      revisions.value = []
    }
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load revisions.'
    revisions.value = []
  } finally {
    loading.value = false
  }
}

/**
 * ROUTING (backend-ready)
 */
const submitRevision = (id: number) => {
  router.push(`/submit-revision/${id}`)
}

const viewDetails = (id: number) => {
  router.push(`/proponent-prop-details/${id}`)
}

onMounted(() => {
  fetchRevisions()
})
</script>

<style scoped>
.revision-wrapper {
  max-width: 100%;
  box-sizing: border-box;
  padding: 2rem;
  background: #f3f4f6;
  min-height: 100vh;
  font-family: 'Inter', sans-serif;
  color: #1f1f3d;
}

/* HEADER */
.revision-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 1rem;
  flex-wrap: wrap;
  margin-bottom: 1.5rem;
}

.header-text h1 {
  margin: 0;
  font-size: 1.75rem;
  font-weight: 700;
  color: #1f1f3d;
  line-height: 1.2;
}

.header-text p {
  margin: 0.375rem 0 0;
  font-size: 0.875rem;
  color: #6b7280;
  font-weight: 400;
}

.revision-count {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: #fef3c7;
  color: #92400e;
  padding: 0.375rem 0.875rem;
  border-radius: 999px;
  font-size: 0.8125rem;
  font-weight: 600;
  white-space: nowrap;
}

.count-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: #d97706;
  flex-shrink: 0;
}

/* SEARCH */
.revision-search {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  margin-bottom: 1.5rem;
}

.search-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #374151;
}

.search-input {
  width: 100%;
  box-sizing: border-box;
  padding: 0.625rem 0.875rem;
  border: 1px solid #d1d5db;
  border-radius: 0.5rem;
  font-size: 0.875rem;
  font-family: inherit;
  color: #1f1f3d;
  background: #ffffff;
}

.search-input:focus {
  outline: none;
  border-color: #ffd803;
  box-shadow: 0 0 0 3px rgba(255, 216, 3, 0.25);
}

/* GRID */
.revision-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 1.25rem;
  align-items: stretch;
}

/* CARD */
.revision-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 0.875rem;
  padding: 1.25rem;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  display: flex;
  flex-direction: column;
}

/* CARD HEADER ROW (days-left indicator + proposal info) */
.card-header-row {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.card-top {
  flex: 1;
  min-width: 0;
}

/* DAYS LEFT INDICATOR */
.days-indicator {
  width: 64px;
  height: 64px;
  border-radius: 0.625rem;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 0.125rem;
  flex-shrink: 0;
  background: #e5e7eb;
}

.days-number {
  font-size: 1.5rem;
  font-weight: 700;
  line-height: 1;
  color: #111827;
}

.days-label {
  font-size: 0.5rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #4b5563;
}

.days-indicator.is-nearest {
  background: #1f1f3d;
}

.days-indicator.is-nearest .days-number {
  color: #ffd803;
}

.days-indicator.is-nearest .days-label {
  color: #f9fafb;
}

/* STATUS */
.status-row {
  display: flex;
  align-items: center;
  gap: 0.375rem;
}

.status-dot {
  width: 0.5rem;
  height: 0.5rem;
  border-radius: 50%;
  background: #dc2626;
  flex-shrink: 0;
}

.revision-tag {
  color: #b91c1c;
  font-size: 0.7rem;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

/* TITLE & ID */
.proposal-title {
  margin: 0.5rem 0 0;
  font-size: 1.05rem;
  font-weight: 700;
  color: #1f1f3d;
  line-height: 1.3;
}

.proposal-id {
  margin: 0.25rem 0 0;
  font-size: 0.75rem;
  color: #64748b;
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  overflow-wrap: anywhere;
}

/* META */
.meta-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 0.5rem 1rem;
  margin-top: 1rem;
  padding-top: 0.875rem;
  border-top: 1px solid #f1f5f9;
}

.deadline {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #1f2937;
}

.deadline-label {
  color: #6b7280;
}

.deadline-value {
  font-weight: 600;
  color: #b45309;
}

.deadline-value.overdue {
  color: #dc2626;
}

.returned-by {
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.75rem;
  color: #6b7280;
}

.meta-icon {
  width: 0.875rem;
  height: 0.875rem;
  flex-shrink: 0;
  color: #9ca3af;
}

/* COMMENTS */
.comments-box {
  background: #f8fafc;
  border-radius: 0.5rem;
  padding: 0.875rem 1rem;
  margin-top: 1rem;
}

.comments-label {
  font-size: 0.6875rem;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #6b7280;
  margin-bottom: 0.375rem;
}

.comment-text {
  margin: 0;
  color: #374151;
  font-size: 0.875rem;
  line-height: 1.5;
}

.rps-note {
  margin-top: 0.75rem;
  padding-top: 0.75rem;
  border-top: 1px solid #e5e7eb;
}

.rps-note strong {
  color: #4f46e5;
}

.rps-note p {
  margin: 0.25rem 0 0;
  color: #374151;
  font-size: 0.875rem;
}

/* ACTIONS */
.card-actions {
  display: flex;
  gap: 0.625rem;
  margin-top: auto;
  padding-top: 1.25rem;
}

.submit-btn {
  background: #ffd803;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  font-weight: 600;
  cursor: pointer;
  color: #1f1f3d;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
}

.details-btn {
  background: #ffffff;
  border: 1px solid #d1d5db;
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  font-weight: 600;
  color: #1f2937;
  display: inline-flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.875rem;
}

.btn-icon {
  width: 0.875rem;
  height: 0.875rem;
  flex-shrink: 0;
}

.submit-btn:hover,
.details-btn:hover {
  opacity: 0.85;
}

.error-text {
  color: #dc2626;
}

/* RESPONSIVE */
@media (max-width: 820px) {
  .revision-grid {
    grid-template-columns: 1fr;
  }
}
</style>
