<template>
  <div class="page">
    <!-- Back -->
    <button class="back" @click="$router.push('fundviewprop')">← Back to Evaluation</button>

    <h1>Return Proposal for Revision</h1>
    <p class="subtitle">Provide feedback and set deadline for resubmission</p>

    <div class="layout">
      <!-- LEFT COLUMN -->
      <div class="left">
        <!-- Proposal Information -->
        <div class="card">
          <h3>Proposal Information</h3>

          <div class="info-grid">
            <div>
              <span class="label">Title</span>
              <p>{{ proposalObj.title || 'Community Development Program 2024' }}</p>
            </div>
            <div>
              <span class="label">Proponent</span>
              <p>{{ proposalObj.proponent || 'Dr. Aileen Shippy' }}</p>
            </div>
            <div>
              <span class="label">Category</span>
              <p>{{ proposalObj.program || 'Kalikasan Program' }}</p>
            </div>
            <div>
              <span class="label">Proposal ID</span>
              <p>#{{ proposalObj.id || '41' }}</p>
            </div>
          </div>
        </div>

        <!-- Revision Comments -->
        <div class="card">
          <h3>Revision Comments</h3>
          <p class="helper">
            Provide detailed feedback on what needs to be revised or improved in the proposal.
          </p>

          <span class="label">Comments for Proponent</span>
          <textarea
            v-model="comments"
            placeholder="List the specific issues that need to be addressed..."
          ></textarea>
        </div>

        <!-- Deadline -->
        <div class="card">
          <h3>Set Revision Deadline</h3>

          <span class="label">Deadline for Resubmission</span>
          <input type="date" v-model="deadline" />

          <div class="info-box">
            The proponent will be notified via email and must resubmit before this deadline.
          </div>
        </div>

        <!-- Attachments -->
        <div class="card">
          <h3>Optional Attachments</h3>
          <p class="helper">
            Upload additional documents or notes to help the proponent with revisions.
          </p>

          <div class="upload-box">
            <div class="upload-icon">⬆</div>
            <p>Drag and drop files or click to browse</p>
            <small>PDF, DOC, DOCX (Max 10MB)</small>
            <button>Select Files</button>
          </div>
        </div>
      </div>

      <!-- RIGHT COLUMN -->
      <div class="right">
        <div class="card">
          <h3>Review Checklist</h3>

          <label class="check">
            <input type="checkbox" v-model="checklist.c1" />
            Comments clearly identify issues to be addressed
          </label>

          <label class="check">
            <input type="checkbox" v-model="checklist.c2" />
            Revision deadline has been set
          </label>

          <label class="check">
            <input type="checkbox" v-model="checklist.c3" />
            Feedback is constructive and specific
          </label>

          <label class="check">
            <input type="checkbox" v-model="checklist.c4" />
            All necessary documentation is attached
          </label>

          <button class="primary" :disabled="!canSubmit" @click="showConfirm = true">
            Return for Revision
          </button>

          <div class="warning">
            The proposal will move back to revision stage and the proponent will receive your
            comments and the revision deadline.
          </div>
        </div>
      </div>
    </div>

    <!-- CONFIRM MODAL -->
    <div v-if="showConfirm" class="modal-bg">
      <div class="modal">
        <h3>Confirm Return for Revision</h3>
        <p>
          Are you sure you want to return this proposal for revision? The proponent will be notified
          via email.
        </p>

        <div class="modal-actions">
          <button class="secondary" @click="showConfirm = false">Cancel</button>
          <button class="primary" @click="confirm">Confirm</button>
        </div>
      </div>
    </div>

    <!-- SUCCESS -->
    <div v-if="showSuccess" class="toast">Proposal successfully returned for revision.</div>
  </div>
</template>

