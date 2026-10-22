<template>
  <div class="signature-panel">
    <div class="panel-header">
      <h2>Electronic Signature</h2>
      <p>
        Create or upload your signature and save it to your profile for use in supported RPIS
        documents.
      </p>
    </div>

    <!-- SAVED SIGNATURE -->
    <section class="panel-block">
      <div class="block-head">
        <h3 class="block-title">Saved signature</h3>
        <span class="state-pill" :class="hasSavedSignature ? 'is-saved' : 'is-empty'">
          {{ hasSavedSignature ? 'Signature saved' : 'No signature saved yet' }}
        </span>
      </div>

      <div class="preview-frame">
        <span v-if="savedLoading" class="preview-state">Loading your saved signature…</span>
        <img v-else-if="savedUrl" class="preview-image" :src="savedUrl" alt="Saved signature" />
        <span v-else class="preview-state">
          You have not saved a signature yet. Draw or upload one below, then choose Save Signature.
        </span>
      </div>

      <p v-if="savedError" class="inline-error">{{ savedError }}</p>
      <p v-if="hasSavedSignature && !draftReady" class="block-note">
        Saving a new signature replaces this one. Documents that already contain the current
        signature are not affected.
      </p>
    </section>

    <!-- NEW SIGNATURE -->
    <section class="panel-block">
      <div class="block-head">
        <h3 class="block-title">{{ hasSavedSignature ? 'Replace signature' : 'New signature' }}</h3>
        <span v-if="draftReady" class="state-pill is-draft">Unsaved — choose Save Signature</span>
      </div>

      <div class="method-switch">
        <button
          type="button"
          class="method-btn"
          :class="{ active: method === 'draw' }"
          @click="selectMethod('draw')"
        >
          Draw Signature
        </button>
        <button
          type="button"
          class="method-btn"
          :class="{ active: method === 'upload' }"
          @click="selectMethod('upload')"
        >
          Upload Signature
        </button>
      </div>

      <!-- OPTION A: DRAW -->
      <div v-show="method === 'draw'" class="method-body">
        <div class="canvas-wrap">
          <canvas
            ref="canvasRef"
            class="signature-canvas"
            :width="CANVAS_WIDTH"
            :height="CANVAS_HEIGHT"
            @pointerdown="startStroke"
            @pointermove="moveStroke"
            @pointerup="endStroke"
            @pointerleave="endStroke"
            @pointercancel="endStroke"
          ></canvas>
          <span v-if="!draftReady" class="canvas-hint">
            Draw your signature here with a mouse, stylus or finger.
          </span>
        </div>
        <div class="method-actions">
          <button type="button" class="ghost-btn" @click="clearDrawing">Clear Drawing</button>
          <span class="method-note">Saved as a PNG with a white background.</span>
        </div>
      </div>

      <!-- OPTION B: UPLOAD -->
      <div v-show="method === 'upload'" class="method-body">
        <div class="upload-row">
          <button type="button" class="ghost-btn" @click="pickFile">
            {{ selectedFileName ? 'Replace Selected Image' : 'Choose Image' }}
          </button>
          <span class="method-note">{{ selectedFileName || 'PNG or JPEG, up to 2 MB.' }}</span>
        </div>
        <input
          ref="fileInputRef"
          class="file-input"
          type="file"
          accept="image/png,image/jpeg"
          @change="onFileSelected"
        />
      </div>

      <p v-if="draftError" class="inline-error">{{ draftError }}</p>

      <!-- UNSAVED PREVIEW -->
      <div v-if="draftReady" class="draft-preview">
        <span class="draft-label">
          Unsaved preview ({{ draftSource === 'draw' ? 'drawing' : 'uploaded image' }})
        </span>
        <div class="preview-frame preview-frame--draft">
          <img class="preview-image" :src="draftUrl" alt="Signature preview" />
        </div>
      </div>
    </section>

    <div class="panel-footer">
      <div class="footer-left">
        <button v-if="draftReady" type="button" class="ghost-btn" @click="discardDraft">
          Discard Draft
        </button>
        <button
          v-if="hasSavedSignature"
          type="button"
          class="ghost-btn ghost-btn--danger"
          @click="removeSignature"
        >
          Remove Signature
        </button>
      </div>
      <button
        type="button"
        class="save-btn"
        :disabled="!draftReady || saving"
        @click="saveSignature"
      >
        {{ saving ? 'Saving…' : 'Save Signature' }}
      </button>
    </div>

    <ConfirmDialog
      v-if="dialogState.show"
      v-bind="dialogState"
      @confirm="dialogState.onConfirm"
      @cancel="dialogState.onCancel"
      @close="dialogState.show = false"
    />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import api from '@/utils/api'
