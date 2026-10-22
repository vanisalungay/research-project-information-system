/**
 * technologyMap.test.js
 * ---------------------------------------------------------------------------
 * Covers the Technology Map upload rules of the Create Proposal form (section
 * 13): the label and instruction text, the 10 MB limit (10 x 1024 x 1024 bytes,
 * the limit itself is still accepted), the PNG/JPEG check by MIME type and by
 * extension, and - the important part - the content check that rejects a
 * non-image file which only carries a .png/.jpg name or an image MIME type.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  TECHNOLOGY_MAP_EXTENSIONS,
  TECHNOLOGY_MAP_FORMAT_ERROR,
  TECHNOLOGY_MAP_HINT,
  TECHNOLOGY_MAP_LABEL,
  TECHNOLOGY_MAP_MAX_BYTES,
  TECHNOLOGY_MAP_MIME_TYPES,
  TECHNOLOGY_MAP_SIZE_ERROR,
  declaredMimeType,
  detectTechnologyMapType,
  fileExtension,
  formatFileSize,
  clearedTechnologyMapSelection,
  nextTechnologyMapSelection,
  technologyMapKey,
  technologyMapUploadError,
  validateTechnologyMapFile,
  verifyTechnologyMapFile,
} from './technologyMap.js'

/** A file-like object; the rules only use `name`, `size`, `type` and `slice`. */
const fileOf = (bytes, name, type = '') =>
  new File([new Uint8Array(bytes)], name, { type })

const pngBytes = (extra = 0) => [
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
  ...new Array(extra).fill(0),
]
const jpegBytes = (extra = 0) => [0xff, 0xd8, 0xff, 0xe0, ...new Array(extra).fill(0)]
const textBytes = (text) => [...new TextEncoder().encode(text)]

test('the field is labelled Technology Map and documents the limit', () => {
  assert.equal(TECHNOLOGY_MAP_LABEL, 'Technology Map')
  assert.equal(
    TECHNOLOGY_MAP_HINT,
    'Upload your technology map as an image (PNG or JPEG, maximum file size: 10 MB).',
  )
})

test('10 MB means 10 x 1024 x 1024 bytes and only PNG/JPEG are accepted', () => {
  assert.equal(TECHNOLOGY_MAP_MAX_BYTES, 10485760)
  assert.deepEqual(TECHNOLOGY_MAP_MIME_TYPES, ['image/png', 'image/jpeg'])
  assert.deepEqual(TECHNOLOGY_MAP_EXTENSIONS, ['png', 'jpg', 'jpeg'])
})

test('the file size is shown in a readable unit', () => {
  assert.equal(formatFileSize(0), '0 B')
  assert.equal(formatFileSize(900), '900 B')
  assert.equal(formatFileSize(2048), '2.0 KB')
  assert.equal(formatFileSize(1024 * 1024), '1.0 MB')
  assert.equal(formatFileSize(TECHNOLOGY_MAP_MAX_BYTES), '10.0 MB')
  assert.equal(formatFileSize(undefined), '')
})

test('the extension of a file name is read in lower case', () => {
  assert.equal(fileExtension('Technology Map.PNG'), 'png')
  assert.equal(fileExtension('photo.jpeg'), 'jpeg')
  assert.equal(fileExtension('photo.jpg'), 'jpg')
  assert.equal(fileExtension('no-extension'), '')
  assert.equal(fileExtension(''), '')
})

test('the declared MIME type is normalised, never guessed', () => {
  assert.equal(declaredMimeType(fileOf(pngBytes(), 'map.png', 'IMAGE/PNG')), 'image/png')
  assert.equal(declaredMimeType(fileOf(pngBytes(), 'map.png')), '')
})

test('a file larger than 10 MB is rejected with the size message', () => {
  const oversized = fileOf(new Array(TECHNOLOGY_MAP_MAX_BYTES + 1).fill(0), 'big.png', 'image/png')
  assert.deepEqual(validateTechnologyMapFile(oversized), {
    valid: false,
    error: TECHNOLOGY_MAP_SIZE_ERROR,
  })
})

test('a file of exactly 10 MB passes the size rule', () => {
  const atLimit = fileOf(new Array(TECHNOLOGY_MAP_MAX_BYTES).fill(0), 'limit.png', 'image/png')
  assert.equal(validateTechnologyMapFile(atLimit).valid, true)
})

test('an unsupported format is rejected with the format message', () => {
  for (const name of ['scan.pdf', 'photo.gif', 'archive.zip', 'notes.txt', 'no-extension']) {
    assert.deepEqual(validateTechnologyMapFile(fileOf(textBytes('x'), name, 'text/plain')), {
      valid: false,
      error: TECHNOLOGY_MAP_FORMAT_ERROR,
    })
  }
})

