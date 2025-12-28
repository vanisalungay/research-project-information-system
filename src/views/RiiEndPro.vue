<template>
  <div class="page">
    <!-- Header -->
    <div class="header">
      <button class="back" @click="$router.push('fundviewprop')">← Back to Proposal</button>
      <h2>Endorse Proposal</h2>
      <p class="subtitle">Review and confirm proposal endorsement</p>
    </div>

    <div class="layout">
      <!-- LEFT -->
      <div class="left">
        <!-- Proposal Summary -->
        <section class="card">
          <h3>Proposal Summary</h3>

          <div class="grid">
            <div>
              <label>Title</label>
              <p>Community Development Program 2024</p>
            </div>
            <div>
              <label>Proponent</label>
              <p>Dr. Alien Shippy</p>
            </div>
            <div>
              <label>Category</label>
              <p>Kalikasan Program</p>
            </div>
            <div>
              <label>Budget</label>
              <p>₱150,000</p>
            </div>
            <div>
              <label>Duration</label>
              <p>12 months</p>
            </div>
            <div>
              <label>Submitted</label>
              <p>2024-12-10</p>
            </div>
          </div>

          <div class="info success">
            ✔ By endorsing this proposal, you confirm that it meets all RII requirements and
            recommend it for funding approval.
          </div>
        </section>

        <!-- Remarks -->
        <section class="card">
          <h3>Endorsement Remarks</h3>
          <textarea
            v-model="remarks"
            placeholder="Add any additional comments or conditions for this endorsement..."
          ></textarea>
        </section>

        <!-- Review Info -->
        <section class="card">
          <h3>Review Information</h3>
          <p><strong>OVCRIGE Status:</strong> <span class="tag">Endorsed</span></p>
          <p><strong>Reviewer Score:</strong> 85/100</p>
          <p><strong>Recommendation:</strong> Strongly Recommended</p>
        </section>
      </div>

      <!-- RIGHT -->
      <div class="right">
        <!-- Checklist -->
        <section class="card">
          <h3>Endorsement Checklist</h3>

          <div class="checklist">
            <label class="check" v-for="(value, key) in checklist" :key="key">
              <input type="checkbox" v-model="checklist[key]" />
              {{ checklistLabels[key] }}
            </label>
          </div>

          <button class="btn endorse" :disabled="!allChecked" @click="openModal('endorse')">
            ✔ Endorse Proposal
          </button>

          <button class="btn return" @click="$router.push('rii-return4rev')">
            ⟳ Return for Revision
          </button>

          <p class="hint">Once endorsed, this proposal will move to the funding approval stage.</p>
        </section>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showModal" class="modal-overlay">
      <div class="modal">
        <h3>{{ modalTitle }}</h3>
        <p v-if="modalType === 'return'">Please provide a reason for returning this proposal.</p>

        <textarea
          v-if="modalType === 'return'"
          v-model="reason"
          placeholder="Enter reason..."
        ></textarea>

        <div class="modal-actions">
          <button class="btn cancel" @click="closeModal">Cancel</button>

          <button
            class="btn confirm"
            :class="modalType === 'return' ? 'danger' : 'success'"
            :disabled="modalType === 'return' && !reason"
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
          <button class="btn confirm" @click="showSuccess = false">OK</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const remarks = ref('')
const reason = ref('')

const checklist = ref({
  docs: false,
  budget: false,
  feasible: false,
  criteria: false,
  ready: false,
})

const checklistLabels = {
  docs: 'All proposal documents reviewed',
  budget: 'Budget is reasonable and justified',
  feasible: 'Implementation plan is feasible',
  criteria: 'Meets RII funding criteria',
  ready: 'Ready for funding approval',
}

const allChecked = computed(() => Object.values(checklist.value).every(Boolean))

const showModal = ref(false)
const showSuccess = ref(false)
const modalType = ref('')
const successMessage = ref('')

const openModal = (type) => {
  modalType.value = type
  showModal.value = true
}

const closeModal = () => {
  showModal.value = false
}

const modalTitle = computed(() =>
  modalType.value === 'endorse' ? 'Confirm Endorsement' : 'Return Proposal for Revision',
)

const confirmAction = () => {
  showModal.value = false
  successMessage.value =
    modalType.value === 'endorse'
      ? 'Proposal has been successfully endorsed.'
      : 'Proposal has been returned for revision.'
  showSuccess.value = true
}
</script>

<style scoped>
.page {
  background: #f6f8fb;
  min-height: 100vh;
  padding: 24px;
  font-family: Arial, sans-serif;
  width: 135%;
}

.back {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
}

.header h2 {
  margin: 8px 0;
}

.layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

label {
  font-size: 12px;
  color: #6b7280;
}

.info.success {
  margin-top: 16px;
  background: #ecfdf5;
  padding: 12px;
  border-radius: 8px;
  color: #065f46;
}

textarea {
  width: 100%;
  min-height: 100px;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #ccc;
}

.checklist {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 16px;
}

.check {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
}

.check input[type='checkbox'] {
  width: 16px;
  height: 16px;
  accent-color: #facc15; /* yellow highlight */
  cursor: pointer;
}

.btn {
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  margin-top: 10px;
}

.endorse {
  background: #facc15;
  color: #1f2937;
  font-weight: bold;
}
.endorse:disabled {
  background: #fef3c7;
  cursor: not-allowed;
}

.return {
  background: #fee2e2;
  color: #b91c1c;
  font-weight: bold;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  justify-content: center;
  align-items: center;
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
  margin-top: 16px;
}

.confirm {
  background: #16a34a;
  color: white;
}
.confirm.danger {
  background: #b91c1c;
}
.cancel {
  background: #e5e7eb;
}
.tag {
  color: #16a34a;
  font-weight: bold;
}
.hint {
  font-size: 12px;
  color: #6b7280;
  margin-top: 10px;
}
</style>
