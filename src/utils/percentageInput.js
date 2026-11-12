/**
 * percentageInput.js
 * ---------------------------------------------------------------------------
 * The one definition of a percentage value in the Quarterly Progress Report:
 * how the proponent types it, how it is displayed, how it is stored and how the
 * Project Workplan's automatic AVERAGE TOTAL is derived.
 *
 * A percentage is stored as its numeric value only ("75", "75.5"); the "%" sign is
 * a display treatment. `sanitizePercentageInput` is applied while typing, so the
 * field only ever holds digits and at most one decimal point, and values are kept
 * inside 0..100. Empty stays empty - it is never turned into 0.
 *
 * Existing reports may contain a legacy value such as "80%": `percentageFormValue`
 * removes the sign so the form shows the number once, and `formatPercentage` never
 * appends a second one. A legacy value that is not numeric is kept verbatim (it is
 * never silently destroyed) and is reported as invalid by `isValidPercentage`, so
 * the proponent has to correct it before the report can be saved or submitted.
 *
 * Run the rules with: npm test
 */

export const PERCENTAGE_MIN = 0
export const PERCENTAGE_MAX = 100

const NUMBER_PATTERN = /^(\d+(?:\.\d*)?|\.\d+)$/

/** True for null, undefined and whitespace-only values. */
function isBlank(value) {
  return value === null || value === undefined || String(value).trim() === ''
}

/**
 * Reads a percentage as a number. A trailing "%" is tolerated, so a value saved by
 * the old form ("80%") still reads as 80. Returns null for anything that is not a
 * number from 0 to 100.
 *
 * @param {string|number} value
 * @returns {number|null}
 */
export function parsePercentage(value) {
  if (isBlank(value)) return null
  const text = String(value).replace(/%/g, '').trim()
  if (!NUMBER_PATTERN.test(text)) return null
  const number = Number(text)
  if (!Number.isFinite(number)) return null
  if (number < PERCENTAGE_MIN || number > PERCENTAGE_MAX) return null
  return number
}

/** True when a value is empty or a valid percentage. */
export function isValidPercentage(value) {
  return isBlank(value) || parsePercentage(value) !== null
}

/** A number without unnecessary trailing zeros ("75.50" -> "75.5", "60.00" -> "60"). */
function formatNumber(number) {
  return String(Number(number.toFixed(2)))
}

/**
 * The value the form state holds. A valid value (including a legacy "80%") becomes
 * its numeric text; a non-numeric legacy value is kept as it is so opening and
 * saving a report never silently discards stored content. Empty stays empty.
 */
export function percentageFormValue(value) {
  if (isBlank(value)) return ''
  const number = parsePercentage(value)
  return number === null ? String(value).trim() : String(number)
}

/**
 * What the proponent's input may contain. Everything that is not a digit or a
 * decimal point is removed (including a manually typed "%"), only one decimal point
 * is kept, and a value above 100 is reduced to 100. The result is never negative.
 */
export function sanitizePercentageInput(value) {
  if (isBlank(value)) return ''
  const digits = String(value).replace(/%/g, '').replace(/[^\d.]/g, '')
  const firstDot = digits.indexOf('.')
  let cleaned = digits
  if (firstDot !== -1) {
    cleaned = `${digits.slice(0, firstDot + 1)}${digits.slice(firstDot + 1).replace(/\./g, '')}`
  }
  if (cleaned === '' || cleaned === '.') return ''
  if (Number(cleaned) > PERCENTAGE_MAX) return String(PERCENTAGE_MAX)
  return cleaned
}

/**
 * The canonical stored value: no "%", no redundant trailing zeros, empty when the
 * field is empty. A non-numeric legacy value is returned unchanged so the backend
 * rejects it and the proponent is asked to correct it, rather than the value being
 * replaced with an empty one.
 */
export function normalizePercentage(value) {
  return percentageFormValue(value)
}

/**
 * The displayed text of a percentage: the number followed by one "%" (never two,
 * even when the stored value still carries the legacy sign). Empty stays empty.
 */
export function formatPercentage(value) {
  if (isBlank(value)) return ''
  const number = parsePercentage(value)
  return number === null ? String(value) : `${formatNumber(number)}%`
}

/**
 * The automatic AVERAGE TOTAL of one Project Workplan row: the arithmetic mean of
 * the monthly percentages that were actually entered. Empty months are excluded
 * from both the sum and the divisor, and a row with no entered value has no average.
 *
 * Returned as numeric text ('' when there is nothing to average, "50.33" for a
 * repeating decimal) - use `formatAverageTotal` for the "%" display.
 *
 * @param {Array<string|number>} months
 * @returns {string}
 */
export function averagePercentage(months) {
  const values = (Array.isArray(months) ? months : [])
    .map(parsePercentage)
    .filter((number) => number !== null)
  if (values.length === 0) return ''
  const total = values.reduce((sum, number) => sum + number, 0)
  return String(Number((total / values.length).toFixed(2)))
}

/** The AVERAGE TOTAL as displayed, including the "%" sign; empty when no months. */
export function formatAverageTotal(months) {
  const average = averagePercentage(months)
  return average === '' ? '' : `${average}%`
}
