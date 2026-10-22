/**
 * scrollReset.js
 * Resets the scroll position of the element that actually scrolls in the
 * authenticated app shell.
 *
 * src/layouts/DefaultLayout.vue pins the page body to the viewport
 * (.app-layout { height: 100vh; overflow: hidden }) and scrolls its
 * <main class="content"> region instead (.content { overflow-y: auto }).
 * That region is never re-created while the router swaps views, so its scrollTop
 * is carried over from the previous page: opening the Quarterly Progress Report
 * right after scrolling down Proposal Monitoring used to start the report
 * half-way down, forcing the proponent to scroll back up manually.
 *
 * Scrolling the window alone does not fix that, because the window is not the
 * element that scrolls - the layout's content region is. These helpers reset the
 * real container (and the document/window as a fallback for layouts that do
 * scroll the page).
 *
 * The DOM access is injectable so the behaviour can be verified without a
 * browser - see scrollReset.test.js.
 */

/** Selector of the scrollable content region of the authenticated layout. */
export const CONTENT_SCROLL_SELECTOR = 'main.content'

function defaultDocument() {
  return typeof document === 'undefined' ? null : document
}

function defaultWindow() {
  return typeof window === 'undefined' ? null : window
}

/**
 * The scroll container of the current layout, or null when there is none.
 *
 * @param {Document|null} root document to query (injectable for tests)
 * @returns {HTMLElement|null}
 */
export function getScrollContainer(root = defaultDocument()) {
  if (!root || typeof root.querySelector !== 'function') return null
  return root.querySelector(CONTENT_SCROLL_SELECTOR)
}

/**
 * Scrolls the layout's content region (and, as a fallback, the document and the
 * window) back to the very top. The reset is an instant jump, never an
 * animation, so the report title and progress indicator are already in view on
 * the first paint after the view is opened or the wizard page changes.
 *
 * @param {Document|null} root document to use (injectable for tests)
 * @param {Window|null} win window to use (injectable for tests)
 */
export function resetScrollPosition(root = defaultDocument(), win = defaultWindow()) {
  const container = getScrollContainer(root)
  if (container) {
    container.scrollTop = 0
    container.scrollLeft = 0
  }

  // Fallback for layouts/pages that scroll the document instead of the layout's
  // content region (e.g. the public pages without the side navigation).
  const scrolling = root?.scrollingElement || root?.documentElement
  if (scrolling) {
    scrolling.scrollTop = 0
    scrolling.scrollLeft = 0
  }

  if (win && typeof win.scrollTo === 'function') {
    // The two-argument form of scrollTo is always an instant jump.
    win.scrollTo(0, 0)
  }
}
