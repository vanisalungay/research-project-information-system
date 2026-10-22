<template>
  <div v-if="modelValue" class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <div class="header-left">
          <h2>Submit New Proposal</h2>
          <span class="header-badge">Step 2 of 4</span>
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
          <button class="btn-download" @click="showEvalCriteria = true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M9 11l3 3L22 4" />
              <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11" />
            </svg>
            Evaluation Criteria
          </button>
          <button class="close-btn" @click="close">✕</button>
        </div>
      </header>

      <!-- PROGRESS BAR -->
      <div class="progress-bar">
        <!-- Step 1 (the Call for Proposal) is already done: it can be reopened to
             select a different call. -->
        <div class="progress-step completed" @click="$emit('goToStep', 1)">
          <span class="step-number">✓</span>
          <span class="step-label">Call for Proposal</span>
        </div>
        <div class="progress-line filled"></div>
        <div class="progress-step active">
          <span class="step-number">2</span>
          <span class="step-label">Proposal Details</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step" @click="$emit('goToStep', 3)">
          <span class="step-number">3</span>
          <span class="step-label">Certification</span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-step" @click="$emit('goToStep', 4)">
          <span class="step-number">4</span>
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
        <!-- The call chosen in step 1. It stays visible so the proposal is always
             prepared for the right call, and it can be changed from here. -->
        <div v-if="selectedCallName" class="selected-call">
          <div class="selected-call-info">
            <span class="selected-call-label">Call for Proposal</span>
            <span class="selected-call-name">{{ selectedCallName }}</span>
            <span v-if="selectedCallDeadline" class="selected-call-deadline">
              Deadline: {{ selectedCallDeadline }}
            </span>
          </div>
          <button type="button" class="btn-change-call" @click="$emit('goToStep', 1)">
            Change Call
          </button>
        </div>

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
                <select v-model="programTitleChoice" :disabled="programTitlesLoading"
                  @change="onProgramTitleChoiceChange">
                  <option value="">Select a program title</option>
                  <option :value="CREATE_NEW_PROGRAM_TITLE">Create New Program Title</option>
                  <option v-for="title in existingProgramTitles" :key="title" :value="title">{{ title }}</option>
                </select>
                <input v-if="programTitleChoice === CREATE_NEW_PROGRAM_TITLE" class="program-title-new-input"
                  v-model="proposal.program_title" placeholder="e.g. Community Development Program" />
                <small v-if="programTitlesError" class="field-hint field-hint-error">{{ programTitlesError }}</small>
                <small v-else-if="programTitlesEmpty" class="field-hint">
                  No previously submitted program titles yet — choose "Create New Program Title" to add one.
                </small>
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
                <label>Project Duration (Months) <span class="req">*</span></label>
                <select v-model="proposal.duration">
                  <option value="12">12 months</option>
                  <option value="24">24 months</option>
                  <option value="36">36 months</option>
                </select>
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
              <label>Address <span class="req">*</span></label>
              <p v-if="proposal.legacy_address" class="field-note ac-note">
                Address saved in the previous format: &ldquo;{{ proposal.legacy_address }}&rdquo;. Please select the
                address parts below.
              </p>
              <div class="ac-grid">
                <div class="ac-field">
                  <label for="address-country">Country <span class="req">*</span></label>
                  <select id="address-country" :value="addressDetails.country"
                    @change="addressCascade.select('country', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.country }}</option>
                    <option v-for="country in addressCascade.countries" :key="country.code" :value="country.name">
                      {{ country.name }}
                    </option>
                  </select>
                </div>
                <div class="ac-field">
                  <label for="address-region">Region <span class="req">*</span></label>
                  <select v-if="!addressCascade.manual" id="address-region" :value="addressDetails.region"
                    :disabled="!addressCascade.regionEnabled"
                    @change="addressCascade.select('region', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.region }}</option>
                    <option v-for="region in addressCascade.regionOptions" :key="region.code" :value="region.name">
                      {{ regionLabel(region) }}
                    </option>
                  </select>
                  <input v-else id="address-region" type="text" :value="addressDetails.region"
                    placeholder="State / Region" @input="addressCascade.select('region', $event.target.value)" />
                </div>
                <div class="ac-field">
                  <!-- Province is only required where the region actually has
                       provinces (NCR has none, and an independent city has none
                       either - the placeholder explains it). -->
                  <label for="address-province">Province <span v-if="addressCascade.provinceEnabled"
                      class="req">*</span></label>
                  <select v-if="!addressCascade.manual" id="address-province" :value="addressDetails.province"
                    :disabled="!addressCascade.provinceEnabled"
                    @change="addressCascade.select('province', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.province }}</option>
                    <option v-for="province in addressCascade.provinceOptions" :key="province.code"
                      :value="province.name">{{ province.name }}</option>
                  </select>
                  <input v-else id="address-province" type="text" :value="addressDetails.province"
                    placeholder="Province / Administrative area"
                    @input="addressCascade.select('province', $event.target.value)" />
                </div>
                <div class="ac-field">
                  <!-- A district only exists in a districted area (NCR): the marker
                       follows the loaded hierarchy so it never claims a level that
                       does not apply to the selected region. -->
                  <label for="address-district">District <span v-if="addressCascade.districtEnabled"
                      class="req">*</span></label>
                  <select v-if="!addressCascade.manual" id="address-district" :value="addressDetails.district"
                    :disabled="!addressCascade.districtEnabled"
                    @change="addressCascade.select('district', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.district }}</option>
                    <option v-for="district in addressCascade.districtOptions" :key="district.code"
                      :value="district.name">{{ district.name }}</option>
                  </select>
                  <input v-else id="address-district" type="text" :value="addressDetails.district"
                    placeholder="District (if any)" @input="addressCascade.select('district', $event.target.value)" />
                </div>
                <div class="ac-field">
                  <label for="address-municipality">Municipality / City <span class="req">*</span></label>
                  <select v-if="!addressCascade.manual" id="address-municipality" :value="addressDetails.municipality"
                    :disabled="!addressCascade.cityEnabled"
                    @change="addressCascade.select('municipality', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.municipality }}</option>
                    <option v-for="city in addressCascade.cityOptions" :key="city.code" :value="city.name">
                      {{ cityLabel(city) }}
                    </option>
                  </select>
                  <input v-else id="address-municipality" type="text" :value="addressDetails.municipality"
                    placeholder="City / Municipality"
                    @input="addressCascade.select('municipality', $event.target.value)" />
                </div>
                <div class="ac-field">
                  <label for="address-barangay">Barangay <span class="req">*</span></label>
                  <select v-if="!addressCascade.manual && !addressCascade.manualBarangay" id="address-barangay"
                    :value="addressDetails.barangay" :disabled="!addressCascade.barangayEnabled"
                    @change="addressCascade.select('barangay', $event.target.value)">
                    <option value="">{{ addressCascade.placeholders.barangay }}</option>
                    <option v-for="barangay in addressCascade.barangayOptions" :key="barangay.code"
                      :value="barangay.name">{{ barangay.name }}</option>
                  </select>
                  <input v-else id="address-barangay" type="text" :value="addressDetails.barangay"
                    placeholder="Barangay" @input="addressCascade.select('barangay', $event.target.value)" />
                </div>
              </div>
              <p v-if="addressCascade.notice" class="field-note ac-note">
                {{ addressCascade.notice }}
                <button v-if="addressCascade.retryable" type="button" class="ac-retry"
                  @click="addressCascade.retry()">Retry</button>
              </p>
              <span class="field-error" v-if="showValidation && errors.address">{{ errors.address }}</span>
            </div>

            <div class="form-group" :class="{ 'has-error': showValidation && errors.contact }">
              <!-- The contact method dropdown is the field's own control: it sits
                   directly under the "Contact *" heading (no separate label) and
                   the value input appears below it once a method is selected. -->
              <label for="contact-type">Contact <span class="req">*</span></label>
              <select id="contact-type" :value="contactDetails.type"
                @change="onContactTypeChange($event.target.value)">
                <option value="">{{ CONTACT_METHOD_PLACEHOLDER }}</option>
                <option v-for="type in CONTACT_TYPES" :key="type.value" :value="type.value">{{ type.label }}</option>
              </select>
              <input v-if="contactDetails.type" id="contact-value" class="contact-value-input"
                v-bind="contactInputAttributes(contactDetails.type)" :value="contactDetails.value"
                :placeholder="contactInputPlaceholder" :aria-label="contactInputLabel" @input="onContactValueInput" />
              <span class="field-error" v-if="showValidation && errors.contact">{{ errors.contact }}</span>
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
              <label>Select all agencies that will collaborate on this research project <span class="req">*</span></label>
              <!-- The checklist is the shared backend directory: the agencies are
                   never typed in per proposal and a new one is only stored when the
                   proponent clicks Add Agency. -->
              <CooperatingAgenciesChecklist v-model:selected="proposal.cooperating_agency_ids"
                v-model:names="proposal.cooperating_agency_names" :proposal-id="proposal.proposal_id"
                :legacy-selection="proposal.cooperating_agencies" />
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
              <label>Select all applicable SDGs</label>
              <div class="sdg-checkbox-list">
                <label v-for="goal in SDG_OPTIONS" :key="goal" class="sdg-checkbox">
                  <input type="checkbox" :value="goal" v-model="proposal.sustainable_development_goals" />
                  <span>{{ goal }}</span>
                </label>
              </div>
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
            <span class="char-count">{{ proposal.executive_summary.length }}/200 words</span>
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
              <h4>Rationale / Significance</h4>
              <p class="field-note">Not to exceed 300 words</p>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.rationale }">
                <textarea v-model="proposal.rationale" rows="4"></textarea>
                <span class="field-error" v-if="showValidation && errors.rationale">{{ errors.rationale }}</span>
              </div>
            </div>
            <div class="sub-section">
              <h4>Scientific Basis / Theoretical Framework</h4>
              <div class="form-group" :class="{ 'has-error': showValidation && errors.theoretical_framework }">
                <textarea v-model="proposal.theoretical_framework" rows="4"></textarea>
                <span class="field-error" v-if="showValidation && errors.theoretical_framework">{{
                  errors.theoretical_framework }}</span>
              </div>
            </div>
            <div class="sub-section">
              <h4>Objectives</h4>
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
              <textarea v-model="proposal.review_of_literature" placeholder="Enter your review of literature..."
                rows="5"></textarea>
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
            <!-- The technology map is uploaded as an image: the file is validated as
                 soon as it is chosen, a preview shows which image will be attached,
                 and the image is uploaded to this proposal when the form is saved. -->
            <div class="file-upload technology-map" :class="{ 'has-error': Boolean(technologyMapError) }">
              <label for="technology-map-input">{{ TECHNOLOGY_MAP_LABEL }}</label>
              <p class="field-hint">{{ TECHNOLOGY_MAP_HINT }}</p>

              <div class="upload-area" @click="chooseTechnologyMap">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                  stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="17 8 12 3 7 8" />
                  <line x1="12" y1="3" x2="12" y2="15" />
                </svg>
                <span>{{ technologyMapFile ? 'Replace your technology map' : 'No image selected yet' }}</span>
                <button type="button" class="btn-choose-image" @click.stop="chooseTechnologyMap">
                  Choose Image
                </button>
              </div>
              <input id="technology-map-input" ref="techFile" type="file"
                accept="image/png,image/jpeg,.png,.jpg,.jpeg" @change="onTechnologyMapChange" hidden />

              <div v-if="technologyMapFile" class="file-info">
                <span class="file-name">🖼 {{ technologyMapFile.name }}</span>
                <span class="file-size">{{ technologyMapSize }}</span>
                <span class="file-actions">
                  <button type="button" class="btn-replace-file" @click="chooseTechnologyMap">
                    Replace Image
                  </button>
                  <button type="button" class="btn-remove-file" @click="removeTechnologyMap">
                    Remove Image
                  </button>
                </span>
              </div>

              <figure v-if="technologyMapPreviewUrl" class="technology-map-preview">
                <img :src="technologyMapPreviewUrl"
                  :alt="`Preview of ${technologyMapFile?.name || 'the technology map'}`" />
              </figure>

              <span class="field-error" v-if="technologyMapError">{{ technologyMapError }}</span>
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
          <div class="section-card" :class="{ 'has-error-card': showValidation && errors.literature_cited }">
            <p class="reference-instruction">{{ REFERENCE_INSTRUCTION }}</p>

            <!-- The references added so far. Each entry keeps its APA formatting
                 (italics included) and can be edited or removed. -->
            <ul v-if="references.length" class="reference-list">
              <li v-for="(entry, index) in references" :key="entry.id" class="reference-entry">
                <div class="reference-entry-main">
                  <span class="reference-badge">{{ entryTypeLabel(entry) }}</span>
                  <span class="reference-text" v-html="entryHtml(entry)"></span>
                </div>
                <div class="reference-actions">
                  <button type="button" class="btn-replace-file" @click="editReference(index)">
                    Edit
                  </button>
                  <button type="button" class="btn-remove-file" @click="removeReference(index)">
                    Remove
                  </button>
                </div>
              </li>
            </ul>
            <p v-else class="field-note">No references added yet.</p>

            <button type="button" class="btn-add" @click="openReferenceModal">
              {{ ADD_REFERENCE_LABEL }}
            </button>

            <span class="field-error" v-if="showValidation && errors.literature_cited">{{
              errors.literature_cited }}</span>
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
            <!-- The official template must be followed, and it can be read in place
                 (View Line-Item Budget Template) instead of being downloaded first. -->
            <p class="budget-template-instruction">
              <strong>Instruction:</strong> {{ LINE_ITEM_BUDGET_INSTRUCTION }}
            </p>
            <ul class="budget-file-rules">
              <li>
                <strong>{{ BUDGET_FILE_FORMATS_LABEL }}</strong> {{ BUDGET_FILE_FORMATS_VALUE }}
              </li>
              <li><strong>{{ BUDGET_FILE_SIZE_LABEL }}</strong> {{ BUDGET_FILE_SIZE_VALUE }}</li>
            </ul>
            <!-- The two template actions share one row; the upload control sits on
                 its own row directly below them (see .budget-upload). -->
            <div class="budget-actions">
              <button class="btn-outline" @click="downloadBudgetFormat">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                  <polyline points="7 10 12 15 17 10" />
                  <line x1="12" y1="15" x2="12" y2="3" />
                </svg>
                Download Budget Template
              </button>
              <button class="btn-outline btn-view-template" @click="showBudgetTemplate = true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
                {{ LINE_ITEM_BUDGET_TEMPLATE_BUTTON_LABEL }}
              </button>
            </div>
            <div class="budget-upload">
              <!-- The filled-out budget is validated as soon as it is chosen, so an
                   unsupported or oversized file never reaches the form; the file is
                   uploaded to this proposal when the form is saved or submitted. -->
              <div class="file-upload budget-file" :class="{ 'has-error': Boolean(budgetFileError) }">
                <div class="upload-area small" @click="chooseBudgetFile">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                    <polyline points="14 2 14 8 20 8" />
                    <line x1="12" y1="18" x2="12" y2="12" />
                    <line x1="9" y1="15" x2="15" y2="15" />
                  </svg>
                  <span>{{ BUDGET_FILE_LABEL }}</span>
                </div>
                <input ref="budgetFileInput" type="file" accept=".pdf,.docx"
                  @change="onBudgetFileChange" hidden />
              </div>
              <div v-if="proposal.line_item_budget_file" class="file-info">
                <span class="file-name">📄 {{ proposal.line_item_budget_file.name }}</span>
                <span class="file-size">{{ budgetFileSize }}</span>
                <span class="file-actions">
                  <button type="button" class="btn-replace-file" @click="chooseBudgetFile">
                    Replace File
                  </button>
                  <button type="button" class="btn-remove-file" @click="removeBudgetFile">
                    Remove File
                  </button>
                </span>
              </div>
              <span class="field-error" v-if="budgetFileError">{{ budgetFileError }}</span>
              <span class="field-error" v-else-if="showValidation && errors.line_item_budget_file">{{
                errors.line_item_budget_file }}</span>
            </div>
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
              <input
                v-model="proposal.other_projects_number"
                type="number"
                min="0"
                step="1"
                inputmode="numeric"
                placeholder="e.g. 2"
                class="input-short"
                @input="onOtherProjectsNumberInput"
              />
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

  <div v-if="showEvalCriteria" class="eval-criteria-backdrop" @click.self="showEvalCriteria = false">
    <div class="modal-container eval-criteria-modal">
      <header class="modal-header">
        <div class="header-left">
          <h2>Evaluation Criteria</h2>
        </div>
        <div class="header-actions">
          <button class="close-btn" @click="showEvalCriteria = false">✕</button>
        </div>
      </header>
      <div class="modal-body">
        <ProponentEvalCriteria />
      </div>
    </div>
  </div>

  <!-- Official Line-Item Budget template (section 25). It is shown in place so
       the proponent can follow its format without downloading the file; the
       budget upload field and the rest of the form are untouched. -->
  <LineItemBudgetTemplateModal v-if="showBudgetTemplate" @close="showBudgetTemplate = false" />

  <!-- Reference builder for section 23 (Literature Cited): generate an APA 7th
       edition reference or paste one that is already formatted. -->
  <LiteratureReferenceModal v-if="showReferenceModal" :entry="editingReference" @save="saveReference"
    @close="closeReferenceModal" />

  <ConfirmDialog v-if="dialogState.show" v-bind="dialogState" @confirm="dialogState.onConfirm"
    @cancel="dialogState.onCancel" @close="dialogState.show = false" />
