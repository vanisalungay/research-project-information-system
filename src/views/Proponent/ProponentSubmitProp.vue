<template>
  <div v-if="modelValue" class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <div class="header-left">
          <h2>Submit New Proposal</h2>
          <span class="header-badge">Step 1 of 3</span>
        </div>
        <div class="header-actions">
          <button class="btn-download" @click="downloadForm">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
              <polyline points="7 10 12 15 17 10" />
              <line x1="12" y1="15" x2="12" y2="3" />
            </svg>
            Download Form
          </button>
          <button class="close-btn" @click="close">✕</button>
        </div>
      </header>

      <!-- PROGRESS BAR -->
      <div class="progress-bar">
        <div class="progress-step active" @click="$emit('goToStep', 1)">
          <span class="step-number">1</span>
          <span class="step-label">Proposal Details</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step" @click="$emit('goToStep', 2)">
          <span class="step-number">2</span>
          <span class="step-label">Certification</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step" @click="$emit('goToStep', 3)">
          <span class="step-number">3</span>
          <span class="step-label">Review & Submit</span>
        </div>
      </div>

      <!-- VALIDATION ERROR BANNER -->
      <div v-if="showValidation && errorList.length > 0" class="validation-banner" ref="errorBannerRef">
        <div class="banner-icon">⚠</div>
        <div class="banner-content">
          <strong>Please complete all required fields before proceeding.</strong>
          <ul>
            <li v-for="(err, i) in errorList" :key="i">
              <a href="#" @click.prevent="scrollToField(err.field)">{{ err.message }}</a>
            </li>
          </ul>
        </div>
      </div>

      <!-- BODY -->
      <div class="modal-body" ref="modalBodyRef">
        <!-- 1. Project Profile -->
        <section class="form-section" ref="section_project_profile">
          <div class="section-header">
            <span class="section-number">01</span>
            <h3>Project Profile</h3>
          </div>
          <div class="section-card">
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': showValidation && errors.program_title }">
                <label>Program Title</label>
                <input v-model="proposal.program_title" placeholder="e.g. Community Development Program" />
                <span class="field-error" v-if="showValidation && errors.program_title">{{ errors.program_title
                }}</span>
              </div>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.project_title }">
                <label>Project Title <span class="req">*</span></label>
                <input v-model="proposal.project_title" placeholder="e.g. Community-Based Water Filtration System" />
                <span class="field-error" v-if="showValidation && errors.project_title">{{ errors.project_title
                }}</span>
              </div>
            </div>
            <div class="form-row">
              <div class="form-group" :class="{ 'has-error': showValidation && errors.project_leader }">
                <label>Project Leader <span class="req">*</span></label>
                <input v-model="proposal.project_leader" placeholder="Full name of project leader" />
                <span class="field-error" v-if="showValidation && errors.project_leader">{{ errors.project_leader
                }}</span>
              </div>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.project_leader_sex }">
                <label>Sex <span class="req">*</span></label>
                <select v-model="proposal.project_leader_sex">
                  <option value="">Select Sex</option>
                  <option value="Male">Male</option>
                  <option value="Female">Female</option>
                  <option value="Other">Prefer not to say</option>
                </select>
                <span class="field-error" v-if="showValidation && errors.project_leader_sex">{{
                  errors.project_leader_sex }}</span>
              </div>
            </div>
            <div class="form-row three-col">
              <div class="form-group" :class="{ 'has-error': showValidation && errors.duration }">
                <label>Duration (months) <span class="req">*</span></label>
                <input v-model="proposal.duration" placeholder="e.g. 12" />
                <span class="field-error" v-if="showValidation && errors.duration">{{ errors.duration }}</span>
              </div>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.start_date }">
                <label>Start Date <span class="req">*</span></label>
                <input type="date" v-model="proposal.start_date" />
                <span class="field-error" v-if="showValidation && errors.start_date">{{ errors.start_date }}</span>
              </div>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.end_date }">
                <label>End Date <span class="req">*</span></label>
                <input type="date" v-model="proposal.end_date" />
                <span class="field-error" v-if="showValidation && errors.end_date">{{ errors.end_date }}</span>
              </div>
            </div>
            <div class="form-group" :class="{ 'has-error': showValidation && errors.department }">
              <label>Implementing College / Department <span class="req">*</span></label>

              <select v-model="proposal.department">
                <option value="">Select College / Department</option>
                <option value="College of Fisheries and Marine Sciences (CFMS)">
                  College of Fisheries and Marine Sciences (CFMS)
                </option>
                <option value="College of Environment and Life Sciences (CELS)">
                  College of Environment and Life Sciences (CELS)
                </option>
                <option value="College of Education and Social Sciences (CESS)">
                  College of Education and Social Sciences (CESS)
                </option>
                <option value="College of Business and Information Technology (CBIT)">
                  College of Business and Information Technology (CBIT)
                </option>
                <option value="Integrated Development School (IDS)">
                  Integrated Development School (IDS)
                </option>
              </select>

              <span class="field-error" v-if="showValidation && errors.department">
                {{ errors.department }}
              </span>
            </div>
            <div class="form-group" :class="{ 'has-error': showValidation && errors.address }">
              <label>Address / Contact <span class="req">*</span></label>
              <textarea v-model="proposal.address"
                placeholder="Barangay, Municipality, District, Province, Region / Telephone / Fax / Email"
                rows="3"></textarea>
              <span class="field-error" v-if="showValidation && errors.address">{{ errors.address }}</span>
            </div>
          </div>
        </section>

        <!-- 2. Cooperating Agency/ies -->
        <section class="form-section" ref="section_cooperating_agencies">
          <div class="section-header">
            <span class="section-number">02</span>
            <h3>Cooperating Agencies</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.cooperating_agencies }">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.cooperating_agencies }">
              <label>Name/s and Address/es <span class="req">*</span></label>
              <textarea v-model="proposal.cooperating_agencies"
                placeholder="List cooperating agencies and their addresses..." rows="3"></textarea>
              <span class="field-error" v-if="showValidation && errors.cooperating_agencies">{{
                errors.cooperating_agencies }}</span>
            </div>
          </div>
        </section>

        <!-- 3. Sites of Implementation -->
        <section class="form-section" ref="section_sites">
          <div class="section-header">
            <span class="section-number">03</span>
            <h3>Sites of Implementation</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.sites }">
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
                    <td><input v-model="site.country" placeholder="Philippines" /></td>
                    <td><input v-model="site.region" placeholder="Region X" /></td>
                    <td><input v-model="site.province" placeholder="Misamis Oriental" /></td>
                    <td><input v-model="site.district" placeholder="District 2" /></td>
                    <td><input v-model="site.municipality" placeholder="Naawan" /></td>
                    <td><input v-model="site.barangay" placeholder="San Miguel" /></td>
                    <td><button class="btn-icon btn-remove" @click="removeSite(index)" title="Remove">✕</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <span class="field-error" v-if="showValidation && errors.sites">{{ errors.sites }}</span>
            <button class="btn-add" @click="addSite">+ Add Site</button>
          </div>
        </section>

        <!-- 4. Type of Research -->
        <section class="form-section" ref="section_research_type">
          <div class="section-header">
            <span class="section-number">04</span>
            <h3>Type of Research</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.research_type }">
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
            <span class="field-error" v-if="showValidation && errors.research_type">{{ errors.research_type }}</span>
          </div>
        </section>

        <!-- 5. Priority Agenda -->
        <section class="form-section" ref="section_priority_agendas">
          <div class="section-header">
            <span class="section-number">05</span>
            <h3>Priority Agenda</h3>
            <span class="section-sub">(based on MSUN RIIDE 2025–2028)</span>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.priority_agendas }">
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
            <span class="field-error" v-if="showValidation && errors.priority_agendas">{{ errors.priority_agendas
            }}</span>
          </div>
        </section>

        <!-- 6–9: Text Sections -->
        <section class="form-section" ref="section_innovation_goals">
          <div class="section-header">
            <span class="section-number">06</span>
            <h3>Innovation Goals</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.innovation_goals }">
              <textarea v-model="proposal.innovation_goals"
                placeholder="Describe the innovation goals of this project..." rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.innovation_goals">{{ errors.innovation_goals
              }}</span>
            </div>
          </div>
        </section>

        <section class="form-section" ref="section_sector_relevance">
          <div class="section-header">
            <span class="section-number">07</span>
            <h3>Sector Relevance</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.sector_relevance }">
              <textarea v-model="proposal.sector_relevance" placeholder="Public Health, Environment, Agriculture, etc."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.sector_relevance">{{ errors.sector_relevance
              }}</span>
            </div>
          </div>
        </section>

        <section class="form-section" ref="section_sdg">
          <div class="section-header">
            <span class="section-number">08</span>
            <h3>Sustainable Development Goals (SDG)</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.sustainable_development_goals }">
              <textarea v-model="proposal.sustainable_development_goals" placeholder="List applicable SDGs addressed..."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.sustainable_development_goals">{{
                errors.sustainable_development_goals }}</span>
            </div>
          </div>
        </section>

        <section class="form-section" ref="section_executive_summary">
          <div class="section-header">
            <span class="section-number">09</span>
            <h3>Executive Summary</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.executive_summary }">
              <textarea v-model="proposal.executive_summary" placeholder="Not to exceed 200 words" rows="5"
                class="word-limit"></textarea>
              <span class="field-error" v-if="showValidation && errors.executive_summary">{{ errors.executive_summary
              }}</span>
            </div>
            <span class="char-count">{{ proposal.executive_summary.length }} / ~1400 chars</span>
          </div>
        </section>

        <!-- 10. INTRODUCTION -->
        <section class="form-section" ref="section_introduction">
          <div class="section-header">
            <span class="section-number">10</span>
            <h3>Introduction</h3>
          </div>
          <div class="section-card">
            <div class="sub-section">
              <h4>10.1 Rationale / Significance</h4>
              <p class="field-note">Not to exceed 300 words</p>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.rationale }">
                <textarea v-model="proposal.rationale" rows="4"></textarea>
                <span class="field-error" v-if="showValidation && errors.rationale">{{ errors.rationale }}</span>
              </div>
            </div>
            <div class="sub-section">
              <h4>10.2 Scientific Basis / Theoretical Framework</h4>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.theoretical_framework }">
                <textarea v-model="proposal.theoretical_framework" rows="4"></textarea>
                <span class="field-error" v-if="showValidation && errors.theoretical_framework">{{
                  errors.theoretical_framework }}</span>
              </div>
            </div>
            <div class="sub-section">
              <h4>10.3 Objectives</h4>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.general_objective }">
                <label>General Objective: <span class="req">*</span></label>
                <textarea v-model="proposal.general_objective" rows="3"></textarea>
                <span class="field-error" v-if="showValidation && errors.general_objective">{{ errors.general_objective
                }}</span>
              </div>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.specific_objectives }">
                <label>Specific Objectives: <span class="req">*</span></label>
                <textarea v-model="proposal.specific_objectives" rows="3"></textarea>
                <span class="field-error" v-if="showValidation && errors.specific_objectives">{{
                  errors.specific_objectives }}</span>
              </div>
            </div>
          </div>
        </section>

        <!-- 11. Review of Literature -->
      <section class="form-section" ref="section_review_of_literature">
        <div class="section-header">
          <span class="section-number">11</span>
          <h3>Review of Literature</h3>
        </div>
        <div class="section-card">
          <div class="form-group" :class="{ 'has-error': showValidation && errors.review_of_literature }">
            <textarea
              v-model="proposal.review_of_literature"
              placeholder="Enter your review of literature..."
              rows="5"
            ></textarea>
            <span class="field-error" v-if="showValidation && errors.review_of_literature">
              {{ errors.review_of_literature }}
            </span>
          </div>
        </div>
      </section>

        <!-- 12. Methodology -->
        <section class="form-section" ref="section_methodology">
          <div class="section-header">
            <span class="section-number">12</span>
            <h3>Methodology</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.methodology }">
              <textarea v-model="proposal.methodology" placeholder="Describe the research methodology..."
                rows="5"></textarea>
              <span class="field-error" v-if="showValidation && errors.methodology">{{ errors.methodology }}</span>
            </div>
          </div>
        </section>

        <!-- 13. Technology Roadmap -->
        <section class="form-section" ref="section_technology">
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
        <section class="form-section" ref="section_expected_outputs">
          <div class="section-header">
            <span class="section-number">14</span>
            <h3>Expected Outputs (6Ps)</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.expected_outputs }">
            <div class="agenda-grid">
              <div v-for="p in sixPsList" :key="p.key" class="agenda-item"
                :class="{ selected: proposal.expected_outputs[p.key].selected }">
                <label class="agenda-checkbox">
                  <input type="checkbox" v-model="proposal.expected_outputs[p.key].selected" />
                  <span class="checkmark"></span>
                  <span class="agenda-label">{{ p.label }}</span>
                </label>
                <div class="sixp-value" v-if="proposal.expected_outputs[p.key].selected">
                  <textarea v-model="proposal.expected_outputs[p.key].value" :placeholder="p.placeholder"
                    rows="3"></textarea>
                </div>
              </div>
            </div>
            <span class="field-error" v-if="showValidation && errors.expected_outputs">{{ errors.expected_outputs
            }}</span>
          </div>
        </section>

        <section class="form-section" ref="section_potential_outcomes">
          <div class="section-header">
            <span class="section-number">15</span>
            <h3>Potential Outcomes</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.potential_outcomes }">
              <textarea v-model="proposal.potential_outcomes" placeholder="Describe potential outcomes..."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.potential_outcomes">{{ errors.potential_outcomes
              }}</span>
            </div>
          </div>
        </section>

        <section class="form-section" ref="section_impacts">
          <div class="section-header">
            <span class="section-number">16</span>
            <h3>Potential Impacts (2Is)</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.economic_impact }">
              <label>a. Economic Impact: <span class="req">*</span></label>
              <textarea v-model="proposal.economic_impact" rows="3"></textarea>
              <span class="field-error" v-if="showValidation && errors.economic_impact">{{ errors.economic_impact
              }}</span>
            </div>
            <div class="form-group" :class="{ 'has-error': showValidation && errors.social_ethical_impact }">
              <label>b. Social / Ethical Impact: <span class="req">*</span></label>
              <textarea v-model="proposal.social_ethical_impact" rows="3"></textarea>
              <span class="field-error" v-if="showValidation && errors.social_ethical_impact">{{
                errors.social_ethical_impact }}</span>
            </div>
          </div>
        </section>

        <!-- 17. Target Beneficiaries -->
        <section class="form-section" ref="section_target_beneficiaries">
          <div class="section-header">
            <span class="section-number">17</span>
            <h3>Target Beneficiaries</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.target_beneficiaries }">
              <textarea v-model="proposal.target_beneficiaries" placeholder="Describe the target beneficiaries..."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.target_beneficiaries">{{
                errors.target_beneficiaries }}</span>
            </div>
          </div>
        </section>

        <!-- 18. Sustainability Plan -->
        <section class="form-section" ref="section_sustainability_plan">
          <div class="section-header">
            <span class="section-number">18</span>
            <h3>Sustainability Plan</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.sustainability_plan }">
              <textarea v-model="proposal.sustainability_plan" placeholder="Describe the sustainability plan..."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.sustainability_plan">{{
                errors.sustainability_plan }}</span>
            </div>
          </div>
        </section>

        <!-- 19. GAD Score -->
        <section class="form-section" ref="section_gad_score">
          <div class="section-header">
            <span class="section-number">19</span>
            <h3>Gender and Development (GAD) Score</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.gad_score_file }">
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
            <span class="field-error" v-if="showValidation && errors.gad_score_file">{{ errors.gad_score_file }}</span>
          </div>
        </section>

        <!-- 20. Limitations -->
        <section class="form-section" ref="section_limitations">
          <div class="section-header">
            <span class="section-number">20</span>
            <h3>Limitations of the Project</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.limitations }">
              <textarea v-model="proposal.limitations" placeholder="Describe project limitations..."
                rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.limitations">{{ errors.limitations }}</span>
            </div>
          </div>
        </section>

        <!-- 21. Risks -->
        <section class="form-section" ref="section_risks">
          <div class="section-header">
            <span class="section-number">21</span>
            <h3>Risk Management Plan</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.risks_assumptions }">
              <textarea v-model="proposal.risks_assumptions"
                placeholder="List risks, assumptions, and mitigation strategies..." rows="4"></textarea>
              <span class="field-error" v-if="showValidation && errors.risks_assumptions">{{ errors.risks_assumptions
              }}</span>
            </div>
          </div>
        </section>

        <!-- 22. Logical Framework -->
        <section class="form-section" ref="section_logical_framework">
          <div class="section-header">
            <span class="section-number">22</span>
            <h3>Logical Framework</h3>
            <span class="section-sub">Outcome and Output Indicators</span>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.logical_framework }">
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
                    <td><input v-model="row.output_indicator" placeholder="e.g. 5 filtration units installed" /></td>
                    <td><button class="btn-icon btn-remove"
                        @click="removeTableRow('logical_framework', index)">✕</button></td>
                  </tr>
                </tbody>
              </table>
            </div>
            <span class="field-error" v-if="showValidation && errors.logical_framework">{{ errors.logical_framework
            }}</span>
            <button class="btn-add" @click="addTableRow('logical_framework')">+ Add Row</button>
          </div>
        </section>

        <!-- 23. Literature Cited -->
        <section class="form-section" ref="section_literature_cited">
          <div class="section-header">
            <span class="section-number">23</span>
            <h3>Literature Cited</h3>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.literature_cited }">
              <textarea v-model="proposal.literature_cited" placeholder="List your references in APA format..."
                rows="5"></textarea>
              <span class="field-error" v-if="showValidation && errors.literature_cited">{{ errors.literature_cited
              }}</span>
            </div>
          </div>
        </section>

        <!-- 24. Personnel -->
        <section class="form-section" ref="section_personnel">
          <div class="section-header">
            <span class="section-number">24</span>
            <h3>Personnel Requirement</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.personnel_requirements }">
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
            <span class="field-error" v-if="showValidation && errors.personnel_requirements">{{
              errors.personnel_requirements }}</span>
            <button class="btn-add" @click="addTableRow('personnel_requirements')">+ Add Row</button>
          </div>
        </section>

        <!-- 25. Budget -->
        <section class="form-section" ref="section_budget">
          <div class="section-header">
            <span class="section-number">25</span>
            <h3>Line-Item Budget Requirement</h3>
          </div>
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.line_item_budget_file }">
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
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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
            <span class="field-error" v-if="showValidation && errors.line_item_budget_file">{{
              errors.line_item_budget_file }}</span>
          </div>
        </section>

        <!-- 26. Other Projects -->
        <section class="form-section" ref="section_other_projects">
          <div class="section-header">
            <span class="section-number">26</span>
            <h3>Other Ongoing Projects</h3>
            <span class="section-sub">Being handled by the Project Leader</span>
          </div>
          <div class="section-card">
            <div class="form-group" :class="{ 'has-error': showValidation && errors.other_projects_number }">
              <label>Number of other projects:</label>
              <input v-model="proposal.other_projects_number" placeholder="e.g. 2" class="input-short" />
              <span class="field-error" v-if="showValidation && errors.other_projects_number">{{
                errors.other_projects_number }}</span>
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
                    <td><button class="btn-icon btn-remove" @click="removeTableRow('other_projects', index)">✕</button>
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

      <!-- FOOTER -->
      <footer class="modal-footer">
        <button class="btn-secondary" @click="close">Cancel</button>
        <div class="footer-right">
          <button class="btn-outline" @click="saveAsDraft">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
              <polyline points="17 21 17 13 7 13 7 21" />
              <polyline points="7 3 7 8 15 8" />
            </svg>
            Save as Draft
          </button>
          <button class="btn-primary" @click="goNext">
            Next Step
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </div>
      </footer>
    </div>
  </div>

  <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
    @cancel="dialogState.onCancel" @close="dialogState.show = false" />
