<template>
  <div class="page-wrapper">
    <!-- HEADER -->
    <div class="page-header">
      <div>
        <h2>{{ proposal.title }}</h2>
        <span class="badge">{{ proposal.status }}</span>
      </div>

      <div class="actions">
        <button class="btn primary" @click="goToDetailed">See Detailed Proposal</button>
        <button class="btn outline" @click="downloadAll">Download All Documents</button>
      </div>
    </div>

    <!-- MAIN CONTENT -->
    <div class="grid">
      <!-- LEFT -->
      <div class="left">
        <section class="card">
          <h3>Proposal Information</h3>
          <div class="info-grid">
            <p><strong>Category:</strong> {{ proposal.program }}</p>
            <p><strong>Proponent:</strong> {{ proposal.proponent }}</p>
            <p><strong>Email:</strong> {{ proposal.email }}</p>
            <p><strong>Phone:</strong> {{ proposal.phone }}</p>
            <p><strong>Budget:</strong> {{ proposal.budget }}</p>
            <p><strong>Date Submitted:</strong> {{ proposal.date }}</p>
          </div>
        </section>

        <section class="card">
          <h3>Timeline</h3>
          <ul class="timeline">
            <li v-for="step in proposal.timeline" :key="step.label">
              <span class="dot"></span>
              <div>
                <strong>{{ step.label }}</strong>
                <small>{{ step.date }}</small>
              </div>
            </li>
          </ul>
        </section>

        <section class="card">
          <h3>Reviewer Evaluation Summary</h3>

          <div v-for="score in proposal.scores" :key="score.label" class="score-row">
            <span>{{ score.label }}</span>
            <div class="bar">
              <div class="fill" :style="{ width: score.score + '%' }"></div>
            </div>
            <small>{{ score.score }}/100</small>
          </div>

          <p class="remarks">{{ proposal.remarks }}</p>
          <div class="recommend">{{ proposal.recommendation }}</div>
        </section>

        <section class="card actions-card">
          <h3>OVCRIGE Actions</h3>

          <button class="btn primary" @click="goToAssignReviewer">Endorse to REC</button>

          <button class="btn warning" @click="goToReturnForRevision">Return for Revision</button>
        </section>
      </div>

      <!-- RIGHT -->
      <div class="right">
       <section class="card center">
  <h3>REC Status</h3>

  <p class="name">{{ proposal.reviewer.name }}</p>

  <p>{{ proposal.reviewer.role }}</p>

  <span class="status-good">
    {{ proposal.status }}
  </span>
</section>
        <section class="card">
          <h3>Attachments</h3>

          <ul class="files">
            <li v-for="file in proposal.attachments" :key="file.name">
              📄 {{ file.name }}
              <button class="download" @click="downloadFile(file)">⬇️</button>
            </li>
          </ul>
        </section>
      </div>
    </div>
    <div v-if="showApproveConfirm" class="modal-overlay">
      <div class="modal-box">
        <p>Are you sure you want to approve this?</p>

        <div class="modal-actions">
          <button class="btn warning" @click="showApproveConfirm = false">Cancel</button>
          <button class="btn primary" @click="confirmApprove">Confirm</button>
        </div>
      </div>
    </div>

    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>Proposal approved successfully!</p>
        <button class="btn primary" @click="showSuccess = false">OK</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const proposal = ref({
  title: '',
  status: '',
  program: '',
  proponent: '',
  email: '',
  phone: '',
  budget: '',
  date: '',
  timeline: [],
  scores: [],
  remarks: '',
  recommendation: '',
  reviewer: {
    name: '',
    role: '',
  },
  attachments: [],
})

onMounted(() => {
  proposal.value = {
    // Header
    title: '',
    status: '',

    // Proposal Information
    program: '',
    proponent: '',
    email: '',
    phone: '',
    budget: '',
    date: '',

    // Timeline (labels only)
    timeline: [
      { label: 'Proposal Submitted', date: '' },
      { label: 'Received by OVCRIGE', date: '' },
      { label: 'Endorsed to REC', date: '' },
      { label: 'REC Evaluation', date: '' },
    ],

    // Reviewer Evaluation Summary
    scores: [
      { label: 'Overall Score', score: 0 },
      { label: 'Relevance', score: 0 },
      { label: 'Budget Feasibility', score: 0 },
      { label: 'Impact', score: 0 },
    ],

    remarks: '',
    recommendation: '',

    reviewer: {
      name: '',
      role: '',
    },

    // Attachments
    attachments: [
      {
        name: '',
        url: '',
      },
    ],
  }
})

const goToDetailed = () => {
  router.push('/rec-prop')
}

const downloadFile = (file) => {
  if (!file.url) return

  const link = document.createElement('a')
  link.href = file.url
  link.download = file.name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const downloadAll = () => {
  proposal.value.attachments.forEach((file, index) => {
    if (!file.url) return

    setTimeout(() => {
      downloadFile(file)
    }, index * 300)
  })
}

const goToReturnForRevision = () => {
  router.push('/return4-revision')
}

const goToAssignReviewer = () => {
  router.push('/assign-reviewer')
}

const showApproveConfirm = ref(false)
const showSuccess = ref(false)

const confirmApprove = () => {
  showApproveConfirm.value = false
  showSuccess.value = true
}
</script>

<style>
.page-wrapper {
  padding: 25px;
  background: #f4f6f8;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.badge {
  background: #ffd803;
  padding: 6px 14px;
  border-radius: 20px;
  font-weight: bold;
}

.grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 15px;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.timeline li {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.dot {
  width: 10px;
  height: 10px;
  background: #ffd803;
  border-radius: 50%;
  margin-top: 6px;
}

.score-row {
  margin-bottom: 10px;
}

.bar {
  height: 8px;
  background: #ddd;
  border-radius: 10px;
}

.fill {
  height: 8px;
  background: #4d7cff;
  border-radius: 10px;
}

.remarks {
  margin-top: 10px;
}

.recommend {
  margin-top: 10px;
  font-weight: bold;
}

.center {
  text-align: center;
}

.status-good {
  display: inline-block;
  background: #c8f7c5;
  padding: 6px 12px;
  border-radius: 8px;
  margin: 10px 0;
}

.files li {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
}

.btn {
  padding: 8px 14px;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  font-weight: bold;
}

.primary {
  background: #4d7cff;
  color: white;
}

.warning {
  background: #f39c12;
  color: white;
}

.outline {
  background: #4d7cff;
  color: white;
}

.modal-overlay {
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

.modal-box {
  background: white;
  padding: 20px;
  border-radius: 10px;
  text-align: center;
  width: 300px;
}

.modal-actions {
  display: flex;
  justify-content: space-between;
  margin-top: 15px;
}
</style>
