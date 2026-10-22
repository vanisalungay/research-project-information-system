/**
 * notificationTarget.test.js
 * Verifies that a notification resolves to the exact Call for Proposals
 * announcement saved with it, and that notifications without an announcement
 * reference have no destination (they only change their read state).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { ANNOUNCEMENT_DETAILS_ROUTE, announcementIdFromNotification } from './notificationTarget.js'

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
