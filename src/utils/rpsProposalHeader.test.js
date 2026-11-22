/**
 * rpsProposalHeader.test.js
 * ---------------------------------------------------------------------------
 * The same proposal must present the same header however RPS Staff reach it. The review,
 * details and forward-revision pages all render the one ProposalReviewHeader component;
 * the legacy review-result viewer route now serves the canonical details page.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const DETAILS = read('../views/Rps/RpsProposalDetails.vue')
const REVIEW = read('../views/Rps/RpsProposalReview.vue')
const FORWARD = read('../views/Rps/RpsForwardRevision.vue')
const SUBMITTED = read('../views/Rps/RpsSubmittedProposalsPanel.vue')
const ROUTER = read('../router/index.js')
const HEADER = read('../components/ProposalReviewHeader.vue')

test('every RPS proposal page renders the one shared header', () => {
  for (const [name, source] of [
    ['Proposal Details', DETAILS],
    ['Proposal Review', REVIEW],
    ['Forward Revision', FORWARD],
  ]) {
    assert.ok(source.includes("import ProposalReviewHeader from '@/components/ProposalReviewHeader.vue'"),
      `${name} imports the shared header`)
    assert.ok(source.includes('<ProposalReviewHeader'), `${name} renders the shared header`)
  }
})

test('the review page no longer strips the shared header padding', () => {
  // A scoped `* { margin: 0; padding: 0 }` also matched the root element of the shared
  // header child component, so this page's header had 0 padding while every other entry
  // point had 20px 24px. Only the box model may be set broadly.
  assert.ok(
    !/\*\s*\{[^}]*padding:\s*0/.test(REVIEW),
    'no blanket padding reset may reach the shared header',
  )
  assert.match(REVIEW, /\.proposal-page,\s*\n\.proposal-page \* \{[^}]*box-sizing: border-box/)
})

test('the shared header keeps navigation, identification and actions in that order', () => {
  const nav = HEADER.indexOf('class="rv-header-nav"')
  const identification = HEADER.indexOf('class="rv-identification"')
  const actions = HEADER.indexOf('class="rv-header-actions"')
  assert.ok(nav > -1 && nav < identification && identification < actions,
    'Back, then the proposal information, then the actions')
  assert.ok(HEADER.includes(':title="title || \'Proposal Details\'"') ||
    HEADER.includes("title || 'Proposal Details'"))
  assert.ok(HEADER.includes(':document-id="documentId"') ||
    HEADER.indexOf('{{ documentId }}') > -1)
  assert.ok(HEADER.includes(':status="status"') || HEADER.includes('{{ statusLabel }}'))
})

test('a proposal back with the proponent opens the canonical details page', () => {
  const revisionBranch = SUBMITTED.slice(
    SUBMITTED.indexOf("proposal.status === 'REVISION'"),
    SUBMITTED.indexOf("proposal.status === 'REVISION'") + 700,
  )
  assert.ok(
    revisionBranch.includes("{ name: 'ProposalDetails', params: { id: proposal.id } }"),
    'Revision/RPS_RETURNED goes to the shared details page, not a separate header',
  )
  assert.ok(!revisionBranch.includes('ReviewResultViewer'))
})

test('the legacy review-viewer route serves the canonical details page', () => {
  assert.match(
    ROUTER,
    /path: 'review-viewer\/:id', name: 'ReviewResultViewer', component: RpsProposalDetails/,
  )
})

test('notification navigation is unchanged for RPS roles', () => {
  const DESTINATION = read('./notificationDestination.js')
  assert.match(
    DESTINATION,
    /case ROLES\.RPS_ADMIN:\s*case ROLES\.RPS_STAFF:\s*return \{ name: 'ProposalDetails', params: \{ id \} \}/,
  )
})
