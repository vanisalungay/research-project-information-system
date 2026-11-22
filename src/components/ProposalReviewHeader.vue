<template>
  <!-- The proposal header every review / detailed-proposal page shows: the way back, the
       title, the proposal id, the workflow status and the download action. The page keeps
       its own handlers; it only borrows the chrome. -->
  <header class="rv-header">
    <!-- 1. Navigation: always its own area, so the standard Back control cannot
         compete with the title for space. -->
    <nav class="rv-header-nav" aria-label="Proposal navigation">
      <BackButton :navigate="false" @click="$emit('back')" />
    </nav>

    <!-- 2. Identification: the title is the page's primary heading, with the saved
         reference and workflow status as secondary metadata beneath it. -->
    <div class="rv-identification">
      <h1 class="rv-title">{{ title || 'Proposal Details' }}</h1>
      <div v-if="documentId || status" class="rv-heading-meta">
        <span v-if="documentId" class="rv-id">
          <span class="rv-id-label">Proposal ID</span>
          <span class="rv-id-value">{{ documentId }}</span>
        </span>
        <span v-if="status" class="rv-status" :class="tone">{{ statusLabel }}</span>
      </div>
    </div>

    <!-- 3. Actions: the page's own controls in one aligned group, then Download. -->
    <div class="rv-header-actions">
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
import BackButton from '@/components/BackButton.vue'

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