<template>
  <div class="notif-wrapper">
    <!-- PAGE HEADER -->
    <div class="notif-header">
      <h1>NOTIFICATIONS</h1>
    </div>

    <!-- NOTIFICATION LIST -->
    <div class="notif-list" v-if="notifications.length > 0">
      <div class="notif-card" v-for="(notif, index) in notifications" :key="index">
        <p class="notif-title">{{ notif.title }}</p>
        <p class="notif-desc">{{ notif.description }}</p>

        <div class="notif-actions">
          <button class="btn-yellow" @click="$router.push('ovcaf-validate')">View Proposal</button>
        </div>
      </div>
    </div>

    <!-- NO NOTIFICATIONS -->
    <div class="empty-box" v-else>
      <p>No new notifications.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const proposalQueue = ref([
  {
    id: 24,
    title: 'New project proposal submitted',
    description: 'Community-Based Water Filtration System — requires approval.',
  },
])

const notifications = ref([])
const router = useRouter()

onMounted(() => {
  // only show proposals that aren't approved yet
  notifications.value = proposalQueue.value
})

// redirect user to your proposal page
const goToProposal = (id) => {
  router.push(`/proposal/${id}`)
}
</script>

<style scoped>
.notif-wrapper {
  font-family: Arial, sans-serif;
  width: 100%;
  padding-bottom: 50px;
}

.notif-header {
  background: #1c2145;
  padding: 20px 30px;
  color: white;
  font-weight: bold;
}

.notif-header h1 {
  margin: 0;
  font-size: 28px;
}

.notif-list {
  padding: 30px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.notif-card {
  background: #3c376b;
  padding: 20px 30px;
  border-radius: 25px;
  color: #ffd700;
}

.notif-title {
  font-size: 18px;
  font-weight: bold;
  margin: 0 0 8px 0;
}

.notif-desc {
  margin: 0;
  font-size: 15px;
  line-height: 1.4;
}

.notif-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-start;
}

.btn-yellow {
  background: #ffd400;
  border: none;
  padding: 10px 18px;
  border-radius: 10px;
  font-weight: bold;
  cursor: pointer;
  font-size: 14px;
}

.empty-box {
  padding: 40px;
  text-align: center;
  color: gray;
  font-size: 16px;
}
</style>
