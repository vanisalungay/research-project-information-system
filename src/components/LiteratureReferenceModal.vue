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
            <h2 id="reference-modal-title">{{ modalTitle }}</h2>
            <p class="ref-header-sub">
              Generate an APA 7th edition reference from the source details, or paste one you
              already have.
            </p>
          </div>
          <button ref="closeButton" class="ref-close" aria-label="Close" @click="close">✕</button>
        </header>

        <!-- METHOD SELECTION -->
        <div class="ref-tabs" role="tablist" aria-label="How to add the reference">
          <button
            type="button"
            class="ref-tab"
            role="tab"
            :class="{ 'ref-tab--active': mode === 'generate' }"
            :aria-selected="mode === 'generate'"
            @click="mode = 'generate'"
          >
            Generate APA Reference
          </button>
          <button
            type="button"
            class="ref-tab"
            role="tab"
            :class="{ 'ref-tab--active': mode === 'manual' }"
            :aria-selected="mode === 'manual'"
            @click="mode = 'manual'"
          >
            Enter APA Reference Manually
          </button>
        </div>

        <!-- BODY -->
        <div class="ref-body">
          <!-- OPTION A: the structured builder -->
          <section v-if="mode === 'generate'" class="ref-panel" role="tabpanel">
            <div class="ref-field ref-field--full">
              <label :for="`ref-type-${uid}`">Reference Type</label>
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

            <div class="ref-grid">
              <div
                v-for="field in fields"
                :key="field.key"
                class="ref-field"
                :class="{ 'ref-field--full': field.full || field.kind === 'textarea' }"
              >
                <label :for="`ref-${field.key}-${uid}`">{{ field.label }}</label>

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
                <span v-if="fieldErrors[field.key]" class="ref-error">{{
                  fieldErrors[field.key]
                }}</span>
              </div>
            </div>

            <button type="button" class="btn-primary ref-generate" @click="generate">
              Generate APA Reference
            </button>

            <p v-if="generateError" class="ref-error ref-error--block">{{ generateError }}</p>

            <!-- PREVIEW -->
            <div v-if="preview.text" class="ref-preview">
              <span class="ref-preview-label">Preview</span>
              <p class="ref-preview-text" v-html="preview.html"></p>
            </div>

            <p v-else-if="detailsEditing" class="ref-hint ref-hint--block">
              Adjust the details above, then generate the reference again.
            </p>
          </section>

          <!-- OPTION B: a reference the proponent already prepared -->
          <section v-else class="ref-panel" role="tabpanel">
            <div class="ref-field ref-field--full">
              <label :for="`ref-manual-${uid}`">Enter APA Reference Manually</label>
              <textarea
                :id="`ref-manual-${uid}`"
                v-model="manualText"
                rows="5"
                placeholder="Paste your complete APA 7th edition reference here..."
                :class="{ 'ref-invalid': manualError }"
              ></textarea>
              <span class="ref-hint">
                The text is saved exactly as you write it - nothing is rewritten or reformatted.
              </span>
              <span v-if="manualError" class="ref-error">{{ manualError }}</span>
            </div>
          </section>
        </div>

        <!-- FOOTER -->
        <footer class="ref-footer">
          <button type="button" class="btn-secondary" @click="close">Cancel</button>
          <div class="ref-footer-actions">
            <button
              v-if="mode === 'generate' && preview.text"
              type="button"
              class="btn-secondary"
              @click="editDetails"
            >
              Edit Details
            </button>
            <button type="button" class="btn-primary" @click="addReference">
              {{ isEditing ? 'Save Changes' : 'Add to Literature Cited' }}
            </button>
          </div>
        </footer>
      </div>
    </div>
  </Teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import {
  MANUAL_REFERENCE_TYPE,
  REFERENCE_TYPES,
  emptyReferenceFields,
  formatReference,
  makeGeneratedEntry,
  makeManualEntry,
  referenceFields,
  validateReference,
} from '@/utils/apaReferences'

