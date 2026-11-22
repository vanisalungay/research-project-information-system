<template>
  <div class="calls-page">
    <BackButton label="Back to Dashboard" to="/proponent-dashboard" />

    <header class="calls-header">
      <div>
        <h1>Call for Proposals</h1>
        <p>
          Every published call you may view. Open calls accept submissions; scheduled
          and closed calls are shown for reference.
        </p>
      </div>
    </header>

    <div class="calls-toolbar">
      <input
        v-model="search"
        type="search"
        class="calls-search"
        placeholder="Search calls..."
        aria-label="Search calls for proposals"
      />
      <select v-model="statusFilter" class="calls-filter" aria-label="Filter by status">
        <option value="all">All statuses</option>
        <option value="Open">Open</option>
        <option value="Scheduled">Scheduled</option>
        <option value="Closed">Closed</option>
      </select>
    </div>

    <div v-if="loading" class="calls-state">
      <span class="calls-spinner" aria-hidden="true"></span>
      <p>Loading calls for proposals...</p>
    </div>

    <div v-else-if="error" class="calls-state calls-state--error">
      <p>{{ error }}</p>
      <button type="button" class="calls-retry" @click="loadCalls">Try again</button>
    </div>

    <p v-else-if="filteredCalls.length === 0" class="calls-state">
      No calls match your filters.
    </p>

    <div v-else class="calls-grid">
      <article v-for="call in filteredCalls" :key="call.id" class="call-card">
        <header class="call-card-head">
          <span class="call-status" :class="statusTone(statusOf(call))">
            <span class="call-status-dot" aria-hidden="true"></span>
            {{ statusOf(call) }}
          </span>
          <span class="call-program">{{ fundingProgramLabel(call) }}</span>
        </header>

        <h2 class="call-card-title">{{ call.name }}</h2>

        <dl class="call-meta">
          <div>
            <dt>Opening date</dt>
            <dd>{{ callOpeningLabel(call) }}</dd>
          </div>
          <div>
            <dt>Deadline</dt>
            <dd>{{ callDeadlineLabel(call) }}</dd>
          </div>
          <div>
            <dt>Submission period</dt>
            <dd>{{ callWindowLabel(call) }}</dd>
          </div>
        </dl>

        <p v-if="call.additionalAnnouncement" class="call-description">
          {{ call.additionalAnnouncement }}
        </p>

        <div class="call-card-actions">
          <router-link
            class="end-btn is-view"
            :to="`/proponent-announcement/${call.id}`"
          >
            View Call Details
          </router-link>
        </div>
      </article>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import api from '@/utils/api'
import BackButton from '@/components/BackButton.vue'
import { announcementStatus } from '@/utils/announcementStatus'
import { statusTone } from '@/utils/statusTone.js'
import {
  PROPONENT_CALLS_ENDPOINT,
  callDeadlineLabel,
  callOpeningLabel,
  callWindowLabel,
} from '@/utils/callForProposal'
import { fundingProgramLabel } from '@/utils/endorsement.js'

const calls = ref([])
const loading = ref(true)
const error = ref('')
const search = ref('')
const statusFilter = ref('all')

const statusOf = (call) => announcementStatus(call)

const filteredCalls = computed(() => {
  const keyword = search.value.trim().toLowerCase()
  return calls.value.filter((call) => {
    const status = statusOf(call)
    if (statusFilter.value !== 'all' && status !== statusFilter.value) return false
    if (!keyword) return true
    return `${call.name || ''} ${call.additionalAnnouncement || ''}`
      .toLowerCase()
      .includes(keyword)
  })
})

async function loadCalls() {
  loading.value = true
  error.value = ''
  try {
    const response = await api.get(PROPONENT_CALLS_ENDPOINT)
    calls.value = Array.isArray(response.data) ? response.data : []
  } catch (requestError) {
    console.error('Failed to load the calls for proposals:', requestError?.response?.status)
    error.value = 'Unable to load the calls for proposals. Please try again.'
    calls.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadCalls)
</script>

<style scoped>
.calls-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px;
  background: #f4f5fa;
  min-height: 100vh;
}

.calls-header {
  margin: 16px 0 18px;
}

.calls-header h1 {
  margin: 0 0 4px;
  color: #1c1a3c;
  font-size: 1.5rem;
  font-weight: 800;
}

.calls-header p {
  margin: 0;
  color: #64748b;
  font-size: 0.875rem;
  line-height: 1.5;
}

.calls-toolbar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.calls-search,
.calls-filter {
  padding: 10px 12px;
  border: 1px solid #e1e4ef;
  border-radius: 9px;
  background: #ffffff;
  color: #1e293b;
  font-family: inherit;
  font-size: 0.8125rem;
}

.calls-search {
  flex: 1 1 260px;
}

.calls-filter {
  min-width: 160px;
}

.calls-search:focus,
.calls-filter:focus {
  outline: none;
  border-color: #315bff;
  box-shadow: 0 0 0 3px rgba(49, 91, 255, 0.12);
}

.calls-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 14px;
}

.call-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  border: 1px solid #e1e4ef;
  border-radius: 13px;
  background: #ffffff;
}

.call-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  flex-wrap: wrap;
}

.call-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 0.6875rem;
  font-weight: 700;
}

.call-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: currentColor;
}

.call-program {
  color: #64748b;
  font-size: 0.6875rem;
  font-weight: 600;
}

.call-card-title {
  margin: 0;
  color: #1c1a3c;
  font-size: 1rem;
  font-weight: 700;
  line-height: 1.4;
}

.call-meta {
  display: grid;
  gap: 5px;
  margin: 0;
}

.call-meta div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.call-meta dt {
  color: #6b7190;
  font-size: 0.6875rem;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.call-meta dd {
  margin: 0;
  color: #1e293b;
  font-size: 0.75rem;
  text-align: right;
}

.call-description {
  margin: 0;
  color: #475569;
  font-size: 0.75rem;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.call-card-actions {
  margin-top: auto;
  display: flex;
  justify-content: flex-end;
}

.calls-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 48px 20px;
  border: 1px solid #e1e4ef;
  border-radius: 13px;
  background: #ffffff;
  color: #64748b;
  font-size: 0.875rem;
  text-align: center;
}

.calls-state--error {
  color: #b91c1c;
}

.calls-spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #e2e8f0;
  border-top-color: #315bff;
  border-radius: 50%;
  animation: calls-spin 0.8s linear infinite;
}

@keyframes calls-spin {
  to {
    transform: rotate(360deg);
  }
}

.calls-retry {
  padding: 8px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #ffffff;
  color: #334155;
  font: inherit;
  font-weight: 600;
  cursor: pointer;
}

@media (max-width: 640px) {
  .calls-page {
    padding: 16px;
  }

  .calls-grid {
    grid-template-columns: 1fr;
  }
}
</style>
