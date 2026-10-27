<template>
  <div class="tr-entries">
    <!-- Document view: the entries as they are printed in the report. -->
    <template v-if="readonly">
      <template v-if="filled.length">
        <template v-if="display === 'references'">
          <p v-for="(entry, i) in filled" :key="i" class="tr-reference">{{ referenceLine(entry) }}</p>
        </template>
        <table v-else class="tr-grid tr-grid--list">
          <thead>
            <tr>
              <th v-for="field in fields" :key="field.key">{{ field.label }}</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(entry, i) in filled" :key="i">
              <td v-for="field in fields" :key="field.key" :class="{ 'tr-cell--text': field.multiline }">
                {{ entry[field.key] }}
              </td>
            </tr>
          </tbody>
        </table>
      </template>
    </template>

    <!-- Form: one card per entry, with add and remove controls. -->
    <template v-else>
      <p v-if="hint" class="tr-hint">{{ hint }}</p>

      <div v-for="(entry, i) in entries" :key="i" class="tr-entry">
        <div class="tr-entry-head">
          <span class="tr-entry-index">{{ entryLabel }} {{ i + 1 }}</span>
          <button
            type="button"
            class="tr-btn tr-btn--remove"
            :aria-label="`Remove ${entryLabel.toLowerCase()} ${i + 1}`"
            @click="removeEntry(i)"
          >
            Remove
          </button>
        </div>
        <div class="tr-entry-grid">
          <label
            v-for="field in fields"
            :key="field.key"
            class="tr-entry-field"
            :class="{ 'tr-entry-field--wide': wideFields.includes(field.key) }"
          >
            <span class="tr-field-label">{{ field.label }}</span>
            <textarea
              v-if="field.multiline"
              v-model="entry[field.key]"
              class="tr-area tr-area--entry"
              rows="4"
            ></textarea>
            <input v-else v-model="entry[field.key]" type="text" class="tr-input" />
          </label>
        </div>
      </div>

      <p v-if="!entries.length" class="tr-empty">No entries yet.</p>

      <div class="tr-actions">
        <button type="button" class="tr-btn tr-btn--add" @click="addEntry">
          + Add {{ entryLabel.toLowerCase() }}
        </button>
        <span v-if="optional" class="tr-hint tr-hint--inline">
          Optional - this section may stay empty.
        </span>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { referenceLine } from '@/utils/terminalReport'

/**
 * Repeatable section editor of the Terminal Report: the lists of the preliminary pages, the
 * literature cited, the appendices and the subsections of a body section all use it.
 *
 * The proponent adds, edits and removes entries; reviewers and the document viewer render
 * the same entries read-only as the printed report (a table for the lists, a hanging-indent
 * paragraph per reference). An entry the proponent leaves completely empty is not part of
 * the report.
 */
const entries = defineModel({ type: Array, default: () => [] })

const props = defineProps({
  /** Columns of one entry: [{ key, label, multiline }]. */
  fields: { type: Array, required: true },
  /** Name of one entry, e.g. "Table", "Reference", "Subsection". */
  entryLabel: { type: String, default: 'Entry' },
  /** Short instruction shown above the entries while editing. */
  hint: { type: String, default: '' },
  /** Marks the section as optional (it may stay empty). */
  optional: { type: Boolean, default: false },
  /** Fields that take the full row in the editor grid. */
  wideFields: { type: Array, default: () => [] },
  /** "table" for the lists, "references" for the literature cited section. */
  display: { type: String, default: 'table' },
  readonly: { type: Boolean, default: false },
})

/** Only the entries that actually carry content appear in the document. */
const filled = computed(() =>
  (entries.value || []).filter((entry) =>
    props.fields.some((field) => String(entry?.[field.key] ?? '').trim() !== '')
  )
)

function addEntry() {
  const row = {}
  for (const field of props.fields) row[field.key] = ''
  entries.value = [...(entries.value || []), row]
}

function removeEntry(index) {
  entries.value = (entries.value || []).filter((_, i) => i !== index)
}
</script>
