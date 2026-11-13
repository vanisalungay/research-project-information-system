<template>
  <!-- The proposal header every review / detailed-proposal page shows: the way back, the
       title, the proposal id, the workflow status and the download action. The page keeps
       its own handlers; it only borrows the chrome. -->
  <header class="rv-header">
    <div class="rv-header-main">
      <button type="button" class="rv-back" @click="$emit('back')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" aria-hidden="true">
          <polyline points="15 18 9 12 15 6" />
        </svg>
        Back
      </button>

      <div class="rv-heading">
        <h1 class="rv-title">{{ title || 'Proposal Details' }}</h1>
        <div class="rv-heading-meta">
          <span v-if="documentId" class="rv-id">ID: {{ documentId }}</span>
          <span v-if="status" class="rv-status" :class="tone">{{ statusLabel }}</span>
        </div>
      </div>
    </div>

    <div class="rv-header-actions">
      <!-- The role's own review actions render here, unchanged. -->
      <slot name="actions" />
      <button v-if="showDownload" type="button" class="rv-download" :disabled="downloadLoading"
        @click="$emit('download')">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
          stroke-width="2" aria-hidden="true">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
          <polyline points="7 10 12 15 17 10" />
          <line x1="12" y1="15" x2="12" y2="3" />
        </svg>
        {{ downloadLoading ? 'PREPARING...' : 'DOWNLOAD' }}
      </button>
    </div>
  </header>
</template>

<script setup>
import { computed } from 'vue'

// The stylesheet the .rv-* markup needs. It is imported here, by the component that renders
// that markup, because the pages use this component directly - importing it only in the page
// wrapper left every .rv-* class undefined and the whole design layer out of the bundle.
import '@/assets/review.css'
import { reviewStatusLabel, reviewStatusTone } from '@/utils/reviewStatus'

/**
 * The shared proposal header of the review and detailed-proposal pages.
 *
 * It renders what every role's page shows and nothing else: the page passes the values it
 * already loaded and keeps its own back / download behaviour through the two events.
 */
const props = defineProps({
  /** The proposal title, shown prominently. */
  title: { type: String, default: '' },
  /** The saved proposal id / document code. */
  documentId: { type: [String, Number], default: '' },
  /** The saved workflow status. */
  status: { type: String, default: '' },
  /** False when the page has no document to download. */
  showDownload: { type: Boolean, default: true },
  /** True while the document is being prepared: the button waits instead of firing twice. */
  downloadLoading: { type: Boolean, default: false },
})

defineEmits(['back', 'download'])

const statusLabel = computed(() => reviewStatusLabel(props.status))
const tone = computed(() => reviewStatusTone(props.status))
</script>