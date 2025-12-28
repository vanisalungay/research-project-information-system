<template>
  <div class="manage-container">
    <h1 class="page-title">REC NOTIFICATIONS</h1>

    <div class="form-box">
      <!-- ASSIGNED PROPOSAL -->
      <div class="notification-card">
        <h3>Assigned Proposals</h3>
        <p>
          You have been assigned to review the proposal
          <strong>{{ assignedProposal.title }}</strong
          >.
        </p>
        <small>Assigned on {{ assignedProposal.date }}</small>
      </div>

      <!-- REVISION SUBMITTED -->
      <div class="notification-card" v-if="revisionSubmitted">
        <h3>Revision Submitted</h3>
        <p>
          No proposal revision submitted
          <strong>{{ revisionSubmitted.title }}</strong
          >.
        </p>
        <small>Submitted on {{ revisionSubmitted.date }}</small>
      </div>

      <!-- DECISION POSTED -->
      <div class="notification-card" v-if="decisionPosted">
        <h3>REC Decision Posted</h3>
        <p>
          A decision has been recorded for
          <strong>{{ decisionPosted.title }}</strong
          >.
        </p>
        <small>Status: {{ decisionPosted.status }}</small>
      </div>

      <!-- DEADLINE REMINDER -->
      <div class="notification-card warning" v-if="deadlineReminder">
        <h3>Review Deadline Reminder</h3>
        <p>
          Your review for
          <strong>{{ deadlineReminder.title }}</strong>
          is due on {{ deadlineReminder.deadline }}.
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const assignedProposal = ref({
  title: 'Community Development Program aand  Healthcare Faculty Upgrade',
  date: 'Dec 26, 2025',
})

const decisionPosted = ref({
  title: 'Healthcare Faculty Upgrade',
  status: 'Approved with Minor Revisions',
})

const revisionSubmitted = ref({})

const deadlineReminder = ref({
  title: 'Community Development Program',
  deadline: 'Dec 30, 2025',
})
</script>

<style scoped>
.page-title {
  background: #1c1a3c;
  padding: 18px;
  color: #ffd429;
  font-size: 28px;
  font-weight: bold;
}

.form-box {
  background: #1c1a3c;
  padding: 30px;
  border-radius: 8px;
  color: white;
  margin-top: 20px;
}

.notification-card {
  border: 1px solid #ffd429;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 15px;
}

.notification-card h3 {
  color: #ffd429;
  margin-bottom: 6px;
}

.notification-card p {
  margin-bottom: 6px;
}

.notification-card small {
  color: #ccc;
}

.notification-card.warning {
  color: black;
}

.notification-card.warning h3 {
  color: #1c1a3c;
}
</style>
