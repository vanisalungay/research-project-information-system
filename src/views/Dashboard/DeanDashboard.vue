<template>
  <!--
    The Dean / Department Chair dashboard.

    Only the presentation changed: the greeting, the three counts, the pending requests and
    the recent decisions are the same records, read from the same endpoint and bound to the
    same computed values. The page reuses the shared design layer (assets/dashboard.css and
    assets/endorsement.css) - the same banner, cards, tables and pills the other role
    dashboards use - so every dashboard belongs to one design system.

    The .end-page--dean modifier stays on the root: it scopes the Dean polish in
    assets/endorsement.css to the two Dean screens.
  -->
  <div class="end-page end-page--fluid end-page--dean">
    <div class="dash-page">
      <!-- ===================== WELCOME ===================== -->
      <section class="dash-banner">
        <span class="dash-avatar" aria-hidden="true" v-html="icons.dean"></span>
        <div class="dash-banner-text">
          <h2 class="dash-banner-title">Welcome, {{ displayName }}!</h2>
          <p class="dash-banner-sub">
            Review and decide the endorsement requests of your college. Everything waiting for
            your decision, and everything you have decided recently, is summarised below.
          </p>
        </div>
      </section>

      <!-- ===================== STATISTICS ===================== -->
      <section class="dash-stats">
        <div class="dash-stat dash-stat--amber">
          <div class="dash-stat-head">
            <span class="dash-stat-icon" v-html="icons.clock"></span>
            <span class="dash-stat-label">Pending requests</span>
          </div>
          <div class="dash-stat-value">{{ pending.length }}</div>
        </div>

        <div class="dash-stat dash-stat--green">
          <div class="dash-stat-head">
            <span class="dash-stat-icon" v-html="icons.check"></span>
            <span class="dash-stat-label">Approved</span>
          </div>
          <div class="dash-stat-value">{{ approved.length }}</div>
        </div>

        <div class="dash-stat dash-stat--red">
          <div class="dash-stat-head">
            <span class="dash-stat-icon" v-html="icons.rejected"></span>
            <span class="dash-stat-label">Rejected</span>
          </div>
          <div class="dash-stat-value">{{ rejected.length }}</div>
        </div>
      </section>

      <!-- ===================== LOADING / ERROR ===================== -->
      <section v-if="loading" class="dash-section">
        <p class="dash-empty">Loading your endorsement requests…</p>
      </section>

      <section v-else-if="error" class="dash-section">
        <p class="end-field-error">{{ error }}</p>
        <button class="end-btn is-primary retry-btn" @click="load">Retry</button>
      </section>

      <template v-else>
        <!-- ===================== PENDING ENDORSEMENT REQUESTS ===================== -->
        <section class="dash-section">
          <header class="dash-section-head">
            <span class="dash-section-icon" v-html="icons.clock"></span>
            <h3 class="dash-section-title">Pending endorsement requests</h3>
            <div class="dash-section-actions">
              <router-link class="section-link" to="/dean-requests">
                <span>See all requests</span>
                <span class="section-link-arrow" aria-hidden="true">&rarr;</span>
              </router-link>
            </div>
          </header>

          <p v-if="pending.length === 0" class="dash-empty">
            Nothing is waiting for your endorsement right now.
          </p>

          <div v-else class="end-table-scroll">
            <table class="end-table dean-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Proponent</th>
                  <th>Project title</th>
                  <th>College</th>
                  <th>Submitted</th>
                  <th class="text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="request in pending" :key="request.id">
                  <td class="cell-id">{{ request.referenceNumber }}</td>
                  <td>{{ request.proponentName }}</td>
                  <td class="cell-title">
                    {{ request.projectTitle }}
                    <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
                  </td>
                  <td>{{ request.college }}</td>
                  <td class="cell-id">{{ formatEndorsementTimestamp(request.createdAt) }}</td>
                  <td class="text-right">
                    <router-link class="end-btn is-primary row-btn" :to="`/dean-requests/${request.id}`">
                      Review
                    </router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>

        <!-- ===================== RECENTLY DECIDED ===================== -->
        <section class="dash-section">
          <header class="dash-section-head">
            <span class="dash-section-icon" v-html="icons.check"></span>
            <h3 class="dash-section-title">Recently decided</h3>
          </header>

          <p v-if="decided.length === 0" class="dash-empty">No decisions recorded yet.</p>

          <div v-else class="end-table-scroll">
            <table class="end-table dean-table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Project title</th>
                  <th>Decided</th>
                  <th>Status</th>
                  <th class="text-right">Action</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="request in decided" :key="request.id">
                  <td class="cell-id">{{ request.referenceNumber }}</td>
                  <td class="cell-title">
                    {{ request.projectTitle }}
                    <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
                  </td>
                  <td class="cell-id">{{ formatEndorsementTimestamp(request.decidedAt) }}</td>
                  <td>
                    <span class="end-badge" :class="endorsementStatusTone(request.status)">
                      {{ endorsementStatusLabel(request.status) }}
                    </span>
                  </td>
                  <td class="text-right">
                    <router-link class="end-btn row-btn" :to="`/dean-requests/${request.id}`">
                      View
                    </router-link>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </template>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import '@/assets/dashboard.css'
