<template>
  <!-- The summary column of the review / detailed-proposal pages. Each card is rendered only
       when it has something to show, so a role that has no review information does not get an
       empty card, and nothing is invented to fill one. -->
  <div class="rv-cards">
    <section v-if="status || createdLabel || updatedLabel" class="rv-card">
      <h2 class="rv-card-head">
        <span class="rv-card-head-icon" aria-hidden="true">&#9646;</span>
        Proposal Status
      </h2>
      <div class="rv-card-body">
        <div class="rv-status-line" :class="tone">{{ statusLabel }}</div>
        <div class="rv-meta">
          <div v-if="createdLabel" class="rv-row">
            <span class="rv-row-label">Created</span>
            <span class="rv-row-value">{{ createdLabel }}</span>
          </div>
          <div v-if="updatedLabel" class="rv-row">
            <span class="rv-row-label">Updated</span>
            <span class="rv-row-value">{{ updatedLabel }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="quickRows.length" class="rv-card">
      <h2 class="rv-card-head">
        <span class="rv-card-head-icon" aria-hidden="true">&#9432;</span>
        Quick Information
      </h2>
      <div class="rv-card-body">
        <div class="rv-rows">
          <div v-for="row in quickRows" :key="row.label" class="rv-row">
            <span class="rv-row-label">{{ row.label }}</span>
            <span class="rv-row-value">{{ row.value }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="reviewRows.length" class="rv-card">
      <h2 class="rv-card-head">
        <span class="rv-card-head-icon" aria-hidden="true">&#9673;</span>
        Review Information
      </h2>
      <div class="rv-card-body">
        <div class="rv-rows">
          <div v-for="row in reviewRows" :key="row.label" class="rv-row">
            <span class="rv-row-label">{{ row.label }}</span>
            <span class="rv-row-value">{{ row.value }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Any extra card a page owns (endorsement letter, SO number, ...) keeps its place. -->
    <slot />
  </div>
</template>

<script setup>
import { computed } from 'vue'

// The stylesheet the .rv-* markup needs - imported here, by the component that renders it.
import '@/assets/review.css'
import { reviewDateLabel, reviewStatusLabel, reviewStatusTone } from '@/utils/reviewStatus'

/**
 * The summary cards of the review and detailed-proposal pages.
 *
 * Everything shown comes from the caller: the page passes the values it already loaded, so
 * this component never fetches, never guesses and never hides information the role is
 * entitled to see. A row with no value is dropped instead of being rendered as blank.
 */
const props = defineProps({
  status: { type: String, default: '' },
  createdAt: { type: [String, Number], default: '' },
  updatedAt: { type: [String, Number], default: '' },
  /** `[{ label, value }]` - the summary rows (Project Leader, College, Duration, ...). */
  quick: { type: Array, default: () => [] },
  /** `[{ label, value }]` - the review rows (Reviewed By, Position, ...). */
  review: { type: Array, default: () => [] },
})

/** Rows with a real value; a blank or 'N/A' placeholder is kept only when it was sent. */
function usableRows(rows) {
  return (Array.isArray(rows) ? rows : []).filter(
    (row) => row && row.label && row.value !== null && row.value !== undefined && String(row.value).trim() !== '',
  )
}

const quickRows = computed(() => usableRows(props.quick))
const reviewRows = computed(() => usableRows(props.review))
const statusLabel = computed(() => reviewStatusLabel(props.status))
const tone = computed(() => reviewStatusTone(props.status))
const createdLabel = computed(() => reviewDateLabel(props.createdAt))
const updatedLabel = computed(() => reviewDateLabel(props.updatedAt))
</script>