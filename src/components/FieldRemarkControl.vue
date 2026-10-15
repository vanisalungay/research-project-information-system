<template>
  <!--
    Nothing is rendered when there is nothing to show. A field without a remark
    therefore produces exactly the original markup (no empty wrapper, no extra
    margin), so fields with and without remarks keep identical spacing.
  -->
  <div v-if="!readonly || hasRemark" class="field-remark"
    :class="{ compact, 'has-remark': hasRemark, 'is-open': open }">
    <!-- Read-only rendering (proponent side). -->
    <div v-if="readonly" class="remark-box">
      <p class="remark-head">
        <span class="remark-flag">Reviewer Remark</span>
        <span v-if="primary.reviewerName" class="remark-meta-item">{{ primary.reviewerName }}</span>
        <span v-if="primary.createdAt" class="remark-meta-item">{{ formatDate(primary.createdAt) }}</span>
        <span v-if="primary.entryIndex !== null && primary.entryIndex !== undefined" class="remark-position">entry #{{
          primary.entryIndex + 1 }}</span>
      </p>
      <p class="remark-text">{{ primary.remark }}</p>
    </div>

    <!-- Reviewer rendering: add / edit / remove a remark for this exact field. -->
    <template v-else>
      <button v-if="!hasRemark && !open" type="button" class="remark-trigger" :title="`Add a remark for ${fieldLabel}`"
        @click="startEditing">
        <span class="trigger-icon">＋</span>
        <span v-if="!compact" class="trigger-label">Add Remark</span>
      </button>

      <div v-if="hasRemark && !open" class="remark-box">
        <div class="remark-head">
          <span class="remark-flag">Reviewer Remark</span>
          <span v-if="primary.reviewerRole" class="remark-role">{{ primary.reviewerRole }}</span>
          <button type="button" class="link-btn" @click="startEditing">Edit</button>
          <button type="button" class="link-btn danger" :disabled="busy" @click="remove">Remove</button>
        </div>
        <p class="remark-text">{{ primary.remark }}</p>
        <p class="remark-meta">
          <span v-if="primary.reviewerName">{{ primary.reviewerName }}</span>
          <span v-if="primary.createdAt">{{ formatDate(primary.createdAt) }}</span>
          <span v-if="primary.entryIndex !== null && primary.entryIndex !== undefined" class="remark-position">entry #{{
            primary.entryIndex + 1 }}</span>
        </p>
      </div>

      <div v-if="open" class="remark-editor">
        <label class="editor-label">{{ fieldLabel }}</label>
        <textarea v-model="text" rows="3" :placeholder="`What needs to be corrected in ${fieldLabel}?`"></textarea>
        <div class="editor-actions">
          <button type="button" class="btn-save" :disabled="busy || !text.trim()" @click="save">
            {{ busy ? 'Saving...' : 'Save Remark' }}
          </button>
          <button type="button" class="btn-cancel" :disabled="busy" @click="cancel">Cancel</button>
        </div>
      </div>

      <p v-if="error" class="remark-error">{{ error }}</p>
    </template>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import api from '@/utils/api'

const props = defineProps({
  proposalId: { type: [Number, String], required: true },
  sectionKey: { type: String, required: true },
  fieldKey: { type: String, required: true },
  fieldLabel: { type: String, default: '' },
  entryIndex: { type: Number, default: null },
  entryId: { type: [Number, String], default: null },
  entrySnapshot: { type: String, default: null },
  /** Remarks already stored for this field in the active round. */
  remarks: { type: Array, default: () => [] },
  compact: { type: Boolean, default: false },
  readonly: { type: Boolean, default: false },
})

const emit = defineEmits(['saved', 'removed'])

const open = ref(false)
const text = ref('')
const busy = ref(false)
const error = ref('')

const primary = computed(() => (props.remarks && props.remarks.length ? props.remarks[0] : null))
const hasRemark = computed(() => Boolean(primary.value))

const startEditing = () => {
  text.value = primary.value ? primary.value.remark : ''
  error.value = ''
  open.value = true
}