import ConfirmDialog from '@/components/ConfirmDialog.vue'
import { useDialog } from '@/composables/useDialog'

/**
 * Electronic Signature panel of Manage Profile.
 *
 * The proponent draws a signature on the canvas or picks a PNG/JPEG image, sees a
 * preview, and saves it to their own account through
 * /api/users/me/signature - the backend derives the account from the bearer token,
 * so the panel never sends (and cannot choose) a user id.
 *
 * Saving a signature here only stores it on the profile: it never signs,
 * approves or submits any document.
 */
const { dialogState, showAlert, showConfirm } = useDialog()

// Server limits, mirrored for immediate feedback. The backend validates the same
// rules again (size, real image format, dimensions) and stays authoritative.
const MAX_BYTES = 2 * 1024 * 1024
const MAX_DIMENSION = 4000
const ALLOWED_TYPES = ['image/png', 'image/jpeg']
const ALLOWED_EXTENSIONS = ['.png', '.jpg', '.jpeg']

// Fixed drawing resolution: the canvas is scaled by CSS, so pointer positions are
// mapped back onto this grid and resizing the window never distorts a drawing.
const CANVAS_WIDTH = 1000
const CANVAS_HEIGHT = 300

const method = ref('draw')
const canvasRef = ref(null)
const fileInputRef = ref(null)

const savedUrl = ref('')
const savedLoading = ref(true)
const savedError = ref('')
const hasSavedSignature = ref(false)

const draftUrl = ref('')
const draftBlob = ref(null)
const draftSource = ref('')
const draftFileName = ref('')
const draftError = ref('')
const saving = ref(false)

const drawing = ref(false)
const hasStrokes = ref(false)

const draftReady = computed(() => !!draftBlob.value)
const selectedFileName = computed(() => (draftSource.value === 'upload' ? draftFileName.value : ''))

function selectMethod(next) {
  method.value = next
  draftError.value = ''
}

// ---------------------------------------------------------------------------
// Drawing (mouse, touch and stylus through pointer events)
// ---------------------------------------------------------------------------
function context() {
  return canvasRef.value ? canvasRef.value.getContext('2d') : null
}

function paintWhite() {
  const ctx = context()
  if (!ctx) return
  ctx.fillStyle = '#ffffff'
  ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
}

function applyBrush() {
  const ctx = context()
  if (!ctx) return
  ctx.lineWidth = 2.5
  ctx.lineCap = 'round'
  ctx.lineJoin = 'round'
  ctx.strokeStyle = '#111827'
}

/** Pointer position in canvas coordinates, whatever the displayed size is. */
function canvasPoint(event) {
  const canvas = canvasRef.value
  const rect = canvas.getBoundingClientRect()
  const scaleX = rect.width ? CANVAS_WIDTH / rect.width : 1
  const scaleY = rect.height ? CANVAS_HEIGHT / rect.height : 1
  return {
    x: (event.clientX - rect.left) * scaleX,
    y: (event.clientY - rect.top) * scaleY,
  }
}