import '@/assets/endorsement.css'
import { useUserDataStore } from '@/stores/userData'
import {
  ENDORSEMENT_STATUS,
  endorsementStatusLabel,
  endorsementStatusTone,
  fetchEndorsementRequests,
  formatEndorsementTimestamp,
} from '@/utils/endorsement.js'

const userStore = useUserDataStore()
const displayName = computed(() => userStore.user?.name || 'Dean')

/**
 * The icons of the page. They are inline SVGs on purpose: the application ships no icon
 * dependency, and one helper keeps every icon the same size, stroke and alignment. The map
 * is presentation only - no count, request or decision depends on it.
 */
const svg = (paths) =>
  `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" ` +
  `stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths}</svg>`

const icons = {
  dean: svg(
    '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>' +
      '<path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>',
  ),
  clock: svg('<circle cx="12" cy="12" r="9"/><path d="M12 7.5V12l3 2"/>'),
  check: svg('<circle cx="12" cy="12" r="9"/><path d="m8.5 12.5 2.5 2.5 4.5-5"/>'),
  rejected: svg('<circle cx="12" cy="12" r="9"/><path d="m9.5 9.5 5 5M14.5 9.5l-5 5"/>'),
}

const requests = ref([])
const loading = ref(true)
const error = ref('')

const pending = computed(() =>
  requests.value.filter((request) => request.status === ENDORSEMENT_STATUS.PENDING),
)
const approved = computed(() =>
  requests.value.filter((request) => request.status === ENDORSEMENT_STATUS.APPROVED),
)
const rejected = computed(() =>
  requests.value.filter((request) => request.status === ENDORSEMENT_STATUS.REJECTED),
)
/** The most recent decisions, newest first (the list already arrives that way). */
const decided = computed(() => requests.value.filter((r) => r.status !== ENDORSEMENT_STATUS.PENDING).slice(0, 5))

async function load() {
  loading.value = true
  error.value = ''
  try {
    requests.value = await fetchEndorsementRequests()
  } catch (err) {
    error.value =
      err?.response?.data?.error || 'Your endorsement requests could not be loaded.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
/* ============================================================================
   Dean dashboard presentation.

   The page reuses the shared design layer: assets/dashboard.css provides the welcome
   banner, the statistic cards and the section cards, and assets/endorsement.css provides
   the table, the status pills and the buttons. Only what is specific to this screen is
   declared here, so nothing can leak into another page.
   ============================================================================ */

/* The banner avatar carries an icon injected with v-html, so it is reached through :deep(). */
.dash-avatar :deep(svg) {
  width: 26px;
  height: 26px;
  stroke-width: 1.8;
}

/* The table keeps the light header band the other role dashboards use. This rule is scoped
   to the component, so the shared .end-page--dean header (used by the endorsement pages) is
   left exactly as it is. */
.end-page--dean .end-table thead th {
  background: #f6f7fd;
  color: #1c1a3c;
  border-bottom: none;
}

.dean-table th.text-right,
.dean-table td.text-right {
  text-align: right;
}

.cell-title {
  font-weight: 600;
  color: #1c1a3c;
}

.cell-id {
  font-size: 0.8125rem;
  color: #5b6484;
  white-space: nowrap;
}

/* A table row action is smaller than a page action. */
.row-btn {
  padding: 7px 14px;
  font-size: 0.8125rem;
}

/* The error card keeps its Retry button at the start of its own row. */
.retry-btn {
  align-self: flex-start;
}

/* ===== Section links (the "See all requests" control) ===== */
.section-link {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 6px;
  border-radius: 8px;
  background: transparent;
  color: #315bff;
  font-family: inherit;
  font-size: 0.8125rem;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.section-link:hover {
  background: #eef1ff;
  color: #1d40cc;
}

.section-link:focus-visible {
  outline: 2px solid #25245b;
  outline-offset: 2px;
}

.section-link-arrow {
  font-size: 0.875rem;
  line-height: 1;
}
</style>
