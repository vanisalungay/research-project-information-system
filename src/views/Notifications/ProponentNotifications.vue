<template>
  <div class="notifications-wrapper">
    <!-- Search -->
    <div class="search-box">
      <input
        type="text"
        placeholder="Search your proposals by title, ID, or status..."
        v-model="search"
      />
    </div>

    <h3 class="section-title">Notifications</h3>

    <!-- Notifications -->
    <div v-if="filteredNotifications.length">

      <div
        class="notification-card"
        v-for="notification in filteredNotifications"
        :key="notification.id"
      >

        <div
          class="notification-header"
          @click="toggleNotification(notification.id)"
        >

          <div class="icon success"></div>

          <div class="notification-text">

            <strong>{{ notification.sender }}</strong>

            <span class="status">
              • {{ notification.status }}
            </span>

            <p>
              {{ notification.message }}
            </p>

            <small>
              {{ notification.proposal_id }}
              ·
              {{ notification.created_at }}
              ·
              {{ notification.thread.length }} messages
            </small>

          </div>

          <div class="arrow">
            {{ activeNotification === notification.id ? '▲' : '▼' }}
          </div>

        </div>

        <transition name="fade">

          <div
            v-if="activeNotification === notification.id"
            class="notification-thread"
          >

            <p class="thread-title">
              Thread: {{ notification.project_title }}
            </p>

            <div
              class="thread-item"
              v-for="message in notification.thread"
              :key="message.id"
            >

              <div class="thread-header">
                {{ message.sender }}
                •
                {{ message.status }}
              </div>

              <p class="thread-message">
                {{ message.message }}
              </p>

              <small class="thread-time">
                {{ message.created_at }}
              </small>
            </div>
          </div>
        </transition>
      </div>
    </div>

    <!-- EMPTY STATE -->
    <div
      v-else
      class="notification-card empty-card"
    >

      <div class="notification-text">

        <strong>No Notifications</strong>

        <p>
          You don't have any notifications yet.
        </p>

        <small>
          Notifications from reviewers, OVCRIGE, REC, and RII staff will appear here once there are updates on your proposals.
        </small>
      </div>
    </div>    
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const loading = ref(false)

const search = ref('')

const activeNotification = ref(null)

const notifications = ref([])

const filteredNotifications = computed(() => {
  if (!search.value) return notifications.value

  const keyword = search.value.toLowerCase()

  return notifications.value.filter(notification =>
    notification.project_title.toLowerCase().includes(keyword) ||
    notification.proposal_id.toLowerCase().includes(keyword) ||
    notification.status.toLowerCase().includes(keyword)
  )
})

function toggleNotification(id) {
  activeNotification.value =
    activeNotification.value === id ? null : id
}

async function fetchNotifications() {
  loading.value = true

  try {
    // API goes here later
    //
    // Example:
    // const response = await axios.get(...)
    //
    // notifications.value = response.data.notifications

  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchNotifications()
})
</script>

<style scoped>
.notifications-wrapper {
  padding: 24px;
  background: #f8fafc;
}

.search-box input {
  width: 100%;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  margin-bottom: 16px;
}

.section-title {
  font-weight: 600;
  margin-bottom: 12px;
}

.notification-card {
  background: #eef6ff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 10px;
}

.notification-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
}

.icon {
  width: 10px;
  height: 10px;
  background: #22c55e;
  border-radius: 50%;
  margin-top: 6px;
}

.notification-text {
  flex: 1;
}

.notification-text p {
  margin: 4px 0;
}

.status {
  color: #2563eb;
  font-size: 13px;
}

.arrow {
  font-size: 14px;
  color: #64748b;
}

.notification-thread {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-top: -5px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
}

.thread-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 14px;
  color: #0f172a;
}

.thread-item {
  background: #f8fafc;
  padding: 12px;
  border-radius: 10px;
  margin-bottom: 10px;
}

.thread-header {
  font-weight: 600;
  font-size: 14px;
  color: #1e293b;
  margin-bottom: 4px;
}

.thread-message {
  font-size: 13px;
  color: #475569;
  line-height: 1.4;
  margin: 0 0 6px 0;
}

.thread-time {
  font-size: 12px;
  color: #94a3b8;
}

.fade-enter-active,
.fade-leave-active {
  transition: all 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.empty-card {
  background: white;
  border: 1px solid #e5e7eb;
  cursor: default;
}

.empty-card .notification-text {
  width: 100%;
  text-align: center;
  padding: 20px;
}

.empty-card strong {
  display: block;
  font-size: 18px;
  margin-bottom: 8px;
  color: #1e293b;
}

.empty-card p {
  color: #64748b;
  margin-bottom: 8px;
}

.empty-card small {
  color: #94a3b8;
}
</style>
