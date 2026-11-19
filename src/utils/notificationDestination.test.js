/**
 * notificationDestination.test.js
 * Every notification type resolves to the page the signed-in role owns - or to null,
 * never to the Dashboard.
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  proposalDestinationForRole,
  resolveNotificationDestination,
} from './notificationDestination.js'

const PROPOSAL_ID = 42

test('each role opens its own proposal page', () => {
  const expected = {
    PROPONENT: { name: 'ProponentPropDetails', params: { id: PROPOSAL_ID } },
    RPS_ADMIN: { name: 'ProposalDetails', params: { id: PROPOSAL_ID } },
    RPS_STAFF: { name: 'ProposalDetails', params: { id: PROPOSAL_ID } },
    REC: { path: '/rec-prop', query: { id: PROPOSAL_ID } },
    OVCRIGE: { name: 'OvcReviewProposal', params: { id: PROPOSAL_ID } },
    OVCAF: { path: `/ovcaf/proposals/${PROPOSAL_ID}` },
    OC: { name: 'OCFinalApproval', params: { id: PROPOSAL_ID } },
  }
  for (const [role, destination] of Object.entries(expected)) {
    assert.deepEqual(proposalDestinationForRole(PROPOSAL_ID, role), destination, role)
  }
  assert.equal(proposalDestinationForRole(PROPOSAL_ID, 'DEAN'), null)
})

test('every proposal-workflow type resolves for every role that has a proposal page', () => {
  for (const type of [
    'PROPOSAL_UPDATE',
    'REVIEW_REQUEST',
    'APPROVAL',
    'REVISION',
    'REJECTION',
    'FUNDING',
    'PROPOSAL_ENDORSED',
    'SOMETHING_NEW',
  ]) {
    for (const role of ['PROPONENT', 'RPS_ADMIN', 'RPS_STAFF', 'REC', 'OVCRIGE', 'OVCAF', 'OC']) {
      const destination = resolveNotificationDestination(
        { type, proposalId: PROPOSAL_ID },
        role,
      )
      assert.ok(destination, `${type} must resolve for ${role}`)
      assert.notEqual(destination.path, '/home')
      assert.notEqual(destination.path, '/dashboard')
    }
  }
})

test('an untyped legacy row with a proposal id still opens the proposal', () => {
  assert.deepEqual(
    resolveNotificationDestination({ type: null, proposalId: 9 }, 'PROPONENT'),
    { name: 'ProponentPropDetails', params: { id: 9 } },
  )
})

test('a Dean endorsement request opens the endorsement screens', () => {
  const notification = { type: 'ENDORSEMENT', proposalId: 7 }
  assert.deepEqual(resolveNotificationDestination(notification, 'DEAN'), {
    name: 'DeanEndorsementReview',
    params: { id: 7 },
  })
  assert.deepEqual(resolveNotificationDestination(notification, 'PROPONENT'), {
    name: 'ProponentEndorsementDetails',
    params: { id: 7 },
  })
  for (const role of ['RPS_ADMIN', 'RPS_STAFF', 'REC', 'OC']) {
    assert.equal(resolveNotificationDestination(notification, role), null)
  }
})

test('a historical ENDORSEMENT row for OVCRIGE/OVCAF opens the proposal', () => {
  const notification = { type: 'ENDORSEMENT', proposalId: 7 }
  assert.deepEqual(resolveNotificationDestination(notification, 'OVCRIGE'), {
    name: 'OvcReviewProposal',
    params: { id: 7 },
  })
  assert.deepEqual(resolveNotificationDestination(notification, 'OVCAF'), {
    path: '/ovcaf/proposals/7',
  })
})

test('an endorsement notification without a request opens the endorsement list', () => {
  assert.deepEqual(
    resolveNotificationDestination({ type: 'ENDORSEMENT', proposalId: null }, 'DEAN'),
    { name: 'DeanEndorsementRequests' },
  )
  assert.deepEqual(
    resolveNotificationDestination({ type: 'ENDORSEMENT', proposalId: null }, 'PROPONENT'),
    { name: 'ProponentEndorsements' },
  )
})

test('a Call for Proposals notification opens the Proponent announcement', () => {
  const notification = { type: 'PROPOSAL_ANNOUNCEMENT', proposalAnnouncementId: 14 }
  assert.deepEqual(resolveNotificationDestination(notification, 'PROPONENT'), {
    name: 'ProponentAnnouncementDetails',
    params: { id: 14 },
  })
  for (const role of ['DEAN', 'RPS_ADMIN', 'OVCRIGE', 'OVCAF', 'REC', 'OC']) {
    assert.equal(resolveNotificationDestination(notification, role), null)
  }
})

test('report notifications open the report page each role owns', () => {
  assert.deepEqual(
    resolveNotificationDestination({ type: 'QUARTERLY_REPORT', proposalId: 3 }, 'PROPONENT'),
    { name: 'ProponentQuarterlyReports', params: { proposalId: 3 } },
  )
  assert.deepEqual(
    resolveNotificationDestination({ type: 'QUARTERLY_REPORT', proposalId: 3 }, 'RPS_STAFF'),
    { name: 'RpsQuarterlyReports', query: { proposalId: 3 } },
  )
  assert.deepEqual(
    resolveNotificationDestination({ type: 'PROJECT_COMPLETION', proposalId: 3 }, 'OVCRIGE'),
    { name: 'OvcQuarterlyReports', query: { proposalId: 3 } },
  )
  assert.equal(
    resolveNotificationDestination({ type: 'QUARTERLY_REPORT', proposalId: 3 }, 'DEAN'),
    null,
  )
})

test('a Dean account notification opens the profile that shows the role', () => {
  assert.deepEqual(
    resolveNotificationDestination({ type: 'DEAN_ASSIGNMENT' }, 'DEAN'),
    { path: '/manage-profile' },
  )
})

test('a revision deadline reminder opens the proposal for the Proponent', () => {
  assert.deepEqual(
    resolveNotificationDestination(
      { type: 'REVISION_DEADLINE_REMINDER', proposalId: 12 },
      'PROPONENT',
    ),
    { name: 'ProponentPropDetails', params: { id: 12 } },
  )
})

test('a Special Order draft has no reachable detail page yet', () => {
  // OVCAF's detail service requires soUploadedAt, which a draft never sets; a null
  // destination makes the client show the honest "nothing to open" message.
  for (const role of ['OVCAF', 'OC', 'PROPONENT', 'RPS_ADMIN']) {
    assert.equal(
      resolveNotificationDestination({ type: 'SPECIAL_ORDER_DRAFT', proposalId: 12 }, role),
      null,
    )
  }
})

test('a missing reference never falls back to the Dashboard', () => {
  for (const notification of [
    { type: 'APPROVAL', proposalId: null },
    { type: 'REVISION', proposalId: 'abc' },
    { type: 'FUNDING', proposalId: 0 },
    {},
    null,
    undefined,
  ]) {
    assert.equal(resolveNotificationDestination(notification, 'PROPONENT'), null)
    assert.equal(resolveNotificationDestination(notification, 'RPS_STAFF'), null)
  }
})
