<template>
  <div class="dashboard-layout">
    <!-- SIDEBAR -->
    <!-- <aside class="sidebar">
      <h3 class="sidebar-title">Menu</h3>

      <ul class="menu">
        <li class="active">Dashboard</li>
        <li>Submitted Proposals</li>
        <li>Endorsed Proposal</li>
        <li>Notifications</li>
      </ul>
    </aside> -->

    <!-- MAIN CONTENT -->
    <div class="main">
      <!-- TOP BAR -->
      <!-- <header class="topbar">
        <h2>OVCRIGE Dashboard</h2>

    
      </header> -->

      <!-- STATS -->
      <div class="stats">
        <div class="card">
          <p>Total Submissions</p>
          <h3>4</h3>
          <small>his week</small>
        </div>

        <div class="card">
          <p>Endorsed Proposals</p>
          <h3>2</h3>
          <small>0% approval rate</small>
        </div>

        <div class="card">
          <p>Pending Endorsement</p>
          <h3>2</h3>
          <small>Awaiting OVCRIGE action</small>
        </div>

        <div class="card">
          <p>Returned for Revision</p>
          <h3>0</h3>
          <small>No deadline today</small>
        </div>
      </div>

      <div class="content">
        <section class="table-section">
          <div class="table-header">
            <h3>Latest Submitted Proposals</h3>
            <button class="view-all" @click="goToSubmitted">View All</button>
          </div>

          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Proponent</th>
                <th>Submitted</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              <tr>
                <td>Community Development Program 2024</td>
                <td>Dr. Aljon Shipay</td>
                <td>2024-12-10</td>
                <td><span class="badge pending">Pending</span></td>
                <td><button class="review" @click="goToReview">View Proposal</button></td>
              </tr>

              <tr>
                <td>Healthcare Facility Upgrade</td>
                <td>Dr. Meow Chan</td>
                <td>2024-12-05</td>
                <td><span class="badge pending">Pending</span></td>
                <td>
                  <button class="review" @click="goToReview">View Proposal</button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

        <aside class="notifications">
          <h3>Notifications</h3>

          <ul>
            <li>
              <strong>New proposal submitted</strong><br />
              Community Development Program 2024
              <small>2 hours ago</small>
            </li>

            <li>
              <strong>Proposal endorsed to REC</strong><br />
              Healthcare Facility Upgrade
              <small>1 day ago</small>
            </li>
          </ul>

          <button class="view-all" @click="goToNotifications">View All</button>
        </aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToNotifications = () => {
  router.push('/notifications')
}

const goToSubmitted = () => {
  router.push('/submit-proposals')
}

const goToReview = () => {
  router.push('/review-prop')
}
</script>

<style scoped>
.dashboard-layout {
  display: flex;
  height: 100vh;
  font-family: Poppins, sans-serif;
}

.sidebar {
  width: 220px;
  background: #ffdf00;
  padding: 20px;
}

.sidebar-title {
  font-weight: 700;
  margin-bottom: 20px;
}

.menu {
  list-style: none;
  padding: 0;
}

.menu li {
  padding: 12px;
  margin-bottom: 10px;
  border-radius: 6px;
  cursor: pointer;
}

.menu li.active,
.menu li:hover {
  background: #1f1a3d;
  color: white;
}

.main {
  flex: 1;
  background: #f6f7fb;
  padding: 20px;
}

.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user {
  text-align: right;
  font-size: 13px;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 15px;
  margin: 20px 0;
}

.card {
  background: white;
  padding: 15px;
  border-radius: 10px;
}

.content {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 20px;
}

.table-section {
  background: white;
  padding: 15px;
  border-radius: 10px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

td {
  padding: 10px;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.badge {
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
}

.pending {
  background: #ffe082;
}

.revision {
  background: #90caf9;
}

.review {
  background: #2452ff;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
}

.notifications {
  background: white;
  padding: 15px;
  border-radius: 10px;
}

.notifications ul {
  list-style: none;
  padding: 0;
}

.notifications li {
  margin-bottom: 12px;
  font-size: 13px;
}

.notifications small {
  display: block;
  color: #777;
}

.view-all-notif {
  margin-top: 10px;
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  background: none;
}
</style>