</template>

<script setup>
import { reactive, ref, computed, nextTick, watch, onMounted, onBeforeUnmount } from 'vue'
import api from '@/utils/api'
import { useUserDataStore } from '@/stores/userData'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import CooperatingAgenciesChecklist from '@/components/CooperatingAgenciesChecklist.vue'
import LineItemBudgetTemplateModal from '@/components/LineItemBudgetTemplateModal.vue'
import LiteratureReferenceModal from '@/components/LiteratureReferenceModal.vue'
import ProponentEvalCriteria from '@/views/Proponent/ProponentEvalCriteria.vue'
import { useDialog } from '@/composables/useDialog'
import { downloadOfficialProposal } from '@/utils/officialProposalForm'
import {
  buildStandaloneHtml,
  printDocument,
  escapeHtml,
  field,
  textBlock,
  fileField,
  tableBlock,
  section,
  sdgChecklistHtml
} from '@/utils/documentExport'
import { SDG_OPTIONS, sdgToList } from '@/utils/sdg'
import { joinAgencyNames, normalizeSelectedIds } from '@/utils/cooperatingAgencies'
import { useAddressCascade } from '@/composables/useAddressCascade'
import {
  cityLabel,
  emptyAddressDetails,
  formatAddressContact,
  regionLabel,
  validateAddress,
} from '@/utils/philippineLocations'
import {
  CONTACT_METHOD_PLACEHOLDER,
  CONTACT_NUMBER,
  CONTACT_TYPES,
  contactDetailsForType,
  contactInputAttributes,
  contactPlaceholder,
  contactTypeLabel,
  emptyContactDetails,
  sanitizeContactNumber,
  validateContact,
} from '@/utils/contactDetails'
import {
  TECHNOLOGY_MAP_HINT,
  TECHNOLOGY_MAP_LABEL,
  clearedTechnologyMapSelection,
  formatFileSize,
  nextTechnologyMapSelection,
  technologyMapKey,
  technologyMapUploadError,
  uploadTechnologyMap,
  verifyTechnologyMapFile,
} from '@/utils/technologyMap'
import {
  LINE_ITEM_BUDGET_INSTRUCTION,
  LINE_ITEM_BUDGET_TEMPLATE_BUTTON_LABEL,
  LINE_ITEM_BUDGET_TEMPLATE_URL,
} from '@/utils/budgetTemplate'
import {
  BUDGET_FILE_FORMATS_LABEL,
  BUDGET_FILE_FORMATS_VALUE,
  BUDGET_FILE_LABEL,
  BUDGET_FILE_SIZE_LABEL,
  BUDGET_FILE_SIZE_VALUE,
  budgetFileKey,
  budgetFileSizeLabel,
  budgetFileUploadError,
  clearedBudgetFileSelection,
  nextBudgetFileSelection,
  uploadBudgetFile,
  verifyBudgetFile,
} from '@/utils/budgetFile'
// Step 1 of the workflow: the selected Call for Proposal is shown on this step.
import { callDeadlineLabel } from '@/utils/callForProposal'
// Section 23: APA 7th edition reference builder.
import {
  ADD_REFERENCE_LABEL,
  REFERENCE_INSTRUCTION,
  entriesToJson,
  entriesToText,
  entryHtml,
  entryText,
  entryTypeLabel,
  normalizeReferenceText,
  parseReferenceEntries,
} from '@/utils/apaReferences'

