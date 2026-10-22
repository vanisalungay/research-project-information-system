/**
 * announcementOptions.js
 * Central definition of the extra information a Proposal Announcement carries:
 * the MSUN RIIDE 2025-2028 priority-agenda programs and the standard
 * announcement reminders.
 *
 * The program keys ('dagat', 'punla', 'kalikasan', 'negosyo', 'tanglaw') are the
 * same stable identifiers the proponent proposal form uses for its Priority
 * Agenda section (ProponentSubmitProp.vue) and the ones persisted with every
 * announcement - never array positions and never the display labels. The backend
 * mirrors them in ProposalAnnouncementCatalog, which is what validates and
 * stores a selection.
 */

export const RIIDE_PROGRAMS = [
  {
    key: 'dagat',
    label: 'DAGAT Program',
    description: 'Development of Aquatic Governance and Advanced Technologies',
  },
  {
    key: 'punla',
    label: 'PUNLA Program',
    description: 'Progressive Upliftment for Nutrition, Livelihood, and Agriculture',
  },
  {
    key: 'kalikasan',
    label: 'KALIKASAN Program',
    description:
      'Knowledge and Actions for Lasting Impact: Keeping Atmospheres, Seas, and Nature Sustainable',
  },
  {
    key: 'negosyo',
    label: 'NEGOSYO Program',
    description:
      'Nurturing Entrepreneurship and Growth through Outstanding Strategic Yielding Opportunities',
  },
  {
    key: 'tanglaw',
    label: 'TANGLAW Program',
    description:
      'Transforming Academia through Nurturing Growth, Leadership, Advancement, and Wisdom',
  },
]

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

/** Stable program identifiers, in display order. */
export const PROGRAM_KEYS = RIIDE_PROGRAMS.map((program) => program.key)

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

/** Normalizes a stored RIIDE priority-agenda selection. */
export function normalizeProgramKeys(value) {
  return normalizeKeys(value, PROGRAM_KEYS)
}

/** Normalizes a stored announcement reminder selection. */
export function normalizeReminderKeys(value) {
  return normalizeKeys(value, REMINDER_KEYS)
}

/** True when every allowed identifier is currently selected. */
export function isAllSelected(selectedKeys, allowedKeys) {
  const allowed = allowedKeys || PROGRAM_KEYS
  const selected = new Set(normalizeKeys(selectedKeys, allowed))
  return allowed.length > 0 && allowed.every((key) => selected.has(key))
}

/**
 * Check All / Uncheck All: returns every allowed identifier, or an empty
 * selection when they are all selected already. The caller derives the button
 * label from {@link isAllSelected} so the label always follows the checkboxes.
 */
export function toggleAllSelection(selectedKeys, allowedKeys) {
  const allowed = allowedKeys || PROGRAM_KEYS
  return isAllSelected(selectedKeys, allowed) ? [] : [...allowed]
}