</template>

<script setup>
import { reactive, ref, computed, nextTick } from 'vue'
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

const { dialogState, showAlert, showConfirm } = useDialog()

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'save', 'next'])

const modalBodyRef = ref(null)
const errorBannerRef = ref(null)
const showValidation = ref(false)
const errors = reactive({})

const sectionRefs = {
  program_title: 'section_project_profile',
  project_title: 'section_project_profile',
  project_leader: 'section_project_profile',
  project_leader_sex: 'section_project_profile',
  duration: 'section_project_profile',
  start_date: 'section_project_profile',
  end_date: 'section_project_profile',
  department: 'section_project_profile',
  address: 'section_project_profile',
  cooperating_agencies: 'section_cooperating_agencies',
  sites: 'section_sites',
  research_type: 'section_research_type',
  priority_agendas: 'section_priority_agendas',
  innovation_goals: 'section_innovation_goals',
  sector_relevance: 'section_sector_relevance',
  sustainable_development_goals: 'section_sdg',
  executive_summary: 'section_executive_summary',
  rationale: 'section_introduction',
  theoretical_framework: 'section_introduction',
  general_objective: 'section_introduction',
  specific_objectives: 'section_introduction',
  review_of_literature: 'section_review_of_literature',
  methodology: 'section_methodology',
  expected_outputs: 'section_expected_outputs',
  potential_outcomes: 'section_potential_outcomes',
  economic_impact: 'section_impacts',
  social_ethical_impact: 'section_impacts',
  target_beneficiaries: 'section_target_beneficiaries',
  sustainability_plan: 'section_sustainability_plan',
  gad_score_file: 'section_gad_score',
  limitations: 'section_limitations',
  risks_assumptions: 'section_risks',
  logical_framework: 'section_logical_framework',
  literature_cited: 'section_literature_cited',
  personnel_requirements: 'section_personnel',
  line_item_budget_file: 'section_budget',
  other_projects_number: 'section_other_projects'
}

