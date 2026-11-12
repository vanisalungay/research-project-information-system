<template>
  <div class="qpr-doc">
    <!-- ==================== PAGE 1 ==================== -->
    <!-- The proponent's 3-step report wizard renders one page at a time through
         the `page` prop. The form state lives in this component's setup scope,
         so hiding a page never clears what was already entered. -->
    <section v-if="showsPage(1)" class="qpr-doc-page">
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
      <article class="qpr-card">
        <header class="qpr-card-head qpr-card-head--navy">
          <span class="qpr-head-icon qpr-head-icon--navy" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 21h18"/><path d="M5 21V7a2 2 0 0 1 1-1.73l6-3.46a2 2 0 0 1 2 0l6 3.46A2 2 0 0 1 21 7v14"/><path d="M9 21v-4h6v4"/><path d="M9 8h.01M15 8h.01M9 12h.01M15 12h.01"/></svg>
          </span>
          <div class="qpr-head-text">
            <h2 class="qpr-card-title">(1) Project Basic Details</h2>
            <p class="qpr-card-sub">Enter general project identification info.</p>
          </div>
          <span class="qpr-period-pill">Period: {{ periodLabel }}</span>
        </header>
        <div class="qpr-card-body">
          <div class="qpr-field-grid">
            <div class="qpr-field qpr-field--span2">
              <span class="qpr-label">Project Title</span>
              <!-- The project identification fields are snapshots of the saved
                   proposal. They are read-only here so the report can never
                   disagree with the project it belongs to. -->
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.projectTitle || '' }}</span>
            </div>
            <div class="qpr-field">
              <span class="qpr-label">Project Leader</span>
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.projectLeader || '' }}</span>
            </div>

            <div class="qpr-field">
              <span class="qpr-label">Gender</span>
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.leaderGender || '' }}</span>
            </div>
            <div class="qpr-field">
              <label class="qpr-label" for="qpr-base-station">Base Station</label>
              <input
                v-if="!readonly"
                id="qpr-base-station"
                v-model="form.baseStation"
                class="qpr-input"
                type="text"
              />
              <span v-else class="qpr-value">{{ form.baseStation }}</span>
            </div>
            <div class="qpr-field">
              <label class="qpr-label" for="qpr-sites">Sites of Implementation</label>
              <input
                v-if="!readonly"
                id="qpr-sites"
                v-model="form.sitesOfImplementation"
                class="qpr-input"
                type="text"
              />
              <span v-else class="qpr-value">{{ form.sitesOfImplementation }}</span>
            </div>

            <div class="qpr-field">
              <span class="qpr-label">Duration (Months)</span>
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.projectDuration || '' }}</span>
            </div>
            <div class="qpr-field">
              <span class="qpr-label">Start Date</span>
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.projectStartDate || '' }}</span>
            </div>
            <div class="qpr-field">
              <span class="qpr-label">End Date</span>
              <span
                class="qpr-value qpr-value--source"
                title="From the saved proposal"
              >{{ form.projectEndDate || '' }}</span>
              <span v-if="errors.projectEndDate" class="field-error">{{ errors.projectEndDate }}</span>
            </div>
          </div>
        </div>
      </article>

      <!-- (7) Major accomplishments -->
      <article class="qpr-card">
        <header class="qpr-card-head">
          <span class="qpr-head-icon qpr-head-icon--green" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>
          </span>
          <div class="qpr-head-text">
            <h2 class="qpr-card-title">(7) Major Accomplishments</h2>
            <p class="qpr-card-sub">Actual accomplishment of the project (vis-à-vis the objectives)</p>
          </div>
          <button v-if="!readonly" type="button" class="qpr-btn-add" @click="addObjective">
            <span aria-hidden="true">+</span> Add Objective
          </button>
        </header>
        <div class="qpr-card-body qpr-card-body--flush">
          <div class="qpr-table-scroll">
            <table class="qpr-table qpr-objectives-table">
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
                  <th>OBJECTIVES</th>
                  <th>TARGET</th>
                  <th>ACTUAL</th>
                  <th>FOR THE PERIOD</th>
                  <th>CUMULATIVE</th>
                  <th v-if="!readonly" class="col-action">ACTION</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in form.objectives" :key="i">
                  <td>
                    <span v-if="readonly" class="qpr-cell-text">{{ row.objective }}</span>
                    <textarea
                      v-else
                      v-model="row.objective"
                      class="qpr-cell-area"
                      rows="2"
                      placeholder="Describe the objective..."
                    ></textarea>
                  </td>
                  <td>
                    <span v-if="readonly" class="qpr-cell-text qpr-cell-text--center">{{ row.target }}</span>
                    <textarea
                      v-else
                      v-model="row.target"
                      class="qpr-cell-area qpr-cell-area--center"
                      rows="2"
                      placeholder="Target"
                    ></textarea>
                  </td>
                  <td>
                    <span v-if="readonly" class="qpr-cell-text qpr-cell-text--center">{{ row.actual }}</span>
                    <textarea
                      v-else
                      v-model="row.actual"
                      class="qpr-cell-area qpr-cell-area--center"
                      rows="2"
                      placeholder="Actual"
                    ></textarea>
                  </td>
                  <td>
                    <span
                      v-if="readonly"
                      class="qpr-pct-value"
                      :class="percentTone(row.percentagePeriod)"
                    >{{ formatPercentage(row.percentagePeriod) }}</span>
                    <template v-else>
                      <div class="qpr-pct-box" :class="percentTone(row.percentagePeriod)">
                        <PercentageInput
                          v-model="row.percentagePeriod"
                          :aria-label="`Percentage for the period, objective ${i + 1}`"
                        />
                      </div>
                      <span v-if="errors[`objective-${i}-percentagePeriod`]" class="field-error">
                        {{ errors[`objective-${i}-percentagePeriod`] }}
                      </span>
                    </template>
                  </td>
                  <td>
                    <span
                      v-if="readonly"
                      class="qpr-pct-value"
                      :class="percentTone(row.percentageCumulative)"
                    >{{ formatPercentage(row.percentageCumulative) }}</span>
                    <template v-else>
                      <div class="qpr-pct-box" :class="percentTone(row.percentageCumulative)">
                        <PercentageInput
                          v-model="row.percentageCumulative"
                          :aria-label="`Cumulative percentage, objective ${i + 1}`"
                        />
                      </div>
                      <span v-if="errors[`objective-${i}-percentageCumulative`]" class="field-error">
                        {{ errors[`objective-${i}-percentageCumulative`] }}
                      </span>
                    </template>
                  </td>
                  <td v-if="!readonly" class="col-action">
                    <button
                      type="button"
                      class="qpr-row-remove"
                      title="Remove row"
                      aria-label="Remove row"
                      @click="removeObjective(i)"
                    >
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"/><path d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/><path d="M10 11v6M14 11v6"/></svg>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p v-if="errors.percentages" class="field-error qpr-table-error">
            {{ errors.percentages }}
          </p>
        </div>
      </article>

      <!-- B. Catch-up plan -->
      <article class="qpr-card">
        <header class="qpr-card-head">
          <span class="qpr-head-icon qpr-head-icon--amber" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>
          </span>
          <div class="qpr-head-text">
            <h2 class="qpr-card-title">B. Catch-up Plan (If behind schedule)</h2>
          </div>
          <span class="qpr-word-count">{{ catchUpWordCount }} / 500 words</span>
        </header>
        <div class="qpr-card-body">
          <span v-if="readonly" class="qpr-note-text">{{ form.catchUpPlan }}</span>
          <textarea
            v-else
            v-model="form.catchUpPlan"
            class="qpr-textarea"
            rows="4"
            placeholder="Describe delays, catch-up measures, and support needed..."
          ></textarea>
        </div>
      </article>
    </section>

    <!-- ==================== PAGE 2 ==================== -->
    <section v-if="showsPage(2)" class="qpr-doc-page">
      <!-- C. Expected outputs / 6Ps -->
      <article class="qpr-card">
        <header class="qpr-card-head qpr-card-head--navy">
          <span class="qpr-head-icon qpr-head-icon--navy" aria-hidden="true">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3v18h18"/><path d="m7 14 4-4 3 3 5-6"/></svg>
          </span>
          <div class="qpr-head-text">
            <h2 class="qpr-card-title">C. Expected Outputs / 6Ps</h2>
            <p class="qpr-card-sub">Expected outputs should be measurable and aligned with target goals.</p>
          </div>
        </header>
        <div class="qpr-card-body qpr-card-body--flush">
          <div class="qpr-table-scroll">
            <table class="qpr-table qpr-outputs-table">
              <colgroup>
                <col class="col-type" />
                <col class="col-expected" />
                <col class="col-actual-out" />
              </colgroup>
              <thead>
                <tr>
                  <th>6PS CATEGORY</th>
                  <th>EXPECTED OUTPUTS</th>
                  <th>ACTUAL OUTPUTS DELIVERED</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in form.outputs" :key="i">
                  <td class="qpr-output-type">{{ row.outputType }}</td>
                  <td>
                    <span v-if="readonly" class="qpr-cell-text">{{ row.expectedOutput }}</span>
                    <textarea
                      v-else
                      v-model="row.expectedOutput"
                      class="qpr-cell-area"
                      rows="2"
                      placeholder="Expected output"
                    ></textarea>
                  </td>
                  <td>
                    <span v-if="readonly" class="qpr-cell-text">{{ row.actualOutput }}</span>
                    <textarea
                      v-else
                      v-model="row.actualOutput"
                      class="qpr-cell-area"
                      rows="2"
                      placeholder="Actual output delivered"
                    ></textarea>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </article>

      <div class="qpr-two-col">
        <!-- (8) Problems / concerns -->
        <article class="qpr-card qpr-card--fill">
          <header class="qpr-card-head">
            <span class="qpr-head-icon qpr-head-icon--red" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"/><path d="M12 9v4M12 17h.01"/></svg>
            </span>
            <div class="qpr-head-text">
              <h2 class="qpr-card-title">(8) Problems / Concerns</h2>
              <p class="qpr-card-sub">Highlight operational bottlenecks, supply chain issues, or weather delays.</p>
            </div>
          </header>
          <div class="qpr-card-body qpr-card-body--fill">
            <span v-if="readonly" class="qpr-note-text">{{ form.problemsConcerns }}</span>
            <textarea
              v-else
              v-model="form.problemsConcerns"
              class="qpr-textarea qpr-textarea--fill"
              rows="4"
              placeholder="Highlight operational bottlenecks, supply chain issues, or weather delays."
            ></textarea>
          </div>
        </article>

        <!-- (9) Suggested solutions -->
        <article class="qpr-card qpr-card--fill">
          <header class="qpr-card-head">
            <span class="qpr-head-icon qpr-head-icon--green" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/></svg>
            </span>
            <div class="qpr-head-text">
              <h2 class="qpr-card-title">(9) Suggested Solutions</h2>
              <p class="qpr-card-sub">Specify corresponding corrective actions or support needed from administration.</p>
            </div>
          </header>
          <div class="qpr-card-body qpr-card-body--fill">
            <span v-if="readonly" class="qpr-note-text">{{ form.suggestedSolutions }}</span>
            <textarea
              v-else
              v-model="form.suggestedSolutions"
              class="qpr-textarea qpr-textarea--fill"
              rows="4"
              placeholder="Specify corresponding corrective actions or support needed from administration."
            ></textarea>
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, computed, watch } from 'vue'
import PercentageInput from '@/components/PercentageInput.vue'
import { formatPercentage } from '@/utils/percentageInput.js'
import {
  emptyObjective,
  reportFormState,
  reportPayload,
} from '@/utils/quarterlyReportFormState'

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