function startStroke(event) {
  const ctx = context()
  if (!ctx) return
  event.preventDefault()
  canvasRef.value.setPointerCapture?.(event.pointerId)
  drawing.value = true
  const { x, y } = canvasPoint(event)
  ctx.beginPath()
  ctx.moveTo(x, y)
  ctx.lineTo(x + 0.01, y) // a single tap still leaves a dot
  ctx.stroke()
  hasStrokes.value = true
  draftError.value = ''
}

function moveStroke(event) {
  if (!drawing.value) return
  const ctx = context()
  event.preventDefault()
  const { x, y } = canvasPoint(event)
  ctx.lineTo(x, y)
  ctx.stroke()
}

function endStroke(event) {
  if (!drawing.value) return
  drawing.value = false
  canvasRef.value?.releasePointerCapture?.(event.pointerId)
  publishDrawing()
}

/** Publishes the current drawing as the unsaved preview. */
function publishDrawing() {
  const canvas = canvasRef.value
  if (!canvas || !hasStrokes.value) return
  canvas.toBlob((blob) => {
    if (blob) setDraft(blob, 'draw', 'signature.png')
  }, 'image/png')
}

/** Clears the current unsaved drawing only; a saved signature is untouched. */
function clearDrawing() {
  paintWhite()
  applyBrush()
  hasStrokes.value = false
  if (draftSource.value === 'draw') clearDraft()
  draftError.value = ''
}

// ---------------------------------------------------------------------------
// Unsaved draft (a drawing or a picked file - never the saved signature)
// ---------------------------------------------------------------------------
function releaseDraftUrl() {
  if (draftUrl.value) {
    URL.revokeObjectURL(draftUrl.value)
    draftUrl.value = ''
  }
}

function clearDraft() {
  draftBlob.value = null
  draftSource.value = ''
  draftFileName.value = ''
  releaseDraftUrl()
}

function setDraft(blob, source, fileName) {
  clearDraft()
  draftBlob.value = blob
  draftSource.value = source
  draftFileName.value = fileName
  draftUrl.value = URL.createObjectURL(blob)
  draftError.value = ''
}

/** Throws the unsaved draft away; the saved signature stays as it is. */
function discardDraft() {
  clearDraft()
  draftError.value = ''
  if (method.value === 'draw') {
    paintWhite()
    applyBrush()
    hasStrokes.value = false
  }
}

// ---------------------------------------------------------------------------
// Uploading a signature image
// ---------------------------------------------------------------------------
function pickFile() {
  fileInputRef.value?.click()
}

function onFileSelected(event) {
  const file = event.target.files?.[0]
  // Allow choosing the same file again after a replace.
  event.target.value = ''
  if (file) validateFile(file)
}

async function validateFile(file) {
  const name = (file.name || '').toLowerCase()
  const typeOk = ALLOWED_TYPES.includes(file.type)
  const extensionOk = ALLOWED_EXTENSIONS.some((extension) => name.endsWith(extension))
  if (!typeOk && !extensionOk) {
    draftError.value = 'Only PNG and JPEG images can be used as a signature.'
    return
  }
  if (file.size > MAX_BYTES) {
    draftError.value = 'The selected image is larger than the 2 MB limit.'
    return
  }

  let size
  try {
    size = await readImageSize(file)
  } catch (error) {
    draftError.value = 'The selected file is not a readable PNG or JPEG image.'
    return
  }
  if (size.width > MAX_DIMENSION || size.height > MAX_DIMENSION) {
    draftError.value = `The image is larger than ${MAX_DIMENSION} x ${MAX_DIMENSION} pixels.`
    return
  }

  setDraft(file, 'upload', file.name || 'signature.png')
}

/** Decodes the file in the browser, so a non-image can never be previewed. */
function readImageSize(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file)
    const image = new Image()
    image.onload = () => {
      URL.revokeObjectURL(url)
      resolve({ width: image.naturalWidth, height: image.naturalHeight })
    }
    image.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('not an image'))
    }
    image.src = url
  })
}

