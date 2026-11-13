<template>
  <Teleport to="body">
    <div class="ref-overlay" @click.self="close">
      <div
        class="ref-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="reference-modal-title"
      >
        <!-- HEADER -->
        <header class="ref-header">
          <div class="ref-header-text">
            <h2 id="reference-modal-title" class="ref-title">{{ modalTitle }}</h2>
            <p class="ref-subtitle">{{ modalSubtitle }}</p>
          </div>
          <button
            ref="closeButton"
            type="button"
            class="ref-close"
            aria-label="Close"
            @click="close"
          >
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
              stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        </header>

        <!-- MODE SWITCH: one connected track with a sliding indicator. Native radio inputs
             keep the control keyboard accessible (Tab reaches it, arrow keys move between
             the two options, Space selects) while the options render as a segmented bar. -->
        <div class="ref-switch-row">
          <div class="ref-switch" role="radiogroup" aria-label="How to add the reference">
            <span
              class="ref-switch-indicator"
              :class="{ 'ref-switch-indicator--manual': mode === 'manual' }"
              aria-hidden="true"
            ></span>

            <label
              class="ref-switch-option"
              :class="{ 'ref-switch-option--active': mode === 'generate' }"
            >
              <input
                class="ref-switch-input"
                type="radio"
                name="reference-mode"
                value="generate"
                :checked="mode === 'generate'"
                @change="setMode('generate')"
              />
              <svg class="ref-switch-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M9.9 15.5a2 2 0 0 0-1.4-1.4l-6.1-1.6a.5.5 0 0 1 0-1l6.1-1.5A2 2 0 0 0 9.9 8.5l1.6-6.1a.5.5 0 0 1 1 0l1.5 6.1a2 2 0 0 0 1.4 1.5l6.1 1.5a.5.5 0 0 1 0 1l-6.1 1.5a2 2 0 0 0-1.4 1.5l-1.6 6.1a.5.5 0 0 1-1 0z" />
                <path d="M20 3v4M22 5h-4M4 17v2M5 18H3" />
              </svg>
              <span class="ref-switch-label">Generate APA Reference</span>
            </label>

            <label
              class="ref-switch-option"
              :class="{ 'ref-switch-option--active': mode === 'manual' }"
            >
              <input
                class="ref-switch-input"
                type="radio"
                name="reference-mode"
                value="manual"
                :checked="mode === 'manual'"
                @change="setMode('manual')"
              />
              <svg class="ref-switch-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                <path d="M12 20h9" />
                <path d="M16.5 3.5a2.12 2.12 0 0 1 3 3L7 19l-4 1 1-4z" />
              </svg>
              <span class="ref-switch-label">Enter APA Reference Manually</span>
            </label>
          </div>
        </div>

        <!-- BODY -->
        <div class="ref-body">
          <Transition name="ref-fade" mode="out-in">
            <!-- OPTION A: the structured builder -->
            <section
              v-if="mode === 'generate'"
              key="generate"
              class="ref-panel"
              role="group"
              aria-label="Generate APA Reference"
            >
              <div class="ref-grid">
                <div class="ref-field ref-field--full">
                  <label class="ref-label" :for="`ref-type-${uid}`">Reference Type</label>
                  <select
                    :id="`ref-type-${uid}`"
                    :value="typeKey"
                    @change="onTypeChange($event.target.value)"
                  >
                    <option v-for="type in REFERENCE_TYPES" :key="type.key" :value="type.key">
                      {{ type.label }}
                    </option>
                  </select>
                </div>

                <div
                  v-for="field in fields"
                  :key="field.key"
                  class="ref-field"
                  :class="{ 'ref-field--full': field.full || field.kind === 'textarea' }"
                >
                  <label class="ref-label" :for="`ref-${field.key}-${uid}`">
                    {{ field.label }}<span v-if="field.required" class="ref-required" aria-hidden="true">*</span>
                  </label>

                  <textarea
                    v-if="field.kind === 'textarea'"
                    :id="`ref-${field.key}-${uid}`"
                    v-model="values[field.key]"
                    rows="3"
                    :placeholder="field.placeholder || ''"
                    :class="{ 'ref-invalid': fieldErrors[field.key] }"
                  ></textarea>

                  <select
                    v-else-if="field.kind === 'select'"
                    :id="`ref-${field.key}-${uid}`"
                    v-model="values[field.key]"
                    :class="{ 'ref-invalid': fieldErrors[field.key] }"
                  >
                    <option value="">
                      {{ field.key === 'month' ? '— (not provided)' : 'Select…' }}
                    </option>
                    <option v-for="option in field.options" :key="option" :value="option">
                      {{ option }}
                    </option>
                  </select>

                  <input
                    v-else
                    :id="`ref-${field.key}-${uid}`"
                    v-model="values[field.key]"
                    type="text"
                    :inputmode="field.kind === 'year' || field.kind === 'day' ? 'numeric' : 'text'"
                    :maxlength="field.kind === 'year' ? 4 : field.kind === 'day' ? 2 : null"
                    :placeholder="field.placeholder || ''"
                    :class="{ 'ref-invalid': fieldErrors[field.key] }"
                  />

                  <span v-if="field.hint" class="ref-hint">{{ field.hint }}</span>
                  <span v-if="fieldErrors[field.key]" class="ref-error">{{ fieldErrors[field.key] }}</span>
                </div>
              </div>

              <p v-if="generateError" class="ref-error ref-error--block">{{ generateError }}</p>
            </section>

            <!-- OPTION B: a reference the proponent already prepared -->
            <section
              v-else
              key="manual"
              class="ref-panel"
              role="group"
              aria-label="Enter APA Reference Manually"
            >
              <div class="ref-grid">
                <div class="ref-field ref-field--full">
                  <label class="ref-label" :for="`ref-manual-${uid}`">APA Reference</label>
                  <textarea
                    :id="`ref-manual-${uid}`"
                    v-model="manualText"
                    rows="6"
                    placeholder="Paste your complete APA 7th edition reference here..."
                    :class="{ 'ref-invalid': manualError }"
                  ></textarea>
                  <span class="ref-hint">
                    The text is saved exactly as you write it - nothing is rewritten or reformatted.
                  </span>
                  <span v-if="manualError" class="ref-error">{{ manualError }}</span>
                </div>
              </div>
            </section>
          </Transition>
        </div>

        <!-- LIVE PREVIEW: rebuilt from the current values, never from an example. The
             citation is escaped by the APA generator, so only its own italics markup
             reaches the page. -->
        <section class="ref-preview">
          <span class="ref-preview-label">Live APA 7th preview</span>
          <p v-if="preview.text" class="ref-preview-text" v-html="preview.html"></p>
          <p v-else class="ref-preview-empty">{{ PREVIEW_PLACEHOLDER }}</p>
        </section>

        <!-- FOOTER -->
        <footer class="ref-footer">
          <button type="button" class="ref-btn ref-btn--secondary" @click="close">Cancel</button>
          <button
            type="button"
            class="ref-btn ref-btn--primary"
            :disabled="submitting"
            @click="addReference"
          >
            {{ isEditing ? 'Save Changes' : 'Add to Literature Cited' }}
          </button>
        </footer>
      </div>
    </div>
  </Teleport>