const { dialogState, showAlert, showConfirm } = useDialog()

const props = defineProps({
  modelValue: Boolean,
  /**
   * The Call for Proposal selected in step 1 of the workflow. The real record (with
   * its database id) is kept by the workflow, shown here and sent with the proposal.
   */
  selectedCall: { type: Object, default: null }
})
const emit = defineEmits(['update:modelValue', 'save', 'next'])

const modalBodyRef = ref(null)
const errorBannerRef = ref(null)
const showValidation = ref(false)
const showEvalCriteria = ref(false)
const showBudgetTemplate = ref(false)
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
  contact: 'section_project_profile',
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

// Selectable project durations (months) for internally funded proposals.
const DURATION_OPTIONS = ['12', '24', '36']

// ===== PROGRAM TITLE (reuse an existing title or create a new one) =====
// A Proponent may pick a program title from their own previously submitted
// proposals or create a brand new one. This sentinel distinguishes the
// "create new" choice from the empty placeholder option in the dropdown.
const CREATE_NEW_PROGRAM_TITLE = '__CREATE_NEW_PROGRAM_TITLE__'
const programTitleChoice = ref('')
const existingProgramTitles = ref([])
const programTitlesLoading = ref(false)
const programTitlesError = ref('')

// Loads the distinct program titles the proponent is allowed to reuse. This reuses
// the existing proposal-list endpoint (which already returns only the caller's own
// proposals) instead of a dedicated backend endpoint, so no backend change is
// required. Failures are surfaced inline and never block the form: the proponent
// can always fall back to "Create New Program Title".
const fetchExistingProgramTitles = async () => {
  programTitlesLoading.value = true
  programTitlesError.value = ''
  try {
    const res = await api.get('/api/proposals')
    const proposals = Array.isArray(res.data) ? res.data : []
    const titles = proposals
      .map((p) => (p && typeof p.programTitle === 'string' ? p.programTitle.trim() : ''))
      .filter((title) => title !== '')
    // De-duplicate the trimmed titles so each distinct value appears only once.
    existingProgramTitles.value = [...new Set(titles)].sort((a, b) => a.localeCompare(b))
  } catch (err) {
    // A genuine API failure must never be reported as "no titles exist".
    console.error(
      'Failed to load existing program titles:',
      err?.response?.status,
      err?.response?.data || err?.message
    )
    existingProgramTitles.value = []
    programTitlesError.value = 'Unable to load existing program titles. You can still create a new one.'
  } finally {
    programTitlesLoading.value = false
  }
}

// True when the request succeeded but the proponent simply has no reusable titles
// yet. This is an empty state, not an error.
const programTitlesEmpty = computed(
  () => !programTitlesLoading.value && !programTitlesError.value && existingProgramTitles.value.length === 0
)

// Keeps the submitted value (proposal.program_title) in sync with the dropdown.
// Switching to "Create New Program Title" clears any previously selected existing
// title so a stale value can never be submitted.
const onProgramTitleChoiceChange = () => {
  if (programTitleChoice.value === CREATE_NEW_PROGRAM_TITLE) {
    proposal.program_title = ''
  } else {
    proposal.program_title = programTitleChoice.value || ''
  }
}

// Reflects an already-saved program title (draft/revision) back into the dropdown
// without losing the value when it is not (yet) part of the loaded list.
const applyProgramTitleSelection = (title) => {
  const value = typeof title === 'string' ? title.trim() : ''
  if (!value) {
    programTitleChoice.value = ''
  } else if (existingProgramTitles.value.includes(value)) {
    programTitleChoice.value = value
  } else {
    programTitleChoice.value = CREATE_NEW_PROGRAM_TITLE
  }
  proposal.program_title = value
}

