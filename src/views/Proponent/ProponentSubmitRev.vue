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
          <p class="subtitle">{{ documentId || '—' }}</p>
        </div>
      </div>
    </header>

    <!-- REVISION DEADLINE -->
    <div v-if="revisionDeadline" class="deadline-banner" :style="deadlineStyle">
      <strong>Revision Deadline:</strong> {{ formatDeadline(revisionDeadline) }}
      <span v-if="isPastDeadline"> (deadline passed)</span>
    </div>

    <!-- DEADLINE PASSED NOTICE -->
    <div v-if="isPastDeadline" class="error-banner">
      <svg class="error-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <circle cx="12" cy="12" r="10"></circle>
        <line x1="12" y1="8" x2="12" y2="12"></line>
        <line x1="12" y1="16" x2="12.01" y2="16"></line>
      </svg>
      <span>The revision deadline has passed. Revision submission is no longer available.</span>
    </div>

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

    <!-- SECTION REMARKS SUMMARY -->
    <div v-if="flaggedSections.length > 0" class="remarks-summary">
      <div class="remarks-summary-head">
        <span class="remarks-summary-count">{{ flaggedSections.length }}</span>
        <div>
          <strong>
            {{ flaggedSections.length === 1 ? 'section needs' : 'sections need' }} revision
          </strong>
          <p class="remarks-summary-sub">
            Review each remark below, then edit the matching section in the form.
          </p>
        </div>
      </div>
      <div class="remarks-summary-list">
        <button
          v-for="section in flaggedSections"
          :key="section.key"
          type="button"
          class="remarks-chip"
          @click="scrollToSection(section.key)"
        >
          <span class="remarks-chip-number">{{ section.key }}</span>
          <span class="remarks-chip-label">{{ section.label }}</span>
          <span class="remarks-chip-count">{{ remarksFor(section.key).length }}</span>
        </button>
      </div>
    </div>

    <p v-if="remarksError" class="remarks-error">{{ remarksError }}</p>

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
              <SectionRemarksNotice :section-key="'01'" :remarks="remarksFor('01')" />
              <div class="section-card">
                <div class="form-row">
                  <div class="form-group">
                    <label>Program Title</label>
                    <input v-model="proposal.program_title" placeholder="e.g. Community Development Program" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'programTitle'" field-label="Program Title" :remarks="remarksForField('programTitle')" readonly />
                  </div>
                  <div class="form-group">
                    <label>Project Title</label>
                    <input v-model="proposal.project_title"
                      placeholder="e.g. Community-Based Water Filtration System" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'projectTitle'" field-label="Project Title" :remarks="remarksForField('projectTitle')" readonly />
                  </div>
                </div>
                <div class="form-row">
                  <div class="form-group">
                    <label>Project Leader</label>
                    <input v-model="proposal.project_leader" placeholder="Full name of project leader" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'projectLeader'" field-label="Project Leader" :remarks="remarksForField('projectLeader')" readonly />
                  </div>
                  <div class="form-group">
                    <label>Sex</label>
                    <select v-model="proposal.project_leader_sex">
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'projectLeaderSex'" field-label="Sex" :remarks="remarksForField('projectLeaderSex')" readonly />
                      <option value="">Select Sex</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Prefer not to say</option>
                    </select>
                  </div>
                </div>
                <div class="form-row three-col">
                  <div class="form-group">
                    <label>Duration (months)</label>
                    <input v-model="proposal.duration" placeholder="e.g. 12" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'duration'" field-label="Duration (months)" :remarks="remarksForField('duration')" readonly />
                  </div>
                  <div class="form-group">
                    <label>Start Date</label>
                    <input type="date" v-model="proposal.start_date" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'startDate'" field-label="Start Date" :remarks="remarksForField('startDate')" readonly />
                  </div>
                  <div class="form-group">
                    <label>End Date</label>
                    <input type="date" v-model="proposal.end_date" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'endDate'" field-label="End Date" :remarks="remarksForField('endDate')" readonly />
                  </div>
                </div>
                <div class="form-group">
                  <label>Implementing College / Department</label>
                  <input v-model="proposal.department" placeholder="e.g. College of Engineering" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'college'" field-label="Implementing College / Department" :remarks="remarksForField('college')" readonly />
                </div>
                <div class="form-group">
                  <label>Address / Contact</label>
                  <textarea v-model="proposal.address"
                    placeholder="Barangay, Municipality, District, Province, Region / Telephone / Fax / Email"
                    rows="3"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'01'" :field-key="'address'" field-label="Address / Contact" :remarks="remarksForField('address')" readonly />
                </div>
              </div>
            </section>

            <!-- 2. Cooperating Agency/ies -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">02</span>
                <h3>Cooperating Agencies</h3>
              </div>
              <SectionRemarksNotice :section-key="'02'" :remarks="remarksFor('02')" />
              <div class="section-card">
                <div class="form-group">
                  <label>Select all agencies that will collaborate on this research project</label>
                  <!-- The same shared directory checklist as the Create Proposal form, so a
                       revision edits the selection relationally instead of as free text. -->
                  <CooperatingAgenciesChecklist v-model:selected="proposal.cooperating_agency_ids"
                    v-model:names="proposal.cooperating_agency_names" :proposal-id="proposalId"
                    :legacy-selection="proposal.cooperating_agencies" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'02'" :field-key="'cooperatingAgencies'" field-label="Cooperating Agencies" :remarks="remarksForField('cooperatingAgencies')" readonly />
                </div>
              </div>
            </section>

            <!-- 3. Sites of Implementation -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">03</span>
                <h3>Sites of Implementation</h3>
              </div>
              <SectionRemarksNotice :section-key="'03'" :remarks="remarksFor('03')" />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].country`" field-label="Country" :remarks="remarksForField(`sites[${index}].country`)" readonly />
                        <td><input v-model="site.region" placeholder="" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].region`" field-label="Region" :remarks="remarksForField(`sites[${index}].region`)" readonly />
                        <td><input v-model="site.province" placeholder="" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].province`" field-label="Province" :remarks="remarksForField(`sites[${index}].province`)" readonly />
                        <td><input v-model="site.district" placeholder="" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].district`" field-label="District" :remarks="remarksForField(`sites[${index}].district`)" readonly />
                        <td><input v-model="site.municipality" placeholder="" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].municipality`" field-label="Municipality" :remarks="remarksForField(`sites[${index}].municipality`)" readonly />
                        <td><input v-model="site.barangay" placeholder="" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'03'" :field-key="`sites[${index}].barangay`" field-label="Barangay" :remarks="remarksForField(`sites[${index}].barangay`)" readonly />
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
              <SectionRemarksNotice :section-key="'04'" :remarks="remarksFor('04')" />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'04'" :field-key="'researchType'" field-label="Type of Research" :remarks="remarksForField('researchType')" readonly />
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
              <SectionRemarksNotice :section-key="'05'" :remarks="remarksFor('05')" />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'05'" :field-key="`priorityAgenda.${program.key}.value`" field-label="Priority Agenda" :remarks="remarksForField(`priorityAgenda.${program.key}.value`)" readonly />
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
              <SectionRemarksNotice :section-key="'06'" :remarks="remarksFor('06')" />
              <div class="section-card">
                <textarea v-model="proposal.innovation_goals"
                  placeholder="Describe the innovation goals of this project..." rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'06'" :field-key="'innovationGoals'" field-label="Innovation Goals" :remarks="remarksForField('innovationGoals')" readonly />
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">07</span>
                <h3>Sector Relevance</h3>
              </div>
              <SectionRemarksNotice :section-key="'07'" :remarks="remarksFor('07')" />
              <div class="section-card">
                <textarea v-model="proposal.sector_relevance"
                  placeholder="Public Health, Environment, Agriculture, etc." rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'07'" :field-key="'sectorRelevance'" field-label="Sector Relevance" :remarks="remarksForField('sectorRelevance')" readonly />
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">08</span>
                <h3>Sustainable Development Goals (SDG)</h3>
              </div>
              <SectionRemarksNotice :section-key="'08'" :remarks="remarksFor('08')" />
              <div class="section-card">
                <div class="form-group">
                  <label>Select all applicable SDGs</label>
                  <div class="sdg-checkbox-list">
                    <label v-for="goal in SDG_OPTIONS" :key="goal" class="sdg-checkbox">
                      <input type="checkbox" :value="goal" v-model="proposal.sustainable_development_goals" />
                      <span>{{ goal }}</span>
                    </label>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'08'" :field-key="'sdg'" field-label="Selected SDGs" :remarks="remarksForField('sdg')" readonly />
                  </div>
                </div>
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">09</span>
                <h3>Executive Summary</h3>
              </div>
              <SectionRemarksNotice :section-key="'09'" :remarks="remarksFor('09')" />
              <div class="section-card">
                <textarea v-model="proposal.executive_summary" placeholder="Not to exceed 200 words" rows="5"
                  class="word-limit"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'09'" :field-key="'executiveSummary'" field-label="Executive Summary" :remarks="remarksForField('executiveSummary')" readonly />
                <span class="char-count">{{ executiveWordCount }} / 200 words</span>
              </div>
            </section>

            <!-- 10. INTRODUCTION -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">10</span>
                <h3>Introduction</h3>
              </div>
              <SectionRemarksNotice :section-key="'10'" :remarks="remarksFor('10')" />
              <div class="section-card">
                <div class="sub-section">
                  <h4>10.1 Rationale / Significance</h4>
                  <p class="field-note">Not to exceed 300 words</p>
                  <textarea v-model="proposal.rationale" rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'10'" :field-key="'rationale'" field-label="Rationale / Significance" :remarks="remarksForField('rationale')" readonly />
                </div>
                <div class="sub-section">
                  <h4>10.2 Scientific Basis / Theoretical Framework</h4>
                  <textarea v-model="proposal.theoretical_framework" rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'10'" :field-key="'framework'" field-label="Scientific Basis / Theoretical Framework" :remarks="remarksForField('framework')" readonly />
                </div>
                <div class="sub-section">
                  <h4>10.3 Objectives</h4>
                  <div class="form-group">
                    <label>General Objective:</label>
                    <textarea v-model="proposal.general_objective" rows="3"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'10'" :field-key="'objectivesGeneral'" field-label="General Objective" :remarks="remarksForField('objectivesGeneral')" readonly />
                  </div>
                  <div class="form-group">
                    <label>Specific Objectives:</label>
                    <textarea v-model="proposal.specific_objectives" rows="3"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'10'" :field-key="'objectivesSpecific'" field-label="Specific Objectives" :remarks="remarksForField('objectivesSpecific')" readonly />
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
              <SectionRemarksNotice :section-key="'11'" :remarks="remarksFor('11')" />
              <div class="section-card">
                <div class="form-group">
                  <label>Review of Literature</label>
                  <textarea v-model="proposal.review_of_literature" placeholder="Enter your review of literature..."
                    rows="5"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'11'" :field-key="'review'" field-label="Review of Literature" :remarks="remarksForField('review')" readonly />
                </div>
                <div class="file-upload">
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'25'" :field-key="'beneficiariesFileName'" field-label="Line-Item Budget (document)" :remarks="remarksForField('beneficiariesFileName')" readonly />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'19'" :field-key="'gadFileName'" field-label="GAD Score (document)" :remarks="remarksForField('gadFileName')" readonly />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'13'" :field-key="'roadmapFileName'" field-label="Technology Roadmap (document)" :remarks="remarksForField('roadmapFileName')" readonly />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'11'" :field-key="'reviewFileName'" field-label="Review of Literature (document)" :remarks="remarksForField('reviewFileName')" readonly />
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
                    <span>{{ proposal.review_of_literature_file.name }}</span>
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
              <SectionRemarksNotice :section-key="'12'" :remarks="remarksFor('12')" />
              <div class="section-card">
                <textarea v-model="proposal.methodology" placeholder="Describe the research methodology..."
                  rows="5"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'12'" :field-key="'methodology'" field-label="Methodology" :remarks="remarksForField('methodology')" readonly />
              </div>
            </section>

            <!-- 13. Technology Roadmap -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">13</span>
                <h3>Technology Roadmap</h3>
                <span class="section-sub">(if applicable)</span>
              </div>
              <SectionRemarksNotice :section-key="'13'" :remarks="remarksFor('13')" />
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
                    <span>{{ proposal.technology_roadmap_file.name }}</span>
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
              <SectionRemarksNotice :section-key="'14'" :remarks="remarksFor('14')" />
              <div class="section-card">
                <textarea v-model="proposal.expected_outputs" placeholder="List expected outputs..."
                  rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'14'" :field-key="'outputs'" field-label="Expected Outputs (6Ps)" :remarks="remarksForField('outputs')" readonly />
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">15</span>
                <h3>Potential Outcomes</h3>
              </div>
              <SectionRemarksNotice :section-key="'15'" :remarks="remarksFor('15')" />
              <div class="section-card">
                <textarea v-model="proposal.potential_outcomes" placeholder="Describe potential outcomes..."
                  rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'15'" :field-key="'outcomes'" field-label="Potential Outcomes" :remarks="remarksForField('outcomes')" readonly />
              </div>
            </section>

            <section class="form-section">
              <div class="section-header">
                <span class="section-number">16</span>
                <h3>Potential Impacts (2Is)</h3>
              </div>
              <SectionRemarksNotice :section-key="'16'" :remarks="remarksFor('16')" />
              <div class="section-card">
                <div class="form-group">
                  <label>a. Economic Impact:</label>
                  <textarea v-model="proposal.economic_impact" rows="3"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'16'" :field-key="'impactEconomic'" field-label="Economic Impact" :remarks="remarksForField('impactEconomic')" readonly />
                </div>
                <div class="form-group">
                  <label>b. Social / Ethical Impact:</label>
                  <textarea v-model="proposal.social_ethical_impact" rows="3"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'16'" :field-key="'impactSocial'" field-label="Social / Ethical Impact" :remarks="remarksForField('impactSocial')" readonly />
                </div>
              </div>
            </section>

            <!-- 17. Target Beneficiaries -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">17</span>
                <h3>Target Beneficiaries</h3>
              </div>
              <SectionRemarksNotice :section-key="'17'" :remarks="remarksFor('17')" />
              <div class="section-card">
                <textarea v-model="proposal.target_beneficiaries" placeholder="Describe the target beneficiaries..."
                  rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'17'" :field-key="'beneficiaries'" field-label="Target Beneficiaries" :remarks="remarksForField('beneficiaries')" readonly />
              </div>
            </section>

            <!-- 18. Sustainability Plan -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">18</span>
                <h3>Sustainability Plan</h3>
              </div>
              <SectionRemarksNotice :section-key="'18'" :remarks="remarksFor('18')" />
              <div class="section-card">
                <textarea v-model="proposal.sustainability_plan" placeholder="Describe the sustainability plan..."
                  rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'18'" :field-key="'sustainability'" field-label="Sustainability Plan" :remarks="remarksForField('sustainability')" readonly />
              </div>
            </section>

            <!-- 19. GAD Score -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">19</span>
                <h3>Gender and Development (GAD) Score</h3>
              </div>
              <SectionRemarksNotice :section-key="'19'" :remarks="remarksFor('19')" />
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
                    <span>{{ proposal.gad_score_file.name }}</span>
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
              <SectionRemarksNotice :section-key="'20'" :remarks="remarksFor('20')" />
              <div class="section-card">
                <textarea v-model="proposal.limitations" placeholder="Describe project limitations..."
                  rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'20'" :field-key="'limitationsText'" field-label="Limitations of the Project" :remarks="remarksForField('limitationsText')" readonly />
              </div>
            </section>

            <!-- 21. Risks -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">21</span>
                <h3>Risk Management Plan</h3>
              </div>
              <SectionRemarksNotice :section-key="'21'" :remarks="remarksFor('21')" />
              <div class="section-card">
                <textarea v-model="proposal.risks_assumptions"
                  placeholder="List risks, assumptions, and mitigation strategies..." rows="4"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'21'" :field-key="'risks'" field-label="Risk Management Plan" :remarks="remarksForField('risks')" readonly />
              </div>
            </section>

            <!-- 22. Logical Framework -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">22</span>
                <h3>Logical Framework</h3>
                <span class="section-sub">Outcome and Output Indicators</span>
              </div>
              <SectionRemarksNotice :section-key="'22'" :remarks="remarksFor('22')" />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'22'" :field-key="`logFrames[${index}].outcome`" field-label="Outcome Indicator" :remarks="remarksForField(`logFrames[${index}].outcome`)" readonly />
                        <td><input v-model="row.output_indicator" placeholder="e.g. 5 filtration units installed" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'22'" :field-key="`logFrames[${index}].output`" field-label="Output Indicator" :remarks="remarksForField(`logFrames[${index}].output`)" readonly />
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
              <SectionRemarksNotice :section-key="'23'" :remarks="remarksFor('23')" />
              <div class="section-card">
                <textarea v-model="proposal.literature_cited" placeholder="List your references in APA format..."
                  rows="5"></textarea>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'23'" :field-key="'referencesText'" field-label="Literature Cited" :remarks="remarksForField('referencesText')" readonly />
              </div>
            </section>

            <!-- 24. Personnel -->
            <section class="form-section">
              <div class="section-header">
                <span class="section-number">24</span>
                <h3>Personnel Requirement</h3>
              </div>
              <SectionRemarksNotice :section-key="'24'" :remarks="remarksFor('24')" />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'24'" :field-key="`personnel[${index}].position`" field-label="Position" :remarks="remarksForField(`personnel[${index}].position`)" readonly />
                        <td><input v-model="row.effort" placeholder="e.g. 50%" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'24'" :field-key="`personnel[${index}].timeDevoted`" field-label="% Time Devoted" :remarks="remarksForField(`personnel[${index}].timeDevoted`)" readonly />
                        <td><input v-model="row.responsibilities" placeholder="e.g. Data collection" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'24'" :field-key="`personnel[${index}].responsibilities`" field-label="Responsibilities" :remarks="remarksForField(`personnel[${index}].responsibilities`)" readonly />
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
              <SectionRemarksNotice :section-key="'25'" :remarks="remarksFor('25')" />
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
                  <span>{{ proposal.line_item_budget_file.name }}</span>
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
              <SectionRemarksNotice :section-key="'26'" :remarks="remarksFor('26')" />
              <div class="section-card">
                <div class="form-group">
                  <label>Number of other projects:</label>
                  <input v-model="proposal.other_projects_number" placeholder="e.g. 2" class="input-short" />
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'26'" :field-key="'otherProjectsNumber'" field-label="Number of other projects" :remarks="remarksForField('otherProjectsNumber')" readonly />
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
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'26'" :field-key="`otherProjects[${index}].title`" field-label="Title" :remarks="remarksForField(`otherProjects[${index}].title`)" readonly />
                        <td><input v-model="row.funding_agency" placeholder="e.g. DOST" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'26'" :field-key="`otherProjects[${index}].agency`" field-label="Funding Agency" :remarks="remarksForField(`otherProjects[${index}].agency`)" readonly />
                        <td><input v-model="row.involvement" placeholder="e.g. Project Leader" /></td>
                  <FieldRemarkControl :proposal-id="proposalId" :section-key="'26'" :field-key="`otherProjects[${index}].involvement`" field-label="Involvement" :remarks="remarksForField(`otherProjects[${index}].involvement`)" readonly />
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
              <SectionRemarksNotice :section-key="'27'" :remarks="remarksFor('27')" />
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

          <div v-if="flaggedSections.length > 0" class="sidebar-card">
            <h3>Sections to Revise</h3>

            <ul class="flagged-list">
              <li v-for="section in flaggedSections" :key="section.key">
                <button type="button" class="flagged-item" @click="scrollToSection(section.key)">
                  <span class="flagged-number">{{ section.key }}</span>
                  <span class="flagged-label">{{ section.label }}</span>
                </button>
                <p v-for="(remark, index) in remarksFor(section.key)" :key="remark.id ?? index" class="flagged-remark">
                  {{ remark.remark }}
                  <span v-if="remark.reviewerRole" class="flagged-role">— {{ remark.reviewerRole }}</span>
                </p>
              </li>
            </ul>
          </div>

          <div v-if="previousRoundsList.length > 0" class="sidebar-card">
            <h3>Previous Revision Rounds</h3>
            <p class="history-note">
              Earlier feedback is kept for reference; only the current round needs to be addressed.
            </p>

            <div v-for="round in previousRoundsList" :key="round.round" class="history-round">
              <span class="history-round-title">Round {{ round.round }}</span>
              <ul class="history-list">
                <li v-for="(remark, index) in round.remarks" :key="remark.id ?? index">
                  <span class="history-section">
                    {{ remark.sectionKey }} · {{ remark.sectionLabel || sectionLabel(remark.sectionKey) }}
                  </span>
                  <p class="history-text">{{ remark.remark }}</p>
                  <span class="history-meta">
                    <template v-if="remark.reviewerRole">{{ remark.reviewerRole }}</template>
                    <template v-if="remark.createdAt"> · {{ formatRemarkDate(remark.createdAt) }}</template>
                  </span>
                </li>
              </ul>
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
              Save Draft
            </button>

            <button class="btn-action btn-submit" @click="submitRevision" :disabled="loading || isPastDeadline">
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
import SectionRemarksNotice from '@/components/SectionRemarksNotice.vue'
import FieldRemarkControl from '@/components/FieldRemarkControl.vue'
import CooperatingAgenciesChecklist from '@/components/CooperatingAgenciesChecklist.vue'
import { useDialog } from '@/composables/useDialog'
import { useUserDataStore } from '@/stores/userData'
import { SDG_OPTIONS, sdgToList } from '@/utils/sdg'
import { PROPOSAL_SECTIONS, activeRemarksBySection, previousRounds, sectionLabel } from '@/utils/proposalSections'
import { buildFieldRemarkIndex } from '@/utils/proposalFields'
import { joinAgencyNames, normalizeSelectedIds } from '@/utils/cooperatingAgencies'

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
const revisionDeadline = ref<string | null>(null)

// ===== Reviewer remarks per proposal section =====
// Remarks are loaded from the backend (GET /api/proposals/{id}/section-remarks) and
// rendered beside the section they belong to. They are stored in their own table, so
// editing and resubmitting this proposal never clears them.
const sectionRemarks = ref<any>(null)
const remarksLoading = ref(false)
const remarksError = ref('')
const remarksBySection = computed(() => activeRemarksBySection(sectionRemarks.value))
const remarksFor = (key: string) => remarksBySection.value[key] || []
/** Field-level remarks of the active round: { fieldKey: [remark, ...] }. */
const fieldRemarksByKey = computed(() => buildFieldRemarkIndex(sectionRemarks.value))
const remarksForField = (key: string) => fieldRemarksByKey.value[key] || []
const flaggedSections = computed(() =>
  PROPOSAL_SECTIONS.filter((section) => remarksFor(section.key).length > 0)
)
const previousRoundsList = computed(() => previousRounds(sectionRemarks.value))

const fetchSectionRemarks = async () => {
  if (!proposalId.value) return
  remarksLoading.value = true
  remarksError.value = ''
  try {
    // One request returns both the section-level and the field-level remarks.
    const res = await api.get(`/api/proposals/${proposalId.value}/field-remarks`)
    sectionRemarks.value = res.data || null
  } catch (err) {
    console.error(err)
    // A missing remark history must never block the revision itself.
    sectionRemarks.value = null
    remarksError.value = 'Unable to load the reviewer remarks for this proposal.'
  } finally {
    remarksLoading.value = false
  }
}

const scrollToSection = (key: string) => {
  const sections = Array.from(document.querySelectorAll('.form-section'))
  const match = sections.find(
    (section) => section.querySelector('.section-number')?.textContent?.trim() === key
  )
  if (match) match.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

const formatRemarkDate = (value: string | null) => {
  if (!value) return ''
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

const isPastDeadline = computed(() => {
  if (!revisionDeadline.value) return false
  const d = new Date(revisionDeadline.value)
  return !isNaN(d.getTime()) && d.getTime() < Date.now()
})

function formatDeadline(value: string | null) {
  if (!value) return '—'
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}

const deadlineStyle = computed(() => {
  const base = 'padding:10px 14px;border-radius:6px;margin-bottom:12px;font-size:14px;'
  if (isPastDeadline.value) {
    return base + 'background:#fde8e8;border-left:4px solid #dc2626;color:#991b1b;'
  }
  return base + 'background:#fffbeb;border-left:4px solid #f59e0b;color:#92400e;'
})



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
  // Section 02: the free text a proposal saved before the checklist carries, plus the
  // ids of the agencies selected from the shared directory and their names (used for
  // the legacy text column).
  cooperating_agencies: '',
  cooperating_agency_ids: [] as number[],
  cooperating_agency_names: [] as string[],
  research_type: '',
  innovation_goals: '',
  sector_relevance: '',
  sustainable_development_goals: [] as string[],
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
    dagat: { selected: false, value: '' },
    punla: { selected: false, value: '' },
    kalikasan: { selected: false, value: '' },
    negosyo: { selected: false, value: '' },
    tanglaw: { selected: false, value: '' }
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
    key: 'dagat',
    label: 'DAGAT Program',
    subLabel: 'Commodity'
  },
  {
    key: 'punla',
    label: 'PUNLA Program',
    subLabel: 'Priority Topic'
  },
  {
    key: 'kalikasan',
    label: 'KALIKASAN Program',
    subLabel: 'Sector'
  },
  {
    key: 'negosyo',
    label: 'NEGOSYO Program',
    subLabel: 'Sector'
  },
  {
    key: 'tanglaw',
    label: 'TANGLAW Program',
    subLabel: 'Sector'
  }
]

const removeSite = (index: number) => {
  proposal.value.sites.splice(index, 1)
}

const handleFileUpload = (type, event) => {
  const file = event.target.files[0]
  if (!file) return
  const map = {
    review_of_literature: 'review_of_literature_file',
    technology: 'technology_roadmap_file',
    gad: 'gad_score_file',
    budget: 'line_item_budget_file'
  }
  if (map[type]) proposal.value[map[type]] = file
}

const executiveWordCount = computed(() => {
  const text = (proposal.value.executive_summary || '').trim()
  return text ? text.split(/\s+/).length : 0
})

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
    revisionDeadline.value = data.revisionDeadline || null

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
      // Section 02: the free text of a proposal saved before the checklist is kept
      // (the checklist shows it until the agencies are selected); the saved selection
      // itself is restored by the checklist from the proposal's own endpoint.
      cooperating_agencies: data.cooperatingAgencies || '',
      cooperating_agency_ids: [] as number[],
      cooperating_agency_names: [] as string[],
      research_type: data.researchType || '',
      innovation_goals: data.innovationGoals || '',
      sector_relevance: data.sectorRelevance || '',
      sustainable_development_goals: sdgToList(data.sdg),
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
      review_of_literature: data.review || '',
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
            // The API returns the entity field name (timeDevoted), not "time".
            effort: p.timeDevoted || '',
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
        dagat: { selected: false, value: '' },
        punla: { selected: false, value: '' },
        kalikasan: { selected: false, value: '' },
        negosyo: { selected: false, value: '' },
        tanglaw: { selected: false, value: '' },
        ...(Array.isArray(data.priorityAgendas)
          ? Object.fromEntries(
            data.priorityAgendas.map(pa => [
              pa.agendaKey || pa.agenda,
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

  if (isPastDeadline.value) {
    error.value = 'The revision deadline has passed. Revision submission is no longer available.'
    return
  }

  if (executiveWordCount.value > 200) {
    error.value = 'Executive Summary must not exceed 200 words.'
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
    projectLeaderSex: data.project_leader_sex || '',
    duration: data.duration || '',
    startDate: data.start_date || '',
    endDate: data.end_date || '',
    college: data.department || '',
    address: data.address || '',
    // Section 02: the checklist selection travels as directory ids and the legacy text
    // column is derived from the selected names (the backend derives it from the ids
    // again, so the text and the relation can never drift apart).
    cooperatingAgencies: joinAgencyNames(data.cooperating_agency_names) || data.cooperating_agencies || '',
    cooperatingAgencyIds: normalizeSelectedIds(data.cooperating_agency_ids),
    reviewFileName: data.review_of_literature_file?.name || '',
    roadmapFileName: data.technology_roadmap_file?.name || '',
    gadFileName: data.gad_score_file?.name || '',
    beneficiariesFileName: data.line_item_budget_file?.name || '',
    researchType: data.research_type || '',
    innovationGoals: data.innovation_goals || '',
    sectorRelevance: data.sector_relevance || '',
    sdg: data.sustainable_development_goals || [],
    executiveSummary: data.executive_summary || '',
    rationale: data.rationale || '',
    framework: data.theoretical_framework || '',
    objectivesGeneral: data.general_objective || '',
    objectivesSpecific: data.specific_objectives || '',
    methodology: data.methodology || '',
    outputs: data.expected_outputs || '',
    review: data.review_of_literature || '',
    limitationsText: data.limitations || '',
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
  fetchSectionRemarks()
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
  /* Keep each field at its natural height so a remark on one field can never
     stretch its row partner and misalign the neighbouring column. */
  align-items: start;
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

.sdg-checkbox-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 6px 16px;
  margin-top: 6px;
}

.sdg-checkbox-list .sdg-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0;
  font-size: 14px;
  font-weight: 400;
  line-height: 1.4;
  color: #475569;
  cursor: pointer;
  padding: 6px 8px;
  border-radius: 6px;
  transition: background 0.15s;
}

.sdg-checkbox-list .sdg-checkbox:hover {
  background: #f8fafc;
}

.sdg-checkbox-list .sdg-checkbox input {
  width: 16px !important;
  height: 16px;
  margin: 0 !important;
  padding: 0 !important;
  border: none !important;
  border-radius: 0 !important;
  background: transparent;
  box-shadow: none !important;
  accent-color: #4f46e5;
  cursor: pointer;
  flex-shrink: 0;
  vertical-align: middle;
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

/* ===== SECTION REMARKS ===== */
.remarks-summary {
  background: #fffbeb;
  border: 1px solid #fcd34d;
  border-left: 4px solid #f59e0b;
  border-radius: 10px;
  padding: 12px 14px;
  margin-bottom: 16px;
}

.remarks-summary-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.remarks-summary-count {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f59e0b;
  color: #ffffff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.remarks-summary-head strong {
  font-size: 14px;
  color: #92400e;
}

.remarks-summary-sub {
  margin: 2px 0 0;
  font-size: 12px;
  color: #a16207;
}

.remarks-summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.remarks-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #ffffff;
  border: 1px solid #fcd34d;
  border-radius: 999px;
  padding: 4px 10px;
  font-family: inherit;
  font-size: 12px;
  color: #78350f;
  cursor: pointer;
}

.remarks-chip:hover {
  background: #fef3c7;
}

.remarks-chip-number {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-weight: 700;
  color: #b45309;
}

.remarks-chip-count {
  background: #f59e0b;
  color: #ffffff;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  padding: 1px 6px;
}

.remarks-error {
  color: #b45309;
  font-size: 13px;
  margin-bottom: 12px;
}

.flagged-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.flagged-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  text-align: left;
  font-family: inherit;
}

.flagged-number {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
  color: #b45309;
}

.flagged-label {
  font-size: 12px;
  font-weight: 600;
  color: #1f2937;
}

.flagged-item:hover .flagged-label {
  text-decoration: underline;
}

.flagged-remark {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.45;
  color: #4b5563;
  white-space: pre-wrap;
}

.flagged-role {
  font-weight: 700;
  color: #b45309;
}

.history-note {
  margin: 0 0 10px;
  font-size: 11px;
  color: #6b7280;
  line-height: 1.45;
}

.history-round + .history-round {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.history-round-title {
  display: inline-block;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  color: #6b7280;
  margin-bottom: 6px;
}

.history-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.history-section {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #4338ca;
}

.history-text {
  margin: 2px 0 0;
  font-size: 12px;
  line-height: 1.45;
  color: #4b5563;
  white-space: pre-wrap;
}

.history-meta {
  font-size: 10px;
  color: #9ca3af;
}

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
  font-weight: 700;
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
  /* A remark inside one field must not stretch the field beside it. */
  align-items: start;
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
  color: #1e293b;
  font-size: 13px;
  font-weight: 700;
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
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 14px;
}

.agenda-item {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 16px 18px;
  background: #fff;
  transition: border-color 0.2s, background 0.2s, box-shadow 0.2s;
}

.agenda-item:hover {
  border-color: #c7d2fe;
  background: #fafbff;
}

.agenda-item.selected {
  border-color: #4f46e5;
  background: #eef2ff;
  box-shadow: 0 0 0 1px #4f46e5;
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
  border-radius: 6px;
  background: #fff;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s, border-color 0.15s;
}

.checkmark::after {
  content: '';
  width: 5px;
  height: 10px;
  border: solid #fff;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg) scale(0);
  margin-top: -2px;
  transition: transform 0.15s;
}

.agenda-checkbox input:checked + .checkmark {
  background: #4f46e5;
  border-color: #4f46e5;
}

.agenda-checkbox input:checked + .checkmark::after {
  transform: rotate(45deg) scale(1);
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
  font-weight: 500;
}

.agenda-value {
  margin-top: 14px;
}

.agenda-value input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  background: #fff;
  font-size: 14px;
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