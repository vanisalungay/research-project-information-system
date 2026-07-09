<template>
  <teleport to="body">
    <div v-if="open" class="modal-backdrop">
      <div class="modal-container">
        <!-- HEADER -->
        <header class="modal-header">
          <div class="header-left">
            <h2>Certification & Endorsement</h2>
            <span class="header-badge">Step 2 of 3</span>
          </div>
          <div class="header-actions">
            <button class="btn-download" @click="downloadForm">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              Download Form
            </button>
            <button class="close-btn" @click="close">✕</button>
          </div>
        </header>

        <!-- PROGRESS BAR -->
        <div class="progress-bar">
          <div class="progress-step completed" @click="$emit('goToStep', 1)">
            <span class="step-number">✓</span>
            <span class="step-label">Proposal Details</span>
          </div>
          <div class="progress-line filled"></div>
          <div class="progress-step active" @click="$emit('goToStep', 2)">
            <span class="step-number">2</span>
            <span class="step-label">Certification</span>
          </div>
          <div class="progress-line"></div>
          <div class="progress-step" @click="$emit('goToStep', 3)">
            <span class="step-number">3</span>
            <span class="step-label">Review & Submit</span>
          </div>
        </div>

        <!-- BODY -->
        <div class="modal-body">
          <!-- Certification Statement -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">01</span>
              <h3>Certification Statement</h3>
            </div>
            <div class="cert-card">
              <div class="cert-icon">
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M9 12l2 2 4-4" />
                  <path d="M12 2a10 10 0 1 0 0 20 10 10 0 0 0 0-20z" />
                </svg>
              </div>
              <p class="cert-text">
                I hereby certify that the foregoing statements are true and accurate, and that I have no
                outstanding financial or technical obligations to the Office of the Vice Chancellor for
                Research, Innovation, and Global Engagement or MSU at Naawan. Furthermore, I certify that
                the programs and projects under my supervision comply with the prescribed limits set
                forth in the IRIDE Agenda Guidelines. I understand that any intentional omission or
                false statement will be grounds for disapproval and cancellation of the project.
              </p>
            </div>
          </section>

          <!-- Submitted By -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">02</span>
              <h3>Submitted By</h3>
            </div>
            <div class="section-card">
              <div class="form-row">
                <div class="form-group">
                  <label>Full Name</label>
                  <input type="text" placeholder="Enter full name of proponent" />
                </div>
                <div class="form-group">
                  <label>Designation / Title</label>
                  <input type="text" placeholder="e.g. Faculty, College of Engineering" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label>Date</label>
                  <input type="date" />
                </div>
              </div>
              <div class="form-group">
                <label>Signature</label>
                <div class="upload-area" @click="triggerUpload('submitted')">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="12" y1="18" x2="12" y2="12" />
                    <line x1="9" y1="15" x2="15" y2="15" />
                  </svg>
                  <span>{{ uploadedFiles.submitted ? uploadedFiles.submitted.name : 'Upload signature image' }}</span>
                </div>
                <input type="file" ref="submittedFile" @change="handleFile('submitted', $event)" hidden
                  accept="image/*,.pdf" />
              </div>
              <div v-if="uploadedFiles.submitted" class="file-info">
                <span>📄 {{ uploadedFiles.submitted.name }}</span>
                <button class="btn-remove-file" @click="uploadedFiles.submitted = null">✕</button>
              </div>
            </div>
          </section>

          <!-- Endorsed By -->
          <section class="form-section">
            <div class="section-header">
              <span class="section-number">03</span>
              <h3>Endorsed By</h3>
              <span class="section-sub">(Department Chair / Dean)</span>
            </div>
            <div class="section-card">
              <div class="form-row">
                <div class="form-group">
                  <label>Full Name of Endorser</label>
                  <input type="text" placeholder="Enter name of department chair or dean" />
                </div>
                <div class="form-group">
                  <label>Designation / Title</label>
                  <input type="text" placeholder="e.g. Department Chair" />
                </div>
              </div>
              <div class="form-row">
                <div class="form-group">
                  <label>Date</label>
                  <input type="date" />
                </div>
              </div>
              <div class="form-group">
                <label>Signature</label>
                <div class="upload-area" @click="triggerUpload('endorsed')">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="12" y1="18" x2="12" y2="12" />
                    <line x1="9" y1="15" x2="15" y2="15" />
                  </svg>
                  <span>{{ uploadedFiles.endorsed ? uploadedFiles.endorsed.name : 'Upload signature image' }}</span>
                </div>
                <input type="file" ref="endorsedFile" @change="handleFile('endorsed', $event)" hidden
                  accept="image/*,.pdf" />
              </div>
              <div v-if="uploadedFiles.endorsed" class="file-info">
                <span>📄 {{ uploadedFiles.endorsed.name }}</span>
                <button class="btn-remove-file" @click="uploadedFiles.endorsed = null">✕</button>
              </div>
            </div>
          </section>
        </div>

        <!-- FOOTER -->
        <footer class="modal-footer">
          <button class="btn-secondary" @click="goBack">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="15 18 9 12 15 6" />
            </svg>
            Back
          </button>
          <div class="footer-right">
            <button class="btn-outline" @click="saveDraft">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                <polyline points="17 21 17 13 7 13 7 21" />
                <polyline points="7 3 7 8 15 8" />
              </svg>
              Save Draft
            </button>
            <button class="btn-outline" @click="viewCriteria">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 11l3 3L22 4" />
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
              </svg>
              View Criteria
            </button>
            <button class="btn-primary" @click="submitForm">
              Submit Proposal
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6" />
              </svg>
            </button>
          </div>
        </footer>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { reactive, ref } from 'vue'

