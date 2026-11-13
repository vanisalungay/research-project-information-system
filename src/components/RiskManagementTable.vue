<template>
  <div class="rmp">
    <p class="rmp-instruction">{{ RISK_SECTION_INSTRUCTION }}</p>

    <div class="rmp-table-wrapper">
      <table class="rmp-table">
        <thead>
          <tr>
            <th class="rmp-index-col">#</th>
            <th v-for="column in RISK_COLUMNS" :key="column.key">{{ column.label }}</th>
            <th v-if="editable" class="rmp-action-col">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(row, index) in rows"
            :key="index"
            :class="{ 'rmp-row--error': showValidation && hasRowErrors(index) }"
          >
            <td class="rmp-index-col">{{ index + 1 }}</td>
            <td
              v-for="column in RISK_COLUMNS"
              :key="column.key"
              :data-label="column.label"
              class="rmp-cell"
            >
              <!-- Likelihood and Impact are dropdowns: only High, Medium or Low can be
                   selected, and nothing is pre-selected until the proponent chooses. -->
              <template v-if="isLevelColumn(column.key)">
                <select
                  v-if="editable"
                  v-model="row[column.key]"
                  class="rmp-select"
                  :class="{ 'rmp-input--error': showValidation && errorFor(index, column.key) }"
                  :aria-label="`${column.label} of risk entry ${index + 1}`"
                >
                  <option value="">Select...</option>
                  <option v-for="level in RISK_LEVELS" :key="level" :value="level">{{ level }}</option>
                </select>
                <span v-else class="rmp-readonly-value">{{ row[column.key] || '-' }}</span>
              </template>

              <template v-else>
                <textarea
                  v-if="editable"
                  v-model="row[column.key]"
                  class="rmp-textarea"
                  :class="{ 'rmp-input--error': showValidation && errorFor(index, column.key) }"
                  rows="2"
                  :placeholder="placeholderFor(column.key)"
                  :aria-label="`${column.label} of risk entry ${index + 1}`"
                ></textarea>
                <span v-else class="rmp-readonly-value">{{ row[column.key] || '-' }}</span>
              </template>

              <span
                v-if="editable && showValidation && errorFor(index, column.key)"
                class="rmp-field-error"
              >
                {{ errorFor(index, column.key) }}
              </span>
            </td>
            <td v-if="editable" class="rmp-action-col" data-label="Action">
              <button
                type="button"
                class="rmp-remove"
                :title="`Remove risk entry ${index + 1}`"
                :aria-label="`Remove risk entry ${index + 1}`"
                @click="removeRow(index)"
              >
                ✕
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p v-if="!rows.length" class="rmp-empty">No risk entry yet. Add one below.</p>

    <button v-if="editable" type="button" class="rmp-add" @click="addRow">+ Add Risk</button>
  </div>
</template>


<script setup>
/**
 * RiskManagementTable.vue
 * ---------------------------------------------------------------------------
 * Section 21 - Risk Management Plan, presented as a table instead of one big text
 * field: one row per identified risk with its likelihood (dropdown), its impact
 * (dropdown) and its mitigation measure, plus an "+ Add Risk" button below the table
 * and a Remove action on every row.
 *
 * The rows are the `v-model` value, so the parent form keeps editing the exact array
 * the payload is built from; adding or removing a row never touches the other rows.
 *
 * Validation messages come from src/utils/riskManagement.js (the same module the
 * submission checks use), so the message beside a field and the message that blocks
 * the submission can never disagree.
 */
import { computed } from 'vue'
import {
  RISK_COLUMNS,
  RISK_LEVELS,
  RISK_SECTION_INSTRUCTION,
  blankRiskEntry,
  validateRiskEntries
} from '@/utils/riskManagement'

/** The rows, edited in place by the parent form. */
const rows = defineModel({ type: Array, default: () => [blankRiskEntry()] })

const props = defineProps({
  /** The form's showValidation flag: messages only appear after a submit attempt. */
  showValidation: { type: Boolean, default: false },
  /** Read-only rendering, for a page that only displays the plan. */
  editable: { type: Boolean, default: true }
})

/** index -> { columnKey: message }, computed from the very same rule the submit uses. */
const rowErrors = computed(() => (props.editable ? validateRiskEntries(rows.value).rowErrors : []))