const sectionElementRefs = {}

const getInitialProposal = () => ({
  proposal_id: null,
  program_title: '',
  project_title: '',
  project_leader: '',
  project_leader_sex: '',
  duration: '',
  start_date: '',
  end_date: '',
  department: '',
  address: '',
  other_projects_number: '',
  cooperating_agencies: '',
  sites: [{ country: '', region: '', province: '', district: '', municipality: '', barangay: '' }],
  research_type: '',
  priority_agendas: {
    dagat: { selected: false, value: '' },
    punla: { selected: false, value: '' },
    kalikasan: { selected: false, value: '' },
    negosyo: { selected: false, value: '' },
    tanglaw: { selected: false, value: '' }
  },
  innovation_goals: '',
  sector_relevance: '',
  sustainable_development_goals: '',
  executive_summary: '',
  rationale: '',
  theoretical_framework: '',
  general_objective: '',
  specific_objectives: '',
  methodology: '',
  expected_outputs: {
    publications: { selected: false, value: '' },
    patents: { selected: false, value: '' },
    products: { selected: false, value: '' },
    people_services: { selected: false, value: '' },
    partnerships: { selected: false, value: '' },
    policy: { selected: false, value: '' }
  },
  potential_outcomes: '',
  economic_impact: '',
  social_ethical_impact: '',
  target_beneficiaries: '',
  sustainability_plan: '',
  limitations: '',
  risks_assumptions: '',
  review_of_literature: '',
  technology_roadmap_file: null,
  gad_score_file: null,
  line_item_budget_file: null,
  supporting_documents: [],
  logical_framework: [{ outcome_indicator: '', output_indicator: '' }],
  literature_cited: '',
  personnel_requirements: [{ position: '', effort: '', responsibilities: '' }],
  other_projects: [{ project_title: '', funding_agency: '', involvement: '' }]
})

