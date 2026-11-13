<template>
  <div class="gad">
    <p class="gad-instruction">{{ GAD_SECTION_INSTRUCTION }}</p>

    <div class="gad-card">
      <h4 class="gad-title">{{ GAD_CHECKLIST_TITLE }}</h4>
      <p class="gad-note">{{ GAD_CHECKLIST_INSTRUCTION }}</p>

      <div class="gad-table" :class="{ 'is-readonly': readonly }">
        <div class="gad-head">
          <span v-for="(column, index) in GAD_COLUMNS" :key="index" class="gad-head-cell">
            {{ column }}
          </span>
        </div>

        <template v-for="criterion in GAD_CRITERIA" :key="criterion.number">
          <div class="gad-criterion">
            <span class="gad-criterion-heading">{{ criterion.number }} {{ criterion.title }}</span>
            <span v-if="criterion.note" class="gad-criterion-note">{{ criterion.note }}</span>
            <span class="gad-criterion-subtotal">
              Subtotal {{ formatGadScore(summary.subtotals[criterion.number]) }} / 2
            </span>
          </div>

          <div v-for="item in criterion.items" :key="item.key" class="gad-row">
            <div class="gad-cell gad-cell--question">
              <span class="gad-item-key">{{ item.key }}</span>
              <p class="gad-item-question">{{ item.question }}</p>
              <p v-if="item.alt" class="gad-item-alt">
                <span class="gad-or">OR</span> {{ item.alt }}
              </p>
              <p v-if="item.conditional" class="gad-item-conditional">{{ item.conditional }}</p>
              <p class="gad-item-scores">Possible scores — {{ gadItemScoreLabel(item.scale) }}</p>
            </div>

            <div class="gad-cell gad-cell--done" data-label="Done?">
              <label v-for="option in GAD_RESPONSE_OPTIONS" :key="option.key" class="gad-option">
                <input
                  v-if="!readonly"
                  type="radio"
                  :name="`${uid}-${item.key}`"
                  :value="option.key"
                  v-model="model.responses[item.key]"
                />
                <input
                  v-else
                  type="radio"
                  :name="`${uid}-${item.key}`"
                  :value="option.key"
                  :checked="model.responses[item.key] === option.key"
                  disabled
                />
                <span class="gad-option-label">
                  {{ option.label }}<span class="gad-option-sub">({{ option.sub }})</span>
                </span>
              </label>
              <button
                v-if="!readonly && responseOf(item.key)"
                type="button"
                class="gad-clear"
                @click="clearResponse(item.key)"
              >
                Clear
              </button>
            </div>

            <div class="gad-cell gad-cell--score" data-label="Score">
              <span class="gad-score" :class="{ 'gad-score--empty': itemScore(item.key) === null }">
                {{ itemScore(item.key) === null ? '—' : formatGadScore(itemScore(item.key)) }}
              </span>
            </div>

            <div class="gad-cell gad-cell--comments" data-label="Comments/Gender Issues Identified">
              <textarea
                v-if="!readonly"
                v-model="model.comments[item.key]"
                class="gad-comments"
                rows="3"
                placeholder="Comments / gender issues identified..."
              ></textarea>
              <p v-else class="gad-comments-readonly">{{ model.comments[item.key] || '—' }}</p>
            </div>
          </div>
        </template>
      </div>

      <p v-if="error" class="gad-error">{{ error }}</p>

      <div class="gad-summary">
        <h4 class="gad-summary-title">
          TOTAL GAD SCORE FOR THE PROJECT IDENTIFICATION AND DESIGN STAGES
        </h4>
        <div class="gad-summary-grid">
          <div class="gad-summary-item">
            <span class="gad-summary-label">Total GAD Score</span>
            <span class="gad-summary-value">{{ summary.formattedTotal }}</span>
          </div>
          <div class="gad-summary-item">
            <span class="gad-summary-label">Maximum Score</span>
            <span class="gad-summary-value">{{ summary.maxScore }}</span>
          </div>
          <div class="gad-summary-item">
            <span class="gad-summary-label">Items Answered</span>
            <span class="gad-summary-value">{{ summary.answeredCount }} / {{ summary.itemCount }}</span>
          </div>
        </div>

        <p v-if="!summary.completed" class="gad-summary-incomplete">
          Assessment incomplete — select a response for all {{ summary.itemCount }} items to see the
          final GAD interpretation.
        </p>
        <p v-else class="gad-summary-interpretation">{{ summary.interpretation.label }}</p>
      </div>

      <GadInterpretationTable :active-index="summary.interpretationIndex" />
    </div>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import {
  GAD_CHECKLIST_INSTRUCTION,
  GAD_CHECKLIST_TITLE,
  GAD_COLUMNS,
  GAD_CRITERIA,
  GAD_RESPONSE_OPTIONS,
  GAD_SECTION_INSTRUCTION,
  emptyGadChecklist,
  formatGadScore,
  gadChecklistSummary,
  gadItemScore,
  gadItemScoreLabel,
} from '@/utils/gadChecklist'
import GadInterpretationTable from '@/components/GadInterpretationTable.vue'

/**
 * The interactive GAD Checklist 2 of Section 19. `v-model` is the checklist state
 * ({ responses, comments }). In read-only mode (the reviewer view and the printable
 * document) the same markup renders the recorded answers, scores, comments and total.
 */
const model = defineModel({ type: Object, default: () => emptyGadChecklist() })

const props = defineProps({
  readonly: { type: Boolean, default: false },
  /** Validation message shown under the checklist (empty when every item is answered). */
  error: { type: String, default: '' },
})

