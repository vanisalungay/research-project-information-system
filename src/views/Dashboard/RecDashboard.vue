<template>
  <div class="dashboard-layout">
    <!-- MAIN -->
    <div class="main">
      <header class="topbar">
        <div>
          <h2>REC Dashboard</h2>
          <small>Research Evaluation Committee</small>
        </div>
      </header>

      <section class="stats">
        <div class="stat-card">
          <p>Assigned Proposal</p>
          <h3>2</h3>
          <small>2 this week</small>
        </div>

        <div class="stat-card">
          <p>Completed Reviews</p>
          <h3>1</h3>
          <small>67% approval rate</small>
        </div>

        <div class="stat-card">
          <p>Pending Evaluation</p>
          <h3>1</h3>
          <small>Awaiting action</small>
        </div>
      </section>

      <section class="content">
        <div class="table-card">
          <div class="table-header">
            <h3>Latest Assigned Proposals</h3>
            <button class="view-all" @click="goToAssigned">View All</button>
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
                <td>Dr. Alien Shippy</td>
                <td>2024-12-10</td>
                <td><span class="badge pending">Pending</span></td>
                <td><button class="review" @click="goToReview">Review</button></td>
              </tr>

              <tr>
                <td>Healthcare Facility Upgrade</td>
                <td>Dr. Meow Chan</td>
                <td>2024-12-05</td>
                <td><span class="badge complete">Complete</span></td>
                <td><button class="btn view" @click="goToMeeting">View</button></td>
              </tr>
            </tbody>
          </table>
        </div>

        <aside class="notif-card">
          <h3>Notifications</h3>

          <ul>
            <li>
              New assigned proposal:
              <strong>Community Development Program 2024</strong>
              <small>2 hours ago</small>
            </li>

            <li>
              Rec decision posted
              <small>5 hours ago</small>
            </li>
          </ul>

          <button class="view-all" @click="goToNotifications">View All</button>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToNotifications = () => {
  router.push('/rec-notif')
}

const goToAssigned = () => {
  router.push('/assigned-proposals')
}

const goToReview = () => {
  router.push('/review-form')
}

const goToMeeting = () => {
  router.push('/meeting-details')
}
</script>

<style>
.dashboard-layout {
  display: flex;
  height: 100vh;
  font-family: Poppins, sans-serif;
}

/* MAIN */
.main {
  flex: 1;
  background: #f5f6fa;
  padding: 20px;
}

/* TOP BAR */
.topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.topbar small {
  color: #666;
}

.user {
  text-align: right;
  font-size: 13px;
}

/* STATS */
.stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
  margin-bottom: 20px;
}

.stat-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
}

.stat-card p {
  font-size: 13px;
  color: #666;
}

/* CONTENT */
.content {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 20px;
}

/* TABLE */
.table-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.view-all {
  border: none;
  background: none;
  color: #2563eb;
  cursor: pointer;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}

th,
td {
  padding: 10px;
  border-bottom: 1px solid #eee;
}

.badge {
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.pending {
  background: #ffe082;
}

.complete {
  background: #a5d6a7;
}

.btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 8px;
  cursor: pointer;
}

.btn.view {
  background: #6b7280;
}

/* NOTIFICATIONS */
.notif-card {
  background: white;
  padding: 16px;
  border-radius: 12px;
}

.notif-card ul {
  list-style: none;
  padding: 0;
}

.notif-card li {
  font-size: 13px;
  margin-bottom: 12px;
}

.notif-card small {
  display: block;
  color: #777;
}

.view-all-notif {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  background: none;
  cursor: pointer;
}
</style>
