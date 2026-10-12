<template>
  <div class="qpr-form-page" :class="{ 'qpr-form-page--terminal': isTerminalStage }">
    <div class="page-header">
      <button class="back-btn" @click="$router.back()">← Back</button>
      <div class="page-header-text">
        <h1>{{ pageTitle }}</h1>
        <p class="subtitle" :class="{ 'subtitle--terminal': isTerminalStage }">
          {{ project?.projectTitle || '' }}
        </p>
      </div>
    </div>

    <div v-if="loading" class="state">Loading...</div>
    <div v-else-if="error" class="state error">{{ error }}</div>
    <div v-else-if="!eligible" class="state notice">
      This project is not yet approved for implementation or does not have an issued Special Order (SO).
      Reporting is only available for approved, SO-issued projects.
    </div>

    <!-- Terminal Report stage: upload only, no manual data entry. -->
    <template v-else-if="isTerminalStage">
      <div class="terminal-card">
        <h2 class="terminal-card-title">Terminal Report</h2>
        <p class="terminal-card-desc">
          Submit your completed Terminal Report for this project. Use the official ORPS Terminal Report
          Format as your guide — you do not need to re-type its contents into RPIS.
        </p>

        <!-- Step 1: get the official format -->
        <div class="terminal-step">
          <span class="terminal-step-badge">1</span>
          <div class="terminal-step-body">
            <h3 class="terminal-step-title">Get the official format</h3>
            <div class="terminal-format-actions">
              <a class="terminal-format-btn" :href="terminalFormatUrl" target="_blank" rel="noopener">
                <svg class="terminal-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8S1 12 1 12z"/><circle cx="12" cy="12" r="3"/></svg>
                View official format
              </a>
              <a
                class="terminal-format-btn terminal-format-btn--download"
                :href="terminalFormatUrl"
                :download="terminalFormatName"
              >
                <svg class="terminal-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
                Download format
              </a>
            </div>
          </div>
        </div>

        <!-- Step 2: upload the completed report -->
        <div class="terminal-step">
          <span class="terminal-step-badge">2</span>
          <div class="terminal-step-body">
            <h3 class="terminal-step-title">
              Upload your completed report <span class="req">*</span>
            </h3>

            <div
              class="terminal-dropzone"
              @click="openTerminalFilePicker"
              @dragover.prevent
              @drop.prevent="onTerminalDrop"
            >
              <span class="terminal-upload-icon-box">
                <svg class="terminal-upload-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/></svg>
              </span>
              <p class="terminal-dropzone-text">
                Drag and drop your file here, or <span class="terminal-browse">browse</span>
              </p>
              <p class="terminal-dropzone-hint">Accepted: PDF, DOC, DOCX · Maximum size: 10 MB</p>
            </div>
            <input
              ref="terminalFileInput"
              type="file"
              accept=".pdf,.doc,.docx"
              @change="onTerminalFile"
              hidden
            />
            <p v-if="terminalUploadError" class="terminal-upload-error">{{ terminalUploadError }}</p>

            <!-- Selected file (below the drop zone), or the file already stored
                 on the server when an existing report is reopened. -->
            <div
              v-if="terminalFileItem"
              class="terminal-file-item"
              :class="{ 'terminal-file-item--uploaded': terminalFileItem.status === 'Uploaded' }"
            >
              <span class="terminal-file-icon" :class="`terminal-file-icon--${terminalFileItem.kind}`">
                {{ terminalFileItem.typeLabel }}
              </span>
              <div class="terminal-file-info">
                <span class="terminal-file-name" :title="terminalFileItem.name">
                  {{ terminalFileItem.name }}
                </span>
                <span class="terminal-file-sub">
                  <span>
                    {{ terminalFileItem.typeLabel }}
                    <template v-if="terminalFileItem.sizeLabel">· {{ terminalFileItem.sizeLabel }}</template>
                  </span>
                  <span
                    class="terminal-file-status"
                    :class="{ 'terminal-file-status--uploaded': terminalFileItem.status === 'Uploaded' }"
                  >
                    {{ terminalFileItem.status }}
                  </span>
                </span>
              </div>
              <button
                v-if="terminalFileItem.removable"
                type="button"
                class="terminal-file-remove"
                aria-label="Remove selected file"
                @click.stop="removeTerminalFile"
              >
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
              </button>
            </div>
          </div>
        </div>

        <hr class="terminal-divider" />

        <div class="terminal-actions">
          <button class="terminal-cancel" @click="$router.back()">Cancel</button>
          <button class="terminal-submit" :disabled="saving" @click="submitTerminal">
            <svg class="terminal-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
            {{ saving ? 'Submitting...' : 'Submit Terminal Report' }}
          </button>
        </div>
      </div>
      <p v-if="saveError" class="state error">{{ saveError }}</p>
    </template>

    <!-- Quarterly Progress Report stages (1-3): QPR form + Project Workplan. -->
    <template v-else>
      <QuarterlyReportForm ref="qprFormRef" :report="report" :saving="saving" :lock-period="true" />
      <div class="doc-card">
        <h3>Project Workplan <span class="req">*</span></h3>
        <p class="doc-desc">
          Complete the MSUN Internally Funded Project Workplan below. It is saved and submitted together with
          the Quarterly Progress Report using the <strong>Save Draft</strong> /
          <strong>Submit Report</strong> buttons at the bottom of this page.
        </p>

        <!-- Interactive workplan form, persisted as structured data with the report. -->
        <ProjectWorkplanForm v-model="workplan" />

        <p v-if="existingWorkplanName" class="existing-file">
          A workplan file previously uploaded for this report is preserved: {{ existingWorkplanName }}
        </p>
      </div>

      <!-- Stage actions, below the entire Project Workplan form. -->
      <div class="workplan-actions">
        <button class="btn-secondary" @click="$router.back()">Cancel</button>
        <div class="workplan-actions-right">
          <button class="btn-draft" :disabled="saving" @click="saveQuarterly('DRAFT', qprPayload())">
            Save Draft
          </button>
          <button class="btn-submit" :disabled="saving" @click="saveQuarterly('SUBMITTED', qprPayload())">
            {{ saving ? 'Saving...' : 'Submit Report' }}
          </button>
        </div>
      </div>
      <p v-if="saveError" class="state error">{{ saveError }}</p>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/utils/api'
