/**
 * proposalFields.js
 * ---------------------------------------------------------------------------
 * Single source of truth for every reviewable proposal field.
 *
 * Field-level remarks are stored against a *stable data path* (never a display
 * label), so the same key identifies the same field on every screen:
 *
 *   scalar field      -> the entity/DTO property name, e.g. `projectTitle`
 *   repeated entry    -> `collection[index]`, e.g. `personnel[1]`
 *   repeated cell     -> `collection[index].column`, e.g. `personnel[1].time`
 *   keyed entry       -> `priorityAgenda.<agendaKey>.value`
 *
 * `key` is the *response* (entity/DTO) property name because remarks are stored
 * against the proposal returned by the API. `form` is the matching property on
 * the proponent's revision form, whose local names differ (e.g. the form calls
 * `college` "department" and `logFrames[0].outcome` "logical_framework[0].outcome_indicator").
 * Without that mapping a remark would be shown beside the wrong input.
 *
 * The backend ProposalFieldCatalog mirrors the accepted key *shape* and a
 * coverage test asserts this registry stays in sync with the entity.
 */

/** Layout hints used by the reviewer renderer (ProposalFieldSection.vue). */
export const FIELD_LAYOUTS = {
  GRID: 'grid',
  STACK: 'stack',
  TABLE: 'table',
  SDG: 'sdg',
  AGENDA: 'agenda',
  NOTE: 'note',
}

