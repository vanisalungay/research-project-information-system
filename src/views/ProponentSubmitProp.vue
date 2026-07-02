<template>
  <div v-if="modelValue" class="modal-backdrop">
    <div class="modal-container">
      <!-- HEADER -->
      <header class="modal-header">
        <h2>SUBMIT NEW PROPOSAL</h2>
        <div class="header-actions">
          <button class="btn-download" @click="downloadForm">⬇ Download Form Format</button>
          <button class="close-btn" @click="close">×</button>
        </div>
      </header>

      <!-- BODY -->
      <div class="modal-body">
        <!-- 1. Project Profile -->
        <section>
          <h3>(1) PROJECT PROFILE</h3>
          <input v-model="proposal.program_title" placeholder="Program Title" />
          <input v-model="proposal.project_title" placeholder="Project Title" />
          <input
            v-model="proposal.project_leader"
            placeholder="Project Leader"
          />

          <select v-model="proposal.project_leader_sex">
            <option value="">Select Sex</option>
            <option value="Male">Male</option>
            <option value="Female">Female</option>
          </select>

          <div class="grid-3">
            <input v-model="proposal.duration" placeholder="Project Duration (months)" />
            <input type="date" v-model="proposal.start_date" placeholder="Project Start Date" />
            <input type="date" v-model="proposal.end_date" placeholder="Project End Date" />
          </div>
          <input v-model="proposal.department" placeholder="Implementing College / Department" />
          <textarea
            v-model="proposal.address"
            placeholder="Address / Telephone / Fax / Email (Barangay, Municipality, District, Province, Region)"
          ></textarea>
        </section>

        <!-- 2. Cooperating Agency/ies -->
        <section>
          <h3>(2) COOPERATING AGENCY / IES</h3>
          <textarea
            v-model="proposal.cooperating_agencies"
            placeholder="Name/s and Address/es"
          ></textarea>
        </section>

        <!-- 3. Sites of Implementation -->
        <section>
          <h3>(3) SITE(S) OF IMPLEMENTATION</h3>
          <table class="sites-table">
            <thead>
              <tr>
                <th>IMPLEMENTATION SITES NO.</th>
                <th>COUNTRY</th>
                <th>REGION</th>
                <th>PROVINCE</th>
                <th>DISTRICT</th>
                <th>MUNICIPALITY</th>
                <th>BARANGAY</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(site, index) in proposal.sites" :key="index">
                <td>{{ index + 1 }}</td>
                <td><input v-model="site.country" /></td>
                <td><input v-model="site.region" /></td>
                <td><input v-model="site.province" /></td>
                <td><input v-model="site.district" /></td>
                <td><input v-model="site.municipality" /></td>
                <td><input v-model="site.barangay" /></td>
                <td><button class="btn-remove" @click="removeSite(index)">✕</button></td>
              </tr>
            </tbody>
          </table>
          <button class="btn-add" @click="addSite">+ Add Site</button>
        </section>

        <!-- 4. Type of Research -->
        <section>
          <h3>(4) TYPE OF RESEARCH</h3>
          <div class="radio-group">
            <label class="radio-label">
              <input type="radio" value="Basic" v-model="proposal.research_type" />
              <span class="radio-text">Basic</span>
            </label>
            <label class="radio-label">
              <input type="radio" value="Applied" v-model="proposal.research_type" />
              <span class="radio-text">Applied</span>
            </label>
          </div>
        </section>

        <!-- 5. Priority Agenda -->
        <section>
          <h3>(5) PRIORITY AGENDA (based on MSUN RIIDE 2025–2028)</h3>
          <div v-for="program in programList" :key="program.key" class="priority-item">
            <label class="checkbox-label">
              <input type="checkbox" v-model="proposal.priority_agendas[program.key].selected" />
              <span class="checkbox-text">{{ program.label }}</span>
            </label>
            <div class="sub-input" v-if="proposal.priority_agendas[program.key].selected">
              {{ program.subLabel }}:
              <input v-model="proposal.priority_agendas[program.key].value" />
            </div>
          </div>
        </section>

        <!-- 6–9: Text Sections -->
        <section>
          <h3>(6) INNOVATION GOALS</h3>
          <textarea v-model="proposal.innovation_goals"></textarea>
        </section>
        <section>
          <h3>(7) SECTOR RELEVANCE</h3>
          <textarea
            v-model="proposal.sector_relevance"
            placeholder="Public Health, Environment, Agriculture, etc."
          ></textarea>
        </section>
        <section>
          <h3>(8) APPLICABLE SUSTAINABLE DEVELOPMENT GOAL (SDG) ADDRESSED</h3>
          <textarea v-model="proposal.sustainable_development_goals"></textarea>
        </section>
        <section>
          <h3>(9) EXECUTIVE SUMMARY</h3>
          <textarea
            v-model="proposal.executive_summary"
            placeholder="Not to exceed 200 words"
          ></textarea>
        </section>

        <!-- 10. INTRODUCTION -->
        <section>
          <h3>(10) INTRODUCTION</h3>
          <div class="sub-section">
            <h4>10.1 RATIONALE / SIGNIFICANCE</h4>
            <p class="note">(Not to exceed 300 words)</p>
            <textarea v-model="proposal.rationale"></textarea>
          </div>
          <div class="sub-section">
            <h4>10.2 SCIENTIFIC BASIS / THEORETICAL FRAMEWORK</h4>
            <textarea v-model="proposal.theoretical_framework"></textarea>
          </div>
          <div class="sub-section">
            <h4>10.3 OBJECTIVES</h4>
            <label class="black-text">General Objective:</label>
            <textarea v-model="proposal.general_objective" class="black-text"></textarea>
            <label class="black-text">Specific Objectives:</label>
            <textarea v-model="proposal.specific_objectives" class="black-text"></textarea>
          </div>
        </section>

        <!-- 11. Review of Literature (File Upload) -->
        <section>
          <h3>(11) REVIEW OF LITERATURE</h3>
          <input
            type="file"
            @change="handleFileUpload('review_of_literature', $event)"
          />

          <p v-if="proposal.review_of_literature_file">
            {{ proposal.review_of_literature_file.name }}
          </p>
        </section>

        <!-- 12. Methodology -->
        <section>
          <h3>(12) METHODOLOGY</h3>
          <textarea v-model="proposal.methodology"></textarea>
        </section>

        <!-- 13. Technology Roadmap (if applicable, file upload) -->
        <section>
          <h3>(13) TECHNOLOGY ROADMAP (if applicable)</h3>
          <input
            type="file"
            @change="handleFileUpload('technology', $event)"
          />
          <p v-if="proposal.technology_roadmap_file">Uploaded file: {{ proposal.technology_roadmap_file.name }}</p>
        </section>

        <!-- 14–16: Outputs, Outcomes, Impacts -->
        <section>
          <h3>(14) EXPECTED OUTPUTS (6Ps)</h3>
          <textarea v-model="proposal.expected_outputs"></textarea>
        </section>

        <section>
          <h3>(15) POTENTIAL OUTCOMES</h3>
          <textarea v-model="proposal.potential_outcomes"></textarea>
        </section>

        <section>
          <h3>(16) POTENTIAL IMPACTS (2Is)</h3>
          <label class="black-text">a. Economic:</label>
          <textarea v-model="proposal.economic_impact" class="black-text"></textarea>
          <label class="black-text">b. Social / Ethical:</label>
          <textarea v-model="proposal.social_ethical_impact" class="black-text"></textarea>
        </section>

        <!-- 17. Target Beneficiaries -->
        <section>
          <h3>(17) TARGET BENEFICIARIES</h3>
          <textarea
            v-model="proposal.target_beneficiaries"
            placeholder="Write the target beneficiaries here..."
          ></textarea>
        </section>

        <!-- 18. Sustainability Plan -->
        <section>
          <h3>(18) SUSTAINABILITY PLAN</h3>
          <textarea
            v-model="proposal.sustainability_plan"
            placeholder="Write the sustainability plan here..."
          ></textarea>
        </section>

        <!-- 19. Gender and Development (GAD) Score -->
        <section>
          <h3>(19) GENDER AND DEVELOPMENT (GAD) SCORE</h3>
          <input
            type="file"
            @change="handleFileUpload('gad', $event)"
          />
          <p v-if="proposal.gad_score_file">Uploaded file: {{ proposal.gad_score_file.name }}</p>
        </section>

        <!-- 20. Limitations of the Project -->
        <section>
          <h3>(20) LIMITATIONS OF THE PROJECT</h3>
          <textarea
            v-model="proposal.limitations"
            placeholder="Write the project limitations here..."
          ></textarea>
        </section>

        <!-- 21. Risks & Assumptions -->
        <section>
          <h3>(21) LIST OF RISKS AND ASSUMPTIONS / RISK MANAGEMENT PLAN</h3>
          <textarea v-model="proposal.risks_assumptions"></textarea>
        </section>

        <!-- 22. Logical Framework Table -->
        <section>
          <h3>(22) LOGICAL FRAMEWORK: OUTCOME AND OUTPUT INDICATORS</h3>
          <table class="sites-table">
            <thead>
              <tr>
                <th>Outcome Indicators</th>
                <th>Output Indicators / Physical Target</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in proposal.logical_framework" :key="index">
                <td><input v-model="row.outcome_indicator" /></td>
                <td><input v-model="row.output_indicator" /></td>
                <td>
                  <button class="btn-remove" @click="removeTableRow('logical_framework', index)">✕</button>
                </td>
              </tr>
            </tbody>
          </table>
          <button class="btn-add" @click="addTableRow('logical_framework')">+ Add Row</button>
        </section>

        <!-- 23. Literature Cited -->
        <section>
          <h3>(23) LITERATURE CITED</h3>
          <textarea
            v-model="proposal.literature_cited"
            placeholder="Write your literature references here..."
          ></textarea>
        </section>

        <!-- 24. Personnel Requirement -->
        <section>
          <h3>(24) PERSONNEL REQUIREMENT</h3>
          <table class="sites-table">
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
                <td><input v-model="row.position" /></td>
                <td><input v-model="row.effort" /></td>
                <td><input v-model="row.responsibilities" /></td>
                <td>
                  <button class="btn-remove" @click="removeTableRow('personnel_requirements', index)">✕</button>
                </td>
              </tr>
            </tbody>
          </table>
          <button class="btn-add" @click="addTableRow('personnel_requirements')">+ Add Row</button>
        </section>

        <section>
          <h3>(25) LINE-ITEM BUDGET REQUIREMENT</h3>

          <!-- Download Template Button -->
          <button class="btn-download" @click="downloadBudgetFormat">
            ⬇ Download Project Line-Item Budget File Format
          </button>

          <!-- Upload Filled File -->
          <input
            type="file"
            accept=".pdf,.doc,.docx"
            @change="handleFileUpload('budget', $event)"
          />

          <p v-if="proposal.line_item_budget_file">
            Uploaded file: {{ proposal.line_item_budget_file.name }}
          </p>
        </section>

        <!-- 26. Other Ongoing Projects -->
        <section>
          <h3>(26) OTHER ONGOING PROJECTS BEING HANDLED BY THE PROJECT LEADER</h3>
          <label class="black-text">Number: <input v-model="proposal.other_projects_number" /></label>
          <table class="sites-table">
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
                <td><input v-model="row.project_title" /></td>
                <td><input v-model="row.funding_agency" /></td>
                <td><input v-model="row.involvement" /></td>
                <td>
                  <button class="btn-remove" @click="removeTableRow('other_projects', index)">
                    ✕
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
          <button class="btn-add" @click="addTableRow('other_projects')">+ Add Row</button>
        </section>

        <!-- 27. Other Supporting Documents -->
        <section>
          <h3>(27) OTHER SUPPORTING DOCUMENTS</h3>
          <p>(Please refer to the next page for the additional necessary documents.)</p>
        </section>
      </div>

      <!-- FOOTER -->
      <footer class="modal-footer">
        <button class="btn-draft" @click="saveAsDraft">Save as Draft</button>
        <button class="btn-next" @click="goNext">Next</button>
      </footer>
    </div>
  </div>
