<template>
  <div ref="rootRef" class="program-title-select">
    <!-- The control that replaces the native <select> this field used to render: a
         real list is needed so the "Create New Program Title" option can carry the
         application's blue (browsers style native option backgrounds inconsistently)
         and so a double click can be handled instead of being swallowed by the
         browser's own popup. Everything else - the placeholder, the create action and
         the proponent's own titles - is exactly what the dropdown already offered. -->
    <button
      type="button"
      class="pts-trigger"
      :class="{ 'is-open': open }"
      role="combobox"
      aria-haspopup="listbox"
      :aria-expanded="open ? 'true' : 'false'"
      :aria-controls="listId"
      :aria-activedescendant="open && activeIndex >= 0 ? `${listId}-${activeIndex}` : undefined"
      :disabled="loading"
      :title="currentLabel"
      @click="onTriggerClick"
      @keydown.down.prevent="moveActive(1)"
      @keydown.up.prevent="moveActive(-1)"
      @keydown.home.prevent="setActive(0)"
      @keydown.end.prevent="setActive(options.length - 1)"
      @keydown.enter.prevent="onEnter"
      @keydown.space.prevent="onEnter"
      @keydown.esc.stop="closeList"
      @keydown.tab="closeList"
    >
      <span class="pts-label">{{ currentLabel }}</span>
      <svg class="pts-caret" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor"
        stroke-width="2" aria-hidden="true">
        <polyline points="6 9 12 15 18 9" />
      </svg>
    </button>

    <!-- The list. The create action sits directly under the placeholder and is also
         the keyboard and touch route to the same flow a double click opens. -->
    <ul v-if="open" :id="listId" class="pts-list" role="listbox" :aria-label="placeholder">
      <li v-for="(option, index) in options" :id="`${listId}-${index}`"
        :key="option.create ? 'create-new' : option.value || 'placeholder'" class="pts-option"
        :class="{
          'pts-option--create': option.create,
          'is-active': index === activeIndex,
          'is-selected': option.selected
        }" role="option" :aria-selected="option.selected ? 'true' : 'false'" @click="choose(option)"
        @mousemove="activeIndex = index">
        {{ option.label }}
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'

import {
  PROGRAM_TITLE_PLACEHOLDER,
  programTitleClickAction,
  programTitleLabel,
  programTitleOptions
} from '@/utils/programTitleOptions'

const props = defineProps({
  /** The current value: '' (nothing selected), the create sentinel, or a title. */
  modelValue: { type: String, default: '' },
  /** The proponent's own previously used program titles. */
  titles: { type: Array, default: () => [] },
  /** True while the titles are being read: the field cannot be used yet. */
  loading: { type: Boolean, default: false },
  placeholder: { type: String, default: PROGRAM_TITLE_PLACEHOLDER }
})
const emit = defineEmits(['update:modelValue', 'change', 'create'])

const listId = `program-title-list-${useId()}`

const rootRef = ref(null)
const open = ref(false)
const activeIndex = ref(-1)

const options = computed(() =>
  programTitleOptions({
    titles: props.titles,
    selected: props.modelValue,
    placeholder: props.placeholder
  })
)
const currentLabel = computed(() => programTitleLabel(options.value, props.placeholder))

const setActive = (index) => {
  activeIndex.value = Math.min(Math.max(index, 0), Math.max(options.value.length - 1, 0))
}

const openList = () => {
  open.value = true
  const selected = options.value.findIndex((option) => option.selected)
  activeIndex.value = selected >= 0 ? selected : 0
}

const closeList = () => {
  open.value = false
  activeIndex.value = -1
}

/**
 * Selects an option. The create action is also announced to the form, which opens its
 * own inline "new program title" input (the existing creation flow).
 */
const choose = (option) => {
  closeList()
  emit('update:modelValue', option.value)
  emit('change', option.value)
  if (option.create) emit('create')
}

const onEnter = () => {
  if (!open.value) {
    openList()
    return
  }
  const option = options.value[activeIndex.value]
  if (option) choose(option)
  else closeList()
}

/** Arrow keys open the list and move the highlight, wrapping at both ends. */
const moveActive = (step) => {
  if (!open.value) {
    openList()
    return
  }
  const count = options.value.length
  const next = activeIndex.value < 0 ? 0 : (activeIndex.value + step + count) % count
  setActive(next)
}

/**
 * One click opens or closes the list. A double click opens the existing
 * "create a new program title" flow instead: the click counter keeps the two apart,
 * so the second click of the pair never selects an unrelated title and no second
 * dialog can be opened while the creation form is already showing.
 */
const onTriggerClick = (event) => {
  const action = programTitleClickAction(event.detail, open.value)
  if (action === 'create') {
    closeList()
    emit('create')
    return
  }
  if (action === 'close') closeList()
  else openList()
}

const onDocumentPointerDown = (event) => {
  if (!rootRef.value?.contains(event.target)) closeList()
}

watch(open, (isOpen) => {
  if (typeof document === 'undefined') return
  if (isOpen) document.addEventListener('mousedown', onDocumentPointerDown)
  else document.removeEventListener('mousedown', onDocumentPointerDown)
})

onBeforeUnmount(() => {
  if (typeof document !== 'undefined') document.removeEventListener('mousedown', onDocumentPointerDown)
})
</script>

<style scoped>
/* The closed field: the same size, spacing and colours as the other form controls
   (it replaces the native select this field used to show). */
.program-title-select {
  position: relative;
}

.pts-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d1d5db;
  border-radius: 8px;
  background: #fff;
  font-family: inherit;
  font-size: 14px;
  color: #1e293b;
  text-align: left;
  cursor: pointer;
  box-sizing: border-box;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.pts-trigger:hover:not(:disabled) {
  border-color: #4f46e5;
}

.pts-trigger:focus-visible,
.pts-trigger.is-open {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.1);
}

.pts-trigger:disabled {
  background: #f8fafc;
  color: #94a3b8;
  cursor: not-allowed;
}

/* The long placeholder stays on one line and is trimmed with an ellipsis instead of
   widening the field; the full text is always available as the field's tooltip. */
.pts-label {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pts-caret {
  flex-shrink: 0;
  color: #94a3b8;
}

.pts-list {
  position: absolute;
  z-index: 40;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  margin: 0;
  padding: 6px;
  max-height: 260px;
  overflow-y: auto;
  list-style: none;
  background: #fff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.16);
}

.pts-option {
  padding: 9px 12px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.4;
  color: #1e293b;
  cursor: pointer;
}

.pts-option.is-active {
  background: #f1f5f9;
}

.pts-option.is-selected {
  font-weight: 600;
}

/* The create action: the application's blue with white text, so it stands out from
   the regular titles. It is a real element, so every browser renders it the same. */
.pts-option--create {
  background: #4f46e5;
  color: #ffffff;
  font-weight: 600;
  margin-bottom: 4px;
}

.pts-option--create.is-active,
.pts-option--create:hover {
  background: #4338ca;
}

.pts-option--create.is-selected {
  box-shadow: inset 0 0 0 2px #c7d2fe;
}
</style>
