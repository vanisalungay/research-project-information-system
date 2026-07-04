<template>
  <div class="assigned-wrapper">
    <h2 class="page-title">Assigned Proposals for Review</h2>
    <p class="page-subtitle">Review and evaluate proposals assigned by OVCRIGE</p>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <input type="text" v-model="search" placeholder="Search by title or proponent..." class="search-bar" />
      <select v-model="statusFilter" class="status-select">
        <option value="ALL">All Status</option>
        <option value="UNDER_REVIEW">Under Review</option>
        <option value="REC_APPROVED">Approved</option>
        <option value="REC_REJECTED">Rejected</option>
      </select>
    </div>

    <!-- Loading -->
    <p v-if="loading" style="padding:20px;">Loading proposals...</p>
    <p v-else-if="error" style="padding:20px;color:#dc2626;">{{ error }}</p>

    <!-- TABLE -->
    <table v-else class="proposal-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Proponent</th>
          <th>Date Submitted</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>
      <tbody v-if="filteredProposals.length === 0">
        <tr>
          <td colspan="5" style="text-align:center">No proposals found.</td>
        </tr>
      </tbody>
      <tbody v-else>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>
            <strong>{{ proposal.projectTitle || proposal.title }}</strong>
            <p class="subtext">{{ proposal.programTitle || 'N/A' }}</p>
          </td>
          <td>{{ proposal.projectLeader || proposal.proponent?.name || 'Unknown' }}</td>
          <td>{{ proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>
          <td>
            <span class="status-pill">{{ proposal.status }}</span>
          </td>
          <td>
            <button v-if="proposal.status === 'UNDER_REVIEW'" class="review-btn" @click="goToReview(proposal.id)">Review</button>
            <button v-else class="view-btn" @click="goToView(proposal.id)">View</button>
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

const router = useRouter()
const proposals = ref([])
const loading = ref(true)
const error = ref(null)
const search = ref('')
const statusFilter = ref('ALL')

const loadProposals = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await api.get('/api/proposals?statusIn=UNDER_REVIEW&statusIn=REC_APPROVED&statusIn=REC_REJECTED&statusIn=REC_REVISION')
    proposals.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals.'
    proposals.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadProposals)

const filteredProposals = computed(() => {
  return proposals.value.filter(p => {
    if (statusFilter.value !== 'ALL' && p.status !== statusFilter.value) return false
    if (search.value.trim() !== '') {
      const keyword = search.value.toLowerCase()
      return (p.projectTitle || p.title || '').toLowerCase().includes(keyword) ||
             (p.projectLeader || p.proponent?.name || '').toLowerCase().includes(keyword)
    }
    return true
  })
})

const goToReview = (id) => {
  router.push({ name: 'RecReviewForm', params: { id } })
}

const goToView = (id) => {
  router.push({ name: 'RecMeetingDetails', params: { id } })
}
</script>

<style>
.assigned-wrapper {
  padding: 20px;
}
.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}
.page-subtitle {
  margin: 4px 0 18px;
  opacity: 0.7;
}
.filter-bar {
  background: white;
  padding: 15px;
  border-radius: 10px;
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
}
.search-bar {
  flex: 1;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.status-select {
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.proposal-table {
  width: 100%;
  border-collapse: collapse;
  background: white;
  border-radius: 12px;
  overflow: hidden;
}
.proposal-table thead {
  background: #2f2a66;
  color: white;
}
.proposal-table th,
.proposal-table td {
  padding: 15px;
  border-bottom: 1px solid #eee;
}
.subtext {
  font-size: 12px;
  opacity: 0.7;
}
.status-pill {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  background: #ffe9b3;
  color: #c78a00;
}
.review-btn,
.view-btn {
  padding: 7px 14px;
  background: #5ab0ff;
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
</style>