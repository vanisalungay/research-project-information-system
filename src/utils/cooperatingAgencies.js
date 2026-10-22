/**
 * cooperatingAgencies.js
 * ---------------------------------------------------------------------------
 * Rules of the "Cooperating Agencies" section (section 02) of the Create
 * Proposal form.
 *
 * The checklist itself is not hardcoded: it comes from the shared directory the
 * backend owns (GET /api/cooperating-agencies), so an agency added by one
 * proponent is offered to every other proponent. What lives here is the
 * client-side half of the same rules the backend enforces for a new agency -
 * the name is trimmed and its inner whitespace collapsed, an empty name is
 * rejected, and a name that already exists (compared case-insensitively) is
 * reported instead of being added a second time.
 */

/** Longest agency name the backend accepts (mirrors the column). */
export const AGENCY_NAME_MAX_LENGTH = 200

/** The name of an agency as it would be stored: trimmed, single-spaced. */
export const normalizeAgencyName = (value) =>
  String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ')

/** Case-insensitive comparison key of an agency name. */
export const agencyNameKey = (value) => normalizeAgencyName(value).toLowerCase()

/** The agency of a loaded list whose name matches (case-insensitively), if any. */
export const findAgencyByName = (agencies, name) => {
  const key = agencyNameKey(name)
  if (!key) return null
  return (
    (Array.isArray(agencies) ? agencies : []).find(
      (agency) => agencyNameKey(agency?.name) === key,
    ) || null
  )
}

/** The directory in the order the checklist shows it (lower-cased name, then id). */
export const sortAgenciesByName = (agencies) =>
  [...(Array.isArray(agencies) ? agencies : [])].sort((left, right) => {
    const leftKey = agencyNameKey(left?.name)
    const rightKey = agencyNameKey(right?.name)
    if (leftKey === rightKey) return Number(left?.id ?? 0) - Number(right?.id ?? 0)
    return leftKey < rightKey ? -1 : 1
  })

/**
 * The directory with an agency added (or refreshed) in its alphabetical place, so
 * a newly added agency appears in the checklist immediately without a reload.
 */
export const mergeAgency = (agencies, agency) => {
  const list = Array.isArray(agencies) ? agencies : []
  if (!agency || agency.id == null) return sortAgenciesByName(list)
  return sortAgenciesByName([
    ...list.filter((entry) => Number(entry?.id) !== Number(agency.id)),
    agency,
  ])
}

/**
 * Client-side validation of the "Add Another Agency" input. Returns '' when the
 * name may be submitted, otherwise the message shown to the proponent. A name
 * that is already in the directory is caught here (case-insensitively) so the
 * common duplicate never leaves the browser; the backend checks it again.
 */
export const validateAgencyName = (value, agencies = []) => {
  const name = normalizeAgencyName(value)
  if (!name) return 'Please enter the name of the cooperating agency.'
  if (name.length > AGENCY_NAME_MAX_LENGTH) {
    return `Agency name must be at most ${AGENCY_NAME_MAX_LENGTH} characters.`
  }
  if (findAgencyByName(agencies, name)) {
    return `${name} is already in the cooperating agencies list.`
  }
  return ''
}

/** The selected ids as numbers, without duplicates and without empty entries. */
export const normalizeSelectedIds = (ids) => {
  const list = Array.isArray(ids) ? ids : []
  return [
    ...new Set(
      list
        .filter((id) => id !== null && id !== undefined && id !== '')
        .map((id) => Number(id))
        .filter((id) => Number.isFinite(id)),
    ),
  ]
}

/** The names of a selection, in the order they are given (used for the legacy text). */
export const joinAgencyNames = (names) =>
  (Array.isArray(names) ? names : [])
    .filter((name) => name != null && String(name).trim() !== '')
    .join(', ')

/**
 * The selected agencies of a proposal, in directory order, as the legacy
 * "Cooperating Agencies" text every existing screen, export and printout reads.
 */
export const describeAgencySelection = (agencies, selectedIds = []) => {
  const selected = new Set(normalizeSelectedIds(selectedIds))
  return (Array.isArray(agencies) ? agencies : [])
    .filter((agency) => selected.has(Number(agency?.id)))
    .map((agency) => agency.name)
    .join(', ')
}