defineProps({ open: Boolean, modalId: String })
const emit = defineEmits(['close', 'openPrevious', 'openCriteria', 'submitProposal', 'goToStep'])

const close = () => emit('close')
const downloadForm = () => console.log('Download form clicked')

const uploadedFiles = reactive({ submitted: null, endorsed: null })
const submittedFile = ref(null)
const endorsedFile = ref(null)

const triggerUpload = (type) => {
  if (type === 'submitted') submittedFile.value.click()
  else if (type === 'endorsed') endorsedFile.value.click()
}

const handleFile = (type, event) => {
  const file = event.target.files[0]
  if (file) uploadedFiles[type] = file
}

const resetForm = () => {
  uploadedFiles.submitted = null
  uploadedFiles.endorsed = null
}

defineExpose({ resetForm })

const goBack = () => emit('openPrevious', modalId)
const saveDraft = () => emit('close')
const submitForm = () => emit('submitProposal')
const viewCriteria = () => emit('openCriteria')
</script>

<style scoped>
/* ===== BACKDROP ===== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

/* ===== CONTAINER ===== */
.modal-container {
  background: #f8fafc;
  width: 100%;
  max-width: 800px;
  max-height: 92vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ===== HEADER ===== */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-badge {
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

/* ===== PROGRESS BAR ===== */
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: #94a3b8;
  transition: all 0.2s;
  flex-shrink: 0;
}

.progress-step.active .step-number {
  background: #4f46e5;
  color: #fff;
}

.progress-step.completed .step-number {
  background: #10b981;
  color: #fff;
  font-size: 12px;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
  white-space: nowrap;
}

.progress-step.active .step-label {
  color: #1e293b;
  font-weight: 600;
}

.progress-step.completed .step-label {
  color: #10b981;
  font-weight: 600;
}

.progress-line {
  width: 60px;
  height: 2px;
  background: #e2e8f0;
  margin: 0 12px;
  flex-shrink: 0;
}

.progress-line.filled {
  background: #10b981;
}

/* ===== BODY ===== */
.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

/* ===== FORM SECTIONS ===== */
.form-section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.section-number {
  font-size: 13px;
  font-weight: 700;
  color: #4f46e5;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.section-sub {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

/* ===== CERTIFICATION CARD ===== */
.cert-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 24px;
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.cert-icon {
  width: 48px;
  height: 48px;
  background: #ecfdf5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #10b981;
  flex-shrink: 0;
}

.cert-text {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin: 0;
}

/* ===== SECTION CARD ===== */
.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.15s;
}

.section-card:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ===== FORM ELEMENTS ===== */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

.form-group {
  margin-bottom: 14px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-sizing: border-box;
  font-family: inherit;
}

input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

input::placeholder {
  color: #94a3b8;
}

input[type="date"] {
  cursor: pointer;
}

/* ===== UPLOAD AREA ===== */
.upload-area {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  color: #64748b;
  font-size: 14px;
}

.upload-area:hover {
  border-color: #4f46e5;
  background: #f8faff;
  color: #4f46e5;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-top: 8px;
  font-size: 13px;
  color: #475569;
}

.btn-remove-file {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
}

.btn-remove-file:hover {
  color: #dc2626;
}

/* ===== FOOTER ===== */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.btn-secondary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #d1d5db;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline:hover {
  border-color: #4f46e5;
  color: #4f46e5;
  background: #f8faff;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #4f46e5;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

/* ===== SCROLLBAR ===== */
.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>