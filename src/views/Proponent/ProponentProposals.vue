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
      @goToStep="handleGoToStep"
      @update:modelValue="refreshOnClose"
    />

    <ProponentSubmitProp2
      v-if="showModal2"
      :open="showModal2"
      @close="showModal2 = false; refreshOnClose()"
      @openPrevious="goBackToStep1"
      @openCriteria="openCriteriaModal"
      @submitProposal="goToStep3"
      @goToStep="handleGoToStep"
    />

    <ProponentSubmitProp3
      v-model="showCriteria"
      :proposalData="proposalData"
      @back="goBackToStep2"
      @savedraft="handleSaveDraft"
      @goToStep="handleGoToStep"
      @update:modelValue="refreshOnClose"
    />

    <p class="subtitle">
      Manage and track all your proposal submissions
    </p>

    <!-- FILTERS -->
    <div class="filters">
      <input type="text" v-model="search" placeholder="Search proposals by title or category..." />
      <select v-model="statusFilter">
        <option value="">All Status</option>
        <option value="DRAFT">Draft</option>
        <option value="SUBMITTED">Submitted</option>
        <option value="UNDER_REVIEW">Under Review</option>
        <option value="APPROVED">Approved</option>
        <option value="REJECTED">Rejected</option>
      </select>
    </div>

    <!-- LOADING -->
    <p v-if="loading">Loading proposals...</p>

    <!-- ERROR -->
    <p v-if="error" class="error-text">{{ error }}</p>

    <!-- EMPTY STATE -->
    <p v-if="!loading && proposals.length === 0">No proposals found.</p>

    <!-- TABLE -->
    <table v-if="proposals.length > 0" class="proposals-table">
      <thead>
        <tr>
          <th>Proposal ID</th>
          <th>Title</th>
          <th>Status</th>
          <th>Submitted</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>{{ proposal.id }}</td>
          <td>{{ proposal.projectTitle || proposal.title }}</td>
          <td>
            <span class="status" :class="(proposal.status || '').toLowerCase()">
              {{ proposal.status }}
            </span>
          </td>
          <td>{{ proposal.createdAt?.substring(0,10) || 'N/A' }}</td>
          <td>
            <button class="btn-view" @click="viewProposal(proposal.id)">View</button>
            <button v-if="proposal.status === 'DRAFT'" class="btn-delete" @click="deleteProposal(proposal.id)">Delete</button>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

import ProponentSubmitProp from './ProponentSubmitProp.vue'
import ProponentSubmitProp2 from './ProponentSubmitProp2.vue'
import ProponentSubmitProp3 from './ProponentSubmitProp3.vue'

const router = useRouter()

const proposals = ref([])
const loading = ref(false)
const error = ref(null)
const search = ref('')
const statusFilter = ref('')

const showModal = ref(false)
const showModal2 = ref(false)
const showCriteria = ref(false)
const proposalData = ref({})

const fetchProposals = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/proposals')
    proposals.value = Array.isArray(res.data) ? res.data : res.data.data || []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals'
    proposals.value = []
  } finally {
    loading.value = false
  }
}

onMounted(fetchProposals)

const filteredProposals = computed(() => {
  return proposals.value.filter(p => {
    if (statusFilter.value && p.status !== statusFilter.value) return false
    if (search.value.trim()) {
      const kw = search.value.toLowerCase()
      return (p.projectTitle || p.title || '').toLowerCase().includes(kw)
    }
    return true
  })
})

const viewProposal = (id) => {
  router.push({ name: 'ProponentDetiailedProp', query: { id } })
}

const deleteProposal = async (id) => {
  if (!confirm('Are you sure you want to delete this draft proposal?')) return
  try {
    await api.delete(`/api/proposals/${id}`)
    proposals.value = proposals.value.filter(p => p.id !== id)
    alert('Draft deleted successfully.')
  } catch (err) {
    console.error(err)
    alert('Failed to delete proposal. Please try again.')
  }
}

// ===== MODAL FLOW WITH DATA PASSING =====

const handleSave = (formData) => {
  // Save form data from Step 1
  proposalData.value = { ...formData }
  showModal.value = false
}

const handleGoToStep = (step) => {
  if (step === 1) {
    showModal2.value = false
    showCriteria.value = false
    showModal.value = true
  } else if (step === 2) {
    showModal.value = false
    showCriteria.value = false
    showModal2.value = true
  } else if (step === 3) {
    showModal.value = false
    showModal2.value = false
    showCriteria.value = true
  }
}

const goToStep2 = (formData) => {
  // Save data from Step 1 and open Step 2
  if (formData) proposalData.value = { ...proposalData.value, ...formData }
  showModal.value = false
  showModal2.value = true
}

const goBackToStep1 = () => {
  showModal2.value = false
  showModal.value = true
}

const goToStep3 = () => {
  showModal2.value = false
  showCriteria.value = true
}

const goBackToStep2 = () => {
  showCriteria.value = false
  showModal2.value = true
}

const openCriteriaModal = () => {
  showModal2.value = false
  showCriteria.value = true
}

const handleSaveDraft = () => {
  showCriteria.value = false
  fetchProposals() // Refresh the list
}

// Watch for any modal to close and refresh the table
const refreshOnClose = () => {
  fetchProposals()
}
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
  cursor: pointer;
}
.filters {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.filters input, .filters select {
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
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
.pending { background: #fef3c7; color: #92400e; }
.approved { background: #dcfce7; color: #166534; }
.revision { background: #e0e7ff; color: #3730a3; }
.submitted { background: #f3e8ff; color: #6b21a8; }
.draft { background: #e5e7eb; color: #374151; }
.rejected { background: #fee2e2; color: #991b1b; }
.btn-view {
  background: #60a5fa;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-right: 6px;
}
.btn-delete {
  background: #ef4444;
  border: none;
  color: #ffffff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.btn-delete:hover {
  background: #dc2626;
}
.error-text { color: #dc2626; }
</style>