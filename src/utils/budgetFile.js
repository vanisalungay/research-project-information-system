/**
 * budgetFile.js
 * ---------------------------------------------------------------------------
 * Rules of the "Upload Filled Budget File" control of the Create Proposal form
 * (section 25 - "Line-Item Budget Requirement").
 *
 * The proponent uploads the filled-out line-item budget instead of entering the
 * budget details into the form: a PDF or a DOCX of at most 10 MB
 * (10 x 1024 x 1024 bytes). The file is validated the moment it is chosen - size,
 * extension, declared MIME type and the real file signature - so an unsupported
 * or oversized file never reaches the form state. The very same rules are
 * enforced again by the backend (POST /api/proposals/{id}/budget-file) because a
 * request can always bypass the browser.
 *
 * A file name or a declared MIME type is only a hint: `verifyBudgetFile` also
 * reads the first bytes of the file, so an image renamed to ".pdf" is rejected
 * instead of being accepted on its extension.
 *
 * Run the rules with: npm test
 */
import api from './api.js'
// One implementation of the human readable file size, shared with the Technology
// Map upload, so both upload fields report a size in exactly the same way.
import { formatFileSize } from './technologyMap.js'

/** Accepted file extensions, lower case and without the dot. */
export const BUDGET_FILE_EXTENSIONS = ['pdf', 'docx']

/** Accepted MIME types (the DOCX one is the official OOXML media type). */
export const BUDGET_FILE_MIME_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]

/**
 * Types some browsers / operating systems report for a .docx, which is an OOXML
 * package (a ZIP archive). They are accepted for a .docx only.
 */
export const BUDGET_FILE_DOCX_ALIAS_MIME_TYPES = ['application/zip', 'application/x-zip-compressed']

/** 10 MB, exactly as the limit is documented: 10 x 1024 x 1024 bytes. */
export const BUDGET_FILE_MAX_BYTES = 10 * 1024 * 1024

/** Visible label of the upload control. */
export const BUDGET_FILE_LABEL = 'Upload Filled Budget File'

/** The two rule lines shown under the instruction. */
export const BUDGET_FILE_FORMATS_LABEL = 'Accepted file formats:'
export const BUDGET_FILE_FORMATS_VALUE = 'PDF (.pdf) and Word Document (.docx)'
export const BUDGET_FILE_SIZE_LABEL = 'Maximum file size:'
export const BUDGET_FILE_SIZE_VALUE = '10 MB'

/** Messages the proponent sees - the backend reports the same two reasons. */
export const BUDGET_FILE_SIZE_ERROR =
  'File size exceeds the 10 MB limit. Please upload a smaller file.'
export const BUDGET_FILE_FORMAT_ERROR = 'Invalid file format. Please upload a PDF or DOCX file.'
export const BUDGET_FILE_EMPTY_ERROR = 'Please choose a Line-Item Budget file first.'

/** PDF files always start with "%PDF-". */
const PDF_SIGNATURE = [0x25, 0x50, 0x44, 0x46, 0x2d]
/** A DOCX is an OOXML package, i.e. a ZIP archive: it starts with "PK". */
const ZIP_SIGNATURE = [0x50, 0x4b]

/** Extension of a file name, lower case and without the dot ('' when none). */
export function budgetFileExtension(name) {
  const clean = String(name ?? '')
    .trim()
    .toLowerCase()
  const dot = clean.lastIndexOf('.')
  return dot === -1 ? '' : clean.slice(dot + 1)
}

/**
 * The MIME type the browser declared for a file, lower case, or '' when it did
 * not provide one. It is never trusted on its own - it can be empty or wrong.
 */
export function budgetFileDeclaredMimeType(file) {
  return String(file?.type ?? '')
    .trim()
    .toLowerCase()
}

/** A human readable file size ("812 KB", "2.4 MB") for the selected file. */
export function budgetFileSizeLabel(bytes) {
  return formatFileSize(bytes)
}

/**
 * The checks that can run on the file object itself: a file must be chosen, must
 * not exceed 10 MB, and must be a PDF or a DOCX (by extension and, when the
 * browser declares one, by MIME type). Returns `{ valid, error }`.
 */
