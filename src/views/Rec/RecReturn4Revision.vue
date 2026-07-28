<template>
  <div class="page-wrapper">
    <!-- HEADER -->
    <div class="page-header">
      <h2>Return Proposal for Revision</h2>
      <p class="subtitle">Review proposal details, provide feedback, and set a revision deadline</p>
    </div>

    <div class="layout">
      <!-- LEFT SIDE -->
      <div class="left" v-if="!error">
        <!-- PROPOSAL INFORMATION -->
        <section class="card">
          <h3>Proposal Information</h3>

          <div class="info-grid">
            <div>
              <label>Proposal Title</label>
              <p>{{ proposal.title || '-' }}</p>
            </div>

            <div>
              <label>Proponent</label>
              <p>{{ proposal.proponent || '-' }}</p>
            </div>

            <div>
              <label>Department</label>
              <p>{{ proposal.department || '-' }}</p>
            </div>

            <div>
              <label>Date Submitted</label>
              <p>{{ proposal.dateSubmitted ? new Date(proposal.dateSubmitted).toLocaleDateString() : '-' }}</p>
            </div>

            <div>
              <label>Status</label>
              <span class="status">{{ proposal.status || '-' }}</span>
            </div>
          </div>
        </section>

        <!-- SET DEADLINE -->
        <section class="card">
          <h3>Set Revision Deadline</h3>

          <label>Deadline for Resubmission</label>
          <input type="date" v-model="revisionDate" />

          <p class="note">The proponent will be notified and must resubmit before this deadline.</p>
        </section>

        <!-- REVISION COMMENTS -->
        <section class="card">
          <h3>Revision Comments</h3>

          <label class="label">Comments to Proponent</label>
          <textarea v-model="revisionComment"
            placeholder="List the specific issues that need to be addressed..."></textarea>

          <p class="quick-title">Quick Templates (click to apply)</p>

          <div class="quick-templates">
            <button v-for="(template, index) in quickTemplates" :key="index" type="button" class="template-btn"
              @click="applyTemplate(template)">
              {{ template }}
            </button>
          </div>
        </section>

        <!-- OPTIONAL ATTACHMENTS -->
        <section class="card">
          <h3>Optional Attachments</h3>
          <p class="hint">
            Upload additional documents or notes to help the proponent with revisions.
          </p>

          <div class="upload-box">
            <div class="upload-icon">⬆️</div>
            <p>Drag and drop files or click to browse</p>
            <small>PDF, DOC, DOCX (Max 10MB)</small>

            <button class="select" @click="filePicker.click()">Select Files</button>

            <input type="file" ref="filePicker" multiple style="display: none" @change="handleFiles" />
          </div>
        </section>
      </div>
      <!-- RIGHT SIDE -->
      <div class="right">
        <section class="card">
          <h3>Review Checklist</h3>

          <ul class="checklist">
            <li>
              <input type="checkbox" v-model="checklist[0]" />
              Comments clearly identify issues
            </li>
            <li>
              <input type="checkbox" v-model="checklist[1]" />
              Revision deadline has been set
            </li>
            <li>
              <input type="checkbox" v-model="checklist[2]" />
              Feedback is constructive and specific
            </li>
            <li>
              <input type="checkbox" v-model="checklist[3]" />
              All necessary documentation is attached
            </li>
          </ul>

          <button class="btn primary" @click="goReturnRevision">Return for Revision</button>

          <div class="info-box">
            The proponent will receive a notification with your comments and the revision deadline.
          </div>
        </section>
      </div>
    </div>

    <!-- ERROR STATE -->
    <div v-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="goBack">Go Back</button>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirmModal && !error" class="modal-overlay">
      <div class="modal-box">
        <h3>Return this proposal for revision?</h3>

        <div class="modal-actions">
          <button class="btn cancel" @click="cancelReturn">Cancel</button>
          <button class="btn primary" @click="confirmReturn">Confirm</button>
        </div>
      </div>
    </div>
    <!-- SUCCESS MESSAGE -->
    <div v-if="showSuccessMessage" class="success-toast">
      ✔ Done: Proposal returned for revision.
    </div>

    <!-- ERROR MESSAGE -->
    <div v-if="showError" class="error-toast">
      ⚠ Please complete all required fields before continuing.
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()
const route = useRoute()

