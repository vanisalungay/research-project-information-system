<template>
  <div class="end-page end-page--fluid end-page--dean">
    <div class="end-page-header">
      <h2>Welcome, {{ displayName }}!</h2>
      <p class="end-subtitle">
        Review and decide the endorsement requests of your college. Everything waiting for
        your decision, and everything you have decided recently, is summarised below.
      </p>
    </div>

    <div class="end-stats">
      <div class="end-stat is-pending">
        <span class="end-stat-label">Pending requests</span>
        <span class="end-stat-value">{{ pending.length }}</span>
      </div>
      <div class="end-stat is-approved">
        <span class="end-stat-label">Approved</span>
        <span class="end-stat-value">{{ approved.length }}</span>
      </div>
      <div class="end-stat is-rejected">
        <span class="end-stat-label">Rejected</span>
        <span class="end-stat-value">{{ rejected.length }}</span>
      </div>
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
      <div class="end-card">
        <div class="end-toolbar">
          <h3 class="end-section-title">Pending endorsement requests</h3>
          <router-link class="end-btn is-link" to="/dean-requests">See all requests</router-link>
        </div>

        <div v-if="pending.length === 0" class="end-muted">
          Nothing is waiting for your endorsement right now.
        </div>

        <div v-else class="end-table-scroll">
          <table class="end-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Proponent</th>
                <th>Project title</th>
                <th>College</th>
                <th>Submitted</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in pending" :key="request.id">
                <td>{{ request.referenceNumber }}</td>
                <td>{{ request.proponentName }}</td>
                <td>
                  {{ request.projectTitle }}
                  <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
                </td>
                <td>{{ request.college }}</td>
                <td>{{ formatEndorsementTimestamp(request.createdAt) }}</td>
                <td>
                  <router-link class="end-btn is-link" :to="`/dean-requests/${request.id}`">
                    Review
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <div class="end-card">
        <h3 class="end-section-title">Recently decided</h3>
        <div v-if="decided.length === 0" class="end-muted">No decisions recorded yet.</div>
        <div v-else class="end-table-scroll">
          <table class="end-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Project title</th>
                <th>Decided</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="request in decided" :key="request.id">
                <td>{{ request.referenceNumber }}</td>
                <td>
                  {{ request.projectTitle }}
                  <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
                </td>
                <td>{{ formatEndorsementTimestamp(request.decidedAt) }}</td>
                <td>
                  <span class="end-badge" :class="endorsementStatusTone(request.status)">
                    {{ endorsementStatusLabel(request.status) }}
                  </span>
                </td>
                <td>
                  <router-link class="end-btn is-link" :to="`/dean-requests/${request.id}`">
                    View
                  </router-link>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
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
