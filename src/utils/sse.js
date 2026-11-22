/**
 * A tiny Server-Sent Events frame parser.
 *
 * The browser's EventSource cannot add the `Authorization` header, so the notification
 * stream is read with fetch. These functions turn the byte stream into named events;
 * they are pure so the chunk-boundary and line-ending cases can be tested directly.
 */

/**
 * Parse one complete SSE frame (the text between blank lines).
 * Returns `{ event, data, id }` or null for a comment/empty frame.
 */
export function parseSseFrame(frame) {
  if (!frame) return null
  const lines = String(frame).split('\n')
  let event = 'message'
  let id = null
  const data = []

  for (const rawLine of lines) {
    const line = rawLine.replace(/\r$/, '')
    if (!line || line.startsWith(':')) continue

    const separator = line.indexOf(':')
    const field = separator === -1 ? line : line.slice(0, separator)
    let value = separator === -1 ? '' : line.slice(separator + 1)
    if (value.startsWith(' ')) value = value.slice(1)

    if (field === 'event') event = value || 'message'
    else if (field === 'data') data.push(value)
    else if (field === 'id') id = value
  }

  if (data.length === 0) return null
  return { event, id, data: data.join('\n') }
}

/**
 * A streaming parser. `push` accepts decoded text chunks and invokes `onEvent` for each
 * complete frame. A frame may be split across any number of chunks.
 */
export function createSseParser(onEvent) {
  let buffer = ''

  function handleFrame(frame) {
    const parsed = parseSseFrame(frame)
    if (!parsed || typeof onEvent !== 'function') return
    let payload = parsed.data
    try {
      payload = JSON.parse(parsed.data)
    } catch {
      // A non-JSON frame is still delivered as text; the store only handles its own
      // named events and ignores unknown ones.
    }
    onEvent(parsed.event, payload, parsed)
  }

  return {
    push(chunk) {
      buffer += String(chunk ?? '')
      // Normalize CRLF and lone CR so the frame separator is always "\n\n".
      buffer = buffer.replace(/\r\n/g, '\n').replace(/\r/g, '\n')
      let boundary
      while ((boundary = buffer.indexOf('\n\n')) !== -1) {
        const frame = buffer.slice(0, boundary)
        buffer = buffer.slice(boundary + 2)
        handleFrame(frame)
      }
    },
    reset() {
      buffer = ''
    },
  }
}