export const PROPOSAL_FIELD_SECTIONS = [
  {
    key: '01',
    label: 'Project Profile',
    layout: FIELD_LAYOUTS.GRID,
    fields: [
      { key: 'programTitle', label: 'Program Title', form: 'program_title' },
      { key: 'projectTitle', label: 'Project Title', form: 'project_title' },
      { key: 'projectLeader', label: 'Project Leader', form: 'project_leader' },
      { key: 'projectLeaderSex', label: 'Sex', form: 'project_leader_sex' },
      { key: 'duration', label: 'Duration (months)', form: 'duration' },
      { key: 'startDate', label: 'Start Date', form: 'start_date' },
      { key: 'endDate', label: 'End Date', form: 'end_date' },
      { key: 'college', label: 'Implementing College / Department', form: 'department', full: true },
      { key: 'address', label: 'Address / Contact', form: 'address', full: true },
      // Address and Contact are separate sections on the Create Proposal form;
      // `address` above keeps the combined text every other screen shows.
      { key: 'addressCountry', label: 'Address: Country', form: 'address_details.country' },
      { key: 'addressRegion', label: 'Address: Region', form: 'address_details.region' },
      { key: 'addressProvince', label: 'Address: Province', form: 'address_details.province' },
      { key: 'addressDistrict', label: 'Address: District', form: 'address_details.district' },
      {
        key: 'addressMunicipality',
        label: 'Address: Municipality / City',
        form: 'address_details.municipality',
      },
      { key: 'addressBarangay', label: 'Address: Barangay', form: 'address_details.barangay' },
      { key: 'contactType', label: 'Contact Type', form: 'contact_details.type' },
      { key: 'contactValue', label: 'Contact', form: 'contact_details.value' },
    ],
  },
  {
    key: '02',
    label: 'Cooperating Agencies',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'cooperatingAgencies', label: 'Cooperating Agencies', form: 'cooperating_agencies', kind: 'long' },
    ],
  },
  {
    key: '03',
    label: 'Sites of Implementation',
    layout: FIELD_LAYOUTS.TABLE,
    collection: {
      prop: 'sites',
      form: 'sites',
      entryLabel: 'Site',
      primary: 'municipality',
      columns: [
        { key: 'country', label: 'Country' },
        { key: 'region', label: 'Region' },
        { key: 'province', label: 'Province' },
        { key: 'district', label: 'District' },
        { key: 'municipality', label: 'Municipality' },
        { key: 'barangay', label: 'Barangay' },
      ],
    },
  },
  {
    key: '04',
    label: 'Type of Research',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'researchType', label: 'Type of Research', form: 'research_type' }],
  },
  {
    key: '05',
    label: 'Priority Agenda',
    layout: FIELD_LAYOUTS.AGENDA,
    fields: [
      { key: 'priorityAgenda.dagat.value', label: 'DAGAT Program', form: 'priority_agendas.dagat.value' },
      { key: 'priorityAgenda.punla.value', label: 'PUNLA Program', form: 'priority_agendas.punla.value' },
      { key: 'priorityAgenda.kalikasan.value', label: 'KALIKASAN Program', form: 'priority_agendas.kalikasan.value' },
      { key: 'priorityAgenda.negosyo.value', label: 'NEGOSYO Program', form: 'priority_agendas.negosyo.value' },
      { key: 'priorityAgenda.tanglaw.value', label: 'TANGLAW Program', form: 'priority_agendas.tanglaw.value' },
    ],
  },
  {
    key: '06',
    label: 'Innovation Goals',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'innovationGoals', label: 'Innovation Goals', form: 'innovation_goals', kind: 'long' }],
  },
  {
    key: '07',
    label: 'Sector Relevance',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'sectorRelevance', label: 'Sector Relevance', form: 'sector_relevance', kind: 'long' }],
  },
  {
    key: '08',
    label: 'Sustainable Development Goals (SDG)',
    layout: FIELD_LAYOUTS.SDG,
    fields: [{ key: 'sdg', label: 'Selected SDGs', form: 'sustainable_development_goals', kind: 'list' }],
  },
  {
    key: '09',
    label: 'Executive Summary',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'executiveSummary', label: 'Executive Summary', form: 'executive_summary', kind: 'long' }],
  },
  {
    key: '10',
    label: 'Introduction',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'rationale', label: '10.1 Rationale / Significance', form: 'rationale', kind: 'long' },
      { key: 'framework', label: '10.2 Scientific Basis / Theoretical Framework', form: 'theoretical_framework', kind: 'long' },
      { key: 'objectivesGeneral', label: '10.3 General Objective', form: 'general_objective', kind: 'long' },
      { key: 'objectivesSpecific', label: '10.3 Specific Objectives', form: 'specific_objectives', kind: 'long' },
    ],
  },
  {
    key: '11',
    label: 'Review of Literature',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'review', label: 'Review of Literature', form: 'review_of_literature', kind: 'long' },
      { key: 'reviewFileName', label: 'Review of Literature (document)', form: 'review_of_literature_file', kind: 'file' },
    ],
  },
  {
    key: '12',
    label: 'Methodology',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'methodology', label: 'Methodology', form: 'methodology', kind: 'long' }],
  },
  {
    key: '13',
    label: 'Technology Roadmap',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'roadmapFileName', label: 'Technology Roadmap (document)', form: 'technology_roadmap_file', kind: 'file' },
      { key: 'technologyTrl', label: 'Technology Readiness Level (TRL)', form: 'technology_trl' },
    ],
  },
  {
    key: '14',
    label: 'Expected Outputs (6Ps)',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'outputs', label: 'Expected Outputs (6Ps)', form: 'expected_outputs', kind: 'long' }],
  },
  {
    key: '15',
    label: 'Potential Outcomes',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'outcomes', label: 'Potential Outcomes', form: 'potential_outcomes', kind: 'long' }],
  },
  {
    key: '16',
    label: 'Potential Impacts (2Is)',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'impactEconomic', label: 'a. Economic Impact', form: 'economic_impact', kind: 'long' },
      { key: 'impactSocial', label: 'b. Social / Ethical Impact', form: 'social_ethical_impact', kind: 'long' },
    ],
  },
  {
    key: '17',
    label: 'Target Beneficiaries',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'beneficiaries', label: 'Target Beneficiaries', form: 'target_beneficiaries', kind: 'long' }],
  },
  {
    key: '18',
    label: 'Sustainability Plan',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'sustainability', label: 'Sustainability Plan', form: 'sustainability_plan', kind: 'long' }],
  },
  {
    key: '19',
    label: 'Gender and Development (GAD) Score',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'gadScore', label: 'GAD Score', form: null },
      { key: 'gadFileName', label: 'GAD Score (document)', form: 'gad_score_file', kind: 'file' },
    ],
  },
  {
    key: '20',
    label: 'Limitations of the Project',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'limitationsText', label: 'Limitations of the Project', form: 'limitations', kind: 'long' }],
    collection: {
      prop: 'limitations',
      form: null,
      entryLabel: 'Limitation',
      primary: 'limitation',
      columns: [
        { key: 'limitation', label: 'Limitation' },
        { key: 'remarks', label: 'Remarks' },
      ],
    },
  },
  {
    key: '21',
    label: 'Risk Management Plan',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'risks', label: 'Risk Management Plan', form: 'risks_assumptions', kind: 'long' }],
  },
  {
    key: '22',
    label: 'Logical Framework',
    layout: FIELD_LAYOUTS.TABLE,
    collection: {
      prop: 'logFrames',
      form: 'logical_framework',
      entryLabel: 'Logical framework row',
      primary: 'outcome',
      columns: [
        { key: 'outcome', label: 'Outcome Indicators', form: 'outcome_indicator' },
        { key: 'output', label: 'Output Indicators / Physical Target', form: 'output_indicator' },
      ],
    },
  },
  {
    key: '23',
    label: 'Literature Cited',
    layout: FIELD_LAYOUTS.STACK,
    fields: [{ key: 'referencesText', label: 'Literature Cited', form: 'literature_cited', kind: 'long' }],
  },
  {
    key: '24',
    label: 'Personnel Requirement',
    layout: FIELD_LAYOUTS.TABLE,
    collection: {
      prop: 'personnel',
      form: 'personnel_requirements',
      entryLabel: 'Personnel',
      primary: 'position',
      columns: [
        { key: 'position', label: 'Position', form: 'position' },
        { key: 'timeDevoted', label: '% Time Devoted', form: 'effort' },
        { key: 'responsibilities', label: 'Responsibilities', form: 'responsibilities' },
      ],
    },
  },
  {
    key: '25',
    label: 'Line-Item Budget Requirement',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'beneficiariesFileName', label: 'Line-Item Budget (document)', form: 'line_item_budget_file', kind: 'file' },
    ],
    collection: {
      prop: 'budget',
      form: null,
      entryLabel: 'Budget line',
      primary: 'agency',
      columns: [
        { key: 'agency', label: 'Agency' },
        { key: 'ps', label: 'PS' },
        { key: 'mooe', label: 'MOOE' },
        { key: 'eo', label: 'EO' },
        { key: 'total', label: 'Total' },
      ],
    },
  },
  {
    key: '26',
    label: 'Other Ongoing Projects',
    layout: FIELD_LAYOUTS.STACK,
    fields: [
      { key: 'otherProjectsNumber', label: 'Number of other projects', form: 'other_projects_number' },
    ],
    collection: {
      prop: 'otherProjects',
      form: 'other_projects',
      entryLabel: 'Project',
      primary: 'title',
      columns: [
        { key: 'title', label: 'Title', form: 'project_title' },
        { key: 'agency', label: 'Funding Agency', form: 'funding_agency' },
        { key: 'involvement', label: 'Involvement', form: 'involvement' },
      ],
    },
  },
  {
    key: '27',
    label: 'Other Supporting Documents',
    layout: FIELD_LAYOUTS.NOTE,
    fields: [],
  },
]

