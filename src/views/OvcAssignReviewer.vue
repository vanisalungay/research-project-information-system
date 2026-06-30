<template>
  <div class="endorse-container">
    <!-- PAGE TITLE -->
    <h2 class="page-title">Endorse Proposal to REC</h2>
    <p class="page-sub">
      Forward this proposal to the Research Evaluation Committee (REC) for evaluation.
    </p>

    <div class="content-row">
      <div class="summary-card">
        <h3>Proposal Endorsement</h3>

        <div class="proposal-info">
          <p>
            This proposal has passed the initial OVCRIGE review and is ready to be
            endorsed to the Research Evaluation Committee (REC).
          </p>

          <div class="info-box">
            <strong>What happens next?</strong>
            <ul>
              <li>REC members will receive the proposal.</li>
              <li>The committee will evaluate the proposal collectively.</li>
            </ul>
          </div>

          <button class="endorse-btn" @click="openConfirmation">
            Endorse to REC
          </button>
        </div>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirmModal" class="modal-backdrop">
      <div class="modal-box">
        <h3>Endorse this proposal to REC?</h3>

        <p>
          This proposal will be forwarded to the Research Evaluation Committee
          for evaluation.
        </p>

        <div class="modal-actions">
          <button class="cancel-btn" @click="showConfirmModal = false">
            Cancel
          </button>

          <button class="confirm-btn" @click="confirmEndorsement">
            Confirm
          </button>
        </div>
      </div>
    </div>

    <!-- SUCCESS TOAST -->
    <div v-if="showSuccessMessage" class="success-toast">
      Proposal successfully endorsed to REC!
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const showConfirmModal = ref(false)
const showSuccessMessage = ref(false)

function openConfirmation() {
  showConfirmModal.value = true
}

function confirmEndorsement() {
  showConfirmModal.value = false
  showSuccessMessage.value = true

  setTimeout(() => {
    showSuccessMessage.value = false
  }, 2000)
}
</script>

<style scoped>
.endorse-container {
  padding: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
}

.page-sub {
  margin-bottom: 25px;
  color: #666;
}

.content-row {
  display: flex;
  justify-content: center;
}

.summary-card {
  width: 100%;
  max-width: 750px;
  background: white;
  border-radius: 12px;
  padding: 30px;
  box-shadow: 0 0 8px #ddd;
}

.summary-card h3 {
  margin-bottom: 20px;
}

.proposal-info p {
  line-height: 1.6;
  color: #444;
}

.info-box {
  margin-top: 20px;
  background: #f8f9fc;
  border-left: 4px solid #ffd803;
  padding: 15px;
  border-radius: 8px;
}

.info-box ul {
  margin-top: 10px;
  padding-left: 20px;
}

.info-box li {
  margin-bottom: 8px;
}

.endorse-btn {
  margin-top: 25px;
  width: 100%;
  padding: 12px;
  border: none;
  background: #ffd803;
  font-weight: bold;
  border-radius: 10px;
  cursor: pointer;
  font-size: 15px;
}

.endorse-btn:hover {
  opacity: 0.95;
}

/* MODAL */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 999;
}

.modal-box {
  background: white;
  width: 380px;
  padding: 25px;
  border-radius: 12px;
  text-align: center;
}

.modal-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

.cancel-btn {
  background: #ddd;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
}

.confirm-btn {
  background: #ffd803;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-weight: bold;
  cursor: pointer;
}

/* SUCCESS TOAST */
.success-toast {
  position: fixed;
  bottom: 25px;
  right: 25px;
  background: #22c55e;
  color: white;
  padding: 12px 20px;
  border-radius: 10px;
  font-weight: bold;
  z-index: 1000;
  box-shadow: 0 0 10px #aaa;
}
</style>