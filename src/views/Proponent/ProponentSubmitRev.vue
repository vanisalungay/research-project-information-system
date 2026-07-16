<template>
  <div class="revision-page">
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
          <h1>Submit Revision</h1>
          <p class="subtitle">{{ documentId || 'Proposal #' + proposalId }}</p>
        </div>
      </div>
    </header>

    <!-- ERROR MESSAGE -->
    <div v-if="error" class="error-banner">
      <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <span>{{ error }}</span>
    </div>

    <!-- SUCCESS MESSAGE -->
    <div v-if="successMessage" class="success-banner">
      <svg class="success-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
        <polyline points="22 4 12 14.01 9 11.01"></polyline>
      </svg>
      <span>{{ successMessage }}</span>
    </div>

    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- REVISION FORM -->
    <template v-else-if="proposalId">
      <div class="content-grid">
        <!-- LEFT COLUMN -->
        <div class="main-content">
          <div class="modal-body">
            <!-- 1. Project Profile -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">01</span>
                <h3>Project Profile</h3>
              </div>
              <div class="section-card">
                <div class="form-row">
                  <div class="form-group">
                    <label>Program Title</label>
                    <input v-model="proposal.program_title" placeholder="e.g. Community Development Program" />
                  </div>
                  <div class="form-group">
                    <label>Project Title</label>
                    <input v-model="proposal.project_title"
                      placeholder="e.g. Community-Based Water Filtration System" />
                  </div>
                </div>
                <div class="form-row">
                  <div class="form-group">
                    <label>Project Leader</label>
                    <input v-model="proposal.project_leader" placeholder="Full name of project leader" />
                  </div>
                  <div class="form-group">
                    <label>Sex</label>
                    <select v-model="proposal.project_leader_sex">
                      <option value="">Select Sex</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                    </select>
                  </div>
                </div>
                <div class="form-row three-col">
                  <div class="form-group">
                    <label>Duration (months)</label>
                    <input v-model="proposal.duration" placeholder="e.g. 12" />
                  </div>
                  <div class="form-group">
                    <label>Start Date</label>
                    <input type="date" v-model="proposal.start_date" />
                  </div>
                  <div class="form-group">
                    <label>End Date</label>
                    <input type="date" v-model="proposal.end_date" />
                  </div>
                </div>
                <div class="form-group">
                  <label>Implementing College / Department</label>
                  <input v-model="proposal.department" placeholder="e.g. College of Engineering" />
                </div>
                <div class="form-group">
                  <label>Address / Contact</label>
                  <textarea v-model="proposal.address"
                    placeholder="Barangay, Municipality, District, Province, Region / Telephone / Fax / Email"
                    rows="3"></textarea>
                </div>
              </div>
            </section>

            <!-- 2. Cooperating Agency/ies -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">02</span>
                <h3>Cooperating Agencies</h3>
              </div>
              <div class="section-card">
                <div class="form-group">
                  <label>Name/s and Address/es</label>
                  <textarea v-model="proposal.cooperating_agencies"
                    placeholder="List cooperating agencies and their addresses..." rows="3"></textarea>
                </div>
              </div>
            </section>

            <!-- 3. Sites of Implementation -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">03</span>
                <h3>Sites of Implementation</h3>
              </div>
              <div class="section-card">
                <div class="table-wrapper">
                  <table class="form-table">
                    <thead>
                      <tr>
                        <th>#</th>
                        <th>Country</th>
                        <th>Region</th>
                        <th>Province</th>
                        <th>District</th>
                        <th>Municipality</th>
                        <th>Barangay</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(site, index) in proposal.sites" :key="index">
                        <td class="row-num">{{ index + 1 }}</td>
                        <td><input v-model="site.country" placeholder="" /></td>
                        <td><input v-model="site.region" placeholder="" /></td>
                        <td><input v-model="site.province" placeholder="" /></td>
                        <td><input v-model="site.district" placeholder="" /></td>
                        <td><input v-model="site.municipality" placeholder="" /></td>
                        <td><input v-model="site.barangay" placeholder="" /></td>
                        <td><button class="btn-icon btn-remove" @click="removeSite(index)" title="Remove">✕</button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <button class="btn-add" @click="addSite">+ Add Site</button>
              </div>
            </section>

            <!-- 4. Type of Research -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">04</span>
                <h3>Type of Research</h3>
              </div>
              <div class="section-card">
                <div class="radio-group">
                  <label class="radio-card" :class="{ active: proposal.research_type === 'Basic' }">
                    <input type="radio" value="Basic" v-model="proposal.research_type" />
                    <div class="radio-content">
                      <span class="radio-title">Basic Research</span>
                      <span class="radio-desc">Fundamental research to expand knowledge</span>
                    </div>
                  </label>
                  <label class="radio-card" :class="{ active: proposal.research_type === 'Applied' }">
                    <input type="radio" value="Applied" v-model="proposal.research_type" />
                    <div class="radio-content">
                      <span class="radio-title">Applied Research</span>
                      <span class="radio-desc">Practical application of existing knowledge</span>
                    </div>
                  </label>
                </div>
              </div>
            </section>

            <!-- 5. Priority Agenda -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">05</span>
                <h3>Priority Agenda</h3>
                <span class="section-sub">(based on MSUN RIIDE 2025–2028)</span>
              </div>
              <div class="section-card">
                <div class="agenda-grid">
                  <div v-for="program in programList" :key="program.key" class="agenda-item"
                    :class="{ selected: proposal.priority_agendas[program.key].selected }">
                    <label class="agenda-checkbox">
                      <input type="checkbox" v-model="proposal.priority_agendas[program.key].selected" />
                      <span class="checkmark"></span>
                      <span class="agenda-label">{{ program.label }}</span>
                    </label>
                    <div class="agenda-value" v-if="proposal.priority_agendas[program.key].selected">
                      <span class="agenda-sub">{{ program.subLabel }}:</span>
                      <input v-model="proposal.priority_agendas[program.key].value"
                        :placeholder="`Enter ${program.subLabel.toLowerCase()}...`" />
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <!-- 6–9: Text Sections -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">06</span>
                <h3>Innovation Goals</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.innovation_goals"
                  placeholder="Describe the innovation goals of this project..." rows="4"></textarea>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">07</span>
                <h3>Sector Relevance</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.sector_relevance"
                  placeholder="Public Health, Environment, Agriculture, etc." rows="4"></textarea>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">08</span>
                <h3>Sustainable Development Goals (SDG)</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.sustainable_development_goals"
                  placeholder="List applicable SDGs addressed..." rows="4"></textarea>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">09</span>
                <h3>Executive Summary</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.executive_summary" placeholder="Not to exceed 200 words" rows="5"
                  class="word-limit"></textarea>
                <span class="char-count">{{ proposal.executive_summary?.length || 0 }} / ~1400 chars</span>
              </div>
            </section>

            <!-- 10. INTRODUCTION -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">10</span>
                <h3>Introduction</h3>
              </div>
              <div class="section-card">
                <div class="sub-section">
                  <h4>10.1 Rationale / Significance</h4>
                  <p class="field-note">Not to exceed 300 words</p>
                  <textarea v-model="proposal.rationale" rows="4"></textarea>
                </div>
                <div class="sub-section">
                  <h4>10.2 Scientific Basis / Theoretical Framework</h4>
                  <textarea v-model="proposal.theoretical_framework" rows="4"></textarea>
                </div>
                <div class="sub-section">
                  <h4>10.3 Objectives</h4>
                  <div class="form-group">
                    <label>General Objective:</label>
                    <textarea v-model="proposal.general_objective" rows="3"></textarea>
                  </div>
                  <div class="form-group">
                    <label>Specific Objectives:</label>
                    <textarea v-model="proposal.specific_objectives" rows="3"></textarea>
                  </div>
                </div>
              </div>
            </section>

            <!-- 11. Review of Literature -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">11</span>
                <h3>Review of Literature</h3>
              </div>
              <div class="section-card">
                <div class="file-upload">
                  <div class="upload-area" @click="$refs.litFile.click()">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                      <line x1="12" y1="18" x2="12" y2="12" />
                      <line x1="9" y1="15" x2="15" y2="15" />
                    </svg>
                    <span>Click to upload literature review file</span>
                    <span class="upload-hint">PDF, DOC, DOCX accepted</span>
                  </div>
                  <input ref="litFile" type="file" @change="handleFileUpload('review_of_literature', $event)" hidden />
                  <div v-if="proposal.review_of_literature_file" class="file-info">
                    <span>📄 {{ proposal.review_of_literature_file.name }}</span>
                    <button class="btn-remove-file" @click="proposal.review_of_literature_file = null">✕</button>
                  </div>
                </div>
              </div>
            </section>

            <!-- 12. Methodology -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">12</span>
                <h3>Methodology</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.methodology" placeholder="Describe the research methodology..."
                  rows="5"></textarea>
              </div>
            </section>

            <!-- 13. Technology Roadmap -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">13</span>
                <h3>Technology Roadmap</h3>
                <span class="section-sub">(if applicable)</span>
              </div>
              <div class="section-card">
                <div class="file-upload">
                  <div class="upload-area" @click="$refs.techFile.click()">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                      <line x1="12" y1="18" x2="12" y2="12" />
                      <line x1="9" y1="15" x2="15" y2="15" />
                    </svg>
                    <span>Upload technology roadmap</span>
                  </div>
                  <input ref="techFile" type="file" @change="handleFileUpload('technology', $event)" hidden />
                  <div v-if="proposal.technology_roadmap_file" class="file-info">
                    <span>📄 {{ proposal.technology_roadmap_file.name }}</span>
                    <button class="btn-remove-file" @click="proposal.technology_roadmap_file = null">✕</button>
                  </div>
                </div>
              </div>
            </section>

            <!-- 14–16: Outputs, Outcomes, Impacts -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">14</span>
                <h3>Expected Outputs (6Ps)</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.expected_outputs" placeholder="List expected outputs..."
                  rows="4"></textarea>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">15</span>
                <h3>Potential Outcomes</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.potential_outcomes" placeholder="Describe potential outcomes..."
                  rows="4"></textarea>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">16</span>
                <h3>Potential Impacts (2Is)</h3>
              </div>
              <div class="section-card">
                <div class="form-group">
                  <label>a. Economic Impact:</label>
                  <textarea v-model="proposal.economic_impact" rows="3"></textarea>
                </div>
                <div class="form-group">
                  <label>b. Social / Ethical Impact:</label>
                  <textarea v-model="proposal.social_ethical_impact" rows="3"></textarea>
                </div>
              </div>
            </section>

            <!-- 17. Target Beneficiaries -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">17</span>
                <h3>Target Beneficiaries</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.target_beneficiaries" placeholder="Describe the target beneficiaries..."
                  rows="4"></textarea>
              </div>
            </section>

            <!-- 18. Sustainability Plan -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">18</span>
                <h3>Sustainability Plan</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.sustainability_plan" placeholder="Describe the sustainability plan..."
                  rows="4"></textarea>
              </div>
            </section>

            <!-- 19. GAD Score -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">19</span>
                <h3>Gender and Development (GAD) Score</h3>
              </div>
              <div class="section-card">
                <div class="file-upload">
                  <div class="upload-area" @click="$refs.gadFile.click()">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                      <polyline points="14 2 14 8 20 8" />
                      <line x1="12" y1="18" x2="12" y2="12" />
                      <line x1="9" y1="15" x2="15" y2="15" />
                    </svg>
                    <span>Upload GAD score document</span>
                  </div>
                  <input ref="gadFile" type="file" @change="handleFileUpload('gad', $event)" hidden />
                  <div v-if="proposal.gad_score_file" class="file-info">
                    <span>📄 {{ proposal.gad_score_file.name }}</span>
                    <button class="btn-remove-file" @click="proposal.gad_score_file = null">✕</button>
                  </div>
                </div>
              </div>
            </section>

            <!-- 20. Limitations -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">20</span>
                <h3>Limitations of the Project</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.limitations" placeholder="Describe project limitations..."
                  rows="4"></textarea>
              </div>
            </section>

            <!-- 21. Risks -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">21</span>
                <h3>Risk Management Plan</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.risks_assumptions"
                  placeholder="List risks, assumptions, and mitigation strategies..." rows="4"></textarea>
              </div>
            </section>

            <!-- 22. Logical Framework -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">22</span>
                <h3>Logical Framework</h3>
                <span class="section-sub">Outcome and Output Indicators</span>
              </div>
              <div class="section-card">
                <div class="table-wrapper">
                  <table class="form-table">
                    <thead>
                      <tr>
                        <th>Outcome Indicators</th>
                        <th>Output Indicators / Physical Target</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, index) in proposal.logical_framework" :key="index">
                        <td><input v-model="row.outcome_indicator" placeholder="e.g. Improved water quality" /></td>
                        <td><input v-model="row.output_indicator" placeholder="e.g. 5 filtration units installed" />
                        </td>
                        <td><button class="btn-icon btn-remove"
                            @click="removeTableRow('logical_framework', index)">✕</button></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <button class="btn-add" @click="addTableRow('logical_framework')">+ Add Row</button>
              </div>
            </section>

            <!-- 23. Literature Cited -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">23</span>
                <h3>Literature Cited</h3>
              </div>
              <div class="section-card">
                <textarea v-model="proposal.literature_cited" placeholder="List your references in APA format..."
                  rows="5"></textarea>
              </div>
            </section>

            <!-- 24. Personnel -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">24</span>
                <h3>Personnel Requirement</h3>
              </div>
              <div class="section-card">
                <div class="table-wrapper">
                  <table class="form-table">
                    <thead>
                      <tr>
                        <th>Position</th>
                        <th>% Time Devoted</th>
                        <th>Responsibilities</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, index) in proposal.personnel_requirements" :key="index">
                        <td><input v-model="row.position" placeholder="e.g. Research Assistant" /></td>
                        <td><input v-model="row.effort" placeholder="e.g. 50%" /></td>
                        <td><input v-model="row.responsibilities" placeholder="e.g. Data collection" /></td>
                        <td><button class="btn-icon btn-remove"
                            @click="removeTableRow('personnel_requirements', index)">✕</button></td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <button class="btn-add" @click="addTableRow('personnel_requirements')">+ Add Row</button>
              </div>
            </section>

            <!-- 25. Budget -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">25</span>
                <h3>Line-Item Budget Requirement</h3>
              </div>
              <div class="section-card">
                <div class="budget-actions">
                  <button class="btn-outline" @click="downloadBudgetFormat">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                      <polyline points="7 10 12 15 17 10" />
                      <line x1="12" y1="15" x2="12" y2="3" />
                    </svg>
                    Download Budget Template
                  </button>
                  <div class="file-upload inline">
                    <div class="upload-area small" @click="$refs.budgetFile.click()">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                        stroke-width="2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                        <polyline points="14 2 14 8 20 8" />
                        <line x1="12" y1="18" x2="12" y2="12" />
                        <line x1="9" y1="15" x2="15" y2="15" />
                      </svg>
                      <span>Upload filled budget file</span>
                    </div>
                    <input ref="budgetFile" type="file" accept=".pdf,.doc,.docx"
                      @change="handleFileUpload('budget', $event)" hidden />
                  </div>
                </div>
                <div v-if="proposal.line_item_budget_file" class="file-info">
                  <span>📄 {{ proposal.line_item_budget_file.name }}</span>
                  <button class="btn-remove-file" @click="proposal.line_item_budget_file = null">✕</button>
                </div>
              </div>
            </section>

            <!-- 26. Other Projects -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">26</span>
                <h3>Other Ongoing Projects</h3>
                <span class="section-sub">Being handled by the Project Leader</span>
              </div>
              <div class="section-card">
                <div class="form-group">
                  <label>Number of other projects:</label>
                  <input v-model="proposal.other_projects_number" placeholder="e.g. 2" class="input-short" />
                </div>
                <div class="table-wrapper">
                  <table class="form-table">
                    <thead>
                      <tr>
                        <th>Title</th>
                        <th>Funding Agency</th>
                        <th>Involvement</th>
                        <th></th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, index) in proposal.other_projects" :key="index">
                        <td><input v-model="row.project_title" placeholder="Project title" /></td>
                        <td><input v-model="row.funding_agency" placeholder="e.g. DOST" /></td>
                        <td><input v-model="row.involvement" placeholder="e.g. Project Leader" /></td>
                        <td><button class="btn-icon btn-remove"
                            @click="removeTableRow('other_projects', index)">✕</button>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
                <button class="btn-add" @click="addTableRow('other_projects')">+ Add Project</button>
              </div>
            </section>

            <!-- 27. Other Supporting Documents -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">27</span>
                <h3>Other Supporting Documents</h3>
              </div>
              <div class="section-card">
                <p class="field-note">Please refer to the next page for additional necessary documents.</p>
              </div>
            </section>
          </div>
        </div>

        <!-- SIDEBAR -->
        <aside class="sidebar">
          <div class="sidebar-card">
            <h3>Revision Information</h3>

            <div class="info-list">
              <div class="info-item">
                <span class="info-label">Document ID</span>
                <span class="info-value document-id">
                  {{ documentId || 'Pending' }}
                </span>
              </div>
              <div class="info-item">
                <span class="info-label">Revision Number</span>
                <span class="info-value">
                  REV{{ String(revisionNumber).padStart(2, '0') }}
                </span>
              </div>

              <div class="info-item">
                <span class="info-label">Status</span>
                <span class="info-value status-badge revision">
                  REVISION
                </span>
              </div>
            </div>
          </div>

          <div class="sidebar-card">
            <h3>Instructions</h3>

            <ul class="instructions-list">
              <li>Review the feedback from reviewers.</li>
              <li>Update all required sections.</li>
              <li>Ensure all corrections are addressed.</li>
              <li>Submit the revised proposal for evaluation.</li>
            </ul>
          </div>

          <div class="sidebar-card action-card">
            <h3>Actions</h3>

            <button class="btn-action btn-cancel" @click="goBack">
              ← Cancel
            </button>

            <button class="btn-action btn-draft" @click="saveDraft">
              💾 Save Draft
            </button>

            <button class="btn-action btn-submit" @click="submitRevision" :disabled="loading">
              {{ loading ? 'Submitting...' : '✓ Submit Revision' }}
            </button>
          </div>
        </aside>
      </div>
    </template>

    <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel" @close="dialogState.show = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'