/** Lookup of section by its canonical two-digit key. */
export const SECTION_BY_KEY = Object.fromEntries(
  PROPOSAL_FIELD_SECTIONS.map((section) => [section.key, section])
)

/** Flat list of every scalar field entry with its owning section key. */
export const SCALAR_FIELDS = PROPOSAL_FIELD_SECTIONS.flatMap((section) =>
  (section.fields || []).map((field) => ({ ...field, section: section.key }))
)

/** Every repeated collection declared by the registry. */
export const COLLECTIONS = PROPOSAL_FIELD_SECTIONS.filter((section) => section.collection).map(
  (section) => ({ ...section.collection, section: section.key })
)

/** Stable key for one repeated entry (the whole row). */
export function entryKey(prop, index) {
  return `${prop}[${index}]`
}

/** Stable key for one cell of a repeated entry. */
export function cellKey(prop, index, columnKey) {
  return `${prop}[${index}].${columnKey}`
}

/** Section key that owns a field key, e.g. `personnel[1].time` -> `24`. */
export function sectionKeyForField(fieldKey) {
  if (!fieldKey) return null
  const key = String(fieldKey)
  for (const field of SCALAR_FIELDS) {
    if (field.key === key) return field.section
  }
  for (const collection of COLLECTIONS) {
    if (key.startsWith(`${collection.prop}[`)) return collection.section
    if (key.startsWith(`priorityAgenda.`)) return '05'
  }
  if (key === 'sdg') return '08'
  return null
}

/** Human label for a field key, optionally enriched with the entry snapshot. */
export function fieldLabel(fieldKey, options = {}) {
  if (!fieldKey) return ''
  const key = String(fieldKey)
  for (const field of SCALAR_FIELDS) {
    if (field.key === key) return field.label
  }
  const collection = COLLECTIONS.find((entry) => key.startsWith(`${entry.prop}[`))
  if (collection) {
    const match = key.match(/\[(\d+)\](?:\.(.+))?$/)
    const index = match ? Number(match[1]) : null
    const columnKey = match && match[2] ? match[2] : null
    const column = columnKey
      ? (collection.columns || []).find((entry) => entry.key === columnKey)
      : null
    const entryName = options.entryLabel || `${collection.entryLabel} #${(index ?? 0) + 1}`
    return column ? `${entryName} — ${column.label}` : entryName
  }
  return key
}