const proposal = reactive(getInitialProposal())

const resetForm = () => {
  const initial = getInitialProposal()
  Object.keys(initial).forEach(key => {
    proposal[key] = initial[key]
  })
  showValidation.value = false
  Object.keys(errors).forEach(key => delete errors[key])
}

const loadProposal = (data) => {
  // Map backend DTO fields to form fields
  proposal.proposal_id = data.id || null
  proposal.program_title = data.programTitle || ''
  proposal.project_title = data.projectTitle || ''
  proposal.project_leader = data.projectLeader || ''
  proposal.project_leader_sex = data.projectLeaderSex || ''
  proposal.duration = data.duration || ''
  proposal.start_date = data.startDate || ''
  proposal.end_date = data.endDate || ''
  proposal.department = data.college || ''
  proposal.address = data.address || ''
  proposal.cooperating_agencies = data.cooperatingAgencies || ''
  proposal.research_type = data.researchType || ''
  proposal.innovation_goals = data.innovationGoals || ''
  proposal.sector_relevance = data.sectorRelevance || ''
  proposal.sustainable_development_goals = data.sdg || ''
  proposal.executive_summary = data.executiveSummary || ''
  proposal.rationale = data.rationale || ''
  proposal.theoretical_framework = data.framework || ''
  proposal.general_objective = data.objectivesGeneral || ''
  proposal.specific_objectives = data.objectivesSpecific || ''
  proposal.methodology = data.methodology || ''
  proposal.expected_outputs = parseSixPs(data.outputs)
  proposal.potential_outcomes = data.outcomes || ''
  proposal.economic_impact = data.impactEconomic || ''
  proposal.social_ethical_impact = data.impactSocial || ''
  proposal.target_beneficiaries = data.beneficiaries || ''
  proposal.sustainability_plan = data.sustainability || ''
  proposal.risks_assumptions = data.risks || ''
  proposal.other_projects_number = data.otherProjectsNumber || ''
  proposal.literature_cited = data.literatureCited || ''

  // Map sites
  if (data.sites && data.sites.length > 0) {
    proposal.sites = data.sites.map(s => ({
      country: s.country || '',
      region: s.region || '',
      province: s.province || '',
      district: s.district || '',
      municipality: s.municipality || '',
      barangay: s.barangay || ''
    }))
  }

  // Map priority agendas
  if (data.priorityAgendas) {
    Object.keys(data.priorityAgendas).forEach(key => {
      if (proposal.priority_agendas[key]) {
        proposal.priority_agendas[key].selected = data.priorityAgendas[key].selected || false
        proposal.priority_agendas[key].value = data.priorityAgendas[key].value || ''
      }
    })
  }

  // Map logical framework
  if (data.logFrame && data.logFrame.length > 0) {
    proposal.logical_framework = data.logFrame.map(lf => ({
      outcome_indicator: lf.outcome || '',
      output_indicator: lf.output || ''
    }))
  }

  // Map personnel
  if (data.personnel && data.personnel.length > 0) {
    proposal.personnel_requirements = data.personnel.map(p => ({
      position: p.position || '',
      effort: p.time || '',
      responsibilities: p.responsibilities || ''
    }))
  }

  // Map other projects
  if (data.otherProjects && data.otherProjects.length > 0) {
    proposal.other_projects = data.otherProjects.map(op => ({
      project_title: op.title || '',
      funding_agency: op.agency || '',
      involvement: op.involvement || ''
    }))
  }

  showValidation.value = false
  Object.keys(errors).forEach(key => delete errors[key])
}

