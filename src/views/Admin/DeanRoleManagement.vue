<template>
  <div class="page">
    <!-- Compact page header: title and subtitle on the left, the single create action
         on the upper-right. -->
    <header class="page-head">
      <div class="page-heading">
        <h2>Dean Role Management</h2>
        <p>Manage Dean accounts, departmental assignments, and their designated academic units.</p>
      </div>
      <button type="button" class="create-btn btn-primary" @click="openCreate">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"
          stroke-linecap="round" aria-hidden="true">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        Create Dean account
      </button>
    </header>

    <!-- Feedback -->
    <div v-if="successMessage" class="success-banner">{{ successMessage }}</div>

    <!-- Filters on the left, search on the right. The counts are derived from the
         assignment records the API returned, never from a hardcoded number. -->
    <div class="toolbar">
      <div class="filters" role="group" aria-label="Filter Dean assignments">
        <button
          v-for="option in filters"
          :key="option.value"
          type="button"
          class="filter-pill"
          :class="{ active: filter === option.value }"
          :aria-pressed="filter === option.value"
          @click="setFilter(option.value)"
        >
          <span>{{ option.label }}</span>
          <span class="filter-count">{{ filterCounts[option.value] }}</span>
        </button>
      </div>

      <label class="search-box">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <circle cx="11" cy="11" r="7" />
          <line x1="20" y1="20" x2="16.2" y2="16.2" />
        </svg>
        <input
          v-model="search"
          type="search"
          placeholder="Search name, email or college..."
          aria-label="Search Dean accounts"
        />
      </label>
    </div>

    <div v-if="error" class="error-banner" role="alert">
      <span>{{ error }}</span>
      <button type="button" class="retry-btn btn-secondary" @click="loadAssignments">Retry</button>
    </div>

    <!-- The whole table, its empty/loading state and its record count live in one
         rounded card, matching the reference management surfaces. -->
    <div class="table-card">
      <div class="table-scroll">
        <table>
          <thead>
            <tr>
              <th class="col-dean">DEAN</th>
              <th class="col-college">COLLEGE / DEPARTMENT</th>
              <th class="col-designation">DESIGNATION</th>
              <th class="col-status">STATUS</th>
              <th class="col-date">ASSIGNED</th>
              <!-- A sitting Dean has no revocation date, so the Active view drops the
                   column entirely. -->
              <th v-if="filter !== 'ACTIVE'" class="col-date">REVOKED</th>
              <!-- Revoked appointments are history: the Revoked view has no actions. -->
              <th v-if="filter !== 'REVOKED'" class="col-action">ACTION</th>
            </tr>
          </thead>

          <tbody>
            <!-- Loading -->
            <tr v-if="loading">
              <td :colspan="columnCount" class="state-cell">Loading Dean assignments...</td>
            </tr>

            <!-- Load failure: the banner above carries the retry. -->
            <tr v-else-if="error">
              <td :colspan="columnCount" class="state-cell">
                The Dean assignments could not be loaded.
              </td>
            </tr>

            <!-- Empty state, worded for the selected filter and the search. -->
            <tr v-else-if="rows.length === 0">
              <td :colspan="columnCount" class="state-cell">
                <span class="state-icon" aria-hidden="true">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7"
                    stroke-linecap="round" stroke-linejoin="round">
                    <path d="M22 10 12 5 2 10l10 5 10-5z" />
                    <path d="M6 12v5c0 1.7 2.7 3 6 3s6-1.3 6-3v-5" />
                  </svg>
                </span>
                {{ emptyMessage }}
              </td>
            </tr>

            <!-- Dean assignments: the sitting Dean of each college and every predecessor. -->
            <template v-else>
              <tr v-for="row in rows" :key="row.id">
                <td>
                  <div class="dean-cell">
                    <span class="dean-avatar" aria-hidden="true">{{ row.initials }}</span>
                    <span class="dean-identity">
                      <span class="dean-name">{{ row.name || '—' }}</span>
                      <span class="dean-email" :title="row.email || ''">{{ row.email || '—' }}</span>
                    </span>
                  </div>
                </td>

                <td>
                  <div class="college-cell">
                    <span class="college-name" :class="{ 'is-muted': !row.college.name }">
                      {{ row.college.name || 'Not assigned' }}
                    </span>
                    <span v-if="row.college.abbreviation" class="college-code">
                      {{ row.college.abbreviation }}
                    </span>
                  </div>
                </td>

                <td class="nowrap">{{ row.designation || '—' }}</td>

                <td>
                  <span class="status-pill" :class="row.statusKey === 'ACTIVE' ? 'is-active' : 'is-revoked'">
                    <span class="status-dot" aria-hidden="true"></span>
                    {{ deanAssignmentStatusLabel(row.status) }}
                  </span>
                </td>

                <td class="nowrap date-cell">{{ row.assignedLabel }}</td>
                <td
                  v-if="filter !== 'ACTIVE'"
                  class="nowrap date-cell"
                  :class="{ 'is-muted': !row.dateRevoked }"
                >
                  {{ row.revokedLabel }}
                </td>

                <td v-if="filter !== 'REVOKED'" class="action-cell">
                  <!-- Revoked appointments are history: they have no management action. -->
                  <button
                    v-if="row.statusKey === 'ACTIVE'"
                    type="button"
                    class="manage-btn"
                    @click="openManage(row)"
                  >
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
                      stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                      <circle cx="12" cy="12" r="3.2" />
                      <path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.9l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.9.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.9V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z" />
                    </svg>
                    Manage
                  </button>
                  <span v-else class="no-action">—</span>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <footer class="table-footer">
        Showing {{ rows.length }} of {{ filteredTotal }} dean records
      </footer>
    </div>

    <!-- The existing standard Create Account form, opened on the Dean role and locked to
         it. For a replacement it is opened with the appointment's college pre-filled and
         locked, and the backend creates the account and completes the succession in one
         call. The institutional email and temporary password are generated by the backend
         and shown once here, so account generation is never duplicated. -->
    <CreateUserModal
      v-if="showCreateModal"
      initial-role="DEAN"
      lock-role
      initial-designation="Dean"
      college-authority
      @close="closeCreate"
      @created="onDeanCreated"
    />

    <!-- Manage Dean: the same shared flow User Accounts opens. Replace Dean hands off to
         the Create Dean Account form with the appointment's college locked; the backend
         creates or reuses the account, transfers the pending requests and closes the
         outgoing appointment automatically, so there is no separate Revoke action. -->
    <ManageDeanFlow
      v-if="managedAssignment"
      :assignment="managedAssignment"
      @close="closeManage"
      @changed="onDeanReplaced"
    />
  </div>
