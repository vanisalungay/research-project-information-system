<template>
  <div class="page-wrapper">
    <!-- HEADER -->
    <div class="page-header">
      <h2>Return Proposal for Revision</h2>
      <p class="subtitle">Review proposal details, provide feedback, and set a revision deadline</p>
    </div>

    <div class="layout">
      <!-- LEFT SIDE -->
      <div class="left">
       <!-- PROPOSAL INFORMATION -->
<section class="card">
  <h3>Proposal Information</h3>

  <div class="info-grid">
    <div>
      <label>Proposal Title</label>
      <p>{{ proposal.projectTitle || '—' }}</p>
    </div>

    <div>
      <label>Proponent</label>
      <p>{{ proposal.projectLeader || proposal.proponent?.name || '—' }}</p>
    </div>

    <div>
      <label>Department</label>
      <p>{{ proposal.college || '—' }}</p>
    </div>

    <div>
      <label>Date Submitted</label>
      <p>{{ proposal.createdAt ? new Date(proposal.createdAt).toLocaleDateString() : '—' }}</p>
    </div>

    <div>
      <label>Status</label>
      <span class="status">{{ proposal.status || '—' }}</span>
    </div>
  </div>
</section>

        <!-- SET DEADLINE (handled by RPS) -->
        <section class="card">
          <h3>Revision Deadline</h3>
          <p class="note">The revision deadline will be set by RPS when they forward this request to the proponent.</p>
        </section>

        <!-- REVISION COMMENTS -->
        <section class="card">
          <h3>Revision Comments</h3>

          <label class="label">Comments to Proponent</label>
          <textarea
            v-model="revisionComment"
            placeholder="List the specific issues that need to be addressed..."
          ></textarea>

          <p class="quick-title">Quick Templates (click to apply)</p>

          <div class="quick-templates">
            <button
              v-for="(template, index) in quickTemplates"
              :key="index"
              type="button"
              class="template-btn"
              @click="applyTemplate(template)"
            >
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
            <button class="select">Select Files</button>
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
              Revision remarks are clear and specific
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

          <button class="btn cancel">Cancel</button>

          <div class="info-box">
            Your remarks will be sent to RPS, who will set the deadline and forward the request to the proponent.
          </div>
        </section>
      </div>
    </div>

    <!-- CONFIRMATION MODAL -->
    <div v-if="showConfirmModal" class="modal-overlay">
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
    <div v-if="showError || loadError" class="error-toast">
      ⚠ {{ loadError || 'Please complete all required fields before continuing.' }}
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'

const router = useRouter()
const route = useRoute()
const userStore = useUserDataStore()

const proposal = ref({})
const loading = ref(false)
const loadError = ref('')

const revisionComment = ref('')
const checklist = ref([false, false, false, false])

const showConfirmModal = ref(false)
const showSuccessMessage = ref(false)
const showError = ref(false)
const submitting = ref(false)

const quickTemplates = [
  'Budget allocation requires more detailed breakdown.',
  'Implementation timeline needs clarification.',
  'Expected outcomes need quantifiable metrics.',
  'Risk mitigation strategies should be expanded.',
  'Community engagement plan needs improvement.',
  'Supporting documentation is incomplete.',
]

const applyTemplate = (text) => {
  if (revisionComment.value) {
    revisionComment.value += '\n\n' + text
  } else {
    revisionComment.value = text
  }
}

const isFormValid = computed(() => {
  return (
    revisionComment.value.trim() !== '' &&
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
  if (submitting.value) return
  submitting.value = true
  try {
    await api.put(`/api/proposals/${proposal.value.id}/return-revision`, {
      returnedById: userStore.user?.id || null,
      returnedByName: userStore.user?.name || null,
      returnedByOffice: 'OVCRIGE',
      remarks: revisionComment.value.trim()
    })
    showConfirmModal.value = false
    showSuccessMessage.value = true
    setTimeout(() => {
      showSuccessMessage.value = false
      router.push('/review-prop')
    }, 2000)
  } catch (err) {
    console.error(err)
    showConfirmModal.value = false
    loadError.value = 'Failed to return proposal for revision.'
  } finally {
    submitting.value = false
  }
}

const cancelReturn = () => {
  showConfirmModal.value = false
}

onMounted(async () => {
  const id = route.params.id || route.query.id
  if (!id) return
  loading.value = true
  try {
    const res = await api.get(`/api/proposals/${id}`)
    proposal.value = res.data
  } catch (err) {
    console.error(err)
    loadError.value = 'Failed to load proposal.'
  } finally {
    loading.value = false
  }
})
</script>

<style>
.page-wrapper {
  padding: 25px;
  background: #f6f7fb;
}

/* HEADER */
.page-header h2 {
  margin-bottom: 5px;
}

.subtitle {
  color: #666;
  font-size: 14px;
}

/* LAYOUT */
.layout {
  display: grid;
  grid-template-columns: 3fr 1.2fr;
  gap: 20px;
  margin-top: 20px;
}

/* CARDS */
.card {
  background: white;
  padding: 18px;
  border-radius: 10px;
  margin-bottom: 15px;
}

/* PROPOSAL INFO */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 15px;
}

.info-grid label {
  font-size: 12px;
  color: #777;
}

.info-grid p {
  margin: 4px 0 0;
  font-weight: 500;
}

.status {
  background: #fff8d6;
  color: #9a7b00;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
}

/* DEADLINE */
input[type='date'] {
  width: 100%;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #ccc;
}

.note {
  font-size: 12px;
  color: #555;
  margin-top: 8px;
}

/* UPLOAD */
.upload-box {
  border: 2px dashed #cbd5e1;
  padding: 25px;
  text-align: center;
  border-radius: 10px;
  background: #fafafa;
}

.upload-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.select {
  margin-top: 10px;
  padding: 6px 12px;
  background: #e6f0ff;
  border: none;
  color: #2452ff;
  border-radius: 6px;
  cursor: pointer;
}

/* CHECKLIST */
.checklist {
  list-style: none;
  padding: 0;
}

.checklist li {
  margin-bottom: 10px;
  font-size: 13px;
}

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
</style>