defineExpose({ resetForm, loadProposal })

const programList = [
  { key: 'dagat', label: 'DAGAT Program', subLabel: 'Commodity' },
  { key: 'punla', label: 'PUNLA Program', subLabel: 'Priority Topic' },
  { key: 'kalikasan', label: 'KALIKASAN Program', subLabel: 'Sector' },
  { key: 'negosyo', label: 'NEGOSYO Program', subLabel: 'Sector' },
  { key: 'tanglaw', label: 'TANGLAW Program', subLabel: 'Sector' },
]

const sixPsList = [
  { key: 'publications', label: 'Publications', placeholder: 'Describe the expected publication/output...' },
  { key: 'patents', label: 'Patents/IP', placeholder: 'Describe the expected patent or intellectual property...' },
  { key: 'products', label: 'Products', placeholder: 'Describe the expected product...' },
  { key: 'people_services', label: 'People Services', placeholder: 'Describe the expected people service...' },
  { key: 'partnerships', label: 'Partnerships', placeholder: 'Describe the expected partnership...' },
  { key: 'policy', label: 'Policy', placeholder: 'Describe the expected policy output...' },
]

const serializeSixPs = (expectedOutputs) => {
  const lines = []
  sixPsList.forEach((p) => {
    const item = expectedOutputs && expectedOutputs[p.key]
    if (item && item.selected && item.value && item.value.trim() !== '') {
      lines.push(`${p.label}: ${item.value.trim()}`)
    }
  })
  return lines.join('\n')
}

const parseSixPs = (raw) => {
  const result = {}
  sixPsList.forEach((p) => { result[p.key] = { selected: false, value: '' } })
  if (!raw || typeof raw !== 'string' || raw.trim() === '') return result

  const lines = raw.split(/\r?\n/)
  let currentKey = null
  lines.forEach((line) => {
    const found = sixPsList.find((p) => line.startsWith(p.label + ':'))
    if (found) {
      currentKey = found.key
      result[currentKey].selected = true
      result[currentKey].value = line.slice(found.label.length + 1).trim()
    } else if (currentKey && line.trim() !== '') {
      result[currentKey].value += (result[currentKey].value ? '\n' : '') + line.trim()
    }
  })
  return result
}

const close = () => emit('update:modelValue', false)
const addSite = () => proposal.sites.push({ country: '', region: '', province: '', district: '', municipality: '', barangay: '' })
const removeSite = (index) => proposal.sites.splice(index, 1)

const addTableRow = (key) => {
  const templates = {
    logical_framework: { outcome_indicator: '', output_indicator: '' },
    personnel_requirements: { position: '', effort: '', responsibilities: '' },
    other_projects: { project_title: '', funding_agency: '', involvement: '' }
  }
  proposal[key].push(templates[key] || {})
}

const removeTableRow = (key, index) => proposal[key].splice(index, 1)

