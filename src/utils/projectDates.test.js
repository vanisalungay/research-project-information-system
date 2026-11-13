import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  PROJECT_DURATION_OPTIONS,
  PROJECT_END_DATE_HINT,
  PROJECT_PERIOD_HINT,
  addMonthsIso,
  durationMonths,
  formatIsoDate,
  parseIsoDate,
  projectEndDate,
  projectEndDateError,
  toIsoDate,
} from './projectDates.js'

describe('projectDates - ISO calendar dates', () => {
  it('reads and writes ISO dates', () => {
    assert.equal(toIsoDate(parseIsoDate('2026-12-01')), '2026-12-01')
    assert.equal(formatIsoDate('2026-12-01'), 'Dec 1, 2026')
  })

  it('rejects empty and impossible dates', () => {
    assert.equal(parseIsoDate(''), null)
    assert.equal(parseIsoDate(null), null)
    assert.equal(parseIsoDate('01/12/2026'), null)
    assert.equal(parseIsoDate('2026-02-31'), null, 'no roll-over into March')
    assert.equal(parseIsoDate('2026-13-01'), null)
    assert.equal(toIsoDate(null), '')
  })

  it('only accepts a positive number of months', () => {
    assert.equal(durationMonths('12'), 12)
    assert.equal(durationMonths(' 24 '), 24)
    assert.equal(durationMonths('6'), 6)
    assert.equal(durationMonths(''), null)
    assert.equal(durationMonths(null), null)
    assert.equal(durationMonths('0'), null)
    assert.equal(durationMonths('-3'), null)
    assert.equal(durationMonths('12 months'), null)
  })

  it('adds months keeping the day of the month', () => {
    assert.equal(addMonthsIso('2026-10-01', 12), '2027-10-01')
    assert.equal(addMonthsIso('2026-10-15', 12), '2027-10-15')
    assert.equal(addMonthsIso('2026-12-01', 12), '2027-12-01')
    assert.equal(addMonthsIso('2026-10-01', 6), '2027-04-01')
    assert.equal(addMonthsIso('2027-01-01', 18), '2028-07-01')
  })

  it('uses the last valid day when the target month is shorter', () => {
    assert.equal(addMonthsIso('2026-01-31', 1), '2026-02-28')
    assert.equal(addMonthsIso('2027-01-31', 1), '2027-02-28')
    assert.equal(addMonthsIso('2026-08-31', 1), '2026-09-30')
    assert.equal(addMonthsIso('2028-02-29', 12), '2029-02-28', 'leap day start')
  })
})

describe('projectDates - calculated End Date', () => {
  it('calculates the examples of the requirement', () => {
    assert.equal(projectEndDate('2026-10-01', '12'), '2027-10-01')
    assert.equal(projectEndDate('2026-12-01', '12'), '2027-12-01')
    assert.equal(projectEndDate('2026-10-01', '6'), '2027-04-01')
    assert.equal(projectEndDate('2027-01-01', '18'), '2028-07-01')
  })

  it('crosses year boundaries for every supported duration', () => {
    assert.equal(projectEndDate('2026-10-01', '12'), '2027-10-01')
    assert.equal(projectEndDate('2026-10-01', '24'), '2028-10-01')
    assert.equal(projectEndDate('2026-10-01', '36'), '2029-10-01')
    assert.equal(projectEndDate('2026-11-30', '12'), '2027-11-30')
  })

  it('keeps the day of the month, clamped when the target month is shorter', () => {
    assert.equal(projectEndDate('2026-01-31', '12'), '2027-01-31')
    assert.equal(projectEndDate('2026-05-31', '12'), '2027-05-31')
    assert.equal(projectEndDate('2026-08-31', '6'), '2027-02-28')
    assert.equal(projectEndDate('2028-02-29', '12'), '2029-02-28')
  })

  it('leaves the End Date empty while a value is missing', () => {
    assert.equal(projectEndDate('', '12'), '')
    assert.equal(projectEndDate('2026-10-01', ''), '')
    assert.equal(projectEndDate('', ''), '')
    assert.equal(projectEndDate('2026-02-31', '12'), '')
    assert.equal(projectEndDate('2026-10-01', 'abc'), '')
  })
})

describe('projectDates - End Date validation', () => {
  it('accepts the calculated date', () => {
    assert.equal(projectEndDateError('2026-10-01', '12', '2027-10-01'), '')
    assert.equal(projectEndDateError('2026-01-31', '12', '2027-01-31'), '')
  })

  it('rejects a date that is not the calculated one', () => {
    const error = projectEndDateError('2026-10-01', '12', '2027-11-30')
    assert.match(error, /must be Oct 1, 2027/)
  })

  it('rejects a date earlier than the start date', () => {
    assert.notEqual(projectEndDateError('2026-10-01', '12', '2026-09-01'), '')
  })

  it('rejects a date that is not a real date', () => {
    assert.match(projectEndDateError('2026-10-01', '12', '2027-02-31'), /valid End Date/)
  })

  it('leaves the empty End Date to the existing required rule', () => {
    assert.equal(projectEndDateError('2026-10-01', '12', ''), '')
  })

  it('reports the hint while the calculation is not possible', () => {
    assert.equal(projectEndDateError('', '12', '2027-10-01'), PROJECT_PERIOD_HINT)
    assert.equal(projectEndDateError('2026-10-01', '', '2027-10-01'), PROJECT_PERIOD_HINT)
  })

  it('offers the two helper messages the form shows', () => {
    assert.match(PROJECT_PERIOD_HINT, /select the project duration and start date first/)
    assert.match(PROJECT_END_DATE_HINT, /Calculated automatically/)
  })

  it('covers every duration the form offers', () => {
    for (const duration of PROJECT_DURATION_OPTIONS) {
      const start = '2026-10-01'
      const end = projectEndDate(start, duration)
      assert.equal(projectEndDateError(start, duration, end), '', `duration ${duration}`)
      assert.equal(end, addMonthsIso(start, Number(duration)))
    }
  })
})
