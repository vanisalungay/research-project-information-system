<template>
  <div class="proposal-view rv-page">
    <!-- HEADER: the shared review header - Back, title, id, status and DOWNLOAD. -->
    <ProposalReviewHeader
      :title="proposal.projectTitle"
      :document-id="proposal.documentId || proposal.proposalCode"
      :status="proposal.status"
      :show-download="false"
      @back="goBack"
    />
    <!-- LOADING STATE -->
    <div v-if="loading" class="loading-state">
      <div class="spinner"></div>
      <p>Loading proposal details...</p>
    </div>

    <!-- ERROR STATE -->
    <div v-else-if="error" class="error-state">
      <p>{{ error }}</p>
      <button class="btn-primary" @click="loadProposal">Retry</button>
      <button class="back-btn btn-secondary" @click="goBack">Go Back</button>
    </div>

    <!-- MISSING ID / NOT FOUND: said out loud instead of an empty page -->
    <div v-else-if="missingId || notFound" class="error-state">
      <p v-if="missingId">No proposal was selected. Open one from the proposal list.</p>
      <p v-else>This proposal could not be found, or it is not available to your account.</p>
      <button class="btn-primary" @click="goBack">Back to Proposals</button>
    </div>

    <!-- CONTENT: the field sections in the wide column, the summary cards beside them. -->
    <template v-else-if="proposal.id">
      <div class="rv-body">
        <div class="rv-main">
      <!-- 1. Project Profile -->
          <!-- Every reviewable field, rendered from the shared field registry
               (src/utils/proposalFields.js) so each field has its own remark control and
               the correct entity/DTO key. Sections 01-27 are covered exhaustively. -->
          <ProposalFieldSection
            v-for="section in PROPOSAL_FIELD_SECTIONS"
            :key="section.key"
            :section="section"
            :proposal="proposal"
            :proposal-id="proposal.id"
            :remark-index="remarkIndex"
            :editable="true"
            @download="downloadFile"
            @remark-saved="onRemarkSaved"
            @remark-removed="onRemarkRemoved"
          />
        </div>

        <!-- Proposal Status and Quick Information: the values this page already loaded. -->
        <ProposalReviewSidebar
          :status="proposal.status"
          :created-at="proposal.createdAt"
          :updated-at="proposal.updatedAt"
          :quick="quickRows"
        />
      </div>
    </template>

    <!-- FALLBACK: the content area is never silently empty -->
    <div v-else class="error-state">
      <p>No proposal details to show.</p>
      <button class="btn-primary" @click="loadProposal">Retry</button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import SdgChecklist from '@/components/SdgChecklist.vue'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import ProposalReviewHeader from '@/components/ProposalReviewHeader.vue'
import ProposalReviewSidebar from '@/components/ProposalReviewSidebar.vue'
import { PROPOSAL_FIELD_SECTIONS, buildFieldRemarkIndex } from '@/utils/proposalFields'

const route = useRoute()
const router = useRouter()

// Starts as loading so the first paint shows the spinner instead of an empty content area.
const loading = ref(true)
const error = ref('')

// Raw proposal response: the field registry resolves values from the real
// entity/DTO keys (the previous local aliases pointed at fields that do not exist).
const remarksResponse = ref(null)
const remarksError = ref('')
const remarkIndex = computed(() => buildFieldRemarkIndex(remarksResponse.value))

const loadFieldRemarks = async (proposalId) => {
  try {
    const res = await api.get(`/api/proposals/${proposalId}/field-remarks`)
    remarksResponse.value = res.data || null
    remarksError.value = ''
  } catch (err) {
    console.error(err)
    remarksResponse.value = null
    remarksError.value = 'Unable to load the existing remarks.'
  }
}

const onRemarkSaved = (item) => {
  const current = { ...(remarksResponse.value || { activeRound: 1, rounds: [] }) }
  const rounds = Array.isArray(current.rounds) ? [...current.rounds] : []
  let activeIndex = rounds.findIndex((round) => round.active)
  if (activeIndex < 0) {
    rounds.unshift({ round: current.activeRound || 1, active: true, remarks: [] })
    activeIndex = 0
    current.activeRound = rounds[0].round
  }
  const remarks = [...(rounds[activeIndex].remarks || [])]
  const existing = remarks.findIndex((row) => row.id === item.id || row.fieldKey === item.fieldKey)
  if (existing >= 0) remarks[existing] = item
  else remarks.push(item)
  rounds[activeIndex] = { ...rounds[activeIndex], remarks }
  remarksResponse.value = { ...current, rounds }
}

