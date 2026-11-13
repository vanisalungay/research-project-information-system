<template>
  <div ref="rootRef" class="agenda-picker">
    <!-- A real text input, because this field is meant to be searched: a native
         <select> cannot filter its own options as the administrator types. The
         options are the agenda records the page loaded from the API. -->
    <input ref="inputRef" v-model="query" type="text" class="ap-input" role="combobox"
      aria-haspopup="listbox" :aria-expanded="open ? 'true' : 'false'" :aria-controls="listId"
      :aria-activedescendant="open && activeIndex >= 0 ? listId + '-' + activeIndex : undefined"
      :placeholder="placeholder" autocomplete="off" @focus="openList" @input="openList"
      @keydown.down.prevent="moveActive(1)" @keydown.up.prevent="moveActive(-1)"
      @keydown.home.prevent="setActive(0)" @keydown.end.prevent="setActive(rows.length - 1)"
      @keydown.enter.prevent="onEnter" @keydown.esc.stop="closeList" @keydown.tab="closeList" />

    <ul v-if="open" :id="listId" class="ap-list" role="listbox" aria-label="Existing agendas">
      <li v-if="!rows.length" class="ap-empty">{{ emptyLabel }}</li>
      <li v-for="(row, index) in rows" :id="listId + '-' + index" :key="row.optionKey" class="ap-option"
        :class="{ 'is-active': index === activeIndex, 'is-create': row.kind === 'create', 'is-disabled': row.disabled }"
        role="option" :aria-selected="index === activeIndex ? 'true' : 'false'"
        :aria-disabled="row.disabled ? 'true' : 'false'" @mousemove="activeIndex = index" @click="choose(row)">
        <template v-if="row.kind === 'create'">{{ row.label }}</template>
        <template v-else>
          <span class="ap-name">{{ row.record.name }}</span>
          <span class="ap-meta">
            <span v-if="row.record.frameworkHeading" class="ap-framework">{{ row.record.frameworkHeading }}</span>
            <span v-if="row.record.subLabel" class="ap-sub">{{ row.record.subLabel }}</span>
            <span v-if="row.disabled" class="ap-flag">Already in this framework</span>
          </span>
        </template>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'

import { createAgendaLabel, searchAgendas } from '@/utils/agendaPicker'

/**
 * The searchable "existing agenda" dropdown of the Add Agenda form.
 *
 * Its options are the agenda records the page loaded from the API, so anything an
 * administrator saved earlier - in this framework or in another one - can be
 * searched and reused instead of retyped. Selecting a record only emits it: the form
 * owns the fields, the validation and the save.
 */

/** How many matches are shown at once; the list scrolls beyond that. */
const LIMIT = 8

const props = defineProps({
  /** The reusable records (see agendaCatalogue) - never a hard-coded list. */
  agendas: { type: Array, default: () => [] },
  /** The framework the new agenda is being added to. */
  targetSetId: { type: [Number, String], default: null },
  placeholder: { type: String, default: 'Search an existing agenda by name, key or prompt...' },
})
const emit = defineEmits(['select', 'create'])

const listId = 'agenda-picker-list-' + useId()

const rootRef = ref(null)
const inputRef = ref(null)
const query = ref('')
const open = ref(false)
const activeIndex = ref(-1)

/**
 * The rows to show: the matching existing agendas, plus - only when nothing matched -
 * the action that creates the typed agenda instead. A record that is already part of
 * this framework is listed but not selectable, which is how a duplicate is prevented
 * before the backend has to refuse it.
 */
const rows = computed(() => {
  const typed = query.value.trim()
  const matches = searchAgendas(props.agendas, typed, {
    targetSetId: props.targetSetId ?? null,
    limit: LIMIT,
  })

  const list = matches.map((record) => ({
    kind: 'agenda',
    optionKey: 'agenda-' + (record.id ?? record.name),
    record,
    disabled: record.alreadyInFramework,
  }))

  if (typed !== '' && matches.length === 0) {
    list.push({
      kind: 'create',
      optionKey: 'create-new',
      label: createAgendaLabel(typed),
      disabled: false,
    })
  }
  return list
})

/** What the empty list says: nothing is configured yet, or nothing matched. */
const emptyLabel = computed(() =>
  query.value.trim() === ''
    ? 'No agenda has been saved yet. Type a name and save it to add the first one.'
    : 'No matching agenda.',
)

function openList() {
  open.value = true
  if (activeIndex.value >= rows.value.length) activeIndex.value = -1
}

function closeList() {
  open.value = false
  activeIndex.value = -1
}

function setActive(index) {
  if (!open.value || !rows.value.length) return
  activeIndex.value = Math.min(Math.max(index, 0), rows.value.length - 1)
}

function moveActive(step) {
  if (!open.value) {
    openList()
    return
  }
  const next = activeIndex.value + step
  if (next < 0) activeIndex.value = rows.value.length - 1
  else if (next >= rows.value.length) activeIndex.value = 0
  else activeIndex.value = next
}

function choose(row) {
  if (!row || row.disabled) return
  if (row.kind === 'create') emit('create', query.value.trim())
  else emit('select', row.record)
  query.value = ''
  closeList()
}

function onEnter() {
  if (open.value && activeIndex.value >= 0 && rows.value[activeIndex.value]) {
    choose(rows.value[activeIndex.value])
    return
  }
  // With nothing highlighted, the first reusable match is the sensible default.
  const first = rows.value.find((row) => !row.disabled)
  if (first) choose(first)
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
.agenda-picker {
  position: relative;
}

/* The same size, spacing and colours as the other form controls on this page. */
.ap-input {
  width: 100%;
  padding: 9px 11px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-family: inherit;
  font-size: 13px;
  box-sizing: border-box;
}

.ap-input:focus {
  outline: none;
  border-color: #4f46e5;
  box-shadow: 0 0 0 3px rgba(79, 70, 229, 0.12);
}

.ap-list {
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
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  box-shadow: 0 12px 28px rgba(15, 23, 42, 0.16);
}

.ap-empty {
  padding: 10px 12px;
  font-size: 12.5px;
  color: #6b7280;
  line-height: 1.45;
}

.ap-option {
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
}

.ap-option.is-active {
  background: #f1f5f9;
}

.ap-option.is-disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

/* The create action carries the application's blue with white text, so it stands
   apart from the existing agendas. */
.ap-option.is-create {
  background: #4f46e5;
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
}

.ap-option.is-create.is-active,
.ap-option.is-create:hover {
  background: #4338ca;
}

.ap-name {
  display: block;
  font-size: 13px;
  font-weight: 600;
  color: #1e293b;
}

.ap-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 3px;
}

.ap-framework {
  padding: 1px 8px;
  border-radius: 999px;
  background: #f1f5f9;
  font-size: 11px;
  color: #475569;
}

.ap-sub {
  font-size: 11px;
  color: #64748b;
}

.ap-flag {
  padding: 1px 8px;
  border-radius: 999px;
  background: #fef3c7;
  font-size: 11px;
  font-weight: 600;
  color: #92400e;
}
</style>
