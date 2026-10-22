<template>
  <div class="doc-scroll">
    <!-- ==================== PAGE 1 ==================== -->
    <!-- The proponent's 3-step report wizard renders one page at a time through
         the `page` prop. The form state lives in this component's setup scope,
         so hiding a page never clears what was already entered. -->
    <section v-if="showsPage(1)" class="doc-sheet">
      <div class="doc-header">
        <span class="doc-ring doc-ring-1" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-2" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-3" aria-hidden="true"></span>

        <div class="doc-header-left">
          <span class="doc-emblem" aria-hidden="true"></span>
          <div class="doc-header-text">
            <span class="doc-eyebrow">MSUN Internally-Funded Project</span>
            <h2 class="doc-title">Quarterly Progress Report</h2>
          </div>
        </div>

        <div class="doc-header-right">
          <span class="doc-badge">Reporting period: {{ periodLabel }}</span>
          <span class="doc-badge-sub">As of {{ yearLabel }}</span>
        </div>
      </div>
      <div class="doc-divider"></div>

      <div class="doc-body">
        <!-- Reporting period controls: only when the host allows changing it. -->
        <div v-if="!readonly && !lockPeriod" class="qr-period-row">
          <label class="qr-period-field">
            <span>Reporting Quarter</span>
            <select v-model="form.period" class="qr-period-input">
              <option value="Q1">Q1</option>
              <option value="Q2">Q2</option>
              <option value="Q3">Q3</option>
              <option value="Q4">Q4</option>
            </select>
          </label>
          <label class="qr-period-field">
            <span>Year</span>
            <input v-model.number="form.year" type="number" class="qr-period-input" />
          </label>
        </div>

        <!-- (1)-(6) Project information -->
        <table class="doc-info-table">
          <tbody>
            <tr>
              <td class="doc-cream" colspan="6">
                <span class="doc-label">(1) Project Title</span>
                <span v-if="readonly" class="doc-value">{{ form.projectTitle }}</span>
                <input v-else v-model="form.projectTitle" class="doc-cell-input" />
              </td>
            </tr>
            <tr>
              <td colspan="3">
                <span class="doc-label">(2) Project Leader</span>
                <span v-if="readonly" class="doc-value">{{ form.projectLeader }}</span>
                <input v-else v-model="form.projectLeader" class="doc-cell-input" />
              </td>
              <td colspan="3">
                <span class="doc-label">Gender</span>
                <span v-if="readonly" class="doc-value">{{ form.leaderGender || '—' }}</span>
                <select v-else v-model="form.leaderGender" class="doc-cell-input">
                  <option value="">—</option>
                  <option>Male</option>
                  <option>Female</option>
                  <option>Prefer not to say</option>
                </select>
              </td>
            </tr>
            <tr>
              <td colspan="3">
                <span class="doc-label">(3) Base Station</span>
                <span v-if="readonly" class="doc-value">{{ form.baseStation }}</span>
                <input v-else v-model="form.baseStation" class="doc-cell-input" />
              </td>
              <td colspan="3">
                <span class="doc-label">Site/s of Implementation</span>
                <span v-if="readonly" class="doc-value">{{ form.sitesOfImplementation }}</span>
                <input v-else v-model="form.sitesOfImplementation" class="doc-cell-input" />
              </td>
            </tr>
            <tr>
              <td colspan="2">
                <span class="doc-label">(4) Project Duration</span>
                <span v-if="readonly" class="doc-value">{{ form.projectDuration }}</span>
                <input v-else v-model="form.projectDuration" class="doc-cell-input" />
              </td>
              <td colspan="2">
                <span class="doc-label">(5) Start Date</span>
                <span v-if="readonly" class="doc-value">{{ form.projectStartDate }}</span>
                <input
                  v-else
                  v-model="form.projectStartDate"
                  type="date"
                  class="doc-cell-input"
                />
              </td>
              <td colspan="2">
                <span class="doc-label">(6) End Date</span>
                <span v-if="readonly" class="doc-value">{{ form.projectEndDate }}</span>
                <input
                  v-else
                  v-model="form.projectEndDate"
                  type="date"
                  class="doc-cell-input"
                  :class="{ 'cell-invalid': !!errors.projectEndDate }"
                />
                <span v-if="errors.projectEndDate" class="field-error">{{ errors.projectEndDate }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- (7) Major accomplishments -->
        <div class="doc-section">
          <span class="doc-section-accent" aria-hidden="true"></span>
          (7) Major accomplishments
        </div>

        <p class="doc-subsection">A. Actual accomplishment of the project (vis-à-vis the objectives)</p>

        <table class="doc-table objectives-table">
          <colgroup>
            <col class="col-objective" />
            <col class="col-target" />
            <col class="col-actual" />
            <col class="col-period" />
            <col class="col-cumulative" />
            <col v-if="!readonly" class="col-action" />
          </colgroup>
          <thead>
            <tr>
              <th rowspan="2">OBJECTIVES</th>
              <th colspan="2">ACCOMPLISHMENTS</th>
              <th colspan="2">PERCENTAGE (%)</th>
              <th v-if="!readonly" rowspan="2" class="col-action">—</th>
            </tr>
            <tr>
              <th>TARGET</th>
              <th>ACTUAL</th>
              <th>FOR THE PERIOD</th>
              <th>CUMULATIVE (FROM START)</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in form.objectives" :key="i">
              <td>
                <span v-if="readonly" class="qr-cell-text">{{ row.objective }}</span>
                <textarea v-else v-model="row.objective" class="qr-cell-area" rows="3"></textarea>
              </td>
              <td>
                <span v-if="readonly" class="qr-cell-text">{{ row.target }}</span>
                <textarea v-else v-model="row.target" class="qr-cell-area" rows="3"></textarea>
              </td>
              <td>
                <span v-if="readonly" class="qr-cell-text">{{ row.actual }}</span>
                <textarea v-else v-model="row.actual" class="qr-cell-area" rows="3"></textarea>
              </td>
              <td>
                <span v-if="readonly" class="qr-cell-value">{{ row.percentagePeriod }}</span>
                <input v-else v-model="row.percentagePeriod" class="qr-cell-input" />
              </td>
              <td>
                <span v-if="readonly" class="qr-cell-value">{{ row.percentageCumulative }}</span>
                <input v-else v-model="row.percentageCumulative" class="qr-cell-input" />
              </td>
              <td v-if="!readonly" class="col-action">
                <button type="button" class="row-remove" @click="removeObjective(i)" title="Remove row">✕</button>
              </td>
            </tr>
          </tbody>
        </table>
        <button v-if="!readonly" type="button" class="qr-add-btn" @click="addObjective">+ Add Objective</button>

        <p class="doc-subsection">B. Catch-up plan</p>
        <div class="doc-note">
          <span v-if="readonly" class="qr-note-text">{{ form.catchUpPlan }}</span>
          <textarea v-else v-model="form.catchUpPlan" class="qr-note-area" rows="3"></textarea>
        </div>
      </div>

      <div class="doc-footer">
        <span class="doc-footer-side">MSUN Internally-Funded Project &middot; Quarterly Progress Report</span>
        <span class="doc-footer-center">{{ form.projectTitle || '—' }}</span>
        <span class="doc-page-badge">Page 1 of 3</span>
      </div>
    </section>

    <!-- ==================== PAGE 2 ==================== -->
    <section v-if="showsPage(2)" class="doc-sheet">
      <div class="doc-header">
        <span class="doc-ring doc-ring-1" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-2" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-3" aria-hidden="true"></span>

        <div class="doc-header-left">
          <span class="doc-emblem" aria-hidden="true"></span>
          <div class="doc-header-text">
            <span class="doc-eyebrow">MSUN Internally-Funded Project</span>
            <h2 class="doc-title">Quarterly Progress Report</h2>
          </div>
        </div>

        <div class="doc-header-right">
          <span class="doc-badge">Reporting period: {{ periodLabel }}</span>
          <span class="doc-badge-sub">As of {{ yearLabel }}</span>
        </div>
      </div>
      <div class="doc-divider"></div>

      <div class="doc-body">
        <!-- C. Expected outputs / 6Ps -->
        <div class="doc-section">
          <span class="doc-section-accent" aria-hidden="true"></span>
          C. Expected outputs / 6Ps
        </div>
        <p class="doc-hint">Expected outputs should be measurable.</p>

        <table class="doc-table outputs-table">
          <colgroup>
            <col class="col-type" />
            <col class="col-expected" />
            <col class="col-actual-out" />
          </colgroup>
          <thead>
            <tr>
              <th>6Ps</th>
              <th>EXPECTED OUTPUTS</th>
              <th>ACTUAL OUTPUTS</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in form.outputs" :key="i">
              <td class="output-type doc-cell-cream">{{ row.outputType }}</td>
              <td>
                <span v-if="readonly" class="qr-cell-text">{{ row.expectedOutput }}</span>
                <textarea v-else v-model="row.expectedOutput" class="qr-cell-area" rows="2"></textarea>
              </td>
              <td>
                <span v-if="readonly" class="qr-cell-text">{{ row.actualOutput }}</span>
                <textarea v-else v-model="row.actualOutput" class="qr-cell-area" rows="2"></textarea>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- (8) Problems / concerns -->
        <div class="doc-section">
          <span class="doc-section-accent" aria-hidden="true"></span>
          (8) Problems / concerns
        </div>
        <div class="doc-note doc-note--yellow">
          <span v-if="readonly" class="qr-note-text">{{ form.problemsConcerns }}</span>
          <textarea v-else v-model="form.problemsConcerns" class="qr-note-area" rows="3"></textarea>
        </div>

        <!-- (9) Suggested solutions -->
        <div class="doc-section">
          <span class="doc-section-accent" aria-hidden="true"></span>
          (9) Suggested solutions to the above concerns
        </div>
        <div class="doc-note doc-note--green">
          <span v-if="readonly" class="qr-note-text">{{ form.suggestedSolutions }}</span>
          <textarea v-else v-model="form.suggestedSolutions" class="qr-note-area" rows="3"></textarea>
        </div>
      </div>

      <div class="doc-footer">
        <span class="doc-footer-side">MSUN Internally-Funded Project &middot; Quarterly Progress Report</span>
        <span class="doc-footer-center">{{ form.projectTitle || '—' }}</span>
        <span class="doc-page-badge">Page 2 of 3</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
// Shared document shell (page, header, footer, tables, print rules) used by the
// Quarterly Progress Report pages and the Project Workplan page.
import '@/assets/reportDocument.css'

const props = defineProps({
  report: { type: Object, default: null },
  readonly: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
  // When true the reporting quarter is fixed to the current reporting stage and
  // cannot be changed by the proponent (the backend validates the stage too).
  lockPeriod: { type: Boolean, default: false },
  // Wizard page gating used by the proponent's 3-step report form:
  //   1 = Quarterly Progress Report, 2 = continuation of the report.
  // When null (the default, used by the read-only viewers) the complete
  // document - both report pages - is rendered exactly as before.
  page: { type: Number, default: null },
  // Field-level validation messages shown next to the field they belong to,
  // keyed by field name (e.g. { projectEndDate: '...' }). The rules themselves
  // live in the host page, which owns the wizard navigation.
  errors: { type: Object, default: () => ({}) },
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

// Reporting period shown in the document header badge, taken from the saved report.
const periodLabel = computed(() => `${form.period || '—'} ${form.year || ''}`.trim())

// Page gating for the proponent's 3-step report wizard. Without a `page` prop
// (read-only viewers) the whole document is rendered, exactly as before.
function showsPage(n) {
  return props.page == null || props.page === n
}

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

// The stage action buttons (Cancel / Save Draft / Submit Report) are rendered by
// the parent page below the Project Workplan form, so the parent reads the
// current quarterly progress report payload through this exposed method. The
// reporting period label is exposed too so the wizard header can show the
// quarter the proponent is reporting on without duplicating the form state.
defineExpose({ buildPayload, periodLabel })
</script>

<style scoped>
/* Reporting-period controls: only rendered when the host allows editing it. */
.qr-period-row {
  display: flex;
  gap: 16px;
  margin-bottom: 8px;
  padding: 6px 8px;
  border: 1px solid #d8d9e8;
  border-radius: 4px;
  background: #f1f0f8;
}

.qr-period-field {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  font-weight: 600;
  color: #25224f;
}

.qr-period-input {
  padding: 1px 4px;
  border: 1px solid #d8d9e8;
  border-radius: 3px;
  background: #ffffff;
  color: #202044;
  font: inherit;
  font-size: 10px;
}

/* Values inside the project-information table read as printed text while staying
   editable inputs for the proponent. */
.doc-cell-input {
  display: block;
  width: 100%;
  margin-top: 1px;
  padding: 0;
  border: none;
  background: transparent;
  color: #202044;
  font: inherit;
  font-size: 10px;
  outline: none;
}

.doc-cell-input:disabled {
  color: #202044;
  opacity: 1;
}

/* ---------- Read-only display values ----------
   The read-only interfaces (viewing a submitted report, reviewing it before
   submission) render the saved values as printed text instead of editable
   controls, keeping the same document look as the fields they replace. The
   project-information cells reuse the shared .doc-value class (reportDocument.css,
   the same pattern the Project Workplan already uses); the tables and the
   narrative notes get the equivalents of .qr-cell-area / .qr-cell-input /
   .qr-note-area. */
.qr-cell-text {
  display: block;
  width: 100%;
  color: #202044;
  font: inherit;
  font-size: 9px;
  line-height: 1.3;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.qr-cell-value {
  display: block;
  width: 100%;
  color: #202044;
  font: inherit;
  font-size: 9px;
  text-align: center;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.qr-note-text {
  display: block;
  width: 100%;
  color: #202044;
  font: inherit;
  font-size: 9.5px;
  line-height: 1.35;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* Field-level validation (page 1 of the wizard): the offending cell keeps the
   printed-document look, so the marker is a red underline plus a short message
   directly under the field it belongs to. */
.doc-cell-input.cell-invalid {
  border-bottom: 1px solid #dc2626;
}

.field-error {
  display: block;
  margin-top: 2px;
  font-size: 8.5px;
  font-weight: 600;
  line-height: 1.25;
  color: #b91c1c;
}

/* (7) Accomplishments table column proportions. */
.col-objective {
  width: 32%;
}

.col-target {
  width: 14%;
}

.col-actual {
  width: 14%;
}

.col-period {
  width: 14%;
}

.col-cumulative {
  width: 18%;
}

.col-action {
  width: 8%;
  text-align: center;
  vertical-align: middle;
}

.qr-cell-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 30px;
  padding: 0;
  border: none;
  background: transparent;
  color: #202044;
  font: inherit;
  font-size: 9px;
  line-height: 1.3;
  resize: vertical;
  outline: none;
}

.qr-cell-input {
  display: block;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  color: #202044;
  font: inherit;
  font-size: 9px;
  text-align: center;
  outline: none;
}

.row-remove {
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 3px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 10px;
  line-height: 1;
  cursor: pointer;
}

.qr-add-btn {
  margin-top: 5px;
  padding: 4px 10px;
  border: 1px dashed #25224f;
  border-radius: 4px;
  background: #f1f0f8;
  color: #25224f;
  font: inherit;
  font-size: 9px;
  font-weight: 600;
  cursor: pointer;
}

/* (C) Expected outputs / 6Ps table column proportions. */
.col-type {
  width: 30%;
}

.col-expected {
  width: 35%;
}

.col-actual-out {
  width: 35%;
}

.output-type {
  font-weight: 600;
  color: #25224f;
}

/* Narrative areas: the bordered box provides the document styling while the
   textarea inside stays the editable / saved value. */
.qr-note-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 34px;
  padding: 0;
  border: none;
  background: transparent;
  color: #202044;
  font: inherit;
  font-size: 9.5px;
  line-height: 1.35;
  resize: vertical;
  outline: none;
}

/* The row-remove column only exists while editing, so it is not part of the
   printed document. */
@media print {
  .objectives-table th.col-action,
  .objectives-table td.col-action {
    display: none;
  }

  .qr-add-btn {
    display: none;
  }

  /* Validation messages are an on-screen aid, never part of the document. */
  .field-error {
    display: none;
  }
}
</style>
