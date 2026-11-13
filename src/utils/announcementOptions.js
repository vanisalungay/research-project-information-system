/**
 * announcementOptions.js
 * Central definition of the extra information a Proposal Announcement carries:
 * the priority-agenda programs of the currently active Priority Agenda framework
 * (section 05) and the standard announcement reminders.
 *
 * The program keys are NOT hard-coded any more. They are the stable identifiers of
 * the Priority Agenda master data, read from `GET /api/priority-agendas/active`, so a
 * framework an administrator configures - "MSUN RIDE 2029-2032", for example - is
 * selectable on a new call without any source change. The backend mirrors the rule in
 * ProposalAnnouncementCatalog, which is what validates and stores a selection; the
 * keys here are never array positions and never the display labels.
 */

export const ANNOUNCEMENT_REMINDERS = [
  {
    key: 'research-plan',
    text: 'Proposals must include a clear research plan, objectives, expected outcomes, and a detailed budget.',
  },
  {
    key: 'eligible-submitters',
    text: 'All full-time faculty, researchers, and staff of MSU at Naawan are eligible to submit proposals.',
  },
  {
    key: 'collaboration',
    text: 'Collaborative and interdisciplinary projects are highly encouraged.',
  },
]

/** Stable reminder identifiers, in display order. */
export const REMINDER_KEYS = ANNOUNCEMENT_REMINDERS.map((reminder) => reminder.key)

/**
 * Default selection for a brand-new announcement: every standard reminder is
 * selected. Priority agendas start empty - a call may target no specific
 * program.
 */
export function defaultReminderKeys() {
  return [...REMINDER_KEYS]
}

/**
 * Normalizes a stored or edited selection into a de-duplicated list of valid
 * identifiers, in canonical order. Accepts an array (current format) or a
 * comma/semicolon/newline separated string (legacy format), ignores unknown or
 * blank entries, and is case-insensitive.
 */
export function normalizeKeys(value, allowedKeys) {
  const allowed = allowedKeys || []
  const requested = new Set()
  const entries = Array.isArray(value)
    ? value
    : value === null || value === undefined
      ? []
      : String(value).split(/[,;\n\r]+/)
  for (const entry of entries) {
    if (entry === null || entry === undefined) continue
    requested.add(String(entry).trim().toLowerCase())
  }
  return allowed.filter((key) => requested.has(key))
}

/**
 * Normalizes a stored or edited priority-agenda selection against the allowed keys
 * (the active framework's active agenda items). A caller that has not loaded the
 * master data yet passes no keys, which yields an empty selection rather than
 * silently accepting an unknown identifier.
 */
export function normalizeProgramKeys(value, allowedKeys) {
  return normalizeKeys(value, allowedKeys || [])
}

/** Normalizes a stored announcement reminder selection. */
export function normalizeReminderKeys(value) {
  return normalizeKeys(value, REMINDER_KEYS)
}

/** True when every allowed identifier is currently selected. */
export function isAllSelected(selectedKeys, allowedKeys) {
  const allowed = allowedKeys || []
  const selected = new Set(normalizeKeys(selectedKeys, allowed))
  return allowed.length > 0 && allowed.every((key) => selected.has(key))
}

/**
 * Check All / Uncheck All: returns every allowed identifier, or an empty
 * selection when they are all selected already. The caller derives the button
 * label from {@link isAllSelected} so the label always follows the checkboxes.
 */
export function toggleAllSelection(selectedKeys, allowedKeys) {
  const allowed = allowedKeys || []
  return isAllSelected(selectedKeys, allowed) ? [] : [...allowed]
}
