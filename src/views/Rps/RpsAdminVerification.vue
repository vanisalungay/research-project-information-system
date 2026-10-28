<template>
  <div class="page">
    <!-- Profile Card -->
    <section class="card">
      <div class="card-header">
        <div class="left">
          <div class="profile">
            <div class="avatar">DMS</div>
            <div>
              <h2>{{ targetUserFullName }}</h2>
              <p class="role">Faculty</p>
            </div>
          </div>
        </div>

        <!-- STATUS -->
        <span class="status" :class="status">
          {{ status }}
        </span>
      </div>

      <div class="info-grid">
        <div>
          <label>University Email</label>
          <p>{{ targetUser?.email || 'N/A' }}</p>
        </div>
        <div>
          <label>Date Registered</label>
          <p>{{ formatDate(targetUser?.dateRegistered) }}</p>
        </div>
        <div>
          <label>Requested Role</label>
          <p>{{ formatRole(targetUser?.role) }}</p>
        </div>
        <div>
          <label>Department/Office</label>
          <p>{{ targetUser?.departmentOffice || 'N/A' }}</p>
        </div>
      </div>
    </section>

    <!-- Documents -->
    <section class="card">
      <h3>Submitted Documents</h3>

      <!--
        The account record carries no attachments, so this list is genuinely
        empty. It used to render three invented rows (a "Valid ID.pdf", an
        "Employment Certificate.pdf" and a "Research Portfolio.pdf", with
        fabricated file sizes) that appeared on every account and could not be
        downloaded. Showing nothing is the accurate answer.
      -->
      <p v-if="documents.length === 0" class="empty-documents">
        No documents were submitted with this registration.
      </p>

      <div class="document" v-for="doc in documents" :key="doc.name">
        <div>
          <strong>{{ doc.name }}</strong>
          <p>{{ doc.type }} • {{ doc.size }}</p>
        </div>
        <button class="download">⬇ Download</button>
      </div>
    </section>

    <!-- Email Verification -->
    <section class="verification">
      <strong>Email Verification Status</strong>
      <p>{{ verificationLabel }}</p>
    </section>

    <!-- Actions -->
    <section class="actions-card">
      <p class="actions-title">Account Actions</p>

      <div class="actions">
        <button class="btn back" @click="$router.push('/rpsadmin-dash')">← Back</button>
        <button class="btn reject" @click="openModal('reject')">Reject</button>
        <button class="btn approve" @click="openModal('approve')">Approve</button>
      </div>
    </section>

    <!-- Modal -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <h3>{{ modalTitle }}</h3>
        <p>{{ modalMessage }}</p>

        <!-- Reject Reason -->
        <textarea
          v-if="modalType === 'reject'"
          v-model="rejectReason"
          placeholder="Enter reason for rejection..."
          class="reason-box"
        ></textarea>

        <div class="modal-actions">
          <button class="btn cancel" @click="closeModal">Cancel</button>
          <button
            class="btn"
            :class="modalType"
            :disabled="modalType === 'reject' && !rejectReason"
            @click="confirmAction"
          >
            Confirm
          </button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal">
        <h3>Success</h3>
        <p>{{ successMessage }}</p>

        <div class="modal-actions">
          <button class="btn confirm" @click="closeSuccess">OK</button>
        </div>
      </div>
    </div>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const route = useRoute()

const targetUser = ref(null)
const targetUserFullName = ref('')
const status = ref('PENDING')

/** Loads the account the RPS administrator is verifying, from the backend only. */
const fetchUser = async () => {
  try {
    const response = await api.get('/api/users')
    const found = response.data.find((user) => user.id == route.query.id)
    if (found) {
      targetUser.value = found
      targetUserFullName.value = found.name
      status.value = found.status
    }
  } catch (error) {
    // There is no local fallback any more. It used to read the account out of an
    // `offline_users` list in localStorage, so a backend that was down produced a
    // page built from whatever that browser happened to remember instead of the
    // real registration record.
    console.error('Could not load the account to verify:', error)
    await showAlert('The account could not be loaded. Please try again.', {
      type: 'error',
      title: 'Load Failed',
    })
  }
}

onMounted(() => {
  fetchUser()
})

const showSuccess = ref(false)
const successMessage = ref('')

/**
 * Documents attached to a registration.
 *
 * Always empty: the account record has no attachments. This used to be a
 * hardcoded list of three invented documents with fabricated file sizes, which
 * rendered on every account and could never be downloaded.
 */
