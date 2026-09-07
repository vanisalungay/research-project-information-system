<template>
  <section class="submission-history">
    <div class="sh-header">
      <h3>Submission &amp; Revision History</h3>
      <span v-if="versions.length" class="sh-count">{{ versions.length }} version{{ versions.length > 1 ? 's' : '' }}</span>
    </div>

    <p v-if="loading" class="sh-muted">Loading history...</p>
    <p v-else-if="error" class="sh-muted">{{ error }}</p>
    <p v-else-if="versions.length === 0" class="sh-muted">
      No submission history recorded yet.
    </p>

    <div v-else class="sh-timeline">
      <div
        v-for="(v, index) in versions"
        :key="v.id || v.versionNumber"
        class="sh-item"
        :class="{ 'sh-item-current': isCurrent(v) }"
      >
        <div class="sh-rail">
          <div class="sh-dot"></div>
          <div v-if="index !== versions.length - 1" class="sh-line"></div>
        </div>

        <div class="sh-card">
          <div class="sh-card-top">
            <div class="sh-version-label">{{ versionLabel(v) }}</div>
            <span v-if="isCurrent(v)" class="sh-current-badge">Current</span>
          </div>

          <div class="sh-meta-grid">
            <div class="sh-meta">
              <span class="sh-meta-label">Document ID</span>
              <span class="sh-meta-value">{{ v.documentId || '—' }}</span>
            </div>
            <div class="sh-meta">
              <span class="sh-meta-label">Status</span>
              <span class="sh-meta-value">{{ v.status || '—' }}</span>
            </div>
            <div class="sh-meta">
              <span class="sh-meta-label">Submitted By</span>
              <span class="sh-meta-value">{{ v.submittedByName || '—' }}</span>
            </div>
            <div class="sh-meta">
              <span class="sh-meta-label">Submission Date</span>
              <span class="sh-meta-value">{{ formatDate(v.submissionDate) }}</span>
            </div>
          </div>

          <div v-if="v.revisionRemarks" class="sh-remarks">
            <span class="sh-meta-label">Revision Remarks</span>
            <p>{{ v.revisionRemarks }}</p>
          </div>

          <button class="sh-btn" @click="toggleDetails(v)">
            {{ activeVersion === v.versionNumber ? 'Hide Details' : 'View Details' }}
          </button>

          <div v-if="activeVersion === v.versionNumber" class="sh-snapshot">
            <p v-if="loadingDetails" class="sh-muted">Loading version details...</p>
            <p v-else-if="detailsError" class="sh-muted">{{ detailsError }}</p>
            <template v-else-if="details">
              <div class="sh-snapshot-grid">
                <div v-for="f in displayFields" :key="f.key" class="sh-snapshot-item">
                  <span class="sh-meta-label">{{ f.label }}</span>
                  <span class="sh-meta-value">{{ displayValue(details[f.key]) }}</span>
                </div>
              </div>

              <div class="sh-files">
                <span class="sh-meta-label">Submitted Files</span>
                <ul>
                  <li v-for="f in fileFields" :key="f.key">
                    <span class="sh-file-name">{{ f.label }}:</span>
                    {{ details[f.key] || 'No file' }}
                  </li>
                </ul>
              </div>

              <div
                v-for="section in collectionSections"
                :key="section.key"
                class="sh-collection"
              >
                <span class="sh-meta-label">{{ section.title }}</span>
                <p v-if="sectionRows(section).length === 0" class="sh-muted">None</p>
                <div v-else class="sh-table-wrap">
                  <table class="sh-table">
                    <thead>
                      <tr>
                        <th v-for="col in section.columns" :key="col.key">{{ col.label }}</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="(row, ri) in sectionRows(section)" :key="ri">
                        <td v-for="col in section.columns" :key="col.key">{{ formatCell(row[col.key]) }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </div>
            </template>
            <p v-else class="sh-muted">The content for this version was not recorded.</p>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import api from '@/utils/api'

const props = defineProps({
  proposalId: { type: [Number, String], required: true }
})

const versions = ref([])
const loading = ref(false)
const error = ref(null)

const activeVersion = ref(null)
const details = ref(null)
const loadingDetails = ref(false)
const detailsError = ref(null)

const displayFields = [
  { key: 'proposalCode', label: 'Proposal Code' },
  { key: 'programTitle', label: 'Program Title' },
  { key: 'projectTitle', label: 'Project Title' },
  { key: 'projectLeader', label: 'Project Leader' },
  { key: 'college', label: 'College' },
  { key: 'address', label: 'Address' },
  { key: 'cooperatingAgencies', label: 'Cooperating Agencies' },
  { key: 'researchType', label: 'Research Type' },
  { key: 'duration', label: 'Duration' },
  { key: 'startDate', label: 'Start Date' },
  { key: 'endDate', label: 'End Date' },
  { key: 'innovationGoals', label: 'Innovation Goals' },
  { key: 'sectorRelevance', label: 'Sector Relevance' },
  { key: 'sdg', label: 'SDG' },
  { key: 'executiveSummary', label: 'Executive Summary' },
  { key: 'rationale', label: 'Rationale' },
  { key: 'framework', label: 'Theoretical Framework' },
  { key: 'objectivesGeneral', label: 'General Objective' },
  { key: 'objectivesSpecific', label: 'Specific Objectives' },
  { key: 'review', label: 'Review of Literature' },
  { key: 'methodology', label: 'Methodology' },
  { key: 'technologyTrl', label: 'Technology TRL' },
  { key: 'outputs', label: 'Expected Outputs' },
  { key: 'outcomes', label: 'Potential Outcomes' },
  { key: 'impactEconomic', label: 'Economic Impact' },
  { key: 'impactSocial', label: 'Social / Ethical Impact' },
  { key: 'beneficiaries', label: 'Target Beneficiaries' },
  { key: 'sustainability', label: 'Sustainability Plan' },
  { key: 'gadScore', label: 'GAD Score' },
  { key: 'risks', label: 'Risks & Assumptions' },
  { key: 'referencesText', label: 'Literature Cited / References' },
  { key: 'otherProjectsNumber', label: 'Number of Other Projects' }
]

const fileFields = [
  { key: 'reviewFileName', label: 'Review of Literature' },
  { key: 'roadmapFileName', label: 'Technology Roadmap' },
  { key: 'beneficiariesFileName', label: 'Beneficiaries' },
  { key: 'gadFileName', label: 'GAD Score' }
]

const collectionSections = [
  {
    key: 'sites',
    title: 'Sites',
    columns: [
      { key: 'country', label: 'Country' },
      { key: 'region', label: 'Region' },
      { key: 'province', label: 'Province' },
      { key: 'district', label: 'District' },
      { key: 'municipality', label: 'Municipality' },
      { key: 'barangay', label: 'Barangay' }
    ]
  },
  {
    key: 'logFrames',
    title: 'Logical Framework',
    columns: [
      { key: 'outcome', label: 'Outcome' },
      { key: 'output', label: 'Output' }
    ]
  },
  {
    key: 'personnel',
    title: 'Personnel Requirements',
    columns: [
      { key: 'position', label: 'Position' },
      { key: 'timeDevoted', label: 'Time Devoted' },
      { key: 'responsibilities', label: 'Responsibilities' }
    ]
  },
  {
    key: 'budget',
    title: 'Line-Item Budget',
    columns: [
      { key: 'agency', label: 'Agency' },
      { key: 'ps', label: 'PS' },
      { key: 'mooe', label: 'MOOE' },
      { key: 'eo', label: 'EO' },
      { key: 'total', label: 'Total' }
    ]
  },
  {
    key: 'otherProjects',
    title: 'Other Projects',
    columns: [
      { key: 'title', label: 'Project Title' },
      { key: 'agency', label: 'Funding Agency' },
      { key: 'involvement', label: 'Involvement' }
    ]
  },
  {
    key: 'priorityAgendas',
    title: 'Priority Agendas',
    columns: [
      { key: 'agendaKey', label: 'Agenda' },
      { key: 'selected', label: 'Selected' },
      { key: 'value', label: 'Value' }
    ]
  },
  {
    key: 'limitations',
    title: 'Limitations',
    columns: [
      { key: 'limitation', label: 'Limitation' },
      { key: 'remarks', label: 'Remarks' }
    ]
  }
]

function ordinal(n) {
  const map = {
    1: 'First', 2: 'Second', 3: 'Third', 4: 'Fourth', 5: 'Fifth',
    6: 'Sixth', 7: 'Seventh', 8: 'Eighth', 9: 'Ninth', 10: 'Tenth'
  }
  if (map[n]) return map[n]
  const s = ['th', 'st', 'nd', 'rd']
  const v = n % 100
  return n + (s[(v - 20) % 10] || s[v] || s[0])
}

function versionLabel(v) {
  const n = v.versionNumber
  if (!n || n <= 1) return `Version ${n || 1} – Original Submission`
  const revisedOn = v.revisionDate ? ` — Revised on ${formatDate(v.revisionDate)}` : ''
  return `Version ${n} – ${ordinal(n - 1)} Revision${revisedOn}`
}

function isCurrent(v) {
  return Boolean(v.isCurrent)
}

function formatDate(dateString) {
  if (!dateString) return '—'
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'short',
    day: 'numeric'
  })
}

