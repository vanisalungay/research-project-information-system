/**
 * ocSpecialOrderUpload.test.js
 * ---------------------------------------------------------------------------
 * Guards the OC Special Order attachment section and the OC Approval Trail.
 *
 * The page is a Vue component, so the upload rules, the messages and the trail source
 * are checked as source text (the technique the other view tests use). The backend
 * configuration and validation are checked in the same way so the two layers cannot
 * drift apart: a 10 MB limit shown in the UI must also be the limit the server
 * enforces.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const ROOT = new URL('../../', import.meta.url)
const read = (path) => readFileSync(fileURLToPath(new URL(path, ROOT)), 'utf8')

const PAGE = read('src/views/Oc/OCFinalApproval.vue')
const SERVICE = read('backend/src/main/java/com/rpis/backend/service/ProposalService.java')
const HANDLER = read('backend/src/main/java/com/rpis/backend/config/MultipartExceptionHandler.java')
const PROPERTIES = read('backend/src/main/resources/application.properties')
const HISTORY_REPO = read(
  'backend/src/main/java/com/rpis/backend/repository/ProposalStatusHistoryRepository.java',
)

test('the upload rules are stated before a file is chosen', () => {
  for (const text of [
    'Special Order Attachment',
    'Attachment Instructions',
    'Accepted file formats:',
    'PDF, DOC, DOCX',
    'Maximum file size:',
    '10 MB',
  ]) {
    assert.ok(PAGE.includes(text), `the attachment section must show "${text}"`)
  }
  assert.ok(PAGE.includes('aria-describedby="soFileInstructions"'),
    'the control must reference the visible instructions')
  assert.ok(
    PAGE.indexOf('Attachment Instructions') < PAGE.indexOf('class="so-file-input"'),
    'the instructions must be rendered above the file control',
  )
})

test('the accepted formats and the 10 MB limit match the backend', () => {
  assert.ok(PAGE.includes('const SO_MAX_MB = 10'), 'the UI limit is 10 MB')
  assert.ok(PAGE.includes('const SO_MAX_BYTES = SO_MAX_MB * 1024 * 1024'),
    'the byte limit is derived from the megabyte limit')
  assert.ok(PAGE.includes("const SO_EXTENSIONS = ['pdf', 'doc', 'docx']"),
    'the UI accepts the same extensions as the backend')
  assert.ok(PAGE.includes("'application/pdf'") && PAGE.includes("'application/msword'"),
    'the declared media type is checked as well as the extension')

  assert.ok(PROPERTIES.includes('rpis.special-order.max-size-mb=10'),
    'the server configuration enforces 10 MB')
  assert.ok(SERVICE.includes('@Value("${rpis.special-order.max-size-mb:10}")'),
    'the service default is 10 MB')
  assert.ok(SERVICE.includes('Set.of("pdf", "doc", "docx")'),
    'the server accepts PDF, DOC and DOCX')
  assert.ok(SERVICE.includes('getSpecialOrderMaxBytes()'),
    'the server checks the size before storing the file')
  assert.ok(HANDLER.includes('@Value("${rpis.special-order.max-size-mb:10}")'),
    'the multipart rejection message names the same limit')
})

test('invalid files are rejected with specific messages before upload', () => {
  assert.ok(
    PAGE.includes(
      "Unsupported file format. Please upload a PDF, DOC, or DOCX file.",
    ),
    'an unsupported format must say which formats are accepted',
  )
  assert.ok(
    PAGE.includes('File exceeds the ${SO_MAX_MB} MB limit. Please select a smaller file.'),
    'an oversized file must name the limit and the remedy',
  )
  assert.ok(PAGE.includes('this.soFileError = SO_FORMAT_MESSAGE'),
    'a bad extension or media type sets the visible error')
  assert.ok(PAGE.includes('this.soFileError = SO_SIZE_MESSAGE'),
    'an oversized file sets the visible error')
  assert.ok(PAGE.includes('v-if="soFileError" class="so-file-error" role="alert"'),
    'the error is announced next to the control')
  assert.ok(PAGE.includes('file.size > SO_MAX_BYTES'),
    'the size is checked before the file is kept')
})

test('the selected file shows its size and the replacement is explicit', () => {
  assert.ok(PAGE.includes('{{ formatFileSize(soFile.size) }}'),
    'the selected file must display its own size')
  assert.ok(PAGE.includes('formatFileSize(bytes)'),
    'the readable size helper must exist')
  assert.ok(PAGE.includes('Choosing a new file above replaces the saved draft document.'),
    'replacing a saved draft document must be stated, not implied')
  assert.ok(PAGE.includes("formData.append('removeSoFile', 'true')"),
    'removing the saved document must still be sent to the server')
  assert.ok(PAGE.includes(':disabled="!soNumber.trim()'),
    'one submission at a time is still enforced by the disabled state')
  assert.ok(PAGE.includes('if (this.busy)'), 'a second submission is ignored while one is running')
})

test('the Approval Trail is real, audited and newest first', () => {
  assert.ok(PAGE.includes("from '@/utils/proposalTimeline.js'"),
    'the shared timeline builder must be used')
  assert.ok(PAGE.includes('buildProposalTimeline(this.proposal, this.statusHistory)'),
    'the trail is built from the proposal and its recorded history')
  assert.ok(PAGE.includes('`/api/proposals/${this.proposal.id}/status-history`'),
    'the trail reads the audited status-history endpoint')
  assert.ok(PAGE.includes('v-for="entry in approvalTrail"'),
    'the card renders the ordered entries')
  assert.ok(PAGE.includes('formatTrailTime(entry.timestamp)'),
    'each entry shows its own event time')
  assert.ok(PAGE.includes('entry.actorName') && PAGE.includes('entry.remarks'),
    'the recorded actor and remarks are preserved')
  assert.ok(PAGE.includes('await this.loadStatusHistory()'),
    'the trail is re-read after an action and on load')

  assert.ok(HISTORY_REPO.includes('findByProposalIdOrderByChangedAtDescIdDesc'),
    'the server returns history newest first by the real timestamp')
  assert.ok(HISTORY_REPO.includes('changed_at') || HISTORY_REPO.includes('ChangedAt'),
    'the ordering uses the event timestamp, not a display date')
})
