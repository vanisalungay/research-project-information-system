/**
 * technologyMap.js
 * ---------------------------------------------------------------------------
 * Rules of the Technology Map upload of the Create Proposal form (section 13 -
 * "Technology Roadmap", the field is labelled "Technology Map").
 *
 * The proponent uploads the technology map as an image: PNG or JPEG, at most
 * 10 MB (10 x 1024 x 1024 bytes). The file is validated as soon as it is chosen,
 * so an unsupported or oversized file never reaches the form state, and the very
 * same rules are enforced again by the backend
 * (POST /api/proposals/{id}/technology-map) because a request can always bypass
 * the browser.
 *
 * A file name or a declared MIME type is only a hint: `verifyTechnologyMapFile`
 * also reads the first bytes of the file, so a text file renamed to ".png" is
 * rejected instead of being accepted on its extension.
 *
 * Run the rules with: npm test
 */
import api from './api.js'

/** Accepted image MIME types. */
export const TECHNOLOGY_MAP_MIME_TYPES = ['image/png', 'image/jpeg']

/** Accepted file extensions, lower case and without the dot. */
export const TECHNOLOGY_MAP_EXTENSIONS = ['png', 'jpg', 'jpeg']

/** 10 MB, exactly as the limit is documented: 10 x 1024 x 1024 bytes. */
export const TECHNOLOGY_MAP_MAX_BYTES = 10 * 1024 * 1024

/** Visible label of the field. */
export const TECHNOLOGY_MAP_LABEL = 'Technology Map'

/** Instruction shown under the label. */
export const TECHNOLOGY_MAP_HINT =
  'Upload your technology map as an image (PNG or JPEG, maximum file size: 10 MB).'

/** Messages the proponent sees - the backend reports the same two reasons. */
export const TECHNOLOGY_MAP_SIZE_ERROR =
  'File size exceeds the 10 MB limit. Please upload a smaller image.'
export const TECHNOLOGY_MAP_FORMAT_ERROR =
  'Invalid file format. Please upload a PNG or JPEG image.'
export const TECHNOLOGY_MAP_EMPTY_ERROR = 'Please choose a Technology Map image first.'

/** PNG signature: 89 50 4E 47 0D 0A 1A 0A. */
const PNG_SIGNATURE = [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]
/** JPEG files always start with FF D8 FF (SOI + the first marker). */
const JPEG_SIGNATURE = [0xff, 0xd8, 0xff]

/** Extension of a file name, lower case and without the dot ('' when none). */
export function fileExtension(name) {
  const clean = String(name ?? '').trim().toLowerCase()
  const dot = clean.lastIndexOf('.')
  return dot === -1 ? '' : clean.slice(dot + 1)
}

/** A human readable file size ("812 KB", "2.4 MB"). */
export function formatFileSize(bytes) {
  const size = Number(bytes)
  if (!Number.isFinite(size) || size < 0) return ''
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

/**
 * The declared MIME type of a file, or '' when the browser did not provide one.
 * It is never trusted on its own - it can be empty or simply wrong.
 */
export function declaredMimeType(file) {
  return String(file?.type ?? '').trim().toLowerCase()
}

/**
 * The size and type checks that can run on the file object itself: the file must
 * exist, must not exceed 10 MB, and must look like a PNG or JPEG (declared MIME
 * type or extension). Returns `{ valid, error }`.
 */
export function validateTechnologyMapFile(file) {
  if (!file) {
    return { valid: false, error: TECHNOLOGY_MAP_EMPTY_ERROR }
  }
  if (Number(file.size) > TECHNOLOGY_MAP_MAX_BYTES) {
    return { valid: false, error: TECHNOLOGY_MAP_SIZE_ERROR }
  }
  const looksLikeImage =
    TECHNOLOGY_MAP_MIME_TYPES.includes(declaredMimeType(file)) ||
    TECHNOLOGY_MAP_EXTENSIONS.includes(fileExtension(file.name))
  if (!looksLikeImage) {
    return { valid: false, error: TECHNOLOGY_MAP_FORMAT_ERROR }
  }
  return { valid: true, error: '' }
}

/** Reads the first bytes of a file without loading the whole image. */
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

const startsWith = (bytes, signature) =>
  signature.every((byte, index) => bytes[index] === byte)

/**
 * The real image type of a file, from its content: 'image/png', 'image/jpeg' or
 * '' when the bytes are not a PNG/JPEG image at all.
 */
export async function detectTechnologyMapType(file) {
  const head = new Uint8Array(await readHead(file, 12))
  if (startsWith(head, PNG_SIGNATURE)) return 'image/png'
  if (startsWith(head, JPEG_SIGNATURE)) return 'image/jpeg'
  return ''
}

/**
 * The complete client-side validation of a chosen file: size, declared type /
 * extension and the real image signature. Returns `{ valid, error, type }`; an
 * invalid file must never be stored on the form.
 */
export async function verifyTechnologyMapFile(file) {
  const basic = validateTechnologyMapFile(file)
  if (!basic.valid) return { ...basic, type: '' }

  let type = ''
  try {
    type = await detectTechnologyMapType(file)
  } catch (err) {
    return { valid: false, error: TECHNOLOGY_MAP_FORMAT_ERROR, type: '' }
  }
  if (!type) {
    return { valid: false, error: TECHNOLOGY_MAP_FORMAT_ERROR, type: '' }
  }
  return { valid: true, error: '', type }
}

/**
 * Identifies the exact file object, so the same image is uploaded only once even
 * when the proponent saves the draft and then submits the proposal.
 */
export function technologyMapKey(file) {
  if (!file) return ''
  return [file.name || '', file.size ?? '', file.lastModified ?? ''].join(':')
}

/**
 * The state of the upload field after the proponent chose a file.
 *
 * A valid image replaces the current selection and clears the error; an invalid one
 * is never accepted, so the previous selection stays as it was and only the reason
 * is reported. Returns `{ file, error }`.
 */
export function nextTechnologyMapSelection(currentFile, verification, chosenFile) {
  if (!verification?.valid) {
    return {
      file: currentFile || null,
      error: verification?.error || TECHNOLOGY_MAP_FORMAT_ERROR,
    }
  }
  return { file: chosenFile || null, error: '' }
}

/** The state of the upload field after "Remove Image": nothing chosen, no error. */
export function clearedTechnologyMapSelection() {
  return { file: null, error: '' }
}

/**
 * Uploads the Technology Map image to a proposal and returns the saved proposal
 * (with its `roadmapFileName` / `roadmapFilePath`).
 */
export async function uploadTechnologyMap(proposalId, file) {
  const form = new FormData()
  form.append('file', file, file.name || 'technology-map')
  const res = await api.post(`/api/proposals/${proposalId}/technology-map`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

/** The message of a failed upload, falling back to a readable default. */
export function technologyMapUploadError(
  error,
  fallback = 'The Technology Map image could not be uploaded. Please try again.',
) {
  const body = error?.response?.data
  if (body && typeof body === 'object' && body.error) return String(body.error)
  if (typeof body === 'string' && body.trim() !== '') return body
  return fallback
}
