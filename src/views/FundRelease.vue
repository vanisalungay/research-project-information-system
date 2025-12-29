<template>
  <div class="release-page">
    <!-- HEADER -->
    <div class="header">
      <button class="back" @click="$router.push('fundviewprop')">← Back to Proposal</button>
      <h2>Release Funds</h2>
      <p class="subtitle">Process fund release for approved proposal</p>
    </div>

    <div class="layout">
      <!-- LEFT COLUMN -->
      <div class="left">
        <!-- Proposal Summary -->
        <div class="card">
          <h3>Proposal Summary</h3>

          <div class="summary-grid">
            <div>
              <label class="highlight-label">Title</label>
              <p>Educational Infrastructure Project</p>
            </div>
            <div>
              <label class="highlight-label">Proponent</label>
              <p>Dr. Cat Moon</p>
            </div>
            <div>
              <label class="highlight-label">Category</label>
              <p>Tanglaw Program</p>
            </div>
            <div>
              <label class="highlight-label">Endorsed Date</label>
              <p>2024-12-09</p>
            </div>
          </div>

          <div class="budget-boxes">
            <div class="budget total">
              <span>Total Budget</span>
              <strong>₱180,000</strong>
            </div>
            <div class="budget released">
              <span>Released</span>
              <strong>₱0</strong>
            </div>
            <div class="budget remaining">
              <span>Remaining</span>
              <strong>₱180,000</strong>
            </div>
          </div>
        </div>

        <!-- Fund Release Details -->
        <div class="card">
          <h3>Fund Release Details</h3>

          <label class="highlight-label">Amount to Release (₱)</label>
          <input type="number" v-model="amount" placeholder="0.00" />

          <div class="hint">Maximum available: ₱180,000</div>

          <label class="highlight-label">Release Date</label>
          <input type="date" v-model="releaseDate" />

          <label class="highlight-label">Remarks / Notes</label>
          <textarea placeholder="Add any remarks or conditions for this fund release..."></textarea>
        </div>

        <!-- Release Summary -->
        <div class="card">
          <h3>Release Summary</h3>

          <div class="summary-row">
            <span>Amount to Release</span>
            <strong>₱{{ amount || '0.00' }}</strong>
          </div>
          <div class="summary-row">
            <span>Previously Released</span>
            <strong>₱0</strong>
          </div>
          <div class="summary-row">
            <span>Remaining After Release</span>
            <strong>₱180,000</strong>
          </div>
        </div>
      </div>

      <!-- RIGHT COLUMN -->
      <div class="right">
        <div class="card checklist">
          <h3>Release Checklist</h3>

          <label class="check">
            <input type="checkbox" v-model="checks.endorsed" />
            Proposal has been endorsed by RII
          </label>

          <label class="check">
            <input type="checkbox" v-model="checks.budget" />
            Amount is within approved budget
          </label>

          <label class="check">
            <input type="checkbox" v-model="checks.complete" />
            All documentation is complete
          </label>

          <label class="check">
            <input type="checkbox" v-model="checks.verified" />
            Proponent information verified
          </label>

          <button class="btn release" :disabled="!canRelease" @click="openModal('release')">
            Release Funds
          </button>

          <button class="btn notify" @click="openModal('notify')">✈ Notify Proponent</button>

          <div class="note">
            Once released, funds will be transferred to the proponent’s account within 3–5 business
            days.
          </div>
        </div>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <h3>{{ modalTitle }}</h3>
        <p>{{ modalMessage }}</p>

        <div class="modal-actions">
          <button class="btn cancel" @click="closeModal">Cancel</button>
          <button class="btn confirm" @click="confirmAction">Confirm</button>
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

const amount = ref('')
const releaseDate = ref('')
const showSuccess = ref(false)
const successMessage = ref('')

const checks = ref({
  endorsed: false,
  budget: false,
  complete: false,
  verified: false,
})

const canRelease = computed(() => Object.values(checks.value).every(Boolean))

const showModal = ref(false)
const modalType = ref(null)

const openModal = (type) => {
  modalType.value = type
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
  modalType.value = null
}

const modalTitle = computed(() =>
  modalType.value === 'release' ? 'Confirm Fund Release' : 'Notify Faculty',
)

const modalMessage = computed(() =>
  modalType.value === 'release'
    ? 'Are you sure you want to release these funds? This action cannot be undone.'
    : 'Are you sure you want to notify the faculty about this release?',
)

const confirmAction = () => {
  if (modalType.value === 'release') {
    successMessage.value = 'Funds have been successfully released.'
  } else {
    successMessage.value = 'Faculty has been successfully notified.'
  }
  closeModal()
  showSuccess.value = true
}

const closeSuccess = () => {
  showSuccess.value = false
}
</script>

<style scoped>
.release-page {
  padding: 24px;
  background: #f6f7fb;
  font-family: Arial, sans-serif;
  width: 135%;
}

.header h2 {
  margin: 10px 0 4px;
}

.subtitle {
  color: #6b7280;
}

.back {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
}

/* LAYOUT */
.layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

/* CARDS */
.card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.highlight-label {
  font-weight: bold;
  color: black;
}

.budget-boxes {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}

.budget {
  flex: 1;
  padding: 14px;
  border-radius: 10px;
  background: #f9fafb;
  color: #000; 
}

input,
textarea {
  width: 100%;
  padding: 10px;
  margin: 6px 0 14px;
  border-radius: 8px;
  border: 1px solid #d1d5db;
}

.hint {
  font-size: 12px;
  color: #2563eb;
  margin-bottom: 10px;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  margin: 8px 0;
}

/* CHECKLIST */
.checklist .check {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 14px;
  color: black;
}

.checklist input {
  width: 16px;
  height: 16px;
}

.btn {
  width: 100%;
  padding: 12px;
  border-radius: 10px;
  border: none;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
}

.release {
  background: #facc15;
  color: #000;
}

.release:disabled {
  background: #fde68a;
  cursor: not-allowed;
}

.notify {
  background: white;
  border: 1px solid #2563eb;
  color: #2563eb;
}

.note {
  font-size: 12px;
  color: #15803d;
  margin-top: 12px;
}

/* MODAL */
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

.modal-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.cancel {
  background: #e5e7eb;
}

.confirm {
  background: #2563eb;
  color: white;
}
</style>
