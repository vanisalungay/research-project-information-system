/**
 * personnelTime.js
 * ---------------------------------------------------------------------------
 * Section 24 (Personnel Requirement) - the "% Time Devoted" column.
 *
 * The proponent types a number only: the form shows the % symbol beside the field, so
 * the symbol is never typed and never stored twice, and the stored value is always a
 * plain number between 0 and 100 (decimals allowed, e.g. 12.5).
 *
 * A row saved before this change may hold "50%", "50 %" or even prose. Loading one is
 * lossless: a value that can be read as a percentage comes back as its number, and a
 * value that cannot (prose) is kept exactly as it is, so an existing proposal still
 * reads correctly - and the row is then flagged for correction instead of being
 * silently rewritten. Run the rules with: npm test
 */

/** Column heading, unchanged from the official form. */
export const TIME_DEVOTED_LABEL = '% Time Devoted'

/** The accepted range: a percentage of the personnel member's time. */
export const TIME_DEVOTED_MIN = 0
export const TIME_DEVOTED_MAX = 100

/** Decimals are supported (half a day a week is 10%, a quarter day is 5%, ...). */
export const TIME_DEVOTED_STEP = 0.01

export const TIME_DEVOTED_PLACEHOLDER = 'e.g. 50'

/** Shown under the table, so the rule is visible before a value is typed. */
export const TIME_DEVOTED_HINT =
  'Enter a number from 0 to 100. The % symbol is added automatically.'

/** Messages the proponent sees; the backend reports the same reason. */
export const TIME_DEVOTED_RANGE_ERROR = 'Enter a number between 0 and 100.'
export const TIME_DEVOTED_CLAMPED_NOTICE = 'Maximum is 100%.'

/** True when a value is neither null nor whitespace-only. */
const isBlank = (value) => value == null || String(value).trim() === ''

/**
 * Keeps only what can be part of a percentage: digits and a single decimal point.
 * Letters, '%', spaces, '-' and any other character a browser might let through
 * (type="number" still accepts "e", "+" and "-" while typing) are dropped, so the
 * field can never hold text or a negative number. A value above 100 is capped at 100
 * and reported as clamped, so the form can say why it changed.
 *
 * Returns `{ value, clamped }`.
 */
export function sanitizeTimeDevotedInput(raw) {
  const text = String(raw ?? '')

  // Digits and the first decimal point survive; every other character is discarded.
  let cleaned = text.replace(/[^\d.]/g, '')
  const firstDot = cleaned.indexOf('.')
  if (firstDot !== -1) {
    cleaned = cleaned.slice(0, firstDot + 1) + cleaned.slice(firstDot + 1).replace(/\./g, '')
  }

  // ".5" is the decimal 0.5; "007" is 7.
  if (cleaned.startsWith('.')) {
    cleaned = '0' + cleaned
  }
  cleaned = cleaned.replace(/^0+(?=\d)/, '')

  let clamped = false
  if (cleaned !== '' && cleaned !== '.') {
    const number = Number(cleaned)
    if (!Number.isFinite(number) || number > TIME_DEVOTED_MAX) {
      cleaned = String(TIME_DEVOTED_MAX)
      clamped = true
    } else if (number < TIME_DEVOTED_MIN) {
      cleaned = String(TIME_DEVOTED_MIN)
      clamped = true
    }
  }

  return { value: cleaned, clamped }
}

/**
 * The finished value: the sanitized input without a dangling decimal point, so
 * "50." (a value still being typed) is the number 50 before it is validated, sent or
 * displayed. Returns '' when nothing usable was entered.
 */
export function finalizeTimeDevoted(raw) {
  const { value } = sanitizeTimeDevotedInput(raw)
  return value.endsWith('.') ? value.slice(0, -1) : value
}

/**
 * A value as it has to look to be a percentage: no spacing, no leading "+", no typed
 * "%" and no dangling decimal point. Everything else is left untouched, so the check
 * below can refuse it (a letter, a comma, a minus sign).
 */
function percentCandidate(raw) {
  let text = String(raw ?? '').trim()
  text = text.replace(/^\+/, '')
  text = text.replace(/%\s*$/, '').trim()
  text = text.replace(/\s+/g, '')
  return text.endsWith('.') ? text.slice(0, -1) : text
}

/**
 * The numeric value of a percentage, or null when the value is not a number.
 * The value is capped exactly as the form and the backend cap it, so a legacy
 * "-25" is read as 0% and a legacy "150" as 100% instead of being shown and sent
 * exactly as it was once typed.
 */
