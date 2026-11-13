<template>
  <div class="wp-doc">
    <!-- ==================== PAGE 3 - PROJECT WORKPLAN ==================== -->
    <article class="wp-card wp-card--matrix">
      <header class="wp-card-head wp-card-head--navy">
        <span class="wp-head-icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="2"/><path d="M16 2v4M8 2v4M3 10h18"/><path d="M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01M16 18h.01"/></svg>
        </span>
        <div class="wp-head-text">
          <h2 class="wp-card-title">Project Workplan Matrix</h2>
          <p class="wp-card-sub">Monthly Target Activities and Accomplishments (in %)</p>
        </div>
        <div class="wp-head-actions">
          <label v-if="!readonly" class="wp-as-of">
            <span>As of</span>
            <input v-model="model.asOf" type="date" aria-label="Workplan as-of date" />
          </label>
          <span v-else-if="model.asOf" class="wp-as-of-text">As of {{ model.asOf }}</span>
          <button v-if="!readonly" type="button" class="wp-btn-add" @click="addObjective">
            <span aria-hidden="true">+</span> Add Row
          </button>
        </div>
      </header>

      <div class="wp-table-scroll">
        <table class="wp-table">
          <colgroup>
            <col class="col-objectives" />
            <col v-for="m in months" :key="m" class="col-month" />
            <col class="col-total" />
          </colgroup>
          <thead>
            <tr>
              <th class="th-objectives">OBJECTIVES</th>
              <th v-for="m in months" :key="m" class="th-month">{{ m }}</th>
              <th class="th-total">AVERAGE TOTAL</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(obj, i) in objectives" :key="i" class="row-objective">
              <td class="td-objectives">
                <textarea
                  v-if="!readonly"
                  v-model="obj.objective"
                  class="wp-objective-input"
                  rows="1"
                  placeholder="Objective description"
                ></textarea>
                <span v-else class="wp-objective-text">{{ obj.objective }}</span>
              </td>
              <td v-for="(m, mi) in months" :key="m" class="td-month">
                <PercentageInput
                  v-if="!readonly"
                  v-model="obj.months[mi]"
                  :aria-label="`${m} percentage for objective ${i + 1}`"
                />
                <span v-else class="wp-month-value">{{ formatPercentage(monthValue(obj, mi)) }}</span>
              </td>
              <td class="td-total" aria-readonly="true">
                <span
                  class="wp-total-value"
                  title="Calculated automatically from the monthly percentages entered"
                >{{ averageTotal(obj) }}</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="wp-table-foot">
        <p class="wp-hint">* AVERAGE TOTAL is calculated automatically based on non-zero month values.</p>
        <button
          v-if="!readonly"
          type="button"
          class="wp-btn-remove"
          :disabled="objectives.length <= minRows"
          @click="removeObjective"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="m9 9 6 6M15 9l-6 6"/></svg>
          Remove Last Row
        </button>
      </div>
    </article>

    <!-- Remarks -->
    <article class="wp-card">
      <header class="wp-card-head">
        <span class="wp-head-icon wp-head-icon--blue" aria-hidden="true">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
        </span>
        <div class="wp-head-text">
          <h2 class="wp-card-title">Remarks (Catch-up Plan, Delays, etc.)</h2>
        </div>
      </header>
      <div class="wp-card-body">
        <span v-if="readonly" class="wp-note-text">{{ model.remarks }}</span>
        <textarea
          v-else
          v-model="model.remarks"
          class="wp-textarea"
          rows="3"
          placeholder="Remarks, delays, catch-up plans, etc."
        ></textarea>
      </div>
    </article>

    <!-- Prepared by / Reviewed by -->
    <article class="wp-card wp-signoff">
      <header class="wp-signoff-head">
        <h2>Sign-Off &amp; Approvals</h2>
      </header>
      <div class="wp-signoff-grid">
        <div class="wp-sign-panel">
          <span class="wp-sign-label">Prepared By</span>
          <span class="wp-sign-name">{{ preparedByName }}</span>
          <span class="wp-sign-role">Project Leader</span>
          <span v-if="preparedStatus" class="wp-status" :class="`wp-status--${preparedStatus.tone}`">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><path d="m9 11 3 3L22 4"/></svg>
            {{ preparedStatus.text }}
          </span>
        </div>
        <div class="wp-sign-panel">
          <span class="wp-sign-label">Reviewed By</span>
          <span class="wp-sign-name">{{ DESIGNATED_REVIEWER_NAME }}</span>
          <span class="wp-sign-role">{{ DESIGNATED_REVIEWER_POSITION }}</span>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import PercentageInput from '@/components/PercentageInput.vue'
