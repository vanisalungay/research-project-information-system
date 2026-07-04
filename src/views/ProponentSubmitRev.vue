<template>
  <div class="revision-wrapper">
    <!-- PAGE HEADER -->
    <div class="revision-header">
      <h2>Revision Dashboard</h2>

      <div class="revision-count">
        {{ revisions.length }} Proposals Need Revision
      </div>
    </div>

    <!-- LOADING (non-invasive, optional text only) -->
    <p v-if="loading">Loading...</p>

    <!-- REVISION CARDS -->
    <div
      v-for="revision in revisions"
      :key="revision.id"
      class="revision-card"
    >
      <!-- CARD HEADER -->
      <div class="card-header">
        <div>
          <h3>{{ revision.title }}</h3>
          <span class="revision-tag">Revision Required</span>
          <p class="proposal-id">ID: {{ revision.code }}</p>
        </div>

        <div class="deadline">
          <p>Revision Deadline</p>
          <span>{{ revision.deadline }}</span>
        </div>
      </div>

      <!-- REVIEWER COMMENTS -->
      <div class="comments-box">
        <strong>Reviewer Comments:</strong>
        <p>{{ revision.comment }}</p>
      </div>

      <!-- ACTION BUTTONS -->
      <div class="card-actions">
        <button class="submit-btn" @click="submitRevision(revision.id)">
          ✏️ Submit Revision
        </button>

        <button class="details-btn" @click="viewDetails(revision.id)">
          👁 View Details
        </button>
      </div>
    </div>

    <!-- EMPTY STATE (optional but safe, no layout change) -->
    <p v-if="!loading && revisions.length === 0">
      No revisions available.
    </p>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// ✅ backend-ready only (no fake data)
type Revision = {
  id: number
  title: string
  code: string
  comment: string
  deadline: string
}

const revisions = ref<Revision[]>([])
const loading = ref(false)

const fetchRevisions = async () => {
  loading.value = true

  try {
    // 🔌 backend will be connected here later
    // const res = await axios.get('/api/revisions')
    // revisions.value = res.data

    revisions.value = [] // keep empty until backend exists
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const submitRevision = (id: number) => {
  router.push('/submit-revision')
}

const viewDetails = (id: number) => {
  router.push('/proponent-prop-details')
}

onMounted(fetchRevisions)
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

.main-content {
  display: flex;
  gap: 30px;
  flex-grow: 1;
  height: 100%;
  overflow: hidden;
}

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

.buttons-row {
  margin-top: auto;
  display: flex;
  gap: 12px;
  padding-bottom: 10px;
}

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

.btn.previous {
  background: #f0f0f0;
  color: #555;
}

.btn.previous:hover {
  background: #d6d6d6;
}

.btn.save-draft {
  background: #e0e0e0;
  color: #333;
}

.btn.save-draft:hover {
  background: #c0c0c0;
}

.btn.save {
  background: #ffca18;
  color: #1e293b;
}

.btn.save:hover {
  background: #e0b717;
}

.icon-save {
  width: 16px;
  height: 16px;
  fill: currentColor;
}
</style>
