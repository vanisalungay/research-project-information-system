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

      <button class="mark-all-btn" @click="markAllAsRead">
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

    <!-- Notification List -->
    <div
      v-if="filteredNotifications.length"
      class="notification-list"
    >

      <div
        v-for="notification in filteredNotifications"
        :key="notification.id"
        class="notification"
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
                <span>{{ notification.proposal }}</span>
                <span class="separator">•</span>
                <span>{{ notification.researcher }}</span>
            </div>

            <small class="time">
              {{ notification.date }}
            </small>

            <!-- ACTION BUTTONS -->

            <div class="actions">

              <button
                v-if="notification.type==='review'"
                class="action-btn"
                @click.stop="$router.push('fundviewprop')"
              >
                Review Proposal
              </button>

              <button
                v-if="notification.type==='endorse'"
                class="action-btn"
                @click.stop="$router.push('rii-endorse')"
              >
                Review & Endorse
              </button>

              <button
                v-if="notification.type==='release'"
                class="action-btn"
                @click.stop="$router.push('fundrelease')"
              >
                Release Funds
              </button>

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
export default {
  name: "Notifications",

  data() {
    return {
      search: "",
      activeTab: "all",

      notifications: [],
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
            n.proposal.toLowerCase().includes(keyword) ||
            n.researcher.toLowerCase().includes(keyword)
          );
        });
      }

      // Latest first
      return list.sort((a, b) => b.id - a.id);
    },
  },

  methods: {

    openNotification(notification) {
      this.$router.push(`/notification/${notification.id}`);
    },
  
    markAllAsRead() {
      this.notifications.forEach(notification => {
        notification.read = true;
      });
    },

    markAsRead(notification) {
      notification.read = true;
    },

    dismiss(notification) {
      this.notifications =
        this.notifications.filter(n => n.id !== notification.id);
    },

  }
};
</script>

<style scoped>
.page {
  padding: 24px;
  width: 500%;
  max-width: 135%;
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

.mark-all-btn:hover {
  background: #1d4ed8;
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
  display: none;
}

.info-box {
  background: transparent;
  padding: 0;
  margin-top: 2px;
}

.info-box p {
  margin: 1px 0;
  font-size: 12px;
  color: #6b7280;
}

.time{
    margin-top:2px;
    font-size:11px;
}
/* ACTION BUTTONS */

.actions {
  display: none;
}

.action-btn {
  background: #2563eb;
  color: white;
  border: none;
  padding: 9px 16px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: .3s;
}

.action-btn:hover {
  background: #1d4ed8;
}

/* CONTROLS */

.controls {
  display: none;
}

.read-btn,
.delete-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: .3s;
  font-size: 16px;
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

/* SCROLLBAR */

.notification-list::-webkit-scrollbar {
  width: 8px;
}

.notification-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 20px;
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

  .actions {
    flex-direction: column;
  }

  .action-btn {
    width: 100%;
  }

}
.info-line{
    display:flex;
    gap:5px;
    font-size:12px;
    color:#6b7280;
    margin-top:2px;
}

.separator {
  color: #9ca3af;
}

</style>