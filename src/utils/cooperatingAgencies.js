/**
 * cooperatingAgencies.js
 * ---------------------------------------------------------------------------
 * Rules of the "Cooperating Agencies" section (section 02) of the Create
 * Proposal form.
 *
 * The checklist is not hardcoded: it is the shared directory the backend owns
 * (GET /api/cooperating-agencies), which an administrator maintains for the whole
 * university. The proponent interface is read-only over that directory - it can
 * order the list, keep a selection and turn that selection into the legacy
 * "Cooperating Agencies" text, but it can never create an agency
 * (POST /api/cooperating-agencies is restricted to the RPS roles in
 * SecurityConfig), so no helper here adds or renames a directory row.
 */

/** The name of an agency as it would be compared: trimmed, single-spaced. */
export const normalizeAgencyName = (value) =>
  String(value ?? '')
    .trim()
    .replace(/\s+/g, ' ')

/** Case-insensitive comparison key of an agency name. */
export const agencyNameKey = (value) => normalizeAgencyName(value).toLowerCase()

/** The directory in the order the checklist shows it (lower-cased name, then id). */
export const sortAgenciesByName = (agencies) =>
  [...(Array.isArray(agencies) ? agencies : [])].sort((left, right) => {
    const leftKey = agencyNameKey(left?.name)
    const rightKey = agencyNameKey(right?.name)
    if (leftKey === rightKey) return Number(left?.id ?? 0) - Number(right?.id ?? 0)
    return leftKey < rightKey ? -1 : 1
  })

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
