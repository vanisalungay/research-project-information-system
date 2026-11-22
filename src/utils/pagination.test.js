/**
 * pagination.test.js
 * ---------------------------------------------------------------------------
 * The shared pagination rules every list uses: real totals, valid page bounds,
 * no duplicate rows, and stable page numbers.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildPageTokens,
  clampPage,
  pageCount,
  pageEnd,
  pageStart,
  paginate,
} from '../composables/usePagination.js'

test('an empty list still has one page and shows 0 of 0', () => {
  assert.equal(pageCount(0, 10), 1)
  assert.equal(pageStart(1, 10, 0), 0)
  assert.equal(pageEnd(1, 10, 0), 0)
  assert.deepEqual(paginate([], 1, 10), [])
})

test('the page count is the ceiling of the total over the page size', () => {
  assert.equal(pageCount(1, 10), 1)
  assert.equal(pageCount(10, 10), 1)
  assert.equal(pageCount(11, 10), 2)
  assert.equal(pageCount(25, 25), 1)
  assert.equal(pageCount(26, 25), 2)
})

test('a page outside the bounds is clamped instead of returning nothing', () => {
  assert.equal(clampPage(0, 3), 1)
  assert.equal(clampPage(-5, 3), 1)
  assert.equal(clampPage(9, 3), 3)
  assert.equal(clampPage('2', 3), 2)
})

test('the visible range is accurate on every page, including the last', () => {
  assert.equal(pageStart(1, 10, 25), 1)
  assert.equal(pageEnd(1, 10, 25), 10)
  assert.equal(pageStart(3, 10, 25), 21)
  assert.equal(pageEnd(3, 10, 25), 25)
  assert.equal(pageStart(9, 10, 25), 21, 'an out-of-range page is clamped')
  assert.equal(pageEnd(9, 10, 25), 25)
})

test('slicing pages never duplicates or drops a row', () => {
  const items = Array.from({ length: 23 }, (_, index) => index + 1)
  const rebuilt = []
  for (let page = 1; page <= pageCount(items.length, 10); page += 1) {
    rebuilt.push(...paginate(items, page, 10))
  }
  assert.deepEqual(rebuilt, items)
  assert.equal(paginate(items, 3, 10).length, 3)
  assert.deepEqual(paginate(items, 2, 10), [11, 12, 13, 14, 15, 16, 17, 18, 19, 20])
})

test('page tokens show the ends, the current page and its neighbours', () => {
  assert.deepEqual(buildPageTokens(1, 1), [1])
  assert.deepEqual(buildPageTokens(1, 3), [1, 2, 3])
  assert.deepEqual(buildPageTokens(1, 10), [1, 2, '…', 10])
  assert.deepEqual(buildPageTokens(5, 10), [1, '…', 4, 5, 6, '…', 10])
  assert.deepEqual(buildPageTokens(10, 10), [1, '…', 9, 10])
  assert.deepEqual(buildPageTokens(99, 10), [1, '…', 9, 10], 'an out-of-range page is clamped')
})
