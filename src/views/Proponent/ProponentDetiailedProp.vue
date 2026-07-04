<template>
  <div class="proposal-detail-page">
    <!-- HEADER -->
    <header class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.go(-1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          Back
        </button>
        <div class="header-info">
          <h1>{{ proposal.projectTitle || 'Proposal Details' }}</h1>
          <div class="header-meta">
            <span class="proposal-id">ID: {{ proposal.id }}</span>
            <span class="status-badge" :class="(proposal.status || '').toLowerCase()">
              {{ proposal.status || 'DRAFT' }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button class="btn-secondary" @click="printProposal">Print</button>
      </div>
    </header>

    <!-- Loading State -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- Error State -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="$router.go(-1)">Go Back</button>
    </div>

    <!-- Content -->
    <template v-else-if="proposal.id">
      <div class="content-grid">
        <!-- LEFT COLUMN: Main Info -->
        <div class="main-content">
          <!-- Project Profile Card -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">01</span>
              <h2>Project Profile</h2>
            </div>
            <div class="card-body">
              <div class="info-grid">
                <div class="info-item">
                  <label>Program Title</label>
                  <p>{{ proposal.programTitle || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Project Title</label>
                  <p>{{ proposal.projectTitle || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Project Leader</label>
                  <p>{{ proposal.projectLeader || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>Duration</label>
                  <p>{{ proposal.duration || 'Not provided' }} months</p>
                </div>
                <div class="info-item">
                  <label>Start Date</label>
                  <p>{{ proposal.startDate || 'Not provided' }}</p>
                </div>
                <div class="info-item">
                  <label>End Date</label>
                  <p>{{ proposal.endDate || 'Not provided' }}</p>
                </div>
                <div class="info-item full-width">
                  <label>Implementing College / Department</label>
                  <p>{{ proposal.college || 'Not provided' }}</p>
                </div>
                <div class="info-item full-width">
                  <label>Address / Contact</label>
                  <p>{{ proposal.address || 'Not provided' }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- Cooperating Agencies -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">02</span>
              <h2>Cooperating Agencies</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.cooperatingAgencies || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Sites of Implementation -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">03</span>
              <h2>Sites of Implementation</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.sites && proposal.sites.length > 0" class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>#</th>
                      <th>Country</th>
                      <th>Region</th>
                      <th>Province</th>
                      <th>District</th>
                      <th>Municipality</th>
                      <th>Barangay</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(site, index) in proposal.sites" :key="index">
                      <td>{{ index + 1 }}</td>
                      <td>{{ site.country }}</td>
                      <td>{{ site.region }}</td>
                      <td>{{ site.province }}</td>
                      <td>{{ site.district }}</td>
                      <td>{{ site.municipality }}</td>
                      <td>{{ site.barangay }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No implementation sites available.</p>
            </div>
          </section>

          <!-- Research Type & Priority Agenda -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">04</span>
              <h2>Type of Research</h2>
            </div>
            <div class="card-body">
              <span class="type-badge" :class="(proposal.researchType || '').toLowerCase()">
                {{ proposal.researchType || 'Not specified' }}
              </span>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">05</span>
              <h2>Priority Agenda</h2>
            </div>
            <div class="card-body">
              <div v-if="hasPriorityAgendas" class="agenda-list">
                <div v-if="proposal.priorityAgendas?.dagat?.selected" class="agenda-item">
                  <strong>DAGAT:</strong> {{ proposal.priorityAgendas.dagat.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.punla?.selected" class="agenda-item">
                  <strong>PUNLA:</strong> {{ proposal.priorityAgendas.punla.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.kalikasan?.selected" class="agenda-item">
                  <strong>KALIKASAN:</strong> {{ proposal.priorityAgendas.kalikasan.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.negosyo?.selected" class="agenda-item">
                  <strong>NEGOSYO:</strong> {{ proposal.priorityAgendas.negosyo.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.tanglaw?.selected" class="agenda-item">
                  <strong>TANGLAW:</strong> {{ proposal.priorityAgendas.tanglaw.value }}
                </div>
              </div>
              <p v-else class="empty-text">No priority agenda selected.</p>
            </div>
          </section>

          <!-- Text Sections -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">06</span>
              <h2>Innovation Goals</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.innovationGoals || 'Not provided' }}</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">07</span>
              <h2>Sector Relevance</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sectorRelevance || 'Not provided' }}</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">08</span>
              <h2>Sustainable Development Goals</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sdg || 'Not provided' }}</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">09</span>
              <h2>Executive Summary</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.executiveSummary || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Introduction -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">10</span>
              <h2>Introduction</h2>
            </div>
            <div class="card-body">
              <div class="subsection">
                <h3>10.1 Rationale / Significance</h3>
                <p class="text-content">{{ proposal.rationale || 'Not provided' }}</p>
              </div>
              <div class="subsection">
                <h3>10.2 Scientific Basis / Theoretical Framework</h3>
                <p class="text-content">{{ proposal.framework || 'Not provided' }}</p>
              </div>
              <div class="subsection">
                <h3>10.3 Objectives</h3>
                <div class="objectives">
                  <div>
                    <strong>General Objective:</strong>
                    <p>{{ proposal.objectivesGeneral || 'Not provided' }}</p>
                  </div>
                  <div>
                    <strong>Specific Objectives:</strong>
                    <p>{{ proposal.objectivesSpecific || 'Not provided' }}</p>
                  </div>
                </div>
              </div>
            </div>
          </section>

          <!-- Documents Section -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">11</span>
              <h2>Review of Literature</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.reviewFileName" class="file-attachment">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <p class="file-name">{{ proposal.reviewFileName }}</p>
                  <p class="file-type">PDF Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.reviewFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No file uploaded.</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">12</span>
              <h2>Methodology</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.methodology || 'Not provided' }}</p>
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">13</span>
              <h2>Technology Roadmap</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.roadmapFileName" class="file-attachment">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <p class="file-name">{{ proposal.roadmapFileName }}</p>
                  <p class="file-type">PDF Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.roadmapFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No file uploaded.</p>
            </div>
          </section>
        </div>

        <!-- RIGHT COLUMN: Sidebar -->
        <aside class="sidebar">
          <!-- Status Card -->
          <div class="sidebar-card status-card">
            <h3>Proposal Status</h3>
            <div class="status-indicator" :class="(proposal.status || '').toLowerCase()">
              <span class="status-dot"></span>
              <span class="status-text">{{ proposal.status || 'DRAFT' }}</span>
            </div>
            <div class="status-meta">
              <p><strong>Created:</strong> {{ formatDate(proposal.createdAt) }}</p>
              <p><strong>Last Updated:</strong> {{ formatDate(proposal.updatedAt) }}</p>
            </div>
          </div>

          <!-- Quick Info Card -->
          <div class="sidebar-card">
            <h3>Quick Information</h3>
            <div class="quick-info-list">
              <div class="quick-info-item">
                <span class="info-label">Project Leader</span>
                <span class="info-value">{{ proposal.projectLeader || 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">College</span>
                <span class="info-value">{{ proposal.college || 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">Duration</span>
                <span class="info-value">{{ proposal.duration ? proposal.duration + ' months' : 'N/A' }}</span>
              </div>
              <div class="quick-info-item">
                <span class="info-label">Research Type</span>
                <span class="info-value">{{ proposal.researchType || 'N/A' }}</span>
              </div>
            </div>
          </div>

          <!-- Documents Card -->
          <div class="sidebar-card">
            <h3>Documents</h3>
            <div class="document-list">
              <div v-if="proposal.reviewFileName" class="doc-item">
                <span>📄 Review of Literature</span>
                <button @click="downloadFile(proposal.reviewFileName)">⬇</button>
              </div>
              <div v-if="proposal.roadmapFileName" class="doc-item">
                <span>📄 Technology Roadmap</span>
                <button @click="downloadFile(proposal.roadmapFileName)">⬇</button>
              </div>
              <div v-if="proposal.gadFileName" class="doc-item">
                <span>📄 GAD Score</span>
                <button @click="downloadFile(proposal.gadFileName)">⬇</button>
              </div>
              <div v-if="proposal.beneficiariesFileName" class="doc-item">
                <span>📄 Beneficiaries</span>
                <button @click="downloadFile(proposal.beneficiariesFileName)">⬇</button>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'

const route = useRoute()
const proposal = ref({})
const loading = ref(true)
const error = ref(null)

const hasPriorityAgendas = computed(() => {
  if (!proposal.value.priorityAgendas) return false
  return Object.values(proposal.value.priorityAgendas).some(agenda => agenda.selected)
})

const fetchProposal = async () => {
  loading.value = true
  error.value = null
  try {
    const proposalId = route.query.id
    if (!proposalId) {
      error.value = 'No proposal ID provided.'
      return
    }
    const res = await api.get(`/api/proposals/${proposalId}`)
    proposal.value = res.data
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchProposal)

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const downloadFile = (fileName) => {
  if (!fileName) return
  const link = document.createElement('a')
  link.href = `http://localhost:8081/uploads/${fileName}`
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const printProposal = () => {
  window.print()
}
</script>

<style scoped>
.proposal-detail-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  gap: 16px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  flex: 1;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
  margin-top: 4px;
}

.back-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.header-info {
  flex: 1;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 8px 0;
}

.header-meta {
  display: flex;
  gap: 12px;
  align-items: center;
}

.proposal-id {
  font-size: 13px;
  color: #64748b;
  font-family: monospace;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.draft { background: #e5e7eb; color: #374151; }
.status-badge.submitted { background: #f3e8ff; color: #6b21a8; }
.status-badge.under_review { background: #ede9fe; color: #6d28d9; }
.status-badge.approved { background: #dcfce7; color: #166534; }
.status-badge.rejected { background: #fee2e2; color: #991b1b; }
.status-badge.revision { background: #e0e7ff; color: #3730a3; }

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-secondary {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #f1f5f9;
}

.btn-primary {
  background: #4f46e5;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
}

/* LOADING & ERROR */
.loading-state, .error-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e2e8f0;
  border-top-color: #4f46e5;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-state p {
  color: #dc2626;
  font-size: 14px;
}

/* CONTENT GRID */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 24px;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* CONTENT CARD */
.content-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  overflow: hidden;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.card-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  background: #4f46e5;
  color: #fff;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.card-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.card-body {
  padding: 20px;
}

/* INFO GRID */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item.full-width {
  grid-column: 1 / -1;
}

.info-item label {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.info-item p {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
  margin: 0;
}

/* TEXT CONTENT */
.text-content {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin: 0;
  white-space: pre-wrap;
}

/* SUBSECTION */
.subsection {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.subsection:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.subsection h3 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px 0;
}

.objectives {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.objectives div {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.objectives strong {
  font-size: 13px;
  color: #64748b;
}

.objectives p {
  font-size: 14px;
  color: #1e293b;
  margin: 0;
  line-height: 1.6;
}

/* TYPE BADGE */
.type-badge {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  background: #eef2ff;
  color: #4f46e5;
}

.type-badge.basic {
  background: #dbeafe;
  color: #1e40af;
}

.type-badge.applied {
  background: #d1fae5;
  color: #065f46;
}

/* AGENDA LIST */
.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.agenda-item {
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border-left: 3px solid #4f46e5;
}

.agenda-item strong {
  color: #4f46e5;
  font-size: 13px;
}

.agenda-item span {
  font-size: 14px;
  color: #1e293b;
}

/* TABLES */
.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table thead {
  background: #f8fafc;
}

.data-table th {
  padding: 10px 12px;
  text-align: left;
  font-weight: 600;
  color: #475569;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f1f5f9;
  color: #1e293b;
}

/* FILE ATTACHMENT */
.file-attachment {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.file-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 10px;
  font-size: 24px;
  border: 1px solid #e2e8f0;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 2px 0;
}

.file-type {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.btn-download {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:hover {
  background: #4338ca;
}

/* SIDEBAR */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.sidebar-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 20px;
}

.sidebar-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 16px 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

/* STATUS CARD */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 10px;
  margin-bottom: 12px;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: currentColor;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-indicator.draft { background: #f1f5f9; color: #64748b; }
.status-indicator.submitted { background: #f3e8ff; color: #6b21a8; }
.status-indicator.under_review { background: #ede9fe; color: #6d28d9; }
.status-indicator.approved { background: #dcfce7; color: #166534; }
.status-indicator.rejected { background: #fee2e2; color: #991b1b; }
.status-indicator.revision { background: #e0e7ff; color: #3730a3; }

.status-meta {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #64748b;
}

.status-meta p {
  margin: 0;
}

.status-meta strong {
  color: #475569;
}

/* QUICK INFO */
.quick-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.quick-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 500;
}

/* DOCUMENT LIST */
.document-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.doc-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 8px;
  font-size: 13px;
  color: #475569;
}

.doc-item button {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  padding: 4px;
  transition: transform 0.15s;
}

.doc-item button:hover {
  transform: scale(1.2);
}

/* EMPTY TEXT */
.empty-text {
  color: #94a3b8;
  font-style: italic;
  font-size: 14px;
}

/* RESPONSIVE */
@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }

  .sidebar {
    order: -1;
  }
}

@media (max-width: 640px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .header-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>