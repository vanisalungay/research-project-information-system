/**
 * ovcafProposalsPage.test.js
 * ---------------------------------------------------------------------------
 * Guards the OVCAF "Proposals" page after the rename and table rebuild:
 *  - College is a real column and the header/cell count matches (the old page had
 *    7 headers for 8 cells, which shifted every value one column right);
 *  - the Budget column is gone (the budget data itself is untouched);
 *  - Date Received is the server's actual receipt event, never createdAt;
 *  - ordering defaults to Date Received newest first and is applied by the API.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Ovcaf/OvcafProposalInbox.vue')
const ROUTER = read('../router/index.js')
const NAV = read('../components/NavMenu.vue')

test('the page is named Proposals and reuses the shared design layer', () => {
  assert.ok(PAGE.includes('<h2>Proposals</h2>'))
  assert.ok(PAGE.includes("import '@/assets/endorsement.css'"))
  for (const className of ['end-table-scroll', 'end-table', 'end-badge', 'PaginationBar']) {
    assert.ok(PAGE.includes(className), `the page must use ${className}`)
  }
  assert.ok(NAV.includes("text: 'Proposals'") && NAV.includes("path: '/ovcaf/proposals'"))
  assert.ok(ROUTER.includes("path: 'ovcaf/proposals'"))
  assert.ok(ROUTER.includes("redirect: '/ovcaf/proposals'"), 'the old inbox URL must redirect')
})

test('the table has seven matching columns, including College and no Budget', () => {
  const headers = [...PAGE.matchAll(/<th[^>]*>([^<]+)<\/th>/g)].map((m) => m[1].trim())
  assert.deepEqual(headers, [
    'Proposal ID',
    'Research Title',
    'Project Leader',
    'College',
    'Date Received',
    'Status',
    'Actions',
  ])
  assert.ok(!headers.includes('Budget'), 'the Budget column must be removed from the table')
  assert.ok(PAGE.includes('colspan="7"'), 'the empty row must span all seven columns')
  assert.equal(
    [...PAGE.matchAll(/<td[^>]*>/g)].length,
    8, // one empty-state cell + seven real row cells
    'each row must render exactly seven cells',
  )
})

test('Date Received is the real OVCAF receipt event', () => {
  assert.ok(PAGE.includes('dateReceived: p.receivedAt || null'))
  assert.ok(!PAGE.includes('p.createdAt'), 'creation date must never be shown as the receipt date')
  assert.ok(PAGE.includes(':title="p.documentId"'), 'proposal IDs stay readable')
})

test('the first page is Date Received, newest first, resolved by the server', () => {
  assert.ok(PAGE.includes("const DEFAULT_SORT = 'receivedAt-desc'"))
  assert.ok(PAGE.includes("sortSelection: DEFAULT_SORT"))
  assert.ok(PAGE.includes("api.get('/api/ovcaf/proposals/page'"))
  for (const param of ['search:', 'status:', 'college:', 'program:', 'sortBy', 'sortDir', 'page:', 'size:']) {
    assert.ok(PAGE.includes(param), `the page request must send ${param}`)
  }
  assert.ok(PAGE.includes("sortDir: sortDir === 'asc' ? 'asc' : 'desc'"))
})

test('the filters include Status, College and Program with a Clear All reset', () => {
  assert.ok(PAGE.includes('v-for="status in options.statuses"'))
  assert.ok(PAGE.includes('v-for="college in options.colleges"'))
  assert.ok(PAGE.includes('v-for="program in options.programs"'))
  assert.ok(PAGE.includes('@click="clearFilters"'))
  const start = PAGE.indexOf('const clearFilters = () => {')
  const source = PAGE.slice(start, PAGE.indexOf('const goToPage', start))
  for (const reset of [
    "filters.search = ''",
    "filters.status = 'All'",
    "filters.college = 'All'",
    "filters.program = 'All'",
    'filters.sortSelection = DEFAULT_SORT',
    'filters.page = 1',
  ]) {
    assert.ok(source.includes(reset), `Clear All must reset ${reset}`)
  }
})

test('loading failures are shown instead of looking like an empty list', () => {
  assert.ok(PAGE.includes('v-else-if="error"'))
  assert.ok(PAGE.includes('role="alert"'))
  assert.ok(PAGE.includes('@click="fetchProposals"'))
})

test('pagination and row actions come from the server result', () => {
  assert.ok(PAGE.includes('<PaginationBar'))
  assert.ok(PAGE.includes(':total="totalItems"'))
  assert.ok(PAGE.includes(':page-size="filters.limit"'))
  assert.ok(PAGE.includes('@update:page="goToPage"'))
  assert.ok(PAGE.includes(':to="`/ovcaf/proposals/${p.id}`"'), 'View Details opens the proposal')
  assert.ok(PAGE.includes(':to="`/ovcaf/validate/${p.id}`"'), 'pending proposals keep the Validate action')
  assert.ok(PAGE.includes("normalizeStatusKey(p.status) === 'FOR_OVCAF_APPROVAL'"))
})
