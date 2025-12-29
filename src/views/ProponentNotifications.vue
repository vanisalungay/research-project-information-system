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

    <!-- Notification Item -->
    <div
      class="notification-card"
      @click="toggleNotification(1)"
      >
      <div class="notification-header">
        <div class="icon success"></div>
        
        <div class="notification-text">
          <strong>REC Committee</strong>
          <span class="status"> • requested revision</span>
          <p>
            Please update the informed consent forms to include detailed risk
            disclosures for community participants.
          </p>
          <small>P-2024-003 · 3 hours ago · 4 messages</small>
        </div>

        <div class="arrow">
          {{ activeNotification === 1 ? "▲" : "▼" }}
        </div>
      </div>
    </div>

    <!-- TOGGLE CONTENT -->
    <transition name="fade">
        <div
        v-if="activeNotification === 1"
        class="notification-thread"
        >
        
        <p class="thread-title"> Thread: Community Development Program 2024 </p>
        
        <div class="thread-item">
            <div class="thread-header"> REC Committee • requested revision </div>
            <p class="thread-message"> Please update the informed consent forms to include detailed risk disclosures for community participants. </p>
            <small class="thread-time">3 hours ago</small>
        </div>
        
        <div class="thread-item">
            <div class="thread-header"> OVCRIGE Office • forwarded to REC </div>
            <p class="thread-message"> Proposal has been forwarded to the Research Ethics Committee for ethics review. </p>
            <small class="thread-time">2 days ago</small>
        </div>
        
        <div class="thread-item">
            <div class="thread-header">
                RII Staff • recommended approval
            </div>
            <p class="thread-message"> Good potential but requires methodology clarification. Recommended for OVCRIGE review. </p>
            <small class="thread-time">1 week ago</small>
        </div>

        <div class="thread-item">
        <div class="thread-header">
            You • submitted proposal
        </div>
        <p class="thread-message">
            Successfully submitted proposal for review.
        </p>
        <small class="thread-time">2 weeks ago</small>
        </div>
    </div>
</transition>
</div>
</template>

<script>
export default {
  name: "ProponentNotifications",
  data() {
    return {
      activeNotification: null,
      search: "",
    };
  },
  methods: {
    toggleNotification(id) {
      this.activeNotification =
        this.activeNotification === id ? null : id;
    },
  },
};
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
  cursor: pointer;
  margin-bottom: 10px;
}

.notification-header {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.icon.success {
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
</style>
