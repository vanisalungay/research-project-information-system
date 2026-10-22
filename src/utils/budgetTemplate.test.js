/**
 * budgetTemplate.test.js
 * ---------------------------------------------------------------------------
 * Covers the Line-Item Budget template of the Create Proposal form (section 25):
 * the exact instruction the proponent must see, the label of the button that
 * opens the viewer, and the fact that the shipped file is the official PDF the
 * modal previews (so the template is never replaced by invented content).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  LINE_ITEM_BUDGET_INSTRUCTION,
  LINE_ITEM_BUDGET_TEMPLATE_BUTTON_LABEL,
  LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME,
  LINE_ITEM_BUDGET_TEMPLATE_TITLE,
  LINE_ITEM_BUDGET_TEMPLATE_URL,
} from './budgetTemplate.js'
import { PREVIEW_PDF, previewKindOf } from './documentPreview.js'

test('the instruction tells the proponent to follow the official template', () => {
  assert.equal(
    LINE_ITEM_BUDGET_INSTRUCTION,
    'Please use and follow the official Line-Item Budget Template provided above when preparing your proposed budget. Once completed, upload your filled-out budget file below.',
  )
})

test('the template is opened with the "View Line-Item Budget Template" button', () => {
  assert.equal(LINE_ITEM_BUDGET_TEMPLATE_BUTTON_LABEL, 'View Line-Item Budget Template')
  assert.equal(LINE_ITEM_BUDGET_TEMPLATE_TITLE, 'Line-Item Budget Template')
})

test('the shipped template is the official PDF and is served from public/templates', () => {
  assert.equal(LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME, 'Project-Line-Item-Budget.pdf')
  assert.equal(LINE_ITEM_BUDGET_TEMPLATE_URL, '/templates/Project-Line-Item-Budget.pdf')
  // The modal picks its viewer from the file name: a PDF is rendered by the
  // browser's own viewer, never by a hand-built table.
  assert.equal(previewKindOf(LINE_ITEM_BUDGET_TEMPLATE_FILE_NAME), PREVIEW_PDF)
})