import {
  averagePercentage,
  formatPercentage,
  percentageFormValue,
} from '@/utils/percentageInput.js'

/**
 * Interactive "MSUN Internally Funded Project Workplan" form.
 *
 * The proponent completes the workplan for reporting stages 1-3 directly in the
 * system; authorized reviewers see the same component read-only. The data is
 * bound with v-model and is saved/submitted through the existing quarterly
 * progress report workflow — the component never persists anything itself.
 */
const props = defineProps({
  readonly: { type: Boolean, default: false },
  // The report state shown in the Sign-Off card. It never claims that a
  // signature or an approval has been recorded when the report is not submitted.
  reportStatus: { type: String, default: 'DRAFT' },
})

const model = defineModel({
  type: Object,
  default: () => ({
    asOf: '',
    projectTitle: '',
    projectLeader: '',
    projectDuration: '',
    objectives: [],
    remarks: '',
  }),
})

const MONTH_COUNT = 12
const MIN_ROWS = 3
const minRows = MIN_ROWS
const months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

// The reviewer designated on the report template. Showing this designation is
// not evidence that a review happened: the Quarterly Progress Report workflow
// records DRAFT and SUBMITTED only, so the indicator below stays pending and the
// card never claims a verified signature or an approval date.
const DESIGNATED_REVIEWER_NAME = 'ANABELLE DECE A. ESPADERO, PhD'
const DESIGNATED_REVIEWER_POSITION = 'Director, Research and Innovation Institute'

// The project leader from the report/proposal snapshot when available.
const preparedByName = computed(() => (model.value?.projectLeader || '').trim() || '—')

// The report workflow records DRAFT and SUBMITTED only. A draft carries no
// sign-off status text; a submitted report states that fact. Neither state
// claims a digital signature that the system has not verified.
const preparedStatus = computed(() =>
  props.reportStatus === 'SUBMITTED'
    ? { tone: 'good', text: 'Report submitted' }
    : null,
)

function emptyObjective() {
  return { objective: '', months: Array(MONTH_COUNT).fill(''), total: '' }
}

function normalizeObjective(o) {
  const src = Array.isArray(o?.months) ? o.months : []
  const list = src.slice(0, MONTH_COUNT).map((m) => percentageFormValue(m))
  while (list.length < MONTH_COUNT) list.push('')
  // The stored TOTAL is always replaced by the value derived from the month cells.
  return { objective: o?.objective || '', months: list, total: averagePercentage(list) }
}

// Keep the bound object structurally valid (twelve month cells per row, and at
// least the minimum number of objective rows while editing) without discarding
// any entered data.
function normalizeModel() {
  const m = model.value
  if (!m) return
  if (typeof m.asOf !== 'string') m.asOf = ''
  if (typeof m.projectTitle !== 'string') m.projectTitle = ''
  if (typeof m.projectLeader !== 'string') m.projectLeader = ''
  if (typeof m.projectDuration !== 'string') m.projectDuration = ''
  if (typeof m.remarks !== 'string') m.remarks = ''
  const list = Array.isArray(m.objectives) ? m.objectives.map(normalizeObjective) : []
  if (!props.readonly) {
    while (list.length < MIN_ROWS) list.push(emptyObjective())
  }
  m.objectives = list
}

watch(() => model.value, normalizeModel, { immediate: true })

const objectives = computed(() => model.value?.objectives || [])

function monthValue(obj, index) {
  return (obj?.months && obj.months[index]) || ''
}

/** The automatic AVERAGE TOTAL shown for a row, calculated from its month cells. */
function averageTotal(obj) {
  const average = averagePercentage(obj?.months)
  return average === '' ? '—' : `${average}%`
}

function addObjective() {
  model.value.objectives.push(emptyObjective())
}

function removeObjective() {
  if (model.value.objectives.length > MIN_ROWS) {
    model.value.objectives.pop()
  }
}
</script>

<style scoped>
/* ============================================================
   Project Workplan (page 3), Remarks and Sign-Off
   ============================================================ */
