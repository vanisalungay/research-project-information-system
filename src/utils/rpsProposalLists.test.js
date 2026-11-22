/**
 * rpsProposalLists.test.js
 * ---------------------------------------------------------------------------
 * Each RPS Staff proposal list is newest-first by the event date that listing actually
 * represents. The ordering is done by the server; the pages ask for the right field and
 * must not re-sort the result in the browser.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const MONITORING = read('../views/Rps/RpsProposalMonitoring.vue')
const SUBMITTED = read('../views/Rps/RpsSubmittedProposalsPanel.vue')
const ENDORSED = read('../views/Rps/RpsEndorsedProPanel.vue')
const REVISIONS = read('../views/Rps/RpsRevisionRequestsPanel.vue')
const BUDGET = read('../views/Rps/RpsBudgetProcessing.vue')

test('All Proposals sorts by submission date and shows that date', () => {
  assert.match(MONITORING, /params:\s*\{\s*sort:\s*'submittedAt'\s*\}/)
  assert.match(MONITORING, /formatDate\(prop\.submittedAt\)/)
  assert.ok(
    !MONITORING.includes('new Date(b.createdAt || 0)'),
    'a client-side createdAt sort must not override the server order',
  )
})

test('Submitted Proposals keeps its submission-date ordering', () => {
  assert.match(SUBMITTED, /sort:\s*'submittedAt'/)
})

test('Endorsed Proposals sorts by the endorsement event', () => {
  assert.match(ENDORSED, /sort=endorsedAt/)
  assert.ok(
    !ENDORSED.includes('.sort('),
    'the list must not re-sort what the server ordered',
  )
})

test('Revision Requests orders each tab by the event that put it there', () => {
  assert.match(REVISIONS, /statusIn=RETURNED_TO_RPS&sort=returnedAt/)
  assert.match(REVISIONS, /statusIn=RPS_RETURNED&sort=revisionForwardedAt/)
  assert.ok(!REVISIONS.includes('.sort('), 'the tabs keep the server order')
})

test('Budget Processing sorts by the latest workflow change', () => {
  assert.match(BUDGET, /sort=updatedAt/)
})

test('search and filters only filter, they never reorder the server list', () => {
  for (const [name, source] of [
    ['All Proposals', MONITORING],
    ['Submitted', SUBMITTED],
    ['Endorsed', ENDORSED],
    ['Budget', BUDGET],
  ]) {
    assert.ok(!source.includes('.sort('), `${name} must not re-sort in the browser`)
  }
})
