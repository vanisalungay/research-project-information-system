<template>
  <div class="page">
    <!-- Header -->
    <h2>Notifications</h2>
    <p class="subtitle">
      You have {{ unreadCount }} unread notification{{ unreadCount !== 1 ? 's' : '' }}
    </p>

    <!-- Tabs -->
    <div class="tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }} ({{ tab.count }})
      </button>
    </div>

    <!-- Notification List -->
    <div
      v-for="notification in filteredNotifications"
      :key="notification.id"
      class="notification"
      :class="{ unread: !notification.read }"
    >
      <!-- Icon -->
      <div class="icon">{{ notification.icon }}</div>

      <!-- Content -->
      <div>
        <h4>
          {{ notification.title }}
          <span v-if="!notification.read" class="dot">•</span>
        </h4>

        <p>{{ notification.message }}</p>

        <div class="meta">
          <div class="box"><strong>Category:</strong> {{ notification.category }}</div>

          <small>{{ notification.date }}</small>
        </div>
      </div>

      <!-- Controls -->
      <div class="controls">
        <button @click="markAsRead(notification)">✓</button>
        <button @click="dismiss(notification)">✕</button>
      </div>
    </div>

    <div v-if="filteredNotifications.length === 0" class="empty">No notifications to show.</div>
  </div>
</template>

<script>
export default {
 name: 'RpsAdminNotification',

  data() {
    return {
      activeTab: 'all',
      notifications: [
        {
          id: 1,
          type: 'account',
          title: 'Pending Account Approvals',
          message:
            'You have new user registrations awaiting approval. Review pending accounts to approve or reject access.',
          category: 'User Account Requests',
          date: '2024-12-15 10:30 AM',
          icon: '👤',
          read: false,
        },
        {
          id: 2,
          type: 'account',
          title: 'New User Registration',
          message:
            'Dr. Maria Santos has submitted a request for a Faculty account and is pending your approval.',
          category: 'Faculty Account',
          date: '2024-12-15 09:10 AM',
          icon: '📝',
          read: false,
        },
        {
          id: 3,
          type: 'account',
          title: 'Account Approval Required',
          message:
            'Multiple user accounts are still pending review. Please take action to avoid delays.',
          category: 'Pending Accounts',
          date: '2024-12-14 04:45 PM',
          icon: '⚠️',
          read: true,
        },
      ],
    }
  },

  computed: {
    unreadCount() {
      return this.notifications.filter((n) => !n.read).length
    },

    tabs() {
      return [
        { key: 'all', label: 'All', count: this.notifications.length },
        { key: 'unread', label: 'Unread', count: this.unreadCount },
        {
          key: 'read',
          label: 'Read',
          count: this.notifications.filter((n) => n.read).length,
        },
      ]
    },

    filteredNotifications() {
      if (this.activeTab === 'unread') {
        return this.notifications.filter((n) => !n.read)
      }
      if (this.activeTab === 'read') {
        return this.notifications.filter((n) => n.read)
      }
      return this.notifications
    },
  },

  methods: {
    markAsRead(notification) {
      notification.read = true
    },

    dismiss(notification) {
      this.notifications = this.notifications.filter((n) => n.id !== notification.id)
    },

    goToPendingAccounts(notification) {
      notification.read = true
      this.$router.push('/rps-admin/pending-accounts')
    },
  },
}
</script>

<style scoped>
.page {
  padding: 24px;
  font-family: Arial, sans-serif;
  width: 500%;
  max-width: 135%;
}

.subtitle {
  color: #666;
  margin-bottom: 12px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.tabs button {
  padding: 6px 12px;
  border-radius: 20px;
  border: 1px solid #ddd;
  background: #f9fafb;
  cursor: pointer;
}

.tabs .active {
  background: #4f46e5;
  color: white;
}

.notification {
  display: flex;
  gap: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
}

.notification.unread {
  border-color: #93c5fd;
  background: #f0f7ff;
}

.icon {
  font-size: 20px;
}

.content {
  flex: 1;
}

.dot {
  color: #3b82f6;
}

.meta {
  margin-top: 6px;
}

.box {
  background: #f9fafb;
  padding: 6px;
  border-radius: 6px;
  font-size: 13px;
  margin-bottom: 4px;
}

.actions {
  margin-top: 8px;
}

.action-btn {
  background: #4f46e5;
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.controls button {
  border: none;
  background: none;
  cursor: pointer;
}

.empty {
  text-align: center;
  color: #777;
  margin-top: 20px;
}
</style>
