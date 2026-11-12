/**
 * rpsProposalMonitoring.test.js
 * ---------------------------------------------------------------------------
 * Source-level guard for the RPS Proposal Monitoring list (RPS Staff,
 * /rps-proposal-monitoring).
 *
 * The page crashed for every user before this test existed: the template called
 * the imported proposalStatusLabel() to build the status-filter labels, but the
 * Options API `methods` block never registered it. Vue therefore rendered no
 * page at all - only the shell - and logged
 *   Property "proposalStatusLabel" was accessed during render but is not
 *   defined on instance.
 * A source check catches that class of mistake without booting a browser.
 *
 * The other assertions keep the API payload defensive: one malformed row or a
 * non-string field must not take down the whole list.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PAGE = read('../views/Rps/RpsProposalMonitoring.vue')
const methods = PAGE.slice(PAGE.indexOf('methods: {'), PAGE.indexOf('mounted() {'))

test('every imported status helper the template calls is registered as a method', () => {
  for (const helper of ['statusTone', 'proposalStatusLabel', 'proposalStatusMatches']) {
    assert.match(PAGE, /import \{[^}]*\bstatusTone\b/, 'statusTone is imported')
    assert.match(
      methods,
      new RegExp(`^\\s{4}${helper},$`, 'm'),
      `${helper} must be registered in methods or the template render fails`,
    )
  }
  assert.match(PAGE, /\{\{ proposalStatusLabel\('FOR REVIEW'\) \}\}/)
})

test('the list survives a malformed record and non-string fields', () => {
  assert.match(
    PAGE,
    /rows\.filter\(\(p\) => p && typeof p === 'object' && !Array\.isArray\(p\)\)/,
    'null and non-object rows are not put into the table',
  )
  assert.match(PAGE, /console\.warn\(/, 'a skipped row is reported, not silently swallowed')
  assert.match(PAGE, /String\(p\.projectTitle \|\| ''\)\.toLowerCase\(\)/, 'search always works on text')
  assert.match(PAGE, /String\(p\.projectLeader \|\| p\.proponent\?\.name \|\| ''\)\.toLowerCase\(\)/)
  assert.match(PAGE, /String\(p\.id\)\.includes\(term\)/)
})

test('loading, API error and empty results each have a rendered state', () => {
  assert.ok(PAGE.includes('v-if="loading"'))
  assert.ok(PAGE.includes('v-else-if="error"'))
  assert.ok(PAGE.includes("this.error = 'Failed to load proposals. Ensure the backend server is running.'"))
  assert.ok(PAGE.includes('v-if="filteredProposals.length === 0"'))
  assert.ok(PAGE.includes('No Proposals Found'))
})

test('every filtered proposal renders in one continuous grid, with no pagination', () => {
  assert.match(
    PAGE,
    /v-for="prop in filteredProposals"/,
    'the grid renders the full filtered list',
  )
  for (const removed of [
    'paginatedProposals',
    'paginationStartIndex',
    'paginationEndIndex',
    'totalPages',
    'currentPage',
    'perPage',
    'btn-pagination',
    'pagination-panel',
    'page-indicator',
    'Showing',
    'Previous',
    'Next',
  ]) {
    assert.ok(!PAGE.includes(removed), `${removed} must be removed with pagination`)
  }
})

test('Current Office shows the project stage for approved and completed proposals', () => {
  assert.match(PAGE, /'APPROVED': 'Ongoing Implementation',/)
  assert.match(PAGE, /'COMPLETED': 'Completed',/)
  assert.match(PAGE, /<option value="Ongoing Implementation">Ongoing Implementation<\/option>/)
  assert.match(PAGE, /<option value="Completed">Completed<\/option>/)
  // The status badge itself keeps the centralized catalog labels.
  assert.match(PAGE, /\{\{ formatStatus\(prop\.status\) \}\}/)
})

test('drafts are excluded before search and filters, and never reach the grid', () => {
  assert.match(
    PAGE,
    /filteredProposals\(\) \{[\s\S]*?normalizeProposalStatus\(p\.status\) !== 'DRAFT'[\s\S]*?const term/,
    'the draft exclusion runs before the search filter',
  )
  assert.match(
    PAGE,
    /v-for="prop in filteredProposals"/,
    'the visible cards come from the same filtered list',
  )
})