</template>

<script setup>
/**
 * Dean Role Management (RPS ADMIN).
 *
 * The page reads every Dean appointment once (`status=ALL`) and derives the Active /
 * Revoked / All views, their counts, the search and the footer from that real data - the
 * page is unpaginated, so there is no server-side page or search to keep in step.
 *
 * Creating a Dean happens here, through the existing standard Create Account form opened
 * on the Dean role: the institutional email and the temporary password are generated by
 * the backend and shown once, so nothing about account generation is duplicated. Every
 * management action for an active appointment is reached through the one Manage button:
 * the modal shows the real appointment/account data, and Replace Dean opens the create
 * form locked to the appointment's college. The backend creates the account, transfers
 * the pending endorsement requests and closes the outgoing appointment as REVOKED in one
 * transaction; there is no separate Revoke action on this page.
 */
import { computed, onMounted, ref } from 'vue'
import api from '@/utils/api'
import CreateUserModal from '@/views/Admin/CreateUserModal.vue'
import ManageDeanFlow from '@/views/Admin/ManageDeanFlow.vue'
import { deanAssignmentStatusLabel } from '@/utils/statusCatalog.js'
import { collegeDisplayParts, initialsFromName } from '@/utils/proposalCardDisplay'

const assignments = ref([])
const loading = ref(true)
const error = ref('')
const successMessage = ref('')