const normalizeValue = (value) =>
  value === null || value === undefined ? '' : String(value).trim().toLowerCase()

/**
 * Builds the identifying snapshot of one repeated entry: every non-empty column
 * value of that row. Stored with the remark so the entry can be recognised again
 * after the proponent edits/removes/reorders rows (child row ids are regenerated
 * on every save, so they cannot be relied on across a resubmission).
 */
export function buildEntrySnapshot(row, collection) {
  const snapshot = {}
  for (const column of collection.columns || []) {
    const value = row ? row[column.key] : null
    if (value !== null && value !== undefined && String(value).trim() !== '') {
      snapshot[column.key] = String(value).trim()
    }
  }
  return snapshot
}

/** Human-readable description of a repeated entry, e.g. "Personnel #2 — Project Leader". */
export function entryDisplayLabel(row, collection, index) {
  const primary = collection.primary ? row?.[collection.primary] : null
  const base = `${collection.entryLabel} #${(index ?? 0) + 1}`
  return primary ? `${base} — ${String(primary).trim()}` : base
}

/**
 * Resolves where a stored remark belongs against the *current* rows of a repeated
 * collection, using the agreed safety ladder:
 *
 *   1. stable entry id (best-effort: valid until the proponent resubmits, because
 *      the backend regenerates child row ids on every save)
 *   2. entry snapshot + field identity (every recorded column value must match)
 *   3. primary-field match
 *   4. positional index — last resort, flagged with `via: 'index'`
 *
 * Returns `{ status, index, via }`:
 *   'matched'   -> render beside row `index`
 *   'ambiguous' -> several rows match; the remark is NEVER attached silently and
 *                  is surfaced separately for manual review
 *   'unmatched' -> the entry no longer exists; the remark text and its history are
 *                  still preserved and shown in a "no longer found" list
 */
export function resolveRemarkEntry(remark, rows, collection) {
  const list = Array.isArray(rows) ? rows : []
  if (!remark || list.length === 0) {
    return { status: 'unmatched', index: null, via: null }
  }

  // 1. Stable entry id.
  if (remark.entryId !== null && remark.entryId !== undefined) {
    const byId = list.findIndex(
      (row) => row && row.id !== undefined && String(row.id) === String(remark.entryId)
    )
    if (byId >= 0) {
      return { status: 'matched', index: byId, via: 'id' }
    }
  }

  const snapshot = parseSnapshot(remark.entrySnapshot)
  if (snapshot && Object.keys(snapshot).length > 0) {
    // 2. Snapshot + field identity.
    const full = list
      .map((row, index) => ({ row, index }))
      .filter(({ row }) =>
        Object.entries(snapshot).every(
          ([columnKey, value]) => normalizeValue(row?.[columnKey]) === normalizeValue(value)
        )
      )
    if (full.length === 1) {
      return { status: 'matched', index: full[0].index, via: 'snapshot' }
    }
    if (full.length > 1) {
      return { status: 'ambiguous', index: null, via: 'snapshot' }
    }

    // 3. Primary-field match.
    const primaryKey = collection?.primary
    const primaryValue = primaryKey ? snapshot[primaryKey] : null
    if (primaryKey && primaryValue !== null && primaryValue !== undefined && primaryValue !== '') {
      const byPrimary = list
        .map((row, index) => ({ row, index }))
        .filter(({ row }) => normalizeValue(row?.[primaryKey]) === normalizeValue(primaryValue))
      if (byPrimary.length === 1) {
        return { status: 'matched', index: byPrimary[0].index, via: 'primary' }
      }
      if (byPrimary.length > 1) {
        return { status: 'ambiguous', index: null, via: 'primary' }
      }
    }
  }

  // 4. Positional index — last resort only.
  const index = Number(remark.entryIndex)
  if (Number.isInteger(index) && index >= 0 && index < list.length) {
    return { status: 'matched', index, via: 'index' }
  }

  return { status: 'unmatched', index: null, via: null }
}

function parseSnapshot(value) {
  if (!value) return null
  if (typeof value === 'object') return value
  try {
    const parsed = JSON.parse(value)
    return parsed && typeof parsed === 'object' ? parsed : null
  } catch {
    return null
  }
}