</template>


<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { getScrollContainer } from '@/utils/scrollReset'
import {
  MANUAL_REFERENCE_TYPE,
  REFERENCE_TYPES,
  emptyReferenceFields,
  entryHtml,
  formatReference,
  makeGeneratedEntry,
  makeManualEntry,
  referenceFields,
  validateReference,
} from '@/utils/apaReferences'

/**
 * "Add Literature" modal for section 23 (Literature Cited).
 *
 * Two independent ways of adding a reference:
 *   Generate APA Reference        the structured builder - a reference type and only the
 *                                 fields that type needs, validated before saving.
 *   Enter APA Reference Manually  a free text area for a reference the proponent already
 *                                 formatted elsewhere; it is stored untouched.
 *
 * The APA 7th edition citation is previewed live while the details are typed: it is built
 * from the current values by the existing generator (utils/apaReferences.js), which escapes
 * every value it prints, so the preview can never inject markup.
 *
 * Switching between the two ways never loses anything: the entered values, the preview and
 * the pasted text all stay in this component's state. When an existing entry is passed in,
 * the same modal edits it (the values are restored, and the primary action becomes
 * "Save Changes").
 */
const props = defineProps({
  /** The entry being edited, or null when a new reference is added. */
  entry: { type: Object, default: null },
})

