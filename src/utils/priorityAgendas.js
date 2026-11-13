/**
 * priorityAgendas.js
 * ---------------------------------------------------------------------------
 * Pure helpers behind section 05 (Priority Agenda) of the Create Proposal form and
 * the admin management page.
 *
 * The framework name, the coverage period and the list of agendas are master data
 * read from the API (`GET /api/priority-agendas/active`). Nothing in this module is
 * hard-coded, so a framework an administrator configures - "MSUN RIDE 2029-2032",
 * for example - is reflected by the form without any source change or redeployment.
 *
 * Keeping the rules here (instead of inside the views) makes them verifiable
 * without a browser - see priorityAgendas.test.js.
 */

/** The heading shown beside "Priority Agenda", or '' when nothing is configured. */
export function frameworkLabel(active) {
  const name = active && typeof active.frameworkName === 'string' ? active.frameworkName.trim() : ''
  if (!name) return ''
  const start = active.coverageStartYear
  const end = active.coverageEndYear
  if (start === null || start === undefined || end === null || end === undefined) {
    return `(based on ${name})`
  }
  return `(based on ${name} ${start}\u2013${end})`
}

/** One active agenda item as `{ key, label, subLabel, description }`. */
function toProgram(item) {
  return {
    key: String(item?.agendaKey ?? ''),
    label: item?.name || String(item?.agendaKey ?? ''),
    subLabel: item?.subLabel || '',
    description: item?.description || '',
  }
}

/**
 * The agenda programs of the active configuration, in display order, with blank
 * keys dropped. The order is the one the API returns (displayOrder), so an
 * administrator controls how the checkboxes are listed.
 */
export function agendaPrograms(items) {
  if (!Array.isArray(items)) return []
  return items
    .filter((item) => item && String(item.agendaKey ?? '').trim() !== '')
    .map(toProgram)
}

/**
 * The form state of a new submission: one entry per active agenda, nothing
 * selected. Returns `{ [agendaKey]: { selected, value } }`.
 */
export function buildAgendaState(items) {
  const state = {}
  for (const program of agendaPrograms(items)) {
    state[program.key] = { selected: false, value: '' }
  }
  return state
}

/** The `{ agendaKey: { selected, value } }` shape of a proposal's saved rows. */
function savedRowsToMap(savedRows) {
  const map = {}
  if (Array.isArray(savedRows)) {
    for (const row of savedRows) {
      const key = row?.agendaKey ?? row?.agenda
      if (key === null || key === undefined || String(key).trim() === '') continue
      map[String(key)] = { selected: Boolean(row.selected), value: row.value || '' }
    }
  } else if (savedRows && typeof savedRows === 'object') {
    for (const [key, value] of Object.entries(savedRows)) {
      map[key] = { selected: Boolean(value?.selected), value: value?.value || '' }
    }
  }
  return map
}

/**
 * The form state for an existing proposal: every active agenda, plus any agenda the
 * proposal already stores even when its framework has since been deactivated. The
 * historical entries are appended after the active ones, so a revision can always
 * show - and resubmit - exactly what was selected before, and nothing is silently
 * dropped when an administrator changes the framework.
 */
export function mergeAgendaState(items, savedRows) {
  const saved = savedRowsToMap(savedRows)
  const state = {}
  const usedKeys = new Set()

  for (const program of agendaPrograms(items)) {
    usedKeys.add(program.key)
    const stored = saved[program.key]
    state[program.key] = { selected: Boolean(stored?.selected), value: stored?.value || '' }
  }

  for (const [key, value] of Object.entries(saved)) {
    if (!usedKeys.has(key)) {
      state[key] = { selected: Boolean(value.selected), value: value.value || '' }
    }
  }
  return state
}

/**
 * The labels of the agendas a proposal already selected, resolved from a lookup map
 * (`agendaKey -> { name, subLabel }`). Falls back to the raw key, so a printout
 * never shows an empty label for a historical agenda.
 */
export function resolveAgendaLabel(labelsByKey, agendaKey) {
  const entry = labelsByKey && labelsByKey[agendaKey]
  const name = entry?.name || agendaKey || 'Priority Agenda'
  const subLabel = entry?.subLabel || ''
  return subLabel ? `${name} (${subLabel})` : name
}

/** Builds `{ agendaKey: item }` from a lookup response array. */
export function agendaLabelIndex(lookupItems) {
  const index = {}
  if (Array.isArray(lookupItems)) {
    for (const item of lookupItems) {
      if (item && item.agendaKey) index[item.agendaKey] = item
    }
  }
  return index
}

/**
 * The `priorityAgenda` payload of a submission: `{ key: { selected, value } }`,
 * exactly the shape `ProposalRequest.priorityAgenda` expects. Every rendered agenda
 * is sent (selected or not), so unchecking one stores the explicit "not selected"
 * instead of leaving a stale row behind.
 */
export function agendaPayload(state) {
  const payload = {}
  for (const [key, value] of Object.entries(state || {})) {
    payload[key] = { selected: Boolean(value?.selected), value: value?.value || '' }
  }
  return payload
}

/** The selected agendas, in state order, as `[{ key, value }]`. */
export function selectedAgendas(state) {
  return Object.entries(state || {})
    .filter(([, value]) => Boolean(value?.selected))
    .map(([key, value]) => ({ key, value: value?.value || '' }))
}