import QuarterlyReportForm from '@/components/QuarterlyReportForm.vue'
import ProjectWorkplanForm from '@/components/ProjectWorkplanForm.vue'

// Official ORPS Terminal Report format shipped with the app (public/templates).
const TERMINAL_FORMAT_URL = '/templates/ORPS-Inhouse-Terminal-Report-Format-2026-Rev01.pdf'

const route = useRoute()
const router = useRouter()
const proposalId = computed(() => route.params.proposalId)
const reportId = computed(() => route.params.reportId)
const isEdit = computed(() => !!reportId.value)

const project = ref(null)
const report = ref(null)
const qprFormRef = ref(null)
const nextStage = ref(null)
const loading = ref(true)
const error = ref('')
const saveError = ref('')
const saving = ref(false)

const terminalFile = ref(null)
const terminalFileInput = ref(null)
const terminalUploadError = ref('')

// The selected file must be a real, named File. A browser File object never
// survives a page reload, so a reopened report falls back to the file already
// stored on the server (see terminalFileItem below).
const hasTerminalFile = computed(() => !!(terminalFile.value && terminalFile.value.name))

// Structured Project Workplan (reporting stages 1-3). Bound to the interactive
// workplan form and persisted through the quarterly report API (workplan_json).
const WORKPLAN_MONTHS = 12
const MIN_WORKPLAN_ROWS = 3

const workplan = reactive({
  asOf: '',
  projectTitle: '',
  projectLeader: '',
  projectDuration: '',
  objectives: [],
  remarks: '',
})

function emptyWorkplanObjective() {
  return { objective: '', months: Array(WORKPLAN_MONTHS).fill(''), total: '' }
}

function normalizeWorkplanObjective(o) {
  const src = Array.isArray(o?.months) ? o.months : []
  const months = src.slice(0, WORKPLAN_MONTHS).map((m) => (typeof m === 'string' ? m : ''))
  while (months.length < WORKPLAN_MONTHS) months.push('')
  return { objective: o?.objective || '', months, total: o?.total || '' }
}

