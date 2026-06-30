<template>
  <div class="notif-container">
    <h2 class="title">Notifications</h2>

    <div class="notif-group" v-for="(group, index) in grouped" :key="index">
      <!-- GROUP HEADER -->
      <div class="group-header" @click="toggleGroup(index)">
        <div class="group-left">
          <img src="https://via.placeholder.com/25" class="sender-icon" />
          <span class="sender">{{ group.sender }}</span>
        </div>

        <i class="arrow" :class="{ open: openGroup === index }">▼</i>
      </div>

      <!-- COLLAPSIBLE CONTENT -->
      <div class="group-content" v-if="openGroup === index">
        <div class="notif-item" v-for="(item, i) in group.items" :key="i">
          <div class="notif-left">
            <span class="dot"></span>

            <div>
              <p class="notif-label">
                <strong>{{ item.label }}</strong>
              </p>

              <p class="notif-msg">{{ item.message }}</p>
            </div>
          </div>

          <p class="notif-time">{{ item.time }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const notifications = ref([
  {
    sender: 'System',
    label: 'New Proposal Submitted',
    message: 'Community Development Program 2024 has been submitted for OVCRIGE review.',
    time: '2 hours ago',
  },
  {
    sender: 'OVCRIGE',
    label: 'Proposal Returned for Revision',
    message:
      'Healthcare Facility Upgrade has been returned to the proponent for revision.',
    time: '6 hours ago',
  },
  {
    sender: 'OVCRIGE',
    label: 'Proposal Endorsed to REC',
    message:
      'Educational Infrastructure Project has been endorsed to the Research Evaluation Committee (REC).',
    time: '1 day ago',
  },
  {
    sender: 'REC',
    label: 'Evaluation Started',
    message:
      'REC has started evaluating Technology Innovation Initiative.',
    time: '2 days ago',
  },
  {
    sender: 'REC',
    label: 'Evaluation Completed',
    message:
      'REC has completed the evaluation of Educational Infrastructure Project.',
    time: '3 days ago',
  },
  {
    sender: 'REC',
    label: 'Recommendation Submitted',
    message:
      'REC has submitted its recommendation for Community Development Program 2024.',
    time: '5 days ago',
  },
])

const grouped = computed(() => {
  const map = {}

  notifications.value.forEach((notification) => {
    if (!map[notification.sender]) {
      map[notification.sender] = []
    }

    map[notification.sender].push(notification)
  })

  return Object.keys(map).map((sender) => ({
    sender,
    items: map[sender],
  }))
})

const openGroup = ref(null)

function toggleGroup(index) {
  openGroup.value = openGroup.value === index ? null : index
}
</script>

<style scoped>
.notif-container {
  padding: 20px;
}

.title {
  font-size: 22px;
  font-weight: 700;
  margin-bottom: 15px;
}

.notif-group {
  background: #fff;
  border-radius: 12px;
  padding: 12px 18px;
  margin-bottom: 15px;
  box-shadow: 0 0 4px #dcdcdc;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  cursor: pointer;
}

.group-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.sender-icon {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.sender {
  font-weight: 600;
  color: #2f2b57;
}

.arrow {
  font-size: 14px;
  transition: 0.2s;
}

.arrow.open {
  transform: rotate(180deg);
}

.group-content {
  margin-top: 12px;
  padding-left: 10px;
  border-left: 3px solid #ddd;
}

.notif-item {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.notif-left {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.dot {
  width: 9px;
  height: 9px;
  background: #3b82f6;
  border-radius: 50%;
  margin-top: 5px;
}

.notif-label {
  font-size: 14px;
  margin: 0;
}

.notif-msg {
  color: #555;
  margin-top: 4px;
  font-size: 13px;
}

.notif-time {
  font-size: 12px;
  color: #777;
  white-space: nowrap;
}
</style>