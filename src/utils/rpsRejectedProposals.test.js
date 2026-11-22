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

test('the rejected list fetches every office rejection and keeps only rejection returns', () => {
  assert.ok(
    PANEL.includes("api.get('/api/proposals/rejections')"),
    'the dedicated ORPS rejection queue is read instead of a status-only query',
  )
  assert.ok(PANEL.includes('isPendingRejection'), 'the rejection test is explicit')
  assert.ok(PANEL.includes('Date Processed'), 'the processed date column is present')
  assert.ok(PANEL.includes('processedDate(proposal)'), 'the column uses the workflow timestamp')
  assert.ok(PANEL.includes('proposal.rejectionRoutedAt || proposal.routedAt'),
    'a pending hand-off shows when it was routed to ORPS')
  assert.ok(PANEL.includes('proposal.rejectionProcessedAt || proposal.processedAt'),
    'a finalized rejection shows when ORPS forwarded it')
  const QUEUE = read('./rejectionQueue.js')
  assert.ok(
    QUEUE.includes("String(proposal?.returnKind || '').toUpperCase() === 'REJECTION'"),
    'return_kind separates a rejection from a revision return',
  )
  assert.ok(PANEL.includes("from '@/utils/rejectionQueue.js'"),
    'the panel uses the shared rejection read model')
  assert.ok(
    !PANEL.includes("String(proposal?.returnedByOffice || '').toUpperCase() === 'OVCRIGE'"),
    'the office alone must not classify a historical revision return as a rejection',
  )
  assert.ok(
    QUEUE.includes('isRejectedProposal'),
    'finalized REJECTED rows stay in the Rejected Proposals category',
  )
  assert.ok(
    PANEL.includes('rejectionOffice(proposal)'),
    'the rejecting office is shown to ORPS',
  )
  assert.ok(
    PANEL.includes('rejectionRemarks(proposal)'),
    'the rejection reason is shown to ORPS',
  )
  assert.ok(
    PANEL.includes('selectedOffice'),
    'ORPS can filter the rejected queue by the rejecting office',
  )
  assert.ok(
    PANEL.includes('rejectionOffices'),
    'the office filter is populated from the real rejected rows',
  )
  assert.ok(PANEL.includes("label: 'All'"), 'the All filter is offered')
  assert.ok(
    PANEL.includes("label: proposalStatusLabel('RETURNED_TO_RPS')"),
    'Returned to RPS uses the shared status label',
  )
  assert.ok(
    PANEL.includes("label: proposalStatusLabel('REJECTED')"),
    'Rejected uses the shared status label',
  )
  assert.ok(PANEL.includes("value: 'RETURNED'"), 'Returned to RPS maps to the pending hand-off')
  assert.ok(PANEL.includes("value: 'REJECTED'"), 'Rejected maps to the finalized status')
  assert.ok(PANEL.includes('matchesRejectionStatus'),
    'the filter is matched against the stored workflow record, not the label')
  assert.ok(PANEL.includes('clearFilters'), 'the filters can be cleared in one action')
  assert.ok(PANEL.includes('hasActiveFilters'), 'Clear is offered only while a filter is active')
  assert.ok(PANEL.includes('activated()'), 'the queue reloads when the category is shown again')
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
  assert.ok(PANEL.includes('awaiting ORPS'), 'the pending category context is shown')
  assert.ok(
    PANEL.includes('forwarded to proponent'),
    'finalized rejections are distinguished from pending ones',
  )
  assert.ok(PANEL.includes('pageSize: 10'), 'the list paginates')
  assert.ok(PANEL.includes('<PaginationBar'), 'the shared pagination bar is rendered')
})
