/**
 * reportWizard.js
 * Pure navigation and validation rules of the proponent's 3-step Quarterly
 * Progress Report wizard:
 *   Page 1 - Quarterly Progress Report
 *   Page 2 - continuation of the Quarterly Progress Report
 *   Page 3 - Project Workplan
 *
 * The rules live here (instead of inside the view) so the wizard stays a thin
 * presentation layer and the behaviour can be verified without a browser - see
 * reportWizard.test.js.
 *
 * The wizard does not invent new mandatory fields: it only enforces rules the
 * existing reporting workflow already has (the backend requires the Project
 * Workplan - at least one objective - for reporting stages 1-3) plus a plain
 * data-consistency check on the project dates printed in the report header.
 */

/** Number of pages of the report wizard. */
export const REPORT_WIZARD_STEPS = 3

/**
 * The pages, in order. `label` is shown in the progress indicator, `title` is
 * the heading of the page.
 */
export const REPORT_WIZARD_PAGES = [
  { key: 'qpr', label: 'Quarterly Progress Report', title: 'Quarterly Progress Report' },
  {
    key: 'continuation',
    label: 'Report Continuation',
    title: 'Quarterly Progress Report — Continuation',
  },
  { key: 'workplan', label: 'Workplan', title: 'Workplan' },
]

/** Keeps a page number inside 1..REPORT_WIZARD_STEPS. */
export function clampStep(step) {
  const value = Number(step)
  if (!Number.isFinite(value)) return 1
  return Math.min(Math.max(Math.trunc(value), 1), REPORT_WIZARD_STEPS)
}

/**
 * Progress-indicator state of a page: 'done' for a page before the current one,
 * 'current' for the visible page and 'upcoming' for the pages after it.
 */
export function stepState(step, currentStep) {
  const current = clampStep(currentStep)
  const value = clampStep(step)
  if (value < current) return 'done'
  if (value === current) return 'current'
  return 'upcoming'
}

/**
 * Validation of a single page.
 *
 * @param {number} step page number (1-3)
 * @param {{ payload?: object, hasWorkplan?: boolean }} context current form
 *   state: the quarterly progress report payload (pages 1-2) and whether the
 *   Project Workplan form has at least one described objective (page 3)
 * @returns {{ errors: Record<string, string>, summary: string }} field messages
 *   (rendered next to the field they belong to) and the page-level message,
 *   which is empty while the page is valid
 */
export function validateReportStep(step, context = {}) {
  const { payload = {}, hasWorkplan = false } = context
  const errors = {}
  let summary = ''

  if (clampStep(step) === 1) {
    const start = payload.projectStartDate
    const end = payload.projectEndDate
    // (4)-(6) Project dates: only checked when both are filled in, so no new
    // mandatory field is introduced.
    if (start && end && String(end) < String(start)) {
      errors.projectEndDate = 'The end date cannot be earlier than the start date.'
    }
    if (Object.keys(errors).length) {
      summary = 'Please correct the highlighted field on this page before continuing.'
    }
  }

  // Page 2 (the continuation: expected outputs / 6Ps, problems and concerns,
  // suggested solutions) has no required field in the existing report
  // specification, so it is always valid.
  if (clampStep(step) === REPORT_WIZARD_STEPS && !hasWorkplan) {
    errors.workplan =
      'The Project Workplan is required before this report can be submitted. ' +
      'Please describe at least one objective.'
    summary = errors.workplan
  }

  return { errors, summary }
}

/** True when a page has no validation message and can therefore be left. */
export function isReportStepValid(step, context) {
  return Object.keys(validateReportStep(step, context).errors).length === 0
}

/**
 * Resolves a navigation request into the page that must be shown.
 *
 * Moving backward (or to the current page) is always allowed. A forward move is
 * limited to the first page that still fails validation, so the proponent stays
 * on the page that needs attention instead of silently skipping it.
 *
 * @param {number} target page the proponent asked for
 * @param {number} currentStep page currently shown
 * @param {object|((step: number) => object)} context validation context, or a
 *   function returning it per page (used to validate each page against the
 *   current form state)
 * @returns {number} the page to show
 */
export function resolveReportStep(target, currentStep, context = {}) {
  const current = clampStep(currentStep)
  const wanted = clampStep(target)
  if (wanted <= current) return wanted

  for (let step = current; step < wanted; step += 1) {
    const stepContext = typeof context === 'function' ? context(step) : context
    if (!isReportStepValid(step, stepContext)) return step
  }
  return wanted
}
