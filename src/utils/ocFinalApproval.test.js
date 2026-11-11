/**
 * ocFinalApproval.test.js
 * ---------------------------------------------------------------------------
 * Source-level guards for the OC final-approval page after the "Download Proposal
 * Details" and "Save Draft" features were added.
 *
 * The page imports Vue and a Vite `?inline` asset indirectly, so it is read as
 * text (the technique the other view tests use). Each check states the mistake it
 * prevents: a draft that accidentally finalizes the approval, a draft that loses
 * the entered values, a download that prints a different proposal, or the
 * existing final approval being replaced.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const PAGE_FILE = fileURLToPath(new URL('../views/Oc/OCFinalApproval.vue', import.meta.url))
const PAGE = readFileSync(PAGE_FILE, 'utf8')

test('the existing final approval flow is preserved', () => {
  assert.ok(
    PAGE.includes('`/api/proposals/${proposalId}/approve-with-so`'),
    'the final approval endpoint must not be replaced by the draft endpoint',
  )
  assert.ok(PAGE.includes('Confirm Approval'), 'the final approval action must still exist')
  assert.match(PAGE, /setStatus|approve-with-so/, 'the approval request must remain')
})

test('the SO form has a Save Draft action that calls the draft endpoint', () => {
  assert.ok(PAGE.includes('Save Draft'), 'the modal must offer a Save Draft button')
  assert.match(PAGE, /async saveDraft\(\)/, 'the draft save handler must exist')
  assert.ok(
    PAGE.includes('`/api/proposals/${proposalId}/special-order-draft`'),
    'the draft must be saved through the dedicated endpoint',
  )
  assert.ok(
    PAGE.includes("formData.append('removeSoFile', 'true')"),
    'removing a saved draft document must be sent to the server',
  )
})

test('a draft never triggers the approval workflow', () => {
  const start = PAGE.indexOf('async saveDraft()')
  assert.ok(start >= 0)
  const end = PAGE.indexOf('async downloadDetails()', start)
  const saveDraftSource = PAGE.slice(start, end === -1 ? PAGE.length : end)

  assert.ok(
    !saveDraftSource.includes('approve-with-so'),
    'saving a draft must not call the final approval endpoint',
  )
  assert.ok(
    !saveDraftSource.includes('soUploadedAt: new Date') &&
      !saveDraftSource.includes('setSoUploadedAt'),
    'a draft must never record a new SO date (it only merges the server response)',
  )
  assert.ok(
    !saveDraftSource.includes('showSuccess = true'),
    'a draft is not a completed approval and must not show its success state',
  )
})

test('a saved draft is restored when the page reopens', () => {
  assert.match(PAGE, /import \{ isSoDraft, soDraftFileLabel, draftSnapshot \} from '@\/utils\/soDraft'/,
    'the shared draft rule must be used')
  assert.ok(PAGE.includes('applyProposalPrefill()'), 'the form must be restored from the proposal')
  assert.ok(PAGE.includes('this.chancellorNotes = draft ? this.proposal.remarks || \'\' : \'\''),
    'notes are prefilled only for a draft, so REC comments are never misattributed')
  assert.ok(PAGE.includes('this.soNumber = draft ? this.proposal.soNumber || \'\' : \'\''),
    'the saved SO number must be restored')
  assert.ok(PAGE.includes('savedSoFileName'), 'a saved draft document must be shown')
  assert.ok(PAGE.includes('downloadUploadedFile'), 'the saved document must be downloadable')
})

test('closing a form with unsaved values asks before discarding them', () => {
  assert.match(PAGE, /import ConfirmDialog from '@\/components\/ConfirmDialog\.vue'/)
  assert.ok(PAGE.includes('showDiscardConfirm'), 'the discard confirmation must exist')
  assert.ok(PAGE.includes('formDirty'), 'the page must compare the form with its saved snapshot')
  assert.ok(PAGE.includes('formSnapshot = this.formSnapshotValue'),
    'the snapshot must be refreshed after a successful save')
  assert.ok(PAGE.includes('Discard unsaved changes?'), 'the confirmation must explain the loss')
})

test('reports draft success and failure without changing the approval messages', () => {
  assert.ok(PAGE.includes('draftMessage'), 'a successful draft save must be reported')
  assert.ok(PAGE.includes('draftError'), 'a failed draft save must be reported')
  assert.ok(PAGE.includes('this.approvalError = `Approval failed: ${errorMsg}`'),
    'the existing approval error path must remain')
  assert.ok(
    PAGE.includes('Enter the SO number or attach the SO document before saving a draft.'),
    'the client must explain the draft rule before a request is sent',
  )
})

test('the proposal details download uses the shared document and its current record', () => {
  assert.match(
    PAGE,
    /import \{ downloadProposalRecord \} from '@\/utils\/proposalDetailsDocument'/,
    'the document generator must be the shared module',
  )
  assert.ok(PAGE.includes('Download Proposal Details'), 'the page must offer the download action')
  assert.ok(
    PAGE.includes('await downloadProposalRecord({'),
    'the shared entry point must render the document',
  )

  const start = PAGE.indexOf('async downloadDetails()')
  assert.ok(start >= 0)
  const source = PAGE.slice(start, PAGE.indexOf('chooseBudget(', start))
  assert.ok(source.includes('proposalId,'), 'the proposal must be re-read by id inside the shared builder')
  assert.ok(source.includes('remarks: this.remarksResponse'), 'the field remarks must be printed')
})

test('the budget options are one exclusive radio group, never two selections', () => {
  assert.equal((PAGE.match(/name="budget-classification"/g) || []).length, 2,
    'both options must belong to the same native radio group')
  assert.equal((PAGE.match(/v-model="needsBudget"/g) || []).length, 2,
    'both options must write the one classification model')
  assert.equal((PAGE.match(/:value="true"/g) || []).length, 1,
    'With Budget must set the model to true')
  assert.equal((PAGE.match(/:value="false"/g) || []).length, 1,
    'Without Budget must set the model to false')
  assert.ok(!PAGE.includes('@click="chooseBudget'),
    'a card click must not run its own independent toggle; the radio group owns exclusivity')
  assert.ok(PAGE.includes('@change="chooseBudget(true)"') && PAGE.includes('@change="chooseBudget(false)"'),
    'each change must record the one chosen classification')
})

test('selecting a classification records it only; the explicit Approve action confirms', () => {
  const start = PAGE.indexOf('chooseBudget(value) {')
  assert.ok(start >= 0, 'the chooseBudget handler must exist')
  const end = PAGE.indexOf('openApproval() {', start)
  const source = PAGE.slice(start, end)
  assert.ok(!source.includes('api.put'), 'choosing a classification must not send a request')
  assert.ok(!source.includes('this.showConfirm = true'), 'choosing must not execute the decision')
  assert.ok(source.includes('rememberBudget('), 'the in-progress choice is remembered for a refresh')
})

test('the saved classification is restored when the proposal is reopened or refreshed', () => {
  assert.ok(PAGE.includes('const storedClassification = this.proposal.needsBudget'),
    'the stored needs_budget value is read')
  assert.ok(PAGE.includes('storedClassification === true || storedClassification === false'),
    'only a real saved boolean is restored')
  assert.ok(PAGE.includes('readRememberedBudget(this.proposal.id)'),
    'an unsaved selection survives a refresh')
})

test('Approve for Implementation is enabled by a chosen classification and opens the confirmation', () => {
  assert.ok(PAGE.includes('@click="openApproval"'), 'Approve must be attached to its handler')
  assert.ok(PAGE.includes(':disabled="busy || needsBudget === null"'),
    'it is disabled only while busy or before a classification is chosen')
  assert.ok(PAGE.includes('approvalActionLabel'), 'the label names the selected routing')

  const start = PAGE.indexOf('openApproval() {')
  const source = PAGE.slice(start, PAGE.indexOf('openReturn()', start))
  assert.ok(source.includes('this.needsBudget !== true && this.needsBudget !== false'),
    'no confirmation may open without one valid classification')
  assert.ok(source.includes('this.showConfirm = true'), 'the confirmation dialog is the execution point')
  assert.ok(!source.includes('api.put'), 'the button must not send the approval directly')

  const confirm = PAGE.slice(PAGE.indexOf('async confirmApproval()'), PAGE.indexOf('async returnToOvcrige()'))
  assert.ok(confirm.includes('this.needsBudget !== true && this.needsBudget !== false'),
    'an ambiguous request is refused on the client as well')
  assert.ok(confirm.includes('formData.append(\'needsBudget\', this.needsBudget)'),
    'exactly one classification is sent')
})

test('the confirmation states where each routing sends the proposal', () => {
  assert.ok(PAGE.includes('FOR_OVCAF_APPROVAL'), 'the with-budget destination must be named')
  assert.ok(PAGE.includes('<strong>APPROVED</strong>'), 'the without-budget destination must be named')
  assert.ok(PAGE.includes('This proposal will be forwarded to the Finance Office / OVCAF'),
    'the with-budget explanation must be kept')
})

test('Return to OVCRIGE uses its own endpoint and never fakes success', () => {
  assert.ok(
    PAGE.includes('`/api/proposals/${proposalId}/return-to-ovcrige`'),
    'the return must call the OC return endpoint, not the generic return-revision route',
  )
  assert.ok(!PAGE.includes('/return-revision'), 'an OC user is not allowed on return-revision')

  const start = PAGE.indexOf('async returnToOvcrige()')
  assert.ok(start >= 0)
  const end = PAGE.indexOf('handleSuccessClose()', start)
  const source = PAGE.slice(start, end)
  assert.ok(source.includes('this.returnError = `Return failed: ${errorMsg}`'),
    'a failed return must be reported as an error')
  assert.ok(!source.includes('successMessage = `Return failed'),
    'a failure must never be written into the success message')
  assert.ok(source.includes('await this.refreshProposalState()'),
    'the page must re-read the stored status after a successful return')
  assert.ok(source.includes('remarks: remarks || null'), 'the Chancellor notes must be sent as remarks')
})

test('isSoDraft is a method, so opening the confirmation cannot crash the page', () => {
  const computed = PAGE.slice(PAGE.indexOf('computed: {'), PAGE.indexOf('async mounted()'))
  assert.ok(
    !computed.includes('isSoDraft,'),
    'a computed named isSoDraft would expose a boolean, and the template call isSoDraft(proposal) would throw',
  )

  const methods = PAGE.slice(PAGE.indexOf('methods: {'), PAGE.indexOf('async loadFieldRemarks()'))
  assert.ok(
    methods.includes('isSoDraft,'),
    'the template calls isSoDraft(proposal), so it must be registered as a method',
  )
})

test('actions are confirmed, single-submission and gated by the current status', () => {
  assert.ok(PAGE.includes('showReturnConfirm'), 'the return must ask for confirmation first')
  assert.ok(PAGE.includes('if (this.busy) return'), 'a second submission must be ignored')
  assert.match(PAGE, /busy\(\) \{/, 'the page must expose the in-flight state')
  assert.ok(PAGE.includes('<template v-if="atOc">'),
    'the decision controls exist only while the proposal is awaiting the Chancellor')
  assert.ok(PAGE.includes('decision-locked'),
    'an ineligible record explains that no Chancellor action is available instead of showing dead buttons')
})
