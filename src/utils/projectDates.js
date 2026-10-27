/**
 * projectDates.js
 * ---------------------------------------------------------------------------
 * The End Date rule of the Create Proposal form (section 01 - Project Profile).
 *
 * The End Date is never chosen by the proponent: it is calculated by adding the selected
 * number of months to the Start Date.
 *
 *   October 2026   + 12 months -> October 2027
 *   December 2026  + 12 months -> December 2027
 *   October 2026   +  6 months -> April 2027
 *   January 2027   + 18 months -> July 2028
 *
 * The day of the month is preserved; when the target month is shorter, the last valid day of
 * that month is used (31 January + 1 month -> 28 February). The backend enforces the same
 * rule (ProposalService), so a modified API request cannot store another date.
 *
 * Dates are handled as plain calendar dates in ISO form (yyyy-mm-dd) - exactly the format
 * <input type="date"> reads and writes - and are calculated in UTC, so no local time zone
 * can shift the result by a day.
 *
 * Run the rules with: npm test
 */

/** Durations (in months) the proposal form offers. Mirrors ProposalService.SUPPORTED_DURATIONS. */
export const PROJECT_DURATION_OPTIONS = ['12', '24', '36']

/** Shown while the End Date cannot be calculated yet (no duration or no start date). */
export const PROJECT_PERIOD_HINT = 'Please select the project duration and start date first.'

/** Shown under the End Date once it has been calculated by the system. */
export const PROJECT_END_DATE_HINT = 'Calculated automatically from the start date and duration.'

/**
 * An ISO calendar date (yyyy-mm-dd) as a UTC Date, or null when the value is empty or not a
 * real date (2026-02-31 is rejected instead of rolling over into March).
 */
export function parseIsoDate(value) {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(String(value ?? '').trim())
  if (!match) return null
  const year = Number(match[1])
  const month = Number(match[2]) - 1
  const day = Number(match[3])
  const date = new Date(Date.UTC(year, month, day))
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month ||
    date.getUTCDate() !== day
  ) {
    return null
  }
  return date
}

/** The ISO form (yyyy-mm-dd) of a Date, or '' when it is not a usable date. */
export function toIsoDate(date) {
  if (!(date instanceof Date) || Number.isNaN(date.getTime())) return ''
  return date.toISOString().slice(0, 10)
}

/** A date as the form shows it, e.g. "Dec 1, 2026" (the app's existing date format). */
export function formatIsoDate(value) {
  const date = parseIsoDate(value)
  if (!date) return ''
  return date.toLocaleDateString('en-US', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    timeZone: 'UTC',
  })
}

/**
 * Whole months a duration value stands for, or null when it is not a positive number of
 * months. The calculation itself is generic - the form offers 12, 24 and 36 months, and
 * that list is validated by the existing duration rule.
 */
export function durationMonths(duration) {
  const normalized = String(duration ?? '').trim()
  if (!/^\d+$/.test(normalized)) return null
  const months = Number(normalized)
  return months > 0 ? months : null
}

/**
 * The ISO date `months` months after `isoDate`, keeping the day of the month and clamping it
 * to the last day of the target month (31 January + 1 month = 28 February) so the result
 * never rolls into the following month. Matches java.time.LocalDate#plusMonths.
 */
export function addMonthsIso(isoDate, months) {
  const date = parseIsoDate(isoDate)
  const count = durationMonths(months)
  if (!date || !count) return ''
  const target = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth() + count, 1))
  const year = target.getUTCFullYear()
  const month = target.getUTCMonth()
  const lastDayOfMonth = new Date(Date.UTC(year, month + 1, 0)).getUTCDate()
  const day = Math.min(date.getUTCDate(), lastDayOfMonth)
  return toIsoDate(new Date(Date.UTC(year, month, day)))
}

/**
 * The End Date the system calculates for a start date and a duration, in ISO form, or ''
 * while either value is missing (the field stays empty then - nothing is invented).
 */
export function projectEndDate(startDate, duration) {
  return addMonthsIso(startDate, duration)
}

/**
 * The error of an End Date that is not the date the system calculates, or '' when it is
 * correct. An empty End Date is reported by the existing "required" rule, so it is not an
 * error here.
 */
export function projectEndDateError(startDate, duration, endDate) {
  if (String(endDate ?? '').trim() === '') return ''
  const calculated = projectEndDate(startDate, duration)
  const value = parseIsoDate(endDate)
  if (!value) return 'Please enter a valid End Date.'
  if (!calculated) return PROJECT_PERIOD_HINT
  if (toIsoDate(value) !== calculated) {
    return `The End Date is calculated from the project duration and start date: it must be ${formatIsoDate(calculated)}.`
  }
  return ''
}