const revisionComment = ref('')
const revisionDate = ref('')
const checklist = ref([false, false, false, false])

const showConfirmModal = ref(false)
const showSuccessMessage = ref(false)
const showError = ref(false)
const loading = ref(false)
const error = ref('')

// Proposal data
const proposal = ref({
  id: null,
  title: '',
  proponent: '',
  department: '',
  dateSubmitted: '',
  status: ''
})

// Fetch proposal data
onMounted(async () => {
  const proposalId = route.params.id || route.query.id
  if (proposalId) {
    try {
      loading.value = true
      const res = await api.get(`/api/proposals/${proposalId}`)
      const data = res.data
      proposal.value = {
        id: data.id,
        title: data.projectTitle || data.title || '',
        proponent: data.projectLeader || data.proponent?.name || '',
        department: data.department || data.college || '',
        dateSubmitted: data.createdAt || '',
        status: data.status || ''
      }
    } catch (err) {
      console.error(err)
      error.value = 'Failed to load proposal details.'
    } finally {
      loading.value = false
    }
  }
})

const quickTemplates = [
  'Budget allocation requires more detailed breakdown.',
  'Implementation timeline needs clarification.',
  'Expected outcomes need quantifiable metrics.',
  'Risk mitigation strategies should be expanded.',
  'Community engagement plan needs improvement.',
  'Supporting documentation is incomplete.',
]

const filePicker = ref<HTMLInputElement | null>(null)
const selectedFiles = ref<File[]>([])

const handleFiles = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files) {
    selectedFiles.value = Array.from(target.files)
    console.log('Selected:', selectedFiles.value)
  }
}

const applyTemplate = (text: string) => {
  if (revisionComment.value) {
    revisionComment.value += '\n\n' + text
  } else {
    revisionComment.value = text
  }
}

const isFormValid = computed(() => {
  return (
    revisionComment.value.trim() !== '' &&
    revisionDate.value !== '' &&
    checklist.value.every((c) => c === true)
  )
})

const goReturnRevision = () => {
  if (!isFormValid.value) {
    showError.value = true
    setTimeout(() => (showError.value = false), 2000)
    return
  }
  showConfirmModal.value = true
}

const confirmReturn = async () => {
  try {
    loading.value = true
    // Update proposal status to REVISION
    await api.put(`/api/proposals/${proposal.value.id}/return-revision`)

    showConfirmModal.value = false
    showSuccessMessage.value = true

    setTimeout(() => {
      showSuccessMessage.value = false
      router.push('/rec-dash')
    }, 2000)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to return proposal for revision.'
  } finally {
    loading.value = false
  }
}

const cancelReturn = () => {
  showConfirmModal.value = false
}

const goBack = () => {
  window.history.back()
}
</script>

<style>
.page-wrapper {
  padding: 25px 32px;
  background: #f6f7fb;
  min-height: 100vh;
}

/* HEADER */
.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 4px 0;
  font-size: 22px;
  color: #1f2937;
}

.subtitle {
  color: #6b7280;
  font-size: 14px;
  margin: 0;
}

/* LAYOUT */
.layout {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

/* CARDS */
.card {
  background: white;
  padding: 20px 22px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  margin-bottom: 18px;
}

.card h3 {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 14px 0;
}

/* PROPOSAL INFO */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px 20px;
}

.info-grid label {
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
  display: block;
  margin-bottom: 2px;
}

.info-grid p {
  margin: 0;
  font-weight: 500;
  font-size: 14px;
  color: #1f2937;
}

.status {
  display: inline-block;
  background: #fef3c7;
  color: #92400e;
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

/* DEADLINE */
label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 6px;
}