// ---------------------------------------------------------------------------
// Saving, loading and removing the signature on the account
// ---------------------------------------------------------------------------

/**
 * Human-readable reason for a failed request.
 *
 * The backend's own message is kept when it explains the problem (validation
 * errors such as "Only PNG and JPEG signature images are supported."). Spring's
 * generic status texts are not useful on their own, so they are replaced with
 * what the status actually means here - a 404/405 means the server that answered
 * does not expose the signature API (for example a backend instance started
 * before the feature was added), which is reported as such instead of the bare
 * "Not Found".
 */
function errorMessage(error, fallback) {
  const status = error?.response?.status

  if (!error?.response) {
    return 'The server could not be reached. Please check that the backend is running and try again.'
  }
  if (status === 404 || status === 405) {
    return `The signature service is not available on the server (HTTP ${status}). Restart the backend with the latest version and try again.`
  }
  if (status === 401 || status === 403) {
    return 'Your session is no longer authorized. Please sign in again and retry.'
  }

  return serverErrorText(error.response.data) || fallback
}

/** The backend's own message, when it is more specific than a bare status text. */
function serverErrorText(data) {
  const raw =
    typeof data === 'string' ? data : data && typeof data.error === 'string' ? data.error : ''
  const message = String(raw || '').trim()
  if (!message || /^(not found|forbidden|unauthorized|bad request)$/i.test(message)) return ''
  return message
}

function releaseSavedUrl() {
  if (savedUrl.value) {
    URL.revokeObjectURL(savedUrl.value)
    savedUrl.value = ''
  }
}

/** Loads the signature saved on the signed-in account (404 = none yet). */
async function loadSavedSignature() {
  savedLoading.value = true
  savedError.value = ''
  releaseSavedUrl()
  try {
    const response = await api.get('/api/users/me/signature', { responseType: 'blob' })
    savedUrl.value = URL.createObjectURL(response.data)
    hasSavedSignature.value = true
  } catch (error) {
    if (error?.response?.status === 404) {
      hasSavedSignature.value = false
      return
    }
    savedError.value = 'Your saved signature could not be loaded. Refresh the page to try again.'
  } finally {
    savedLoading.value = false
  }
}

async function saveSignature() {
  if (!draftBlob.value || saving.value) return
  saving.value = true
  draftError.value = ''
  try {
    const form = new FormData()
    form.append('signature', draftBlob.value, draftFileName.value || 'signature.png')
    await api.post('/api/users/me/signature', form)
    // The backend confirmed the save: show what is now stored on the account.
    await loadSavedSignature()
    discardDraft()
    await showAlert('Your signature has been saved to your profile.', {
      type: 'success',
      title: 'Signature Saved',
    })
  } catch (error) {
    draftError.value = errorMessage(error, 'The signature could not be saved. Please try again.')
  } finally {
    saving.value = false
  }
}

async function removeSignature() {
  const confirmed = await showConfirm(
    'Remove the signature saved on your profile? Documents that already contain it are not affected.',
    { title: 'Remove Signature', type: 'warning', confirmText: 'Remove' },
  )
  if (!confirmed) return

  savedError.value = ''
  try {
    await api.delete('/api/users/me/signature')
    releaseSavedUrl()
    hasSavedSignature.value = false
    await showAlert('Your saved signature has been removed.', {
      type: 'success',
      title: 'Signature Removed',
    })
  } catch (error) {
    savedError.value = 'The signature could not be removed. Please try again.'
  }
}

onMounted(async () => {
  paintWhite()
  applyBrush()
  await loadSavedSignature()
})

onBeforeUnmount(() => {
  releaseSavedUrl()
  releaseDraftUrl()
})
</script>

<style scoped>
/* The panel lives inside the profile page's .form-card, so it only styles its own
   content and reuses the page's design tokens (colours, radii, typography). */