const emit = defineEmits(['save', 'close'])

const uid = Math.random().toString(36).slice(2, 8)

const isEditing = computed(() => Boolean(props.entry))
const modalTitle = computed(() => (isEditing.value ? 'Edit Literature' : 'Add Literature'))
const modalSubtitle = 'Add a reference to your proposal using an APA 7th edition citation.'

/** Shown in the preview panel while the citation cannot be produced yet. */
const PREVIEW_PLACEHOLDER = 'Your APA reference will appear here as you enter the details.'

/** The starting mode: a manual entry is edited as text, anything else as a builder. */
const initialMode = computed(() =>
  props.entry?.type === MANUAL_REFERENCE_TYPE ? 'manual' : 'generate',
)

const mode = ref(initialMode.value)
const typeKey = ref(
  props.entry?.type && props.entry.type !== MANUAL_REFERENCE_TYPE ? props.entry.type : 'journal',
)
const values = reactive(
  props.entry?.type && props.entry.type !== MANUAL_REFERENCE_TYPE
    ? { ...emptyReferenceFields(props.entry.type), ...(props.entry.fields || {}) }
    : emptyReferenceFields('journal'),
)
const manualText = ref(props.entry?.type === MANUAL_REFERENCE_TYPE ? props.entry.text || '' : '')

const fieldErrors = reactive({})
const generateError = ref('')
const manualError = ref('')
// Blocks a second click while the reference is being handed to the page.
const submitting = ref(false)
const closeButton = ref(null)

/** The fields of the selected type - only the ones that type actually needs. */
const fields = computed(() => referenceFields(typeKey.value))

/**
 * The live preview of the citation, rebuilt from the current values:
 *   - manual mode   the proponent's own text, unchanged (only escaped for display)
 *   - builder mode  the APA 7th edition reference of the selected type, or empty while a
 *                   required field is still missing (the generator never invents a part)
 */
const preview = computed(() => {
  if (mode.value === 'manual') {
    const text = String(manualText.value ?? '').trim()
    if (!text) return { text: '', html: '' }
    return { text, html: entryHtml({ type: MANUAL_REFERENCE_TYPE, text }) }
  }
  const { text, html } = formatReference(typeKey.value, values)
  return { text, html }
})

function setMode(next) {
  mode.value = next
}

/** A different type starts from its own blank field set (nothing is invented). */
function onTypeChange(key) {
  typeKey.value = key
  for (const field of fields.value) values[field.key] = ''
  Object.assign(values, emptyReferenceFields(key))
  clearErrors()
}

function clearErrors() {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  generateError.value = ''
  manualError.value = ''
}

function close() {
  emit('close')
}

/**
 * Hands the finished reference to the page (Add to Literature Cited / Save Changes) after
 * the existing validation. The page closes the modal once the entry is accepted, so a
 * rejected duplicate keeps the modal open with the entered values intact.
 */
async function addReference() {
  clearErrors()
  if (submitting.value) return

  if (mode.value === 'manual') {
    const text = String(manualText.value ?? '').trim()
    if (!text) {
      manualError.value = 'Enter or paste a complete APA reference first.'
      return
    }
    submitting.value = true
    const created = makeManualEntry(text)
    emit('save', props.entry ? { ...created, id: props.entry.id } : created)
    await nextTick()
    submitting.value = false
    return
  }

  const result = validateReference(typeKey.value, values)
  if (!result.valid) {
    Object.assign(fieldErrors, result.errors)
    generateError.value = 'Please complete the highlighted fields to add the reference.'
    return
  }
  const created = makeGeneratedEntry(typeKey.value, values)
  if (!created.text) {
    generateError.value = 'Please complete the highlighted fields to add the reference.'
    return
  }
  submitting.value = true
  emit('save', props.entry ? { ...created, id: props.entry.id } : created)
  await nextTick()
  submitting.value = false
}

// Escape closes the modal, like the other dialogs of the application.
function onKeydown(event) {
  if (event.key === 'Escape') close()
}

