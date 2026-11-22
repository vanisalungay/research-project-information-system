<template>
  <div class="gad">
    <h3 class="gad-sr-only">{{ GAD_CHECKLIST_TITLE }}</h3>

    <!-- COMPACT INSTRUCTION BANNER -->
    <div class="gad-instruction">
      <span class="gad-info-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
          stroke-linecap="round" stroke-linejoin="round">
          <circle cx="12" cy="12" r="9" />
          <path d="M12 11v5" />
          <path d="M12 8h.01" />
        </svg>
      </span>
      <p>{{ GAD_SCORE_UI_INSTRUCTION }}</p>
    </div>

    <!-- ONE CARD PER OFFICIAL GAD CATEGORY -->
    <GadCriterionCard
      v-for="criterion in GAD_CRITERIA"
      :key="criterion.number"
      :criterion="criterion"
      :responses="model.responses"
      :comments="model.comments"
      :subtotal="summary.subtotals[criterion.number]"
      :readonly="readonly"
      @update:response="setResponse"
      @update:comment="setComment"
      @clear="clearResponse"
    />

    <p v-if="error" class="gad-error" role="alert">{{ error }}</p>

    <!-- OVERALL SCORE (the existing calculation, restyled only) -->
    <section class="gad-summary">
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
    </section>

    <GadInterpretationTable :active-index="summary.interpretationIndex" />
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import {
  GAD_CHECKLIST_TITLE,
  GAD_CRITERIA,
  GAD_SCORE_UI_INSTRUCTION,
  emptyGadChecklist,
  gadChecklistSummary,
} from '@/utils/gadChecklist'
import GadCriterionCard from '@/components/GadCriterionCard.vue'
import GadInterpretationTable from '@/components/GadInterpretationTable.vue'

/**
 * The interactive GAD Checklist 2 of Section 19. `v-model` is the checklist state
 * ({ responses, comments }). In read-only mode (the reviewer view and the printable
 * document) the same cards render the recorded answers, scores and comments.
 *
 * The scoring rules are not in this component: they come from utils/gadChecklist.js,
 * the source of truth for every item's scale, the criterion subtotals and the total.
 * This component only presents them and writes the chosen response / comment back
 * to the same `{ responses, comments }` object.
 */
const model = defineModel({ type: Object, default: () => emptyGadChecklist() })

const props = defineProps({
  readonly: { type: Boolean, default: false },
  /** Validation message shown under the cards (empty when every item is answered). */
  error: { type: String, default: '' },
})

// Keep the bound object structurally valid without discarding any entered value.
function ensureShape() {
  const value = model.value
  if (!value || typeof value !== 'object') return
  if (!value.responses || typeof value.responses !== 'object') value.responses = {}
  if (!value.comments || typeof value.comments !== 'object') value.comments = {}
}
watch(model, ensureShape, { immediate: true })

const summary = computed(() => gadChecklistSummary(model.value))

function setResponse(itemKey, response) {
  if (props.readonly) return
  model.value.responses[itemKey] = response
}

function setComment(itemKey, comment) {
  if (props.readonly) return
  model.value.comments[itemKey] = comment
}

/** Clears only the answer. Comments are deliberately kept. */
function clearResponse(itemKey) {
  if (props.readonly) return
  delete model.value.responses[itemKey]
}
</script>

<style scoped>
.gad {
  display: flex;
  flex-direction: column;
  gap: 9px;
  padding: 12px;
  border-radius: 14px;
  background: #f4f5fa;
}

.gad-sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

.gad-instruction {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #e8eeff;
  color: #263553;
  font-size: 10.5px;
  line-height: 1.4;
}

.gad-instruction p {
  margin: 0;
}

.gad-info-icon {
  display: inline-flex;
  flex: 0 0 auto;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  color: #3d63c9;
}

.gad-info-icon svg {
  width: 16px;
  height: 16px;
}

.gad-error {
  margin: 0;
  padding: 8px 10px;
  border: 1px solid #f3c5c5;
  border-radius: 8px;
  background: #fdf2f2;
  color: #a83232;
  font-size: 10.5px;
  font-weight: 600;
}

/* ===== OVERALL SCORE ===== */
.gad-summary {
  padding: 12px;
  border: 1px solid #e1e4ef;
  border-radius: 13px;
  background: #ffffff;
}

.gad-summary-title {
  margin: 0 0 9px;
  color: #24274a;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.02em;
  line-height: 1.35;
}

.gad-summary-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.gad-summary-item {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 7px 9px;
  border-radius: 9px;
  background: #f7f8fc;
}

.gad-summary-label {
  color: #6b7190;
  font-size: 9px;
  font-weight: 600;
  letter-spacing: 0.02em;
  text-transform: uppercase;
}

.gad-summary-value {
  color: #23294a;
  font-size: 16px;
  font-weight: 800;
  line-height: 1.2;
}

.gad-summary-incomplete {
  margin: 9px 0 0;
  color: #a66a12;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.4;
}

.gad-summary-interpretation {
  margin: 9px 0 0;
  padding: 7px 9px;
  border-radius: 8px;
  background: #ddf5e8;
  color: #248b58;
  font-size: 10.5px;
  font-weight: 600;
  line-height: 1.4;
}

@media (max-width: 640px) {
  .gad {
    padding: 9px;
  }

  .gad-summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>
