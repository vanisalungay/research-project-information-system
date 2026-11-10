/**
 * endorsementDocument.test.js
 * ---------------------------------------------------------------------------
 * The "Endorsed By" upload of the Certification & Endorsement step: the endorsement
 * document is a PDF only. These are executed assertions on the real rules - the label and
 * the helper line, the 10 MB limit (the limit itself is still accepted), the extension
 * and declared-MIME checks, and - the important part - the content check that rejects a
 * PNG/JPG/DOCX, and an image renamed to ".pdf" because its bytes are not a PDF.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'

import {
  ENDORSEMENT_DOCUMENT_ACCEPT,
  ENDORSEMENT_DOCUMENT_EMPTY_ERROR,
  ENDORSEMENT_DOCUMENT_EXTENSIONS,
  ENDORSEMENT_DOCUMENT_FORMAT_ERROR,
  ENDORSEMENT_DOCUMENT_HELPER,
  ENDORSEMENT_DOCUMENT_INSTRUCTION,
  ENDORSEMENT_DOCUMENT_LABEL,
  ENDORSEMENT_DOCUMENT_MAX_BYTES,
  ENDORSEMENT_DOCUMENT_MIME_TYPES,
  ENDORSEMENT_DOCUMENT_REQUIRED_ERROR,
  ENDORSEMENT_DOCUMENT_SIZE_ERROR,
  ENDORSEMENT_DOCUMENT_SIZE_LABEL,
  ENDORSEMENT_DOCUMENT_SIZE_VALUE,
  detectEndorsementDocumentType,
  draftEndorsementFields,
  endorsementDocumentDeclaredMimeType,
  endorsementDocumentExtension,
  endorsementDocumentKey,
  endorsementDocumentUploadError,
  isPdfBytes,
  nextEndorsementDocumentSelection,
  validateEndorsementDocument,
  verifyEndorsementDocument
} from './endorsementDocument.js'

const read = (path) => readFileSync(fileURLToPath(new URL(path, import.meta.url)), 'utf8')
const readBackend = (path) =>
  readFileSync(fileURLToPath(new URL('../../backend/' + path, import.meta.url)), 'utf8')

const STEP2 = read('../views/Proponent/ProponentSubmitProp2.vue')
const STEP4 = read('../views/Proponent/ProponentSubmitProp3.vue')
const PARENT = read('../views/Proponent/ProponentProposals.vue')

/** A file-like object; the rules only use `name`, `size`, `type` and `slice`. */
const fileOf = (bytes, name, type = '') => new File([new Uint8Array(bytes)], name, { type })

/** A PDF starts with "%PDF-1.4". */
const pdfBytes = (extra = 0) => [
  0x25, 0x50, 0x44, 0x46, 0x2d, 0x31, 0x2e, 0x34, ...new Array(extra).fill(0),
]
/** A PNG signature - a real image. */
const pngBytes = (extra = 0) => [
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, ...new Array(extra).fill(0),
]
/** A JPEG starts with FF D8 FF. */
const jpegBytes = (extra = 0) => [0xff, 0xd8, 0xff, 0xe0, ...new Array(extra).fill(0)]
/** A DOCX is an OOXML package: a ZIP archive ("PK" + the local file header). */
const docxBytes = (extra = 0) => [0x50, 0x4b, 0x03, 0x04, 0x14, 0x00, ...new Array(extra).fill(0)]
const textBytes = (text) => [...new TextEncoder().encode(text)]

const PDF_TYPE = 'application/pdf'

test('the control is labelled as a PDF-only endorsement document', () => {
  assert.equal(ENDORSEMENT_DOCUMENT_LABEL, 'Endorsement Document')
  assert.equal(ENDORSEMENT_DOCUMENT_INSTRUCTION, 'Upload Endorsement Document (PDF only)')
  assert.equal(ENDORSEMENT_DOCUMENT_HELPER, 'Accepted file format: PDF (.pdf)')
  assert.equal(ENDORSEMENT_DOCUMENT_SIZE_LABEL, 'Maximum file size:')
  assert.equal(ENDORSEMENT_DOCUMENT_SIZE_VALUE, '10 MB')
  assert.deepEqual(ENDORSEMENT_DOCUMENT_EXTENSIONS, ['pdf'])
  assert.deepEqual(ENDORSEMENT_DOCUMENT_MIME_TYPES, ['application/pdf'])
  assert.equal(ENDORSEMENT_DOCUMENT_ACCEPT, '.pdf,application/pdf')
  assert.equal(ENDORSEMENT_DOCUMENT_MAX_BYTES, 10 * 1024 * 1024)
  assert.match(ENDORSEMENT_DOCUMENT_REQUIRED_ERROR, /PDF \(\.pdf\)/)
})

