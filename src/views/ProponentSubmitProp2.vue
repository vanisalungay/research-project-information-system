<template>
  <teleport to="body">
    <div v-if="open" class="modal-backdrop">
      <div class="modal-container">
        <!-- HEADER -->
        <header class="modal-header">
          <h2>CERTIFICATION AND ENDORSEMENT</h2>
          <div class="header-actions">
            <button class="btn-download" @click="downloadForm">⬇ Download Form</button>
            <button class="close-btn" @click="close">×</button>
          </div>
        </header>

        <!-- BODY -->
        <div class="modal-body">
          <!-- Description -->
          <p class="description black-text">
            I hereby certify that the foregoing statements are true and accurate, and that I have no
            outstanding financial or technical obligations to the Office of the Vice Chancellor for
            Research, Innovation, and Global Engagement or MSU at Nawan. Furthermore, I certify that
            the programs and projects under my supervision comply with the prescribed limits set
            forth in the IRIDE Agenda Guidelines. I understand that any intentional omission or
            false statement will be grounds for disapproval and cancellation of the project.
          </p>

          <!-- Submitted By -->
          <section>
            <h3 class="black-text">SUBMITTED BY</h3>

            <div class="form-grid">
              <div>
                <label class="black-text">Name:</label>
                <input type="text" />
              </div>

              <div>
                <label class="black-text">Designation/Title:</label>
                <input type="text" />
              </div>

              <div>
                <label class="black-text">Date:</label>
                <input type="date" />
              </div>
            </div>

            <label class="signature-label black-text">Signature:</label>

            <div class="upload-box" @click="triggerUpload('submitted')">
              <span>{{
                uploadedFiles.submitted
                  ? uploadedFiles.submitted.name
                  : 'Click or drag file to this area to upload'
              }}</span>
              <input
                type="file"
                ref="submittedFile"
                @change="handleFile('submitted', $event)"
                style="display: none"
              />
            </div>
          </section>

          <!-- Endorsed By -->
          <section>
            <h3 class="black-text">ENDORSED BY</h3>

            <div class="form-grid">
              <div>
                <label class="black-text">Name:</label>
                <input type="text" />
              </div>

              <div>
                <label class="black-text">Designation/Title:</label>
                <input type="text" />
              </div>

              <div>
                <label class="black-text">Date:</label>
                <input type="date" />
              </div>
            </div>

            <label class="signature-label black-text">Signature:</label>

            <div class="upload-box" @click="triggerUpload('endorsed')">
              <span>{{
                uploadedFiles.endorsed
                  ? uploadedFiles.endorsed.name
                  : 'Click or drag file to this area to upload'
              }}</span>
              <input
                type="file"
                ref="endorsedFile"
                @change="handleFile('endorsed', $event)"
                style="display: none"
              />
            </div>
          </section>
        </div>

        <!-- FOOTER -->
        <footer class="modal-footer">
          <button class="btn-draft" @click="goBack">BACK</button>
          <button class="btn-draft" @click="saveDraft">SAVE DRAFT</button>
          <button class="btn-draft" @click="viewCriteria">VIEW CRITERIA</button>
          <button class="btn-next" @click="submitForm">SUBMIT</button>
        </footer>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { reactive, ref } from 'vue'

defineProps({ open: Boolean, modalId: String })
const emit = defineEmits(['close', 'openPrevious', 'openCriteria'])
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

// Footer buttons
const goBack = () => emit('openPrevious', modalId)
const saveDraft = () => emit('close')
const submitForm = () => emit('close')

const viewCriteria = () => {
  emit('openCriteria')
}
</script>

<style scoped>
/* Overlay & Modal Container */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
}
.modal-container {
  background: #fff;
  width: 95%;
  max-width: 1100px;
  max-height: 90vh;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}
.modal-body {
  padding: 16px;
  overflow-y: auto;
}

/* Header */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid #ddd;
}
.modal-header h2 {
  font-size: 18px;
  font-weight: bold;
  color: #1f2a6d;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-download {
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
}
.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
}

/* Footer */
.modal-footer {
  border-top: 1px solid #ddd;
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px;
}
.modal-footer button {
  width: 160px;
  height: 50px;
  font-size: 16px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}
.btn-draft {
  background-color: #e5e7eb;
  border: none;
  color: #111;
}
.btn-next {
  background-color: #2563eb;
  border: none;
  color: #fff;
}

/* Other styles */
.description {
  margin: 16px 0 24px;
  font-size: 13px;
  line-height: 1.6;
  color: #000;
}
section {
  margin-bottom: 28px;
}
h3 {
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #000;
}
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 20px;
}
label {
  font-size: 12px;
  font-weight: bold;
  display: block;
  margin-bottom: 4px;
  color: #000;
}
input {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 3px;
  font-size: 13px;
}
.signature-label {
  margin-top: 14px;
  display: block;
  color: #000;
}
.upload-box {
  margin-top: 6px;
  border: 2px dashed #cfcfcf;
  padding: 20px;
  text-align: center;
  font-size: 12px;
  color: #000;
  border-radius: 4px;
  cursor: pointer;
}
.upload-box:hover {
  border-color: #2563eb;
}
</style>
