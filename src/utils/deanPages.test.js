/**
 * deanPages.test.js
 * ---------------------------------------------------------------------------
 * Guards the two Dean screens: the Dean dashboard (views/Dashboard/DeanDashboard.vue) and
 * the Dean's endorsement requests (views/Dean/DeanEndorsementRequests.vue).
 *
 * They are Vue components, so nothing type-checks the things that made them look wrong: the
 * width cap that left a large empty area on the right of both pages, a table that could be
 * clipped instead of scrolled on a narrow screen, and a table header whose label colour was
 * unreadable on the blue band the application paints behind it. The files are read as text
 * here - the technique proponentDashboard.test.js and navigation.test.js already use - and
 * each check states the mistake it prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

/** The two pages this test covers, and the stylesheet section that polishes them. */
const DEAN_PAGES = [
  { name: 'the Dean dashboard', file: '../views/Dashboard/DeanDashboard.vue' },
  { name: "the Dean's endorsement requests", file: '../views/Dean/DeanEndorsementRequests.vue' },
]

const STYLES = read('../assets/endorsement.css')

test('both Dean pages fill the width the shell gives them', () => {
  // .end-page alone caps a page at 1040px, which is what left the large empty area on the
  // right of these two pages on any wide screen. The fluid modifier is what prevents that;
  // the Dean modifier is what keeps the polish below scoped to these pages only.
  for (const page of DEAN_PAGES) {
    assert.match(
      read(page.file),
      /class="end-page end-page--fluid end-page--dean"/,
      `${page.name} must keep the full-width modifier`,
    )
  }

  // No page-level width cap may be reintroduced for the Dean pages.
  assert.ok(
    !/\.end-page--dean\s*\{[^}]*max-width/.test(STYLES),
    'the Dean pages must not cap their own width',
  )
})

test('every Dean table scrolls inside its own card', () => {
  // .end-page--fluid gives the tables a 720px minimum width. Without a scroll container the
  // overflow would be clipped by the shell (overflow: hidden) instead of scrolled.
  for (const page of DEAN_PAGES) {
    const source = read(page.file)
    assert.ok(source.includes('end-table-scroll'), `${page.name} must wrap its tables`)
    assert.ok(
      !/<table[^>]*class="[^"]*end-card/.test(source),
      `${page.name} must keep the card on the wrapper, not on the table itself`,
    )
  }
})

test('the table header is readable on the blue band behind it', () => {
  // The application paints #3c3b6e behind every table head, and the shared .end-table th
  // rule overrides that rule's white label with #64748b - 2.2:1, unreadable. The Dean pages
  // set the pair explicitly, so the label stays white whatever the shared rule does.
  const header = /\.end-page--dean \.end-table thead th\s*\{([^}]*)\}/.exec(STYLES)
  assert.ok(header, 'the Dean table header rule must exist')
  assert.match(header[1], /background:\s*#3c3b6e/, 'the header must keep the blue band')
  assert.match(header[1], /color:\s*#ffffff/, 'the header label must be white on that blue')
})

test('the corrected text colours are the darker, readable ones', () => {
  assert.match(
    STYLES,
    /\.end-page--dean \.end-subtitle\s*\{[^}]*color:\s*#475569/,
    'the description must clear the WCAG AA floor on the content background',
  )
  assert.match(
    STYLES,
    /\.end-page--dean \.end-muted\s*\{[^}]*color:\s*#64748b/,
    'the muted metadata must be readable on white',
  )
})

test('the shared endorsement styles stay untouched', () => {
  // Every rule the Dean section adds is scoped to .end-page--dean, and the shared page cap
  // must remain what the other endorsement screens are laid out with.
  assert.match(
    STYLES,
    /\.end-page \{\s*max-width: 1040px;?\s*\}/,
    'the shared .end-page cap must be unchanged',
  )
  assert.match(
    STYLES,
    /\.end-page--fluid \{\s*max-width: none;?\s*\}/,
    'the full-width modifier must stay as it is',
  )
})