const handleFileUpload = (type, event) => {
  const file = event.target.files[0]
  if (!file) return
  const map = {
    technology: 'technology_roadmap_file',
    gad: 'gad_score_file',
    budget: 'line_item_budget_file'
  }
  if (map[type]) proposal[map[type]] = file
  else if (type === 'supporting') proposal.supporting_documents.push(file)
}

const downloadForm = () => {
  const data = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(proposal, null, 2))
  const a = document.createElement('a')
  a.href = data
  a.download = 'research-proposal.json'
  a.click()
}

// ===== VALIDATION =====
const isBlank = (val) => !val || (typeof val === 'string' && val.trim() === '')

const validateStep1 = () => {
  // Clear previous errors
  Object.keys(errors).forEach(key => delete errors[key])

  // 01 - Project Profile
  if (isBlank(proposal.project_title)) errors.project_title = 'Project Title is required.'
  if (isBlank(proposal.project_leader)) errors.project_leader = 'Project Leader is required.'
  if (isBlank(proposal.project_leader_sex)) errors.project_leader_sex = 'Please select a sex.'
  if (isBlank(proposal.duration)) errors.duration = 'Duration is required.'
  if (isBlank(proposal.start_date)) errors.start_date = 'Start Date is required.'
  if (isBlank(proposal.end_date)) errors.end_date = 'End Date is required.'
  if (isBlank(proposal.department)) errors.department = 'Implementing College/Department is required.'
  if (isBlank(proposal.address)) errors.address = 'Address / Contact is required.'

  // 02 - Cooperating Agencies
  if (isBlank(proposal.cooperating_agencies)) errors.cooperating_agencies = 'Cooperating Agencies is required.'

  // 03 - Sites of Implementation
  if (!proposal.sites.length) {
    errors.sites = 'At least one site of implementation is required.'
  } else {
    const siteFields = ['country', 'region', 'province', 'district', 'municipality', 'barangay']
    const hasIncompleteSite = proposal.sites.some(site =>
      siteFields.some(f => isBlank(site[f]))
    )
    if (hasIncompleteSite) errors.sites = 'All site location fields must be filled for each site.'
  }

  // 04 - Research Type
  if (isBlank(proposal.research_type)) errors.research_type = 'Please select a research type.'

  // 05 - Priority Agenda
  const selectedAgendas = Object.entries(proposal.priority_agendas).filter(([, v]) => v.selected)
  if (selectedAgendas.length === 0) {
    errors.priority_agendas = 'Please select at least one Priority Agenda.'
  } else {
    const hasEmptyValue = selectedAgendas.some(([, v]) => isBlank(v.value))
    if (hasEmptyValue) errors.priority_agendas = 'Please fill in the details for each selected Priority Agenda.'
  }

  // 06-09 - Text sections
  if (isBlank(proposal.innovation_goals)) errors.innovation_goals = 'Innovation Goals is required.'
  if (isBlank(proposal.sector_relevance)) errors.sector_relevance = 'Sector Relevance is required.'
  if (isBlank(proposal.sustainable_development_goals)) errors.sustainable_development_goals = 'SDG is required.'
  if (isBlank(proposal.executive_summary)) errors.executive_summary = 'Executive Summary is required.'

  // 10 - Introduction
  if (isBlank(proposal.rationale)) errors.rationale = 'Rationale / Significance is required.'
  if (isBlank(proposal.theoretical_framework)) errors.theoretical_framework = 'Theoretical Framework is required.'
  if (isBlank(proposal.general_objective)) errors.general_objective = 'General Objective is required.'
  if (isBlank(proposal.specific_objectives)) errors.specific_objectives = 'Specific Objectives is required.'

  // 11 - Review of Literature
if (isBlank(proposal.review_of_literature)) errors.review_of_literature = 'Review of Literature is required.'

  // 12 - Methodology
  if (isBlank(proposal.methodology)) errors.methodology = 'Methodology is required.'

  // 14-16 - Outputs, Outcomes, Impacts
  const selectedOutputs = Object.entries(proposal.expected_outputs).filter(([, v]) => v.selected)
  if (selectedOutputs.length === 0) {
    errors.expected_outputs = 'Please select at least one Expected Output (6P).'
  } else {
    const hasEmptyValue = selectedOutputs.some(([, v]) => isBlank(v.value))
    if (hasEmptyValue) errors.expected_outputs = 'Please fill in the details for each selected Expected Output (6P).'
  }
  if (isBlank(proposal.potential_outcomes)) errors.potential_outcomes = 'Potential Outcomes is required.'
  if (isBlank(proposal.economic_impact)) errors.economic_impact = 'Economic Impact is required.'
  if (isBlank(proposal.social_ethical_impact)) errors.social_ethical_impact = 'Social / Ethical Impact is required.'

  // 17-18
  if (isBlank(proposal.target_beneficiaries)) errors.target_beneficiaries = 'Target Beneficiaries is required.'
  if (isBlank(proposal.sustainability_plan)) errors.sustainability_plan = 'Sustainability Plan is required.'

  // 19 - GAD Score
  if (!proposal.gad_score_file) errors.gad_score_file = 'GAD Score document is required.'

  // 20-21
  if (isBlank(proposal.limitations)) errors.limitations = 'Limitations is required.'
  if (isBlank(proposal.risks_assumptions)) errors.risks_assumptions = 'Risk Management Plan is required.'

  // 22 - Logical Framework
  if (!proposal.logical_framework.length) {
    errors.logical_framework = 'At least one Logical Framework row is required.'
  } else {
    const hasIncompleteRow = proposal.logical_framework.some(row =>
      isBlank(row.outcome_indicator) || isBlank(row.output_indicator)
    )
    if (hasIncompleteRow) errors.logical_framework = 'All Logical Framework fields must be filled.'
  }

  // 23 - Literature Cited
  if (isBlank(proposal.literature_cited)) errors.literature_cited = 'Literature Cited is required.'

  // 24 - Personnel
  if (!proposal.personnel_requirements.length) {
    errors.personnel_requirements = 'At least one personnel entry is required.'
  } else {
    const hasIncompletePersonnel = proposal.personnel_requirements.some(row =>
      isBlank(row.position) || isBlank(row.effort) || isBlank(row.responsibilities)
    )
    if (hasIncompletePersonnel) errors.personnel_requirements = 'All personnel fields must be filled.'
  }

  // 25 - Budget
  if (!proposal.line_item_budget_file) errors.line_item_budget_file = 'Line-Item Budget file is required.'

  

  return Object.keys(errors).length === 0
}