const filter = ref('ACTIVE')
const search = ref('')
const filters = [
  { value: 'ACTIVE', label: deanAssignmentStatusLabel('ACTIVE') },
  { value: 'REVOKED', label: deanAssignmentStatusLabel('REVOKED') },
  { value: 'ALL', label: 'All' },
]

// The ordinary Create Dean Account form. Replacement is owned by the shared Manage Dean
// flow, so this form is only the standalone creation entry point.
const showCreateModal = ref(false)

// The appointment whose Manage Dean flow is open.
const managedAssignment = ref(null)

/** Counts shown in the filter badges, derived from the records the API returned. */
const filterCounts = computed(() => {
  let active = 0
  let revoked = 0
  for (const assignment of assignments.value) {
    const status = String(assignment.status || '').toUpperCase()
    if (status === 'ACTIVE') active += 1
    else if (status === 'REVOKED') revoked += 1
  }
  return { ACTIVE: active, REVOKED: revoked, ALL: assignments.value.length }
})

/** The records that belong to the selected filter. */
const filteredAssignments = computed(() => {
  if (filter.value === 'ALL') return assignments.value
  return assignments.value.filter(
    (assignment) => String(assignment.status || '').toUpperCase() === filter.value,
  )
})

/** Search is case-insensitive across the Dean's name, email and assigned college. */
const searchTerm = computed(() => search.value.trim().toLowerCase())

const searchedAssignments = computed(() => {
  const term = searchTerm.value
  if (!term) return filteredAssignments.value
  return filteredAssignments.value.filter((assignment) => (
    String(assignment.name || '').toLowerCase().includes(term)
    || String(assignment.email || '').toLowerCase().includes(term)
    || String(assignment.assignedCollege || '').toLowerCase().includes(term)
  ))
})

/** The table's read model: identity, college split, formatted dates and status. */
const rows = computed(() => searchedAssignments.value.map((assignment) => ({
  ...assignment,
  initials: initialsFromName(assignment.name),
  college: collegeDisplayParts(assignment.assignedCollege),
  statusKey: String(assignment.status || '').toUpperCase(),
  assignedLabel: formatDate(assignment.dateAssigned),
  revokedLabel: formatDate(assignment.dateRevoked),
})))

/** The footer's total is the selected filter's size, before the search narrows it. */
const filteredTotal = computed(() => filteredAssignments.value.length)

/**
 * The columns actually rendered: Active and Revoked each drop one column (the revoked
 * date and the action respectively), while All shows both. The state rows span exactly
 * the rendered table.
 */
const columnCount = computed(() => (filter.value === 'ALL' ? 7 : 6))

const emptyMessage = computed(() => {
  if (searchTerm.value) return 'No matching Dean accounts found.'
  if (filter.value === 'REVOKED') return 'No revoked Dean accounts.'
  if (filter.value === 'ACTIVE') return 'No active Dean accounts found.'
  return 'No Dean accounts found.'
})

/**
 * Loads the real appointment history once. The page is not paginated, so filtering,
 * searching, counting and the footer all work from this one truthful data set.
 */
async function loadAssignments() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get('/api/users/deans/assignments', {
      params: { status: 'ALL' },
    })
    assignments.value = Array.isArray(response.data) ? response.data : []
  } catch (requestError) {
    console.error('Could not load the Dean assignments', requestError)
    error.value = 'The Dean assignments could not be loaded. Please try again.'
    assignments.value = []
  } finally {
    loading.value = false
  }
}

function setFilter(value) {
  filter.value = value
}

/** Opens the account-creation form for a new Dean, not tied to an appointment. */
function openCreate() {
  successMessage.value = ''
  showCreateModal.value = true
}

