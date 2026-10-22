/**
 * budgetFile.test.js
 * ---------------------------------------------------------------------------
 * Covers the "Upload Filled Budget File" rules of the Create Proposal form
 * (section 25): the label and the two rule lines, the 10 MB limit
 * (10 x 1024 x 1024 bytes, the limit itself is still accepted), the PDF/DOCX
 * check by extension and by declared MIME type, and - the important part - the
 * content check that rejects a file whose bytes are not the format its name
 * claims (a PNG renamed to ".pdf", a PDF renamed to ".docx", ...).
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  BUDGET_FILE_DOCX_ALIAS_MIME_TYPES,
  BUDGET_FILE_EMPTY_ERROR,
  BUDGET_FILE_EXTENSIONS,
  BUDGET_FILE_FORMAT_ERROR,
  BUDGET_FILE_FORMATS_LABEL,
  BUDGET_FILE_FORMATS_VALUE,
  BUDGET_FILE_LABEL,
  BUDGET_FILE_MAX_BYTES,
  BUDGET_FILE_MIME_TYPES,
  BUDGET_FILE_SIZE_ERROR,
  BUDGET_FILE_SIZE_LABEL,
  BUDGET_FILE_SIZE_VALUE,
  budgetFileDeclaredMimeType,
  budgetFileExtension,
  budgetFileKey,
  budgetFileSizeLabel,
  budgetFileUploadError,
  clearedBudgetFileSelection,
  detectBudgetFileType,
  nextBudgetFileSelection,
  validateBudgetFile,
  verifyBudgetFile,
} from './budgetFile.js'

/** A file-like object; the rules only use `name`, `size`, `type` and `slice`. */
const fileOf = (bytes, name, type = '') => new File([new Uint8Array(bytes)], name, { type })

/** A PDF starts with "%PDF-1.4". */
const pdfBytes = (extra = 0) => [
  0x25,
  0x50,
  0x44,
  0x46,
  0x2d,
  0x31,
  0x2e,
  0x34,
  ...new Array(extra).fill(0),
]
/** A DOCX is an OOXML package: a ZIP archive ("PK" + the local file header). */
const docxBytes = (extra = 0) => [0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, ...new Array(extra).fill(0)]
/** A PNG signature - a real image, but not a budget document. */
const pngBytes = (extra = 0) => [
  0x89,
  0x50,
  0x4e,
  0x47,
  0x0d,
  0x0a,
  0x1a,
  0x0a,
  ...new Array(extra).fill(0),
]
const textBytes = (text) => [...new TextEncoder().encode(text)]

const PDF_TYPE = 'application/pdf'
const DOCX_TYPE = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'

test('the control is labelled and documents the accepted formats and the limit', () => {
  assert.equal(BUDGET_FILE_LABEL, 'Upload Filled Budget File')
  assert.equal(BUDGET_FILE_FORMATS_LABEL, 'Accepted file formats:')
  assert.equal(BUDGET_FILE_FORMATS_VALUE, 'PDF (.pdf) and Word Document (.docx)')
  assert.equal(BUDGET_FILE_SIZE_LABEL, 'Maximum file size:')
  assert.equal(BUDGET_FILE_SIZE_VALUE, '10 MB')
})

test('10 MB means 10 x 1024 x 1024 bytes and only PDF/DOCX are accepted', () => {
  assert.equal(BUDGET_FILE_MAX_BYTES, 10485760)
  assert.deepEqual(BUDGET_FILE_EXTENSIONS, ['pdf', 'docx'])
  assert.deepEqual(BUDGET_FILE_MIME_TYPES, [PDF_TYPE, DOCX_TYPE])
  assert.deepEqual(BUDGET_FILE_DOCX_ALIAS_MIME_TYPES, [
    'application/zip',
    'application/x-zip-compressed',
  ])
})

test('the two failure messages are the exact ones the form shows', () => {
  assert.equal(
    BUDGET_FILE_SIZE_ERROR,
    'File size exceeds the 10 MB limit. Please upload a smaller file.',
  )
  assert.equal(BUDGET_FILE_FORMAT_ERROR, 'Invalid file format. Please upload a PDF or DOCX file.')
  assert.equal(BUDGET_FILE_EMPTY_ERROR, 'Please choose a Line-Item Budget file first.')
})

