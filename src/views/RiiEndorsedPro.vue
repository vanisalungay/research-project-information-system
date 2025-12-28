<template>
  <div class="page">
    <!-- Header -->
    <h2>RII Endorsed Proposals</h2>
    <p class="subtitle">Proposals endorsed by RII and forwarded to OVCRIGE</p>

    <!-- Search -->
    <input type="text" v-model="search" placeholder="Search endorsed proposals..." class="search" />

    <!-- Table -->
    <table class="table">
      <thead>
        <tr>
          <th>Title</th>
          <th>Proponent</th>
          <th>Category</th>
          <th>RII Endorsed</th>
          <th>Sent to OVCRIGE</th>
          <th>OVCRIGE Status</th>
          <th>Actions</th>
        </tr>
      </thead>

      <tbody>
        <tr v-for="proposal in filteredProposals" :key="proposal.id">
          <td>{{ proposal.title }}</td>
          <td>{{ proposal.proponent }}</td>
          <td>{{ proposal.category }}</td>

          <td>
            <span class="check">✔</span>
            {{ proposal.riiEndorsed }}
          </td>

          <td>
            <span class="plane">✈</span>
            {{ proposal.sentToOVCRIGE }}
          </td>

          <td>
            <span
              :class="[
                'status',
                proposal.ovcrigeStatus === 'Approved by OVCRIGE' ? 'approved' : 'pending',
              ]"
            >
              {{ proposal.ovcrigeStatus }}
            </span>
          </td>

          <td>
            <button class="view-btn" @click="$router.push('fundviewprop')">View</button>
          </td>
        </tr>

        <tr v-if="filteredProposals.length === 0">
          <td colspan="7" class="empty">No endorsed proposals found.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'RIIEndorsedProposals',

  data() {
    return {
      search: '',
      proposals: [
        {
          id: 1,
          title: 'Community Development Program 2024',
          proponent: 'Dr. Alien Shippy',
          category: 'Kalikasan Program',
          riiEndorsed: '2024-12-11',
          sentToOVCRIGE: '2024-12-11',
          ovcrigeStatus: 'Pending OVCRIGE Review',
        },
        {
          id: 2,
          title: 'Educational Infrastructure Project',
          proponent: 'Dr. Cat Moon',
          category: 'Tanglaw Program',
          riiEndorsed: '2024-12-09',
          sentToOVCRIGE: '2024-12-09',
          ovcrigeStatus: 'Approved by OVCRIGE',
        },
        {
          id: 3,
          title: 'Environmental Conservation Project',
          proponent: 'Dr. Awie Chun',
          category: 'Tanglaw Program',
          riiEndorsed: '2024-12-01',
          sentToOVCRIGE: '2024-12-01',
          ovcrigeStatus: 'Approved by OVCRIGE',
        },
      ],
    }
  },

  computed: {
    filteredProposals() {
      return this.proposals.filter((p) => p.title.toLowerCase().includes(this.search.toLowerCase()))
    },
  },

  methods: {
    viewProposal(proposal) {
      alert(`Viewing Proposal:\n\n${proposal.title}\nStatus: ${proposal.ovcrigeStatus}`)
    },
  },
}
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: Arial, sans-serif;
}

.subtitle {
  color: #666;
  margin-bottom: 16px;
}

.search {
  width: 130%;
  padding: 8px;
  margin-bottom: 16px;
}

.table {
  width: 135%;
  border-collapse: collapse;
  table-layout: fixed;
}

thead {
  background: #3f3b74;
  color: white;
}

th,
td {
  padding: 12px;
  border-bottom: 1px solid #eee;
  max-width: 260px;
}

.check {
  color: green;
  margin-right: 4px;
}

.plane {
  color: #3b82f6;
  margin-right: 4px;
}

.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  margin-bottom: 4px;
  display: inline-block;
}

.pending {
  background: #fde68a;
  color: #92400e;
}

.approved {
  background: #dcfce7;
  color: #166534;
}

.view-btn {
  background: #60a5fa;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #777;
}
</style>
