<template>
  <div class="manage-container">
    <h1 class="page-title">REC NOTIFICATIONS</h1>

    <div class="form-box">
      <div
        v-if="notifications.length"
        v-for="notification in notifications"
        :key="notification.id"
        class="notification-card"
        :class="{ unread: !notification.isRead }"
      >
        <h3>
          {{ notification.isRead ? "Notification" : "New Notification" }}
        </h3>

        <p>{{ notification.message }}</p>

        <small>
          {{ formatDate(notification.createdAt) }}
        </small>

        <button
          v-if="!notification.isRead"
          class="mark-btn"
          @click="markAsRead(notification.id)"
        >
          Mark as Read
        </button>
      </div>

      <div v-else class="empty-state">
        No notifications available.
      </div>
    </div>
  </div>
</template>

<script setup>
import api from "@/utils/api"
import { ref, onMounted } from "vue"

const notifications = ref([])

const loadNotifications = async () => {
  try {
    const user = JSON.parse(
      atob(localStorage.getItem("user_data"))
    )

    const response = await api.get(
      `/api/notifications/user/${user.id}`
    )

    notifications.value = response.data
  } catch (err) {
    console.error(err)
  }
}

const markAsRead = async (id) => {
  try {
    await api.put(
      `/api/notifications/${id}/read`
    )

    loadNotifications()
  } catch (err) {
    console.error(err)
  }
}

const formatDate = (date) => {
  return new Date(date).toLocaleString()
}

onMounted(loadNotifications)
</script>

<style scoped>
.manage-container {
  padding: 30px;
}

.page-title {
  background: #1c1a3c;
  color: #ffd429;
  padding: 18px;
  font-size: 28px;
  font-weight: bold;
  border-radius: 8px;
}

.form-box {
  background: #1c1a3c;
  margin-top: 20px;
  padding: 30px;
  border-radius: 10px;
}

.notification-card {
  border: 1px solid #ffd429;
  border-radius: 8px;
  padding: 18px;
  margin-bottom: 18px;
  color: white;
}

.notification-card.unread {
  border-left: 6px solid #ffd429;
}

.notification-card h3 {
  color: #ffd429;
  margin-bottom: 10px;
}

.notification-card p {
  margin-bottom: 8px;
  line-height: 1.5;
}

.notification-card small {
  color: #ccc;
}

.mark-btn {
  margin-top: 12px;
  background: #ffd429;
  color: #1c1a3c;
  border: none;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: bold;
}

.mark-btn:hover {
  opacity: 0.9;
}

.empty-state {
  text-align: center;
  color: white;
  padding: 40px;
  font-size: 16px;
}
</style>