/**
 * fileDownload.js
 * Central helper for downloading uploaded documents (Special Orders and, later,
 * proposal attachments). All "Download" buttons should use these helpers so the
 * URL is consistent and there are no hardcoded host/port references.
 */

import api from '@/utils/api'

const API_BASE = 'http://localhost:8081'

/** Normalize a stored path/name into a clean relative path. */
function normalizePath(path) {
  return String(path || '')
    .replace(/\\/g, '/')
    .replace(/^\/+/, '')
    .replace(/^uploads\//, '')
}

/** Trigger a browser download of a single uploaded file. */
export function downloadUploadedFile(path) {
  if (!path) return
  const clean = normalizePath(path)
  const url = `${API_BASE}/api/files?path=${encodeURIComponent(clean)}`
  const a = document.createElement('a')
  a.href = url
  a.download = clean.split('/').pop() || 'file'
  a.rel = 'noopener'
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
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
