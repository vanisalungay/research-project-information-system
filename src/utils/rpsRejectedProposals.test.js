/**
 * rpsRejectedProposals.test.js
 * ---------------------------------------------------------------------------
 * The ORPS Submitted Proposals page has a Rejected Proposals category. It must use the
 * same table as the Submitted list, fetch the real RETURNED_TO_RPS rows, and keep only
 * the proposals OVCRIGE returned. Revision Requests must exclude those rows so the two
 * categories never overlap.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PAGE = read('../views/Rps/RpsSubmittedProposals.vue')
const PANEL = read('../views/Rps/RpsSubmittedProposalsPanel.vue')
const REVISIONS = read('../views/Rps/RpsRevisionRequestsPanel.vue')

test('Rejected Proposals is a normal category beside the existing filters', () => {
  assert.ok(PAGE.includes('Rejected Proposals'), 'the category label is rendered')
  assert.ok(PAGE.includes("activeCategory === 'rejected'"), 'the tab has an active state')
  assert.ok(PAGE.includes("activeCategory = 'rejected'"), 'the tab switches the category')
  assert.ok(PAGE.includes('rejected: RpsSubmittedProposalsPanel'), 'it reuses the submitted panel')
  assert.ok(PAGE.includes("category: 'rejected'"), 'the panel receives the rejected mode')
  assert.ok(
    PAGE.includes(':key="activeCategory"'),
    'each category mounts its own cached panel, so All data cannot leak into Rejected',
  )
  assert.ok(
    PAGE.indexOf('Rejected Proposals') > PAGE.indexOf('Endorsed Proposals'),
    'the new category follows Endorsed Proposals',
  )
})

test('the rejected list fetches returned-to-RPS rows and keeps only rejection returns', () => {
  assert.ok(PANEL.includes("statusIn: ['RETURNED_TO_RPS'], sort: 'returnedAt'"))
  assert.ok(PANEL.includes('isOvcrigeRejection'), 'the rejection test is explicit')
  assert.ok(
    PANEL.includes("String(proposal?.returnKind || '').toUpperCase() === 'REJECTION'"),
    'return_kind separates a rejection from a revision return',
  )
  assert.ok(
    !PANEL.includes("String(proposal?.returnedByOffice || '').toUpperCase() === 'OVCRIGE'"),
    'the office alone must not classify a historical revision return as a rejection',
  )
  assert.ok(
    PANEL.includes("statusIn: ['FOR REVIEW', 'SUBMITTED', 'REVISION', 'RETURNED_TO_RPS']"),
    'the All Submitted request is unchanged',
  )
  assert.ok(PANEL.includes("sort: 'submittedAt'"), 'the All Submitted order is unchanged')
  assert.ok(!PANEL.includes('.sort('), 'the client must not re-sort the server order')
})

test('Revision Requests excludes OVCRIGE rejections', () => {
  assert.ok(REVISIONS.includes('isOvcrigeRejection'))
  assert.ok(REVISIONS.includes('.filter((p) => !isOvcrigeRejection(p))'))
  assert.ok(
    !REVISIONS.includes('.sort('),
    'the revision tabs keep the server order',
  )
})

test('the rejected table reuses the submitted columns and the Review action', () => {
  for (const header of [
    'Document ID',
    'Proposal Title',
    'Proponent',
    'Date Submitted',
    'Status',
    'Actions',
  ]) {
    assert.ok(PANEL.includes(header), header)
  }
  assert.ok(PANEL.includes('@click="viewProposal(proposal)"'), 'the row action is kept')
  assert.ok(PANEL.includes("name: 'RpsForwardRevision'"), 'rejections open the ORPS processing page')
  assert.ok(PANEL.includes('Rejected by OVCRIGE'), 'the category context is shown')
  assert.ok(PANEL.includes('pageSize: 10'), 'the rejected list paginates')
  assert.ok(PANEL.includes('pager-controls'), 'the pager is rendered for the rejected category')
})
