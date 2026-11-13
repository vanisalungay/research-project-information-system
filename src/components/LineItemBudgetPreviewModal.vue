<template>
  <Teleport to="body">
    <div class="lib-preview-overlay" @click.self="emit('close')">
      <div
        class="lib-preview-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="line-item-budget-preview-title"
      >
        <!-- HEADER: title, uploaded file name, Download and the close icon -->
        <header class="lib-preview-header">
          <div class="lib-preview-heading">
            <h2 id="line-item-budget-preview-title">Line-Item Budget Preview</h2>
            <p class="lib-preview-file" :title="displayName">{{ displayName }}</p>
          </div>
          <div class="lib-preview-header-actions">
            <button
              type="button"
              class="btn-download"
              :disabled="!filePath || downloading"
              @click="download"
            >
              <svg
                width="16"
                height="16"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="7 10 12 15 17 10" />
                <line x1="12" y1="15" x2="12" y2="3" />
              </svg>
              {{ downloading ? 'Downloading…' : 'Download' }}
            </button>
            <button
              ref="closeIconRef"
              type="button"
              class="close-btn"
              aria-label="Close Line-Item Budget preview"
              @click="emit('close')"
            >
              ✕
            </button>
          </div>
        </header>

        <!-- BODY: the actual uploaded document, rendered in place -->
        <div class="lib-preview-body">
          <p v-if="downloadError" class="lib-preview-error">{{ downloadError }}</p>

          <div v-if="previewLoading" class="lib-preview-state">
            <span class="lib-preview-spinner" aria-hidden="true"></span>
            <span>Loading the Line-Item Budget…</span>
          </div>
          <div v-else-if="previewError" class="lib-preview-state lib-preview-state--error">
            <span>{{ previewError }}</span>
            <button type="button" class="btn-retry btn-secondary" @click="loadPreview">Try again</button>
          </div>
          <p v-else-if="!filePath" class="lib-preview-state">
            No Line-Item Budget document is attached to this proposal.
          </p>
          <p v-else-if="previewKind === PREVIEW_UNSUPPORTED" class="lib-preview-state">
            In-browser preview is not available for {{ typeLabel }} files. Use Download to open
            the document in its own application.
          </p>
          <iframe
            v-else-if="previewKind === PREVIEW_PDF"
            class="lib-preview-frame"
            :src="previewUrl"
            :title="displayName"
            @error="onPreviewError"
          ></iframe>
          <img
            v-else-if="previewKind === PREVIEW_IMAGE"
            class="lib-preview-image"
            :src="previewUrl"
            :alt="displayName"
            @error="onPreviewError"
          />
          <div
            v-else-if="previewKind === PREVIEW_DOCX"
            ref="docxContainer"
            class="lib-preview-docx"
          ></div>
        </div>

        <!-- FOOTER: an always visible Close action besides the header icon -->
        <footer class="lib-preview-footer">
          <button type="button" class="btn-close btn-secondary" @click="emit('close')">Close</button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import {
  PREVIEW_DOCX,
  PREVIEW_IMAGE,
  PREVIEW_PDF,
  PREVIEW_UNSUPPORTED,
  fileExtension,
  fileTypeLabel,
  previewKindForBudget,
  retypeForPreview,
} from '@/utils/documentPreview'
import { convertDocxToHtml, sanitizeDocxHtml } from '@/utils/docxPreview'
import {
  downloadUploadedFile,
  fetchUploadedFileBlob,
  releaseObjectUrl,
} from '@/utils/fileDownload'

/**
 * Read-only preview modal for the uploaded Line-Item Budget of a proposal.
 *
 * It is opened from the OVCAF screen and stays on top of the current page. The
 * bytes come from the existing authenticated `/api/files` route (the same file the
 * View/Download actions already used); the original upload is never modified,
 * replaced or copied:
 *
 * - PDF: rendered by the browser's own viewer in an <iframe>.
 * - DOCX: converted to readable HTML in memory by mammoth and shown in a
 *   sanitised fragment (never an iframe - browsers do not render DOCX).
 * - Image: shown in an <img>, contained and never stretched.
 * - Anything else: a clear message plus the Download action.
 *
 * The preview object URL is released when the modal closes or the preview is
 * replaced, and Escape / the × / clicking outside close it like the other RPIS
 * modals.
 */
const props = defineProps({
  /** Stored path served by the authorized file route (never a public URL). */
  filePath: { type: String, default: '' },
  /** Display name of the uploaded document (the name OVCAF sees on the card). */
  fileName: { type: String, default: '' },
})

const emit = defineEmits(['close'])