/**
 * Resolves the value of one registry field from a proposal response object.
 * Handles the three shapes the API actually returns:
 *   * plain scalar          -> proposal[field.key]
 *   * keyed priority agenda -> proposal.priorityAgendas is an ARRAY of
 *                              { agendaKey, selected, value } (not an object)
 *   * repeated collections  -> proposal[collection.prop] is an array of rows
 */
export function fieldValue(proposal, field) {
  if (!proposal || !field) return ''
  const key = String(field.key)
  const agendaMatch = key.match(/^priorityAgenda\.(.+)\.value$/)
  if (agendaMatch) {
    const agendaKey = agendaMatch[1]
    const rows = Array.isArray(proposal.priorityAgendas) ? proposal.priorityAgendas : []
    const row = rows.find((entry) => entry && entry.agendaKey === agendaKey)
    return row && row.selected ? (row.value ?? '') : ''
  }
  const value = proposal[key]
  if (value === null || value === undefined) return ''
  return value
}

/**
 * The Priority Agenda fields of a proposal (section 05): the registry's default
 * agendas, plus every agenda the proposal itself stores. An agenda an administrator
 * added later - or one that has since been deactivated - therefore still gets its
 * value and its own remark control. The registry keeps the original five keys, so a
 * proposal written before the agendas became configurable stays covered.
 */
export function agendaFieldsFor(proposal, section) {
  const fields = Array.isArray(section?.fields) ? [...section.fields] : []
  const known = new Set(
    fields.map((field) =>
      String(field.key).replace(/^priorityAgenda\./, '').replace(/\.value$/, '')
    )
  )
  const rows = Array.isArray(proposal?.priorityAgendas) ? proposal.priorityAgendas : []
  for (const row of rows) {
    const agendaKey = row?.agendaKey
    if (!agendaKey || known.has(agendaKey)) continue
    known.add(agendaKey)
    fields.push({
      key: `priorityAgenda.${agendaKey}.value`,
      label: row.name || String(agendaKey).toUpperCase(),
      form: `priority_agendas.${agendaKey}.value`,
    })
  }
  return fields
}

/** True when the priority-agenda field is selected (so it should be displayed). */
export function isAgendaSelected(proposal, fieldKey) {
  const agendaMatch = String(fieldKey || '').match(/^priorityAgenda\.(.+)\.value$/)
  if (!agendaMatch) return false
  const rows = Array.isArray(proposal?.priorityAgendas) ? proposal.priorityAgendas : []
  const row = rows.find((entry) => entry && entry.agendaKey === agendaMatch[1])
  return Boolean(row && row.selected)
}

/** The rows of a repeated collection, always as an array. */
export function collectionRows(proposal, prop) {
  const rows = proposal ? proposal[prop] : null
  return Array.isArray(rows) ? rows : []
}

/** The revision round the proponent must currently address (or null). */
export function activeRoundOf(response) {
  const rounds = response?.rounds || []
  if (response?.activeRound != null) {
    return rounds.find((round) => round.round === response.activeRound) || null
  }
  return rounds.find((round) => round.active) || null
}

/**
 * `{ fieldKey: [remark, ...] }` for the ACTIVE revision round of a
 * `GET /api/proposals/{id}/field-remarks` response.
 */
export function buildFieldRemarkIndex(response) {
  const map = {}
  for (const item of activeRoundOf(response)?.remarks || []) {
    const key = item.fieldKey || `__section__${item.sectionKey}`
    if (!map[key]) map[key] = []
    map[key].push(item)
  }
  return map
}

/** Section-level remarks (no fieldKey) of the active round, keyed by section key. */
export function buildSectionRemarkIndex(response) {
  const map = {}
  for (const item of activeRoundOf(response)?.remarks || []) {
    if (item.fieldKey) continue
    const key = String(item.sectionKey || '')
    if (!map[key]) map[key] = []
    map[key].push(item)
  }
  return map
}

/** Every round except the active one, newest first (kept as revision history). */
export function previousRounds(response) {
  return (response?.rounds || []).filter((round) => !round.active)
}

/**
 * Converts the section editor's `{ '12': 'remark' }` map into the API payload
 * shape for the return/forward actions (section-level remarks).
 */
export function toSectionRemarkPayload(map) {
  return Object.entries(map || {})
    .map(([key, value]) => ({
      sectionKey: String(key),
      sectionLabel: fieldLabel(String(key)),
      remark: String(value ?? '').trim(),
    }))
    .filter((entry) => entry.remark !== '')
}
