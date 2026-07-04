<template>
  <div class="proposals-content">
    <div class="top-bar">
      <h2>Proposals</h2>
      <button class="btn-new" @click="showModal = true">
        + New Proposal
      </button>
    </div>

    <!-- MODALS -->
    <ProponentSubmitProp
      v-model="showModal"
      @save="handleSave"
      @next="goToStep2"
    />

    <ProponentSubmitProp2
      v-if="showModal2"
      :open="showModal2"
      @close="showModal2 = false"
      @openCriteria="openCriteriaModal"
    />

    <ProponentSubmitProp3
      v-model="showCriteria"
      @update:modelValue="(val) => (showCriteria = val)"
    />

    <p class="subtitle">
      Manage and track all your proposal submissions
    </p>

    <!-- FILTERS (UI ONLY, NO DATA YET) -->
    <div class="filters">
      <input
        type="text"
        placeholder="Search proposals by title or category..."
      />
      <select>
        <option>All Status</option>
      </select>
      <button class="btn-filter">Filters</button>
    </div>

    <!-- LOADING -->
    <p v-if="loading">Loading proposals...</p>

    <!-- ERROR -->
    <p v-if="error" class="error-text">
      {{ error }}
    </p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && proposals.length === 0">
      No proposals found.
    </p>

    <!-- TABLE -->
    <table
      v-if="proposals.length > 0"
      class="proposals-table"
    >
      <thead>
        <tr>
          <th>Proposal ID</th>
          <th>Title</th>
          <th>Category</th>
          <th>Status</th>
          <th>Submitted</th>
          <th>Reviewer</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        <tr
          v-for="proposal in proposals"
          :key="proposal.id"
        >
          <td>{{ proposal.proposal_id }}</td>
          <td>{{ proposal.title }}</td>
          <td>{{ proposal.category }}</td>
          <td>
            <span class="status" :class="proposal.status">
              {{ proposal.status }}
            </span>
          </td>
          <td>{{ proposal.submitted_at }}</td>
          <td>{{ proposal.reviewer }}</td>
          <td>
            <button
              class="btn-view"
              @click="viewProposal(proposal.id)"
            >
              View
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

import ProponentSubmitProp from './ProponentSubmitProp.vue'
import ProponentSubmitProp2 from './ProponentSubmitProp2.vue'
import ProponentSubmitProp3 from './ProponentSubmitProp3.vue'

const router = useRouter()

/**
 * STATE (NO FAKE DATA)
 */
const proposals = ref([])
const loading = ref(false)
const error = ref(null)

const showModal = ref(false)
const showModal2 = ref(false)
const showCriteria = ref(false)

/**
 * FETCH FROM BACKEND
 * Expected: GET /api/proposals
 */
const fetchProposals = async () => {
  loading.value = true
  error.value = null

  try {
    const res = await axios.get('/api/proposals')

    proposals.value = Array.isArray(res.data)
      ? res.data
      : res.data.data || []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals'
    proposals.value = []
  } finally {
    loading.value = false
  }
}

/**
 * NAVIGATION
 */
const viewProposal = (id) => {
  router.push({
    name: 'ProponentDetiailedProp',
    query: { id }
  })
}

/**
 * MODAL FLOW
 */
const handleSave = (formData) => {
  console.log('Saved:', formData)
  showModal.value = false
}

const goToStep2 = () => {
  showModal.value = false
  showModal2.value = true
}

const openCriteriaModal = () => {
  showModal2.value = false
  showCriteria.value = true
}

/**
 * INIT
 */
onMounted(() => {
  fetchProposals()
})
</script>

<style scoped>
.proposals-content {
  padding: 24px;
  background: #ffffff;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 16px;
}

.btn-new {
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filters input,
.filters select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.btn-filter {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 8px 14px;
  border-radius: 6px;
}

.proposals-table td:first-child {
  font-family: monospace;
  font-weight: 600;
  color: #4b3f72;
}

.proposals-table th {
  text-align: left;
  padding: 12px;
  background: #4b3f72;
  color: #ffffff;
}

.proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.pending {
  background: #fef3c7;
  color: #92400e;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.draft {
  background: #e5e7eb;
  color: #374151;
}

.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.btn-view {
  background: #60a5fa;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
}
</style>


<style scoped>
.proposals-content {
  padding: 24px;
  background: #ffffff;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 16px;
}

.btn-new {
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-weight: 600;
}

.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.filters input,
.filters select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
}

.btn-filter {
  border: 1px solid #e5e7eb;
  background: #fff;
  padding: 8px 14px;
  border-radius: 6px;
}

.proposals-table td:first-child {
  font-family: monospace;
  font-weight: 600;
  color: #4b3f72;
}

.proposals-table th {
  text-align: left;
  padding: 12px;
  background: #4b3f72;
  color: #ffffff;
}

.proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #e5e7eb;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.pending {
  background: #fef3c7;
  color: #92400e;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.draft {
  background: #e5e7eb;
  color: #374151;
}

.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.btn-view {
  background: #60a5fa;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
}
</style>
