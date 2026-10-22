<template>
  <div class="modal-overlay modal-overlay--terminal" @click.self="emit('close')">
    <div
      class="modal modal--terminal"
      role="dialog"
      aria-modal="true"
      aria-labelledby="terminal-report-modal-title"
    >
      <div class="modal-header">
        <div class="modal-header-text">
          <h2 id="terminal-report-modal-title" :title="fileName || 'Terminal Report'">
            {{ fileName || 'Terminal Report' }}
          </h2>
          <p v-if="fileName" class="modal-header-sub">
            {{ typeLabel }}
            <template v-if="periodLabel"> · {{ periodLabel }}</template>
          </p>
        </div>
        <div class="modal-header-actions">
          <button class="btn-primary" :disabled="!fileName || downloading" @click="download">
            {{ downloading ? 'Downloading…' : 'Download' }}
          </button>
          <button class="close-btn" @click="emit('close')" aria-label="Close">✕</button>
        </div>
      </div>
      <div class="modal-body">
        <p v-if="downloadError" class="terminal-preview-error">{{ downloadError }}</p>

        <div class="terminal-preview">
          <p v-if="!fileName" class="terminal-preview-state">
            <span class="terminal-preview-text">
              No Terminal Report file is attached to this report.
            </span>
          </p>
          <div v-else-if="previewLoading" class="terminal-preview-state">
            <span class="terminal-preview-spinner" aria-hidden="true"></span>
            <span class="terminal-preview-text">Loading the Terminal Report…</span>
          </div>
          <div
            v-else-if="previewError"
            class="terminal-preview-state terminal-preview-state--error"
          >
            <span class="terminal-preview-text">{{ previewError }}</span>
            <button class="btn-secondary" @click="loadPreview">Try again</button>
          </div>
          <p v-else-if="previewKind === 'unsupported'" class="terminal-preview-state">
            <span class="terminal-preview-text">
              In-browser preview is not available for {{ typeLabel }} files. Use Download to open
              the file in its own application.
            </span>
          </p>
          <iframe
            v-else-if="previewKind === 'pdf'"
            class="terminal-preview-frame"
            :src="previewUrl"
            :title="fileName"
            @error="onPreviewError"
          ></iframe>
          <img
            v-else-if="previewKind === 'image'"
            class="terminal-preview-image"
            :src="previewUrl"
            :alt="fileName"
            @error="onPreviewError"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  downloadReportDocument,
  fetchReportDocumentPreviewUrl,
  releaseObjectUrl,
} from '@/utils/fileDownload'
import { fileTypeLabel, previewKindOf } from '@/utils/documentPreview'

/**
 * Read-only preview modal for a submitted Terminal Report.
 *
 * It shows the actual uploaded file: a PDF through the browser's own viewer in
 * an iframe, an image in an <img> (never stretched or cropped), and a clear
 * message for formats the browser cannot display in-app (DOC/DOCX) - the modal
 * is never blank. Download fetches the same file through the existing authorized
 * endpoint and keeps the original file name. Nothing here edits, replaces or
 * deletes the report.
 */
const props = defineProps({
  /** Report row id used by the existing document endpoint. */
  reportId: { type: [Number, String], required: true },
  /** Stored file name (the original name of the upload). */
  fileName: { type: String, default: '' },
  /** Reporting period shown under the file name, when the host knows it. */
  periodLabel: { type: String, default: '' },
})

const emit = defineEmits(['close'])

const typeLabel = computed(() => fileTypeLabel(props.fileName))

// ---------------------------------------------------------------------------
// Document preview
// The bytes come from the existing authorized endpoint and are displayed from an
// object URL: that route needs the bearer token, so a plain <iframe src="/api/…">
// could not authenticate. The object URL is released when the modal closes.
// ---------------------------------------------------------------------------
const previewUrl = ref('')
const previewKind = ref('')
const previewLoading = ref(false)
const previewError = ref('')

/** Releases the current preview URL and clears its state. */
function clearPreview() {
  releaseObjectUrl(previewUrl.value)
  previewUrl.value = ''
  previewKind.value = ''
  previewLoading.value = false
  previewError.value = ''
}

/**
 * Loads the attached file for preview. Formats the browser cannot display in-app
 * skip the fetch and fall back to an explanatory message.
 */
async function loadPreview() {
  clearPreview()
  if (!props.fileName) return

  previewKind.value = previewKindOf(props.fileName)
  if (previewKind.value === 'unsupported') return

  previewLoading.value = true
  try {
    previewUrl.value = await fetchReportDocumentPreviewUrl(
      props.reportId,
      'terminal',
      props.fileName,
    )
  } catch (e) {
    previewError.value = 'The Terminal Report file could not be loaded for preview.'
  } finally {
    previewLoading.value = false
  }
}

