<template>
  <div class="proposal-page">
    <div class="top-grid">
      <!-- Proposal Information -->
      <div class="card">
        <h2 class="title"> {{ proposal.project_title }}</h2>
        <p class="subtitle"> Proposal ID: {{ proposal.proposal_id }} </p>

        <div class="info-grid">
          <div>
            <span class="label">Category</span>
            <span class="value">{{ proposal.program_title }}</span>
          </div>
          <div>
            <span class="label">Total Budget</span>
            <span class="value">₱{{ Number(proposal.total_budget).toLocaleString() }}</span>
          </div>
          <div>
            <span class="label">Duration</span>
            <span class="value">{{ proposal.duration }}</span>
          </div>
        </div>

        <div class="actions">
          <button class="btn btn-edit" @click="editProposal">Edit Proposal</button>

          <button class="btn btn-save" @click="viewDetails">See Detailed Proposal</button>

          <button class="btn btn-download" @click="downloadPdf">Download PDF</button>
        </div>
      </div>

      
    </div>

    <!-- WORKFLOW + ATTACHMENTS -->
    <div class="mid-grid">
      <!-- WORKFLOW TIMELINE -->
      <div class="card">
        <h3 class="section-title">Workflow Timeline</h3>

        <div class="workflow">
          <template
            v-for="(item, index) in proposal.workflow"
            :key="item.id"
          >
            <div class="wf-item" :class="item.status">
              <div class="wf-header">
                <span>{{ item.stage }}</span>
                <span class="wf-date">{{ item.created_at }}</span>
              </div>

              <p class="wf-meta">{{ item.action_by }}</p>

              <p>{{ item.remarks }}</p>

              <span
                v-if="item.status === 'current'"
                class="status-pill"
              >
                Current Stage
              </span>
            </div>

            <div
              v-if="index !== proposal.workflow.length - 1"
              class="wf-arrow"
            >
              ↓
            </div>
          </template>
        </div>
      </div>

      <!-- ATTACHMENTS -->
      <div class="card">
        <h3 class="section-title">Attachments</h3>

        <ul class="files">
          <li
            v-for="file in proposal.attachments"
            :key="file.id"
          >
            {{ file.file_name }}
          </li>
        </ul>
      </div>
    </div>

    <!-- ACTIVITY LOG -->
    <div class="card">
      <h3 class="section-title">Activity Log</h3>

      <div
        v-for="log in proposal.activity_logs"
        :key="log.id"
        class="log-item"
        :class="log.type"
      >
        <div class="log-header">
          <strong>{{ log.title }}</strong>
          <span>{{ log.created_at }}</span>
        </div>

        <p>{{ log.description }}</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)

const proposal = ref({
  // DATABASE
  id: null,
  proposal_id: '',

  // PROJECT PROFILE
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
  sites: [],

  // TYPE OF RESEARCH
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

  literature_cited: '',

  // TABLES
  logical_framework: [],

  personnel_requirements: [],

  other_projects: [],

  // FILES
  review_of_literature_file: null,
  technology_roadmap_file: null,
  gad_score_file: null,
  line_item_budget_file: null,

  supporting_documents: [],

  // DETAILS PAGE
  total_budget: 0,
  status: '',
  current_stage: '',
  pdf_url: '',

  // WORKFLOW
  workflow: [
    // backend
    // {
    //   id: 1,
    //   stage: '',
    //   created_at: '',
    //   action_by
    //   remarks: '',
    //   status: '' 
    // }
  ],

  // ATTACHMENTS
  attachments: [
    // backend
    // {
    //   id: null,
    //   file_name: '',
    //   file_path: '',
    //   file_type: '',
    //   uploaded_at: ''
    // }
  ],

  // ACTIVITY LOG
  activity_logs: [
    // backend
    // {
    //   id: 1,
    //   title: '',
    //   description: '',
    //   created_at: '',
    //   type: 'success'
    // }
  ],
})

async function fetchProposal() {
  loading.value = true

  try {
    // API goes here later

    // Example:
    // const response = await axios.get(...)
    // proposal.value = response.data

  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

function editProposal() {
  router.push(`/submit-revision/${proposal.value.id}`)
}

function viewDetails() {
  router.push(`/detailed-proposal/${proposal.value.id}`)
}

function downloadPdf() {
  if (!proposal.value.pdf_url) return

  window.open(proposal.value.pdf_url, '_blank')
}

onMounted(() => {
  fetchProposal()
})
</script>

<style scoped>
.proposal-page {
  padding: 24px;
  background: #f2f2f2;
}

/* GRID */
.top-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}

.mid-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 20px;
  margin-top: 20px;
}

/* CARD */
.card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05);
}

/* TEXT */
.title {
  font-size: 20px;
  font-weight: 600;
}

.subtitle {
  color: #888;
  margin-bottom: 16px;
}

.label {
  font-size: 12px;
  color: #888;
}

.value {
  display: block;
  font-weight: 600;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;
}

/* INFO */
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

/* BUTTONS */
.actions {
  display: flex;
  gap: 10px;
}

.btn {
  padding: 8px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  background: transparent;
  transition: all 0.2s ease;
}

.btn-edit {
  color: #1976d2;
  border: 1px solid #90caf9;
}

.btn-edit:hover {
  background: #e3f2fd;
}

.btn-save {
  background: #fdd835;
  border: 1px solid #fbc02d;
  color: #000;
}

.btn-save:hover {
  background: #fbc02d;
}

.btn-download {
  color: #555;
  border: 1px solid #ccc;
}

.btn-download:hover {
  background: #f5f5f5;
}

/* WORKFLOW */
.workflow {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wf-item {
  padding: 14px;
  border-radius: 8px;
  font-size: 13px;
}

.wf-header {
  display: flex;
  justify-content: space-between;
  font-weight: 600;
}

.wf-date {
  font-size: 12px;
  color: #666;
}

.wf-meta {
  font-size: 12px;
  color: #666;
}

.wf-arrow {
  text-align: center;
  color: #bbb;
  font-size: 18px;
}

.success {
  background: #e9fff1;
  border: 1px solid #9be7b2;
}

.current {
  background: #fff4cc;
  border: 1px solid #ffca28;
}

.pending {
  background: #f5f5f5;
  border: 1px solid #ddd;
}

.status-pill {
  margin-top: 8px;
  display: inline-block;
  background: #ffca28;
  padding: 4px 10px;
  font-size: 11px;
  border-radius: 12px;
  font-weight: 600;
}

/* FILES */
.files {
  list-style: none;
  padding: 0;
  margin: 0;
}

.files li {
  padding: 8px 0;
  font-size: 14px;
}

/* ACTIVITY LOG */
.activity-log {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.log-item {
  border-radius: 8px;
  padding: 12px;
  font-size: 13px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
}

.log-header span {
  font-size: 11px;
  color: #555;
}

.log-item {
  background: #e8f5e9;
  border: 1px solid #a5d6a7;
}
</style>
