/**
 * riskManagement.js
 * ---------------------------------------------------------------------------
 * Section 21 - Risk Management Plan.
 *
 * The plan used to be one free-text field ("risks"). It is now a table: one row
 * per identified risk, each with its likelihood, its impact and the mitigation
 * measure that addresses it. This module is the single place where those rows are
 * shaped, validated and converted, so the Create Proposal form, the Revision form
 * and the Review & Submit step all behave identically.
 *
 * Row shape (the same four keys the API and the entity use, so the payload is a
 * straight mapping of what the proponent typed; no snake_case translation layer):
 *
 *   { identifiedRisks, likelihood, impact, mitigationMeasure }
 *
 * `likelihood` and `impact` only ever hold 'High', 'Medium' or 'Low' (RISK_LEVELS);
 * the backend rejects anything else, so the dropdowns and this module stay aligned.
 */

/** The only values a likelihood or an impact may carry. */
export const RISK_LEVELS = ['High', 'Medium', 'Low']

/** Shown above the table, exactly as the section reads on the form. */
export const RISK_SECTION_INSTRUCTION =
  'Identify the potential risks associated with the implementation of the proposed project. ' +
  'For each identified risk, specify its likelihood of occurrence, potential impact, and ' +
  'corresponding mitigation measure.'

/** The table columns, in order (also used by the documents and the review pages). */
export const RISK_COLUMNS = [
  { key: 'identifiedRisks', label: 'Identified Risks' },
  { key: 'likelihood', label: 'Likelihood' },
  { key: 'impact', label: 'Impact' },
  { key: 'mitigationMeasure', label: 'Mitigation Measure' }
]

/** Human label of one column, keyed by its row property. */
export const RISK_FIELD_LABELS = Object.fromEntries(
  RISK_COLUMNS.map((column) => [column.key, column.label])
)

/** A brand-new, empty risk entry. */
export const blankRiskEntry = () => ({
  identifiedRisks: '',
  likelihood: '',
  impact: '',
  mitigationMeasure: ''
})

/** True for null, undefined and whitespace-only text. */
export const isBlankText = (value) => value == null || String(value).trim() === ''

/** Canonical High / Medium / Low for a value the API or a save returned, else ''. */
export function normalizeRiskLevel(value) {
  const text = isBlankText(value) ? '' : String(value).trim().toLowerCase()
  if (!text) return ''
  return RISK_LEVELS.find((level) => level.toLowerCase() === text) || ''
}

/** A row with nothing in any of its four fields. */
export function isEmptyRiskEntry(row) {
  if (!row) return true
  return RISK_COLUMNS.every((column) => isBlankText(row[column.key]))
}

/**
 * The rows of a proposal as the form edits them, from whatever a save returned.
 * Handles the entity/DTO array the API sends (`riskManagement`) and a value that
 * arrives as a JSON string; unknown or empty entries are dropped so a reopened
 * proposal never shows duplicated or blank rows. A proposal that predates the
 * table simply has no rows here - its single text is shown from `risks` instead.
 */
export function riskEntriesFromSaved(saved) {
  let rows = saved
  if (typeof rows === 'string') {
    try {
      rows = JSON.parse(rows)
    } catch {
      return []
    }
  }
  if (!Array.isArray(rows)) return []

  return rows
    .filter((row) => row && typeof row === 'object' && !isEmptyRiskEntry(row))
    .map((row) => ({
      identifiedRisks: row.identifiedRisks ?? '',
      likelihood: normalizeRiskLevel(row.likelihood),
      impact: normalizeRiskLevel(row.impact),
      mitigationMeasure: row.mitigationMeasure ?? ''
    }))
}

/** The rows a proposal actually carries, for the review pages and the documents. */
export const riskRowsFrom = (proposal) => riskEntriesFromSaved(proposal && proposal.riskManagement)

/**
 * The rows to display in the editor: the saved ones, or one empty row so a new
 * proposal always starts with a row the proponent can type in.
 */
export function ensureRiskRows(rows) {
  if (Array.isArray(rows) && rows.length && rows.every((row) => row && typeof row === 'object')) {
    return rows
  }
  const list = Array.isArray(rows) ? rows.filter((row) => row && typeof row === 'object') : []
  return list.length ? list : [blankRiskEntry()]
}

/**
 * The `riskManagement` payload of a request: trimmed values, levels canonicalized,
 * and no all-empty row (the seeded empty row carries nothing to store). A form
 * that was never opened for this section sends an empty array, which the backend
 * stores as "no entries" - it never touches the rows of a client that omits the
 * field entirely.
 */
export function riskEntriesPayload(rows) {
  if (!Array.isArray(rows)) return []
  return rows
    .filter((row) => row && typeof row === 'object' && !isEmptyRiskEntry(row))
    .map((row) => ({
      identifiedRisks: isBlankText(row.identifiedRisks) ? '' : String(row.identifiedRisks).trim(),
      likelihood: normalizeRiskLevel(row.likelihood),
      impact: normalizeRiskLevel(row.impact),
      mitigationMeasure: isBlankText(row.mitigationMeasure) ? '' : String(row.mitigationMeasure).trim()
    }))
}

