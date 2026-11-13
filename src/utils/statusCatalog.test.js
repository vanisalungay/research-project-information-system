/**
 * statusCatalog.test.js
 * ---------------------------------------------------------------------------
 * Pins the canonical status vocabulary of RPIS.
 *
 * Stored values never change: these tests read the raw codes the API and the
 * database use and assert only the text rendered on screen. The critical rules
 * covered here are the ones a single global label map could never express:
 *
 *   1. FOR REVIEW and legacy SUBMITTED both read "For RPS Review".
 *   2. ENDORSED reads "Endorsed to OVCRIGE" (RPS forwarding, not the Dean).
 *   3. UNDER_REVIEW reads "Under Review by REC".
 *   4. APPROVED reads "Implementation Approved".
 *   5. COMPLETED reads "Completed".
 *   6. APPROVED and COMPLETED are never treated as equivalent.
 *   7. The OVCAF decision APPROVED_ENDORSED reads "Approved" in its own context.
 *   8. That OVCAF "Approved" is not the proposal's "Implementation Approved".
 *   9. RETURNED_TO_RPS, RPS_RETURNED and RETURNED stay distinct and each keeps the
 *      label of the office/person the proposal is returned to.
 *  10. QPR/Terminal Report statuses are separate from proposal statuses.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ACCOUNT_STATUS_LABELS,
  ACTIVITY_STATUS_LABELS,
  ANNOUNCEMENT_STATUS_LABELS,
  CERTIFICATION_STATUS_LABELS,
  DEAN_ASSIGNMENT_STATUS_LABELS,
  ENDORSEMENT_STATUS_LABELS,
  OVCAF_DECISION_LABELS,
  PRIORITY_AGENDA_STATUS_LABELS,
  PROPOSAL_STATUS_LABELS,
  PROPOSAL_TIMELINE_STATUS_LABELS,
  REPORT_STATUS_LABELS,
  REVIEW_DECISION_LABELS,
  SECTION_REMARK_STATUS_LABELS,
  accountStatusLabel,
  activityStatusLabel,
  certificationStatusLabel,
  deanAssignmentStatusLabel,
  normalizeStatusKey,
  ovcafDecisionLabel,
  priorityAgendaStatusLabel,
  reportStatusLabel,
  reviewDecisionLabel,
  sectionRemarkStatusLabel,
} from './statusCatalog.js'
import {
  isCompletedStatus,
  proposalStatusLabel,
  proposalStatusMatches,
} from './proposalStatus.js'
import { announcementStatus } from './announcementStatus.js'
import { endorsementStatusLabel } from './endorsement.js'

test('1. both submission spellings read "For RPS Review"', () => {
  assert.equal(proposalStatusLabel('FOR REVIEW'), 'For RPS Review')
  assert.equal(proposalStatusLabel('FOR_REVIEW'), 'For RPS Review')
  assert.equal(proposalStatusLabel('for review'), 'For RPS Review')
  assert.equal(proposalStatusLabel('SUBMITTED'), 'For RPS Review')
  assert.equal(proposalStatusLabel('submitted'), 'For RPS Review')
  assert.equal(PROPOSAL_STATUS_LABELS.FOR_REVIEW, PROPOSAL_STATUS_LABELS.SUBMITTED)
})

test('2. ENDORSED reads "Endorsed to OVCRIGE"', () => {
  // The stored ENDORSED value is written by RPS's endorsement action
  // (PUT /api/proposals/{id}/endorse); the backend notifies OVCRIGE that the proposal
  // "has been endorsed to OVCRIGE". The Dean's endorsement is the earlier
  // endorsement-request stage and has its own status map.
  assert.equal(proposalStatusLabel('ENDORSED'), 'Endorsed to OVCRIGE')
  assert.notEqual(proposalStatusLabel('ENDORSED'), 'Endorsed by the Dean')
  assert.notEqual(proposalStatusLabel('ENDORSED'), proposalStatusLabel('OVC_APPROVED'))
  assert.equal(proposalStatusLabel('OVC_APPROVED'), 'Approved by OVCRIGE')
  assert.equal(ENDORSEMENT_STATUS_LABELS.APPROVED, 'Approved')
})

test('3. UNDER_REVIEW reads "Under Review by REC"', () => {
  assert.equal(proposalStatusLabel('UNDER_REVIEW'), 'Under Review by REC')
})

test('4-5. APPROVED and COMPLETED carry their own canonical labels', () => {
  assert.equal(proposalStatusLabel('APPROVED'), 'Implementation Approved')
  assert.equal(proposalStatusLabel('COMPLETED'), 'Completed')
  assert.notEqual(
    proposalStatusLabel('APPROVED'),
    proposalStatusLabel('COMPLETED'),
    'APPROVED must never be displayed as Completed',
  )
})

test('6. APPROVED and COMPLETED are never equivalent', () => {
  assert.equal(proposalStatusMatches('APPROVED', 'COMPLETED'), false)
  assert.equal(proposalStatusMatches('COMPLETED', 'APPROVED'), false)
  assert.equal(proposalStatusMatches('APPROVED', 'APPROVED'), true)
  assert.equal(proposalStatusMatches('COMPLETED', 'COMPLETED'), true)
  assert.equal(isCompletedStatus('APPROVED'), false)
  assert.equal(isCompletedStatus('COMPLETED'), true)
})

test('7-8. the OVCAF decision has its own labels, separate from proposal statuses', () => {
  assert.equal(ovcafDecisionLabel('APPROVED_ENDORSED'), 'Budget Approved')
  assert.equal(ovcafDecisionLabel('RETURNED_FOR_REVISION'), 'Returned for Revision')
  assert.equal(ovcafDecisionLabel('REJECTED'), 'Rejected')

  // The proposal label of APPROVED is a different word from the OVCAF decision label.
  assert.notEqual(PROPOSAL_STATUS_LABELS.APPROVED, OVCAF_DECISION_LABELS.APPROVED_ENDORSED)
  assert.equal(proposalStatusLabel('APPROVED'), 'Implementation Approved')

  // Decision values are not proposal statuses, so the proposal map must not claim them.
  assert.equal(PROPOSAL_STATUS_LABELS.APPROVED_ENDORSED, undefined)
  assert.equal(PROPOSAL_STATUS_LABELS.RETURNED_FOR_REVISION, undefined)
})

test('9. each return direction keeps its own label', () => {
  // A reviewing office returns the proposal to RPS; RPS acts next.
  assert.equal(proposalStatusLabel('RETURNED_TO_RPS'), 'Returned to RPS')
  // RPS returns / forwards the proposal to the Proponent, who must revise and resubmit.
  assert.equal(proposalStatusLabel('RPS_RETURNED'), 'Returned to Proponent')
  // OVCAF's budget return keeps its own wording.
  assert.equal(proposalStatusLabel('RETURNED'), 'Returned for Revision')
  // The directions are never merged: different stored values, different labels.
  assert.notEqual(
    proposalStatusLabel('RETURNED_TO_RPS'),
    proposalStatusLabel('RPS_RETURNED'),
    'return to RPS and return to the Proponent are opposite directions',
  )
  assert.notEqual(proposalStatusLabel('RETURNED'), proposalStatusLabel('RETURNED_TO_RPS'))
  assert.equal(proposalStatusMatches('RETURNED_TO_RPS', 'RPS_RETURNED'), false)
  assert.equal(proposalStatusMatches('RPS_RETURNED', 'RETURNED'), false)
})

test('10. QPR/Terminal Report statuses are separate from proposal statuses', () => {
  assert.equal(reportStatusLabel('DRAFT'), 'Draft')
  assert.equal(reportStatusLabel('SUBMITTED'), 'Submitted')
  // SUBMITTED is a report status meaning "Submitted" and a proposal status meaning
  // "For RPS Review"; the two maps are deliberately different.
  assert.equal(proposalStatusLabel('SUBMITTED'), 'For RPS Review')
  assert.notEqual(REPORT_STATUS_LABELS.SUBMITTED, PROPOSAL_STATUS_LABELS.SUBMITTED)
  // Report statuses do not have a COMPLETED value; that is a proposal status only.
  assert.equal(REPORT_STATUS_LABELS.COMPLETED, undefined)
})

test('account statuses: APPROVED means an active account', () => {
  assert.equal(accountStatusLabel('PENDING'), 'Pending')
  assert.equal(accountStatusLabel('APPROVED'), 'Approved')
  assert.equal(accountStatusLabel('REJECTED'), 'Rejected')
  assert.equal(accountStatusLabel('INACTIVE'), 'Inactive')
  assert.equal(accountStatusLabel('ACTIVE'), '', 'ACTIVE is not an account status')
  assert.notEqual(accountStatusLabel('APPROVED'), PROPOSAL_STATUS_LABELS.APPROVED)
})

test('endorsement, certification, assignment and remark statuses', () => {
  assert.equal(endorsementStatusLabel('PENDING'), 'Pending')
  assert.equal(endorsementStatusLabel('APPROVED'), 'Approved')
  assert.equal(endorsementStatusLabel('REJECTED'), 'Rejected')

  assert.equal(certificationStatusLabel('ISSUED'), 'Issued')
  assert.equal(certificationStatusLabel('SUPERSEDED'), 'Superseded')

  assert.equal(deanAssignmentStatusLabel('ACTIVE'), 'Active')
  assert.equal(deanAssignmentStatusLabel('REVOKED'), 'Revoked')
  assert.equal(DEAN_ASSIGNMENT_STATUS_LABELS.ALL, undefined, '"All" is a filter, not a status')

  assert.equal(sectionRemarkStatusLabel('ACTIVE'), 'Active')
  assert.equal(sectionRemarkStatusLabel('SUPERSEDED'), 'Superseded')
})

test('announcements: stored ACTIVE is never displayed as "Active"', () => {
  assert.equal(ANNOUNCEMENT_STATUS_LABELS.ACTIVE, undefined)
  assert.equal(ANNOUNCEMENT_STATUS_LABELS.DRAFT, 'Draft')
  assert.equal(ANNOUNCEMENT_STATUS_LABELS.SCHEDULED, 'Scheduled')
  assert.equal(ANNOUNCEMENT_STATUS_LABELS.OPEN, 'Open')
  assert.equal(ANNOUNCEMENT_STATUS_LABELS.CLOSED, 'Closed')

  const today = '2026-03-10'
  assert.equal(
    announcementStatus({ status: 'ACTIVE', startDate: '2026-04-01', endDate: '2026-04-30' }, today),
    'Scheduled',
  )
  assert.equal(
    announcementStatus({ status: 'ACTIVE', startDate: '2026-03-01', endDate: '2026-03-31' }, today),
    'Open',
  )
  assert.equal(
    announcementStatus({ status: 'ACTIVE', startDate: '2026-01-01', endDate: '2026-02-01' }, today),
    'Closed',
  )
  assert.equal(announcementStatus({ status: 'DRAFT' }, today), 'Draft')
  assert.equal(announcementStatus({ status: 'CLOSED' }, today), 'Closed')
})

test('priority agendas are derived from the Boolean flag', () => {
  assert.equal(priorityAgendaStatusLabel(true), 'Active')
  assert.equal(priorityAgendaStatusLabel(false), 'Inactive')
  assert.deepEqual(Object.keys(PRIORITY_AGENDA_STATUS_LABELS).sort(), ['ACTIVE', 'INACTIVE'])
})

test('review decisions and activity results have their own vocabulary', () => {
  assert.equal(reviewDecisionLabel('APPROVED'), 'Approved')
  assert.equal(reviewDecisionLabel('REJECTED'), 'Rejected')
  assert.equal(reviewDecisionLabel('REVISION_REQUIRED'), 'Revision Required')
  assert.equal(reviewDecisionLabel('REVISION'), 'For Revision')
  assert.notEqual(REVIEW_DECISION_LABELS.APPROVED, PROPOSAL_STATUS_LABELS.APPROVED)

  assert.equal(activityStatusLabel('SUCCESS'), 'Success')
  assert.equal(activityStatusLabel('FAILED'), 'Failed')
  assert.deepEqual(Object.keys(ACTIVITY_STATUS_LABELS).sort(), ['FAILED', 'SUCCESS'])
})

test('timeline-only codes stay out of the canonical proposal map', () => {
  for (const code of [
    'RPS_REVIEW',
    'OVCRIGE_COORDINATION',
    'REC_EVALUATION',
    'CHANCELLOR_REVIEW',
    'FINANCE_AUTHORIZATION',
    'IMPLEMENTATION',
    'FOR_IMPLEMENTATION',
    'REC_REJECTED',
    'REVISION',
    'REC_REVISION',
    'PENDING_REVISION',
    'PENDING',
  ]) {
    assert.equal(PROPOSAL_STATUS_LABELS[code], undefined, `${code} is not a canonical status`)
    assert.ok(PROPOSAL_TIMELINE_STATUS_LABELS[code], `${code} keeps its historical label`)
  }
})

test('unknown and missing values fall back safely', () => {
  assert.equal(normalizeStatusKey('  for review  '), 'FOR_REVIEW')
  assert.equal(normalizeStatusKey('for-review'), 'FOR_REVIEW')
  assert.equal(normalizeStatusKey(null), '')
  assert.equal(accountStatusLabel(''), '')
  assert.equal(accountStatusLabel(null), '')
  assert.equal(ovcafDecisionLabel('SOMETHING_ELSE'), '')
})
