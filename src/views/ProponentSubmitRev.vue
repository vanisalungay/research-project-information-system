<template>
  <div class="edit-proposal-container">
     <div class="back" @click="$router.go(-1)">← Back to Dashboard</div> <br>
    <!-- Top info bar -->
    <div class="top-bar">
      <h3>
        Edit Proposal
        <span class="editing-mode-badge">Editing Mode</span>
      </h3>
      <p class="top-subtitle">Update your proposal information and resubmit for review</p>
    </div>

    <!-- Notice box -->
    <div class="notice-box">
      <p>
        <strong>⚠️ You are editing an existing proposal</strong><br />
        Any changes made will update your original submission. Once you resubmit, the proposal will
        go through the review process again.
      </p>
    </div>

    <div class="main-content">
      <!-- Sections left block -->
      <div class="sections">
        <div class="section-steps">
          <button
            v-for="step in steps"
            :key="step.id"
            :class="['step-btn', { active: currentStep === step.id }]"
            @click="currentStep = step.id"
          >
            <span class="step-number">{{ step.id }}</span>
            <span class="step-title">{{ step.title }}</span>
          </button>
        </div>

        <div class="changes-summary">
          <p class="version-title">Changes Summary</p>
          <p>
            <b>Version 2.0</b><br />
            <small>Last edited: Dec 14, 2024</small>
          </p>
          <p class="previous-versions">
            <small>Previous versions:<br />v1.0 - Dec 10, 2024</small>
          </p>
        </div>
      </div>

      <!-- Form right block -->
      <div class="form-section">
        <div class="form-header">
          <h4>Basic Information</h4>
          <div class="progress-bar">
            <div class="progress" :style="{ width: progressWidth + '%' }"></div>
          </div>
        </div>

        <form @submit.prevent="saveProposal">
          <label class="form-label">
            Proposal Title
            <input
              type="text"
              v-model="form.proposalTitle"
              placeholder="Community Development Program 2024"
              autocomplete="off"
            />
          </label>

          <label class="form-label">
            Category
            <input type="text" v-model="form.category" autocomplete="off" />
          </label>

          <div class="form-row">
            <label class="form-label">
              Organization Name
              <input
                type="text"
                v-model="form.organizationName"
                placeholder="ABC Foundation"
                autocomplete="off"
              />
            </label>

            <label class="form-label">
              Contact Person
              <input
                type="text"
                v-model="form.contactPerson"
                placeholder="John Doe"
                autocomplete="off"
              />
            </label>
          </div>

          <div class="form-row">
            <label class="form-label">
              Email Address
              <input
                type="email"
                v-model="form.emailAddress"
                placeholder="john@abc.org"
                autocomplete="off"
              />
            </label>

            <label class="form-label">
              Phone Number
              <input
                type="tel"
                v-model="form.phoneNumber"
                placeholder="+1 (555) 123-4567"
                autocomplete="off"
              />
            </label>
          </div>

          <label class="form-label">
            Executive Summary
            <textarea
              v-model="form.executiveSummary"
              placeholder="Provide a brief overview of your proposal"
              rows="4"
            ></textarea>
          </label>

          <div class="buttons-row">
            <button type="button" class="btn previous" @click="previousStep">Previous</button>
            <button type="button" class="btn save-draft" @click="saveDraft">
              <svg
                xmlns="http://www.w3.org/2000/svg"
                width="16"
                height="16"
                fill="currentColor"
                viewBox="0 0 24 24"
                class="icon-save"
              >
                <path
                  d="M17 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-12l-4-4zM7 18a1 1 0 1 1-2 0 1 1 0 0 1 2 0zm7-6h-4v-2h4v2zm3-5.5L18.5 7H16v-2.5z"
                />
              </svg>
              Save Draft
            </button>
            <button type="submit" class="btn save">Save</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'EditProposalContent',
  data() {
    return {
      currentStep: 1,
      steps: [
        { id: 1, title: 'Basic Information' },
        { id: 2, title: 'Project Details' },
        { id: 3, title: 'Budget & Timeline' },
        { id: 4, title: 'Attachments & Review' },
      ],
      form: {
        proposalTitle: 'Community Development Program 2024',
        category: '',
        organizationName: 'ABC Foundation',
        contactPerson: 'John Doe',
        emailAddress: 'john@abc.org',
        phoneNumber: '+1 (555) 123-4567',
        executiveSummary: '',
      },
    }
  },
  computed: {
    progressWidth() {
      return this.currentStep * 25
    },
  },
  methods: {
    previousStep() {
      if (this.currentStep > 1) {
        this.currentStep--
      }
    },
    saveDraft() {
      alert('Draft saved!')
    },
    saveProposal() {
      alert('Proposal saved!')
    },
  },
}
</script>