.wp-doc {
  display: flex;
  flex-direction: column;
  gap: 18px;
  color: #172b4d;
}

.wp-card {
  background: #ffffff;
  border: 1px solid #dce5f0;
  border-radius: 10px;
  box-shadow: 0 1px 2px rgba(16, 27, 51, 0.04);
  overflow: hidden;
}

/* ---------- Card headers ---------- */
.wp-card-head {
  display: flex;
  align-items: center;
  gap: 11px;
  padding: 14px 18px;
}

.wp-card-head--navy {
  padding: 12px 18px;
  background: #101b33;
  color: #ffffff;
}

.wp-head-icon {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 27px;
  height: 27px;
  border-radius: 7px;
}

.wp-head-icon svg {
  width: 15px;
  height: 15px;
}

.wp-card-head--navy .wp-head-icon {
  background: rgba(255, 255, 255, 0.09);
  color: #ffd45e;
}

.wp-head-icon--blue {
  background: #eaf0fd;
  color: #2454e8;
}

.wp-head-text {
  min-width: 0;
}

.wp-card-title {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.35;
  color: #172b4d;
}

.wp-card-head--navy .wp-card-title {
  font-size: 14.5px;
  color: #ffffff;
}

.wp-card-sub {
  margin: 2px 0 0;
  font-size: 10.5px;
  line-height: 1.4;
  color: #b9c6da;
}

.wp-head-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.wp-as-of {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  flex: none;
  font-size: 9.5px;
  font-weight: 600;
  letter-spacing: 0.05em;
  text-transform: uppercase;
  white-space: nowrap;
  color: #b9c6da;
}

.wp-as-of input {
  padding: 4px 6px;
  border: 1px solid rgba(255, 255, 255, 0.22);
  border-radius: 5px;
  background: rgba(255, 255, 255, 0.08);
  color: #ffffff;
  font: inherit;
  font-size: 10.5px;
  color-scheme: dark;
  outline: none;
}

.wp-as-of-text {
  font-size: 10px;
  color: #b9c6da;
  white-space: nowrap;
}

.wp-btn-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 11px;
  border: 1px solid #2454e8;
  border-radius: 6px;
  background: #2454e8;
  color: #ffffff;
  font: inherit;
  font-size: 10.5px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.wp-btn-add:hover {
  background: #1c46c7;
  border-color: #1c46c7;
}

/* ---------- Workplan table ---------- */
.wp-table-scroll {
  overflow-x: auto;
}

.wp-table {
  width: 100%;
  min-width: 1080px;
  border-collapse: collapse;
  table-layout: fixed;
}

.wp-table th {
  padding: 9px 3px;
  background: #eff3f8;
  border-bottom: 1px solid #dce5f0;
  color: #40506b;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.05em;
  line-height: 1.3;
  text-align: center;
  text-transform: uppercase;
}

.wp-table .col-objectives {
  width: 30%;
}

.wp-table .col-month {
  width: 5.1%;
}

.wp-table .col-total {
  width: 8.8%;
}

/* Below full desktop width the objectives and total columns give back some
   space so all twelve months and the derived total stay visible on one screen
   instead of being cut off at the card edge. */
@media (max-width: 1439px) {
  .wp-table {
    min-width: 0;
  }

  .wp-table .col-objectives {
    width: 26%;
  }

  .wp-table .col-month {
    width: 5.25%;
  }

  .wp-table .col-total {
    width: 11%;
  }

  .wp-table th {
    padding: 9px 2px;
  }

  .wp-table td {
    padding: 6px 2px;
  }

  .th-objectives {
    padding-left: 10px !important;
  }

  .td-objectives {
    padding-left: 10px !important;
    padding-right: 6px !important;
  }
}

/* On phones the month grid keeps a usable cell width and scrolls sideways. */
@media (max-width: 760px) {
  .wp-table {
    min-width: 720px;
  }
}

.th-objectives {
  padding-left: 14px !important;
  text-align: left !important;
}

.th-total {
  background: #e9effa !important;
  border-left: 1px solid #dce5f0;
  color: #172b4d !important;
  font-size: 8.5px !important;
  line-height: 1.25;
  white-space: normal;
}

.wp-table td {
  padding: 6px 3px;
  border-bottom: 1px solid #edf1f7;
  vertical-align: middle;
}

.wp-table tbody tr:last-child td {
  border-bottom: 0;
}

