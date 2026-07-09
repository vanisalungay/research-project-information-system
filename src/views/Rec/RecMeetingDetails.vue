<template>
  <div class="meeting-container">
    <!-- HEADER -->
    <div class="meeting-header">
      <div>
        <h2 class="title">
          {{ proposal.projectTitle || 'Untitled Proposal' }}
        </h2>

        <span class="status">
          {{ proposal.status || 'N/A' }}
        </span>

        <p class="proposal-id">
          Proposal ID: #{{ proposal.id || 'N/A' }}
        </p>
      </div>

      <div class="header-actions">
        <button class="btn yellow" @click="goToDetailed">
          See Detailed Proposal
        </button>

        <button class="btn outline" @click="downloadAll">
          Download All Documents
        </button>
      </div>
    </div>

    <!-- LOADING -->
    <div v-if="loading" class="loading-state">
      <p>Loading proposal details...</p>
    </div>

    <!-- ERROR -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="goBack">
        Go Back
      </button>
    </div>

    <!-- CONTENT -->
    <div v-else class="content-grid">
      <!-- LEFT -->
      <div class="left">

        <!-- PROJECT PROFILE -->
        <div class="card">
          <h3>Project Profile</h3>

          <div class="info-grid">
            <div>
              <p class="label">Program Title</p>
              <p>{{ proposal.programTitle || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Project Title</p>
              <p>{{ proposal.projectTitle || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Project Leader</p>
              <p>
                {{ proposal.projectLeader || proposal.proponent?.name || 'N/A' }}
              </p>
            </div>

            <div>
              <p class="label">College / Department</p>
              <p>{{ proposal.college || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Duration</p>
              <p>{{ proposal.duration || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Research Type</p>
              <p>{{ proposal.researchType || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Start Date</p>
              <p>{{ proposal.startDate || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">End Date</p>
              <p>{{ proposal.endDate || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Submitted Date</p>
              <p>{{ proposal.createdAt || 'N/A' }}</p>
            </div>

            <div>
              <p class="label">Total Budget</p>
              <p>
                ₱{{ proposal.totalBudget?.toLocaleString() || '0.00' }}
              </p>
            </div>
          </div>
        </div>

        <!-- COOPERATING AGENCIES -->
        <div class="card">
          <h3>Cooperating Agencies</h3>
          <p>{{ proposal.cooperatingAgencies || 'Not provided' }}</p>
        </div>

        <!-- SITES -->
        <div class="card">
          <h3>Sites of Implementation</h3>

          <table v-if="proposal.sites && proposal.sites.length" class="proposal-table">
            <thead>
              <tr>
                <th>Country</th>
                <th>Region</th>
                <th>Province</th>
                <th>Municipality</th>
                <th>Barangay</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(site, i) in proposal.sites" :key="i">
                <td>{{ site.country }}</td>
                <td>{{ site.region }}</td>
                <td>{{ site.province }}</td>
                <td>{{ site.municipality }}</td>
                <td>{{ site.barangay }}</td>
              </tr>
            </tbody>
          </table>

          <p v-else>
            No implementation sites provided.
          </p>
        </div>

        <!-- EXECUTIVE SUMMARY -->
        <div class="card">
          <h3>Executive Summary</h3>
          <p>{{ proposal.executiveSummary || 'Not provided' }}</p>
        </div>

        <!-- METHODOLOGY -->
        <div class="card">
          <h3>Methodology</h3>
          <p>{{ proposal.methodology || 'Not provided' }}</p>
        </div>

        <!-- EXPECTED OUTPUTS -->
        <div class="card">
          <h3>Expected Outputs</h3>
          <p>{{ proposal.expectedOutputs || 'Not provided' }}</p>
        </div>

        <!-- MEETING DETAILS -->
        <div class="card">
          <h3>Meeting Details</h3>

          <div class="meeting-item">
            <div class="icon green">📍</div>
            <div>
              <p class="label">Venue</p>
              <p>{{ proposal.meetingVenue || 'Not scheduled yet' }}</p>
            </div>
          </div>

          <div class="meeting-item">
            <div class="icon green">👤</div>
            <div>
              <p class="label">Assigned By</p>
              <p>{{ proposal.assignedBy || 'N/A' }}</p>
            </div>
          </div>

          <div class="meeting-item">
            <div class="icon green">🕒</div>
            <div>
              <p class="label">Meeting Date</p>
              <p>{{ proposal.meetingDate || 'Not scheduled yet' }}</p>
              <small>{{ proposal.meetingTime || '' }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- RIGHT -->
      <div class="right">
        <div class="card">
          <h3>Attachments</h3>

          <ul class="attachments">
            <li v-if="attachments.length === 0" style="color:#999;font-style:italic;">
              No attachments available
            </li>

            <li v-for="file in attachments" :key="file.name">
              <span>📄 {{ file.name }}</span>

              <button class="download" @click="downloadFile(file)">
                ⬇️
              </button>
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

const loading = ref(true)
const error = ref('')

const proposal = ref({
  id: null,
  projectTitle: '',
  programTitle: '',
  projectLeader: '',
  college: '',
  duration: '',
  totalBudget: '',
  createdAt: '',
  status: '',
  proponent: null,
  contactPerson: '',
  email: '',
  phone: '',
  venue: '',
  assignedBy: '',
  meetingDate: '',
  meetingTime: ''
})

const attachments = ref([])

const goBack = () => {
  router.back()
}

const goToDetailed = () => {
  if (!proposal.value.id) return

  router.push({
    path: '/detailed-proposal',
    query: {
      id: proposal.value.id
    }
  })
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
  attachments.value.forEach(file => {
    downloadFile(file)
  })
}

const loadProposal = async () => {
  const proposalId = route.params.id || route.query.id

  if (!proposalId) {
    error.value = 'No proposal ID found.'
    loading.value = false
    return
  }

  try {
    const res = await api.get(`/api/proposals/${proposalId}`)
    const data = res.data

    proposal.value = {
      id: data.id,
      projectTitle: data.projectTitle || '',
      programTitle: data.programTitle || '',
      projectLeader:
        data.projectLeader ||
        data.proponent?.name ||
        '',
      college: data.college || '',
      duration: data.duration || '',
      totalBudget: data.totalBudget || '',
      createdAt: data.createdAt || '',
      status: data.status || '',
      proponent: data.proponent || null,
      contactPerson:
        data.contactPerson ||
        data.projectLeader ||
        '',
      email:
        data.email ||
        data.proponent?.email ||
        '',
      phone: data.phone || '',
      venue: data.venue || '',
      assignedBy: data.assignedBy || '',
      meetingDate: data.meetingDate || '',
      meetingTime: data.meetingTime || ''
    }

    attachments.value = []

    if (data.proposalFileName) {
      attachments.value.push({
        name: data.proposalFileName,
        url: `http://localhost:8081/uploads/${data.proposalFileName}`
      })
    }

    if (data.reviewFileName) {
      attachments.value.push({
        name: data.reviewFileName,
        url: `http://localhost:8081/uploads/${data.reviewFileName}`
      })
    }

    if (data.roadmapFileName) {
      attachments.value.push({
        name: data.roadmapFileName,
        url: `http://localhost:8081/uploads/${data.roadmapFileName}`
      })
    }

    if (data.gadFileName) {
      attachments.value.push({
        name: data.gadFileName,
        url: `http://localhost:8081/uploads/${data.gadFileName}`
      })
    }
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadProposal()
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
