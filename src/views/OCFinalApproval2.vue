<template>
  <div class="page">
    <!-- Loading -->
    <div v-if="loading" class="card">
      <h3>Loading...</h3>
      <p>Please wait while the proposal is being loaded.</p>
    </div>

    <!-- Error -->
    <div v-else-if="error" class="card">
      <h3>Unable to Load Proposal</h3>
      <p>Something went wrong while retrieving the proposal.</p>
    </div>

    <!-- Empty State -->
    <div v-else-if="!proposal.project_title" class="card">
      <h3>No Proposal Found</h3>
      <p>The requested proposal does not exist or has been removed.</p>
    </div>

    <!-- CONTENT -->
    <template v-else>
      <!-- BACK -->
      <div class="back" @click="$router.go(-1)">← Back to Dashboard</div>

      <!-- PROPOSAL HEADER CARD -->
      <div class="card proposal-card">
        <div class="proposal-left">
          <h1>{{ proposal.project_title }}</h1>

          <div class="proposal-meta">
            <div>
              <span>Proponent</span>
              <p>{{ proposal.project_leader }}</p>
            </div>

            <div>
              <span>Program</span>
              <p>{{ proposal.program_title }}</p>
            </div>

            <div>
              <span>Budget</span>
              <p>{{ proposal.total_budget }}</p>
            </div>

            <div>
              <span>Duration</span>
              <p>{{ proposal.duration }}</p>
            </div>
          </div>
        </div>

        <button class="view-btn" @click="goToDetailed">
          View Details
        </button>
      </div>

      <!-- STATUS -->
      <div class="status-row">
        <div
          class="card status-card"
          v-for="status in workflowStatus"
          :key="status.id"
        >
          <div class="status-dot"></div>

          <div>
            <h4>{{ status.stage }}</h4>
            <p>{{ status.status }}</p>
            <small>{{ status.description }}</small>
          </div>
        </div>
      </div>

      <!-- REC EVALUATION -->
      <div class="card">
        <h3>REC Evaluation Summary</h3>

        <div
          class="eval-row"
          v-for="item in evaluation"
          :key="item.label"
        >
          <div class="eval-header">
            <span>{{ item.label }}</span>
            <span>{{ item.score }}</span>
          </div>

          <div class="bar">
            <div
              class="bar-fill"
              :style="{ width: item.percent + '%' }"
            ></div>
          </div>
        </div>

        <div class="overall">
          <div class="eval-header">
            <strong>Overall Evaluation Score</strong>
            <strong>{{ proposal.overall_score }}</strong>
          </div>

          <div class="bar">
            <div
              class="bar-fill"
              :style="{ width: proposal.overall_percent + '%' }"
            ></div>
          </div>
        </div>
      </div>

      <!-- RECOMMENDATION -->
      <div class="card">
        <h3>REC Recommendation</h3>

        <div class="recommendation">
          <strong>{{ proposal.recommendation_title }}</strong>
          <p>{{ proposal.recommendation }}</p>
        </div>
      </div>

      <!-- FINAL APPROVAL -->
      <div class="card">
        <div class="recommendation">
          <strong>Proposal Approved</strong>

          <p>
            This proposal has been approved for implementation.
            Please upload the Special Order document.
          </p>
        </div>
      </div>

      <!-- BUTTON -->
      <div class="footer">
        <button
          class="approve-btn"
          @click="showUploadModal = true"
        >
          Upload Special Order
        </button>
      </div>

      <!-- UPLOAD MODAL -->
      <div
        v-if="showUploadModal"
        class="modal-overlay"
      >
        <div class="modal-card">

          <div class="modal-header">
            <div class="modal-icon">🏅</div>

            <h3>Upload Special Order</h3>

            <p>
              Upload the official Special Order document.
            </p>
          </div>

          <div class="modal-info">
            <div>
              <span>Proposal Title</span>
              <p>{{ proposal.project_title }}</p>
            </div>

            <div>
              <span>Proponent</span>
              <p>{{ proposal.project_leader }}</p>
            </div>

            <div>
              <span>Program</span>
              <p>{{ proposal.program_title }}</p>
            </div>

            <div>
              <span>Budget</span>
              <p>{{ proposal.total_budget }}</p>
            </div>
          </div>

          <div class="upload-box">

            <input
              type="file"
              accept=".pdf"
              hidden
              ref="fileInput"
              @change="handleFileUpload"
            />

            <div
              class="upload-area"
              @click="$refs.fileInput.click()"
            >
              <p v-if="!selectedFile">
                Click to upload or drag and drop
              </p>

              <p v-else>
                {{ selectedFile.name }}
              </p>

              <small>
                PDF files only (Max 5MB)
              </small>
            </div>

          </div>

          <div class="modal-actions">
            <button
              class="cancel-btn"
              @click="showUploadModal = false"
            >
              Cancel
            </button>

            <button
              class="issue-btn"
              @click="issueSpecialOrder"
            >
              Issue Special Order
            </button>
          </div>

        </div>
      </div>

      <!-- SUCCESS MODAL -->
      <div
        v-if="showSuccessModal"
        class="modal-overlay"
      >
        <div class="success-card">

          <button
            class="close-btn"
            @click="showSuccessModal = false"
          >
            ×
          </button>

          <div class="success-icon">
            ✔
          </div>

          <h3>Proposal Approved!</h3>

          <p class="success-text">
            The proposal has been successfully approved and the
            Special Order has been issued.
          </p>

          <button
            class="success-btn"
            @click="goToDashboard"
          >
            View Approved Proposals
          </button>

        </div>
      </div>
    </template>
  </div>
