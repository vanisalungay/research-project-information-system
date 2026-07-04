<template>
  <div class="endorsed-page">
    <h2>Endorsed Proposals</h2>
    <p class="subtitle">View all proposals approved by OVCRIGE</p>
    <input
      class="search"
      v-model="search"
      placeholder="Search endorsed proposals..."
    />

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>Project Title</th>
            <th>Program</th>
            <th>Project Leader</th>
            <th>Status</th>
            <th>Date Forwarded</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>

<tr
v-for="proposal in filteredProposals"
:key="proposal.id"
>

<td>{{ proposal.projectTitle }}</td>

<td>{{ proposal.programTitle }}</td>

<td>{{ proposal.projectLeader }}</td>

<td>

<span class="status">
{{ proposal.status }}
</span>

</td>

<td>

{{ proposal.updatedAt?.substring(0,10) }}

</td>

<td>

<button
class="view"
@click="goToReview(proposal.id)"
>

View

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

const loadEndorsedProposals = async () => {
  try {
    const response = await axios.get(
      'http://localhost:8081/api/proposals',
      {
        params: {
          status: 'UNDER_REVIEW'
        }
      }
    )

    proposals.value = response.data
  } catch (error) {
    console.error(error)
  }
}

onMounted(loadEndorsedProposals)

const filteredProposals = computed(() => {
  return proposals.value.filter((proposal) => {
    const keyword = search.value.toLowerCase()

    return (
      proposal.projectTitle?.toLowerCase().includes(keyword) ||
      proposal.programTitle?.toLowerCase().includes(keyword) ||
      proposal.projectLeader?.toLowerCase().includes(keyword)
    )
  })
})

const goToReview = (id) => {
  router.push(`/review-prop/${id}`)
}
</script>

<style>
.endorsed-page {
  padding: 25px;
}

.subtitle {
  color: #777;
  margin-bottom: 15px;
}

.search {
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ddd;
  margin-bottom: 15px;
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
  background: #3c3b6e;
  color: white;
  padding: 10px;
  text-align: left;
}

td {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.score {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status {
  background: #dff3ff;
  color: #0067b8;
  padding: 5px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.view {
  background: #6cb2eb;
  border: none;
  padding: 6px 14px;
  color: white;
  border-radius: 20px;
  cursor: pointer;
}
</style>
