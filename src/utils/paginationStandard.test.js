/**
 * paginationStandard.test.js
 * ---------------------------------------------------------------------------
 * Guards the system-wide pagination standard and the two targeted RPS/Admin
 * changes:
 *  - every list starts at 10 rows and uses the shared PaginationBar;
 *  - the RPS Rejected Proposals table shows the workflow Date Processed;
 *  - the Activity Log no longer renders its Status column.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const PAGER = read('../components/PaginationBar.vue')
const COMPOSABLE = read('../composables/usePagination.js')
const MONITORING = read('../views/Rps/RpsProposalMonitoring.vue')
const OVC_SUBMITTED = read('../views/Ovc/OvcSubmittedProposal.vue')
const ACTIVITY = read('../views/Admin/ActivityLog.vue')
const REJECTED = read('../views/Rps/RpsSubmittedProposalsPanel.vue')

test('the shared pager defaults to ten rows and offers the same size choices', () => {
  assert.ok(COMPOSABLE.includes('export const DEFAULT_PAGE_SIZE = 10'))
  assert.ok(COMPOSABLE.includes('export const PAGE_SIZE_OPTIONS = [10, 25, 50, 100]'))
  assert.ok(PAGER.includes("pageSize: { type: Number, default: 10 }"))
  assert.ok(PAGER.includes('Rows per page'))
  assert.ok(PAGER.includes('Page {{ page }} of {{ pages }}'))
  assert.ok(PAGER.includes('pagination-number.active'), 'the active page keeps the RPS styling')
})

test('RPS Proposal Monitoring starts at ten rows per page', () => {
  assert.ok(MONITORING.includes('pageSize: 10'), 'the page state must start at 10')
  assert.ok(MONITORING.includes('<PaginationBar'), 'the shared pager must be used')
  assert.ok(MONITORING.includes(':page-size="pageSize"'))
  assert.ok(!MONITORING.includes('pageSize: 12'))
})

test('the OVC Submitted list uses the shared pager with a ten-row default', () => {
  assert.ok(OVC_SUBMITTED.includes('const pageSize = ref(10)'))
  assert.ok(OVC_SUBMITTED.includes('<PaginationBar'))
  assert.ok(OVC_SUBMITTED.includes(':page-size="pageSize"'))
  assert.ok(OVC_SUBMITTED.includes('@update:page-size="setPageSize"'))
  assert.ok(!OVC_SUBMITTED.includes('PAGE_SIZE = 8'))
})

test('the Rejected Proposals table adds the workflow Date Processed column', () => {
  assert.ok(REJECTED.includes('>Date Processed</th>'))
  assert.ok(REJECTED.includes('processedDate(proposal)'))
  assert.ok(REJECTED.includes("api.get('/api/proposals/rejections')"),
    'the dedicated ORPS rejection queue supplies the real timestamps')
  assert.ok(REJECTED.includes('proposal.rejectionRoutedAt || proposal.routedAt'))
  assert.ok(REJECTED.includes('proposal.rejectionProcessedAt || proposal.processedAt'))
  assert.ok(REJECTED.includes("toLocaleString('en-US'"),
    'the processed date is shown with a consistent date and time format')
  assert.ok(REJECTED.includes(":colspan=\"category === 'rejected' ? 7 : 6\""),
    'the empty row accounts for the extra column')
})

test('the Activity Log no longer renders a Status column but keeps its filter', () => {
  const tableHead = ACTIVITY.slice(ACTIVITY.indexOf('<thead>'), ACTIVITY.indexOf('</thead>'))
  assert.ok(!tableHead.includes('>Status</th>'), 'the Status header is removed')
  assert.ok(!ACTIVITY.includes('status-badge" :class="statusTone(entry.status)'),
    'the row no longer renders the status badge')
  assert.ok(ACTIVITY.includes('v-model="statusFilter"'), 'the status filter is preserved')
  assert.ok(ACTIVITY.includes('<PaginationBar'), 'the shared pager is used')
  assert.ok(ACTIVITY.includes(':page="page + 1"'), 'the 1-based pager is wired to the 0-based API')
  assert.ok(ACTIVITY.includes('@update:page-size="setPageSize"'))
})
