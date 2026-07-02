krisha, [29/12/2025 2:12 pm]
<template>
  <div class="dashboard">
    <!-- Header -->
    <div class="header">
      <h2>RPS Dashboard</h2>
      <p>Overview of research proposals and funding management</p>
    </div>

<!-- Stats Cards -->
<div class="stats">

  <div class="card">
    <div>
      <p class="label">Total Proposals</p>
      <h3>{{ totalProposals }}</h3>
    </div>
  </div>

  <div class="card">
    <div>
      <p class="label">Endorsed Proposals</p>
      <h3>{{ endorsedProposals }}</h3>
    </div>
  </div>

  <div class="card">
    <div>
      <p class="label">Pending Review</p>
      <h3>{{ pendingReview }}</h3>
    </div>
  </div>

  <div class="card">
    <div>
      <p class="label">Failed / Returned</p>
      <h3>{{ failedProposals }}</h3>
    </div>
  </div>

</div>

    <!-- Main Content -->
    <div class="content">
      <!-- Proposals Table -->
      <div class="panel">
        <div class="panel-header">
          <h3>RECENT ACTIVITY</h3>
        </div>

        <div v-if="activities.length > 0" class="activity-container">
          <div
            v-for="item in activities"
            :key="item.id"
            :class="['activity-item', item.borderClass]"
          >
            <div class="activity-main">
              <div class="activity-text">
                <h4>{{ item.title }}</h4>
                <p>{{ item.description }}</p>
              </div>
              <span :class="['badge', item.badgeClass]">{{ item.status }}</span>
            </div>
          </div>
        </div>

        <div v-else class="empty-state">
  <div class="empty-icon-wrapper">
    📄
  </div>

  <h3>No Research Activity Yet</h3>

  <p>
    Research proposals, reviews, and status updates
    will appear here once records are added.
  </p>
</div>
      </div>

    
    </div>
  </div>
</template>

<script>
export default {
  name: "RiiDashboard",

  data() {
    return {
      activities: [],
      totalProposals: 0,
      endorsedProposals: 0,
      pendingReview: 0,
      failedProposals: 0
    };
  }
};
</script>

<style scoped>
.dashboard {
  padding: 24px;
  width: 500%;
  max-width: 135%;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
  background: #f7f8fc;
}

.header h2 {
  margin: 0;
}

.header p {
  color: #777;
  margin-bottom: 20px;
}

/* Stats */
.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
}

.card {
  background: #fff;
  padding: 16px;
  border-radius: 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.label {
  font-size: 14px;
  color: #666;
}

.sub {
  font-size: 12px;
  color: #999;
}

.icon {
  padding: 10px;
  border-radius: 50%;
  font-size: 18px;
}

.purple {
  background: #ece9ff;
}
.green {
  background: #e6fff3;
}
.yellow {
  background: #fff6e0;
}
.blue {
  background: #e8f2ff;
}

/* Main Content */
.content {
  display: grid;
grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));  gap: 16px;
  margin-top: 20px;
}

.panel {
  background: #fff;
  padding: 16px;
  border-radius: 12px;
}
.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}



.activity-box {
  width: 100%;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
}

.activity-header h3 {
  margin: 0 0 16px 0;
  color: #555555;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.activity-container {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.activity-item {
  background: #f7f6f0; /* Soft tint content wrapper to match the photo */
  border-radius: 16px;
  padding: 16px 20px;
  box-sizing: border-box;
  width: 100%;
}

/* Vertical Left Borders styled directly from screenshot */
.pass-border { border-left: 5px solid #4a7c59; }
.review-border { border-left: 5px solid #2b6cb0; }
.fail-border { border-left: 5px solid #c53030; }

.activity-main {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.activity-text h4 {
  margin: 0 0 4px 0;
  font-size: 16px;
  color: #2d3748;
  font-weight: 700;
}

.activity-text p {
  margin: 0;
  font-size: 13px;
  color: #718096;
}

/* Badge styling matching the layout */
.badge {
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 700;
}

.passed-badge {
  background: #e6f4ea;
  color: #137333;
}

.review-badge {
  background: #e8f0fe;
  color: #1a73e8;
}

.failed-badge {
  background: #fde8e8;
  color: #9b1c1c;
}

.empty-state{
  display:flex;
  flex-direction:column;
  justify-content:center;
  align-items:center;
  text-align:center;
  padding:70px 20px;
}

.empty-icon-wrapper{
  width:90px;
  height:90px;
  border-radius:50%;
  background:#f3f4f6;
  display:flex;
  align-items:center;
  justify-content:center;
  font-size:42px;
  margin-bottom:20px;
}

.empty-state h3{
  margin:0;
  color:#374151;
  font-size:22px;
}

.empty-state p{
  margin-top:10px;
  color:#6b7280;
  max-width:420px;
  line-height:1.6;
}
</style>