</template>

<script>
export default {
  name: "OCFinalApproval2",

  data() {
    return {
      loading: false,
      error: false,

      showUploadModal: false,
      showSuccessModal: false,

      selectedFile: null,

      proposal: {
        id: null,
        project_title: "",
        project_leader: "",
        program_title: "",
        total_budget: "",
        duration: "",
        recommendation_title: "",
        recommendation: "",
        overall_score: "",
        overall_percent: 0,
      },

      workflowStatus: [],

      evaluation: [],
    }
  },

  mounted() {
    this.fetchProposal()
  },

  methods: {
    async fetchProposal() {
      this.loading = true

      try {
        // const response = await axios.get(`/api/oc/final-approval/${this.$route.params.id}`)

        // this.proposal = response.data.proposal
        // this.workflowStatus = response.data.workflow_status
        // this.evaluation = response.data.evaluation

      } catch (e) {
        this.error = true
        console.log(e)
      } finally {
        this.loading = false
      }
    },

    handleFileUpload(event) {
      this.selectedFile = event.target.files[0]
    },

    goToDetailed() {
      this.$router.push("/oc-detailed")
    },

    async issueSpecialOrder() {

      if (!this.selectedFile) {
        alert("Please upload a PDF first.")
        return
      }

      // Upload API here

      this.showUploadModal = false
      this.showSuccessModal = true
    },

    goToDashboard() {
      this.$router.push("/home")
    },
  },
}
</script>

<style scoped>
.page {
  padding: 24px;
  background: #f4f6f9;
  min-height: 100vh;
}

/* BACK */
.back {
  font-size: 13px;
  color: #4b5563;
  margin-bottom: 12px;
  cursor: pointer;
}

/* CARD */
.card {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

/* TOP CARD */
.proposal-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.proposal-left h1 {
  font-size: 20px;
  margin-bottom: 12px;
}

.proposal-meta {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.proposal-meta span {
  font-size: 12px;
  color: #888;
}

.proposal-meta p {
  font-weight: 600;
  margin-top: 4px;
}

/* VIEW BUTTON */
.view-btn {
  background: gray;
  border: none;
  border-radius: 10px;
  padding: 8px 14px;
  cursor: pointer;
}

/* STATUS ROW */
.status-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.status-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.status-dot {
  width: 10px;
  height: 10px;
  background: #28a745;
  border-radius: 50%;
  margin-top: 6px;
}

.status-card h4 {
  margin: 0;
  font-size: 14px;
}

.status-card p {
  margin: 4px 0;
}

.bold {
  font-weight: 700;
}

/* EVALUATION */
.eval-row {
  margin-bottom: 14px;
}

.eval-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 14px;
}

.bar {
  width: 100%;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
}

.bar-fill {
  height: 100%;
  background: #374151;
  border-radius: 4px;
}

.overall {
  margin-top: 20px;
}

.overall-bar .bar-fill {
  width: 89%;
}

/* RECOMMENDATION */
.recommendation {
  background: #ecfdf3;
  border-radius: 12px;
  padding: 16px;
  font-size: 14px;
  color: #065f46;
}

/* SUMMARY */
.summary-text {
  font-size: 14px;
  color: #374151;
}

.summary-stats {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  margin-top: 14px;
}

.summary-stats span {
  font-size: 12px;
  color: #6b7280;
}

/* FOOTER */
.footer {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.approve-btn {
  width: 60%;
  padding: 14px;
  background: #2f2e41;
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 15px;
  cursor: pointer;
}

/* MODAL OVERLAY */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

/* MODAL CARD */
.modal-card {
  background: #fff;
  width: 520px;
  border-radius: 16px;
  padding: 24px;
}

/* HEADER */
.modal-header {
  text-align: center;
  margin-bottom: 20px;
}

.modal-icon {
  font-size: 32px;
  margin-bottom: 8px;
}

.modal-header h3 {
  margin-bottom: 6px;
}

.modal-header p {
  font-size: 13px;
  color: #6b7280;
}

/* INFO GRID */
.modal-info {
  background: #f7f7ff;
  border-radius: 12px;
  padding: 14px;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 20px;
}

.modal-info span {
  font-size: 11px;
  color: #6b7280;
}

.modal-info p {
  font-weight: 600;
  font-size: 13px;
}

/* UPLOAD BOX */
.upload-box {
  margin-bottom: 20px;
}

.upload-area {
  border: 2px dashed #c7c7ff;
  border-radius: 12px;
  padding: 24px;
  text-align: center;
  cursor: pointer;
  color: #6b7280;
}

/* ACTION BUTTONS */
.modal-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.cancel-btn {
  flex: 1;
  background: #f3f4f6;
  border: none;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
}

.issue-btn {
  flex: 1;
  background: #a855f7;
  color: white;
  border: none;
  border-radius: 10px;
  padding: 12px;
  cursor: pointer;
}

/* SUCCESS MODAL */
.success-card {
  background: #fff;
  width: 420px;
  border-radius: 16px;
  padding: 28px;
  text-align: center;
  position: relative;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 16px;
  border: none;
  background: transparent;
  font-size: 20px;
  cursor: pointer;
}

.success-icon {
  width: 48px;
  height: 48px;
  background: #22c55e;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 14px;
  font-size: 22px;
}

.success-text {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 20px;
}

.success-btn {
  width: 100%;
  background: #facc15;
  border: none;
  border-radius: 10px;
  padding: 12px;
  font-weight: 600;
  cursor: pointer;
}
</style>