const errorList = computed(() => {
  return Object.entries(errors).map(([field, message]) => ({
    field,
    message
  }))
})

const scrollToField = (fieldName) => {
  const sectionName = sectionRefs[fieldName]
  if (!sectionName) return
  // Find the section element by ref in the template
  const sectionEl = document.querySelector(`[ref="${sectionName}"]`) ||
    document.querySelector(`section[ref="${sectionName}"]`)
  // Fallback: try to find by data attribute or scroll to error banner
  if (sectionEl) {
    sectionEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
  } else if (modalBodyRef.value) {
    // Try to find the first has-error element
    const errorEl = modalBodyRef.value.querySelector('.has-error')
    if (errorEl) {
      errorEl.scrollIntoView({ behavior: 'smooth', block: 'center' })
    } else {
      modalBodyRef.value.scrollTo({ top: 0, behavior: 'smooth' })
    }
  }
}

const saveAsDraft = async () => {
  try {
    const userStore = useUserDataStore()
    const proponentId = userStore.user?.id
    if (!proponentId) {
      await showAlert('You must be logged in to save drafts.', { type: 'warning', title: 'Login Required' })
      return
    }
    const payload = mapFormToDTO({ ...proposal }, proponentId, 'DRAFT')
    let res
    if (proposal.proposal_id) {
      // Update existing draft
      res = await api.put(`/api/proposals/${proposal.proposal_id}`, payload)
    } else {
      // Create new draft
      res = await api.post('/api/proposals', payload)
    }
    await showAlert('Draft saved successfully!', { type: 'success', title: 'Draft Saved' })
    emit('save', res.data)
    emit('update:modelValue', false)
  } catch (err) {
    console.error(err)
    await showAlert('Failed to save draft. Please try again.', { type: 'error', title: 'Save Failed' })
  }
}