/** The uploaded document's name, falling back to the stored path's base name. */
const displayName = computed(() => {
  const name = String(props.fileName || '').trim()
  if (name) return name
  const path = String(props.filePath || '').replace(/\\/g, '/')
  return path.split('/').pop() || 'Line-Item Budget'
})
/**
 * Name used to decide the preview method. The displayed name is preferred; when a
 * historic record carries a name without an extension, the stored path supplies it
 * so a real DOCX is still previewed. The shown/downloaded name stays `displayName`.
 */
const previewFileName = computed(() => {
  if (fileExtension(displayName.value)) return displayName.value
  const path = String(props.filePath || '').replace(/\\/g, '/')
  const base = path.split('/').pop() || ''
  return fileExtension(base) ? base : displayName.value
})
const typeLabel = computed(() => fileTypeLabel(previewFileName.value))

// ---------------------------------------------------------------------------
// Preview
// ---------------------------------------------------------------------------
const previewKind = ref('')
const previewUrl = ref('')
const previewLoading = ref(false)
const previewError = ref('')
const docxContainer = ref(null)

/** Releases the current preview resources and clears its state. */
function clearPreview() {
  releaseObjectUrl(previewUrl.value)
  previewUrl.value = ''
  if (docxContainer.value) docxContainer.value.replaceChildren()
}

/**
 * Loads the attached file. A PDF/image is displayed from an object URL; a DOCX is
 * converted in memory. Formats the browser cannot display skip the fetch and show
 * the explanatory message instead.
 */
async function loadPreview() {
  clearPreview()
  previewError.value = ''
  previewKind.value = previewKindForBudget(previewFileName.value)

  if (!props.filePath) {
    previewError.value = ''
    return
  }
  if (previewKind.value === PREVIEW_UNSUPPORTED) return

  previewLoading.value = true
  let docxHtml = ''
  try {
    const blob = await fetchUploadedFileBlob(props.filePath)
    if (!blob) throw new Error('The Line-Item Budget file is empty.')

    if (previewKind.value === PREVIEW_DOCX) {
      docxHtml = await convertDocxToHtml(await blob.arrayBuffer())
      if (!docxHtml.trim()) throw new Error('The Line-Item Budget file has no readable content.')
    } else {
      previewUrl.value = window.URL.createObjectURL(
        retypeForPreview(blob, previewFileName.value),
      )
    }
  } catch (error) {
    console.error('Error loading the Line-Item Budget preview:', error)
    previewError.value = 'The Line-Item Budget document could not be loaded for preview.'
  } finally {
    previewLoading.value = false
  }

  if (previewKind.value === PREVIEW_DOCX && !previewError.value && docxHtml) {
    // The container only exists once the loading branch is gone.
    await nextTick()
    if (!docxContainer.value) {
      previewError.value = 'The Line-Item Budget document could not be displayed.'
      return
    }
    docxContainer.value.replaceChildren(sanitizeDocxHtml(docxHtml))
  }
}

/** Called when the browser cannot render the previewed file. */
function onPreviewError() {
  clearPreview()
  previewError.value = 'The Line-Item Budget document could not be displayed.'
}

// ---------------------------------------------------------------------------
// Download: the same uploaded file, saved under its display name.
// ---------------------------------------------------------------------------
const downloading = ref(false)
const downloadError = ref('')

async function download() {
  if (!props.filePath || downloading.value) return
  downloading.value = true
  downloadError.value = ''
  try {
    await downloadUploadedFile(props.filePath, displayName.value)
  } catch (error) {
    console.error('Error downloading the Line-Item Budget:', error)
    downloadError.value = 'Failed to download the Line-Item Budget. Please try again.'
  } finally {
    downloading.value = false
  }
}

// ---------------------------------------------------------------------------
// Modal interactions
// ---------------------------------------------------------------------------
const closeIconRef = ref(null)

// Escape closes the modal, like the other modals in the application.
function onKeydown(event) {
  if (event.key === 'Escape') emit('close')
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  loadPreview()
  // Move the focus into the dialog so Escape reaches it immediately.
  await nextTick()
  closeIconRef.value?.focus?.()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  // Never leave a preview object URL or rendered document behind.
  clearPreview()
})
</script>

<style scoped>
/* ============================================================
   Line-Item Budget preview modal
   The chrome mirrors the other document modals of the application (fixed
   overlay, white card, header with the file name, Download and close). It is
   teleported to <body> so it always sits on top of the OVCAF page, which keeps
   its state underneath.
   ============================================================ */
.lib-preview-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.65);
  backdrop-filter: blur(4px);
}

