<template>
  <div class="proposal-detail-page">
    <!-- HEADER -->
    <header class="page-header">
      <div class="header-left">
        <button class="back-btn" @click="$router.go(-1)">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6" />
          </svg>
          Back
        </button>
        <div class="header-info">
          <h1>{{ proposal.projectTitle || 'Proposal Details' }}</h1>
          <div class="header-meta">
            <span class="document-id">{{ proposal.documentId || proposal.proposalCode || '—' }}</span>
            <span v-if="proposal.revisionNumber !== undefined" class="revision-badge">
              REV{{ String(proposal.revisionNumber).padStart(2, '0') }}
            </span>
            <span class="status-badge" :class="(proposal.status || '').toLowerCase()">
              {{ proposal.status || 'DRAFT' }}
            </span>
          </div>
        </div>
      </div>
      <div class="header-actions">
        <button v-if="proposal.status === 'DRAFT'" class="btn-edit-draft" @click="editDraft">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
          </svg>
          Edit Draft
        </button>
        <button class="btn-secondary" @click="printProposal">Print</button>
        <button
          v-if="isDuplicable(proposal.status)"
          class="btn-duplicate"
          :disabled="duplicating"
          @click="duplicateProposal"
        >
          {{ duplicating ? 'Duplicating...' : 'Duplicate Proposal' }}
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
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'programTitle'" field-label="Program Title" :remarks="remarksForField('programTitle')" readonly />
                </div>
                <div class="info-item">
                  <label>Project Title</label>
                  <p>{{ proposal.projectTitle || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'projectTitle'" field-label="Project Title" :remarks="remarksForField('projectTitle')" readonly />
                </div>
                <div class="info-item">
                  <label>Project Leader</label>
                  <p>{{ proposal.projectLeader || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'projectLeader'" field-label="Project Leader" :remarks="remarksForField('projectLeader')" readonly />
                </div>
                <div class="info-item">
                  <label>Sex</label>
                  <p>{{ proposal.projectLeaderSex || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'projectLeaderSex'" field-label="Sex" :remarks="remarksForField('projectLeaderSex')" readonly />
                </div>
                <div class="info-item">
                  <label>Duration</label>
                  <p>{{ proposal.duration || 'Not provided' }} months</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'duration'" field-label="Duration (months)" :remarks="remarksForField('duration')" readonly />
                </div>
                <div class="info-item">
                  <label>Start Date</label>
                  <p>{{ proposal.startDate || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'startDate'" field-label="Start Date" :remarks="remarksForField('startDate')" readonly />
                </div>
                <div class="info-item">
                  <label>End Date</label>
                  <p>{{ proposal.endDate || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'endDate'" field-label="End Date" :remarks="remarksForField('endDate')" readonly />
                </div>
                <div class="info-item full-width">
                  <label>Implementing College / Department</label>
                  <p>{{ proposal.college || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'college'" field-label="Implementing College / Department" :remarks="remarksForField('college')" readonly />
                </div>
                <div class="info-item full-width">
                  <label>Address / Contact</label>
                  <p>{{ proposal.address || 'Not provided' }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'01'" :field-key="'address'" field-label="Address / Contact" :remarks="remarksForField('address')" readonly />
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
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'02'" :field-key="'cooperatingAgencies'" field-label="Name/s and Address/es" :remarks="remarksForField('cooperatingAgencies')" readonly />
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
                      <td>{{ site.country }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].country`" field-label="Country" :remarks="remarksForField(`sites[${index}].country`)" readonly /></td>
                      <td>{{ site.region }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].region`" field-label="Region" :remarks="remarksForField(`sites[${index}].region`)" readonly /></td>
                      <td>{{ site.province }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].province`" field-label="Province" :remarks="remarksForField(`sites[${index}].province`)" readonly /></td>
                      <td>{{ site.district }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].district`" field-label="District" :remarks="remarksForField(`sites[${index}].district`)" readonly /></td>
                      <td>{{ site.municipality }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].municipality`" field-label="Municipality" :remarks="remarksForField(`sites[${index}].municipality`)" readonly /></td>
                      <td>{{ site.barangay }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'03'" :field-key="`sites[${index}].barangay`" field-label="Barangay" :remarks="remarksForField(`sites[${index}].barangay`)" readonly /></td>
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
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'04'" :field-key="'researchType'" field-label="Type of Research" :remarks="remarksForField('researchType')" readonly />
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
                <div v-for="agenda in proposal.priorityAgendas" :key="agenda.agendaKey" class="agenda-item">
                  <template v-if="agenda.selected">
                    <strong>{{ agenda.agendaKey.toUpperCase() }}:</strong> {{ agenda.value }}
                    <FieldRemarkControl :proposal-id="proposal.id" :section-key="'05'" :field-key="`priorityAgenda.${agenda.agendaKey}.value`" :field-label="`Priority Agenda — ${agenda.agendaKey}`" :remarks="remarksForField(`priorityAgenda.${agenda.agendaKey}.value`)" readonly />
                  </template>
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
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'06'" :field-key="'innovationGoals'" field-label="Innovation Goals" :remarks="remarksForField('innovationGoals')" readonly />
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">07</span>
              <h2>Sector Relevance</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sectorRelevance || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'07'" :field-key="'sectorRelevance'" field-label="Sector Relevance" :remarks="remarksForField('sectorRelevance')" readonly />
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">08</span>
              <h2>Sustainable Development Goals</h2>
            </div>
            <div class="card-body">
              <SdgChecklist :value="proposal.sdg" />
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'08'" :field-key="'sdg'" field-label="Selected SDGs" :remarks="remarksForField('sdg')" readonly />
            </div>
          </section>

          <section class="content-card">
            <div class="card-header">
              <span class="card-number">09</span>
              <h2>Executive Summary</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.executiveSummary || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'09'" :field-key="'executiveSummary'" field-label="Executive Summary" :remarks="remarksForField('executiveSummary')" readonly />
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
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'rationale'" field-label="Rationale / Significance" :remarks="remarksForField('rationale')" readonly />
              </div>
              <div class="subsection">
                <h3>10.2 Scientific Basis / Theoretical Framework</h3>
                <p class="text-content">{{ proposal.framework || 'Not provided' }}</p>
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'framework'" field-label="Scientific Basis / Theoretical Framework" :remarks="remarksForField('framework')" readonly />
              </div>
              <div class="subsection">
                <h3>10.3 Objectives</h3>
                <div class="objectives">
                  <div>
                    <strong>General Objective:</strong>
                    <p>{{ proposal.objectivesGeneral || 'Not provided' }}</p>
                    <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'objectivesGeneral'" field-label="General Objective" :remarks="remarksForField('objectivesGeneral')" readonly />
                    <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'objectivesGeneral'" field-label="General Objective" :remarks="remarksForField('objectivesGeneral')" readonly />
                  </div>
                  <div>
                    <strong>Specific Objectives:</strong>
                    <p>{{ proposal.objectivesSpecific || 'Not provided' }}</p>
                    <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'objectivesSpecific'" field-label="Specific Objectives" :remarks="remarksForField('objectivesSpecific')" readonly />
                    <FieldRemarkControl :proposal-id="proposal.id" :section-key="'10'" :field-key="'objectivesSpecific'" field-label="Specific Objectives" :remarks="remarksForField('objectivesSpecific')" readonly />
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
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'11'" :field-key="'reviewFileName'" field-label="Review of Literature (document)" :remarks="remarksForField('reviewFileName')" readonly />
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
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'12'" :field-key="'methodology'" field-label="Methodology" :remarks="remarksForField('methodology')" readonly />
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
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'13'" :field-key="'roadmapFileName'" field-label="Technology Roadmap (document)" :remarks="remarksForField('roadmapFileName')" readonly />
                  <p class="file-type">PDF Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.roadmapFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No file uploaded.</p>
            </div>
          </section>

          <!-- 14. Expected Outputs -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">14</span>
              <h2>Expected Outputs (6Ps)</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.outputs || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'14'" :field-key="'outputs'" field-label="Expected Outputs (6Ps)" :remarks="remarksForField('outputs')" readonly />
            </div>
          </section>

          <!-- 15. Potential Outcomes -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">15</span>
              <h2>Potential Outcomes</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.outcomes || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'15'" :field-key="'outcomes'" field-label="Potential Outcomes" :remarks="remarksForField('outcomes')" readonly />
            </div>
          </section>

          <!-- 16. Potential Impacts -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">16</span>
              <h2>Potential Impacts (2Is)</h2>
            </div>
            <div class="card-body">
              <div class="subsection">
                <h3>a. Economic Impact</h3>
                <p class="text-content">{{ proposal.impactEconomic || 'Not provided' }}</p>
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'16'" :field-key="'impactEconomic'" field-label="Economic Impact" :remarks="remarksForField('impactEconomic')" readonly />
              </div>
              <div class="subsection">
                <h3>b. Social / Ethical Impact</h3>
                <p class="text-content">{{ proposal.impactSocial || 'Not provided' }}</p>
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'16'" :field-key="'impactSocial'" field-label="Social / Ethical Impact" :remarks="remarksForField('impactSocial')" readonly />
              </div>
            </div>
          </section>

          <!-- 17. Target Beneficiaries -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">17</span>
              <h2>Target Beneficiaries</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.beneficiaries || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'17'" :field-key="'beneficiaries'" field-label="Target Beneficiaries" :remarks="remarksForField('beneficiaries')" readonly />
            </div>
          </section>

          <!-- 18. Sustainability Plan -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">18</span>
              <h2>Sustainability Plan</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.sustainability || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'18'" :field-key="'sustainability'" field-label="Sustainability Plan" :remarks="remarksForField('sustainability')" readonly />
            </div>
          </section>

          <!-- 19. GAD Score -->
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
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'19'" :field-key="'gadFileName'" field-label="GAD Score (document)" :remarks="remarksForField('gadFileName')" readonly />
                  <p class="file-type">PDF Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.gadFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No file uploaded.</p>
            </div>
          </section>

          <!-- 20. Limitations -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">20</span>
              <h2>Limitations of the Project</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.limitationsText || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'20'" :field-key="'limitationsText'" field-label="Limitations of the Project" :remarks="remarksForField('limitationsText')" readonly />
              <table v-if="proposal.limitations && proposal.limitations.length" class="data-table" style="margin-top:8px;">
                <thead>
                  <tr><th>#</th><th>Limitation</th><th>Remarks</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(row, index) in proposal.limitations" :key="row.id ?? index">
                    <td>{{ index + 1 }}</td>
                    <td>
                      {{ row.limitation || '-' }}
                      <FieldRemarkControl :proposal-id="proposal.id" :section-key="'20'" :field-key="`limitations[${index}].limitation`" :field-label="`Limitation #${index + 1}`" :remarks="remarksForField(`limitations[${index}].limitation`)" readonly />
                    </td>
                    <td>
                      {{ row.remarks || '-' }}
                      <FieldRemarkControl :proposal-id="proposal.id" :section-key="'20'" :field-key="`limitations[${index}].remarks`" :field-label="`Limitation #${index + 1} — Remarks`" :remarks="remarksForField(`limitations[${index}].remarks`)" readonly />
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </section>

          <!-- 21. Risks & Assumptions -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">21</span>
              <h2>Risk Management Plan</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.risks || 'Not provided' }}</p>
              <FieldRemarkControl :proposal-id="proposal.id" :section-key="'21'" :field-key="'risks'" field-label="Risk Management Plan" :remarks="remarksForField('risks')" readonly />
            </div>
          </section>

          <!-- 22. Logical Framework -->
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
                    <tr v-for="(row, index) in proposal.logFrames" :key="index">
                      <td>{{ row.outcome || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'22'" :field-key="`logFrames[${index}].outcome`" field-label="Outcome Indicator" :remarks="remarksForField(`logFrames[${index}].outcome`)" readonly /></td>
                      <td>{{ row.output || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'22'" :field-key="`logFrames[${index}].output`" field-label="Output Indicator" :remarks="remarksForField(`logFrames[${index}].output`)" readonly /></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No logical framework data.</p>
            </div>
          </section>

          <!-- 23. Literature Cited -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">23</span>
              <h2>Literature Cited</h2>
            </div>
            <div class="card-body">
              <p class="text-content">{{ proposal.referencesText || 'Not provided' }}</p>
            </div>
          </section>

          <!-- 24. Personnel Requirement -->
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
                      <td>{{ row.position || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'24'" :field-key="`personnel[${index}].position`" field-label="Position" :remarks="remarksForField(`personnel[${index}].position`)" readonly /></td>
                      <td>{{ row.time || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'24'" :field-key="`personnel[${index}].timeDevoted`" field-label="% Time Devoted" :remarks="remarksForField(`personnel[${index}].timeDevoted`)" readonly /></td>
                      <td>{{ row.responsibilities || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'24'" :field-key="`personnel[${index}].responsibilities`" field-label="Responsibilities" :remarks="remarksForField(`personnel[${index}].responsibilities`)" readonly /></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No personnel data.</p>
            </div>
          </section>

          <!-- 25. Line-Item Budget -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">25</span>
              <h2>Line-Item Budget Requirement</h2>
            </div>
            <div class="card-body">
              <div v-if="proposal.budgetFileName" class="file-attachment">
                <div class="file-icon">📄</div>
                <div class="file-info">
                  <p class="file-name">{{ proposal.beneficiariesFileName }}</p>
                  <FieldRemarkControl :proposal-id="proposal.id" :section-key="'25'" :field-key="'beneficiariesFileName'" field-label="Line-Item Budget (document)" :remarks="remarksForField('beneficiariesFileName')" readonly />
                  <p class="file-type">Budget Document</p>
                </div>
                <button class="btn-download" @click="downloadFile(proposal.budgetFileName)">Download</button>
              </div>
              <p v-else class="empty-text">No budget file uploaded.</p>
            </div>
          </section>

          <!-- 26. Other Ongoing Projects -->
          <section class="content-card">
            <div class="card-header">
              <span class="card-number">26</span>
              <h2>Other Ongoing Projects</h2>
            </div>
            <div class="card-body">
              <p v-if="proposal.otherProjectsNumber" class="text-content" style="margin-bottom: 12px;">
                <strong>Number of other projects:</strong> {{ proposal.otherProjectsNumber }}
                <FieldRemarkControl :proposal-id="proposal.id" :section-key="'26'" :field-key="'otherProjectsNumber'" field-label="Number of other projects" :remarks="remarksForField('otherProjectsNumber')" readonly />
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
                      <td>{{ row.title || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'26'" :field-key="`otherProjects[${index}].title`" field-label="Title" :remarks="remarksForField(`otherProjects[${index}].title`)" readonly /></td>
                      <td>{{ row.agency || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'26'" :field-key="`otherProjects[${index}].agency`" field-label="Funding Agency" :remarks="remarksForField(`otherProjects[${index}].agency`)" readonly /></td>
                      <td>{{ row.involvement || '-' }}<FieldRemarkControl :proposal-id="proposal.id" :section-key="'26'" :field-key="`otherProjects[${index}].involvement`" field-label="Involvement" :remarks="remarksForField(`otherProjects[${index}].involvement`)" readonly /></td>
                    </tr>
                  </tbody>
                </table>
              </div>
              <p v-else class="empty-text">No other projects.</p>
            </div>
          </section>

          <!-- 27. Other Supporting Documents -->
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

          <!-- Review Information Card (shows who reviewed the proposal) -->
          <div v-if="proposal.reviewedBy" class="sidebar-card review-info-card">
            <h3>Review Information</h3>
            <div class="review-info-list">
              <div class="review-info-item">
                <span class="review-info-label">Reviewed By:</span>
                <span class="review-info-value">{{ proposal.reviewedBy }}</span>
              </div>
              <div class="review-info-item">
                <span class="review-info-label">Position:</span>
                <span class="review-info-value reviewer-position-badge">
                  {{ formatReviewerPosition(proposal.reviewedByPosition) }}
                </span>
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

      <SubmissionHistory v-if="proposal.id" :proposal-id="proposal.id" />
    </template>

    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import { buildStandaloneHtml, printDocument, buildProposalDocument } from '@/utils/documentExport'
import { downloadUploadedFile } from '@/utils/fileDownload'
import SubmissionHistory from '@/components/SubmissionHistory.vue'
import SdgChecklist from '@/components/SdgChecklist.vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import FieldRemarkControl from '@/components/FieldRemarkControl.vue'
import { useDialog } from '@/composables/useDialog'
import { isDuplicable } from '@/utils/proposalStatus'
import { buildFieldRemarkIndex } from '@/utils/proposalFields'

const route = useRoute()
const router = useRouter()
const proposal = ref({})
const loading = ref(true)
const error = ref(null)

// Reviewer remarks of the active revision round, shown beside the matching field.
const remarksResponse = ref(null)
const remarksByKey = computed(() => buildFieldRemarkIndex(remarksResponse.value))
const remarksForField = (key) => remarksByKey.value[key] || []

const fetchFieldRemarks = async (proposalId) => {
  try {
    const res = await api.get(`/api/proposals/${proposalId}/field-remarks`)
    remarksResponse.value = res.data || null
  } catch (err) {
    // Remarks are informational here: never block the proposal view.
    console.error(err)
    remarksResponse.value = null
  }
}

const { dialogState, showAlert, showConfirm } = useDialog()
const duplicating = ref(false)

// Creates a new DRAFT from this completed proposal. The backend allocates a new
// proposal code / document id and copies only the reusable content; the copy is
// then opened in the proposal form for editing before it is submitted.
const duplicateProposal = async () => {
  const title = proposal.value.projectTitle || 'this proposal'
  const confirmed = await showConfirm(
    `Create a new draft proposal from "${title}"? You can edit the copy before submitting it.`,
    { title: 'Duplicate Proposal', type: 'info', confirmText: 'Duplicate' }
  )
  if (!confirmed) return

  duplicating.value = true
  try {
    const res = await api.post(`/api/proposals/${proposal.value.id}/duplicate`)
    const created = res.data || {}
    await showAlert(
      `A new draft was created from this proposal (${created.documentId || created.proposalCode || 'new draft'}). Edit it and submit it when ready.`,
      { type: 'success', title: 'Proposal Duplicated' }
    )
    if (created.id) {
      router.push({ path: '/proposals', query: { edit: created.id } })
    }
  } catch (err) {
    console.error(err)
    const body = err.response?.data
    const message =
      (body && typeof body === 'object' ? body.error : body) ||
      'Failed to duplicate the proposal. Please try again.'
    await showAlert(String(message), { type: 'error', title: 'Duplicate Failed' })
  } finally {
    duplicating.value = false
  }
}

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
    await fetchFieldRemarks(proposalId)
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
  downloadUploadedFile(fileName)
}

const printProposal = () => {
  const p = proposal.value
  const html = buildStandaloneHtml({
    title: 'Research Proposal Form',
    subtitle: p.projectTitle || 'Project Proposal',
    body: buildProposalDocument(p)
  })
  printDocument({ title: 'Research Proposal Form', html })
}

const editDraft = () => {
  router.push({ path: '/proposals', query: { edit: proposal.value.id } })
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

.document-id {
  font-size: 13px;
  color: #4b3f72;
  font-family: monospace;
  font-weight: 600;
}

.revision-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: #e0e7ff;
  color: #3730a3;
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

.status-badge.under_review {
  background: #ede9fe;
  color: #6d28d9;
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

.btn-duplicate {
  background: #4b3f72;
  border: 1px solid #4b3f72;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-duplicate:hover:not(:disabled) {
  background: #3c3160;
}

.btn-duplicate:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-edit-draft {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #facc15;
  border: none;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-edit-draft:hover {
  background: #eab308;
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
  font-weight: 700;
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
  /* Keep each field at its natural height so a remark on one field can never
     stretch its row partner and misalign the neighbouring column. */
  align-items: start;
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
  font-weight: 700;
  color: #1e293b;
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

.status-indicator.draft {
  background: #f1f5f9;
  color: #64748b;
}

.status-indicator.submitted {
  background: #f3e8ff;
  color: #6b21a8;
}

.status-indicator.under_review {
  background: #ede9fe;
  color: #6d28d9;
}

.status-indicator.approved {
  background: #dcfce7;
  color: #166534;
}

.status-indicator.rejected {
  background: #fee2e2;
  color: #991b1b;
}

.status-indicator.revision {
  background: #e0e7ff;
  color: #3730a3;
}

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

/* REVIEW INFO CARD */
.review-info-card {
  background: #f0fdf4;
  border-color: #86efac;
}

.review-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.review-info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.review-info-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.review-info-value {
  font-size: 14px;
  color: #1e293b;
  font-weight: 600;
}

.reviewer-position-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  background: #dcfce7;
  color: #166534;
  width: fit-content;
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