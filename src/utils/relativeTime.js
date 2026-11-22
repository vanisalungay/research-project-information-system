/**
 * relativeTime.js
 * ---------------------------------------------------------------------------
 * Human-readable times for notifications: "Just now", "5 minutes ago", "Yesterday".
 *
 * The API sends an ISO timestamp with an explicit offset (for example
 * `2026-11-18T14:07:12.345+08:00`). Older rows may still be zone-less; those were
 * written in the institution's zone and are read as Asia/Manila rather than being
 * shifted by the browser's own time zone.
 *
 * Calendar-day comparisons are done in the display time zone, so a notification at
 * 11:30 pm yesterday reads as "Yesterday" regardless of where the browser is.
 */

export const DEFAULT_TIME_ZONE = 'Asia/Manila'

/** How long after a timestamp the page re-renders its relative labels. */
export const RELATIVE_TIME_TICK_MS = 30_000

const MINUTE = 60_000
const HOUR = 60 * MINUTE
const DAY = 24 * HOUR

const HAS_OFFSET = /(z|[+-]\d{2}:?\d{2})$/i

/** Convert an API value to a valid Date, or null. */
export function toNotificationDate(value) {
  if (value === null || value === undefined || value === '') return null
  if (value instanceof Date) {
    return Number.isNaN(value.getTime()) ? null : new Date(value.getTime())
  }
  if (typeof value === 'number') {
    const date = new Date(value)
    return Number.isNaN(date.getTime()) ? null : date
  }
  if (typeof value !== 'string') return null

  let text = value.trim()
  if (!text) return null
  // A zone-less value came from the old serializer, which wrote server-local
  // wall-clock time (pinned to Asia/Manila). Make that explicit instead of letting
  // the browser parse it in its own zone.
  if (/^\d{4}-\d{2}-\d{2}T/.test(text) && !HAS_OFFSET.test(text)) {
    text += '+08:00'
  }
  const date = new Date(text)
  return Number.isNaN(date.getTime()) ? null : date
}

function plural(value, singular, pluralForm = `${singular}s`) {
  return `${value} ${value === 1 ? singular : pluralForm} ago`
}

/** The calendar day number (days since epoch) of an instant in a time zone. */
function calendarDay(date, timeZone) {
  try {
    const parts = new Intl.DateTimeFormat('en-CA', {
      timeZone,
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
    }).formatToParts(date)
    const get = (type) => Number(parts.find((part) => part.type === type)?.value)
    return Math.floor(Date.UTC(get('year'), get('month') - 1, get('day')) / DAY)
  } catch {
    return Math.floor(
      Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()) / DAY,
    )
  }
}

/** The exact date and time, for the hover title. */
export function formatAbsoluteTime(value, options = {}) {
  const date = toNotificationDate(value)
  if (!date) return ''
  const timeZone = options.timeZone || DEFAULT_TIME_ZONE
  try {
    return new Intl.DateTimeFormat('en-US', {
      timeZone,
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit',
    }).format(date)
  } catch {
    return date.toLocaleString('en-US')
  }
}

/**
 * Relative label for an instant. `now` may be a Date, a number or a timestamp string;
 * it is injectable so the tests can pin the clock.
 */
export function formatRelativeTime(value, now = new Date(), options = {}) {
  const date = toNotificationDate(value)
  const nowDate = toNotificationDate(now)
  if (!date || !nowDate) return ''

  const timeZone = options.timeZone || DEFAULT_TIME_ZONE
  const diff = nowDate.getTime() - date.getTime()

  // A clock a few seconds ahead of the server must not show a negative age.
  if (diff < 0) {
    return -diff <= MINUTE ? 'Just now' : formatAbsoluteTime(date, { timeZone })
  }

  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return 'Just now'

  // Minutes keep the compact wording the notification feed uses ("5 mins ago").
  const minutes = Math.floor(diff / MINUTE)
  if (minutes < 60) return minutes === 1 ? '1 min ago' : `${minutes} mins ago`

  const hours = Math.floor(diff / HOUR)
  if (hours < 24) return plural(hours, 'hour')

  const days = calendarDay(nowDate, timeZone) - calendarDay(date, timeZone)
  if (days <= 0) {
    // Same calendar day but at least 24 hours of difference can only happen with a
    // clock/ZONE anomaly; report the elapsed hours rather than inventing a date.
    return plural(hours, 'hour')
  }
  if (days < 7) return plural(days, 'day')
  if (days === 7) return '1 week ago'

  // Older than one week: the exact saved date and time, never an approximate age.
  return formatAbsoluteTime(date, { timeZone })
}
