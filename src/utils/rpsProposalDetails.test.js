/**
 * rpsProposalDetails.test.js
 * ---------------------------------------------------------------------------
 * Guards the page the Budget Processing View buttons open (RPS, /proposal/:id).
 *
 * A pasted block once left the field-remarks bindings nested inside fetchProposal, so the
 * page threw "loadFieldRemarks is not defined" on every load, silently disabled every remark
 * control, and could sit on its loading state with nothing ever appearing. Braces are counted
 * here, so a binding that drifts inside another function fails the test instead of reaching
 * the browser.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PAGE = read('../views/Rps/RpsProposalDetails.vue')

/** Braces are counted with interpolations removed, so template literals stay balanced. */
function depthOf(source, needle) {
  let depth = 0
  for (const line of source.split('\n')) {
    if (line.includes(needle)) return depth
    const cleaned = line.replace(/\$\{[^}]*\}/g, '')
    depth += (cleaned.match(/\{/g) || []).length - (cleaned.match(/\}/g) || []).length
  }
  return null
}

test('the bindings the template uses live at the top level of setup', () => {
  // Depth 0 means "reachable by the template". Nested inside a function they are undefined,
  // which is exactly how this page used to break.
  for (const binding of [
    'const fetchProposal =',
    'const remarksResponse =',
    'const remarkIndex =',
    'const loadFieldRemarks =',
    'const onRemarkSaved =',
    'const onRemarkRemoved =',
    'const load =',
  ]) {
    assert.equal(depthOf(PAGE, binding), 0, binding + ' must be a setup-scope binding')
  }
})

test('the page loads the proposal from the route and reacts when the id changes', () => {
  assert.ok(PAGE.includes('route.params.id ?? route.query.id'), 'the id comes from the route')
  assert.ok(
    PAGE.includes('api.get(`/api/proposals/${routeProposalId}`)'),
    'the proposal is fetched with the route id',
  )
  assert.ok(
    PAGE.includes('const proposalId = computed(() => proposal.value?.id ?? null)'),
    'the loaded proposal id is available to the remark bindings outside fetchProposal',
  )
  assert.ok(
    PAGE.includes('api.get(`/api/proposals/${proposalId.value}/field-remarks`)'),
    'and so are its remarks',
  )
  assert.match(PAGE, /watch\(/, 'a route change reloads')
  assert.ok(PAGE.includes('onMounted(load)'), 'the first load needs no navigation')
})

test('every outcome is a state, so the page is never left blank', () => {
  for (const state of [
    'v-if="loading"',
    'v-else-if="error"',
    'v-else-if="notFound"',
    'v-else-if="proposal.id"',
    '<div v-else class="error-state">',
  ]) {
    assert.ok(PAGE.includes(state), 'the page must render the ' + state + ' state')
  }
})

test('navigation never reloads the whole page', () => {
  assert.ok(!PAGE.includes('window.location'), 'use the router, not a full page load')
  assert.ok(PAGE.includes("router.push('/budget-processing')"), 'Back has a fallback')
})