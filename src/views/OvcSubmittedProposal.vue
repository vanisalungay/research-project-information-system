<template>
  <div class="submitted-page">
    <!-- PAGE TITLE -->
    <div class="page-header">
      <h2>Submitted Proposals</h2>
      <p>Review and manage all proposal submissions</p>
    </div>

    <!-- FILTER BAR -->
    <div class="filter-bar">

  <input
    v-model="search"
    type="text"
    placeholder="Search proposal..."
  />

  <select v-model="statusFilter">
    <option value="ALL">All Status</option>
    <option value="ENDORSED">Endorsed</option>
  </select>

</div>

    <!-- TABLE -->
    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Proponent</th>
            <th>Date Submitted</th>
            <th>Status</th>
            <th>Assigned Reviewer</th>
            <th>Actions</th>
          </tr>
        </thead>

        <tbody>

<tr
v-for="proposal in filteredProposals"
:key="proposal.id"
>

<td>
<strong>{{ proposal.projectTitle }}</strong>
<br>
<small>{{ proposal.programTitle }}</small>
</td>

<td>
{{ proposal.projectLeader }}
</td>

<td>
{{ proposal.createdAt?.substring(0,10) || '-' }}
</td>

<td>

<span class="status pending">
{{ proposal.status }}
</span>

</td>

<td>

<button
class="assign-btn"
@click="goToAssignReviewer(proposal.id)"
>

Assign

</button>

</td>

<td>

<button
class="review-btn"
@click="goToReview(proposal.id)"
>

Review

</button>

</td>

</tr>

<tr v-if="filteredProposals.length===0">

<td colspan="6" style="text-align:center">

No endorsed proposals found.

</td>

</tr>

</tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'

const router = useRouter()

const proposals = ref([])
const search = ref('')
const statusFilter = ref('ALL')

const loadProposals = async () => {
  try {
    const res = await axios.get('http://localhost:8081/api/proposals')

    // OVCRIGE only sees proposals endorsed by RPS
    proposals.value = res.data.filter(
      p => p.status === 'ENDORSED'
    )
  } catch (err) {
    console.error(err)
  }
}

onMounted(loadProposals)

const filteredProposals = computed(() => {
  return proposals.value.filter(p => {

    const keyword =
      search.value === '' ||
      p.projectTitle?.toLowerCase().includes(search.value.toLowerCase()) ||
      p.programTitle?.toLowerCase().includes(search.value.toLowerCase()) ||
      p.projectLeader?.toLowerCase().includes(search.value.toLowerCase())

    const status =
      statusFilter.value === 'ALL' ||
      p.status === statusFilter.value

    return keyword && status
  })
})

const goToReview = (proposalId) => {
  router.push(`/review-prop/${proposalId}`)
}

const goToAssignReviewer = (proposalId) => {
  router.push(`/assign-reviewer/${proposalId}`)
}
</script>

<style>
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

/* FILTER BAR */
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

.filter-btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
}

/* TABLE */
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
  text-align: left;
  padding: 12px;
}

td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

small {
  color: #777;
}

/* STATUS */
.status {
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.pending {
  background: #ffe082;
}

.revision {
  background: #90caf9;
}
/* BUTTONS */
.assign-btn {
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid #ccc;
  background: white;
  cursor: pointer;
}

.review-btn {
  padding: 6px 14px;
  border-radius: 6px;
  background: #6cb4ff;
  border: none;
  color: white;
  cursor: pointer;
}
</style>
