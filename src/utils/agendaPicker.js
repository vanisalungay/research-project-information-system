/**
 * agendaPicker.js
 * ---------------------------------------------------------------------------
 * Pure helpers behind the searchable "existing agenda" dropdown of the Add Agenda
 * form (src/views/Admin/PriorityAgendas.vue).
 *
 * An administrator used to retype the name, the prompt and the description of an
 * agenda that already exists in another framework. The dropdown replaces that
 * typing, and its options are built from the agenda records the page already loads
 * from the API (GET /api/priority-agendas/sets) - so nothing is hard-coded and an
 * agenda created later shows up without any source change.
 *
 * Nothing here writes anything: selecting a record only prefills the form, and
 * saving still goes through the existing endpoints and validation.
 */

/** The heading of one framework, e.g. "MSUN RIIDE 2025-2028". */
export function frameworkHeading(set) {
  const name = String(set?.frameworkName ?? '').trim()
  const start = set?.coverageStartYear
  const end = set?.coverageEndYear
  if (!name) return ''
  if (start === null || start === undefined || end === null || end === undefined) return name
  return name + ' ' + start + '\u2013' + end
}

/** Matching text: lower case, no diacritics, single spaces. */
export function normalizeAgendaText(value) {
  return String(value ?? '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim()
}

/**
 * The reusable agenda records the dropdown offers, built from the sets the API
 * returns. Every stored item of every framework is included, so an agenda created
 * for an earlier coverage period can be reused in a new one.
 *
 * The same agenda name is listed once - the framework it was found in first is the
 * one displayed - but every framework it occurs in is remembered, which is what
 * tells the form whether it is already part of the framework being edited.
 */
export function agendaCatalogue(sets) {
  const catalogue = []
  const byName = new Map()

  for (const set of Array.isArray(sets) ? sets : []) {
    const heading = frameworkHeading(set)
    for (const item of Array.isArray(set?.agendas) ? set.agendas : []) {
      const name = String(item?.name ?? '').trim()
      if (!name) continue

      const signature = normalizeAgendaText(name)
      const existing = byName.get(signature)
      if (existing) {
        if (!existing.setIds.includes(set?.id ?? null)) existing.setIds.push(set?.id ?? null)
        existing.reuseCount += 1
        continue
      }

      const record = {
        id: item?.id ?? null,
        agendaKey: String(item?.agendaKey ?? '').trim(),
        name,
        subLabel: String(item?.subLabel ?? '').trim(),
        description: String(item?.description ?? '').trim(),
        setId: set?.id ?? null,
        setIds: [set?.id ?? null],
        frameworkHeading: heading,
        active: Boolean(item?.active),
        reuseCount: 1,
      }
      byName.set(signature, record)
      catalogue.push(record)
    }
  }

  return catalogue
}

/** How well one record answers a keyword; -1 means "no match at all". */
function matchScore(record, keyword) {
  const name = normalizeAgendaText(record.name)
  const key = normalizeAgendaText(record.agendaKey)

  if (name === keyword) return 100
  if (name.startsWith(keyword)) return 90
  if (name.split(' ').some((word) => word.startsWith(keyword))) return 80
  if (key === keyword) return 70
  if (name.includes(keyword)) return 60
  if (key.startsWith(keyword)) return 50
  if (normalizeAgendaText(record.subLabel).includes(keyword)) return 40
  if (normalizeAgendaText(record.description).includes(keyword)) return 30
  return -1
}

/**
 * The existing agendas matching a keyword, best match first: the exact name, then a
 * name that starts with it, then a word inside the name, then the stable key, the
 * prompt and the description. A blank keyword lists the whole catalogue, so the
 * dropdown is already useful before anything is typed.
 *
 * `targetSetId` is the framework the new agenda is being added to; a record that is
 * already part of it comes back with `alreadyInFramework`, so the form shows it
 * instead of offering to create it a second time.
 */
export function searchAgendas(catalogue, query, options) {
  const settings = options || {}
  const targetSetId = settings.targetSetId ?? null
  const limit = Number.isInteger(settings.limit) && settings.limit > 0 ? settings.limit : 0
  const keyword = normalizeAgendaText(query)

  const matches = []
  for (const record of Array.isArray(catalogue) ? catalogue : []) {
    const score = keyword ? matchScore(record, keyword) : 0
    if (score < 0) continue
    matches.push({
      ...record,
      alreadyInFramework: record.setIds.includes(targetSetId),
      score,
    })
  }

  matches.sort((a, b) => {
    if (b.score !== a.score) return b.score - a.score
    // Of two equally good matches, the one that can still be reused comes first.
    if (a.alreadyInFramework !== b.alreadyInFramework) return a.alreadyInFramework ? 1 : -1
    return a.name.localeCompare(b.name)
  })

  return limit > 0 ? matches.slice(0, limit) : matches
}

/** The stable keys already used inside one framework (the backend refuses a repeat). */
export function frameworkKeys(sets, setId) {
  const keys = []
  for (const set of Array.isArray(sets) ? sets : []) {
    if (set?.id !== setId) continue
    for (const item of Array.isArray(set?.agendas) ? set.agendas : []) {
      const key = String(item?.agendaKey ?? '').trim()
      if (key) keys.push(key)
    }
  }
  return keys
}

/**
 * The form values of a selected record. The stable key is carried over: it is what
 * every proposal stores, so reusing the agenda under the same key keeps its
 * historical labels resolvable. When that key is already used in this framework the
 * field is left blank instead, so the backend derives a unique one rather than
 * refusing the save - `keyTaken` lets the form say why.
 */
export function agendaPrefill(record, options) {
  const settings = options || {}
  const taken = new Set((settings.existingKeys ?? []).map(normalizeAgendaText).filter(Boolean))
  const key = String(record?.agendaKey ?? '').trim()
  const keyTaken = key !== '' && taken.has(normalizeAgendaText(key))

  return {
    name: String(record?.name ?? '').trim(),
    subLabel: String(record?.subLabel ?? '').trim(),
    description: String(record?.description ?? '').trim(),
    agendaKey: keyTaken ? '' : key,
    keyTaken,
  }
}

/** The catalogue record whose name is exactly this text, or null. */
export function existingAgendaByName(catalogue, value) {
  const wanted = normalizeAgendaText(value)
  if (!wanted) return null
  for (const record of Array.isArray(catalogue) ? catalogue : []) {
    if (normalizeAgendaText(record?.name) === wanted) return record
  }
  return null
}

/** The label of the "create a new agenda" action for the keyword being typed. */
export function createAgendaLabel(query) {
  const typed = String(query ?? '').trim()
  return typed ? 'Create new agenda \u201c' + typed + '\u201d' : 'Create a new agenda'
}