const getInitialProposal = () => ({
  proposal_id: null,
  program_title: '',
  project_title: '',
  project_leader: '',
  project_leader_sex: '',
  duration: '12',
  start_date: '',
  end_date: '',
  department: '',
  address_details: emptyAddressDetails(),
  contact_details: emptyContactDetails(),
  legacy_address: '',
  other_projects_number: '',
  // Section 02: the free text a proposal saved before the checklist still carries,
  // plus the ids of the agencies selected from the shared directory and the names
  // they map to (used for the legacy text column and the printout).
  cooperating_agencies: '',
  cooperating_agency_ids: [],
  cooperating_agency_names: [],
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
  sustainable_development_goals: [],
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
  // Name / stored path of the technology map already attached to the proposal (set by
  // the upload, so the printout and a later save keep describing the same image).
  roadmap_file_name: '',
  roadmap_file_path: '',
  gad_score_file: null,
  line_item_budget_file: null,
  // Name / stored path of the Line-Item Budget already attached to the proposal (set
  // by the upload, so a later save keeps describing the same file).
  budget_file_name: '',
  budget_file_path: '',
  // Step 1 of the workflow: the Call for Proposal this proposal is being prepared
  // for. The id is the real announcement id the backend stores on the proposal.
  proposal_announcement_id: null,
  proposal_announcement_name: '',
  // Section 23: the structured reference entries behind the literature cited list.
  literature_json: '',
  supporting_documents: [],
  logical_framework: [{ outcome_indicator: '', output_indicator: '' }],
  literature_cited: '',
  personnel_requirements: [{ position: '', effort: '', responsibilities: '' }],
  other_projects: [{ project_title: '', funding_agency: '', involvement: '' }]
})

const proposal = reactive(getInitialProposal())

// The selected Call for Proposal is part of the proposal state, so it travels with
// the form data through every step (and into the saved / submitted payload) without
// changing any existing field of this form.
watch(
  () => props.selectedCall,
  (call) => {
    proposal.proposal_announcement_id = call?.id ?? null
    proposal.proposal_announcement_name = call?.name || ''
  },
  { immediate: true }
)

/** The selected call's title, shown in the banner above the form. */
const selectedCallName = computed(
  () => props.selectedCall?.name || proposal.proposal_announcement_name || ''
)

/** The selected call's real saved deadline, shown in the same banner. */
const selectedCallDeadline = computed(() =>
  props.selectedCall ? callDeadlineLabel(props.selectedCall) : ''
)

// ===== LITERATURE CITED (section 23) =====
// The references live as a list of entries (what the builder produces). The list is
// mirrored into the proposal state twice: as plain text - the canonical value the API
// stores and every other screen shows - and as the structured entries, so italics and
// "Edit" still work after a save, a step change or a reload.
const references = ref([])
const showReferenceModal = ref(false)
const editingReference = ref(null)
const editingReferenceIndex = ref(-1)

/** Keeps the proposal state in sync with the entry list. */
const syncReferences = () => {
  proposal.literature_cited = entriesToText(references.value)
  proposal.literature_json = entriesToJson(references.value)
}

const openReferenceModal = () => {
  editingReference.value = null
  editingReferenceIndex.value = -1
  showReferenceModal.value = true
}

const editReference = (index) => {
  editingReference.value = references.value[index] || null
  editingReferenceIndex.value = index
  showReferenceModal.value = true
}

const closeReferenceModal = () => {
  showReferenceModal.value = false
  editingReference.value = null
  editingReferenceIndex.value = -1
}

/** Adds (or updates) a reference; an identical one is never added twice. */
const saveReference = async (entry) => {
  const text = entryText(entry)
  if (!text) return

  const isDuplicate = references.value.some(
    (existing, index) =>
      index !== editingReferenceIndex.value &&
      normalizeReferenceText(entryText(existing)) === normalizeReferenceText(text),
  )
  if (isDuplicate) {
    await showAlert('That reference is already in your literature cited list.', {
      type: 'warning',
      title: 'Duplicate Reference'
    })
    return
  }

  if (editingReferenceIndex.value >= 0) {
    references.value.splice(editingReferenceIndex.value, 1, entry)
  } else {
    references.value.push(entry)
  }
  syncReferences()
  closeReferenceModal()
}

/** Removes a reference, after the proponent confirms it. */
const removeReference = async (index) => {
  const entry = references.value[index]
  if (!entry) return
  const confirmed = await showConfirm('Remove this reference from the literature cited?', {
    type: 'warning',
    title: 'Remove Reference',
    confirmText: 'Remove'
  })
  if (!confirmed) return
  references.value.splice(index, 1)
  syncReferences()
}

// "Address *" and "Contact *" replace the old combined "Address / Contact"
// field. The address is a cascading Philippine location selection (with manual
// entry for any other country) and the contact is a type + value pair; both are
// kept in the proposal state so they travel with the rest of the form.
const addressDetails = computed({
  get: () => proposal.address_details,
  set: (value) => { proposal.address_details = value }
})
const contactDetails = computed({
  get: () => proposal.contact_details,
  set: (value) => { proposal.contact_details = value }
})
const addressCascade = useAddressCascade(addressDetails)

/** Accessible name of the value input (its visible label is the heading above). */
const contactInputLabel = computed(() => contactTypeLabel(contactDetails.value.type))
const contactInputPlaceholder = computed(() => contactPlaceholder(contactDetails.value.type))
const contactIsNumber = computed(() => contactDetails.value.type === CONTACT_NUMBER)

// A new contact method starts empty: a phone number is not an email address, and
// the input type and validation change with the method, so the value typed for
// the previous one is dropped instead of being submitted as the wrong contact.
const onContactTypeChange = (type) => {
  contactDetails.value = contactDetailsForType(type)
}

// Contact numbers only accept characters a number can contain (letters and the
// like are dropped as they are typed); email addresses are validated instead.
const onContactValueInput = (event) => {
  const raw = event.target.value
  const value = contactIsNumber.value ? sanitizeContactNumber(raw) : raw
  contactDetails.value = { ...contactDetails.value, value }
  if (value !== raw) event.target.value = value
}

/**
 * Maps the address / contact saved on a proposal back into the form.
 *
 * Proposals saved before the split only carry the combined `address` text. That
 * text is never dropped: it is shown above the new fields (proposal.legacy_address)
 * so the proponent can transfer it, while the structured columns simply start
 * empty.
 */
const applySavedAddressAndContact = (data) => {
  const saved = {
    country: data.addressCountry || '',
    region: data.addressRegion || '',
    province: data.addressProvince || '',
    district: data.addressDistrict || '',
    municipality: data.addressMunicipality || '',
    barangay: data.addressBarangay || ''
  }
  const hasStructured = Object.values(saved).some((value) => String(value ?? '').trim() !== '')
  proposal.address_details = hasStructured ? saved : emptyAddressDetails()
  proposal.legacy_address = hasStructured ? '' : (data.address || '')
  proposal.contact_details = { type: data.contactType || '', value: data.contactValue || '' }
}

const resetForm = () => {
  const initial = getInitialProposal()
  Object.keys(initial).forEach(key => {
    proposal[key] = initial[key]
  })
  showValidation.value = false
  Object.keys(errors).forEach(key => delete errors[key])
  programTitleChoice.value = ''
  budgetFileError.value = ''
  uploadedBudgetFileKey.value = ''
  // Section 23: the reference list is part of the form state that is reset.
  references.value = []
  showReferenceModal.value = false
  editingReference.value = null
  editingReferenceIndex.value = -1
}

