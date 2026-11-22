/**
 * recAssignedProposal.test.js
 * ---------------------------------------------------------------------------
 * Guards the REC Assigned Proposals worklist UI while the pinned action contract
 * (REVIEW/VIEW labels, handlers and destinations) stays exactly as it was.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Rec/RecAssignedProposal.vue')

test('the page uses the shared list/table design layer', () => {
  assert.ok(
    PAGE.includes("import '@/assets/endorsement.css'"),
    'the page opts into the shared endorsement/list stylesheet',
  )
  for (const className of ['end-table-scroll', 'end-table', 'end-badge', 'end-btn is-view']) {
    assert.ok(PAGE.includes(className), `the table uses ${className}`)
  }
})

test('the worklist shows the proposal ID, a readable title and the submission date', () => {
  assert.ok(PAGE.includes('<th>Proposal ID</th>'), 'Proposal ID column')
  assert.ok(PAGE.includes("proposal.documentId || proposal.proposalCode || '—'"))
  assert.ok(PAGE.includes('title-text'), 'the title has a clamped text span')
  assert.ok(PAGE.includes(':title="proposal.projectTitle || proposal.title"'))
  assert.ok(PAGE.includes('proposal?.submittedAt || proposal?.createdAt'), 'real submission event')
  assert.ok(PAGE.includes('proposalStatusLabel(proposal.status)'))
  assert.ok(PAGE.includes('statusTone(proposal.status)'))
})

test('the category filter includes REC Revision', () => {
  assert.ok(PAGE.includes("value=\"REC_REVISION\""))
  assert.ok(PAGE.includes("proposalStatusLabel('REC_REVISION')"))
})

test('the worklist paginates without re-sorting the server result', () => {
  assert.ok(PAGE.includes('const pageSize = ref(10)'))
  assert.ok(PAGE.includes('pagedProposals'))
  assert.ok(PAGE.includes('<PaginationBar'))
  assert.ok(PAGE.includes('@update:page="goToPage"'))
  assert.ok(!PAGE.includes('.sort('), 'the server order must not be re-sorted')
})

test('the pinned REVIEW/VIEW labels, handlers and destinations are unchanged', () => {
  assert.match(PAGE, />\s*REVIEW\s*</)
  assert.match(PAGE, />\s*VIEW\s*</)
  assert.ok(PAGE.includes('@click="goToReview(proposal.id)"'))
  assert.ok(PAGE.includes('@click="goToView(proposal.id)"'))
  assert.ok(PAGE.includes("name: 'RecReviewForm', params: { id }"))
  assert.ok(PAGE.includes("name: 'RecMeetingDetails', params: { id }"))
})
