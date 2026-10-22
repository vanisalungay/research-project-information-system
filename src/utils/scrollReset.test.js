/**
 * scrollReset.test.js
 * Verifies that the scroll reset targets the element that actually scrolls in
 * the authenticated layout - <main class="content"> - instead of only the
 * window, so opening the Quarterly Progress Report after scrolling down Proposal
 * Monitoring starts the report at the very top.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import { CONTENT_SCROLL_SELECTOR, getScrollContainer, resetScrollPosition } from './scrollReset.js'

/** Minimal stand-in for a scrollable element. */
function fakeElement(scrollTop = 0, scrollLeft = 0) {
  return { scrollTop, scrollLeft }
}

/** Minimal stand-in for a document, with an injectable content region. */
function fakeDocument({
  container = fakeElement(1200, 40),
  scrollingElement = fakeElement(800, 10),
} = {}) {
  return {
    querySelector: (selector) => (selector === CONTENT_SCROLL_SELECTOR ? container : null),
    scrollingElement,
    documentElement: scrollingElement,
  }
}

test('the layout content region is the element that controls scrolling', () => {
  const container = fakeElement()
  const root = fakeDocument({ container })

  assert.equal(CONTENT_SCROLL_SELECTOR, 'main.content')
  assert.equal(getScrollContainer(root), container)
})

test('the layout content region is reset to the very top', () => {
  const container = fakeElement(1500, 80)
  const root = fakeDocument({ container })
  const calls = []
  const win = { scrollTo: (x, y) => calls.push([x, y]) }

  resetScrollPosition(root, win)

  assert.equal(container.scrollTop, 0)
  assert.equal(container.scrollLeft, 0)
  assert.deepEqual(calls, [[0, 0]])
})

test('the document and the window are reset as a fallback', () => {
  const scrollingElement = fakeElement(900, 30)
  const root = fakeDocument({ container: null, scrollingElement })
  const calls = []
  const win = { scrollTo: (x, y) => calls.push([x, y]) }

  resetScrollPosition(root, win)

  assert.equal(scrollingElement.scrollTop, 0)
  assert.equal(scrollingElement.scrollLeft, 0)
  assert.deepEqual(calls, [[0, 0]])
})

test('a missing container, document or window never throws', () => {
  const root = fakeDocument({ container: null, scrollingElement: null })

  assert.doesNotThrow(() => resetScrollPosition(root, null))
  assert.equal(getScrollContainer(null), null)
  assert.equal(getScrollContainer({}), null)
})
