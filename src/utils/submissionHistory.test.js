/**
 * submissionHistory.test.js
 * ---------------------------------------------------------------------------
 * The Submission & Revision History is a UI layer over the existing versions API. The
 * redesign must not lose a field, a version, the snapshot loader or the details toggle.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const HISTORY = read('../components/SubmissionHistory.vue')
const FORWARD = read('../views/Rps/RpsForwardRevision.vue')
const DETAILS = read('../views/Rps/RpsProposalDetails.vue')

test('the section has a clear title, subtitle and version count', () => {
  assert.ok(HISTORY.includes('Submission &amp; Revision History'))
  assert.ok(HISTORY.includes('class="sh-subtitle"'))
  assert.ok(HISTORY.includes('Newest first, with the current version marked.'))
  assert.ok(HISTORY.includes("{{ versions.length }} version"))
})

test('the timeline renders the newest event first without changing the stored list', () => {
  assert.ok(HISTORY.includes("import { sortVersionsNewestFirst } from '@/utils/versionHistory.js'"))
  assert.ok(HISTORY.includes('sortVersionsNewestFirst(versions.value)'))
  assert.match(HISTORY, /v-for="\(v, index\) in sortedVersions"/)
  assert.match(HISTORY, /index !== sortedVersions\.length - 1/)
  assert.ok(!HISTORY.includes('versions.value.sort('), 'the fetched array is not mutated')
})

test('every existing history field is still displayed', () => {
  for (const field of [
    'v.documentId',
    'v.status',
    'v.submittedByName',
    'v.submissionDate',
    'v.revisionDate',
    'v.revisionRemarks',
    'v.versionNumber',
  ]) {
    assert.ok(HISTORY.includes(field), `${field} remains visible`)
  }
  assert.ok(HISTORY.includes(':class="statusTone(v.status)"'), 'status is a tone badge')
  assert.ok(HISTORY.includes('formatDateTime(v.submissionDate)'), 'date includes the time')
})

test('the versions API, snapshot loader and details toggle are unchanged', () => {
  assert.ok(HISTORY.includes('`/api/proposals/${props.proposalId}/versions`'))
  assert.ok(HISTORY.includes('/versions/${v.versionNumber}'))
  assert.ok(HISTORY.includes('activeVersion === v.versionNumber'))
  assert.ok(HISTORY.includes('@click="toggleDetails(v)"'))
})

test('the empty state is a clean message, not a broken card', () => {
  assert.ok(HISTORY.includes('class="sh-state sh-empty"'))
  assert.ok(HISTORY.includes('No submission history has been recorded for this proposal.'))
})

test('the history uses the full page width on both RPS detail pages', () => {
  assert.ok(DETAILS.includes('<SubmissionHistory v-if="proposal.id" :proposal-id="proposal.id" />'))
  assert.ok(FORWARD.includes('<SubmissionHistory :proposal-id="proposal.id" />'))
  assert.ok(
    !/class="side"[\s\S]{0,200}?<SubmissionHistory/.test(FORWARD),
    'the history is no longer squeezed into the 360px side column',
  )
})

test('metadata reflows instead of forcing four narrow columns', () => {
  assert.match(HISTORY, /\.sh-meta-grid \{[\s\S]*?repeat\(auto-fit, minmax\(190px, 1fr\)\)/)
  assert.match(HISTORY, /@media \(max-width: 1024px\)[\s\S]*?\.sh-meta-grid \{[\s\S]*?repeat\(2, minmax\(0, 1fr\)\)/)
  assert.match(HISTORY, /@media \(max-width: 640px\)[\s\S]*?\.sh-meta-grid \{[\s\S]*?grid-template-columns: minmax\(0, 1fr\)/)
})
