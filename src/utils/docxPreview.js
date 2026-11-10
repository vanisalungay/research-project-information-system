/**
 * docxPreview.js
 * ---------------------------------------------------------------------------
 * Read-only preview of an uploaded DOCX (the Line-Item Budget Requirement file).
 *
 * The original upload is never modified. The bytes fetched from the authorized file
 * route are converted in the browser to HTML by mammoth and dropped into a
 * sanitised document fragment, so the OVCAF user reads the actual document instead
 * of downloading it or seeing a generated substitute.
 *
 * Mammoth is loaded lazily: it is only needed when a DOCX is previewed, so a PDF
 * preview (or any page that never opens the modal) does not load it.
 */

/**
 * Convert DOCX bytes to HTML. Throws when the converter is unavailable or the file
 * is not a readable Word document; the caller then shows the download fallback.
 *
 * @param {ArrayBuffer} data the uploaded file's bytes
 * @returns {Promise<string>} HTML produced from the document's own contents
 */
export async function convertDocxToHtml(data) {
  const module = await import('mammoth')
  const mammoth = module.default || module
  if (!mammoth || typeof mammoth.convertToHtml !== 'function') {
    throw new Error('DOCX preview is unavailable.')
  }

  // mammoth's browser build reads `arrayBuffer`, its Node build (used by tests)
  // reads `buffer`; providing the shape each one needs keeps the preview identical.
  const bytes = data instanceof Uint8Array ? data : new Uint8Array(data)
  const input = {
    arrayBuffer: bytes.buffer.slice(bytes.byteOffset, bytes.byteOffset + bytes.byteLength),
  }
  if (typeof Buffer !== 'undefined') input.buffer = Buffer.from(bytes)

  const result = await mammoth.convertToHtml(input)
  return String(result?.value || '')
}

/** Elements allowed in the previewed document. */
const ALLOWED_TAGS = new Set([
  'p', 'br', 'strong', 'b', 'em', 'i', 'u', 's', 'sup', 'sub',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'ul', 'ol', 'li', 'table', 'thead', 'tbody', 'tfoot', 'tr', 'td', 'th',
  'blockquote', 'a', 'img', 'span', 'div',
])

/** Attributes allowed on a given element. Everything else is dropped. */
const ALLOWED_ATTRIBUTES = {
  a: new Set(['href', 'title']),
  img: new Set(['src', 'alt', 'title']),
  td: new Set(['colspan', 'rowspan']),
  th: new Set(['colspan', 'rowspan']),
}

/** Elements that must not be copied at all, including their text content. */
const DROPPED_TAGS = new Set([
  'script', 'style', 'iframe', 'object', 'embed', 'link', 'meta', 'form',
  'input', 'button', 'textarea', 'select', 'base', 'svg', 'math',
])

/** True when a link/image URL is safe to keep in the preview. */
function isSafeUrl(value, tag) {
  const url = String(value || '').trim()
  if (!url) return false
  if (tag === 'img') return /^data:image\//i.test(url)
  return /^(https?:|mailto:)/i.test(url) || url.startsWith('#')
}

function copyChildren(source, target, doc) {
  for (const node of Array.from(source.childNodes)) {
    // Text (and any other safe non-element content) is copied as text, never markup.
    if (node.nodeType === 3 || node.nodeType === 4) {
      target.appendChild(doc.createTextNode(node.nodeValue || ''))
      continue
    }
    if (node.nodeType !== 1) continue

    const tag = node.tagName.toLowerCase()
    if (DROPPED_TAGS.has(tag)) continue

    // An element the preview does not render keeps its safe children, so a
    // wrapper the converter emits never hides the document's text.
    if (!ALLOWED_TAGS.has(tag)) {
      copyChildren(node, target, doc)
      continue
    }

    const element = doc.createElement(tag)
    const allowed = ALLOWED_ATTRIBUTES[tag]
    if (allowed) {
      for (const attribute of Array.from(node.attributes)) {
        const name = attribute.name.toLowerCase()
        if (!allowed.has(name)) continue
        if ((name === 'href' || name === 'src') && !isSafeUrl(attribute.value, tag)) continue
        element.setAttribute(name, attribute.value)
      }
    }
    if (tag === 'a' && element.getAttribute('href')) {
      element.setAttribute('target', '_blank')
      element.setAttribute('rel', 'noopener noreferrer')
    }

    copyChildren(node, element, doc)
    target.appendChild(element)
  }
}

/**
 * Turn the converter's HTML into a safe fragment that can be appended to the
 * preview container. Only the document's readable structure (paragraphs, lists,
 * tables, emphasis, links and embedded images) is kept; scripts, event handlers,
 * styles and unsafe URLs are dropped.
 *
 * @param {string} html HTML produced by convertDocxToHtml
 * @param {Document} [doc] document used to create the fragment
 * @returns {DocumentFragment|null}
 */
export function sanitizeDocxHtml(html, doc = globalThis.document) {
  if (!doc) return null
  const template = doc.createElement('template')
  template.innerHTML = String(html || '')
  const fragment = doc.createDocumentFragment()
  copyChildren(template.content, fragment, doc)
  return fragment
}
