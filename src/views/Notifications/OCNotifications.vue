<template>
  <div class="notifications-wrapper">
    <h2 class="title">Notifications</h2>
    <p class="subtitle">Stay updated with your recent activities</p>

    <div class="notifications-card">
      <div class="notification-item">
        <div class="icon blue">
          <i class="pi pi-bell"></i>
        </div>
        <div class="text">
          <p class="message">New proposal for final approval</p>
          <span class="time">1 hour ago</span>
        </div>
      </div>

      <div class="divider"></div>

      <div class="notification-item">
        <div class="icon green">
          <i class="pi pi-check"></i>
        </div>
        <div class="text">
          <p class="message">Proposal approved and forwarded to funding</p>
          <span class="time">3 hours ago</span>
        </div>
      </div>

      <div class="divider"></div>

      <div class="notification-item">
        <div class="icon blue">
          <i class="pi pi-bell"></i>
        </div>
        <div class="text">
          <p class="message">2 proposals awaiting final endorsement</p>
          <span class="time">1 day ago</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'NotificationsContent',
}
</script>

<style scoped>
.notifications-wrapper {
  padding: 32px;
  background-color: #f9fafb;
  min-height: 100vh;
  width: 100%;
  box-sizing: border-box;
}

.title {
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.notifications-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 8px 0;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  width: 100%;
}

.notification-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
}

.icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 14px;
  font-size: 16px;
}

.icon.blue {
  background-color: #e0f2fe;
  color: #0284c7;
}

.icon.green {
  background-color: #dcfce7;
  color: #16a34a;
}

.text .message {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  margin: 0;
}

.text .time {
  font-size: 12px;
  color: #6b7280;
}

.divider {
  height: 1px;
  background-color: #e5e7eb;
  margin: 0 20px;
}
</style>
