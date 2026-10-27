<template>
  <div class="end-page">
    <div class="end-page-header">
      <h2>My Endorsements</h2>
      <p class="end-subtitle">
        Every Request for Endorsement you have raised, with its current status and the
        official letter once your Dean has endorsed it.
      </p>
    </div>

    <div v-if="loading" class="end-card is-empty">
      <p>Loading your endorsement requests…</p>
    </div>

    <div v-else-if="error" class="end-card is-error">
      <p>{{ error }}</p>
      <div class="end-actions">
        <button class="end-btn" @click="load">Retry</button>
      </div>
    </div>

    <template v-else>
      <div class="end-toolbar">
        <router-link class="end-btn is-primary" to="/request-endorsement">
          + New Request for Endorsement
        </router-link>
        <select v-model="statusFilter" class="end-select">
          <option v-for="option in filters" :key="option.value" :value="option.value">
            {{ option.label }}
          </option>
        </select>
        <span class="end-muted">{{ visibleRequests.length }} request(s)</span>
      </div>

      <div v-if="visibleRequests.length === 0" class="end-card is-empty">
        <p>
          You have no endorsement requests{{ statusFilter ? ' with this status' : '' }} yet.
          Raise one before you start a new proposal.
        </p>
      </div>

      <table v-else class="end-table end-card">
        <thead>
          <tr>
            <th>Reference</th>
            <th>Proposal title</th>
            <th>Call for Proposal</th>
            <th>College</th>
            <th>Submitted</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="request in visibleRequests" :key="request.id">
            <td>{{ request.referenceNumber }}</td>
            <td>{{ request.proposalTitle }}</td>
            <td>{{ request.proposalAnnouncementName || '—' }}</td>
            <td>{{ request.college }}</td>
            <td>{{ formatEndorsementTimestamp(request.createdAt) }}</td>
            <td>
              <span class="end-badge" :class="endorsementStatusTone(request.status)">
                {{ endorsementStatusLabel(request.status) }}
              </span>
            </td>
            <td>
              <router-link class="end-btn is-link" :to="`/endorsements/${request.id}`">
                View
              </router-link>
            </td>
          </tr>
        </tbody>
      </table>
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
} from '@/utils/endorsement.js'

const filters = ENDORSEMENT_FILTERS
const requests = ref([])
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')

const visibleRequests = computed(() =>
  statusFilter.value
    ? requests.value.filter((request) => request.status === statusFilter.value)
    : requests.value,
)

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