.td-objectives {
  padding-left: 12px !important;
  padding-right: 8px !important;
}

.wp-objective-input {
  box-sizing: border-box;
  display: block;
  width: 100%;
  min-height: 32px;
  padding: 6px 8px;
  border: 1px solid #dce5f0;
  border-radius: 5px;
  background: #ffffff;
  color: #172b4d;
  font: inherit;
  font-size: 11px;
  line-height: 1.4;
  resize: vertical;
  outline: none;
}

.wp-objective-input:focus {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.1);
}

.wp-objective-text {
  display: block;
  padding-right: 4px;
  font-size: 11px;
  line-height: 1.45;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.td-month {
  text-align: center;
}

.td-month :deep(.percentage-input) {
  width: 100%;
}

.td-month :deep(.percentage-input__field) {
  box-sizing: border-box;
  width: 100%;
  min-height: 32px;
  padding: 5px 3px;
  border: 1px solid #dce5f0;
  border-radius: 5px;
  background: #ffffff;
  color: #172b4d;
  font-size: 11px;
  text-align: center;
}

.td-month :deep(.percentage-input__field:focus) {
  border-color: #2454e8;
  box-shadow: 0 0 0 2px rgba(36, 84, 232, 0.1);
}

/* The month cells are percentages already ("in %" in the card subtitle), so
   the repeated sign is hidden here to keep all twelve columns legible. */
.td-month :deep(.percentage-input__symbol) {
  display: none;
}

.wp-month-value {
  display: block;
  padding: 7px 2px;
  font-size: 11px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* AVERAGE TOTAL: the derived summary figure of the row. */
.td-total {
  padding: 5px 4px !important;
  border-left: 1px solid #dce5f0;
  background: #f3f7fd;
  text-align: center;
}

.wp-total-value {
  display: block;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
  color: #172b4d;
  text-align: center;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.wp-table-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 16px 12px;
}

.wp-hint {
  margin: 0;
  font-size: 10px;
  line-height: 1.4;
  color: #7a879c;
}

.wp-btn-remove {
  flex: none;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 5px 9px;
  border: 1px solid #f0c9c9;
  border-radius: 6px;
  background: #ffffff;
  color: #d64545;
  font: inherit;
  font-size: 10px;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
}

.wp-btn-remove svg {
  width: 13px;
  height: 13px;
}

.wp-btn-remove:hover:not(:disabled) {
  background: #fef2f2;
}

.wp-btn-remove:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

/* ---------- Remarks ---------- */
.wp-card-body {
  padding: 0 18px 18px;
}

.wp-textarea {
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

.wp-textarea:focus {
  border-color: #2454e8;
  box-shadow: 0 0 0 3px rgba(36, 84, 232, 0.1);
}

.wp-note-text {
  display: block;
  font-size: 11.5px;
  line-height: 1.55;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

/* ---------- Sign-Off & Approvals ---------- */
.wp-signoff-head {
  padding: 14px 18px 0;
}

.wp-signoff-head h2 {
  margin: 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e6edf6;
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #63708a;
}

.wp-signoff-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 16px 18px 18px;
}

.wp-sign-panel {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
  padding: 14px 16px;
  border: 1px solid #e3eaf4;
  border-radius: 8px;
  background: #fbfcfe;
}

.wp-sign-label {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: #7a879c;
}

.wp-sign-name {
  min-height: 24px;
  padding: 2px 0 9px;
  border-bottom: 1px solid #c9d5e5;
  font-size: 15.5px;
  font-weight: 700;
  line-height: 1.35;
  color: #172b4d;
  overflow-wrap: anywhere;
}

.wp-sign-role {
  font-size: 11px;
  line-height: 1.4;
  color: #63708a;
}

.wp-status {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  margin-top: 4px;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.35;
}

.wp-status svg {
  flex: none;
  width: 13px;
  height: 13px;
}

.wp-status--good {
  color: #0e9f6e;
}

.wp-status--pending {
  color: #b7791f;
}

.wp-status--muted {
  color: #7a879c;
}

/* ---------- Responsive ---------- */
@media (max-width: 900px) {
  .wp-signoff-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .wp-card-head {
    flex-wrap: wrap;
  }

  .wp-head-actions {
    width: 100%;
    justify-content: flex-start;
    margin-left: 0;
  }

  .wp-table-foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
