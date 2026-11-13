/**
 * fileDownload.js
 * Central helper for downloading uploaded documents (Special Orders and, later,
 * proposal attachments). All "Download" buttons should use these helpers so the
 * URL is consistent and there are no hardcoded host/port references.
 */

import api from '@/utils/api'
import { retypeForPreview } from '@/utils/documentPreview'

/** Normalize a stored path/name into a clean relative path. */
function normalizePath(path) {
  return String(path || '')
    .replace(/\\/g, '/')
    .replace(/^\/+/, '')
    .replace(/^uploads\//, '')
}

/**
 * Trigger a browser download of a single uploaded file. The file route requires
 * authentication, so the request is sent through the shared axios client (which
 * attaches the bearer token) and the response is saved as a blob.
 *
 * @param {string} path stored path used by the authorized file route
 * @param {string} [fileName] original/display file name for the saved file; the
 *   stored name (which carries a generated uniqueness prefix) is used when absent
 */
export async function downloadUploadedFile(path, fileName) {
  if (!path) return
  const clean = normalizePath(path)
  const res = await api.get('/api/files', {
    params: { path: clean },
    responseType: 'blob',
  })
  const suggested = String(fileName || '')
    .replace(/\\/g, '/')
    .split('/')
    .pop()
  const url = window.URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = suggested || clean.split('/').pop() || 'file'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/**
 * Fetch an uploaded file through the existing authenticated file route. The caller
 * decides how to display the bytes (object URL for a PDF/image, an in-memory DOCX
 * conversion); nothing is written to the server.
 *
 * @param {string} path stored path, e.g. "budget-files/<uuid>_line-item-budget.docx"
 * @returns {Promise<Blob|null>}
 */
export async function fetchUploadedFileBlob(path) {
  if (!path) return null
  const clean = normalizePath(path)
  const res = await api.get('/api/files', {
    params: { path: clean },
    responseType: 'blob',
  })
  return res.data || null
}

/** Open an uploaded file (e.g. a PDF) in a new tab for preview. */
export async function previewUploadedFile(path) {
  if (!path) return
  const clean = normalizePath(path)
  const res = await api.get('/api/files', {
    params: { path: clean },
    responseType: 'blob',
  })
  // The file route serves every document as application/octet-stream, which the
  // browser downloads instead of rendering. The stored path carries the extension,
  // so the blob is re-typed before the object URL is created and a PDF opens in the
  // browser's viewer (a legacy image opens in an image tab).
  const url = window.URL.createObjectURL(retypeForPreview(res.data, clean))
  window.open(url, '_blank')
  setTimeout(() => window.URL.revokeObjectURL(url), 60000)
}

/** Download several uploaded files in sequence. */
export function downloadAllFiles(files) {
  const list = Array.isArray(files) ? files.filter(Boolean) : []
  list.forEach((file, index) => {
    setTimeout(() => downloadUploadedFile(file), index * 300)
  })
}

/**
 * Reporting-stage documents (Project Workplan / Terminal Report) are fetched
 * through the authorized `/api/quarterly-reports` endpoint rather than the
 * public file route, so only permitted roles can retrieve them.
 *
 * @param {number|string} reportId report row id
 * @param {'workplan'|'terminal'} docType which attached document to fetch
 * @param {string} [fileName] suggested download file name
 */
export async function downloadReportDocument(reportId, docType, fileName) {
  const res = await api.get(`/api/quarterly-reports/${reportId}/documents/${docType}`, {
    responseType: 'blob',
  })
  const url = window.URL.createObjectURL(res.data)
  const a = document.createElement('a')
  a.href = url
  a.download = fileName || docType
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  window.URL.revokeObjectURL(url)
}

/** Open a reporting-stage document (e.g. a PDF) in a new tab for preview. */
export async function previewReportDocument(reportId, docType) {
  const res = await api.get(`/api/quarterly-reports/${reportId}/documents/${docType}`, {
    responseType: 'blob',
  })
  const url = window.URL.createObjectURL(res.data)
  window.open(url, '_blank')
  // Release the object URL once the new tab has had time to load it.
  setTimeout(() => window.URL.revokeObjectURL(url), 60000)
}

/**
 * Fetch an attached reporting-stage document and return an object URL that can
 * be used as the src of an in-modal <iframe>/<img> preview.
 *
 * The document route requires authentication, so the bytes are fetched through
 * the shared axios client (which attaches the bearer token) - a plain src=""
 * attribute could not authenticate. The route serves every document as
 * application/octet-stream, so the bytes are re-typed from the file extension
 * before the object URL is created; browsers would not render them inline
 * otherwise.
 *
 * @param {number|string} reportId report row id
 * @param {'workplan'|'terminal'} docType which attached document to fetch
 * @param {string} [fileName] stored file name (used to restore the media type)
 * @returns {Promise<string>} object URL - release it with releaseObjectUrl()
 */
export async function fetchReportDocumentPreviewUrl(reportId, docType, fileName) {
  const res = await api.get(`/api/quarterly-reports/${reportId}/documents/${docType}`, {
    responseType: 'blob',
  })
  return window.URL.createObjectURL(retypeForPreview(res.data, fileName))
}

/** Release an object URL created by the preview helpers. */
export function releaseObjectUrl(url) {
  if (url) window.URL.revokeObjectURL(url)
}
