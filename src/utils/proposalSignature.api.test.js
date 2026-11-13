/**
 * proposalSignature.api.test.js
 * ---------------------------------------------------------------------------
 * The loading contract of the Certification & Endorsement signature: the module
 * talks to the token-scoped account endpoint only (never to a user id, so another
 * account's signature can never be read), a saved signature arrives as a blob with
 * the file name the server announced, an account without one is an empty list, and
 * any other failure is thrown so the step can offer the upload fallback.
 *
 * The module is loaded through Vite (the same pipeline the app uses) with the app's
 * axios instance replaced by a stub, so no real request is made.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createServer } from 'vite'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

/** Swaps the app's axios instance for `globalThis.__rpisApiStub`. */
const stubApi = () => ({
  name: 'rpis-api-stub',
  enforce: 'pre',
  load(id) {
    return id.replace(/\\/g, '/').endsWith('/src/utils/api.js')
      ? 'export default globalThis.__rpisApiStub'
      : null
  },
})

const startServer = () =>
  createServer({
    root: ROOT,
    server: { middlewareMode: true },
    appType: 'custom',
    logLevel: 'silent',
    plugins: [stubApi()],
  })

/** Records the calls the module makes and answers with `respond`. */
const installApi = (respond) => {
  const calls = []
  globalThis.__rpisApiStub = {
    get: async (url, config) => {
      calls.push({ url, config })
      return respond(url, config)
    },
  }
  return calls
}

const notFound = () => {
  const error = new Error('Request failed with status code 404')
  error.response = { status: 404 }
  throw error
}

test('the saved signature of the signed-in account is loaded', async () => {
  const server = await startServer()

  try {
    const calls = installApi(async () => ({
      data: new Blob([new Uint8Array([137, 80, 78, 71])], { type: 'image/png' }),
      headers: { 'content-disposition': 'inline; filename="my-signature.png"' },
    }))
    const { loadAccountSignatures } = await server.ssrLoadModule(
      '/src/utils/proposalSignature.js',
    )

    const signatures = await loadAccountSignatures()

    assert.equal(signatures.length, 1)
    assert.equal(signatures[0].fileName, 'my-signature.png')
    assert.equal(signatures[0].source, 'account')
    assert.equal(signatures[0].blob.size, 4)
    // Only the caller's own signature is ever requested: the endpoint carries no
    // user id, and the image is fetched as a blob so it can be previewed.
    assert.equal(calls.length, 1)
    assert.equal(calls[0].url, '/api/users/me/signature')
    assert.equal(calls[0].config.responseType, 'blob')
  } finally {
    await server.close()
  }
})

test('an account without a saved signature has none to offer', async () => {
  const server = await startServer()

  try {
    installApi(notFound)
    const { loadAccountSignatures } = await server.ssrLoadModule(
      '/src/utils/proposalSignature.js',
    )

    assert.deepEqual(await loadAccountSignatures(), [])
  } finally {
    await server.close()
  }
})

test('a failing request is reported so the step can fall back to uploading', async () => {
  const server = await startServer()

  try {
    installApi(() => {
      const error = new Error('Network Error')
      throw error
    })
    const { loadAccountSignatures } = await server.ssrLoadModule(
      '/src/utils/proposalSignature.js',
    )

    await assert.rejects(() => loadAccountSignatures(), /Network Error/)
  } finally {
    await server.close()
  }
})