test('the extension of a file name is read case-insensitively', () => {
  assert.equal(endorsementDocumentExtension('endorsement.pdf'), 'pdf')
  assert.equal(endorsementDocumentExtension('ENDORSEMENT.PDF'), 'pdf')
  assert.equal(endorsementDocumentExtension('endorsement.png'), 'png')
  assert.equal(endorsementDocumentExtension('endorsement'), '')
  assert.equal(endorsementDocumentExtension(null), '')
})

test('only a real PDF header is recognized as PDF content', () => {
  assert.equal(isPdfBytes(new Uint8Array(pdfBytes())), true)
  assert.equal(isPdfBytes(new Uint8Array(pngBytes())), false)
  assert.equal(isPdfBytes(new Uint8Array(jpegBytes())), false)
  assert.equal(isPdfBytes(new Uint8Array(docxBytes())), false)
  assert.equal(isPdfBytes(new Uint8Array(textBytes('%PDF'))), false, 'a truncated header is not enough')
})

test('a valid PDF passes every check', async () => {
  const file = fileOf(pdfBytes(64), 'endorsement-letter.pdf', PDF_TYPE)
  assert.deepEqual(validateEndorsementDocument(file), { valid: true, error: '' })
  assert.deepEqual(await verifyEndorsementDocument(file), { valid: true, error: '', type: 'pdf' })
})

test('the declared MIME type does not have to be present, but must not contradict the PDF', () => {
  // Some browsers report nothing (or the generic type) for a .pdf - that is accepted,
  // because the bytes decide.
  assert.equal(validateEndorsementDocument(fileOf(pdfBytes(), 'e.pdf', '')).valid, true)
  assert.equal(validateEndorsementDocument(fileOf(pdfBytes(), 'e.pdf', 'application/octet-stream')).valid, true)
  assert.equal(endorsementDocumentDeclaredMimeType(fileOf(pdfBytes(), 'e.pdf', 'IMAGE/PNG')), 'image/png')
  // A file named .pdf that declares an image type is refused before it is even read.
  assert.equal(validateEndorsementDocument(fileOf(pdfBytes(), 'e.pdf', 'image/png')).valid, false)
})

test('a PNG, a JPG and a DOCX are refused', () => {
  for (const [name, type] of [['endorsement.png', 'image/png'], ['endorsement.jpg', 'image/jpeg']]) {
    const result = validateEndorsementDocument(fileOf(pngBytes(), name, type))
    assert.equal(result.valid, false, `${name} is refused`)
    assert.equal(result.error, ENDORSEMENT_DOCUMENT_FORMAT_ERROR)
  }
  const docx = validateEndorsementDocument(
    fileOf(
      docxBytes(),
      'endorsement.docx',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    )
  )
  assert.equal(docx.valid, false)
  assert.equal(docx.error, ENDORSEMENT_DOCUMENT_FORMAT_ERROR)
})

test('an image renamed to ".pdf" is refused on its content', async () => {
  const renamed = fileOf(pngBytes(), 'endorsement.pdf', PDF_TYPE)
  // The extension and the declared type look right, so only the bytes can catch it.
  assert.equal(validateEndorsementDocument(renamed).valid, true)
  const result = await verifyEndorsementDocument(renamed)
  assert.equal(result.valid, false)
  assert.equal(result.error, ENDORSEMENT_DOCUMENT_FORMAT_ERROR)
  assert.equal(result.type, '')
})

test('a DOCX renamed to ".pdf" and a text file are refused too', async () => {
  assert.equal((await verifyEndorsementDocument(fileOf(docxBytes(), 'e.pdf', PDF_TYPE))).valid, false)
  assert.equal((await verifyEndorsementDocument(fileOf(textBytes('not a pdf'), 'e.pdf', PDF_TYPE))).valid, false)
  assert.equal(await detectEndorsementDocumentType(fileOf(pdfBytes(), 'e.pdf', PDF_TYPE)), 'pdf')
})