</template>

<script setup>
import { reactive } from 'vue'

defineProps({ modelValue: Boolean })
const emit = defineEmits(['update:modelValue', 'save', 'next'])

const proposal = reactive({
  // PROJECT PROFILE
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

  // COOPERATING AGENCIES
  cooperating_agencies: '',

  // IMPLEMENTATION SITES
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

  // RESEARCH
  research_type: '',

  // PRIORITY AGENDA
  priority_agendas: {
  dagat: {
    selected: false,
    value: ''
  },
  punla: {
    selected: false,
    value: ''
  },
  kalikasan: {
    selected: false,
    value: ''
  },
  negosyo: {
    selected: false,
    value: ''
  },
  tanglaw: {
    selected: false,
    value: ''
  }
},

  // TEXT FIELDS
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

  // FILE UPLOADS
  review_of_literature_file: null,

  technology_roadmap_file: null,

  gad_score_file: null,

  line_item_budget_file: null,

  supporting_documents: [],

  // TABLES
  logical_framework: [
    {
      outcome_indicator: '',
      output_indicator: ''
    }
  ],

  literature_cited: '',

  personnel_requirements: [
    {
      position: '',
      effort: '',
      responsibilities: ''
    }
  ],

  other_projects: [
    {
      project_title: '',
      funding_agency: '',
      involvement: ''
    }
  ]
})

