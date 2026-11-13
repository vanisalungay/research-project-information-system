/**
 * lineItemBudgetPreview.test.js
 * ---------------------------------------------------------------------------
 * Guards for the OVCAF Line-Item Budget preview.
 *
 * The upload field accepts PDF and DOCX. A PDF is displayed by the browser's own
 * viewer; a DOCX is converted in memory to readable HTML (the original file is never
 * modified); every other format shows an explanatory message with Download available.
 * The modal fetches the bytes through the existing authenticated /api/files route.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { crc32, deflateRawSync } from 'node:zlib'

import {
  PREVIEW_DOCX,
  PREVIEW_PDF,
  PREVIEW_UNSUPPORTED,
  previewKindForBudget,
  previewKindOf,
} from './documentPreview.js'
import { convertDocxToHtml } from './docxPreview.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')

const MODAL = read('../components/LineItemBudgetPreviewModal.vue')
const CARD = read('../views/Ovcaf/OvcafFinancialValidation.vue')
const DETAILS = read('../views/Ovcaf/OvcafProposalDetails.vue')
const VALIDATE = read('../views/Ovcaf/OvcafValidate.vue')
const DOCX_UTIL = read('./docxPreview.js')

// ---------------------------------------------------------------------------
// A minimal, real DOCX built in memory: OVCAF previews the document's own contents,
// so the conversion is exercised rather than mocked.
// ---------------------------------------------------------------------------
function buildZip(files) {
  const localParts = []
  const centralParts = []
  let offset = 0

  for (const [name, content] of Object.entries(files)) {
    const nameBytes = Buffer.from(name, 'utf8')
    const raw = Buffer.from(content, 'utf8')
    const compressed = deflateRawSync(raw)
    const checksum = crc32(raw)

    const local = Buffer.alloc(30)
    local.writeUInt32LE(0x04034b50, 0)
    local.writeUInt16LE(20, 4)
    local.writeUInt16LE(0, 6)
    local.writeUInt16LE(8, 8) // deflate
    local.writeUInt16LE(0, 10)
    local.writeUInt16LE(0, 12)
    local.writeUInt32LE(checksum, 14)
    local.writeUInt32LE(compressed.length, 18)
    local.writeUInt32LE(raw.length, 22)
    local.writeUInt16LE(nameBytes.length, 26)
    local.writeUInt16LE(0, 28)

    const central = Buffer.alloc(46)
    central.writeUInt32LE(0x02014b50, 0)
    central.writeUInt16LE(20, 4)
    central.writeUInt16LE(20, 6)
    central.writeUInt16LE(0, 8)
    central.writeUInt16LE(8, 10)
    central.writeUInt16LE(0, 12)
    central.writeUInt16LE(0, 14)
    central.writeUInt32LE(checksum, 16)
    central.writeUInt32LE(compressed.length, 20)
    central.writeUInt32LE(raw.length, 24)
    central.writeUInt16LE(nameBytes.length, 28)
    central.writeUInt16LE(0, 30)
    central.writeUInt16LE(0, 32)
    central.writeUInt16LE(0, 34)
    central.writeUInt16LE(0, 36)
    central.writeUInt32LE(0, 38)
    central.writeUInt32LE(offset, 42)

    localParts.push(local, nameBytes, compressed)
    centralParts.push(central, nameBytes)
    offset += local.length + nameBytes.length + compressed.length
  }

  const centralOffset = offset
  const centralSize = centralParts.reduce((total, part) => total + part.length, 0)
  const count = Object.keys(files).length

  const end = Buffer.alloc(22)
  end.writeUInt32LE(0x06054b50, 0)
  end.writeUInt16LE(0, 4)
  end.writeUInt16LE(0, 6)
  end.writeUInt16LE(count, 8)
  end.writeUInt16LE(count, 10)
  end.writeUInt32LE(centralSize, 12)
  end.writeUInt32LE(centralOffset, 16)
  end.writeUInt16LE(0, 20)

  return Buffer.concat([...localParts, ...centralParts, end])
}

function buildDocx() {
  const documentXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>Community-Based Mangrove Restoration</w:t></w:r></w:p>
    <w:p><w:r><w:t>Line-Item Budget</w:t></w:r></w:p>
    <w:tbl>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Item</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>Amount</w:t></w:r></w:p></w:tc>
      </w:tr>
      <w:tr>
        <w:tc><w:p><w:r><w:t>Water quality testing kits</w:t></w:r></w:p></w:tc>
        <w:tc><w:p><w:r><w:t>12,500.00</w:t></w:r></w:p></w:tc>
      </w:tr>
    </w:tbl>
  </w:body>
</w:document>`

  return buildZip({
    '[Content_Types].xml': `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>`,
    'word/document.xml': documentXml,
  })
}

function arrayBufferOf(buffer) {
  return buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength)
}

test('the budget preview maps each accepted format to its preview method', () => {
  assert.equal(previewKindForBudget('line-item-budget.pdf'), PREVIEW_PDF)
  assert.equal(previewKindForBudget('Line-Item Budget.DOCX'), PREVIEW_DOCX)
  assert.equal(previewKindForBudget('budget.xlsx'), PREVIEW_UNSUPPORTED)
  assert.equal(previewKindForBudget(''), PREVIEW_UNSUPPORTED)
})

test('the other report modals keep their existing DOCX fallback', () => {
  // previewKindOf is shared by the Terminal Report and template modals; enabling
  // DOCX only for the budget preview must not change those screens.
  assert.equal(previewKindOf('line-item-budget.docx'), PREVIEW_UNSUPPORTED)
})

test('a DOCX is converted to readable HTML from the document itself', async () => {
  const html = await convertDocxToHtml(arrayBufferOf(buildDocx()))
  assert.match(html, /Community-Based Mangrove Restoration/)
  assert.match(html, /Water quality testing kits/)
  assert.match(html, /12,500\.00/)
  assert.match(html, /<table>/, 'budget tables stay readable')
})

test('the DOCX converter loads the library lazily', () => {
  assert.ok(DOCX_UTIL.includes("import('mammoth')"), 'the dependency is code-split')
  assert.ok(DOCX_UTIL.includes('arrayBuffer'), 'the uploaded bytes are read in memory')
})

test('the modal is titled and shows the uploaded file name', () => {
  assert.ok(MODAL.includes('Line-Item Budget Preview'), 'the required modal title')
  assert.ok(MODAL.includes('{{ displayName }}'), 'the uploaded file name')
  assert.ok(MODAL.includes('aria-label="Close Line-Item Budget preview"'), 'the × action')
})

test('the modal previews PDF, DOCX, images and explains unsupported formats', () => {
  assert.ok(MODAL.includes('PREVIEW_PDF'), 'PDF uses the browser viewer')
  assert.ok(MODAL.includes('lib-preview-frame'), 'in an iframe')
  assert.ok(MODAL.includes('PREVIEW_DOCX'), 'DOCX has its own preview path')
  assert.ok(MODAL.includes('convertDocxToHtml('), 'the actual document is converted')
  assert.ok(MODAL.includes('sanitizeDocxHtml('), 'the converted HTML is sanitised')
  assert.ok(!MODAL.includes('v-html'), 'no raw HTML is injected')
  assert.ok(MODAL.includes('In-browser preview is not available'), 'the unsupported message')
  assert.ok(MODAL.includes('previewLoading'), 'a loading state')
  assert.ok(MODAL.includes('Try again'), 'a failed request can be retried')
})

test('the modal fetches the authenticated file and releases its object URL', () => {
  assert.ok(MODAL.includes('fetchUploadedFileBlob('), 'the existing /api/files route')
  assert.ok(MODAL.includes('releaseObjectUrl('), 'blob URLs are revoked')
  assert.ok(MODAL.includes('onBeforeUnmount'), 'including when the modal closes')
  assert.ok(!MODAL.includes('window.open'), 'no navigation or new tab')
})

test('the modal closes with ×, Escape and the overlay, and downloads the original', () => {
  assert.ok(MODAL.includes("@click.self=\"emit('close')\""), 'clicking outside closes')
  assert.ok(MODAL.includes("event.key === 'Escape'"), 'Escape closes')
  assert.ok(MODAL.includes('downloadUploadedFile('), 'Download fetches the same file')
  assert.ok(MODAL.includes('downloadUploadedFile(props.filePath, displayName.value)'), 'with its name')
})

test('every OVCAF budget View action opens the modal instead of a new tab', () => {
  for (const [name, source] of Object.entries({ CARD, DETAILS, VALIDATE })) {
    assert.ok(source.includes('LineItemBudgetPreviewModal'), `${name} imports the modal`)
    assert.ok(source.includes('showBudgetPreview.value = true'), `${name} opens it on View`)
    assert.ok(source.includes(':file-path="budgetDocumentPath('), `${name} passes the stored path`)
    assert.ok(source.includes(':file-name="budgetFileName"'), `${name} passes the uploaded name`)
    assert.ok(!source.includes('previewUploadedFile('), `${name} no longer opens a new tab`)
  }
})