function closeCreate() {
  showCreateModal.value = false
}

/**
 * A Dean account was created. The credentials stay on screen in the form.
 */
async function onDeanCreated() {
  successMessage.value = 'The Dean account was created. Give the Dean the credentials shown above - the temporary password must be changed at first login.'
  await loadAssignments()
}

/**
 * The shared Manage Dean flow completed a replacement. The backend has already closed the
 * previous appointment and appointed the new (or reused) account, so the reload shows the
 * change; the flow keeps the credentials on screen.
 */
async function onDeanReplaced(info) {
  successMessage.value = info?.reused
    ? 'The person already had an account, so it was reused: the Dean role was added, the new appointment is active, pending endorsement requests were transferred, and the previous appointment is kept as history (REVOKED). The existing password is unchanged.'
    : 'The Dean was replaced. The new account is now the sitting Dean, pending endorsement requests were transferred to it, and the previous appointment is kept as history (REVOKED).'
  await loadAssignments()
}

/** Opens Manage Dean for the appointment of the clicked row. */
function openManage(assignment) {
  successMessage.value = ''
  managedAssignment.value = assignment
}

function closeManage() {
  managedAssignment.value = null
}

/** Consistent short date across the Assigned and Revoked columns, e.g. "Nov 20, 2026". */
function formatDate(value) {
  if (!value) return '—'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return '—'
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

onMounted(loadAssignments)
</script>

<style scoped>
/* The layout already paints the light cool-gray page background and owns the outer
   page padding; this page only styles its own compact dashboard content. */
.page {
  padding: 0;
}

/* ===== Header ===== */
.page-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.page-heading h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: -0.01em;
  line-height: 1.25;
  color: #1c1a3c;
}

.page-heading p {
  margin: 3px 0 0;
  font-size: 11px;
  line-height: 1.4;
  color: #7b8298;
}

/* Layout only; the primary colour comes from assets/actions.css. */
.create-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  height: 32px;
  padding: 0 14px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 11.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.create-btn svg {
  width: 13px;
  height: 13px;
  flex: 0 0 auto;
}

.create-btn:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

/* ===== Feedback ===== */
.success-banner,
.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 11.5px;
  line-height: 1.4;
  margin-bottom: 12px;
}

.success-banner {
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  color: #065f46;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.retry-btn {
  height: 26px;
  padding: 0 10px;
  border-radius: 7px;
  font-family: inherit;
  font-size: 10.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

/* ===== Filters + search ===== */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.filter-pill {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  height: 30px;
  padding: 0 12px;
  border: 1px solid #dfe3ee;
  border-radius: 999px;
  background: #ffffff;
  color: #3b3d55;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s, color 0.15s;
}

.filter-pill:hover:not(.active) {
  background: #f6f7fb;
  border-color: #cdd3e3;
}

.filter-pill.active {
  background: #262342;
  border-color: #262342;
  color: #ffffff;
}

.filter-pill:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.filter-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #eef1f8;
  color: #4a5169;
  font-size: 9.5px;
  font-weight: 700;
  line-height: 1;
}

.filter-pill.active .filter-count {
  background: #ffd000;
  color: #262342;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 7px;
  width: 240px;
  max-width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #dfe3ee;
  border-radius: 8px;
  background: #ffffff;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.search-box:focus-within {
  border-color: #315bff;
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.12);
}

.search-box svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: #8a90a5;
}

.search-box input {
  width: 100%;
  min-width: 0;
  border: none;
  outline: none;
  background: transparent;
  font-family: inherit;
  font-size: 11px;
  color: #1c1a3c;
}

.search-box input::placeholder {
  color: #a0a5b6;
}

/* ===== Table card ===== */
.table-card {
  overflow: hidden;
  border: 1px solid #e6e8f0;
  border-radius: 12px;
  background: #ffffff;
}

.table-scroll {
  overflow-x: auto;
}