/* ===== Background scroll lock =====
   The modal is open on top of the proposal form, so the page behind it must not scroll.
   The authenticated layout scrolls its own content region (see utils/scrollReset.js), so
   both that region and the document body are frozen while the modal is mounted and restored
   exactly as they were when it closes. */
let lockedBodyOverflow = ''
let lockedContainerOverflow = ''

function lockBackgroundScroll() {
  if (typeof document === 'undefined') return
  const body = document.body
  if (body) {
    lockedBodyOverflow = body.style.overflow
    body.style.overflow = 'hidden'
  }
  const container = getScrollContainer()
  if (container && container !== body) {
    lockedContainerOverflow = container.style.overflow
    container.style.overflow = 'hidden'
  }
}

function unlockBackgroundScroll() {
  if (typeof document === 'undefined') return
  const body = document.body
  if (body) body.style.overflow = lockedBodyOverflow
  const container = getScrollContainer()
  if (container && container !== body) container.style.overflow = lockedContainerOverflow
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  lockBackgroundScroll()
  await nextTick()
  closeButton.value?.focus?.()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
  unlockBackgroundScroll()
})
</script>


<style scoped>
/* ============================================================
   Add Literature modal (section 23 - Literature Cited)

   A single dialog: fixed backdrop, centred card, a fixed header, the mode switch, a body
   that scrolls on its own, the live citation preview and a fixed footer, so the actions
   stay reachable while a long form scrolls. Everything is scoped to this modal.
   ============================================================ */

/* ---------- Backdrop and card ---------- */
.ref-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.6);
  -webkit-backdrop-filter: blur(8px);
  backdrop-filter: blur(8px);
  overscroll-behavior: contain;
}

.ref-modal {
  display: flex;
  flex-direction: column;
  width: 92%;
  max-width: 640px;
  max-height: calc(100vh - 48px);
  max-height: calc(100dvh - 48px);
  border-radius: 16px;
  background: #ffffff;
  color: #0f172a;
  box-shadow:
    0 20px 25px -5px rgba(0, 0, 0, 0.1),
    0 8px 10px -6px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica,
    Arial, sans-serif;
}

/* ---------- Header ---------- */
.ref-header {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 24px 20px;
}

.ref-header-text {
  min-width: 0;
}

.ref-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
  color: #0f172a;
}

.ref-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: #64748b;
}

.ref-close {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #64748b;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    color 0.15s ease;
}

.ref-close svg {
  width: 18px;
  height: 18px;
}

.ref-close:hover {
  background: #f1f5f9;
  color: #0f172a;
}

.ref-close:focus-visible {
  outline: 2px solid #4f46e5;
  outline-offset: 2px;
}


/* ---------- Mode switch ----------
   One connected track with a sliding white indicator. The indicator is a separate layer
   behind the options, so it can animate between them; the options themselves only change
   their label colour. The radio inputs stay in the DOM (visually hidden) to keep the native
   keyboard behaviour. */
.ref-switch-row {
  flex-shrink: 0;
  padding: 0 24px;
}

.ref-switch {
  position: relative;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  width: 100%;
  height: 48px;
  padding: 4px;
  box-sizing: border-box;
  border-radius: 32px;
  background: #f1f5f9;
}

.ref-switch-indicator {
  position: absolute;
  top: 4px;
  bottom: 4px;
  left: 4px;
  width: calc(50% - 4px);
  border-radius: 32px;
  background: #ffffff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  transition: transform 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.ref-switch-indicator--manual {
  transform: translateX(100%);
}

.ref-switch-option {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  min-width: 0;
  padding: 0 10px;
  border-radius: 32px;
  color: #64748b;
  font-size: 13.5px;
  font-weight: 600;
  line-height: 1.2;
  text-align: center;
  cursor: pointer;
  transition: color 0.2s ease;
}

.ref-switch-option:hover {
  color: #334155;
}

.ref-switch-option--active,
.ref-switch-option--active:hover {
  color: #0f172a;
  font-weight: 700;
}

.ref-switch-icon {
  flex-shrink: 0;
  width: 16px;
  height: 16px;
}

.ref-switch-label {
  min-width: 0;
  overflow-wrap: anywhere;
}

