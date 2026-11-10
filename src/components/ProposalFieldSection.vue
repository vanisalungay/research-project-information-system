<template>
  <div class="pf-section" :id="`pf-section-${section.key}`">
    <h3>({{ Number(section.key) }}) {{ section.label }}</h3>

    <!-- Grid layout (Project Profile) -->
    <div v-if="section.layout === 'grid'" class="grid-2">
      <div v-for="field in fieldsOf(section)" :key="field.key" :class="{ 'full-width': field.full }">
        <span class="pf-label">{{ field.label }}</span>
        <p class="pf-value">{{ displayValue(field) || 'N/A' }}</p>
        <FieldRemarkControl
          v-bind="controlProps(field.key, field.label)"
          :remarks="remarksFor(field.key)"
          @saved="onSaved"
          @removed="onRemoved(field.key, $event)"
        />
      </div>
    </div>

    <!-- Stack layout (most narrative sections) -->
    <template v-else-if="section.layout === 'stack'">
      <div v-for="field in fieldsOf(section)" :key="field.key" class="pf-field">
        <h4 class="pf-subhead">{{ field.label }}</h4>
        <div v-if="field.kind === 'file'" class="pf-file">
          <template v-if="displayValue(field)">
            <span>📄 {{ displayValue(field) }}</span>
            <button type="button" class="btn-download-sm" @click="$emit('download', downloadTarget(field))">
              DOWNLOAD
            </button>
          </template>
          <span v-else class="pf-empty">No file uploaded.</span>
        </div>
        <!-- Section 19: the interactive GAD Checklist 2, rendered read-only. It is data,
             not a file, so there is no "No file uploaded." state for it. -->
        <div v-else-if="field.kind === 'gadChecklist'" class="pf-checklist">
          <GadChecklist :model-value="gadValue" readonly />
        </div>
        <!-- A document that may be attached to an older proposal for a section that is
             now text/structured data. It is shown (with DOWNLOAD) only when it exists;
             a missing optional document is never reported as an error. -->
        <div v-else-if="field.kind === 'optionalFile'" class="pf-file">
          <template v-if="displayValue(field)">
            <span>📄 {{ displayValue(field) }}</span>
            <button type="button" class="btn-download-sm" @click="$emit('download', downloadTarget(field))">
              DOWNLOAD
            </button>
          </template>
        </div>
        <p v-else class="pf-value pf-long">{{ displayValue(field) || 'Not provided' }}</p>
        <FieldRemarkControl
          v-bind="controlProps(field.key, field.label)"
          :remarks="remarksFor(field.key)"
          @saved="onSaved"
          @removed="onRemoved(field.key, $event)"
        />
      </div>
    </template>

    <!-- Table layout (repeated entries: sites, logical framework, personnel) -->
    <template v-else-if="section.layout === 'table'">
      <table v-if="collectionState.rows.length" class="proposal-table">
        <thead>
          <tr>
            <th class="pf-index-col">#</th>
            <th v-for="column in section.collection.columns" :key="column.key">{{ column.label }}</th>
            <th class="pf-entry-col">Entry Remark</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in collectionState.rows" :key="row.id ?? i">
            <td class="pf-index-col">{{ i + 1 }}</td>
            <td v-for="column in section.collection.columns" :key="column.key">
              <span class="pf-cell-value">{{ cellText(row, column) }}</span>
              <FieldRemarkControl
                v-bind="cellControlProps(section.collection, row, i, column.key)"
                :remarks="collectionState.cells[i][column.key] || []"
                compact
                @saved="onSaved"
                @removed="onRemoved(cellKeyFor(section.collection, i, column.key), $event)"
              />
            </td>
            <td class="pf-entry-col">
              <FieldRemarkControl
                v-bind="entryControlProps(section.collection, row, i)"
                :remarks="collectionState.entries[i] || []"
                compact
                @saved="onSaved"
                @removed="onRemoved(entryKeyFor(section.collection, i), $event)"
              />
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="pf-empty">No entries provided.</p>
    </template>

    <!-- Keyed agenda entries (Priority Agenda, section 05): the registry's default
         agendas plus every agenda the proposal itself stores, so an agenda that is no
         longer offered still shows its value and its own remark control. -->
    <template v-else-if="section.layout === 'agenda'">
      <div v-for="field in agendaFields" :key="field.key" class="pf-field">
        <h4 class="pf-subhead">{{ field.label }}</h4>
        <p class="pf-value">{{ displayValue(field) || 'Not provided' }}</p>
        <FieldRemarkControl
          v-bind="controlProps(field.key, field.label)"
          :remarks="remarksFor(field.key)"
          @saved="onSaved"
          @removed="onRemoved(field.key, $event)"
        />
      </div>
    </template>

    <!-- SDG checklist -->
    <template v-else-if="section.layout === 'sdg'">
      <SdgChecklist :value="proposal.sdg" />
      <FieldRemarkControl
        v-for="field in fieldsOf(section)"
        :key="field.key"
        v-bind="controlProps(field.key, field.label)"
        :remarks="remarksFor(field.key)"
        @saved="onSaved"
        @removed="onRemoved(field.key, $event)"
      />
    </template>

    <!-- Informational section -->
    <p v-else class="pf-value pf-long">
      Please refer to the attached documents for additional necessary documents.
    </p>

    <!-- Repeated collection attached to a non-table section (limitations, budget) -->
    <template v-if="section.collection && section.layout !== 'table'">
      <h4 class="pf-subhead">{{ section.collection.entryLabel }}s</h4>
      <table v-if="collectionState.rows.length" class="proposal-table">
        <thead>
          <tr>
            <th class="pf-index-col">#</th>
            <th v-for="column in section.collection.columns" :key="column.key">{{ column.label }}</th>
            <th class="pf-entry-col">Entry Remark</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, i) in collectionState.rows" :key="row.id ?? i">
            <td class="pf-index-col">{{ i + 1 }}</td>
            <td v-for="column in section.collection.columns" :key="column.key">
              <span class="pf-cell-value">{{ cellText(row, column) }}</span>
              <FieldRemarkControl
                v-bind="cellControlProps(section.collection, row, i, column.key)"
                :remarks="collectionState.cells[i][column.key] || []"
                compact
                @saved="onSaved"
                @removed="onRemoved(cellKeyFor(section.collection, i, column.key), $event)"
              />
            </td>
            <td class="pf-entry-col">
              <FieldRemarkControl
                v-bind="entryControlProps(section.collection, row, i)"
                :remarks="collectionState.entries[i] || []"
                compact
                @saved="onSaved"
                @removed="onRemoved(entryKeyFor(section.collection, i), $event)"
              />
            </td>
          </tr>
        </tbody>
      </table>
      <p v-else class="pf-empty">No entries provided.</p>
    </template>

    <!-- Remarks that can no longer be matched to a current entry. Never dropped. -->
    <div v-if="collectionState.unmatched.length" class="pf-unmatched">
      <p class="pf-unmatched-title">Remarks that no longer match a current entry</p>
      <p class="pf-unmatched-note">
        These were recorded in this round, but the entry they referred to was removed, renamed or
        became ambiguous. They are kept here so nothing is lost.
      </p>
      <div v-for="item in collectionState.unmatched" :key="item.id" class="pf-unmatched-item">
        <span class="pf-unmatched-label">{{ item.fieldLabel || item.fieldKey }}</span>
        <p class="pf-unmatched-text">{{ item.remark }}</p>
        <span class="pf-unmatched-meta">
          {{ item.reviewerRole }}
          <template v-if="item.reviewerName"> · {{ item.reviewerName }}</template>
          <template v-if="item.status === 'ambiguous'"> · needs manual confirmation</template>
        </span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import SdgChecklist from '@/components/SdgChecklist.vue'