const goNext = async () => {
  showValidation.value = true
  const isValid = validateStep1()
  if (!isValid) {
    // Scroll to error banner
    await nextTick()
    if (errorBannerRef.value) {
      errorBannerRef.value.scrollIntoView({ behavior: 'smooth', block: 'start' })
    } else if (modalBodyRef.value) {
      const firstError = modalBodyRef.value.querySelector('.has-error')
      if (firstError) {
        firstError.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
    await showAlert(
      `Please fill in all ${errorList.value.length} required field(s) before proceeding to the next step.`,
      { type: 'warning', title: 'Incomplete Proposal' }
    )
    return
  }
  emit('next', { ...proposal })
}

const downloadBudgetFormat = () => window.open('/templates/Project-Line-Item-Budget.docx')

const mapFormToDTO = (data, proponentId, status) => {
  return {
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
    outputs: serializeSixPs(data.expected_outputs),
    outcomes: data.potential_outcomes || '',
    impactEconomic: data.economic_impact || '',
    impactSocial: data.social_ethical_impact || '',
    beneficiaries: data.target_beneficiaries || '',
    sustainability: data.sustainability_plan || '',
    risks: data.risks_assumptions || '',
    otherProjectsNumber: data.other_projects_number || '',
    status,
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
}
</script>

<style scoped>
/* ===== BACKDROP ===== */
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  padding: 20px;
}

/* ===== CONTAINER ===== */
.modal-container {
  background: #f8fafc;
  width: 100%;
  max-width: 1100px;
  max-height: 92vh;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ===== HEADER ===== */
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left h2 {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.header-badge {
  background: #eef2ff;
  color: #4f46e5;
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 20px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:hover {
  background: #e2e8f0;
  color: #1e293b;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

/* ===== PROGRESS BAR ===== */
.progress-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  padding: 20px 28px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.progress-step {
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: #94a3b8;
  transition: all 0.2s;
}

.progress-step.active .step-number {
  background: #4f46e5;
  color: #fff;
}

.step-label {
  font-size: 13px;
  font-weight: 500;
  color: #94a3b8;
}

.progress-step.active .step-label {
  color: #1e293b;
  font-weight: 600;
}

.progress-line {
  width: 60px;
  height: 2px;
  background: #e2e8f0;
  margin: 0 12px;
}

/* ===== VALIDATION BANNER ===== */
.validation-banner {
  display: flex;
  gap: 12px;
  padding: 14px 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 4px solid #dc2626;
  margin: 0;
  flex-shrink: 0;
  max-height: 150px;
  overflow-y: auto;
}

.banner-icon {
  font-size: 20px;
  color: #dc2626;
  flex-shrink: 0;
  line-height: 1;
}

.banner-content {
  flex: 1;
}

.banner-content strong {
  display: block;
  font-size: 14px;
  color: #991b1b;
  margin-bottom: 6px;
}

.banner-content ul {
  margin: 0;
  padding-left: 18px;
  list-style: disc;
}

.banner-content li {
  font-size: 12px;
  color: #b91c1c;
  margin-bottom: 2px;
  line-height: 1.4;
}

.banner-content li a {
  color: #b91c1c;
  text-decoration: underline;
  cursor: pointer;
}

.banner-content li a:hover {
  color: #991b1b;
}

/* ===== BODY ===== */
.modal-body {
  padding: 24px 28px;
  overflow-y: auto;
  flex: 1;
}

/* ===== FORM SECTIONS ===== */
.form-section {
  margin-bottom: 28px;
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 12px;
}

.section-number {
  font-size: 13px;
  font-weight: 700;
  color: #4f46e5;
  background: #eef2ff;
  padding: 2px 8px;
  border-radius: 4px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.section-sub {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
}

.section-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  transition: box-shadow 0.15s;
}

.section-card:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ===== VALIDATION ERROR STYLES ===== */
.req {
  color: #dc2626;
  font-weight: 400;
}

.has-error input,
.has-error select,
.has-error textarea {
  border-color: #dc2626 !important;
  background: #fef2f2;
}

.has-error input:focus,
.has-error select:focus,
.has-error textarea:focus {
  box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.1);
  border-color: #dc2626 !important;
}

.has-error-card {
  border-color: #dc2626 !important;
}

.field-error {
  display: block;
  font-size: 12px;
  color: #dc2626;
  margin-top: 4px;
  font-weight: 500;
}

/* ===== FORM ELEMENTS ===== */
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 0;
}

.form-row.three-col {
  grid-template-columns: 1fr 1fr 1fr;
}

.form-group {
  margin-bottom: 14px;
}

.form-group:last-child {
  margin-bottom: 0;
}

.form-group label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

input,
select,
textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-size: 14px;
  color: #1e293b;
  background: #fff;
  transition: border-color 0.15s, box-shadow 0.15s;
  box-sizing: border-box;
  font-family: inherit;
}

input:focus,
select:focus,
textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

input::placeholder,
textarea::placeholder {
  color: #94a3b8;
}

textarea {
  resize: vertical;
  min-height: 80px;
}

select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 24 24' fill='none' stroke='%2394a3b8' stroke-width='2'%3E%3Cpolyline points='6 9 12 15 18 9'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 36px;
}

.input-short {
  max-width: 200px;
}

.char-count {
  display: block;
  text-align: right;
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

.field-note {
  font-size: 12px;
  color: #94a3b8;
  font-style: italic;
  margin: 0 0 10px 0;
}

/* ===== SUB-SECTIONS ===== */
.sub-section {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f1f5f9;
}

.sub-section:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.sub-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 6px 0;
}

/* ===== TABLES ===== */
.table-wrapper {
  overflow-x: auto;
  margin-bottom: 12px;
}

.form-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.form-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.form-table td {
  padding: 6px 8px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: middle;
}

.form-table td.row-num {
  text-align: center;
  font-weight: 600;
  color: #94a3b8;
  width: 30px;
}

.form-table input {
  border: 1px solid #e2e8f0;
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  width: 100%;
  box-sizing: border-box;
}

.form-table input:focus {
  border-color: #4f46e5;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.08);
}

/* ===== BUTTONS ===== */
.btn-add {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px dashed #cbd5e1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #4f46e5;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-add:hover {
  border-color: #4f46e5;
  background: #eef2ff;
}

.btn-icon {
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.15s;
}

.btn-remove {
  background: none;
  color: #94a3b8;
}

.btn-remove:hover {
  background: #fef2f2;
  color: #dc2626;
}

.btn-remove-file {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  font-size: 14px;
  padding: 4px;
}

.btn-remove-file:hover {
  color: #dc2626;
}

/* ===== RADIO CARDS ===== */
.radio-group {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.radio-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border: 2px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
}

.radio-card:hover {
  border-color: #c7d2fe;
}

.radio-card.active {
  border-color: #4f46e5;
  background: #eef2ff;
}

.radio-card input {
  display: none;
}

.radio-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.radio-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
}

.radio-desc {
  font-size: 12px;
  color: #64748b;
}

/* ===== AGENDA ===== */
.agenda-grid {
  display: grid;
  gap: 10px;
}

.agenda-item {
  padding: 12px 16px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  transition: all 0.15s;
}

.agenda-item.selected {
  border-color: #4f46e5;
  background: #f8faff;
}

.agenda-checkbox {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
}

.agenda-checkbox input {
  display: none;
}

.checkmark {
  width: 20px;
  height: 20px;
  border: 2px solid #cbd5e1;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
  flex-shrink: 0;
}

.agenda-checkbox input:checked+.checkmark {
  background: #4f46e5;
  border-color: #4f46e5;
}

.agenda-checkbox input:checked+.checkmark::after {
  content: '✓';
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.agenda-label {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.agenda-value {
  margin-top: 10px;
  margin-left: 30px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.agenda-sub {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
}

.agenda-value input {
  flex: 1;
  border: none;
  border-bottom: 1px solid #d1d5db;
  padding: 4px 0;
  font-size: 13px;
  background: transparent;
}

.agenda-value input:focus {
  outline: none;
  border-bottom-color: #4f46e5;
}

/* ===== EXPECTED OUTPUTS (6Ps) ===== */
.sixp-value {
  margin-top: 10px;
  margin-left: 30px;
}

.sixp-value textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  padding: 8px 10px;
  font-size: 13px;
  font-family: inherit;
  resize: vertical;
  background: #fff;
  transition: border-color 0.15s;
}

.sixp-value textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

/* ===== FILE UPLOAD ===== */
.file-upload {
  margin-bottom: 8px;
}

.upload-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 24px;
  border: 2px dashed #d1d5db;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.15s;
  color: #64748b;
}

.upload-area:hover {
  border-color: #4f46e5;
  background: #f8faff;
  color: #4f46e5;
}

.upload-area.small {
  flex-direction: row;
  padding: 10px 16px;
  gap: 8px;
}

.upload-hint {
  font-size: 11px;
  color: #94a3b8;
}

.file-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  margin-top: 8px;
  font-size: 13px;
  color: #475569;
}

.budget-actions {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.btn-outline {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #fff;
  border: 1px solid #d1d5db;
  padding: 10px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-outline:hover {
  border-color: #4f46e5;
  color: #4f46e5;
  background: #f8faff;
}

/* ===== FOOTER ===== */
.modal-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 28px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
  flex-shrink: 0;
}

.footer-right {
  display: flex;
  gap: 10px;
}

.btn-secondary {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  color: #475569;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

.btn-primary {
  display: flex;
  align-items: center;
  gap: 6px;
  background: #4f46e5;
  border: none;
  padding: 10px 24px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary:hover {
  background: #4338ca;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.3);
}

/* ===== SCROLLBAR ===== */
.modal-body::-webkit-scrollbar {
  width: 6px;
}

.modal-body::-webkit-scrollbar-track {
  background: transparent;
}

.modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 3px;
}

.modal-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>