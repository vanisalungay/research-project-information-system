/**
 * proposalSignature.test.js
 * ---------------------------------------------------------------------------
 * Covers the rule that decides which saved signature a new proposal starts with,
 * and the small helpers the Certification & Endorsement step uses around it.
 *
 * The default is the signature the proponent created *first*, decided by the
 * creation timestamp - never by the order an API happens to return records in.
 * Reading the account's signatures always goes through the token-scoped endpoint,
 * and nothing in the module can delete or replace an account signature.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'

import {
  ACCOUNT_SIGNATURE_ENDPOINT,
  DEFAULT_SIGNATURE_FILE_NAME,
  SIGNATURE_SOURCES,
  fileNameFromContentDisposition,
  isSameSignature,
  normalizeSignatureRecord,
  pickDefaultSignature,
  signatureAsFile,
  signatureKey,
  signatureLabel,
  sortSignaturesOldestFirst,
} from './proposalSignature.js'

const record = (id, createdAt, fileName = `${id}.png`) =>
  normalizeSignatureRecord({ id, fileName, createdAt, blob: { size: 10 } })

// Signature A was created first, B second, C third.
const A = record('A', '2026-01-05T08:00:00Z')
const B = record('B', '2026-02-11T09:30:00Z')
const C = record('C', '2026-03-02T14:15:00Z')

test('the default signature is the one created first', () => {
  assert.equal(pickDefaultSignature([A, B, C]).id, 'A')
})

test('the default does not depend on the order the API returned', () => {
  // Newest first, oldest first, shuffled - A is always the default.
  assert.equal(pickDefaultSignature([C, B, A]).id, 'A')
  assert.equal(pickDefaultSignature([A, C, B]).id, 'A')
  assert.equal(pickDefaultSignature([B, A, C]).id, 'A')
  assert.equal(pickDefaultSignature([B, C, A]).id, 'A')
})

test('records are ordered oldest first', () => {
  assert.deepEqual(
    sortSignaturesOldestFirst([C, A, B]).map((signature) => signature.id),
    ['A', 'B', 'C'],
  )
})

test('a record without a timestamp keeps the order it was returned in', () => {
  const first = record('first', null)
  const second = record('second', null)
  // Nothing to compare: the API's own order is kept, so a single-signature account
  // (the current account store) is unaffected.
  assert.deepEqual(
    sortSignaturesOldestFirst([first, second]).map((signature) => signature.id),
    ['first', 'second'],
  )
  // A timestamped record is ordered around the untimed ones, which stay stable.
  assert.deepEqual(
    sortSignaturesOldestFirst([first, C, second, A]).map((signature) => signature.id),
    ['A', 'C', 'first', 'second'],
  )
})

test('the same creation time keeps the returned order', () => {
  const x = record('X', '2026-01-05T08:00:00Z')
  const y = record('Y', '2026-01-05T08:00:00Z')
  assert.deepEqual(
    sortSignaturesOldestFirst([x, y]).map((signature) => signature.id),
    ['X', 'Y'],
  )
})

test('an account without a saved signature has no default', () => {
  assert.equal(pickDefaultSignature([]), null)
  assert.equal(pickDefaultSignature(), null)
  assert.equal(pickDefaultSignature([null]), null)
})

test('a selection keeps a usable file name and source', () => {
  const saved = normalizeSignatureRecord({ id: 'A', fileName: '', blob: { size: 4 } })
  assert.equal(saved.fileName, DEFAULT_SIGNATURE_FILE_NAME)
  assert.equal(saved.source, SIGNATURE_SOURCES.account)

  const uploaded = normalizeSignatureRecord({ fileName: 'my-sign.png', source: SIGNATURE_SOURCES.upload })
  assert.equal(uploaded.source, SIGNATURE_SOURCES.upload)
  assert.equal(signatureLabel(uploaded), 'Uploaded image · my-sign.png')
  assert.equal(signatureLabel(saved), `Saved signature · ${DEFAULT_SIGNATURE_FILE_NAME}`)
  assert.equal(signatureLabel(null), '')
})

test('a selection is handed to the form as a File', () => {
  const blob = new Blob([new Uint8Array([1, 2, 3])], { type: 'image/png' })
  const file = signatureAsFile(normalizeSignatureRecord({ fileName: 'a.png', blob }))
  assert.equal(file.name, 'a.png')
  assert.equal(file.type, 'image/png')
  assert.equal(file.size, 3)

  // Nothing selected: the form's required-field check must keep seeing an empty slot.
  assert.equal(signatureAsFile(null), null)
  assert.equal(signatureAsFile(normalizeSignatureRecord({ fileName: 'a.png' })), null)
})

test('the currently selected signature is recognised', () => {
  assert.equal(isSameSignature(A, { ...A }), true)
  assert.equal(isSameSignature(A, B), false)
  assert.equal(isSameSignature(A, null), false)
  assert.equal(isSameSignature(null, null), false)

  // An uploaded replacement is a different selection, even with the same name.
  const uploaded = normalizeSignatureRecord({ fileName: 'A.png', blob: { size: 10 }, source: SIGNATURE_SOURCES.upload })
  assert.equal(isSameSignature(A, uploaded), false)
  assert.equal(signatureKey(null), '')
})

test('the account signature endpoint is the token-scoped one', () => {
  // Reading another account's signature is impossible: the endpoint has no user id.
  assert.equal(ACCOUNT_SIGNATURE_ENDPOINT, '/api/users/me/signature')
  assert.equal(ACCOUNT_SIGNATURE_ENDPOINT.includes('{'), false)
})

test('the served file name is read from the response header', () => {
  assert.equal(
    fileNameFromContentDisposition('inline; filename="my-signature.png"'),
    'my-signature.png',
  )
  assert.equal(
    fileNameFromContentDisposition("inline; filename*=UTF-8''my%20sign.png"),
    'my sign.png',
  )
  assert.equal(fileNameFromContentDisposition('', 'fallback.png'), 'fallback.png')
  assert.equal(fileNameFromContentDisposition(undefined), DEFAULT_SIGNATURE_FILE_NAME)
  assert.equal(fileNameFromContentDisposition('inline; filename=""', 'fallback.png'), 'fallback.png')
})
