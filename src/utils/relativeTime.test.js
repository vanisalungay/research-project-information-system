/**
 * relativeTime.test.js
 * Human-readable notification ages, including the calendar-day boundary and the
 * time-zone handling that keeps "Yesterday" honest for a browser outside Asia/Manila.
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  formatAbsoluteTime,
  formatRelativeTime,
  toNotificationDate,
} from './relativeTime.js'

const NOW = new Date('2026-11-18T12:00:00+08:00')
const at = (iso) => formatRelativeTime(iso, NOW)

test('an unusable timestamp renders nothing', () => {
  for (const value of [null, undefined, '', 'not-a-date', {}, []]) {
    assert.equal(formatRelativeTime(value, NOW), '')
  }
  assert.equal(formatRelativeTime('2026-11-18T12:00:00+08:00', null), '')
})

test('a fresh notification is "Just now"', () => {
  assert.equal(at('2026-11-18T11:59:50+08:00'), 'Just now')
  assert.equal(at('2026-11-18T12:00:00+08:00'), 'Just now')
  // A small clock skew in the server direction is still "Just now".
  assert.equal(at('2026-11-18T12:00:30+08:00'), 'Just now')
})

test('minutes use singular and plural grammar', () => {
  assert.equal(at('2026-11-18T11:59:00+08:00'), '1 minute ago')
  assert.equal(at('2026-11-18T11:58:00+08:00'), '2 minutes ago')
  assert.equal(at('2026-11-18T11:05:00+08:00'), '55 minutes ago')
})

test('hours use singular and plural grammar', () => {
  assert.equal(at('2026-11-18T11:00:00+08:00'), '1 hour ago')
  assert.equal(at('2026-11-18T09:00:00+08:00'), '3 hours ago')
  assert.equal(at('2026-11-17T13:00:00+08:00'), '23 hours ago')
})

test('the previous calendar day is "Yesterday"', () => {
  assert.equal(at('2026-11-17T10:00:00+08:00'), 'Yesterday')
  assert.equal(at('2026-11-17T00:01:00+08:00'), 'Yesterday')
})

test('older notifications count days, weeks, months and years', () => {
  assert.equal(at('2026-11-16T10:00:00+08:00'), '2 days ago')
  assert.equal(at('2026-11-12T10:00:00+08:00'), '6 days ago')
  assert.equal(at('2026-11-08T10:00:00+08:00'), '1 week ago')
  assert.equal(at('2026-11-01T10:00:00+08:00'), '2 weeks ago')
  assert.equal(at('2026-10-01T10:00:00+08:00'), '1 month ago')
  assert.equal(at('2026-08-01T10:00:00+08:00'), '3 months ago')
  assert.equal(at('2024-11-18T10:00:00+08:00'), '2 years ago')
})

test('the day boundary is compared in the display zone, not the browser zone', () => {
  // 00:30 on the 18th and 23:30 on the 17th are one hour apart but different days.
  const justAfterMidnight = new Date('2026-11-18T00:30:00+08:00')
  assert.equal(
    formatRelativeTime('2026-11-17T23:30:00+08:00', justAfterMidnight),
    '1 hour ago',
    'less than a day is reported in hours, even across midnight',
  )
  assert.equal(
    formatRelativeTime('2026-11-17T00:00:00+08:00', justAfterMidnight),
    'Yesterday',
    'more than a day and the previous calendar day is Yesterday',
  )
})

test('a zone-less historical value is read as Asia/Manila', () => {
  const parsed = toNotificationDate('2026-11-18T11:00:00')
  assert.equal(parsed.toISOString(), '2026-11-18T03:00:00.000Z')
  assert.equal(formatRelativeTime('2026-11-18T11:00:00', NOW), '1 hour ago')
})

test('an explicit offset is respected', () => {
  // 01:00 UTC is 09:00 in Manila, three hours before the 12:00 Manila "now".
  assert.equal(formatRelativeTime('2026-11-18T01:00:00Z', NOW), '3 hours ago')
})

test('historical wall-clock values keep their intended +08 date', () => {
  // These are the shape and values of existing rpis_db rows (2026-07-05 and
  // 2026-11-18), serialized with the configured offset.
  assert.equal(
    formatRelativeTime('2026-07-05T08:15:00+08:00', NOW),
    '4 months ago',
  )
  assert.equal(
    formatRelativeTime('2026-11-17T22:08:00+08:00', NOW),
    '13 hours ago',
  )
})

test('the absolute hover time is a readable date and time', () => {
  const label = formatAbsoluteTime('2026-11-18T14:07:00+08:00')
  assert.match(label, /Nov 18, 2026/)
  assert.match(label, /2:07\s?PM/i)
})
