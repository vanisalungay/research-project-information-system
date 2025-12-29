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
          <p>maria.santos@university.edu</p>
        </div>
        <div>
          <label>Date Registered</label>
          <p>Dec 15, 2024</p>
        </div>
        <div>
          <label>Requested Role</label>
          <p>Faculty</p>
        </div>
        <div>
          <label>Department/Office</label>
          <p>College of Engineering</p>
        </div>
      </div>
    </section>

    <!-- Documents -->
    <section class="card">
      <h3>Submitted Documents</h3>

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
      <p>Email verified on Dec 15, 2024</p>
    </section>

    <!-- Actions -->
    <section class="actions-card">
      <p class="actions-title">Account Actions</p>

      <div class="actions">
        <button class="btn back" @click="$router.push('/riiadmin-dash')">← Back</button>
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
        <textarea v-if="modalType === 'reject'" v-model="rejectReason" placeholder="Enter reason for rejection..."
          class="reason-box"></textarea>

        <div class="modal-actions">
          <button class="btn cancel" @click="closeModal">Cancel</button>
          <button class="btn" :class="modalType" :disabled="modalType === 'reject' && !rejectReason"
            @click="confirmAction">
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
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const allUsers = JSON.parse(localStorage.getItem('riiAdminUsers'))
console.log('allUsers', allUsers)

const route = useRoute()

const targetUser = allUsers.find((user) => user.id == route.query.id)
console.log('targetUser', targetUser)

const targetUserFullName = ref(targetUser.name)

const showSuccess = ref(false)
const successMessage = ref('')

const documents = [
  { name: 'Valid ID.pdf', type: 'ID Document', size: '1.2 MB' },
  { name: 'Employment Certificate.pdf', type: 'Employment Proof', size: '856 KB' },
  { name: 'Research Portfolio.pdf', type: 'Supporting Document', size: '3.4 MB' },
]

const status = ref('Pending')

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

const confirmAction = () => {
  console.log('cofnirm', modalType.value)
  for (const user of allUsers) {
    if (user.id == targetUser.id) {
      if (modalType.value === 'approve') {
        successMessage.value = 'Account have been successfully approved.'
        console.log('Account Approved')
        user.status = 'Approved'
      } else {
        successMessage.value = 'Account has been successfully rejected.'
        console.log('Account Rejected')
        user.status = 'Rejected'
      }
      break
    }
  }
  // update local storage
  localStorage.setItem('riiAdminUsers', JSON.stringify(allUsers))

  status.value = modalType.value === 'approve' ? 'Approved' : 'Rejected'
  closeModal()
  showSuccess.value = true
}

const closeSuccess = () => {
  showSuccess.value = false
}
</script>

<style scoped>
.page {
  font-family: Arial, sans-serif;
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
}
</style>
