<template>
  <div class="assign-container">
    <!-- PAGE TITLE -->
    <h2 class="page-title">Assign Reviewer</h2>
    <p class="page-sub">Select a recommended reviewer for this proposal</p>

    <div class="content-row">
      <!-- LEFT SIDE – REVIEWER LIST -->
      <div class="reviewer-list">
        <div
          class="reviewer-card"
          v-for="(rev, index) in reviewers"
          :key="index"
          @click="selectReviewer(rev)"
          :class="{ selected: selectedReviewer?.id === rev.id }"
        >
          <div class="profile-row">
            <img class="profile-pic" src="https://via.placeholder.com/60" />

            <div>
              <h3>{{ rev.name }}</h3>
              <p class="rating">⭐️ {{ rev.rating }} • {{ rev.reviews }} reviews</p>
            </div>

            <span class="status">Available</span>
          </div>

          <div class="expertise-section">
            <p class="expertise-label">Expertise</p>
            <div class="chip-row">
              <span class="chip" v-for="(tag, i) in rev.expertise" :key="i">
                {{ tag }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT SIDE – SUMMARY -->
      <div class="summary-card">
        <h3>Assignment Summary</h3>

        <div v-if="!selectedReviewer" class="placeholder">
          <img class="placeholder-icon" src="https://via.placeholder.com/80?text=User" />
          <p>Select a reviewer from the list<br />to continue</p>
        </div>

        <div v-else class="summary-info">
          <h4>{{ selectedReviewer.name }}</h4>
          <p class="rating">
            ⭐️ {{ selectedReviewer.rating }} • {{ selectedReviewer.reviews }} reviews
          </p>

          <div class="chip-row">
            <span class="chip" v-for="tag in selectedReviewer.expertise" :key="tag">
              {{ tag }}
            </span>
          </div>

          <button class="assign-btn" @click="openConfirmation">Assign Reviewer</button>
        </div>
      </div>
    </div>
    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirmModal" class="modal-backdrop">
      <div class="modal-box">
        <h3>Assign this reviewer?</h3>
        <p>
          This will assign <strong>{{ selectedReviewer.name }}</strong> to this proposal.
        </p>

        <div class="modal-actions">
          <button class="cancel-btn" @click="showConfirmModal = false">Cancel</button>
          <button class="confirm-btn" @click="confirmAssignment">Confirm</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS MESSAGE -->
    <div v-if="showSuccessMessage" class="success-toast">Reviewer successfully assigned!</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const reviewers = ref([
  {
    id: 1,
    name: 'Dr. Jane Smith',
    rating: 4.8,
    reviews: 45,
    expertise: ['Community Development', 'Social Impact', 'Project Management'],
  },
  {
    id: 2,
    name: 'Dr. Michael Chen',
    rating: 4.9,
    reviews: 52,
    expertise: ['Technology Innovation', 'Digital Transformation', 'Community Development'],
  },
])

const showConfirmModal = ref(false)
const showSuccessMessage = ref(false)

function openConfirmation() {
  showConfirmModal.value = true
}

function confirmAssignment() {
  showConfirmModal.value = false
  showSuccessMessage.value = true

  setTimeout(() => {
    showSuccessMessage.value = false
  }, 2000)
}

const selectedReviewer = ref(null)

function selectReviewer(reviewer) {
  selectedReviewer.value = reviewer
}
</script>

<style scoped>
.assign-container {
  padding: 30px;
}

.page-title {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #262342;
  color: #e6f0ff;
}

.page-sub {
  margin-bottom: 20px;
  color: #666;
}

.content-row {
  display: flex;
  gap: 25px;
}

/* LEFT SIDE LIST */
.reviewer-list {
  width: 65%;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.reviewer-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 0 5px #ddd;
  cursor: pointer;
  transition: 0.2s;
}

.reviewer-card:hover {
  transform: translateY(-2px);
}
.reviewer-card.selected {
  border: 2px solid #ffd803;
}

.profile-row {
  display: flex;
  align-items: center;
  gap: 15px;
}

.profile-pic {
  width: 55px;
  height: 55px;
  border-radius: 50%;
}

.status {
  margin-left: auto;
  color: #22c55e;
  font-weight: bold;
}

.rating {
  font-size: 14px;
  color: #777;
}

/* Chips */
.chip-row {
  margin-top: 5px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.chip {
  background: #e6f0ff;
  color: #1a3998;
  padding: 5px 10px;
  border-radius: 20px;
  font-size: 12px;
}

.expertise-section {
  margin-top: 15px;
}

.expertise-label {
  font-size: 13px;
  color: #555;
}

/* RIGHT SIDE SUMMARY */
.summary-card {
  width: 30%;
  background: white;
  border-radius: 12px;
  padding: 20px;
  height: fit-content;
  box-shadow: 0 0 5px #ddd;
  text-align: center;
}

.placeholder {
  margin-top: 30px;
  color: #888;
}

.placeholder-icon {
  width: 60px;
  margin-bottom: 15px;
}

.summary-info {
  margin-top: 20px;
}

.assign-btn {
  margin-top: 20px;
  padding: 10px 20px;
  width: 100%;
  border: none;
  background: #ffd803;
  font-weight: bold;
  border-radius: 10px;
  cursor: pointer;
}
/* --- MODAL BACKDROP --- */
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

/* --- MODAL BOX --- */
.modal-box {
  background: white;
  width: 360px;
  padding: 25px;
  border-radius: 12px;
  text-align: center;
  box-shadow: 0 0 10px #ccc;
}

/* --- BUTTONS --- */
.modal-actions {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
}

.cancel-btn {
  background: #ddd;
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
}

.confirm-btn {
  background: #ffd803;
  padding: 8px 20px;
  border-radius: 8px;
  border: none;
  font-weight: bold;
  cursor: pointer;
}

/* --- SUCCESS TOAST --- */
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