function isBlankValue(value) {
  return value === null || value === undefined || value === ''
}

function isBlankRow(row, columns) {
  return columns.every((col) => isBlankValue(row[col.key]))
}

function sectionRows(section) {
  const rows = details.value?.[section.key] || []
  return rows.filter((row) => !isBlankRow(row, section.columns))
}

function displayValue(value) {
  if (value === null || value === undefined || value === '') return 'Not provided'
  return value
}

function formatCell(value) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'boolean') return value ? 'Yes' : 'No'
  return value
}

async function fetchVersions() {
  if (!props.proposalId) return
  loading.value = true
  error.value = null
  try {
    const res = await api.get(`/api/proposals/${props.proposalId}/versions`)
    versions.value = res.data || []
  } catch (err) {
    console.error(err)
    error.value = 'Failed to load submission history.'
    versions.value = []
  } finally {
    loading.value = false
  }
}

async function toggleDetails(v) {
  if (activeVersion.value === v.versionNumber) {
    activeVersion.value = null
    details.value = null
    return
  }
  activeVersion.value = v.versionNumber
  details.value = null
  detailsError.value = null
  loadingDetails.value = true
  try {
    const res = await api.get(`/api/proposals/${props.proposalId}/versions/${v.versionNumber}`)
    details.value = res.data?.details || null
  } catch (err) {
    console.error(err)
    detailsError.value = 'Failed to load this version.'
  } finally {
    loadingDetails.value = false
  }
}