/* The radio is not painted: the whole option is the control. */
.ref-switch-input {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  border: 0;
  overflow: hidden;
  white-space: nowrap;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
}

.ref-switch-option:focus-within {
  outline: 2px solid #4f46e5;
  outline-offset: -2px;
}

/* ---------- Body ---------- */
.ref-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding: 20px 24px 4px;
}

.ref-panel {
  display: block;
}

.ref-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  row-gap: 16px;
  column-gap: 12px;
}

.ref-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.ref-field--full {
  grid-column: 1 / -1;
}

.ref-label {
  font-size: 13px;
  font-weight: 600;
  line-height: 1.4;
  color: #334155;
}

.ref-required {
  margin-left: 3px;
  color: #dc2626;
  font-weight: 600;
}

.ref-hint {
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.ref-error {
  font-size: 12px;
  font-weight: 500;
  line-height: 1.5;
  color: #dc2626;
}

.ref-error--block {
  margin: 16px 0 0;
}


/* ---------- Inputs ---------- */
.ref-field input,
.ref-field select,
.ref-field textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  color: #0f172a;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.45;
  transition:
    border-color 0.15s ease,
    box-shadow 0.15s ease,
    background-color 0.15s ease;
}

.ref-field textarea {
  min-height: 80px;
  resize: vertical;
}

.ref-field input::placeholder,
.ref-field textarea::placeholder {
  color: #94a3b8;
}

.ref-field input:hover,
.ref-field select:hover,
.ref-field textarea:hover {
  border-color: #cbd5e1;
}

.ref-field input:focus,
.ref-field select:focus,
.ref-field textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.15);
}

.ref-field .ref-invalid {
  border-color: #dc2626;
}

/* ---------- Live preview ---------- */
.ref-preview {
  flex-shrink: 0;
  margin: 16px 24px 20px;
  padding: 14px 16px;
  max-height: 26vh;
  overflow-y: auto;
  border: 1px dashed #cbd5e1;
  border-radius: 8px;
  background: #f8fafc;
}

.ref-preview-label {
  display: block;
  margin-bottom: 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.6px;
  text-transform: uppercase;
  color: #64748b;
}

.ref-preview-text {
  margin: 0;
  padding-left: 36px;
  text-indent: -36px;
  font-size: 13.5px;
  line-height: 1.7;
  color: #0f172a;
  overflow-wrap: anywhere;
}

.ref-preview-empty {
  margin: 0;
  font-size: 13px;
  font-style: italic;
  line-height: 1.6;
  color: #94a3b8;
}

/* ---------- Footer ---------- */
.ref-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  border-top: 1px solid #e2e8f0;
  background: #ffffff;
}

.ref-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 10px 20px;
  border: 1px solid transparent;
  border-radius: 8px;
  font-family: inherit;
  font-size: 14px;
  font-weight: 600;
  line-height: 1.2;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    border-color 0.15s ease,
    color 0.15s ease,
    transform 0.1s ease,
    box-shadow 0.15s ease;
}

.ref-btn:focus-visible {
  outline: 2px solid #4f46e5;
  outline-offset: 2px;
}

.ref-btn:disabled {
  opacity: 0.65;
  cursor: not-allowed;
}

.ref-btn--secondary {
  background: #ffffff;
  border-color: #e2e8f0;
  color: #475569;
}

.ref-btn--secondary:hover:not(:disabled) {
  background: #f1f5f9;
}

.ref-btn--primary {
  background: #4f46e5;
  border-color: #4f46e5;
  color: #ffffff;
}

.ref-btn--primary:hover:not(:disabled) {
  background: #4338ca;
  border-color: #4338ca;
}

.ref-btn--primary:active:not(:disabled) {
  transform: scale(0.98);
}

/* ---------- Panel transition ---------- */
.ref-fade-enter-active,
.ref-fade-leave-active {
  transition: opacity 0.2s ease;
}

.ref-fade-enter-from,
.ref-fade-leave-to {
  opacity: 0;
}


/* ---------- Smaller screens ----------
   The card keeps 92% of the viewport width, the two-column field grid stacks into one
   column, the switch keeps both options side by side (tighter padding, labels wrap) and the
   footer buttons share the available width. */