import GadChecklist from '@/components/GadChecklist.vue'
import FieldRemarkControl from '@/components/FieldRemarkControl.vue'
import { gadChecklistFromSaved } from '@/utils/gadChecklist'
import {
  agendaFieldsFor,
  buildEntrySnapshot,
  cellKey as cellKeyOf,
  collectionRows,
  entryDisplayLabel,
  entryKey as entryKeyOf,
  fieldLabel as labelOf,
  fieldValue,
  resolveRemarkEntry,
} from '@/utils/proposalFields'

const props = defineProps({
  /** A section object from PROPOSAL_FIELD_SECTIONS. */
  section: { type: Object, required: true },
  /** The proposal returned by GET /api/proposals/{id}. */
  proposal: { type: Object, required: true },
  proposalId: { type: [Number, String], required: true },
  /** { fieldKey: [remark, ...] } for the active round. */
  remarkIndex: { type: Object, default: () => ({}) },
  /** Reviewer pages: render the add/edit/remove controls. */
  editable: { type: Boolean, default: false },
})

const emit = defineEmits(['remark-saved', 'remark-removed', 'download'])

// Local overlay so a saved/removed remark shows immediately without a full refetch.
const overrides = ref({})
const removedIds = ref([])

const index = computed(() => ({ ...props.remarkIndex, ...overrides.value }))