// The form state itself (defaults, the report → form mapping and the save payload) lives
// in utils/quarterlyReportFormState.js, so the values shown in the form and the values
// that are saved come from one tested place.
const form = reactive(reportFormState(null))

function applyReport(r) {
  Object.assign(form, reportFormState(r))
}

watch(() => props.report, (r) => applyReport(r), { immediate: true })

// Reporting period shown in the document header badge, taken from the saved report.
const periodLabel = computed(() => `${form.period || '—'} ${form.year || ''}`.trim())

// Word count of the catch-up plan. It is display guidance only: the report keeps
// accepting a longer plan, exactly as it did before.
const catchUpWordCount = computed(() => {
  const text = (form.catchUpPlan || '').trim()
  return text ? text.split(/\s+/).length : 0
})

/**
 * Tone of a percentage value, used only for its on-screen colour. It never
 * changes the stored value or the arithmetic: a reported percentage is
 * highlighted green when it is on track and amber when it needs attention.
 */
function percentTone(value) {
  if (value === '' || value === null || value === undefined) return ''
  const number = Number(value)
  if (!Number.isFinite(number)) return ''
  return number >= 75 ? 'is-good' : 'is-warn'
}

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
  return reportPayload(form)
}

// The stage action buttons (Cancel / Save Draft / Submit Report) are rendered by
// the parent page next to the Project Workplan form, so the parent reads the
// current quarterly progress report payload through this exposed method. The
// reporting period label is exposed too so the wizard header can show the
// quarter the proponent is reporting on without duplicating the form state.
defineExpose({ buildPayload, periodLabel })
</script>

