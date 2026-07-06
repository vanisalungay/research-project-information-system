<template>
  <div class="page">

    <!-- Header -->
    <div class="header">
      <div>
        <h2>Notifications</h2>
        <p class="subtitle">
          Stay updated with proposal submissions, endorsements, reviews, and funding activities.
        </p>
      </div>

      <button class="mark-all-btn" @click="markAllAsRead" :disabled="unreadCount === 0">
        ✓ Mark All as Read
      </button>
    </div>

    <!-- Search -->
    <div class="search-container">
      <input
        v-model="search"
        type="text"
        class="search-input"
        placeholder="🔍 Search proposal title or researcher..."
      />
    </div>

    <!-- Tabs -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span class="count">{{ tab.count }}</span>
      </button>
    </div>

    <!-- Loading -->
    <div v-if="loading" class="empty-state">
      <p>Loading notifications...</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="empty-state">
      <p class="error-text">{{ error }}</p>
      <button class="retry-btn" @click="fetchNotifications">Retry</button>
    </div>

    <!-- Notification List -->
    <div
      v-else-if="filteredNotifications.length"
      class="notification-list"
    >

      <div
        v-for="notification in filteredNotifications"
        :key="notification.id"
        class="notification"
        :class="{ unread: !notification.read }"
        @click="openNotification(notification)"
      >

        <!-- LEFT -->
        <div class="left">

          <div class="icon">
            {{ notification.icon }}
          </div>

          <div class="content">

            <div class="title-row">

              <h4>
                {{ notification.title }}
              </h4>

              <span
                v-if="!notification.read"
                class="dot"
              ></span>

            </div>

            <p class="message">
              {{ notification.message }}
            </p>

            <div class="info-line">
                <span>{{ notification.type }}</span>
                <span class="separator">•</span>
                <span>{{ notification.date }}</span>
            </div>

          </div>

        </div>

        <!-- RIGHT -->

        <div class="controls">

          <button
            v-if="!notification.read"
            class="read-btn"
            @click.stop="markAsRead(notification)"
          >
            ✓
          </button>

          <button
            class="delete-btn"
            @click.stop="dismiss(notification)"
          >
            ✕
          </button>

        </div>

      </div>

    </div>

    <!-- EMPTY STATE -->

    <div
      v-else
      class="empty-state"
    >

      <div class="empty-icon">
        🔔
      </div>

      <h3>No Notifications Found</h3>

      <p>
        Proposal submissions,
        endorsements,
        reviews,
        approvals,
        and funding updates
        will appear here once records are available.
      </p>

    </div>
    
  </div>
</template>

<script>
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'

export default {
  name: "RpsStaffNotification",

  data() {
    return {
      search: "",
      activeTab: "all",
      notifications: [],
      loading: false,
      error: null,
    };
  },

  computed: {
    unreadCount() {
      return this.notifications.filter((n) => !n.read).length;
    },

    tabs() {
      return [
        {
          key: "all",
          label: "All",
          count: this.notifications.length,
        },
        {
          key: "unread",
          label: "Unread",
          count: this.notifications.filter((n) => !n.read).length,
        },
        {
          key: "read",
          label: "Read",
          count: this.notifications.filter((n) => n.read).length,
        },
      ];
    },

    filteredNotifications() {
      let list = [...this.notifications];

      // Filter tabs
      if (this.activeTab === "unread") {
        list = list.filter((n) => !n.read);
      }

      if (this.activeTab === "read") {
        list = list.filter((n) => n.read);
      }

      // Search
      if (this.search.trim() !== "") {
        const keyword = this.search.toLowerCase();

        list = list.filter((n) => {
          return (
            n.title.toLowerCase().includes(keyword) ||
            n.message.toLowerCase().includes(keyword)
          );
        });
      }

      // Latest first
      return list.sort((a, b) => b.id - a.id);
    },
  },

  methods: {
    getIconForType(type) {
      const iconMap = {
        'PROPOSAL_UPDATE': '📄',
        'ENDORSEMENT': '✅',
        'REVIEW_REQUEST': '🔍',
        'APPROVAL': '🎉',
        'REVISION': '🔄',
        'REJECTION': '❌',
        'FUNDING': '💰',
      };
      return iconMap[type] || '🔔';
    },

    formatDate(dateString) {
      if (!dateString) return '';
      const date = new Date(dateString);
      const now = new Date();
      const diffMs = now - date;
      const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
      const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

      if (diffHours < 1) return 'Just now';
      if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
      if (diffDays < 7) return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
      return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
    },

    async fetchNotifications() {
      this.loading = true;
      this.error = null;
      try {
        const userStore = useUserDataStore();
        const userId = userStore.user?.id;
        if (!userId) {
          this.error = 'User not authenticated.';
          return;
        }
        const res = await api.get(`/api/notifications`, { params: { userId } });
        const data = Array.isArray(res.data) ? res.data : [];
        this.notifications = data.map(n => ({
          id: n.id,
          title: n.title || 'Notification',
          message: n.message || '',
          type: n.type || 'GENERAL',
          icon: this.getIconForType(n.type),
          read: n.isRead || false,
          date: this.formatDate(n.createdAt),
          proposalId: n.proposalId,
        }));
      } catch (err) {
        console.error('Failed to fetch notifications:', err);
        this.error = 'Failed to load notifications. Please try again.';
        this.notifications = [];
      } finally {
        this.loading = false;
      }
    },

    openNotification(notification) {
      // Mark as read when opened
      if (!notification.read) {
        this.markAsRead(notification);
      }
      // Navigate to proposal details if linked
      if (notification.proposalId) {
        this.$router.push(`/proposal/${notification.proposalId}`);
      }
    },

    async markAllAsRead() {
      try {
        const userStore = useUserDataStore();
        const userId = userStore.user?.id;
        if (!userId) return;
        await api.put(`/api/notifications/read-all`, null, { params: { userId } });
        this.notifications.forEach(n => { n.read = true; });
      } catch (err) {
        console.error('Failed to mark all as read:', err);
      }
    },

    async markAsRead(notification) {
      try {
        await api.put(`/api/notifications/${notification.id}/read`);
        notification.read = true;
      } catch (err) {
        console.error('Failed to mark as read:', err);
      }
    },

    dismiss(notification) {
      this.notifications = this.notifications.filter(n => n.id !== notification.id);
    },
  },

  mounted() {
    this.fetchNotifications();
  }
};
</script>

