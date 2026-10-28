/**
 * timestamps.js
 * ---------------------------------------------------------------------------
 * The readable date-and-time format the application shows for a stored timestamp
 * (e.g. "Oct 29, 2026, 4:12 AM"), in one place so the screens that display one - the
 * endorsement workflow and the notifications - cannot drift apart.
 */

/**
 * A stored timestamp as a readable local date and time. An unusable value is returned
 * unchanged (never "Invalid Date") and a missing one as an empty string, so a row never
 * shows a broken timestamp.
 */
export function formatTimestamp(value) {
  const text = String(value ?? '').trim()
  if (!text) return ''
  const date = new Date(text)
  if (Number.isNaN(date.getTime())) return text
  return date.toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}