export function timeDevotedNumber(raw) {
  const candidate = percentCandidate(raw)
  if (candidate === '' || !Number.isFinite(Number(candidate))) return null
  return Math.min(TIME_DEVOTED_MAX, Math.max(TIME_DEVOTED_MIN, Number(candidate)))
}

/**
 * True for a value that is a number within 0 - 100. Only digits and (optionally) one
 * decimal point are accepted: a letter, a comma, a minus sign or anything else makes the
 * value invalid, so a negative percentage can never pass validation either.
 */
export function isValidTimeDevoted(raw) {
  const candidate = percentCandidate(raw)
  if (!/^\d+(\.\d+)?$/.test(candidate)) return false
  const number = Number(candidate)
  return number >= TIME_DEVOTED_MIN && number <= TIME_DEVOTED_MAX
}

/**
 * The form value of a stored percentage. "50%" (or "50 %") becomes "50"; a value that is
 * outside 0-100 is capped exactly as the field caps a typed value; and a value that is not
 * a number at all is returned unchanged, so an old record is never rewritten behind the
 * proponent's back - it is flagged for correction instead.
 */
export function parseTimeDevoted(stored) {
  if (isBlank(stored)) return ''
  const text = String(stored).trim()
  // The same field used to be typed by hand: a trailing % and any spacing are removed.
  const withoutSymbol = text.replace(/%\s*$/, '').trim()
  if (withoutSymbol === '') return text
  if (/[A-Za-z]/.test(withoutSymbol)) return text
  // A legacy negative percentage is clamped to 0 here, exactly as the backend clamps
  // it. The field drops a minus sign as it is typed, so without this check reopening
  // an old "-25" row would silently turn it into +25.
  if (/^-\s*\d+(?:\.\d+)?$/.test(withoutSymbol)) return String(TIME_DEVOTED_MIN)
  const { value } = sanitizeTimeDevotedInput(withoutSymbol)
  const finished = value.endsWith('.') ? value.slice(0, -1) : value
  if (finished === '' || !Number.isFinite(Number(finished))) return text
  return finished
}

/**
 * The display form of a percentage: always with the % symbol ("50%"), the legacy text
 * of an unreadable old value kept as it is, and '' for an empty value.
 */
export function formatTimeDevoted(raw) {
  if (isBlank(raw)) return ''
  const number = timeDevotedNumber(raw)
  if (number === null) return String(raw).trim()
  return `${number}%`
}

/** The value the API receives: the plain number, never the % symbol. */
export function timeDevotedPayload(raw) {
  const number = timeDevotedNumber(raw)
  if (number === null) {
    // An unreadable legacy value is passed through unchanged, so saving never loses it.
    return isBlank(raw) ? '' : String(raw).trim()
  }
  return String(number)
}

/**
 * Validates the % Time Devoted column of every personnel row.
 *
 * A blank cell is not reported here: the section already requires all three personnel
 * fields, and that message is the one the form shows for an empty row. What is
 * reported here is a value that is present but is not a number between 0 and 100 -
 * including the prose of a legacy row - with the row number, so the proponent knows
 * which entry to correct.
 *
 * Returns `{ valid, rowErrors, message }`, where `rowErrors[index]` is the message of
 * that row (absent when the row is fine).
 */
export function validateTimeDevotedRows(rows) {
  const list = Array.isArray(rows) ? rows : []
  const rowErrors = []
  let invalid = 0

  list.forEach((row, index) => {
    const value = row ? (row.effort ?? row.timeDevoted) : ''
    if (isBlank(value)) return
    if (!isValidTimeDevoted(value)) {
      rowErrors[index] = `${TIME_DEVOTED_LABEL}: ${TIME_DEVOTED_RANGE_ERROR}`
      invalid += 1
    }
  })

  if (!invalid) return { valid: true, rowErrors, message: '' }

  const rowsWithErrors = rowErrors
    .map((message, index) => (message ? index + 1 : null))
    .filter((position) => position !== null)
  const label = rowsWithErrors.length === 1 ? 'personnel entry' : 'personnel entries'
  return {
    valid: false,
    rowErrors,
    message:
      `${TIME_DEVOTED_LABEL} must be a number between 0 and 100 ` +
      `(${label} #${rowsWithErrors.join(', #')}).`,
  }
}