<style scoped>
/* ============================================================
   Quarterly Progress Report pages (1 and 2)
   Clean institutional cards: navy section headers, white bodies,
   thin blue-grey borders and compact, legible controls.
   ============================================================ */
.qpr-doc {
  display: flex;
  flex-direction: column;
  gap: 18px;
  color: #172b4d;
}

.qpr-doc-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.qpr-card {
  background: #ffffff;
  border: 1px solid #dce5f0;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(16, 27, 51, 0.04);
  overflow: hidden;
}

.qpr-card--fill {
  display: flex;
  flex-direction: column;
}

/* ---------- Card headers ---------- */
.qpr-card-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 15px 18px;
  border-bottom: 1px solid #e6edf6;
  background: #ffffff;
}

.qpr-card-head--navy {
  padding: 13px 18px;
  border-bottom: 0;
  background: #101b33;
  color: #ffffff;
}

.qpr-head-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 7px;
}

.qpr-head-icon svg {
  width: 15px;
  height: 15px;
}

.qpr-head-icon--navy {
  background: rgba(255, 255, 255, 0.09);
  color: #8fb8ff;
}

.qpr-head-icon--green {
  background: #e8f8f1;
  color: #0e9f6e;
}

.qpr-head-icon--amber {
  background: #fef4e2;
  color: #d18b1f;
}