test('the selected file size is shown in a readable unit', () => {
  assert.equal(budgetFileSizeLabel(0), '0 B')
  assert.equal(budgetFileSizeLabel(1536), '1.5 KB')
  assert.equal(budgetFileSizeLabel(2 * 1024 * 1024), '2.0 MB')
  assert.equal(budgetFileSizeLabel(BUDGET_FILE_MAX_BYTES), '10.0 MB')
})

test('the extension check ignores case and spaces', () => {
  assert.equal(budgetFileExtension('  Budget.PDF  '), 'pdf')
  assert.equal(budgetFileExtension('Line-Item Budget.DOCX'), 'docx')
  assert.equal(budgetFileExtension('no-extension'), '')
  assert.equal(budgetFileExtension(''), '')
})

test('the declared MIME type is read in lower case and may be missing', () => {
  assert.equal(budgetFileDeclaredMimeType({ type: 'Application/PDF' }), 'application/pdf')
  assert.equal(budgetFileDeclaredMimeType({ type: '' }), '')
  assert.equal(budgetFileDeclaredMimeType({}), '')
  assert.equal(budgetFileDeclaredMimeType(null), '')
})

test('a real PDF of at most 10 MB is accepted', async () => {
  const file = fileOf(pdfBytes(64), 'Line-Item Budget.pdf', PDF_TYPE)
  assert.deepEqual(validateBudgetFile(file), { valid: true, error: '' })

  const verified = await verifyBudgetFile(file)
  assert.equal(verified.valid, true)
  assert.equal(verified.error, '')
  assert.equal(verified.type, 'pdf')
  assert.equal(await detectBudgetFileType(file), 'pdf')
})

test('a real DOCX of at most 10 MB is accepted', async () => {
  const file = fileOf(docxBytes(64), 'Line-Item Budget.docx', DOCX_TYPE)
  assert.deepEqual(validateBudgetFile(file), { valid: true, error: '' })

  const verified = await verifyBudgetFile(file)
  assert.equal(verified.valid, true)
  assert.equal(verified.type, 'docx')
  assert.equal(await detectBudgetFileType(file), 'docx')
})

test('a DOCX is accepted even when the browser reports a ZIP media type', async () => {
  const verified = await verifyBudgetFile(
    fileOf(docxBytes(32), 'budget.docx', 'application/x-zip-compressed'),
  )
  assert.equal(verified.valid, true)
  assert.equal(verified.type, 'docx')
})

test('a DOCX is accepted when the browser reports no media type at all', async () => {
  const verified = await verifyBudgetFile(fileOf(docxBytes(32), 'budget.docx'))
  assert.equal(verified.valid, true)
  assert.equal(verified.type, 'docx')
})

test('10 MB is the limit itself, so a file of exactly 10 MB is still accepted', async () => {
  const bytes = pdfBytes(BUDGET_FILE_MAX_BYTES - pdfBytes().length)
  const file = fileOf(bytes, 'budget.pdf', PDF_TYPE)

  assert.equal(file.size, BUDGET_FILE_MAX_BYTES)
  assert.deepEqual(validateBudgetFile(file), { valid: true, error: '' })
  assert.equal((await verifyBudgetFile(file)).valid, true)
})

test('a file larger than 10 MB is rejected with the size message', async () => {
  const file = fileOf(pdfBytes(BUDGET_FILE_MAX_BYTES), 'budget.pdf', PDF_TYPE)

  assert.equal(file.size, BUDGET_FILE_MAX_BYTES + pdfBytes().length)
  assert.deepEqual(validateBudgetFile(file), { valid: false, error: BUDGET_FILE_SIZE_ERROR })
  const verified = await verifyBudgetFile(file)
  assert.equal(verified.valid, false)
  assert.equal(verified.error, BUDGET_FILE_SIZE_ERROR)
})

test('an unsupported extension is rejected with the format message', async () => {
  for (const name of ['scan.png', 'sheet.xlsx', 'report.doc', 'archive.zip']) {
    const file = fileOf(pngBytes(16), name)
    assert.deepEqual(
      validateBudgetFile(file),
      { valid: false, error: BUDGET_FILE_FORMAT_ERROR },
      `${name} should be rejected`,
    )
    assert.equal((await verifyBudgetFile(file)).error, BUDGET_FILE_FORMAT_ERROR)
  }
})