const documents = []

/** Human-readable registration date, or "N/A" when the backend has none. */
const formatDate = (value) => {
  if (!value) return 'N/A'
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime())
    ? 'N/A'
    : parsed.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
}

/**
 * Human-readable role label, so an operator never reads a raw role value. The Dean is
 * an independent role (not an RPS sub-role), and it is displayed as "Dean".
 */
const formatRole = (role) => {
  const map = {
    PROPONENT: 'Proponent',
    DEAN: 'Dean',
    RPS_ADMIN: 'RPS Admin',
    RPS_STAFF: 'RPS Staff',
    OVCRIGE: 'OVCRIGE',
    OVCAF: 'OVCAF',
    REC: 'REC',
    OC: 'Chancellor',
  }
  return map[role] || role || 'N/A'
}

/** Email verification state taken from the account, never a fixed sentence. */
const verificationLabel = computed(() => {
  if (!targetUser.value) return 'Unknown'
  return targetUser.value.emailVerified
    ? 'Email address verified.'
    : 'Email address not verified yet.'
})

const showModal = ref(false)
const modalType = ref(null)
const rejectReason = ref('')

const openModal = (type) => {
  modalType.value = type
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  modalType.value = null
  rejectReason.value = ''
}

const modalTitle = computed(() =>
  modalType.value === 'approve' ? 'Approve Account' : 'Reject Account',
)

const modalMessage = computed(() =>
  modalType.value === 'approve'
    ? 'Are you sure you want to approve this account?'
    : 'Please provide a reason for rejecting this account.',
)

const confirmAction = async () => {
  if (!targetUser.value) return

  try {
    if (modalType.value === 'approve') {
      const response = await api.put(`/api/users/${targetUser.value.id}/approve`)
      status.value = response.data.status
      successMessage.value = 'Account has been successfully approved.'
    } else {
      const response = await api.put(`/api/users/${targetUser.value.id}/reject`)
      status.value = response.data.status
      successMessage.value = 'Account has been successfully rejected.'
    }
    closeModal()
    showSuccess.value = true
  } catch (error) {
    // The decision is reported as failed. It used to be written into the
    // `offline_users` list in localStorage instead, so the administrator saw
    // "Account has been successfully approved." while the real account stayed
    // pending in the database - the opposite of what the screen claimed.
    console.error('Could not update the account status:', error)
    closeModal()
    await showAlert(
      'The account status could not be updated. Please check your connection and try again.',
      { type: 'error', title: 'Update Failed' },
    )
  }
}

const closeSuccess = () => {
  showSuccess.value = false
}
</script>

<style scoped>
.page {
  font-family: 'Inter', sans-serif;
  background: #f4f6fb;
  min-height: 100vh;
  width: 500%;
  max-width: 135%;
}

.card,
.actions-card {
  background: white;
  margin: 24px;
  padding: 20px;
  border-radius: 12px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.profile {
  display: flex;
  gap: 12px;
}

.avatar {
  width: 48px;
  height: 48px;
  background: #2b6ef3;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-weight: bold;
}

.status {
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 14px;
}

.status.Pending {
  background: #fff3cd;
  color: #856404;
}

.status.Approved {
  background: #d1fae5;
  color: #065f46;
}

.status.Rejected {
  background: #fee2e2;
  color: #991b1b;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin-top: 16px;
}

.document {
  background: #f9fafc;
  padding: 14px;
  border-radius: 8px;
  display: flex;
  justify-content: space-between;
  margin-top: 12px;
}

/* Legitimate empty state: the account record carries no attachments. */
.empty-documents {
  margin-top: 12px;
  color: #6b7280;
  font-size: 14px;
}

.verification {
  background: #eaf4ff;
  margin: 24px;
  padding: 16px;
  border-left: 4px solid #2b6ef3;
}

.actions {
  display: flex;
  gap: 16px;
}

.btn {
  flex: 1;
  padding: 14px;
  border-radius: 10px;
  font-weight: bold;
  border: none;
  cursor: pointer;
}

.reject {
  background: #e00000;
  color: white;
}

.approve {
  background: #0aa63e;
  color: white;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 360px;
  text-align: center;
}

.reason-box {
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
  font-family: inherit;
  font-size: 14px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.cancel {
  background: #ddd;
  color: #1f2937;
}
</style>
