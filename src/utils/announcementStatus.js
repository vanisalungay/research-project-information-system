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