test('a declared type of another format is rejected even with a .docx name', async () => {
  const file = fileOf(docxBytes(32), 'budget.docx', 'image/png')
  assert.deepEqual(validateBudgetFile(file), { valid: false, error: BUDGET_FILE_FORMAT_ERROR })
})

test('a file whose bytes are not the format its name claims is rejected', async () => {
  // An image renamed to ".pdf" and a PDF renamed to ".docx" both look acceptable
  // by name and type, so only the content check can catch them.
  const fakePdf = fileOf(pngBytes(32), 'budget.pdf', PDF_TYPE)
  assert.deepEqual(validateBudgetFile(fakePdf), { valid: true, error: '' })
  assert.equal(await detectBudgetFileType(fakePdf), '')
  assert.equal((await verifyBudgetFile(fakePdf)).error, BUDGET_FILE_FORMAT_ERROR)

  const fakeDocx = fileOf(pdfBytes(32), 'budget.docx', DOCX_TYPE)
  assert.equal((await verifyBudgetFile(fakeDocx)).error, BUDGET_FILE_FORMAT_ERROR)

  const text = fileOf(textBytes('this is not a budget document'), 'budget.pdf', PDF_TYPE)
  assert.equal((await verifyBudgetFile(text)).error, BUDGET_FILE_FORMAT_ERROR)
})

test('no file at all is reported as such', async () => {
  assert.deepEqual(validateBudgetFile(null), { valid: false, error: BUDGET_FILE_EMPTY_ERROR })
  assert.equal((await verifyBudgetFile(undefined)).error, BUDGET_FILE_EMPTY_ERROR)
})

test('a valid file replaces the previous selection and clears the error', () => {
  const previous = fileOf(pdfBytes(8), 'old.pdf', PDF_TYPE)
  const chosen = fileOf(docxBytes(8), 'new.docx', DOCX_TYPE)

  const next = nextBudgetFileSelection(previous, { valid: true, error: '' }, chosen)
  assert.equal(next.file, chosen)
  assert.equal(next.error, '')
})

test('an invalid file is never accepted, so the previous selection survives', () => {
  const previous = fileOf(pdfBytes(8), 'old.pdf', PDF_TYPE)

  const next = nextBudgetFileSelection(
    previous,
    { valid: false, error: BUDGET_FILE_SIZE_ERROR },
    fileOf(pdfBytes(BUDGET_FILE_MAX_BYTES), 'huge.pdf', PDF_TYPE),
  )
  assert.equal(next.file, previous)
  assert.equal(next.error, BUDGET_FILE_SIZE_ERROR)

  const first = nextBudgetFileSelection(null, { valid: false, error: BUDGET_FILE_FORMAT_ERROR })
  assert.equal(first.file, null)
  assert.equal(first.error, BUDGET_FILE_FORMAT_ERROR)
})

test('removing the file clears both the selection and the error', () => {
  assert.deepEqual(clearedBudgetFileSelection(), { file: null, error: '' })
})

test('the same file object is identified by name, size and modification time', () => {
  const first = fileOf(pdfBytes(8), 'budget.pdf', PDF_TYPE)
  assert.equal(budgetFileKey(first), budgetFileKey(first))
  assert.notEqual(budgetFileKey(first), budgetFileKey(fileOf(pdfBytes(9), 'budget.pdf', PDF_TYPE)))
  assert.equal(budgetFileKey(null), '')
})

test('an upload failure reports the message the server sent', () => {
  assert.equal(
    budgetFileUploadError({ response: { data: { error: BUDGET_FILE_FORMAT_ERROR } } }),
    BUDGET_FILE_FORMAT_ERROR,
  )
  assert.equal(
    budgetFileUploadError({ response: { data: 'Not your proposal' } }),
    'Not your proposal',
  )
  assert.equal(
    budgetFileUploadError(new Error('network down')),
    'The Line-Item Budget file could not be uploaded. Please try again.',
  )
})
