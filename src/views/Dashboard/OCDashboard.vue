<template>
  <div class="dashboard-content">
    
    <div class="summary-cards">
      <div class="card">
        <p class="card-title">Pending Final Approval</p>
        <h2>{{ stats.pending }}</h2>
      </div>

      <div class="card">
        <p class="card-title">Approved This Month</p>
        <h2>{{ stats.approvedThisMonth }}</h2>
      </div>

      <div class="card">
        <p class="card-title">Special Orders Issued</p>
        <h2>{{ stats.specialOrders }}</h2>
      </div>
    </div>

    
    <div class="section">
      <h3>Proposals for Final Approval</h3>

      <div class="proposal-card" v-for="proposal in forFinalApproval" :key="proposal.id">
        <div class="proposal-header">
          <h4>{{ proposal.title }}</h4>
          <span class="status endorsed">Endorsed</span>
        </div>

        <p class="tag">{{ proposal.category }}</p>

        <p class="meta">{{ proposal.adviser }} • {{ proposal.date }}</p>

        <button class="action-btn" @click="reviewProposal(proposal.id)">Review & Approve</button>
      </div>
    </div>

    
    <div class="section">
      <h3>Approved Proposals</h3>

      <div class="proposal-card" v-for="proposal in approvedProposals" :key="proposal.id">
        <div class="proposal-header">
          <h4>{{ proposal.title }}</h4>
          <span class="status approved">Approved</span>
        </div>

        <p class="tag">{{ proposal.category }}</p>

        <p class="meta">{{ proposal.adviser }} • {{ proposal.date }}</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DashboardContent',
  data() {
    return {
      stats: {
        pending: 3,
        approvedThisMonth: 7,
        specialOrders: 15,
      },

      forFinalApproval: [
        {
          id: 1,
          title: 'Community Development Program 2024',
          category: 'Dagat/Punla',
          adviser: 'Dr. Maria Santos',
          date: 'Dec 13, 2024',
        },
        {
          id: 2,
          title: 'Biodegradable Packaging from Agricultural Waste',
          category: 'Kalikasan',
          adviser: 'Dr. Lisa Garcia',
          date: 'Dec 12, 2024',
        },
      ],

      approvedProposals: [
        {
          id: 3,
          title: 'Climate-Resilient Rice Varieties Development',
          category: 'Punla',
          adviser: 'Dr. Thomas Lee',
          date: 'Nov 20, 2024',
        },
        {
          id: 4,
          title: 'Digital Literacy for Rural Communities',
          category: 'Tanglaw',
          adviser: 'Dr. Ramon Diaz',
          date: 'Nov 18, 2024',
        },
      ],
    }
  },
  methods: {
    reviewProposal(id) {
      this.$router.push(`/oc/final-approval/${id}`)
    },
  },
}
</script>

<style scoped>
.dashboard-content {
  padding: 24px;
}


.summary-cards {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.card {
  background: #ffffff;
  padding: 16px;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.card-title {
  font-size: 14px;
  color: #666;
}

.card h2 {
  margin-top: 8px;
  font-size: 28px;
  font-weight: bold;
}


.section {
  margin-bottom: 32px;
}

.section h3 {
  margin-bottom: 16px;
}


.proposal-card {
  background: #ffffff;
  padding: 16px;
  border-radius: 10px;
  margin-bottom: 16px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.proposal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.tag {
  color: #0aa;
  font-size: 13px;
  margin: 4px 0;
}

.meta {
  font-size: 13px;
  color: #777;
}


.status {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

.status.endorsed {
  background: #efe3ff;
  color: #6a1b9a;
}

.status.approved {
  background: #e3f9ec;
  color: #2e7d32;
}


.action-btn {
  margin-top: 12px;
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 6px;
  background: linear-gradient(to right, #009688, #1976d2);
  color: #fff;
  cursor: pointer;
}
</style>