const cancel = () => {
  open.value = false
  error.value = ''
}

const save = async () => {
  if (!text.value.trim()) return
  busy.value = true
  error.value = ''
  try {
    const res = await api.put(`/api/proposals/${props.proposalId}/field-remarks`, {
      sectionKey: props.sectionKey,
      fieldKey: props.fieldKey,
      fieldLabel: props.fieldLabel,
      entryIndex: props.entryIndex,
      entryId: props.entryId,
      entrySnapshot: props.entrySnapshot,
      remark: text.value.trim(),
    })
    emit('saved', res.data)
    open.value = false
  } catch (err) {
    console.error(err)
    error.value =
      err.response?.data?.error || err.response?.data || 'Failed to save the remark. Please try again.'
  } finally {
    busy.value = false
  }
}

const remove = async () => {
  if (!primary.value) return
  busy.value = true
  error.value = ''
  try {
    await api.delete(`/api/proposals/${props.proposalId}/field-remarks/${primary.value.id}`)
    emit('removed', primary.value.id)
  } catch (err) {
    console.error(err)
    error.value =
      err.response?.data?.error || err.response?.data || 'Failed to remove the remark. Please try again.'
  } finally {
    busy.value = false
  }
}

function formatDate(value) {
  if (!value) return ''
  const d = new Date(value)
  return isNaN(d.getTime()) ? String(value) : d.toLocaleString()
}
</script>

<style scoped>
.field-remark {
  /* No margin here: spacing lives on the note itself, so a field without a remark
     contributes nothing at all to the layout. */
  min-width: 0;
  max-width: 100%;
}

.remark-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  background: #ffffff;
  border: 1px dashed #cbd5e1;
  border-radius: 6px;
  color: #4f46e5;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  cursor: pointer;
}

.remark-trigger:hover {
  border-color: #4f46e5;
  background: #eef2ff;
}

.trigger-icon {
  font-size: 12px;
  line-height: 1;
}

.remark-box {
  /* Compact note that reads as part of its field: a single amber accent on the
     left instead of a full box, and spacing that matches the parent's own rhythm. */
  margin-top: 4px;
  padding: 4px 8px;
  background: #fffbeb;
  border-left: 3px solid #f59e0b;
  border-radius: 0 4px 4px 0;
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
}

.remark-head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px;
  margin: 0 0 2px;
}

.remark-flag {
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  color: #92400e;
}

.link-btn {
  background: none;
  border: none;
  padding: 0;
  font-family: inherit;
  font-size: 10px;
  font-weight: 600;
  color: #4f46e5;
  cursor: pointer;
}

.link-btn.danger {
  color: #dc2626;
}

.remark-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.4;
  color: #3f3f46;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

.remark-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 2px 0 0;
  font-size: 10px;
  color: #78716c;
}

.remark-meta-item {
  font-size: 10px;
  color: #a16207;
}

.remark-role {
  font-weight: 700;
  color: #92400e;
}

.remark-position {
  color: #b45309;
  font-weight: 600;
  font-size: 10px;
}

.remark-editor {
  border: 1px solid #c7d2fe;
  border-radius: 6px;
  padding: 8px;
  background: #f8fafc;
}

.editor-label {
  display: block;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #6b7280;
  margin-bottom: 4px;
}

textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid #d1d5db;
  border-radius: 5px;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.45;
  padding: 6px 8px;
  resize: vertical;
  color: #1f2937;
}

textarea:focus {
  outline: none;
  border-color: #4f46e5;
}

.editor-actions {
  display: flex;
  gap: 6px;
  margin-top: 6px;
}

.btn-save,
.btn-cancel {
  border-radius: 5px;
  border: none;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  padding: 4px 10px;
  cursor: pointer;
}

.btn-save {
  background: #4f46e5;
  color: #ffffff;
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-cancel {
  background: #e5e7eb;
  color: #374151;
}

.remark-error {
  margin: 4px 0 0;
  font-size: 11px;
  color: #dc2626;
}
</style>