import { useUserDataStore } from '@/stores/userData'

const route = useRoute()
const router = useRouter()
const userStore = useUserDataStore()

const { dialogState, showAlert } = useDialog()

const proposalId = computed(() => route.params.id as string | undefined)

const loading = ref(false)
const error = ref('')
const successMessage = ref('')
const documentId = ref('')
const revisionNumber = ref(0)



const proposal = ref({
  program_title: '',
  project_title: '',
  project_leader: '',
  project_leader_sex: '',
  duration: '',
  start_date: '',
  end_date: '',
  department: '',
  address: '',
  cooperating_agencies: '',
  research_type: '',
  innovation_goals: '',
  sector_relevance: '',
  sustainable_development_goals: '',
  executive_summary: '',
  rationale: '',
  theoretical_framework: '',
  general_objective: '',
  specific_objectives: '',
  methodology: '',
  expected_outputs: '',
  potential_outcomes: '',
  economic_impact: '',
  social_ethical_impact: '',
  target_beneficiaries: '',
  sustainability_plan: '',
  limitations: '',
  risks_assumptions: '',
  literature_cited: '',
  other_projects_number: '',

  review_of_literature_file: null,
  technology_roadmap_file: null,
  gad_score_file: null,
  line_item_budget_file: null,

  sites: [
    {
      country: '',
      region: '',
      province: '',
      district: '',
      municipality: '',
      barangay: ''
    }
  ],

  logical_framework: [],
  personnel_requirements: [],
  other_projects: [],

  priority_agendas: {
    agriculture: { selected: false, value: '' },
    environment: { selected: false, value: '' },
    health: { selected: false, value: '' }
  }
})

