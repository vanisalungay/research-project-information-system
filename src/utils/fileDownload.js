/**
 * fileDownload.js
 * Central helper for downloading uploaded documents (Special Orders and, later,
 * proposal attachments). All "Download" buttons should use these helpers so the
 * URL is consistent and there are no hardcoded host/port references.
 */

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
