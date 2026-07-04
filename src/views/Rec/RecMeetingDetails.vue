<template>
  <div class="meeting-container">
    <!-- HEADER -->
    <div class="meeting-header">
      <div>
        <h2 class="title">Healthcare Facility Upgrade 2024</h2>
        <span class="status">Pending</span>
        <p class="proposal-id">Proposal ID: #1</p>
      </div>

      <div class="header-actions">
        <button class="btn yellow" @click="goToDetailed">See Detailed Proposal</button>
        <button class="btn outline" @click="downloadAll">Download All Documents</button>
      </div>
    </div>

    <!-- ERROR STATE -->
    <div v-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="goBack">Go Back</button>
    </div>

    <!-- MAIN GRID -->
    <div v-else class="content-grid">
      <!-- LEFT -->
      <div class="left">
        <!-- Proposal Information -->
        <div class="card">
          <h3>Proposal Information</h3>

          <div class="info-grid">
            <div>
              <p class="label">Category</p>
              <p>Kalikasan Program</p>
            </div>

            <div>
              <p class="label">Proponent Organization</p>
              <p>Dr. Meow Chan</p>
            </div>

            <div>
              <p class="label">Contact Person</p>
              <p>Allen Shippy</p>
            </div>

            <div>
              <p class="label">Email</p>
              <p>allenshippy@msunaawan.edu.ph</p>
            </div>

            <div>
              <p class="label">Phone</p>
              <p>+1 (555) 123-4567</p>
            </div>

            <div>
              <p class="label">Total Budget</p>
              <p>₱150,000</p>
            </div>

            <div>
              <p class="label">Duration</p>
              <p>12 months</p>
            </div>

            <div>
              <p class="label">Submitted Date</p>
              <p>2024-12-05</p>
            </div>
          </div>
        </div>

        <!-- Meeting Details -->
        <div class="card">
          <h3>Meeting Details</h3>

          <div class="meeting-item">
            <div class="icon green">📍</div>
            <div>
              <p class="label">Venue</p>
              <p>REC Conference Room / Zoom / MS Teams</p>
            </div>
          </div>

          <div class="meeting-item">
            <div class="icon green">👤</div>
            <div>
              <p class="label">Assigned By</p>
              <p>OVCRIGE</p>
            </div>
          </div>

          <div class="meeting-item">
            <div class="icon green">🕒</div>
            <div>
              <p class="label">Date</p>
              <p>March 10, 2025</p>
              <small>1:00 PM – 4:00 PM</small>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT -->
      <div class="right">
        <!-- Attachments -->
        <div class="card">
          <h3>Attachments</h3>
          <ul class="attachments">
            <li v-if="attachments.length === 0" style="color: #999; font-style: italic;">
              No attachments available
            </li>
            <li v-for="file in attachments" :key="file.name">
              <span>📄 {{ file.name }}</span>
              <button class="download" @click="downloadFile(file)">⬇️</button>
            </li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const error = ref('')
const proposal = ref({
  id: null,
  title: '',
  proponent: '',
  status: '',
  programTitle: '',
  projectLeader: '',
  duration: '',
  totalBudget: 0,
  createdAt: '',
  attachments: []
})

const attachments = ref([])

const downloadFile = (file) => {
  const link = document.createElement('a')
  link.href = file.url || `http://localhost:8081/uploads/${file.name}`
  link.download = file.name
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const downloadAll = () => {
  if (attachments.value.length > 0) {
    attachments.value.forEach((file) => downloadFile(file))
  }
}

const goToDetailed = () => {
  const proposalId = proposal.value.id || route.params.id || route.query.id
  router.push({ path: '/detailed-proposal', query: { id: proposalId } })
}

// Fetch proposal data
onMounted(async () => {
  const proposalId = route.params.id || route.query.id
  if (proposalId) {
    loading.value = true
    error.value = ''
    
    try {
      const res = await api.get(`/api/proposals/${proposalId}`)
      const data = res.data
      
      proposal.value = {
        id: data.id,
        title: data.projectTitle || data.title || 'Untitled Proposal',
        proponent: data.projectLeader || data.proponent?.name || 'Unknown',
        status: data.status || 'PENDING',
        programTitle: data.programTitle || '',
        projectLeader: data.projectLeader || '',
        duration: data.duration || '',
        totalBudget: 0,
        createdAt: data.createdAt || '',
        attachments: []
      }

      // Set attachments if available
      if (data.reviewFileName) {
        attachments.value.push({ name: data.reviewFileName, url: '' })
      }
      if (data.roadmapFileName) {
        attachments.value.push({ name: data.roadmapFileName, url: '' })
      }
      if (data.gadFileName) {
        attachments.value.push({ name: data.gadFileName, url: '' })
      }
    } catch (err) {
      console.error(err)
      error.value = 'Failed to load proposal details.'
    } finally {
      loading.value = false
    }
  }
})
</script>

<style>
.meeting-container {
  padding: 25px;
  background: #f5f6f8;
}

.meeting-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.title {
  margin: 0;
}

.status {
  background: #ffd803;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: bold;
  margin-left: 10px;
}

.proposal-id {
  color: #666;
  margin-top: 6px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.content-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

.card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.label {
  font-weight: bold;
  color: #777;
}

.meeting-item {
  display: flex;
  gap: 12px;
  margin-bottom: 15px;
}
.icon {
  width: 36px;
  height: 36px;
  background: #d4f4dd;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attachments {
  list-style: none;
  padding: 0;
}

.attachments li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.download {
  background: #f1f1f1;
  border: none;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
}

.btn {
  padding: 10px 16px;
  border-radius: 8px;
  border: none;
  font-weight: bold;
  cursor: pointer;
}

.btn.yellow {
  background: #ffd803;
}

.btn.outline {
  background: #2563eb;
  border: 1px solid #ccc;
}
</style>
