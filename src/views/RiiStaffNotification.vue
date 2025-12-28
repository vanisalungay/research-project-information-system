<template>
  <div class="page">
    <!-- Header -->
    <h2>Notifications</h2>
    <p class="subtitle">You have {{ unreadCount }} unread notifications</p>

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
          <div class="box">
            <strong>Proposal:</strong> {{ notification.proposal }}
            <span v-if="notification.amount">
              <br />
              <strong>Amount:</strong>
              ₱{{ notification.amount.toLocaleString() }}
            </span>
          </div>

          <small>{{ notification.date }}</small>
        </div>

        <!-- ACTION BUTTONS (ADDED) -->
        <div class="actions">
          <button
            v-if="notification.type === 'review'"
            class="action-btn"
            @click="$router.push('fundviewprop')"
          >
            Review Proposal
          </button>

          <button
            v-if="notification.type === 'endorse'"
            class="action-btn"
            @click="$router.push('rii-endorse')"
          >
            Review & Endorse
          </button>

          <button
            v-if="notification.type === 'release'"
            class="action-btn"
            @click="$router.push('fundrelease')"
          >
            Release Funds
          </button>
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
  name: 'Notifications',

  data() {
    return {
      activeTab: 'all',
      notifications: [
        {
          id: 1,
          type: 'review',
          title: 'New Proposal Submitted',
          message:
            'Educational Infrastructure Project has been submitted and requires your review.',
          proposal: 'Educational Infrastructure Project',
          date: '2024-12-14 09:30 AM',
          icon: '📄',
          read: false,
        },
        {
          id: 2,
          type: 'endorse',
          title: 'Endorsement Required',
          message:
            'Community Development Program 2024 has been reviewed by REC and is awaiting your endorsement decision.',
          proposal: 'Community Development Program 2024',
          date: '2024-12-14 08:15 AM',
          icon: '🟡',
          read: false,
        },
        {
          id: 3,
          type: 'release',
          title: 'Funding Approved by OVCF',
          message: 'Agricultural Research Program has received funding approval from OVCF.',
          proposal: 'Agricultural Research Program',
          amount: 200000,
          date: '2024-12-13 02:45 PM',
          icon: '💲',
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

    // BUTTON HANDLERS (ADDED)
    reviewProposal(notification) {
      notification.read = true
      console.log('Review Proposal:', notification.proposal)
    },

    reviewAndEndorse(notification) {
      notification.read = true
      console.log('Review & Endorse:', notification.proposal)
    },

    releaseFunds(notification) {
      notification.read = true
      console.log('Release Funds:', notification.proposal)
    },
  },
}
</script>

<style scoped>
.page {
  width: 500%;
  max-width: 135%; /* prevents it from becoming too wide on big screens */
  padding: 24px 40px; /* reduce side whitespace */
  font-family: Arial, sans-serif;
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
  background: #60a5fa;
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