const addSite = () => {
  proposal.value.sites.push({
    country: '',
    region: '',
    province: '',
    municipality: '',
    barangay: ''
  })
}

const programList = [
  {
    key: 'agriculture',
    label: 'Agriculture',
    subLabel: 'Program'
  },
  {
    key: 'environment',
    label: 'Environment',
    subLabel: 'Program'
  },
  {
    key: 'health',
    label: 'Health',
    subLabel: 'Program'
  }
]

const removeSite = (index: number) => {
  proposal.value.sites.splice(index, 1)
}

const fetchProposal = async () => {
  if (!proposalId.value) return

  loading.value = true
  error.value = ''

  try {
    const res = await api.get(`/api/proposals/${proposalId.value}`)
    const data = res.data

    // Set document ID and revision number from API response
    documentId.value = data.documentId || ''
    revisionNumber.value = data.revisionNumber || 0

    // Map API response (camelCase) to form model (snake_case)
    proposal.value = {
      program_title: data.programTitle || '',
      project_title: data.projectTitle || '',
      project_leader: data.projectLeader || '',
      project_leader_sex: data.projectLeaderSex || '',
      duration: data.duration || '',
      start_date: data.startDate || '',
      end_date: data.endDate || '',
      department: data.college || '',
      address: data.address || '',
      cooperating_agencies: data.cooperatingAgencies || '',
      research_type: data.researchType || '',
      innovation_goals: data.innovationGoals || '',
      sector_relevance: data.sectorRelevance || '',
      sustainable_development_goals: data.sdg || '',
      executive_summary: data.executiveSummary || '',
      rationale: data.rationale || '',
      theoretical_framework: data.framework || '',
      general_objective: data.objectivesGeneral || '',
      specific_objectives: data.objectivesSpecific || '',
      methodology: data.methodology || '',
      expected_outputs: data.outputs || '',
      potential_outcomes: data.outcomes || '',
      economic_impact: data.impactEconomic || '',
      social_ethical_impact: data.impactSocial || '',
      target_beneficiaries: data.beneficiaries || '',
      sustainability_plan: data.sustainability || '',
      limitations: data.limitationsText || '',
      risks_assumptions: data.risks || '',
      literature_cited: data.referencesText || '',
      other_projects_number: data.otherProjectsNumber || '',

      review_of_literature_file: data.reviewFileName ? { name: data.reviewFileName } : null,
      technology_roadmap_file: data.roadmapFileName ? { name: data.roadmapFileName } : null,
      gad_score_file: data.gadFileName ? { name: data.gadFileName } : null,
      line_item_budget_file: data.beneficiariesFileName ? { name: data.beneficiariesFileName } : null,

      sites:
        data.sites?.length > 0
          ? data.sites.map(s => ({
            country: s.country || '',
            region: s.region || '',
            province: s.province || '',
            district: s.district || '',
            municipality: s.municipality || '',
            barangay: s.barangay || ''
          }))
          : [
            {
              country: '',
              region: '',
              province: '',
              district: '',
              municipality: '',
              barangay: ''
            }
          ],

      logical_framework:
        data.logFrames?.length > 0
          ? data.logFrames.map(lf => ({
            outcome_indicator: lf.outcome || '',
            output_indicator: lf.output || ''
          }))
          : [],

      personnel_requirements:
        data.personnel?.length > 0
          ? data.personnel.map(p => ({
            position: p.position || '',
            effort: p.time || '',
            responsibilities: p.responsibilities || ''
          }))
          : [],

      other_projects:
        data.otherProjects?.length > 0
          ? data.otherProjects.map(op => ({
            project_title: op.title || '',
            funding_agency: op.agency || '',
            involvement: op.involvement || ''
          }))
          : [],

      priority_agendas: {
        agriculture: { selected: false, value: '' },
        environment: { selected: false, value: '' },
        health: { selected: false, value: '' },
        ...(data.priorityAgendas
          ? Object.fromEntries(
            data.priorityAgendas.map(pa => [
              pa.agenda,
              { selected: pa.selected || false, value: pa.value || '' }
            ])
          )
          : {})
      }
    }
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

const submitRevision = async () => {
  if (!proposalId.value) {
    error.value = 'No proposal ID provided.'
    return
  }

  const proponentId = userStore.user?.id
  if (!proponentId) {
    error.value = 'User not logged in.'
    return
  }

  loading.value = true
  error.value = ''
  successMessage.value = ''

  const data = proposal.value

  // Build payload matching ProposalRequest DTO (camelCase)
  const payload = {
    proponentId,
    programTitle: data.program_title || '',
    projectTitle: data.project_title || '',
    projectLeader: data.project_leader || '',
    duration: data.duration || '',
    startDate: data.start_date || '',
    endDate: data.end_date || '',
    college: data.department || '',
    address: data.address || '',
    cooperatingAgencies: data.cooperating_agencies || '',
    researchType: data.research_type || '',
    innovationGoals: data.innovation_goals || '',
    sectorRelevance: data.sector_relevance || '',
    sdg: data.sustainable_development_goals || '',
    executiveSummary: data.executive_summary || '',
    rationale: data.rationale || '',
    framework: data.theoretical_framework || '',
    objectivesGeneral: data.general_objective || '',
    objectivesSpecific: data.specific_objectives || '',
    methodology: data.methodology || '',
    outputs: data.expected_outputs || '',
    outcomes: data.potential_outcomes || '',
    impactEconomic: data.economic_impact || '',
    impactSocial: data.social_ethical_impact || '',
    beneficiaries: data.target_beneficiaries || '',
    sustainability: data.sustainability_plan || '',
    risks: data.risks_assumptions || '',
    otherProjectsNumber: data.other_projects_number || '',
    status: 'SUBMITTED',
    sites: (data.sites || []).map(s => ({
      country: s.country || '',
      region: s.region || '',
      province: s.province || '',
      district: s.district || '',
      municipality: s.municipality || '',
      barangay: s.barangay || ''
    })),
    priorityAgenda: Object.fromEntries(
      Object.entries(data.priority_agendas || {}).map(([key, val]) => [
        key,
        { selected: val.selected || false, value: val.value || '' }
      ])
    ),
    logFrame: (data.logical_framework || []).map(lf => ({
      outcome: lf.outcome_indicator || '',
      output: lf.output_indicator || ''
    })),
    personnel: (data.personnel_requirements || []).map(p => ({
      position: p.position || '',
      time: p.effort || '',
      responsibilities: p.responsibilities || ''
    })),
    otherProjects: (data.other_projects || []).map(op => ({
      title: op.project_title || '',
      agency: op.funding_agency || '',
      involvement: op.involvement || ''
    }))
  }

  try {
    await api.put(`/api/proposals/${proposalId.value}`, payload)

    successMessage.value = 'Revision submitted successfully!'

    setTimeout(() => {
      router.push('/revisions')
    }, 1500)
  } catch (err) {
    console.error(err)
    error.value = 'Failed to submit revision. Please try again.'
  } finally {
    loading.value = false
  }
}

const saveDraft = async () => {
  await showAlert('Draft saved successfully!', {
    type: 'success',
    title: 'Draft Saved'
  })
}

const goBack = () => {
  router.back()
}

onMounted(() => {
  fetchProposal()
})
</script>

<style scoped>
.revision-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
}

/* HEADER */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
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
}

