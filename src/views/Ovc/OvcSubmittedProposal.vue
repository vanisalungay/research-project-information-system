<template>
  <div class="submitted-page">
    <!-- PAGE TITLE -->
    <div class="page-header">
      <h2>Submitted Proposals</h2>
      <p>Review and manage all proposal submissions</p>
    </div>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <input v-model="search" type="text" placeholder="Search proposal..." />
      <select v-model="statusFilter">
        <option value="ALL">All Status</option>
        <option value="ENDORSED">Endorsed</option>
        <option value="FOR_OC_APPROVAL">For OC Approval</option>
      </select>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="table-card">
      <p style="text-align:center;padding:40px;">Loading proposals...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="table-card">
      <p style="text-align:center;padding:40px;color:#dc2626;">{{ error }}</p>
    </div>

    <!-- TABLE -->
    <div v-else class="table-card">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Proponent</th>
            <th>Date Submitted</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="proposal in filteredProposals" :key="proposal.id">
            <td>
              <strong>{{ proposal.projectTitle }}</strong>
              <br><small>{{ proposal.programTitle }}</small>
            </td>
            <td>{{ proposal.projectLeader }}</td>
            <td>{{ proposal.createdAt?.substring(0,10) || '-' }}</td>
            <td>
              <span class="status pending">{{ proposal.status }}</span>
            </td>
            <td>
              <button class="review-btn" @click="goToReview(proposal.id)">Review</button>
            </td>
          </tr>
          <tr v-if="filteredProposals.length===0">
            <td colspan="5" style="text-align:center">No endorsed proposals found.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()
const proposals = ref([])
const search = ref('')
const statusFilter = ref('ALL')
const loading = ref(false)
const error = ref(null)

const loadProposals = async () => {
  loading.value = true
  error.value = null
  try {
    // OVCRIGE sees proposals that are ENDORSED (from RPS) and FOR_OC_APPROVAL (after REC)
    const res = await api.get('/api/proposals?statusIn=ENDORSED&statusIn=FOR_OC_APPROVAL&statusIn=REC_APPROVED&statusIn=OVC_APPROVED')
    proposals.value = Array.isArray(res.data) ? res.data : []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposals. Ensure the backend server is running.'
    proposals.value = []
  } finally {
    loading.value = false
  }
}
onMounted(loadProposals)

const filteredProposals = computed(() => {
  return proposals.value.filter(p => {
    const keyword = search.value === '' ||
      p.projectTitle?.toLowerCase().includes(search.value.toLowerCase()) ||
      p.programTitle?.toLowerCase().includes(search.value.toLowerCase()) ||
      p.projectLeader?.toLowerCase().includes(search.value.toLowerCase())
    const status = statusFilter.value === 'ALL' || p.status === statusFilter.value
    return keyword && status
  })
})

const goToReview = (proposalId) => {
  router.push({ name: 'OvcReviewProposal', params: { id: proposalId } })
}
</script>

<style scoped>
.submitted-page {
  padding: 25px;
}
.page-header h2 {
  margin: 0;
}
.page-header p {
  color: #666;
  margin-top: 5px;
}
.filter-bar {
  display: flex;
  gap: 12px;
  margin: 20px 0;
}
.filter-bar input {
  flex: 1;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.filter-bar select {
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
}
.table-card {
  background: white;
  border-radius: 12px;
  padding: 15px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th {
  background: #2f2b57;
  color: white;
  padding: 12px;
  text-align: left;
}
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}
small {
  color: #777;
}
.status {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}
.pending {
  background: #FEF3C7;
  color: #92400E;
}
.review-btn {
  padding: 7px 16px;
  border: none;
  border-radius: 6px;
  background: #3B82F6;
  color: white;
  cursor: pointer;
  transition: .2s;
}
.review-btn:hover {
  background: #2563EB;
}
</style>