/** Section 05: the registry's default agendas plus the proposal's own agenda rows. */
const agendaFields = computed(() => agendaFieldsFor(props.proposal, props.section))

/**
 * Section 19: the stored checklist in the shape the shared component reads. A proposal
 * saved before the checklist existed has no value, which renders as an unanswered
 * checklist rather than an error.
 */
const gadValue = computed(() => gadChecklistFromSaved(props.proposal?.gadChecklist))

/**
 * The registry's fields for a section, minus the ones that do not apply to this proposal.
 * A field may declare `when(proposal)`; section 21 uses it to show the single-text Risk
 * Management Plan only while the proposal carries no structured risk entries, so a
 * reviewer never sees the old plan and the new table side by side.
 */
const fieldsOf = (section) =>
  (section.fields || []).filter(
    (field) => typeof field.when !== 'function' || field.when(props.proposal)
  )

const remarksFor = (fieldKey) => {
  const items = index.value[fieldKey] || []
  return items.filter((item) => !removedIds.value.includes(item.id))
}

const onSaved = (item) => {
  overrides.value = { ...overrides.value, [item.fieldKey]: [item] }
  emit('remark-saved', item)
}

const onRemoved = (fieldKey, id) => {
  removedIds.value = [...removedIds.value, id]
  emit('remark-removed', id)
}

const cellKeyFor = (collection, index, columnKey) => cellKeyOf(collection.prop, index, columnKey)
const entryKeyFor = (collection, index) => entryKeyOf(collection.prop, index)

const displayValue = (field) => {
  const value = fieldValue(props.proposal, field)
  if (field.kind === 'list' && Array.isArray(value)) return value.join(', ')
  return value
}

/**
 * One repeated-table cell as text. A column may declare `format` (the registry's
 * "% Time Devoted" column does), which turns the stored value into what the reviewer
 * should read - so an older record that stored "50%" and a new one that stores "50" are
 * both shown as "50%". Without a formatter the stored value is shown as it is.
 */
const cellText = (row, column) => {
  const value = row ? row[column.key] : null
  if (typeof column.format === 'function') {
    const formatted = column.format(value)
    return formatted === null || formatted === undefined || formatted === '' ? '-' : formatted
  }
  return value || '-'
}

/**
 * What a Download hands to the parent: the stored relative path when the proposal carries
 * one (a document uploaded into a subfolder such as technology-maps/ can only be fetched by
 * that path through the authorized /api/files route), otherwise the display name the row
 * already shows. The label and the on-screen value are unchanged.
 */
const downloadTarget = (field) => {
  const pathKey = String(field.key).replace(/FileName$/, 'FilePath')
  if (pathKey !== field.key) {
    const path = props.proposal?.[pathKey]
    if (path) return path
  }
  return displayValue(field)
}

const controlProps = (fieldKey, fieldLabel) => ({
  proposalId: props.proposalId,
  sectionKey: props.section.key,
  fieldKey,
  fieldLabel,
  entryIndex: null,
  entryId: null,
  entrySnapshot: null,
  readonly: !props.editable,
})

const snapshotFor = (collection, row) => JSON.stringify(buildEntrySnapshot(row, collection))

const cellControlProps = (collection, row, index, columnKey) => {
  const key = cellKeyOf(collection.prop, index, columnKey)
  return {
    proposalId: props.proposalId,
    sectionKey: props.section.key,
    fieldKey: key,
    fieldLabel: labelOf(key, { entryLabel: entryDisplayLabel(row, collection, index) }),
    entryIndex: index,
    entryId: row?.id ?? null,
    entrySnapshot: snapshotFor(collection, row),
    readonly: !props.editable,
  }
}