.back-btn:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.header-info h1 {
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 4px 0;
}

.subtitle {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* ERROR & SUCCESS BANNERS */
.error-banner,
.success-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 20px;
  font-size: 14px;
}

.error-banner {
  background: #fef2f2;
  border: 1px solid #fecaca;
  color: #991b1b;
}

.success-banner {
  background: #dcfce7;
  border: 1px solid #86efac;
  color: #166534;
}

.error-icon,
.success-icon {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
}

/* LOADING STATE */
.loading-state {
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
  gap: 6px;
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

.info-item input,
.info-item select {
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  transition: all 0.15s;
}

.info-item input:focus,
.info-item select:focus,
.info-item textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

/* FORM GROUP */
.form-group {
  margin-bottom: 20px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #475569;
  margin-bottom: 6px;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  font-family: inherit;
  transition: all 0.15s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
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

/* INFO LIST */
.info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
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

.status-badge {
  display: inline-block;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
}

.status-badge.revision {
  background: #e0e7ff;
  color: #3730a3;
}

/* INSTRUCTIONS */
.instructions-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.instructions-list li {
  padding: 8px 0;
  padding-left: 20px;
  position: relative;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.instructions-list li:before {
  content: "•";
  position: absolute;
  left: 0;
  color: #4f46e5;
  font-weight: bold;
}

/* ACTION CARD */
.action-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.btn-action {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-action.btn-cancel {
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-action.btn-cancel:hover {
  background: #f1f5f9;
}

.btn-action.btn-draft {
  background: #fff;
  color: #475569;
  border: 1px solid #e2e8f0;
}

.btn-action.btn-draft:hover {
  background: #f1f5f9;
}

.btn-action.btn-submit {
  background: #4f46e5;
  color: #fff;
}

.btn-action.btn-submit:hover:not(:disabled) {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

.btn-action.btn-submit:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* NO PROPOSAL MESSAGE */
.no-proposal-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  gap: 16px;
}

.no-proposal-message p {
  font-size: 16px;
  color: #64748b;
}

.btn-primary {
  background: #4f46e5;
  color: #fff;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary:hover {
  background: #4338ca;
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
    gap: 12px;
  }
}

/* =========================
   ADDITIONAL FORM STYLES
========================= */

.modal-body {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-section {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.05);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 22px;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  flex-wrap: wrap;
}

.section-number {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #4f46e5;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.section-sub {
  font-size: 12px;
  color: #64748b;
}

.section-card {
  padding: 24px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}

.form-row.three-col {
  grid-template-columns: repeat(3, 1fr);
}

.form-group {
  width: 100%;
}

.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  box-sizing: border-box;
}

.input-short {
  max-width: 200px;
}

.sub-section {
  margin-bottom: 30px;
}

.sub-section:last-child {
  margin-bottom: 0;
}

.field-note {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 10px;
}

.char-count {
  display: block;
  margin-top: 8px;
  text-align: right;
  font-size: 12px;
  color: #64748b;
}

/* =========================
   TABLES
========================= */

.table-wrapper {
  overflow-x: auto;
}

.form-table {
  width: 100%;
  border-collapse: collapse;
  min-width: 700px;
}

.form-table th {
  background: #f8fafc;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  padding: 12px;
  border: 1px solid #e2e8f0;
  text-align: left;
}

.form-table td {
  border: 1px solid #e2e8f0;
  padding: 10px;
}

.form-table input,
.form-table select {
  width: 100%;
  border: none;
  outline: none;
  background: transparent;
}

.row-num {
  text-align: center;
  font-weight: 600;
  width: 50px;
}

/* =========================
   BUTTONS
========================= */

.btn-add {
  margin-top: 16px;
  background: #4f46e5;
  color: white;
  border: none;
  padding: 10px 18px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.btn-add:hover {
  background: #4338ca;
}

.btn-outline {
  background: white;
  border: 1px solid #cbd5e1;
  color: #334155;
  border-radius: 8px;
  padding: 10px 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
}

.btn-outline:hover {
  background: #f8fafc;
}

.btn-icon {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.btn-remove {
  background: #fee2e2;
  color: #dc2626;
}

.btn-remove:hover {
  background: #fecaca;
}

.btn-remove-file {
  border: none;
  background: transparent;
  color: #dc2626;
  cursor: pointer;
  font-size: 16px;
}

/* =========================
   FILE UPLOAD
========================= */

.file-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.file-upload.inline {
  flex-direction: row;
  align-items: center;
}

.upload-area {
  border: 2px dashed #cbd5e1;
  border-radius: 12px;
  padding: 30px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #475569;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.upload-area:hover {
  border-color: #4f46e5;
  background: #f8faff;
}

.upload-area.small {
  padding: 14px 18px;
}

.upload-hint {
  font-size: 12px;
  color: #64748b;
}

.file-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8fafc;
  border-radius: 8px;
  padding: 12px 16px;
}

/* =========================
   RADIO CARDS
========================= */

.radio-group {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.radio-card {
  flex: 1;
  min-width: 260px;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.2s;
}

.radio-card.active {
  border-color: #4f46e5;
  background: #eef2ff;
}

.radio-card input {
  display: none;
}

.radio-title {
  display: block;
  font-weight: 600;
  color: #0f172a;
}

.radio-desc {
  display: block;
  font-size: 13px;
  color: #64748b;
  margin-top: 4px;
}

.agenda-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 18px;
}

.agenda-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px;
  background: #fff;
  transition: all 0.2s;
}

.agenda-item.selected {
  border: 2px solid #4f46e5;
  background: #eef2ff;
}

.agenda-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agenda-label {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

.agenda-sub {
  display: block;
  margin-bottom: 8px;
  color: #64748b;
  font-size: 13px;
}

.agenda-value input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
}

.agenda-value {
  margin-top: 16px;
}

.agenda-value input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
}

/* =========================
   BUDGET
========================= */

.budget-actions {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
}

/* =========================
   RESPONSIVE
========================= */

@media (max-width: 768px) {

  .form-row,
  .form-row.three-col {
    grid-template-columns: 1fr;
  }

  .section-card {
    padding: 18px;
  }

  .radio-group {
    flex-direction: column;
  }

  .agenda-grid {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }


  .budget-actions {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>