// Seed from the saved workplan when present, otherwise from the proposal. Saved
// values always win so reopening a report restores what the proponent entered.
function initWorkplan(saved, proposal) {
  workplan.asOf = saved?.asOf || ''
  workplan.projectTitle = saved?.projectTitle || proposal?.projectTitle || ''
  workplan.projectLeader = saved?.projectLeader || proposal?.projectLeader || ''
  workplan.projectDuration = saved?.projectDuration || proposal?.duration || ''
  const objectives = Array.isArray(saved?.objectives) ? saved.objectives.map(normalizeWorkplanObjective) : []
  while (objectives.length < MIN_WORKPLAN_ROWS) objectives.push(emptyWorkplanObjective())
  workplan.objectives = objectives
  workplan.remarks = saved?.remarks || ''
}

function serializeWorkplan() {
  return {
    asOf: workplan.asOf || '',
    projectTitle: workplan.projectTitle || '',
    projectLeader: workplan.projectLeader || '',
    projectDuration: workplan.projectDuration || '',
    objectives: workplan.objectives.map((o) => ({
      objective: o.objective || '',
      months: (o.months || []).slice(0, WORKPLAN_MONTHS).map((m) => m || ''),
      total: o.total || '',
    })),
    remarks: workplan.remarks || '',
  }
}

function hasWorkplanContent() {
  return workplan.objectives.some((o) => (o.objective || '').trim() !== '')
}

// The stage action buttons live below the Project Workplan form, so the
// Quarterly Progress Report payload is read from the QPR form component's
// exposed buildPayload() method.
function qprPayload() {
  return qprFormRef.value?.buildPayload() || {}
}

const eligible = computed(() => {
  const p = project.value
  if (!p) return false
  const hasSo = p.soNumber && p.soNumber.trim() !== ''
  const approved = p.status === 'APPROVED' || p.status === 'RELEASED'
  return hasSo && approved
})

// Stage of the report being edited, or (for a new report) the next required
// stage reported by the backend.
const stageType = computed(() =>
  isEdit.value
    ? report.value?.reportType || 'QUARTERLY_PROGRESS'
    : nextStage.value?.reportType || 'QUARTERLY_PROGRESS',
)
const stagePeriod = computed(() => (isEdit.value ? report.value?.period || '' : nextStage.value?.period || ''))
const isTerminalStage = computed(() => stageType.value === 'TERMINAL_REPORT')

const pageTitle = computed(() => {
  if (isTerminalStage.value) return isEdit.value ? 'Terminal Report' : 'Submit Terminal Report'
  return isEdit.value
    ? 'Edit Quarterly Progress Report'
    : `New Quarterly Progress Report (${stagePeriod.value})`
})

const terminalFormatUrl = TERMINAL_FORMAT_URL
const terminalFormatName = 'ORPS-Inhouse-Terminal-Report-Format-2026-Rev01.pdf'

const existingWorkplanName = computed(() => report.value?.workplanFileName || '')
const existingTerminalName = computed(() => report.value?.terminalReportFileName || '')

async function fetchData() {
  loading.value = true
  error.value = ''
  try {
    const p = await api.get(`/api/proposals/${proposalId.value}`)
    project.value = p.data
    if (isEdit.value) {
      const r = await api.get(`/api/quarterly-reports/${reportId.value}`)
      report.value = r.data
      initWorkplan(r.data.workplan, p.data)
    } else {
      const n = await api.get(`/api/quarterly-reports/next-stage/${proposalId.value}`)
      nextStage.value = n.data
      if (n.data?.allStagesComplete) {
        error.value = 'All required reports for this project have already been submitted.'
        return
      }
      report.value = {
        period: n.data?.period || 'Q1',
        reportType: n.data?.reportType || 'QUARTERLY_PROGRESS',
        projectTitle: p.data.projectTitle || '',
        projectLeader: p.data.projectLeader || '',
        projectDuration: p.data.duration || '',
        projectStartDate: p.data.startDate || '',
        projectEndDate: p.data.endDate || '',
        objectives: [],
        outputs: [],
      }
      initWorkplan(null, p.data)
    }
  } catch (e) {
    error.value = e.response?.data?.error || 'Failed to load.'
  } finally {
    loading.value = false
  }
}