const programList = [
  { key: 'dagat', label: 'DAGAT Program', subLabel: 'Commodity' },
  { key: 'punla', label: 'PUNLA Program', subLabel: 'Priority Topic' },
  { key: 'kalikasan', label: 'KALIKASAN Program', subLabel: 'Sector' },
  { key: 'negosyo', label: 'NEGOSYO Program', subLabel: 'Sector' },
  { key: 'tanglaw', label: 'TANGLAW Program', subLabel: 'Sector' },
]

const close = () => emit('update:modelValue', false)
const addSite = () =>
  proposal.sites.push({
    country: '',
    region: '',
    province: '',
    district: '',
    municipality: '',
    barangay: '',
  })
const removeSite = (index) => proposal.sites.splice(index, 1)

const addTableRow = (key) => {
  let newRow = {}

  if (key === 'logical_framework') {
    newRow = {
      outcome_indicator: '',
      output_indicator: ''
    }
  }

  else if (key === 'personnel_requirements') {
    newRow = {
      position: '',
      effort: '',
      responsibilities: ''
    }
  }

  else if (key === 'other_projects') {
    newRow = {
      project_title: '',
      funding_agency: '',
      involvement: ''
    }
  }

  proposal[key].push(newRow)
}

const removeTableRow = (key, index) => {
  proposal[key].splice(index, 1)
}

