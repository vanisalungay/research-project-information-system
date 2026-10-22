/**
 * documentPreview.js
 * Pure helpers that decide how an uploaded report document can be previewed
 * inside the application. The bytes are always fetched from the existing
 * authorized endpoint (utils/fileDownload.js); only the preview decision lives
 * here, so it can be verified without a browser.
 *
 * The decision is made from the stored file name (its extension) because the
 * download endpoint serves every document as application/octet-stream - it does
 * not report the real media type (see
 * QuarterlyProgressReportController#downloadDocument).
 */

/** Preview kind: the browser renders the document with its built-in PDF viewer. */
export const PREVIEW_PDF = 'pdf'
/** Preview kind: the document is an image and is shown in an <img>. */
export const PREVIEW_IMAGE = 'image'
/** Preview kind: no in-browser preview; the Download action stays available. */
export const PREVIEW_UNSUPPORTED = 'unsupported'

/** Image extensions the browser can display in an <img> without extra tooling. */
const IMAGE_EXTENSIONS = ['png', 'jpg', 'jpeg', 'gif', 'webp', 'bmp', 'svg']

/**
 * Media type used when the fetched bytes are turned into an object URL. The
 * server sends application/octet-stream for every document, which browsers
 * refuse to render inline, so the type is restored from the extension.
 */
const MIME_BY_EXTENSION = {
  pdf: 'application/pdf',
  png: 'image/png',
  jpg: 'image/jpeg',
  jpeg: 'image/jpeg',
  gif: 'image/gif',
  webp: 'image/webp',
  bmp: 'image/bmp',
  svg: 'image/svg+xml',
}

/**
 * Lower-case extension of a file name, without the dot. Returns '' when the name
 * has no usable extension (empty, null, no dot, a leading dot only or a trailing
 * dot).
 */
export function fileExtension(fileName) {
  const name = String(fileName ?? '')
    .trim()
    .toLowerCase()
  const dot = name.lastIndexOf('.')
  if (dot <= 0 || dot === name.length - 1) return ''
  return name.slice(dot + 1)
}

/**
 * How a stored document can be previewed in the browser:
 * 'pdf', 'image' or 'unsupported'.
 */
export function previewKindOf(fileName) {
  const extension = fileExtension(fileName)
  if (extension === 'pdf') return PREVIEW_PDF
  if (IMAGE_EXTENSIONS.includes(extension)) return PREVIEW_IMAGE
  return PREVIEW_UNSUPPORTED
}

/** Media type for previewing the file, or '' when it cannot be previewed. */
export function previewMimeType(fileName) {
  return MIME_BY_EXTENSION[fileExtension(fileName)] || ''
}

/**
 * Short file-type label shown in the UI, e.g. "PDF" or "DOCX". Falls back to
 * "FILE" when the name has no usable extension.
 */
export function fileTypeLabel(fileName) {
  const extension = fileExtension(fileName)
  return extension ? extension.toUpperCase() : 'FILE'
}
