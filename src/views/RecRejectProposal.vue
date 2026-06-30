<template>
  <div class="reject-wrapper">
    <!-- BACK BUTTON -->
    <button class="back-btn" @click="goToSubmission">← Back to Submission</button>

    <!-- HEADER -->
    <div class="page-header">
      <h2>Reject Proposal</h2>
      <span class="badge-danger">⚠️ Permanent Action</span>
    </div>

    <p class="subtitle">Provide clear justification for proposal rejection</p>

    <div class="layout-grid">
      <!-- LEFT SECTION -->
      <div class="left-col">
        <!-- PROPOSAL INFORMATION -->
        <div class="card">
          <h3 class="card-title">Proposal Information</h3>

          <div class="info-grid">
            <div class="info-box">
              <label>Title</label>
              <p>Community Development Program</p>
            </div>

            <div class="info-box">
              <label>Proponent</label>
              <p>Dr. Allen Shiopy</p>
            </div>

            <div class="info-box">
              <label>Type</label>
              <p>Research</p>
            </div>

            <div class="info-box">
              <label>Program ID</label>
              <p>43</p>
            </div>

            <div class="info-box">
              <label>Category</label>
              <p>Extension Program</p>
            </div>
          </div>
        </div>

        <!-- REJECTION DETAILS -->
        <div class="card">
          <h3 class="card-title">Rejection Details</h3>

          <div class="form-group">
            <label>Primary Reason for Rejection</label>
            <input type="text" placeholder="Enter primary reason" />
          </div>

          <div class="form-group">
            <label>Detailed Explanation</label>
            <textarea
              rows="5"
              placeholder="Provide a clear and structured explanation for the rejection decision. Be specific and concise."
            ></textarea>
          </div>

          <!-- WARNING BOX -->
          <div class="warning-box">
            ⚠️ Rejection confirmation is permanent and cannot be reversed. The proposal will be
            locked with a “Rejected” status.
          </div>
        </div>

      </div>

      <!-- RIGHT SECTION -->
      <div class="right-col">
        <div class="side-card">
          <h4 class="side-title">Rejection Confirmation</h4>

          <p>This action will mark the proposal as <strong>Rejected</strong>.</p>

          <ul class="side-list">
            <li>Proponent will receive a notification</li>
            <li>Decision cannot be undone</li>
            <li>Comments will be visible to the applicant</li>
          </ul>

          <button class="reject-btn" @click="rejectProposal">Reject Proposal</button>

          <div class="note-box">
            Ensure the rejection details are properly filled before submitting.
          </div>
        </div>
      </div>
    </div>
    <!-- REJECT CONFIRMATION MODAL -->
    <div v-if="showRejectModal" class="modal-overlay">
      <div class="modal-box">
        <p>Are you sure you want to reject this proposal?</p>
        <div class="modal-actions">
          <button class="cancel-btn" @click="cancelReject">Cancel</button>
          <button class="reject-btn" @click="confirmReject">Confirm</button>
        </div>
      </div>
    </div>

    <!-- REJECTION SUCCESS MESSAGE -->
    <div v-if="showRejectSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>Proposal rejected successfully!</p>
        <div class="modal-actions">
          <button class="reject-btn" @click="closeSuccess">OK</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const showRejectModal = ref(false)
const showRejectSuccess = ref(false)

const rejectProposal = () => {
  showRejectModal.value = true
}

const cancelReject = () => {
  showRejectModal.value = false
}

// Confirm rejection
const confirmReject = () => {
  showRejectModal.value = false
  showRejectSuccess.value = true
}

// Close success modal
const closeSuccess = () => {
  showRejectSuccess.value = false
}

const goToSubmission = () => {
  router.push('/assigned-proposals')
}
</script>

<style>
.reject-wrapper {
  padding: 25px 30px;
  background: #f4f5fa;
}

.back-btn {
  background: transparent;
  border: none;
  color: #2f2a66;
  font-size: 14px;
  cursor: pointer;
  margin-bottom: 10px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-header h2 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.badge-danger {
  background: #ffe5e5;
  padding: 4px 12px;
  border-radius: 6px;
  color: #c62828;
  font-size: 13px;
}

.subtitle {
  margin-top: 6px;
  font-size: 14px;
  color: #777;
}

.layout-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 25px;
  margin-top: 20px;
}

.card {
  background: white;
  padding: 22px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.card-title {
  margin-bottom: 18px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18px;
}

.info-box label {
  font-size: 12px;
  color: #777;
}

.info-box p {
  font-size: 14px;
  margin-top: 4px;
}

.form-group {
  margin-bottom: 18px;
}

input,
textarea {
  width: 100%;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
  margin-top: 6px;
  font-size: 14px;
}

/* Warning Box */
.warning-box {
  background: #ffecec;
  border-left: 4px solid #d32f2f;
  padding: 12px;
  border-radius: 6px;
  font-size: 13px;
  color: #a62828;
}

.summary-header {
  margin-bottom: 4px;
}

.summary-note {
  font-size: 13px;
  color: #555;
  margin-bottom: 12px;
}

.side-card {
  background: white;
  padding: 22px;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.side-title {
  margin-top: 0;
}

.side-list {
  padding-left: 18px;
  margin: 14px 0;
  font-size: 14px;
}

.reject-btn {
  background: #e53935;
  color: white;
  border: none;
  width: 100%;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
  font-size: 14px;
}

.cancel-btn {
  background: white;

  background: transparent;
  border: 1px solid #ccc;
  width: 100%;
  padding: 12px;
  border-radius: 6px;
  cursor: pointer;
  margin-top: 10px;
  font-size: 14px;
}

.note-box {
  background: #fff8d8;
  padding: 12px;
  border-radius: 6px;
  margin-top: 15px;
  font-size: 13px;
  color: #7a6a00;
}
</style>
