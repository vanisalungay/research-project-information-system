<template>
  <div class="proposal-detail-page">
    <!-- HEADER -->
    <header class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"></polyline>
          </svg>
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
        <button class="btn-secondary" @click="downloadAll">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          Download All Documents
        </button>
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
      <button class="btn-primary" @click="goBack">Go Back</button>
    </div>

    <!-- Content -->
    <template v-else-if="proposal.id">
      <div class="content-grid">
        <!-- LEFT COLUMN: Main Content -->
        <div class="main-content">
          <!-- Project Profile -->
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
                  <label>Sex</label>
                  <p>{{ proposal.projectLeaderSex || 'Not provided' }}</p>
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

          <!-- Research Type -->
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

          <!-- Priority Agenda -->
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
                <div v-if="proposal.priorityAgendas?.agriculture?.selected" class="agenda-item">
                  <strong>Agriculture:</strong> {{ proposal.priorityAgendas.agriculture.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.environment?.selected" class="agenda-item">
                  <strong>Environment:</strong> {{ proposal.priorityAgendas.environment.value }}
                </div>
                <div v-if="proposal.priorityAgendas?.health?.selected" class="agenda-item">
                  <strong>Health:</strong> {{ proposal.priorityAgendas.health.value }}
                </div>
              </div>
              <p v-else class="empty-text">No priority agenda selected.</p>
            </div>
          </section>

          <!-- Innovation Goals -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">06</span>
              <h2>Innovation Goals</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.innovationGoals || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Sector Relevance -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">07</span>
              <h2>Sector Relevance</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sectorRelevance || 'Not provided' }}</p>
            </div>
          </section>

          <!-- SDG -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">08</span>
              <h2>Sustainable Development Goals</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sdg || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Executive Summary -->
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

          <!-- Review of Literature -->
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

          <!-- Methodology -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">12</span>
              <h2>Methodology</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.methodology || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Technology Roadmap -->
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

          <!-- Expected Outputs -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">14</span>
              <h2>Expected Outputs (6Ps)</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.outputs || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Potential Outcomes -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">15</span>
              <h2>Potential Outcomes</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.outcomes || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Potential Impacts -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">16</span>
              <h2>Potential Impacts (2Is)</h2>
            </div>
            <div class="card-body">
              <div class="subsection">
                <h3>a. Economic Impact</h3>
                <p class="text-content">{{ proposal.impactEconomic || 'Not provided' }}</p>
              </div>
              <div class="subsection">
                <h3>b. Social / Ethical Impact</h3>
                <p class="text-content">{{ proposal.impactSocial || 'Not provided' }}</p>
              </div>
            </div>
          </section>

          <!-- Target Beneficiaries -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">17</span>
              <h2>Target Beneficiaries</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.beneficiaries || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Sustainability Plan -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">18</span>
              <h2>Sustainability Plan</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sustainability || 'Not provided' }}</p>
            </div>
          </section>

          <!-- GAD Score -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">19</span>
              <h2>Gender and Development (GAD) Score</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.gadFileName" class="file-attachment">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <p class="file-name">{{ proposal.gadFileName }}</p>
                  <p class="file-type">PDF Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.gadFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No file uploaded.</p>
            </div>
          </section>

          <!-- Limitations -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">20</span>
              <h2>Limitations of the Project</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.limitations || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Risks -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">21</span>
              <h2>Risk Management Plan</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.risks || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Logical Framework -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">22</span>
              <h2>Logical Framework</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.logFrame && proposal.logFrame.length > 0" class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Outcome Indicators</th>
                      <th>Output Indicators / Physical Target</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in proposal.logFrame" :key="index">
                      <td>{{ row.outcome || '-' }}</td>
                      <td>{{ row.output || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No logical framework data.</p>
            </div>
          </section>

          <!-- Literature Cited -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">23</span>
              <h2>Literature Cited</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.literatureCited || 'Not provided' }}</p>
            </div>
          </section>

          <!-- Personnel -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">24</span>
              <h2>Personnel Requirement</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.personnel && proposal.personnel.length > 0" class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Position</th>
                      <th>% Time Devoted</th>
                      <th>Responsibilities</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in proposal.personnel" :key="index">
                      <td>{{ row.position || '-' }}</td>
                      <td>{{ row.time || '-' }}</td>
                      <td>{{ row.responsibilities || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No personnel data.</p>
            </div>
          </section>

          <!-- Budget -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">25</span>
              <h2>Line-Item Budget Requirement</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.budgetFileName" class="file-attachment">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <p class="file-name">{{ proposal.budgetFileName }}</p>
                  <p class="file-type">Budget Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.budgetFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No budget file uploaded.</p>
            </div>
          </section>

          <!-- Other Projects -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">26</span>
              <h2>Other Ongoing Projects</h2>
            </div>
            <div class="card-body">
              <p v-if="proposal.otherProjectsNumber" class="text-content" style="margin-bottom: 12px;">
                <strong>Number of other projects:</strong> {{ proposal.otherProjectsNumber }}
              </p>
              <div v-if="proposal.otherProjects && proposal.otherProjects.length > 0" class="table-wrapper">
                <table class="data-table">
                  <thead>
                    <tr>
                      <th>Title</th>
                      <th>Funding Agency</th>
                      <th>Involvement</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in proposal.otherProjects" :key="index">
                      <td>{{ row.title || '-' }}</td>
                      <td>{{ row.agency || '-' }}</td>
                      <td>{{ row.involvement || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No other projects.</p>
            </div>
          </section>

          <!-- Other Supporting Documents -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">27</span>
              <h2>Other Supporting Documents</h2>
            </div>
            <div class="card-body">
              <p class="text-content">Please refer to the attached documents for additional necessary documents.</p>
            </div>
          </section>
        </div>

        <!-- RIGHT COLUMN: Sidebar -->
        <aside class="sidebar">
          <!-- Status Card -->
          <div class="sidebar-card">
            <h3>Proposal Status</h3>
            <div class="status-indicator" :class="(proposal.status || '').toLowerCase()">
              <span class="status-dot"></span>
              <span class="status-text">{{ proposal.status || 'DRAFT' }}</span>
            </div>
            <div class="status-meta">
              <p><strong>Created:</strong> {{ formatDate(proposal.createdAt) }}</p>
              <p><strong>Updated:</strong> {{ formatDate(proposal.updatedAt) }}</p>
            </div>
          </div>

          <!-- Quick Info -->
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

          <!-- Review Information -->
          <div v-if="proposal.reviewedBy" class="sidebar-card review-info-card">
            <h3>Review Information</h3>
            <div class="review-info-list">
              <div class="review-info-item">
                <span class="review-info-label">Reviewed By:</span>
                <span class="review-info-value">{{ proposal.reviewedBy }}</span>
              </div>
              <div class="review-info-item">
                <span class="review-info-label">Position:</span>
                <span class="review-info-value reviewer-position">
                  {{ formatReviewerPosition(proposal.reviewedByPosition) }}
                </span>
              </div>
            </div>
          </div>

          <!-- Status Timeline -->
          <div class="sidebar-card">
            <h3>Status Timeline</h3>
            <ul class="timeline">
              <li>
                <span class="dot completed"></span>
                <div>
                  <strong>Proposal Submitted</strong>
                  <small>{{ formatDate(proposal.createdAt) }}</small>
                </div>
              </li>
              <li v-if="proposal.status !== 'SUBMITTED'" :class="{ active: proposal.status === 'ENDORSED' }">
                <span class="dot"
                  :class="{ completed: ['ENDORSED', 'UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>RPS Endorsed</strong>
                  <small>{{ proposal.status === 'ENDORSED' || ['UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL',
                    'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) ? 'Completed' : 'Pending' }}</small>
                </div>
              </li>
              <li
                v-if="['UNDER_REVIEW', 'REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'UNDER_REVIEW' }">
                <span class="dot"
                  :class="{ completed: ['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>Forwarded to REC</strong>
                  <small>{{ ['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL',
                    'APPROVED'].includes(proposal.status) ? 'Completed' : 'In Progress' }}</small>
                </div>
              </li>
              <li v-if="['REC_APPROVED', 'FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'REC_APPROVED' }">
                <span class="dot"
                  :class="{ completed: ['FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) }"></span>
                <div>
                  <strong>REC Approved</strong>
                  <small>{{ ['FOR_OVCAF_APPROVAL', 'FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status) ?
                    'Completed' : 'Current' }}</small>
                </div>
              </li>
              <li v-if="['FOR_OC_APPROVAL', 'APPROVED'].includes(proposal.status)"
                :class="{ active: proposal.status === 'FOR_OC_APPROVAL' }">
                <span class="dot" :class="{ completed: proposal.status === 'APPROVED' }"></span>
                <div>
                  <strong>Forwarded to Chancellor</strong>
                  <small>{{ proposal.status === 'APPROVED' ? 'Completed' : 'In Progress' }}</small>
                </div>
              </li>
              <li v-if="proposal.status === 'APPROVED'" class="active">
                <span class="dot completed"></span>
                <div>
                  <strong>Final Approval</strong>
                  <small>Completed</small>
                </div>
              </li>
            </ul>
          </div>

          <!-- OVCRIGE Actions -->
          <div class="sidebar-card action-card">
            <h3>OVCRIGE Actions</h3>
            <button v-if="proposal.status === 'ENDORSED'" class="btn-action btn-primary" @click="forwardToRec"
              :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Forward to REC for Evaluation' }}
            </button>
            <button v-if="proposal.status === 'REC_APPROVED'" class="btn-action btn-primary" @click="forwardToOc"
              :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Forward to Chancellor (OC) for Final Approval' }}
            </button>
            <button class="btn-action btn-warning" @click="returnForRevision" :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Return for Revision' }}
            </button>
            <button class="btn-action btn-danger" @click="rejectProposal" :disabled="actionLoading">
              {{ actionLoading ? 'Processing...' : 'Reject Proposal' }}
            </button>
          </div>

          <!-- Attachments -->
          <div class="sidebar-card">
            <h3>Attachments</h3>
            <ul class="files-list">
              <li v-if="proposal.reviewFileName" class="file-item">
                <span>📄 Review of Literature</span>
                <button class="download-btn" @click="downloadFile(proposal.reviewFileName)">⬇️</button>
              </li>
              <li v-if="proposal.roadmapFileName" class="file-item">
                <span>📄 Technology Roadmap</span>
                <button class="download-btn" @click="downloadFile(proposal.roadmapFileName)">⬇️</button>
              </li>
              <li v-if="proposal.gadFileName" class="file-item">
                <span>📄 GAD Score</span>
                <button class="download-btn" @click="downloadFile(proposal.gadFileName)">⬇️</button>
              </li>
              <li v-if="proposal.budgetFileName" class="file-item">
                <span>📄 Budget Document</span>
                <button class="download-btn" @click="downloadFile(proposal.budgetFileName)">⬇️</button>
              </li>
              <li
                v-if="!proposal.reviewFileName && !proposal.roadmapFileName && !proposal.gadFileName && !proposal.budgetFileName"
                class="empty-files">
                No attachments uploaded.
              </li>
            </ul>
          </div>
        </aside>
      </div>
    </template>

    <!-- SUCCESS MODAL -->
    <div v-if="showSuccess" class="modal-overlay">
      <div class="modal-box">
        <p>{{ successMessage }}</p>
        <button class="btn-primary" @click="showSuccess = false; $router.push('/submit-proposals')">OK</button>
      </div>
    </div>

    <!-- ERROR MODAL -->
    <div v-if="showError" class="modal-overlay">
      <div class="modal-box">
        <p class="error-text">{{ errorMessage }}</p>
        <button class="btn-primary" @click="showError = false">OK</button>
      </div>
    </div>

    <!-- CONFIRM DIALOG -->
    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

const router = useRouter()
const route = useRoute()

const proposal = ref({})
const loading = ref(true)
const error = ref(null)
const actionLoading = ref(false)
const showSuccess = ref(false)
const showError = ref(false)
const successMessage = ref('')
const errorMessage = ref('')

const hasPriorityAgendas = computed(() => {
  if (!proposal.value.priorityAgendas) return false
  return Object.values(proposal.value.priorityAgendas).some(agenda => agenda.selected)
})

const loadProposal = async () => {
  loading.value = true
  error.value = null
  try {
    const proposalId = route.params.id
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

onMounted(loadProposal)

const forwardToRec = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-rec`)
    successMessage.value = 'Proposal has been forwarded to REC for evaluation.'
    showSuccess.value = true
    proposal.value.status = 'UNDER_REVIEW'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const forwardToOc = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/forward-to-oc`)
    successMessage.value = 'Proposal has been forwarded to the Chancellor (OC) for final approval.'
    showSuccess.value = true
    proposal.value.status = 'FOR_OC_APPROVAL'
  } catch (err) {
    errorMessage.value = 'Failed to forward proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const returnForRevision = async () => {
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/return-revision`)
    successMessage.value = 'Proposal has been returned to the proponent for revision.'
    showSuccess.value = true
    proposal.value.status = 'REVISION'
  } catch (err) {
    errorMessage.value = 'Failed to return proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
}

const rejectProposal = async () => {
  const confirmed = await showConfirm('Are you sure you want to reject this proposal?', {
    title: 'Reject Proposal',
    type: 'danger',
    confirmText: 'Reject'
  })
  if (!confirmed) return
  actionLoading.value = true
  try {
    const proposalId = route.params.id
    await api.put(`/api/proposals/${proposalId}/reject`)
    successMessage.value = 'Proposal has been rejected.'
    showSuccess.value = true
    proposal.value.status = 'REJECTED'
  } catch (err) {
    errorMessage.value = 'Failed to reject proposal. Please try again.'
    showError.value = true
  } finally {
    actionLoading.value = false
  }
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

const downloadAll = () => {
  const files = [
    proposal.value.reviewFileName,
    proposal.value.roadmapFileName,
    proposal.value.gadFileName,
    proposal.value.budgetFileName
  ].filter(Boolean)
  files.forEach((file, index) => {
    setTimeout(() => downloadFile(file), index * 300)
  })
}

const goBack = () => {
  router.back()
}

const formatDate = (dateString) => {
  if (!dateString) return 'N/A'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

const formatReviewerPosition = (position) => {
  if (!position) return 'N/A'
  const positionMap = {
    'RPS_STAFF': 'RPS Staff',
    'RPS_DIRECTOR': 'RPS Director'
  }
  return positionMap[position] || position
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

.status-badge.draft {
  background: #e5e7eb;
  color: #374151;
}

.status-badge.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.status-badge.endorsed {
  background: #bbdefb;
  color: #1565c0;
}

.status-badge.under_review {
  background: #ede9fe;
  color: #6d28d9;
}

.status-badge.rec_approved {
  background: #c8e6c9;
  color: #2e7d32;
}

.status-badge.for_ovcaf_approval {
  background: #fff3cd;
  color: #856404;
}

.status-badge.for_oc_approval {
  background: #ffe0b2;
  color: #e65100;
}

.status-badge.approved {
  background: #dcfce7;
  color: #166534;
}

.status-badge.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-badge.revision {
  background: #e0e7ff;
  color: #3730a3;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.btn-secondary {
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
.loading-state,
.error-state {
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
  to {
    transform: rotate(360deg);
  }
}

.error-state p {
  color: #dc2626;
  font-size: 14px;
}

/* CONTENT GRID */
.content-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
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
  gap: 16px;
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
  margin: 0;
}

/* TEXT CONTENT */
.text-content {
  font-size: 14px;
  color: #334155;
  line-height: 1.6;
  white-space: pre-wrap;
}

.empty-text {
  color: #94a3b8;
  font-style: italic;
  font-size: 14px;
}

/* TABLES */
.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
  text-transform: uppercase;
}

.data-table td {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  font-size: 14px;
  color: #334155;
}

/* SUBSECTIONS */
.subsection {
  margin-bottom: 20px;
}

.subsection:last-child {
  margin-bottom: 0;
}

.subsection h3 {
  font-size: 14px;
  font-weight: 600;
  color: #475569;
  margin: 0 0 8px 0;
}

.objectives>div {
  margin-bottom: 12px;
}

.objectives strong {
  display: block;
  font-size: 13px;
  color: #475569;
  margin-bottom: 4px;
}

/* TYPE BADGE */
.type-badge {
  display: inline-block;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.type-badge.basic {
  background: #dbeafe;
  color: #1e40af;
}

.type-badge.applied {
  background: #dcfce7;
  color: #166534;
}

/* AGENDA LIST */
.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.agenda-item {
  padding: 8px 12px;
  background: #f8fafc;
  border-radius: 6px;
  font-size: 14px;
}

.agenda-item strong {
  color: #4f46e5;
}

/* FILE ATTACHMENT */
.file-attachment {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f8fafc;
  border-radius: 8px;
}

.file-icon {
  font-size: 24px;
}

.file-info {
  flex: 1;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  margin: 0;
}

.file-type {
  font-size: 12px;
  color: #64748b;
  margin: 0;
}

.btn-download {
  padding: 6px 12px;
  background: #4f46e5;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
}

.btn-download:hover {
  background: #4338ca;
}

/* SIDEBAR */
.sidebar {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 16px;
}

.sidebar-card h3 {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 12px 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

/* STATUS INDICATOR */
.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.status-indicator.submitted {
  background: #f3e8ff;
}

.status-indicator.endorsed {
  background: #bbdefb;
}

.status-indicator.under_review {
  background: #ede9fe;
}

.status-indicator.rec_approved {
  background: #c8e6c9;
}

.status-indicator.approved {
  background: #dcfce7;
}

.status-indicator.rejected {
  background: #fee2e2;
}

.status-indicator.revision {
  background: #e0e7ff;
}

.status-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #4f46e5;
}

.status-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.status-meta p {
  font-size: 13px;
  color: #64748b;
  margin: 4px 0;
}

/* QUICK INFO */
.quick-info-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: 12px;
  color: #64748b;
}

.info-value {
  font-size: 13px;
  font-weight: 500;
  color: #1e293b;
}

/* REVIEW INFO */
.review-info-card {
  background: #f0fdf4;
  border-color: #86efac;
}

.review-info-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.review-info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.review-info-label {
  font-size: 11px;
  color: #64748b;
  text-transform: uppercase;
}

.review-info-value {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.reviewer-position {
  color: #4f46e5;
}

/* TIMELINE */
.timeline {
  list-style: none;
  padding: 0;
  margin: 0;
}

.timeline li {
  display: flex;
  gap: 12px;
  padding-bottom: 16px;
  position: relative;
}

.timeline li:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 16px;
  bottom: 0;
  width: 2px;
  background: #e2e8f0;
}

.timeline li.active::before {
  background: #4f46e5;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #e2e8f0;
  flex-shrink: 0;
  margin-top: 4px;
}

.dot.completed {
  background: #22c55e;
}

.timeline li strong {
  display: block;
  font-size: 13px;
  color: #1e293b;
}

.timeline li small {
  font-size: 12px;
  color: #64748b;
}

/* ACTION CARD */
.action-card {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.btn-action {
  width: 100%;
  padding: 10px 14px;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-action.btn-primary {
  background: #4f46e5;
  color: #fff;
}

.btn-action.btn-primary:hover:not(:disabled) {
  background: #4338ca;
}

.btn-action.btn-warning {
  background: #f59e0b;
  color: #fff;
}

.btn-action.btn-warning:hover:not(:disabled) {
  background: #d97706;
}

.btn-action.btn-danger {
  background: #ef4444;
  color: #fff;
}

.btn-action.btn-danger:hover:not(:disabled) {
  background: #dc2626;
}

/* FILES LIST */
.files-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.file-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #f1f5f9;
  font-size: 13px;
}

.file-item:last-child {
  border-bottom: none;
}

.download-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
}

.empty-files {
  color: #94a3b8;
  font-style: italic;
  font-size: 13px;
  padding: 8px 0;
}

/* MODALS */
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
  padding: 24px;
  border-radius: 12px;
  text-align: center;
  width: 320px;
}

.modal-box p {
  margin-bottom: 16px;
  font-size: 14px;
  color: #334155;
}

.error-text {
  color: #dc2626;
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

  .page-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>