<template>
  <div class="proposal-view">
    <!-- HEADER -->
    <div class="proposal-header">
      <button class="back-btn" @click="goBack">← Back</button>
      <div class="header-info">
        <h1>{{ proposal.projectTitle || 'Proposal Details' }}</h1>
        <p class="subtitle">Proposal ID: {{ proposal.id }}</p>
      </div>
    </div>

    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="goBack">Go Back</button>
    </div>

    <!-- CONTENT -->
    <template v-else-if="proposal.id">
      <!-- 1. Project Profile -->
      <section class="content-section">
        <h2>(1) PROJECT PROFILE</h2>
        <div class="info-grid">
          <p><strong>Program Title:</strong> {{ proposal.programTitle || '-' }}</p>
          <p><strong>Project Title:</strong> {{ proposal.projectTitle || '-' }}</p>
          <p><strong>Project Leader:</strong> {{ proposal.projectLeader || '-' }}</p>
          <p><strong>Duration:</strong> {{ proposal.duration || '-' }} months</p>
          <p><strong>Start Date:</strong> {{ proposal.startDate || '-' }}</p>
          <p><strong>End Date:</strong> {{ proposal.endDate || '-' }}</p>
          <p><strong>Implementing College / Department:</strong> {{ proposal.college || '-' }}</p>
          <p><strong>Address:</strong> {{ proposal.address || '-' }}</p>
        </div>
      </section>

      <!-- 2. Cooperating Agencies -->
      <section class="content-section">
        <h2>(2) COOPERATING AGENCY / IES</h2>
        <p>{{ proposal.cooperatingAgencies || '-' }}</p>
      </section>

      <!-- 3. Sites of Implementation -->
      <section class="content-section">
        <h2>(3) SITE(S) OF IMPLEMENTATION</h2>

        <div v-if="proposal.sites && proposal.sites.length > 0" class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>No.</th>
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
                <td>{{ site.district || '-' }}</td>
                <td>{{ site.municipality }}</td>
                <td>{{ site.barangay }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else>-</p>
      </section>

      <!-- 4. Type of Research -->
      <section class="content-section">
        <h2>(4) TYPE OF RESEARCH</h2>
        <p>{{ proposal.researchType || '-' }}</p>
      </section>

      <!-- 5. Priority Agenda -->
      <section class="content-section">
        <h2>(5) PRIORITY AGENDA</h2>
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
        <p v-else>-</p>
      </section>

      <!-- 6. Innovation Goals -->
      <section class="content-section">
        <h2>(6) INNOVATION GOALS</h2>
        <p>{{ proposal.innovationGoals || '-' }}</p>
      </section>

      <!-- 7. Sector Relevance -->
      <section class="content-section">
        <h2>(7) SECTOR RELEVANCE</h2>
        <p>{{ proposal.sectorRelevance || '-' }}</p>
      </section>

      <!-- 8. SDG -->
      <section class="content-section">
        <h2>(8) APPLICABLE SUSTAINABLE DEVELOPMENT GOAL (SDG)</h2>
        <p>{{ proposal.sdg || '-' }}</p>
      </section>

      <!-- 9. Executive Summary -->
      <section class="content-section">
        <h2>(9) EXECUTIVE SUMMARY</h2>
        <p>{{ proposal.executiveSummary || '-' }}</p>
      </section>

      <!-- 10. Introduction -->
      <section class="content-section">
        <h2>(10) INTRODUCTION</h2>

        <h3>10.1 Rationale / Significance</h3>
        <p>{{ proposal.rationale || '-' }}</p>

        <h3>10.2 Scientific Basis / Theoretical Framework</h3>
        <p>{{ proposal.framework || '-' }}</p>

        <h3>10.3 Objectives</h3>
        <p><strong>General Objective:</strong> {{ proposal.objectivesGeneral || '-' }}</p>
        <p><strong>Specific Objectives:</strong> {{ proposal.objectivesSpecific || '-' }}</p>
      </section>

      <!-- 11. Review of Literature -->
      <section class="content-section">
        <h2>(11) REVIEW OF LITERATURE</h2>
        <p>{{ proposal.reviewOfLiterature || '-' }}</p>
      </section>

      <!-- 12. Methodology -->
      <section class="content-section">
        <h2>(12) METHODOLOGY</h2>
        <p>{{ proposal.methodology || '-' }}</p>
      </section>

      <!-- 13. Technology Roadmap -->
      <section class="content-section">
        <h2>(13) TECHNOLOGY ROADMAP</h2>
        <p v-if="proposal.roadmapFileName">📄 {{ proposal.roadmapFileName }}</p>
        <p v-else>-</p>
      </section>

      <!-- 14. Expected Outputs -->
      <section class="content-section">
        <h2>(14) EXPECTED OUTPUTS</h2>
        <p>{{ proposal.expectedOutputs || '-' }}</p>
      </section>

      <!-- 15. Potential Outcomes -->
      <section class="content-section">
        <h2>(15) POTENTIAL OUTCOMES</h2>
        <p>{{ proposal.potentialOutcomes || '-' }}</p>
      </section>

      <!-- 16. Potential Impacts -->
      <section class="content-section">
        <h2>(16) POTENTIAL IMPACTS</h2>
        <p><strong>Economic:</strong> {{ proposal.economicImpact || '-' }}</p>
        <p><strong>Social / Ethical:</strong> {{ proposal.socialEthicalImpact || '-' }}</p>
      </section>

      <!-- 17. Target Beneficiaries -->
      <section class="content-section">
        <h2>(17) TARGET BENEFICIARIES</h2>
        <p>{{ proposal.targetBeneficiaries || '-' }}</p>
      </section>

      <!-- 18. Sustainability Plan -->
      <section class="content-section">
        <h2>(18) SUSTAINABILITY PLAN</h2>
        <p>{{ proposal.sustainabilityPlan || '-' }}</p>
      </section>

      <!-- 19. GAD Score -->
      <section class="content-section">
        <h2>(19) GENDER AND DEVELOPMENT (GAD) SCORE</h2>
        <p v-if="proposal.gadFileName">📄 {{ proposal.gadFileName }}</p>
        <p v-else>-</p>
      </section>

      <!-- 20. Limitations -->
      <section class="content-section">
        <h2>(20) LIMITATIONS</h2>
        <p>{{ proposal.limitations || '-' }}</p>
      </section>

      <!-- 21. Risks & Assumptions -->
      <section class="content-section">
        <h2>(21) RISKS & ASSUMPTIONS</h2>
        <p>{{ proposal.risksAssumptions || '-' }}</p>
      </section>

      <!-- 22. Logical Framework -->
      <section class="content-section">
        <h2>(22) LOGICAL FRAMEWORK</h2>

        <div v-if="proposal.logicalFramework && proposal.logicalFramework.length > 0" class="table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Outcome Indicators</th>
                <th>Output Indicators</th>
              </tr>
            </thead>

            <tbody>
              <tr v-for="(item, index) in proposal.logicalFramework" :key="index">
                <td>{{ item.outcome || '-' }}</td>
                <td>{{ item.output || '-' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <p v-else>-</p>
      </section>

      <!-- 23. Literature Cited -->
      <section class="content-section">
        <h2>(23) LITERATURE CITED</h2>
        <p>{{ proposal.literatureCited || '-' }}</p>
      </section>

      <!-- 24. Personnel Requirement -->
      <section class="content-section">
        <h2>(24) PERSONNEL REQUIREMENT</h2>
        <p>{{ proposal.personnelRequirements || '-' }}</p>
      </section>

      <!-- 25. Line-Item Budget -->
      <section class="content-section">
        <h2>(25) LINE-ITEM BUDGET</h2>
        <p v-if="proposal.lineItemBudgetFile">📄 {{ proposal.lineItemBudgetFile }}</p>
        <p v-else>-</p>
      </section>

      <!-- 26. Other Ongoing Projects -->
      <section class="content-section">
        <h2>(26) OTHER ONGOING PROJECTS</h2>
        <p>{{ proposal.otherProjects || '-' }}</p>
      </section>
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'

const route = useRoute()

const loading = ref(false)
const error = ref('')

const proposal = ref < any > ({
  id: null,
  programTitle: '',
  projectTitle: '',
  projectLeader: '',
  duration: '',
  startDate: '',
  endDate: '',
  college: '',
  address: '',
  cooperatingAgencies: '',
  sites: [],
  researchType: '',
  priorityAgendas: {},
  innovationGoals: '',
  sectorRelevance: '',
  sdg: '',
  executiveSummary: '',
  rationale: '',
  framework: '',
  objectivesGeneral: '',
  objectivesSpecific: '',
  reviewOfLiterature: '',
  methodology: '',
  roadmapFileName: '',
  expectedOutputs: '',
  potentialOutcomes: '',
  economicImpact: '',
  socialEthicalImpact: '',
  targetBeneficiaries: '',
  sustainabilityPlan: '',
  gadFileName: '',
  limitations: '',
  risksAssumptions: '',
  logicalFramework: [],
  literatureCited: '',
  personnelRequirements: '',
  lineItemBudgetFile: '',
  otherProjects: ''
})

const hasPriorityAgendas = computed(() => {
  if (!proposal.value.priorityAgendas) return false
  return Object.values(proposal.value.priorityAgendas).some(agenda => agenda.selected)
})

// Fetch proposal data
onMounted(async () => {
  const proposalId = route.params.id || route.query.id
  if (!proposalId) {
    error.value = 'No proposal ID provided.'
    return
  }

  loading.value = true
  error.value = ''

  try {
    const res = await api.get(`/api/proposals/${proposalId}`)
    const data = res.data

    proposal.value = {
      id: data.id,
      programTitle: data.programTitle || '',
      projectTitle: data.projectTitle || '',
      projectLeader: data.projectLeader || '',
      duration: data.duration || '',
      startDate: data.startDate || '',
      endDate: data.endDate || '',
      college: data.college || '',
      address: data.address || '',
      cooperatingAgencies: data.cooperatingAgencies || '',
      sites: data.sites || [],
      researchType: data.researchType || '',
      priorityAgendas: data.priorityAgendas || {},
      innovationGoals: data.innovationGoals || '',
      sectorRelevance: data.sectorRelevance || '',
      sdg: data.sdg || '',
      executiveSummary: data.executiveSummary || '',
      rationale: data.rationale || '',
      framework: data.framework || '',
      objectivesGeneral: data.objectivesGeneral || '',
      objectivesSpecific: data.objectivesSpecific || '',
      reviewOfLiterature: data.reviewOfLiterature || '',
      methodology: data.methodology || '',
      roadmapFileName: data.roadmapFileName || '',
      expectedOutputs: data.expectedOutputs || '',
      potentialOutcomes: data.potentialOutcomes || '',
      economicImpact: data.economicImpact || '',
      socialEthicalImpact: data.socialEthicalImpact || '',
      targetBeneficiaries: data.targetBeneficiaries || '',
      sustainabilityPlan: data.sustainabilityPlan || '',
      gadFileName: data.gadFileName || '',
      limitations: data.limitations || '',
      risksAssumptions: data.risksAssumptions || '',
      logicalFramework: data.logicalFramework || [],
      literatureCited: data.literatureCited || '',
      personnelRequirements: data.personnelRequirements || '',
      lineItemBudgetFile: data.lineItemBudgetFile || '',
      otherProjects: data.otherProjects || ''
    }
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }

  const goBack = () => {
    window.history.back()
  }
})
</script>

<style scoped>
.proposal-view {
  max-width: 900px;
  margin: 24px auto;
  padding: 16px;
  font-family: sans-serif;
  line-height: 1.6;
}

/* HEADER */
.proposal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
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

.header-info {
  flex: 1;
}

.header-info h1 {
  font-size: 22px;
  margin-bottom: 4px;
}

.subtitle {
  font-size: 14px;
  color: #555;
  margin-bottom: 0;
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

/* CONTENT SECTIONS */
.content-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
}

.content-section h2 {
  font-size: 16px;
  margin-top: 0;
  margin-bottom: 12px;
  border-bottom: 1px solid #ccc;
  padding-bottom: 4px;
  font-weight: bold;
  color: #0f172a;
}

.content-section h3 {
  font-size: 14px;
  margin-top: 12px;
  margin-bottom: 6px;
  color: #1e293b;
}

.content-section p {
  margin: 4px 0 8px 0;
  color: #334155;
}

.content-section p strong {
  color: #0f172a;
}

/* INFO GRID */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-top: 8px;
}

/* AGENDA LIST */
.agenda-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.agenda-item {
  padding: 10px 14px;
  background: #f8fafc;
  border-radius: 6px;
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
  margin-top: 8px;
}

table {
  width: 100%;
  border-collapse: collapse;
}

table th,
table td {
  border: 1px solid #ccc;
  padding: 8px;
  text-align: left;
  font-size: 14px;
}

table th {
  background: #f8fafc;
  font-weight: 600;
  color: #475569;
}

table td {
  color: #1e293b;
}
</style>