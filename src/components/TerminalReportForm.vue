<template>
  <div class="tr-doc">
    <!-- ==================== COVER PAGE ==================== -->
    <TerminalReportSheet v-if="shows('cover')" cover :page-label="pageLabel('cover')">
      <p class="tr-cover-title">{{ model.projectTitle || 'Project Title' }}</p>
      <p v-if="model.projectTeam" class="tr-cover-team">{{ model.projectTeam }}</p>
      <p class="tr-cover-report">Terminal Report</p>
      <p v-if="model.submissionMonthYear" class="tr-cover-month">{{ model.submissionMonthYear }}</p>

      <div v-if="readonly" class="tr-cover-fields">
        <table class="tr-grid">
          <tbody>
            <tr v-for="field in extraCoverFields" :key="field.key">
              <th scope="row">{{ field.label }}</th>
              <td>{{ model[field.key] }}</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-else class="tr-cover-fields">
        <div v-for="field in TERMINAL_COVER_FIELDS" :key="field.key" class="tr-field">
          <span class="tr-field-label">
            {{ field.label }}<span v-if="field.required" class="tr-required">Required</span>
          </span>
          <input
            v-model="model[field.key]"
            type="text"
            class="tr-input"
            :class="{ 'tr-input--error': errors[field.key] }"
          />
          <p v-if="errors[field.key]" class="tr-error">{{ errors[field.key] }}</p>
        </div>
      </div>
    </TerminalReportSheet>

    <!-- ==================== SUMMARY SHEET ==================== -->
    <TerminalReportSheet v-if="shows('summary')" :page-label="pageLabel('summary')">
      <h2 class="tr-front-heading">SUMMARY FOR THE TERMINAL REPORT</h2>

      <TerminalReportEntries
        v-if="!readonly"
        v-model="model.summaryRows"
        :fields="TERMINAL_SUMMARY_ROW_FIELDS"
        entry-label="Item"
        hint="The items the official summary sheet prints. Fill in the details; an item you leave empty is not printed."
        :wide-fields="['detail']"
      />
      <table v-else-if="summaryRows.length" class="tr-grid">
        <thead>
          <tr>
            <th>Project Information</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in summaryRows" :key="index">
            <td>{{ row.label }}</td>
            <td>{{ row.detail }}</td>
          </tr>
        </tbody>
      </table>

      <div v-if="!readonly" class="tr-field">
        <span class="tr-field-label">Caption above the objectives table</span>
        <input
          v-model="model.summaryAccomplishmentsTitle"
          type="text"
          class="tr-input"
          placeholder="e.g. Major accomplishments of the project"
        />
      </div>
      <p v-else-if="model.summaryAccomplishmentsTitle" class="tr-paragraph tr-paragraph--flush">
        {{ model.summaryAccomplishmentsTitle }}
      </p>

      <TerminalReportEntries
        v-if="!readonly"
        v-model="model.summaryObjectives"
        :fields="TERMINAL_OBJECTIVE_FIELDS"
        entry-label="Row"
        hint="One row per objective of the project and what was accomplished."
      />
      <table v-else-if="summaryObjectives.length" class="tr-grid">
        <thead>
          <tr>
            <th v-for="field in TERMINAL_OBJECTIVE_FIELDS" :key="field.key">{{ field.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, index) in summaryObjectives" :key="index">
            <td v-for="field in TERMINAL_OBJECTIVE_FIELDS" :key="field.key">{{ row[field.key] }}</td>
          </tr>
        </tbody>
      </table>

      <template v-if="!readonly">
        <div v-for="field in TERMINAL_FINANCIAL_FIELDS" :key="field.key" class="tr-field">
          <span class="tr-field-label">{{ field.label }}</span>
          <input v-model="model[field.key]" type="text" class="tr-input" />
        </div>
      </template>
      <table v-else-if="financialRows.length" class="tr-grid">
        <thead>
          <tr>
            <th>Financial Information</th>
            <th>Details</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="field in financialRows" :key="field.key">
            <td>{{ field.label }}</td>
            <td>{{ model[field.key] }}</td>
          </tr>
        </tbody>
      </table>
    </TerminalReportSheet>

    <!-- ============ ACKNOWLEDGMENT / ABSTRACT ============ -->
    <TerminalReportSheet v-if="shows('acknowledgment')" :page-label="pageLabel('acknowledgment')">
      <h2 class="tr-front-heading">ACKNOWLEDGMENT</h2>
      <template v-if="readonly">
        <p
          v-for="(paragraph, index) in textParagraphs(model.acknowledgment)"
          :key="index"
          class="tr-paragraph"
        >
          {{ paragraph }}
        </p>
      </template>
      <div v-else class="tr-field">
        <textarea
          v-model="model.acknowledgment"
          class="tr-area"
          rows="10"
          placeholder="Key persons, institutions and other entities that contributed to the research."
        ></textarea>
      </div>
    </TerminalReportSheet>

    <TerminalReportSheet v-if="shows('abstract')" :page-label="pageLabel('abstract')">
      <h2 class="tr-front-heading">ABSTRACT</h2>
      <template v-if="readonly">
        <p
          v-for="(paragraph, index) in textParagraphs(model.abstractText)"
          :key="index"
          class="tr-paragraph"
        >
          {{ paragraph }}
        </p>
        <p v-if="model.keywords" class="tr-paragraph tr-keywords">
          <strong>Keywords:</strong> <em>{{ model.keywords }}</em>
        </p>
      </template>
      <template v-else>
        <div v-for="field in narrativeFields" :key="field.key" class="tr-field">
          <span class="tr-field-label">
            {{ field.label }}<span v-if="field.required" class="tr-required">Required</span>
          </span>
          <p v-if="field.hint" class="tr-hint">{{ field.hint }}</p>
          <textarea
            v-model="model[field.key]"
            class="tr-area"
            :class="{ 'tr-area--error': errors[field.key] }"
            :rows="field.key === 'abstractText' ? 12 : 5"
          ></textarea>
          <p
            v-if="field.maxWords"
            class="tr-counter"
            :class="{ 'tr-counter--over': abstractWords > field.maxWords }"
          >
            {{ abstractWords }} / {{ field.maxWords }} words
            <template v-if="abstractWords > field.maxWords">
              — shorten the abstract before submitting.
            </template>
          </p>
          <p v-if="errors[field.key]" class="tr-error">{{ errors[field.key] }}</p>
        </div>
      </template>
    </TerminalReportSheet>

    <!-- ============ TABLE OF CONTENTS AND THE LISTS ============ -->
    <TerminalReportSheet v-if="shows('contents')" :page-label="pageLabel('contents')">
      <h2 class="tr-front-heading">TABLE OF CONTENTS</h2>
      <TerminalReportEntries
        v-if="!readonly"
        v-model="model.tableOfContents"
        :fields="listOf('tableOfContents').fields"
        :entry-label="listOf('tableOfContents').entryLabel"
        :hint="listOf('tableOfContents').hint"
        :wide-fields="['label']"
      />
      <div v-else class="tr-list-entries">
        <div v-for="(row, index) in listRows('tableOfContents')" :key="index" class="tr-list-entry">
          <span class="tr-list-entry-text">{{ row.label }}</span>
          <span class="tr-list-leader" aria-hidden="true"></span>
          <span class="tr-list-page">{{ row.page }}</span>
        </div>
      </div>
    </TerminalReportSheet>

    <template v-for="list in TERMINAL_LISTS.slice(1)" :key="list.key">
      <TerminalReportSheet v-if="shows(list.key)" :page-label="pageLabel(list.key)">
        <h2 class="tr-front-heading">{{ list.label }}</h2>
        <template v-if="readonly">
          <div v-if="hasPageColumn(list)" class="tr-list-entries">
            <div v-for="(row, index) in listRows(list.key)" :key="index" class="tr-list-entry">
              <span class="tr-list-entry-text">{{ row.label }}</span>
              <span class="tr-list-leader" aria-hidden="true"></span>
              <span class="tr-list-page">{{ row.page }}</span>
            </div>
          </div>
          <TerminalReportEntries
            v-else
            :model-value="model[list.key]"
            :fields="list.fields"
            readonly
          />
        </template>
        <TerminalReportEntries
          v-else
          v-model="model[list.key]"
          :fields="list.fields"
          :entry-label="list.entryLabel"
          :optional="list.optional"
          :wide-fields="[list.fields[0].key]"
        />
      </TerminalReportSheet>
    </template>

    <!-- ============ BODY SECTIONS 1-10, IN DOCUMENT ORDER ============ -->
    <template v-for="(section, index) in TERMINAL_BODY_SECTIONS" :key="section.key">
      <TerminalReportSheet v-if="shows(section.key)" :page-label="pageLabel(section.key)">
        <!-- The first body page repeats the report title, the project team and the
             affiliation, exactly as the official format does. -->
        <template v-if="index === 0">
          <p class="tr-body-title">{{ model.projectTitle }}</p>
          <p v-if="model.projectTeam" class="tr-body-team">{{ model.projectTeam }}</p>
          <p class="tr-body-affiliation">{{ letterhead.affiliation }}</p>
        </template>

        <h2 class="tr-section-heading">{{ section.number }}. {{ section.title }}</h2>

        <!-- Section written as text, with its numbered subsections and tables. -->
        <template v-if="section.kind === 'text'">
          <template v-if="readonly">
            <p
              v-for="(paragraph, paragraphIndex) in textParagraphs(model[section.key].text)"
              :key="paragraphIndex"
              class="tr-paragraph"
            >
              {{ paragraph }}
            </p>
            <template v-for="entry in subsectionNumbers(section)" :key="entry.number">
              <h3 class="tr-subsection-heading">{{ entry.number }} {{ entry.subsection.heading }}</h3>
              <p
                v-for="(paragraph, paragraphIndex) in textParagraphs(entry.subsection.text)"
                :key="paragraphIndex"
                class="tr-paragraph"
              >
                {{ paragraph }}
              </p>
            </template>
            <TerminalReportTables
              :model-value="model[section.key].tables"
              :numbered="tablesOf(section.key)"
              readonly
            />
          </template>

          <template v-else>
            <p class="tr-hint">{{ section.hint }}</p>
            <div class="tr-field">
              <span class="tr-field-label">Section text</span>
              <textarea
                v-model="model[section.key].text"
                class="tr-area"
                :class="{ 'tr-area--error': errors[section.key] }"
                rows="8"
                placeholder="Write the text of this section. Leave it empty when the section is written as subsections below."
              ></textarea>
              <p v-if="errors[section.key]" class="tr-error">{{ errors[section.key] }}</p>
            </div>
            <TerminalReportEntries
              v-model="model[section.key].subsections"
              :fields="TERMINAL_SUBSECTION_FIELDS"
              entry-label="Subsection"
              hint="Subsections are numbered automatically in the report (for example 3.1, 3.2)."
              :wide-fields="['text']"
            />
            <TerminalReportTables
              v-model="model[section.key].tables"
              hint="Tables are numbered automatically in the report (Table 1, Table 2, ...)."
            />
          </template>
        </template>

        <!-- 6. LITERATURE CITED -->
        <template v-else-if="section.kind === 'references'">
          <template v-if="readonly">
            <p v-for="(row, rowIndex) in referenceRows" :key="rowIndex" class="tr-reference">
              {{ referenceLine(row) }}
            </p>
          </template>
          <TerminalReportEntries
            v-else
            v-model="model.literatureCited"
            :fields="TERMINAL_REFERENCE_FIELDS"
            entry-label="Reference"
            :hint="section.hint"
            :wide-fields="['title']"
          />
        </template>

        <!-- 7. APPENDIX -->
        <template v-else>
          <template v-if="readonly">
            <template v-for="entry in appendixNumbers(model)" :key="entry.number">
              <h3 class="tr-subsection-heading">
                Appendix {{ entry.number }}. {{ entry.appendix.title }}
              </h3>
              <p
                v-for="(paragraph, paragraphIndex) in textParagraphs(entry.appendix.text)"
                :key="paragraphIndex"
                class="tr-paragraph"
              >
                {{ paragraph }}
              </p>
            </template>
            <TerminalReportTables
              :model-value="model.appendixTables"
              :numbered="appendixTablesOf()"
              readonly
            />
          </template>
          <template v-else>
            <p class="tr-hint">{{ section.hint }}</p>
            <TerminalReportEntries
              v-model="model.appendices"
              :fields="TERMINAL_APPENDIX_FIELDS"
              entry-label="Appendix"
              :wide-fields="['text']"
            />
            <TerminalReportTables
              v-model="model.appendixTables"
              hint="Tables of the appendix are numbered automatically (Table A1, Table A2, ...)."
            />
          </template>
          <slot name="attachment" />
        </template>
      </TerminalReportSheet>
    </template>

  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
// Terminal Report document styling; the print export inlines this same file, so the
// document on screen and the printed / PDF report stay identical.
import '@/assets/terminalReportDocument.css'
import TerminalReportEntries from '@/components/TerminalReportEntries.vue'
import TerminalReportSheet from '@/components/TerminalReportSheet.vue'
import TerminalReportTables from '@/components/TerminalReportTables.vue'
import {
  TERMINAL_APPENDIX_FIELDS,
  TERMINAL_BODY_KEYS,
  TERMINAL_BODY_SECTIONS,
  TERMINAL_COVER_FIELDS,
  TERMINAL_FINANCIAL_FIELDS,
  TERMINAL_LETTERHEAD,
  TERMINAL_LISTS,
  TERMINAL_NARRATIVE_FIELDS,
  TERMINAL_OBJECTIVE_FIELDS,
  TERMINAL_REFERENCE_FIELDS,
  TERMINAL_SECTIONS,
  TERMINAL_SUBSECTION_FIELDS,
  TERMINAL_SUMMARY_ROW_FIELDS,
  abstractWordCount,
  appendixNumbers,
  appendixTableNumbers,
  bodyTableNumbers,
  normalizeTerminalReport,
  referenceLine,
  sectionHasText,
  sectionSubsectionNumbers,
  summaryRowHasValue,
  textParagraphs,
} from '@/utils/terminalReport'

/**
 * The structured MSUN-Naawan Terminal Report (reporting stage 4).
 *
 * The document follows the official terminal report format: cover page with the
 * institutional letterhead, summary sheet, acknowledgment, abstract with keywords, table of
 * contents and the lists of tables, figures, graphs, appendices and acronyms, the numbered
 * body sections 1-7 and the additional sections 8-10 required by RPIS. Body sections carry
 * numbered subsections and captioned tables, both numbered by the document itself.
 *
 * The proponent completes the report one step at a time; the reviewer, the review step and
 * the submitted document viewer render the same component read-only. The component never
 * persists anything: the data is bound with v-model and is saved / submitted through the
 * existing terminal reporting workflow.
 */
const model = defineModel({ type: Object, default: () => normalizeTerminalReport(null) })

const props = defineProps({
  readonly: { type: Boolean, default: false },
  /**
   * Step of the form being edited (a key of TERMINAL_SECTIONS). Null renders the whole
   * document - used by the review step and by the document viewer.
   */
  section: { type: String, default: null },
  /** Field errors keyed by field name (see terminalReportValidation). */
  errors: { type: Object, default: () => ({}) },
})

const ROMAN = ['i', 'ii', 'iii', 'iv', 'v', 'vi', 'vii', 'viii', 'ix', 'x', 'xi', 'xii']
const FRONT_SHEETS = [
  'summary',
  'acknowledgment',
  'abstract',
  'contents',
  ...TERMINAL_LISTS.slice(1).map((list) => list.key),
]
/** The sheets of the document, in document order. */
const SHEET_KEYS = ['cover', ...FRONT_SHEETS, ...TERMINAL_BODY_KEYS]

const letterhead = TERMINAL_LETTERHEAD
const narrativeFields = TERMINAL_NARRATIVE_FIELDS

function hasText(value) {
  return String(value ?? '').trim() !== ''
}

function hasAnyField(row, fields) {
  return fields.some((field) => hasText(row?.[field.key]))
}

const abstractWords = computed(() => abstractWordCount(model.value?.abstractText))

/** Cover fields beyond the ones the official cover page prints (RPIS requirements). */
const extraCoverFields = computed(() =>
  TERMINAL_COVER_FIELDS.filter(
    (field) =>
      !['projectTitle', 'projectTeam', 'submissionMonthYear'].includes(field.key) &&
      hasText(model.value?.[field.key])
  )
)

const summaryRows = computed(() => (model.value?.summaryRows || []).filter(summaryRowHasValue))

const summaryObjectives = computed(() =>
  (model.value?.summaryObjectives || []).filter((row) => hasAnyField(row, TERMINAL_OBJECTIVE_FIELDS))
)

const financialRows = computed(() =>
  TERMINAL_FINANCIAL_FIELDS.filter((field) => hasText(model.value?.[field.key]))
)

const referenceRows = computed(() =>
  (model.value?.literatureCited || []).filter((row) => hasAnyField(row, TERMINAL_REFERENCE_FIELDS))
)

function listOf(key) {
  return TERMINAL_LISTS.find((list) => list.key === key)
}

/** The filled entries of a list, in the order the proponent entered them. */
function listRows(key) {
  const list = listOf(key)
  if (!list) return []
  return (model.value?.[key] || []).filter((row) => hasAnyField(row, list.fields))
}

function hasPageColumn(list) {
  return list.fields.some((field) => field.key === 'page')
}

function sectionOf(key) {
  return TERMINAL_BODY_SECTIONS.find((section) => section.key === key)
}

/** The numbered subsections of a body section, as the document prints them. */
function subsectionNumbers(section) {
  return sectionSubsectionNumbers(model.value?.[section.key], section.number)
}

/** The numbered tables of a body section, as the document prints them. */
function tablesOf(sectionKey) {
  return bodyTableNumbers(model.value)[sectionKey] || []
}

function appendixTablesOf() {
  return appendixTableNumbers(model.value)
}

/** True when a sheet carries anything at all (read-only document view). */
function sheetHasContent(key) {
  const current = model.value
  if (key === 'cover') return true
  if (key === 'summary') {
    return (
      summaryRows.value.length > 0 ||
      summaryObjectives.value.length > 0 ||
      financialRows.value.length > 0 ||
      hasText(current.summaryAccomplishmentsTitle)
    )
  }
  if (key === 'acknowledgment') return hasText(current.acknowledgment)
  if (key === 'abstract') return hasText(current.abstractText) || hasText(current.keywords)
  if (key === 'contents') return listRows('tableOfContents').length > 0
  if (TERMINAL_LISTS.some((list) => list.key === key)) return listRows(key).length > 0
  if (key === 'literatureCited') return referenceRows.value.length > 0
  if (key === 'appendix') {
    return appendixNumbers(current).length > 0 || appendixTablesOf().length > 0
  }
  return sectionHasText(current[key]) || tablesOf(key).length > 0
}

/**
 * Whether a sheet is part of the current render. While editing, exactly the sheets of the
 * current step are shown (so the form is never one endless page); read-only, only the
 * sheets that carry content are shown, which is what the printed document contains.
 */
function shows(key) {
  if (props.section) {
    const step = TERMINAL_SECTIONS.find((entry) => entry.key === props.section)
    return !!step && step.sheets.includes(key)
  }
  return !props.readonly || sheetHasContent(key)
}

const visibleSheets = computed(() => SHEET_KEYS.filter((key) => shows(key)))

/**
 * The page number printed at the bottom of a sheet: lowercase Roman for the front matter
 * and Arabic for the body, exactly like the official format. The cover page has none.
 */
function pageLabel(key) {
  if (key === 'cover') return ''
  const visible = visibleSheets.value
  const front = FRONT_SHEETS.filter((sheet) => visible.includes(sheet))
  const frontIndex = front.indexOf(key)
  if (frontIndex >= 0) return ROMAN[frontIndex] || String(frontIndex + 1)
  const body = TERMINAL_BODY_KEYS.filter((sheet) => visible.includes(sheet))
  const bodyIndex = body.indexOf(key)
  return bodyIndex >= 0 ? String(bodyIndex + 1) : ''
}

// Keep the bound object structurally valid (every field present, every section an object
// with its own lists) without discarding any entered data.
function normalizeModel() {
  const current = model.value
  if (!current) return
  const shape = normalizeTerminalReport(current)
  for (const [key, value] of Object.entries(shape)) {
    if (Array.isArray(value)) {
      if (!Array.isArray(current[key])) current[key] = value
    } else if (value && typeof value === 'object') {
      if (!current[key] || typeof current[key] !== 'object') current[key] = value
    } else if (typeof current[key] !== 'string') {
      current[key] = value
    }
  }
}

watch(model, normalizeModel, { immediate: true })
</script>