/** Called when the browser cannot render the previewed file. */
function onPreviewError() {
  clearPreview()
  previewError.value = 'The Terminal Report file could not be displayed.'
}

// ---------------------------------------------------------------------------
// Download: the same file, through the same endpoint, with the original name.
// The preview stays open and the report record is never touched.
// ---------------------------------------------------------------------------
const downloading = ref(false)
const downloadError = ref('')

async function download() {
  if (!props.fileName || downloading.value) return
  downloading.value = true
  downloadError.value = ''
  try {
    await downloadReportDocument(props.reportId, 'terminal', props.fileName)
  } catch (e) {
    downloadError.value = 'Failed to download the Terminal Report. Please try again.'
  } finally {
    downloading.value = false
  }
}

// Escape closes the modal, like the other modals in the application.
function onKeydown(e) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  window.addEventListener('keydown', onKeydown)
  loadPreview()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  // Never leave a preview object URL behind.
  clearPreview()
})
</script>

<style scoped>
/* ============================================================
   Terminal Report preview modal
   The modal chrome mirrors the report view modal so both document modals look
   the same: fixed overlay, white card, and a header holding the file name, the
   Download action and the close button.
   ============================================================ */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: flex-start;
  z-index: 200;
  padding: 24px;
  overflow-y: auto;
}
.modal {
  background: #fff;
  width: 100%;
  max-width: 1000px;
  border-radius: 12px;
  max-height: 90vh;
  overflow-y: auto;
}
.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #e2e8f0;
  position: sticky;
  top: 0;
  background: #fff;
}
.modal-header h2 {
  margin: 0;
  font-size: 18px;
}
.close-btn {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: none;
  background: #f1f5f9;
  cursor: pointer;
}
.modal-body {
  padding: 16px 20px 24px;
}
.btn-primary {
  background: #ffd400;
  color: #1c2145;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
.btn-primary:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}
.btn-secondary {
  background: #e2e8f0;
  color: #1c2145;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 700;
  cursor: pointer;
}
/* ---------- Layout ----------
   Column layout: the header keeps its place and only the document area scrolls.
   The modal uses most of the viewport height (capped so it can never leave the
   screen) so as much of the document as possible is visible at once. */
.modal-overlay--terminal .modal {
  /* Centre the modal vertically. Auto margins (instead of align-items: center)
     keep the top of the modal reachable if the overlay has to scroll on a very
     short viewport. */
  margin: auto 0;
}
.modal.modal--terminal {
  display: flex;
  flex-direction: column;
  overflow: hidden; /* the modal itself never scrolls */
  height: 90vh;
  height: 90dvh; /* dynamic viewport unit where supported (mobile toolbars) */
  /* 48px = the overlay's 24px padding above and below, so the modal can never
     extend beyond the screen. */
  max-height: calc(100vh - 48px);
  max-height: calc(100dvh - 48px);
}
.modal--terminal .modal-header {
  flex-shrink: 0;
  gap: 12px;
}
.modal--terminal .modal-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 0; /* the preview area owns its own padding */
}
.modal--terminal .modal-header-text {
  min-width: 0;
}
.modal--terminal .modal-header h2 {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.modal-header-sub {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
}
.modal-header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}
/* ---------- Document preview ----------
   The document fills the modal body: a PDF is rendered by the browser's own
   viewer inside the iframe (which scrolls internally, so a multi-page report
   scrolls without moving the modal header), and an image is contained so it is
   never stretched, cropped or distorted. */
.terminal-preview {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #f8fafc;
}
.terminal-preview-state {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin: 0;
  padding: 32px 24px;
  text-align: center;
  color: #64748b;
  font-size: 14px;
  line-height: 1.5;
}
.terminal-preview-state--error {
  color: #dc2626;
}
.terminal-preview-text {
  max-width: 560px;
}
.terminal-preview-spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #e2e8f0;
  border-top-color: #1c2145;
  border-radius: 50%;
  animation: terminalPreviewSpin 0.8s linear infinite;
}
@keyframes terminalPreviewSpin {
  to {
    transform: rotate(360deg);
  }
}
.terminal-preview-frame {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  border: 0;
  background: #fff;
}
.terminal-preview-image {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  padding: 16px;
  box-sizing: border-box;
  object-fit: contain;
}
/* A failed Download keeps the preview in place and reports itself above it. */
.terminal-preview-error {
  flex-shrink: 0;
  margin: 0;
  padding: 10px 20px;
  border-bottom: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

/* Small screens: the header actions wrap below the file name instead of
   squeezing it away, and the document keeps the modal width (no horizontal
   overflow, no distortion). */
@media (max-width: 640px) {
  .modal--terminal .modal-header {
    flex-wrap: wrap;
  }
  .modal--terminal .modal-header-text {
    min-width: 180px;
  }
  .terminal-preview-state {
    padding: 24px 16px;
  }
}
</style>