table {
  width: 100%;
  min-width: 880px;
  border-collapse: collapse;
}

thead th {
  padding: 8px 14px;
  background: #f7f8fc;
  border-bottom: 1px solid #eef0f6;
  color: #7b8298;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-align: left;
  white-space: nowrap;
}

.col-college {
  width: 24%;
}

.col-date {
  width: 11%;
}

.col-action {
  width: 10%;
  text-align: center;
}

tbody td {
  padding: 8px 14px;
  border-bottom: 1px solid #eef0f6;
  color: #2f3248;
  font-size: 11px;
  line-height: 1.4;
  vertical-align: middle;
}

tbody tr:last-child td {
  border-bottom: none;
}

tbody tr:hover {
  background: #f9fafe;
}

.nowrap {
  white-space: nowrap;
}

/* Dean identity */
.dean-cell {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 0;
}

.dean-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: #e9eefb;
  color: #262342;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.02em;
}

.dean-identity {
  display: flex;
  flex-direction: column;
  gap: 1px;
  min-width: 0;
}

.dean-name {
  font-size: 12px;
  font-weight: 600;
  line-height: 1.3;
  color: #1c1a3c;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dean-email {
  max-width: 230px;
  color: #8a90a5;
  font-size: 10px;
  line-height: 1.3;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* College */
.college-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
  min-width: 0;
}

.college-name {
  color: #2f3248;
  font-size: 11px;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.college-code {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  border-radius: 999px;
  background: #e6edfc;
  color: #31539b;
  font-size: 8.5px;
  font-weight: 700;
  letter-spacing: 0.04em;
  line-height: 1.5;
}

/* Status */
.status-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 8px;
  border-radius: 999px;
  font-size: 9.5px;
  font-weight: 700;
  line-height: 1.2;
  white-space: nowrap;
}

.status-dot {
  width: 5px;
  height: 5px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: currentColor;
}

.status-pill.is-active {
  background: #e8f7ee;
  color: #16794a;
}

.status-pill.is-revoked {
  background: #fdf0ef;
  color: #b42318;
}

/* Dates */
.date-cell {
  color: #5b6280;
  font-size: 10.5px;
}

.is-muted {
  color: #a0a5b6;
}

/* Manage action */
.action-cell {
  text-align: center;
}

.manage-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  height: 28px;
  padding: 0 10px;
  border: 1px solid #d9e2ff;
  border-radius: 7px;
  background: #eaf0ff;
  color: #2f57c9;
  font-family: inherit;
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
  transition: background 0.15s, border-color 0.15s;
}

.manage-btn:hover {
  background: #dfe8ff;
  border-color: #c8d6ff;
}

.manage-btn:focus-visible {
  outline: 2px solid #315bff;
  outline-offset: 2px;
}

.manage-btn svg {
  width: 12px;
  height: 12px;
  flex: 0 0 auto;
}

.no-action {
  color: #b0b6c6;
}

/* Loading / empty */
.state-cell {
  padding: 30px 16px;
  text-align: center;
  color: #7b8298;
  font-size: 11.5px;
}

.state-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-right: 8px;
  border-radius: 50%;
  background: #f2f4fa;
  color: #8a90a5;
  vertical-align: middle;
}

.state-icon svg {
  width: 16px;
  height: 16px;
}

/* Footer */
.table-footer {
  padding: 10px 14px;
  border-top: 1px solid #eef0f6;
  background: #ffffff;
  color: #8a90a5;
  font-size: 10px;
  letter-spacing: 0.01em;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .search-box {
    width: 100%;
    max-width: 320px;
  }
}

@media (max-width: 640px) {
  .page-head {
    flex-direction: column;
    align-items: stretch;
  }

  .create-btn {
    justify-content: center;
  }

  .toolbar {
    align-items: stretch;
  }

  .filters {
    width: 100%;
  }

  .search-box {
    width: 100%;
    max-width: none;
  }
}
</style>
