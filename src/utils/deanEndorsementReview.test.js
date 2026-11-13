/**
 * deanEndorsementReview.test.js
 * ---------------------------------------------------------------------------
 * Guards the Dean's endorsement review screen (src/views/Dean/DeanEndorsementReview.vue).
 *
 * The page is a Vue component, so nothing type-checks the mistake that made "Approve and
 * Issue Endorsement" do nothing at all: the page awaited showConfirm() but never rendered
 * the <ConfirmDialog> that resolves it, so the confirmation promise stayed pending for
 * ever and approve() (and reject(), and reissue()) were never called - no request was
 * sent, no error was shown, no loading state appeared. The file is read as text here - the
 * technique proponentDashboard.test.js uses - and each check states the mistake it
 * prevents.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

const PAGE = readFileSync(
  fileURLToPath(new URL('../views/Dean/DeanEndorsementReview.vue', import.meta.url)),
  'utf8',
)

test('the page renders the confirm dialog its actions await', () => {
  // showConfirm() is resolved only from the dialog's own buttons: without the host the
  // decision never happens, silently.
  assert.match(
    PAGE,
    /import ConfirmDialog from '@\/components\/ConfirmDialog\.vue'/,
    'the dialog component must be imported',
  )
  assert.match(
    PAGE,
    /const \{[^}]*\bdialogState\b[^}]*\} = useDialog\(\)/,
    'the dialog state must be taken from useDialog()',
  )
  assert.match(PAGE, /<ConfirmDialog/, 'the dialog must be rendered')
  assert.match(PAGE, /@confirm="dialogState\.onConfirm"/, 'confirming must resolve the promise')
  assert.match(PAGE, /@cancel="dialogState\.onCancel"/, 'cancelling must resolve the promise')
  assert.match(PAGE, /@close="dialogState\.show = false"/, 'closing must hide the dialog')
})

test('every dialog-driven action is wired to its button and confirms first', () => {
  // Approving, rejecting and reissuing each confirm before they run, and each one stays
  // bound to the button that triggers it.
  for (const action of ['confirmApprove', 'confirmReject', 'confirmReissue']) {
    assert.match(
      PAGE,
      new RegExp(`async function ${action}\\(\\) \\{[\\s\\S]*?await showConfirm\\(`),
      `${action} must confirm before it acts`,
    )
    assert.match(PAGE, new RegExp(`@click="${action}"`), `${action} must stay bound to its button`)
  }
})

test('a decision cannot be submitted twice and a failure is always reported', () => {
  // While a decision is in flight every button is disabled, so a double click cannot
  // issue a second letter, and the backend message is shown when it fails.
  assert.match(PAGE, /:disabled="busy \|\| Boolean\(blockedReason\)"/, 'the approval is disabled while busy')
  assert.match(PAGE, /:disabled="busy"/, 'the other actions are disabled while busy')
  assert.match(
    PAGE,
    /actionError\.value = err\?\.response\?\.data\?\.error \|\| /,
    'the backend message must reach the user',
  )
  assert.match(
    PAGE,
    /finally \{\s*busy\.value = false/,
    'the loading state must be cleared on every outcome',
  )
})

test('the page still explains what blocks an approval', () => {
  // The electronic signature, the letter template and the status are the reasons the
  // backend refuses an approval; the screen must keep saying so before the click.
  assert.match(PAGE, /approveBlockedReason\(/, 'the blocked reason must be shown')
  assert.match(PAGE, /loadAccountSignatures\(\)/, 'the saved signature must be checked')
  assert.match(PAGE, /blockedReason/, 'the blocked reason must reach the template')
})