export function validateBudgetFile(file) {
  if (!file) {
    return { valid: false, error: BUDGET_FILE_EMPTY_ERROR }
  }
  if (Number(file.size) > BUDGET_FILE_MAX_BYTES) {
    return { valid: false, error: BUDGET_FILE_SIZE_ERROR }
  }

  const extension = budgetFileExtension(file.name)
  if (!BUDGET_FILE_EXTENSIONS.includes(extension)) {
    return { valid: false, error: BUDGET_FILE_FORMAT_ERROR }
  }

  const declared = budgetFileDeclaredMimeType(file)
  // A browser does not always report a type (and sometimes reports only the
  // generic one), so a type that clearly belongs to another format is what gets
  // rejected here - the content check below is the decisive one.
  const genericType = declared === '' || declared === 'application/octet-stream'
  const accepted =
    BUDGET_FILE_MIME_TYPES.includes(declared) ||
    (extension === 'docx' && BUDGET_FILE_DOCX_ALIAS_MIME_TYPES.includes(declared))
  if (!genericType && !accepted) {
    return { valid: false, error: BUDGET_FILE_FORMAT_ERROR }
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

/**
 * The real format of a file, from its content: 'pdf' (a PDF document), 'docx'
 * (an OOXML / ZIP package) or '' when the bytes are neither.
 */
export async function detectBudgetFileType(file) {
  const head = new Uint8Array(await readHead(file, 8))
  if (startsWith(head, PDF_SIGNATURE)) return 'pdf'
  if (startsWith(head, ZIP_SIGNATURE)) return 'docx'
  return ''
}

/**
 * The complete client-side validation of a chosen file: size, extension, declared
 * type and the real file signature - the bytes must be the format the name
 * claims, so a PNG renamed to ".pdf" never gets stored. Returns
 * `{ valid, error, type }`; an invalid file must never be stored on the form.
 */
export async function verifyBudgetFile(file) {
  const basic = validateBudgetFile(file)
  if (!basic.valid) return { ...basic, type: '' }

  const extension = budgetFileExtension(file.name)
  let type = ''
  try {
    type = await detectBudgetFileType(file)
  } catch (err) {
    return { valid: false, error: BUDGET_FILE_FORMAT_ERROR, type: '' }
  }
  if (type !== extension) {
    return { valid: false, error: BUDGET_FILE_FORMAT_ERROR, type: '' }
  }
  return { valid: true, error: '', type }
}

/**
 * Identifies the exact file object, so the same file is uploaded only once even
 * when the proponent saves the draft and then submits the proposal.
 */
export function budgetFileKey(file) {
  if (!file) return ''
  return [file.name || '', file.size ?? '', file.lastModified ?? ''].join(':')
}

/**
 * The state of the upload field after the proponent chose a file.
 *
 * A valid file replaces the current selection and clears the error; an invalid one
 * is never accepted, so the previous selection stays as it was and only the reason
 * is reported. Returns `{ file, error }`.
 */
export function nextBudgetFileSelection(currentFile, verification, chosenFile) {
  if (!verification?.valid) {
    return {
      file: currentFile || null,
      error: verification?.error || BUDGET_FILE_FORMAT_ERROR,
    }
  }
  return { file: chosenFile || null, error: '' }
}

/** The state of the upload field after removing the file: nothing chosen, no error. */
export function clearedBudgetFileSelection() {
  return { file: null, error: '' }
}

/**
 * Uploads the filled-out Line-Item Budget to a proposal and returns the saved
 * proposal (with its `beneficiariesFileName` / `beneficiariesFilePath`).
 */
export async function uploadBudgetFile(proposalId, file) {
  const form = new FormData()
  form.append('file', file, file.name || 'line-item-budget')
  const res = await api.post(`/api/proposals/${proposalId}/budget-file`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

/** The message of a failed upload, falling back to a readable default. */
export function budgetFileUploadError(
  error,
  fallback = 'The Line-Item Budget file could not be uploaded. Please try again.',
) {
  const body = error?.response?.data
  if (body && typeof body === 'object' && body.error) return String(body.error)
  if (typeof body === 'string' && body.trim() !== '') return body
  return fallback
}
