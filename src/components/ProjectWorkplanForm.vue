<template>
  <div class="doc-scroll">
    <!-- ==================== PAGE 3 - PROJECT WORKPLAN ==================== -->
    <section class="doc-sheet">
      <div class="doc-header">
        <span class="doc-ring doc-ring-1" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-2" aria-hidden="true"></span>
        <span class="doc-ring doc-ring-3" aria-hidden="true"></span>

        <div class="doc-header-left">
          <span class="doc-emblem" aria-hidden="true"></span>
          <div class="doc-header-text">
            <span class="doc-eyebrow">MSUN Internally-Funded Project</span>
            <h2 class="doc-title">Project Workplan</h2>
          </div>
        </div>

        <div class="doc-header-right">
          <span class="doc-badge">As of: {{ model.asOf || '—' }}</span>
          <input
            v-if="!readonly"
            v-model="model.asOf"
            type="date"
            class="doc-badge-input"
            aria-label="Workplan as-of date"
          />
        </div>
      </div>
      <div class="doc-divider"></div>

      <div class="doc-body">
        <!-- (1)-(3) Project information strip -->
        <table class="doc-info-table wp-strip">
          <colgroup>
            <col class="col-title" />
            <col class="col-leader" />
            <col class="col-duration" />
          </colgroup>
          <tbody>
            <tr>
              <td class="doc-cream">
                <span class="doc-label">(1) Project Title</span>
                <input v-if="!readonly" v-model="model.projectTitle" class="doc-cell-input" />
                <span v-else class="doc-value">{{ model.projectTitle || '' }}</span>
              </td>
              <td>
                <span class="doc-label">(2) Project Leader</span>
                <input v-if="!readonly" v-model="model.projectLeader" class="doc-cell-input" />
                <span v-else class="doc-value">{{ model.projectLeader || '' }}</span>
              </td>
              <td>
                <span class="doc-label">(3) Duration (Months)</span>
                <!-- Fixed at 12 months for internally funded projects; the workplan
                     must not contradict the proposal's approved duration. -->
                <input v-if="!readonly" v-model="model.projectDuration" class="doc-cell-input" readonly />
                <span v-else class="doc-value">{{ model.projectDuration || '' }}</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Monthly target activities and accomplishments -->
        <table class="doc-table wp-table">
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
              <td class="td-objectives doc-cell-cream">
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

        <!-- Remarks -->
        <p class="doc-label wp-remarks-label">Remarks (catch-up plan, etc.)</p>
        <div class="doc-note">
          <textarea
            v-if="!readonly"
            v-model="model.remarks"
            class="wp-note-area"
            rows="2"
            placeholder="Remarks, delays, catch-up plans, etc."
          ></textarea>
          <span v-else class="wp-note-text">{{ model.remarks }}</span>
        </div>

        <!-- Prepared by / Reviewed by -->
        <div class="wp-signatures">
          <div class="wp-sign">
            <span class="doc-label">Prepared by</span>
            <span class="wp-sign-name">{{ preparedByName }}</span>
            <span class="wp-sign-line"></span>
            <span class="wp-sign-role">Project Leader</span>
          </div>
          <div class="wp-sign">
            <span class="doc-label">Reviewed by</span>
            <span class="wp-sign-name">{{ REVIEWER_NAME }}</span>
            <span class="wp-sign-line"></span>
            <span class="wp-sign-role">{{ REVIEWER_DESIGNATION }}</span>
          </div>
        </div>
      </div>

      <div class="doc-footer">
        <span class="doc-footer-side">MSUN Internally-Funded Project &middot; Project Workplan</span>
        <span class="doc-footer-center">{{ model.projectTitle || '—' }}</span>
        <span class="doc-page-badge">Page 3 of 3</span>
      </div>
    </section>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
// Shared document shell (page, header, footer, tables, print rules) used by the
// Quarterly Progress Report pages and the Project Workplan page.
import '@/assets/reportDocument.css'

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
/* (1)-(3) Project information strip proportions. */
.wp-strip .col-title {
  width: 44%;
}

.wp-strip .col-leader {
  width: 30%;
}

.wp-strip .col-duration {
  width: 26%;
}

/* Values inside the strip read as printed text while staying editable inputs. */
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

.doc-cell-input[readonly] {
  color: #202044;
}

/* Workplan table column proportions: OBJECTIVES wider than each month column. */
.wp-table .col-objectives {
  width: 11%;
}

.wp-table .col-month {
  width: 6.9%;
}

.wp-table .col-total {
  width: 6.2%;
}

.th-objectives,
.th-total,
.th-months,
.th-month {
  text-align: center;
  vertical-align: middle;
  font-weight: 700;
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

.wp-cell-area {
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
  line-height: 1.25;
  resize: vertical;
  outline: none;
}

.wp-cell-month,
.wp-cell-total {
  font-size: 8.5px;
}

.wp-cell-text {
  display: block;
  white-space: pre-wrap;
  word-break: break-word;
}

.wp-row-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}

.wp-btn-add,
.wp-btn-remove {
  font-family: inherit;
  font-size: 9px;
  font-weight: 700;
  padding: 4px 9px;
  border-radius: 4px;
  cursor: pointer;
}

.wp-btn-add {
  background: #f1f0f8;
  color: #25224f;
  border: 1px dashed #25224f;
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
  font-size: 8.5px;
  color: #64647d;
}

/* Remarks */
.wp-remarks-label {
  margin: 10px 0 4px;
}

.wp-note-area {
  display: block;
  width: 100%;
  box-sizing: border-box;
  min-height: 30px;
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

.wp-note-text {
  display: block;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 9.5px;
}

/* Prepared by / Reviewed by */
.wp-signatures {
  display: flex;
  justify-content: space-between;
  gap: 22px;
  margin-top: 22px;
}

.wp-sign {
  display: flex;
  flex-direction: column;
  width: 48%;
  min-width: 0;
}

.wp-sign-name {
  margin-top: 12px;
  font-size: 10.5px;
  font-weight: 600;
  text-align: center;
  color: #202044;
  overflow-wrap: anywhere;
}

.wp-sign-line {
  height: 0;
  margin: 3px 0 3px;
  border-bottom: 1px solid #8f8fa8;
}

.wp-sign-role {
  font-size: 8.5px;
  text-align: center;
  color: #64647d;
}

/* Stack the two signature areas on smaller screens so they never overlap. */
@media (max-width: 720px) {
  .wp-signatures {
    flex-direction: column;
    gap: 14px;
  }

  .wp-sign {
    width: 100%;
  }
}
</style>
