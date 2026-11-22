/**
 * ovcafFinancialValidationUi.test.js
 * ---------------------------------------------------------------------------
 * Guards the OVCAF financial-validation form:
 *  - Submit starts disabled and only enables for a complete checklist (approval)
 *    or written remarks (return/rejection);
 *  - the remaining checklist items are explained;
 *  - the decision is view-only once the project is Implementation Approved;
 *  - return/rejection copy states the ORPS hand-off;
 *  - no emoji remains in the page.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Ovcaf/OvcafFinancialValidation.vue')

test('the page contains no emoji characters', () => {
  const emoji = /[\u{1F300}-\u{1FAFF}\u{2600}-\u{27BF}\u{2B00}-\u{2BFF}]/gu
  const found = [...PAGE.matchAll(emoji)].map((m) => m[0])
  assert.deepEqual(found, [], 'emoji glyphs must be replaced with inline SVG icons')
  assert.ok(PAGE.includes('const icons = {'), 'the professional icon map must be used')
  assert.ok(PAGE.includes("check: svg(") && PAGE.includes("alert: svg("))
})

test('Submit is disabled until the selected decision is complete', () => {
  assert.ok(PAGE.includes(':disabled="!canSubmit"'), 'the Submit button must be bound to canSubmit')
  const start = PAGE.indexOf('const canSubmit = computed(() => {')
  assert.ok(start >= 0)
  const source = PAGE.slice(start, PAGE.indexOf('const confirmBtnClass', start))
  assert.ok(source.includes('submitLoading.value || approvalBlocked.value || decisionLocked.value'))
  assert.ok(source.includes("if (decision.value === 'APPROVED_ENDORSED') return isAllChecked.value"))
  assert.ok(source.includes("return remarks.value.trim() !== ''"))
})

test('the form explains which checklist items remain', () => {
  assert.ok(PAGE.includes('const missingChecklistItems = computed('))
  assert.ok(PAGE.includes('v-for="item in missingChecklistItems"'))
  assert.ok(PAGE.includes('Remaining before approval'))
  assert.ok(PAGE.includes('Remaining: ${missingChecklistItems.value.join(\'; \')}'))
})

test('Implementation Approved closes the evaluation decision', () => {
  assert.ok(PAGE.includes("normalizeStatusKey(proposal.value?.status) === 'APPROVED'"))
  assert.ok(PAGE.includes('v-if="decisionLocked" class="locked-banner"'))
  assert.ok(PAGE.includes('This proposal is Implementation Approved. The financial evaluation decision is closed.'))
  assert.ok(PAGE.includes(':disabled="decisionLocked"'), 'inputs/radios must be disabled')
  assert.ok(PAGE.includes(':disabled="submitLoading || decisionLocked"'), 'Reset is disabled as well')
  assert.ok(PAGE.includes('if (decisionLocked.value) {'), 'the handler must refuse a locked submission')
})

test('the checklist is required only for approval, remarks for return/rejection', () => {
  assert.ok(PAGE.includes("decision.value === 'APPROVED_ENDORSED' && !isAllChecked.value"))
  assert.ok(PAGE.includes("decision.value !== 'APPROVED_ENDORSED' && !remarks.value.trim()"))
  assert.ok(PAGE.includes("const remarksRequired = computed(() => decision.value !== 'APPROVED_ENDORSED')"))
})

test('return and rejection describe the ORPS hand-off, not a direct proponent action', () => {
  assert.ok(PAGE.includes('to ORPS for budget revision'))
  assert.ok(PAGE.includes('return it to ORPS for processing'))
  assert.ok(PAGE.includes('ORPS records the final rejection and notifies the proponent.'))
  assert.ok(!PAGE.includes('permanently reject'), 'the old direct-rejection wording is gone')
})

test('successful submission returns to the renamed Proposals page', () => {
  assert.ok(PAGE.includes("router.push('/ovcaf/proposals')"))
  assert.ok(PAGE.includes('label="Back to Proposals" to="/ovcaf/proposals"'))
})
