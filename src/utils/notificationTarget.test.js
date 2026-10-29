/**
 * notificationTarget.test.js
 * Verifies that a notification resolves to the exact record saved with it: the Call for
 * Proposals announcement for an announcement notification, and the endorsement request for
 * an endorsement notification - which must never be routed to the announcement page, since
 * a Dean is not authorized to open a Proponent's announcement. A notification without a
 * usable reference has no destination (it only changes its read state).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ANNOUNCEMENT_DETAILS_ROUTE,
  ENDORSEMENT_LIST_ROUTE,
  ENDORSEMENT_NOTIFICATION_TYPE,
  ENDORSEMENT_REVIEW_ROUTE,
  PROPONENT_ENDORSEMENT_DETAILS_ROUTE,
  PROPONENT_ENDORSEMENTS_ROUTE,
  PROPONENT_ROLE,
  announcementIdFromNotification,
  endorsementRequestIdFromNotification,
  isEndorsementNotification,
  notificationDestination,
} from './notificationTarget.js'

test('an announcement notification resolves to its own announcement id', () => {
  assert.equal(
    announcementIdFromNotification({ id: 5, type: 'PROPOSAL_ANNOUNCEMENT', proposalAnnouncementId: 14 }),
    14
  )
  assert.equal(
    announcementIdFromNotification({ type: 'PROPOSAL_ANNOUNCEMENT', proposalAnnouncementId: 1 }),
    1
  )
})

test('different announcement notifications resolve to different announcements', () => {
  const first = announcementIdFromNotification({ proposalAnnouncementId: 4 })
  const second = announcementIdFromNotification({ proposalAnnouncementId: 9 })

  assert.equal(first, 4)
  assert.equal(second, 9)
  assert.notEqual(first, second)
})

test('notifications without an announcement reference have no destination', () => {
  assert.equal(announcementIdFromNotification({ id: 1, type: 'REVISION', proposalId: 42 }), null)
  assert.equal(announcementIdFromNotification({ id: 2, type: 'PROPOSAL_ANNOUNCEMENT' }), null)
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: null }), null)
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: '' }), null)
  assert.equal(announcementIdFromNotification({}), null)
  assert.equal(announcementIdFromNotification(null), null)
  assert.equal(announcementIdFromNotification(undefined), null)
})

test('an unusable announcement reference never produces a destination', () => {
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: 0 }), null)
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: -3 }), null)
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: 'abc' }), null)
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: 1.5 }), null)
})

test('a numeric string reference is accepted (JSON ids stay usable)', () => {
  assert.equal(announcementIdFromNotification({ proposalAnnouncementId: '14' }), 14)
})

test('the announcement details route name is the Proponent view', () => {
  assert.equal(ANNOUNCEMENT_DETAILS_ROUTE, 'ProponentAnnouncementDetails')
})

// ===================== ENDORSEMENT NOTIFICATIONS =====================

test('an endorsement notification resolves to the endorsement request it is about', () => {
  const notification = {
    id: 7,
    type: ENDORSEMENT_NOTIFICATION_TYPE,
    proposalId: 42,
    proposalAnnouncementId: 14,
  }

  assert.equal(isEndorsementNotification(notification), true)
  assert.equal(endorsementRequestIdFromNotification(notification), 42)
  // A numeric string id is accepted here too.
  assert.equal(
    endorsementRequestIdFromNotification({ type: ENDORSEMENT_NOTIFICATION_TYPE, proposalId: '42' }),
    42,
  )
  // Only an endorsement notification carries an endorsement request.
  assert.equal(endorsementRequestIdFromNotification({ type: 'APPROVAL', proposalId: 42 }), null)
  assert.equal(endorsementRequestIdFromNotification({ proposalId: 42 }), null)
  assert.equal(endorsementRequestIdFromNotification(null), null)
})

test('an endorsement notification is never treated as an announcement notification', () => {
  // The record keeps the request's Call for Proposals for the card; it must never become
  // the destination of the click (a Dean cannot open a Proponent's announcement page).
  const legacy = {
    id: 3,
    type: ENDORSEMENT_NOTIFICATION_TYPE,
    proposalId: null,
    proposalAnnouncementId: 14,
  }

  assert.equal(announcementIdFromNotification(legacy), null)
  assert.notEqual(notificationDestination(legacy, 'DEAN').name, ANNOUNCEMENT_DETAILS_ROUTE)
})

test('the Dean opens the endorsement request itself', () => {
  const notification = { type: ENDORSEMENT_NOTIFICATION_TYPE, proposalId: 42 }

  assert.deepEqual(notificationDestination(notification, 'DEAN'), {
    name: ENDORSEMENT_REVIEW_ROUTE,
    params: { id: 42 },
  })
  // Any other reviewing role that receives one is sent to the same review screen.
  assert.deepEqual(notificationDestination(notification, 'RPS_ADMIN'), {
    name: ENDORSEMENT_REVIEW_ROUTE,
    params: { id: 42 },
  })
})

test('the Proponent opens their own endorsement request', () => {
  const notification = { type: ENDORSEMENT_NOTIFICATION_TYPE, proposalId: 42 }

  assert.deepEqual(notificationDestination(notification, PROPONENT_ROLE), {
    name: PROPONENT_ENDORSEMENT_DETAILS_ROUTE,
    params: { id: 42 },
  })
})

test('an endorsement notification without a usable request opens the endorsement list', () => {
  // A notification raised before the request was carried has no record to open: the list
  // is the accurate destination instead of a page that cannot be resolved.
  for (const proposalId of [null, undefined, 0, -1, 'abc']) {
    const notification = { type: ENDORSEMENT_NOTIFICATION_TYPE, proposalId }

    assert.equal(endorsementRequestIdFromNotification(notification), null)
    assert.deepEqual(notificationDestination(notification, 'DEAN'), {
      name: ENDORSEMENT_LIST_ROUTE,
    })
    assert.deepEqual(notificationDestination(notification, PROPONENT_ROLE), {
      name: PROPONENT_ENDORSEMENTS_ROUTE,
    })
  }
})

test('an announcement notification still opens its announcement for every role', () => {
  const notification = {
    type: 'PROPOSAL_ANNOUNCEMENT',
    proposalId: null,
    proposalAnnouncementId: 14,
  }

  for (const role of [PROPONENT_ROLE, 'DEAN']) {
    assert.deepEqual(notificationDestination(notification, role), {
      name: ANNOUNCEMENT_DETAILS_ROUTE,
      params: { id: 14 },
    })
  }
})

test('a notification with no reference at all has no destination', () => {
  assert.equal(notificationDestination({ id: 1, type: 'REVISION', proposalId: null }, 'DEAN'), null)
  assert.equal(notificationDestination({ type: 'APPROVAL' }, PROPONENT_ROLE), null)
  assert.equal(notificationDestination({}, 'DEAN'), null)
  assert.equal(notificationDestination(null, 'DEAN'), null)
  assert.equal(notificationDestination(undefined, undefined), null)
})