test('the size limit is 10 MB and the limit itself is still accepted', () => {
  const tooBig = fileOf([], 'e.pdf', PDF_TYPE)
  Object.defineProperty(tooBig, 'size', { value: ENDORSEMENT_DOCUMENT_MAX_BYTES + 1 })
  const result = validateEndorsementDocument(tooBig)
  assert.equal(result.valid, false)
  assert.equal(result.error, ENDORSEMENT_DOCUMENT_SIZE_ERROR)

  const exactly = fileOf([], 'e.pdf', PDF_TYPE)
  Object.defineProperty(exactly, 'size', { value: ENDORSEMENT_DOCUMENT_MAX_BYTES })
  assert.equal(validateEndorsementDocument(exactly).valid, true, 'the limit itself passes')
})

test('no file chosen is reported as such', () => {
  assert.deepEqual(validateEndorsementDocument(null), {
    valid: false,
    error: ENDORSEMENT_DOCUMENT_EMPTY_ERROR,
  })
})

test('a refused file never replaces the PDF already chosen', () => {
  const chosen = fileOf(pdfBytes(), 'good.pdf', PDF_TYPE)
  const next = nextEndorsementDocumentSelection(chosen, { valid: false, error: 'nope' }, null)
  assert.equal(next.file, chosen, 'the previous selection stays')
  assert.equal(next.error, 'nope')

  const accepted = nextEndorsementDocumentSelection(chosen, { valid: true, error: '' }, chosen)
  assert.equal(accepted.file, chosen)
  assert.equal(accepted.error, '')
})

test('the file object is identified for the one-time upload', () => {
  const file = fileOf(pdfBytes(), 'e.pdf', PDF_TYPE)
  assert.equal(endorsementDocumentKey(file), `${file.name}:${file.size}:${file.lastModified}`)
  assert.equal(endorsementDocumentKey(null), '')
})

test('a reopened draft keeps the two document references the certification step reads', () => {
  assert.deepEqual(
    draftEndorsementFields({
      endorsementFileName: 'Dean Letter.pdf',
      endorsementFilePath: 'endorsement-documents/abc.pdf',
    }),
    {
      endorsementFileName: 'Dean Letter.pdf',
      endorsementFilePath: 'endorsement-documents/abc.pdf',
    }
  )
  // Nothing stored yet: the step gets two empty strings, never undefined.
  const empty = { endorsementFileName: '', endorsementFilePath: '' }
  assert.deepEqual(draftEndorsementFields(null), empty)
  assert.deepEqual(draftEndorsementFields({}), empty)
  // A half-filled record keeps whichever reference it has.
  assert.deepEqual(draftEndorsementFields({ endorsementFilePath: 'endorsement-documents/x.pdf' }), {
    endorsementFileName: '',
    endorsementFilePath: 'endorsement-documents/x.pdf',
  })
})

test('a failed upload reports the backend reason when there is one', () => {
  assert.equal(
    endorsementDocumentUploadError({ response: { data: { error: 'Invalid file format.' } } }),
    'Invalid file format.'
  )
  assert.match(endorsementDocumentUploadError(new Error('boom')), /could not be uploaded/)
})

// ======================= how the field is wired =======================

test('the form asks for the endorsement document and filters the dialog for PDFs', () => {
  // The template half, so every assertion below is about what the proponent actually
  // sees rather than about the imported constant.
  const template = STEP2.slice(0, STEP2.indexOf('<script'))
  assert.ok(STEP2.includes('@/utils/endorsementDocument'), 'the form uses the shared rules')
  assert.ok(template.includes(':accept="ENDORSEMENT_DOCUMENT_ACCEPT"'), 'the dialog filters for PDF files')
  assert.ok(template.includes('ENDORSEMENT_DOCUMENT_LABEL'), 'the field is labelled')
  assert.ok(template.includes('ENDORSEMENT_DOCUMENT_INSTRUCTION'), 'the instruction is displayed')
  assert.ok(template.includes('ENDORSEMENT_DOCUMENT_HELPER'), 'the accepted format is displayed')
  assert.ok(template.includes('ENDORSEMENT_DOCUMENT_SIZE_LABEL'), 'and the size limit')
  // The only remaining image accept is the "Submitted By" signature; the endorsement
  // document no longer accepts an image.
  assert.equal(
    (STEP2.match(/accept="image\/\*,.pdf"/g) || []).length,
    1,
    'the endorsement document no longer accepts images'
  )
  assert.ok(STEP2.includes('verifyEndorsementDocument(file)'), 'the chosen file is validated on its bytes')
  assert.ok(STEP2.includes('nextEndorsementDocumentSelection('), 'a refused file never replaces the chosen one')
  assert.ok(STEP2.includes('errors.endorsed_document'), 'the field has its own validation message')
})