<script>
export default {
  name: 'ReturnProposalForRevision',
  data() {
    return {
      comments: '',
      deadline: '',
      checklist: { c1: false, c2: false, c3: false, c4: false },
      showConfirm: false,
      showSuccess: false,
      proposalObj: {
        id: '',
        title: '',
        proponent: '',
        program: ''
      }
    }
  },
  computed: {
    canSubmit() {
      return this.comments.trim() && this.deadline && Object.values(this.checklist).every(Boolean)
    },
  },
  methods: {
    confirm() {
      this.showConfirm = false
      this.showSuccess = true
      
      // Update proposal status in proposals database in localStorage
      const stored = localStorage.getItem('proposals_workflow_db');
      if (stored && this.proposalObj.id) {
        try {
          const proposals = JSON.parse(stored);
          const match = proposals.find(p => p.id === this.proposalObj.id);
          if (match) {
            match.stages.rii.status = "Returned for Revision";
            match.stages.rii.remarks = this.comments;
            match.stages.rii.date = new Date().toLocaleDateString();
            localStorage.setItem('proposals_workflow_db', JSON.stringify(proposals));
          }
        } catch (e) {
          console.error(e);
        }
      }
      
      setTimeout(() => {
        this.showSuccess = false;
        this.$router.push('/rii-subproposal');
      }, 3000)
    },
  },
  mounted() {
    const saved = localStorage.getItem("reviewProposal");
    if (saved) {
      try {
        this.proposalObj = JSON.parse(saved);
      } catch (e) {
        console.error(e);
      }
    }
  }
}
</script>

<style scoped>
.page,
.page * {
  color: #1f2937;
}

.page {
  background: #f3f4f6;
  padding: 32px 40px;
  min-height: 100vh;
  font-family: Arial, sans-serif;
  width: 135%;
}

/* Back */
.back {
  background: none;
  border: none;
  color: #2563eb;
  cursor: pointer;
  font-size: 14px;
  margin-bottom: 12px;
}

/* Titles */
h1 {
  margin: 0;
  font-size: 26px;
}

.subtitle {
  color: #6b7280;
  margin-bottom: 24px;
}

/* Layout */
.layout {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: start;
}

/* Cards */
.card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

/* Info */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.label {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.helper {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

/* Inputs */
textarea,
input[type='date'] {
  width: 100%;
  padding: 8px 10px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  margin-top: 6px;
}

textarea {
  min-height: 100px;
}

/* Info box */
.info-box {
  margin-top: 10px;
  background: #eef4ff;
  padding: 10px;
  font-size: 12px;
  border-radius: 6px;
  color: #2563eb;
}

/* Upload */
.upload-box {
  border: 2px dashed #cbd5e1;
  padding: 24px;
  text-align: center;
  border-radius: 8px;
  color: #6b7280;
}

/* Checklist */
.check {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  font-size: 14px;
}

/* Custom checkbox */
.check input {
  appearance: none;
  width: 18px;
  height: 18px;
  border: 2px solid #9ca3af;
  border-radius: 4px;
  cursor: pointer;
  position: relative;
}

.check input:checked {
  background: #2563eb;
  border-color: #2563eb;
}

.check input:checked::after {
  content: '✓';
  position: absolute;
  top: -2px;
  left: 3px;
  color: white;
  font-size: 14px;
}

/* Buttons */
.primary {
  background: #dc4c3f;
  color: white;
  border: none;
  border-radius: 8px;
  width: 100%;
  padding: 10px;
  margin-top: 14px;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.secondary {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
}

/* Warning */
.warning {
  margin-top: 12px;
  font-size: 12px;
  color: #b91c1c;
  background: #fde8e8;
  padding: 8px;
  border-radius: 6px;
}

/* Modal */
.modal-bg {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal {
  background: #ffffff;
  width: 420px;
  padding: 20px;
  border-radius: 10px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* Toast */
.toast {
  position: fixed;
  bottom: 20px;
  right: 20px;
  background: #16a34a;
  color: white;
  padding: 12px 18px;
  border-radius: 6px;
}

/* Responsive */
@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>