// A per-instance id, so the radio groups of two checklists on one screen never merge.
const uid = `gad-${Math.random().toString(36).slice(2, 9)}`

// Keep the bound object structurally valid (responses/comments always present) without
// discarding any entered value.
function ensureShape() {
  const value = model.value
  if (!value || typeof value !== 'object') return
  if (!value.responses || typeof value.responses !== 'object') value.responses = {}
  if (!value.comments || typeof value.comments !== 'object') value.comments = {}
}
watch(model, ensureShape, { immediate: true })

const summary = computed(() => gadChecklistSummary(model.value))

/** The score of one item (null while it is unanswered). */
function itemScore(itemKey) {
  return gadItemScore(model.value, itemKey)
}

/** The chosen response of one item, or '' when it is unanswered. */
function responseOf(itemKey) {
  return model.value?.responses?.[itemKey] || ''
}

/** Clears a response, so the item counts as unanswered again (never as a zero). */
function clearResponse(itemKey) {
  if (props.readonly) return
  delete model.value.responses[itemKey]
}
</script>

<style scoped>
.gad-instruction {
  margin: 0 0 14px;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.gad-card {
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 18px;
  background: #fff;
}

.gad-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.gad-note {
  margin: 0 0 14px;
  font-size: 12px;
  font-style: italic;
  color: #64748b;
}

.gad-table {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.gad-head,
.gad-row {
  display: grid;
  grid-template-columns: 2.4fr 1.15fr 0.7fr 1.9fr;
}

.gad-head {
  background: #f1f5f9;
  font-size: 12px;
  font-weight: 700;
  color: #334155;
}

.gad-head-cell {
  padding: 10px 12px;
  border-right: 1px solid #e2e8f0;
}

.gad-head-cell:last-child {
  border-right: none;
}

.gad-criterion {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px 14px;
  padding: 10px 12px;
  background: #eef2ff;
  border-top: 1px solid #e2e8f0;
  font-weight: 700;
  color: #312e81;
  font-size: 13px;
}

.gad-criterion-heading {
  font-weight: 700;
}

.gad-criterion-note {
  font-weight: 500;
  color: #4f46e5;
  font-size: 12px;
}

.gad-criterion-subtotal {
  margin-left: auto;
  font-weight: 600;
  color: #4338ca;
}

.gad-row {
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.gad-cell {
  padding: 12px;
  border-right: 1px solid #e2e8f0;
  font-size: 13px;
  color: #1e293b;
}

.gad-cell:last-child {
  border-right: none;
}

.gad-item-key {
  display: inline-block;
  min-width: 36px;
  font-weight: 700;
  color: #4338ca;
}

.gad-item-question {
  display: inline;
  margin: 0;
}

.gad-item-alt,
.gad-item-conditional {
  margin: 6px 0 0;
  font-size: 12.5px;
}

.gad-or {
  display: inline-block;
  font-weight: 700;
  color: #b91c1c;
  margin-right: 2px;
}

.gad-item-conditional {
  font-weight: 600;
  color: #92400e;
}

.gad-item-scores {
  margin: 6px 0 0;
  font-size: 11.5px;
  color: #64748b;
}

.gad-option {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
  cursor: pointer;
}

.gad-option input {
  margin: 0;
}

.gad-option-sub {
  margin-left: 4px;
  color: #94a3b8;
  font-size: 11px;
}

.gad-clear {
  margin-top: 6px;
  padding: 2px 8px;
  font-size: 11px;
  border: 1px solid #cbd5e1;
  border-radius: 6px;
  background: #f8fafc;
  color: #475569;
  cursor: pointer;
}

.gad-score {
  font-weight: 700;
  color: #0f172a;
}

.gad-score--empty {
  color: #cbd5e1;
  font-weight: 400;
}

.gad-comments {
  width: 100%;
  border: 1px solid #cbd5e1;
  border-radius: 8px;
  padding: 8px;
  font: inherit;
  resize: vertical;
}

.gad-comments-readonly {
  margin: 0;
  white-space: pre-wrap;
}

.gad-error {
  margin: 12px 0 0;
  color: #b91c1c;
  font-size: 12.5px;
  font-weight: 600;
}

.gad-summary {
  margin-top: 18px;
  border: 1px solid #bbf7d0;
  background: #f0fdf4;
  border-radius: 10px;
  padding: 16px;
}

.gad-summary-title {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 700;
  color: #166534;
}

.gad-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.gad-summary-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.gad-summary-label {
  font-size: 11.5px;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: #15803d;
}

.gad-summary-value {
  font-size: 20px;
  font-weight: 800;
  color: #14532d;
}

.gad-summary-incomplete {
  margin: 12px 0 0;
  font-size: 12.5px;
  color: #b45309;
  font-weight: 600;
}

.gad-summary-interpretation {
  margin: 12px 0 0;
  font-size: 13px;
  color: #14532d;
  font-weight: 600;
}

@media (max-width: 860px) {
  .gad-head {
    display: none;
  }

  .gad-row {
    grid-template-columns: 1fr;
  }

  .gad-cell {
    border-right: none;
    border-top: 1px dashed #e2e8f0;
  }

  .gad-cell:first-child {
    border-top: none;
  }

  .gad-cell--done::before,
  .gad-cell--score::before,
  .gad-cell--comments::before {
    content: attr(data-label);
    display: block;
    font-size: 11px;
    font-weight: 700;
    text-transform: uppercase;
    letter-spacing: 0.4px;
    color: #94a3b8;
    margin-bottom: 6px;
  }

  .gad-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>


