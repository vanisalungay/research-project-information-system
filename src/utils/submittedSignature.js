/**
 * submittedSignature.js
 * ---------------------------------------------------------------------------
 * Upload of the "Submitted By" signature of the Certification & Endorsement step.
 *
 * The signature is chosen in the Certification step (an account signature or a file
 * uploaded for this proposal only). The bytes never travel in the proposal JSON: the
 * file is posted to the proposal's own endpoint
 * (POST /api/proposals/{id}/submitted-by-signature), which validates it, stores it and
 * sets both the display name and the stored path on the proposal. That keeps the file
 * associated with exactly the proposal it was chosen for, and means a client cannot
 * point a proposal at an arbitrary stored file.
 */
import api from './api.js'

/**
 * Identifies the exact file object, so the same signature is uploaded only once even
 * when the proponent saves a draft and then submits the proposal.
 */
export function submittedSignatureKey(file) {
  if (!file) return ''
  return [file.name || '', file.size ?? '', file.lastModified ?? ''].join(':')
}

/** Uploads the signature to a proposal and returns the saved proposal. */
export async function uploadSubmittedSignature(proposalId, file) {
  const form = new FormData()
  form.append('file', file, file.name || 'signature')
  const res = await api.post(`/api/proposals/${proposalId}/submitted-by-signature`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return res.data
}

/** The message of a failed upload, falling back to a readable default. */
export function submittedSignatureUploadError(
  error,
  fallback = 'The signature could not be uploaded. Please try again.',
) {
  const body = error?.response?.data
  if (body && typeof body === 'object' && body.error) return String(body.error)
  if (typeof body === 'string' && body.trim() !== '') return body
  return fallback
}
