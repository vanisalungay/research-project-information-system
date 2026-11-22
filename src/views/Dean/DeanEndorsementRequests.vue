<template>
  <div class="end-page end-page--fluid end-page--dean">
    <div class="end-page-header">
      <h2>Endorsement Requests</h2>
      <p class="end-subtitle">
        The endorsement requests inside your authorized scope. You may only decide the
        requests of your own college.
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading the requests…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <button class="end-btn" @click="load">Retry</button>
      </div>
    </div>

    <template v-else>
      <div class="end-toolbar">
        <select v-model="statusFilter" class="end-select" @change="load">
          <option v-for="option in filters" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <span class="end-muted end-toolbar-count">{{ requests.length }} request(s)</span>
      </div>

      <div v-if="requests.length === 0" class="end-card is-empty">
        <p>There are no endorsement requests{{ statusFilter ? ' with this status' : '' }}.</p>
      </div>

      <div v-else class="end-card end-table-scroll">
        <table class="end-table">
          <thead>
            <tr>
              <th>Reference</th>
              <th>Proponent</th>
              <th>Project title</th>
              <th>Call for Proposal</th>
              <th>College</th>
              <th>Submitted</th>
              <th>Status</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="request in pagedRequests" :key="request.id">
              <td>{{ request.referenceNumber }}</td>
              <td>
                {{ request.proponentName }}
                <div class="end-muted">{{ request.proponentEmail }}</div>
              </td>
              <td>
                {{ request.projectTitle }}
                <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
              </td>
              <td>
                {{ request.proposalAnnouncementName || '—' }}
                <div class="end-muted">{{ fundingProgramLabel(request) }}</div>
              </td>
              <td>{{ request.college }}</td>
              <td>{{ formatEndorsementTimestamp(request.createdAt) }}</td>
              <td>
                <span class="end-badge" :class="endorsementStatusTone(request.status)">
                  {{ endorsementStatusLabel(request.status) }}
                </span>
              </td>
              <td>
                <router-link class="end-btn is-link" :to="`/dean-requests/${request.id}`">
                  {{ isPending(request.status) ? 'Review' : 'View' }}
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="requests.length" class="end-pagination">
        <span class="end-muted">{{ rangeLabel }}</span>
        <div class="end-pager">
          <button
            type="button"
            class="end-page-btn"
            :disabled="page <= 1"
            @click="goToPage(page - 1)"
          >
            Previous
          </button>
          <span class="end-muted">Page {{ page }} of {{ totalPages }}</span>
          <button
            type="button"
            class="end-page-btn"
            :disabled="page >= totalPages"
            @click="goToPage(page + 1)"
          >
            Next
          </button>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import '@/assets/endorsement.css'
import {
  ENDORSEMENT_FILTERS,
  endorsementStatusLabel,
  endorsementStatusTone,
  fetchEndorsementRequests,
  formatEndorsementTimestamp,
  fundingProgramLabel,
  isPending,
} from '@/utils/endorsement.js'

const filters = ENDORSEMENT_FILTERS
const requests = ref([])
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')

// The list endpoint returns every request in the Dean's scope, so the table pages it
// locally. The page size matches the other administrator/end-user tables.
const PAGE_SIZE = 10
const page = ref(1)
const totalPages = computed(() =>
  Math.max(1, Math.ceil(requests.value.length / PAGE_SIZE)),
)
const pagedRequests = computed(() => {
  const start = (page.value - 1) * PAGE_SIZE
  return requests.value.slice(start, start + PAGE_SIZE)
})
const rangeLabel = computed(() => {
  if (!requests.value.length) return 'No requests'
  const first = (page.value - 1) * PAGE_SIZE + 1
  const last = Math.min(page.value * PAGE_SIZE, requests.value.length)
  return `Showing ${first}–${last} of ${requests.value.length}`
})

function goToPage(next) {
  if (next < 1 || next > totalPages.value) return
  page.value = next
}

async function load() {
  loading.value = true
  error.value = ''
  // A filter change starts at the first page of the new result set.
  page.value = 1
  try {
    requests.value = await fetchEndorsementRequests({ status: statusFilter.value })
  } catch (err) {
    error.value =
      err?.response?.data?.error || 'The endorsement requests could not be loaded.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
