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
    'PROPOSAL_ENDORSED',
  ]) {
    assert.equal(notificationKind(n(1, type)), PROPOSAL_KIND, `${type} is a proposal update`)
  }

  assert.equal(notificationKind(n(2, 'PROPOSAL_ANNOUNCEMENT')), 'call')
  assert.equal(notificationKind(n(3, 'ENDORSEMENT')), 'endorsement')
  assert.equal(notificationKind(n(3, 'ENDORSEMENT'), 'DEAN'), 'endorsement')
  assert.equal(notificationKind(n(3, 'ENDORSEMENT'), 'PROPONENT'), 'endorsement')
  assert.equal(
    notificationKind(n(3, 'ENDORSEMENT'), 'OVCRIGE'),
    PROPOSAL_KIND,
    'a historical ENDORSEMENT row addressed to OVCRIGE was a proposal notification',
  )
  assert.equal(notificationKind(n(4, 'DEAN_ASSIGNMENT')), 'other')
  assert.equal(notificationKind(n(4, 'QUARTERLY_REPORT')), PROPOSAL_KIND)
  assert.equal(notificationKind(n(5, null)), 'other')
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

test('the page renders the proposal updates expanded, with no collapse control', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  assert.ok(page.includes('groupNotifications'), 'the page must use the shared split')
  assert.ok(page.includes('Proposal Updates'), 'the group must be labelled')
  assert.ok(
    page.includes('v-for="group in notificationGroups"'),
    'the page must render the groups, not one flat list',
  )
  assert.ok(
    page.includes('<div class="notifications-list">'),
    'the notification list must always be rendered, never hidden behind a toggle',
  )

  // The old compact presentation: a collapsed group the reader had to open.
  for (const removed of [
    'showProposalUpdates',
    'isGroupOpen',
    'toggleGroup',
    'collapsible',
    'group-chevron',
    'v-show=',
    'Show more',
    'Read more',
  ]) {
    assert.ok(!page.includes(removed), `${removed} must not hide notification content`)
  }
})

test('each proposal update shows the full message and its record details', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  assert.match(page, /\{\{ notification\.message \}\}/, 'the complete message is rendered')
  assert.match(
    page,
    /notification-proposal[\s\S]{0,220}notification\.proposalDocumentId/,
    'the linked proposal\'s formatted Proposal ID is shown',
  )
  assert.ok(
    !page.includes('Proposal #') && !page.includes('notification.proposalId'),
    'the database primary key must never be displayed as the Proposal ID',
  )
  assert.match(
    page,
    /<RelativeTime[^>]*:value="notification\.createdAt"/,
    'the notification time is rendered by the shared relative-time component',
  )
  assert.match(
    page,
    /\.notification-message\s*\{[^}]*white-space:\s*pre-line[^}]*overflow-wrap:\s*anywhere/,
    'long messages wrap onto multiple lines instead of being clipped',
  )
  assert.ok(
    !/line-clamp|text-overflow:\s*ellipsis/.test(page),
    'notification content is never truncated with a clamp or ellipsis',
  )
})

test('the OVCRIGE feed is one list, without a redundant Proposal Updates heading', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  // Every notification OVCRIGE receives is a proposal update, so a heading above the only
  // group adds nothing. The role shares the Proponent's single untitled group branch.
  assert.match(
    page,
    /role === 'PROPONENT' \|\| role === 'OVCRIGE'/,
    'OVCRIGE must use the single, untitled group',
  )
  assert.match(
    page,
    /\{ key: 'all', title: '', items: filteredNotifications\.value \}/,
    'the single group keeps the newest-first filtered list',
  )
})

test('the Proposal updates kind is offered to every role', () => {
  const page = read('../views/Notifications/NotificationPage.vue')

  assert.match(
    page,
    /<option value="proposal">Proposal updates<\/option>/,
    'every role can now receive proposal-workflow notifications',
  )
  assert.ok(
    !page.includes('showProposalKind'),
    'the role-specific exclusion must be gone',
  )
})

test('the backend delivers proposal-workflow rows to the RPS Admin too', () => {
  const service = read('../../backend/src/main/java/com/rpis/backend/service/NotificationService.java')
  const types = read('../../backend/src/main/java/com/rpis/backend/util/NotificationTypes.java')

  for (const type of [
    'PROPOSAL_UPDATE',
    'REVIEW_REQUEST',
    'APPROVAL',
    'REVISION',
    'REJECTION',
    'FUNDING',
    'PROPOSAL_ENDORSED',
    'PROPOSAL_SUBMISSION',
  ]) {
    assert.ok(types.includes(`"${type}"`), `${type} is part of the shared workflow vocabulary`)
  }

  assert.ok(
    !service.includes('hiddenFrom'),
    'the RPS Admin must see the workflow notifications written for that office',
  )
  assert.ok(
    !service.includes('notificationRepository.delete'),
    'records must never be deleted to change what a role sees',
  )
})

