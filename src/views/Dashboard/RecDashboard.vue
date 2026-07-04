<template>
  <div class="dashboard-layout">
    <div class="main">
      <header class="topbar">
        <div>
          <h2>REC Dashboard</h2>
          <small>Research Evaluation Committee</small>
        </div>
      </header>

      <!-- Statistics -->
      <section class="stats">
        <div class="stat-card">
          <p>Submitted Proposals</p>
          <h3>{{ totalAssigned }}</h3>
        </div>

        <div class="stat-card">
          <p>Completed Reviews</p>
          <h3>{{ completedReviews }}</h3>
        </div>

        <div class="stat-card">
          <p>Pending Evaluation</p>
          <h3>{{ pendingEvaluation }}</h3>
        </div>
      </section>

      <section class="content">
        <!-- Latest Proposals -->
        <div class="table-card">
          <div class="table-header">
            <h3>Latest Assigned Proposals</h3>

            <button class="view-all" @click="goToAssigned">
              View All
            </button>
          </div>

          <table>
            <thead>
              <tr>
                <th>Title</th>
                <th>Proponent</th>
                <th>Date Submitted</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody v-if="latestProposals.length">
              <tr
                v-for="proposal in latestProposals"
                :key="proposal.id"
              >
                <td>{{ proposal.projectTitle }}</td>

                <td>{{ proposal.proponent?.name }}</td>

                <td>{{ proposal.createdAt?.substring(0, 10) }}</td>

                <td>
                  <span
                    class="badge"
                    :class="
                      proposal.status === 'UNDER_REVIEW'
                        ? 'pending'
                        : 'complete'
                    "
                  >
                    {{ proposal.status }}
                  </span>
                </td>

                <td>
                  <button
                    v-if="proposal.status === 'UNDER_REVIEW'"
                    class="btn"
                    @click="goToReview(proposal.id)"
                  >
                    Review
                  </button>

                  <button
                    v-else
                    class="btn view"
                    @click="goToMeeting(proposal.id)"
                  >
                    View
                  </button>
                </td>
              </tr>
            </tbody>

            <tbody v-else>
              <tr>
                <td colspan="5" style="text-align:center">
                  No submitted proposals.
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Notifications -->
        <aside class="notif-card">
          <h3>Notifications</h3>

          <ul v-if="notifications.length">
            <li
              v-for="notification in notifications.slice(0,5)"
              :key="notification.id"
            >
              {{ notification.message }}

              <small>
                {{ formatDate(notification.createdAt) }}
              </small>
            </li>
          </ul>

          <small v-else>No notifications.</small>

          <button class="view-all" @click="goToNotifications">
            View All
          </button>
        </aside>
      </section>
    </div>
  </div>
</template>

<script setup>
import api from '@/utils/api'
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const proposals = ref([])
const notifications = ref([])

const latestProposals = computed(() =>
  [...proposals.value]
    .sort((a, b) => b.id - a.id)
    .slice(0, 5)
)

const totalAssigned = computed(() =>
  proposals.value.length
)

const completedReviews = computed(() =>
  proposals.value.filter(
    p =>
      p.status === 'REC_APPROVED' ||
      p.status === 'ENDORSED'
  ).length
)

const pendingEvaluation = computed(() =>
  proposals.value.filter(
    p =>
      p.status === 'UNDER_REVIEW'
  ).length
)

const loadDashboard = async () => {
  try {
    const proposalRes = await api.get('/api/proposals')
    const allProposals = proposalRes.data || []

    // Get current user
    const stored = localStorage.getItem('user_data')
    if (stored) {
      const user = JSON.parse(atob(stored))

      // Filter proposals assigned to this REC user
      // Assuming proposals have an 'assignedTo' field or similar
      // For now, show all UNDER_REVIEW proposals
      proposals.value = allProposals.filter(p =>
        p.status === 'UNDER_REVIEW' ||
        p.status === 'REC_APPROVED' ||
        p.status === 'REC_REJECTED'
      )

      // Load notifications
      const notifRes = await api.get(`/api/notifications?userId=${user.id}`)
      notifications.value = notifRes.data || []
    }
  } catch (err) {
    console.error(err)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(loadDashboard)

const goToNotifications = () => {
  router.push('/rec-notif')
}

const goToAssigned = () => {
  router.push('/assigned-proposals')
}

const goToReview = (id) => {
  router.push({ name: 'RecReviewForm', params: { id } })
}

const goToMeeting = (id) => {
  router.push({ name: 'RecMeetingDetails', params: { id } })
}
</script>

<style>
.dashboard-layout {
  display: flex;
  height: 100vh;
  font-family: Poppins, sans-serif;
}

.main {
  flex: 1;
  background: #f5f6fa;
  padding: 20px;
}

.topbar {
  margin-bottom: 20px;
}

.topbar small {
  color: #666;
}

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
  color: #666;
  font-size: 13px;
}

.content {
  gap: 20px;
}

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
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
}

.btn.view {
  background: #6b7280;
}

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
  margin-bottom: 12px;
}

.notif-card small {
  display: block;
  color: #777;
}
</style>