const entryControlProps = (collection, row, index) => ({
  proposalId: props.proposalId,
  sectionKey: props.section.key,
  fieldKey: entryKeyOf(collection.prop, index),
  fieldLabel: entryDisplayLabel(row, collection, index),
  entryIndex: index,
  entryId: row?.id ?? null,
  entrySnapshot: snapshotFor(collection, row),
  readonly: !props.editable,
})

/**
 * Places every stored remark of this section's repeated collection beside the row it
 * belongs to, using the agreed ladder (entry id -> snapshot -> primary field ->
 * position). Remarks that cannot be matched confidently are surfaced separately and
 * are never attached to a different entry.
 */
const collectionState = computed(() => {
  const collection = props.section.collection
  const empty = { rows: [], cells: [], entries: [], unmatched: [] }
  if (!collection) return empty

  const rows = collectionRows(props.proposal, collection.prop)
  const cells = rows.map(() => ({}))
  const entries = rows.map(() => [])
  const unmatched = []

  for (const [fieldKey, items] of Object.entries(index.value)) {
    if (!fieldKey.startsWith(`${collection.prop}[`)) continue
    const parsed = fieldKey.match(/^[A-Za-z0-9_]+\[(\d+)\](?:\.(.+))?$/)
    if (!parsed) continue
    const columnKey = parsed[2] || null
    for (const item of items) {
      if (removedIds.value.includes(item.id)) continue
      const resolved = resolveRemarkEntry(item, rows, collection)
      if (resolved.status !== 'matched') {
        unmatched.push({ ...item, fieldKey, status: resolved.status })
        continue
      }
      if (columnKey) {
        if (!cells[resolved.index][columnKey]) cells[resolved.index][columnKey] = []
        cells[resolved.index][columnKey].push({ ...item, fieldKey })
      } else {
        entries[resolved.index].push({ ...item, fieldKey })
      }
    }
  }

  return { rows, cells, entries, unmatched }
})
</script>

<style scoped>
.pf-section {
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 18px;
  margin-bottom: 18px;
}

.pf-section:last-child {
  border-bottom: none;
}

.pf-section h3 {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #1f2937;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  /* A remark must never stretch the field beside it, otherwise the neighbouring
     column drifts out of line with the rest of the section. */
  align-items: start;
}

.grid-2 > div {
  min-width: 0;
}

.full-width {
  grid-column: 1 / -1;
}

.pf-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.03em;
}

.pf-value {
  margin: 2px 0 0;
  font-size: 13px;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.pf-long {
  white-space: pre-wrap;
  line-height: 1.5;
}

.pf-field + .pf-field {
  margin-top: 14px;
}

.pf-subhead {
  margin: 0 0 4px;
  font-size: 13px;
  font-weight: 600;
  color: #374151;
}

.pf-file {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #1f2937;
}

.btn-download-sm {
  background: #eef2ff;
  border: 1px solid #c7d2fe;
  color: #3730a3;
  border-radius: 5px;
  font-family: inherit;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 9px;
  cursor: pointer;
}

.pf-empty {
  font-size: 12px;
  color: #9ca3af;
}

.proposal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  margin-top: 8px;
}

.proposal-table th,
.proposal-table td {
  border: 1px solid #e5e7eb;
  padding: 6px 8px;
  text-align: left;
  vertical-align: top;
}

.proposal-table th {
  background: #f8fafc;
  font-size: 11px;
  font-weight: 700;
  color: #475569;
}

.pf-index-col {
  width: 36px;
  text-align: center;
  color: #94a3b8;
}

.pf-entry-col {
  width: 130px;
}

.pf-cell-value {
  display: block;
  color: #1f2937;
  overflow-wrap: anywhere;
}

.pf-unmatched {
  margin-top: 12px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-left: 3px solid #dc2626;
  border-radius: 8px;
  padding: 10px 12px;
}

.pf-unmatched-title {
  margin: 0;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.04em;
  color: #991b1b;
}

.pf-unmatched-note {
  margin: 4px 0 8px;
  font-size: 11px;
  color: #7f1d1d;
  line-height: 1.45;
}