test('png and jpeg are accepted by MIME type or by extension', () => {
  assert.equal(validateTechnologyMapFile(fileOf(pngBytes(), 'map.png', 'image/png')).valid, true)
  assert.equal(validateTechnologyMapFile(fileOf(jpegBytes(), 'map.jpeg', 'image/jpeg')).valid, true)
  // A browser may not report a type for a local file, so the extension is used.
  assert.equal(validateTechnologyMapFile(fileOf(jpegBytes(), 'map.jpg')).valid, true)
  assert.equal(validateTechnologyMapFile(fileOf(pngBytes(), 'map.PNG')).valid, true)
})

test('a missing file is reported instead of being accepted', () => {
  assert.equal(validateTechnologyMapFile(null).valid, false)
  assert.equal(validateTechnologyMapFile(undefined).valid, false)
})

test('the real image type is read from the file content', async () => {
  assert.equal(await detectTechnologyMapType(fileOf(pngBytes(), 'map.png')), 'image/png')
  assert.equal(await detectTechnologyMapType(fileOf(jpegBytes(), 'map.jpg')), 'image/jpeg')
  assert.equal(await detectTechnologyMapType(fileOf(textBytes('not an image'), 'map.png')), '')
})

test('a text file renamed to .png is rejected even with an image MIME type', async () => {
  const fake = fileOf(textBytes('this is not an image'), 'technology-map.png', 'image/png')

  // The extension and the declared type both look right, so only the content
  // check can tell the difference.
  assert.equal(validateTechnologyMapFile(fake).valid, true)
  assert.deepEqual(await verifyTechnologyMapFile(fake), {
    valid: false,
    error: TECHNOLOGY_MAP_FORMAT_ERROR,
    type: '',
  })
})

test('a real PNG or JPEG passes the complete validation', async () => {
  assert.deepEqual(await verifyTechnologyMapFile(fileOf(pngBytes(), 'map.png', 'image/png')), {
    valid: true,
    error: '',
    type: 'image/png',
  })
  assert.deepEqual(await verifyTechnologyMapFile(fileOf(jpegBytes(), 'map.jpeg', 'image/jpeg')), {
    valid: true,
    error: '',
    type: 'image/jpeg',
  })
})

test('the size rule runs before the content check', async () => {
  const oversized = fileOf(new Array(TECHNOLOGY_MAP_MAX_BYTES + 1).fill(0), 'big.png', 'image/png')
  assert.equal((await verifyTechnologyMapFile(oversized)).error, TECHNOLOGY_MAP_SIZE_ERROR)
})

test('the same file object is identified by name, size and modification time', () => {
  const file = fileOf(pngBytes(), 'map.png', 'image/png')
  assert.equal(technologyMapKey(file), technologyMapKey(file))
  assert.notEqual(
    technologyMapKey(file),
    technologyMapKey(fileOf(pngBytes(4), 'map.png', 'image/png')),
  )
  assert.equal(technologyMapKey(null), '')
})

test('a valid image replaces the previous selection and clears the error', () => {
  const first = fileOf(pngBytes(), 'first.png', 'image/png')
  const second = fileOf(jpegBytes(), 'second.jpg', 'image/jpeg')

  assert.deepEqual(
    nextTechnologyMapSelection(first, { valid: true, error: '' }, second),
    { file: second, error: '' },
  )
  // The first image of a fresh form has nothing to replace.
  assert.deepEqual(nextTechnologyMapSelection(null, { valid: true, error: '' }, first), {
    file: first,
    error: '',
  })
})

test('an invalid file is never accepted, so the previous selection survives', () => {
  const kept = fileOf(pngBytes(), 'kept.png', 'image/png')
  const rejected = fileOf(textBytes('not an image'), 'fake.png', 'image/png')

  assert.deepEqual(
    nextTechnologyMapSelection(kept, { valid: false, error: TECHNOLOGY_MAP_FORMAT_ERROR }, rejected),
    { file: kept, error: TECHNOLOGY_MAP_FORMAT_ERROR },
  )
  // Nothing was selected before, so an invalid pick leaves the field empty.
  assert.deepEqual(
    nextTechnologyMapSelection(null, { valid: false, error: TECHNOLOGY_MAP_SIZE_ERROR }, rejected),
    { file: null, error: TECHNOLOGY_MAP_SIZE_ERROR },
  )
})

test('removing the image clears both the selection and the error', () => {
  assert.deepEqual(clearedTechnologyMapSelection(), { file: null, error: '' })
})

test('an upload failure reports the message the server sent', () => {
  assert.equal(
    technologyMapUploadError({ response: { data: { error: TECHNOLOGY_MAP_SIZE_ERROR } } }),
    TECHNOLOGY_MAP_SIZE_ERROR,
  )
  assert.equal(
    technologyMapUploadError({ response: { data: TECHNOLOGY_MAP_FORMAT_ERROR } }),
    TECHNOLOGY_MAP_FORMAT_ERROR,
  )
  assert.match(technologyMapUploadError(new Error('network')), /could not be uploaded/)
})
