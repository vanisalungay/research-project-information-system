<template>
  <div class="page">
    <!-- Header -->
    <h2>Submitted Proposals</h2>
    <p class="subtitle">Review and manage all proposals forwarded to RII</p>

    <!-- Controls -->
    <div class="controls">
      <input
        type="text"
        v-model="search"
        placeholder="Search proposals by title or category..."
        class="search"
      />

      <select v-model="selectedStatus" class="status-select">
        <option value="">All Status</option>
        <option value="Pending">Pending</option>
        <option value="Revision">Revision</option>
        <option value="Endorsed">Endorsed</option>
        <option value="Rejected">Rejected</option>
      </select>

      <button class="filter-btn" @click="resetFilters">Filters</button>
    </div>

    <!-- Table -->
    <table class="proposals-table">
      <thead>
        <tr>
          <th>Proposal Title</th>
          <th>Proponent</th>
          <th>Date Submitted</th>
          <th>Budget</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>
            <strong>{{ proposal.title }}</strong>
            <div class="subtext">{{ proposal.program }}</div>
          </td>
          <td>{{ proposal.proponent }}</td>
          <td>{{ proposal.date }}</td>
          <td>₱{{ proposal.budget.toLocaleString() }}</td>
          <td>
            <span :class="['status', proposal.status.toLowerCase()]">
              {{ proposal.status }}
            </span>
          </td>
          <td>
           <button
              class="review-btn"
              @click="$router.push('fundviewprop')"
            >
              Review
            </button>
          </td>
        </tr>

        <tr v-if="filteredProposals.length === 0">
          <td colspan="6" class="empty">No proposals found.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'SubmittedProposals',

  data() {
    return {
      search: '',
      selectedStatus: '',
      proposals: [
        {
          id: 1,
          title: 'Community Development Program 2024',
          program: 'Kalikasan Program',
          proponent: 'Dr. Alien Shippy',
          date: '2024-12-10',
          budget: 150000,
          status: 'Pending',
        },
        {
          id: 2,
          title: 'Educational Infrastructure Project',
          program: 'Tanglaw Program',
          proponent: 'Dr. Cat Moon',
          date: '2024-12-05',
          budget: 150000,
          status: 'Endorsed',
        },
      
        {
          id: 3,
          title: 'Healthcare Facility Upgrade',
          program: 'Kalikasan Program',
          proponent: 'Dr. Meow Chan',
          date: '2024-12-05',
          budget: 220000,
          status: 'Pending',
        },
        {
          id: 4,
          title: 'Technology Innovation Initiative',
          program: 'Tanglaw Program',
          proponent: 'Dr. Blair Gwen',
          date: '2024-12-03',
          budget: 220000,
          status: 'Endorsed',
        },
      ],
    }
  },

  computed: {
    filteredProposals() {
      return this.proposals.filter((p) => {
        const matchesSearch =
          p.title.toLowerCase().includes(this.search.toLowerCase()) ||
          p.program.toLowerCase().includes(this.search.toLowerCase())

        const matchesStatus =
          this.selectedStatus === '' || p.status === this.selectedStatus

        return matchesSearch && matchesStatus
      })
    },
  },

  methods: {
    reviewProposal(proposal) {
      alert(`Reviewing:\n\n${proposal.title}\nProponent: ${proposal.proponent}`)
    },

    resetFilters() {
      this.search = ''
      this.selectedStatus = ''
    },
  },
}
</script>

<style scoped>
.page {
  padding: 32px;
  font-family: Arial, sans-serif;
  width: 100%;
  max-width: 100%;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 24px;
  width: 100%;
}

.search {
  flex: 1;
  min-width: 280px;
  padding: 10px 14px;
  border-radius: 6px;
  border: 1px solid #ccc;
}

.status-select,
.filter-btn {
  padding: 10px 22px;
  border-radius: 20px;
  border: 1px solid #ccc;
  background: white;
  min-width: 150px;
}

.proposals-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.proposals-table th,
.proposals-table td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  word-wrap: break-word;
}

.proposals-table th:nth-child(4),
.proposals-table td:nth-child(4) {
  text-align: right;
}

.proposals-table th:nth-child(5),
.proposals-table td:nth-child(5),
.proposals-table th:nth-child(6),
.proposals-table td:nth-child(6) {
  text-align: center;
}

.proposals-table th:nth-child(7),
.proposals-table td:nth-child(7) {
  text-align: center;
}

.subtext {
  font-size: 12px;
  color: #777;
}

.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.pending {
  background-color: #ffe2a8;
  color: #8a5b00;
}

.revision {
  background-color: #d6e4ff;
  color: #2a4dbf;
}

.endorsed {
  background-color: #d9f5e5;
  color: #1e7f4f;
}

.rejected {
  background-color: #ffd6d6;
  color: #a10000;
}

.review-btn {
  background-color: #5da9ff;
  color: white;
  border: none;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #777;
}
</style>