const onRemarkRemoved = (remarkId) => {
  if (!remarksResponse.value) return
  const rounds = (remarksResponse.value.rounds || []).map((round) => ({
    ...round,
    remarks: (round.remarks || []).filter((row) => row.id !== remarkId),
  }))
  remarksResponse.value = { ...remarksResponse.value, rounds }
}

const downloadFile = (fileName) => {
  if (fileName) window.open(`/api/files/${encodeURIComponent(fileName)}`, '_blank')
}

// The TypeScript generic that used to be on this line is not valid JavaScript: it parses as
// a comparison against an undefined type name and threw a ReferenceError while this
// component was being set up, so the page rendered nothing at all.
const proposal = ref({
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
  sdg: [],
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

/**
 * The proposal this page shows, taken from the route so a refresh, a direct link, the
 * browser's Back/Forward and a move to another proposal all resolve the same way. The id is
 * a query parameter on this route (`/rec-prop?id=...`); the params form is accepted too.
 */
const routeProposalId = computed(() => route.params.id ?? route.query.id ?? null)

/** The route carries no id at all. */
const missingId = ref(false)

/** The id was given, but no such proposal is readable by this account. */
const notFound = ref(false)

/**
 * Reads the proposal the route points at. Every outcome is a state the template shows:
 * loading, a failure with a retry, a missing id, a proposal that is not there, or the
 * proposal itself - never an empty page.
 */
const loadProposal = async () => {
  const proposalId = routeProposalId.value
  error.value = ''
  notFound.value = false

  if (!proposalId) {
    missingId.value = true
    loading.value = false
    proposal.value = { id: null }
    return
  }

  missingId.value = false
  loading.value = true
  try {
    const res = await api.get(`/api/proposals/${proposalId}`)
    const data = res.data || null

    if (!data || data.id === null || data.id === undefined) {
      proposal.value = { id: null }
      notFound.value = true
      return
    }

    // Keep the raw API response: the field registry resolves the real entity/DTO keys.
    proposal.value = data
    await loadFieldRemarks(proposalId)
  } catch (err) {
    console.error(err)
    proposal.value = { id: null }
    if (err?.response?.status === 404) notFound.value = true
    else error.value = 'Failed to load proposal details.'
  } finally {
    loading.value = false
  }
}

// The route is the single source of truth: refreshing the page, opening a link, moving
// Back/Forward between proposals and clicking VIEW for a different proposal all reload
// from the id. Nothing is kept in temporary component state.
watch(routeProposalId, () => {
  loadProposal()
}, { immediate: true })

/** The summary rows the shared sidebar shows, from the values this page already loaded. */
const quickRows = computed(() => [
  { label: 'Project Leader', value: proposal.value.projectLeader || 'N/A' },
  { label: 'College', value: proposal.value.college || 'N/A' },
  { label: 'Duration', value: proposal.value.duration ? proposal.value.duration + ' months' : 'N/A' },
  { label: 'Research Type', value: proposal.value.researchType || 'N/A' },
])

/**
 * Back always works: it returns to the previous page when there is one, and otherwise falls
 * back to the REC proposal list instead of leaving the reader on a dead page. This used to
 * be declared inside onMounted, so the template's @click had no handler at all.
 */
const goBack = () => {
  if (typeof window !== 'undefined' && window.history.length > 1) {
    window.history.back()
    return
  }
  router.push('/assigned-proposals')
}
</script>

<style scoped>
/* The shell already pads the content area and supplies the typeface. The 900px cap squeezed
   the summary column until it dropped under the field sections, and the local font stack made
   this one page read differently from the rest of the application. */
.proposal-view {
  width: 100%;
  line-height: 1.6;
}

/* HEADER */
.proposal-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

/* Layout only; .back-btn is a neutral cancel-style button (assets/actions.css). */
.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s;
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

/* Layout only; the primary colour comes from assets/actions.css. */
.btn-primary {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
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