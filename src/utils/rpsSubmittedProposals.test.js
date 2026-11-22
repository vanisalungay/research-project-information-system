/**
 * rpsSubmittedProposals.test.js
 * ---------------------------------------------------------------------------
 * The RPS Staff "Submitted Proposals" list must be ordered by the real submission
 * event. `created_at` is when the draft row was first saved, so it is the wrong value
 * both for ordering and for the "Date Submitted" column.
 *
 * The ordering itself lives on the server (ProposalService), where it applies to the
 * whole result set; this test guards the request that asks for it and the column that
 * displays it.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PANEL = read('../views/Rps/RpsSubmittedProposalsPanel.vue')

test('the list asks the server for submission-date ordering', () => {
  assert.match(PANEL, /sort:\s*'submittedAt'/, 'the request carries sort=submittedAt')
  assert.ok(
    PANEL.indexOf("sort: 'submittedAt'") < PANEL.indexOf('paramsSerializer'),
    'the sort is a query parameter of the same /api/proposals request',
  )
  assert.ok(
    PANEL.includes("statusIn: ['FOR REVIEW', 'SUBMITTED', 'REVISION', 'RETURNED_TO_RPS']"),
    'the existing status filter is unchanged',
  )
})

test('the Date Submitted column reads submittedAt, never createdAt', () => {
  assert.match(PANEL, /proposal\.submittedAt\?\.substring\(0, 10\)/)
  assert.ok(
    !PANEL.includes('proposal.createdAt?.substring'),
    'the creation date must not be shown as the submission date',
  )
})

test('search and status filtering keep the server order', () => {
  // filteredProposals must filter only: sorting it again in the browser would discard the
  // server's tie-breaker and missing-timestamp rule.
  assert.match(PANEL, /return this\.proposals\.filter\(/)
  assert.ok(
    !PANEL.includes('.sort('),
    'the client must not re-sort what the server ordered',
  )
})
