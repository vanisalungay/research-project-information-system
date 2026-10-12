<template>
  <div class="wp-scroll">
    <div class="wp-doc">
      <!-- (A) Form heading -->
      <div class="wp-heading">
        <h2>MSUN INTERNALLY FUNDED PROJECT WORKPLAN</h2>
        <p class="wp-asof">
          <span class="wp-asof-label">As of:</span>
          <input v-if="!readonly" v-model="model.asOf" type="date" class="wp-date" />
          <span v-else class="wp-asof-value">{{ model.asOf || '__________' }}</span>
        </p>
      </div>

      <!-- (B) Project information -->
      <div class="wp-fields">
        <div class="wp-field">
          <span class="wp-label">(1) Project Title:</span>
          <input v-if="!readonly" v-model="model.projectTitle" class="wp-input" />
          <span v-else class="wp-static">{{ model.projectTitle || '' }}</span>
        </div>
        <div class="wp-field">
          <span class="wp-label">(2) Project Leader:</span>
          <input v-if="!readonly" v-model="model.projectLeader" class="wp-input" />
          <span v-else class="wp-static">{{ model.projectLeader || '' }}</span>
        </div>
        <div class="wp-field">
          <span class="wp-label">(3) Project Duration (number of months):</span>
          <!-- Fixed at 12 months for internally funded projects; the workplan must
               not contradict the proposal's approved duration. -->
          <input v-if="!readonly" v-model="model.projectDuration" class="wp-input wp-input-short" readonly />
          <span v-else class="wp-static">{{ model.projectDuration || '' }}</span>
        </div>
      </div>

      <!-- (C/D/E) Monthly target activities and accomplishments -->
      <table class="wp-table">
        <colgroup>
          <col class="col-objectives" />
          <col v-for="m in months" :key="m" class="col-month" />
          <col class="col-total" />
        </colgroup>
        <thead>
          <tr>
            <th rowspan="2" class="th-objectives">OBJECTIVES</th>
            <th :colspan="months.length" class="th-months">
              MONTHLY TARGET ACTIVITIES AND ACCOMPLISHMENTS (in %)
            </th>
            <th rowspan="2" class="th-total">TOTAL</th>
          </tr>
          <tr>
            <th v-for="m in months" :key="m" class="th-month">{{ m }}</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(obj, i) in objectives"
            :key="i"
            class="row-objective"
            :class="{ 'row-first': i === 0 }"
          >
            <td class="td-objectives">
              <span class="obj-num">{{ i + 1 }}.</span>
              <textarea
                v-if="!readonly"
                v-model="obj.objective"
                class="wp-cell-area"
                rows="4"
                placeholder="Objective description"
              ></textarea>
              <span v-else class="wp-cell-text">{{ obj.objective }}</span>
            </td>
            <td v-for="(m, mi) in months" :key="m" class="td-month">
              <textarea
                v-if="!readonly"
                v-model="obj.months[mi]"
                class="wp-cell-area wp-cell-month"
                rows="4"
                placeholder="Activity / Target accomplishment: __%"
              ></textarea>
              <span v-else class="wp-cell-text">{{ monthValue(obj, mi) }}</span>
            </td>
            <td class="td-total">
              <textarea
                v-if="!readonly"
                v-model="obj.total"
                class="wp-cell-area wp-cell-total"
                rows="3"
                placeholder="%"
              ></textarea>
              <span v-else class="wp-cell-text">{{ obj.total }}</span>
            </td>
          </tr>
          <tr class="row-remarks">
            <td class="td-remarks-label">
              REMARKS
              <span class="remarks-sub">(Catch up plan, etc)</span>
            </td>
            <td :colspan="months.length + 1" class="td-remarks">
              <textarea
                v-if="!readonly"
                v-model="model.remarks"
                class="wp-remarks-area"
                rows="3"
                placeholder="Remarks, delays, catch-up plans, etc."
              ></textarea>
              <span v-else class="wp-cell-text">{{ model.remarks }}</span>
            </td>
          </tr>
        </tbody>
      </table>

      <div v-if="!readonly" class="wp-row-actions">
        <button type="button" class="wp-btn-add" @click="addObjective">+ Add objective row</button>
        <button
          type="button"
          class="wp-btn-remove"
          :disabled="objectives.length <= minRows"
          @click="removeObjective"
        >
          − Remove last row
        </button>
        <span class="wp-hint">
          TOTAL is entered manually — the official format does not define an automatic calculation.
        </span>
      </div>

      <!-- (G) Prepared by / Reviewed by -->
      <div class="wp-footer">
        <div class="wp-sign">
          <span class="wp-sign-label">Prepared by:</span>
          <span class="wp-sign-name">{{ preparedByName }}</span>
          <span class="wp-sign-line"></span>
          <span class="wp-sign-role">Project Leader</span>
        </div>
        <div class="wp-sign">
          <span class="wp-sign-label">Reviewed by:</span>
          <span class="wp-sign-name">{{ REVIEWER_NAME }}</span>
          <span class="wp-sign-line"></span>
          <span class="wp-sign-role">{{ REVIEWER_DESIGNATION }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'

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

// Official ORPS reviewer printed on the workplan. The signature line stays blank
// and no review/approval status is implied until the system actually records one.
const REVIEWER_NAME = 'ANABELLE DECE A. ESPADERO, PhD'
const REVIEWER_DESIGNATION = 'Director, Research and Publication Services'

// The actual project leader's name from the report/proposal when available;
// otherwise the official template's placeholder text is shown.
const preparedByName = computed(() => (model.value?.projectLeader || '').trim() || 'Name of Project Leader')

function emptyObjective() {
  return { objective: '', months: Array(MONTH_COUNT).fill(''), total: '' }
}

function normalizeObjective(o) {
  const src = Array.isArray(o?.months) ? o.months : []
  const list = src.slice(0, MONTH_COUNT).map((m) => (typeof m === 'string' ? m : ''))
  while (list.length < MONTH_COUNT) list.push('')
  return { objective: o?.objective || '', months: list, total: o?.total || '' }
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
/* Horizontally scrollable on small screens so the twelve month columns stay
   usable instead of being compressed. */
.wp-scroll {
  overflow-x: auto;
  margin-bottom: 12px;
}

/* ============================================================
   Light "paper document" theme guard (mirrors QuarterlyReportForm).
   The workplan is a printable MSUN form and must always render as a light
   document regardless of the OS-level dark mode the rest of the app adapts to.
   ============================================================ */
.wp-doc {
  color-scheme: light;
  background: #fff !important;
  border: 2px solid #1c2145 !important;
  color: #0f172a !important;
  padding: 12px 14px 14px;
  min-width: 940px;
  font-family: Arial, sans-serif;
  font-size: 11px;
  line-height: 1.25;
}

.wp-doc input,
.wp-doc textarea {
  background: transparent !important;
  color: #0f172a !important;
  border-radius: 0 !important;
  font-family: inherit;
}

.wp-heading {
  text-align: center;
  margin-bottom: 8px;
}
.wp-heading h2 {
  margin: 0;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.4px;
  text-transform: uppercase;
  color: #0f172a !important;
}
.wp-asof {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin: 4px 0 0;
  font-size: 11px;
  color: #0f172a !important;
}
.wp-asof-label {
  font-weight: 700;
}
.wp-date {
  width: 150px;
  padding: 1px 4px;
  font: inherit;
  font-size: 11px;
  cursor: pointer;
  border: none !important;
  border-bottom: 1px solid #1c2145 !important;
}
.wp-asof-value {
  font-weight: 700;
}

.wp-fields {
  margin-bottom: 8px;
}
.wp-field {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 3px;
}
.wp-label {
  font-weight: 700;
  white-space: nowrap;
  color: #0f172a !important;
}
.wp-input {
  flex: 1;
  min-width: 0;
  font: inherit;
  font-size: 11px;
  padding: 1px 4px;
  border: none !important;
  border-bottom: 1px solid #94a3b8 !important;
}
.wp-input-short {
  flex: 0 0 90px;
}
.wp-static {
  flex: 1;
  padding: 1px 4px;
  border-bottom: 1px solid #94a3b8;
  color: #0f172a !important;
}

.wp-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}
.wp-table,
.wp-table tr {
  background-color: transparent !important;
}
.wp-table th,
.wp-table td {
  border: 1px solid #1c2145 !important;
  padding: 3px 4px;
  font-size: 10px;
  font-weight: 400;
  vertical-align: top;
  text-align: left;
  color: #0f172a !important;
  background-color: transparent !important;
}
.wp-table tbody tr:hover td {
  background-color: transparent !important;
}

/* Column proportions match the reference: OBJECTIVES ~11%, each month ~7%,
   TOTAL ~6%. */
.col-objectives {
  width: 11%;
}
.col-month {
  width: 6.9%;
}
.col-total {
  width: 6.2%;
}

.th-objectives,
.th-total {
  text-align: center !important;
  vertical-align: middle !important;
  font-weight: 700 !important;
}
.th-months {
  text-align: center !important;
  font-weight: 700 !important;
}
.th-month {
  text-align: center !important;
  font-weight: 700 !important;
  font-size: 9px !important;
}

.obj-num {
  font-weight: 700;
}
/* The first objective row is taller to hold longer descriptions and multi-line
   monthly activities. */
.row-first td {
  height: 96px;
}
.row-objective td {
  height: 44px;
}
.row-remarks td {
  height: 56px;
}

.wp-cell-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 34px;
  padding: 0;
  border: none !important;
  font: inherit;
  font-size: 10px;
  line-height: 1.25;
  resize: vertical;
  outline: none;
}
.wp-cell-month,
.wp-cell-total {
  font-size: 9px;
}
.wp-cell-total {
  min-height: 24px;
}
.wp-cell-text {
  display: block;
  white-space: pre-wrap;
  word-break: break-word;
}
.wp-remarks-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 40px;
  padding: 0;
  border: none !important;
  font: inherit;
  font-size: 10px;
  resize: vertical;
  outline: none;
}

