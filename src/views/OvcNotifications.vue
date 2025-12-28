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
    label: 'new proposal submitted',
    message: 'Community Development Program 2024 has been submitted for OVCRIGE review.',
    time: '2 hours ago',
  },
  {
    sender: 'REC Committee',
    label: 'evaluation submitted',
    message: 'REC has completed evaluation for Healthcare Facility Upgrade.',
    time: '5 hours ago',
  },
  {
    sender: 'System',
    label: 'proposal pending action',
    message: 'Youth Empowerment Program is awaiting OVCRIGE decision.',
    time: '1 day ago',
  },
  {
    sender: 'System',
    label: 'proposal forwarded',
    message: 'Educational Infrastructure Project has been forwarded to REC for review.',
    time: '2 days ago',
  },

  {
    sender: 'System',
    label: 'proposal endorsed',
    message: 'Environmental Conservation Project has been endorsed by OVCRIGE.',
    time: '1 week ago',
  },
])

const grouped = computed(() => {
  const map = {}

  notifications.value.forEach((noti) => {
    if (!map[noti.sender]) map[noti.sender] = []
    map[noti.sender].push(noti)
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
  margin-bottom: 18px;
}

.notif-left {
  display: flex;
  align-items: start;
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
}

.notif-msg {
  color: #555;
  margin-top: 2px;
  font-size: 13px;
}

.notif-time {
  font-size: 12px;
  color: #777;
}
</style>
