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

            <p>
                <strong>Number of Other Ongoing Projects:</strong>
                {{ proposal.other_projects_number }}
            </p>
        </section>

        <!-- 2. Cooperating Agencies -->
        <section>
            <h2>(2) COOPERATING AGENCY / IES</h2>
            <p>{{ proposal.cooperating_agencies }}</p>
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
                    <tr v-for="(site, index) in proposal.sites" :key="site.id">
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

            <p v-else class="empty-text">
                No research type available.
            </p>
        </section>

        <!-- 5. Priority Agenda -->
        <section>
            <h2>(5) PRIORITY AGENDA</h2>

            <div v-if="proposal.priority_agendas.dagat.selected">
                <strong>DAGAT:</strong>
                {{ proposal.priority_agendas.dagat.value }}
            </div>

            <div v-if="proposal.priority_agendas.punla.selected">
                <strong>PUNLA:</strong>
                {{ proposal.priority_agendas.punla.value }}
            </div>

            <div v-if="proposal.priority_agendas.kalikasan.selected">
                <strong>KALIKASAN:</strong>
                {{ proposal.priority_agendas.kalikasan.value }}
            </div>

            <div v-if="proposal.priority_agendas.negosyo.selected">
                <strong>NEGOSYO:</strong>
                {{ proposal.priority_agendas.negosyo.value }}
            </div>

            <div v-if="proposal.priority_agendas.tanglaw.selected">
                <strong>TANGLAW:</strong>
                {{ proposal.priority_agendas.tanglaw.value }}
            </div>

            <p v-else class="empty-text">
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

            <ul v-if="proposal.sustainable_development_goals?.length">
                <li v-for="(goal, index) in proposal.sustainable_development_goals" :key="index">
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

            <p v-else class="empty-text">
                No rationale provided.
            </p>

            <!-- 10.2 -->
            <h3>10.2 Scientific Basis / Theoretical Framework</h3>

            <p v-if="proposal.theoretical_framework">
                {{ proposal.theoretical_framework }}
            </p>

            <p v-else class="empty-text">
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

            <p v-if="proposal.specific_objectives">
                {{ proposal.specific_objectives }}
            </p>

            <p v-else class="empty-text">
                No specific objectives provided.
            </p>
        </section>

        <!-- 11. REVIEW OF LITERATURE -->
        <section>
            <h2>(11) REVIEW OF LITERATURE</h2>

            <a v-if="proposal.review_of_literature_file" :href="proposal.review_of_literature_file" target="_blank">
                View Review of Literature
            </a>

            <p v-else class="empty-text">
                No file uploaded.
            </p>
        </section>

        <!-- 12. METHODOLOGY -->
        <section>
            <h2>(12) METHODOLOGY</h2>

            <p v-if="proposal.methodology">
                {{ proposal.methodology }}
            </p>

            <p v-else class="empty-text">
                No methodology provided.
            </p>
        </section>

        <!-- 13–27 Simplified for brevity -->
        <section>
            <h2>(13) TECHNOLOGY ROADMAP</h2>

            <a v-if="proposal.technology_roadmap_file" :href="proposal.technology_roadmap_file" target="_blank">
                View Technology Roadmap
            </a>

            <p v-else>No file uploaded.</p>
        </section>

        <section>
            <h2>(14) EXPECTED OUTPUTS</h2>

            <p v-if="proposal.expected_outputs">
                {{ proposal.expected_outputs }}
            </p>

            <p v-else class="empty-text">
                No expected outputs provided.
            </p>
        </section>

        <section>
            <h2>(15) POTENTIAL OUTCOMES</h2>

            <p v-if="proposal.potential_outcomes">
                {{ proposal.potential_outcomes }}
            </p>

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

            <a v-if="proposal.gad_score_file" :href="proposal.gad_score_file" target="_blank">
                View GAD Score
            </a>

            <p v-else class="empty-text">
                No GAD file uploaded.
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

                    <tr v-for="item in proposal.logical_framework" :key="item.id">
                        <td>{{ item.outcome_indicator }}</td>
                        <td>{{ item.output_indicator }}</td>
                    </tr>

                </tbody>
            </table>
        </section>

        <section>
            <h2>(23) LITERATURE CITED</h2>

            <p v-if="proposal.literature_cited">
                {{ proposal.literature_cited }}
            </p>

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

                    <tr v-if="proposal.personnel_requirements.length === 0">
                        <td colspan="3">
                            No personnel listed.
                        </td>
                    </tr>

                    <tr v-for="person in proposal.personnel_requirements" :key="person.id">
                        <td>{{ person.name }}</td>
                        <td>{{ person.effort }}</td>
                        <td>{{ person.responsibilities }}</td>
                    </tr>

                </tbody>

            </table>
        </section>

        <section>
            <h2>(25) LINE-ITEM BUDGET</h2>

            <a v-if="proposal.line_item_budget_file" :href="proposal.line_item_budget_file" target="_blank">
                View Line Item Budget
            </a>

            <p v-else>No file uploaded.</p>
        </section>

        <section>
            <h2>(26) OTHER ONGOING PROJECTS</h2>

            <ul v-if="proposal.other_projects.length">
                <li v-for="project in proposal.other_projects" :key="project.id">
                    {{ project.project_title }}
                </li>
            </ul>

            <p v-else class="empty-text">
                No ongoing projects.
            </p>
        </section>

        <section>
            <h2>(27) SUPPORTING DOCUMENTS</h2>

            <ul v-if="proposal.supporting_documents.length">
                <li v-for="file in proposal.supporting_documents" :key="file.id">
                    <a :href="file.file_path" target="_blank">
                        {{ file.file_name }}
                    </a>
                </li>
            </ul>

            <p v-else>No supporting documents uploaded.</p>
        </section>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'

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
    other_projects_number: '',

    // Cooperating Agencies
    cooperating_agencies: '',

    // Sites of Implementation
    sites: [],

    // Research Information
    research_type: '',
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
    specific_objectives: '',

    review_of_literature_file: null,
    methodology: '',
    technology_roadmap_file: null,
    expected_outputs: '',
    potential_outcomes: '',
    economic_impact: '',
    social_ethical_impact: '',
    target_beneficiaries: '',
    sustainability_plan: '',
    gad_score_file: null,
    limitations: '',
    risks_assumptions: '',
    logical_framework: [],
    literature_cited: '',
    personnel_requirements: [],
    line_item_budget_file: null,
    other_projects: [],
    supporting_documents: [],
})

const loading = ref(false)

async function fetchProposal() {
    loading.value = true

    try {

        // API goes here later

    } catch (error) {
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