.td-remarks-label {
  font-weight: 700 !important;
}
.remarks-sub {
  display: block;
  font-size: 9px;
  font-weight: 400 !important;
}

.wp-row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.wp-btn-add,
.wp-btn-remove {
  font-family: inherit;
  font-size: 11px;
  font-weight: 700;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
}
.wp-btn-add {
  background: #eef2ff;
  color: #1c2145;
  border: 1px solid #1c2145;
}
.wp-btn-remove {
  background: #fee2e2;
  color: #b91c1c;
  border: 1px solid #fca5a5;
}
.wp-btn-remove:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.wp-hint {
  font-size: 10px;
  color: #64748b;
}

.wp-footer {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 24px;
  margin-top: 12px;
}
.wp-sign {
  display: flex;
  flex-direction: column;
  width: 44%;
  min-width: 220px;
}
.wp-sign-label {
  font-size: 10px;
  color: #0f172a !important;
}
.wp-sign-name {
  margin-top: 4px;
  font-size: 10px;
  font-weight: 700;
  text-align: center;
  color: #0f172a !important;
  overflow-wrap: anywhere;
}
.wp-sign-line {
  height: 0;
  margin: 4px 0 3px;
  border-bottom: 1px solid #1c2145;
}
.wp-sign-role {
  font-size: 10px;
  text-align: center;
  color: #0f172a !important;
}

/* Stack the two signature areas on smaller screens so they never overlap. */
@media (max-width: 720px) {
  .wp-footer {
    flex-direction: column;
    gap: 14px;
  }
  .wp-sign {
    width: 100%;
  }
}
</style>