@media (max-width: 640px) {
  .ref-overlay {
    padding: 12px;
  }

  .ref-modal {
    max-height: calc(100vh - 24px);
    max-height: calc(100dvh - 24px);
  }

  .ref-header {
    padding: 20px 18px 16px;
  }

  .ref-switch-row {
    padding: 0 18px;
  }

  .ref-switch {
    height: auto;
    min-height: 48px;
  }

  .ref-switch-option {
    min-height: 40px;
    padding: 8px;
    gap: 6px;
    font-size: 12.5px;
  }

  .ref-body {
    padding: 18px 18px 4px;
  }

  .ref-grid {
    grid-template-columns: 1fr;
  }

  .ref-preview {
    margin: 14px 18px 16px;
  }

  .ref-footer {
    padding: 14px 18px 16px;
  }

  .ref-footer .ref-btn {
    flex: 1 1 auto;
  }
}

/* ---------- Dark mode ----------
   The application follows the operating system (see src/assets/css/base.css), which forces
   its own surfaces with !important. These rules keep the modal intentional in dark mode:
   #0f172a card, #1e293b surfaces, #334155 borders, muted #94a3b8 text and the indigo accent
   for the active option and the primary action. */
@media (prefers-color-scheme: dark) {
  .ref-overlay {
    background: rgba(2, 6, 23, 0.7);
  }

  .ref-modal {
    background: #0f172a !important;
    color: #f1f5f9 !important;
  }

  .ref-title {
    color: #f8fafc !important;
  }

  .ref-subtitle,
  .ref-hint,
  .ref-preview-label {
    color: #94a3b8 !important;
  }

  .ref-close {
    color: #94a3b8 !important;
  }

  .ref-close:hover {
    background: #1e293b !important;
    color: #f8fafc !important;
  }

  .ref-switch {
    background: #1e293b !important;
  }

  .ref-switch-indicator {
    background: #334155 !important;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.35) !important;
  }

  .ref-switch-option {
    color: #94a3b8 !important;
  }

  .ref-switch-option:hover {
    color: #cbd5e1 !important;
  }

  .ref-switch-option--active,
  .ref-switch-option--active:hover {
    color: #f8fafc !important;
  }

  .ref-label {
    color: #cbd5e1 !important;
  }

  .ref-field input,
  .ref-field select,
  .ref-field textarea {
    background: #1e293b !important;
    border-color: #334155 !important;
    color: #f1f5f9 !important;
  }

  .ref-field input::placeholder,
  .ref-field textarea::placeholder {
    color: #64748b !important;
  }

  .ref-field input:focus,
  .ref-field select:focus,
  .ref-field textarea:focus {
    border-color: #818cf8 !important;
    box-shadow: 0 0 0 3px rgba(129, 140, 248, 0.25) !important;
  }

  .ref-preview {
    background: #1e293b !important;
    border-color: #334155 !important;
  }

  .ref-preview-text {
    color: #e2e8f0 !important;
  }

  .ref-preview-empty {
    color: #94a3b8 !important;
  }

  .ref-footer {
    background: #0f172a !important;
    border-top-color: #334155 !important;
  }

  .ref-btn--secondary {
    background: #1e293b !important;
    border-color: #334155 !important;
    color: #cbd5e1 !important;
  }

  .ref-btn--secondary:hover:not(:disabled) {
    background: #334155 !important;
  }

  .ref-btn--primary {
    background: #4f46e5 !important;
    border-color: #4f46e5 !important;
    color: #ffffff !important;
  }

  .ref-btn--primary:hover:not(:disabled) {
    background: #4338ca !important;
    border-color: #4338ca !important;
  }
}

/* ---------- Reduced motion ----------
   The indicator, the panel fade and the small hover/press transitions are decoration: they
   are switched off for anyone who asked the system for less motion. */
@media (prefers-reduced-motion: reduce) {
  .ref-switch-indicator,
  .ref-fade-enter-active,
  .ref-fade-leave-active,
  .ref-btn,
  .ref-close,
  .ref-field input,
  .ref-field select,
  .ref-field textarea {
    transition: none !important;
  }

  .ref-btn--primary:active:not(:disabled) {
    transform: none;
  }
}
</style>

