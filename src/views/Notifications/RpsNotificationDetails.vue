<template>
  <div class="page">

    <!-- Header -->
    <div class="header">

      <button class="back-btn" @click="$router.back()">
        ← Back
      </button>

      <div>
        <h2>Notification Details</h2>
        <p>
          View complete notification information and perform the required action.
        </p>
      </div>

    </div>

    <!-- Notification Card -->
    <div class="card">

      <div class="card-header">

        <div class="icon">
          {{ notification.icon }}
        </div>

        <div class="title">

          <h3>
            {{ notification.title }}
          </h3>

          <small>
            {{ notification.date }}
          </small>

        </div>

      </div>

      <div class="divider"></div>

      <div class="section">

        <label>Notification</label>

        <p>
          {{ notification.message }}
        </p>

      </div>

      <div class="section">

        <label>Proposal Title</label>

        <p>
          {{ notification.proposal }}
        </p>

      </div>

      <div class="section">

        <label>Submitted By</label>

        <p>
          {{ notification.researcher }}
        </p>

      </div>

      <div class="section">

        <label>Status</label>

        <span
          class="status"
          :class="notification.statusClass"
        >
          {{ notification.status }}
        </span>

      </div>

            <div class="divider"></div>

      <!-- Action Buttons -->
      <div class="actions">

        <button
          v-if="notification.type === 'review'"
          class="primary-btn"
          @click="viewProposal"
        >
          View Proposal
        </button>

        <button
          v-if="notification.type === 'endorse'"
          class="primary-btn"
          @click="reviewProposal"
        >
          Review & Endorse
        </button>

        <button
          v-if="notification.type === 'release'"
          class="primary-btn"
          @click="releaseFunds"
        >
          Release Funds
        </button>

        <button
          class="secondary-btn"
          @click="markAsRead"
        >
          Mark as Read
        </button>

      </div>

    </div>

    <ConfirmDialog
      v-if="dialog.show"
      :type="dialog.type"
      :variant="dialog.variant"
      :title="dialog.title"
      :message="dialog.message"
      :confirmText="dialog.confirmText"
      @confirm="dialog.onConfirm"
      @cancel="dialog.onCancel"
      @close="dialog.show = false"
    />
  </div>
</template>

<script>
import ConfirmDialog from '@/components/ConfirmDialog.vue'

export default {
  name: "RpsNotificationDetails",
  components: { ConfirmDialog },

  data() {
    return {
      notification: {},
      dialog: {
        show: false, type: 'info', variant: 'alert', title: '', message: '',
        confirmText: 'OK', cancelText: 'Cancel',
        onConfirm: () => {}, onCancel: () => {},
      },
    };
  },

    methods: {
    _showAlert(message, options = {}) {
      return new Promise((resolve) => {
        this.dialog.type = options.type || 'info'
        this.dialog.variant = 'alert'
        this.dialog.title = options.title || 'Notice'
        this.dialog.message = message
        this.dialog.confirmText = options.confirmText || 'OK'
        this.dialog.onConfirm = () => { this.dialog.show = false; resolve() }
        this.dialog.onCancel = () => { this.dialog.show = false; resolve() }
        this.dialog.show = true
      })
    },

    viewProposal() {
      this.$router.push(`/proposal/${this.notification.proposalId}`);
    },

    reviewProposal() {
      this.$router.push('/rii-endorse');
    },

    releaseFunds() {
      this.$router.push('/fundrelease');
    },

    async markAsRead() {
      await this._showAlert("Notification marked as read.", { type: 'success', title: 'Marked as Read' });
    },

  },

};
</script>

<style scoped>
.page{
  width:500%;
  max-width:135%;
  padding:24px;
  font-family:Arial,sans-serif;
  background:#f7f8fc;
  box-sizing:border-box;
}

.header{
  display:flex;
  align-items:center;
  gap:16px;
  margin-bottom:20px;
}

.header h2{
  margin:0;
  color:#1f2937;
}

.header p{
  margin-top:4px;
  color:#6b7280;
  font-size:14px;
}

.back-btn{
  background:#fff;
  border:1px solid #d1d5db;
  padding:8px 16px;
  border-radius:8px;
  cursor:pointer;
  font-weight:600;
  transition:.2s;
}

.back-btn:hover{
  background:#f3f4f6;
}

.card{
  background:#fff;
  border-radius:12px;
  padding:24px;
  box-shadow:0 2px 8px rgba(0,0,0,.05);
}

.card-header{
  display:flex;
  align-items:center;
  gap:16px;
}

.icon{
  width:60px;
  height:60px;
  border-radius:50%;
  background:#eef2ff;
  display:flex;
  justify-content:center;
  align-items:center;
  font-size:28px;
}

.title h3{
  margin:0;
  color:#111827;
}

.title small{
  color:#6b7280;
}

.divider{
  margin:20px 0;
  border-top:1px solid #e5e7eb;
}

.section{
  margin-bottom:18px;
}

.section label{
  display:block;
  font-size:13px;
  font-weight:700;
  color:#6b7280;
  margin-bottom:6px;
}

.section p{
  margin:0;
  color:#1f2937;
  line-height:1.6;
}

.status{
  display:inline-block;
  padding:6px 14px;
  border-radius:20px;
  font-size:12px;
  font-weight:700;
}

.pending{
  background:#fef3c7;
  color:#92400e;
}

.review{
  background:#dbeafe;
  color:#1d4ed8;
}

.approved{
  background:#dcfce7;
  color:#166534;
}

.returned{
  background:#fee2e2;
  color:#b91c1c;
}

.actions{
  display:flex;
  gap:12px;
  margin-top:28px;
}

.primary-btn{
  background:#1e40af;
  color:#fff;
  border:none;
  border-radius:8px;
  padding:10px 18px;
  font-size:14px;
  font-weight:600;
  cursor:pointer;
  transition:.2s;
}

.primary-btn:hover{
  background:#1d4ed8;
}

.secondary-btn{
  background:#fff;
  color:#374151;
  border:1px solid #d1d5db;
  border-radius:8px;
  padding:10px 18px;
  font-size:14px;
  font-weight:600;
  cursor:pointer;
  transition:.2s;
}

.secondary-btn:hover{
  background:#f3f4f6;
}

@media (max-width:768px){

  .page{
    width:100%;
    max-width:100%;
    padding:16px;
  }

  .header{
    flex-direction:column;
    align-items:flex-start;
  }

  .actions{
    flex-direction:column;
  }

  .primary-btn,
  .secondary-btn{
    width:100%;
  }

}
</style>