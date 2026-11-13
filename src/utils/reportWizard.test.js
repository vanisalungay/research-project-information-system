/**
 * reportWizard.test.js
 * Verifies the navigation and validation rules of the proponent's 3-step
 * Quarterly Progress Report wizard: page numbers never leave 1-3, the progress
 * indicator reports the correct state, a forward move is blocked by the first
 * page that fails validation while a backward move is always allowed, and only
 * the rules the existing workflow already has are enforced (Project Workplan
 * required, project dates in order) - no new mandatory field is introduced.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  REPORT_WIZARD_PAGES,
  REPORT_WIZARD_STEPS,
  clampStep,
  isReportStepValid,
  resolveReportStep,
  stepState,
  validateReportStep,
} from './reportWizard.js'

// 1. The wizard has the three pages of the report, with descriptive labels.
test('the wizard has the three report pages in order', () => {
  assert.equal(REPORT_WIZARD_STEPS, 3)
  assert.deepEqual(
    REPORT_WIZARD_PAGES.map((page) => page.label),
    ['Quarterly Progress Report', 'Report Continuation', 'Workplan'],
  )
  assert.equal(REPORT_WIZARD_PAGES[0].title, 'Quarterly Progress Report')
  assert.equal(REPORT_WIZARD_PAGES[2].title, 'Workplan')
})

// 2. Page numbers stay inside the wizard.
test('page numbers are clamped to the 1-3 range', () => {
  assert.equal(clampStep(0), 1)
  assert.equal(clampStep(1), 1)
  assert.equal(clampStep(2), 2)
  assert.equal(clampStep(3), 3)
  assert.equal(clampStep(4), 3)
  assert.equal(clampStep(-2), 1)
})

test('a missing or unusable page number falls back to page 1', () => {
  assert.equal(clampStep(undefined), 1)
  assert.equal(clampStep(null), 1)
  assert.equal(clampStep('not-a-page'), 1)
  assert.equal(clampStep('2'), 2)
})

// 3. Progress indicator states.
test('the progress indicator marks earlier pages done, the visible page current and later pages upcoming', () => {
  assert.equal(stepState(1, 1), 'current')
  assert.equal(stepState(2, 1), 'upcoming')
  assert.equal(stepState(3, 1), 'upcoming')

  assert.equal(stepState(1, 2), 'done')
  assert.equal(stepState(2, 2), 'current')
  assert.equal(stepState(3, 2), 'upcoming')

  assert.equal(stepState(1, 3), 'done')
  assert.equal(stepState(2, 3), 'done')
  assert.equal(stepState(3, 3), 'current')
})

// 4. Page 1 - the project dates printed in the report header must be in order.
test('page 1 rejects an end date that is earlier than the start date', () => {
  const context = { payload: { projectStartDate: '2026-01-01', projectEndDate: '2025-12-31' } }
  const result = validateReportStep(1, context)

  assert.equal(result.errors.projectEndDate, 'The end date cannot be earlier than the start date.')
  assert.equal(
    result.summary,
    'Please correct the highlighted field on this page before continuing.',
  )
  assert.equal(isReportStepValid(1, context), false)
})

test('page 1 accepts ordered, partial or empty dates', () => {
  const cases = [
    { projectStartDate: '2026-01-01', projectEndDate: '2026-12-31' },
    { projectStartDate: '2026-01-01', projectEndDate: '2026-01-01' },
    { projectStartDate: '2026-01-01', projectEndDate: '' },
    { projectStartDate: '', projectEndDate: '2026-12-31' },
    {},
  ]

  for (const payload of cases) {
    const result = validateReportStep(1, { payload })
    assert.deepEqual(result.errors, {}, JSON.stringify(payload))
    assert.equal(result.summary, '')
  }
})

// 5. Page 2 - the continuation of the report has no required field in the
//    existing report specification, so it never blocks navigation.
test('page 2 has no required field and is always valid', () => {
  assert.deepEqual(validateReportStep(2).errors, {})
  assert.equal(validateReportStep(2, { payload: {} }).summary, '')
  assert.equal(isReportStepValid(2, { payload: { projectTitle: '' } }), true)
})

// 6. Page 3 - the Project Workplan rule the workflow already enforces.
test('page 3 requires the Project Workplan with at least one objective', () => {
  const result = validateReportStep(3, { hasWorkplan: false })

  assert.match(result.errors.workplan, /Project Workplan is required/)
  assert.equal(result.summary, result.errors.workplan)
  assert.equal(isReportStepValid(3, { hasWorkplan: true }), true)
})

// 7. Navigation: forward moves are validated, backward moves are free.
test('moving forward is allowed when every page passed through is valid', () => {
  const context = { payload: {}, hasWorkplan: false }

  assert.equal(resolveReportStep(2, 1, context), 2)
  // Leaving page 2 for the Workplan is allowed; the Workplan itself is only
  // validated when it is left (Review / Submit).
  assert.equal(resolveReportStep(3, 2, context), 3)
})

test('a forward move stops on the first page that fails validation', () => {
  const invalidPage1 = { payload: { projectStartDate: '2026-06-01', projectEndDate: '2026-01-01' } }

  assert.equal(resolveReportStep(2, 1, invalidPage1), 1)
  assert.equal(resolveReportStep(3, 1, invalidPage1), 1)
})

test('moving backward or staying on the current page is always allowed', () => {
  const invalidWorkplan = { hasWorkplan: false }

  assert.equal(resolveReportStep(1, 3, invalidWorkplan), 1)
  assert.equal(resolveReportStep(2, 3, invalidWorkplan), 2)
  assert.equal(resolveReportStep(2, 2, invalidWorkplan), 2)
})

test('navigation never leaves the 1-3 range', () => {
  assert.equal(resolveReportStep(0, 1, {}), 1)
  assert.equal(resolveReportStep(9, 1, {}), 3)
  assert.equal(resolveReportStep(4, 3, {}), 3)
})

// 8. Each page can be validated against its own part of the current form state.
test('the validation context can be resolved per page', () => {
  const byStep = (step) =>
    step === 1
      ? { payload: { projectStartDate: '2026-06-01', projectEndDate: '2026-01-01' } }
      : { hasWorkplan: true }

  assert.equal(resolveReportStep(3, 1, byStep), 1)
  assert.equal(resolveReportStep(3, 2, byStep), 3)
})

test('an empty context keeps the wizard usable', () => {
  assert.deepEqual(validateReportStep(1, undefined).errors, {})
  assert.equal(resolveReportStep(3, 1), 3)
  assert.equal(stepState(1, undefined), 'current')
})
