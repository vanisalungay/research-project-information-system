/**
 * proposalReviewDesign.test.js
 * ---------------------------------------------------------------------------
 * Guards the shared review design layer and the field section it repaints.
 *
 * The redesign is visual: every page keeps its own fields, actions, states and handlers. The
 * risk this covers is that a restyle silently drops one of them on the way. The files are read
 * as text - the technique the other guard tests use - so this needs no browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const CSS = read('../assets/review.css')
const FIELD_SECTION = read('../components/ProposalFieldSection.vue')
const HEADER = read('../components/ProposalReviewHeader.vue')
const SIDEBAR = read('../components/ProposalReviewSidebar.vue')
const PAGE = read('../components/ProposalReviewPage.vue')

test('the design layer defines the page, header, card and summary primitives', () => {
  for (const cls of [
    '.rv-page',
    '.rv-header',
    '.rv-back',
    '.rv-title',
    '.rv-id',
    '.rv-status',
    '.rv-header-actions',
    '.rv-body',
    '.rv-main',
    '.rv-aside',
    '.rv-card',
    '.rv-card-head',
    '.rv-card-body',
    '.rv-rows',
    '.rv-row',
    '.rv-state',
    '.rv-spinner',
  ]) {
    assert.ok(
      CSS.includes(cls + ' ') || CSS.includes(cls + ','),
      'review.css must define ' + cls,
    )
  }
})

test('the design layer is responsive and cannot overflow horizontally', () => {
  assert.ok(CSS.includes('@media (max-width: 1024px)'), 'the summary column drops below content')
  assert.ok(CSS.includes('@media (max-width: 640px)'), 'the header stacks on a phone')
  assert.ok(CSS.includes('minmax(0, 1fr)'), 'grid tracks are minmax(0, ...) so long values wrap')
  assert.ok(CSS.includes('overflow-wrap: anywhere'), 'long values wrap instead of overflowing')
})

test('the shared field section paints the review card design', () => {
  assert.ok(FIELD_SECTION.includes('Review design layer'), 'the design rules are present')
  assert.ok(FIELD_SECTION.includes('linear-gradient(100deg, #262342'), 'the navy header band')
  assert.ok(FIELD_SECTION.includes('border-radius: var(--radius-xl, 14px)'), 'the card radius')
  assert.ok(FIELD_SECTION.includes('max-width: 640px'), 'the field grid collapses on a phone')
})

test('the field section still renders every field through the registry', () => {
  // No field may be removed or merged by the redesign: the sections still come from the
  // registry and every field still carries its own remark control.
  assert.ok(FIELD_SECTION.includes('FieldRemarkControl'), 'remark controls are untouched')
  assert.ok(FIELD_SECTION.includes('displayValue(field)'), 'values still come from the registry')
  assert.ok(FIELD_SECTION.includes('section.fields'), 'fields are still iterated from the section')
  assert.ok(FIELD_SECTION.includes('agendaFields'), 'keyed agenda entries are still rendered')
})

test('the shared header and summary cards are prop-driven and never fetch', () => {
  for (const [name, source] of [['header', HEADER], ['sidebar', SIDEBAR], ['page', PAGE]]) {
    assert.ok(!/api\.(get|post|put|delete)/.test(source), name + ' must not call the API')
    assert.ok(source.includes('defineProps'), name + ' must take what the page already loaded')
  }
  assert.ok(HEADER.includes("$emit('back')"), 'the header keeps the page in charge of Back')
  assert.ok(HEADER.includes("$emit('download')"), 'and of DOWNLOAD')
  assert.ok(PAGE.includes('slot name="actions"'), 'the role actions render in the header')
  assert.ok(SIDEBAR.includes('Proposal Status'), 'the status card')
  assert.ok(SIDEBAR.includes('Quick Information'), 'the quick information card')
  assert.ok(SIDEBAR.includes('Review Information'), 'the review information card')
})

test('the page shell owns the four states, so no page can render blank', () => {
  assert.ok(PAGE.includes('v-if="loading"'), 'a loading state')
  assert.ok(PAGE.includes('v-else-if="error"'), 'an error state with a retry')
  assert.ok(PAGE.includes('v-else-if="notFound"'), 'a not-found state')
  assert.ok(PAGE.includes("$emit('retry')"), 'the retry path')
})

test('every component that renders .rv-* markup carries the stylesheet', () => {
  // The defect this guards: the design layer was imported only by a page wrapper that no page
  // used, so Rollup dropped the whole layer from the bundle and every .rv-* class was
  // undefined at runtime - the header lost its row, the cards lost their boxes and the labels
  // ran into their values. A component that renders .rv-* markup must import the stylesheet.
  for (const [name, source] of [
    ['ProposalReviewHeader.vue', HEADER],
    ['ProposalReviewSidebar.vue', SIDEBAR],
    ['ProposalReviewPage.vue', PAGE],
  ]) {
    assert.ok(
      source.includes('@/assets/review.css'),
      name + ' must import the review design layer it renders with',
    )
  }
  assert.ok(HEADER.includes('class="rv-header"'), 'the header renders .rv-* markup')
  assert.ok(SIDEBAR.includes('class="rv-cards"'), 'the summary cards render .rv-* markup')
})

test('a label and its value can never run together', () => {
  // The reported symptom was "CreatedJul 29, 2026". The row is a flex column, so the two are
  // always on their own lines whatever else is loaded around them.
  assert.match(CSS, /\.rv-row \{[^}]*display: flex/, 'the summary row is a flex box')
  assert.match(CSS, /\.rv-row \{[^}]*flex-direction: column/, 'stacked, never inline')
  assert.match(CSS, /\.rv-row-value \{[^}]*text-align: left/, 'the value sits under its label')
})

test('the header keeps its actions on the far side and wraps instead of overlapping', () => {
  assert.match(CSS, /\.rv-header-actions \{[^}]*margin-left: auto/, 'DOWNLOAD stays right')
  assert.match(CSS, /\.rv-header \{[^}]*flex-wrap: wrap/, 'the header wraps on a narrow screen')
  assert.match(CSS, /\.rv-heading-meta \{[^}]*flex-wrap: wrap/, 'the status pill never overlaps the id')
})