/** Field-level messages of one row, keyed by the four row properties. */
function rowErrorsOf(row) {
  const errors = {}
  const missing = {
    identifiedRisks: 'Enter the identified risk.',
    likelihood: 'Select the likelihood.',
    impact: 'Select the impact.',
    mitigationMeasure: 'Enter the mitigation measure.'
  }
  for (const column of RISK_COLUMNS) {
    const value = row[column.key]
    if (column.key === 'likelihood' || column.key === 'impact') {
      if (!normalizeRiskLevel(value)) errors[column.key] = missing[column.key]
    } else if (isBlankText(value)) {
      errors[column.key] = missing[column.key]
    }
  }
  return errors
}

/**
 * Validates the structured Risk Management Plan of a form.
 *
 * Returns `{ valid, hasEntries, hasIncomplete, emptyRows, rowErrors, message }`:
 *
 *   `rowErrors`   index -> `{ identifiedRisks, likelihood, impact, mitigationMeasure }`
 *                 with one message per missing value, so the form can point at the
 *                 exact field of the exact row (the second row is named as the second row).
 *   `emptyRows`   indexes of rows nothing was typed in. They are not errors by themselves
 *                 - the form seeds one for a new proposal - but a plan made only of empty
 *                 rows has no entries.
 *
 * Whitespace-only text counts as missing. A row the proponent started but did not finish
 * is reported and blocks submission.
 */
export function validateRiskEntries(rows) {
  const list = Array.isArray(rows) ? rows : []
  const rowErrors = []
  const emptyRows = []
  let hasEntries = false
  let hasIncomplete = false

  list.forEach((row, index) => {
    if (!row || typeof row !== 'object' || isEmptyRiskEntry(row)) {
      emptyRows.push(index)
      return
    }
    hasEntries = true
    const errors = rowErrorsOf(row)
    rowErrors[index] = errors
    if (Object.keys(errors).length) hasIncomplete = true
  })

  if (hasIncomplete) {
    const rowsWithErrors = rowErrors
      .map((errors, index) => (errors && Object.keys(errors).length ? index + 1 : null))
      .filter((position) => position !== null)
    const label = rowsWithErrors.length === 1 ? 'risk entry' : 'risk entries'
    return {
      valid: false,
      hasEntries,
      hasIncomplete,
      emptyRows,
      rowErrors,
      message: `Complete ${label} #${rowsWithErrors.join(', #')}: every entry needs an identified risk, a likelihood, an impact and a mitigation measure.`
    }
  }

  if (!hasEntries) {
    return {
      valid: false,
      hasEntries,
      hasIncomplete,
      emptyRows,
      rowErrors,
      message: 'Add at least one risk entry: an identified risk, its likelihood, its impact and its mitigation measure.'
    }
  }

  return { valid: true, hasEntries, hasIncomplete, emptyRows, rowErrors, message: '' }
}

/**
 * The rule the forms actually apply. Section 21 was already required before the table
 * existed, so a submission still needs a completed plan - but a proposal whose plan is
 * only the old single text (no structured rows, and the proponent did not start a new
 * one) stays valid, so an older record is never blocked or rewritten by this change.
 */
export function validateRiskPlan(rows, options = {}) {
  const result = validateRiskEntries(rows)
  if (result.valid) return result
  const legacyText = options.legacyText
  if (!result.hasEntries && !result.hasIncomplete && !isBlankText(legacyText)) {
    return { ...result, valid: true, legacyOnly: true, message: '' }
  }
  return result
}

/**
 * The readable one-entry-per-line rendering of the plan, used by the proponent-side
 * preview/print. It mirrors what the backend stores in the legacy `risks` column, so the
 * printed form and the saved record read the same way.
 */
export function riskPlanText(rows) {
  const plan = []
  ;(Array.isArray(rows) ? rows : []).forEach((row) => {
    if (!row || isEmptyRiskEntry(row)) return
    const levels = []
    if (normalizeRiskLevel(row.likelihood)) levels.push(`Likelihood: ${normalizeRiskLevel(row.likelihood)}`)
    if (normalizeRiskLevel(row.impact)) levels.push(`Impact: ${normalizeRiskLevel(row.impact)}`)
    let line = `${plan.length + 1}. ${isBlankText(row.identifiedRisks) ? '' : String(row.identifiedRisks).trim()}`
    if (levels.length) line += ` (${levels.join(', ')})`
    if (!isBlankText(row.mitigationMeasure)) {
      line += `\n   Mitigation: ${String(row.mitigationMeasure).trim()}`
    }
    plan.push(line)
  })
  return plan.join('\n')
}
