<template>
  <div class="notif-container">
    <h2 class="title">Notifications</h2>

    <div class="notif-group" v-for="(group, index) in grouped" :key="index">
      <!-- GROUP HEADER -->
      <div class="group-header" @click="toggleGroup(index)">
        <div class="group-left">
          <div class="sender-icon">🔔</div>          
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
                <strong>
                    {{ item.isRead ? 'Read' : 'New Notification' }}
                </strong>
              </p>

              <p class="notif-msg">
                {{ item.message }}
              </p>

<p class="notif-time">
    {{ new Date(item.createdAt).toLocaleString() }}
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
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'
import { useUserDataStore } from '@/stores/userData'

const userStore = useUserDataStore()

const notifications = ref([])

const loadNotifications = async () => {
  try {
    const res = await axios.get(
      `http://localhost:8081/api/notifications/user/${userStore.user.id}`
    )

    notifications.value = res.data
  } catch (err) {
    console.error('Failed to load notifications', err)
  }
}

onMounted(loadNotifications)

const grouped = computed(() => {
  const map = {}

  notifications.value.forEach((notification) => {
    const sender = notification.sender || 'System'

    if (!map[sender]) {
      map[sender] = []
    }

    map[sender].push(notification)
  })

  return Object.keys(map).map((sender) => ({
    sender,
    items: map[sender],
  }))
})

const openGroup = ref(null)

const toggleGroup = (index) => {
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

.sender-icon{
    width:30px;
    height:30px;
    border-radius:50%;
    background:#2452ff;
    color:white;
    display:flex;
    align-items:center;
    justify-content:center;
    font-size:15px;
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