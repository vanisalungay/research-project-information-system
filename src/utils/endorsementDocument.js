/**
 * endorsementDocument.js
 * ---------------------------------------------------------------------------
 * Rules of the "Endorsed By" upload of the Certification & Endorsement step
 * (the Endorsement Document of the Create Proposal workflow).
 *
 * The Dean's signed endorsement is uploaded as a PDF only, at most 10 MB
 * (10 x 1024 x 1024 bytes). The file is validated the moment it is chosen - size,
 * extension, declared MIME type and the real file signature - so a PNG/JPG/DOCX or a
 * renamed image never reaches the form state. The very same rules are enforced again
 * by the backend (POST /api/proposals/{id}/endorsement-document) because a request can
 * always bypass the browser, and the backend additionally opens the document, so a
 * corrupted PDF is refused too.
 *
 * A file name or a declared MIME type is only a hint: `detectEndorsementDocumentType`
 * reads the first bytes of the file, so an image renamed to ".pdf" is rejected instead
 * of being accepted on its extension.
 *
 * Run the rules with: npm test
 */
import api from './api.js'
// One implementation of the human readable file size, shared with the other uploads,
// so every upload field reports a size in exactly the same way.
import { formatFileSize } from './technologyMap.js'

/** Accepted file extensions, lower case and without the dot. */
export const ENDORSEMENT_DOCUMENT_EXTENSIONS = ['pdf']

/** Accepted MIME types. */
export const ENDORSEMENT_DOCUMENT_MIME_TYPES = ['application/pdf']

/** 10 MB, exactly as the limit is documented: 10 x 1024 x 1024 bytes. */
export const ENDORSEMENT_DOCUMENT_MAX_BYTES = 10 * 1024 * 1024

/** The value of the file dialog's `accept` attribute (filters for PDF files). */
export const ENDORSEMENT_DOCUMENT_ACCEPT = '.pdf,application/pdf'

/** Visible label and instruction of the upload control. */
export const ENDORSEMENT_DOCUMENT_LABEL = 'Endorsement Document'
export const ENDORSEMENT_DOCUMENT_INSTRUCTION = 'Upload Endorsement Document (PDF only)'
export const ENDORSEMENT_DOCUMENT_HELPER = 'Accepted file format: PDF (.pdf)'
export const ENDORSEMENT_DOCUMENT_SIZE_LABEL = 'Maximum file size:'
export const ENDORSEMENT_DOCUMENT_SIZE_VALUE = '10 MB'

/** Messages the proponent sees - the backend reports the same reasons. */
export const ENDORSEMENT_DOCUMENT_FORMAT_ERROR =
  'Invalid file format. Please upload the endorsement document as a PDF (.pdf) file.'
export const ENDORSEMENT_DOCUMENT_SIZE_ERROR =
  'File size exceeds the 10 MB limit. Please upload a smaller PDF file.'
export const ENDORSEMENT_DOCUMENT_EMPTY_ERROR = 'Please choose the endorsement document (PDF) first.'
export const ENDORSEMENT_DOCUMENT_REQUIRED_ERROR =
  'Upload the endorsement document as a PDF file. Accepted file format: PDF (.pdf).'

/** PDF documents always start with "%PDF-". */
const PDF_SIGNATURE = [0x25, 0x50, 0x44, 0x46, 0x2d]

/** Extension of a file name, lower case and without the dot ('' when none). */
export function endorsementDocumentExtension(name) {
  const clean = String(name ?? '')
    .trim()
    .toLowerCase()
  const dot = clean.lastIndexOf('.')
  return dot === -1 ? '' : clean.slice(dot + 1)
}

/**
 * The MIME type the browser declared for a file, lower case, or '' when it did not
 * provide one. It is never trusted on its own - it can be empty or wrong.
 */
export function endorsementDocumentDeclaredMimeType(file) {
  return String(file?.type ?? '')
    .trim()
    .toLowerCase()
}

/** A human readable file size ("812 KB", "2.4 MB") for the selected file. */
export const endorsementDocumentSizeLabel = (bytes) => formatFileSize(bytes)

/**
 * The checks that can run on the file object itself: a file must be chosen, must not
 * exceed 10 MB, and must be a PDF (by extension and, when the browser declares one, by
 * MIME type). Returns `{ valid, error }`.
 */
