/**
 * documentPreview.test.js
 * Verifies the preview decision for uploaded report documents: PDFs and images
 * are previewable (with the media type restored from the extension, because the
 * endpoint serves application/octet-stream), while DOC/DOCX and anything else
 * falls back to the "no preview available" state instead of a blank modal.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  PREVIEW_IMAGE,
  PREVIEW_PDF,
  PREVIEW_UNSUPPORTED,
  fileExtension,
  fileTypeLabel,
  previewKindOf,
  previewMimeType,
} from './documentPreview.js'

test('a PDF is previewed by the browser PDF viewer', () => {
  assert.equal(previewKindOf('Terminal Report.pdf'), PREVIEW_PDF)
  assert.equal(previewMimeType('Terminal Report.pdf'), 'application/pdf')
})

test('the extension check ignores case, spaces and dotted file names', () => {
  assert.equal(fileExtension('  Report.PDF  '), 'pdf')
  assert.equal(previewKindOf('Report.PDF'), PREVIEW_PDF)
  assert.equal(previewKindOf('my.project.terminal.v2.pdf'), PREVIEW_PDF)
  assert.equal(previewMimeType('Report.PDF'), 'application/pdf')
})

test('image documents are previewable with their own media type', () => {
  const cases = [
    ['scan.png', 'image/png'],
    ['scan.PNG', 'image/png'],
    ['photo.jpg', 'image/jpeg'],
    ['photo.jpeg', 'image/jpeg'],
    ['animation.gif', 'image/gif'],
    ['photo.webp', 'image/webp'],
    ['photo.bmp', 'image/bmp'],
    ['diagram.svg', 'image/svg+xml'],
  ]

  for (const [fileName, mime] of cases) {
    assert.equal(previewKindOf(fileName), PREVIEW_IMAGE, fileName)
    assert.equal(previewMimeType(fileName), mime, fileName)
  }
})

test('DOC and DOCX files report no in-browser preview', () => {
  assert.equal(previewKindOf('Terminal Report.docx'), PREVIEW_UNSUPPORTED)
  assert.equal(previewKindOf('Terminal Report.doc'), PREVIEW_UNSUPPORTED)
  assert.equal(previewMimeType('Terminal Report.docx'), '')
  assert.equal(previewMimeType('Terminal Report.doc'), '')
})

test('unknown or missing extensions are unsupported rather than blank', () => {
  const cases = ['archive.zip', 'report', '', null, undefined, 'report.', '.pdf', '   ']
  for (const fileName of cases) {
    assert.equal(previewKindOf(fileName), PREVIEW_UNSUPPORTED, String(fileName))
    assert.equal(previewMimeType(fileName), '', String(fileName))
  }
})

test('the file-type label is the upper-case extension, or FILE when there is none', () => {
  assert.equal(fileTypeLabel('Terminal Report.pdf'), 'PDF')
  assert.equal(fileTypeLabel('Terminal Report.docx'), 'DOCX')
  assert.equal(fileTypeLabel('scan.PNG'), 'PNG')
  assert.equal(fileTypeLabel('report'), 'FILE')
  assert.equal(fileTypeLabel(''), 'FILE')
  assert.equal(fileTypeLabel(null), 'FILE')
})
