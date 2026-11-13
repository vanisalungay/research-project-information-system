/**
 * scripts/fetch-psgc-data.mjs
 * ---------------------------------------------------------------------------
 * Regenerates the bundled Philippine administrative location dataset used by
 * the Create Proposal form's cascading address dropdowns
 * (src/utils/philippineLocations.js -> /locations/ph/*).
 *
 * Source of the data
 *   Philippine Standard Geographic Code (PSGC), the official list of regions,
 *   provinces, districts, cities/municipalities and barangays maintained by the
 *   Philippine Statistics Authority (PSA). It is consumed here through the
 *   public, read-only static JSON mirror at https://psgc.gitlab.io/api, which
 *   republishes the PSGC release unchanged (no third-party invention of
 *   administrative units).
 *
 * Why a script instead of fetching at runtime
 *   The RPIS is deployed on campus and must keep working without internet
 *   access, so the hierarchy is downloaded once, trimmed to the fields the form
 *   actually needs (which keeps the repository copy small) and committed under
 *   public/locations/ph/. Re-run this script to refresh it:
 *
 *       node scripts/fetch-psgc-data.mjs
 *
 * Output (all values are real PSGC codes/names - nothing is generated)
 *   public/locations/ph/regions.json                 [{ code, name, regionName }]
 *   public/locations/ph/provinces.json               [{ code, name, regionCode }]
 *   public/locations/ph/districts.json               [{ code, name, regionCode }]
 *   public/locations/ph/cities-municipalities.json   [{ code, name, regionCode,
 *                                                       provinceCode, districtCode,
 *                                                       isCity, isMunicipality }]
 *   public/locations/ph/barangays/<regionCode>.json  [{ code, name, city }]
 *   public/locations/ph/meta.json                    source + counts + fetchedAt
 *
 * `city` on a barangay is the PSGC code of its city or municipality (the PSGC
 * records either cityCode or municipalityCode for every barangay; the script
 * fails loudly if that is ever not the case).
 *
 * After refreshing the bundle, regenerate the backend's copy of the hierarchy
 * (used to validate the address parts of a submitted proposal) as well:
 *
 *       node scripts/build-backend-location-catalog.mjs
 */
import { mkdir, rm, writeFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const API = 'https://psgc.gitlab.io/api'
const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..')
const OUT = join(ROOT, 'public', 'locations', 'ph')

/** Fetches one PSGC resource, retrying a few times on transient failures. */
const get = async (path, attempts = 4) => {
  let lastError
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const response = await fetch(`${API}/${path}`)
      if (!response.ok) throw new Error(`HTTP ${response.status}`)
      return await response.json()
    } catch (error) {
      lastError = error
      await new Promise((resolve) => setTimeout(resolve, 500 * attempt))
    }
  }
  throw new Error(`Failed to fetch ${API}/${path}: ${lastError?.message}`)
}

/** PSGC uses `false` for "not applicable"; the dataset uses null. */
const ref = (value) => (typeof value === 'string' && value.trim() ? value.trim() : null)

const write = async (relativePath, data) => {
  const target = join(OUT, relativePath)
  await mkdir(dirname(target), { recursive: true })
  await writeFile(target, `${JSON.stringify(data)}\n`, 'utf8')
  const bytes = Buffer.byteLength(JSON.stringify(data))
  console.log(`  wrote ${relativePath} (${data.length} records, ${(bytes / 1024).toFixed(1)} KB)`)
}

const main = async () => {
  console.log(`Fetching PSGC data from ${API} ...`)
  const [regions, provinces, districts, cities] = await Promise.all([
    get('regions.json'),
    get('provinces.json'),
    get('districts.json'),
    get('cities-municipalities.json'),
  ])

  const regionCodes = new Set(regions.map((r) => r.code))
  const provinceCodes = new Set(provinces.map((p) => p.code))
  const districtCodes = new Set(districts.map((d) => d.code))
  const cityCodes = new Set(cities.map((c) => c.code))

  // Integrity checks: the committed dataset must describe a coherent hierarchy.
  for (const province of provinces) {
    if (!regionCodes.has(province.regionCode)) throw new Error(`province ${province.name} has no region`)
  }
  for (const city of cities) {
    if (!regionCodes.has(city.regionCode)) throw new Error(`city ${city.name} has no region`)
    if (ref(city.provinceCode) && !provinceCodes.has(city.provinceCode)) {
      throw new Error(`city ${city.name} has an unknown province`)
    }
    if (ref(city.districtCode) && !districtCodes.has(city.districtCode)) {
      throw new Error(`city ${city.name} has an unknown district`)
    }
  }

  console.log('Writing trimmed datasets ...')
  await rm(join(OUT, 'barangays'), { recursive: true, force: true })
  await write('regions.json', regions.map((r) => ({
    code: r.code,
    name: r.name,
    regionName: r.regionName,
  })))
  await write('provinces.json', provinces.map((p) => ({
    code: p.code,
    name: p.name,
    regionCode: p.regionCode,
  })))
  await write('districts.json', districts.map((d) => ({
    code: d.code,
    name: d.name,
    regionCode: d.regionCode,
  })))
  await write('cities-municipalities.json', cities.map((c) => ({
    code: c.code,
    name: c.name,
    regionCode: c.regionCode,
    provinceCode: ref(c.provinceCode),
    districtCode: ref(c.districtCode),
    isCity: Boolean(c.isCity),
    isMunicipality: Boolean(c.isMunicipality),
  })))

  console.log('Writing barangays per region ...')
  let barangayTotal = 0
  for (const region of regions) {
    const barangays = await get(`regions/${region.code}/barangays.json`)
    const trimmed = barangays.map((b) => {
      const city = ref(b.cityCode) || ref(b.municipalityCode)
      if (!city) throw new Error(`barangay ${b.name} (${b.code}) has no city/municipality`)
      if (!cityCodes.has(city)) throw new Error(`barangay ${b.name} has an unknown city/municipality`)
      return { code: b.code, name: b.name, city }
    })
    barangayTotal += trimmed.length
    await write(join('barangays', `${region.code}.json`), trimmed)
  }

  await write('meta.json', [{
    source: 'Philippine Standard Geographic Code (PSGC), Philippine Statistics Authority',
    sourceUrl: `${API}/`,
    fetchedAt: new Date().toISOString(),
    counts: {
      regions: regions.length,
      provinces: provinces.length,
      districts: districts.length,
      citiesMunicipalities: cities.length,
      barangays: barangayTotal,
    },
  }])

  console.log(`Done. ${regions.length} regions, ${provinces.length} provinces, ${districts.length} districts, ` +
    `${cities.length} cities/municipalities, ${barangayTotal} barangays.`)
}

main().catch((error) => {
  console.error(error)
  process.exitCode = 1
})
