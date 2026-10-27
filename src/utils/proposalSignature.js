/**
 * proposalSignature.js
 * ---------------------------------------------------------------------------
 * The proponent's electronic signature in the Certification & Endorsement step
 * (step 3 of the Create Proposal form, section "Submitted By").
 *
 * The signature an account has saved (Manage Profile -> Electronic Signature) is
 * offered here as the default, so a proponent does not have to upload the same
 * image for every proposal:
 *
 *   - GET /api/users/me/signature returns the signature of the *authenticated*
 *     account - the backend resolves the user from the bearer token - so this
 *     module can never read or expose another account's signature;
 *   - the default is the signature the proponent created *first*: the records are
 *     ordered by their creation timestamp before one is picked, so the choice never
 *     depends on the order an API happens to return;
 *   - a selection is a plain Blob (never a temporary URL), so the same image can be
 *     previewed, printed into the certification form and re-selected freely;
 *   - nothing here writes to the account: removing a signature from a proposal only
 *     clears that proposal's selection, and the account keeps its saved signature.
 */
import api from './api.js'

/** The account signature endpoint. Read-only here; it is token-scoped. */
export const ACCOUNT_SIGNATURE_ENDPOINT = '/api/users/me/signature'

/** Name used when the account's signature has no usable file name. */
export const DEFAULT_SIGNATURE_FILE_NAME = 'signature.png'

/** Where a selection comes from: the account's saved signature or an upload. */
export const SIGNATURE_SOURCES = { account: 'account', upload: 'upload' }

/**
 * A signature record: `{ id, fileName, blob, createdAt, source }`.
 * `createdAt` is only present when the API reports it; the order then falls back to
 * the order the records were returned in.
 */
export function normalizeSignatureRecord({
  id = null,
  fileName = '',
  blob = null,
  createdAt = null,
  source = SIGNATURE_SOURCES.account,
} = {}) {
  return {
    id: id == null ? null : String(id),
    fileName: fileName || DEFAULT_SIGNATURE_FILE_NAME,
    blob: blob || null,
    createdAt: createdAt || null,
    source,
  }
}

/** Creation time of a record in milliseconds, or null when it carries none. */
function timestampOf(signature) {
  const value = signature?.createdAt
  if (!value) return null
  const time = new Date(value).getTime()
  return Number.isFinite(time) ? time : null
}

/**
 * The saved signatures, oldest first.
 *
 * The creation timestamp decides the order. A record without one keeps the position
 * it was returned in (the sort is stable), so the current account store - one
 * signature per account - is unaffected, and a future endpoint that returns several
 * signatures with timestamps is ordered correctly.
 */
export function sortSignaturesOldestFirst(signatures = []) {
  return (Array.isArray(signatures) ? signatures : [])
    .map((signature, index) => ({ signature, index }))
    .sort((left, right) => {
      const a = timestampOf(left.signature)
      const b = timestampOf(right.signature)
      if (a === null && b === null) return left.index - right.index
      if (a === null) return 1
      if (b === null) return -1
      if (a === b) return left.index - right.index
      return a - b
    })
    .map((entry) => entry.signature)
}

/**
 * The default signature of a new proposal: the one the proponent created first.
 * Returns null when the account has no saved signature.
 */
export function pickDefaultSignature(signatures = []) {
  return sortSignaturesOldestFirst(signatures)[0] || null
}

/** File name announced by the server (`Content-Disposition`) or a fallback. */
export function fileNameFromContentDisposition(header, fallback = DEFAULT_SIGNATURE_FILE_NAME) {
  const match = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(String(header || ''))
  if (!match) return fallback
  let name = match[1].trim().replace(/"$/, '')
  try {
    name = decodeURIComponent(name)
  } catch {
    // A malformed header keeps the raw value rather than failing the request.
  }
  return name || fallback
}

/**
 * The signature saved on the signed-in account, or an empty list when the account
 * has none yet (404). Any other failure is thrown so the caller can offer the
 * upload fallback instead of showing a broken image.
 */
export async function loadAccountSignatures() {
  try {
    const response = await api.get(ACCOUNT_SIGNATURE_ENDPOINT, { responseType: 'blob' })
    const blob = response.data instanceof Blob ? response.data : new Blob([response.data])
    const fileName = fileNameFromContentDisposition(
      response.headers?.['content-disposition'],
      DEFAULT_SIGNATURE_FILE_NAME,
    )
    return [normalizeSignatureRecord({ id: fileName, fileName, blob })]
  } catch (error) {
    if (error?.response?.status === 404) return []
    throw error
  }
}

/**
 * The selection as a File, so it is used exactly like an uploaded signature by the
 * rest of the form: the required-field check, the file row and the printed
 * certification form all read the uploaded signature slot.
 */
export function signatureAsFile(signature) {
  if (!signature?.blob) return null
  const name = signature.fileName || DEFAULT_SIGNATURE_FILE_NAME
  const type = signature.blob.type || 'image/png'
  if (typeof File === 'function') {
    return new File([signature.blob], name, { type })
  }
  return signature.blob
}

/** Stable key of a selection, used to mark the one that is currently selected. */
export function signatureKey(signature) {
  if (!signature) return ''
  const size = signature.blob?.size ?? ''
  return [
    signature.source || SIGNATURE_SOURCES.account,
    signature.id || '',
    signature.fileName || '',
    size,
  ].join('|')
}

/** True when both describe the same selected signature. */
export function isSameSignature(left, right) {
  if (!left || !right) return false
  return signatureKey(left) === signatureKey(right)
}

/** Label of a selection for the field ("Saved signature · a.png"). */
export function signatureLabel(signature) {
  if (!signature) return ''
  const name = signature.fileName || DEFAULT_SIGNATURE_FILE_NAME
  return signature.source === SIGNATURE_SOURCES.upload
    ? `Uploaded image · ${name}`
    : `Saved signature · ${name}`
}