.pf-unmatched-item + .pf-unmatched-item {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #fecaca;
}

.pf-unmatched-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  color: #991b1b;
}

.pf-unmatched-text {
  margin: 2px 0 0;
  font-size: 12px;
  color: #3f3f46;
  white-space: pre-wrap;
}

.pf-unmatched-meta {
  font-size: 10px;
  color: #b91c1c;
}

@media (max-width: 720px) {
  .grid-2 {
    grid-template-columns: 1fr;
  }
}
/* ===== Review design layer =====================================================
   The card / grid design the proposal review and detailed-proposal pages share. These rules
   come last on purpose: they override the older flat styling above for the same selectors, so
   the section markup, its fields and its remark controls did not have to change at all. */

/* Section card with the navy header band. The band bleeds to the card edges through
   negative margins, so no wrapper element was needed. */
.pf-section {
  position: relative;
  padding: 20px;
  margin-bottom: 20px;
  background: #ffffff;
  border: 1px solid #e4e4f2;
  border-bottom: 1px solid #e4e4f2;
  border-radius: var(--radius-xl, 14px);
  box-shadow: 0 6px 18px rgba(38, 35, 66, 0.05);
  overflow: hidden;
}

.pf-section:last-child {
  border-bottom: 1px solid #e4e4f2;
  margin-bottom: 0;
}

.pf-section h3 {
  margin: -20px -20px 18px;
  padding: 14px 20px;
  background: linear-gradient(100deg, #262342 0%, #2f2b63 100%);
  color: #ffffff;
  font-size: var(--fs-base, 1rem);
  font-weight: 700;
  letter-spacing: -0.005em;
}

/* Two columns where the section asks for one, with a divider under every field so long
   values stay readable and each field is separate from the next. A remark still never
   stretches the field beside it. */
.grid-2 {
  gap: 0 24px;
  align-items: start;
}

.grid-2 > div {
  padding: 14px 0;
  border-bottom: 1px dashed #e8e8f5;
}

.grid-2 > div:last-child {
  border-bottom: none;
}

.pf-label {
  margin-bottom: 4px;
  font-size: var(--fs-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.07em;
  color: #6b6b8c;
}

.pf-value {
  margin: 0;
  font-size: var(--fs-sm, 0.875rem);
  font-weight: 600;
  line-height: 1.5;
  color: #1c1a3c;
}

/* Narrative sections read better lighter, with a roomier line height. */
.pf-long {
  line-height: 1.6;
  font-weight: 400;
  color: #2b2b45;
}

.pf-field {
  padding: 14px 0;
  border-bottom: 1px dashed #e8e8f5;
}

.pf-field:last-child {
  border-bottom: none;
}

.pf-field + .pf-field {
  margin-top: 0;
}

.pf-subhead {
  margin: 0 0 4px;
  font-size: var(--fs-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.07em;
  color: #6b6b8c;
}

.pf-file {
  font-size: var(--fs-sm, 0.875rem);
  color: #1c1a3c;
}

.btn-download-sm {
  border-radius: var(--radius-sm, 6px);
  font-weight: 700;
  padding: 4px 10px;
}

.btn-download-sm:hover {
  background: #e0e7ff;
}

.pf-empty {
  margin: 0;
  font-size: var(--fs-sm, 0.875rem);
  color: #6b6b8c;
}

/* Repeated entries: a light band with dark text, readable whatever else is on the page. */
.proposal-table {
  font-size: var(--fs-sm, 0.875rem);
  margin-top: 10px;
}

.proposal-table th,
.proposal-table td {
  border: 1px solid #e8e8f5;
  padding: 9px 10px;
}

.proposal-table th {
  background: #f5f5fc;
  font-size: var(--fs-xs, 0.75rem);
  font-weight: 700;
  letter-spacing: 0.05em;
  color: #4b4b6b;
}

/* One column on a phone, with the card padding tightened to match, so no field is squeezed
   and nothing overflows horizontally. */
@media (max-width: 640px) {
  .pf-section {
    padding: 16px;
  }

  .pf-section h3 {
    margin: -16px -16px 14px;
    padding: 12px 16px;
  }

  .grid-2 {
    grid-template-columns: minmax(0, 1fr);
    gap: 0;
  }
}

</style>