// Terminal Report upload: the drop zone applies the same rules as the file
// picker (accepted types + the 10 MB limit configured by the application), so
// dragging a file in behaves exactly like choosing it through "browse".
const TERMINAL_ACCEPTED_EXT = ['.pdf', '.doc', '.docx']
const TERMINAL_MAX_BYTES = 10 * 1024 * 1024

function applyTerminalFile(file) {
  if (!file || !file.name) return
  const name = (file.name || '').toLowerCase()
  if (!TERMINAL_ACCEPTED_EXT.some((ext) => name.endsWith(ext))) {
    terminalUploadError.value = 'Unsupported file type. Please upload a PDF, DOC or DOCX file.'
    return
  }
  if (file.size > TERMINAL_MAX_BYTES) {
    terminalUploadError.value = 'The selected file is larger than the 10 MB limit.'
    return
  }
  terminalUploadError.value = ''
  terminalFile.value = file
}

function onTerminalFile(e) {
  applyTerminalFile(e.target.files[0] || null)
  // Reset so re-selecting the same file (after removing it) still fires change.
  e.target.value = ''
}

function onTerminalDrop(e) {
  applyTerminalFile(e.dataTransfer?.files?.[0] || null)
}

function openTerminalFilePicker() {
  terminalFileInput.value?.click()
}

function removeTerminalFile() {
  terminalFile.value = null
  terminalUploadError.value = ''
  // Clear the input so re-selecting the same file fires change again.
  if (terminalFileInput.value) terminalFileInput.value.value = ''
}

/** File-type bucket used for the preview icon (pdf / doc / generic). */
function terminalFileKind(name) {
  const ext = (name || '').toLowerCase().split('.').pop()
  if (ext === 'pdf') return 'pdf'
  if (ext === 'doc' || ext === 'docx') return 'doc'
  return 'file'
}

/** Extension shown on the preview icon, e.g. "PDF" or "DOCX". */
function terminalFileTypeLabel(name) {
  const lower = (name || '').toLowerCase()
  const ext = lower.split('.').pop()
  if (!ext || ext === lower) return 'FILE'
  return ext.toUpperCase()
}

