<template>
  <div class="section-remarks-editor">
    <div class="editor-header">
      <div>
        <h4>Section Remarks</h4>
        <p class="helper">
          Leave a comment against each section that needs to be corrected. The proponent
          sees every remark beside the corresponding section on the Revision Page.
        </p>
      </div>
      <span class="flagged-count" :class="{ none: flaggedCount === 0 }">
        {{ flaggedCount }} {{ flaggedCount === 1 ? 'section' : 'sections' }} flagged
      </span>
    </div>

    <div class="section-list">
      <div v-for="section in PROPOSAL_SECTIONS" :key="section.key" class="section-row">
        <div class="section-row-head">
          <span class="section-number">{{ section.key }}</span>
          <span class="section-title">{{ section.label }}</span>
          <button
            v-if="hasValue(section.key)"
            type="button"
            class="clear-btn"
            @click="clearRemark(section.key)"
          >
            Clear
          </button>
        </div>
        <textarea
          :value="modelValue[section.key] || ''"
          :placeholder="`Revision remark for ${section.label}...`"
          rows="2"
          @input="updateRemark(section.key, $event.target.value)"
        ></textarea>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { PROPOSAL_SECTIONS } from '@/utils/proposalSections'

const props = defineProps({
  /** `{ '12': 'remark text', ... }` keyed by canonical section key. */
  modelValue: {
    type: Object,
    default: () => ({}),
  },
})

const emit = defineEmits(['update:modelValue'])

const flaggedCount = computed(
  () => Object.values(props.modelValue || {}).filter((value) => String(value || '').trim() !== '').length
)

const hasValue = (key) => String(props.modelValue?.[key] || '').trim() !== ''

const updateRemark = (key, value) => {
  emit('update:modelValue', { ...(props.modelValue || {}), [key]: value })
}

const clearRemark = (key) => {
  const next = { ...(props.modelValue || {}) }
  delete next[key]
  emit('update:modelValue', next)
}
</script>

<style scoped>
.section-remarks-editor {
  width: 100%;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 12px;
}

.editor-header h4 {
  margin: 0 0 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.helper {
  margin: 0;
  font-size: 12px;
  color: #6b7280;
  line-height: 1.45;
  max-width: 60ch;
}

.flagged-count {
  flex-shrink: 0;
  padding: 4px 10px;
  border-radius: 999px;
  background: #fef3c7;
  color: #92400e;
  font-size: 11px;
  font-weight: 700;
  white-space: nowrap;
}

.flagged-count.none {
  background: #f1f5f9;
  color: #64748b;
}

.section-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 460px;
  overflow-y: auto;
  padding-right: 4px;
}

.section-row {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 8px 10px;
  background: #ffffff;
}

.section-row-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.section-number {
  font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
  font-size: 11px;
  font-weight: 700;
  color: #4338ca;
  background: #eef2ff;
  border-radius: 4px;
  padding: 1px 6px;
}

.section-title {
  font-size: 12px;
  font-weight: 600;
  color: #374151;
  flex: 1;
}

.clear-btn {
  background: none;
  border: none;
  color: #dc2626;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  padding: 0 2px;
}

textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 7px 9px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-family: inherit;
  font-size: 12px;
  line-height: 1.45;
  resize: vertical;
  color: #1f2937;
}

textarea:focus {
  outline: none;
  border-color: #4f46e5;
}
</style>