const isLevelColumn = (key) => key === 'likelihood' || key === 'impact'

const hasRowErrors = (index) => Object.keys(rowErrors.value[index] || {}).length > 0

const errorFor = (index, columnKey) => (rowErrors.value[index] || {})[columnKey] || ''

const placeholderFor = (key) =>
  key === 'identifiedRisks'
    ? 'e.g. Delayed delivery of research equipment'
    : 'e.g. Identify alternative suppliers and coordinate procurement in advance.'

/** Adds one empty row; the rows already there keep everything the proponent typed. */
const addRow = () => {
  rows.value = [...rows.value, blankRiskEntry()]
}

/** Removes exactly one entry. A table with no row left gets a fresh empty one. */
const removeRow = (index) => {
  const next = rows.value.filter((_, position) => position !== index)
  rows.value = next.length ? next : [blankRiskEntry()]
}
</script>

<style scoped>
/* The same visual language as the proposal form's own tables (form-table, btn-add):
   light header band, thin row rules and a dashed Add button, so section 21 does not
   look like a different form. */
.rmp {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.rmp-instruction {
  margin: 0;
  padding: 10px 14px;
  background: #f8faff;
  border-left: 4px solid #4f46e5;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: #475569;
}

.rmp-table-wrapper {
  overflow-x: auto;
}

.rmp-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.rmp-table th {
  background: #f8fafc;
  color: #1e293b;
  font-weight: 700;
  padding: 10px 12px;
  text-align: left;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.rmp-table td {
  padding: 8px;
  border-bottom: 1px solid #f1f5f9;
  vertical-align: top;
}

.rmp-index-col {
  width: 34px;
  text-align: center;
  font-weight: 600;
  color: #94a3b8;
}

.rmp-action-col {
  width: 60px;
  text-align: center;
}

.rmp-cell {
  min-width: 150px;
}

.rmp-textarea,
.rmp-select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 13px;
  font-family: inherit;
  color: #1e293b;
  background: #ffffff;
  resize: vertical;
}

.rmp-select {
  cursor: pointer;
}

.rmp-textarea:focus,
.rmp-select:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 2px rgba(79, 70, 229, 0.08);
}

/* An unselected dropdown is visibly empty rather than looking answered. */
.rmp-select:invalid,
.rmp-select option[value=''] {
  color: #94a3b8;
}

.rmp-input--error {
  border-color: #dc2626;
  background: #fef2f2;
}

.rmp-row--error td {
  background: #fffbfb;
}

.rmp-field-error {
  display: block;
  margin-top: 4px;
  font-size: 11.5px;
  font-weight: 600;
  color: #dc2626;
}

.rmp-readonly-value {
  display: block;
  font-size: 13px;
  line-height: 1.5;
  color: #1e293b;
}

.rmp-remove {
  width: 30px;
  height: 30px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 6px;
  background: none;
  color: #94a3b8;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.15s;
}

.rmp-remove:hover {
  background: #fee2e2;
  color: #dc2626;
}

.rmp-add {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: 1px dashed #cbd5e1;
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  color: #4f46e5;
  cursor: pointer;
  transition: all 0.15s;
}

.rmp-add:hover {
  border-color: #4f46e5;
  background: #eef2ff;
}

.rmp-empty {
  margin: 0;
  font-size: 13px;
  color: #64748b;
}

/* On a phone the table becomes a stack of labelled cards, so no column (and no
   dropdown) is cut off or scrolled out of reach. */
@media (max-width: 640px) {
  .rmp-table thead {
    display: none;
  }

  .rmp-table,
  .rmp-table tbody,
  .rmp-table tr,
  .rmp-table td {
    display: block;
    width: 100%;
  }

  .rmp-table tr {
    border: 1px solid #e2e8f0;
    border-radius: 8px;
    margin-bottom: 10px;
    padding: 6px 8px;
  }

  .rmp-table td {
    border-bottom: none;
    padding: 4px 0;
  }

  .rmp-index-col {
    text-align: left;
    width: auto;
  }

  .rmp-cell::before {
    content: attr(data-label);
    display: block;
    margin-bottom: 3px;
    font-size: 11px;
    font-weight: 700;
    letter-spacing: 0.05em;
    text-transform: uppercase;
    color: #64748b;
  }

  .rmp-action-col {
    width: auto;
    text-align: left;
  }
}
</style>
