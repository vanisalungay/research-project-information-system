/**
 * quarterlyReportPercentageUi.test.js
 * ---------------------------------------------------------------------------
 * Source-level guards for the presentation rules of the proposal-populated
 * Quarterly Progress Report form:
 *   - the six project identification fields are displayed from the saved
 *     proposal and are never editable;
 *   - the Major Accomplishments percentage columns and the Project Workplan
 *     month cells use the shared numeric PercentageInput;
 *   - the Project Workplan's AVERAGE TOTAL is labelled automatic, read-only and
 *     derived from the month cells instead of being typed in.
 *
 * The components import Vite-only assets, so they are checked at the source
 * level, the technique the other guard tests use.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const QPR = read('../components/QuarterlyReportForm.vue')
const WORKPLAN = read('../components/ProjectWorkplanForm.vue')
const PERCENTAGE_INPUT = read('../components/PercentageInput.vue')
const PAGE = read('../views/Proponent/ProponentQuarterlyReportForm.vue')

const PROJECT_FIELDS = [
  'form.projectTitle',
  'form.projectLeader',
  'form.leaderGender',
  'form.projectDuration',
  'form.projectStartDate',
  'form.projectEndDate',
]

test('a new report takes the six project fields from the saved proposal', () => {
  assert.match(
    PAGE,
    /Object\.assign\(report\.value, proposalProjectInfo\(p\.data\)\)/,
    'a new report is populated from the associated proposal',
  )
  assert.match(
    PAGE,
    /report\.value = withProposalProjectInfo\(report\.value, p\.data\)/,
    'a reopened report only fills blank fields, keeping its saved snapshot',
  )
})

test('the six project fields are displayed read-only, never as inputs', () => {
  for (const field of PROJECT_FIELDS) {
    const pattern = new RegExp(
      `class="qpr-value qpr-value--source"[^>]*>\\{\\{ ${field.replace(/\./g, '\\.')} \\|\\|`,
      's',
    )
    assert.match(QPR, pattern, `${field} is shown as a proposal-sourced value`)
    assert.ok(
      !QPR.includes(`v-model="${field}"`),
      `${field} must not be editable in the report`,
    )
  }
  assert.match(QPR, /title="From the saved proposal"/)
})

test('the Major Accomplishments percentage columns are numeric PercentageInputs', () => {
  assert.match(QPR, /v-model="row\.percentagePeriod"/)
  assert.match(QPR, /v-model="row\.percentageCumulative"/)
  assert.match(QPR, /import PercentageInput from '@\/components\/PercentageInput\.vue'/)
  assert.match(
    QPR,
    /formatPercentage\(row\.percentagePeriod\)/,
    'the read-only view prints the stored value with one sign',
  )
  assert.match(QPR, /formatPercentage\(row\.percentageCumulative\)/)
})

test('the Workplan month cells use PercentageInput and the total is automatic', () => {
  assert.match(WORKPLAN, /v-model="obj\.months\[mi\]"/)
  assert.match(WORKPLAN, /import PercentageInput from '@\/components\/PercentageInput\.vue'/)
  assert.match(WORKPLAN, /AVERAGE TOTAL/)
  assert.match(
    WORKPLAN,
    /\* AVERAGE TOTAL is calculated automatically based on non-zero month values\./,
    'the column explains that the total is derived, not typed in',
  )
  assert.match(WORKPLAN, /aria-readonly="true"/)
  assert.match(WORKPLAN, /\{\{ averageTotal\(obj\) \}\}/, 'the total is derived on screen')
  assert.ok(
    !WORKPLAN.includes('v-model="obj.total"'),
    'the total can never be typed in or sent as an editable field',
  )
})

test('TARGET and ACTUAL remain free-text fields without percentage headings', () => {
  for (const heading of ['OBJECTIVES', 'TARGET', 'ACTUAL', 'FOR THE PERIOD', 'CUMULATIVE']) {
    assert.match(QPR, new RegExp(`<th>${heading}</th>`), `${heading} has no percentage sign`)
  }
  assert.ok(!QPR.includes('TARGET (%)'), 'TARGET heading does not carry a percentage sign')
  assert.ok(!QPR.includes('ACTUAL (%)'), 'ACTUAL heading does not carry a percentage sign')
  assert.match(QPR, /v-model="row\.target"[\s\S]{0,300}<\/textarea>/)
  assert.match(QPR, /v-model="row\.actual"[\s\S]{0,300}<\/textarea>/)
  // The block containing each free-text field is a plain textarea. Only the
  // FOR THE PERIOD / CUMULATIVE cells use the shared PercentageInput.
  const targetBlock = QPR.slice(QPR.indexOf('v-model="row.target"') - 300, QPR.indexOf('v-model="row.target"') + 120)
  const actualBlock = QPR.slice(QPR.indexOf('v-model="row.actual"') - 300, QPR.indexOf('v-model="row.actual"') + 120)
  assert.ok(!targetBlock.includes('PercentageInput'), 'TARGET is not a PercentageInput')
  assert.ok(!actualBlock.includes('PercentageInput'), 'ACTUAL is not a PercentageInput')
})

test('the sign-off card shows the designated reviewer without claiming a review', () => {
  assert.match(WORKPLAN, /Prepared By/)
  assert.match(WORKPLAN, /Reviewed By/)
  assert.match(WORKPLAN, /ANABELLE DECE A\. ESPADERO, PhD/)
  assert.match(WORKPLAN, /Director, Research and Innovation Institute/)
  assert.ok(!WORKPLAN.includes('Draft — not yet submitted'), 'the draft status message is gone')
  assert.ok(!WORKPLAN.includes('Pending review'), 'the pending review message is gone')
  assert.ok(!WORKPLAN.includes('No review recorded'), 'the designated reviewer replaces the raw workflow label')
  assert.ok(!WORKPLAN.includes('RPS_STAFF'), 'the raw role code is not shown as the reviewer')
  assert.ok(!/signature verified/i.test(WORKPLAN), 'no signature is claimed as verified')
})

test('the shared percentage field is numeric and shows exactly one sign', () => {
  assert.match(PERCENTAGE_INPUT, /inputmode="decimal"/)
  assert.match(PERCENTAGE_INPUT, /sanitizePercentageInput/)
  assert.match(PERCENTAGE_INPUT, /normalizePercentage/)
  assert.match(PERCENTAGE_INPUT, /percentage-input__symbol/)
  assert.ok(
    !PERCENTAGE_INPUT.includes('type="number"'),
    'a text field with a decimal keyboard hint keeps the cursor predictable',
  )
})
