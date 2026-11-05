/**
 * notificationGroups.test.js
 * ---------------------------------------------------------------------------
 * Guards the notification list split (src/utils/notificationGroups.js).
 *
 * The page groups the proposal updates under their own heading, so the rule must never lose
 * a record and must never reorder the list. The rules are pure functions, verified without
 * a browser.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import { PROPOSAL_KIND, groupNotifications, notificationKind } from './notificationGroups.js'

const n = (id, type) => ({ id, type, message: `m${id}` })

test('the proposal workflow types are one kind, and the rest are not', () => {
  for (const type of [
    'PROPOSAL_UPDATE',
    'REVIEW_REQUEST',
    'APPROVAL',
    'REVISION',
    'REJECTION',
    'FUNDING',
  ]) {
    assert.equal(notificationKind(n(1, type)), PROPOSAL_KIND, `${type} is a proposal update`)
  }

  assert.equal(notificationKind(n(2, 'PROPOSAL_ANNOUNCEMENT')), 'call')
  assert.equal(notificationKind(n(3, 'ENDORSEMENT')), 'endorsement')
  assert.equal(notificationKind(n(4, null)), 'other')
  assert.equal(notificationKind(null), 'other')
})

test('the split keeps every record, in order, and drops nothing', () => {
  const list = [
    n(1, 'PROPOSAL_UPDATE'),
    n(2, 'PROPOSAL_ANNOUNCEMENT'),
    n(3, 'REVIEW_REQUEST'),
    n(4, 'ENDORSEMENT'),
    n(5, null),
  ]

  const { proposalUpdates, others } = groupNotifications(list)

  assert.deepEqual(proposalUpdates.map((r) => r.id), [1, 3], 'proposal updates, newest first')
  assert.deepEqual(others.map((r) => r.id), [2, 4, 5], 'the rest, newest first')
  assert.equal(
    proposalUpdates.length + others.length,
    list.length,
    'no notification may be dropped',
  )
})

test('the legitimate RPS Admin kinds stay out of the proposal group', () => {
  // Calls for Proposals, endorsements and administrative notifications are what an RPS
  // Admin keeps, so they must never be swept into the proposal updates group.
  const { proposalUpdates, others } = groupNotifications([
    n(1, 'PROPOSAL_ANNOUNCEMENT'),
    n(2, 'ENDORSEMENT'),
    n(3, null),
  ])
  assert.equal(proposalUpdates.length, 0)
  assert.deepEqual(others.map((r) => r.id), [1, 2, 3])
})

test('a missing or invalid list yields two empty groups', () => {
  assert.deepEqual(groupNotifications(null), { proposalUpdates: [], others: [] })
  assert.deepEqual(groupNotifications(undefined), { proposalUpdates: [], others: [] })
  assert.deepEqual(groupNotifications([]), { proposalUpdates: [], others: [] })
})

// ---------------------------------------------------------------------------
// The wiring, read as text - the technique navigation.test.js and the dashboard
// guard tests already use, so this needs no browser and no build.
// ---------------------------------------------------------------------------

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

test('the page renders the two groups and collapses the proposal updates', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  assert.ok(page.includes('groupNotifications'), 'the page must use the shared split')
  assert.ok(page.includes('Proposal Updates'), 'the group must be labelled')
  assert.ok(
    page.includes('v-for="group in notificationGroups"'),
    'the page must render the groups, not one flat list',
  )
  // Collapsed until opened, and opened by itself while its kind filter is selected.
  assert.match(
    page,
    /showProposalUpdates\.value \|\| filterKind\.value === 'proposal'/,
    'the group must auto-expand with the Proposal updates filter',
  )
})

test('the Proposal updates kind is not offered to the RPS Admin', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  assert.match(
    page,
    /<option v-if="showProposalKind" value="proposal">/,
    'the kind option must be hidden for a role that never receives proposal updates',
  )
  assert.match(
    page,
    /userStore\.user\?\.role !== UserRole\.RPS_ADMIN/,
    'the rule must be keyed on the RPS Admin role',
  )
})

test('the backend is the authority: an RPS Admin never receives proposal-workflow types', () => {
  const service = read('../../backend/src/main/java/com/rpis/backend/service/NotificationService.java')

  for (const type of [
    'PROPOSAL_UPDATE',
    'REVIEW_REQUEST',
    'APPROVAL',
    'REVISION',
    'REJECTION',
    'FUNDING',
  ]) {
    assert.ok(service.includes(`"${type}"`), `${type} must be excluded for the RPS Admin`)
  }

  assert.ok(service.includes('UserRoles.RPS_ADMIN'), 'the rule must be keyed on the RPS Admin role')
  // Both the list and "mark all as read" must apply the same rule, so a hidden notification
  // is never silently marked read.
  assert.ok(
    service.includes('hiddenFrom(user, notification)'),
    'the rule must be applied to the list and to mark-all-as-read',
  )
  assert.ok(
    !service.includes('notificationRepository.delete'),
    'records must never be deleted - only hidden from the one role',
  )
})