/**
 * "Add Literature Reference" modal for section 23 (Literature Cited).
 *
 * Two independent ways of adding a reference:
 *   Generate APA Reference   the structured builder - a reference type and only the
 *                            fields that type needs, validated before generation.
 *   Enter APA Reference Manually  a free text area for a reference the proponent
 *                            already formatted elsewhere; it is stored untouched.
 *
 * Switching between the two never loses anything: the entered values, the generated
 * preview and the pasted text all stay in this component's state.
 *
 * When an existing entry is passed in, the same modal edits it (the values are
 * restored, and the primary action becomes "Save Changes").
 */
const props = defineProps({
  /** The entry being edited, or null when a new reference is added. */
  entry: { type: Object, default: null },
})

const emit = defineEmits(['save', 'close'])

const uid = Math.random().toString(36).slice(2, 8)

const isEditing = computed(() => Boolean(props.entry))
const modalTitle = computed(() =>
  isEditing.value ? 'Edit Literature Reference' : 'Add Literature Reference',
)

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
const preview = ref({ text: '', html: '' })
const detailsEditing = ref(false)
const closeButton = ref(null)

/** The fields of the selected type - only the ones that type actually needs. */
const fields = computed(() => referenceFields(typeKey.value))

/** A different type starts from its own blank field set (nothing is invented). */
function onTypeChange(key) {
  typeKey.value = key
  for (const field of fields.value) values[field.key] = ''
  Object.assign(values, emptyReferenceFields(key))
  clearErrors()
  preview.value = { text: '', html: '' }
  detailsEditing.value = false
}

function clearErrors() {
  for (const key of Object.keys(fieldErrors)) delete fieldErrors[key]
  generateError.value = ''
  manualError.value = ''
}

/** Validates the required fields and builds the reference; nothing is invented. */
function generate() {
  clearErrors()
  const result = validateReference(typeKey.value, values)
  if (!result.valid) {
    Object.assign(fieldErrors, result.errors)
    generateError.value = 'Please complete the highlighted fields to generate the reference.'
    preview.value = { text: '', html: '' }
    return
  }
  preview.value = formatReference(typeKey.value, values)
  detailsEditing.value = false
}

/** Back to the details: the preview is hidden while the values are adjusted. */
function editDetails() {
  preview.value = { text: '', html: '' }
  detailsEditing.value = true
}

function close() {
  emit('close')
}

/** Hands the finished reference to the page (Add to Literature Cited / Save Changes). */
function addReference() {
  clearErrors()

  if (mode.value === 'manual') {
    const text = String(manualText.value ?? '').trim()
    if (!text) {
      manualError.value = 'Enter or paste a complete APA reference first.'
      return
    }
    const created = makeManualEntry(text)
    emit('save', props.entry ? { ...created, id: props.entry.id } : created)
    return
  }

  const result = validateReference(typeKey.value, values)
  if (!result.valid) {
    Object.assign(fieldErrors, result.errors)
    generateError.value = 'Please complete the highlighted fields to add the reference.'
    return
  }
  if (!preview.value.text) {
    generateError.value = 'Generate the reference first, then add it to the literature cited.'
    return
  }
  const created = makeGeneratedEntry(typeKey.value, values)
  emit('save', props.entry ? { ...created, id: props.entry.id } : created)
}

// Escape closes the modal, like the other dialogs of the application.
function onKeydown(event) {
  if (event.key === 'Escape') close()
}

onMounted(async () => {
  window.addEventListener('keydown', onKeydown)
  await nextTick()
  closeButton.value?.focus?.()
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown)
})
</script>

<style scoped>
/* ============================================================
   Add Literature Reference modal
   Chrome mirrors the other modals of the Create Proposal form: fixed overlay, white
   card, header with the title and a close button, a body that scrolls on its own and
   a footer with the actions.
   ============================================================ */
.ref-overlay {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: rgba(15, 23, 42, 0.7);
  backdrop-filter: blur(4px);
}

.ref-modal {
  display: flex;
  flex-direction: column;
  width: 100%;
  max-width: 900px;
  max-height: calc(100vh - 40px);
  max-height: calc(100dvh - 40px);
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
  overflow: hidden;
}

