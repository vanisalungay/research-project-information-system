<template>
  <div class="qr-form">
    <!-- Form title -->
    <div class="qr-title">
      <h2>MSUN INTERNALLY-FUNDED PROJECT</h2>
      <h2>QUARTERLY PROGRESS REPORT</h2>
      <p class="qr-subtitle">As of {{ yearLabel }}</p>
    </div>

    <!-- Reporting period -->
    <div v-if="!readonly" class="qr-period-row">
      <label class="period-field">
        <span>Reporting Quarter</span>
        <select v-model="form.period" class="qr-select">
          <option value="Q1">Q1</option>
          <option value="Q2">Q2</option>
          <option value="Q3">Q3</option>
          <option value="Q4">Q4</option>
        </select>
      </label>
      <label class="period-field">
        <span>Year</span>
        <input v-model.number="form.year" type="number" class="qr-input" />
      </label>
    </div>
    <div v-else class="qr-period-row readonly-period">
      <span>Reporting Period: {{ form.period }} {{ form.year }}</span>
    </div>

    <!-- (1) Project Title -->
    <div class="qr-field-row">
      <label class="qr-field-label">(1) Project Title:</label>
      <input v-model="form.projectTitle" class="qr-input" :readonly="readonly" />
    </div>

    <!-- (2) Project Leader / Gender -->
    <div class="qr-field-row">
      <label class="qr-field-label">(2) Project Leader/Gender:</label>
      <div class="qr-inline-group">
        <input v-model="form.projectLeader" class="qr-input" :readonly="readonly" placeholder="Project Leader" />
        <select v-model="form.leaderGender" class="qr-select" :disabled="readonly">
          <option value="">Select Gender</option>
          <option>Male</option>
          <option>Female</option>
          <option>Prefer not to say</option>
        </select>
      </div>
    </div>

    <!-- (3) Site/s of Implementation -->
    <div class="qr-field-row">
      <label class="qr-field-label">(3) Site/s of Implementation</label>
      <div class="qr-sub-fields">
        <div class="qr-sub-field">
          <span class="qr-sub-label">Base Station:</span>
          <input v-model="form.baseStation" class="qr-input" :readonly="readonly" />
        </div>
        <div class="qr-sub-field">
          <span class="qr-sub-label">Site/s of Implementation:</span>
          <input v-model="form.sitesOfImplementation" class="qr-input" :readonly="readonly" />
        </div>
      </div>
    </div>

    <!-- (4) Duration, (5) Start, (6) End -->
    <div class="qr-field-row">
      <label class="qr-field-label">(4) Project Duration:</label>
      <input v-model="form.projectDuration" class="qr-input" :readonly="readonly" />
    </div>
    <div class="qr-field-row">
      <label class="qr-field-label">(5) Project Start Date:</label>
      <input v-model="form.projectStartDate" type="date" class="qr-input" :readonly="readonly" />
    </div>
    <div class="qr-field-row">
      <label class="qr-field-label">(6) Project End Date:</label>
      <input v-model="form.projectEndDate" type="date" class="qr-input" :readonly="readonly" />
    </div>

    <!-- (7) Major Accomplishments -->
    <div class="qr-section-title">(7) Major Accomplishments</div>

    <!-- A. Objectives table -->
    <div class="qr-sub-section">
      <p class="qr-sub-title">A. Actual accomplishment of the project (via-a-vis the objectives)</p>
      <table class="qr-table objectives-table">
        <thead>
          <tr>
            <th rowspan="2" class="col-objective">OBJECTIVES</th>
            <th colspan="2">ACCOMPLISHMENTS</th>
            <th colspan="2">PERCENTAGE (%)</th>
          </tr>
          <tr>
            <th class="col-sm">TARGET</th>
            <th class="col-sm">ACTUAL</th>
            <th class="col-sm">FOR THE PERIOD</th>
            <th class="col-sm">CUMULATIVE (FROM START)</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in form.objectives" :key="i">
            <td><textarea v-model="row.objective" class="qr-cell-area" :readonly="readonly" rows="3"></textarea></td>
            <td><textarea v-model="row.target" class="qr-cell-area" :readonly="readonly" rows="3"></textarea></td>
            <td><textarea v-model="row.actual" class="qr-cell-area" :readonly="readonly" rows="3"></textarea></td>
            <td><input v-model="row.percentagePeriod" class="qr-cell-input" :readonly="readonly" /></td>
            <td><input v-model="row.percentageCumulative" class="qr-cell-input" :readonly="readonly" /></td>
            <td v-if="!readonly" class="col-action">
              <button class="row-remove" @click="removeObjective(i)" title="Remove row">✕</button>
            </td>
          </tr>
        </tbody>
      </table>
      <button v-if="!readonly" class="qr-add-btn" @click="addObjective">+ Add Objective</button>
    </div>

    <!-- B. Catch-up Plan -->
    <div class="qr-sub-section">
      <p class="qr-sub-title">B. Catch-up Plan</p>
      <textarea v-model="form.catchUpPlan" class="qr-textarea" :readonly="readonly" rows="5"></textarea>
    </div>

    <!-- C. Expected Outputs / 6Ps -->
    <div class="qr-sub-section">
      <p class="qr-sub-title">C. Expected Outputs / 6Ps (Expected Outputs should be measurable.)</p>
      <table class="qr-table outputs-table">
        <thead>
          <tr>
            <th class="col-type"></th>
            <th>EXPECTED OUTPUTS</th>
            <th>ACTUAL OUTPUTS</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in form.outputs" :key="i">
            <td class="output-type">{{ row.outputType }}</td>
            <td><textarea v-model="row.expectedOutput" class="qr-cell-area" :readonly="readonly" rows="2"></textarea></td>
            <td><textarea v-model="row.actualOutput" class="qr-cell-area" :readonly="readonly" rows="2"></textarea></td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- (8) Problems/Concerns -->
    <div class="qr-sub-section">
      <p class="qr-sub-title">(8) Problems/Concerns</p>
      <textarea v-model="form.problemsConcerns" class="qr-textarea" :readonly="readonly" rows="4"></textarea>
    </div>

    <!-- (9) Suggested solutions -->
    <div class="qr-sub-section">
      <p class="qr-sub-title">(9) Suggested solutions to the above concerns</p>
      <textarea v-model="form.suggestedSolutions" class="qr-textarea" :readonly="readonly" rows="4"></textarea>
    </div>

    <!-- Actions (edit mode only) -->
    <div v-if="!readonly" class="qr-actions">
      <button class="btn-secondary" @click="emit('cancel')">Cancel</button>
      <button class="btn-draft" :disabled="saving" @click="emit('save-draft', buildPayload())">Save Draft</button>
      <button class="btn-submit" :disabled="saving" @click="emit('submit', buildPayload())">
        {{ saving ? 'Saving...' : 'Submit Report' }}
      </button>
    </div>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'

