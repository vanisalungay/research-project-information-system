/**
 * recProposalDetail.test.js
 * ---------------------------------------------------------------------------
 * Guards the REC proposal detail pages against the defect that made them blank.
 *
 * `ref < any > ({...})` is a TypeScript generic left in a plain-JavaScript script setup.
 * JavaScript reads it as the comparison `(ref < any) > ({...})`, so the type name is
 * dereferenced while the component is being set up and a ReferenceError is thrown - the page
 * then renders nothing at all (header and sidebar only) on every navigation, refresh and
 * direct link. The production build does not catch it, because the code is syntactically
 * valid JavaScript.
 *
 * These assertions read the files as text, so they need no browser and no build.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readdirSync, readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const DETAIL = read('../views/Rec/RecDetailedProp.vue')
const REVIEW_FORM = read('../views/Rec/RecReviewForm.vue')
const MEETING = read('../views/Rec/RecMeetingDetails.vue')

test('no REC view carries a TypeScript generic into the browser', () => {
  // ref<X>(...), ref < X > (...), ref<any[]> - every form is the same fatal defect.
  const pattern = /ref\s*<\s*[A-Za-z]/
  for (const file of readdirSync(fileURLToPath(new URL('../views/Rec', import.meta.url)))) {
    if (!file.endsWith('.vue')) continue
    const source = read('../views/Rec/' + file)
    assert.ok(!pattern.test(source), file + ' must not contain a TypeScript generic')
  }
})

test('the detail page resolves the proposal from the route, not from temporary state', () => {
  assert.ok(
    DETAIL.includes('route.params.id ?? route.query.id'),
    'the id is read from the route (query form first used by the VIEW buttons)',
  )
  assert.match(DETAIL, /watch\(\s*routeProposalId/, 'a route change must reload the proposal')
  assert.ok(DETAIL.includes('{ immediate: true }'), 'the first load needs no navigation')
  assert.ok(DETAIL.includes('useRoute()'), 'the route is the source of truth')
})

test('the detail page can never render an empty content area', () => {
  assert.ok(DETAIL.includes('v-if="loading"'), 'a loading state')
  assert.ok(DETAIL.includes('v-else-if="error"'), 'an error state')
  assert.ok(DETAIL.includes('missingId || notFound'), 'a missing-id / not-found state')
  assert.ok(
    DETAIL.includes('<div v-else class="error-state">'),
    'a final fallback, so the content area is never silently empty',
  )
  assert.ok(DETAIL.includes('loadProposal'), 'a retry path exists')
})

test('Back has a handler the template can actually reach', () => {
  // It used to be declared inside onMounted, where @click="goBack" resolved to nothing.
  assert.match(DETAIL, /\nconst goBack = \(\) => \{/, 'goBack must be a setup-scope binding')
  assert.ok(!DETAIL.includes('onMounted('), 'the page no longer depends on a single mount')
  assert.ok(DETAIL.includes('@click="goBack"'), 'the Back control uses it')
})

test('the review and meeting pages resolve the id the same way', () => {
  assert.ok(REVIEW_FORM.includes('route.params.id ?? route.query.id'))
  assert.ok(REVIEW_FORM.includes('This proposal could not be found.'))
  assert.ok(MEETING.includes('route.params.id ?? route.query.id'))
  assert.match(MEETING, /watch\(/, 'the meeting page reloads when the id changes')
  assert.ok(
    MEETING.includes('const proposalId = proposal.value.id || route.params.id || route.query.id'),
    'the VIEW control can no longer do nothing at all',
  )
})