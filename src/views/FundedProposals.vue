krisha, [29/12/2025 2:29 pm]
<template>
  <div class="page">
    <!-- Header -->
    <h2>Funded Proposals</h2>
    <p class="subtitle">Manage all approved and funded research projects</p>

    <!-- SUMMARY -->
    <div class="stats">
      <div class="card">
        <p>Total Funded</p>
        <h3>2</h3>
      </div>

      <div class="card">
        <p>Funds Released</p>
        <h3>2</h3>
      </div>

      <div class="card">
        <p>Pending Release</p>
        <h3>1</h3>
      </div>

      <div class="card highlight">
        <p>Total Released</p>
        <h3>₱ {{ totalReleased }}K</h3>
      </div>
    </div>

    <!-- Search -->
    <input type="text" v-model="search" placeholder="Search funded proposals..." class="search" />

    <!-- Table -->
    <table class="table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Proponent</th>
          <th>Total Budget</th>
          <th>Released</th>
          <th>Release Date</th>
          <th>Status</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>{{ proposal.title }}</td>
          <td>{{ proposal.proponent }}</td>
          <td>₱{{ proposal.totalBudget.toLocaleString() }}</td>

          <td :class="{ green: proposal.released > 0 }">
            ₱{{ proposal.released.toLocaleString() }}
          </td>

          <td>{{ proposal.releaseDate || '-' }}</td>

          <td>
            <span v-if="proposal.released > 0" class="badge success"> Fund Released </span>
            <span v-else class="badge pending"> Pending Release </span>

            <span v-if="proposal.released > 0" class="badge active"> Active </span>
          </td>

          <td>
            <button class="btn view" @click="$router.push('fundviewprop')">View</button>

            <button
              v-if="proposal.released === 0"
              class="btn release"
              @click="$router.push('fundrelease')"
            >
              Release
            </button>
          </td>
        </tr>

        <tr v-if="filteredProposals.length === 0">
          <td colspan="7" class="empty">No funded proposals found.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'FundedProposals',

  data() {
    return {
      search: '',
      proposals: [
        {
          id: 1,
          title: 'Community Development Program',
          proponent: 'Dr. Allen Shippy',
          totalBudget: 95000,
          released: 95000,
          releaseDate: '2024-11-28',
        },
        {
          id: 2,
          title: 'Healthcare Facility Upgrade',
          proponent: 'Dr. Meow Chan',
          totalBudget: 200000,
          released: 200000,
          releaseDate: '2024-12-01',
        },
        {
          id: 3,
          title: 'Educational Infrastructure Project',
          proponent: 'Dr. Cat Moon',
          totalBudget: 180000,
          released: 0,
          releaseDate: null,
        },
      ],
    }
  },

  computed: {
    filteredProposals() {
      return this.proposals.filter((p) => p.title.toLowerCase().includes(this.search.toLowerCase()))
    },

    totalFunded() {
      return this.proposals.length
    },

    fundsReleased() {
      return this.proposals.filter((p) => p.released > 0).length
    },

    pendingRelease() {
      return this.proposals.filter((p) => p.released === 0).length
    },

    totalReleased() {
      return Math.round(this.proposals.reduce((sum, p) => sum + p.released, 0) / 1000)
    },
  },
}
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: Arial, sans-serif;
  width: 135%;
}

.subtitle {
  color: #666;
  margin-bottom: 16px;
}

/* STATS */
.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.card {
  background: white;
  padding: 16px;
  border-radius: 10px;
}

.card.highlight {
  background: #fff8cc;
}

/* SEARCH */
.search {
  width: 100%;
  padding: 10px;
  margin-bottom: 16px;
}
/* TABLE */
.table {
  width: 100%;
  border-collapse: collapse;
}

thead {
  background: #4a437a;
  color: white;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
}

.green {
  color: #2e7d32;
  font-weight: bold;
}

/* BADGES */
.badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  margin-right: 6px;
  display: inline-block;
}

.success {
  background: #c8e6c9;
  color: #1b5e20;
}

.active {
  background: #e1bee7;
  color: #4a148c;
}

.pending {
  background: #ffe082;
  color: #e65100;
}

/* BUTTONS */
.btn {
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 12px;
  margin-right: 6px;
  cursor: pointer;
}

.view {
  background: #64b5f6;
  color: white;
}

.release {
  background: #ffd600;
  font-weight: bold;
}

.empty {
  text-align: center;
  color: #777;
}
</style>