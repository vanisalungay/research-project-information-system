/**
 * ovcafQuarterlyReports.test.js
 * ---------------------------------------------------------------------------
 * Guards the OVCAF Quarterly Reports page and its read-only contract:
 *  - the sidebar item and route replace "Financial Reports";
 *  - the page lists real submitted reports through OVCAF's own read-only API;
 *  - the table columns and filters required by the office are present;
 *  - ordering defaults to Date Submitted, newest first;
 *  - reports are viewed with the shared components and never edited.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const PAGE = read('../views/Ovcaf/OvcafQuarterlyReports.vue')
const ROUTER = read('../router/index.js')
const NAV = read('../components/NavMenu.vue')
const SECURITY = read('../../backend/src/main/java/com/rpis/backend/config/SecurityConfig.java')

test('the sidebar and router name it Quarterly Reports', () => {
  assert.ok(NAV.includes("text: 'Quarterly Reports'"))
  assert.ok(NAV.includes("path: '/ovcaf/quarterly-reports'"))
  assert.ok(!NAV.includes("text: 'Financial Reports'"), 'the old label must be gone')
  assert.ok(ROUTER.includes("path: 'ovcaf/quarterly-reports'"))
  assert.ok(ROUTER.includes("redirect: '/ovcaf/quarterly-reports'"), 'the old reports URL must redirect')
  assert.ok(PAGE.includes('<h2>Quarterly Reports</h2>'))
})

test('the page reads OVCAF\'s own authorized, read-only endpoint', () => {
  assert.ok(PAGE.includes("api.get('/api/ovcaf/quarterly-reports'"))
  assert.ok(SECURITY.includes('.requestMatchers("/api/ovcaf/**").hasRole(UserRoles.OVCAF)'))
  assert.ok(!PAGE.includes('api.post('), 'OVCAF must not create reports')
  assert.ok(!PAGE.includes('api.put(/' + 'api/quarterly-reports'), 'OVCAF must not update reports')
  assert.ok(!PAGE.includes('Approve') && !PAGE.includes('Return Report'),
    'no approve/return action may be exposed')
})

test('the table carries the proposal, period, submission and status columns', () => {
  const headers = [...PAGE.matchAll(/<th[^>]*>([^<]+)<\/th>/g)].map((m) => m[1].trim())
  assert.deepEqual(headers, [
    'Proposal ID',
    'Project Title',
    'Proponent',
    'College',
    'Period',
    'Date Submitted',
    'Status',
    'Actions',
  ])
  assert.ok(PAGE.includes('colspan="8"'))
})

test('filters cover search, period, college, program and project with Clear All', () => {
  assert.ok(PAGE.includes('v-for="period in options.periods"'))
  assert.ok(PAGE.includes('v-for="college in options.colleges"'))
  assert.ok(PAGE.includes('v-for="program in options.programs"'))
  assert.ok(PAGE.includes('v-for="project in options.projects"'))
  assert.ok(PAGE.includes('@click="clearFilters"'))
  for (const param of ['search:', 'period:', 'college:', 'program:', 'proposalId:', 'sortBy', 'sortDir', 'page:', 'size:']) {
    assert.ok(PAGE.includes(param), `the request must send ${param}`)
  }
})

test('Date Submitted newest first is the default order', () => {
  assert.ok(PAGE.includes("const DEFAULT_SORT = 'submittedAt-desc'"))
  assert.ok(PAGE.includes("sortSelection: DEFAULT_SORT"))
  assert.ok(PAGE.includes('<option value="submittedAt-desc">Newest submitted</option>'))
  assert.ok(PAGE.includes("sortBy: sortBy || 'submittedAt'"))
})

test('reports open in the shared read-only viewers', () => {
  assert.ok(PAGE.includes("import QuarterlyReportViewer from '@/components/QuarterlyReportViewer.vue'"))
  assert.ok(PAGE.includes('<QuarterlyReportViewer :report="viewing"'))
  assert.ok(PAGE.includes("import TerminalReportForm from '@/components/TerminalReportForm.vue'"))
  assert.ok(PAGE.includes('<TerminalReportForm :model-value="terminalDoc.terminalReport" readonly />'))
  assert.ok(PAGE.includes('documents-base-path="/api/ovcaf/quarterly-reports"'))
  assert.ok(PAGE.includes('@click="viewReport(row)"'))
})

test('pagination is driven by the server result', () => {
  assert.ok(PAGE.includes('Showing {{ rangeStart }}–{{ rangeEnd }} of {{ totalItems }}'))
  assert.ok(PAGE.includes('Page {{ page }} of {{ totalPages }}'))
  assert.ok(PAGE.includes('@click="goToPage(filters.page + 1)"'))
})