input[type='date'] {
  width: 100%;
  padding: 10px 12px;
  border-radius: 6px;
  border: 1px solid #d1d5db;
  font-size: 14px;
  color: #1f2937;
}

.note {
  font-size: 12px;
  color: #6b7280;
  margin-top: 8px;
  line-height: 1.5;
}

/* UPLOAD */
.upload-box {
  border: 2px dashed #d1d5db;
  padding: 28px;
  text-align: center;
  border-radius: 10px;
  background: #f9fafb;
}

.upload-box p {
  margin: 4px 0;
  color: #6b7280;
  font-size: 14px;
}

.upload-box small {
  color: #9ca3af;
  font-size: 12px;
}

.upload-icon {
  font-size: 28px;
  margin-bottom: 8px;
}

.select {
  margin-top: 12px;
  padding: 8px 16px;
  background: #eff6ff;
  border: none;
  color: #2563eb;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
}

.select:hover {
  background: #dbeafe;
}

/* CHECKLIST */
.checklist {
  list-style: none;
  padding: 0;
  margin: 0 0 16px 0;
}

.checklist li {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  font-size: 13px;
  color: #374151;
  line-height: 1.4;
}

.checklist input[type="checkbox"] {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  cursor: pointer;
  accent-color: #2563eb;
}

/* REVISION COMMENTS */
.label {
  font-size: 13px;
  color: #555;
  margin-bottom: 6px;
  display: block;
}

textarea {
  width: 100%;
  min-height: 120px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid #cbd5e1;
  resize: vertical;
  font-size: 14px;
  outline: none;
}

textarea:focus {
  border-color: #2563eb;
}

.quick-title {
  font-size: 12px;
  color: #666;
  margin-top: 12px;
  margin-bottom: 6px;
}

.quick-templates {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.template-btn {
  background: #e6f0ff;
  color: #2563eb;
  border: none;
  padding: 6px 12px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.template-btn:hover {
  background: #dbeafe;
}

/* BUTTONS */
.btn {
  width: 100%;
  padding: 10px;
  border-radius: 8px;
  border: none;
  font-weight: bold;
  cursor: pointer;
  margin-top: 10px;
}

.primary {
  background: #ffd803;
}

.cancel {
  background: white;
  border: 1px solid #ccc;
}

/* INFO BOX */
.info-box {
  background: #fff8d6;
  padding: 12px;
  font-size: 12px;
  border-radius: 8px;
  margin-top: 12px;
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

/* MODAL BOX */
.modal-box {
  background: white;
  padding: 25px;
  width: 380px;
  border-radius: 10px;
  text-align: center;
}

.modal-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 20px;
}

/* SUCCESS MESSAGE */
.success-toast {
  position: fixed;
  bottom: 25px;
  right: 25px;
  background: #4caf50;
  color: white;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: bold;
  animation: fadeInOut 2s ease;
}

@keyframes fadeInOut {
  0% {
    opacity: 0;
    transform: translateY(15px);
  }

  10% {
    opacity: 1;
    transform: translateY(0);
  }

  90% {
    opacity: 1;
  }

  100% {
    opacity: 0;
    transform: translateY(15px);
  }
}

.error-toast {
  position: fixed;
  bottom: 25px;
  right: 25px;
  background: #e63946;
  color: white;
  padding: 12px 18px;
  border-radius: 8px;
  font-weight: bold;
  animation: fadeInOut 2s ease;
}

/* ERROR STATE */
.error-state {
  text-align: center;
  padding: 60px 20px;
  background: white;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  margin-top: 24px;
}

.error-state p {
  color: #dc2626;
  font-size: 15px;
  margin-bottom: 16px;
}

.btn-primary {
  padding: 10px 24px;
  background: #2563eb;
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  font-size: 14px;
}

.btn-primary:hover {
  background: #1d4ed8;
}

/* HINT TEXT */
.hint {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 12px;
  line-height: 1.5;
}
</style>