const loadProposal = (data) => {
  // Map backend DTO fields to form fields
  proposal.proposal_id = data.id || null
  applyProgramTitleSelection(data.programTitle)
  proposal.project_title = data.projectTitle || ''
  proposal.project_leader = data.projectLeader || ''
  proposal.project_leader_sex = data.projectLeaderSex || ''
  // Preserve the proposal's saved duration (12, 24 or 36 months).
  proposal.duration = DURATION_OPTIONS.includes(String(data.duration)) ? String(data.duration) : '12'
  proposal.start_date = data.startDate || ''
  proposal.end_date = data.endDate || ''
  proposal.department = data.college || ''
  applySavedAddressAndContact(data)
  // Section 02: the free text of a proposal saved before the checklist is kept (the
  // checklist shows it until the agencies are selected); the saved selection itself
  // is restored by the checklist from the proposal's own endpoint.
  proposal.cooperating_agencies = data.cooperatingAgencies || ''
  proposal.cooperating_agency_ids = []
  proposal.cooperating_agency_names = []
  proposal.research_type = data.researchType || ''
  proposal.innovation_goals = data.innovationGoals || ''
  proposal.sector_relevance = data.sectorRelevance || ''
  proposal.sustainable_development_goals = sdgToList(data.sdg)
  proposal.executive_summary = data.executiveSummary || ''
  proposal.rationale = data.rationale || ''
  proposal.theoretical_framework = data.framework || ''
  proposal.general_objective = data.objectivesGeneral || ''
  proposal.specific_objectives = data.objectivesSpecific || ''
  proposal.methodology = data.methodology || ''
  proposal.expected_outputs = parseSixPs(data.outputs)
  proposal.review_of_literature = data.review || ''
  proposal.limitations = data.limitationsText || ''
  proposal.potential_outcomes = data.outcomes || ''
  proposal.economic_impact = data.impactEconomic || ''
  proposal.social_ethical_impact = data.impactSocial || ''
  proposal.target_beneficiaries = data.beneficiaries || ''
  proposal.sustainability_plan = data.sustainability || ''
  proposal.risks_assumptions = data.risks || ''
  proposal.other_projects_number = data.otherProjectsNumber || ''
  // Section 23: the references are restored from the stored plain text and, when the
  // proposal was built with the reference builder, from the structured entries too
  // (which is what keeps the italics and "Edit" working).
  proposal.literature_cited = data.referencesText || ''
  proposal.literature_json = data.referencesJson || ''
  references.value = parseReferenceEntries(proposal.literature_json, proposal.literature_cited)
  // Section 25: the name / path of the Line-Item Budget already attached to this
  // proposal. Only the reference is restored - the file itself stays on the server
  // until the proponent uploads a replacement.
  proposal.budget_file_name = data.beneficiariesFileName || ''
  proposal.budget_file_path = data.beneficiariesFilePath || ''

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
  if (data.logFrames && data.logFrames.length > 0) {
    proposal.logical_framework = data.logFrames.map(lf => ({
      outcome_indicator: lf.outcome || '',
      output_indicator: lf.output || ''
    }))
  }

  // Map personnel
  if (data.personnel && data.personnel.length > 0) {
    proposal.personnel_requirements = data.personnel.map(p => ({
      position: p.position || '',
      // The API returns the entity field name (timeDevoted), not "time".
      effort: p.timeDevoted || '',
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

// "Number of other projects" is a count. A type="number" input already refuses
// letters, but browsers still accept "e", "+", "-" and "." in one, so anything
// that is not a digit is dropped here: the form state - and therefore the
// submitted payload - always holds a whole, non-negative number, or is empty.
const onOtherProjectsNumberInput = (event) => {
  const digits = String(event.target.value || '').replace(/[^\d]/g, '')
  event.target.value = digits
  proposal.other_projects_number = digits
}

const handleFileUpload = (type, event) => {
  const file = event.target.files[0]
  if (!file) return
  // The Line-Item Budget (section 25) and the Technology Map (section 13) have
  // their own validated upload handlers - onBudgetFileChange / onTechnologyMapChange -
  // so this generic one is left for the remaining fields.
  const map = {
    gad: 'gad_score_file'
  }
  if (map[type]) proposal[map[type]] = file
  else if (type === 'supporting') proposal.supporting_documents.push(file)
}

// ===== TECHNOLOGY MAP (section 13) =====
// The technology map is uploaded as an image (PNG or JPEG, at most 10 MB). The file
// is validated the moment it is chosen - size, type and the real image signature - so
// an unsupported file never reaches the form, a preview lets the proponent verify the
// right image, and the image is uploaded to the proposal right after the proposal has
// been saved, so it is attached to the correct record.
const techFile = ref(null)
const technologyMapError = ref('')
const technologyMapPreviewUrl = ref('')
const uploadedTechnologyMapKey = ref('')

/** The image chosen on this form (a saved file name is not a file). */
const technologyMapFile = computed(() => {
  const file = proposal.technology_roadmap_file
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

/** The chosen file's size, shown next to its name. */
const technologyMapSize = computed(() => formatFileSize(technologyMapFile.value?.size))

const releaseTechnologyMapPreview = () => {
  if (technologyMapPreviewUrl.value) {
    URL.revokeObjectURL(technologyMapPreviewUrl.value)
    technologyMapPreviewUrl.value = ''
  }
}

/** Previews the chosen image so the proponent can verify the correct file. */
const showTechnologyMapPreview = (file) => {
  releaseTechnologyMapPreview()
  technologyMapPreviewUrl.value = URL.createObjectURL(file)
}

const chooseTechnologyMap = () => techFile.value?.click()

const onTechnologyMapChange = async (event) => {
  const input = event.target
  const file = input?.files?.[0]
  // Clear the input so choosing the same file again still fires a change event.
  if (input) input.value = ''
  if (!file) return

  const result = await verifyTechnologyMapFile(file)
  // A valid image replaces the previous selection and clears the error; an invalid
  // one is never accepted, so the previous selection stays and only the reason is
  // reported under the field.
  const next = nextTechnologyMapSelection(technologyMapFile.value, result, file)
  technologyMapError.value = next.error
  if (next.file !== file) return

  proposal.technology_roadmap_file = file
  uploadedTechnologyMapKey.value = ''
  showTechnologyMapPreview(file)
}

const removeTechnologyMap = () => {
  const next = clearedTechnologyMapSelection()
  proposal.technology_roadmap_file = next.file
  uploadedTechnologyMapKey.value = ''
  technologyMapError.value = next.error
  releaseTechnologyMapPreview()
}

/**
 * Uploads the chosen image to the proposal it belongs to - once. The proposal has
 * just been created or updated, so its id is known here.
 */
const uploadTechnologyMapIfNeeded = async (proposalId) => {
  const file = technologyMapFile.value
  if (!proposalId || !file) return
  const key = technologyMapKey(file)
  if (key === uploadedTechnologyMapKey.value) return

  try {
    const saved = await uploadTechnologyMap(proposalId, file)
    uploadedTechnologyMapKey.value = key
    // Keep the saved name / path, so the printout and a later save still describe the
    // image that is attached to the proposal.
    proposal.roadmap_file_name = saved?.roadmapFileName || ''
    proposal.roadmap_file_path = saved?.roadmapFilePath || ''
  } catch (err) {
    // The proposal itself was saved; only the image could not be attached, so the
    // failure is reported without hiding the successful save.
    await showAlert(technologyMapUploadError(err), {
      type: 'error',
      title: 'Technology Map Not Uploaded'
    })
  }
}

onBeforeUnmount(releaseTechnologyMapPreview)

// ===== LINE-ITEM BUDGET FILE (section 25) =====
// The proponent uploads the filled-out line-item budget instead of entering the
// budget details into the form. The file is validated the moment it is chosen -
// size, format and the real file signature - so an unsupported file never reaches
// the form, and it is uploaded to the proposal right after the proposal has been
// saved, so it is attached to the correct record.
const budgetFileInput = ref(null)
const budgetFileError = ref('')
const uploadedBudgetFileKey = ref('')

/** The chosen file, if any (a saved file name is not a file). */
const budgetFileToUpload = computed(() => {
  const file = proposal.line_item_budget_file
  return file && typeof file.slice === 'function' && typeof file.size === 'number' ? file : null
})

/** The chosen file's size, shown next to its name. */
const budgetFileSize = computed(() => budgetFileSizeLabel(budgetFileToUpload.value?.size))

const chooseBudgetFile = () => budgetFileInput.value?.click()

const onBudgetFileChange = async (event) => {
  const input = event.target
  const file = input?.files?.[0]
  // Clear the input so choosing the same file again still fires a change event.
  if (input) input.value = ''
  if (!file) return

  const result = await verifyBudgetFile(file)
  // A valid file replaces the previous selection and clears the error; an invalid
  // one is never accepted, so the previous selection stays and only the reason is
  // reported under the field.
  const next = nextBudgetFileSelection(proposal.line_item_budget_file, result, file)
  proposal.line_item_budget_file = next.file
  budgetFileError.value = next.error
  uploadedBudgetFileKey.value = ''
}

const removeBudgetFile = () => {
  const next = clearedBudgetFileSelection()
  proposal.line_item_budget_file = next.file
  budgetFileError.value = next.error
  uploadedBudgetFileKey.value = ''
  if (budgetFileInput.value) budgetFileInput.value.value = ''
}

/**
 * Uploads the chosen file to the proposal it belongs to - once. The proposal has
 * just been created or updated, so its id is known here.
 */
const uploadBudgetFileIfNeeded = async (proposalId) => {
  const file = budgetFileToUpload.value
  if (!proposalId || !file) return
  const key = budgetFileKey(file)
  if (key === uploadedBudgetFileKey.value) return

  try {
    const saved = await uploadBudgetFile(proposalId, file)
    uploadedBudgetFileKey.value = key
    // Keep the stored name / path, so the printout and a later save still describe
    // the file that is attached to the proposal.
    proposal.budget_file_name = saved?.beneficiariesFileName || ''
    proposal.budget_file_path = saved?.beneficiariesFilePath || ''
  } catch (err) {
    // The proposal itself was saved; only the file could not be attached, so the
    // failure is reported without hiding the successful save.
    await showAlert(budgetFileUploadError(err), {
      type: 'error',
      title: 'Budget File Not Uploaded'
    })
  }
}

/**
 * This page holds its own draft (snake_case) shape, so map it onto the field
 * names the official form renderer expects. Nothing is invented: any key with
 * no draft counterpart is simply left blank on the form.
 */
const toOfficialProposal = () => {
  const d = proposal
  const fileName = (file) => (file && file.name ? file.name : file ? String(file) : '')
  const agendas = Object.entries(d.priority_agendas || {}).map(([agendaKey, v]) => ({
    agendaKey,
    selected: Boolean(v && v.selected),
    value: (v && v.value) || ''
  }))
  const outputs = Object.values(d.expected_outputs || {})
    .map((o) => (o && o.value ? o.value : ''))
    .filter(Boolean)
    .join('\n')
  return {
    programTitle: d.program_title,
    projectTitle: d.project_title,
    projectLeader: d.project_leader,
    projectLeaderSex: d.project_leader_sex,
    duration: d.duration,
    startDate: d.start_date,
    endDate: d.end_date,
    college: d.department,
    // The official form still has a single "Address / Contact" line: it is fed
    // the combined value of the two new sections.
    address: formatAddressContact(d.address_details, d.contact_details),
    cooperatingAgencies: joinAgencyNames(d.cooperating_agency_names) || d.cooperating_agencies,
    sites: d.sites,
    researchType: d.research_type,
    priorityAgendas: agendas,
    innovationGoals: d.innovation_goals,
    sectorRelevance: d.sector_relevance,
    sdg: d.sustainable_development_goals,
    executiveSummary: d.executive_summary,
    rationale: d.rationale,
    framework: d.theoretical_framework,
    objectivesGeneral: d.general_objective,
    objectivesSpecific: d.specific_objectives,
    review: d.review_of_literature,
    methodology: d.methodology,
    outputs,
    outcomes: d.potential_outcomes,
    impactEconomic: d.economic_impact,
    impactSocial: d.social_ethical_impact,
    beneficiaries: d.target_beneficiaries,
    sustainability: d.sustainability_plan,
    limitationsText: d.limitations,
    risks: d.risks_assumptions,
    referencesText: d.literature_cited || '',
    // Section 23: the structured entries behind the list (italics + editing). Sent
    // together with the plain text; the backend stores both.
    referencesJson: d.literature_json || '',
    logFrames: (d.logical_framework || []).map((r) => ({
      outcome: r.outcome_indicator,
      output: r.output_indicator
    })),
    personnel: (d.personnel_requirements || []).map((r) => ({
      position: r.position,
      timeDevoted: r.effort,
      responsibilities: r.responsibilities
    })),
    otherProjectsNumber: d.other_projects_number,
    otherProjects: (d.other_projects || []).map((r) => ({
      title: r.project_title,
      agency: r.funding_agency,
      involvement: r.involvement
    })),
    roadmapFileName: fileName(d.technology_roadmap_file) || d.roadmap_file_name || '',
    gadFileName: fileName(d.gad_score_file),
    // Section 25: the Line-Item Budget document. The stored name (set by the upload)
    // wins, so a re-save never overwrites it with a local file name.
    beneficiariesFileName: d.budget_file_name || fileName(d.line_item_budget_file),
    supportingDocuments: [
      fileName(d.line_item_budget_file),
      ...(d.supporting_documents || []).map(fileName)
    ].filter(Boolean)
  }
}

const downloadForm = () => {
  downloadOfficialProposal(toOfficialProposal())
}

const proposalDocumentBody = () => {
  return [...proposalSectionsA(), ...proposalSectionsB()].join('')
}

const proposalSectionsA = () => {
  const p = proposal

  const researchTypeLabel =
    p.research_type === 'Basic'
      ? 'Basic Research'
      : p.research_type === 'Applied'
        ? 'Applied Research'
        : p.research_type

  const agendaBody = programList
    .filter((prog) => p.priority_agendas && p.priority_agendas[prog.key] && p.priority_agendas[prog.key].selected)
    .map((prog) => {
      const item = p.priority_agendas[prog.key]
      return field(`${prog.label} (${prog.subLabel})`, item.value || '')
    })
    .join('')

  const sixPsBody = sixPsList
    .filter((ps) => p.expected_outputs && p.expected_outputs[ps.key] && p.expected_outputs[ps.key].selected)
    .map((ps) => field(ps.label, p.expected_outputs[ps.key].value || ''))
    .join('')

  const sitesRows = (p.sites || []).map((s) => [
    s.country,
    s.region,
    s.province,
    s.district,
    s.municipality,
    s.barangay
  ])

  return [
    section({
      number: '01',
      title: 'Project Profile',
      body: `<div class="row">
          ${field('Program Title', p.program_title)}
          ${field('Project Title', p.project_title)}
          ${field('Project Leader', p.project_leader)}
          ${field('Sex', p.project_leader_sex)}
          ${field('Duration (months)', p.duration)}
          ${field('Start Date', p.start_date)}
          ${field('End Date', p.end_date)}
          ${field('Implementing College / Department', p.department)}
        </div>
        ${textBlock('Address / Contact', formatAddressContact(p.address_details, p.contact_details))}`
    }),
    section({
      number: '02',
      title: 'Cooperating Agencies',
      body: textBlock('Cooperating Agencies', joinAgencyNames(p.cooperating_agency_names) || p.cooperating_agencies)
    }),
    section({
      number: '03',
      title: 'Sites of Implementation',
      body: tableBlock(
        ['Country', 'Region', 'Province', 'District', 'Municipality', 'Barangay'],
        sitesRows
      )
    }),
    section({
      number: '04',
      title: 'Type of Research',
      body: field('Type of Research', researchTypeLabel)
    }),
    section({
      number: '05',
      title: 'Priority Agenda',
      sub: '(based on MSUN RIIDE 2025–2028)',
      body: agendaBody || textBlock('Priority Agenda', '')
    }),
    section({
      number: '06',
      title: 'Innovation Goals',
      body: textBlock('Innovation Goals', p.innovation_goals)
    }),
    section({
      number: '07',
      title: 'Sector Relevance',
      body: textBlock('Sector Relevance', p.sector_relevance)
    }),
    section({
      number: '08',
      title: 'Sustainable Development Goals (SDG)',
      body: sdgChecklistHtml(p.sustainable_development_goals)
    }),
    section({
      number: '09',
      title: 'Executive Summary',
      body: textBlock('Executive Summary', p.executive_summary)
    }),
    section({
      number: '10',
      title: 'Introduction',
      body: `${textBlock('10.1 Rationale / Significance', p.rationale)}
        ${textBlock('10.2 Scientific Basis / Theoretical Framework', p.theoretical_framework)}
        ${textBlock('10.3 General Objective', p.general_objective)}
        ${textBlock('10.4 Specific Objectives', p.specific_objectives)}`
    }),
    section({
      number: '11',
      title: 'Review of Literature',
      body: textBlock('Review of Literature', p.review_of_literature)
    }),
    section({
      number: '12',
      title: 'Methodology',
      body: textBlock('Methodology', p.methodology)
    }),
    section({
      number: '13',
      title: 'Technology Roadmap',
      body: fileField('Technology Roadmap', p.technology_roadmap_file)
    }),
    section({
      number: '14',
      title: 'Expected Outputs (6Ps)',
      body: sixPsBody || textBlock('Expected Outputs', '')
    })
  ]
}

const proposalSectionsB = () => {
  const p = proposal

  const logFrameRows = (p.logical_framework || []).map((r) => [r.outcome_indicator, r.output_indicator])
  const personnelRows = (p.personnel_requirements || []).map((r) => [r.position, r.effort, r.responsibilities])
  const otherProjectsRows = (p.other_projects || []).map((r) => [
    r.project_title,
    r.funding_agency,
    r.involvement
  ])

  const supportFiles = (p.supporting_documents || [])
    .map((f) => `<li>${escapeHtml(f.name || '')}</li>`)
    .join('')

  return [
    section({
      number: '15',
      title: 'Potential Outcomes',
      body: textBlock('Potential Outcomes', p.potential_outcomes)
    }),
    section({
      number: '16',
      title: 'Potential Impacts (2Is)',
      body: `${textBlock('a. Economic Impact', p.economic_impact)}
        ${textBlock('b. Social / Ethical Impact', p.social_ethical_impact)}`
    }),
    section({
      number: '17',
      title: 'Target Beneficiaries',
      body: textBlock('Target Beneficiaries', p.target_beneficiaries)
    }),
    section({
      number: '18',
      title: 'Sustainability Plan',
      body: textBlock('Sustainability Plan', p.sustainability_plan)
    }),
    section({
      number: '19',
      title: 'Gender and Development (GAD) Score',
      body: fileField('GAD Score Document', p.gad_score_file)
    }),
    section({
      number: '20',
      title: 'Limitations of the Project',
      body: textBlock('Limitations', p.limitations)
    }),
    section({
      number: '21',
      title: 'Risk Management Plan',
      body: textBlock('Risks & Assumptions', p.risks_assumptions)
    }),
    section({
      number: '22',
      title: 'Logical Framework',
      sub: 'Outcome and Output Indicators',
      body: tableBlock(['Outcome Indicators', 'Output Indicators / Physical Target'], logFrameRows)
    }),
    section({
      number: '23',
      title: 'Literature Cited',
      body: textBlock('Literature Cited', p.literature_cited)
    }),
    section({
      number: '24',
      title: 'Personnel Requirement',
      body: tableBlock(['Position', '% Time Devoted', 'Responsibilities'], personnelRows)
    }),
    section({
      number: '25',
      title: 'Line-Item Budget Requirement',
      body: fileField('Line-Item Budget File', p.line_item_budget_file)
    }),
    section({
      number: '26',
      title: 'Other Ongoing Projects',
      sub: 'Being handled by the Project Leader',
      body: `${field('Number of other projects', p.other_projects_number)}
        ${tableBlock(['Title', 'Funding Agency', 'Involvement'], otherProjectsRows)}`
    }),
    section({
      number: '27',
      title: 'Other Supporting Documents',
      body: supportFiles
        ? `<ul class="file-list">${supportFiles}</ul>`
        : textBlock('Supporting Documents', '')
    })
  ]
}

// ===== VALIDATION =====
const isBlank = (val) => !val || (typeof val === 'string' && val.trim() === '')

const validateStep1 = () => {
  // Clear previous errors
  Object.keys(errors).forEach(key => delete errors[key])

  // 01 - Project Profile
  // Program Title is now chosen from a dropdown (existing title) or entered as a
  // new one, so an empty/whitespace-only value is rejected either way.
  if (isBlank(proposal.program_title)) {
    errors.program_title = programTitleChoice.value === CREATE_NEW_PROGRAM_TITLE
      ? 'Please enter a new Program Title.'
      : 'Please select a program title or create a new one.'
  }
  if (isBlank(proposal.project_title)) errors.project_title = 'Project Title is required.'
  if (isBlank(proposal.project_leader)) errors.project_leader = 'Project Leader is required.'
  if (isBlank(proposal.project_leader_sex)) errors.project_leader_sex = 'Please select a sex.'
  if (!DURATION_OPTIONS.includes(String(proposal.duration))) errors.duration = 'Please select a valid project duration (12, 24, or 36 months).'
  if (isBlank(proposal.start_date)) errors.start_date = 'Start Date is required.'
  if (isBlank(proposal.end_date)) errors.end_date = 'End Date is required.'
  if (isBlank(proposal.department)) errors.department = 'Implementing College/Department is required.'
  // 01 - Address and Contact (replacing the old combined "Address / Contact"):
  // the address must be complete for the selected administrative hierarchy and
  // the contact must match its selected type.
  const addressError = validateAddress(proposal.address_details, {
    hasProvinces: addressCascade.provinceOptions.length > 0,
    hasDistricts: addressCascade.districtOptions.length > 0,
    manual: addressCascade.manual,
    manualBarangay: addressCascade.manualBarangay,
    // An independent city (City of Isabela, City of Cotabato) belongs to no
    // province and no district, so neither level is required for its address.
    independentCity: addressCascade.independentCity
  })
  if (addressError) errors.address = addressError
  const contactError = validateContact(proposal.contact_details)
  if (contactError) errors.contact = contactError

  // 02 - Cooperating Agencies: at least one agency from the shared directory. A
  // proposal saved before the checklist (free text only) keeps the previous rule.
  if (!normalizeSelectedIds(proposal.cooperating_agency_ids).length && isBlank(proposal.cooperating_agencies)) {
    errors.cooperating_agencies = 'Please select at least one cooperating agency.'
  }

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
  if (!Array.isArray(proposal.sustainable_development_goals) || proposal.sustainable_development_goals.length === 0) errors.sustainable_development_goals = 'Please select at least one SDG.'
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
      // Keep the id of the draft that was just created: the technology map is attached
      // to it, and saving again updates the draft instead of creating a second one.
      proposal.proposal_id = res.data?.id ?? proposal.proposal_id
    }
    // The image is uploaded after the proposal exists, so it is attached to this draft.
    await uploadTechnologyMapIfNeeded(proposal.proposal_id)
    // The filled-out Line-Item Budget is attached to the same draft.
    await uploadBudgetFileIfNeeded(proposal.proposal_id)
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

// Section 25: the same official template the modal previews can still be saved
// to disk (it used to point at a file that was not shipped with the app).
const downloadBudgetFormat = () => window.open(LINE_ITEM_BUDGET_TEMPLATE_URL)

const mapFormToDTO = (data, proponentId, status) => {
  return {
    proponentId,
    // Step 1 of the workflow: the Call for Proposal the proponent selected. The
    // backend attaches the proposal to that exact call, and refuses it when the call
    // is no longer open for submissions.
    proposalAnnouncementId: data.proposal_announcement_id || null,
    programTitle: data.program_title || '',
    projectTitle: data.project_title || '',
    projectLeader: data.project_leader || '',
    projectLeaderSex: data.project_leader_sex || '',
    duration: data.duration || '',
    startDate: data.start_date || '',
    endDate: data.end_date || '',
    college: data.department || '',
    // The combined "Address / Contact" column is kept up to date from the two
    // new sections, so every existing screen and export still shows both.
    address: formatAddressContact(data.address_details, data.contact_details),
    addressCountry: data.address_details?.country || '',
    addressRegion: data.address_details?.region || '',
    addressProvince: data.address_details?.province || '',
    addressDistrict: data.address_details?.district || '',
    addressMunicipality: data.address_details?.municipality || '',
    addressBarangay: data.address_details?.barangay || '',
    contactType: data.contact_details?.type || '',
    contactValue: (data.contact_details?.value || '').trim(),
    // Section 02: the selection travels as directory ids, and the legacy text column
    // is derived from the selected names (the backend derives it from the ids again,
    // so the text and the relation can never drift apart).
    cooperatingAgencies: joinAgencyNames(data.cooperating_agency_names) || data.cooperating_agencies || '',
    cooperatingAgencyIds: normalizeSelectedIds(data.cooperating_agency_ids),
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
    outputs: serializeSixPs(data.expected_outputs),
    review: data.review_of_literature || '',
    limitationsText: data.limitations || '',
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

// Load the reusable program titles once, then refresh them whenever the form is
// (re)opened so a title created in the meantime is available in the dropdown.
onMounted(fetchExistingProgramTitles)
watch(() => props.modelValue, (isOpen) => {
  if (isOpen) fetchExistingProgramTitles()
})
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

/* The steps already completed (step 1 once a call has been selected). */
.progress-step.completed .step-number {
  background: #10b981;
  color: #fff;
  font-size: 12px;
}

.progress-step.completed .step-label {
  color: #10b981;
  font-weight: 600;
}

.progress-step.completed {
  cursor: pointer;
}

.progress-line.filled {
  background: #10b981;
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

/* ===== SECTION 23: LITERATURE CITED ===== */
/* The instruction sits above the list, clearly apart from the Add Reference button. */
.reference-instruction {
  margin: 0 0 14px 0;
  padding: 10px 14px;
  background: #f8faff;
  border-left: 4px solid #4f46e5;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
}

.reference-list {
  margin: 0 0 14px 0;
  padding: 0;
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.reference-entry {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}

.reference-entry-main {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.reference-badge {
  align-self: flex-start;
  padding: 2px 9px;
  border-radius: 20px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 11px;
  font-weight: 600;
}

/* APA uses a hanging indent: the first line starts at the margin, the rest are
   indented. */
.reference-text {
  font-size: 13px;
  line-height: 1.7;
  color: #1e293b;
  padding-left: 2em;
  text-indent: -2em;
  overflow-wrap: anywhere;
}

.reference-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

@media (max-width: 640px) {
  .reference-entry {
    flex-direction: column;
    align-items: stretch;
  }

  .reference-actions {
    justify-content: flex-end;
  }
}

/* Step 1 selection carried into this step: a slim strip that names the Call for
   Proposal this proposal is being prepared for, with the action to change it. */
.selected-call {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 20px;
  padding: 12px 16px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  border-radius: 12px;
}

.selected-call-info {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  min-width: 0;
}

.selected-call-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #4f46e5;
}

.selected-call-name {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
  overflow-wrap: anywhere;
}

.selected-call-deadline {
  font-size: 12px;
  color: #475569;
}

.btn-change-call {
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #c7d2fe;
  color: #4f46e5;
  border-radius: 8px;
  padding: 7px 14px;
  font-size: 12px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-change-call:hover {
  background: #e0e7ff;
  border-color: #4f46e5;
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
  color: #ffffff;
  background: #4f46e5;
  padding: 2px 8px;
  border-radius: 4px;
}

.section-header h3 {
  font-size: 16px;
  font-weight: 700;
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

/* Neutral helper text (e.g. program title load failure) shown under a field. */
.field-hint {
  display: block;
  font-size: 12px;
  color: #64748b;
  margin-top: 4px;
}

/* Genuine load failure is shown as a warning, distinct from the empty state. */
.field-hint-error {
  color: #b45309;
}

/* The "Create New Program Title" text input sits below its dropdown. */
.program-title-new-input {
  margin-top: 8px;
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
  width: 16px;
  height: 16px;
  margin: 0;
  padding: 0;
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  accent-color: #4f46e5;
  cursor: pointer;
  flex-shrink: 0;
  vertical-align: middle;
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

/* Address (section 01): six cascading location controls, spaced like every other
   row of the form. The Contact section has no grid of its own - its dropdown and
   value input stack inside one .form-group under the "Contact *" heading. */
.ac-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.ac-field {
  min-width: 0;
}

/* The Contact value input sits directly below the contact-method dropdown. */
.contact-value-input {
  margin-top: 8px;
}

.ac-note {
  margin: 0 0 10px 0;
}

.ac-retry {
  margin-left: 6px;
  padding: 0;
  font-size: 12px;
  font-weight: 600;
  color: #4f46e5;
  text-decoration: underline;
}

@media (max-width: 900px) {
  .ac-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 560px) {
  .ac-grid {
    grid-template-columns: minmax(0, 1fr);
  }
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
  color: #1e293b;
  font-weight: 700;
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

/* ===== TECHNOLOGY MAP (section 13) ===== */
/* The upload field is labelled and documented like every other form field, but it
   sits inside the section card rather than inside a .form-group, so its label is
   styled here. */
.technology-map label {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 6px;
}

.technology-map .upload-area {
  gap: 8px;
}

.technology-map .file-info {
  gap: 12px;
  flex-wrap: wrap;
}

.technology-map .file-name {
  font-weight: 500;
  color: #1e293b;
  overflow-wrap: anywhere;
}

.technology-map .file-size {
  font-size: 12px;
  color: #64748b;
}

.file-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: auto;
}

.btn-choose-image {
  margin-top: 4px;
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #4f46e5;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-choose-image:hover {
  background: #e0e7ff;
  border-color: #4f46e5;
}

.btn-replace-file {
  background: none;
  border: 1px solid #d1d5db;
  color: #475569;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-replace-file:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.technology-map .btn-remove-file {
  border: 1px solid #fecaca;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
}

.technology-map-preview {
  margin: 12px 0 0;
  padding: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  text-align: center;
}

.technology-map-preview img {
  display: block;
  max-width: 100%;
  max-height: 280px;
  margin: 0 auto;
  border-radius: 6px;
  object-fit: contain;
}

.technology-map.has-error .upload-area {
  border-color: #dc2626;
  background: #fef2f2;
  color: #b91c1c;
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

/* Section 25: the upload control sits on its own row directly below the two
   template buttons, with vertical breathing room between the rows. */
.budget-upload {
  margin-top: 16px;
}

/* The upload control never overflows the card on narrow screens. */
.budget-upload .upload-area.small {
  max-width: 100%;
  box-sizing: border-box;
  overflow-wrap: anywhere;
}

/* Section 25: the two rule lines under the instruction - the accepted formats and
   the maximum file size. */
.budget-file-rules {
  margin: 0 0 12px 0;
  padding-left: 18px;
  list-style: disc;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.budget-file-rules strong {
  color: #0f172a;
  font-weight: 700;
}

/* The selected budget file: its name and size, then Replace / Remove. */
.budget-file .file-info {
  gap: 12px;
  flex-wrap: wrap;
}

.budget-file .file-name {
  font-weight: 500;
  color: #1e293b;
  overflow-wrap: anywhere;
}

.budget-file .file-size {
  font-size: 12px;
  color: #64748b;
}

.budget-file .btn-remove-file {
  border: 1px solid #fecaca;
  border-radius: 6px;
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
}

/* A file that was rejected (wrong format or too large) is reported on the control
   itself, so the proponent sees immediately which field is wrong. */
.budget-file.has-error .upload-area {
  border-color: #dc2626;
  background: #fef2f2;
  color: #b91c1c;
}

/* Very narrow screens: the two template buttons stack full width (no horizontal
   overflow) and the upload control spans the row, which reads as the three
   stacked actions of the section. */
@media (max-width: 560px) {
  .budget-actions {
    gap: 10px;
  }

  .budget-actions .btn-outline {
    flex: 1 1 100%;
    justify-content: center;
  }

  .budget-upload .upload-area.small {
    width: 100%;
    justify-content: center;
  }
}

/* Section 25: the instruction reads as a note (accent bar + tinted panel) so it
   is clearly distinguished from the buttons below it, above all from the
   "View Line-Item Budget Template" button that opens the viewer modal. */
.budget-template-instruction {
  margin: 0 0 12px 0;
  padding: 10px 14px;
  background: #f8faff;
  border-left: 4px solid #4f46e5;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
}

.budget-template-instruction strong {
  color: #0f172a;
  font-weight: 700;
}

/* The template viewer is the primary action of the section: it keeps the
   .btn-outline shape but carries the design system's indigo accent. */
.btn-view-template {
  border-color: #c7d2fe;
  background: #eef2ff;
  color: #4f46e5;
  font-weight: 600;
}

.btn-view-template:hover {
  border-color: #4f46e5;
  background: #e0e7ff;
  color: #4338ca;
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

/* ===== EVALUATION CRITERIA MODAL ===== */
.eval-criteria-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1100;
  padding: 20px;
}

.eval-criteria-modal {
  max-width: 850px;
}
</style>