<style scoped>
.page {
  padding: 24px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  font-family: Arial, sans-serif;
  background: #f7f8fc;
  min-height: 100vh;
}

/* HEADER */

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.header h2 {
  margin: 0;
  color: #1f2937;
}

.subtitle {
  margin-top: 6px;
  color: #6b7280;
}

.mark-all-btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  transition: .3s;
}

.mark-all-btn:hover:not(:disabled) {
  background: #1d4ed8;
}

.mark-all-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* SEARCH */

.search-container {
  margin: 20px 0;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  border-radius: 10px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  outline: none;
  transition: .3s;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #2563eb;
}

/* TABS */

.tabs {
  display: flex;
  gap: 10px;
  margin-bottom: 22px;
}

.tabs button {
  border: none;
  background: #eef2f7;
  padding: 10px 18px;
  border-radius: 20px;
  cursor: pointer;
  transition: .3s;
  font-weight: 600;
}

.tabs button:hover {
  background: #dbeafe;
}

.tabs button.active {
  background: #2563eb;
  color: white;
}

.count {
  margin-left: 6px;
  font-size: 12px;
  opacity: .9;
}

/* LIST */

.notification-list{
    background:#fff;
    border-radius:12px;
    overflow:hidden;
    border:1px solid #e5e7eb;
}

/* CARD */

.notification{
    display:flex;
    align-items:center;
    justify-content:space-between;
    gap:12px;
    padding:10px 16px;
    margin:0;
    background:#fff;
    border:none;
    border-bottom:1px solid #ececec;
    border-radius:0;
    box-shadow:none;
    cursor:pointer;
    transition:.2s;
}

.notification:last-child{
    border-bottom:none;
}

.notification:hover{
    background:#f8fafc;
}

.notification.unread{
    background:#eef6ff;
}

/* LEFT */
.left{
    display:flex;
    align-items:center;
    gap:10px;
    flex:1;
}

.icon{
    width:34px;
    height:34px;
    min-width:34px;

    display:flex;
    justify-content:center;
    align-items:center;

    border-radius:50%;
    background:#eef2ff;

    font-size:16px;
}

.content{
    flex:1;
    line-height:1.2;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.title-row h4{
    font-size:14px;
    margin:0;
}

.dot {
  width: 10px;
  height: 10px;
  background: #2563eb;
  border-radius: 50%;
}

.message {
  margin-top: 4px;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.4;
}

.time{
    margin-top:2px;
    font-size:11px;
}

/* CONTROLS */

.controls {
  display: flex;
  gap: 6px;
  flex-shrink: 0;
}

.read-btn,
.delete-btn {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: .3s;
  font-size: 14px;
  font-weight: bold;
}

.read-btn {
  background: #dcfce7;
  color: #15803d;
}

.read-btn:hover {
  background: #bbf7d0;
}

.delete-btn {
  background: #fee2e2;
  color: #dc2626;
}

.delete-btn:hover {
  background: #fecaca;
}

/* EMPTY STATE */

.empty-state {
  padding: 45px 20px;
  text-align: center;
}
.empty-icon {
  width: 90px;
  height: 90px;
  margin: auto;
  margin-bottom: 20px;
  border-radius: 50%;
  background: #eef2ff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
}

.empty-state h3 {
  margin: 0;
  color: #374151;
}

.empty-state p {
  margin-top: 12px;
  color: #6b7280;
  max-width: 420px;
  margin-left: auto;
  margin-right: auto;
  line-height: 1.6;
}

.error-text {
  color: #dc2626;
}

.retry-btn {
  margin-top: 12px;
  padding: 8px 20px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.info-line{
    display:flex;
    gap:5px;
    font-size:12px;
    color:#6b7280;
    margin-top:4px;
}

.separator {
  color: #9ca3af;
}

/* RESPONSIVE */

@media (max-width: 768px) {

  .page {
    width: 100%;
    max-width: 100%;
    padding: 20px;
  }

  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 15px;
  }

  .mark-all-btn {
    width: 100%;
  }

  .tabs {
    overflow-x: auto;
    flex-wrap: nowrap;
  }

  .tabs button {
    white-space: nowrap;
  }

  .notification {
    flex-direction: column;
  }

  .left {
    width: 100%;
  }

  .controls {
    width: 100%;
    flex-direction: row;
    justify-content: flex-end;
  }

}
</style>