.qpr-head-icon--red {
  background: #fdecec;
  color: #d64545;
}

.qpr-head-text {
  min-width: 0;
}

.qpr-card-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  color: #172b4d;
}

.qpr-card-head--navy .qpr-card-title {
  font-size: 14.5px;
  color: #ffffff;
}

.qpr-card-sub {
  margin: 3px 0 0;
  font-size: 10.5px;
  line-height: 1.4;
  color: #7a879c;
}

.qpr-card-head--navy .qpr-card-sub {
  color: #b9c6da;
}

.qpr-period-pill {
  margin-left: auto;
  flex: none;
  padding: 4px 9px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 5px;
  font-size: 10px;
  font-weight: 500;
  color: #d8e2f2;
  white-space: nowrap;
}

.qpr-word-count {
  margin-left: auto;
  flex: none;
  font-size: 10.5px;
  color: #94a1b5;
  white-space: nowrap;
}

.qpr-btn-add {
  margin-left: auto;
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 11px;
  border: 1px solid #86d3b6;
  border-radius: 6px;
  background: #ffffff;
  color: #0e9f6e;
  font: inherit;
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
}

.qpr-btn-add:hover {
  background: #f1fbf6;
}

/* ---------- Card bodies ---------- */
.qpr-card-body {
  padding: 16px 18px 18px;
}

.qpr-card-body--flush {
  padding: 0;
}

.qpr-card-body--fill {
  display: flex;
  flex: 1 1 auto;
  flex-direction: column;
}

/* ---------- Project basic details ---------- */
.qpr-field-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 16px;
}

.qpr-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.qpr-field--span2 {
  grid-column: span 2;
}

.qpr-label {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.07em;
  text-transform: uppercase;
  color: #63708a;
}

.qpr-value,
.qpr-input {
  box-sizing: border-box;
  width: 100%;
  min-height: 36px;
  padding: 8px 11px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  font-size: 12.5px;
  font-weight: 500;
  line-height: 1.4;
}

.qpr-value {
  display: flex;
  align-items: center;
  overflow-wrap: anywhere;
}

.qpr-input {
  outline: none;
}

.qpr-input:focus {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.12);
}

/* Proposal-sourced values are visibly read-only without looking disabled. */
.qpr-value--source {
  background: #f8fafd;
  color: #33415c;
  font-weight: 600;
}

/* ---------- Tables ---------- */
.qpr-table-scroll {
  overflow-x: auto;
}

.qpr-table {
  width: 100%;
  min-width: 860px;
  border-collapse: collapse;
}

.qpr-table th {
  padding: 10px 11px;
  background: #eff3f8;
  border-bottom: 1px solid #dce5f0;
  color: #40506b;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.06em;
  line-height: 1.35;
  text-align: left;
  text-transform: uppercase;
  white-space: nowrap;
}

