/**
 * ovcafProposalDisplay.test.js
 * ---------------------------------------------------------------------------
 * The OVCAF proposal card used to show a "Proposed Budget" amount and a "Funding Source"
 * - values that came from the legacy amount table (always ₱0/"Not specified" for proposals
 * created since section 25 became a file upload). OVCAF must instead see the actual
 * uploaded Line-Item Budget document, and the proponent must be the name typed into the
 * proposal form, not the generic login account.
 *
 * The pages are validated at the source level, the technique the other guard tests use,
 * because they import Vite `?inline` assets that Node cannot resolve.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CARD = read('../views/Ovcaf/OvcafFinancialValidation.vue')
const DETAILS = read('../views/Ovcaf/OvcafProposalDetails.vue')
const INBOX = read('../views/Ovcaf/OvcafProposalInbox.vue')
const VALIDATE = read('../views/Ovcaf/OvcafValidate.vue')

test('the OVCAF card no longer shows a proposed-budget amount or funding source', () => {
  assert.ok(!CARD.includes('Proposed Budget'), 'the legacy budget chip is gone')
  assert.ok(!CARD.includes('Funding Source'), 'the legacy funding-source chip is gone')
  assert.ok(!CARD.includes('formatCurrency(proposal.budget)'), 'no amount is rendered')
})

test('the OVCAF card shows the uploaded Line-Item Budget document', () => {
  assert.ok(CARD.includes('In-line Item Budget'), 'the card label')
  assert.ok(CARD.includes('budgetDocumentName('), 'the name comes from the shared rule')
  assert.ok(CARD.includes('budgetDocumentPath('), 'the stored path is what is fetched')
  assert.ok(CARD.includes('LineItemBudgetPreviewModal'), 'View opens the in-page preview modal')
  assert.ok(CARD.includes('downloadUploadedFile('), 'Download saves the real document')
  assert.ok(CARD.includes('No Line-Item Budget uploaded'), 'the only empty state')
  assert.ok(
    CARD.includes('The Line-Item Budget document could not be'),
    'a retrieval failure is reported instead of being shown as empty',
  )
  assert.ok(
    CARD.includes('hasBudgetReference'),
    'a stored reference that cannot be named is an error, not an empty state',
  )
})

test('the card reads the same saved record as the proposal viewer', () => {
  assert.ok(CARD.includes('api.get(`/api/proposals/${id}`)'), 'loads GET /api/proposals/{id}')
  assert.ok(
    CARD.includes('fullProposal.value || proposal.value'),
    'the viewer record wins, the OVCAF response is the fallback',
  )
})

test('the OVCAF card opens the proposal details in a modal', () => {
  assert.ok(CARD.includes('View Proposal Details'), 'the button label')
  assert.ok(CARD.includes('OvcafProposalDetails'), 'the existing details viewer is embedded')
  assert.ok(CARD.includes('showProposalDetails'), 'the modal state drives it')
  assert.ok(CARD.includes(':proposal-id="proposal.id"'), 'and shows this proposal')
})

test('the card is labelled Project Leader and reads the form value, not the account', () => {
  assert.ok(CARD.includes('Project Leader'), 'the label')
  assert.ok(CARD.includes('p.projectLeader || p.proponentName'), 'the form value wins')
  assert.ok(!CARD.includes("proponent: p.proponentName || 'Unknown'"), 'never uses the account first')
  assert.ok(!CARD.includes('chip-label">Proponent'), 'the generic account label is gone')
})

test('the details page renders every saved section read-only through the shared registry', () => {
  assert.ok(DETAILS.includes("import ProposalFieldSection from '@/components/ProposalFieldSection.vue'"))
  assert.ok(DETAILS.includes('PROPOSAL_FIELD_SECTIONS'), 'the same registry as the proposal form')
  assert.ok(DETAILS.includes(':editable="false"'), 'OVCAF cannot edit the proposal')
  assert.ok(DETAILS.includes('@download="downloadAttachment"'), 'attachments stay downloadable')
  assert.ok(DETAILS.includes('api.get(`/api/proposals/${id}`)'), 'the complete saved record is loaded')
})

test('the details page shows the uploaded budget document and its empty state', () => {
  assert.ok(DETAILS.includes('In-line Item Budget'))
  assert.ok(DETAILS.includes('budgetDocumentName('))
  assert.ok(DETAILS.includes('LineItemBudgetPreviewModal'))
  assert.ok(DETAILS.includes('downloadUploadedFile('))
  assert.ok(DETAILS.includes('No Line-Item Budget uploaded'))
  assert.ok(DETAILS.includes('hasBudgetReference'), 'a dangling reference is an error, not empty')
  assert.ok(!DETAILS.includes('budgetBreakdown'), 'the legacy amount table is not used')
  assert.ok(!DETAILS.includes('Detailed Line-Item Budget (LIB)'), 'the old table is gone')
})

test('the details header reads the Project Leader and budget from the viewer record', () => {
  assert.ok(DETAILS.includes('(Project Leader)'), 'the label')
  assert.ok(DETAILS.includes('fullProposal.value || proposal.value'), 'same source as the viewer')
  assert.ok(DETAILS.includes('p.projectLeader || p.proponentName'), 'the form value wins')
  assert.ok(!DETAILS.includes('(Proponent)'), 'the generic account label is gone')
})

test('the details page still follows the route and downloads the official document', () => {
  assert.ok(/route\.(params|query)\.id/.test(DETAILS))
  assert.match(DETAILS, /watch\(/)
  assert.ok(DETAILS.includes('downloadProposalRecord('))
  assert.ok(DETAILS.includes('Could not prepare the proposal document'), 'a failure is reported')
})

test('the validation screen reviews the uploaded budget instead of the legacy table', () => {
  assert.ok(VALIDATE.includes('LINE-ITEM BUDGET'))
  assert.ok(VALIDATE.includes('budgetDocumentName('))
  assert.ok(VALIDATE.includes('hasBudgetReference'), 'a dangling reference is an error, not empty')
  assert.ok(VALIDATE.includes('No Line-Item Budget uploaded'))
  assert.ok(!VALIDATE.includes('v-for="(item, idx) in proposal.budget"'), 'no legacy amount rows')
  assert.ok(!VALIDATE.includes('No budget data available.'), 'the misleading empty table is gone')
})

test('the inbox lists the Project Leader and offers the details view', () => {
  assert.ok(INBOX.includes('Project Leader'), 'the column label')
  assert.ok(INBOX.includes('p.projectLeader || p.proponentName'), 'the form value wins')
  assert.ok(INBOX.includes('View Details'), 'the row opens the proposal details page')
})