test('a stored document satisfies the field and can be viewed and downloaded', () => {
  assert.ok(STEP2.includes('hasSavedEndorsement'), 'the form knows about the stored document')
  assert.ok(STEP2.includes('savedEndorsementFileName'), 'it shows its name')
  assert.ok(STEP2.includes('previewUploadedFile('), 'View opens it')
  assert.ok(STEP2.includes('downloadUploadedFile('), 'Download saves it')
  assert.ok(
    PARENT.includes(':saved-endorsement-file-path="proposalData.endorsementFilePath || \'\'"'),
    'the loaded proposal is handed to the step'
  )
})

test('reopening a draft hands the stored document back to the certification step', () => {
  const openDraft = PARENT.slice(
    PARENT.indexOf('const openDraftForEdit'),
    PARENT.indexOf('const filteredProposals')
  )
  assert.ok(openDraft.length > 0, 'the draft workflow is present')
  assert.ok(openDraft.includes('const loaded = res.data'), 'the fetched draft keeps its own reference')
  assert.ok(
    openDraft.includes('draftEndorsementFields(loaded)'),
    'its document name and path are carried into the workflow state'
  )
  assert.ok(
    !openDraft.includes('const proposalData = res.data'),
    'the local must not shadow the workflow ref the steps read'
  )
})

test('the PDF is uploaded right after the proposal is saved', () => {
  assert.ok(STEP4.includes('uploadEndorsementDocumentIfNeeded('), 'step 4 attaches it')
  assert.ok(STEP4.includes('await uploadEndorsementDocumentIfNeeded(proposalId)'), 'it is awaited')
  assert.ok(STEP4.includes('proposalData?.signatureFiles?.endorsed'), 'the file chosen in step 3 is the one uploaded')
  assert.ok(STEP4.includes('endorsementDocumentKey(file)'), 'the same file is uploaded only once')
})

test('the backend endpoint validates the PDF itself', () => {
  const controller = readBackend('src/main/java/com/rpis/backend/controller/ProposalController.java')
  assert.ok(controller.includes('"/{id}/endorsement-document"'), 'the endpoint exists')
  assert.ok(controller.includes('attachEndorsementDocument(id, file)'), 'and delegates to the service')

  const service = readBackend('src/main/java/com/rpis/backend/service/ProposalService.java')
  assert.ok(service.includes('requireEndorsementPdf(file, bytes)'), 'the service validates the file')
  assert.ok(service.includes('ENDORSEMENT_DOCUMENT_MAX_BYTES'), 'the size limit is enforced')
  assert.ok(service.includes('PDF_HEADER'), 'the real PDF header is checked')
  assert.ok(service.includes('new PdfReader(bytes)'), 'and the document is opened, so a corrupted PDF is refused')
  assert.ok(service.includes('ENDORSEMENT_DOCUMENT_DIR'), 'the file is stored under its own folder')

  const security = readBackend('src/main/java/com/rpis/backend/config/SecurityConfig.java')
  assert.ok(
    security.includes('"/api/proposals/*/endorsement-document"'),
    'uploading is restricted to the Proponent role'
  )
  const files = readBackend('src/main/java/com/rpis/backend/controller/FileController.java')
  assert.ok(files.includes('getEndorsementFilePath()'), 'the authorized file route serves the document')
  assert.ok(files.includes('getEndorsementFileName()'), 'by its stored path or its display name')
})

test('the columns are added additively and never destroy an existing record', () => {
  const migration = readBackend('migration-add-proposal-endorsement-document.sql')
  assert.ok(migration.includes('ADD COLUMN IF NOT EXISTS endorsement_file_name VARCHAR(255)'))
  assert.ok(migration.includes('ADD COLUMN IF NOT EXISTS endorsement_file_path VARCHAR(500)'))
  assert.ok(!/DROP\s+(TABLE|COLUMN)/i.test(migration), 'nothing is dropped')
  const schema = readBackend('schema.sql')
  assert.ok(schema.includes('endorsement_file_name VARCHAR(255)'))
  const launcher = readFileSync(
    fileURLToPath(new URL('../../run-backend.bat', import.meta.url)),
    'utf8'
  )
  assert.ok(launcher.includes('migration-add-proposal-endorsement-document.sql'))
})
