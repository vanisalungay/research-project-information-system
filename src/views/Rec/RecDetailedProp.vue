<template>
  <div class="proposal-view">
    <!-- HEADER -->
    <div class="proposal-header">
      <button class="back-btn" @click="goBack">← Back</button>
      <div class="header-info">
        <h1>{{ proposal.projectTitle || 'Proposal Details' }}</h1>
        <p class="subtitle">Proposal ID: {{ proposal.documentId || proposal.proposalCode || '—' }}</p>
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
    </template>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import api from '@/utils/api'
import SdgChecklist from '@/components/SdgChecklist.vue'
import ProposalFieldSection from '@/components/ProposalFieldSection.vue'
import { PROPOSAL_FIELD_SECTIONS, buildFieldRemarkIndex } from '@/utils/proposalFields'

const route = useRoute()

const loading = ref(false)
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

    // Keep the raw API response: the field registry resolves the real entity/DTO keys.
    proposal.value = data
    await loadFieldRemarks(proposalId)
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