/**
 * useAddressCascade.test.js
 * ---------------------------------------------------------------------------
 * Guards the cascading Address section of the Create Proposal form.
 *
 * The composable is loaded through Vite (the same pipeline the app uses) so its
 * `@/utils/...` imports resolve, and the bundled PSGC files are served from disk,
 * so the real dataset is exercised. Two invariants are covered:
 *
 *   1. every property the form's template reads is a plain value, not a ref. The
 *      template uses `addressCascade.manual`, `addressCascade.regionOptions`, ...
 *      directly; a ref object is always truthy, which silently replaced the five
 *      dropdowns with their free-text fallback inputs.
 *   2. the cascade itself against the real PSGC extract: Philippines -> Region ->
 *      Province -> City -> Barangay, clearing the levels below a changed parent,
 *      the districted region (NCR) and an independent city.
 *
 * Run with: npm test
 */
import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { ref } from 'vue'
import { createServer } from 'vite'

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', '..')

/** Serves the bundled PSGC extract to the module's own `fetch` calls. */
const serveBundledData = () => {
  const original = globalThis.fetch
  globalThis.fetch = async (url) => {
    const path = String(url).replace(/^\/+/, '')
    const text = await readFile(join(ROOT, 'public', path), 'utf8')
    return { ok: true, status: 200, json: async () => JSON.parse(text) }
  }
  return () => {
    globalThis.fetch = original
  }
}

/** Waits for a load the composable performs in the background. */
const waitFor = async (predicate, timeout = 5000) => {
  const deadline = Date.now() + timeout
  while (Date.now() < deadline) {
    if (predicate()) return
    await new Promise((resolve) => setTimeout(resolve, 20))
  }
  throw new Error('timed out waiting for the location data')
}

const startServer = () =>
  createServer({
    root: ROOT,
    server: { middlewareMode: true },
    appType: 'custom',
    logLevel: 'silent',
  })

const emptyDetails = () => ({
  country: 'Philippines',
  region: '',
  province: '',
  district: '',
  municipality: '',
  barangay: '',
})

test('the cascade exposes plain values, not refs, for the template to read', async () => {
  const restoreFetch = serveBundledData()
  const server = await startServer()

  try {
    const { useAddressCascade } = await server.ssrLoadModule(
      '/src/composables/useAddressCascade.js',
    )
    const details = ref(emptyDetails())
    const cascade = useAddressCascade(details)

    // A ref object is always truthy: `v-if="!addressCascade.manual"` would be false
    // and the five levels would render their free-text fallback input.
    assert.equal(typeof cascade.manual, 'boolean')
    assert.equal(cascade.manual, false)
    assert.equal(typeof cascade.dropdowns, 'boolean')
    assert.equal(typeof cascade.regionEnabled, 'boolean')
    assert.equal(typeof cascade.notice, 'string')
    assert.equal(typeof cascade.placeholders.country, 'string')
    assert.ok(Array.isArray(cascade.regionOptions))

    // Nothing is reported as a problem while the data loads and afterwards - the
    // placeholders and the notice are the visible loading / empty / error states.
    assert.equal(cascade.notice, '')
    assert.equal(cascade.retryable, false)

    await waitFor(() => cascade.regionOptions.length > 0)
    assert.equal(cascade.regionOptions.length, 17)
    assert.equal(cascade.placeholders.region, 'Select Region')
  } finally {
    await server.close()
    restoreFetch()
  }
})

test('the whole cascade works against the bundled PSGC dataset', async () => {
  const restoreFetch = serveBundledData()
  const server = await startServer()

  try {
    const { useAddressCascade } = await server.ssrLoadModule(
      '/src/composables/useAddressCascade.js',
    )
    const details = ref(emptyDetails())
    const cascade = useAddressCascade(details)
    await waitFor(() => cascade.regionOptions.length > 0)

    // Region -> province -> city -> barangay.
    cascade.select('region', 'Northern Mindanao')
    await waitFor(() => cascade.provinceOptions.length > 0)
    assert.deepEqual(
      cascade.provinceOptions.map((province) => province.name),
      ['Bukidnon', 'Camiguin', 'Lanao Del Norte', 'Misamis Occidental', 'Misamis Oriental'],
    )
    assert.deepEqual(cascade.districtOptions, [])
    assert.equal(cascade.placeholders.district, 'Not applicable - no districts in this area')

    cascade.select('province', 'Misamis Oriental')
    await waitFor(() => cascade.cityOptions.length > 0)
    assert.ok(cascade.cityOptions.some((city) => city.name === 'Naawan'))

    cascade.select('municipality', 'Naawan')
    await waitFor(() => cascade.barangayOptions.length > 0)
    const barangay = cascade.barangayOptions[0].name
    cascade.select('barangay', barangay)
    assert.deepEqual(details.value, {
      country: 'Philippines',
      region: 'Northern Mindanao',
      province: 'Misamis Oriental',
      district: '',
      municipality: 'Naawan',
      barangay,
    })

    // Changing a parent clears every dependent level.
    cascade.select('region', 'NCR')
    assert.deepEqual(details.value, {
      country: 'Philippines',
      region: 'NCR',
      province: '',
      district: '',
      municipality: '',
      barangay: '',
    })
    await waitFor(() => cascade.districtOptions.length > 0)
    assert.equal(cascade.provinceOptions.length, 0)
    assert.equal(cascade.districtOptions.length, 4)
    assert.equal(cascade.cityOptions.length, 0) // waits for the district
    assert.equal(cascade.placeholders.province, 'Not applicable - this region has no provinces')
    cascade.select('district', 'First District')
    await waitFor(() => cascade.cityOptions.length > 0)
    assert.deepEqual(
      cascade.cityOptions.map((city) => city.name),
      ['City of Manila'],
    )

    // An independent city belongs to no province and no district.
    cascade.select('region', 'Zamboanga Peninsula')
    await waitFor(() => cascade.cityOptions.length > 0)
    assert.deepEqual(
      cascade.cityOptions.map((city) => city.name),
      ['City of Isabela'],
    )
    cascade.select('municipality', 'City of Isabela')
    await waitFor(() => cascade.barangayOptions.length > 0)
    assert.equal(cascade.independentCity, true)
  } finally {
    await server.close()
    restoreFetch()
  }
})
