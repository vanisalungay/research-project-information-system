<template>
  <div class="proposal-view">
    <div class="back" @click="$router.go(-1)">← Back</div>
    <br />
    <h1>{{ proposal.project_title }}</h1>
    <p class="subtitle"> Proposal ID: {{ proposal.proposal_id }} </p>

    <!-- 1. Project Profile -->
    <section>
      <h2>(1) PROJECT PROFILE</h2>
      <p>
        <strong>Program Title:</strong>
        {{ proposal.program_title }}
      </p>

      <p>
        <strong>Project Title:</strong>
        {{ proposal.project_title }}
      </p>

      <p>
        <strong>Project Leader / Sex:</strong>
        {{ proposal.project_leader }}
        {{ proposal.project_leader_sex }}
      </p>

      <div class="grid-3">
        <p>
          <strong>Duration:</strong>
          {{ proposal.duration }}
        </p>

        <p>
          <strong>Start Date:</strong>
          {{ proposal.start_date }}
        </p>

        <p>
          <strong>End Date:</strong>
          {{ proposal.end_date }}
        </p>
      </div>

      <p>
        <strong>Implementing College / Department:</strong> 
        {{ proposal.department }}
      </p>
      
      <p>
        <strong>Address:</strong> 
        {{ proposal.address }}
      </p>
    </section>

    <!-- 2. Cooperating Agencies -->
    <section>
      <h2>(2) COOPERATING AGENCY / IES</h2>
      <p>{{ proposal.agency }}</p>
    </section>

    <!-- 3. Sites of Implementation -->
    <section>
      <h2>(3) SITE(S) OF IMPLEMENTATION</h2>

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
          <!-- Empty State -->
          <tr v-if="proposal.sites.length === 0">
            <td colspan="7" class="empty-table">
              No implementation sites available.
            </td>
          </tr>

          <!-- Site List -->
          <tr
            v-for="(site, index) in proposal.sites"
            :key="site.id"
          >
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
    </section>

    <!-- 4. Type of Research -->
    <section>
      <h2>(4) TYPE OF RESEARCH</h2>

      <p v-if="proposal.research_type">
        {{ proposal.research_type }}
      </p>

      <p
        v-else
        class="empty-text"
      >
        No research type available.
      </p>
    </section>

    <!-- 5. Priority Agenda -->
    <section>
      <h2>(5) PRIORITY AGENDA</h2>

      <ul v-if="proposal.priority_agendas.length > 0">
        <li
          v-for="agenda in proposal.priority_agendas"
          :key="agenda.id"
        >
          {{ agenda.name }}
        </li>
      </ul>

      <p
        v-else
        class="empty-text"
      >
        No priority agenda available.
      </p>
    </section>

    <!-- 6–9 -->
    <section>
      <h2>(6) INNOVATION GOALS</h2>

      <p v-if="proposal.innovation_goals">
        {{ proposal.innovation_goals }}
      </p>

      <p v-else class="empty-field">
        No innovation goals provided.
      </p>
    </section>

    <section>
      <h2>(7) SECTOR RELEVANCE</h2>

      <p v-if="proposal.sector_relevance">
        {{ proposal.sector_relevance }}
      </p>

      <p v-else class="empty-field">
        No sector relevance specified.
      </p>
    </section>

    <section>
      <h2>(8) APPLICABLE SUSTAINABLE DEVELOPMENT GOAL (SDG)</h2>

      <ul
        v-if="proposal.sustainable_development_goals?.length"
      >
        <li
          v-for="(goal, index) in proposal.sustainable_development_goals"
          :key="index"
        >
          {{ goal }}
        </li>
      </ul>

      <p v-else class="empty-field">
        No SDGs specified.
      </p>
    </section>

    <section>
      <h2>(9) EXECUTIVE SUMMARY</h2>

      <p v-if="proposal.executive_summary">
        {{ proposal.executive_summary }}
      </p>

      <p v-else class="empty-field">
        No executive summary provided.
      </p>
    </section>

    <!-- 10. INTRODUCTION -->
    <section>
      <h2>(10) INTRODUCTION</h2>

      <!-- 10.1 -->
      <h3>10.1 Rationale / Significance</h3>

      <p v-if="proposal.rationale">
        {{ proposal.rationale }}
      </p>

      <p
        v-else
        class="empty-text"
      >
        No rationale provided.
      </p>

      <!-- 10.2 -->
      <h3>10.2 Scientific Basis / Theoretical Framework</h3>

      <p v-if="proposal.theoretical_framework">
        {{ proposal.theoretical_framework }}
      </p>

      <p
        v-else
        class="empty-text"
      >
        No theoretical framework provided.
      </p>

      <!-- 10.3 -->
      <h3>10.3 Objectives</h3>

      <strong>General Objective</strong>

      <p v-if="proposal.general_objective">
        {{ proposal.general_objective }}
      </p>

      <p v-else class="empty-text">
        No general objective provided.
      </p>

      <strong>Specific Objectives</strong>

      <ul v-if="proposal.specific_objectives.length">
        <li
          v-for="objective in proposal.specific_objectives"
          :key="objective.id"
        >
          {{ objective.objective }}
        </li>
      </ul>

      <p v-else class="empty-text">
        No specific objectives provided.
      </p>
    </section>

    <!-- 11. REVIEW OF LITERATURE -->
    <section>
      <h2>(11) REVIEW OF LITERATURE</h2>

      <p v-if="proposal.review_of_literature">
        {{ proposal.review_of_literature }}
      </p>

      <p
        v-else
        class="empty-text"
      >
        No review of literature provided.
      </p>
    </section>

    <!-- 12. METHODOLOGY -->
    <section>
      <h2>(12) METHODOLOGY</h2>

      <p v-if="proposal.methodology">
        {{ proposal.methodology }}
      </p>

      <p
        v-else
        class="empty-text"
      >
        No methodology provided.
      </p>
    </section>

    <!-- 13–27 Simplified for brevity -->
    <section>
      <h2>(13) TECHNOLOGY ROADMAP</h2>

      <p v-if="proposal.technology_roadmap">
        {{ proposal.technology_roadmap }}
      </p>

      <p v-else class="empty-text">
        No technology roadmap provided.
      </p>
    </section>

    <section>
      <h2>(14) EXPECTED OUTPUTS</h2>

      <ul v-if="proposal.expected_outputs.length">
        <li
          v-for="output in proposal.expected_outputs"
          :key="output.id"
        >
          {{ output.output }}
        </li>
      </ul>

      <p v-else class="empty-text">
        No expected outputs provided.
      </p>
    </section>

    <section>
      <h2>(15) POTENTIAL OUTCOMES</h2>

      <ul v-if="proposal.potential_outcomes.length">
        <li
          v-for="outcome in proposal.potential_outcomes"
          :key="outcome.id"
        >
          {{ outcome.outcome }}
        </li>
      </ul>

      <p v-else class="empty-text">
        No potential outcomes provided.
      </p>
    </section>

    <section>
      <h2>(16) POTENTIAL IMPACTS</h2>

      <p>
        <strong>Economic:</strong>
        {{ proposal.economic_impact || 'Not provided.' }}
      </p>

      <p>
        <strong>Social / Ethical:</strong>
        {{ proposal.social_ethical_impact || 'Not provided.' }}
      </p>
    </section>

    <section>
      <h2>(17) TARGET BENEFICIARIES</h2>

      <p v-if="proposal.target_beneficiaries">
        {{ proposal.target_beneficiaries }}
      </p>

      <p v-else class="empty-text">
        No beneficiaries specified.
      </p>
    </section>

    <section>
      <h2>(18) SUSTAINABILITY PLAN</h2>

      <p v-if="proposal.sustainability_plan">
        {{ proposal.sustainability_plan }}
      </p>

      <p v-else class="empty-text">
        No sustainability plan provided.
      </p>
    </section>

    <section>
      <h2>(19) GENDER AND DEVELOPMENT (GAD) SCORE</h2>

      <p v-if="proposal.gad_score">
        {{ proposal.gad_score }}
      </p>

      <p v-else class="empty-text">
        No GAD information provided.
      </p>
    </section>

    <section>
      <h2>(20) LIMITATIONS</h2>

      <p v-if="proposal.limitations">
        {{ proposal.limitations }}
      </p>

      <p v-else class="empty-text">
        No limitations provided.
      </p>
    </section>

    <section>
      <h2>(21) RISKS & ASSUMPTIONS</h2>

      <p v-if="proposal.risks_assumptions">
        {{ proposal.risks_assumptions }}
      </p>

      <p v-else class="empty-text">
        No risks or assumptions provided.
      </p>
    </section>

    <section>
      <h2>(22) LOGICAL FRAMEWORK</h2>

      <table>
        <thead>
          <tr>
            <th>Outcome Indicators</th>
            <th>Output Indicators</th>
          </tr>
        </thead>

        <tbody>

          <tr v-if="proposal.logical_framework.length === 0">
            <td colspan="2" class="empty-table">
              No logical framework available.
            </td>
          </tr>

          <tr
            v-for="item in proposal.logical_framework"
            :key="item.id"
          >
            <td>{{ item.outcome_indicator }}</td>
            <td>{{ item.output_indicator }}</td>
          </tr>

        </tbody>
      </table>
    </section>

    <section>
      <h2>(23) LITERATURE CITED</h2>

      <ul v-if="proposal.literature_cited.length">
        <li
          v-for="reference in proposal.literature_cited"
          :key="reference.id"
        >
          {{ reference.reference }}
        </li>
      </ul>

      <p v-else class="empty-text">
        No references provided.
      </p>
    </section>

    <section>
      <h2>(24) PERSONNEL REQUIREMENT</h2>

      <table>

        <thead>
          <tr>
            <th>Name</th>
            <th>Effort</th>
            <th>Responsibilities</th>
          </tr>
        </thead>

        <tbody>

          <tr
            v-if="proposal.personnel_requirements.length === 0"
          >
            <td colspan="3">
              No personnel listed.
            </td>
          </tr>

          <tr
            v-for="person in proposal.personnel_requirements"
            :key="person.id"
          >
            <td>{{ person.name }}</td>
            <td>{{ person.effort }}</td>
            <td>{{ person.responsibilities }}</td>
          </tr>

        </tbody>

      </table>
    </section>

    <section>
      <h2>(25) LINE-ITEM BUDGET</h2>

      <table>

        <thead>
          <tr>
            <th>Category</th>
            <th>Amount</th>
          </tr>
        </thead>

        <tbody>

          <tr
            v-for="budget in proposal.line_item_budget"
            :key="budget.id"
          >
            <td>{{ budget.category }}</td>
            <td>{{ budget.amount }}</td>
          </tr>

        </tbody>

      </table>
    </section>

    <section>
      <h2>(26) OTHER ONGOING PROJECTS</h2>

      <ul v-if="proposal.other_projects.length">
        <li
          v-for="project in proposal.other_projects"
          :key="project.id"
        >
          {{ project.project_title }}
        </li>
      </ul>

      <p v-else class="empty-text">
        No ongoing projects.
      </p>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const proposal = ref({
  // Basic Information
  id: null,
  proposal_id: '',
  program_title: '',
  project_title: '',

  // Project Profile
  project_leader: '',
  project_leader_sex: '',
  duration: '',
  start_date: '',
  end_date: '',
  department: '',
  address: '',

  // Cooperating Agencies
  agency: '',

  // Sites of Implementation
  sites: [],

  // Research Information
  research_type: '',
  priority_agendas: [],

  // Innovation
  innovation_goals: '',

  // Sector
  sector_relevance: '',

  // Sustainable Development Goals
  sustainable_development_goals: [],

  // Executive Summary
  executive_summary: '',

  // Introduction
  rationale: '',
  theoretical_framework: '',
  general_objective: '',
  specific_objectives: [],

  review_of_literature: '',
  methodology: '',
  technology_roadmap: '',
  expected_outputs: [],
  potential_outcomes: [],
  economic_impact: '',
  social_ethical_impact: '',
  target_beneficiaries: '',
  sustainability_plan: '',
  gad_score: '',
  limitations: '',
  risks_assumptions: '',
  logical_framework: [],
  literature_cited: [],
  personnel_requirements: [],
  line_item_budget: [],
  other_projects: [],
})

const loading = ref(false)

async function fetchProposal() {
    loading.value = true

    try {

        // API goes here later

    } catch(error) {
        console.error(error)
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchProposal()
})

</script>

<style scoped>
.proposal-view {
  width: 100%;
  max-width: 100%;
  padding: 2rem;
  box-sizing: border-box;
  background: #f3f4f6;
  min-height: 100vh;
}

h1 {
  font-size: 22px;
  margin-bottom: 4px;
}
.subtitle {
  font-size: 14px;
  color: #555;
  margin-bottom: 16px;
}
h2 {
  font-size: 16px;
  margin-top: 16px;
  margin-bottom: 8px;
  border-bottom: 1px solid #ccc;
  padding-bottom: 2px;
  font-weight: bold;
}
h3 {
  font-size: 14px;
  margin-top: 8px;
  margin-bottom: 4px;
}
p {
  margin: 2px 0 6px 0;
}
.grid-3 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 4px;
  margin-bottom: 8px;
}
table th,
table td {
  border: 1px solid #ccc;
  padding: 6px;
  text-align: left;
}
</style>