watch(() => props.proposalId, fetchVersions, { immediate: true })
</script>

<style scoped>
.submission-history {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 20px;
  margin-top: 20px;
}

.sh-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.sh-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.sh-count {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 3px 10px;
  border-radius: 12px;
}

.sh-muted {
  color: #94a3b8;
  font-size: 13px;
  font-style: italic;
}

.sh-timeline {
  display: flex;
  flex-direction: column;
}

.sh-item {
  display: flex;
  gap: 12px;
}

.sh-rail {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex-shrink: 0;
}

.sh-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #cbd5e1;
  border: 2px solid #fff;
  box-shadow: 0 0 0 2px #cbd5e1;
  margin-top: 6px;
}

.sh-item-current .sh-dot {
  background: #4f46e5;
  box-shadow: 0 0 0 2px #4f46e5;
}

.sh-line {
  width: 2px;
  flex: 1;
  background: #e2e8f0;
  margin: 4px 0;
}

.sh-card {
  flex: 1;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 14px;
  margin-bottom: 16px;
}

.sh-item-current .sh-card {
  border-color: #a5b4fc;
  background: #f8faff;
}

.sh-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 10px;
}

.sh-version-label {
  font-size: 14px;
  font-weight: 700;
  color: #1e293b;
}

.sh-current-badge {
  background: #dcfce7;
  color: #166534;
  font-size: 11px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.sh-meta-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.sh-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sh-meta-label {
  font-size: 11px;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.sh-meta-value {
  font-size: 13px;
  color: #1e293b;
  font-weight: 500;
}

.sh-remarks {
  margin-top: 10px;
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  border-radius: 4px;
  padding: 8px 12px;
}

.sh-remarks p {
  margin: 4px 0 0;
  font-size: 13px;
  color: #475569;
  font-style: italic;
  line-height: 1.4;
}

.sh-btn {
  margin-top: 10px;
  background: #fff;
  border: 1px solid #d1d5db;
  color: #475569;
  font-size: 12px;
  font-weight: 600;
  padding: 6px 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}

.sh-btn:hover {
  border-color: #4f46e5;
  color: #4f46e5;
}

.sh-snapshot {
  margin-top: 12px;
  border-top: 1px dashed #e2e8f0;
  padding-top: 12px;
}

.sh-snapshot-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.sh-snapshot-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sh-snapshot-item .sh-meta-value {
  white-space: pre-wrap;
  line-height: 1.5;
}

.sh-files {
  margin-top: 12px;
}

.sh-files ul {
  list-style: none;
  padding: 0;
  margin: 6px 0 0;
}

.sh-files li {
  font-size: 13px;
  color: #475569;
  padding: 4px 0;
}

.sh-file-name {
  font-weight: 600;
  color: #334155;
}

.sh-collection {
  margin-top: 16px;
}

.sh-collection .sh-meta-label {
  display: block;
  margin-bottom: 6px;
}

.sh-table-wrap {
  overflow-x: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}

.sh-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.sh-table th,
.sh-table td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid #f1f5f9;
  white-space: normal;
  word-break: break-word;
  vertical-align: top;
}

.sh-table th {
  background: #f8fafc;
  color: #475569;
  font-weight: 600;
  white-space: nowrap;
}

.sh-table tbody tr:last-child td {
  border-bottom: none;
}

@media (max-width: 640px) {
  .sh-meta-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .sh-snapshot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