.lib-preview-modal {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 1000px;
  height: 90vh;
  height: 90dvh;
  max-height: calc(100vh - 40px);
  max-height: calc(100dvh - 40px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ===== HEADER ===== */
.lib-preview-header {
  flex-shrink: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.lib-preview-heading {
  min-width: 0;
}

.lib-preview-heading h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lib-preview-file {
  margin: 3px 0 0;
  font-size: 12px;
  color: #64748b;
  overflow-wrap: anywhere;
}

.lib-preview-header-actions {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

/* Layout only; the action colours are global (assets/actions.css). */
.btn-download {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-download:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.close-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  border-radius: 8px;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  transition: all 0.15s;
}

.close-btn:hover {
  background: #f1f5f9;
  color: #475569;
}

.close-btn:focus-visible,
.btn-download:focus-visible,
.btn-close:focus-visible,
.btn-retry:focus-visible {
  outline: 2px solid var(--color-primary, #8b0000);
  outline-offset: 2px;
}

/* ===== DOCUMENT AREA =====
   The document fills the modal body: a PDF scrolls inside its iframe, a DOCX
   scrolls in its own page-styled area, and every state message is centred. */
.lib-preview-body {
  flex: 1 1 auto;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #f8fafc;
}

.lib-preview-state {
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
  line-height: 1.55;
}

.lib-preview-state--error {
  color: #dc2626;
}

.lib-preview-spinner {
  width: 22px;
  height: 22px;
  border: 3px solid #e2e8f0;
  border-top-color: var(--color-primary, #8b0000);
  border-radius: 50%;
  animation: libPreviewSpin 0.8s linear infinite;
}

@keyframes libPreviewSpin {
  to {
    transform: rotate(360deg);
  }
}

.lib-preview-frame {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  border: 0;
  background: #fff;
}

.lib-preview-image {
  flex: 1 1 auto;
  width: 100%;
  min-height: 0;
  padding: 16px;
  box-sizing: border-box;
  object-fit: contain;
}

/* The DOCX page: a readable white sheet, wide enough for budget tables. */
.lib-preview-docx {
  flex: 1 1 auto;
  min-height: 0;
  overflow: auto;
  margin: 0 auto;
  width: 100%;
  max-width: 900px;
  padding: 32px 40px 48px;
  box-sizing: border-box;
  background: #fff;
  color: #1e293b;
  font-size: 14px;
  line-height: 1.65;
}

.lib-preview-docx :deep(p) {
  margin: 0 0 10px;
}

.lib-preview-docx :deep(h1),
.lib-preview-docx :deep(h2),
.lib-preview-docx :deep(h3),
.lib-preview-docx :deep(h4) {
  margin: 18px 0 8px;
  color: #0f172a;
  line-height: 1.3;
}

.lib-preview-docx :deep(ul),
.lib-preview-docx :deep(ol) {
  margin: 0 0 12px;
  padding-left: 26px;
}

.lib-preview-docx :deep(li) {
  margin-bottom: 4px;
}

.lib-preview-docx :deep(table) {
  width: 100%;
  margin: 12px 0 18px;
  border-collapse: collapse;
  font-size: 13px;
}

.lib-preview-docx :deep(th),
.lib-preview-docx :deep(td) {
  padding: 7px 10px;
  border: 1px solid #cbd5e1;
  text-align: left;
  vertical-align: top;
}

.lib-preview-docx :deep(th) {
  background: #f1f5f9;
  font-weight: 700;
}

.lib-preview-docx :deep(a) {
  color: var(--color-primary, #8b0000);
  text-decoration: underline;
}

.lib-preview-docx :deep(img) {
  max-width: 100%;
  height: auto;
}

/* A failed Download keeps the preview in place and reports itself above it. */
.lib-preview-error {
  flex-shrink: 0;
  margin: 0;
  padding: 10px 20px;
  border-bottom: 1px solid #fecaca;
  background: #fef2f2;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

/* ===== FOOTER ===== */
.lib-preview-footer {
  flex-shrink: 0;
  display: flex;
  justify-content: flex-end;
  padding: 14px 24px;
  background: #fff;
  border-top: 1px solid #e2e8f0;
}

.btn-close {
  padding: 10px 20px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-retry {
  padding: 8px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
}

/* Small screens: the header actions wrap below the title, and the DOCX page uses
   the full width without horizontal overflow. */
@media (max-width: 640px) {
  .lib-preview-overlay {
    padding: 8px;
  }
  .lib-preview-modal {
    max-height: calc(100vh - 16px);
    max-height: calc(100dvh - 16px);
  }
  .lib-preview-header {
    flex-wrap: wrap;
    padding: 14px 16px;
  }
  .lib-preview-heading {
    min-width: 160px;
  }
  .lib-preview-docx {
    padding: 20px 16px 32px;
  }
  .lib-preview-footer {
    padding: 12px 16px;
  }
  .btn-close {
    width: 100%;
  }
}
</style>