export function validateEndorsementDocument(file) {
  if (!file) {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_EMPTY_ERROR }
  }
  if (Number(file.size) > ENDORSEMENT_DOCUMENT_MAX_BYTES) {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_SIZE_ERROR }
  }

  const extension = endorsementDocumentExtension(file.name)
  if (!ENDORSEMENT_DOCUMENT_EXTENSIONS.includes(extension)) {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_FORMAT_ERROR }
  }

  const declared = endorsementDocumentDeclaredMimeType(file)
  // A browser does not always report a type (and sometimes reports only the generic
  // one), so a type that clearly belongs to another format is what gets rejected here
  // - the content check below is the decisive one.
  const genericType = declared === '' || declared === 'application/octet-stream'
  if (!genericType && !ENDORSEMENT_DOCUMENT_MIME_TYPES.includes(declared)) {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_FORMAT_ERROR }
  }

  return { valid: true, error: '' }
}

/** Reads the first bytes of a file without loading the whole document. */
function readHead(file, length) {
  const slice = typeof file.slice === 'function' ? file.slice(0, length) : file
  if (typeof slice.arrayBuffer === 'function') {
    return slice.arrayBuffer()
  }
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result)
    reader.onerror = () => reject(reader.error || new Error('The file could not be read.'))
    reader.readAsArrayBuffer(slice)
  })
}

const startsWith = (bytes, signature) => signature.every((byte, index) => bytes[index] === byte)

/** True when the bytes of a file are a PDF ("%PDF-" header). */
export function isPdfBytes(bytes) {
  return startsWith(bytes instanceof Uint8Array ? bytes : new Uint8Array(bytes), PDF_SIGNATURE)
}

/**
 * The real format of a file, from its content: 'pdf' (a PDF document) or '' when the
 * bytes are not a PDF header.
 */
export async function detectEndorsementDocumentType(file) {
  const head = new Uint8Array(await readHead(file, 8))
  return isPdfBytes(head) ? 'pdf' : ''
}

/**
 * The complete client-side validation of a chosen file: size, extension, declared
 * type and the real file signature - the bytes must be a PDF, so a PNG renamed to
 * ".pdf" never gets stored. Returns `{ valid, error, type }`; an invalid file must
 * never reach the form state.
 */
export async function verifyEndorsementDocument(file) {
  const basic = validateEndorsementDocument(file)
  if (!basic.valid) return { ...basic, type: '' }

  let type = ''
  try {
    type = await detectEndorsementDocumentType(file)
  } catch {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_FORMAT_ERROR, type: '' }
  }
  if (type !== 'pdf') {
    return { valid: false, error: ENDORSEMENT_DOCUMENT_FORMAT_ERROR, type: '' }
  }
  return { valid: true, error: '', type }
}

/**
 * The two document references the proposal workflow keeps in `proposalData` when a
 * saved draft is reopened: the display name and the stored path of the PDF. The
 * certification step reads them to show the stored document (and its View/Download
 * actions) instead of asking the proponent to upload it again.
 */
export function draftEndorsementFields(proposal) {
  return {
    endorsementFileName: proposal?.endorsementFileName || '',
    endorsementFilePath: proposal?.endorsementFilePath || '',
  }
}

/**
 * Identifies the exact file object, so the same document is uploaded only once even
 * when the proponent saves the draft and then submits the proposal.
 */
export function endorsementDocumentKey(file) {
  if (!file) return ''
  return [file.name || '', file.size ?? '', file.lastModified ?? ''].join(':')
}

/**
 * The state of the upload field after the proponent chose a file.
 *
 * A valid PDF replaces the current selection and clears the error; an invalid file is
 * never accepted, so the previous selection stays as it was and only the reason is
 * reported. Returns `{ file, error }`.
 */
export function nextEndorsementDocumentSelection(currentFile, verification, chosenFile) {
  if (!verification?.valid) {
    return {
      file: currentFile || null,
      error: verification?.error || ENDORSEMENT_DOCUMENT_FORMAT_ERROR,
    }
  }
  return { file: chosenFile || null, error: '' }
}

/** Uploads the endorsement document to a proposal and returns the saved proposal. */
export async function uploadEndorsementDocument(proposalId, file) {
  const form = new FormData()
  form.append('file', file, file.name || 'endorsement-document.pdf')
  const res = await api.post(`/api/proposals/${proposalId}/endorsement-document`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

/** The message of a failed upload, falling back to a readable default. */
export function endorsementDocumentUploadError(
  error,
  fallback = 'The endorsement document could not be uploaded. Please try again.',
) {
  const body = error?.response?.data
  if (body && typeof body === 'object' && body.error) return String(body.error)
  if (typeof body === 'string' && body.trim() !== '') return body
  return fallback
}
