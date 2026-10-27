/**
 * announcementStatus.test.js
 * Verifies the status label and colour tone shown next to a Call for Proposal: Open is
 * green, Closed is red and Scheduled is blue, and the label is always available so a
 * status never depends on colour alone.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  announcementStatus,
  announcementStatusLabel,
  announcementStatusTone,
} from './announcementStatus.js'

test('the status codes a notification carries map to a label and a tone', () => {
  assert.equal(announcementStatusLabel('OPEN'), 'Open')
  assert.equal(announcementStatusTone('OPEN'), 'open')

  assert.equal(announcementStatusLabel('CLOSED'), 'Closed')
  assert.equal(announcementStatusTone('CLOSED'), 'closed')

  assert.equal(announcementStatusLabel('SCHEDULED'), 'Scheduled')
  assert.equal(announcementStatusTone('SCHEDULED'), 'scheduled')

  assert.equal(announcementStatusLabel('DRAFT'), 'Draft')
  assert.equal(announcementStatusTone('DRAFT'), 'draft')
})

test('the labels the status helper returns map to the same badge', () => {
  const call = { status: 'ACTIVE', startDate: '2026-10-01', endDate: '2026-11-15' }
  const label = announcementStatus(call, '2026-10-10')

  assert.equal(label, 'Open')
  assert.equal(announcementStatusLabel(label), 'Open')
  assert.equal(announcementStatusTone(label), 'open')
})

test('open is green, closed is red and scheduled is blue', () => {
  assert.deepEqual(
    [
      announcementStatusTone(
        announcementStatus(
          { status: 'ACTIVE', startDate: '2026-10-01', endDate: '2026-11-15' },
          '2026-10-10',
        ),
      ),
      announcementStatusTone(
        announcementStatus(
          { status: 'CLOSED', startDate: '2026-10-01', endDate: '2026-11-15' },
          '2026-10-10',
        ),
      ),
      announcementStatusTone(
        announcementStatus(
          { status: 'ACTIVE', startDate: '2026-12-01', endDate: '2026-12-20' },
          '2026-10-10',
        ),
      ),
    ],
    ['open', 'closed', 'scheduled'],
  )
})

test('an unknown or missing status has no label and no tone', () => {
  assert.equal(announcementStatusLabel(null), '')
  assert.equal(announcementStatusLabel(undefined), '')
  assert.equal(announcementStatusLabel(''), '')
  assert.equal(announcementStatusTone(null), '')
  assert.equal(announcementStatusTone('PENDING'), '')
})