<style scoped>
.edit-proposal-container {
  font-family:
    'Inter',
    -apple-system,
    BlinkMacSystemFont,
    'Segoe UI',
    Roboto,
    Oxygen,
    Ubuntu,
    Cantarell,
    'Open Sans',
    'Helvetica Neue',
    sans-serif;
  color: #323232;
  background: #fff;
  border-radius: 8px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding: 30px 40px;
}

/* Top bar with title and badge */
.top-bar {
  margin-bottom: 10px;
  flex-shrink: 0;
}

.top-bar h3 {
  font-weight: 700;
  font-size: 20px;
  color: #2d2d2d;
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
}

.editing-mode-badge {
  background: #ffca18;
  padding: 3px 10px;
  font-size: 12px;
  font-weight: 700;
  color: #1e293b;
  border-radius: 12px;
  white-space: nowrap;
}

.top-subtitle {
  font-weight: 400;
  font-size: 13px;
  color: #727272;
  margin-top: 5px;
}

/* Notice box */
.notice-box {
  background: #fef9e5;
  border: 1px solid #f1da4e;
  border-radius: 8px;
  padding: 12px 20px;
  margin-bottom: 30px;
  font-size: 13px;
  line-height: 1.4;
  color: #7b7b7b;
  font-weight: 600;
  flex-shrink: 0;
}

.notice-box strong {
  color: #db6b0b;
  font-weight: 700;
}

/* Main content flex fills remaining space */
.main-content {
  display: flex;
  gap: 30px;
  flex-grow: 1;
  height: 100%;
  overflow: hidden;
}

/* Left side sections fills full height */
.sections {
  width: 230px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e4e4e7;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 25px 20px;
  font-weight: 600;
  font-size: 14px;
  color: #545454;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

/* Section steps as buttons */
.section-steps {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.step-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  background: none;
  border: none;
  color: #545454;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.25s ease;
  text-align: left;
  user-select: none;
}

.step-btn:hover {
  background: #f7f7f7;
}

.step-btn.active {
  background: #1e293b;
  color: white;
  font-weight: 700;
}

.step-number {
  background: #1e293b;
  color: #fff;
  font-weight: 700;
  width: 26px;
  height: 26px;
  border-radius: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

/* Changes summary block */
.changes-summary {
  margin-top: 25px;
  font-size: 12px;
  color: #7b7b7b;
  font-weight: 500;
  border-top: 1px solid #e4e4e7;
  padding-top: 18px;
}

.version-title {
  font-weight: 700;
  color: #323232;
  margin-bottom: 8px;
}

.previous-versions {
  margin-top: 6px;
}

.form-section {
  flex-grow: 1;
  background: #fafafa;
  padding: 30px 35px;
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e4e4e7;
  display: flex;
  flex-direction: column;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

/* Header and progress bar */
.form-header {
  margin-bottom: 25px;
  flex-shrink: 0;
}

.form-header h4 {
  font-weight: 700;
  font-size: 18px;
  color: #1e293b;
  margin-bottom: 6px;
}

.progress-bar {
  height: 3px;
  background: #e0e0e0;
  border-radius: 3px;
  width: 100%;
  overflow: hidden;
}

.progress {
  background: #ffca18;
  height: 3px;
  border-radius: 3px;
  transition: width 0.3s ease;
  width: 25%;
}

/* Form label and input styling */
.form-label {
  display: block;
  font-weight: 600;
  font-size: 14px;
  color: #444;
  margin-bottom: 6px;
  margin-top: 15px;
}

input[type='text'],
input[type='email'],
input[type='tel'],
textarea {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  color: #2d2d2d;
  border-radius: 6px;
  border: 1.5px solid #ccc;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s ease;
  resize: vertical;
}

input[type='text']:focus,
input[type='email']:focus,
input[type='tel']:focus,
textarea:focus {
  outline: none;
  border-color: #ffca18;
}

.form-row {
  display: flex;
  gap: 20px;
  margin-top: 15px;
}

.form-row .form-label {
  flex: 1;
  margin-top: 0;
}

/* Buttons row */
.buttons-row {
  margin-top: auto;
  display: flex;
  gap: 12px;
  padding-bottom: 10px;
}

/* Buttons styling */
.btn {
  padding: 9px 22px;
  font-weight: 700;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  border: none;
  font-family: inherit;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  user-select: none;
  transition: background-color 0.25s ease;
}

/* Previous button */
.btn.previous {
  background: #f0f0f0;
  color: #555;
}

.btn.previous:hover {
  background: #d6d6d6;
}

/* Save draft button */
.btn.save-draft {
  background: #e0e0e0;
  color: #333;
}

.btn.save-draft:hover {
  background: #c0c0c0;
}

/* Save button */
.btn.save {
  background: #ffca18;
  color: #1e293b;
}

.btn.save:hover {
  background: #e0b717;
}

/* Save icon svg */
.icon-save {
  width: 16px;
  height: 16px;
  fill: currentColor;
}
</style>