.signature-panel {
  display: flex;
  flex-direction: column;
  gap: 24px;
  text-align: left;
}

/* Heading: same typography as the other tabs' card header. */
.panel-header {
  border-bottom: 1px solid var(--color-border, #f1f5f9);
  padding-bottom: 18px;
}
.panel-header h2 {
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  margin: 0 0 6px 0;
}
.panel-header p {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}

.panel-block {
  display: flex;
  flex-direction: column;
  gap: 14px;
}
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.block-title {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--color-heading, #1e293b);
  border-left: 3px solid #2452ff;
  padding-left: 10px;
  margin: 0;
}
.state-pill {
  font-size: 0.75rem;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 999px;
}
.state-pill.is-saved {
  background: #dcfce7;
  color: #166534;
}
.state-pill.is-empty {
  background: #f1f5f9;
  color: #64748b;
}
.state-pill.is-draft {
  background: #fef3c7;
  color: #92400e;
}

/* Preview of the saved signature / of the unsaved draft */
.preview-frame {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 140px;
  padding: 16px;
  border: 1px dashed var(--color-border, #e2e8f0);
  border-radius: 12px;
  background: #f8fafc;
}
.preview-frame--draft {
  border-style: solid;
  border-color: #2452ff;
}
.preview-image {
  max-width: 100%;
  max-height: 220px;
  object-fit: contain; /* never stretched or cropped */
  background: #ffffff;
}
.preview-state {
  font-size: 0.875rem;
  color: var(--color-text-soft, #64748b);
  text-align: center;
}
.block-note {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
  margin: 0;
}
.inline-error {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #dc2626;
  margin: 0;
}

/* Draw / Upload switch */
.method-switch {
  display: flex;
  gap: 8px;
  border-bottom: 1px solid var(--color-border, #e2e8f0);
}
.method-btn {
  background: transparent;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 10px 16px;
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--color-text-soft, #64748b);
  cursor: pointer;
  border-radius: 8px 8px 0 0;
  transition: all 0.2s ease;
}
.method-btn:hover {
  color: var(--color-heading, #0f172a);
  background: var(--color-background-soft, #f8fafc);
}
.method-btn.active {
  color: #2452ff;
  border-bottom-color: #2452ff;
}
.method-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* Signature canvas: white area with a visible border, scaled to the card width
   while its drawing resolution stays fixed (no distortion on resize). */
.canvas-wrap {
  position: relative;
}
.signature-canvas {
  display: block;
  width: 100%;
  height: auto;
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  background: #ffffff;
  cursor: crosshair;
  touch-action: none; /* a stroke must not scroll the page on touch screens */
}
.canvas-hint {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.875rem;
  color: #94a3b8;
  pointer-events: none;
}

.method-actions,
.upload-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.method-note {
  font-size: 0.8125rem;
  color: var(--color-text-soft, #64748b);
}
.file-input {
  display: none;
}

.draft-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.draft-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #92400e;
}

/* Footer: same convention as the other tabs' action footer */
.panel-footer {
  border-top: 1px solid var(--color-border, #f1f5f9);
  padding-top: 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.footer-left {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.save-btn {
  background: #2452ff;
  border: 1px solid #2452ff;
  color: #ffffff !important;
  padding: 11px 22px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.save-btn:hover:not(:disabled) {
  background: #1d40cc;
  border-color: #1d40cc;
}
.save-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.ghost-btn {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #475569;
  padding: 10px 18px;
  border-radius: 8px;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}
.ghost-btn:hover {
  background: #f1f5f9;
}
.ghost-btn--danger {
  color: #dc2626;
  border-color: #fecaca;
}
.ghost-btn--danger:hover {
  background: #fef2f2;
}

@media (max-width: 768px) {
  .panel-footer {
    flex-direction: column;
    align-items: stretch;
  }
  .footer-left {
    flex-direction: column;
  }
  .save-btn,
  .ghost-btn {
    width: 100%;
  }
}
</style>