/** Human readable file size in KB or MB. */
function formatFileSize(bytes) {
  if (typeof bytes !== 'number' || Number.isNaN(bytes) || bytes < 0) return ''
  if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${Math.max(1, Math.round(bytes / 1024))} KB`
}

// Preview item shown below the drop zone: the locally selected file while it is
// only "Selected" (upload happens on submit), or the file already stored on the
// server ("Uploaded") when an existing report is reopened. Only one item is ever
// shown, so selecting a replacement never produces duplicates.
const terminalFileItem = computed(() => {
  if (hasTerminalFile.value) {
    const file = terminalFile.value
    return {
      name: file.name,
      kind: terminalFileKind(file.name),
      typeLabel: terminalFileTypeLabel(file.name),
      sizeLabel: formatFileSize(file.size),
      status: 'Selected',
      removable: true,
    }
  }
  if (existingTerminalName.value) {
    return {
      name: existingTerminalName.value,
      kind: terminalFileKind(existingTerminalName.value),
      typeLabel: terminalFileTypeLabel(existingTerminalName.value),
      // The size of a previously uploaded file is not returned by the API.
      sizeLabel: '',
      status: 'Uploaded',
      removable: false,
    }
  }
  return null
})

// Ensure a DRAFT report row exists for the current stage and return its id. The
// backend derives/validates the stage, so retries never create duplicate rows.
async function ensureDraft(payload, type, period) {
  const body = {
    ...(payload || {}),
    proposalId: Number(proposalId.value),
    reportType: type,
    period,
    status: 'DRAFT',
  }
  const existingId = reportId.value || report.value?.id
  if (existingId) {
    const res = await api.put(`/api/quarterly-reports/${existingId}`, body)
    report.value = res.data
    return res.data.id
  }
  const res = await api.post('/api/quarterly-reports', body)
  report.value = res.data
  return res.data.id
}

async function uploadDocs(id, type) {
  // Only the Terminal Report stage still uses a file upload; the Project
  // Workplan (stages 1-3) is entered in the structured workplan form instead.
  if (type !== 'TERMINAL_REPORT' || !terminalFile.value) return
  const fd = new FormData()
  fd.append('terminalReport', terminalFile.value)
  const res = await api.post(`/api/quarterly-reports/${id}/documents`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  report.value = res.data
}

async function submitStage(payload, type, period) {
  if (saving.value) return
  // Frontend guard (the backend validates this too). A stage cannot be
  // submitted until its required document has been attached.
  if (type === 'TERMINAL_REPORT' && !terminalFile.value && !existingTerminalName.value) {
    saveError.value = 'Please attach the completed Terminal Report file before submitting.'
    return
  }
  if (type === 'QUARTERLY_PROGRESS' && !hasWorkplanContent()) {
    saveError.value = 'Please complete the Project Workplan form (at least one objective) before submitting.'
    return
  }
  saving.value = true
  saveError.value = ''
  try {
    const id = await ensureDraft(payload, type, period)
    await uploadDocs(id, type)
    const res = await api.put(`/api/quarterly-reports/${id}`, {
      ...(payload || report.value || {}),
      proposalId: Number(proposalId.value),
      reportType: type,
      period,
      status: 'SUBMITTED',
    })
    report.value = res.data
    router.push(`/quarterly-reports/${proposalId.value}`)
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Failed to submit the report.'
  } finally {
    saving.value = false
  }
}

async function saveQuarterly(status, payload) {
  // The Project Workplan is part of the same stage submission as the QPR.
  const body = { ...payload, workplan: serializeWorkplan() }
  if (status === 'SUBMITTED') {
    await submitStage(body, 'QUARTERLY_PROGRESS', stagePeriod.value)
    return
  }
  if (saving.value) return
  saving.value = true
  saveError.value = ''
  try {
    await ensureDraft(body, 'QUARTERLY_PROGRESS', stagePeriod.value)
    router.push(`/quarterly-reports/${proposalId.value}`)
  } catch (e) {
    saveError.value = e.response?.data?.error || 'Failed to save report.'
  } finally {
    saving.value = false
  }
}

async function submitTerminal() {
  await submitStage({}, 'TERMINAL_REPORT', 'TERMINAL')
}

onMounted(fetchData)
</script>

<style scoped>
.qpr-form-page {
  padding: 24px;
  background: #f8fafc;
  min-height: 100vh;
  max-width: 1100px;
  margin: 0 auto;
}
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}
.page-header h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
}
.subtitle {
  margin: 4px 0 0;
  color: #64748b;
  font-size: 14px;
}
.page-header-text {
  min-width: 0;
}
/* Submit Terminal Report page: use the available content width. The page keeps
   the app's standard outer padding, so the card stays inset from the edges of
   the content area on every viewport (no fixed narrow width). */
.qpr-form-page--terminal {
  max-width: 1400px;
}
.subtitle--terminal {
  margin-top: 3px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: #1c2145;
}
.back-btn {
  background: #fff;
  color: #0f172a;
  border: 1px solid #cbd5e1;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-weight: 600;
  flex-shrink: 0;
}
.state {
  background: #fff;
  border: 1px solid #e2e8f0;
  padding: 24px;
  border-radius: 8px;
  text-align: center;
  color: #64748b;
}
.state.error {
  color: #dc2626;
}
.state.notice {
  color: #92400e;
  background: #fffbeb;
  border-color: #fde68a;
}
.doc-card {
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 20px;
  margin-top: 16px;
}
.doc-card h2 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}
.doc-card h3 {
  margin: 0 0 6px;
  font-size: 15px;
  color: #0f172a;
}
.doc-desc {
  color: #64748b;
  font-size: 13px;
  margin: 0 0 12px;
}
.req {
  color: #dc2626;
}
/* ============================================================
   Submit Terminal Report page
   ============================================================ */
.terminal-card {
  background: #fff;
  border: 1px solid #e6e8ee;
  border-radius: 14px;
  padding: 18px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.04);
}
.terminal-card-title {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 700;
  color: #1c2145;
}
.terminal-card-desc {
  margin: 0 0 16px;
  max-width: 760px;
  font-size: 12.5px;
  line-height: 1.5;
  color: #64748b;
}
.terminal-step {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 16px;
}
.terminal-step-badge {
  flex: 0 0 22px;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #1c2145;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}
.terminal-step-body {
  flex: 1;
  min-width: 0;
}
.terminal-step-title {
  margin: 1px 0 8px;
  font-size: 13px;
  font-weight: 700;
  color: #1c2145;
}
.terminal-format-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.terminal-format-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 12px;
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: #1c2145;
  text-decoration: none;
  background: #fff;
  border: 1px solid #c7d2fe;
}
.terminal-format-btn:hover {
  background: #f8fafc;
}
.terminal-format-btn--download {
  background: #eef2ff;
  border-color: transparent;
}
.terminal-format-btn--download:hover {
  background: #e0e7ff;
}
.terminal-icon {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}
.terminal-dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  min-height: 124px;
  padding: 14px;
  box-sizing: border-box;
  background: #f8f8ff;
  border: 1px dashed #c7d2fe;
  border-radius: 12px;
  cursor: pointer;
  text-align: center;
  transition: background 0.15s ease, border-color 0.15s ease;
}
.terminal-dropzone:hover {
  background: #f1f2ff;
  border-color: #a5b4fc;
}
.terminal-upload-icon-box {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: #fef9c3;
  color: #b45309;
}
.terminal-upload-icon {
  width: 18px;
  height: 18px;
}
.terminal-dropzone-text {
  margin: 0;
  font-size: 12.5px;
  color: #1c2145;
}
.terminal-browse {
  font-weight: 600;
  text-decoration: underline;
  color: #1c2145;
}
.terminal-dropzone-hint {
  margin: 0;
  font-size: 11px;
  color: #94a3b8;
}
.terminal-file-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 10px;
  padding: 10px 12px;
  background: #fbfbff;
  border: 1px solid #e2e4f3;
  border-radius: 10px;
}
.terminal-file-item--uploaded {
  background: #fafdfb;
  border-color: #dcefe3;
}
.terminal-file-icon {
  flex-shrink: 0;
  width: 38px;
  height: 38px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  background: #e2e8f0;
  color: #475569;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.2px;
}
.terminal-file-icon--pdf {
  background: #fee2e2;
  color: #b91c1c;
}
.terminal-file-icon--doc {
  background: #dbeafe;
  color: #1d4ed8;
}
.terminal-file-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.terminal-file-name {
  font-size: 12.5px;
  font-weight: 600;
  color: #1c2145;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.terminal-file-sub {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
}
.terminal-file-status {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  background: #eef2ff;
  color: #3730a3;
}
.terminal-file-status--uploaded {
  background: #dcfce7;
  color: #166534;
}
.terminal-file-remove {
  flex-shrink: 0;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 7px;
  background: #fee2e2;
  color: #dc2626;
  cursor: pointer;
}
.terminal-file-remove:hover {
  background: #fecaca;
}
.terminal-file-remove svg {
  width: 14px;
  height: 14px;
}
.terminal-upload-error {
  margin: 8px 0 0;
  font-size: 12px;
  color: #dc2626;
}
.terminal-divider {
  border: none;
  border-top: 1px solid #e5e7eb;
  margin: 18px 0;
}
.terminal-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}
.terminal-cancel {
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #eef2ff;
  color: #1c2145;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.terminal-submit {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 10px 18px;
  border: none;
  border-radius: 8px;
  background: #ffd400;
  color: #1c2145;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}
.terminal-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.existing-file {
  margin: 8px 0 0;
  font-size: 13px;
  color: #166534;
}
.btn-secondary {
  background: #e2e8f0;
  color: #1c2145;
  border: none;
  padding: 12px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-submit {
  background: #ffd400;
  color: #1c2145;
  border: none;
  padding: 12px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
/* Stage action row, placed below the entire Project Workplan form. */
.workplan-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 16px;
}
.workplan-actions-right {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.btn-draft {
  background: #fff;
  color: #1c2145;
  border: 2px solid #1c2145;
  padding: 12px 22px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-draft:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Keep the action buttons visible and usable on smaller screens. */
@media (max-width: 640px) {
  .workplan-actions,
  .workplan-actions-right {
    flex-direction: column;
    align-items: stretch;
  }
  /* Submit Terminal Report page */
  .qpr-form-page--terminal {
    padding: 16px;
  }
  .terminal-card {
    padding: 14px;
  }
  .terminal-actions {
    justify-content: stretch;
  }
  .terminal-cancel,
  .terminal-submit {
    flex: 1 1 auto;
    justify-content: center;
  }
}
</style>
