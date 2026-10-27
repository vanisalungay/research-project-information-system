/**
 * Proposal announcement status display helpers.
 *
 * The database stores only three values for an announcement's status
 * (DRAFT / ACTIVE / CLOSED). Those stored values — and the API contract — are
 * kept unchanged; only the label shown in the UI is derived from the stored
 * status plus the announcement's date window:
 *
 *   DRAFT                           -> Draft      (still being prepared)
 *   ACTIVE, start date in the future -> Scheduled  (published, not started yet)
 *   ACTIVE, today inside the range   -> Open       (accepting submissions)
 *   ACTIVE, end date in the past     -> Closed     (window has passed)
 *   CLOSED                           -> Closed
 */

/**
 * The label shown for a Call for Proposal status, from either form the app uses: the
 * uppercase code the API sends on a notification (OPEN / CLOSED / SCHEDULED / DRAFT) or
 * the label {@link announcementStatus} returns (Open / Closed / Scheduled / Draft).
 * A status without a known label has no badge.
 */
export const ANNOUNCEMENT_STATUS_LABELS = {
  DRAFT: 'Draft',
  SCHEDULED: 'Scheduled',
  OPEN: 'Open',
  CLOSED: 'Closed',
}

/**
 * The colour tone of a Call for Proposal status, used by the status badges:
 * open -> green, closed -> red, scheduled -> blue, draft -> neutral. The label is always
 * rendered next to the colour, so a status never depends on colour alone.
 */
export const ANNOUNCEMENT_STATUS_TONES = {
  DRAFT: 'draft',
  SCHEDULED: 'scheduled',
  OPEN: 'open',
  CLOSED: 'closed',
}

/** The status code both forms above share, or '' for anything unusable. */
function statusKey(value) {
  return String(value ?? '')
    .trim()
    .toUpperCase()
}

/** The readable label of a status, or '' when it is not one of the known statuses. */
export function announcementStatusLabel(value) {
  return ANNOUNCEMENT_STATUS_LABELS[statusKey(value)] || ''
}

/** The badge colour tone of a status, or '' when it is not one of the known statuses. */
export function announcementStatusTone(value) {
  return ANNOUNCEMENT_STATUS_TONES[statusKey(value)] || ''
}

/** Local today as YYYY-MM-DD (matches the backend LocalDate serialization). */
export function localTodayISO() {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/**
 * Human-readable announcement status: Draft, Scheduled, Open or Closed.
 * {@code today} is injectable for testing and defaults to the local date.
 */
export function announcementStatus(announcement, today = localTodayISO()) {
  if (!announcement) return ''
  const status = String(announcement.status || '').toUpperCase()
  if (status === 'DRAFT') return 'Draft'
  if (status === 'CLOSED') return 'Closed'
  if (status === 'ACTIVE') {
    if (announcement.startDate && announcement.startDate > today) return 'Scheduled'
    if (announcement.endDate && announcement.endDate < today) return 'Closed'
    return 'Open'
  }
  return announcement.status || ''
}