const handleFileUpload = (type, event) => {
  const file = event.target.files[0]

  if (!file) return

  if (type === 'review_of_literature') {
    proposal.review_of_literature_file = file
  }

  else if (type === 'technology') {
    proposal.technology_roadmap_file = file
  }

  else if (type === 'gad') {
    proposal.gad_score_file = file
  }

  else if (type === 'budget') {
    proposal.line_item_budget_file = file
  }

  else if (type === 'supporting') {
    proposal.supporting_documents.push(file)
  }
}

const downloadForm = () => {
  const data = 'data:text/json;charset=utf-8,' + encodeURIComponent(JSON.stringify(proposal, null, 2))
  const a = document.createElement('a')
  a.href = data
  a.download = 'research-proposal.json'
  a.click()
}

const saveAsDraft = () => {
  console.log('Form saved as draft', { ...proposal })
  emit('save', { ...proposal })
}

const goNext = () => {
  console.log('Next step clicked')
  emit('next', { ...proposal })
}

const downloadBudgetFormat = () => {
  window.open('/templates/Project-Line-Item-Budget.docx')
}

</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
}

.modal-container {
  background: #fff;
  width: 95%;
  max-width: 1100px;
  max-height: 90vh;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
}

.modal-header,
.modal-footer {
  padding: 14px 16px;
  border-bottom: 1px solid #ddd;
  border-top: 1px solid #ddd;
  display: flex;
  justify-content: center;
  gap: 16px;
  padding: 16px;
}

.modal-footer button {
  width: 160px;
  height: 50px;
  font-size: 16px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
}

/* Save as Draft */
.btn-draft {
  background-color: #e5e7eb;
  border: none;
  color: #111;
}

/* Next - blue */
.btn-next {
  background-color: #2563eb;
  border: none;
  color: #fff;
}

.modal-body {
  padding: 16px;
  overflow-y: auto;
}

section {
  margin-bottom: 22px;
}
h3 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}
input,
textarea {
  width: 100%;
  padding: 6px 8px;
  border: 1px solid #ccc;
  border-radius: 4px;
  margin-bottom: 6px;
  font-size: 14px;
}
textarea {
  min-height: 80px;
}
.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}
.sites-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
}
.sites-table th,
.sites-table td {
  border: 1px solid #ccc;
  padding: 4px;
}
.sites-table input {
  border: none;
}
.btn-add {
  font-size: 13px;
}
.btn-remove {
  background: none;
  border: none;
  color: #dc2626;
  cursor: pointer;
}
.btn-save {
  background: #2563eb;
  color: #fff;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
}
.btn-cancel {
  background: #e5e7eb;
  border: none;
  padding: 6px 14px;
  border-radius: 4px;
}
.close-btn {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-download {
  background: #f3f4f6;
  border: 1px solid #d1d5db;
  padding: 6px 10px;
  border-radius: 4px;
  font-size: 13px;
}
.black-text {
  color: #000;
}
.sub-section {
  margin-bottom: 16px;
}
.sub-section h4 {
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 4px;
}
.sub-section .note {
  font-size: 12px;
  font-style: italic;
  margin-bottom: 6px;
  color: #555;
}
.sub-input {
  margin-left: 28px;
  margin-top: 4px;
  font-size: 14px;
}
.sub-input input {
  border: none;
  border-bottom: 1px solid #000;
  width: 300px;
  padding: 2px 4px;
  font-family: sans-serif;
}
/* Priority Agenda */
.priority-item {
  margin-bottom: 6px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #000;
  font-size: 14px;
}

.checkbox-label input[type='checkbox'] {
  width: 16px;
  height: 16px;
}

.checkbox-text {
  color: #000;
}

.sub-input {
  margin-left: 24px;
  margin-top: 4px;
  font-size: 14px;
}

/* Type of Research */
.radio-group {
  display: flex;
  gap: 20px;
  margin-bottom: 6px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #000;
  font-size: 14px;
}

.radio-label input[type='radio'] {
  width: 16px;
  height: 16px;
}

.radio-text {
  color: #000;
}
</style>
