<template>
  <div class="rv-page">
    <ProposalReviewHeader
      :title="title"
      :document-id="documentId"
      :status="status"
      :show-download="showDownload"
      @back="$emit('back')"
      @download="$emit('download')"
    >
      <!-- The role's own review actions, unchanged, in the header where they belong. -->
      <template #actions><slot name="actions" /></template>
    </ProposalReviewHeader>

    <div v-if="loading" class="rv-state">
      <span class="rv-spinner" aria-hidden="true"></span>
      <p class="rv-empty">{{ loadingText }}</p>
    </div>

    <div v-else-if="error" class="rv-state">
      <p class="rv-empty">{{ error }}</p>
      <div class="rv-state-actions">
        <button type="button" class="rv-back" @click="$emit('retry')">Retry</button>
        <button type="button" class="rv-back" @click="$emit('back')">Go Back</button>
      </div>
    </div>

    <div v-else-if="notFound" class="rv-state">
      <p class="rv-empty">{{ notFoundText }}</p>
      <button type="button" class="rv-back" @click="$emit('back')">Back to Proposals</button>
    </div>

    <template v-else>
      <div class="rv-body">
        <div class="rv-main"><slot /></div>
        <ProposalReviewSidebar
          v-if="hasAside"
          :status="status"
          :created-at="createdAt"
          :updated-at="updatedAt"
          :quick="quick"
          :review="review"
        >
          <slot name="aside" />
        </ProposalReviewSidebar>
      </div>
      <slot name="footer" />
    </template>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

import '@/assets/review.css'
import ProposalReviewHeader from '@/components/ProposalReviewHeader.vue'
import ProposalReviewSidebar from '@/components/ProposalReviewSidebar.vue'

/**
 * The shared shell of every proposal review / detailed-proposal page.
 *
 * It owns the page chrome and the four states a page can be in - loading, a failure, a
 * proposal that is not there, and the proposal itself - so every role shows the same thing
 * in the same place. The page keeps everything that is its own: the fields it renders, the
 * actions it offers, the data it loads and the handlers behind them.
 */
const props = defineProps({
  title: { type: String, default: '' },
  documentId: { type: [String, Number], default: '' },
  status: { type: String, default: '' },
  createdAt: { type: [String, Number], default: '' },
  updatedAt: { type: [String, Number], default: '' },
  /** `[{ label, value }]` summary rows for the Quick Information card. */
  quick: { type: Array, default: () => [] },
  /** `[{ label, value }]` rows for the Review Information card. */
  review: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  error: { type: String, default: '' },
  notFound: { type: Boolean, default: false },
  showDownload: { type: Boolean, default: true },
  loadingText: { type: String, default: 'Loading proposal details...' },
  notFoundText: {
    type: String,
    default: 'This proposal could not be found, or it is not available to your account.',
  },
})

defineEmits(['back', 'download', 'retry'])

const slots = useSlots()

/**
 * The summary column is rendered when it has anything to show - a status, a date, a row or a
 * card the page owns - so no role is left with an empty column.
 */
const hasAside = computed(
  () =>
    Boolean(slots.aside) ||
    Boolean(props.status) ||
    Boolean(props.createdAt) ||
    Boolean(props.updatedAt) ||
    (props.quick || []).length > 0 ||
    (props.review || []).length > 0,
)
</script>