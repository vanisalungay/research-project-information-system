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
          <p>Endorsed from RPS</p>
          <h3>{{ stats.endorsed }}</h3>
        </div>

        <div class="card">
          <p>Under REC Review</p>
          <h3>{{ stats.underReview }}</h3>
        </div>

        <div class="card">
          <p>REC Approved</p>
          <h3>{{ stats.recApproved }}</h3>
        </div>

        <div class="card">
          <p>For Revision</p>
          <h3>{{ stats.revision }}</h3>
        </div>
      </div>

      <div class="content">
        <section class="table-section">
          <div class="table-header">
            <h3>Latest Endorsed Proposals</h3>
            <button class="view-all" @click="goToEndorsed">View All</button>
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
              <tr v-if="loading">
                <td colspan="5" style="text-align:center">
                  Loading...
                </td>
              </tr>

              <tr v-else-if="proposals.length === 0">
                <td colspan="5" style="text-align:center">
                  No proposals found.
                </td>
              </tr>

              <tr
                v-for="proposal in proposals.slice(0,5)"
                :key="proposal.id"
              >
                <td>{{ proposal.projectTitle }}</td>
                <td>
                  {{ proposal.proponent?.name || '-' }}
                </td>
                <td>
                  {{ proposal.createdAt?.substring(0,10) }}
                </td>
                <td>
                  <span
                    class="badge"
                    :class="proposal.status.toLowerCase()"
                  >
                    {{ proposal.status }}
                  </span>
                </td>
                <td>
                  <button
                    class="review"
                    @click="goToReview(proposal.id)"
                  >
                    Review
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </section>

       <aside class="notifications">

<h3>Notifications</h3>

<ul>

<li
    v-if="loading"
>
Loading...
</li>

<li
    v-else-if="notifications.length===0"
>
No notifications.
</li>

<li
    v-for="notification in notifications.slice(0,5)"
    :key="notification.id"
>

<strong>

{{ notification.message }}

</strong>

<small>

{{ notification.createdAt?.substring(0,16) }}

</small>

</li>

</ul>

<button
class="view-all"
@click="goToNotifications"
>

View All

</button>

</aside>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()

// Dashboard data (will come from backend)
const stats = ref({
  endorsed: 0,
  underReview: 0,
  recApproved: 0,
  revision: 0
})

const proposals = ref([])
const notifications = ref([])

const loading = ref(true)

const loadDashboard = async () => {
  try {
    // Fetch proposals
    const proposalResponse = await api.get('/api/proposals')

    proposals.value = proposalResponse.data

    stats.value.endorsed = proposals.value.filter(
      p => p.status === 'ENDORSED'
    ).length

    stats.value.underReview = proposals.value.filter(
      p => p.status === 'UNDER_REVIEW'
    ).length

    stats.value.recApproved = proposals.value.filter(
      p => p.status === 'REC_APPROVED' || p.status === 'OVC_APPROVED'
    ).length

    stats.value.revision = proposals.value.filter(
      p => p.status === 'REVISION' || p.status === 'REC_REVISION'
    ).length

    // Notification endpoint
    // Replace userId later using logged in user
    const notifResponse = await api.get(
      '/api/notifications?userId=4'
    )

    notifications.value = notifResponse.data

  } catch (error) {
    console.log(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDashboard()
})

const goToNotifications = () => {
  router.push('/notifications')
}

const goToEndorsed = () => {
  router.push('/endorsed-proposals')
}

const goToReview = id => {
  router.push(`/review-prop/${id}`)
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

.approved {
  background: #c8e6c9;
}

.endorsed {
  background: #bbdefb;
}

.submitted {
  background: #fff3cd;
}

.revision {
  background: #ffccbc;
}

.rejected {
  background: #ffcdd2;
}

.loading {
  text-align: center;
}
</style>