/* ===== HEADER ===== */
.ref-header {
  flex-shrink: 0;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 24px;
  border-bottom: 1px solid #e2e8f0;
}

.ref-header-text h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.ref-header-sub {
  margin: 4px 0 0;
  font-size: 12px;
  line-height: 1.5;
  color: #64748b;
}

.ref-close {
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

.ref-close:hover {
  background: #f1f5f9;
  color: #475569;
}

.ref-close:focus-visible,
.ref-tab:focus-visible {
  outline: 2px solid #4f46e5;
  outline-offset: 2px;
}

/* ===== METHOD TABS ===== */
.ref-tabs {
  flex-shrink: 0;
  display: flex;
  gap: 8px;
  padding: 14px 24px 0;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.ref-tab {
  padding: 9px 16px;
  border: 1px solid #e2e8f0;
  border-bottom: none;
  border-radius: 10px 10px 0 0;
  background: #f1f5f9;
  color: #64748b;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.ref-tab:hover {
  color: #4f46e5;
}

.ref-tab--active {
  background: #fff;
  color: #4f46e5;
  border-color: #c7d2fe;
}

/* ===== BODY ===== */
.ref-body {
  flex: 1 1 auto;
  min-height: 0;
  overflow-y: auto;
  padding: 20px 24px;
  background: #fff;
}

.ref-panel {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.ref-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}

.ref-field {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.ref-field--full {
  grid-column: 1 / -1;
}

.ref-field label {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.ref-field input,
.ref-field select,
.ref-field textarea {
  width: 100%;
  padding: 9px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  color: #1e293b;
  background: #fff;
  box-sizing: border-box;
}

.ref-field textarea {
  resize: vertical;
  min-height: 68px;
}

.ref-field input:focus,
.ref-field select:focus,
.ref-field textarea:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.ref-invalid {
  border-color: #dc2626 !important;
  background: #fef2f2;
}

.ref-hint {
  font-size: 11px;
  line-height: 1.5;
  color: #94a3b8;
}

.ref-hint--block {
  margin: 0;
  font-size: 12px;
}

.ref-error {
  font-size: 12px;
  font-weight: 500;
  color: #dc2626;
}

.ref-error--block {
  margin: 0;
  padding: 9px 12px;
  border: 1px solid #fecaca;
  border-radius: 8px;
  background: #fef2f2;
}

/* ===== GENERATE + PREVIEW ===== */
.ref-generate {
  align-self: flex-start;
  font-size: 13px;
  padding: 10px 18px;
}

.ref-preview {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid #c7d2fe;
  border-radius: 12px;
  background: #f8faff;
}

.ref-preview-label {
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #4f46e5;
}

.ref-preview-text {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: #1e293b;
  overflow-wrap: anywhere;
}

/* ===== FOOTER ===== */
.ref-footer {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
}

.ref-footer-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-primary,
.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 8px;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-primary {
  background: #4f46e5;
  border: 1px solid #4f46e5;
  color: #fff;
}

.btn-primary:hover {
  background: #4338ca;
  border-color: #4338ca;
}

.btn-secondary {
  background: #f1f5f9;
  border: 1px solid #e2e8f0;
  color: #475569;
  font-weight: 500;
}

.btn-secondary:hover {
  background: #e2e8f0;
}

/* ===== SMALL SCREENS =====
   One field per row, the footer stacks and the modal keeps the viewport width. */
@media (max-width: 640px) {
  .ref-overlay {
    padding: 8px;
  }

  .ref-modal {
    max-height: calc(100vh - 16px);
    max-height: calc(100dvh - 16px);
  }

  .ref-header,
  .ref-body,
  .ref-footer {
    padding-left: 16px;
    padding-right: 16px;
  }

  .ref-tabs {
    padding-left: 16px;
    padding-right: 16px;
    flex-wrap: wrap;
  }

  .ref-grid {
    grid-template-columns: 1fr;
  }

  .ref-field--full {
    grid-column: auto;
  }

  .ref-footer {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .ref-footer-actions {
    flex-direction: column-reverse;
    align-items: stretch;
  }

  .btn-primary,
  .btn-secondary {
    justify-content: center;
  }
}
</style>
