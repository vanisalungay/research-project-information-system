<template>
  <div class="pf-section" :id="`pf-section-${section.key}`">
    <h3>({{ Number(section.key) }}) {{ section.label }}</h3>

    <!-- Grid layout (Project Profile) -->
    <div v-if="section.layout === 'grid'" class="grid-2">
      <div v-for="field in section.fields" :key="field.key" :class="{ 'full-width': field.full }">
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
      <div v-for="field in section.fields" :key="field.key" class="pf-field">
        <h4 class="pf-subhead">{{ field.label }}</h4>
        <div v-if="field.kind === 'file'" class="pf-file">
          <template v-if="displayValue(field)">
            <span>📄 {{ displayValue(field) }}</span>
            <button type="button" class="btn-download-sm" @click="$emit('download', displayValue(field))">
              Download
            </button>
          </template>
          <span v-else class="pf-empty">No file uploaded.</span>
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
              <span class="pf-cell-value">{{ row[column.key] || '-' }}</span>
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

    <!-- SDG checklist -->
    <template v-else-if="section.layout === 'sdg'">
      <SdgChecklist :value="proposal.sdg" />
      <FieldRemarkControl
        v-for="field in section.fields"
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
              <span class="pf-cell-value">{{ row[column.key] || '-' }}</span>
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
import FieldRemarkControl from '@/components/FieldRemarkControl.vue'
import {
  buildEntrySnapshot,
  cellKey as cellKeyOf,
  collectionRows,
  entryDisplayLabel,
  entryKey as entryKeyOf,
  fieldLabel as labelOf,
  fieldValue,
  isAgendaSelected,
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
</style>