.qpr-table td {
  padding: 9px 11px;
  border-bottom: 1px solid #edf1f7;
  vertical-align: middle;
}

.qpr-table tbody tr:last-child td {
  border-bottom: 0;
}

.qpr-objectives-table .col-objective {
  width: 32%;
}

.qpr-objectives-table .col-target,
.qpr-objectives-table .col-actual {
  width: 13%;
}

.qpr-objectives-table .col-period,
.qpr-objectives-table .col-cumulative {
  width: 15%;
}

.qpr-objectives-table .col-action {
  width: 7%;
  text-align: center;
}

.qpr-cell-area {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 34px;
  padding: 6px 9px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  font-size: 11.5px;
  line-height: 1.4;
  resize: vertical;
  outline: none;
}

.qpr-cell-area:focus {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.1);
}

.qpr-cell-area--center {
  text-align: center;
}

.qpr-cell-text {
  display: block;
  font-size: 11.5px;
  line-height: 1.45;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.qpr-cell-text--center {
  text-align: center;
}

/* The percentage fields keep the numeric model; the box, the aligned sign and
   the tone colour are presentation only. */
.qpr-pct-box {
  display: flex;
  align-items: center;
  min-height: 34px;
  padding: 6px 9px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  font-size: 11.5px;
  font-weight: 600;
  color: #33415c;
}

.qpr-pct-box:focus-within {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.1);
}

.qpr-pct-box.is-good {
  color: #0e9f6e;
  background: #f5fbf8;
}

.qpr-pct-box.is-warn {
  color: #b7791f;
  background: #fffbf2;
}

.qpr-pct-box :deep(.percentage-input__symbol) {
  color: inherit;
  opacity: 0.85;
}

.qpr-pct-value {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 34px;
  padding: 6px 9px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  font-size: 11.5px;
  font-weight: 600;
  color: #33415c;
}

.qpr-pct-value.is-good {
  color: #0e9f6e;
  background: #f5fbf8;
}

.qpr-pct-value.is-warn {
  color: #b7791f;
  background: #fffbf2;
}

.qpr-row-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: #8695ad;
  cursor: pointer;
}

.qpr-row-remove svg {
  width: 14px;
  height: 14px;
}

.qpr-row-remove:hover {
  border-color: #f3c6c6;
  background: #fef2f2;
  color: #d64545;
}

.qpr-table-error {
  padding: 0 18px 12px;
}

/* ---------- Expected outputs ---------- */
.qpr-outputs-table {
  min-width: 700px;
}

.qpr-outputs-table .col-type {
  width: 22%;
}

.qpr-outputs-table .col-expected,
.qpr-outputs-table .col-actual-out {
  width: 39%;
}

.qpr-output-type {
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
  color: #40506b;
  white-space: nowrap;
}

/* ---------- Narrative cards ---------- */
.qpr-two-col {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.qpr-textarea {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 92px;
  padding: 10px 11px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  font-size: 11.5px;
  line-height: 1.55;
  resize: vertical;
  outline: none;
}

.qpr-textarea:focus {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.1);
}

.qpr-textarea--fill {
  flex: 1 1 auto;
  min-height: 120px;
}

.qpr-note-text {
  display: block;
  font-size: 11.5px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ---------- Optional reporting-period controls ---------- */
.qr-period-row {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  padding: 10px 14px;
  border: 1px solid #dce5f0;
  border-radius: 8px;
  background: #ffffff;
}

.qr-period-field {
  display: flex;
  align-items: center;
  gap: 7px;
  font-size: 11px;
  font-weight: 600;
  color: #40506b;
}

.qr-period-input {
  min-height: 32px;
  padding: 5px 8px;
  border: 1px solid #dce5f0;
  border-radius: 6px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  font-size: 11px;
  outline: none;
}

/* ---------- Validation ---------- */
.field-error {
  display: block;
  margin-top: 5px;
  font-size: 10px;
  font-weight: 600;
  line-height: 1.35;
  color: #c0392b;
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .qpr-field-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .qpr-field--span2 {
    grid-column: span 2;
  }

  .qpr-two-col {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .qpr-field-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .qpr-field--span2 {
    grid-column: auto;
  }

  .qpr-card-head {
    flex-wrap: wrap;
  }

  .qpr-period-pill,
  .qpr-word-count,
  .qpr-btn-add {
    margin-left: 0;
  }

  .qpr-btn-add {
    width: 100%;
    justify-content: center;
  }
}
</style>
