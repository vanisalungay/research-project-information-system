<template>
  <div class="assigned-wrapper">
    <h2 class="page-title">Submitted Proposals</h2>
    <p class="page-subtitle">Review and manage all proposal submissions</p>

    <!-- FILTER BAR -->
    <div class="filter-bar">
      <input
        type="text"
        placeholder="Search by title, proponent, or category..."
        class="search-bar"
      />

      <select class="status-select">
        <option>All Status</option>
        <option>Pending</option>
        <option>Reviewed</option>
        <option>Approved</option>
      </select>

      <button class="filter-btn">More Filters</button>
    </div>

    <!-- TABLE -->
    <table class="proposal-table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Proponent</th>
          <th>Date Submitted</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody v-if="loading">
        <tr>
          <td colspan="5" style="text-align:center">
            Loading...
          </td>
        </tr>
      </tbody>

      <tbody v-else-if="proposals.length === 0">
        <tr>
          <td colspan="5" style="text-align:center">
            No proposals found.
          </td>
        </tr>
      </tbody>

      <tbody v-else>
        <tr v-for="proposal in proposals" :key="proposal.id">
          <td>
            <strong>{{ proposal.title || proposal.projectTitle }}</strong>
            <p class="subtext">{{ proposal.category || 'N/A' }}</p>
          </td>

          <td>{{ proposal.proponent?.name || 'Unknown' }}</td>
          <td>{{ proposal.dateSubmitted || proposal.createdAt?.substring(0, 10) || 'N/A' }}</td>

          <td>
            <span class="status-pill pending">{{ proposal.status || 'Pending' }}</span>
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()
const proposals = ref([])
const loading = ref(true)

const loadProposals = async () => {
  try {
    const response = await axios.get('http://localhost:8081/api/proposals')
    proposals.value = response.data
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProposals()
})

const goToReview = (id) => {
  router.push(`/review-form/${id || ''}`)
}

const goToView = (id) => {
  router.push(`/meeting-details/${id || ''}`)
}
</script>

<style>
.assigned-wrapper {
  padding: 20px;
}

/* Titles */
.page-title {
  margin: 0;
  font-size: 26px;
  font-weight: 700;
}

.page-subtitle {
  margin: 4px 0 18px;
  opacity: 0.7;
}

/* FILTER BAR */
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

.filter-btn {
  padding: 10px 16px;
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #ccc;
  cursor: pointer;
}

/* TABLE */
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

/* Status Pills */
.status-pill {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.pending {
  background: #ffe9b3;
  color: #c78a00;
}

.status {
  padding: 6px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.complete {
  background: #ffe9b3;
  color: #2f2a66;
}

/* Review and view Button */
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
