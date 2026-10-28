<template>
  <div class="end-page end-page--fluid">
    <div class="end-page-header">
      <h2>My Endorsements</h2>
      <p class="end-subtitle">
        Raise a Request for Endorsement, follow every request you have submitted, and open
        the official letters your Dean has issued — all in one place.
      </p>
    </div>

    <!-- The three parts of the endorsement workflow are sections of this one page instead
         of three sidebar items. The active section is part of the URL (?tab=...), so a
         refresh, a bookmark or a link from another screen opens the same section. -->
    <div class="end-tabs" role="tablist" aria-label="Endorsement sections">
      <button
        v-for="tab in tabs"
        :key="tab.id"
        type="button"
        role="tab"
        class="end-tab"
        :class="{ active: activeTab === tab.id }"
        :aria-selected="activeTab === tab.id ? 'true' : 'false'"
        @click="selectTab(tab.id)"
      >
        {{ tab.label }}
        <span v-if="tab.id === 'requests' && requests.length" class="end-tab-count">
          {{ requests.length }}
        </span>
      </button>
    </div>

    <p class="end-subtitle">{{ activeTabMeta.description }}</p>

    <!-- ===================== MY REQUESTS ===================== -->
    <template v-if="activeTab === 'requests'">
      <div v-if="successMessage" class="end-banner is-success">
        <span>{{ successMessage }}</span>
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
          <button class="end-btn is-primary" type="button" @click="selectTab('new')">
            + New Request for Endorsement
          </button>
          <select v-model="statusFilter" class="end-select">
            <option v-for="option in filters" :key="option.value" :value="option.value">
              {{ option.label }}
            </option>
          </select>
          <span class="end-muted end-toolbar-count">
            {{ visibleRequests.length }} request(s)
          </span>
        </div>

        <div v-if="visibleRequests.length === 0" class="end-card is-empty">
          <p>
            You have no endorsement requests{{ statusFilter ? ' with this status' : '' }} yet.
            Raise one before you start a new proposal.
          </p>
          <div class="end-actions">
            <button class="end-btn is-primary" type="button" @click="selectTab('new')">
              Request Endorsement
            </button>
          </div>
        </div>

        <div v-else class="end-card end-table-scroll">
          <table class="end-table">
            <thead>
              <tr>
                <th>Reference</th>
                <th>Project title</th>
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
                <td>
                  {{ request.projectTitle }}
                  <div v-if="request.programTitle" class="end-muted">{{ request.programTitle }}</div>
                </td>
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
        </div>
      </template>
    </template>

    <!-- ===================== REQUEST ENDORSEMENT ===================== -->
    <!-- The existing form, unchanged: it only reports the result and the Cancel action
         back to this page instead of navigating away. -->
    <ProponentEndorsementRequestForm
      v-else-if="activeTab === 'new'"
      @submitted="onSubmitted"
      @cancel="selectTab('requests')"
    />

    <!-- ===================== ISSUED CERTIFICATIONS ===================== -->
    <!-- The issued endorsement letters, from the existing Certifications endpoint. -->
    <ProponentCertifications v-else @open-requests="selectTab('requests')" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '@/assets/endorsement.css'
import ProponentCertifications from './ProponentCertifications.vue'
import ProponentEndorsementRequestForm from './ProponentEndorsementRequestForm.vue'
import {
  DEFAULT_ENDORSEMENT_TAB,
  ENDORSEMENT_FILTERS,
  ENDORSEMENT_TABS,
  endorsementStatusLabel,
  endorsementStatusTone,
  endorsementTab,
  endorsementTabFromQuery,
  fetchEndorsementRequests,
  formatEndorsementTimestamp,
} from '@/utils/endorsement.js'

const route = useRoute()
const router = useRouter()

const tabs = ENDORSEMENT_TABS
const filters = ENDORSEMENT_FILTERS
const requests = ref([])
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')
const successMessage = ref('')

// The section the page opens on. `?tab=` is read here and watched below, so a link to
// another section of this same page (or the redirect from the old Certifications URL)
// switches the section instead of replacing the page.
const activeTab = ref(endorsementTabFromQuery(route.query.tab))
const activeTabMeta = computed(() => endorsementTab(activeTab.value))

watch(
  () => route.query.tab,
  (value) => {
    activeTab.value = endorsementTabFromQuery(value)
  },
)

/**
 * Moves between the sections of this page. The section is written to the URL so a
 * refresh, a bookmark or a shared link reopens it, while the default section keeps the
 * URL clean. `replace` is used on purpose: these are views of one page, not steps of a
 * flow the Back button should walk through.
 */
function selectTab(id) {
  const tab = endorsementTabFromQuery(id)
  activeTab.value = tab
  const query = { ...route.query }
  if (tab === DEFAULT_ENDORSEMENT_TAB) delete query.tab
  else query.tab = tab
  if (route.query.tab !== query.tab) {
    router.replace({ path: '/endorsements', query })
  }
}

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

/**
 * A request was accepted by the form: the confirmation is shown here, the page returns to
 * the tracking list and the list is reloaded so the new request is already in it.
 */
function onSubmitted(created) {
  successMessage.value = `Request ${created?.referenceNumber ?? ''} was submitted and is now pending your Dean's endorsement.`
  selectTab(DEFAULT_ENDORSEMENT_TAB)
  load()
}

onMounted(load)
</script>
