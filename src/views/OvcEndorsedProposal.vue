<template>
  <div class="endorsed-page">
    <h2>Endorsed Proposals</h2>
    <p class="subtitle"> View all proposals endorsed to the Research Evaluation Committee (REC)</p>    
    <input class="search" placeholder="Search endorsed proposals..." />

    <div class="table-card">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Proponent</th>
            <th>Category</th>
            <th>Endorsed Date</th>
            <th>Budget</th>
            <th>REC Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in proposals" :key="item.title">
            <td>{{ item.title }}</td>
            <td>{{ item.proponent }}</td>
            <td>{{ item.category }}</td>
            <td>{{ item.date }}</td>
            <td>{{ item.budget }}</td>
            <td>{{ item.status }}</td>
            
            <td>
              <button class="view" @click="goToReview">View</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goToReview = () => {
  router.push('/review-prop')
}

const proposals = ref([
  {
    title: 'Educational Infrastructure Project',
    proponent: 'Dr. Cat Moon',
    category: 'Tanglaw Program',
    date: '2024-12-09',
    budget: '₱180,000',
    status: 'Endorsed to REC',
  },
  {
    title: 'Technology Innovation Initiative',
    proponent: 'Dr. Blair Gwen',
    category: 'Tanglaw Program',
    date: '2024-12-03',
    budget: '₱95,000',
    status: 'Under REC Evaluation',
  },
])
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

.score .bar {
  height: 6px;
  background: #2ecc71;
  border-radius: 4px;
  width: 60px;
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
