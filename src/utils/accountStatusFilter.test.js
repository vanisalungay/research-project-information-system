/**
 * accountStatusFilter.test.js
 * ---------------------------------------------------------------------------
 * The User Accounts status filter is a pure function over the loaded account list
 * (utils/accountStatusFilter.js), so it is checked without a browser.
 *
 * What matters:
 *   - the options are the account statuses the backend actually stores, in one
 *     stable order, each with the real number of accounts behind it;
 *   - no status is invented (REVOKED is a Dean *appointment* state, not an account
 *     status, so it must not appear here);
 *   - the stored value decides the match, regardless of its letter case, and the
 *     "All Accounts" option matches everything.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ACCOUNT_STATUS_FILTER_ORDER,
  accountStatusFilterOptions,
  matchesAccountStatus,
  normalizeAccountStatus,
} from './accountStatusFilter.js'

test('the filter offers exactly the account statuses the backend stores', () => {
  assert.deepEqual([...ACCOUNT_STATUS_FILTER_ORDER], ['PENDING', 'APPROVED', 'REJECTED', 'INACTIVE'])
  assert.ok(
    !ACCOUNT_STATUS_FILTER_ORDER.includes('REVOKED'),
    'REVOKED is a Dean appointment status; the account itself stays APPROVED',
  )
})

test('every offered option carries its real count, and All is not an option here', () => {
  const users = [
    { id: 1, status: 'PENDING' },
    { id: 2, status: 'APPROVED' },
    { id: 3, status: 'approved' },
    { id: 4, status: 'REJECTED' },
  ]

  const options = accountStatusFilterOptions(users)

  assert.deepEqual(
    options.map((option) => option.value),
    ['PENDING', 'APPROVED', 'REJECTED', 'INACTIVE'],
    'the supported statuses are always offered, even at zero',
  )
  assert.deepEqual(
    Object.fromEntries(options.map((option) => [option.value, option.count])),
    { PENDING: 1, APPROVED: 2, REJECTED: 1, INACTIVE: 0 },
    'counts come from the loaded rows, not from a hardcoded number',
  )
  assert.deepEqual(
    options.map((option) => option.label),
    ['Pending', 'Approved', 'Rejected', 'Inactive'],
    'the wording comes from the shared status catalog',
  )
  assert.ok(
    options.every((option) => option.value !== ''),
    'the All Accounts option is the page state, not a status in this list',
  )
})

test('a stored status the catalog has not seen yet is shown, never dropped', () => {
  const options = accountStatusFilterOptions([{ status: 'SUSPENDED' }])

  const added = options.find((option) => option.value === 'SUSPENDED')
  assert.ok(added, 'an extra stored status must still be filterable')
  assert.equal(added.count, 1)
  assert.equal(added.label, 'Suspended')
})

test('the match uses the stored status and ignores case and spacing', () => {
  assert.equal(matchesAccountStatus({ status: 'pending' }, 'PENDING'), true)
  assert.equal(matchesAccountStatus({ status: ' Approved ' }, 'APPROVED'), true)
  assert.equal(matchesAccountStatus({ status: 'APPROVED' }, 'REJECTED'), false)
  assert.equal(matchesAccountStatus({ status: null }, 'APPROVED'), false)
})

test('the All Accounts option matches every row, including one without a status', () => {
  assert.equal(matchesAccountStatus({ status: 'PENDING' }, ''), true)
  assert.equal(matchesAccountStatus({ status: null }, ''), true)
  assert.equal(matchesAccountStatus({}, undefined), true)
})

test('normalization is the shared comparison key', () => {
  assert.equal(normalizeAccountStatus('for review'), 'FOR_REVIEW')
  assert.equal(normalizeAccountStatus(null), '')
  assert.equal(normalizeAccountStatus({}), '')
})

test('an empty or missing list still produces the supported options at zero', () => {
  for (const input of [[], null, undefined]) {
    const options = accountStatusFilterOptions(input)
    assert.equal(options.length, ACCOUNT_STATUS_FILTER_ORDER.length)
    assert.ok(options.every((option) => option.count === 0))
  }
})