const props = defineProps({
  report: { type: Object, default: null },
  readonly: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
})
const emit = defineEmits(['save-draft', 'submit', 'cancel'])

const OUTPUT_TYPES = ['Publications', 'Patents/IP', 'Products', 'People Services', 'Partnerships', 'Policy']

const form = reactive({
  period: 'Q1',
  year: new Date().getFullYear(),
  projectTitle: '',
  projectLeader: '',
  leaderGender: '',
  baseStation: '',
  sitesOfImplementation: '',
  projectDuration: '',
  projectStartDate: '',
  projectEndDate: '',
  catchUpPlan: '',
  problemsConcerns: '',
  suggestedSolutions: '',
  objectives: [],
  outputs: [],
})

const emptyObjective = () => ({ objective: '', target: '', actual: '', percentagePeriod: '', percentageCumulative: '' })
const seedOutputs = () => OUTPUT_TYPES.map((t) => ({ outputType: t, expectedOutput: '', actualOutput: '' }))

function applyReport(r) {
  form.period = r?.period || 'Q1'
  form.year = r?.year || new Date().getFullYear()
  form.projectTitle = r?.projectTitle || ''
  form.projectLeader = r?.projectLeader || ''
  form.leaderGender = r?.leaderGender || ''
  form.baseStation = r?.baseStation || ''
  form.sitesOfImplementation = r?.sitesOfImplementation || ''
  form.projectDuration = r?.projectDuration || ''
  form.projectStartDate = r?.projectStartDate || ''
  form.projectEndDate = r?.projectEndDate || ''
  form.catchUpPlan = r?.catchUpPlan || ''
  form.problemsConcerns = r?.problemsConcerns || ''
  form.suggestedSolutions = r?.suggestedSolutions || ''
  const objectives = Array.isArray(r?.objectives) && r.objectives.length ? r.objectives : [emptyObjective()]
  form.objectives = objectives.map((o) => ({ ...emptyObjective(), ...o }))
  form.outputs = seedOutputs().map((seed) => {
    const found = (r?.outputs || []).find((o) => o.outputType === seed.outputType)
    return { ...seed, ...(found || {}) }
  })
}

