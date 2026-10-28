<template>
  <div class="end-page">
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
        <span class="end-muted">{{ requests.length }} request(s)</span>
      </div>

      <div v-if="requests.length === 0" class="end-card is-empty">
        <p>There are no endorsement requests{{ statusFilter ? ' with this status' : '' }}.</p>
      </div>

      <table v-else class="end-table end-card">
        <thead>
          <tr>
            <th>Reference</th>
            <th>Proponent</th>
            <th>Proposal title</th>
            <th>Call for Proposal</th>
            <th>College</th>
            <th>Submitted</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in requests" :key="request.id">
            <td>{{ request.referenceNumber }}</td>
            <td>
              {{ request.proponentName }}
              <div class="end-muted">{{ request.proponentEmail }}</div>
            </td>
            <td>{{ request.proposalTitle }}</td>
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
    </template>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
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

async function load() {
  loading.value = true
  error.value = ''
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
