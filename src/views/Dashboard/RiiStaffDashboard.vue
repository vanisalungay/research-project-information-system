<template>
  <div class="dashboard">
    <!-- Header -->
    <div class="header">
      <h2>RII Dashboard</h2>
      <p>Overview of research proposals and funding management</p>
    </div>

    <!-- Stats Cards -->
    <div class="stats">
      <div class="card">
        <div>
          <p class="label">Submitted Proposals</p>
          <h3>4</h3>
          <span class="sub">+4 this month</span>
        </div>
        <div class="icon purple">📄</div>
      </div>

      <div class="card">
        <div>
          <p class="label">Endorsed Proposals</p>
          <h3>3</h3>
          <span class="sub">75% approval rate</span>
        </div>
        <div class="icon green">✔</div>
      </div>

      <div class="card">
        <div>
          <p class="label">Funded Projects</p>
          <h3>2</h3>
          <span class="sub">Active projects</span>
        </div>
        <div class="icon yellow">💰</div>
      </div>

      <div class="card">
        <div>
          <p class="label">Funds Released</p>
          <h3>₱1.87M</h3>
          <span class="sub">76% of allocated</span>
        </div>
        <div class="icon blue">📈</div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="content">
      <!-- Proposals Table -->
      <div class="panel">
        <div class="panel-header">
          <h3>Proposals Awaiting RII Action</h3>
          <button class="view-all" @click="$router.push('riiendorsed-prop')">View All</button>
        </div>

        <div class="table">
          <div class="table-header">
            <span>Title</span>
            <span>Proponent</span>
            <span>Budget</span>
            <span>Status</span>
            <span>Actions</span>
          </div>

          <div class="table-row">
            <span>Community Development Program 2024</span>
            <span>Dr. Alien Shippy</span>
            <span>₱150,000</span>
            <span class="status pending">Pending</span>
            <button class="review" @click="$router.push('rii-endorse')">Review</button>
          </div>


        </div>
      </div>

      <!-- Right Side -->
      <div class="side">
        <!-- Recent Activity -->
        <div class="panel">
          <h3>Recent Activity</h3>
          <ul class="activity">
            <li>
              <strong>New proposal submitted:</strong> “Community Development Program 2024”
              <span>2 hours ago</span>
            </li>
           
          </ul>
          <button class="view-activity" @click="$router.push('riistaff-notification')">
            View All Activity
          </button>
        </div>

        <!-- Quick Actions -->
        <div class="panel">
          <h3>Quick Actions</h3>
          <button class="primary" @click="$router.push('rii-subproposal')">Review Proposals</button>
          <button class="secondary" @click="$router.push('funded-prop')">Manage Funding</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'RiiDashboard',
}
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
  grid-template-columns: 2fr 1fr;
  gap: 16px;
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

.view-all {
  background: #eee;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

/* Table */
.table {
  margin-top: 12px;
}

.table-header,
.table-row {
  display: grid;
  grid-template-columns: 2fr 1.5fr 1fr 1fr 1fr;
  padding: 10px;
  align-items: center;
}

.table-header {
  background: #4b3f72;
  color: #fff;
  border-radius: 6px;
}

.table-row {
  border-bottom: 1px solid #eee;
}

.status {
  padding: 4px 8px;
  border-radius: 12px;
  font-size: 12px;
  text-align: center;
}

.pending {
  background: #fff1cc;
  color: #b8860b;
}

.revision {
  background: #e0e7ff;
  color: #3f51b5;
}

.ready {
  background: #e6fff3;
  color: #2e7d32;
}

.review {
  background: #6aa9ff;
  border: none;
  color: #fff;
  padding: 6px;
  border-radius: 6px;
  cursor: pointer;
}

/* Activity */
.activity {
  list-style: none;
  padding: 0;
  font-size: 14px;
}

.activity li {
  margin-bottom: 12px;
}

.activity span {
  display: block;
  font-size: 12px;
  color: #999;
}

.view-activity {
  width: 100%;
  margin-top: 10px;
  padding: 8px;
  border-radius: 6px;
  border: none;
  background: #f0f0f0;
}

/* Buttons */
.primary {
  width: 100%;
  background: #ffd400;
  border: none;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 10px;
  font-weight: bold;
}

.secondary {
  width: 100%;
  background: #6aa9ff;
  border: none;
  padding: 10px;
  border-radius: 8px;
  color: #fff;
}
</style>