watch(() => props.report, (r) => applyReport(r), { immediate: true })

const yearLabel = computed(() => form.year || new Date().getFullYear())

function addObjective() {
  form.objectives.push(emptyObjective())
}
function removeObjective(i) {
  if (form.objectives.length > 1) {
    form.objectives.splice(i, 1)
  }
}

function buildPayload() {
  return {
    period: form.period,
    year: form.year,
    projectTitle: form.projectTitle,
    projectLeader: form.projectLeader,
    leaderGender: form.leaderGender,
    baseStation: form.baseStation,
    sitesOfImplementation: form.sitesOfImplementation,
    projectDuration: form.projectDuration,
    projectStartDate: form.projectStartDate || null,
    projectEndDate: form.projectEndDate || null,
    catchUpPlan: form.catchUpPlan,
    problemsConcerns: form.problemsConcerns,
    suggestedSolutions: form.suggestedSolutions,
    objectives: form.objectives,
    outputs: form.outputs,
  }
}
</script>

<style scoped>
.qr-form {
  background: #fff;
  border: 2px solid #1c2145;
  padding: 24px;
  color: #0f172a;
  font-family: Arial, sans-serif;
}
.qr-title {
  text-align: center;
  border-bottom: 2px solid #1c2145;
  padding-bottom: 12px;
  margin-bottom: 16px;
}
.qr-title h2 {
  margin: 0;
  font-size: 16px;
  letter-spacing: 1px;
  color: #1c2145;
}
.qr-subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #444;
}
.qr-period-row {
  display: flex;
  gap: 20px;
  margin-bottom: 14px;
  padding: 10px 12px;
  background: #f1f5f9;
  border: 1px solid #cbd5e1;
}
.period-field {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 600;
}
.readonly-period {
  font-size: 14px;
  font-weight: 700;
  color: #1c2145;
}
.qr-field-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  border: 1px solid #1c2145;
  padding: 8px 10px;
}
.qr-field-label {
  font-weight: 700;
  font-size: 13px;
  white-space: nowrap;
  min-width: 180px;
}
.qr-input,
.qr-select {
  flex: 1;
  border: none;
  border-bottom: 1px solid #94a3b8;
  padding: 6px 4px;
  font-size: 14px;
  outline: none;
  background: transparent;
}
.qr-input[readonly],
.qr-select:disabled {
  border-bottom: none;
  color: #0f172a;
  background: transparent;
}
.qr-inline-group {
  display: flex;
  gap: 12px;
  flex: 1;
}
.qr-sub-fields {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.qr-sub-field {
  display: flex;
  align-items: center;
  gap: 10px;
}
.qr-sub-label {
  font-weight: 600;
  font-size: 13px;
  white-space: nowrap;
}
.qr-section-title {
  margin: 20px 0 8px;
  font-size: 15px;
  font-weight: 700;
  color: #1c2145;
  text-transform: uppercase;
  border-bottom: 2px solid #1c2145;
  padding-bottom: 4px;
}
.qr-sub-section {
  margin: 14px 0;
}
.qr-sub-title {
  margin: 0 0 8px;
  font-size: 14px;
  font-weight: 700;
}
.qr-table {
  width: 100%;
  border-collapse: collapse;
  margin-bottom: 8px;
}
.qr-table th,
.qr-table td {
  border: 1px solid #1c2145;
  padding: 6px;
  vertical-align: top;
  text-align: left;
}
.qr-table th {
  background: #eef2ff;
  font-size: 12px;
  text-align: center;
  font-weight: 700;
}
.qr-cell-area {
  width: 100%;
  border: none;
  resize: vertical;
  font-size: 13px;
  font-family: inherit;
  outline: none;
  background: transparent;
}
.qr-cell-input {
  width: 100%;
  border: none;
  font-size: 13px;
  outline: none;
  text-align: center;
  background: transparent;
}
.output-type {
  font-weight: 700;
  font-size: 13px;
  white-space: nowrap;
  width: 150px;
}
.col-action {
  width: 34px;
  text-align: center;
}
.row-remove {
  background: #fee2e2;
  border: none;
  color: #dc2626;
  width: 26px;
  height: 26px;
  border-radius: 4px;
  cursor: pointer;
}
.qr-add-btn {
  background: #eef2ff;
  border: 1px dashed #1c2145;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  font-size: 13px;
}
.qr-textarea {
  width: 100%;
  border: 1px solid #1c2145;
  padding: 10px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}
.qr-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
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
.btn-draft {
  background: #fff;
  color: #1c2145;
  border: 2px solid #1c2145;
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
.btn-draft:disabled,
.btn-submit:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
/* ============================================================
   Light "paper document" theme guard
   The quarterly progress report is a printable MSUN form and must
   always render as a light document, regardless of the OS-level
   dark mode the rest of the app adapts to. These scoped !important
   rules out-rank the global dark-mode overrides in
   src/assets/css/base.css.
   ============================================================ */
.qr-form {
  color-scheme: light;
  background: #fff !important;
  border: 2px solid #1c2145 !important;
  color: #0f172a !important;
}

/* Dark, readable text everywhere in the document. */
.qr-form h2,
.qr-form p,
.qr-form span,
.qr-form label,
.qr-form td,
.qr-form th,
.qr-form strong,
.qr-form b,
.qr-form i,
.qr-form em {
  color: #0f172a !important;
}

.qr-form .qr-title h2,
.qr-form .qr-section-title {
  color: #1c2145 !important;
}

.qr-form .qr-subtitle {
  color: #444 !important;
}

/* Reset the dark-mode table/row/cell surfaces back to the white paper. */
.qr-form table,
.qr-form tr,
.qr-form td {
  background-color: transparent !important;
  border-color: #1c2145 !important;
}

.qr-form tr:hover td {
  background-color: transparent !important;
}

.qr-form .qr-table th {
  background-color: #eef2ff !important;
  color: #0f172a !important;
}

.qr-form .qr-table th,
.qr-form .qr-table td {
  border: 1px solid #1c2145 !important;
}

/* Form controls: transparent background + dark text, restore borders. */
.qr-form input,
.qr-form select,
.qr-form textarea {
  background: transparent !important;
  color: #0f172a !important;
  box-shadow: none !important;
  border-radius: 0 !important;
}

.qr-form .qr-input,
.qr-form .qr-select {
  border: none !important;
  border-bottom: 1px solid #94a3b8 !important;
}

.qr-form .qr-input[readonly],
.qr-form .qr-select:disabled {
  border-bottom: none !important;
}

.qr-form .qr-cell-input,
.qr-form .qr-cell-area {
  border: none !important;
}

.qr-form .qr-textarea {
  border: 1px solid #1c2145 !important;
}

/* Light surfaces the global dark theme would otherwise force dark. */
.qr-form .qr-period-row {
  background: #f1f5f9 !important;
  border: 1px solid #cbd5e1 !important;
}

/* Buttons */
.qr-form .qr-add-btn {
  background: #eef2ff !important;
  border: 1px dashed #1c2145 !important;
  color: #1c2145 !important;
}

.qr-form .row-remove {
  background: #fee2e2 !important;
  color: #dc2626 !important;
}

.qr-form .btn-secondary {
  background: #e2e8f0 !important;
  color: #1c2145 !important;
}

.qr-form .btn-draft {
  background: #fff !important;
  color: #1c2145 !important;
  border: 2px solid #1c2145 !important;
}

.qr-form .btn-submit {
  background: #ffd400 !important;
  color: #1c2145 !important;
}
</style>
