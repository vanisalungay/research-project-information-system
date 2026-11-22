/**
 * recProposalInfo.test.js
 * ---------------------------------------------------------------------------
 * The REC review page's Proposal Info section no longer shows Funding Source or
 * Requested Budget. The removal is REC-only: the OVCAF/RPS budget screens keep
 * their own fields.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const REVIEW = read('../views/Rec/RecReviewForm.vue')
const RPS_BUDGET = read('../views/Rps/RpsBudgetRelease.vue')

test('the REC Proposal Info section no longer renders the two budget fields', () => {
  for (const removed of ['Funding Source', 'Requested Budget', 'fundingSource', 'requestedBudget']) {
    assert.ok(!REVIEW.includes(removed), `${removed} must be gone from the REC review page`)
  }
})

test('the remaining Proposal Info fields and grid are intact', () => {
  assert.ok(REVIEW.includes('info-grid'), 'the Proposal Info grid remains')
  for (const label of [
    'Title',
    'Proponent',
    'Department / Office',
    'Date Submitted',
    'Status',
    'Research Category',
  ]) {
    assert.ok(REVIEW.includes(`<label>${label}</label>`), `${label} must remain`)
  }
})

test('another role keeps its own Requested Budget field', () => {
  assert.ok(
    RPS_BUDGET.includes('Requested Budget'),
    'the RPS budget release screen must not be affected by the REC-only removal',
  )
})
