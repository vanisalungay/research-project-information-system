<template>
  <span
    class="percentage-input"
    :class="{
      'percentage-input--empty': isEmpty,
      'percentage-input--invalid': isInvalid,
    }"
  >
    <input
      ref="inputRef"
      type="text"
      inputmode="decimal"
      autocomplete="off"
      spellcheck="false"
      class="percentage-input__field"
      :value="displayValue"
      :aria-label="ariaLabel || 'Percentage'"
      :aria-invalid="isInvalid ? 'true' : undefined"
      @input="onInput"
      @blur="onBlur"
      @keydown="onKeydown"
    />
    <!-- The sign is a display treatment, never part of the model value. -->
    <span v-if="!isEmpty" class="percentage-input__symbol" aria-hidden="true">%</span>
  </span>
</template>

<script setup>
import { computed, ref } from 'vue'
import {
  normalizePercentage,
  percentageFormValue,
  sanitizePercentageInput,
  isValidPercentage,
} from '@/utils/percentageInput.js'

/**
 * A percentage field of the Quarterly Progress Report.
 *
 * The proponent types only the number; the "%" sign is rendered beside it. Alphabetic
 * characters, a manually typed "%" and a second decimal point are prevented while
 * typing, and a value above 100 is reduced to 100. The model carries the numeric text
 * ("75", "75.5") - never the sign. An empty field stays empty, which is different from
 * 0 (0 means "reported 0%", empty means "not reported").
 */
const props = defineProps({
  modelValue: { type: [String, Number], default: '' },
  ariaLabel: { type: String, default: '' },
})
const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)

// The stored value without a legacy "%", so opening an old report never shows "80%%".
const displayValue = computed(() => percentageFormValue(props.modelValue))
const isEmpty = computed(() => displayValue.value === '')
const isInvalid = computed(() => !isValidPercentage(displayValue.value))

function onInput(event) {
  const input = event.target
  const cleaned = sanitizePercentageInput(input.value)
  // Write the sanitized text back so a rejected character never stays in the field.
  if (input.value !== cleaned) input.value = cleaned
  emit('update:modelValue', cleaned)
}

function onBlur() {
  const normalized = normalizePercentage(props.modelValue)
  if (normalized !== props.modelValue) emit('update:modelValue', normalized)
}

function onKeydown(event) {
  if (event.ctrlKey || event.metaKey || event.altKey) return
  // Single-character keys are the ones that insert text. Allow digits, the decimal
  // point and navigation/editing keys; block letters, "%" and the sign characters.
  if (event.key.length === 1 && !/[\d.]/.test(event.key)) event.preventDefault()
}
</script>

<style scoped>
.percentage-input {
  display: flex;
  align-items: center;
  gap: 1px;
  width: 100%;
  min-width: 0;
  color: inherit;
  font: inherit;
  font-size: inherit;
}

.percentage-input__field {
  width: 100%;
  min-width: 0;
  padding: 0;
  border: none;
  background: transparent;
  color: inherit;
  font: inherit;
  font-size: inherit;
  text-align: right;
  outline: none;
}

.percentage-input__field:focus {
  background: transparent;
}

/* The sign is part of the display only. Its size and colour follow the cell
   that contains the field, so the same component fits the report table and
   the compact workplan grid. */
.percentage-input__symbol {
  flex: none;
  font: inherit;
  font-size: inherit;
  color: #7a879c;
}

.percentage-input--invalid .percentage-input__field {
  color: #b91c1c;
  text-decoration: underline;
  text-decoration-color: #dc2626;
}

.percentage-input--empty .percentage-input__field {
  background: transparent;
}
</style>
